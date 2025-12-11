# Facade Test Guide — **단위 테스트**

> Facade는 **여러 Transaction Manager 조합**을 담당합니다.
>
> **순수 조율 로직**이므로 **Mock 기반 단위 테스트**를 작성합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | Manager 조합 검증 | Facade만 (Mock Manager) |

### 테스트 포인트

| 항목 | 검증 내용 |
|------|----------|
| **Manager 호출 순서** | 올바른 순서로 Manager 호출 |
| **Manager 조합** | 2개 이상 Manager 조합 |
| **반환값 전달** | 첫 번째 Manager 결과 반환 |
| **ID Enrichment** | 영속화 후 ID 전달 |
| **호출 횟수** | 각 Manager 정확히 1번 호출 |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/facade/
   │      └─ {Bc}Facade.java
   └─ test/java/
      └─ com/ryuqq/application/{bc}/facade/
          └─ {Bc}FacadeTest.java
```

---

## 3) 단위 테스트 예시

### 기본 테스트

```java
package com.ryuqq.application.order.facade;

import com.ryuqq.application.order.dto.bundle.OrderPersistBundle;
import com.ryuqq.application.order.manager.OrderTransactionManager;
import com.ryuqq.application.order.manager.OrderHistoryTransactionManager;
import com.ryuqq.application.outbox.manager.OutboxTransactionManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderHistory;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@Tag("unit")
@Tag("facade")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFacade 단위 테스트")
class OrderFacadeTest {

    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private OrderHistoryTransactionManager historyManager;

    @Mock
    private OutboxTransactionManager outboxManager;

    @InjectMocks
    private OrderFacade facade;

    @Nested
    @DisplayName("persistOrderWithHistoryAndOutbox 테스트")
    class PersistOrderWithHistoryAndOutboxTest {

        @Test
        @DisplayName("여러 Manager를 올바른 순서로 호출해야 한다")
        void shouldCallManagersInOrder() {
            // given
            Order order = createTestOrder();
            OrderHistory history = createTestHistory();
            OutboxEvent outboxEvent = createTestOutboxEvent();

            Order savedOrder = createSavedOrder(1L);
            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            facade.persistOrderWithHistoryAndOutbox(order, history, outboxEvent);

            // then - 호출 순서 검증
            InOrder inOrder = inOrder(orderManager, historyManager, outboxManager);
            inOrder.verify(orderManager).persist(order);
            inOrder.verify(historyManager).persist(any(OrderHistory.class));
            inOrder.verify(outboxManager).persist(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("OrderManager의 결과를 반환해야 한다")
        void shouldReturnOrderManagerResult() {
            // given
            Order order = createTestOrder();
            OrderHistory history = createTestHistory();
            OutboxEvent outboxEvent = createTestOutboxEvent();

            Order savedOrder = createSavedOrder(1L);
            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            Order result = facade.persistOrderWithHistoryAndOutbox(order, history, outboxEvent);

            // then
            assertThat(result).isEqualTo(savedOrder);
            assertThat(result.id().value()).isEqualTo(1L);
        }

        @Test
        @DisplayName("각 Manager를 정확히 1번씩 호출해야 한다")
        void shouldCallEachManagerOnce() {
            // given
            Order order = createTestOrder();
            OrderHistory history = createTestHistory();
            OutboxEvent outboxEvent = createTestOutboxEvent();

            Order savedOrder = createSavedOrder(1L);
            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            facade.persistOrderWithHistoryAndOutbox(order, history, outboxEvent);

            // then
            then(orderManager).should(times(1)).persist(order);
            then(historyManager).should(times(1)).persist(any(OrderHistory.class));
            then(outboxManager).should(times(1)).persist(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("저장된 Order의 ID를 History에 전달해야 한다")
        void shouldPassOrderIdToHistory() {
            // given
            Order order = createTestOrder();
            OrderHistory history = createTestHistory();
            OutboxEvent outboxEvent = createTestOutboxEvent();

            Order savedOrder = createSavedOrder(1L);
            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            facade.persistOrderWithHistoryAndOutbox(order, history, outboxEvent);

            // then
            then(historyManager).should().persist(argThat(h ->
                h.getOrderId().value().equals(1L)
            ));
        }

        @Test
        @DisplayName("저장된 Order의 ID를 OutboxEvent에 전달해야 한다")
        void shouldPassOrderIdToOutboxEvent() {
            // given
            Order order = createTestOrder();
            OrderHistory history = createTestHistory();
            OutboxEvent outboxEvent = createTestOutboxEvent();

            Order savedOrder = createSavedOrder(1L);
            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            facade.persistOrderWithHistoryAndOutbox(order, history, outboxEvent);

            // then
            then(outboxManager).should().persist(argThat(event ->
                event.getAggregateId().equals(1L)
            ));
        }
    }

    @Nested
    @DisplayName("persistOrderBundle 테스트")
    class PersistOrderBundleTest {

        @Test
        @DisplayName("Bundle의 Order를 영속화하고 ID를 할당해야 한다")
        void shouldPersistBundleAndAssignId() {
            // given
            OrderPersistBundle bundle = createTestBundle();
            Order savedOrder = createSavedOrder(1L);

            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            Order result = facade.persistOrderBundle(bundle);

            // then
            assertThat(result.id().value()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Bundle Enrichment 후 관련 객체를 영속화해야 한다")
        void shouldEnrichBundleAndPersist() {
            // given
            OrderPersistBundle bundle = createTestBundle();
            Order savedOrder = createSavedOrder(1L);

            given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

            // when
            facade.persistOrderBundle(bundle);

            // then
            then(orderManager).should().persist(bundle.order());
            then(historyManager).should().persist(any(OrderHistory.class));
            then(outboxManager).should().persist(argThat(event ->
                event.getAggregateId().equals(1L)
            ));
        }
    }

    // ==================== Helper Methods ====================

    private Order createTestOrder() {
        // 테스트용 Order 생성 (ID 없음)
        return Order.forNew(/* ... */);
    }

    private Order createSavedOrder(Long id) {
        // 저장된 Order (ID 있음)
        return Order.forExisting(new OrderId(id), /* ... */);
    }

    private OrderHistory createTestHistory() {
        return OrderHistory.forNew(/* ... */);
    }

    private OutboxEvent createTestOutboxEvent() {
        return OutboxEvent.forNew("Order", null, "OrderPlaced", "{}");
    }

    private OrderPersistBundle createTestBundle() {
        return new OrderPersistBundle(
            createTestOrder(),
            createTestHistory(),
            createTestOutboxEvent()
        );
    }
}
```

---

## 4) 테스트 체크리스트

### Manager 조합 검증
- [ ] 2개 이상 Manager Mock 주입
- [ ] 올바른 호출 순서 (InOrder)
- [ ] 각 Manager 정확히 1번 호출

### ID Enrichment 검증
- [ ] 첫 번째 Manager 결과의 ID 획득
- [ ] 관련 객체에 ID 전달 (History, OutboxEvent)
- [ ] Bundle enrichWithId() 호출 검증

### 반환값 검증
- [ ] 첫 번째 Manager 결과 반환
- [ ] ID가 할당된 Domain 반환

### 금지 사항 검증
- [ ] 비즈니스 로직 없음
- [ ] 객체 생성 없음 (Factory 책임)

---

## 5) Do / Don't

### ✅ Good

```java
// ✅ Good: Mock 기반 단위 테스트
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {
    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private OutboxTransactionManager outboxManager;

    @InjectMocks
    private OrderFacade facade;
}

// ✅ Good: 호출 순서 검증
InOrder inOrder = inOrder(orderManager, historyManager, outboxManager);
inOrder.verify(orderManager).persist(order);
inOrder.verify(historyManager).persist(any());
inOrder.verify(outboxManager).persist(any());

// ✅ Good: ID Enrichment 검증
then(outboxManager).should().persist(argThat(event ->
    event.getAggregateId().equals(1L)
));

// ✅ Good: persist* 메서드 테스트
facade.persistOrderBundle(bundle);
facade.persistOrderWithHistoryAndOutbox(order, history, event);
```

### ❌ Bad

```java
// ❌ Bad: Spring Context 로딩
@SpringBootTest
class OrderFacadeTest { ... }

// ❌ Bad: 실제 Manager 사용
private OrderTransactionManager orderManager = new OrderTransactionManager(...);

// ❌ Bad: 비즈니스 로직 테스트 (Domain Test로)
order.place();
facade.persistOrderBundle(bundle);

// ❌ Bad: 트랜잭션 실제 동작 테스트 (Integration Test로)
@Transactional
void persistOrder_ShouldRollbackOnException() { ... }

// ❌ Bad: 단일 Manager만 테스트 (Facade 필요 없음)
facade.persistOrder(order);  // Manager 직접 호출해야 함

// ❌ Bad: save* 메서드명 (persist* 사용)
facade.saveOrderWithOutbox(order, event);  // ❌
```

---

## 6) Fixture 활용

```java
import com.ryuqq.fixture.domain.OrderFixture;
import com.ryuqq.fixture.domain.OrderHistoryFixture;
import com.ryuqq.fixture.application.OrderPersistBundleFixture;

@DisplayName("OrderFacade 단위 테스트")
class OrderFacadeTest {

    @Test
    @DisplayName("Bundle을 영속화한다")
    void shouldPersistBundle() {
        // given
        OrderPersistBundle bundle = OrderPersistBundleFixture.create();
        Order savedOrder = OrderFixture.withId(1L);

        given(orderManager.persist(any(Order.class))).willReturn(savedOrder);

        // when
        Order result = facade.persistOrderBundle(bundle);

        // then
        assertThat(result.id().value()).isEqualTo(1L);
    }
}
```

---

## 7) Integration Test와의 관계

| 구분 | 단위 테스트 (이 문서) | 통합 테스트 |
|------|----------------------|-------------|
| **목적** | Manager 조합 검증 | 트랜잭션 동작 검증 |
| **방식** | Mock 기반 | 실제 DB 사용 |
| **속도** | 밀리초 | 초 단위 |
| **검증 항목** | 호출 순서, ID 전달 | Commit/Rollback |

---

## 8) 관련 문서

- **[Facade Guide](./facade-guide.md)** - Facade 구현 가이드
- **[Facade ArchUnit](./facade-archunit.md)** - 아키텍처 규칙
- **[Transaction Manager Test Guide](../manager/transaction-manager-test-guide.md)** - Manager 테스트

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0
