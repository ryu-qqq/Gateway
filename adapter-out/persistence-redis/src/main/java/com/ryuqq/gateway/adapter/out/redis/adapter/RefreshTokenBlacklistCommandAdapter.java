package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.RefreshTokenBlacklistRedisRepository;
import com.ryuqq.gateway.application.authentication.port.out.command.RefreshTokenBlacklistCommandPort;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Command Adapter
 *
 * <p>RefreshTokenBlacklistCommandPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Refresh Token Blacklist 등록 (SET + TTL)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RefreshTokenBlacklistCommandAdapter implements RefreshTokenBlacklistCommandPort {

    private final RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository;

    public RefreshTokenBlacklistCommandAdapter(
            RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository) {
        this.refreshTokenBlacklistRedisRepository = refreshTokenBlacklistRedisRepository;
    }

    /**
     * Refresh Token을 Blacklist에 등록
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken Blacklist에 등록할 Refresh Token
     * @param ttlSeconds TTL (초)
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> addToBlacklist(String tenantId, RefreshToken refreshToken, long ttlSeconds) {
        return refreshTokenBlacklistRedisRepository
                .addToBlacklist(tenantId, refreshToken.value(), Duration.ofSeconds(ttlSeconds))
                .then();
    }
}
