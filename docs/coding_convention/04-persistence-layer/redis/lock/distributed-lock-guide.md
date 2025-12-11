# 분산락 (Distributed Lock) 가이드

> **목적**: Redisson 기반 분산락 전략 및 구현 가이드

---

## 1) 분산락이란?

### 정의
**분산락(Distributed Lock)**은 여러 서버/인스턴스에서 **동일한 리소스에 대한 동시 접근을 제어**하는 메커니즘입니다.

### 사용 시나리오

| 시나리오 | 설명 | 예시 |
|----------|------|------|
| **재고 차감** | 동시 주문 시 재고 정합성 | 100개 재고, 동시 120건 주문 |
| **포인트 사용** | 중복 차감 방지 | 동시에 같은 포인트 사용 |
| **좌석 예약** | 중복 예약 방지 | 같은 좌석 동시 예약 |
| **결제 처리** | 중복 결제 방지 | 동일 주문 동시 결제 시도 |
| **스케줄러** | 중복 실행 방지 | 여러 인스턴스에서 같은 배치 실행 |

---

## 2) 왜 Redisson인가?

### Lettuce vs Redisson 분산락 비교

| 항목 | Lettuce | Redisson |
|------|---------|----------|
| **구현 방식** | SETNX + Polling (스핀락) | Pub/Sub 이벤트 리스너 |
| **Lock 대기** | while 루프로 반복 조회 | 이벤트 수신 후 즉시 시도 |
| **CPU 사용** | 높음 (polling) | 낮음 (event-driven) |
| **네트워크** | 높음 (반복 요청) | 낮음 (subscribe) |
| **Lock 연장** | 직접 구현 필요 | Watchdog 자동 연장 |
| **Lock 타입** | 기본만 | Fair, Read/Write, MultiLock 등 |
| **검증 수준** | 직접 테스트 필요 | 프로덕션 검증됨 |

### Redisson Pub/Sub 메커니즘

```
┌─────────────────────────────────────────────────────────────┐
│                        Redis Server                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Thread A                           Thread B                 │
│  ┌───────────┐                     ┌───────────┐            │
│  │ Lock 획득 │                     │ Lock 대기 │            │
│  │ (성공)    │                     │ (구독 중) │            │
│  └───────────┘                     └───────────┘            │
│       │                                  ▲                  │
│       │ unlock()                         │                  │
│       ▼                                  │                  │
│  ┌───────────────────────────────────────┘                  │
│  │ PUBLISH "redisson_lock__channel:mylock"                  │
│  └─────────────────────────────────────────────────────────│
│                                                              │
│  Thread B: 이벤트 수신 → 즉시 Lock 시도 → 획득 성공         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Watchdog 자동 연장

```
Lock 획득 (leaseTime 미지정 시)
    ↓
Watchdog 활성화 (기본 30초 주기)
    ↓
┌─────────────────────────────────────┐
│ while (Lock 보유 중) {              │
│   10초마다 TTL 갱신 (30초로 연장)   │
│ }                                   │
└─────────────────────────────────────┘
    ↓
unlock() 호출 → Watchdog 종료
```

**장점**: 작업이 예상보다 오래 걸려도 Lock이 만료되지 않음

---

## 3) Lock 타입별 사용법

### 3.1 기본 Lock (RLock)

가장 일반적인 분산락입니다.

```java
RLock lock = redissonClient.getLock("order:123");

try {
    // waitTime: 최대 대기 시간, leaseTime: Lock 유지 시간
    boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

    if (!acquired) {
        throw new LockAcquisitionException("Lock 획득 실패");
    }

    // 비즈니스 로직
    processOrder();

} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new LockAcquisitionException("Lock 획득 중 인터럽트", e);
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 3.2 Fair Lock (RFairLock)

**선착순 보장**: 먼저 요청한 스레드가 먼저 Lock 획득

```java
RLock fairLock = redissonClient.getFairLock("ticket:seat:A1");

try {
    // 먼저 요청한 순서대로 Lock 획득
    boolean acquired = fairLock.tryLock(30, 60, TimeUnit.SECONDS);

    if (acquired) {
        reserveSeat();
    }
} finally {
    if (fairLock.isHeldByCurrentThread()) {
        fairLock.unlock();
    }
}
```

**사용 시나리오**: 좌석 예약, 선착순 이벤트

### 3.3 Read/Write Lock (RReadWriteLock)

**읽기/쓰기 분리**: 읽기는 동시 허용, 쓰기는 배타적

```java
RReadWriteLock rwLock = redissonClient.getReadWriteLock("product:123");

// 읽기 Lock (동시 여러 스레드 허용)
RLock readLock = rwLock.readLock();
try {
    readLock.lock();
    return getProductInfo();
} finally {
    readLock.unlock();
}

// 쓰기 Lock (배타적)
RLock writeLock = rwLock.writeLock();
try {
    writeLock.lock();
    updateProductInfo();
} finally {
    writeLock.unlock();
}
```

**사용 시나리오**: 캐시 갱신, 설정 변경

### 3.4 MultiLock

**여러 리소스 동시 Lock**: 원자적으로 여러 Lock 획득

```java
RLock lock1 = redissonClient.getLock("account:A");
RLock lock2 = redissonClient.getLock("account:B");
RLock multiLock = redissonClient.getMultiLock(lock1, lock2);

try {
    // 두 계좌 모두 Lock 획득 (또는 모두 실패)
    boolean acquired = multiLock.tryLock(10, 30, TimeUnit.SECONDS);

    if (acquired) {
        transferMoney(accountA, accountB, amount);
    }
} finally {
    multiLock.unlock();
}
```

**사용 시나리오**: 계좌 이체, 여러 리소스 동시 수정

### 3.5 RedLock (고가용성)

**분산 환경 Lock**: 여러 Redis 인스턴스에서 과반수 Lock 획득

```java
// 여러 Redis 인스턴스 설정
RLock lock1 = redisson1.getLock("myLock");
RLock lock2 = redisson2.getLock("myLock");
RLock lock3 = redisson3.getLock("myLock");

RLock redLock = redissonClient.getRedLock(lock1, lock2, lock3);

try {
    // 3개 중 2개 이상에서 Lock 획득 성공 시 전체 성공
    boolean acquired = redLock.tryLock(10, 30, TimeUnit.SECONDS);

    if (acquired) {
        processCriticalOperation();
    }
} finally {
    redLock.unlock();
}
```

**사용 시나리오**: 결제 처리, 중요 비즈니스 로직

---

## 4) Timeout 전략

### 권장 Timeout 값

| 작업 유형 | waitTime | leaseTime | 예시 |
|----------|----------|-----------|------|
| **빠른 작업** | 5초 | 10초 | 재고 차감, 포인트 사용 |
| **일반 작업** | 10초 | 30초 | 주문 처리, 결제 |
| **무거운 작업** | 30초 | 60초 | 대량 데이터 처리 |
| **배치 작업** | 60초 | 300초 | 스케줄러, 정산 |

### Timeout 설정 원칙

```java
// ❌ Bad: leaseTime 없이 (Watchdog 의존)
lock.tryLock(10, TimeUnit.SECONDS);  // leaseTime = -1 (무한)

// ✅ Good: 명시적 leaseTime
lock.tryLock(10, 30, TimeUnit.SECONDS);  // waitTime=10초, leaseTime=30초

// ✅ Good: 작업 시간 기반 계산
Duration estimatedTime = Duration.ofSeconds(5);
Duration buffer = Duration.ofSeconds(10);
long leaseTime = estimatedTime.plus(buffer).getSeconds();
lock.tryLock(10, leaseTime, TimeUnit.SECONDS);
```

---

## 5) Key Naming Convention

### 패턴
```
lock:{domain}:{entity}:{id}
```

### 예시
```java
// 주문 Lock
"lock:order:123"

// 재고 Lock
"lock:stock:item:456"

// 좌석 Lock
"lock:reservation:seat:A1"

// 계좌 Lock
"lock:account:user:789"

// 스케줄러 Lock (ID 없음)
"lock:scheduler:daily-batch"
```

---

## 6) 예외 처리

### 커스텀 예외

```java
/**
 * Lock 획득 실패 예외
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message) {
        super(message);
    }

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Lock Timeout 예외
 */
public class LockTimeoutException extends LockAcquisitionException {

    private final String lockKey;
    private final long waitTimeMs;

    public LockTimeoutException(String lockKey, long waitTimeMs) {
        super(String.format("Lock 획득 타임아웃: key=%s, waitTime=%dms", lockKey, waitTimeMs));
        this.lockKey = lockKey;
        this.waitTimeMs = waitTimeMs;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }
}
```

### 예외 처리 패턴

```java
public void processWithLock(Long orderId) {
    String lockKey = "lock:order:" + orderId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);

        if (!acquired) {
            throw new LockTimeoutException(lockKey, 10000);
        }

        processOrder(orderId);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new LockAcquisitionException("Lock 획득 중 인터럽트: " + lockKey, e);
    } finally {
        // 현재 스레드가 Lock을 보유한 경우에만 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

---

## 7) Do / Don't

### ❌ Bad Examples

```java
// ❌ finally에서 무조건 unlock (다른 스레드 Lock 해제 위험)
finally {
    lock.unlock();  // IllegalMonitorStateException 가능
}

// ❌ Lock 중첩 (데드락 위험)
lock1.lock();
lock2.lock();  // lock1 보유 상태에서 lock2 대기 → 데드락 가능

// ❌ Lock 내에서 외부 API 호출 (장시간 점유)
lock.lock();
try {
    externalApiClient.call();  // 네트워크 지연 → Lock 장시간 점유
} finally {
    lock.unlock();
}

// ❌ 넓은 범위 Lock (성능 저하)
lock.lock();
try {
    validateOrder();     // Lock 불필요
    calculatePrice();    // Lock 불필요
    decreaseStock();     // Lock 필요
    saveOrder();         // Lock 불필요
} finally {
    lock.unlock();
}
```

### ✅ Good Examples

```java
// ✅ 현재 스레드 확인 후 unlock
finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}

// ✅ MultiLock으로 원자적 획득
RLock multiLock = redissonClient.getMultiLock(lock1, lock2);
multiLock.tryLock(10, 30, TimeUnit.SECONDS);

// ✅ Lock 범위 최소화
validateOrder();
calculatePrice();

lock.lock();
try {
    decreaseStock();  // Lock이 필요한 부분만
} finally {
    lock.unlock();
}

saveOrder();

// ✅ 명시적 Timeout
boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
```

---

## 8) 체크리스트

분산락 구현 시:
- [ ] **Redisson 사용** (Lettuce 스핀락 금지)
- [ ] **적절한 Lock 타입 선택** (기본, Fair, Read/Write, Multi)
- [ ] **Key Naming Convention 준수** (`lock:{domain}:{entity}:{id}`)
- [ ] **waitTime, leaseTime 명시**
- [ ] **try-finally 패턴** (unlock 보장)
- [ ] **isHeldByCurrentThread() 확인** (다른 스레드 Lock 해제 방지)
- [ ] **InterruptedException 처리** (스레드 인터럽트 전파)
- [ ] **Lock 범위 최소화** (성능 최적화)
- [ ] **Lock 내 외부 API 호출 금지**
- [ ] **커스텀 예외 정의** (LockAcquisitionException)
- [ ] **모니터링 설정** (Lock 획득 시간, 실패율)

---

## 📖 관련 문서

- **[Lock Adapter 가이드](lock-adapter-guide.md)** - LockAdapter 구현
- **[Lock Adapter 테스트](lock-adapter-test-guide.md)** - 테스트 전략
- **[Lock Adapter ArchUnit](lock-adapter-archunit.md)** - ArchUnit 규칙
- **[Redisson Documentation](https://github.com/redisson/redisson/wiki)** - 공식 문서

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
