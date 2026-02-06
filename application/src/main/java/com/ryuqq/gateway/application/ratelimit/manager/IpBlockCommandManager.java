package com.ryuqq.gateway.application.ratelimit.manager;

import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * IP Block Command Manager (Reactive)
 *
 * <p>IP 차단 및 해제를 담당하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 (TTL 포함)
 *   <li>IP 차단 해제
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>IpBlockCommandPort - Redis 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class IpBlockCommandManager {

    private final IpBlockCommandPort ipBlockCommandPort;

    public IpBlockCommandManager(IpBlockCommandPort ipBlockCommandPort) {
        this.ipBlockCommandPort = ipBlockCommandPort;
    }

    /**
     * IP 차단
     *
     * @param ipAddress IP 주소
     * @param duration 차단 기간
     * @return Mono&lt;Boolean&gt; 차단 성공 여부
     */
    public Mono<Boolean> block(String ipAddress, Duration duration) {
        return ipBlockCommandPort.block(ipAddress, duration);
    }

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    public Mono<Boolean> unblock(String ipAddress) {
        return ipBlockCommandPort.unblock(ipAddress);
    }
}
