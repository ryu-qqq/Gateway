package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
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

/**
 * RateLimitCounterCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitCounterCommandAdapter 단위 테스트")
class RateLimitCounterCommandAdapterTest {

    @Mock private RateLimitRedisRepository rateLimitRedisRepository;

    private RateLimitCounterCommandAdapter rateLimitCounterCommandAdapter;

    @BeforeEach
    void setUp() {
        rateLimitCounterCommandAdapter =
                new RateLimitCounterCommandAdapter(rateLimitRedisRepository);
    }

    @Nested
    @DisplayName("incrementAndGet 메서드")
    class IncrementAndGetTest {

        @Test
        @DisplayName("카운터를 증가시키고 증가된 값을 반환해야 한다")
        void shouldIncrementAndReturnNewCount() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:192.168.1.1");
            Duration window = Duration.ofMinutes(1);

            given(rateLimitRedisRepository.incrementAndExpire(eq(key.value()), eq(window)))
                    .willReturn(Mono.just(1L));

            // when
            Mono<Long> result = rateLimitCounterCommandAdapter.incrementAndGet(key, window);

            // then
            StepVerifier.create(result).expectNext(1L).verifyComplete();

            then(rateLimitRedisRepository).should().incrementAndExpire(key.value(), window);
        }

        @Test
        @DisplayName("여러 번 호출 시 카운터가 증가해야 한다")
        void shouldIncrementCounterOnMultipleCalls() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");
            Duration window = Duration.ofHours(1);

            given(rateLimitRedisRepository.incrementAndExpire(eq(key.value()), eq(window)))
                    .willReturn(Mono.just(5L));

            // when
            Mono<Long> result = rateLimitCounterCommandAdapter.incrementAndGet(key, window);

            // then
            StepVerifier.create(result).expectNext(5L).verifyComplete();
        }

        @Test
        @DisplayName("다른 윈도우 크기로 카운터를 증가시킬 수 있어야 한다")
        void shouldSupportDifferentWindowSizes() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:endpoint:/api/users:GET");
            Duration window = Duration.ofSeconds(10);

            given(rateLimitRedisRepository.incrementAndExpire(eq(key.value()), eq(window)))
                    .willReturn(Mono.just(100L));

            // when
            Mono<Long> result = rateLimitCounterCommandAdapter.incrementAndGet(key, window);

            // then
            StepVerifier.create(result).expectNext(100L).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:10.0.0.1");
            Duration window = Duration.ofMinutes(1);

            given(rateLimitRedisRepository.incrementAndExpire(eq(key.value()), eq(window)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Long> result = rateLimitCounterCommandAdapter.incrementAndGet(key, window);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    class DeleteTest {

        @Test
        @DisplayName("카운터를 삭제하고 true를 반환해야 한다")
        void shouldDeleteCounterAndReturnTrue() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:192.168.1.1");

            given(rateLimitRedisRepository.delete(eq(key.value()))).willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = rateLimitCounterCommandAdapter.delete(key);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();

            then(rateLimitRedisRepository).should().delete(key.value());
        }

        @Test
        @DisplayName("존재하지 않는 카운터 삭제 시 false를 반환해야 한다")
        void shouldReturnFalseWhenCounterNotExists() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:non-existent");

            given(rateLimitRedisRepository.delete(eq(key.value()))).willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = rateLimitCounterCommandAdapter.delete(key);

            // then
            StepVerifier.create(result).expectNext(false).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:10.0.0.1");

            given(rateLimitRedisRepository.delete(eq(key.value())))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = rateLimitCounterCommandAdapter.delete(key);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}
