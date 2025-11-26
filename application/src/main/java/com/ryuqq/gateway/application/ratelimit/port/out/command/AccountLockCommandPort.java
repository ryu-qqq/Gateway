package com.ryuqq.gateway.application.ratelimit.port.out.command;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * 계정 잠금 Command Port
 *
 * <p>계정 잠금 및 해제를 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>AccountLockCommandAdapter (Redis)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>계정 잠금 (SET + TTL)
 *   <li>계정 잠금 해제 (DELETE)
 * </ul>
 *
 * <p><strong>Redis Key 형식</strong>:
 *
 * <pre>gateway:locked_account:{userId}</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface AccountLockCommandPort {

    /**
     * 계정 잠금
     *
     * @param userId 사용자 ID
     * @param duration 잠금 기간
     * @return Mono&lt;Boolean&gt; 잠금 성공 여부
     */
    Mono<Boolean> lock(String userId, Duration duration);

    /**
     * 계정 잠금 해제
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    Mono<Boolean> unlock(String userId);
}
