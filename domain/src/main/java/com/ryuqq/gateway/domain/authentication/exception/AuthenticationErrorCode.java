package com.ryuqq.gateway.domain.authentication.exception;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;

/**
 * AuthenticationErrorCode - Authentication Bounded Context 에러 코드
 *
 * <p>JWT 인증 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 *
 * <p><strong>에러 코드 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 형식: AUTH-{3자리 숫자}
 *   <li>✅ HTTP 상태 코드 매핑
 *   <li>✅ 명확한 에러 메시지
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * throw new JwtExpiredException("expired-token");
 * // → ErrorCode: AUTH-001, HTTP Status: 401
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum AuthenticationErrorCode implements ErrorCode {

    /**
     * JWT 토큰 만료
     *
     * <p>JWT의 exp claim이 현재 시간보다 과거인 경우 발생
     */
    JWT_EXPIRED("AUTH-001", 401, "JWT token has expired"),

    /**
     * JWT 토큰 유효하지 않음
     *
     * <p>JWT 형식 오류, 서명 검증 실패, 필수 claim 누락 등의 경우 발생
     */
    JWT_INVALID("AUTH-002", 401, "JWT token is invalid"),

    /**
     * Public Key 찾을 수 없음
     *
     * <p>JWT Header의 kid에 해당하는 Public Key가 JWKS에 없는 경우 발생
     */
    PUBLIC_KEY_NOT_FOUND("AUTH-003", 404, "Public key not found"),

    /**
     * Refresh Token 유효하지 않음
     *
     * <p>Refresh Token 형식 오류, 검증 실패 등의 경우 발생
     */
    REFRESH_TOKEN_INVALID("AUTH-004", 401, "Refresh token is invalid"),

    /**
     * Refresh Token 만료
     *
     * <p>Refresh Token의 exp claim이 현재 시간보다 과거인 경우 발생
     */
    REFRESH_TOKEN_EXPIRED("AUTH-005", 401, "Refresh token has expired"),

    /**
     * Refresh Token 재사용 감지
     *
     * <p>이미 사용된(Blacklist 등록된) Refresh Token 재사용 시 발생 (탈취 의심)
     */
    REFRESH_TOKEN_REUSED("AUTH-006", 401, "Refresh token reuse detected"),

    /**
     * Refresh Token 누락
     *
     * <p>Cookie에 Refresh Token이 없는 경우 발생
     */
    REFRESH_TOKEN_MISSING("AUTH-007", 401, "Refresh token is missing"),

    /**
     * Token Refresh 실패
     *
     * <p>Lock 획득 실패, AuthHub 장애 등으로 재발급 실패 시 발생
     */
    TOKEN_REFRESH_FAILED("AUTH-008", 500, "Token refresh failed");

    private final String code;
    private final int httpStatus;
    private final String message;

    /**
     * Constructor - ErrorCode 생성
     *
     * @param code 에러 코드 (AUTH-XXX)
     * @param httpStatus HTTP 상태 코드
     * @param message 에러 메시지
     * @author development-team
     * @since 1.0.0
     */
    AuthenticationErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 문자열 (예: AUTH-001)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드 (예: 401, 404)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 에러 메시지 반환
     *
     * @return 에러 메시지 문자열
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getMessage() {
        return message;
    }
}
