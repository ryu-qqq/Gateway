package com.ryuqq.gateway.application.ratelimit.internal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.manager.IpBlockQueryManager;
import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterCommandManager;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
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
 * RateLimitCheckCoordinator 단위 테스트
 *
 * <p>Rate Limit 체크 로직 상세 테스트
 *
 * <ul>
 *   <li>IP 차단 여부 선제 확인
 *   <li>카운터 증가 및 결과 확인
 *   <li>허용/거부 Response 생성
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitCheckCoordinator 단위 테스트")
class RateLimitCheckCoordinatorTest {

    @Mock private RateLimitCounterCommandManager rateLimitCounterCommandManager;

    @Mock private IpBlockQueryManager ipBlockQueryManager;

    @Mock private RateLimitProperties rateLimitProperties;

    @InjectMocks private RateLimitCheckCoordinator rateLimitCheckCoordinator;

    @Nested
    @DisplayName("IP 차단 선제 확인")
    class IpBlockCheckFirst {

        @Test
        @DisplayName("IP 차단된 경우 IpBlockedException 발생")
        void throwExceptionWhenIpIsBlocked() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(true));
            given(ipBlockQueryManager.getBlockTtlSeconds(command.identifier()))
                    .willReturn(Mono.just(1800L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(IpBlockedException.class);
                                IpBlockedException exception = (IpBlockedException) error;
                                assertThat(exception.retryAfterSeconds()).isEqualTo(1800);
                            })
                    .verify();

            then(rateLimitCounterCommandManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("IP 차단되지 않은 경우 Rate Limit 체크 진행")
        void proceedToRateLimitCheckWhenIpNotBlocked() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(null);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(50L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(50);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("User 타입은 IP 차단 확인 스킵")
        void skipIpBlockCheckForUserType() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForUser();

            given(rateLimitProperties.getUserLimit()).willReturn(null);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(10L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.allowed()).isTrue())
                    .verifyComplete();

            then(ipBlockQueryManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Login 타입은 IP 차단 확인 실행 (IP 기반)")
        void executeIpBlockCheckForLoginType() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForLogin();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getLoginLimit()).willReturn(null);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(3L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.allowed()).isTrue())
                    .verifyComplete();

            then(ipBlockQueryManager).should().isBlocked(command.identifier());
        }
    }

    @Nested
    @DisplayName("Rate Limit 허용 처리")
    class RateLimitAllowed {

        @Test
        @DisplayName("한도 미만일 때 허용 응답 반환")
        void returnAllowedResponseWhenUnderLimit() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(100);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(50L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(50);
                                assertThat(response.limit()).isEqualTo(100);
                                assertThat(response.action()).isNull();
                                assertThat(response.retryAfterSeconds()).isZero();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("한도 직전 경계값일 때 허용 응답 반환 (count < limit)")
        void returnAllowedResponseJustBelowLimit() {
            // given
            // Note: limit에 도달(count >= limit)하면 초과로 판단되므로
            // 마지막 허용 요청은 count = limit - 1
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(100);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(99L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(99);
                                assertThat(response.limit()).isEqualTo(100);
                                assertThat(response.remaining()).isEqualTo(1);
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Rate Limit 초과 처리")
    class RateLimitExceeded {

        @Test
        @DisplayName("한도 초과 시 REJECT 액션의 denied 응답 반환")
        void returnDeniedResponseWithRejectAction() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(100);
            given(rateLimitProperties.getWindowSeconds()).willReturn(60);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(101L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isFalse();
                                assertThat(response.currentCount()).isEqualTo(101);
                                assertThat(response.limit()).isEqualTo(100);
                                assertThat(response.action()).isEqualTo(RateLimitAction.REJECT);
                                assertThat(response.retryAfterSeconds()).isEqualTo(60);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Login 한도 초과 시 RateLimitExceededException 발생 (BLOCK_IP 액션)")
        void throwExceptionWhenLoginLimitExceeded() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForLogin();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getLoginLimit()).willReturn(5);
            given(rateLimitProperties.getWindowSeconds()).willReturn(60);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(6L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(RateLimitExceededException.class);
                                RateLimitExceededException exception =
                                        (RateLimitExceededException) error;
                                assertThat(exception.retryAfterSeconds()).isEqualTo(60);
                            })
                    .verify();
        }
    }

    @Nested
    @DisplayName("설정 기반 정책 적용")
    class ConfigBasedPolicy {

        @Test
        @DisplayName("설정된 limit 값 사용")
        void useConfiguredLimitValue() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(200);
            given(rateLimitProperties.getWindowSeconds()).willReturn(120);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(150L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.limit()).isEqualTo(200);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("설정이 없으면 기본값 사용")
        void useDefaultValueWhenNoConfig() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(null);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(50L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.limit()).isEqualTo(100); // IP 기본값
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Endpoint Rate Limit")
    class EndpointRateLimit {

        @Test
        @DisplayName("Endpoint Rate Limit 체크 - 허용")
        void checkEndpointRateLimitAllowed() {
            // given
            CheckRateLimitCommand command =
                    RateLimitFixture.aCheckRateLimitCommandForEndpoint("/api/v1/orders", "GET");

            given(rateLimitProperties.getEndpointLimit()).willReturn(1000);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.just(500L));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.limit()).isEqualTo(1000);
                            })
                    .verifyComplete();

            then(ipBlockQueryManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("IP 차단 조회 실패 시 에러 전파")
        void propagateErrorWhenIpBlockCheckFails() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier()))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Redis connection failed").verify();
        }

        @Test
        @DisplayName("카운터 증가 실패 시 에러 전파")
        void propagateErrorWhenCounterIncrementFails() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(ipBlockQueryManager.isBlocked(command.identifier())).willReturn(Mono.just(false));
            given(rateLimitProperties.getIpLimit()).willReturn(null);
            given(rateLimitProperties.getWindowSeconds()).willReturn(null);
            given(rateLimitCounterCommandManager.incrementAndGet(any(), any()))
                    .willReturn(Mono.error(new RuntimeException("Counter increment failed")));

            // when
            Mono<CheckRateLimitResponse> result = rateLimitCheckCoordinator.check(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Counter increment failed").verify();
        }
    }
}
