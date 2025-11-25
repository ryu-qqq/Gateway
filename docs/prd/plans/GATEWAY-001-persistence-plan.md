# GATEWAY-001 TDD Plan - Persistence Layer (Redis)

**Task**: JWT 인증 기능 - Persistence Layer (Redis)
**Layer**: Persistence (Redis)
**브랜치**: feature/GATEWAY-001-jwt-authentication
**예상 소요 시간**: 135분 (9 사이클 × 15분)
**Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ PublicKeyEntity (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/entity/PublicKeyEntityTest.java` 파일 생성
- [ ] `shouldCreatePublicKeyEntityWithValidData()` 테스트 작성
- [ ] `shouldSerializeToJson()` 테스트 작성 (Redis 저장 시 JSON 직렬화)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyEntity JSON 직렬화 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/entity/PublicKeyEntity.java` 생성 (Plain Java, Lombok 금지)
- [ ] 필드: `kid`, `modulus`, `exponent`, `kty`, `use`, `alg` 추가
- [ ] 생성자 + Getter 작성 (Lombok 금지)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyEntity 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] 불변성 보장 (`final` 필드)
- [ ] Entity ArchUnit 테스트 추가: `PublicKeyEntityArchUnitTest.java`
  - Lombok 사용 금지 검증
  - Plain Java 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyEntity 불변성 및 ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/fixtures/PublicKeyEntityFixture.java` 생성
- [ ] `aValidPublicKeyEntity()` 메서드 작성
- [ ] `aPublicKeyEntityWithKid(String kid)` 메서드 작성
- [ ] `PublicKeyEntityTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyEntityFixture 정리 (Tidy)`

---

### 2️⃣ PublicKeyMapper (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/mapper/PublicKeyMapperTest.java` 파일 생성
- [ ] `shouldMapEntityToPublicKey()` 테스트 작성
- [ ] `shouldMapPublicKeyToEntity()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyMapper 양방향 매핑 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/mapper/PublicKeyMapper.java` 생성
- [ ] `@Component` 추가
- [ ] `PublicKey toPublicKey(PublicKeyEntity entity)` 메서드 구현
  - Base64 Modulus/Exponent → RSAPublicKey 변환
- [ ] `PublicKeyEntity toPublicKeyEntity(PublicKey publicKey)` 메서드 구현
  - RSAPublicKey → Base64 Modulus/Exponent 변환
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyMapper 양방향 매핑 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Mapper ArchUnit 테스트 추가: `PublicKeyMapperArchUnitTest.java`
  - `@Component` 어노테이션 검증
  - 패키지 위치 검증 (*.mapper)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyMapper ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyMapperFixture.java` 생성
- [ ] `PublicKeyMapperTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyMapperFixture 정리 (Tidy)`

---

### 3️⃣ PublicKeyRedisRepository (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/repository/PublicKeyRedisRepositoryTest.java` 파일 생성 (@DataRedisTest)
- [ ] Testcontainers Redis 설정
- [ ] `shouldSavePublicKeyWithTTL()` 테스트 작성
- [ ] `shouldFindPublicKeyByKid()` 테스트 작성
- [ ] `shouldReturnEmptyWhenKidNotFound()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyRedisRepository 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/repository/PublicKeyRedisRepository.java` 생성
- [ ] `@Repository` 추가
- [ ] `ReactiveRedisTemplate<String, PublicKeyEntity>` 의존성 주입
- [ ] `Mono<Void> save(String kid, PublicKeyEntity publicKey, Duration ttl)` 메서드 구현
  - Redis Key: `authhub:jwt:publickey:{kid}`
  - TTL: 1시간
- [ ] `Mono<PublicKeyEntity> findByKid(String kid)` 메서드 구현
- [ ] `Mono<Void> deleteAll()` 메서드 구현
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyRedisRepository 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Redis Key 상수 추출: `PUBLIC_KEY_PREFIX = "authhub:jwt:publickey"`
- [ ] Repository ArchUnit 테스트 추가: `PublicKeyRedisRepositoryArchUnitTest.java`
  - `@Repository` 어노테이션 검증
  - ReactiveRedisTemplate 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyRedisRepository 상수 추출 및 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyRedisRepositoryFixture.java` 생성
- [ ] Testcontainers Redis 설정 공통화
- [ ] `PublicKeyRedisRepositoryTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyRedisRepositoryFixture 정리 (Tidy)`

---

### 4️⃣ PublicKeyQueryAdapter (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/PublicKeyQueryAdapterTest.java` 파일 생성
- [ ] Mock Repository로 `getPublicKey()` Cache Hit 시나리오 테스트 작성
- [ ] Mock Repository로 `getPublicKey()` Cache Miss 시나리오 테스트 작성 (AuthHub 호출)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyQueryAdapter Cache Hit/Miss 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/PublicKeyQueryAdapter.java` 생성
- [ ] `@Component` 추가
- [ ] `PublicKeyPort` 구현
- [ ] `PublicKeyRedisRepository`, `AuthHubPort`, `PublicKeyMapper` 의존성 주입
- [ ] `Mono<PublicKey> getPublicKey(String kid)` 메서드 구현
  1. Redis 조회 (Cache Hit → 즉시 반환)
  2. Cache Miss → AuthHub JWKS 호출 → Redis 저장
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyQueryAdapter Cache 전략 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Query Adapter ArchUnit 테스트 추가: `PublicKeyQueryAdapterArchUnitTest.java`
  - `@Component` 어노테이션 검증
  - PublicKeyPort 구현 검증
  - Reactive 타입 (Mono/Flux) 사용 검증
- [ ] Reactive Error Handling 적용 (`onErrorResume()`)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyQueryAdapter ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyQueryAdapterFixture.java` 생성
- [ ] Mock Adapter Factory 메서드 작성
- [ ] `PublicKeyQueryAdapterTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyQueryAdapterFixture 정리 (Tidy)`

---

### 5️⃣ PublicKeyCommandAdapter (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/PublicKeyCommandAdapterTest.java` 파일 생성
- [ ] Mock Repository로 `refreshPublicKeys()` 테스트 작성 (기존 캐시 삭제 + 전체 교체)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyCommandAdapter refreshPublicKeys 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/PublicKeyCommandAdapter.java` 생성
- [ ] `@Component` 추가
- [ ] `PublicKeyPort` 구현
- [ ] `PublicKeyRedisRepository`, `AuthHubPort`, `PublicKeyMapper` 의존성 주입
- [ ] `Mono<Void> refreshPublicKeys()` 메서드 구현
  1. AuthHub JWKS 엔드포인트 호출
  2. 기존 캐시 삭제 (`deleteAll()`)
  3. 모든 Public Key를 Redis에 저장
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyCommandAdapter refreshPublicKeys 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Command Adapter ArchUnit 테스트 추가: `PublicKeyCommandAdapterArchUnitTest.java`
- [ ] Reactive Error Handling 적용
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyCommandAdapter ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyCommandAdapterFixture.java` 생성
- [ ] `PublicKeyCommandAdapterTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyCommandAdapterFixture 정리 (Tidy)`

---

### 6️⃣ JwtValidationAdapter (Cycle 6)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/JwtValidationAdapterTest.java` 파일 생성
- [ ] `shouldVerifySignatureWithValidKey()` 테스트 작성
- [ ] `shouldExtractClaimsFromValidToken()` 테스트 작성
- [ ] `shouldThrowExceptionWhenSignatureInvalid()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtValidationAdapter 서명 검증 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/adapter/JwtValidationAdapter.java` 생성
- [ ] `@Component` 추가
- [ ] `JwtValidationPort` 구현
- [ ] `Mono<Boolean> verifySignature(String accessToken, PublicKey publicKey)` 메서드 구현
  - JWT 라이브러리 (nimbus-jose-jwt 또는 jjwt) 사용
  - RS256 서명 검증
- [ ] `Mono<JwtClaims> extractClaims(String accessToken)` 메서드 구현
  - JWT Payload 파싱
  - JwtClaims 도메인 모델로 변환
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtValidationAdapter JWT 검증 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Adapter ArchUnit 테스트에 JwtValidationAdapter 추가
- [ ] Reactive Error Handling 적용 (JwtInvalidException, JwtExpiredException)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtValidationAdapter ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtValidationAdapterFixture.java` 생성
- [ ] Mock JWT 생성 메서드 작성 (테스트용 RS256 Private Key)
- [ ] `JwtValidationAdapterTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtValidationAdapterFixture 정리 (Tidy)`

---

### 7️⃣ Redis Configuration (Cycle 7)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/config/RedisConfigTest.java` 파일 생성
- [ ] `shouldConfigureReactiveRedisTemplate()` 테스트 작성
- [ ] `shouldConfigureConnectionPool()` 테스트 작성 (Lettuce)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: Redis Configuration 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/redis/src/main/java/com/ryuqq/connectly/gateway/adapter/out/redis/config/RedisConfig.java` 생성
- [ ] `@Configuration` 추가
- [ ] `ReactiveRedisTemplate<String, PublicKeyEntity>` Bean 정의
- [ ] Lettuce Connection Pool 설정
  - max-active: 16
  - max-idle: 8
  - min-idle: 4
  - max-wait: 1000ms
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Redis Configuration 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Configuration ArchUnit 테스트 추가
  - `@Configuration` 어노테이션 검증
- [ ] application.yml에서 Redis 설정 외부화
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Redis Configuration 외부화 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] Testcontainers Redis 설정 공통화 (모든 Redis 테스트에서 재사용)
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Testcontainers Redis 설정 공통화 (Tidy)`

---

### 8️⃣ AuthHubAdapter (WebClient) (Cycle 8)

#### 🔴 Red: 테스트 작성
- [ ] `adapter-out/authhub/src/test/java/com/ryuqq/connectly/gateway/adapter/out/authhub/AuthHubAdapterTest.java` 파일 생성
- [ ] WireMock으로 `/api/v1/auth/jwks` 엔드포인트 Mock
- [ ] `shouldGetPublicKeyFromJwks()` 테스트 작성
- [ ] `shouldRetryOnFailure()` 테스트 작성 (Resilience4j Retry)
- [ ] `shouldUseCachedKeyOnCircuitBreakerOpen()` 테스트 작성 (Circuit Breaker)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: AuthHubAdapter JWKS 조회 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `adapter-out/authhub/src/main/java/com/ryuqq/connectly/gateway/adapter/out/authhub/AuthHubAdapter.java` 생성
- [ ] `@Component` 추가
- [ ] `AuthHubPort` 구현
- [ ] `WebClient` 의존성 주입
- [ ] `Mono<String> getPublicKey()` 메서드 구현 (JWKS 엔드포인트 호출)
- [ ] Resilience4j Retry 설정 (최대 3회, Exponential Backoff)
- [ ] Resilience4j Circuit Breaker 설정
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: AuthHubAdapter JWKS 조회 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] WebClient Timeout 설정 (Connection: 3초, Response: 3초)
- [ ] Adapter ArchUnit 테스트 추가
- [ ] Reactive Error Handling 적용
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: AuthHubAdapter Timeout 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `AuthHubAdapterFixture.java` 생성
- [ ] WireMock 설정 공통화
- [ ] `AuthHubAdapterTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: AuthHubAdapterFixture 및 WireMock 설정 정리 (Tidy)`

---

### 9️⃣ Persistence Layer 통합 검증 (Cycle 9)

#### 🔴 Red: 통합 테스트 작성
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/adapter/out/redis/PersistenceLayerIntegrationTest.java` 파일 생성
- [ ] Testcontainers Redis 사용
- [ ] PublicKeyQueryAdapter + PublicKeyRedisRepository + AuthHubAdapter 통합 테스트 작성
- [ ] Cache Hit/Miss 시나리오 통합 검증
- [ ] 테스트 실행 → 실패 확인 (통합 시나리오)
- [ ] 커밋: `test: Persistence Layer 통합 테스트 추가 (Red)`

#### 🟢 Green: 통합 시나리오 구현
- [ ] Adapter 간 연동 검증
- [ ] Redis Cache 동작 확인
- [ ] AuthHub WebClient 호출 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Persistence Layer 통합 시나리오 구현 (Green)`

#### ♻️ Refactor: 전체 Persistence ArchUnit 검증
- [ ] `adapter-out/redis/src/test/java/com/ryuqq/connectly/gateway/architecture/PersistenceLayerArchUnitTest.java` 생성
- [ ] Lombok 금지 검증 (Entity는 Plain Java)
- [ ] Cache TTL 검증 (Public Key는 1시간)
- [ ] ReactiveRedisTemplate 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Persistence Layer 전체 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: 모든 Fixture 통합 정리
- [ ] 모든 Fixture 파일 통합 검토
- [ ] Testcontainers 설정 공통화
- [ ] WireMock 설정 공통화
- [ ] 중복 메서드 제거
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Persistence Layer 모든 Fixture 통합 정리 (Tidy)`

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (9 사이클, 36개 단계 모두 ✅)
- [ ] Entity 1개 구현 완료 (PublicKeyEntity)
- [ ] Repository 1개 구현 완료 (PublicKeyRedisRepository)
- [ ] Adapter 3개 구현 완료 (PublicKeyQueryAdapter, PublicKeyCommandAdapter, JwtValidationAdapter)
- [ ] Mapper 1개 구현 완료 (PublicKeyMapper)
- [ ] AuthHubAdapter 구현 완료 (WebClient + Resilience4j)
- [ ] 모든 Unit 테스트 통과
- [ ] Persistence Layer ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수 (Lombok 금지, Cache TTL)
- [ ] TestFixture 모두 정리 (Object Mother 패턴)
- [ ] Testcontainers 통합 테스트 통과
- [ ] 테스트 커버리지 > 90%

---

## 🔗 관련 문서

- **Task**: docs/prd/tasks/GATEWAY-001-jwt-authentication.md
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Persistence 규칙**: docs/coding_convention/04-persistence-layer/redis/
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🎯 Zero-Tolerance 규칙 체크리스트

### Persistence Layer 필수 준수 사항
- [ ] ✅ Lombok 금지 (Entity는 Plain Java 또는 Record)
- [ ] ✅ Cache TTL: Public Key는 1시간
- [ ] ✅ ReactiveRedisTemplate 사용 필수
- [ ] ✅ Blocking Call 절대 금지 (WebClient 필수, RestTemplate 금지)
- [ ] ✅ Testcontainers 사용 (실제 Redis)

---

## 📊 진행 상황 추적

**완료된 사이클**: 0 / 9
**예상 남은 시간**: 135분

**다음 단계**: `/kb/persistence/go` 명령으로 TDD 사이클 시작
