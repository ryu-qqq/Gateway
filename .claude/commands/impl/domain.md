---
description: Domain Layer Doc-Driven 구현. 신규 Aggregate, VO, Event, Exception 생성. 구현 + 테스트 동시 작성.
tags: [project]
---

# /impl domain - Domain Layer Implementation

**Doc-Driven Development**로 Domain Layer 신규 코드를 생성합니다.

## 사용법

```bash
/impl domain {feature-name}
/impl domain order-cancel
/impl domain member-register
```

## 실행 프로세스

```
/impl domain cancel-order
        ↓
┌─────────────────────────────────────────────────┐
│ 1️⃣ Plan 로드 (Serena memory)                    │
│    - read_memory("plan-{feature}")              │
│    - 비즈니스 규칙 확인                           │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 2️⃣ Domain Skill 활성화                          │
│    - domain-expert 규칙 적용                     │
│    - Zero-Tolerance 검증                        │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 3️⃣ 구현 + 테스트 동시 작성                       │
│    - Production 코드                            │
│    - Unit 테스트                                │
│    - TestFixture                               │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 4️⃣ 검증 및 커밋                                 │
│    - ./gradlew test                            │
│    - feat: 커밋 (테스트 포함)                    │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 5️⃣ Memory 업데이트                              │
│    - Plan 진행상태 업데이트                       │
└─────────────────────────────────────────────────┘
```

---

## Zero-Tolerance 규칙 (필수)

### ✅ MUST
- **Lombok 금지**: Pure Java 또는 Record 사용
- **Law of Demeter**: Getter 체이닝 금지
- **Tell, Don't Ask**: 상태 묻지 말고 행동 요청
- **Long FK 전략**: JPA 관계 어노테이션 금지

### ❌ NEVER
```java
// ❌ Lombok
@Data
@Builder
public class Order { }

// ❌ Getter 체이닝
order.getCustomer().getAddress().getCity();

// ❌ 외부 의존성
@Autowired
private OrderRepository repository;
```

---

## 생성 대상

### 1. Aggregate Root

```java
// domain/src/main/java/{basePackage}/domain/{feature}/
public class Order {
    private final OrderId id;
    private final Long customerId;  // Long FK
    private OrderStatus status;

    // Private 생성자
    private Order(OrderId id, Long customerId) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        validateInvariant();
    }

    // 정적 팩토리 메서드
    public static Order create(OrderId id, Long customerId) {
        return new Order(id, customerId);
    }

    // 비즈니스 메서드 (Tell, Don't Ask)
    public void cancel(CancelReason reason) {
        if (!this.status.isCancellable()) {
            throw new OrderCannotBeCancelledException(this.id, this.status);
        }
        this.status = OrderStatus.CANCELLED;
        // Domain Event 발행 가능
    }

    // 불변식 검증
    private void validateInvariant() {
        if (id == null) throw new IllegalArgumentException("OrderId cannot be null");
    }

    // Getter (필요한 것만)
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
}
```

### 2. Value Object (Record)

```java
// domain/src/main/java/{basePackage}/domain/{feature}/
public record CancelReason(String value) {

    public CancelReason {
        if (value == null || value.isBlank()) {
            throw new InvalidCancelReasonException("취소 사유는 필수입니다");
        }
        if (value.length() > 200) {
            throw new InvalidCancelReasonException("취소 사유는 200자 이내여야 합니다");
        }
    }

    public static CancelReason of(String value) {
        return new CancelReason(value);
    }
}
```

### 3. Domain Exception

```java
// domain/src/main/java/{basePackage}/domain/{feature}/exception/
public class OrderCannotBeCancelledException extends DomainException {

    public OrderCannotBeCancelledException(OrderId orderId, OrderStatus status) {
        super(
            OrderErrorCode.CANNOT_CANCEL,
            String.format("주문 %s은(는) %s 상태에서 취소할 수 없습니다",
                orderId.value(), status)
        );
    }
}
```

### 4. Domain Event

```java
// domain/src/main/java/{basePackage}/domain/{feature}/event/
public record OrderCancelledEvent(
    OrderId orderId,
    Long customerId,
    CancelReason reason,
    LocalDateTime cancelledAt
) implements DomainEvent {

    public static OrderCancelledEvent of(Order order, CancelReason reason) {
        return new OrderCancelledEvent(
            order.getId(),
            order.getCustomerId(),
            reason,
            LocalDateTime.now()
        );
    }
}
```

---

## 테스트 작성

### Unit Test

```java
// domain/src/test/java/{basePackage}/domain/{feature}/
class OrderTest {

    @Test
    @DisplayName("주문 취소 - PLACED 상태에서 성공")
    void shouldCancelOrderWhenPlaced() {
        // Given
        Order order = OrderFixture.createPlaced();
        CancelReason reason = CancelReasonFixture.customerRequest();

        // When
        order.cancel(reason);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 - SHIPPED 상태에서 실패")
    void shouldNotCancelOrderWhenShipped() {
        // Given
        Order order = OrderFixture.createShipped();
        CancelReason reason = CancelReasonFixture.customerRequest();

        // When & Then
        assertThatThrownBy(() -> order.cancel(reason))
            .isInstanceOf(OrderCannotBeCancelledException.class)
            .hasMessageContaining("SHIPPED");
    }
}
```

### TestFixture

```java
// domain/src/testFixtures/java/{basePackage}/domain/fixture/
public final class OrderFixture {

    private OrderFixture() {}

    public static Order create() {
        return Order.create(OrderIdFixture.create(), 1L);
    }

    public static Order createPlaced() {
        Order order = create();
        order.place();
        return order;
    }

    public static Order createShipped() {
        Order order = createPlaced();
        order.confirm();
        order.ship();
        return order;
    }
}
```

---

## 커밋 규칙

```bash
# 구현 + 테스트 함께 커밋
git commit -m "feat: Order 취소 기능 구현 (Domain Layer)

- Order.cancel() 메서드 추가
- CancelReason VO 생성
- OrderCannotBeCancelledException 추가
- Unit 테스트 추가"
```

---

## Memory 업데이트

구현 완료 후 Plan 상태 업데이트:

```python
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle="- [ ] Domain Layer",
    repl="- [x] Domain Layer (completed)",
    mode="literal"
)
```

---

## 다음 단계

Domain Layer 완료 후:

```bash
/impl application {feature}  # Application Layer 구현
```
