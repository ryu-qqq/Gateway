package com.ryuqq.gateway.application.ratelimit.port.in.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 체크 UseCase (Command Port-In)
 *
 * <p>요청에 대한 Rate Limit을 체크하는 Inbound Port
 *
 * <p><strong>체크 단계</strong>:
 *
 * <ol>
 *   <li>Rate Limit Policy 조회 (LimitType 기반)
 *   <li>현재 카운터 조회 (Redis)
 *   <li>Rate Limit 초과 여부 판단
 *   <li>허용 시 카운터 증가
 *   <li>거부 시 Action 결정 (REJECT, BLOCK_IP, LOCK_ACCOUNT, REVOKE_TOKEN)
 * </ol>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>CheckRateLimitService (application.ratelimit.service.command)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface CheckRateLimitUseCase {

    /**
     * Rate Limit 체크 실행
     *
     * @param command CheckRateLimitCommand
     * @return Mono&lt;CheckRateLimitResponse&gt; (체크 결과)
     */
    Mono<CheckRateLimitResponse> execute(CheckRateLimitCommand command);
}
