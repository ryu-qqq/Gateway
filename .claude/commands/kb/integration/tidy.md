# Integration Layer TDD Tidy - Clean Up Tests

You are in the TIDY phase of Kent Beck's TDD cycle for **Integration Layer (E2E Testing)**.

## Instructions

1. **Enforce IntegrationTestFixture usage** - Replace inline object creation
2. **Organize tests** - Use @Nested for logical grouping
3. **Clean up test names** - Ensure descriptive DisplayName
4. **Verify Zero-Tolerance rules** - Check Flyway vs @Sql separation
5. **Run all tests** and ensure they pass

## Tidy Phase Goals

- **IntegrationTestFixture enforcement**: All tests use Fixture.create()
- **Test organization**: @Nested for logical grouping
- **Readability**: Clear test names and structure
- **Maintainability**: Consistent patterns across tests
- **Zero-Tolerance compliance**: Flyway (DDL), @Sql (DML), TestRestTemplate

## Tidy Patterns

### 1. Enforce IntegrationTestFixture Usage

**Before** (Inline object creation):
```java
@Test
@Sql("/sql/orders-test-data.sql")
void shouldCreateOrder() {
    // ❌ Inline object creation
    PlaceOrderRequest request = new PlaceOrderRequest(1L, 100L, 10);

    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
    assertCreated(response);
}
```

**After** (IntegrationTestFixture):
```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("E2E - 주문 생성 성공")
void shouldCreateOrder() {
    // ✅ Use IntegrationTestFixture
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
    assertCreated(response);
}
```

### 2. Organize Tests with @Nested

**Before** (Flat structure):
```java
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("E2E - 주문 생성 성공")
    void shouldCreateOrder() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 생성 실패 - Validation")
    void shouldFailValidation() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 조회 성공")
    void shouldGetOrder() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 조회 실패 - Not Found")
    void shouldFailGetOrder() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 취소 성공")
    void shouldCancelOrder() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 취소 실패 - 이미 취소됨")
    void shouldFailCancelOrder() { /* ... */ }
}
```

**After** (@Nested grouping):
```java
@Sql("/sql/orders-test-data.sql")  // Class-level @Sql
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("주문 생성 API")
    class CreateOrderTests {

        @Test
        @DisplayName("E2E - 주문 생성 성공")
        void shouldCreateOrder() {
            // Given
            PlaceOrderRequest request = PlaceOrderRequestFixture.create();

            // When
            ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

            // Then
            assertCreated(response);
            assertThat(response.getBody().orderId()).isNotNull();
        }

        @Test
        @DisplayName("E2E - Validation 실패 - customerId null")
        void shouldFailWhenCustomerIdIsNull() {
            // Given
            PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNullCustomerId();

            // When
            ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

            // Then
            assertBadRequest(response);
        }

        @Test
        @DisplayName("E2E - Validation 실패 - quantity 음수")
        void shouldFailWhenQuantityIsNegative() {
            // Given
            PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNegativeQuantity();

            // When
            ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

            // Then
            assertBadRequest(response);
        }
    }

    @Nested
    @DisplayName("주문 조회 API")
    class GetOrderTests {

        @Test
        @DisplayName("E2E - 주문 조회 성공")
        void shouldGetOrder() {
            // Given
            String orderId = createOrder(PlaceOrderRequestFixture.create());

            // When
            ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);

            // Then
            assertOk(response);
            assertThat(response.getBody().orderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("E2E - 주문 조회 실패 - 존재하지 않는 주문")
        void shouldFailWhenOrderNotFound() {
            // Given
            String nonExistentOrderId = "non-existent-id";

            // When
            ResponseEntity<OrderResponse> response = get("/api/orders/" + nonExistentOrderId, OrderResponse.class);

            // Then
            assertNotFound(response);
        }
    }

    @Nested
    @DisplayName("주문 취소 API")
    class CancelOrderTests {

        @Test
        @DisplayName("E2E - 주문 취소 성공")
        void shouldCancelOrder() {
            // Given
            String orderId = createOrder(PlaceOrderRequestFixture.create());

            // When
            delete("/api/orders/" + orderId);

            // Then
            ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);
            assertNotFound(response);
        }

        @Test
        @DisplayName("E2E - 주문 취소 실패 - 이미 취소된 주문")
        void shouldFailWhenAlreadyCancelled() {
            // Given - Use @Sql test data (already cancelled order)
            String cancelledOrderId = String.valueOf(TEST_ORDER_ID_CONFIRMED);

            // When
            delete("/api/orders/" + cancelledOrderId);

            // Then
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/orders/" + cancelledOrderId,
                HttpMethod.DELETE,
                null,
                ErrorResponse.class
            );
            assertBadRequest(response);
        }
    }

    @Nested
    @DisplayName("주문 프로세스 워크플로우")
    class OrderWorkflowTests {

        @Test
        @DisplayName("E2E - 전체 주문 프로세스 (생성 → 확인 → 배송 → 완료)")
        void shouldCompleteOrderProcess() {
            // Step 1: 주문 생성
            String orderId = createOrder(PlaceOrderRequestFixture.create());
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.PLACED);

            // Step 2: 주문 확인
            changeOrderStatus(orderId, "confirm");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.CONFIRMED);

            // Step 3: 배송 시작
            changeOrderStatus(orderId, "ship");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.SHIPPED);

            // Step 4: 배송 완료
            changeOrderStatus(orderId, "complete");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.COMPLETED);
        }
    }
}
```

### 3. Clean Up Test Names (DisplayName)

**Before**:
```java
@Test
void test1() { /* ... */ }

@Test
void test2() { /* ... */ }

@Test
void createOrder() { /* ... */ }
```

**After**:
```java
@Test
@DisplayName("E2E - 주문 생성 성공")
void shouldCreateOrder() { /* ... */ }

@Test
@DisplayName("E2E - 주문 조회 성공")
void shouldGetOrder() { /* ... */ }

@Test
@DisplayName("E2E - Validation 실패 - customerId null")
void shouldFailWhenCustomerIdIsNull() { /* ... */ }
```

### 4. Verify Flyway vs @Sql Separation

**Check Flyway Migration Files** (DDL):
```sql
-- ✅ CORRECT: src/main/resources/db/migration/V1__create_order_table.sql
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount BIGINT NOT NULL,
    order_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_customer_id (customer_id),
    INDEX idx_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Check @Sql Test Data Files** (DML):
```sql
-- ✅ CORRECT: src/test/resources/sql/orders-test-data.sql
DELETE FROM orders;
DELETE FROM customers;

INSERT INTO customers (customer_id, name, email)
VALUES (1, 'Alice', 'alice@example.com');

INSERT INTO customers (customer_id, name, email)
VALUES (2, 'Bob', 'bob@example.com');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (100, 1, 'PENDING', 10000, '2024-01-01');

-- ❌ WRONG: No DDL in @Sql files
-- CREATE TABLE orders (...);  -- Use Flyway migration instead
```

### 5. Extract Reusable Test Data Constants

**Before**:
```java
@Test
void shouldGetExistingOrder() {
    // ❌ Magic number
    ResponseEntity<OrderResponse> response = get("/api/orders/100", OrderResponse.class);
}
```

**After**:
```java
public abstract class IntegrationTestSupport {

    // @Sql Test Data IDs
    protected static final Long TEST_CUSTOMER_ID = 1L;
    protected static final Long TEST_ORDER_ID_PENDING = 100L;
    protected static final Long TEST_ORDER_ID_CONFIRMED = 200L;
}

@Test
@DisplayName("E2E - 기존 주문 조회 성공")
void shouldGetExistingOrder() {
    // ✅ Use constant
    ResponseEntity<OrderResponse> response = get(
        "/api/orders/" + TEST_ORDER_ID_PENDING,
        OrderResponse.class
    );
    assertOk(response);
}
```

### 6. Consistent Given-When-Then Structure

**Before**:
```java
@Test
void shouldCreateOrder() {
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();
    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
    assertCreated(response);
}
```

**After**:
```java
@Test
@DisplayName("E2E - 주문 생성 성공")
void shouldCreateOrder() {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    // When
    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

    // Then
    assertCreated(response);
    assertThat(response.getBody().orderId()).isNotNull();
}
```

### 7. Extract WireMock Tests to Separate Class

**Before**:
```java
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("E2E - 주문 생성 성공")
    void shouldCreateOrder() { /* ... */ }

    @Test
    @DisplayName("E2E - 외부 API 호출 포함 주문 생성")
    void shouldCreateOrderWithExternalApi() {
        // WireMock setup...
    }
}
```

**After** (Separate WireMock tests):
```java
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("E2E - 주문 생성 성공")
    void shouldCreateOrder() { /* ... */ }
}

@DisplayName("Order 외부 API 통합 테스트")
class OrderExternalApiIntegrationTest extends WireMockTestSupport {

    @Test
    @Sql("/sql/orders-test-data.sql")
    @DisplayName("E2E - 외부 결제 API 호출 포함 주문 생성")
    void shouldCreateOrderWithExternalPaymentApi() {
        // Given
        stubExternalApiSuccess("/payment", "{\"status\":\"success\"}");
        PlaceOrderRequest request = PlaceOrderRequestFixture.create();

        // When
        ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

        // Then
        assertCreated(response);

        // Verify
        verify(postRequestedFor(urlEqualTo("/payment")));
    }
}
```

## Final Clean Test Structure Example

```java
package com.company.template.integration;

import com.company.template.integration.fixture.PlaceOrderRequestFixture;
import com.company.template.integration.support.IntegrationTestSupport;
import com.company.template.restapi.dto.request.PlaceOrderRequest;
import com.company.template.restapi.dto.response.OrderResponse;
import com.company.template.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Sql("/sql/orders-test-data.sql")
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("주문 생성 API")
    class CreateOrderTests {

        @Test
        @DisplayName("E2E - 주문 생성 성공")
        void shouldCreateOrder() {
            // Given
            PlaceOrderRequest request = PlaceOrderRequestFixture.create();

            // When
            ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

            // Then
            assertCreated(response);
            assertThat(response.getBody().orderId()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(OrderStatus.PLACED);
        }

        @Test
        @DisplayName("E2E - Validation 실패 - customerId null")
        void shouldFailWhenCustomerIdIsNull() {
            // Given
            PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNullCustomerId();

            // When
            ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

            // Then
            assertBadRequest(response);
        }
    }

    @Nested
    @DisplayName("주문 조회 API")
    class GetOrderTests {

        @Test
        @DisplayName("E2E - 주문 조회 성공")
        void shouldGetOrder() {
            // Given
            String orderId = createOrder(PlaceOrderRequestFixture.create());

            // When
            ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);

            // Then
            assertOk(response);
            assertThat(response.getBody().orderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("E2E - 기존 주문 조회 성공 (@Sql 데이터)")
        void shouldGetExistingOrder() {
            // Given - Use @Sql test data
            String orderId = String.valueOf(TEST_ORDER_ID_PENDING);

            // When
            ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);

            // Then
            assertOk(response);
            assertThat(response.getBody().orderId()).isEqualTo(orderId);
            assertThat(response.getBody().customerId()).isEqualTo(TEST_CUSTOMER_ID);
        }

        @Test
        @DisplayName("E2E - 주문 조회 실패 - 존재하지 않는 주문")
        void shouldFailWhenOrderNotFound() {
            // Given
            String nonExistentOrderId = "non-existent-id";

            // When
            ResponseEntity<OrderResponse> response = get("/api/orders/" + nonExistentOrderId, OrderResponse.class);

            // Then
            assertNotFound(response);
        }
    }

    @Nested
    @DisplayName("주문 취소 API")
    class CancelOrderTests {

        @Test
        @DisplayName("E2E - 주문 취소 성공")
        void shouldCancelOrder() {
            // Given
            String orderId = createOrder(PlaceOrderRequestFixture.create());

            // When
            delete("/api/orders/" + orderId);

            // Then
            ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);
            assertNotFound(response);
        }
    }

    @Nested
    @DisplayName("주문 프로세스 워크플로우")
    class OrderWorkflowTests {

        @Test
        @DisplayName("E2E - 전체 주문 프로세스 (생성 → 확인 → 배송 → 완료)")
        void shouldCompleteOrderProcess() {
            // Step 1: 주문 생성
            String orderId = createOrder(PlaceOrderRequestFixture.create());
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.PLACED);

            // Step 2: 주문 확인
            changeOrderStatus(orderId, "confirm");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.CONFIRMED);

            // Step 3: 배송 시작
            changeOrderStatus(orderId, "ship");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.SHIPPED);

            // Step 4: 배송 완료
            changeOrderStatus(orderId, "complete");
            assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.COMPLETED);
        }
    }
}
```

## Core Principles

- **IntegrationTestFixture enforcement**: No inline object creation
- **@Nested organization**: Logical grouping by API or workflow
- **Descriptive names**: Clear @DisplayName for all tests
- **Consistent structure**: Given-When-Then in all tests
- **Zero-Tolerance compliance**: Verify Flyway vs @Sql separation
- **Reusable patterns**: Extract constants and helper methods

## Success Criteria

- ✅ All tests use IntegrationTestFixture.create() (no inline objects)
- ✅ Tests organized with @Nested grouping
- ✅ All tests have descriptive @DisplayName
- ✅ Given-When-Then structure consistent
- ✅ Flyway (DDL) and @Sql (DML) properly separated
- ✅ Test data constants extracted (no magic numbers)
- ✅ WireMock tests in separate class (if exists)
- ✅ All tests passing

## What NOT to Do

- ❌ Don't add new tests (wait for next RED phase)
- ❌ Don't change test behavior or assertions
- ❌ Don't refactor production code (focus on test code)
- ❌ Don't put DDL in @Sql files (use Flyway)
- ❌ Don't use inline object creation (use IntegrationTestFixture)

## Checklist

- [ ] All tests use IntegrationTestFixture
- [ ] @Nested grouping applied
- [ ] @DisplayName for all tests
- [ ] Given-When-Then structure
- [ ] Flyway (DDL) and @Sql (DML) separated
- [ ] Test data constants extracted
- [ ] WireMock tests separated (if exists)
- [ ] All tests passing

## 참고 문서

- [Integration Testing Overview](../../../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)
  - IntegrationTestFixture Pattern
  - Flyway vs @Sql 명확한 구분
  - Test Organization Best Practices

This is Kent Beck's TDD: Clean up tests to enforce best practices and maintainability.
