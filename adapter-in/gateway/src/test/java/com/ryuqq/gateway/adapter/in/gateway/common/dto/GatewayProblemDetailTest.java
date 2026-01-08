package com.ryuqq.gateway.adapter.in.gateway.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GatewayProblemDetail 단위 테스트
 *
 * <p>RFC 7807 Problem Detail 구현의 동작을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("GatewayProblemDetail 단위 테스트")
class GatewayProblemDetailTest {

    @Nested
    @DisplayName("of() 정적 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("기본 ProblemDetail을 생성해야 한다")
        void shouldCreateBasicProblemDetail() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(401, "Unauthorized", "인증이 필요합니다", "UNAUTHORIZED");

            // then
            assertThat(problemDetail.type()).isEqualTo(URI.create("about:blank"));
            assertThat(problemDetail.title()).isEqualTo("Unauthorized");
            assertThat(problemDetail.status()).isEqualTo(401);
            assertThat(problemDetail.detail()).isEqualTo("인증이 필요합니다");
            assertThat(problemDetail.code()).isEqualTo("UNAUTHORIZED");
            assertThat(problemDetail.instance()).isNull();
            assertThat(problemDetail.requestId()).isNull();
            assertThat(problemDetail.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("요청 정보를 포함한 ProblemDetail을 생성해야 한다")
        void shouldCreateProblemDetailWithRequestInfo() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(
                            403,
                            "Forbidden",
                            "접근이 거부되었습니다",
                            "ACCESS_DENIED",
                            "/api/v1/users",
                            "req-123");

            // then
            assertThat(problemDetail.status()).isEqualTo(403);
            assertThat(problemDetail.instance()).isEqualTo(URI.create("/api/v1/users"));
            assertThat(problemDetail.requestId()).isEqualTo("req-123");
        }

        @Test
        @DisplayName("instance가 null이면 URI도 null이어야 한다")
        void shouldHandleNullInstance() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(
                            500, "Internal Server Error", "서버 오류", "SERVER_ERROR", null, "req-456");

            // then
            assertThat(problemDetail.instance()).isNull();
        }

        @Test
        @DisplayName("instance가 빈 문자열이면 URI도 null이어야 한다")
        void shouldHandleEmptyInstance() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(
                            500, "Internal Server Error", "서버 오류", "SERVER_ERROR", "", "req-789");

            // then
            assertThat(problemDetail.instance()).isNull();
        }

        @Test
        @DisplayName("instance가 공백 문자열이면 URI도 null이어야 한다")
        void shouldHandleBlankInstance() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(
                            500,
                            "Internal Server Error",
                            "서버 오류",
                            "SERVER_ERROR",
                            "   ",
                            "req-abc");

            // then
            assertThat(problemDetail.instance()).isNull();
        }

        @Test
        @DisplayName("잘못된 URI 형식이면 예외 없이 null을 반환해야 한다")
        void shouldHandleInvalidUriGracefully() {
            // given - 공백이 포함된 잘못된 URI
            String invalidUri = "/api/v1/users with spaces";

            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.of(
                            400, "Bad Request", "잘못된 요청입니다", "BAD_REQUEST", invalidUri, "req-def");

            // then - 예외 없이 null 반환
            assertThat(problemDetail.instance()).isNull();
            assertThat(problemDetail.status()).isEqualTo(400);
            assertThat(problemDetail.code()).isEqualTo("BAD_REQUEST");
        }
    }

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderTest {

        @Test
        @DisplayName("Builder로 ProblemDetail을 생성해야 한다")
        void shouldCreateProblemDetailWithBuilder() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.builder()
                            .status(429)
                            .title("Too Many Requests")
                            .detail("요청이 너무 많습니다")
                            .code("RATE_LIMIT_EXCEEDED")
                            .instance("/api/v1/orders")
                            .requestId("req-builder-123")
                            .build();

            // then
            assertThat(problemDetail.status()).isEqualTo(429);
            assertThat(problemDetail.title()).isEqualTo("Too Many Requests");
            assertThat(problemDetail.detail()).isEqualTo("요청이 너무 많습니다");
            assertThat(problemDetail.code()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(problemDetail.instance()).isEqualTo(URI.create("/api/v1/orders"));
            assertThat(problemDetail.requestId()).isEqualTo("req-builder-123");
        }

        @Test
        @DisplayName("Builder의 instance가 null이면 URI도 null이어야 한다")
        void shouldHandleNullInstanceInBuilder() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.builder()
                            .status(500)
                            .title("Internal Server Error")
                            .detail("서버 오류")
                            .code("SERVER_ERROR")
                            .instance(null)
                            .build();

            // then
            assertThat(problemDetail.instance()).isNull();
        }

        @Test
        @DisplayName("Builder의 instance가 잘못된 URI 형식이면 예외 없이 null을 반환해야 한다")
        void shouldHandleInvalidUriInBuilderGracefully() {
            // given - 공백이 포함된 잘못된 URI
            String invalidUri = "/api with spaces/endpoint";

            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.builder()
                            .status(400)
                            .title("Bad Request")
                            .detail("잘못된 요청")
                            .code("BAD_REQUEST")
                            .instance(invalidUri)
                            .build();

            // then - 예외 없이 null 반환
            assertThat(problemDetail.instance()).isNull();
        }

        @Test
        @DisplayName("Builder의 기본값이 적용되어야 한다")
        void shouldApplyDefaultValuesInBuilder() {
            // when
            GatewayProblemDetail problemDetail =
                    GatewayProblemDetail.builder().status(200).title("OK").build();

            // then
            assertThat(problemDetail.type()).isEqualTo(URI.create("about:blank"));
            assertThat(problemDetail.timestamp()).isNotNull();
        }
    }
}
