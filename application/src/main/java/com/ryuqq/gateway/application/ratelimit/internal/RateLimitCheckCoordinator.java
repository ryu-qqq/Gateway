package com.ryuqq.gateway.application.ratelimit.internal;

import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockQueryManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import java.time.Duration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Rate Limit Check Coordinator
 *
 * <p>Rate Limit 체크 로직을 조정하는 내부 컴포넌트
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 여부 선제 확인 (IP 기반 타입인 경우)
 *   <li>카운터 증가 및 결과 확인
 *   <li>허용/거부 Response 생성
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitCheckCoordinator {

    private static final int DEFAULT_IP_LIMIT = 100;
    private static final int DEFAULT_USER_LIMIT = 100;
    private static final int DEFAULT_LOGIN_LIMIT = 5;
    private static final int DEFAULT_ENDPOINT_LIMIT = 1000;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final RateLimitCounterCommandManager rateLimitCounterCommandManager;
    private final IpBlockQueryManager ipBlockQueryManager;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitCheckCoordinator(
            RateLimitCounterCommandManager rateLimitCounterCommandManager,
            IpBlockQueryManager ipBlockQueryManager,
            RateLimitProperties rateLimitProperties) {
        this.rateLimitCounterCommandManager = rateLimitCounterCommandManager;
        this.ipBlockQueryManager = ipBlockQueryManager;
        this.rateLimitProperties = rateLimitProperties;
    }

    /**
     * Rate Limit 체크
     *
     * @param command Rate Limit 체크 요청
     * @return Mono&lt;CheckRateLimitResponse&gt; 체크 결과
     */
    public Mono<CheckRateLimitResponse> check(CheckRateLimitCommand command) {
        LimitType limitType = command.limitType();

        // IP 기반 타입인 경우 먼저 IP 차단 여부 확인
        if (limitType.isIpBased()) {
            return checkIpBlockedFirst(command);
        }

        // 그 외 타입은 바로 Rate Limit 체크
        return performRateLimitCheck(command);
    }

    private Mono<CheckRateLimitResponse> checkIpBlockedFirst(CheckRateLimitCommand command) {
        return ipBlockQueryManager
                .isBlocked(command.identifier())
                .flatMap(
                        blocked -> {
                            if (blocked) {
                                return ipBlockQueryManager
                                        .getBlockTtlSeconds(command.identifier())
                                        .flatMap(
                                                ttl ->
                                                        Mono.error(
                                                                new IpBlockedException(
                                                                        command.identifier(),
                                                                        ttl.intValue())));
                            }
                            return performRateLimitCheck(command);
                        });
    }

    private Mono<CheckRateLimitResponse> performRateLimitCheck(CheckRateLimitCommand command) {
        LimitType limitType = command.limitType();
        int limit = getLimit(limitType);
        int windowSeconds = getWindowSeconds();
        Duration window = Duration.ofSeconds(windowSeconds);

        RateLimitKey key = buildRateLimitKey(command);

        return rateLimitCounterCommandManager
                .incrementAndGet(key, window)
                .flatMap(
                        currentCount -> {
                            if (currentCount < limit) {
                                return Mono.just(
                                        CheckRateLimitResponse.allowed(currentCount, limit));
                            }

                            // Rate Limit 초과
                            RateLimitAction action = limitType.getDefaultAction();

                            // LOGIN, INVALID_JWT는 예외 발생
                            if (action == RateLimitAction.BLOCK_IP) {
                                return Mono.error(
                                        new RateLimitExceededException(limit, 0, windowSeconds));
                            }

                            // 그 외 타입은 denied Response 반환
                            return Mono.just(
                                    CheckRateLimitResponse.denied(
                                            currentCount,
                                            limit,
                                            windowSeconds,
                                            RateLimitAction.REJECT));
                        });
    }

    private RateLimitKey buildRateLimitKey(CheckRateLimitCommand command) {
        String[] keyParts;
        if (command.additionalKeyParts() != null && command.additionalKeyParts().length > 0) {
            keyParts = new String[command.additionalKeyParts().length + 1];
            keyParts[0] = command.identifier();
            System.arraycopy(
                    command.additionalKeyParts(),
                    0,
                    keyParts,
                    1,
                    command.additionalKeyParts().length);
        } else {
            keyParts = new String[] {command.identifier()};
        }
        return RateLimitKey.of(command.limitType(), keyParts);
    }

    private int getLimit(LimitType limitType) {
        return switch (limitType) {
            case ENDPOINT ->
                    rateLimitProperties.getEndpointLimit() != null
                            ? rateLimitProperties.getEndpointLimit()
                            : DEFAULT_ENDPOINT_LIMIT;
            case USER ->
                    rateLimitProperties.getUserLimit() != null
                            ? rateLimitProperties.getUserLimit()
                            : DEFAULT_USER_LIMIT;
            case IP ->
                    rateLimitProperties.getIpLimit() != null
                            ? rateLimitProperties.getIpLimit()
                            : DEFAULT_IP_LIMIT;
            case LOGIN ->
                    rateLimitProperties.getLoginLimit() != null
                            ? rateLimitProperties.getLoginLimit()
                            : DEFAULT_LOGIN_LIMIT;
            case OTP ->
                    rateLimitProperties.getOtpLimit() != null
                            ? rateLimitProperties.getOtpLimit()
                            : limitType.getDefaultMaxRequests();
            case TOKEN_REFRESH ->
                    rateLimitProperties.getTokenRefreshLimit() != null
                            ? rateLimitProperties.getTokenRefreshLimit()
                            : limitType.getDefaultMaxRequests();
            case INVALID_JWT ->
                    rateLimitProperties.getInvalidJwtLimit() != null
                            ? rateLimitProperties.getInvalidJwtLimit()
                            : limitType.getDefaultMaxRequests();
        };
    }

    private int getWindowSeconds() {
        return rateLimitProperties.getWindowSeconds() != null
                ? rateLimitProperties.getWindowSeconds()
                : DEFAULT_WINDOW_SECONDS;
    }
}
