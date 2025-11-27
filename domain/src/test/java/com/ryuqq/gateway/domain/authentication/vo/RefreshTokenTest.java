package com.ryuqq.gateway.domain.authentication.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken VO 테스트")
class RefreshTokenTest {

    private static final String VALID_REFRESH_TOKEN =
            "dGVzdC1yZWZyZXNoLXRva2VuLXZhbHVlLXRoYXQtaXMtbG9uZy1lbm91Z2g";

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 생성 성공")
        void shouldCreateRefreshTokenWithValidValue() {
            // given
            String validToken = VALID_REFRESH_TOKEN;

            // when
            RefreshToken refreshToken = RefreshToken.of(validToken);

            // then
            assertThat(refreshToken).isNotNull();
            assertThat(refreshToken.getValue()).isEqualTo(validToken);
        }

        @Test
        @DisplayName("32자 이상의 토큰으로 생성 성공")
        void shouldCreateRefreshTokenWithMinimumLength() {
            // given - 정확히 32자
            String token32Chars = "a".repeat(32);

            // when
            RefreshToken refreshToken = RefreshToken.of(token32Chars);

            // then
            assertThat(refreshToken.getValue()).isEqualTo(token32Chars);
        }

        @Test
        @DisplayName("긴 토큰으로 생성 성공")
        void shouldCreateRefreshTokenWithLongValue() {
            // given
            String longToken = "a".repeat(256);

            // when
            RefreshToken refreshToken = RefreshToken.of(longToken);

            // then
            assertThat(refreshToken.getValue()).isEqualTo(longToken);
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null 토큰이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsNull() {
            // when & then
            assertThatThrownBy(() -> RefreshToken.of(null))
                    .isInstanceOf(RefreshTokenInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsEmpty() {
            // when & then
            assertThatThrownBy(() -> RefreshToken.of(""))
                    .isInstanceOf(RefreshTokenInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("공백만 있으면 예외 발생")
        void shouldThrowExceptionWhenTokenIsBlank() {
            // when & then
            assertThatThrownBy(() -> RefreshToken.of("   "))
                    .isInstanceOf(RefreshTokenInvalidException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("32자 미만이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsTooShort() {
            // given - 31자
            String shortToken = "a".repeat(31);

            // when & then
            assertThatThrownBy(() -> RefreshToken.of(shortToken))
                    .isInstanceOf(RefreshTokenInvalidException.class)
                    .hasMessageContaining("at least 32 characters");
        }

        @Test
        @DisplayName("1자 토큰이면 예외 발생")
        void shouldThrowExceptionWhenTokenIsSingleChar() {
            // when & then
            assertThatThrownBy(() -> RefreshToken.of("a"))
                    .isInstanceOf(RefreshTokenInvalidException.class)
                    .hasMessageContaining("at least 32 characters");
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("getValue()가 원본 토큰 반환")
        void shouldReturnOriginalTokenValue() {
            // given
            String token = VALID_REFRESH_TOKEN;
            RefreshToken refreshToken = RefreshToken.of(token);

            // when
            String value = refreshToken.getValue();

            // then
            assertThat(value).isEqualTo(token);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValue() {
            // given
            String token = VALID_REFRESH_TOKEN;
            RefreshToken token1 = RefreshToken.of(token);
            RefreshToken token2 = RefreshToken.of(token);

            // when & then
            assertThat(token1).isEqualTo(token2);
            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValue() {
            // given
            RefreshToken token1 = RefreshToken.of("a".repeat(32));
            RefreshToken token2 = RefreshToken.of("b".repeat(32));

            // when & then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("자기 자신과 equals true")
        void shouldBeEqualToItself() {
            // given
            RefreshToken token = RefreshToken.of(VALID_REFRESH_TOKEN);

            // when & then
            assertThat(token).isEqualTo(token);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            RefreshToken token = RefreshToken.of(VALID_REFRESH_TOKEN);

            // when & then
            assertThat(token).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            RefreshToken token = RefreshToken.of(VALID_REFRESH_TOKEN);

            // when & then
            assertThat(token).isNotEqualTo("not a RefreshToken");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString에 토큰 값이 포함되지 않음 (보안)")
        void shouldNotIncludeTokenValueInToString() {
            // given
            String token = VALID_REFRESH_TOKEN;
            RefreshToken refreshToken = RefreshToken.of(token);

            // when
            String result = refreshToken.toString();

            // then
            assertThat(result).contains("RefreshToken");
            assertThat(result).doesNotContain(token);
            assertThat(result).contains("masked");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(RefreshToken.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = RefreshToken.class.getDeclaredFields();
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

    @Nested
    @DisplayName("다양한 형식 테스트")
    class VariousFormatsTest {

        @Test
        @DisplayName("Base64 인코딩 형식 토큰 처리")
        void shouldHandleBase64EncodedToken() {
            // given
            String base64Token = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkw";

            // when
            RefreshToken token = RefreshToken.of(base64Token);

            // then
            assertThat(token.getValue()).isEqualTo(base64Token);
        }

        @Test
        @DisplayName("UUID 형식 토큰 처리")
        void shouldHandleUuidToken() {
            // given - UUID는 36자
            String uuidToken = "550e8400-e29b-41d4-a716-446655440000";

            // when
            RefreshToken token = RefreshToken.of(uuidToken);

            // then
            assertThat(token.getValue()).isEqualTo(uuidToken);
        }

        @Test
        @DisplayName("특수 문자 포함 토큰 처리")
        void shouldHandleTokenWithSpecialCharacters() {
            // given - 32자 이상
            String tokenWithSpecial = "refresh_token-2024.v1_abc123xyz_extra_padding";

            // when
            RefreshToken token = RefreshToken.of(tokenWithSpecial);

            // then
            assertThat(token.getValue()).isEqualTo(tokenWithSpecial);
        }
    }
}
