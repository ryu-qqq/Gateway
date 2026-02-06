package com.ryuqq.gateway.application.authentication.service.query;

import com.ryuqq.gateway.application.authentication.internal.PublicKeyCacheCoordinator;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Public Key 조회 Service
 *
 * <p>Redis Cache + AuthHub Fallback 전략을 사용하여 Public Key를 조회하는 서비스
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>PublicKeyCacheCoordinator에 조회 위임
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPublicKeyService {

    private final PublicKeyCacheCoordinator publicKeyCacheCoordinator;

    public GetPublicKeyService(PublicKeyCacheCoordinator publicKeyCacheCoordinator) {
        this.publicKeyCacheCoordinator = publicKeyCacheCoordinator;
    }

    /**
     * Public Key 조회 (Cache Hit/Miss 전략)
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt;
     */
    public Mono<PublicKey> getPublicKey(String kid) {
        return publicKeyCacheCoordinator.getPublicKey(kid);
    }
}
