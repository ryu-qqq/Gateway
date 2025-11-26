package com.ryuqq.gateway.adapter.in.gateway.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("TraceIdMdcContext 테스트")
class TraceIdMdcContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("상수 테스트")
    class ConstantsTest {

        @Test
        @DisplayName("TRACE_ID_KEY가 'traceId'임")
        void shouldHaveCorrectTraceIdKey() {
            assertThat(TraceIdMdcContext.TRACE_ID_KEY).isEqualTo("traceId");
        }
    }

    @Nested
    @DisplayName("put() 메서드 테스트")
    class PutTest {

        @Test
        @DisplayName("traceId를 MDC에 저장")
        void shouldPutTraceIdToMdc() {
            // given
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

            // when
            TraceIdMdcContext.put(traceId);

            // then
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isEqualTo(traceId);
        }

        @Test
        @DisplayName("null traceId는 저장하지 않음")
        void shouldNotPutNullTraceId() {
            // when
            TraceIdMdcContext.put(null);

            // then
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isNull();
        }

        @Test
        @DisplayName("기존 값을 덮어씀")
        void shouldOverwriteExistingValue() {
            // given
            String oldTraceId = "old-trace-id-12345678901234567890123456789012";
            String newTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceIdMdcContext.put(oldTraceId);

            // when
            TraceIdMdcContext.put(newTraceId);

            // then
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isEqualTo(newTraceId);
        }
    }

    @Nested
    @DisplayName("get() 메서드 테스트")
    class GetTest {

        @Test
        @DisplayName("저장된 traceId 반환")
        void shouldReturnStoredTraceId() {
            // given
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            MDC.put(TraceIdMdcContext.TRACE_ID_KEY, traceId);

            // when
            String result = TraceIdMdcContext.get();

            // then
            assertThat(result).isEqualTo(traceId);
        }

        @Test
        @DisplayName("저장된 값이 없으면 null 반환")
        void shouldReturnNullWhenNotSet() {
            // when
            String result = TraceIdMdcContext.get();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("clear() 메서드 테스트")
    class ClearTest {

        @Test
        @DisplayName("traceId를 MDC에서 제거")
        void shouldClearTraceIdFromMdc() {
            // given
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            MDC.put(TraceIdMdcContext.TRACE_ID_KEY, traceId);

            // when
            TraceIdMdcContext.clear();

            // then
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isNull();
        }

        @Test
        @DisplayName("저장된 값이 없어도 예외 발생하지 않음")
        void shouldNotThrowExceptionWhenEmpty() {
            // when & then - no exception
            TraceIdMdcContext.clear();
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isNull();
        }

        @Test
        @DisplayName("다른 MDC 값은 유지됨")
        void shouldNotAffectOtherMdcValues() {
            // given
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            MDC.put(TraceIdMdcContext.TRACE_ID_KEY, traceId);
            MDC.put("otherKey", "otherValue");

            // when
            TraceIdMdcContext.clear();

            // then
            assertThat(MDC.get(TraceIdMdcContext.TRACE_ID_KEY)).isNull();
            assertThat(MDC.get("otherKey")).isEqualTo("otherValue");
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("put → get → clear 전체 흐름")
        void shouldWorkEndToEnd() {
            // given
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

            // when & then - put
            TraceIdMdcContext.put(traceId);
            assertThat(TraceIdMdcContext.get()).isEqualTo(traceId);

            // when & then - clear
            TraceIdMdcContext.clear();
            assertThat(TraceIdMdcContext.get()).isNull();
        }

        @Test
        @DisplayName("요청 처리 시뮬레이션")
        void shouldSimulateRequestProcessing() {
            // Request 시작 - Trace-ID 설정
            String traceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";
            TraceIdMdcContext.put(traceId);

            // 비즈니스 로직 실행 중 Trace-ID 사용
            String currentTraceId = TraceIdMdcContext.get();
            assertThat(currentTraceId).isEqualTo(traceId);

            // Request 종료 - Trace-ID 제거
            TraceIdMdcContext.clear();
            assertThat(TraceIdMdcContext.get()).isNull();
        }
    }

    @Nested
    @DisplayName("유틸리티 클래스 테스트")
    class UtilityClassTest {

        @Test
        @DisplayName("인스턴스화 불가")
        void shouldNotBeInstantiable() throws Exception {
            var constructor = TraceIdMdcContext.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThatThrownBy(constructor::newInstance)
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("final 클래스임")
        void shouldBeFinalClass() {
            assertThat(java.lang.reflect.Modifier.isFinal(
                    TraceIdMdcContext.class.getModifiers()))
                    .isTrue();
        }
    }
}
