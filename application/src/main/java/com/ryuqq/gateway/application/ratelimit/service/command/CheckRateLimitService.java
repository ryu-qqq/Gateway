package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.internal.RateLimitCheckCoordinator;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.observability.logging.annotation.Loggable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 체크 Service
 *
 * <p>RateLimitCheckCoordinator를 통해 Rate Limit 체크를 수행하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * CheckRateLimitService (Application Service - UseCase 구현)
 *   ↓ (calls)
 * RateLimitCheckCoordinator (Application Coordinator - internal)
 *   ↓ (calls)
 * Manager → Port
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class CheckRateLimitService implements CheckRateLimitUseCase {

    private final RateLimitCheckCoordinator rateLimitCheckCoordinator;

    public CheckRateLimitService(RateLimitCheckCoordinator rateLimitCheckCoordinator) {
        this.rateLimitCheckCoordinator = rateLimitCheckCoordinator;
    }

    /**
     * Rate Limit 체크 실행
     *
     * @param command CheckRateLimitCommand
     * @return Mono&lt;CheckRateLimitResponse&gt;
     */
    @Loggable(value = "Rate Limit 체크", includeArgs = false, slowThreshold = 100)
    @Override
    public Mono<CheckRateLimitResponse> execute(CheckRateLimitCommand command) {
        return rateLimitCheckCoordinator.check(command);
    }
}
