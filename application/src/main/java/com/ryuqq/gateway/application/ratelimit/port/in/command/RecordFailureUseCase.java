package com.ryuqq.gateway.application.ratelimit.port.in.command;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import reactor.core.publisher.Mono;

/**
 * 실패 기록 UseCase (Command Port-In)
 *
 * <p>로그인 실패, JWT 검증 실패 등의 실패를 기록하는 Inbound Port
 *
 * <p><strong>사용 사례</strong>:
 *
 * <ul>
 *   <li>로그인 실패 시 호출 (IP별 실패 횟수 증가)
 *   <li>JWT 검증 실패 시 호출 (IP별 실패 횟수 증가)
 * </ul>
 *
 * <p><strong>동작</strong>:
 *
 * <ol>
 *   <li>실패 카운터 증가 (Redis INCR + TTL 설정)
 *   <li>임계값 초과 시 IP 차단 또는 계정 잠금 처리
 *   <li>Audit Log 기록 (필요 시)
 * </ol>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RecordFailureService (application.ratelimit.service.command)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RecordFailureUseCase {

    /**
     * 실패 기록 실행
     *
     * @param command RecordFailureCommand
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> execute(RecordFailureCommand command);
}
