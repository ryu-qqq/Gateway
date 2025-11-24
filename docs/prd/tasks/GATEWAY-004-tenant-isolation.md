# GATEWAY-004: Tenant 격리 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: 멀티테넌트 격리 (Multi-Tenant Isolation)
**브랜치**: feature/GATEWAY-004-tenant-isolation
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-5

---

## 🚀 Quick Reference (개발 시 필수 참조)

이 섹션은 개발 중 반복적으로 참조해야 하는 핵심 정보를 모아둔 것입니다.

### 1. Filter Order (Filter Chain 내 위치)

```java
// TenantIsolationFilter Order: 4 (HIGHEST_PRECEDENCE + 4)
[0] TraceIdFilter (GATEWAY-006) ← traceId 생성
[1] RateLimitFilter (GATEWAY-005) ← Rate Limit 검사
[2] JwtAuthenticationFilter (GATEWAY-001) ← JWT 검증, userId/tenantId 추출
[3] TokenRefreshFilter (GATEWAY-003) ← Token 갱신
[4] TenantIsolationFilter ← 이 태스크 ✅
[5] PermissionFilter (GATEWAY-002) ← Permission 검증
[6] MfaVerificationFilter (GATEWAY-007) ← MFA 검증
```

**Filter Chain 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#filter-chain-실행-순서)

**의존성**:
- **Upstream**: JwtAuthenticationFilter (GATEWAY-001)에서 `userId`, `tenantId` 추출 필요
- **Downstream**: PermissionFilter, MfaVerificationFilter가 `tenantContext` 사용

**⚠️ CRITICAL**: `userId`와 `tenantId`는 JwtAuthenticationFilter가 설정해야 함!

---

### 2. Exchange Attributes (Filter 간 데이터 전달)

#### Input Attributes (이 Filter가 사용하는 값)

```java
// JwtAuthenticationFilter (GATEWAY-001)에서 설정된 값 사용
String userId = exchange.getAttribute("userId");       // 필수
String tenantId = exchange.getAttribute("tenantId");   // 필수
String traceId = exchange.getAttribute("traceId");     // MDC 전파용
Set<String> roles = exchange.getAttribute("roles");    // 역할 정보

// Validation (Filter 진입 시)
if (userId == null || tenantId == null) {
    return Mono.error(new UnauthorizedException("Missing authentication attributes"));
}
```

#### Output Attributes (이 Filter가 설정하는 값)

```java
// Tenant Config 조회 후 설정
exchange.getAttributes().put("tenantContext", tenantConfig);  // Downstream Filter가 사용
exchange.getAttributes().put("mfaRequired", tenantConfig.mfaRequired());  // MFA 필수 여부

// Request Header에 추가 (Backend Service로 전달)
ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", userId)
    .header("X-Tenant-Id", tenantId)
    .header("X-Permissions", objectMapper.writeValueAsString(permissions))
    .header("X-Roles", objectMapper.writeValueAsString(roles))
    .build();
```

**Exchange Attributes 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#exchange-attributes-사용-규칙)

---

### 3. Port Definitions (Gateway-Wide 공통 정의)

이 Feature에서 사용하는 Port들 (전체 정의는 [Port Matrix](../gateway-port-matrix.md) 참조):

#### 3.1 TenantConfigPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 및 캐시 관리 Port
 */
public interface TenantConfigPort {

    /**
     * Tenant Config 조회 (Redis Cache → AuthHub API)
     *
     * @param tenantId Tenant ID
     * @return Tenant Config Aggregate
     */
    Mono<TenantConfig> getTenantConfig(String tenantId);

    /**
     * Tenant Config 캐시 무효화 (Webhook 트리거)
     *
     * @param tenantId Tenant ID
     * @return Void
     */
    Mono<Void> invalidateTenantConfigCache(String tenantId);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#4-tenant-config-port)

#### 3.2 AuthHubPort (Shared Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;

/**
 * AuthHub 서비스 연동 Port (Gateway 전체 공통)
 */
public interface AuthHubPort {

    /**
     * Tenant Config 조회 (AuthHub API 호출)
     *
     * @param tenantId Tenant ID
     * @return Tenant Config Aggregate
     */
    Mono<TenantConfig> getTenantConfig(String tenantId);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#1-authhub-integration-port)

---

### 4. Redis Key Design & TTL (Redis Naming Convention)

#### 4.1 Tenant Config Cache

```java
// Redis Key Pattern
String KEY_PREFIX = "gateway:tenant:config";
String key = String.format("%s:%s", KEY_PREFIX, tenantId);

// 예시: gateway:tenant:config:tenant-123
// Value: TenantConfigEntity (JSON)
// TTL: 1시간 (빠른 변경 반영 필요)

// Adapter 구현 예시 (TenantConfigCacheAdapter)
@Component
@RequiredArgsConstructor
public class TenantConfigCacheAdapter implements TenantConfigPort {

    private final ReactiveRedisTemplate<String, TenantConfigEntity> redisTemplate;
    private final AuthHubPort authHubPort;
    private final TenantConfigMapper mapper;

    private static final String KEY_PREFIX = "gateway:tenant:config";
    private static final Duration TTL = Duration.ofHours(1);

    @Override
    public Mono<TenantConfig> getTenantConfig(String tenantId) {
        String key = String.format("%s:%s", KEY_PREFIX, tenantId);

        return redisTemplate.opsForValue().get(key)
            .map(mapper::toTenantConfig)
            .switchIfEmpty(
                // Cache Miss → AuthHub API 호출
                authHubPort.getTenantConfig(tenantId)
                    .flatMap(config -> {
                        TenantConfigEntity entity = mapper.toTenantConfigEntity(config);
                        return redisTemplate.opsForValue()
                            .set(key, entity, TTL)
                            .thenReturn(config);
                    })
            );
    }

    @Override
    public Mono<Void> invalidateTenantConfigCache(String tenantId) {
        String key = String.format("%s:%s", KEY_PREFIX, tenantId);
        return redisTemplate.delete(key).then();
    }
}
```

**Redis Naming Convention 참조**: [Redis Naming Convention & TTL Standards](../redis-naming-convention.md)

**⚠️ CRITICAL**: Webhook 기반 캐시 무효화 지원!
- AuthHub에서 Tenant Config 변경 시 Webhook 전송
- Gateway는 Redis 캐시 삭제 → 다음 요청 시 AuthHub API 호출 → 새 Config 캐시

---

### 5. Webhook Endpoint (Tenant Config 변경 알림)

#### 5.1 Webhook Payload & Handler

```java
// Webhook Endpoint
@RestController
@RequestMapping("/internal/gateway/tenants")
@RequiredArgsConstructor
public class TenantConfigWebhookController {

    private final SyncTenantConfigPort syncTenantConfigPort;

    /**
     * Tenant Config 변경 알림 수신 (AuthHub → Gateway)
     *
     * Webhook Payload:
     * {
     *   "tenantId": "tenant-123",
     *   "timestamp": "2025-11-24T12:34:56Z"
     * }
     */
    @PostMapping("/config-changed")
    public Mono<Void> handleTenantConfigChanged(@RequestBody TenantConfigChangedEvent event) {
        SyncTenantConfigCommand command = new SyncTenantConfigCommand(event.getTenantId());
        return syncTenantConfigPort.syncTenantConfig(command).then();
    }
}

// Webhook Event DTO
public record TenantConfigChangedEvent(
    String tenantId,
    Instant timestamp
) {}

// Use Case (SyncTenantConfigUseCase)
@UseCase
@RequiredArgsConstructor
public class SyncTenantConfigService implements SyncTenantConfigPort {

    private final TenantConfigPort tenantConfigPort;

    @Override
    public Mono<SyncTenantConfigResponse> syncTenantConfig(SyncTenantConfigCommand command) {
        // Redis 캐시 무효화
        return tenantConfigPort.invalidateTenantConfigCache(command.tenantId())
            .thenReturn(new SyncTenantConfigResponse(true));
    }
}
```

**Webhook Security (IP Whitelist)**:
```java
// application.yml
webhook:
  ip-whitelist: 10.0.1.0/24,10.0.2.0/24

// WebhookSecurityFilter (Order: HIGHEST_PRECEDENCE - 1)
@Component
public class WebhookSecurityFilter implements WebFilter {

    private final Set<String> allowedIpRanges;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getPath().toString().startsWith("/internal/")) {
            String clientIp = getClientIp(request);
            if (!isIpAllowed(clientIp, allowedIpRanges)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }
}
```

---

### 6. Tenant Context 전달 (Backend Service로)

#### 6.1 Request Header Mutation

```java
// TenantIsolationFilter에서 Request Header 추가
@Component
@Order(4)  // HIGHEST_PRECEDENCE + 4
@RequiredArgsConstructor
public class TenantIsolationFilter implements GlobalFilter {

    private final TenantConfigPort tenantConfigPort;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getAttribute("userId");
        String tenantId = exchange.getAttribute("tenantId");
        Set<String> permissions = exchange.getAttribute("permissions");
        Set<String> roles = exchange.getAttribute("roles");

        // Tenant Config 조회
        return tenantConfigPort.getTenantConfig(tenantId)
            .flatMap(tenantConfig -> {
                // Exchange Attribute에 저장 (Downstream Filter가 사용)
                exchange.getAttributes().put("tenantContext", tenantConfig);
                exchange.getAttributes().put("mfaRequired", tenantConfig.mfaRequired());

                // Request Header에 추가 (Backend Service로 전달)
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-Tenant-Id", tenantId)
                    .header("X-Permissions", serializeToJson(permissions))
                    .header("X-Roles", serializeToJson(roles))
                    .build();

                ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

                return chain.filter(mutatedExchange);
            });
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
```

**Backend Service에서 수신**:
```java
// Spring Boot Backend Service
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping
    public Mono<List<OrderResponse>> getOrders(
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestHeader("X-Permissions") String permissionsJson,
        @RequestHeader("X-Roles") String rolesJson
    ) {
        // Tenant ID로 데이터 격리 (WHERE tenant_id = :tenantId)
        Set<String> permissions = objectMapper.readValue(permissionsJson, new TypeReference<>() {});
        Set<String> roles = objectMapper.readValue(rolesJson, new TypeReference<>() {});

        return orderService.getOrders(tenantId, userId, permissions);
    }
}
```

---

### 7. Error Handling (TenantIsolationFilter)

#### 7.1 Error Code Table

| Error Code | HTTP Status | 발생 조건 | 처리 |
|-----------|-------------|---------|------|
| `TENANT_MISMATCH` | 403 | JWT tenantId ≠ Request tenantId | 권한 없음 |
| `MFA_REQUIRED` | 403 | MFA 필수이나 미검증 | MFA 인증 페이지로 리다이렉트 |
| `SOCIAL_LOGIN_NOT_ALLOWED` | 403 | 허용되지 않은 소셜 로그인 | 로그인 페이지로 리다이렉트 |
| `TENANT_CONFIG_NOT_FOUND` | 500 | Tenant Config 조회 실패 | 관리자 문의 |

#### 7.2 Global Error Handler Integration

```java
@Component
@Order(-2)  // Spring Security 전에 실행
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof TenantMismatchException) {
            return handleTenantMismatch(exchange, (TenantMismatchException) ex);
        } else if (ex instanceof MfaRequiredException) {
            return handleMfaRequired(exchange, (MfaRequiredException) ex);
        } else if (ex instanceof SocialLoginNotAllowedException) {
            return handleSocialLoginNotAllowed(exchange, (SocialLoginNotAllowedException) ex);
        }
        return Mono.error(ex);
    }

    private Mono<Void> handleTenantMismatch(ServerWebExchange exchange, TenantMismatchException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        TenantErrorResponse error = new TenantErrorResponse(
            "TENANT_MISMATCH",
            "Tenant ID mismatch. Access denied.",
            ex.getTenantId()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleMfaRequired(ServerWebExchange exchange, MfaRequiredException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        TenantErrorResponse error = new TenantErrorResponse(
            "MFA_REQUIRED",
            "Multi-Factor Authentication is required for this tenant.",
            ex.getTenantId()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleSocialLoginNotAllowed(ServerWebExchange exchange, SocialLoginNotAllowedException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        TenantErrorResponse error = new TenantErrorResponse(
            "SOCIAL_LOGIN_NOT_ALLOWED",
            String.format("Social login provider '%s' is not allowed for this tenant.", ex.getProvider()),
            ex.getTenantId()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

---

### 8. MDC (Mapped Diagnostic Context) 전파

```java
// TenantIsolationFilter에서 MDC 설정
return Mono.deferContextual(ctx -> {
    String traceId = exchange.getAttribute("traceId");
    String userId = exchange.getAttribute("userId");
    String tenantId = exchange.getAttribute("tenantId");

    MDC.put("traceId", traceId);
    MDC.put("userId", userId);
    MDC.put("tenantId", tenantId);

    return tenantConfigPort.getTenantConfig(tenantId);
})
.contextWrite(context -> {
    context = context.put("traceId", exchange.getAttribute("traceId"));
    context = context.put("userId", exchange.getAttribute("userId"));
    context = context.put("tenantId", exchange.getAttribute("tenantId"));
    return context;
});
```

**MDC 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#mdc-전파-규칙)

---

### 9. MFA Required Validation (MfaRequiredFilter)

#### 9.1 MFA 검증 로직

```java
@Component
@Order(7)  // HIGHEST_PRECEDENCE + 7 (TenantIsolationFilter 이후)
@RequiredArgsConstructor
public class MfaRequiredFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Exchange Attribute에서 Tenant Config 조회
        TenantConfig tenantConfig = exchange.getAttribute("tenantContext");
        Boolean mfaVerified = exchange.getAttribute("mfaVerified");  // JWT Claim

        if (tenantConfig == null) {
            // TenantIsolationFilter가 실행 안 됨 (비정상)
            return Mono.error(new UnauthorizedException("Tenant context not found"));
        }

        // MFA 필수 여부 검증
        if (tenantConfig.mfaRequired() && (mfaVerified == null || !mfaVerified)) {
            return Mono.error(new MfaRequiredException(
                String.format("MFA verification required for tenant: %s", tenantConfig.tenantId())
            ));
        }

        return chain.filter(exchange);
    }
}
```

#### 9.2 MFA 검증 흐름

```
1. JwtAuthenticationFilter (Order 2)
   └─> JWT에서 mfaVerified Claim 추출
       └─> exchange.getAttributes().put("mfaVerified", claims.getMfaVerified())

2. TenantIsolationFilter (Order 4)
   └─> Tenant Config 조회
       └─> exchange.getAttributes().put("tenantContext", tenantConfig)
       └─> exchange.getAttributes().put("mfaRequired", tenantConfig.mfaRequired())

3. MfaRequiredFilter (Order 7)
   └─> Exchange Attribute에서 tenantContext, mfaVerified 조회
       └─> if (mfaRequired && !mfaVerified) → 403 Forbidden
```

---

## 📝 목적

멀티테넌트 격리 및 테넌트별 동작 제어 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- Tenant ID 기반 격리 보장
- 테넌트별 MFA 필수 검증
- 테넌트별 소셜 로그인 허용 여부 제어
- Tenant Config 기반 동적 정책 적용
- Webhook 기반 Tenant Config 동기화

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
- [ ] **WebClient**: AuthHub API 연동 (Tenant Config 조회)
  - Connection Timeout: 3초
  - Response Timeout: 3초
  - Circuit Breaker: Resilience4j (Tenant Config 조회 실패 시 캐시 사용)
  - Retry: Exponential Backoff (최대 3회)

### Redis Configuration
- [ ] **개발/테스트**: Redis Standalone (Testcontainers)
- [ ] **Production**: AWS ElastiCache Redis Cluster (Master 3 + Replica 3)
- [ ] **Connection Pool (Lettuce)**:
  - max-active: 16 (CPU Core * 2)
  - max-idle: 8 (CPU Core)
  - min-idle: 4 (CPU Core / 2)
  - max-wait: 1000ms
- [ ] **Cache TTL**:
  - Tenant Config: 1시간 (빠른 변경 반영 필요)
  - Webhook 기반 캐시 무효화 지원
- [ ] **Redis AUTH**: Production 필수

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.tenant.config.cache.hit (Counter)
  - gateway.tenant.config.cache.miss (Counter)
  - gateway.tenant.mfa.validation.duration (Timer)
  - gateway.tenant.webhook.received (Counter)
  - gateway.tenant.context.propagation.duration (Timer)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 7-alpine (실제 컨테이너)
- [ ] **WireMock**: AuthHub Mock Server (Tenant Config API)
- [ ] **WebTestClient**: Reactive 통합 테스트 (TestRestTemplate 대체)
- [ ] **StepVerifier**: Reactor 테스트 (Mono/Flux 검증)

### Deployment (AWS ECS Fargate)
- [ ] **Dockerfile**: Multi-stage Build
  - Base Image: eclipse-temurin:21-jre-alpine
  - Layered JAR (Spring Boot 2.3+)
- [ ] **ECS Task Definition**:
  - CPU: 1 vCPU (1024)
  - Memory: 2 GB (2048)
  - 환경변수: AUTHHUB_BASE_URL, REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, WEBHOOK_IP_WHITELIST
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
  - `WEBHOOK_IP_WHITELIST`: Webhook IP Whitelist (쉼표 구분)

---

## 🎯 요구사항

### 📦 Domain Layer

#### Aggregate Root
- [ ] **TenantConfig**: 테넌트 설정 도메인 모델
  - tenantId (String, 테넌트 ID)
  - mfaRequired (boolean, MFA 필수 여부)
  - allowedSocialLogins (Set<SocialProvider>, 허용된 소셜 로그인)
  - roleHierarchy (Map<String, Set<String>>, 역할별 권한)
  - sessionConfig (SessionConfig VO, 세션 설정)
  - rateLimitConfig (RateLimitConfig VO, Rate Limit 설정)

#### Value Objects
- [ ] **TenantId**: 테넌트 ID VO
  - value (String, 테넌트 ID)
  - 검증: 형식 "tenant-{숫자}" 또는 UUID

- [ ] **SessionConfig**: 세션 설정 VO
  - maxActiveSessions (int, 최대 동시 세션 수)
  - accessTokenTTL (Duration, Access Token TTL - 기본 15분)
  - refreshTokenTTL (Duration, Refresh Token TTL - 기본 7일)
  - 검증: TTL > 0

- [ ] **RateLimitConfig**: Rate Limit 설정 VO
  - loginAttemptsPerHour (int, 시간당 로그인 시도 횟수)
  - otpRequestsPerHour (int, 시간당 OTP 요청 횟수)
  - 검증: 횟수 > 0

#### Enum
- [ ] **SocialProvider**: 소셜 로그인 제공자
  - KAKAO, NAVER, GOOGLE

#### 도메인 비즈니스 규칙
- [ ] **Tenant ID 검증**: JWT의 tenantId와 요청의 X-Tenant-Id 일치 확인
- [ ] **MFA 필수 검증**: tenantConfig.mfaRequired = true → jwtClaims.mfaVerified 검증

#### 도메인 예외
- [ ] **TenantMismatchException**: Tenant ID 불일치 (403 Forbidden)
- [ ] **MfaRequiredException**: MFA 필수이나 검증되지 않음 (403 Forbidden)
- [ ] **SocialLoginNotAllowedException**: 허용되지 않은 소셜 로그인 (403 Forbidden)

---

### 🔧 Application Layer

#### Use Case
- [ ] **GetTenantConfigUseCase** (Query)
  - **Input**: GetTenantConfigQuery
    - tenantId (String, 테넌트 ID)
  - **Output**: GetTenantConfigResponse
    - tenantConfig (TenantConfig Aggregate)
  - **Transaction**: 불필요 (읽기 전용)

- [ ] **SyncTenantConfigUseCase** (Command)
  - **Input**: SyncTenantConfigCommand
    - tenantId (String, 테넌트 ID)
  - **Output**: SyncTenantConfigResponse
    - success (boolean, 동기화 성공 여부)
  - **Transaction**: 불필요 (읽기 전용)
  - **Webhook 트리거**: AuthHub → Gateway

#### Port 정의 (In)
- [ ] **GetTenantConfigPort** (In)
  - getTenantConfig(GetTenantConfigQuery): GetTenantConfigResponse

- [ ] **SyncTenantConfigPort** (In)
  - syncTenantConfig(SyncTenantConfigCommand): SyncTenantConfigResponse

#### Port 정의 (Out)
- [ ] **TenantConfigPort** (Out)
  - getTenantConfig(String tenantId): TenantConfig
  - invalidateTenantConfigCache(String tenantId): void

- [ ] **AuthHubPort** (Out)
  - getTenantConfig(String tenantId): TenantConfig

#### DTO
- [ ] **GetTenantConfigQuery** (Record)
- [ ] **GetTenantConfigResponse** (Record)
- [ ] **SyncTenantConfigCommand** (Record)
- [ ] **SyncTenantConfigResponse** (Record)

#### Assembler
- [ ] **TenantConfigAssembler**
  - toTenantConfig(TenantConfigEntity): TenantConfig

---

### 💾 Persistence Layer (Redis)

#### Entity
- [ ] **TenantConfigEntity**: Tenant Config 캐시 엔티티
  - tenantId (String, 테넌트 ID)
  - mfaRequired (boolean, MFA 필수 여부)
  - allowedSocialLogins (Set<String>, 허용된 소셜 로그인)
  - roleHierarchy (Map<String, Set<String>>, 역할별 권한)
  - sessionConfig (SessionConfigEntity, 세션 설정)
  - rateLimitConfig (RateLimitConfigEntity, Rate Limit 설정)

- [ ] **SessionConfigEntity**: 세션 설정 엔티티
  - maxActiveSessions (int, 최대 동시 세션 수)
  - accessTokenTTL (int, Access Token TTL - 초)
  - refreshTokenTTL (int, Refresh Token TTL - 초)

- [ ] **RateLimitConfigEntity**: Rate Limit 설정 엔티티
  - loginAttemptsPerHour (int, 시간당 로그인 시도 횟수)
  - otpRequestsPerHour (int, 시간당 OTP 요청 횟수)

#### Repository
- [ ] **TenantConfigRedisRepository**
  - save(String tenantId, TenantConfigEntity tenantConfig, Duration ttl): void
  - findByTenantId(String tenantId): Optional<TenantConfigEntity>
  - delete(String tenantId): void

#### Adapter (Port 구현체)
- [ ] **TenantConfigQueryAdapter** (TenantConfigPort 구현)
  - getTenantConfig(String tenantId): TenantConfig
    - Redis 조회 (Cache Hit → 즉시 반환)
    - Cache Miss → AuthHub API 호출 → Redis 저장

- [ ] **TenantConfigCommandAdapter** (TenantConfigPort 구현)
  - invalidateTenantConfigCache(String tenantId): void
    - Redis 캐시 삭제

#### Mapper
- [ ] **TenantConfigMapper**
  - toTenantConfig(TenantConfigEntity entity): TenantConfig
  - toTenantConfigEntity(TenantConfig tenantConfig): TenantConfigEntity

#### Redis Key Design
```
Key: "tenant_config:{tenantId}"
Value: TenantConfigEntity (JSON)
TTL: 1시간
```

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **TenantContextFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 5`
  - **로직**:
    1. ServerWebExchange Attribute에서 jwtClaims 조회
    2. Request Header에 추가:
       - `X-User-Id`: jwtClaims.userId
       - `X-Tenant-Id`: jwtClaims.tenantId
       - `X-Permissions`: jwtClaims.permissions (JSON Array)
       - `X-Roles`: jwtClaims.roles (JSON Array)
  - **예외 처리**: 없음 (jwtClaims 없으면 헤더 추가 안 함)

- [ ] **MfaRequiredFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 7`
  - **로직**:
    1. ServerWebExchange Attribute에서 jwtClaims 조회
    2. TenantConfigPort를 통해 Tenant Config 조회
    3. tenantConfig.mfaRequired = true이면 jwtClaims.mfaVerified 검증
    4. MFA 미검증 시 `403 Forbidden` 반환
  - **예외 처리**:
    - MFA 필수이나 미검증 → 403 Forbidden (MFA_REQUIRED 에러 코드)

#### Webhook Controller
- [ ] **TenantConfigWebhookController** (RestController)
  - **Endpoint**: `POST /internal/gateway/tenants/config-changed`
  - **목적**: AuthHub로부터 Tenant Config 변경 알림 수신
  - **로직**:
    1. Webhook Payload 파싱: `{ "tenantId": "tenant-1" }`
    2. TenantConfigPort.invalidateTenantConfigCache() 호출
  - **보안**: Internal API이므로 IP Whitelist 필수

#### Error Response
- [ ] **TenantErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - tenantId (String, 테넌트 ID)

#### Error Handling
- [ ] **TenantErrorHandler** (ErrorWebExceptionHandler 일부)
  - TenantMismatchException → `{ "errorCode": "TENANT_MISMATCH", "message": "Tenant ID mismatch" }`
  - MfaRequiredException → `{ "errorCode": "MFA_REQUIRED", "message": "MFA verification required" }`
  - SocialLoginNotAllowedException → `{ "errorCode": "SOCIAL_LOGIN_NOT_ALLOWED", "message": "Provider not allowed for this tenant" }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: Tenant Context 전달 성공**
  - Given: JWT의 tenantId: "tenant-1", userId: 123
  - When: `GET /api/v1/orders` 요청
  - Then: Backend Service 요청에 `X-User-Id: 123`, `X-Tenant-Id: tenant-1` 포함
  - 검증: TenantContextFilter 통과

- [ ] **Scenario 2: Tenant ID 불일치 → 403 Forbidden**
  - Given: JWT의 tenantId: "tenant-1", 요청의 X-Tenant-Id: "tenant-2"
  - When: `GET /api/v1/orders` 요청
  - Then: 403 Forbidden, Error Response: `{ "errorCode": "TENANT_MISMATCH" }`
  - 검증: TenantContextFilter에서 Tenant ID 불일치 감지

- [ ] **Scenario 3: MFA 필수 검증 성공**
  - Given: TenantConfig.mfaRequired = true, JWT.mfaVerified = true
  - When: `GET /api/v1/orders` 요청
  - Then: 200 OK, Backend Service로 요청 전달됨
  - 검증: MfaRequiredFilter 통과

- [ ] **Scenario 4: MFA 필수이나 미검증 → 403 Forbidden**
  - Given: TenantConfig.mfaRequired = true, JWT.mfaVerified = false
  - When: `GET /api/v1/orders` 요청
  - Then: 403 Forbidden, Error Response: `{ "errorCode": "MFA_REQUIRED" }`
  - 검증: MfaRequiredFilter에서 MFA 미검증 감지

- [ ] **Scenario 5: Tenant Config 변경 → Webhook 캐시 무효화**
  - Given: Redis에 tenant-1 Config 캐시됨
  - When: AuthHub가 Webhook 전송: `POST /internal/gateway/tenants/config-changed` (tenantId: "tenant-1")
  - Then: Redis 캐시 삭제, 다음 요청 시 AuthHub API 호출 → 새 Config 캐시
  - 검증: TenantConfigWebhookController 호출 성공, Redis에 tenant-1 Config 없음

- [ ] **Scenario 6: 소셜 로그인 허용 여부 검증**
  - Given: TenantConfig.allowedSocialLogins = ["kakao"], 요청 provider = "naver"
  - When: 소셜 로그인 요청 (provider: naver)
  - Then: 403 Forbidden, Error Response: `{ "errorCode": "SOCIAL_LOGIN_NOT_ALLOWED" }`
  - 검증: SocialLoginFilter에서 허용되지 않은 제공자 감지

#### Testcontainers
- [ ] **Redis Testcontainers**: 실제 Redis 사용
- [ ] **AuthHub Mock Server**: WireMock 사용
  - `/api/v1/tenants/{tenantId}/config` 엔드포인트 Mock

#### TestFixture
- [ ] **TenantConfigTestFixture**: 테스트용 Tenant Config 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지
- [ ] Tenant ID 검증 로직 도메인에 위치

#### Application Layer
- [ ] Transaction 불필요 (읽기 전용 Use Case)
- [ ] Port 의존성 역전

#### Persistence Layer
- [ ] Cache TTL: Tenant Config는 1시간

#### Gateway Filter Layer
- [ ] Filter Order: TenantContextFilter `HIGHEST_PRECEDENCE + 5`, MfaRequiredFilter `HIGHEST_PRECEDENCE + 7`
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
- [ ] **MockMvc 금지**
- [ ] **Testcontainers 사용** (Redis)
- [ ] **WireMock 사용** (AuthHub Mock)
- [ ] **StepVerifier 사용** (Reactor 테스트)

---

## ✅ 완료 조건

- [ ] Domain Layer 구현 완료 (Aggregate 1개, VO 3개, Enum 1개, Exception 3개)
- [ ] Application Layer 구현 완료 (UseCase 2개, Port 4개, DTO 4개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (Entity 3개, Repository 1개, Adapter 2개, Mapper 1개)
- [ ] Gateway Filter Layer 구현 완료 (Filter 2개, Webhook 1개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 6개, TestFixture 1개)
- [ ] 모든 테스트 통과 (Unit + Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90%
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: docs/prd/access-gateway.md (Tenant 격리 섹션)
- **Plan**: docs/prd/plans/GATEWAY-004-tenant-isolation-plan.md (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/
- Persistence: docs/coding_convention/04-persistence-layer/redis/

### PRD 섹션
- 멀티테넌트 격리 및 라우팅 (Line 1648-1999)
- Tenant Config Cache (Line 1650-1756)
- Tenant Context 전달 (Line 1994-1999)
