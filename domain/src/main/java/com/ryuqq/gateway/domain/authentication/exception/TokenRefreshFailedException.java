package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * TokenRefreshFailedException - Token Refresh가 실패한 경우 발생하는 예외
 *
 * <p>Lock 획득 실패, AuthHub 장애 등으로 재발급 실패 시 발생합니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>Redis Lock 획득 실패 (동시 Refresh 요청)
 *   <li>AuthHub 서비스 장애
 *   <li>네트워크 오류
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TokenRefreshFailedException extends DomainException {

    /**
     * Constructor - 상세 정보로 예외 생성
     *
     * @param detail 상세 정보
     */
    public TokenRefreshFailedException(String detail) {
        super(AuthenticationErrorCode.TOKEN_REFRESH_FAILED, detail);
    }

    /**
     * Constructor - 상세 정보와 원인으로 예외 생성
     *
     * @param detail 상세 정보
     * @param cause 원인 예외
     */
    public TokenRefreshFailedException(String detail, Throwable cause) {
        super(AuthenticationErrorCode.TOKEN_REFRESH_FAILED, detail + ": " + cause.getMessage());
        initCause(cause);
    }

    /** Constructor - 기본 예외 생성 */
    public TokenRefreshFailedException() {
        super(AuthenticationErrorCode.TOKEN_REFRESH_FAILED);
    }
}
