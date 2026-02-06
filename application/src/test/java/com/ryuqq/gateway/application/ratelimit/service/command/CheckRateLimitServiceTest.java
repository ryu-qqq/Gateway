package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.internal.RateLimitCheckCoordinator;
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
 * CheckRateLimitService 단위 테스트
 *
 * <p>Service → Coordinator 위임 테스트
 *
 * <p>상세한 Rate Limit 체크 로직 테스트는 RateLimitCheckCoordinatorTest 참조
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckRateLimitService 단위 테스트")
class CheckRateLimitServiceTest {

    @Mock private RateLimitCheckCoordinator rateLimitCheckCoordinator;

    @InjectMocks private CheckRateLimitService checkRateLimitService;

    @Nested
    @DisplayName("Coordinator 위임 테스트")
    class CoordinatorDelegation {

        @Test
        @DisplayName("execute 호출 시 Coordinator.check로 위임")
        void delegateToCoordinator() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();
            CheckRateLimitResponse expectedResponse = CheckRateLimitResponse.allowed(50L, 100);

            given(rateLimitCheckCoordinator.check(command)).willReturn(Mono.just(expectedResponse));

            // when
            Mono<CheckRateLimitResponse> result = checkRateLimitService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.allowed()).isTrue();
                                assertThat(response.currentCount()).isEqualTo(50);
                                assertThat(response.limit()).isEqualTo(100);
                            })
                    .verifyComplete();

            then(rateLimitCheckCoordinator).should().check(command);
        }

        @Test
        @DisplayName("Coordinator 에러 발생 시 에러 전파")
        void propagateCoordinatorError() {
            // given
            CheckRateLimitCommand command = RateLimitFixture.aCheckRateLimitCommandForIp();

            given(rateLimitCheckCoordinator.check(command))
                    .willReturn(Mono.error(new RuntimeException("Rate limit check failed")));

            // when
            Mono<CheckRateLimitResponse> result = checkRateLimitService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Rate limit check failed").verify();
        }
    }
}
