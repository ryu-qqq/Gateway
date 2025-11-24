# Controller Guide — **Thin Controller Pattern**

> **목적**: REST API Controller 설계 및 구현 가이드
>
> **철학**: Thin Controller, 비즈니스 로직 절대 금지, 단순 변환 + 위임만

---

## 1️⃣ 핵심 원칙 (Core Principles)

### Controller의 단 하나의 책임

**Controller는 HTTP 요청을 UseCase로 전달하고, 결과를 HTTP 응답으로 반환하는 역할만 수행합니다.**

```
HTTP Request
    ↓
@Valid 검증 (Request DTO)
    ↓
Mapper 변환 (API DTO → UseCase DTO)
    ↓
UseCase 실행
    ↓
Mapper 변환 (UseCase DTO → API DTO)
    ↓
ResponseEntity<ApiResponse<T>> 래핑
    ↓
HTTP Response
```

### 금지 사항 (Zero-Tolerance)

- ❌ **비즈니스 로직**: Controller에 비즈니스 규칙 구현 절대 금지
- ❌ **Domain 직접 호출**: Domain 객체 직접 생성/조작 금지
- ❌ **Transaction 관리**: `@Transactional` 사용 금지 (UseCase 책임)
- ❌ **예외 처리**: try-catch로 예외 처리 금지 (GlobalExceptionHandler 위임)
- ❌ **DELETE 메서드**: DELETE 엔드포인트 지원 안 함 (소프트 삭제는 PATCH)
- ❌ **Lombok**: `@Data`, `@Builder` 등 모든 Lombok 어노테이션 금지

### 필수 사항

- ✅ **ResponseEntity<ApiResponse<T>> 래핑**: 모든 응답은 이 형식으로 반환
- ✅ **@Valid 검증**: 모든 Request DTO에 `@Valid` 어노테이션 필수
- ✅ **Mapper DI**: Mapper를 생성자 주입으로 DI
- ✅ **UseCase 직접 의존**: UseCase를 생성자 주입으로 직접 의존
- ✅ **RESTful URI**: 리소스 기반 URI (명사 복수형) + HTTP 메서드 활용
- ✅ **CQRS 분리 권장**: Command/Query Controller 분리 (선택)

---

## 2️⃣ 기본 템플릿 (Basic Template)

### Command Controller (POST, PUT, PATCH)

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.ryuqq.adapter.in.rest.common.dto.ApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.command.CreateOrderApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderApiResponse;
import com.ryuqq.adapter.in.rest.order.mapper.OrderApiMapper;
import com.ryuqq.application.order.port.in.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Order Command Controller
 *
 * <p>Order 도메인의 상태 변경 API를 제공합니다.</p>
 *
 * <p>제공하는 API:</p>
 * <ul>
 *   <li>POST /api/v1/orders - 주문 생성</li>
 *   <li>PATCH /api/v1/orders/{id}/cancel - 주문 취소</li>
 *   <li>PATCH /api/v1/orders/{id}/confirm - 주문 확인</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.endpoints.base-v1}/orders")
@Validated
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderApiMapper orderApiMapper;

    /**
     * OrderCommandController 생성자
     *
     * @param createOrderUseCase 주문 생성 UseCase
     * @param orderApiMapper Order Mapper
     */
    public OrderCommandController(
            CreateOrderUseCase createOrderUseCase,
            OrderApiMapper orderApiMapper) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderApiMapper = orderApiMapper;
    }

    /**
     * 주문 생성
     *
     * @param request 주문 생성 요청 DTO
     * @return 주문 생성 결과 (201 Created)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
            @RequestBody @Valid CreateOrderApiRequest request) {

        // ✅ 1. API Request → UseCase Command 변환 (Mapper)
        var command = orderApiMapper.toCreateCommand(request);

        // ✅ 2. UseCase 실행 (비즈니스 로직)
        var useCaseResponse = createOrderUseCase.execute(command);

        // ✅ 3. UseCase Response → API Response 변환 (Mapper)
        var apiResponse = orderApiMapper.toApiResponse(useCaseResponse);

        // ✅ 4. ResponseEntity<ApiResponse<T>> 래핑
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(apiResponse));
    }
}
```

### Query Controller (GET)

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.ryuqq.adapter.in.rest.common.dto.ApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.query.OrderSearchApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderDetailApiResponse;
import com.ryuqq.adapter.in.rest.order.mapper.OrderApiMapper;
import com.ryuqq.application.order.port.in.GetOrderQueryService;
import com.ryuqq.application.order.port.in.SearchOrderQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Order Query Controller
 *
 * <p>Order 도메인의 조회 API를 제공합니다.</p>
 *
 * <p>제공하는 API:</p>
 * <ul>
 *   <li>GET /api/v1/orders/{id} - 주문 단건 조회</li>
 *   <li>GET /api/v1/orders - 주문 검색</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.endpoints.base-v1}/orders")
@Validated
public class OrderQueryController {

    private final GetOrderQueryService getOrderQueryService;
    private final SearchOrderQueryService searchOrderQueryService;
    private final OrderApiMapper orderApiMapper;

    /**
     * OrderQueryController 생성자
     *
     * @param getOrderQueryService 주문 조회 Query Service
     * @param searchOrderQueryService 주문 검색 Query Service
     * @param orderApiMapper Order Mapper
     */
    public OrderQueryController(
            GetOrderQueryService getOrderQueryService,
            SearchOrderQueryService searchOrderQueryService,
            OrderApiMapper orderApiMapper) {
        this.getOrderQueryService = getOrderQueryService;
        this.searchOrderQueryService = searchOrderQueryService;
        this.orderApiMapper = orderApiMapper;
    }

    /**
     * 주문 단건 조회
     *
     * @param id 주문 ID (양수)
     * @return 주문 상세 정보 (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(
            @PathVariable @Positive Long id) {

        // ✅ 1. API Request → UseCase Query 변환 (Mapper)
        var query = orderApiMapper.toGetQuery(id);

        // ✅ 2. UseCase 실행 (조회 로직)
        var useCaseResponse = getOrderQueryService.getById(query);

        // ✅ 3. UseCase Response → API Response 변환 (Mapper)
        var apiResponse = orderApiMapper.toDetailApiResponse(useCaseResponse);

        // ✅ 4. ResponseEntity<ApiResponse<T>> 래핑
        return ResponseEntity.ok(ApiResponse.ofSuccess(apiResponse));
    }

    /**
     * 주문 검색
     *
     * @param searchRequest 검색 조건
     * @return 주문 검색 결과 (200 OK)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> searchOrders(
            @Valid @ModelAttribute OrderSearchApiRequest searchRequest) {

        // ✅ 1. API Request → UseCase Query 변환 (Mapper)
        var query = orderApiMapper.toSearchQuery(searchRequest);

        // ✅ 2. UseCase 실행 (검색 로직)
        var useCaseResponse = searchOrderQueryService.search(query);

        // ✅ 3. UseCase Response → API Response 변환 (Mapper)
        var apiResponse = orderApiMapper.toApiResponse(useCaseResponse);

        // ✅ 4. ResponseEntity<ApiResponse<T>> 래핑
        return ResponseEntity.ok(ApiResponse.ofSuccess(apiResponse));
    }
}
```

---

## 3️⃣ ResponseEntity<ApiResponse<T>> 래핑 (Response Wrapping)

### 필수 형식

**모든 Controller 응답은 `ResponseEntity<ApiResponse<T>>` 형식으로 반환해야 합니다.**

```java
// ✅ Good: ResponseEntity<ApiResponse<T>> 래핑
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
    // ...
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.ofSuccess(response));
}

// ❌ Bad: ApiResponse만 반환 (HTTP 상태 제어 불가)
@PostMapping
public ApiResponse<OrderApiResponse> createOrder(...) {
    // ...
    return ApiResponse.ofSuccess(response);  // ❌ HTTP 상태 200만 가능
}

// ❌ Bad: ResponseEntity만 반환 (표준 응답 형식 미준수)
@PostMapping
public ResponseEntity<OrderApiResponse> createOrder(...) {
    // ...
    return ResponseEntity.status(HttpStatus.CREATED).body(response);  // ❌ ApiResponse 미사용
}
```

### HTTP 상태 코드 매핑

| 메서드 | HTTP 상태 | 용도 |
|--------|-----------|------|
| **POST** | 201 Created | 리소스 생성 성공 |
| **GET** | 200 OK | 조회 성공 |
| **PATCH** | 200 OK | 부분 수정 성공 |
| **PUT** | 200 OK | 전체 수정 성공 |
| **DELETE** | ❌ 지원 안 함 | 소프트 삭제는 PATCH로 처리 |

### ApiResponse 사용 패턴

```java
// ✅ 성공 응답 (데이터 있음)
return ResponseEntity.ok(ApiResponse.ofSuccess(data));

// ✅ 성공 응답 (데이터 없음, 예: PATCH)
return ResponseEntity.ok(ApiResponse.ofSuccess());

// ✅ 생성 성공 (201 Created)
return ResponseEntity
    .status(HttpStatus.CREATED)
    .body(ApiResponse.ofSuccess(data));

// ❌ 실패 응답은 Controller에서 생성 금지 (GlobalExceptionHandler 책임)
// Domain Exception 발생 시 GlobalExceptionHandler가 자동으로 처리
```

---

## 4️⃣ DELETE 메서드 금지 (No DELETE)

### 원칙

**DELETE 엔드포인트는 지원하지 않습니다. 소프트 삭제는 PATCH로 처리합니다.**

### ❌ Bad: DELETE 사용

```java
// ❌ Bad: DELETE 엔드포인트 (금지)
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
    // ...
    return ResponseEntity.ok(ApiResponse.ofSuccess());
}
```

### ✅ Good: PATCH로 소프트 삭제

```java
// ✅ Good: PATCH로 소프트 삭제 (상태 변경)
@PatchMapping("/{id}/delete")
public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
    // ✅ 1. Mapper 변환
    var command = orderApiMapper.toDeleteCommand(id);

    // ✅ 2. UseCase 실행 (상태를 DELETED로 변경)
    deleteOrderUseCase.execute(command);

    // ✅ 3. 성공 응답 (데이터 없음)
    return ResponseEntity.ok(ApiResponse.ofSuccess());
}
```

**이유**:
- 실제 DB 삭제는 위험 (복구 불가)
- 소프트 삭제는 상태 변경이므로 PATCH가 적합
- 감사 추적 (Audit Trail) 유지 가능

---

## 6️⃣ @Valid 검증 (Bean Validation)

### 필수 검증

**모든 Request DTO에 `@Valid` 어노테이션을 사용하여 Bean Validation을 수행합니다.**

```java
// ✅ Good: @Valid 검증
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @RequestBody @Valid CreateOrderApiRequest request) {  // ✅ @Valid 필수
    // ...
}

// ❌ Bad: @Valid 누락 (검증 안 됨)
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @RequestBody CreateOrderApiRequest request) {  // ❌ @Valid 누락
    // ...
}
```

### PathVariable, RequestParam 검증

```java
// ✅ Good: @Validated + @Positive
@RestController
@Validated  // ✅ 클래스 레벨에 필수
public class OrderQueryController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(
            @PathVariable @Positive Long id) {  // ✅ @Positive 검증
        // ...
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> searchOrders(
            @RequestParam @Positive Integer page,  // ✅ @Positive 검증
            @RequestParam @Positive @Max(100) Integer size) {  // ✅ @Max 검증
        // ...
    }
}
```

### Validation 실패 시 처리

```java
// ✅ Validation 실패 시 GlobalExceptionHandler가 자동으로 처리
// Controller에서 try-catch 불필요

@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @RequestBody @Valid CreateOrderApiRequest request) {

    // ✅ Validation 실패 시 MethodArgumentNotValidException 발생
    // → GlobalExceptionHandler가 400 Bad Request 응답 생성

    var command = orderApiMapper.toCreateCommand(request);
    var useCaseResponse = createOrderUseCase.execute(command);
    var apiResponse = orderApiMapper.toApiResponse(useCaseResponse);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.ofSuccess(apiResponse));
}
```

---

---

## 8️⃣ RESTful URI 설계 (RESTful URI Design)

### 엔드포인트 Properties 관리

**엔드포인트는 절대 하드코딩하지 않고 Properties로 중앙 관리합니다.**

```java
// ✅ Good: Properties 사용
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")

// ❌ Bad: 하드코딩
@RequestMapping("/api/v1/orders")
```

**자세한 내용은 [Endpoint Properties Guide](../config/endpoint-properties-guide.md)를 참고하세요.**

---

### URI 네이밍 규칙

| 패턴 | 예시 | 설명 |
|------|------|------|
| **리소스 복수형** | `/orders` | 명사 복수형 사용 |
| **계층 구조** | `/orders/{id}/items` | 하위 리소스 표현 |
| **행위는 HTTP 메서드로** | `POST /orders` | URI에 동사 금지 (`/createOrder` ❌) |
| **상태 변경은 PATCH** | `PATCH /orders/{id}/cancel` | 동사 허용 (상태 전이) |
| **kebab-case** | `/order-items` | 단어 구분은 하이픈 사용 |

### ✅ Good Examples

```java
// ✅ Good: RESTful URI
POST   /api/v1/orders              // 주문 생성
GET    /api/v1/orders/{id}         // 주문 조회
GET    /api/v1/orders              // 주문 검색
PATCH  /api/v1/orders/{id}/cancel  // 주문 취소 (상태 변경)
PATCH  /api/v1/orders/{id}/confirm // 주문 확인 (상태 변경)
GET    /api/v1/orders/{id}/items   // 주문 아이템 조회
```

### ❌ Bad Examples

```java
// ❌ Bad: RPC 스타일 (동사 사용)
POST   /api/v1/createOrder         // ❌ 동사 사용 금지
POST   /api/v1/orders/create       // ❌ 동사 사용 금지
GET    /api/v1/getOrders           // ❌ 동사 사용 금지
POST   /api/v1/orders/cancel       // ❌ 상태 변경은 PATCH

// ❌ Bad: 단수형 사용
GET    /api/v1/order/{id}          // ❌ 복수형 사용 권장
```

---

## 9️⃣ Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. ResponseEntity<ApiResponse<T>> 래핑
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.ofSuccess(response));
}

// ✅ 2. Mapper로 변환
var command = orderApiMapper.toCreateCommand(request);
var useCaseResponse = createOrderUseCase.execute(command);
var apiResponse = orderApiMapper.toApiResponse(useCaseResponse);

// ✅ 3. @Valid 검증
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @RequestBody @Valid CreateOrderApiRequest request) {
    // ...
}

// ✅ 4. UseCase 직접 의존
private final CreateOrderUseCase createOrderUseCase;
private final OrderApiMapper orderApiMapper;

public OrderCommandController(
        CreateOrderUseCase createOrderUseCase,
        OrderApiMapper orderApiMapper) {
    this.createOrderUseCase = createOrderUseCase;
    this.orderApiMapper = orderApiMapper;
}

// ✅ 5. 소프트 삭제는 PATCH로
@PatchMapping("/{id}/delete")
public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
    var command = orderApiMapper.toDeleteCommand(id);
    deleteOrderUseCase.execute(command);
    return ResponseEntity.ok(ApiResponse.ofSuccess());
}
```

### ❌ Anti-Patterns

```java
// ❌ 1. 비즈니스 로직 포함
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
    // ❌ Controller에 비즈니스 로직 금지
    if (request.totalAmount() < 10000) {
        throw new IllegalArgumentException("최소 주문 금액은 10,000원입니다");
    }

    // ❌ Domain 객체 직접 생성 금지
    Order order = new Order(request.customerId(), request.items());
    order.place();

    // ...
}

// ❌ 2. try-catch로 예외 처리
@PostMapping
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
    try {
        // ❌ Controller에서 예외 처리 금지 (GlobalExceptionHandler 위임)
        var command = orderApiMapper.toCreateCommand(request);
        var useCaseResponse = createOrderUseCase.execute(command);
        var apiResponse = orderApiMapper.toApiResponse(useCaseResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(apiResponse));
    } catch (DomainException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.ofFailure(e.code(), e.getMessage()));
    }
}

// ❌ 3. @Transactional 사용
@PostMapping
@Transactional  // ❌ Controller에 Transaction 관리 금지 (UseCase 책임)
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
    // ...
}

// ❌ 4. DELETE 메서드 사용
@DeleteMapping("/{id}")  // ❌ DELETE 지원 안 함
public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
    // ...
}

// ❌ 5. ResponseEntity 없이 ApiResponse만 반환
@PostMapping
public ApiResponse<OrderApiResponse> createOrder(...) {  // ❌ HTTP 상태 제어 불가
    return ApiResponse.ofSuccess(response);
}

// ❌ 6. ApiResponse 없이 ResponseEntity만 반환
@PostMapping
public ResponseEntity<OrderApiResponse> createOrder(...) {  // ❌ 표준 형식 미준수
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// ❌ 7. Lombok 사용
@Data  // ❌ Lombok 금지
@RestController
public class OrderCommandController {
    // ...
}
```

---

## 🔟 CQRS 분리 (Command/Query Separation)

### CQRS 패턴 (권장)

**Command Controller와 Query Controller를 분리하면 다음과 같은 이점이 있습니다:**

- ✅ **의존성 관리 용이**: Command와 Query UseCase를 분리하여 의존성 최소화
- ✅ **책임 분리**: 상태 변경과 조회를 명확히 구분
- ✅ **확장성**: Command와 Query를 독립적으로 확장 가능
- ✅ **테스트 용이**: Command와 Query 테스트를 분리하여 명확한 테스트 작성

### 분리 예시

```java
// ✅ Command Controller (POST, PATCH)
@RestController
@RequestMapping("${api.endpoints.base-v1}/orders")
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderApiMapper orderApiMapper;

    // POST, PATCH 엔드포인트만 제공
}

// ✅ Query Controller (GET)
@RestController
@RequestMapping("${api.endpoints.base-v1}/orders")
public class OrderQueryController {

    private final GetOrderQueryService getOrderQueryService;
    private final SearchOrderQueryService searchOrderQueryService;
    private final OrderApiMapper orderApiMapper;

    // GET 엔드포인트만 제공
}
```

### 통합 Controller (선택사항)

**소규모 Bounded Context는 통합 Controller 사용 가능:**

```java
// ✅ 통합 Controller (Command + Query)
@RestController
@RequestMapping("${api.endpoints.base-v1}/orders")
public class OrderController {

    // Command UseCases
    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    // Query Services
    private final GetOrderQueryService getOrderQueryService;
    private final SearchOrderQueryService searchOrderQueryService;

    private final OrderApiMapper orderApiMapper;

    // POST, GET, PATCH 엔드포인트 모두 제공
}
```

---

## 1️⃣1️⃣ 체크리스트

- [ ] `@RestController`, `@RequestMapping` 어노테이션 선언
- [ ] `@Validated` 어노테이션 선언 (PathVariable, RequestParam 검증 시)
- [ ] UseCase, Mapper DI (Constructor Injection)
- [ ] 모든 응답은 `ResponseEntity<ApiResponse<T>>` 형식
- [ ] HTTP 상태 코드 올바르게 설정 (POST → 201, GET → 200)
- [ ] 모든 Request DTO에 `@Valid` 어노테이션
- [ ] PathVariable, RequestParam 검증 어노테이션 (`@Positive`, `@Max` 등)
- [ ] Mapper로 API DTO ↔ UseCase DTO 변환
- [ ] UseCase만 호출, 비즈니스 로직 절대 금지
- [ ] DELETE 메서드 사용 금지 (소프트 삭제는 PATCH)
- [ ] try-catch로 예외 처리 금지 (GlobalExceptionHandler 위임)
- [ ] `@Transactional` 사용 금지 (UseCase 책임)
- [ ] RESTful URI 설계 (명사 복수형, 동사 금지)
- [ ] Lombok 사용 금지
- [ ] Javadoc 작성 (`@author`, `@since`)

---

## 1️⃣2️⃣ 추가 가이드 링크

- **[Error Handling Strategy Guide](../error/error-handling-strategy.md)** - 에러 처리 전략
- **[ErrorMapper Implementation Guide](../error/error-mapper-implementation-guide.md)** - ErrorMapper 구현 가이드
- **[Command DTO Guide](../dto/command/command-dto-guide.md)** - Command DTO 작성 가이드
- **[Query DTO Guide](../dto/query/query-dto-guide.md)** - Query DTO 작성 가이드
- **[Response DTO Guide](../dto/response/response-dto-guide.md)** - Response DTO 작성 가이드
- **[Mapper Guide](../mapper/mapper-guide.md)** - Mapper 작성 가이드 (TBD)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
