package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * JWT 유효하지 않음 예외
 *
 * <p>JWT 토큰이 유효하지 않을 때 발생하는 예외
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>JWT 형식이 올바르지 않은 경우 (header.payload.signature 형식 위반)
 *   <li>JWT 서명 검증 실패 (Public Key로 검증 불가)
 *   <li>JWT Claims가 필수 값을 포함하지 않은 경우 (sub, iss, exp 누락)
 *   <li>JWT 알고리즘이 RS256이 아닌 경우
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 401 UNAUTHORIZED
 *   <li>Error Code: AUTH-002
 *   <li>Message: "JWT token is invalid: {reason}"
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class JwtInvalidException extends DomainException {

    /**
     * 유효하지 않은 이유를 포함한 예외 생성
     *
     * @param reason 유효하지 않은 이유 (형식 오류, 서명 실패 등)
     * @author development-team
     * @since 1.0.0
     */
    public JwtInvalidException(String reason) {
        super(AuthenticationErrorCode.JWT_INVALID, reason);
    }

    /**
     * 기본 에러 메시지 사용
     *
     * <p>AuthenticationErrorCode의 기본 메시지를 사용합니다.
     *
     * @author development-team
     * @since 1.0.0
     */
    public JwtInvalidException() {
        super(AuthenticationErrorCode.JWT_INVALID);
    }
}
