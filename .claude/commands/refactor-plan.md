# Refactoring Plan Command

기존 프로젝트를 Claude Spring Standards 컨벤션에 맞게 리팩토링하는 계획을 수립합니다.

---

## 명령어

```
/refactor-plan <layer>
```

**⚠️ layer 필수 지정** (기본값 없음):
- `domain` - Domain Layer 분석
- `application` - Application Layer 분석
- `persistence` - Persistence Layer 분석 (MySQL)
- `persistence-redis` - Redis Layer 분석
- `rest-api` - REST API Layer 분석

> **참고**: 전체 프로젝트 분석이 필요한 경우 각 레이어를 순차적으로 실행하세요.

---

## 실행 프로세스 (이중 검증)

```
┌─────────────────────────────────────────────────────────────┐
│           Refactoring Plan Process (이중 검증)              │
├─────────────────────────────────────────────────────────────┤
│  1️⃣ Serena Memory 규칙 로드                                │
│     └─ 해당 레이어의 모든 규칙 문서 로드                     │
│                                                              │
│  2️⃣ Serena Memory 기반 규칙 검증                           │
│     └─ 문서에 정의된 모든 규칙 자동 검증                     │
│                                                              │
│  3️⃣ ArchUnit 테스트 실행                                    │
│     └─ 해당 레이어 ArchUnit 테스트 자동 실행                 │
│                                                              │
│  4️⃣ 이중 검증 결과 종합                                     │
│     └─ Serena 위반 + ArchUnit 실패 = 전체 위반 목록          │
│                                                              │
│  5️⃣ 리팩토링 계획 생성 + Serena Memory 저장                 │
│     └─ refactor-plan-{layer}-{timestamp} 저장               │
└─────────────────────────────────────────────────────────────┘
```

---

## 1️⃣ 레이어별 Serena Memory 규칙 매핑

### Domain Layer
```yaml
serena_memories:
  - domain-rules-01-aggregate
  - domain-rules-02-value-object
  - domain-rules-03-exception
  - domain-rules-04-event
  - domain-rules-05-criteria
  - domain-rules-06-common
  - domain-rules-07-testing
  - convention-domain-layer-validation-rules

archunit_tests:
  - "**/domain/architecture/*ArchTest*"
```

### Application Layer
```yaml
serena_memories:
  - app-rules-01-service
  - app-rules-02-dto
  - app-rules-03-port
  - app-rules-04-manager-facade
  - app-rules-05-factory-assembler
  - app-rules-06-event-scheduler
  - app-rules-07-testing
  - convention-application-layer-validation-rules

archunit_tests:
  - "**/application/architecture/*ArchTest*"
```

### Persistence Layer (MySQL)
```yaml
serena_memories:
  - persistence-rules-01-entity
  - persistence-rules-02-jpa-repository
  - persistence-rules-03-querydsl-repository
  - persistence-rules-04-admin-querydsl-repository
  - persistence-rules-05-lock-repository
  - persistence-rules-06-mapper
  - persistence-rules-07-command-adapter
  - persistence-rules-08-query-adapter
  - persistence-rules-09-admin-query-adapter
  - persistence-rules-10-lock-query-adapter
  - persistence-rules-11-testing
  - convention-persistence-mysql-validation-rules

archunit_tests:
  - "**/persistence/architecture/*ArchTest*"
```

### Persistence Layer (Redis)
```yaml
serena_memories:
  - redis-rules-01-cache-adapter
  - redis-rules-02-lock-adapter
  - redis-rules-03-config
  - redis-rules-04-testing
  - convention-persistence-redis-validation-rules

archunit_tests:
  - "**/persistence/redis/architecture/*ArchTest*"
```

### REST API Layer
```yaml
serena_memories:
  - rest-api-rules-01-controller
  - rest-api-rules-02-command-dto
  - rest-api-rules-03-query-dto
  - rest-api-rules-04-response-dto
  - rest-api-rules-05-mapper
  - rest-api-rules-06-error
  - rest-api-rules-07-security
  - rest-api-rules-08-openapi
  - rest-api-rules-09-testing
  - convention-rest-api-layer-validation-rules

archunit_tests:
  - "**/rest/architecture/*ArchTest*"
```

---

## 2️⃣ 실행 단계

### Step 1: Serena Memory 규칙 로드

```markdown
## 실행 지침

1. Serena MCP `read_memory` 호출하여 해당 레이어의 모든 규칙 로드
2. 각 규칙 문서에서 Zero-Tolerance 항목 추출
3. 검증 대상 패턴 목록 생성
```

### Step 2: Serena Memory 기반 규칙 검증

```markdown
## 검증 방법

각 규칙 문서의 "체크리스트" 또는 "Do/Don't" 섹션을 기준으로:

1. `search_for_pattern` 사용하여 위반 코드 검색
2. `find_symbol` 사용하여 클래스/메서드 구조 검증
3. 위반 파일 및 라인 번호 수집
```

### Step 3: ArchUnit 테스트 실행

```bash
# Domain Layer
./gradlew :domain:test --tests "*ArchTest*" -x jacocoTestCoverageVerification

# Application Layer
./gradlew :application:test --tests "*ArchTest*" -x jacocoTestCoverageVerification

# Persistence Layer (MySQL)
./gradlew :adapter-out:persistence-mysql:test --tests "*ArchTest*" -x jacocoTestCoverageVerification

# Persistence Layer (Redis)
./gradlew :adapter-out:persistence-redis:test --tests "*ArchTest*" -x jacocoTestCoverageVerification

# REST API Layer
./gradlew :adapter-in:rest-api:test --tests "*ArchTest*" -x jacocoTestCoverageVerification
```

### Step 4: 이중 검증 결과 종합

```markdown
## 결과 종합 형식

### 🔴 Critical (즉시 수정 필요)

#### Serena Memory 규칙 위반
| 규칙 | 위반 파일 | 라인 | 설명 |
|------|----------|------|------|
| domain-rules-01-aggregate | Order.java | 45 | Lombok @Data 사용 |

#### ArchUnit 테스트 실패
| 테스트 클래스 | 테스트 메서드 | 실패 원인 |
|--------------|--------------|----------|
| AggregateArchTest | aggregate_MustNotUseLombok | Order.java uses @Data |

### 🟡 Important (빠른 수정 권장)
[동일 형식]

### 🟢 Recommended (점진적 개선)
[동일 형식]
```

---

## 3️⃣ 산출물

### Serena Memory 저장 형식

```markdown
# Refactoring Plan: {layer}

## 메타 정보
- 생성일: {timestamp}
- 대상 레이어: {layer}
- Serena Memory 규칙 수: {count}개
- ArchUnit 테스트 수: {count}개

## 이중 검증 결과

### Serena Memory 규칙 검증
- 총 규칙 수: {count}
- 통과: {count}
- 실패: {count}

### ArchUnit 테스트 검증
- 총 테스트 수: {count}
- 통과: {count}
- 실패: {count}

## 위반 상세

### 🔴 Critical 위반: {count}개
{상세 목록}

### 🟡 Important 위반: {count}개
{상세 목록}

### 🟢 Recommended 위반: {count}개
{상세 목록}

## 리팩토링 우선순위

| 순위 | 항목 | 영향 파일 수 | 검증 방식 |
|------|------|-------------|----------|
| 1 | {item} | {count} | Serena + ArchUnit |
| 2 | {item} | {count} | Serena only |
| 3 | {item} | {count} | ArchUnit only |

## 권장 수정 순서
1. {step1}
2. {step2}
3. {step3}
```

---

## 4️⃣ 실행 예시

```bash
# ❌ 레이어 없이 실행 - 오류 발생
/refactor-plan
# Error: layer 파라미터가 필수입니다. (domain|application|persistence|persistence-redis|rest-api)

# ✅ Domain Layer 분석
/refactor-plan domain

# ✅ Persistence Layer (MySQL) 분석
/refactor-plan persistence

# ✅ REST API Layer 분석
/refactor-plan rest-api
```

---

## 5️⃣ 연계 워크플로우

```
/refactor-plan {layer}
    ↓
이중 검증 결과 확인
    ↓
위반 항목 승인
    ↓
리팩토링 실행
    ├─ /kb/domain/go (Domain 리팩토링)
    ├─ /kb/application/go (Application 리팩토링)
    ├─ /kb/persistence/go (Persistence 리팩토링)
    └─ /kb/rest-api/go (REST API 리팩토링)
    ↓
재검증 (다시 /refactor-plan 실행)
    ↓
완료 (위반 0개)
```

---

## 6️⃣ Zero-Tolerance 규칙 (레이어 공통)

모든 레이어에서 다음 항목은 **무조건 검증**됩니다:

| 규칙 | 검증 방식 | 검색 패턴 |
|------|----------|----------|
| Lombok 금지 | Serena + ArchUnit | `import lombok.` |
| Law of Demeter | Serena | `\.get.*\(\)\.get` |
| Transaction 경계 | Serena | `@Transactional` 내 외부 호출 |
| JPA 관계 금지 | Serena + ArchUnit | `@OneToMany`, `@ManyToOne` |
| Javadoc 필수 | Serena | public 클래스/메서드 |

---

## 참조 문서

- **Serena Memory 규칙**: `list_memories()` 참조
- **ArchUnit 테스트**: 각 모듈 `src/test/java/.../architecture/`
- **Zero-Tolerance 규칙**: `.claude/CLAUDE.md`
