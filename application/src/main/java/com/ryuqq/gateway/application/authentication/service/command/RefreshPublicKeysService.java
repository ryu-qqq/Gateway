package com.ryuqq.gateway.application.authentication.service.command;

import com.ryuqq.gateway.application.authentication.port.in.command.RefreshPublicKeysUseCase;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.application.authentication.port.out.command.PublicKeyCommandPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Refresh Public Keys Service
 *
 * <p>Public Key Cache 갱신 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * Service (Application)
 *   ↓ (calls)
 * AuthHubQueryPort (Application Out Port)
 *   ↓ (implemented by)
 * AuthHubAdapter (Infrastructure)
 *
 * Service (Application)
 *   ↓ (calls)
 * PublicKeyCommandPort (Application Out Port)
 *   ↓ (implemented by)
 * PublicKeyCommandAdapter (Infrastructure)
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RefreshPublicKeysService implements RefreshPublicKeysUseCase {

    private final AuthHubClient authHubClient;
    private final PublicKeyCommandPort publicKeyCommandPort;

    public RefreshPublicKeysService(
            AuthHubClient authHubClient, PublicKeyCommandPort publicKeyCommandPort) {
        this.authHubClient = authHubClient;
        this.publicKeyCommandPort = publicKeyCommandPort;
    }

    /**
     * Public Key Cache 전체 갱신
     *
     * <p><strong>Process</strong>:
     *
     * <ol>
     *   <li>AuthHub JWKS 엔드포인트에서 Public Keys 조회
     *   <li>PublicKeyCommandPort를 통해 Redis Cache 저장
     * </ol>
     *
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> execute() {
        return authHubClient
                .fetchPublicKeys()
                .collectList()
                .flatMap(publicKeyCommandPort::saveAll)
                .onErrorMap(e -> new RuntimeException("Failed to refresh public keys", e));
    }
}
