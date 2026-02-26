package com.ryuqq.gateway.application.ratelimit.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockCommandManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.fixture.ratelimit.RateLimitFixture;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
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
 * FailureRecordCoordinator 단위 테스트
 *
 * <p>실패 기록 및 IP 차단 로직 상세 테스트
 *
 * <ul>
 *   <li>실패 카운터 증가
 *   <li>임계값 초과 시 IP 차단 처리
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FailureRecordCoordinator 단위 테스트")
class FailureRecordCoordinatorTest {

    @Mock private RateLimitCounterCommandManager rateLimitCounterCommandManager;

    @Mock private IpBlockCommandManager ipBlockCommandManager;

    @Mock private RateLimitProperties rateLimitProperties;

    @InjectMocks private FailureRecordCoordinator failureRecordCoordinator;

    private static final Duration EXPECTED_BLOCK_DURATION = Duration.ofMinutes(30);

    @BeforeEach
    void setUp() {
        // IP 차단 기능 기본 활성화 (기존 동작 호환)
        // lenient: 에러 전파 테스트 등에서는 flatMap에 도달하지 않아 사용되지 않을 수 있음
        lenient().when(rateLimitProperties.isIpBlockEnabled()).thenReturn(true);
        // Properties 미설정 시 LimitType 기본값 사용 (기존 동작 호환: LOGIN=5회/30분, INVALID_JWT=10회/30분)
        lenient().when(rateLimitProperties.getLoginFailureThreshold()).thenReturn(null);
        lenient().when(rateLimitProperties.getLoginBlockDurationMinutes()).thenReturn(null);
        lenient().when(rateLimitProperties.getInvalidJwtFailureThreshold()).thenReturn(null);
        lenient().when(rateLimitProperties.getInvalidJwtBlockDurationMinutes()).thenReturn(null);
    }

    @Nested
    @DisplayName("실패 카운터 증가")
    class FailureCounterIncrement {

        @Test
        @DisplayName("로그인 실패 시 카운터 증가")
        void incrementCounterOnLoginFailure() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(1L));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitCounterCommandManager).should().incrementAndGet(any(), any());
        }

        @Test
        @DisplayName("Invalid JWT 시 카운터 증가")
        void incrementCounterOnInvalidJwt() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForInvalidJwt();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(1L));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitCounterCommandManager).should().incrementAndGet(any(), any());
        }
    }

    @Nested
    @DisplayName("IP 차단 처리 (Login 타입)")
    class IpBlockForLogin {

        @Test
        @DisplayName("Login 실패 5회 미만 - IP 차단 안함")
        void noIpBlockWhenLoginFailuresUnderThreshold() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(4L)); // 5회 미만

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Login 실패 5회 이상 - IP 30분 차단")
        void blockIpWhenLoginFailuresExceedThreshold() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(5L)); // 5회 이상
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager)
                    .should()
                    .block(command.identifier(), EXPECTED_BLOCK_DURATION);
        }

        @Test
        @DisplayName("Login 실패 연속 초과 - IP 차단 실행")
        void blockIpOnConsecutiveLoginFailures() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(10L)); // 많은 실패
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager)
                    .should()
                    .block(command.identifier(), EXPECTED_BLOCK_DURATION);
        }
    }

    @Nested
    @DisplayName("IP 차단 처리 (Invalid JWT 타입)")
    class IpBlockForInvalidJwt {

        @Test
        @DisplayName("Invalid JWT 10회 미만 - IP 차단 안함")
        void noIpBlockWhenInvalidJwtUnderThreshold() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForInvalidJwt();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(9L)); // 10회 미만

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Invalid JWT 10회 이상 - IP 30분 차단")
        void blockIpWhenInvalidJwtExceedsThreshold() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForInvalidJwt();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(10L)); // 10회 이상
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(ipBlockCommandManager)
                    .should()
                    .block(command.identifier(), EXPECTED_BLOCK_DURATION);
        }
    }

    @Nested
    @DisplayName("DDD: LimitType 동작 검증")
    class LimitTypeDddBehavior {

        @Test
        @DisplayName("LOGIN 타입은 requiresIpBlock() = true")
        void loginTypeRequiresIpBlock() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            // LOGIN 타입의 기본 maxRequests는 5
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(6L)); // 임계값 초과
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            // LOGIN.requiresIpBlock() == true 이므로 IP 차단 실행됨
            then(ipBlockCommandManager).should().block(any(), any());
        }

        @Test
        @DisplayName("INVALID_JWT 타입은 requiresIpBlock() = true")
        void invalidJwtTypeRequiresIpBlock() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForInvalidJwt();

            // INVALID_JWT 타입의 기본 maxRequests는 10
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(11L)); // 임계값 초과
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).verifyComplete();

            // INVALID_JWT.requiresIpBlock() == true 이므로 IP 차단 실행됨
            then(ipBlockCommandManager).should().block(any(), any());
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("카운터 증가 실패 시 에러 전파")
        void propagateErrorWhenCounterIncrementFails() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.error(new RuntimeException("Counter increment failed")));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Counter increment failed").verify();
        }

        @Test
        @DisplayName("IP 차단 실패 시 에러 전파")
        void propagateErrorWhenIpBlockFails() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(5L)); // 임계값 초과
            given(
                            ipBlockCommandManager.block(
                                    eq(command.identifier()), eq(EXPECTED_BLOCK_DURATION)))
                    .willReturn(Mono.error(new RuntimeException("IP block failed")));

            // when
            Mono<Void> result = failureRecordCoordinator.record(command);

            // then
            StepVerifier.create(result).expectErrorMessage("IP block failed").verify();
        }
    }
}
