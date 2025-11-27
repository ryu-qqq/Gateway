package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.application.authentication.port.out.command.RedisLockCommandPort;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Redis Lock Command Adapter
 *
 * <p>RedisLockCommandPort 구현체 (Redisson)
 *
 * <p><strong>Lock 전략</strong>:
 *
 * <ul>
 *   <li>Wait Time: 0초 (즉시 실패 - 동시 요청 거부)
 *   <li>Lease Time: 10초 (자동 해제 - 데드락 방지)
 * </ul>
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code tenant:{tenantId}:refresh:lock:{userId}}
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RedisLockCommandAdapter implements RedisLockCommandPort {

    private static final Logger log = LoggerFactory.getLogger(RedisLockCommandAdapter.class);

    private static final String KEY_PREFIX = "tenant";
    private static final String KEY_SUFFIX = "refresh:lock";
    private static final long WAIT_TIME_SECONDS = 0L;
    private static final long LEASE_TIME_SECONDS = 10L;

    private final RedissonReactiveClient redissonReactiveClient;

    public RedisLockCommandAdapter(RedissonReactiveClient redissonReactiveClient) {
        this.redissonReactiveClient = redissonReactiveClient;
    }

    /**
     * Lock 획득 시도
     *
     * <p>Wait Time 0초로 즉시 결과 반환 (Non-blocking)
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Boolean&gt; Lock 획득 성공 여부
     */
    @Override
    public Mono<Boolean> tryLock(String tenantId, Long userId) {
        String lockKey = buildKey(tenantId, userId);
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return lock.tryLock(WAIT_TIME_SECONDS, LEASE_TIME_SECONDS, TimeUnit.SECONDS)
                .doOnNext(
                        acquired -> {
                            if (acquired) {
                                log.debug("Lock acquired: {}", lockKey);
                            } else {
                                log.debug("Lock acquisition failed: {}", lockKey);
                            }
                        })
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Lock acquisition error for key {}: {}",
                                    lockKey,
                                    e.getMessage());
                            return Mono.just(false);
                        });
    }

    /**
     * Lock 해제
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> unlock(String tenantId, Long userId) {
        String lockKey = buildKey(tenantId, userId);
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return lock.forceUnlock()
                .doOnSuccess(
                        released -> {
                            if (released) {
                                log.debug("Lock released: {}", lockKey);
                            }
                        })
                .then()
                .onErrorResume(
                        e -> {
                            log.warn("Lock release error for key {}: {}", lockKey, e.getMessage());
                            return Mono.empty();
                        });
    }

    /**
     * Redis Lock Key 생성
     *
     * <p>Format: tenant:{tenantId}:refresh:lock:{userId}
     *
     * @param tenantId Tenant 식별자
     * @param userId 사용자 식별자
     * @return Redis Lock Key
     */
    private String buildKey(String tenantId, Long userId) {
        return KEY_PREFIX + ":" + tenantId + ":" + KEY_SUFFIX + ":" + userId;
    }
}
