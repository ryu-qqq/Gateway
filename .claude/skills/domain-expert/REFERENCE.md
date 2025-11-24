# Domain Layer 전문가 - API 참조

Domain Layer Zero-Tolerance 규칙 전체, 코드 템플릿, Anti-patterns를 제공합니다.

## Zero-Tolerance 규칙 전체

### ✅ Mandatory (7개)

#### 1. Aggregate Root 필수
- 모든 Entity 접근은 Aggregate Root를 통해서만
- Root 외부에서 Entity 직접 접근 금지

```java
// ✅ CORRECT
Order order = Order.create(...);
order.addOrderLine(productId, quantity);

// ❌ WRONG
OrderLine line = new OrderLine(...);
order.getOrderLines().add(line);  // 직접 접근 금지
```

#### 2. Factory Method 필수
- 생성자 대신 Factory Method 사용
- `new Order()` 금지

```java
// ✅ CORRECT
public static Order create(Long customerId, Long productId, Integer quantity) {
    return new Order(
        OrderId.generate(),
        customerId,
        OrderStatus.PENDING,
        calculateAmount(productId, quantity),
        LocalDate.now()
    );
}

// ❌ WRONG
Order order = new Order(customerId, productId, quantity);
```

#### 3. Value Object Immutable
- Value Object는 생성 후 변경 불가
- Setter 금지, 새 인스턴스 생성

```java
// ✅ CORRECT
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) {
        return new Money(amount.add(other.amount), currency);
    }
}

// ❌ WRONG
public class Money {
    private BigDecimal amount;
    public void setAmount(BigDecimal amount) { ... }  // Setter 금지
}
```

#### 4. Business Method 필수
- 상태 변경은 비즈니스 메서드로만
- Public Setter 금지

```java
// ✅ CORRECT
public void place() {
    if (this.status != OrderStatus.PENDING) {
        throw new IllegalStateException("Only PENDING orders can be placed");
    }
    this.status = OrderStatus.PLACED;
    this.domainEvents.add(new OrderPlacedEvent(this.orderId));
}

// ❌ WRONG
public void setStatus(OrderStatus status) {
    this.status = status;  // Public Setter 금지
}
```

#### 5. Encapsulation 엄격 (Law of Demeter)
- Getter 체이닝 금지
- Tell, Don't Ask 패턴 적용

```java
// ✅ CORRECT
public String getCustomerZipCode() {
    return this.customer.getZipCode();
}

// ❌ WRONG
String zip = order.getCustomer().getAddress().getZipCode();
```

#### 6. No Lombok
- Lombok 어노테이션 금지
- Pure Java getter/setter 사용

```java
// ✅ CORRECT
public OrderId getOrderId() {
    return orderId;
}

// ❌ WRONG
@Getter
@Setter
@Data
@Builder
```

#### 7. Domain Event 발행
- 중요한 상태 변경 시 Domain Event 발행

```java
// ✅ CORRECT
public void place() {
    this.status = OrderStatus.PLACED;
    this.domainEvents.add(new OrderPlacedEvent(this.orderId));
}
```

### ❌ Prohibited (7개)

#### 1. No Getter 체이닝
- `order.getCustomer().getAddress()` 금지
- Tell, Don't Ask 적용

```java
// ❌ WRONG
String zip = order.getCustomer().getAddress().getZipCode();
String city = order.getCustomer().getAddress().getCity();

// ✅ CORRECT
String zip = order.getCustomerZipCode();
String city = order.getCustomerCity();
```

#### 2. No Public Setter
- 상태 변경은 Business Method로만
- Public Setter 금지

```java
// ❌ WRONG
public void setStatus(OrderStatus status) { ... }
order.setStatus(OrderStatus.PLACED);

// ✅ CORRECT
public void place() { ... }
order.place();
```

#### 3. No JPA 어노테이션
- Domain은 순수 Java
- JPA 의존성 금지

```java
// ❌ WRONG
@Entity
@Table(name = "orders")
public class Order { ... }

// ✅ CORRECT (No JPA annotations in Domain)
public class Order {
    private final OrderId orderId;
    // ...
}
```

#### 4. No Lombok
- `@Data`, `@Builder`, `@Getter`, `@Setter` 금지

```java
// ❌ WRONG
@Data
@Builder
public class Order { ... }

// ✅ CORRECT
public class Order {
    public OrderId getOrderId() {
        return orderId;
    }
}
```

#### 5. No Service 로직
- UseCase 로직이 Domain에 침범 금지
- Domain은 비즈니스 규칙만

```java
// ❌ WRONG (Domain에 UseCase 로직)
public void placeOrder() {
    // Port 호출 (UseCase 영역)
    Order savedOrder = saveOrderPort.save(this);
}

// ✅ CORRECT (Domain은 비즈니스 규칙만)
public void place() {
    if (this.status != OrderStatus.PENDING) {
        throw new IllegalStateException("Only PENDING orders can be placed");
    }
    this.status = OrderStatus.PLACED;
}
```

#### 6. No DB 의존성
- Domain은 Infrastructure 독립
- Repository, JPA 의존 금지

```java
// ❌ WRONG
public class Order {
    @Autowired
    private OrderRepository repository;  // DB 의존성 금지
}

// ✅ CORRECT (Domain은 순수 Java)
public class Order {
    private final OrderId orderId;
    // No Infrastructure dependencies
}
```

#### 7. No Primitive Obsession
- 기본 타입 대신 Value Object 사용

```java
// ❌ WRONG
public class Order {
    private String orderId;  // String 대신 OrderId
    private Long amount;     // Long 대신 Money
}

// ✅ CORRECT
public class Order {
    private final OrderId orderId;
    private final Money totalAmount;
}
```

## 코드 템플릿

### Aggregate Root 템플릿

```java
package com.company.template.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order Aggregate Root.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class Order {

    private final OrderId orderId;
    private final Long customerId;
    private OrderStatus status;
    private final Money totalAmount;
    private final LocalDate orderDate;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Order 생성자 (Private).
     */
    private Order(
        OrderId orderId,
        Long customerId,
        OrderStatus status,
        Money totalAmount,
        LocalDate orderDate
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
    }

    /**
     * Order 생성 (Factory Method).
     *
     * @param customerId  고객 ID
     * @param productId   상품 ID
     * @param quantity    수량
     * @return 생성된 Order
     */
    public static Order create(Long customerId, Long productId, Integer quantity) {
        return new Order(
            OrderId.generate(),
            customerId,
            OrderStatus.PENDING,
            Money.of(calculateAmount(productId, quantity)),
            LocalDate.now()
        );
    }

    /**
     * 주문 생성 (Business Method).
     */
    public void place() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be placed");
        }
        this.status = OrderStatus.PLACED;
        this.domainEvents.add(new OrderPlacedEvent(this.orderId));
    }

    /**
     * 주문 취소 (Business Method).
     */
    public void cancel() {
        if (this.status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only PLACED orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.domainEvents.add(new OrderCancelledEvent(this.orderId));
    }

    // Getters (No Setters)
    public String getOrderIdValue() {
        return orderId.getValue();
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    private static Long calculateAmount(Long productId, Integer quantity) {
        return productId * quantity;
    }
}
```

### Value Object 템플릿 (Record)

```java
package com.company.template.domain;

import java.util.UUID;

/**
 * OrderId Value Object.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public record OrderId(String value) {

    /**
     * OrderId 생성자.
     */
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId cannot be null or blank");
        }
    }

    /**
     * OrderId 생성 (Factory Method).
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    /**
     * String으로부터 생성.
     */
    public static OrderId from(String value) {
        return new OrderId(value);
    }

    /**
     * String 값 반환.
     */
    public String getValue() {
        return value;
    }
}
```

### Enum 템플릿

```java
package com.company.template.domain;

/**
 * Order Status Enum.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public enum OrderStatus {
    PENDING,
    PLACED,
    CONFIRMED,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    /**
     * PENDING 여부 확인.
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * PLACED 여부 확인.
     */
    public boolean isPlaced() {
        return this == PLACED;
    }

    /**
     * 취소 가능 여부 확인.
     */
    public boolean isCancellable() {
        return this == PLACED || this == CONFIRMED;
    }
}
```

## Anti-patterns (자주 하는 실수)

### 1. Getter 체이닝
```java
// ❌ WRONG
String zip = order.getCustomer().getAddress().getZipCode();

// ✅ CORRECT
String zip = order.getCustomerZipCode();
```

### 2. Public Setter 사용
```java
// ❌ WRONG
order.setStatus(OrderStatus.PLACED);

// ✅ CORRECT
order.place();
```

### 3. Lombok 사용
```java
// ❌ WRONG
@Data
@Builder
public class Order { ... }

// ✅ CORRECT
public class Order {
    public OrderId getOrderId() { return orderId; }
}
```

### 4. Primitive Obsession
```java
// ❌ WRONG
private String orderId;
private Long amount;

// ✅ CORRECT
private OrderId orderId;
private Money totalAmount;
```

### 5. JPA 어노테이션
```java
// ❌ WRONG
@Entity
public class Order { ... }

// ✅ CORRECT (Domain은 순수 Java)
public class Order { ... }
```

## Checklist

Domain Layer 코드 작성 시 다음을 확인하세요:

- [ ] Factory Method 사용 (`Order.create()`)
- [ ] Business Method로 상태 변경 (`order.place()`)
- [ ] Getter 체이닝 금지 (Tell, Don't Ask)
- [ ] Public Setter 금지
- [ ] Lombok 미사용
- [ ] JPA 어노테이션 미사용
- [ ] Value Object 불변성 보장
- [ ] Primitive Obsession 회피 (OrderId, Money)
- [ ] Domain Event 발행 (중요한 상태 변경 시)
- [ ] Infrastructure 의존성 없음

## 참조 문서

- [Domain 전체 가이드](../../../docs/coding_convention/02-domain-layer/domain-guide.md)
- [Aggregate 설계](../../../docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md)
- Law of Demeter 문서들: docs/coding_convention/02-domain-layer/law-of-demeter/
