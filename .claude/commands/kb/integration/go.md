# Integration Layer TDD Go - Execute Next Test from Plan

You are executing the Kent Beck TDD workflow for **Integration Layer (E2E Testing)**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-integration-plan.md`
2. **Find the next unmarked test** in the Integration Layer section
3. **Mark the test as in-progress** by adding a checkbox or marker
4. **Execute the TDD Cycle**:
   - **RED**: Write the simplest failing test first
   - **GREEN**: Implement minimum code to make the test pass
   - **REFACTOR**: Improve structure only after tests pass
   - **TIDY**: Clean up tests using IntegrationTestFixture pattern
5. **Run all tests** (exclude long-running tests with `@Tag("integration")`)
6. **Verify** all tests pass before proceeding
7. **Mark test complete** in plan file

## Integration Layer Specific Rules

### Zero-Tolerance Rules (MUST follow)

**✅ Mandatory (7개)**:
1. `@SpringBootTest(webEnvironment = RANDOM_PORT)` - Full Spring context
2. `TestRestTemplate` - Actual HTTP requests (NOT MockMvc)
3. `@Transactional + @Rollback(true)` - Data isolation
4. Flyway for schema - `spring.flyway.enabled=true` (DDL)
5. `@Sql` for test data ONLY - INSERT only (DML)
6. `@ActiveProfiles("test")` - Test-specific configuration
7. `@Testcontainers` - Real database (MySQL container)

**❌ Prohibited (7개)**:
1. No DDL in @Sql files - CREATE TABLE prohibited
2. No modifying Flyway migration files
3. No @WebMvcTest in integration tests
4. No MockMvc - use TestRestTemplate
5. No excessive @MockBean - minimize mocking
6. No EntityManager.persist() direct calls
7. No excessive @DirtiesContext - use @Transactional instead

### IntegrationTestFixture Pattern (MANDATORY)

**Integration Layer에서는 IntegrationTestFixture가 필수입니다**:

```java
// ✅ CORRECT (Use IntegrationTestFixture)
@Test
@Sql("/sql/orders-test-data.sql")  // Flyway 후 실행 (DML만)
@DisplayName("E2E - 주문 생성 및 조회")
void shouldCreateAndGetOrder() {
    // Given - Use Fixture
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();

    // When - 실제 HTTP POST
    ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
        "/api/orders",
        request,
        OrderResponse.class
    );

    // Then - 생성 검증
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String orderId = createResponse.getBody().orderId();

    // When - 실제 HTTP GET
    ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
        "/api/orders/" + orderId,
        OrderResponse.class
    );

    // Then - 조회 검증
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().orderId()).isEqualTo(orderId);
}

// ❌ WRONG (No Fixture, Inline object creation)
@Test
void shouldCreateOrder() {
    PlaceOrderRequest request = new PlaceOrderRequest(1L, 100L, 10);
    // ...
}
```

**Fixture 위치**: `bootstrap-web-api/src/testFixtures/java/{basePackage}/integration/fixture/`

### Integration Test Focus
- **E2E Testing**:
  - HTTP → Controller → UseCase → Repository → Real DB
  - TestRestTemplate 기반 실제 HTTP 요청/응답 검증
  - Full Spring context loading
- **Test Data Management**:
  - Flyway: Schema (DDL) - `CREATE TABLE`
  - @Sql: Test data (DML) - `INSERT`
  - @Transactional: Automatic rollback
- **External Dependencies**:
  - WireMock: 외부 API mocking
  - TestContainers: Real database (MySQL)

## Core Principles

- Write ONE test at a time
- Make it run with minimum code
- Improve structure ONLY after green
- Run ALL tests after each change
- Never skip the Red phase
- Never mix structural and behavioral changes
- **ALWAYS use IntegrationTestFixture** (Integration Layer 필수!)
- **ALWAYS use @Sql for test data ONLY** (DML only, NOT DDL)

## Success Criteria

- ✅ Plan file updated (test marked as in-progress)
- ✅ Test written and initially failing (RED)
- ✅ Minimum code makes test pass (GREEN)
- ✅ Code structure improved if needed (REFACTOR)
- ✅ IntegrationTestFixture used (NOT inline object creation)
- ✅ All tests passing
- ✅ Zero-Tolerance rules followed (TestRestTemplate, @Sql for DML only, Flyway for DDL)
- ✅ Test marked complete in plan file

## What NOT to Do

- ❌ Don't use MockMvc (use TestRestTemplate)
- ❌ Don't create tests without IntegrationTestFixture
- ❌ Don't put DDL in @Sql files (use Flyway migration files)
- ❌ Don't modify Flyway migration files for test data
- ❌ Don't use @WebMvcTest (use @SpringBootTest)

## Example Workflow

```bash
# 1. User: /kb-integration /go
# 2. Claude: Reads docs/prd/plans/PROJ-123-integration-plan.md
# 3. Claude: Finds next test: "E2E - 주문 생성 및 조회"
# 4. Claude: Marks test as in-progress
# 5. Claude: RED - Writes failing TestRestTemplate test (uses IntegrationTestFixture)
# 6. Claude: GREEN - Implements full layer (Controller → UseCase → Repository)
# 7. Claude: REFACTOR - Extracts IntegrationTestSupport
# 8. Claude: TIDY - Ensures IntegrationTestFixture is used properly
# 9. Claude: Runs all tests (./gradlew test)
# 10. Claude: Marks test as complete
```

## Integration Testing Architecture

```
HTTP Request (TestRestTemplate)
    ↓
Controller (REST API Layer)
    ↓
UseCase (Application Layer)
    ↓
Repository (Persistence Layer)
    ↓
Real Database (MySQL via TestContainers)
    ↓
HTTP Response
```

## Execution Flow

```
1. TestContainers start → MySQL container
2. Spring Boot start → Full context
3. Flyway auto-run → V1, V2, V3... tables created (DDL)
4. @Sql execution → INSERT test data (DML)
5. Test method execution → Full HTTP flow
6. @Transactional rollback → Data cleanup
7. Repeat for next test
8. TestContainers stop → Container deleted
```

## Integration Test Template Example

```java
// ✅ CORRECT (Integration Test with Fixture)
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
    @Sql("/sql/orders-test-data.sql")  // Test data only (INSERT)
    @DisplayName("E2E - 주문 생성 및 조회")
    void shouldCreateAndGetOrder() {
        // Given - Use Fixture
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
}

// ❌ WRONG (MockMvc, No Fixture, DDL in @Sql)
@WebMvcTest(OrderController.class)  // ❌ Use @SpringBootTest
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;  // ❌ Use TestRestTemplate

    @Test
    @Sql("/sql/create-order-table.sql")  // ❌ No DDL in @Sql
    void shouldCreateOrder() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 100L, 10);  // ❌ No Fixture
        // ...
    }
}
```

## Test Data Management

### Flyway Migration (DDL)
```sql
-- src/main/resources/db/migration/V1__create_order_table.sql
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

### @Sql Test Data (DML)
```sql
-- src/test/resources/sql/orders-test-data.sql
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
```

## External API Mocking (WireMock)

```java
@Test
@DisplayName("E2E - 외부 API 호출 포함 주문 생성")
void shouldCreateOrderWithExternalApi() {
    // Given - WireMock stub
    stubFor(post(urlEqualTo("/payment"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"status\":\"success\"}")));

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
    verify(postRequestedFor(urlEqualTo("/payment")));
}
```

## 참고 문서

- [Integration Testing Overview](../../../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)
  - Flyway vs @Sql 구분
  - TestContainers 설정
  - E2E 테스트 패턴

Follow the workflow from CLAUDE.md precisely. Stop and report if any step fails.
