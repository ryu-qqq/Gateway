# Transaction Manager Guide — **단일 Out Port 트랜잭션 위임**

> Transaction Manager는 **단일 Out Port**의 트랜잭션 경계를 제공하고 **순수 위임**만 수행합니다.
>
> **트랜잭션은 짧게** 유지하며, `persist()` 메서드만 가집니다.
>
> 비즈니스 로직은 **UseCase/Service**에서 처리합니다.

---

## 1) 핵심 역할

* **단일 Out Port**: 하나의 Persistence Port만 의존
* **순수 위임**: Port 호출만, 비즈니스 로직 없음
* **persist() 메서드만**: 저장 위임만 담당
* **트랜잭션 경계**: `@Component` + `@Transactional`
* **Facade와 분리**: 여러 Manager 조합은 Facade 책임

---

## 2) 왜 persist()만 허용하는가?

### 문제: 비즈니스 로직 혼재
```java
// ❌ Bad: 비즈니스 메서드가 Manager에 존재
public class OutboxTransactionManager {
    public OutboxEvent markAsSent(OutboxEvent event) {
        event.markAsSent();  // ← 비즈니스 로직!
        return persistencePort.persist(event);
    }
}
```

### 해결: 순수 위임 + UseCase 분리
```java
// ✅ Good: Manager는 순수 위임만
public class OutboxTransactionManager {
    public OutboxEvent persist(OutboxEvent event) {
        return persistencePort.persist(event);
    }
}

// ✅ Good: 비즈니스 로직은 UseCase에서
@Service
@Transactional
public class MarkOutboxSentService implements MarkOutboxSentUseCase {
    private final OutboxQueryPort queryPort;
    private final OutboxTransactionManager txManager;

    @Override
    public void execute(Long eventId) {
        OutboxEvent event = queryPort.findById(eventId);
        event.markAsSent();  // ← 비즈니스 로직은 UseCase에서!
        txManager.persist(event);
    }
}
```

### 장점
1. **책임 명확**: TransactionManager = "트랜잭션 + 위임만"
2. **메서드명 통일**: Port와 동일하게 `persist()` 사용
3. **ArchUnit 검증 쉬움**: "persist() 메서드만" 검증 가능
4. **비즈니스 로직 분리**: UseCase에서 도메인 로직 처리

---

## 3) 패키지 구조

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

## 4) 기본 구조

```java
package com.ryuqq.application.{bc}.manager;

import com.ryuqq.application.{bc}.port.out.{Bc}PersistencePort;
import com.ryuqq.domain.{bc}.{Bc};
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {Bc} Transaction Manager
 * - 단일 Persistence Port만 의존
 * - persist() 메서드만 제공 (순수 위임)
 * - 비즈니스 로직 금지 (UseCase에서 처리)
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
     * {Bc} 저장 (트랜잭션 위임)
     *
     * @param {bc} 저장할 도메인 객체
     * @return 저장된 도메인 객체
     */
    public {Bc} persist({Bc} {bc}) {
        return persistencePort.persist({bc});
    }
}
```

---

## 5) 실전 예시

### OrderTransactionManager

```java
package com.ryuqq.application.order.manager;

import com.ryuqq.application.order.port.out.OrderPersistencePort;
import com.ryuqq.domain.order.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order Transaction Manager
 * - OrderPersistencePort만 의존
 * - persist() 메서드만 제공
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
     * Order 저장 (트랜잭션 위임)
     */
    public Order persist(Order order) {
        return persistencePort.persist(order);
    }
}
```

### OutboxTransactionManager

```java
package com.ryuqq.application.outbox.manager;

import com.ryuqq.application.outbox.port.out.OutboxPersistencePort;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox Transaction Manager
 * - OutboxPersistencePort만 의존
 * - persist() 메서드만 제공
 * - 상태 변경(markAsSent 등)은 UseCase에서 처리
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
     * Outbox 이벤트 저장 (트랜잭션 위임)
     */
    public OutboxEvent persist(OutboxEvent event) {
        return persistencePort.persist(event);
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
    public Order persistOrderWithOutbox(Order order, String eventType) {
        // 1. Order 저장 (OrderTransactionManager)
        Order savedOrder = orderManager.persist(order);

        // 2. Outbox 저장 (OutboxTransactionManager)
        OutboxEvent event = OutboxEvent.forNew(eventType, savedOrder.getIdValue());
        outboxManager.persist(event);

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
 * Create Order UseCase
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

        // 3. Transaction Manager로 저장 (순수 위임)
        Order savedOrder = transactionManager.persist(order);

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
        Order savedOrder = orderFacade.persistOrderWithOutbox(order, "OrderCreated");

        // 4. Domain → Response (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

### Option 3: 상태 변경 UseCase

```java
package com.ryuqq.outbox.application.service;

import com.ryuqq.application.outbox.manager.OutboxTransactionManager;
import com.ryuqq.application.outbox.port.in.command.MarkOutboxSentUseCase;
import com.ryuqq.application.outbox.port.out.OutboxQueryPort;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mark Outbox Sent UseCase
 * - 비즈니스 로직(markAsSent)은 여기서 처리
 * - TransactionManager는 persist만
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
@Transactional
public class MarkOutboxSentService implements MarkOutboxSentUseCase {

    private final OutboxQueryPort queryPort;
    private final OutboxTransactionManager transactionManager;

    public MarkOutboxSentService(
        OutboxQueryPort queryPort,
        OutboxTransactionManager transactionManager
    ) {
        this.queryPort = queryPort;
        this.transactionManager = transactionManager;
    }

    @Override
    public void execute(Long eventId) {
        // 1. 조회
        OutboxEvent event = queryPort.findById(eventId);

        // 2. 비즈니스 로직 (상태 변경)
        event.markAsSent();

        // 3. 저장 (순수 위임)
        transactionManager.persist(event);
    }
}
```

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: 단일 Persistence Port만 + persist()만
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort persistencePort;  // ← 하나만!

    public Order persist(Order order) {
        return persistencePort.persist(order);  // ← 순수 위임
    }
}

// ✅ Good: 비즈니스 로직은 UseCase에서
@Service
@Transactional
public class MarkOutboxSentService {
    private final OutboxQueryPort queryPort;
    private final OutboxTransactionManager txManager;

    public void execute(Long eventId) {
        OutboxEvent event = queryPort.findById(eventId);
        event.markAsSent();      // ← 비즈니스 로직은 여기서!
        txManager.persist(event);
    }
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
// ❌ Bad: 비즈니스 메서드 정의
@Component
@Transactional
public class OutboxTransactionManager {
    public OutboxEvent markAsSent(OutboxEvent event) {
        event.markAsSent();  // ← 비즈니스 로직! UseCase로 이동
        return persistencePort.persist(event);
    }
}

// ❌ Bad: Transaction Manager에 여러 Port 의존
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPort;
    private final OutboxPersistencePort outboxPort;  // ← Facade로!
}

// ❌ Bad: Transaction Manager에 비즈니스 로직
@Component
public class OrderTransactionManager {
    public Order persist(Order order) {
        // ❌ 비즈니스 로직은 UseCase에서!
        if (order.getAmount() > 10000) {
            throw new BusinessException("Too much");
        }
        return persistencePort.persist(order);
    }
}

// ❌ Bad: Transaction Manager에 UseCase 로직
@Component
public class OrderTransactionManager {
    public OrderResponse processOrder(CreateOrderCommand command) {
        // ❌ DTO 변환은 UseCase에서!
        Order order = assembler.toDomain(command);
        Order saved = persistencePort.persist(order);
        return assembler.toResponse(saved);
    }
}

// ❌ Bad: save() 메서드 사용 (persist()로 통일)
@Component
public class OrderTransactionManager {
    public Order save(Order order) {  // ❌ persist()로 변경
        return persistencePort.persist(order);
    }
}
```

---

## 9) Transaction Manager vs Facade

| 구분 | Transaction Manager | Facade |
|------|---------------------|--------|
| **역할** | 단일 Port 트랜잭션 위임 | 여러 Manager 조합 |
| **위치** | `manager/` | `facade/` |
| **의존성** | Persistence Port 1개 | Manager 여러 개 |
| **메서드** | `persist()` 만 | 도메인 동작 조합 |
| **트랜잭션** | `@Transactional` 필수 | `@Transactional` 필수 |
| **비즈니스 로직** | ❌ 금지 | ⚠️ 조합 로직만 |
| **네이밍** | `{Bc}TransactionManager` | `{Bc}Facade` |
| **예시** | `OrderTransactionManager` | `OrderFacade` |

---

## 10) 체크리스트

- [ ] `@Component` + `@Transactional` 적용
- [ ] 패키지: `application.{bc}.manager`
- [ ] Persistence Port 1개만 의존
- [ ] `persist()` 메서드만 정의
- [ ] 비즈니스 로직 없음 (UseCase에서 처리)
- [ ] 조합 로직 없음 (Facade에서 처리)
- [ ] Lombok 사용하지 않음
- [ ] 생성자 주입만 (필드 주입 금지)
- [ ] 메서드명: Port와 동일하게 `persist()` 사용

---

## 📖 관련 문서

- **[Transaction Manager ArchUnit](transaction-manager-archunit.md)** - ArchUnit 검증 규칙
- **[Transaction Manager Test Guide](transaction-manager-test-guide.md)** - 테스트 작성 가이드
- **[Facade Guide](../facade/facade-guide.md)** - Facade 구현 가이드
- **[UseCase Guide](../port/in/command/port-in-command-guide.md)** - UseCase 구현 가이드
- **[PersistencePort Guide](../port/out/command/port-out-command-guide.md)** - Out Port 구현 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 3.0.0 (persist() 단일 메서드 + 순수 위임)
