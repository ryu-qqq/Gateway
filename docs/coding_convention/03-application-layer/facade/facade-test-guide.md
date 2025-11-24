# Facade 테스트 가이드

> **목적**: Facade의 단위 테스트 전략 (Mock 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
Facade는 **여러 Transaction Manager 조합**만 검증합니다:

```
✅ 테스트 항목:
1. 여러 Manager 호출 순서 검증
2. Manager 조합 로직 검증
3. 트랜잭션 조율 검증
4. 반환값 전달 검증
5. 비즈니스 로직 없음 검증
```

### 테스트 범위
- ✅ 단위 테스트 (Mock 사용)
- ✅ Manager 호출 위임 검증
- ✅ 호출 순서 검증
- ✅ 빠른 실행 (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ 비즈니스 로직 테스트 금지 (Domain Test로 분리)
- ❌ 트랜잭션 실제 동작 테스트 금지 (Integration Test로)

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.application.{bc}.facade;

import com.ryuqq.application.{bc}.manager.{Bc}TransactionManager;
import com.ryuqq.application.outbox.manager.OutboxTransactionManager;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

/**
 * {Bc} Facade 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("facade")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
@DisplayName("{Bc} Facade 단위 테스트")
class {Bc}FacadeTest {

    @Mock
    private {Bc}TransactionManager {bc}Manager;

    @Mock
    private OutboxTransactionManager outboxManager;

    @InjectMocks
    private {Bc}Facade facade;

    @Test
    @DisplayName("여러 Manager를 올바른 순서로 호출해야 한다")
    void saveWithOutbox_ShouldCallManagersInOrder() {
        // Given
        {Bc} {bc} = {Bc}.forNew(/* domain fields */);
        {Bc} saved{Bc} = {Bc}.forExisting({Bc}Id.of(1L), /* fields */);

        given({bc}Manager.save(any({Bc}.class)))
            .willReturn(saved{Bc});

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        facade.saveWithOutbox({bc}, "EventType");

        // Then - 호출 순서 검증
        InOrder inOrder = inOrder({bc}Manager, outboxManager);
        inOrder.verify({bc}Manager).save({bc});
        inOrder.verify(outboxManager).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("첫 번째 Manager의 결과를 반환해야 한다")
    void saveWithOutbox_ShouldReturnFirstManagerResult() {
        // Given
        {Bc} {bc} = {Bc}.forNew(/* domain fields */);
        {Bc} saved{Bc} = {Bc}.forExisting({Bc}Id.of(1L), /* fields */);

        given({bc}Manager.save(any({Bc}.class)))
            .willReturn(saved{Bc});

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        {Bc} result = facade.saveWithOutbox({bc}, "EventType");

        // Then
        assertThat(result).isEqualTo(saved{Bc});
        assertThat(result.getIdValue()).isEqualTo(1L);
    }
}
```

---

## 3️⃣ 실전 예시 (Order + Outbox)

```java
package com.ryuqq.application.order.facade;

import com.ryuqq.application.order.manager.OrderTransactionManager;
import com.ryuqq.application.outbox.manager.OutboxTransactionManager;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.Money;
import com.ryuqq.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

/**
 * Order Facade 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("facade")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Facade 단위 테스트")
class OrderFacadeTest {

    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private OutboxTransactionManager outboxManager;

    @InjectMocks
    private OrderFacade facade;

    @Test
    @DisplayName("여러 Manager를 올바른 순서로 호출해야 한다")
    void saveOrderWithOutbox_ShouldCallManagersInOrder() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        Order savedOrder = Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(orderManager.save(any(Order.class)))
            .willReturn(savedOrder);

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        facade.saveOrderWithOutbox(order, "OrderCreated");

        // Then - 호출 순서 검증
        InOrder inOrder = inOrder(orderManager, outboxManager);
        inOrder.verify(orderManager).save(order);
        inOrder.verify(outboxManager).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("OrderManager의 결과를 반환해야 한다")
    void saveOrderWithOutbox_ShouldReturnOrderManagerResult() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        Order savedOrder = Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(orderManager.save(any(Order.class)))
            .willReturn(savedOrder);

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        Order result = facade.saveOrderWithOutbox(order, "OrderCreated");

        // Then
        assertThat(result).isEqualTo(savedOrder);
        assertThat(result.getIdValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("OrderManager와 OutboxManager를 정확히 1번씩 호출해야 한다")
    void saveOrderWithOutbox_ShouldCallEachManagerOnce() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        Order savedOrder = Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(orderManager.save(any(Order.class)))
            .willReturn(savedOrder);

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        facade.saveOrderWithOutbox(order, "OrderCreated");

        // Then
        then(orderManager).should(times(1)).save(order);
        then(outboxManager).should(times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("저장된 Order의 ID를 사용하여 OutboxEvent를 생성해야 한다")
    void saveOrderWithOutbox_ShouldUseOrderIdForOutboxEvent() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        Order savedOrder = Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(orderManager.save(any(Order.class)))
            .willReturn(savedOrder);

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        facade.saveOrderWithOutbox(order, "OrderCreated");

        // Then
        then(outboxManager).should().save(argThat(event ->
            event.getAggregateId().equals(1L) &&
            event.getEventType().equals("OrderCreated")
        ));
    }

    @Test
    @DisplayName("비즈니스 로직 없이 Manager 호출만 해야 한다")
    void saveOrderWithOutbox_ShouldNotContainBusinessLogic() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(orderManager.save(any(Order.class)))
            .willReturn(order);

        given(outboxManager.save(any(OutboxEvent.class)))
            .willReturn(any(OutboxEvent.class));

        // When
        facade.saveOrderWithOutbox(order, "OrderCreated");

        // Then
        // ✅ Manager 호출만 검증 (비즈니스 로직 없음)
        then(orderManager).should(times(1)).save(order);
        then(outboxManager).should(times(1)).save(any(OutboxEvent.class));
        then(orderManager).shouldHaveNoMoreInteractions();
        then(outboxManager).shouldHaveNoMoreInteractions();
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩
@SpringBootTest
class OrderFacadeTest {
    // Spring Context 로딩 불필요! (단위 테스트)
}

// ❌ 실제 Manager 사용
class OrderFacadeTest {
    private OrderTransactionManager orderManager = new OrderTransactionManager(...);
    // Mock 사용해야 함!
}

// ❌ 비즈니스 로직 테스트
@Test
void saveOrderWithOutbox_WithBusinessLogic() {
    Order order = Order.forNew(...);
    order.place();  // 비즈니스 로직은 Domain Test로!

    facade.saveOrderWithOutbox(order, "OrderCreated");
}

// ❌ 트랜잭션 실제 동작 테스트
@Test
@Transactional
void saveOrderWithOutbox_ShouldRollbackOnException() {
    // 트랜잭션 실제 동작은 Integration Test로!
}

// ❌ 단일 Manager만 호출
@Test
void saveOrder_WithSingleManager() {
    facade.saveOrder(order);  // ❌ 단일 Manager는 UseCase에서 직접!
}

// ❌ UseCase 호출
@Test
void processOrder_WithUseCases() {
    // ❌ Facade는 Manager를 조합! UseCase 조합 금지!
    facade.processOrder(command);
}
```

### ✅ Good Examples

```java
// ✅ Mock 기반 단위 테스트
@Tag("unit")
@Tag("facade")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {
    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private OutboxTransactionManager outboxManager;

    @InjectMocks
    private OrderFacade facade;
}

// ✅ Manager 호출 순서 검증
@Test
void saveOrderWithOutbox_ShouldCallManagersInOrder() {
    given(orderManager.save(any(Order.class)))
        .willReturn(savedOrder);
    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    facade.saveOrderWithOutbox(order, "OrderCreated");

    InOrder inOrder = inOrder(orderManager, outboxManager);
    inOrder.verify(orderManager).save(order);
    inOrder.verify(outboxManager).save(any(OutboxEvent.class));
}

// ✅ 반환값 전달 검증
@Test
void saveOrderWithOutbox_ShouldReturnFirstManagerResult() {
    given(orderManager.save(any(Order.class)))
        .willReturn(savedOrder);

    Order result = facade.saveOrderWithOutbox(order, "OrderCreated");

    assertThat(result).isEqualTo(savedOrder);
}

// ✅ 여러 Manager 호출 검증
@Test
void saveOrderWithOutbox_ShouldCallBothManagers() {
    facade.saveOrderWithOutbox(order, "OrderCreated");

    then(orderManager).should(times(1)).save(order);
    then(outboxManager).should(times(1)).save(any(OutboxEvent.class));
}
```

---

## 5️⃣ 테스트 시나리오

### Manager 호출 순서 검증
```java
@Test
@DisplayName("OrderManager → OutboxManager 순서로 호출해야 한다")
void saveOrderWithOutbox_ShouldCallManagersInCorrectOrder() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    Order savedOrder = Order.forExisting(
        OrderId.of(1L),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(orderManager.save(any(Order.class)))
        .willReturn(savedOrder);

    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    // When
    facade.saveOrderWithOutbox(order, "OrderCreated");

    // Then - InOrder로 순서 검증
    InOrder inOrder = inOrder(orderManager, outboxManager);
    inOrder.verify(orderManager).save(order);
    inOrder.verify(outboxManager).save(any(OutboxEvent.class));
}
```

### 반환값 전달 검증
```java
@Test
@DisplayName("첫 번째 Manager(OrderManager)의 결과를 반환해야 한다")
void saveOrderWithOutbox_ShouldReturnOrderManagerResult() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    Order savedOrder = Order.forExisting(
        OrderId.of(1L),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(orderManager.save(any(Order.class)))
        .willReturn(savedOrder);

    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    // When
    Order result = facade.saveOrderWithOutbox(order, "OrderCreated");

    // Then
    assertThat(result).isEqualTo(savedOrder);
    assertThat(result.getIdValue()).isEqualTo(1L);
}
```

### Manager 호출 횟수 검증
```java
@Test
@DisplayName("각 Manager를 정확히 1번씩만 호출해야 한다")
void saveOrderWithOutbox_ShouldCallEachManagerOnce() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(orderManager.save(any(Order.class)))
        .willReturn(order);

    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    // When
    facade.saveOrderWithOutbox(order, "OrderCreated");

    // Then
    then(orderManager).should(times(1)).save(order);
    then(outboxManager).should(times(1)).save(any(OutboxEvent.class));
    then(orderManager).shouldHaveNoMoreInteractions();
    then(outboxManager).shouldHaveNoMoreInteractions();
}
```

### 파라미터 전달 검증
```java
@Test
@DisplayName("저장된 Order의 ID를 OutboxEvent에 전달해야 한다")
void saveOrderWithOutbox_ShouldPassOrderIdToOutbox() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    Order savedOrder = Order.forExisting(
        OrderId.of(1L),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(orderManager.save(any(Order.class)))
        .willReturn(savedOrder);

    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    // When
    facade.saveOrderWithOutbox(order, "OrderCreated");

    // Then
    then(outboxManager).should().save(argThat(event ->
        event.getAggregateId().equals(1L) &&
        event.getEventType().equals("OrderCreated")
    ));
}
```

---

## 6️⃣ 체크리스트

Facade 테스트 작성 시:
- [ ] `@Tag("unit")`, `@Tag("facade")`, `@Tag("application-layer")` 필수
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` Manager 주입 (2개 이상)
- [ ] `@InjectMocks` Facade 주입
- [ ] Manager 호출 순서 검증 (InOrder)
- [ ] 반환값 전달 검증
- [ ] 각 Manager 호출 횟수 검증 (times(1))
- [ ] 파라미터 전달 검증 (argThat)
- [ ] 여러 Manager 조합 검증
- [ ] Spring Context 로딩 금지
- [ ] 실제 Manager 사용 금지
- [ ] 비즈니스 로직 테스트 금지
- [ ] 트랜잭션 실제 동작 테스트 금지 (Integration Test로)
- [ ] 단일 Manager 호출 금지 (UseCase에서 직접)
- [ ] UseCase 조합 금지 (Facade는 Manager만)

---

## 7️⃣ 성능 고려사항

### 빠른 실행
```java
@Test
@DisplayName("Facade 테스트는 밀리초 단위로 실행되어야 한다")
void facade_ShouldExecuteQuickly() {
    // Given
    long startTime = System.currentTimeMillis();

    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(orderManager.save(any(Order.class)))
        .willReturn(order);

    given(outboxManager.save(any(OutboxEvent.class)))
        .willReturn(any(OutboxEvent.class));

    // When
    facade.saveOrderWithOutbox(order, "OrderCreated");

    // Then
    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(10);  // 10ms 이하
}
```

---

## 8️⃣ Integration Test와의 관계

### 단위 테스트 (여기서 다룸)
- ✅ Manager 호출 순서 검증
- ✅ Manager 조합 로직 검증
- ✅ Mock 기반
- ✅ 빠른 실행 (밀리초)

### Integration Test (별도 문서)
- ✅ 트랜잭션 실제 동작 검증
- ✅ Rollback 검증
- ✅ 실제 DB 사용 (Testcontainers)
- ⚠️ 느린 실행 (초 단위)

---

## 📖 관련 문서

- **[Facade Guide](facade-guide.md)** - Facade 구현 가이드
- **[Transaction Manager Test Guide](../manager/transaction-manager-test-guide.md)** - Transaction Manager 테스트 가이드
- **[UseCase Test Guide](../testing/01_usecase-unit-test.md)** - UseCase 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
