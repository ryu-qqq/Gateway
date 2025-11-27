package com.ryuqq.gateway.adapter.out.redis.repository;

import java.time.Duration;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Rate Limit Redis Repository
 *
 * <p>Redis에 Rate Limit 카운터를 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code gateway:rate_limit:{type}:{identifier}}
 *   <li>Value: 요청 횟수 (Long)
 *   <li>TTL: LimitType별 window 시간
 * </ul>
 *
 * <p><strong>기술 스택</strong>:
 *
 * <ul>
 *   <li>ReactiveStringRedisTemplate (Reactive)
 *   <li>Lettuce (Connection Pool)
 *   <li>INCR + EXPIRE 원자적 연산
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class RateLimitRedisRepository {

    /**
     * Lua Script: INCR + EXPIRE 원자적 연산
     *
     * <p>INCR로 카운터 증가 후, TTL이 없으면(-1) EXPIRE 설정
     *
     * <p>KEYS[1] = rate limit key, ARGV[1] = TTL (초)
     *
     * @return 증가 후 카운트 값
     */
    private static final String INCREMENT_AND_EXPIRE_SCRIPT =
            """
            local count = redis.call('INCR', KEYS[1])
            if redis.call('TTL', KEYS[1]) == -1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

    private static final RedisScript<Long> INCREMENT_EXPIRE_SCRIPT =
            RedisScript.of(INCREMENT_AND_EXPIRE_SCRIPT, Long.class);

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public RateLimitRedisRepository(
            @Qualifier("reactiveStringRedisTemplate")
                    ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * 카운터 증가 및 TTL 설정 (Atomic)
     *
     * <p>Lua Script를 사용하여 INCR + EXPIRE를 원자적으로 실행합니다. Race condition 방지.
     *
     * @param key Redis Key
     * @param ttl TTL (Duration)
     * @return Mono&lt;Long&gt; 증가 후 카운트 값
     */
    public Mono<Long> incrementAndExpire(String key, Duration ttl) {
        return reactiveStringRedisTemplate
                .execute(
                        INCREMENT_EXPIRE_SCRIPT,
                        Collections.singletonList(key),
                        Collections.singletonList(String.valueOf(ttl.getSeconds())))
                .next()
                .defaultIfEmpty(0L);
    }

    /**
     * 현재 카운터 값 조회
     *
     * @param key Redis Key
     * @return Mono&lt;Long&gt; 현재 카운트 값 (없으면 0)
     */
    public Mono<Long> getCount(String key) {
        return reactiveStringRedisTemplate
                .opsForValue()
                .get(key)
                .map(Long::parseLong)
                .defaultIfEmpty(0L);
    }

    /**
     * 남은 TTL 조회 (초)
     *
     * @param key Redis Key
     * @return Mono&lt;Long&gt; 남은 TTL (초, 키 없으면 -2, TTL 설정 안됨 -1)
     */
    public Mono<Long> getTtl(String key) {
        return reactiveStringRedisTemplate
                .getExpire(key)
                .map(Duration::getSeconds)
                .defaultIfEmpty(-2L);
    }

    /**
     * 키 삭제
     *
     * @param key Redis Key
     * @return Mono&lt;Boolean&gt; 삭제 성공 여부
     */
    public Mono<Boolean> delete(String key) {
        return reactiveStringRedisTemplate.delete(key).map(count -> count > 0);
    }

    /**
     * 키 존재 여부 확인
     *
     * @param key Redis Key
     * @return Mono&lt;Boolean&gt; 존재 여부
     */
    public Mono<Boolean> exists(String key) {
        return reactiveStringRedisTemplate.hasKey(key);
    }
}
