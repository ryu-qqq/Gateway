package com.ryuqq.gateway.application.authentication.dto.command;

/**
 * Access Token Refresh Command DTO
 *
 * <p>Access Token Refresh 요청을 나타내는 불변 Command 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>tenantId는 null 또는 blank일 수 없다
 *   <li>userId는 null일 수 없다
 *   <li>refreshToken은 null 또는 blank일 수 없다
 * </ul>
 *
 * @param tenantId Tenant 식별자 (null/blank 불가)
 * @param userId 사용자 식별자 (null 불가)
 * @param refreshToken Refresh Token 값 (null/blank 불가)
 */
public record RefreshAccessTokenCommand(String tenantId, Long userId, String refreshToken) {

    /** Compact Constructor - 검증 로직 */
    public RefreshAccessTokenCommand {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("RefreshToken cannot be null or blank");
        }
    }
}
