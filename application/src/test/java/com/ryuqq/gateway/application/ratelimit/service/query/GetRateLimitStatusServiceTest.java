package com.ryuqq.gateway.application.ratelimit.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.ratelimit.manager.RateLimitCounterQueryManager;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
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
 * GetRateLimitStatusService 단위 테스트
 *
 * <p>Service → Manager 위임 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetRateLimitStatusService 단위 테스트")
class GetRateLimitStatusServiceTest {

    @Mock private RateLimitCounterQueryManager rateLimitCounterQueryManager;

    @InjectMocks private GetRateLimitStatusService getRateLimitStatusService;

    @Nested
    @DisplayName("execute 메서드")
    class Execute {

        @Test
        @DisplayName("Rate Limit 상태 조회 - 정상 상태")
        void shouldReturnRateLimitStatus() {
            // given
            given(rateLimitCounterQueryManager.getCurrentCount(any())).willReturn(Mono.just(50L));
            given(rateLimitCounterQueryManager.getTtlSeconds(any())).willReturn(Mono.just(30L));

            // when & then
            StepVerifier.create(getRateLimitStatusService.execute(LimitType.IP, "192.168.1.1"))
                    .assertNext(
                            response -> {
                                assertThat(response.limitType()).isEqualTo(LimitType.IP);
                                assertThat(response.identifier()).isEqualTo("192.168.1.1");
                                assertThat(response.currentCount()).isEqualTo(50);
                                assertThat(response.limit()).isEqualTo(100); // IP default
                                assertThat(response.remaining()).isEqualTo(50);
                                assertThat(response.ttlSeconds()).isEqualTo(30);
                                assertThat(response.blocked()).isFalse();
                            })
                    .verifyComplete();

            then(rateLimitCounterQueryManager).should().getCurrentCount(any());
            then(rateLimitCounterQueryManager).should().getTtlSeconds(any());
        }

        @Test
        @DisplayName("Rate Limit 상태 조회 - 차단된 상태")
        void shouldReturnBlockedStatus() {
            // given
            given(rateLimitCounterQueryManager.getCurrentCount(any())).willReturn(Mono.just(150L));
            given(rateLimitCounterQueryManager.getTtlSeconds(any())).willReturn(Mono.just(45L));

            // when & then
            StepVerifier.create(getRateLimitStatusService.execute(LimitType.IP, "192.168.1.1"))
                    .assertNext(
                            response -> {
                                assertThat(response.currentCount()).isEqualTo(150);
                                assertThat(response.remaining()).isEqualTo(0);
                                assertThat(response.blocked()).isTrue();
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rate Limit 상태 조회 - 존재하지 않는 키")
        void shouldReturnNotFoundStatus() {
            // given
            given(rateLimitCounterQueryManager.getCurrentCount(any())).willReturn(Mono.empty());
            given(rateLimitCounterQueryManager.getTtlSeconds(any())).willReturn(Mono.empty());

            // when & then
            StepVerifier.create(getRateLimitStatusService.execute(LimitType.IP, "192.168.1.1"))
                    .assertNext(
                            response -> {
                                assertThat(response.currentCount()).isEqualTo(0);
                                assertThat(response.remaining()).isEqualTo(100);
                                assertThat(response.ttlSeconds()).isEqualTo(0);
                                assertThat(response.blocked()).isFalse();
                            })
                    .verifyComplete();
        }
    }
}
