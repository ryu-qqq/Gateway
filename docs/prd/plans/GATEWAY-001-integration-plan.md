# GATEWAY-001 TDD Plan - Integration Test

**Task**: JWT 인증 기능 - Integration Test (E2E)
**Layer**: Integration Test
**브랜치**: feature/GATEWAY-001-jwt-authentication
**예상 소요 시간**: 90분 (6 사이클 × 15분)
**Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ Scenario 1: JWT 인증 성공 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/JwtAuthenticationSuccessTest.java` 파일 생성
- [ ] `@SpringBootTest` + WebTestClient
- [ ] Testcontainers Redis 설정
- [ ] WireMock AuthHub Mock Server 설정
- [ ] `shouldAuthenticateWithValidJwtAndPassToBackend()` 테스트 작성:
  - Given: 유효한 Access Token (RS256 서명), Public Key가 Redis에 캐시됨
  - When: `GET /api/v1/orders` 요청 (Authorization: Bearer {accessToken})
  - Then: 200 OK, Backend Service로 요청 전달됨
  - 검증: JwtAuthenticationFilter 통과, ServerWebExchange Attribute에 jwtClaims 저장
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: JWT 인증 성공 E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] 전체 스택 연동 (Domain → Application → Persistence → Filter)
- [ ] Redis Cache 동작 확인
- [ ] Exchange Attributes 설정 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JWT 인증 성공 E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Integration Test ArchUnit 테스트 추가: `JwtAuthenticationIntegrationArchUnitTest.java`
  - WebTestClient 사용 검증
  - MockMvc 금지 검증
  - Testcontainers 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JWT 인증 성공 E2E ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/fixtures/JwtTestFixture.java` 생성
- [ ] RS256 Private Key로 서명된 테스트용 JWT 생성 메서드 작성
- [ ] Claims 커스터마이징 가능한 Factory 메서드 작성
- [ ] `JwtAuthenticationSuccessTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtTestFixture 정리 (Tidy)`

---

### 2️⃣ Scenario 2: JWT 만료 → 401 Unauthorized (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/JwtExpiredTest.java` 파일 생성
- [ ] `shouldReturn401WhenJwtExpired()` 테스트 작성:
  - Given: 만료된 Access Token
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "JWT_EXPIRED" }`
  - 검증: JwtAuthenticationFilter에서 JwtExpiredException 발생
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: JWT 만료 E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] 만료된 JWT 생성 (expiresAt < now)
- [ ] Filter에서 JwtExpiredException 발생 확인
- [ ] Global Error Handler에서 401 반환 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JWT 만료 E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 에러 응답 포맷 검증 강화
- [ ] traceId 포함 여부 확인
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JWT 만료 E2E 에러 응답 검증 강화 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtTestFixture`에 `anExpiredJwt()` 메서드 추가
- [ ] `JwtExpiredTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtTestFixture에 만료 JWT 추가 (Tidy)`

---

### 3️⃣ Scenario 3: JWT 서명 검증 실패 → 401 Unauthorized (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/JwtInvalidSignatureTest.java` 파일 생성
- [ ] `shouldReturn401WhenJwtSignatureInvalid()` 테스트 작성:
  - Given: 잘못된 서명의 Access Token
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "JWT_INVALID" }`
  - 검증: JwtValidationPort에서 서명 검증 실패
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: JWT 서명 실패 E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] 잘못된 서명의 JWT 생성 (다른 Private Key 사용)
- [ ] JwtValidationAdapter에서 서명 검증 실패 확인
- [ ] JwtInvalidException 발생 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JWT 서명 실패 E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 서명 검증 로직 정확성 확인
- [ ] 에러 응답 포맷 일관성 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JWT 서명 실패 E2E 검증 강화 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtTestFixture`에 `aJwtWithInvalidSignature()` 메서드 추가
- [ ] `JwtInvalidSignatureTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtTestFixture에 잘못된 서명 JWT 추가 (Tidy)`

---

### 4️⃣ Scenario 4: Public Key Rotation (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/PublicKeyRotationTest.java` 파일 생성
- [ ] `shouldHandlePublicKeyRotation()` 테스트 작성:
  - Given: 현재 Public Key (kid="key-2025-01-01"), 새 Public Key (kid="key-2025-01-08")
  - When: JWKS 엔드포인트가 두 Key 모두 반환, 새 JWT (kid="key-2025-01-08")로 요청
  - Then: 200 OK, 새 Public Key로 검증 성공
  - 검증: PublicKeyPort.getPublicKey("key-2025-01-08") 성공
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: Public Key Rotation E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] WireMock JWKS 엔드포인트에서 여러 Public Key 반환
- [ ] 새 kid로 JWT 생성
- [ ] PublicKeyQueryAdapter에서 새 Public Key 조회 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Public Key Rotation E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Public Key Cache 동작 검증 강화
- [ ] kid 매칭 로직 정확성 확인
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Public Key Rotation E2E 검증 강화 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/fixtures/PublicKeyTestFixture.java` 생성
- [ ] JWKS 형식 Public Key 생성 메서드 작성
- [ ] 여러 Public Key 반환 Mock 메서드 작성
- [ ] `PublicKeyRotationTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyTestFixture 정리 (Tidy)`

---

### 5️⃣ Scenario 5: Public Key Cache Hit (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/PublicKeyCacheHitTest.java` 파일 생성
- [ ] `shouldUseCachedPublicKeyWithoutAuthHubCall()` 테스트 작성:
  - Given: Redis에 Public Key 캐시됨
  - When: 동일한 kid로 여러 번 요청
  - Then: 200 OK, AuthHub JWKS 호출 없이 Redis에서 조회
  - 검증: Redis Cache Hit 로그 확인, WireMock 호출 횟수 1회
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: Public Key Cache Hit E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] 첫 번째 요청: AuthHub 호출 → Redis 저장
- [ ] 두 번째 요청: Redis에서 조회 (AuthHub 호출 없음)
- [ ] WireMock 호출 횟수 검증
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Public Key Cache Hit E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Cache Hit/Miss 로그 검증 강화
- [ ] TTL 동작 확인
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Public Key Cache Hit E2E 검증 강화 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] Cache Hit/Miss 시나리오 Fixture 정리
- [ ] WireMock 호출 횟수 검증 메서드 추출
- [ ] `PublicKeyCacheHitTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Public Key Cache Hit Fixture 정리 (Tidy)`

---

### 6️⃣ Scenario 6: Public Key 수동 갱신 (Actuator) (Cycle 6)

#### 🔴 Red: 테스트 작성
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/integration/PublicKeyManualRefreshTest.java` 파일 생성
- [ ] `shouldRefreshPublicKeysManually()` 테스트 작성:
  - Given: Redis에 기존 Public Key 캐시됨
  - When: `POST /actuator/refresh-public-keys` 호출
  - Then: 200 OK, Redis 캐시 갱신 완료
  - 검증: PublicKeyPort.refreshPublicKeys() 실행, WireMock 호출 확인
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: Public Key 수동 갱신 E2E 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] PublicKeyRefreshController 엔드포인트 호출
- [ ] PublicKeyCommandAdapter.refreshPublicKeys() 실행 확인
- [ ] Redis 캐시 갱신 확인 (deleteAll + save)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Public Key 수동 갱신 E2E 시나리오 구현 (Green)`

#### ♻️ Refactor: Integration Test 전체 검증
- [ ] `integration/src/test/java/com/ryuqq/connectly/gateway/architecture/IntegrationTestArchUnitTest.java` 생성
- [ ] WebTestClient 사용 검증 (TestRestTemplate 금지)
- [ ] MockMvc 금지 검증
- [ ] Testcontainers 사용 검증 (Redis)
- [ ] WireMock 사용 검증 (AuthHub)
- [ ] StepVerifier 사용 검증 (Reactor 테스트)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Integration Test 전체 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: 모든 Fixture 통합 정리
- [ ] 모든 Integration Test Fixture 통합 검토
- [ ] Testcontainers 설정 공통화
- [ ] WireMock 설정 공통화
- [ ] 중복 메서드 제거
- [ ] 모든 E2E 테스트 여전히 통과 확인
- [ ] 커밋: `test: Integration Test 모든 Fixture 통합 정리 (Tidy)`

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (6 사이클, 24개 단계 모두 ✅)
- [ ] E2E Scenario 6개 구현 완료
  - Scenario 1: JWT 인증 성공
  - Scenario 2: JWT 만료 → 401
  - Scenario 3: JWT 서명 실패 → 401
  - Scenario 4: Public Key Rotation
  - Scenario 5: Public Key Cache Hit
  - Scenario 6: Public Key 수동 갱신
- [ ] TestFixture 2개 구현 완료 (JwtTestFixture, PublicKeyTestFixture)
- [ ] 모든 Integration 테스트 통과
- [ ] Integration Test ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수 (WebTestClient, MockMvc 금지, Testcontainers)
- [ ] 전체 스택 연동 검증 완료
- [ ] 테스트 커버리지 > 90%

---

## 🔗 관련 문서

- **Task**: docs/prd/tasks/GATEWAY-001-jwt-authentication.md
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Integration 규칙**: docs/coding_convention/05-testing/integration-testing/
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🎯 Zero-Tolerance 규칙 체크리스트

### Integration Test 필수 준수 사항
- [ ] ✅ WebTestClient 사용 (TestRestTemplate 대체 - Reactive 표준)
- [ ] ✅ MockMvc 금지 (이미 명시됨)
- [ ] ✅ Testcontainers 사용 (Redis)
- [ ] ✅ WireMock 사용 (AuthHub Mock)
- [ ] ✅ StepVerifier 사용 (Reactor 테스트)
- [ ] ✅ E2E 시나리오 완전성 (전체 스택 연동)

---

## 📊 진행 상황 추적

**완료된 사이클**: 0 / 6
**예상 남은 시간**: 90분

**다음 단계**: `/kb/integration/go` 명령으로 TDD 사이클 시작

---

## 🧪 Testcontainers 설정

### Redis Testcontainers
```java
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);

@DynamicPropertySource
static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
}
```

### WireMock AuthHub Mock Server
```java
@WireMockTest(httpPort = 8888)
class JwtAuthenticationSuccessTest {

    @BeforeEach
    void setupWireMock() {
        stubFor(get(urlEqualTo("/api/v1/auth/jwks"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(jwksResponse)));
    }
}
```

---

## 🔑 JWT Test Fixture 예시

```java
public class JwtTestFixture {

    private static final RSAPrivateKey PRIVATE_KEY = generateRSAPrivateKey();
    private static final RSAPublicKey PUBLIC_KEY = generateRSAPublicKey();

    public static String aValidJwt() {
        return createJwt(
            "user-123",
            "tenant-abc",
            "permission-hash-xyz",
            Set.of("ROLE_USER"),
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
    }

    public static String anExpiredJwt() {
        return createJwt(
            "user-123",
            "tenant-abc",
            "permission-hash-xyz",
            Set.of("ROLE_USER"),
            Instant.now().minus(1, ChronoUnit.HOURS) // 만료됨
        );
    }

    public static String aJwtWithInvalidSignature() {
        RSAPrivateKey wrongKey = generateWrongRSAPrivateKey();
        return createJwtWithKey(wrongKey, "user-123", "tenant-abc", ...);
    }
}
```
