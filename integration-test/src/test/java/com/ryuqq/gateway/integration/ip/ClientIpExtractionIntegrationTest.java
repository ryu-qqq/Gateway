package com.ryuqq.gateway.integration.ip;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
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
 * ClientIpExtractor Integration Test
 *
 * <p>클라이언트 IP 추출 로직 E2E 테스트 (AWS CloudFront/ALB 환경 시뮬레이션)
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>X-Forwarded-For 헤더 기반 IP 추출
 *   <li>CloudFront-Viewer-Address 헤더 기반 IP 추출
 *   <li>IPv4/IPv6 형식 지원
 *   <li>잘못된 IP 형식 처리
 * </ul>
 *
 * <p>참고: 이 테스트는 실제 ClientIpExtractor 빈을 사용합니다. RateLimitIntegrationTest는 Mock을 사용하여 IP를 제어합니다.
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
@Import(ClientIpExtractionIntegrationTest.TestGatewayConfig.class)
class ClientIpExtractionIntegrationTest {

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
        // Rate limit 비활성화 - IP 추출 로직만 테스트
        registry.add("gateway.rate-limit.enabled", () -> "false");
        // Redisson 설정
        registry.add(
                "spring.redis.redisson.config",
                () ->
                        String.format(
                                "singleServerConfig:\n  address: redis://%s:%d",
                                redis.getHost(), redis.getFirstMappedPort()));
    }

    @TestConfiguration
    static class TestGatewayConfig {
        @Bean
        public RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route(
                            "ip-extraction-test-route",
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

        // Mock Tenant Config API
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));
    }

    @Nested
    @DisplayName("X-Forwarded-For 헤더 기반 IP 추출")
    class XForwardedForTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 요청이 정상 처리되어야 한다")
        void shouldProcessRequestWithXForwardedForHeader() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            String clientIp = "203.0.113.50";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", clientIp)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("X-Forwarded-For에 여러 IP가 있으면 첫 번째 IP가 사용되어야 한다")
        void shouldUseFirstIpFromXForwardedForHeader() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // AWS ALB가 추가하는 형식: client, proxy1, proxy2
            String xForwardedFor = "203.0.113.50, 10.0.0.1, 10.0.0.2";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", xForwardedFor)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더 없이도 요청이 정상 처리되어야 한다 (RemoteAddress 사용)")
        void shouldFallbackToRemoteAddressWhenNoXForwardedFor() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then - X-Forwarded-For 없이 요청
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
    @DisplayName("CloudFront-Viewer-Address 헤더 기반 IP 추출")
    class CloudFrontViewerAddressTest {

        @Test
        @DisplayName("CloudFront-Viewer-Address IPv4 형식 (IP:port)이 정상 처리되어야 한다")
        void shouldProcessCloudFrontViewerAddressIPv4() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // CloudFront 형식: IP:port
            String viewerAddress = "203.0.113.100:46532";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("CloudFront-Viewer-Address", viewerAddress)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("CloudFront-Viewer-Address IPv6 형식이 정상 처리되어야 한다")
        void shouldProcessCloudFrontViewerAddressIPv6() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // IPv6 형식: [IP]:port
            String viewerAddress = "[2001:db8::1]:8080";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("CloudFront-Viewer-Address", viewerAddress)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("CloudFront-Viewer-Address가 X-Forwarded-For보다 우선되어야 한다")
        void shouldPrioritizeCloudFrontViewerAddressOverXForwardedFor() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            String viewerAddress = "203.0.113.200:46532";
            String xForwardedFor = "203.0.113.100";

            // when & then - 두 헤더 모두 있을 때 CloudFront-Viewer-Address 우선
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("CloudFront-Viewer-Address", viewerAddress)
                    .header("X-Forwarded-For", xForwardedFor)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("IPv6 지원")
    class IPv6SupportTest {

        @Test
        @DisplayName("X-Forwarded-For에 IPv6 주소가 있어도 정상 처리되어야 한다")
        void shouldProcessIPv6InXForwardedFor() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            String ipv6Address = "2001:db8:85a3::8a2e:370:7334";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", ipv6Address)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("IPv4-mapped IPv6 주소가 정상 처리되어야 한다")
        void shouldProcessIPv4MappedIPv6() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // IPv4-mapped IPv6 형식
            String ipv4MappedIpv6 = "::ffff:192.0.2.1";

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", ipv4MappedIpv6)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("잘못된 IP 형식이어도 요청이 처리되어야 한다 (fallback to RemoteAddress)")
        void shouldFallbackWhenInvalidIpFormat() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            String invalidIp = "not-an-ip-address";

            // when & then - 잘못된 형식이어도 RemoteAddress로 fallback
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", invalidIp)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        @Test
        @DisplayName("빈 X-Forwarded-For 헤더도 정상 처리되어야 한다")
        void shouldHandleEmptyXForwardedFor() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/test/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", "")
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }
}
