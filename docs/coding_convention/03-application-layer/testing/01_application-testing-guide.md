# Application Layer 테스트 가이드

> **목적**: Application Layer 전체 테스트 전략 및 규칙 통합 문서
>
> **핵심 원칙**: Mock 기반 단위 테스트, 조율 로직 검증, 빠른 실행

---

## 1) 테스트 전략 개요

### Application Layer 테스트 특징

| 특징 | 설명 |
|------|------|
| **테스트 유형** | Mock 기반 단위 테스트 |
| **외부 의존성** | Mock 처리 (Port, Repository) |
| **실행 속도** | 밀리초 단위 (< 300ms) |
| **Mock 사용** | Mockito 기반 협력 객체 Mock |
| **검증 초점** | 조율 로직, 협력 객체 호출 순서 |

### 컴포넌트별 테스트 범위

| 컴포넌트 | 테스트 대상 | 상세 가이드 |
|----------|-----------|------------|
| **CommandService** | Factory, Manager/Facade, Assembler 호출 검증 | [command-service-test-guide.md](../service/command/command-service-test-guide.md) |
| **QueryService** | QueryPort 호출, 결과 조립 검증 | [query-service-test-guide.md](../service/query/query-service-test-guide.md) |
| **Facade** | Manager 조합, 트랜잭션 경계 검증 | [facade-test-guide.md](../facade/facade-test-guide.md) |
| **TransactionManager** | PersistencePort 호출 검증 | [transaction-manager-test-guide.md](../manager/transaction-manager-test-guide.md) |
| **Assembler** | Domain ↔ Response 변환 검증 | [assembler-test-guide.md](../assembler/assembler-test-guide.md) |
| **Factory** | Command/Query → Domain 변환 검증 | [command-factory-test-guide.md](../factory/command/command-factory-test-guide.md) |
| **EventListener** | 이벤트 수신 및 처리 검증 | [event-listener-test-guide.md](../listener/event-listener-test-guide.md) |
| **Scheduler** | 스케줄러 실행 검증 | [scheduler-test-guide.md](../scheduler/scheduler-test-guide.md) |

---

## 2) 테스트 태그 규칙 (Zero-Tolerance)

### 필수 태그 조합

```java
// ✅ 모든 Application Layer 테스트에 필수
@Tag("unit")         // 단위 테스트 표시
@Tag("application")  // Application Layer 표시
```

### 컴포넌트별 추가 태그

| 컴포넌트 | 추가 태그 | 전체 태그 조합 |
|----------|----------|---------------|
| CommandService | `@Tag("service")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("service")` |
| QueryService | `@Tag("service")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("service")` |
| Facade | `@Tag("facade")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("facade")` |
| Manager | `@Tag("manager")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("manager")` |
| Assembler | `@Tag("assembler")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("assembler")` |
| Factory | `@Tag("factory")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("factory")` |
| EventListener | `@Tag("listener")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("listener")` |
| Scheduler | `@Tag("scheduler")` | `@Tag("unit")`, `@Tag("application")`, `@Tag("scheduler")` |

### 태그 기반 테스트 실행

```bash
# Application Layer 전체 테스트
./gradlew :application:test

# 단위 테스트만 실행
./gradlew :application:test -PincludeTags=unit

# Service 테스트만 실행
./gradlew :application:test -PincludeTags=service

# Facade 테스트만 실행
./gradlew :application:test -PincludeTags=facade

# Assembler 테스트만 실행
./gradlew :application:test -PincludeTags=assembler
```

---

## 3) 테스트 구조 규칙

### 디렉토리 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/
   │      ├─ service/
   │      │   ├─ command/
   │      │   │   └─ PlaceOrderService.java
   │      │   └─ query/
   │      │       └─ GetOrderService.java
   │      ├─ facade/
   │      │   └─ command/
   │      │       └─ OrderFacade.java
   │      ├─ manager/
   │      │   └─ transaction/
   │      │       └─ OrderTransactionManager.java
   │      ├─ assembler/
   │      │   └─ OrderAssembler.java
   │      └─ factory/
   │          └─ command/
   │              └─ OrderCommandFactory.java
   │
   └─ test/java/
      └─ com/ryuqq/application/{bc}/
          ├─ service/
          │   ├─ command/
          │   │   └─ PlaceOrderServiceTest.java  # ✅ 동일 패키지
          │   └─ query/
          │       └─ GetOrderServiceTest.java
          ├─ facade/
          │   └─ command/
          │       └─ OrderFacadeTest.java
          ├─ manager/
          │   └─ transaction/
          │       └─ OrderTransactionManagerTest.java
          ├─ assembler/
          │   └─ OrderAssemblerTest.java
          └─ factory/
              └─ command/
                  └─ OrderCommandFactoryTest.java
```

### 테스트 클래스 네이밍

| 패턴 | 용도 | 예시 |
|------|------|------|
| `{ClassName}Test.java` | 단위 테스트 | `PlaceOrderServiceTest.java` |
| `{ClassName}IntegrationTest.java` | 통합 테스트 (필요 시) | `OrderFacadeIntegrationTest.java` |

---

## 4) 테스트 템플릿

### CommandService 테스트

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.facade.command.OrderFacade;
import com.ryuqq.application.order.factory.command.OrderCommandFactory;
import com.ryuqq.domain.order.aggregate.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * PlaceOrderService 단위 테스트
 *
 * <p>조율 로직 및 협력 객체 호출 순서 검증
 *
 * @see PlaceOrderService
 */
@Tag("unit")
@Tag("application")
@Tag("service")
@DisplayName("PlaceOrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderCommandFactory commandFactory;

    @Mock
    private OrderFacade orderFacade;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private PlaceOrderService placeOrderService;

    @Nested
    @DisplayName("placeOrder()")
    class PlaceOrderTests {

        @Test
        @DisplayName("정상 주문 시 Factory → Facade → Assembler 순서로 호출")
        void placeOrder_ShouldCallCollaboratorsInOrder() {
            // Given
            PlaceOrderCommand command = PlaceOrderCommand.of(1L, List.of());
            Order order = Orders.pending();
            OrderResponse expectedResponse = OrderResponse.of(1L);

            given(commandFactory.createOrder(command)).willReturn(order);
            given(orderFacade.place(order)).willReturn(order);
            given(assembler.toResponse(order)).willReturn(expectedResponse);

            // When
            OrderResponse result = placeOrderService.placeOrder(command);

            // Then
            assertThat(result).isEqualTo(expectedResponse);

            // ✅ 호출 순서 검증
            then(commandFactory).should().createOrder(command);
            then(orderFacade).should().place(order);
            then(assembler).should().toResponse(order);
        }

        @Test
        @DisplayName("Factory에서 예외 발생 시 Facade는 호출되지 않음")
        void placeOrder_ShouldNotCallFacade_WhenFactoryThrows() {
            // Given
            PlaceOrderCommand command = PlaceOrderCommand.of(1L, List.of());
            given(commandFactory.createOrder(command))
                .willThrow(new IllegalArgumentException("Invalid command"));

            // When & Then
            assertThatThrownBy(() -> placeOrderService.placeOrder(command))
                .isInstanceOf(IllegalArgumentException.class);

            then(orderFacade).shouldHaveNoInteractions();
            then(assembler).shouldHaveNoInteractions();
        }
    }
}
```

### QueryService 테스트

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.port.out.OrderQueryPort;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * GetOrderService 단위 테스트
 *
 * @see GetOrderService
 */
@Tag("unit")
@Tag("application")
@Tag("service")
@DisplayName("GetOrderService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private OrderQueryPort orderQueryPort;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private GetOrderService getOrderService;

    @Nested
    @DisplayName("getOrder()")
    class GetOrderTests {

        @Test
        @DisplayName("존재하는 주문 조회 시 Response 반환")
        void getOrder_ShouldReturnResponse_WhenOrderExists() {
            // Given
            OrderId orderId = OrderId.of(1L);
            Order order = Orders.confirmed();
            OrderResponse expectedResponse = OrderResponse.of(1L);

            given(orderQueryPort.findById(orderId)).willReturn(Optional.of(order));
            given(assembler.toResponse(order)).willReturn(expectedResponse);

            // When
            OrderResponse result = getOrderService.getOrder(orderId);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외 발생")
        void getOrder_ShouldThrowException_WhenOrderNotFound() {
            // Given
            OrderId orderId = OrderId.of(999L);
            given(orderQueryPort.findById(orderId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> getOrderService.getOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);

            then(assembler).shouldHaveNoInteractions();
        }
    }
}
```

### Assembler 테스트

```java
package com.ryuqq.application.order.assembler;

import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.mother.Orders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderAssembler 단위 테스트
 *
 * <p>Domain → Response 변환 검증
 *
 * @see OrderAssembler
 */
@Tag("unit")
@Tag("application")
@Tag("assembler")
@DisplayName("OrderAssembler 단위 테스트")
class OrderAssemblerTest {

    private final OrderAssembler assembler = new OrderAssembler();

    @Nested
    @DisplayName("toResponse()")
    class ToResponseTests {

        @Test
        @DisplayName("Order → OrderResponse 변환")
        void toResponse_ShouldConvertOrderToResponse() {
            // Given
            Order order = Orders.confirmed();

            // When
            OrderResponse response = assembler.toResponse(order);

            // Then
            assertThat(response.orderId()).isEqualTo(order.id().value());
            assertThat(response.status()).isEqualTo(order.status().name());
        }

        @Test
        @DisplayName("null Order 시 예외 발생")
        void toResponse_ShouldThrowException_WhenOrderIsNull() {
            // When & Then
            assertThatThrownBy(() -> assembler.toResponse(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toResponseList()")
    class ToResponseListTests {

        @Test
        @DisplayName("Order 리스트 → Response 리스트 변환")
        void toResponseList_ShouldConvertOrderListToResponseList() {
            // Given
            List<Order> orders = List.of(Orders.pending(), Orders.confirmed());

            // When
            List<OrderResponse> responses = assembler.toResponseList(orders);

            // Then
            assertThat(responses).hasSize(2);
        }
    }
}
```

### Factory 테스트

```java
package com.ryuqq.application.order.factory.command;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.domain.order.aggregate.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderCommandFactory 단위 테스트
 *
 * <p>Command → Domain 변환 검증
 *
 * @see OrderCommandFactory
 */
@Tag("unit")
@Tag("application")
@Tag("factory")
@DisplayName("OrderCommandFactory 단위 테스트")
class OrderCommandFactoryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    private final OrderCommandFactory factory = new OrderCommandFactory(FIXED_CLOCK);

    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("PlaceOrderCommand → Order 변환")
        void createOrder_ShouldConvertCommandToOrder() {
            // Given
            PlaceOrderCommand command = PlaceOrderCommand.of(
                1L,
                List.of(OrderItemCommand.of(100L, 2))
            );

            // When
            Order order = factory.createOrder(command);

            // Then
            assertThat(order.id()).isNull();  // 신규 생성은 ID null
            assertThat(order.customerId().value()).isEqualTo(1L);
        }
    }
}
```

---

## 5) Mock 사용 규칙

### BDD Mockito 스타일 필수

```java
// ✅ BDD 스타일 (권장)
given(mockPort.findById(id)).willReturn(Optional.of(entity));
then(mockPort).should().findById(id);

// ❌ Classic 스타일 (금지)
when(mockPort.findById(id)).thenReturn(Optional.of(entity));
verify(mockPort).findById(id);
```

### Mock 대상

| 대상 | Mock 여부 | 이유 |
|------|----------|------|
| **Port (Out)** | ✅ Mock | 외부 의존성 |
| **Facade/Manager** | ✅ Mock | 협력 객체 |
| **Assembler** | ✅ Mock (Service 테스트 시) | 협력 객체 |
| **Factory** | ✅ Mock (Service 테스트 시) | 협력 객체 |
| **Domain Object** | ❌ 실제 객체 | 비즈니스 로직 포함 |
| **DTO** | ❌ 실제 객체 | 단순 데이터 |

---

## 6) 금지 사항 (Zero-Tolerance)

### 절대 금지

| 항목 | 이유 |
|------|------|
| ❌ `@SpringBootTest` | 단위 테스트에서 Spring Context 로딩 금지 |
| ❌ `@DataJpaTest` | Application Layer는 DB 직접 접근 금지 |
| ❌ `@Transactional` 테스트 | 실제 트랜잭션 테스트는 통합 테스트에서 |
| ❌ 실제 DB 연결 | Mock으로 대체 |
| ❌ 실제 외부 API 호출 | Mock으로 대체 |

### 허용되는 경우

| 항목 | 허용 조건 |
|------|----------|
| ✅ `@ExtendWith(MockitoExtension.class)` | Mockito 사용 시 필수 |
| ✅ `Clock` Mock | 시간 관련 테스트 |

---

## 7) 실행 속도 기준

| 기준 | 임계값 | 조치 |
|------|--------|------|
| 단일 테스트 | < 50ms | 정상 |
| 단일 테스트 | 50-300ms | 검토 필요 |
| 단일 테스트 | > 300ms | 리팩토링 필수 |
| 전체 Application 테스트 | < 30초 | 정상 |

---

## 8) 체크리스트

### 테스트 작성 전

- [ ] 테스트 대상 컴포넌트 확인 (Service, Facade, Manager, Assembler, Factory)
- [ ] 해당 컴포넌트 테스트 가이드 참조
- [ ] 협력 객체 목록 파악

### 테스트 작성 중

- [ ] `@Tag("unit")`, `@Tag("application")` 필수 적용
- [ ] 컴포넌트별 추가 태그 적용 (`@Tag("service")` 등)
- [ ] `@ExtendWith(MockitoExtension.class)` 적용
- [ ] BDD Mockito 스타일 (`given`/`then`) 사용
- [ ] `@DisplayName` 한글로 작성
- [ ] `@Nested`로 관심사 분리

### 테스트 작성 후

- [ ] 실행 속도 < 300ms 확인
- [ ] Spring Context 로딩 없음 확인
- [ ] `./gradlew :application:test` 전체 통과

---

## 참조 문서

- [CommandService 테스트 가이드](../service/command/command-service-test-guide.md)
- [QueryService 테스트 가이드](../service/query/query-service-test-guide.md)
- [Facade 테스트 가이드](../facade/facade-test-guide.md)
- [TransactionManager 테스트 가이드](../manager/transaction-manager-test-guide.md)
- [Assembler 테스트 가이드](../assembler/assembler-test-guide.md)
- [Factory 테스트 가이드](../factory/command/command-factory-test-guide.md)
