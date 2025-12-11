# Scheduler Test Guide — **단위 테스트 및 통합 테스트**

> Scheduler의 단위 테스트와 통합 테스트 가이드입니다.
>
> **UseCase Mock**, **분산 락 Mock**, **메트릭 검증** 등을 다룹니다.

---

## 1) 테스트 전략

| 테스트 유형 | 대상 | 목적 |
|------------|------|------|
| **단위 테스트** | Scheduler | UseCase 호출, 락 처리, 메트릭 검증 |
| **통합 테스트** | Scheduler + UseCase | 실제 흐름 검증 |
| **ArchUnit** | 아키텍처 규칙 | 의존성, 네이밍 검증 |

---

## 2) 단위 테스트 (Scheduler)

### 기본 구조

```java
package com.ryuqq.application.{bc}.scheduler;

import com.ryuqq.application.{bc}.port.in.command.Retry{Bc}UseCase;
import com.ryuqq.application.{bc}.port.in.command.RetryResult;
import com.ryuqq.application.common.port.out.DistributedLockPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {Bc} Retry Scheduler 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("{Bc}RetryScheduler 단위 테스트")
class {Bc}RetrySchedulerTest {

    @Mock
    private Retry{Bc}UseCase retryUseCase;

    @Mock
    private DistributedLockPort lockPort;

    private MeterRegistry meterRegistry;
    private {Bc}RetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new {Bc}RetryScheduler(retryUseCase, lockPort, meterRegistry);
    }

    // =========================================================================
    // 1. 분산 락 테스트
    // =========================================================================

    @Nested
    @DisplayName("분산 락 테스트")
    class DistributedLockTests {

        @Test
        @DisplayName("락 획득 실패 시 UseCase를 호출하지 않아야 한다")
        void should_not_call_usecase_when_lock_not_acquired() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(false);

            // when
            scheduler.retry();

            // then
            verify(retryUseCase, never()).execute();
            verify(lockPort, never()).unlock(anyString());
        }

        @Test
        @DisplayName("락 획득 성공 시 UseCase를 호출해야 한다")
        void should_call_usecase_when_lock_acquired() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute()).willReturn(RetryResult.empty());

            // when
            scheduler.retry();

            // then
            verify(retryUseCase).execute();
            verify(lockPort).unlock(anyString());
        }

        @Test
        @DisplayName("예외 발생 시에도 락을 해제해야 한다")
        void should_unlock_even_when_exception_occurs() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute()).willThrow(new RuntimeException("Test error"));

            // when & then
            assertThatThrownBy(() -> scheduler.retry())
                    .isInstanceOf(RuntimeException.class);

            verify(lockPort).unlock(anyString());
        }
    }

    // =========================================================================
    // 2. UseCase 호출 테스트
    // =========================================================================

    @Nested
    @DisplayName("UseCase 호출 테스트")
    class UseCaseInvocationTests {

        @Test
        @DisplayName("UseCase 결과에 따라 반복 호출해야 한다")
        void should_iterate_while_has_more_data() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute())
                    .willReturn(new RetryResult(100, 100, 0, true))   // 1회차: 더 있음
                    .willReturn(new RetryResult(50, 50, 0, false));   // 2회차: 끝

            // when
            scheduler.retry();

            // then
            verify(retryUseCase, times(2)).execute();
        }

        @Test
        @DisplayName("더 이상 데이터가 없으면 반복을 멈춰야 한다")
        void should_stop_when_no_more_data() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute())
                    .willReturn(new RetryResult(10, 10, 0, false));  // 더 이상 없음

            // when
            scheduler.retry();

            // then
            verify(retryUseCase, times(1)).execute();
        }

        @Test
        @DisplayName("최대 반복 횟수를 초과하면 멈춰야 한다")
        void should_stop_at_max_iterations() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute())
                    .willReturn(new RetryResult(100, 100, 0, true));  // 항상 더 있음

            // when
            scheduler.retry();

            // then
            // MAX_ITERATIONS (10회) 까지만 호출
            verify(retryUseCase, times(10)).execute();
        }
    }

    // =========================================================================
    // 3. 메트릭 테스트
    // =========================================================================

    @Nested
    @DisplayName("메트릭 테스트")
    class MetricsTests {

        @Test
        @DisplayName("성공 시 메트릭을 기록해야 한다")
        void should_record_metrics_on_success() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute())
                    .willReturn(new RetryResult(100, 90, 10, false));

            // when
            scheduler.retry();

            // then
            assertThat(meterRegistry.get("scheduler.execution.time")
                    .tag("job", "{bc}-retry")
                    .tag("status", "success")
                    .timer().count()).isEqualTo(1);

            assertThat(meterRegistry.get("scheduler.items.processed")
                    .tag("job", "{bc}-retry")
                    .counter().count()).isEqualTo(100);

            assertThat(meterRegistry.get("scheduler.items.succeeded")
                    .tag("job", "{bc}-retry")
                    .counter().count()).isEqualTo(90);

            assertThat(meterRegistry.get("scheduler.items.failed")
                    .tag("job", "{bc}-retry")
                    .counter().count()).isEqualTo(10);
        }

        @Test
        @DisplayName("실패 시 failure 메트릭을 기록해야 한다")
        void should_record_failure_metric_on_exception() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute()).willThrow(new RuntimeException("Test error"));

            // when
            try {
                scheduler.retry();
            } catch (RuntimeException ignored) {
            }

            // then
            assertThat(meterRegistry.get("scheduler.execution.time")
                    .tag("job", "{bc}-retry")
                    .tag("status", "failure")
                    .timer().count()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 4. 빈 결과 테스트
    // =========================================================================

    @Nested
    @DisplayName("빈 결과 테스트")
    class EmptyResultTests {

        @Test
        @DisplayName("처리할 데이터가 없으면 정상 종료해야 한다")
        void should_complete_successfully_when_no_data() {
            // given
            given(lockPort.tryLock(anyString())).willReturn(true);
            given(retryUseCase.execute()).willReturn(RetryResult.empty());

            // when
            scheduler.retry();

            // then
            verify(retryUseCase, times(1)).execute();
            verify(lockPort).unlock(anyString());
        }
    }
}
```

---

## 3) 테스트 케이스 체크리스트

### 분산 락 관련

- [ ] 락 획득 실패 시 UseCase 미호출
- [ ] 락 획득 성공 시 UseCase 호출
- [ ] 예외 발생 시에도 락 해제
- [ ] 락 키 값 검증

### UseCase 호출 관련

- [ ] 정상 호출 검증
- [ ] 반복 호출 (hasMore=true 시)
- [ ] 반복 중단 (hasMore=false 시)
- [ ] 최대 반복 횟수 제한

### 메트릭 관련

- [ ] 성공 시 메트릭 기록
- [ ] 실패 시 메트릭 기록
- [ ] 처리 건수 합산

### 예외 처리 관련

- [ ] UseCase 예외 시 전파
- [ ] 락 예외 시 처리

---

## 4) UseCase 단위 테스트

```java
package com.ryuqq.application.{bc}.service.command;

import com.ryuqq.application.{bc}.assembler.{Bc}Assembler;
import com.ryuqq.application.{bc}.manager.command.{Bc}OutboxTransactionManager;
import com.ryuqq.application.{bc}.manager.query.{Bc}OutboxReadManager;
import com.ryuqq.application.{bc}.port.in.command.RetryResult;
import com.ryuqq.application.{bc}.port.out.{Bc}PublishPort;
import com.ryuqq.domain.{bc}.{Bc}Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Retry{Bc}Service 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Retry{Bc}Service 단위 테스트")
class Retry{Bc}ServiceTest {

    @Mock
    private {Bc}OutboxReadManager readManager;

    @Mock
    private {Bc}OutboxTransactionManager transactionManager;

    @Mock
    private {Bc}PublishPort publishPort;

    @Mock
    private {Bc}Assembler assembler;

    private Retry{Bc}Service service;

    @BeforeEach
    void setUp() {
        service = new Retry{Bc}Service(
                readManager, transactionManager, publishPort, assembler);
    }

    // =========================================================================
    // 1. 빈 결과 테스트
    // =========================================================================

    @Nested
    @DisplayName("빈 결과 테스트")
    class EmptyResultTests {

        @Test
        @DisplayName("미발행 건이 없으면 빈 결과를 반환해야 한다")
        void should_return_empty_when_no_unpublished() {
            // given
            given(readManager.findUnpublished(anyInt())).willReturn(Collections.emptyList());

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.processed()).isZero();
            assertThat(result.succeeded()).isZero();
            assertThat(result.failed()).isZero();
            assertThat(result.hasMore()).isFalse();

            verify(publishPort, never()).publish(any());
            verify(transactionManager, never()).persist(any());
        }
    }

    // =========================================================================
    // 2. 정상 처리 테스트
    // =========================================================================

    @Nested
    @DisplayName("정상 처리 테스트")
    class SuccessProcessingTests {

        @Test
        @DisplayName("발행 성공 시 상태를 변경하고 저장해야 한다")
        void should_mark_as_published_and_persist_when_publish_succeeds() {
            // given
            {Bc}Outbox outbox = mock({Bc}Outbox.class);
            given(readManager.findUnpublished(anyInt())).willReturn(List.of(outbox));
            given(assembler.toMessage(any())).willReturn(mock({Bc}Message.class));
            given(publishPort.publish(any())).willReturn(true);

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.processed()).isEqualTo(1);
            assertThat(result.succeeded()).isEqualTo(1);
            assertThat(result.failed()).isZero();

            verify(outbox).markAsPublished();
            verify(transactionManager).persist(outbox);
        }

        @Test
        @DisplayName("발행 실패 시 상태를 변경하지 않아야 한다")
        void should_not_mark_as_published_when_publish_fails() {
            // given
            {Bc}Outbox outbox = mock({Bc}Outbox.class);
            given(readManager.findUnpublished(anyInt())).willReturn(List.of(outbox));
            given(assembler.toMessage(any())).willReturn(mock({Bc}Message.class));
            given(publishPort.publish(any())).willReturn(false);

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.processed()).isEqualTo(1);
            assertThat(result.succeeded()).isZero();
            assertThat(result.failed()).isEqualTo(1);

            verify(outbox, never()).markAsPublished();
            verify(transactionManager, never()).persist(any());
        }
    }

    // =========================================================================
    // 3. 개별 실패 격리 테스트
    // =========================================================================

    @Nested
    @DisplayName("개별 실패 격리 테스트")
    class IndividualFailureIsolationTests {

        @Test
        @DisplayName("하나가 실패해도 나머지는 처리해야 한다")
        void should_continue_processing_when_one_fails() {
            // given
            {Bc}Outbox outbox1 = mock({Bc}Outbox.class);
            {Bc}Outbox outbox2 = mock({Bc}Outbox.class);
            {Bc}Outbox outbox3 = mock({Bc}Outbox.class);

            given(readManager.findUnpublished(anyInt()))
                    .willReturn(List.of(outbox1, outbox2, outbox3));

            given(assembler.toMessage(outbox1)).willThrow(new RuntimeException("Error"));
            given(assembler.toMessage(outbox2)).willReturn(mock({Bc}Message.class));
            given(assembler.toMessage(outbox3)).willReturn(mock({Bc}Message.class));

            given(publishPort.publish(any())).willReturn(true);

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.processed()).isEqualTo(3);
            assertThat(result.succeeded()).isEqualTo(2);
            assertThat(result.failed()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 4. hasMore 테스트
    // =========================================================================

    @Nested
    @DisplayName("hasMore 테스트")
    class HasMoreTests {

        @Test
        @DisplayName("배치 크기만큼 데이터가 있으면 hasMore=true")
        void should_return_has_more_true_when_batch_size_reached() {
            // given
            List<{Bc}Outbox> outboxList = mock(List.class);
            given(outboxList.size()).willReturn(100);  // BATCH_SIZE
            given(outboxList.isEmpty()).willReturn(false);
            given(outboxList.iterator()).willReturn(Collections.emptyIterator());

            given(readManager.findUnpublished(anyInt())).willReturn(outboxList);

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.hasMore()).isTrue();
        }

        @Test
        @DisplayName("배치 크기보다 적으면 hasMore=false")
        void should_return_has_more_false_when_less_than_batch_size() {
            // given
            {Bc}Outbox outbox = mock({Bc}Outbox.class);
            given(readManager.findUnpublished(anyInt())).willReturn(List.of(outbox));
            given(assembler.toMessage(any())).willReturn(mock({Bc}Message.class));
            given(publishPort.publish(any())).willReturn(true);

            // when
            RetryResult result = service.execute();

            // then
            assertThat(result.hasMore()).isFalse();
        }
    }
}
```

---

## 5) 통합 테스트 (선택적)

```java
package com.ryuqq.application.{bc}.scheduler;

import com.ryuqq.application.common.port.out.DistributedLockPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {Bc}RetryScheduler 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "scheduler.{bc}-retry.enabled=true"
})
@DisplayName("{Bc}RetryScheduler 통합 테스트")
class {Bc}RetrySchedulerIntegrationTest {

    @Autowired
    private {Bc}RetryScheduler scheduler;

    @MockBean
    private DistributedLockPort lockPort;

    @Test
    @DisplayName("전체 흐름이 정상 작동해야 한다")
    void should_work_end_to_end() {
        // given
        given(lockPort.tryLock(anyString())).willReturn(true);

        // when
        scheduler.retry();

        // then
        verify(lockPort).unlock(anyString());
    }
}
```

---

## 6) 테스트 Fixture

```java
package com.ryuqq.fixture.application;

import com.ryuqq.application.{bc}.port.in.command.RetryResult;

/**
 * RetryResult Fixture
 */
public final class RetryResultFixture {

    private RetryResultFixture() {}

    public static RetryResult empty() {
        return RetryResult.empty();
    }

    public static RetryResult success(int count) {
        return new RetryResult(count, count, 0, false);
    }

    public static RetryResult partial(int processed, int succeeded, int failed) {
        return new RetryResult(processed, succeeded, failed, false);
    }

    public static RetryResult hasMore(int processed) {
        return new RetryResult(processed, processed, 0, true);
    }
}
```

---

## 7) 체크리스트

### Scheduler 테스트

- [ ] 분산 락 획득/해제 검증
- [ ] UseCase 호출 검증
- [ ] 반복 로직 검증 (hasMore)
- [ ] 최대 반복 횟수 검증
- [ ] 메트릭 기록 검증
- [ ] 예외 시 락 해제 검증

### UseCase 테스트

- [ ] 빈 결과 처리
- [ ] 정상 발행 및 상태 변경
- [ ] 발행 실패 시 상태 미변경
- [ ] 개별 실패 격리
- [ ] hasMore 로직

### 공통

- [ ] Mock 적절히 사용
- [ ] 테스트 격리
- [ ] BDD 스타일 (given/when/then)
- [ ] DisplayName 작성

---

## 📖 관련 문서

- **[Scheduler Guide](./scheduler-guide.md)** - Scheduler 구현 가이드
- **[Scheduler ArchUnit Guide](./scheduler-archunit.md)** - ArchUnit 검증
- **[UseCase Test Guide](../service/command/command-service-test-guide.md)** - UseCase 테스트

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
