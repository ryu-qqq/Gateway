package com.ryuqq.gateway.application.authentication.dto.command;

/**
 * Access Token Refresh Command DTO
 *
 * <p>Access Token Refresh 요청을 나타내는 불변 Command 객체
 *
 * @param tenantId Tenant 식별자
 * @param userId 사용자 식별자
 * @param refreshToken Refresh Token 값
 */
public record RefreshAccessTokenCommand(String tenantId, Long userId, String refreshToken) {

    /**
     * 정적 팩토리 메서드
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @param refreshToken Refresh Token 값
     * @return RefreshAccessTokenCommand 인스턴스
     */
    public static RefreshAccessTokenCommand of(String tenantId, Long userId, String refreshToken) {
        return new RefreshAccessTokenCommand(tenantId, userId, refreshToken);
    }
}
