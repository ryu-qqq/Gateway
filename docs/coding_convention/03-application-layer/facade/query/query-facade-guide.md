# Query Facade Guide — **여러 ReadManager 조합**

> QueryFacade는 **여러 ReadManager**를 조합하여 복합 조회를 수행합니다.
>
> **UseCase에서 호출**하며, **QueryBundle**을 반환합니다.

---

## 1) 핵심 역할

* **여러 ReadManager 조합**: 2개 이상의 ReadManager를 조합 (필수 조건)
* **UseCase에서 호출**: Query Service에서 사용
* **QueryBundle 반환**: 조회 결과를 Bundle로 묶어 반환
* **순수 조율만**: 비즈니스 로직 없음, 객체 생성 없음

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **2개 이상 ReadManager 의존** | 단일 ReadManager는 UseCase에서 직접 호출 |
| **`@Component` 어노테이션** | `@Service` 아님 |
| **`@Transactional(readOnly=true)` 메서드 단위** | 읽기 전용 트랜잭션 |
| **`fetch*()` 메서드 네이밍** | persist/save 등 금지 |
| **QueryBundle 반환** | 여러 조회 결과를 Bundle로 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ facade/
│  ├─ command/
│  │  └─ {Bc}Facade.java             ← Command용 (persist)
│  └─ query/
│     └─ {Bc}QueryFacade.java        ← Query용 (fetch) - 이 문서
└─ manager/
   └─ query/
      ├─ OrderReadManager.java
      └─ CustomerReadManager.java
```

---

## 4) Facade vs QueryFacade

| 구분 | Facade (Command) | QueryFacade (Query) |
|------|------------------|---------------------|
| **역할** | 여러 Manager 조합 (영속화) | 여러 ReadManager 조합 (조회) |
| **의존성** | TransactionManager 2개+ | ReadManager 2개+ |
| **트랜잭션** | `@Transactional` | `@Transactional(readOnly=true)` |
| **메서드** | `persist*()` | `fetch*()` |
| **반환** | Domain (ID 할당됨) | QueryBundle |
| **위치** | `facade/command/` | `facade/query/` |

---

## 5) QueryFacade 사용 기준

### ✅ QueryFacade가 필요한 경우

1. **여러 ReadManager 조합**
   - Order + OrderItems + Customer 함께 조회
   - Product + Inventory + PriceHistory 함께 조회

2. **복합 조회 결과**
   - 여러 Domain 객체를 QueryBundle로 묶어 반환
   - Assembler가 Bundle을 Response로 변환

### ❌ QueryFacade가 불필요한 경우

1. **단일 ReadManager만 사용**
   - UseCase → ReadManager 직접 호출

2. **단순 목록 조회**
   - ReadManager로 충분

---

## 6) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.facade.query;

import com.ryuqq.application.order.dto.bundle.OrderDetailQueryBundle;
import com.ryuqq.application.order.manager.query.OrderReadManager;
import com.ryuqq.application.order.manager.query.OrderItemReadManager;
import com.ryuqq.application.customer.manager.query.CustomerReadManager;
import com.ryuqq.application.shipping.manager.query.ShippingReadManager;
import com.ryuqq.domain.customer.aggregate.Customer;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.aggregate.OrderItem;
import com.ryuqq.domain.order.criteria.OrderDetailCriteria;
import com.ryuqq.domain.shipping.aggregate.ShippingInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Order Query Facade
 * - 여러 ReadManager 조합
 * - QueryBundle 반환
 */
@Component
public class OrderQueryFacade {

    private final OrderReadManager orderReadManager;
    private final OrderItemReadManager itemReadManager;
    private final CustomerReadManager customerReadManager;
    private final ShippingReadManager shippingReadManager;

    public OrderQueryFacade(
        OrderReadManager orderReadManager,
        OrderItemReadManager itemReadManager,
        CustomerReadManager customerReadManager,
        ShippingReadManager shippingReadManager
    ) {
        this.orderReadManager = orderReadManager;
        this.itemReadManager = itemReadManager;
        this.customerReadManager = customerReadManager;
        this.shippingReadManager = shippingReadManager;
    }

    /**
     * 주문 상세 조회
     * - Order + Items + Customer + Shipping
     */
    @Transactional(readOnly = true)
    public OrderDetailQueryBundle fetchOrderDetail(OrderDetailCriteria criteria) {
        // 1. Order 조회
        Order order = orderReadManager.findById(criteria.orderId());

        // 2. OrderItems 조회 (조건에 따라)
        List<OrderItem> items = criteria.includeItems()
            ? itemReadManager.findByOrderId(criteria.orderId())
            : List.of();

        // 3. Customer 조회
        Customer customer = customerReadManager.findById(order.customerId());

        // 4. Shipping 조회 (조건에 따라)
        ShippingInfo shipping = criteria.includeShipping()
            ? shippingReadManager.findByOrderId(criteria.orderId())
            : null;

        // 5. QueryBundle로 묶어 반환
        return new OrderDetailQueryBundle(order, items, customer, shipping);
    }

    /**
     * 주문 + 결제 정보 조회
     */
    @Transactional(readOnly = true)
    public OrderPaymentQueryBundle fetchOrderWithPayment(OrderId orderId) {
        Order order = orderReadManager.findById(orderId);
        PaymentInfo payment = paymentReadManager.findByOrderId(orderId);

        return new OrderPaymentQueryBundle(order, payment);
    }
}
```

### 복잡한 조합 예시

```java
/**
 * 대시보드 데이터 조회
 * - 여러 BC의 ReadManager 조합
 */
@Transactional(readOnly = true)
public DashboardQueryBundle fetchDashboard(DashboardCriteria criteria) {
    // 1. 주문 통계
    OrderStatistics orderStats = orderReadManager.getStatistics(criteria.dateRange());

    // 2. 상품 통계
    ProductStatistics productStats = productReadManager.getStatistics(criteria.dateRange());

    // 3. 고객 통계
    CustomerStatistics customerStats = customerReadManager.getStatistics(criteria.dateRange());

    // 4. 최근 활동
    List<RecentActivity> activities = activityReadManager.findRecent(10);

    return new DashboardQueryBundle(
        orderStats,
        productStats,
        customerStats,
        activities
    );
}

/**
 * 주문 이력 상세 조회
 */
@Transactional(readOnly = true)
public OrderHistoryQueryBundle fetchOrderHistory(OrderId orderId) {
    Order order = orderReadManager.findById(orderId);
    List<OrderItem> items = itemReadManager.findByOrderId(orderId);
    List<StatusChange> statusHistory = historyReadManager.findByOrderId(orderId);
    List<PaymentRecord> paymentHistory = paymentReadManager.findHistoryByOrderId(orderId);

    return new OrderHistoryQueryBundle(
        order,
        items,
        statusHistory,
        paymentHistory
    );
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Component 어노테이션
@Component
public class OrderQueryFacade { ... }

// ✅ Good: 2개 이상 ReadManager 조합
@Component
public class OrderQueryFacade {
    private final OrderReadManager orderReadManager;
    private final CustomerReadManager customerReadManager;
    private final ShippingReadManager shippingReadManager;
}

// ✅ Good: @Transactional(readOnly = true) 메서드 단위
@Transactional(readOnly = true)
public OrderDetailQueryBundle fetchOrderDetail(OrderDetailCriteria criteria) { ... }

// ✅ Good: fetch* 메서드 네이밍
public OrderDetailQueryBundle fetchOrderDetail(...) { ... }
public DashboardQueryBundle fetchDashboard(...) { ... }

// ✅ Good: QueryBundle 반환
return new OrderDetailQueryBundle(order, items, customer, shipping);
```

### ❌ Bad

```java
// ❌ Bad: @Service 어노테이션
@Service
public class OrderQueryFacade { ... }

// ❌ Bad: @Transactional 클래스 레벨
@Component
@Transactional(readOnly = true)
public class OrderQueryFacade { ... }

// ❌ Bad: 단일 ReadManager만 의존
@Component
public class OrderQueryFacade {
    private final OrderReadManager orderReadManager;  // 1개만 - UseCase에서 직접!
}

// ❌ Bad: persist/save 메서드명
public Order persistOrder(Order order) { ... }  // ❌ Command Facade 아님

// ❌ Bad: Domain 직접 반환 (Bundle 아님)
public Order fetchOrder(OrderId orderId) {  // ❌
    return orderReadManager.findById(orderId);
}

// ❌ Bad: 비즈니스 로직 포함
public OrderDetailQueryBundle fetchOrderDetail(OrderDetailCriteria criteria) {
    Order order = orderReadManager.findById(criteria.orderId());
    if (order.isExpired()) {  // ❌ 비즈니스 로직
        throw new OrderExpiredException();
    }
}

// ❌ Bad: 객체 생성 (Factory 책임)
public OrderDetailQueryBundle fetchOrderDetail(OrderDetailQuery query) {
    OrderDetailCriteria criteria = new OrderDetailCriteria(...);  // ❌ Factory 사용
}
```

---

## 8) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] 2개 이상 ReadManager 의존
- [ ] `@Transactional(readOnly = true)` 메서드 단위 (클래스 레벨 금지)
- [ ] `fetch*()` 메서드 네이밍
- [ ] QueryBundle 반환
- [ ] 비즈니스 로직 없음
- [ ] 객체 생성 없음 (Factory 책임)
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.facade.query`

---

## 9) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[Facade Guide](../command/facade-guide.md)** - Command Facade
- **[QueryFactory Guide](../../factory/query/query-factory-guide.md)** - Query → Criteria
- **[ReadManager Guide](../../manager/query/read-manager-guide.md)** - 단일 조회
- **[QueryBundle Guide](../../dto/bundle/query-bundle-guide.md)** - Bundle 구조

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
