# Error Handling Strategy — **예외 처리 전략**

> REST API Layer에서 Domain Exception을 HTTP 응답으로 변환하는 전체 전략 가이드입니다.
>
> **RFC 7807 Problem Details** 표준을 준수하며, **ErrorMapper Pattern**을 통해 확장 가능한 에러 핸들링을 제공합니다.

---

## 1) 핵심 원칙

* **RFC 7807 준수**: Problem Details 표준 준수 (type, title, status, detail, instance)
* **OCP 준수**: ErrorMapper 패턴으로 GlobalExceptionHandler 수정 없이 확장 가능
* **I18N 지원**: MessageSource 기반 다국어 에러 메시지
* **로깅 전략**: 5xx/404/4xx 레벨 차별화 (ERROR/DEBUG/WARN)
* **레이어 분리**: Domain Exception → Adapter-In Layer에서 HTTP 변환
* **DDD 통합**: Domain Layer exception-guide.md 전략과 완벽 연계

### 금지사항

* **Domain Exception 직접 노출**: REST API에서 Domain Exception을 그대로 반환 금지
* **Generic Exception 처리**: `Exception` catch로 모든 예외 처리 금지
* **ErrorMapper 직접 호출**: Controller에서 ErrorMapper 직접 호출 금지 (GlobalExceptionHandler가 담당)
* **비즈니스 로직**: ErrorMapper에 비즈니스 로직 포함 금지 (단순 변환만)

---

## 2) 전체 에러 처리 흐름

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│ 1️⃣ Domain Layer (domain/exception/exception-guide.md)          │
├─────────────────────────────────────────────────────────────────┤
│ Domain Object                                                    │
│   └─ throw DomainException("ORDER_NOT_FOUND", args)            │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2️⃣ Application Layer (UseCase)                                  │
├─────────────────────────────────────────────────────────────────┤
│ UseCase                                                          │
│   └─ 전파 (try-catch 불필요)                                     │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3️⃣ Adapter-In Layer (REST API)                                  │
├─────────────────────────────────────────────────────────────────┤
│ Controller                                                       │
│   └─ UseCase 호출 → DomainException 발생                        │
│                                                                  │
│ GlobalExceptionHandler (@RestControllerAdvice)                  │
│   └─ @ExceptionHandler(DomainException.class)                  │
│        ↓                                                         │
│   ErrorMapperRegistry                                           │
│        ├─ orderErrorMapper.supports("ORDER_*") ✅              │
│        ├─ productErrorMapper.supports("PRODUCT_*") ❌           │
│        └─ Select: orderErrorMapper                              │
│                ↓                                                 │
│   ErrorMapper.map()                                             │
│        ├─ HttpStatus 매핑 (404, 409, 400 등)                    │
│        ├─ MessageSource i18n (ko/en)                            │
│        ├─ RFC 7807 type URI                                     │
│        └─ MappedError 생성                                       │
│                ↓                                                 │
│   ProblemDetail (RFC 7807)                                      │
│        ├─ type: URI                                             │
│        ├─ title: "Not Found"                                    │
│        ├─ status: 404                                           │
│        ├─ detail: "주문을 찾을 수 없습니다"                       │
│        ├─ instance: "/api/orders/123"                           │
│        └─ properties: {code, args}                              │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4️⃣ HTTP Response (RFC 7807 JSON)                                │
├─────────────────────────────────────────────────────────────────┤
│ {                                                                │
│   "type": "https://api.example.com/problems/order-not-found",  │
│   "title": "Not Found",                                         │
│   "status": 404,                                                │
│   "detail": "주문을 찾을 수 없습니다",                            │
│   "instance": "/api/orders/123",                                │
│   "code": "ORDER_NOT_FOUND",                                    │
│   "args": []                                                    │
│ }                                                                │
└─────────────────────────────────────────────────────────────────┘
```

### 흐름 단계별 설명

#### 1️⃣ Domain Exception 발생 (Domain Layer)

```java
// Order.java (Domain Aggregate)
public void cancel() {
    if (this.status != OrderStatus.PLACED) {
        throw new DomainException("ORDER_INVALID_STATE",
            "취소 가능한 상태가 아닙니다",
            Map.of("currentStatus", status));
    }
    this.status = OrderStatus.CANCELLED;
}
```

**참고**: Domain Exception 전략은 [exception-guide.md](../../../02-domain-layer/exception/exception-guide.md) 참조

#### 2️⃣ UseCase 전파 (Application Layer)

```java
// CancelOrderUseCase.java
@Transactional
public void cancel(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new DomainException("ORDER_NOT_FOUND"));

    order.cancel();  // ✅ DomainException 발생 시 자동 전파 (try-catch 불필요)
}
```

#### 3️⃣ HTTP 변환 (Adapter-In Layer)

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex,
                                                      HttpServletRequest req,
                                                      Locale locale) {
        // A. ErrorMapperRegistry가 적절한 Mapper 선택
        var mapped = errorMapperRegistry.map(ex, locale)
            .orElseGet(() -> errorMapperRegistry.defaultMapping(ex));

        // B. RFC 7807 ProblemDetail 생성
        var res = build(mapped.status(), mapped.title(), mapped.detail(), req);
        var pd  = res.getBody();

        // C. Extension members 추가
        pd.setType(mapped.type());
        pd.setProperty("code", ex.code());
        if (!ex.args().isEmpty()) pd.setProperty("args", ex.args());

        // D. 로깅 전략
        if (mapped.status().is5xxServerError()) {
            log.error("DomainException (Server Error): code={}, status={}",
                ex.code(), mapped.status().value(), ex);
        } else if (mapped.status() == HttpStatus.NOT_FOUND) {
            log.debug("DomainException (Not Found): code={}, status={}",
                ex.code(), mapped.status().value());
        } else {
            log.warn("DomainException (Client Error): code={}, status={}",
                ex.code(), mapped.status().value());
        }

        return ResponseEntity.status(mapped.status()).body(pd);
    }
}
```

#### 4️⃣ RFC 7807 JSON 응답

```json
{
  "type": "https://api.example.com/problems/order-invalid-state",
  "title": "Conflict",
  "status": 409,
  "detail": "취소 가능한 상태가 아닙니다",
  "instance": "/api/orders/123/cancel",
  "code": "ORDER_INVALID_STATE",
  "args": {
    "currentStatus": "CONFIRMED"
  }
}
```

---

## 3) ErrorMapper Pattern (OCP 준수)

### 패턴 개요

**Strategy Pattern + Registry Pattern** 조합으로 OCP (Open-Closed Principle) 준수:
- **Open for Extension**: 새로운 ErrorMapper 추가 가능 (기존 코드 수정 불필요)
- **Closed for Modification**: GlobalExceptionHandler 수정 불필요

### 핵심 컴포넌트

| 컴포넌트 | 역할 | 위치 |
|---------|------|------|
| **ErrorMapper** (Interface) | ErrorCode → HTTP 변환 계약 | `common/mapper/ErrorMapper.java` |
| **ErrorMapperRegistry** | ErrorMapper 관리 및 선택 | `common/error/ErrorMapperRegistry.java` |
| **OrderErrorMapper** (예시) | Order Domain 전용 매핑 | `order/error/OrderErrorMapper.java` |
| **GlobalExceptionHandler** | 예외 → RFC 7807 변환 | `common/controller/GlobalExceptionHandler.java` |
| **MessageSource** | i18n 메시지 관리 | `messages_ko.properties` |

### ErrorMapper Interface

```java
public interface ErrorMapper {

    /**
     * 이 Mapper가 처리 가능한 ErrorCode인지 판단
     *
     * @param code ErrorCode (예: "ORDER_NOT_FOUND")
     * @return 처리 가능 여부
     */
    boolean supports(String code);

    /**
     * DomainException을 HTTP 에러로 변환
     *
     * @param ex DomainException
     * @param locale 다국어 Locale
     * @return MappedError (HttpStatus, title, detail, type)
     */
    MappedError map(DomainException ex, Locale locale);

    /**
     * 변환 결과
     */
    record MappedError(
        HttpStatus status,  // HTTP 상태 코드
        String title,       // RFC 7807 title
        String detail,      // RFC 7807 detail
        URI type           // RFC 7807 type URI
    ) {}
}
```

### ErrorMapperRegistry

```java
@Component
public class ErrorMapperRegistry {

    private final List<ErrorMapper> mappers;

    /**
     * DomainException을 MappedError로 변환
     *
     * @param ex DomainException
     * @param locale 다국어 Locale
     * @return MappedError (Optional)
     */
    public Optional<ErrorMapper.MappedError> map(DomainException ex, Locale locale) {
        return mappers.stream()
            .filter(m -> m.supports(ex.code()))  // ✅ supports() 기반 선택
            .findFirst()
            .map(m -> m.map(ex, locale));
    }

    /**
     * 기본 매핑 (Fallback)
     */
    public ErrorMapper.MappedError defaultMapping(DomainException ex) {
        return new ErrorMapper.MappedError(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage() != null ? ex.getMessage() : "Invalid request",
            URI.create("about:blank")
        );
    }
}
```

### ErrorMapper 구현 예시

```java
@Component
public class OrderErrorMapper implements ErrorMapper {

    private static final String PREFIX = "ORDER_";
    private static final String TYPE_BASE = "https://api.example.com/problems/";

    private final MessageSource messageSource;

    @Override
    public boolean supports(String code) {
        return code != null && code.startsWith(PREFIX);  // ✅ ORDER_* 처리
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        String code = ex.code();

        // 1. HttpStatus 매핑
        HttpStatus status = switch (code) {
            case "ORDER_NOT_FOUND"     -> HttpStatus.NOT_FOUND;      // 404
            case "ORDER_DUPLICATE_KEY" -> HttpStatus.CONFLICT;       // 409
            case "ORDER_INVALID_STATE" -> HttpStatus.CONFLICT;       // 409
            default                    -> HttpStatus.BAD_REQUEST;    // 400
        };

        // 2. MessageSource i18n
        String titleKey  = "problem.title."  + code.toLowerCase();
        String detailKey = "problem.detail." + code.toLowerCase();

        String title  = messageSource.getMessage(titleKey,
            new Object[0], status.getReasonPhrase(), locale);
        String detail = messageSource.getMessage(detailKey,
            new Object[0], ex.getMessage(), locale);

        // 3. RFC 7807 type URI
        URI type = URI.create(TYPE_BASE + code.toLowerCase().replace('_', '-'));

        return new MappedError(status, title, detail, type);
    }
}
```

**참고**: ErrorMapper 구현 가이드는 [error-mapper-implementation-guide.md](./error-mapper-implementation-guide.md) 참조 (작성 예정)

---

## 4) RFC 7807 Problem Details 표준

### RFC 7807 필드

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| **type** | URI | ✅ | 문제 타입 URI | `https://api.example.com/problems/order-not-found` |
| **title** | String | ✅ | 짧은 요약 | `"Not Found"` |
| **status** | Integer | ✅ | HTTP 상태 코드 | `404` |
| **detail** | String | ✅ | 구체적인 설명 | `"주문을 찾을 수 없습니다"` |
| **instance** | URI | ✅ | 발생한 요청 URI | `"/api/orders/123"` |
| **code** | String | ❌ | Extension (ErrorCode) | `"ORDER_NOT_FOUND"` |
| **args** | Object | ❌ | Extension (추가 정보) | `{"orderId": 123}` |

### Spring ProblemDetail 사용

```java
// GlobalExceptionHandler.java
private ResponseEntity<ProblemDetail> build(HttpStatus status,
                                             String title,
                                             String detail,
                                             HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    pd.setInstance(URI.create(req.getRequestURI()));  // ✅ instance 자동 설정
    return ResponseEntity.status(status).body(pd);
}
```

**참고**: RFC 7807 Response Format 가이드는 [rfc7807-response-format-guide.md](./rfc7807-response-format-guide.md) 참조 (작성 예정)

---

## 5) MessageSource I18N 전략

### messages.properties 구조

```properties
# messages_ko.properties (한국어)
problem.title.order_not_found=찾을 수 없음
problem.detail.order_not_found=주문을 찾을 수 없습니다

problem.title.order_invalid_state=상태 오류
problem.detail.order_invalid_state=취소 가능한 상태가 아닙니다

# messages_en.properties (영어)
problem.title.order_not_found=Not Found
problem.detail.order_not_found=Order not found

problem.title.order_invalid_state=Invalid State
problem.detail.order_invalid_state=Cannot cancel order in current state
```

### 네이밍 규칙

| 패턴 | 용도 | 예시 |
|------|------|------|
| `problem.title.{error_code}` | RFC 7807 title | `problem.title.order_not_found` |
| `problem.detail.{error_code}` | RFC 7807 detail | `problem.detail.order_not_found` |

**규칙**:
- ErrorCode는 **소문자 + 언더스코어** (`ORDER_NOT_FOUND` → `order_not_found`)
- Prefix (`problem.title.`, `problem.detail.`) 고정

### ErrorMapper에서 사용

```java
String title = messageSource.getMessage(
    "problem.title." + code.toLowerCase(),  // Key
    new Object[0],                          // Args
    status.getReasonPhrase(),               // Fallback
    locale                                  // Locale (ko/en)
);
```

---

## 6) 로깅 전략

### 로그 레벨 차별화

| HTTP 상태 | 로그 레벨 | StackTrace | 이유 |
|-----------|----------|------------|------|
| **5xx** | `ERROR` | ✅ 포함 | 서버 에러 (즉시 조치 필요) |
| **404** | `DEBUG` | ❌ 제외 | Not Found (정상 케이스) |
| **4xx** | `WARN` | ❌ 제외 | 클라이언트 에러 (모니터링 필요) |

### 구현

```java
// GlobalExceptionHandler.java
if (mapped.status().is5xxServerError()) {
    log.error("DomainException (Server Error): code={}, status={}, detail={}, args={}",
        ex.code(), mapped.status().value(), mapped.detail(), ex.args(), ex);  // ✅ StackTrace
} else if (mapped.status() == HttpStatus.NOT_FOUND) {
    log.debug("DomainException (Not Found): code={}, status={}, detail={}, args={}",
        ex.code(), mapped.status().value(), mapped.detail(), ex.args());  // ❌ StackTrace 제외
} else {
    log.warn("DomainException (Client Error): code={}, status={}, detail={}, args={}",
        ex.code(), mapped.status().value(), mapped.detail(), ex.args());  // ❌ StackTrace 제외
}
```

### 로그 메시지 포맷

```
DomainException (Server Error): code=ORDER_PROCESSING_FAILED, status=500, detail=주문 처리 중 오류, args={orderId=123}
DomainException (Not Found): code=ORDER_NOT_FOUND, status=404, detail=주문을 찾을 수 없습니다, args={}
DomainException (Client Error): code=ORDER_INVALID_STATE, status=409, detail=취소 불가능한 상태, args={currentStatus=CONFIRMED}
```

---

## 7) 컴포넌트 책임 분리

### 책임 매트릭스

| 컴포넌트 | 책임 | 금지사항 |
|---------|------|----------|
| **Domain Layer** | 비즈니스 규칙 검증, DomainException throw | HTTP 상태 코드, REST API 관련 로직 |
| **Application Layer** | UseCase 조율, Domain Exception 전파 | try-catch로 Domain Exception 처리 금지 |
| **ErrorMapper** | ErrorCode → HttpStatus 매핑, i18n 처리 | 비즈니스 로직, Domain 객체 직접 접근 |
| **ErrorMapperRegistry** | ErrorMapper 선택 및 관리 | 변환 로직 (ErrorMapper에 위임) |
| **GlobalExceptionHandler** | 예외 → RFC 7807 변환, 로깅 | 비즈니스 로직, 복잡한 변환 로직 |
| **MessageSource** | 다국어 메시지 관리 | 변환 로직, HTTP 관련 처리 |

### 레이어 의존성 규칙

```
┌────────────────────────────────────────────────┐
│ Domain Layer                                   │
│ - DomainException만 throw                     │
│ - HTTP, REST API 개념 없음                     │
└────────────────────────────────────────────────┘
                ↑ (의존성 역전)
┌────────────────────────────────────────────────┐
│ Application Layer                              │
│ - Domain Exception 전파 (try-catch 불필요)     │
│ - HTTP 변환 책임 없음                           │
└────────────────────────────────────────────────┘
                ↑ (의존성)
┌────────────────────────────────────────────────┐
│ Adapter-In Layer (REST API)                    │
│ - ErrorMapper: ErrorCode → HttpStatus 변환     │
│ - GlobalExceptionHandler: RFC 7807 변환        │
│ - HTTP 응답 생성                                │
└────────────────────────────────────────────────┘
```

**핵심**: Domain Layer는 HTTP를 알지 못하며, REST API Layer가 Domain Exception을 HTTP로 변환

---

## 8) 확장 시나리오

### 새로운 Bounded Context 추가

```java
// ProductErrorMapper.java (새로운 ErrorMapper)
@Component
public class ProductErrorMapper implements ErrorMapper {

    private static final String PREFIX = "PRODUCT_";

    @Override
    public boolean supports(String code) {
        return code != null && code.startsWith(PREFIX);  // ✅ PRODUCT_* 처리
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        // HttpStatus 매핑 + MessageSource i18n
        // ...
    }
}
```

**효과**:
- ✅ GlobalExceptionHandler 수정 불필요 (OCP)
- ✅ 기존 OrderErrorMapper 영향 없음
- ✅ ErrorMapperRegistry가 자동 감지 (`@Component` + `List<ErrorMapper>`)

### 새로운 HTTP 상태 코드 추가

```java
// OrderErrorMapper.java
@Override
public MappedError map(DomainException ex, Locale locale) {
    HttpStatus status = switch (ex.code()) {
        case "ORDER_NOT_FOUND"     -> HttpStatus.NOT_FOUND;
        case "ORDER_DUPLICATE_KEY" -> HttpStatus.CONFLICT;
        case "ORDER_INVALID_STATE" -> HttpStatus.CONFLICT;
        case "ORDER_RATE_LIMITED"  -> HttpStatus.TOO_MANY_REQUESTS;  // ✅ 429 추가
        default                    -> HttpStatus.BAD_REQUEST;
    };
    // ...
}
```

**효과**:
- ✅ GlobalExceptionHandler 수정 불필요
- ✅ 다른 ErrorMapper 영향 없음

---

## 9) 테스트 전략

### ErrorMapper 단위 테스트

```java
@Test
@DisplayName("ORDER_NOT_FOUND는 404로 매핑된다")
void orderNotFound_ShouldMap404() {
    // Given
    DomainException ex = new DomainException("ORDER_NOT_FOUND", "Not found", Map.of());

    // When
    ErrorMapper.MappedError mapped = orderErrorMapper.map(ex, Locale.KOREAN);

    // Then
    assertThat(mapped.status()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(mapped.title()).isEqualTo("찾을 수 없음");
    assertThat(mapped.detail()).isEqualTo("주문을 찾을 수 없습니다");
    assertThat(mapped.type().toString()).contains("order-not-found");
}
```

### GlobalExceptionHandler 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Test
    @DisplayName("Domain Exception은 RFC 7807 형식으로 반환된다")
    void domainException_ShouldReturnProblemDetail() throws Exception {
        mockMvc.perform(get("/api/orders/999"))  // 존재하지 않는 주문
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").value("찾을 수 없음"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("주문을 찾을 수 없습니다"))
            .andExpect(jsonPath("$.instance").value("/api/orders/999"))
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }
}
```

---

## 10) 체크리스트

- [ ] Domain Layer exception-guide.md 숙지
- [ ] ErrorMapper 인터페이스 이해
- [ ] ErrorMapperRegistry 동작 원리 이해
- [ ] RFC 7807 Problem Details 표준 숙지
- [ ] MessageSource i18n 설정 완료
- [ ] GlobalExceptionHandler 로깅 전략 이해
- [ ] ErrorMapper 구현 (Bounded Context별)
- [ ] messages_ko.properties / messages_en.properties 작성
- [ ] ErrorMapper 단위 테스트 작성
- [ ] GlobalExceptionHandler 통합 테스트 작성

---

## 11) 추가 가이드 링크

- **Domain Exception 전략**: [exception-guide.md](../../../02-domain-layer/exception/exception-guide.md) ⭐ 필수
- **ErrorMapper 구현 가이드**: [error-mapper-implementation-guide.md](./error-mapper-implementation-guide.md) (작성 예정)
- **GlobalExceptionHandler 가이드**: [global-exception-handler-guide.md](./global-exception-handler-guide.md) (작성 예정)
- **RFC 7807 Response Format**: [rfc7807-response-format-guide.md](./rfc7807-response-format-guide.md) (작성 예정)
- **Error Package Structure**: [error-package-structure-guide.md](./error-package-structure-guide.md) (작성 예정)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
