package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.internal.RateLimitResetCoordinator;
import com.ryuqq.gateway.application.ratelimit.port.in.command.ResetRateLimitUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 리셋 Service
 *
 * <p>RateLimitResetCoordinator를 통해 Rate Limit 리셋을 수행하는 서비스 (Admin 전용)
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * ResetRateLimitService (Application Service - UseCase 구현)
 *   ↓ (calls)
 * RateLimitResetCoordinator (Application Coordinator - internal)
 *   ↓ (calls)
 * Manager → Port
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class ResetRateLimitService implements ResetRateLimitUseCase {

    private final RateLimitResetCoordinator rateLimitResetCoordinator;

    public ResetRateLimitService(RateLimitResetCoordinator rateLimitResetCoordinator) {
        this.rateLimitResetCoordinator = rateLimitResetCoordinator;
    }

    /**
     * Rate Limit 리셋 실행
     *
     * @param command ResetRateLimitCommand
     * @return Mono&lt;Void&gt;
     */
    @Override
    public Mono<Void> execute(ResetRateLimitCommand command) {
        return rateLimitResetCoordinator.reset(command);
    }
}
