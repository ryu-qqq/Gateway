package com.ryuqq.gateway.application.authentication.service.command;

import com.ryuqq.gateway.application.authentication.assembler.JwtAssembler;
import com.ryuqq.gateway.application.authentication.component.JwtValidator;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.application.authentication.service.query.GetPublicKeyService;
import com.ryuqq.gateway.domain.authentication.vo.AccessToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * JWT 검증 Service (ValidateJwtUseCase 구현체)
 *
 * <p>JWT Access Token의 유효성을 검증하는 UseCase 구현체
 *
 * <p><strong>오케스트레이션 역할만 수행</strong>:
 *
 * <ol>
 *   <li>Command → Domain VO 변환 (Assembler)
 *   <li>Public Key 조회 (GetPublicKeyService)
 *   <li>JWT 서명 검증 (JwtValidationService)
 *   <li>JWT Claims 추출 (JwtValidationService)
 *   <li>JWT 만료 검증 (Domain VO)
 *   <li>Domain VO → Response 변환 (Assembler)
 * </ol>
 *
 * <p><strong>Zero-Tolerance 준수</strong>:
 *
 * <ul>
 *   <li>Transaction 불필요 (읽기 전용)
 *   <li>Lombok 금지
 *   <li>비즈니스 로직은 Domain에 위임
 *   <li>Reactive Programming (Mono/Flux)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class ValidateJwtService implements ValidateJwtUseCase {

    private final JwtValidator jwtValidator;
    private final GetPublicKeyService getPublicKeyService;
    private final JwtAssembler jwtAssembler;

    /** 생성자 (Lombok 금지) */
    public ValidateJwtService(
            JwtValidator jwtValidator,
            GetPublicKeyService getPublicKeyService,
            JwtAssembler jwtAssembler) {
        this.jwtValidator = jwtValidator;
        this.getPublicKeyService = getPublicKeyService;
        this.jwtAssembler = jwtAssembler;
    }

    /**
     * JWT 검증 실행
     *
     * @param command ValidateJwtCommand
     * @return Mono&lt;ValidateJwtResponse&gt; (검증 결과)
     */
    @Override
    public Mono<ValidateJwtResponse> execute(ValidateJwtCommand command) {
        return Mono.fromCallable(() -> jwtAssembler.toAccessToken(command))
                .flatMap(this::validateJwt)
                .onErrorResume(e -> Mono.just(jwtAssembler.toFailedValidateJwtResponse()));
    }

    private Mono<ValidateJwtResponse> validateJwt(AccessToken accessToken) {
        return getPublicKeyService
                .getPublicKey(accessToken.getKid())
                .flatMap(
                        publicKey ->
                                jwtValidator
                                        .verifySignature(accessToken.getValue(), publicKey)
                                        .flatMap(
                                                isValid -> {
                                                    if (!isValid) {
                                                        return Mono.just(
                                                                jwtAssembler
                                                                        .toFailedValidateJwtResponse());
                                                    }
                                                    return jwtValidator
                                                            .extractClaims(accessToken.getValue())
                                                            .map(
                                                                    claims -> {
                                                                        if (claims.isExpired()) {
                                                                            return jwtAssembler
                                                                                    .toFailedValidateJwtResponse();
                                                                        }
                                                                        return jwtAssembler
                                                                                .toValidateJwtResponse(
                                                                                        claims);
                                                                    });
                                                }));
    }
}
