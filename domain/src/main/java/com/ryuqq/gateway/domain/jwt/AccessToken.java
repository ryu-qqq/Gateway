package com.ryuqq.gateway.gateway.domain.jwt;

/**
 * JWT Access Token Value Object
 * JWT 형식 (header.payload.signature)을 검증하는 불변 객체
 */
public record AccessToken(String value) {

    private static final String JWT_PATTERN = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$";

    /**
     * Compact Constructor - JWT 형식 검증
     */
    public AccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AccessToken value cannot be null or blank");
        }

        if (!value.matches(JWT_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid JWT format: must be header.payload.signature (3 parts separated by dots)"
            );
        }
    }
}
