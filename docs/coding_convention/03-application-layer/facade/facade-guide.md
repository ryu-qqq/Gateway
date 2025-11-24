# Facade Guide — **여러 Transaction Manager 조합**

> Facade는 **여러 Transaction Manager**를 조합하여 복잡한 트랜잭션 흐름을 관리합니다.
>
> **UseCase에서 호출**하며, **하나의 트랜잭션**으로 여러 Manager를 묶습니다.

---

## 1) 핵심 역할

* **여러 Manager 조합**: 2개 이상의 Transaction Manager를 조합
* **UseCase에서 호출**: Controller가 아닌 UseCase(Port-In 구현체)에서 사용
* **트랜잭션 조율**: 여러 Manager를 하나의 트랜잭션으로
* **복잡한 흐름 관리**: Manager 호출 순서 및 조합 관리
* **비즈니스 로직 없음**: Domain Layer 책임

---

## 2) 패키지 구조

```
application/{bc}/
├─ assembler/
├─ dto/
├─ port/
│  ├─ in/
│  │  └─ command/
│  │     ├─ CreateOrderUseCase.java
│  │     ├─ DecreaseStockUseCase.java
│  │     └─ SendEmailUseCase.java
│  └─ out/
├─ manager/
│  ├─ OrderTransactionManager.java
│  └─ OutboxTransactionManager.java
└─ facade/                    ← Facade 위치
   └─ OrderFacade.java
```

---

## 3) Facade vs Transaction Manager

| 구분 | Transaction Manager | Facade |
|------|---------------------|--------|
| **역할** | 단일 Port 트랜잭션 | 여러 Manager 조합 |
| **의존성** | Persistence Port 1개 | Manager 여러 개 |
| **트랜잭션** | `@Transactional` 필수 | `@Transactional` 필수 |
| **사용처** | UseCase에서 공통 사용 | UseCase에서 호출 |
| **네이밍** | `{Bc}TransactionManager` | `{Bc}Facade` |
| **예시** | `OrderTransactionManager` | `OrderFacade` |

---

## 4) Facade 사용 기준

### ✅ Facade가 필요한 경우

1. **여러 Manager 조합**
   - Order 저장 + Outbox 저장 (하나의 트랜잭션)
   - Product 재고 차감 + Inventory 로그 (하나의 트랜잭션)

2. **트랜잭션 조율**
   - 여러 Persistence 작업을 하나의 트랜잭션으로
   - 원자성(Atomicity)이 필요한 복합 작업

3. **논리적 그룹화**
   - 관련된 여러 Manager 작업을 묶어 UseCase에서 재사용
   - UseCase가 여러 Manager를 직접 조합하는 중복 로직 방지

### ❌ Facade가 불필요한 경우

1. **단일 Manager만 사용**
   - UseCase → Manager 직접 호출

2. **트랜잭션 불필요**
   - 단순 조회 로직

3. **Manager로 충분**
   - 단일 Port 트랜잭션만 필요

---

## 5) 예시 1: OrderFacade (여러 Manager 조합)

### 문제: Order + Outbox를 하나의 트랜잭션으로

```java
package com.ryuqq.application.order.facade;

import com.ryuqq.application.order.manager.OrderTransactionManager;
import com.ryuqq.application.outbox.manager.OutboxTransactionManager;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order Facade
 * - OrderTransactionManager + OutboxTransactionManager 조합
 * - 하나의 트랜잭션으로 묶음
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
@Transactional
public class OrderFacade {

    private final OrderTransactionManager orderManager;
    private final OutboxTransactionManager outboxManager;

    public OrderFacade(
        OrderTransactionManager orderManager,
        OutboxTransactionManager outboxManager
    ) {
        this.orderManager = orderManager;
        this.outboxManager = outboxManager;
    }

    /**
     * Order 저장 + Outbox 저장 (하나의 트랜잭션)
     */
    public Order saveOrderWithOutbox(Order order, String eventType) {
        // 1. Order 저장 (OrderTransactionManager)
        Order savedOrder = orderManager.save(order);

        // 2. Outbox 저장 (OutboxTransactionManager)
        OutboxEvent event = OutboxEvent.forNew(eventType, savedOrder.getIdValue());
        outboxManager.save(event);

        return savedOrder;
    }
}
```

### UseCase에서 사용

```java
package com.ryuqq.order.application.service;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.command.CreateOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.facade.OrderFacade;
import com.ryuqq.application.order.port.in.command.CreateOrderUseCase;
import com.ryuqq.domain.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create Order UseCase (Outbox 필요)
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderFacade orderFacade;
    private final OrderAssembler assembler;

    public CreateOrderService(
        OrderFacade orderFacade,
        OrderAssembler assembler
    ) {
        this.orderFacade = orderFacade;
        this.assembler = assembler;
    }

    @Override
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Command → Domain (Assembler)
        Order order = assembler.toDomain(command);

        // 2. Domain 비즈니스 로직
        order.place();

        // 3. Facade로 저장 (Order + Outbox)
        Order savedOrder = orderFacade.saveOrderWithOutbox(order, "OrderCreated");

        // 4. Domain → Response (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

---

## 6) Do / Don't

### ✅ Good

```java
// ✅ Good: 여러 Manager 조합
@Service
@Transactional
public class OrderFacade {
    private final OrderTransactionManager orderManager;
    private final OutboxTransactionManager outboxManager;

    public Order saveOrderWithOutbox(Order order) {
        Order saved = orderManager.save(order);
        outboxManager.save(...);
        return saved;
    }
}
```

### ❌ Bad

```java
// ❌ Bad: Facade에 비즈니스 로직
@Service
public class OrderFacade {
    public OrderResponse processOrder(CreateOrderCommand command) {
        // ❌ 비즈니스 로직은 Domain Layer에!
        if (command.amount() > 10000) {
            throw new BusinessException("Too much");
        }
        return createOrderUseCase.execute(command);
    }
}

// ❌ Bad: Facade에 DTO 변환
@Service
public class OrderFacade {
    public OrderResponse processOrder(CreateOrderRequest request) {
        // ❌ DTO 변환은 Controller 또는 Assembler에!
        CreateOrderCommand command = new CreateOrderCommand(...);
        return createOrderUseCase.execute(command);
    }
}

// ❌ Bad: 단일 Manager만 호출
@Service
public class OrderFacade {
    private final OrderTransactionManager orderManager;

    public Order saveOrder(Order order) {
        // ❌ 단일 Manager는 UseCase에서 직접 호출!
        return orderManager.save(order);
    }
}
```

---

## 7) 체크리스트

- [ ] `@Service` 어노테이션 적용
- [ ] 패키지: `application.{bc}.facade`
- [ ] 2개 이상 Manager 조합
- [ ] 비즈니스 로직 없음 (Domain Layer 책임)
- [ ] DTO 변환 없음 (Controller 또는 Assembler 책임)
- [ ] 트랜잭션 필요 시 `@Transactional` 적용
- [ ] Lombok 사용하지 않음

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
