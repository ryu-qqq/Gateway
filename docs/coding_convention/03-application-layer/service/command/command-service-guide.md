# Command Service Guide — **UseCase 구현체**

> CommandService는 **Port-In(UseCase) 인터페이스의 구현체**입니다.
>
> **Command → Domain → 영속화 → Response** 흐름을 조율합니다.

---

## 1) 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Port-In 구현** | UseCase 인터페이스를 구현 |
| **조율만 수행** | 비즈니스 로직은 Domain, 변환은 Factory/Assembler |
| **트랜잭션 위임** | 직접 `@Transactional` 사용 금지, Manager/Facade에 위임 |
| **Lombok 금지** | 생성자 직접 작성 (Plain Java) |

---

## 2) 패키지 구조

```
application/{bc}/
├─ service/
│  └─ command/                        ← Command UseCase 구현
│     ├─ PlaceOrderService.java       ← 복잡한 Command (Facade 사용)
│     └─ UpdateOrderStatusService.java ← 단순 Command (Manager 직접)
│
├─ factory/command/
│  └─ OrderCommandFactory.java        ← Command → Domain, PersistBundle
│
├─ facade/command/
│  └─ OrderFacade.java                ← 복잡한 Command (Manager 2개+)
│
├─ manager/command/
│  └─ OrderTransactionManager.java    ← 단일 영속화 담당
│
├─ assembler/
│  └─ OrderAssembler.java             ← Domain → Response
│
└─ port/in/command/
   └─ PlaceOrderUseCase.java          ← Port-In 인터페이스
```

---

## 3) Command 흐름

### 복잡한 Command (Manager 2개 이상)

```
Controller
    ↓
UseCase (PlaceOrderService)
    ↓
CommandFactory.createBundle(Command) → PersistBundle
    ↓
Facade.persistXxx(Bundle)
    ├─ Manager1.persist(Order)
    ├─ Manager2.persist(History)
    └─ Manager3.persist(Outbox)
    ↓
EventRegistry.registerForPublish(Event)
    ↓
Assembler.toResponse(Order) → Response
```

### 단순 Command (Manager 1개)

```
Controller
    ↓
UseCase
    ↓
CommandFactory.create(Command) → Domain
    ↓
Manager.persist(Domain)
    ↓
Assembler.toResponse(Domain) → Response
```

---

## 4) 사용 기준

### Facade vs Manager 직접 호출

| 조건 | 사용 |
|------|------|
| **Manager 2개 이상 조합** | Facade 사용 |
| **Manager 1개** | Manager 직접 호출 |

### Factory 사용 기준

| 조건 | 사용 |
|------|------|
| **Command → Domain 변환 필요** | CommandFactory 사용 |
| **Command → PersistBundle 필요** | CommandFactory.createBundle() 사용 |
| **단순 상태 변경 (ID만 필요)** | Factory 불필요 |

---

## 5) 구현 예시

### 복잡한 Command Service

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

### 단순 Command Service

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

### void 반환 Command Service

```java
package com.ryuqq.application.order.service.command;

import com.ryuqq.application.order.dto.command.CancelOrderCommand;
import com.ryuqq.application.order.manager.command.OrderTransactionManager;
import com.ryuqq.application.port.in.command.CancelOrderUseCase;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.vo.OrderId;
import org.springframework.stereotype.Service;

/**
 * 주문 취소 UseCase 구현체
 * - void 반환: Response 불필요
 */
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderTransactionManager orderManager;

    public CancelOrderService(OrderTransactionManager orderManager) {
        this.orderManager = orderManager;
    }

    @Override
    public void execute(CancelOrderCommand command) {
        // 1. 조회 (Manager)
        Order order = orderManager.getById(new OrderId(command.orderId()));

        // 2. 도메인 로직 실행 (Domain)
        order.cancel(command.reason());

        // 3. 영속화 (Manager)
        orderManager.persist(order);
    }
}
```

---

## 6) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `@Service` 어노테이션 | UseCase 구현체 표시 | 빌드 실패 |
| Port-In 구현 | UseCase 인터페이스 implements | 빌드 실패 |
| `service/command/` 패키지 | 올바른 위치 | 빌드 실패 |
| `@Transactional` 금지 | Manager/Facade 책임 | 빌드 실패 |
| Port 직접 호출 금지 | Manager/Facade 통해 접근 | 빌드 실패 |
| 객체 직접 생성 금지 | Factory 책임 | 빌드 실패 |
| 비즈니스 로직 금지 | Domain 책임 | 코드 리뷰 |
| Lombok 금지 | Plain Java | 빌드 실패 |

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Service 어노테이션
@Service
public class PlaceOrderService implements PlaceOrderUseCase { ... }

// ✅ Good: Port-In 인터페이스 구현
public class PlaceOrderService implements PlaceOrderUseCase { ... }

// ✅ Good: Factory, Facade/Manager, Assembler 조합
OrderPersistBundle bundle = commandFactory.createBundle(command);
Order saved = orderFacade.persistOrderBundle(bundle);
return assembler.toResponse(saved);

// ✅ Good: 단순한 경우 Manager 직접 호출
Order order = commandFactory.create(command);
Order saved = orderManager.persist(order);
return assembler.toResponse(saved);

// ✅ Good: 명시적 생성자 (Lombok 금지)
public PlaceOrderService(
    OrderCommandFactory commandFactory,
    OrderFacade orderFacade,
    OrderAssembler assembler
) {
    this.commandFactory = commandFactory;
    this.orderFacade = orderFacade;
    this.assembler = assembler;
}
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

// ❌ Bad: Lombok 사용
@Service
@RequiredArgsConstructor  // ❌ Lombok 금지
public class PlaceOrderService { ... }
```

---

## 8) 체크리스트

- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/command/` 패키지 위치
- [ ] CommandFactory 사용 (Command → Domain/Bundle)
- [ ] 복잡: Facade 사용 / 단순: Manager 직접
- [ ] Assembler 사용 (Domain → Response)
- [ ] `@Transactional` 금지
- [ ] Port 직접 호출 금지
- [ ] 객체 직접 생성 금지
- [ ] Lombok 금지 (명시적 생성자)

---

## 9) 관련 문서

- **[Service Guide](../service-guide.md)** - 전체 Service 가이드
- **[CommandService ArchUnit](./command-service-archunit.md)** - 아키텍처 규칙
- **[CommandService Test Guide](./command-service-test-guide.md)** - 테스트 가이드
- **[CommandFactory Guide](../../factory/command/command-factory-guide.md)** - Factory 구현
- **[TransactionManager Guide](../../manager/command/transaction-manager-guide.md)** - Manager 구현
- **[Facade Guide](../../facade/command/facade-guide.md)** - Facade 구현
- **[Assembler Guide](../../assembler/assembler-guide.md)** - Assembler 구현

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
