package com.ryuqq.gateway.application.ratelimit.port.in.query;

import com.ryuqq.gateway.application.ratelimit.dto.response.BlockedIpResponse;
import reactor.core.publisher.Flux;

/**
 * 차단된 IP 목록 조회 UseCase
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GetBlockedIpsUseCase {

    /**
     * 모든 차단된 IP 목록 조회
     *
     * @return Flux&lt;BlockedIpResponse&gt; 차단된 IP 목록 (TTL 포함)
     */
    Flux<BlockedIpResponse> execute();
}
