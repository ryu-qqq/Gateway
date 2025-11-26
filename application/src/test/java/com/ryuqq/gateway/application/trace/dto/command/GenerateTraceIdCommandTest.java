package com.ryuqq.gateway.application.trace.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateTraceIdCommand 테스트")
class GenerateTraceIdCommandTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("빈 Command 생성 가능")
        void shouldCreateEmptyCommand() {
            // when
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // then
            assertThat(command).isNotNull();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("모든 빈 Command는 동일함")
        void shouldBeEqualForEmptyCommands() {
            // given
            GenerateTraceIdCommand command1 = new GenerateTraceIdCommand();
            GenerateTraceIdCommand command2 = new GenerateTraceIdCommand();

            // when & then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("자기 자신과 동일함")
        void shouldBeEqualToItself() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when & then
            assertThat(command).isEqualTo(command);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 클래스명 포함")
        void shouldIncludeClassName() {
            // given
            GenerateTraceIdCommand command = new GenerateTraceIdCommand();

            // when
            String result = command.toString();

            // then
            assertThat(result).contains("GenerateTraceIdCommand");
        }
    }

    @Nested
    @DisplayName("Record 특성 테스트")
    class RecordTest {

        @Test
        @DisplayName("Record 타입임")
        void shouldBeRecord() {
            assertThat(GenerateTraceIdCommand.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("컴포넌트가 없음")
        void shouldHaveNoComponents() {
            assertThat(GenerateTraceIdCommand.class.getRecordComponents()).isEmpty();
        }
    }
}
