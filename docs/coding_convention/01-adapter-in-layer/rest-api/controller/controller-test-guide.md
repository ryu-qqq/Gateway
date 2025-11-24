# Controller Unit Test Guide — **@WebMvcTest로 HTTP 계층 검증**

> **목적**: Controller의 HTTP 요청/응답 처리 로직을 빠르게 검증
>
> **철학**: Thin Controller, UseCase Mock, Validation 중심

---

## 1️⃣ 핵심 원칙 (Core Principles)

### Controller 단위 테스트는 HTTP 계층만 검증합니다

**Controller는 Thin하므로 테스트도 간단합니다.**

### 검증 범위

| 검증 항목 | 설명 | 방법 |
|----------|------|------|
| **HTTP 매핑** | URL, Method, Path Variable 올바른지 | MockMvc 요청 |
| **Request DTO** | JSON → DTO 역직렬화, Validation | `@Valid` 검증 |
| **Response DTO** | DTO → JSON 직렬화, 구조 검증 | JsonPath |
| **HTTP Status** | 201, 400, 404 등 올바른 상태 코드 | `.andExpect(status())` |
| **Error Response** | RFC 7807 준수 에러 응답 | ErrorInfo 구조 |
| **UseCase 호출** | 올바른 파라미터로 호출했는지 | Mock 검증 |

### 검증하지 않는 것 (UseCase 책임)

- ❌ 비즈니스 로직 검증 → UseCase Test에서
- ❌ Domain 객체 생성 → Domain Test에서
- ❌ Database 저장/조회 → Persistence Test에서
- ❌ Transaction 관리 → Integration Test에서

---

## 2️⃣ 테스트 구조 (Test Structure)

### 기본 테스트 클래스 구조

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.adapter.in.rest.common.dto.ApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.command.CreateOrderApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderApiResponse;
import com.ryuqq.adapter.in.rest.order.mapper.OrderApiMapper;
import com.ryuqq.application.order.port.in.CreateOrderUseCase;
import com.ryuqq.application.order.dto.command.CreateOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderCommandController 단위 테스트
 *
 * <p>검증 범위:</p>
 * <ul>
 *   <li>HTTP 요청/응답 매핑</li>
 *   <li>Request DTO Validation</li>
 *   <li>Response DTO 직렬화</li>
 *   <li>HTTP Status Code</li>
 *   <li>UseCase 호출 검증</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@WebMvcTest(OrderCommandController.class)  // ✅ Controller만 로드 (슬라이스 테스트)
@DisplayName("OrderCommandController 단위 테스트")
class OrderCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private OrderApiMapper orderApiMapper;

    // 테스트 메서드들...
}
```

### 핵심 어노테이션

| 어노테이션 | 역할 | 설명 |
|----------|------|------|
| `@WebMvcTest` | Controller 슬라이스 테스트 | 특정 Controller만 로드, Spring Context 최소화 |
| `@MockBean` | UseCase/Mapper Mock | Spring Context에 Mock Bean 등록 |
| `@Autowired MockMvc` | HTTP 요청 테스트 | MockMvc로 HTTP 요청 시뮬레이션 |
| `@Autowired ObjectMapper` | JSON 변환 | Request/Response JSON 생성 |

### 테스트 태그 전략

**목적**: 테스트를 분류하여 선택적으로 실행할 수 있도록 함

#### 기본 태그

| 태그 | 대상 | 설명 | 실행 명령 |
|------|------|------|----------|
| `@Tag("unit")` | 단위 테스트 | 빠른 피드백 (100-300ms) | `./gradlew test --tests *Test` |
| `@Tag("integration")` | 통합 테스트 | 실제 DB 연동 (1-3초) | `./gradlew test --tests *IntTest` |
| `@Tag("architecture")` | ArchUnit 테스트 | 아키텍처 규칙 검증 | `./gradlew test --tests *ArchTest` |
| `@Tag("restdocs")` | REST Docs 테스트 | API 문서 생성 | `./gradlew test --tests *DocsTest` |

#### 레이어별 태그

| 태그 | 대상 | 설명 |
|------|------|------|
| `@Tag("adapter-rest")` | REST API Layer | Controller, DTO, Mapper |
| `@Tag("application")` | Application Layer | UseCase, Assembler |
| `@Tag("domain")` | Domain Layer | Aggregate, VO, Policy |
| `@Tag("persistence")` | Persistence Layer | Repository, Entity, Query |

#### 사용 예시

```java
@WebMvcTest(OrderCommandController.class)
@DisplayName("OrderCommandController 단위 테스트")
@Tag("unit")           // ✅ 단위 테스트
@Tag("adapter-rest")   // ✅ REST API Layer
class OrderCommandControllerTest {
    // 테스트 메서드들...
}
```

#### Gradle 설정 (선택적 실행)

```gradle
test {
    useJUnitPlatform {
        // 단위 테스트만 실행 (빠른 피드백)
        includeTags 'unit'

        // 또는 통합 테스트 제외
        excludeTags 'integration'

        // 또는 특정 레이어만 실행
        includeTags 'adapter-rest'
    }
}
```

#### 실행 예시

```bash
# 단위 테스트만 실행 (빠른 피드백)
./gradlew test -Dtest.includeTags=unit

# REST API Layer 테스트만 실행
./gradlew test -Dtest.includeTags=adapter-rest

# ArchUnit 테스트 제외하고 실행 (개발 중)
./gradlew test -Dtest.excludeTags=architecture

# 통합 테스트만 실행 (CI/CD)
./gradlew test -Dtest.includeTags=integration
```

---

## 3️⃣ Command Controller 테스트 (상태 변경)

### POST - 생성 테스트

```java
@Test
@DisplayName("POST /api/v1/orders - 주문 생성 성공 (201 Created)")
void createOrder_Success() throws Exception {
    // Given
    CreateOrderApiRequest request = new CreateOrderApiRequest(
        1L,  // customerId
        List.of(
            new CreateOrderApiRequest.OrderItem(101L, 2),
            new CreateOrderApiRequest.OrderItem(102L, 1)
        )
    );

    CreateOrderCommand command = new CreateOrderCommand(1L, List.of(/* ... */));
    OrderResponse useCaseResponse = new OrderResponse(1001L, "PLACED", /* ... */);
    OrderApiResponse apiResponse = OrderApiResponse.from(useCaseResponse);

    given(orderApiMapper.toCommand(any(CreateOrderApiRequest.class)))
        .willReturn(command);
    given(createOrderUseCase.execute(any(CreateOrderCommand.class)))
        .willReturn(useCaseResponse);
    given(orderApiMapper.toApiResponse(any(OrderResponse.class)))
        .willReturn(apiResponse);

    // When & Then
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())  // ✅ 201 Created
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.orderId").value(1001))
        .andExpect(jsonPath("$.data.status").value("PLACED"));

    // UseCase 호출 검증
    verify(createOrderUseCase).execute(any(CreateOrderCommand.class));
}
```

### PATCH - 부분 수정 테스트

```java
@Test
@DisplayName("PATCH /api/v1/orders/{id}/cancel - 주문 취소 성공 (200 OK)")
void cancelOrder_Success() throws Exception {
    // Given
    Long orderId = 1001L;

    // When & Then
    mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isEmpty());  // ✅ Void 응답

    verify(cancelOrderUseCase).execute(orderId);
}
```

### Validation 실패 테스트

```java
@Test
@DisplayName("POST /api/v1/orders - Validation 실패 (400 Bad Request)")
void createOrder_ValidationFail() throws Exception {
    // Given - 잘못된 요청 (customerId null)
    String invalidRequest = """
        {
            "customerId": null,
            "items": []
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
        .andExpect(status().isBadRequest())  // ✅ 400 Bad Request
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.type").exists())
        .andExpect(jsonPath("$.error.title").value("Validation Failed"))
        .andExpect(jsonPath("$.error.invalidParams").isArray());

    // UseCase 호출되지 않아야 함
    verify(createOrderUseCase, never()).execute(any());
}
```

---

## 4️⃣ Query Controller 테스트 (조회)

### GET - 단건 조회 테스트

```java
@Test
@DisplayName("GET /api/v1/orders/{id} - 주문 조회 성공 (200 OK)")
void getOrder_Success() throws Exception {
    // Given
    Long orderId = 1001L;
    OrderDetailResponse useCaseResponse = new OrderDetailResponse(/* ... */);
    OrderDetailApiResponse apiResponse = OrderDetailApiResponse.from(useCaseResponse);

    given(getOrderUseCase.execute(orderId)).willReturn(useCaseResponse);
    given(orderApiMapper.toDetailApiResponse(any())).willReturn(apiResponse);

    // When & Then
    mockMvc.perform(get("/api/v1/orders/{id}", orderId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.orderId").value(orderId))
        .andExpect(jsonPath("$.data.status").exists())
        .andExpect(jsonPath("$.data.items").isArray());

    verify(getOrderUseCase).execute(orderId);
}
```

### GET - 목록 조회 테스트 (Slice 페이징)

```java
@Test
@DisplayName("GET /api/v1/orders - 주문 검색 성공 (200 OK, Slice)")
void searchOrders_Success() throws Exception {
    // Given
    OrderSearchCriteria criteria = new OrderSearchCriteria(/* ... */);
    Slice<OrderSummaryResponse> useCaseResponse = new SliceImpl<>(
        List.of(/* ... */),
        PageRequest.of(0, 20),
        true  // hasNext
    );
    SliceApiResponse<OrderSummaryApiResponse> apiResponse =
        SliceApiResponse.from(useCaseResponse, OrderSummaryApiResponse::from);

    given(searchOrdersUseCase.execute(any())).willReturn(useCaseResponse);

    // When & Then
    mockMvc.perform(get("/api/v1/orders")
            .param("status", "PLACED")
            .param("page", "0")
            .param("size", "20")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(20));

    verify(searchOrdersUseCase).execute(any(OrderSearchCriteria.class));
}
```

### 404 Not Found 테스트

```java
@Test
@DisplayName("GET /api/v1/orders/{id} - 주문 없음 (404 Not Found)")
void getOrder_NotFound() throws Exception {
    // Given
    Long orderId = 9999L;
    given(getOrderUseCase.execute(orderId))
        .willThrow(new OrderNotFoundException(orderId));

    // When & Then
    mockMvc.perform(get("/api/v1/orders/{id}", orderId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())  // ✅ 404 Not Found
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.type").exists())
        .andExpect(jsonPath("$.error.title").value("Order Not Found"))
        .andExpect(jsonPath("$.error.status").value(404));
}
```

---

## 5️⃣ Exception 처리 테스트

### Domain Exception 테스트

```java
@Test
@DisplayName("PATCH /api/v1/orders/{id}/cancel - 취소 불가 상태 (400 Bad Request)")
void cancelOrder_InvalidStatus() throws Exception {
    // Given
    Long orderId = 1001L;
    given(cancelOrderUseCase.execute(orderId))
        .willThrow(new InvalidOrderStatusException("DELIVERED", "PLACED"));

    // When & Then
    mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())  // ✅ 400 Bad Request
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.type").exists())
        .andExpect(jsonPath("$.error.title").value("Invalid Order Status"))
        .andExpect(jsonPath("$.error.detail").exists());
}
```

### 500 Internal Server Error 테스트

```java
@Test
@DisplayName("POST /api/v1/orders - 예상치 못한 에러 (500 Internal Server Error)")
void createOrder_UnexpectedError() throws Exception {
    // Given
    CreateOrderApiRequest request = new CreateOrderApiRequest(/* ... */);
    given(createOrderUseCase.execute(any()))
        .willThrow(new RuntimeException("Unexpected error"));

    // When & Then
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError())  // ✅ 500
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.type").exists())
        .andExpect(jsonPath("$.error.title").value("Internal Server Error"));
}
```

---

## 6️⃣ MockMvc 패턴 모음

### Request 패턴

```java
// POST with JSON Body
mockMvc.perform(post("/api/v1/orders")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(request)));

// GET with Path Variable
mockMvc.perform(get("/api/v1/orders/{id}", orderId)
    .accept(MediaType.APPLICATION_JSON));

// GET with Query Parameters
mockMvc.perform(get("/api/v1/orders")
    .param("status", "PLACED")
    .param("page", "0")
    .param("size", "20"));

// PATCH with Path Variable
mockMvc.perform(patch("/api/v1/orders/{id}/cancel", orderId)
    .contentType(MediaType.APPLICATION_JSON));

// PUT with JSON Body
mockMvc.perform(put("/api/v1/orders/{id}", orderId)
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(request)));
```

### Response 검증 패턴

```java
// HTTP Status
.andExpect(status().isCreated())        // 201
.andExpect(status().isOk())             // 200
.andExpect(status().isBadRequest())     // 400
.andExpect(status().isNotFound())       // 404
.andExpect(status().isInternalServerError())  // 500

// JSON Path
.andExpect(jsonPath("$.success").value(true))
.andExpect(jsonPath("$.data.orderId").value(1001))
.andExpect(jsonPath("$.data.items").isArray())
.andExpect(jsonPath("$.data.items[0].productId").value(101))
.andExpect(jsonPath("$.error.type").exists())
.andExpect(jsonPath("$.error.invalidParams").isArray())

// Content Type
.andExpect(content().contentType(MediaType.APPLICATION_JSON))

// Header
.andExpect(header().string("Location", "/api/v1/orders/1001"))
```

---

## 7️⃣ Test Fixture 패턴

### Fixture 클래스 작성

```java
package com.ryuqq.adapter.in.rest.order.fixture;

import com.ryuqq.adapter.in.rest.order.dto.command.CreateOrderApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderApiResponse;
import com.ryuqq.application.order.dto.response.OrderResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order 관련 테스트 Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderFixture {

    /**
     * 기본 주문 생성 요청 Fixture
     */
    public static CreateOrderApiRequest createOrderRequest() {
        return new CreateOrderApiRequest(
            1L,  // customerId
            List.of(
                new CreateOrderApiRequest.OrderItem(101L, 2),
                new CreateOrderApiRequest.OrderItem(102L, 1)
            )
        );
    }

    /**
     * UseCase Response Fixture
     */
    public static OrderResponse orderResponse() {
        return new OrderResponse(
            1001L,
            "PLACED",
            1L,
            LocalDateTime.now(),
            List.of(/* ... */)
        );
    }

    /**
     * API Response Fixture
     */
    public static OrderApiResponse orderApiResponse() {
        return new OrderApiResponse(
            1001L,
            "PLACED",
            LocalDateTime.now()
        );
    }
}
```

### Fixture 사용

```java
@Test
@DisplayName("POST /api/v1/orders - Fixture 사용")
void createOrder_WithFixture() throws Exception {
    // Given
    CreateOrderApiRequest request = OrderFixture.createOrderRequest();
    OrderResponse useCaseResponse = OrderFixture.orderResponse();
    OrderApiResponse apiResponse = OrderFixture.orderApiResponse();

    given(orderApiMapper.toCommand(any())).willReturn(/* ... */);
    given(createOrderUseCase.execute(any())).willReturn(useCaseResponse);
    given(orderApiMapper.toApiResponse(any())).willReturn(apiResponse);

    // When & Then
    mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
}
```

---

## 8️⃣ Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. @WebMvcTest로 Controller만 테스트
@WebMvcTest(OrderCommandController.class)

// ✅ 2. UseCase는 Mock으로 처리
@MockBean
private CreateOrderUseCase createOrderUseCase;

// ✅ 3. HTTP 계층 검증에 집중
.andExpect(status().isCreated())
.andExpect(jsonPath("$.data.orderId").value(1001))

// ✅ 4. Validation 검증
.andExpect(jsonPath("$.error.invalidParams").isArray())

// ✅ 5. UseCase 호출 검증
verify(createOrderUseCase).execute(any(CreateOrderCommand.class));

// ✅ 6. Fixture 패턴 사용
CreateOrderApiRequest request = OrderFixture.createOrderRequest();

// ✅ 7. DisplayName으로 명확한 의도 표현
@DisplayName("POST /api/v1/orders - 주문 생성 성공 (201 Created)")

// ✅ 8. BDD 스타일 (Given-When-Then)
// Given
CreateOrderApiRequest request = ...;
given(createOrderUseCase.execute(any())).willReturn(...);

// When & Then
mockMvc.perform(...)
    .andExpect(...);
```

### ❌ Anti-Patterns

```java
// ❌ 1. @SpringBootTest 사용 (느림)
@SpringBootTest
@AutoConfigureMockMvc
class OrderCommandControllerTest { }  // ❌ 전체 Context 로드

// ❌ 2. 실제 UseCase 사용 (통합 테스트가 됨)
@Autowired
private CreateOrderUseCase createOrderUseCase;  // ❌ Mock 사용해야 함

// ❌ 3. 비즈니스 로직 검증
assertThat(order.getStatus()).isEqualTo("PLACED");  // ❌ UseCase Test 책임

// ❌ 4. Domain 객체 검증
assertThat(order.getTotalAmount()).isEqualTo(10000);  // ❌ Domain Test 책임

// ❌ 5. Database 조회 검증
verify(orderRepository).save(any());  // ❌ Persistence Test 책임

// ❌ 6. 테스트 이름이 불명확
@Test
void test1() { }  // ❌ 무엇을 테스트하는지 불명확

// ❌ 7. 하드코딩된 JSON 문자열 (가독성 낮음)
String json = "{\"customerId\":1,\"items\":[...]}";  // ❌ ObjectMapper 사용

// ❌ 8. Mock 검증 누락
mockMvc.perform(...).andExpect(status().isOk());
// ❌ UseCase 호출 검증 없음
```

---

## 9️⃣ 성능 최적화

### @WebMvcTest vs @SpringBootTest

| 항목 | @WebMvcTest | @SpringBootTest |
|------|-------------|-----------------|
| **로딩 범위** | Controller만 | 전체 Context |
| **실행 속도** | 빠름 (100-300ms) | 느림 (3-5초) |
| **사용 목적** | 단위 테스트 | 통합 테스트 |
| **Mock 필요** | ✅ 필요 | ❌ 불필요 (실제 Bean) |

### 빠른 테스트 팁

```java
// ✅ 1. 특정 Controller만 로드
@WebMvcTest(OrderCommandController.class)  // ✅ 빠름

@WebMvcTest  // ❌ 모든 Controller 로드 (느림)

// ✅ 2. @MockBean 최소화
@MockBean
private CreateOrderUseCase createOrderUseCase;  // ✅ 필요한 것만

@MockBean
private ApplicationContext applicationContext;  // ❌ 불필요

// ✅ 3. 테스트 격리 (독립 실행)
@Test
void createOrder() {
    // ✅ 다른 테스트에 영향 없음
}
```

---

## 🔟 체크리스트

### 작성 전 체크리스트

- [ ] `@WebMvcTest(XxxController.class)` 사용
- [ ] UseCase는 `@MockBean`으로 Mock 처리
- [ ] Mapper는 `@MockBean`으로 Mock 처리
- [ ] `MockMvc`와 `ObjectMapper` 주입
- [ ] Test Fixture 클래스 작성 (선택)

### 테스트 메서드 체크리스트

- [ ] `@DisplayName`으로 명확한 의도 표현
- [ ] Given-When-Then 구조 준수
- [ ] HTTP Status Code 검증
- [ ] Response Body JSON 구조 검증
- [ ] UseCase 호출 검증 (`verify`)
- [ ] Validation 실패 케이스 포함
- [ ] Exception 처리 케이스 포함
- [ ] 404 Not Found 케이스 포함 (조회)

### 리뷰 체크리스트

- [ ] 비즈니스 로직 검증하지 않음 (UseCase Test로 분리)
- [ ] Domain 객체 검증하지 않음 (Domain Test로 분리)
- [ ] Database 검증하지 않음 (Persistence Test로 분리)
- [ ] HTTP 계층 검증에만 집중
- [ ] 모든 테스트가 독립적으로 실행 가능
- [ ] 테스트 실행 시간 < 1초 (빠른 피드백)

---

## 1️⃣1️⃣ 예제 프로젝트 구조

```
adapter-in/rest-api/
└── src/
    ├── main/java/
    │   └── com/ryuqq/adapter/in/rest/
    │       └── order/
    │           ├── controller/
    │           │   ├── OrderCommandController.java
    │           │   └── OrderQueryController.java
    │           ├── dto/
    │           │   ├── command/
    │           │   │   └── CreateOrderApiRequest.java
    │           │   └── response/
    │           │       └── OrderApiResponse.java
    │           └── mapper/
    │               └── OrderApiMapper.java
    │
    └── test/java/
        └── com/ryuqq/adapter/in/rest/
            └── order/
                ├── controller/
                │   ├── OrderCommandControllerTest.java  # ← Controller 단위 테스트
                │   └── OrderQueryControllerTest.java
                └── fixture/
                    └── OrderFixture.java               # ← Test Fixture
```

---

## 1️⃣2️⃣ 추가 가이드 링크

- **[Controller Guide](controller-guide.md)** - Controller 작성 가이드
- **[Controller ArchUnit Guide](controller-archunit.md)** - 아키텍처 검증 가이드
- **[REST Docs Guide](controller-restdocs-guide.md)** - 테스트 기반 API 문서 자동 생성 가이드
- **[E2E Test Guide](../../../05-testing/e2e-testing/e2e-test-guide.md)** - 전체 시스템 E2E 테스트 가이드 (TBD)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
