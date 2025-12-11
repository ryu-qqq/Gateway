package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
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
 * AccountLockCommandAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLockCommandAdapter 단위 테스트")
class AccountLockCommandAdapterTest {

    @Mock private AccountLockRedisRepository accountLockRedisRepository;

    private AccountLockCommandAdapter accountLockCommandAdapter;

    @BeforeEach
    void setUp() {
        accountLockCommandAdapter = new AccountLockCommandAdapter(accountLockRedisRepository);
    }

    @Nested
    @DisplayName("lock 메서드")
    class LockTest {

        @Test
        @DisplayName("계정을 잠금하고 true를 반환해야 한다")
        void shouldLockAccountAndReturnTrue() {
            // given
            String userId = "user-123";
            Duration duration = Duration.ofMinutes(30);

            given(accountLockRedisRepository.lock(eq(userId), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.lock(userId, duration);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();

            then(accountLockRedisRepository).should().lock(userId, duration);
        }

        @Test
        @DisplayName("짧은 기간으로 계정을 잠금할 수 있어야 한다")
        void shouldLockWithShortDuration() {
            // given
            String userId = "user-456";
            Duration duration = Duration.ofSeconds(30);

            given(accountLockRedisRepository.lock(eq(userId), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.lock(userId, duration);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();
        }

        @Test
        @DisplayName("긴 기간으로 계정을 잠금할 수 있어야 한다")
        void shouldLockWithLongDuration() {
            // given
            String userId = "user-789";
            Duration duration = Duration.ofDays(7);

            given(accountLockRedisRepository.lock(eq(userId), eq(duration)))
                    .willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.lock(userId, duration);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();
        }

        @Test
        @DisplayName("잠금 실패 시 false를 반환해야 한다")
        void shouldReturnFalseWhenLockFails() {
            // given
            String userId = "user-error";
            Duration duration = Duration.ofMinutes(10);

            given(accountLockRedisRepository.lock(eq(userId), eq(duration)))
                    .willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.lock(userId, duration);

            // then
            StepVerifier.create(result).expectNext(false).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String userId = "user-123";
            Duration duration = Duration.ofMinutes(30);

            given(accountLockRedisRepository.lock(eq(userId), eq(duration)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.lock(userId, duration);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }

    @Nested
    @DisplayName("unlock 메서드")
    class UnlockTest {

        @Test
        @DisplayName("계정 잠금을 해제하고 true를 반환해야 한다")
        void shouldUnlockAccountAndReturnTrue() {
            // given
            String userId = "user-123";

            given(accountLockRedisRepository.unlock(eq(userId))).willReturn(Mono.just(true));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.unlock(userId);

            // then
            StepVerifier.create(result).expectNext(true).verifyComplete();

            then(accountLockRedisRepository).should().unlock(userId);
        }

        @Test
        @DisplayName("잠금되지 않은 계정 해제 시도 시 false를 반환해야 한다")
        void shouldReturnFalseWhenAccountNotLocked() {
            // given
            String userId = "user-not-locked";

            given(accountLockRedisRepository.unlock(eq(userId))).willReturn(Mono.just(false));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.unlock(userId);

            // then
            StepVerifier.create(result).expectNext(false).verifyComplete();
        }

        @Test
        @DisplayName("Redis 에러 발생 시 에러를 전파해야 한다")
        void shouldPropagateRedisError() {
            // given
            String userId = "user-123";

            given(accountLockRedisRepository.unlock(eq(userId)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Boolean> result = accountLockCommandAdapter.unlock(userId);

            // then
            StepVerifier.create(result).expectError(RuntimeException.class).verify();
        }
    }
}
