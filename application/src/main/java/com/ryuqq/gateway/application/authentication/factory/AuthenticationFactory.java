package com.ryuqq.gateway.application.authentication.factory;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import org.springframework.stereotype.Component;

/**
 * Authentication Factory
 *
 * <p>Application Command를 Domain VO로 변환하는 Factory
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Command DTO → Domain VO 변환
 *   <li>VO 생성 로직 중앙화
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthenticationFactory {

    /**
     * RefreshAccessTokenCommand에서 RefreshToken VO 생성
     *
     * @param command RefreshAccessTokenCommand
     * @return RefreshToken Domain VO
     */
    public RefreshToken createRefreshToken(RefreshAccessTokenCommand command) {
        return RefreshToken.of(command.refreshToken());
    }

    /**
     * RefreshToken 문자열로 RefreshToken VO 생성
     *
     * @param refreshTokenValue Refresh Token 문자열
     * @return RefreshToken Domain VO
     */
    public RefreshToken createRefreshToken(String refreshTokenValue) {
        return RefreshToken.of(refreshTokenValue);
    }
}
