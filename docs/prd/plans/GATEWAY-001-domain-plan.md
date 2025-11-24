# GATEWAY-001 TDD Plan - Domain Layer

**Task**: JWT 인증 기능 - Domain Layer
**Layer**: Domain
**브랜치**: feature/GATEWAY-001-jwt-authentication
**예상 소요 시간**: 120분 (8 사이클 × 15분)
**Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ JwtToken Aggregate Root (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/JwtTokenTest.java` 파일 생성
- [ ] `shouldCreateJwtTokenWithValidData()` 테스트 작성
- [ ] `shouldValidateTokenNotExpired()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtToken Aggregate 생성 및 만료 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/JwtToken.java` 생성 (Plain Java, Lombok 금지)
- [ ] 필드: `accessToken`, `expiresAt`, `createdAt` 추가
- [ ] 생성자 + Getter 작성
- [ ] `isExpired()` 메서드 구현 (expiresAt < now)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtToken Aggregate 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 불변성 보장 (`final` 필드)
- [ ] Law of Demeter 준수 확인
- [ ] Domain ArchUnit 테스트 추가: `JwtTokenArchUnitTest.java`
  - Lombok 사용 금지 검증
  - 외부 의존성 없는지 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtToken Aggregate 불변성 및 ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/fixtures/JwtTokenFixture.java` 생성
- [ ] `aValidJwtToken()` 메서드 작성 (유효한 토큰)
- [ ] `anExpiredJwtToken()` 메서드 작성 (만료된 토큰)
- [ ] `JwtTokenTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtTokenFixture 정리 (Tidy)`

---

### 2️⃣ JwtClaims Aggregate Root (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/JwtClaimsTest.java` 파일 생성
- [ ] `shouldCreateJwtClaimsWithValidData()` 테스트 작성
- [ ] `shouldThrowExceptionWhenUserIdIsNull()` 테스트 작성
- [ ] `shouldThrowExceptionWhenTenantIdIsNull()` 테스트 작성 (필수 필드 검증)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtClaims Aggregate 필수 필드 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/JwtClaims.java` 생성 (Plain Java)
- [ ] 필드: `userId`, `tenantId`, `permissionHash`, `roles`, `issuedAt`, `expiresAt` 추가
- [ ] 생성자에서 `userId`, `tenantId` null 검증 (필수 필드)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtClaims Aggregate 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 불변성 보장 (`final` 필드)
- [ ] `roles`를 `Set<String>`으로 변경 (중복 방지)
- [ ] Domain ArchUnit 테스트에 JwtClaims 검증 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtClaims Aggregate 불변성 및 Set 타입 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/fixtures/JwtClaimsFixture.java` 생성
- [ ] `aValidJwtClaims()` 메서드 작성
- [ ] `aJwtClaimsWithRoles(Set<String> roles)` 메서드 작성
- [ ] `JwtClaimsTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtClaimsFixture 정리 (Tidy)`

---

### 3️⃣ AccessToken Value Object (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/AccessTokenTest.java` 파일 생성
- [ ] `shouldCreateAccessTokenWithValidFormat()` 테스트 작성
- [ ] `shouldThrowExceptionWhenInvalidFormat()` 테스트 작성 (3 parts 검증)
- [ ] `shouldThrowExceptionWhenNullValue()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: AccessToken VO JWT 형식 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/AccessToken.java` 생성 (Record)
- [ ] 필드: `value` (String)
- [ ] Compact Constructor에서 JWT 형식 검증 (3 parts: header.payload.signature)
- [ ] 정규식: `^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$`
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: AccessToken VO JWT 형식 검증 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] JWT 정규식을 상수로 추출 (`JWT_PATTERN`)
- [ ] 검증 로직 메서드 추출: `validateFormat(String value)`
- [ ] VO ArchUnit 테스트 추가: `AccessTokenArchUnitTest.java`
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: AccessToken VO 검증 로직 메서드 추출 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/fixtures/AccessTokenFixture.java` 생성
- [ ] `aValidAccessToken()` 메서드 작성
- [ ] `anInvalidAccessToken()` 메서드 작성
- [ ] `AccessTokenTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: AccessTokenFixture 정리 (Tidy)`

---

### 4️⃣ PublicKey Value Object (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/PublicKeyTest.java` 파일 생성
- [ ] `shouldCreatePublicKeyWithValidData()` 테스트 작성
- [ ] `shouldThrowExceptionWhenKidIsInvalid()` 테스트 작성 (kid 형식 검증)
- [ ] `shouldThrowExceptionWhenPublicKeyIsNull()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKey VO kid 형식 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/PublicKey.java` 생성 (Plain Java)
- [ ] 필드: `kid` (String), `publicKey` (RSAPublicKey)
- [ ] 생성자에서 kid 형식 검증 (예: "key-YYYY-MM-DD")
- [ ] 정규식: `^key-\\d{4}-\\d{2}-\\d{2}$`
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKey VO kid 형식 검증 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] kid 정규식을 상수로 추출 (`KID_PATTERN`)
- [ ] 불변성 보장 (`final` 필드)
- [ ] VO ArchUnit 테스트에 PublicKey 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKey VO 검증 로직 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/fixtures/PublicKeyFixture.java` 생성
- [ ] `aValidPublicKey()` 메서드 작성 (Mock RSAPublicKey 사용)
- [ ] `aPublicKeyWithKid(String kid)` 메서드 작성
- [ ] `PublicKeyTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyFixture 정리 (Tidy)`

---

### 5️⃣ JwtExpiredException (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/exception/JwtExpiredExceptionTest.java` 파일 생성
- [ ] `shouldCreateExceptionWithMessage()` 테스트 작성
- [ ] `shouldExtendRuntimeException()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtExpiredException 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/exception/JwtExpiredException.java` 생성
- [ ] `RuntimeException` 상속
- [ ] 생성자: `JwtExpiredException(String message)`
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtExpiredException 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Exception ArchUnit 테스트 추가: `JwtExceptionArchUnitTest.java`
  - RuntimeException 상속 검증
  - 패키지 위치 검증 (*.exception)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtExpiredException ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/fixtures/JwtExceptionFixture.java` 생성
- [ ] `aJwtExpiredException()` 메서드 작성
- [ ] `JwtExpiredExceptionTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtExceptionFixture 정리 (Tidy)`

---

### 6️⃣ JwtInvalidException (Cycle 6)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/exception/JwtInvalidExceptionTest.java` 파일 생성
- [ ] `shouldCreateExceptionWithMessage()` 테스트 작성
- [ ] `shouldExtendRuntimeException()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtInvalidException 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/exception/JwtInvalidException.java` 생성
- [ ] `RuntimeException` 상속
- [ ] 생성자: `JwtInvalidException(String message)`
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtInvalidException 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Exception ArchUnit 테스트에 JwtInvalidException 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtInvalidException ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtExceptionFixture`에 `aJwtInvalidException()` 메서드 추가
- [ ] `JwtInvalidExceptionTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtExceptionFixture에 JwtInvalidException 추가 (Tidy)`

---

### 7️⃣ PublicKeyNotFoundException (Cycle 7)

#### 🔴 Red: 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/exception/PublicKeyNotFoundExceptionTest.java` 파일 생성
- [ ] `shouldCreateExceptionWithMessage()` 테스트 작성
- [ ] `shouldExtendRuntimeException()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyNotFoundException 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `domain/src/main/java/com/ryuqq/connectly/gateway/domain/jwt/exception/PublicKeyNotFoundException.java` 생성
- [ ] `RuntimeException` 상속
- [ ] 생성자: `PublicKeyNotFoundException(String message)`
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyNotFoundException 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Exception ArchUnit 테스트에 PublicKeyNotFoundException 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyNotFoundException ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtExceptionFixture`에 `aPublicKeyNotFoundException()` 메서드 추가
- [ ] `PublicKeyNotFoundExceptionTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtExceptionFixture에 PublicKeyNotFoundException 추가 (Tidy)`

---

### 8️⃣ Domain Layer 통합 검증 (Cycle 8)

#### 🔴 Red: 통합 테스트 작성
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/domain/jwt/JwtDomainIntegrationTest.java` 파일 생성
- [ ] `shouldValidateExpiredTokenAndThrowException()` 테스트 작성 (JwtToken + JwtExpiredException 통합)
- [ ] `shouldValidateJwtClaimsMandatoryFields()` 테스트 작성 (JwtClaims 필수 필드)
- [ ] 테스트 실행 → 실패 확인 (통합 시나리오)
- [ ] 커밋: `test: Domain Layer 통합 테스트 추가 (Red)`

#### 🟢 Green: 통합 시나리오 구현
- [ ] JwtToken과 JwtClaims 간 연동 검증
- [ ] 만료된 토큰 → JwtExpiredException 발생 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Domain Layer 통합 시나리오 구현 (Green)`

#### ♻️ Refactor: 전체 Domain ArchUnit 검증
- [ ] `domain/src/test/java/com/ryuqq/connectly/gateway/architecture/DomainLayerArchUnitTest.java` 생성
- [ ] Lombok 사용 금지 검증 (모든 Domain 클래스)
- [ ] Law of Demeter 준수 검증
- [ ] 외부 의존성 없는지 검증 (Spring, Lombok, 외부 라이브러리)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Domain Layer 전체 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: 모든 Fixture 통합 정리
- [ ] 모든 Fixture 파일 통합 검토
- [ ] 중복 메서드 제거
- [ ] Fixture 간 의존성 최소화
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Domain Layer 모든 Fixture 통합 정리 (Tidy)`

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (8 사이클, 32개 단계 모두 ✅)
- [ ] Aggregate 2개 구현 완료 (JwtToken, JwtClaims)
- [ ] Value Object 2개 구현 완료 (AccessToken, PublicKey)
- [ ] Exception 3개 구현 완료 (JwtExpiredException, JwtInvalidException, PublicKeyNotFoundException)
- [ ] 모든 Unit 테스트 통과
- [ ] Domain Layer ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수 (Lombok 금지, Law of Demeter, 외부 의존성 없음)
- [ ] TestFixture 모두 정리 (Object Mother 패턴)
- [ ] 테스트 커버리지 > 90%

---

## 🔗 관련 문서

- **Task**: docs/prd/tasks/GATEWAY-001-jwt-authentication.md
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Domain 규칙**: docs/coding_convention/02-domain-layer/
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🎯 Zero-Tolerance 규칙 체크리스트

### Domain Layer 필수 준수 사항
- [ ] ✅ Lombok 절대 금지 (Plain Java 또는 Record만 사용)
- [ ] ✅ Law of Demeter 준수 (Getter 체이닝 금지)
- [ ] ✅ 외부 의존성 절대 금지 (Spring, Lombok, 외부 라이브러리)
- [ ] ✅ 불변성 보장 (final 필드)
- [ ] ✅ Tell Don't Ask 패턴 적용

---

## 📊 진행 상황 추적

**완료된 사이클**: 0 / 8
**예상 남은 시간**: 120분

**다음 단계**: `/kb/domain/go` 명령으로 TDD 사이클 시작
