# QueryDSL Repository 가이드

> **목적**: QueryDSL 기반 Repository 클래스 컨벤션 (Query 전용, 4개 메서드 표준화)

---

## 1️⃣ 핵심 원칙

### QueryDSL Repository는 4개 메서드만 제공

**표준 메서드**:
1. `findById(Long id)` - 단건 조회
2. `existsById(Long id)` - 존재 여부 확인
3. `findByCriteria(Criteria criteria)` - 목록 조회 (Criteria 기반 동적 쿼리)
4. `countByCriteria(Criteria criteria)` - 개수 조회

**규칙**:
- ✅ `@Repository` 클래스로 구현
- ✅ `JPAQueryFactory` 생성자 주입
- ✅ QType을 static final 상수로 선언
- ✅ **4개 메서드만 제공** (추가 메서드 금지)
- ✅ **Join 절대 금지** (성능보다 정확성과 빠른 개발 우선)
- ✅ 동적 쿼리 (BooleanExpression)
- ❌ 비즈니스 로직 작성 금지
- ❌ Mapper 호출 금지 (Adapter에서)
- ❌ Transaction 관리 금지 (Service Layer에서)
- ❌ **Join 사용 금지** (fetch join, left join, inner join 모두 금지)

**이유**:
- QueryDSL Repository는 **Query 작업 (find, exists, count)만** 담당
- **4개 메서드로 표준화**하여 일관성 있는 API 제공
- **Join 금지**로 복잡도 제거, N+1 문제는 Adapter에서 해결
- 타입 안전 쿼리로 컴파일 시점 검증
- 복잡한 동적 쿼리를 간결하게 표현

---

## 2️⃣ 기본 템플릿

```java
package com.company.adapter.out.persistence.order.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.company.adapter.out.persistence.order.entity.OrderJpaEntity;
import com.company.adapter.out.persistence.order.entity.QOrderJpaEntity;
import com.company.application.order.dto.query.SearchOrderQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OrderQueryDslRepository - Order QueryDSL Repository
 *
 * <p>QueryDSL 기반 조회 쿼리를 처리하는 전용 Repository입니다.</p>
 *
 * <p><strong>표준 메서드 (4개):</strong></p>
 * <ul>
 *   <li>findById(Long id): 단건 조회</li>
 *   <li>existsById(Long id): 존재 여부 확인</li>
 *   <li>findByCriteria(Criteria): 목록 조회 (동적 쿼리)</li>
 *   <li>countByCriteria(Criteria): 개수 조회 (동적 쿼리)</li>
 * </ul>
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>동적 쿼리 구성 (BooleanExpression)</li>
 *   <li>정렬 조건 구성 (OrderSpecifier)</li>
 *   <li>Offset/Cursor 페이징</li>
 * </ul>
 *
 * <p><strong>금지 사항:</strong></p>
 * <ul>
 *   <li>❌ Join 절대 금지 (fetch join, left join, inner join)</li>
 *   <li>❌ 추가 메서드 금지 (4개 메서드만 허용)</li>
 *   <li>❌ 비즈니스 로직 금지</li>
 *   <li>❌ Mapper 호출 금지</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Repository
public class OrderQueryDslRepository {

    private final JPAQueryFactory queryFactory;
    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;

    public OrderQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * ID로 Order 단건 조회
     *
     * @param id Order ID
     * @return OrderJpaEntity (Optional)
     */
    public Optional<OrderJpaEntity> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .fetchOne()
        );
    }

    /**
     * ID로 Order 존재 여부 확인
     *
     * @param id Order ID
     * @return 존재 여부
     */
    public boolean existsById(Long id) {
        Integer count = queryFactory
            .selectOne()
            .from(qOrder)
            .where(qOrder.id.eq(id))
            .fetchFirst();

        return count != null;
    }

    /**
     * 검색 조건으로 Order 목록 조회
     *
     * <p>Offset 페이징과 Cursor 페이징을 모두 지원합니다.</p>
     *
     * @param criteria 검색 조건 (SearchOrderQuery)
     * @return OrderJpaEntity 목록
     */
    public List<OrderJpaEntity> findByCriteria(SearchOrderQuery criteria) {
        var query = queryFactory
            .selectFrom(qOrder)
            .where(buildSearchConditions(criteria));

        // Cursor 페이징
        if (criteria.lastId() != null) {
            query = query.where(qOrder.id.gt(criteria.lastId()));
        }

        // Offset 페이징
        if (criteria.page() != null && criteria.size() != null) {
            query = query
                .offset((long) criteria.page() * criteria.size())
                .limit(criteria.size());
        } else if (criteria.size() != null) {
            // Cursor 전용 (size+1 조회)
            query = query.limit(criteria.size() + 1);
        }

        // 정렬
        if (criteria.sortBy() != null) {
            query = query.orderBy(buildOrderSpecifier(criteria));
        }

        return query.fetch();
    }

    /**
     * 검색 조건으로 Order 개수 조회
     *
     * @param criteria 검색 조건 (SearchOrderQuery)
     * @return Order 개수
     */
    public long countByCriteria(SearchOrderQuery criteria) {
        Long count = queryFactory
            .select(qOrder.count())
            .from(qOrder)
            .where(buildSearchConditions(criteria))
            .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 검색 조건 구성 (Private 헬퍼 메서드)
     *
     * <p>BooleanExpression을 사용하여 동적 쿼리를 구성합니다.</p>
     */
    private BooleanExpression buildSearchConditions(SearchOrderQuery criteria) {
        BooleanExpression expression = null;

        // 조건 1: 주문 번호
        if (criteria.orderNumber() != null && !criteria.orderNumber().isBlank()) {
            expression = qOrder.orderNumber.containsIgnoreCase(criteria.orderNumber());
        }

        // 조건 2: 상태
        if (criteria.status() != null) {
            BooleanExpression statusCondition = qOrder.status.eq(criteria.status());
            expression = expression != null ? expression.and(statusCondition) : statusCondition;
        }

        // 조건 3: 날짜 범위
        if (criteria.startDate() != null) {
            BooleanExpression dateCondition = qOrder.createdAt.goe(criteria.startDate());
            expression = expression != null ? expression.and(dateCondition) : dateCondition;
        }

        if (criteria.endDate() != null) {
            BooleanExpression dateCondition = qOrder.createdAt.loe(criteria.endDate());
            expression = expression != null ? expression.and(dateCondition) : dateCondition;
        }

        return expression;
    }

    /**
     * 정렬 조건 구성 (Private 헬퍼 메서드)
     */
    private OrderSpecifier<?> buildOrderSpecifier(SearchOrderQuery criteria) {
        String sortBy = criteria.sortBy();
        boolean isAsc = "ASC".equalsIgnoreCase(criteria.sortDirection());

        return switch (sortBy.toLowerCase()) {
            case "id" -> isAsc ? qOrder.id.asc() : qOrder.id.desc();
            case "ordernumber" -> isAsc ? qOrder.orderNumber.asc() : qOrder.orderNumber.desc();
            case "status" -> isAsc ? qOrder.status.asc() : qOrder.status.desc();
            default -> isAsc ? qOrder.createdAt.asc() : qOrder.createdAt.desc();
        };
    }
}
```

---

## 3️⃣ 예시

### ✅ 올바른 예시

```java
// ✅ 4개 메서드만 제공
@Repository
public class OrderQueryDslRepository {
    private final JPAQueryFactory queryFactory;
    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;

    public OrderQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    // ✅ 1. 단건 조회
    public Optional<OrderJpaEntity> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .fetchOne()
        );
    }

    // ✅ 2. 존재 여부 확인
    public boolean existsById(Long id) {
        Integer count = queryFactory
            .selectOne()
            .from(qOrder)
            .where(qOrder.id.eq(id))
            .fetchFirst();

        return count != null;
    }

    // ✅ 3. 목록 조회 (동적 쿼리, Join 없음)
    public List<OrderJpaEntity> findByCriteria(SearchOrderQuery criteria) {
        return queryFactory.selectFrom(qOrder)
            .where(buildConditions(criteria))
            .fetch();
    }

    // ✅ 4. 개수 조회
    public long countByCriteria(SearchOrderQuery criteria) {
        Long count = queryFactory
            .select(qOrder.count())
            .from(qOrder)
            .where(buildConditions(criteria))
            .fetchOne();

        return count != null ? count : 0L;
    }

    // ✅ Private 헬퍼 메서드
    private BooleanExpression buildConditions(SearchOrderQuery criteria) {
        // 동적 쿼리 구성
    }
}
```

### ❌ 위반 예시

```java
// ❌ Join 사용 금지
@Repository
public class OrderQueryDslRepository {
    public List<OrderJpaEntity> findWithCustomer(Long customerId) {  // ❌ 추가 메서드
        return queryFactory.selectFrom(qOrder)
            .join(qCustomer).on(qOrder.customerId.eq(qCustomer.id))  // ❌ Join 금지
            .where(qCustomer.id.eq(customerId))
            .fetch();
    }
}

// ❌ 추가 메서드 금지
@Repository
public class OrderQueryDslRepository {
    // 4개 메서드 외 추가 메서드 금지
    public List<OrderJpaEntity> findByStatus(OrderStatus status) {  // ❌
        return queryFactory.selectFrom(qOrder)
            .where(qOrder.status.eq(status))
            .fetch();
    }

    // ✅ findByCriteria()로 통합 처리
    public List<OrderJpaEntity> findByCriteria(SearchOrderQuery criteria) {
        return queryFactory.selectFrom(qOrder)
            .where(buildConditions(criteria))  // status 조건 포함
            .fetch();
    }
}

// ❌ Fetch Join 금지
@Repository
public class ProductQueryDslRepository {
    public List<ProductJpaEntity> findWithCategory(Long categoryId) {
        return queryFactory.selectFrom(qProduct)
            .join(qProduct.category, qCategory).fetchJoin()  // ❌ Fetch Join 금지
            .where(qCategory.id.eq(categoryId))
            .fetch();
    }
}

// ❌ Mapper 호출 금지
@Repository
public class OrderQueryDslRepository {
    private final OrderJpaEntityMapper mapper;  // ❌

    public List<OrderDomain> findByCriteria(SearchOrderQuery criteria) {  // ❌
        List<OrderJpaEntity> entities = queryFactory.selectFrom(qOrder).fetch();
        return entities.stream()
            .map(mapper::toDomain)  // ❌ Adapter에서 처리
            .toList();
    }
}

// ❌ @Transactional 사용 금지
@Repository
@Transactional  // ❌ Service Layer에서 관리
public class OrderQueryDslRepository {
}
```

---

## 4️⃣ Join 금지 정책

### 왜 Join을 금지하는가?

1. **N+1 문제는 Adapter에서 해결**: QueryAdapter에서 Mapper로 변환할 때 추가 조회
2. **빠른 개발**: Join 없이 단순 쿼리만 작성
3. **정확성 우선**: 복잡한 Join 로직 실수 방지
4. **성능보다 안정성**: 성능은 Cache로 해결

### N+1 해결 방법 (Adapter에서)

```java
// ❌ QueryDslRepository에서 Join (금지!)
@Repository
public class OrderQueryDslRepository {
    public List<OrderJpaEntity> findWithCustomer(Long customerId) {
        return queryFactory.selectFrom(qOrder)
            .join(qCustomer).on(qOrder.customerId.eq(qCustomer.id))  // ❌
            .where(qCustomer.id.eq(customerId))
            .fetch();
    }
}

// ✅ QueryAdapter에서 N+1 해결
@Component
public class OrderQueryAdapter implements OrderQueryPort {
    private final OrderQueryDslRepository orderRepository;
    private final CustomerQueryDslRepository customerRepository;  // ✅
    private final OrderJpaEntityMapper mapper;

    @Override
    public List<OrderDomain> findByCriteria(SearchOrderQuery criteria) {
        // 1. Order 조회
        List<OrderJpaEntity> orders = orderRepository.findByCriteria(criteria);

        // 2. Customer ID 추출
        Set<Long> customerIds = orders.stream()
            .map(OrderJpaEntity::getCustomerId)
            .collect(Collectors.toSet());

        // 3. Customer 일괄 조회 (N+1 해결)
        Map<Long, CustomerJpaEntity> customerMap = customerRepository
            .findByIds(customerIds)
            .stream()
            .collect(Collectors.toMap(CustomerJpaEntity::getId, Function.identity()));

        // 4. Mapper로 변환 (Customer 정보 포함)
        return orders.stream()
            .map(order -> mapper.toDomain(order, customerMap.get(order.getCustomerId())))
            .toList();
    }
}
```

---

## 5️⃣ Query Adapter에서 사용

```java
@Component
public class OrderQueryAdapter implements OrderQueryPort {

    private final OrderQueryDslRepository queryDslRepository;  // ✅ QueryDSL Repository
    private final OrderJpaEntityMapper mapper;

    public OrderQueryAdapter(
        OrderQueryDslRepository queryDslRepository,
        OrderJpaEntityMapper mapper
    ) {
        this.queryDslRepository = queryDslRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OrderDomain> findById(OrderId orderId) {
        return queryDslRepository.findById(orderId.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(OrderId orderId) {
        return queryDslRepository.existsById(orderId.getValue());
    }

    @Override
    public List<OrderDomain> findByCriteria(SearchOrderQuery criteria) {
        List<OrderJpaEntity> entities = queryDslRepository.findByCriteria(criteria);
        return entities.stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public long countByCriteria(SearchOrderQuery criteria) {
        return queryDslRepository.countByCriteria(criteria);
    }
}
```

---

## 6️⃣ 동적 쿼리 구성 패턴

### BooleanExpression 조합

```java
private BooleanExpression buildSearchConditions(SearchOrderQuery criteria) {
    BooleanExpression expression = null;

    // 조건 1: 주문 번호
    if (criteria.orderNumber() != null && !criteria.orderNumber().isBlank()) {
        expression = qOrder.orderNumber.containsIgnoreCase(criteria.orderNumber());
    }

    // 조건 2: 상태
    if (criteria.status() != null) {
        BooleanExpression statusCondition = qOrder.status.eq(criteria.status());
        expression = expression != null ? expression.and(statusCondition) : statusCondition;
    }

    // 조건 3: 날짜 범위
    if (criteria.startDate() != null) {
        BooleanExpression dateCondition = qOrder.createdAt.goe(criteria.startDate());
        expression = expression != null ? expression.and(dateCondition) : dateCondition;
    }

    return expression;
}
```

### 정렬 조건 구성

```java
private OrderSpecifier<?> buildOrderSpecifier(SearchOrderQuery criteria) {
    String sortBy = criteria.sortBy() != null ? criteria.sortBy() : "createdAt";
    boolean isAsc = "ASC".equalsIgnoreCase(criteria.sortDirection());

    return switch (sortBy.toLowerCase()) {
        case "id" -> isAsc ? qOrder.id.asc() : qOrder.id.desc();
        case "ordernumber" -> isAsc ? qOrder.orderNumber.asc() : qOrder.orderNumber.desc();
        case "status" -> isAsc ? qOrder.status.asc() : qOrder.status.desc();
        default -> isAsc ? qOrder.createdAt.asc() : qOrder.createdAt.desc();
    };
}
```

---

## 7️⃣ 페이징 전략

### Offset 페이징

```java
public List<OrderJpaEntity> findByCriteria(SearchOrderQuery criteria) {
    var query = queryFactory
        .selectFrom(qOrder)
        .where(buildSearchConditions(criteria));

    // Offset 페이징
    if (criteria.page() != null && criteria.size() != null) {
        query = query
            .offset((long) criteria.page() * criteria.size())
            .limit(criteria.size());
    }

    return query.fetch();
}
```

### Cursor 페이징

```java
public List<OrderJpaEntity> findByCriteria(SearchOrderQuery criteria) {
    var query = queryFactory
        .selectFrom(qOrder)
        .where(buildSearchConditions(criteria));

    // Cursor 페이징
    if (criteria.lastId() != null) {
        query = query.where(qOrder.id.gt(criteria.lastId()));
    }

    // size+1 조회 (hasNext 판단용)
    if (criteria.size() != null) {
        query = query.limit(criteria.size() + 1);
    }

    return query.fetch();
}
```

---

## 8️⃣ 디렉토리 구조

```
adapter-out/persistence-mysql/
└─ src/main/java/
   └─ com/company/adapter/out/persistence/
       └─ order/
           ├─ entity/
           │  └─ OrderJpaEntity.java
           ├─ repository/
           │  ├─ OrderRepository.java          (JPA Repository - Command)
           │  └─ OrderQueryDslRepository.java  ⭐ QueryDSL Repository (Query, 4개 메서드)
           └─ adapter/
              ├─ OrderCommandPersistenceAdapter.java  (JPA Repository 사용)
              └─ OrderQueryPersistenceAdapter.java    ⭐ (QueryDSL Repository 사용)
```

---

## 9️⃣ 체크리스트

QueryDSL Repository 작성 시:
- [ ] **클래스 구조**
  - [ ] `@Repository` 어노테이션
  - [ ] `JPAQueryFactory` 생성자 주입
  - [ ] QType을 static final 상수로 선언
- [ ] **표준 메서드 (4개만)**
  - [ ] findById(Long id)
  - [ ] existsById(Long id)
  - [ ] findByCriteria(Criteria criteria)
  - [ ] countByCriteria(Criteria criteria)
- [ ] **쿼리 구성**
  - [ ] 동적 쿼리: private BooleanExpression 메서드
  - [ ] 정렬 조건: private OrderSpecifier 메서드
  - [ ] Offset/Cursor 페이징 지원
- [ ] **금지 사항**
  - [ ] Join 절대 금지 (fetch join, left join, inner join)
  - [ ] 추가 메서드 금지 (4개만 허용)
  - [ ] 비즈니스 로직 없음
  - [ ] Mapper 호출 없음
  - [ ] @Transactional 없음

---

## 🔟 참고 문서

- [jpa-repository-guide.md](./jpa-repository-guide.md) - JPA Repository 가이드
- [querydsl-repository-archunit.md](./querydsl-repository-archunit.md) - ArchUnit 규칙
- [repository-test-guide.md](./repository-test-guide.md) - 테스트 가이드
- [query-adapter-guide.md](../adapter/query/query-adapter-guide.md) - QueryAdapter 가이드
- [query-dto-guide.md](../../../../03-application-layer/dto/query/query-dto-guide.md) - Query DTO 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 2.0.0 (4개 메서드 표준화 + Join 금지)
