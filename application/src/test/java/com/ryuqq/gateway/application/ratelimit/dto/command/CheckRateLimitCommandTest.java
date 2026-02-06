package com.ryuqq.gateway.application.ratelimit.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CheckRateLimitCommand 단위 테스트")
class CheckRateLimitCommandTest {

    @Nested
    @DisplayName("생성 검증")
    class Creation {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        void shouldCreateWithValidValues() {
            // given
            LimitType limitType = LimitType.IP;
            String identifier = "192.168.1.1";

            // when
            CheckRateLimitCommand command = new CheckRateLimitCommand(limitType, identifier);

            // then
            assertThat(command.limitType()).isEqualTo(limitType);
            assertThat(command.identifier()).isEqualTo(identifier);
            assertThat(command.additionalKeyParts()).isEmpty();
        }

        @Test
        @DisplayName("추가 키 구성 요소와 함께 생성 성공")
        void shouldCreateWithAdditionalKeyParts() {
            // given
            LimitType limitType = LimitType.ENDPOINT;
            String identifier = "/api/users";
            String method = "POST";

            // when
            CheckRateLimitCommand command =
                    new CheckRateLimitCommand(limitType, identifier, method);

            // then
            assertThat(command.limitType()).isEqualTo(limitType);
            assertThat(command.identifier()).isEqualTo(identifier);
            assertThat(command.additionalKeyParts()).containsExactly(method);
        }

        @Test
        @DisplayName("null additionalKeyParts는 빈 배열로 변환")
        void shouldConvertNullAdditionalKeyPartsToEmptyArray() {
            // when
            CheckRateLimitCommand command =
                    new CheckRateLimitCommand(LimitType.IP, "192.168.1.1", (String[]) null);

            // then
            assertThat(command.additionalKeyParts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class FactoryMethods {

        @Test
        @DisplayName("forEndpoint로 ENDPOINT Command 생성")
        void shouldCreateEndpointCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forEndpoint("/api/users", "GET");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.ENDPOINT);
            assertThat(command.identifier()).isEqualTo("/api/users");
            assertThat(command.additionalKeyParts()).containsExactly("GET");
        }

        @Test
        @DisplayName("forUser로 USER Command 생성")
        void shouldCreateUserCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forUser("user-123");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.USER);
            assertThat(command.identifier()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("forIp로 IP Command 생성")
        void shouldCreateIpCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forIp("192.168.1.1");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.IP);
            assertThat(command.identifier()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("forOtp로 OTP Command 생성")
        void shouldCreateOtpCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forOtp("010-1234-5678");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.OTP);
            assertThat(command.identifier()).isEqualTo("010-1234-5678");
        }

        @Test
        @DisplayName("forLogin으로 LOGIN Command 생성")
        void shouldCreateLoginCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forLogin("192.168.1.1");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.LOGIN);
            assertThat(command.identifier()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("forTokenRefresh로 TOKEN_REFRESH Command 생성")
        void shouldCreateTokenRefreshCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forTokenRefresh("user-123");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.TOKEN_REFRESH);
            assertThat(command.identifier()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("forInvalidJwt로 INVALID_JWT Command 생성")
        void shouldCreateInvalidJwtCommand() {
            // when
            CheckRateLimitCommand command = CheckRateLimitCommand.forInvalidJwt("192.168.1.1");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.INVALID_JWT);
            assertThat(command.identifier()).isEqualTo("192.168.1.1");
        }
    }
}
