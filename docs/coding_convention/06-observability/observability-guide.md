# Observability Guide — **Logging, Metrics, Tracing**

> 이 문서는 애플리케이션의 **관측 가능성(Observability)** 가이드입니다.
>
> ADOT Sidecar 기반의 Metrics/Traces 수집과 CloudWatch 기반 Logging을 다룹니다.

---

## 1) 핵심 원칙

### 필수 규칙

| 원칙 | 설명 |
|------|------|
| **Three Pillars** | Logs + Metrics + Traces 통합 관측성 |
| **JSON Structured Logging** | Production 환경에서 JSON 형식 로그 필수 |
| **MDC Context 전파** | traceId, spanId, errorCode 등 컨텍스트 정보 전파 |
| **Application에서 직접 알람 금지** | 로그 기반 인프라 알람 사용 |
| **Metrics Tags 필수** | `application`, `environment` 태그로 서비스 구분 |

### 금지 사항

| 금지 | 이유 |
|------|------|
| **Application에서 Slack 직접 호출** | @Transactional 내 외부 API 호출 금지 |
| **System.out.println** | 구조화되지 않음, 성능 저하 |
| **민감정보 로깅** | 보안 위험, 규정 위반 |
| **과도한 DEBUG 로그 (Production)** | 비용 증가, 성능 저하 |

---

## 2) 아키텍처 개요 (ADOT Sidecar)

### 2.1) 전체 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              ECS Task                                    │
│                                                                          │
│  ┌────────────────────────────┐     ┌─────────────────────────────────┐ │
│  │    Spring Boot Container   │     │      ADOT Collector (Sidecar)   │ │
│  │    + ADOT Java Agent       │     │                                 │ │
│  │                            │     │  ┌─────────────────────────┐    │ │
│  │  :8080/actuator/prometheus │◀────┼──│ prometheus receiver     │    │ │
│  │         (scrape 30s)       │     │  │ (Pull: metrics)         │    │ │
│  │                            │     │  └─────────────────────────┘    │ │
│  │                            │     │                                 │ │
│  │  OTLP push ────────────────┼────▶│  ┌─────────────────────────┐    │ │
│  │  (:4317 gRPC)              │     │  │ otlp receiver           │    │ │
│  │                            │     │  │ (Push: traces, metrics) │    │ │
│  │                            │     │  └─────────────────────────┘    │ │
│  │                            │     │                                 │ │
│  │  stdout (JSON logs) ───────┼─────┼──▶ awslogs driver ──────────┐   │ │
│  │                            │     │                              │   │ │
│  └────────────────────────────┘     │  ┌─────────────────────────┐ │   │ │
│                                     │  │ awsecscontainermetrics  │ │   │ │
│                                     │  │ (Container CPU/Memory)  │ │   │ │
│                                     │  └─────────────────────────┘ │   │ │
│                                     └───────────────┬─────────────┘│   │ │
└─────────────────────────────────────────────────────┼──────────────┼───┘
                                                      │              │
                    ┌─────────────────────────────────┼──────────────┼────┐
                    │                                 ▼              ▼    │
                    │  ┌──────────────────┐  ┌──────────────┐  ┌────────┐ │
                    │  │   AWS X-Ray      │  │    AMP       │  │CloudWatch│
                    │  │   (Traces)       │  │  (Metrics)   │  │ (Logs)  │ │
                    │  └────────┬─────────┘  └──────┬───────┘  └────┬───┘ │
                    │           │                   │               │     │
                    │           ▼                   ▼               ▼     │
                    │  ┌──────────────────────────────────────────────┐   │
                    │  │              Grafana Dashboard               │   │
                    │  │  (Metrics: AMP, Traces: X-Ray, Logs: CW)    │   │
                    │  └──────────────────────────────────────────────┘   │
                    │                                                     │
                    │  ┌──────────────────────────────────────────────┐   │
                    │  │         CloudWatch Alarm → SNS → Slack       │   │
                    │  └──────────────────────────────────────────────┘   │
                    └─────────────────────────────────────────────────────┘
```

### 2.2) Three Pillars 분리

| Pillar | 수집 방식 | 저장소 | 용도 |
|--------|----------|--------|------|
| **Logs** | awslogs driver (stdout) | CloudWatch Logs | 에러 디버깅, 감사 로그 |
| **Metrics** | Prometheus scrape + OTLP push | Amazon Managed Prometheus | 대시보드, 알람 |
| **Traces** | ADOT Agent (auto-instrumentation) | AWS X-Ray | 분산 추적, 성능 분석 |

### 2.3) ADOT Collector Receivers

| Receiver | 방식 | 수집 대상 | Export 대상 |
|----------|------|----------|-------------|
| `prometheus` | Pull (scrape) | `/actuator/prometheus` | AMP |
| `otlp` | Push (gRPC :4317) | Traces, Metrics | X-Ray, AMP |
| `awsecscontainermetrics` | Internal | ECS Task CPU/Memory | AMP |

---

## 3) Spring Boot 설정

### 3.1) application.yml (Actuator + Metrics)

```yaml
# bootstrap-web-api/src/main/resources/application.yml
management:
  endpoints:
    web:
      exposure:
        # ADOT Collector가 scrape할 엔드포인트
        include: health,info,metrics,prometheus
      base-path: /actuator

  endpoint:
    health:
      show-details: when-authorized
    prometheus:
      enabled: true

  metrics:
    export:
      prometheus:
        enabled: true  # ADOT Collector가 scrape

    # ⚠️ 필수: 서비스 구분용 태그
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:local}

    # 히스토그램 분포 설정 (SLO 기반)
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
      slo:
        http.server.requests: 100ms,500ms,1s,5s
```

### 3.2) 의존성 (build.gradle)

```groovy
dependencies {
    // Micrometer Prometheus Registry
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Logstash Encoder (JSON 로깅)
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'

    // OpenTelemetry (선택: 수동 계측 시)
    // ADOT Agent가 자동 계측하므로 보통 불필요
    // implementation 'io.opentelemetry:opentelemetry-api'
}
```

### 3.3) ADOT Agent 설정 (ECS Task Definition)

```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "image": "${ECR_IMAGE}",
      "environment": [
        {
          "name": "JAVA_TOOL_OPTIONS",
          "value": "-javaagent:/opt/aws-opentelemetry-agent.jar"
        },
        {
          "name": "OTEL_EXPORTER_OTLP_ENDPOINT",
          "value": "http://localhost:4317"
        },
        {
          "name": "OTEL_SERVICE_NAME",
          "value": "${SERVICE_NAME}"
        },
        {
          "name": "OTEL_RESOURCE_ATTRIBUTES",
          "value": "service.namespace=${NAMESPACE},deployment.environment=${ENV}"
        }
      ]
    },
    {
      "name": "adot-collector",
      "image": "public.ecr.aws/aws-observability/aws-otel-collector:latest",
      "essential": true
    }
  ]
}
```

---

## 4) Metrics 상세

### 4.1) 수집되는 Metrics

**Prometheus Receiver (Pull)**:
| 메트릭 | 설명 | 필터 |
|--------|------|------|
| `http_server_requests_*` | HTTP 요청 수, latency | `http_*` |
| `jvm_*` | JVM 메모리, GC, 스레드 | - |
| `hikaricp_*` | DB Connection Pool | - |
| `application_*` | 커스텀 애플리케이션 메트릭 | `application_*` |
| `business_*` | 커스텀 비즈니스 메트릭 | `business_*` |

**ADOT Agent (Push via OTLP)**:
| 메트릭 | 설명 |
|--------|------|
| JVM metrics | 자동 계측 |
| HTTP client metrics | RestTemplate, WebClient |
| DB metrics | JDBC 자동 계측 |

**ECS Container Metrics**:
| 메트릭 | 설명 |
|--------|------|
| `ecs.task.cpu.*` | Task CPU 사용량 |
| `ecs.task.memory.*` | Task 메모리 사용량 |
| `ecs.container.network.*` | 네트워크 I/O |

### 4.2) 커스텀 Metrics 구현

```java
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // Counter: 이벤트 발생 횟수
    public void incrementOrderCount(String status) {
        Counter.builder("business.orders.total")
            .tag("status", status)  // success, failed
            .register(meterRegistry)
            .increment();
    }

    // Timer: 처리 시간 측정
    public void recordPaymentDuration(Duration duration, String method) {
        Timer.builder("business.payment.duration")
            .tag("method", method)  // card, bank_transfer
            .register(meterRegistry)
            .record(duration);
    }

    // Gauge: 현재 상태 값
    public void setActiveUsers(int count) {
        Gauge.builder("business.users.active", () -> count)
            .register(meterRegistry);
    }
}
```

### 4.3) 권장 커스텀 Metrics

| 분류 | 메트릭 | 설명 |
|------|--------|------|
| **Business** | `business.orders.total` | 주문 건수 (status: success/failed) |
| **Business** | `business.payment.duration` | 결제 처리 시간 |
| **Downstream** | `downstream.redis.latency` | Redis 응답 시간 |
| **Downstream** | `downstream.external_api.latency` | 외부 API 응답 시간 |
| **Scheduler** | `scheduler.job.runs.total` | 스케줄러 실행 횟수 |

---

## 5) Traces 상세 (AWS X-Ray)

### 5.1) ADOT Agent 자동 계측

| 항목 | 설명 |
|------|------|
| `http.method` | HTTP 메서드 (GET, POST 등) |
| `http.status_code` | HTTP 응답 코드 |
| `http.route` | 요청 경로 |
| `db.system` | DB 시스템 (mysql, postgresql) |
| `db.name` | 데이터베이스명 |
| `rpc.service` | gRPC 서비스명 |

### 5.2) 수동 Span 추가 (선택)

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final Tracer tracer;

    public Order processOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("processOrder")
            .setAttribute("order.type", request.type())
            .startSpan();

        try (var scope = span.makeCurrent()) {
            // 비즈니스 로직
            return createOrder(request);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 5.3) X-Ray Trace 예시

```
[Client] → [ALB] → [Spring Boot] → [MySQL]
                        │
                        ├── GET /api/v1/orders
                        │       duration: 120ms
                        │
                        ├── SELECT * FROM orders
                        │       duration: 45ms
                        │
                        └── Redis GET order:123
                                duration: 5ms
```

---

## 6) Logs 상세 (CloudWatch)

### 6.1) Logback 설정

| Profile | 형식 | 용도 |
|---------|------|------|
| `local`, `test` | Pattern (Human-readable) | 개발/디버깅 |
| `prod`, `staging` | JSON (LogstashEncoder) | CloudWatch/ELK |

### 6.2) MDC 필드

| 필드 | 설명 | 설정 위치 |
|------|------|----------|
| `traceId` | X-Ray Trace ID | ADOT Agent 자동 |
| `spanId` | 현재 Span ID | ADOT Agent 자동 |
| `requestId` | 요청 고유 ID | RequestIdFilter |
| `userId` | 인증된 사용자 ID | Security Filter |
| `errorCode` | 도메인 에러 코드 | GlobalExceptionHandler |

### 6.3) JSON 로그 출력 예시

```json
{
  "@timestamp": "2025-12-05T10:30:00.000+09:00",
  "level": "ERROR",
  "message": "DomainException: code=ORDER_NOT_FOUND",
  "logger": "c.r.a.i.r.c.GlobalExceptionHandler",
  "traceId": "abc123def456",
  "spanId": "789xyz",
  "errorCode": "ORDER_NOT_FOUND",
  "application": "spring-standards-api",
  "environment": "prod",
  "stack_trace": "..."
}
```

---

## 7) 알람 전략

### 7.1) 데이터 소스별 알람

| 소스 | 알람 대상 | 도구 |
|------|----------|------|
| **CloudWatch Logs** | ERROR 로그 급증, 특정 errorCode | Metric Filter + Alarm |
| **AMP (Metrics)** | HTTP 5xx rate, latency p99 | Prometheus Alerting Rules |
| **X-Ray (Traces)** | Error rate, Fault rate | X-Ray Insights |
| **ECS Metrics** | CPU > 80%, Memory > 80% | CloudWatch Alarm |

### 7.2) 알람 우선순위

| 우선순위 | 조건 | 알람 채널 |
|---------|------|----------|
| **P1 (Critical)** | 5xx > 50/min, 서비스 다운 | PagerDuty + Slack |
| **P2 (High)** | 5xx > 10/5min, latency p99 > 5s | Slack (#alerts-prod) |
| **P3 (Medium)** | 4xx 급증, CPU > 80% | Slack (#alerts-warning) |
| **P4 (Low)** | 비정상 패턴 감지 | Slack (#alerts-info) |

### 7.3) Prometheus Alerting Rules (AMP)

```yaml
# alerting-rules.yml (인프라 프로젝트)
groups:
  - name: application-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High 5xx error rate (> 5%)"

      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 latency > 5s"
```

---

## 8) Grafana 대시보드

### 8.1) 권장 패널 구성

| 패널 | 데이터 소스 | 쿼리 |
|------|------------|------|
| **Request Rate** | AMP | `rate(http_server_requests_seconds_count[5m])` |
| **Error Rate** | AMP | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` |
| **Latency P99** | AMP | `histogram_quantile(0.99, ...)` |
| **JVM Heap** | AMP | `jvm_memory_used_bytes{area="heap"}` |
| **DB Pool** | AMP | `hikaricp_connections_active` |
| **Trace Explorer** | X-Ray | Service Map |
| **Error Logs** | CloudWatch | `{ $.level = "ERROR" }` |

### 8.2) Service Map (X-Ray)

X-Ray 콘솔에서 자동 생성되는 서비스 맵으로 전체 아키텍처 시각화:
- 서비스 간 의존성
- 평균 응답 시간
- 에러율

---

## 9) Do / Don't

### ✅ Do

```java
// ✅ Good: Metrics 태그로 서비스 구분
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}

// ✅ Good: 비즈니스 메트릭 추가
Counter.builder("business.orders.total")
    .tag("status", "success")
    .register(meterRegistry)
    .increment();

// ✅ Good: MDC로 컨텍스트 전파
MDC.put("errorCode", ex.code());
try {
    log.error("DomainException: code={}", ex.code(), ex);
} finally {
    MDC.remove("errorCode");
}
```

### ❌ Don't

```java
// ❌ Bad: Metrics 태그 없이 사용
management:
  metrics:
    export:
      prometheus:
        enabled: true
// → 서비스 구분 불가!

// ❌ Bad: Application에서 직접 알람 발송
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    slackClient.sendMessage("Order placed!");  // 절대 금지!
}

// ❌ Bad: 민감정보 로깅
log.info("Payment: cardNumber={}", cardNumber);
```

---

## 10) 체크리스트

### Spring Boot 설정

- [ ] `/actuator/prometheus` 엔드포인트 노출
- [ ] `management.metrics.tags` 설정 (application, environment)
- [ ] `management.metrics.distribution` 히스토그램 설정
- [ ] Logback JSON 구조화 로깅 (prod/staging)

### ADOT 설정 (인프라)

- [ ] ADOT Agent Java 옵션 추가 (`-javaagent`)
- [ ] ADOT Collector Sidecar 설정
- [ ] AMP Workspace 생성
- [ ] X-Ray 권한 설정

### 알람 설정 (인프라)

- [ ] CloudWatch Metric Filter (ERROR 로그)
- [ ] AMP Alerting Rules (5xx rate, latency)
- [ ] SNS → Slack 연동

### 대시보드

- [ ] Grafana 대시보드 구성
- [ ] X-Ray Service Map 확인

---

## 11) 관련 문서

| 문서 | 설명 |
|------|------|
| [Logging Configuration](./logging-configuration.md) | Logback 상세 설정 |
| [CloudWatch Integration](./cloudwatch-integration.md) | CloudWatch Logs 연동 |
| [ADOT Integration](./adot-integration.md) | ADOT + AMP + X-Ray 연동 |
| [Error Handling Guide](../01-adapter-in-layer/rest-api/error/error-guide.md) | RFC 7807 에러 처리 |

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 2.0.0
