# Query Service Guide — **UseCase 구현체**

> QueryService는 **Port-In(UseCase) 인터페이스의 구현체**입니다.
>
> **Query → Criteria → 조회 → Response** 흐름을 조율합니다.

---

## 1) 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Port-In 구현** | UseCase 인터페이스를 구현 |
| **조율만 수행** | 비즈니스 로직은 Domain, 변환은 Factory/Assembler |
| **읽기 전용** | 상태 변경 없음, 조회만 수행 |
| **Lombok 금지** | 생성자 직접 작성 (Plain Java) |

---

## 2) 패키지 구조

```
application/{bc}/
├─ service/
│  └─ query/                          ← Query UseCase 구현
│     ├─ GetOrderDetailService.java   ← 복잡한 Query (QueryFacade 사용)
│     └─ SearchOrdersService.java     ← 단순 Query (ReadManager 직접)
│
├─ factory/query/
│  └─ OrderQueryFactory.java          ← Query → Criteria
│
├─ facade/query/
│  └─ OrderQueryFacade.java           ← 복잡한 Query (ReadManager 2개+)
│
├─ manager/query/
│  └─ OrderReadManager.java           ← 단일 조회 담당
│
├─ assembler/
│  └─ OrderAssembler.java             ← Domain/Bundle → Response
│
└─ port/in/query/
   └─ GetOrderDetailUseCase.java      ← Port-In 인터페이스
```

---

## 3) Query 흐름

### 복잡한 Query (ReadManager 2개 이상)

```
Controller
    ↓
UseCase (GetOrderDetailService)
    ↓
QueryFactory.createCriteria(Query) → Criteria
    ↓
QueryFacade.fetchXxx(Criteria)
    ├─ ReadManager1.findBy(Criteria) → Order
    ├─ ReadManager2.findBy(OrderId) → List<Item>
    └─ ReadManager3.findBy(CustomerId) → Customer
    ↓
QueryBundle 반환
    ↓
Assembler.toResponse(QueryBundle) → Response
```

### 단순 Query (ReadManager 1개)

```
Controller
    ↓
UseCase
    ↓
QueryFactory.createCriteria(Query) → Criteria (필요시)
    ↓
ReadManager.findBy(Criteria) → Domain
    ↓
Assembler.toResponse(Domain) → Response
```

---

## 4) 사용 기준

### QueryFacade vs ReadManager 직접 호출

| 조건 | 사용 |
|------|------|
| **ReadManager 2개 이상 조합** | QueryFacade 사용 |
| **ReadManager 1개** | ReadManager 직접 호출 |

### Factory 사용 기준

| 조건 | 사용 |
|------|------|
| **Query → Criteria 변환 필요** | QueryFactory 사용 |
| **단순 ID 조회** | Factory 불필요 |

---

## 5) 구현 예시

### 복잡한 Query Service

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.bundle.OrderDetailQueryBundle;
import com.ryuqq.application.order.dto.query.OrderDetailQuery;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;
import com.ryuqq.application.order.facade.query.OrderQueryFacade;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.application.port.in.query.GetOrderDetailUseCase;
import com.ryuqq.domain.order.criteria.OrderDetailCriteria;
import org.springframework.stereotype.Service;

/**
 * 주문 상세 조회 UseCase 구현체
 * - 복잡한 Query: QueryFacade 사용 (ReadManager 3개 조합)
 */
@Service
public class GetOrderDetailService implements GetOrderDetailUseCase {

    private final OrderQueryFactory queryFactory;
    private final OrderQueryFacade queryFacade;
    private final OrderAssembler assembler;

    public GetOrderDetailService(
        OrderQueryFactory queryFactory,
        OrderQueryFacade queryFacade,
        OrderAssembler assembler
    ) {
        this.queryFactory = queryFactory;
        this.queryFacade = queryFacade;
        this.assembler = assembler;
    }

    @Override
    public OrderDetailResponse execute(OrderDetailQuery query) {
        // 1. Query → Criteria (Factory)
        OrderDetailCriteria criteria = queryFactory.createDetailCriteria(query);

        // 2. 조회 (QueryFacade - 여러 ReadManager 조합)
        OrderDetailQueryBundle bundle = queryFacade.fetchOrderDetail(criteria);

        // 3. Response 변환 (Assembler)
        return assembler.toDetailResponse(bundle);
    }
}
```

### 단순 Query Service

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.query.OrderSearchQuery;
import com.ryuqq.application.order.dto.response.OrderListResponse;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.application.port.in.query.SearchOrdersUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.criteria.OrderSearchCriteria;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 주문 목록 조회 UseCase 구현체
 * - 단순 Query: ReadManager 직접 호출 (1개)
 */
@Service
public class SearchOrdersService implements SearchOrdersUseCase {

    private final OrderQueryFactory queryFactory;
    private final OrderReadManager orderReadManager;
    private final OrderAssembler assembler;

    public SearchOrdersService(
        OrderQueryFactory queryFactory,
        OrderReadManager orderReadManager,
        OrderAssembler assembler
    ) {
        this.queryFactory = queryFactory;
        this.orderReadManager = orderReadManager;
        this.assembler = assembler;
    }

    @Override
    public OrderListResponse execute(OrderSearchQuery query) {
        // 1. Query → Criteria (Factory)
        OrderSearchCriteria criteria = queryFactory.createSearchCriteria(query);

        // 2. 조회 (ReadManager 직접 - 단일)
        List<Order> orders = orderReadManager.findBy(criteria);

        // 3. Response 변환 (Assembler)
        return assembler.toListResponse(orders);
    }
}
```

### ID로 단순 조회 Service

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.application.port.in.query.GetOrderByIdUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import org.springframework.stereotype.Service;

/**
 * 주문 단건 조회 UseCase 구현체
 * - 단순 ID 조회: Factory 불필요
 */
@Service
public class GetOrderByIdService implements GetOrderByIdUseCase {

    private final OrderReadManager orderReadManager;
    private final OrderAssembler assembler;

    public GetOrderByIdService(
        OrderReadManager orderReadManager,
        OrderAssembler assembler
    ) {
        this.orderReadManager = orderReadManager;
        this.assembler = assembler;
    }

    @Override
    public OrderResponse execute(Long orderId) {
        // 1. 조회 (ReadManager - ID 직접 사용)
        Order order = orderReadManager.getById(new OrderId(orderId));

        // 2. Response 변환 (Assembler)
        return assembler.toResponse(order);
    }
}
```

### 페이지네이션 Query Service

```java
package com.ryuqq.application.order.service.query;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.query.OrderPageQuery;
import com.ryuqq.application.order.dto.response.OrderPageResponse;
import com.ryuqq.application.order.factory.query.OrderQueryFactory;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.application.port.in.query.GetOrderPageUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.criteria.OrderPageCriteria;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 주문 페이지 조회 UseCase 구현체
 */
@Service
public class GetOrderPageService implements GetOrderPageUseCase {

    private final OrderQueryFactory queryFactory;
    private final OrderReadManager orderReadManager;
    private final OrderAssembler assembler;

    public GetOrderPageService(
        OrderQueryFactory queryFactory,
        OrderReadManager orderReadManager,
        OrderAssembler assembler
    ) {
        this.queryFactory = queryFactory;
        this.orderReadManager = orderReadManager;
        this.assembler = assembler;
    }

    @Override
    public OrderPageResponse execute(OrderPageQuery query) {
        // 1. Query → Criteria (Factory)
        OrderPageCriteria criteria = queryFactory.createPageCriteria(query);

        // 2. 조회 (ReadManager)
        List<Order> orders = orderReadManager.findBy(criteria);
        long totalCount = orderReadManager.countBy(criteria);

        // 3. Response 변환 (Assembler)
        return assembler.toPageResponse(orders, totalCount, criteria.page(), criteria.size());
    }
}
```

---

## 6) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `@Service` 어노테이션 | UseCase 구현체 표시 | 빌드 실패 |
| Port-In 구현 | UseCase 인터페이스 implements | 빌드 실패 |
| `service/query/` 패키지 | 올바른 위치 | 빌드 실패 |
| `@Transactional` 금지 | 읽기 전용, 필요 없음 | 빌드 실패 |
| Port 직접 호출 금지 | ReadManager/QueryFacade 통해 접근 | 빌드 실패 |
| 상태 변경 금지 | 조회만 수행 | 코드 리뷰 |
| Lombok 금지 | Plain Java | 빌드 실패 |

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Service 어노테이션
@Service
public class GetOrderDetailService implements GetOrderDetailUseCase { ... }

// ✅ Good: Port-In 인터페이스 구현
public class GetOrderDetailService implements GetOrderDetailUseCase { ... }

// ✅ Good: Factory, QueryFacade/ReadManager, Assembler 조합
OrderDetailCriteria criteria = queryFactory.createDetailCriteria(query);
OrderDetailQueryBundle bundle = queryFacade.fetchOrderDetail(criteria);
return assembler.toDetailResponse(bundle);

// ✅ Good: 단순한 경우 ReadManager 직접 호출
OrderSearchCriteria criteria = queryFactory.createSearchCriteria(query);
List<Order> orders = orderReadManager.findBy(criteria);
return assembler.toListResponse(orders);

// ✅ Good: 명시적 생성자 (Lombok 금지)
public GetOrderDetailService(
    OrderQueryFactory queryFactory,
    OrderQueryFacade queryFacade,
    OrderAssembler assembler
) {
    this.queryFactory = queryFactory;
    this.queryFacade = queryFacade;
    this.assembler = assembler;
}
```

### ❌ Bad

```java
// ❌ Bad: @Component 어노테이션
@Component  // ❌ @Service 사용해야 함
public class GetOrderDetailService { ... }

// ❌ Bad: @Transactional 사용 (읽기 전용, 불필요)
@Service
public class GetOrderDetailService {
    @Transactional(readOnly = true)  // ❌ 불필요
    public OrderDetailResponse execute(...) { ... }
}

// ❌ Bad: Port 직접 호출
@Service
public class GetOrderDetailService {
    private final OrderQueryPort orderPort;  // ❌ ReadManager 사용

    public OrderDetailResponse execute(...) {
        orderPort.findById(id);  // ❌
    }
}

// ❌ Bad: 상태 변경 (Query에서 금지)
public OrderDetailResponse execute(OrderDetailQuery query) {
    Order order = orderReadManager.getById(orderId);
    order.updateStatus(...);  // ❌ Command 책임
}

// ❌ Bad: Lombok 사용
@Service
@RequiredArgsConstructor  // ❌ Lombok 금지
public class GetOrderDetailService { ... }
```

---

## 8) 체크리스트

- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/query/` 패키지 위치
- [ ] QueryFactory 사용 (Query → Criteria)
- [ ] 복잡: QueryFacade 사용 / 단순: ReadManager 직접
- [ ] Assembler 사용 (Domain/Bundle → Response)
- [ ] `@Transactional` 금지
- [ ] Port 직접 호출 금지
- [ ] 상태 변경 금지
- [ ] Lombok 금지 (명시적 생성자)

---

## 9) 관련 문서

- **[Service Guide](../service-guide.md)** - 전체 Service 가이드
- **[QueryService ArchUnit](./query-service-archunit.md)** - 아키텍처 규칙
- **[QueryService Test Guide](./query-service-test-guide.md)** - 테스트 가이드
- **[QueryFactory Guide](../../factory/query/query-factory-guide.md)** - Factory 구현
- **[ReadManager Guide](../../manager/query/read-manager-guide.md)** - Manager 구현
- **[QueryFacade Guide](../../facade/query/query-facade-guide.md)** - Facade 구현
- **[Assembler Guide](../../assembler/assembler-guide.md)** - Assembler 구현

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
