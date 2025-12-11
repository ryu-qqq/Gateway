# Transaction Event Registry Test Guide — **단위 테스트 및 통합 테스트**

> TransactionEventRegistry는 **트랜잭션 커밋 후 Event 발행**을 보장합니다.
>
> **단위 테스트**와 **통합 테스트** 두 가지 접근법이 필요합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | Registry 로직 검증 | TransactionEventRegistry만 |
| **통합 테스트** | 커밋 후 발행 검증 | Registry + Transaction + Listener |

### 테스트 포인트

| 항목 | 검증 내용 |
|------|----------|
| **트랜잭션 내 호출** | Event 등록 성공 |
| **트랜잭션 없이 호출** | IllegalStateException 발생 |
| **커밋 시** | Event 발행됨 |
| **롤백 시** | Event 발행 안 됨 |
| **여러 Event** | 모든 Event 발행됨 |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/common/config/
   │      └─ TransactionEventRegistry.java
   └─ test/java/
      └─ com/ryuqq/application/common/config/
          ├─ TransactionEventRegistryTest.java           # 단위 테스트
          └─ TransactionEventRegistryIntegrationTest.java # 통합 테스트
```

---

## 3) 단위 테스트 예시

### 기본 테스트

```java
package com.ryuqq.application.common.config;

import com.ryuqq.domain.common.event.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionEventRegistry 단위 테스트")
class TransactionEventRegistryTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TransactionEventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TransactionEventRegistry(eventPublisher);
    }

    @AfterEach
    void tearDown() {
        // 트랜잭션 동기화 정리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    @DisplayName("registerForPublish 테스트")
    class RegisterForPublishTest {

        @Test
        @DisplayName("트랜잭션 없이 호출하면 IllegalStateException 발생")
        void shouldThrowException_WhenNoTransaction() {
            // given
            DomainEvent event = createTestEvent();

            // when & then
            assertThatThrownBy(() -> registry.registerForPublish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction synchronization is not active");
        }

        @Test
        @DisplayName("트랜잭션 내에서 호출하면 Synchronization이 등록된다")
        void shouldRegisterSynchronization_WhenInTransaction() {
            // given
            TransactionSynchronizationManager.initSynchronization();
            DomainEvent event = createTestEvent();

            // when
            registry.registerForPublish(event);

            // then
            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
        }

        @Test
        @DisplayName("afterCommit 호출 시 Event가 발행된다")
        void shouldPublishEvent_WhenAfterCommitCalled() {
            // given
            TransactionSynchronizationManager.initSynchronization();
            DomainEvent event = createTestEvent();
            registry.registerForPublish(event);

            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

            // when (커밋 시뮬레이션)
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            // then
            verify(eventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("afterCommit 호출 전에는 Event가 발행되지 않는다")
        void shouldNotPublishEvent_BeforeAfterCommit() {
            // given
            TransactionSynchronizationManager.initSynchronization();
            DomainEvent event = createTestEvent();

            // when
            registry.registerForPublish(event);

            // then
            verify(eventPublisher, never()).publishEvent(event);
        }
    }

    @Nested
    @DisplayName("registerAllForPublish 테스트")
    class RegisterAllForPublishTest {

        @Test
        @DisplayName("null 리스트는 무시된다")
        void shouldIgnore_WhenListIsNull() {
            // given
            TransactionSynchronizationManager.initSynchronization();

            // when
            registry.registerAllForPublish(null);

            // then
            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트는 무시된다")
        void shouldIgnore_WhenListIsEmpty() {
            // given
            TransactionSynchronizationManager.initSynchronization();

            // when
            registry.registerAllForPublish(List.of());

            // then
            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).isEmpty();
        }

        @Test
        @DisplayName("여러 Event가 등록되면 각각 Synchronization이 생성된다")
        void shouldRegisterMultipleSynchronizations() {
            // given
            TransactionSynchronizationManager.initSynchronization();
            DomainEvent event1 = createTestEvent();
            DomainEvent event2 = createTestEvent();
            DomainEvent event3 = createTestEvent();

            // when
            registry.registerAllForPublish(List.of(event1, event2, event3));

            // then
            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(3);
        }

        @Test
        @DisplayName("afterCommit 시 모든 Event가 발행된다")
        void shouldPublishAllEvents_WhenAfterCommitCalled() {
            // given
            TransactionSynchronizationManager.initSynchronization();
            DomainEvent event1 = createTestEvent();
            DomainEvent event2 = createTestEvent();
            registry.registerAllForPublish(List.of(event1, event2));

            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

            // when
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            // then
            verify(eventPublisher).publishEvent(event1);
            verify(eventPublisher).publishEvent(event2);
        }
    }

    private DomainEvent createTestEvent() {
        return new TestDomainEvent();
    }

    // 테스트용 DomainEvent 구현
    private static class TestDomainEvent implements DomainEvent {
        // DomainEvent 인터페이스 구현
    }
}
```

---

## 4) 통합 테스트 예시

```java
package com.ryuqq.application.common.config;

import com.ryuqq.domain.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("TransactionEventRegistry 통합 테스트")
class TransactionEventRegistryIntegrationTest {

    @Autowired
    private TransactionEventRegistry eventRegistry;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private TestEventListener testListener;

    @Nested
    @DisplayName("트랜잭션 커밋 테스트")
    class TransactionCommitTest {

        @Test
        @DisplayName("트랜잭션 커밋 후 Event가 발행된다")
        void shouldPublishEvent_AfterTransactionCommit() {
            // given
            TestEvent event = new TestEvent("test-id");

            // when
            transactionTemplate.executeWithoutResult(status -> {
                eventRegistry.registerForPublish(event);
            });

            // then (커밋 후 리스너가 호출됨)
            verify(testListener, timeout(1000)).handleTestEvent(any(TestEvent.class));
        }

        @Test
        @DisplayName("트랜잭션 롤백 시 Event가 발행되지 않는다")
        void shouldNotPublishEvent_WhenTransactionRollback() {
            // given
            TestEvent event = new TestEvent("test-id");
            AtomicBoolean eventReceived = new AtomicBoolean(false);

            // when
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    eventRegistry.registerForPublish(event);
                    throw new RuntimeException("Rollback trigger");
                });
            } catch (RuntimeException e) {
                // 예상된 예외
            }

            // then (약간의 대기 후 확인)
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}

            assertThat(testListener.isEventReceived()).isFalse();
        }
    }

    @Nested
    @DisplayName("Event 수신 테스트")
    class EventReceiveTest {

        @Test
        @DisplayName("Listener가 커밋 후 Event를 수신한다")
        void shouldReceiveEvent_InListener() {
            // given
            String testId = "unique-" + System.currentTimeMillis();
            TestEvent event = new TestEvent(testId);

            // when
            transactionTemplate.executeWithoutResult(status -> {
                eventRegistry.registerForPublish(event);
            });

            // then
            verify(testListener, timeout(1000)).handleTestEvent(any());
        }
    }

    // 테스트용 Event
    public record TestEvent(String id) implements DomainEvent {
    }

    // 테스트용 Listener
    @Component
    public static class TestEventListener {

        private volatile boolean eventReceived = false;

        @EventListener
        public void handleTestEvent(TestEvent event) {
            eventReceived = true;
        }

        public boolean isEventReceived() {
            return eventReceived;
        }

        public void reset() {
            eventReceived = false;
        }
    }
}
```

---

## 5) 테스트 체크리스트

### 단위 테스트
- [ ] 트랜잭션 없이 호출 시 예외 발생
- [ ] 트랜잭션 내에서 Synchronization 등록
- [ ] afterCommit 시 Event 발행
- [ ] afterCommit 전에는 발행 안 됨
- [ ] null/빈 리스트 무시
- [ ] 여러 Event 각각 등록

### 통합 테스트
- [ ] 커밋 후 Event 발행
- [ ] 롤백 시 Event 미발행
- [ ] Listener에서 Event 수신

---

## 6) Do / Don't

### ✅ Good

```java
// ✅ Good: TransactionSynchronizationManager로 트랜잭션 시뮬레이션
@BeforeEach
void setUp() {
    TransactionSynchronizationManager.initSynchronization();
}

@AfterEach
void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.clearSynchronization();
    }
}

// ✅ Good: afterCommit 직접 호출로 커밋 시뮬레이션
synchronizations.forEach(TransactionSynchronization::afterCommit);

// ✅ Good: 통합 테스트에서 TransactionTemplate 사용
transactionTemplate.executeWithoutResult(status -> {
    eventRegistry.registerForPublish(event);
});
```

### ❌ Bad

```java
// ❌ Bad: 실제 트랜잭션 없이 테스트 (예외 발생)
registry.registerForPublish(event);

// ❌ Bad: afterCompletion 정리 누락
@AfterEach
void tearDown() {
    // TransactionSynchronizationManager.clearSynchronization(); 누락!
}

// ❌ Bad: 비동기 검증 없이 즉시 확인
transactionTemplate.executeWithoutResult(status -> {
    eventRegistry.registerForPublish(event);
});
verify(listener).handle(event);  // ❌ 아직 발행 안 됐을 수 있음

// ✅ 수정: timeout 사용
verify(listener, timeout(1000)).handle(event);
```

---

## 7) 관련 문서

- **[Transaction Event Registry Guide](./transaction-event-registry-guide.md)** - Registry 구현 가이드
- **[Event Listener Guide](../listener/event-listener-guide.md)** - Listener 구현 가이드
- **[Facade Guide](../facade/facade-guide.md)** - EventRegistry 사용처

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
