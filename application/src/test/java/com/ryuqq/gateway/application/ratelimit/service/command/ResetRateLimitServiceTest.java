package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.dto.command.ResetRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.internal.RateLimitResetCoordinator;
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
 * ResetRateLimitService 단위 테스트
 *
 * <p>Service → Coordinator 위임 테스트
 *
 * <p>상세한 Rate Limit 리셋 로직 테스트는 RateLimitResetCoordinatorTest 참조
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResetRateLimitService 단위 테스트")
class ResetRateLimitServiceTest {

    @Mock private RateLimitResetCoordinator rateLimitResetCoordinator;

    @InjectMocks private ResetRateLimitService resetRateLimitService;

    @Nested
    @DisplayName("Coordinator 위임 테스트")
    class CoordinatorDelegation {

        @Test
        @DisplayName("execute 호출 시 Coordinator.reset으로 위임")
        void delegateToCoordinator() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitResetCoordinator.reset(command)).willReturn(Mono.empty());

            // when
            Mono<Void> result = resetRateLimitService.execute(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(rateLimitResetCoordinator).should().reset(command);
        }

        @Test
        @DisplayName("Coordinator 에러 발생 시 에러 전파")
        void propagateCoordinatorError() {
            // given
            ResetRateLimitCommand command = RateLimitFixture.aResetRateLimitCommandForIp();

            given(rateLimitResetCoordinator.reset(command))
                    .willReturn(Mono.error(new RuntimeException("Reset failed")));

            // when
            Mono<Void> result = resetRateLimitService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Reset failed").verify();
        }
    }
}
