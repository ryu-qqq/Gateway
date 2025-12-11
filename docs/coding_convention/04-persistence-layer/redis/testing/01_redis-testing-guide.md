# Persistence Redis Layer 테스트 가이드

> **목적**: Redis Persistence Layer의 테스트 작성 규칙 및 패턴 정의
>
> **Lettuce (캐싱)** + **Redisson (분산락)** 이원화 전략 기반

---

## 1. 개요

### Persistence Redis Layer 테스트 전략

```
┌─────────────────────────────────────────────────────────────────┐
│  Persistence Redis Layer 테스트 피라미드                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                      ┌─────────┐                                 │
│                      │ 통합    │  ← Adapter 통합 테스트           │
│                      │ Test    │    TestContainers + Redis       │
│                  ┌───┴─────────┴───┐                             │
│                  │  Unit Test      │  ← Adapter 단위 테스트       │
│                  │  (Mocking)      │    Mockito                   │
│              ┌───┴─────────────────┴───┐                         │
│              │  ArchUnit Tests         │  ← 아키텍처 검증 (필수)   │
│              │  (Architecture Rules)   │    Zero-Tolerance        │
│              └─────────────────────────┘                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 테스트 유형별 역할

| 테스트 유형 | 필수 여부 | 목적 | 도구 |
|------------|----------|------|------|
| **통합 테스트** | ✅ 필수 | Cache/Lock 실제 동작 검증 | TestContainers + Redis |
| **단위 테스트** | 🔶 선택 | Adapter 로직 격리 검증 | Mockito |
| **ArchUnit** | ✅ 필수 | 아키텍처 규칙 강제 | ArchUnit |

### 왜 통합 테스트가 중요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│  Redis 통합 테스트가 필수인 이유                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  🔴 분산락 테스트:                                               │
│     • 동시성 문제는 Mocking으로 검증 불가                         │
│     • Redisson Pub/Sub 동작 검증 필요                            │
│     • Watchdog 자동 연장 검증                                    │
│     • Lock 타임아웃 동작 확인                                    │
│                                                                  │
│  🔵 캐시 테스트:                                                 │
│     • TTL 만료 동작 검증                                         │
│     • 직렬화/역직렬화 검증                                       │
│     • Eviction 정책 동작 확인                                    │
│                                                                  │
│  ✅ TestContainers 장점:                                         │
│     • 운영 환경과 동일한 Redis 버전                              │
│     • 실제 네트워크 지연 시뮬레이션                              │
│     • CI/CD에서 일관된 테스트 환경                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 유형별 상세 가이드

### 2.1 Cache Adapter 테스트

> **상세 가이드**: [Cache Adapter 테스트](./02_cache-adapter-test.md)

**적용 대상**:
- CachePort 구현체 동작 검증
- TTL 설정 검증
- 직렬화/역직렬화 검증
- Cache Miss 처리

**테스트 우선순위**:
1. ✅ 통합 테스트 (TestContainers) - 필수
2. 🔶 단위 테스트 (Mockito) - 선택

### 2.2 Lock Adapter 테스트

> **상세 가이드**: [Lock Adapter 테스트](./03_lock-adapter-test.md)

**적용 대상**:
- LockPort 구현체 동작 검증
- 동시성 테스트
- Lock 타임아웃 검증
- Watchdog 연장 검증

**테스트 우선순위**:
1. ✅ 통합 테스트 (TestContainers) - **필수** (동시성 검증)
2. 🔶 단위 테스트 (Mockito) - 선택

---

## 3. Zero-Tolerance 규칙

### 3.1 필수 규칙 ✅

| 규칙 | 설명 | 검증 방법 |
|------|------|----------|
| TestContainers Redis | 실제 Redis 동작 검증 | ArchUnit |
| CacheKey/LockKey VO 사용 | 타입 안전성 보장 | ArchUnit |
| TTL 검증 | 모든 캐시에 TTL 설정 | 통합 테스트 |
| 동시성 테스트 | 분산락 동시성 검증 | 통합 테스트 |

### 3.2 금지 규칙 ❌

| 금지 항목 | 이유 | 대안 |
|----------|------|------|
| Embedded Redis | 운영 환경과 차이 | TestContainers |
| 분산락 Mock 테스트만 | 동시성 미검증 | TestContainers 통합 테스트 |
| String 키 직접 사용 | 타입 안전성 없음 | CacheKey/LockKey VO |
| @Transactional 내 Cache 무효화 | 롤백 시 불일치 | @TransactionalEventListener |
| Lombok | Plain Java 원칙 | 수동 생성자 |

### 3.3 분산락 테스트 필수 사항

```
분산락 테스트 체크리스트:
    │
    ├─ ✅ Lock 획득 성공 케이스
    │
    ├─ ✅ Lock 획득 실패 케이스 (타임아웃)
    │
    ├─ ✅ 동시성 테스트 (다중 스레드)
    │   • ExecutorService + CountDownLatch
    │   • 단 하나의 스레드만 작업 완료
    │
    ├─ ✅ Lock 해제 검증 (finally 블록)
    │
    └─ 🔶 Watchdog 연장 테스트 (선택)
        • 장시간 작업 시 Lock 유지 확인
```

---

## 4. 테스트 디렉토리 구조

```
persistence-redis/src/test/java/
└── com/ryuqq/adapter/out/persistence/redis/
    ├── architecture/                    # ArchUnit 테스트
    │   ├── config/
    │   │   └── RedisConfigArchTest.java
    │   └── adapter/
    │       ├── cache/
    │       │   └── CacheAdapterArchTest.java
    │       └── lock/
    │           └── DistributedLockAdapterArchTest.java
    │
    ├── common/                          # 공통 테스트 지원
    │   ├── RedisTestSupport.java        # Redis 통합 테스트 기반 클래스
    │   ├── CacheTestSupport.java        # Cache 테스트 전용 지원
    │   └── LockTestSupport.java         # Lock 테스트 전용 지원
    │
    ├── cache/                           # Cache Adapter 테스트
    │   └── adapter/
    │       ├── ObjectCacheAdapterTest.java
    │       └── StringCacheAdapterTest.java
    │
    └── lock/                            # Lock Adapter 테스트
        └── adapter/
            └── DistributedLockAdapterTest.java
```

---

## 5. 테스트 데이터 관리

### 5.1 CacheKey/LockKey VO 사용

```java
// ✅ 올바른 방법 - CacheKey VO
public record OrderCacheKey(Long orderId) implements CacheKey {
    private static final String PREFIX = "cache:order:";

    public OrderCacheKey {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + orderId;
    }
}

// 테스트에서 사용
@Test
void cacheTest() {
    OrderCacheKey key = new OrderCacheKey(100L);
    cacheAdapter.set(key, order, Duration.ofMinutes(10));
}

// ❌ 잘못된 방법 - String 직접 사용
@Test
void badCacheTest() {
    String key = "cache:order:100";  // 타입 안전성 없음
    cacheAdapter.set(key, order, Duration.ofMinutes(10));
}
```

### 5.2 테스트 데이터 격리

```java
@AfterEach
void tearDown() {
    // 테스트 후 Redis 데이터 정리
    redisTemplate.getConnectionFactory()
        .getConnection()
        .serverCommands()
        .flushDb();
}
```

---

## 6. 성능 테스트 가이드

### 6.1 캐시 성능 테스트

```java
@Test
@DisplayName("캐시 히트 시 응답 시간 검증")
void cacheHit_responseTime() {
    // Given
    OrderCacheKey key = new OrderCacheKey(100L);
    Order order = createTestOrder();
    cacheAdapter.set(key, order, Duration.ofMinutes(10));

    // When
    long start = System.nanoTime();
    Optional<Order> result = cacheAdapter.get(key, Order.class);
    long elapsed = System.nanoTime() - start;

    // Then
    assertThat(result).isPresent();
    assertThat(elapsed).isLessThan(10_000_000L);  // 10ms 이내
}
```

### 6.2 분산락 동시성 테스트

```java
@Test
@DisplayName("동시 요청 시 단 하나만 Lock 획득")
void concurrentLock_onlyOneSucceeds() throws Exception {
    // Given
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    OrderLockKey lockKey = new OrderLockKey(100L);

    // When
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                startLatch.await();  // 동시 시작

                boolean acquired = lockAdapter.tryLock(
                    lockKey, 100, 5000, TimeUnit.MILLISECONDS);

                if (acquired) {
                    try {
                        successCount.incrementAndGet();
                        Thread.sleep(100);  // 작업 시뮬레이션
                    } finally {
                        lockAdapter.unlock(lockKey);
                    }
                }
            } catch (Exception e) {
                // 예외 처리
            } finally {
                endLatch.countDown();
            }
        });
    }

    startLatch.countDown();  // 모든 스레드 동시 시작
    endLatch.await(10, TimeUnit.SECONDS);

    // Then - 동시에 Lock을 획득한 스레드는 1개뿐
    assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
}
```

---

## 7. 체크리스트

### Cache Adapter 테스트

- [ ] TestContainers Redis 설정
- [ ] `RedisTestSupport` 상속
- [ ] CacheKey VO 사용
- [ ] TTL 설정 검증
- [ ] Cache Hit/Miss 검증
- [ ] 직렬화/역직렬화 검증
- [ ] Eviction 동작 검증

### Lock Adapter 테스트

- [ ] TestContainers Redis 설정
- [ ] `LockTestSupport` 상속
- [ ] LockKey VO 사용
- [ ] Lock 획득/해제 검증
- [ ] 동시성 테스트 (ExecutorService)
- [ ] 타임아웃 동작 검증
- [ ] finally 블록 unlock 검증

### 금지 사항 확인

- [ ] Embedded Redis 사용하지 않음
- [ ] String 키 직접 사용하지 않음
- [ ] 분산락 Mock 테스트만으로 끝내지 않음
- [ ] Lombok 사용하지 않음

---

## 8. 참고 문서

- [Cache Adapter 테스트](./02_cache-adapter-test.md)
- [Lock Adapter 테스트](./03_lock-adapter-test.md)
- [Persistence Redis 전체 가이드](../persistence-redis-guide.md)
- [Cache Adapter 가이드](../adapter/cache-adapter-guide.md)
- [Lock Adapter 가이드](../lock/lock-adapter-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
