package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.query.IpBlockQueryPort;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckRateLimitService 단위 테스트")
class CheckRateLimitServiceTest {

    @Mock private RateLimitCounterCommandPort rateLimitCounterCommandPort;

    @Mock private IpBlockQueryPort ipBlockQueryPort;

    private RateLimitProperties rateLimitProperties;

    private CheckRateLimitService checkRateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        checkRateLimitService =
                new CheckRateLimitService(
                        rateLimitCounterCommandPort, ipBlockQueryPort, rateLimitProperties);
    }

    @Nested
    @DisplayName("execute 메서드")
    class Execute {

        @Test
        @DisplayName("Rate Limit 미초과 시 허용 Response 반환")
        void shouldReturnAllowedResponseWhenNotExceeded() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forIp("192.168.1.1");
            when(ipBlockQueryPort.isBlocked("192.168.1.1")).thenReturn(Mono.just(false));
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(50L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(50);
                                assertThat(response.limit()).isEqualTo(100); // IP default
                                assertThat(response.remaining()).isEqualTo(50);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rate Limit 초과 시 거부 Response 반환")
        void shouldReturnDeniedResponseWhenExceeded() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forIp("192.168.1.1");
            when(ipBlockQueryPort.isBlocked("192.168.1.1")).thenReturn(Mono.just(false));
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(101L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isFalse();
                                assertThat(response.currentCount()).isEqualTo(101);
                                assertThat(response.remaining()).isEqualTo(0);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("IP 차단된 경우 IpBlockedException 발생")
        void shouldThrowIpBlockedExceptionWhenIpIsBlocked() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forIp("192.168.1.1");
            when(ipBlockQueryPort.isBlocked("192.168.1.1")).thenReturn(Mono.just(true));
            when(ipBlockQueryPort.getBlockTtlSeconds("192.168.1.1")).thenReturn(Mono.just(1500L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .expectErrorMatches(
                            error -> {
                                assertThat(error).isInstanceOf(IpBlockedException.class);
                                IpBlockedException ex = (IpBlockedException) error;
                                assertThat(ex.ipAddress()).isEqualTo("192.168.1.1");
                                assertThat(ex.retryAfterSeconds()).isEqualTo(1500);
                                return true;
                            })
                    .verify();
        }

        @Test
        @DisplayName("USER 타입은 IP 차단 체크 건너뛰기")
        void shouldSkipIpBlockCheckForUserType() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forUser("user-123");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(10L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(10);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("ENDPOINT 타입 Rate Limit 체크")
        void shouldCheckRateLimitForEndpointType() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forEndpoint("/api/users", "POST");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(500L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(500);
                                assertThat(response.limit()).isEqualTo(1000); // ENDPOINT default
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("LOGIN 타입 Rate Limit 체크")
        void shouldCheckRateLimitForLoginType() {
            // given
            CheckRateLimitCommand command = CheckRateLimitCommand.forLogin("192.168.1.1");
            when(ipBlockQueryPort.isBlocked("192.168.1.1")).thenReturn(Mono.just(false));
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(3L));

            // when & then
            StepVerifier.create(checkRateLimitService.execute(command))
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(3);
                                assertThat(response.limit()).isEqualTo(5); // LOGIN default
                            })
                    .verifyComplete();
        }
    }
}
