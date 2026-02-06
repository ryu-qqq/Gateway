package com.ryuqq.gateway.application.ratelimit.service.command;

import com.ryuqq.gateway.application.ratelimit.manager.IpBlockCommandManager;
import com.ryuqq.gateway.application.ratelimit.port.in.command.UnblockIpUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * IP 차단 해제 Service
 *
 * <p>차단된 IP를 해제하는 Command Service
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class UnblockIpService implements UnblockIpUseCase {

    private final IpBlockCommandManager ipBlockCommandManager;

    public UnblockIpService(IpBlockCommandManager ipBlockCommandManager) {
        this.ipBlockCommandManager = ipBlockCommandManager;
    }

    /**
     * IP 차단 해제
     *
     * @param ipAddress IP 주소
     * @return Mono&lt;Boolean&gt; 해제 성공 여부
     */
    @Override
    public Mono<Boolean> execute(String ipAddress) {
        return ipBlockCommandManager.unblock(ipAddress);
    }
}
