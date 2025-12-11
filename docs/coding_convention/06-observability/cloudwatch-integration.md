# CloudWatch Integration Guide — **AWS 로그 모니터링**

> 이 문서는 **CloudWatch 연동** 가이드입니다.
>
> ECS 로그 수집, Metric Filter, Alarm 설정을 다룹니다.

---

## 1) 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│ ECS Task (Fargate)                                          │
│   └─ Container: app                                         │
│       └─ stdout/stderr → awslogs driver                    │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ CloudWatch Logs                                             │
│   └─ Log Group: /ecs/{service-name}                        │
│       └─ Log Stream: {container-id}                        │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ CloudWatch Metric Filter                                    │
│   ├─ ErrorCount (level = ERROR)                            │
│   ├─ PaymentFailure (errorCode = PAYMENT_*)                │
│   └─ SlowResponse (duration > 3000)                        │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ CloudWatch Alarm                                            │
│   └─ Threshold 초과 시 SNS 발행                             │
└─────────────────────────┬───────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ SNS Topic                                                   │
│   ├─ AWS Chatbot → Slack                                   │
│   ├─ Lambda → Custom Notification                          │
│   └─ Email Subscription                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 2) ECS Task Definition 설정

### 2.1) awslogs 드라이버 설정

```json
{
  "containerDefinitions": [
    {
      "name": "app",
      "image": "${ECR_IMAGE}",
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/${SERVICE_NAME}",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "true"
        }
      }
    }
  ]
}
```

### 2.2) Terraform 예시

```hcl
# 인프라 프로젝트에서 설정
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.service_name}"
  retention_in_days = 30

  tags = {
    Service     = var.service_name
    Environment = var.environment
  }
}

resource "aws_ecs_task_definition" "app" {
  family = var.service_name

  container_definitions = jsonencode([
    {
      name  = "app"
      image = "${var.ecr_repository_url}:${var.image_tag}"

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}
```

---

## 3) Metric Filter 패턴

### 3.1) 기본 에러 감지

```hcl
# 모든 ERROR 로그 카운트
resource "aws_cloudwatch_log_metric_filter" "error_count" {
  name           = "${var.service_name}-error-count"
  pattern        = "{ $.level = \"ERROR\" }"
  log_group_name = aws_cloudwatch_log_group.app.name

  metric_transformation {
    name          = "ErrorCount"
    namespace     = "Application/${var.service_name}"
    value         = "1"
    default_value = "0"
  }
}
```

### 3.2) 비즈니스 에러 감지

```hcl
# 결제 실패 에러
resource "aws_cloudwatch_log_metric_filter" "payment_failure" {
  name           = "${var.service_name}-payment-failure"
  pattern        = "{ $.errorCode = \"PAYMENT_*\" }"
  log_group_name = aws_cloudwatch_log_group.app.name

  metric_transformation {
    name          = "PaymentFailureCount"
    namespace     = "Application/${var.service_name}"
    value         = "1"
    default_value = "0"
  }
}

# 주문 에러
resource "aws_cloudwatch_log_metric_filter" "order_error" {
  name           = "${var.service_name}-order-error"
  pattern        = "{ $.errorCode = \"ORDER_*\" && $.level = \"ERROR\" }"
  log_group_name = aws_cloudwatch_log_group.app.name

  metric_transformation {
    name          = "OrderErrorCount"
    namespace     = "Application/${var.service_name}"
    value         = "1"
    default_value = "0"
  }
}
```

### 3.3) 5xx 서버 에러

```hcl
# 스택트레이스가 있는 에러 (5xx)
resource "aws_cloudwatch_log_metric_filter" "server_error" {
  name           = "${var.service_name}-server-error"
  pattern        = "{ $.level = \"ERROR\" && $.stack_trace EXISTS }"
  log_group_name = aws_cloudwatch_log_group.app.name

  metric_transformation {
    name          = "ServerErrorCount"
    namespace     = "Application/${var.service_name}"
    value         = "1"
    default_value = "0"
  }
}
```

### 3.4) Filter Pattern 문법

| 패턴 | 설명 | 예시 |
|------|------|------|
| `{ $.field = "value" }` | 정확히 일치 | `{ $.level = "ERROR" }` |
| `{ $.field = "prefix*" }` | Prefix 매칭 | `{ $.errorCode = "ORDER_*" }` |
| `{ $.field EXISTS }` | 필드 존재 | `{ $.stack_trace EXISTS }` |
| `{ $.field NOT EXISTS }` | 필드 미존재 | `{ $.userId NOT EXISTS }` |
| `{ $.field > 1000 }` | 숫자 비교 | `{ $.duration > 3000 }` |
| `&&` | AND 조건 | `{ $.level = "ERROR" && $.service = "api" }` |
| `\|\|` | OR 조건 | `{ $.level = "ERROR" \|\| $.level = "WARN" }` |

---

## 4) CloudWatch Alarm 설정

### 4.1) 에러율 알람

```hcl
resource "aws_cloudwatch_metric_alarm" "error_rate_high" {
  alarm_name          = "${var.service_name}-error-rate-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ErrorCount"
  namespace           = "Application/${var.service_name}"
  period              = 300  # 5분
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "Error count exceeded 10 in 5 minutes"

  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]

  treat_missing_data = "notBreaching"

  tags = {
    Service     = var.service_name
    Environment = var.environment
    Severity    = "high"
  }
}
```

### 4.2) 결제 실패 알람 (Critical)

```hcl
resource "aws_cloudwatch_metric_alarm" "payment_failure_critical" {
  alarm_name          = "${var.service_name}-payment-failure-critical"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "PaymentFailureCount"
  namespace           = "Application/${var.service_name}"
  period              = 60  # 1분
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Payment failures exceeded 5 in 1 minute"

  alarm_actions = [
    aws_sns_topic.alerts_critical.arn,  # PagerDuty
    aws_sns_topic.alerts.arn            # Slack
  ]

  treat_missing_data = "notBreaching"

  tags = {
    Service     = var.service_name
    Environment = var.environment
    Severity    = "critical"
  }
}
```

### 4.3) 알람 우선순위 매핑

| Severity | Threshold | Period | Action |
|----------|-----------|--------|--------|
| Critical (P1) | > 5 errors | 1분 | PagerDuty + Slack |
| High (P2) | > 10 errors | 5분 | Slack (#alerts-prod) |
| Medium (P3) | > 50 errors | 15분 | Slack (#alerts-warning) |
| Low (P4) | > 100 errors | 1시간 | Slack (#alerts-info) |

---

## 5) SNS + Slack 연동

### 5.1) AWS Chatbot (권장)

```hcl
# SNS Topic
resource "aws_sns_topic" "alerts" {
  name = "${var.service_name}-alerts"
}

# AWS Chatbot (콘솔에서 설정 필요)
# 1. AWS Chatbot 콘솔 접속
# 2. Slack workspace 연결
# 3. Channel 설정
# 4. SNS Topic 구독
```

### 5.2) Lambda + Slack Webhook (커스텀)

```hcl
# Lambda Function
resource "aws_lambda_function" "slack_notifier" {
  filename         = "slack_notifier.zip"
  function_name    = "${var.service_name}-slack-notifier"
  role             = aws_iam_role.lambda_exec.arn
  handler          = "index.handler"
  runtime          = "nodejs18.x"

  environment {
    variables = {
      SLACK_WEBHOOK_URL = var.slack_webhook_url
    }
  }
}

# SNS → Lambda 구독
resource "aws_sns_topic_subscription" "lambda" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.slack_notifier.arn
}
```

**Lambda 코드 (Node.js)**:
```javascript
const https = require('https');

exports.handler = async (event) => {
    const message = JSON.parse(event.Records[0].Sns.Message);

    const slackMessage = {
        blocks: [
            {
                type: "header",
                text: {
                    type: "plain_text",
                    text: `🚨 ${message.AlarmName}`,
                    emoji: true
                }
            },
            {
                type: "section",
                fields: [
                    { type: "mrkdwn", text: `*Status:*\n${message.NewStateValue}` },
                    { type: "mrkdwn", text: `*Reason:*\n${message.NewStateReason}` }
                ]
            }
        ]
    };

    const options = {
        hostname: 'hooks.slack.com',
        path: process.env.SLACK_WEBHOOK_URL.replace('https://hooks.slack.com', ''),
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    };

    return new Promise((resolve, reject) => {
        const req = https.request(options, (res) => resolve({ statusCode: 200 }));
        req.write(JSON.stringify(slackMessage));
        req.end();
    });
};
```

---

## 6) CloudWatch Logs Insights 쿼리

### 6.1) 에러 분석

```sql
-- 최근 1시간 에러 목록
fields @timestamp, level, errorCode, message, traceId
| filter level = "ERROR"
| sort @timestamp desc
| limit 100

-- 에러 코드별 집계
fields errorCode
| filter level = "ERROR"
| stats count(*) as count by errorCode
| sort count desc
| limit 20

-- 특정 traceId 추적
fields @timestamp, level, message, errorCode
| filter traceId = "abc123def456"
| sort @timestamp asc
```

### 6.2) 사용자별 에러

```sql
-- 사용자별 에러 빈도
fields userId, errorCode
| filter level = "ERROR" and userId != ""
| stats count(*) as errorCount by userId, errorCode
| sort errorCount desc
| limit 50
```

### 6.3) 시간대별 에러 추이

```sql
-- 5분 단위 에러 추이
fields @timestamp
| filter level = "ERROR"
| stats count(*) as errorCount by bin(5m)
| sort @timestamp asc
```

---

## 7) Dashboard 구성

### 7.1) Terraform 예시

```hcl
resource "aws_cloudwatch_dashboard" "app" {
  dashboard_name = "${var.service_name}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6

        properties = {
          title   = "Error Count"
          region  = var.aws_region
          metrics = [
            ["Application/${var.service_name}", "ErrorCount", { stat = "Sum", period = 300 }]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6

        properties = {
          title   = "Payment Failures"
          region  = var.aws_region
          metrics = [
            ["Application/${var.service_name}", "PaymentFailureCount", { stat = "Sum", period = 60 }]
          ]
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 6
        width  = 24
        height = 6

        properties = {
          title  = "Recent Errors"
          region = var.aws_region
          query  = "SOURCE '/ecs/${var.service_name}' | fields @timestamp, errorCode, message | filter level = 'ERROR' | sort @timestamp desc | limit 20"
        }
      }
    ]
  })
}
```

---

## 8) 비용 최적화

### 8.1) Log Retention 설정

```hcl
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.service_name}"
  retention_in_days = 30  # 30일 후 자동 삭제

  # 또는 환경별 설정
  # Production: 90일
  # Staging: 14일
  # Development: 7일
}
```

### 8.2) Log Class 선택

| Log Class | 용도 | 비용 |
|-----------|------|------|
| Standard | 실시간 분석, 알람 | 높음 |
| Infrequent Access | 아카이브, 감사 로그 | 50% 저렴 |

```hcl
resource "aws_cloudwatch_log_group" "audit" {
  name              = "/ecs/${var.service_name}/audit"
  retention_in_days = 365
  log_group_class   = "INFREQUENT_ACCESS"
}
```

### 8.3) Sampling (고트래픽 서비스)

Application 레벨에서 DEBUG/INFO 로그 샘플링:
```java
// 10% 샘플링 (고트래픽)
if (ThreadLocalRandom.current().nextInt(10) == 0) {
    log.debug("Request processed: {}", requestId);
}
```

---

## 9) 체크리스트

### ECS 설정

- [ ] awslogs 드라이버 설정
- [ ] Log Group 생성
- [ ] IAM Role에 logs:CreateLogStream, logs:PutLogEvents 권한

### Metric Filter

- [ ] ERROR 카운트 필터
- [ ] 비즈니스 크리티컬 에러 필터
- [ ] 5xx 서버 에러 필터

### Alarm

- [ ] 에러율 알람 (P2)
- [ ] 크리티컬 에러 알람 (P1)
- [ ] SNS Topic 연결
- [ ] Slack 알람 테스트

### 비용

- [ ] Log Retention 설정
- [ ] 불필요한 DEBUG 로그 제거

---

## 10) 관련 문서

| 문서 | 설명 |
|------|------|
| [Observability Guide](./observability-guide.md) | 전체 관측성 가이드 |
| [Logging Configuration](./logging-configuration.md) | Logback 설정 |
| [AWS CloudWatch Docs](https://docs.aws.amazon.com/cloudwatch/) | AWS 공식 문서 |

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
