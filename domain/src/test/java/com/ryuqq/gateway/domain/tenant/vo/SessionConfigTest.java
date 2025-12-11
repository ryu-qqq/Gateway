package com.ryuqq.gateway.domain.tenant.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SessionConfig VO 테스트")
class SessionConfigTest {

    @Nested
    @DisplayName("of() 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("유효한 값으로 SessionConfig 생성 성공")
        void shouldCreateWithValidValues() {
            // given
            int maxActiveSessions = 5;
            Duration accessTokenTTL = Duration.ofMinutes(15);
            Duration refreshTokenTTL = Duration.ofDays(7);

            // when
            SessionConfig config =
                    SessionConfig.of(maxActiveSessions, accessTokenTTL, refreshTokenTTL);

            // then
            assertThat(config).isNotNull();
            assertThat(config.maxActiveSessions()).isEqualTo(5);
            assertThat(config.accessTokenTTL()).isEqualTo(Duration.ofMinutes(15));
            assertThat(config.refreshTokenTTL()).isEqualTo(Duration.ofDays(7));
        }

        @Test
        @DisplayName("maxActiveSessions가 1인 경우 성공")
        void shouldCreateWithMinimalMaxActiveSessions() {
            // when
            SessionConfig config =
                    SessionConfig.of(1, Duration.ofMinutes(1), Duration.ofMinutes(1));

            // then
            assertThat(config.maxActiveSessions()).isEqualTo(1);
        }

        @Test
        @DisplayName("maxActiveSessions가 0이면 예외 발생")
        void shouldThrowExceptionWhenMaxActiveSessionsIsZero() {
            assertThatThrownBy(
                            () -> SessionConfig.of(0, Duration.ofMinutes(15), Duration.ofDays(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxActiveSessions must be positive");
        }

        @Test
        @DisplayName("maxActiveSessions가 음수면 예외 발생")
        void shouldThrowExceptionWhenMaxActiveSessionsIsNegative() {
            assertThatThrownBy(
                            () -> SessionConfig.of(-1, Duration.ofMinutes(15), Duration.ofDays(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxActiveSessions must be positive");
        }

        @Test
        @DisplayName("accessTokenTTL이 null이면 예외 발생")
        void shouldThrowExceptionWhenAccessTokenTTLIsNull() {
            assertThatThrownBy(() -> SessionConfig.of(5, null, Duration.ofDays(7)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("accessTokenTTL cannot be null");
        }

        @Test
        @DisplayName("accessTokenTTL이 0이면 예외 발생")
        void shouldThrowExceptionWhenAccessTokenTTLIsZero() {
            assertThatThrownBy(() -> SessionConfig.of(5, Duration.ZERO, Duration.ofDays(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accessTokenTTL must be positive");
        }

        @Test
        @DisplayName("accessTokenTTL이 음수면 예외 발생")
        void shouldThrowExceptionWhenAccessTokenTTLIsNegative() {
            assertThatThrownBy(
                            () -> SessionConfig.of(5, Duration.ofMinutes(-15), Duration.ofDays(7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("accessTokenTTL must be positive");
        }

        @Test
        @DisplayName("refreshTokenTTL이 null이면 예외 발생")
        void shouldThrowExceptionWhenRefreshTokenTTLIsNull() {
            assertThatThrownBy(() -> SessionConfig.of(5, Duration.ofMinutes(15), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("refreshTokenTTL cannot be null");
        }

        @Test
        @DisplayName("refreshTokenTTL이 0이면 예외 발생")
        void shouldThrowExceptionWhenRefreshTokenTTLIsZero() {
            assertThatThrownBy(() -> SessionConfig.of(5, Duration.ofMinutes(15), Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("refreshTokenTTL must be positive");
        }

        @Test
        @DisplayName("refreshTokenTTL이 음수면 예외 발생")
        void shouldThrowExceptionWhenRefreshTokenTTLIsNegative() {
            assertThatThrownBy(
                            () -> SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(-7)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("refreshTokenTTL must be positive");
        }
    }

    @Nested
    @DisplayName("defaultConfig() 팩토리 메서드 테스트")
    class DefaultConfigTest {

        @Test
        @DisplayName("기본 설정값 확인")
        void shouldCreateWithDefaultValues() {
            // when
            SessionConfig config = SessionConfig.defaultConfig();

            // then
            assertThat(config.maxActiveSessions()).isEqualTo(5);
            assertThat(config.accessTokenTTL()).isEqualTo(Duration.ofMinutes(15));
            assertThat(config.refreshTokenTTL()).isEqualTo(Duration.ofDays(7));
        }
    }

    @Nested
    @DisplayName("ofSeconds() 팩토리 메서드 테스트")
    class OfSecondsTest {

        @Test
        @DisplayName("초 단위로 SessionConfig 생성 성공")
        void shouldCreateWithSecondsValues() {
            // given
            int maxActiveSessions = 10;
            long accessTokenTTLSeconds = 900; // 15분
            long refreshTokenTTLSeconds = 604800; // 7일

            // when
            SessionConfig config =
                    SessionConfig.ofSeconds(
                            maxActiveSessions, accessTokenTTLSeconds, refreshTokenTTLSeconds);

            // then
            assertThat(config.maxActiveSessions()).isEqualTo(10);
            assertThat(config.accessTokenTTL()).isEqualTo(Duration.ofSeconds(900));
            assertThat(config.refreshTokenTTL()).isEqualTo(Duration.ofSeconds(604800));
        }
    }

    @Nested
    @DisplayName("canCreateNewSession() 메서드 테스트")
    class CanCreateNewSessionTest {

        @Test
        @DisplayName("현재 세션 수가 최대치보다 작으면 true")
        void shouldReturnTrueWhenBelowMax() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config.canCreateNewSession(0)).isTrue();
            assertThat(config.canCreateNewSession(1)).isTrue();
            assertThat(config.canCreateNewSession(4)).isTrue();
        }

        @Test
        @DisplayName("현재 세션 수가 최대치와 같으면 false")
        void shouldReturnFalseWhenAtMax() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config.canCreateNewSession(5)).isFalse();
        }

        @Test
        @DisplayName("현재 세션 수가 최대치보다 크면 false")
        void shouldReturnFalseWhenAboveMax() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config.canCreateNewSession(10)).isFalse();
        }

        @Test
        @DisplayName("경계값 테스트 - maxActiveSessions가 1인 경우")
        void shouldHandleEdgeCaseWithOneSession() {
            // given
            SessionConfig config = SessionConfig.of(1, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config.canCreateNewSession(0)).isTrue();
            assertThat(config.canCreateNewSession(1)).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 초 단위 변환 메서드 테스트")
    class TTLSecondsTest {

        @Test
        @DisplayName("accessTokenTTLSeconds() 반환값 확인")
        void shouldReturnAccessTokenTTLInSeconds() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when
            long seconds = config.accessTokenTTLSeconds();

            // then
            assertThat(seconds).isEqualTo(900); // 15분 = 900초
        }

        @Test
        @DisplayName("refreshTokenTTLSeconds() 반환값 확인")
        void shouldReturnRefreshTokenTTLInSeconds() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when
            long seconds = config.refreshTokenTTLSeconds();

            // then
            assertThat(seconds).isEqualTo(604800); // 7일 = 604800초
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 값이면 equals true")
        void shouldBeEqualWhenSameValues() {
            // given
            SessionConfig config1 = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
            SessionConfig config2 = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("다른 maxActiveSessions면 equals false")
        void shouldNotBeEqualWhenDifferentMaxActiveSessions() {
            // given
            SessionConfig config1 = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
            SessionConfig config2 =
                    SessionConfig.of(10, Duration.ofMinutes(15), Duration.ofDays(7));

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("다른 accessTokenTTL면 equals false")
        void shouldNotBeEqualWhenDifferentAccessTokenTTL() {
            // given
            SessionConfig config1 = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
            SessionConfig config2 = SessionConfig.of(5, Duration.ofMinutes(30), Duration.ofDays(7));

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("다른 refreshTokenTTL면 equals false")
        void shouldNotBeEqualWhenDifferentRefreshTokenTTL() {
            // given
            SessionConfig config1 = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
            SessionConfig config2 =
                    SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(14));

            // when & then
            assertThat(config1).isNotEqualTo(config2);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 SessionConfig 정보 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            SessionConfig config = SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));

            // when
            String result = config.toString();

            // then
            assertThat(result).contains("SessionConfig");
            assertThat(result).contains("maxActiveSessions=5");
            assertThat(result).contains("accessTokenTTL");
            assertThat(result).contains("refreshTokenTTL");
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("record 클래스는 final임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(SessionConfig.class.getModifiers()))
                    .isTrue();
        }
    }
}
