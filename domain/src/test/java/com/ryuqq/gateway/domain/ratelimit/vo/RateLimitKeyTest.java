package com.ryuqq.gateway.domain.ratelimit.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitKey VO 테스트")
class RateLimitKeyTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 키 생성")
        void shouldCreateValidKey() {
            // when
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:endpoint:/api/v1/orders:GET");

            // then
            assertThat(key).isNotNull();
            assertThat(key.value()).isEqualTo("gateway:rate_limit:endpoint:/api/v1/orders:GET");
        }

        @Test
        @DisplayName("null 값이면 예외 발생")
        void shouldThrowExceptionWhenNull() {
            assertThatThrownBy(() -> RateLimitKey.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("key cannot be null");
        }

        @Test
        @DisplayName("빈 문자열이면 예외 발생")
        void shouldThrowExceptionWhenEmpty() {
            assertThatThrownBy(() -> RateLimitKey.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("key cannot be blank");
        }

        @Test
        @DisplayName("공백만 있으면 예외 발생")
        void shouldThrowExceptionWhenBlank() {
            assertThatThrownBy(() -> RateLimitKey.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("key cannot be blank");
        }
    }

    @Nested
    @DisplayName("LimitType으로 생성 테스트")
    class CreateFromLimitTypeTest {

        @Test
        @DisplayName("ENDPOINT 타입으로 키 생성")
        void shouldCreateKeyFromEndpointType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.ENDPOINT, "/api/v1/orders", "GET");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:endpoint:/api/v1/orders:GET");
        }

        @Test
        @DisplayName("USER 타입으로 키 생성")
        void shouldCreateKeyFromUserType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.USER, "user-123");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:user:user-123");
        }

        @Test
        @DisplayName("IP 타입으로 키 생성")
        void shouldCreateKeyFromIpType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.IP, "192.168.1.1");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:ip:192.168.1.1");
        }

        @Test
        @DisplayName("OTP 타입으로 키 생성")
        void shouldCreateKeyFromOtpType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.OTP, "01012345678");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:otp:01012345678");
        }

        @Test
        @DisplayName("LOGIN 타입으로 키 생성")
        void shouldCreateKeyFromLoginType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.LOGIN, "192.168.1.1");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:login:192.168.1.1");
        }

        @Test
        @DisplayName("TOKEN_REFRESH 타입으로 키 생성")
        void shouldCreateKeyFromTokenRefreshType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.TOKEN_REFRESH, "user-123");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:token_refresh:user-123");
        }

        @Test
        @DisplayName("INVALID_JWT 타입으로 키 생성")
        void shouldCreateKeyFromInvalidJwtType() {
            // when
            RateLimitKey key = RateLimitKey.of(LimitType.INVALID_JWT, "192.168.1.1");

            // then
            assertThat(key.value()).isEqualTo("gateway:rate_limit:invalid_jwt:192.168.1.1");
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValue() {
            // given
            RateLimitKey key1 = RateLimitKey.of("gateway:rate_limit:user:user-123");
            RateLimitKey key2 = RateLimitKey.of("gateway:rate_limit:user:user-123");

            // when & then
            assertThat(key1).isEqualTo(key2);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValue() {
            // given
            RateLimitKey key1 = RateLimitKey.of("gateway:rate_limit:user:user-123");
            RateLimitKey key2 = RateLimitKey.of("gateway:rate_limit:user:user-456");

            // when & then
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        @DisplayName("null과 equals false")
        void shouldNotBeEqualToNull() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");

            // when & then
            assertThat(key).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 equals false")
        void shouldNotBeEqualToDifferentType() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");

            // when & then
            assertThat(key).isNotEqualTo("gateway:rate_limit:user:user-123");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 키 값 정보 포함")
        void shouldIncludeValueInToString() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");

            // when
            String result = key.toString();

            // then
            assertThat(result).contains("RateLimitKey");
            assertThat(result).contains("gateway:rate_limit:user:user-123");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(RateLimitKey.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = RateLimitKey.class.getDeclaredFields();
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
