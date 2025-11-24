# /create-plan - Task를 Kent Beck TDD Plan으로 변환

**목적**: Task 파일을 읽어 Kent Beck TDD 사이클 기반 Plan 파일 생성

**사용법**:
```bash
/create-plan <issue-key>
/create-plan MEMBER-001
```

---

## 📋 작업 순서

### 1. Task 파일 읽기

**입력**:
- Issue Key (예: `MEMBER-001`)

**파일 위치**:
- Task: `docs/prd/tasks/{ISSUE-KEY}.md`

**추출할 정보**:
- Layer (domain/application/persistence/rest-api/integration)
- 요구사항 체크리스트
- 제약사항 (Zero-Tolerance 규칙)

### 2. Kent Beck TDD 사이클 분할

**TDD 4단계 사이클**:

```
Red (테스트 작성)
    ↓
Green (최소 구현)
    ↓
Refactor (리팩토링)
    ↓
Tidy (TestFixture 정리)
    ↓
(다음 요구사항으로 반복)
```

**각 요구사항마다 4단계 생성**:
- ✅ 하나의 요구사항 = 하나의 완전한 Red → Green → Refactor → Tidy 사이클
- ✅ 작은 단위로 쪼개기 (5-15분 내 완료 가능)
- ✅ 각 단계마다 체크박스 (`[ ]`)

### 3. Plan 파일 템플릿

```markdown
# {ISSUE-KEY} TDD Plan

**Task**: {Task 제목}
**Layer**: {Layer 이름}
**브랜치**: feature/{ISSUE-KEY}-{layer}
**예상 소요 시간**: {총 사이클 수 × 15분}

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ {첫 번째 요구사항} (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `{Entity}Test.java` 파일 생성
- [ ] `{테스트 메서드}` 작성
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: {요구사항} 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `{Entity}.java` 파일 생성
- [ ] 테스트 통과할 만큼만 구현
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: {요구사항} 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 코드 개선 (가독성, 성능, 중복 제거)
- [ ] 테스트 여전히 통과 확인
- [ ] ArchUnit 테스트 통과 확인
- [ ] 커밋: `refactor: {요구사항} 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `{Entity}Fixture.java` 생성/업데이트 (Object Mother 패턴)
- [ ] 테스트 코드를 Fixture 사용하도록 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: {Entity}Fixture 정리 (Tidy)`

---

### 2️⃣ {두 번째 요구사항} (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] ...

(반복)

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (체크박스 모두 ✅)
- [ ] 모든 테스트 통과
- [ ] ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수
- [ ] TestFixture 모두 정리

---

## 🔗 관련 문서

- Task: docs/prd/tasks/{ISSUE-KEY}.md
- PRD: {PRD 파일 경로}
```

---

## 🎯 레이어별 Plan 생성 규칙

### Domain Layer Plan

**TDD 사이클 예시**:

```markdown
### 1️⃣ Member Aggregate Root 설계 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `MemberTest.java` 생성
- [ ] `shouldCreateMemberWithValidData()` 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: Member Aggregate 생성 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `Member.java` 생성 (Plain Java)
- [ ] 생성자 + Getter 작성 (Lombok 금지)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Member Aggregate 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 불변성 보장 (final 필드)
- [ ] Law of Demeter 준수 확인
- [ ] ArchUnit 테스트 추가 및 통과
- [ ] 커밋: `refactor: Member Aggregate 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `MemberFixture.java` 생성 (Object Mother 패턴)
- [ ] `MemberFixture.aMember()` 메서드 작성
- [ ] `MemberTest` → Fixture 사용으로 리팩토링
- [ ] 커밋: `test: MemberFixture 정리 (Tidy)`

---

### 2️⃣ Email Value Object 설계 (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `EmailTest.java` 생성
- [ ] `shouldCreateEmailWithValidFormat()` 작성
- [ ] `shouldThrowExceptionWhenInvalidFormat()` 작성
- [ ] 커밋: `test: Email VO 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `Email.java` 생성 (Record)
- [ ] 형식 검증 로직 추가
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Email VO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Regex 패턴 상수로 추출
- [ ] VO ArchUnit 테스트 통과 확인
- [ ] 커밋: `refactor: Email VO 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `EmailFixture.anEmail()` 메서드 추가
- [ ] `EmailTest` → Fixture 사용
- [ ] 커밋: `test: EmailFixture 정리 (Tidy)`
```

### Application Layer Plan

**TDD 사이클 예시**:

```markdown
### 1️⃣ RegisterMemberUseCase 구현 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `RegisterMemberUseCaseTest.java` 생성
- [ ] Mock Port 준비 (MemberCommandPort, MemberQueryPort)
- [ ] `shouldRegisterMemberSuccessfully()` 작성
- [ ] 커밋: `test: RegisterMemberUseCase 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `RegisterMemberUseCase.java` 생성
- [ ] `RegisterMemberCommand` 생성 (Record)
- [ ] `@Transactional` 추가
- [ ] 이메일 중복 체크 + Member 생성 + 저장 로직
- [ ] 커밋: `impl: RegisterMemberUseCase 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Transaction 경계 검증 (외부 API 호출 없는지)
- [ ] Assembler 패턴 적용
- [ ] ArchUnit 테스트 통과
- [ ] 커밋: `refactor: RegisterMemberUseCase 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `RegisterMemberCommandFixture` 생성
- [ ] 테스트 → Fixture 사용
- [ ] 커밋: `test: RegisterMemberCommand Fixture 정리 (Tidy)`
```

### Persistence Layer Plan

**TDD 사이클 예시**:

```markdown
### 1️⃣ MemberEntity 설계 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `MemberEntityTest.java` 생성
- [ ] `shouldMapToMember()` 작성
- [ ] 커밋: `test: MemberEntity 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `MemberEntity.java` 생성
- [ ] Long FK 전략 (관계 어노테이션 금지)
- [ ] BaseAuditEntity 상속
- [ ] 테스트 통과
- [ ] 커밋: `impl: MemberEntity 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] JPA Entity ArchUnit 테스트 통과
- [ ] Lombok 사용 여부 확인 (금지)
- [ ] 커밋: `refactor: MemberEntity 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `MemberEntityFixture` 생성
- [ ] 커밋: `test: MemberEntityFixture 정리 (Tidy)`

---

### 2️⃣ MemberCommandAdapter 구현 (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `MemberCommandAdapterTest.java` 생성 (@DataJpaTest)
- [ ] `shouldSaveMember()` 작성
- [ ] 커밋: `test: MemberCommandAdapter 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `MemberCommandAdapter.java` 생성
- [ ] MemberCommandPort 구현
- [ ] MemberMapper 사용
- [ ] 커밋: `impl: MemberCommandAdapter 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] ArchUnit 테스트 통과
- [ ] 커밋: `refactor: MemberCommandAdapter 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] Fixture 사용으로 테스트 정리
- [ ] 커밋: `test: MemberCommandAdapter 테스트 정리 (Tidy)`
```

### REST API Layer Plan

**TDD 사이클 예시**:

```markdown
### 1️⃣ POST /api/v1/members 구현 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `MemberControllerTest.java` 생성
- [ ] MockMvc 금지, TestRestTemplate 사용
- [ ] `shouldRegisterMemberSuccessfully()` 작성
- [ ] 커밋: `test: 회원 가입 API 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `MemberController.java` 생성
- [ ] `RegisterMemberRequest` DTO 생성 (Record + @Valid)
- [ ] `MemberResponse` DTO 생성
- [ ] POST /api/v1/members 엔드포인트 구현
- [ ] 커밋: `impl: 회원 가입 API 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] RESTful 설계 검증
- [ ] Controller ArchUnit 테스트 통과
- [ ] DTO ArchUnit 테스트 통과
- [ ] 커밋: `refactor: 회원 가입 API 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `RegisterMemberRequestFixture` 생성
- [ ] RestDocs 문서화 추가
- [ ] 커밋: `test: 회원 가입 API 테스트 정리 (Tidy)`
```

### Integration Test Plan

**TDD 사이클 예시**:

```markdown
### 1️⃣ 회원 가입 → 조회 E2E 시나리오 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `MemberIntegrationTest.java` 생성
- [ ] @SpringBootTest + TestRestTemplate
- [ ] 회원 가입 → 조회 시나리오 작성
- [ ] 커밋: `test: 회원 E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] Flyway 마이그레이션 설정
- [ ] 테스트 데이터 준비 (@Sql)
- [ ] 테스트 통과
- [ ] 커밋: `impl: 회원 E2E 테스트 통과 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 테스트 격리 확인
- [ ] Integration Test ArchUnit 테스트 통과
- [ ] 커밋: `refactor: 회원 E2E 테스트 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] E2E Fixture 정리
- [ ] 커밋: `test: 회원 E2E Fixture 정리 (Tidy)`
```

---

## 📂 출력 구조

```
docs/prd/plans/
├── MEMBER-001-domain-plan.md
├── MEMBER-002-application-plan.md
├── MEMBER-003-persistence-plan.md
├── MEMBER-004-rest-api-plan.md
└── MEMBER-005-integration-plan.md
```

---

## 🚀 실행 예시

**입력**:
```bash
/create-plan MEMBER-001
```

**출력**:
```
✅ Task 분석 완료: MEMBER-001 (Domain Layer)
📋 TDD Plan 생성 완료:
   └─ docs/prd/plans/MEMBER-001-domain-plan.md

📊 사이클 요약:
   - 총 사이클 수: 5
   - 예상 소요 시간: 75분 (5 사이클 × 15분)
   - Red 단계: 5개
   - Green 단계: 5개
   - Refactor 단계: 5개
   - Tidy 단계: 5개

🔗 다음 단계:
   /jira-start MEMBER-001  (브랜치 생성 + Jira 시작)
```

---

## ⚙️ 설정

**사이클 크기 기준**:
- ✅ 작은 단위: 5-15분 내 완료 가능
- ✅ 하나의 요구사항 = 하나의 완전한 사이클
- ✅ 각 사이클마다 커밋 (Red, Green, Refactor, Tidy 각각)

**커밋 메시지 규칙**:
```
test: {요구사항} 테스트 추가 (Red)
impl: {요구사항} 구현 (Green)
refactor: {요구사항} 개선 (Refactor)
test: {Entity}Fixture 정리 (Tidy)
```

---

## 🎯 핵심 원칙

1. **작은 단위**: 각 사이클은 5-15분 내 완료
2. **4단계 필수**: Red → Green → Refactor → Tidy 모두 수행
3. **TestFixture 필수**: Tidy 단계에서 Object Mother 패턴 적용
4. **Zero-Tolerance**: 각 Refactor 단계에서 ArchUnit 검증
5. **체크박스 추적**: `/kb/{layer}/go` 명령이 Plan 파일을 읽고 진행 상황 추적
