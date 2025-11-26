package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 카운터 Command Adapter
 *
 * <p>RateLimitCounterCommandPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>카운터 증가 및 TTL 설정 (원자적 연산)
 *   <li>카운터 삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitCounterCommandAdapter implements RateLimitCounterCommandPort {

    private final RateLimitRedisRepository rateLimitRedisRepository;

    public RateLimitCounterCommandAdapter(RateLimitRedisRepository rateLimitRedisRepository) {
        this.rateLimitRedisRepository = rateLimitRedisRepository;
    }

    /**
     * 카운터 증가 및 TTL 설정
     *
     * @param key Rate Limit Key
     * @param window 시간 윈도우 (TTL)
     * @return Mono&lt;Long&gt; 증가 후 카운트 값
     */
    @Override
    public Mono<Long> incrementAndGet(RateLimitKey key, Duration window) {
        return rateLimitRedisRepository.incrementAndExpire(key.getValue(), window);
    }

    /**
     * 카운터 삭제
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Boolean&gt; 삭제 성공 여부
     */
    @Override
    public Mono<Boolean> delete(RateLimitKey key) {
        return rateLimitRedisRepository.delete(key.getValue());
    }
}
