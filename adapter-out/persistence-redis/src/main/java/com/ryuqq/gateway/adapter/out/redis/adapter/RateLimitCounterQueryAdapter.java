package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.query.RateLimitCounterQueryPort;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 카운터 Query Adapter
 *
 * <p>RateLimitCounterQueryPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>현재 카운터 값 조회
 *   <li>남은 TTL 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitCounterQueryAdapter implements RateLimitCounterQueryPort {

    private final RateLimitRedisRepository rateLimitRedisRepository;

    public RateLimitCounterQueryAdapter(RateLimitRedisRepository rateLimitRedisRepository) {
        this.rateLimitRedisRepository = rateLimitRedisRepository;
    }

    /**
     * 현재 카운터 값 조회
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 현재 카운트 값 (없으면 0)
     */
    @Override
    public Mono<Long> getCurrentCount(RateLimitKey key) {
        return rateLimitRedisRepository.getCount(key.getValue());
    }

    /**
     * 남은 TTL 조회 (초)
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 남은 TTL (초)
     */
    @Override
    public Mono<Long> getTtlSeconds(RateLimitKey key) {
        return rateLimitRedisRepository.getTtl(key.getValue());
    }
}
