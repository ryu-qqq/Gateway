# OpenAPI Guide — **Swagger/OpenAPI 3.0 컨벤션**

> 이 문서는 `adapter-in/rest-api` 레이어의 **OpenAPI 어노테이션 가이드**입니다.
>
> Controller, DTO에 적용하는 OpenAPI 3.0 어노테이션 규칙을 다룹니다.

---

## 1) 핵심 원칙

### 필수 규칙

| 원칙 | 설명 |
|------|------|
| **모든 Controller 메서드에 @Operation** | API 설명, summary 필수 |
| **모든 DTO 필드에 @Schema** | 필드 설명, 예시 필수 |
| **응답 코드별 @ApiResponse** | 200, 400, 404, 500 등 명시 |
| **Enum은 @Schema(enumAsRef)** | Enum 참조 방식 통일 |
| **description은 한국어** | 사용자 친화적 문서화 |

### 금지 사항

| 금지 | 이유 |
|------|------|
| **@Operation 누락** | API 문서 불완전 |
| **@Schema 누락** | 필드 설명 없음 |
| **영문 description만 사용** | 한국어 문서화 필수 |
| **example 누락** | 사용 예시 없음 |

---

## 2) 의존성 설정

### build.gradle

```groovy
dependencies {
    // SpringDoc OpenAPI 3.0
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
}
```

### application.yml

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

---

## 3) Controller 어노테이션

### 3.1) @Tag — Controller 그룹화

```java
@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {
    // ...
}
```

### 3.2) @Operation — 메서드 설명

```java
@Operation(
    summary = "주문 생성",
    description = "새로운 주문을 생성합니다. 재고 확인 후 주문이 생성됩니다."
)
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
    @Valid @RequestBody CreateOrderApiRequest request
) {
    // ...
}
```

### 3.3) @ApiResponse — 응답 코드별 정의

```java
@Operation(summary = "주문 조회")
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = OrderApiResponse.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "주문을 찾을 수 없음",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
})
@GetMapping("/{orderId}")
public ResponseEntity<ApiResponse<OrderApiResponse>> getOrder(
    @Parameter(description = "주문 ID", example = "12345")
    @PathVariable Long orderId
) {
    // ...
}
```

### 3.4) @Parameter — 파라미터 설명

```java
@GetMapping
public ResponseEntity<SliceApiResponse<OrderSummaryApiResponse>> searchOrders(
    @Parameter(description = "주문 상태", example = "PENDING")
    @RequestParam(required = false) OrderStatus status,

    @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
    @RequestParam(defaultValue = "0") int page,

    @Parameter(description = "페이지 크기", example = "20")
    @RequestParam(defaultValue = "20") int size
) {
    // ...
}
```

---

## 4) DTO 어노테이션

### 4.1) Request DTO — @Schema 필수

```java
@Schema(description = "주문 생성 요청")
public record CreateOrderApiRequest(
    @Schema(
        description = "주문자 ID",
        example = "1001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "주문자 ID는 필수입니다")
    Long customerId,

    @Schema(
        description = "주문 상품 목록",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "주문 상품은 1개 이상 필요합니다")
    @Valid
    List<OrderItemApiRequest> items,

    @Schema(
        description = "배송 메모",
        example = "문 앞에 놓아주세요",
        nullable = true
    )
    String deliveryNote
) {}
```

### 4.2) Response DTO — @Schema 필수

```java
@Schema(description = "주문 응답")
public record OrderApiResponse(
    @Schema(description = "주문 ID", example = "12345")
    Long orderId,

    @Schema(description = "주문 상태", example = "PENDING")
    OrderStatus status,

    @Schema(description = "총 주문 금액", example = "50000")
    Long totalAmount,

    @Schema(description = "주문 일시", example = "2025-12-04T10:30:00")
    LocalDateTime orderedAt,

    @Schema(description = "주문 상품 목록")
    List<OrderItemApiResponse> items
) {}
```

### 4.3) Enum — @Schema(enumAsRef)

```java
@Schema(
    description = "주문 상태",
    enumAsRef = true
)
public enum OrderStatus {
    @Schema(description = "대기중")
    PENDING,

    @Schema(description = "결제 완료")
    PAID,

    @Schema(description = "배송중")
    SHIPPING,

    @Schema(description = "배송 완료")
    DELIVERED,

    @Schema(description = "취소됨")
    CANCELLED
}
```

---

## 5) 전체 예시

### 5.1) Controller 전체 예시

```java
@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderApiMapper orderApiMapper;

    public OrderCommandController(
        CreateOrderUseCase createOrderUseCase,
        OrderApiMapper orderApiMapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderApiMapper = orderApiMapper;
    }

    @Operation(
        summary = "주문 생성",
        description = "새로운 주문을 생성합니다. 재고 확인 후 주문이 생성됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(schema = @Schema(implementation = OrderApiResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "재고 부족",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "주문 생성 요청",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateOrderApiRequest.class))
        )
        @Valid @RequestBody CreateOrderApiRequest request
    ) {
        CreateOrderCommand command = orderApiMapper.toCommand(request);
        OrderResult result = createOrderUseCase.execute(command);
        OrderApiResponse response = orderApiMapper.toApiResponse(result);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(response));
    }
}
```

---

## 6) OpenAPI 설정 클래스

### 6.1) OpenApiConfig

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order API")
                .description("주문 관리 시스템 API")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Development Team")
                    .email("dev@example.com"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("https://api.example.com").description("Production")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

---

## 7) Do / Don't

### ✅ Do

```java
// ✅ 모든 Controller 메서드에 @Operation
@Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다")
@PostMapping
public ResponseEntity<...> createOrder(...) { ... }

// ✅ 모든 DTO 필드에 @Schema
@Schema(description = "주문자 ID", example = "1001")
Long customerId;

// ✅ 응답 코드별 @ApiResponse
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "404", description = "찾을 수 없음")
})

// ✅ Enum에 @Schema(enumAsRef = true)
@Schema(enumAsRef = true)
public enum OrderStatus { ... }
```

### ❌ Don't

```java
// ❌ @Operation 누락
@PostMapping
public ResponseEntity<...> createOrder(...) { ... }

// ❌ @Schema 누락
Long customerId;

// ❌ @ApiResponse 누락
@GetMapping("/{id}")
public ResponseEntity<...> getOrder(...) { ... }

// ❌ description 영문만 사용
@Schema(description = "Customer ID")  // 한국어 사용 권장
```

---

## 8) 관련 문서

| 문서 | 설명 |
|------|------|
| [OpenAPI ArchUnit](./openapi-archunit.md) | 아키텍처 검증 규칙 |
| [Controller Guide](../controller/controller-guide.md) | Controller 작성 가이드 |
| [DTO Guide](../dto/command/command-dto-guide.md) | DTO 작성 가이드 |

---

## 9) 체크리스트

### Controller

- [ ] `@Tag`로 Controller 그룹화
- [ ] 모든 메서드에 `@Operation(summary, description)` 적용
- [ ] 모든 메서드에 `@ApiResponses` 적용 (200, 4xx, 5xx)
- [ ] PathVariable/RequestParam에 `@Parameter` 적용

### Request DTO

- [ ] Record 클래스에 `@Schema(description)` 적용
- [ ] 모든 필드에 `@Schema(description, example)` 적용
- [ ] 필수 필드에 `requiredMode = REQUIRED` 적용

### Response DTO

- [ ] Record 클래스에 `@Schema(description)` 적용
- [ ] 모든 필드에 `@Schema(description, example)` 적용

### Enum

- [ ] `@Schema(enumAsRef = true)` 적용
- [ ] 각 값에 `@Schema(description)` 적용

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
