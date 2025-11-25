package com.ryuqq.gateway.application.authentication.service.query;

import reactor.core.publisher.Mono;

import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import com.ryuqq.gateway.application.authentication.port.out.query.PublicKeyQueryPort;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;

import org.springframework.stereotype.Service;

/**
 * Public Key 조회 Service
 *
 * <p>Redis Cache + AuthHub Fallback 전략을 사용하여 Public Key를 조회하는 서비스
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>Redis Cache에서 조회 (PublicKeyQueryPort)
 *   <li>Cache Miss 시 AuthHub JWKS 호출 (AuthHubClient)
 *   <li>조회된 Public Key를 Redis에 저장 (PublicKeyCommandPort)
 * </ol>
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * GetPublicKeyService (Application Service)
 *   ↓ (calls)
 * PublicKeyQueryPort + AuthHubClient + PublicKeyCommandPort (Application Out Ports)
 *   ↓ (implemented by)
 * PublicKeyQueryAdapter + AuthHubAdapter + PublicKeyCommandAdapter (Infrastructure Adapters)
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPublicKeyService {

    private final PublicKeyQueryPort publicKeyQueryPort;
    private final AuthHubClient authHubClient;
    private final PublicKeyCommandPort publicKeyCommandPort;

    public GetPublicKeyService(
            PublicKeyQueryPort publicKeyQueryPort,
            AuthHubClient authHubClient,
            PublicKeyCommandPort publicKeyCommandPort) {
        this.publicKeyQueryPort = publicKeyQueryPort;
        this.authHubClient = authHubClient;
        this.publicKeyCommandPort = publicKeyCommandPort;
    }

    /**
     * Public Key 조회 (Cache Hit/Miss 전략)
     *
     * @param kid Key ID
     * @return Mono<PublicKey>
     */
    public Mono<PublicKey> getPublicKey(String kid) {
        return publicKeyQueryPort
                .findByKid(kid)
                .switchIfEmpty(fetchFromAuthHubAndCache(kid))
                .onErrorMap(
                        e -> new RuntimeException("Failed to get public key for kid: " + kid, e));
    }

    /**
     * AuthHub에서 Public Key 조회 후 Redis에 캐싱
     *
     * @param kid Key ID
     * @return Mono<PublicKey>
     */
    private Mono<PublicKey> fetchFromAuthHubAndCache(String kid) {
        return authHubClient
                .fetchPublicKeys()
                .collectList()
                .flatMap(
                        publicKeys -> {
                            var targetKey =
                                    publicKeys.stream()
                                            .filter(pk -> pk.kid().equals(kid))
                                            .findFirst()
                                            .orElse(null);

                            if (targetKey == null) {
                                return Mono.error(
                                        new RuntimeException("Public Key not found for kid: " + kid));
                            }

                            return publicKeyCommandPort.saveAll(publicKeys).thenReturn(targetKey);
                        });
    }
}
