package com.ryuqq.gateway.domain.ratelimit.vo;

/**
 * RateLimitAction - Rate Limit 초과 시 수행할 조치 열거형
 *
 * <p>Rate Limit 정책 초과 시 수행할 조치를 정의합니다.
 *
 * <p><strong>조치 유형:</strong>
 *
 * <ul>
 *   <li>REJECT: 429 Too Many Requests 응답 반환
 *   <li>BLOCK_IP: IP 차단 (30분)
 *   <li>LOCK_ACCOUNT: 계정 잠금 (30분)
 *   <li>REVOKE_TOKEN: Refresh Token 무효화
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum RateLimitAction {

    /**
     * 요청 거부
     *
     * <p>429 Too Many Requests 응답 반환
     */
    REJECT(429, "Too Many Requests - Reject the request"),

    /**
     * IP 차단
     *
     * <p>악의적인 IP를 30분간 차단
     */
    BLOCK_IP(403, "Forbidden - Block IP for 30 minutes"),

    /**
     * 계정 잠금
     *
     * <p>계정을 30분간 잠금
     */
    LOCK_ACCOUNT(403, "Forbidden - Lock account for 30 minutes"),

    /**
     * Token 무효화
     *
     * <p>Refresh Token을 무효화하고 재로그인 요구
     */
    REVOKE_TOKEN(429, "Too Many Requests - Revoke refresh token");

    private final int httpStatus;
    private final String description;

    RateLimitAction(int httpStatus, String description) {
        this.httpStatus = httpStatus;
        this.description = description;
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 조치 설명 반환
     *
     * @return 조치 설명
     */
    public String getDescription() {
        return description;
    }
}
