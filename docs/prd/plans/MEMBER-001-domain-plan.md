# MEMBER-001 TDD Plan

**Task**: Domain Layer 구현 - 간단한 회원 가입
**Layer**: Domain Layer
**브랜치**: feature/MEMBER-001-domain
**예상 소요 시간**: 75분 (5 사이클 × 15분)

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ Email Value Object 설계 (Cycle 1) ✅ COMPLETED

#### 🔴 Red: 테스트 작성
- [x] `domain/src/test/java/com/ryuqq/domain/member/Email/EmailTest.java` 파일 생성
- [x] `shouldCreateEmailWithValidFormat()` 테스트 작성
  - 유효한 이메일 형식으로 Email 생성
  - `user@example.com` 형식 검증
- [x] `shouldThrowExceptionWhenInvalidFormat()` 테스트 작성
  - `@` 없는 이메일
  - 도메인 없는 이메일
  - 320자 초과 이메일
- [x] 테스트 실행 → 컴파일 에러 확인
- [x] 커밋: `test: Email VO 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [x] `domain/src/main/java/com/ryuqq/domain/member/Email/Email.java` 파일 생성
- [x] Record 패턴으로 구현: `public record Email(String value)`
- [x] 생성자에 검증 로직 추가:
  - RFC 5322 이메일 형식 검증 (정규식)
  - 최대 길이 320자 검증
  - `@` 기호 포함 검증
  - 도메인 부분 검증
- [x] `InvalidEmailFormatException` 생성 및 throw
- [x] 테스트 실행 → 통과 확인
- [x] 커밋: `impl: Email VO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [x] 정규식 패턴 상수로 추출: `private static final Pattern EMAIL_PATTERN`
- [x] 검증 로직 메서드로 분리: `private static void validate(String value)`
- [x] `equals()`, `hashCode()` 자동 생성 확인 (Record)
- [x] VO ArchUnit 테스트 작성 및 통과
  - VO는 `equals()`, `hashCode()` 구현 필수
  - VO는 Immutable 필수
- [x] 테스트 여전히 통과 확인
- [x] 커밋: `refactor: Email VO 개선 (Refactor)` → 이미 완료됨

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/domain/member/Email/EmailFixture.java` 생성
- [ ] Object Mother 패턴 적용:
  ```java
  public class EmailFixture {
      public static Email anEmail() {
          return new Email("user@example.com");
      }

      public static Email anEmail(String value) {
          return new Email(value);
      }
  }
  ```
- [ ] `EmailTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: EmailFixture 정리 (Tidy)`

---

### 2️⃣ Password Value Object 설계 (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/domain/member/Password/PasswordTest.java` 파일 생성
- [ ] `shouldCreatePasswordWithValidFormat()` 테스트 작성
  - 유효한 비밀번호 형식 (8자 이상, 영문/숫자/특수문자 포함)
  - 평문 비밀번호 → BCrypt 암호화 확인
- [ ] `shouldThrowExceptionWhenInvalidFormat()` 테스트 작성
  - 8자 미만
  - 영문 미포함
  - 숫자 미포함
  - 특수문자 미포함
  - 100자 초과
- [ ] `shouldMatchRawPassword()` 테스트 작성
  - BCrypt 매칭 검증
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: Password VO 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/domain/member/Password/Password.java` 파일 생성
- [ ] Record 패턴으로 구현: `public record Password(String value)`
- [ ] 정적 팩토리 메서드 추가:
  ```java
  public static Password of(String rawPassword) {
      validate(rawPassword);
      return new Password(encrypt(rawPassword));
  }
  ```
- [ ] 검증 로직 구현:
  - 최소 8자, 최대 100자
  - 영문 대소문자 포함 (정규식)
  - 숫자 포함 (정규식)
  - 특수문자 포함 (정규식)
- [ ] BCrypt 암호화 구현: `BCryptPasswordEncoder`
- [ ] `matches(String rawPassword)` 메서드 구현
- [ ] `InvalidPasswordFormatException` 생성 및 throw
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Password VO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 정규식 패턴 상수로 추출
- [ ] 검증 로직 메서드로 분리
- [ ] VO ArchUnit 테스트 통과 확인
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `refactor: Password VO 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/domain/member/Password/PasswordFixture.java` 생성
- [ ] Object Mother 패턴 적용:
  ```java
  public class PasswordFixture {
      public static Password aPassword() {
          return Password.of("SecurePass123!");
      }

      public static Password aPassword(String rawPassword) {
          return Password.of(rawPassword);
      }
  }
  ```
- [ ] `PasswordTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PasswordFixture 정리 (Tidy)`

---

### 3️⃣ Member Aggregate Root 설계 (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/domain/member/Member/MemberTest.java` 파일 생성
- [ ] `shouldCreateMemberWithValidData()` 테스트 작성
  - Email, Password로 Member 생성
  - 생성일시 자동 설정 확인
- [ ] `shouldNotAllowEmailChange()` 테스트 작성 (Immutable)
- [ ] `shouldValidatePasswordCorrectly()` 테스트 작성
  - Law of Demeter 준수: `member.validatePassword(rawPassword)`
  - Tell Don't Ask 패턴 적용
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: Member Aggregate 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/domain/member/Member/Member.java` 파일 생성
- [ ] Plain Java 클래스로 구현 (Lombok 금지):
  ```java
  public class Member {
      private final Long id;
      private final Email email;
      private final Password password;
      private final LocalDateTime createdAt;
      private LocalDateTime updatedAt;

      public Member(Long id, Email email, Password password,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
          this.id = id;
          this.email = email;
          this.password = password;
          this.createdAt = createdAt;
          this.updatedAt = updatedAt;
      }

      // Getter 직접 작성 (Lombok 금지)
      public Long getId() { return id; }
      public Email getEmail() { return email; }
      // ... 나머지 Getter
  }
  ```
- [ ] 정적 팩토리 메서드 추가:
  ```java
  public static Member create(Email email, Password password) {
      return new Member(null, email, password,
                       LocalDateTime.now(), LocalDateTime.now());
  }
  ```
- [ ] Law of Demeter 준수 메서드 추가:
  ```java
  public boolean validatePassword(String rawPassword) {
      return password.matches(rawPassword);
  }
  ```
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Member Aggregate 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 불변성 보장 (final 필드)
- [ ] Law of Demeter 준수 확인
  - ❌ `member.getPassword().getValue()` 제거
  - ✅ `member.validatePassword()` 사용
- [ ] Tell Don't Ask 패턴 확인
- [ ] Aggregate ArchUnit 테스트 작성 및 통과
  - Aggregate는 `Aggregate` suffix 또는 명확한 도메인 이름
  - Domain 패키지는 다른 레이어 의존 금지
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `refactor: Member Aggregate 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/domain/member/Member/MemberFixture.java` 생성
- [ ] Object Mother 패턴 적용:
  ```java
  public class MemberFixture {
      public static Member aMember() {
          return Member.create(
              EmailFixture.anEmail(),
              PasswordFixture.aPassword()
          );
      }

      public static Member aMember(Email email, Password password) {
          return Member.create(email, password);
      }
  }
  ```
- [ ] `MemberTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: MemberFixture 정리 (Tidy)`

---

### 4️⃣ Domain Exceptions 계층 구조 (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/domain/member/exception/MemberExceptionTest.java` 파일 생성
- [ ] `shouldThrowDuplicateEmailException()` 테스트 작성
- [ ] `shouldThrowInvalidEmailFormatException()` 테스트 작성
- [ ] `shouldThrowInvalidPasswordFormatException()` 테스트 작성
- [ ] Exception 메시지 검증
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: Domain Exception 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/domain/member/exception/MemberRegistrationException.java` 생성 (부모 클래스)
  ```java
  public class MemberRegistrationException extends RuntimeException {
      public MemberRegistrationException(String message) {
          super(message);
      }
  }
  ```
- [ ] `DuplicateEmailException.java` 생성
- [ ] `InvalidEmailFormatException.java` 생성 (이미 Email VO에서 사용 중)
- [ ] `InvalidPasswordFormatException.java` 생성 (이미 Password VO에서 사용 중)
- [ ] 각 Exception에 명확한 메시지 제공
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Domain Exception 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Exception 계층 구조 정리
- [ ] Exception ArchUnit 테스트 작성 및 통과
  - Exception은 `Exception` suffix 필수
  - Domain Exception은 RuntimeException 상속
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `refactor: Domain Exception 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] Exception 테스트는 Fixture 불필요 (간단한 구조)
- [ ] 테스트 코드 가독성 개선
- [ ] 커밋: `test: Domain Exception 테스트 정리 (Tidy)`

---

### 5️⃣ Domain Layer ArchUnit 통합 테스트 (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/domain/architecture/aggregate/AggregateRootArchTest.java` 업데이트
- [ ] Member Aggregate ArchUnit 규칙 추가:
  - Aggregate는 다른 레이어 의존 금지
  - Lombok 어노테이션 사용 금지
- [ ] `domain/src/test/java/com/ryuqq/domain/architecture/vo/ValueObjectArchTest.java` 업데이트
- [ ] Email, Password VO ArchUnit 규칙 추가:
  - VO는 `equals()`, `hashCode()` 구현 필수
  - VO는 Immutable (final 필드)
- [ ] `domain/src/test/java/com/ryuqq/domain/architecture/exception/ExceptionArchTest.java` 업데이트
- [ ] Domain Exception ArchUnit 규칙 추가
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `test: Domain ArchUnit 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] ArchUnit 테스트 통과 확인
- [ ] 위반 사항 수정 (있을 경우)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `impl: Domain ArchUnit 테스트 통과 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] ArchUnit 규칙 정리 및 최적화
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `refactor: Domain ArchUnit 테스트 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] 모든 Fixture 최종 검토
- [ ] Fixture 일관성 확인
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Domain Fixture 최종 정리 (Tidy)`

---

## ✅ 완료 조건

### 구현 완료
- [ ] Email VO (RFC 5322, 320자 제한)
- [ ] Password VO (BCrypt 암호화, 8-100자, 영문/숫자/특수문자)
- [ ] Member Aggregate (Plain Java, Law of Demeter)
- [ ] Domain Exceptions (3개 - Duplicate, InvalidEmail, InvalidPassword)

### 테스트 완료
- [ ] `EmailTest.java` - 정상/오류 케이스
- [ ] `PasswordTest.java` - 정상/오류/매칭 케이스
- [ ] `MemberTest.java` - 생성/검증 케이스
- [ ] `MemberExceptionTest.java` - Exception 케이스
- [ ] `AggregateRootArchTest.java` - Member Aggregate 규칙
- [ ] `ValueObjectArchTest.java` - Email/Password VO 규칙
- [ ] `ExceptionArchTest.java` - Domain Exception 규칙
- [ ] 테스트 커버리지 > 90%

### Zero-Tolerance 준수
- [ ] Lombok 미사용 (Plain Java 생성자/Getter)
- [ ] Law of Demeter 준수 (`member.validatePassword()`)
- [ ] Tell Don't Ask 패턴 (`member.validatePassword()` 반환)
- [ ] ArchUnit 테스트 모두 통과

### TestFixture 완료
- [ ] `EmailFixture.java` - Object Mother 패턴
- [ ] `PasswordFixture.java` - Object Mother 패턴
- [ ] `MemberFixture.java` - Object Mother 패턴

### 최종 검증
- [ ] 모든 TDD 사이클 체크박스 완료 (총 20개 단계)
- [ ] `./gradlew :domain:test` 통과
- [ ] `./gradlew :domain:test --tests *ArchTest` 통과
- [ ] 모든 커밋 메시지 규칙 준수 (총 20개 커밋)

---

## 🔗 관련 문서

- **Task**: `docs/prd/tasks/MEMBER-001.md`
- **PRD**: `docs/prd/simple-member-signup.md`
- **Jira**: (sync-to-jira 후 추가)

### 코딩 규칙
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md`
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-test-guide.md`
- `docs/coding_convention/02-domain-layer/aggregate/aggregate-archunit.md`
- `docs/coding_convention/02-domain-layer/vo/vo-guide.md`
- `docs/coding_convention/02-domain-layer/vo/vo-test-guide.md`
- `docs/coding_convention/02-domain-layer/vo/vo-archunit.md`
- `docs/coding_convention/02-domain-layer/exception/exception-guide.md`
- `docs/coding_convention/02-domain-layer/exception/exception-archunit-guide.md`

---

## 📊 진행 상황 추적

**사이클 완료**: 0 / 5
**예상 남은 시간**: 75분

**다음 실행**:
```bash
/jira-start MEMBER-001  # 브랜치 생성 + Jira In Progress
# 또는
/kb/domain/go           # TDD 사이클 시작 (Plan 파일 기반)
```

---

## 💡 TDD 사이클 팁

### Red 단계
- 테스트를 먼저 작성하여 인터페이스 설계
- 실패하는 테스트 확인 (컴파일 에러 포함)
- 작은 단위로 테스트 작성 (5-10분)

### Green 단계
- 테스트 통과할 만큼만 구현
- 완벽한 구현보다 빠른 통과 우선
- 중복 코드 허용 (Refactor에서 제거)

### Refactor 단계
- 코드 개선 (중복 제거, 가독성, 성능)
- 테스트는 여전히 통과해야 함
- ArchUnit 규칙 준수 확인

### Tidy 단계
- TestFixture 정리 (Object Mother 패턴)
- 테스트 코드 가독성 개선
- 다음 사이클을 위한 준비
