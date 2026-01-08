package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.port.in.query.GetTenantConfigUseCase;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
 *   <li>Reactor Context에 tenantId 저장 (로깅용)
 * </ul>
 *
 * <p><strong>주의</strong>:
 *
 * <ul>
 *   <li>JWT 인증 필터가 userId, tenantId를 설정해야 합니다.
 *   <li>Downstream 헤더 전달은 JwtAuthenticationFilter에서 담당합니다.
 *   <li>이 필터는 TenantConfig 로딩 및 내부 컨텍스트 설정만 담당합니다.
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
// @Component  // TODO: 테넌트 고도화 시 활성화
public class TenantIsolationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationFilter.class);

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";
    private static final String MFA_REQUIRED_ATTRIBUTE = "mfaRequired";

    private final GetTenantConfigUseCase getTenantConfigUseCase;
    private final GatewayErrorResponder errorResponder;

    public TenantIsolationFilter(
            GetTenantConfigUseCase getTenantConfigUseCase, GatewayErrorResponder errorResponder) {
        this.getTenantConfigUseCase = getTenantConfigUseCase;
        this.errorResponder = errorResponder;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.TENANT_ISOLATION_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getAttribute(USER_ID_ATTRIBUTE);
        String tenantId = exchange.getAttribute(TENANT_ID_ATTRIBUTE);

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
                                    "Tenant context loaded: tenantId={}, mfaRequired={}, userId={}",
                                    tenantId,
                                    tenantConfig.isMfaRequired(),
                                    userId);

                            // Reactor Context에 tenantId 저장 (로깅 컨텍스트 전파)
                            // Note: Downstream 헤더 전달은 JwtAuthenticationFilter에서 담당
                            return chain.filter(exchange)
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

    private Mono<Void> internalError(ServerWebExchange exchange, String message) {
        return errorResponder.internalServerError(exchange, "TENANT_CONFIG_ERROR", message);
    }
}
