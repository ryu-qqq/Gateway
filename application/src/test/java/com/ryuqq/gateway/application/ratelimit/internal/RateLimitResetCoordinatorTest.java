package com.ryuqq.gateway.application.ratelimit.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.manager.AccountLockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.fixture.ratelimit.RateLimitFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * RateLimitResetCoordinator 단위 테스트
 *
 * <p>Rate Limit 리셋 로직 상세 테스트
 *
 * <ul>
 *   <li>Rate Limit 카운터 삭제
 *   <li>IP 차단 해제 (IP 기반인 경우)
 *   <li>계정 잠금 해제 (User 기반인 경우)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitResetCoordinator 단위 테스트")
class RateLimitResetCoordinatorTest {

    @Mock private RateLimitCounterCommandManager rateLimitCounterCommandManager;

    @Mock private IpBlockCommandManager ipBlockCommandManager;

    @Mock private AccountLockCommandManager accountLockCommandManager;

    @InjectMocks private RateLimitResetCoordinator rateLimitResetCoordinator;

    @Nested
    @DisplayName("IP 기반 타입 리셋")
    class IpBasedTypeReset {

        @Test
        @DisplayName("IP 타입 리셋 - 카운터 삭제 및 IP 차단 해제")
        void resetIpTypeAndUnblockIp() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(ipBlockCommandManager.unblock(command.identifier())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitCounterCommandManager).should().delete(any());
            then(ipBlockCommandManager).should().unblock(command.identifier());
            then(accountLockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("LOGIN 타입 리셋 - 카운터 삭제 및 IP 차단 해제")
        void resetLoginTypeAndUnblockIp() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForLogin();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(ipBlockCommandManager.unblock(command.identifier())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager).should().unblock(command.identifier());
            then(accountLockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("INVALID_JWT 타입 리셋 - 카운터 삭제 및 IP 차단 해제")
        void resetInvalidJwtTypeAndUnblockIp() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(
                            LimitType.INVALID_JWT,
                            RateLimitFixture.defaultIp(),
                            RateLimitFixture.defaultAdminId());

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(ipBlockCommandManager.unblock(command.identifier())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            // INVALID_JWT.isIpBased() == true
            then(ipBlockCommandManager).should().unblock(command.identifier());
            then(accountLockCommandManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("User 기반 타입 리셋")
    class UserBasedTypeReset {

        @Test
        @DisplayName("USER 타입 리셋 - 카운터 삭제 및 계정 잠금 해제")
        void resetUserTypeAndUnlockAccount() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForUser();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(accountLockCommandManager.unlock(command.identifier()))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitCounterCommandManager).should().delete(any());
            then(accountLockCommandManager).should().unlock(command.identifier());
            then(ipBlockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TOKEN_REFRESH 타입 리셋 - 카운터 삭제 및 계정 잠금 해제")
        void resetTokenRefreshTypeAndUnlockAccount() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(
                            LimitType.TOKEN_REFRESH,
                            RateLimitFixture.defaultUserId(),
                            RateLimitFixture.defaultAdminId());

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(accountLockCommandManager.unlock(command.identifier()))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            // TOKEN_REFRESH.isUserBased() == true
            then(accountLockCommandManager).should().unlock(command.identifier());
            then(ipBlockCommandManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("일반 타입 리셋")
    class GeneralTypeReset {

        @Test
        @DisplayName("ENDPOINT 타입 리셋 - 카운터만 삭제")
        void resetEndpointTypeWithoutUnblock() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(
                            LimitType.ENDPOINT, "/api/v1/users", RateLimitFixture.defaultAdminId());

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitCounterCommandManager).should().delete(any());
            // ENDPOINT.isIpBased() == false && ENDPOINT.isUserBased() == false
            then(ipBlockCommandManager).shouldHaveNoInteractions();
            then(accountLockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("OTP 타입 리셋 - 카운터만 삭제")
        void resetOtpTypeWithoutUnblock() {
            // given
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(
                            LimitType.OTP, "phone-123", RateLimitFixture.defaultAdminId());

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            // OTP.isIpBased() == false && OTP.isUserBased() == false
            then(ipBlockCommandManager).shouldHaveNoInteractions();
            then(accountLockCommandManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("DDD: LimitType 동작 검증")
    class LimitTypeDddBehavior {

        @Test
        @DisplayName("isIpBased() == true인 타입은 IP 차단 해제")
        void unblockIpForIpBasedTypes() {
            // IP, LOGIN, INVALID_JWT는 isIpBased() == true
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(ipBlockCommandManager.unblock(command.identifier())).willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager).should().unblock(command.identifier());
        }

        @Test
        @DisplayName("isUserBased() == true인 타입은 계정 잠금 해제")
        void unlockAccountForUserBasedTypes() {
            // USER, TOKEN_REFRESH는 isUserBased() == true
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForUser();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(accountLockCommandManager.unlock(command.identifier()))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(accountLockCommandManager).should().unlock(command.identifier());
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("카운터 삭제 실패 시 에러 전파")
        void propagateErrorWhenCounterDeleteFails() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitCounterCommandManager.delete(any()))
                    .willReturn(Mono.error(new RuntimeException("Counter delete failed")));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Counter delete failed").verify();
        }

        @Test
        @DisplayName("IP 차단 해제 실패 시 에러 전파")
        void propagateErrorWhenIpUnblockFails() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(ipBlockCommandManager.unblock(command.identifier()))
                    .willReturn(Mono.error(new RuntimeException("IP unblock failed")));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).expectErrorMessage("IP unblock failed").verify();
        }

        @Test
        @DisplayName("계정 잠금 해제 실패 시 에러 전파")
        void propagateErrorWhenAccountUnlockFails() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForUser();

            given(rateLimitCounterCommandManager.delete(any())).willReturn(Mono.just(true));
            given(accountLockCommandManager.unlock(command.identifier()))
                    .willReturn(Mono.error(new RuntimeException("Account unlock failed")));

            // when
            Mono<Void> result = rateLimitResetCoordinator.reset(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Account unlock failed").verify();
        }
    }
}
