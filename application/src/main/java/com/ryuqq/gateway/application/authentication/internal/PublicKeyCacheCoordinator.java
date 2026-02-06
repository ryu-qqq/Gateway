package com.ryuqq.gateway.application.authentication.internal;

import com.ryuqq.gateway.application.authentication.manager.PublicKeyCommandManager;
import com.ryuqq.gateway.application.authentication.manager.PublicKeyQueryManager;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.PublicKeyNotFoundException;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Public Key Cache Coordinator
 *
 * <p>Public Key Cache 로직을 조율하는 Coordinator
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>Redis Cache에서 조회 (PublicKeyQueryManager)
 *   <li>Cache Miss 시 AuthHub JWKS 호출 (AuthHubClient)
 *   <li>조회된 Public Key를 Redis에 저장 (PublicKeyCommandManager)
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PublicKeyCacheCoordinator {

    private final PublicKeyQueryManager publicKeyQueryManager;
    private final PublicKeyCommandManager publicKeyCommandManager;
    private final AuthHubClient authHubClient;

    public PublicKeyCacheCoordinator(
            PublicKeyQueryManager publicKeyQueryManager,
            PublicKeyCommandManager publicKeyCommandManager,
            AuthHubClient authHubClient) {
        this.publicKeyQueryManager = publicKeyQueryManager;
        this.publicKeyCommandManager = publicKeyCommandManager;
        this.authHubClient = authHubClient;
    }

    /**
     * Public Key 조회 (Cache Hit/Miss 전략)
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt;
     */
    public Mono<PublicKey> getPublicKey(String kid) {
        return publicKeyQueryManager
                .findByKid(kid)
                .switchIfEmpty(fetchFromAuthHubAndCache(kid))
                .onErrorMap(
                        e -> !(e instanceof PublicKeyNotFoundException),
                        e -> new RuntimeException("Failed to get public key for kid: " + kid, e));
    }

    /**
     * AuthHub에서 Public Key 조회 후 Redis에 캐싱
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt;
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
                                return Mono.error(new PublicKeyNotFoundException(kid));
                            }

                            return publicKeyCommandManager
                                    .saveAll(publicKeys)
                                    .thenReturn(targetKey);
                        });
    }

    /**
     * Public Key Cache 전체 갱신
     *
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> refreshAllKeys() {
        return authHubClient
                .fetchPublicKeys()
                .collectList()
                .flatMap(publicKeyCommandManager::saveAll)
                .onErrorMap(e -> new RuntimeException("Failed to refresh public keys", e));
    }
}
