package com.ryuqq.gateway.integration.observability;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import com.ryuqq.gateway.integration.helper.PermissionTestFixture;
import com.ryuqq.gateway.integration.helper.TenantConfigTestFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Observability Integration Test
 *
 * <p>TraceId 전파, HTTP 로깅 제외 경로 등 Observability SDK 관련 E2E 테스트
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>X-Trace-Id 헤더 생성 및 응답 전파
 *   <li>기존 X-Trace-Id 헤더 재사용
 *   <li>/actuator 경로 제외 처리
 *   <li>TraceId 형식 검증
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ObservabilityIntegrationTest.TestGatewayConfig.class)
class ObservabilityIntegrationTest {

    private static final String X_TRACE_ID_HEADER = "X-Trace-Id";

    static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withReuse(true);

    @Autowired private WebTestClient webTestClient;

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("authhub.client.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("gateway.rate-limit.enabled", () -> "false");
        // Redisson 설정
        registry.add(
                "spring.redis.redisson.config",
                () ->
                        String.format(
                                "singleServerConfig:\n  address: redis://%s:%d",
                                redis.getHost(), redis.getFirstMappedPort()));
        // Observability 설정
        registry.add("observability.reactive-trace.enabled", () -> "true");
        registry.add("observability.reactive-trace.generate-if-missing", () -> "true");
        registry.add("observability.reactive-http.enabled", () -> "true");
        // Management 엔드포인트 설정 (prometheus 포함)
        registry.add(
                "management.endpoints.web.exposure.include",
                () -> "health,info,metrics,prometheus");
    }

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route(
                            "observability-test-route",
                            r ->
                                    r.path("/test/**")
                                            .uri("http://localhost:" + wireMockServer.port()))
                    .build();
        }
    }

    @BeforeEach
    void setupWireMock() {
        wireMockServer.resetAll();

        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Mock Permission Spec endpoint (Internal API)
        wireMockServer.stubFor(
                get(urlEqualTo(PermissionTestFixture.PERMISSION_SPEC_PATH))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture.allPublicPermissionSpec(
                                                        "/test/.*"))));

        // Mock User Permissions endpoint (Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching(PermissionTestFixture.USER_PERMISSIONS_PATH_PATTERN))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture
                                                        .userPermissionHashResponse())));

        // Mock downstream service
        wireMockServer.stubFor(
                get(urlEqualTo("/test/resource"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));

        // Mock Tenant Config API (Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/internal/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));
    }

    @Nested
    @DisplayName("TraceId 생성 및 전파")
    class TraceIdGenerationTest {

        @Test
        @DisplayName("요청 시 X-Trace-Id 헤더가 응답에 포함되어야 한다")
        void shouldIncludeTraceIdInResponse() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .exists(X_TRACE_ID_HEADER)
                    .expectHeader()
                    .value(
                            X_TRACE_ID_HEADER,
                            traceId -> {
                                assertThat(traceId).isNotBlank();
                                assertThat(traceId).matches("[a-zA-Z0-9-_]+");
                            });
        }

        @Test
        @DisplayName("기존 X-Trace-Id 헤더가 있으면 재사용해야 한다")
        void shouldReuseExistingTraceId() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // TraceId 형식: {17자리 timestamp}-{uuid}
            String existingTraceId = "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header(X_TRACE_ID_HEADER, existingTraceId)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .valueEquals(X_TRACE_ID_HEADER, existingTraceId);
        }

        @Test
        @DisplayName("여러 요청에서 각각 고유한 TraceId가 생성되어야 한다")
        void shouldGenerateUniqueTraceIdPerRequest() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when - 첫 번째 요청
            String[] traceId1 = new String[1];
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .value(X_TRACE_ID_HEADER, id -> traceId1[0] = id);

            // when - 두 번째 요청
            String[] traceId2 = new String[1];
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .value(X_TRACE_ID_HEADER, id -> traceId2[0] = id);

            // then - 서로 다른 TraceId
            assertThat(traceId1[0]).isNotEqualTo(traceId2[0]);
        }
    }

    @Nested
    @DisplayName("Actuator 경로 처리")
    class ActuatorPathTest {

        @Test
        @DisplayName("/actuator/health 요청이 성공해야 한다")
        void shouldHandleActuatorHealthEndpoint() {
            webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        }

        @Test
        @DisplayName("/actuator/info 요청이 성공해야 한다")
        void shouldHandleActuatorInfoEndpoint() {
            webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
        }

        @Test
        @DisplayName("/actuator/metrics 요청이 성공해야 한다")
        void shouldHandleActuatorMetricsEndpoint() {
            webTestClient.get().uri("/actuator/metrics").exchange().expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("인증 실패 시 TraceId 처리")
    class TraceIdOnAuthFailureTest {

        @Test
        @DisplayName("401 응답에도 X-Trace-Id가 포함되어야 한다")
        void shouldIncludeTraceIdIn401Response() {
            // 인증 없이 요청
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .exchange()
                    .expectStatus()
                    .isUnauthorized()
                    .expectHeader()
                    .exists(X_TRACE_ID_HEADER);
        }

        @Test
        @DisplayName("만료된 JWT로 401 응답 시에도 X-Trace-Id가 포함되어야 한다")
        void shouldIncludeTraceIdWithExpiredJwt() {
            // given
            String expiredJwt = JwtTestFixture.anExpiredJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized()
                    .expectHeader()
                    .exists(X_TRACE_ID_HEADER);
        }
    }
}
