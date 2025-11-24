# /breakdown-prd - PRD를 레이어별 Task로 분할

**목적**: PRD 문서를 헥사고날 아키텍처 레이어별 Jira Issue로 분할

**사용법**:
```bash
/breakdown-prd <prd-file-path>
/breakdown-prd docs/prd/member-management.md
```

---

## 📋 작업 순서

### 1. PRD 파일 읽기 및 분석

**필수 입력**:
- PRD 파일 경로 (예: `docs/prd/member-management.md`)

**추출할 정보**:
- Epic 이름 (예: "회원 관리 시스템")
- Issue Key Prefix (예: "MEMBER")
- 도메인 요구사항
- 비즈니스 로직 요구사항
- 데이터 저장 요구사항
- API 요구사항
- 통합 테스트 시나리오

### 2. 레이어별 Task 분할 전략

**헥사고날 아키텍처 레이어 순서** (의존성 역방향):

```
1. Domain Layer (가장 안쪽)
   └─ Issue: {PREFIX}-001
   └─ 파일: docs/prd/tasks/{PREFIX}-001.md
   └─ 브랜치: feature/{PREFIX}-001-domain

2. Application Layer
   └─ Issue: {PREFIX}-002
   └─ 파일: docs/prd/tasks/{PREFIX}-002.md
   └─ 브랜치: feature/{PREFIX}-002-application

3. Persistence Layer (Adapter-Out)
   └─ Issue: {PREFIX}-003
   └─ 파일: docs/prd/tasks/{PREFIX}-003.md
   └─ 브랜치: feature/{PREFIX}-003-persistence

4. REST API Layer (Adapter-In)
   └─ Issue: {PREFIX}-004
   └─ 파일: docs/prd/tasks/{PREFIX}-004.md
   └─ 브랜치: feature/{PREFIX}-004-rest-api

5. Integration Test
   └─ Issue: {PREFIX}-005
   └─ 파일: docs/prd/tasks/{PREFIX}-005.md
   └─ 브랜치: feature/{PREFIX}-005-integration
```

### 3. Task 파일 템플릿

각 Task 파일은 다음 구조로 생성:

```markdown
# {ISSUE-KEY}: {Layer} 구현

**Epic**: {Epic 이름}
**Layer**: {Layer 이름}
**브랜치**: feature/{ISSUE-KEY}-{layer}
**Jira URL**: (sync-to-jira 후 추가)

---

## 📝 목적

{해당 레이어의 역할 및 목적}

---

## 🎯 요구사항

### 도메인 규칙 (Domain Layer만)
- [ ] ...

### 비즈니스 로직 (Application Layer만)
- [ ] ...

### 데이터 저장 (Persistence Layer만)
- [ ] ...

### API 엔드포인트 (REST API Layer만)
- [ ] ...

### E2E 시나리오 (Integration Test만)
- [ ] ...

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙
- [ ] Lombok 금지
- [ ] Law of Demeter 준수
- [ ] Long FK 전략 (Persistence만)
- [ ] Transaction 경계 (Application만)

### 테스트 규칙
- [ ] ArchUnit 테스트 필수
- [ ] TestFixture 사용 필수
- [ ] 테스트 커버리지 > 80%

---

## ✅ 완료 조건

- [ ] 모든 요구사항 구현 완료
- [ ] 모든 테스트 통과 (Unit + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- PRD: {PRD 파일 경로}
- Plan: docs/prd/plans/{ISSUE-KEY}-{layer}-plan.md (create-plan 후 생성)
- Jira: (sync-to-jira 후 추가)
```

---

## 📂 출력 구조

```
docs/prd/tasks/
├── MEMBER-001.md  (Domain Layer)
├── MEMBER-002.md  (Application Layer)
├── MEMBER-003.md  (Persistence Layer)
├── MEMBER-004.md  (REST API Layer)
└── MEMBER-005.md  (Integration Test)
```

---

## 🔄 레이어별 요구사항 추출 규칙

### Domain Layer (001)
**추출 대상**:
- 도메인 개념 (Aggregate, Entity, VO)
- 비즈니스 규칙 및 불변식
- 도메인 이벤트
- 도메인 예외

**예시**:
```markdown
## 🎯 요구사항

### Aggregate Root
- [ ] Member Aggregate 설계
  - memberId (Long, PK)
  - email (Email VO)
  - password (Password VO)
  - status (MemberStatus Enum)

### Value Objects
- [ ] Email VO: 이메일 형식 검증
- [ ] Password VO: 암호화 + 강도 검증

### 비즈니스 규칙
- [ ] 이메일 중복 불가
- [ ] 비밀번호 최소 8자 이상
- [ ] 탈퇴 후 재가입 30일 제한
```

### Application Layer (002)
**추출 대상**:
- Use Case (Command/Query)
- 비즈니스 로직 흐름
- Transaction 경계
- Port 정의 (In/Out)

**예시**:
```markdown
## 🎯 요구사항

### Command Use Cases
- [ ] RegisterMemberUseCase
  - Input: RegisterMemberCommand
  - Output: RegisterMemberResponse
  - Transaction: 필수

### Query Use Cases
- [ ] GetMemberUseCase
  - Input: GetMemberQuery
  - Output: MemberResponse

### Port 정의
- [ ] MemberCommandPort (Out): save()
- [ ] MemberQueryPort (Out): findById(), existsByEmail()
```

### Persistence Layer (003)
**추출 대상**:
- JPA Entity 설계
- Repository 인터페이스
- QueryDSL 쿼리
- Adapter 구현

**예시**:
```markdown
## 🎯 요구사항

### JPA Entity
- [ ] MemberEntity (Long FK 전략)
  - id, email, password, status
  - BaseAuditEntity 상속

### Repository
- [ ] JpaRepository: save(), findById()
- [ ] QueryDslRepository: 복잡한 조회 쿼리

### Adapter
- [ ] MemberCommandAdapter (Port 구현)
- [ ] MemberQueryAdapter (Port 구현)
```

### REST API Layer (004)
**추출 대상**:
- API 엔드포인트
- Request/Response DTO
- 에러 핸들링
- API 문서화

**예시**:
```markdown
## 🎯 요구사항

### API 엔드포인트
- [ ] POST /api/v1/members - 회원 가입
- [ ] GET /api/v1/members/{id} - 회원 조회
- [ ] PUT /api/v1/members/{id} - 회원 수정
- [ ] DELETE /api/v1/members/{id} - 회원 탈퇴

### DTO
- [ ] RegisterMemberRequest (Command DTO)
- [ ] MemberResponse (Response DTO)

### 검증
- [ ] @Valid 필수
- [ ] 커스텀 Validator
```

### Integration Test (005)
**추출 대상**:
- E2E 시나리오
- 테스트 데이터 준비
- API 호출 시퀀스

**예시**:
```markdown
## 🎯 요구사항

### E2E 시나리오
- [ ] 회원 가입 → 조회 → 수정 → 탈퇴 전체 플로우
- [ ] 이메일 중복 검증
- [ ] 비밀번호 강도 검증
- [ ] 권한 검증 (인증/인가)

### 테스트 환경
- [ ] Flyway 마이그레이션
- [ ] TestRestTemplate 사용
- [ ] @Sql 데이터 준비
```

---

## 🚀 실행 예시

**입력**:
```bash
/breakdown-prd docs/prd/member-management.md
```

**출력**:
```
✅ PRD 분석 완료: 회원 관리 시스템
📋 5개 Task 생성 완료:

1. MEMBER-001: Domain Layer 구현
   └─ docs/prd/tasks/MEMBER-001.md

2. MEMBER-002: Application Layer 구현
   └─ docs/prd/tasks/MEMBER-002.md

3. MEMBER-003: Persistence Layer 구현
   └─ docs/prd/tasks/MEMBER-003.md

4. MEMBER-004: REST API Layer 구현
   └─ docs/prd/tasks/MEMBER-004.md

5. MEMBER-005: Integration Test
   └─ docs/prd/tasks/MEMBER-005.md

🔗 다음 단계:
1. /create-plan MEMBER-001  (TDD Plan 생성)
2. /sync-to-jira docs/prd/tasks/  (Jira Epic + Issues 생성)
```

---

## ⚙️ 설정

**PRD 파일 요구사항**:
- Epic 이름 명시
- Issue Key Prefix 명시
- 레이어별 요구사항 섹션 포함

**예시 PRD 구조**:
```markdown
# 회원 관리 시스템 PRD

**Epic**: 회원 관리 시스템
**Issue Key Prefix**: MEMBER

## 도메인 요구사항
...

## 비즈니스 로직 요구사항
...

## API 요구사항
...
```

---

## 🎯 핵심 원칙

1. **의존성 순서 준수**: Domain → Application → Persistence/REST API
2. **Zero-Tolerance 규칙**: 모든 Task에 규칙 체크리스트 포함
3. **완료 조건 명확화**: 각 Task의 완료 조건 명시
4. **Jira 연동 준비**: Jira URL 필드 미리 준비
