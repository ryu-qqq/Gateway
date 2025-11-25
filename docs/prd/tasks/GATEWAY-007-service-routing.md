# GATEWAY-007: Service Routing Configuration (전체 스택)

**Epic**: AccessGateway (Zero-Trust API Gateway)
**Feature**: Backend Service 라우팅 설정
**브랜치**: feature/GATEWAY-007-service-routing
**Jira URL**: https://ryuqqq.atlassian.net/browse/GAT-8

---

## 🚀 Quick Reference (개발 시 필수 참조)

이 섹션은 **GATEWAY-007 Service Routing 기능 개발 시 필요한 모든 정보를 한 곳에 모아둔 통합 참조 가이드**입니다. 다른 문서를 참조하지 않고도 이 섹션만으로 개발을 완료할 수 있습니다.

### 1️⃣ Routing Rules (Backend Service 라우팅 규칙)

Gateway는 **Path 기반 라우팅**을 사용하여 Backend Service로 요청을 전달합니다.

```
Client Request → Gateway → Backend Service
    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│ Path Matching (Spring Cloud Gateway RouteLocator)                      │
├─────────────────────────────────────────────────────────────────────────┤
│ /api/v1/orders/**    → Order Service (http://order-service:8080)       │
│ /api/v1/products/**  → Product Service (http://product-service:8080)   │
│ /api/v1/users/**     → User Service (http://user-service:8080)         │
│ /api/v1/payments/**  → Payment Service (http://payment-service:8080)   │
│ /api/v1/inventory/** → Inventory Service (http://inventory-service:8080)│
└─────────────────────────────────────────────────────────────────────────┘
```

**RouteLocator 설정** (`application.yml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        # Order Service
        - id: order-service
          uri: http://order-service:8080
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: orderServiceCircuitBreaker
                fallbackUri: forward:/fallback/order-service

        # Product Service
        - id: product-service
          uri: http://product-service:8080
          predicates:
            - Path=/api/v1/products/**
          filters:
            - name: CircuitBreaker
              args:
                name: productServiceCircuitBreaker
                fallbackUri: forward:/fallback/product-service

        # User Service
        - id: user-service
          uri: http://user-service:8080
          predicates:
            - Path=/api/v1/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user-service

        # Payment Service
        - id: payment-service
          uri: http://payment-service:8080
          predicates:
            - Path=/api/v1/payments/**
          filters:
            - name: CircuitBreaker
              args:
                name: paymentServiceCircuitBreaker
                fallbackUri: forward:/fallback/payment-service

        # Inventory Service
        - id: inventory-service
          uri: http://inventory-service:8080
          predicates:
            - Path=/api/v1/inventory/**
          filters:
            - name: CircuitBreaker
              args:
                name: inventoryServiceCircuitBreaker
                fallbackUri: forward:/fallback/inventory-service
```

**코드 기반 RouteLocator**:
```java
package com.ryuqq.connectly.gateway.adapter.in.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Order Service
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f.circuitBreaker(config -> config
                    .setName("orderServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/order-service")))
                .uri("http://order-service:8080"))

            // Product Service
            .route("product-service", r -> r
                .path("/api/v1/products/**")
                .filters(f -> f.circuitBreaker(config -> config
                    .setName("productServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/product-service")))
                .uri("http://product-service:8080"))

            // User Service
            .route("user-service", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f.circuitBreaker(config -> config
                    .setName("userServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/user-service")))
                .uri("http://user-service:8080"))

            // Payment Service
            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f.circuitBreaker(config -> config
                    .setName("paymentServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/payment-service")))
                .uri("http://payment-service:8080"))

            // Inventory Service
            .route("inventory-service", r -> r
                .path("/api/v1/inventory/**")
                .filters(f -> f.circuitBreaker(config -> config
                    .setName("inventoryServiceCircuitBreaker")
                    .setFallbackUri("forward:/fallback/inventory-service")))
                .uri("http://inventory-service:8080"))

            .build();
    }
}
```

### 2️⃣ Circuit Breaker Configuration (Resilience4j)

**Circuit Breaker**는 Backend Service 장애 시 빠른 실패(Fail Fast)를 보장합니다.

**Circuit Breaker 상태 전환**:
```
CLOSED (정상) → OPEN (장애) → HALF_OPEN (복구 중) → CLOSED (정상)
    ↓              ↓                ↓                  ↓
정상 응답      실패율 > 50%      테스트 요청       성공률 > 50%
               (10초 대기)       (5개 요청)        (CLOSED 복귀)
```

**Resilience4j 설정** (`application.yml`):
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        # Circuit Breaker 열림 임계값 (실패율 50% 이상)
        failureRateThreshold: 50
        # 최소 요청 수 (10개 요청 이상부터 실패율 계산)
        minimumNumberOfCalls: 10
        # OPEN → HALF_OPEN 전환 대기 시간 (10초)
        waitDurationInOpenState: 10s
        # HALF_OPEN 상태에서 허용할 요청 수 (5개)
        permittedNumberOfCallsInHalfOpenState: 5
        # Sliding Window 크기 (100개 요청)
        slidingWindowSize: 100
        # Sliding Window 타입 (COUNT_BASED)
        slidingWindowType: COUNT_BASED
        # Slow Call 임계값 (3초 이상 응답)
        slowCallDurationThreshold: 3s
        # Slow Call 비율 임계값 (50% 이상)
        slowCallRateThreshold: 50
        # 자동 HALF_OPEN 전환 (true)
        automaticTransitionFromOpenToHalfOpenEnabled: true

    instances:
      # Order Service Circuit Breaker
      orderServiceCircuitBreaker:
        baseConfig: default
        failureRateThreshold: 60
        waitDurationInOpenState: 15s

      # Product Service Circuit Breaker
      productServiceCircuitBreaker:
        baseConfig: default

      # User Service Circuit Breaker
      userServiceCircuitBreaker:
        baseConfig: default

      # Payment Service Circuit Breaker (더 엄격한 설정)
      paymentServiceCircuitBreaker:
        baseConfig: default
        failureRateThreshold: 30
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 20s

      # Inventory Service Circuit Breaker
      inventoryServiceCircuitBreaker:
        baseConfig: default

  # Retry 설정
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2

  # Timeout 설정
  timelimiter:
    configs:
      default:
        timeoutDuration: 5s
```

**Circuit Breaker Metrics**:
```java
@Component
public class CircuitBreakerMetrics {

    private final MeterRegistry meterRegistry;

    public CircuitBreakerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Circuit Breaker 상태 (Gauge)
        Gauge.builder("gateway.circuit_breaker.state", () -> getCircuitBreakerState())
            .description("Circuit Breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
            .register(meterRegistry);

        // Fallback 호출 수 (Counter)
        Counter.builder("gateway.circuit_breaker.fallback.count")
            .description("Circuit Breaker fallback invocation count")
            .register(meterRegistry);
    }
}
```

### 3️⃣ Fallback Controller (Circuit Breaker Fallback)

**Fallback Controller**는 Circuit Breaker OPEN 상태에서 실행되는 대체 응답입니다.

```java
package com.ryuqq.connectly.gateway.adapter.in.rest.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Order Service Fallback
     */
    @GetMapping(value = "/order-service", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/order-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<FallbackResponse>> orderServiceFallback() {
        FallbackResponse response = new FallbackResponse(
            "SERVICE_UNAVAILABLE",
            "Order Service is temporarily unavailable. Please try again later.",
            "order-service"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Product Service Fallback
     */
    @GetMapping(value = "/product-service", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/product-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<FallbackResponse>> productServiceFallback() {
        FallbackResponse response = new FallbackResponse(
            "SERVICE_UNAVAILABLE",
            "Product Service is temporarily unavailable. Please try again later.",
            "product-service"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * User Service Fallback
     */
    @GetMapping(value = "/user-service", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/user-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<FallbackResponse>> userServiceFallback() {
        FallbackResponse response = new FallbackResponse(
            "SERVICE_UNAVAILABLE",
            "User Service is temporarily unavailable. Please try again later.",
            "user-service"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Payment Service Fallback
     */
    @GetMapping(value = "/payment-service", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/payment-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<FallbackResponse>> paymentServiceFallback() {
        FallbackResponse response = new FallbackResponse(
            "SERVICE_UNAVAILABLE",
            "Payment Service is temporarily unavailable. Please try again later.",
            "payment-service"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    /**
     * Inventory Service Fallback
     */
    @GetMapping(value = "/inventory-service", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/inventory-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<FallbackResponse>> inventoryServiceFallback() {
        FallbackResponse response = new FallbackResponse(
            "SERVICE_UNAVAILABLE",
            "Inventory Service is temporarily unavailable. Please try again later.",
            "inventory-service"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
```

**Fallback Response DTO**:
```java
package com.ryuqq.connectly.gateway.adapter.in.rest.fallback;

public record FallbackResponse(
    String errorCode,       // 에러 코드 (SERVICE_UNAVAILABLE)
    String message,         // 에러 메시지
    String serviceName      // 장애 발생 서비스 이름
) {}
```

### 4️⃣ Load Balancer Configuration (Service Discovery)

**Service Discovery** (Eureka 또는 Kubernetes Service):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          # Eureka Service Discovery
          uri: lb://order-service  # 👈 lb:// 프리픽스 (Load Balancer)
          predicates:
            - Path=/api/v1/orders/**

# Eureka Client 설정
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka-server:8761/eureka/
  instance:
    preferIpAddress: true
```

**Kubernetes Service Discovery** (대안):
```yaml
# Kubernetes Service
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080

# Gateway에서 Kubernetes Service DNS 사용
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://order-service.default.svc.cluster.local:8080
          predicates:
            - Path=/api/v1/orders/**
```

### 5️⃣ Request/Response Transformation (Header 추가)

**Request Header Transformation** (Backend에 X-User-Id, X-Tenant-Id 전달):
```java
// TenantIsolationFilter에서 이미 처리됨 (GATEWAY-004)
ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", userId)
    .header("X-Tenant-Id", tenantId)
    .header("X-Permissions", serializeToJson(permissions))
    .header("X-Roles", serializeToJson(roles))
    .header("X-Trace-Id", traceId)  // TraceIdFilter에서 추가됨
    .build();
```

**Response Header Transformation** (CORS, Cache-Control 등):
```java
@Configuration
public class ResponseHeaderConfig {

    @Bean
    public GlobalFilter responseHeaderFilter() {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // CORS Headers
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");

            // Cache-Control
            exchange.getResponse().getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
        }));
    }
}
```

### 6️⃣ Health Check & Actuator Endpoints

**Actuator 설정** (`application.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, circuitbreakers, gateway
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

**Health Check Endpoint**:
```bash
# Gateway Health Check
curl http://localhost:8080/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "orderServiceCircuitBreaker": "CLOSED",
        "productServiceCircuitBreaker": "CLOSED",
        "paymentServiceCircuitBreaker": "OPEN"  // 장애 상태
      }
    },
    "gateway": {
      "status": "UP"
    }
  }
}
```

**Circuit Breaker 상태 조회**:
```bash
curl http://localhost:8080/actuator/circuitbreakers

# Response:
{
  "circuitBreakers": {
    "orderServiceCircuitBreaker": {
      "state": "CLOSED",
      "failureRate": 12.5,
      "slowCallRate": 0.0,
      "bufferedCalls": 8,
      "failedCalls": 1
    },
    "paymentServiceCircuitBreaker": {
      "state": "OPEN",
      "failureRate": 75.0,
      "slowCallRate": 25.0,
      "bufferedCalls": 20,
      "failedCalls": 15
    }
  }
}
```

### 7️⃣ Routing Table (Service URL Mapping)

| Service Name | Path Pattern | Backend URL | Circuit Breaker | Fallback |
|--------------|--------------|-------------|-----------------|----------|
| **Order Service** | `/api/v1/orders/**` | `http://order-service:8080` | `orderServiceCircuitBreaker` | `/fallback/order-service` |
| **Product Service** | `/api/v1/products/**` | `http://product-service:8080` | `productServiceCircuitBreaker` | `/fallback/product-service` |
| **User Service** | `/api/v1/users/**` | `http://user-service:8080` | `userServiceCircuitBreaker` | `/fallback/user-service` |
| **Payment Service** | `/api/v1/payments/**` | `http://payment-service:8080` | `paymentServiceCircuitBreaker` | `/fallback/payment-service` |
| **Inventory Service** | `/api/v1/inventory/**` | `http://inventory-service:8080` | `inventoryServiceCircuitBreaker` | `/fallback/inventory-service` |

### 8️⃣ Error Handling (Routing Errors)

**Routing Error Codes**:
| Error Code | HTTP Status | 설명 | Cause |
|------------|-------------|------|-------|
| `SERVICE_UNAVAILABLE` | 503 | Backend Service 장애 | Circuit Breaker OPEN, Timeout |
| `GATEWAY_TIMEOUT` | 504 | Backend Service 응답 지연 | Timeout > 5초 |
| `BAD_GATEWAY` | 502 | Backend Service 연결 실패 | Network Error, DNS 실패 |

**Global Error Handler** (Routing Error 통합):
```java
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String traceId = exchange.getAttribute("traceId");

        ErrorResponse errorResponse;
        HttpStatus status;

        if (ex instanceof TimeoutException) {
            errorResponse = new ErrorResponse(
                "GATEWAY_TIMEOUT",
                "Backend service timeout",
                traceId
            );
            status = HttpStatus.GATEWAY_TIMEOUT;
        } else if (ex instanceof ConnectException) {
            errorResponse = new ErrorResponse(
                "BAD_GATEWAY",
                "Backend service connection failed",
                traceId
            );
            status = HttpStatus.BAD_GATEWAY;
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

        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(serializeToJson(errorResponse).getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
```

### 9️⃣ Integration Test (E2E Routing Test)

**E2E Routing Test**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ServiceRoutingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void orderService_라우팅_성공() {
        // Given: Order Service Mock 서버 실행 (WireMock)
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"orders\":[]}")));

        // When: Gateway를 통한 Order Service 호출
        webTestClient.get()
            .uri("/api/v1/orders")
            .header("Authorization", "Bearer " + validJwt)
            .exchange()

            // Then: 200 OK 응답
            .expectStatus().isOk()
            .expectHeader().exists("X-Trace-Id")
            .expectBody()
            .jsonPath("$.orders").isArray();

        // Verify: Backend Service 호출 확인
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/orders"))
            .withHeader("X-User-Id", equalTo("user123"))
            .withHeader("X-Tenant-Id", equalTo("tenant-a"))
            .withHeader("X-Trace-Id", matching("^\\d{17}-[a-f0-9-]{36}$")));
    }

    @Test
    void circuitBreaker_OPEN_상태에서_Fallback_호출() {
        // Given: Backend Service 장애 (Circuit Breaker OPEN)
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse().withStatus(500).withFixedDelay(10000)));

        // When: 10회 연속 요청 (Circuit Breaker 열림)
        for (int i = 0; i < 10; i++) {
            webTestClient.get().uri("/api/v1/orders").exchange();
        }

        // Then: Fallback 응답 반환
        webTestClient.get()
            .uri("/api/v1/orders")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.errorCode").isEqualTo("SERVICE_UNAVAILABLE")
            .jsonPath("$.serviceName").isEqualTo("order-service");
    }
}
```

### 🔟 Performance Metrics (Core Web Vitals)

**Routing 성능 목표**:
- **Latency**: < 50ms (Gateway Overhead)
- **Throughput**: > 5,000 requests/sec
- **Circuit Breaker**: 10초 내 복구

**Micrometer Metrics**:
```java
@Component
public class RoutingMetrics {

    private final MeterRegistry meterRegistry;

    public RoutingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Gateway Latency (Timer)
        Timer.builder("gateway.routing.latency")
            .description("Gateway routing latency")
            .register(meterRegistry);

        // Backend Service Call (Counter)
        Counter.builder("gateway.backend.calls")
            .description("Backend service call count")
            .tag("service", "order-service")
            .register(meterRegistry);
    }
}
```

---

## 📝 목적

Spring Cloud Gateway 라우팅 규칙 정의 및 Backend Service 연동 설정:
- Path 기반 라우팅 규칙 정의
- Backend Service별 URI 매핑
- Circuit Breaker 및 Fallback 설정
- Load Balancing 전략 설정
- Health Check 및 Service Discovery 연동

**이 Feature는 독립적으로 배포 가능한 완전한 기능 단위입니다.**

---

## 🏗️ Infrastructure & Tech Stack

### Core Framework
- [ ] **Spring Cloud Gateway 3.1.x**: Filter Chain 기반 라우팅
- [ ] **Spring WebFlux**: Reactive Non-Blocking I/O
- [ ] **Netty**: 비동기 이벤트 기반 서버
- [ ] **Project Reactor**: Mono/Flux 기반 Reactive Programming

### Reactive Stack
- [ ] **Lettuce**: Reactive Redis Client (미사용 - 설정 기반 기능)
- [ ] **Redisson**: Distributed Lock (미사용 - 설정 기반 기능)
- [ ] **WebClient**: Backend Service 연동 (라우팅된 요청 전달)
  - Connection Timeout: 3초
  - Response Timeout: 10초 (Backend 처리 시간 고려)
  - Circuit Breaker: Resilience4j
  - Retry: Exponential Backoff (최대 3회)

### Service Discovery (선택적)
- [ ] **Eureka Client** (선택 1): Service Discovery 사용 시
  - `uri: lb://service-name` 형식
- [ ] **Consul Client** (선택 2): Consul 사용 시
- [ ] **Static URI** (선택 3): 직접 URL 지정
  - `uri: http://backend-service:8080`

### Redis Configuration
- [ ] **라우팅은 Stateless이므로 Redis 불필요** (설정 기반)

### Observability
- [ ] **Spring Cloud Sleuth 3.1.x**: Distributed Tracing
  - MDC 자동 추가 (traceId, spanId, userId, tenantId)
  - Reactor Context Propagation
  - Backend Service로 Trace Header 자동 전파
- [ ] **Micrometer + Prometheus**: Metrics
  - gateway.route.request.duration (Timer, tags: routeId)
  - gateway.route.request.count (Counter, tags: routeId, status)
  - gateway.route.circuit_breaker.open (Counter, tags: routeId)
  - gateway.route.fallback.invoked (Counter, tags: routeId)
- [ ] **Logback JSON**: Structured Logging
  - CloudWatch Logs 연동
  - MDC: traceId, routeId, backendUri

### Testing Infrastructure
- [ ] **Testcontainers**: Backend Service Mock (미사용 - WireMock 사용)
- [ ] **WireMock**: Backend Service Mock Server
  - 각 Backend Service 엔드포인트 Mock
- [ ] **WebTestClient**: Reactive 통합 테스트 (TestRestTemplate 대체)
- [ ] **StepVerifier**: Reactor 테스트 (Mono/Flux 검증)

### Deployment (AWS ECS Fargate)
- [ ] **Dockerfile**: Multi-stage Build
  - Base Image: eclipse-temurin:21-jre-alpine
  - Layered JAR (Spring Boot 2.3+)
- [ ] **ECS Task Definition**:
  - CPU: 1 vCPU (1024)
  - Memory: 2 GB (2048)
  - 환경변수: BACKEND_SERVICE_URLS (쉼표 구분)
- [ ] **ECS Service**:
  - Auto Scaling (Target: CPU 70%, Min: 2, Max: 10)
  - Application Load Balancer (Health Check: /actuator/health)
- [ ] **AWS Secrets Manager**: 불필요 (라우팅 설정만)

### Configuration Management
- [ ] **application.yml**: 라우팅 규칙 정의 (핵심)
  - routes 설정 (Path, URI, Filters)
  - Circuit Breaker 설정
  - Retry 설정
- [ ] **환경변수 (ECS Task Definition)**:
  - `BACKEND_SERVICE_URLS`: Backend Service URL 목록 (선택적)
  - Service Discovery 미사용 시 환경변수로 URI 주입

---

## 🎯 요구사항

### 📦 Domain Layer

**참고**: 라우팅은 설정 기반 기능이므로 Domain Layer 불필요 (Pass-through)

---

### 🔧 Application Layer

**참고**: 라우팅은 Spring Cloud Gateway가 자동 처리하므로 Application Layer 불필요

---

### 💾 Persistence Layer

**참고**: 라우팅은 Stateless이므로 Persistence Layer 불필요

---

### 🌐 Gateway Configuration Layer

#### Route Configuration (application.yml)

**기본 구조**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        # TODO: Backend Service별 라우팅 규칙 정의
        # 예시:
        # - id: service-name
        #   uri: lb://service-name (Service Discovery 사용)
        #   uri: http://backend-service:8080 (직접 URL 지정)
        #   predicates:
        #     - Path=/api/v1/resource/**
        #   filters:
        #     - StripPrefix=0
        #     - name: CircuitBreaker
        #       args:
        #         name: serviceCircuitBreaker
        #         fallbackUri: forward:/fallback/service

      default-filters:
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback

resilience4j:
  circuitbreaker:
    instances:
      defaultCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
```

#### Route 정의 항목 (각 Backend Service별)

- [ ] **Route ID**: 고유 식별자 (예: `order-service`, `product-service`)
- [ ] **URI**: Backend Service 주소
  - Service Discovery 사용: `lb://service-name`
  - 직접 URL: `http://backend-service:8080`
- [ ] **Predicates**: 라우팅 조건
  - Path: `/api/v1/resource/**`
  - Method: `GET`, `POST`, `PUT`, `DELETE`
  - Header: 특정 헤더 존재 여부
- [ ] **Filters**: 요청/응답 변환
  - StripPrefix: Path Prefix 제거
  - AddRequestHeader: 헤더 추가
  - CircuitBreaker: Circuit Breaker 적용
  - Retry: 재시도 정책

#### Circuit Breaker Configuration

- [ ] **Circuit Breaker 이름**: 각 서비스별 고유 이름
- [ ] **slidingWindowSize**: 슬라이딩 윈도우 크기 (기본 10)
- [ ] **failureRateThreshold**: 실패율 임계값 (기본 50%)
- [ ] **waitDurationInOpenState**: Open 상태 대기 시간 (기본 10초)
- [ ] **fallbackUri**: Fallback 엔드포인트 (예: `forward:/fallback/service`)

#### Fallback Controller

- [ ] **FallbackController** (RestController)
  - **Endpoint**: `GET /fallback`
  - **Endpoint**: `GET /fallback/{serviceName}`
  - **목적**: Circuit Breaker Open 시 Fallback 응답 제공
  - **로직**:
    1. Service Unavailable 메시지 반환
    2. Retry-After 헤더 추가
    3. Audit Log 기록 (Circuit Breaker Open 이벤트)

**Fallback Response 예시**:
```json
{
  "errorCode": "SERVICE_UNAVAILABLE",
  "message": "Backend service is temporarily unavailable",
  "serviceName": "order-service",
  "retryAfter": 10,
  "timestamp": "2025-01-24T12:34:56Z"
}
```

---

### ✅ Integration Test

#### E2E 시나리오

- [ ] **Scenario 1: Path 기반 라우팅 성공**
  - Given: 라우팅 규칙 정의됨 (`/api/v1/orders/** → order-service`)
  - When: `GET /api/v1/orders` 요청
  - Then: Order Service로 라우팅됨, 200 OK
  - 검증: WireMock에서 Order Service 요청 수신 확인

- [ ] **Scenario 2: 여러 Backend Service 라우팅**
  - Given: 3개 서비스 라우팅 규칙 정의 (Order, Product, User)
  - When: 각 서비스로 요청 전송
  - Then: 각각 올바른 Backend Service로 라우팅됨
  - 검증: WireMock에서 각 서비스 요청 수신 확인

- [ ] **Scenario 3: Circuit Breaker Open → Fallback 호출**
  - Given: Backend Service가 10회 연속 실패 (failureRateThreshold 초과)
  - When: 11번째 요청 전송
  - Then: Circuit Breaker Open, Fallback 엔드포인트 호출, 503 Service Unavailable
  - 검증: Fallback Response 수신, Metrics에 circuit_breaker.open 기록

- [ ] **Scenario 4: Circuit Breaker Half-Open → 복구**
  - Given: Circuit Breaker Open 상태
  - When: waitDurationInOpenState 경과 후 요청, Backend Service 정상 응답
  - Then: Circuit Breaker Closed로 전환, 정상 라우팅
  - 검증: Metrics에 circuit_breaker.closed 기록

- [ ] **Scenario 5: Retry 정책 적용**
  - Given: Backend Service가 일시적 오류 (503) 반환
  - When: 요청 전송
  - Then: 최대 3회 재시도 후 성공 또는 실패
  - 검증: WireMock에서 재시도 횟수 확인

- [ ] **Scenario 6: Backend Service 응답 지연 → Timeout**
  - Given: Backend Service 응답 시간 10초 초과
  - When: 요청 전송
  - Then: Timeout 발생, 504 Gateway Timeout
  - 검증: Response Timeout 설정 준수

- [ ] **Scenario 7: Path Parameter 전달**
  - Given: 라우팅 규칙 `/api/v1/orders/{orderId}/**`
  - When: `GET /api/v1/orders/123/items` 요청
  - Then: Backend Service로 전체 Path 전달됨
  - 검증: WireMock에서 `/api/v1/orders/123/items` 수신 확인

- [ ] **Scenario 8: Load Balancing (Service Discovery 사용 시)**
  - Given: 동일 Service ID로 2개 인스턴스 등록 (Service Discovery)
  - When: 10회 요청 전송
  - Then: 2개 인스턴스에 고르게 분산됨 (Round Robin)
  - 검증: 각 인스턴스 요청 수 거의 동일

#### Testcontainers

- [ ] **WireMock Testcontainers**: Backend Service Mock
  - 각 Backend Service 엔드포인트 Mock
  - 성공/실패/지연 시나리오 설정

#### TestFixture

- [ ] **RouteTestFixture**: 테스트용 라우팅 설정 생성

---

## ⚠️ 제약사항

### Zero-Tolerance 규칙

#### Domain Layer
- [ ] 라우팅은 설정 기반이므로 Domain Layer 불필요

#### Application Layer
- [ ] 라우팅은 Spring Cloud Gateway가 자동 처리

#### Persistence Layer
- [ ] 라우팅은 Stateless이므로 Persistence Layer 불필요

#### Gateway Configuration Layer
- [ ] **라우팅 규칙 명확성**: 각 Route는 고유 ID와 명확한 Path 필요
- [ ] **Circuit Breaker 필수**: 모든 Backend Service에 Circuit Breaker 적용
- [ ] **Fallback 엔드포인트 구현**: Circuit Breaker Open 시 Fallback 필수

#### Reactive 규칙 (추가)
- [ ] **Blocking Call 절대 금지**
  - JDBC (JPA Repository) 사용 금지
  - RestTemplate 사용 금지 → WebClient 필수
  - Thread.sleep() 금지
  - Mono.block(), Flux.blockFirst(), Flux.blockLast() 금지 (테스트 제외)
- [ ] **Reactive Repository 필수**
  - 라우팅은 Stateless이므로 Repository 불필요
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

- [ ] Domain Layer 구현 완료 (없음 - 설정 기반)
- [ ] Application Layer 구현 완료 (없음 - Spring Cloud Gateway 자동 처리)
- [ ] Persistence Layer 구현 완료 (없음 - Stateless)
- [ ] Gateway Configuration Layer 구현 완료 (application.yml 라우팅 규칙, Fallback Controller)
- [ ] Integration Test 구현 완료 (E2E Scenario 8개, TestFixture 1개)
- [ ] 모든 테스트 통과 (Integration + ArchUnit)
- [ ] Zero-Tolerance 규칙 준수
- [ ] 테스트 커버리지 > 90% (Fallback Controller만 해당)
- [ ] 코드 리뷰 승인
- [ ] PR 머지 완료

---

## 🔗 관련 문서

- **PRD**: docs/prd/access-gateway.md (Configuration Layer 섹션)
- **Plan**: docs/prd/plans/GATEWAY-007-service-routing-plan.md (create-plan 후 생성)
- **Jira**: (sync-to-jira 후 추가)

---

## 📚 참고 자료

### 코딩 규칙
- Configuration: docs/coding_convention/05-configuration-layer/ (필요 시 생성)
- REST API: docs/coding_convention/01-adapter-in-layer/rest-api/ (Fallback Controller)

### PRD 섹션
- Configuration Layer (Line 3065-3138)
- Circuit Breaker 설정 (Line 3132-3138)

---

## 🧪 Integration Test 예시

### Scenario 1: Path 기반 라우팅 성공

```java
@Test
void path_기반_라우팅_성공() {
    // Given: Order Service Mock 설정
    wireMockServer.stubFor(
        get(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"orders\": []}")
            )
    );

    // When: Gateway를 통해 Order Service 요청
    ResponseEntity<String> response = webTestClient
        .get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();

    // Then: Order Service로 라우팅됨
    assertThat(response).contains("orders");

    // 검증: WireMock에서 요청 수신 확인
    wireMockServer.verify(
        getRequestedFor(urlEqualTo("/api/v1/orders"))
    );
}
```

### Scenario 3: Circuit Breaker Open → Fallback

```java
@Test
void circuit_breaker_open_시_fallback_호출() {
    // Given: Backend Service 10회 연속 실패 (failureRateThreshold 초과)
    wireMockServer.stubFor(
        get(urlMatching("/api/v1/orders.*"))
            .willReturn(aResponse().withStatus(503))
    );

    // Circuit Breaker Open 상태로 만들기 (10회 실패)
    for (int i = 0; i < 10; i++) {
        webTestClient.get().uri("/api/v1/orders").exchange();
    }

    // When: 11번째 요청
    ResponseEntity<String> response = webTestClient
        .get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();

    // Then: Fallback 응답 수신
    assertThat(response).contains("SERVICE_UNAVAILABLE");
    assertThat(response).contains("order-service");
}
```

---

## 📋 TODO: 라우팅 규칙 정의 필요

**다음 항목들은 구체적인 Backend Service 정보가 결정된 후 작성 필요**:

1. **Backend Service 목록**
   - Service 1: (이름, URL, Path 패턴)
   - Service 2: (이름, URL, Path 패턴)
   - Service 3: (이름, URL, Path 패턴)

2. **Service Discovery 전략**
   - Eureka 사용 여부
   - Consul 사용 여부
   - Static URI 사용 여부

3. **Circuit Breaker 정책**
   - 서비스별 failureRateThreshold 조정 필요 시
   - Fallback 전략 (기본 응답 vs 캐시된 응답)

4. **Load Balancing 전략**
   - Round Robin (기본)
   - Weighted Round Robin
   - Custom Load Balancer

**현재는 템플릿 구조만 제공하며, 실제 라우팅 규칙은 Backend Service 구성 확정 후 추가 예정입니다.**
