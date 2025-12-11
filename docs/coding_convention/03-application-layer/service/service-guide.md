# Service Guide — **UseCase 구현체 (CQRS)**

> Service는 **Port-In(UseCase) 인터페이스의 구현체**입니다.
>
> **Command**와 **Query**를 분리하여 각각의 흐름을 따릅니다.

---

## 1) 핵심 원칙

* **CQRS 분리**: Command와 Query는 완전히 다른 흐름
* **Port-In 구현**: UseCase 인터페이스를 구현
* **조율만 수행**: 비즈니스 로직은 Domain, 변환은 Factory/Assembler
* **트랜잭션 위임**: 직접 @Transactional 사용 금지, Manager/Facade에 위임

---

## 2) 패키지 구조

```
application/{bc}/
├─ service/
│  ├─ command/                       ← Command UseCase 구현
│  │  └─ PlaceOrderService.java
│  └─ query/                         ← Query UseCase 구현
│     └─ GetOrderService.java
│
├─ factory/
│  ├─ command/
│  │  └─ OrderCommandFactory.java    ← Command → Domain, PersistBundle
│  └─ query/
│     └─ OrderQueryFactory.java      ← Query → Criteria
│
├─ facade/
│  ├─ command/
│  │  └─ OrderFacade.java            ← 복잡한 Command (Manager 2개+)
│  └─ query/
│     └─ OrderQueryFacade.java       ← 복잡한 Query (ReadManager 2개+)
│
├─ manager/
│  ├─ command/
│  │  └─ OrderTransactionManager.java ← Command용 (영속화)
│  └─ query/
│     └─ OrderReadManager.java        ← Query용 (조회)
│
├─ dto/
│  ├─ command/
│  │  └─ PlaceOrderCommand.java
│  ├─ query/
│  │  └─ OrderSearchQuery.java
│  ├─ bundle/
│  │  ├─ OrderPersistBundle.java     ← Command용 Bundle
│  │  └─ OrderQueryBundle.java       ← Query용 Bundle
│  └─ response/
│     └─ OrderResponse.java
│
├─ assembler/
│  └─ OrderAssembler.java            ← Domain/Bundle → Response
│
└─ config/
   └─ TransactionEventRegistry.java  ← 커밋 후 Event 발행
```

---

## 3) Command vs Query 흐름

### Command 흐름

```
┌─────────────────────────────────────────────────────────────┐
│ [복잡한 Command] - Manager 2개 이상                          │
│                                                             │
│ Controller                                                  │
│     ↓                                                       │
│ UseCase (PlaceOrderService)                                 │
│     ↓                                                       │
│ CommandFactory.createBundle(Command) → PersistBundle        │
│     ↓                                                       │
│ Facade.persistXxx(Bundle)                                   │
│     ├─ Manager1.persist(Order)                              │
│     ├─ Manager2.persist(History)                            │
│     └─ Manager3.persist(Outbox)                             │
│     ↓                                                       │
│ EventRegistry.registerForPublish(Event)                     │
│     ↓                                                       │
│ Assembler.toResponse(Order) → Response                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ [단순 Command] - Manager 1개                                 │
│                                                             │
│ Controller                                                  │
│     ↓                                                       │
│ UseCase                                                     │
│     ↓                                                       │
│ CommandFactory.create(Command) → Domain                     │
│     ↓                                                       │
│ Manager.persist(Domain)                                     │
│     ↓                                                       │
│ Assembler.toResponse(Domain) → Response                     │
└─────────────────────────────────────────────────────────────┘
```

### Query 흐름

```
┌─────────────────────────────────────────────────────────────┐
│ [복잡한 Query] - ReadManager 2개 이상                        │
│                                                             │
│ Controller                                                  │
│     ↓                                                       │
│ UseCase (GetOrderDetailService)                             │
│     ↓                                                       │
│ QueryFactory.createCriteria(Query) → Criteria               │
│     ↓                                                       │
│ QueryFacade.fetchXxx(Criteria)                              │
│     ├─ ReadManager1.findBy(Criteria) → Order                │
│     ├─ ReadManager2.findBy(OrderId) → List<Item>            │
│     └─ ReadManager3.findBy(CustomerId) → Customer           │
│     ↓                                                       │
│ QueryBundle 반환                                             │
│     ↓                                                       │
│ Assembler.toResponse(QueryBundle) → Response                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ [단순 Query] - ReadManager 1개                               │
│                                                             │
│ Controller                                                  │
│     ↓                                                       │
│ UseCase                                                     │
│     ↓                                                       │
│ QueryFactory.createCriteria(Query) → Criteria (필요시)      │
│     ↓                                                       │
│ ReadManager.findBy(Criteria) → Domain                       │
│     ↓                                                       │
│ Assembler.toResponse(Domain) → Response                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 4) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **`@Service` 어노테이션** | UseCase 구현체는 @Service |
| **Port-In 구현** | UseCase 인터페이스 implements |
| **command/query 분리** | 패키지로 명확히 구분 |
| **@Transactional 금지** | Manager/Facade에 위임 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **객체 생성 금지** | Factory 책임 |
| **직접 Port 호출 금지** | Manager/Facade 통해 접근 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 5) 구현 예시

### Command Service (복잡한 경우)

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.bundle.OrderPersistBundle;
import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.facade.command.OrderFacade;
import com.ryuqq.application.order.factory.command.OrderCommandFactory;
import com.ryuqq.application.common.config.TransactionEventRegistry;
import com.ryuqq.application.port.in.command.PlaceOrderUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import org.springframework.stereotype.Service;

/**
 * 주문 생성 UseCase 구현체
 * - 복잡한 Command: Facade 사용 (Manager 3개 조합)
 */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderCommandFactory commandFactory;
    private final OrderFacade orderFacade;
    private final TransactionEventRegistry eventRegistry;
    private final OrderAssembler assembler;

    public PlaceOrderService(
        OrderCommandFactory commandFactory,
        OrderFacade orderFacade,
        TransactionEventRegistry eventRegistry,
        OrderAssembler assembler
    ) {
        this.commandFactory = commandFactory;
        this.orderFacade = orderFacade;
        this.eventRegistry = eventRegistry;
        this.assembler = assembler;
    }

    @Override
    public OrderResponse execute(PlaceOrderCommand command) {
        // 1. Command → Bundle (Factory)
        OrderPersistBundle bundle = commandFactory.createBundle(command);

        // 2. 영속화 (Facade - 여러 Manager 조합)
        Order savedOrder = orderFacade.persistOrderBundle(bundle);

        // 3. Event 등록 (커밋 후 발행)
        eventRegistry.registerForPublish(savedOrder.pullDomainEvents());

        // 4. Response 변환 (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

### Command Service (단순한 경우)

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.assembler.OrderAssembler;
import com.ryuqq.application.order.dto.command.UpdateOrderStatusCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.factory.command.OrderCommandFactory;
import com.ryuqq.application.order.manager.command.OrderTransactionManager;
import com.ryuqq.application.port.in.command.UpdateOrderStatusUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import org.springframework.stereotype.Service;

/**
 * 주문 상태 변경 UseCase 구현체
 * - 단순 Command: Manager 직접 호출 (1개)
 */
@Service
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderCommandFactory commandFactory;
    private final OrderTransactionManager orderManager;
    private final OrderAssembler assembler;

    public UpdateOrderStatusService(
        OrderCommandFactory commandFactory,
        OrderTransactionManager orderManager,
        OrderAssembler assembler
    ) {
        this.commandFactory = commandFactory;
        this.orderManager = orderManager;
        this.assembler = assembler;
    }

    @Override
    public OrderResponse execute(UpdateOrderStatusCommand command) {
        // 1. Command → Domain (Factory)
        Order order = commandFactory.createForStatusUpdate(command);

        // 2. 영속화 (Manager 직접 - 단일)
        Order savedOrder = orderManager.persist(order);

        // 3. Response 변환 (Assembler)
        return assembler.toResponse(savedOrder);
    }
}
```

### Query Service (복잡한 경우)

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

### Query Service (단순한 경우)

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

---

## 6) 컴포넌트 역할 요약

| 컴포넌트 | Command | Query |
|---------|---------|-------|
| **Service** | PlaceOrderService | GetOrderService |
| **Factory** | CommandFactory | QueryFactory |
| **Facade** | Facade (persist*) | QueryFacade (fetch*) |
| **Manager** | TransactionManager | ReadManager |
| **Bundle** | PersistBundle | QueryBundle |
| **Assembler** | Domain → Response | Domain/Bundle → Response |

---

## 7) 사용 기준

### Facade 사용 기준

| 조건 | Command | Query |
|------|---------|-------|
| **Manager 2개 이상** | Facade 사용 | QueryFacade 사용 |
| **Manager 1개** | Manager 직접 호출 | ReadManager 직접 호출 |

### Factory 사용 기준

| 조건 | Command | Query |
|------|---------|-------|
| **DTO → Domain/Criteria 변환 필요** | CommandFactory 사용 | QueryFactory 사용 |
| **단순 ID 조회** | Factory 불필요 | Factory 불필요 |

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: @Service 어노테이션
@Service
public class PlaceOrderService implements PlaceOrderUseCase { ... }

// ✅ Good: Port-In 인터페이스 구현
public class PlaceOrderService implements PlaceOrderUseCase { ... }

// ✅ Good: Factory, Facade/Manager, Assembler 조합
Order order = commandFactory.create(command);
Order saved = orderManager.persist(order);
return assembler.toResponse(saved);

// ✅ Good: 복잡한 경우 Facade 사용
OrderPersistBundle bundle = commandFactory.createBundle(command);
Order saved = orderFacade.persistOrderBundle(bundle);
```

### ❌ Bad

```java
// ❌ Bad: @Component 어노테이션
@Component  // ❌ @Service 사용해야 함
public class PlaceOrderService { ... }

// ❌ Bad: @Transactional 직접 사용
@Service
public class PlaceOrderService {
    @Transactional  // ❌ Manager/Facade에 위임
    public OrderResponse execute(...) { ... }
}

// ❌ Bad: Port 직접 호출
@Service
public class PlaceOrderService {
    private final OrderCommandPort orderPort;  // ❌ Manager 사용

    public OrderResponse execute(...) {
        orderPort.save(order);  // ❌
    }
}

// ❌ Bad: 객체 직접 생성
public OrderResponse execute(PlaceOrderCommand command) {
    Order order = Order.forNew(...);  // ❌ Factory 사용
}

// ❌ Bad: 비즈니스 로직 포함
public OrderResponse execute(PlaceOrderCommand command) {
    if (command.totalAmount() > MAX) {  // ❌ Domain 책임
        throw new BusinessException("Too expensive");
    }
}
```

---

## 9) 체크리스트

### Command Service
- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/command/` 패키지 위치
- [ ] CommandFactory 사용 (Command → Domain)
- [ ] 복잡: Facade 사용 / 단순: Manager 직접
- [ ] Assembler 사용 (Domain → Response)
- [ ] `@Transactional` 금지

### Query Service
- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/query/` 패키지 위치
- [ ] QueryFactory 사용 (Query → Criteria)
- [ ] 복잡: QueryFacade 사용 / 단순: ReadManager 직접
- [ ] Assembler 사용 (Domain/Bundle → Response)
- [ ] `@Transactional` 금지

---

## 10) 관련 문서

### Command 관련
- **[CommandFactory Guide](../factory/command/command-factory-guide.md)**
- **[Facade Guide](../facade/command/facade-guide.md)**
- **[TransactionManager Guide](../manager/command/transaction-manager-guide.md)**
- **[PersistBundle Guide](../dto/bundle/persist-bundle-guide.md)**

### Query 관련
- **[QueryFactory Guide](../factory/query/query-factory-guide.md)**
- **[QueryFacade Guide](../facade/query/query-facade-guide.md)**
- **[ReadManager Guide](../manager/query/read-manager-guide.md)**
- **[QueryBundle Guide](../dto/bundle/query-bundle-guide.md)**

### 공통
- **[Assembler Guide](../assembler/assembler-guide.md)**
- **[EventRegistry Guide](../event/transaction-event-registry-guide.md)**

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
