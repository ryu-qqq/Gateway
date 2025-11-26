package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccessToken VO 테스트")
class AccessTokenTest {

    private static final String VALID_KID = "test-key-id-123";

    private String createValidJwtHeader(String kid) {
        String headerJson = String.format("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"%s\"}", kid);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    }

    private String createValidJwt(String kid) {
        String header = createValidJwtHeader(kid);
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"user123\",\"exp\":1700000000}".getBytes(StandardCharsets.UTF_8));
        String signature = "dummy-signature";
        return header + "." + payload + "." + signature;
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("유효한 JWT로 AccessToken 생성")
        void shouldCreateAccessTokenWithValidJwt() {
            // given
            String validJwt = createValidJwt(VALID_KID);

            // when
            AccessToken accessToken = AccessToken.of(validJwt);

            // then
            assertThat(accessToken).isNotNull();
            assertThat(accessToken.getValue()).isEqualTo(validJwt);
            assertThat(accessToken.getKid()).isEqualTo(VALID_KID);
        }

        @Test
        @DisplayName("다양한 kid로 AccessToken 생성")
        void shouldCreateAccessTokenWithVariousKids() {
            // given
            String kid1 = "key-uuid-1234-5678";
            String kid2 = "production_key_v2";
            String kid3 = "550e8400-e29b-41d4-a716-446655440000";

            // when
            AccessToken token1 = AccessToken.of(createValidJwt(kid1));
            AccessToken token2 = AccessToken.of(createValidJwt(kid2));
            AccessToken token3 = AccessToken.of(createValidJwt(kid3));

            // then
            assertThat(token1.getKid()).isEqualTo(kid1);
            assertThat(token2.getKid()).isEqualTo(kid2);
            assertThat(token3.getKid()).isEqualTo(kid3);
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null 토큰 값이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsNull() {
            // when & then
            assertThatThrownBy(() -> AccessToken.of(null))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열 토큰이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsEmpty() {
            // when & then
            assertThatThrownBy(() -> AccessToken.of(""))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있는 토큰이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsBlank() {
            // when & then
            assertThatThrownBy(() -> AccessToken.of("   "))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("JWT 형식이 아니면 예외 발생 (파트 부족)")
        void shouldThrowExceptionWhenJwtHasInsufficientParts() {
            // given
            String invalidJwt = "header.payload";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("expected 3 parts");
        }

        @Test
        @DisplayName("JWT 형식이 아니면 예외 발생 (파트 초과)")
        void shouldThrowExceptionWhenJwtHasExcessParts() {
            // given
            String invalidJwt = "header.payload.signature.extra";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("expected 3 parts");
        }

        @Test
        @DisplayName("단일 문자열이면 예외 발생")
        void shouldThrowExceptionWhenSingleString() {
            // given
            String invalidJwt = "single-string-without-dots";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("expected 3 parts but got 1");
        }

        @Test
        @DisplayName("kid가 없는 헤더면 예외 발생")
        void shouldThrowExceptionWhenKidIsMissing() {
            // given
            String headerWithoutKid =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    "{\"alg\":\"RS256\",\"typ\":\"JWT\"}"
                                            .getBytes(StandardCharsets.UTF_8));
            String payload =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString("{\"sub\":\"user123\"}".getBytes(StandardCharsets.UTF_8));
            String invalidJwt = headerWithoutKid + "." + payload + ".signature";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("kid");
        }

        @Test
        @DisplayName("유효하지 않은 Base64 헤더면 예외 발생")
        void shouldThrowExceptionWhenHeaderIsNotValidBase64() {
            // given
            String invalidJwt = "!!!invalid-base64!!!.payload.signature";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("Failed to decode JWT Header");
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getValue()가 원본 JWT 반환")
        void shouldReturnOriginalJwtValue() {
            // given
            String jwt = createValidJwt(VALID_KID);
            AccessToken accessToken = AccessToken.of(jwt);

            // when
            String value = accessToken.getValue();

            // then
            assertThat(value).isEqualTo(jwt);
        }

        @Test
        @DisplayName("getKid()가 올바른 kid 반환")
        void shouldReturnCorrectKid() {
            // given
            String expectedKid = "my-special-key-id";
            String jwt = createValidJwt(expectedKid);
            AccessToken accessToken = AccessToken.of(jwt);

            // when
            String kid = accessToken.getKid();

            // then
            assertThat(kid).isEqualTo(expectedKid);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 JWT 값이면 equals true")
        void shouldBeEqualWhenSameJwtValue() {
            // given
            String jwt = createValidJwt(VALID_KID);
            AccessToken token1 = AccessToken.of(jwt);
            AccessToken token2 = AccessToken.of(jwt);

            // when & then
            assertThat(token1).isEqualTo(token2);
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }

        @Test
        @DisplayName("다른 JWT 값이면 equals false")
        void shouldNotBeEqualWhenDifferentJwtValue() {
            // given
            AccessToken token1 = AccessToken.of(createValidJwt("kid1"));
            AccessToken token2 = AccessToken.of(createValidJwt("kid2"));

            // when & then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            AccessToken token = AccessToken.of(createValidJwt(VALID_KID));

            // when & then
            assertThat(token).isEqualTo(token);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            AccessToken token = AccessToken.of(createValidJwt(VALID_KID));

            // when & then
            assertThat(token).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            AccessToken token = AccessToken.of(createValidJwt(VALID_KID));

            // when & then
            assertThat(token).isNotEqualTo("not an AccessToken");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString에 kid가 포함됨")
        void shouldIncludeKidInToString() {
            // given
            AccessToken token = AccessToken.of(createValidJwt(VALID_KID));

            // when
            String result = token.toString();

            // then
            assertThat(result).contains("AccessToken");
            assertThat(result).contains("kid='" + VALID_KID + "'");
        }

        @Test
        @DisplayName("toString에 토큰 값이 포함되지 않음 (보안)")
        void shouldNotIncludeTokenValueInToString() {
            // given
            String jwt = createValidJwt(VALID_KID);
            AccessToken token = AccessToken.of(jwt);

            // when
            String result = token.toString();

            // then
            assertThat(result).doesNotContain(jwt);
            assertThat(result).doesNotContain("eyJ");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(AccessToken.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() throws Exception {
            java.lang.reflect.Field[] fields = AccessToken.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                            .as("Field '%s' should be final", field.getName())
                            .isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("다양한 JWT 형식 테스트")
    class VariousJwtFormatsTest {

        @Test
        @DisplayName("실제 RS256 JWT 형식 처리")
        void shouldHandleRealJwtFormat() {
            // given - 실제 JWT 형식과 유사한 토큰
            String header = createValidJwtHeader("real-production-key");
            String payload =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    "{\"sub\":\"user@example.com\",\"iss\":\"auth.example.com\",\"exp\":1700000000,\"iat\":1699900000}"
                                            .getBytes(StandardCharsets.UTF_8));
            String signature = "very_long_signature_that_would_normally_be_256_bytes";
            String jwt = header + "." + payload + "." + signature;

            // when
            AccessToken token = AccessToken.of(jwt);

            // then
            assertThat(token.getKid()).isEqualTo("real-production-key");
            assertThat(token.getValue()).isEqualTo(jwt);
        }

        @Test
        @DisplayName("특수 문자가 포함된 kid 처리")
        void shouldHandleKidWithSpecialCharacters() {
            // given
            String specialKid = "key_2024-01-prod.v1";
            String jwt = createValidJwt(specialKid);

            // when
            AccessToken token = AccessToken.of(jwt);

            // then
            assertThat(token.getKid()).isEqualTo(specialKid);
        }

        @Test
        @DisplayName("UUID 형식 kid 처리")
        void shouldHandleUuidKid() {
            // given
            String uuidKid = "550e8400-e29b-41d4-a716-446655440000";
            String jwt = createValidJwt(uuidKid);

            // when
            AccessToken token = AccessToken.of(jwt);

            // then
            assertThat(token.getKid()).isEqualTo(uuidKid);
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("빈 kid면 예외 발생")
        void shouldThrowExceptionWhenKidIsEmpty() {
            // given
            String headerWithEmptyKid =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"\"}"
                                            .getBytes(StandardCharsets.UTF_8));
            String payload =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString("{\"sub\":\"user123\"}".getBytes(StandardCharsets.UTF_8));
            String jwt = headerWithEmptyKid + "." + payload + ".signature";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(jwt))
                    .isInstanceOf(JwtInvalidException.class)
                    .hasMessageContaining("Failed to extract 'kid'");
        }

        @Test
        @DisplayName("점만 있는 문자열이면 예외 발생")
        void shouldThrowExceptionWhenOnlyDots() {
            // given
            String invalidJwt = "..";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(invalidJwt))
                    .isInstanceOf(JwtInvalidException.class);
        }

        @Test
        @DisplayName("추가 공백이 있는 토큰이면 예외 발생")
        void shouldThrowExceptionWhenTokenHasExtraSpaces() {
            // given
            String jwtWithSpaces = " " + createValidJwt(VALID_KID) + " ";

            // when & then
            assertThatThrownBy(() -> AccessToken.of(jwtWithSpaces))
                    .isInstanceOf(JwtInvalidException.class);
        }
    }
}
