# Query Factory Guide — **Query DTO → Criteria 변환**

> QueryFactory는 **Query DTO를 Domain Criteria로 변환**합니다.
>
> **UseCase에서 호출**하며, **비즈니스 로직 없이 변환만** 수행합니다.

---

## 1) 핵심 역할

* **Query → Criteria 변환**: Application DTO를 Domain Criteria로 변환
* **UseCase에서 호출**: Query Service에서 직접 사용
* **변환만 수행**: 비즈니스 로직 없음, 조회 없음
* **Domain Criteria 사용**: Domain Layer에 정의된 조회 조건 객체

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **`@Component` 어노테이션** | `@Service` 아님 |
| **`createCriteria*()` 메서드 네이밍** | Criteria 생성 시 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **조회 금지** | Port 호출 금지 |
| **Lombok 금지** | 생성자 직접 작성 |
| **`@Transactional` 금지** | 트랜잭션 책임 아님 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ factory/
│  ├─ command/
│  │  └─ {Bc}CommandFactory.java     ← Command → Domain
│  └─ query/
│     └─ {Bc}QueryFactory.java       ← Query → Criteria (이 문서)
└─ service/
   └─ query/
      └─ GetOrderService.java        ← Factory 사용처
```

---

## 4) Domain Criteria

### Criteria란?

Domain Layer에 정의된 **조회 조건 객체**입니다.

```java
// domain/{bc}/criteria/OrderSearchCriteria.java
package com.ryuqq.domain.order.criteria;

import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.vo.OrderStatus;

import java.time.LocalDate;

/**
 * Order 검색 Criteria
 * - Domain Layer에 위치
 * - VO 사용으로 타입 안전성 보장
 */
public record OrderSearchCriteria(
    CustomerId customerId,
    OrderStatus status,
    LocalDate fromDate,
    LocalDate toDate,
    int page,
    int size
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CustomerId customerId;
        private OrderStatus status;
        private LocalDate fromDate;
        private LocalDate toDate;
        private int page = 0;
        private int size = 20;

        public Builder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public OrderSearchCriteria build() {
            return new OrderSearchCriteria(
                customerId, status, fromDate, toDate, page, size
            );
        }
    }
}
```

---

## 5) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.factory.query;

import com.ryuqq.application.order.dto.query.OrderDetailQuery;
import com.ryuqq.application.order.dto.query.OrderSearchQuery;
import com.ryuqq.domain.order.criteria.OrderDetailCriteria;
import com.ryuqq.domain.order.criteria.OrderSearchCriteria;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.vo.OrderStatus;
import org.springframework.stereotype.Component;

/**
 * Order Query Factory
 * - Query DTO → Domain Criteria 변환
 * - 비즈니스 로직 없음 (순수 변환)
 */
@Component
public class OrderQueryFactory {

    /**
     * OrderSearchQuery → OrderSearchCriteria
     */
    public OrderSearchCriteria createSearchCriteria(OrderSearchQuery query) {
        return OrderSearchCriteria.builder()
            .customerId(query.customerId() != null
                ? new CustomerId(query.customerId())
                : null)
            .status(query.status() != null
                ? OrderStatus.valueOf(query.status())
                : null)
            .fromDate(query.fromDate())
            .toDate(query.toDate())
            .page(query.page())
            .size(query.size())
            .build();
    }

    /**
     * OrderDetailQuery → OrderDetailCriteria
     */
    public OrderDetailCriteria createDetailCriteria(OrderDetailQuery query) {
        return new OrderDetailCriteria(
            new OrderId(query.orderId()),
            query.includeItems(),
            query.includeShipping(),
            query.includePayment()
        );
    }

    /**
     * 단순 ID 기반 Criteria
     */
    public OrderDetailCriteria createByIdCriteria(Long orderId) {
        return new OrderDetailCriteria(
            new OrderId(orderId),
            true,   // includeItems
            true,   // includeShipping
            true    // includePayment
        );
    }
}
```

### 복잡한 변환 예시

```java
/**
 * 여러 조건을 조합하는 Criteria
 */
public OrderAnalyticsCriteria createAnalyticsCriteria(OrderAnalyticsQuery query) {
    // 날짜 범위 검증 및 기본값
    LocalDate fromDate = query.fromDate() != null
        ? query.fromDate()
        : LocalDate.now().minusMonths(1);

    LocalDate toDate = query.toDate() != null
        ? query.toDate()
        : LocalDate.now();

    // 상태 목록 변환
    List<OrderStatus> statuses = query.statuses() != null
        ? query.statuses().stream()
            .map(OrderStatus::valueOf)
            .toList()
        : List.of();

    return OrderAnalyticsCriteria.builder()
        .customerId(new CustomerId(query.customerId()))
        .statuses(statuses)
        .dateRange(new DateRange(fromDate, toDate))
        .groupBy(GroupingType.valueOf(query.groupBy()))
        .build();
}

/**
 * 페이징 + 정렬 조건 포함
 */
public OrderListCriteria createListCriteria(OrderListQuery query) {
    SortDirection direction = "desc".equalsIgnoreCase(query.sortDirection())
        ? SortDirection.DESC
        : SortDirection.ASC;

    return OrderListCriteria.builder()
        .customerId(new CustomerId(query.customerId()))
        .status(OrderStatus.valueOf(query.status()))
        .sortBy(SortField.valueOf(query.sortBy()))
        .sortDirection(direction)
        .pageRequest(new PageRequest(query.page(), query.size()))
        .build();
}
```

---

## 6) CommandFactory vs QueryFactory

| 구분 | CommandFactory | QueryFactory |
|------|----------------|--------------|
| **역할** | Command → Domain | Query → Criteria |
| **입력** | Command DTO | Query DTO |
| **출력** | Domain, PersistBundle | Criteria |
| **위치** | `factory/command/` | `factory/query/` |
| **메서드** | `create*()`, `createBundle()` | `createCriteria*()` |
| **사용처** | Command Service | Query Service |

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Component 어노테이션
@Component
public class OrderQueryFactory { ... }

// ✅ Good: createCriteria* 메서드 네이밍
public OrderSearchCriteria createSearchCriteria(OrderSearchQuery query) { ... }
public OrderDetailCriteria createDetailCriteria(OrderDetailQuery query) { ... }

// ✅ Good: 순수 변환 (비즈니스 로직 없음)
public OrderSearchCriteria createSearchCriteria(OrderSearchQuery query) {
    return OrderSearchCriteria.builder()
        .customerId(new CustomerId(query.customerId()))
        .status(OrderStatus.valueOf(query.status()))
        .build();
}

// ✅ Good: null 안전 처리
.customerId(query.customerId() != null
    ? new CustomerId(query.customerId())
    : null)
```

### ❌ Bad

```java
// ❌ Bad: @Service 어노테이션
@Service
public class OrderQueryFactory { ... }

// ❌ Bad: @Transactional 사용
@Component
public class OrderQueryFactory {
    @Transactional  // ❌ Factory는 트랜잭션 책임 없음
    public OrderSearchCriteria createSearchCriteria(...) { ... }
}

// ❌ Bad: Port 호출 (조회)
@Component
public class OrderQueryFactory {
    private final CustomerQueryPort customerPort;  // ❌

    public OrderSearchCriteria createSearchCriteria(...) {
        Customer customer = customerPort.findById(...);  // ❌ 조회 금지
    }
}

// ❌ Bad: 비즈니스 로직 포함
public OrderSearchCriteria createSearchCriteria(OrderSearchQuery query) {
    if (query.fromDate().isAfter(query.toDate())) {  // ❌ 비즈니스 로직
        throw new BusinessException("Invalid date range");
    }
}

// ❌ Bad: create() 메서드명 (Criteria 아님)
public OrderSearchCriteria create(OrderSearchQuery query) { ... }  // ❌
```

---

## 8) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] `createCriteria*()` 메서드 네이밍
- [ ] 비즈니스 로직 없음 (순수 변환)
- [ ] Port 호출 없음 (조회 금지)
- [ ] `@Transactional` 없음
- [ ] Domain Criteria 사용 (VO 포함)
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.factory.query`

---

## 9) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[CommandFactory Guide](../command/command-factory-guide.md)** - Command 변환
- **[QueryFacade Guide](../../facade/query/query-facade-guide.md)** - 복잡한 Query 조합
- **[ReadManager Guide](../../manager/query/read-manager-guide.md)** - 단일 조회

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
