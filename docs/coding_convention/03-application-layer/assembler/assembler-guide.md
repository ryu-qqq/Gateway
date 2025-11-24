# Assembler — **DTO ↔ Domain 변환 전용**

> Assembler는 **DTO와 Domain 간 변환만** 담당하는 단순 변환기입니다.
>
> 비즈니스 로직 없이 **필드 매핑만** 수행합니다.

---

## 1) 핵심 역할

* **Command → Domain**: CUD 요청 DTO를 Domain 객체로 변환
* **Query → Criteria**: 조회 조건 DTO를 Domain 검색 조건으로 변환
* **Domain → Response**: Domain 객체를 Response DTO로 변환
* **단순 변환만**: 비즈니스 로직 포함 금지, 필드 매핑만
* **Bean 등록**: `@Component`로 등록

---

## 2) 패키지 구조

```
application/{bc}/assembler/
└── {Bc}Assembler.java
```

---

## 3) 기본 구조

```java
package com.ryuqq.application.{bc}.assembler;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class {Bc}Assembler {

    /**
     * Command → Domain
     */
    public {Bc} toDomain({Action}{Bc}Command command) {
        // 필드 매핑만
    }

    /**
     * Query → Criteria (Domain 검색 조건)
     */
    public {Bc}Criteria toCriteria(Search{Bc}Query query) {
        // 필드 매핑만
    }

    /**
     * Domain → Response
     */
    public {Bc}Response toResponse({Bc} domain) {
        // 필드 매핑만
    }

    /**
     * List 변환
     */
    public List<{Bc}Response> toResponseList(List<{Bc}> domains) {
        return domains.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

---

## 4) Command → Domain 변환

### 기본 패턴
```java
/**
 * Command → Domain
 */
public Order toDomain(PlaceOrderCommand command) {
    return Order.forNew(
        OrderId.forNew(),
        CustomerId.of(command.customerId()),
        Money.of(command.amount()),
        Address.of(command.deliveryAddress())
    );
}
```

### Nested Record 변환
```java
public Order toDomain(PlaceOrderCommand command) {
    List<OrderItem> items = command.items().stream()
        .map(item -> OrderItem.of(
            ProductId.of(item.productId()),
            Quantity.of(item.quantity()),
            Money.of(item.unitPrice())
        ))
        .toList();

    return Order.forNew(
        OrderId.forNew(),
        CustomerId.of(command.customerId()),
        items
    );
}
```

### Do / Don't

```java
// ✅ Good: 필드 매핑만
public Order toDomain(PlaceOrderCommand command) {
    return Order.forNew(
        OrderId.forNew(),
        Money.of(command.amount())
    );
    // ✅ OrderStatus는 forNew() 내부에서 설정
}

// ❌ Bad: 상태 설정 금지
public Order toDomain(PlaceOrderCommand command) {
    return Order.forNew(
        OrderId.forNew(),
        Money.of(command.amount()),
        OrderStatus.PLACED  // ← Domain 생성자가 처리!
    );
}

// ❌ Bad: 비즈니스 메서드 호출 금지
public Order toDomain(PlaceOrderCommand command) {
    Order order = Order.forNew(...);
    order.validate();  // ← UseCase에서!
    order.place();     // ← UseCase에서!
    return order;
}
```

---

## 5) Query → Criteria 변환

### 기본 패턴
```java
/**
 * Query → Criteria (Domain 검색 조건)
 */
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    return OrderSearchCriteria.of(
        query.customerId(),
        query.status(),
        query.startDate(),
        query.endDate(),
        query.sortBy(),
        query.sortDirection()
    );
}
```

### 정렬 조건 변환
```java
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    return OrderSearchCriteria.builder()
        .customerId(query.customerId())
        .status(query.status())
        .startDate(query.startDate())
        .endDate(query.endDate())
        .sortBy(query.sortBy())           // 정렬 필드
        .sortDirection(query.sortDirection()) // 정렬 방향
        .build();
}
```

### 페이징 조건 변환
```java
// Offset 페이징
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    return OrderSearchCriteria.of(
        query.customerId(),
        query.status(),
        query.page(),    // Offset 페이징
        query.size()
    );
}

// No-Offset (커서) 페이징
public OrderSearchCriteria toCriteria(SearchOrdersCursorQuery query) {
    return OrderSearchCriteria.of(
        query.lastOrderId(),  // 커서
        query.status(),
        query.size()
    );
}
```

### Do / Don't

```java
// ✅ Good: 필드 매핑만
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    return OrderSearchCriteria.of(
        query.customerId(),
        query.status()
    );
}

// ❌ Bad: 기본값 설정 금지
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    return OrderSearchCriteria.of(
        query.customerId(),
        query.status() != null ? query.status() : "PLACED"  // ← REST API에서!
    );
}

// ❌ Bad: 비즈니스 로직 금지
public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
    if (query.startDate().isAfter(query.endDate())) {  // ← UseCase에서!
        throw new IllegalArgumentException();
    }
    return OrderSearchCriteria.of(...);
}
```

---

## 6) Domain → Response 변환

### 기본 패턴
```java
/**
 * Domain → Response
 */
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getIdValue(),      // ✅ Law of Demeter 준수
        order.getAmountValue(),
        order.getStatusName(),
        order.getCreatedAt()
    );
}
```

### Nested Response 변환
```java
public OrderDetailResponse toResponse(Order order) {
    return new OrderDetailResponse(
        order.getIdValue(),
        toCustomerInfo(order.getCustomer()),
        toLineItems(order.getLineItems()),
        order.getTotalAmountValue(),
        order.getStatusName(),
        order.getOrderedAt()
    );
}

private OrderDetailResponse.CustomerInfo toCustomerInfo(Customer customer) {
    return new OrderDetailResponse.CustomerInfo(
        customer.getIdValue(),
        customer.getName(),
        customer.getEmail()
    );
}

private List<OrderDetailResponse.LineItem> toLineItems(List<OrderItem> items) {
    return items.stream()
        .map(item -> new OrderDetailResponse.LineItem(
            item.getIdValue(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitPriceValue()
        ))
        .toList();
}
```

### List 변환
```java
/**
 * List<Domain> → List<Response>
 */
public List<OrderResponse> toResponseList(List<Order> orders) {
    return orders.stream()
        .map(this::toResponse)
        .toList();
}
```

### Do / Don't

```java
// ✅ Good: Law of Demeter 준수
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getIdValue(),      // ✅ 체이닝 없음
        order.getAmountValue()
    );
}

// ❌ Bad: Getter 체이닝 금지
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getId().value(),     // ← Law of Demeter 위반!
        order.getAmount().value()
    );
}

// ❌ Bad: 계산 로직 금지
public OrderResponse toResponse(Order order) {
    BigDecimal tax = order.getAmountValue().multiply(0.1);  // ← Domain에서!
    return new OrderResponse(
        order.getIdValue(),
        order.getAmountValue().add(tax)
    );
}

// ❌ Bad: 편의 메서드 호출 금지
public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.getIdValue(),
        order.isCompleted() ? "완료" : "진행중"  // ← UseCase에서 처리!
    );
}
```

---

## 7) 실전 예시

### OrderAssembler
```java
package com.ryuqq.application.order.assembler;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OrderAssembler {

    /**
     * Command → Domain
     */
    public Order toDomain(PlaceOrderCommand command) {
        List<OrderItem> items = command.items().stream()
            .map(item -> OrderItem.of(
                ProductId.of(item.productId()),
                Quantity.of(item.quantity()),
                Money.of(item.unitPrice())
            ))
            .toList();

        return Order.forNew(
            OrderId.forNew(),
            CustomerId.of(command.customerId()),
            items,
            Address.of(command.deliveryAddress())
        );
    }

    /**
     * Query → Criteria
     */
    public OrderSearchCriteria toCriteria(SearchOrdersQuery query) {
        return OrderSearchCriteria.of(
            query.customerId(),
            query.status(),
            query.startDate(),
            query.endDate(),
            query.sortBy(),
            query.sortDirection(),
            query.page(),
            query.size()
        );
    }

    /**
     * Domain → Response
     */
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getIdValue(),
            order.getCustomerIdValue(),
            order.getTotalAmountValue(),
            order.getStatusName(),
            order.getOrderedAt()
        );
    }

    /**
     * Domain → DetailResponse
     */
    public OrderDetailResponse toDetailResponse(Order order) {
        return new OrderDetailResponse(
            order.getIdValue(),
            toCustomerInfo(order.getCustomer()),
            toLineItems(order.getLineItems()),
            order.getTotalAmountValue(),
            order.getStatusName(),
            order.getOrderedAt()
        );
    }

    private OrderDetailResponse.CustomerInfo toCustomerInfo(Customer customer) {
        return new OrderDetailResponse.CustomerInfo(
            customer.getIdValue(),
            customer.getName(),
            customer.getEmail()
        );
    }

    private List<OrderDetailResponse.LineItem> toLineItems(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderDetailResponse.LineItem(
                item.getIdValue(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPriceValue()
            ))
            .toList();
    }

    /**
     * List 변환
     */
    public List<OrderResponse> toResponseList(List<Order> orders) {
        return orders.stream()
            .map(this::toResponse)
            .toList();
    }
}
```

---

## 8) 금지사항

* **비즈니스 로직**: 검증, 계산, 상태 설정 금지
* **Getter 체이닝**: `order.getId().value()` 금지
* **Static 메서드**: 반드시 `@Component` Bean 등록
* **Port 의존성**: Repository 등 주입 금지
* **기본값 설정**: REST API Layer 책임
* **PageResponse 조립**: UseCase 책임 (Assembler는 List 변환만)

---

## 9) 체크리스트

- [ ] `@Component` 어노테이션 적용
- [ ] 패키지: `application.{bc}.assembler`
- [ ] Command → Domain 변환 메서드
- [ ] Query → Criteria 변환 메서드
- [ ] Domain → Response 변환 메서드
- [ ] 비즈니스 로직 포함하지 않음
- [ ] Getter 체이닝 사용하지 않음
- [ ] Port 의존성 주입하지 않음
- [ ] Static 메서드 사용하지 않음
- [ ] Lombok 사용하지 않음

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 2.0.0 (Command/Query/Response 명확화)
