package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.ResetRateLimitUseCase;
import com.ryuqq.gateway.application.ratelimit.port.out.command.AccountLockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 리셋 Service (ResetRateLimitUseCase 구현체)
 *
 * <p>Rate Limit 카운터를 리셋하는 UseCase 구현체 (Admin 전용)
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>Rate Limit 카운터 삭제
 *   <li>IP 차단 해제 (IP 기반인 경우)
 *   <li>계정 잠금 해제 (User 기반인 경우)
 *   <li>Audit Log 기록
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class ResetRateLimitService implements ResetRateLimitUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResetRateLimitService.class);

    private final RateLimitCounterCommandPort rateLimitCounterCommandPort;
    private final IpBlockCommandPort ipBlockCommandPort;
    private final AccountLockCommandPort accountLockCommandPort;

    /** 생성자 (Lombok 금지) */
    public ResetRateLimitService(
            RateLimitCounterCommandPort rateLimitCounterCommandPort,
            IpBlockCommandPort ipBlockCommandPort,
            AccountLockCommandPort accountLockCommandPort) {
        this.rateLimitCounterCommandPort = rateLimitCounterCommandPort;
        this.ipBlockCommandPort = ipBlockCommandPort;
        this.accountLockCommandPort = accountLockCommandPort;
    }

    /**
     * Rate Limit 리셋 실행
     *
     * @param command ResetRateLimitCommand
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> execute(ResetRateLimitCommand command) {
        RateLimitKey key = RateLimitKey.of(command.limitType(), command.identifier());

        return rateLimitCounterCommandPort
                .delete(key)
                .flatMap(deleted -> unblockIfNecessary(command))
                .doOnSuccess(v -> logReset(command))
                .then();
    }

    /** IP 차단 또는 계정 잠금 해제 */
    private Mono<Boolean> unblockIfNecessary(ResetRateLimitCommand command) {
        LimitType limitType = command.limitType();
        String identifier = command.identifier();

        if (isIpBasedLimitType(limitType)) {
            return ipBlockCommandPort.unblock(identifier);
        }
        if (isUserBasedLimitType(limitType)) {
            return accountLockCommandPort.unlock(identifier);
        }
        return Mono.just(true);
    }

    /** IP 기반 LimitType 여부 */
    private boolean isIpBasedLimitType(LimitType limitType) {
        return limitType == LimitType.IP
                || limitType == LimitType.LOGIN
                || limitType == LimitType.INVALID_JWT;
    }

    /** User 기반 LimitType 여부 */
    private boolean isUserBasedLimitType(LimitType limitType) {
        return limitType == LimitType.USER || limitType == LimitType.TOKEN_REFRESH;
    }

    /** 리셋 로그 기록 */
    private void logReset(ResetRateLimitCommand command) {
        log.info(
                "Rate limit reset by admin. type={}, identifier={}, adminId={}",
                command.limitType(),
                command.identifier(),
                command.adminId());
    }
}
