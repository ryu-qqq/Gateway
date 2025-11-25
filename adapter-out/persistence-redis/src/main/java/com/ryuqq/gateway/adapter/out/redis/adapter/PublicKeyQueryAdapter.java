package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PublicKeyMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PublicKeyRedisRepository;
import com.ryuqq.gateway.application.authentication.port.out.query.PublicKeyQueryPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Public Key Query Adapter
 *
 * <p>PublicKeyQueryPort 구현체 (Redis Cache 조회만)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Public Key 조회
 *   <li>Cache Miss 시 empty Mono 반환
 * </ul>
 *
 * <p><strong>설계 결정</strong>:
 *
 * <ul>
 *   <li>Adapter는 Redis 조회만 담당
 *   <li>Cache Miss Fallback(AuthHub 호출)은 Application Service에서 처리
 *   <li>이렇게 함으로써 Adapter가 Application Port를 호출하지 않음
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyQueryAdapter implements PublicKeyQueryPort {

    private final PublicKeyRedisRepository publicKeyRedisRepository;
    private final PublicKeyMapper publicKeyMapper;

    public PublicKeyQueryAdapter(
            PublicKeyRedisRepository publicKeyRedisRepository, PublicKeyMapper publicKeyMapper) {
        this.publicKeyRedisRepository = publicKeyRedisRepository;
        this.publicKeyMapper = publicKeyMapper;
    }

    /**
     * Redis Cache에서 Public Key 조회
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt; (Cache Miss 시 empty Mono)
     */
    @Override
    public Mono<PublicKey> findByKid(String kid) {
        return publicKeyRedisRepository
                .findByKid(kid)
                .map(publicKeyMapper::toPublicKey)
                .onErrorMap(
                        e -> new RuntimeException("Failed to get public key from Redis: " + kid, e));
    }
}
