package com.ryuqq.gateway.application.ratelimit.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RecordFailureCommand 단위 테스트")
class RecordFailureCommandTest {

    @Nested
    @DisplayName("생성 검증")
    class Creation {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        void shouldCreateWithValidValues() {
            // given
            LimitType limitType = LimitType.LOGIN;
            String identifier = "192.168.1.1";

            // when
            RecordFailureCommand command = new RecordFailureCommand(limitType, identifier);

            // then
            assertThat(command.limitType()).isEqualTo(limitType);
            assertThat(command.identifier()).isEqualTo(identifier);
        }
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class FactoryMethods {

        @Test
        @DisplayName("forLoginFailure로 LOGIN Command 생성")
        void shouldCreateLoginFailureCommand() {
            // when
            RecordFailureCommand command = RecordFailureCommand.forLoginFailure("192.168.1.1");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.LOGIN);
            assertThat(command.identifier()).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("forInvalidJwt로 INVALID_JWT Command 생성")
        void shouldCreateInvalidJwtCommand() {
            // when
            RecordFailureCommand command = RecordFailureCommand.forInvalidJwt("192.168.1.1");

            // then
            assertThat(command.limitType()).isEqualTo(LimitType.INVALID_JWT);
            assertThat(command.identifier()).isEqualTo("192.168.1.1");
        }
    }
}
