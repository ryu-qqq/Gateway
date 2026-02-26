package com.ryuqq.gateway.integration.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import com.ryuqq.gateway.integration.helper.PermissionTestFixture;
import com.ryuqq.gateway.integration.helper.TenantConfigTestFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Token Refresh Integration Test
 *
 * <p>Token Refresh 기능 전용 통합 테스트 (Circuit Breaker 격리를 위해 별도 클래스로 분리)
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
@Import(TokenRefreshIntegrationTest.TestGatewayConfig.class)
class TokenRefreshIntegrationTest {

    static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withReuse(true);

    @Autowired private WebTestClient webTestClient;

    @Autowired
    private org.springframework.data.redis.core.ReactiveRedisTemplate<String, String> redisTemplate;

    @MockitoBean private AuthHubClient authHubClient;


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
                            "test-route",
                            r ->
                                    r.path("/test/**")
                                            .uri("http://localhost:" + wireMockServer.port()))
                    .build();
        }
    }

    @BeforeEach
    void setupWireMock() {
        wireMockServer.resetAll();

        // Clear Redis blacklist data between tests
        redisTemplate
                .getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .flushAll()
                .block();


        // JWKS endpoint mock
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Permission Spec endpoint mock (Internal API)
        wireMockServer.stubFor(
                get(urlEqualTo(PermissionTestFixture.PERMISSION_SPEC_PATH))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture.allPublicPermissionSpec(
                                                        "/test/.*"))));

        // User Permissions endpoint mock (Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching(PermissionTestFixture.USER_PERMISSIONS_PATH_PATTERN))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture
                                                        .userPermissionHashResponse())));

        // Downstream service mock
        wireMockServer.stubFor(
                get(urlEqualTo("/test/resource"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));

        // Tenant Config API mock (Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/internal/tenants/.+/config"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001", false))));

        // AuthHubClient mock (SDK uses internal HTTP client, so we mock the port)
        // Mock fetchPublicKeys for JWT signature verification
        PublicKey testPublicKey =
                PublicKey.fromRSAPublicKey(JwtTestFixture.defaultKid(), JwtTestFixture.publicKey());
        when(authHubClient.fetchPublicKeys()).thenReturn(Flux.just(testPublicKey));

        // Mock refreshAccessToken for token refresh flow
        String newAccessToken = JwtTestFixture.aValidJwt("refreshed-user-123");
        String newRefreshToken = "new-refresh-token-from-authhub-32chars";
        when(authHubClient.refreshAccessToken(anyString(), anyString()))
                .thenReturn(Mono.just(TokenPair.of(newAccessToken, newRefreshToken)));
    }

    @Test
    @DisplayName("만료된 토큰 + refresh_token 쿠키로 요청 시 X-New-Access-Token 헤더가 반환되어야 한다")
    void shouldReturnNewAccessTokenHeaderOnTokenRefresh() {
        // given
        String expiredJwt = JwtTestFixture.anExpiredJwt();
        // Use unique refresh token to avoid blacklist collision between tests
        String uniqueRefreshToken = "unique-refresh-token-for-test-1-32chars";

        // when & then
        webTestClient
                .get()
                .uri("/test/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                .cookie("refresh_token", uniqueRefreshToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists("X-New-Access-Token")
                .expectHeader()
                .value(
                        "X-New-Access-Token",
                        value -> {
                            // JWT format: header.payload.signature (3 parts separated by dots)
                            assertThat(value).isNotNull();
                            assertThat(value.split("\\.")).hasSize(3);
                        });
    }

    @Test
    @DisplayName("만료된 토큰 + refresh_token 쿠키로 요청 시 새 refresh_token 쿠키가 설정되어야 한다")
    void shouldSetNewRefreshTokenCookieOnTokenRefresh() {
        // given
        String expiredJwt = JwtTestFixture.anExpiredJwt();
        // Use unique refresh token to avoid blacklist collision between tests
        String uniqueRefreshToken = "unique-refresh-token-for-test-2-32chars";

        // when & then
        webTestClient
                .get()
                .uri("/test/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                .cookie("refresh_token", uniqueRefreshToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectCookie()
                .exists("refresh_token")
                .expectCookie()
                .value(
                        "refresh_token",
                        value ->
                                assertThat(value)
                                        .isEqualTo("new-refresh-token-from-authhub-32chars"))
                .expectCookie()
                .httpOnly("refresh_token", true)
                .expectCookie()
                .secure("refresh_token", true);
    }

    @Test
    @DisplayName("유효한 토큰 요청 시 X-New-Access-Token 헤더가 없어야 한다")
    void shouldNotReturnNewAccessTokenHeaderWhenTokenValid() {
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
                .doesNotExist("X-New-Access-Token");
    }

    @Test
    @DisplayName("만료된 토큰 + refresh_token 없이 요청 시 401 반환")
    void shouldReturn401WhenExpiredTokenWithoutRefreshToken() {
        // given
        String expiredJwt = JwtTestFixture.anExpiredJwt();

        // when & then - refresh_token 쿠키 없이 요청
        webTestClient
                .get()
                .uri("/test/resource")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredJwt)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
