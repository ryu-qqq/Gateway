package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import java.time.Duration;
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
@DisplayName("RecordFailureService 단위 테스트")
class RecordFailureServiceTest {

    @Mock private RateLimitCounterCommandPort rateLimitCounterCommandPort;

    @Mock private IpBlockCommandPort ipBlockCommandPort;

    private RecordFailureService recordFailureService;

    @BeforeEach
    void setUp() {
        recordFailureService =
                new RecordFailureService(rateLimitCounterCommandPort, ipBlockCommandPort);
    }

    @Nested
    @DisplayName("execute 메서드")
    class Execute {

        @Test
        @DisplayName("로그인 실패 기록 - 임계값 미만")
        void shouldRecordLoginFailureBelowThreshold() {
            // given
            RecordFailureCommand command = RecordFailureCommand.forLoginFailure("192.168.1.1");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(3L));

            // when & then
            StepVerifier.create(recordFailureService.execute(command)).verifyComplete();

            verify(ipBlockCommandPort, never()).block(any(), any());
        }

        @Test
        @DisplayName("로그인 실패 기록 - 임계값 초과 시 IP 차단")
        void shouldBlockIpWhenLoginFailureExceedsThreshold() {
            // given
            RecordFailureCommand command = RecordFailureCommand.forLoginFailure("192.168.1.1");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(6L)); // 5 초과
            when(ipBlockCommandPort.block(eq("192.168.1.1"), any())).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(recordFailureService.execute(command)).verifyComplete();

            verify(ipBlockCommandPort).block(eq("192.168.1.1"), eq(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("Invalid JWT 기록 - 임계값 미만")
        void shouldRecordInvalidJwtBelowThreshold() {
            // given
            RecordFailureCommand command = RecordFailureCommand.forInvalidJwt("192.168.1.1");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(8L));

            // when & then
            StepVerifier.create(recordFailureService.execute(command)).verifyComplete();

            verify(ipBlockCommandPort, never()).block(any(), any());
        }

        @Test
        @DisplayName("Invalid JWT 기록 - 임계값 초과 시 IP 차단")
        void shouldBlockIpWhenInvalidJwtExceedsThreshold() {
            // given
            RecordFailureCommand command = RecordFailureCommand.forInvalidJwt("192.168.1.1");
            when(rateLimitCounterCommandPort.incrementAndGet(any(), any()))
                    .thenReturn(Mono.just(11L)); // 10 초과
            when(ipBlockCommandPort.block(eq("192.168.1.1"), any())).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(recordFailureService.execute(command)).verifyComplete();

            verify(ipBlockCommandPort).block(eq("192.168.1.1"), eq(Duration.ofMinutes(30)));
        }
    }
}
