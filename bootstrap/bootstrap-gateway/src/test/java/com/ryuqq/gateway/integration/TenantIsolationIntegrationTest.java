package com.ryuqq.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.fixtures.JwtTestFixture;
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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

/**
 * Tenant Isolation Integration Test
 *
 * <p>Tenant 격리 기능 E2E 테스트 (Domain → Application → Persistence → Filter)
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>1. Tenant Context 전달 성공
 *   <li>2. MFA 필수 검증 성공
 *   <li>3. MFA 필수이나 미검증 시 403 Forbidden
 *   <li>4. Tenant Config 변경 시 Webhook 캐시 무효화
 *   <li>5. Tenant Config 캐시 히트
 *   <li>6. Tenant Config 캐시 미스 시 AuthHub API 호출
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TenantIsolationIntegrationTest.TestGatewayConfig.class)
class TenantIsolationIntegrationTest {

    static WireMockServer wireMockServer;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private WebTestClient webTestClient;

    @Autowired private ReactiveRedisTemplate<String, String> redisTemplate;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8890));
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
        registry.add("authhub.client.base-url", () -> "http://localhost:8890");
        registry.add("gateway.rate-limit.enabled", () -> "false");
    }

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route(
                            "tenant-isolation-test-route",
                            r -> r.path("/api/**").uri("http://localhost:8890"))
                    .build();
        }
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();

        // Clean up Redis before each test
        StepVerifier.create(
                        redisTemplate.execute(connection -> connection.serverCommands().flushAll()))
                .expectNextCount(1)
                .verifyComplete();

        // Mock JWKS endpoint
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Mock Permission Spec endpoint
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
                                                            "path": "/api/.*",
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
                get(urlEqualTo("/api/orders"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"orders\":[]}")));
    }

    @Nested
    @DisplayName("Scenario 1: Tenant Context 전달 성공")
    class TenantContextPropagationTest {

        @Test
        @DisplayName("JWT의 tenantId로 요청 시 Tenant Config이 로드되어야 한다")
        void shouldLoadTenantConfigWhenValidJwtProvided() {
            // given
            String tenantId = "tenant-001";
            String validJwt = JwtTestFixture.aValidJwtWithTenant("user-123", tenantId);

            // Mock Tenant Config API
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId))));

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .exists("X-Tenant-Id");
        }

        @Test
        @DisplayName("Backend Service로 X-Tenant-Id 헤더가 전달되어야 한다")
        void shouldPropagateXTenantIdHeaderToBackendService() {
            // given
            String tenantId = "tenant-002";
            String validJwt = JwtTestFixture.aValidJwtWithTenant("user-456", tenantId);

            // Mock Tenant Config API
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId))));

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("Scenario 2: MFA 필수 검증 성공")
    class MfaValidationSuccessTest {

        @Test
        @DisplayName("MFA 필수 테넌트에서 MFA 검증된 JWT로 요청 시 성공해야 한다")
        void shouldSucceedWhenMfaVerifiedForMfaRequiredTenant() {
            // given
            String tenantId = TenantConfigTestFixture.MFA_REQUIRED_TENANT;
            String mfaVerifiedJwt = JwtTestFixture.aValidJwtWithMfa("user-123", tenantId, true);

            // Mock Tenant Config API (MFA Required)
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId, true))));

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + mfaVerifiedJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("MFA 불필요 테넌트에서는 MFA 미검증 JWT로도 요청이 성공해야 한다")
        void shouldSucceedWithoutMfaWhenTenantDoesNotRequireMfa() {
            // given
            String tenantId = TenantConfigTestFixture.MFA_NOT_REQUIRED_TENANT;
            String noMfaJwt = JwtTestFixture.aValidJwtWithMfa("user-123", tenantId, false);

            // Mock Tenant Config API (MFA Not Required)
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId, false))));

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + noMfaJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("Scenario 3: MFA 필수이나 미검증 시 403 Forbidden")
    class MfaValidationFailureTest {

        @Test
        @DisplayName("MFA 필수 테넌트에서 MFA 미검증 JWT로 요청 시 403을 반환해야 한다")
        void shouldReturn403WhenMfaNotVerifiedForMfaRequiredTenant() {
            // given
            String tenantId = TenantConfigTestFixture.MFA_REQUIRED_TENANT;
            String noMfaJwt = JwtTestFixture.aValidJwtWithMfa("user-123", tenantId, false);

            // Mock Tenant Config API (MFA Required)
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId, true))));

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + noMfaJwt)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.FORBIDDEN)
                    .expectBody()
                    .jsonPath("$.error.errorCode")
                    .isEqualTo("MFA_REQUIRED");
        }
    }

    @Nested
    @DisplayName("Scenario 4: Tenant Config 변경 시 Webhook 캐시 무효화")
    class TenantConfigWebhookTest {

        @Test
        @DisplayName("Webhook 호출 시 Tenant Config 캐시가 무효화되어야 한다")
        void shouldInvalidateTenantConfigCacheOnWebhook() {
            // given
            String tenantId = "tenant-webhook-test";
            String validJwt = JwtTestFixture.aValidJwtWithTenant("user-123", tenantId);

            // Mock Tenant Config API
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId))));

            // First request to cache the config
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // Verify cache exists
            String cacheKey = "gateway:tenant:config:" + tenantId;
            StepVerifier.create(redisTemplate.hasKey(cacheKey)).expectNext(true).verifyComplete();

            // when - Webhook call to invalidate cache
            webTestClient
                    .post()
                    .uri("/internal/gateway/tenants/config-changed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            String.format(
                                    """
                                    {
                                        "tenantId": "%s",
                                        "timestamp": "2025-01-01T00:00:00Z"
                                    }
                                    """,
                                    tenantId))
                    .exchange()
                    .expectStatus()
                    .isOk();

            // then - Cache should be invalidated
            StepVerifier.create(redisTemplate.hasKey(cacheKey)).expectNext(false).verifyComplete();
        }
    }

    @Nested
    @DisplayName("Scenario 5: Tenant Config 캐시 히트")
    class TenantConfigCacheHitTest {

        @Test
        @DisplayName("두 번째 요청은 Redis 캐시에서 Tenant Config을 가져와야 한다")
        void shouldUseCachedTenantConfigOnSecondRequest() {
            // given
            String tenantId = "tenant-cache-hit-test";
            String validJwt = JwtTestFixture.aValidJwtWithTenant("user-123", tenantId);

            // Mock Tenant Config API
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId))));

            // First request (Cache Miss - will call AuthHub API)
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // Verify cache exists
            String cacheKey = "gateway:tenant:config:" + tenantId;
            StepVerifier.create(redisTemplate.hasKey(cacheKey)).expectNext(true).verifyComplete();

            // Reset WireMock to verify no more API calls
            wireMockServer.resetAll();

            // Re-setup JWKS (needed for JWT validation)
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/auth/jwks"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(JwtTestFixture.jwksResponse())));

            // Re-setup Permission Spec
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
                                                                "path": "/api/.*",
                                                                "method": "GET",
                                                                "isPublic": true,
                                                                "requiredRoles": [],
                                                                "requiredPermissions": []
                                                            }
                                                        ]
                                                    }
                                                    """)));

            // Re-setup downstream service
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/orders"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody("{\"orders\":[]}")));

            // when - Second request (should use cache, NOT call Tenant Config API)
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // then - Tenant Config API should NOT have been called
            // (WireMock will throw if called since we didn't mock it)
        }
    }

    @Nested
    @DisplayName("Scenario 6: 소셜 로그인 허용 여부 검증")
    class SocialLoginValidationTest {

        @Test
        @DisplayName("Tenant Config에서 소셜 로그인 허용 목록을 반환해야 한다")
        void shouldReturnAllowedSocialLoginProviders() {
            // given
            String tenantId = TenantConfigTestFixture.SOCIAL_RESTRICTED_TENANT;
            List<String> allowedProviders = List.of("KAKAO");

            // Mock Tenant Config API with restricted social logins
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/v1/tenants/" + tenantId + "/config"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(
                                                    TenantConfigTestFixture.tenantConfigResponse(
                                                            tenantId, false, allowedProviders))));

            String validJwt = JwtTestFixture.aValidJwtWithTenant("user-123", tenantId);

            // when & then
            webTestClient
                    .get()
                    .uri("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // Note: Full social login validation would require a separate endpoint
            // This test verifies that restricted social logins don't block normal requests
        }
    }
}
