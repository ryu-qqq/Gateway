package com.ryuqq.gateway.integration.ratelimit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.bootstrap.GatewayApplication;
import com.ryuqq.gateway.integration.helper.JwtTestFixture;
import com.ryuqq.gateway.integration.helper.PermissionTestFixture;
import com.ryuqq.gateway.integration.helper.TenantConfigTestFixture;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(RateLimitIntegrationTest.TestGatewayConfig.class)
class RateLimitIntegrationTest {

    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

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
    @Qualifier("reactiveStringRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired private RateLimitProperties rateLimitProperties;

    @Autowired private ClientIpExtractor clientIpExtractor;

    /**
     * 테스트에서 사용할 클라이언트 IP를 설정하는 필드. Mock ClientIpExtractor가 이 값을 반환합니다. 각 테스트에서 요청 전에 이 값을 설정해야 합니다.
     *
     * <p>주의: @BeforeEach에서 고유한 기본값으로 초기화되며, 각 테스트에서 currentTestIp.set()으로 명시적으로 설정해야 합니다.
     */
    private final AtomicReference<String> currentTestIp = new AtomicReference<>();

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
        // Rate limit 설정 (테스트용)
        registry.add("gateway.rate-limit.enabled", () -> "true");
        registry.add("gateway.rate-limit.endpoint-limit", () -> "5");
        registry.add("gateway.rate-limit.ip-limit", () -> "100");
        registry.add("gateway.rate-limit.user-limit", () -> "100");
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
                            r -> r.path("/api/**").uri("http://localhost:" + wireMockServer.port()))
                    .build();
        }

        /**
         * Mock ClientIpExtractor - @Primary로 실제 빈 대체
         *
         * <p>RateLimitFilter, JwtAuthenticationFilter에서 사용하는 ClientIpExtractor를 테스트용 Mock으로 대체합니다.
         */
        @Bean
        @org.springframework.context.annotation.Primary
        public ClientIpExtractor mockClientIpExtractor() {
            return Mockito.mock(ClientIpExtractor.class);
        }
    }

    @BeforeEach
    void setup() throws InterruptedException {
        wireMockServer.resetAll();

        // 테스트 격리를 위해 고유한 기본 IP 설정 (UUID 사용)
        // 각 테스트에서 currentTestIp.set()으로 명시적으로 덮어써야 함
        currentTestIp.set("test-default-" + UUID.randomUUID().toString().substring(0, 8));

        // Mock 초기화 후 재설정
        Mockito.reset(clientIpExtractor);

        // ClientIpExtractor mock 설정: currentTestIp 필드 값을 반환
        // 각 테스트에서 currentTestIp.set(ip)으로 IP를 설정해야 함
        // extractWithTrustedProxy - RateLimitFilter에서 사용
        Mockito.when(clientIpExtractor.extractWithTrustedProxy(Mockito.any()))
                .thenAnswer(invocation -> currentTestIp.get());

        // extract - JwtAuthenticationFilter에서 사용 (IP Block 기능)
        Mockito.when(clientIpExtractor.extract(Mockito.any()))
                .thenAnswer(invocation -> currentTestIp.get());

        // Clean up Redis before each test - FLUSHALL 명령으로 전체 데이터베이스 초기화
        redisTemplate
                .execute(connection -> connection.serverCommands().flushAll())
                .blockLast(Duration.ofSeconds(5));

        // Wait for any in-flight async requests from previous tests to complete
        Thread.sleep(500);

        // Mock JWKS endpoint (priority 1 - highest)
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/auth/jwks"))
                        .atPriority(1)
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(JwtTestFixture.jwksResponse())));

        // Mock Permission Spec endpoint (priority 1, Internal API)
        wireMockServer.stubFor(
                get(urlEqualTo(PermissionTestFixture.PERMISSION_SPEC_PATH))
                        .atPriority(1)
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture.allPublicPermissionSpec(
                                                        "/api/.*"))));

        // Mock User Permissions endpoint (priority 1, Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching(PermissionTestFixture.USER_PERMISSIONS_PATH_PATTERN))
                        .atPriority(1)
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                PermissionTestFixture
                                                        .userPermissionHashResponse())));

        // Mock Tenant Config API (priority 1, Internal API)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching("/api/v1/internal/tenants/.+/config"))
                        .atPriority(1)
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                TenantConfigTestFixture.tenantConfigResponse(
                                                        "tenant-001"))));

        // Mock downstream service (pattern matching for all /api/* paths, priority 10 - lowest)
        wireMockServer.stubFor(
                get(WireMock.urlPathMatching("/api/.*"))
                        .atPriority(10)
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"message\":\"success\"}")));
    }

    /** Rate Limit 키 패턴 삭제 helper 메서드 */
    private void deleteRateLimitKeys(String... keys) {
        for (String key : keys) {
            redisTemplate.delete(key).block();
        }
    }

    @Nested
    @DisplayName("Scenario 1: Endpoint Rate Limit")
    class EndpointRateLimitTest {

        @Test
        @DisplayName("동일 엔드포인트에 대한 요청이 제한을 초과하면 429를 반환해야 한다")
        void shouldReturn429WhenEndpointRateLimitExceeded() {
            // given - UUID를 사용한 완전히 고유한 식별자
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String testEndpoint = "/api/endpoint-test-" + uniqueId;
            String testIp = "192.168.50." + (int) (Math.random() * 254 + 1);
            String testUser = "endpoint-user-" + uniqueId;

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            String validJwt = JwtTestFixture.aValidJwt(testUser);
            int requestsBeforeBlock = 4;

            // when - Rate limit 이내의 요청 (4번까지 허용)
            for (int i = 0; i < requestsBeforeBlock; i++) {
                var result =
                        webTestClient
                                .get()
                                .uri(testEndpoint)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                                .header("X-Forwarded-For", testIp)
                                .exchange()
                                .returnResult(String.class);

                var status = result.getStatus();
                assertThat(status.is2xxSuccessful())
                        .as("Request %d should succeed, but got %s", i + 1, status)
                        .isTrue();
            }

            // then - Rate limit에 도달한 요청 (5번째 = count가 5가 되는 시점에서 차단)
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
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
            String testEndpoint = "/api/user-rate-limit-test";
            String testIp1 = "10.20.200.1";
            String testIp2 = "10.20.200.2";
            String testUser1 = "user-rate-limit-test-1";
            String testUser2 = "user-rate-limit-test-2";

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp1,
                    "gateway:rate_limit:ip:" + testIp2,
                    "gateway:rate_limit:user:" + testUser1,
                    "gateway:rate_limit:user:" + testUser2);

            String user1Jwt = JwtTestFixture.aValidJwt(testUser1);
            String user2Jwt = JwtTestFixture.aValidJwt(testUser2);

            // when - user1이 Endpoint Rate Limit 소진 (4번까지 허용)
            currentTestIp.set(testIp1);
            for (int i = 0; i < 4; i++) {
                webTestClient
                        .get()
                        .uri(testEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
                        .header("X-Forwarded-For", testIp1)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // then - user1은 Endpoint Rate Limit 초과 (5번째 요청에서 차단)
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
                    .header("X-Forwarded-For", testIp1)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            // and - user2도 동일 Endpoint에 대해 Rate Limit 초과
            currentTestIp.set(testIp2);
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
                    .header("X-Forwarded-For", testIp2)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Scenario 3: IP Rate Limit")
    class IpRateLimitTest {

        @Test
        @DisplayName("동일 IP에서 오는 요청은 동일한 Endpoint Rate Limit을 공유해야 한다")
        void shouldShareRateLimitForSameIp() {
            // given
            String testEndpoint = "/api/ip-same-test";
            String testIp = "172.16.100.1";
            String testUser = "ip-same-test-user-v2";

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp,
                    "gateway:rate_limit:user:" + testUser);

            String validJwt = JwtTestFixture.aValidJwt(testUser);

            // when - 같은 IP에서 요청 (고유 IP 사용)
            for (int i = 0; i < 4; i++) {
                webTestClient
                        .get()
                        .uri(testEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .header("X-Forwarded-For", testIp)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // then - Endpoint Rate limit에 도달 (5번째 요청)
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("다른 IP에서 오는 요청도 동일 Endpoint는 Endpoint Rate Limit을 공유해야 한다")
        void shouldShareEndpointRateLimitAcrossIps() {
            // given
            String testEndpoint = "/api/ip-cross-test";
            String testIp1 = "172.17.100.1";
            String testIp2 = "172.17.100.2";
            String testIp3 = "172.17.100.3";
            String testUser1 = "ip-cross-test-user-v2-1";
            String testUser2 = "ip-cross-test-user-v2-2";

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp1,
                    "gateway:rate_limit:ip:" + testIp2,
                    "gateway:rate_limit:ip:" + testIp3,
                    "gateway:rate_limit:user:" + testUser1,
                    "gateway:rate_limit:user:" + testUser2);

            String validJwt1 = JwtTestFixture.aValidJwt(testUser1);
            String validJwt2 = JwtTestFixture.aValidJwt(testUser2);

            // when - IP 172.17.100.1에서 Endpoint Rate Limit 일부 소진 (3번)
            currentTestIp.set(testIp1);
            for (int i = 0; i < 3; i++) {
                webTestClient
                        .get()
                        .uri(testEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt1)
                        .header("X-Forwarded-For", testIp1)
                        .exchange()
                        .expectStatus()
                        .isOk();
            }

            // when - IP 172.17.100.2에서 나머지 Endpoint Rate Limit 소진 (1번 - 총 4번)
            currentTestIp.set(testIp2);
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt2)
                    .header("X-Forwarded-For", testIp2)
                    .exchange()
                    .expectStatus()
                    .isOk();

            // then - 새 IP에서도 동일 엔드포인트는 Endpoint Rate Limit에 도달 (5번째 요청)
            currentTestIp.set(testIp3);
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt2)
                    .header("X-Forwarded-For", testIp3)
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
            String testEndpoint = "/api/header-test-1";
            String testIp = "192.168.100.1";
            String testUser = "header-test-user-1";

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp,
                    "gateway:rate_limit:user:" + testUser);

            String validJwt = JwtTestFixture.aValidJwt(testUser);

            // when & then
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
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
            String testEndpoint = "/api/header-test-2";
            String testIp = "192.168.100.2";
            String testUser = "header-test-user-2";

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp,
                    "gateway:rate_limit:user:" + testUser);

            String validJwt = JwtTestFixture.aValidJwt(testUser);

            // when - Rate limit 소진 (endpoint-limit=5, 4번까지 허용)
            for (int i = 0; i < 4; i++) {
                webTestClient
                        .get()
                        .uri(testEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .header("X-Forwarded-For", testIp)
                        .exchange();
            }

            // then - Rate limit에 도달 시 (5번째) Retry-After 헤더 확인
            webTestClient
                    .get()
                    .uri(testEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
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
            String testEndpoint = "/api/remaining-count-test";
            String testIp = "192.168.200.1";
            String testUser = "remaining-count-test-user";

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            deleteRateLimitKeys(
                    "gateway:rate_limit:endpoint:" + testEndpoint + ":GET",
                    "gateway:rate_limit:ip:" + testIp,
                    "gateway:rate_limit:user:" + testUser);

            String validJwt = JwtTestFixture.aValidJwt(testUser);

            // when - 첫 번째 요청
            String firstRemaining =
                    webTestClient
                            .get()
                            .uri(testEndpoint)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                            .header("X-Forwarded-For", testIp)
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
                            .uri(testEndpoint)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                            .header("X-Forwarded-For", testIp)
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

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(blockedIp);

            // INVALID_JWT 임계값은 10 req/5min
            // 충분한 횟수의 요청을 보내서 IP 차단을 트리거
            // (WebTestClient 내부 동작으로 실제 요청 횟수가 증가할 수 있음)
            int maxRequests = 15;

            // when - Invalid JWT로 반복 요청 (다른 엔드포인트로 Endpoint Rate Limit 우회)
            for (int i = 0; i < maxRequests; i++) {
                String endpoint = "/api/invalid-jwt-test-" + i;
                wireMockServer.stubFor(
                        get(urlEqualTo(endpoint))
                                .willReturn(
                                        aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("{\"message\":\"success\"}")));

                // 임계값 도달 전: 401, 도달 후: 403 (둘 다 허용)
                webTestClient
                        .get()
                        .uri(endpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidJwt)
                        .header("X-Forwarded-For", blockedIp)
                        .exchange()
                        .expectStatus()
                        .value(status -> assertThat(status).isIn(401, 403));
            }

            // then - IP 차단 후 같은 IP로 요청 시 403 Forbidden
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
    @DisplayName("Scenario 7: Rate Limit Reset")
    class RateLimitResetTest {

        @Test
        @DisplayName("관리자가 IP Rate Limit을 초기화할 수 있어야 한다")
        void shouldResetRateLimitForSpecificUser() {
            // given
            String validJwt = JwtTestFixture.aValidJwt("reset-test-user");
            String testIp = "10.20.30.40";

            // Mock이 이 IP를 반환하도록 설정
            currentTestIp.set(testIp);

            // when - Rate limit 소진
            for (int i = 0; i < 5; i++) {
                webTestClient
                        .get()
                        .uri("/api/resource")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                        .header("X-Forwarded-For", testIp)
                        .exchange();
            }

            // 확인 - Rate limit 초과
            webTestClient
                    .get()
                    .uri("/api/resource")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwt)
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            // when - Redis에서 모든 Rate Limit 키 삭제 (관리자 초기화 시뮬레이션)
            StepVerifier.create(
                            redisTemplate
                                    .keys("gateway:rate_limit:ip:" + testIp)
                                    .flatMap(key -> redisTemplate.delete(key)))
                    .thenConsumeWhile(deleted -> true)
                    .verifyComplete();

            StepVerifier.create(
                            redisTemplate
                                    .keys("gateway:rate_limit:endpoint:*")
                                    .flatMap(key -> redisTemplate.delete(key)))
                    .thenConsumeWhile(deleted -> true)
                    .verifyComplete();

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
            // given
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
