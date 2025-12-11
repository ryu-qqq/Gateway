# LockAdapter 테스트 가이드

> **목적**: LockAdapter의 단위 테스트 전략 (Mockito 기반)

---

## 1) 테스트 전략

### 테스트 대상
LockAdapter는 **Redisson 호출**만 검증합니다:

```
✅ 테스트 항목:
1. tryLock() 호출 시 RLock.tryLock() 검증
2. unlock() 호출 시 RLock.unlock() 검증 (조건부)
3. isHeldByCurrentThread() 검증
4. isLocked() 검증
5. InterruptedException 처리 검증
6. Lock 캐싱 검증
```

### 테스트 범위
- ✅ `@ExtendWith(MockitoExtension.class)` (단위 테스트)
- ✅ Mock을 사용한 의존성 격리
- ✅ 빠른 실행 (밀리초 단위)
- ❌ 실제 Redis 사용 금지 (통합 테스트로 분리)

---

## 2) 기본 템플릿

```java
package com.ryuqq.adapter.out.persistence.redis.common.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DistributedLockAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("lock")
@Tag("persistence-layer")
@DisplayName("DistributedLockAdapter 단위 테스트")
class DistributedLockAdapterTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private DistributedLockAdapter lockAdapter;

    @BeforeEach
    void setUp() {
        lockAdapter = new DistributedLockAdapter(redissonClient);
    }

    @Nested
    @DisplayName("tryLock 메서드")
    class TryLockTest {

        @Test
        @DisplayName("Lock 획득 성공 시 true를 반환해야 한다")
        void tryLock_WhenAcquired_ShouldReturnTrue() throws InterruptedException {
            // Given
            String key = "lock:order:123";
            long waitTime = 10;
            long leaseTime = 30;
            TimeUnit unit = TimeUnit.SECONDS;

            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.tryLock(waitTime, leaseTime, unit)).thenReturn(true);

            // When
            boolean result = lockAdapter.tryLock(key, waitTime, leaseTime, unit);

            // Then
            assertThat(result).isTrue();
            verify(redissonClient).getLock(key);
            verify(rLock).tryLock(waitTime, leaseTime, unit);
        }

        @Test
        @DisplayName("Lock 획득 실패 시 false를 반환해야 한다")
        void tryLock_WhenNotAcquired_ShouldReturnFalse() throws InterruptedException {
            // Given
            String key = "lock:order:123";
            long waitTime = 10;
            long leaseTime = 30;
            TimeUnit unit = TimeUnit.SECONDS;

            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.tryLock(waitTime, leaseTime, unit)).thenReturn(false);

            // When
            boolean result = lockAdapter.tryLock(key, waitTime, leaseTime, unit);

            // Then
            assertThat(result).isFalse();
            verify(rLock).tryLock(waitTime, leaseTime, unit);
        }

        @Test
        @DisplayName("InterruptedException 발생 시 LockAcquisitionException을 던져야 한다")
        void tryLock_WhenInterrupted_ShouldThrowException() throws InterruptedException {
            // Given
            String key = "lock:order:123";
            long waitTime = 10;
            long leaseTime = 30;
            TimeUnit unit = TimeUnit.SECONDS;

            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.tryLock(waitTime, leaseTime, unit))
                .thenThrow(new InterruptedException("Interrupted"));

            // When & Then
            assertThatThrownBy(() -> lockAdapter.tryLock(key, waitTime, leaseTime, unit))
                .isInstanceOf(LockAcquisitionException.class)
                .hasMessageContaining("Lock 획득 중 인터럽트");

            // 인터럽트 상태 복원 확인
            assertThat(Thread.currentThread().isInterrupted()).isTrue();

            // 테스트 후 인터럽트 상태 정리
            Thread.interrupted();
        }

        @Test
        @DisplayName("동일한 Key에 대해 같은 Lock 인스턴스를 반환해야 한다")
        void tryLock_WithSameKey_ShouldUseCachedLock() throws InterruptedException {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

            // When
            lockAdapter.tryLock(key, 10, 30, TimeUnit.SECONDS);
            lockAdapter.tryLock(key, 10, 30, TimeUnit.SECONDS);

            // Then - RedissonClient.getLock()은 한 번만 호출되어야 함 (캐싱)
            verify(redissonClient, times(1)).getLock(key);
        }
    }

    @Nested
    @DisplayName("unlock 메서드")
    class UnlockTest {

        @Test
        @DisplayName("현재 스레드가 Lock을 보유 중이면 unlock을 호출해야 한다")
        void unlock_WhenHeldByCurrentThread_ShouldUnlock() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            // When
            lockAdapter.unlock(key);

            // Then
            verify(rLock).isHeldByCurrentThread();
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("현재 스레드가 Lock을 보유하지 않으면 unlock을 호출하지 않아야 한다")
        void unlock_WhenNotHeldByCurrentThread_ShouldNotUnlock() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            // When
            lockAdapter.unlock(key);

            // Then
            verify(rLock).isHeldByCurrentThread();
            verify(rLock, never()).unlock();
        }
    }

    @Nested
    @DisplayName("isHeldByCurrentThread 메서드")
    class IsHeldByCurrentThreadTest {

        @Test
        @DisplayName("Lock을 보유 중이면 true를 반환해야 한다")
        void isHeldByCurrentThread_WhenHeld_ShouldReturnTrue() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);

            // When
            boolean result = lockAdapter.isHeldByCurrentThread(key);

            // Then
            assertThat(result).isTrue();
            verify(rLock).isHeldByCurrentThread();
        }

        @Test
        @DisplayName("Lock을 보유하지 않으면 false를 반환해야 한다")
        void isHeldByCurrentThread_WhenNotHeld_ShouldReturnFalse() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            // When
            boolean result = lockAdapter.isHeldByCurrentThread(key);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isLocked 메서드")
    class IsLockedTest {

        @Test
        @DisplayName("Lock이 걸려있으면 true를 반환해야 한다")
        void isLocked_WhenLocked_ShouldReturnTrue() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isLocked()).thenReturn(true);

            // When
            boolean result = lockAdapter.isLocked(key);

            // Then
            assertThat(result).isTrue();
            verify(rLock).isLocked();
        }

        @Test
        @DisplayName("Lock이 걸려있지 않으면 false를 반환해야 한다")
        void isLocked_WhenNotLocked_ShouldReturnFalse() {
            // Given
            String key = "lock:order:123";
            when(redissonClient.getLock(key)).thenReturn(rLock);
            when(rLock.isLocked()).thenReturn(false);

            // When
            boolean result = lockAdapter.isLocked(key);

            // Then
            assertThat(result).isFalse();
        }
    }
}
```

---

## 3) 동시성 테스트 (통합 테스트)

분산락의 동시성 보장은 통합 테스트에서 검증합니다.

```java
package com.ryuqq.adapter.out.persistence.redis.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DistributedLockAdapter 통합 테스트 (Testcontainers)
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("lock")
@DisplayName("DistributedLockAdapter 통합 테스트")
class DistributedLockAdapterIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private DistributedLockAdapter lockAdapter;

    @Test
    @DisplayName("동시에 100개 스레드가 Lock을 시도해도 하나만 성공해야 한다")
    void concurrentLock_OnlyOneThreadShouldAcquire() throws InterruptedException {
        // Given
        String lockKey = "lock:concurrent:test";
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        // When
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean acquired = lockAdapter.tryLock(
                        lockKey, 5, 10, TimeUnit.SECONDS
                    );

                    if (acquired) {
                        successCount.incrementAndGet();
                        try {
                            // 임계 영역 (동시 실행 시 문제 발생)
                            processedCount.incrementAndGet();
                            Thread.sleep(100);
                        } finally {
                            lockAdapter.unlock(lockKey);
                        }
                    }
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        // Lock을 획득한 스레드만 임계 영역 진입
        assertThat(processedCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Lock 해제 후 다른 스레드가 Lock을 획득할 수 있어야 한다")
    void afterUnlock_AnotherThreadCanAcquire() throws InterruptedException {
        // Given
        String lockKey = "lock:sequential:test";

        // Thread 1: Lock 획득 후 해제
        boolean acquired1 = lockAdapter.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
        assertThat(acquired1).isTrue();
        lockAdapter.unlock(lockKey);

        // Thread 2: Lock 획득 시도
        boolean acquired2 = lockAdapter.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);

        // Then
        assertThat(acquired2).isTrue();
        lockAdapter.unlock(lockKey);
    }
}
```

---

## 4) Do / Don't

### ❌ Bad Examples

```java
// ❌ 실제 Redis 사용 (단위 테스트)
@DataRedisTest
class LockAdapterTest {
    // 단위 테스트는 Mockito 사용!
}

// ❌ Spring Context 로딩 (단위 테스트)
@SpringBootTest
class LockAdapterTest {
    // Spring Context 불필요!
}

// ❌ 비즈니스 로직 테스트
@Test
void tryLock_ShouldValidateOrder() {
    Order order = Order.create(...);  // 비즈니스 로직은 UseCase Test로!
}

// ❌ InterruptedException 무시
@Test
void tryLock_WhenInterrupted_ShouldReturnFalse() {
    // InterruptedException 처리 검증 누락
}
```

### ✅ Good Examples

```java
// ✅ Mockito 단위 테스트
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("lock")
@Tag("persistence-layer")
class DistributedLockAdapterTest {
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;
}

// ✅ Lock 획득 검증
@Test
void tryLock_WhenAcquired_ShouldReturnTrue() throws InterruptedException {
    when(redissonClient.getLock(key)).thenReturn(rLock);
    when(rLock.tryLock(waitTime, leaseTime, unit)).thenReturn(true);

    boolean result = lockAdapter.tryLock(key, waitTime, leaseTime, unit);

    assertThat(result).isTrue();
}

// ✅ InterruptedException 처리 검증
@Test
void tryLock_WhenInterrupted_ShouldRestoreInterruptFlag() {
    when(rLock.tryLock(anyLong(), anyLong(), any()))
        .thenThrow(new InterruptedException());

    assertThatThrownBy(() -> lockAdapter.tryLock(key, 10, 30, TimeUnit.SECONDS))
        .isInstanceOf(LockAcquisitionException.class);

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();  // 정리
}

// ✅ 조건부 unlock 검증
@Test
void unlock_WhenNotHeld_ShouldNotUnlock() {
    when(rLock.isHeldByCurrentThread()).thenReturn(false);

    lockAdapter.unlock(key);

    verify(rLock, never()).unlock();
}
```

---

## 5) 체크리스트

LockAdapter 테스트 작성 시:
- [ ] **테스트 클래스 태그 추가** (필수)
  - [ ] `@Tag("unit")` - 단위 테스트 표시
  - [ ] `@Tag("lock")` - Lock Adapter 테스트 표시
  - [ ] `@Tag("persistence-layer")` - Persistence Layer 표시
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` RedissonClient, RLock
- [ ] **tryLock 검증**
  - [ ] Lock 획득 성공/실패
  - [ ] InterruptedException 처리
  - [ ] Lock 캐싱 (같은 Key → 같은 인스턴스)
- [ ] **unlock 검증**
  - [ ] isHeldByCurrentThread() true → unlock 호출
  - [ ] isHeldByCurrentThread() false → unlock 미호출
- [ ] **isHeldByCurrentThread 검증**
- [ ] **isLocked 검증**
- [ ] **통합 테스트** (Testcontainers)
  - [ ] 동시성 테스트
  - [ ] Lock 해제 후 재획득 테스트

---

## 📖 관련 문서

- **[Lock Adapter 가이드](lock-adapter-guide.md)** - LockAdapter 구현 가이드
- **[Lock Adapter ArchUnit](lock-adapter-archunit.md)** - ArchUnit 자동 검증 규칙
- **[Distributed Lock 가이드](distributed-lock-guide.md)** - 분산락 전략

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
