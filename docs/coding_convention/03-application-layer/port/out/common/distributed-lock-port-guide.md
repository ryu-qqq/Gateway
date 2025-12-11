# DistributedLockPort 가이드

> Application Layer의 Cross-cutting 분산락 포트 인터페이스 가이드

## 1. 개요

### 1.1 위치 및 역할

```
application/
└── common/
    └── port/out/
        └── DistributedLockPort.java    # Cross-cutting concern
```

**특징:**
- **Cross-cutting concern**: 모든 Bounded Context에서 공유
- **인프라 추상화**: Redisson 구현체로부터 분리
- **타입 안전성**: `LockKey` VO 사용

### 1.2 CommandPort/QueryPort와의 차이

| 구분 | CommandPort/QueryPort | DistributedLockPort |
|------|----------------------|---------------------|
| 위치 | `{BC}/port/out/` | `common/port/out/` |
| 범위 | Bounded Context 내부 | 전체 애플리케이션 공유 |
| 용도 | 도메인 데이터 접근 | 동시성 제어 |

## 2. 인터페이스 정의

```java
package com.ryuqq.application.common.port.out;

import com.ryuqq.domain.common.vo.LockKey;
import java.util.concurrent.TimeUnit;

public interface DistributedLockPort {

    /**
     * 분산락 획득 시도
     *
     * @param key       Lock 키 (도메인 특화 LockKey 구현체)
     * @param waitTime  최대 대기 시간
     * @param leaseTime Lock 유지 시간
     * @param unit      시간 단위
     * @return Lock 획득 성공 여부
     */
    boolean tryLock(LockKey key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 분산락 해제
     */
    void unlock(LockKey key);

    /**
     * 현재 스레드가 Lock을 보유 중인지 확인
     */
    boolean isHeldByCurrentThread(LockKey key);

    /**
     * Lock 상태 확인 (어떤 스레드든)
     */
    boolean isLocked(LockKey key);
}
```

## 3. LockKey VO 패턴

### 3.1 기반 인터페이스

```java
// domain/common/vo/LockKey.java
public interface LockKey {

    /**
     * Redis Lock Key 값 반환
     * 형식: lock:{domain}:{entity}:{id}
     */
    String value();
}
```

### 3.2 도메인 특화 구현

각 Bounded Context에서 자신만의 LockKey를 구현합니다.

```java
// domain/order/vo/OrderLockKey.java
public record OrderLockKey(Long orderId) implements LockKey {

    private static final String PREFIX = "lock:order:";

    public OrderLockKey {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + orderId;
    }
}
```

```java
// domain/stock/vo/StockLockKey.java
public record StockLockKey(Long productId) implements LockKey {

    private static final String PREFIX = "lock:stock:item:";

    public StockLockKey {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + productId;
    }
}
```

```java
// 복합 키 예시
public record StockWarehouseLockKey(
    Long productId,
    Long warehouseId
) implements LockKey {

    @Override
    public String value() {
        return "lock:stock:" + productId + ":warehouse:" + warehouseId;
    }
}
```

## 4. UseCase에서 사용

### 4.1 기본 패턴

```java
@Component
public class PlaceOrderUseCase {

    private final DistributedLockPort lockPort;
    private final OrderCommandPort orderCommandPort;

    public void execute(Long orderId) {
        OrderLockKey lockKey = new OrderLockKey(orderId);

        boolean acquired = lockPort.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);

        if (!acquired) {
            throw new LockAcquisitionFailedException("주문 락 획득 실패: " + orderId);
        }

        try {
            // 비즈니스 로직
            orderCommandPort.save(order);
        } finally {
            lockPort.unlock(lockKey);
        }
    }
}
```

### 4.2 권장 파라미터

| 작업 유형 | waitTime | leaseTime | 예시 |
|----------|----------|-----------|------|
| 빠른 작업 | 5초 | 10초 | 재고 차감 |
| 일반 작업 | 10초 | 30초 | 주문 생성 |
| 무거운 작업 | 30초 | 60초 | 대량 데이터 처리 |

## 5. 금지 사항

### 5.1 Adapter에서 금지

```java
// ❌ 금지: @Transactional
@Component
@Transactional  // 금지!
public class DistributedLockAdapter implements DistributedLockPort {
    // Lock은 트랜잭션과 별개로 동작
}

// ❌ 금지: 비즈니스 로직
public boolean tryLock(LockKey key, ...) {
    if (isVipOrder(key)) {  // 금지! 비즈니스 로직
        // ...
    }
}

// ❌ 금지: DB 접근
public boolean tryLock(LockKey key, ...) {
    orderRepository.findById(...);  // 금지! DB 접근
}
```

### 5.2 UseCase에서 금지

```java
// ❌ 금지: Lock 안에서 외부 API 호출
lockPort.tryLock(lockKey, ...);
try {
    externalApiClient.call();  // 위험! Lock 시간 초과 가능
} finally {
    lockPort.unlock(lockKey);
}

// ✅ 권장: 외부 호출 후 Lock
ExternalResult result = externalApiClient.call();  // Lock 밖에서
lockPort.tryLock(lockKey, ...);
try {
    processWithResult(result);
} finally {
    lockPort.unlock(lockKey);
}
```

## 6. 키 네이밍 컨벤션

### 6.1 형식

```
lock:{domain}:{id}
lock:{domain}:{entity}:{id}
lock:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
```

### 6.2 예시

| 도메인 | 키 형식 | 예시 |
|--------|---------|------|
| 주문 | `lock:order:{orderId}` | `lock:order:123` |
| 재고 | `lock:stock:item:{productId}` | `lock:stock:item:456` |
| 결제 | `lock:payment:{paymentId}` | `lock:payment:789` |
| 재고+창고 | `lock:stock:{productId}:warehouse:{warehouseId}` | `lock:stock:456:warehouse:1` |

## 7. DomainEvent와의 유사성

| 항목 | DomainEvent | LockKey |
|------|-------------|---------|
| 기반 | `interface DomainEvent` | `interface LockKey` |
| 위치 | `domain/common/event/` | `domain/common/vo/` |
| 구현체 | `OrderPlacedEvent` | `OrderLockKey` |
| 구현체 위치 | `domain/{bc}/event/` | `domain/{bc}/vo/` |
| 패턴 | 상속 | 상속 |

## 8. 체크리스트

### Port 구현 시

- [ ] `common/port/out/` 위치 확인
- [ ] `LockKey` VO 사용 (String 금지)
- [ ] `@Transactional` 없음

### LockKey 구현 시

- [ ] `domain/{bc}/vo/` 위치
- [ ] `record` 사용 (불변성)
- [ ] compact constructor에서 검증
- [ ] 키 형식 규칙 준수

### UseCase 사용 시

- [ ] try-finally로 unlock 보장
- [ ] 적절한 waitTime/leaseTime 설정
- [ ] Lock 내부에서 외부 API 호출 금지
- [ ] 획득 실패 시 예외 처리
