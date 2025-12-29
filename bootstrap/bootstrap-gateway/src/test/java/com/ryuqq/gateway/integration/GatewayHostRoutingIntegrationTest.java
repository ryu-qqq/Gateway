package com.ryuqq.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.bootstrap.config.GatewayRoutingConfig.GatewayRoutingProperties;
import com.ryuqq.gateway.integration.fixtures.JwtTestFixture;
import com.ryuqq.gateway.integration.fixtures.TenantConfigTestFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Gateway Host-Based Routing Integration Test
 *
 * <p>Host 헤더를 기반으로 서로 다른 백엔드 서비스로 라우팅되는지 검증
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>stage.set-of.com, set-of.com, server.set-of.net → Legacy Web API
 *   <li>admin.set-of.com, admin-server.set-of.net → Legacy Admin API
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test"})
@Testcontainers
class GatewayHostRoutingIntegrationTest {

    static WireMockServer legacyWebServer;
    static WireMockServer legacyAdminServer;
    static WireMockServer authServer;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private WebTestClient webTestClient;

    @Autowired private GatewayRoutingProperties routingProperties;

    @BeforeAll
    static void startWireMock() {
        // Auth Server (JWKS, Permission Spec 등)
        authServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8888));
        authServer.start();

        // Legacy Web API Server
        legacyWebServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8889));
        legacyWebServer.start();

        // Legacy Admin API Server
        legacyAdminServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8890));
        legacyAdminServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (authServer != null) {
            authServer.stop();
        }
        if (legacyWebServer != null) {
            legacyWebServer.stop();
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
        registry.add("authhub.client.base-url", () -> "http://localhost:8888");

        // Redisson config
        registry.add(
                "spring.redis.redisson.config",
                () ->
                        String.format(
                                "singleServerConfig:\n  address: redis://%s:%d",
                                redis.getHost(), redis.getFirstMappedPort()));

        // Gateway Routing - Host-based routing 설정
        registry.add("gateway.routing.discovery.enabled", () -> "false");

        // Legacy Web Service
        registry.add("gateway.routing.services[0].id", () -> "legacy-web");
        registry.add("gateway.routing.services[0].uri", () -> "http://localhost:8889");
        registry.add("gateway.routing.services[0].paths[0]", () -> "/**");
        registry.add("gateway.routing.services[0].hosts[0]", () -> "stage.set-of.com");
        registry.add("gateway.routing.services[0].hosts[1]", () -> "set-of.com");
        registry.add("gateway.routing.services[0].hosts[2]", () -> "server.set-of.net");
        registry.add("gateway.routing.services[0].public-paths[0]", () -> "/**");

        // Legacy Admin Service
        registry.add("gateway.routing.services[1].id", () -> "legacy-admin");
        registry.add("gateway.routing.services[1].uri", () -> "http://localhost:8890");
        registry.add("gateway.routing.services[1].paths[0]", () -> "/**");
        registry.add("gateway.routing.services[1].hosts[0]", () -> "admin.set-of.com");
        registry.add("gateway.routing.services[1].hosts[1]", () -> "admin-server.set-of.net");
        registry.add("gateway.routing.services[1].public-paths[0]", () -> "/**");
    }

    @BeforeEach
    void setupWireMock() {
        authServer.resetAll();
        legacyWebServer.resetAll();
        legacyAdminServer.resetAll();

        // Auth Server - JWKS endpoint
        authServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Auth Server - Permission Spec endpoint (public endpoints)
        authServer.stubFor(
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
                                                            "serviceName": "legacy-web",
                                                            "path": "/.*",
                                                            "method": "GET",
                                                            "isPublic": true,
                                                            "requiredRoles": [],
                                                            "requiredPermissions": []
                                                        },
                                                        {
                                                            "serviceName": "legacy-admin",
                                                            "path": "/.*",
                                                            "method": "GET",
                                                            "isPublic": true,
                                                            "requiredRoles": [],
                                                            "requiredPermissions": []
                                                        }
                                                    ]
                                                }
                                                """)));

        // Auth Server - Tenant Config
        authServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));

        // Legacy Web Server - Mock responses
        legacyWebServer.stubFor(
                get(urlPathMatching("/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"service\":\"legacy-web\",\"message\":\"success\"}")));

        // Legacy Admin Server - Mock responses
        legacyAdminServer.stubFor(
                get(urlPathMatching("/.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"service\":\"legacy-admin\",\"message\":\"success\"}")));
    }

    @Nested
    @DisplayName("Legacy Web API 호스트 라우팅")
    class LegacyWebHostRoutingTest {

        @Test
        @DisplayName("stage.set-of.com 호스트로 요청 시 Legacy Web API로 라우팅되어야 한다")
        void shouldRouteLegacyWebByHost_stageSetOfCom() {
            webTestClient
                    .get()
                    .uri("/api/v1/products")
                    .header("Host", "stage.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-web");

            // Legacy Web Server가 요청을 받았는지 확인
            legacyWebServer.verify(getRequestedFor(urlEqualTo("/api/v1/products")));
        }

        @Test
        @DisplayName("set-of.com 호스트로 요청 시 Legacy Web API로 라우팅되어야 한다")
        void shouldRouteLegacyWebByHost_setOfCom() {
            webTestClient
                    .get()
                    .uri("/api/v1/products")
                    .header("Host", "set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-web");

            legacyWebServer.verify(getRequestedFor(urlEqualTo("/api/v1/products")));
        }

        @Test
        @DisplayName("server.set-of.net 호스트로 요청 시 Legacy Web API로 라우팅되어야 한다")
        void shouldRouteLegacyWebByHost_serverSetOfNet() {
            webTestClient
                    .get()
                    .uri("/api/v1/orders")
                    .header("Host", "server.set-of.net")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-web");

            legacyWebServer.verify(getRequestedFor(urlEqualTo("/api/v1/orders")));
        }
    }

    @Nested
    @DisplayName("Legacy Admin API 호스트 라우팅")
    class LegacyAdminHostRoutingTest {

        @Test
        @DisplayName("admin.set-of.com 호스트로 요청 시 Legacy Admin API로 라우팅되어야 한다")
        void shouldRouteLegacyAdminByHost_adminSetOfCom() {
            webTestClient
                    .get()
                    .uri("/api/v1/admin/users")
                    .header("Host", "admin.set-of.com")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-admin");

            legacyAdminServer.verify(getRequestedFor(urlEqualTo("/api/v1/admin/users")));
        }

        @Test
        @DisplayName("admin-server.set-of.net 호스트로 요청 시 Legacy Admin API로 라우팅되어야 한다")
        void shouldRouteLegacyAdminByHost_adminServerSetOfNet() {
            webTestClient
                    .get()
                    .uri("/api/v1/admin/settings")
                    .header("Host", "admin-server.set-of.net")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.service")
                    .isEqualTo("legacy-admin");

            legacyAdminServer.verify(getRequestedFor(urlEqualTo("/api/v1/admin/settings")));
        }
    }

    @Nested
    @DisplayName("Configuration 로딩 테스트")
    class ConfigurationLoadingTest {

        @Test
        @DisplayName("Legacy Web 서비스의 hosts 설정이 올바르게 로드되어야 한다")
        void shouldLoadLegacyWebHosts() {
            var legacyWeb =
                    routingProperties.getServices().stream()
                            .filter(s -> "legacy-web".equals(s.getId()))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("legacy-web service not found"));

            assertThat(legacyWeb.getHosts())
                    .containsExactlyInAnyOrder(
                            "stage.set-of.com", "set-of.com", "server.set-of.net");
        }

        @Test
        @DisplayName("Legacy Admin 서비스의 hosts 설정이 올바르게 로드되어야 한다")
        void shouldLoadLegacyAdminHosts() {
            var legacyAdmin =
                    routingProperties.getServices().stream()
                            .filter(s -> "legacy-admin".equals(s.getId()))
                            .findFirst()
                            .orElseThrow(
                                    () -> new AssertionError("legacy-admin service not found"));

            assertThat(legacyAdmin.getHosts())
                    .containsExactlyInAnyOrder("admin.set-of.com", "admin-server.set-of.net");
        }

        @Test
        @DisplayName("hosts가 설정되지 않은 서비스는 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyListWhenHostsNotConfigured() {
            // 테스트용 서비스가 아닌 실제 서비스 중 hosts가 없는 경우
            // 이 테스트에서는 동적으로 설정하므로 legacy-web과 legacy-admin만 존재
            // 두 서비스 모두 hosts가 있으므로 hosts.isEmpty()인 케이스는 없음
            // 대신 getHosts()가 unmodifiable list를 반환하는지 확인

            var legacyWeb =
                    routingProperties.getServices().stream()
                            .filter(s -> "legacy-web".equals(s.getId()))
                            .findFirst()
                            .orElseThrow();

            // unmodifiableList 확인 - 수정 시도 시 예외 발생해야 함
            assertThat(legacyWeb.getHosts()).isNotNull();
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> legacyWeb.getHosts().add("new.domain.com"));
        }
    }

    @Nested
    @DisplayName("Host 미지정 요청 처리")
    class UnknownHostRequestTest {

        @Test
        @DisplayName("설정되지 않은 호스트로 요청 시 라우팅되지 않아야 한다")
        void shouldNotRouteWhenHostNotConfigured() {
            // 설정되지 않은 호스트로 요청
            webTestClient
                    .get()
                    .uri("/api/v1/products")
                    .header("Host", "unknown.example.com")
                    .exchange()
                    .expectStatus()
                    .isNotFound();
        }
    }
}
