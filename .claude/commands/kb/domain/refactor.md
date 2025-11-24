# Domain Layer TDD Refactor - Improve Structure

You are in the **♻️ REFACTOR phase** of Kent Beck's TDD cycle for **Domain Layer**.

## Tidy First Principle

**CRITICAL**: Refactoring = **Structural Changes ONLY** (동작 변경 없음!)

- ✅ **DO**: Rename, extract method, move code, remove duplication
- ❌ **DON'T**: Add features, change logic, modify behavior
- 🧪 **VERIFY**: Tests pass before AND after (same results)

## Instructions

1. **Test is already PASSING** (GREEN phase complete)
2. **Improve code structure** without changing behavior
3. **Apply design patterns** and best practices
4. **Ensure Zero-Tolerance compliance** (Lombok 금지, Law of Demeter, Long FK 전략)
5. **Run all tests** after each refactoring step
6. **Commit with struct: prefix**:
   ```bash
   git add .
   git commit -m "struct: Email 검증 로직 메서드 추출"
   ```
7. **IMPORTANT**: Never mix Structural and Behavioral changes!

## Refactoring Goals

- **Clarity**: Make code easier to understand
- **Maintainability**: Make code easier to change
- **DRY**: Eliminate duplication
- **Design Patterns**: Apply appropriate patterns
- **Domain Modeling**: Strengthen domain concepts

## Domain Layer Refactoring Patterns

### 1. Extract Value Objects

**Before**:
```java
public class OrderDomain {
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Lots of customer-related validation scattered
}
```

**After**:
```java
// Extract Customer Value Object
public record CustomerInfo(String name, String email, String phone) {
    public CustomerInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name cannot be blank");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        // Validation centralized in Value Object
    }
}

public class OrderDomain {
    private CustomerInfo customerInfo;
    // Clean, encapsulated
}
```

### 2. Extract Domain Events

**Before**:
```java
public void cancel(CancelReason reason) {
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();
    // Missing: event notification for other systems
}
```

**After**:
```java
public void cancel(CancelReason reason) {
    if (this.status != OrderStatus.PLACED) {
        throw new OrderCannotBeCancelledException(
            "Only PLACED orders can be cancelled. Current: " + this.status
        );
    }

    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();

    // Raise domain event
    this.registerEvent(new OrderCancelledEvent(
        this.orderId,
        this.customerId,
        reason,
        LocalDateTime.now()
    ));
}

// Domain Event
public record OrderCancelledEvent(
    OrderId orderId,
    Long customerId,
    CancelReason reason,
    LocalDateTime occurredAt
) {}
```

### 3. Extract Policy Objects

**Before**:
```java
public void cancel(CancelReason reason) {
    // Complex cancellation policy inline
    if (this.status == OrderStatus.PLACED) {
        this.status = OrderStatus.CANCELLED;
    } else if (this.status == OrderStatus.CONFIRMED) {
        this.status = OrderStatus.CANCELLED;
        this.cancellationFee = BigDecimal.valueOf(1000);
    } else if (this.status == OrderStatus.SHIPPED) {
        throw new OrderCannotBeCancelledException("Cannot cancel shipped orders");
    }
}
```

**After**:
```java
// Extract Cancellation Policy
public class OrderCancellationPolicy {

    public CancellationResult evaluate(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PLACED -> CancellationResult.allowed();
            case CONFIRMED -> CancellationResult.allowedWithFee(BigDecimal.valueOf(1000));
            case SHIPPED, DELIVERED -> CancellationResult.denied("Cannot cancel after shipping");
            case CANCELLED -> CancellationResult.denied("Order already cancelled");
            default -> CancellationResult.denied("Invalid order status");
        };
    }
}

// In OrderDomain
public void cancel(CancelReason reason) {
    CancellationResult result = cancellationPolicy.evaluate(this.status);

    if (!result.isAllowed()) {
        throw new OrderCannotBeCancelledException(result.getReason());
    }

    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    this.cancelledAt = LocalDateTime.now();

    if (result.hasFee()) {
        this.cancellationFee = result.getFee();
    }

    this.registerEvent(new OrderCancelledEvent(this.orderId, reason));
}
```

### 4. Strengthen Type Safety (Sealed Classes)

**Before**:
```java
// Enum with separate validation logic
public enum OrderStatus {
    PENDING, PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// State transitions scattered
public void confirmPayment() {
    if (this.status != OrderStatus.PENDING) {
        throw new InvalidOrderStateException("...");
    }
    this.status = OrderStatus.PLACED;
}
```

**After**:
```java
// Sealed interface for state transitions (Java 21)
public sealed interface OrderState
    permits Pending, Placed, Confirmed, Shipped, Delivered, Cancelled {

    OrderState confirmPayment();
    OrderState cancel(CancelReason reason);
    OrderState ship();
}

public record Pending() implements OrderState {
    @Override
    public OrderState confirmPayment() {
        return new Placed();
    }

    @Override
    public OrderState cancel(CancelReason reason) {
        return new Cancelled(reason);
    }

    @Override
    public OrderState ship() {
        throw new InvalidStateTransitionException("Cannot ship pending order");
    }
}

public record Placed() implements OrderState {
    @Override
    public OrderState confirmPayment() {
        throw new InvalidStateTransitionException("Already placed");
    }

    @Override
    public OrderState cancel(CancelReason reason) {
        return new Cancelled(reason);
    }

    @Override
    public OrderState ship() {
        return new Shipped();
    }
}
```

### 5. Extract Calculation Methods

**Before**:
```java
public BigDecimal getTotalPrice() {
    // Complex calculation inline
    BigDecimal basePrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    BigDecimal discountAmount = basePrice.multiply(this.discountRate);
    BigDecimal subtotal = basePrice.subtract(discountAmount);
    BigDecimal taxAmount = subtotal.multiply(this.taxRate);
    return subtotal.add(taxAmount).add(this.shippingFee);
}
```

**After**:
```java
// Extract calculation methods
public BigDecimal getTotalPrice() {
    BigDecimal subtotal = calculateSubtotal();
    BigDecimal taxAmount = calculateTax(subtotal);
    return subtotal.add(taxAmount).add(shippingFee);
}

private BigDecimal calculateSubtotal() {
    BigDecimal basePrice = calculateBasePrice();
    BigDecimal discountAmount = calculateDiscount(basePrice);
    return basePrice.subtract(discountAmount);
}

private BigDecimal calculateBasePrice() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
}

private BigDecimal calculateDiscount(BigDecimal basePrice) {
    return basePrice.multiply(discountRate);
}

private BigDecimal calculateTax(BigDecimal subtotal) {
    return subtotal.multiply(taxRate);
}
```

### 6. Strengthen Invariants

**Before**:
```java
public static OrderDomain create(...) {
    if (quantity < 1 || quantity > 100) {
        throw new InvalidQuantityException("Invalid quantity");
    }
    return new OrderDomain(...);
}
```

**After**:
```java
// Extract Quantity Value Object
public record Quantity(int value) {
    private static final int MIN = 1;
    private static final int MAX = 100;

    public Quantity {
        if (value < MIN || value > MAX) {
            throw new InvalidQuantityException(
                "Quantity must be between %d and %d, but was: %d"
                .formatted(MIN, MAX, value)
            );
        }
    }

    public Quantity increase(int amount) {
        return new Quantity(value + amount);
    }

    public Quantity decrease(int amount) {
        return new Quantity(value - amount);
    }
}

public class OrderDomain {
    private Quantity quantity;  // Type-safe, immutable, validated
}
```

## Refactoring Workflow

### Step 1: Identify Code Smells
- Long methods (>20 lines)
- Duplicated code
- Primitive obsession (String, int instead of Value Objects)
- Feature envy (method using another object's data)
- Scattered validation logic

### Step 2: Apply Refactoring
```bash
# 1. Run tests to ensure GREEN
./gradlew test

# 2. Apply ONE refactoring
# (e.g., Extract Value Object)

# 3. Run tests again
./gradlew test

# 4. If GREEN, commit
git add .
git commit -m "struct: extract CustomerInfo Value Object"

# 5. Repeat for next refactoring
```

### Step 3: Verify Zero-Tolerance Compliance

**Check 1: No Lombok**
```bash
grep -r "@Data\|@Builder\|@Getter\|@Setter" domain/src/main/java/
# Should return empty
```

**Check 2: No Getter Chaining**
```bash
grep -r "\\)\\." domain/src/main/java/ | grep "get"
# Review each match manually
```

**Check 3: No JPA Relationships**
```bash
grep -r "@ManyToOne\|@OneToMany\|@OneToOne\|@ManyToMany" domain/src/main/java/
# Should return empty
```

## Common Domain Refactorings

### 1. Replace Conditional with Polymorphism
```java
// Before
public BigDecimal calculateShippingFee() {
    if (shippingMethod.equals("STANDARD")) {
        return BigDecimal.valueOf(5.00);
    } else if (shippingMethod.equals("EXPRESS")) {
        return BigDecimal.valueOf(15.00);
    } else if (shippingMethod.equals("OVERNIGHT")) {
        return BigDecimal.valueOf(30.00);
    }
    return BigDecimal.ZERO;
}

// After
public sealed interface ShippingMethod permits Standard, Express, Overnight {
    BigDecimal calculateFee();
}

public record Standard() implements ShippingMethod {
    @Override
    public BigDecimal calculateFee() {
        return BigDecimal.valueOf(5.00);
    }
}
```

### 2. Introduce Parameter Object
```java
// Before
public static OrderDomain create(OrderId orderId, Long customerId,
                                 String customerName, String customerEmail,
                                 Long productId, String productName,
                                 Integer quantity, BigDecimal unitPrice) {
    // Too many parameters!
}

// After
public record OrderCreationParams(
    OrderId orderId,
    CustomerInfo customerInfo,
    ProductInfo productInfo,
    Quantity quantity
) {}

public static OrderDomain create(OrderCreationParams params) {
    // Clean, organized
}
```

### 3. Replace Magic Numbers with Constants
```java
// Before
if (quantity > 100) {
    throw new InvalidQuantityException("Quantity too large");
}

// After
private static final int MAX_QUANTITY = 100;

if (quantity > MAX_QUANTITY) {
    throw new InvalidQuantityException(
        "Quantity exceeds maximum allowed: " + MAX_QUANTITY
    );
}
```

## Success Criteria

- ✅ All tests still PASS after refactoring
- ✅ Code is more readable and maintainable
- ✅ Duplication eliminated
- ✅ Design patterns applied appropriately
- ✅ Zero-Tolerance rules maintained (Lombok 금지, Law of Demeter, Long FK)
- ✅ Value Objects extracted for domain concepts
- ✅ Business logic well-encapsulated

## What NOT to Do

- ❌ Don't change behavior (tests should still pass)
- ❌ Don't refactor without tests passing first
- ❌ Don't introduce Lombok during refactoring
- ❌ Don't add JPA relationship annotations
- ❌ Don't violate Law of Demeter
- ❌ Don't over-engineer (YAGNI)

This is Kent Beck's TDD: After tests pass, REFACTOR to improve structure while keeping tests GREEN.
