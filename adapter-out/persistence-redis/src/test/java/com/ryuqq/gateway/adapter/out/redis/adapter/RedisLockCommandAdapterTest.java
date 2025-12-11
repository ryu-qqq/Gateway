package com.ryuqq.gateway.adapter.out.redis.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("RedisLockCommandAdapter 테스트")
class RedisLockCommandAdapterTest {

    private RedissonReactiveClient redissonReactiveClient;
    private RLockReactive lock;
    private RedisLockCommandAdapter adapter;

    @BeforeEach
    void setUp() {
        redissonReactiveClient = mock(RedissonReactiveClient.class);
        lock = mock(RLockReactive.class);
        adapter = new RedisLockCommandAdapter(redissonReactiveClient);

        // Default stubbing
        given(redissonReactiveClient.getLock(anyString())).willReturn(lock);
    }

    @Nested
    @DisplayName("tryLock() 테스트")
    class TryLockTest {

        @Test
        @DisplayName("Lock 획득 성공 시 true 반환")
        void shouldReturnTrueWhenLockAcquired() {
            // given
            String tenantId = "tenant-1";
            Long userId = 123L;

            given(lock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(adapter.tryLock(tenantId, userId))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            verify(redissonReactiveClient).getLock("tenant:tenant-1:refresh:lock:123");
        }

        @Test
        @DisplayName("Lock 획득 실패 시 false 반환")
        void shouldReturnFalseWhenLockNotAcquired() {
            // given
            String tenantId = "tenant-2";
            Long userId = 456L;

            given(lock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).willReturn(Mono.just(false));

            // when & then
            StepVerifier.create(adapter.tryLock(tenantId, userId))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Lock 획득 중 에러 발생 시 false 반환")
        void shouldReturnFalseWhenErrorOccurs() {
            // given
            String tenantId = "tenant-3";
            Long userId = 789L;

            given(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS)))
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(adapter.tryLock(tenantId, userId))
                    .assertNext(result -> assertThat(result).isFalse())
                    .verifyComplete();
        }

        @Test
        @DisplayName("다양한 tenantId와 userId 조합으로 Lock Key 생성")
        void shouldBuildCorrectLockKey() {
            // given
            String tenantId = "tenant-999";
            Long userId = 12345L;

            given(lock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(adapter.tryLock(tenantId, userId))
                    .assertNext(result -> assertThat(result).isTrue())
                    .verifyComplete();

            verify(redissonReactiveClient).getLock("tenant:tenant-999:refresh:lock:12345");
        }
    }

    @Nested
    @DisplayName("unlock() 테스트")
    class UnlockTest {

        @Test
        @DisplayName("Lock 해제 성공")
        void shouldUnlockSuccessfully() {
            // given
            String tenantId = "tenant-1";
            Long userId = 123L;

            given(lock.forceUnlock()).willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(adapter.unlock(tenantId, userId)).verifyComplete();

            verify(redissonReactiveClient).getLock("tenant:tenant-1:refresh:lock:123");
            verify(lock).forceUnlock();
        }

        @Test
        @DisplayName("Lock 해제 중 에러 발생 시 무시하고 완료")
        void shouldIgnoreErrorOnUnlock() {
            // given
            String tenantId = "tenant-2";
            Long userId = 456L;

            given(lock.forceUnlock())
                    .willReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when & then
            StepVerifier.create(adapter.unlock(tenantId, userId)).verifyComplete();
        }

        @Test
        @DisplayName("다양한 tenantId와 userId 조합으로 Lock Key 생성")
        void shouldBuildCorrectLockKeyForUnlock() {
            // given
            String tenantId = "tenant-888";
            Long userId = 99999L;

            given(lock.forceUnlock()).willReturn(Mono.just(true));

            // when & then
            StepVerifier.create(adapter.unlock(tenantId, userId)).verifyComplete();

            verify(redissonReactiveClient).getLock("tenant:tenant-888:refresh:lock:99999");
        }
    }
}
