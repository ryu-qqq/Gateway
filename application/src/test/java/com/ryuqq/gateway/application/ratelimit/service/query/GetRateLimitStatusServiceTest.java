package com.ryuqq.gateway.application.ratelimit.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.application.ratelimit.port.out.query.RateLimitCounterQueryPort;
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
@DisplayName("GetRateLimitStatusService 단위 테스트")
class GetRateLimitStatusServiceTest {

    @Mock private RateLimitCounterQueryPort rateLimitCounterQueryPort;

    private GetRateLimitStatusService getRateLimitStatusService;

    @BeforeEach
    void setUp() {
        getRateLimitStatusService = new GetRateLimitStatusService(rateLimitCounterQueryPort);
    }

    @Nested
    @DisplayName("execute 메서드")
    class Execute {

        @Test
        @DisplayName("Rate Limit 상태 조회 - 정상 상태")
        void shouldReturnRateLimitStatus() {
            // given
            when(rateLimitCounterQueryPort.getCurrentCount(any())).thenReturn(Mono.just(50L));
            when(rateLimitCounterQueryPort.getTtlSeconds(any())).thenReturn(Mono.just(30L));

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
        }

        @Test
        @DisplayName("Rate Limit 상태 조회 - 차단된 상태")
        void shouldReturnBlockedStatus() {
            // given
            when(rateLimitCounterQueryPort.getCurrentCount(any())).thenReturn(Mono.just(150L));
            when(rateLimitCounterQueryPort.getTtlSeconds(any())).thenReturn(Mono.just(45L));

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
            when(rateLimitCounterQueryPort.getCurrentCount(any())).thenReturn(Mono.empty());
            when(rateLimitCounterQueryPort.getTtlSeconds(any())).thenReturn(Mono.empty());

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
