# Command Factory Test Guide — **단위 테스트**

> CommandFactory는 **Command → Domain 변환**과 **PersistBundle 생성**을 담당합니다.
>
> **순수 변환 로직**이므로 **단위 테스트만** 작성합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | 변환 로직 검증 | CommandFactory만 |

### 테스트 포인트

| 항목 | 검증 내용 |
|------|----------|
| **Command → Domain 변환** | 모든 필드 올바르게 매핑 |
| **하위 객체 변환** | 중첩 Command 처리 |
| **PersistBundle 생성** | 여러 객체 올바르게 묶음 |
| **null 처리** | 선택 필드 null 안전 |
| **컬렉션 변환** | List 원소 변환 정확성 |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/factory/command/
   │      └─ {Bc}CommandFactory.java
   └─ test/java/
      └─ com/ryuqq/application/{bc}/factory/command/
          └─ {Bc}CommandFactoryTest.java
```

---

## 3) 단위 테스트 예시

### 기본 테스트

```java
package com.ryuqq.application.order.factory.command;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.command.OrderItemCommand;
import com.ryuqq.application.order.dto.bundle.OrderPersistBundle;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderCommandFactory 단위 테스트")
class OrderCommandFactoryTest {

    private OrderCommandFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OrderCommandFactory();
    }

    @Nested
    @DisplayName("create 테스트")
    class CreateTest {

        @Test
        @DisplayName("PlaceOrderCommand를 Order로 변환한다")
        void shouldConvertCommandToOrder() {
            // given
            OrderItemCommand itemCommand = new OrderItemCommand(
                1001L,  // productId
                2,      // quantity
                new BigDecimal("10000")  // unitPrice
            );

            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,  // customerId
                List.of(itemCommand)
            );

            // when
            Order order = factory.create(command);

            // then
            assertThat(order).isNotNull();
            assertThat(order.getCustomerId().value()).isEqualTo(100L);
            assertThat(order.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("여러 OrderItem을 변환한다")
        void shouldConvertMultipleItems() {
            // given
            List<OrderItemCommand> itemCommands = List.of(
                new OrderItemCommand(1001L, 2, new BigDecimal("10000")),
                new OrderItemCommand(1002L, 3, new BigDecimal("20000")),
                new OrderItemCommand(1003L, 1, new BigDecimal("30000"))
            );

            PlaceOrderCommand command = new PlaceOrderCommand(100L, itemCommands);

            // when
            Order order = factory.create(command);

            // then
            assertThat(order.getItems()).hasSize(3);
        }

        @Test
        @DisplayName("OrderItem의 모든 필드가 올바르게 변환된다")
        void shouldConvertAllFieldsCorrectly() {
            // given
            OrderItemCommand itemCommand = new OrderItemCommand(
                1001L,
                5,
                new BigDecimal("15000")
            );

            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,
                List.of(itemCommand)
            );

            // when
            Order order = factory.create(command);

            // then
            OrderItem item = order.getItems().get(0);
            assertThat(item.getProductId().value()).isEqualTo(1001L);
            assertThat(item.getQuantity().value()).isEqualTo(5);
            assertThat(item.getUnitPrice().value()).isEqualByComparingTo(new BigDecimal("15000"));
        }
    }

    @Nested
    @DisplayName("createBundle 테스트")
    class CreateBundleTest {

        @Test
        @DisplayName("Order와 OutboxEvent를 PersistBundle로 묶는다")
        void shouldCreateBundleWithOrderAndEvent() {
            // given
            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,
                List.of(new OrderItemCommand(1001L, 1, new BigDecimal("10000")))
            );

            // when
            OrderPersistBundle bundle = factory.createBundle(command);

            // then
            assertThat(bundle).isNotNull();
            assertThat(bundle.order()).isNotNull();
            assertThat(bundle.outboxEvent()).isNotNull();
            assertThat(bundle.outboxEvent().getEventType()).isEqualTo("OrderPlaced");
        }

        @Test
        @DisplayName("PersistBundle의 OutboxEvent는 aggregateId가 null이다")
        void shouldCreateOutboxEventWithNullAggregateId() {
            // given
            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,
                List.of(new OrderItemCommand(1001L, 1, new BigDecimal("10000")))
            );

            // when
            OrderPersistBundle bundle = factory.createBundle(command);

            // then
            assertThat(bundle.outboxEvent().getAggregateId()).isNull();
        }

        @Test
        @DisplayName("enrichWithId로 ID 할당 후 새 Bundle이 반환된다")
        void shouldEnrichWithIdReturnNewBundle() {
            // given
            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,
                List.of(new OrderItemCommand(1001L, 1, new BigDecimal("10000")))
            );
            OrderPersistBundle originalBundle = factory.createBundle(command);

            // when
            OrderPersistBundle enrichedBundle = originalBundle.enrichWithId(new OrderId(999L));

            // then
            assertThat(enrichedBundle).isNotSameAs(originalBundle);
            assertThat(enrichedBundle.outboxEvent().getAggregateId()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("복잡한 변환 테스트")
    class ComplexConversionTest {

        @Test
        @DisplayName("AddressCommand를 Address로 변환한다")
        void shouldConvertAddressCommand() {
            // given
            AddressCommand addressCommand = new AddressCommand(
                "123 Main St",
                "Seoul",
                "12345",
                "Korea"
            );

            PlaceOrderCommand command = new PlaceOrderCommand(
                100L,
                List.of(new OrderItemCommand(1001L, 1, new BigDecimal("10000"))),
                addressCommand
            );

            // when
            Order order = factory.create(command);

            // then
            Address address = order.getShippingAddress();
            assertThat(address.getStreet()).isEqualTo("123 Main St");
            assertThat(address.getCity()).isEqualTo("Seoul");
            assertThat(address.getZipCode()).isEqualTo("12345");
            assertThat(address.getCountry()).isEqualTo("Korea");
        }

        @Test
        @DisplayName("빈 아이템 리스트는 빈 Order 아이템이 된다")
        void shouldHandleEmptyItemList() {
            // given
            PlaceOrderCommand command = new PlaceOrderCommand(100L, List.of());

            // when
            Order order = factory.create(command);

            // then
            assertThat(order.getItems()).isEmpty();
        }
    }
}
```

---

## 4) 테스트 체크리스트

### Command → Domain 변환
- [ ] 모든 필드가 올바르게 매핑됨
- [ ] 중첩 객체 변환 정확성
- [ ] 컬렉션(List) 변환 정확성
- [ ] VO 생성 정확성

### PersistBundle 생성
- [ ] 모든 구성 요소가 Bundle에 포함됨
- [ ] OutboxEvent aggregateId가 null (Service/Facade에서 할당)
- [ ] enrichWithId() 호출 후 ID 할당됨

### 엣지 케이스
- [ ] 빈 컬렉션 처리
- [ ] 선택 필드 null 처리
- [ ] 최소값/최대값 경계

---

## 5) Do / Don't

### ✅ Good

```java
// ✅ Good: 순수 단위 테스트 (Mock 불필요)
@BeforeEach
void setUp() {
    factory = new OrderCommandFactory();  // 직접 생성
}

// ✅ Good: 모든 필드 검증
assertThat(order.getCustomerId().value()).isEqualTo(100L);
assertThat(order.getItems()).hasSize(1);

// ✅ Good: PersistBundle 구성 요소 검증
assertThat(bundle.order()).isNotNull();
assertThat(bundle.outboxEvent()).isNotNull();

// ✅ Good: VO 값 검증
assertThat(item.getProductId().value()).isEqualTo(1001L);

// ✅ Good: enrichWithId 불변성 검증
assertThat(enrichedBundle).isNotSameAs(originalBundle);
```

### ❌ Bad

```java
// ❌ Bad: Mock 사용 (Factory는 순수 변환)
@Mock
private SomePort somePort;  // ❌ Factory는 Port 의존 안 함

// ❌ Bad: 비즈니스 로직 테스트 (Domain 책임)
assertThat(order.calculateTotal()).isEqualTo(...);  // ❌ Domain 테스트

// ❌ Bad: 트랜잭션 테스트 (Factory는 트랜잭션 없음)
@Transactional
void testCreate() { ... }  // ❌

// ❌ Bad: 데이터베이스 접근 테스트
@SpringBootTest
class OrderCommandFactoryTest { ... }  // ❌ 단위 테스트로 충분
```

---

## 6) Fixture 활용

### TestFixtures 사용

```java
import com.ryuqq.fixture.application.PlaceOrderCommandFixture;

@DisplayName("OrderCommandFactory 단위 테스트")
class OrderCommandFactoryTest {

    private OrderCommandFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OrderCommandFactory();
    }

    @Test
    @DisplayName("기본 Command를 Order로 변환한다")
    void shouldConvertDefaultCommand() {
        // given
        PlaceOrderCommand command = PlaceOrderCommandFixture.create();

        // when
        Order order = factory.create(command);

        // then
        assertThat(order).isNotNull();
    }

    @Test
    @DisplayName("커스텀 Command를 Order로 변환한다")
    void shouldConvertCustomCommand() {
        // given
        PlaceOrderCommand command = PlaceOrderCommandFixture.builder()
            .customerId(999L)
            .itemCount(5)
            .build();

        // when
        Order order = factory.create(command);

        // then
        assertThat(order.getCustomerId().value()).isEqualTo(999L);
        assertThat(order.getItems()).hasSize(5);
    }
}
```

### Fixture 정의

```java
// application/src/testFixtures/java/com/ryuqq/fixture/application/PlaceOrderCommandFixture.java
public final class PlaceOrderCommandFixture {

    private PlaceOrderCommandFixture() {}

    public static PlaceOrderCommand create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long customerId = 100L;
        private int itemCount = 1;
        private BigDecimal unitPrice = new BigDecimal("10000");

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder itemCount(int itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public PlaceOrderCommand build() {
            List<OrderItemCommand> items = IntStream.range(0, itemCount)
                .mapToObj(i -> new OrderItemCommand(
                    1000L + i,
                    1,
                    unitPrice
                ))
                .toList();

            return new PlaceOrderCommand(customerId, items);
        }
    }
}
```

---

## 7) 관련 문서

- **[Command Factory Guide](./command-factory-guide.md)** - Factory 구현 가이드
- **[Command Factory ArchUnit](./command-factory-archunit.md)** - 아키텍처 규칙
- **[Query Factory Test Guide](../query/query-factory-test-guide.md)** - Query Factory 테스트
- **[Assembler Test Guide](../../assembler/assembler-test-guide.md)** - 비교: Domain → Response

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
