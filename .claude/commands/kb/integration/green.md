# Integration Layer TDD Green - Make Test Pass

You are in the GREEN phase of Kent Beck's TDD cycle for **Integration Layer (E2E Testing)**.

## Instructions

1. **Read the failing test** carefully
2. **Write MINIMUM code** to make test pass (full layer implementation)
3. **Run the test** and verify it PASSES
4. **Report success** clearly

## Green Phase Principles

- **Minimum code to pass**: No extra features, no abstractions yet
- **Full layer implementation**: Controller → UseCase → Repository → Database
- **Real dependencies**: No mocking (except external APIs with WireMock)
- **TestRestTemplate**: Actual HTTP requests
- **@Transactional rollback**: Automatic data cleanup

## Implementation Pattern

### Step 1: Controller (REST API Layer)
```java
package com.company.template.restapi.controller;

import com.company.template.application.port.in.PlaceOrderUseCase;
import com.company.template.restapi.dto.request.PlaceOrderRequest;
import com.company.template.restapi.dto.response.OrderResponse;
import com.company.template.restapi.mapper.OrderRequestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Order REST API Controller.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;

    /**
     * 주문 생성.
     *
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 응답 DTO
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody PlaceOrderRequest request) {
        // 1. Request DTO → Command 변환
        PlaceOrderCommand command = OrderRequestMapper.toCommand(request);

        // 2. UseCase 호출
        OrderResponse response = placeOrderUseCase.execute(command);

        // 3. HTTP 201 Created 반환 (Location 헤더 포함)
        URI location = URI.create("/api/orders/" + response.orderId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * 주문 조회.
     *
     * @param orderId 주문 ID
     * @return 주문 응답 DTO
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse response = loadOrderUseCase.loadById(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 주문 취소.
     *
     * @param orderId 주문 ID
     * @return No Content (204)
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        CancelOrderCommand command = new CancelOrderCommand(orderId);
        cancelOrderUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 2: UseCase (Application Layer)
```java
package com.company.template.application.usecase;

import com.company.template.application.port.in.PlaceOrderUseCase;
import com.company.template.application.port.in.command.PlaceOrderCommand;
import com.company.template.application.port.out.SaveOrderPort;
import com.company.template.domain.Order;
import com.company.template.restapi.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PlaceOrderUseCase 구현체.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {

    private final SaveOrderPort saveOrderPort;

    /**
     * 주문 생성 UseCase 실행.
     *
     * @param command 주문 생성 Command
     * @return 생성된 주문 응답 DTO
     */
    @Override
    public OrderResponse execute(PlaceOrderCommand command) {
        // 1. Command → Domain 변환
        Order order = Order.create(
            command.customerId(),
            command.productId(),
            command.quantity()
        );

        // 2. Domain 비즈니스 메서드 호출
        order.place();

        // 3. Port 호출 (Persistence)
        Order savedOrder = saveOrderPort.save(order);

        // 4. Domain → Response DTO 변환
        return OrderResponseMapper.toResponse(savedOrder);
    }
}
```

### Step 3: Repository (Persistence Layer)
```java
package com.company.template.persistence.adapter;

import com.company.template.application.port.out.SaveOrderPort;
import com.company.template.domain.Order;
import com.company.template.persistence.entity.OrderJpaEntity;
import com.company.template.persistence.mapper.OrderEntityMapper;
import com.company.template.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Order Save Adapter (Command).
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class OrderSaveAdapter implements SaveOrderPort {

    private final OrderJpaRepository orderJpaRepository;

    /**
     * Order 저장.
     *
     * @param order 저장할 Order Domain
     * @return 저장된 Order Domain
     */
    @Override
    public Order save(Order order) {
        // 1. Domain → Entity 변환
        OrderJpaEntity entity = OrderEntityMapper.toEntity(order);

        // 2. JPA Repository 저장
        OrderJpaEntity savedEntity = orderJpaRepository.save(entity);

        // 3. Entity → Domain 변환
        return OrderEntityMapper.toDomain(savedEntity);
    }
}
```

### Step 4: JPA Entity (Persistence Layer)
```java
package com.company.template.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Order JPA Entity.
 *
 * <p>✅ Long FK 전략 - 관계 어노테이션 금지</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // ✅ Long FK (관계 어노테이션 금지)

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /**
     * Order JPA Entity 생성자.
     *
     * @param customerId  고객 ID
     * @param status      주문 상태
     * @param totalAmount 총 금액
     * @param orderDate   주문 일자
     */
    public OrderJpaEntity(
        Long customerId,
        String status,
        Long totalAmount,
        LocalDate orderDate
    ) {
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
    }

    /**
     * 주문 상태 변경.
     *
     * @param status 새로운 주문 상태
     */
    public void changeStatus(String status) {
        this.status = status;
    }
}
```

### Step 5: JPA Repository (Persistence Layer)
```java
package com.company.template.persistence.repository;

import com.company.template.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Order JPA Repository.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    /**
     * customerId로 주문 조회.
     *
     * @param customerId 고객 ID
     * @return 주문 Entity Optional
     */
    Optional<OrderJpaEntity> findByCustomerId(Long customerId);
}
```

### Step 6: Domain (Domain Layer)
```java
package com.company.template.domain;

import java.time.LocalDate;

/**
 * Order Aggregate Root.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class Order {

    private final OrderId orderId;
    private final Long customerId;
    private OrderStatus status;
    private final Long totalAmount;
    private final LocalDate orderDate;

    /**
     * Order 생성자.
     */
    private Order(
        OrderId orderId,
        Long customerId,
        OrderStatus status,
        Long totalAmount,
        LocalDate orderDate
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
    }

    /**
     * Order 생성 (Factory Method).
     *
     * @param customerId  고객 ID
     * @param productId   상품 ID
     * @param quantity    수량
     * @return 생성된 Order
     */
    public static Order create(Long customerId, Long productId, Integer quantity) {
        return new Order(
            OrderId.generate(),
            customerId,
            OrderStatus.PENDING,
            calculateAmount(productId, quantity),
            LocalDate.now()
        );
    }

    /**
     * 주문 생성 (비즈니스 메서드).
     */
    public void place() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be placed");
        }
        this.status = OrderStatus.PLACED;
    }

    /**
     * 주문 취소 (비즈니스 메서드).
     */
    public void cancel() {
        if (this.status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    // Getters
    public String getOrderIdValue() {
        return orderId.getValue();
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    private static Long calculateAmount(Long productId, Integer quantity) {
        // 간단한 계산 로직
        return productId * quantity;
    }
}
```

## Integration Test Configuration

### application-test.yml
```yaml
spring:
  datasource:
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
    url: jdbc:tc:mysql:8.0:///test?TC_DAEMON=true
    username: test
    password: test

  flyway:
    enabled: true                           # Flyway 활성화
    locations: classpath:db/migration       # 마이그레이션 파일 위치
    baseline-on-migrate: true
    clean-disabled: false                   # 테스트 시 DB 초기화 허용

  jpa:
    hibernate:
      ddl-auto: validate                    # Flyway가 스키마 관리, Hibernate는 검증만
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

logging:
  level:
    com.company.template: DEBUG
    org.hibernate.SQL: DEBUG
    org.springframework.jdbc.core: DEBUG
```

## External API Mocking (WireMock)

### WireMock Setup
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Order 통합 테스트 - 외부 API")
class OrderExternalApiIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

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
    @Sql("/sql/orders-test-data.sql")
    @DisplayName("E2E - 외부 결제 API 호출 포함 주문 생성")
    void shouldCreateOrderWithExternalPaymentApi() {
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
}
```

## Error Handling

### GlobalExceptionHandler
```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * 도메인 예외 처리.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 리소스 없음 예외 처리.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.of(
            "RESOURCE_NOT_FOUND",
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Validation 예외 처리.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_FAILED", message);
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
```

## Core Principles

- **Minimum code**: Only write code to make test pass
- **Full layer implementation**: Controller → UseCase → Repository → Database
- **No abstractions yet**: Don't extract patterns or common code
- **No extra features**: Stick to test requirements exactly
- **Real dependencies**: No mocking (except external APIs)
- **TestRestTemplate**: Actual HTTP requests
- **@Transactional rollback**: Automatic data cleanup

## Success Criteria

- ✅ Test runs and PASSES
- ✅ Full layer implementation complete (Controller → UseCase → Repository → Database)
- ✅ Real database interactions (via TestContainers)
- ✅ No mocking (except external APIs with WireMock)
- ✅ Code is simple and direct (no abstractions yet)
- ✅ Zero-Tolerance rules followed (Long FK, Law of Demeter, Transaction 경계)

## What NOT to Do

- ❌ Don't add extra features not in test
- ❌ Don't extract abstractions yet (wait for REFACTOR phase)
- ❌ Don't optimize code (wait for REFACTOR phase)
- ❌ Don't add logging, monitoring, etc. (wait for requirements)
- ❌ Don't mock real dependencies (use TestContainers)

## 참고 문서

- [Integration Testing Overview](../../../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)
  - Full Layer Implementation Pattern
  - TestContainers Setup
  - WireMock Integration

This is Kent Beck's TDD: Make it pass with minimum code, full layer implementation.
