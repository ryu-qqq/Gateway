package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.RateLimitRedisRepository;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitKey;
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
 * RateLimitCounterQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitCounterQueryAdapter 단위 테스트")
class RateLimitCounterQueryAdapterTest {

    @Mock private RateLimitRedisRepository rateLimitRedisRepository;

    private RateLimitCounterQueryAdapter rateLimitCounterQueryAdapter;

    @BeforeEach
    void setUp() {
        rateLimitCounterQueryAdapter = new RateLimitCounterQueryAdapter(rateLimitRedisRepository);
    }

    @Nested
    @DisplayName("getCurrentCount 메서드")
    class GetCurrentCountTest {

        @Test
        @DisplayName("현재 카운트 값을 반환해야 한다")
        void shouldReturnCurrentCount() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:192.168.1.1");

            given(rateLimitRedisRepository.getCount(eq(key.value()))).willReturn(Mono.just(5L));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getCurrentCount(key);

            // then
            StepVerifier.create(result).expectNext(5L).verifyComplete();

            then(rateLimitRedisRepository).should().getCount(key.value());
        }

        @Test
        @DisplayName("카운터가 없으면 0을 반환해야 한다")
        void shouldReturnZeroWhenCounterNotExists() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:non-existent");

            given(rateLimitRedisRepository.getCount(eq(key.value()))).willReturn(Mono.just(0L));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getCurrentCount(key);

            // then
            StepVerifier.create(result).expectNext(0L).verifyComplete();
        }

        @Test
        @DisplayName("높은 카운트 값도 정확히 반환해야 한다")
        void shouldReturnHighCountValue() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");

            given(rateLimitRedisRepository.getCount(eq(key.value())))
                    .willReturn(Mono.just(1000000L));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getCurrentCount(key);

            // then
            StepVerifier.create(result).expectNext(1000000L).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:10.0.0.1");

            given(rateLimitRedisRepository.getCount(eq(key.value())))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getCurrentCount(key);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }

    @Nested
    @DisplayName("getTtlSeconds 메서드")
    class GetTtlSecondsTest {

        @Test
        @DisplayName("남은 TTL을 초 단위로 반환해야 한다")
        void shouldReturnTtlInSeconds() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:192.168.1.1");

            given(rateLimitRedisRepository.getTtl(eq(key.value()))).willReturn(Mono.just(45L));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getTtlSeconds(key);

            // then
            StepVerifier.create(result).expectNext(45L).verifyComplete();

            then(rateLimitRedisRepository).should().getTtl(key.value());
        }

        @Test
        @DisplayName("TTL이 없는 키에 대해 -1 또는 -2를 반환해야 한다")
        void shouldReturnNegativeForNoTtl() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:no-ttl");

            given(rateLimitRedisRepository.getTtl(eq(key.value())))
                    .willReturn(Mono.just(-1L)); // -1: TTL 없음, -2: 키 없음

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getTtlSeconds(key);

            // then
            StepVerifier.create(result).expectNext(-1L).verifyComplete();
        }

        @Test
        @DisplayName("긴 TTL 값도 정확히 반환해야 한다")
        void shouldReturnLongTtlValue() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:user:user-123");
            long longTtl = 86400L; // 24시간

            given(rateLimitRedisRepository.getTtl(eq(key.value()))).willReturn(Mono.just(longTtl));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getTtlSeconds(key);

            // then
            StepVerifier.create(result).expectNext(longTtl).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            RateLimitKey key = RateLimitKey.of("gateway:rate_limit:ip:10.0.0.1");

            given(rateLimitRedisRepository.getTtl(eq(key.value())))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Long> result = rateLimitCounterQueryAdapter.getTtlSeconds(key);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}
