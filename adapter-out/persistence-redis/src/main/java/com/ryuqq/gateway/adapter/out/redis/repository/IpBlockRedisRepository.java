package com.ryuqq.gateway.adapter.out.redis.repository;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * IP 차단 Redis Repository
 *
 * <p>Redis에 차단된 IP를 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code gateway:blocked_ip:{ipAddress}}
 *   <li>Value: "blocked"
 *   <li>TTL: 차단 기간 (기본 30분)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class IpBlockRedisRepository {

    private static final String IP_BLOCK_PREFIX = "gateway:blocked_ip";
    private static final String BLOCKED_VALUE = "blocked";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public IpBlockRedisRepository(
            @Qualifier("reactiveStringRedisTemplate")
                    ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * IP 차단
     *
     * @param ipAddress IP 주소
     * @param ttl 차단 기간
     * @return Mono&lt;Boolean&gt; 차단 성공 여부
     */
    public Mono<Boolean> block(String ipAddress, Duration ttl) {
        String key = buildKey(ipAddress);
        return reactiveStringRedisTemplate.opsForValue().set(key, BLOCKED_VALUE, ttl);
    }

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    public Mono<Boolean> unblock(String ipAddress) {
        String key = buildKey(ipAddress);
        return reactiveStringRedisTemplate.delete(key).map(count -> count > 0);
    }

    /**
     * IP 차단 여부 확인
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 차단 여부
     */
    public Mono<Boolean> isBlocked(String ipAddress) {
        String key = buildKey(ipAddress);
        return reactiveStringRedisTemplate.hasKey(key);
    }

    /**
     * 차단 남은 시간 조회 (초)
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Long&gt; 남은 시간 (초)
     */
    public Mono<Long> getBlockTtl(String ipAddress) {
        String key = buildKey(ipAddress);
        return reactiveStringRedisTemplate
                .getExpire(key)
                .map(Duration::getSeconds)
                .defaultIfEmpty(-2L);
    }

    /**
     * Redis Key 생성
     *
     * @param ipAddress IP 주소
     * @return Redis Key
     */
    private String buildKey(String ipAddress) {
        return IP_BLOCK_PREFIX + ":" + ipAddress;
    }
}
