# GATEWAY-001 TDD Plan - Gateway Filter Layer

**Task**: JWT 인증 기능 - Gateway Filter Layer
**Layer**: Gateway Filter (Spring Cloud Gateway)
**브랜치**: feature/GATEWAY-001-jwt-authentication
**예상 소요 시간**: 105분 (7 사이클 × 15분)
**Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📝 TDD 사이클 체크리스트

### 1️⃣ GatewayFilterOrder 상수 정의 (Cycle 1)

#### 🔴 Red: 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/config/GatewayFilterOrderTest.java` 파일 생성
- [ ] `shouldDefineCorrectFilterOrder()` 테스트 작성 (JWT_AUTH_FILTER = 2)
- [ ] `shouldMaintainFilterSequence()` 테스트 작성 (0 → 1 → 2 → ...)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: GatewayFilterOrder 상수 정의 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `filter/src/main/java/com/ryuqq/connectly/gateway/filter/config/GatewayFilterOrder.java` 생성
- [ ] Filter Order 상수 정의:
  ```java
  public static final int HIGHEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;
  public static final int TRACE_ID_FILTER = HIGHEST_PRECEDENCE;           // 0
  public static final int RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 1;     // 1
  public static final int JWT_AUTH_FILTER = HIGHEST_PRECEDENCE + 2;       // 2
  public static final int TOKEN_REFRESH_FILTER = HIGHEST_PRECEDENCE + 3;  // 3
  public static final int TENANT_ISOLATION_FILTER = HIGHEST_PRECEDENCE + 4; // 4
  public static final int PERMISSION_FILTER = HIGHEST_PRECEDENCE + 5;     // 5
  public static final int MFA_VERIFICATION_FILTER = HIGHEST_PRECEDENCE + 6; // 6
  ```
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: GatewayFilterOrder 상수 정의 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] ArchUnit 테스트 추가: `GatewayFilterOrderArchUnitTest.java`
  - 모든 필드가 `public static final int` 타입인지 검증
  - 순차적 증가 검증 (0 → 1 → 2 → ...)
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: GatewayFilterOrder ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `GatewayFilterOrderFixture.java` 생성
- [ ] Filter Order 상수 테스트 Fixture 작성
- [ ] `GatewayFilterOrderTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: GatewayFilterOrderFixture 정리 (Tidy)`

---

### 2️⃣ JwtAuthenticationFilter (Cycle 2)

#### 🔴 Red: 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/JwtAuthenticationFilterTest.java` 파일 생성
- [ ] WebTestClient로 `shouldExtractBearerTokenFromAuthorizationHeader()` 테스트 작성
- [ ] `shouldReturn401WhenAuthorizationHeaderMissing()` 테스트 작성
- [ ] `shouldValidateJwtAndSetAttributes()` 테스트 작성 (Exchange Attributes 설정)
- [ ] `shouldSetMDCWithUserIdAndTenantId()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtAuthenticationFilter 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `filter/src/main/java/com/ryuqq/connectly/gateway/filter/JwtAuthenticationFilter.java` 생성
- [ ] `GlobalFilter` 구현, `Ordered` 구현
- [ ] `@Component` 추가
- [ ] `ValidateJwtUseCase` 의존성 주입
- [ ] `getOrder()` 메서드: `return GatewayFilterOrder.JWT_AUTH_FILTER;` (2)
- [ ] `filter()` 메서드 구현:
  1. Authorization 헤더에서 Bearer Token 추출
  2. ValidateJwtUseCase 호출 (JWT 검증)
  3. JwtClaims 추출
  4. ServerWebExchange Attribute 설정 (userId, tenantId, permissionHash, roles)
  5. MDC에 userId, tenantId 추가
  6. `doFinally()` 블록에서 MDC.clear()
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtAuthenticationFilter 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Filter Order 하드코딩 제거 확인 (GatewayFilterOrder 상수 사용)
- [ ] Reactive Error Handling 적용 (`onErrorResume()`)
- [ ] Filter ArchUnit 테스트 추가: `JwtAuthenticationFilterArchUnitTest.java`
  - `GlobalFilter` 구현 검증
  - `Ordered` 구현 검증
  - `@Component` 어노테이션 검증
  - Reactive 타입 (Mono/Flux) 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtAuthenticationFilter ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtAuthenticationFilterFixture.java` 생성
- [ ] Mock ServerWebExchange Factory 메서드 작성
- [ ] Mock JWT Factory 메서드 작성
- [ ] `JwtAuthenticationFilterTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtAuthenticationFilterFixture 정리 (Tidy)`

---

### 3️⃣ JwtErrorResponse (Cycle 3)

#### 🔴 Red: 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/error/JwtErrorResponseTest.java` 파일 생성
- [ ] `shouldCreateErrorResponseWithValidData()` 테스트 작성
- [ ] `shouldSerializeToJson()` 테스트 작성 (JSON 직렬화)
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtErrorResponse JSON 직렬화 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `filter/src/main/java/com/ryuqq/connectly/gateway/filter/error/JwtErrorResponse.java` 생성 (Record)
- [ ] 필드: `errorCode`, `message`, `timestamp`, `traceId` 추가
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtErrorResponse Record 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] DTO Record ArchUnit 테스트에 JwtErrorResponse 추가
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtErrorResponse ArchUnit 검증 추가 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtErrorResponseFixture.java` 생성
- [ ] `aJwtErrorResponse()` 메서드 작성
- [ ] `JwtErrorResponseTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtErrorResponseFixture 정리 (Tidy)`

---

### 4️⃣ JwtErrorHandler (Global Error Handler) (Cycle 4)

#### 🔴 Red: 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/error/JwtErrorHandlerTest.java` 파일 생성
- [ ] `shouldHandle401ForJwtExpiredException()` 테스트 작성
- [ ] `shouldHandle401ForJwtInvalidException()` 테스트 작성
- [ ] `shouldHandle500ForPublicKeyNotFoundException()` 테스트 작성
- [ ] `shouldIncludeTraceIdInErrorResponse()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: JwtErrorHandler 에러 응답 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `filter/src/main/java/com/ryuqq/connectly/gateway/filter/error/JwtErrorHandler.java` 생성
- [ ] `ErrorWebExceptionHandler` 구현
- [ ] `@Component` 추가
- [ ] `handle(ServerWebExchange exchange, Throwable ex)` 메서드 구현:
  1. traceId 추출 (exchange.getAttribute("traceId"))
  2. ErrorResponse 생성
  3. HTTP Status 설정 (401 or 500)
  4. JSON 응답 반환
- [ ] `getHttpStatus(Throwable ex)` private 메서드 구현
- [ ] `getErrorCode(Throwable ex)` private 메서드 구현
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtErrorHandler Global Error Handler 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Error Handler ArchUnit 테스트 추가: `JwtErrorHandlerArchUnitTest.java`
  - `ErrorWebExceptionHandler` 구현 검증
  - `@Component` 어노테이션 검증
- [ ] Reactive Error Handling 최적화
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtErrorHandler ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `JwtErrorHandlerFixture.java` 생성
- [ ] Mock Exception Factory 메서드 작성
- [ ] `JwtErrorHandlerTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtErrorHandlerFixture 정리 (Tidy)`

---

### 5️⃣ PublicKeyRefreshController (Actuator) (Cycle 5)

#### 🔴 Red: 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/controller/PublicKeyRefreshControllerTest.java` 파일 생성
- [ ] WebTestClient로 `POST /actuator/refresh-public-keys` 테스트 작성
- [ ] `shouldRefreshPublicKeysSuccessfully()` 테스트 작성
- [ ] `shouldReturn500WhenRefreshFails()` 테스트 작성
- [ ] 테스트 실행 → 컴파일 에러 확인
- [ ] 커밋: `test: PublicKeyRefreshController 테스트 추가 (Red)`

#### 🟢 Green: 최소 구현
- [ ] `filter/src/main/java/com/ryuqq/connectly/gateway/filter/controller/PublicKeyRefreshController.java` 생성
- [ ] `@RestController` 추가
- [ ] `@RequestMapping("/actuator")` 추가
- [ ] `PublicKeyPort` 의존성 주입
- [ ] `POST /refresh-public-keys` 엔드포인트 구현:
  ```java
  @PostMapping("/refresh-public-keys")
  public Mono<ResponseEntity<Void>> refreshPublicKeys() {
      return publicKeyPort.refreshPublicKeys()
          .then(Mono.just(ResponseEntity.ok().build()));
  }
  ```
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: PublicKeyRefreshController Actuator 엔드포인트 구현 (Green)`

#### ♻️ Refactor: 리팩토링
- [ ] Controller ArchUnit 테스트 추가: `PublicKeyRefreshControllerArchUnitTest.java`
  - `@RestController` 어노테이션 검증
  - Reactive 타입 (Mono/Flux) 사용 검증
- [ ] Reactive Error Handling 적용
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: PublicKeyRefreshController ArchUnit 및 에러 처리 개선 (Refactor)`

#### 🧹 Tidy: TestFixture 정리
- [ ] `PublicKeyRefreshControllerFixture.java` 생성
- [ ] `PublicKeyRefreshControllerTest` → Fixture 사용으로 리팩토링
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: PublicKeyRefreshControllerFixture 정리 (Tidy)`

---

### 6️⃣ Filter Layer Unit Test (Cycle 6)

#### 🔴 Red: Unit 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/JwtAuthenticationFilterUnitTest.java` 파일 생성
- [ ] Mock UseCase로 Filter 로직 단위 테스트 작성
- [ ] `shouldPassValidJwtToNextFilter()` 테스트 작성
- [ ] `shouldSetExchangeAttributesCorrectly()` 테스트 작성
- [ ] `shouldClearMDCInFinallyBlock()` 테스트 작성
- [ ] 테스트 실행 → 실패 확인
- [ ] 커밋: `test: JwtAuthenticationFilter Unit 테스트 추가 (Red)`

#### 🟢 Green: Unit 테스트 통과
- [ ] Filter 로직 세부 검증
- [ ] Exchange Attributes 설정 검증
- [ ] MDC 정리 검증 (doFinally)
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: JwtAuthenticationFilter Unit 테스트 통과 (Green)`

#### ♻️ Refactor: Unit 테스트 리팩토링
- [ ] 테스트 코드 중복 제거
- [ ] Fixture 활용 최적화
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: JwtAuthenticationFilter Unit 테스트 리팩토링 (Refactor)`

#### 🧹 Tidy: Unit 테스트 Fixture 정리
- [ ] Fixture 메서드 통합
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: JwtAuthenticationFilter Unit 테스트 Fixture 정리 (Tidy)`

---

### 7️⃣ Filter Layer 통합 검증 (Cycle 7)

#### 🔴 Red: 통합 테스트 작성
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/filter/FilterLayerIntegrationTest.java` 파일 생성
- [ ] `@SpringBootTest` + WebTestClient
- [ ] Filter Chain 통합 테스트 작성 (JwtAuthenticationFilter → 다음 Filter)
- [ ] Exchange Attributes 전달 검증
- [ ] MDC 전파 검증
- [ ] 테스트 실행 → 실패 확인 (통합 시나리오)
- [ ] 커밋: `test: Filter Layer 통합 테스트 추가 (Red)`

#### 🟢 Green: 통합 시나리오 구현
- [ ] Filter Chain 동작 확인
- [ ] Exchange Attributes 전달 확인
- [ ] MDC 전파 확인
- [ ] 테스트 실행 → 통과 확인
- [ ] 커밋: `feat: Filter Layer 통합 시나리오 구현 (Green)`

#### ♻️ Refactor: 전체 Filter ArchUnit 검증
- [ ] `filter/src/test/java/com/ryuqq/connectly/gateway/architecture/FilterLayerArchUnitTest.java` 생성
- [ ] Filter Order 상수 사용 검증 (하드코딩 금지)
- [ ] GlobalFilter 구현 검증
- [ ] Reactive 타입 (Mono/Flux) 사용 검증
- [ ] 테스트 통과 확인
- [ ] 커밋: `struct: Filter Layer 전체 ArchUnit 검증 (Refactor)`

#### 🧹 Tidy: 모든 Fixture 통합 정리
- [ ] 모든 Fixture 파일 통합 검토
- [ ] 중복 메서드 제거
- [ ] 테스트 여전히 통과 확인
- [ ] 커밋: `test: Filter Layer 모든 Fixture 통합 정리 (Tidy)`

---

## ✅ 완료 조건

- [ ] 모든 TDD 사이클 완료 (7 사이클, 28개 단계 모두 ✅)
- [ ] Filter 1개 구현 완료 (JwtAuthenticationFilter)
- [ ] Controller 1개 구현 완료 (PublicKeyRefreshController)
- [ ] ErrorHandler 1개 구현 완료 (JwtErrorHandler)
- [ ] ErrorResponse 1개 구현 완료 (JwtErrorResponse)
- [ ] GatewayFilterOrder 상수 정의 완료
- [ ] 모든 Unit 테스트 통과
- [ ] Filter Layer ArchUnit 테스트 통과
- [ ] Zero-Tolerance 규칙 준수 (Filter Order 상수 사용, Reactive Programming)
- [ ] TestFixture 모두 정리 (Object Mother 패턴)
- [ ] WebTestClient 통합 테스트 통과
- [ ] 테스트 커버리지 > 90%

---

## 🔗 관련 문서

- **Task**: docs/prd/tasks/GATEWAY-001-jwt-authentication.md
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Gateway Filter Chain**: docs/prd/gateway-filter-chain.md
- **Spring Cloud Gateway 문서**: https://spring.io/projects/spring-cloud-gateway
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🎯 Zero-Tolerance 규칙 체크리스트

### Gateway Filter Layer 필수 준수 사항
- [ ] ✅ Filter Order: `GatewayFilterOrder.JWT_AUTH_FILTER` (값: 2) 고정
- [ ] ✅ Order 상수 사용 필수 (하드코딩 금지)
- [ ] ✅ Reactive Programming: Mono/Flux 사용 필수
- [ ] ✅ GlobalFilter 구현 + Ordered 구현
- [ ] ✅ MDC 정리: doFinally 블록에서 MDC.clear()

---

## 📊 진행 상황 추적

**완료된 사이클**: 0 / 7
**예상 남은 시간**: 105분

**다음 단계**: `/kb/filter/go` 명령으로 TDD 사이클 시작
