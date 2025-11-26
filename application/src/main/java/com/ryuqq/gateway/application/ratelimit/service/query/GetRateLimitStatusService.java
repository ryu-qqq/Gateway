package com.ryuqq.gateway.application.ratelimit.service.query;

import com.ryuqq.gateway.application.ratelimit.dto.response.RateLimitStatusResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.query.GetRateLimitStatusUseCase;
import com.ryuqq.gateway.application.ratelimit.port.out.query.RateLimitCounterQueryPort;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitPolicy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 상태 조회 Service (GetRateLimitStatusUseCase 구현체)
 *
 * <p>특정 키의 Rate Limit 상태를 조회하는 UseCase 구현체 (Admin 전용)
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>현재 카운터 조회
 *   <li>TTL 조회
 *   <li>Response 생성
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetRateLimitStatusService implements GetRateLimitStatusUseCase {

    private final RateLimitCounterQueryPort rateLimitCounterQueryPort;

    /** 생성자 (Lombok 금지) */
    public GetRateLimitStatusService(RateLimitCounterQueryPort rateLimitCounterQueryPort) {
        this.rateLimitCounterQueryPort = rateLimitCounterQueryPort;
    }

    /**
     * Rate Limit 상태 조회
     *
     * @param limitType Rate Limit 타입
     * @param identifier 식별자
     * @return Mono&lt;RateLimitStatusResponse&gt; (상태 정보)
     */
    @Override
    public Mono<RateLimitStatusResponse> execute(LimitType limitType, String identifier) {
        RateLimitKey key = RateLimitKey.of(limitType, identifier);
        RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(limitType);

        return Mono.zip(
                        rateLimitCounterQueryPort.getCurrentCount(key),
                        rateLimitCounterQueryPort.getTtlSeconds(key))
                .map(
                        tuple -> {
                            long currentCount = tuple.getT1();
                            long ttlSeconds = tuple.getT2();
                            return RateLimitStatusResponse.of(
                                    limitType,
                                    identifier,
                                    currentCount,
                                    policy.getMaxRequests(),
                                    ttlSeconds);
                        })
                .defaultIfEmpty(
                        RateLimitStatusResponse.notFound(
                                limitType, identifier, policy.getMaxRequests()));
    }
}
