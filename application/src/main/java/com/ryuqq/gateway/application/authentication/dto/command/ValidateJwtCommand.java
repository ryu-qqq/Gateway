package com.ryuqq.gateway.application.authentication.dto.command;

/**
 * JWT 검증 Command DTO
 *
 * <p>JWT Access Token 검증 요청을 나타내는 불변 Command 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>accessToken은 null 또는 blank일 수 없다
 * </ul>
 *
 * @param accessToken JWT Access Token (null/blank 불가)
 */
public record ValidateJwtCommand(String accessToken) {

    /** Compact Constructor - 검증 로직 */
    public ValidateJwtCommand {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("AccessToken cannot be null or blank");
        }
    }
}
