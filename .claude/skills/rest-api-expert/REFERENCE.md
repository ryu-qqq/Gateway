# REST API Layer 상세 참조 가이드

## 1. Controller 설계 규칙

### 1.1 REST API 표준

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/controller-design/01_rest-api-conventions.md`

**HTTP 메서드와 엔드포인트 패턴**:

| 작업 | HTTP 메서드 | 엔드포인트 | 예시 |
|------|-------------|-----------|------|
| 목록 조회 | GET | `/api/v1/{resource}` | `GET /api/v1/orders` |
| 단건 조회 | GET | `/api/v1/{resource}/{id}` | `GET /api/v1/orders/1` |
| 생성 | POST | `/api/v1/{resource}` | `POST /api/v1/orders` |
| 전체 수정 | PUT | `/api/v1/{resource}/{id}` | `PUT /api/v1/orders/1` |
| 부분 수정 | PATCH | `/api/v1/{resource}/{id}` | `PATCH /api/v1/orders/1/status` |
| 삭제 | DELETE | `/api/v1/{resource}/{id}` | `DELETE /api/v1/orders/1` |

### 1.2 Controller 기본 구조

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor  // Constructor Injection
@Validated               // Class-level validation
public class OrderController {

    // 1. UseCase 의존성 주입
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final FindOrderUseCase findOrderUseCase;

    // 2. Mapper 의존성 주입
    private final OrderRequestMapper requestMapper;
    private final OrderResponseMapper responseMapper;

    // 3. API 메서드
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
        @Valid @RequestBody OrderRequest request
    ) {
        PlaceOrderCommand command = requestMapper.toCommand(request);
        OrderResult result = placeOrderUseCase.execute(command);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(responseMapper.toResponse(result));
    }
}
```

## 2. DTO 패턴

### 2.1 Request DTO

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/dto-patterns/01_request-dto.md`

**Record 패턴 + Validation**:

```java
public record OrderRequest(
    @NotBlank(message = "주문 번호는 필수입니다")
    @Size(min = 10, max = 20, message = "주문 번호는 10-20자여야 합니다")
    String orderNumber,

    @NotNull(message = "고객 ID는 필수입니다")
    @Positive(message = "고객 ID는 양수여야 합니다")
    Long customerId,

    @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다")
    @Size(max = 100, message = "주문 항목은 최대 100개입니다")
    List<OrderItemRequest> items,

    @NotNull(message = "배송 주소는 필수입니다")
    @Valid  // Nested validation
    AddressRequest shippingAddress
) {
    // Compact Constructor
    public OrderRequest {
        Objects.requireNonNull(orderNumber, "orderNumber must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);  // Immutable copy
    }
}
```

**Validation 어노테이션**:

| 어노테이션 | 용도 | 예시 |
|-----------|------|------|
| `@NotNull` | null 방지 | `@NotNull Long id` |
| `@NotBlank` | null, 빈 문자열, 공백 방지 | `@NotBlank String name` |
| `@NotEmpty` | null, 빈 컬렉션 방지 | `@NotEmpty List<Item> items` |
| `@Size` | 길이/크기 제한 | `@Size(min=1, max=100) String text` |
| `@Positive` | 양수 검증 | `@Positive Long count` |
| `@Email` | 이메일 형식 검증 | `@Email String email` |
| `@Pattern` | 정규식 검증 | `@Pattern(regexp="...") String code` |
| `@Valid` | 중첩 객체 검증 | `@Valid Address address` |

### 2.2 Response DTO

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/dto-patterns/02_response-dto.md`

**Immutable Record 패턴**:

```java
public record OrderResponse(
    Long orderId,
    String orderNumber,
    String status,
    Long customerId,
    String customerName,
    List<OrderItemResponse> items,
    AddressResponse shippingAddress,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Response는 Validation 불필요
    // Immutable이므로 별도 처리 없음
}

public record OrderItemResponse(
    Long productId,
    String productName,
    int quantity,
    BigDecimal price
) {}
```

### 2.3 DTO 네이밍 규칙

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/dto-patterns/04_dto-naming-convention.md`

| DTO 유형 | 네이밍 패턴 | 예시 |
|----------|------------|------|
| Request | `{Entity}Request` | `OrderRequest` |
| Response | `{Entity}Response` | `OrderResponse` |
| 생성 요청 | `Create{Entity}Request` | `CreateOrderRequest` |
| 수정 요청 | `Update{Entity}Request` | `UpdateOrderRequest` |
| 검색 조건 | `{Entity}SearchCriteria` | `OrderSearchCriteria` |
| 페이징 응답 | `{Entity}PageResponse` | `OrderPageResponse` |

## 3. Exception 처리

### 3.1 Global Exception Handler

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/exception-handling/01_global-exception-handler.md`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
        DomainException ex,
        HttpServletRequest request
    ) {
        log.warn("Domain exception occurred: path={}, error={}",
            request.getRequestURI(), ex.getMessage());

        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage()
            ))
            .toList();

        log.warn("Validation failed: path={}, errors={}",
            request.getRequestURI(), fieldErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.ofValidation(
                "VALIDATION_FAILED",
                "입력 검증에 실패했습니다",
                request.getRequestURI(),
                fieldErrors
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected exception occurred: path={}, error={}",
            request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다",
                request.getRequestURI()
            ));
    }
}
```

### 3.2 Error Response 형식

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/dto-patterns/03_error-response.md`

```java
public record ErrorResponse(
    String errorCode,
    String message,
    String path,
    LocalDateTime timestamp,
    List<FieldError> fieldErrors
) {
    public static ErrorResponse of(
        String errorCode,
        String message,
        String path
    ) {
        return new ErrorResponse(
            errorCode,
            message,
            path,
            LocalDateTime.now(),
            Collections.emptyList()
        );
    }

    public static ErrorResponse ofValidation(
        String errorCode,
        String message,
        String path,
        List<FieldError> fieldErrors
    ) {
        return new ErrorResponse(
            errorCode,
            message,
            path,
            LocalDateTime.now(),
            fieldErrors
        );
    }

    public record FieldError(String field, String message) {}
}
```

## 4. Mapper 패턴

### 4.1 Request Mapper

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/mapper-patterns/01_request-mapper.md`

```java
@Component
public class OrderRequestMapper {

    public PlaceOrderCommand toCommand(OrderRequest request) {
        return new PlaceOrderCommand(
            request.orderNumber(),
            request.customerId(),
            toItemCommands(request.items()),
            toAddressCommand(request.shippingAddress())
        );
    }

    private List<OrderItemCommand> toItemCommands(
        List<OrderItemRequest> items
    ) {
        return items.stream()
            .map(item -> new OrderItemCommand(
                item.productId(),
                item.quantity(),
                item.price()
            ))
            .toList();
    }

    private AddressCommand toAddressCommand(AddressRequest address) {
        return new AddressCommand(
            address.zipCode(),
            address.street(),
            address.city(),
            address.state()
        );
    }
}
```

### 4.2 Response Mapper

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/mapper-patterns/02_response-mapper.md`

```java
@Component
public class OrderResponseMapper {

    public OrderResponse toResponse(OrderResult result) {
        return new OrderResponse(
            result.orderId(),
            result.orderNumber(),
            result.status().name(),
            result.customerId(),
            result.customerName(),
            toItemResponses(result.items()),
            toAddressResponse(result.shippingAddress()),
            result.createdAt(),
            result.updatedAt()
        );
    }

    private List<OrderItemResponse> toItemResponses(
        List<OrderItemResult> items
    ) {
        return items.stream()
            .map(item -> new OrderItemResponse(
                item.productId(),
                item.productName(),
                item.quantity(),
                item.price()
            ))
            .toList();
    }

    private AddressResponse toAddressResponse(AddressResult address) {
        return new AddressResponse(
            address.zipCode(),
            address.street(),
            address.city(),
            address.state()
        );
    }
}
```

## 5. OpenAPI/Swagger 문서화

### 5.1 OpenAPI 설정

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/config/01_swagger-openapi-guide.md`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Management API")
                .version("v1.0.0")
                .description("주문 관리 REST API")
                .contact(new Contact()
                    .name("Backend Team")
                    .email("backend@example.com")
                )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development")
            ));
    }
}
```

### 5.2 API 문서화 어노테이션

```java
@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Operation(
        summary = "주문 생성",
        description = "새로운 주문을 생성합니다"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (Validation 실패)",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "중복된 주문 번호",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
        @Parameter(description = "주문 생성 요청", required = true)
        @Valid @RequestBody OrderRequest request
    ) {
        // ...
    }
}
```

## 6. 테스트

### 6.1 통합 테스트 (MockMvc)

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/testing/02_integration-test-guide.md`

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceOrderUseCase placeOrderUseCase;

    @MockBean
    private OrderRequestMapper requestMapper;

    @MockBean
    private OrderResponseMapper responseMapper;

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 성공")
    void testPlaceOrder_Success() throws Exception {
        // Given
        OrderRequest request = new OrderRequest(
            "ORD-20250104-001",
            1L,
            List.of(new OrderItemRequest(1L, 2, BigDecimal.valueOf(10000)))
        );

        PlaceOrderCommand command = new PlaceOrderCommand(...);
        OrderResult result = new OrderResult(...);
        OrderResponse response = new OrderResponse(...);

        when(requestMapper.toCommand(any())).thenReturn(command);
        when(placeOrderUseCase.execute(command)).thenReturn(result);
        when(responseMapper.toResponse(result)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(response.orderId()))
            .andExpect(jsonPath("$.orderNumber").value(response.orderNumber()))
            .andDo(print());
    }

    @Test
    @DisplayName("POST /api/v1/orders - Validation 실패")
    void testPlaceOrder_ValidationFailed() throws Exception {
        // Given
        OrderRequest invalidRequest = new OrderRequest(
            "",  // ❌ Blank
            null,  // ❌ Null
            Collections.emptyList()  // ❌ Empty
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andDo(print());
    }
}
```

## 7. ArchUnit 테스트

**문서**: `docs/coding_convention/01-adapter-rest-api-layer/testing/01_archunit-test-guide.md`

```java
@AnalyzeClasses(packages = "com.ryuqq.adapter.in.rest")
public class RestApiArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_be_in_controller_package =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..controller..")
            .because("Controllers must be in controller package");

    @ArchTest
    static final ArchRule controllers_should_be_annotated_with_rest_controller =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController.class)
            .because("Controllers must use @RestController");

    @ArchTest
    static final ArchRule request_dtos_should_be_records =
        classes()
            .that().haveSimpleNameEndingWith("Request")
            .should().beRecords()
            .because("Request DTOs must use Record pattern");
}
```

## 참고 문서

모든 상세 규칙은 다음 디렉토리를 참조하세요:

- `docs/coding_convention/01-adapter-rest-api-layer/`
