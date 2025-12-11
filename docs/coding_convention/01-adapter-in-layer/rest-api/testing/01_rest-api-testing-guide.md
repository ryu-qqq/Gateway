# REST API Layer 통합 테스트 가이드

> **목적**: REST API Layer의 통합 테스트 작성 규칙 및 패턴 정의

---

## 1. 개요

### REST API Layer 테스트 전략

```
┌─────────────────────────────────────────────────────────────┐
│  REST API Layer 테스트 피라미드                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    ┌─────────┐                              │
│                    │ E2E     │  ← 통합 테스트 (필수)          │
│                    │ Test    │    TestRestTemplate          │
│                ┌───┴─────────┴───┐                          │
│                │  Unit Test      │  ← 단위 테스트 (선택적)    │
│                │  (Mapper, DTO)  │    JUnit 5               │
│            ┌───┴─────────────────┴───┐                      │
│            │  ArchUnit Tests         │  ← 아키텍처 검증 (필수) │
│            │  (Architecture Rules)    │    Zero-Tolerance     │
│            └─────────────────────────┘                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 테스트 유형별 역할

| 테스트 유형 | 필수 여부 | 목적 | 도구 |
|------------|----------|------|------|
| **통합 테스트** | ✅ 필수 | 전체 레이어 통합 검증 | TestRestTemplate |
| **단위 테스트** | 🔶 선택 | Mapper, DTO 개별 검증 | JUnit 5 |
| **ArchUnit** | ✅ 필수 | 아키텍처 규칙 강제 | ArchUnit |
| **REST Docs** | 🔶 권장 | API 문서 자동화 | Spring REST Docs |

---

## 2. 통합 테스트 (Integration Test)

### 2.1 필수 어노테이션

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Order API 통합 테스트")
class OrderApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    // 테스트 메서드...
}
```

### 2.2 테스트 클래스 명명 규칙

| 패턴 | 용도 | 예시 |
|------|------|------|
| `*ApiIntegrationTest` | API 엔드포인트 통합 테스트 | `OrderApiIntegrationTest` |
| `*ControllerTest` | 컨트롤러 단위 테스트 (권장하지 않음) | - |

### 2.3 테스트 메서드 구조

```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("POST /api/v1/orders - 주문 생성 성공")
void createOrder_Success() {
    // Given - 요청 데이터 준비
    PlaceOrderApiRequest request = new PlaceOrderApiRequest(
        1L,           // customerId
        100L,         // productId
        10            // quantity
    );

    // When - 실제 HTTP 요청
    ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.POST,
        new HttpEntity<>(request),
        new ParameterizedTypeReference<>() {}
    );

    // Then - 응답 검증
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isTrue();
    assertThat(response.getBody().data().orderId()).isNotNull();
}
```

### 2.4 HTTP 메서드별 테스트 패턴

#### GET (조회)

```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("GET /api/v1/orders/{orderId} - 주문 단건 조회")
void getOrder_Success() {
    // Given
    Long orderId = 100L;

    // When
    ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
        "/api/v1/orders/{orderId}",
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<>() {},
        orderId
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().data().orderId()).isEqualTo(orderId);
}
```

#### POST (생성)

```java
@Test
@Sql("/sql/customers-test-data.sql")
@DisplayName("POST /api/v1/orders - 주문 생성")
void createOrder_Success() {
    // Given
    PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 100L, 5);

    // When
    ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.POST,
        new HttpEntity<>(request),
        new ParameterizedTypeReference<>() {}
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
}
```

#### PUT (전체 수정)

```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("PUT /api/v1/orders/{orderId} - 주문 전체 수정")
void updateOrder_Success() {
    // Given
    Long orderId = 100L;
    UpdateOrderApiRequest request = new UpdateOrderApiRequest("CONFIRMED", 20);

    // When
    ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
        "/api/v1/orders/{orderId}",
        HttpMethod.PUT,
        new HttpEntity<>(request),
        new ParameterizedTypeReference<>() {},
        orderId
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

#### PATCH (부분 수정)

```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("PATCH /api/v1/orders/{orderId}/status - 주문 상태 변경")
void patchOrderStatus_Success() {
    // Given
    Long orderId = 100L;
    PatchOrderStatusApiRequest request = new PatchOrderStatusApiRequest("SHIPPED");

    // When
    ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
        "/api/v1/orders/{orderId}/status",
        HttpMethod.PATCH,
        new HttpEntity<>(request),
        new ParameterizedTypeReference<>() {},
        orderId
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

---

## 3. Zero-Tolerance 규칙

### 3.1 필수 규칙 ✅

| 규칙 | 설명 | 검증 방법 |
|------|------|----------|
| `@SpringBootTest(RANDOM_PORT)` | 전체 컨텍스트 + 실제 HTTP | ArchUnit |
| `TestRestTemplate` 사용 | 실제 HTTP 요청/응답 | ArchUnit |
| `@Transactional` | 테스트 격리 | ArchUnit |
| `@Testcontainers` | 실제 DB 사용 | ArchUnit |
| `@Sql` 데이터 | INSERT만 포함 | Code Review |

### 3.2 금지 규칙 ❌

| 금지 항목 | 이유 | 대안 |
|----------|------|------|
| `MockMvc` | 가짜 HTTP, 직렬화 미검증 | `TestRestTemplate` |
| `@WebMvcTest` | 부분 컨텍스트 | `@SpringBootTest` |
| `@MockBean` 남발 | 실제 통합 검증 불가 | 실제 Bean 사용 |
| `@Sql`에 DDL | Flyway 역할 침범 | Flyway 마이그레이션 |
| `H2 Database` | 운영 환경과 불일치 | TestContainers |

### 3.3 MockMvc 금지 이유

```java
// ❌ WRONG - MockMvc 사용 금지
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{...}"))
            .andExpect(status().isCreated());
        // 문제: 실제 직렬화/역직렬화 검증 안 됨
        // 문제: 실제 HTTP 헤더/쿠키 동작 검증 안 됨
        // 문제: 필터 체인 동작 검증 안 됨
    }
}

// ✅ CORRECT - TestRestTemplate 사용
@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrderApiIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createOrder() {
        ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<>() {}
        );
        // 장점: 실제 HTTP 요청/응답
        // 장점: 실제 직렬화/역직렬화
        // 장점: 필터, 인터셉터 모두 동작
    }
}
```

---

## 4. 에러 케이스 테스트

### 4.1 Validation 에러 (400 Bad Request)

```java
@Test
@DisplayName("POST /api/v1/orders - 필수 필드 누락 시 400 에러")
void createOrder_ValidationError_MissingField() {
    // Given - 필수 필드 누락
    PlaceOrderApiRequest request = new PlaceOrderApiRequest(
        null,   // customerId 누락
        100L,
        10
    );

    // When
    ResponseEntity<ProblemDetail> response = restTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.POST,
        new HttpEntity<>(request),
        ProblemDetail.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getType()).hasToString("about:blank");
    assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
}
```

### 4.2 Not Found 에러 (404)

```java
@Test
@DisplayName("GET /api/v1/orders/{orderId} - 존재하지 않는 주문 조회 시 404 에러")
void getOrder_NotFound() {
    // Given
    Long nonExistentOrderId = 999999L;

    // When
    ResponseEntity<ProblemDetail> response = restTemplate.exchange(
        "/api/v1/orders/{orderId}",
        HttpMethod.GET,
        null,
        ProblemDetail.class,
        nonExistentOrderId
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

### 4.3 비즈니스 에러 (422 Unprocessable Entity)

```java
@Test
@Sql("/sql/orders-test-data.sql")
@DisplayName("POST /api/v1/orders/{orderId}/cancel - 이미 배송된 주문 취소 시 422 에러")
void cancelOrder_AlreadyShipped() {
    // Given - 배송 완료된 주문
    Long shippedOrderId = 102L;

    // When
    ResponseEntity<ProblemDetail> response = restTemplate.exchange(
        "/api/v1/orders/{orderId}/cancel",
        HttpMethod.POST,
        null,
        ProblemDetail.class,
        shippedOrderId
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody().getDetail()).contains("배송된 주문은 취소할 수 없습니다");
}
```

---

## 5. 테스트 데이터 관리

### 5.1 @Sql 파일 구조

```
src/test/resources/sql/
├── common/
│   └── cleanup.sql              # 공통 정리 스크립트
├── orders/
│   ├── orders-test-data.sql     # 주문 테스트 데이터
│   └── orders-edge-cases.sql    # 엣지 케이스 데이터
└── customers/
    └── customers-test-data.sql  # 고객 테스트 데이터
```

### 5.2 @Sql 파일 작성 규칙

```sql
-- src/test/resources/sql/orders/orders-test-data.sql

-- 1. 기존 데이터 정리 (FK 순서 고려: 자식 먼저)
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM customers;

-- 2. 부모 테이블 먼저 삽입
INSERT INTO customers (customer_id, name, email)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Alice', 'alice@example.com'),
    (2, 'Bob', 'bob@example.com');

-- 3. 자식 테이블 삽입
INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
OVERRIDING SYSTEM VALUE
VALUES
    (100, 1, 'PENDING', 10000, '2024-01-01'),
    (101, 1, 'CONFIRMED', 20000, '2024-01-02'),
    (102, 2, 'SHIPPED', 30000, '2024-01-03');

-- 4. 시퀀스 리셋 (다음 INSERT를 위해)
SELECT setval('orders_order_id_seq', 200, false);
SELECT setval('customers_customer_id_seq', 100, false);
```

### 5.3 Test Fixtures 활용

복잡한 테스트 데이터는 Test Fixtures 패턴을 활용합니다.

> **참고**: [Test Fixtures 가이드](../../../05-testing/test-fixtures/01_test-fixtures-guide.md)

---

## 6. 인증/인가 테스트

### 6.1 인증 없이 접근 시 401 에러

```java
@Test
@DisplayName("인증 없이 보호된 API 접근 시 401 에러")
void accessProtectedApi_WithoutAuth_Returns401() {
    // When - 인증 헤더 없이 요청
    ResponseEntity<ProblemDetail> response = restTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.GET,
        null,
        ProblemDetail.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

### 6.2 인증된 요청 테스트

```java
@Test
@Sql("/sql/users-test-data.sql")
@DisplayName("유효한 JWT로 보호된 API 접근 성공")
void accessProtectedApi_WithValidJwt_Success() {
    // Given - JWT 토큰 발급
    String accessToken = jwtTokenProvider.createAccessToken(1L, "USER");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    // When
    ResponseEntity<ApiResponse<List<OrderApiResponse>>> response = restTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {}
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

### 6.3 권한 부족 시 403 에러

```java
@Test
@Sql("/sql/users-test-data.sql")
@DisplayName("권한 부족 시 403 에러")
void accessAdminApi_WithUserRole_Returns403() {
    // Given - USER 권한 토큰
    String accessToken = jwtTokenProvider.createAccessToken(1L, "USER");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    // When - ADMIN 전용 API 접근
    ResponseEntity<ProblemDetail> response = restTemplate.exchange(
        "/api/v1/admin/orders",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        ProblemDetail.class
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

---

## 7. 페이징/정렬 테스트

### 7.1 Slice 기반 페이징

```java
@Test
@Sql("/sql/orders-bulk-test-data.sql")  // 100개 주문 데이터
@DisplayName("GET /api/v1/orders - 페이징 조회")
void getOrders_WithPaging() {
    // Given
    String url = "/api/v1/orders?page=0&size=10&sort=orderDate,desc";

    // When
    ResponseEntity<ApiResponse<SliceApiResponse<OrderApiResponse>>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        new HttpEntity<>(authHeaders),
        new ParameterizedTypeReference<>() {}
    );

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    SliceApiResponse<OrderApiResponse> slice = response.getBody().data();
    assertThat(slice.content()).hasSize(10);
    assertThat(slice.hasNext()).isTrue();
    assertThat(slice.number()).isEqualTo(0);
}
```

---

## 8. 테스트 클래스 템플릿

### 8.1 기본 통합 테스트 템플릿

```java
package com.ryuqq.adapter.in.rest.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Order API 통합 테스트
 *
 * <p><strong>테스트 범위:</strong>
 * <ul>
 *   <li>주문 생성 API (POST /api/v1/orders)</li>
 *   <li>주문 조회 API (GET /api/v1/orders/{orderId})</li>
 *   <li>주문 목록 조회 API (GET /api/v1/orders)</li>
 *   <li>주문 상태 변경 API (PATCH /api/v1/orders/{orderId}/status)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 * @see OrderController
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Order API 통합 테스트")
class OrderApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        authHeaders = new HttpHeaders();
        // 테스트용 인증 헤더 설정 (필요시)
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrder {

        @Test
        @Sql("/sql/customers-test-data.sql")
        @DisplayName("성공 - 주문 생성")
        void success() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 100L, 5);

            // When
            ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                new ParameterizedTypeReference<>() {}
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().data().orderId()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        void fail_MissingRequiredField() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(null, 100L, 5);

            // When
            ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders),
                ProblemDetail.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{orderId}")
    class GetOrder {

        @Test
        @Sql("/sql/orders-test-data.sql")
        @DisplayName("성공 - 주문 단건 조회")
        void success() {
            // Given
            Long orderId = 100L;

            // When
            ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate.exchange(
                "/api/v1/orders/{orderId}",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                new ParameterizedTypeReference<>() {},
                orderId
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().orderId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void fail_NotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When
            ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                "/api/v1/orders/{orderId}",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                ProblemDetail.class,
                nonExistentId
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
```

---

## 9. 체크리스트

### 통합 테스트 작성 전

- [ ] `@SpringBootTest(webEnvironment = RANDOM_PORT)` 사용
- [ ] `@ActiveProfiles("test")` 설정
- [ ] `@Testcontainers` + `PostgreSQLContainer` 설정
- [ ] `@Transactional` 설정
- [ ] `TestRestTemplate` 주입

### 테스트 메서드 작성

- [ ] `@DisplayName`으로 테스트 의도 명시
- [ ] `@Sql`로 테스트 데이터 준비
- [ ] Given-When-Then 구조 준수
- [ ] `ParameterizedTypeReference` 사용 (제네릭 응답)
- [ ] HTTP 상태 코드 검증
- [ ] 응답 본문 검증

### 금지 사항 확인

- [ ] MockMvc 사용하지 않음
- [ ] @WebMvcTest 사용하지 않음
- [ ] @MockBean 남발하지 않음
- [ ] @Sql에 DDL 작성하지 않음

---

## 10. 참고 문서

- [통합 테스트 종합 가이드](../../../05-testing/integration-testing/01_integration-testing-overview.md)
- [Test Fixtures 가이드](../../../05-testing/test-fixtures/01_test-fixtures-guide.md)
- [Test Fixtures ArchUnit](../../../05-testing/test-fixtures/02_test-fixtures-archunit.md)
- [REST API Layer 전체 가이드](../rest-api-guide.md)
- [Error 처리 가이드](../error/error-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
