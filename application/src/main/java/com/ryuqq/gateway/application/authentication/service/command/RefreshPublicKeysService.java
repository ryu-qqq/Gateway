package com.ryuqq.gateway.application.authentication.service.command;

import com.ryuqq.gateway.application.authentication.internal.PublicKeyCacheCoordinator;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshPublicKeysUseCase;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Refresh Public Keys Service
 *
 * <p>Public Key Cache 갱신 서비스
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>PublicKeyCacheCoordinator에 갱신 위임
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class RefreshPublicKeysService implements RefreshPublicKeysUseCase {

    private final PublicKeyCacheCoordinator publicKeyCacheCoordinator;

    public RefreshPublicKeysService(PublicKeyCacheCoordinator publicKeyCacheCoordinator) {
        this.publicKeyCacheCoordinator = publicKeyCacheCoordinator;
    }

    /**
     * Public Key Cache 전체 갱신
     *
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    @Override
    public Mono<Void> execute() {
        return publicKeyCacheCoordinator.refreshAllKeys();
    }
}
