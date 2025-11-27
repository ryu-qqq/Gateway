package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenPair VO 테스트")
class TokenPairTest {

    private static final String VALID_REFRESH_TOKEN = "a".repeat(32);

    private String createValidJwt(String kid) {
        String headerJson =
                String.format("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"%s\"}", kid);
        String header =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                "{\"sub\":\"user123\",\"exp\":1700000000}"
                                        .getBytes(StandardCharsets.UTF_8));
        String signature = "dummy-signature";
        return header + "." + payload + "." + signature;
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("유효한 Access Token과 Refresh Token으로 생성 성공")
        void shouldCreateTokenPairWithValidTokens() {
            // given
            AccessToken accessToken = AccessToken.of(createValidJwt("test-kid"));
            RefreshToken refreshToken = RefreshToken.of(VALID_REFRESH_TOKEN);

            // when
            TokenPair tokenPair = TokenPair.of(accessToken, refreshToken);

            // then
            assertThat(tokenPair).isNotNull();
            assertThat(tokenPair.getAccessToken()).isEqualTo(accessToken);
            assertThat(tokenPair.getRefreshToken()).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("문자열로 생성 성공")
        void shouldCreateTokenPairFromStrings() {
            // given
            String accessTokenValue = createValidJwt("test-kid");
            String refreshTokenValue = VALID_REFRESH_TOKEN;

            // when
            TokenPair tokenPair = TokenPair.of(accessTokenValue, refreshTokenValue);

            // then
            assertThat(tokenPair.getAccessToken().getValue()).isEqualTo(accessTokenValue);
            assertThat(tokenPair.getRefreshToken().getValue()).isEqualTo(refreshTokenValue);
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("Access Token이 null이면 예외 발생")
        void shouldThrowExceptionWhenAccessTokenIsNull() {
            // given
            RefreshToken refreshToken = RefreshToken.of(VALID_REFRESH_TOKEN);

            // when & then
            assertThatThrownBy(() -> TokenPair.of((AccessToken) null, refreshToken))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Access token");
        }

        @Test
        @DisplayName("Refresh Token이 null이면 예외 발생")
        void shouldThrowExceptionWhenRefreshTokenIsNull() {
            // given
            AccessToken accessToken = AccessToken.of(createValidJwt("test-kid"));

            // when & then
            assertThatThrownBy(() -> TokenPair.of(accessToken, (RefreshToken) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Refresh token");
        }

        @Test
        @DisplayName("둘 다 null이면 Access Token 예외 발생")
        void shouldThrowExceptionWhenBothNull() {
            // when & then
            assertThatThrownBy(() -> TokenPair.of((AccessToken) null, (RefreshToken) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Access token");
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getAccessToken()이 올바른 값 반환")
        void shouldReturnCorrectAccessToken() {
            // given
            AccessToken accessToken = AccessToken.of(createValidJwt("test-kid"));
            RefreshToken refreshToken = RefreshToken.of(VALID_REFRESH_TOKEN);
            TokenPair tokenPair = TokenPair.of(accessToken, refreshToken);

            // when
            AccessToken result = tokenPair.getAccessToken();

            // then
            assertThat(result).isSameAs(accessToken);
        }

        @Test
        @DisplayName("getRefreshToken()이 올바른 값 반환")
        void shouldReturnCorrectRefreshToken() {
            // given
            AccessToken accessToken = AccessToken.of(createValidJwt("test-kid"));
            RefreshToken refreshToken = RefreshToken.of(VALID_REFRESH_TOKEN);
            TokenPair tokenPair = TokenPair.of(accessToken, refreshToken);

            // when
            RefreshToken result = tokenPair.getRefreshToken();

            // then
            assertThat(result).isSameAs(refreshToken);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 토큰 쌍이면 equals true")
        void shouldBeEqualWhenSameTokenPair() {
            // given
            String jwt = createValidJwt("test-kid");
            TokenPair pair1 = TokenPair.of(jwt, VALID_REFRESH_TOKEN);
            TokenPair pair2 = TokenPair.of(jwt, VALID_REFRESH_TOKEN);

            // when & then
            assertThat(pair1).isEqualTo(pair2);
            assertThat(pair1.hashCode()).isEqualTo(pair2.hashCode());
        }

        @Test
        @DisplayName("다른 Access Token이면 equals false")
        void shouldNotBeEqualWhenDifferentAccessToken() {
            // given
            TokenPair pair1 = TokenPair.of(createValidJwt("kid1"), VALID_REFRESH_TOKEN);
            TokenPair pair2 = TokenPair.of(createValidJwt("kid2"), VALID_REFRESH_TOKEN);

            // when & then
            assertThat(pair1).isNotEqualTo(pair2);
        }

        @Test
        @DisplayName("다른 Refresh Token이면 equals false")
        void shouldNotBeEqualWhenDifferentRefreshToken() {
            // given
            String jwt = createValidJwt("test-kid");
            TokenPair pair1 = TokenPair.of(jwt, "a".repeat(32));
            TokenPair pair2 = TokenPair.of(jwt, "b".repeat(32));

            // when & then
            assertThat(pair1).isNotEqualTo(pair2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            TokenPair pair = TokenPair.of(createValidJwt("test-kid"), VALID_REFRESH_TOKEN);

            // when & then
            assertThat(pair).isEqualTo(pair);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            TokenPair pair = TokenPair.of(createValidJwt("test-kid"), VALID_REFRESH_TOKEN);

            // when & then
            assertThat(pair).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            TokenPair pair = TokenPair.of(createValidJwt("test-kid"), VALID_REFRESH_TOKEN);

            // when & then
            assertThat(pair).isNotEqualTo("not a TokenPair");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString에 토큰 값이 포함되지 않음 (보안)")
        void shouldNotIncludeTokenValuesInToString() {
            // given
            String jwt = createValidJwt("test-kid");
            TokenPair pair = TokenPair.of(jwt, VALID_REFRESH_TOKEN);

            // when
            String result = pair.toString();

            // then
            assertThat(result).contains("TokenPair");
            assertThat(result).doesNotContain(jwt);
            assertThat(result).doesNotContain(VALID_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(TokenPair.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = TokenPair.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (!field.isSynthetic()
                        && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                            .as("Field '%s' should be final", field.getName())
                            .isTrue();
                }
            }
        }
    }
}
