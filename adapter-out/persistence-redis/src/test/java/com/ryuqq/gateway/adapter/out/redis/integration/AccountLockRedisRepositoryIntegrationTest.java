package com.ryuqq.gateway.adapter.out.redis.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.redis.repository.AccountLockRedisRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * AccountLockRedisRepository 통합 테스트
 *
 * <p>TestContainers Redis를 사용하여 실제 계정 잠금 동작을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AccountLockRedisRepository 통합 테스트")
class AccountLockRedisRepositoryIntegrationTest extends RedisTestSupport {

    @Autowired private AccountLockRedisRepository accountLockRedisRepository;

    @Nested
    @DisplayName("lock 메서드")
    class LockTest {

        @Test
        @DisplayName("계정 잠금이 TTL과 함께 저장되어야 한다")
        void shouldLockAccountWithTtl() {
            // given
            String userId = "user-locked-001";
            Duration lockDuration = Duration.ofMinutes(30);

            // when
            StepVerifier.create(accountLockRedisRepository.lock(userId, lockDuration))
                    .assertNext(success -> assertThat(success).isTrue())
                    .verifyComplete();

            // then - 잠금 상태 확인
            StepVerifier.create(accountLockRedisRepository.isLocked(userId))
                    .assertNext(locked -> assertThat(locked).isTrue())
                    .verifyComplete();

            // TTL 확인
            StepVerifier.create(accountLockRedisRepository.getLockTtl(userId))
                    .assertNext(
                            ttl -> {
                                assertThat(ttl).isPositive();
                                assertThat(ttl).isLessThanOrEqualTo(30 * 60L);
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("unlock 메서드")
    class UnlockTest {

        @Test
        @DisplayName("잠긴 계정을 해제하면 true를 반환해야 한다")
        void shouldUnlockLockedAccount() {
            // given
            String userId = "user-to-unlock";
            accountLockRedisRepository.lock(userId, Duration.ofMinutes(30)).block();

            // when
            StepVerifier.create(accountLockRedisRepository.unlock(userId))
                    .assertNext(success -> assertThat(success).isTrue())
                    .verifyComplete();

            // then - 잠금 해제 확인
            StepVerifier.create(accountLockRedisRepository.isLocked(userId))
                    .assertNext(locked -> assertThat(locked).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("잠기지 않은 계정 해제 시 false를 반환해야 한다")
        void shouldReturnFalseWhenUnlockingNonLockedAccount() {
            // given
            String userId = "user-never-locked";

            // when & then
            StepVerifier.create(accountLockRedisRepository.unlock(userId))
                    .assertNext(success -> assertThat(success).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("isLocked 메서드")
    class IsLockedTest {

        @Test
        @DisplayName("잠긴 계정은 true를 반환해야 한다")
        void shouldReturnTrueForLockedAccount() {
            // given
            String userId = "user-is-locked";
            accountLockRedisRepository.lock(userId, Duration.ofMinutes(30)).block();

            // when & then
            StepVerifier.create(accountLockRedisRepository.isLocked(userId))
                    .assertNext(locked -> assertThat(locked).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("잠기지 않은 계정은 false를 반환해야 한다")
        void shouldReturnFalseForNonLockedAccount() {
            // given
            String userId = "user-not-locked";

            // when & then
            StepVerifier.create(accountLockRedisRepository.isLocked(userId))
                    .assertNext(locked -> assertThat(locked).isFalse())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getLockTtl 메서드")
    class GetLockTtlTest {

        @Test
        @DisplayName("잠긴 계정의 남은 TTL을 반환해야 한다")
        void shouldReturnRemainingTtlForLockedAccount() {
            // given
            String userId = "user-with-ttl";
            Duration lockDuration = Duration.ofSeconds(120);
            accountLockRedisRepository.lock(userId, lockDuration).block();

            // when & then
            StepVerifier.create(accountLockRedisRepository.getLockTtl(userId))
                    .assertNext(
                            ttl -> {
                                assertThat(ttl).isPositive();
                                assertThat(ttl).isLessThanOrEqualTo(120L);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 키는 -2를 반환해야 한다")
        void shouldReturnNegativeTwoForNonExistentKey() {
            // given
            String userId = "user-no-lock";

            // when & then
            StepVerifier.create(accountLockRedisRepository.getLockTtl(userId))
                    .assertNext(ttl -> assertThat(ttl).isEqualTo(-2L))
                    .verifyComplete();
        }
    }
}
