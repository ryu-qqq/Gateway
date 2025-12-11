# Transaction Event Registry Guide — **커밋 후 Event 발행**

> TransactionEventRegistry는 **트랜잭션 커밋 후 Event를 자동 발행**합니다.
>
> **ThreadLocal 대신 `TransactionSynchronizationManager`**를 사용하여 Virtual Thread 안전합니다.

---

## 1) 핵심 역할

* **커밋 후 발행**: 트랜잭션 성공 시에만 Event 발행
* **롤백 시 미발행**: 트랜잭션 실패 시 Event 발행 안 함
* **Virtual Thread 안전**: ThreadLocal 문제 없음
* **Facade에서 호출**: `@Transactional` 메서드 내에서 등록

---

## 2) 왜 ThreadLocal을 사용하지 않는가?

### ThreadLocal 문제점

```java
// ❌ ThreadLocal 방식의 문제
public class EventHolder {
    private static final ThreadLocal<List<DomainEvent>> events = new ThreadLocal<>();

    // 문제 1: Virtual Thread에서 부모 스레드 값 공유 안 됨
    // 문제 2: 수동 clear() 필요 (누락 시 메모리 누수)
    // 문제 3: 롤백 시 수동 처리 필요
}
```

### TransactionSynchronization 장점

```java
// ✅ TransactionSynchronization 방식의 장점
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            eventPublisher.publishEvent(event);  // 커밋 성공 시에만 실행
        }
        // 롤백 시: afterCommit() 호출 안 됨 (자동 처리)
    }
);
```

| 비교 항목 | ThreadLocal | TransactionSynchronization |
|----------|-------------|---------------------------|
| **커밋 후 발행** | 수동 구현 필요 | `afterCommit()` 자동 호출 |
| **롤백 시 처리** | 수동 clear() 필요 | 자동 미호출 |
| **Virtual Thread** | 문제 발생 가능 | 안전 |
| **메모리 누수** | clear() 누락 시 발생 | 없음 |
| **복잡도** | 높음 | 낮음 |

---

## 3) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **`@Component` 어노테이션** | 공통 컴포넌트 |
| **`TransactionSynchronizationManager` 사용** | ThreadLocal 금지 |
| **`@Transactional` 내에서 호출** | 트랜잭션 컨텍스트 필요 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 4) 패키지 구조

```
application/
├─ common/
│  └─ config/                     ← TransactionEventRegistry 위치
│      └─ TransactionEventRegistry.java
└─ order/
   └─ facade/
       └─ OrderFacade.java        ← 사용처
```

---

## 5) 구현 예시

### TransactionEventRegistry

```java
package com.ryuqq.application.common.config;

import com.ryuqq.domain.common.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Transaction Event Registry
 * - 트랜잭션 커밋 후 Event 자동 발행
 * - ThreadLocal 대신 TransactionSynchronization 사용
 * - Virtual Thread 안전
 */
@Component
public class TransactionEventRegistry {

    private final ApplicationEventPublisher eventPublisher;

    public TransactionEventRegistry(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 단일 Event 등록
     * - 트랜잭션 커밋 후 발행
     * - 롤백 시 발행 안 됨
     *
     * @param event 발행할 Domain Event
     * @throws IllegalStateException 트랜잭션 컨텍스트 없을 시
     */
    public void registerForPublish(DomainEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                "Transaction synchronization is not active. " +
                "registerForPublish() must be called within @Transactional context."
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            }
        );
    }

    /**
     * 여러 Event 등록
     * - 각 Event별로 Synchronization 등록
     *
     * @param events 발행할 Domain Event 목록
     */
    public void registerAllForPublish(List<? extends DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        events.forEach(this::registerForPublish);
    }
}
```

### Facade에서 사용

```java
@Component
public class OrderFacade {

    private final OrderTransactionManager orderManager;
    private final TransactionEventRegistry eventRegistry;

    public OrderFacade(
        OrderTransactionManager orderManager,
        TransactionEventRegistry eventRegistry
    ) {
        this.orderManager = orderManager;
        this.eventRegistry = eventRegistry;
    }

    @Transactional
    public Order persistOrderBundle(OrderPersistBundle bundle) {
        // 1. Order 영속화 → ID 획득
        Order saved = orderManager.persist(bundle.order());
        OrderId orderId = saved.id();

        // 2. Bundle Enrichment (Event에 ID 할당)
        OrderPersistBundle enriched = bundle.enrichWithId(orderId);

        // 3. Event 등록 (커밋 후 발행)
        eventRegistry.registerAllForPublish(enriched.events());

        // 4. 다른 영속화 작업...
        // ...

        return saved;
        // [트랜잭션 커밋] → Event 자동 발행
    }
}
```

---

## 6) Event 발행 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│ @Transactional 메서드 시작                                       │
│                                                                  │
│   1. Domain 영속화                                               │
│   2. eventRegistry.registerForPublish(event)                     │
│      └─ TransactionSynchronization 등록 (발행 대기)              │
│   3. 추가 영속화 작업...                                         │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│ 트랜잭션 종료 시점                                               │
│                                                                  │
│   ✅ 커밋 성공 시:                                               │
│      └─ afterCommit() 호출 → eventPublisher.publishEvent(event)  │
│                                                                  │
│   ❌ 롤백 시:                                                    │
│      └─ afterCommit() 호출 안 됨 → Event 발행 안 됨              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7) Listener에서 Event 수신

```java
package com.ryuqq.application.order.listener;

import com.ryuqq.domain.order.event.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class OrderEventListener {

    /**
     * 동기 처리 (같은 스레드)
     * - 발행 즉시 실행
     * - 트랜잭션 커밋 후 실행됨 (EventRegistry 덕분)
     */
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // 동기 처리 로직
    }

    /**
     * 비동기 처리 (별도 스레드)
     * - 발행 후 별도 스레드에서 실행
     * - 외부 API 호출 등 오래 걸리는 작업에 적합
     */
    @Async
    @EventListener
    public void handleOrderPlacedAsync(OrderPlacedEvent event) {
        // 비동기 처리 로직 (외부 API 호출 등)
    }
}
```

### @TransactionalEventListener vs @EventListener

| 어노테이션 | 실행 시점 | 사용 시 |
|-----------|----------|--------|
| `@EventListener` | Event 발행 즉시 | EventRegistry가 커밋 후 발행하므로 충분 |
| `@TransactionalEventListener` | 지정된 트랜잭션 Phase | 특수한 Phase 제어 필요 시 |

**권장**: `@EventListener` 사용 (EventRegistry가 이미 커밋 후 발행 보장)

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: @Transactional 내에서 호출
@Transactional
public Order persistOrder(OrderPersistBundle bundle) {
    Order saved = orderManager.persist(bundle.order());
    eventRegistry.registerForPublish(event);  // ✅ 트랜잭션 내
    return saved;
}

// ✅ Good: 영속화 후 Event 등록
Order saved = orderManager.persist(order);
OrderId orderId = saved.id();
DomainEvent enrichedEvent = event.withOrderId(orderId);  // ID 할당
eventRegistry.registerForPublish(enrichedEvent);

// ✅ Good: 여러 Event 등록
eventRegistry.registerAllForPublish(bundle.events());
```

### ❌ Bad

```java
// ❌ Bad: @Transactional 없이 호출
public Order persistOrder(Order order) {
    Order saved = orderManager.persist(order);
    eventRegistry.registerForPublish(event);  // ❌ IllegalStateException
    return saved;
}

// ❌ Bad: ThreadLocal 사용
public class EventHolder {
    private static final ThreadLocal<List<DomainEvent>> events
        = new ThreadLocal<>();  // ❌
}

// ❌ Bad: 트랜잭션 밖에서 직접 발행
eventPublisher.publishEvent(event);  // ❌ 롤백 시에도 발행됨

// ❌ Bad: ID 없이 Event 등록
Order order = factory.create(command);
eventRegistry.registerForPublish(order.domainEvents());  // ❌ ID 없음
// 영속화 후 enrichment 필요
```

---

## 9) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] `TransactionSynchronizationManager` 사용
- [ ] `@Transactional` 내에서만 호출
- [ ] 영속화 후 ID Enrichment 완료 후 등록
- [ ] ThreadLocal 사용 안 함
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.common.config`

---

## 10) 관련 문서

- **[Application Layer Guide](../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[Facade Guide](../facade/facade-guide.md)** - EventRegistry 사용처
- **[Bundle Guide](../dto/bundle/bundle-guide.md)** - Event Enrichment 패턴
- **[Domain Event Guide](../../02-domain-layer/event/event-guide.md)** - Domain Event 정의 (TBD)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
