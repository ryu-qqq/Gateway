# Domain Layer TDD Green - Implement Minimum Code

You are in the **🟢 GREEN phase** of Kent Beck's TDD cycle for **Domain Layer**.

## Instructions

1. **Test is already FAILING** (RED phase complete)
2. **Write the SIMPLEST code** to make the test pass
3. **No premature optimization** - just make it work
4. **Run the test** and verify it PASSES
5. **Commit with feat: prefix**:
   ```bash
   git add .
   git commit -m "feat: Email VO 구현 (RFC 5322 검증)"
   ```
6. **Report success** clearly

## Domain Layer Implementation Guidelines

### Core Principles
- **Minimum Code**: Write only what's needed to pass the test
- **Pure Java**: No Lombok (`@Data`, `@Getter`, `@Setter`, `@Builder` 금지)
- **Law of Demeter**: No getter chaining
- **Tell, Don't Ask**: Encapsulate business logic in Domain methods
- **Long FK Strategy**: Use `private Long customerId;` (No JPA relationships)

### Implementation Pattern

**Step 1: Start with the Domain Class**
```java
package com.company.template.domain;

import java.time.LocalDateTime;

/**
 * Order Aggregate Root.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderDomain {

    private final OrderId orderId;
    private final Long customerId;
    private final Long productId;
    private final Integer quantity;
    private OrderStatus status;
    private CancelReason cancelReason;
    private LocalDateTime cancelledAt;

    // Constructor (package-private for factory methods)
    OrderDomain(OrderId orderId, Long customerId, Long productId,
                Integer quantity, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    /**
     * Factory method to create new Order (PLACED status).
     */
    public static OrderDomain create(OrderId orderId, Long customerId,
                                     Long productId, Integer quantity) {
        validateQuantity(quantity);
        return new OrderDomain(orderId, customerId, productId,
                              quantity, OrderStatus.PLACED);
    }

    // Business methods (GREEN phase implementation)
    public void cancel(CancelReason reason) {
        if (this.status != OrderStatus.PLACED) {
            throw new OrderCannotBeCancelledException(
                "Only PLACED orders can be cancelled. Current status: " + this.status
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    // Validation methods
    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 100) {
            throw new InvalidQuantityException(
                "Quantity must be between 1 and 100, but was: " + quantity
            );
        }
    }

    // Getters (Pure Java)
    public OrderId getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public CancelReason getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
```

**Step 2: Implement Value Objects (if needed)**
```java
package com.company.template.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Order ID Value Object.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderId {

    private final String value;

    public OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId cannot be null or blank");
        }
        this.value = value;
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderId{" + value + '}';
    }
}
```

**Step 3: Implement Enums**
```java
package com.company.template.domain;

/**
 * Order Status Enum.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public enum OrderStatus {
    PENDING,    // 결제 대기
    PLACED,     // 주문 완료
    CONFIRMED,  // 주문 승인
    SHIPPED,    // 배송 중
    DELIVERED,  // 배송 완료
    CANCELLED   // 주문 취소
}
```

**Step 4: Implement Domain Exceptions**
```java
package com.company.template.domain;

/**
 * Exception thrown when order cannot be cancelled.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderCannotBeCancelledException extends RuntimeException {

    public OrderCannotBeCancelledException(String message) {
        super(message);
    }
}
```

### GREEN Phase Workflow

**Step 1: Focus on the Failing Test**
```java
// Test from RED phase
@Test
@DisplayName("주문 취소 - PLACED 상태만 가능")
void shouldCancelOrderWhenPlaced() {
    // Given
    OrderDomain order = OrderDomainFixture.create();

    // When
    order.cancel(CancelReason.CUSTOMER_REQUEST);

    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order.getCancelReason()).isEqualTo(CancelReason.CUSTOMER_REQUEST);
}
```

**Step 2: Write Minimum Code**
```java
// Simplest implementation to pass the test
public void cancel(CancelReason reason) {
    if (this.status != OrderStatus.PLACED) {
        throw new OrderCannotBeCancelledException(
            "Only PLACED orders can be cancelled. Current status: " + this.status
        );
    }
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();
}
```

**Step 3: Run the Test**
```bash
./gradlew test --tests "*OrderDomainTest.shouldCancelOrderWhenPlaced"
```

**Step 4: Verify GREEN**
```
✅ Test PASSED
```

## Domain-Specific Implementation Patterns

### 1. Factory Methods (Static Creation)
```java
// ✅ CORRECT (Factory method)
public static OrderDomain create(OrderId orderId, Long customerId,
                                 Long productId, Integer quantity) {
    validateQuantity(quantity);
    return new OrderDomain(orderId, customerId, productId,
                          quantity, OrderStatus.PLACED);
}

// ❌ WRONG (Public constructor)
public OrderDomain(OrderId orderId, Long customerId,
                   Long productId, Integer quantity) {
    // Direct instantiation - harder to control
}
```

### 2. Validation Methods (Private)
```java
// ✅ CORRECT (Private validation)
private static void validateQuantity(Integer quantity) {
    if (quantity == null || quantity < 1 || quantity > 100) {
        throw new InvalidQuantityException(
            "Quantity must be between 1 and 100, but was: " + quantity
        );
    }
}
```

### 3. Business Methods (Public)
```java
// ✅ CORRECT (Tell, Don't Ask)
public void cancel(CancelReason reason) {
    if (this.status != OrderStatus.PLACED) {
        throw new OrderCannotBeCancelledException(
            "Only PLACED orders can be cancelled. Current status: " + this.status
        );
    }
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();
}

// ❌ WRONG (Ask, Then Tell - External logic)
// Don't do this:
if (order.getStatus() == OrderStatus.PLACED) {
    order.setStatus(OrderStatus.CANCELLED);
    order.setCancelReason(reason);
}
```

### 4. State Transitions
```java
// ✅ CORRECT (Encapsulated state transition)
public void confirmPayment() {
    if (this.status != OrderStatus.PENDING) {
        throw new InvalidOrderStateException(
            "Payment can only be confirmed for PENDING orders. Current: " + this.status
        );
    }
    this.status = OrderStatus.PLACED;
    this.placedAt = LocalDateTime.now();
}
```

### 5. Invariant Enforcement
```java
// ✅ CORRECT (Invariants enforced at creation)
public static OrderDomain create(OrderId orderId, Long customerId,
                                 Long productId, Integer quantity) {
    validateQuantity(quantity);      // Invariant: 1 <= quantity <= 100
    validateCustomerId(customerId);  // Invariant: customerId not null
    validateProductId(productId);    // Invariant: productId not null

    return new OrderDomain(orderId, customerId, productId,
                          quantity, OrderStatus.PLACED);
}
```

## Common Mistakes to Avoid

### ❌ WRONG: Using Lombok
```java
// ❌ NO Lombok in Domain
@Data
@Builder
public class OrderDomain {
    // This violates Zero-Tolerance rules!
}
```

### ❌ WRONG: Law of Demeter Violation
```java
// ❌ Getter chaining
public String getCustomerAddress() {
    return this.customer.getAddress().getStreet();
    // ^^^ Law of Demeter violation!
}

// ✅ CORRECT: Tell, Don't Ask
public String getCustomerAddressStreet() {
    return this.customerAddressStreet; // Stored as Long FK
}
```

### ❌ WRONG: JPA Relationship Annotations
```java
// ❌ NO JPA relationships
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;

// ✅ CORRECT: Long FK Strategy
private Long customerId;
```

### ❌ WRONG: Premature Optimization
```java
// ❌ Adding features not required by test
public void cancel(CancelReason reason) {
    // Only add cancellation fee if test requires it!
    if (this.status == OrderStatus.CONFIRMED) {
        this.cancellationFee = BigDecimal.valueOf(1000);
    }
    this.status = OrderStatus.CANCELLED;
}
```

## Success Criteria

- ✅ Test runs and PASSES
- ✅ Minimum code written (no extra features)
- ✅ Pure Java (no Lombok)
- ✅ Law of Demeter followed (no getter chaining)
- ✅ Long FK Strategy used (no JPA relationships)
- ✅ Business logic encapsulated in Domain methods
- ✅ All invariants validated at creation

## What NOT to Do

- ❌ Don't write more code than needed to pass the test
- ❌ Don't add "nice to have" features
- ❌ Don't refactor yet (that's the next phase!)
- ❌ Don't use Lombok
- ❌ Don't violate Law of Demeter
- ❌ Don't use JPA relationship annotations

This is Kent Beck's TDD: Write the SIMPLEST code to pass the test, then REFACTOR.
