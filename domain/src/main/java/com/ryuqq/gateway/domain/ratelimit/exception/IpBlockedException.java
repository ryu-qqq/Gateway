package com.ryuqq.gateway.domain.ratelimit.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * IpBlockedException - IP 차단 예외
 *
 * <p>악의적인 요청 패턴이 감지되어 IP가 차단된 경우 발생하는 예외입니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * <p><strong>차단 조건:</strong>
 *
 * <ul>
 *   <li>로그인 시도 10회 초과 (5분 내)
 *   <li>잘못된 JWT 반복 제출 10회 초과 (5분 내)
 * </ul>
 *
 * <p><strong>차단 기간:</strong> 30분
 *
 * @author development-team
 * @since 1.0.0
 */
public final class IpBlockedException extends DomainException {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 1800; // 30분

    private final String ipAddress;
    private final int retryAfterSeconds;

    /** 기본 생성자 */
    public IpBlockedException() {
        super(RateLimitErrorCode.IP_BLOCKED);
        this.ipAddress = null;
        this.retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * IP 주소를 포함한 생성자
     *
     * @param ipAddress 차단된 IP 주소
     */
    public IpBlockedException(String ipAddress) {
        super(RateLimitErrorCode.IP_BLOCKED, buildDetail(ipAddress, DEFAULT_RETRY_AFTER_SECONDS));
        this.ipAddress = ipAddress;
        this.retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * IP 주소와 차단 해제 시간을 포함한 생성자
     *
     * @param ipAddress 차단된 IP 주소
     * @param retryAfterSeconds 재시도 가능 시간 (초)
     */
    public IpBlockedException(String ipAddress, int retryAfterSeconds) {
        super(RateLimitErrorCode.IP_BLOCKED, buildDetail(ipAddress, retryAfterSeconds));
        this.ipAddress = ipAddress;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * 차단된 IP 주소 반환
     *
     * @return IP 주소
     */
    public String ipAddress() {
        return ipAddress;
    }

    /**
     * 재시도 가능 시간 반환 (초)
     *
     * @return 재시도 가능 시간
     */
    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String buildDetail(String ipAddress, int retryAfterSeconds) {
        return String.format("ipAddress=%s, retryAfterSeconds=%d", ipAddress, retryAfterSeconds);
    }
}
