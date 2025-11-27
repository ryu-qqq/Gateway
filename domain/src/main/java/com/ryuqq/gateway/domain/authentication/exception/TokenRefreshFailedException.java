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
public class TokenRefreshFailedException extends DomainException {

    /**
     * Constructor - 메시지로 예외 생성
     *
     * @param message 상세 에러 메시지
     */
    public TokenRefreshFailedException(String message) {
        super(AuthenticationErrorCode.TOKEN_REFRESH_FAILED.getCode(), message);
    }

    /**
     * Constructor - 메시지와 원인으로 예외 생성
     *
     * @param message 상세 에러 메시지
     * @param cause 원인 예외
     */
    public TokenRefreshFailedException(String message, Throwable cause) {
        super(AuthenticationErrorCode.TOKEN_REFRESH_FAILED.getCode(), message + ": " + cause.getMessage());
    }

    /**
     * 에러 코드 조회
     *
     * @return AuthenticationErrorCode.TOKEN_REFRESH_FAILED
     */
    public AuthenticationErrorCode getErrorCode() {
        return AuthenticationErrorCode.TOKEN_REFRESH_FAILED;
    }
}
