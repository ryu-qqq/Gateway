---
description: REST API Layer Doc-Driven 구현. Controller, Request/Response DTO, Mapper 생성. 구현 + 테스트 동시 작성.
tags: [project]
---

# /impl rest-api - REST API Layer Implementation

**Doc-Driven Development**로 REST API Layer 신규 코드를 생성합니다.

## 사용법

```bash
/impl rest-api {feature-name}
/impl rest-api order-cancel
/impl rest-api member-register
```

## 실행 프로세스

```
/impl rest-api cancel-order
        ↓
┌─────────────────────────────────────────────────┐
│ 1️⃣ Plan 로드 (Serena memory)                    │
│    - read_memory("plan-{feature}")              │
│    - API 스펙 확인                               │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 2️⃣ REST API Skill 활성화                        │
│    - rest-api-expert 규칙 적용                   │
│    - Zero-Tolerance 검증                        │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 3️⃣ 구현 + 테스트 동시 작성                       │
│    - Controller                                │
│    - Request DTO                               │
│    - Response DTO                              │
│    - REST Mapper                               │
│    - Integration Test (TestRestTemplate)       │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 4️⃣ 검증 및 커밋                                 │
│    - ./gradlew test                            │
│    - feat: 커밋 (테스트 포함)                    │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 5️⃣ Memory 업데이트                              │
│    - Plan 진행상태 업데이트                       │
└─────────────────────────────────────────────────┘
```

---

## Zero-Tolerance 규칙 (필수)

### ✅ MUST
- **RESTful 설계**: 리소스 중심 URI, HTTP 메서드 의미 준수
- **DTO 분리**: Request/Response DTO 필수 분리
- **Validation**: `@Valid` + Bean Validation 필수
- **TestRestTemplate**: 통합 테스트 필수 (MockMvc 금지)

### ❌ NEVER
```java
// ❌ MockMvc 사용 금지
@WebMvcTest
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;  // 🚨 금지!
}

// ❌ Domain 직접 노출
@PostMapping("/orders")
public Order createOrder(@RequestBody Order order) {  // Domain 노출 금지
    return orderService.create(order);
}

// ❌ Validation 누락
@PostMapping("/orders")
public OrderResponse create(@RequestBody CreateOrderRequest request) {  // @Valid 누락
    // ...
}
```

---

## 생성 대상

### 1. Controller

```java
// adapter-in/rest-api/src/main/java/{basePackage}/adapter/in/rest/{feature}/
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {

    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderRestMapper orderRestMapper;

    public OrderCommandController(
            CancelOrderUseCase cancelOrderUseCase,
            OrderRestMapper orderRestMapper) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.orderRestMapper = orderRestMapper;
    }

    /**
     * 주문을 취소합니다.
     *
     * @param orderId 주문 ID
     * @param request 취소 요청 정보
     * @return 취소된 주문 정보
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        CancelOrderCommand command = orderRestMapper.toCommand(orderId, request);
        OrderResponse response = cancelOrderUseCase.cancel(command);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 2. Request DTO

```java
// adapter-in/rest-api/src/main/java/{basePackage}/adapter/in/rest/{feature}/dto/
public record CancelOrderRequest(
    @NotBlank(message = "취소 사유는 필수입니다")
    @Size(max = 200, message = "취소 사유는 200자 이내여야 합니다")
    String reason
) {
    public static CancelOrderRequest of(String reason) {
        return new CancelOrderRequest(reason);
    }
}
```

### 3. Response DTO

```java
// adapter-in/rest-api/src/main/java/{basePackage}/adapter/in/rest/{feature}/dto/
public record OrderResponse(
    String orderId,
    String status,
    BigDecimal totalPrice,
    LocalDateTime cancelledAt
) {
    public static OrderResponse of(
            String orderId,
            String status,
            BigDecimal totalPrice,
            LocalDateTime cancelledAt) {
        return new OrderResponse(orderId, status, totalPrice, cancelledAt);
    }
}
```

### 4. REST Mapper

```java
// adapter-in/rest-api/src/main/java/{basePackage}/adapter/in/rest/{feature}/mapper/
@Component
public class OrderRestMapper {

    /**
     * Request → Command 변환
     */
    public CancelOrderCommand toCommand(Long orderId, CancelOrderRequest request) {
        return CancelOrderCommand.of(orderId, request.reason());
    }

    /**
     * Application Response → REST Response 변환 (필요 시)
     */
    public OrderResponse toRestResponse(
            com.company.application.order.dto.OrderResponse appResponse) {
        return OrderResponse.of(
            appResponse.orderId(),
            appResponse.status(),
            appResponse.totalPrice(),
            appResponse.cancelledAt()
        );
    }
}
```

### 5. API Response Wrapper

```java
// adapter-in/rest-api/src/main/java/{basePackage}/adapter/in/rest/common/
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message));
    }

    public record ErrorInfo(String code, String message) {}
}
```

---

## 테스트 작성

### Integration Test (TestRestTemplate 필수)

```java
// adapter-in/rest-api/src/test/java/{basePackage}/adapter/in/rest/{feature}/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderCommandControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel - 주문 취소 성공")
    void shouldCancelOrder() {
        // Given
        OrderEntity order = orderJpaRepository.save(
            OrderEntityFixture.createPlaced()
        );

        CancelOrderRequest request = CancelOrderRequest.of("고객 요청");

        // When
        ResponseEntity<ApiResponse<OrderResponse>> response = restTemplate.postForEntity(
            "/api/v1/orders/{orderId}/cancel",
            request,
            new ParameterizedTypeReference<ApiResponse<OrderResponse>>() {},
            order.getId()
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel - Validation 실패")
    void shouldReturnBadRequestWhenReasonIsBlank() {
        // Given
        CancelOrderRequest request = CancelOrderRequest.of("");

        // When
        ResponseEntity<ApiResponse<Object>> response = restTemplate.postForEntity(
            "/api/v1/orders/1/cancel",
            request,
            new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/orders/{id}/cancel - 존재하지 않는 주문")
    void shouldReturnNotFoundWhenOrderNotExists() {
        // Given
        CancelOrderRequest request = CancelOrderRequest.of("고객 요청");

        // When
        ResponseEntity<ApiResponse<Object>> response = restTemplate.postForEntity(
            "/api/v1/orders/99999/cancel",
            request,
            new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

### REST Mapper Unit Test

```java
// adapter-in/rest-api/src/test/java/{basePackage}/adapter/in/rest/{feature}/mapper/
class OrderRestMapperTest {

    private OrderRestMapper sut;

    @BeforeEach
    void setUp() {
        sut = new OrderRestMapper();
    }

    @Test
    @DisplayName("Request → Command 변환")
    void shouldMapRequestToCommand() {
        // Given
        Long orderId = 1L;
        CancelOrderRequest request = CancelOrderRequest.of("고객 요청");

        // When
        CancelOrderCommand command = sut.toCommand(orderId, request);

        // Then
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.reason()).isEqualTo("고객 요청");
    }
}
```

---

## RESTful 설계 규칙

### HTTP 메서드 매핑

| 작업 | HTTP 메서드 | URI 패턴 | 예시 |
|------|------------|---------|------|
| 생성 | POST | /resources | POST /orders |
| 조회 (단건) | GET | /resources/{id} | GET /orders/1 |
| 조회 (목록) | GET | /resources | GET /orders?status=PLACED |
| 수정 | PUT/PATCH | /resources/{id} | PUT /orders/1 |
| 삭제 | DELETE | /resources/{id} | DELETE /orders/1 |
| 행위 | POST | /resources/{id}/action | POST /orders/1/cancel |

### 상태 코드 규칙

| 상황 | 상태 코드 |
|------|----------|
| 성공 | 200 OK |
| 생성 성공 | 201 Created |
| Validation 실패 | 400 Bad Request |
| 인증 실패 | 401 Unauthorized |
| 권한 없음 | 403 Forbidden |
| 리소스 없음 | 404 Not Found |
| 비즈니스 규칙 위반 | 422 Unprocessable Entity |
| 서버 오류 | 500 Internal Server Error |

---

## 커밋 규칙

```bash
# 구현 + 테스트 함께 커밋
git commit -m "feat: 주문 취소 REST API 구현

- POST /api/v1/orders/{id}/cancel 엔드포인트
- CancelOrderRequest DTO 추가
- OrderRestMapper 추가
- Integration 테스트 추가"
```

---

## Memory 업데이트

구현 완료 후 Plan 상태 업데이트:

```python
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle="- [ ] REST API Layer",
    repl="- [x] REST API Layer (completed)",
    mode="literal"
)
```

---

## 다음 단계

REST API Layer 완료 후:

```bash
# 전체 통합 테스트 실행
./gradlew test

# 모든 Layer 완료 확인
mcp__serena__read_memory(memory_file_name="plan-{feature}")
```

## 완료 체크리스트

- [ ] Controller 생성 (RESTful 설계)
- [ ] Request DTO 생성 (@Valid 적용)
- [ ] Response DTO 생성
- [ ] REST Mapper 생성
- [ ] Integration Test 작성 (TestRestTemplate)
- [ ] Validation 테스트 작성
- [ ] 에러 케이스 테스트 작성
- [ ] ./gradlew test 통과
- [ ] feat: 커밋 완료
- [ ] Memory 업데이트 완료
