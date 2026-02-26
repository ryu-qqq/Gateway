package com.ryuqq.gateway.application.authentication.manager;

import com.ryuqq.gateway.application.authentication.port.out.command.RedisLockCommandPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Redis Lock Command Manager
 *
 * <p>RedisLockCommandPort를 래핑하는 Manager
 *
 * <p><strong>Lock 전략</strong>:
 *
 * <ul>
 *   <li>Wait Time: 0초 (즉시 실패 - 동시 요청 거부)
 *   <li>Lease Time: 10초 (자동 해제 - 데드락 방지)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RedisLockCommandManager {

    private final RedisLockCommandPort redisLockCommandPort;

    public RedisLockCommandManager(RedisLockCommandPort redisLockCommandPort) {
        this.redisLockCommandPort = redisLockCommandPort;
    }

    /**
     * Lock 획득 시도
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Boolean&gt; Lock 획득 성공 여부
     */
    public Mono<Boolean> tryLock(String tenantId, Long userId) {
        return redisLockCommandPort.tryLock(tenantId, userId);
    }

    /**
     * Lock 해제
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    public Mono<Void> unlock(String tenantId, Long userId) {
        return redisLockCommandPort.unlock(tenantId, userId);
    }
}
