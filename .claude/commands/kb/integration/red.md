# Integration Layer TDD Red - Write Failing Test

You are in the RED phase of Kent Beck's TDD cycle for **Integration Layer (E2E Testing)**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-integration-plan.md`
2. **Understand the requirement** for the current test
3. **Create IntegrationTestFixture classes FIRST** (if not exists)
4. **Create @Sql test data files** (if not exists)
5. **Write the simplest failing test** using IntegrationTestFixture
6. **Run the test** and verify it FAILS for the right reason
7. **Report the failure** clearly

## Integration Layer IntegrationTestFixture Pattern (MANDATORY)

### Why IntegrationTestFixture in Integration Layer?
- **Reusability**: Share Request/Response DTO creation across E2E tests
- **Maintainability**: Change test data in one place
- **Test Data Consistency**: Align with @Sql data
- **E2E Scenarios**: Support complex multi-step workflows

### IntegrationTestFixture Structure
```
bootstrap-web-api/src/
├── main/java/
│   └── {basePackage}/
│       ├── controller/
│       │   └── OrderController.java
│       ├── dto/
│       │   ├── request/
│       │   │   └── PlaceOrderRequest.java
│       │   └── response/
│       │       └── OrderResponse.java
│       └── Application.java
└── testFixtures/java/
    └── {basePackage}/integration/fixture/
        ├── PlaceOrderRequestFixture.java
        ├── OrderResponseFixture.java
        └── IntegrationTestSupport.java  (Optional)
```

### IntegrationTestFixture Template (Request DTO)
```java
package com.company.template.integration.fixture;

import com.company.template.restapi.dto.request.PlaceOrderRequest;

/**
 * IntegrationTestFixture for PlaceOrderRequest.
 *
 * <p>E2E 테스트에서 사용할 Request DTO를 생성합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class PlaceOrderRequestFixture {

    private static final Long DEFAULT_CUSTOMER_ID = 1L;  // Matches @Sql data
    private static final Long DEFAULT_PRODUCT_ID = 100L;
    private static final Integer DEFAULT_QUANTITY = 10;

    /**
     * 기본 PlaceOrderRequest 생성.
     *
     * <p>@Sql 테스트 데이터와 일치하는 customerId를 사용합니다.</p>
     */
    public static PlaceOrderRequest create() {
        return new PlaceOrderRequest(
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    /**
     * 특정 고객 ID로 생성.
     */
    public static PlaceOrderRequest createWithCustomerId(Long customerId) {
        return new PlaceOrderRequest(
            customerId,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    /**
     * 특정 수량으로 생성.
     */
    public static PlaceOrderRequest createWithQuantity(int quantity) {
        return new PlaceOrderRequest(
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            quantity
        );
    }

    /**
     * Validation 실패 케이스 - null customerId.
     */
    public static PlaceOrderRequest createWithNullCustomerId() {
        return new PlaceOrderRequest(
            null,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY
        );
    }

    private PlaceOrderRequestFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

### IntegrationTestFixture Template (Response DTO)
```java
package com.company.template.integration.fixture;

import com.company.template.restapi.dto.response.OrderResponse;
import com.company.template.domain.OrderStatus;

/**
 * IntegrationTestFixture for OrderResponse.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderResponseFixture {

    private static final String DEFAULT_ORDER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Long DEFAULT_CUSTOMER_ID = 1L;
    private static final Long DEFAULT_PRODUCT_ID = 100L;
    private static final Integer DEFAULT_QUANTITY = 10;
    private static final OrderStatus DEFAULT_STATUS = OrderStatus.PLACED;

    /**
     * 기본 OrderResponse 생성.
     */
    public static OrderResponse create() {
        return new OrderResponse(
            DEFAULT_ORDER_ID,
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY,
            DEFAULT_STATUS
        );
    }

    /**
     * 특정 주문 ID로 생성.
     */
    public static OrderResponse createWithOrderId(String orderId) {
        return new OrderResponse(
            orderId,
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY,
            DEFAULT_STATUS
        );
    }

    /**
     * 특정 상태로 생성.
     */
    public static OrderResponse createWithStatus(OrderStatus status) {
        return new OrderResponse(
            DEFAULT_ORDER_ID,
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            DEFAULT_QUANTITY,
            status
        );
    }

    private OrderResponseFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

## RED Phase Workflow with IntegrationTestFixture

**Step 1: Create Flyway Migration (DDL) - IF NOT EXISTS**
```bash
# Check existing migrations
ls src/main/resources/db/migration/

# Create new migration ONLY if needed
touch src/main/resources/db/migration/V1__create_order_table.sql
```

**V1__create_order_table.sql (Flyway)**:
```sql
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

**Step 2: Create @Sql Test Data (DML)**
```bash
# Create test data directory
mkdir -p src/test/resources/sql/

# Create @Sql test data file
touch src/test/resources/sql/orders-test-data.sql
```

**orders-test-data.sql (@Sql)**:
```sql
-- ✅ CORRECT: DML only (INSERT, DELETE)
DELETE FROM orders;
DELETE FROM customers;

INSERT INTO customers (customer_id, name, email)
VALUES (1, 'Alice', 'alice@example.com');

INSERT INTO customers (customer_id, name, email)
VALUES (2, 'Bob', 'bob@example.com');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (100, 1, 'PENDING', 10000, '2024-01-01');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (200, 2, 'CONFIRMED', 20000, '2024-01-02');

-- ❌ WRONG: No DDL in @Sql files
-- CREATE TABLE orders (...);  -- ❌ Use Flyway migration instead
```

**Step 3: Create Fixtures**
```bash
# Create testFixtures directory structure
mkdir -p bootstrap-web-api/src/testFixtures/java/{basePackage}/integration/fixture/

# Create Fixture classes
touch bootstrap-web-api/src/testFixtures/java/.../PlaceOrderRequestFixture.java
touch bootstrap-web-api/src/testFixtures/java/.../OrderResponseFixture.java
```

**Step 4: Write Tests Using Fixtures**
```java
package com.company.template.integration;

import com.company.template.integration.fixture.PlaceOrderRequestFixture;
import com.company.template.integration.fixture.OrderResponseFixture;
import com.company.template.restapi.dto.request.PlaceOrderRequest;
import com.company.template.restapi.dto.response.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Sql("/sql/orders-test-data.sql")  // Test data only (DML)
    @DisplayName("E2E - 주문 생성 및 조회")
    void shouldCreateAndGetOrder() {
        // Given - Use Fixtures
        PlaceOrderRequest request = PlaceOrderRequestFixture.create();

        // When - 주문 생성 (실제 HTTP POST)
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then - 생성 검증
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();
        String orderId = createResponse.getBody().orderId();
        assertThat(orderId).isNotNull();

        // When - 주문 조회 (실제 HTTP GET)
        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId,
            OrderResponse.class
        );

        // Then - 조회 검증
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderResponse retrievedOrder = getResponse.getBody();
        assertThat(retrievedOrder.orderId()).isEqualTo(orderId);
        assertThat(retrievedOrder.customerId()).isEqualTo(request.customerId());
    }

    @Test
    @Sql("/sql/orders-test-data.sql")
    @DisplayName("E2E - 존재하지 않는 주문 조회시 404 Not Found")
    void shouldReturn404WhenOrderNotFound() {
        // Given
        String nonExistentOrderId = "non-existent-id";

        // When - 실제 HTTP GET
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
            "/api/orders/" + nonExistentOrderId,
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql("/sql/orders-test-data.sql")
    @DisplayName("E2E - Validation 실패 시 400 Bad Request")
    void shouldReturn400WhenValidationFails() {
        // Given - Use Fixture for invalid case
        PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNullCustomerId();

        // When - 실제 HTTP POST
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

## Integration Layer Specific Test Patterns

### 1. E2E Test (Full HTTP Flow)
```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("E2E - 주문 생성, 조회, 수정, 삭제")
void shouldPerformFullOrderLifecycle() {
    // Given
    PlaceOrderRequest createRequest = PlaceOrderRequestFixture.create();

    // When - CREATE (POST)
    ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
        "/api/orders",
        createRequest,
        OrderResponse.class
    );
    String orderId = createResponse.getBody().orderId();

    // Then - CREATE 검증
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // When - READ (GET)
    ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );

    // Then - READ 검증
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // When - UPDATE (PUT)
    UpdateOrderRequest updateRequest = UpdateOrderRequestFixture.create();
    restTemplate.put("/api/orders/" + orderId, updateRequest);

    // Then - UPDATE 검증
    ResponseEntity<OrderResponse> updatedResponse = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );
    assertThat(updatedResponse.getBody().status()).isEqualTo(OrderStatus.CONFIRMED);

    // When - DELETE
    restTemplate.delete("/api/orders/" + orderId);

    // Then - DELETE 검증
    ResponseEntity<OrderResponse> deletedResponse = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );
    assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

### 2. Multi-Step Workflow Test
```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("E2E - 복잡한 주문 프로세스 (생성 → 확인 → 배송 → 완료)")
void shouldCompleteOrderProcess() {
    // Step 1: 주문 생성
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();
    ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
        "/api/orders",
        request,
        OrderResponse.class
    );
    String orderId = createResponse.getBody().orderId();

    // Step 2: 주문 확인
    restTemplate.put("/api/orders/" + orderId + "/confirm", null);
    assertThat(getOrderStatus(orderId)).isEqualTo(OrderStatus.CONFIRMED);

    // Step 3: 배송 시작
    restTemplate.put("/api/orders/" + orderId + "/ship", null);
    assertThat(getOrderStatus(orderId)).isEqualTo(OrderStatus.SHIPPED);

    // Step 4: 배송 완료
    restTemplate.put("/api/orders/" + orderId + "/complete", null);
    assertThat(getOrderStatus(orderId)).isEqualTo(OrderStatus.COMPLETED);
}

private OrderStatus getOrderStatus(String orderId) {
    ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );
    return response.getBody().status();
}
```

### 3. Error Handling Test
```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("E2E - 비즈니스 로직 예외 처리")
void shouldHandleBusinessLogicException() {
    // Given - 이미 취소된 주문
    String cancelledOrderId = "100";  // orders-test-data.sql에서 정의

    // When - 취소된 주문을 다시 취소하려고 시도
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(
        "/api/orders/" + cancelledOrderId + "/cancel",
        HttpMethod.DELETE,
        null,
        ErrorResponse.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().errorCode()).isEqualTo("ORDER_ALREADY_CANCELLED");
}
```

### 4. External API Test (WireMock)
```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("E2E - 외부 API 호출 포함 주문 생성")
void shouldCreateOrderWithExternalApi() {
    // Given - WireMock stub
    stubFor(post(urlEqualTo("/payment"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"status\":\"success\",\"transactionId\":\"tx-123\"}")));

    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    // When - 실제 HTTP POST
    ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
        "/api/orders",
        request,
        OrderResponse.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // Verify external API called
    verify(postRequestedFor(urlEqualTo("/payment"))
        .withHeader("Content-Type", equalTo("application/json")));
}
```

## Core Principles

- **Fixture First**: Always create IntegrationTestFixture classes before writing tests
- Write the SIMPLEST test that could possibly fail
- Test should fail for the RIGHT reason (not compilation error)
- One E2E scenario per test when possible
- Test name describes the expected behavior
- No implementation code yet - just the test
- **Use Fixture.create()** instead of inline object creation
- **Use TestRestTemplate** for actual HTTP requests
- **Use @Sql for test data ONLY** (DML, NOT DDL)

## Success Criteria

- ✅ Flyway migration file exists (DDL)
- ✅ @Sql test data file created (DML only, NO DDL)
- ✅ IntegrationTestFixture classes created in `testFixtures/` directory
- ✅ Test written with clear, descriptive name
- ✅ Test uses Fixture.create() methods (NOT inline object creation)
- ✅ Test runs and FAILS
- ✅ Failure message is clear and informative
- ✅ Test defines a small, specific E2E scenario
- ✅ Zero-Tolerance rules followed (TestRestTemplate, @Sql for DML only, Flyway for DDL)

## What NOT to Do

- ❌ Don't write implementation code yet
- ❌ Don't write multiple tests at once
- ❌ Don't skip running the test to verify failure
- ❌ Don't write tests that pass immediately
- ❌ Don't create objects inline in tests (use Fixture instead)
- ❌ Don't put DDL in @Sql files (use Flyway migration files)
- ❌ Don't use MockMvc (use TestRestTemplate)
- ❌ Don't use @WebMvcTest (use @SpringBootTest)

## 참고 문서

- [Integration Testing Overview](../../../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)
  - Flyway vs @Sql 명확한 구분
  - TestContainers 설정
  - Execution Flow

This is Kent Beck's TDD: Start with RED, make the failure explicit, and use IntegrationTestFixture for maintainability.
