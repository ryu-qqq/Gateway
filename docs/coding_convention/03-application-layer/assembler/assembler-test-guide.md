# Assembler 테스트 가이드

> **목적**: Assembler의 단위 테스트 전략 (순수 Java 기반)
>
> **핵심**: Domain → Response 변환만 테스트 (toDomain 테스트 없음!)

---

## 1) 테스트 전략

### 테스트 대상

Assembler는 **Domain → Response 변환만** 검증합니다:

```
✅ 테스트 항목:
1. Domain → Response 변환 검증
2. Domain → Detail Response 변환 검증
3. List<Domain> → List<Response> 변환 검증
4. 빈 List 처리 검증
5. null 처리 검증
6. 여러 Domain 조립 검증 (선택적)

❌ 테스트하지 않는 항목:
1. Command → Domain 변환 (Creator 테스트로!)
2. Query → Criteria 변환 (UseCase 테스트로!)
3. 비즈니스 로직 (Domain 테스트로!)
4. PageResponse 조립 (UseCase 테스트로!)
```

### 테스트 범위

- ✅ 순수 Java 단위 테스트 (외부 의존성 없음)
- ✅ 실제 Domain 객체 사용 (Mock 불필요)
- ✅ 빠른 실행 (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ Mock 사용 불필요 (Assembler는 의존성이 없음)
- ❌ 비즈니스 로직 테스트 금지 (Domain Test로 분리)

---

## 2) 기본 템플릿

```java
package com.ryuqq.application.{bc}.assembler;

import com.ryuqq.application.{bc}.dto.response.{Bc}Response;
import com.ryuqq.application.{bc}.dto.response.{Bc}DetailResponse;
import com.ryuqq.domain.{bc}.aggregate.{bc}.{Bc};
import com.ryuqq.domain.{bc}.vo.{Bc}Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * {Bc} Assembler 단위 테스트
 *
 * <p>Assembler는 Domain → Response 변환만 테스트합니다.</p>
 * <p>Command → Domain 변환은 Creator 테스트에서 검증합니다.</p>
 *
 * @author development-team
 * @since 3.0.0
 */
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
@DisplayName("{Bc} Assembler 단위 테스트")
class {Bc}AssemblerTest {

    private {Bc}Assembler assembler;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        assembler = new {Bc}Assembler();
        fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T10:00:00Z"),
            ZoneId.of("UTC")
        );
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponseTest {

        @Test
        @DisplayName("Domain → Response 변환이 올바르게 동작해야 한다")
        void shouldConvertDomainToResponse() {
            // Given
            {Bc} domain = createTestDomain();

            // When
            {Bc}Response result = assembler.toResponse(domain);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(domain.id().value());
            assertThat(result.status()).isEqualTo(domain.status().name());
            assertThat(result.createdAt()).isEqualTo(domain.createdAt());
        }
    }

    @Nested
    @DisplayName("toResponseList")
    class ToResponseListTest {

        @Test
        @DisplayName("List<Domain> → List<Response> 변환이 올바르게 동작해야 한다")
        void shouldConvertListCorrectly() {
            // Given
            List<{Bc}> domains = List.of(
                createTestDomain(),
                createTestDomain()
            );

            // When
            List<{Bc}Response> result = assembler.toResponseList(domains);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isNotNull();
            assertThat(result.get(1)).isNotNull();
        }

        @Test
        @DisplayName("빈 List를 전달하면 빈 List를 반환해야 한다")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            List<{Bc}Response> result = assembler.toResponseList(List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null을 전달하면 빈 List를 반환해야 한다")
        void shouldReturnEmptyListForNull() {
            // When
            List<{Bc}Response> result = assembler.toResponseList(null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Test Fixtures ====================

    private {Bc} createTestDomain() {
        return {Bc}.forNew(
            /* 필수 파라미터 */
            fixedClock
        );
    }
}
```

---

## 3) 실전 예시 (Order)

```java
package com.ryuqq.application.order.assembler;

import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.aggregate.order.OrderLineItem;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;
import com.ryuqq.domain.order.vo.Money;
import com.ryuqq.domain.order.vo.Quantity;
import com.ryuqq.domain.member.vo.MemberId;
import com.ryuqq.domain.product.vo.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Order Assembler 단위 테스트
 *
 * <p>Assembler는 Domain → Response 변환만 테스트합니다.</p>
 * <p>Command → Domain 변환은 OrderCreator 테스트에서 검증합니다.</p>
 *
 * @author development-team
 * @since 3.0.0
 */
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
@DisplayName("Order Assembler 단위 테스트")
class OrderAssemblerTest {

    private OrderAssembler assembler;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        assembler = new OrderAssembler();
        fixedClock = Clock.fixed(
            Instant.parse("2025-01-01T10:00:00Z"),
            ZoneId.of("UTC")
        );
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponseTest {

        @Test
        @DisplayName("Domain → Response 변환이 올바르게 동작해야 한다")
        void shouldConvertDomainToResponse() {
            // Given
            Order order = createTestOrder();

            // When
            OrderResponse result = assembler.toResponse(order);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(order.id().value());
            assertThat(result.customerId()).isEqualTo(order.customerId().value());
            assertThat(result.totalAmount()).isEqualTo(order.totalAmount().value());
            assertThat(result.status()).isEqualTo(order.status().name());
            assertThat(result.createdAt()).isEqualTo(order.createdAt());
        }

        @Test
        @DisplayName("모든 필드가 올바르게 매핑되어야 한다")
        void shouldMapAllFieldsCorrectly() {
            // Given
            Order order = createTestOrder();

            // When
            OrderResponse result = assembler.toResponse(order);

            // Then: 모든 필드 검증
            assertThat(result.orderId()).isNotNull();
            assertThat(result.customerId()).isNotNull();
            assertThat(result.totalAmount()).isNotNull();
            assertThat(result.status()).isNotNull();
            assertThat(result.createdAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toDetailResponse")
    class ToDetailResponseTest {

        @Test
        @DisplayName("Domain → DetailResponse 변환이 올바르게 동작해야 한다")
        void shouldConvertToDetailResponse() {
            // Given
            Order order = createTestOrderWithLineItems();

            // When
            OrderDetailResponse result = assembler.toDetailResponse(order);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(order.id().value());
            assertThat(result.lineItems()).hasSize(2);
        }

        @Test
        @DisplayName("LineItem 목록이 올바르게 변환되어야 한다")
        void shouldConvertLineItemsCorrectly() {
            // Given
            Order order = createTestOrderWithLineItems();

            // When
            OrderDetailResponse result = assembler.toDetailResponse(order);

            // Then
            assertThat(result.lineItems()).hasSize(2);
            assertThat(result.lineItems().get(0).productId()).isNotNull();
            assertThat(result.lineItems().get(0).quantity()).isPositive();
            assertThat(result.lineItems().get(0).unitPrice()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toResponseList")
    class ToResponseListTest {

        @Test
        @DisplayName("List<Order> → List<OrderResponse> 변환이 올바르게 동작해야 한다")
        void shouldConvertListCorrectly() {
            // Given
            List<Order> orders = List.of(
                createTestOrder(Money.of(BigDecimal.valueOf(10000))),
                createTestOrder(Money.of(BigDecimal.valueOf(20000))),
                createTestOrder(Money.of(BigDecimal.valueOf(30000)))
            );

            // When
            List<OrderResponse> result = assembler.toResponseList(orders);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).totalAmount()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(result.get(1).totalAmount()).isEqualTo(BigDecimal.valueOf(20000));
            assertThat(result.get(2).totalAmount()).isEqualTo(BigDecimal.valueOf(30000));
        }

        @Test
        @DisplayName("빈 List를 전달하면 빈 List를 반환해야 한다")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            List<OrderResponse> result = assembler.toResponseList(List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null을 전달하면 빈 List를 반환해야 한다")
        void shouldReturnEmptyListForNull() {
            // When
            List<OrderResponse> result = assembler.toResponseList(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("각 Response의 ID가 원본 Domain과 일치해야 한다")
        void shouldPreserveIdMapping() {
            // Given
            Order order1 = createTestOrder();
            Order order2 = createTestOrder();
            List<Order> orders = List.of(order1, order2);

            // When
            List<OrderResponse> result = assembler.toResponseList(orders);

            // Then
            assertThat(result.get(0).orderId()).isEqualTo(order1.id().value());
            assertThat(result.get(1).orderId()).isEqualTo(order2.id().value());
        }
    }

    @Nested
    @DisplayName("toSummaryResponse (여러 Domain 조립)")
    class ToSummaryResponseTest {

        @Test
        @DisplayName("Order와 Member를 조합하여 SummaryResponse를 생성해야 한다")
        void shouldCombineMultipleDomains() {
            // Given
            Order order = createTestOrder();
            String memberName = "홍길동";  // UseCase에서 조회한 값

            // When
            // OrderSummaryResponse result = assembler.toSummaryResponse(order, memberName);

            // Then
            // assertThat(result.memberName()).isEqualTo(memberName);
            // assertThat(result.orderId()).isEqualTo(order.id().value());
        }
    }

    // ==================== Test Fixtures ====================

    private Order createTestOrder() {
        return createTestOrder(Money.of(BigDecimal.valueOf(50000)));
    }

    private Order createTestOrder(Money totalAmount) {
        return Order.reconstitute(
            OrderId.of(1L),
            MemberId.of(100L),
            totalAmount,
            OrderStatus.CREATED,
            List.of(),
            fixedClock.instant(),
            fixedClock.instant(),
            fixedClock
        );
    }

    private Order createTestOrderWithLineItems() {
        List<OrderLineItem> lineItems = List.of(
            createTestLineItem(1L, 10000, 2),
            createTestLineItem(2L, 20000, 1)
        );

        return Order.reconstitute(
            OrderId.of(1L),
            MemberId.of(100L),
            Money.of(BigDecimal.valueOf(40000)),
            OrderStatus.CREATED,
            lineItems,
            fixedClock.instant(),
            fixedClock.instant(),
            fixedClock
        );
    }

    private OrderLineItem createTestLineItem(Long id, int price, int quantity) {
        return OrderLineItem.reconstitute(
            OrderLineItemId.of(id),
            ProductId.of(id * 10),
            "상품" + id,
            Money.of(BigDecimal.valueOf(price)),
            Quantity.of(quantity)
        );
    }
}
```

---

## 4) Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩 (불필요!)
@SpringBootTest
class OrderAssemblerTest {
    // Assembler는 Spring 의존성이 없어서 Context 불필요!
}

// ❌ Mock 사용 (Assembler는 의존성이 없음!)
@ExtendWith(MockitoExtension.class)
class OrderAssemblerTest {
    @Mock private Order order;  // 실제 Domain 객체 사용!
}

// ❌ Command → Domain 변환 테스트 (Creator 테스트로!)
@Test
void toDomain_ShouldConvertCommandToDomain() {
    PlaceOrderCommand command = new PlaceOrderCommand(...);
    Order order = assembler.toDomain(command);  // ❌ Assembler에 이 메서드 없음!
}

// ❌ 비즈니스 로직 테스트 (Domain 테스트로!)
@Test
void toResponse_WithBusinessLogic() {
    Order order = createTestOrder();
    order.confirm();  // ❌ 비즈니스 로직은 Domain Test에서!
    OrderResponse result = assembler.toResponse(order);
}

// ❌ PageResponse 조립 테스트 (UseCase 테스트로!)
@Test
void toPageResponse_ShouldConvert() {
    // ❌ PageResponse 조립은 UseCase 책임!
}

// ❌ Port/Repository Mock 사용 (Assembler는 의존성 없음!)
@Test
void toResponse_WithMockedPort() {
    when(memberQueryPort.findById(any())).thenReturn(member);  // ❌
}
```

### ✅ Good Examples

```java
// ✅ 순수 Java 단위 테스트
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
class OrderAssemblerTest {
    private OrderAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new OrderAssembler();  // 직접 인스턴스 생성
    }
}

// ✅ 실제 Domain 객체 사용
@Test
void toResponse_ShouldConvertCorrectly() {
    // Given: 실제 Domain 객체
    Order order = Order.reconstitute(...);

    // When
    OrderResponse result = assembler.toResponse(order);

    // Then
    assertThat(result.orderId()).isEqualTo(order.id().value());
}

// ✅ Domain → Response 변환만 테스트
@Test
void toResponse_ShouldMapAllFields() {
    Order order = createTestOrder();

    OrderResponse result = assembler.toResponse(order);

    assertThat(result.orderId()).isEqualTo(order.id().value());
    assertThat(result.status()).isEqualTo(order.status().name());
}

// ✅ List 변환 테스트
@Test
void toResponseList_ShouldConvertList() {
    List<Order> orders = List.of(createTestOrder(), createTestOrder());

    List<OrderResponse> result = assembler.toResponseList(orders);

    assertThat(result).hasSize(2);
}

// ✅ 경계 조건 테스트
@Test
void toResponseList_ShouldHandleEmptyAndNull() {
    assertThat(assembler.toResponseList(List.of())).isEmpty();
    assertThat(assembler.toResponseList(null)).isEmpty();
}
```

---

## 5) 테스트 시나리오

### Domain → Response 변환

```java
@Test
@DisplayName("Domain 필드가 Response에 올바르게 매핑되어야 한다")
void toResponse_ShouldMapAllFields() {
    // Given
    Order order = createTestOrder();

    // When
    OrderResponse result = assembler.toResponse(order);

    // Then
    assertThat(result.orderId()).isEqualTo(order.id().value());
    assertThat(result.customerId()).isEqualTo(order.customerId().value());
    assertThat(result.totalAmount()).isEqualTo(order.totalAmount().value());
    assertThat(result.status()).isEqualTo(order.status().name());
    assertThat(result.createdAt()).isEqualTo(order.createdAt());
}
```

### Nested Response 변환

```java
@Test
@DisplayName("중첩 객체가 올바르게 변환되어야 한다")
void toDetailResponse_ShouldConvertNestedObjects() {
    // Given
    Order order = createTestOrderWithLineItems();

    // When
    OrderDetailResponse result = assembler.toDetailResponse(order);

    // Then
    assertThat(result.lineItems()).hasSize(2);
    assertThat(result.lineItems().get(0).productId()).isNotNull();
}
```

### 빈 컬렉션 처리

```java
@Test
@DisplayName("빈 List를 전달하면 빈 List를 반환해야 한다")
void toResponseList_ShouldHandleEmptyList() {
    // When
    List<OrderResponse> result = assembler.toResponseList(List.of());

    // Then
    assertThat(result).isEmpty();
}
```

### Null 처리

```java
@Test
@DisplayName("null을 전달하면 빈 List를 반환해야 한다")
void toResponseList_ShouldHandleNull() {
    // When
    List<OrderResponse> result = assembler.toResponseList(null);

    // Then
    assertThat(result).isEmpty();
}
```

---

## 6) 테스트 Fixtures

### Clock 고정

```java
private Clock fixedClock;

@BeforeEach
void setUp() {
    fixedClock = Clock.fixed(
        Instant.parse("2025-01-01T10:00:00Z"),
        ZoneId.of("UTC")
    );
}
```

### Domain 생성 헬퍼

```java
private Order createTestOrder() {
    return Order.reconstitute(
        OrderId.of(1L),
        MemberId.of(100L),
        Money.of(BigDecimal.valueOf(50000)),
        OrderStatus.CREATED,
        List.of(),
        fixedClock.instant(),
        fixedClock.instant(),
        fixedClock
    );
}

private Order createTestOrderWithLineItems() {
    List<OrderLineItem> lineItems = List.of(
        createTestLineItem(1L, 10000, 2),
        createTestLineItem(2L, 20000, 1)
    );

    return Order.reconstitute(
        OrderId.of(1L),
        MemberId.of(100L),
        Money.of(BigDecimal.valueOf(40000)),
        OrderStatus.CREATED,
        lineItems,
        fixedClock.instant(),
        fixedClock.instant(),
        fixedClock
    );
}
```

---

## 7) 체크리스트

Assembler 테스트 작성 시:
- [ ] `@Tag("unit")`, `@Tag("assembler")`, `@Tag("application-layer")` 필수
- [ ] `@BeforeEach`에서 Assembler 인스턴스 생성 (Spring 없이!)
- [ ] Domain → Response 변환 테스트
- [ ] Domain → DetailResponse 변환 테스트
- [ ] List 변환 테스트
- [ ] 빈 List 처리 테스트
- [ ] null 처리 테스트
- [ ] 모든 필드 매핑 검증
- [ ] Spring Context 로딩 금지
- [ ] Mock 사용 금지 (Assembler는 의존성 없음)
- [ ] **toDomain 테스트 금지** (Creator 테스트로!)
- [ ] **PageResponse 테스트 금지** (UseCase 테스트로!)

---

## 8) 성능 고려사항

### 빠른 실행

Assembler 테스트는 외부 의존성이 없어 밀리초 단위로 실행됩니다:

```java
@Test
@DisplayName("Assembler 테스트는 10ms 이하로 실행되어야 한다")
void assembler_ShouldExecuteQuickly() {
    // Given
    long startTime = System.currentTimeMillis();
    Order order = createTestOrder();

    // When
    OrderResponse response = assembler.toResponse(order);
    List<OrderResponse> responses = assembler.toResponseList(List.of(order, order));

    // Then
    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(10);  // 10ms 이하
}
```

---

## 9) 관련 문서

- [Assembler Guide](./assembler-guide.md) - Assembler 구현 가이드
- [Assembler ArchUnit](./assembler-archunit.md) - ArchUnit 자동 검증 규칙
- [Creator Test Guide](../creator/creator-test-guide.md) - Creator 테스트 가이드 (Command → Domain)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 3.0.0 (Domain → Response 변환 전용)
