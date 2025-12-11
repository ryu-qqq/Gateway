# Lock Adapter 테스트 가이드

> **목적**: Redisson 기반 분산락 Adapter 테스트 작성 규칙

---

## 1. 개요

### Lock Adapter 테스트 범위

```
┌─────────────────────────────────────────────────────────────────┐
│  Lock Adapter 테스트 범위                                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐           ┌──────────────┐                    │
│  │   LockPort   │ ◀──────── │   Redis      │                    │
│  │  (Adapter)   │  Redisson │ (Container)  │                    │
│  └──────────────┘           └──────────────┘                    │
│                                                                  │
│  검증 대상:                                                      │
│  • tryLock() - Lock 획득 시도                                    │
│  • unlock() - Lock 해제                                          │
│  • isLocked() - Lock 상태 확인                                   │
│  • 동시성 - 다중 스레드 Lock 경쟁                                │
│  • Watchdog - 장시간 작업 시 Lock 연장                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 왜 통합 테스트가 필수인가?

```
┌─────────────────────────────────────────────────────────────────┐
│  분산락 테스트에서 Mocking의 한계                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ❌ Mock으로 검증 불가능한 것들:                                  │
│     • 동시성 Race Condition                                      │
│     • Redisson Pub/Sub 동작                                      │
│     • Watchdog 자동 연장                                         │
│     • 네트워크 지연/실패 시 동작                                  │
│     • Lock 획득 대기 중 타임아웃                                  │
│                                                                  │
│  ✅ TestContainers로 검증 가능:                                  │
│     • 실제 다중 스레드 동시성                                    │
│     • 실제 Lock 획득/해제 흐름                                   │
│     • 실제 타임아웃 동작                                         │
│     • 운영 환경과 동일한 동작                                    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 지원 클래스

### 2.1 LockTestSupport (기반 클래스)

```java
/**
 * Lock Adapter 테스트 지원 추상 클래스
 *
 * <p>모든 Lock Adapter 테스트는 이 클래스를 상속받아 작성합니다.
 *
 * <p>제공 기능:
 * <ul>
 *   <li>TestContainers Redis 자동 설정</li>
 *   <li>RedissonClient 자동 주입</li>
 *   <li>동시성 테스트 유틸리티</li>
 *   <li>Lock 상태 검증 유틸리티</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class LockTestSupport {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    protected RedissonClient redissonClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 모든 Lock 해제
        redissonClient.getKeys().flushdb();
    }

    /**
     * Lock이 획득되어 있는지 검증
     *
     * @param lockKey Lock 키
     */
    protected void assertLocked(LockKey lockKey) {
        RLock lock = redissonClient.getLock(lockKey.value());
        assertThat(lock.isLocked())
                .as("Lock should be held for key: %s", lockKey.value())
                .isTrue();
    }

    /**
     * Lock이 해제되어 있는지 검증
     *
     * @param lockKey Lock 키
     */
    protected void assertUnlocked(LockKey lockKey) {
        RLock lock = redissonClient.getLock(lockKey.value());
        assertThat(lock.isLocked())
                .as("Lock should be released for key: %s", lockKey.value())
                .isFalse();
    }

    /**
     * 동시성 테스트 실행 유틸리티
     *
     * @param threadCount 동시 실행 스레드 수
     * @param task 각 스레드에서 실행할 작업
     * @return 성공한 스레드 수
     */
    protected int runConcurrently(int threadCount, Callable<Boolean> task)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();  // 모든 스레드 동시 시작 대기
                    if (task.call()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 발생 시 실패로 처리
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 모든 스레드 동시 시작
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        return successCount.get();
    }
}
```

---

## 3. 통합 테스트

### 3.1 기본 Lock 동작 테스트

```java
@DisplayName("DistributedLockAdapter 통합 테스트")
class DistributedLockAdapterTest extends LockTestSupport {

    @Autowired
    private DistributedLockAdapter lockAdapter;

    @Nested
    @DisplayName("tryLock()")
    class TryLock {

        @Test
        @DisplayName("성공 - Lock 획득")
        void success() {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);

            // When
            boolean acquired = lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);

            // Then
            assertThat(acquired).isTrue();
            assertLocked(lockKey);

            // Cleanup
            lockAdapter.unlock(lockKey);
        }

        @Test
        @DisplayName("성공 - 이미 잡힌 Lock 대기 후 획득")
        void waitAndAcquire() throws InterruptedException {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);

            // 먼저 Lock 획득
            boolean firstAcquired = lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
            assertThat(firstAcquired).isTrue();

            // When - 별도 스레드에서 Lock 획득 시도 (2초 후 해제 예정)
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
                    lockAdapter.tryLock(lockKey, 5, 30, TimeUnit.SECONDS)
            );

            // 1초 후 Lock 해제
            Thread.sleep(1000);
            lockAdapter.unlock(lockKey);

            // Then - 대기 중이던 스레드가 Lock 획득
            boolean secondAcquired = future.get(10, TimeUnit.SECONDS);
            assertThat(secondAcquired).isTrue();

            // Cleanup
            lockAdapter.unlock(lockKey);
        }

        @Test
        @DisplayName("실패 - 타임아웃으로 Lock 획득 실패")
        void timeout() throws InterruptedException {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);

            // 먼저 Lock 획득 (장시간 유지)
            boolean firstAcquired = lockAdapter.tryLock(lockKey, 10, 60, TimeUnit.SECONDS);
            assertThat(firstAcquired).isTrue();

            // When - 짧은 대기 시간으로 Lock 획득 시도
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
                    lockAdapter.tryLock(lockKey, 1, 30, TimeUnit.SECONDS)  // 1초만 대기
            );

            // Then - 타임아웃으로 실패
            boolean secondAcquired = future.get(5, TimeUnit.SECONDS);
            assertThat(secondAcquired).isFalse();

            // Cleanup
            lockAdapter.unlock(lockKey);
        }
    }

    @Nested
    @DisplayName("unlock()")
    class Unlock {

        @Test
        @DisplayName("성공 - Lock 해제")
        void success() {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);
            lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
            assertLocked(lockKey);

            // When
            lockAdapter.unlock(lockKey);

            // Then
            assertUnlocked(lockKey);
        }

        @Test
        @DisplayName("성공 - 이미 해제된 Lock 해제 시도 (예외 없음)")
        void unlockAlreadyReleased() {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);
            assertUnlocked(lockKey);

            // When & Then - 예외 없이 성공
            assertThatCode(() -> lockAdapter.unlock(lockKey))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("isLocked()")
    class IsLocked {

        @Test
        @DisplayName("성공 - Lock 상태 확인 (잠김)")
        void locked() {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);
            lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);

            // When
            boolean isLocked = lockAdapter.isLocked(lockKey);

            // Then
            assertThat(isLocked).isTrue();

            // Cleanup
            lockAdapter.unlock(lockKey);
        }

        @Test
        @DisplayName("성공 - Lock 상태 확인 (해제됨)")
        void unlocked() {
            // Given
            OrderLockKey lockKey = new OrderLockKey(100L);

            // When
            boolean isLocked = lockAdapter.isLocked(lockKey);

            // Then
            assertThat(isLocked).isFalse();
        }
    }
}
```

### 3.2 동시성 테스트 (필수)

```java
@Nested
@DisplayName("동시성 테스트")
class Concurrency {

    @Test
    @DisplayName("성공 - 10개 스레드 중 동시에 1개만 Lock 획득")
    void onlyOneAcquiresLock() throws InterruptedException {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);
        int threadCount = 10;
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        // When
        int successCount = runConcurrently(threadCount, () -> {
            boolean acquired = lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // 동시 실행 수 추적
                    int current = concurrentCount.incrementAndGet();
                    maxConcurrent.updateAndGet(max -> Math.max(max, current));

                    // 작업 시뮬레이션
                    Thread.sleep(100);

                    concurrentCount.decrementAndGet();
                    return true;
                } finally {
                    lockAdapter.unlock(lockKey);
                }
            }
            return false;
        });

        // Then
        assertThat(successCount).isEqualTo(threadCount);  // 모두 순차적으로 성공
        assertThat(maxConcurrent.get()).isEqualTo(1);     // 동시 실행은 최대 1개
    }

    @Test
    @DisplayName("성공 - 다른 Lock Key는 동시 획득 가능")
    void differentKeysCanBeLocked() throws InterruptedException {
        // Given
        int threadCount = 5;
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // When - 각 스레드가 다른 Lock Key 사용
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OrderLockKey lockKey = new OrderLockKey((long) index);

                    if (lockAdapter.tryLock(lockKey, 5, 30, TimeUnit.SECONDS)) {
                        try {
                            successCount.incrementAndGet();
                            Thread.sleep(100);
                        } finally {
                            lockAdapter.unlock(lockKey);
                        }
                    }
                } catch (Exception e) {
                    // 무시
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 모든 스레드가 동시에 각자의 Lock 획득
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("성공 - Lock 획득 실패 시 다른 스레드에 양보")
    void failedThreadsYield() throws InterruptedException {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);
        int threadCount = 5;
        AtomicInteger completedWork = new AtomicInteger(0);

        // When
        int successCount = runConcurrently(threadCount, () -> {
            boolean acquired = lockAdapter.tryLock(lockKey, 1, 5, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    completedWork.incrementAndGet();
                    Thread.sleep(500);  // 작업 시뮬레이션
                    return true;
                } finally {
                    lockAdapter.unlock(lockKey);
                }
            }
            return false;  // Lock 획득 실패
        });

        // Then - 시간 제약으로 일부만 성공
        assertThat(successCount).isGreaterThanOrEqualTo(1);
        assertThat(completedWork.get()).isEqualTo(successCount);
    }
}
```

### 3.3 Lock 만료/연장 테스트

```java
@Nested
@DisplayName("Lock 만료 테스트")
class LockExpiration {

    @Test
    @DisplayName("성공 - leaseTime 후 자동 만료")
    void autoExpireAfterLeaseTime() throws InterruptedException {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);

        // 2초 유지 Lock
        boolean acquired = lockAdapter.tryLock(lockKey, 10, 2, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        assertLocked(lockKey);

        // When - 3초 대기
        Thread.sleep(3000);

        // Then - 자동 만료
        assertUnlocked(lockKey);
    }

    @Test
    @DisplayName("성공 - Watchdog가 작업 중 Lock 연장 (leaseTime=-1)")
    void watchdogExtendsLock() throws InterruptedException {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);

        // leaseTime -1: Watchdog 활성화 (기본 30초마다 연장)
        // Watchdog 테스트를 위해 짧은 lockWatchdogTimeout 설정 필요
        boolean acquired = lockAdapter.tryLockWithWatchdog(lockKey, 10, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();

        // When - Watchdog 기본 연장 주기보다 길게 대기
        // (실제 테스트에서는 설정에 따라 조정)
        Thread.sleep(5000);

        // Then - Watchdog이 연장하므로 여전히 Lock 유지
        assertLocked(lockKey);

        // Cleanup
        lockAdapter.unlock(lockKey);
    }
}
```

### 3.4 LockKey VO 테스트

```java
@Nested
@DisplayName("LockKey VO 검증")
class LockKeyValidation {

    @Test
    @DisplayName("성공 - 유효한 LockKey 생성")
    void validLockKey() {
        // When
        OrderLockKey key = new OrderLockKey(100L);

        // Then
        assertThat(key.value()).isEqualTo("lock:order:100");
    }

    @Test
    @DisplayName("실패 - null orderId")
    void nullOrderId() {
        // When & Then
        assertThatThrownBy(() -> new OrderLockKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must be positive");
    }

    @Test
    @DisplayName("실패 - 0 orderId")
    void zeroOrderId() {
        // When & Then
        assertThatThrownBy(() -> new OrderLockKey(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("성공 - 동일 ID면 동일 Lock Key")
    void sameIdSameKey() {
        // Given
        OrderLockKey key1 = new OrderLockKey(100L);
        OrderLockKey key2 = new OrderLockKey(100L);

        // Then
        assertThat(key1.value()).isEqualTo(key2.value());
        assertThat(key1).isEqualTo(key2);
    }
}
```

---

## 4. finally 블록 테스트

분산락은 반드시 finally에서 해제해야 합니다.

```java
@Nested
@DisplayName("finally 블록 해제 검증")
class FinallyUnlock {

    @Test
    @DisplayName("성공 - 정상 종료 시 Lock 해제")
    void normalCompletion() {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);

        // When
        boolean acquired = lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();

        try {
            // 정상 작업
            performWork();
        } finally {
            lockAdapter.unlock(lockKey);
        }

        // Then
        assertUnlocked(lockKey);
    }

    @Test
    @DisplayName("성공 - 예외 발생 시에도 Lock 해제")
    void exceptionOccurred() {
        // Given
        OrderLockKey lockKey = new OrderLockKey(100L);

        // When
        boolean acquired = lockAdapter.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();

        try {
            // 예외 발생
            throw new RuntimeException("Business error");
        } catch (Exception e) {
            // 예외 처리
        } finally {
            lockAdapter.unlock(lockKey);
        }

        // Then - 예외 발생에도 Lock 해제됨
        assertUnlocked(lockKey);
    }

    private void performWork() {
        // 작업 시뮬레이션
    }
}
```

---

## 5. 체크리스트

### 통합 테스트 작성

- [ ] `LockTestSupport` 상속
- [ ] TestContainers Redis 설정
- [ ] LockKey VO 사용
- [ ] tryLock/unlock 기본 동작 검증
- [ ] 동시성 테스트 (ExecutorService + CountDownLatch)
- [ ] Lock 타임아웃 검증
- [ ] Lock 만료 검증
- [ ] finally 블록 해제 검증

### 동시성 테스트

- [ ] 동일 Lock Key 경쟁 테스트
- [ ] 다른 Lock Key 동시 획득 테스트
- [ ] 최대 동시 실행 수 = 1 검증
- [ ] Lock 실패 시 양보 검증

### LockKey VO 테스트

- [ ] 유효한 키 생성
- [ ] null/0/음수 값 검증
- [ ] key prefix 형식 검증
- [ ] 동등성 테스트

---

## 6. 참고 문서

- [Redis 테스트 가이드](./01_redis-testing-guide.md)
- [Cache Adapter 테스트](./02_cache-adapter-test.md)
- [Lock Adapter 가이드](../lock/lock-adapter-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
