package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
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
 * AccountLockQueryAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockQueryAdapter 단위 테스트")
class AccountLockQueryAdapterTest {

    @Mock
    private AccountLockRedisRepository accountLockRedisRepository;

    private AccountLockQueryAdapter accountLockQueryAdapter;

    @BeforeEach
    void setUp() {
        accountLockQueryAdapter = new AccountLockQueryAdapter(accountLockRedisRepository);
    }

    @Nested
    @DisplayName("isLocked 메서드")
    class IsLockedTest {

        @Test
        @DisplayName("잠금된 계정은 true를 반환해야 한다")
        void shouldReturnTrueWhenAccountIsLocked() {
            // given
            String userId = "user-123";

            given(accountLockRedisRepository.isLocked(eq(userId)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = accountLockQueryAdapter.isLocked(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();

            then(accountLockRedisRepository).should().isLocked(userId);
        }

        @Test
        @DisplayName("잠금되지 않은 계정은 false를 반환해야 한다")
        void shouldReturnFalseWhenAccountIsNotLocked() {
            // given
            String userId = "user-456";

            given(accountLockRedisRepository.isLocked(eq(userId)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = accountLockQueryAdapter.isLocked(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 사용자는 false를 반환해야 한다")
        void shouldReturnFalseForNonExistentUser() {
            // given
            String userId = "non-existent-user";

            given(accountLockRedisRepository.isLocked(eq(userId)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = accountLockQueryAdapter.isLocked(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String userId = "user-error";

            given(accountLockRedisRepository.isLocked(eq(userId)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = accountLockQueryAdapter.isLocked(userId);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("getLockTtlSeconds 메서드")
    class GetLockTtlSecondsTest {

        @Test
        @DisplayName("잠금 남은 시간을 초 단위로 반환해야 한다")
        void shouldReturnLockTtlInSeconds() {
            // given
            String userId = "user-123";

            given(accountLockRedisRepository.getLockTtl(eq(userId)))
                    .willReturn(Mono.just(1800L)); // 30분

            // when
            Mono<Long> result = accountLockQueryAdapter.getLockTtlSeconds(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(1800L)
                    .verifyComplete();

            then(accountLockRedisRepository).should().getLockTtl(userId);
        }

        @Test
        @DisplayName("잠금되지 않은 계정은 -2를 반환해야 한다")
        void shouldReturnNegativeForNotLockedAccount() {
            // given
            String userId = "user-not-locked";

            given(accountLockRedisRepository.getLockTtl(eq(userId)))
                    .willReturn(Mono.just(-2L)); // 키 없음

            // when
            Mono<Long> result = accountLockQueryAdapter.getLockTtlSeconds(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(-2L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("TTL이 없는 잠금은 -1을 반환해야 한다")
        void shouldReturnNegativeOneForNoTtl() {
            // given
            String userId = "user-permanent-lock";

            given(accountLockRedisRepository.getLockTtl(eq(userId)))
                    .willReturn(Mono.just(-1L)); // TTL 없음 (영구 잠금)

            // when
            Mono<Long> result = accountLockQueryAdapter.getLockTtlSeconds(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(-1L)
                    .verifyComplete();
        }

        @Test
        @DisplayName("긴 잠금 시간도 정확히 반환해야 한다")
        void shouldReturnLongLockTtl() {
            // given
            String userId = "user-long-lock";
            long longTtl = 604800L; // 7일

            given(accountLockRedisRepository.getLockTtl(eq(userId)))
                    .willReturn(Mono.just(longTtl));

            // when
            Mono<Long> result = accountLockQueryAdapter.getLockTtlSeconds(userId);

            // then
            StepVerifier.create(result)
                    .expectNext(longTtl)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String userId = "user-error";

            given(accountLockRedisRepository.getLockTtl(eq(userId)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Long> result = accountLockQueryAdapter.getLockTtlSeconds(userId);

            // then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}
