# Domain Event Guide

> Domain Layer의 **Domain Event** 설계 가이드
>
> 📌 **V2 컨벤션**: Aggregate 내부에서 Event 생성, `pullDomainEvents()` 패턴 사용

---

## 1) 핵심 원칙

### 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 | 예시 |
|-----|------|------|
| **DomainEvent 인터페이스 구현** | 모든 도메인 이벤트는 `DomainEvent` 인터페이스 구현 필수 | `OrderCreatedEvent implements DomainEvent` |
| **Record 타입 사용** | 불변성 보장을 위해 record 사용 필수 | `public record OrderCreatedEvent(...)` |
| **VO 타입 필드 사용** | 원시 타입 대신 Value Object 사용 | `OrderId orderId` (O), `Long orderId` (X) |
| **from() 팩토리 메서드** | Aggregate로부터 Event 생성 시 `from()` 사용 | `OrderCreatedEvent.from(order)` |
| **Event 접미사** | 클래스명은 `*Event`로 끝나야 함 | `OrderCreatedEvent`, `OrderCancelledEvent` |
| **Aggregate 내부 생성** | Event는 Aggregate 내부에서만 생성 | `order.registerEvent(...)` |

### 금지사항

| 금지 항목 | 이유 |
|----------|------|
| **Lombok 사용** | Pure Java 원칙, 명시적 코드 작성 |
| **원시 타입 필드** | 타입 안전성 부족, 도메인 의미 불명확 |
| **Setter 메서드** | 불변성 위반, record 사용으로 자동 방지 |
| **외부에서 직접 생성** | Aggregate 상태와 Event 불일치 위험 |
| **Mutable 필드** | Event는 발생 시점 상태의 스냅샷 |

---

## 2) DomainEvent 인터페이스

```java
package com.ryuqq.domain.common.event;

import java.time.Instant;

/**
 * 도메인 이벤트 마커 인터페이스
 *
 * <p>모든 도메인 이벤트는 이 인터페이스를 구현해야 합니다.</p>
 */
public interface DomainEvent {

    /**
     * 이벤트 발생 시각
     *
     * @return 이벤트가 생성된 시각
     */
    Instant occurredAt();

    /**
     * 이벤트 타입 식별자
     *
     * <p>기본 구현은 클래스 단순명을 반환합니다.</p>
     *
     * @return 이벤트 타입 문자열
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
```

---

## 3) Domain Event 구현 패턴

### 기본 구조

```java
package com.ryuqq.domain.order.event;

import com.ryuqq.domain.common.event.DomainEvent;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;
import com.ryuqq.domain.order.vo.Money;
import com.ryuqq.domain.member.vo.MemberId;

import java.time.Instant;

/**
 * 주문 생성 이벤트
 *
 * <p>주문이 성공적으로 생성되었을 때 발행됩니다.</p>
 *
 * @param orderId 주문 ID (VO)
 * @param memberId 회원 ID (VO)
 * @param totalAmount 주문 총액 (VO)
 * @param status 주문 상태 (VO/Enum)
 * @param occurredAt 이벤트 발생 시각
 */
public record OrderCreatedEvent(
    OrderId orderId,
    MemberId memberId,
    Money totalAmount,
    OrderStatus status,
    Instant occurredAt
) implements DomainEvent {

    /**
     * Aggregate로부터 Event 생성
     *
     * <p>이 메서드만 사용하여 Event를 생성해야 합니다.</p>
     * <p>Aggregate가 Clock에서 얻은 시간을 전달받아 사용합니다.</p>
     *
     * @param order 주문 Aggregate
     * @param occurredAt 이벤트 발생 시각 (Aggregate의 clock.instant()에서 전달)
     * @return 주문 생성 이벤트
     */
    public static OrderCreatedEvent from(Order order, Instant occurredAt) {
        return new OrderCreatedEvent(
            order.id(),
            order.memberId(),
            order.totalAmount(),
            order.status(),
            occurredAt
        );
    }
}
```

### 상태 변경 이벤트

```java
package com.ryuqq.domain.order.event;

import com.ryuqq.domain.common.event.DomainEvent;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;

import java.time.Instant;

/**
 * 주문 취소 이벤트
 *
 * @param orderId 주문 ID
 * @param previousStatus 이전 상태
 * @param currentStatus 현재 상태 (CANCELLED)
 * @param cancelReason 취소 사유
 * @param occurredAt 이벤트 발생 시각
 */
public record OrderCancelledEvent(
    OrderId orderId,
    OrderStatus previousStatus,
    OrderStatus currentStatus,
    String cancelReason,
    Instant occurredAt
) implements DomainEvent {

    public static OrderCancelledEvent from(Order order, OrderStatus previousStatus, String reason, Instant occurredAt) {
        return new OrderCancelledEvent(
            order.id(),
            previousStatus,
            order.status(),
            reason,
            occurredAt
        );
    }
}
```

---

## 4) Aggregate와 Event 통합

### Aggregate 내부 Event 관리

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.common.event.DomainEvent;
import com.ryuqq.domain.order.event.OrderCreatedEvent;
import com.ryuqq.domain.order.event.OrderCancelledEvent;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;
import com.ryuqq.domain.order.vo.Money;
import com.ryuqq.domain.member.vo.MemberId;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 Aggregate Root
 */
public class Order {

    private final OrderId id;
    private final MemberId memberId;
    private final Money totalAmount;
    private OrderStatus status;
    private final Clock clock;  // Clock 주입 (테스트 가능성)

    // Domain Events 내부 저장소
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Order(OrderId id, MemberId memberId, Money totalAmount, OrderStatus status, Clock clock) {
        this.id = id;
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.clock = clock;
    }

    /**
     * 신규 주문 생성 + Event 등록
     */
    public static Order forNew(MemberId memberId, Money totalAmount, Clock clock) {
        OrderId orderId = OrderId.generate();
        Order order = new Order(orderId, memberId, totalAmount, OrderStatus.CREATED, clock);

        // Aggregate 내부에서 Event 생성 및 등록 (Clock에서 시간 획득)
        Instant now = clock.instant();
        order.registerEvent(OrderCreatedEvent.from(order, now));

        return order;
    }

    /**
     * 영속화 복원 (Event 없음)
     */
    public static Order reconstitute(OrderId id, MemberId memberId, Money totalAmount, OrderStatus status, Clock clock) {
        return new Order(id, memberId, totalAmount, status, clock);
    }

    /**
     * 주문 취소 + Event 등록
     */
    public void cancel(String reason) {
        validateCancellable();

        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CANCELLED;

        // 상태 변경 후 Event 등록 (Clock에서 시간 획득)
        Instant now = clock.instant();
        registerEvent(OrderCancelledEvent.from(this, previousStatus, reason, now));
    }

    // ========== Event 관리 메서드 ==========

    /**
     * Event 등록 (내부용)
     */
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * 등록된 Event 조회 및 초기화
     *
     * <p>Application Layer에서 호출하여 Event 발행 후 초기화합니다.</p>
     *
     * @return 등록된 도메인 이벤트 목록
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // ========== 검증 메서드 ==========

    private void validateCancellable() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료된 주문은 취소할 수 없습니다.");
        }
    }

    // ========== Getter (VO 반환) ==========

    public OrderId id() { return id; }
    public MemberId memberId() { return memberId; }
    public Money totalAmount() { return totalAmount; }
    public OrderStatus status() { return status; }
}
```

### Application Layer에서 Event 발행

```java
package com.ryuqq.application.order.manager;

import com.ryuqq.application.port.out.command.OrderPersistencePort;
import com.ryuqq.domain.order.aggregate.order.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderPersistenceManager {

    private final OrderPersistencePort persistencePort;
    private final ApplicationEventPublisher eventPublisher;

    public OrderPersistenceManager(
        OrderPersistencePort persistencePort,
        ApplicationEventPublisher eventPublisher
    ) {
        this.persistencePort = persistencePort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void save(Order order) {
        // 1. 영속화
        persistencePort.save(order);

        // 2. Event 발행 (영속화 성공 후)
        order.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
```

---

## 5) Event 필드 설계 원칙

### VO 사용 필수

```java
// ✅ 올바른 예: VO 타입 사용
public record OrderCreatedEvent(
    OrderId orderId,           // VO
    MemberId memberId,         // VO
    Money totalAmount,         // VO
    OrderStatus status,        // Enum (VO 취급)
    Instant occurredAt         // Java 표준 불변 타입
) implements DomainEvent { ... }

// ❌ 잘못된 예: 원시 타입 사용
public record OrderCreatedEvent(
    Long orderId,              // 원시 타입 래퍼
    Long memberId,             // 원시 타입 래퍼
    BigDecimal totalAmount,    // 도메인 의미 불명확
    String status,             // 타입 안전성 부족
    Instant occurredAt
) implements DomainEvent { ... }
```

### 필드 선택 가이드라인

| 포함해야 할 필드 | 포함하지 말아야 할 필드 |
|----------------|---------------------|
| Aggregate ID (식별자) | 전체 Aggregate 객체 |
| 변경된 상태 값 | 변경되지 않은 값 |
| 이벤트 발생 시각 | 민감 정보 (비밀번호 등) |
| 컨텍스트에 필요한 최소 정보 | 과도한 중첩 객체 |

### 상태 변경 이벤트 패턴

```java
// 이전 상태와 현재 상태 모두 포함 (변경 추적 가능)
public record OrderStatusChangedEvent(
    OrderId orderId,
    OrderStatus previousStatus,    // 이전 상태
    OrderStatus currentStatus,     // 현재 상태
    Instant occurredAt
) implements DomainEvent {

    public static OrderStatusChangedEvent from(Order order, OrderStatus previousStatus, Instant occurredAt) {
        return new OrderStatusChangedEvent(
            order.id(),
            previousStatus,
            order.status(),
            occurredAt
        );
    }
}
```

---

## 6) 패키지 구조

```
domain/
└─ [boundedContext]/           # 예: order
   ├─ aggregate/
   │  └─ order/
   │     └─ Order.java         # Aggregate Root (Event 등록)
   │
   ├─ event/                   # Domain Event
   │  ├─ OrderCreatedEvent.java
   │  ├─ OrderCancelledEvent.java
   │  └─ OrderStatusChangedEvent.java
   │
   └─ vo/
      ├─ OrderId.java
      ├─ OrderStatus.java
      └─ Money.java
```

---

## 7) 체크리스트

### Event 생성 시 확인사항

- [ ] `DomainEvent` 인터페이스 구현
- [ ] `record` 타입 사용
- [ ] 클래스명 `*Event` 접미사
- [ ] 모든 필드 VO 타입 사용
- [ ] `from(Aggregate)` 팩토리 메서드 제공
- [ ] `occurredAt()` 필드 포함
- [ ] Aggregate 내부에서만 생성

### Aggregate 확인사항

- [ ] `List<DomainEvent> domainEvents` 필드
- [ ] `registerEvent(DomainEvent)` 메서드
- [ ] `pullDomainEvents()` 메서드
- [ ] 비즈니스 메서드 실행 후 Event 등록

---

## 8) 관련 문서

- [Aggregate Guide](../aggregate/aggregate-guide.md) - Aggregate 설계 가이드
- [Value Object Guide](../vo/vo-guide.md) - VO 설계 가이드
- [Event ArchUnit Guide](./event-archunit.md) - Event ArchUnit 검증 가이드
