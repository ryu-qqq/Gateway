package com.ryuqq.gateway.application.ratelimit.service.command;

import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.internal.FailureRecordCoordinator;
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
 * RecordFailureService 단위 테스트
 *
 * <p>Service → Coordinator 위임 테스트
 *
 * <p>상세한 실패 기록 로직 테스트는 FailureRecordCoordinatorTest 참조
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordFailureService 단위 테스트")
class RecordFailureServiceTest {

    @Mock private FailureRecordCoordinator failureRecordCoordinator;

    @InjectMocks private RecordFailureService recordFailureService;

    @Nested
    @DisplayName("Coordinator 위임 테스트")
    class CoordinatorDelegation {

        @Test
        @DisplayName("execute 호출 시 Coordinator.record로 위임")
        void delegateToCoordinator() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(failureRecordCoordinator.record(command)).willReturn(Mono.empty());

            // when
            Mono<Void> result = recordFailureService.execute(command);

            // then
            StepVerifier.create(result).verifyComplete();

            then(failureRecordCoordinator).should().record(command);
        }

        @Test
        @DisplayName("Coordinator 에러 발생 시 에러 전파")
        void propagateCoordinatorError() {
            // given
            RecordFailureCommand command = RateLimitFixture.aRecordFailureCommandForLogin();

            given(failureRecordCoordinator.record(command))
                    .willReturn(Mono.error(new RuntimeException("Record failed")));

            // when
            Mono<Void> result = recordFailureService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Record failed").verify();
        }
    }
}
