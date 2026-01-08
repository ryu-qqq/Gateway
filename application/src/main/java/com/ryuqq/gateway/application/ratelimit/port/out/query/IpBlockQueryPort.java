package com.ryuqq.gateway.application.ratelimit.port.out.query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * IP 차단 Query Port
 *
 * <p>IP 차단 여부 조회를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>IpBlockQueryAdapter (Redis)
 * </ul>
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
public interface IpBlockQueryPort {

    /**
     * IP 차단 여부 조회
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 차단 여부
     */
    Mono<Boolean> isBlocked(String ipAddress);

    /**
     * 차단 남은 시간 조회 (초)
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Long&gt; 남은 시간 (초, 차단되지 않았으면 -2)
     */
    Mono<Long> getBlockTtlSeconds(String ipAddress);

    /**
     * 모든 차단된 IP 목록 조회
     *
     * @return Flux&lt;String&gt; 차단된 IP 주소 목록
     */
    Flux<String> findAllBlockedIps();

    /**
     * 모든 차단된 IP 목록과 TTL을 함께 조회 (N+1 문제 방지)
     *
     * @return Flux&lt;BlockedIpWithTtl&gt; IP 주소와 TTL 정보
     */
    Flux<BlockedIpWithTtl> findAllBlockedIpsWithTtl();

    /**
     * IP와 TTL 정보를 담는 DTO
     *
     * @param ip IP 주소
     * @param ttlSeconds 남은 시간 (초)
     */
    record BlockedIpWithTtl(String ip, Long ttlSeconds) {}
}
