package com.ryuqq.gateway.application.ratelimit.port.in.query;

import com.ryuqq.gateway.application.ratelimit.dto.response.RateLimitStatusResponse;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 상태 조회 UseCase (Query Port-In)
 *
 * <p>특정 키의 Rate Limit 상태를 조회하는 Inbound Port (Admin 전용)
 *
 * <p><strong>사용 사례</strong>:
 *
 * <ul>
 *   <li>특정 IP의 Rate Limit 상태 확인
 *   <li>특정 사용자의 Rate Limit 상태 확인
 *   <li>Admin 대시보드에서 모니터링
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>GetRateLimitStatusService (application.ratelimit.service.query)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GetRateLimitStatusUseCase {

    /**
     * Rate Limit 상태 조회
     *
     * @param limitType Rate Limit 타입
     * @param identifier 식별자
     * @return Mono&lt;RateLimitStatusResponse&gt; (상태 정보)
     */
    Mono<RateLimitStatusResponse> execute(LimitType limitType, String identifier);
}
