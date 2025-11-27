package com.ryuqq.gateway.domain.ratelimit.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LimitType Enum 테스트")
class LimitTypeTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 제한 타입이 정의되어 있음")
        void shouldHaveAllLimitTypes() {
            // when
            LimitType[] limitTypes = LimitType.values();

            // then
            assertThat(limitTypes).hasSize(7);
            assertThat(limitTypes)
                    .containsExactly(
                            LimitType.ENDPOINT,
                            LimitType.USER,
                            LimitType.IP,
                            LimitType.OTP,
                            LimitType.LOGIN,
                            LimitType.TOKEN_REFRESH,
                            LimitType.INVALID_JWT);
        }
    }

    @Nested
    @DisplayName("ENDPOINT 타입 테스트")
    class EndpointTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.ENDPOINT.getKeyPrefix()).isEqualTo("gateway:rate_limit:endpoint");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (1000 req/min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.ENDPOINT.getDefaultMaxRequests()).isEqualTo(1000);
            assertThat(LimitType.ENDPOINT.getDefaultWindow()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("Audit Log가 필수가 아님")
        void shouldNotRequireAuditLog() {
            assertThat(LimitType.ENDPOINT.isAuditLogRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("USER 타입 테스트")
    class UserTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.USER.getKeyPrefix()).isEqualTo("gateway:rate_limit:user");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (100 req/min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.USER.getDefaultMaxRequests()).isEqualTo(100);
            assertThat(LimitType.USER.getDefaultWindow()).isEqualTo(Duration.ofMinutes(1));
        }
    }

    @Nested
    @DisplayName("IP 타입 테스트")
    class IpTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.IP.getKeyPrefix()).isEqualTo("gateway:rate_limit:ip");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (100 req/min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.IP.getDefaultMaxRequests()).isEqualTo(100);
            assertThat(LimitType.IP.getDefaultWindow()).isEqualTo(Duration.ofMinutes(1));
        }
    }

    @Nested
    @DisplayName("OTP 타입 테스트")
    class OtpTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.OTP.getKeyPrefix()).isEqualTo("gateway:rate_limit:otp");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (3 req/hour)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.OTP.getDefaultMaxRequests()).isEqualTo(3);
            assertThat(LimitType.OTP.getDefaultWindow()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("Audit Log가 필수임")
        void shouldRequireAuditLog() {
            assertThat(LimitType.OTP.isAuditLogRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("LOGIN 타입 테스트")
    class LoginTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.LOGIN.getKeyPrefix()).isEqualTo("gateway:rate_limit:login");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (5 req/5min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.LOGIN.getDefaultMaxRequests()).isEqualTo(5);
            assertThat(LimitType.LOGIN.getDefaultWindow()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("Audit Log가 필수임")
        void shouldRequireAuditLog() {
            assertThat(LimitType.LOGIN.isAuditLogRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("TOKEN_REFRESH 타입 테스트")
    class TokenRefreshTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.TOKEN_REFRESH.getKeyPrefix())
                    .isEqualTo("gateway:rate_limit:token_refresh");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (3 req/min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.TOKEN_REFRESH.getDefaultMaxRequests()).isEqualTo(3);
            assertThat(LimitType.TOKEN_REFRESH.getDefaultWindow()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("Audit Log가 필수임")
        void shouldRequireAuditLog() {
            assertThat(LimitType.TOKEN_REFRESH.isAuditLogRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("INVALID_JWT 타입 테스트")
    class InvalidJwtTest {

        @Test
        @DisplayName("올바른 Key Prefix를 가짐")
        void shouldHaveCorrectKeyPrefix() {
            assertThat(LimitType.INVALID_JWT.getKeyPrefix())
                    .isEqualTo("gateway:rate_limit:invalid_jwt");
        }

        @Test
        @DisplayName("올바른 기본 제한을 가짐 (10 req/5min)")
        void shouldHaveCorrectDefaultLimit() {
            assertThat(LimitType.INVALID_JWT.getDefaultMaxRequests()).isEqualTo(10);
            assertThat(LimitType.INVALID_JWT.getDefaultWindow()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("Audit Log가 필수임")
        void shouldRequireAuditLog() {
            assertThat(LimitType.INVALID_JWT.isAuditLogRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("buildKey 메서드 테스트")
    class BuildKeyTest {

        @Test
        @DisplayName("ENDPOINT 타입의 키 생성")
        void shouldBuildEndpointKey() {
            // when
            String key = LimitType.ENDPOINT.buildKey("/api/v1/orders", "GET");

            // then
            assertThat(key).isEqualTo("gateway:rate_limit:endpoint:/api/v1/orders:GET");
        }

        @Test
        @DisplayName("USER 타입의 키 생성")
        void shouldBuildUserKey() {
            // when
            String key = LimitType.USER.buildKey("user-123");

            // then
            assertThat(key).isEqualTo("gateway:rate_limit:user:user-123");
        }

        @Test
        @DisplayName("IP 타입의 키 생성")
        void shouldBuildIpKey() {
            // when
            String key = LimitType.IP.buildKey("192.168.1.1");

            // then
            assertThat(key).isEqualTo("gateway:rate_limit:ip:192.168.1.1");
        }

        @Test
        @DisplayName("OTP 타입의 키 생성")
        void shouldBuildOtpKey() {
            // when
            String key = LimitType.OTP.buildKey("01012345678");

            // then
            assertThat(key).isEqualTo("gateway:rate_limit:otp:01012345678");
        }

        @Test
        @DisplayName("LOGIN 타입의 키 생성")
        void shouldBuildLoginKey() {
            // when
            String key = LimitType.LOGIN.buildKey("192.168.1.1");

            // then
            assertThat(key).isEqualTo("gateway:rate_limit:login:192.168.1.1");
        }
    }
}
