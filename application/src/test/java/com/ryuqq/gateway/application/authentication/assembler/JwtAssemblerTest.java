package com.ryuqq.gateway.application.authentication.assembler;

import static org.assertj.core.api.Assertions.*;

import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.GetPublicKeyResponse;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.domain.authentication.vo.AccessToken;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JwtAssembler 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("JwtAssembler 단위 테스트")
class JwtAssemblerTest {

    private JwtAssembler jwtAssembler;

    private static final String VALID_JWT =
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2lkIn0."
                    + "eyJzdWIiOiJ1c2VyLTEyMyIsImlzcyI6ImF1dGgtaHViIn0."
                    + "signature";

    @BeforeEach
    void setUp() {
        jwtAssembler = new JwtAssembler();
    }

    @Nested
    @DisplayName("toAccessToken 메서드")
    class ToAccessTokenTest {

        @Test
        @DisplayName("유효한 command를 AccessToken으로 변환해야 한다")
        void shouldConvertValidCommandToAccessToken() {
            // given
            ValidateJwtCommand command = new ValidateJwtCommand(VALID_JWT);

            // when
            AccessToken result = jwtAssembler.toAccessToken(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo(VALID_JWT);
        }

        @Test
        @DisplayName("command가 null이면 IllegalArgumentException을 던져야 한다")
        void shouldThrowExceptionWhenCommandIsNull() {
            // when & then
            assertThatThrownBy(() -> jwtAssembler.toAccessToken(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("ValidateJwtCommand cannot be null");
        }
    }

    @Nested
    @DisplayName("toValidateJwtResponse 메서드")
    class ToValidateJwtResponseTest {

        @Test
        @DisplayName("JwtClaims를 ValidateJwtResponse로 변환해야 한다")
        void shouldConvertClaimsToValidateJwtResponse() {
            // given
            JwtClaims claims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now().minusSeconds(60),
                            List.of("ROLE_USER"),
                            "tenant-123",
                            "org-789",
                            "hash-456",
                            false);

            // when
            ValidateJwtResponse result = jwtAssembler.toValidateJwtResponse(claims);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.jwtClaims()).isEqualTo(claims);
            assertThat(result.jwtClaims().subject()).isEqualTo("user-123");
        }
    }

    @Nested
    @DisplayName("toFailedValidateJwtResponse 메서드")
    class ToFailedValidateJwtResponseTest {

        @Test
        @DisplayName("실패 응답을 생성해야 한다")
        void shouldCreateFailedResponse() {
            // when
            ValidateJwtResponse result = jwtAssembler.toFailedValidateJwtResponse();

            // then
            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.jwtClaims()).isNull();
        }
    }

    @Nested
    @DisplayName("toGetPublicKeyResponse 메서드")
    class ToGetPublicKeyResponseTest {

        @Test
        @DisplayName("PublicKey를 GetPublicKeyResponse로 변환해야 한다")
        void shouldConvertPublicKeyToGetPublicKeyResponse() {
            // given
            PublicKey publicKey =
                    PublicKey.of("test-kid", "modulus", "exponent", "RSA", "sig", "RS256");

            // when
            GetPublicKeyResponse result = jwtAssembler.toGetPublicKeyResponse(publicKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.publicKey()).isEqualTo(publicKey);
            assertThat(result.publicKey().kid()).isEqualTo("test-kid");
        }

        @Test
        @DisplayName("publicKey가 null이면 IllegalArgumentException을 던져야 한다")
        void shouldThrowExceptionWhenPublicKeyIsNull() {
            // when & then
            assertThatThrownBy(() -> jwtAssembler.toGetPublicKeyResponse(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("PublicKey cannot be null");
        }
    }
}
