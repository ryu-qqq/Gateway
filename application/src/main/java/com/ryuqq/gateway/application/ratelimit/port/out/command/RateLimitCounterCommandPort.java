package com.ryuqq.gateway.application.ratelimit.port.out.command;

import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 카운터 Command Port
 *
 * <p>Rate Limit 카운터 증가 및 삭제를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RateLimitCounterCommandAdapter (Redis)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>카운터 증가 (INCR + TTL 설정)
 *   <li>카운터 삭제 (리셋)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RateLimitCounterCommandPort {

    /**
     * 카운터 증가 및 TTL 설정
     *
     * <p>Redis INCR 실행 후 TTL 설정
     *
     * @param key Rate Limit Key
     * @param window 시간 윈도우 (TTL)
     * @return Mono&lt;Long&gt; 증가 후 카운트 값
     */
    Mono<Long> incrementAndGet(RateLimitKey key, Duration window);

    /**
     * 카운터 삭제 (리셋)
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Boolean&gt; 삭제 성공 여부
     */
    Mono<Boolean> delete(RateLimitKey key);
}
