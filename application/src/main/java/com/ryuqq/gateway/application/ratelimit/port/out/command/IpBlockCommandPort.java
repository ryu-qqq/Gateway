package com.ryuqq.gateway.application.ratelimit.port.out.command;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * IP 차단 Command Port
 *
 * <p>IP 차단 및 해제를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>IpBlockCommandAdapter (Redis)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 (SET + TTL)
 *   <li>IP 차단 해제 (DELETE)
 * </ul>
 *
 * <p><strong>Redis Key 형식</strong>:
 *
 * <pre>gateway:blocked_ip:{ipAddress}</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface IpBlockCommandPort {

    /**
     * IP 차단
     *
     * @param ipAddress IP 주소
     * @param duration 차단 기간
     * @return Mono&lt;Boolean&gt; 차단 성공 여부
     */
    Mono<Boolean> block(String ipAddress, Duration duration);

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    Mono<Boolean> unblock(String ipAddress);
}
