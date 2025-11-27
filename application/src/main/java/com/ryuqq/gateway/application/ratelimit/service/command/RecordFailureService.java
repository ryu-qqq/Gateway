package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitPolicy;
import java.time.Duration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 실패 기록 Service (RecordFailureUseCase 구현체)
 *
 * <p>로그인 실패, JWT 검증 실패 등의 실패를 기록하는 UseCase 구현체
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>실패 카운터 증가
 *   <li>임계값 초과 시 IP 차단 처리
 * </ol>
 *
 * <p><strong>차단 정책</strong>:
 *
 * <ul>
 *   <li>LOGIN: 5회 실패 시 IP 30분 차단
 *   <li>INVALID_JWT: 10회 실패 시 IP 30분 차단
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RecordFailureService implements RecordFailureUseCase {

    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofMinutes(30);

    private final RateLimitCounterCommandPort rateLimitCounterCommandPort;
    private final IpBlockCommandPort ipBlockCommandPort;

    /** 생성자 (Lombok 금지) */
    public RecordFailureService(
            RateLimitCounterCommandPort rateLimitCounterCommandPort,
            IpBlockCommandPort ipBlockCommandPort) {
        this.rateLimitCounterCommandPort = rateLimitCounterCommandPort;
        this.ipBlockCommandPort = ipBlockCommandPort;
    }

    /**
     * 실패 기록 실행
     *
     * @param command RecordFailureCommand
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> execute(RecordFailureCommand command) {
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(command.limitType());
        RateLimitKey key = RateLimitKey.of(command.limitType(), command.identifier());

        return rateLimitCounterCommandPort
                .incrementAndGet(key, policy.getWindow())
                .flatMap(currentCount -> handleExceedIfNecessary(currentCount, policy, command))
                .then();
    }

    /** 임계값 초과 시 IP 차단 처리 */
    private Mono<Void> handleExceedIfNecessary(
            long currentCount, RateLimitPolicy policy, RecordFailureCommand command) {
        if (policy.isExceeded(currentCount)) {
            return blockIpIfRequired(command);
        }
        return Mono.empty();
    }

    /** IP 차단 처리 (LOGIN, INVALID_JWT) */
    private Mono<Void> blockIpIfRequired(RecordFailureCommand command) {
        if (isIpBlockRequired(command.limitType())) {
            return ipBlockCommandPort.block(command.identifier(), DEFAULT_BLOCK_DURATION).then();
        }
        return Mono.empty();
    }

    /** IP 차단이 필요한 LimitType 여부 */
    private boolean isIpBlockRequired(LimitType limitType) {
        return limitType == LimitType.LOGIN || limitType == LimitType.INVALID_JWT;
    }
}
