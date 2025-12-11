package com.ryuqq.gateway.domain.tenant.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantRateLimitConfig VO 테스트")
class TenantRateLimitConfigTest {

    @Nested
    @DisplayName("of() 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("유효한 값으로 TenantRateLimitConfig 생성 성공")
        void shouldCreateWithValidValues() {
            // given
            int loginAttemptsPerHour = 10;
            int otpRequestsPerHour = 5;

            // when
            TenantRateLimitConfig config = TenantRateLimitConfig.of(loginAttemptsPerHour, otpRequestsPerHour);

            // then
            assertThat(config).isNotNull();
            assertThat(config.loginAttemptsPerHour()).isEqualTo(10);
            assertThat(config.otpRequestsPerHour()).isEqualTo(5);
        }

        @Test
        @DisplayName("loginAttemptsPerHour가 1인 경우 성공")
        void shouldCreateWithMinimalLoginAttempts() {
            // when
            TenantRateLimitConfig config = TenantRateLimitConfig.of(1, 1);

            // then
            assertThat(config.loginAttemptsPerHour()).isEqualTo(1);
        }

        @Test
        @DisplayName("loginAttemptsPerHour가 0이면 예외 발생")
        void shouldThrowExceptionWhenLoginAttemptsIsZero() {
            assertThatThrownBy(() -> TenantRateLimitConfig.of(0, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("loginAttemptsPerHour must be positive");
        }

        @Test
        @DisplayName("loginAttemptsPerHour가 음수면 예외 발생")
        void shouldThrowExceptionWhenLoginAttemptsIsNegative() {
            assertThatThrownBy(() -> TenantRateLimitConfig.of(-10, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("loginAttemptsPerHour must be positive");
        }

        @Test
        @DisplayName("otpRequestsPerHour가 0이면 예외 발생")
        void shouldThrowExceptionWhenOtpRequestsIsZero() {
            assertThatThrownBy(() -> TenantRateLimitConfig.of(10, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("otpRequestsPerHour must be positive");
        }

        @Test
        @DisplayName("otpRequestsPerHour가 음수면 예외 발생")
        void shouldThrowExceptionWhenOtpRequestsIsNegative() {
            assertThatThrownBy(() -> TenantRateLimitConfig.of(10, -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("otpRequestsPerHour must be positive");
        }
    }

    @Nested
    @DisplayName("defaultConfig() 팩토리 메서드 테스트")
    class DefaultConfigTest {

        @Test
        @DisplayName("기본 설정값 확인")
        void shouldCreateWithDefaultValues() {
            // when
            TenantRateLimitConfig config = TenantRateLimitConfig.defaultConfig();

            // then
            assertThat(config.loginAttemptsPerHour()).isEqualTo(10);
            assertThat(config.otpRequestsPerHour()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("isLoginAttemptAllowed() 메서드 테스트")
    class IsLoginAttemptAllowedTest {

        @Test
        @DisplayName("현재 횟수가 한도보다 작으면 true")
        void shouldReturnTrueWhenBelowLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isLoginAttemptAllowed(0)).isTrue();
            assertThat(config.isLoginAttemptAllowed(5)).isTrue();
            assertThat(config.isLoginAttemptAllowed(9)).isTrue();
        }

        @Test
        @DisplayName("현재 횟수가 한도와 같으면 false")
        void shouldReturnFalseWhenAtLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isLoginAttemptAllowed(10)).isFalse();
        }

        @Test
        @DisplayName("현재 횟수가 한도보다 크면 false")
        void shouldReturnFalseWhenAboveLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isLoginAttemptAllowed(15)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOtpRequestAllowed() 메서드 테스트")
    class IsOtpRequestAllowedTest {

        @Test
        @DisplayName("현재 횟수가 한도보다 작으면 true")
        void shouldReturnTrueWhenBelowLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isOtpRequestAllowed(0)).isTrue();
            assertThat(config.isOtpRequestAllowed(2)).isTrue();
            assertThat(config.isOtpRequestAllowed(4)).isTrue();
        }

        @Test
        @DisplayName("현재 횟수가 한도와 같으면 false")
        void shouldReturnFalseWhenAtLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isOtpRequestAllowed(5)).isFalse();
        }

        @Test
        @DisplayName("현재 횟수가 한도보다 크면 false")
        void shouldReturnFalseWhenAboveLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config.isOtpRequestAllowed(10)).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateRemainingLoginAttempts() 메서드 테스트")
    class CalculateRemainingLoginAttemptsTest {

        @Test
        @DisplayName("남은 횟수 계산 - 시도 전")
        void shouldCalculateRemainingWhenNoAttempts() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingLoginAttempts(0);

            // then
            assertThat(remaining).isEqualTo(10);
        }

        @Test
        @DisplayName("남은 횟수 계산 - 일부 시도 후")
        void shouldCalculateRemainingAfterSomeAttempts() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingLoginAttempts(3);

            // then
            assertThat(remaining).isEqualTo(7);
        }

        @Test
        @DisplayName("남은 횟수 계산 - 한도 도달 시 0")
        void shouldReturnZeroWhenAtLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingLoginAttempts(10);

            // then
            assertThat(remaining).isZero();
        }

        @Test
        @DisplayName("남은 횟수 계산 - 한도 초과 시 0 (음수 아님)")
        void shouldReturnZeroWhenAboveLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingLoginAttempts(15);

            // then
            assertThat(remaining).isZero();
        }
    }

    @Nested
    @DisplayName("calculateRemainingOtpRequests() 메서드 테스트")
    class CalculateRemainingOtpRequestsTest {

        @Test
        @DisplayName("남은 OTP 횟수 계산 - 요청 전")
        void shouldCalculateRemainingWhenNoRequests() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingOtpRequests(0);

            // then
            assertThat(remaining).isEqualTo(5);
        }

        @Test
        @DisplayName("남은 OTP 횟수 계산 - 일부 요청 후")
        void shouldCalculateRemainingAfterSomeRequests() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingOtpRequests(2);

            // then
            assertThat(remaining).isEqualTo(3);
        }

        @Test
        @DisplayName("남은 OTP 횟수 계산 - 한도 도달 시 0")
        void shouldReturnZeroWhenAtLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingOtpRequests(5);

            // then
            assertThat(remaining).isZero();
        }

        @Test
        @DisplayName("남은 OTP 횟수 계산 - 한도 초과 시 0 (음수 아님)")
        void shouldReturnZeroWhenAboveLimit() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            int remaining = config.calculateRemainingOtpRequests(10);

            // then
            assertThat(remaining).isZero();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValues() {
            // given
            TenantRateLimitConfig config1 = TenantRateLimitConfig.of(10, 5);
            TenantRateLimitConfig config2 = TenantRateLimitConfig.of(10, 5);

            // when & then
            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("다른 loginAttemptsPerHour면 equals false")
        void shouldNotBeEqualWhenDifferentLoginAttempts() {
            // given
            TenantRateLimitConfig config1 = TenantRateLimitConfig.of(10, 5);
            TenantRateLimitConfig config2 = TenantRateLimitConfig.of(20, 5);

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("다른 otpRequestsPerHour면 equals false")
        void shouldNotBeEqualWhenDifferentOtpRequests() {
            // given
            TenantRateLimitConfig config1 = TenantRateLimitConfig.of(10, 5);
            TenantRateLimitConfig config2 = TenantRateLimitConfig.of(10, 10);

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 TenantRateLimitConfig 정보 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            TenantRateLimitConfig config = TenantRateLimitConfig.of(10, 5);

            // when
            String result = config.toString();

            // then
            assertThat(result).contains("TenantRateLimitConfig");
            assertThat(result).contains("loginAttemptsPerHour=10");
            assertThat(result).contains("otpRequestsPerHour=5");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("record 클래스는 final임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(TenantRateLimitConfig.class.getModifiers()))
                    .isTrue();
        }
    }
}
