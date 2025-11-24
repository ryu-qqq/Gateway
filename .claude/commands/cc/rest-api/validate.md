# REST API Layer Coding Convention Validation

**목적**: TDD로 작성된 REST API Layer 코드가 프로젝트 코딩 컨벤션을 준수하는지 검증하고, 위반 사항에 대한 리팩토링 PRD를 생성합니다.

---

## 🎯 검증 범위

### 1. REST API Layer 코딩 컨벤션 문서

다음 문서들의 규칙을 기준으로 검증합니다:

```
docs/coding_convention/01-adapter-in-layer/rest-api/
├── controller/
│   ├── controller-guide.md
│   ├── controller-test-guide.md
│   └── controller-test-restdocs-guide.md
├── dto/
│   ├── command/  (Request DTO)
│   ├── query/    (Query Parameter DTO)
│   └── response/ (Response DTO)
├── error/
│   ├── error-handling-strategy.md
│   └── error-mapper-implementation-guide.md
├── mapper/  (Request DTO → Application Command)
└── rest-api-guide.md
```

### 2. 핵심 검증 항목

#### Zero-Tolerance 규칙 (절대 위반 금지)
- **RESTful 설계**: HTTP 메서드 정확한 의미 준수 (POST=생성, PUT=전체수정, PATCH=부분수정)
- **MockMvc 금지**: TestRestTemplate 사용 필수 (E2E 테스트)
- **Validation 필수**: `@Valid` 또는 `@Validated` 사용
- **ErrorMapper 패턴**: 도메인 예외 → HTTP 응답 변환은 ErrorMapper 사용
- **Status Code 정확성**: 201(생성), 204(삭제), 400(검증실패), 404(없음), 500(서버오류)

#### 구조 규칙
- **Controller 네이밍**: `*RestController`
- **DTO Record 패턴**: Command/Query/Response는 Record 사용
- **Mapper 패턴**: Request DTO → Application Command 변환
- **API 문서화**: Spring REST Docs 사용 (Swagger 금지)
- **Endpoint Properties**: 엔드포인트 URL 외부 설정 파일 관리

#### 테스트 규칙
- **E2E 테스트**: TestRestTemplate로 실제 HTTP 요청/응답 검증
- **REST Docs**: 모든 API는 문서화 (snippets 생성)
- **Status Code 검증**: 응답 상태 코드 명시적 검증

---

## 🔍 검증 프로세스

### 1단계: RESTful 설계 검증 (Zero-Tolerance)

```markdown
**검증 대상**: `adapter-in-rest/src/main/java/**/controller/*RestController.java`

**검증 항목**:
1. HTTP 메서드 정확성
   - POST: 리소스 생성 → 201 Created
   - GET: 리소스 조회 → 200 OK
   - PUT: 전체 수정 → 200 OK
   - PATCH: 부분 수정 → 200 OK
   - DELETE: 삭제 → 204 No Content

2. URL 설계
   - ✅ `/api/v1/orders` (명사 복수형)
   - ❌ `/api/v1/createOrder` (동사 사용)
   - ✅ `/api/v1/orders/{orderId}` (Path Variable)
   - ❌ `/api/v1/orders?id=123` (ID는 Path로)

3. 상태 코드 정확성
   - ❌ POST 생성 → 200 OK (201이어야 함)
   - ❌ 검증 실패 → 500 (400이어야 함)
   - ❌ 리소스 없음 → 200 (404이어야 함)
```

**Serena MCP 활용**:
```python
# 1. POST 메서드에서 잘못된 상태 코드 검색
search_for_pattern(
    substring_pattern="@PostMapping.*\\{[\\s\\S]*?ResponseEntity.*200",
    relative_path="adapter-in-rest/src/main/java/**/controller",
    multiline=True
)

# 2. URL에 동사 사용 검색 (안티패턴)
search_for_pattern(
    substring_pattern="@(Get|Post|Put|Patch|Delete)Mapping.*/(create|update|delete|get|list)",
    relative_path="adapter-in-rest/src/main/java/**/controller"
)

# 3. Query Parameter로 ID 전달 (안티패턴)
search_for_pattern(
    substring_pattern="@RequestParam.*\\s+(id|orderId|userId)",
    relative_path="adapter-in-rest/src/main/java/**/controller"
)
```

### 2단계: MockMvc 사용 검증 (Zero-Tolerance)

```markdown
**검증 대상**: `adapter-in-rest/src/test/java/**/*RestControllerTest.java`

**검증 항목**:
1. MockMvc 사용 금지
   - ❌ `@WebMvcTest`, `@AutoConfigureMockMvc`
   - ❌ `mockMvc.perform()`
   - ✅ `@SpringBootTest(webEnvironment = RANDOM_PORT)`
   - ✅ `TestRestTemplate`

2. E2E 테스트 패턴
   - ✅ 실제 HTTP 요청/응답
   - ✅ 전체 스프링 컨텍스트 로드
   - ❌ Mock 기반 단위 테스트
```

**Serena MCP 활용**:
```python
# 1. MockMvc 사용 검색
search_for_pattern(
    substring_pattern="@(WebMvcTest|AutoConfigureMockMvc)|import.*MockMvc",
    relative_path="adapter-in-rest/src/test/java"
)

# 2. mockMvc.perform() 패턴 검색
search_for_pattern(
    substring_pattern="mockMvc\\.perform\\(",
    relative_path="adapter-in-rest/src/test/java"
)

# 3. TestRestTemplate 사용 여부
search_for_pattern(
    substring_pattern="TestRestTemplate",
    relative_path="adapter-in-rest/src/test/java"
)
```

### 3단계: Validation 검증

```markdown
**검증 대상**: `adapter-in-rest/src/main/java/**/controller/*RestController.java`

**검증 항목**:
1. @Valid 또는 @Validated 사용
   - ✅ `public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest request)`
   - ❌ `public ResponseEntity<?> create(@RequestBody CreateOrderRequest request)` (검증 없음)

2. DTO에 검증 어노테이션
   - ✅ `@NotNull`, `@NotBlank`, `@Min`, `@Max` 등
   - ❌ 검증 어노테이션 없음

3. 검증 실패 시 400 Bad Request
   - ✅ `MethodArgumentNotValidException` → 400
   - ❌ 검증 실패 시 500 반환
```

**Serena MCP 활용**:
```python
# 1. @Valid 없는 @RequestBody 검색 (안티패턴)
search_for_pattern(
    substring_pattern="@RequestBody\\s+(?!@Valid|@Validated)[A-Z]",
    relative_path="adapter-in-rest/src/main/java/**/controller"
)

# 2. DTO에 검증 어노테이션 존재 여부
find_symbol(
    name_path="Request",
    relative_path="adapter-in-rest/src/main/java/**/dto",
    substring_matching=True,
    include_body=True
)

# 3. GlobalExceptionHandler에서 MethodArgumentNotValidException 처리 여부
search_for_pattern(
    substring_pattern="MethodArgumentNotValidException",
    relative_path="adapter-in-rest/src/main/java/**/error"
)
```

### 4단계: ErrorMapper 패턴 검증

```markdown
**검증 대상**: `adapter-in-rest/src/main/java/**/error/*ErrorMapper.java`

**검증 항목**:
1. ErrorMapper 클래스 존재
   - ✅ `OrderErrorMapper.java`
   - ❌ Controller에서 직접 예외 변환

2. 도메인 예외 → HTTP 응답 변환
   - ✅ `OrderNotFoundException` → 404 Not Found
   - ✅ `InvalidOrderException` → 400 Bad Request
   - ❌ 모든 예외 → 500 Internal Server Error

3. GlobalExceptionHandler 사용
   - ✅ `@RestControllerAdvice`
   - ✅ `@ExceptionHandler`
```

**Serena MCP 활용**:
```python
# 1. ErrorMapper 클래스 탐색
find_symbol(
    name_path="ErrorMapper",
    relative_path="adapter-in-rest/src/main/java/**/error",
    substring_matching=True
)

# 2. Controller에서 직접 예외 처리 (안티패턴)
search_for_pattern(
    substring_pattern="try\\s*\\{[\\s\\S]*?catch\\s*\\(",
    relative_path="adapter-in-rest/src/main/java/**/controller",
    multiline=True
)

# 3. GlobalExceptionHandler 존재 여부
search_for_pattern(
    substring_pattern="@RestControllerAdvice",
    relative_path="adapter-in-rest/src/main/java/**/error"
)
```

### 5단계: DTO 패턴 검증

```markdown
**검증 대상**:
- `adapter-in-rest/src/main/java/**/dto/command/*Request.java`
- `adapter-in-rest/src/main/java/**/dto/query/*Query.java`
- `adapter-in-rest/src/main/java/**/dto/response/*Response.java`

**검증 항목**:
1. Record 패턴 사용
   - ✅ `public record CreateOrderRequest(...)`
   - ❌ `public class CreateOrderRequest { ... }` (Class 사용)

2. DTO 네이밍
   - Command: `*Request` (POST, PUT, PATCH)
   - Query: `*Query` (GET with parameters)
   - Response: `*Response` (모든 응답)

3. Validation 어노테이션
   - ✅ `@NotNull`, `@NotBlank`, `@Min`, `@Max`
```

**Serena MCP 활용**:
```python
# 1. Class로 정의된 DTO 검색 (안티패턴)
search_for_pattern(
    substring_pattern="public\\s+class\\s+.*(Request|Query|Response)\\s*\\{",
    relative_path="adapter-in-rest/src/main/java/**/dto"
)

# 2. Record 패턴 사용 여부
search_for_pattern(
    substring_pattern="public\\s+record\\s+",
    relative_path="adapter-in-rest/src/main/java/**/dto"
)
```

### 6단계: Mapper 패턴 검증

```markdown
**검증 대상**: `adapter-in-rest/src/main/java/**/mapper/*RequestMapper.java`

**검증 항목**:
1. RequestMapper 존재
   - ✅ Request DTO → Application Command 변환
   - ❌ Controller에서 직접 변환

2. Mapper 메서드 네이밍
   - ✅ `toCommand()`: Request → Command
   - ✅ `toQuery()`: QueryParams → Query
   - ❌ `convert()`, `map()` (모호함)
```

**Serena MCP 활용**:
```python
# 1. RequestMapper 클래스 탐색
find_symbol(
    name_path="RequestMapper",
    relative_path="adapter-in-rest/src/main/java/**/mapper",
    substring_matching=True
)

# 2. Controller에서 직접 변환 패턴 검색 (안티패턴)
search_for_pattern(
    substring_pattern="new\\s+.*Command\\(request\\.",
    relative_path="adapter-in-rest/src/main/java/**/controller"
)
```

### 7단계: 테스트 검증

```markdown
**검증 대상**: `adapter-in-rest/src/test/java/**/*RestControllerTest.java`

**검증 항목**:
1. TestRestTemplate 사용
   - ✅ `testRestTemplate.postForEntity()`
   - ❌ `mockMvc.perform()`

2. REST Docs 사용
   - ✅ `@AutoConfigureRestDocs`
   - ✅ `document("api-name", ...)`

3. Status Code 검증
   - ✅ `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED)`
```

---

## 📊 검증 결과 리포트

### 리포트 형식

```markdown
# REST API Layer 코딩 컨벤션 검증 결과

**프로젝트**: claude-spring-standards
**검증 날짜**: {검증 실행 날짜}
**검증 범위**: adapter-in-rest/src/main/java, adapter-in-rest/src/test/java

---

## ✅ 준수 항목 (통과)

### RESTful 설계
- [✓] HTTP 메서드 정확한 사용
- [✓] URL 설계 (명사 복수형)
- [✓] 상태 코드 정확성

### Validation
- [✓] @Valid 사용
- [✓] DTO 검증 어노테이션

### ErrorMapper
- [✓] 도메인 예외 → HTTP 응답 변환
- [✓] GlobalExceptionHandler 사용

---

## ❌ 위반 항목 (리팩토링 필요)

### 1. MockMvc 사용 (Zero-Tolerance)

**파일**: `adapter-in-rest/src/test/java/.../OrderRestControllerTest.java:15`

```java
// ❌ 위반 (MockMvc 사용)
@WebMvcTest(OrderRestController.class)
@AutoConfigureMockMvc
class OrderRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("..."))
            .andExpect(status().isCreated());
    }
}

// ✅ 개선 (TestRestTemplate 사용)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestDocs
class OrderRestControllerTest {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void createOrder() {
        CreateOrderRequest request = new CreateOrderRequest(...);

        ResponseEntity<OrderResponse> response = testRestTemplate.postForEntity(
            "/api/v1/orders",
            request,
            OrderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

**심각도**: 🔴 CRITICAL (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 2. RESTful 설계 위반 (상태 코드)

**파일**: `adapter-in-rest/src/main/java/.../OrderRestController.java:23`

```java
// ❌ 위반 (POST 생성 시 200 반환)
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = createOrderUseCase.execute(...);
    return ResponseEntity.ok(response);  // ❌ 200 OK
}

// ✅ 개선 (POST 생성 시 201 Created)
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = createOrderUseCase.execute(...);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);  // ✅ 201 Created
}
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 3. @Valid 누락

**파일**: `adapter-in-rest/src/main/java/.../OrderRestController.java:34`

```java
// ❌ 위반 (@Valid 누락)
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
    // ...
}

// ✅ 개선 (@Valid 추가)
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    // ...
}
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 4. Controller에서 직접 예외 처리

**파일**: `adapter-in-rest/src/main/java/.../OrderRestController.java:45`

```java
// ❌ 위반 (Controller에서 try-catch)
@GetMapping("/{orderId}")
public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
    try {
        OrderResponse response = getOrderUseCase.execute(orderId);
        return ResponseEntity.ok(response);
    } catch (OrderNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

// ✅ 개선 (ErrorMapper로 처리)
@GetMapping("/{orderId}")
public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
    OrderResponse response = getOrderUseCase.execute(orderId);
    return ResponseEntity.ok(response);
}

// GlobalExceptionHandler에서 처리
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(orderErrorMapper.toErrorResponse(e));
    }
}
```

**심각도**: 🟡 MEDIUM
**리팩토링 필요**: 권장

---

## 📋 리팩토링 우선순위

### Priority 1 (즉시 수정 필요)
1. MockMvc 제거, TestRestTemplate 전환 (8건)
2. 상태 코드 수정 (5건)
3. @Valid 누락 추가 (3건)

### Priority 2 (권장)
1. ErrorMapper 패턴 적용 (4건)
2. RequestMapper 패턴 적용 (6건)

### Priority 3 (선택)
1. REST Docs 추가 (10건)
2. API 문서화 개선 (5건)

---

## 🎯 리팩토링 PRD 생성 여부

**위반 항목 수**: 41건
**Zero-Tolerance 위반**: 16건

→ **리팩토링 PRD 생성 필수**
```

---

## 🚀 리팩토링 PRD 자동 생성

위반 항목이 발견되면 자동으로 리팩토링 PRD를 생성합니다.

### PRD 생성 조건

```yaml
auto_generate_prd:
  conditions:
    - zero_tolerance_violations > 0  # Zero-Tolerance 위반 1건 이상
    - mockmvc_count > 0              # MockMvc 사용 1건 이상
    - status_code_violations > 0     # 상태 코드 오류 1건 이상

  prd_location: "docs/prd/refactoring/{ISSUE-KEY}-rest-api-refactoring.md"
```

### PRD 템플릿

```markdown
# REST API Layer 리팩토링 PRD

**이슈 키**: REFACTOR-API-001
**생성 날짜**: {생성 날짜}
**우선순위**: CRITICAL
**예상 소요 시간**: {위반 건수 기반 자동 계산}

---

## 📋 리팩토링 개요

**목적**: REST API Layer 코딩 컨벤션 위반 사항 해결
**범위**: adapter-in-rest/src/main/java, adapter-in-rest/src/test/java
**위반 항목 수**: {총 위반 건수}
**Zero-Tolerance 위반**: {심각도 HIGH/CRITICAL 건수}

---

## 🎯 리팩토링 목표

### 필수 목표 (Zero-Tolerance)
- [ ] MockMvc 제거, TestRestTemplate 전환 (8건)
- [ ] 상태 코드 수정 (5건)
- [ ] @Valid 누락 추가 (3건)

### 권장 목표
- [ ] ErrorMapper 패턴 적용 (4건)
- [ ] RequestMapper 패턴 적용 (6건)

---

## 📝 상세 리팩토링 계획

### Task 1: MockMvc → TestRestTemplate 전환

**파일**: OrderRestControllerTest.java:15

**Before**:
```java
@WebMvcTest(OrderRestController.class)
class OrderRestControllerTest {
    @Autowired
    private MockMvc mockMvc;
}
```

**After**:
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestDocs
class OrderRestControllerTest {
    @Autowired
    private TestRestTemplate testRestTemplate;
}
```

**TDD 사이클**:
1. **Struct**: `struct: @WebMvcTest → @SpringBootTest 전환`
2. **Test**: `test: TestRestTemplate로 E2E 테스트 재작성`
3. **Green**: `feat: 모든 테스트 통과 확인`

---

## ✅ 완료 조건

### Definition of Done
- [ ] 모든 MockMvc 제거
- [ ] 모든 상태 코드 정확히 수정
- [ ] 모든 @Valid 추가
- [ ] ArchUnit 테스트 통과
- [ ] REST Docs 빌드 성공

### 검증 방법
```bash
# ArchUnit 실행
./gradlew :adapter-in-rest:test --tests "*ArchitectureTest"

# REST Docs 빌드
./gradlew :adapter-in-rest:asciidoctor

# 코딩 컨벤션 재검증
/cc/rest-api/validate
```

---

## 📊 예상 메트릭

**예상 커밋 수**: {위반 건수 * 1.5}
**예상 소요 시간**: {위반 건수 * 15분}
**우선순위별 분포**:
- Priority 1: 16건 (240분)
- Priority 2: 10건 (150분)
- Priority 3: 15건 (225분)

**총 예상 시간**: 약 10시간
```

---

## 🛠️ 실행 방법

```bash
# REST API Layer 검증 실행
/cc/rest-api/validate

# 특정 Controller만 검증
/cc/rest-api/validate --target OrderRestController

# 리팩토링 PRD 강제 생성
/cc/rest-api/validate --force-prd
```

---

## 🎯 검증 프로세스

1. **Serena MCP**로 REST API Layer 코드 탐색
2. **RESTful 설계** 검증 (HTTP 메서드, 상태 코드)
3. **MockMvc 사용** Zero-Tolerance 검증
4. **Validation** 검증 (@Valid, 검증 어노테이션)
5. **ErrorMapper 패턴** 검증
6. **DTO/Mapper 패턴** 검증
7. **위반 항목 리포트** 생성
8. **리팩토링 PRD** 자동 생성

---

## 📌 참고 문서

- `docs/coding_convention/01-adapter-in-layer/rest-api/rest-api-guide.md`
- `docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-guide.md`
- `docs/coding_convention/01-adapter-in-layer/rest-api/controller/controller-test-guide.md`
- `.claude/CLAUDE.md` (MockMvc 금지 규칙)
