package com.ryuqq.gateway.domain.ratelimit.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitPolicy VO 테스트")
class RateLimitPolicyTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 정책 생성")
        void shouldCreateValidPolicy() {
            // when
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            1000,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // then
            assertThat(policy).isNotNull();
            assertThat(policy.limitType()).isEqualTo(LimitType.ENDPOINT);
            assertThat(policy.maxRequests()).isEqualTo(1000);
            assertThat(policy.window()).isEqualTo(Duration.ofMinutes(1));
            assertThat(policy.action()).isEqualTo(RateLimitAction.REJECT);
            assertThat(policy.auditLogRequired()).isFalse();
        }

        @Test
        @DisplayName("Audit Log 필수 정책 생성")
        void shouldCreatePolicyWithAuditLogRequired() {
            // when
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.OTP, 3, Duration.ofHours(1), RateLimitAction.REJECT, true);

            // then
            assertThat(policy.auditLogRequired()).isTrue();
        }

        @Test
        @DisplayName("maxRequests가 0 이하이면 예외 발생")
        void shouldThrowExceptionWhenMaxRequestsIsZeroOrNegative() {
            assertThatThrownBy(
                            () ->
                                    RateLimitPolicy.of(
                                            LimitType.ENDPOINT,
                                            0,
                                            Duration.ofMinutes(1),
                                            RateLimitAction.REJECT,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxRequests must be positive");
        }

        @Test
        @DisplayName("window가 null이면 예외 발생")
        void shouldThrowExceptionWhenWindowIsNull() {
            assertThatThrownBy(
                            () ->
                                    RateLimitPolicy.of(
                                            LimitType.ENDPOINT,
                                            1000,
                                            null,
                                            RateLimitAction.REJECT,
                                            false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("window cannot be null");
        }

        @Test
        @DisplayName("window가 0 이하이면 예외 발생")
        void shouldThrowExceptionWhenWindowIsZeroOrNegative() {
            assertThatThrownBy(
                            () ->
                                    RateLimitPolicy.of(
                                            LimitType.ENDPOINT,
                                            1000,
                                            Duration.ZERO,
                                            RateLimitAction.REJECT,
                                            false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("window must be positive");
        }

        @Test
        @DisplayName("limitType이 null이면 예외 발생")
        void shouldThrowExceptionWhenLimitTypeIsNull() {
            assertThatThrownBy(
                            () ->
                                    RateLimitPolicy.of(
                                            null,
                                            1000,
                                            Duration.ofMinutes(1),
                                            RateLimitAction.REJECT,
                                            false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("limitType cannot be null");
        }

        @Test
        @DisplayName("action이 null이면 예외 발생")
        void shouldThrowExceptionWhenActionIsNull() {
            assertThatThrownBy(
                            () ->
                                    RateLimitPolicy.of(
                                            LimitType.ENDPOINT,
                                            1000,
                                            Duration.ofMinutes(1),
                                            null,
                                            false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("action cannot be null");
        }
    }

    @Nested
    @DisplayName("기본 정책 생성 테스트")
    class DefaultPolicyTest {

        @Test
        @DisplayName("ENDPOINT 기본 정책 생성")
        void shouldCreateDefaultEndpointPolicy() {
            // when
            RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(LimitType.ENDPOINT);

            // then
            assertThat(policy.limitType()).isEqualTo(LimitType.ENDPOINT);
            assertThat(policy.maxRequests()).isEqualTo(1000);
            assertThat(policy.window()).isEqualTo(Duration.ofMinutes(1));
            assertThat(policy.action()).isEqualTo(RateLimitAction.REJECT);
            assertThat(policy.auditLogRequired()).isFalse();
        }

        @Test
        @DisplayName("OTP 기본 정책 생성")
        void shouldCreateDefaultOtpPolicy() {
            // when
            RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(LimitType.OTP);

            // then
            assertThat(policy.limitType()).isEqualTo(LimitType.OTP);
            assertThat(policy.maxRequests()).isEqualTo(3);
            assertThat(policy.window()).isEqualTo(Duration.ofHours(1));
            assertThat(policy.action()).isEqualTo(RateLimitAction.REJECT);
            assertThat(policy.auditLogRequired()).isTrue();
        }

        @Test
        @DisplayName("LOGIN 기본 정책 생성")
        void shouldCreateDefaultLoginPolicy() {
            // when
            RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(LimitType.LOGIN);

            // then
            assertThat(policy.limitType()).isEqualTo(LimitType.LOGIN);
            assertThat(policy.maxRequests()).isEqualTo(5);
            assertThat(policy.window()).isEqualTo(Duration.ofMinutes(5));
            assertThat(policy.action()).isEqualTo(RateLimitAction.BLOCK_IP);
            assertThat(policy.auditLogRequired()).isTrue();
        }

        @Test
        @DisplayName("TOKEN_REFRESH 기본 정책 생성")
        void shouldCreateDefaultTokenRefreshPolicy() {
            // when
            RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(LimitType.TOKEN_REFRESH);

            // then
            assertThat(policy.limitType()).isEqualTo(LimitType.TOKEN_REFRESH);
            assertThat(policy.maxRequests()).isEqualTo(3);
            assertThat(policy.window()).isEqualTo(Duration.ofMinutes(1));
            assertThat(policy.action()).isEqualTo(RateLimitAction.REVOKE_TOKEN);
            assertThat(policy.auditLogRequired()).isTrue();
        }

        @Test
        @DisplayName("INVALID_JWT 기본 정책 생성")
        void shouldCreateDefaultInvalidJwtPolicy() {
            // when
            RateLimitPolicy policy = RateLimitPolicy.defaultPolicy(LimitType.INVALID_JWT);

            // then
            assertThat(policy.limitType()).isEqualTo(LimitType.INVALID_JWT);
            assertThat(policy.maxRequests()).isEqualTo(10);
            assertThat(policy.window()).isEqualTo(Duration.ofMinutes(5));
            assertThat(policy.action()).isEqualTo(RateLimitAction.BLOCK_IP);
            assertThat(policy.auditLogRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("isExceeded 메서드 테스트")
    class IsExceededTest {

        @Test
        @DisplayName("현재 카운트가 최대값 미만이면 false")
        void shouldReturnFalseWhenCountIsBelowLimit() {
            // given
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            100,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then
            assertThat(policy.isExceeded(99)).isFalse();
        }

        @Test
        @DisplayName("현재 카운트가 최대값 이상이면 true (경계값 포함)")
        void shouldReturnTrueWhenCountReachesOrExceedsLimit() {
            // given
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            100,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then - maxRequests=100일 때 100번째 요청부터 차단
            assertThat(policy.isExceeded(100)).isTrue();
            assertThat(policy.isExceeded(101)).isTrue();
            assertThat(policy.isExceeded(1000)).isTrue();
        }
    }

    @Nested
    @DisplayName("calculateRemaining 메서드 테스트")
    class CalculateRemainingTest {

        @Test
        @DisplayName("남은 요청 수 계산")
        void shouldCalculateRemainingRequests() {
            // given
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            100,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then
            assertThat(policy.calculateRemaining(0)).isEqualTo(100);
            assertThat(policy.calculateRemaining(50)).isEqualTo(50);
            assertThat(policy.calculateRemaining(100)).isEqualTo(0);
        }

        @Test
        @DisplayName("초과 시 0 반환")
        void shouldReturnZeroWhenExceeded() {
            // given
            RateLimitPolicy policy =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            100,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then
            assertThat(policy.calculateRemaining(101)).isEqualTo(0);
            assertThat(policy.calculateRemaining(200)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValues() {
            // given
            RateLimitPolicy policy1 =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            1000,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);
            RateLimitPolicy policy2 =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            1000,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then
            assertThat(policy1).isEqualTo(policy2);
            assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
        }

        @Test
        @DisplayName("다른 값이면 equals false")
        void shouldNotBeEqualWhenDifferentValues() {
            // given
            RateLimitPolicy policy1 =
                    RateLimitPolicy.of(
                            LimitType.ENDPOINT,
                            1000,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);
            RateLimitPolicy policy2 =
                    RateLimitPolicy.of(
                            LimitType.USER,
                            100,
                            Duration.ofMinutes(1),
                            RateLimitAction.REJECT,
                            false);

            // when & then
            assertThat(policy1).isNotEqualTo(policy2);
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(RateLimitPolicy.class.getModifiers()))
                    .isTrue();
        }

        @Test
        @DisplayName("모든 필드가 final임")
        void shouldHaveAllFinalFields() {
            java.lang.reflect.Field[] fields = RateLimitPolicy.class.getDeclaredFields();
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
