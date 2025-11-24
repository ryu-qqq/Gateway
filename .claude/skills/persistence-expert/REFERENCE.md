# Persistence Layer 전문가 - API 참조

Persistence Layer Zero-Tolerance 규칙 전체, Long FK 전략, QueryDSL 패턴을 제공합니다.

## Zero-Tolerance 규칙 요약

### ✅ Mandatory
1. Long FK 전략 (`private Long userId`)
2. Command/Query Adapter 분리
3. QueryDSL DTO Projection
4. No N+1 (fetch join)
5. BaseAuditEntity 상속
6. Constructor Pattern
7. Flyway 필수

### ❌ Prohibited
1. No JPA 관계 어노테이션 (`@ManyToOne`, `@OneToMany`)
2. No Entity Graph
3. No Lazy Loading
4. No Entity 직접 반환
5. No Setter
6. No Lombok
7. No JPQL String

## JPA Entity 템플릿

```java
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // ✅ Long FK (관계 어노테이션 금지)

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    public OrderJpaEntity(Long customerId, String status) {
        this.customerId = customerId;
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}
```

## QueryDSL DTO Projection

```java
@Component
@RequiredArgsConstructor
public class OrderQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public List<OrderDto> findAll() {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

        return queryFactory
            .select(Projections.constructor(
                OrderDto.class,
                order.orderId,
                order.customerId,
                order.status
            ))
            .from(order)
            .fetch();
    }
}
```

## Zero-Tolerance 규칙 상세

### ✅ Mandatory 상세 설명

#### 1. Long FK 전략
모든 연관관계는 Long 타입 외래키로 표현합니다.

```java
// ✅ CORRECT (Long FK)
@Entity
public class OrderJpaEntity extends BaseAuditEntity {
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
}

// ❌ WRONG (JPA 관계 어노테이션)
@Entity
public class OrderJpaEntity extends BaseAuditEntity {
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerJpaEntity customer;
}
```

#### 2. Command/Query Adapter 분리
Command (저장)와 Query (조회)를 별도 Adapter로 분리합니다.

```java
// ✅ CORRECT (Command Adapter)
@Component
@RequiredArgsConstructor
public class OrderCommandPersistenceAdapter implements SaveOrderPort {
    private final OrderRepository orderRepository;
    private final OrderEntityMapper orderEntityMapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = orderEntityMapper.toEntity(order);
        OrderJpaEntity saved = orderRepository.save(entity);
        return orderEntityMapper.toDomain(saved);
    }
}

// ✅ CORRECT (Query Adapter)
@Component
@RequiredArgsConstructor
public class OrderQueryPersistenceAdapter implements LoadOrderPort {
    private final OrderQueryDslRepository orderQueryDslRepository;

    @Override
    public Optional<Order> loadById(String orderId) {
        return orderQueryDslRepository.findById(Long.parseLong(orderId))
            .map(orderEntityMapper::toDomain);
    }
}
```

#### 3. QueryDSL DTO Projection
Entity 반환 금지, DTO Projection 필수.

```java
// ✅ CORRECT (DTO Projection)
public List<OrderDto> findAll() {
    QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

    return queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            order.orderId,
            order.customerId,
            order.status
        ))
        .from(order)
        .fetch();
}

// ❌ WRONG (Entity 반환)
public List<OrderJpaEntity> findAll() {
    return queryFactory
        .selectFrom(QOrderJpaEntity.orderJpaEntity)
        .fetch();
}
```

#### 4. No N+1 (fetch join)
항상 fetch join으로 N+1 문제 방지.

```java
// ✅ CORRECT (fetch join)
public List<OrderDto> findAllWithItems() {
    QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;
    QOrderItemJpaEntity item = QOrderItemJpaEntity.orderItemJpaEntity;

    return queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            order.orderId,
            order.customerId,
            order.status
        ))
        .from(order)
        .leftJoin(item).on(item.orderId.eq(order.orderId))
        .fetch();
}

// ❌ WRONG (N+1 발생)
public List<OrderDto> findAllWithItems() {
    List<OrderDto> orders = queryFactory.select(...).from(order).fetch();
    // N+1 발생: items를 별도 조회
}
```

#### 5. BaseAuditEntity 상속
createdAt, updatedAt 자동 관리.

```java
// ✅ CORRECT
@Entity
public class OrderJpaEntity extends BaseAuditEntity {
    // createdAt, updatedAt 자동 생성
}

// BaseAuditEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

#### 6. Constructor Pattern
Protected 기본 생성자 + Public 생성자.

```java
// ✅ CORRECT
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long customerId;
    private String status;

    public OrderJpaEntity(Long customerId, String status) {
        this.customerId = customerId;
        this.status = status;
    }
}
```

#### 7. Flyway 필수
Schema 버전 관리.

```sql
-- V1__create_order_table.sql
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
```

### ❌ Prohibited 상세 설명

#### 1. No JPA 관계 어노테이션
`@ManyToOne`, `@OneToMany` 등 모두 금지.

```java
// ❌ WRONG
@Entity
public class OrderJpaEntity {
    @ManyToOne
    private CustomerJpaEntity customer;

    @OneToMany(mappedBy = "order")
    private List<OrderItemJpaEntity> items;
}

// ✅ CORRECT
@Entity
public class OrderJpaEntity {
    private Long customerId;
    // items는 별도 조회
}
```

#### 2. No Entity Graph
Long FK 전략만 사용.

```java
// ❌ WRONG
@EntityGraph(attributePaths = {"customer", "items"})
Order findByIdWithCustomerAndItems(Long id);

// ✅ CORRECT (QueryDSL fetch join)
public OrderDto findByIdWithCustomerAndItems(Long id) {
    return queryFactory
        .select(Projections.constructor(...))
        .from(order)
        .leftJoin(customer).on(...)
        .leftJoin(item).on(...)
        .where(order.id.eq(id))
        .fetchOne();
}
```

#### 3. No Lazy Loading
QueryDSL로 명시적 join.

```java
// ❌ WRONG
@ManyToOne(fetch = FetchType.LAZY)
private CustomerJpaEntity customer;

// ✅ CORRECT (QueryDSL 명시적 join)
public OrderDto findById(Long id) {
    return queryFactory
        .select(Projections.constructor(...))
        .from(order)
        .leftJoin(customer).on(...)
        .where(order.id.eq(id))
        .fetchOne();
}
```

#### 4. No Entity 직접 반환
DTO Projection 필수.

```java
// ❌ WRONG
public List<OrderJpaEntity> findAll() {
    return orderRepository.findAll();
}

// ✅ CORRECT
public List<OrderDto> findAll() {
    return queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            order.orderId,
            order.customerId,
            order.status
        ))
        .from(order)
        .fetch();
}
```

#### 5. No Setter
Entity 상태 변경은 메서드로.

```java
// ❌ WRONG
@Entity
public class OrderJpaEntity {
    private String status;

    public void setStatus(String status) {
        this.status = status;
    }
}

// ✅ CORRECT
@Entity
public class OrderJpaEntity {
    private String status;

    public void changeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
```

#### 6. No Lombok
Pure Java getter만 사용.

```java
// ❌ WRONG
@Entity
@Data
@Builder
public class OrderJpaEntity { ... }

// ✅ CORRECT
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }
}
```

#### 7. No JPQL String
QueryDSL 사용.

```java
// ❌ WRONG
@Query("SELECT o FROM OrderJpaEntity o WHERE o.customerId = :customerId")
List<OrderJpaEntity> findByCustomerId(@Param("customerId") Long customerId);

// ✅ CORRECT (QueryDSL)
public List<OrderDto> findByCustomerId(Long customerId) {
    return queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            order.orderId,
            order.customerId,
            order.status
        ))
        .from(order)
        .where(order.customerId.eq(customerId))
        .fetch();
}
```

## 코드 템플릿

### Command Adapter 템플릿

```java
package com.company.template.adapter.out.persistence.order.adapter;

import com.company.template.adapter.out.persistence.order.entity.OrderJpaEntity;
import com.company.template.adapter.out.persistence.order.mapper.OrderEntityMapper;
import com.company.template.adapter.out.persistence.order.repository.OrderRepository;
import com.company.template.application.order.port.out.SaveOrderPort;
import com.company.template.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Order Command Persistence Adapter.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class OrderCommandPersistenceAdapter implements SaveOrderPort {

    private final OrderRepository orderRepository;
    private final OrderEntityMapper orderEntityMapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = orderEntityMapper.toEntity(order);
        OrderJpaEntity saved = orderRepository.save(entity);
        return orderEntityMapper.toDomain(saved);
    }

    @Override
    public void delete(Order order) {
        orderRepository.deleteById(order.getOrderIdValue());
    }
}
```

### Query Adapter 템플릿

```java
package com.company.template.adapter.out.persistence.order.adapter;

import com.company.template.adapter.out.persistence.order.repository.OrderQueryDslRepository;
import com.company.template.application.order.port.out.LoadOrderPort;
import com.company.template.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Order Query Persistence Adapter.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class OrderQueryPersistenceAdapter implements LoadOrderPort {

    private final OrderQueryDslRepository orderQueryDslRepository;

    @Override
    public Optional<Order> loadById(String orderId) {
        return orderQueryDslRepository.findById(Long.parseLong(orderId));
    }

    @Override
    public List<Order> loadByCustomerId(Long customerId) {
        return orderQueryDslRepository.findByCustomerId(customerId);
    }
}
```

### QueryDSL Repository 템플릿

```java
package com.company.template.adapter.out.persistence.order.repository;

import com.company.template.adapter.out.persistence.order.entity.QOrderJpaEntity;
import com.company.template.domain.order.Order;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Order QueryDSL Repository.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class OrderQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Order> findById(Long orderId) {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

        Order result = queryFactory
            .select(Projections.constructor(
                Order.class,
                order.orderId,
                order.customerId,
                order.status
            ))
            .from(order)
            .where(order.orderId.eq(orderId))
            .fetchOne();

        return Optional.ofNullable(result);
    }

    public List<Order> findByCustomerId(Long customerId) {
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

        return queryFactory
            .select(Projections.constructor(
                Order.class,
                order.orderId,
                order.customerId,
                order.status
            ))
            .from(order)
            .where(order.customerId.eq(customerId))
            .fetch();
    }
}
```

## Anti-patterns (자주 하는 실수)

### 1. JPA 관계 어노테이션 사용
```java
// ❌ WRONG
@Entity
public class OrderJpaEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerJpaEntity customer;
}

// ✅ CORRECT
@Entity
public class OrderJpaEntity {
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
}
```

### 2. Entity 직접 반환
```java
// ❌ WRONG
public List<OrderJpaEntity> findAll() {
    return orderRepository.findAll();
}

// ✅ CORRECT
public List<OrderDto> findAll() {
    return queryFactory
        .select(Projections.constructor(OrderDto.class, ...))
        .from(order)
        .fetch();
}
```

### 3. JPQL String 사용
```java
// ❌ WRONG
@Query("SELECT o FROM OrderJpaEntity o WHERE o.customerId = :customerId")
List<OrderJpaEntity> findByCustomerId(@Param("customerId") Long customerId);

// ✅ CORRECT
public List<OrderDto> findByCustomerId(Long customerId) {
    return queryFactory
        .select(Projections.constructor(OrderDto.class, ...))
        .from(order)
        .where(order.customerId.eq(customerId))
        .fetch();
}
```

## Checklist

- [ ] Long FK 전략 (`private Long userId`)
- [ ] Command/Query Adapter 분리
- [ ] QueryDSL DTO Projection
- [ ] No N+1 (fetch join)
- [ ] BaseAuditEntity 상속
- [ ] Constructor Pattern
- [ ] Flyway 필수
- [ ] No JPA 관계 어노테이션
- [ ] No Entity Graph
- [ ] No Lazy Loading
- [ ] No Entity 직접 반환
- [ ] No Setter
- [ ] No Lombok
- [ ] No JPQL String

## 상세 문서
- docs/coding_convention/04-persistence-layer/ 하위 모든 문서 참조
