package com.ryuqq.gateway.application.authentication.port.in.command;

import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import reactor.core.publisher.Mono;

/**
 * JWT 검증 UseCase (Command Port-In)
 *
 * <p>JWT Access Token의 유효성을 검증하는 Inbound Port
 *
 * <p><strong>검증 단계</strong>:
 *
 * <ol>
 *   <li>JWT Header에서 kid 추출
 *   <li>Public Key 조회 (kid 기반)
 *   <li>JWT Signature 검증
 *   <li>JWT Expiration 검증
 *   <li>JWT Claims 추출
 * </ol>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>ValidateJwtService (application.authentication.service.command)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface ValidateJwtUseCase {

    /**
     * JWT 검증 실행
     *
     * @param command ValidateJwtCommand
     * @return Mono&lt;ValidateJwtResponse&gt; (검증 결과)
     */
    Mono<ValidateJwtResponse> execute(ValidateJwtCommand command);
}
