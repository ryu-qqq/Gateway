package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * RefreshTokenExpiredException - Refresh Token이 만료된 경우 발생하는 예외
 *
 * <p>Refresh Token의 exp claim이 현재 시간보다 과거인 경우 발생합니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>AuthHub에서 Refresh Token 만료로 응답한 경우
 *   <li>Refresh Token의 유효 기간(7일)이 지난 경우
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class RefreshTokenExpiredException extends DomainException {

    /**
     * Constructor - 상세 정보로 예외 생성
     *
     * @param detail 상세 정보
     */
    public RefreshTokenExpiredException(String detail) {
        super(AuthenticationErrorCode.REFRESH_TOKEN_EXPIRED, detail);
    }

    /** Constructor - 기본 예외 생성 */
    public RefreshTokenExpiredException() {
        super(AuthenticationErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
