package com.ryuqq.gateway.integration.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.ryuqq.gateway.integration.base.GatewayIntegrationTest;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Tenant Header Propagation Integration Test
 *
 * <p>JWT에서 추출한 tenantId가 X-Tenant-Id 헤더로 downstream 서비스에 전달되는지 검증
 *
 * <p>JwtAuthenticationFilter에서 처리하는 기본 기능 테스트 (TenantIsolationFilter 불필요)
 *
 * @author development-team
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(TenantHeaderPropagationIntegrationTest.TestGatewayConfig.class)
class TenantHeaderPropagationIntegrationTest extends GatewayIntegrationTest {

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route(
                            "tenant-header-test-route",
                            r ->
                                    r.path("/api/test/**")
                                            .uri("http://localhost:" + authHubWireMock.port()))
                    .build();
        }
    }

    @BeforeEach
    void setupDownstreamMock() {
        // Mock downstream service that echoes back headers
        authHubWireMock.stubFor(
                get(urlPathEqualTo("/api/test/orders"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"orders\":[]}")));
    }

    @Test
    @DisplayName("JWT의 tenantId가 X-Tenant-Id 헤더로 downstream 서비스에 전달되어야 한다")
    void shouldPropagateXTenantIdHeaderToDownstreamService() {
        // given
        String tenantId = "tenant-001";
        String validJwt = JwtTestFixture.aValidJwtWithTenant("user-123", tenantId);

        // when
        webTestClient
                .get()
                .uri("/api/test/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .exchange()
                .expectStatus()
                .isOk();

        // then - Verify downstream received X-Tenant-Id header
        authHubWireMock.verify(
                getRequestedFor(urlPathEqualTo("/api/test/orders"))
                        .withHeader(
                                "X-Tenant-Id",
                                com.github.tomakehurst.wiremock.client.WireMock.equalTo(tenantId)));
    }

    @Test
    @DisplayName("다른 tenantId를 가진 JWT도 올바르게 X-Tenant-Id 헤더가 전달되어야 한다")
    void shouldPropagateCorrectTenantIdForDifferentTenants() {
        // given
        String tenantId = "tenant-002";
        String validJwt = JwtTestFixture.aValidJwtWithTenant("user-456", tenantId);

        // when
        webTestClient
                .get()
                .uri("/api/test/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .exchange()
                .expectStatus()
                .isOk();

        // then
        authHubWireMock.verify(
                getRequestedFor(urlPathEqualTo("/api/test/orders"))
                        .withHeader(
                                "X-Tenant-Id",
                                com.github.tomakehurst.wiremock.client.WireMock.equalTo(tenantId)));
    }

    @Test
    @DisplayName("X-User-Id 헤더도 downstream 서비스에 전달되어야 한다")
    void shouldPropagateXUserIdHeaderToDownstreamService() {
        // given
        String tenantId = "tenant-001";
        String userId = "user-789";
        String validJwt = JwtTestFixture.aValidJwtWithTenant(userId, tenantId);

        // when
        webTestClient
                .get()
                .uri("/api/test/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                .exchange()
                .expectStatus()
                .isOk();

        // then - Verify downstream received X-User-Id header
        // Note: X-User-Id contains the JWT subject value (e.g., "user-789")
        authHubWireMock.verify(
                getRequestedFor(urlPathEqualTo("/api/test/orders"))
                        .withHeader(
                                "X-User-Id",
                                com.github.tomakehurst.wiremock.client.WireMock.equalTo(userId)));
    }
}
