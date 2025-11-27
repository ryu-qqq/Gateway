package com.ryuqq.gateway.application.authentication.port.out.client;

import com.ryuqq.gateway.domain.authentication.vo.ExpiredTokenInfo;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    /**
     * Access Token Refresh 호출
     *
     * <p>AuthHub의 Token Refresh 엔드포인트를 호출하여 새 Token Pair를 발급받습니다.
     *
     * <p>엔드포인트: {@code POST /api/v1/auth/refresh}
     *
     * <p><strong>요청</strong>:
     *
     * <ul>
     *   <li>Header: X-Tenant-ID (필수)
     *   <li>Body: refreshToken (필수)
     * </ul>
     *
     * <p><strong>응답</strong>:
     *
     * <ul>
     *   <li>accessToken: 새 Access Token
     *   <li>refreshToken: 새 Refresh Token (Rotation)
     * </ul>
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken 현재 유효한 Refresh Token
     * @return Mono&lt;TokenPair&gt; 새 Access Token + Refresh Token
     */
    Mono<TokenPair> refreshAccessToken(String tenantId, String refreshToken);

    /**
     * 만료된 JWT에서 정보 추출
     *
     * <p>AuthHub의 토큰 검증 엔드포인트를 호출하여 만료된 JWT에서 사용자 정보를 추출합니다. 서명은 검증하되 만료 시간은 무시합니다.
     *
     * <p>엔드포인트: {@code POST /api/v1/auth/extract-expired-info}
     *
     * <p><strong>응답</strong>:
     *
     * <ul>
     *   <li>expired: JWT 만료 여부
     *   <li>userId: 사용자 ID (만료된 경우에도 추출)
     *   <li>tenantId: 테넌트 ID (만료된 경우에도 추출)
     * </ul>
     *
     * @param token JWT Access Token
     * @return Mono&lt;ExpiredTokenInfo&gt; 만료 토큰 정보
     */
    Mono<ExpiredTokenInfo> extractExpiredTokenInfo(String token);
}
