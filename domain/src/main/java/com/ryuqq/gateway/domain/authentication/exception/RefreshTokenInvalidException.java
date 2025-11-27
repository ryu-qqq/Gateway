package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * RefreshTokenInvalidException - Refresh Token이 유효하지 않은 경우 발생하는 예외
 *
 * <p>Refresh Token 형식 오류, 검증 실패, 필수 조건 미충족 등의 경우 발생합니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>Refresh Token이 null 또는 빈 문자열인 경우
 *   <li>Refresh Token 길이가 최소 길이 미만인 경우
 *   <li>Refresh Token 형식이 올바르지 않은 경우
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class RefreshTokenInvalidException extends DomainException {

    /**
     * Constructor - 메시지로 예외 생성
     *
     * @param message 상세 에러 메시지
     */
    public RefreshTokenInvalidException(String message) {
        super(AuthenticationErrorCode.REFRESH_TOKEN_INVALID.getCode(), message);
    }

    /**
     * 에러 코드 조회
     *
     * @return AuthenticationErrorCode.REFRESH_TOKEN_INVALID
     */
    public AuthenticationErrorCode getErrorCode() {
        return AuthenticationErrorCode.REFRESH_TOKEN_INVALID;
    }
}
