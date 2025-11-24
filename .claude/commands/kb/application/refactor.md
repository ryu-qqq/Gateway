# Application Layer TDD Refactor - Improve Structure

You are in the ♻️ REFACTOR phase of Kent Beck's TDD cycle for **Application Layer**.

## Tidy First Principle

**CRITICAL**: Refactoring = **Structural Changes ONLY** (동작 변경 없음!)

- ✅ **DO**: Extract method, rename variables, remove duplication, improve clarity
- ❌ **DON'T**: Add features, change logic, modify behavior
- 🧪 **VERIFY**: Tests pass before AND after (same results)

**Kent Beck's Rule**: "Separate Structural Changes from Behavioral Changes!"

## Instructions

1. **Test is already PASSING** (GREEN phase complete)
2. **Improve code structure** without changing behavior (Structural Changes only!)
3. **Apply design patterns** and best practices
4. **Ensure Zero-Tolerance compliance** (Transaction 경계, Spring Proxy 제약)
5. **Run all tests** after each refactoring step
6. **Commit with struct: prefix**:
   ```bash
   git add .
   git commit -m "struct: extract validation logic in PlaceOrderUseCase"
   ```
7. **IMPORTANT**: Never mix Structural and Behavioral changes!

## Refactoring Goals

- **Clarity**: Make code easier to understand
- **Maintainability**: Make code easier to change
- **Transaction Safety**: Ensure correct transaction boundaries
- **CQRS Compliance**: Maintain Command/Query separation
- **Assembler Delegation**: Proper DTO conversion patterns

## Application Layer Refactoring Patterns

### 1. Extract Validation to Separate Method

**Before**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    if (command.customerId() == null) {
        throw new IllegalArgumentException("customerId cannot be null");
    }
    if (command.quantity() < 1 || command.quantity() > 100) {
        throw new IllegalArgumentException("Invalid quantity");
    }

    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(...));

    // ...
}
```

**After**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    validateCommand(command);  // Extracted

    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(...));

    // ...
}

private void validateCommand(PlaceOrderCommand command) {
    if (command.customerId() == null) {
        throw new IllegalArgumentException("customerId cannot be null");
    }
    if (command.quantity() < 1 || command.quantity() > 100) {
        throw new InvalidQuantityException(
            "Quantity must be between 1 and 100, but was: " + command.quantity()
        );
    }
}
```

### 2. Extract Transaction Logic (외부 API 분리)

**Before**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // 트랜잭션 내부
    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(...));

    OrderDomain order = OrderDomain.create(...);
    OrderDomain savedOrder = saveOrderPort.save(order);

    // ❌ 외부 API 호출 (트랜잭션 내부에서 호출하면 안 됨!)
    PaymentResult paymentResult = paymentClient.requestPayment(...);

    if (!paymentResult.isSuccess()) {
        throw new PaymentFailedException(...);
    }

    return OrderAssembler.toResponse(savedOrder);
}
```

**After**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // 트랜잭션 내부 (DB 작업만)
    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(...));

    OrderDomain order = OrderDomain.create(...);
    OrderDomain savedOrder = saveOrderPort.save(order);

    // 트랜잭션 외부로 위임
    return executeExternalOperations(savedOrder);
}

// 트랜잭션 밖에서 외부 API 호출
private OrderResponse executeExternalOperations(OrderDomain order) {
    PaymentResult paymentResult = paymentClient.requestPayment(
        order.getOrderId(),
        order.getTotalPrice()
    );

    if (!paymentResult.isSuccess()) {
        throw new PaymentFailedException(paymentResult.getReason());
    }

    return OrderAssembler.toResponse(order);
}
```

### 3. Extract Assembler Logic

**Before**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    OrderDomain savedOrder = saveOrderPort.save(order);

    // UseCase에서 직접 변환 (책임 분리 위반)
    return new OrderResponse(
        savedOrder.getOrderId().getValue(),
        savedOrder.getCustomerId(),
        savedOrder.getStatus(),
        savedOrder.getCreatedAt()
    );
}
```

**After**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    OrderDomain savedOrder = saveOrderPort.save(order);

    // Assembler에 위임 (책임 분리)
    return OrderAssembler.toResponse(savedOrder);
}

// OrderAssembler.java
public class OrderAssembler {
    public static OrderResponse toResponse(OrderDomain order) {
        return new OrderResponse(
            order.getOrderId().getValue(),
            order.getCustomerId(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
```

### 4. Split Complex UseCase (CQRS 강화)

**Before**:
```java
// Command + Query 혼합 (CQRS 위반)
@Transactional
public OrderListResponse placeOrderAndGetHistory(PlaceOrderCommand command) {
    // Command 로직
    OrderDomain order = OrderDomain.create(...);
    OrderDomain savedOrder = saveOrderPort.save(order);

    // Query 로직 (혼합!)
    List<OrderDomain> allOrders = loadOrderPort.findAllByCustomerId(
        command.customerId()
    );

    return OrderAssembler.toListResponse(allOrders);
}
```

**After**:
```java
// Command UseCase (쓰기 전용)
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    @Override
    @Transactional
    public OrderResponse execute(PlaceOrderCommand command) {
        OrderDomain order = OrderDomain.create(...);
        OrderDomain savedOrder = saveOrderPort.save(order);
        return OrderAssembler.toResponse(savedOrder);
    }
}

// Query UseCase (읽기 전용)
@UseCase
@RequiredArgsConstructor
public class FindOrderHistoryUseCase implements FindOrderHistoryPort {

    @Override
    @Transactional(readOnly = true)
    public OrderListResponse execute(Long customerId) {
        List<OrderDomain> orders = loadOrderPort.findAllByCustomerId(customerId);
        return OrderAssembler.toListResponse(orders);
    }
}
```

### 5. Extract Policy Objects

**Before**:
```java
@Transactional
public OrderResponse execute(CancelOrderCommand command) {
    OrderDomain order = loadOrderPort.loadById(command.orderId())
        .orElseThrow(() -> new OrderNotFoundException(...));

    // Complex cancellation policy inline
    if (order.getStatus() == OrderStatus.PLACED) {
        order.cancel(command.reason());
    } else if (order.getStatus() == OrderStatus.CONFIRMED) {
        order.cancel(command.reason());
        order.applyCancellationFee(BigDecimal.valueOf(1000));
    } else if (order.getStatus() == OrderStatus.SHIPPED) {
        throw new OrderCannotBeCancelledException("Cannot cancel shipped order");
    }

    OrderDomain cancelledOrder = saveOrderPort.save(order);
    return OrderAssembler.toResponse(cancelledOrder);
}
```

**After**:
```java
@Transactional
public OrderResponse execute(CancelOrderCommand command) {
    OrderDomain order = loadOrderPort.loadById(command.orderId())
        .orElseThrow(() -> new OrderNotFoundException(...));

    // Policy 위임
    orderCancellationPolicy.validateAndApply(order, command.reason());

    OrderDomain cancelledOrder = saveOrderPort.save(order);
    return OrderAssembler.toResponse(cancelledOrder);
}

// OrderCancellationPolicy.java
@Component
public class OrderCancellationPolicy {

    public void validateAndApply(OrderDomain order, CancelReason reason) {
        CancellationResult result = evaluate(order.getStatus());

        if (!result.isAllowed()) {
            throw new OrderCannotBeCancelledException(result.getReason());
        }

        order.cancel(reason);

        if (result.hasFee()) {
            order.applyCancellationFee(result.getFee());
        }
    }

    private CancellationResult evaluate(OrderStatus status) {
        return switch (status) {
            case PLACED -> CancellationResult.allowed();
            case CONFIRMED -> CancellationResult.allowedWithFee(BigDecimal.valueOf(1000));
            case SHIPPED, DELIVERED -> CancellationResult.denied("Cannot cancel after shipping");
            default -> CancellationResult.denied("Invalid order status");
        };
    }
}
```

### 6. Extract Exception Handling

**Before**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + command.customerId()));

    ProductDomain product = loadProductPort.loadById(command.productId())
        .orElseThrow(() -> new ProductNotFoundException("Product not found: " + command.productId()));

    // ...
}
```

**After**:
```java
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    CustomerDomain customer = loadCustomer(command.customerId());
    ProductDomain product = loadProduct(command.productId());
    // ...
}

private CustomerDomain loadCustomer(Long customerId) {
    return loadCustomerPort.loadById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(
            "Customer not found: " + customerId
        ));
}

private ProductDomain loadProduct(Long productId) {
    return loadProductPort.loadById(productId)
        .orElseThrow(() -> new ProductNotFoundException(
            "Product not found: " + productId
        ));
}
```

## Refactoring Workflow

### Step 1: Identify Code Smells
- Long methods (>30 lines)
- Transaction violations (외부 API 호출 in `@Transactional`)
- CQRS violations (Command + Query 혼합)
- Direct DTO conversion (Assembler 미사용)
- Spring Proxy violations (Private `@Transactional`)

### Step 2: Apply Refactoring
```bash
# 1. Run tests to ensure GREEN
./gradlew test

# 2. Apply ONE refactoring
# (e.g., Extract validation method)

# 3. Run tests again
./gradlew test

# 4. If GREEN, commit
git add .
git commit -m "struct: extract validation logic in PlaceOrderUseCase"

# 5. Repeat for next refactoring
```

### Step 3: Verify Zero-Tolerance Compliance

**Check 1: Transaction Boundaries**
```bash
# Find @Transactional methods with external API calls
grep -A 20 "@Transactional" application/src/main/java/ | grep -E "Client|HttpClient|WebClient|RestTemplate"
# Should return empty or only non-transactional methods
```

**Check 2: Spring Proxy Constraints**
```bash
# Find private/final methods with @Transactional
grep -B 5 "@Transactional" application/src/main/java/ | grep -E "private|final"
# Should return empty
```

**Check 3: CQRS Separation**
```bash
# Verify Command and Query UseCase separation
ls application/src/main/java/.../usecase/
# Should see: PlaceOrderUseCase (Command), FindOrderUseCase (Query)
```

## Common Application Refactorings

### 1. Extract Configuration to Properties
```java
// Before
private static final BigDecimal CANCELLATION_FEE = BigDecimal.valueOf(1000);
private static final int MAX_RETRY_COUNT = 3;

// After
@Component
@ConfigurationProperties(prefix = "order.cancellation")
public class OrderCancellationProperties {
    private BigDecimal fee = BigDecimal.valueOf(1000);
    private int maxRetryCount = 3;
    // Getters/Setters
}
```

### 2. Introduce Facade for Complex Orchestration
```java
// Before: Complex orchestration in UseCase
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // Load customer
    // Load product
    // Validate inventory
    // Create order
    // Apply discount
    // Calculate shipping
    // ...
}

// After: Facade pattern
@Component
@RequiredArgsConstructor
public class OrderCreationFacade {

    private final CustomerLoader customerLoader;
    private final ProductLoader productLoader;
    private final InventoryValidator inventoryValidator;
    private final DiscountCalculator discountCalculator;
    private final ShippingCalculator shippingCalculator;

    public OrderCreationContext prepare(PlaceOrderCommand command) {
        CustomerDomain customer = customerLoader.load(command.customerId());
        ProductDomain product = productLoader.load(command.productId());
        inventoryValidator.validate(product, command.quantity());

        return new OrderCreationContext(customer, product, command.quantity());
    }
}

@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    private final OrderCreationFacade facade;
    private final SaveOrderPort saveOrderPort;

    @Override
    @Transactional
    public OrderResponse execute(PlaceOrderCommand command) {
        OrderCreationContext context = facade.prepare(command);
        OrderDomain order = context.createOrder();
        OrderDomain savedOrder = saveOrderPort.save(order);
        return OrderAssembler.toResponse(savedOrder);
    }
}
```

### 3. Replace Magic Numbers with Enum
```java
// Before
if (retryCount > 3) {
    throw new MaxRetryExceededException();
}

// After
public enum RetryPolicy {
    MAX_RETRY_COUNT(3),
    RETRY_DELAY_MS(1000);

    private final int value;

    RetryPolicy(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

if (retryCount > RetryPolicy.MAX_RETRY_COUNT.getValue()) {
    throw new MaxRetryExceededException();
}
```

## Success Criteria

- ✅ All tests still PASS after refactoring
- ✅ Code is more readable and maintainable
- ✅ Transaction boundaries correct (no external API in `@Transactional`)
- ✅ Spring Proxy constraints followed (Public methods only)
- ✅ CQRS separation maintained
- ✅ Assembler delegation used
- ✅ Port naming conventions followed

## What NOT to Do

- ❌ Don't change behavior (tests should still pass)
- ❌ Don't refactor without tests passing first
- ❌ Don't call external APIs in `@Transactional` methods
- ❌ Don't use `@Transactional` on private/final methods
- ❌ Don't mix Command and Query logic
- ❌ Don't over-engineer (YAGNI)

This is Kent Beck's TDD: After tests pass, REFACTOR to improve structure while keeping tests GREEN.
