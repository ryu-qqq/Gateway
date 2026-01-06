package com.ryuqq.gateway.integration.base;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import com.ryuqq.gateway.integration.helper.PermissionTestFixture;
import com.ryuqq.gateway.integration.helper.TenantConfigTestFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Gateway integration tests.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>TestContainers (Redis)
 *   <li>WireMock for external HTTP APIs (AuthHub)
 *   <li>WebTestClient for E2E HTTP testing
 *   <li>Common WireMock stub configurations
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
public abstract class GatewayIntegrationTest {

    // Static containers for reuse across tests
    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withReuse(true);

    // WireMock server for AuthHub API
    protected static WireMockServer authHubWireMock;

    static {
        authHubWireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        authHubWireMock.start();
    }

    @Autowired protected WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redis
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);

        // Redisson config
        registry.add(
                "spring.redis.redisson.config",
                () ->
                        String.format(
                                "singleServerConfig:\n  address: redis://%s:%d",
                                REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getFirstMappedPort()));

        // AuthHub Client
        registry.add("authhub.client.base-url", () -> "http://localhost:" + authHubWireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (authHubWireMock != null) {
            authHubWireMock.stop();
        }
    }

    @BeforeEach
    void setUpBase() {
        resetWireMockServers();
        setupDefaultWireMockStubs();
    }

    /** Reset all WireMock servers. */
    protected void resetWireMockServers() {
        if (authHubWireMock != null) {
            authHubWireMock.resetAll();
        }
    }

    /**
     * Setup default WireMock stubs for common endpoints. Subclasses can override this to customize
     * stubs.
     */
    protected void setupDefaultWireMockStubs() {
        // JWKS endpoint
        authHubWireMock.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Permission Spec endpoint
        authHubWireMock.stubFor(
                get(urlEqualTo("/api/v1/permissions/spec"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture
                                                        .legacyServicesPermissionSpec())));

        // Tenant Config endpoint
        authHubWireMock.stubFor(
                get(WireMock.urlPathMatching("/api/v1/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));
    }

    /** Returns the WireMock server for AuthHub. */
    protected WireMockServer getAuthHubWireMock() {
        return authHubWireMock;
    }

    /** Returns the base URL for API calls. */
    protected String baseUrl() {
        return webTestClient.toString();
    }
}
