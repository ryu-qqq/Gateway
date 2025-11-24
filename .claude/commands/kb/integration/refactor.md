# Integration Layer TDD Refactor - Improve Structure

You are in the REFACTOR phase of Kent Beck's TDD cycle for **Integration Layer (E2E Testing)**.

## Instructions

1. **Keep tests passing** - Run tests before and after each refactor
2. **Improve structure** - Extract common patterns, reduce duplication
3. **No behavior changes** - Tests must still pass with same assertions
4. **Focus on test code** - Refactor test setup, fixtures, and helpers

## Refactoring Principles

- **DRY**: Extract repeated setup code
- **Readability**: Make test intent clearer
- **Maintainability**: Reduce test brittleness
- **Performance**: Optimize TestContainers, @Sql usage
- **Test-First**: Only refactor test code, not production code (unless necessary)

## Integration Layer Refactoring Patterns

### 1. Extract IntegrationTestSupport Base Class

**Before**:
```java
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

    // Tests...
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Customer 통합 테스트")
class CustomerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    // Tests...
}
```

**After** (Extract Base Class):
```java
package com.company.template.integration.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration Test Support Base Class.
 *
 * <p>모든 E2E 테스트에서 상속받는 Base Class입니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class IntegrationTestSupport {

    @Container
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    protected TestRestTemplate restTemplate;
}
```

**Usage**:
```java
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    @Sql("/sql/orders-test-data.sql")
    @DisplayName("E2E - 주문 생성 및 조회")
    void shouldCreateAndGetOrder() {
        // Given
        PlaceOrderRequest request = PlaceOrderRequestFixture.create();

        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### 2. Extract Common API Call Helpers

**Before**:
```java
@Test
void shouldCreateAndGetOrder() {
    // Create
    ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
        "/api/orders",
        request,
        OrderResponse.class
    );
    String orderId = createResponse.getBody().orderId();

    // Get
    ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );
}
```

**After** (Extract Helper Methods):
```java
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * POST 요청 헬퍼.
     */
    protected <T, R> ResponseEntity<R> post(String url, T body, Class<R> responseType) {
        return restTemplate.postForEntity(url, body, responseType);
    }

    /**
     * GET 요청 헬퍼.
     */
    protected <R> ResponseEntity<R> get(String url, Class<R> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    /**
     * PUT 요청 헬퍼.
     */
    protected <T> void put(String url, T body) {
        restTemplate.put(url, body);
    }

    /**
     * DELETE 요청 헬퍼.
     */
    protected void delete(String url) {
        restTemplate.delete(url);
    }
}
```

**Usage**:
```java
@Test
void shouldCreateAndGetOrder() {
    // Create
    ResponseEntity<OrderResponse> createResponse = post(
        "/api/orders",
        PlaceOrderRequestFixture.create(),
        OrderResponse.class
    );
    String orderId = createResponse.getBody().orderId();

    // Get
    ResponseEntity<OrderResponse> getResponse = get(
        "/api/orders/" + orderId,
        OrderResponse.class
    );
}
```

### 3. Extract @Sql Data Management Helpers

**Before**:
```java
@Test
@Sql("/sql/orders-test-data.sql")
void shouldGetExistingOrder() {
    // Hardcoded order ID from @Sql file
    ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
        "/api/orders/100",
        OrderResponse.class
    );
}
```

**After** (Extract Constants):
```java
public abstract class IntegrationTestSupport {

    // @Sql 파일의 Test Data IDs
    protected static final Long TEST_CUSTOMER_ID = 1L;
    protected static final Long TEST_ORDER_ID_PENDING = 100L;
    protected static final Long TEST_ORDER_ID_CONFIRMED = 200L;
}
```

**Usage**:
```java
@Test
@Sql("/sql/orders-test-data.sql")
void shouldGetExistingOrder() {
    // Use constant instead of magic number
    ResponseEntity<OrderResponse> response = get(
        "/api/orders/" + TEST_ORDER_ID_PENDING,
        OrderResponse.class
    );
}
```

### 4. Extract WireMock Test Support

**Before**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class OrderExternalApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldCallExternalApi() {
        stubFor(post(urlEqualTo("/payment"))
            .willReturn(aResponse().withStatus(200)));
        // ...
    }
}
```

**After** (Extract WireMockTestSupport):
```java
package com.company.template.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock Test Support.
 *
 * <p>외부 API Mocking이 필요한 E2E 테스트에서 상속받습니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public abstract class WireMockTestSupport extends IntegrationTestSupport {

    protected WireMockServer wireMockServer;

    @BeforeEach
    void setUpWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDownWireMock() {
        wireMockServer.stop();
    }

    /**
     * 외부 API 성공 응답 Stub.
     */
    protected void stubExternalApiSuccess(String url, String responseBody) {
        stubFor(post(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)));
    }

    /**
     * 외부 API 실패 응답 Stub.
     */
    protected void stubExternalApiFailure(String url, int statusCode) {
        stubFor(post(urlEqualTo(url))
            .willReturn(aResponse()
                .withStatus(statusCode)));
    }
}
```

**Usage**:
```java
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
        ResponseEntity<OrderResponse> response = post(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify
        verify(postRequestedFor(urlEqualTo("/payment")));
    }
}
```

### 5. Optimize @Sql Usage (Reduce Duplication)

**Before**:
```java
@Test
@Sql("/sql/orders-test-data.sql")
void test1() { /* ... */ }

@Test
@Sql("/sql/orders-test-data.sql")
void test2() { /* ... */ }

@Test
@Sql("/sql/orders-test-data.sql")
void test3() { /* ... */ }
```

**After** (Class-Level @Sql):
```java
@Sql("/sql/orders-test-data.sql")  // ✅ Class-level
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("E2E - 주문 생성 및 조회")
    void test1() { /* ... */ }

    @Test
    @DisplayName("E2E - 주문 취소")
    void test2() { /* ... */ }

    @Test
    @DisplayName("E2E - 존재하지 않는 주문 조회")
    void test3() { /* ... */ }
}
```

### 6. Extract Assertion Helpers

**Before**:
```java
@Test
void shouldCreateOrder() {
    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().orderId()).isNotNull();
}
```

**After** (Extract Helper):
```java
public abstract class IntegrationTestSupport {

    /**
     * HTTP 201 Created 응답 검증.
     */
    protected <T> void assertCreated(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
    }

    /**
     * HTTP 200 OK 응답 검증.
     */
    protected <T> void assertOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    /**
     * HTTP 404 Not Found 응답 검증.
     */
    protected <T> void assertNotFound(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * HTTP 400 Bad Request 응답 검증.
     */
    protected <T> void assertBadRequest(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
```

**Usage**:
```java
@Test
void shouldCreateOrder() {
    ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
    assertCreated(response);
}
```

### 7. Extract Multi-Step Workflow Helpers

**Before**:
```java
@Test
void shouldCompleteOrderProcess() {
    // Step 1: Create
    ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
        "/api/orders",
        PlaceOrderRequestFixture.create(),
        OrderResponse.class
    );
    String orderId = createResponse.getBody().orderId();

    // Step 2: Confirm
    restTemplate.put("/api/orders/" + orderId + "/confirm", null);

    // Step 3: Ship
    restTemplate.put("/api/orders/" + orderId + "/ship", null);

    // Step 4: Complete
    restTemplate.put("/api/orders/" + orderId + "/complete", null);
}
```

**After** (Extract Helper):
```java
public abstract class IntegrationTestSupport {

    /**
     * 주문 생성 헬퍼.
     */
    protected String createOrder(PlaceOrderRequest request) {
        ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
        assertCreated(response);
        return response.getBody().orderId();
    }

    /**
     * 주문 상태 변경 헬퍼.
     */
    protected void changeOrderStatus(String orderId, String action) {
        put("/api/orders/" + orderId + "/" + action, null);
    }

    /**
     * 주문 조회 헬퍼.
     */
    protected OrderResponse getOrder(String orderId) {
        ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);
        assertOk(response);
        return response.getBody();
    }
}
```

**Usage**:
```java
@Test
void shouldCompleteOrderProcess() {
    // Step 1: Create
    String orderId = createOrder(PlaceOrderRequestFixture.create());

    // Step 2: Confirm
    changeOrderStatus(orderId, "confirm");
    assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.CONFIRMED);

    // Step 3: Ship
    changeOrderStatus(orderId, "ship");
    assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.SHIPPED);

    // Step 4: Complete
    changeOrderStatus(orderId, "complete");
    assertThat(getOrder(orderId).status()).isEqualTo(OrderStatus.COMPLETED);
}
```

## Final IntegrationTestSupport Structure

```java
package com.company.template.integration.support;

import com.company.template.restapi.dto.request.PlaceOrderRequest;
import com.company.template.restapi.dto.response.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test Support Base Class.
 *
 * <p>모든 E2E 테스트에서 상속받는 Base Class입니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class IntegrationTestSupport {

    @Container
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    protected TestRestTemplate restTemplate;

    // @Sql Test Data IDs
    protected static final Long TEST_CUSTOMER_ID = 1L;
    protected static final Long TEST_ORDER_ID_PENDING = 100L;
    protected static final Long TEST_ORDER_ID_CONFIRMED = 200L;

    // ===== HTTP Request Helpers =====

    protected <T, R> ResponseEntity<R> post(String url, T body, Class<R> responseType) {
        return restTemplate.postForEntity(url, body, responseType);
    }

    protected <R> ResponseEntity<R> get(String url, Class<R> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    protected <T> void put(String url, T body) {
        restTemplate.put(url, body);
    }

    protected void delete(String url) {
        restTemplate.delete(url);
    }

    // ===== Assertion Helpers =====

    protected <T> void assertCreated(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
    }

    protected <T> void assertOk(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    protected <T> void assertNotFound(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    protected <T> void assertBadRequest(ResponseEntity<T> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===== Domain-Specific Helpers =====

    protected String createOrder(PlaceOrderRequest request) {
        ResponseEntity<OrderResponse> response = post("/api/orders", request, OrderResponse.class);
        assertCreated(response);
        return response.getBody().orderId();
    }

    protected void changeOrderStatus(String orderId, String action) {
        put("/api/orders/" + orderId + "/" + action, null);
    }

    protected OrderResponse getOrder(String orderId) {
        ResponseEntity<OrderResponse> response = get("/api/orders/" + orderId, OrderResponse.class);
        assertOk(response);
        return response.getBody();
    }
}
```

## Core Principles

- **Keep tests passing**: Run tests before and after each refactor
- **Extract common patterns**: DRY principle
- **Improve readability**: Make test intent clearer
- **Maintain behavior**: No changes to test assertions
- **Test-First**: Focus on refactoring test code

## Success Criteria

- ✅ Tests still pass after refactoring
- ✅ IntegrationTestSupport base class created
- ✅ Common setup code extracted (TestContainers, TestRestTemplate)
- ✅ Helper methods reduce duplication
- ✅ Test code is more readable and maintainable
- ✅ No behavior changes (same assertions)

## What NOT to Do

- ❌ Don't change test behavior or assertions
- ❌ Don't add new tests (wait for next RED phase)
- ❌ Don't refactor production code (focus on test code)
- ❌ Don't optimize prematurely (extract patterns that exist)

## 참고 문서

- [Integration Testing Overview](../../../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)
  - IntegrationTestSupport Pattern
  - Test Helper Best Practices

This is Kent Beck's TDD: Refactor to improve structure while keeping tests green.
