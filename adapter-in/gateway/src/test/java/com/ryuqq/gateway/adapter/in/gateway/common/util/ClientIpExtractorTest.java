package com.ryuqq.gateway.adapter.in.gateway.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * ClientIpExtractor 테스트
 *
 * <p>X-Forwarded-For spoofing 방어를 위한 IP 추출 로직 검증
 *
 * @author development-team
 * @since 1.0.0
 */
class ClientIpExtractorTest {

    @Nested
    @DisplayName("Direct Mode (기본 - 안전)")
    class DirectModeTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있어도 RemoteAddress만 사용해야 한다")
        void shouldIgnoreXForwardedForHeader() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                            .remoteAddress(new InetSocketAddress("192.168.1.100", 12345))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extract(exchange);

            // then
            assertThat(clientIp).isEqualTo("192.168.1.100");
        }

        @Test
        @DisplayName("RemoteAddress가 있으면 해당 IP를 반환해야 한다")
        void shouldReturnRemoteAddressIp() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .remoteAddress(new InetSocketAddress("10.20.30.40", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extract(exchange);

            // then
            assertThat(clientIp).isEqualTo("10.20.30.40");
        }

        @Test
        @DisplayName("RemoteAddress가 없으면 'unknown'을 반환해야 한다")
        void shouldReturnUnknownWhenRemoteAddressIsNull() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extract(exchange);

            // then
            assertThat(clientIp).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("Trusted Proxy Mode (신뢰할 수 있는 프록시 환경)")
    class TrustedProxyModeTest {

        @Test
        @DisplayName("유효한 X-Forwarded-For가 있으면 첫 번째 IP를 반환해야 한다")
        void shouldReturnFirstIpFromXForwardedFor() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "203.0.113.50, 10.0.0.1, 10.0.0.2")
                            .remoteAddress(new InetSocketAddress("192.168.1.1", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then
            assertThat(clientIp).isEqualTo("203.0.113.50");
        }

        @Test
        @DisplayName("단일 IP X-Forwarded-For도 정상 처리해야 한다")
        void shouldHandleSingleIpInXForwardedFor() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "198.51.100.178")
                            .remoteAddress(new InetSocketAddress("10.0.0.1", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then
            assertThat(clientIp).isEqualTo("198.51.100.178");
        }

        @Test
        @DisplayName("유효하지 않은 IP 형식이면 RemoteAddress로 폴백해야 한다")
        void shouldFallbackToRemoteAddressForInvalidIpFormat() {
            // given - 악의적인 spoofing 시도: 유효하지 않은 IP 형식
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "malicious-string")
                            .remoteAddress(new InetSocketAddress("172.16.0.1", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then
            assertThat(clientIp).isEqualTo("172.16.0.1");
        }

        @Test
        @DisplayName("빈 X-Forwarded-For 헤더면 RemoteAddress로 폴백해야 한다")
        void shouldFallbackToRemoteAddressForEmptyHeader() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "   ")
                            .remoteAddress(new InetSocketAddress("172.16.0.2", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then
            assertThat(clientIp).isEqualTo("172.16.0.2");
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 없으면 RemoteAddress를 사용해야 한다")
        void shouldUseRemoteAddressWhenHeaderMissing() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .remoteAddress(new InetSocketAddress("10.10.10.10", 9000))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then
            assertThat(clientIp).isEqualTo("10.10.10.10");
        }
    }

    @Nested
    @DisplayName("IP Spoofing 방어")
    class SpoofingDefenseTest {

        @Test
        @DisplayName("스크립트 삽입 시도는 거부되어야 한다")
        void shouldRejectScriptInjectionAttempt() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "<script>alert('xss')</script>")
                            .remoteAddress(new InetSocketAddress("192.168.0.1", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then - RemoteAddress로 폴백
            assertThat(clientIp).isEqualTo("192.168.0.1");
        }

        @Test
        @DisplayName("SQL 삽입 시도는 거부되어야 한다")
        void shouldRejectSqlInjectionAttempt() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "'; DROP TABLE users; --")
                            .remoteAddress(new InetSocketAddress("192.168.0.2", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then - RemoteAddress로 폴백
            assertThat(clientIp).isEqualTo("192.168.0.2");
        }

        @Test
        @DisplayName("범위 외 IP 주소는 거부되어야 한다")
        void shouldRejectOutOfRangeIpAddress() {
            // given - 256 이상의 옥텟은 유효하지 않음
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "999.999.999.999")
                            .remoteAddress(new InetSocketAddress("192.168.0.3", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            String clientIp = ClientIpExtractor.extractWithTrustedProxy(exchange);

            // then - RemoteAddress로 폴백
            assertThat(clientIp).isEqualTo("192.168.0.3");
        }

        @Test
        @DisplayName("Direct Mode는 X-Forwarded-For spoofing에 면역이어야 한다")
        void directModeShouldBeImmuneToSpoofing() {
            // given - 악의적인 클라이언트가 X-Forwarded-For를 조작
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "1.2.3.4")
                            .remoteAddress(new InetSocketAddress("actual-client.example.com", 80))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when - Direct Mode 사용
            String clientIp = ClientIpExtractor.extract(exchange);

            // then - X-Forwarded-For가 아닌 실제 연결 주소 반환
            assertThat(clientIp).isNotEqualTo("1.2.3.4");
        }
    }
}
