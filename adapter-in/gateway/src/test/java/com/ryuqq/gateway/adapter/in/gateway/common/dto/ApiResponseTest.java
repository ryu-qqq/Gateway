package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ApiResponse 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
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
            assertThat(response.success()).isTrue();
            assertThat(response.data()).isEqualTo("test-data");
            assertThat(response.error()).isNull();
            assertThat(response.timestamp()).isNotNull();
            assertThat(response.traceId()).isNull();
        }

        @Test
        @DisplayName("데이터 없이 성공 응답을 생성해야 한다")
        void shouldCreateSuccessResponseWithoutData() {
            // when
            ApiResponse<Void> response = ApiResponse.ofSuccess();

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNull();
            assertThat(response.error()).isNull();
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("실패 응답 생성")
    class FailureResponseTest {

        @Test
        @DisplayName("ErrorInfo로 실패 응답을 생성해야 한다")
        void shouldCreateFailureResponseWithErrorInfo() {
            // given
            ErrorInfo error = new ErrorInfo("JWT_EXPIRED", "토큰이 만료되었습니다");

            // when
            ApiResponse<Void> response = ApiResponse.ofFailure(error);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.data()).isNull();
            assertThat(response.error()).isEqualTo(error);
            assertThat(response.timestamp()).isNotNull();
            assertThat(response.traceId()).isNull();
        }

        @Test
        @DisplayName("ErrorInfo와 traceId로 실패 응답을 생성해야 한다")
        void shouldCreateFailureResponseWithErrorInfoAndTraceId() {
            // given
            ErrorInfo error = new ErrorInfo("UNAUTHORIZED", "인증 실패");
            String traceId = "trace-123456";

            // when
            ApiResponse<Void> response = ApiResponse.ofFailure(error, traceId);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.data()).isNull();
            assertThat(response.error()).isEqualTo(error);
            assertThat(response.traceId()).isEqualTo("trace-123456");
        }

        @Test
        @DisplayName("에러코드와 메시지로 실패 응답을 생성해야 한다")
        void shouldCreateFailureResponseWithCodeAndMessage() {
            // given
            String errorCode = "FORBIDDEN";
            String message = "권한이 부족합니다";

            // when
            ApiResponse<Void> response = ApiResponse.ofFailure(errorCode, message);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.error().errorCode()).isEqualTo("FORBIDDEN");
            assertThat(response.error().message()).isEqualTo("권한이 부족합니다");
        }

        @Test
        @DisplayName("에러코드, 메시지, traceId로 실패 응답을 생성해야 한다")
        void shouldCreateFailureResponseWithCodeMessageAndTraceId() {
            // given
            String errorCode = "INTERNAL_ERROR";
            String message = "내부 서버 오류";
            String traceId = "trace-789";

            // when
            ApiResponse<Void> response = ApiResponse.ofFailure(errorCode, message, traceId);

            // then
            assertThat(response.success()).isFalse();
            assertThat(response.error().errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.error().message()).isEqualTo("내부 서버 오류");
            assertThat(response.traceId()).isEqualTo("trace-789");
        }
    }
}
