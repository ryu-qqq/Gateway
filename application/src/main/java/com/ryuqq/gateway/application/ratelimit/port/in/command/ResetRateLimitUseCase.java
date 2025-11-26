package com.ryuqq.gateway.application.ratelimit.port.in.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import reactor.core.publisher.Mono;

/**
 * Rate Limit 리셋 UseCase (Command Port-In)
 *
 * <p>Rate Limit 카운터를 리셋하는 Inbound Port (Admin 전용)
 *
 * <p><strong>사용 사례</strong>:
 *
 * <ul>
 *   <li>차단된 IP 해제
 *   <li>잠금된 계정 해제
 *   <li>Rate Limit 카운터 초기화
 * </ul>
 *
 * <p><strong>동작</strong>:
 *
 * <ol>
 *   <li>Redis Key 삭제
 *   <li>Audit Log 기록
 * </ol>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>ResetRateLimitService (application.ratelimit.service.command)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface ResetRateLimitUseCase {

    /**
     * Rate Limit 리셋 실행
     *
     * @param command ResetRateLimitCommand
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> execute(ResetRateLimitCommand command);
}
