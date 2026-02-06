package com.ryuqq.gateway.application.ratelimit.manager;

import com.ryuqq.gateway.application.ratelimit.port.out.query.RateLimitCounterQueryPort;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit Counter Query Manager (Reactive)
 *
 * <p>Rate Limit 카운터 조회를 담당하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>현재 카운터 값 조회
 *   <li>남은 TTL 조회
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>RateLimitCounterQueryPort - Redis 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitCounterQueryManager {

    private final RateLimitCounterQueryPort rateLimitCounterQueryPort;

    public RateLimitCounterQueryManager(RateLimitCounterQueryPort rateLimitCounterQueryPort) {
        this.rateLimitCounterQueryPort = rateLimitCounterQueryPort;
    }

    /**
     * 현재 카운터 값 조회
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 현재 카운트 값 (존재하지 않으면 0)
     */
    public Mono<Long> getCurrentCount(RateLimitKey key) {
        return rateLimitCounterQueryPort.getCurrentCount(key);
    }

    /**
     * 남은 TTL 조회 (초)
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Long&gt; 남은 TTL (초)
     */
    public Mono<Long> getTtlSeconds(RateLimitKey key) {
        return rateLimitCounterQueryPort.getTtlSeconds(key);
    }
}
