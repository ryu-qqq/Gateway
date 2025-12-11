# Query Service Test Guide — **단위 테스트**

> QueryService는 **UseCase 구현체**로 **조율 로직**을 담당합니다.
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
| **Factory 호출** | Query → Criteria 변환 호출 |
| **ReadManager/QueryFacade 호출** | 조회 메서드 호출 |
| **Assembler 호출** | Domain/Bundle → Response 변환 호출 |
| **반환값** | 올바른 Response 반환 |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/service/query/
   │      └─ GetOrderDetailService.java
   └─ test/java/
      └─ com/ryuqq/application/{bc}/service/query/
          └─ GetOrderDetailServiceTest.java
```

---

## 3) 단위 테스트 예시

### 복잡한 Query Service 테스트

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.bundle.OrderDetailQueryBundle;
import com.ryuqq.application.order.dto.query.OrderDetailQuery;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;
import com.ryuqq.application.order.facade.query.OrderQueryFacade;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.domain.order.criteria.OrderDetailCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("GetOrderDetailService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetOrderDetailServiceTest {

    @Mock
    private OrderQueryFactory queryFactory;

    @Mock
    private OrderQueryFacade queryFacade;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private GetOrderDetailService getOrderDetailService;

    // Test Fixtures
    private OrderDetailQuery query;
    private OrderDetailCriteria criteria;
    private OrderDetailQueryBundle bundle;
    private OrderDetailResponse expectedResponse;

    @BeforeEach
    void setUp() {
        query = OrderDetailQueryFixture.create();
        criteria = OrderDetailCriteriaFixture.create();
        bundle = OrderDetailQueryBundleFixture.create();
        expectedResponse = OrderDetailResponseFixture.create();
    }

    @Nested
    @DisplayName("execute 테스트")
    class ExecuteTest {

        @Test
        @DisplayName("주문 상세 조회 시 Factory, QueryFacade, Assembler가 순서대로 호출된다")
        void shouldCallCollaboratorsInOrder() {
            // given
            given(queryFactory.createDetailCriteria(query)).willReturn(criteria);
            given(queryFacade.fetchOrderDetail(criteria)).willReturn(bundle);
            given(assembler.toDetailResponse(bundle)).willReturn(expectedResponse);

            // when
            OrderDetailResponse result = getOrderDetailService.execute(query);

            // then
            then(queryFactory).should(times(1)).createDetailCriteria(query);
            then(queryFacade).should(times(1)).fetchOrderDetail(criteria);
            then(assembler).should(times(1)).toDetailResponse(bundle);
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Factory가 Criteria를 올바르게 생성하면 QueryFacade에 전달한다")
        void shouldPassCriteriaFromFactoryToFacade() {
            // given
            given(queryFactory.createDetailCriteria(query)).willReturn(criteria);
            given(queryFacade.fetchOrderDetail(criteria)).willReturn(bundle);
            given(assembler.toDetailResponse(bundle)).willReturn(expectedResponse);

            // when
            getOrderDetailService.execute(query);

            // then
            then(queryFacade).should().fetchOrderDetail(criteria);
        }

        @Test
        @DisplayName("QueryFacade가 반환한 Bundle을 Assembler에 전달한다")
        void shouldPassBundleFromFacadeToAssembler() {
            // given
            given(queryFactory.createDetailCriteria(query)).willReturn(criteria);
            given(queryFacade.fetchOrderDetail(criteria)).willReturn(bundle);
            given(assembler.toDetailResponse(bundle)).willReturn(expectedResponse);

            // when
            getOrderDetailService.execute(query);

            // then
            then(assembler).should().toDetailResponse(bundle);
        }
    }
}
```

### 단순 Query Service 테스트

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.query.OrderSearchQuery;
import com.ryuqq.application.order.dto.response.OrderListResponse;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.criteria.OrderSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@DisplayName("SearchOrdersService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class SearchOrdersServiceTest {

    @Mock
    private OrderQueryFactory queryFactory;

    @Mock
    private OrderReadManager orderReadManager;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private SearchOrdersService searchOrdersService;

    private OrderSearchQuery query;
    private OrderSearchCriteria criteria;
    private List<Order> orders;
    private OrderListResponse expectedResponse;

    @BeforeEach
    void setUp() {
        query = OrderSearchQueryFixture.create();
        criteria = OrderSearchCriteriaFixture.create();
        orders = List.of(OrderFixture.create(), OrderFixture.create());
        expectedResponse = OrderListResponseFixture.create();
    }

    @Test
    @DisplayName("주문 목록 조회 시 Factory, ReadManager, Assembler가 순서대로 호출된다")
    void shouldCallCollaboratorsInOrder() {
        // given
        given(queryFactory.createSearchCriteria(query)).willReturn(criteria);
        given(orderReadManager.findBy(criteria)).willReturn(orders);
        given(assembler.toListResponse(orders)).willReturn(expectedResponse);

        // when
        OrderListResponse result = searchOrdersService.execute(query);

        // then
        then(queryFactory).should(times(1)).createSearchCriteria(query);
        then(orderReadManager).should(times(1)).findBy(criteria);
        then(assembler).should(times(1)).toListResponse(orders);
        assertThat(result).isEqualTo(expectedResponse);
    }
}
```

### ID로 단순 조회 Service 테스트

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
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

@DisplayName("GetOrderByIdService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetOrderByIdServiceTest {

    @Mock
    private OrderReadManager orderReadManager;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private GetOrderByIdService getOrderByIdService;

    private Long orderId;
    private Order order;
    private OrderResponse expectedResponse;

    @BeforeEach
    void setUp() {
        orderId = 100L;
        order = OrderFixture.create();
        expectedResponse = OrderResponseFixture.create();
    }

    @Test
    @DisplayName("주문 단건 조회 시 ReadManager, Assembler가 순서대로 호출된다")
    void shouldCallCollaboratorsInOrder() {
        // given
        given(orderReadManager.getById(new OrderId(orderId))).willReturn(order);
        given(assembler.toResponse(order)).willReturn(expectedResponse);

        // when
        OrderResponse result = getOrderByIdService.execute(orderId);

        // then
        then(orderReadManager).should(times(1)).getById(new OrderId(orderId));
        then(assembler).should(times(1)).toResponse(order);
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("Factory 없이 ID를 직접 VO로 변환하여 조회한다")
    void shouldConvertIdToVoDirectly() {
        // given
        given(orderReadManager.getById(new OrderId(orderId))).willReturn(order);
        given(assembler.toResponse(order)).willReturn(expectedResponse);

        // when
        getOrderByIdService.execute(orderId);

        // then
        then(orderReadManager).should().getById(new OrderId(orderId));
    }
}
```

### 페이지네이션 Query Service 테스트

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.query.OrderPageQuery;
import com.ryuqq.application.order.dto.response.OrderPageResponse;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.criteria.OrderPageCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@DisplayName("GetOrderPageService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetOrderPageServiceTest {

    @Mock
    private OrderQueryFactory queryFactory;

    @Mock
    private OrderReadManager orderReadManager;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private GetOrderPageService getOrderPageService;

    private OrderPageQuery query;
    private OrderPageCriteria criteria;
    private List<Order> orders;
    private OrderPageResponse expectedResponse;

    @BeforeEach
    void setUp() {
        query = new OrderPageQuery(100L, 0, 10);
        criteria = OrderPageCriteriaFixture.create();
        orders = List.of(OrderFixture.create());
        expectedResponse = OrderPageResponseFixture.create();
    }

    @Test
    @DisplayName("페이지 조회 시 목록과 총 개수를 함께 조회한다")
    void shouldFetchListAndTotalCount() {
        // given
        given(queryFactory.createPageCriteria(query)).willReturn(criteria);
        given(orderReadManager.findBy(criteria)).willReturn(orders);
        given(orderReadManager.countBy(criteria)).willReturn(100L);
        given(assembler.toPageResponse(orders, 100L, criteria.page(), criteria.size()))
            .willReturn(expectedResponse);

        // when
        OrderPageResponse result = getOrderPageService.execute(query);

        // then
        then(orderReadManager).should().findBy(criteria);
        then(orderReadManager).should().countBy(criteria);
        assertThat(result).isEqualTo(expectedResponse);
    }
}
```

---

## 4) 테스트 체크리스트

### 협력 객체 호출 검증
- [ ] Factory 메서드가 올바른 인자로 호출됨 (필요 시)
- [ ] ReadManager/QueryFacade 메서드가 올바른 인자로 호출됨
- [ ] Assembler 메서드가 올바른 인자로 호출됨

### 호출 순서 검증
- [ ] Factory → ReadManager/QueryFacade → Assembler 순서

### 반환값 검증
- [ ] Assembler가 반환한 Response가 그대로 반환됨

### 페이지네이션 (해당 시)
- [ ] 목록 조회와 총 개수 조회가 모두 호출됨

---

## 5) Do / Don't

### ✅ Good

```java
// ✅ Good: @ExtendWith(MockitoExtension.class) 사용
@ExtendWith(MockitoExtension.class)
class GetOrderDetailServiceTest { ... }

// ✅ Good: Mock 협력 객체 주입
@Mock
private OrderQueryFactory queryFactory;

@Mock
private OrderQueryFacade queryFacade;

@InjectMocks
private GetOrderDetailService getOrderDetailService;

// ✅ Good: 협력 객체 호출 검증
then(queryFactory).should(times(1)).createDetailCriteria(query);
then(queryFacade).should(times(1)).fetchOrderDetail(criteria);
then(assembler).should(times(1)).toDetailResponse(bundle);

// ✅ Good: 반환값 검증
assertThat(result).isEqualTo(expectedResponse);

// ✅ Good: BDDMockito 스타일
given(queryFactory.createDetailCriteria(query)).willReturn(criteria);
then(queryFactory).should().createDetailCriteria(query);
```

### ❌ Bad

```java
// ❌ Bad: @SpringBootTest 사용 (단위 테스트에 불필요)
@SpringBootTest
class GetOrderDetailServiceTest { ... }

// ❌ Bad: 실제 객체 생성
OrderQueryFactory queryFactory = new OrderQueryFactory();  // ❌ Mock 사용

// ❌ Bad: 비즈니스 로직 테스트 (Domain 책임)
assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);  // ❌ Domain 테스트

// ❌ Bad: 데이터베이스 접근 테스트 (통합 테스트에서)
assertThat(orderRepository.findById(id)).isPresent();  // ❌

// ❌ Bad: 상태 변경 검증 (Query는 읽기 전용)
then(order).should().updateStatus(...);  // ❌ Query에서 상태 변경 금지
```

---

## 6) Fixture 활용

### TestFixtures 사용

```java
import com.ryuqq.fixture.application.OrderDetailQueryFixture;
import com.ryuqq.fixture.application.OrderDetailQueryBundleFixture;
import com.ryuqq.fixture.domain.OrderDetailCriteriaFixture;

@DisplayName("GetOrderDetailService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetOrderDetailServiceTest {

    @BeforeEach
    void setUp() {
        query = OrderDetailQueryFixture.create();
        criteria = OrderDetailCriteriaFixture.create();
        bundle = OrderDetailQueryBundleFixture.create();
        expectedResponse = OrderDetailResponseFixture.create();
    }
}
```

### Fixture 정의

```java
// application/src/testFixtures/java/com/ryuqq/fixture/application/OrderDetailQueryFixture.java
public final class OrderDetailQueryFixture {

    private OrderDetailQueryFixture() {}

    public static OrderDetailQuery create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long orderId = 100L;
        private boolean includeItems = true;
        private boolean includeShipping = true;
        private boolean includePayment = false;

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder includeItems(boolean includeItems) {
            this.includeItems = includeItems;
            return this;
        }

        public Builder includeShipping(boolean includeShipping) {
            this.includeShipping = includeShipping;
            return this;
        }

        public Builder includePayment(boolean includePayment) {
            this.includePayment = includePayment;
            return this;
        }

        public OrderDetailQuery build() {
            return new OrderDetailQuery(orderId, includeItems, includeShipping, includePayment);
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
        given(queryFactory.createDetailCriteria(query))
            .willThrow(new IllegalArgumentException("Invalid query"));

        // when & then
        assertThatThrownBy(() -> getOrderDetailService.execute(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid query");

        then(queryFacade).shouldHaveNoInteractions();
        then(assembler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("QueryFacade에서 예외 발생 시 그대로 전파된다")
    void shouldPropagateFacadeException() {
        // given
        given(queryFactory.createDetailCriteria(query)).willReturn(criteria);
        given(queryFacade.fetchOrderDetail(criteria))
            .willThrow(new OrderNotFoundException(new OrderId(100L)));

        // when & then
        assertThatThrownBy(() -> getOrderDetailService.execute(query))
            .isInstanceOf(OrderNotFoundException.class);

        then(assembler).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ReadManager에서 조회 결과가 없으면 예외가 전파된다")
    void shouldPropagateNotFoundExceptionFromReadManager() {
        // given
        given(orderReadManager.getById(new OrderId(orderId)))
            .willThrow(new OrderNotFoundException(new OrderId(orderId)));

        // when & then
        assertThatThrownBy(() -> getOrderByIdService.execute(orderId))
            .isInstanceOf(OrderNotFoundException.class);

        then(assembler).shouldHaveNoInteractions();
    }
}
```

---

## 8) 관련 문서

- **[QueryService Guide](./query-service-guide.md)** - Service 구현 가이드
- **[QueryService ArchUnit](./query-service-archunit.md)** - 아키텍처 규칙
- **[QueryFactory Test Guide](../../factory/query/query-factory-test-guide.md)** - Factory 테스트
- **[Assembler Test Guide](../../assembler/assembler-test-guide.md)** - Assembler 테스트

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
