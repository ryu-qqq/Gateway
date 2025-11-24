# Transaction Manager Guide — **단일 Out Port 트랜잭션 처리**

> Transaction Manager는 **단일 Out Port**의 트랜잭션 로직을 캡슐화합니다.
>
> **트랜잭션은 짧게** 유지하며, Out Port 호출 → 저장 → 완료만 담당합니다.

---

## 1) 핵심 역할

* **단일 Out Port**: 하나의 Persistence Port만 의존
* **트랜잭션 짧게 유지**: Port 호출 → 저장 → 완료
* **비즈니스 로직 없음**: Domain Layer 책임
* **조합 로직 없음**: Facade 책임
* **Bean 등록**: `@Component` + `@Transactional`

---

## 2) 패키지 구조

```
application/{bc}/
├─ assembler/
├─ dto/
├─ port/
│  ├─ in/
│  └─ out/                    ← Transaction Manager가 의존
│     ├─ OrderPersistencePort.java
│     ├─ OutboxPersistencePort.java
│     └─ ProductPersistencePort.java
├─ manager/                   ← Transaction Manager 위치
│  ├─ OrderTransactionManager.java
│  └─ OutboxTransactionManager.java
└─ facade/                    ← Facade (여러 Manager 조합)
   └─ OrderFacade.java
```

---

## 3) 기본 구조

```java
package com.ryuqq.application.{bc}.manager;

import com.ryuqq.application.{bc}.port.out.{Bc}PersistencePort;
import com.ryuqq.domain.{bc}.{Bc};
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {Bc} Transaction Manager
 * - 단일 Persistence Port만 의존
 * - 트랜잭션 짧게 유지
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@Transactional
public class {Bc}TransactionManager {

    private final {Bc}PersistencePort persistencePort;

    public {Bc}TransactionManager({Bc}PersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    /**
     * {Bc} 저장 (트랜잭션)
     */
    public {Bc} save({Bc} {bc}) {
        return persistencePort.save({bc});
    }
}
```

---

## 4) OrderTransactionManager 예시

```java
package com.ryuqq.application.order.manager;

import com.ryuqq.application.order.port.out.OrderPersistencePort;
import com.ryuqq.domain.order.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order Transaction Manager
 * - OrderPersistencePort만 의존
 * - 트랜잭션 짧게 유지
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@Transactional
public class OrderTransactionManager {

    private final OrderPersistencePort persistencePort;

    public OrderTransactionManager(OrderPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    /**
     * Order 저장 (트랜잭션)
     */
    public Order save(Order order) {
        return persistencePort.save(order);
    }
}
```

---

## 5) OutboxTransactionManager 예시

```java
package com.ryuqq.application.outbox.manager;

import com.ryuqq.application.outbox.port.out.OutboxPersistencePort;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Transaction Manager
 * - OutboxPersistencePort만 의존
 * - 트랜잭션 짧게 유지
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@Transactional
public class OutboxTransactionManager {

    private final OutboxPersistencePort persistencePort;

    public OutboxTransactionManager(OutboxPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    /**
     * Outbox 이벤트 저장 (트랜잭션)
     */
    public OutboxEvent save(OutboxEvent event) {
        return persistencePort.save(event);
    }

    /**
     * Outbox 상태 변경: Pending → Sent (트랜잭션)
     */
    public OutboxEvent markAsSent(OutboxEvent event) {
        event.markAsSent();  // Domain 메서드
        return persistencePort.save(event);
    }

    /**
     * Outbox 상태 변경: Pending → Failed (트랜잭션)
     */
    public OutboxEvent markAsFailed(OutboxEvent event, String errorMessage) {
        event.markAsFailed(errorMessage);  // Domain 메서드
        return persistencePort.save(event);
    }
}
```

---

## 6) Facade (여러 Manager 조합)

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
 * - 여러 Transaction Manager 조합
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

---

## 7) UseCase에서 사용

### Option 1: Transaction Manager 직접 사용

```java
package com.ryuqq.order.application.service;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.command.CreateOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.manager.OrderTransactionManager;
import com.ryuqq.application.order.port.in.command.CreateOrderUseCase;
import com.ryuqq.domain.order.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Create Order UseCase (Outbox 불필요)
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderTransactionManager transactionManager;
    private final OrderAssembler assembler;

    public CreateOrderService(
        OrderTransactionManager transactionManager,
        OrderAssembler assembler
    ) {
        this.transactionManager = transactionManager;
        this.assembler = assembler;
    }

    @Override
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Command → Domain (Assembler)
        Order order = assembler.toDomain(command);

        // 2. Domain 비즈니스 로직
        order.place();

        // 3. Transaction Manager로 저장 (트랜잭션 짧게)
        Order savedOrder = transactionManager.save(order);

        // 4. Domain → Response (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

### Option 2: Facade 사용

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
public class CreateOrderWithEventService implements CreateOrderUseCase {

    private final OrderFacade orderFacade;
    private final OrderAssembler assembler;

    public CreateOrderWithEventService(
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

        // 3. Facade로 저장 (Order + Outbox, 하나의 트랜잭션)
        Order savedOrder = orderFacade.saveOrderWithOutbox(order, "OrderCreated");

        // 4. Domain → Response (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: 단일 Persistence Port만
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort persistencePort;  // ← 하나만!

    public Order save(Order order) {
        return persistencePort.save(order);
    }
}

// ✅ Good: UseCase에서 Manager 사용
@Service
@Transactional
public class CreateOrderService {
    private final OrderTransactionManager transactionManager;
    private final OrderAssembler assembler;
}

// ✅ Good: Facade에서 여러 Manager 조합
@Service
@Transactional
public class OrderFacade {
    private final OrderTransactionManager orderManager;
    private final OutboxTransactionManager outboxManager;
}
```

### ❌ Bad

```java
// ❌ Bad: Transaction Manager에 여러 Port 의존
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPort;
    private final OutboxPersistencePort outboxPort;  // ← Facade로!

    public Order saveWithOutbox(Order order) {
        Order saved = orderPort.save(order);
        outboxPort.save(...);  // ← 이건 Facade 책임!
        return saved;
    }
}

// ❌ Bad: Transaction Manager에 비즈니스 로직
@Component
public class OrderTransactionManager {
    public Order save(Order order) {
        // ❌ 비즈니스 로직은 Domain Layer에!
        if (order.getAmount() > 10000) {
            throw new BusinessException("Too much");
        }
        return persistencePort.save(order);
    }
}

// ❌ Bad: Transaction Manager에 UseCase 로직
@Component
public class OrderTransactionManager {
    public OrderResponse processOrder(CreateOrderCommand command) {
        // ❌ DTO 변환은 UseCase에서!
        Order order = assembler.toDomain(command);
        Order saved = persistencePort.save(order);
        return assembler.toResponse(saved);
    }
}
```

---

## 9) Transaction Manager vs Facade

| 구분 | Transaction Manager | Facade |
|------|---------------------|--------|
| **역할** | 단일 Port 트랜잭션 | 여러 Manager 조합 |
| **위치** | `manager/` | `facade/` |
| **의존성** | Persistence Port 1개 | Manager 여러 개 |
| **트랜잭션** | `@Transactional` 필수 | `@Transactional` 필수 |
| **네이밍** | `{Bc}TransactionManager` | `{Bc}Facade` |
| **예시** | `OrderTransactionManager` | `OrderFacade` |

---

## 10) 체크리스트

- [ ] `@Component` + `@Transactional` 적용
- [ ] 패키지: `application.{bc}.manager`
- [ ] Persistence Port 1개만 의존
- [ ] 트랜잭션 짧게 유지 (저장만)
- [ ] 비즈니스 로직 없음 (Domain Layer 책임)
- [ ] 조합 로직 없음 (Facade 책임)
- [ ] Lombok 사용하지 않음

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 2.0.0 (단일 Port + Facade 분리)
