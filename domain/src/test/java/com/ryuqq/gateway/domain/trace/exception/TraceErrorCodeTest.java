package com.ryuqq.gateway.domain.trace.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TraceErrorCode 테스트")
class TraceErrorCodeTest {

    @Nested
    @DisplayName("INVALID_TRACE_ID 테스트")
    class InvalidTraceIdTest {

        @Test
        @DisplayName("에러 코드가 TRACE-001이어야 함")
        void shouldHaveCorrectCode() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID.getCode()).isEqualTo("TRACE-001");
        }

        @Test
        @DisplayName("HTTP 상태 코드가 400이어야 함")
        void shouldHaveCorrectHttpStatus() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID.getHttpStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("에러 메시지가 올바르게 설정되어야 함")
        void shouldHaveCorrectMessage() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID.getMessage())
                    .isEqualTo("Invalid Trace-ID format");
        }
    }

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 테스트")
    class ErrorCodeInterfaceTest {

        @Test
        @DisplayName("ErrorCode 인터페이스를 구현해야 함")
        void shouldImplementErrorCodeInterface() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID).isInstanceOf(ErrorCode.class);
        }

        @Test
        @DisplayName("모든 enum 값이 ErrorCode 인터페이스 메서드를 구현해야 함")
        void shouldImplementAllInterfaceMethods() {
            for (TraceErrorCode errorCode : TraceErrorCode.values()) {
                assertThat(errorCode.getCode()).isNotNull().isNotBlank();
                assertThat(errorCode.getHttpStatus()).isGreaterThan(0);
                assertThat(errorCode.getMessage()).isNotNull().isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("Enum 동작 테스트")
    class EnumBehaviorTest {

        @Test
        @DisplayName("valueOf로 enum 값 조회")
        void shouldGetEnumValueUsingValueOf() {
            assertThat(TraceErrorCode.valueOf("INVALID_TRACE_ID"))
                    .isEqualTo(TraceErrorCode.INVALID_TRACE_ID);
        }

        @Test
        @DisplayName("values로 모든 enum 값 조회")
        void shouldGetAllEnumValues() {
            TraceErrorCode[] values = TraceErrorCode.values();
            assertThat(values).hasSize(1);
            assertThat(values).contains(TraceErrorCode.INVALID_TRACE_ID);
        }

        @Test
        @DisplayName("ordinal 값이 올바름")
        void shouldHaveCorrectOrdinal() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID.ordinal()).isEqualTo(0);
        }

        @Test
        @DisplayName("name이 올바름")
        void shouldHaveCorrectName() {
            assertThat(TraceErrorCode.INVALID_TRACE_ID.name()).isEqualTo("INVALID_TRACE_ID");
        }
    }
}
