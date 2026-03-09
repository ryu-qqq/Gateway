package com.ryuqq.gateway.integration.routing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.bootstrap.config.GatewayRoutingConfig.GatewayRoutingProperties;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import com.ryuqq.gateway.integration.helper.TenantConfigTestFixture;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MarketPlace OMS 라우팅 통합 테스트
 *
 * <p>OMS(사방넷/셀릭)가 호출하는 엔드포인트가 marketplace-web-api로 리라우팅되고, path가 {@code /api/v1/{path}} → {@code
 * /api/v1/legacy/{path}}로 리라이트되는지 검증
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>OMS 대상 경로 → marketplace-web-api (path rewrite 적용)
 *   <li>비대상 경로 → legacy-admin (기존 동작 유지)
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
class MarketplaceOmsRoutingIntegrationTest {

    static WireMockServer authServer;
    static WireMockServer marketplaceServer;
    static WireMockServer legacyAdminServer;

    // WireMock 서버는 @DynamicPropertySource보다 먼저 시작되어야 하므로 static 초기화 블록 사용
    static {
        authServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        authServer.start();

        marketplaceServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        marketplaceServer.start();

        legacyAdminServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        legacyAdminServer.start();
    }

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withReuse(true);

    @Autowired private WebTestClient webTestClient;

    @Autowired private GatewayRoutingProperties routingProperties;

    @Autowired private ReactiveRedisTemplate<String, String> redisTemplate;

    @AfterAll
    static void stopWireMock() {
        if (authServer != null) {
            authServer.stop();
        }
        if (marketplaceServer != null) {
            marketplaceServer.stop();
        }
        if (legacyAdminServer != null) {
            legacyAdminServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // AuthHub Client
        registry.add("authhub.client.base-url", () -> "http://localhost:" + authServer.port());

        // Redisson config
        registry.add(
                "spring.redis.redisson.config",
                () ->
                        String.format(
                                "singleServerConfig:\n  address: redis://%s:%d",
                                redis.getHost(), redis.getFirstMappedPort()));

        // Rate Limit 비활성화
        registry.add("gateway.rate-limit.enabled", () -> "false");

        // Gateway Routing 설정
        registry.add("gateway.routing.discovery.enabled", () -> "false");

        // services[0]: marketplace-oms-legacy (OMS 대상 경로 → marketplace-web-api)
        registry.add("gateway.routing.services[0].id", () -> "marketplace-oms-legacy");
        registry.add(
                "gateway.routing.services[0].uri",
                () -> "http://localhost:" + marketplaceServer.port());
        registry.add("gateway.routing.services[0].paths[0]", () -> "/api/v1/auth/authentication");
        registry.add("gateway.routing.services[0].paths[1]", () -> "/api/v1/seller");
        registry.add("gateway.routing.services[0].paths[2]", () -> "/api/v1/product/group/**");
        registry.add("gateway.routing.services[0].paths[3]", () -> "/api/v1/order/**");
        registry.add("gateway.routing.services[0].paths[4]", () -> "/api/v1/orders");
        registry.add("gateway.routing.services[0].paths[5]", () -> "/api/v1/qnas");
        registry.add("gateway.routing.services[0].paths[6]", () -> "/api/v1/shipment/**");
        registry.add("gateway.routing.services[0].paths[7]", () -> "/api/v1/image/presigned");
        registry.add("gateway.routing.services[0].hosts[0]", () -> "stage-admin.set-of.com");
        registry.add("gateway.routing.services[0].public-paths[0]", () -> "/api/v1/**");
        registry.add(
                "gateway.routing.services[0].rewrite-path-pattern",
                () -> "/api/v1/(?<remaining>.*)");
        registry.add(
                "gateway.routing.services[0].rewrite-path-replacement",
                () -> "/api/v1/legacy/${remaining}");

        // services[1]: legacy-admin (비대상 경로 fallback)
        registry.add("gateway.routing.services[1].id", () -> "legacy-admin");
        registry.add(
                "gateway.routing.services[1].uri",
                () -> "http://localhost:" + legacyAdminServer.port());
        registry.add("gateway.routing.services[1].paths[0]", () -> "/**");
        registry.add("gateway.routing.services[1].hosts[0]", () -> "stage-admin.set-of.com");
        registry.add("gateway.routing.services[1].public-paths[0]", () -> "/**");
    }

    @BeforeEach
    void setupWireMock() {
        authServer.resetAll();
        marketplaceServer.resetAll();
        legacyAdminServer.resetAll();

        // Clean up Redis before each test
        redisTemplate
                .execute(connection -> connection.serverCommands().flushAll())
                .blockLast(Duration.ofSeconds(5));

        // Auth Server - JWKS endpoint
        authServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Auth Server - Permission Spec endpoint
        authServer.stubFor(
                get(urlEqualTo("/api/v1/internal/endpoint-permissions/spec"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(marketplaceOmsPermissionSpec())));

        // Auth Server - User Permissions endpoint
        authServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/internal/users/.+/permissions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                                {
                                                    "success": true,
                                                    "data": {
                                                        "userId": "user-123",
                                                        "hash": "abc123hash",
                                                        "permissions": [],
                                                        "roles": [],
                                                        "generatedAt": "2025-01-01T00:00:00Z"
                                                    },
                                                    "timestamp": "2025-01-01T00:00:00",
                                                    "requestId": "test-request-id"
                                                }
                                                """)));

        // Auth Server - Tenant Config
        authServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/internal/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));

        // Marketplace Server - 모든 요청 수신 (rewrite된 경로 포함)
        marketplaceServer.stubFor(
                any(urlPathMatching("/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"service\":\"marketplace\",\"message\":\"success\"}")));

        // Legacy Admin Server - 모든 요청 수신 (fallback)
        legacyAdminServer.stubFor(
                any(urlPathMatching("/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"service\":\"legacy-admin\",\"message\":\"success\"}")));
    }

    @Nested
    @DisplayName("OMS 엔드포인트 MarketPlace 라우팅 테스트")
    class OmsEndpointMarketplaceRoutingTest {

        @Test
        @DisplayName("POST /api/v1/auth/authentication → marketplace로 라우팅되어야 한다")
        void shouldRouteAuthenticationToMarketplace() {
            webTestClient
                    .post()
                    .uri("/api/v1/auth/authentication")
                    .header("Host", "stage-admin.set-of.com")
                    .header("Content-Type", "application/json")
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(
                    postRequestedFor(urlEqualTo("/api/v1/legacy/auth/authentication")));
        }

        @Test
        @DisplayName("GET /api/v1/seller → marketplace로 라우팅되어야 한다")
        void shouldRouteSellerToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/seller")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(getRequestedFor(urlEqualTo("/api/v1/legacy/seller")));
        }

        @Test
        @DisplayName("POST /api/v1/product/group → marketplace로 라우팅되어야 한다")
        void shouldRouteProductGroupPostToMarketplace() {
            webTestClient
                    .post()
                    .uri("/api/v1/product/group")
                    .header("Host", "stage-admin.set-of.com")
                    .header("Content-Type", "application/json")
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(postRequestedFor(urlEqualTo("/api/v1/legacy/product/group")));
        }

        @Test
        @DisplayName("GET /api/v1/product/group/123 → marketplace로 라우팅되어야 한다")
        void shouldRouteProductGroupDetailToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/product/group/123")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(
                    getRequestedFor(urlEqualTo("/api/v1/legacy/product/group/123")));
        }

        @Test
        @DisplayName("GET /api/v1/orders → marketplace로 라우팅되어야 한다")
        void shouldRouteOrdersToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/orders")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(getRequestedFor(urlEqualTo("/api/v1/legacy/orders")));
        }

        @Test
        @DisplayName("GET /api/v1/order/456 → marketplace로 라우팅되어야 한다")
        void shouldRouteOrderDetailToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/order/456")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(getRequestedFor(urlEqualTo("/api/v1/legacy/order/456")));
        }

        @Test
        @DisplayName("GET /api/v1/qnas → marketplace로 라우팅되어야 한다")
        void shouldRouteQnasToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/qnas")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(getRequestedFor(urlEqualTo("/api/v1/legacy/qnas")));
        }

        @Test
        @DisplayName("GET /api/v1/shipment/company-codes → marketplace로 라우팅되어야 한다")
        void shouldRouteShipmentCompanyCodesToMarketplace() {
            webTestClient
                    .get()
                    .uri("/api/v1/shipment/company-codes")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(
                    getRequestedFor(urlEqualTo("/api/v1/legacy/shipment/company-codes")));
        }

        @Test
        @DisplayName("POST /api/v1/image/presigned → marketplace로 라우팅되어야 한다")
        void shouldRouteImagePresignedToMarketplace() {
            webTestClient
                    .post()
                    .uri("/api/v1/image/presigned")
                    .header("Host", "stage-admin.set-of.com")
                    .header("Content-Type", "application/json")
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            marketplaceServer.verify(
                    postRequestedFor(urlEqualTo("/api/v1/legacy/image/presigned")));
        }
    }

    @Nested
    @DisplayName("Path Rewrite 검증 테스트")
    class PathRewriteVerificationTest {

        @Test
        @DisplayName(
                "/api/v1/auth/authentication → /api/v1/legacy/auth/authentication 으로 리라이트되어야 한다")
        void shouldRewriteAuthenticationPath() {
            webTestClient
                    .post()
                    .uri("/api/v1/auth/authentication")
                    .header("Host", "stage-admin.set-of.com")
                    .header("Content-Type", "application/json")
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus()
                    .isOk();

            // 리라이트된 경로로 요청이 도착했는지 확인
            marketplaceServer.verify(
                    postRequestedFor(urlEqualTo("/api/v1/legacy/auth/authentication")));
            // 원본 경로로는 요청이 도착하지 않아야 함
            marketplaceServer.verify(
                    0, postRequestedFor(urlEqualTo("/api/v1/auth/authentication")));
        }

        @Test
        @DisplayName(
                "/api/v1/product/group/123/images → /api/v1/legacy/product/group/123/images 으로"
                        + " 리라이트되어야 한다")
        void shouldRewriteNestedProductPath() {
            webTestClient
                    .get()
                    .uri("/api/v1/product/group/123/images")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk();

            marketplaceServer.verify(
                    getRequestedFor(urlEqualTo("/api/v1/legacy/product/group/123/images")));
        }

        @Test
        @DisplayName("쿼리 파라미터가 리라이트 후에도 보존되어야 한다")
        void shouldPreserveQueryParametersAfterRewrite() {
            webTestClient
                    .get()
                    .uri("/api/v1/orders?page=1&size=10")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("marketplace");

            // 리라이트된 경로로 쿼리 파라미터가 보존되었는지 확인
            marketplaceServer.verify(
                    getRequestedFor(urlPathEqualTo("/api/v1/legacy/orders"))
                            .withQueryParam("page", WireMock.equalTo("1"))
                            .withQueryParam("size", WireMock.equalTo("10")));
        }
    }

    @Nested
    @DisplayName("Fallback 라우팅 테스트 (비대상 경로 → legacy-admin)")
    class FallbackRoutingTest {

        @Test
        @DisplayName("GET /api/v1/admin/users → legacy-admin으로 라우팅되어야 한다")
        void shouldRouteAdminUsersToLegacyAdmin() {
            webTestClient
                    .get()
                    .uri("/api/v1/admin/users")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-admin");

            legacyAdminServer.verify(getRequestedFor(urlEqualTo("/api/v1/admin/users")));
            // marketplace는 요청을 받지 않아야 함
            marketplaceServer.verify(0, getRequestedFor(urlEqualTo("/api/v1/admin/users")));
            marketplaceServer.verify(0, getRequestedFor(urlEqualTo("/api/v1/legacy/admin/users")));
        }

        @Test
        @DisplayName("GET /api/v1/unknown/path → legacy-admin으로 라우팅되어야 한다")
        void shouldRouteUnknownPathToLegacyAdmin() {
            webTestClient
                    .get()
                    .uri("/api/v1/unknown/path")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-admin");

            legacyAdminServer.verify(getRequestedFor(urlEqualTo("/api/v1/unknown/path")));
        }

        @Test
        @DisplayName("GET /some/other/path → legacy-admin으로 라우팅되어야 한다")
        void shouldRouteOtherPathToLegacyAdmin() {
            webTestClient
                    .get()
                    .uri("/some/other/path")
                    .header("Host", "stage-admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-admin");

            legacyAdminServer.verify(getRequestedFor(urlEqualTo("/some/other/path")));
        }
    }

    @Nested
    @DisplayName("Configuration 로딩 테스트")
    class ConfigurationLoadingTest {

        @Test
        @DisplayName("marketplace-oms-legacy 서비스의 rewritePathPattern 설정이 올바르게 로드되어야 한다")
        void shouldLoadRewritePathPattern() {
            var marketplaceOms =
                    routingProperties.getServices().stream()
                            .filter(s -> "marketplace-oms-legacy".equals(s.getId()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "marketplace-oms-legacy service not found"));

            assertThat(marketplaceOms.getRewritePathPattern())
                    .isEqualTo("/api/v1/(?<remaining>.*)");
        }

        @Test
        @DisplayName("marketplace-oms-legacy 서비스의 rewritePathReplacement 설정이 올바르게 로드되어야 한다")
        void shouldLoadRewritePathReplacement() {
            var marketplaceOms =
                    routingProperties.getServices().stream()
                            .filter(s -> "marketplace-oms-legacy".equals(s.getId()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "marketplace-oms-legacy service not found"));

            assertThat(marketplaceOms.getRewritePathReplacement())
                    .isEqualTo("/api/v1/legacy/${remaining}");
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * MarketPlace OMS 라우팅 테스트용 Permission Spec
     *
     * <p>marketplace-oms-legacy와 legacy-admin 서비스의 모든 경로를 public으로 설정
     */
    private static String marketplaceOmsPermissionSpec() {
        return """
               {
                   "success": true,
                   "data": {
                       "version": "v1.0",
                       "updatedAt": "2025-01-01T00:00:00Z",
                       "endpoints": [
                           {
                               "serviceName": "marketplace-oms-legacy",
                               "pathPattern": "/.*",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Marketplace OMS all public (GET)"
                           },
                           {
                               "serviceName": "marketplace-oms-legacy",
                               "pathPattern": "/.*",
                               "httpMethod": "POST",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Marketplace OMS all public (POST)"
                           },
                           {
                               "serviceName": "legacy-admin",
                               "pathPattern": "/.*",
                               "httpMethod": "GET",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Legacy admin all public (GET)"
                           },
                           {
                               "serviceName": "legacy-admin",
                               "pathPattern": "/.*",
                               "httpMethod": "POST",
                               "requiredPermissions": [],
                               "requiredRoles": [],
                               "isPublic": true,
                               "description": "Legacy admin all public (POST)"
                           }
                       ]
                   },
                   "timestamp": "2025-01-01T00:00:00",
                   "requestId": "test-request-id"
               }
               """;
    }
}
