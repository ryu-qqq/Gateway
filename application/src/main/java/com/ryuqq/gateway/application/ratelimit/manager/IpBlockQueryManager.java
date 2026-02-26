package com.ryuqq.gateway.application.ratelimit.manager;

import com.ryuqq.gateway.application.ratelimit.port.out.query.IpBlockQueryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * IP Block Query Manager (Reactive)
 *
 * <p>IP 차단 여부 조회를 담당하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 여부 조회
 *   <li>차단 남은 시간 조회
 *   <li>차단된 IP 목록 조회
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>IpBlockQueryPort - Redis 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class IpBlockQueryManager {

    private final IpBlockQueryPort ipBlockQueryPort;

    public IpBlockQueryManager(IpBlockQueryPort ipBlockQueryPort) {
        this.ipBlockQueryPort = ipBlockQueryPort;
    }

    /**
     * IP 차단 여부 조회
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 차단 여부
     */
    public Mono<Boolean> isBlocked(String ipAddress) {
        return ipBlockQueryPort.isBlocked(ipAddress);
    }

    /**
     * 차단 남은 시간 조회 (초)
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Long&gt; 남은 시간 (초)
     */
    public Mono<Long> getBlockTtlSeconds(String ipAddress) {
        return ipBlockQueryPort.getBlockTtlSeconds(ipAddress);
    }

    /**
     * 모든 차단된 IP 목록 조회
     *
     * @return Flux&lt;String&gt; 차단된 IP 주소 목록
     */
    public Flux<String> findAllBlockedIps() {
        return ipBlockQueryPort.findAllBlockedIps();
    }

    /**
     * 모든 차단된 IP 목록과 TTL을 함께 조회
     *
     * @return Flux&lt;BlockedIpWithTtl&gt; IP 주소와 TTL 정보
     */
    public Flux<IpBlockQueryPort.BlockedIpWithTtl> findAllBlockedIpsWithTtl() {
        return ipBlockQueryPort.findAllBlockedIpsWithTtl();
    }
}
