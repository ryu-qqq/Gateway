package com.ryuqq.gateway.application.ratelimit.port.in.command;

import reactor.core.publisher.Mono;

/**
 * IP 차단 해제 UseCase
 *
 * <p>차단된 IP를 해제하는 Command UseCase
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>UnblockIpService
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 차단 해제 처리
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface UnblockIpUseCase {

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부 (true: 해제됨, false: 이미 차단되지 않음)
     */
    Mono<Boolean> execute(String ipAddress);
}
