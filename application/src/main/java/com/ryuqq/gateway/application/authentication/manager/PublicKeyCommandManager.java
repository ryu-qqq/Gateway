package com.ryuqq.gateway.application.authentication.manager;

import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Public Key Command Manager
 *
 * <p>PublicKeyCommandPort를 래핑하는 Manager
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyCommandManager {

    private final PublicKeyCommandPort publicKeyCommandPort;

    public PublicKeyCommandManager(PublicKeyCommandPort publicKeyCommandPort) {
        this.publicKeyCommandPort = publicKeyCommandPort;
    }

    /**
     * Public Key 목록을 Cache에 저장
     *
     * @param publicKeys Public Key 목록
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    public Mono<Void> saveAll(List<PublicKey> publicKeys) {
        return publicKeyCommandPort.saveAll(publicKeys);
    }
}
