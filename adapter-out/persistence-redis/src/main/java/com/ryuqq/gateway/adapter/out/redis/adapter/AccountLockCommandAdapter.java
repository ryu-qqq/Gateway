package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
import com.ryuqq.gateway.application.ratelimit.port.out.command.AccountLockCommandPort;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 계정 잠금 Command Adapter
 *
 * <p>AccountLockCommandPort 구현체 (Redis)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>계정 잠금 (SET + TTL)
 *   <li>계정 잠금 해제 (DELETE)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AccountLockCommandAdapter implements AccountLockCommandPort {

    private final AccountLockRedisRepository accountLockRedisRepository;

    public AccountLockCommandAdapter(AccountLockRedisRepository accountLockRedisRepository) {
        this.accountLockRedisRepository = accountLockRedisRepository;
    }

    /**
     * 계정 잠금
     *
     * @param userId 사용자 ID
     * @param duration 잠금 기간
     * @return Mono&lt;Boolean&gt; 잠금 성공 여부
     */
    @Override
    public Mono<Boolean> lock(String userId, Duration duration) {
        return accountLockRedisRepository.lock(userId, duration);
    }

    /**
     * 계정 잠금 해제
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    @Override
    public Mono<Boolean> unlock(String userId) {
        return accountLockRedisRepository.unlock(userId);
    }
}
