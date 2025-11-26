package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.IpBlockRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.query.IpBlockQueryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * IP 차단 Query Adapter
 *
 * <p>IpBlockQueryPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 여부 조회
 *   <li>차단 남은 시간 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class IpBlockQueryAdapter implements IpBlockQueryPort {

    private final IpBlockRedisRepository ipBlockRedisRepository;

    public IpBlockQueryAdapter(IpBlockRedisRepository ipBlockRedisRepository) {
        this.ipBlockRedisRepository = ipBlockRedisRepository;
    }

    /**
     * IP 차단 여부 조회
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 차단 여부
     */
    @Override
    public Mono<Boolean> isBlocked(String ipAddress) {
        return ipBlockRedisRepository.isBlocked(ipAddress);
    }

    /**
     * 차단 남은 시간 조회 (초)
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Long&gt; 남은 시간 (초)
     */
    @Override
    public Mono<Long> getBlockTtlSeconds(String ipAddress) {
        return ipBlockRedisRepository.getBlockTtl(ipAddress);
    }
}
