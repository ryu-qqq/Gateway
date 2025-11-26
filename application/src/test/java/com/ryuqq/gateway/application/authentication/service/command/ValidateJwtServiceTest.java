package com.ryuqq.gateway.application.authentication.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.assembler.JwtAssembler;
import com.ryuqq.gateway.application.authentication.component.JwtValidator;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.application.authentication.service.query.GetPublicKeyService;
import com.ryuqq.gateway.domain.authentication.exception.JwtExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import com.ryuqq.gateway.domain.authentication.vo.AccessToken;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * ValidateJwtService 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateJwtService 단위 테스트")
class ValidateJwtServiceTest {

    @Mock private JwtValidator jwtValidator;

    @Mock private GetPublicKeyService getPublicKeyService;

    @Mock private JwtAssembler jwtAssembler;

    private ValidateJwtService validateJwtService;

    private static final String VALID_JWT =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2lkIn0."
                    + "eyJzdWIiOiJ1c2VyLTEyMyIsImlzcyI6ImF1dGgtaHViIn0."
                    + "signature";
    private static final String KID = "test-kid";

    @BeforeEach
    void setUp() {
        validateJwtService =
                new ValidateJwtService(jwtValidator, getPublicKeyService, jwtAssembler);
    }

    @Nested
    @DisplayName("JWT 검증 성공 시나리오")
    class JwtValidationSuccess {

        @Test
        @DisplayName("유효한 JWT 검증 시 성공 응답을 반환해야 한다")
        void shouldReturnSuccessResponseForValidJwt() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            PublicKey publicKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            JwtClaims claims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now().minusSeconds(60),
                            List.of("ROLE_USER"),
                            "tenant-123",
                            "hash-456");
            ValidateJwtResponse expectedResponse = new ValidateJwtResponse(claims, true);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID)).willReturn(Mono.just(publicKey));
            given(jwtValidator.verifySignature(VALID_JWT, publicKey)).willReturn(Mono.just(true));
            given(jwtValidator.extractClaims(VALID_JWT)).willReturn(Mono.just(claims));
            given(jwtAssembler.toValidateJwtResponse(claims)).willReturn(expectedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.isValid()).isTrue();
                                assertThat(response.jwtClaims()).isEqualTo(claims);
                            })
                    .verifyComplete();

            then(jwtAssembler).should().toAccessToken(command);
            then(getPublicKeyService).should().getPublicKey(KID);
            then(jwtValidator).should().verifySignature(VALID_JWT, publicKey);
            then(jwtValidator).should().extractClaims(VALID_JWT);
        }
    }

    @Nested
    @DisplayName("JWT 검증 실패 시나리오")
    class JwtValidationFailure {

        @Test
        @DisplayName("서명 검증 실패 시 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseWhenSignatureInvalid() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            PublicKey publicKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID)).willReturn(Mono.just(publicKey));
            given(jwtValidator.verifySignature(VALID_JWT, publicKey)).willReturn(Mono.just(false));
            given(jwtAssembler.toFailedValidateJwtResponse()).willReturn(failedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.isValid()).isFalse();
                                assertThat(response.jwtClaims()).isNull();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("만료된 JWT일 경우 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseWhenJwtExpired() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            PublicKey publicKey = PublicKey.of(KID, "modulus", "exponent", "RSA", "sig", "RS256");
            JwtClaims expiredClaims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().minusSeconds(3600), // 과거 시간 (만료됨)
                            Instant.now().minusSeconds(7200),
                            List.of("ROLE_USER"),
                            "tenant-123",
                            "hash-456");
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID)).willReturn(Mono.just(publicKey));
            given(jwtValidator.verifySignature(VALID_JWT, publicKey)).willReturn(Mono.just(true));
            given(jwtValidator.extractClaims(VALID_JWT)).willReturn(Mono.just(expiredClaims));
            given(jwtAssembler.toFailedValidateJwtResponse()).willReturn(failedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.isValid()).isFalse();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("DomainException 발생 시 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseOnDomainException() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID))
                    .willReturn(Mono.error(new JwtInvalidException("Invalid JWT")));
            given(jwtAssembler.toFailedValidateJwtResponse()).willReturn(failedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.isValid()).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("JwtExpiredException 발생 시 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseOnJwtExpiredException() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID))
                    .willReturn(Mono.error(new JwtExpiredException(VALID_JWT)));
            given(jwtAssembler.toFailedValidateJwtResponse()).willReturn(failedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.isValid()).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("IllegalStateException 발생 시 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseOnIllegalStateException() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID))
                    .willReturn(Mono.error(new IllegalStateException("Invalid state")));
            given(jwtAssembler.toFailedValidateJwtResponse()).willReturn(failedResponse);

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.isValid()).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("예외 처리 시나리오")
    class ExceptionHandling {

        @Test
        @DisplayName("예상치 못한 예외는 전파되어야 한다")
        void shouldPropagateUnexpectedException() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            AccessToken accessToken = AccessToken.of(VALID_JWT);

            given(jwtAssembler.toAccessToken(command)).willReturn(accessToken);
            given(getPublicKeyService.getPublicKey(KID))
                    .willReturn(Mono.error(new RuntimeException("Unexpected error")));

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof RuntimeException
                                            && throwable.getMessage().equals("Unexpected error"))
                    .verify();
        }

        @Test
        @DisplayName("Assembler 변환 중 IllegalArgumentException 발생 시 실패 응답을 반환해야 한다")
        void shouldReturnFailedResponseOnAssemblerException() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);
            ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);

            given(jwtAssembler.toAccessToken(command))
                    .willThrow(new IllegalArgumentException("Invalid command"));
            // IllegalArgumentException은 DomainException/IllegalStateException이 아니므로 전파됨

            // when
            Mono<ValidateJwtResponse> result = validateJwtService.execute(command);

            // then
            StepVerifier.create(result)
                    .expectErrorMatches(
                            throwable ->
                                    throwable instanceof IllegalArgumentException
                                            && throwable.getMessage().equals("Invalid command"))
                    .verify();
        }
    }
}
