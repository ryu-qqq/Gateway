package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiResponse 단위 테스트
 *
 * <p>ApiResponse는 성공 응답만 담당합니다. 에러 응답은 RFC 7807 ProblemDetail 형식을 사용합니다.
 *
 * @author development-team
 * @since 1.0.0
 * @see GatewayProblemDetail
 */
@DisplayName("ApiResponse 단위 테스트")
class ApiResponseTest {

    @Nested
    @DisplayName("성공 응답 생성")
    class SuccessResponseTest {

        @Test
        @DisplayName("데이터와 함께 성공 응답을 생성해야 한다")
        void shouldCreateSuccessResponseWithData() {
            // given
            String data = "test-data";

            // when
            ApiResponse<String> response = ApiResponse.ofSuccess(data);

            // then
            assertThat(response.requestId()).isNotNull();
            assertThat(response.requestId())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            assertThat(response.data()).isEqualTo("test-data");
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("데이터 없이 성공 응답을 생성해야 한다")
        void shouldCreateSuccessResponseWithoutData() {
            // when
            ApiResponse<Void> response = ApiResponse.ofSuccess();

            // then
            assertThat(response.requestId()).isNotNull();
            assertThat(response.requestId())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            assertThat(response.data()).isNull();
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("복잡한 객체 데이터로 성공 응답을 생성해야 한다")
        void shouldCreateSuccessResponseWithComplexData() {
            // given
            record TestData(String name, int value) {}
            TestData data = new TestData("test", 42);

            // when
            ApiResponse<TestData> response = ApiResponse.ofSuccess(data);

            // then
            assertThat(response.requestId()).isNotNull();
            assertThat(response.requestId())
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            assertThat(response.data()).isNotNull();
            assertThat(response.data().name()).isEqualTo("test");
            assertThat(response.data().value()).isEqualTo(42);
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("매 호출마다 새로운 requestId가 생성되어야 한다")
        void shouldGenerateUniqueRequestIdForEachCall() {
            // when
            ApiResponse<String> response1 = ApiResponse.ofSuccess("data1");
            ApiResponse<String> response2 = ApiResponse.ofSuccess("data2");

            // then
            assertThat(response1.requestId()).isNotEqualTo(response2.requestId());
        }
    }
}
