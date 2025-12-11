# Error Handling Guide — **RFC 7807 기반 예외 처리**

> 이 문서는 `adapter-in/rest-api` 레이어의 **에러 처리 가이드**입니다.
>
> GlobalExceptionHandler, ErrorMapper 패턴, RFC 7807 표준을 다룹니다.

---

## 1) 핵심 원칙

### 필수 규칙

| 원칙 | 설명 |
|------|------|
| **RFC 7807 준수** | 모든 에러 응답은 `ProblemDetail` 형식 사용 |
| **Content-Type 명시** | `application/problem+json` Content-Type 필수 |
| **에러 코드 표준화** | 모든 예외에 표준 에러 코드 부여 (`x-error-code` 헤더) |
| **Trace ID 통합** | MDC `traceId`, `spanId` 확장 필드로 추가 |
| **OCP 준수** | ErrorMapperRegistry 패턴으로 확장성 보장 |
| **로깅 레벨 전략** | 5xx → ERROR, 404 → DEBUG, 기타 4xx → WARN |

### 금지 사항

| 금지 | 이유 |
|------|------|
| **Domain 예외 직접 노출** | API 계층에서 HTTP 응답으로 변환 필수 |
| **스택트레이스 노출** | 보안 위험, 에러 메시지만 반환 |
| **일반적인 500 에러만 반환** | 구체적인 에러 코드와 메시지 필수 |
| **Content-Type 누락** | RFC 7807 위반, 클라이언트 파싱 어려움 |

---

## 2) 패키지 구조

```
adapter-in/rest-api/
├── common/
│   ├── controller/
│   │   └── GlobalExceptionHandler.java   # 전역 예외 처리
│   ├── dto/
│   │   ├── ApiResponse.java              # 성공 응답 래퍼
│   │   └── ErrorInfo.java                # (선택) 에러 정보 DTO
│   ├── error/
│   │   └── ErrorMapperRegistry.java      # ErrorMapper 레지스트리
│   └── mapper/
│       └── ErrorMapper.java              # ErrorMapper 인터페이스
│
└── [boundedContext]/
    └── error/
        └── OrderApiErrorMapper.java      # BC별 ErrorMapper 구현체
```

---

## 3) GlobalExceptionHandler 템플릿

### 3.1) 기본 구조

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorMapperRegistry errorMapperRegistry;

    public GlobalExceptionHandler(ErrorMapperRegistry errorMapperRegistry) {
        this.errorMapperRegistry = errorMapperRegistry;
    }

    // ======= Common builder (RFC 7807 완전 준수) =======
    private ResponseEntity<ProblemDetail> build(
        HttpStatus status,
        String title,
        String detail,
        String errorCode,
        HttpServletRequest req
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title != null ? title : status.getReasonPhrase());

        // 에러 타입 URI
        pd.setType(URI.create("about:blank"));

        // RFC 7807 확장 필드
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("code", errorCode);

        // 요청 경로를 instance로
        if (req != null) {
            String uri = req.getRequestURI();
            if (req.getQueryString() != null && !req.getQueryString().isBlank()) {
                uri = uri + "?" + req.getQueryString();
            }
            pd.setInstance(URI.create(uri));
        }

        // Trace ID 추가 (Micrometer/Logback MDC)
        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        if (traceId != null) pd.setProperty("traceId", traceId);
        if (spanId != null) pd.setProperty("spanId", spanId);

        // RFC 7807: Content-Type + x-error-code 헤더
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header("x-error-code", errorCode)
            .body(pd);
    }
}
```

### 3.2) 표준 에러 코드 상수

```java
// ======= 400 - Validation =======
private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
private static final String BINDING_FAILED = "BINDING_FAILED";
private static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
private static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
private static final String INVALID_FORMAT = "INVALID_FORMAT";
private static final String TYPE_MISMATCH = "TYPE_MISMATCH";
private static final String MISSING_PARAMETER = "MISSING_PARAMETER";

// ======= 404, 405, 409, 500 =======
private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
private static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
private static final String STATE_CONFLICT = "STATE_CONFLICT";
private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
```

### 3.3) 예외 핸들러 예시

```java
// ======= 400 - Validation (@RequestBody) =======
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleValidationException(
    MethodArgumentNotValidException ex,
    HttpServletRequest req
) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
        errors.put(fe.getField(), fe.getDefaultMessage());
    }

    var res = build(HttpStatus.BAD_REQUEST, "Bad Request",
        "Validation failed for request", VALIDATION_FAILED, req);
    res.getBody().setProperty("errors", errors);

    log.warn("MethodArgumentNotValid: code={}, errors={}", VALIDATION_FAILED, errors);
    return res;
}

// ======= Domain Exception =======
@ExceptionHandler(DomainException.class)
public ResponseEntity<ProblemDetail> handleDomain(
    DomainException ex,
    HttpServletRequest req,
    Locale locale
) {
    var mapped = errorMapperRegistry.map(ex, locale)
        .orElseGet(() -> errorMapperRegistry.defaultMapping(ex));

    var res = build(mapped.status(), mapped.title(), mapped.detail(), ex.code(), req);
    var pd = res.getBody();

    pd.setType(mapped.type());
    if (!ex.args().isEmpty()) pd.setProperty("args", ex.args());

    // 로깅 레벨 전략
    if (mapped.status().is5xxServerError()) {
        log.error("DomainException (Server Error): code={}", ex.code(), ex);
    } else if (mapped.status() == HttpStatus.NOT_FOUND) {
        log.debug("DomainException (Not Found): code={}", ex.code());
    } else {
        log.warn("DomainException (Client Error): code={}", ex.code());
    }

    return ResponseEntity
        .status(mapped.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .header("x-error-code", ex.code())
        .body(pd);
}
```

---

## 4) ErrorMapper 패턴

### 4.1) ErrorMapper 인터페이스

```java
/**
 * Domain Exception → HTTP 응답 매핑 인터페이스
 *
 * <p>OCP 원칙을 준수하여 새로운 도메인 예외 추가 시
 * GlobalExceptionHandler 수정 없이 확장 가능</p>
 *
 * <p>매칭 전략:</p>
 * <ul>
 *   <li>예외 타입: {@code ex instanceof OrderException}</li>
 *   <li>에러 코드: {@code ex.code().startsWith("ORDER_")}</li>
 *   <li>복합 조건: 타입 + 코드 조합</li>
 * </ul>
 */
public interface ErrorMapper {

    /**
     * 이 매퍼가 처리할 수 있는 예외인지 확인
     *
     * @param ex 도메인 예외
     * @return 처리 가능하면 true
     */
    boolean supports(DomainException ex);

    /**
     * 예외를 HTTP 응답 정보로 변환
     *
     * @param ex 도메인 예외
     * @param locale 다국어 지원을 위한 로케일
     * @return RFC 7807 호환 에러 정보
     */
    MappedError map(DomainException ex, Locale locale);

    /**
     * RFC 7807 호환 에러 응답 DTO
     *
     * @param status HTTP 상태 코드
     * @param title 에러 유형 요약
     * @param detail 에러 상세 설명
     * @param type 에러 유형 URI (문서 링크)
     */
    record MappedError(HttpStatus status, String title, String detail, URI type) {}
}
```

### 4.2) BC별 ErrorMapper 구현

**방법 1: 예외 타입으로 매칭 (권장)**

```java
@Component
public class OrderApiErrorMapper implements ErrorMapper {

    @Override
    public boolean supports(DomainException ex) {
        // 예외 타입으로 매칭 (타입 안전성 보장)
        return ex instanceof OrderException;
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        return switch (ex.code()) {
            case "ORDER_NOT_FOUND" -> new MappedError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "주문을 찾을 수 없습니다",
                URI.create("/errors/order/not-found")
            );
            case "ORDER_INVALID_STATUS" -> new MappedError(
                HttpStatus.CONFLICT,
                "Conflict",
                "주문 상태가 올바르지 않습니다",
                URI.create("/errors/order/invalid-status")
            );
            default -> new MappedError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                URI.create("/errors/order")
            );
        };
    }
}
```

**방법 2: 에러 코드 prefix로 매칭**

```java
@Component
public class MemberApiErrorMapper implements ErrorMapper {

    @Override
    public boolean supports(DomainException ex) {
        // 에러 코드 prefix로 매칭 (유연성 제공)
        return ex.code().startsWith("MEMBER_");
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        return switch (ex.code()) {
            case "MEMBER_NOT_FOUND" -> new MappedError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "회원을 찾을 수 없습니다",
                URI.create("/errors/member/not-found")
            );
            case "MEMBER_DUPLICATE_EMAIL" -> new MappedError(
                HttpStatus.CONFLICT,
                "Conflict",
                "이미 사용 중인 이메일입니다",
                URI.create("/errors/member/duplicate-email")
            );
            default -> new MappedError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                URI.create("/errors/member")
            );
        };
    }
}
```

### 4.3) ErrorMapperRegistry

```java
@Component
public class ErrorMapperRegistry {

    private final List<ErrorMapper> mappers;

    public ErrorMapperRegistry(List<ErrorMapper> mappers) {
        this.mappers = mappers;
    }

    /**
     * 예외에 맞는 ErrorMapper 찾아 매핑
     */
    public Optional<MappedError> map(DomainException ex, Locale locale) {
        return mappers.stream()
            .filter(mapper -> mapper.supports(ex))
            .findFirst()
            .map(mapper -> mapper.map(ex, locale));
    }

    /**
     * 기본 매핑 (일치하는 ErrorMapper 없을 때)
     */
    public MappedError defaultMapping(DomainException ex) {
        return new MappedError(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage() != null ? ex.getMessage() : "Invalid request",
            URI.create("about:blank")
        );
    }
}
```

---

## 5) 응답 형식 (RFC 7807)

### 5.1) Validation 에러 (400)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for request",
  "instance": "/api/orders",
  "code": "VALIDATION_FAILED",
  "timestamp": "2025-12-04T10:30:00.000Z",
  "traceId": "abc123def456",
  "errors": {
    "email": "올바른 이메일 형식이 아닙니다",
    "name": "이름은 필수입니다"
  }
}
```

**HTTP 헤더**:
```http
Content-Type: application/problem+json
x-error-code: VALIDATION_FAILED
```

### 5.2) 도메인 에러 (404)

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "ID가 12345인 주문을 찾을 수 없습니다",
  "instance": "/api/orders/12345",
  "code": "ORDER_NOT_FOUND",
  "timestamp": "2025-12-04T10:30:00.000Z",
  "traceId": "abc123def456",
  "args": {
    "orderId": 12345
  }
}
```

### 5.3) 서버 에러 (500)

```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
  "instance": "/api/orders",
  "code": "INTERNAL_ERROR",
  "timestamp": "2025-12-04T10:30:00.000Z",
  "traceId": "abc123def456"
}
```

---

## 6) 로깅 전략

### 6.1) HTTP 상태별 로깅 레벨

| HTTP 상태 | 로깅 레벨 | 스택트레이스 | 이유 |
|----------|----------|------------|------|
| **5xx** | ERROR | ✅ 포함 | 서버 문제, 즉시 대응 필요 |
| **404** | DEBUG | ❌ 제외 | 정상 흐름, 로그 노이즈 방지 |
| **기타 4xx** | WARN | ❌ 제외 | 클라이언트 오류, 모니터링 필요 |

### 6.2) 로깅 예시

```java
// 5xx: ERROR 레벨 + 스택트레이스
log.error("DomainException (Server Error): code={}, status={}, detail={}, args={}",
    ex.code(), mapped.status().value(), mapped.detail(), ex.args(), ex);

// 404: DEBUG 레벨 (로그 노이즈 방지)
log.debug("DomainException (Not Found): code={}, status={}, detail={}, args={}",
    ex.code(), mapped.status().value(), mapped.detail(), ex.args());

// 기타 4xx: WARN 레벨
log.warn("DomainException (Client Error): code={}, status={}, detail={}, args={}",
    ex.code(), mapped.status().value(), mapped.detail(), ex.args());
```

---

## 7) Do / Don't

### ✅ Do

```java
// ✅ RFC 7807 Content-Type 사용
return ResponseEntity
    .status(status)
    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
    .header("x-error-code", errorCode)
    .body(pd);

// ✅ Trace ID 포함
String traceId = MDC.get("traceId");
if (traceId != null) pd.setProperty("traceId", traceId);

// ✅ 표준 에러 코드 사용
private static final String VALIDATION_FAILED = "VALIDATION_FAILED";

// ✅ ErrorMapper로 OCP 준수
@Component
public class OrderApiErrorMapper implements ErrorMapper {
    @Override
    public boolean supports(DomainException ex) {
        // 방법 1: 예외 타입 매칭 (권장)
        return ex instanceof OrderException;
        // 방법 2: 에러 코드 prefix 매칭
        // return ex.code().startsWith("ORDER_");
    }
}
```

### ❌ Don't

```java
// ❌ 일반 JSON으로 에러 반환
return ResponseEntity.badRequest().body(error);

// ❌ 스택트레이스 노출
pd.setProperty("stackTrace", ex.getStackTrace());

// ❌ GlobalExceptionHandler에서 직접 분기
if (ex.code().equals("ORDER_NOT_FOUND")) { ... }
else if (ex.code().equals("USER_NOT_FOUND")) { ... }

// ❌ 하드코딩된 에러 메시지
return build(HttpStatus.BAD_REQUEST, "Bad Request", "error", "ERROR", req);
```

---

## 8) 관련 문서

| 문서 | 설명 |
|------|------|
| [Error Test Guide](./error-test-guide.md) | 테스트 가이드 |
| [Error ArchUnit](./error-archunit.md) | 아키텍처 검증 |

---

## 9) 체크리스트

### GlobalExceptionHandler

- [ ] `@RestControllerAdvice` 어노테이션 사용
- [ ] `MediaType.APPLICATION_PROBLEM_JSON` Content-Type 설정
- [ ] `x-error-code` 응답 헤더 추가
- [ ] 모든 핸들러에 표준 에러 코드 적용
- [ ] MDC `traceId`, `spanId` 확장 필드 추가
- [ ] 로깅 레벨 전략 적용 (5xx → ERROR, 404 → DEBUG, 4xx → WARN)

### ErrorMapper

- [ ] BC별 ErrorMapper 구현 (`@Component`)
- [ ] `supports(DomainException ex)` 메서드로 매칭 (타입 or 에러 코드)
- [ ] `map()` 메서드로 `MappedError` 반환
- [ ] ErrorMapperRegistry에 자동 등록 확인

### 응답 형식

- [ ] RFC 7807 필드 포함 (type, title, status, detail, instance)
- [ ] 확장 필드 포함 (code, timestamp, traceId)
- [ ] Validation 에러 시 `errors` 필드 추가

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
