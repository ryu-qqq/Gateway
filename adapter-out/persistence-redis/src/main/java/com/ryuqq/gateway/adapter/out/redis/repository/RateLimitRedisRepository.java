package com.ryuqq.gateway.adapter.out.redis.repository;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public RateLimitRedisRepository(
            @Qualifier("reactiveStringRedisTemplate")
                    ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * 카운터 증가 및 TTL 설정
     *
     * <p>INCR 실행 후 TTL이 설정되지 않은 경우에만 EXPIRE 설정 (최초 1회)
     *
     * @param key Redis Key
     * @param ttl TTL (Duration)
     * @return Mono&lt;Long&gt; 증가 후 카운트 값
     */
    public Mono<Long> incrementAndExpire(String key, Duration ttl) {
        return reactiveStringRedisTemplate
                .opsForValue()
                .increment(key)
                .flatMap(
                        count -> {
                            if (count == 1L) {
                                // 최초 증가 시에만 TTL 설정
                                return reactiveStringRedisTemplate
                                        .expire(key, ttl)
                                        .thenReturn(count);
                            }
                            return Mono.just(count);
                        });
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
     * @return Mono&lt;Long&gt; 남은 TTL (초, 없으면 -2, TTL 없으면 -1)
     */
    public Mono<Long> getTtl(String key) {
        return reactiveStringRedisTemplate.getExpire(key).map(Duration::getSeconds);
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
