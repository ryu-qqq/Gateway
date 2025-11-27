package com.ryuqq.gateway.application.authentication.port.out.query;

import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Query Port (Outbound)
 *
 * <p>Refresh Token Blacklist 조회를 담당하는 Outbound Port
 *
 * <p><strong>Redis Key 패턴</strong>:
 * {@code tenant:{tenantId}:refresh:blacklist:{tokenHash}}
 *
 * <p><strong>사용 시점</strong>:
 *
 * <ul>
 *   <li>Token Refresh 요청 시 재사용 감지
 *   <li>탈취된 Refresh Token 검증
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RefreshTokenBlacklistAdapter (adapter-out.persistence-redis)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RefreshTokenBlacklistQueryPort {

    /**
     * Refresh Token이 Blacklist에 존재하는지 확인
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken 확인할 Refresh Token
     * @return Mono&lt;Boolean&gt; Blacklist 존재 여부
     */
    Mono<Boolean> isBlacklisted(String tenantId, RefreshToken refreshToken);
}
