package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.RefreshTokenBlacklistRedisRepository;
import com.ryuqq.gateway.application.authentication.port.out.query.RefreshTokenBlacklistQueryPort;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Query Adapter
 *
 * <p>RefreshTokenBlacklistQueryPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Refresh Token Blacklist 존재 여부 확인
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RefreshTokenBlacklistQueryAdapter implements RefreshTokenBlacklistQueryPort {

    private final RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository;

    public RefreshTokenBlacklistQueryAdapter(
            RefreshTokenBlacklistRedisRepository refreshTokenBlacklistRedisRepository) {
        this.refreshTokenBlacklistRedisRepository = refreshTokenBlacklistRedisRepository;
    }

    /**
     * Refresh Token이 Blacklist에 존재하는지 확인
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken 확인할 Refresh Token
     * @return Mono&lt;Boolean&gt; Blacklist 존재 여부
     */
    @Override
    public Mono<Boolean> isBlacklisted(String tenantId, RefreshToken refreshToken) {
        return refreshTokenBlacklistRedisRepository.isBlacklisted(
                tenantId, refreshToken.getValue());
    }
}
