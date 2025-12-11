package com.ryuqq.gateway.domain.trace.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidTraceIdException 테스트")
class InvalidTraceIdExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void shouldCreateExceptionWithDefaultMessage() {
            // when
            InvalidTraceIdException exception = new InvalidTraceIdException();

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).isEqualTo("Invalid Trace-ID format");
        }

        @Test
        @DisplayName("Trace-ID 값으로 예외 생성")
        void shouldCreateExceptionWithTraceId() {
            // given
            String invalidTraceId = "invalid-trace-id";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(invalidTraceId);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            // DomainException 형식: ErrorCode.getMessage() + ": " + buildDetail()
            assertThat(exception.getMessage())
                    .isEqualTo("Invalid Trace-ID format: traceId=" + invalidTraceId);
        }

        @Test
        @DisplayName("빈 문자열 Trace-ID로 예외 생성")
        void shouldCreateExceptionWithEmptyTraceId() {
            // given
            String emptyTraceId = "";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(emptyTraceId);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).isEqualTo("Invalid Trace-ID format: traceId=");
        }

        @Test
        @DisplayName("null Trace-ID로 예외 생성")
        void shouldCreateExceptionWithNullTraceId() {
            // given
            String nullTraceId = null;

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(nullTraceId);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).isEqualTo("Invalid Trace-ID format: traceId=null");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("TraceErrorCode.INVALID_TRACE_ID와 매핑됨")
        void shouldMapToInvalidTraceIdErrorCode() {
            // given
            InvalidTraceIdException exception = new InvalidTraceIdException("test");

            // when & then
            assertThat(exception.getCode()).isEqualTo(TraceErrorCode.INVALID_TRACE_ID.getCode());
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
        }

        @Test
        @DisplayName("기본 생성자도 동일한 ErrorCode 사용")
        void shouldUseSameErrorCodeForDefaultConstructor() {
            // given
            InvalidTraceIdException exception = new InvalidTraceIdException();

            // when & then
            assertThat(exception.getCode()).isEqualTo(TraceErrorCode.INVALID_TRACE_ID.getCode());
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            InvalidTraceIdException exception = new InvalidTraceIdException("test");

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            // when & then
            assertThat(
                            java.lang.reflect.Modifier.isFinal(
                                    InvalidTraceIdException.class.getModifiers()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("메시지 형식 테스트")
    class MessageFormatTest {

        @Test
        @DisplayName("메시지에 Trace-ID가 포함됨")
        void shouldIncludeTraceIdInMessage() {
            // given
            String traceId = "my-invalid-trace-id-value";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(traceId);

            // then
            assertThat(exception.getMessage()).contains(traceId);
            assertThat(exception.getMessage()).startsWith("Invalid Trace-ID format:");
        }

        @Test
        @DisplayName("기본 메시지가 ErrorCode 메시지와 동일")
        void shouldHaveDefaultMessageMatchingErrorCode() {
            // when
            InvalidTraceIdException exception = new InvalidTraceIdException();

            // then
            assertThat(exception.getMessage())
                    .isEqualTo(TraceErrorCode.INVALID_TRACE_ID.getMessage());
        }
    }

    @Nested
    @DisplayName("예외 던지기 테스트")
    class ThrowExceptionTest {

        @Test
        @DisplayName("예외가 정상적으로 던져짐")
        void shouldThrowException() {
            // when & then
            try {
                throw new InvalidTraceIdException("bad-trace-id");
            } catch (InvalidTraceIdException e) {
                assertThat(e.getCode()).isEqualTo("TRACE-001");
                assertThat(e.getMessage()).contains("bad-trace-id");
            }
        }

        @Test
        @DisplayName("DomainException으로 잡을 수 있음")
        void shouldBeCatchableAsDomainException() {
            // given
            boolean caught = false;
            String caughtCode = null;

            // when
            try {
                throw new InvalidTraceIdException("test");
            } catch (DomainException e) {
                caught = true;
                caughtCode = e.getCode();
            }

            // then
            assertThat(caught).isTrue();
            assertThat(caughtCode).isEqualTo("TRACE-001");
        }
    }

    @Nested
    @DisplayName("비즈니스 시나리오 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("형식이 잘못된 Trace-ID 시나리오")
        void invalidFormatScenario() {
            // given
            String invalidFormat = "not-a-valid-trace-id-format";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(invalidFormat);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).contains(invalidFormat);
        }

        @Test
        @DisplayName("길이가 부족한 Trace-ID 시나리오")
        void shortLengthScenario() {
            // given
            String shortTraceId = "20250124-abc123";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(shortTraceId);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).contains(shortTraceId);
        }

        @Test
        @DisplayName("UUID 부분이 잘못된 Trace-ID 시나리오")
        void invalidUuidPartScenario() {
            // given
            String invalidUuid = "20250124123456789-invalid-uuid-format";

            // when
            InvalidTraceIdException exception = new InvalidTraceIdException(invalidUuid);

            // then
            assertThat(exception.getCode()).isEqualTo("TRACE-001");
            assertThat(exception.getMessage()).contains(invalidUuid);
        }
    }
}
