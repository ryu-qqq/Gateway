# Event Listener Test Guide — **단위 테스트**

> Event Listener는 **부가 작업만 처리**하므로 **단위 테스트로 충분**합니다.
>
> 이벤트 발행-수신 통합은 **TransactionEventRegistry 테스트**에서 검증합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | Listener 로직 검증 | Listener만 |
| **통합 테스트** | Event 발행-수신 검증 | Facade + Registry + Listener |

### 단위 테스트 범위

* 이벤트 수신 시 올바른 서비스 호출
* 예외 발생 시 로깅 처리 (예외 전파 안 함)
* null/빈 이벤트 처리

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/order/listener/
   │      └─ OrderEventListener.java
   └─ test/java/
      └─ com/ryuqq/application/order/listener/
          └─ OrderEventListenerTest.java
```

---

## 3) 단위 테스트 예시

### 기본 테스트

```java
package com.ryuqq.application.order.listener;

import com.ryuqq.domain.order.event.OrderPlacedEvent;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.vo.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener 단위 테스트")
class OrderEventListenerTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private MetricsService metricsService;

    private OrderEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEventListener(cacheService, metricsService);
    }

    @Nested
    @DisplayName("handleOrderPlaced 테스트")
    class HandleOrderPlacedTest {

        @Test
        @DisplayName("주문 생성 이벤트 수신 시 캐시를 무효화한다")
        void shouldEvictCacheOnOrderPlaced() {
            // given
            OrderPlacedEvent event = createOrderPlacedEvent();

            // when
            listener.handleOrderPlaced(event);

            // then
            verify(cacheService).evict("order:1");
            verify(cacheService).evict("customer-orders:100");
        }

        @Test
        @DisplayName("주문 생성 이벤트 수신 시 통계를 업데이트한다")
        void shouldUpdateMetricsOnOrderPlaced() {
            // given
            OrderPlacedEvent event = createOrderPlacedEvent();

            // when
            listener.handleOrderPlaced(event);

            // then
            verify(metricsService).incrementOrderCount();
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 예외를 전파하지 않는다")
        void shouldNotPropagateExceptionOnCacheFailure() {
            // given
            OrderPlacedEvent event = createOrderPlacedEvent();
            doThrow(new RuntimeException("Cache error"))
                .when(cacheService).evict(anyString());

            // when & then
            assertThatCode(() -> listener.handleOrderPlaced(event))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("통계 업데이트 실패 시 예외를 전파하지 않는다")
        void shouldNotPropagateExceptionOnMetricsFailure() {
            // given
            OrderPlacedEvent event = createOrderPlacedEvent();
            doThrow(new RuntimeException("Metrics error"))
                .when(metricsService).incrementOrderCount();

            // when & then
            assertThatCode(() -> listener.handleOrderPlaced(event))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("캐시 실패해도 통계 업데이트는 시도한다")
        void shouldContinueToMetricsEvenIfCacheFails() {
            // given
            OrderPlacedEvent event = createOrderPlacedEvent();
            doThrow(new RuntimeException("Cache error"))
                .when(cacheService).evict(anyString());

            // when
            listener.handleOrderPlaced(event);

            // then
            verify(metricsService).incrementOrderCount();
        }

        private OrderPlacedEvent createOrderPlacedEvent() {
            return new OrderPlacedEvent(
                new OrderId(1L),
                new CustomerId(100L)
            );
        }
    }
}
```

### safeExecute 헬퍼 테스트

```java
@Nested
@DisplayName("안전한 실행 헬퍼 테스트")
class SafeExecuteTest {

    @Test
    @DisplayName("정상 실행 시 작업이 완료된다")
    void shouldCompleteTaskOnSuccess() {
        // given
        OrderPlacedEvent event = createOrderPlacedEvent();

        // when
        listener.handleOrderPlaced(event);

        // then
        verify(cacheService).evict("order:1");
    }

    @Test
    @DisplayName("예외 발생 시 로그만 남기고 계속 진행한다")
    void shouldLogAndContinueOnException() {
        // given
        OrderPlacedEvent event = createOrderPlacedEvent();
        doThrow(new RuntimeException("Error"))
            .when(cacheService).evict(anyString());

        // when & then
        assertThatCode(() -> listener.handleOrderPlaced(event))
            .doesNotThrowAnyException();

        // 다른 작업은 계속 시도됨
        verify(metricsService).incrementOrderCount();
    }
}
```

---

## 4) 통합 테스트 (이벤트 발행-수신)

```java
package com.ryuqq.application.order.listener;

import com.ryuqq.application.common.config.TransactionEventRegistry;
import com.ryuqq.domain.order.event.OrderPlacedEvent;
import com.ryuqq.domain.order.vo.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("Event 발행-수신 통합 테스트")
class OrderEventIntegrationTest {

    @Autowired
    private TransactionEventRegistry eventRegistry;

    @MockBean
    private CacheService cacheService;

    @MockBean
    private MetricsService metricsService;

    @Test
    @Transactional
    @DisplayName("트랜잭션 커밋 후 이벤트가 리스너에 전달된다")
    void shouldDeliverEventAfterCommit() {
        // given
        OrderPlacedEvent event = new OrderPlacedEvent(new OrderId(1L));

        // when
        eventRegistry.registerForPublish(event);
        // 트랜잭션 커밋 시 이벤트 발행

        // then (커밋 후 비동기 처리 대기)
        verify(cacheService, timeout(1000)).evict("order:1");
    }
}
```

---

## 5) 테스트 체크리스트

### 단위 테스트
- [ ] 이벤트 수신 시 올바른 서비스 호출
- [ ] 예외 발생 시 로깅 처리 (전파 안 함)
- [ ] 한 작업 실패해도 다른 작업 시도
- [ ] null/빈 이벤트 처리

### 통합 테스트 (선택)
- [ ] 커밋 후 이벤트 발행 확인
- [ ] 리스너까지 이벤트 전달 확인

---

## 6) Do / Don't

### ✅ Good

```java
// ✅ Good: 예외 전파 안 함 검증
assertThatCode(() -> listener.handleOrderPlaced(event))
    .doesNotThrowAnyException();

// ✅ Good: 실패 후에도 다른 작업 시도 검증
doThrow(new RuntimeException()).when(cacheService).evict(anyString());
listener.handleOrderPlaced(event);
verify(metricsService).incrementOrderCount();  // 여전히 호출됨
```

### ❌ Bad

```java
// ❌ Bad: 예외 전파 테스트 (리스너는 예외 전파 안 함)
assertThatThrownBy(() -> listener.handleOrderPlaced(event))
    .isInstanceOf(RuntimeException.class);

// ❌ Bad: 외부 시스템 호출 테스트 (Outbox로 처리해야 함)
verify(externalApiClient).send(any());
```

---

## 7) 관련 문서

- **[Event Listener Guide](./event-listener-guide.md)** - Listener 구현 가이드
- **[Event Listener ArchUnit](./event-listener-archunit.md)** - ArchUnit 검증 규칙
- **[Transaction Event Registry Guide](../event/transaction-event-registry-guide.md)** - 커밋 후 Event 발행

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
