package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * MFA Verification Filter
 *
 * <p>Spring Cloud Gateway GlobalFilter로 MFA 필수 검증을 수행합니다.
 *
 * <p><strong>실행 순서</strong>: Tenant Isolation Filter 이후 (Order: HIGHEST_PRECEDENCE + 7)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Exchange Attribute에서 Tenant Config 조회
 *   <li>Tenant Config의 mfaRequired 설정 확인
 *   <li>MFA 필수 시 JWT Claim의 mfaVerified 검증
 *   <li>MFA 미검증 시 403 Forbidden 응답
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>TenantIsolationFilter (HIGHEST_PRECEDENCE + 5)에서 tenantContext 설정
 *   <li>JwtAuthenticationFilter (HIGHEST_PRECEDENCE + 2)에서 mfaVerified 설정
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class MfaVerificationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MfaVerificationFilter.class);

    private static final String TENANT_CONTEXT_ATTRIBUTE = "tenantContext";
    private static final String MFA_VERIFIED_ATTRIBUTE = "mfaVerified";
    private static final String USER_ID_ATTRIBUTE = "userId";

    private final GatewayErrorResponder errorResponder;

    public MfaVerificationFilter(GatewayErrorResponder errorResponder) {
        this.errorResponder = errorResponder;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.MFA_VERIFICATION_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        TenantConfig tenantConfig = exchange.getAttribute(TENANT_CONTEXT_ATTRIBUTE);
        String userId = exchange.getAttribute(USER_ID_ATTRIBUTE);

        // Tenant Context가 없는 경우 (비인증 요청 또는 TenantIsolationFilter 미실행)
        if (tenantConfig == null) {
            log.debug("No tenant context found, skipping MFA verification");
            return chain.filter(exchange);
        }

        // MFA 필수 여부 확인
        if (!tenantConfig.isMfaRequired()) {
            log.debug("MFA not required for tenant: tenantId={}", tenantConfig.getTenantIdValue());
            return chain.filter(exchange);
        }

        // MFA 검증 상태 확인
        Boolean mfaVerified = exchange.getAttribute(MFA_VERIFIED_ATTRIBUTE);

        if (mfaVerified == null || !mfaVerified) {
            log.warn(
                    "MFA verification required but not verified: tenantId={}, userId={}",
                    tenantConfig.getTenantIdValue(),
                    userId);
            return forbidden(
                    exchange,
                    "MFA_REQUIRED",
                    "이 테넌트는 MFA 인증이 필요합니다",
                    tenantConfig.getTenantIdValue());
        }

        log.debug(
                "MFA verification passed: tenantId={}, userId={}",
                tenantConfig.getTenantIdValue(),
                userId);
        return chain.filter(exchange);
    }

    private Mono<Void> forbidden(
            ServerWebExchange exchange, String errorCode, String message, String tenantId) {
        return errorResponder.forbidden(exchange, errorCode, message);
    }
}
