package com.ryuqq.gateway.application.ratelimit.internal;

import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
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

    /** 기본 IP 차단 기간 (Properties에 설정이 없을 경우 사용) */
    private static final int DEFAULT_BLOCK_DURATION_MINUTES = 30;

    private final RateLimitCounterCommandManager rateLimitCounterCommandManager;
    private final IpBlockCommandManager ipBlockCommandManager;
    private final RateLimitProperties rateLimitProperties;

    public FailureRecordCoordinator(
            RateLimitCounterCommandManager rateLimitCounterCommandManager,
            IpBlockCommandManager ipBlockCommandManager,
            RateLimitProperties rateLimitProperties) {
        this.rateLimitCounterCommandManager = rateLimitCounterCommandManager;
        this.ipBlockCommandManager = ipBlockCommandManager;
        this.rateLimitProperties = rateLimitProperties;
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
                            // IP 차단이 비활성화된 경우 스킵 (Stage 환경 등)
                            if (!rateLimitProperties.isIpBlockEnabled()) {
                                return Mono.empty();
                            }

                            // 임계값 초과 시 IP 차단 필요 여부 확인
                            int threshold = getFailureThreshold(limitType);
                            if (limitType.requiresIpBlock() && count >= threshold) {
                                Duration blockDuration = getBlockDuration(limitType);
                                return ipBlockCommandManager
                                        .block(command.identifier(), blockDuration)
                                        .then();
                            }

                            return Mono.empty();
                        });
    }

    /**
     * LimitType별 실패 임계값 반환
     *
     * <p>Properties에 설정된 값이 있으면 사용하고, 없으면 LimitType의 기본값 사용
     *
     * @param limitType 제한 타입
     * @return 실패 임계값
     */
    private int getFailureThreshold(LimitType limitType) {
        return switch (limitType) {
            case LOGIN ->
                    rateLimitProperties.getLoginFailureThreshold() != null
                            ? rateLimitProperties.getLoginFailureThreshold()
                            : limitType.getFailureThreshold();
            case INVALID_JWT ->
                    rateLimitProperties.getInvalidJwtFailureThreshold() != null
                            ? rateLimitProperties.getInvalidJwtFailureThreshold()
                            : limitType.getFailureThreshold();
            default -> limitType.getFailureThreshold();
        };
    }

    /**
     * LimitType별 IP 차단 기간 반환
     *
     * <p>Properties에 설정된 값이 있으면 사용하고, 없으면 기본 30분 적용
     *
     * @param limitType 제한 타입
     * @return IP 차단 기간
     */
    private Duration getBlockDuration(LimitType limitType) {
        return switch (limitType) {
            case LOGIN ->
                    Duration.ofMinutes(
                            rateLimitProperties.getLoginBlockDurationMinutes() != null
                                    ? rateLimitProperties.getLoginBlockDurationMinutes()
                                    : DEFAULT_BLOCK_DURATION_MINUTES);
            case INVALID_JWT ->
                    Duration.ofMinutes(
                            rateLimitProperties.getInvalidJwtBlockDurationMinutes() != null
                                    ? rateLimitProperties.getInvalidJwtBlockDurationMinutes()
                                    : DEFAULT_BLOCK_DURATION_MINUTES);
            default -> Duration.ofMinutes(DEFAULT_BLOCK_DURATION_MINUTES);
        };
    }
}
