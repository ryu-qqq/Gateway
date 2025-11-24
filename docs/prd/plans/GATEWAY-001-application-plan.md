# GATEWAY-001 TDD Plan - Application Layer

**Task**: JWT 인증 기능 - Application Layer
**Layer**: Application
**브랜치**: feature/GATEWAY-001-jwt-authentication
**예상 소요 시간**: 150분 (10 사이클 × 15분)
**Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ ValidateJwtCommand DTO (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/dto/ValidateJwtCommandTest.java` 파일 생성
- [ ] `shouldCreateCommandWithValidAccessToken()` 테스트 작성
- [ ] `shouldRejectNullAccessToken()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: ValidateJwtCommand DTO 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/dto/ValidateJwtCommand.java` 생성 (Record)
- [ ] 필드: `accessToken` (String)
- [ ] Compact Constructor에서 null 검증
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: ValidateJwtCommand DTO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] DTO Record ArchUnit 테스트 추가: `ValidateJwtCommandArchUnitTest.java`
  - Record 타입 검증
  - Command 패키지 위치 검증 (*.dto.command)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: ValidateJwtCommand DTO ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/fixtures/ValidateJwtCommandFixture.java` 생성
- [ ] `aValidValidateJwtCommand()` 메서드 작성
- [ ] `ValidateJwtCommandTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: ValidateJwtCommandFixture 정리 (Tidy)`

---

### 2️⃣ ValidateJwtResponse DTO (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/dto/ValidateJwtResponseTest.java` 파일 생성
- [ ] `shouldCreateResponseWithValidData()` 테스트 작성
- [ ] `shouldContainJwtClaimsAndValidFlag()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: ValidateJwtResponse DTO 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/dto/ValidateJwtResponse.java` 생성 (Record)
- [ ] 필드: `jwtClaims` (JwtClaims), `isValid` (boolean)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: ValidateJwtResponse DTO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] DTO Record ArchUnit 테스트에 ValidateJwtResponse 추가
- [ ] Response 패키지 위치 검증 (*.dto.response)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: ValidateJwtResponse DTO ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `ValidateJwtCommandFixture`에 `aValidValidateJwtResponse()` 메서드 추가
- [ ] `ValidateJwtResponseTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: ValidateJwtCommandFixture에 Response 추가 (Tidy)`

---

### 3️⃣ GetPublicKeyQuery DTO (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/dto/GetPublicKeyQueryTest.java` 파일 생성
- [ ] `shouldCreateQueryWithValidKid()` 테스트 작성
- [ ] `shouldRejectNullKid()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: GetPublicKeyQuery DTO 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/dto/GetPublicKeyQuery.java` 생성 (Record)
- [ ] 필드: `kid` (String)
- [ ] Compact Constructor에서 null 검증
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: GetPublicKeyQuery DTO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] DTO Record ArchUnit 테스트에 GetPublicKeyQuery 추가
- [ ] Query 패키지 위치 검증 (*.dto.query)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: GetPublicKeyQuery DTO ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/fixtures/GetPublicKeyQueryFixture.java` 생성
- [ ] `aValidGetPublicKeyQuery()` 메서드 작성
- [ ] `GetPublicKeyQueryTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: GetPublicKeyQueryFixture 정리 (Tidy)`

---

### 4️⃣ GetPublicKeyResponse DTO (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/dto/GetPublicKeyResponseTest.java` 파일 생성
- [ ] `shouldCreateResponseWithValidPublicKey()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: GetPublicKeyResponse DTO 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/dto/GetPublicKeyResponse.java` 생성 (Record)
- [ ] 필드: `publicKey` (PublicKey)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: GetPublicKeyResponse DTO 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] DTO Record ArchUnit 테스트에 GetPublicKeyResponse 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: GetPublicKeyResponse DTO ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `GetPublicKeyQueryFixture`에 `aValidGetPublicKeyResponse()` 메서드 추가
- [ ] `GetPublicKeyResponseTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: GetPublicKeyQueryFixture에 Response 추가 (Tidy)`

---

### 5️⃣ JwtValidationPort (Out Port) (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/port/out/JwtValidationPortTest.java` 파일 생성
- [ ] Mock 구현체로 `verifySignature()` 동작 테스트 작성
- [ ] Mock 구현체로 `extractClaims()` 동작 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtValidationPort 인터페이스 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/JwtValidationPort.java` 생성 (Interface)
- [ ] `Mono<Boolean> verifySignature(String accessToken, PublicKey publicKey)` 메서드 선언
- [ ] `Mono<JwtClaims> extractClaims(String accessToken)` 메서드 선언
- [ ] 테스트 실행 → 통과 확인 (Mock 구현)
- [ ] 커밋: `feat: JwtValidationPort Out Port 정의 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Port ArchUnit 테스트 추가: `JwtValidationPortArchUnitTest.java`
  - Interface 타입 검증
  - 패키지 위치 검증 (*.port.out)
  - Reactive 타입 (Mono/Flux) 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtValidationPort ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/fixtures/JwtValidationPortFixture.java` 생성
- [ ] Mock Port 구현체 Factory 메서드 작성
- [ ] `JwtValidationPortTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtValidationPortFixture 정리 (Tidy)`

---

### 6️⃣ PublicKeyPort (Out Port) (Cycle 6)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/port/out/PublicKeyPortTest.java` 파일 생성
- [ ] Mock 구현체로 `getPublicKey(String kid)` 동작 테스트 작성
- [ ] Mock 구현체로 `refreshPublicKeys()` 동작 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyPort 인터페이스 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/PublicKeyPort.java` 생성 (Interface)
- [ ] `Mono<PublicKey> getPublicKey(String kid)` 메서드 선언
- [ ] `Mono<Void> refreshPublicKeys()` 메서드 선언
- [ ] 테스트 실행 → 통과 확인 (Mock 구현)
- [ ] 커밋: `feat: PublicKeyPort Out Port 정의 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Port ArchUnit 테스트에 PublicKeyPort 추가
- [ ] Reactive 타입 (Mono/Flux) 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyPort ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyPortFixture.java` 생성 (Mock Port 구현체)
- [ ] `PublicKeyPortTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyPortFixture 정리 (Tidy)`

---

### 7️⃣ ValidateJwtUseCase (Command UseCase) (Cycle 7)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/usecase/ValidateJwtUseCaseTest.java` 파일 생성
- [ ] Mock Port로 `validateJwt()` 성공 시나리오 테스트 작성
- [ ] Mock Port로 JWT 만료 시나리오 테스트 작성 (JwtExpiredException)
- [ ] Mock Port로 JWT 서명 실패 시나리오 테스트 작성 (JwtInvalidException)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: ValidateJwtUseCase 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/usecase/ValidateJwtUseCase.java` 생성
- [ ] `@Component` 추가 (Spring Bean)
- [ ] `@RequiredArgsConstructor` 대신 생성자 직접 작성 (Lombok 금지)
- [ ] `JwtValidationPort`, `PublicKeyPort` 의존성 주입
- [ ] `Mono<ValidateJwtResponse> validateJwt(ValidateJwtCommand command)` 메서드 구현
  1. Public Key 조회 (kid 기반)
  2. Signature 검증
  3. Expiration 검증
  4. Claims 추출
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: ValidateJwtUseCase 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] `@Transactional` 불필요 확인 (읽기 전용 UseCase)
- [ ] UseCase ArchUnit 테스트 추가: `ValidateJwtUseCaseArchUnitTest.java`
  - `@Component` 어노테이션 검증
  - Port만 의존 (Adapter 의존 금지)
  - Transaction 경계 검증 (읽기 전용 → @Transactional 없음)
- [ ] Reactive Error Handling (`onErrorResume()`) 적용
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: ValidateJwtUseCase ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `ValidateJwtUseCaseFixture.java` 생성
- [ ] Mock UseCase Factory 메서드 작성
- [ ] `ValidateJwtUseCaseTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: ValidateJwtUseCaseFixture 정리 (Tidy)`

---

### 8️⃣ GetPublicKeyUseCase (Query UseCase) (Cycle 8)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/usecase/GetPublicKeyUseCaseTest.java` 파일 생성
- [ ] Mock Port로 `getPublicKey()` 성공 시나리오 테스트 작성
- [ ] Mock Port로 Public Key 없음 시나리오 테스트 작성 (PublicKeyNotFoundException)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: GetPublicKeyUseCase 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/usecase/GetPublicKeyUseCase.java` 생성
- [ ] `@Component` 추가
- [ ] 생성자 직접 작성 (Lombok 금지)
- [ ] `PublicKeyPort` 의존성 주입
- [ ] `Mono<GetPublicKeyResponse> getPublicKey(GetPublicKeyQuery query)` 메서드 구현
  - PublicKeyPort.getPublicKey(kid) 호출
  - GetPublicKeyResponse로 변환
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: GetPublicKeyUseCase 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] UseCase ArchUnit 테스트에 GetPublicKeyUseCase 추가
- [ ] Reactive Error Handling 적용
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: GetPublicKeyUseCase ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `GetPublicKeyUseCaseFixture.java` 생성
- [ ] `GetPublicKeyUseCaseTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: GetPublicKeyUseCaseFixture 정리 (Tidy)`

---

### 9️⃣ JwtAssembler (Cycle 9)

#### 🔴 Red: 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/assembler/JwtAssemblerTest.java` 파일 생성
- [ ] `shouldConvertValidateJwtResponseToJwtClaims()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtAssembler 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `application/src/main/java/com/ryuqq/connectly/gateway/application/assembler/JwtAssembler.java` 생성
- [ ] `@Component` 추가
- [ ] `JwtClaims toJwtClaims(ValidateJwtResponse response)` 메서드 구현
  - ValidateJwtResponse → JwtClaims 변환
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtAssembler 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Assembler ArchUnit 테스트 추가: `JwtAssemblerArchUnitTest.java`
  - `@Component` 어노테이션 검증
  - 패키지 위치 검증 (*.assembler)
  - 정적 메서드 또는 인스턴스 메서드 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtAssembler ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtAssemblerFixture.java` 생성
- [ ] `JwtAssemblerTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtAssemblerFixture 정리 (Tidy)`

---

### 🔟 Application Layer 통합 검증 (Cycle 10)

#### 🔴 Red: 통합 테스트 작성
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/application/ApplicationLayerIntegrationTest.java` 파일 생성
- [ ] ValidateJwtUseCase + JwtAssembler 통합 테스트 작성
- [ ] GetPublicKeyUseCase + PublicKeyPort 통합 테스트 작성
- [ ] 테스트 실행 → 실패 확인 (통합 시나리오)
- [ ] 커밋: `test: Application Layer 통합 테스트 추가 (Red)`

#### 🟢 Green: 통합 시나리오 구현
- [ ] UseCase 간 연동 검증
- [ ] Port 의존성 역전 확인
- [ ] Reactive Pipeline 정상 동작 확인 (Mono/Flux)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Application Layer 통합 시나리오 구현 (Green)`

#### ♻️ Refactor: 전체 Application ArchUnit 검증
- [ ] `application/src/test/java/com/ryuqq/connectly/gateway/architecture/ApplicationLayerArchUnitTest.java` 생성
- [ ] Transaction 경계 검증 (읽기 전용 UseCase는 @Transactional 없음)
- [ ] Port 의존성 역전 검증 (UseCase는 Port만 의존)
- [ ] DTO Record 타입 검증 (모든 DTO는 Record)
- [ ] Reactive 타입 사용 검증 (Mono/Flux)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Application Layer 전체 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: 모든 Fixture 통합 정리
- [ ] 모든 Fixture 파일 통합 검토
- [ ] 중복 메서드 제거
- [ ] Fixture 간 의존성 최소화
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Application Layer 모든 Fixture 통합 정리 (Tidy)`

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (10 사이클, 40개 단계 모두 ✅)
- [ ] UseCase 2개 구현 완료 (ValidateJwtUseCase, GetPublicKeyUseCase)
- [ ] Port 2개 정의 완료 (JwtValidationPort, PublicKeyPort)
- [ ] DTO 4개 구현 완료 (ValidateJwtCommand, ValidateJwtResponse, GetPublicKeyQuery, GetPublicKeyResponse)
- [ ] Assembler 1개 구현 완료 (JwtAssembler)
- [ ] 모든 Unit 테스트 통과
- [ ] Application Layer ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수 (Transaction 경계, Port 의존성 역전)
- [ ] TestFixture 모두 정리 (Object Mother 패턴)
- [ ] 테스트 커버리지 > 90%

---

## 🔗 관련 문서

- **Task**: docs/prd/tasks/GATEWAY-001-jwt-authentication.md
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Application 규칙**: docs/coding_convention/03-application-layer/
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🎯 Zero-Tolerance 규칙 체크리스트

### Application Layer 필수 준수 사항
- [ ] ✅ Transaction 경계: 읽기 전용 UseCase는 `@Transactional` 불필요
- [ ] ✅ Port 의존성 역전: UseCase는 Port만 의존 (Adapter 의존 금지)
- [ ] ✅ DTO는 Record 타입만 사용
- [ ] ✅ Reactive Programming: Mono/Flux 사용 필수
- [ ] ✅ Blocking Call 절대 금지 (WebClient 필수, RestTemplate 금지)

---

## 📊 진행 상황 추적

**완료된 사이클**: 0 / 10
**예상 남은 시간**: 150분

**다음 단계**: `/kb/application/go` 명령으로 TDD 사이클 시작
