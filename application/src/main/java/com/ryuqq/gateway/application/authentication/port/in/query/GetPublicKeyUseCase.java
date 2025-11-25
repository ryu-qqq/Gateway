package com.ryuqq.gateway.application.authentication.port.in.query;

import com.ryuqq.gateway.application.authentication.dto.query.GetPublicKeyQuery;
import com.ryuqq.gateway.application.authentication.dto.response.GetPublicKeyResponse;
import reactor.core.publisher.Mono;

/**
 * Public Key 조회 UseCase (Query Port-In)
 *
 * <p>JWT Header의 kid를 기반으로 Public Key를 조회하는 Inbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Public Key 조회 (kid 기반)
 *   <li>GetPublicKeyResponse로 변환
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>GetPublicKeyService (application.authentication.service.query)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GetPublicKeyUseCase {

    /**
     * Public Key 조회 실행
     *
     * @param query GetPublicKeyQuery (kid 포함)
     * @return Mono&lt;GetPublicKeyResponse&gt; (Public Key 정보)
     */
    Mono<GetPublicKeyResponse> execute(GetPublicKeyQuery query);
}
