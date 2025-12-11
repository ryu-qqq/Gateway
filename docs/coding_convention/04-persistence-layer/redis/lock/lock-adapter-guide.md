# LockAdapter 가이드

> **목적**: Redisson 기반 분산락 Adapter 구현 가이드

---

## 1) LockAdapter란?

### 역할
**Application Layer ↔ Redisson (분산락)**

분산락 획득/해제만 담당하는 **단순 인프라 어댑터** 역할만 수행합니다.

### 책임
- ✅ 분산락 획득 (tryLock)
- ✅ 분산락 해제 (unlock)
- ✅ Lock 객체 제공 (getLock)
- ❌ **비즈니스 로직 금지** (Application/Domain에서 처리)
- ❌ **Lock 내에서 DB 접근 금지**

### 아키텍처

```
Application Layer (UseCase)
  ├─ 1. Lock 획득 시도 (LockPort)
  │     └─ LockAdapter.tryLock(key, waitTime, leaseTime)
  ├─ 2. 비즈니스 로직 실행
  │     └─ Application/Domain 로직
  └─ 3. Lock 해제 (LockPort)
        └─ LockAdapter.unlock(key)
```

---

## 2) 핵심 원칙

### 원칙 1: Port 인터페이스 분리

```java
// Application Layer에 정의
public interface DistributedLockPort {
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);
    void unlock(String key);
    boolean isLocked(String key);
}
```

### 원칙 2: Key Naming Convention

```java
// 패턴: lock:{domain}:{entity}:{id}
private static final String KEY_PREFIX = "lock:order:";

private String generateKey(Long orderId) {
    return KEY_PREFIX + orderId;
}
```

### 원칙 3: 현재 스레드 확인 후 unlock

```java
// ❌ 무조건 unlock (다른 스레드 Lock 해제 위험)
lock.unlock();

// ✅ 현재 스레드가 보유한 경우에만 unlock
if (lock.isHeldByCurrentThread()) {
    lock.unlock();
}
```

### 원칙 4: InterruptedException 처리

```java
try {
    return lock.tryLock(waitTime, leaseTime, unit);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // 인터럽트 상태 복원
    throw new LockAcquisitionException("Lock 획득 중 인터럽트", e);
}
```

---

## 3) 템플릿 코드

### Port 인터페이스 (Application Layer)

```java
package com.ryuqq.application.common.port.out;

import java.util.concurrent.TimeUnit;

/**
 * 분산락 포트 (출력 포트)
 *
 * <p>Redisson 기반 분산락 추상화</p>
 *
 * @author Development Team
 * @since 1.0.0
 */
public interface DistributedLockPort {

    /**
     * 분산락 획득 시도
     *
     * @param key       Lock 키 (예: "lock:order:123")
     * @param waitTime  최대 대기 시간
     * @param leaseTime Lock 유지 시간
     * @param unit      시간 단위
     * @return Lock 획득 성공 여부
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 분산락 해제
     *
     * @param key Lock 키
     */
    void unlock(String key);

    /**
     * Lock 보유 여부 확인
     *
     * @param key Lock 키
     * @return 현재 스레드가 Lock을 보유 중인지 여부
     */
    boolean isHeldByCurrentThread(String key);

    /**
     * Lock 상태 확인
     *
     * @param key Lock 키
     * @return Lock이 걸려있는지 여부
     */
    boolean isLocked(String key);
}
```

### LockAdapter 구현 (Persistence Layer)

```java
package com.ryuqq.adapter.out.persistence.redis.common.adapter;

import com.ryuqq.application.common.port.out.DistributedLockPort;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 분산락 Adapter
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>분산락 획득/해제</li>
 *   <li>Lock 상태 관리</li>
 *   <li>Thread-safe Lock 인스턴스 관리</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Component
public class DistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;

    /**
     * Thread-safe Lock 인스턴스 캐시
     *
     * <p>같은 키에 대해 동일한 Lock 인스턴스 반환</p>
     */
    private final ConcurrentHashMap<String, RLock> lockCache = new ConcurrentHashMap<>();

    public DistributedLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * {@inheritDoc}
     *
     * @throws LockAcquisitionException Lock 획득 중 인터럽트 발생 시
     */
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(key);

        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(
                String.format("Lock 획득 중 인터럽트: key=%s", key), e
            );
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>현재 스레드가 Lock을 보유한 경우에만 해제합니다.</p>
     */
    @Override
    public void unlock(String key) {
        RLock lock = getLock(key);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHeldByCurrentThread(String key) {
        RLock lock = getLock(key);
        return lock.isHeldByCurrentThread();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked(String key) {
        RLock lock = getLock(key);
        return lock.isLocked();
    }

    /**
     * Lock 인스턴스 조회 (캐싱)
     *
     * @param key Lock 키
     * @return RLock 인스턴스
     */
    private RLock getLock(String key) {
        return lockCache.computeIfAbsent(key, redissonClient::getLock);
    }
}
```

### 커스텀 예외

```java
package com.ryuqq.adapter.out.persistence.redis.common.exception;

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
```

---

## 4) Redisson 설정

### RedissonConfig.java

```java
package com.ryuqq.adapter.out.persistence.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 설정
 *
 * @author Development Team
 * @since 1.0.0
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * RedissonClient 생성
     *
     * <p>분산락, 분산 자료구조 등에 사용</p>
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
            .setAddress(address)
            .setPassword(password.isEmpty() ? null : password)
            .setConnectionPoolSize(16)
            .setConnectionMinimumIdleSize(4)
            .setIdleConnectionTimeout(10000)
            .setConnectTimeout(3000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
```

### Cluster 모드 설정 (Prod)

```java
@Bean(destroyMethod = "shutdown")
public RedissonClient redissonClient() {
    Config config = new Config();

    config.useClusterServers()
        .addNodeAddress(
            "redis://node1:6379",
            "redis://node2:6379",
            "redis://node3:6379"
        )
        .setPassword(password)
        .setScanInterval(2000)
        .setMasterConnectionPoolSize(16)
        .setSlaveConnectionPoolSize(16);

    return Redisson.create(config);
}
```

---

## 5) UseCase에서 사용

### 재고 차감 예시

```java
package com.ryuqq.application.stock.usecase;

import com.ryuqq.application.common.port.out.DistributedLockPort;
import com.ryuqq.application.stock.port.in.DecreaseStockUseCase;
import com.ryuqq.application.stock.port.out.StockPersistPort;
import com.ryuqq.application.stock.port.out.StockQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 재고 차감 UseCase
 */
@Service
public class DecreaseStockService implements DecreaseStockUseCase {

    private static final String LOCK_KEY_PREFIX = "lock:stock:item:";
    private static final long LOCK_WAIT_TIME = 10;
    private static final long LOCK_LEASE_TIME = 30;

    private final DistributedLockPort lockPort;
    private final StockQueryPort stockQueryPort;
    private final StockPersistPort stockPersistPort;

    public DecreaseStockService(
            DistributedLockPort lockPort,
            StockQueryPort stockQueryPort,
            StockPersistPort stockPersistPort) {
        this.lockPort = lockPort;
        this.stockQueryPort = stockQueryPort;
        this.stockPersistPort = stockPersistPort;
    }

    /**
     * 재고 차감 (분산락 적용)
     */
    @Override
    @Transactional
    public void decreaseStock(Long itemId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + itemId;

        boolean acquired = lockPort.tryLock(
            lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS
        );

        if (!acquired) {
            throw new StockLockException("재고 Lock 획득 실패: itemId=" + itemId);
        }

        try {
            Stock stock = stockQueryPort.findByItemId(itemId)
                .orElseThrow(() -> new StockNotFoundException(itemId));

            stock.decrease(quantity);
            stockPersistPort.save(stock);

        } finally {
            lockPort.unlock(lockKey);
        }
    }
}
```

---

## 6) Do / Don't

### ❌ Bad Examples

```java
// ❌ 비즈니스 로직 포함
@Override
public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
    if (key.startsWith("order:")) {  // ❌ 비즈니스 판단
        // 주문은 더 오래 대기
        waitTime = waitTime * 2;
    }
    return lock.tryLock(waitTime, leaseTime, unit);
}

// ❌ Lock 내에서 DB 접근
@Override
public boolean tryLockAndProcess(String key, Runnable action) {
    if (tryLock(key, 10, 30, TimeUnit.SECONDS)) {
        repository.save(entity);  // ❌ Adapter에서 DB 접근
        return true;
    }
    return false;
}

// ❌ 무조건 unlock
@Override
public void unlock(String key) {
    getLock(key).unlock();  // ❌ IllegalMonitorStateException 가능
}

// ❌ InterruptedException 무시
@Override
public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
    try {
        return lock.tryLock(waitTime, leaseTime, unit);
    } catch (InterruptedException e) {
        return false;  // ❌ 인터럽트 상태 손실
    }
}
```

### ✅ Good Examples

```java
// ✅ 단순 Lock 획득/해제만
@Override
public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
    try {
        return getLock(key).tryLock(waitTime, leaseTime, unit);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // ✅ 인터럽트 상태 복원
        throw new LockAcquisitionException("Lock 획득 중 인터럽트", e);
    }
}

// ✅ 현재 스레드 확인 후 unlock
@Override
public void unlock(String key) {
    RLock lock = getLock(key);
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}

// ✅ Lock 인스턴스 캐싱 (Thread-safe)
private RLock getLock(String key) {
    return lockCache.computeIfAbsent(key, redissonClient::getLock);
}
```

---

## 7) 체크리스트

LockAdapter 구현 시:
- [ ] **@Component 어노테이션**
- [ ] **Port 인터페이스 구현** (`DistributedLockPort`)
- [ ] **RedissonClient 의존성 주입**
- [ ] **tryLock() 구현** (waitTime, leaseTime 필수)
- [ ] **unlock() 구현** (isHeldByCurrentThread 확인)
- [ ] **InterruptedException 처리** (Thread.currentThread().interrupt())
- [ ] **Lock 인스턴스 캐싱** (ConcurrentHashMap 사용)
- [ ] **커스텀 예외 정의** (LockAcquisitionException)
- [ ] **비즈니스 로직 없음**
- [ ] **DB 접근 없음**
- [ ] **@Transactional 없음**

---

## 📖 관련 문서

- **[Distributed Lock 가이드](distributed-lock-guide.md)** - 분산락 전략
- **[Lock Adapter 테스트](lock-adapter-test-guide.md)** - 테스트 가이드
- **[Lock Adapter ArchUnit](lock-adapter-archunit.md)** - ArchUnit 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
