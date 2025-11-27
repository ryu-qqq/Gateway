package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * RefreshTokenMissingException - Refresh Token이 누락된 경우 발생하는 예외
 *
 * <p>Cookie에 Refresh Token이 없는 경우 발생합니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>Cookie에 refresh_token이 없는 경우
 *   <li>Token Refresh 요청 시 Refresh Token 미전달
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class RefreshTokenMissingException extends DomainException {

    /**
     * Constructor - 메시지로 예외 생성
     *
     * @param message 상세 에러 메시지
     */
    public RefreshTokenMissingException(String message) {
        super(AuthenticationErrorCode.REFRESH_TOKEN_MISSING.getCode(), message);
    }

    /**
     * 에러 코드 조회
     *
     * @return AuthenticationErrorCode.REFRESH_TOKEN_MISSING
     */
    public AuthenticationErrorCode getErrorCode() {
        return AuthenticationErrorCode.REFRESH_TOKEN_MISSING;
    }
}
