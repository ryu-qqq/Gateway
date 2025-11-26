package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.query.AccountLockQueryPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 계정 잠금 Query Adapter
 *
 * <p>AccountLockQueryPort 구현체 (Redis)
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
@Component
public class AccountLockQueryAdapter implements AccountLockQueryPort {

    private final AccountLockRedisRepository accountLockRedisRepository;

    public AccountLockQueryAdapter(AccountLockRedisRepository accountLockRedisRepository) {
        this.accountLockRedisRepository = accountLockRedisRepository;
    }

    /**
     * 계정 잠금 여부 조회
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 잠금 여부
     */
    @Override
    public Mono<Boolean> isLocked(String userId) {
        return accountLockRedisRepository.isLocked(userId);
    }

    /**
     * 잠금 남은 시간 조회 (초)
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Long&gt; 남은 시간 (초)
     */
    @Override
    public Mono<Long> getLockTtlSeconds(String userId) {
        return accountLockRedisRepository.getLockTtl(userId);
    }
}
