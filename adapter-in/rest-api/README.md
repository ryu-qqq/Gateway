# REST API Layer - Hexagonal Architecture

> **목적**: Spring MVC 기반 REST API 계층 구현 (Ports & Adapters Presentation Layer)

---

## 📋 목차

1. [개요](#-개요)
2. [아키텍처 원칙](#-아키텍처-원칙)
3. [디렉토리 구조](#-디렉토리-구조)
4. [핵심 패턴](#-핵심-패턴)
5. [레이어별 가이드](#-레이어별-가이드)
6. [ArchUnit 검증](#-archunit-검증)
7. [테스트 전략](#-테스트-전략)
8. [설정 가이드](#-설정-가이드)

---

## 🎯 개요

### REST API Layer의 역할

**헥사고날 아키텍처의 Presentation Layer (Adapter In)**:
- **Application → REST API 의존성 역전**: REST API가 Application을 의존
- **Port 의존**: Application Layer의 UseCase Port 인터페이스 의존
- **HTTP 요청/응답 처리**: Domain 로직을 HTTP API로 노출
- **기술 세부사항 캡슐화**: Spring MVC/Validation/Error Handling 등 기술 스택 숨김

### 핵심 원칙

```
┌─────────────────────────────────────────────────────────┐
│                      Client (HTTP)                       │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP Request/Response
┌───────────────────────▼─────────────────────────────────┐
│                REST API Layer (Adapter In)               │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Controller (Command/Query 분리)                  │   │
│  │    ↓           ↓           ↓                      │   │
│  │  Mapper     ErrorMapper  Validation               │   │
│  │    ↓           ↓           ↓                      │   │
│  │  Request/Command/Query DTO → Response DTO         │   │
│  └────────────────────┬─────────────────────────────┘   │
└───────────────────────┼─────────────────────────────────┘
                        │ 의존 (Interface)
┌───────────────────────▼─────────────────────────────────┐
│                  Application Layer                       │
│  ┌──────────────────────────────────────────────────┐   │
│  │     UseCase Port (Command/Query Port)            │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**Zero-Tolerance 원칙**:
- ❌ **Lombok 금지**: Pure Java 사용 (특히 DTO)
- ❌ **Domain 직접 의존 금지**: ErrorMapper/GlobalExceptionHandler 제외
- ❌ **Domain 객체 반환 금지**: API Response DTO 사용 필수
- ❌ **하드코딩 엔드포인트 금지**: Properties로 중앙 관리

---

## 🏗️ 아키텍처 원칙

### 1. CQRS 패턴 (Command Query Responsibility Segregation)

**Command Controller (쓰기)**:
```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    private final OrderCommandUseCase useCase;
    private final OrderApiMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @Valid @RequestBody OrderCreateCommand command
    ) {
        OrderCommand useCaseCommand = mapper.toUseCaseCommand(command);
        OrderResponse response = useCase.createOrder(useCaseCommand);
        return ResponseEntity.ok(
            ApiResponse.ofSuccess(mapper.toApiResponse(response))
        );
    }
}
```

**Query Controller (읽기)**:
```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderQueryController {

    private final OrderQueryUseCase useCase;
    private final OrderApiMapper mapper;

    @GetMapping("${api.endpoints.order.by-id}")
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(
        @PathVariable Long id
    ) {
        OrderQuery query = new OrderQuery(id);
        OrderResponse response = useCase.getOrder(query);
        return ResponseEntity.ok(
            ApiResponse.ofSuccess(mapper.toDetailApiResponse(response))
        );
    }
}
```

### 2. DTO 3분할 (Command/Query/Response)

**Command DTO (POST/PUT/PATCH 요청)**:
```java
/**
 * Order 생성 요청 DTO
 */
public record OrderCreateCommand(
    @NotNull(message = "고객 ID는 필수입니다")
    Long customerId,

    @NotNull(message = "주문 항목은 필수입니다")
    @Size(min = 1, message = "최소 1개 이상의 주문 항목이 필요합니다")
    List<OrderItemCommand> items
) {
    // Compact Constructor로 추가 검증
    public OrderCreateCommand {
        if (customerId != null && customerId <= 0) {
            throw new IllegalArgumentException("고객 ID는 양수여야 합니다");
        }
    }
}
```

**Query DTO (GET 요청 파라미터)**:
```java
/**
 * Order 검색 조건 DTO
 */
public record OrderSearchQuery(
    @Min(value = 0, message = "페이지는 0 이상이어야 합니다")
    Integer page,

    @Min(value = 1, message = "사이즈는 1 이상이어야 합니다")
    @Max(value = 100, message = "사이즈는 100 이하여야 합니다")
    Integer size,

    String status
) {
    // 기본값 제공
    public OrderSearchQuery {
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;
    }
}
```

**Response DTO (응답)**:
```java
/**
 * Order 상세 응답 DTO
 */
public record OrderDetailApiResponse(
    Long orderId,
    Long customerId,
    String status,
    Long totalAmount,
    List<OrderItemApiResponse> items,
    LocalDateTime createdAt
) {
    // 불변성 보장
    public OrderDetailApiResponse {
        items = (items != null) ? List.copyOf(items) : List.of();
    }
}
```

### 3. ErrorMapper 패턴 (RFC 7807 Problem Details)

**ErrorMapper 인터페이스**:
```java
public interface ErrorMapper {

    /**
     * 특정 에러 코드를 처리할 수 있는지 확인
     */
    boolean supports(String errorCode);

    /**
     * Domain Exception을 API Error Response로 변환
     */
    ErrorInfo map(DomainException exception, Locale locale);
}
```

**ErrorMapper 구현체**:
```java
@Component
public class OrderErrorMapper implements ErrorMapper {

    private final MessageSource messageSource;

    @Override
    public boolean supports(String errorCode) {
        return errorCode.startsWith("ORDER_");
    }

    @Override
    public ErrorInfo map(DomainException exception, Locale locale) {
        String message = messageSource.getMessage(
            exception.getCode(),
            exception.getArgs(),
            locale
        );
        return new ErrorInfo(exception.getCode(), message);
    }
}
```

**GlobalExceptionHandler**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorMapperRegistry registry;

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(
        DomainException ex,
        Locale locale
    ) {
        ErrorMapper mapper = registry.findMapper(ex.getCode())
            .orElseThrow(() -> new IllegalStateException("ErrorMapper not found"));

        ErrorInfo error = mapper.map(ex, locale);
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ApiResponse.ofFailure(error));
    }
}
```

### 4. ApiResponse 표준화

**성공 응답**:
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "status": "PENDING"
  },
  "error": null,
  "timestamp": "2025-11-23T10:30:00",
  "requestId": "req-123456"
}
```

**실패 응답 (RFC 7807)**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "errorCode": "ORDER_NOT_FOUND",
    "message": "존재하지 않는 주문입니다"
  },
  "timestamp": "2025-11-23T10:30:00",
  "requestId": "req-123456"
}
```

### 5. 중앙 집중식 엔드포인트 관리

**application.yml**:
```yaml
api:
  endpoints:
    base-v1: /api/v1
    order:
      base: /orders
      by-id: /{id}
      cancel: /{id}/cancel
```

**Controller**:
```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    @PatchMapping("${api.endpoints.order.cancel}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        // ...
    }
}
```

---

## 📁 디렉토리 구조

```
adapter-in/rest-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.ryuqq.adapter.in.rest/
│   │   │       ├── order/                       # Bounded Context
│   │   │       │   ├── controller/
│   │   │       │   │   ├── OrderCommandController.java
│   │   │       │   │   └── OrderQueryController.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── command/             # POST/PUT/PATCH DTO
│   │   │       │   │   │   ├── OrderCreateCommand.java
│   │   │       │   │   │   └── OrderUpdateCommand.java
│   │   │       │   │   ├── query/               # GET Query DTO
│   │   │       │   │   │   └── OrderSearchQuery.java
│   │   │       │   │   └── response/            # Response DTO
│   │   │       │   │       ├── OrderApiResponse.java
│   │   │       │   │       └── OrderDetailApiResponse.java
│   │   │       │   ├── mapper/
│   │   │       │   │   └── OrderApiMapper.java
│   │   │       │   └── error/
│   │   │       │       └── OrderErrorMapper.java
│   │   │       ├── common/                      # 공통 컴포넌트
│   │   │       │   ├── controller/
│   │   │       │   │   └── GlobalExceptionHandler.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── ApiResponse.java
│   │   │       │   │   ├── PageApiResponse.java
│   │   │       │   │   ├── SliceApiResponse.java
│   │   │       │   │   └── ErrorInfo.java
│   │   │       │   ├── mapper/
│   │   │       │   │   ├── ErrorMapper.java     # Interface
│   │   │       │   │   └── ErrorMapperRegistry.java
│   │   │       │   └── filter/
│   │   │       │       └── RequestIdFilter.java
│   │   │       └── config/
│   │   │           └── properties/
│   │   │               ├── ApiEndpointProperties.java
│   │   │               └── ApiErrorProperties.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── messages/
│   │           ├── errors_ko.properties
│   │           └── errors_en.properties
│   └── test/
│       ├── java/
│       │   └── com.ryuqq.adapter.in.rest/
│       │       ├── order/
│       │       │   └── controller/              # Controller 테스트
│       │       │       ├── OrderCommandControllerTest.java
│       │       │       └── OrderQueryControllerTest.java
│       │       ├── common/
│       │       │   └── RestDocsTestSupport.java # REST Docs 기본 클래스
│       │       └── architecture/                # ArchUnit 테스트
│       │           ├── RestApiLayerArchTest.java
│       │           ├── ControllerArchTest.java
│       │           ├── CommandDtoArchTest.java
│       │           ├── QueryDtoArchTest.java
│       │           ├── ResponseDtoArchTest.java
│       │           ├── MapperArchTest.java
│       │           ├── error/
│       │           │   └── ErrorHandlingArchTest.java
│       │           ├── config/
│       │           │   └── ApiEndpointPropertiesArchTest.java
│       │           └── common/
│       │               └── ApiResponseArchTest.java
│       └── resources/
│           └── application-test.yml
```

---

## 🔧 핵심 패턴

### 1. Controller 패턴

**Command Controller 책임**:
- HTTP POST/PUT/PATCH/DELETE 요청 처리
- Request DTO → UseCase Command 변환
- UseCase 호출
- UseCase Response → API Response DTO 변환

**Query Controller 책임**:
- HTTP GET 요청 처리
- Query Parameter → UseCase Query 변환
- UseCase 호출
- UseCase Response → API Response DTO 변환

**규칙**:
- ✅ `*CommandController` 또는 `*QueryController` 네이밍
- ✅ `@RestController` 어노테이션
- ✅ Properties 기반 `@RequestMapping`
- ✅ UseCase Port + Mapper 의존
- ✅ `ResponseEntity<ApiResponse<T>>` 반환
- ❌ `@Transactional` 금지
- ❌ Domain 객체 반환 금지
- ❌ 비즈니스 로직 금지

### 2. DTO 패턴

**Command DTO (Java 21 Record)**:
```java
public record OrderCreateCommand(
    @NotNull Long customerId,
    @NotEmpty List<OrderItemCommand> items
) {
    // Compact Constructor로 추가 검증
    public OrderCreateCommand {
        if (customerId != null && customerId <= 0) {
            throw new IllegalArgumentException("Invalid customerId");
        }
        items = List.copyOf(items);  // 불변성 보장
    }
}
```

**Query DTO (Java 21 Record)**:
```java
public record OrderSearchQuery(
    Integer page,
    Integer size,
    String status
) {
    // 기본값 제공
    public OrderSearchQuery {
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;
    }
}
```

**Response DTO (Java 21 Record)**:
```java
public record OrderApiResponse(
    Long orderId,
    String status,
    Long totalAmount
) {
    // 추가 로직 없음 (단순 데이터 전달)
}
```

**규칙**:
- ✅ Java 21 Record 사용
- ✅ Compact Constructor로 validation/기본값
- ✅ Bean Validation 어노테이션
- ✅ 불변성 보장 (List.copyOf)
- ❌ Lombok 금지
- ❌ 비즈니스 로직 금지
- ❌ Spring 어노테이션 금지 (@Component 등)

### 3. Mapper 패턴

**ApiMapper 책임**:
```java
@Component
public class OrderApiMapper {

    // Request DTO → UseCase Command
    public OrderCommand toUseCaseCommand(OrderCreateCommand request) {
        return OrderCommand.builder()
            .customerId(request.customerId())
            .items(request.items().stream()
                .map(this::toUseCaseItem)
                .toList())
            .build();
    }

    // UseCase Response → API Response DTO
    public OrderApiResponse toApiResponse(OrderResponse response) {
        return new OrderApiResponse(
            response.orderId(),
            response.status(),
            response.totalAmount()
        );
    }
}
```

**규칙**:
- ✅ `*ApiMapper` 네이밍
- ✅ `@Component` 어노테이션
- ✅ `to*()` 메서드 네이밍
- ✅ Application DTO만 사용
- ❌ Static 메서드 금지
- ❌ Domain 직접 의존 금지
- ❌ 비즈니스 로직 금지

### 4. ErrorMapper 패턴

**ErrorMapper 구현**:
```java
@Component
public class OrderErrorMapper implements ErrorMapper {

    @Override
    public boolean supports(String errorCode) {
        return errorCode.startsWith("ORDER_");
    }

    @Override
    public ErrorInfo map(DomainException exception, Locale locale) {
        return new ErrorInfo(
            exception.getCode(),
            getMessage(exception, locale)
        );
    }
}
```

**ErrorMapperRegistry**:
```java
@Component
public class ErrorMapperRegistry {

    private final List<ErrorMapper> mappers;

    public Optional<ErrorMapper> findMapper(String errorCode) {
        return mappers.stream()
            .filter(mapper -> mapper.supports(errorCode))
            .findFirst();
    }
}
```

**규칙**:
- ✅ `*ErrorMapper` 네이밍
- ✅ `ErrorMapper` 인터페이스 구현
- ✅ `supports()` + `map()` 메서드 필수
- ✅ Domain Exception 의존 가능 (유일한 예외)
- ❌ 비즈니스 로직 금지
- ❌ `@Transactional` 금지

---

## 📚 레이어별 가이드

### 1. Controller Layer

**Command Controller**:
- [Controller 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-guide.md)
- [Controller 테스트 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-test-guide.md)
- [Controller REST Docs 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-test-restdocs-guide.md)
- [Controller ArchUnit](../../docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-archunit.md)

### 2. DTO Layer

**Command DTO**:
- [Command DTO 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/command/command-dto-guide.md)
- [Command DTO 테스트 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/command/command-dto-test-guide.md)
- [Command DTO ArchUnit](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/command/command-dto-archunit.md)

**Query DTO**:
- [Query DTO 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/query/query-dto-guide.md)
- [Query DTO 테스트 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/query/query-dto-test-guide.md)
- [Query DTO ArchUnit](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/query/query-dto-archunit.md)

**Response DTO**:
- [Response DTO 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/response/response-dto-guide.md)
- [Response DTO 테스트 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/response/response-dto-test-guide.md)
- [Response DTO ArchUnit](../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/response/response-dto-archunit.md)

### 3. Error Handling Layer

**Error Handling**:
- [에러 처리 전략](../../docs/coding_convention/01-adapter-in-layer/rest-api/error/error-handling-strategy.md)
- [ErrorMapper 구현 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/error/error-mapper-implementation-guide.md)

### 4. Mapper Layer

**Mapper**:
- [Mapper 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/mapper/mapper-guide.md)
- [Mapper 테스트 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/mapper/mapper-test-guide.md)
- [Mapper ArchUnit](../../docs/coding_convention/01-adapter-in-layer/rest-api/mapper/mapper-archunit.md)

### 5. Config Layer

**Endpoint Properties**:
- [Endpoint Properties 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/config/endpoint-properties-guide.md)

---

## ✅ ArchUnit 검증

### 1. RestApiLayerArchTest (14개 규칙)

**검증 항목**:
- Package 구조 (controller, dto, mapper, error, config)
- Bounded Context별 패키지 구조
- Common 패키지 구조
- DTO 패키지 분리 (command/query/response)
- Controller는 Application Port 의존 필수
- Domain Layer 직접 의존 금지 (ErrorMapper/GlobalExceptionHandler 제외)
- Controller는 Domain 객체 반환 금지
- Mapper는 Domain 직접 의존 금지
- Persistence Layer 의존 금지
- Config/Properties 클래스 패키지 위치
- 올바른 Stereotype 사용
- Lombok 금지

**실행**:
```bash
./gradlew :adapter-in:rest-api:test --tests "RestApiLayerArchTest"
```

### 2. ErrorHandlingArchTest (12개 규칙)

**검증 항목**:
- ErrorMapper @Component 어노테이션
- ErrorMapper 인터페이스 구현
- supports() / map() 메서드 필수
- 비즈니스 로직 메서드 금지
- MessageSource 의존 가능
- *ErrorMapper 네이밍 규칙
- error 패키지 위치
- GlobalExceptionHandler @RestControllerAdvice
- GlobalExceptionHandler ErrorMapperRegistry 의존
- Lombok 금지
- @Transactional 금지

**실행**:
```bash
./gradlew :adapter-in:rest-api:test --tests "ErrorHandlingArchTest"
```

### 3. ApiEndpointPropertiesArchTest (8개 규칙)

**검증 항목**:
- config.properties 패키지 위치
- @ConfigurationProperties 어노테이션
- @Component 어노테이션
- Nested Static Class *Endpoints 네이밍
- public, non-final 클래스
- Nested Static Class public
- Lombok 금지

**실행**:
```bash
./gradlew :adapter-in:rest-api:test --tests "ApiEndpointPropertiesArchTest"
```

### 4. ApiResponseArchTest (8개 규칙)

**검증 항목**:
- common.dto 패키지 위치
- Java 21 Record 타입
- ApiResponse ofSuccess/ofFailure static factory methods
- PageApiResponse from() static method
- ErrorInfo validation
- public Record
- Lombok 금지

**실행**:
```bash
./gradlew :adapter-in:rest-api:test --tests "ApiResponseArchTest"
```

### 전체 ArchUnit 실행

```bash
# 신규 추가된 4개 ArchUnit 테스트 실행 (42개 규칙)
./gradlew :adapter-in:rest-api:test \
  --tests "RestApiLayerArchTest" \
  --tests "ErrorHandlingArchTest" \
  --tests "ApiEndpointPropertiesArchTest" \
  --tests "ApiResponseArchTest"

# 전체 ArchUnit 테스트 실행 (93개 규칙)
./gradlew :adapter-in:rest-api:test --tests "*.architecture.*"
```

---

## 🧪 테스트 전략

### 1. Controller 테스트 (TestRestTemplate)

**Command Controller 테스트**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderCommandControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_Success() {
        // Given
        OrderCreateCommand command = new OrderCreateCommand(1L, items);

        // When
        ResponseEntity<ApiResponse<OrderApiResponse>> response =
            restTemplate.postForEntity(
                "/api/v1/orders",
                command,
                new ParameterizedTypeReference<>() {}
            );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
    }
}
```

**Query Controller 테스트**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderQueryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("주문 단건 조회 성공")
    void getOrder_Success() {
        // Given
        Long orderId = 1L;

        // When
        ResponseEntity<ApiResponse<OrderDetailApiResponse>> response =
            restTemplate.exchange(
                "/api/v1/orders/{id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                orderId
            );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().orderId()).isEqualTo(orderId);
    }
}
```

### 2. REST Docs 테스트

**REST Docs 자동 생성**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestDocs
class OrderCommandControllerRestDocsTest extends RestDocsTestSupport {

    @Test
    @DisplayName("주문 생성 API 문서화")
    void createOrder_RestDocs() {
        // Given
        OrderCreateCommand command = new OrderCreateCommand(1L, items);

        // When & Then
        restTemplate.postForEntity("/api/v1/orders", command, ...)
            .andDo(document("order-create",
                requestFields(
                    fieldWithPath("customerId").description("고객 ID"),
                    fieldWithPath("items").description("주문 항목")
                ),
                responseFields(
                    fieldWithPath("success").description("성공 여부"),
                    fieldWithPath("data.orderId").description("주문 ID")
                )
            ));
    }
}
```

### 3. DTO Validation 테스트

**Command DTO 검증 테스트**:
```java
class OrderCreateCommandTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("customerId null 시 검증 실패")
    void validation_CustomerIdNull_Fail() {
        // Given
        OrderCreateCommand command = new OrderCreateCommand(null, items);

        // When
        Set<ConstraintViolation<OrderCreateCommand>> violations =
            validator.validate(command);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
            v.getPropertyPath().toString().equals("customerId")
        );
    }
}
```

### 4. ArchUnit 테스트

**자동 실행**:
```bash
# Gradle 빌드 시 자동 실행
./gradlew :adapter-in:rest-api:test

# 특정 ArchUnit 테스트만 실행
./gradlew :adapter-in:rest-api:test --tests "*ArchTest"
```

---

## ⚙️ 설정 가이드

### 1. application.yml

**API Endpoint 설정**:
```yaml
api:
  endpoints:
    base-v1: /api/v1

    order:
      base: /orders
      by-id: /{id}
      cancel: /{id}/cancel
      confirm: /{id}/confirm

    product:
      base: /products
      by-id: /{id}
```

**Error Message 설정**:
```yaml
spring:
  messages:
    basename: messages/errors
    encoding: UTF-8
```

### 2. Error Messages (i18n)

**errors_ko.properties**:
```properties
ORDER_NOT_FOUND=존재하지 않는 주문입니다
ORDER_ALREADY_CANCELLED=이미 취소된 주문입니다
ORDER_INVALID_STATUS=유효하지 않은 주문 상태입니다
```

**errors_en.properties**:
```properties
ORDER_NOT_FOUND=Order not found
ORDER_ALREADY_CANCELLED=Order already cancelled
ORDER_INVALID_STATUS=Invalid order status
```

### 3. Spring MVC 설정

**WebMvcConfig.java**:
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(
        List<HttpMessageConverter<?>> converters
    ) {
        // Jackson 설정
        MappingJackson2HttpMessageConverter converter =
            new MappingJackson2HttpMessageConverter();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        converter.setObjectMapper(mapper);
        converters.add(converter);
    }
}
```

### 4. Bean Validation 설정

**build.gradle**:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

**GlobalExceptionHandler.java**:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Void>> handleValidation(
    MethodArgumentNotValidException ex
) {
    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));

    ErrorInfo error = new ErrorInfo("VALIDATION_ERROR", message);
    return ResponseEntity
        .badRequest()
        .body(ApiResponse.ofFailure(error));
}
```

---

## 📋 체크리스트

### Controller 구현 시

- [ ] `*CommandController` 또는 `*QueryController` 네이밍
- [ ] `@RestController` 어노테이션
- [ ] Properties 기반 `@RequestMapping`
- [ ] UseCase Port + Mapper 의존
- [ ] `ResponseEntity<ApiResponse<T>>` 반환
- [ ] `@Transactional` 금지
- [ ] Domain 객체 반환 금지
- [ ] 비즈니스 로직 금지

### DTO 구현 시

- [ ] Java 21 Record 사용
- [ ] command/query/response 패키지 분리
- [ ] Bean Validation 어노테이션
- [ ] Compact Constructor로 추가 검증/기본값
- [ ] 불변성 보장 (List.copyOf)
- [ ] Lombok 금지
- [ ] Spring 어노테이션 금지

### Mapper 구현 시

- [ ] `*ApiMapper` 네이밍
- [ ] `@Component` 어노테이션
- [ ] `to*()` 메서드 네이밍
- [ ] Application DTO만 사용
- [ ] Static 메서드 금지
- [ ] Domain 직접 의존 금지

### ErrorMapper 구현 시

- [ ] `*ErrorMapper` 네이밍
- [ ] `ErrorMapper` 인터페이스 구현
- [ ] `@Component` 어노테이션
- [ ] `supports()` + `map()` 메서드 구현
- [ ] MessageSource 의존 (i18n)
- [ ] error 패키지 위치
- [ ] 비즈니스 로직 금지

### ApiEndpointProperties 구현 시

- [ ] config.properties 패키지 위치
- [ ] `@ConfigurationProperties(prefix = "api.endpoints")`
- [ ] `@Component` 어노테이션
- [ ] Nested Static Class로 BC별 그룹화
- [ ] `*Endpoints` 네이밍
- [ ] 기본값 제공
- [ ] Lombok 금지

---

## 📚 참고 문서

### 내부 문서
- [REST API Layer 가이드](../../docs/coding_convention/01-adapter-in-layer/rest-api/rest-api-guide.md)
- [Application Layer README](../../application/README.md)
- [Domain Layer README](../../domain/README.md)

### 외부 문서
- [Spring MVC 공식 문서](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring REST Docs 공식 문서](https://docs.spring.io/spring-restdocs/docs/current/reference/html5/)
- [Bean Validation 공식 문서](https://beanvalidation.org/2.0/spec/)
- [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-23
**버전**: 1.0.0
