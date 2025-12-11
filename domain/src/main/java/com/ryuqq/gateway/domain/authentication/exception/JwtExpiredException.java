package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * JWT 만료 예외
 *
 * <p>JWT 토큰이 만료되었을 때 발생하는 예외
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>JWT의 exp claim이 현재 시간보다 과거인 경우
 *   <li>{@link com.ryuqq.gateway.domain.authentication.vo.JwtToken#isExpired()} 호출 시 true인 경우
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 401 UNAUTHORIZED
 *   <li>Error Code: AUTH-001
 *   <li>Message: "JWT token has expired: {accessToken}"
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class JwtExpiredException extends DomainException {

    /**
     * JWT Access Token을 포함한 예외 생성
     *
     * @param accessToken 만료된 JWT Access Token
     * @author development-team
     * @since 1.0.0
     */
    public JwtExpiredException(String accessToken) {
        super(AuthenticationErrorCode.JWT_EXPIRED, "accessToken: " + accessToken);
    }

    /**
     * 기본 에러 메시지 사용
     *
     * <p>AuthenticationErrorCode의 기본 메시지를 사용합니다.
     *
     * @author development-team
     * @since 1.0.0
     */
    public JwtExpiredException() {
        super(AuthenticationErrorCode.JWT_EXPIRED);
    }
}
