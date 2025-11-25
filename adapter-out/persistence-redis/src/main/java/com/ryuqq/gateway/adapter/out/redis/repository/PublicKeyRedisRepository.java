package com.ryuqq.gateway.adapter.out.redis.repository;

import com.ryuqq.gateway.adapter.out.redis.entity.PublicKeyEntity;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Public Key Redis Repository
 *
 * <p>Redis에 Public Key를 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code authhub:jwt:publickey:{kid}}
 *   <li>TTL: 1시간
 * </ul>
 *
 * <p><strong>기술 스택</strong>:
 *
 * <ul>
 *   <li>ReactiveRedisTemplate (Reactive)
 *   <li>Lettuce (Connection Pool)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class PublicKeyRedisRepository {

    private static final String PUBLIC_KEY_PREFIX = "authhub:jwt:publickey";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final ReactiveRedisTemplate<String, PublicKeyEntity> reactiveRedisTemplate;

    public PublicKeyRedisRepository(
            ReactiveRedisTemplate<String, PublicKeyEntity> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Public Key 저장 (TTL 포함)
     *
     * @param kid Key ID
     * @param publicKey PublicKeyEntity
     * @param ttl TTL (Duration)
     * @return Void
     */
    public Mono<Void> save(String kid, PublicKeyEntity publicKey, Duration ttl) {
        String redisKey = buildRedisKey(kid);
        return reactiveRedisTemplate.opsForValue().set(redisKey, publicKey, ttl).then();
    }

    /**
     * Public Key 저장 (기본 TTL: 1시간)
     *
     * @param kid Key ID
     * @param publicKey PublicKeyEntity
     * @return Void
     */
    public Mono<Void> save(String kid, PublicKeyEntity publicKey) {
        return save(kid, publicKey, DEFAULT_TTL);
    }

    /**
     * Public Key 조회
     *
     * @param kid Key ID
     * @return PublicKeyEntity (없으면 Mono.empty())
     */
    public Mono<PublicKeyEntity> findByKid(String kid) {
        String redisKey = buildRedisKey(kid);
        return reactiveRedisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Public Key 전체 삭제
     *
     * @return Void
     */
    public Mono<Void> deleteAll() {
        String pattern = PUBLIC_KEY_PREFIX + ":*";
        return reactiveRedisTemplate.keys(pattern).flatMap(reactiveRedisTemplate::delete).then();
    }

    /**
     * Redis Key 생성
     *
     * @param kid Key ID
     * @return Redis Key (authhub:jwt:publickey:{kid})
     */
    private String buildRedisKey(String kid) {
        return PUBLIC_KEY_PREFIX + ":" + kid;
    }
}
