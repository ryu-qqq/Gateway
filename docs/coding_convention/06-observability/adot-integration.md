# ADOT Integration Guide — **AWS Distro for OpenTelemetry**

> 이 문서는 **ADOT (AWS Distro for OpenTelemetry)** 연동 가이드입니다.
>
> ECS Sidecar 기반 Metrics/Traces 수집, AMP, X-Ray 연동을 다룹니다.

---

## 1) ADOT 아키텍처

### 1.1) ECS Task 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                         ECS Task                                 │
│                                                                  │
│  ┌──────────────────┐              ┌──────────────────────┐     │
│  │  Spring Boot     │              │   ADOT Collector     │     │
│  │  + ADOT Agent    │              │   (Sidecar)          │     │
│  │                  │              │                      │     │
│  │  :8080/actuator  │◀──scrape────│  prometheus receiver │     │
│  │    /prometheus   │   (30s)     │                      │     │
│  │                  │              │                      │     │
│  │  OTLP push ──────┼────────────▶│  otlp receiver       │     │
│  │  (:4317 gRPC)    │              │  (:4317, :4318)      │     │
│  └──────────────────┘              │                      │     │
│                                    │  awsecscontainermetrics │  │
│                                    │  (ECS 컨테이너 자체 메트릭)  │  │
│                                    └──────────┬─────────────┘     │
└───────────────────────────────────────────────┼─────────────────┘
                                                │
                      ┌───────────────────────────┼───────────────────┐
                      ▼                           ▼                   ▼
                AWS X-Ray                   Amazon Managed       CloudWatch
                (Traces)                    Prometheus           (Logs)
                                            (Metrics)
```

### 1.2) 3가지 Receiver

| Receiver | 방식 | 수집 대상 | Export 대상 |
|----------|------|----------|-------------|
| **prometheus** | Pull (scrape 30s) | `/actuator/prometheus` | AMP |
| **otlp** | Push (gRPC :4317) | ADOT Agent 자동 계측 | X-Ray, AMP |
| **awsecscontainermetrics** | Internal (30s) | ECS Task CPU/Memory | AMP |

---

## 2) Spring Boot 설정

### 2.1) application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        # ADOT Collector가 scrape할 엔드포인트 노출
        include: health,info,metrics,prometheus
      base-path: /actuator

  endpoint:
    prometheus:
      enabled: true

  metrics:
    export:
      prometheus:
        enabled: true  # ADOT Collector가 scrape

    # ⚠️ 필수: 서비스 구분용 태그
    # AMP에서 쿼리 시 application, environment로 필터링
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

### 2.2) 의존성 (build.gradle)

```groovy
dependencies {
    // Micrometer Prometheus Registry (ADOT scrape용)
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // OpenTelemetry API (선택: 수동 계측 시)
    // ADOT Agent가 자동 계측하므로 보통 불필요
    // implementation 'io.opentelemetry:opentelemetry-api'
}
```

---

## 3) Prometheus Receiver (Metrics Pull)

### 3.1) ADOT Config 예시

```yaml
# adot-config.yaml (인프라 프로젝트)
receivers:
  prometheus:
    config:
      scrape_configs:
        - job_name: 'application-metrics'
          scrape_interval: 30s
          static_configs:
            - targets: ['localhost:${APP_PORT}']
          metrics_path: /actuator/prometheus

          # 필터: 필요한 메트릭만 수집
          metric_relabel_configs:
            - source_labels: [__name__]
              regex: '(http_.*|jvm_.*|hikaricp_.*|application_.*|business_.*)'
              action: keep
```

### 3.2) 수집되는 Metrics

| 메트릭 | 설명 | 예시 |
|--------|------|------|
| `http_server_requests_seconds_count` | HTTP 요청 수 | 요청 카운터 |
| `http_server_requests_seconds_sum` | HTTP 요청 총 시간 | latency 계산용 |
| `http_server_requests_seconds_bucket` | HTTP 요청 히스토그램 | percentile 계산용 |
| `jvm_memory_used_bytes` | JVM 메모리 사용량 | 힙/논힙 |
| `jvm_gc_pause_seconds` | GC 일시정지 시간 | GC 모니터링 |
| `hikaricp_connections_active` | 활성 DB 커넥션 | Pool 모니터링 |
| `application_*` | 커스텀 애플리케이션 메트릭 | 개발자 정의 |
| `business_*` | 커스텀 비즈니스 메트릭 | 개발자 정의 |

### 3.3) PromQL 쿼리 예시

```promql
# 5분간 요청 rate
rate(http_server_requests_seconds_count[5m])

# 5xx 에러 rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# P99 latency
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# 에러율 (%)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count[5m])) * 100

# 서비스별 필터링
http_server_requests_seconds_count{application="spring-standards-api", environment="prod"}
```

---

## 4) OTLP Receiver (Traces/Metrics Push)

### 4.1) ADOT Agent 설정

**ECS Task Definition**:
```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "image": "${ECR_REPOSITORY}:${IMAGE_TAG}",
      "portMappings": [
        { "containerPort": 8080, "protocol": "tcp" }
      ],
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
          "value": "service.namespace=${NAMESPACE},deployment.environment=${ENVIRONMENT}"
        },
        {
          "name": "OTEL_TRACES_SAMPLER",
          "value": "parentbased_traceidratio"
        },
        {
          "name": "OTEL_TRACES_SAMPLER_ARG",
          "value": "0.1"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/${SERVICE_NAME}",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    },
    {
      "name": "adot-collector",
      "image": "public.ecr.aws/aws-observability/aws-otel-collector:latest",
      "essential": true,
      "command": ["--config=/etc/ecs/ecs-default-config.yaml"],
      "portMappings": [
        { "containerPort": 4317, "protocol": "tcp" },
        { "containerPort": 4318, "protocol": "tcp" }
      ]
    }
  ]
}
```

### 4.2) 자동 계측 항목 (Traces)

| Span Attribute | 설명 | 예시 |
|----------------|------|------|
| `http.method` | HTTP 메서드 | GET, POST |
| `http.status_code` | 응답 코드 | 200, 404, 500 |
| `http.route` | 요청 경로 | /api/v1/orders/{id} |
| `http.url` | 전체 URL | https://api.example.com/... |
| `db.system` | DB 시스템 | mysql, postgresql |
| `db.name` | 데이터베이스명 | spring_standards |
| `db.statement` | SQL 쿼리 | SELECT * FROM ... |
| `rpc.service` | gRPC 서비스 | OrderService |
| `rpc.method` | gRPC 메서드 | CreateOrder |

### 4.3) Sampling 전략

| 전략 | 설명 | 권장 환경 |
|------|------|----------|
| `always_on` | 모든 트레이스 수집 | 개발, 디버깅 |
| `always_off` | 트레이스 비활성화 | 부하 테스트 |
| `traceidratio` | 비율 기반 샘플링 | 프로덕션 |
| `parentbased_traceidratio` | 부모 기반 + 비율 | **프로덕션 권장** |

```bash
# 10% 샘플링 (프로덕션 권장)
OTEL_TRACES_SAMPLER=parentbased_traceidratio
OTEL_TRACES_SAMPLER_ARG=0.1
```

---

## 5) ECS Container Metrics

### 5.1) ADOT Config

```yaml
# adot-config.yaml
receivers:
  awsecscontainermetrics:
    collection_interval: 30s
```

### 5.2) 수집 메트릭

| 메트릭 | 설명 | 용도 |
|--------|------|------|
| `ecs.task.cpu.utilized` | Task CPU 사용량 | Auto Scaling |
| `ecs.task.cpu.reserved` | Task CPU 예약량 | 용량 계획 |
| `ecs.task.memory.utilized` | Task 메모리 사용량 | OOM 감지 |
| `ecs.task.memory.reserved` | Task 메모리 예약량 | 용량 계획 |
| `ecs.container.network.rx_bytes` | 네트워크 수신 | 트래픽 모니터링 |
| `ecs.container.network.tx_bytes` | 네트워크 송신 | 트래픽 모니터링 |
| `ecs.container.storage.read_bytes` | 스토리지 읽기 | I/O 모니터링 |
| `ecs.container.storage.write_bytes` | 스토리지 쓰기 | I/O 모니터링 |

---

## 6) 커스텀 Metrics 구현

### 6.1) Business Metrics

```java
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Counter: 주문 건수
    public void incrementOrderCount(String status) {
        Counter.builder("business.orders.total")
            .description("Total number of orders")
            .tag("status", status)  // success, failed, cancelled
            .register(meterRegistry)
            .increment();
    }

    // Timer: 결제 처리 시간
    public void recordPaymentDuration(Duration duration, String method) {
        Timer.builder("business.payment.duration")
            .description("Payment processing duration")
            .tag("method", method)  // card, bank_transfer, virtual_account
            .register(meterRegistry)
            .record(duration);
    }

    // Gauge: 장바구니 아이템 수
    public void recordCartItems(String userId, int count) {
        Gauge.builder("business.cart.items", () -> count)
            .description("Number of items in cart")
            .tag("userId", userId)
            .register(meterRegistry);
    }
}
```

### 6.2) Downstream Metrics

```java
@Component
public class DownstreamMetrics {

    private final MeterRegistry meterRegistry;

    public DownstreamMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Timer: Redis 응답 시간
    public Timer.Sample startRedisTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopRedisTimer(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("downstream.redis.latency")
            .description("Redis operation latency")
            .tag("operation", operation)  // GET, SET, DEL
            .register(meterRegistry));
    }

    // Timer: 외부 API 응답 시간
    public void recordExternalApiLatency(Duration duration, String service, String endpoint) {
        Timer.builder("downstream.external_api.latency")
            .description("External API call latency")
            .tag("service", service)  // payment-gateway, notification-service
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .record(duration);
    }
}
```

### 6.3) 권장 Metrics 네이밍

| 분류 | Prefix | 예시 |
|------|--------|------|
| **비즈니스** | `business.*` | `business.orders.total`, `business.payment.duration` |
| **Downstream** | `downstream.*` | `downstream.redis.latency`, `downstream.db.latency` |
| **스케줄러** | `scheduler.*` | `scheduler.job.runs.total`, `scheduler.job.duration` |
| **캐시** | `cache.*` | `cache.hit.ratio`, `cache.size` |
| **큐** | `queue.*` | `queue.messages.count`, `queue.processing.duration` |

---

## 7) AMP (Amazon Managed Prometheus)

### 7.1) Terraform 설정 (인프라 프로젝트)

```hcl
# AMP Workspace
resource "aws_prometheus_workspace" "main" {
  alias = "${var.project_name}-${var.environment}"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ADOT Collector가 AMP에 쓰기 위한 IAM Role
resource "aws_iam_role" "adot_collector" {
  name = "${var.project_name}-adot-collector"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "adot_amp_write" {
  name = "amp-remote-write"
  role = aws_iam_role.adot_collector.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "aps:RemoteWrite"
        ]
        Resource = aws_prometheus_workspace.main.arn
      }
    ]
  })
}
```

### 7.2) ADOT Exporter 설정

```yaml
# adot-config.yaml
exporters:
  prometheusremotewrite:
    endpoint: ${AMP_ENDPOINT}/api/v1/remote_write
    auth:
      authenticator: sigv4auth

  awsxray:
    region: ${AWS_REGION}
```

---

## 8) X-Ray 설정

### 8.1) IAM 권한

```hcl
# ADOT Collector X-Ray 권한
resource "aws_iam_role_policy" "adot_xray" {
  name = "xray-write"
  role = aws_iam_role.adot_collector.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "xray:PutTraceSegments",
          "xray:PutTelemetryRecords",
          "xray:GetSamplingRules",
          "xray:GetSamplingTargets"
        ]
        Resource = "*"
      }
    ]
  })
}
```

### 8.2) X-Ray Sampling Rules

```json
{
  "version": 2,
  "rules": [
    {
      "description": "Health check - no sampling",
      "host": "*",
      "http_method": "GET",
      "url_path": "/actuator/health",
      "fixed_target": 0,
      "rate": 0
    },
    {
      "description": "Default sampling",
      "host": "*",
      "http_method": "*",
      "url_path": "*",
      "fixed_target": 1,
      "rate": 0.1
    }
  ],
  "default": {
    "fixed_target": 1,
    "rate": 0.05
  }
}
```

---

## 9) Alerting Rules (AMP)

### 9.1) 애플리케이션 알람

```yaml
# alerting-rules.yml (인프라 프로젝트)
groups:
  - name: application-alerts
    rules:
      # 5xx 에러율 > 5%
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High 5xx error rate (> 5%)"
          description: "Service {{ $labels.application }} has high error rate"

      # P99 latency > 5초
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)
          ) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 latency > 5s"

      # JVM 힙 사용률 > 90%
      - alert: HighHeapUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap usage > 90%"

      # DB Connection Pool 고갈 임박
      - alert: DBConnectionPoolExhausted
        expr: |
          hikaricp_connections_active
          / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool > 90% utilized"
```

### 9.2) ECS 인프라 알람

```yaml
  - name: infrastructure-alerts
    rules:
      # ECS Task CPU > 80%
      - alert: HighCPUUsage
        expr: |
          ecs_task_cpu_utilized / ecs_task_cpu_reserved > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "ECS Task CPU > 80%"

      # ECS Task Memory > 80%
      - alert: HighMemoryUsage
        expr: |
          ecs_task_memory_utilized / ecs_task_memory_reserved > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "ECS Task Memory > 80%"
```

---

## 10) 데이터 흐름 요약

| Source | Receiver | Destination | 용도 |
|--------|----------|-------------|------|
| `/actuator/prometheus` | prometheus | AMP | 애플리케이션 메트릭 |
| ADOT Agent (JVM) | otlp | X-Ray | 분산 추적 |
| ADOT Agent (JVM) | otlp | AMP | 자동 계측 메트릭 |
| ECS Task Metadata | awsecscontainermetrics | AMP | 컨테이너 리소스 메트릭 |
| stdout (JSON) | awslogs | CloudWatch Logs | 로그 |

---

## 11) 체크리스트

### Spring Boot 설정

- [ ] `/actuator/prometheus` 엔드포인트 노출
- [ ] `management.metrics.tags` 설정 (application, environment)
- [ ] `management.metrics.distribution` 히스토그램 설정
- [ ] `micrometer-registry-prometheus` 의존성 추가

### ECS Task Definition

- [ ] ADOT Agent JAR 포함 (또는 다운로드 스크립트)
- [ ] `JAVA_TOOL_OPTIONS` 환경변수 설정
- [ ] `OTEL_*` 환경변수 설정
- [ ] ADOT Collector Sidecar 컨테이너 추가

### IAM 권한

- [ ] AMP RemoteWrite 권한
- [ ] X-Ray PutTraceSegments 권한
- [ ] CloudWatch Logs CreateLogStream 권한

### 인프라 (Terraform)

- [ ] AMP Workspace 생성
- [ ] ADOT Collector 설정 파일
- [ ] Alerting Rules 설정
- [ ] Grafana 데이터 소스 연결

---

## 12) 관련 문서

| 문서 | 설명 |
|------|------|
| [Observability Guide](./observability-guide.md) | 전체 관측성 가이드 |
| [Logging Configuration](./logging-configuration.md) | Logback 상세 설정 |
| [CloudWatch Integration](./cloudwatch-integration.md) | CloudWatch Logs 연동 |
| [AWS ADOT Documentation](https://aws.amazon.com/otel/) | AWS 공식 문서 |
| [OpenTelemetry Documentation](https://opentelemetry.io/docs/) | OpenTelemetry 공식 문서 |

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
