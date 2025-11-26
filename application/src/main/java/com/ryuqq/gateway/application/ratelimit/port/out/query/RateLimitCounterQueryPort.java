package com.ryuqq.gateway.application.ratelimit.port.out.query;

import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 카운터 Query Port
 *
 * <p>Rate Limit 카운터 조회를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RateLimitCounterQueryAdapter (Redis)
 * </ul>
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
public interface RateLimitCounterQueryPort {

    /**
     * 현재 카운터 값 조회
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 현재 카운트 값 (존재하지 않으면 0)
     */
    Mono<Long> getCurrentCount(RateLimitKey key);

    /**
     * 남은 TTL 조회 (초)
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 남은 TTL (초, 존재하지 않으면 -2, TTL 없으면 -1)
     */
    Mono<Long> getTtlSeconds(RateLimitKey key);
}
