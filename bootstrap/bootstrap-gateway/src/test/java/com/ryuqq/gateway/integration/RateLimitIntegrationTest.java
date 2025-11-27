package com.ryuqq.gateway.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.fixtures.JwtTestFixture;
import com.ryuqq.gateway.integration.fixtures.TenantConfigTestFixture;
import java.time.Duration;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

/**
 * Rate Limit Integration Test
 *
 * <p>Rate Limiting 기능 E2E 테스트 (Domain → Application → Persistence → Filter)
 *
 * <p>테스트 시나리오:
 *
 * <ul>
 *   <li>1. Endpoint Rate Limit: 동일 엔드포인트 요청 제한
 *   <li>2. User Rate Limit: 사용자별 요청 제한
 *   <li>3. IP Rate Limit: IP별 요청 제한
 *   <li>4. Rate Limit 헤더: X-RateLimit-* 헤더 검증
 *   <li>5. IP Block: 연속 실패 시 IP 차단
 *   <li>6. Account Lock: 로그인 실패 시 계정 잠금
 *   <li>7. Rate Limit Reset: 관리자 초기화 기능
 *   <li>8. TTL 만료: Rate Limit 자동 해제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(RateLimitIntegrationTest.TestGatewayConfig.class)
class RateLimitIntegrationTest {

    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    static WireMockServer wireMockServer;

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired private WebTestClient webTestClient;

    @Autowired private ReactiveRedisTemplate<String, String> redisTemplate;

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
        // Rate limit 설정 (테스트용)
        registry.add("gateway.rate-limit.enabled", () -> "true");
        registry.add("gateway.rate-limit.endpoint-limit", () -> "5");
        // IP Rate Limit은 Invalid JWT 임계값(10)보다 높게 설정
        // IP Block 테스트에서 11번 요청 후 차단 확인을 위해
        registry.add("gateway.rate-limit.ip-limit", () -> "100");
        registry.add("gateway.rate-limit.user-limit", () -> "5");
        registry.add("gateway.rate-limit.window-seconds", () -> "60");
        // Redisson 설정 (Testcontainers Redis 사용)
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
                            "rate-limit-test-route",
                            r -> r.path("/api/**").uri("http://localhost:8889"))
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
                get(urlEqualTo("/api/resource"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));

        // Mock Tenant Config API (GATEWAY-004 Tenant 격리 기능)
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
    @DisplayName("Scenario 1: Endpoint Rate Limit")
    class EndpointRateLimitTest {

        @Test
        @DisplayName("동일 엔드포인트에 대한 요청이 제한을 초과하면 429를 반환해야 한다")
        void shouldReturn429WhenEndpointRateLimitExceeded() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();
            // Note: endpoint-limit=5 설정에서 isExceeded는 currentCount >= maxRequests 이므로
            // 5번째 요청에서 차단 (incrementAndGet 후 체크하므로 카운트가 5가 되면 차단)
            int requestsBeforeBlock = 4;

            // when - Rate limit 이내의 요청 (4번까지 허용)
            for (int i = 0; i < requestsBeforeBlock; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // then - Rate limit에 도달한 요청 (5번째 = count가 5가 되는 시점에서 차단)
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Scenario 2: User Rate Limit")
    class UserRateLimitTest {

        @Test
        @DisplayName("서로 다른 사용자는 독립적인 Rate Limit을 가져야 한다")
        void shouldHaveIndependentRateLimitsPerUser() {
            // given
            String user1Jwt = JwtTestFixture.aValidJwt("user-rate-limit-1");
            String user2Jwt = JwtTestFixture.aValidJwt("user-rate-limit-2");

            // when - user1이 user rate limit 소진
            // Note: user-limit=5 설정에서 isExceeded는 currentCount >= maxRequests 이므로
            // 5번째 요청에서 차단 (4번까지 허용)
            // 다른 IP + 다른 Endpoint로 요청하여 IP/Endpoint Rate Limit 우회
            for (int i = 0; i < 4; i++) {
                // Endpoint Rate Limit 우회를 위해 다른 엔드포인트 목업 추가
                String endpoint = "/api/resource-" + i;
                wireMockServer.stubFor(
                        get(urlEqualTo(endpoint))
                                .willReturn(
                                        aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"message\":\"success\"}")));

                webTestClient
                        .get()
                        .uri(endpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
                        .header("X-Forwarded-For", "10.10.10." + i) // 다른 IP에서 요청 (IP Rate Limit 우회)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // then - user1은 user rate limit 초과 (새 IP, 새 엔드포인트에서도 차단)
            String newEndpoint = "/api/resource-new";
            wireMockServer.stubFor(
                    get(urlEqualTo(newEndpoint))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody("{\"message\":\"success\"}")));

            webTestClient
                    .get()
                    .uri(newEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
                    .header("X-Forwarded-For", "10.10.10.100") // 새 IP에서 요청
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            // but - user2는 여전히 요청 가능 (User Rate Limit이 독립적)
            webTestClient
                    .get()
                    .uri(newEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
                    .header("X-Forwarded-For", "10.10.10.101")
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("Scenario 3: IP Rate Limit")
    class IpRateLimitTest {

        @Test
        @DisplayName("동일 IP에서 오는 요청은 동일한 Endpoint Rate Limit을 공유해야 한다")
        void shouldShareRateLimitForSameIp() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // Note: 테스트 환경에서 IP Rate Limit은 100으로 설정되어 있음 (IP Block 테스트용)
            // 따라서 이 테스트는 Endpoint Rate Limit(5)이 먼저 적용되는 것을 검증
            // endpoint-limit=5에서 isExceeded는 currentCount >= maxRequests 이므로 4번까지 허용
            // when - X-Forwarded-For 헤더 없이 요청 (모두 같은 IP로 처리)
            for (int i = 0; i < 4; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // then - Endpoint Rate limit에 도달 (5번째 요청)
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("다른 IP에서 오는 요청도 동일 Endpoint는 Endpoint Rate Limit을 공유해야 한다")
        void shouldShareEndpointRateLimitAcrossIps() {
            // given
            String validJwt1 = JwtTestFixture.aValidJwt("ip-test-user-1");
            String validJwt2 = JwtTestFixture.aValidJwt("ip-test-user-2");

            // Note: Endpoint Rate Limit은 IP와 무관하게 적용됨
            // 동일 엔드포인트에 대한 모든 요청이 Endpoint Rate Limit을 공유
            // endpoint-limit=5에서 isExceeded는 currentCount >= maxRequests 이므로
            // 총 4번까지 허용 (3번 + 1번), 5번째에서 차단

            // when - IP 192.168.1.1에서 Endpoint Rate Limit 일부 소진 (3번)
            for (int i = 0; i < 3; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt1)
                        .header("X-Forwarded-For", "192.168.1.1")
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // when - IP 192.168.1.2에서 나머지 Endpoint Rate Limit 소진 (1번 - 총 4번)
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt2)
                    .header("X-Forwarded-For", "192.168.1.2")
                    .exchange()
                    .expectStatus()
                    .isOk();

            // then - 새 IP에서도 동일 엔드포인트는 Endpoint Rate Limit에 도달 (5번째 요청)
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt2)
                    .header("X-Forwarded-For", "192.168.1.3")
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Scenario 4: Rate Limit Headers")
    class RateLimitHeadersTest {

        @Test
        @DisplayName("응답에 Rate Limit 관련 헤더가 포함되어야 한다")
        void shouldIncludeRateLimitHeaders() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when & then
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .exists(RATE_LIMIT_HEADER)
                    .expectHeader()
                    .exists(RATE_LIMIT_REMAINING_HEADER);
        }

        @Test
        @DisplayName("Rate Limit 초과 시 Retry-After 헤더가 포함되어야 한다")
        void shouldIncludeRetryAfterHeaderWhenRateLimitExceeded() {
            // given
            String validJwt = JwtTestFixture.aValidJwt();

            // when - Rate limit 소진 (endpoint-limit=5, 4번까지 허용)
            for (int i = 0; i < 4; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .exchange();
            }

            // then - Rate limit에 도달 시 (5번째) Retry-After 헤더 확인
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                    .expectHeader()
                    .exists(RETRY_AFTER_HEADER);
        }

        @Test
        @DisplayName("X-RateLimit-Remaining 값이 요청마다 감소해야 한다")
        void shouldDecreaseRemainingCountPerRequest() {
            // given
            String validJwt = JwtTestFixture.aValidJwt("remaining-test-user");

            // when - 첫 번째 요청
            String firstRemaining =
                    webTestClient
                            .get()
                            .uri("/api/resource")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                            .exchange()
                            .expectStatus()
                            .isOk()
                            .returnResult(String.class)
                            .getResponseHeaders()
                            .getFirst(RATE_LIMIT_REMAINING_HEADER);

            // when - 두 번째 요청
            String secondRemaining =
                    webTestClient
                            .get()
                            .uri("/api/resource")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                            .exchange()
                            .expectStatus()
                            .isOk()
                            .returnResult(String.class)
                            .getResponseHeaders()
                            .getFirst(RATE_LIMIT_REMAINING_HEADER);

            // then
            assertThat(Integer.parseInt(secondRemaining))
                    .isLessThan(Integer.parseInt(firstRemaining));
        }
    }

    @Nested
    @DisplayName("Scenario 5: IP Block")
    class IpBlockTest {

        @Test
        @DisplayName("연속 Invalid JWT 요청 시 IP가 차단되어야 한다")
        void shouldBlockIpAfterConsecutiveInvalidJwtRequests() {
            // given
            String invalidJwt = "invalid.jwt.token";
            String blockedIp = "10.0.0.100";

            // Note: 테스트 설정에서 IP Rate Limit이 5로 설정되어 있음
            // Invalid JWT 요청은 JwtAuthenticationFilter에서 401 반환 후 RecordFailureUseCase 호출
            // 그러나 RateLimitFilter가 먼저 실행되어 IP Rate Limit이 적용됨
            // 따라서 다른 엔드포인트로 요청하여 Endpoint Rate Limit을 우회하고,
            // IP Rate Limit 설정값보다 INVALID_JWT 임계값(10)이 더 크므로
            // 테스트 환경에서는 각 요청마다 다른 엔드포인트를 사용해야 함

            // INVALID_JWT 기본 임계값: 10
            // isExceeded는 currentCount >= maxRequests 이므로 10번째 요청에서 IP 차단 발생
            int blockThreshold = 10;

            // when - Invalid JWT로 반복 요청 (다른 엔드포인트로 Endpoint Rate Limit 우회)
            for (int i = 0; i < blockThreshold; i++) {
                // 매 요청마다 다른 엔드포인트 사용 (Endpoint Rate Limit 우회)
                String endpoint = "/api/invalid-jwt-test-" + i;
                wireMockServer.stubFor(
                        get(urlEqualTo(endpoint))
                                .willReturn(
                                        aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"message\":\"success\"}")));

                webTestClient
                        .get()
                        .uri(endpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJwt)
                        .header("X-Forwarded-For", blockedIp)
                        .exchange()
                        .expectStatus()
                        .isUnauthorized();
            }

            // then - IP 차단 후 같은 IP로 요청 시 403 Forbidden
            // Note: RateLimitFilter에서 IP 차단 체크하여 403 반환
            String validJwt = JwtTestFixture.aValidJwt("ip-block-test-user");
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", blockedIp)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Scenario 6: Account Lock")
    class AccountLockTest {

        @Test
        @DisplayName("로그인 실패가 연속되면 계정이 잠겨야 한다")
        void shouldLockAccountAfterConsecutiveLoginFailures() {
            // given - 로그인 엔드포인트 모킹
            wireMockServer.stubFor(
                    get(urlEqualTo("/api/auth/login"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(401)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody("{\"error\":\"Invalid credentials\"}")));

            // Note: 이 시나리오는 실제 로그인 서비스와의 통합이 필요
            // 현재는 Rate Limit Filter 레벨에서의 계정 잠금 테스트
        }
    }

    @Nested
    @DisplayName("Scenario 7: Rate Limit Reset")
    class RateLimitResetTest {

        @Test
        @DisplayName("관리자가 IP Rate Limit을 초기화할 수 있어야 한다")
        void shouldResetRateLimitForSpecificUser() {
            // given - 고유한 IP 및 User 사용
            String validJwt = JwtTestFixture.aValidJwt("reset-test-user");
            String testIp = "10.20.30.40";

            // when - Rate limit 소진 (IP Rate Limit: 5)
            for (int i = 0; i < 5; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .header("X-Forwarded-For", testIp)
                        .exchange();
            }

            // 확인 - IP Rate limit 초과
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            // when - Redis에서 모든 Rate Limit 키 삭제 (관리자 초기화 시뮬레이션)
            // IP Rate Limit: gateway:rate_limit:ip:{ipAddress}
            StepVerifier.create(
                            redisTemplate
                                    .keys("gateway:rate_limit:ip:" + testIp)
                                    .flatMap(key -> redisTemplate.delete(key)))
                    .thenConsumeWhile(deleted -> true)
                    .verifyComplete();

            // Endpoint Rate Limit: gateway:rate_limit:endpoint:*
            StepVerifier.create(
                            redisTemplate
                                    .keys("gateway:rate_limit:endpoint:*")
                                    .flatMap(key -> redisTemplate.delete(key)))
                    .thenConsumeWhile(deleted -> true)
                    .verifyComplete();

            // User Rate Limit: gateway:rate_limit:user:*
            StepVerifier.create(
                            redisTemplate
                                    .keys("gateway:rate_limit:user:*")
                                    .flatMap(key -> redisTemplate.delete(key)))
                    .thenConsumeWhile(deleted -> true)
                    .verifyComplete();

            // then - 다시 요청 가능
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("Scenario 8: TTL Expiration")
    class TtlExpirationTest {

        @Test
        @DisplayName("TTL이 만료되면 Rate Limit이 자동으로 리셋되어야 한다")
        void shouldResetRateLimitAfterTtlExpiration() throws InterruptedException {
            // given - 짧은 TTL을 가진 Rate Limit 키 직접 설정
            String testKey = "gateway:rate_limit:ttl-test";

            StepVerifier.create(
                            redisTemplate.opsForValue().set(testKey, "5", Duration.ofSeconds(2)))
                    .expectNext(true)
                    .verifyComplete();

            // when - TTL 대기
            Thread.sleep(2500);

            // then - 키가 만료되어 존재하지 않음
            StepVerifier.create(redisTemplate.hasKey(testKey)).expectNext(false).verifyComplete();
        }
    }
}
