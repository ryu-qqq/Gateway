# /validate-conventions - PRD/Task 문서 컨벤션 검증 및 수정

**목적**: PRD/Task 문서가 `docs/coding_convention/` 규칙을 준수하는지 검증하고 자동 수정

**사용법**:
```bash
/validate-conventions <file-path>
/validate-conventions docs/prd/tasks/MEMBER-001.md
/validate-conventions docs/prd/member-management.md
```

---

## 📋 작업 순서

### 1. 문서 읽기 및 Layer 식별

**입력**:
- 문서 파일 경로 (PRD 또는 Task)

**Layer 식별 규칙**:
```markdown
# Task 파일인 경우
**Layer**: Domain Layer → domain
**Layer**: Application Layer → application
**Layer**: Persistence Layer → persistence
**Layer**: REST API Layer → rest-api
**Layer**: Integration Test → integration

# PRD 파일인 경우
전체 레이어 규칙 적용 (Multi-layer)
```

### 2. 레이어별 컨벤션 규칙 로드

**컨벤션 디렉토리 구조**:
```
docs/coding_convention/
├── 02-domain-layer/          (12개 규칙)
│   ├── aggregate/
│   ├── vo/
│   ├── exception/
│   └── domain-guide.md
├── 03-application-layer/      (26개 규칙)
│   ├── port/in/command/
│   ├── port/in/query/
│   └── application-guide.md
├── 04-persistence-layer/      (23개 규칙)
│   ├── mysql/entity/
│   ├── mysql/repository/
│   └── persistence-mysql-guide.md
└── 01-adapter-in-layer/rest-api/  (22개 규칙)
    ├── controller/
    ├── dto/
    └── rest-api-guide.md
```

**로드할 규칙**:
- Layer가 `domain`이면 → `02-domain-layer/**/*.md` 읽기
- Layer가 `application`이면 → `03-application-layer/**/*.md` 읽기
- Layer가 `persistence`이면 → `04-persistence-layer/**/*.md` 읽기
- Layer가 `rest-api`이면 → `01-adapter-in-layer/rest-api/**/*.md` 읽기
- PRD 파일이면 → 모든 레이어 규칙 읽기

### 3. Zero-Tolerance 규칙 검증

**검증 항목**:

#### Domain Layer 규칙
- ❌ Lombok 사용 금지 (`@Getter`, `@Setter`, `@Builder` 등)
- ❌ Getter 체이닝 금지 (`order.getCustomer().getAddress()`)
- ✅ Tell Don't Ask 패턴 준수
- ✅ Law of Demeter 준수
- ✅ Plain Java 사용 (Record는 VO만 허용)

#### Application Layer 규칙
- ❌ `@Transactional` 내 외부 API 호출 금지
- ❌ UseCase에서 다른 UseCase 호출 금지
- ✅ Command/Query 분리 (CQRS)
- ✅ Assembler 패턴 사용
- ✅ Port는 Interface로 정의

#### Persistence Layer 규칙
- ❌ JPA 관계 어노테이션 금지 (`@OneToMany`, `@ManyToOne` 등)
- ❌ Lombok 금지
- ✅ Long FK 전략 사용
- ✅ QueryDSL DTO Projection 사용
- ✅ BaseAuditEntity 상속

#### REST API Layer 규칙
- ❌ MockMvc 테스트 금지
- ✅ TestRestTemplate 사용 필수
- ✅ Request/Response DTO (Record)
- ✅ `@Valid` 검증 필수
- ✅ RESTful 설계 준수

### 4. 위반 사항 검출 및 수정

**검출 패턴**:
```yaml
domain_violations:
  lombok:
    pattern: '@(Getter|Setter|Builder|Data|AllArgsConstructor|NoArgsConstructor)'
    severity: critical
    fix: "Plain Java로 변경 (수동 Getter/Constructor 작성)"

  getter_chaining:
    pattern: '\.\s*get\w+\(\)\s*\.\s*get\w+'
    severity: critical
    fix: "Tell Don't Ask 패턴 적용"

application_violations:
  transactional_external_call:
    pattern: '@Transactional.*RestTemplate|WebClient|FeignClient'
    severity: critical
    fix: "외부 API 호출은 @Transactional 밖으로 분리"

  usecase_coupling:
    pattern: 'UseCase.*\.execute\(|UseCase.*\.process\('
    severity: critical
    fix: "UseCase 간 호출 금지, Facade 패턴 사용"

persistence_violations:
  jpa_relations:
    pattern: '@(OneToMany|ManyToOne|OneToOne|ManyToMany)'
    severity: critical
    fix: "Long FK 전략으로 변경 (Long userId)"

  lombok_entity:
    pattern: '@(Getter|Setter|Builder|Data)'
    severity: critical
    fix: "Plain Java Entity로 변경"

rest_api_violations:
  mockmvc:
    pattern: 'MockMvc|@WebMvcTest'
    severity: critical
    fix: "TestRestTemplate + @SpringBootTest 사용"

  missing_validation:
    pattern: 'public.*Request.*\n(?!.*@Valid)'
    severity: warning
    fix: "@Valid 어노테이션 추가"
```

### 5. 검증 리포트 생성

**출력 형식**:
```markdown
# 컨벤션 검증 리포트

**파일**: docs/prd/tasks/MEMBER-001.md
**Layer**: Domain Layer
**검증 시간**: 2025-11-18 12:34:56

---

## ✅ 통과한 규칙 (8/10)

- ✅ Lombok 금지 준수
- ✅ Law of Demeter 준수
- ✅ Plain Java 사용
- ✅ Tell Don't Ask 패턴 적용
- ✅ ArchUnit 테스트 포함
- ✅ TestFixture 사용
- ✅ Javadoc 필수 항목 포함
- ✅ Record는 VO만 사용

---

## ❌ 위반 사항 (2개)

### 🚨 CRITICAL: Getter 체이닝 발견

**위치**: Line 45
```java
String city = order.getCustomer().getAddress().getCity();
```

**수정 방안**:
```java
// Before (❌)
String city = order.getCustomer().getAddress().getCity();

// After (✅)
String city = order.getCustomerCity();  // Tell Don't Ask

// Order 클래스 내부
public String getCustomerCity() {
    return customer.getCity();  // Customer가 Address를 처리
}
```

**관련 규칙**: `docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md`

---

### ⚠️ WARNING: Javadoc 누락

**위치**: Line 78
```java
public Member create(String email, String password) {
    // Javadoc 없음
}
```

**수정 방안**:
```java
/**
 * 새로운 회원을 생성합니다.
 *
 * @param email 회원 이메일
 * @param password 암호화되지 않은 비밀번호
 * @return 생성된 Member Aggregate
 * @throws InvalidEmailException 이메일 형식이 유효하지 않은 경우
 */
public Member create(String email, String password) {
    ...
}
```

**관련 규칙**: `docs/coding_convention/02-domain-layer/domain-guide.md`

---

## 📊 통계

- **총 규칙 수**: 10
- **통과**: 8 (80%)
- **위반**: 2 (20%)
  - Critical: 1
  - Warning: 1

---

## 🔧 자동 수정 가능 여부

- ❌ Getter 체이닝: 수동 리팩토링 필요 (Tell Don't Ask 패턴)
- ✅ Javadoc 누락: 자동 생성 가능

---

## 🎯 다음 단계

1. Critical 위반 사항 수정 (Getter 체이닝)
2. Warning 위반 사항 수정 (Javadoc)
3. `/validate-conventions` 재실행
4. 모든 규칙 통과 시 `/create-plan MEMBER-001` 진행
```

---

## 🚀 실행 예시

### 예시 1: Task 파일 검증

**입력**:
```bash
/validate-conventions docs/prd/tasks/MEMBER-001.md
```

**출력**:
```
🔍 문서 분석 중...
   └─ Layer: Domain Layer
   └─ 적용 규칙: 12개

✅ 검증 완료: 10/12 규칙 통과 (83%)

❌ 위반 사항:
   1. [CRITICAL] Lombok 사용 발견 (Line 56)
   2. [WARNING] ArchUnit 테스트 누락

📋 상세 리포트: docs/prd/tasks/MEMBER-001-validation-report.md

🔧 자동 수정 가능:
   - Lombok 제거 → Plain Java 변환

❓ 자동 수정하시겠습니까? (y/n)
```

### 예시 2: PRD 파일 검증 (Multi-layer)

**입력**:
```bash
/validate-conventions docs/prd/member-management.md
```

**출력**:
```
🔍 PRD 분석 중...
   └─ Epic: 회원 관리 시스템
   └─ Issue Prefix: MEMBER
   └─ 적용 규칙: 88개 (전체 레이어)

✅ 검증 완료: 82/88 규칙 통과 (93%)

📊 레이어별 위반 사항:
   - Domain Layer: 1개 (Lombok)
   - Application Layer: 2개 (Transaction 경계, UseCase 호출)
   - Persistence Layer: 1개 (JPA 관계)
   - REST API Layer: 2개 (MockMvc, Validation)

📋 상세 리포트: docs/prd/member-management-validation-report.md

🔗 다음 단계:
   1. 위반 사항 수정
   2. /validate-conventions 재실행
   3. /breakdown-prd docs/prd/member-management.md
```

---

## 🎯 통합 워크플로우

```bash
# 1. PRD 작성 (대화형)
/create-prd

# 2. PRD 컨벤션 검증 ← NEW!
/validate-conventions docs/prd/member-management.md

# 3. 위반 사항 수정 후 재검증
/validate-conventions docs/prd/member-management.md

# 4. 레이어별 Task 분할
/breakdown-prd docs/prd/member-management.md

# 5. 각 Task 컨벤션 검증 ← NEW!
/validate-conventions docs/prd/tasks/MEMBER-001.md

# 6. TDD Plan 생성
/create-plan MEMBER-001

# 7. TDD 실행 (Cursor 멀티 에이전트)
/kb/domain/go
```

---

## ⚙️ 설정

### 검증 레벨

```yaml
validation_levels:
  strict:
    critical_only: false
    warnings_enabled: true
    auto_fix: false

  standard:
    critical_only: false
    warnings_enabled: true
    auto_fix: true  # 안전한 수정만

  quick:
    critical_only: true
    warnings_enabled: false
    auto_fix: true
```

### 자동 수정 가능 항목

```yaml
auto_fixable:
  - javadoc_missing: "기본 템플릿 생성"
  - validation_missing: "@Valid 어노테이션 추가"
  - naming_convention: "camelCase/PascalCase 변환"

not_auto_fixable:
  - lombok_usage: "Plain Java 수동 변환 필요"
  - getter_chaining: "Tell Don't Ask 리팩토링 필요"
  - jpa_relations: "Long FK 전략 수동 마이그레이션"
  - transactional_boundary: "아키텍처 리팩토링 필요"
```

---

## 🔗 관련 문서

- 컨벤션 규칙: [docs/coding_convention/README.md](../../docs/coding_convention/README.md)
- GitHub Pages: https://ryu-qqq.github.io/claude-spring-standards/
- PRD 작성 가이드: [/create-prd](.claude/commands/create-prd.md)
- Task 분할 가이드: [/breakdown-prd](.claude/commands/breakdown-prd.md)
