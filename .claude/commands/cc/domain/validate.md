# Domain Layer Coding Convention Validation

**목적**: TDD로 작성된 Domain Layer 코드가 프로젝트 코딩 컨벤션을 준수하는지 검증하고, 위반 사항에 대한 리팩토링 PRD를 생성합니다.

---

## 🎯 검증 범위

### 1. Domain Layer 코딩 컨벤션 문서

다음 문서들의 규칙을 기준으로 검증합니다:

```
docs/coding_convention/02-domain-layer/
├── aggregate/
│   ├── aggregate-guide.md
│   ├── aggregate-test-guide.md
│   └── aggregate-archunit.md
├── vo/
│   ├── vo-guide.md
│   ├── vo-test-guide.md
│   └── vo-archunit.md
├── exception/
│   ├── domain-exception-guide.md
│   ├── domain-exception-test-guide.md
│   └── domain-exception-archunit-guide.md
└── domain-guide.md
```

### 2. 핵심 검증 항목

#### Zero-Tolerance 규칙 (절대 위반 금지)
- **Lombok 금지**: Domain layer에서 Lombok 어노테이션 사용 금지
- **Law of Demeter**: Getter 체이닝 금지 (`order.getCustomer().getAddress()` ❌)
- **Tell Don't Ask**: Getter 대신 도메인 메서드 사용
- **불변성**: Value Object와 Entity는 불변 객체로 설계
- **Package-Private**: 도메인 내부 구현은 패키지 프라이빗으로 캡슐화

#### 구조 규칙
- **Aggregate Root**: 도메인 모델의 일관성 경계 정의
- **Value Object**: 식별자 없는 불변 값 객체
- **도메인 예외**: 비즈니스 규칙 위반 시 명확한 도메인 예외 발생
- **생성자 패턴**: 정적 팩토리 메서드 권장 (`of()`, `from()`, `create()`)

#### 테스트 규칙
- **단위 테스트**: 각 도메인 객체는 독립적인 단위 테스트 보유
- **Given-When-Then**: 명확한 BDD 스타일 테스트 구조
- **경계값 테스트**: 도메인 규칙의 경계값 검증

---

## 🔍 검증 프로세스

### 1단계: Aggregate 검증

```markdown
**검증 대상**: `domain/src/main/java/**/aggregate/`

**검증 항목**:
1. Lombok 사용 여부 체크
   - `@Getter`, `@Setter`, `@Data` 등 금지

2. Law of Demeter 위반 체크
   - Getter 체이닝 패턴 검색
   - 예: `obj.getX().getY()` 패턴

3. Tell Don't Ask 위반 체크
   - Getter 남용 여부
   - 도메인 로직이 외부에 노출되어 있는지

4. 불변성 체크
   - Final 필드 사용 여부
   - Setter 메서드 존재 여부

5. 캡슐화 체크
   - 생성자 접근 제어자 (Protected 권장)
   - 정적 팩토리 메서드 존재 여부

6. 비즈니스 로직 위치
   - 도메인 로직이 Aggregate 내부에 존재하는지
   - 외부 Service에 노출되지 않았는지
```

**Serena MCP 활용**:
```python
# 1. Aggregate 클래스 탐색
find_symbol(name_path="/", relative_path="domain/src/main/java",
            include_kinds=[5], substring_matching=True)  # Class only

# 2. Lombok 어노테이션 검색
search_for_pattern(
    substring_pattern="@(Getter|Setter|Data|Builder|AllArgsConstructor|NoArgsConstructor)",
    relative_path="domain/src/main/java",
    restrict_search_to_code_files=True
)

# 3. Getter 체이닝 패턴 검색
search_for_pattern(
    substring_pattern="\\.get[A-Z][a-zA-Z0-9_]*\\(\\)\\.get[A-Z]",
    relative_path="domain/src/main/java"
)

# 4. Setter 메서드 검색
search_for_pattern(
    substring_pattern="public\\s+void\\s+set[A-Z]",
    relative_path="domain/src/main/java"
)
```

### 2단계: Value Object 검증

```markdown
**검증 대상**: `domain/src/main/java/**/vo/`

**검증 항목**:
1. Record 또는 Final Class 사용
2. Equals/HashCode 구현 여부
3. 검증 로직 존재 여부 (생성자 또는 정적 팩토리 메서드)
4. Lombok 미사용
5. 불변성 보장 (모든 필드 final)
```

**Serena MCP 활용**:
```python
# VO 클래스 탐색 및 검증
find_symbol(name_path="/", relative_path="domain/src/main/java/**/vo",
            include_body=True, depth=1)
```

### 3단계: Domain Exception 검증

```markdown
**검증 대상**: `domain/src/main/java/**/exception/`

**검증 항목**:
1. RuntimeException 상속
2. 명확한 에러 메시지
3. 에러 코드 정의
4. 비즈니스 규칙 위반 시나리오별 예외 클래스
```

### 4단계: 테스트 검증

```markdown
**검증 대상**: `domain/src/test/java/`

**검증 항목**:
1. 각 도메인 객체마다 테스트 클래스 존재 여부
2. Given-When-Then 구조 준수
3. DisplayName 명확성
4. 경계값 테스트 존재 여부
5. 예외 케이스 테스트 존재 여부
```

---

## 📊 검증 결과 리포트

### 리포트 형식

```markdown
# Domain Layer 코딩 컨벤션 검증 결과

**프로젝트**: claude-spring-standards
**검증 날짜**: {검증 실행 날짜}
**검증 범위**: domain/src/main/java, domain/src/test/java

---

## ✅ 준수 항목 (통과)

### Aggregate
- [✓] Lombok 미사용
- [✓] 불변성 보장 (final 필드)
- [✓] 정적 팩토리 메서드 사용

### Value Object
- [✓] Record 패턴 사용
- [✓] 검증 로직 존재

### Exception
- [✓] RuntimeException 상속
- [✓] 명확한 에러 메시지

---

## ❌ 위반 항목 (리팩토링 필요)

### 1. Law of Demeter 위반

**파일**: `domain/src/main/java/com/company/template/domain/order/aggregate/Order.java:45`

```java
// ❌ 위반
Address address = order.getCustomer().getAddress();

// ✅ 개선
Address address = order.getCustomerAddress();  // Tell Don't Ask
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 2. Getter 남용 (Tell Don't Ask 위반)

**파일**: `domain/src/main/java/com/company/template/domain/order/aggregate/Order.java:67`

```java
// ❌ 위반 (외부에서 도메인 로직 수행)
if (order.getStatus() == OrderStatus.PENDING && order.getAmount() > 1000) {
    order.setStatus(OrderStatus.APPROVED);
}

// ✅ 개선 (도메인 내부로 로직 이동)
order.approveIfEligible();  // 내부에서 상태/금액 검증
```

**심각도**: 🟡 MEDIUM
**리팩토링 필요**: 권장

---

### 3. 불변성 위반

**파일**: `domain/src/main/java/com/company/template/domain/order/vo/Money.java:12`

```java
// ❌ 위반
private BigDecimal amount;  // non-final

// ✅ 개선
private final BigDecimal amount;
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

## 📋 리팩토링 우선순위

### Priority 1 (즉시 수정 필요)
1. Law of Demeter 위반 3건
2. 불변성 위반 2건
3. Lombok 사용 1건

### Priority 2 (권장)
1. Tell Don't Ask 위반 5건
2. 테스트 누락 2건

### Priority 3 (선택)
1. DisplayName 개선 10건
2. 생성자 접근 제어자 조정 3건

---

## 🎯 리팩토링 PRD 생성 여부

**위반 항목 수**: 23건
**Zero-Tolerance 위반**: 6건

→ **리팩토링 PRD 생성 권장**
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
    - severity_high_count > 3        # 심각도 HIGH 3건 이상

  prd_location: "docs/prd/refactoring/{ISSUE-KEY}-domain-refactoring.md"
```

### PRD 템플릿

```markdown
# Domain Layer 리팩토링 PRD

**이슈 키**: REFACTOR-DOMAIN-001
**생성 날짜**: {생성 날짜}
**우선순위**: HIGH
**예상 소요 시간**: {위반 건수 기반 자동 계산}

---

## 📋 리팩토링 개요

**목적**: Domain Layer 코딩 컨벤션 위반 사항 해결
**범위**: domain/src/main/java, domain/src/test/java
**위반 항목 수**: {총 위반 건수}
**Zero-Tolerance 위반**: {심각도 HIGH 건수}

---

## 🎯 리팩토링 목표

### 필수 목표 (Zero-Tolerance)
- [ ] Law of Demeter 위반 해결 (3건)
- [ ] 불변성 보장 (2건)
- [ ] Lombok 제거 (1건)

### 권장 목표
- [ ] Tell Don't Ask 패턴 적용 (5건)
- [ ] 누락된 테스트 추가 (2건)

---

## 📝 상세 리팩토링 계획

### Task 1: Law of Demeter 위반 해결

**파일**: Order.java:45

**Before**:
```java
Address address = order.getCustomer().getAddress();
```

**After**:
```java
Address address = order.getCustomerAddress();  // Aggregate에 메서드 추가
```

**TDD 사이클**:
1. **Red**: `test: Order.getCustomerAddress() 테스트 추가`
2. **Green**: `feat: Order.getCustomerAddress() 구현`
3. **Refactor**: `struct: 기존 Getter 체이닝 제거`

---

### Task 2: 불변성 보장

**파일**: Money.java:12

**Before**:
```java
private BigDecimal amount;
```

**After**:
```java
private final BigDecimal amount;
```

**TDD 사이클**:
1. **Refactor**: `struct: Money 필드 final 선언` (동작 변경 없음)

---

## ✅ 완료 조건

### Definition of Done
- [ ] 모든 Zero-Tolerance 위반 해결
- [ ] ArchUnit 테스트 통과
- [ ] 기존 단위 테스트 모두 통과
- [ ] 코드 리뷰 승인

### 검증 방법
```bash
# ArchUnit 실행
./gradlew :domain:test --tests "*ArchitectureTest"

# 전체 테스트 실행
./gradlew :domain:test

# 코딩 컨벤션 재검증
/cc/domain/validate
```

---

## 📊 예상 메트릭

**예상 커밋 수**: {위반 건수 * 1.5} (TDD 사이클 기준)
**예상 소요 시간**: {위반 건수 * 15분}
**우선순위별 분포**:
- Priority 1: 6건 (90분)
- Priority 2: 7건 (105분)
- Priority 3: 10건 (150분)

**총 예상 시간**: 약 5.75시간
```

---

## 🛠️ 실행 방법

```bash
# Domain Layer 검증 실행
/cc/domain/validate

# 특정 Aggregate만 검증
/cc/domain/validate --target Order

# 리팩토링 PRD 강제 생성 (위반 건수 무관)
/cc/domain/validate --force-prd
```

---

## 🎯 검증 프로세스

1. **Serena MCP**로 Domain Layer 코드 탐색
2. **Zero-Tolerance 규칙** 위반 검색
3. **구조 규칙** 위반 검색
4. **테스트 규칙** 위반 검색
5. **위반 항목 리포트** 생성
6. **리팩토링 PRD** 자동 생성 (조건 충족 시)

---

## 📌 참고 문서

- `docs/coding_convention/02-domain-layer/domain-guide.md`
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md`
- `docs/coding_convention/02-domain-layer/vo/vo-guide.md`
- `.claude/CLAUDE.md` (TDD + Tidy First 철학)
