# Query Bundle Guide — **조회 결과 묶음**

> QueryBundle은 **여러 조회 결과를 하나로 묶어** Assembler에게 전달합니다.
>
> **QueryFacade에서 생성**하고, **Assembler에서 Response로 변환**합니다.

---

## 1) 핵심 역할

* **조회 결과 묶음**: 여러 Domain 객체를 하나로 묶음
* **QueryFacade → Assembler 전달**: 중간 전달 객체
* **불변 객체**: record 사용 권장
* **타입 안전성**: 명시적 타입으로 조합 보장

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **record 사용** | 불변성 보장 |
| **`*QueryBundle` 접미사** | 네이밍 규칙 |
| **Domain 객체만 포함** | DTO/Response 포함 금지 |
| **비즈니스 로직 금지** | 순수 데이터 객체 |
| **Lombok 금지** | record 사용 |
| **bundle 패키지 위치** | `dto/bundle/` |

---

## 3) 패키지 구조

```
application/{bc}/dto/bundle/
├─ OrderPersistBundle.java       ← Command용 (영속화 객체)
├─ OrderDetailQueryBundle.java   ← Query용 (조회 결과) - 이 문서
└─ OrderHistoryQueryBundle.java  ← Query용 (조회 결과)
```

---

## 4) PersistBundle vs QueryBundle

| 구분 | PersistBundle | QueryBundle |
|------|---------------|-------------|
| **용도** | 영속화할 객체 묶음 | 조회된 객체 묶음 |
| **생성** | CommandFactory | QueryFacade |
| **소비** | Facade (영속화) | Assembler (Response 변환) |
| **특수 메서드** | `enrichWithId()` | 없음 (순수 데이터) |
| **접미사** | `*PersistBundle` | `*QueryBundle` |

---

## 5) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.dto.bundle;

import com.ryuqq.domain.customer.aggregate.Customer;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderItem;
import com.ryuqq.domain.shipping.aggregate.ShippingInfo;

import java.util.List;

/**
 * 주문 상세 조회 Bundle
 * - QueryFacade에서 생성
 * - Assembler에서 Response로 변환
 */
public record OrderDetailQueryBundle(
    Order order,
    List<OrderItem> items,
    Customer customer,
    ShippingInfo shipping
) {
    // 불변 객체 - 비즈니스 로직 없음
}
```

### 다양한 예시

```java
/**
 * 주문 이력 조회 Bundle
 */
public record OrderHistoryQueryBundle(
    Order order,
    List<OrderItem> items,
    List<StatusChange> statusHistory,
    List<PaymentRecord> paymentHistory
) {}

/**
 * 주문 + 결제 정보 Bundle
 */
public record OrderPaymentQueryBundle(
    Order order,
    PaymentInfo payment
) {}

/**
 * 대시보드 데이터 Bundle
 */
public record DashboardQueryBundle(
    OrderStatistics orderStats,
    ProductStatistics productStats,
    CustomerStatistics customerStats,
    List<RecentActivity> recentActivities
) {}

/**
 * 상품 상세 Bundle
 */
public record ProductDetailQueryBundle(
    Product product,
    List<ProductImage> images,
    List<ProductOption> options,
    Inventory inventory,
    List<Review> reviews
) {}
```

### Optional 필드 처리

```java
/**
 * 선택적 필드가 있는 Bundle
 * - nullable 필드는 명시적으로 표현
 */
public record OrderDetailQueryBundle(
    Order order,
    List<OrderItem> items,
    Customer customer,
    ShippingInfo shipping  // nullable (배송 정보 없을 수 있음)
) {
    /**
     * 배송 정보 존재 여부
     */
    public boolean hasShipping() {
        return shipping != null;
    }

    /**
     * 아이템 존재 여부
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }
}
```

---

## 6) Assembler에서 사용

```java
@Component
public class OrderAssembler {

    /**
     * QueryBundle → Response 변환
     */
    public OrderDetailResponse toDetailResponse(OrderDetailQueryBundle bundle) {
        return new OrderDetailResponse(
            bundle.order().id().value(),
            bundle.order().status().name(),
            bundle.order().totalAmount().value(),
            toItemResponses(bundle.items()),
            toCustomerResponse(bundle.customer()),
            bundle.hasShipping() ? toShippingResponse(bundle.shipping()) : null
        );
    }

    private List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
        return items.stream()
            .map(this::toItemResponse)
            .toList();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.productId().value(),
            item.quantity().value(),
            item.unitPrice().value()
        );
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
            customer.id().value(),
            customer.name().value(),
            customer.email().value()
        );
    }

    private ShippingResponse toShippingResponse(ShippingInfo shipping) {
        return new ShippingResponse(
            shipping.address().fullAddress(),
            shipping.status().name(),
            shipping.estimatedDelivery()
        );
    }
}
```

---

## 7) QueryFacade에서 생성

```java
@Component
public class OrderQueryFacade {

    @Transactional(readOnly = true)
    public OrderDetailQueryBundle fetchOrderDetail(OrderDetailCriteria criteria) {
        // 1. 각 ReadManager로 조회
        Order order = orderReadManager.findById(criteria.orderId());
        List<OrderItem> items = itemReadManager.findByOrderId(criteria.orderId());
        Customer customer = customerReadManager.findById(order.customerId());
        ShippingInfo shipping = criteria.includeShipping()
            ? shippingReadManager.findByOrderId(criteria.orderId())
            : null;

        // 2. QueryBundle로 묶어 반환
        return new OrderDetailQueryBundle(order, items, customer, shipping);
    }
}
```

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: record 사용
public record OrderDetailQueryBundle(
    Order order,
    List<OrderItem> items,
    Customer customer,
    ShippingInfo shipping
) {}

// ✅ Good: *QueryBundle 접미사
public record OrderDetailQueryBundle(...) {}
public record DashboardQueryBundle(...) {}

// ✅ Good: Domain 객체만 포함
public record OrderDetailQueryBundle(
    Order order,           // ✅ Domain
    List<OrderItem> items, // ✅ Domain
    Customer customer      // ✅ Domain
) {}

// ✅ Good: 편의 메서드 (조건 확인만)
public boolean hasShipping() {
    return shipping != null;
}
```

### ❌ Bad

```java
// ❌ Bad: class 사용 (record 사용해야 함)
public class OrderDetailQueryBundle {
    private Order order;
    private List<OrderItem> items;
    // ...
}

// ❌ Bad: *QueryBundle 접미사 누락
public record OrderDetail(...) {}  // ❌

// ❌ Bad: DTO/Response 포함
public record OrderDetailQueryBundle(
    Order order,
    OrderResponse response  // ❌ Response 포함 금지
) {}

// ❌ Bad: 비즈니스 로직 포함
public record OrderDetailQueryBundle(...) {
    public Money calculateTotal() {  // ❌ 비즈니스 로직
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// ❌ Bad: Lombok 사용
@Data  // ❌
public class OrderDetailQueryBundle { ... }

// ❌ Bad: 가변 컬렉션
public record OrderDetailQueryBundle(
    Order order,
    ArrayList<OrderItem> items  // ❌ 불변 List 사용
) {}
```

---

## 9) 체크리스트

- [ ] `record` 사용 (불변성)
- [ ] `*QueryBundle` 접미사
- [ ] Domain 객체만 포함
- [ ] DTO/Response 포함 금지
- [ ] 비즈니스 로직 없음
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.dto.bundle`
- [ ] 불변 컬렉션 사용 (`List`, `Set`)

---

## 10) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[QueryFacade Guide](../../facade/query/query-facade-guide.md)** - Bundle 생성
- **[Assembler Guide](../../assembler/assembler-guide.md)** - Bundle → Response
- **[PersistBundle Guide](./persist-bundle-guide.md)** - Command Bundle

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
