package com.ryuqq.gateway.application.ratelimit.manager;

import com.ryuqq.gateway.application.ratelimit.port.out.command.AccountLockCommandPort;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Account Lock Command Manager (Reactive)
 *
 * <p>계정 잠금 및 해제를 담당하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>계정 잠금 (TTL 포함)
 *   <li>계정 잠금 해제
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>AccountLockCommandPort - Redis 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AccountLockCommandManager {

    private final AccountLockCommandPort accountLockCommandPort;

    public AccountLockCommandManager(AccountLockCommandPort accountLockCommandPort) {
        this.accountLockCommandPort = accountLockCommandPort;
    }

    /**
     * 계정 잠금
     *
     * @param userId 사용자 ID
     * @param duration 잠금 기간
     * @return Mono&lt;Boolean&gt; 잠금 성공 여부
     */
    public Mono<Boolean> lock(String userId, Duration duration) {
        return accountLockCommandPort.lock(userId, duration);
    }

    /**
     * 계정 잠금 해제
     *
     * @param userId 사용자 ID
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    public Mono<Boolean> unlock(String userId) {
        return accountLockCommandPort.unlock(userId);
    }
}
