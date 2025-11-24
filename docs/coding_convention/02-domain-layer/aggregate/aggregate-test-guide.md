# Aggregate Root 테스트 가이드

> **목적**: Aggregate Root의 단위 테스트 전략 (Object Mother 패턴 중심)

---

## 1️⃣ 테스트 전략

### 테스트 대상
Aggregate Root는 **비즈니스 로직과 상태 전이**를 검증합니다:

```
✅ 테스트 항목:
1. 정적 팩토리 메서드 (forNew, of, reconstitute)
2. 비즈니스 메서드 (confirm, cancel, ship 등)
3. 상태 전이 (PENDING → CONFIRMED → SHIPPED)
4. 도메인 규칙 검증 (Invariant)
5. Law of Demeter 준수 (getIdValue 등)
6. Clock 의존성 (테스트 가능성)
```

### 테스트 범위
- ✅ **Pure Java 단위 테스트** (외부 의존성 제로)
- ✅ **Object Mother 패턴** (비즈니스 시나리오 표현)
- ✅ **빠른 실행** (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ Database 의존성 금지
- ❌ Mock 사용 최소화 (Pure Domain Logic)

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.domain.{bc}.aggregate.{name};

import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import com.ryuqq.domain.{bc}.{Bc}Status;
import com.ryuqq.domain.{bc}.mother.{Bc}s;
import com.ryuqq.domain.{bc}.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.*;

/**
 * {Bc} Aggregate Root 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>Object Mother 패턴 활용 (비즈니스 시나리오 표현)</li>
 *   <li>Pure Java 단위 테스트 (외부 의존성 제로)</li>
 *   <li>Clock 고정 (테스트 재현성)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("aggregate")
@DisplayName("{Bc} Aggregate Root 단위 테스트")
class {Bc}Test {

    // ✅ Clock 고정 (테스트 재현성)
    private static final Clock FIXED_CLOCK = Clock.fixed(
        parse("2024-01-01T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("forNew() - 신규 생성 시 ID는 null, 상태는 PENDING")
        void forNew_ShouldCreateNewInstanceWithNullIdAndPendingStatus() {
            // When
            {Bc} {bc} = {Bc}.forNew(FIXED_CLOCK);

            // Then
            assertThat({bc}.getId()).isNull();  // Auto Increment용 null
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.PENDING);
            assertThat({bc}.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
            assertThat({bc}.getUpdatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
        }

        @Test
        @DisplayName("of() - ID가 null이면 예외 발생")
        void of_WithNullId_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> {Bc}.of(null, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ID는 null일 수 없습니다.");
        }

        @Test
        @DisplayName("of() - 유효한 ID로 생성 성공")
        void of_WithValidId_ShouldCreateInstance() {
            // Given
            {Bc}Id id = {Bc}Id.of(1L);

            // When
            {Bc} {bc} = {Bc}.of(id, FIXED_CLOCK);

            // Then
            assertThat({bc}.getId()).isEqualTo(id);
            assertThat({bc}.getIdValue()).isEqualTo(1L);  // Law of Demeter
        }

        @Test
        @DisplayName("reconstitute() - 영속성 복원 시 모든 필드 설정")
        void reconstitute_ShouldRestoreAllFields() {
            // Given
            {Bc}Id id = {Bc}Id.of(100L);
            LocalDateTime createdAt = LocalDateTime.now(FIXED_CLOCK).minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now(FIXED_CLOCK);

            // When
            {Bc} {bc} = {Bc}.reconstitute(id, {Bc}Status.CONFIRMED, createdAt, updatedAt, FIXED_CLOCK);

            // Then
            assertThat({bc}.getId()).isEqualTo(id);
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CONFIRMED);
            assertThat({bc}.getCreatedAt()).isEqualTo(createdAt);
            assertThat({bc}.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트 (Object Mother 활용)")
    class BusinessMethodTests {

        @Test
        @DisplayName("confirm() - PENDING 상태에서 CONFIRMED로 전이")
        void confirm_FromPendingStatus_ShouldTransitionToConfirmed() {
            // Given - ✅ Object Mother 패턴
            {Bc} {bc} = {Bc}s.pending{Bc}();

            // When
            {bc}.confirm();

            // Then
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CONFIRMED);
            assertThat({bc}.getUpdatedAt()).isAfter({bc}.getCreatedAt());
        }

        @Test
        @DisplayName("confirm() - 이미 CONFIRMED 상태면 예외 발생")
        void confirm_WhenAlreadyConfirmed_ShouldThrowException() {
            // Given - ✅ Object Mother 패턴 (비즈니스 시나리오 명확)
            {Bc} {bc} = {Bc}s.confirmed{Bc}();

            // When & Then
            assertThatThrownBy({bc}::confirm)
                .isInstanceOf(Invalid{Bc}StateException.class)
                .hasMessageContaining("이미 확정된 상태");
        }

        @Test
        @DisplayName("cancel() - PENDING 상태에서 취소 가능")
        void cancel_FromPendingStatus_ShouldSucceed() {
            // Given - ✅ Object Mother 패턴
            {Bc} {bc} = {Bc}s.pending{Bc}();

            // When
            {bc}.cancel();

            // Then
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CANCELLED);
        }

        @Test
        @DisplayName("cancel() - SHIPPED 상태에서 취소 불가")
        void cancel_FromShippedStatus_ShouldThrowException() {
            // Given - ✅ Object Mother 패턴 (취소 불가 시나리오)
            {Bc} {bc} = {Bc}s.shipped{Bc}();

            // When & Then
            assertThatThrownBy({bc}::cancel)
                .isInstanceOf(Invalid{Bc}StateException.class)
                .hasMessageContaining("취소 불가");
        }

        @Test
        @DisplayName("ship() - CONFIRMED 상태에서만 배송 시작 가능")
        void ship_FromConfirmedStatus_ShouldSucceed() {
            // Given - ✅ Object Mother 패턴
            {Bc} {bc} = {Bc}s.confirmed{Bc}();

            // When
            {bc}.ship();

            // Then
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.SHIPPED);
        }
    }

    @Nested
    @DisplayName("상태 전이 테스트")
    class StateTransitionTests {

        @Test
        @DisplayName("전체 수명 주기 - PENDING → CONFIRMED → SHIPPED → COMPLETED")
        void fullLifecycle_ShouldTransitionThroughAllStates() {
            // Given
            {Bc} {bc} = {Bc}.forNew(FIXED_CLOCK);
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.PENDING);

            // When & Then - PENDING → CONFIRMED
            {bc}.confirm();
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CONFIRMED);

            // When & Then - CONFIRMED → SHIPPED
            {bc}.ship();
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.SHIPPED);

            // When & Then - SHIPPED → COMPLETED
            {bc}.complete();
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.COMPLETED);
        }

        @Test
        @DisplayName("잘못된 상태 전이 - PENDING → SHIPPED (직접 불가)")
        void invalidTransition_FromPendingToShipped_ShouldThrowException() {
            // Given
            {Bc} {bc} = {Bc}s.pending{Bc}();

            // When & Then
            assertThatThrownBy({bc}::ship)
                .isInstanceOf(Invalid{Bc}StateException.class)
                .hasMessageContaining("배송 시작 불가");
        }
    }

    @Nested
    @DisplayName("Law of Demeter 테스트")
    class LawOfDemeterTests {

        @Test
        @DisplayName("getIdValue() - 원시 타입 반환 (ID.getValue() 체이닝 방지)")
        void getIdValue_ShouldReturnPrimitiveValue() {
            // Given
            {Bc} {bc} = {Bc}.of({Bc}Id.of(999L), FIXED_CLOCK);

            // When
            Long idValue = {bc}.getIdValue();

            // Then - ✅ Law of Demeter 준수
            assertThat(idValue).isEqualTo(999L);
            
            // ❌ 이렇게 하면 안됨: {bc}.getId().getValue()
        }

        @Test
        @DisplayName("isCancellable() - 취소 가능 여부 판단 (외부에서 상태 체크 금지)")
        void isCancellable_ShouldProvideBusinessLogic() {
            // Given
            {Bc} pending = {Bc}s.pending{Bc}();
            {Bc} confirmed = {Bc}s.confirmed{Bc}();
            {Bc} shipped = {Bc}s.shipped{Bc}();

            // Then - ✅ Law of Demeter: 비즈니스 메서드 제공
            assertThat(pending.isCancellable()).isTrue();
            assertThat(confirmed.isCancellable()).isTrue();
            assertThat(shipped.isCancellable()).isFalse();

            // ❌ 이렇게 하면 안됨: if ({bc}.getStatus() == PENDING || {bc}.getStatus() == CONFIRMED)
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
                parse("2024-12-25T15:30:00Z"),
                ZoneId.of("Asia/Seoul")
            );

            // When
            {Bc} {bc} = {Bc}.forNew(fixedClock);

            // Then - ✅ 테스트 재현성 보장
            LocalDateTime expectedTime = LocalDateTime.now(fixedClock);
            assertThat({bc}.getCreatedAt()).isEqualTo(expectedTime);
            assertThat({bc}.getUpdatedAt()).isEqualTo(expectedTime);
        }

        @Test
        @DisplayName("상태 변경 시 updatedAt 자동 갱신")
        void statusChange_ShouldUpdateUpdatedAtAutomatically() {
            // Given
            Clock clock1 = Clock.fixed(parse("2024-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
            Clock clock2 = Clock.fixed(parse("2024-01-02T00:00:00Z"), ZoneId.of("Asia/Seoul"));

            {Bc} {bc} = {Bc}.forNew(clock1);
            LocalDateTime initialUpdatedAt = {bc}.getUpdatedAt();

            // When - Clock 변경 후 상태 전이
            {bc} = {Bc}.reconstitute(
                {bc}.getId(),
                {bc}.getStatus(),
                {bc}.getCreatedAt(),
                {bc}.getUpdatedAt(),
                clock2  // ✅ 새로운 Clock 주입
            );
            {bc}.confirm();

            // Then - updatedAt 갱신 확인
            assertThat({bc}.getUpdatedAt()).isAfter(initialUpdatedAt);
        }
    }

    @Nested
    @DisplayName("도메인 규칙 검증 테스트")
    class InvariantTests {

        @Test
        @DisplayName("ID는 불변 - 생성 후 변경 불가")
        void id_ShouldBeImmutable() {
            // Given
            {Bc}Id id = {Bc}Id.of(1L);
            {Bc} {bc} = {Bc}.of(id, FIXED_CLOCK);

            // When
            {bc}.confirm();
            {bc}.ship();

            // Then - ✅ ID는 변경되지 않음
            assertThat({bc}.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("createdAt은 불변 - 상태 변경 시에도 유지")
        void createdAt_ShouldBeImmutable() {
            // Given
            {Bc} {bc} = {Bc}.forNew(FIXED_CLOCK);
            LocalDateTime initialCreatedAt = {bc}.getCreatedAt();

            // When
            {bc}.confirm();
            {bc}.ship();

            // Then - ✅ createdAt은 변경되지 않음
            assertThat({bc}.getCreatedAt()).isEqualTo(initialCreatedAt);
        }
    }

    @Nested
    @DisplayName("Object Mother 패턴 활용 예시")
    class ObjectMotherUsageExamples {

        @Test
        @DisplayName("승인된 주문 시나리오 - 비즈니스 의미 명확")
        void confirmedScenario_WithObjectMother() {
            // Given - ✅ "승인된 주문"이라는 비즈니스 의미 명확
            {Bc} {bc} = {Bc}s.confirmed{Bc}();

            // Then - 승인된 주문의 특성 검증
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CONFIRMED);
            assertThat({bc}.isShippable()).isTrue();
            assertThat({bc}.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("배송 중인 주문 시나리오 - 복잡한 상태 전이")
        void shippedScenario_WithObjectMother() {
            // Given - ✅ "배송 중인 주문"이라는 비즈니스 시나리오
            {Bc} {bc} = {Bc}s.shipped{Bc}();

            // Then - 배송 중인 주문의 특성 검증
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.SHIPPED);
            assertThat({bc}.isCancellable()).isFalse();
            assertThat({bc}.isCompletable()).isTrue();
        }

        @Test
        @DisplayName("취소된 주문 시나리오 - 종료 상태")
        void cancelledScenario_WithObjectMother() {
            // Given - ✅ "취소된 주문"이라는 비즈니스 종료 상태
            {Bc} {bc} = {Bc}s.cancelled{Bc}();

            // Then - 취소된 주문의 특성 검증
            assertThat({bc}.getStatus()).isEqualTo({Bc}Status.CANCELLED);
            assertThat({bc}.isTerminated()).isTrue();
            assertThat({bc}.isCancellable()).isFalse();
        }
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
@Tag("unit")
@Tag("domain")
@Tag("aggregate")
@DisplayName("Order Aggregate Root 단위 테스트")
class OrderTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        parse("2024-01-01T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
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
            assertThat(order.getId()).isNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getLineItems()).isEmpty();
        }

        @Test
        @DisplayName("of() - 유효한 ID로 생성 성공")
        void of_WithValidId_ShouldCreateInstance() {
            // Given
            OrderId id = OrderId.of(1L);
            CustomerId customerId = CustomerId.of(100L);

            // When
            Order order = Order.of(id, customerId, OrderStatus.PENDING, FIXED_CLOCK);

            // Then
            assertThat(order.getId()).isEqualTo(id);
            assertThat(order.getIdValue()).isEqualTo(1L);  // Law of Demeter
            assertThat(order.getCustomerIdValue()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트 (Object Mother 활용)")
    class BusinessMethodTests {

        @Test
        @DisplayName("addLineItem() - PENDING 상태에서만 상품 추가 가능")
        void addLineItem_FromPendingStatus_ShouldSucceed() {
            // Given
            Order order = Orders.pendingOrder();

            // When
            order.addLineItem(ProductId.of(101L), Quantity.of(2), Money.of(20000));

            // Then
            assertThat(order.getLineItems()).hasSize(1);
            assertThat(order.getTotalItemCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("confirm() - 상품이 있어야 확정 가능")
        void confirm_WithoutLineItems_ShouldThrowException() {
            // Given
            Order order = Orders.pendingOrder();  // 상품 없는 주문

            // When & Then
            assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("상품이 없습니다");
        }

        @Test
        @DisplayName("confirm() - 상품 추가 후 확정 성공")
        void confirm_WithLineItems_ShouldSucceed() {
            // Given
            Order order = Orders.pendingOrder();
            order.addLineItem(ProductId.of(101L), Quantity.of(1), Money.of(10000));

            // When
            order.confirm();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("ship() - CONFIRMED 상태에서만 배송 시작 가능")
        void ship_FromConfirmedStatus_ShouldSucceed() {
            // Given - ✅ Object Mother 패턴
            Order order = Orders.confirmedOrder();

            // When
            order.ship();

            // Then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("cancel() - SHIPPED 상태에서 취소 불가")
        void cancel_FromShippedStatus_ShouldThrowException() {
            // Given - ✅ Object Mother 패턴 (취소 불가 시나리오)
            Order order = Orders.shippedOrder();

            // When & Then
            assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("취소 불가");
        }
    }

    @Nested
    @DisplayName("Law of Demeter 테스트")
    class LawOfDemeterTests {

        @Test
        @DisplayName("getTotalAmount() - 총 금액 계산 (외부에서 계산 금지)")
        void getTotalAmount_ShouldCalculateInternally() {
            // Given
            Order order = Orders.pendingOrder();
            order.addLineItem(ProductId.of(101L), Quantity.of(2), Money.of(10000));
            order.addLineItem(ProductId.of(102L), Quantity.of(1), Money.of(5000));

            // When - ✅ Law of Demeter: Aggregate가 계산 제공
            Money totalAmount = order.getTotalAmount();

            // Then
            assertThat(totalAmount.getValue()).isEqualTo(25000);
        }

        @Test
        @DisplayName("containsProduct() - 상품 포함 여부 (외부에서 순회 금지)")
        void containsProduct_ShouldCheckInternally() {
            // Given
            Order order = Orders.pendingOrder();
            ProductId productId = ProductId.of(101L);
            order.addLineItem(productId, Quantity.of(1), Money.of(10000));

            // When - ✅ Law of Demeter: Aggregate가 판단 제공
            boolean contains = order.containsProduct(productId);

            // Then
            assertThat(contains).isTrue();
        }
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩
@SpringBootTest
class OrderTest {
    // Domain 테스트는 Spring 의존성 제로!
}

// ❌ Mock 남발
@Test
void confirm_ShouldWork() {
    Order order = mock(Order.class);  // ❌ Domain 객체 Mock 금지
    when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);
}

// ❌ Reflection 사용
@Test
void confirm_WithReflection() {
    Order order = Orders.pendingOrder();
    ReflectionTestUtils.setField(order, "status", OrderStatus.CONFIRMED);  // ❌
    // 비즈니스 로직 우회!
}

// ❌ Fixture만 사용 (비즈니스 의미 불명확)
@Test
void ship_ShouldWork() {
    Order order = OrderFixture.reconstitute(1L, OrderStatus.CONFIRMED);  // ❌
    // "승인된 주문"이 무엇인지 불명확 (비즈니스 로직 없이 상태만 설정)
}

// ❌ System.currentTimeMillis() 사용
@Test
void create_ShouldSetCurrentTime() {
    Order order = Order.forNew(Clock.systemDefaultZone());  // ❌ 테스트 재현성 없음
}
```

### ✅ Good Examples

```java
// ✅ Pure Java 단위 테스트
@Tag("unit")
@Tag("domain")
@Tag("aggregate")
class OrderTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(...);
}

// ✅ 실제 객체 사용
@Test
void confirm_ShouldTransitionState() {
    Order order = Orders.pendingOrder();  // ✅ 실제 Domain 객체
    order.addLineItem(...);
    order.confirm();
}

// ✅ Object Mother 패턴 (비즈니스 의미 명확)
@Test
void ship_FromConfirmedStatus_ShouldSucceed() {
    Order order = Orders.confirmedOrder();  // ✅ "승인된 주문" 명확
    order.ship();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
}

// ✅ Clock 고정 (테스트 재현성)
@Test
void create_ShouldUseClock() {
    Clock fixedClock = Clock.fixed(parse("2024-01-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    Order order = Order.forNew(CustomerId.of(1L), fixedClock);  // ✅
    assertThat(order.getCreatedAt()).isEqualTo(LocalDateTime.now(fixedClock));
}

// ✅ Law of Demeter 준수
@Test
void getIdValue_ShouldReturnPrimitive() {
    Order order = Orders.confirmedOrder();
    Long idValue = order.getIdValue();  // ✅ order.getId().getValue() 금지
}
```

---

## 5️⃣ TestFixture & Object Mother 패턴

### 패턴 비교

| 구분 | Fixture | Object Mother |
|------|---------|---------------|
| **목적** | 기본 데이터 생성 | 비즈니스 시나리오 표현 |
| **네이밍** | `forNew()`, `of()` | `pendingOrder()` |
| **복잡도** | 단순 (1-2 필드) | 복잡 (여러 단계 상태 전이) |
| **비즈니스 의미** | 없음 | 있음 |
| **패키지** | `fixture/` | `mother/` |

---

### OrderFixture 클래스 (Aggregate 생성 패턴 준수) ⭐

**위치**: `domain/src/testFixtures/java/com/ryuqq/domain/{bc}/fixture/`

**핵심**: Aggregate와 **동일한 생성 패턴** 사용 (`forNew`, `of`, `reconstitute`)

```java
package com.ryuqq.domain.order.fixture;

import com.ryuqq.domain.order.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Order Aggregate TestFixture
 *
 * <p><strong>생성 패턴</strong>: Aggregate와 동일한 패턴 강제</p>
 * <ul>
 *   <li>{@code forNew()} - 신규 생성 (ID = null, Auto Increment)</li>
 *   <li>{@code of()} - ID 기반 생성 (비즈니스 로직용)</li>
 *   <li>{@code reconstitute()} - 영속성 복원 (Mapper 패턴)</li>
 * </ul>
 *
 * <p><strong>금지</strong>: {@code create*()} 메서드 사용 금지 (ArchUnit 검증)</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderFixture {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    /**
     * 신규 생성 (ID = null, Auto Increment)
     * Aggregate의 forNew()와 동일한 패턴
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
     * ID 기반 생성 (비즈니스 로직용)
     * Aggregate의 of()와 동일한 패턴
     */
    public static Order of(Long id) {
        return Order.of(
            OrderId.of(id),
            CustomerId.of(1L),
            OrderStatus.PENDING,
            new ArrayList<>(),
            LocalDateTime.now(FIXED_CLOCK),
            LocalDateTime.now(FIXED_CLOCK),
            FIXED_CLOCK
        );
    }

    /**
     * ID와 고객 지정하여 생성
     */
    public static Order of(Long id, CustomerId customerId) {
        return Order.of(
            OrderId.of(id),
            customerId,
            OrderStatus.PENDING,
            new ArrayList<>(),
            LocalDateTime.now(FIXED_CLOCK),
            LocalDateTime.now(FIXED_CLOCK),
            FIXED_CLOCK
        );
    }

    /**
     * 영속성 복원 (Mapper 패턴)
     * Aggregate의 reconstitute()와 동일한 패턴
     *
     * <p><strong>주의</strong>: 파라미터는 Aggregate마다 다를 수 있음 (ArchUnit 검증 제외)</p>
     */
    public static Order reconstitute(Long id, OrderStatus status) {
        return Order.reconstitute(
            OrderId.of(id),
            CustomerId.of(1L),
            status,
            new ArrayList<>(),
            LocalDateTime.now(FIXED_CLOCK),
            LocalDateTime.now(FIXED_CLOCK),
            FIXED_CLOCK
        );
    }

    private OrderFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

---

### Object Mother 클래스 작성

**위치**: `domain/src/testFixtures/java/com/ryuqq/domain/{bc}/mother/`

```java
package com.ryuqq.domain.order.mother;

import com.ryuqq.domain.order.*;
import com.ryuqq.domain.order.fixture.OrderFixture;

/**
 * Order Object Mother - 비즈니스 시나리오 표현
 */
public class Orders {

    /**
     * 대기 중인 주문 (생성 직후 상태)
     */
    public static Order pendingOrder() {
        return OrderFixture.forNew();  // ✅ forNew() 사용
    }

    /**
     * 승인된 주문 (결제 완료 후 상태)
     */
    public static Order confirmedOrder() {
        Order order = OrderFixture.forNew();  // ✅ forNew() 사용
        order.addLineItem(ProductId.of(101L), Quantity.of(1), Money.of(10000));
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
        Order order = pendingOrder();
        order.addLineItem(ProductId.of(101L), Quantity.of(1), Money.of(10000));
        order.cancel();
        return order;
    }

    private Orders() {
        throw new AssertionError("Object Mother 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

---

## 6️⃣ 체크리스트

Aggregate Root 테스트 작성 시:
- [ ] `@Tag("unit")`, `@Tag("domain")`, `@Tag("aggregate")` 필수
- [ ] Clock 고정 (테스트 재현성)
- [ ] Object Mother 패턴 활용 (비즈니스 시나리오 명확)
- [ ] 정적 팩토리 메서드 테스트 (forNew, of, reconstitute)
- [ ] 비즈니스 메서드 테스트 (confirm, cancel, ship 등)
- [ ] 상태 전이 테스트
- [ ] Law of Demeter 준수 테스트 (getIdValue 등)
- [ ] 도메인 규칙 검증 (Invariant)
- [ ] Spring Context 로딩 금지
- [ ] Mock 사용 최소화
- [ ] Reflection 사용 금지

---

## 📖 관련 문서

- **[Aggregate Guide](aggregate-guide.md)** - Aggregate Root 구현 가이드
- **[Aggregate ArchUnit](aggregate-archunit.md)** - ArchUnit 자동 검증 규칙
- **[Object Mother 패턴](../legacy/testing/04_object-mother-pattern.md)** - 비즈니스 시나리오 표현
- **[Test Fixture 패턴](../legacy/testing/03_test-fixture-pattern.md)** - 기본 데이터 생성

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
