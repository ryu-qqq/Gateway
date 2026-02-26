package com.ryuqq.gateway.application.authentication.manager;

import com.ryuqq.gateway.application.authentication.port.out.command.RefreshTokenBlacklistCommandPort;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Command Manager
 *
 * <p>RefreshTokenBlacklistCommandPort를 래핑하는 Manager
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RefreshTokenBlacklistCommandManager {

    private final RefreshTokenBlacklistCommandPort refreshTokenBlacklistCommandPort;

    public RefreshTokenBlacklistCommandManager(
            RefreshTokenBlacklistCommandPort refreshTokenBlacklistCommandPort) {
        this.refreshTokenBlacklistCommandPort = refreshTokenBlacklistCommandPort;
    }

    /**
     * Refresh Token을 Blacklist에 등록
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken Blacklist에 등록할 Refresh Token
     * @param ttlSeconds TTL (초)
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    public Mono<Void> addToBlacklist(String tenantId, RefreshToken refreshToken, long ttlSeconds) {
        return refreshTokenBlacklistCommandPort.addToBlacklist(tenantId, refreshToken, ttlSeconds);
    }
}
