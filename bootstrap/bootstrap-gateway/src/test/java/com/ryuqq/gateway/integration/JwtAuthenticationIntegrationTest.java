package com.ryuqq.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.fixtures.JwtTestFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * JWT Authentication Integration Test
 *
 * <p>전체 스택 E2E 테스트 (Domain → Application → Persistence → Filter)
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(JwtAuthenticationIntegrationTest.TestGatewayConfig.class)
class JwtAuthenticationIntegrationTest {

    static WireMockServer wireMockServer;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8888));
        wireMockServer.start();
    }

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
        registry.add("authhub.client.base-url", () -> "http://localhost:8888");
    }

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-route", r -> r.path("/test/**").uri("http://localhost:8888"))
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

        // Mock Permission Spec endpoint - /test/resource 경로를 public endpoint로 설정
        // PermissionSpecResponse(version, updatedAt, permissions) 형식으로 응답
        // path는 정규식 패턴으로 작성 (Ant-style '**' 대신 정규식 '.*' 사용)
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/permissions/spec"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                        {
                            "version": 1,
                            "updatedAt": "2025-01-01T00:00:00Z",
                            "permissions": [
                                {
                                    "serviceName": "test-service",
                                    "path": "/test/.*",
                                    "method": "GET",
                                    "isPublic": true,
                                    "requiredRoles": [],
                                    "requiredPermissions": []
                                }
                            ]
                        }
                        """)));

        // Mock downstream service
        wireMockServer.stubFor(
                get(urlEqualTo("/test/resource"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));
    }

    @Nested
    @DisplayName("JWT 인증 성공 시나리오")
    class JwtAuthenticationSuccessTest {

        @Test
        @DisplayName("유효한 JWT로 인증 시 요청이 성공해야 한다")
        void shouldAuthenticateWithValidJwt() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("커스텀 subject로 JWT 인증이 성공해야 한다")
        void shouldAuthenticateWithCustomSubject() {
            // given
            String validJwt = JwtTestFixture.aValidJwt("custom-user-456");

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("JWT 인증 실패 시나리오")
    class JwtAuthenticationFailureTest {

        @Test
        @DisplayName("Authorization 헤더가 없으면 401을 반환해야 한다")
        void shouldReturn401WhenAuthorizationHeaderMissing() {
            webTestClient.get().uri("/test/resource").exchange().expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("만료된 JWT로 요청 시 401을 반환해야 한다")
        void shouldReturn401WhenJwtExpired() {
            // given
            String expiredJwt = JwtTestFixture.anExpiredJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized();
        }

        @Test
        @DisplayName("잘못된 서명의 JWT로 요청 시 401을 반환해야 한다")
        void shouldReturn401WhenJwtSignatureInvalid() {
            // given
            String invalidSignatureJwt = JwtTestFixture.aJwtWithInvalidSignature();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSignatureJwt)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰으로 요청 시 401을 반환해야 한다")
        void shouldReturn401WhenTokenFormatInvalid() {
            // given
            String invalidToken = "not-a-valid-jwt-format";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                    .exchange()
                    .expectStatus()
                    .isUnauthorized();
        }
    }

    @Nested
    @DisplayName("Public Key 갱신 시나리오")
    class PublicKeyRefreshTest {

        @Test
        @DisplayName("POST /actuator/refresh-public-keys 호출 시 성공해야 한다")
        void shouldRefreshPublicKeysSuccessfully() {
            // given - 먼저 인증하여 캐시 생성
            String validJwt = JwtTestFixture.aValidJwt();
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // when & then - Public Key 갱신
            webTestClient
                    .post()
                    .uri("/actuator/refresh-public-keys")
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }
}
