package com.ryuqq.gateway.application.ratelimit.manager;

import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit Counter Command Manager (Reactive)
 *
 * <p>Rate Limit 카운터 증가 및 삭제를 담당하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>카운터 증가 (INCR + TTL 설정)
 *   <li>카운터 삭제 (리셋)
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>RateLimitCounterCommandPort - Redis 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitCounterCommandManager {

    private final RateLimitCounterCommandPort rateLimitCounterCommandPort;

    public RateLimitCounterCommandManager(RateLimitCounterCommandPort rateLimitCounterCommandPort) {
        this.rateLimitCounterCommandPort = rateLimitCounterCommandPort;
    }

    /**
     * 카운터 증가 및 TTL 설정
     *
     * @param key Rate Limit Key
     * @param window 시간 윈도우 (TTL)
     * @return Mono&lt;Long&gt; 증가 후 카운트 값
     */
    public Mono<Long> incrementAndGet(RateLimitKey key, Duration window) {
        return rateLimitCounterCommandPort.incrementAndGet(key, window);
    }

    /**
     * 카운터 삭제 (리셋)
     *
     * @param key Rate Limit Key
     * @return Mono&lt;Boolean&gt; 삭제 성공 여부
     */
    public Mono<Boolean> delete(RateLimitKey key) {
        return rateLimitCounterCommandPort.delete(key);
    }
}
