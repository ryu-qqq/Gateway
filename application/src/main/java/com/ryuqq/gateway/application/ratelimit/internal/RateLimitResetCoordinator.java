package com.ryuqq.gateway.application.ratelimit.internal;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.manager.AccountLockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit Reset Coordinator
 *
 * <p>Rate Limit 리셋 로직을 조정하는 내부 컴포넌트
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Rate Limit 카운터 삭제
 *   <li>IP 차단 해제 (IP 기반인 경우)
 *   <li>계정 잠금 해제 (User 기반인 경우)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitResetCoordinator {

    private final RateLimitCounterCommandManager rateLimitCounterCommandManager;
    private final IpBlockCommandManager ipBlockCommandManager;
    private final AccountLockCommandManager accountLockCommandManager;

    public RateLimitResetCoordinator(
            RateLimitCounterCommandManager rateLimitCounterCommandManager,
            IpBlockCommandManager ipBlockCommandManager,
            AccountLockCommandManager accountLockCommandManager) {
        this.rateLimitCounterCommandManager = rateLimitCounterCommandManager;
        this.ipBlockCommandManager = ipBlockCommandManager;
        this.accountLockCommandManager = accountLockCommandManager;
    }

    /**
     * Rate Limit 리셋
     *
     * @param command 리셋 요청
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> reset(ResetRateLimitCommand command) {
        LimitType limitType = command.limitType();
        RateLimitKey key = RateLimitKey.of(limitType, command.identifier());

        // 1. 카운터 삭제
        return rateLimitCounterCommandManager
                .delete(key)
                .flatMap(
                        deleted -> {
                            // 2. IP 기반인 경우 IP 차단 해제, User 기반인 경우 계정 잠금 해제
                            if (limitType.isIpBased()) {
                                return ipBlockCommandManager.unblock(command.identifier());
                            } else if (limitType.isUserBased()) {
                                return accountLockCommandManager.unlock(command.identifier());
                            }
                            return Mono.just(true);
                        })
                .then();
    }
}
