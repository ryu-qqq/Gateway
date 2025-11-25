package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PublicKeyMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PublicKeyRedisRepository;
import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Public Key Command Adapter
 *
 * <p>PublicKeyCommandPort 구현체 (Redis Cache 저장)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>기존 Redis Cache 전체 삭제
 *   <li>새로운 Public Key들을 Redis에 저장
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyCommandAdapter implements PublicKeyCommandPort {

    private final PublicKeyRedisRepository publicKeyRedisRepository;
    private final PublicKeyMapper publicKeyMapper;

    public PublicKeyCommandAdapter(
            PublicKeyRedisRepository publicKeyRedisRepository, PublicKeyMapper publicKeyMapper) {
        this.publicKeyRedisRepository = publicKeyRedisRepository;
        this.publicKeyMapper = publicKeyMapper;
    }

    /**
     * Public Key 목록을 Cache에 저장
     *
     * <p><strong>Process</strong>:
     *
     * <ol>
     *   <li>기존 Redis Cache 전체 삭제
     *   <li>새로운 Public Key들을 Redis에 저장
     * </ol>
     *
     * @param publicKeys Public Key 목록
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> saveAll(List<PublicKey> publicKeys) {
        return publicKeyRedisRepository
                .deleteAll()
                .thenMany(Flux.fromIterable(publicKeys))
                .flatMap(
                        publicKey -> {
                            var entity = publicKeyMapper.toPublicKeyEntity(publicKey);
                            return publicKeyRedisRepository.save(publicKey.kid(), entity);
                        })
                .then()
                .onErrorMap(e -> new RuntimeException("Failed to save public keys to Redis", e));
    }
}
