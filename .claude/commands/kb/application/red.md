# Application Layer TDD Red - Write Failing Test

You are in the 🔴 RED phase of Kent Beck's TDD cycle for **Application Layer**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-application-plan.md`
2. **Understand the requirement** for the current test
3. **Apply Tidy First**: If code needs cleanup, do Structural Changes FIRST → `struct:` 커밋
4. **Create TestFixture classes FIRST** (if not exists)
5. **Write the simplest failing test** using TestFixture
6. **Run the test** and verify it FAILS for the right reason
7. **Report the failure** clearly
8. **Commit with test: prefix**:
   ```bash
   git add .
   git commit -m "test: 주문 생성 UseCase 테스트 추가"
   ```

## Application Layer TestFixture Pattern (MANDATORY)

### Why TestFixture in Application Layer?
- **Reusability**: Share Command/Query DTO creation across tests
- **Mock Management**: Consistent Port mocking patterns
- **Maintainability**: Change test data in one place
- **Transaction Testing**: Simulate transaction boundaries safely

### TestFixture Structure
```
application/src/
├── main/java/
│   └── {basePackage}/application/
│       ├── port/
│       │   ├── in/
│       │   │   ├── command/
│       │   │   │   └── PlaceOrderPort.java
│       │   │   └── query/
│       │   │       └── FindOrderPort.java
│       │   └── out/
│       │       ├── SaveOrderPort.java
│       │       └── LoadCustomerPort.java
│       ├── usecase/
│       │   ├── PlaceOrderUseCase.java
│       │   └── FindOrderUseCase.java
│       └── assembler/
│           └── OrderAssembler.java
└── testFixtures/java/
    └── {basePackage}/application/fixture/
        ├── PlaceOrderCommandFixture.java
        ├── OrderResponseFixture.java
        └── OrderAssemblerFixture.java
```

### TestFixture Template (Command DTO)
```java
package com.company.template.application.fixture;

import com.company.template.application.port.in.command.PlaceOrderCommand;

/**
 * TestFixture for PlaceOrderCommand.
 *
 * <p>Object Mother 패턴으로 Command DTO를 생성합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class PlaceOrderCommandFixture {

    private static final Long DEFAULT_CUSTOMER_ID = 1L;
    private static final Long DEFAULT_PRODUCT_ID = 100L;
    private static final Integer DEFAULT_QUANTITY = 10;

    /**
     * 기본 PlaceOrderCommand 생성.
     */
    public static PlaceOrderCommand create() {
        return new PlaceOrderCommand(
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    /**
     * 특정 고객 ID로 PlaceOrderCommand 생성.
     */
    public static PlaceOrderCommand createWithCustomerId(Long customerId) {
        return new PlaceOrderCommand(
            customerId,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    /**
     * 특정 수량으로 PlaceOrderCommand 생성.
     */
    public static PlaceOrderCommand createWithQuantity(int quantity) {
        return new PlaceOrderCommand(
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            quantity
        );
    }

    /**
     * 잘못된 Command 생성 (Validation 테스트용).
     */
    public static PlaceOrderCommand createInvalid() {
        return new PlaceOrderCommand(
            null,  // Invalid: customerId is null
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    private PlaceOrderCommandFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

### Response DTO Fixture
```java
package com.company.template.application.fixture;

import com.company.template.application.dto.response.OrderResponse;
import com.company.template.domain.OrderStatus;

import java.time.LocalDateTime;

/**
 * TestFixture for OrderResponse.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderResponseFixture {

    private static final String DEFAULT_ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long DEFAULT_CUSTOMER_ID = 1L;
    private static final OrderStatus DEFAULT_STATUS = OrderStatus.PLACED;

    /**
     * 기본 OrderResponse 생성.
     */
    public static OrderResponse create() {
        return new OrderResponse(
            DEFAULT_ORDER_ID,
            DEFAULT_CUSTOMER_ID,
            DEFAULT_STATUS,
            LocalDateTime.now()
        );
    }

    /**
     * 특정 상태로 OrderResponse 생성.
     */
    public static OrderResponse createWithStatus(OrderStatus status) {
        return new OrderResponse(
            DEFAULT_ORDER_ID,
            DEFAULT_CUSTOMER_ID,
            status,
            LocalDateTime.now()
        );
    }

    private OrderResponseFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

## RED Phase Workflow with TestFixture

**Step 1: Create Fixtures FIRST**
```bash
# Create testFixtures directory structure
mkdir -p application/src/testFixtures/java/{basePackage}/application/fixture/

# Create Fixture classes
touch application/src/testFixtures/java/.../PlaceOrderCommandFixture.java
touch application/src/testFixtures/java/.../OrderResponseFixture.java
```

**Step 2: Write Tests Using Fixtures**
```java
package com.company.template.application.usecase;

import com.company.template.application.fixture.PlaceOrderCommandFixture;
import com.company.template.application.port.in.command.PlaceOrderCommand;
import com.company.template.application.port.out.LoadCustomerPort;
import com.company.template.application.port.out.SaveOrderPort;
import com.company.template.domain.OrderDomain;
import com.company.template.domain.fixture.CustomerFixture;
import com.company.template.domain.fixture.OrderDomainFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseTest {

    @Mock
    private LoadCustomerPort loadCustomerPort;

    @Mock
    private SaveOrderPort saveOrderPort;

    @InjectMocks
    private PlaceOrderUseCase placeOrderUseCase;

    @Test
    @DisplayName("주문 생성 - 정상 케이스")
    void shouldPlaceOrder() {
        // Given - Use Fixtures
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
        verify(saveOrderPort).save(any(OrderDomain.class));
    }

    @Test
    @DisplayName("주문 생성 - 고객 없음 예외")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given - Use Fixtures
        PlaceOrderCommand command = PlaceOrderCommandFixture.create();

        given(loadCustomerPort.loadById(command.customerId()))
            .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> placeOrderUseCase.execute(command))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("주문 생성 - 잘못된 수량 예외")
    void shouldThrowExceptionWhenInvalidQuantity() {
        // Given - Use Fixtures
        PlaceOrderCommand command = PlaceOrderCommandFixture.createWithQuantity(0);

        given(loadCustomerPort.loadById(command.customerId()))
            .willReturn(Optional.of(CustomerFixture.create()));

        // When & Then
        assertThatThrownBy(() -> placeOrderUseCase.execute(command))
            .isInstanceOf(InvalidQuantityException.class)
            .hasMessageContaining("Quantity must be between 1 and 100");
    }
}
```

## Application Layer Specific Test Patterns

### 1. Command UseCase Test (Transaction 내부)
```java
@Test
@DisplayName("주문 생성 UseCase - 트랜잭션 내부 로직 검증")
void shouldExecuteTransactionalLogic() {
    // Given
    PlaceOrderCommand command = PlaceOrderCommandFixture.create();
    CustomerDomain customer = CustomerFixture.create();
    OrderDomain order = OrderDomainFixture.create();

    given(loadCustomerPort.loadById(command.customerId()))
        .willReturn(Optional.of(customer));
    given(saveOrderPort.save(any(OrderDomain.class)))
        .willReturn(order);

    // When
    OrderResponse response = placeOrderUseCase.execute(command);

    // Then
    assertThat(response).isNotNull();
    verify(loadCustomerPort).loadById(command.customerId());
    verify(saveOrderPort).save(any(OrderDomain.class));
}
```

### 2. Query UseCase Test (DTO 변환)
```java
@Test
@DisplayName("주문 조회 UseCase - DTO 변환 검증")
void shouldFindOrderAndConvertToResponse() {
    // Given
    String orderId = "order-123";
    OrderDomain order = OrderDomainFixture.create();

    given(loadOrderPort.loadById(orderId))
        .willReturn(Optional.of(order));

    // When
    OrderResponse response = findOrderUseCase.execute(orderId);

    // Then
    assertThat(response.orderId()).isEqualTo(order.getOrderId().getValue());
    assertThat(response.status()).isEqualTo(order.getStatus());
}
```

### 3. Assembler Test (DTO 변환 로직)
```java
@Test
@DisplayName("Assembler - Domain → Response 변환")
void shouldConvertDomainToResponse() {
    // Given
    OrderDomain order = OrderDomainFixture.create();

    // When
    OrderResponse response = OrderAssembler.toResponse(order);

    // Then
    assertThat(response.orderId()).isEqualTo(order.getOrderId().getValue());
    assertThat(response.customerId()).isEqualTo(order.getCustomerId());
    assertThat(response.status()).isEqualTo(order.getStatus());
}
```

### 4. Validation Test
```java
@Test
@DisplayName("Command Validation - customerId null 검증")
void shouldThrowExceptionWhenCustomerIdIsNull() {
    // Given
    PlaceOrderCommand command = PlaceOrderCommandFixture.createInvalid();

    // When & Then
    assertThatThrownBy(() -> placeOrderUseCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("customerId cannot be null");
}
```

### 5. Transaction Boundary Test
```java
@Test
@DisplayName("Transaction 경계 - 외부 API 호출은 트랜잭션 밖")
void shouldCallExternalApiOutsideTransaction() {
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
    assertThat(response).isNotNull();
    // 외부 API 호출은 트랜잭션 밖에서 실행되어야 함
    // (Integration Test에서 검증)
}
```

## Core Principles

- **Fixture First**: Always create Fixture classes before writing tests
- Write the SIMPLEST test that could possibly fail
- Test should fail for the RIGHT reason (not compilation error)
- One assertion per test when possible
- Test name describes the expected behavior
- No implementation code yet - just the test
- **Use Fixture.create()** instead of inline object creation
- Mock all Outbound Ports

## Success Criteria

- ✅ TestFixture classes created in `testFixtures/` directory
- ✅ Test written with clear, descriptive name
- ✅ Test uses Fixture.create() methods (NOT inline object creation)
- ✅ Test runs and FAILS
- ✅ Failure message is clear and informative
- ✅ Test defines a small, specific increment of functionality
- ✅ Zero-Tolerance rules followed (Transaction 경계, Spring Proxy 제약)

## What NOT to Do

- ❌ Don't write implementation code yet
- ❌ Don't write multiple tests at once
- ❌ Don't skip running the test to verify failure
- ❌ Don't write tests that pass immediately
- ❌ Don't create objects inline in tests (use Fixture instead)
- ❌ Don't test external API calls inside `@Transactional` methods
- ❌ Don't use `@Transactional` on private/final methods

This is Kent Beck's TDD: Start with RED, make the failure explicit, and use TestFixture for maintainability.
