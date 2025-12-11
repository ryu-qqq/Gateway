# Git Hooks (Pre-Commit)

Git hooks를 사용하여 코드 품질 검증을 자동화합니다.

---

## 개요

이 프로젝트의 Git hooks는 **코드 품질 안전망**을 제공합니다:

### Pre-Commit Hook (코드 품질 검증)
- **ArchUnit 테스트** - 아키텍처 규칙 검증 (Zero-Tolerance 포함)
- **Gradle Quality Checks** - Checkstyle, PMD, SpotBugs

> **Note**: 이전 버전의 13개 validator 스크립트는 제거되고, ArchUnit으로 통합되었습니다.

---

## 설치

### 1. 자동 설치 (권장)

```bash
# 프로젝트 루트에서 실행
./scripts/setup-hooks.sh

# 또는 수동 설치:
ln -sf ../../config/hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

### 2. 설치 확인

```bash
# Hooks가 제대로 링크되었는지 확인
ls -la .git/hooks/pre-commit

# 출력 예시:
# lrwxr-xr-x  1 user  staff  28 Nov  4 16:00 .git/hooks/pre-commit -> ../../config/hooks/pre-commit
```

---

## 검증 규칙

### 1. ArchUnit 테스트

다음 아키텍처 규칙을 자동으로 검증합니다:

#### Zero-Tolerance 규칙 (절대 위반 불가)
- **Lombok 금지** - Domain, JPA Entity, Orchestration
- **Transaction Boundary** - `@Transactional` 내 외부 API 호출 금지
- **Spring 프록시 제약** - `@Transactional`은 public만, private/final 금지
- **Long FK 전략** - `@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany` 금지
- **Orchestration Pattern** - executeInternal() @Async 필수, Command Record 패턴 등

#### Layer 의존성 규칙
- **Domain Layer** - 외부 프레임워크 의존성 금지 (Spring, JPA 등)
- **Application Layer** - Domain만 의존 가능, Adapter 직접 의존 금지
- **Hexagonal Architecture** - 의존성 방향 준수

#### 네이밍 규칙
- **UseCase 인터페이스** - `*UseCase` 또는 `*QueryService`
- **Out Port** - `*CommandOutPort`, `*QueryOutPort`
- **JPA Entity** - `*JpaEntity`, BaseAuditEntity 상속 필수

**테스트 파일 위치:**
- `ZeroToleranceArchitectureTest.java` - Zero-Tolerance 규칙 통합
- `DomainLayerRulesTest.java` - Domain Layer 규칙
- `ApplicationLayerRulesTest.java` - Application Layer 규칙
- `DomainObjectConventionTest.java` - Domain Object 컨벤션
- `JpaEntityConventionTest.java` - JPA Entity 컨벤션
- `OrchestrationConventionTest.java` - Orchestration Pattern 규칙
- `HexagonalArchitectureTest.java` - 헥사고날 아키텍처 규칙

### 2. Gradle Quality Checks

다음 품질 도구를 실행합니다:

- **Checkstyle** - 코드 스타일 검증
  - 설정: `config/checkstyle/checkstyle.xml`
- **PMD** - 코드 품질 검증
  - 설정: `config/pmd/pmd-ruleset.xml`
- **SpotBugs** - 잠재적 버그 검증
  - 설정: `config/spotbugs/spotbugs-exclude.xml`
- **JaCoCo** - 코드 커버리지 측정
  - 커버리지 리포트: `build/reports/jacoco/test/html/index.html`

---

## 작동 방식

```bash
git add .
git commit -m "feat: Add new feature"

# Pre-commit hook 자동 실행:
# 1. Staged 파일 확인
# 2. Java 파일이 있으면 검증 시작
# 3. ArchUnit 테스트 실행 (아키텍처 규칙)
# 4. Gradle check 실행 (품질 검증)
# 5. 모든 검증 통과 시 커밋 허용
# 6. 실패 시 커밋 차단, 에러 메시지 출력
```

### 성공 예시

```
ℹ️  Checking staged changes...
ℹ️  Found 3 Java file(s) changed

ℹ️  Running ArchUnit architecture tests...
✅ ArchUnit tests passed

ℹ️  Running Gradle quality checks (Checkstyle, PMD, SpotBugs)...
✅ Gradle quality checks passed

========================================
✅ All validations passed! ✨
========================================

  ✅ ArchUnit architecture tests: PASSED
  ✅ Gradle quality checks: PASSED
```

### 실패 예시

```
ℹ️  Running ArchUnit architecture tests...
❌ ArchUnit tests failed - architecture violations detected

  ArchUnit tests enforce:
  • Lombok prohibition (Domain, JPA Entity, Orchestration)
  • Transaction boundaries (@Transactional rules)
  • Spring proxy constraints (public/private/final)
  • Long FK strategy (JPA relationship annotations)
  • Orchestration pattern rules
  • Layer dependency rules (Hexagonal Architecture)

  Run './gradlew test' to see detailed violations

========================================
❌ Validation failed - commit blocked
========================================

Fix the issues above and try again.
Or use 'git commit --no-verify' to skip (NOT RECOMMENDED)
```

---

## 검증 우회 (비권장)

긴급 상황에서만 사용하세요:

```bash
# 검증을 건너뛰고 커밋
git commit --no-verify -m "emergency fix"

# 또는
git commit -n -m "emergency fix"
```

**⚠️ 경고**: 검증을 우회하면 Zero-Tolerance 규칙을 위반한 코드가 커밋될 수 있습니다.

---

## 로컬에서 검증 실행

커밋 전에 미리 검증을 실행할 수 있습니다:

```bash
# ArchUnit 테스트만 실행
./gradlew test --tests "*ArchitectureTest" --tests "*ConventionTest"

# Gradle 품질 검증만 실행
./gradlew check

# 전체 검증 실행 (ArchUnit + Gradle)
./gradlew test check
```

---

## 검증 리포트

### ArchUnit 리포트

```bash
# 테스트 결과 위치
build/reports/tests/test/index.html

# 브라우저로 열기
open build/reports/tests/test/index.html  # macOS
xdg-open build/reports/tests/test/index.html  # Linux
```

### Gradle Quality 리포트

```bash
# Checkstyle 리포트
build/reports/checkstyle/main.html

# PMD 리포트
build/reports/pmd/main.html

# SpotBugs 리포트
build/reports/spotbugs/main.html

# JaCoCo 커버리지 리포트
build/reports/jacoco/test/html/index.html
```

---

## 트러블슈팅

### 문제: pre-commit hook이 실행되지 않음

```bash
# 1. 심볼릭 링크 확인
ls -la .git/hooks/pre-commit

# 2. 실행 권한 확인
chmod +x .git/hooks/pre-commit
chmod +x hooks/pre-commit

# 3. 재설치
rm .git/hooks/pre-commit
ln -sf ../../hooks/pre-commit .git/hooks/pre-commit
```

### 문제: ArchUnit 테스트 실패

```bash
# 상세 로그 확인
./gradlew test --tests "*ArchitectureTest" --tests "*ConventionTest" --info

# 특정 테스트만 실행
./gradlew test --tests "ZeroToleranceArchitectureTest"
```

### 문제: Gradle check 실패

```bash
# 상세 리포트 확인
./gradlew check --info

# 특정 도구만 실행
./gradlew checkstyleMain  # Checkstyle
./gradlew pmdMain         # PMD
./gradlew spotbugsMain    # SpotBugs
```

---

## Git Hook vs Claude Hook 비교

> **중요**: 이것은 **Git Hooks**입니다. **Claude Code 동적 훅**과는 다릅니다.

| 항목 | Git Hooks (`config/hooks/`) | Claude Hooks (`.claude/hooks/`) |
|------|----------------------|----------------------------------|
| **실행 시점** | `git commit` 실행 시 | Claude가 코드 생성/수정 시 |
| **실행 주체** | Git (개발자 로컬) | Claude Code AI |
| **목적** | 잘못된 코드 커밋 차단 | AI 코드 생성 가이드 제공 |
| **검증 방식** | ArchUnit + Gradle | 프롬프트 주입 + 실시간 검증 |
| **차단 여부** | ❌ pre-commit 실패 시 커밋 차단 | ⚠️ 경고만 제공 (차단 안 함) |

### 실행 흐름 비교

**Git Hooks 흐름**:
```
개발자가 코드 작성
    ↓
git add .
    ↓
git commit -m "..."
    ↓
config/hooks/pre-commit 실행  ← 코드 검증 (blocking)
    ↓
검증 통과 → 커밋 완료
```

**Claude Hooks 흐름**:
```
Claude에게 코드 요청
    ↓
.claude/hooks/user-prompt-submit.sh 실행  ← 요청 전 가이드 주입
    ↓
Claude가 코드 생성
    ↓
.claude/hooks/after-tool-use.sh 실행  ← 생성 후 즉시 검증
    ↓
경고 발견 → 사용자에게 알림 (코드는 생성됨)
문제 없음 → 완료
```

**Best Practice**: 두 시스템을 모두 활성화하여 이중 안전망 구축

---

## 변경 이력

### 2025-12-05 (v3.0.0) - Remove Metrics Tracking

**Major Changes:**
- ✅ Post-commit hook 제거 (TDD 메트릭 수집 제거)
- ✅ LangFuse 및 JSONL 로깅 제거
- ✅ Pre-commit hook만 유지 (코드 품질 검증)

### 2025-11-04 (v2.0.0) - Simplified Architecture

**Major Changes:**
- ✅ 13개 validator 스크립트 제거 (1,929줄 → 0줄)
- ✅ ArchUnit 테스트로 통합
- ✅ `ZeroToleranceArchitectureTest` 추가 (Transaction Boundary, Law of Demeter 등)
- ✅ pre-commit hook 단순화 (294줄 → 152줄, 48% 감소)

**Rationale:**
- 유지보수성 향상: Shell 스크립트 → Java ArchUnit (IDE 지원, 타입 안전성)
- 단일 진실 공급원: 분산된 validator → 중앙화된 ArchUnit
- 성능 개선: 모듈별 검증 → 통합 테스트 (병렬 실행 가능)

**Removed:**
- `hooks/validators/` 디렉토리 전체 (13개 스크립트)

**Added:**
- `ZeroToleranceArchitectureTest.java` - Zero-Tolerance 규칙 통합 테스트

### 2025-10-30 (v1.0.0) - Initial Release

- ✅ 13개 validator 스크립트 시스템
- ✅ Git hooks 자동화
- ✅ Gradle 품질 검증 통합

---

## 참고

- **ArchUnit 공식 문서**: https://www.archunit.org/userguide/html/000_Index.html
- **프로젝트 코딩 컨벤션**: [docs/coding_convention/](../docs/coding_convention/)
- **Zero-Tolerance 규칙**: [.claude/CLAUDE.md](../.claude/CLAUDE.md#🚨-zero-tolerance-규칙)
- **Gradle 품질 도구 설정**: [config/](../config/)
