package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.port.in.query.GetTenantConfigUseCase;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tenant Isolation Filter
 *
 * <p>Spring Cloud Gateway GlobalFilter로 테넌트 격리를 수행합니다.
 *
 * <p><strong>실행 순서</strong>: JWT 인증 필터 이후 (Order: HIGHEST_PRECEDENCE + 5)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>JWT에서 추출된 tenantId로 Tenant Config 조회
 *   <li>ServerWebExchange Attribute 설정 (tenantContext, mfaRequired)
 *   <li>Backend Service로 X-Tenant-Id 헤더 전달
 *   <li>Reactor Context에 tenantId 저장 (로깅용)
 * </ul>
 *
 * <p><strong>주의</strong>: JWT 인증 필터가 userId, tenantId를 설정해야 합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantIsolationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationFilter.class);

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String ORGANIZATION_ID_ATTRIBUTE = "organizationId";
    private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";
    private static final String MFA_REQUIRED_ATTRIBUTE = "mfaRequired";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String X_ORGANIZATION_ID_HEADER = "X-Organization-Id";
    private static final String X_ROLES_HEADER = "X-Roles";

    private final GetTenantConfigUseCase getTenantConfigUseCase;
    private final ObjectMapper objectMapper;

    public TenantIsolationFilter(
            GetTenantConfigUseCase getTenantConfigUseCase, ObjectMapper objectMapper) {
        this.getTenantConfigUseCase = getTenantConfigUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.TENANT_ISOLATION_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getAttribute(USER_ID_ATTRIBUTE);
        String tenantId = exchange.getAttribute(TENANT_ID_ATTRIBUTE);
        String organizationId = exchange.getAttribute(ORGANIZATION_ID_ATTRIBUTE);
        Set<String> roles = exchange.getAttribute(ROLES_ATTRIBUTE);

        // JWT 인증 필터에서 설정되지 않은 경우 (비인증 요청)
        if (userId == null || tenantId == null) {
            log.debug("No authentication context found, skipping tenant isolation");
            return chain.filter(exchange);
        }

        return getTenantConfigUseCase
                .execute(new GetTenantConfigQuery(tenantId))
                .flatMap(
                        response -> {
                            TenantConfig tenantConfig = response.tenantConfig();

                            // Exchange Attribute 설정 (Downstream Filter가 사용)
                            exchange.getAttributes().put(TENANT_CONTEXT_ATTRIBUTE, tenantConfig);
                            exchange.getAttributes()
                                    .put(MFA_REQUIRED_ATTRIBUTE, tenantConfig.isMfaRequired());

                            log.debug(
                                    "Tenant context loaded: tenantId={}, organizationId={},"
                                            + " mfaRequired={}, userId={}",
                                    tenantId,
                                    organizationId,
                                    tenantConfig.isMfaRequired(),
                                    userId);

                            // Request Header에 추가 (Backend Service로 전달)
                            ServerHttpRequest.Builder requestBuilder =
                                    exchange.getRequest()
                                            .mutate()
                                            .header(X_TENANT_ID_HEADER, tenantId)
                                            .header(X_ROLES_HEADER, serializeToJson(roles));

                            // organizationId가 있는 경우에만 헤더 추가
                            if (organizationId != null) {
                                requestBuilder.header(X_ORGANIZATION_ID_HEADER, organizationId);
                            }

                            ServerHttpRequest mutatedRequest = requestBuilder.build();

                            // Response Header에도 X-Tenant-Id, X-Organization-Id 추가 (클라이언트에게 반환)
                            exchange.getResponse().getHeaders().add(X_TENANT_ID_HEADER, tenantId);
                            if (organizationId != null) {
                                exchange.getResponse()
                                        .getHeaders()
                                        .add(X_ORGANIZATION_ID_HEADER, organizationId);
                            }

                            ServerWebExchange mutatedExchange =
                                    exchange.mutate().request(mutatedRequest).build();

                            // Reactor Context에 tenantId 저장 (로깅 컨텍스트 전파)
                            return chain.filter(mutatedExchange)
                                    .contextWrite(ctx -> ctx.put(TENANT_ID_ATTRIBUTE, tenantId));
                        })
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Failed to load tenant config: tenantId={}, error={}",
                                    tenantId,
                                    e.getMessage());
                            return internalError(exchange, "테넌트 설정을 불러오는 중 오류가 발생했습니다");
                        });
    }

    private String serializeToJson(Object obj) {
        if (obj == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private Mono<Void> internalError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorInfo error = new ErrorInfo("TENANT_CONFIG_ERROR", message);
        ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize error response: message={}, exception={}",
                    message,
                    e.getMessage());
            return exchange.getResponse().setComplete();
        }
    }
}
