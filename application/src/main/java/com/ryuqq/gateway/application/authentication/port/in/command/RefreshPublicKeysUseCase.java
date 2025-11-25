package com.ryuqq.gateway.application.authentication.port.in.command;

import reactor.core.publisher.Mono;

/**
 * Refresh Public Keys UseCase
 *
 * <p>Public Key Cache 갱신 UseCase
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>AuthHub에서 최신 Public Keys 조회
 *   <li>Redis Cache 갱신
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface RefreshPublicKeysUseCase {

    /**
     * Public Key Cache 전체 갱신
     *
     * <p><strong>Process</strong>:
     *
     * <ol>
     *   <li>AuthHub JWKS 엔드포인트에서 Public Keys 조회
     *   <li>Redis Cache 전체 삭제
     *   <li>새로운 Public Keys를 Redis에 저장
     * </ol>
     *
     * @return Mono&lt;Void&gt; 완료 시그널
     */
    Mono<Void> execute();
}
