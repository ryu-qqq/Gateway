package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.query.IpBlockQueryPort;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitPolicy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 체크 Service (CheckRateLimitUseCase 구현체)
 *
 * <p>요청에 대한 Rate Limit을 체크하는 UseCase 구현체
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>IP 차단 여부 확인 (선제 차단)
 *   <li>Rate Limit Policy 결정 (LimitType 기반)
 *   <li>카운터 증가 및 결과 확인
 *   <li>허용/거부 Response 생성
 * </ol>
 *
 * <p><strong>Zero-Tolerance 준수</strong>:
 *
 * <ul>
 *   <li>Transaction 불필요 (Redis 원자적 연산)
 *   <li>Lombok 금지
 *   <li>비즈니스 로직은 Domain에 위임
 *   <li>Reactive Programming (Mono/Flux)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class CheckRateLimitService implements CheckRateLimitUseCase {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 60;

    private final RateLimitCounterCommandPort rateLimitCounterCommandPort;
    private final IpBlockQueryPort ipBlockQueryPort;

    /** 생성자 (Lombok 금지) */
    public CheckRateLimitService(
            RateLimitCounterCommandPort rateLimitCounterCommandPort,
            IpBlockQueryPort ipBlockQueryPort) {
        this.rateLimitCounterCommandPort = rateLimitCounterCommandPort;
        this.ipBlockQueryPort = ipBlockQueryPort;
    }

    /**
     * Rate Limit 체크 실행
     *
     * @param command CheckRateLimitCommand
     * @return Mono&lt;CheckRateLimitResponse&gt; (체크 결과)
     */
    @Override
    public Mono<CheckRateLimitResponse> execute(CheckRateLimitCommand command) {
        return checkIpBlockedFirst(command).flatMap(this::processRateLimitCheck);
    }

    /**
     * IP 차단 여부 선제 확인
     *
     * <p>IP 기반 Rate Limit (IP, LOGIN, INVALID_JWT)의 경우 먼저 차단 여부 확인
     */
    private Mono<CheckRateLimitCommand> checkIpBlockedFirst(CheckRateLimitCommand command) {
        if (isIpBasedLimitType(command.limitType())) {
            return ipBlockQueryPort
                    .isBlocked(command.identifier())
                    .flatMap(
                            blocked -> {
                                if (blocked) {
                                    return ipBlockQueryPort
                                            .getBlockTtlSeconds(command.identifier())
                                            .flatMap(
                                                    ttl ->
                                                            Mono.error(
                                                                    new IpBlockedException(
                                                                            command.identifier(),
                                                                            ttl.intValue())));
                                }
                                return Mono.just(command);
                            });
        }
        return Mono.just(command);
    }

    /** IP 기반 LimitType 여부 확인 */
    private boolean isIpBasedLimitType(LimitType limitType) {
        return limitType == LimitType.IP
                || limitType == LimitType.LOGIN
                || limitType == LimitType.INVALID_JWT;
    }

    /** Rate Limit 체크 처리 */
    private Mono<CheckRateLimitResponse> processRateLimitCheck(CheckRateLimitCommand command) {
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(command.limitType());
        RateLimitKey key = buildKey(command);

        return rateLimitCounterCommandPort
                .incrementAndGet(key, policy.getWindow())
                .map(currentCount -> buildResponse(currentCount, policy));
    }

    /** Rate Limit Key 생성 */
    private RateLimitKey buildKey(CheckRateLimitCommand command) {
        String[] allKeyParts = buildKeyParts(command);
        return RateLimitKey.of(command.limitType(), allKeyParts);
    }

    /** Key 구성 요소 배열 생성 */
    private String[] buildKeyParts(CheckRateLimitCommand command) {
        String[] additionalParts = command.additionalKeyParts();
        String[] allKeyParts = new String[1 + additionalParts.length];
        allKeyParts[0] = command.identifier();
        System.arraycopy(additionalParts, 0, allKeyParts, 1, additionalParts.length);
        return allKeyParts;
    }

    /** Response 생성 */
    private CheckRateLimitResponse buildResponse(long currentCount, RateLimitPolicy policy) {
        if (policy.isExceeded(currentCount)) {
            return buildDeniedResponse(currentCount, policy);
        }
        return CheckRateLimitResponse.allowed(currentCount, policy.getMaxRequests());
    }

    /** 거부 Response 생성 */
    private CheckRateLimitResponse buildDeniedResponse(long currentCount, RateLimitPolicy policy) {
        int retryAfterSeconds = calculateRetryAfterSeconds(policy);
        RateLimitAction action = policy.getAction();

        // 예외 발생 (Filter에서 처리)
        if (action == RateLimitAction.BLOCK_IP || action == RateLimitAction.LOCK_ACCOUNT) {
            throw new RateLimitExceededException(policy.getMaxRequests(), 0, retryAfterSeconds);
        }

        return CheckRateLimitResponse.denied(
                currentCount, policy.getMaxRequests(), retryAfterSeconds, action);
    }

    /** 재시도 가능 시간 계산 */
    private int calculateRetryAfterSeconds(RateLimitPolicy policy) {
        long windowSeconds = policy.getWindow().getSeconds();
        return windowSeconds > 0 ? (int) windowSeconds : DEFAULT_RETRY_AFTER_SECONDS;
    }
}
