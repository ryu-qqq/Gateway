# Facade Guide — **여러 Transaction Manager 조합**

> Facade는 **여러 Transaction Manager**를 조합하여 복잡한 영속화 흐름을 관리합니다.
>
> **UseCase에서 호출**하며, **하나의 트랜잭션**으로 여러 Manager를 묶습니다.

---

## 1) 핵심 역할

* **여러 Manager 조합**: 2개 이상의 Transaction Manager를 조합 (필수 조건)
* **UseCase에서 호출**: Controller가 아닌 UseCase(Port-In 구현체)에서 사용
* **트랜잭션 조율**: 여러 Manager를 하나의 트랜잭션으로
* **Event Enrichment**: 영속화 후 ID를 Event에 할당
* **순수 조율만**: 비즈니스 로직 없음, 객체 생성 없음

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **2개 이상 Manager 의존** | 단일 Manager는 UseCase에서 직접 호출 |
| **`@Component` 어노테이션** | `@Service` 아님 (TransactionManager와 동일) |
| **`@Transactional` 메서드 단위** | 클래스 레벨 금지 |
| **`persist*()` 메서드 네이밍** | save, create 등 금지 |
| **객체 생성 금지** | Factory 책임 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ facade/
│  └─ command/                ← CommandFacade 위치
│     └─ {Bc}CommandFacade.java
└─ manager/
   └─ command/
      ├─ {Bc}TransactionManager.java
      └─ {OtherBc}TransactionManager.java
```

---

## 4) Facade vs Transaction Manager

| 구분 | Transaction Manager | Facade |
|------|---------------------|--------|
| **역할** | 단일 Port 트랜잭션 | 여러 Manager 조합 |
| **의존성** | Persistence Port 1개 | Manager 2개 이상 |
| **어노테이션** | `@Component` | `@Component` |
| **트랜잭션** | `@Transactional` 메서드 | `@Transactional` 메서드 |
| **메서드** | `persist()` only | `persist*()` 패턴 |
| **사용처** | UseCase, Facade | UseCase에서 호출 |
| **네이밍** | `{Bc}TransactionManager` | `{Bc}CommandFacade` |

---

## 5) Facade 사용 기준

### ✅ Facade가 필요한 경우

1. **여러 Manager 조합**
   - Order 저장 + Outbox 저장 (하나의 트랜잭션)
   - Product 재고 차감 + Inventory 로그 (하나의 트랜잭션)

2. **트랜잭션 조율**
   - 여러 Persistence 작업을 하나의 트랜잭션으로
   - 원자성(Atomicity)이 필요한 복합 작업

### ❌ Facade가 불필요한 경우

1. **단일 Manager만 사용**
   - UseCase → Manager 직접 호출

2. **Manager로 충분**
   - 단일 Port 트랜잭션만 필요

---

## 6) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.facade.command;

import com.ryuqq.application.order.manager.command.OrderTransactionManager;
import com.ryuqq.application.order.manager.command.OrderHistoryTransactionManager;
import com.ryuqq.application.outbox.manager.command.OutboxTransactionManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderHistory;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order CommandFacade
 * - 여러 TransactionManager 조합
 * - 순수 조율만 (비즈니스 로직 없음)
 */
@Component
public class OrderCommandFacade {

    private final OrderTransactionManager orderManager;
    private final OrderHistoryTransactionManager historyManager;
    private final OutboxTransactionManager outboxManager;

    public OrderCommandFacade(
        OrderTransactionManager orderManager,
        OrderHistoryTransactionManager historyManager,
        OutboxTransactionManager outboxManager
    ) {
        this.orderManager = orderManager;
        this.historyManager = historyManager;
        this.outboxManager = outboxManager;
    }

    /**
     * Order + History + Outbox 영속화
     * - 하나의 트랜잭션으로 묶음
     */
    @Transactional
    public Order persistOrderWithHistoryAndOutbox(
            Order order,
            OrderHistory history,
            OutboxEvent outboxEvent
    ) {
        // 1. Order 영속화 → ID 획득
        Order saved = orderManager.persist(order);
        OrderId assignedId = saved.id();

        // 2. History 영속화 (ID 전달)
        historyManager.persist(history.withOrderId(assignedId));

        // 3. Outbox 영속화 (ID 전달)
        outboxManager.persist(outboxEvent.withAggregateId(assignedId.value()));

        return saved;
    }
}
```

### Bundle 사용 시 (권장)

```java
/**
 * Bundle을 받아서 영속화
 * - Bundle은 Factory에서 생성
 * - Facade는 조율만
 */
@Transactional
public Order persistOrderBundle(OrderPersistBundle bundle) {
    // 1. Order 영속화 → ID 획득
    Order saved = orderManager.persist(bundle.order());
    OrderId assignedId = saved.id();

    // 2. Bundle Enrichment + 영속화
    OrderPersistBundle enriched = bundle.enrichWithId(assignedId);
    historyManager.persist(enriched.history());
    outboxManager.persist(enriched.outboxEvent());

    return saved;
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Component 어노테이션
@Component
public class OrderCommandFacade { ... }

// ✅ Good: 2개 이상 Manager 조합
@Component
public class OrderCommandFacade {
    private final OrderTransactionManager orderManager;
    private final OrderHistoryTransactionManager historyManager;
    private final OutboxTransactionManager outboxManager;
}

// ✅ Good: @Transactional 메서드 단위
@Transactional
public Order persistOrderBundle(OrderPersistBundle bundle) { ... }

// ✅ Good: persist* 메서드 네이밍
public Order persistOrderBundle(...) { ... }
public Order persistOrderWithHistory(...) { ... }
```

### ❌ Bad

```java
// ❌ Bad: @Service 어노테이션
@Service
public class OrderCommandFacade { ... }

// ❌ Bad: @Transactional 클래스 레벨
@Component
@Transactional
public class OrderCommandFacade { ... }

// ❌ Bad: 단일 Manager만 의존
@Component
public class OrderCommandFacade {
    private final OrderTransactionManager orderManager;  // 1개만
}

// ❌ Bad: save() 메서드명
public Order saveOrder(Order order) { ... }

// ❌ Bad: 객체 생성 (Factory 책임)
public Order persistOrder(CreateOrderCommand command) {
    Order order = Order.forNew(...);  // ❌
}

// ❌ Bad: 비즈니스 로직 (Domain 책임)
public Order persistOrder(Order order) {
    if (order.totalAmount().isGreaterThan(MAX)) {  // ❌
        throw new BusinessException("Too expensive");
    }
}
```

---

## 8) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] 2개 이상 TransactionManager 의존
- [ ] `@Transactional` 메서드 단위 (클래스 레벨 금지)
- [ ] `persist*()` 메서드 네이밍
- [ ] 객체 생성 없음 (Factory 책임)
- [ ] 비즈니스 로직 없음 (Domain 책임)
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.facade.command`

---

## 9) 관련 문서

- **[Application Layer Guide](../../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[Transaction Manager Guide](../../manager/command/transaction-manager-guide.md)** - 단일 Port 트랜잭션
- **[CommandFactory Guide](../../factory/command/command-factory-guide.md)** - Command → Domain, Bundle 생성
- **[Bundle Guide](../../dto/bundle/bundle-guide.md)** - Bundle 패턴

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0
