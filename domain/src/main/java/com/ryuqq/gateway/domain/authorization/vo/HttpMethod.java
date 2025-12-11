package com.ryuqq.gateway.domain.authorization.vo;

/**
 * HttpMethod - HTTP 메서드 열거형
 *
 * <p>Permission Spec에서 사용하는 HTTP 메서드를 정의합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");

    private final String displayName;

    HttpMethod(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 표시용 이름 반환
     *
     * @return 표시용 이름
     * @author development-team
     * @since 1.0.0
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 문자열을 HttpMethod로 변환
     *
     * @param method HTTP 메서드 문자열
     * @return HttpMethod enum
     * @throws IllegalArgumentException 유효하지 않은 메서드인 경우
     */
    public static HttpMethod from(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method cannot be null or blank");
        }
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid HTTP method: " + method);
        }
    }
}
