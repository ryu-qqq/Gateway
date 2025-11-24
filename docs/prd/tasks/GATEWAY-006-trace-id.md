# GATEWAY-006: Trace-ID 생성 및 전달 기능 (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: Trace-ID 생성 및 전달 (Distributed Tracing)
**브랜치**: feature/GATEWAY-006-trace-id
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-7

---

## 🚀 Quick Reference (개발 시 필수 참조)

이 섹션은 **GATEWAY-006 Trace-ID 기능 개발 시 필요한 모든 정보를 한 곳에 모아둔 통합 참조 가이드**입니다. 다른 문서를 참조하지 않고도 이 섹션만으로 개발을 완료할 수 있습니다.

### 1️⃣ Filter Order (Filter Chain 실행 순서)

Trace-ID는 **가장 먼저 실행되는 Filter**입니다 (Position 0: `Ordered.HIGHEST_PRECEDENCE`).

```
Gateway Filter Chain 실행 순서:
┌─────────────────────────────────────────────────────────────────────┐
│ 0. TraceIdFilter (HIGHEST_PRECEDENCE) ← 👈 GATEWAY-006 (이 태스크)   │
│    └─ Trace-ID 생성 → X-Trace-Id 헤더 추가 → MDC 설정              │
├─────────────────────────────────────────────────────────────────────┤
│ 1. RateLimitFilter (HIGHEST_PRECEDENCE + 1)                         │
│    └─ Rate Limit 체크 → 초과 시 429 반환                            │
├─────────────────────────────────────────────────────────────────────┤
│ 2. JwtAuthenticationFilter (HIGHEST_PRECEDENCE + 2)                 │
│    └─ JWT 검증 → userId, tenantId, permissions 추출                 │
├─────────────────────────────────────────────────────────────────────┤
│ 3. TokenRefreshFilter (HIGHEST_PRECEDENCE + 3)                      │
│    └─ Refresh Token 처리 → Access Token 재발급                      │
├─────────────────────────────────────────────────────────────────────┤
│ 4. TenantIsolationFilter (HIGHEST_PRECEDENCE + 4)                   │
│    └─ Tenant Config 조회 → Policy 적용                              │
├─────────────────────────────────────────────────────────────────────┤
│ 5. PermissionCheckFilter (HIGHEST_PRECEDENCE + 5)                   │
│    └─ Permission 검증 → 403 반환                                     │
├─────────────────────────────────────────────────────────────────────┤
│ 6. MfaRequiredFilter (HIGHEST_PRECEDENCE + 6)                       │
│    └─ MFA 필요 시 451 반환                                           │
├─────────────────────────────────────────────────────────────────────┤
│ 7. Backend Service Routing                                          │
│    └─ Circuit Breaker → Load Balancer → Downstream Service          │
└─────────────────────────────────────────────────────────────────────┘

👉 TraceIdFilter는 모든 필터보다 먼저 실행되어 모든 로그에 Trace-ID가 포함되도록 보장합니다.
```

**Filter 구현**:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements GlobalFilter {

    private final GenerateTraceIdPort generateTraceIdPort;

    public TraceIdFilter(GenerateTraceIdPort generateTraceIdPort) {
        this.generateTraceIdPort = generateTraceIdPort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Trace-ID 생성
        return generateTraceIdPort.generateTraceId(new GenerateTraceIdCommand())
            .flatMap(response -> {
                String traceId = response.traceId();

                // 2. Request Header에 X-Trace-Id 추가 (Downstream 전달)
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Trace-Id", traceId)
                    .build();

                // 3. Exchange Attribute에 traceId 저장 (다른 Filter에서 사용)
                exchange.getAttributes().put("traceId", traceId);

                // 4. MDC에 traceId 추가 (로깅용)
                return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .contextWrite(Context.of("traceId", traceId))
                    .doFinally(signalType -> {
                        // 5. Response Header에 X-Trace-Id 추가 (Client 반환)
                        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
                    });
            });
    }
}
```

### 2️⃣ Exchange Attributes (Filter 간 데이터 전달)

TraceIdFilter는 다음 데이터를 **Exchange Attributes에 저장**하여 다른 Filter에서 사용할 수 있도록 합니다.

| Attribute Key | Type | 설명 | 설정 위치 | 사용 위치 |
|---------------|------|------|-----------|-----------|
| `traceId` | String | 생성된 Trace-ID (`{timestamp}-{UUID}` 형식) | TraceIdFilter | 모든 Filter (로그, 에러 응답) |

**Exchange Attributes 접근 방법**:
```java
// TraceIdFilter에서 설정
exchange.getAttributes().put("traceId", traceId);

// 다른 Filter에서 읽기
String traceId = exchange.getAttribute("traceId");

// ErrorHandler에서 Trace-ID 포함
ErrorResponse errorResponse = new ErrorResponse(
    "RATE_LIMIT_EXCEEDED",
    "Too many requests",
    exchange.getAttribute("traceId")  // 👈 Trace-ID 포함
);
```

### 3️⃣ Trace-ID 형식 (Format Specification)

**형식**: `{timestamp}-{UUID}`

```
예시: 20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789
      │                │
      │                └─ UUID (32자 + 하이픈)
      └──────────────── Timestamp (yyyyMMddHHmmssSSS, 17자)

총 길이: 17 + 1 (하이픈) + 36 (UUID) = 54자
```

**검증 정규식**:
```java
private static final Pattern TRACE_ID_PATTERN = Pattern.compile(
    "^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"
);
```

**도메인 규칙**:
- **Timestamp**: `yyyyMMddHHmmssSSS` 형식 (17자, 밀리초 단위)
- **UUID**: UUID v4 (소문자 hex + 하이픈)
- **유일성 보장**: Timestamp(밀리초) + UUID로 충돌 방지
- **형식 유효성**: 정규식으로 검증

### 4️⃣ Port Definitions (In/Out 인터페이스)

**In Port** (UseCase → Application):
```java
package com.ryuqq.connectly.gateway.application.port.in.trace;

import reactor.core.publisher.Mono;

public interface GenerateTraceIdPort {
    Mono<GenerateTraceIdResponse> generateTraceId(GenerateTraceIdCommand command);
}
```

```java
package com.ryuqq.connectly.gateway.application.port.in.trace;

import reactor.core.publisher.Mono;

public interface ValidateTraceIdPort {
    Mono<ValidateTraceIdResponse> validateTraceId(ValidateTraceIdQuery query);
}
```

**DTO 정의**:
```java
package com.ryuqq.connectly.gateway.application.dto.trace;

// Command (Empty - 파라미터 없음)
public record GenerateTraceIdCommand() {}

// Response
public record GenerateTraceIdResponse(
    String traceId  // 생성된 Trace-ID (예: 20250124123456789-a1b2c3d4-...)
) {}

// Query
public record ValidateTraceIdQuery(
    String traceId  // 검증할 Trace-ID
) {}

// Response
public record ValidateTraceIdResponse(
    boolean valid,   // 유효성 여부
    String reason    // 실패 이유 (valid=false일 때)
) {}
```

**Out Port** (Application → Infrastructure):
- **Trace-ID는 Stateless**이므로 Out Port 불필요 (Pass-through 방식)

### 5️⃣ MDC Integration (Logging Context)

**MDC (Mapped Diagnostic Context)**는 Reactive 환경에서 로그에 Trace-ID를 자동 추가하는 메커니즘입니다.

**TraceIdMdcContext**:
```java
package com.ryuqq.connectly.gateway.adapter.in.filter.trace;

import org.slf4j.MDC;

public class TraceIdMdcContext {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * MDC에 Trace-ID 추가
     */
    public static void put(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * MDC에서 Trace-ID 제거
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * MDC에서 Trace-ID 조회
     */
    public static String get() {
        return MDC.get(TRACE_ID_KEY);
    }
}
```

**Logback 설정** (`logback-spring.xml`):
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

**Reactor Context Propagation**:
```java
// TraceIdFilter에서 Reactor Context에 Trace-ID 추가
return chain.filter(exchange)
    .contextWrite(Context.of("traceId", traceId))
    .doOnEach(signal -> {
        // Reactive 체인 내에서 MDC 자동 설정 (Sleuth가 처리)
        if (signal.isOnNext() || signal.isOnError()) {
            TraceIdMdcContext.put(signal.getContextView().get("traceId"));
        }
    });
```

**로그 출력 예시**:
```
2025-01-24 12:34:56.789 [reactor-http-nio-2] INFO  c.r.c.g.filter.TraceIdFilter [traceId=20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789] - Trace-ID generated
2025-01-24 12:34:56.791 [reactor-http-nio-2] INFO  c.r.c.g.filter.JwtAuthFilter [traceId=20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789] - JWT validation started
```

### 6️⃣ Use Case Flow (Trace-ID 생성 및 전달)

```
Client Request
    ↓
┌─────────────────────────────────────────────────────────────────┐
│ TraceIdFilter (Order: HIGHEST_PRECEDENCE)                       │
├─────────────────────────────────────────────────────────────────┤
│ 1. GenerateTraceIdUseCase 호출                                  │
│    └─ Timestamp 생성 (yyyyMMddHHmmssSSS)                        │
│    └─ UUID 생성 (v4)                                            │
│    └─ Trace-ID 조합: {timestamp}-{UUID}                         │
│                                                                 │
│ 2. Request Header 추가 (Downstream 전달)                        │
│    └─ X-Trace-Id: {traceId}                                     │
│                                                                 │
│ 3. Exchange Attribute 저장 (다른 Filter 사용)                   │
│    └─ exchange.getAttributes().put("traceId", traceId)          │
│                                                                 │
│ 4. Reactor Context 추가 (MDC 전파)                              │
│    └─ .contextWrite(Context.of("traceId", traceId))            │
│                                                                 │
│ 5. Response Header 추가 (Client 반환)                           │
│    └─ X-Trace-Id: {traceId}                                     │
└─────────────────────────────────────────────────────────────────┘
    ↓
Downstream Service (Backend)
    ├─ Request Header: X-Trace-Id 포함
    └─ Backend도 동일한 Trace-ID로 로깅
    ↓
Client Response
    └─ Response Header: X-Trace-Id 포함
```

**코드 흐름**:
```java
// 1. TraceIdFilter
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return generateTraceIdPort.generateTraceId(new GenerateTraceIdCommand())
        .flatMap(response -> {
            String traceId = response.traceId();

            // 2. Request Header 추가
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .build();

            // 3. Exchange Attribute 저장
            exchange.getAttributes().put("traceId", traceId);

            // 4. Reactor Context 추가 + Response Header 추가
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(Context.of("traceId", traceId))
                .doFinally(signalType -> {
                    exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
                });
        });
}

// 2. GenerateTraceIdUseCase
@Override
public Mono<GenerateTraceIdResponse> generateTraceId(GenerateTraceIdCommand command) {
    return Mono.fromCallable(() -> {
        // 1. Timestamp 생성
        String timestamp = Timestamp.now().value();

        // 2. UUID 생성
        String uuid = UUID.randomUUID().toString();

        // 3. Trace-ID 조합
        TraceId traceId = new TraceId(timestamp + "-" + uuid);

        return new GenerateTraceIdResponse(traceId.value());
    });
}
```

### 7️⃣ Downstream Service Propagation (Backend 전달)

**WebClient 설정** (Spring Cloud Gateway에서 자동 처리):
```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter((request, next) -> {
                // Spring Cloud Gateway가 자동으로 X-Trace-Id 헤더 전달
                // 추가 설정 불필요 (TraceIdFilter에서 Request Header 추가)
                return next.exchange(request);
            })
            .build();
    }
}
```

**Backend Service에서 Trace-ID 수신**:
```java
// Backend Service (Order Service)
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping
    public Mono<List<OrderResponse>> getOrders(
        @RequestHeader("X-Trace-Id") String traceId  // 👈 Gateway에서 전달
    ) {
        log.info("Trace-ID received: {}", traceId);
        // 동일한 Trace-ID로 로깅
        MDC.put("traceId", traceId);
        return orderService.getOrders();
    }
}
```

### 8️⃣ Error Handling (Error Response with Trace-ID)

**Error Response DTO**:
```java
package com.ryuqq.connectly.gateway.adapter.in.rest.error;

public record TraceIdErrorResponse(
    String errorCode,    // 에러 코드 (예: INVALID_TRACE_ID)
    String message,      // 에러 메시지
    String traceId       // Trace-ID (디버깅용)
) {}
```

**Global Error Handler 통합**:
```java
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Exchange Attribute에서 Trace-ID 추출
        String traceId = exchange.getAttribute("traceId");

        ErrorResponse errorResponse;
        HttpStatus status;

        if (ex instanceof InvalidTraceIdException) {
            errorResponse = new ErrorResponse(
                "INVALID_TRACE_ID",
                "Invalid Trace-ID format",
                traceId  // 👈 Trace-ID 포함
            );
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                ex.getMessage(),
                traceId
            );
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);  // Response Header

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(serializeToJson(errorResponse).getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

**Error Code 정의**:
| Error Code | HTTP Status | 설명 | Response Example |
|------------|-------------|------|------------------|
| `INVALID_TRACE_ID` | 500 | Trace-ID 형식 오류 | `{"errorCode":"INVALID_TRACE_ID","message":"Invalid format","traceId":"..."}` |

### 9️⃣ Spring Cloud Sleuth Integration (자동 MDC 관리)

**Sleuth Dependency** (`build.gradle`):
```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth:3.1.11'
}
```

**Sleuth 설정** (`application.yml`):
```yaml
spring:
  sleuth:
    enabled: true
    reactor:
      instrumentation-type: decorate_on_each  # Reactor Context → MDC 자동 전파
    sampler:
      probability: 1.0  # 모든 요청 추적
    baggage:
      remote-fields:
        - traceId
      correlation-fields:
        - traceId
```

**Sleuth 동작 방식**:
```
TraceIdFilter
    ↓
Reactor Context에 traceId 추가
    └─ .contextWrite(Context.of("traceId", traceId))
         ↓
Sleuth가 자동으로 Reactor Context → MDC 전파
    ├─ onNext() 시 MDC.put("traceId", ...)
    ├─ onError() 시 MDC.put("traceId", ...)
    └─ onComplete() 시 MDC.remove("traceId")
         ↓
모든 로그에 [traceId=...] 자동 포함
```

### 🔟 Core Web Vitals (성능 목표)

**Trace-ID 생성 성능**:
- **목표 지연 시간**: < 1ms (Trace-ID 생성)
- **Throughput**: > 10,000 requests/sec
- **메모리 사용량**: < 10 MB (Heap)

**메트릭** (Micrometer):
```java
@Component
public class TraceIdMetrics {

    private final MeterRegistry meterRegistry;

    public TraceIdMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Trace-ID 생성 시간 (Timer)
        Timer.builder("gateway.trace_id.generation.duration")
            .description("Trace-ID generation duration")
            .register(meterRegistry);

        // Trace-ID 전파 성공 (Counter)
        Counter.builder("gateway.trace_id.propagation.success")
            .description("Trace-ID propagation success count")
            .register(meterRegistry);
    }
}
```

---

## 📝 목적

Trace-ID 기반 분산 추적 기능 구현 (Domain → Application → Persistence → Filter → Integration):
- Gateway 진입 시 Trace-ID 자동 생성
- Downstream 서비스로 X-Trace-Id 헤더 전달
- Client에게 동일한 Trace-ID 반환
- MDC 기반 로그 통합
- Trace-ID 기반 요청 추적 및 디버깅

**이 Feature는 독립적으로 배포 가능한 완전한 기능 단위입니다.**

---

## 🏗️ Infrastructure & Tech Stack

### Core Framework
- [ ] **Spring Cloud Gateway 3.1.x**: Filter Chain 기반 라우팅
- [ ] **Spring WebFlux**: Reactive Non-Blocking I/O
- [ ] **Netty**: 비동기 이벤트 기반 서버
- [ ] **Project Reactor**: Mono/Flux 기반 Reactive Programming

### Reactive Stack
- [ ] **Lettuce**: Reactive Redis Client (미사용 - Stateless 기능)
- [ ] **Redisson**: Distributed Lock (미사용 - Stateless 기능)
- [ ] **WebClient**: Downstream 서비스 연동 시 Trace-ID 전달
  - X-Trace-Id 헤더 자동 전파

### Redis Configuration
- [ ] **Trace-ID는 Stateless이므로 Redis 불필요** (Pass-through)

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing - **핵심 기능**
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - WebClient Trace Header 자동 추가 (X-Trace-Id)
  - 모든 로그에 Trace-ID 자동 포함
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.trace_id.generation.duration (Timer)
  - gateway.trace_id.propagation.success (Counter)
- [ ] **Logback JSON**: Structured Logging - **핵심 기능**
  - CloudWatch Logs 연동
  - MDC: traceId, userId, tenantId
  - 로그 패턴: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n`

### Testing Infrastructure
- [ ] **Testcontainers**: Redis 불필요 (Stateless)
- [ ] **WireMock**: Backend Service Mock (Trace-ID 전달 검증)
- [ ] **WebTestClient**: Reactive 통합 테스트 (TestRestTemplate 대체)
- [ ] **StepVerifier**: Reactor 테스트 (Mono/Flux 검증)

### Deployment (AWS ECS Fargate)
- [ ] **Dockerfile**: Multi-stage Build
  - Base Image: eclipse-temurin:21-jre-alpine
  - Layered JAR (Spring Boot 2.3+)
- [ ] **ECS Task Definition**:
  - CPU: 1 vCPU (1024)
  - Memory: 2 GB (2048)
  - 환경변수: SLEUTH_ENABLED=true
- [ ] **ECS Service**:
  - Auto Scaling (Target: CPU 70%, Min: 2, Max: 10)
  - Application Load Balancer (Health Check: /actuator/health)
- [ ] **AWS Secrets Manager**: 불필요 (Stateless 기능)

### Configuration Management
- [ ] **application.yml**: 기본 설정 (로컬 개발용)
  - Sleuth 활성화
  - MDC 패턴 설정
- [ ] **환경변수 (ECS Task Definition)**:
  - `SLEUTH_ENABLED`: true (Distributed Tracing 활성화)

---

## 🎯 요구사항

### 📦 Domain Layer

#### Value Objects
- [ ] **TraceId**: Trace-ID VO
  - value (String, Trace-ID 문자열)
  - 검증: 형식 `{timestamp}-{UUID}`
  - 검증: 최소 길이 40자 이상

- [ ] **Timestamp**: Timestamp VO
  - value (String, yyyyMMddHHmmssSSS 형식)
  - 검증: 형식 유효성

#### 도메인 비즈니스 규칙
- [ ] **Trace-ID 생성 규칙**: `{timestamp}-{UUID}` 형식 (예: `20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789`)
- [ ] **Trace-ID 유일성**: 타임스탬프(밀리초) + UUID로 충돌 방지

#### 도메인 예외
- [ ] **InvalidTraceIdException**: Trace-ID 형식 오류 (500 Internal Server Error)

---

### 🔧 Application Layer

#### Use Case
- [ ] **GenerateTraceIdUseCase** (Command)
  - **Input**: GenerateTraceIdCommand (Empty - 파라미터 없음)
  - **Output**: GenerateTraceIdResponse
    - traceId (String, 생성된 Trace-ID)
  - **Transaction**: 불필요 (Stateless 연산)
  - **비즈니스 로직**:
    1. 현재 시각 추출 (yyyyMMddHHmmssSSS)
    2. UUID 생성
    3. Trace-ID 조합 (`{timestamp}-{UUID}`)

- [ ] **ValidateTraceIdUseCase** (Query)
  - **Input**: ValidateTraceIdQuery
    - traceId (String, 검증할 Trace-ID)
  - **Output**: ValidateTraceIdResponse
    - valid (boolean, 유효성 여부)
    - reason (String, 실패 이유)
  - **Transaction**: 불필요 (읽기 전용)

#### Port 정의 (In)
- [ ] **GenerateTraceIdPort** (In)
  - generateTraceId(GenerateTraceIdCommand): GenerateTraceIdResponse

- [ ] **ValidateTraceIdPort** (In)
  - validateTraceId(ValidateTraceIdQuery): ValidateTraceIdResponse

#### DTO
- [ ] **GenerateTraceIdCommand** (Record) - Empty Record
- [ ] **GenerateTraceIdResponse** (Record)
- [ ] **ValidateTraceIdQuery** (Record)
- [ ] **ValidateTraceIdResponse** (Record)

#### Assembler
- [ ] **TraceIdAssembler**
  - toTraceId(GenerateTraceIdResponse): TraceId

---

### 💾 Persistence Layer

**참고**: Trace-ID는 Stateless이므로 Persistence Layer 불필요 (Pass-through)

---

### 🌐 Gateway Filter Layer

#### Global Filter
- [ ] **TraceIdFilter** (GlobalFilter)
  - **Order**: `Ordered.HIGHEST_PRECEDENCE` (첫 번째 Filter)
  - **로직**:
    1. GenerateTraceIdUseCase 호출 (Trace-ID 생성)
    2. Request Header에 `X-Trace-Id` 추가 (Downstream으로 전달)
    3. Response Header에 `X-Trace-Id` 추가 (Client로 반환)
    4. MDC에 `traceId` 추가 (로깅용)
    5. Exchange Attribute에 `traceId` 저장 (다른 Filter에서 사용)
  - **예외 처리**: 없음 (Trace-ID 생성 실패 시에도 통과)

#### MDC Integration
- [ ] **TraceIdMdcContext**
  - put(String traceId): void
    - MDC에 `traceId` 추가
  - clear(): void
    - MDC에서 `traceId` 삭제

#### Logback Configuration
- [ ] **logback-spring.xml** (로그 패턴 설정)
  - 패턴: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n`

#### Error Response
- [ ] **TraceIdErrorResponse** (Record)
  - errorCode (String, 에러 코드)
  - message (String, 에러 메시지)
  - traceId (String, Trace-ID)

#### Error Handling
- [ ] **TraceIdErrorHandler** (ErrorWebExceptionHandler 일부)
  - InvalidTraceIdException → `{ "errorCode": "INVALID_TRACE_ID", "message": "Invalid Trace-ID format" }`

---

### ✅ Integration Test

#### E2E 시나리오
- [ ] **Scenario 1: Trace-ID 자동 생성 및 전달**
  - Given: Gateway 진입 요청 (Trace-ID 없음)
  - When: `GET /api/v1/orders` 요청
  - Then: Response Header에 `X-Trace-Id` 포함, Backend Service 요청에도 동일한 `X-Trace-Id` 포함
  - 검증: TraceIdFilter에서 생성, Downstream 전달 확인

- [ ] **Scenario 2: Trace-ID 형식 검증**
  - Given: 생성된 Trace-ID
  - When: Trace-ID 형식 확인
  - Then: `{timestamp}-{UUID}` 형식 준수 (예: `20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789`)
  - 검증: ValidateTraceIdUseCase 호출 → valid = true

- [ ] **Scenario 3: MDC에 Trace-ID 추가 (로깅)**
  - Given: Trace-ID 생성됨
  - When: Gateway 내부 로직 실행
  - Then: 로그에 `[traceId={traceId}]` 포함
  - 검증: MDC.get("traceId") == 생성된 Trace-ID

- [ ] **Scenario 4: Client에게 Trace-ID 반환**
  - Given: Gateway 진입 요청
  - When: `GET /api/v1/orders` 요청
  - Then: Response Header에 `X-Trace-Id` 포함
  - 검증: Client가 Trace-ID 수신 확인

- [ ] **Scenario 5: Trace-ID 유일성 보장**
  - Given: 동시에 1,000개 요청
  - When: 각 요청마다 Trace-ID 생성
  - Then: 모든 Trace-ID 서로 다름 (충돌 없음)
  - 검증: Set<String> 크기 == 1,000

- [ ] **Scenario 6: Trace-ID 기반 요청 추적 (End-to-End)**
  - Given: Client → Gateway → Order Service → Product Service
  - When: Client 요청
  - Then: 모든 서비스 로그에 동일한 Trace-ID 포함
  - 검증: Gateway, Order, Product 로그 Trace-ID 일치

#### Testcontainers
- [ ] **Backend Service Mock**: WireMock 사용
  - `/api/v1/orders` 엔드포인트 Mock
  - Request Header `X-Trace-Id` 검증

#### TestFixture
- [ ] **TraceIdTestFixture**: 테스트용 Trace-ID 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] Lombok 금지
- [ ] Trace-ID 생성 로직 도메인에 위치

#### Application Layer
- [ ] Transaction 불필요 (Stateless)
- [ ] Port 의존성 역전

#### Persistence Layer
- [ ] Persistence Layer 불필요 (Stateless)

#### Gateway Filter Layer
- [ ] Filter Order: TraceIdFilter `HIGHEST_PRECEDENCE` (최우선 실행)
- [ ] MDC 필수: 모든 로그에 Trace-ID 포함

#### Reactive 규칙 (추가)
- [ ] **Blocking Call 절대 금지**
  - JDBC (JPA Repository) 사용 금지
  - RestTemplate 사용 금지 → WebClient 필수
  - Thread.sleep() 금지
  - Mono.block(), Flux.blockFirst(), Flux.blockLast() 금지 (테스트 제외)
- [ ] **Reactive Repository 필수**
  - Trace-ID는 Stateless이므로 Repository 불필요
- [ ] **Reactor Context 사용**
  - Trace-ID 전파용 (ThreadLocal 대신)
  - MDC는 Sleuth가 자동 관리
- [ ] **Error Handling**
  - onErrorResume(), onErrorReturn() 사용
  - Exception을 Mono.error()로 변환

#### Integration Test
- [ ] **WebTestClient 사용** (TestRestTemplate 대체 - Reactive 표준)
- [ ] **MockMvc 금지**
- [ ] **WireMock 사용** (Backend Service Mock)
- [ ] **StepVerifier 사용** (Reactor 테스트)

---

## ✅ 완료 조건

- [ ] Domain Layer 구현 완료 (VO 2개, Exception 1개)
- [ ] Application Layer 구현 완료 (UseCase 2개, Port 2개, DTO 4개, Assembler 1개)
- [ ] Persistence Layer 구현 완료 (없음 - Stateless)
- [ ] Gateway Filter Layer 구현 완료 (Filter 1개, MDC 1개, ErrorHandler 1개)
- [ ] Integration Test 구현 완료 (E2E Scenario 6개, TestFixture 1개)
- [ ] 모든 테스트 통과 (Unit + Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90%
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: docs/prd/access-gateway.md (Trace-ID 섹션)
- **Plan**: docs/prd/plans/GATEWAY-006-trace-id-plan.md (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)

---

## 📚 참고 자료

### 코딩 규칙
- Domain: docs/coding_convention/02-domain-layer/
- Application: docs/coding_convention/03-application-layer/

### PRD 섹션
- Trace-ID 생성 및 전달 (Line 1594-1644)

---

## 🔍 Trace-ID 형식 예시

```
20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789
│                │
│                └─ UUID (32자 + 하이픈)
└──────────────── Timestamp (yyyyMMddHHmmssSSS, 17자)

총 길이: 17 + 1 (하이픈) + 36 (UUID) = 54자
```

---

## 🧪 Integration Test 예시

### Scenario 1: Trace-ID 자동 생성 및 전달

```java
@Test
void traceId_자동_생성_및_전달() {
    // Given: Gateway 진입 요청
    // When: GET /api/v1/orders
    ResponseEntity<String> response = testRestTemplate.exchange(
        "/api/v1/orders",
        HttpMethod.GET,
        null,
        String.class
    );

    // Then: Response Header에 X-Trace-Id 포함
    String traceId = response.getHeaders().getFirst("X-Trace-Id");
    assertThat(traceId).isNotNull();
    assertThat(traceId).matches("^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    // Downstream Service 요청에도 동일한 Trace-ID 포함
    wireMockServer.verify(
        getRequestedFor(urlEqualTo("/api/v1/orders"))
            .withHeader("X-Trace-Id", equalTo(traceId))
    );
}
```

### Scenario 6: End-to-End Trace-ID 추적

```java
@Test
void traceId_EndToEnd_추적() {
    // Given: Client → Gateway → Order Service → Product Service
    String traceId = captureTraceId();

    // Then: 모든 서비스 로그에 동일한 Trace-ID 포함
    assertThat(gatewayLogs).contains("[traceId=" + traceId + "]");
    assertThat(orderServiceLogs).contains("[traceId=" + traceId + "]");
    assertThat(productServiceLogs).contains("[traceId=" + traceId + "]");
}
```
