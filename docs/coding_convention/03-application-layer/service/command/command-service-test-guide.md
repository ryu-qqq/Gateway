# Command Service Test Guide — **단위 테스트**

> CommandService는 **UseCase 구현체**로 **조율 로직**을 담당합니다.
>
> **Mock 기반 단위 테스트**로 협력 객체 호출을 검증합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | 조율 로직 검증 | Service + Mock 협력 객체 |

### 테스트 포인트

| 항목 | 검증 내용 |
|------|----------|
| **Factory 호출** | Command → Domain/Bundle 변환 호출 |
| **Manager/Facade 호출** | 영속화 메서드 호출 |
| **Assembler 호출** | Domain → Response 변환 호출 |
| **EventRegistry 호출** | 이벤트 등록 호출 (필요 시) |
| **반환값** | 올바른 Response 반환 |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/service/command/
   │      └─ PlaceOrderService.java
   └─ test/java/
      └─ com/ryuqq/application/{bc}/service/command/
          └─ PlaceOrderServiceTest.java
```

---

## 3) 단위 테스트 예시

### 복잡한 Command Service 테스트

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.common.config.TransactionEventRegistry;
import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.bundle.OrderPersistBundle;
import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.facade.command.OrderFacade;
import com.ryuqq.application.order.factory.command.OrderCommandFactory;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("PlaceOrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderCommandFactory commandFactory;

    @Mock
    private OrderFacade orderFacade;

    @Mock
    private TransactionEventRegistry eventRegistry;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private PlaceOrderService placeOrderService;

    // Test Fixtures
    private PlaceOrderCommand command;
    private OrderPersistBundle bundle;
    private Order savedOrder;
    private OrderResponse expectedResponse;
    private List<OrderPlacedEvent> domainEvents;

    @BeforeEach
    void setUp() {
        // Fixture 설정
        command = PlaceOrderCommandFixture.create();
        bundle = OrderPersistBundleFixture.create();
        savedOrder = OrderFixture.create();
        expectedResponse = OrderResponseFixture.create();
        domainEvents = List.of(new OrderPlacedEvent(savedOrder.getId()));
    }

    @Nested
    @DisplayName("execute 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("주문 생성 시 Factory, Facade, Assembler가 순서대로 호출된다")
        void shouldCallCollaboratorsInOrder() {
            // given
            given(commandFactory.createBundle(command)).willReturn(bundle);
            given(orderFacade.persistOrderBundle(bundle)).willReturn(savedOrder);
            given(savedOrder.pullDomainEvents()).willReturn(domainEvents);
            given(assembler.toResponse(savedOrder)).willReturn(expectedResponse);

            // when
            OrderResponse result = placeOrderService.execute(command);

            // then
            then(commandFactory).should(times(1)).createBundle(command);
            then(orderFacade).should(times(1)).persistOrderBundle(bundle);
            then(eventRegistry).should(times(1)).registerForPublish(domainEvents);
            then(assembler).should(times(1)).toResponse(savedOrder);
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Factory가 Bundle을 올바르게 생성하면 Facade에 전달한다")
        void shouldPassBundleFromFactoryToFacade() {
            // given
            given(commandFactory.createBundle(command)).willReturn(bundle);
            given(orderFacade.persistOrderBundle(bundle)).willReturn(savedOrder);
            given(savedOrder.pullDomainEvents()).willReturn(domainEvents);
            given(assembler.toResponse(savedOrder)).willReturn(expectedResponse);

            // when
            placeOrderService.execute(command);

            // then
            then(orderFacade).should().persistOrderBundle(bundle);
        }

        @Test
        @DisplayName("Facade가 반환한 Domain을 Assembler에 전달한다")
        void shouldPassDomainFromFacadeToAssembler() {
            // given
            given(commandFactory.createBundle(command)).willReturn(bundle);
            given(orderFacade.persistOrderBundle(bundle)).willReturn(savedOrder);
            given(savedOrder.pullDomainEvents()).willReturn(domainEvents);
            given(assembler.toResponse(savedOrder)).willReturn(expectedResponse);

            // when
            placeOrderService.execute(command);

            // then
            then(assembler).should().toResponse(savedOrder);
        }

        @Test
        @DisplayName("Domain Event가 EventRegistry에 등록된다")
        void shouldRegisterDomainEventsToEventRegistry() {
            // given
            given(commandFactory.createBundle(command)).willReturn(bundle);
            given(orderFacade.persistOrderBundle(bundle)).willReturn(savedOrder);
            given(savedOrder.pullDomainEvents()).willReturn(domainEvents);
            given(assembler.toResponse(savedOrder)).willReturn(expectedResponse);

            // when
            placeOrderService.execute(command);

            // then
            then(eventRegistry).should().registerForPublish(domainEvents);
        }
    }
}
```

### 단순 Command Service 테스트

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.command.UpdateOrderStatusCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.factory.command.OrderCommandFactory;
import com.ryuqq.application.order.manager.command.OrderTransactionManager;
import com.ryuqq.domain.order.aggregate.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("UpdateOrderStatusService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusServiceTest {

    @Mock
    private OrderCommandFactory commandFactory;

    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private UpdateOrderStatusService updateOrderStatusService;

    private UpdateOrderStatusCommand command;
    private Order order;
    private Order savedOrder;
    private OrderResponse expectedResponse;

    @BeforeEach
    void setUp() {
        command = UpdateOrderStatusCommandFixture.create();
        order = OrderFixture.create();
        savedOrder = OrderFixture.create();
        expectedResponse = OrderResponseFixture.create();
    }

    @Test
    @DisplayName("주문 상태 변경 시 Factory, Manager, Assembler가 순서대로 호출된다")
    void shouldCallCollaboratorsInOrder() {
        // given
        given(commandFactory.createForStatusUpdate(command)).willReturn(order);
        given(orderManager.persist(order)).willReturn(savedOrder);
        given(assembler.toResponse(savedOrder)).willReturn(expectedResponse);

        // when
        OrderResponse result = updateOrderStatusService.execute(command);

        // then
        then(commandFactory).should(times(1)).createForStatusUpdate(command);
        then(orderManager).should(times(1)).persist(order);
        then(assembler).should(times(1)).toResponse(savedOrder);
        assertThat(result).isEqualTo(expectedResponse);
    }
}
```

### void 반환 Command Service 테스트

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.dto.command.CancelOrderCommand;
import com.ryuqq.application.order.manager.command.OrderTransactionManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("CancelOrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock
    private OrderTransactionManager orderManager;

    @Mock
    private Order order;

    @InjectMocks
    private CancelOrderService cancelOrderService;

    private CancelOrderCommand command;

    @BeforeEach
    void setUp() {
        command = new CancelOrderCommand(100L, "고객 요청");
    }

    @Test
    @DisplayName("주문 취소 시 조회 → 취소 → 영속화 순서로 호출된다")
    void shouldCallCollaboratorsInOrder() {
        // given
        given(orderManager.getById(new OrderId(100L))).willReturn(order);

        // when
        cancelOrderService.execute(command);

        // then
        then(orderManager).should(times(1)).getById(new OrderId(100L));
        then(order).should(times(1)).cancel("고객 요청");
        then(orderManager).should(times(1)).persist(order);
    }
}
```

---

## 4) 테스트 체크리스트

### 협력 객체 호출 검증
- [ ] Factory 메서드가 올바른 인자로 호출됨
- [ ] Manager/Facade 메서드가 올바른 인자로 호출됨
- [ ] Assembler 메서드가 올바른 인자로 호출됨
- [ ] EventRegistry 메서드가 올바른 인자로 호출됨 (필요 시)

### 호출 순서 검증
- [ ] Factory → Manager/Facade → Assembler 순서
- [ ] Event 등록이 영속화 후에 실행됨

### 반환값 검증
- [ ] Assembler가 반환한 Response가 그대로 반환됨
- [ ] void 메서드의 경우 예외 없이 완료됨

---

## 5) Do / Don't

### ✅ Good

```java
// ✅ Good: @ExtendWith(MockitoExtension.class) 사용
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest { ... }

// ✅ Good: Mock 협력 객체 주입
@Mock
private OrderCommandFactory commandFactory;

@Mock
private OrderFacade orderFacade;

@InjectMocks
private PlaceOrderService placeOrderService;

// ✅ Good: 협력 객체 호출 검증
then(commandFactory).should(times(1)).createBundle(command);
then(orderFacade).should(times(1)).persistOrderBundle(bundle);
then(assembler).should(times(1)).toResponse(savedOrder);

// ✅ Good: 반환값 검증
assertThat(result).isEqualTo(expectedResponse);

// ✅ Good: BDDMockito 스타일
given(commandFactory.createBundle(command)).willReturn(bundle);
then(commandFactory).should().createBundle(command);
```

### ❌ Bad

```java
// ❌ Bad: @SpringBootTest 사용 (단위 테스트에 불필요)
@SpringBootTest
class PlaceOrderServiceTest { ... }

// ❌ Bad: 실제 객체 생성
OrderCommandFactory commandFactory = new OrderCommandFactory();  // ❌ Mock 사용

// ❌ Bad: 비즈니스 로직 테스트 (Domain 책임)
assertThat(order.getTotalAmount()).isGreaterThan(0);  // ❌ Domain 테스트

// ❌ Bad: 트랜잭션 테스트 (Service는 트랜잭션 없음)
@Transactional
void testExecute() { ... }  // ❌

// ❌ Bad: 데이터베이스 접근 테스트 (통합 테스트에서)
assertThat(orderRepository.findById(id)).isPresent();  // ❌
```

---

## 6) Fixture 활용

### TestFixtures 사용

```java
import com.ryuqq.fixture.application.PlaceOrderCommandFixture;
import com.ryuqq.fixture.application.OrderPersistBundleFixture;
import com.ryuqq.fixture.domain.OrderFixture;

@DisplayName("PlaceOrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @BeforeEach
    void setUp() {
        command = PlaceOrderCommandFixture.create();
        bundle = OrderPersistBundleFixture.create();
        savedOrder = OrderFixture.create();
        expectedResponse = OrderResponseFixture.create();
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
        private List<OrderItemCommand> items = List.of(
            new OrderItemCommand(1001L, 2, new BigDecimal("10000"))
        );

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder items(List<OrderItemCommand> items) {
            this.items = items;
            return this;
        }

        public PlaceOrderCommand build() {
            return new PlaceOrderCommand(customerId, items);
        }
    }
}
```

---

## 7) 예외 테스트

### 협력 객체 예외 전파 테스트

```java
@Nested
@DisplayName("예외 상황 테스트")
class ExceptionTest {

    @Test
    @DisplayName("Factory에서 예외 발생 시 그대로 전파된다")
    void shouldPropagateFactoryException() {
        // given
        given(commandFactory.createBundle(command))
            .willThrow(new IllegalArgumentException("Invalid command"));

        // when & then
        assertThatThrownBy(() -> placeOrderService.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid command");

        then(orderFacade).shouldHaveNoInteractions();
        then(assembler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Facade에서 예외 발생 시 그대로 전파된다")
    void shouldPropagateFacadeException() {
        // given
        given(commandFactory.createBundle(command)).willReturn(bundle);
        given(orderFacade.persistOrderBundle(bundle))
            .willThrow(new RuntimeException("Persistence failed"));

        // when & then
        assertThatThrownBy(() -> placeOrderService.execute(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Persistence failed");

        then(assembler).shouldHaveNoInteractions();
    }
}
```

---

## 8) 관련 문서

- **[CommandService Guide](./command-service-guide.md)** - Service 구현 가이드
- **[CommandService ArchUnit](./command-service-archunit.md)** - 아키텍처 규칙
- **[CommandFactory Test Guide](../../factory/command/command-factory-test-guide.md)** - Factory 테스트
- **[Assembler Test Guide](../../assembler/assembler-test-guide.md)** - Assembler 테스트

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
