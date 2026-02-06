package com.ryuqq.gateway.application.ratelimit.internal;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Failure Record Coordinator
 *
 * <p>실패 기록 및 IP 차단 로직을 조정하는 내부 컴포넌트
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>실패 카운터 증가
 *   <li>임계값 초과 시 IP 차단 처리
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class FailureRecordCoordinator {

    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);

    private final RateLimitCounterCommandManager rateLimitCounterCommandManager;
    private final IpBlockCommandManager ipBlockCommandManager;

    public FailureRecordCoordinator(
            RateLimitCounterCommandManager rateLimitCounterCommandManager,
            IpBlockCommandManager ipBlockCommandManager) {
        this.rateLimitCounterCommandManager = rateLimitCounterCommandManager;
        this.ipBlockCommandManager = ipBlockCommandManager;
    }

    /**
     * 실패 기록
     *
     * @param command 실패 기록 요청
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> record(RecordFailureCommand command) {
        LimitType limitType = command.limitType();
        RateLimitKey key = RateLimitKey.of(limitType, command.identifier());
        Duration window = limitType.getDefaultWindow();

        return rateLimitCounterCommandManager
                .incrementAndGet(key, window)
                .flatMap(
                        count -> {
                            // 임계값 초과 시 IP 차단 필요 여부 확인
                            if (limitType.requiresIpBlock()
                                    && count >= limitType.getFailureThreshold()) {
                                return ipBlockCommandManager
                                        .block(command.identifier(), BLOCK_DURATION)
                                        .then();
                            }

                            return Mono.empty();
                        });
    }
}
