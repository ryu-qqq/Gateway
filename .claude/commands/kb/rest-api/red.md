# REST API Layer TDD Red - Write Failing Test

You are in the 🔴 RED phase of Kent Beck's TDD cycle for **REST API Layer**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-rest-api-plan.md`
2. **Understand the requirement** for the current test
3. **Apply Tidy First**: If code needs cleanup, do Structural Changes FIRST → `struct:` 커밋
4. **Create TestFixture classes FIRST** (if not exists)
5. **Write the simplest failing test** using TestFixture
6. **Run the test** and verify it FAILS for the right reason
7. **Report the failure** clearly
8. **Commit with test: prefix**:
   ```bash
   git add .
   git commit -m "test: POST /api/orders 엔드포인트 테스트 추가"
   ```

## REST API Layer TestFixture Pattern (MANDATORY)

### Why TestFixture in REST API Layer?
- **Reusability**: Share Request/Response DTO creation across tests
- **Maintainability**: Change test data in one place
- **Mock Setup**: Consistent UseCase mock patterns
- **Integration Testing**: Support E2E test scenarios

### TestFixture Structure
```
rest-api/src/
├── main/java/
│   └── {basePackage}/restapi/
│       ├── controller/
│       │   └── OrderController.java
│       ├── dto/
│       │   ├── request/
│       │   │   ├── PlaceOrderRequest.java
│       │   │   └── CancelOrderRequest.java
│       │   └── response/
│       │       ├── OrderResponse.java
│       │       └── ErrorResponse.java
│       └── mapper/
│           └── OrderRequestMapper.java
└── testFixtures/java/
    └── {basePackage}/restapi/fixture/
        ├── PlaceOrderRequestFixture.java
        ├── CancelOrderRequestFixture.java
        └── OrderResponseFixture.java
```

### TestFixture Template (Request DTO)
```java
package com.company.template.restapi.fixture;

import com.company.template.restapi.dto.request.PlaceOrderRequest;

/**
 * TestFixture for PlaceOrderRequest.
 *
 * <p>Object Mother 패턴으로 Request DTO를 생성합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class PlaceOrderRequestFixture {

    private static final Long DEFAULT_CUSTOMER_ID = 1L;
    private static final Long DEFAULT_PRODUCT_ID = 100L;
    private static final Integer DEFAULT_QUANTITY = 10;

    /**
     * 기본 PlaceOrderRequest 생성.
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

    /**
     * Validation 실패 케이스 - 음수 quantity.
     */
    public static PlaceOrderRequest createWithNegativeQuantity() {
        return new PlaceOrderRequest(
            DEFAULT_CUSTOMER_ID,
            DEFAULT_PRODUCT_ID,
            -1
        );
    }

    private PlaceOrderRequestFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

### TestFixture Template (Response DTO)
```java
package com.company.template.restapi.fixture;

import com.company.template.restapi.dto.response.OrderResponse;
import com.company.template.domain.OrderStatus;

/**
 * TestFixture for OrderResponse.
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

## RED Phase Workflow with TestFixture

**Step 1: Create Fixtures FIRST**
```bash
# Create testFixtures directory structure
mkdir -p rest-api/src/testFixtures/java/{basePackage}/restapi/fixture/

# Create Fixture classes
touch rest-api/src/testFixtures/java/.../PlaceOrderRequestFixture.java
touch rest-api/src/testFixtures/java/.../OrderResponseFixture.java
```

**Step 2: Write Tests Using Fixtures**
```java
package com.company.template.restapi.controller;

import com.company.template.restapi.fixture.PlaceOrderRequestFixture;
import com.company.template.restapi.fixture.OrderResponseFixture;
import com.company.template.restapi.dto.request.PlaceOrderRequest;
import com.company.template.restapi.dto.response.OrderResponse;
import com.company.template.application.port.in.PlaceOrderPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaceOrderPort placeOrderUseCase;

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공")
    void shouldCreateOrder() throws Exception {
        // Given - Use Fixtures
        PlaceOrderRequest request = PlaceOrderRequestFixture.create();
        OrderResponse response = OrderResponseFixture.create();

        given(placeOrderUseCase.execute(any()))
            .willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.orderId").value(response.orderId()))
            .andExpect(jsonPath("$.status").value(response.status().name()));
    }

    @Test
    @DisplayName("POST /api/orders - customerId가 null이면 400 Bad Request")
    void shouldReturn400WhenCustomerIdIsNull() throws Exception {
        // Given - Use Fixture for invalid case
        PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNullCustomerId();

        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("고객 ID는 필수입니다"));
    }
}
```

## REST API Layer Specific Test Patterns

### 1. Controller Unit Test (MockMvc)
```java
@Test
@DisplayName("GET /api/orders/{orderId} - 주문 조회 성공")
void shouldGetOrder() throws Exception {
    // Given
    String orderId = "550e8400-e29b-41d4-a716-446655440000";
    OrderResponse response = OrderResponseFixture.createWithOrderId(orderId);

    given(loadOrderUseCase.loadById(orderId))
        .willReturn(response);

    // When & Then
    mockMvc.perform(get("/api/orders/{orderId}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(orderId))
        .andExpect(jsonPath("$.customerId").value(response.customerId()));
}

@Test
@DisplayName("GET /api/orders/{orderId} - 존재하지 않는 주문이면 404 Not Found")
void shouldReturn404WhenOrderNotFound() throws Exception {
    // Given
    String orderId = "non-existent-id";

    given(loadOrderUseCase.loadById(orderId))
        .willThrow(new OrderNotFoundException(orderId));

    // When & Then
    mockMvc.perform(get("/api/orders/{orderId}", orderId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
}
```

### 2. Request Validation Test
```java
@Test
@DisplayName("POST /api/orders - quantity가 음수면 400 Bad Request")
void shouldReturn400WhenQuantityIsNegative() throws Exception {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNegativeQuantity();

    // When & Then
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("수량은 양수여야 합니다"));
}

@Test
@DisplayName("POST /api/orders - productId가 null이면 400 Bad Request")
void shouldReturn400WhenProductIdIsNull() throws Exception {
    // Given
    PlaceOrderRequest request = new PlaceOrderRequest(1L, null, 10);

    // When & Then
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("상품 ID는 필수입니다"));
}
```

### 3. Mapper Test (Request → Command)
```java
@Test
@DisplayName("Request DTO → Command 변환")
void shouldMapRequestToCommand() {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    // When
    PlaceOrderCommand command = OrderRequestMapper.toCommand(request);

    // Then
    assertThat(command.customerId()).isEqualTo(request.customerId());
    assertThat(command.productId()).isEqualTo(request.productId());
    assertThat(command.quantity()).isEqualTo(request.quantity());
}

@Test
@DisplayName("Response → Response DTO 변환")
void shouldMapResponseToDto() {
    // Given
    OrderDomain domain = OrderDomainFixture.create();

    // When
    OrderResponse response = OrderResponseMapper.toResponse(domain);

    // Then
    assertThat(response.orderId()).isEqualTo(domain.getOrderId().getValue());
    assertThat(response.customerId()).isEqualTo(domain.getCustomerId());
    assertThat(response.status()).isEqualTo(domain.getStatus());
}
```

### 4. Integration Test (E2E)
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E - 주문 생성 및 조회")
    void shouldCreateAndGetOrder() {
        // Given
        PlaceOrderRequest request = PlaceOrderRequestFixture.create();

        // When - Create Order
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then - Verify Creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();
        OrderResponse createdOrder = createResponse.getBody();
        assertThat(createdOrder.orderId()).isNotNull();

        // When - Get Order
        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
            "/api/orders/" + createdOrder.orderId(),
            OrderResponse.class
        );

        // Then - Verify Retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderResponse retrievedOrder = getResponse.getBody();
        assertThat(retrievedOrder.orderId()).isEqualTo(createdOrder.orderId());
    }
}
```

### 5. Error Handling Test
```java
@Test
@DisplayName("GlobalExceptionHandler - DomainException 처리")
void shouldHandleDomainException() throws Exception {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    given(placeOrderUseCase.execute(any()))
        .willThrow(new InvalidOrderStateException("PLACED 상태가 아닙니다"));

    // When & Then
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").exists())
        .andExpect(jsonPath("$.message").value("PLACED 상태가 아닙니다"));
}

@Test
@DisplayName("GlobalExceptionHandler - ResourceNotFoundException 처리")
void shouldHandleResourceNotFoundException() throws Exception {
    // Given
    String orderId = "non-existent-id";

    given(loadOrderUseCase.loadById(orderId))
        .willThrow(new OrderNotFoundException(orderId));

    // When & Then
    mockMvc.perform(get("/api/orders/{orderId}", orderId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
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
- Use `@WebMvcTest` for Controller unit tests
- Use `@SpringBootTest` for E2E integration tests

## Success Criteria

- ✅ TestFixture classes created in `testFixtures/` directory
- ✅ Test written with clear, descriptive name
- ✅ Test uses Fixture.create() methods (NOT inline object creation)
- ✅ Test runs and FAILS
- ✅ Failure message is clear and informative
- ✅ Test defines a small, specific increment of functionality
- ✅ Zero-Tolerance rules followed (RESTful 설계, DTO 패턴, Validation)

## What NOT to Do

- ❌ Don't write implementation code yet
- ❌ Don't write multiple tests at once
- ❌ Don't skip running the test to verify failure
- ❌ Don't write tests that pass immediately
- ❌ Don't create objects inline in tests (use Fixture instead)
- ❌ Don't expose Domain/Entity directly in Response DTO
- ❌ Don't skip Validation annotations in Request DTO

This is Kent Beck's TDD: Start with RED, make the failure explicit, and use TestFixture for maintainability.
