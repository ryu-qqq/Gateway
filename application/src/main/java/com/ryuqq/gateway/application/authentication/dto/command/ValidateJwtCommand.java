package com.ryuqq.gateway.application.authentication.dto.command;

/**
 * JWT 검증 Command DTO
 *
 * <p>JWT Access Token 검증 요청을 나타내는 불변 Command 객체
 *
 * @param accessToken JWT Access Token
 */
public record ValidateJwtCommand(String accessToken) {

    /**
     * 정적 팩토리 메서드
     *
     * @param accessToken JWT Access Token
     * @return ValidateJwtCommand 인스턴스
     */
    public static ValidateJwtCommand of(String accessToken) {
        return new ValidateJwtCommand(accessToken);
    }
}
