# Scheduler Guide — **배치 작업 오케스트레이션**

> Scheduler는 **주기적 배치 작업**을 오케스트레이션합니다.
>
> **UseCase/Service를 호출**하여 작업을 위임하고, **직접 비즈니스 로직이나 Port를 호출하지 않습니다.**
>
> ECS Scheduled Task로 독립 배포 가능한 구조를 권장합니다.

---

## 1) 핵심 원칙

* **오케스트레이션만**: Scheduler는 UseCase/Service 호출만, 비즈니스 로직 없음
* **Port 직접 의존 금지**: QueryPort, PersistencePort 직접 호출 금지
* **분산 락 필수**: 여러 인스턴스에서 동시 실행 방지
* **멱등성 보장**: 같은 데이터 여러 번 처리해도 동일 결과
* **배치 크기 제한**: 무한 루프 방지, 최대 처리 건수 설정
* **메트릭 필수**: 실행 시간, 처리 건수, 성공/실패율 추적

---

## 2) 아키텍처 패턴

### Scheduler ↔ Application Layer 관계

```
┌─────────────────────────────────────────────────────────────────┐
│ Scheduler (오케스트레이터)                                        │
│                                                                 │
│   @Scheduled(fixedRate = 300000)                                │
│   public void retryUnpublishedOutboxes() {                      │
│       // 1. 분산 락 획득                                          │
│       // 2. UseCase 호출 (비즈니스 위임)                          │
│       // 3. 메트릭 기록                                          │
│   }                                                             │
└─────────────────────────────────────────┬───────────────────────┘
                                          │
                                          ↓
┌─────────────────────────────────────────────────────────────────┐
│ UseCase / Service (비즈니스 로직)                                │
│                                                                 │
│   RetryOutboxUseCase.execute()                                  │
│   ├─ QueryPort로 미발행 Outbox 조회                              │
│   ├─ Domain 상태 변경 (outbox.markAsPublished())                │
│   ├─ 외부 시스템 발행 (SQS, Kafka 등)                            │
│   └─ PersistencePort로 저장                                     │
└─────────────────────────────────────────────────────────────────┘
```

### 왜 UseCase를 통해야 하는가?

| 직접 Port 호출 (❌) | UseCase 통한 호출 (✅) |
|-------------------|---------------------|
| 비즈니스 로직 중복 | 로직 재사용 |
| 테스트 어려움 | UseCase 단위 테스트 |
| 트랜잭션 경계 불명확 | UseCase에서 명확한 경계 |
| CQRS 원칙 위반 | CQRS 분리 유지 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ scheduler/                          ← Scheduler 위치
│  └─ {Bc}RetryScheduler.java
├─ service/
│  └─ command/
│      └─ Retry{Bc}Service.java        ← UseCase 구현체
├─ port/
│  ├─ in/
│  │   └─ command/
│  │       └─ Retry{Bc}UseCase.java    ← UseCase 인터페이스
│  └─ out/
│      └─ common/
│          └─ DistributedLockPort.java ← 분산 락 Port
└─ assembler/
   └─ {Bc}Assembler.java               ← DTO 변환
```

---

## 4) 기본 구조

```java
package com.ryuqq.application.{bc}.scheduler;

import com.ryuqq.application.{bc}.port.in.command.Retry{Bc}UseCase;
import com.ryuqq.application.common.port.out.DistributedLockPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {Bc} Retry Scheduler
 *
 * <p>미발행 {Bc}를 주기적으로 재시도합니다.
 *
 * <p><strong>실행 주기</strong>: 5분 (설정 가능)
 *
 * <p><strong>핵심 원칙</strong>:
 * <ul>
 *   <li>UseCase를 통한 비즈니스 위임 (Port 직접 호출 금지)</li>
 *   <li>분산 락으로 중복 실행 방지</li>
 *   <li>최대 처리 건수 제한 (무한 루프 방지)</li>
 *   <li>메트릭 기록 (실행 시간, 성공/실패율)</li>
 * </ul>
 *
 * <p><strong>활성화 조건</strong>: {@code scheduler.{bc}-retry.enabled=true}
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(
        name = "scheduler.{bc}-retry.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class {Bc}RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger({Bc}RetryScheduler.class);
    private static final String JOB_NAME = "{bc}-retry";
    private static final String LOCK_KEY = "scheduler:{bc}-retry";
    private static final int MAX_ITERATIONS = 10;  // 무한 루프 방지

    private final Retry{Bc}UseCase retryUseCase;
    private final DistributedLockPort lockPort;
    private final MeterRegistry meterRegistry;

    public {Bc}RetryScheduler(
            Retry{Bc}UseCase retryUseCase,
            DistributedLockPort lockPort,
            MeterRegistry meterRegistry) {
        this.retryUseCase = retryUseCase;
        this.lockPort = lockPort;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 미발행 {Bc}를 재시도합니다.
     *
     * <p>5분마다 실행됩니다.
     */
    @Scheduled(fixedRateString = "${scheduler.{bc}-retry.fixed-rate:300000}")
    public void retry() {
        log.info("[{}] Starting scheduled job", JOB_NAME);
        Timer.Sample sample = Timer.start(meterRegistry);

        // 1. 분산 락 획득 시도
        boolean lockAcquired = lockPort.tryLock(LOCK_KEY);
        if (!lockAcquired) {
            log.info("[{}] Lock not acquired, skipping", JOB_NAME);
            return;
        }

        try {
            // 2. UseCase 호출 (비즈니스 위임)
            RetryResult result = executeWithLimit();

            // 3. 메트릭 기록
            recordMetrics(sample, result, true);

            log.info("[{}] Completed. processed={}, succeeded={}, failed={}",
                    JOB_NAME, result.processed(), result.succeeded(), result.failed());

        } catch (Exception e) {
            recordMetrics(sample, RetryResult.empty(), false);
            log.error("[{}] Failed", JOB_NAME, e);
            throw e;
        } finally {
            lockPort.unlock(LOCK_KEY);
        }
    }

    /**
     * 최대 반복 횟수 제한하여 실행
     */
    private RetryResult executeWithLimit() {
        int totalProcessed = 0;
        int totalSucceeded = 0;
        int totalFailed = 0;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            RetryResult batchResult = retryUseCase.execute();

            totalProcessed += batchResult.processed();
            totalSucceeded += batchResult.succeeded();
            totalFailed += batchResult.failed();

            // 더 이상 처리할 데이터 없으면 종료
            if (!batchResult.hasMore()) {
                break;
            }

            log.debug("[{}] Iteration {} completed. processed={}",
                    JOB_NAME, i + 1, batchResult.processed());
        }

        return new RetryResult(totalProcessed, totalSucceeded, totalFailed, false);
    }

    private void recordMetrics(Timer.Sample sample, RetryResult result, boolean success) {
        sample.stop(Timer.builder("scheduler.execution.time")
                .tag("job", JOB_NAME)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));

        meterRegistry.counter("scheduler.items.processed",
                "job", JOB_NAME).increment(result.processed());
        meterRegistry.counter("scheduler.items.succeeded",
                "job", JOB_NAME).increment(result.succeeded());
        meterRegistry.counter("scheduler.items.failed",
                "job", JOB_NAME).increment(result.failed());
    }
}
```

---

## 5) UseCase 구현

### Port-In Interface

```java
package com.ryuqq.application.{bc}.port.in.command;

/**
 * {Bc} Retry UseCase
 *
 * <p>미발행 {Bc}를 배치로 재시도합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public interface Retry{Bc}UseCase {

    /**
     * 미발행 건 배치 재시도
     *
     * @return 처리 결과 (처리 건수, 성공/실패, 추가 데이터 유무)
     */
    RetryResult execute();
}
```

### Service 구현체

```java
package com.ryuqq.application.{bc}.service.command;

import com.ryuqq.application.{bc}.assembler.{Bc}Assembler;
import com.ryuqq.application.{bc}.manager.command.{Bc}OutboxTransactionManager;
import com.ryuqq.application.{bc}.manager.query.{Bc}OutboxReadManager;
import com.ryuqq.application.{bc}.port.in.command.Retry{Bc}UseCase;
import com.ryuqq.application.{bc}.port.in.command.RetryResult;
import com.ryuqq.application.{bc}.port.out.{Bc}PublishPort;
import com.ryuqq.domain.{bc}.{Bc}Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {Bc} Retry Service
 *
 * <p>미발행 Outbox를 조회하고 재발행합니다.
 *
 * <p><strong>책임</strong>:
 * <ul>
 *   <li>ReadManager로 미발행 Outbox 조회</li>
 *   <li>Domain 상태 변경 (markAsPublished)</li>
 *   <li>외부 시스템 발행</li>
 *   <li>TransactionManager로 저장</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class Retry{Bc}Service implements Retry{Bc}UseCase {

    private static final Logger log = LoggerFactory.getLogger(Retry{Bc}Service.class);
    private static final int BATCH_SIZE = 100;

    private final {Bc}OutboxReadManager readManager;
    private final {Bc}OutboxTransactionManager transactionManager;
    private final {Bc}PublishPort publishPort;
    private final {Bc}Assembler assembler;

    public Retry{Bc}Service(
            {Bc}OutboxReadManager readManager,
            {Bc}OutboxTransactionManager transactionManager,
            {Bc}PublishPort publishPort,
            {Bc}Assembler assembler) {
        this.readManager = readManager;
        this.transactionManager = transactionManager;
        this.publishPort = publishPort;
        this.assembler = assembler;
    }

    @Override
    public RetryResult execute() {
        // 1. 미발행 Outbox 조회
        List<{Bc}Outbox> unpublishedList = readManager.findUnpublished(BATCH_SIZE);

        if (unpublishedList.isEmpty()) {
            return RetryResult.empty();
        }

        int succeeded = 0;
        int failed = 0;

        // 2. 개별 처리
        for ({Bc}Outbox outbox : unpublishedList) {
            try {
                processOutbox(outbox);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("Outbox 재시도 실패: id={}", outbox.idValue(), e);
            }
        }

        boolean hasMore = unpublishedList.size() >= BATCH_SIZE;
        return new RetryResult(unpublishedList.size(), succeeded, failed, hasMore);
    }

    /**
     * 개별 Outbox 처리
     *
     * <p>트랜잭션 단위로 처리하여 개별 실패 격리
     */
    private void processOutbox({Bc}Outbox outbox) {
        // 1. DTO 변환 (Assembler 책임)
        {Bc}Message message = assembler.toMessage(outbox);

        // 2. 외부 시스템 발행
        boolean published = publishPort.publish(message);

        if (published) {
            // 3. Domain 상태 변경
            outbox.markAsPublished();

            // 4. 저장 (TransactionManager는 persist만)
            transactionManager.persist(outbox);
        }
    }
}
```

### RetryResult Record

```java
package com.ryuqq.application.{bc}.port.in.command;

/**
 * Retry 작업 결과
 *
 * @param processed 처리된 건수
 * @param succeeded 성공 건수
 * @param failed 실패 건수
 * @param hasMore 추가 데이터 유무
 */
public record RetryResult(
        int processed,
        int succeeded,
        int failed,
        boolean hasMore
) {
    public static RetryResult empty() {
        return new RetryResult(0, 0, 0, false);
    }
}
```

---

## 6) 분산 락 (DistributedLockPort)

### Port Interface

```java
package com.ryuqq.application.common.port.out;

/**
 * Distributed Lock Port
 *
 * <p>분산 환경에서 동시 실행 방지를 위한 락
 *
 * @author development-team
 * @since 1.0.0
 */
public interface DistributedLockPort {

    /**
     * 락 획득 시도
     *
     * @param key 락 키
     * @return 획득 성공 여부
     */
    boolean tryLock(String key);

    /**
     * 락 해제
     *
     * @param key 락 키
     */
    void unlock(String key);

    /**
     * TTL과 함께 락 획득 시도
     *
     * @param key 락 키
     * @param ttlSeconds 락 유효 시간 (초)
     * @return 획득 성공 여부
     */
    boolean tryLock(String key, long ttlSeconds);
}
```

### Redis 구현체 예시

```java
@Component
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private static final long DEFAULT_TTL_SECONDS = 300;  // 5분

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryLock(String key) {
        return tryLock(key, DEFAULT_TTL_SECONDS);
    }

    @Override
    public boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "locked", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
```

---

## 7) Configuration

### application.yml

```yaml
# ===============================================
# Scheduler Configuration
# ===============================================
scheduler:
  # Outbox Retry Scheduler
  outbox-retry:
    enabled: true                    # 활성화 여부
    fixed-rate: 300000               # 5분 (밀리초)
    batch-size: 100                  # 배치 크기
    max-iterations: 10               # 최대 반복 횟수
    lock-ttl-seconds: 300            # 분산 락 TTL

  # External Download Retry Scheduler
  external-download-retry:
    enabled: true
    fixed-rate: 300000
    batch-size: 100
```

### Profile별 설정

```yaml
---
# Production 환경
spring:
  config:
    activate:
      on-profile: prod

scheduler:
  outbox-retry:
    enabled: true
    fixed-rate: 60000  # 1분 (프로덕션은 더 자주)

---
# Local 환경
spring:
  config:
    activate:
      on-profile: local

scheduler:
  outbox-retry:
    enabled: false  # 로컬에서는 비활성화
```

### SchedulerConfig

```java
package com.ryuqq.bootstrap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler Configuration
 *
 * <p>@EnableScheduling으로 스케줄러 활성화
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // @EnableScheduling만 필요
}
```

---

## 8) ECS Scheduled Task 배포

### 독립 Bootstrap 모듈

```
bootstrap/
├─ bootstrap-web-api/      ← REST API 서버
└─ bootstrap-scheduler/    ← 스케줄러 전용 (ECS Scheduled Task)
```

### bootstrap-scheduler 구조

```
bootstrap-scheduler/
├─ src/main/java/
│  └─ com/ryuqq/bootstrap/scheduler/
│      └─ SchedulerApplication.java
├─ src/main/resources/
│  └─ application.yml
├─ Dockerfile
└─ build.gradle
```

### SchedulerApplication

```java
package com.ryuqq.bootstrap.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler Bootstrap Application
 *
 * <p>ECS Scheduled Task로 배포됩니다.
 * <p>REST API 없이 Scheduler만 실행합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.ryuqq")
@EnableScheduling
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
```

### ECS Task Definition 예시

```json
{
  "family": "scheduler-task",
  "containerDefinitions": [
    {
      "name": "scheduler",
      "image": "your-ecr-repo/scheduler:latest",
      "memory": 512,
      "cpu": 256,
      "essential": true,
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/scheduler",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### CloudWatch Events Rule

```json
{
  "Name": "outbox-retry-schedule",
  "ScheduleExpression": "rate(5 minutes)",
  "State": "ENABLED",
  "Targets": [
    {
      "Id": "scheduler-task",
      "Arn": "arn:aws:ecs:ap-northeast-2:123456789:cluster/your-cluster",
      "RoleArn": "arn:aws:iam::123456789:role/ecsEventsRole",
      "EcsParameters": {
        "TaskDefinitionArn": "arn:aws:ecs:ap-northeast-2:123456789:task-definition/scheduler-task:1",
        "TaskCount": 1,
        "LaunchType": "FARGATE"
      }
    }
  ]
}
```

---

## 9) 메트릭 & 모니터링

### 필수 메트릭

| 메트릭 | 설명 | 알림 기준 |
|--------|------|----------|
| `scheduler.execution.time` | 실행 시간 | p99 > 5분 |
| `scheduler.items.processed` | 처리 건수 | - |
| `scheduler.items.succeeded` | 성공 건수 | - |
| `scheduler.items.failed` | 실패 건수 | > 10% |
| `scheduler.lock.acquired` | 락 획득 수 | - |
| `scheduler.lock.failed` | 락 실패 수 | > 50% |

### PromQL 알림 규칙

```yaml
groups:
  - name: scheduler-alerts
    rules:
      # 스케줄러 실행 시간 초과
      - alert: SchedulerExecutionSlow
        expr: |
          histogram_quantile(0.99,
            sum(rate(scheduler_execution_time_seconds_bucket[5m])) by (le, job)
          ) > 300
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "스케줄러 실행 시간 초과"

      # 스케줄러 실패율 높음
      - alert: SchedulerHighFailureRate
        expr: |
          sum(rate(scheduler_items_failed_total[5m])) by (job) /
          sum(rate(scheduler_items_processed_total[5m])) by (job) > 0.1
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "스케줄러 실패율 10% 초과"
```

---

## 10) Do / Don't

### ✅ Good

```java
// ✅ Good: UseCase 통한 비즈니스 위임
@Scheduled(fixedRate = 300000)
public void retry() {
    if (!lockPort.tryLock(LOCK_KEY)) return;
    try {
        retryUseCase.execute();  // ← UseCase 호출
    } finally {
        lockPort.unlock(LOCK_KEY);
    }
}

// ✅ Good: Service에서 비즈니스 로직 처리
@Service
public class RetryOutboxService implements RetryOutboxUseCase {
    public RetryResult execute() {
        List<Outbox> list = readManager.findUnpublished(100);
        for (Outbox outbox : list) {
            outbox.markAsPublished();          // ← Domain 메서드
            transactionManager.persist(outbox); // ← Manager는 persist만
        }
    }
}

// ✅ Good: 분산 락 사용
if (!lockPort.tryLock("scheduler:outbox-retry")) {
    log.info("Lock not acquired, skipping");
    return;
}

// ✅ Good: 최대 반복 횟수 제한
for (int i = 0; i < MAX_ITERATIONS; i++) {
    RetryResult result = useCase.execute();
    if (!result.hasMore()) break;
}

// ✅ Good: 개별 실패 격리
for (Outbox outbox : list) {
    try {
        processOutbox(outbox);  // ← 개별 try-catch
    } catch (Exception e) {
        log.warn("Outbox 처리 실패", e);
    }
}
```

### ❌ Bad

```java
// ❌ Bad: Scheduler에서 Port 직접 호출
@Scheduled(fixedRate = 300000)
public void retry() {
    List<Outbox> list = outboxQueryPort.findUnpublished(100);  // ← Port 직접!
    for (Outbox outbox : list) {
        outboxManager.markAsPublished(outbox);  // ← Manager에 비즈니스 메서드!
    }
}

// ❌ Bad: Manager에 비즈니스 메서드
public class OutboxTransactionManager {
    public void markAsPublished(Outbox outbox) {  // ← persist만 허용!
        outbox.markAsPublished();
        persistencePort.persist(outbox);
    }
}

// ❌ Bad: DTO 변환 로직이 Scheduler에 존재
private Message toMessage(Outbox outbox) {  // ← Assembler로 이동!
    return new Message(
        outbox.getId().value().toString(),
        outbox.getPayload()
    );
}

// ❌ Bad: 분산 락 없음
@Scheduled(fixedRate = 300000)
public void retry() {
    // 여러 인스턴스에서 동시 실행될 수 있음!
    useCase.execute();
}

// ❌ Bad: 무한 루프 가능성
while (!list.isEmpty()) {  // ← 최대 반복 횟수 없음!
    list = queryPort.findUnpublished(100);
}

// ❌ Bad: Law of Demeter 위반
outbox.getExternalDownloadId().value()  // ← Getter 체이닝!
```

---

## 11) 체크리스트

### Scheduler 구현 시

- [ ] `@Component` + `@ConditionalOnProperty` 적용
- [ ] 패키지: `application.{bc}.scheduler`
- [ ] **UseCase 호출만** (Port 직접 호출 금지)
- [ ] **분산 락** 사용 (DistributedLockPort)
- [ ] **최대 반복 횟수** 제한 (무한 루프 방지)
- [ ] **메트릭 기록** (실행 시간, 성공/실패율)
- [ ] Lombok 사용하지 않음
- [ ] 생성자 주입만

### UseCase 구현 시

- [ ] Port-In 인터페이스 정의
- [ ] Service에서 구현
- [ ] ReadManager로 조회 (QueryPort 직접 호출 금지)
- [ ] **Domain 상태 변경**은 Service에서
- [ ] TransactionManager는 **persist()만**
- [ ] Assembler로 DTO 변환

### 설정 시

- [ ] `application.yml`에 scheduler 설정
- [ ] Profile별 활성화/비활성화
- [ ] 분산 락 TTL 설정
- [ ] 배치 크기, 최대 반복 횟수 설정

---

## 12) 관련 문서

- **[Application Layer Guide](../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[Transaction Manager Guide](../manager/command/transaction-manager-guide.md)** - persist() 단일 메서드 규칙
- **[Event Listener Guide](../listener/event-listener-guide.md)** - 이벤트 처리 패턴
- **[DistributedLockPort Guide](../port/out/common/distributed-lock-port-guide.md)** - 분산 락 설계

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
