package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * IP 차단 Command Adapter
 *
 * <p>IpBlockCommandPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 (SET + TTL)
 *   <li>IP 차단 해제 (DELETE)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class IpBlockCommandAdapter implements IpBlockCommandPort {

    private final IpBlockRedisRepository ipBlockRedisRepository;

    public IpBlockCommandAdapter(IpBlockRedisRepository ipBlockRedisRepository) {
        this.ipBlockRedisRepository = ipBlockRedisRepository;
    }

    /**
     * IP 차단
     *
     * @param ipAddress IP 주소
     * @param duration 차단 기간
     * @return Mono&lt;Boolean&gt; 차단 성공 여부
     */
    @Override
    public Mono<Boolean> block(String ipAddress, Duration duration) {
        return ipBlockRedisRepository.block(ipAddress, duration);
    }

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    @Override
    public Mono<Boolean> unblock(String ipAddress) {
        return ipBlockRedisRepository.unblock(ipAddress);
    }
}
