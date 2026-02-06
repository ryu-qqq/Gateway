package com.ryuqq.gateway.application.authentication.manager;

import com.ryuqq.gateway.application.authentication.port.out.query.PublicKeyQueryPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Public Key Query Manager
 *
 * <p>PublicKeyQueryPort를 래핑하는 Manager
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyQueryManager {

    private final PublicKeyQueryPort publicKeyQueryPort;

    public PublicKeyQueryManager(PublicKeyQueryPort publicKeyQueryPort) {
        this.publicKeyQueryPort = publicKeyQueryPort;
    }

    /**
     * Redis Cache에서 Public Key 조회
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt; (Cache Miss 시 empty Mono)
     */
    public Mono<PublicKey> findByKid(String kid) {
        return publicKeyQueryPort.findByKid(kid);
    }
}
