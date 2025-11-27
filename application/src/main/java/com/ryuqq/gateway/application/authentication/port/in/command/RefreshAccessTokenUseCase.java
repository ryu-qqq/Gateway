package com.ryuqq.gateway.application.authentication.port.in.command;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import reactor.core.publisher.Mono;

/**
 * Access Token Refresh UseCase (Command Port-In)
 *
 * <p>Refresh Token을 사용하여 새로운 Access Token을 발급받는 Inbound Port
 *
 * <p><strong>처리 단계</strong>:
 *
 * <ol>
 *   <li>Redis Lock 획득 (Race Condition 방지)
 *   <li>Refresh Token Blacklist 확인 (재사용 탐지)
 *   <li>AuthHub Token Refresh 호출
 *   <li>기존 Refresh Token Blacklist 등록 (Rotation)
 *   <li>새 Token Pair 반환
 * </ol>
 *
 * <p><strong>보안 특징</strong>:
 *
 * <ul>
 *   <li>Refresh Token Rotation: 매 Refresh 시 새 Refresh Token 발급
 *   <li>Refresh Token Reuse Detection: 탈취 감지 및 방어
 *   <li>Distributed Lock: 동시 Refresh 요청 직렬화
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>RefreshAccessTokenService (application.authentication.service.command)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RefreshAccessTokenUseCase {

    /**
     * Access Token Refresh 실행
     *
     * @param command RefreshAccessTokenCommand (tenantId, userId, refreshToken)
     * @return Mono&lt;RefreshAccessTokenResponse&gt; (새 Access Token + Refresh Token)
     */
    Mono<RefreshAccessTokenResponse> execute(RefreshAccessTokenCommand command);
}
