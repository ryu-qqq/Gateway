package com.ryuqq.gateway.application.authentication.service.command;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.factory.AuthenticationFactory;
import com.ryuqq.gateway.application.authentication.internal.TokenRefreshCoordinator;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshAccessTokenUseCase;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import com.ryuqq.observability.logging.annotation.Loggable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Access Token Refresh Service (RefreshAccessTokenUseCase 구현체)
 *
 * <p>Refresh Token을 사용하여 새 Access Token을 발급받는 UseCase 구현체
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>Command → Domain VO 변환 (AuthenticationFactory)
 *   <li>Token Refresh 조율 위임 (TokenRefreshCoordinator)
 * </ol>
 *
 * <p><strong>Zero-Tolerance 준수</strong>:
 *
 * <ul>
 *   <li>Transaction 불필요 (외부 API + Redis 사용)
 *   <li>Lombok 금지
 *   <li>Reactive Programming (Mono/Flux)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {

    private final TokenRefreshCoordinator tokenRefreshCoordinator;
    private final AuthenticationFactory authenticationFactory;

    public RefreshAccessTokenService(
            TokenRefreshCoordinator tokenRefreshCoordinator,
            AuthenticationFactory authenticationFactory) {
        this.tokenRefreshCoordinator = tokenRefreshCoordinator;
        this.authenticationFactory = authenticationFactory;
    }

    /**
     * Access Token Refresh 실행
     *
     * @param command RefreshAccessTokenCommand (tenantId, userId, refreshToken)
     * @return Mono&lt;RefreshAccessTokenResponse&gt; (새 Access Token + Refresh Token)
     */
    @Loggable(value = "Access Token Refresh", includeArgs = false, slowThreshold = 1000)
    @Override
    public Mono<RefreshAccessTokenResponse> execute(RefreshAccessTokenCommand command) {
        String tenantId = command.tenantId();
        Long userId = command.userId();
        RefreshToken currentRefreshToken = authenticationFactory.createRefreshToken(command);

        return tokenRefreshCoordinator.coordinate(tenantId, userId, currentRefreshToken);
    }
}
