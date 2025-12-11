# Assembler — **Domain → Response 변환 전용**

> Assembler는 **Domain 객체를 Response DTO로 변환**하는 단일 책임 컴포넌트입니다.
>
> **업계 표준**: [Sairyss/domain-driven-hexagon](https://github.com/Sairyss/domain-driven-hexagon), Martin Fowler PoEAA 패턴 준수

---

## 1) 핵심 원칙 (Zero-Tolerance)

### Assembler의 단일 책임

| 역할 | 담당 컴포넌트 | Assembler 사용 |
|-----|-------------|---------------|
| Request DTO → Command | Controller (Adapter) | ❌ |
| **Command → Domain** | **CommandFactory** | ❌ |
| **Bundle 생성** | **CommandFactory** | ❌ |
| **Domain → Response DTO** | **Assembler** | ✅ |

### 왜 Domain → Response만 담당하는가?

**Martin Fowler, PoEAA**:
> "Usually an **assembler** is used on the server side to **transfer data between the DTO and any domain objects**."

**Implementing DDD (Vaughn Vernon)**:
> "The Application Service will use Repositories to read the necessary Aggregate instances and then **delegate to a DTO Assembler to map the attributes of the DTO**."

**핵심 이유**:
1. **역할 분리**: Command → Domain은 CommandFactory 책임
2. **순수성 유지**: Assembler는 Port 의존 없이 순수 변환만
3. **단방향 변환**: 인바운드(Factory) / 아웃바운드(Assembler) 분리

---

## 2) 핵심 역할

* **Domain → Response 변환**: Domain 객체를 Response DTO로 변환
* **List 변환**: `List<Domain>` → `List<Response>`
* **Nested Response 조립**: 복잡한 Response 구조 조립
* **단순 변환만**: 비즈니스 로직 포함 금지, 필드 매핑만
* **DB 조회 금지**: Port/Repository 의존 절대 금지
* **Bean 등록**: `@Component`로 등록

### 금지사항

| 금지 항목 | 이유 | 담당 컴포넌트 |
|----------|------|-------------|
| **Command → Domain 변환** | 인바운드 변환 | CommandFactory |
| **Bundle 생성** | 여러 객체 묶음 | CommandFactory |
| **Port/Repository 의존** | 순수성 깨짐 | UseCase |
| **비즈니스 로직** | 단일 책임 위반 | Domain / Policy |
| **@Transactional** | UseCase 책임 | UseCase |
| **PageResponse 조립** | 페이징 메타데이터 | UseCase |

---

## 3) 패키지 구조

```
application/{bc}/assembler/
└── {Bc}Assembler.java
```

---

## 4) 기본 구조

```java
package com.ryuqq.application.{bc}.assembler;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * {Bc} Assembler - Domain → Response 변환 전용
 *
 * <p>Assembler는 Domain 객체를 Response DTO로 변환하는 단일 책임을 가집니다.</p>
 * <p>Command → Domain 변환은 CommandFactory에서 처리합니다.</p>
 *
 * @see <a href="https://github.com/Sairyss/domain-driven-hexagon">domain-driven-hexagon</a>
 */
@Component
public class {Bc}Assembler {

    /**
     * Domain → Response 변환
     */
    public {Bc}Response toResponse({Bc} domain) {
        return new {Bc}Response(
            domain.id().value(),           // Record 스타일 getter
            domain.status().name(),
            domain.createdAt()
        );
    }

    /**
     * Domain → Detail Response 변환
     */
    public {Bc}DetailResponse toDetailResponse({Bc} domain) {
        return new {Bc}DetailResponse(
            domain.id().value(),
            domain.status().name(),
            // 중첩 객체 변환
            domain.createdAt()
        );
    }

    /**
     * List 변환
     */
    public List<{Bc}Response> toResponseList(List<{Bc}> domains) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        return domains.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

---

## 5) Domain → Response 변환

### 기본 패턴

```java
/**
 * Domain → Response
 */
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.id().value(),              // ✅ Record 스타일 getter
        order.customerId().value(),
        order.totalAmount().value(),
        order.status().name(),
        order.createdAt()
    );
}
```

### Nested Response 변환

```java
public OrderDetailResponse toDetailResponse(Order order) {
    return new OrderDetailResponse(
        order.id().value(),
        toCustomerInfo(order),           // 중첩 객체 변환
        toLineItems(order.lineItems()),
        order.totalAmount().value(),
        order.status().name(),
        order.createdAt()
    );
}

/**
 * 중첩 객체 변환 (private helper)
 */
private OrderDetailResponse.CustomerInfo toCustomerInfo(Order order) {
    return new OrderDetailResponse.CustomerInfo(
        order.customerId().value(),
        order.customerName()             // Domain이 제공하는 값
    );
}

private List<OrderDetailResponse.LineItem> toLineItems(List<OrderLineItem> items) {
    return items.stream()
        .map(item -> new OrderDetailResponse.LineItem(
            item.id().value(),
            item.productName(),
            item.quantity().value(),
            item.unitPrice().value()
        ))
        .toList();
}
```

### 여러 Domain → Response 조립

```java
/**
 * 여러 Domain 객체를 조합하여 Response 생성
 *
 * <p>주의: 조회는 UseCase에서 수행하고, Assembler는 조립만 담당</p>
 */
public OrderSummaryResponse toSummaryResponse(Order order, Member member) {
    return new OrderSummaryResponse(
        order.id().value(),
        order.totalAmount().value(),
        member.name(),                   // UseCase에서 조회한 Member
        order.status().name()
    );
}
```

### List 변환

```java
/**
 * List<Domain> → List<Response>
 */
public List<OrderResponse> toResponseList(List<Order> orders) {
    if (orders == null || orders.isEmpty()) {
        return List.of();
    }
    return orders.stream()
        .map(this::toResponse)
        .toList();
}
```

---

## 6) Do / Don't

### ❌ Bad Examples

```java
// ❌ Bad: Command → Domain 변환 (CommandFactory가 담당!)
public Order toDomain(PlaceOrderCommand command) {
    return Order.forNew(
        OrderId.generate(),
        Money.of(command.amount())
    );
}

// ❌ Bad: Port 의존 (순수성 깨짐!)
@Component
public class OrderAssembler {
    private final MemberQueryPort memberQueryPort;  // ❌ 금지!

    public OrderResponse toResponse(Order order) {
        Member member = memberQueryPort.findById(order.memberId());  // ❌
        return new OrderResponse(...);
    }
}

// ❌ Bad: 비즈니스 로직 포함
public OrderResponse toResponse(Order order) {
    BigDecimal tax = order.totalAmount().value()
        .multiply(BigDecimal.valueOf(0.1));  // ❌ 계산 로직!
    return new OrderResponse(
        order.id().value(),
        order.totalAmount().value().add(tax)  // ❌
    );
}

// ❌ Bad: Getter 체이닝 (Law of Demeter 위반)
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.id().value(),       // ✅ OK
        order.customer().address().city()  // ❌ 체이닝!
    );
}

// ❌ Bad: PageResponse 조립 (UseCase가 담당!)
public PageResponse<OrderResponse> toPageResponse(List<Order> orders, long total) {
    return new PageResponse<>(toResponseList(orders), total);  // ❌
}
```

### ✅ Good Examples

```java
// ✅ Good: Domain → Response 변환만
@Component
public class OrderAssembler {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.id().value(),
            order.customerId().value(),
            order.totalAmount().value(),
            order.status().name(),
            order.createdAt()
        );
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        return orders.stream()
            .map(this::toResponse)
            .toList();
    }
}

// ✅ Good: 여러 Domain 조립 (UseCase에서 조회 후 전달)
public OrderSummaryResponse toSummaryResponse(Order order, Member member) {
    return new OrderSummaryResponse(
        order.id().value(),
        order.totalAmount().value(),
        member.name(),
        order.status().name()
    );
}

// ✅ Good: Domain이 제공하는 계산 값 사용
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.id().value(),
        order.totalAmountWithTax().value()  // Domain이 계산 제공
    );
}
```

---

## 7) 실전 예시

### OrderAssembler (업계 표준 패턴)

```java
package com.ryuqq.application.order.assembler;

import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.aggregate.order.OrderLineItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Order Assembler - Domain → Response 변환 전용
 *
 * <p>Assembler는 Domain 객체를 Response DTO로 변환하는 단일 책임을 가집니다.</p>
 *
 * <p><strong>참고</strong>: Command → Domain 변환은
 * {@code OrderCommandFactory}에서 처리합니다.</p>
 *
 * @see <a href="https://github.com/Sairyss/domain-driven-hexagon">domain-driven-hexagon</a>
 */
@Component
public class OrderAssembler {

    /**
     * Domain → Response 변환
     */
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.id().value(),
            order.customerId().value(),
            order.totalAmount().value(),
            order.status().name(),
            order.createdAt()
        );
    }

    /**
     * Domain → Detail Response 변환
     */
    public OrderDetailResponse toDetailResponse(Order order) {
        return new OrderDetailResponse(
            order.id().value(),
            order.customerId().value(),
            toLineItems(order.lineItems()),
            order.totalAmount().value(),
            order.status().name(),
            order.createdAt()
        );
    }

    /**
     * LineItem 목록 변환 (private helper)
     */
    private List<OrderDetailResponse.LineItem> toLineItems(List<OrderLineItem> items) {
        return items.stream()
            .map(item -> new OrderDetailResponse.LineItem(
                item.id().value(),
                item.productId().value(),
                item.productName(),
                item.quantity().value(),
                item.unitPrice().value()
            ))
            .toList();
    }

    /**
     * List 변환
     */
    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        return orders.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

### UseCase에서 Assembler 사용

```java
@Service
public class GetOrderService implements GetOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderAssembler assembler;

    public GetOrderService(OrderQueryPort orderQueryPort, OrderAssembler assembler) {
        this.orderQueryPort = orderQueryPort;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse execute(Long orderId) {
        // 1. 조회
        Order order = orderQueryPort.findById(OrderId.of(orderId))
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Domain → Response 변환 (Assembler 위임)
        return assembler.toDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> search(OrderSearchQuery query) {
        // 1. 조회
        Page<Order> orderPage = orderQueryPort.search(query);

        // 2. List 변환 (Assembler 위임)
        List<OrderResponse> responses = assembler.toResponseList(orderPage.getContent());

        // 3. PageResponse 조립 (UseCase 책임)
        return PageResponse.of(responses, orderPage.getTotalElements());
    }
}
```

---

## 8) Command → Domain 변환은 어디서?

### CommandFactory 패턴 (권장)

```java
/**
 * Order CommandFactory - Command → Domain 변환 담당
 *
 * <p>순수 변환만 수행, Port 의존 없음</p>
 *
 * @see CommandFactory Guide
 */
@Component
public class OrderCommandFactory {

    /**
     * Command → Domain 변환
     */
    public Order create(PlaceOrderCommand command) {
        List<OrderItem> items = command.items().stream()
            .map(this::createOrderItem)
            .toList();

        return Order.forNew(
            new CustomerId(command.customerId()),
            items
        );
    }

    private OrderItem createOrderItem(OrderItemCommand itemCommand) {
        return OrderItem.forNew(
            new ProductId(itemCommand.productId()),
            new Quantity(itemCommand.quantity()),
            new Money(itemCommand.unitPrice())
        );
    }

    /**
     * Bundle 생성
     */
    public OrderPersistBundle createBundle(Order order, String eventType) {
        OutboxEvent outboxEvent = OutboxEvent.forNew(
            "Order", null, eventType, order.toEventPayload()
        );
        return new OrderPersistBundle(order, outboxEvent);
    }
}
```

### Factory vs Assembler 요약

| 구분 | CommandFactory | Assembler |
|------|---------------|-----------|
| **방향** | Command → Domain (인바운드) | Domain → Response (아웃바운드) |
| **메서드** | `create*()` | `toResponse()` |
| **역할** | 객체 생성, Bundle 생성 | 응답 변환 |

---

## 9) 업계 레퍼런스

### Sairyss/domain-driven-hexagon (⭐ 12k+)

```typescript
// user.mapper.ts - Mapper 3종 메서드
@Injectable()
export class UserMapper implements Mapper<UserEntity, UserModel, UserResponseDto> {

  toPersistence(entity: UserEntity): UserModel { ... }  // Domain → DB
  toDomain(record: UserModel): UserEntity { ... }       // DB → Domain
  toResponse(entity: UserEntity): UserResponseDto { ... } // Domain → Response

  // ❌ Command → Domain 메서드 없음!
}
```

### Martin Fowler - PoEAA

> "A DTO Assembler has the single responsibility of mapping (as in Mapper)
> the attributes from the Aggregate(s) to the DTO."

---

## 10) 체크리스트

Assembler 작성 시:
- [ ] `@Component` 어노테이션 적용
- [ ] 패키지: `application.{bc}.assembler`
- [ ] **Domain → Response 변환 메서드만** (toDomain 금지!)
- [ ] List 변환 메서드 (`toResponseList`)
- [ ] 비즈니스 로직 포함하지 않음
- [ ] Port/Repository 의존하지 않음
- [ ] Getter 체이닝 사용하지 않음 (Law of Demeter)
- [ ] PageResponse 조립하지 않음 (UseCase 책임)
- [ ] Static 메서드 사용하지 않음
- [ ] Lombok 사용하지 않음
- [ ] @Transactional 사용하지 않음

---

## 11) 관련 문서

- **[Application Layer Guide](../application-guide.md)** - 전체 흐름 및 컴포넌트 구조
- **[CommandFactory Guide](../factory/command/command-factory-guide.md)** - Command → Domain, Bundle 생성
- **[Assembler Test Guide](./assembler-test-guide.md)** - Assembler 테스트 가이드
- **[Assembler ArchUnit](./assembler-archunit.md)** - ArchUnit 자동 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 3.2.0 (CommandFactory 분리, Domain → Response 전용)
