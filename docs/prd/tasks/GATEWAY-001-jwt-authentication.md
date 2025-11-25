# GATEWAY-001: JWT 인증 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: JWT 인증 (Stateless JWT Authentication)
**브랜치**: feature/GATEWAY-001-jwt-authentication
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 🚀 Quick Reference (이것만 보면 작업 가능!)

### Filter Order
- **JwtAuthenticationFilter Order**: `2` (`HIGHEST_PRECEDENCE + 2`)
- **Filter Chain 위치**:
  ```
  [0] TraceIdFilter (GATEWAY-006) ← traceId 생성
  [1] RateLimitFilter (GATEWAY-005) ← Rate Limit 검사
  [2] JwtAuthenticationFilter ← 이 태스크 ✅
  [3] TokenRefreshFilter (GATEWAY-003) ← Token 갱신
  [4] TenantIsolationFilter (GATEWAY-004) ← Tenant 격리
  [5] PermissionFilter (GATEWAY-002) ← 권한 검증
  [6] MfaVerificationFilter ← MFA 검증
  ```

### Exchange Attributes (Filter 간 데이터 전달)
- **입력 (상위 Filter에서 받음)**:
  - `traceId` (String) - TraceIdFilter에서 생성 (선택, 로깅용)

- **출력 (하위 Filter에 전달)**:
  - `userId` (String) - 사용자 ID
  - `tenantId` (String) - 테넌트 ID
  - `permissionHash` (String) - 권한 해시값
  - `roles` (Set\<String\>) - 사용자 역할 목록

- **코드 예시**:
  ```java
  // JwtAuthenticationFilter에서 설정
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      return validateJwt(accessToken)
          .flatMap(claims -> {
              // Exchange Attributes 설정 (하위 Filter에 전달)
              exchange.getAttributes().put("userId", claims.getUserId());
              exchange.getAttributes().put("tenantId", claims.getTenantId());
              exchange.getAttributes().put("permissionHash", claims.getPermissionHash());
              exchange.getAttributes().put("roles", claims.getRoles());

              return chain.filter(exchange);
          });
  }
  ```

### Port 정의 (이 태스크 전용)

**사용할 Port**: `AuthHubPort` (이미 정의됨, 공유 Port)

**위치**: `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/AuthHubPort.java`

**이 태스크에서 사용할 메서드** (전체 중 일부만):
```java
public interface AuthHubPort {
    /**
     * JWT Public Key 조회 (JWKS 엔드포인트)
     * @return Public Key (PEM 형식)
     */
    Mono<String> getPublicKey();

    /**
     * JWT 검증 (AuthHub 위임 방식 - 선택적)
     * @param accessToken Access Token
     * @return JWT Claims
     */
    Mono<JwtClaims> verifyJwt(String accessToken);
}
```

**Adapter 구현체**: `AuthHubAdapter`
- **위치**: `adapter-out/authhub/src/main/java/.../AuthHubAdapter.java`
- **기술**: WebClient (Reactive HTTP Client)
- **Circuit Breaker**: Resilience4j (JWKS 실패 시 캐시된 Public Key 사용)

### Redis Key 규칙 (이 태스크 전용)

| Key Pattern | 용도 | Data Type | TTL | Tenant Isolation |
|-------------|------|-----------|-----|------------------|
| `authhub:jwt:publickey` | JWT Public Key 캐시 | String (PEM) | 1h | ❌ 불필요 (전역) |

**코드 예시**:
```java
@Component
@RequiredArgsConstructor
public class PublicKeyQueryAdapter implements PublicKeyPort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final AuthHubPort authHubPort;

    private static final String PUBLIC_KEY_CACHE_KEY = "authhub:jwt:publickey";
    private static final Duration PUBLIC_KEY_TTL = Duration.ofHours(1);

    @Override
    public Mono<String> getPublicKey() {
        // Redis Cache 조회
        return redisTemplate.opsForValue()
            .get(PUBLIC_KEY_CACHE_KEY)
            .switchIfEmpty(
                // Cache Miss → AuthHub 호출 → Redis 저장
                authHubPort.getPublicKey()
                    .flatMap(key -> redisTemplate.opsForValue()
                        .set(PUBLIC_KEY_CACHE_KEY, key, PUBLIC_KEY_TTL)
                        .thenReturn(key)
                    )
            );
    }
}
```

**Cache Invalidation**:
- **방법**: TTL 만료 (1시간)
- **Webhook**: ❌ 불필요 (Public Key Rotation은 긴급하지 않음)

### MDC (Mapped Diagnostic Context) 전파

**JwtAuthenticationFilter에서 MDC 설정**:
```java
// JWT 검증 성공 후 MDC 추가
MDC.put("userId", claims.getUserId());
MDC.put("tenantId", claims.getTenantId());

// 모든 로그에 자동 포함됨
log.info("JWT 검증 성공");
// → [traceId=abc123] [userId=user-123] [tenantId=tenant-abc] JWT 검증 성공
```

**MDC 정리 (Filter 종료 시 - finally 블록)**:
```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return validateJwt(accessToken)
        .flatMap(claims -> {
            MDC.put("userId", claims.getUserId());
            MDC.put("tenantId", claims.getTenantId());

            return chain.filter(exchange);
        })
        .doFinally(signalType -> {
            // Filter 종료 시 MDC 정리
            MDC.clear();
        });
}
```

### Error Handling

**이 Filter의 에러 처리 책임**:

| 에러 상황 | HTTP Status | Error Code | Response Body |
|----------|-------------|------------|---------------|
| JWT 없음 (Authorization 헤더 없음) | 401 Unauthorized | `JWT_MISSING` | `{ "errorCode": "JWT_MISSING", "message": "Authorization header is missing" }` |
| JWT 만료 | 401 Unauthorized | `JWT_EXPIRED` | `{ "errorCode": "JWT_EXPIRED", "message": "Access Token expired" }` |
| JWT 서명 검증 실패 | 401 Unauthorized | `JWT_INVALID` | `{ "errorCode": "JWT_INVALID", "message": "Invalid JWT signature" }` |
| Public Key 없음 | 500 Internal Server Error | `PUBLIC_KEY_NOT_FOUND` | `{ "errorCode": "PUBLIC_KEY_NOT_FOUND", "message": "Public Key not found" }` |

**Global Error Handler 통합**:
```java
// GlobalErrorWebExceptionHandler에 추가
@Override
public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    String traceId = exchange.getAttribute("traceId");

    ErrorResponse errorResponse = ErrorResponse.builder()
        .errorCode(getErrorCode(ex))
        .message(ex.getMessage())
        .traceId(traceId)
        .timestamp(Instant.now())
        .build();

    exchange.getResponse().setStatusCode(getHttpStatus(ex));
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    return exchange.getResponse().writeWith(
        Mono.just(exchange.getResponse().bufferFactory().wrap(
            objectMapper.writeValueAsBytes(errorResponse)
        ))
    );
}

private HttpStatus getHttpStatus(Throwable ex) {
    if (ex instanceof JwtExpiredException || ex instanceof JwtInvalidException) {
        return HttpStatus.UNAUTHORIZED; // 401
    }
    if (ex instanceof PublicKeyNotFoundException) {
        return HttpStatus.INTERNAL_SERVER_ERROR; // 500
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
}
```

---

## 📝 목적

JWT 기반 Stateless 인증 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- JWT 토큰 검증 (RS256, Public Key 기반)
- Public Key Rotation 지원 (JWKS 엔드포인트)
- JWT Claims 추출 및 검증
- 인증 실패 시 401 Unauthorized 반환

**이 Feature는 독립적으로 배포 가능한 완전한 기능 단위입니다.**

---

## 🏗️ Infrastructure & Tech Stack

### Core Framework
- [ ] **Spring Cloud Gateway 3.1.x**: Filter Chain 기반 라우팅
- [ ] **Spring WebFlux**: Reactive Non-Blocking I/O
- [ ] **Netty**: 비동기 이벤트 기반 서버
- [ ] **Project Reactor**: Mono/Flux 기반 Reactive Programming

### Reactive Stack
- [ ] **Lettuce**: Reactive Redis Client (Connection Pool 관리)
- [ ] **Redisson**: Distributed Lock (미래 확장용)
- [ ] **WebClient**: AuthHub API 연동 (JWKS 엔드포인트 호출)
  - Connection Timeout: 3초
  - Response Timeout: 3초
  - Circuit Breaker: Resilience4j (JWKS 조회 실패 시 캐시된 Public Key 사용)
  - Retry: Exponential Backoff (최대 3회)

### Redis Configuration
- [ ] **개발/테스트**: Redis Standalone (Testcontainers)
- [ ] **Production**: AWS ElastiCache Redis Cluster (Master 3 + Replica 3)
- [ ] **Connection Pool (Lettuce)**:
  - max-active: 16 (CPU Core * 2)
  - max-idle: 8 (CPU Core)
  - min-idle: 4 (CPU Core / 2)
  - max-wait: 1000ms
- [ ] **Cache TTL**: Public Key 1시간 (JWKS Rotation 주기)
- [ ] **Redis AUTH**: Production 필수

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.jwt.validation.duration (Timer)
  - gateway.jwt.validation.success (Counter)
  - gateway.jwt.validation.failure (Counter)
  - gateway.redis.publickey.cache.hit (Counter)
  - gateway.redis.publickey.cache.miss (Counter)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 7-alpine (실제 컨테이너)
- [ ] **WireMock**: AuthHub Mock Server
  - `/api/v1/auth/jwks` 엔드포인트 Mock
- [ ] **WebTestClient**: Reactive 통합 테스트 (TestRestTemplate 대체)
- [ ] **StepVerifier**: Reactor 테스트 (Mono/Flux 검증)

### Deployment (AWS ECS Fargate)
- [ ] **Dockerfile**: Multi-stage Build
  - Base Image: eclipse-temurin:21-jre-alpine
  - Layered JAR (Spring Boot 2.3+)
- [ ] **ECS Task Definition**:
  - CPU: 1 vCPU (1024)
  - Memory: 2 GB (2048)
  - 환경변수: AUTHHUB_BASE_URL, REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
- [ ] **ECS Service**:
  - Auto Scaling (Target: CPU 70%, Min: 2, Max: 10)
  - Application Load Balancer (Health Check: /actuator/health)
- [ ] **AWS Secrets Manager**: Redis AUTH, JWT Secret 관리

### Configuration Management
- [ ] **application.yml**: 기본 설정 (로컬 개발용)
- [ ] **환경변수 (ECS Task Definition)**:
  - `AUTHHUB_BASE_URL`: AuthHub 서비스 URL
  - `REDIS_HOST`: Redis 호스트
  - `REDIS_PORT`: Redis 포트
  - `REDIS_PASSWORD`: AWS Secrets Manager에서 주입

---

## 🎯 요구사항

### 📦 Domain Layer

#### Aggregate Root
- [ ] **JwtToken**: JWT 토큰 도메인 모델
  - accessToken (String, JWT 원본 문자열)
  - expiresAt (Instant, 만료 시각)
  - createdAt (Instant, 생성 시각)

- [ ] **JwtClaims**: JWT Payload 도메인 모델
  - userId (String, 사용자 ID)
  - tenantId (String, 테넌트 ID)
  - permissionHash (String, 권한 해시값)
  - roles (Set<String>, 역할 목록)
  - issuedAt (Instant, 발급 시각)
  - expiresAt (Instant, 만료 시각)

#### Value Objects
- [ ] **AccessToken**: Access Token VO
  - value (String, JWT 문자열)
  - 검증: JWT 형식 유효성 (3 parts: header.payload.signature)

- [ ] **PublicKey**: RSA Public Key VO
  - kid (String, Key ID)
  - publicKey (RSAPublicKey, Java RSA Public Key)
  - 검증: kid 형식 (예: "key-2025-01-01")

#### 도메인 비즈니스 규칙
- [ ] **Access Token 만료 검증**: expiresAt < now → 만료
- [ ] **JWT Claims 필수 필드**: userId, tenantId 필수

#### 도메인 예외
- [ ] **JwtExpiredException**: Access Token 만료 (401 Unauthorized)
- [ ] **JwtInvalidException**: JWT 서명 검증 실패 (401 Unauthorized)
- [ ] **PublicKeyNotFoundException**: Public Key 없음 (500 Internal Server Error)

---

### 🔧 Application Layer

#### Use Case
- [ ] **ValidateJwtUseCase** (Command)
  - **Input**: ValidateJwtCommand
    - accessToken (String, JWT 원본)
  - **Output**: ValidateJwtResponse
    - jwtClaims (JwtClaims, JWT Payload)
    - isValid (boolean, 검증 성공 여부)
  - **Transaction**: 불필요 (읽기 전용)
  - **비즈니스 로직**:
    1. JwtValidationPort를 통해 JWT 검증
    2. Public Key 조회 (kid 기반)
    3. Signature 검증
    4. Expiration 검증
    5. Issuer/Audience 검증
    6. Claims 추출

- [ ] **GetPublicKeyUseCase** (Query)
  - **Input**: GetPublicKeyQuery
    - kid (String, Key ID)
  - **Output**: GetPublicKeyResponse
    - publicKey (PublicKey VO, RSA Public Key)
  - **Transaction**: 불필요 (읽기 전용)

#### Port 정의 (In)
- [ ] **ValidateJwtPort** (In)
  - validateJwt(ValidateJwtCommand): ValidateJwtResponse

- [ ] **GetPublicKeyPort** (In)
  - getPublicKey(GetPublicKeyQuery): GetPublicKeyResponse

#### Port 정의 (Out)
- [ ] **JwtValidationPort** (Out)
  - verifySignature(String accessToken, PublicKey publicKey): boolean
  - extractClaims(String accessToken): JwtClaims

- [ ] **PublicKeyPort** (Out)
  - getPublicKey(String kid): PublicKey
  - refreshPublicKeys(): void

#### DTO
- [ ] **ValidateJwtCommand** (Record)
- [ ] **ValidateJwtResponse** (Record)
- [ ] **GetPublicKeyQuery** (Record)
- [ ] **GetPublicKeyResponse** (Record)

#### Assembler
- [ ] **JwtAssembler**
  - toJwtClaims(ValidateJwtResponse): JwtClaims

---

### 💾 Persistence Layer (Redis)

#### Entity
- [ ] **PublicKeyEntity**: Public Key 캐시 엔티티
  - kid (String, Key ID)
  - modulus (String, Base64 인코딩된 Modulus)
  - exponent (String, Base64 인코딩된 Exponent)
  - kty (String, Key Type - "RSA")
  - use (String, Public Key Use - "sig")
  - alg (String, Algorithm - "RS256")

#### Repository
- [ ] **PublicKeyRedisRepository**
  - save(String kid, PublicKeyEntity publicKey, Duration ttl): void
  - findByKid(String kid): Optional<PublicKeyEntity>
  - deleteAll(): void

#### Adapter (Port 구현체)
- [ ] **PublicKeyCommandAdapter** (PublicKeyPort 구현)
  - refreshPublicKeys(): void
    - AuthHub JWKS 엔드포인트 호출
    - 모든 Public Key를 Redis에 저장
    - 기존 캐시 삭제 후 전체 교체

- [ ] **PublicKeyQueryAdapter** (PublicKeyPort 구현)
  - getPublicKey(String kid): PublicKey
    - Redis 조회 (Cache Hit → 즉시 반환)
    - Cache Miss → AuthHub JWKS 호출 → Redis 저장

- [ ] **JwtValidationAdapter** (JwtValidationPort 구현)
  - verifySignature(String accessToken, PublicKey publicKey): boolean
  - extractClaims(String accessToken): JwtClaims

#### Mapper
- [ ] **PublicKeyMapper**
  - toPublicKey(PublicKeyEntity entity): PublicKey
  - toPublicKeyEntity(PublicKey publicKey): PublicKeyEntity

#### Redis Key Design
```
Key: "authhub:jwt:publickey"
Value: PublicKeyEntity (JSON)
TTL: 1시간
```

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **JwtAuthenticationFilter** (GlobalFilter)
  - **Order**: `2` (`GatewayFilterOrder.JWT_AUTH_FILTER`)
  - **Order 상수 정의**:
    ```java
    public class GatewayFilterOrder {
        public static final int HIGHEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;
        public static final int TRACE_ID_FILTER = HIGHEST_PRECEDENCE;           // 0
        public static final int RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 1;     // 1
        public static final int JWT_AUTH_FILTER = HIGHEST_PRECEDENCE + 2;       // 2 ← 이 태스크
        public static final int TOKEN_REFRESH_FILTER = HIGHEST_PRECEDENCE + 3;  // 3
        public static final int TENANT_ISOLATION_FILTER = HIGHEST_PRECEDENCE + 4; // 4
        public static final int PERMISSION_FILTER = HIGHEST_PRECEDENCE + 5;     // 5
        public static final int MFA_VERIFICATION_FILTER = HIGHEST_PRECEDENCE + 6; // 6
    }
    ```
  - **로직**:
    1. Authorization 헤더에서 Bearer Token 추출
    2. ValidateJwtUseCase 호출 (JWT 검증)
    3. JwtClaims 추출
    4. ServerWebExchange Attribute에 jwtClaims 저장
    5. MDC에 userId, tenantId 추가
  - **예외 처리**:
    - JWT 없음 → 401 Unauthorized
    - JWT 만료 → 401 Unauthorized
    - JWT 검증 실패 → 401 Unauthorized

#### Actuator Endpoint
- [ ] **PublicKeyRefreshController** (RestController)
  - **Endpoint**: `POST /actuator/refresh-public-keys`
  - **목적**: 긴급 Public Key 갱신 (수동 트리거)
  - **로직**: PublicKeyPort.refreshPublicKeys() 호출

#### Error Response
- [ ] **JwtErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - timestamp (Instant, 발생 시각)
  - traceId (String, Trace-ID)

#### Error Handling
- [ ] **JwtErrorHandler** (ErrorWebExceptionHandler 일부)
  - JwtExpiredException → `{ "errorCode": "JWT_EXPIRED", "message": "Access Token expired" }`
  - JwtInvalidException → `{ "errorCode": "JWT_INVALID", "message": "Invalid JWT signature" }`
  - PublicKeyNotFoundException → `{ "errorCode": "PUBLIC_KEY_NOT_FOUND", "message": "Public Key not found for kid: {kid}" }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: JWT 인증 성공**
  - Given: 유효한 Access Token (RS256 서명), Public Key가 Redis에 캐시됨
  - When: `GET /api/v1/orders` 요청 (Authorization: Bearer {accessToken})
  - Then: 200 OK, Backend Service로 요청 전달됨
  - 검증: JwtAuthenticationFilter 통과, ServerWebExchange Attribute에 jwtClaims 저장

- [ ] **Scenario 2: JWT 만료 → 401 Unauthorized**
  - Given: 만료된 Access Token
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "JWT_EXPIRED" }`
  - 검증: JwtAuthenticationFilter에서 JwtExpiredException 발생

- [ ] **Scenario 3: JWT 서명 검증 실패 → 401 Unauthorized**
  - Given: 잘못된 서명의 Access Token
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "JWT_INVALID" }`
  - 검증: JwtValidationPort에서 서명 검증 실패

- [ ] **Scenario 4: Public Key Rotation**
  - Given: 현재 Public Key (kid="key-2025-01-01"), 새 Public Key (kid="key-2025-01-08")
  - When: JWKS 엔드포인트가 두 Key 모두 반환, 새 JWT (kid="key-2025-01-08")로 요청
  - Then: 200 OK, 새 Public Key로 검증 성공
  - 검증: PublicKeyPort.getPublicKey("key-2025-01-08") 성공

- [ ] **Scenario 5: Public Key Cache Hit**
  - Given: Redis에 Public Key 캐시됨
  - When: 동일한 kid로 여러 번 요청
  - Then: 200 OK, AuthHub JWKS 호출 없이 Redis에서 조회
  - 검증: Redis Cache Hit 로그 확인

- [ ] **Scenario 6: Public Key 수동 갱신 (Actuator)**
  - Given: Redis에 기존 Public Key 캐시됨
  - When: `POST /actuator/refresh-public-keys` 호출
  - Then: 200 OK, Redis 캐시 갱신 완료
  - 검증: PublicKeyPort.refreshPublicKeys() 실행

#### Testcontainers
- [ ] **Redis Testcontainers**: 실제 Redis 사용
- [ ] **AuthHub Mock Server**: WireMock 사용
  - `/api/v1/auth/jwks` 엔드포인트 Mock

#### TestFixture
- [ ] **JwtTestFixture**: 테스트용 JWT 생성
  - RS256 Private Key로 서명
  - Claims 커스터마이징 가능

- [ ] **PublicKeyTestFixture**: 테스트용 Public Key 생성
  - JWKS 형식 Public Key

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지 (Plain Java 또는 Record 사용)
- [ ] Law of Demeter 준수
- [ ] 외부 의존성 절대 금지

#### Application Layer
- [ ] Transaction 경계: 읽기 전용 Use Case는 `@Transactional` 불필요
- [ ] Port 의존성 역전: UseCase는 Port만 의존

#### Persistence Layer
- [ ] Lombok 금지 (Entity는 Plain Java 또는 Record)
- [ ] Cache TTL: Public Key는 1시간

#### Gateway Filter Layer
- [ ] **Filter Order**: `GatewayFilterOrder.JWT_AUTH_FILTER` (값: 2) 고정
- [ ] **Order 상수 사용 필수**: 하드코딩 금지
- [ ] Reactive Programming: Mono/Flux 사용 필수

#### Reactive 규칙 (추가)
- [ ] **Blocking Call 절대 금지**
  - JDBC (JPA Repository) 사용 금지
  - RestTemplate 사용 금지 → WebClient 필수
  - Thread.sleep() 금지
  - Mono.block(), Flux.blockFirst(), Flux.blockLast() 금지 (테스트 제외)
- [ ] **Reactive Repository 필수**
  - ReactiveRedisTemplate 사용
  - Spring Data Redis Reactive 사용
- [ ] **Reactor Context 사용**
  - Trace-ID 전파용 (ThreadLocal 대신)
  - MDC는 Sleuth가 자동 관리
- [ ] **Error Handling**
  - onErrorResume(), onErrorReturn() 사용
  - Exception을 Mono.error()로 변환

#### Integration Test
- [ ] **WebTestClient 사용** (TestRestTemplate 대체 - Reactive 표준)
- [ ] **MockMvc 금지** (이미 명시됨)
- [ ] **Testcontainers 사용** (Redis)
- [ ] **WireMock 사용** (AuthHub Mock)
- [ ] **StepVerifier 사용** (Reactor 테스트)

---

## ✅ 완료 조건

- [ ] Domain Layer 구현 완료 (Aggregate 2개, VO 2개, Exception 3개)
- [ ] Application Layer 구현 완료 (UseCase 2개, Port 4개, DTO 4개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (Entity 1개, Repository 1개, Adapter 3개, Mapper 1개)
- [ ] Gateway Filter Layer 구현 완료 (Filter 1개, Controller 1개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 6개, TestFixture 2개)
- [ ] 모든 테스트 통과 (Unit + Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90%
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

### 통합 가이드 (전체 시스템 규칙)
- **Gateway Filter Chain**: docs/prd/gateway-filter-chain.md
- **Gateway Port Matrix**: docs/prd/gateway-port-matrix.md
- **Redis Naming Convention**: docs/prd/redis-naming-convention.md

### PRD 및 Plan
- **PRD**: docs/prd/access-gateway.md (JWT 인증 섹션)
- **Plan**: docs/prd/plans/GATEWAY-001-jwt-authentication-plan.md (create-plan 후 생성)
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-2

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/
- Persistence: docs/coding_convention/04-persistence-layer/redis/
- Filter: Spring Cloud Gateway 공식 문서
- Integration: docs/coding_convention/05-testing/integration-testing/

### PRD 섹션
- JWT 검증 프로세스 (Line 102-145)
- Public Key 관리 및 Rotation (Line 147-380)
