# GATEWAY-003: 토큰 재발급 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: 자동 토큰 재발급 (Auto Token Refresh with Rotation)
**브랜치**: feature/GATEWAY-003-token-refresh
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-4

---

## 🚀 Quick Reference (개발 시 필수 참조)

이 섹션은 개발 중 반복적으로 참조해야 하는 핵심 정보를 모아둔 것입니다.

### 1. Filter Order (Filter Chain 내 위치)

```java
// TokenRefreshFilter Order: 3 (HIGHEST_PRECEDENCE + 3)
[0] TraceIdFilter (GATEWAY-006) ← traceId 생성
[1] RateLimitFilter (GATEWAY-005) ← Rate Limit 검사
[2] JwtAuthenticationFilter (GATEWAY-001) ← JWT 검증, userId/tenantId 추출
[3] TokenRefreshFilter ← 이 태스크 ✅
[4] TenantIsolationFilter (GATEWAY-004) ← Tenant 격리
[5] PermissionFilter (GATEWAY-002) ← Permission 검증
[6] MfaVerificationFilter (GATEWAY-007) ← MFA 검증
```

**Filter Chain 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#filter-chain-실행-순서)

**의존성**:
- **Upstream**: JwtAuthenticationFilter (GATEWAY-001)에서 `userId`, `tenantId` 추출 필요
- **Downstream**: 없음

**⚠️ CRITICAL**: `userId`와 `tenantId`는 JwtAuthenticationFilter가 설정해야 함!

---

### 2. Exchange Attributes (Filter 간 데이터 전달)

#### Input Attributes (이 Filter가 사용하는 값)

```java
// JwtAuthenticationFilter (GATEWAY-001)에서 설정된 값 사용
String userId = exchange.getAttribute("userId");       // 필수
String tenantId = exchange.getAttribute("tenantId");   // 필수 (Blacklist Key에 사용)
String traceId = exchange.getAttribute("traceId");     // MDC 전파용

// Validation (Filter 진입 시)
if (userId == null || tenantId == null) {
    return Mono.error(new UnauthorizedException("Missing authentication attributes"));
}
```

#### Output Attributes (이 Filter가 설정하는 값)

```java
// Token Refresh 성공 시
exchange.getAttributes().put("tokenRefreshed", true);  // 재발급 성공 플래그
```

**Exchange Attributes 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#exchange-attributes-사용-규칙)

---

### 3. Port Definitions (Gateway-Wide 공통 정의)

이 Feature에서 사용하는 Port들 (전체 정의는 [Port Matrix](../gateway-port-matrix.md) 참조):

#### 3.1 AuthHubPort (Shared Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;

/**
 * AuthHub 서비스 연동 Port (Gateway 전체 공통)
 */
public interface AuthHubPort {

    /**
     * Refresh Token 검증 및 새 Token Pair 발급
     *
     * @param refreshToken Refresh Token
     * @return 새 Access Token + 새 Refresh Token (Rotation)
     */
    Mono<TokenPair> refreshAccessToken(String refreshToken);

    /**
     * Refresh Token 즉시 무효화
     *
     * @param refreshToken 무효화할 Refresh Token
     * @return Void
     */
    Mono<Void> revokeRefreshToken(String refreshToken);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#1-authhub-integration-port)

#### 3.2 RefreshTokenBlacklistPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Refresh Token Blacklist 관리 Port
 *
 * ⚠️ CRITICAL: tenantId 파라미터 필수 (Multi-Tenant Isolation)
 */
public interface RefreshTokenBlacklistPort {

    /**
     * Refresh Token이 Blacklist에 등록되어 있는지 확인 (재사용 감지)
     *
     * @param tenantId Tenant ID (격리 필수!)
     * @param refreshToken Refresh Token
     * @return Blacklist 등록 여부
     */
    Mono<Boolean> isBlacklisted(String tenantId, String refreshToken);

    /**
     * 사용된 Refresh Token을 Blacklist에 추가 (Rotation 시)
     *
     * @param tenantId Tenant ID (격리 필수!)
     * @param refreshToken Refresh Token
     * @param ttl Blacklist TTL (7일)
     * @return Void
     */
    Mono<Void> addToBlacklist(String tenantId, String refreshToken, Duration ttl);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#2-redis-cache-ports)

#### 3.3 RedisLockPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Redis Distributed Lock Port (Race Condition 방지)
 *
 * Redisson RLock 사용
 */
public interface RedisLockPort {

    /**
     * Redis Lock 획득 (Race Condition 방지)
     *
     * @param key Lock Key (e.g., "gateway:lock:refresh:{tenantId}:{userId}")
     * @param timeout Lock 획득 대기 시간 (5초)
     * @return Lock 획득 성공 여부
     */
    Mono<Boolean> acquireLock(String key, Duration timeout);

    /**
     * Redis Lock 해제
     *
     * @param key Lock Key
     * @return Void
     */
    Mono<Void> releaseLock(String key);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#3-redis-lock-port)

---

### 4. Redis Key Design & TTL (Redis Naming Convention)

#### 4.1 Refresh Token Blacklist (사용자별 - Tenant Isolation 필수!)

```java
// Redis Key Pattern
String KEY_PREFIX = "gateway:blacklist:refresh";
String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, tokenHash);

// 예시: gateway:blacklist:refresh:tenant-123:a1b2c3d4e5f6...
// Value: "blacklisted"
// TTL: 7일 (Refresh Token TTL과 동일)

// Adapter 구현 예시 (RefreshTokenBlacklistAdapter)
@Component
@RequiredArgsConstructor
public class RefreshTokenBlacklistAdapter implements RefreshTokenBlacklistPort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "gateway:blacklist:refresh";
    private static final Duration TTL = Duration.ofDays(7);

    @Override
    public Mono<Boolean> isBlacklisted(String tenantId, String refreshToken) {
        String tokenHash = hashToken(refreshToken);  // SHA-256
        String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, tokenHash);

        return redisTemplate.hasKey(key);
    }

    @Override
    public Mono<Void> addToBlacklist(String tenantId, String refreshToken, Duration ttl) {
        String tokenHash = hashToken(refreshToken);
        String key = String.format("%s:%s:%s", KEY_PREFIX, tenantId, tokenHash);

        return redisTemplate.opsForValue()
            .set(key, "blacklisted", ttl)
            .then();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
```

#### 4.2 Redis Lock (Token Refresh Race Condition 방지)

```java
// Redis Key Pattern
String LOCK_PREFIX = "gateway:lock:refresh";
String lockKey = String.format("%s:%s:%s", LOCK_PREFIX, tenantId, userId);

// 예시: gateway:lock:refresh:tenant-123:user-456
// Value: "locked"
// TTL: 5초 (Refresh 시간 고려)

// Adapter 구현 예시 (RedisLockAdapter)
@Component
@RequiredArgsConstructor
public class RedisLockAdapter implements RedisLockPort {

    private final RedissonReactiveClient redissonClient;
    private static final String LOCK_PREFIX = "gateway:lock:refresh";

    @Override
    public Mono<Boolean> acquireLock(String key, Duration timeout) {
        RLockReactive lock = redissonClient.getLock(key);

        return Mono.fromFuture(
            lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public Mono<Void> releaseLock(String key) {
        RLockReactive lock = redissonClient.getLock(key);

        return Mono.fromFuture(lock.unlock())
            .then();
    }
}
```

**Redis Naming Convention 참조**: [Redis Naming Convention & TTL Standards](../redis-naming-convention.md)

**⚠️ CRITICAL FIX**: tenantId 추가됨!
- **이전**: `blacklist:refresh_token:{tokenHash}` (tenantId 누락 - 보안 위험!)
- **현재**: `gateway:blacklist:refresh:{tenantId}:{tokenHash}` (Tenant 격리)

**Tenant Isolation 중요성**:
- tenantId 없이 tokenHash만 사용 시, 테넌트 A의 Token이 테넌트 B의 Blacklist에 영향 가능
- 보안 위험: 크로스 테넌트 Token 재사용 허용 가능성

---

### 5. Use Case 흐름 (RefreshAccessTokenUseCase)

```java
@UseCase
@RequiredArgsConstructor
public class RefreshAccessTokenService implements RefreshAccessTokenPort {

    private final AuthHubPort authHubPort;
    private final RefreshTokenBlacklistPort blacklistPort;
    private final RedisLockPort redisLockPort;

    @Override
    public Mono<RefreshAccessTokenResponse> refreshAccessToken(RefreshAccessTokenCommand command) {
        String lockKey = String.format("gateway:lock:refresh:%s:%s",
            command.tenantId(), command.userId());

        // 1. Redis Lock 획득 (Race Condition 방지)
        return redisLockPort.acquireLock(lockKey, Duration.ofSeconds(5))
            .flatMap(acquired -> {
                if (!acquired) {
                    return Mono.error(new TokenRefreshFailedException("Lock acquisition failed"));
                }

                // 2. Blacklist 확인 (재사용 감지)
                return blacklistPort.isBlacklisted(command.tenantId(), command.refreshToken())
                    .flatMap(isBlacklisted -> {
                        if (isBlacklisted) {
                            return Mono.error(new RefreshTokenReusedException("Token reuse detected"));
                        }

                        // 3. AuthHub에서 새 Token Pair 발급
                        return authHubPort.refreshAccessToken(command.refreshToken())
                            .flatMap(tokenPair ->
                                // 4. 기존 Token Blacklist 추가
                                blacklistPort.addToBlacklist(
                                    command.tenantId(),
                                    command.refreshToken(),
                                    Duration.ofDays(7)
                                ).thenReturn(tokenPair)
                            );
                    });
            })
            // 5. Lock 해제
            .doFinally(signal -> redisLockPort.releaseLock(lockKey).subscribe())
            // 6. Response 변환
            .map(tokenPair -> new RefreshAccessTokenResponse(
                tokenPair.accessToken().value(),
                tokenPair.refreshToken().value()
            ));
    }
}
```

---

### 6. Error Handling (TokenRefreshFilter)

#### 6.1 Error Code Table

| Error Code | HTTP Status | 발생 조건 | 처리 |
|-----------|-------------|---------|------|
| `REFRESH_TOKEN_MISSING` | 401 | Cookie에 Refresh Token 없음 | 로그인 페이지로 리다이렉트 |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 | 재로그인 필요 |
| `REFRESH_TOKEN_REUSED` | 401 | Blacklist에 등록된 Token 재사용 | 탈취 의심, 세션 강제 종료 |
| `REFRESH_TOKEN_INVALID` | 401 | Token 검증 실패 | 재로그인 필요 |
| `TOKEN_REFRESH_FAILED` | 500 | Lock 획득 실패, AuthHub 장애 | 재시도 또는 재로그인 |

#### 6.2 Global Error Handler Integration

```java
@Component
@Order(-2)  // Spring Security 전에 실행
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof RefreshTokenExpiredException) {
            return handleRefreshTokenExpired(exchange);
        } else if (ex instanceof RefreshTokenReusedException) {
            return handleRefreshTokenReused(exchange);
        } else if (ex instanceof RefreshTokenInvalidException) {
            return handleRefreshTokenInvalid(exchange);
        } else if (ex instanceof TokenRefreshFailedException) {
            return handleTokenRefreshFailed(exchange);
        }
        return Mono.error(ex);
    }

    private Mono<Void> handleRefreshTokenExpired(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
            "REFRESH_TOKEN_EXPIRED",
            "Refresh Token has expired. Please login again.",
            Instant.now()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleRefreshTokenReused(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse error = new ErrorResponse(
            "REFRESH_TOKEN_REUSED",
            "Refresh Token reuse detected. Session has been terminated.",
            Instant.now()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

---

### 7. MDC (Mapped Diagnostic Context) 전파

```java
// TokenRefreshFilter에서 MDC 설정
return Mono.deferContextual(ctx -> {
    String traceId = exchange.getAttribute("traceId");
    String userId = exchange.getAttribute("userId");
    String tenantId = exchange.getAttribute("tenantId");

    MDC.put("traceId", traceId);
    MDC.put("userId", userId);
    MDC.put("tenantId", tenantId);

    return refreshAccessTokenPort.refreshAccessToken(command);
})
.contextWrite(context ->
    context.put("traceId", exchange.getAttribute("traceId"))
);
```

**MDC 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#mdc-전파-규칙)

---

### 8. Distributed Lock (Redisson RLock)

#### 8.1 Lock 획득 패턴

```java
// RedisLockAdapter 구현 (Redisson 사용)
@Component
@RequiredArgsConstructor
public class RedisLockAdapter implements RedisLockPort {

    private final RedissonReactiveClient redissonClient;

    @Override
    public Mono<Boolean> acquireLock(String key, Duration timeout) {
        RLockReactive lock = redissonClient.getLock(key);

        // tryLock(waitTime, leaseTime, unit)
        // waitTime: Lock 획득 대기 시간 (5초)
        // leaseTime: Lock 자동 해제 시간 (5초) - Deadlock 방지
        return Mono.fromFuture(
            lock.tryLock(
                timeout.toMillis(),        // waitTime: 5초
                timeout.toMillis(),        // leaseTime: 5초
                TimeUnit.MILLISECONDS
            )
        );
    }

    @Override
    public Mono<Void> releaseLock(String key) {
        RLockReactive lock = redissonClient.getLock(key);

        return Mono.fromFuture(lock.unlock())
            .then()
            .onErrorResume(IllegalMonitorStateException.class, e -> {
                // Lock이 이미 해제된 경우 (TTL 만료)
                log.warn("Lock already released: {}", key);
                return Mono.empty();
            });
    }
}
```

#### 8.2 Lock 사용 패턴 (Use Case)

```java
// RefreshAccessTokenUseCase에서 Lock 사용
String lockKey = String.format("gateway:lock:refresh:%s:%s", tenantId, userId);

return redisLockPort.acquireLock(lockKey, Duration.ofSeconds(5))
    .flatMap(acquired -> {
        if (!acquired) {
            return Mono.error(new TokenRefreshFailedException(
                "Failed to acquire lock. Concurrent refresh in progress."
            ));
        }

        // Token Refresh 로직 실행
        return doRefreshToken(command);
    })
    .doFinally(signal -> {
        // 성공/실패/취소 모두 Lock 해제
        redisLockPort.releaseLock(lockKey)
            .subscribe(
                null,
                error -> log.error("Failed to release lock: {}", lockKey, error)
            );
    });
```

**Redisson 참조**: https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers

---

## 📝 목적

Access Token 만료 시 자동 재발급 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- Refresh Token 기반 Access Token 재발급
- Refresh Token Rotation (보안 강화)
- Refresh Token 재사용 감지 (탈취 방지)
- Race Condition 방지 (Redis Lock)

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
- [ ] **Redisson**: Distributed Lock (Refresh Token Race Condition 방지) - **핵심 기능**
  - RLock 사용 (Race Condition 방지)
  - Lock Timeout: 5초
  - Lock Retry: Exponential Backoff
- [ ] **WebClient**: AuthHub API 연동
  - Connection Timeout: 3초
  - Response Timeout: 3초
  - Circuit Breaker: Resilience4j (Refresh Token 검증 실패 시 캐시 무효화)
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
  - Refresh Token Blacklist: 7일 (Refresh Token TTL과 동일)
  - Redis Lock: 5초 (Refresh 시간 고려)
- [ ] **Redis AUTH**: Production 필수

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.token.refresh.duration (Timer)
  - gateway.token.refresh.success (Counter)
  - gateway.token.refresh.failed (Counter)
  - gateway.token.blacklist.hit (Counter)
  - gateway.redis.lock.acquisition.duration (Timer)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 7-alpine (실제 컨테이너)
- [ ] **WireMock**: AuthHub Mock Server
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

#### Value Objects
- [ ] **RefreshToken**: Refresh Token VO
  - value (String, Refresh Token 문자열)
  - 검증: 최소 32자 이상
  - 검증: Base64 인코딩 유효성

- [ ] **TokenPair**: Access Token + Refresh Token 쌍 VO
  - accessToken (AccessToken, 새 Access Token)
  - refreshToken (RefreshToken, 새 Refresh Token - Rotation)

#### 도메인 비즈니스 규칙
- [ ] **Refresh Token Rotation**: 재발급 시 새 Refresh Token 발급, 기존 Token Blacklist 등록
- [ ] **Refresh Token 재사용 감지**: 동일 Refresh Token 2회 사용 시 탈취 의심

#### 도메인 예외
- [ ] **RefreshTokenExpiredException**: Refresh Token 만료 (401 Unauthorized)
- [ ] **RefreshTokenReusedException**: Refresh Token 재사용 감지 (401 Unauthorized)
- [ ] **RefreshTokenInvalidException**: Refresh Token 검증 실패 (401 Unauthorized)

---

### 🔧 Application Layer

#### Use Case
- [ ] **RefreshAccessTokenUseCase** (Command)
  - **Input**: RefreshAccessTokenCommand
    - refreshToken (String, Refresh Token)
    - **tenantId (String, Tenant ID)** - **⚠️ CRITICAL: Blacklist Key에 필수!**
    - userId (String, User ID)
  - **Output**: RefreshAccessTokenResponse
    - newAccessToken (String, 새 Access Token)
    - newRefreshToken (String, 새 Refresh Token)
  - **Transaction**: 불필요 (읽기 전용 + Redis Atomic Operations)
  - **비즈니스 로직**:
    1. Redis Lock 획득 (Race Condition 방지) - Key: `gateway:lock:refresh:{tenantId}:{userId}`
    2. **RefreshTokenBlacklistPort로 재사용 여부 확인 (tenantId 전달)**
    3. AuthHubPort를 통해 Refresh Token 검증
    4. 새 Access Token 발급
    5. 새 Refresh Token 발급 (Rotation)
    6. **기존 Refresh Token Blacklist 등록 (tenantId 전달)**
    7. Redis Lock 해제

#### Port 정의 (In)
- [ ] **RefreshAccessTokenPort** (In)
  - refreshAccessToken(RefreshAccessTokenCommand): RefreshAccessTokenResponse

#### Port 정의 (Out)

**⚠️ 중요**: 아래 Port들은 Gateway 전체 공통 정의를 따릅니다.
- 참조: [Gateway-Wide Port Matrix](../gateway-port-matrix.md)
- 중복 정의 금지: 이 Task에서는 Port를 새로 정의하지 않고, Port Matrix에 정의된 Port를 사용합니다.

**이 Feature에서 사용하는 Port**:

- [ ] **AuthHubPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#1-authhub-integration-port)
  - refreshAccessToken(String refreshToken): Mono\<TokenPair\>

- [ ] **RefreshTokenBlacklistPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#2-redis-cache-ports)
  - **⚠️ CRITICAL**: tenantId 파라미터 필수 (Multi-Tenant Isolation)
  - isBlacklisted(String tenantId, String refreshToken): Mono\<Boolean\>
  - addToBlacklist(String tenantId, String refreshToken, Duration ttl): Mono\<Void\>

- [ ] **RedisLockPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#3-redis-lock-port)
  - acquireLock(String key, Duration timeout): Mono\<Boolean\>
  - releaseLock(String key): Mono\<Void\>

#### DTO
- [ ] **RefreshAccessTokenCommand** (Record)
- [ ] **RefreshAccessTokenResponse** (Record)

#### Assembler
- [ ] **TokenAssembler**
  - toTokenPair(RefreshAccessTokenResponse): TokenPair

---

### 💾 Persistence Layer (Redis)

#### Repository
- [ ] **RefreshTokenBlacklistRedisRepository**
  - add(String tokenHash, Duration ttl): void
  - exists(String tokenHash): boolean

- [ ] **RedisLockRepository**
  - acquireLock(String key, String value, Duration timeout): boolean
  - releaseLock(String key, String value): boolean

#### Adapter (Port 구현체)
- [ ] **RefreshTokenBlacklistAdapter** (RefreshTokenBlacklistPort 구현)
  - **⚠️ CRITICAL**: tenantId 파라미터 추가됨!
  - isBlacklisted(String tenantId, String refreshToken): Mono\<Boolean\>
    - SHA-256 해시 생성
    - Redis Key: `gateway:blacklist:refresh:{tenantId}:{hash}`
    - Redis에서 Blacklist 조회
  - addToBlacklist(String tenantId, String refreshToken, Duration ttl): Mono\<Void\>
    - SHA-256 해시 생성
    - Redis Key: `gateway:blacklist:refresh:{tenantId}:{hash}`
    - Redis에 Blacklist 추가 (TTL: 7일)

- [ ] **RedisLockAdapter** (RedisLockPort 구현)
  - acquireLock(String key, Duration timeout): boolean
    - Redisson RLock 사용
    - timeout 내 Lock 획득 실패 시 false 반환
  - releaseLock(String key): void
    - Redisson RLock 해제

- [ ] **AuthHubAdapter** (AuthHubPort 구현)
  - refreshAccessToken(String refreshToken): TokenPair
    - AuthHub `/api/v1/auth/refresh` 엔드포인트 호출
    - 새 Access Token + Refresh Token 수령

#### Redis Key Design

**⚠️ 중요**: Redis Key 네이밍은 Gateway 전체 표준을 따릅니다.
- 참조: [Redis Naming Convention & TTL Standards](../redis-naming-convention.md)

```
# Refresh Token Blacklist (사용자별 - Tenant Isolation 필수!)
Key: "gateway:blacklist:refresh:{tenantId}:{tokenHash}"
Value: "blacklisted"
TTL: 7일 (Refresh Token TTL과 동일)
Invalidation: TTL 만료 (Webhook 없음)

# ⚠️ CRITICAL FIX: tenantId 추가됨!
# 이전: "blacklist:refresh_token:{tokenHash}" (tenantId 누락 - 보안 위험!)
# 현재: "gateway:blacklist:refresh:{tenantId}:{tokenHash}" (Tenant 격리)

# Redis Lock (Token Refresh Race Condition 방지)
Key: "gateway:lock:refresh:{tenantId}:{userId}"
Value: "locked"
TTL: 5초
Invalidation: TTL 만료

# ⚠️ CRITICAL FIX: tenantId 추가됨!
# 이전: "token:refresh:lock:{refreshToken}"
# 현재: "gateway:lock:refresh:{tenantId}:{userId}"
```

**Naming Convention 설명**:
- Prefix `gateway:` - Gateway에서 관리하는 데이터
- Feature `blacklist` / `lock` - 기능 구분
- Scope `{tenantId}:{userId}` / `{tenantId}:{tokenHash}` - 테넌트별 격리 필수

**Tenant Isolation 중요성**:
- tenantId 없이 tokenHash만 사용 시, 테넌트 A의 Token이 테넌트 B의 Blacklist에 영향 가능
- 보안 위험: 크로스 테넌트 Token 재사용 허용 가능성

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **TokenRefreshFilter** (GlobalFilter)
  - **Order**: `GatewayFilterOrder.TOKEN_REFRESH_FILTER` (= `Ordered.HIGHEST_PRECEDENCE + 3`)
    - 참조: [Gateway Filter Chain Specification](../gateway-filter-chain.md#filter-chain-실행-순서)
    - 의존성: `userId`, `tenantId` (JwtAuthenticationFilter에서 설정) - **⚠️ CRITICAL: tenantId 필수!**
  - **로직**:
    1. JwtAuthenticationFilter에서 JwtExpiredException 감지
    2. Cookie에서 Refresh Token 추출
    3. **Exchange Attribute에서 tenantId 추출** (Blacklist Key에 사용)
    4. RefreshAccessTokenUseCase 호출 (토큰 재발급, tenantId 전달)
    5. Response Header에 `X-New-Access-Token` 추가
    6. Response Cookie에 `refresh_token` 갱신 (Rotation)
    7. 원래 요청 재시도 (새 Access Token 사용)
  - **예외 처리**:
    - Refresh Token 없음 → 401 Unauthorized
    - Refresh Token 만료 → 401 Unauthorized
    - Refresh Token 재사용 감지 → 401 Unauthorized
    - **tenantId 없음 → 401 Unauthorized (JwtAuthenticationFilter 실행 안 됨)**

#### Error Response
- [ ] **TokenRefreshErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - timestamp (Instant, 발생 시각)

#### Error Handling
- [ ] **TokenRefreshErrorHandler** (ErrorWebExceptionHandler 일부)
  - RefreshTokenExpiredException → `{ "errorCode": "REFRESH_TOKEN_EXPIRED", "message": "Refresh Token expired" }`
  - RefreshTokenReusedException → `{ "errorCode": "REFRESH_TOKEN_REUSED", "message": "Refresh Token reuse detected" }`
  - RefreshTokenInvalidException → `{ "errorCode": "REFRESH_TOKEN_INVALID", "message": "Invalid Refresh Token" }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: JWT 만료 → 자동 재발급 성공**
  - Given: 만료된 Access Token, 유효한 Refresh Token (Cookie)
  - When: `GET /api/v1/orders` 요청 (Authorization: Bearer {expiredAccessToken})
  - Then: 200 OK, Response Header에 `X-New-Access-Token`, Cookie에 새 `refresh_token` (Rotation)
  - 검증: TokenRefreshFilter에서 재발급 성공, 원래 요청 재시도 성공

- [ ] **Scenario 2: Refresh Token 만료 → 401 Unauthorized**
  - Given: 만료된 Access Token, 만료된 Refresh Token
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "REFRESH_TOKEN_EXPIRED" }`
  - 검증: TokenRefreshFilter에서 재발급 실패

- [ ] **Scenario 3: Refresh Token 재사용 감지 → 401 Unauthorized**
  - Given: Refresh Token이 이미 사용됨 (Redis Blacklist 등록)
  - When: 동일한 Refresh Token으로 재발급 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "REFRESH_TOKEN_REUSED" }`
  - 검증: RefreshTokenBlacklistPort.isBlacklisted() == true

- [ ] **Scenario 4: Refresh Token Rotation 검증**
  - Given: 유효한 Refresh Token
  - When: 토큰 재발급 요청
  - Then: 새 Access Token + 새 Refresh Token 수령, 기존 Refresh Token Blacklist 등록
  - 검증: Redis Blacklist에 기존 Refresh Token 존재

- [ ] **Scenario 5: Race Condition 방지 (Redis Lock)**
  - Given: 동일한 Refresh Token으로 2개의 동시 요청
  - When: 두 요청이 거의 동시에 재발급 시도
  - Then: 하나는 성공, 하나는 Lock 획득 실패로 대기 후 재사용 감지
  - 검증: RedisLockPort.acquireLock() 로그 확인

- [ ] **Scenario 6: Refresh Token 없음 → 401 Unauthorized**
  - Given: 만료된 Access Token, Refresh Token 없음 (Cookie 없음)
  - When: `GET /api/v1/orders` 요청
  - Then: 401 Unauthorized, Error Response: `{ "errorCode": "REFRESH_TOKEN_MISSING" }`
  - 검증: TokenRefreshFilter에서 Cookie 없음 감지

#### Testcontainers
- [ ] **Redis Testcontainers**: 실제 Redis 사용
- [ ] **AuthHub Mock Server**: WireMock 사용
  - `/api/v1/auth/refresh` 엔드포인트 Mock

#### TestFixture
- [ ] **RefreshTokenTestFixture**: 테스트용 Refresh Token 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지
- [ ] Refresh Token Rotation 로직 도메인에 위치

#### Application Layer
- [ ] **Transaction 불필요**: RefreshAccessTokenUseCase는 읽기 전용 + Redis Atomic Operations
- [ ] **Race Condition 방지**: Redis Lock 필수 (Redisson RLock)
- [ ] **⚠️ CRITICAL**: tenantId 파라미터 모든 Port 호출 시 전달 필수

#### Persistence Layer
- [ ] **Blacklist TTL**: 7일 (Refresh Token TTL과 동일)
- [ ] **Lock TTL**: 5초 (재발급 시간 고려)

#### Gateway Filter Layer
- [ ] Filter Order: `GatewayFilterOrder.TOKEN_REFRESH_FILTER` (= `HIGHEST_PRECEDENCE + 3`) 고정
- [ ] Cookie 설정: `HttpOnly`, `Secure`, `SameSite=Strict`
- [ ] **⚠️ CRITICAL**: Exchange Attribute에서 tenantId 추출 후 UseCase 전달 필수

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

- [ ] Domain Layer 구현 완료 (VO 2개, Exception 3개)
- [ ] Application Layer 구현 완료 (UseCase 1개, Port 3개, DTO 2개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (Repository 2개, Adapter 3개)
- [ ] Gateway Filter Layer 구현 완료 (Filter 1개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 6개, TestFixture 1개)
- [ ] 모든 테스트 통과 (Unit + Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90%
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

### Gateway 공통 문서
- **Gateway-Wide Port Matrix**: [docs/prd/gateway-port-matrix.md](../gateway-port-matrix.md) - 공통 Port 정의
- **Gateway Filter Chain Specification**: [docs/prd/gateway-filter-chain.md](../gateway-filter-chain.md) - Filter 실행 순서
- **Redis Naming Convention & TTL Standards**: [docs/prd/redis-naming-convention.md](../redis-naming-convention.md) - Redis Key 설계 규칙

### Task 문서
- **PRD**: docs/prd/access-gateway.md (토큰 재발급 섹션)
- **Plan**: docs/prd/plans/GATEWAY-003-token-refresh-plan.md (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/
- Persistence: docs/coding_convention/04-persistence-layer/redis/

### PRD 섹션
- 자동 토큰 재발급 (Line 1494-1591)
- Race Condition 방지 (Line 1547-1589)
