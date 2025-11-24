# Transaction Manager 테스트 가이드

> **목적**: Transaction Manager의 단위 테스트 전략 (Mock 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
Transaction Manager는 **단일 Port 트랜잭션 처리**만 검증합니다:

```
✅ 테스트 항목:
1. Port 호출 위임 검증
2. 반환값 전달 검증
3. 트랜잭션 경계 검증 (@Transactional)
4. 단일 Port 의존성 검증
5. 비즈니스 로직 없음 검증
```

### 테스트 범위
- ✅ 단위 테스트 (Mock 사용)
- ✅ Port 호출 위임 검증
- ✅ 빠른 실행 (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ 비즈니스 로직 테스트 금지 (Domain Test로 분리)
- ❌ 트랜잭션 실제 동작 테스트 금지 (Integration Test로)

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.application.{bc}.manager;

import com.ryuqq.application.{bc}.port.out.{Bc}PersistencePort;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * {Bc} Transaction Manager 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("manager")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
@DisplayName("{Bc} Transaction Manager 단위 테스트")
class {Bc}TransactionManagerTest {

    @Mock
    private {Bc}PersistencePort persistencePort;

    @InjectMocks
    private {Bc}TransactionManager transactionManager;

    @Test
    @DisplayName("save()는 Port에 저장을 위임하고 결과를 반환해야 한다")
    void save_ShouldDelegateToPortAndReturnResult() {
        // Given
        {Bc} {bc} = {Bc}.forNew(/* domain fields */);
        {Bc} saved{Bc} = {Bc}.forExisting(
            {Bc}Id.of(1L),
            /* domain fields */
        );

        given(persistencePort.save(any({Bc}.class)))
            .willReturn(saved{Bc});

        // When
        {Bc} result = transactionManager.save({bc});

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isEqualTo(1L);

        // Verify
        then(persistencePort).should(times(1)).save({bc});
    }

    @Test
    @DisplayName("비즈니스 로직 없이 Port 호출만 해야 한다")
    void save_ShouldNotContainBusinessLogic() {
        // Given
        {Bc} {bc} = {Bc}.forNew(/* domain fields */);

        given(persistencePort.save(any({Bc}.class)))
            .willReturn({bc});

        // When
        transactionManager.save({bc});

        // Then
        // ✅ Port 호출만 검증 (비즈니스 로직 없음)
        then(persistencePort).should(times(1)).save({bc});
        then(persistencePort).shouldHaveNoMoreInteractions();
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
package com.ryuqq.application.order.manager;

import com.ryuqq.application.order.port.out.OrderPersistencePort;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Order Transaction Manager 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("manager")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Transaction Manager 단위 테스트")
class OrderTransactionManagerTest {

    @Mock
    private OrderPersistencePort persistencePort;

    @InjectMocks
    private OrderTransactionManager transactionManager;

    @Test
    @DisplayName("save()는 Port에 저장을 위임하고 결과를 반환해야 한다")
    void save_ShouldDelegateToPortAndReturnResult() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        Order savedOrder = Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(persistencePort.save(any(Order.class)))
            .willReturn(savedOrder);

        // When
        Order result = transactionManager.save(order);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isEqualTo(1L);
        assertThat(result.getAmountValue()).isEqualTo(BigDecimal.valueOf(50000));

        // Verify
        then(persistencePort).should(times(1)).save(order);
    }

    @Test
    @DisplayName("비즈니스 로직 없이 Port 호출만 해야 한다")
    void save_ShouldNotContainBusinessLogic() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(persistencePort.save(any(Order.class)))
            .willReturn(order);

        // When
        transactionManager.save(order);

        // Then
        // ✅ Port 호출만 검증 (비즈니스 로직 없음)
        then(persistencePort).should(times(1)).save(order);
        then(persistencePort).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("동일한 Domain 객체를 Port에 전달해야 한다")
    void save_ShouldPassSameDomainObjectToPort() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        given(persistencePort.save(any(Order.class)))
            .willReturn(order);

        // When
        transactionManager.save(order);

        // Then
        // ✅ 동일한 객체 전달 검증
        then(persistencePort).should().save(order);
        then(persistencePort).should().save(argThat(arg ->
            arg.getAmountValue().equals(BigDecimal.valueOf(50000))
        ));
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩
@SpringBootTest
class OrderTransactionManagerTest {
    // Spring Context 로딩 불필요! (단위 테스트)
}

// ❌ 실제 Port 사용
class OrderTransactionManagerTest {
    private OrderPersistencePort persistencePort = new OrderPersistencePortImpl();
    // Mock 사용해야 함!
}

// ❌ 비즈니스 로직 테스트
@Test
void save_WithBusinessLogic() {
    Order order = Order.forNew(...);
    order.place();  // 비즈니스 로직은 Domain Test로!

    transactionManager.save(order);
}

// ❌ 트랜잭션 실제 동작 테스트
@Test
@Transactional
void save_ShouldRollbackOnException() {
    // 트랜잭션 실제 동작은 Integration Test로!
}

// ❌ 여러 Port 호출 테스트
@Test
void save_WithMultiplePorts() {
    transactionManager.save(order);

    // ❌ Transaction Manager는 단일 Port만!
    then(orderPort).should().save(order);
    then(outboxPort).should().save(event);
}
```

### ✅ Good Examples

```java
// ✅ Mock 기반 단위 테스트
@Tag("unit")
@Tag("manager")
@Tag("application-layer")
@ExtendWith(MockitoExtension.class)
class OrderTransactionManagerTest {
    @Mock
    private OrderPersistencePort persistencePort;

    @InjectMocks
    private OrderTransactionManager transactionManager;
}

// ✅ Port 위임 검증
@Test
void save_ShouldDelegateToPort() {
    Order order = Order.forNew(...);

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    transactionManager.save(order);

    then(persistencePort).should(times(1)).save(order);
}

// ✅ 반환값 전달 검증
@Test
void save_ShouldReturnPortResult() {
    Order savedOrder = Order.forExisting(OrderId.of(1L), ...);

    given(persistencePort.save(any(Order.class)))
        .willReturn(savedOrder);

    Order result = transactionManager.save(Order.forNew(...));

    assertThat(result.getIdValue()).isEqualTo(1L);
}

// ✅ 단일 Port만 호출 검증
@Test
void save_ShouldCallOnlyOnePort() {
    Order order = Order.forNew(...);

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    transactionManager.save(order);

    // ✅ 단일 Port만 호출
    then(persistencePort).should(times(1)).save(order);
    then(persistencePort).shouldHaveNoMoreInteractions();
}
```

---

## 5️⃣ 테스트 시나리오

### Port 위임 검증
```java
@Test
@DisplayName("save()는 persistencePort.save()를 정확히 1번 호출해야 한다")
void save_ShouldCallPersistencePortOnce() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    // When
    transactionManager.save(order);

    // Then
    then(persistencePort).should(times(1)).save(order);
}
```

### 반환값 전달 검증
```java
@Test
@DisplayName("save()는 Port가 반환한 Domain 객체를 그대로 반환해야 한다")
void save_ShouldReturnPortResult() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    Order savedOrder = Order.forExisting(
        OrderId.of(1L),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(persistencePort.save(any(Order.class)))
        .willReturn(savedOrder);

    // When
    Order result = transactionManager.save(order);

    // Then
    assertThat(result).isEqualTo(savedOrder);
    assertThat(result.getIdValue()).isEqualTo(1L);
}
```

### 파라미터 전달 검증
```java
@Test
@DisplayName("save()는 전달받은 Domain 객체를 Port에 그대로 전달해야 한다")
void save_ShouldPassDomainObjectToPort() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    // When
    transactionManager.save(order);

    // Then
    then(persistencePort).should().save(order);
    then(persistencePort).should().save(argThat(arg ->
        arg.getAmountValue().equals(BigDecimal.valueOf(50000))
    ));
}
```

### 단일 Port 호출 검증
```java
@Test
@DisplayName("save()는 단일 Port만 호출하고 다른 의존성은 없어야 한다")
void save_ShouldCallOnlyOnePort() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    // When
    transactionManager.save(order);

    // Then
    // ✅ 단일 Port만 호출
    then(persistencePort).should(times(1)).save(order);
    then(persistencePort).shouldHaveNoMoreInteractions();
}
```

---

## 6️⃣ 체크리스트

Transaction Manager 테스트 작성 시:
- [ ] `@Tag("unit")`, `@Tag("manager")`, `@Tag("application-layer")` 필수
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` Port 주입
- [ ] `@InjectMocks` Manager 주입
- [ ] Port 위임 검증 (times(1))
- [ ] 반환값 전달 검증
- [ ] 파라미터 전달 검증
- [ ] 단일 Port만 호출 검증 (shouldHaveNoMoreInteractions)
- [ ] Spring Context 로딩 금지
- [ ] 실제 Port 사용 금지
- [ ] 비즈니스 로직 테스트 금지
- [ ] 트랜잭션 실제 동작 테스트 금지 (Integration Test로)

---

## 7️⃣ 성능 고려사항

### 빠른 실행
```java
@Test
@DisplayName("Transaction Manager 테스트는 밀리초 단위로 실행되어야 한다")
void transactionManager_ShouldExecuteQuickly() {
    // Given
    long startTime = System.currentTimeMillis();

    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    given(persistencePort.save(any(Order.class)))
        .willReturn(order);

    // When
    transactionManager.save(order);

    // Then
    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(10);  // 10ms 이하
}
```

---

## 8️⃣ Integration Test와의 관계

### 단위 테스트 (여기서 다룸)
- ✅ Port 호출 위임 검증
- ✅ 반환값 전달 검증
- ✅ Mock 기반
- ✅ 빠른 실행 (밀리초)

### Integration Test (별도 문서)
- ✅ 트랜잭션 실제 동작 검증
- ✅ Rollback 검증
- ✅ 실제 DB 사용 (Testcontainers)
- ⚠️ 느린 실행 (초 단위)

---

## 📖 관련 문서

- **[Transaction Manager Guide](transaction-manager-guide.md)** - Transaction Manager 구현 가이드
- **[Facade Test Guide](../facade/facade-test-guide.md)** - Facade 테스트 가이드
- **[UseCase Test Guide](../testing/01_usecase-unit-test.md)** - UseCase 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
