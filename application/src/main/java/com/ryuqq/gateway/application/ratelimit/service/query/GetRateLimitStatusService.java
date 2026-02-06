package com.ryuqq.gateway.application.ratelimit.service.query;

import com.ryuqq.gateway.application.ratelimit.dto.response.RateLimitStatusResponse;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterQueryManager;
import com.ryuqq.gateway.application.ratelimit.port.in.query.GetRateLimitStatusUseCase;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitPolicy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 상태 조회 Service
 *
 * <p>특정 키의 Rate Limit 상태를 조회하는 Service (Admin 전용)
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetRateLimitStatusService implements GetRateLimitStatusUseCase {

    private final RateLimitCounterQueryManager rateLimitCounterQueryManager;

    public GetRateLimitStatusService(RateLimitCounterQueryManager rateLimitCounterQueryManager) {
        this.rateLimitCounterQueryManager = rateLimitCounterQueryManager;
    }

    /**
     * Rate Limit 상태 조회
     *
     * @param limitType Rate Limit 타입
     * @param identifier 식별자
     * @return Mono&lt;RateLimitStatusResponse&gt;
     */
    @Override
    public Mono<RateLimitStatusResponse> execute(LimitType limitType, String identifier) {
        RateLimitKey key = RateLimitKey.of(limitType, identifier);
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(limitType);

        return Mono.zip(
                        rateLimitCounterQueryManager.getCurrentCount(key),
                        rateLimitCounterQueryManager.getTtlSeconds(key))
                .map(
                        tuple -> {
                            long currentCount = tuple.getT1();
                            long ttlSeconds = tuple.getT2();
                            return RateLimitStatusResponse.of(
                                    limitType,
                                    identifier,
                                    currentCount,
                                    policy.maxRequests(),
                                    ttlSeconds);
                        })
                .defaultIfEmpty(
                        RateLimitStatusResponse.notFound(
                                limitType, identifier, policy.maxRequests()));
    }
}
