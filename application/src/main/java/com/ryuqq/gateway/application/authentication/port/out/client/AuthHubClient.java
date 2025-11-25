package com.ryuqq.gateway.application.authentication.port.out.client;

import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import reactor.core.publisher.Flux;

/**
 * AuthHub Query Port
 *
 * <p>AuthHub 외부 시스템과의 통신을 담당하는 Port
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>AuthHubAdapter (WebClient + Resilience4j)
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 *   <li>Timeout: Connection 3초, Response 3초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface AuthHubClient {

    /**
     * JWKS 엔드포인트 호출
     *
     * <p>AuthHub의 JWKS 엔드포인트를 호출하여 Public Key 목록을 조회합니다.
     *
     * <p>엔드포인트: {@code GET /api/v1/auth/jwks}
     *
     * @return Flux&lt;PublicKey&gt; Public Key 스트림
     */
    Flux<PublicKey> fetchPublicKeys();
}
