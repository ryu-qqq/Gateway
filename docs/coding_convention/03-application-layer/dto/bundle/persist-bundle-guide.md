# Persist Bundle Guide — **영속화 객체 묶음**

> PersistBundle은 **여러 영속화 대상 객체를 하나로 묶어** Facade에게 전달합니다.
>
> **CommandFactory에서 생성**하고, **Facade에서 영속화**합니다.

---

## 1) 핵심 역할

* **영속화 객체 묶음**: 여러 Domain 객체 + Event를 하나로 묶음
* **CommandFactory → Facade 전달**: 중간 전달 객체
* **ID Enrichment**: 영속화 후 생성된 ID를 관련 객체에 할당
* **불변 객체**: record 사용 권장

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **record 사용** | 불변성 보장 |
| **`*PersistBundle` 접미사** | 네이밍 규칙 |
| **Domain 객체만 포함** | DTO/Response 포함 금지 |
| **`enrichWithId()` 메서드** | ID 할당 메서드 제공 |
| **비즈니스 로직 금지** | 순수 데이터 객체 |
| **Lombok 금지** | record 사용 |

---

## 3) 패키지 구조

```
application/{bc}/dto/bundle/
├─ OrderPersistBundle.java       ← Command용 (영속화) - 이 문서
├─ OrderDetailQueryBundle.java   ← Query용 (조회 결과)
└─ OrderHistoryQueryBundle.java  ← Query용 (조회 결과)
```

---

## 4) PersistBundle vs QueryBundle

| 구분 | PersistBundle | QueryBundle |
|------|---------------|-------------|
| **용도** | 영속화할 객체 묶음 | 조회된 객체 묶음 |
| **생성** | CommandFactory | QueryFacade |
| **소비** | Facade (영속화) | Assembler (Response 변환) |
| **특수 메서드** | `enrichWithId()` | 없음 (순수 데이터) |
| **접미사** | `*PersistBundle` | `*QueryBundle` |

---

## 5) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.dto.bundle;

import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderHistory;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;

/**
 * Order 영속화 Bundle
 * - CommandFactory에서 생성
 * - Facade에서 영속화
 * - enrichWithId()로 ID 할당
 */
public record OrderPersistBundle(
    Order order,
    OrderHistory history,
    OutboxEvent outboxEvent
) {
    /**
     * ID 할당 후 새 Bundle 반환
     * - 불변성 유지
     * - Law of Demeter 준수 (Facade에서 내부 객체 직접 접근 안 함)
     */
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        return new OrderPersistBundle(
            order,
            history.withOrderId(orderId),
            outboxEvent.withAggregateId(orderId.value())
        );
    }
}
```

### 다양한 예시

```java
/**
 * 단순 Bundle (Event만)
 */
public record OrderPersistBundle(
    Order order,
    OutboxEvent outboxEvent
) {
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        return new OrderPersistBundle(
            order,
            outboxEvent.withAggregateId(orderId.value())
        );
    }
}

/**
 * 여러 Event가 있는 Bundle
 */
public record OrderPersistBundle(
    Order order,
    List<OutboxEvent> outboxEvents
) {
    public OrderPersistBundle enrichWithId(OrderId orderId) {
        List<OutboxEvent> enrichedEvents = outboxEvents.stream()
            .map(event -> event.withAggregateId(orderId.value()))
            .toList();

        return new OrderPersistBundle(order, enrichedEvents);
    }
}

/**
 * Event 없는 Bundle
 */
public record ProductPersistBundle(
    Product product,
    List<ProductImage> images,
    Inventory inventory
) {
    public ProductPersistBundle enrichWithId(ProductId productId) {
        return new ProductPersistBundle(
            product,
            images.stream()
                .map(img -> img.withProductId(productId))
                .toList(),
            inventory.withProductId(productId)
        );
    }
}
```

---

## 6) Facade에서 사용

```java
@Component
public class OrderFacade {

    @Transactional
    public Order persistOrderBundle(OrderPersistBundle bundle) {
        // 1. Order 영속화 → ID 획득
        Order saved = orderManager.persist(bundle.order());
        OrderId assignedId = saved.id();

        // 2. Bundle Enrichment (ID 할당)
        OrderPersistBundle enriched = bundle.enrichWithId(assignedId);

        // 3. 나머지 객체 영속화
        historyManager.persist(enriched.history());
        outboxManager.persist(enriched.outboxEvent());

        return saved;
    }
}
```

---

## 7) CommandFactory에서 생성

```java
@Component
public class OrderCommandFactory {

    public OrderPersistBundle createBundle(PlaceOrderCommand command) {
        // 1. Order 생성
        Order order = create(command);

        // 2. History 생성
        OrderHistory history = OrderHistory.forNew();

        // 3. OutboxEvent 생성 (ID는 null)
        OutboxEvent outboxEvent = OutboxEvent.forNew(
            "Order",
            null,  // ID는 Facade에서 할당
            "OrderPlaced",
            order.toEventPayload()
        );

        // 4. Bundle로 묶어 반환
        return new OrderPersistBundle(order, history, outboxEvent);
    }
}
```

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: record 사용
public record OrderPersistBundle(
    Order order,
    OutboxEvent outboxEvent
) {}

// ✅ Good: *PersistBundle 접미사
public record OrderPersistBundle(...) {}
public record ProductPersistBundle(...) {}

// ✅ Good: enrichWithId() 메서드
public OrderPersistBundle enrichWithId(OrderId orderId) {
    return new OrderPersistBundle(
        order,
        outboxEvent.withAggregateId(orderId.value())
    );
}

// ✅ Good: 불변 컬렉션
public record OrderPersistBundle(
    Order order,
    List<OutboxEvent> events  // ✅ 불변 List
) {}
```

### ❌ Bad

```java
// ❌ Bad: class 사용 (record 사용해야 함)
public class OrderPersistBundle {
    private Order order;
    private OutboxEvent outboxEvent;
}

// ❌ Bad: *PersistBundle 접미사 누락
public record OrderBundle(...) {}  // ❌

// ❌ Bad: enrichWithId() 없음
public record OrderPersistBundle(Order order, OutboxEvent event) {
    // enrichWithId() 메서드 누락 ❌
}

// ❌ Bad: DTO/Response 포함
public record OrderPersistBundle(
    Order order,
    OrderResponse response  // ❌ Response 포함 금지
) {}

// ❌ Bad: 비즈니스 로직 포함
public record OrderPersistBundle(...) {
    public Money calculateTotal() {  // ❌ 비즈니스 로직
        return order.calculateTotal();
    }
}

// ❌ Bad: Lombok 사용
@Data  // ❌
public class OrderPersistBundle { ... }
```

---

## 9) 체크리스트

- [ ] `record` 사용 (불변성)
- [ ] `*PersistBundle` 접미사
- [ ] `enrichWithId()` 메서드 제공
- [ ] Domain 객체만 포함
- [ ] DTO/Response 포함 금지
- [ ] 비즈니스 로직 없음
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.dto.bundle`
- [ ] 불변 컬렉션 사용 (`List`, `Set`)

---

## 10) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[CommandFactory Guide](../../factory/command/command-factory-guide.md)** - Bundle 생성
- **[Facade Guide](../../facade/command/facade-guide.md)** - Bundle 영속화
- **[QueryBundle Guide](./query-bundle-guide.md)** - Query Bundle

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
