# REST API Layer TDD Go - Execute Next Test from Plan

You are executing the Kent Beck TDD + Tidy First workflow for **REST API Layer**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-rest-api-plan.md`
2. **Find the next unmarked test** in the REST API Layer section
3. **Mark the test as in-progress** by adding a checkbox or marker
4. **Execute the TDD Cycle (3 phases)**:
   - **🔴 RED**: Write the simplest failing test first → `test:` 커밋
   - **🟢 GREEN**: Implement minimum code to make the test pass → `feat:` 커밋
   - **♻️ REFACTOR**: Improve structure only after tests pass → `struct:` 커밋
5. **Apply Tidy First** principle:
   - If Structural Changes needed, do them FIRST → `struct:` 커밋
   - Then proceed with Behavioral Changes (Red → Green)
   - Never mix Structural and Behavioral in same commit
6. **Run all tests** (excluding long-running tests)
7. **Verify** all tests pass before proceeding
8. **Mark test complete** in plan file

## REST API Layer Specific Rules

### Zero-Tolerance Rules (MUST follow)
- ✅ **RESTful 설계**: HTTP 메서드 및 상태 코드 올바르게 사용
- ✅ **DTO 패턴**: Request/Response DTO 분리 (Domain/Entity 직접 노출 금지)
- ✅ **Validation**: `@Valid` + `@NotNull`, `@NotBlank` 등 Bean Validation 사용
- ✅ **Error Handling**: `@RestControllerAdvice`로 전역 예외 처리
- ✅ **MockMvc 테스트**: Controller는 MockMvc로 테스트 (단위 테스트)
- ✅ **Javadoc 필수**: Controller, DTO에 `@author`, `@since` 포함

### TestFixture Pattern (MANDATORY)
**REST API Layer에서는 TestFixture가 필수입니다**:

```java
// ✅ CORRECT (Use Fixture)
@Test
@DisplayName("POST /api/orders - 주문 생성 성공")
void shouldCreateOrder() throws Exception {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.create();
    OrderResponse response = OrderResponseFixture.create();

    given(placeOrderUseCase.execute(any(PlaceOrderCommand.class)))
        .willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").value(response.orderId()))
        .andExpect(jsonPath("$.status").value(response.status().name()));
}

// ❌ WRONG (Inline object creation)
@Test
void shouldCreateOrder() throws Exception {
    PlaceOrderRequest request = new PlaceOrderRequest(1L, 100L, 10);
    // ...
}
```

**Fixture 위치**: `rest-api/src/testFixtures/java/{basePackage}/restapi/fixture/`

### REST API Test Focus
- **Controller 단위 테스트**:
  - MockMvc 기반 HTTP 요청/응답 검증
  - Request DTO Validation 테스트
  - Error Response 테스트 (400, 404, 500 등)
- **Mapper 테스트**:
  - Request DTO → Command 변환
  - Domain/Response → Response DTO 변환
- **Integration 테스트**:
  - `@SpringBootTest` + TestRestTemplate (E2E)
  - 실제 HTTP 요청/응답 테스트

## Core Principles (Kent Beck + Tidy First)

- **Tidy First**: Structural Changes BEFORE Behavioral Changes
- **Never mix** Structural and Behavioral in same commit
- **3 commit types**: `test:` (Red) → `feat:` (Green) → `struct:` (Refactor)
- Write ONE test at a time
- Make it run with minimum code
- Improve structure ONLY after green
- Run ALL tests after each change
- Never skip the Red phase
- **ALWAYS use TestFixture** (REST API Layer 필수!)

## Success Criteria

- ✅ Plan file updated (test marked as in-progress)
- ✅ Structural Changes (if needed) → `struct:` 커밋
- ✅ Test written and initially failing (RED) → `test:` 커밋
- ✅ Minimum code makes test pass (GREEN) → `feat:` 커밋
- ✅ Code structure improved if needed (REFACTOR) → `struct:` 커밋
- ✅ TestFixture used (NOT inline object creation)
- ✅ All tests passing
- ✅ Zero-Tolerance rules followed (RESTful 설계, DTO 패턴, Validation, Error Handling)
- ✅ Commit messages follow prefix rules (`test:`/`feat:`/`struct:`)
- ✅ Test marked complete in plan file

## What NOT to Do

- ❌ Don't work on Domain, Application, or Persistence code
- ❌ Don't create tests without TestFixture
- ❌ Don't expose Domain/Entity directly in API (use Response DTO)
- ❌ Don't skip Validation (`@Valid` 필수)
- ❌ Don't ignore HTTP status codes (201 Created, 400 Bad Request, etc.)

## Example Workflow

```bash
# 1. User: /kb/rest-api/go
# 2. Claude: Reads docs/prd/plans/PROJ-123-rest-api-plan.md
# 3. Claude: Finds next test: "POST /api/orders - 주문 생성"
# 4. Claude: Marks test as in-progress

# 5. 🔴 RED Phase
#    - Writes failing MockMvc test (uses PlaceOrderRequestFixture)
#    - git commit -m "test: POST /api/orders 엔드포인트 테스트 추가"

# 6. 🟢 GREEN Phase
#    - Implements OrderController.createOrder() (minimum code)
#    - Follows Zero-Tolerance rules (RESTful 설계, DTO 패턴, Validation)
#    - git commit -m "feat: POST /api/orders 엔드포인트 구현 (최소 코드)"

# 7. ♻️ REFACTOR Phase (if needed)
#    - Extracts Request validation logic (NO behavior change)
#    - git commit -m "struct: extract validation logic in OrderController"

# 8. Claude: Runs all tests (./gradlew test)
# 9. Claude: Marks test as complete
```

## RESTful API Design Example

```java
// ✅ CORRECT (RESTful 설계)
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
     * @author Claude Code
     * @since 2025-01-13
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
     * @author Claude Code
     * @since 2025-01-13
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
     * @param request 취소 요청 DTO
     * @return No Content (204)
     * @author Claude Code
     * @since 2025-01-13
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
        @PathVariable String orderId,
        @Valid @RequestBody CancelOrderRequest request
    ) {
        CancelOrderCommand command = OrderRequestMapper.toCancelCommand(orderId, request);
        cancelOrderUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}

// ❌ WRONG (비RESTful 설계)
@RestController
public class OrderController {

    @PostMapping("/createOrder")  // ❌ 동사 사용
    public OrderDomain createOrder(@RequestBody OrderDomain order) {  // ❌ Domain 노출
        return orderService.save(order);
    }

    @GetMapping("/getOrder")  // ❌ GET은 Path Variable 사용
    public OrderDomain getOrder(@RequestParam String id) {
        return orderService.findById(id);
    }
}
```

## Request DTO Validation Example

```java
// ✅ CORRECT (Request DTO with Validation)
/**
 * 주문 생성 요청 DTO.
 *
 * @param customerId 고객 ID
 * @param productId 상품 ID
 * @param quantity 수량
 * @author Claude Code
 * @since 2025-01-13
 */
public record PlaceOrderRequest(
    @NotNull(message = "고객 ID는 필수입니다")
    @Positive(message = "고객 ID는 양수여야 합니다")
    Long customerId,

    @NotNull(message = "상품 ID는 필수입니다")
    @Positive(message = "상품 ID는 양수여야 합니다")
    Long productId,

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 양수여야 합니다")
    Integer quantity
) {}

// MockMvc Test with Validation
@Test
@DisplayName("POST /api/orders - 수량이 null이면 400 Bad Request")
void shouldReturn400WhenQuantityIsNull() throws Exception {
    // Given
    PlaceOrderRequest request = PlaceOrderRequestFixture.createWithNullQuantity();

    // When & Then
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("수량은 필수입니다"));
}
```

## Error Handling Example

```java
// ✅ CORRECT (Global Exception Handler)
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * 도메인 예외 처리 (비즈니스 로직 예외).
     *
     * @param ex 도메인 예외
     * @return 400 Bad Request
     * @author Claude Code
     * @since 2025-01-13
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
     *
     * @param ex 리소스 없음 예외
     * @return 404 Not Found
     * @author Claude Code
     * @since 2025-01-13
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
     *
     * @param ex Validation 예외
     * @return 400 Bad Request
     * @author Claude Code
     * @since 2025-01-13
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

// Error Response DTO
public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, LocalDateTime.now());
    }
}
```

Follow the workflow from CLAUDE.md precisely. Stop and report if any step fails.
