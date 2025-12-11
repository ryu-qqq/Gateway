# Aggregate Root 테스트 가이드

> **목적**: Aggregate Root의 단위 테스트 전략
>
> **참조**: 설계 원칙은 [Aggregate Guide](aggregate-guide.md) 참조

---

## 1) 테스트 전략

### 테스트 대상

| 항목 | 설명 |
|------|------|
| 정적 팩토리 메서드 | forNew, of, reconstitute |
| 비즈니스 메서드 | confirm, cancel, ship 등 |
| 상태 전이 | PENDING → CONFIRMED → SHIPPED |
| 도메인 규칙 | Invariant 검증 |
| 판단 메서드 | canConfirm(), isCancellable() |
| Clock 의존성 | 테스트 가능성 검증 |

### 테스트 범위

- ✅ **Pure Java 단위 테스트** (외부 의존성 제로)
- ✅ **Object Mother 패턴** (비즈니스 시나리오 표현)
- ✅ **빠른 실행** (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ Database 의존성 금지
- ❌ Mock 사용 최소화

---

## 2) 기본 템플릿

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.mother.Orders;
import com.ryuqq.domain.order.exception.InvalidOrderStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

/**
 * Order Aggregate Root 단위 테스트
 */
@Tag("unit")
@Tag("domain")
@Tag("aggregate")
@DisplayName("Order Aggregate Root 단위 테스트")
class OrderTest {

    // ✅ Clock 고정 (테스트 재현성)
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("forNew() - 신규 생성 시 ID는 null, 상태는 PENDING")
        void forNew_ShouldCreateNewInstanceWithNullIdAndPendingStatus() {
            // When
            Order order = Order.forNew(CustomerId.of(1L), FIXED_CLOCK);

            // Then
            assertThat(order.id()).isNull();  // Auto Increment용 null
            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.createdAt()).isEqualTo(FIXED_CLOCK.instant());
            assertThat(order.updatedAt()).isEqualTo(FIXED_CLOCK.instant());
        }

        @Test
        @DisplayName("of() - ID가 null이면 예외 발생")
        void of_WithNullId_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Order.of(null, CustomerId.of(1L), OrderStatus.PENDING, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID는 null일 수 없습니다.");
        }

        @Test
        @DisplayName("of() - 유효한 ID로 생성 성공")
        void of_WithValidId_ShouldCreateInstance() {
            // Given
            OrderId id = OrderId.of(1L);

            // When
            Order order = Order.of(id, CustomerId.of(100L), OrderStatus.PENDING, FIXED_CLOCK);

            // Then
            assertThat(order.id()).isEqualTo(id);
        }

        @Test
        @DisplayName("reconstitute() - 영속성 복원 시 모든 필드 설정")
        void reconstitute_ShouldRestoreAllFields() {
            // Given
            OrderId id = OrderId.of(100L);
            Instant createdAt = FIXED_CLOCK.instant().minusSeconds(86400);
            Instant updatedAt = FIXED_CLOCK.instant();

            // When
            Order order = Order.reconstitute(
                id, CustomerId.of(1L), OrderStatus.CONFIRMED,
                createdAt, updatedAt, FIXED_CLOCK
            );

            // Then
            assertThat(order.id()).isEqualTo(id);
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.createdAt()).isEqualTo(createdAt);
            assertThat(order.updatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트 (Object Mother 활용)")
    class BusinessMethodTests {

        @Test
        @DisplayName("confirm() - PENDING 상태에서 CONFIRMED로 전이")
        void confirm_FromPendingStatus_ShouldTransitionToConfirmed() {
            // Given - ✅ Object Mother 패턴
            Order order = Orders.pendingOrder();

            // When
            order.confirm();

            // Then
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.updatedAt()).isAfter(order.createdAt());
        }

        @Test
        @DisplayName("confirm() - 이미 CONFIRMED 상태면 예외 발생")
        void confirm_WhenAlreadyConfirmed_ShouldThrowException() {
            // Given - ✅ Object Mother 패턴
            Order order = Orders.confirmedOrder();

            // When & Then
            assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("이미 확정된 상태");
        }

        @Test
        @DisplayName("cancel() - PENDING 상태에서 취소 가능")
        void cancel_FromPendingStatus_ShouldSucceed() {
            // Given
            Order order = Orders.pendingOrder();

            // When
            order.cancel("고객 요청");

            // Then
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancel() - SHIPPED 상태에서 취소 불가")
        void cancel_FromShippedStatus_ShouldThrowException() {
            // Given
            Order order = Orders.shippedOrder();

            // When & Then
            assertThatThrownBy(() -> order.cancel("고객 요청"))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("취소 불가");
        }
    }

    @Nested
    @DisplayName("판단 메서드 테스트 (Tell Don't Ask)")
    class JudgmentMethodTests {

        @Test
        @DisplayName("isCancellable() - 취소 가능 여부 판단")
        void isCancellable_ShouldProvideBusinessLogic() {
            // Given
            Order pending = Orders.pendingOrder();
            Order confirmed = Orders.confirmedOrder();
            Order shipped = Orders.shippedOrder();

            // Then - ✅ 도메인 객체가 스스로 판단
            assertThat(pending.isCancellable()).isTrue();
            assertThat(confirmed.isCancellable()).isTrue();
            assertThat(shipped.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("isShippable() - 배송 가능 여부 판단")
        void isShippable_ShouldCheckConditions() {
            // Given
            Order pending = Orders.pendingOrder();
            Order confirmed = Orders.confirmedOrder();

            // Then
            assertThat(pending.isShippable()).isFalse();
            assertThat(confirmed.isShippable()).isTrue();
        }

        @Test
        @DisplayName("canConfirm() - 확정 가능 여부 판단")
        void canConfirm_ShouldCheckConditions() {
            // Given
            Order pendingWithItems = Orders.pendingOrderWithItems();
            Order pendingEmpty = Orders.pendingOrder();
            Order confirmed = Orders.confirmedOrder();

            // Then
            // 상품이 있는 PENDING만 확정 가능
            assertThat(pendingWithItems.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("상태 전이 테스트")
    class StateTransitionTests {

        @Test
        @DisplayName("전체 수명 주기 - PENDING → CONFIRMED → SHIPPED → COMPLETED")
        void fullLifecycle_ShouldTransitionThroughAllStates() {
            // Given
            Order order = Orders.pendingOrderWithItems();
            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);

            // PENDING → CONFIRMED
            order.confirm();
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);

            // CONFIRMED → SHIPPED
            order.ship();
            assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);

            // SHIPPED → COMPLETED
            order.complete();
            assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("잘못된 상태 전이 - PENDING → SHIPPED (직접 불가)")
        void invalidTransition_FromPendingToShipped_ShouldThrowException() {
            // Given
            Order order = Orders.pendingOrder();

            // When & Then
            assertThatThrownBy(order::ship)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("배송 시작 불가");
        }
    }

    @Nested
    @DisplayName("Clock 의존성 테스트")
    class ClockDependencyTests {

        @Test
        @DisplayName("Clock 고정 시 시간 값 예측 가능")
        void withFixedClock_TimeShouldBePredictable() {
            // Given
            Clock fixedClock = Clock.fixed(
                Instant.parse("2024-12-25T15:30:00Z"),
                ZoneId.of("UTC")
            );

            // When
            Order order = Order.forNew(CustomerId.of(1L), fixedClock);

            // Then - ✅ 테스트 재현성 보장
            Instant expectedTime = fixedClock.instant();
            assertThat(order.createdAt()).isEqualTo(expectedTime);
            assertThat(order.updatedAt()).isEqualTo(expectedTime);
        }

        @Test
        @DisplayName("상태 변경 시 updatedAt 자동 갱신")
        void statusChange_ShouldUpdateUpdatedAtAutomatically() {
            // Given
            Order order = Orders.pendingOrderWithItems();
            Instant initialUpdatedAt = order.updatedAt();

            // When
            order.confirm();

            // Then
            assertThat(order.updatedAt()).isAfterOrEqualTo(initialUpdatedAt);
        }
    }

    @Nested
    @DisplayName("도메인 규칙 검증 테스트")
    class InvariantTests {

        @Test
        @DisplayName("ID는 불변 - 생성 후 변경 불가")
        void id_ShouldBeImmutable() {
            // Given
            OrderId id = OrderId.of(1L);
            Order order = Order.of(id, CustomerId.of(1L), OrderStatus.PENDING, FIXED_CLOCK);

            // When - 상태 변경
            order.confirm();

            // Then - ID는 변경되지 않음
            assertThat(order.id()).isEqualTo(id);
        }

        @Test
        @DisplayName("createdAt은 불변 - 상태 변경 시에도 유지")
        void createdAt_ShouldBeImmutable() {
            // Given
            Order order = Orders.pendingOrderWithItems();
            Instant initialCreatedAt = order.createdAt();

            // When
            order.confirm();
            order.ship();

            // Then
            assertThat(order.createdAt()).isEqualTo(initialCreatedAt);
        }
    }
}
```

---

## 3) Object Mother 패턴

### 패턴 비교

| 구분 | Fixture | Object Mother |
|------|---------|---------------|
| **목적** | 기본 데이터 생성 | 비즈니스 시나리오 표현 |
| **네이밍** | `forNew()`, `of()` | `pendingOrder()` |
| **복잡도** | 단순 (1-2 필드) | 복잡 (상태 전이 포함) |
| **비즈니스 의미** | 없음 | 있음 |
| **패키지** | `fixture/` | `mother/` |

### Object Mother 클래스

**위치**: `domain/src/testFixtures/java/com/ryuqq/domain/order/mother/`

```java
package com.ryuqq.domain.order.mother;

import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.*;
import com.ryuqq.domain.order.fixture.OrderFixture;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Order Object Mother - 비즈니스 시나리오 표현
 */
public final class Orders {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    /**
     * 대기 중인 주문 (생성 직후 상태)
     */
    public static Order pendingOrder() {
        return OrderFixture.forNew();
    }

    /**
     * 상품이 있는 대기 중인 주문
     */
    public static Order pendingOrderWithItems() {
        Order order = OrderFixture.forNew();
        order.addLineItem(ProductId.of(101L), Quantity.of(1), Money.of(10000));
        return order;
    }

    /**
     * 승인된 주문 (결제 완료 후 상태)
     */
    public static Order confirmedOrder() {
        Order order = pendingOrderWithItems();
        order.confirm();  // ✅ 비즈니스 로직 사용
        return order;
    }

    /**
     * 배송 중인 주문
     */
    public static Order shippedOrder() {
        Order order = confirmedOrder();
        order.ship();
        return order;
    }

    /**
     * 취소된 주문
     */
    public static Order cancelledOrder() {
        Order order = pendingOrderWithItems();
        order.cancel("고객 요청");
        return order;
    }

    private Orders() {
        throw new AssertionError("Object Mother 클래스는 인스턴스화 불가");
    }
}
```

### TestFixture 클래스

**위치**: `domain/src/testFixtures/java/com/ryuqq/domain/order/fixture/`

```java
package com.ryuqq.domain.order.fixture;

import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * Order Aggregate TestFixture
 *
 * <p>Aggregate와 동일한 생성 패턴: forNew, of, reconstitute</p>
 */
public final class OrderFixture {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    /**
     * 신규 생성 (ID = null, Auto Increment)
     */
    public static Order forNew() {
        return Order.forNew(CustomerId.of(1L), FIXED_CLOCK);
    }

    /**
     * 특정 고객으로 신규 생성
     */
    public static Order forNew(CustomerId customerId) {
        return Order.forNew(customerId, FIXED_CLOCK);
    }

    /**
     * ID 기반 생성
     */
    public static Order of(Long id) {
        return Order.of(
            OrderId.of(id),
            CustomerId.of(1L),
            OrderStatus.PENDING,
            FIXED_CLOCK
        );
    }

    /**
     * 영속성 복원
     */
    public static Order reconstitute(Long id, OrderStatus status) {
        return Order.reconstitute(
            OrderId.of(id),
            CustomerId.of(1L),
            status,
            new ArrayList<>(),
            FIXED_CLOCK.instant(),
            FIXED_CLOCK.instant(),
            FIXED_CLOCK
        );
    }

    private OrderFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화 불가");
    }
}
```

---

## 4) Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩
@SpringBootTest
class OrderTest { }

// ❌ Mock 남발
Order order = mock(Order.class);
when(order.status()).thenReturn(OrderStatus.CONFIRMED);

// ❌ Reflection 사용
ReflectionTestUtils.setField(order, "status", OrderStatus.CONFIRMED);

// ❌ LocalDateTime 사용
assertThat(order.createdAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));

// ❌ get* 스타일 getter
assertThat(order.getId()).isNull();
assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

// ❌ System Clock 사용
Order order = Order.forNew(Clock.systemDefaultZone());
```

### ✅ Good Examples

```java
// ✅ Pure Java 단위 테스트
@Tag("unit")
@Tag("domain")
class OrderTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(...);
}

// ✅ 실제 객체 사용
Order order = Orders.pendingOrder();
order.confirm();

// ✅ Object Mother 패턴
Order order = Orders.confirmedOrder();  // 비즈니스 의미 명확

// ✅ Instant 사용
assertThat(order.createdAt()).isEqualTo(FIXED_CLOCK.instant());

// ✅ record 스타일 getter
assertThat(order.id()).isNull();
assertThat(order.status()).isEqualTo(OrderStatus.PENDING);

// ✅ Clock 고정 (테스트 재현성)
Order order = Order.forNew(CustomerId.of(1L), FIXED_CLOCK);
```

---

## 5) 체크리스트

- [ ] `@Tag("unit")`, `@Tag("domain")`, `@Tag("aggregate")`
- [ ] Clock 고정 (`Clock.fixed()`)
- [ ] Instant 타입 사용 (LocalDateTime 금지)
- [ ] record 스타일 getter (`id()`, `status()`)
- [ ] Object Mother 패턴 활용
- [ ] 정적 팩토리 메서드 테스트 (forNew, of, reconstitute)
- [ ] 비즈니스 메서드 테스트
- [ ] 판단 메서드 테스트 (Tell Don't Ask)
- [ ] 상태 전이 테스트
- [ ] Spring Context 로딩 금지
- [ ] Mock 사용 최소화
- [ ] Reflection 사용 금지

---

## 📖 관련 문서

- **[Aggregate Guide](aggregate-guide.md)** - Aggregate Root 설계 원칙
- **[Aggregate ArchUnit](aggregate-archunit.md)** - ArchUnit 검증 규칙
