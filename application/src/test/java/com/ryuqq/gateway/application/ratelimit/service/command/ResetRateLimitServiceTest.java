package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.port.out.command.AccountLockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.IpBlockCommandPort;
import com.ryuqq.gateway.application.ratelimit.port.out.command.RateLimitCounterCommandPort;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
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
@DisplayName("ResetRateLimitService 단위 테스트")
class ResetRateLimitServiceTest {

    @Mock private RateLimitCounterCommandPort rateLimitCounterCommandPort;

    @Mock private IpBlockCommandPort ipBlockCommandPort;

    @Mock private AccountLockCommandPort accountLockCommandPort;

    private ResetRateLimitService resetRateLimitService;

    @BeforeEach
    void setUp() {
        resetRateLimitService =
                new ResetRateLimitService(
                        rateLimitCounterCommandPort, ipBlockCommandPort, accountLockCommandPort);
    }

    @Nested
    @DisplayName("execute 메서드")
    class Execute {

        @Test
        @DisplayName("IP 타입 리셋 - 카운터 삭제 및 IP 차단 해제")
        void shouldResetIpTypeAndUnblockIp() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(LimitType.IP, "192.168.1.1", "admin-001");
            when(rateLimitCounterCommandPort.delete(any())).thenReturn(Mono.just(true));
            when(ipBlockCommandPort.unblock("192.168.1.1")).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(resetRateLimitService.execute(command)).verifyComplete();

            verify(rateLimitCounterCommandPort).delete(any());
            verify(ipBlockCommandPort).unblock("192.168.1.1");
            verify(accountLockCommandPort, never()).unlock(any());
        }

        @Test
        @DisplayName("LOGIN 타입 리셋 - 카운터 삭제 및 IP 차단 해제")
        void shouldResetLoginTypeAndUnblockIp() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(LimitType.LOGIN, "192.168.1.1", "admin-001");
            when(rateLimitCounterCommandPort.delete(any())).thenReturn(Mono.just(true));
            when(ipBlockCommandPort.unblock("192.168.1.1")).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(resetRateLimitService.execute(command)).verifyComplete();

            verify(ipBlockCommandPort).unblock("192.168.1.1");
        }

        @Test
        @DisplayName("USER 타입 리셋 - 카운터 삭제 및 계정 잠금 해제")
        void shouldResetUserTypeAndUnlockAccount() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(LimitType.USER, "user-123", "admin-001");
            when(rateLimitCounterCommandPort.delete(any())).thenReturn(Mono.just(true));
            when(accountLockCommandPort.unlock("user-123")).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(resetRateLimitService.execute(command)).verifyComplete();

            verify(accountLockCommandPort).unlock("user-123");
            verify(ipBlockCommandPort, never()).unblock(any());
        }

        @Test
        @DisplayName("ENDPOINT 타입 리셋 - 카운터만 삭제")
        void shouldResetEndpointTypeWithoutUnblock() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(LimitType.ENDPOINT, "/api/users", "admin-001");
            when(rateLimitCounterCommandPort.delete(any())).thenReturn(Mono.just(true));

            // when & then
            StepVerifier.create(resetRateLimitService.execute(command)).verifyComplete();

            verify(rateLimitCounterCommandPort).delete(any());
            verify(ipBlockCommandPort, never()).unblock(any());
            verify(accountLockCommandPort, never()).unlock(any());
        }
    }
}
