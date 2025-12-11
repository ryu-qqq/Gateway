# Command Factory Guide — **Command → Domain, PersistBundle 생성**

> CommandFactory는 **Command DTO를 Domain 객체로 변환**하고 **PersistBundle을 생성**합니다.
>
> **Command Service에서 호출**하며, **비즈니스 로직 없이 변환만** 수행합니다.

---

## 1) 핵심 역할

* **Command → Domain 변환**: Application Command DTO를 Domain 객체로 변환
* **PersistBundle 생성**: 여러 Domain 객체 + Event를 하나로 묶음
* **Command Service에서 호출**: Command UseCase 구현체에서 직접 사용
* **변환만 수행**: 비즈니스 로직 없음, 조회 없음

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **`@Component` 어노테이션** | `@Service` 아님 |
| **`*CommandFactory` 접미사** | 네이밍 규칙 |
| **`create*()` 메서드 네이밍** | Domain 생성 시 |
| **`createBundle()` 메서드** | PersistBundle 생성 시 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **조회 금지** | Port 호출 금지 |
| **Lombok 금지** | 생성자 직접 작성 |
| **`@Transactional` 금지** | 트랜잭션 책임 아님 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ factory/
│  ├─ command/                       ← Command Factory 위치
│  │  └─ {Bc}CommandFactory.java
│  └─ query/
│     └─ {Bc}QueryFactory.java       ← Query Factory (별도 문서)
├─ dto/
│  ├─ command/
│  │  └─ PlaceOrderCommand.java
│  └─ bundle/
│     └─ OrderPersistBundle.java
└─ service/
   └─ command/
      └─ PlaceOrderService.java      ← Factory 사용처
```

---

## 4) CommandFactory vs QueryFactory

| 구분 | CommandFactory | QueryFactory |
|------|----------------|--------------|
| **역할** | Command → Domain | Query → Criteria |
| **입력** | Command DTO | Query DTO |
| **출력** | Domain, PersistBundle | Criteria |
| **위치** | `factory/command/` | `factory/query/` |
| **메서드** | `create*()`, `createBundle()` | `createCriteria*()` |
| **사용처** | Command Service | Query Service |

---

## 5) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.factory.command;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.command.OrderItemCommand;
import com.ryuqq.application.order.dto.bundle.OrderPersistBundle;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderItem;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.vo.Money;
import com.ryuqq.domain.order.vo.ProductId;
import com.ryuqq.domain.order.vo.Quantity;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Order Command Factory
 * - Command → Domain 변환
 * - PersistBundle 생성
 * - 비즈니스 로직 없음 (순수 변환)
 */
@Component
public class OrderCommandFactory {

    /**
     * PlaceOrderCommand → Order 변환
     */
    public Order create(PlaceOrderCommand command) {
        List<OrderItem> items = command.items().stream()
            .map(this::createOrderItem)
            .toList();

        return Order.forNew(
            new CustomerId(command.customerId()),
            items
        );
    }

    /**
     * OrderItemCommand → OrderItem 변환
     */
    private OrderItem createOrderItem(OrderItemCommand itemCommand) {
        return OrderItem.forNew(
            new ProductId(itemCommand.productId()),
            new Quantity(itemCommand.quantity()),
            new Money(itemCommand.unitPrice())
        );
    }

    /**
     * PersistBundle 생성 (Order + Outbox)
     * - 영속화에 필요한 객체들을 하나로 묶음
     * - Event는 ID 없이 생성됨 (Facade에서 Enrichment)
     */
    public OrderPersistBundle createBundle(PlaceOrderCommand command) {
        Order order = create(command);

        OutboxEvent outboxEvent = OutboxEvent.forNew(
            "Order",
            null,  // ID는 Facade에서 할당
            "OrderPlaced",
            order.toEventPayload()
        );

        return new OrderPersistBundle(order, outboxEvent);
    }
}
```

### 복잡한 변환 예시

```java
/**
 * 여러 하위 객체가 있는 경우
 */
public Order create(PlaceOrderCommand command) {
    // 1. ShippingAddress 변환
    Address shippingAddress = createAddress(command.shippingAddress());

    // 2. OrderItems 변환
    List<OrderItem> items = command.items().stream()
        .map(this::createOrderItem)
        .toList();

    // 3. PaymentInfo 변환
    PaymentInfo paymentInfo = createPaymentInfo(command.payment());

    // 4. Order 생성 (forNew 팩토리 메서드 사용)
    return Order.forNew(
        new CustomerId(command.customerId()),
        shippingAddress,
        items,
        paymentInfo
    );
}

private Address createAddress(AddressCommand cmd) {
    return new Address(
        cmd.street(),
        cmd.city(),
        cmd.zipCode(),
        cmd.country()
    );
}

private PaymentInfo createPaymentInfo(PaymentCommand cmd) {
    return new PaymentInfo(
        PaymentMethod.valueOf(cmd.method()),
        new CardNumber(cmd.cardNumber())
    );
}
```

---

## 6) PersistBundle 구조

```java
package com.ryuqq.application.order.dto.bundle;

import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;

/**
 * Order 영속화 Bundle
 * - 하나의 트랜잭션에서 영속화할 객체들
 * - enrichWithId()로 ID 할당 (Law of Demeter 준수)
 */
public record OrderPersistBundle(
    Order order,
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
            outboxEvent.withAggregateId(orderId.value())
        );
    }
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Component 어노테이션
@Component
public class OrderCommandFactory { ... }

// ✅ Good: *CommandFactory 접미사
public class OrderCommandFactory { ... }
public class ProductCommandFactory { ... }

// ✅ Good: create* 메서드 네이밍
public Order create(PlaceOrderCommand command) { ... }
public OrderPersistBundle createBundle(PlaceOrderCommand command) { ... }

// ✅ Good: 순수 변환 (비즈니스 로직 없음)
public Order create(PlaceOrderCommand command) {
    return Order.forNew(
        new CustomerId(command.customerId()),
        createItems(command.items())
    );
}

// ✅ Good: Domain.forNew() 팩토리 메서드 사용
return Order.forNew(...);
```

### ❌ Bad

```java
// ❌ Bad: @Service 어노테이션
@Service
public class OrderCommandFactory { ... }

// ❌ Bad: *DomainFactory 접미사 (구버전)
public class OrderDomainFactory { ... }  // ❌ CommandFactory 사용

// ❌ Bad: @Transactional 사용
@Component
public class OrderCommandFactory {
    @Transactional  // ❌ Factory는 트랜잭션 책임 없음
    public Order create(...) { ... }
}

// ❌ Bad: Port 호출 (조회)
@Component
public class OrderCommandFactory {
    private final ProductQueryPort productPort;  // ❌

    public Order create(PlaceOrderCommand command) {
        Product product = productPort.findById(...);  // ❌ 조회 금지
    }
}

// ❌ Bad: 비즈니스 로직 포함
public Order create(PlaceOrderCommand command) {
    if (command.totalAmount() > MAX_AMOUNT) {  // ❌ 비즈니스 로직
        throw new BusinessException("Too expensive");
    }
}

// ❌ Bad: toXxx 메서드명 (Assembler 스타일)
public Order toOrder(PlaceOrderCommand command) { ... }  // ❌
```

---

## 8) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] `*CommandFactory` 접미사
- [ ] `create*()` 메서드 네이밍
- [ ] `createBundle()` 메서드 (Bundle 필요 시)
- [ ] 비즈니스 로직 없음 (순수 변환)
- [ ] Port 호출 없음 (조회 금지)
- [ ] `@Transactional` 없음
- [ ] Domain.forNew() 팩토리 메서드 사용
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.factory.command`

---

## 9) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[QueryFactory Guide](../query/query-factory-guide.md)** - Query → Criteria
- **[Facade Guide](../../facade/command/facade-guide.md)** - 복잡한 Command 조합
- **[PersistBundle Guide](../../dto/bundle/persist-bundle-guide.md)** - Bundle 구조
- **[Assembler Guide](../../assembler/assembler-guide.md)** - Domain → Response 변환

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0
