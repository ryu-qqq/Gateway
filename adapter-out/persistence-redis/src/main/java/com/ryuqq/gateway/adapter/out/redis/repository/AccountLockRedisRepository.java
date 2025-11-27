package com.ryuqq.gateway.adapter.out.redis.repository;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 계정 잠금 Redis Repository
 *
 * <p>Redis에 잠금된 계정을 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code gateway:locked_account:{userId}}
 *   <li>Value: "locked"
 *   <li>TTL: 잠금 기간 (기본 30분)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class AccountLockRedisRepository {

    private static final String ACCOUNT_LOCK_PREFIX = "gateway:locked_account";
    private static final String LOCKED_VALUE = "locked";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public AccountLockRedisRepository(
            @Qualifier("reactiveStringRedisTemplate")
                    ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * 계정 잠금
     *
     * @param userId 사용자 ID
     * @param ttl 잠금 기간
     * @return Mono&lt;Boolean&gt; 잠금 성공 여부
     */
    public Mono<Boolean> lock(String userId, Duration ttl) {
        String key = buildKey(userId);
        return reactiveStringRedisTemplate.opsForValue().set(key, LOCKED_VALUE, ttl);
    }

    /**
     * 계정 잠금 해제
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    public Mono<Boolean> unlock(String userId) {
        String key = buildKey(userId);
        return reactiveStringRedisTemplate.delete(key).map(count -> count > 0);
    }

    /**
     * 계정 잠금 여부 확인
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 잠금 여부
     */
    public Mono<Boolean> isLocked(String userId) {
        String key = buildKey(userId);
        return reactiveStringRedisTemplate.hasKey(key);
    }

    /**
     * 잠금 남은 시간 조회 (초)
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Long&gt; 남은 시간 (초)
     */
    public Mono<Long> getLockTtl(String userId) {
        String key = buildKey(userId);
        return reactiveStringRedisTemplate
                .getExpire(key)
                .map(Duration::getSeconds)
                .defaultIfEmpty(-2L);
    }

    /**
     * Redis Key 생성
     *
     * @param userId 사용자 ID
     * @return Redis Key
     */
    private String buildKey(String userId) {
        return ACCOUNT_LOCK_PREFIX + ":" + userId;
    }
}
