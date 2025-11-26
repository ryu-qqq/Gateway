package com.ryuqq.gateway.application.trace.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GenerateTraceIdResponse 테스트")
class GenerateTraceIdResponseTest {

    private static final String VALID_TRACE_ID =
            "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("traceId로 Response 생성")
        void shouldCreateResponseWithTraceId() {
            // when
            GenerateTraceIdResponse response = new GenerateTraceIdResponse(VALID_TRACE_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.traceId()).isEqualTo(VALID_TRACE_ID);
        }

        @Test
        @DisplayName("null traceId로 Response 생성 가능")
        void shouldAllowNullTraceId() {
            // when
            GenerateTraceIdResponse response = new GenerateTraceIdResponse(null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.traceId()).isNull();
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("traceId() 반환")
        void shouldReturnTraceId() {
            // given
            GenerateTraceIdResponse response = new GenerateTraceIdResponse(VALID_TRACE_ID);

            // when & then
            assertThat(response.traceId()).isEqualTo(VALID_TRACE_ID);
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("같은 traceId면 동일함")
        void shouldBeEqualWhenSameTraceId() {
            // given
            GenerateTraceIdResponse response1 = new GenerateTraceIdResponse(VALID_TRACE_ID);
            GenerateTraceIdResponse response2 = new GenerateTraceIdResponse(VALID_TRACE_ID);

            // when & then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("다른 traceId면 다름")
        void shouldNotBeEqualWhenDifferentTraceId() {
            // given
            GenerateTraceIdResponse response1 = new GenerateTraceIdResponse(VALID_TRACE_ID);
            GenerateTraceIdResponse response2 =
                    new GenerateTraceIdResponse("20250124000000000-11111111-2222-3333-4444-555555555555");

            // when & then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("자기 자신과 동일함")
        void shouldBeEqualToItself() {
            // given
            GenerateTraceIdResponse response = new GenerateTraceIdResponse(VALID_TRACE_ID);

            // when & then
            assertThat(response).isEqualTo(response);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString이 traceId 포함")
        void shouldIncludeTraceId() {
            // given
            GenerateTraceIdResponse response = new GenerateTraceIdResponse(VALID_TRACE_ID);

            // when
            String result = response.toString();

            // then
            assertThat(result).contains("GenerateTraceIdResponse");
            assertThat(result).contains(VALID_TRACE_ID);
        }
    }

    @Nested
    @DisplayName("Record 특성 테스트")
    class RecordTest {

        @Test
        @DisplayName("Record 타입임")
        void shouldBeRecord() {
            assertThat(GenerateTraceIdResponse.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("컴포넌트가 1개임")
        void shouldHaveOneComponent() {
            assertThat(GenerateTraceIdResponse.class.getRecordComponents()).hasSize(1);
        }

        @Test
        @DisplayName("컴포넌트 이름이 traceId임")
        void shouldHaveTraceIdComponent() {
            assertThat(GenerateTraceIdResponse.class.getRecordComponents()[0].getName())
                    .isEqualTo("traceId");
        }
    }
}
