# MEMBER-002: Application Layer 구현

**Epic**: 간단한 회원 가입
**Layer**: Application Layer
**브랜치**: feature/MEMBER-002-application
**Jira URL**: https://ryuqqq.atlassian.net/browse/AESA-72

---

## 📝 목적

회원 가입 유스케이스를 오케스트레이션하고 트랜잭션을 관리합니다.
- RegisterMemberUseCase: 회원 가입 비즈니스 로직 흐름
- Command/Query DTO: 입출력 데이터 구조
- Port 정의: Persistence Layer와의 인터페이스
- Assembler: DTO ↔ Domain 변환

---

## 🎯 요구사항

### Command Use Cases
- [ ] **RegisterMemberUseCase 구현**
  - Input: `RegisterMemberCommand`
    - `String email` - 이메일 주소
    - `String rawPassword` - 평문 비밀번호
  - Output: `MemberResponse`
    - `Long memberId` - 생성된 회원 ID
    - `String email` - 이메일 주소
    - `LocalDateTime createdAt` - 가입일시

### 비즈니스 로직 흐름
- [ ] **회원 가입 프로세스**
  1. Email VO 생성 및 검증
  2. 이메일 중복 확인 (CheckDuplicateEmailQuery)
  3. Password VO 생성 및 검증
  4. Member Aggregate 생성
  5. Member 저장 (SaveMemberCommand)
  6. MemberResponse 반환

### Transaction 경계
- [ ] **@Transactional 적용**
  ```
  ┌─────────────────────────────────────┐
  │ @Transactional (RegisterMemberUseCase) │
  ├─────────────────────────────────────┤
  │ 1. Email 중복 확인 (Query)           │
  │ 2. Member Aggregate 생성            │
  │ 3. Member 저장 (Command)            │
  └─────────────────────────────────────┘
  ```
  - ⚠️ Transaction 내 외부 API 호출 절대 금지

### Port 정의 (Out)
- [ ] **CheckDuplicateEmailQueryPort**
  - `boolean existsByEmail(String email)`
  - Query 전용 Port

- [ ] **SaveMemberCommandPort**
  - `Member save(Member member)`
  - Command 전용 Port

### DTO 설계
- [ ] **RegisterMemberCommand** (Command DTO)
  - Record 패턴 사용
  - Immutable 구조

- [ ] **MemberResponse** (Response DTO)
  - Record 패턴 사용
  - Immutable 구조

### Assembler
- [ ] **MemberAssembler**
  - `MemberResponse toResponse(Member member)`
  - `Member toDomain(RegisterMemberCommand command, Email email, Password password)`

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙
- [ ] **Transaction 경계 준수**
  - ❌ `@Transactional` 내 외부 API 호출 금지
  - ✅ Transaction = DB 작업 + 도메인 로직만

- [ ] **CQRS 분리**
  - ❌ 단일 Port로 Command/Query 혼재 금지
  - ✅ CommandPort, QueryPort 명확히 분리

- [ ] **Assembler 사용 필수**
  - ❌ UseCase 내 직접 DTO 변환 금지
  - ✅ Assembler에 변환 책임 위임

- [ ] **Spring 프록시 제약사항**
  - ❌ Private 메서드에 `@Transactional` 불가
  - ❌ Final 클래스/메서드에 `@Transactional` 불가
  - ❌ 같은 클래스 내부 호출 (`this.method()`) 프록시 미작동

### 테스트 규칙
- [ ] ArchUnit 테스트 필수
  - UseCase는 `UseCase` suffix 필수
  - Port는 `Port` suffix 필수
  - Assembler는 `Assembler` suffix 필수

- [ ] TestFixture 사용 필수
  - `RegisterMemberCommandFixture.java`
  - `MemberResponseFixture.java`

- [ ] 테스트 커버리지 > 85%
  - UseCase 정상 시나리오
  - UseCase 예외 시나리오 (중복 이메일, 형식 오류)
  - Assembler 변환 테스트

---

## ✅ 완료 조건

- [ ] 모든 요구사항 구현 완료
  - RegisterMemberUseCase
  - Command/Query DTO
  - Port 인터페이스
  - Assembler

- [ ] 모든 테스트 통과 (Unit + ArchUnit)
  - `RegisterMemberUseCaseTest.java`
  - `MemberAssemblerTest.java`
  - `UseCaseArchTest.java`
  - `PortArchTest.java`

- [ ] Zero-Tolerance 규칙 준수
  - Transaction 경계 확인
  - CQRS 분리 확인
  - Assembler 사용 확인

- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: `docs/prd/simple-member-signup.md`
- **Plan**: `docs/prd/plans/MEMBER-002-application-plan.md` (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)
- **코딩 규칙**: `docs/coding_convention/03-application-layer/application-guide.md`

---

## 📚 참고 규칙

- `docs/coding_convention/03-application-layer/application-guide.md`
- `docs/coding_convention/03-application-layer/assembler/assembler-guide.md`
- `docs/coding_convention/03-application-layer/dto/command/command-dto-guide.md`
- `docs/coding_convention/03-application-layer/dto/response/response-dto-guide.md`
- `docs/coding_convention/03-application-layer/port/out/command/command-port-guide.md`
- `docs/coding_convention/03-application-layer/port/out/query/query-port-guide.md`
