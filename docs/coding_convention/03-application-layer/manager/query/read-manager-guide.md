# Read Manager Guide — **단일 Port 조회**

> ReadManager는 **단일 Query Port**를 통해 조회를 수행합니다.
>
> **UseCase 또는 QueryFacade에서 호출**합니다.

---

## 1) 핵심 역할

* **단일 Query Port 조회**: Query Port 1개만 의존
* **UseCase/QueryFacade에서 호출**: 단순 조회는 UseCase, 복합 조회는 QueryFacade
* **Criteria 기반 조회**: Domain Criteria를 받아 조회
* **읽기 전용**: 영속화 작업 없음

---

## 2) 필수 규칙 (Zero-Tolerance)

| 규칙 | 설명 |
|------|------|
| **단일 Query Port 의존** | 2개 이상이면 QueryFacade로 |
| **`@Component` 어노테이션** | `@Service` 아님 |
| **`@Transactional(readOnly=true)` 메서드 단위** | 읽기 전용 트랜잭션 |
| **`find*()` / `get*()` 메서드 네이밍** | persist/save 등 금지 |
| **영속화 작업 금지** | TransactionManager 책임 |
| **비즈니스 로직 금지** | Domain Layer 책임 |
| **Lombok 금지** | 생성자 직접 작성 |

---

## 3) 패키지 구조

```
application/{bc}/
├─ manager/
│  ├─ command/
│  │  └─ {Bc}TransactionManager.java  ← Command용 (persist)
│  └─ query/
│     └─ {Bc}ReadManager.java         ← Query용 (find) - 이 문서
└─ port/
   └─ out/
      └─ query/
         └─ {Bc}QueryPort.java        ← ReadManager가 사용
```

---

## 4) TransactionManager vs ReadManager

| 구분 | TransactionManager | ReadManager |
|------|---------------------|-------------|
| **역할** | 영속화 (저장/수정/삭제) | 조회 |
| **의존성** | Command Port 1개 | Query Port 1개 |
| **트랜잭션** | `@Transactional` | `@Transactional(readOnly=true)` |
| **메서드** | `persist()` | `findBy*()`, `get*()` |
| **위치** | `manager/command/` | `manager/query/` |

---

## 5) 구현 예시

### 기본 구조

```java
package com.ryuqq.application.order.manager.query;

import com.ryuqq.application.port.out.query.OrderQueryPort;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.order.criteria.OrderSearchCriteria;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.order.vo.OrderId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Order Read Manager
 * - 단일 Query Port 조회
 * - 읽기 전용 트랜잭션
 */
@Component
public class OrderReadManager {

    private final OrderQueryPort orderQueryPort;

    public OrderReadManager(OrderQueryPort orderQueryPort) {
        this.orderQueryPort = orderQueryPort;
    }

    /**
     * ID로 단건 조회
     */
    @Transactional(readOnly = true)
    public Order findById(OrderId orderId) {
        return orderQueryPort.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * ID로 Optional 조회
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByIdOptional(OrderId orderId) {
        return orderQueryPort.findById(orderId);
    }

    /**
     * 고객 ID로 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(CustomerId customerId) {
        return orderQueryPort.findByCustomerId(customerId);
    }

    /**
     * Criteria 기반 검색
     */
    @Transactional(readOnly = true)
    public List<Order> findBy(OrderSearchCriteria criteria) {
        return orderQueryPort.findBy(criteria);
    }

    /**
     * Criteria 기반 페이징 검색
     */
    @Transactional(readOnly = true)
    public Page<Order> findPageBy(OrderSearchCriteria criteria) {
        return orderQueryPort.findPageBy(criteria);
    }

    /**
     * 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsById(OrderId orderId) {
        return orderQueryPort.existsById(orderId);
    }

    /**
     * 개수 조회
     */
    @Transactional(readOnly = true)
    public long countBy(OrderSearchCriteria criteria) {
        return orderQueryPort.countBy(criteria);
    }
}
```

### 통계 조회 예시

```java
/**
 * 통계 조회
 */
@Transactional(readOnly = true)
public OrderStatistics getStatistics(DateRange dateRange) {
    return orderQueryPort.getStatistics(dateRange);
}

/**
 * 집계 조회
 */
@Transactional(readOnly = true)
public List<OrderSummary> getSummaryByStatus(CustomerId customerId) {
    return orderQueryPort.getSummaryByStatus(customerId);
}

/**
 * 최근 N개 조회
 */
@Transactional(readOnly = true)
public List<Order> findRecent(int limit) {
    return orderQueryPort.findRecent(limit);
}
```

---

## 6) 메서드 네이밍 규칙

| 패턴 | 용도 | 반환 타입 |
|------|------|----------|
| `findById()` | 단건 조회 (필수) | Domain / throw Exception |
| `findByIdOptional()` | 단건 조회 (선택) | `Optional<Domain>` |
| `findBy*()` | 조건 조회 | `List<Domain>` |
| `findPageBy()` | 페이징 조회 | `Page<Domain>` |
| `existsById()` | 존재 여부 | `boolean` |
| `countBy()` | 개수 | `long` |
| `get*()` | 통계/집계 | Statistics, Summary 등 |

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: @Component 어노테이션
@Component
public class OrderReadManager { ... }

// ✅ Good: 단일 Query Port 의존
@Component
public class OrderReadManager {
    private final OrderQueryPort orderQueryPort;
}

// ✅ Good: @Transactional(readOnly = true) 메서드 단위
@Transactional(readOnly = true)
public Order findById(OrderId orderId) { ... }

// ✅ Good: find*/get* 메서드 네이밍
public Order findById(OrderId orderId) { ... }
public List<Order> findByCustomerId(CustomerId customerId) { ... }
public OrderStatistics getStatistics(DateRange range) { ... }

// ✅ Good: Criteria 기반 조회
public List<Order> findBy(OrderSearchCriteria criteria) {
    return orderQueryPort.findBy(criteria);
}
```

### ❌ Bad

```java
// ❌ Bad: @Service 어노테이션
@Service
public class OrderReadManager { ... }

// ❌ Bad: @Transactional 클래스 레벨
@Component
@Transactional(readOnly = true)
public class OrderReadManager { ... }

// ❌ Bad: 2개 이상 Port 의존
@Component
public class OrderReadManager {
    private final OrderQueryPort orderQueryPort;
    private final CustomerQueryPort customerQueryPort;  // ❌ QueryFacade로!
}

// ❌ Bad: persist/save 메서드
public Order persist(Order order) { ... }  // ❌ TransactionManager 역할

// ❌ Bad: 쓰기 작업
@Transactional(readOnly = true)
public Order updateStatus(OrderId orderId, OrderStatus status) {  // ❌
    Order order = findById(orderId);
    order.updateStatus(status);  // ❌ 쓰기 작업
    return order;
}

// ❌ Bad: 비즈니스 로직 포함
@Transactional(readOnly = true)
public Order findById(OrderId orderId) {
    Order order = orderQueryPort.findById(orderId)
        .orElseThrow(...);
    if (order.isExpired()) {  // ❌ 비즈니스 로직
        throw new OrderExpiredException();
    }
    return order;
}

// ❌ Bad: readOnly=true 누락
@Transactional  // ❌ readOnly = true 필요
public Order findById(OrderId orderId) { ... }
```

---

## 8) 체크리스트

- [ ] `@Component` 어노테이션
- [ ] 단일 Query Port 의존
- [ ] `@Transactional(readOnly = true)` 메서드 단위 (클래스 레벨 금지)
- [ ] `findBy*()` / `get*()` 메서드 네이밍
- [ ] 영속화 작업 없음 (조회만)
- [ ] 비즈니스 로직 없음
- [ ] Lombok 사용 안 함
- [ ] 패키지: `application.{bc}.manager.query`

---

## 9) 관련 문서

- **[Service Guide](../../service/service-guide.md)** - 전체 CQRS 흐름
- **[TransactionManager Guide](../command/transaction-manager-guide.md)** - Command Manager
- **[QueryFacade Guide](../../facade/query/query-facade-guide.md)** - 복합 조회
- **[QueryFactory Guide](../../factory/query/query-factory-guide.md)** - Criteria 생성

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
