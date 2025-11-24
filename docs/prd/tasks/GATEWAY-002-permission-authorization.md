# GATEWAY-002: Permission 인가 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: Permission 기반 인가 (Permission-Based Authorization)
**브랜치**: feature/GATEWAY-002-permission-authorization
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-3

---

## 🚀 Quick Reference (이것만 보면 작업 가능!)

### Filter Order
- **PermissionFilter Order**: `5` (`HIGHEST_PRECEDENCE + 5`)
- **Filter Chain 위치**:
  ```
  [0] TraceIdFilter (GATEWAY-006) ← traceId 생성
  [1] RateLimitFilter (GATEWAY-005) ← Rate Limit 검사
  [2] JwtAuthenticationFilter (GATEWAY-001) ← JWT 검증
  [3] TokenRefreshFilter (GATEWAY-003) ← Token 갱신
  [4] TenantIsolationFilter (GATEWAY-004) ← Tenant 격리
  [5] PermissionFilter ← 이 태스크 ✅
  [6] MfaVerificationFilter ← MFA 검증
  ```

### Exchange Attributes (Filter 간 데이터 전달)
- **입력 (상위 Filter에서 받음)** - 모두 필수:
  - `userId` (String) - JwtAuthenticationFilter
  - `tenantId` (String) - JwtAuthenticationFilter
  - `permissionHash` (String) - JwtAuthenticationFilter
  - `roles` (Set\<String\>) - JwtAuthenticationFilter
  - `traceId` (String) - TraceIdFilter (선택, 로깅용)

- **출력 (하위 Filter에 전달)**:
  - `endpointPermission` (EndpointPermission) - 엔드포인트 권한 설정
  - `authorized` (Boolean) - 권한 검증 결과

- **코드 예시**:
  ```java
  // PermissionFilter에서 입력 검증
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
      // 의존성 검증 (상위 Filter 실행 확인)
      String userId = exchange.getAttribute("userId");
      String tenantId = exchange.getAttribute("tenantId");
      String permissionHash = exchange.getAttribute("permissionHash");
      Set<String> roles = exchange.getAttribute("roles");

      if (userId == null || tenantId == null || permissionHash == null) {
          return Mono.error(new UnauthorizedException("Missing authentication attributes"));
      }

      // 권한 검증
      return validatePermission(exchange.getRequest().getPath(), userId, tenantId, roles)
          .flatMap(endpointPermission -> {
              // Exchange Attributes 설정
              exchange.getAttributes().put("endpointPermission", endpointPermission);
              exchange.getAttributes().put("authorized", true);

              return chain.filter(exchange);
          });
  }
  ```

### Port 정의 (이 태스크 전용)

**사용할 Port**: `AuthHubPort`, `PermissionSpecQueryPort`, `PermissionHashQueryPort`

#### 1. AuthHubPort (공유 Port)
**위치**: `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/AuthHubPort.java`

**이 태스크에서 사용할 메서드** (전체 중 일부만):
```java
public interface AuthHubPort {
    /**
     * Permission Spec 조회
     * @return Permission Spec (전체)
     */
    Mono<PermissionSpec> getPermissionSpec();

    /**
     * 사용자별 Permission Hash 조회
     * @param tenantId Tenant ID
     * @param userId User ID
     * @return Permission Set
     */
    Mono<Set<String>> getUserPermissions(String tenantId, String userId);
}
```

#### 2. PermissionSpecQueryPort (이 태스크 전용)
**위치**: `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/PermissionSpecQueryPort.java`

```java
public interface PermissionSpecQueryPort {
    /**
     * Permission Spec 조회 (Redis Cache)
     * @return Permission Spec (Optional)
     */
    Mono<Optional<PermissionSpec>> getPermissionSpec();

    /**
     * Permission Spec 저장 (Cache)
     * @param permissionSpec Permission Spec
     * @param ttl TTL
     */
    Mono<Void> savePermissionSpec(PermissionSpec permissionSpec, Duration ttl);

    /**
     * Permission Spec 캐시 무효화
     */
    Mono<Void> invalidatePermissionSpec();
}
```

**Adapter**: `PermissionSpecCacheAdapter`

#### 3. PermissionHashQueryPort (이 태스크 전용)
**위치**: `application/src/main/java/com/ryuqq/connectly/gateway/application/port/out/PermissionHashQueryPort.java`

```java
public interface PermissionHashQueryPort {
    /**
     * 사용자별 Permission 조회 (Redis Cache)
     * @param tenantId Tenant ID
     * @param userId User ID
     * @return Permission Set
     */
    Mono<Optional<Set<String>>> getUserPermissions(String tenantId, String userId);

    /**
     * 사용자별 Permission 저장 (Cache)
     * @param tenantId Tenant ID
     * @param userId User ID
     * @param permissions Permission Set
     * @param ttl TTL
     */
    Mono<Void> saveUserPermissions(String tenantId, String userId, Set<String> permissions, Duration ttl);

    /**
     * 사용자별 Permission 캐시 무효화
     * @param tenantId Tenant ID
     * @param userId User ID
     */
    Mono<Void> invalidateUserPermissions(String tenantId, String userId);
}
```

**Adapter**: `PermissionHashCacheAdapter`

### Redis Key 규칙 (이 태스크 전용)

| Key Pattern | 용도 | Data Type | TTL | Tenant Isolation |
|-------------|------|-----------|-----|------------------|
| `authhub:permission:spec` | Permission Spec 캐시 | String (JSON) | 30s | ❌ 불필요 (전역) |
| `authhub:permission:hash:{tenantId}:{userId}` | 사용자별 권한 해시 | String (JSON) | 30s | ✅ 필수 |

**코드 예시 1: Permission Spec Cache**
```java
@Component
@RequiredArgsConstructor
public class PermissionSpecCacheAdapter implements PermissionSpecQueryPort {

    private final ReactiveRedisTemplate<String, PermissionSpec> redisTemplate;
    private final AuthHubPort authHubPort;

    private static final String KEY_PREFIX = "authhub:permission:spec";
    private static final Duration TTL = Duration.ofSeconds(30);

    @Override
    public Mono<Optional<PermissionSpec>> getPermissionSpec() {
        return redisTemplate.opsForValue()
            .get(KEY_PREFIX)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .switchIfEmpty(
                // Cache Miss → AuthHub 호출
                authHubPort.getPermissionSpec()
                    .flatMap(spec -> savePermissionSpec(spec, TTL)
                        .thenReturn(Optional.of(spec))
                    )
            );
    }

    @Override
    public Mono<Void> savePermissionSpec(PermissionSpec permissionSpec, Duration ttl) {
        return redisTemplate.opsForValue()
            .set(KEY_PREFIX, permissionSpec, ttl)
            .then();
    }

    @Override
    public Mono<Void> invalidatePermissionSpec() {
        return redisTemplate.delete(KEY_PREFIX).then();
    }
}
```

**코드 예시 2: Permission Hash Cache (Tenant Isolation)**
```java
@Component
@RequiredArgsConstructor
public class PermissionHashCacheAdapter implements PermissionHashQueryPort {

    private final ReactiveRedisTemplate<String, Set<String>> redisTemplate;
    private final AuthHubPort authHubPort;

    private static final String KEY_PREFIX = "authhub:permission:hash";
    private static final Duration TTL = Duration.ofSeconds(30);

    @Override
    public Mono<Optional<Set<String>>> getUserPermissions(String tenantId, String userId) {
        String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, userId);

        return redisTemplate.opsForValue()
            .get(key)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .switchIfEmpty(
                // Cache Miss → AuthHub 호출
                authHubPort.getUserPermissions(tenantId, userId)
                    .flatMap(permissions -> saveUserPermissions(tenantId, userId, permissions, TTL)
                        .thenReturn(Optional.of(permissions))
                    )
            );
    }

    @Override
    public Mono<Void> saveUserPermissions(String tenantId, String userId, Set<String> permissions, Duration ttl) {
        String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, userId);
        return redisTemplate.opsForValue()
            .set(key, permissions, ttl)
            .then();
    }

    @Override
    public Mono<Void> invalidateUserPermissions(String tenantId, String userId) {
        String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, userId);
        return redisTemplate.delete(key).then();
    }
}
```

**Cache Invalidation**:
- **Permission Spec**: Webhook (AuthHub → Gateway) + TTL 30s Fallback
- **Permission Hash**: Webhook (사용자 권한 변경 시) + TTL 30s Fallback

**⚠️ Critical - Tenant Isolation**:
```java
// ✅ 올바른 예시: tenantId + userId
String key = String.format("authhub:permission:hash:%s:%s", tenantId, userId);

// ❌ 잘못된 예시: userId만 사용 (테넌트 충돌!)
String key = String.format("authhub:permission:hash:%s", userId);
```

### Webhook 엔드포인트

#### 1. Permission Spec 변경 Webhook
**Endpoint**: `POST /internal/gateway/permissions/refresh`

**Payload**:
```json
{
  "version": 42,
  "changedServices": ["order-service", "product-service"]
}
```

**처리 로직**:
```java
@PostMapping("/internal/gateway/permissions/refresh")
public Mono<Void> handlePermissionSpecUpdated(@RequestBody PermissionSpecUpdatedEvent event) {
    return permissionSpecQueryPort.invalidatePermissionSpec()
        .then();
}
```

#### 2. Permission Hash 변경 Webhook
**Endpoint**: `POST /internal/gateway/permissions/hash-changed`

**Payload**:
```json
{
  "tenantId": "tenant-abc",
  "userId": "user-123"
}
```

**처리 로직**:
```java
@PostMapping("/internal/gateway/permissions/hash-changed")
public Mono<Void> handleUserPermissionUpdated(@RequestBody UserPermissionUpdatedEvent event) {
    return permissionHashQueryPort.invalidateUserPermissions(
        event.getTenantId(),
        event.getUserId()
    ).then();
}
```

**⚠️ 보안**: Internal API이므로 IP Whitelist 필수

### Error Handling

**이 Filter의 에러 처리 책임**:

| 에러 상황 | HTTP Status | Error Code | Response Body |
|----------|-------------|------------|---------------|
| 권한 부족 | 403 Forbidden | `PERMISSION_DENIED` | `{ "errorCode": "PERMISSION_DENIED", "message": "Missing required permission", "requiredPermissions": ["order:delete"], "userPermissions": ["order:read"] }` |
| Permission Spec 없음 | 403 Forbidden | `PERMISSION_DENIED` | `{ "errorCode": "PERMISSION_DENIED", "message": "Endpoint not defined in Permission Spec (Default Deny)" }` |

**Global Error Handler 통합**:
```java
@Override
public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    if (ex instanceof PermissionDeniedException) {
        PermissionDeniedException pde = (PermissionDeniedException) ex;

        PermissionErrorResponse errorResponse = PermissionErrorResponse.builder()
            .errorCode("PERMISSION_DENIED")
            .message(pde.getMessage())
            .requiredPermissions(pde.getRequiredPermissions())
            .userPermissions(pde.getUserPermissions())
            .traceId(exchange.getAttribute("traceId"))
            .timestamp(Instant.now())
            .build();

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN); // 403
        return writeResponse(exchange, errorResponse);
    }

    return Mono.error(ex);
}
```

---

## 📝 목적

Permission 기반 Fine-grained 인가 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- Permission Spec 기반 엔드포인트 권한 검증
- 2-Tier Cache (JWT Payload + Redis)
- Permission Hash 기반 권한 변경 감지
- Webhook 기반 Permission Spec 동기화

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
- [ ] **WebClient**: AuthHub API 연동 (Permission Spec, Permission Hash 조회)
  - Connection Timeout: 3초
  - Response Timeout: 3초
  - Circuit Breaker: Resilience4j (조회 실패 시 기본 정책 적용)
  - Retry: Exponential Backoff (최대 2회)

### Redis Configuration
- [ ] **개발/테스트**: Redis Standalone (Testcontainers)
- [ ] **Production**: AWS ElastiCache Redis Cluster (Master 3 + Replica 3)
- [ ] **Connection Pool (Lettuce)**:
  - max-active: 16 (CPU Core * 2)
  - max-idle: 8 (CPU Core)
  - min-idle: 4 (CPU Core / 2)
  - max-wait: 1000ms
- [ ] **Cache TTL**:
  - Permission Spec: 30초 (빠른 반영 필요)
  - Permission Hash: 30초 (사용자별 권한)
- [ ] **Redis AUTH**: Production 필수

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.permission.check.duration (Timer)
  - gateway.permission.check.success (Counter)
  - gateway.permission.check.denied (Counter)
  - gateway.redis.permission.cache.hit (Counter)
  - gateway.redis.permission.cache.miss (Counter)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 7-alpine (실제 컨테이너)
- [ ] **WireMock**: AuthHub Mock Server
  - `/api/v1/permissions/spec` 엔드포인트 Mock
  - `/api/v1/permissions/hash/{tenantId}/{userId}` 엔드포인트 Mock
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
- [ ] **PermissionSpec**: 전체 Permission Spec 도메인 모델
  - version (Long, Spec 버전)
  - updatedAt (Instant, 마지막 업데이트 시각)
  - permissions (List<EndpointPermission>, 엔드포인트 권한 목록)

#### Value Objects
- [ ] **Permission**: 권한 VO
  - value (String, 권한 문자열 - 예: "order:read")
  - 검증: 형식 "{resource}:{action}"
  - 검증: 와일드카드 지원 ("order:*")

- [ ] **EndpointPermission**: 엔드포인트 권한 매핑 VO
  - serviceName (String, 서비스 이름)
  - path (String, API 경로)
  - method (HttpMethod, HTTP 메서드)
  - requiredPermissions (Set<Permission>, 필수 권한)
  - requiredRoles (Set<String>, 필수 역할)
  - isPublic (boolean, 인증 불필요 여부)

- [ ] **PermissionHash**: 권한 해시 VO
  - hash (String, SHA-256 해시)
  - permissions (Set<Permission>, 권한 목록)
  - roles (Set<String>, 역할 목록)
  - generatedAt (Instant, 생성 시각)

#### Enum
- [ ] **HttpMethod**: HTTP 메서드
  - GET, POST, PUT, PATCH, DELETE

#### 도메인 비즈니스 규칙
- [ ] **권한 매칭**: Required Permissions ALL 보유 여부 검증
- [ ] **와일드카드 매칭**: "order:*" → "order:read", "order:create" 포함
- [ ] **Permission Hash 검증**: JWT Hash ≠ Cache Hash → Cache 우선

#### 도메인 예외
- [ ] **PermissionDeniedException**: 권한 부족 (403 Forbidden)
- [ ] **PermissionSpecNotFoundException**: Permission Spec 없음 (403 Forbidden - Default Deny)

---

### 🔧 Application Layer

#### Use Case
- [ ] **ValidatePermissionUseCase** (Command)
  - **Input**: ValidatePermissionCommand
    - jwtClaims (JwtClaims, JWT Payload)
    - requestPath (String, 요청 경로)
    - requestMethod (String, HTTP 메서드)
  - **Output**: ValidatePermissionResponse
    - isAuthorized (boolean, 권한 검증 성공 여부)
  - **Transaction**: 불필요 (읽기 전용)
  - **비즈니스 로직**:
    1. PermissionSpecPort를 통해 엔드포인트 권한 조회
    2. Required Permissions 추출
    3. JWT Claims의 permissions와 비교
    4. Permission Hash 검증 (2-Tier Cache)
    5. 권한 변경 감지 시 Cache 우선

- [ ] **GetPermissionSpecUseCase** (Query)
  - **Input**: GetPermissionSpecQuery
    - version (Long, Spec 버전 - 선택)
  - **Output**: GetPermissionSpecResponse
    - permissionSpec (PermissionSpec Aggregate)
  - **Transaction**: 불필요 (읽기 전용)

- [ ] **SyncPermissionSpecUseCase** (Command)
  - **Input**: SyncPermissionSpecCommand
    - version (Long, 새 Spec 버전)
  - **Output**: SyncPermissionSpecResponse
    - success (boolean, 동기화 성공 여부)
  - **Transaction**: 불필요 (읽기 전용)
  - **Webhook 트리거**: AuthHub → Gateway

#### Port 정의 (In)
- [ ] **ValidatePermissionPort** (In)
  - validatePermission(ValidatePermissionCommand): ValidatePermissionResponse

- [ ] **GetPermissionSpecPort** (In)
  - getPermissionSpec(GetPermissionSpecQuery): GetPermissionSpecResponse

- [ ] **SyncPermissionSpecPort** (In)
  - syncPermissionSpec(SyncPermissionSpecCommand): SyncPermissionSpecResponse

#### Port 정의 (Out)
- [ ] **PermissionSpecQueryPort** (Out)
  - getPermissionSpec(): Mono\<Optional\<PermissionSpec\>\>
  - savePermissionSpec(PermissionSpec, Duration): Mono\<Void\>
  - invalidatePermissionSpec(): Mono\<Void\>

- [ ] **PermissionHashQueryPort** (Out)
  - getUserPermissions(String tenantId, String userId): Mono\<Optional\<Set\<String\>\>\>
  - saveUserPermissions(String tenantId, String userId, Set\<String\>, Duration): Mono\<Void\>
  - invalidateUserPermissions(String tenantId, String userId): Mono\<Void\>

- [ ] **AuthHubPort** (Out) - 공유 Port
  - getPermissionSpec(): Mono\<PermissionSpec\>
  - getUserPermissions(String tenantId, String userId): Mono\<Set\<String\>\>

#### DTO
- [ ] **ValidatePermissionCommand** (Record)
- [ ] **ValidatePermissionResponse** (Record)
- [ ] **GetPermissionSpecQuery** (Record)
- [ ] **GetPermissionSpecResponse** (Record)
- [ ] **SyncPermissionSpecCommand** (Record)
- [ ] **SyncPermissionSpecResponse** (Record)

#### Assembler
- [ ] **PermissionAssembler**
  - toEndpointPermission(EndpointPermissionEntity): EndpointPermission
  - toPermissionSpec(PermissionSpecEntity): PermissionSpec

---

### 💾 Persistence Layer (Redis)

#### Entity
- [ ] **PermissionSpecEntity**: Permission Spec 캐시 엔티티
  - version (Long, Spec 버전)
  - updatedAt (Instant, 마지막 업데이트 시각)
  - permissions (List<EndpointPermissionEntity>, 엔드포인트 권한 목록)

- [ ] **EndpointPermissionEntity**: 엔드포인트 권한 엔티티
  - serviceName (String, 서비스 이름)
  - path (String, API 경로)
  - method (String, HTTP 메서드)
  - requiredPermissions (Set<String>, 필수 권한)
  - requiredRoles (Set<String>, 필수 역할)
  - isPublic (boolean, 인증 불필요 여부)

- [ ] **PermissionHashEntity**: Permission Hash 캐시 엔티티
  - hash (String, SHA-256 해시)
  - permissions (Set<String>, 권한 목록)
  - roles (Set<String>, 역할 목록)
  - generatedAt (Instant, 생성 시각)

#### Repository
- [ ] **PermissionSpecRedisRepository**
  - save(PermissionSpecEntity permissionSpec, Duration ttl): void
  - findCurrent(): Optional<PermissionSpecEntity>
  - delete(): void

- [ ] **PermissionHashRedisRepository**
  - save(String tenantId, String userId, PermissionHashEntity permissionHash, Duration ttl): void
  - findByTenantAndUser(String tenantId, String userId): Optional<PermissionHashEntity>
  - delete(String tenantId, String userId): void

#### Adapter (Port 구현체)
- [ ] **PermissionSpecQueryAdapter** (PermissionSpecPort 구현)
  - getCurrentSpec(): PermissionSpec
    - Local Memory Cache 조회 (0ms latency)
    - Redis 조회 (1-3ms latency)
    - Cache Miss → AuthHub API 호출 → Redis + Local Memory 저장
  - findPermission(String path, String method): EndpointPermission
    - getCurrentSpec() → 엔드포인트 권한 매칭 (Regex)

- [ ] **PermissionHashQueryAdapter** (PermissionCachePort 구현)
  - getPermissionHash(String userId, String tenantId): PermissionHash
    - Redis 조회 (Cache Hit → 즉시 반환)
    - Cache Miss → AuthHub API 호출 → Redis 저장

- [ ] **PermissionCacheCommandAdapter** (PermissionCachePort 구현)
  - invalidatePermissionCache(String userId, String tenantId): void
    - Redis 캐시 삭제

#### Mapper
- [ ] **PermissionSpecMapper**
  - toPermissionSpec(PermissionSpecEntity entity): PermissionSpec
  - toPermissionSpecEntity(PermissionSpec permissionSpec): PermissionSpecEntity

- [ ] **PermissionHashMapper**
  - toPermissionHash(PermissionHashEntity entity): PermissionHash
  - toPermissionHashEntity(PermissionHash permissionHash): PermissionHashEntity

#### Redis Key Design
```
# Permission Spec Cache (전역 - Tenant Isolation 불필요)
Key: "authhub:permission:spec"
Value: PermissionSpecEntity (JSON)
TTL: 30초
Invalidation: Webhook (AuthHub → Gateway)

# Permission Hash Cache (사용자별 - Tenant Isolation 필수)
Key: "authhub:permission:hash:{tenantId}:{userId}"
Value: PermissionHashEntity (JSON)
TTL: 30초
Invalidation: Webhook (AuthHub → Gateway) + TTL Fallback
```

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **PermissionFilter** (GlobalFilter)
  - **Order**: `5` (`GatewayFilterOrder.PERMISSION_FILTER`)
  - **Order 상수 정의**:
    ```java
    public class GatewayFilterOrder {
        public static final int HIGHEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;
        public static final int TRACE_ID_FILTER = HIGHEST_PRECEDENCE;           // 0
        public static final int RATE_LIMIT_FILTER = HIGHEST_PRECEDENCE + 1;     // 1
        public static final int JWT_AUTH_FILTER = HIGHEST_PRECEDENCE + 2;       // 2
        public static final int TOKEN_REFRESH_FILTER = HIGHEST_PRECEDENCE + 3;  // 3
        public static final int TENANT_ISOLATION_FILTER = HIGHEST_PRECEDENCE + 4; // 4
        public static final int PERMISSION_FILTER = HIGHEST_PRECEDENCE + 5;     // 5 ← 이 태스크
        public static final int MFA_VERIFICATION_FILTER = HIGHEST_PRECEDENCE + 6; // 6
    }
    ```
  - **로직**:
    1. ServerWebExchange Attribute에서 jwtClaims 조회
    2. ValidatePermissionUseCase 호출 (Permission 검증)
    3. PermissionSpecPort를 통해 엔드포인트 권한 조회
    4. Required Permissions ALL 보유 여부 확인
    5. Permission Hash 검증 (2-Tier Cache)
  - **예외 처리**:
    - Permission 부족 → 403 Forbidden
    - Permission Spec 없음 → 403 Forbidden (Default Deny)

#### Webhook Controller
- [ ] **PermissionSpecWebhookController** (RestController)
  - **Endpoint**: `POST /internal/gateway/permissions/refresh`
  - **목적**: AuthHub로부터 Permission Spec 변경 알림 수신
  - **보안**: Internal API이므로 IP Whitelist 필수

- [ ] **PermissionHashWebhookController** (RestController)
  - **Endpoint**: `POST /internal/gateway/permissions/hash-changed`
  - **목적**: AuthHub로부터 Permission Hash 변경 알림 수신
  - **보안**: Internal API이므로 IP Whitelist 필수

#### Error Response
- [ ] **PermissionErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - requiredPermissions (Set<String>, 필요한 권한 목록)
  - userPermissions (Set<String>, 사용자 권한 목록)

#### Error Handling
- [ ] **PermissionErrorHandler** (ErrorWebExceptionHandler 일부)
  - PermissionDeniedException → `{ "errorCode": "PERMISSION_DENIED", "message": "Missing required permission", "requiredPermissions": [...], "userPermissions": [...] }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: Permission 검증 성공**
  - Given: JWT permissions: ["order:read", "order:create"], Required: ["order:read"]
  - When: `GET /api/v1/orders` 요청
  - Then: 200 OK, Backend Service로 요청 전달됨

- [ ] **Scenario 2: Permission 부족 → 403 Forbidden**
  - Given: JWT permissions: ["order:read"], Required: ["order:delete"]
  - When: `DELETE /api/v1/orders/123` 요청
  - Then: 403 Forbidden, Error Response: `{ "errorCode": "PERMISSION_DENIED" }`

- [ ] **Scenario 3: Permission Hash 변경 감지 → Cache 우선**
  - Given: JWT permissionHash: "old_hash", Cache permissionHash: "new_hash", Cache permissions: ["order:read", "order:delete"]
  - When: `DELETE /api/v1/orders/123` 요청
  - Then: 200 OK (Cache의 새 권한으로 허용)

- [ ] **Scenario 4: 와일드카드 권한 매칭**
  - Given: JWT permissions: ["order:*"], Required: ["order:read", "order:create", "order:delete"]
  - When: `DELETE /api/v1/orders/123` 요청
  - Then: 200 OK (와일드카드로 모든 order 권한 포함)

- [ ] **Scenario 5: Permission Spec 변경 → Webhook 동기화**
  - Given: 현재 Permission Spec version: 41
  - When: AuthHub가 Webhook 전송: `POST /internal/gateway/permissions/refresh` (version: 42)
  - Then: Gateway가 AuthHub API 호출, Redis 캐시 갱신 (version: 42)

- [ ] **Scenario 6: Permission Hash 변경 → Webhook 캐시 무효화**
  - Given: Redis에 userId=123, tenantId=tenant-1의 Permission Hash 캐시됨
  - When: AuthHub가 Webhook 전송: `POST /internal/gateway/permissions/hash-changed`
  - Then: Redis 캐시 삭제, 다음 요청 시 AuthHub API 호출

- [ ] **Scenario 7: Permission Spec 없음 → 403 Forbidden (Default Deny)**
  - Given: Permission Spec에 `/api/v1/unknown` 엔드포인트 정의 없음
  - When: `GET /api/v1/unknown` 요청
  - Then: 403 Forbidden, Error Response: `{ "errorCode": "PERMISSION_DENIED" }`

- [ ] **Scenario 8: Public 엔드포인트 → 인증 불필요**
  - Given: Permission Spec에서 `/api/v1/health`가 isPublic = true
  - When: `GET /api/v1/health` 요청 (JWT 없음)
  - Then: 200 OK, Backend Service로 요청 전달됨

#### Testcontainers
- [ ] **Redis Testcontainers**: 실제 Redis 사용
- [ ] **AuthHub Mock Server**: WireMock 사용
  - `/internal/authhub/permissions/spec` 엔드포인트 Mock
  - `/api/v1/permissions/hash` 엔드포인트 Mock

#### TestFixture
- [ ] **PermissionSpecTestFixture**: 테스트용 Permission Spec 생성
- [ ] **PermissionHashTestFixture**: 테스트용 Permission Hash 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지
- [ ] Law of Demeter 준수
- [ ] 와일드카드 매칭 로직 도메인에 위치

#### Application Layer
- [ ] Transaction 불필요 (읽기 전용 Use Case)
- [ ] Port 의존성 역전

#### Persistence Layer
- [ ] Cache TTL: Permission Spec 30초, Permission Hash 30초
- [ ] Local Memory Cache + Redis 2-Tier Cache

#### Gateway Filter Layer
- [ ] **Filter Order**: `GatewayFilterOrder.PERMISSION_FILTER` (값: 5) 고정
- [ ] **Order 상수 사용 필수**: 하드코딩 금지
- [ ] Webhook IP Whitelist 필수

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
- [ ] **Testcontainers 사용** (Redis)
- [ ] **WireMock 사용** (AuthHub Mock)
- [ ] **StepVerifier 사용** (Reactor 테스트)

---

## ✅ 완료 조건

- [ ] Domain Layer 구현 완료 (Aggregate 1개, VO 3개, Enum 1개, Exception 2개)
- [ ] Application Layer 구현 완료 (UseCase 3개, Port 5개, DTO 6개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (Entity 3개, Repository 2개, Adapter 3개, Mapper 2개)
- [ ] Gateway Filter Layer 구현 완료 (Filter 1개, Webhook 2개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 8개, TestFixture 2개)
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
- **PRD**: docs/prd/access-gateway.md (Permission 인가 섹션)
- **Plan**: docs/prd/plans/GATEWAY-002-permission-authorization-plan.md (create-plan 후 생성)
- **Jira**: https://ryuqqq.atlassian.net/browse/GAT-3

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/
- Persistence: docs/coding_convention/04-persistence-layer/redis/

### PRD 섹션
- Permission 기반 인가 (Line 382-1481)
- 권한 캐싱 전략 (Line 384-567)
- 엔드포인트-권한 매핑 (Line 570-1009)
