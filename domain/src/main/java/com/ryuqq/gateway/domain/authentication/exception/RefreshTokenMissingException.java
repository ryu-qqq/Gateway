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
public final class RefreshTokenMissingException extends DomainException {

    /**
     * Constructor - 상세 정보로 예외 생성
     *
     * @param detail 상세 정보
     */
    public RefreshTokenMissingException(String detail) {
        super(AuthenticationErrorCode.REFRESH_TOKEN_MISSING, detail);
    }

    /** Constructor - 기본 예외 생성 */
    public RefreshTokenMissingException() {
        super(AuthenticationErrorCode.REFRESH_TOKEN_MISSING);
    }
}
