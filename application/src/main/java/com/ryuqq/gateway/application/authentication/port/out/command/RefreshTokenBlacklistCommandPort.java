package com.ryuqq.gateway.application.authentication.port.out.command;

import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Command Port (Outbound)
 *
 * <p>Refresh Token Blacklist 저장을 담당하는 Outbound Port
 *
 * <p><strong>Redis Key 패턴</strong>: {@code tenant:{tenantId}:refresh:blacklist:{tokenHash}}
 *
 * <p><strong>사용 시점</strong>:
 *
 * <ul>
 *   <li>Refresh Token Rotation 시 기존 토큰 등록
 *   <li>사용자 로그아웃 시 토큰 무효화
 *   <li>보안 위협 감지 시 토큰 강제 폐기
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
public interface RefreshTokenBlacklistCommandPort {

    /**
     * Refresh Token을 Blacklist에 등록
     *
     * <p>SHA-256 해시로 저장하여 원본 토큰 노출 방지
     *
     * @param tenantId Tenant 식별자
     * @param refreshToken Blacklist에 등록할 Refresh Token
     * @param ttlSeconds TTL (초) - 일반적으로 Refresh Token 만료 시간과 동일
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> addToBlacklist(String tenantId, RefreshToken refreshToken, long ttlSeconds);
}
