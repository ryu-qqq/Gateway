package com.ryuqq.gateway.application.authentication.port.out.query;

import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import reactor.core.publisher.Mono;

/**
 * Public Key Query Port
 *
 * <p>Redis Cache에서 Public Key를 조회하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>PublicKeyQueryAdapter (Redis Cache 조회만)
 * </ul>
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Public Key 조회
 *   <li>Cache Miss 시 empty Mono 반환 (Fallback은 Application Service에서 처리)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface PublicKeyQueryPort {

    /**
     * Redis Cache에서 Public Key 조회
     *
     * @param kid Key ID
     * @return Mono&lt;PublicKey&gt; (Cache Miss 시 empty Mono)
     */
    Mono<PublicKey> findByKid(String kid);
}
