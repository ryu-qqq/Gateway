# MEMBER-001: Domain Layer 구현

**Epic**: 간단한 회원 가입
**Layer**: Domain Layer
**브랜치**: feature/MEMBER-001-domain
**Jira URL**: https://ryuqqq.atlassian.net/browse/AESA-71
**상태**: In Progress (진행 중)
**시작일**: 2025-11-14
**담당자**: 류상

---

## 📝 목적

회원 가입의 핵심 비즈니스 규칙을 담은 도메인 모델을 설계합니다.
- Member Aggregate: 회원의 핵심 비즈니스 규칙 관리
- Email VO: 이메일 주소의 유효성 검증
- Password VO: 비밀번호 정책 검증 및 암호화
- Domain Exceptions: 도메인 규칙 위반 예외 처리

---

## 🎯 요구사항

### Aggregate Root
- [ ] **Member Aggregate 설계**
  - `Long id` - 회원 고유 식별자
  - `Email email` - 이메일 (VO)
  - `Password password` - 비밀번호 (VO)
  - `LocalDateTime createdAt` - 가입일시
  - `LocalDateTime updatedAt` - 수정일시

### 비즈니스 규칙
- [ ] 이메일은 반드시 유효한 형식이어야 함
- [ ] 비밀번호는 최소 8자 이상이어야 함
- [ ] 비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 함
- [ ] 생성 후 이메일은 변경 불가능 (Immutable)

### Value Objects
- [ ] **Email VO 설계**
  - `String value` - 이메일 주소
  - RFC 5322 표준 형식 준수
  - 최대 길이 320자
  - `@` 기호 포함 필수
  - 도메인 부분 필수

- [ ] **Password VO 설계**
  - `String value` - 암호화된 비밀번호
  - 최소 길이 8자
  - 최대 길이 100자
  - 영문 대소문자 포함
  - 숫자 포함
  - 특수문자 포함
  - BCrypt 암호화 사용

### Domain Exceptions
- [ ] **MemberRegistrationException 계층 구조**
  - `DuplicateEmailException` - 이메일 중복
  - `InvalidEmailFormatException` - 이메일 형식 오류
  - `InvalidPasswordFormatException` - 비밀번호 형식 오류

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙
- [ ] **Lombok 금지** - Plain Java 생성자/메서드 사용
  - ❌ `@Getter`, `@Setter`, `@Builder` 사용 불가
  - ✅ 명시적 생성자, getter/setter 직접 작성

- [ ] **Law of Demeter 준수**
  - ❌ `member.getPassword().getValue()` 금지
  - ✅ `member.validatePassword(rawPassword)` 사용

- [ ] **Tell Don't Ask 패턴**
  - ❌ `if (member.getPassword().isValid())` 금지
  - ✅ `member.validatePassword()` 메서드 제공

### 테스트 규칙
- [ ] ArchUnit 테스트 필수
  - Domain 패키지는 다른 레이어 의존 금지
  - Aggregate는 `Aggregate` suffix 필수
  - VO는 `equals()`, `hashCode()` 구현 필수

- [ ] TestFixture 사용 필수
  - `MemberFixture.java` - Object Mother 패턴
  - `EmailFixture.java`
  - `PasswordFixture.java`

- [ ] 테스트 커버리지 > 90%
  - Member Aggregate 생성 테스트
  - Email VO 검증 테스트 (정상/오류)
  - Password VO 검증 테스트 (정상/오류)
  - Exception 발생 테스트

---

## ✅ 완료 조건

- [ ] 모든 요구사항 구현 완료
  - Member Aggregate
  - Email VO
  - Password VO
  - Domain Exceptions

- [ ] 모든 테스트 통과 (Unit + ArchUnit)
  - `MemberTest.java`
  - `EmailTest.java`
  - `PasswordTest.java`
  - `AggregateRootArchTest.java`

- [ ] Zero-Tolerance 규칙 준수
  - Lombok 미사용 확인
  - Law of Demeter 준수 확인
  - Tell Don't Ask 패턴 적용 확인

- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: `docs/prd/simple-member-signup.md`
- **Plan**: `docs/prd/plans/MEMBER-001-domain-plan.md` (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)
- **코딩 규칙**: `docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md`

---

## 📚 참고 규칙

- `docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md`
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-test-guide.md`
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-archunit.md`
- `docs/coding_convention/02-domain-layer/vo/vo-guide.md`
- `docs/coding_convention/02-domain-layer/exception/exception-guide.md`
