package com.ryuqq.gateway.application.ratelimit.port.out.query;

import reactor.core.publisher.Mono;

/**
 * 계정 잠금 Query Port
 *
 * <p>계정 잠금 여부 조회를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>AccountLockQueryAdapter (Redis)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>계정 잠금 여부 조회
 *   <li>잠금 남은 시간 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface AccountLockQueryPort {

    /**
     * 계정 잠금 여부 조회
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 잠금 여부
     */
    Mono<Boolean> isLocked(String userId);

    /**
     * 잠금 남은 시간 조회 (초)
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Long&gt; 남은 시간 (초, 잠금되지 않았으면 -2)
     */
    Mono<Long> getLockTtlSeconds(String userId);
}
