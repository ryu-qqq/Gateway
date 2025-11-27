package com.ryuqq.gateway.application.authentication.port.out.command;

import reactor.core.publisher.Mono;

/**
 * Redis Lock Command Port (Outbound)
 *
 * <p>분산 Lock 관리를 담당하는 Outbound Port
 *
 * <p><strong>Redis Key 패턴</strong>:
 * {@code tenant:{tenantId}:refresh:lock:{userId}}
 *
 * <p><strong>사용 시점</strong>:
 *
 * <ul>
 *   <li>Token Refresh 시 Race Condition 방지
 *   <li>동시 요청 직렬화
 * </ul>
 *
 * <p><strong>Lock 전략</strong>:
 *
 * <ul>
 *   <li>Wait Time: 0초 (즉시 실패 - 동시 요청 거부)
 *   <li>Lease Time: 10초 (자동 해제 - 데드락 방지)
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RedisLockAdapter (adapter-out.persistence-redis) - Redisson 사용
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RedisLockCommandPort {

    /**
     * Lock 획득 시도
     *
     * <p>Wait Time 0초로 즉시 결과 반환 (Non-blocking)
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Boolean&gt; Lock 획득 성공 여부
     */
    Mono<Boolean> tryLock(String tenantId, Long userId);

    /**
     * Lock 해제
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> unlock(String tenantId, Long userId);
}
