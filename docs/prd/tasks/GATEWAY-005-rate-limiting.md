# GATEWAY-005: Rate Limiting 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: Rate Limiting & Abuse Protection
**브랜치**: feature/GATEWAY-005-rate-limiting
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-6

---

## 🚀 Quick Reference (개발 시 필수 참조)

이 섹션은 개발 중 반복적으로 참조해야 하는 핵심 정보를 모아둔 것입니다.

### 1. Filter Order (Filter Chain 내 위치)

```java
// RateLimitFilter Order: 1 (HIGHEST_PRECEDENCE + 1)
[0] TraceIdFilter (GATEWAY-006) ← traceId 생성
[1] RateLimitFilter ← 이 태스크 ✅
[2] JwtAuthenticationFilter (GATEWAY-001) ← JWT 검증
[3] TokenRefreshFilter (GATEWAY-003) ← Token 갱신
[4] TenantIsolationFilter (GATEWAY-004) ← Tenant 격리
[5] PermissionFilter (GATEWAY-002) ← Permission 검증
[6] MfaVerificationFilter (GATEWAY-007) ← MFA 검증
```

**Filter Chain 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#filter-chain-실행-순서)

**의존성**:
- **Upstream**: TraceIdFilter (GATEWAY-006)에서 `traceId` 생성
- **Downstream**: 없음 (Rate Limit 초과 시 즉시 차단)

**⚠️ CRITICAL**: Rate Limit은 Filter Chain 최상단에서 실행되어 악의적 트래픽을 조기 차단!

---

### 2. Exchange Attributes (Filter 간 데이터 전달)

#### Input Attributes (이 Filter가 사용하는 값)

```java
// TraceIdFilter (GATEWAY-006)에서 설정된 값 사용
String traceId = exchange.getAttribute("traceId");  // 로깅용

// Request에서 직접 추출
String ipAddress = getClientIp(exchange.getRequest());
String path = exchange.getRequest().getPath().toString();
String method = exchange.getRequest().getMethod().name();
```

#### Output Attributes (이 Filter가 설정하는 값)

```java
// Rate Limit 검사 결과 (Response Header)
response.getHeaders().add("X-RateLimit-Limit", String.valueOf(policy.maxRequests()));
response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
response.getHeaders().add("X-RateLimit-Reset", String.valueOf(resetTime));

// Rate Limit 초과 시 Audit Log 기록
exchange.getAttributes().put("rateLimitExceeded", true);
exchange.getAttributes().put("limitType", limitType.name());
```

**Exchange Attributes 참조**: [Gateway Filter Chain Specification](../gateway-filter-chain.md#exchange-attributes-사용-규칙)

---

### 3. Port Definitions (Gateway-Wide 공통 정의)

이 Feature에서 사용하는 Port들 (전체 정의는 [Port Matrix](../gateway-port-matrix.md) 참조):

#### 3.1 RateLimitPolicyQueryPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * Rate Limit 정책 조회 Port
 *
 * Phase 1: Gateway에서 정책 관리 (기본 정책 반환)
 * Phase 2: AuthHub에서 정책 관리 (미래 확장)
 */
public interface RateLimitPolicyQueryPort {

    /**
     * 엔드포인트별 Rate Limit 정책 조회
     *
     * @param path HTTP Path
     * @param method HTTP Method
     * @return Rate Limit Policy (Cache Miss 시 기본 정책: 1,000 req/min)
     */
    Mono<RateLimitPolicy> findPolicy(String path, String method);

    /**
     * 모든 Rate Limit 정책 조회 (초기화용)
     *
     * @return Rate Limit Policy 목록
     */
    Flux<RateLimitPolicy> loadAllPolicies();
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#5-rate-limit-policy-port)

#### 3.2 RateLimitCounterPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Rate Limit Counter 관리 Port (Redis INCR)
 */
public interface RateLimitCounterPort {

    /**
     * Rate Limit Counter 증가 (Atomic)
     *
     * @param key Redis Key (rate_limit:{type}:{key})
     * @param ttl Counter TTL (정책별 상이)
     * @return 증가 후 현재 카운트
     */
    Mono<Long> increment(String key, Duration ttl);

    /**
     * 현재 Counter 조회
     *
     * @param key Redis Key
     * @return 현재 카운트 (없으면 0)
     */
    Mono<Long> get(String key);

    /**
     * Counter 리셋 (Admin API용)
     *
     * @param key Redis Key
     * @return Void
     */
    Mono<Void> reset(String key);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#6-rate-limit-counter-port)

#### 3.3 IpBlockPort (Task-Specific Port)

```java
package com.ryuqq.connectly.gateway.application.port.out;

import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * IP 차단 관리 Port
 */
public interface IpBlockPort {

    /**
     * IP 차단 (Abuse 감지 시)
     *
     * @param ipAddress IP 주소
     * @param duration 차단 기간 (30분)
     * @return Void
     */
    Mono<Void> blockIp(String ipAddress, Duration duration);

    /**
     * IP 차단 여부 확인
     *
     * @param ipAddress IP 주소
     * @return 차단 여부
     */
    Mono<Boolean> isBlocked(String ipAddress);
}
```

**참조**: [Gateway Port Matrix](../gateway-port-matrix.md#7-ip-block-port)

---

### 4. Redis Key Design & TTL (Redis Naming Convention)

#### 4.1 Rate Limit Counter (엔드포인트/사용자/IP별)

```java
// Redis Key Patterns (LimitType별)
String ENDPOINT_KEY = "gateway:rate_limit:endpoint:{path}:{method}";
String USER_KEY = "gateway:rate_limit:user:{userId}";
String IP_KEY = "gateway:rate_limit:ip:{ipAddress}";
String OTP_KEY = "gateway:rate_limit:otp:{phoneNumber}";
String LOGIN_KEY = "gateway:rate_limit:login:{ipAddress}";
String TOKEN_REFRESH_KEY = "gateway:rate_limit:token_refresh:{userId}";
String INVALID_JWT_KEY = "gateway:rate_limit:invalid_jwt:{ipAddress}";

// Adapter 구현 예시 (RateLimitCounterAdapter)
@Component
@RequiredArgsConstructor
public class RateLimitCounterAdapter implements RateLimitCounterPort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Long> increment(String key, Duration ttl) {
        return redisTemplate.opsForValue()
            .increment(key)  // Redis INCR (Atomic)
            .flatMap(count -> {
                if (count == 1) {
                    // 첫 요청 시 TTL 설정
                    return redisTemplate.expire(key, ttl)
                        .thenReturn(count);
                }
                return Mono.just(count);
            });
    }

    @Override
    public Mono<Long> get(String key) {
        return redisTemplate.opsForValue()
            .get(key)
            .map(Long::parseLong)
            .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> reset(String key) {
        return redisTemplate.delete(key).then();
    }
}
```

**TTL 정책**:
- 엔드포인트: 1분 (1,000 req/min)
- 사용자: 1분 (100 req/min)
- IP: 5분 (10 req/5min for login)
- OTP: 1시간 (3 req/hour)
- Token Refresh: 1분 (3 req/min)
- Invalid JWT: 5분 (10 req/5min)

#### 4.2 IP Block (Abuse 감지 시)

```java
// Redis Key Pattern
String IP_BLOCK_KEY = "gateway:ip_block:{ipAddress}";
// Value: "BLOCKED"
// TTL: 30분

// Adapter 구현 예시 (IpBlockAdapter)
@Component
@RequiredArgsConstructor
public class IpBlockAdapter implements IpBlockPort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "gateway:ip_block";
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(30);

    @Override
    public Mono<Void> blockIp(String ipAddress, Duration duration) {
        String key = String.format("%s:%s", KEY_PREFIX, ipAddress);
        return redisTemplate.opsForValue()
            .set(key, "BLOCKED", duration)
            .then();
    }

    @Override
    public Mono<Boolean> isBlocked(String ipAddress) {
        String key = String.format("%s:%s", KEY_PREFIX, ipAddress);
        return redisTemplate.hasKey(key);
    }
}
```

**Redis Naming Convention 참조**: [Redis Naming Convention & TTL Standards](../redis-naming-convention.md)

---

### 5. Rate Limit 정책 (LimitType별)

#### 5.1 정책 테이블

| LimitType | Key Pattern | Max Requests | Window | Action | Audit Log |
|-----------|------------|--------------|--------|--------|-----------|
| **ENDPOINT** | `gateway:rate_limit:endpoint:{path}:{method}` | 1,000 | 1분 | REJECT (429) | ❌ |
| **USER** | `gateway:rate_limit:user:{userId}` | 100 | 1분 | REJECT (429) | ❌ |
| **IP** | `gateway:rate_limit:ip:{ipAddress}` | 100 | 1분 | REJECT (429) | ❌ |
| **OTP** | `gateway:rate_limit:otp:{phoneNumber}` | 3 | 1시간 | REJECT (429) | ✅ |
| **LOGIN** | `gateway:rate_limit:login:{ipAddress}` | 5 | 5분 | BLOCK_IP (30분) | ✅ |
| **TOKEN_REFRESH** | `gateway:rate_limit:token_refresh:{userId}` | 3 | 1분 | REVOKE_TOKEN | ✅ |
| **INVALID_JWT** | `gateway:rate_limit:invalid_jwt:{ipAddress}` | 10 | 5분 | BLOCK_IP (30분) | ✅ |

#### 5.2 Policy Adapter 구현

```java
@Component
@RequiredArgsConstructor
public class RateLimitPolicyAdapter implements RateLimitPolicyQueryPort {

    @Override
    public Mono<RateLimitPolicy> findPolicy(String path, String method) {
        // Phase 1: 기본 정책 반환 (Gateway에서 하드코딩)
        return Mono.just(
            new RateLimitPolicy(
                LimitType.ENDPOINT,
                String.format("gateway:rate_limit:endpoint:%s:%s", path, method),
                1000,  // maxRequests
                Duration.ofMinutes(1),  // window
                RateLimitAction.REJECT,  // action
                false  // auditLogRequired
            )
        );
    }
}
```

---

### 6. Use Case 흐름 (CheckRateLimitUseCase)

```java
@UseCase
@RequiredArgsConstructor
public class CheckRateLimitService implements CheckRateLimitPort {

    private final RateLimitPolicyQueryPort policyPort;
    private final RateLimitCounterPort counterPort;
    private final IpBlockPort ipBlockPort;
    private final AuditLogPort auditLogPort;

    @Override
    public Mono<CheckRateLimitResponse> checkRateLimit(CheckRateLimitCommand command) {
        // 1. IP 차단 여부 확인 (Login/Invalid JWT Abuse)
        return ipBlockPort.isBlocked(command.ipAddress())
            .flatMap(isBlocked -> {
                if (isBlocked) {
                    return Mono.error(new IpBlockedException(
                        String.format("IP blocked due to abuse: %s", command.ipAddress())
                    ));
                }

                // 2. Rate Limit Policy 조회
                return policyPort.findPolicy(command.path(), command.method())
                    .flatMap(policy -> {
                        // 3. Redis Counter 증가 (Atomic)
                        String key = String.format(policy.keyPattern(),
                            command.limitType().getKeyValue(command));

                        return counterPort.increment(key, policy.window())
                            .flatMap(currentCount -> {
                                int remaining = (int) (policy.maxRequests() - currentCount);

                                // 4. 정책 초과 여부 확인
                                if (currentCount > policy.maxRequests()) {
                                    // 5. Audit Log 기록 (필수 정책만)
                                    if (policy.auditLogRequired()) {
                                        return auditLogPort.log(
                                            AuditEventType.RATE_LIMIT_EXCEEDED,
                                            Map.of(
                                                "limitType", command.limitType().name(),
                                                "key", command.key(),
                                                "currentCount", currentCount,
                                                "limit", policy.maxRequests()
                                            )
                                        ).thenReturn(new CheckRateLimitResponse(
                                            false,  // allowed
                                            currentCount,
                                            policy.maxRequests(),
                                            remaining < 0 ? 0 : remaining
                                        ));
                                    }
                                }

                                return Mono.just(new CheckRateLimitResponse(
                                    true,  // allowed
                                    currentCount,
                                    policy.maxRequests(),
                                    remaining
                                ));
                            });
                    });
            });
    }
}
```

---

### 7. Error Handling (RateLimitFilter)

#### 7.1 Error Code Table

| Error Code | HTTP Status | 발생 조건 | 처리 |
|-----------|-------------|---------|------|
| `RATE_LIMIT_EXCEEDED` | 429 | Rate Limit 초과 | Retry-After 헤더 포함 |
| `IP_BLOCKED` | 403 | IP 차단됨 (Abuse) | 30분 후 재시도 |
| `ACCOUNT_LOCKED` | 403 | 계정 잠금됨 | 관리자 문의 |
| `OTP_ABUSE_DETECTED` | 429 | OTP 남용 감지 | 1시간 후 재시도 |

#### 7.2 Global Error Handler Integration

```java
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof RateLimitExceededException) {
            return handleRateLimitExceeded(exchange, (RateLimitExceededException) ex);
        } else if (ex instanceof IpBlockedException) {
            return handleIpBlocked(exchange, (IpBlockedException) ex);
        }
        return Mono.error(ex);
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange,
                                                RateLimitExceededException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("Retry-After", "60");  // 60초 후 재시도

        RateLimitErrorResponse error = new RateLimitErrorResponse(
            "RATE_LIMIT_EXCEEDED",
            "Too many requests. Please try again later.",
            ex.getLimit(),
            ex.getRemaining(),
            60  // retryAfter
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleIpBlocked(ServerWebExchange exchange, IpBlockedException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        RateLimitErrorResponse error = new RateLimitErrorResponse(
            "IP_BLOCKED",
            "IP blocked due to abuse. Please try again in 30 minutes.",
            0, 0, 1800  // retryAfter: 30분
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(objectMapper.writeValueAsBytes(error));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

---

### 8. Admin API (Rate Limit 수동 리셋)

```java
@RestController
@RequestMapping("/admin/rate-limit")
@RequiredArgsConstructor
public class RateLimitAdminController {

    private final ResetRateLimitPort resetRateLimitPort;

    /**
     * Rate Limit 수동 리셋 (긴급 해제)
     *
     * DELETE /admin/rate-limit/{limitType}/{key}
     * 예: DELETE /admin/rate-limit/otp/01012345678
     */
    @DeleteMapping("/{limitType}/{key}")
    public Mono<ResponseEntity<Void>> resetRateLimit(
        @PathVariable String limitType,
        @PathVariable String key
    ) {
        ResetRateLimitCommand command = new ResetRateLimitCommand(
            LimitType.valueOf(limitType.toUpperCase()),
            key
        );

        return resetRateLimitPort.resetRateLimit(command)
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}
```

---

## 📝 목적

Rate Limiting 기반 보안 공격 방어 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- 엔드포인트/사용자/IP별 Rate Limit 적용
- OTP 남용 방지 (SMS 폭탄)
- Brute Force 공격 방지 (로그인 시도 제한)
- Refresh Token 남용 방지
- 잘못된 JWT 반복 제출 차단
- Audit Log 자동 기록

**이 Feature는 독립적으로 배포 가능한 완전한 기능 단위입니다.**

---

## 🏗️ Infrastructure & Tech Stack

### Core Framework
- [ ] **Spring Cloud Gateway 3.1.x**: Filter Chain 기반 라우팅
- [ ] **Spring WebFlux**: Reactive Non-Blocking I/O
- [ ] **Netty**: 비동기 이벤트 기반 서버
- [ ] **Project Reactor**: Mono/Flux 기반 Reactive Programming

### Reactive Stack
- [ ] **Lettuce**: Reactive Redis Client (Connection Pool 관리) - **핵심 기능**
  - INCR/DECR Atomic 연산 (Rate Limit Counter)
  - TTL 기반 자동 만료
- [ ] **Redisson**: Distributed Lock (미래 확장용)
- [ ] **WebClient**: AuthHub API 연동 (Refresh Token 무효화)
  - Connection Timeout: 3초
  - Response Timeout: 3초
  - Circuit Breaker: Resilience4j
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
  - Rate Limit Counter: 정책별 상이 (1분~1시간)
  - IP Block: 30분
  - Rate Limit Policy Cache: 1시간
- [ ] **Redis AUTH**: Production 필수

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.rate_limit.endpoint.check.duration (Timer)
  - gateway.rate_limit.exceeded (Counter, tags: limitType)
  - gateway.rate_limit.ip.blocked (Counter)
  - gateway.rate_limit.otp.abuse (Counter)
  - gateway.rate_limit.login.brute_force (Counter)
  - gateway.redis.counter.incr.duration (Timer)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId, ipAddress

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 7-alpine (실제 컨테이너)
- [ ] **WireMock**: AuthHub Mock Server (Refresh Token 무효화)
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
- [ ] **RateLimitPolicy**: Rate Limit 정책 도메인 모델
  - limitType (LimitType Enum, 제한 타입)
  - keyPattern (String, Redis Key 패턴)
  - maxRequests (int, 최대 요청 수)
  - window (Duration, 시간 창)
  - action (RateLimitAction, 초과 시 조치)
  - auditLogRequired (boolean, Audit Log 필수 여부)

#### Value Objects
- [ ] **RateLimitKey**: Rate Limit Redis Key VO
  - value (String, Redis Key)
  - 검증: Key 패턴 유효성

- [ ] **RequestCount**: 요청 횟수 VO
  - count (long, 현재 카운트)
  - limit (int, 최대 허용)
  - 검증: count >= 0

- [ ] **RateLimitWindow**: 시간 창 VO
  - duration (Duration, 시간 창)
  - unit (ChronoUnit, 시간 단위)
  - 검증: duration > 0

#### Enum
- [ ] **LimitType**: Rate Limit 타입
  - ENDPOINT (엔드포인트별)
  - USER (사용자별)
  - IP (IP별)
  - OTP (OTP 요청)
  - LOGIN (로그인 API)
  - TOKEN_REFRESH (토큰 재발급)
  - INVALID_JWT (잘못된 JWT)
  - PASSWORD_FAIL (비밀번호 실패)

- [ ] **RateLimitAction**: 초과 시 조치
  - REJECT (429 Too Many Requests)
  - BLOCK_IP (IP 차단 30분)
  - LOCK_ACCOUNT (계정 잠금 30분)
  - REVOKE_TOKEN (Refresh Token 무효화)

#### 도메인 비즈니스 규칙
- [ ] **Rate Limit 정책 검증**: maxRequests > 0, window > 0
- [ ] **보안 공격 감지**: 임계값 초과 시 자동 차단
- [ ] **Audit Log 필수**: 보안 관련 제한은 반드시 로그 기록

#### 도메인 예외
- [ ] **RateLimitExceededException**: Rate Limit 초과 (429 Too Many Requests)
- [ ] **IpBlockedException**: IP 차단됨 (403 Forbidden)
- [ ] **AccountLockedException**: 계정 잠금됨 (403 Forbidden)

---

### 🔧 Application Layer

#### Use Case
- [ ] **CheckRateLimitUseCase** (Command)
  - **Input**: CheckRateLimitCommand
    - limitType (LimitType, 제한 타입)
    - key (String, 제한 대상 키 - userId/IP/phoneNumber 등)
  - **Output**: CheckRateLimitResponse
    - allowed (boolean, 허용 여부)
    - currentCount (long, 현재 카운트)
    - limit (int, 최대 허용)
    - remaining (int, 남은 요청 수)
  - **Transaction**: 불필요 (읽기 전용)
  - **비즈니스 로직**:
    1. RateLimitPolicyPort로 정책 조회
    2. RateLimitCounterPort로 현재 카운트 조회 및 증가
    3. 정책 초과 여부 확인
    4. 초과 시 AuditLogPort 호출 (필수 정책만)

- [ ] **GetRateLimitPolicyUseCase** (Query)
  - **Input**: GetRateLimitPolicyQuery
    - path (String, 엔드포인트 경로)
    - method (String, HTTP 메서드)
  - **Output**: GetRateLimitPolicyResponse
    - policy (RateLimitPolicy, 적용할 정책)
  - **Transaction**: 불필요 (읽기 전용)

- [ ] **ResetRateLimitUseCase** (Command)
  - **Input**: ResetRateLimitCommand
    - limitType (LimitType, 제한 타입)
    - key (String, 제한 대상 키)
  - **Output**: ResetRateLimitResponse
    - success (boolean, 리셋 성공 여부)
  - **Transaction**: 불필요 (Redis 삭제 연산)
  - **Admin API**: `DELETE /admin/rate-limit/{limitType}/{key}` 용도

#### Port 정의 (In)
- [ ] **CheckRateLimitPort** (In)
  - checkRateLimit(CheckRateLimitCommand): CheckRateLimitResponse

- [ ] **GetRateLimitPolicyPort** (In)
  - getRateLimitPolicy(GetRateLimitPolicyQuery): GetRateLimitPolicyResponse

- [ ] **ResetRateLimitPort** (In)
  - resetRateLimit(ResetRateLimitCommand): ResetRateLimitResponse

#### Port 정의 (Out)

**⚠️ 중요**: 아래 Port들은 Gateway 전체 공통 정의를 따릅니다.
- 참조: [Gateway-Wide Port Matrix](../gateway-port-matrix.md)

**⚠️ 정책 관리 주체 명확화**:
- **Phase 1 (현재 구현)**: Gateway에서 정책 관리
  - Gateway 배포 시 정책 하드코딩 또는 Redis에 직접 저장
  - Cache Miss 시 기본 정책 (1,000 req/min) 반환
  - 정책 변경: Gateway 코드 수정 또는 Redis 직접 수정
- **Phase 2 (미래, Optional)**: AuthHub에서 정책 관리
  - AuthHub API를 통해 정책 조회 (`/api/v1/rate-limits/policies`)
  - Webhook 기반 정책 동기화 (Permission Spec과 동일 패턴)
  - Gateway는 Cache만 유지 (AuthHub가 단일 소스)
  - 장점: 중앙 집중식 정책 관리, UI 기반 정책 수정 가능

**이 Feature에서 사용하는 Port (Phase 1)**:

- [ ] **RateLimitPolicyQueryPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#2-redis-cache-ports)
  - findPolicy(String path, String method): Mono\<RateLimitPolicy\>
    - Redis 조회 (Cache Hit → 즉시 반환)
    - **Cache Miss → 기본 정책 반환 (Gateway에서 하드코딩)**
      - 기본 정책: 1,000 req/min (엔드포인트별)
  - loadAllPolicies(): Flux\<RateLimitPolicy\>

- [ ] **RateLimitCounterPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#2-redis-cache-ports)
  - increment(String key, Duration ttl): Mono\<Long\>
  - get(String key): Mono\<Long\>
  - reset(String key): Mono\<Void\>

- [ ] **AuditLogPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#4-audit-log-port)
  - log(AuditEventType eventType, Map\<String, Object\> metadata): Mono\<Void\>

- [ ] **IpBlockPort** (Out) - [Port Matrix 참조](../gateway-port-matrix.md#2-redis-cache-ports)
  - blockIp(String ipAddress, Duration duration): Mono\<Void\>
  - isBlocked(String ipAddress): Mono\<Boolean\>

#### DTO
- [ ] **CheckRateLimitCommand** (Record)
- [ ] **CheckRateLimitResponse** (Record)
- [ ] **GetRateLimitPolicyQuery** (Record)
- [ ] **GetRateLimitPolicyResponse** (Record)
- [ ] **ResetRateLimitCommand** (Record)
- [ ] **ResetRateLimitResponse** (Record)

#### Assembler
- [ ] **RateLimitAssembler**
  - toRateLimitPolicy(RateLimitPolicyEntity): RateLimitPolicy

---

### 💾 Persistence Layer (Redis)

#### Entity
- [ ] **RateLimitPolicyEntity**: Rate Limit 정책 캐시 엔티티
  - path (String, 엔드포인트 경로)
  - method (String, HTTP 메서드)
  - limitType (String, 제한 타입)
  - keyPattern (String, Redis Key 패턴)
  - maxRequests (int, 최대 요청 수)
  - windowSeconds (int, 시간 창 - 초)
  - action (String, 초과 시 조치)
  - auditLogRequired (boolean, Audit Log 필수 여부)

#### Repository
- [ ] **RateLimitCounterRedisRepository**
  - increment(String key, Duration ttl): long
  - get(String key): long
  - delete(String key): void

- [ ] **RateLimitPolicyRedisRepository**
  - save(String path, String method, RateLimitPolicyEntity policy): void
  - findByPathAndMethod(String path, String method): Optional<RateLimitPolicyEntity>
  - findAll(): List<RateLimitPolicyEntity>

- [ ] **IpBlockRedisRepository**
  - block(String ipAddress, Duration duration): void
  - isBlocked(String ipAddress): boolean
  - delete(String ipAddress): void

#### Adapter (Port 구현체)
- [ ] **RateLimitPolicyAdapter** (RateLimitPolicyPort 구현)
  - findPolicy(String path, String method): RateLimitPolicy
    - Redis 조회 (Cache Hit → 즉시 반환)
    - Cache Miss → 기본 정책 반환 (1,000 req/min)

- [ ] **RateLimitCounterAdapter** (RateLimitCounterPort 구현)
  - increment(String key, Duration ttl): long
    - Redis INCR 연산
    - 첫 요청 시 TTL 설정
  - get(String key): long
    - Redis GET 연산
  - reset(String key): void
    - Redis DEL 연산

- [ ] **IpBlockAdapter** (IpBlockPort 구현)
  - blockIp(String ipAddress, Duration duration): void
    - Redis에 IP 차단 키 저장
  - isBlocked(String ipAddress): boolean
    - Redis에서 차단 키 조회

#### Mapper
- [ ] **RateLimitPolicyMapper**
  - toRateLimitPolicy(RateLimitPolicyEntity entity): RateLimitPolicy
  - toRateLimitPolicyEntity(RateLimitPolicy policy): RateLimitPolicyEntity

#### Redis Key Design
```
# Rate Limit Counter
Key: "rate_limit:endpoint:{path}:{method}"
Key: "rate_limit:user:{userId}"
Key: "rate_limit:ip:{ipAddress}"
Key: "rate_limit:otp:{phoneNumber}"
Key: "rate_limit:login:{ipAddress}"
Key: "rate_limit:token_refresh:{userId}"
Key: "rate_limit:invalid_jwt:{ipAddress}"
Key: "rate_limit:password_fail:{email}"
Value: "{count}" (숫자)
TTL: 정책별 상이 (1분~1시간)

# IP Block
Key: "ip_block:{ipAddress}"
Value: "BLOCKED"
TTL: 30분

# Rate Limit Policy (Cache)
Key: "rate_limit_policy:{path}:{method}"
Value: RateLimitPolicyEntity (JSON)
TTL: 1시간
```

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **RateLimitFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 2`
  - **로직**:
    1. IP Block 여부 확인 (IpBlockPort)
    2. 엔드포인트 기반 Rate Limit Policy 조회
    3. CheckRateLimitUseCase 호출
    4. 초과 시 429 Too Many Requests 반환
    5. Response Header에 X-RateLimit-Limit, X-RateLimit-Remaining 추가
  - **예외 처리**:
    - IP 차단됨 → 403 Forbidden
    - Rate Limit 초과 → 429 Too Many Requests

- [ ] **OtpRateLimitFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 2` (RateLimitFilter 내부)
  - **로직**:
    1. OTP 요청 여부 확인 (POST /api/v1/auth/otp)
    2. CheckRateLimitUseCase 호출 (limitType: OTP, key: phoneNumber)
    3. 초과 시 429 Too Many Requests + Audit Log 기록
  - **제한**: 1시간 3회

- [ ] **LoginRateLimitFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 2` (RateLimitFilter 내부)
  - **로직**:
    1. Login 요청 여부 확인 (POST /api/v1/auth/login)
    2. CheckRateLimitUseCase 호출 (limitType: LOGIN, key: ipAddress)
    3. 5회 초과 시 429, 10회 초과 시 IP 차단 30분
  - **제한**: 5분 5회

- [ ] **TokenRefreshRateLimitFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 2` (RateLimitFilter 내부)
  - **로직**:
    1. Token Refresh 요청 여부 확인 (POST /api/v1/auth/refresh)
    2. CheckRateLimitUseCase 호출 (limitType: TOKEN_REFRESH, key: userId)
    3. 초과 시 429 + Refresh Token 무효화
  - **제한**: 1분 3회

- [ ] **InvalidJwtRateLimitFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE + 3` (JwtAuthenticationFilter 내부)
  - **로직**:
    1. JWT 검증 실패 시 카운터 증가
    2. CheckRateLimitUseCase 호출 (limitType: INVALID_JWT, key: ipAddress)
    3. 10회 초과 시 IP 차단 30분
  - **제한**: 5분 10회

#### Admin Controller
- [ ] **RateLimitAdminController** (RestController)
  - **Endpoint**: `DELETE /admin/rate-limit/{limitType}/{key}`
  - **목적**: Admin이 수동으로 Rate Limit 리셋 (긴급 해제)
  - **로직**:
    1. ResetRateLimitUseCase 호출
    2. 성공 시 200 OK 반환
  - **보안**: Admin Role 필수

#### Error Response
- [ ] **RateLimitErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - limit (int, 최대 허용)
  - remaining (int, 남은 요청 수)
  - retryAfter (int, 다시 시도 가능한 시간 - 초)

#### Error Handling
- [ ] **RateLimitErrorHandler** (ErrorWebExceptionHandler 일부)
  - RateLimitExceededException → `{ "errorCode": "RATE_LIMIT_EXCEEDED", "limit": 100, "remaining": 0, "retryAfter": 60 }`
  - IpBlockedException → `{ "errorCode": "IP_BLOCKED", "message": "IP blocked due to abuse" }`
  - AccountLockedException → `{ "errorCode": "ACCOUNT_LOCKED", "message": "Account locked due to too many failures" }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: 엔드포인트 Rate Limit 성공**
  - Given: `/api/v1/orders` 엔드포인트 Rate Limit = 1,000 req/min
  - When: 100회 요청
  - Then: 200 OK, Response Header에 `X-RateLimit-Limit: 1000`, `X-RateLimit-Remaining: 900`
  - 검증: RateLimitFilter 통과

- [ ] **Scenario 2: 엔드포인트 Rate Limit 초과 → 429**
  - Given: `/api/v1/orders` 엔드포인트 Rate Limit = 10 req/min (테스트용)
  - When: 11회 요청
  - Then: 11번째 요청 → 429 Too Many Requests
  - 검증: RateLimitFilter에서 초과 감지

- [ ] **Scenario 3: OTP 남용 감지 → 1시간 차단**
  - Given: OTP Rate Limit = 3 req/hour
  - When: 동일 전화번호로 4회 요청
  - Then: 4번째 요청 → 429 Too Many Requests, Audit Log 기록 (`OTP_ABUSE_DETECTED`)
  - 검증: OtpRateLimitFilter에서 초과 감지, Redis TTL 1시간

- [ ] **Scenario 4: Brute Force 공격 감지 → IP 차단**
  - Given: Login Rate Limit = 5 req/5min (IP별)
  - When: 동일 IP에서 11회 로그인 시도
  - Then: 6번째~10번째 → 429, 11번째 → IP 차단 30분
  - 검증: LoginRateLimitFilter에서 IP 차단, Redis `ip_block:{ip}` 존재

- [ ] **Scenario 5: Refresh Token 남용 → Token 무효화**
  - Given: Token Refresh Rate Limit = 3 req/min (User별)
  - When: 동일 사용자로 4회 재발급 시도
  - Then: 4번째 요청 → 429, Refresh Token 무효화
  - 검증: TokenRefreshRateLimitFilter에서 초과 감지, AuthHub API 호출 확인

- [ ] **Scenario 6: 잘못된 JWT 반복 제출 → IP 차단**
  - Given: Invalid JWT Rate Limit = 10 req/5min (IP별)
  - When: 동일 IP에서 잘못된 JWT 11회 제출
  - Then: 11번째 요청 → 403 Forbidden, IP 차단 30분
  - 검증: InvalidJwtRateLimitFilter에서 IP 차단

- [ ] **Scenario 7: Admin이 Rate Limit 수동 리셋**
  - Given: OTP Rate Limit 초과로 차단됨
  - When: Admin이 `DELETE /admin/rate-limit/otp/{phoneNumber}` 호출
  - Then: 200 OK, Redis 카운터 삭제, 즉시 재요청 가능
  - 검증: ResetRateLimitUseCase 호출 성공

- [ ] **Scenario 8: Response Header에 Rate Limit 정보 포함**
  - Given: User Rate Limit = 100 req/min
  - When: 10회 요청
  - Then: Response Header에 `X-RateLimit-Limit: 100`, `X-RateLimit-Remaining: 90`
  - 검증: RateLimitFilter에서 헤더 추가

#### Testcontainers
- [ ] **Redis Testcontainers**: 실제 Redis 사용
- [ ] **AuthHub Mock Server**: WireMock 사용
  - `/api/v1/auth/revoke-refresh-token` 엔드포인트 Mock (Refresh Token 무효화)

#### TestFixture
- [ ] **RateLimitPolicyTestFixture**: 테스트용 Rate Limit Policy 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지
- [ ] Rate Limit 정책 검증 로직 도메인에 위치

#### Application Layer
- [ ] Transaction 불필요 (읽기 전용 Use Case)
- [ ] Port 의존성 역전

#### Persistence Layer
- [ ] Counter TTL: 정책별 상이 (1분~1시간)
- [ ] IP Block TTL: 30분

#### Gateway Filter Layer
- [ ] Filter Order: RateLimitFilter `HIGHEST_PRECEDENCE + 2` (TraceIdFilter 다음)
- [ ] Audit Log 필수: 보안 관련 Rate Limit 초과 시 반드시 기록

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

- [ ] Domain Layer 구현 완료 (Aggregate 1개, VO 3개, Enum 2개, Exception 3개)
- [ ] Application Layer 구현 완료 (UseCase 3개, Port 7개, DTO 6개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (Entity 1개, Repository 3개, Adapter 3개, Mapper 1개)
- [ ] Gateway Filter Layer 구현 완료 (Filter 5개, Controller 1개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 8개, TestFixture 1개)
- [ ] 모든 테스트 통과 (Unit + Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90%
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: docs/prd/access-gateway.md (Rate Limiting 섹션)
- **Plan**: docs/prd/plans/GATEWAY-005-rate-limiting-plan.md (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/
- Persistence: docs/coding_convention/04-persistence-layer/redis/

### PRD 섹션
- Rate Limiting & Abuse Protection (Line 2067-2465)
- Rate Limit 규칙 확장 (Line 2077-2089)
- 보안 공격 방어 메커니즘 (Line 2092-2402)
