package com.ryuqq.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.fixtures.JwtTestFixture;
import com.ryuqq.gateway.integration.fixtures.PermissionTestFixture;
import com.ryuqq.gateway.integration.fixtures.TenantConfigTestFixture;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Permission Authorization Integration Test
 *
 * <p>Permission 기반 인가 전체 스택 E2E 테스트
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>Public 엔드포인트 접근 (인증 필요, 권한 불필요)
 *   <li>Protected 엔드포인트 권한 검증 성공/실패
 *   <li>와일드카드 권한 매칭
 *   <li>역할 기반 권한 검증
 *   <li>Webhook 기반 캐시 무효화
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(PermissionAuthorizationIntegrationTest.TestGatewayConfig.class)
class PermissionAuthorizationIntegrationTest {

    static WireMockServer wireMockServer;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8889));
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
        registry.add("authhub.client.base-url", () -> "http://localhost:8889");
        // Redisson 설정 (Testcontainers Redis 사용)
        registry.add("spring.redis.redisson.config", () ->
                String.format("singleServerConfig:\n  address: redis://%s:%d",
                        redis.getHost(), redis.getFirstMappedPort()));
    }

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator permissionTestRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route(
                            "permission-test-route",
                            r -> r.path("/test/**").uri("http://localhost:8889"))
                    .build();
        }
    }

    @BeforeEach
    void setupWireMock() {
        wireMockServer.resetAll();

        // JWKS 응답 설정
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Permission Spec 응답 설정
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/permissions/spec"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(PermissionTestFixture.permissionSpecResponse())));

        // 일반 사용자 Permission Hash 응답 설정
        wireMockServer.stubFor(
                get(urlPathMatching("/api/v1/permissions/users/.+"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture
                                                        .userPermissionHashResponse())));

        // Downstream 서비스 Mock
        wireMockServer.stubFor(
                get(urlPathMatching("/test/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));

        // Mock Tenant Config API (GATEWAY-004 Tenant 격리 기능)
        wireMockServer.stubFor(
                get(urlPathMatching("/api/v1/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));
    }

    @Nested
    @DisplayName("Public 엔드포인트 접근 시나리오")
    class PublicEndpointTest {

        @Test
        @DisplayName("Public 엔드포인트는 유효한 JWT만 있으면 접근할 수 있어야 한다")
        void shouldAccessPublicEndpointWithValidJwt() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/public")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("Protected 엔드포인트 권한 검증 시나리오")
    class ProtectedEndpointTest {

        @Test
        @DisplayName("필요한 권한이 있으면 Protected 엔드포인트에 접근할 수 있어야 한다")
        void shouldAccessProtectedEndpointWithRequiredPermission() {
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
        @DisplayName("Path Variable이 있는 엔드포인트도 권한 검증이 되어야 한다")
        void shouldValidatePermissionWithPathVariable() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/users/12345")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("권한 부족 시나리오")
    class InsufficientPermissionTest {

        @BeforeEach
        void setupNoPermissionUser() {
            // 권한 없는 사용자 응답으로 재설정
            wireMockServer.stubFor(
                    get(urlPathMatching("/api/v1/permissions/users/.+"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    PermissionTestFixture
                                                            .noPermissionHashResponse())));
        }

        @Test
        @DisplayName("권한이 없으면 403 Forbidden을 반환해야 한다")
        void shouldReturn403WhenNoPermission() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isForbidden();
        }
    }

    @Nested
    @DisplayName("Webhook 캐시 무효화 시나리오")
    class WebhookCacheInvalidationTest {

        @Test
        @DisplayName("Permission Spec Sync Webhook 호출 시 성공해야 한다")
        void shouldHandleSpecSyncWebhook() {
            // given
            String requestBody = PermissionTestFixture.specSyncRequest(2L, List.of("test-service"));

            // when & then
            webTestClient
                    .post()
                    .uri("/webhooks/permission/spec-sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("User Permission Invalidate Webhook 호출 시 성공해야 한다")
        void shouldHandleUserInvalidateWebhook() {
            // given
            String requestBody =
                    PermissionTestFixture.userInvalidateRequest(
                            PermissionTestFixture.DEFAULT_TENANT_ID,
                            PermissionTestFixture.DEFAULT_USER_ID);

            // when & then
            webTestClient
                    .post()
                    .uri("/webhooks/permission/user-invalidate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("Spec Sync 후 새로운 Permission Spec이 적용되어야 한다")
        void shouldApplyNewSpecAfterSync() {
            // given - 먼저 기존 Spec으로 요청 성공
            String validJwt = JwtTestFixture.aValidJwt();

            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // when - Spec Sync Webhook 호출
            String requestBody = PermissionTestFixture.specSyncRequest(2L, List.of("test-service"));

            webTestClient
                    .post()
                    .uri("/webhooks/permission/spec-sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // then - 다음 요청에서 새로운 Spec이 적용됨 (AuthHub에서 다시 조회)
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
    @DisplayName("역할 기반 권한 검증 시나리오")
    class RoleBasedPermissionTest {

        @BeforeEach
        void setupAdminUser() {
            // 관리자 권한 사용자 응답으로 설정
            wireMockServer.stubFor(
                    get(urlPathMatching("/api/v1/permissions/users/.+"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    PermissionTestFixture
                                                            .adminPermissionHashResponse())));
        }

        @Test
        @DisplayName("ADMIN 역할과 권한이 있으면 관리자 엔드포인트에 접근할 수 있어야 한다")
        void shouldAccessAdminEndpointWithAdminRoleAndPermission() {
            // given
            String validJwt = JwtTestFixture.aValidJwt("admin-456", List.of("ADMIN"));

            // when & then
            webTestClient
                    .get()
                    .uri("/test/admin")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }
}
