package com.ryuqq.gateway.adapter.in.gateway.config;

import org.springframework.core.Ordered;

/**
 * Gateway Filter Order 상수 정의
 *
 * <p>Spring Cloud Gateway Filter Chain의 실행 순서를 정의합니다.
 *
 * <p><strong>Note:</strong> 모든 상수는 {@link Ordered#HIGHEST_PRECEDENCE} ({@value Integer#MIN_VALUE})를
 * 기준으로 상대적 오프셋을 사용합니다.
 *
 * <p><strong>Filter 실행 순서 (상대적 오프셋)</strong>:
 *
 * <ol>
 *   <li>TRACE_ID_FILTER (HIGHEST_PRECEDENCE + 0) - 요청 추적 ID 생성
 *   <li>RATE_LIMIT_FILTER (HIGHEST_PRECEDENCE + 1) - IP/Endpoint Rate Limiting
 *   <li>JWT_AUTH_FILTER (HIGHEST_PRECEDENCE + 2) - JWT 인증
 *   <li>USER_RATE_LIMIT_FILTER (HIGHEST_PRECEDENCE + 3) - User Rate Limiting (JWT 파싱 후)
 *   <li>TOKEN_REFRESH_FILTER (HIGHEST_PRECEDENCE + 4) - Token 갱신
 *   <li>TENANT_ISOLATION_FILTER (HIGHEST_PRECEDENCE + 5) - 테넌트 격리
 *   <li>PERMISSION_FILTER (HIGHEST_PRECEDENCE + 6) - 권한 검사
 *   <li>MFA_VERIFICATION_FILTER (HIGHEST_PRECEDENCE + 7) - MFA 검증
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class GatewayFilterOrder {

    /** Highest Precedence (Ordered.HIGHEST_PRECEDENCE = Integer.MIN_VALUE) */
    public static final int HIGHEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;

    /** Trace ID Filter Order (HIGHEST_PRECEDENCE + 0) - 가장 먼저 실행 */
    public static final int TRACE_ID_FILTER = HIGHEST_PRECEDENCE;

    /** Rate Limit Filter Order (HIGHEST_PRECEDENCE + 1) */
    public static final int RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 1;

    /** JWT Authentication Filter Order (HIGHEST_PRECEDENCE + 2) */
    public static final int JWT_AUTH_FILTER = HIGHEST_PRECEDENCE + 2;

    /** User Rate Limit Filter Order (HIGHEST_PRECEDENCE + 3) - JWT 파싱 후 User Rate Limit 체크 */
    public static final int USER_RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 3;

    /** Token Refresh Filter Order (HIGHEST_PRECEDENCE + 4) */
    public static final int TOKEN_REFRESH_FILTER = HIGHEST_PRECEDENCE + 4;

    /** Tenant Isolation Filter Order (HIGHEST_PRECEDENCE + 5) */
    public static final int TENANT_ISOLATION_FILTER = HIGHEST_PRECEDENCE + 5;

    /** Permission Filter Order (HIGHEST_PRECEDENCE + 6) */
    public static final int PERMISSION_FILTER = HIGHEST_PRECEDENCE + 6;

    /** MFA Verification Filter Order (HIGHEST_PRECEDENCE + 7) */
    public static final int MFA_VERIFICATION_FILTER = HIGHEST_PRECEDENCE + 7;

    private GatewayFilterOrder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
