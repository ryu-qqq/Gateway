# Application Layer TDD Green - Implement Minimum Code

You are in the 🟢 GREEN phase of Kent Beck's TDD cycle for **Application Layer**.

## Instructions

1. **Test is already FAILING** (RED phase complete)
2. **Write the SIMPLEST code** to make the test pass
3. **No premature optimization** - just make it work
4. **Run the test** and verify it PASSES
5. **Report success** clearly
6. **Commit with feat: prefix**:
   ```bash
   git add .
   git commit -m "feat: 주문 생성 UseCase 구현 (최소 코드)"
   ```

## Application Layer Implementation Guidelines

### Core Principles
- **Minimum Code**: Write only what's needed to pass the test
- **Transaction Boundaries**: `@Transactional` 내 외부 API 호출 절대 금지
- **Spring Proxy**: Private/Final 메서드에 `@Transactional` 금지
- **CQRS Separation**: Command와 Query UseCase 명확히 분리
- **Assembler Delegation**: DTO 변환은 Assembler에 위임

### Implementation Pattern

**Step 1: Command UseCase (트랜잭션 내부)**
```java
package com.company.template.application.usecase;

import com.company.template.application.annotation.UseCase;
import com.company.template.application.port.in.command.PlaceOrderCommand;
import com.company.template.application.port.in.command.PlaceOrderPort;
import com.company.template.application.dto.response.OrderResponse;
import com.company.template.application.assembler.OrderAssembler;
import com.company.template.application.port.out.LoadCustomerPort;
import com.company.template.application.port.out.SaveOrderPort;
import com.company.template.domain.CustomerDomain;
import com.company.template.domain.OrderDomain;
import com.company.template.domain.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 UseCase.
 *
 * <p>Command Pattern: 주문 생성 비즈니스 로직을 처리합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    private final LoadCustomerPort loadCustomerPort;
    private final SaveOrderPort saveOrderPort;

    /**
     * 주문 생성 실행.
     *
     * <p>트랜잭션 내부에서 주문을 생성하고 저장합니다.</p>
     *
     * @param command 주문 생성 명령
     * @return 주문 응답 DTO
     * @throws CustomerNotFoundException 고객을 찾을 수 없는 경우
     */
    @Override
    @Transactional
    public OrderResponse execute(PlaceOrderCommand command) {
        // 1. 고객 조회 (트랜잭션 내부)
        CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(
                "Customer not found: " + command.customerId()
            ));

        // 2. 주문 생성 (Domain 비즈니스 로직)
        OrderDomain order = OrderDomain.create(
            OrderId.generate(),
            command.customerId(),
            command.productId(),
            command.quantity()
        );

        // 3. 주문 저장 (트랜잭션 내부)
        OrderDomain savedOrder = saveOrderPort.save(order);

        // 4. Response 변환 (Assembler 위임)
        return OrderAssembler.toResponse(savedOrder);
    }
}
```

**Step 2: Query UseCase (읽기 전용)**
```java
package com.company.template.application.usecase;

import com.company.template.application.annotation.UseCase;
import com.company.template.application.port.in.query.FindOrderPort;
import com.company.template.application.dto.response.OrderResponse;
import com.company.template.application.assembler.OrderAssembler;
import com.company.template.application.port.out.LoadOrderPort;
import com.company.template.domain.OrderDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 조회 UseCase.
 *
 * <p>Query Pattern: 주문 조회 로직을 처리합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@UseCase
@RequiredArgsConstructor
public class FindOrderUseCase implements FindOrderPort {

    private final LoadOrderPort loadOrderPort;

    /**
     * 주문 조회 실행.
     *
     * @param orderId 주문 ID
     * @return 주문 응답 DTO
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse execute(String orderId) {
        // 1. 주문 조회
        OrderDomain order = loadOrderPort.loadById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(
                "Order not found: " + orderId
            ));

        // 2. Response 변환 (Assembler 위임)
        return OrderAssembler.toResponse(order);
    }
}
```

**Step 3: Assembler (DTO 변환)**
```java
package com.company.template.application.assembler;

import com.company.template.application.dto.response.OrderResponse;
import com.company.template.domain.OrderDomain;

/**
 * Order Assembler.
 *
 * <p>Domain과 DTO 간 변환을 담당합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderAssembler {

    /**
     * OrderDomain → OrderResponse 변환.
     */
    public static OrderResponse toResponse(OrderDomain order) {
        return new OrderResponse(
            order.getOrderId().getValue(),
            order.getCustomerId(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }

    private OrderAssembler() {
        throw new AssertionError("Assembler 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

**Step 4: Command DTO (Record)**
```java
package com.company.template.application.port.in.command;

/**
 * 주문 생성 Command.
 *
 * @param customerId 고객 ID
 * @param productId 상품 ID
 * @param quantity 수량
 * @author Claude Code
 * @since 2025-01-13
 */
public record PlaceOrderCommand(
    Long customerId,
    Long productId,
    Integer quantity
) {
    // Compact constructor for validation
    public PlaceOrderCommand {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId cannot be null");
        }
        if (quantity == null || quantity < 1 || quantity > 100) {
            throw new IllegalArgumentException(
                "Quantity must be between 1 and 100, but was: " + quantity
            );
        }
    }
}
```

**Step 5: Response DTO (Record)**
```java
package com.company.template.application.dto.response;

import com.company.template.domain.OrderStatus;

import java.time.LocalDateTime;

/**
 * 주문 응답 DTO.
 *
 * @param orderId 주문 ID
 * @param customerId 고객 ID
 * @param status 주문 상태
 * @param createdAt 생성 시간
 * @author Claude Code
 * @since 2025-01-13
 */
public record OrderResponse(
    String orderId,
    Long customerId,
    OrderStatus status,
    LocalDateTime createdAt
) {}
```

## GREEN Phase Workflow

**Step 1: Focus on the Failing Test**
```java
// Test from RED phase
@Test
@DisplayName("주문 생성 - 정상 케이스")
void shouldPlaceOrder() {
    // Given
    PlaceOrderCommand command = PlaceOrderCommandFixture.create();
    OrderDomain order = OrderDomainFixture.create();

    given(loadCustomerPort.loadById(command.customerId()))
        .willReturn(Optional.of(CustomerFixture.create()));
    given(saveOrderPort.save(any(OrderDomain.class)))
        .willReturn(order);

    // When
    OrderResponse response = placeOrderUseCase.execute(command);

    // Then
    assertThat(response.orderId()).isNotNull();
    assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
}
```

**Step 2: Write Minimum Code (UseCase)**
```java
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    private final LoadCustomerPort loadCustomerPort;
    private final SaveOrderPort saveOrderPort;

    @Override
    @Transactional
    public OrderResponse execute(PlaceOrderCommand command) {
        CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(
                "Customer not found: " + command.customerId()
            ));

        OrderDomain order = OrderDomain.create(
            OrderId.generate(),
            command.customerId(),
            command.productId(),
            command.quantity()
        );

        OrderDomain savedOrder = saveOrderPort.save(order);

        return OrderAssembler.toResponse(savedOrder);
    }
}
```

**Step 3: Run the Test**
```bash
./gradlew test --tests "*PlaceOrderUseCaseTest.shouldPlaceOrder"
```

**Step 4: Verify GREEN**
```
✅ Test PASSED
```

## Application-Specific Implementation Patterns

### 1. Transaction 경계 관리
```java
// ✅ CORRECT (Transaction 내부는 DB 작업만)
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // DB 작업 (트랜잭션 내부)
    CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
        .orElseThrow(() -> new CustomerNotFoundException(...));

    OrderDomain order = OrderDomain.create(...);
    OrderDomain savedOrder = saveOrderPort.save(order);

    return OrderAssembler.toResponse(savedOrder);
}

// ❌ WRONG (트랜잭션 내부에서 외부 API 호출)
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    PaymentResult result = paymentClient.requestPayment(...);  // ❌ 절대 금지!
    // ...
}
```

### 2. Spring Proxy 제약사항 준수
```java
// ✅ CORRECT (Public 메서드에 @Transactional)
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    @Override
    @Transactional  // ✅ Public method
    public OrderResponse execute(PlaceOrderCommand command) {
        // ...
    }
}

// ❌ WRONG (Private 메서드에 @Transactional)
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    @Transactional  // ❌ Spring Proxy 작동 안 함!
    private OrderResponse executeInternal(PlaceOrderCommand command) {
        // ...
    }
}
```

### 3. CQRS 분리
```java
// ✅ CORRECT (Command와 Query 분리)

// Command UseCase
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    @Override
    @Transactional  // 쓰기 트랜잭션
    public OrderResponse execute(PlaceOrderCommand command) {
        // Command 로직: 상태 변경
    }
}

// Query UseCase
@UseCase
@RequiredArgsConstructor
public class FindOrderUseCase implements FindOrderPort {

    @Override
    @Transactional(readOnly = true)  // 읽기 전용 트랜잭션
    public OrderResponse execute(String orderId) {
        // Query 로직: 상태 변경 없음
    }
}
```

### 4. Assembler 위임
```java
// ✅ CORRECT (Assembler에 변환 위임)
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    OrderDomain savedOrder = saveOrderPort.save(order);
    return OrderAssembler.toResponse(savedOrder);  // Assembler 위임
}

// ❌ WRONG (UseCase에서 직접 변환)
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    OrderDomain savedOrder = saveOrderPort.save(order);
    // UseCase에서 직접 변환 - 책임 분리 위반
    return new OrderResponse(
        savedOrder.getOrderId().getValue(),
        savedOrder.getCustomerId(),
        savedOrder.getStatus(),
        savedOrder.getCreatedAt()
    );
}
```

### 5. Port 명명 규칙
```java
// ✅ CORRECT (명명 규칙 준수)

// Inbound Port (Command)
public interface PlaceOrderPort {
    OrderResponse execute(PlaceOrderCommand command);
}

// Outbound Port (Command)
public interface SaveOrderPort {
    OrderDomain save(OrderDomain order);
}

// Outbound Port (Query)
public interface LoadOrderPort {
    Optional<OrderDomain> loadById(String orderId);
}
```

## Common Mistakes to Avoid

### ❌ WRONG: Transaction 내부에서 외부 API 호출
```java
// ❌ 절대 금지!
@Transactional
public OrderResponse execute(PlaceOrderCommand command) {
    // ...
    PaymentResult result = paymentClient.requestPayment(...);  // ❌
    // DB 커넥션이 외부 API 호출 동안 잠김!
}
```

### ❌ WRONG: Private 메서드에 @Transactional
```java
// ❌ Spring Proxy 작동 안 함
@Transactional
private void processOrder() {
    // Proxy가 private 메서드를 가로챌 수 없음
}
```

### ❌ WRONG: Command와 Query 혼합
```java
// ❌ CQRS 위반
@Transactional
public OrderResponse placeAndFindOrder(PlaceOrderCommand command) {
    // Command (쓰기) + Query (읽기) 혼합
    OrderDomain order = saveOrderPort.save(...);
    List<OrderDomain> allOrders = loadOrderPort.findAll();  // 혼합!
    return OrderAssembler.toResponse(order);
}
```

## Success Criteria

- ✅ Test runs and PASSES
- ✅ Minimum code written (no extra features)
- ✅ Transaction boundaries correct (`@Transactional` 내 DB 작업만)
- ✅ Spring Proxy constraints followed (Public 메서드에만 `@Transactional`)
- ✅ CQRS separation maintained (Command vs Query)
- ✅ Assembler delegation used (DTO 변환)
- ✅ Port naming conventions followed (Save/Load/Find)

## What NOT to Do

- ❌ Don't write more code than needed to pass the test
- ❌ Don't add "nice to have" features
- ❌ Don't refactor yet (that's the next phase!)
- ❌ Don't call external APIs inside `@Transactional`
- ❌ Don't use `@Transactional` on private/final methods
- ❌ Don't mix Command and Query logic

This is Kent Beck's TDD: Write the SIMPLEST code to pass the test, then REFACTOR.
