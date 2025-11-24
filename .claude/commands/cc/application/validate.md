# Application Layer Coding Convention Validation

**목적**: TDD로 작성된 Application Layer 코드가 프로젝트 코딩 컨벤션을 준수하는지 검증하고, 위반 사항에 대한 리팩토링 PRD를 생성합니다.

---

## 🎯 검증 범위

### 1. Application Layer 코딩 컨벤션 문서

다음 문서들의 규칙을 기준으로 검증합니다:

```
docs/coding_convention/03-application-layer/
├── port/
│   ├── in/
│   │   ├── command/  (UseCase 인터페이스)
│   │   └── query/    (ReadUseCase 인터페이스)
│   └── out/
│       ├── command/  (Repository 포트)
│       └── query/    (조회 포트)
├── assembler/  (DTO ↔ Domain 변환)
├── facade/  (복잡한 조율)
├── manager/  (Transaction 경계)
└── application-guide.md
```

### 2. 핵심 검증 항목

#### Zero-Tolerance 규칙 (절대 위반 금지)
- **Transaction 경계**: `@Transactional`은 오직 `*TransactionManager`에만 사용
- **외부 API 호출 금지**: `@Transactional` 내부에서 외부 API 호출 절대 금지
- **Spring 프록시 제약**: Private/Final 메서드에 `@Transactional` 금지
- **CQRS 분리**: Command와 Query UseCase 명확히 분리
- **Assembler 필수**: DTO ↔ Domain 변환은 반드시 Assembler 사용

#### 구조 규칙
- **UseCase 인터페이스**: Port In 패턴 준수
- **Repository 포트**: Port Out 패턴 준수
- **DTO Record 패턴**: Command/Query/Response는 Record 사용
- **Facade 패턴**: 복잡한 다중 UseCase 조율 시 사용
- **Manager 패턴**: Transaction 경계 관리 전담

#### 테스트 규칙
- **단위 테스트**: Mock을 활용한 UseCase 단위 테스트
- **Transaction 테스트**: TransactionManager의 트랜잭션 동작 검증
- **Assembler 테스트**: 변환 로직 정확성 검증

---

## 🔍 검증 프로세스

### 1단계: Transaction 경계 검증 (Zero-Tolerance)

```markdown
**검증 대상**: `application/src/main/java/**/*UseCase.java`

**검증 항목**:
1. UseCase 클래스에 `@Transactional` 사용 금지
   - ❌ `@Transactional public class CreateOrderUseCase`
   - ✅ `public class CreateOrderUseCase` (TransactionManager에서 호출)

2. TransactionManager만 `@Transactional` 사용
   - ✅ `@Transactional public class OrderTransactionManager`

3. `@Transactional` 내부에서 외부 API 호출 금지
   - ❌ `restTemplate.postForEntity()` (Transaction 내)
   - ❌ `webClient.post().retrieve()` (Transaction 내)
   - ✅ 외부 API는 Transaction 외부에서 호출

4. Spring 프록시 제약 준수
   - ❌ `@Transactional private void method()`
   - ❌ `@Transactional final void method()`
   - ❌ 같은 클래스 내부에서 `this.transactionalMethod()` 호출
```

**Serena MCP 활용**:
```python
# 1. UseCase 클래스에서 @Transactional 검색
search_for_pattern(
    substring_pattern="@Transactional.*class\\s+.*UseCase",
    relative_path="application/src/main/java",
    restrict_search_to_code_files=True
)

# 2. @Transactional 내부에서 외부 API 호출 검색
search_for_pattern(
    substring_pattern="@Transactional.*\\{[\\s\\S]*?(restTemplate|webClient|httpClient|feignClient)",
    relative_path="application/src/main/java",
    multiline=True
)

# 3. Private/Final 메서드에 @Transactional 검색
search_for_pattern(
    substring_pattern="@Transactional\\s+(private|final)\\s+",
    relative_path="application/src/main/java"
)
```

### 2단계: CQRS 분리 검증

```markdown
**검증 대상**:
- `application/src/main/java/**/port/in/command/*UseCase.java`
- `application/src/main/java/**/port/in/query/*ReadUseCase.java`

**검증 항목**:
1. Command UseCase 네이밍
   - ✅ `CreateOrderUseCase`, `UpdateOrderUseCase`
   - ❌ `OrderUseCase`, `GetOrderUseCase` (모호함)

2. Query UseCase 네이밍
   - ✅ `GetOrderReadUseCase`, `ListOrdersReadUseCase`
   - ❌ `OrderQueryUseCase` (Query 접미사 금지, ReadUseCase 사용)

3. Command/Query 혼합 금지
   - ❌ Command UseCase에서 조회 로직 포함
   - ❌ Query UseCase에서 상태 변경

4. DTO 분리
   - Command: `*Command` record
   - Query: `*Query` record (조건이 있는 경우만)
   - Response: `*Response` record
```

**Serena MCP 활용**:
```python
# 1. Command UseCase 탐색
find_symbol(
    name_path="UseCase",
    relative_path="application/src/main/java/**/port/in/command",
    substring_matching=True,
    include_kinds=[5]  # Interface
)

# 2. Query UseCase 탐색
find_symbol(
    name_path="ReadUseCase",
    relative_path="application/src/main/java/**/port/in/query",
    substring_matching=True,
    include_kinds=[5]  # Interface
)

# 3. Query 접미사 사용 검색 (금지 패턴)
search_for_pattern(
    substring_pattern="interface\\s+.*QueryUseCase",
    relative_path="application/src/main/java/**/port/in"
)
```

### 3단계: Assembler 패턴 검증

```markdown
**검증 대상**: `application/src/main/java/**/assembler/`

**검증 항목**:
1. Assembler 클래스 존재 여부
   - ✅ `OrderAssembler.java`
   - ❌ UseCase에서 직접 변환 (`new Order(command.name())`)

2. Assembler 메서드 네이밍
   - ✅ `toDomain()`: DTO → Domain
   - ✅ `toResponse()`: Domain → Response
   - ❌ `convert()`, `map()` (모호함)

3. DTO ↔ Domain 변환은 항상 Assembler 사용
   - ❌ UseCase에서 직접 변환
   - ❌ Domain에서 DTO 의존

4. Assembler는 순수 변환 로직만
   - ❌ 비즈니스 로직 포함
   - ❌ Repository 호출
```

**Serena MCP 활용**:
```python
# 1. Assembler 클래스 탐색
find_symbol(
    name_path="Assembler",
    relative_path="application/src/main/java/**/assembler",
    substring_matching=True,
    include_kinds=[5]  # Class
)

# 2. UseCase에서 직접 변환 패턴 검색 (안티패턴)
search_for_pattern(
    substring_pattern="new\\s+[A-Z][a-zA-Z0-9_]*\\(command\\.",
    relative_path="application/src/main/java/**/*UseCase.java"
)

# 3. Domain에서 DTO 의존 검색 (안티패턴)
search_for_pattern(
    substring_pattern="import.*\\.application\\..*\\.(Command|Query|Response)",
    relative_path="domain/src/main/java"
)
```

### 4단계: Port 패턴 검증

```markdown
**검증 대상**:
- `application/src/main/java/**/port/in/`
- `application/src/main/java/**/port/out/`

**검증 항목**:
1. Port In 인터페이스
   - ✅ Command UseCase는 `port/in/command/` 위치
   - ✅ Query UseCase는 `port/in/query/` 위치

2. Port Out 인터페이스
   - ✅ `*Port` 네이밍
   - ✅ `SaveOrderPort`, `LoadOrderPort`
   - ❌ `*Repository` (JPA Repository와 혼동)

3. 의존성 방향
   - ✅ Domain ← Application → Adapter
   - ❌ Domain → Application (역전 위반)
```

### 5단계: Manager 패턴 검증

```markdown
**검증 대상**: `application/src/main/java/**/manager/*TransactionManager.java`

**검증 항목**:
1. TransactionManager 네이밍
   - ✅ `OrderTransactionManager`
   - ❌ `OrderService`, `OrderManager`

2. @Transactional 위치
   - ✅ TransactionManager 메서드에만
   - ❌ UseCase 클래스/메서드에

3. Transaction 경계 명확성
   - ✅ 한 메서드 = 한 트랜잭션
   - ❌ 중첩 트랜잭션 (전파 속성 주의)

4. 외부 API 호출 위치
   - ✅ TransactionManager 외부 (Before/After)
   - ❌ TransactionManager 내부
```

### 6단계: 테스트 검증

```markdown
**검증 대상**: `application/src/test/java/`

**검증 항목**:
1. UseCase 단위 테스트
   - Mock 활용 (Port Out Mock)
   - Given-When-Then 구조

2. TransactionManager 테스트
   - 트랜잭션 롤백 검증
   - 외부 API 호출 순서 검증

3. Assembler 테스트
   - DTO → Domain 변환 정확성
   - Domain → Response 변환 정확성
```

---

## 📊 검증 결과 리포트

### 리포트 형식

```markdown
# Application Layer 코딩 컨벤션 검증 결과

**프로젝트**: claude-spring-standards
**검증 날짜**: {검증 실행 날짜}
**검증 범위**: application/src/main/java, application/src/test/java

---

## ✅ 준수 항목 (통과)

### Transaction 경계
- [✓] TransactionManager에만 @Transactional 사용
- [✓] 외부 API 호출 Transaction 외부에서 수행
- [✓] Spring 프록시 제약 준수

### CQRS 분리
- [✓] Command/Query UseCase 명확히 분리
- [✓] DTO 네이밍 규칙 준수

### Assembler 패턴
- [✓] DTO ↔ Domain 변환은 Assembler 사용
- [✓] Assembler 메서드 네이밍 준수

---

## ❌ 위반 항목 (리팩토링 필요)

### 1. Transaction 경계 위반 (Zero-Tolerance)

**파일**: `application/src/main/java/.../CreateOrderUseCase.java:12`

```java
// ❌ 위반 (UseCase에 @Transactional)
@Transactional
public class CreateOrderUseCase implements CreateOrderPort {
    ...
}

// ✅ 개선 (TransactionManager로 이동)
public class OrderTransactionManager {
    @Transactional
    public OrderResponse createOrder(CreateOrderCommand command) {
        return createOrderUseCase.execute(command);
    }
}
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 2. 외부 API 호출 Transaction 내부 (Zero-Tolerance)

**파일**: `application/src/main/java/.../OrderTransactionManager.java:34`

```java
// ❌ 위반 (Transaction 내부에서 외부 API 호출)
@Transactional
public void processOrder(Long orderId) {
    Order order = loadOrderPort.load(orderId);
    order.process();

    // ❌ 외부 API 호출 (Payment Gateway)
    PaymentResult result = paymentClient.charge(order.getAmount());

    saveOrderPort.save(order);
}

// ✅ 개선 (외부 API는 Transaction 외부에서)
public void processOrder(Long orderId) {
    // 1. Transaction 외부 - 외부 API 호출
    PaymentResult result = paymentClient.charge(amount);

    // 2. Transaction 내부 - DB 작업만
    processOrderInternal(orderId, result);
}

@Transactional
private void processOrderInternal(Long orderId, PaymentResult result) {
    Order order = loadOrderPort.load(orderId);
    order.process(result);
    saveOrderPort.save(order);
}
```

**심각도**: 🔴 CRITICAL (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 3. Assembler 미사용 (Direct Conversion)

**파일**: `application/src/main/java/.../CreateOrderUseCase.java:23`

```java
// ❌ 위반 (UseCase에서 직접 변환)
public OrderResponse execute(CreateOrderCommand command) {
    Order order = new Order(
        command.customerId(),
        command.items(),
        command.totalAmount()
    );

    // ...
}

// ✅ 개선 (Assembler 사용)
public OrderResponse execute(CreateOrderCommand command) {
    Order order = orderAssembler.toDomain(command);
    // ...
    return orderAssembler.toResponse(order);
}
```

**심각도**: 🟡 MEDIUM
**리팩토링 필요**: 권장

---

### 4. CQRS 분리 위반

**파일**: `application/src/main/java/.../CreateOrderUseCase.java:45`

```java
// ❌ 위반 (Command UseCase에서 조회 로직)
public OrderResponse execute(CreateOrderCommand command) {
    // ❌ 조회 로직 혼재
    List<Order> existingOrders = loadOrdersPort.findByCustomerId(command.customerId());

    Order order = orderAssembler.toDomain(command);
    // ...
}

// ✅ 개선 (조회는 별도 Query UseCase)
public OrderResponse execute(CreateOrderCommand command) {
    Order order = orderAssembler.toDomain(command);
    saveOrderPort.save(order);
    return orderAssembler.toResponse(order);
}

// 조회는 별도 ReadUseCase
public class ListOrdersReadUseCase {
    public List<OrderResponse> execute(ListOrdersQuery query) {
        // ...
    }
}
```

**심각도**: 🟡 MEDIUM
**리팩토링 필요**: 권장

---

## 📋 리팩토링 우선순위

### Priority 1 (즉시 수정 필요)
1. Transaction 경계 위반 2건
2. 외부 API Transaction 내부 호출 1건
3. Spring 프록시 제약 위반 1건

### Priority 2 (권장)
1. Assembler 미사용 4건
2. CQRS 분리 위반 3건
3. Port 네이밍 규칙 위반 2건

### Priority 3 (선택)
1. 테스트 누락 5건
2. DisplayName 개선 8건

---

## 🎯 리팩토링 PRD 생성 여부

**위반 항목 수**: 26건
**Zero-Tolerance 위반**: 4건

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
    - total_violations > 10          # 전체 위반 10건 이상
    - severity_critical_count > 0    # CRITICAL 위반 1건 이상

  prd_location: "docs/prd/refactoring/{ISSUE-KEY}-application-refactoring.md"
```

### PRD 템플릿

```markdown
# Application Layer 리팩토링 PRD

**이슈 키**: REFACTOR-APP-001
**생성 날짜**: {생성 날짜}
**우선순위**: CRITICAL
**예상 소요 시간**: {위반 건수 기반 자동 계산}

---

## 📋 리팩토링 개요

**목적**: Application Layer 코딩 컨벤션 위반 사항 해결
**범위**: application/src/main/java, application/src/test/java
**위반 항목 수**: {총 위반 건수}
**Zero-Tolerance 위반**: {심각도 HIGH/CRITICAL 건수}

---

## 🎯 리팩토링 목표

### 필수 목표 (Zero-Tolerance)
- [ ] Transaction 경계 위반 해결 (2건)
- [ ] 외부 API Transaction 내부 호출 제거 (1건)
- [ ] Spring 프록시 제약 준수 (1건)

### 권장 목표
- [ ] Assembler 패턴 적용 (4건)
- [ ] CQRS 분리 강화 (3건)

---

## 📝 상세 리팩토링 계획

### Task 1: Transaction 경계 위반 해결

**파일**: CreateOrderUseCase.java:12

**Before**:
```java
@Transactional
public class CreateOrderUseCase {
    public OrderResponse execute(CreateOrderCommand command) {
        // ...
    }
}
```

**After**:
```java
// 1. UseCase에서 @Transactional 제거
public class CreateOrderUseCase {
    public OrderResponse execute(CreateOrderCommand command) {
        // ...
    }
}

// 2. TransactionManager 생성
public class OrderTransactionManager {
    private final CreateOrderUseCase createOrderUseCase;

    @Transactional
    public OrderResponse createOrder(CreateOrderCommand command) {
        return createOrderUseCase.execute(command);
    }
}
```

**TDD 사이클**:
1. **Struct**: `struct: @Transactional을 TransactionManager로 이동` (동작 변경 없음)
2. **Test**: `test: TransactionManager 트랜잭션 경계 테스트`
3. **Green**: `feat: OrderTransactionManager 구현`

---

### Task 2: 외부 API Transaction 외부 호출

**파일**: OrderTransactionManager.java:34

**Before**:
```java
@Transactional
public void processOrder(Long orderId) {
    Order order = loadOrderPort.load(orderId);
    PaymentResult result = paymentClient.charge(order.getAmount());  // ❌
    saveOrderPort.save(order);
}
```

**After**:
```java
public void processOrder(Long orderId) {
    // 1. Transaction 외부 - 외부 API
    Order order = loadOrderPort.load(orderId);
    PaymentResult result = paymentClient.charge(order.getAmount());

    // 2. Transaction 내부 - DB 작업
    processOrderInternal(orderId, result);
}

@Transactional
private void processOrderInternal(Long orderId, PaymentResult result) {
    Order order = loadOrderPort.load(orderId);
    order.process(result);
    saveOrderPort.save(order);
}
```

**TDD 사이클**:
1. **Test**: `test: 외부 API 호출이 Transaction 외부에서 수행되는지 검증`
2. **Struct**: `struct: Transaction 경계 분리` (메서드 추출)
3. **Green**: `feat: Transaction 외부/내부 로직 분리 구현`

---

## ✅ 완료 조건

### Definition of Done
- [ ] 모든 Zero-Tolerance 위반 해결
- [ ] Transaction 경계 테스트 통과
- [ ] ArchUnit 테스트 통과
- [ ] 기존 단위 테스트 모두 통과

### 검증 방법
```bash
# ArchUnit 실행
./gradlew :application:test --tests "*ArchitectureTest"

# Transaction 경계 검증
./gradlew :application:test --tests "*TransactionTest"

# 코딩 컨벤션 재검증
/cc/application/validate
```

---

## 📊 예상 메트릭

**예상 커밋 수**: {위반 건수 * 2} (TDD 사이클 + Tidy First)
**예상 소요 시간**: {위반 건수 * 20분}
**우선순위별 분포**:
- Priority 1: 4건 (80분)
- Priority 2: 7건 (140분)
- Priority 3: 13건 (260분)

**총 예상 시간**: 약 8시간
```

---

## 🛠️ 실행 방법

```bash
# Application Layer 검증 실행
/cc/application/validate

# 특정 UseCase만 검증
/cc/application/validate --target CreateOrderUseCase

# 리팩토링 PRD 강제 생성
/cc/application/validate --force-prd
```

---

## 🎯 검증 프로세스

1. **Serena MCP**로 Application Layer 코드 탐색
2. **Transaction 경계** Zero-Tolerance 규칙 검증
3. **CQRS 분리** 검증
4. **Assembler 패턴** 적용 여부 검증
5. **Port 패턴** 구조 검증
6. **위반 항목 리포트** 생성
7. **리팩토링 PRD** 자동 생성

---

## 📌 참고 문서

- `docs/coding_convention/03-application-layer/application-guide.md`
- `docs/coding_convention/03-application-layer/manager/transaction-manager-guide.md`
- `docs/coding_convention/03-application-layer/assembler/assembler-guide.md`
- `.claude/CLAUDE.md` (Spring 프록시 제약사항)
