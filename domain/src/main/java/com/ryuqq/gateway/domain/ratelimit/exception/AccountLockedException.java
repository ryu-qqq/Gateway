package com.ryuqq.gateway.domain.ratelimit.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * AccountLockedException - 계정 잠금 예외
 *
 * <p>너무 많은 실패로 계정이 잠긴 경우 발생하는 예외입니다.
 *
 * <p><strong>HTTP 응답:</strong> 403 Forbidden
 *
 * <p><strong>잠금 조건:</strong>
 *
 * <ul>
 *   <li>비밀번호 실패 횟수 초과
 *   <li>MFA 인증 실패 횟수 초과
 * </ul>
 *
 * <p><strong>잠금 기간:</strong> 30분
 *
 * @author development-team
 * @since 1.0.0
 */
public final class AccountLockedException extends DomainException {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 1800; // 30분

    private final String userId;
    private final int retryAfterSeconds;

    /**
     * 기본 생성자
     */
    public AccountLockedException() {
        super(RateLimitErrorCode.ACCOUNT_LOCKED);
        this.userId = null;
        this.retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * 사용자 ID를 포함한 생성자
     *
     * @param userId 잠금된 사용자 ID
     */
    public AccountLockedException(String userId) {
        super(RateLimitErrorCode.ACCOUNT_LOCKED, buildDetail(userId, DEFAULT_RETRY_AFTER_SECONDS));
        this.userId = userId;
        this.retryAfterSeconds = DEFAULT_RETRY_AFTER_SECONDS;
    }

    /**
     * 사용자 ID와 잠금 해제 시간을 포함한 생성자
     *
     * @param userId 잠금된 사용자 ID
     * @param retryAfterSeconds 재시도 가능 시간 (초)
     */
    public AccountLockedException(String userId, int retryAfterSeconds) {
        super(RateLimitErrorCode.ACCOUNT_LOCKED, buildDetail(userId, retryAfterSeconds));
        this.userId = userId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * 잠금된 사용자 ID 반환
     *
     * @return 사용자 ID
     */
    public String userId() {
        return userId;
    }

    /**
     * 재시도 가능 시간 반환 (초)
     *
     * @return 재시도 가능 시간
     */
    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String buildDetail(String userId, int retryAfterSeconds) {
        return String.format("userId=%s, retryAfterSeconds=%d", userId, retryAfterSeconds);
    }
}
