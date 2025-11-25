package com.ryuqq.gateway.adapter.in.gateway.config;

import org.springframework.core.Ordered;

/**
 * Gateway Filter Order 상수 정의
 *
 * <p>Spring Cloud Gateway Filter Chain의 실행 순서를 정의합니다.
 *
 * <p><strong>Filter 실행 순서</strong>:
 *
 * <ol>
 *   <li>TRACE_ID_FILTER (0) - 요청 추적 ID 생성
 *   <li>RATE_LIMIT_FILTER (1) - Rate Limiting
 *   <li>JWT_AUTH_FILTER (2) - JWT 인증
 *   <li>TOKEN_REFRESH_FILTER (3) - Token 갱신
 *   <li>TENANT_ISOLATION_FILTER (4) - 테넌트 격리
 *   <li>PERMISSION_FILTER (5) - 권한 검사
 *   <li>MFA_VERIFICATION_FILTER (6) - MFA 검증
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class GatewayFilterOrder {

    /** Highest Precedence (Ordered.HIGHEST_PRECEDENCE) */
    public static final int HIGHEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;

    /** Trace ID Filter Order (0) */
    public static final int TRACE_ID_FILTER = HIGHEST_PRECEDENCE;

    /** Rate Limit Filter Order (1) */
    public static final int RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 1;

    /** JWT Authentication Filter Order (2) */
    public static final int JWT_AUTH_FILTER = HIGHEST_PRECEDENCE + 2;

    /** Token Refresh Filter Order (3) */
    public static final int TOKEN_REFRESH_FILTER = HIGHEST_PRECEDENCE + 3;

    /** Tenant Isolation Filter Order (4) */
    public static final int TENANT_ISOLATION_FILTER = HIGHEST_PRECEDENCE + 4;

    /** Permission Filter Order (5) */
    public static final int PERMISSION_FILTER = HIGHEST_PRECEDENCE + 5;

    /** MFA Verification Filter Order (6) */
    public static final int MFA_VERIFICATION_FILTER = HIGHEST_PRECEDENCE + 6;

    private GatewayFilterOrder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
