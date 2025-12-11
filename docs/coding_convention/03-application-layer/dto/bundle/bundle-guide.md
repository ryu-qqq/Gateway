# Bundle Guide — **영속화 객체 묶음 패턴**

> Bundle은 **하나의 트랜잭션에서 영속화할 여러 객체**를 묶는 Record입니다.
>
> **DomainFactory에서 생성**하고 **Facade에서 사용**합니다.

---

## 1) 핵심 역할

* **객체 묶음**: 여러 Domain 객체 + Event를 하나로 묶음
* **Law of Demeter 준수**: `enrichWithId()` 메서드로 내부 객체 수정
* **불변성 유지**: 수정 시 새 Bundle 반환
* **파라미터 간소화**: Facade 메서드 시그니처 단순화

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **Record 타입** | Class 아님 |
| **`*Bundle` 또는 `*PersistBundle` 네이밍** | 명확한 역할 표현 |
| **`enrichWithId()` 메서드** | ID 할당 후 새 Bundle 반환 |
| **불변성 유지** | 내부 객체 직접 수정 금지 |
| **비즈니스 로직 금지** | 변환만 허용 |
| **Lombok 금지** | Record 사용 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ dto/
│  └─ bundle/                     ← Bundle 위치
│      └─ {Bc}PersistBundle.java
├─ factory/
│  └─ {Bc}DomainFactory.java      ← Bundle 생성
└─ facade/
   └─ {Bc}Facade.java             ← Bundle 사용
```

---

## 4) Bundle을 사용하는 이유

### 문제: 파라미터 폭발

```java
// ❌ Bad: 파라미터가 많아짐
public Order persistOrder(
    Order order,
    OrderHistory history,
    OutboxEvent outboxEvent,
    NotificationEvent notificationEvent
) { ... }
```

### 해결: Bundle로 묶음

```java
// ✅ Good: Bundle로 파라미터 단순화
public Order persistOrderBundle(OrderPersistBundle bundle) { ... }
```

### 문제: Law of Demeter 위반

```java
// ❌ Bad: Facade에서 내부 객체 직접 접근
historyManager.persist(bundle.history().withOrderId(orderId));
//                      ↑ history()에 .withOrderId() 체이닝 = LoD 위반
```

### 해결: enrichWithId() 메서드

```java
// ✅ Good: Bundle에서 처리 (Law of Demeter 준수)
OrderPersistBundle enriched = bundle.enrichWithId(orderId);
historyManager.persist(enriched.history());
```

---

## 5) 구현 예시

### 기본 Bundle

```java
package com.ryuqq.application.order.dto.bundle;

import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;

/**
 * Order 영속화 Bundle
 * - Order + OutboxEvent 묶음
 * - enrichWithId()로 ID 할당
 */
public record OrderPersistBundle(
    Order order,
    OutboxEvent outboxEvent
) {
    /**
     * ID 할당 후 새 Bundle 반환
     * - 불변성 유지 (새 객체 생성)
     * - Law of Demeter 준수 (Facade에서 내부 접근 안 함)
     */
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        return new OrderPersistBundle(
            order,
            outboxEvent != null
                ? outboxEvent.withAggregateId(orderId.value())
                : null
        );
    }
}
```

### 여러 객체 포함 Bundle

```java
package com.ryuqq.application.order.dto.bundle;

import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderHistory;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;

import java.util.List;

/**
 * 복잡한 Order 영속화 Bundle
 * - Order + History + Multiple Events
 */
public record OrderFullPersistBundle(
    Order order,
    OrderHistory history,
    OutboxEvent outboxEvent,
    List<DomainEvent> domainEvents
) {
    /**
     * ID 할당 후 새 Bundle 반환
     * - 모든 관련 객체에 ID 전파
     */
    public OrderFullPersistBundle enrichWithId(OrderId orderId) {
        return new OrderFullPersistBundle(
            order,
            history.withOrderId(orderId),
            outboxEvent != null
                ? outboxEvent.withAggregateId(orderId.value())
                : null,
            domainEvents.stream()
                .map(e -> e.withAggregateId(orderId))
                .toList()
        );
    }

    /**
     * Domain Events 조회
     * - Facade에서 EventRegistry 등록용
     */
    public List<DomainEvent> events() {
        return domainEvents != null ? domainEvents : List.of();
    }
}
```

### Optional 객체 처리

```java
public record OrderPersistBundle(
    Order order,
    OrderHistory history,          // nullable
    OutboxEvent outboxEvent        // nullable
) {
    /**
     * ID 할당 (null-safe)
     */
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        return new OrderPersistBundle(
            order,
            history != null ? history.withOrderId(orderId) : null,
            outboxEvent != null ? outboxEvent.withAggregateId(orderId.value()) : null
        );
    }

    /**
     * History 존재 여부
     */
    public boolean hasHistory() {
        return history != null;
    }

    /**
     * OutboxEvent 존재 여부
     */
    public boolean hasOutboxEvent() {
        return outboxEvent != null;
    }
}
```

---

## 6) Facade에서 Bundle 사용

```java
@Component
public class OrderFacade {

    private final OrderTransactionManager orderManager;
    private final OrderHistoryTransactionManager historyManager;
    private final OutboxTransactionManager outboxManager;
    private final TransactionEventRegistry eventRegistry;

    // 생성자 생략...

    @Transactional
    public Order persistOrderBundle(OrderPersistBundle bundle) {
        // 1. Order 영속화 → ID 획득
        Order saved = orderManager.persist(bundle.order());
        OrderId orderId = saved.id();

        // 2. Bundle Enrichment (Law of Demeter 준수)
        OrderPersistBundle enriched = bundle.enrichWithId(orderId);

        // 3. Event 등록 (커밋 후 발행)
        enriched.events().forEach(eventRegistry::registerForPublish);

        // 4. 관련 객체 영속화
        if (enriched.hasHistory()) {
            historyManager.persist(enriched.history());
        }

        if (enriched.hasOutboxEvent()) {
            outboxManager.persist(enriched.outboxEvent());
        }

        return saved;
    }
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: Record 타입
public record OrderPersistBundle(...) { ... }

// ✅ Good: *Bundle 또는 *PersistBundle 네이밍
OrderPersistBundle
OrderFullPersistBundle
ProductSaveBundle

// ✅ Good: enrichWithId() 메서드
public OrderPersistBundle enrichWithId(OrderId orderId) {
    return new OrderPersistBundle(
        order,
        outboxEvent.withAggregateId(orderId.value())
    );
}

// ✅ Good: 불변성 유지 (새 객체 반환)
return new OrderPersistBundle(order, enrichedEvent);

// ✅ Good: null-safe 처리
outboxEvent != null ? outboxEvent.withAggregateId(...) : null
```

### ❌ Bad

```java
// ❌ Bad: Class 타입
public class OrderPersistBundle { ... }

// ❌ Bad: 비즈니스 로직 포함
public record OrderPersistBundle(...) {
    public void validate() {  // ❌
        if (order.totalAmount().isGreaterThan(MAX)) {
            throw new BusinessException("Too expensive");
        }
    }
}

// ❌ Bad: 내부 객체 직접 수정
public void setOrderId(OrderId orderId) {  // ❌ 불변성 위반
    this.outboxEvent.setAggregateId(orderId.value());
}

// ❌ Bad: enrichWithId 없이 Facade에서 직접 접근
// Facade에서:
historyManager.persist(bundle.history().withOrderId(orderId));  // ❌ LoD 위반

// ❌ Bad: 잘못된 네이밍
OrderData, OrderInfo, OrderDto  // ❌ Bundle 역할 불명확
```

---

## 8) enrichWithId() 패턴 상세

### 왜 필요한가?

1. **ID가 영속화 후에 생성됨**: Domain 객체는 ID 없이 생성
2. **여러 객체에 ID 전파 필요**: History, Event 등에 같은 ID 할당
3. **Law of Demeter 준수**: Facade에서 내부 객체 직접 접근 방지
4. **불변성 유지**: 원본 Bundle 변경 없이 새 Bundle 반환

### 구현 패턴

```java
public record OrderPersistBundle(
    Order order,
    OutboxEvent outboxEvent
) {
    /**
     * enrichWithId() 구현 규칙:
     * 1. 새 Bundle 반환 (불변성)
     * 2. null-safe 처리
     * 3. 내부 객체의 withXxx() 메서드 사용
     */
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        // 1. Order는 이미 ID 있음 (persist 후)
        // 2. OutboxEvent에 ID 할당
        return new OrderPersistBundle(
            order,  // 그대로 사용
            outboxEvent != null
                ? outboxEvent.withAggregateId(orderId.value())
                : null
        );
    }
}
```

### Domain 객체의 withXxx() 메서드

```java
// OutboxEvent
public record OutboxEvent(
    String aggregateType,
    Long aggregateId,
    String eventType,
    String payload
) {
    public OutboxEvent withAggregateId(Long id) {
        return new OutboxEvent(aggregateType, id, eventType, payload);
    }
}

// OrderHistory
public record OrderHistory(
    OrderId orderId,
    OrderStatus status,
    LocalDateTime changedAt
) {
    public OrderHistory withOrderId(OrderId id) {
        return new OrderHistory(id, status, changedAt);
    }
}
```

---

## 9) 체크리스트

- [ ] Record 타입 사용
- [ ] `*Bundle` 또는 `*PersistBundle` 네이밍
- [ ] `enrichWithId()` 메서드 구현
- [ ] 불변성 유지 (새 객체 반환)
- [ ] null-safe 처리
- [ ] 비즈니스 로직 없음
- [ ] 패키지: `application.{bc}.dto.bundle`

---

## 10) 관련 문서

- **[Application Layer Guide](../../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[Domain Factory Guide](../../factory/domain-factory-guide.md)** - Bundle 생성 담당
- **[Facade Guide](../../facade/facade-guide.md)** - Bundle 사용처

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
