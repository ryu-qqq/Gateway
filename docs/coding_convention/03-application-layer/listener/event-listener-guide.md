# Event Listener Guide — **동기 처리, 커밋 후 실행**

> Event Listener는 **트랜잭션 커밋 후 동기적으로 실행**됩니다.
>
> 리스너 실패 시에도 **원래 작업은 롤백되지 않습니다** (이미 커밋 완료).

---

## 1) 핵심 원칙

* **동기 처리**: `@EventListener`는 같은 스레드에서 실행
* **커밋 후 실행**: `TransactionEventRegistry`가 커밋 후 발행
* **롤백 불가**: 리스너 실패해도 원래 작업(Order 저장 등) 롤백 안 됨
* **try-catch 필수**: 예외 전파 방지
* **부가 작업만**: 캐시 무효화, 로깅, 통계 등
* **중요 작업은 Outbox**: 외부 연동, 실패하면 안 되는 작업

---

## 2) 동작 원리

```
┌─────────────────────────────────────────────────────────────────┐
│ @Transactional 메서드                                           │
│                                                                 │
│   1. Order 저장                                                 │
│   2. eventRegistry.registerForPublish(event)                    │
│   3. Outbox 저장 (중요 작업용)                                   │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ ✅ 트랜잭션 커밋 (DB에 확정)                                     │
├─────────────────────────────────────────────────────────────────┤
│ afterCommit() 호출 (같은 스레드)                                 │
│   └─ eventPublisher.publishEvent(event)                         │
│        └─ @EventListener 실행                                   │
│             └─ 캐시 무효화, 통계 업데이트 등                      │
│             └─ ❌ 예외 발생해도 Order는 이미 저장됨!              │
└─────────────────────────────────────────────────────────────────┘
```

### 리스너 실패 시 영향

| 상황 | 원래 작업 (Order 저장) | 리스너 작업 |
|------|----------------------|------------|
| 리스너 성공 | ✅ 저장됨 | ✅ 완료 |
| 리스너 실패 | ✅ 저장됨 (롤백 안 됨) | ❌ 실패 (로깅) |

---

## 3) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **`@Component` 어노테이션** | Bean 등록 |
| **`@EventListener` 사용** | `@TransactionalEventListener` 불필요 |
| **try-catch 필수** | 예외 전파 방지 |
| **부가 작업만** | 캐시, 로깅, 통계 등 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 4) 패키지 구조

```
application/{bc}/
├─ listener/                      ← Listener 위치
│  └─ {Bc}EventListener.java
└─ facade/
   └─ {Bc}Facade.java             ← Event 발행처
```

---

## 5) 이벤트 처리 기준

### Listener로 처리 (부가 작업)

| 작업 | 실패 영향 | 처리 방법 |
|------|----------|----------|
| **캐시 무효화** | 낮음 (조회 시 재생성) | try-catch + 로깅 |
| **통계 업데이트** | 낮음 | try-catch + 로깅 |
| **로그 기록** | 낮음 | try-catch + 로깅 |
| **내부 알림** | 낮음 | try-catch + 로깅 |

### Outbox로 처리 (중요 작업)

| 작업 | 실패 영향 | 처리 방법 |
|------|----------|----------|
| **외부 API 호출** | 높음 | Outbox + 스케줄러 재시도 |
| **SQS/Kafka 발송** | 높음 | Outbox + 스케줄러 재시도 |
| **결제/정산** | 매우 높음 | Outbox + 스케줄러 재시도 |
| **다른 시스템 동기화** | 높음 | Outbox + 스케줄러 재시도 |

---

## 6) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.listener;

import com.ryuqq.domain.order.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Order Event Listener
 * - 커밋 후 동기 실행
 * - 부가 작업만 처리 (캐시, 통계 등)
 * - 중요 작업은 Outbox 패턴 사용
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final CacheService cacheService;
    private final MetricsService metricsService;

    public OrderEventListener(
        CacheService cacheService,
        MetricsService metricsService
    ) {
        this.cacheService = cacheService;
        this.metricsService = metricsService;
    }

    /**
     * 주문 생성 이벤트 처리
     * - 캐시 무효화
     * - 통계 업데이트
     */
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // 캐시 무효화
        evictCacheSafely(event);

        // 통계 업데이트
        updateMetricsSafely(event);
    }

    /**
     * 캐시 무효화 (실패해도 괜찮음)
     */
    private void evictCacheSafely(OrderPlacedEvent event) {
        try {
            cacheService.evict("order:" + event.orderId().value());
            cacheService.evict("customer-orders:" + event.customerId().value());
        } catch (Exception e) {
            log.warn("캐시 무효화 실패 (무시): orderId={}", event.orderId(), e);
        }
    }

    /**
     * 통계 업데이트 (실패해도 괜찮음)
     */
    private void updateMetricsSafely(OrderPlacedEvent event) {
        try {
            metricsService.incrementOrderCount();
            metricsService.addOrderAmount(event.totalAmount());
        } catch (Exception e) {
            log.warn("통계 업데이트 실패 (무시): orderId={}", event.orderId(), e);
        }
    }
}
```

### 여러 이벤트 처리

```java
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        safeExecute("OrderPlaced", event.orderId(), () -> {
            // 처리 로직
        });
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        safeExecute("OrderCancelled", event.orderId(), () -> {
            // 처리 로직
        });
    }

    @EventListener
    public void handleOrderShipped(OrderShippedEvent event) {
        safeExecute("OrderShipped", event.orderId(), () -> {
            // 처리 로직
        });
    }

    /**
     * 안전한 실행 헬퍼
     */
    private void safeExecute(String eventType, Object id, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("{} 처리 실패 (무시): id={}", eventType, id, e);
        }
    }
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @EventListener + try-catch
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    try {
        cacheService.evict("order:" + event.orderId());
    } catch (Exception e) {
        log.warn("캐시 무효화 실패", e);
    }
}

// ✅ Good: 부가 작업만 처리
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    safeExecute(() -> cacheService.evict(...));      // 캐시
    safeExecute(() -> metricsService.increment(...)); // 통계
}

// ✅ Good: 중요 작업은 Outbox로 처리
// Facade에서:
outboxManager.persist(outboxEvent);  // DB에 저장
// 스케줄러에서:
outboxScheduler.processAndSend();    // 재시도 가능
```

### ❌ Bad

```java
// ❌ Bad: try-catch 없음 (예외 전파됨)
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    externalApi.notify(event);  // 예외 발생 시 전파
}

// ❌ Bad: 중요 작업을 리스너에서 처리
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    paymentService.capture(event.orderId());  // ❌ 실패하면 안 됨!
    // → Outbox로 처리해야 함
}

// ❌ Bad: @Async 불필요한 사용
@Async
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    cacheService.evict(...);  // 캐시 무효화는 동기로 충분
}

// ❌ Bad: @TransactionalEventListener (불필요)
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handleOrderPlaced(OrderPlacedEvent event) {
    // TransactionEventRegistry가 이미 커밋 후 발행하므로 불필요
}
```

---

## 8) @EventListener vs @TransactionalEventListener

| 어노테이션 | 설명 | 사용 시 |
|-----------|------|--------|
| `@EventListener` | 이벤트 발행 즉시 실행 | **권장** (EventRegistry가 커밋 후 발행) |
| `@TransactionalEventListener` | 트랜잭션 Phase 지정 | 불필요 (이미 커밋 후 발행됨) |

**결론**: `TransactionEventRegistry`가 커밋 후 발행하므로 `@EventListener`만 사용하면 됩니다.

---

## 9) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] `@EventListener` 사용 (`@TransactionalEventListener` 불필요)
- [ ] try-catch로 예외 처리
- [ ] 로깅 추가 (실패 시)
- [ ] 부가 작업만 처리 (캐시, 통계 등)
- [ ] 중요 작업은 Outbox 패턴 사용
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.listener`

---

## 10) 관련 문서

- **[Application Layer Guide](../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[Transaction Event Registry Guide](../event/transaction-event-registry-guide.md)** - 커밋 후 Event 발행
- **[Facade Guide](../facade/facade-guide.md)** - Event 발행처

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
