package com.ryuqq.gateway.application.ratelimit.service.query;

import com.ryuqq.gateway.application.ratelimit.dto.response.BlockedIpResponse;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockQueryManager;
import com.ryuqq.gateway.application.ratelimit.port.in.query.GetBlockedIpsUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 차단된 IP 목록 조회 Service
 *
 * <p>모든 차단된 IP를 조회하고 TTL 정보를 포함하여 반환합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetBlockedIpsService implements GetBlockedIpsUseCase {

    private final IpBlockQueryManager ipBlockQueryManager;

    public GetBlockedIpsService(IpBlockQueryManager ipBlockQueryManager) {
        this.ipBlockQueryManager = ipBlockQueryManager;
    }

    /**
     * 모든 차단된 IP 목록 조회
     *
     * <p>N+1 문제를 방지하기 위해 IP와 TTL을 함께 조회합니다.
     *
     * @return Flux&lt;BlockedIpResponse&gt; 차단된 IP 목록 (TTL 포함)
     */
    @Override
    public Flux<BlockedIpResponse> execute() {
        return ipBlockQueryManager
                .findAllBlockedIpsWithTtl()
                .map(dto -> BlockedIpResponse.of(dto.ip(), dto.ttlSeconds()));
    }
}
