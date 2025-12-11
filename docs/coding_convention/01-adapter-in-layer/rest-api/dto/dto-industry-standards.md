# DTO Industry Standards — **업계 표준 가이드**

> 이 문서는 REST API DTO 설계 시 준수해야 할 **업계 표준**을 정리합니다.
>
> **Zalando**, **Microsoft**, **Google**, **Stripe** 등 주요 API 가이드라인을 참조하여 작성되었습니다.

---

## 1) 참조 표준

| 출처 | 가이드라인 | 링크 |
|------|-----------|------|
| Zalando | RESTful API Guidelines | https://opensource.zalando.com/restful-api-guidelines/ |
| Microsoft | REST API Guidelines | https://github.com/microsoft/api-guidelines |
| Google | JSON Style Guide | https://google.github.io/styleguide/jsoncstyleguide.xml |
| Stripe | API Design Principles | https://docs.stripe.com/api |
| Spring | DTO Best Practices | https://www.baeldung.com/java-dto-pattern |

---

## 2) 날짜/시간 형식 (ISO 8601)

### 표준

**RFC 3339 / ISO 8601** 형식을 **필수** 사용합니다.

```
yyyy-MM-dd'T'HH:mm:ss.SSSXXX
```

**출처**: [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines), [Zalando Guidelines](https://opensource.zalando.com/restful-api-guidelines/)

### 글로벌 설정

```yaml
# application.yml
spring:
  jackson:
    date-format: "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
```

### DTO 예시

```java
public record OrderApiResponse(
    Long orderId,
    LocalDateTime createdAt,   // → "2025-12-04T10:30:00.000Z"
    LocalDate orderDate        // → "2025-12-04"
) {}
```

### Do / Don't

```java
// ✅ Good: ISO 8601 + UTC
"createdAt": "2025-12-04T10:30:00.000Z"
"orderDate": "2025-12-04"

// ❌ Bad: 로컬 시간, 비표준 형식
"createdAt": "2025-12-04 19:30:00"
"orderDate": "25/12/04"

// ❌ Bad: Unix Timestamp (가독성 저하)
"createdAt": 1733307000000
```

### 타임존 규칙

| 상황 | 권장 |
|------|------|
| 저장/전송 | **UTC** 기준 |
| 표시 | 클라이언트에서 로컬 변환 |
| API 응답 | `Z` 접미사 (UTC) 또는 `+09:00` (오프셋 명시) |

---

## 3) JSON 필드 네이밍 규칙

### 표준

**camelCase** 사용을 기본으로 합니다. (Java 표준과 일치)

**예외**: 외부 시스템 연동 시 **snake_case** 필요한 경우 `@JsonProperty` 허용.

### 정책

```java
// ✅ Good: Java camelCase (기본)
public record OrderApiResponse(
    Long orderId,
    Long customerId,
    LocalDateTime createdAt
) {}
// → {"orderId": 1, "customerId": 100, "createdAt": "..."}

// ✅ Allowed: 외부 API 계약 시 snake_case (명시적 승인 필요)
public record ExternalOrderApiResponse(
    @JsonProperty("order_id") Long orderId,
    @JsonProperty("customer_id") Long customerId
) {}
// → {"order_id": 1, "customer_id": 100}
```

### Jackson 어노테이션 정책

| 어노테이션 | 허용 여부 | 조건 |
|-----------|----------|------|
| `@JsonProperty` | ⚠️ 제한적 허용 | 외부 API 계약 시 (코드 리뷰 필수) |
| `@JsonIgnore` | ⚠️ 제한적 허용 | 내부 필드 숨김 시 |
| `@JsonFormat` | ❌ 금지 | 글로벌 설정으로 통일 |
| `@JsonInclude` | ❌ 금지 | 응답 일관성 유지 |
| `@JsonNaming` | ❌ 금지 | 개별 DTO에서 사용 금지 |

**출처**: [Zalando Guidelines - snake_case 권장](https://opensource.zalando.com/restful-api-guidelines/), [Google JSON Style Guide](https://google.github.io/styleguide/jsoncstyleguide.xml)

---

## 4) DTO 버전 관리

### 호환성 규칙

**클라이언트는 알 수 없는 필드를 무시해야 합니다.** (Forward Compatibility)

**출처**: [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)

### Breaking vs Non-Breaking 변경

| 변경 유형 | Breaking? | 권장 조치 |
|----------|-----------|----------|
| 필드 추가 | ❌ Non-breaking | 허용 (버전 유지) |
| Optional → Required 변경 | ✅ Breaking | 새 API 버전 필요 |
| 필드 제거 | ✅ Breaking | 새 API 버전 필요 |
| 필드 이름 변경 | ✅ Breaking | 새 API 버전 필요 |
| 타입 변경 | ✅ Breaking | 새 API 버전 필요 |
| Enum 값 추가 | ⚠️ 주의 | 클라이언트 대응 필요 |

### 버전 관리 전략

```java
// ✅ Good: 필드 추가 (Non-breaking)
// v1.0
public record OrderApiResponse(
    Long orderId,
    Long customerId
) {}

// v1.1 (호환)
public record OrderApiResponse(
    Long orderId,
    Long customerId,
    String memo           // ← 추가 (Optional, 기존 클라이언트 영향 없음)
) {}

// ❌ Bad: 필드 제거 (Breaking - 새 버전 필요)
// v2.0 (/v2/orders)
public record OrderApiResponseV2(
    Long orderId
    // customerId 제거 → Breaking Change!
) {}
```

### Deprecation 패턴

```java
/**
 * Order 응답
 *
 * @param orderId 주문 ID
 * @param customerId 고객 ID
 * @param customerNo 고객 번호
 * @deprecated customerId 대신 customerNo 사용 (v2.0에서 제거 예정)
 */
public record OrderApiResponse(
    Long orderId,
    @Deprecated Long customerId,  // v2.0에서 제거 예정
    Long customerNo               // 새 필드
) {}
```

---

## 5) Idempotency (멱등성)

### 표준

**POST 요청의 중복 처리 방지**를 위해 Idempotency Key 패턴을 권장합니다.

**출처**: [Stripe Idempotency](https://docs.stripe.com/api-v2-overview), [Microsoft Guidelines](https://github.com/microsoft/api-guidelines)

### HTTP 메서드별 멱등성

| 메서드 | 멱등성 | 비고 |
|--------|--------|------|
| GET | ✅ 보장 | 항상 안전하게 재시도 가능 |
| PUT | ✅ 보장 | 동일 요청은 동일 결과 |
| DELETE | ✅ 보장 | 재시도 가능 |
| POST | ❌ 미보장 | Idempotency Key 필요 |
| PATCH | ⚠️ 구현 의존 | 설계에 따라 다름 |

### 구현 패턴

#### Request Header 방식 (권장)

```http
POST /api/orders
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{
  "customerId": 100,
  "items": [...]
}
```

```java
@PostMapping("/orders")
public ResponseEntity<ApiResponse<OrderCreatedApiResponse>> createOrder(
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody CreateOrderApiRequest request
) {
    // 1. Idempotency Key로 중복 체크
    // 2. 중복이면 기존 결과 반환
    // 3. 신규면 처리 후 결과 저장
}
```

#### Request Body 방식 (대안)

```java
public record CreateOrderApiRequest(
    @NotNull Long customerId,
    @NotEmpty @Valid List<OrderItemRequest> items,
    String idempotencyKey  // Optional: 클라이언트 제공
) {}
```

### Idempotency Key 규칙

- **형식**: UUID v4 권장
- **유효 기간**: 24시간 (설정 가능)
- **저장소**: Redis 또는 DB
- **응답**: 중복 요청 시 **동일한 응답** 반환 (HTTP 200)

---

## 6) Partial Update (PATCH)

### 표준

**JSON Merge Patch (RFC 7396)** 패턴을 권장합니다.

**출처**: [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines), [Zalando Guidelines](https://opensource.zalando.com/restful-api-guidelines/)

### 패턴

```java
/**
 * Order 부분 수정 요청 (PATCH)
 *
 * <p>필드별 null 처리:
 * <ul>
 *   <li>null: 변경 안 함 (필드 미포함과 동일)</li>
 *   <li>값 있음: 해당 값으로 변경</li>
 * </ul>
 *
 * @param orderId 주문 ID (필수, 변경 대상 식별)
 * @param status 주문 상태 (Optional)
 * @param memo 메모 (Optional)
 */
public record UpdateOrderApiRequest(
    @NotNull Long orderId,      // 필수: 변경 대상 식별
    String status,              // Optional: null이면 변경 안 함
    String memo                 // Optional: null이면 변경 안 함
) {}
```

### Request 예시

```json
// 전체 수정 (PUT)
PUT /api/orders/123
{
  "status": "CONFIRMED",
  "memo": "긴급 배송",
  "shippingAddress": "서울시..."
}

// 부분 수정 (PATCH) - status만 변경
PATCH /api/orders/123
{
  "status": "CONFIRMED"
}
// memo, shippingAddress는 변경되지 않음
```

### Do / Don't

```java
// ✅ Good: Optional 필드, null 허용
public record UpdateOrderApiRequest(
    @NotNull Long orderId,
    String status,              // null = 변경 안 함
    String memo                 // null = 변경 안 함
) {}

// ❌ Bad: 모든 필드 @NotNull (PATCH에 부적합)
public record UpdateOrderApiRequest(
    @NotNull Long orderId,
    @NotNull String status,     // ❌ PATCH인데 필수?
    @NotNull String memo        // ❌ PATCH인데 필수?
) {}

// ❌ Bad: Optional<T> 사용 (Jackson 직렬화 복잡)
public record UpdateOrderApiRequest(
    @NotNull Long orderId,
    Optional<String> status     // ❌ 직렬화 복잡, 권장하지 않음
) {}
```

---

## 7) 에러 응답 형식 (RFC 7807)

### 표준

**RFC 7807 Problem Details** 형식을 사용합니다.

**출처**: [Zalando Guidelines](https://opensource.zalando.com/restful-api-guidelines/), [Spring Framework 6.0+](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

### Content-Type

```http
Content-Type: application/problem+json
```

### 응답 구조

```java
/**
 * RFC 7807 Problem Details 응답
 */
public record ProblemDetail(
    String type,      // 에러 타입 URI (문서 링크)
    String title,     // 사람이 읽을 수 있는 제목
    int status,       // HTTP 상태 코드
    String detail,    // 상세 설명
    String instance,  // 요청 식별자 (URI)
    String code,      // 내부 에러 코드 (확장 필드)
    LocalDateTime timestamp  // 발생 시간 (확장 필드)
) {}
```

### 응답 예시

```json
{
  "type": "https://api.example.com/errors/order-not-found",
  "title": "Order Not Found",
  "status": 404,
  "detail": "Order with ID 12345 was not found",
  "instance": "/api/orders/12345",
  "code": "ORDER_NOT_FOUND",
  "timestamp": "2025-12-04T10:30:00.000Z"
}
```

### Spring 6.0+ 통합

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFound(
        OrderNotFoundException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.example.com/errors/order-not-found"));
        problem.setTitle("Order Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "ORDER_NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
}
```

---

## 8) 페이징 표준

### Cursor vs Offset 선택

| 구분 | Cursor (Slice) | Offset (Page) |
|------|----------------|---------------|
| **성능** | 빠름 (COUNT 불필요) | 느림 (COUNT 필수) |
| **일관성** | 데이터 추가/삭제에 강함 | 페이지 이탈 가능 |
| **사용 사례** | 무한 스크롤, 모바일 | 관리자 테이블 |
| **표준** | Stripe, Twitter | 전통적 웹 |

**출처**: [Stripe Pagination](https://docs.stripe.com/api/pagination)

### Cursor 응답 형식

```json
{
  "data": {
    "content": [...],
    "size": 20,
    "hasNext": true,
    "nextCursor": "eyJpZCI6MTIzNH0="
  }
}
```

### Offset 응답 형식

```json
{
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

---

## 9) 체크리스트

### 날짜/시간
- [ ] ISO 8601 형식 사용 (`yyyy-MM-dd'T'HH:mm:ss.SSSXXX`)
- [ ] UTC 기준 저장/전송
- [ ] 글로벌 Jackson 설정 적용

### 네이밍
- [ ] camelCase 기본 사용
- [ ] snake_case 필요 시 `@JsonProperty` 명시 (리뷰 필수)

### 버전 관리
- [ ] 필드 추가는 Non-breaking (버전 유지)
- [ ] 필드 제거/변경은 새 API 버전

### 멱등성
- [ ] POST 요청 시 Idempotency Key 고려
- [ ] UUID v4 형식 사용

### 부분 수정
- [ ] PATCH 요청은 Optional 필드 사용
- [ ] null = 변경 안 함

### 에러 응답
- [ ] RFC 7807 Problem Details 준수
- [ ] `application/problem+json` Content-Type

### 페이징
- [ ] 일반 사용자: Cursor (SliceApiResponse)
- [ ] 관리자: Offset (PageApiResponse)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
