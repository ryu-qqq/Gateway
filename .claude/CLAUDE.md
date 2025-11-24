# Spring Standards Project - Claude Code Configuration

이 프로젝트는 **Spring Boot 3.5.x + Java 21** 기반의 헥사고날 아키텍처 엔터프라이즈 표준 프로젝트입니다.

---

##  Kent Beck TDD + Tidy First 철학

이 프로젝트의 핵심 철학은 **테스트 주도 개발 (TDD)**과 **Tidy First (구조 먼저, 기능 나중)**입니다:

### Kent Beck의 TDD 사이클 (3단계)

```
Red (테스트 작성) → Green (최소 구현) → Refactor (코드 개선)
         ↓                ↓                  ↓
    실패하는 테스트     테스트 통과         구조 개선
         ↓                ↓                  ↓
     test: 커밋        feat: 커밋        struct: 커밋
```

### Tidy First 원칙 (Kent Beck)

**핵심 개념**: 코드 변경을 **구조적 변경(Structural)**과 **동작 변경(Behavioral)**으로 엄격히 분리

#### 1️⃣ Structural Changes (구조적 변경)
- **정의**: 동작을 변경하지 않고 코드 구조만 개선
- **예시**:
  - 변수/메서드 이름 변경 (Rename)
  - 메서드 추출 (Extract Method)
  - 코드 이동 (Move Code)
  - 중복 제거 (Remove Duplication)
- **검증**: 테스트 결과가 변경 전후 동일해야 함
- **커밋**: `struct:` prefix 사용
  ```bash
  git commit -m "struct: Email 검증 로직 메서드 추출"
  ```

#### 2️⃣ Behavioral Changes (동작 변경)
- **정의**: 실제 기능 추가 또는 변경
- **예시**:
  - 새 메서드/클래스 추가
  - 비즈니스 로직 변경
  - 알고리즘 개선
- **커밋**: `test:` (Red) 또는 `feat:` (Green) prefix 사용
  ```bash
  git commit -m "test: Email VO 검증 테스트 추가"
  git commit -m "feat: Email VO 구현 (RFC 5322 검증)"
  ```

#### 3️⃣ 철칙: 절대 섞지 말 것!

```
❌ 잘못된 예 (섞음):
- 메서드 이름 변경 + 새 기능 추가 (동시에)

✅ 올바른 예 (분리):
1. struct: 메서드 이름 변경 → 커밋
2. test: 새 기능 테스트 추가 → 커밋
3. feat: 새 기능 구현 → 커밋
```

### TDD + Tidy First 통합 워크플로우

```
1️⃣ Structural Changes 먼저 (필요 시)
   ├─ struct: 리네이밍, 메서드 추출 등
   ├─ 테스트 통과 확인
   └─ struct: 커밋

2️⃣ Red: 테스트 작성
   ├─ test: 실패하는 테스트 작성
   ├─ 컴파일 에러 확인
   └─ test: 커밋

3️⃣ Green: 최소 구현
   ├─ feat: 테스트 통과할 만큼만 구현
   ├─ 테스트 통과 확인
   └─ feat: 커밋

4️⃣ Refactor: 구조 개선 (필요 시)
   ├─ struct: 중복 제거, 명확성 개선
   ├─ 테스트 통과 확인 (동작 변경 없음)
   └─ struct: 커밋

5️⃣ 반복
```

### LangFuse 메트릭 추적

```
개발자: TDD + Tidy First 수행
    ↓
.git/hooks/post-commit (자동 트리거)
    ↓
log-to-langfuse.py (메트릭 수집)
    ├─ 커밋 타입 분류 (struct:/test:/feat:)
    ├─ TDD Phase 추적 (Red/Green/Refactor)
    ├─ 커밋 크기 측정
    └─ JSONL 로그 + LangFuse 업로드
```

### 핵심 메트릭

| 메트릭 | 측정 항목 | 목표 |
|--------|----------|------|
| **커밋 타입 비율** | struct:/test:/feat: 비율 | Balanced |
| **TDD 사이클 시간** | test: → feat: 평균 시간 | < 15분 |
| **커밋 크기** | 파일 변경 수, 라인 수 | 작을수록 좋음 (1-3 파일) |
| **Tidy First 준수율** | Structural 먼저 커밋 비율 | > 80% |
| **테스트 성공률** | 테스트 통과율 | > 95% |
| **ArchUnit 준수율** | 아키텍처 규칙 위반 | 0회 |

**핵심 원칙**:
- **작은 커밋**: 한 번에 한 가지만 변경
- **분리된 커밋**: Structural과 Behavioral 절대 섞지 않음
- **테스트 주도**: 테스트 먼저, 구현은 나중

### LangFuse 통합 (자동 메트릭 수집)

**목적**: TDD + Tidy First 워크플로우 메트릭 자동 수집

**자동 추적 이벤트**:
1. **tdd_commit**: Git 커밋 시
   - 커밋 타입 자동 분류 (`struct:`, `test:`, `feat:`)
   - TDD Phase 감지 (Red/Green/Refactor)
   - 커밋 크기 (파일 수, 라인 수)
   - Tidy First 준수 여부

2. **tdd_test**: 테스트 실행 시 (`./gradlew test`)
   - 테스트 성공/실패 수
   - 실행 시간

3. **archunit_check**: ArchUnit 실행 시
   - 아키텍처 규칙 위반 수

**커밋 타입 자동 감지**:
```bash
# Structural Changes
"struct: ..." → Phase: structural

# Red (Test First)
"test: ..." → Phase: red

# Green (Make it Work)
"feat: ..." 또는 "impl: ..." → Phase: green
```

**설정**:
```bash
# ~/.zshrc에 환경 변수 추가 (영구 설정)
echo 'export LANGFUSE_PUBLIC_KEY="pk-lf-..."' >> ~/.zshrc
echo 'export LANGFUSE_SECRET_KEY="sk-lf-..."' >> ~/.zshrc
echo 'export LANGFUSE_HOST="https://us.cloud.langfuse.com"' >> ~/.zshrc
source ~/.zshrc
```

**로그 파일 위치**:
- `~/.claude/logs/tdd-cycle.jsonl` (로컬 JSONL 로그)
- LangFuse Cloud (환경 변수 설정 시)

**대시보드 확인**: https://us.cloud.langfuse.com

---

## 📚 코딩 규칙 (docs/coding_convention/)

### 레이어별 규칙 구조

```
docs/coding_convention/
├── 00-project-setup/  (2개 규칙)
│   ├── multi-module-structure.md
│   └── version-management.md
│
├── 01-adapter-in-layer/rest-api/  (22개 규칙)
│   ├── controller/  (4개)
│   │   ├── controller-guide.md
│   │   ├── controller-test-guide.md
│   │   ├── controller-test-restdocs-guide.md
│   │   └── controller-archunit.md
│   ├── dto/
│   │   ├── command/  (3개: guide, test-guide, archunit)
│   │   ├── query/    (3개: guide, test-guide, archunit)
│   │   └── response/ (3개: guide, test-guide, archunit)
│   ├── error/  (2개)
│   │   ├── error-handling-strategy.md
│   │   └── error-mapper-implementation-guide.md
│   ├── mapper/  (3개: guide, test-guide, archunit)
│   ├── config/  (1개: endpoint-properties-guide)
│   └── rest-api-guide.md
│
├── 02-domain-layer/  (12개 규칙)
│   ├── aggregate/  (3개: guide, test-guide, archunit)
│   ├── exception/  (3개: guide, test-guide, archunit-guide)
│   ├── vo/  (3개: guide, test-guide, archunit)
│   ├── event/  (디렉토리만 존재, 파일 없음)
│   └── domain-guide.md
│
├── 03-application-layer/  (26개 규칙)
│   ├── assembler/  (3개: guide, test-guide, archunit)
│   ├── dto/
│   │   ├── command/  (1개: command-dto-guide)
│   │   ├── query/    (1개: query-dto-guide)
│   │   ├── response/ (1개: response-dto-guide)
│   │   ├── dto-record-archunit.md
│   │   └── 06_archunit-dto-record-rules.md
│   ├── facade/  (2개: guide, test-guide)
│   ├── manager/  (2개: transaction-manager-guide, test-guide)
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/  (2개: guide, archunit)
│   │   │   └── query/    (2개: guide, archunit)
│   │   └── out/
│   │       ├── command/  (2개: guide, archunit)
│   │       └── query/    (2개: guide, archunit)
│   ├── listener/  (디렉토리만 존재)
│   ├── scheduler/  (디렉토리만 존재)
│   ├── service/  (디렉토리만 존재)
│   └── application-guide.md
│
├── 04-persistence-layer/  (23개 규칙)
│   ├── mysql/  (18개)
│   │   ├── adapter/
│   │   │   ├── command/  (3개: guide, test-guide, archunit)
│   │   │   └── query/    (7개)
│   │   │       ├── query-adapter-guide.md
│   │   │       ├── query-adapter-test-guide.md
│   │   │       ├── query-adapter-integration-testing.md
│   │   │       ├── query-adapter-archunit.md
│   │   │       ├── lock-query-adapter-guide.md
│   │   │       ├── lock-query-adapter-test-guide.md
│   │   │       └── lock-query-adapter-archunit.md
│   │   ├── config/  (2개: flyway-testing, hikaricp-configuration)
│   │   ├── entity/  (3개: guide, test-guide, archunit)
│   │   ├── mapper/  (3개: guide, test-guide, archunit)
│   │   ├── repository/  (5개)
│   │   │   ├── jpa-repository-guide.md
│   │   │   ├── jpa-repository-archunit.md
│   │   │   ├── querydsl-repository-guide.md
│   │   │   ├── querydsl-repository-test-guide.md
│   │   │   └── querydsl-repository-archunit.md
│   │   └── persistence-mysql-guide.md
│   └── redis/  (5개)
│       ├── adapter/  (3개: guide, test-guide, archunit)
│       ├── config/  (1개: cache-configuration)
│       └── persistence-redis-guide.md
│
└── 05-testing/  (3개 규칙)
    ├── integration-testing/  (1개: 01_integration-testing-overview)
    └── test-fixtures/  (2개: guide, archunit)
```

**총 88개 규칙** (README.md 포함)

---

## 🏗️ 프로젝트 핵심 원칙

### 1. 아키텍처 패턴
- **헥사고날 아키텍처** (Ports & Adapters) - 의존성 역전
- **도메인 주도 설계** (DDD) - Aggregate 중심 설계
- **CQRS** - Command/Query 분리

### 2. 코드 품질 규칙 (Zero-Tolerance)
- **Lombok 금지** - Plain Java 사용 (Domain layer에서 특히 엄격)
- **Law of Demeter** - Getter 체이닝 금지 (`order.getCustomer().getAddress()` ❌)
- **Long FK 전략** - JPA 관계 어노테이션 금지, Long userId 사용
- **Transaction 경계** - `@Transactional` 내 외부 API 호출 절대 금지

### 3. Spring 프록시 제약사항 (중요!)
⚠️ **다음 경우 `@Transactional`이 작동하지 않습니다:**
- Private 메서드
- Final 클래스/메서드
- 같은 클래스 내부 호출 (`this.method()`)

---

## 🔧 자동화 시스템

### 1. TDD Workflow Tracking

**위치**: `.claude/hooks/track-tdd-cycle.sh`, `.claude/scripts/log-to-langfuse.py`

**목적**: Kent Beck TDD 사이클 자동 추적 및 메트릭 수집

#### 작동 원리

```
개발자: TDD 사이클 수행
    ↓
Red: 테스트 작성 (실패하는 테스트)
    ↓
Green: 최소 구현 (테스트 통과)
    ↓
Refactor: 코드 개선
    ↓
Commit: 작은 변경 커밋
    ↓
track-tdd-cycle.sh (자동 감지)
    ├─ git commit 감지 → TDD Phase 분석
    ├─ ./gradlew test 감지 → 테스트 결과 파싱
    └─ ArchUnit 감지 → 아키텍처 규칙 검증
         ↓
log-to-langfuse.py (메트릭 저장)
    ├─ JSONL 로그 (항상 작동)
    └─ LangFuse 업로드 (선택적)
         ↓
LangFuse Dashboard (분석)
    ├─ TDD 사이클 시간 분석
    ├─ 커밋 크기 추적
    ├─ 테스트 성공률 모니터링
    └─ 리팩토링 빈도 분석
```

#### 메트릭 수집

**자동 수집되는 메트릭**:
- **TDD Phase**: 커밋 메시지로 Red/Green/Refactor 자동 분류
- **Commit Size**: 변경된 파일 수, 라인 수
- **Test Results**: 통과/실패 테스트 수, 실행 시간
- **ArchUnit**: 아키텍처 규칙 위반 수

**로그 위치**:
- `~/.claude/logs/tdd-cycle.jsonl` (항상 저장)
- LangFuse Cloud (환경 변수 설정 시)

### 2. Kent Beck TDD 커맨드 (/kb)

**목적**: Plan 파일 기반으로 짧은 TDD 사이클(5-15분)을 실행하는 Layer별 커맨드

**핵심 개념**:
- **Plan 파일 기반**: `docs/prd/plans/{ISSUE-KEY}-{layer}-plan.md` 파일에서 다음 테스트 읽기
- **TDD 3단계**: Red (test:) → Green (feat:) → Refactor (struct:)
- **Tidy First 준수**: Structural 변경은 항상 별도 커밋
- **작은 커밋**: 한 번에 한 가지만 (test:, feat:, struct: 분리)
- **Zero-Tolerance 자동 준수**: 각 레이어별 규칙 자동 검증

**Layer별 TDD 커맨드**:
```bash
# Domain Layer TDD
/kb/domain/go          # Plan 파일에서 다음 테스트 실행
/kb/domain/red         # Red: test: 테스트 작성 → 실패 확인 → 커밋
/kb/domain/green       # Green: feat: 최소 구현 → 테스트 통과 → 커밋
/kb/domain/refactor    # Refactor: struct: 구조 개선 → 커밋

# Application Layer TDD
/kb/application/go     # UseCase TDD 실행
/kb/application/red    # test: 커밋 (Transaction 경계 주의)
/kb/application/green  # feat: 커밋 (최소 구현)
/kb/application/refactor  # struct: 커밋 (구조 개선)

# Persistence Layer TDD
/kb/persistence/go     # Repository/Adapter TDD 실행
/kb/persistence/red    # test: 커밋 (Long FK 전략 준수)
/kb/persistence/green  # feat: 커밋 (QueryDSL DTO Projection)
/kb/persistence/refactor  # struct: 커밋

# REST API Layer TDD
/kb/rest-api/go        # Controller TDD 실행
/kb/rest-api/red       # test: 커밋 (MockMvc 테스트)
/kb/rest-api/green     # feat: 커밋 (RESTful 설계)
/kb/rest-api/refactor  # struct: 커밋

# Integration Tests
/kb/integration/go     # E2E 테스트 실행
```

**워크플로우 예시**:
```bash
# 1. Plan 파일 생성 (PRD → Plan)
docs/prd/plans/MEMBER-001-domain-plan.md

# 2. TDD 사이클 실행
/kb/domain/go
→ Plan 파일 읽기 → 다음 테스트 찾기
→ Red: test: 테스트 작성 → 실패 확인 → test: 커밋
→ Green: feat: 최소 구현 → 통과 확인 → feat: 커밋
→ Refactor: struct: 구조 개선 → 통과 확인 → struct: 커밋
→ Plan 파일에 완료 표시

# 3. 다음 테스트로 이동
/kb/domain/go (반복)
```

**커밋 메시지 규칙**:
- `test:` - 실패하는 테스트 추가 (Red Phase)
- `feat:` - 테스트 통과 구현 (Green Phase)
- `struct:` - 구조 개선 (Refactor Phase, 동작 변경 없음)
- `fix:` - 버그 수정 (test: + feat: 조합)

**Layer별 Zero-Tolerance 규칙**:
- **Domain**: Lombok 금지, Law of Demeter, Tell Don't Ask
- **Application**: Transaction 경계, CQRS 분리, Assembler 사용
- **Persistence**: Long FK 전략, QueryDSL DTO Projection, Lombok 금지
- **REST API**: RESTful 설계, DTO 패턴, Validation 필수

### 3. 실시간 메트릭 모니터링

**JSONL 로그 확인**:
```bash
# TDD 사이클 로그 실시간 모니터링
tail -f ~/.claude/logs/tdd-cycle.jsonl

# 출력 예시:
# {"timestamp":"2025-11-13T12:34:56Z","event_type":"tdd_commit","data":{"project":"claude-spring-standards","commit_hash":"a1b2c3d","commit_msg":"test: Order 생성 테스트 추가","tdd_phase":"red","files_changed":"2 files changed","lines_changed":"45 insertions","timestamp":"2025-11-13T12:34:56Z"}}
# {"timestamp":"2025-11-13T12:38:12Z","event_type":"tdd_test","data":{"project":"claude-spring-standards","test_status":"failed","tests_passed":"0","tests_failed":"1","duration_seconds":"3","timestamp":"2025-11-13T12:38:12Z"}}
# {"timestamp":"2025-11-13T12:45:23Z","event_type":"tdd_commit","data":{"project":"claude-spring-standards","commit_hash":"d4e5f6g","commit_msg":"impl: Order 생성 로직 구현","tdd_phase":"green","files_changed":"1 file changed","lines_changed":"28 insertions","timestamp":"2025-11-13T12:45:23Z"}}
```

**LangFuse 대시보드** (환경 변수 설정 시):
- TDD 사이클 시간 차트
- 커밋 크기 분포
- 테스트 성공률 트렌드
- Phase별 시간 소요 분석

### 5. Git Pre-commit Hooks (별도 시스템)

**위치**: `hooks/pre-commit`, `hooks/validators/`

- **트랜잭션 경계 검증**: `@Transactional` 내 외부 API 호출 차단
- **프록시 제약사항 검증**: Private/Final 메서드 `@Transactional` 차단
- **최종 안전망 역할**: 커밋 시 강제 검증

### 6. ArchUnit Tests

**위치**: `application/src/test/java/com/company/template/architecture/`

- **아키텍처 규칙 자동 검증**: 레이어 의존성, 네이밍 규칙
- **빌드 시 자동 실행**: 위반 시 빌드 실패

---

## 🎯 개발 워크플로우 (Kent Beck TDD + Tidy First)

### 1. TDD 사이클 워크플로우

```bash
# 🔴 Red Phase: 실패하는 테스트 작성
vim domain/src/test/java/.../EmailTest.java
# → 테스트 작성
./gradlew test
# → 컴파일 에러 또는 테스트 실패 확인
git add .
git commit -m "test: Email VO 검증 테스트 추가"
# → post-commit hook → LangFuse (Phase: red)

# 🟢 Green Phase: 최소 구현
vim domain/src/main/java/.../Email.java
# → 테스트 통과할 만큼만 구현 (최소한의 코드)
./gradlew test
# → 테스트 통과 확인
git add .
git commit -m "feat: Email VO 구현 (RFC 5322 검증)"
# → post-commit hook → LangFuse (Phase: green)

# ♻️ Refactor Phase: 구조 개선 (필요 시)
vim domain/src/main/java/.../Email.java
# → 중복 제거, 메서드 추출, 이름 변경 등
# → 동작 변경 없음! 테스트 결과 동일해야 함
./gradlew test
# → 테스트 여전히 통과 확인
git add .
git commit -m "struct: Email 검증 로직 메서드 추출"
# → post-commit hook → LangFuse (Phase: structural)

# 결과: LangFuse에 3개 커밋 메트릭 자동 수집
# - test: (Red)
# - feat: (Green)
# - struct: (Refactor)
```

### 2. Tidy First 실전 예시

```bash
# 시나리오: Email VO에 도메인 검증 추가하기

# ❌ 잘못된 방법 (섞음)
git commit -m "feat: 도메인 검증 추가 및 변수명 변경"
# → Structural(변수명)과 Behavioral(검증) 섞음

# ✅ 올바른 방법 (분리)

# 1️⃣ Structural 먼저
vim Email.java
# → 변수명 value → emailAddress 변경
./gradlew test  # 통과
git commit -m "struct: Email 변수명 명확화 (value → emailAddress)"

# 2️⃣ Red
vim EmailTest.java
# → 도메인 검증 테스트 추가
./gradlew test  # 실패
git commit -m "test: Email 도메인 형식 검증 테스트 추가"

# 3️⃣ Green
vim Email.java
# → 도메인 검증 로직 추가
./gradlew test  # 통과
git commit -m "feat: Email 도메인 형식 검증 구현"
```

### 2. 검증 워크플로우

```bash
# 특정 파일 검증
/validate-domain domain/src/main/java/.../Order.java

# 전체 프로젝트 검증
/validate-architecture

# ArchUnit 실행 (빌드 시 자동)
./gradlew test
# → track-tdd-cycle.sh가 ArchUnit 결과 자동 수집
```

### 3. 메트릭 분석 워크플로우

```bash
# JSONL 로그 실시간 확인
tail -f ~/.claude/logs/tdd-cycle.jsonl

# 예시 출력:
# {"event_type":"tdd_commit","data":{"commit_msg":"test: Email 검증 테스트 추가","tdd_phase":"red",...}}
# {"event_type":"tdd_commit","data":{"commit_msg":"feat: Email VO 구현","tdd_phase":"green",...}}
# {"event_type":"tdd_commit","data":{"commit_msg":"struct: 검증 로직 메서드 추출","tdd_phase":"structural",...}}

# LangFuse 대시보드 (환경 변수 설정 시)
# → https://us.cloud.langfuse.com
# → 커밋 타입 비율 (struct:/test:/feat:)
# → TDD 사이클 시간 (test: → feat: 평균)
# → Tidy First 준수율
```

### 4. 커밋 규칙 요약

| Prefix | 용도 | Phase | 예시 |
|--------|------|-------|------|
| `test:` | 실패하는 테스트 추가 | Red | `test: Email VO 검증 테스트 추가` |
| `feat:` | 테스트 통과 구현 | Green | `feat: Email VO 구현 (RFC 5322)` |
| `struct:` | 구조 개선 (동작 동일) | Refactor | `struct: Email 검증 로직 메서드 추출` |
| `fix:` | 버그 수정 | - | `fix: Email null 처리 누락 수정` |
| `chore:` | 빌드/설정 변경 | - | `chore: Gradle 버전 업데이트` |

**핵심 원칙**:
- ✅ 한 커밋에는 하나의 타입만
- ✅ Structural과 Behavioral 절대 섞지 않기
- ✅ 작은 커밋 (1-3 파일)
- ✅ 모든 테스트 통과 시에만 커밋

---
