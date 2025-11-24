# Persistence Layer TDD Green - Implement Minimum Code

You are in the 🟢 GREEN phase of Kent Beck's TDD cycle for **Persistence Layer**.

## Instructions

1. **Test is already FAILING** (RED phase complete)
2. **Write the SIMPLEST code** to make the test pass
3. **No premature optimization** - just make it work
4. **Run the test** and verify it PASSES
5. **Report success** clearly
6. **Commit with feat: prefix**:
   ```bash
   git add .
   git commit -m "feat: 주문 저장 Adapter 구현 (Long FK 전략)"
   ```

## Persistence Layer Implementation Guidelines

### Core Principles
- **Minimum Code**: Write only what's needed to pass the test
- **Long FK Strategy**: Use `private Long customerId;` (No JPA relationships)
- **Lombok 금지**: Pure Java in JPA Entity
- **Constructor Pattern**: Private/Protected constructor + Static factory method
- **QueryDSL DTO Projection**: Never query Entity directly

### Implementation Pattern

**Step 1: JPA Entity (Long FK 전략)**
```java
package com.company.template.persistence.entity;

import com.company.template.domain.OrderStatus;
import jakarta.persistence.*;

/**
 * Order JPA Entity.
 *
 * <p>Long FK 전략: JPA 관계 어노테이션 없이 Long FK만 사용합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // ✅ Long FK (관계 어노테이션 없음)

    @Column(name = "product_id", nullable = false)
    private Long productId;   // ✅ Long FK

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    // Protected constructor for JPA
    protected OrderJpaEntity() {
    }

    // Private constructor for factory method
    private OrderJpaEntity(String orderId, Long customerId, Long productId,
                           Integer quantity, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    /**
     * Factory method to create OrderJpaEntity.
     */
    public static OrderJpaEntity of(String orderId, Long customerId, Long productId,
                                    Integer quantity, OrderStatus status) {
        return new OrderJpaEntity(orderId, customerId, productId, quantity, status);
    }

    // Getters (Pure Java - No Lombok)
    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    // Setters (if needed for updates)
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}
```

**Step 2: BaseAuditEntity (Audit 정보)**
```java
package com.company.template.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base Audit Entity.
 *
 * <p>모든 Entity가 상속하는 Audit 정보를 담습니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
```

**Step 3: JPA Repository**
```java
package com.company.template.persistence.repository;

import com.company.template.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Order JPA Repository.
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    /**
     * orderId로 조회.
     */
    Optional<OrderJpaEntity> findByOrderId(String orderId);

    /**
     * orderId 존재 여부 확인.
     */
    boolean existsByOrderId(String orderId);
}
```

**Step 4: Command Adapter (저장)**
```java
package com.company.template.persistence.adapter;

import com.company.template.application.port.out.SaveOrderPort;
import com.company.template.domain.OrderDomain;
import com.company.template.persistence.entity.OrderJpaEntity;
import com.company.template.persistence.mapper.OrderEntityMapper;
import com.company.template.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 주문 저장 Adapter.
 *
 * <p>Domain → Entity 변환 후 저장합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class SaveOrderAdapter implements SaveOrderPort {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public OrderDomain save(OrderDomain domain) {
        // 1. Domain → Entity 변환
        OrderJpaEntity entity = OrderEntityMapper.toEntity(domain);

        // 2. Entity 저장
        OrderJpaEntity saved = orderJpaRepository.save(entity);

        // 3. Entity → Domain 변환
        return OrderEntityMapper.toDomain(saved);
    }
}
```

**Step 5: Query Adapter (조회 with DTO Projection)**
```java
package com.company.template.persistence.adapter;

import com.company.template.application.port.out.LoadOrderPort;
import com.company.template.domain.OrderDomain;
import com.company.template.persistence.dto.OrderDto;
import com.company.template.persistence.mapper.OrderEntityMapper;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.company.template.persistence.entity.QOrderJpaEntity.orderJpaEntity;

/**
 * 주문 조회 Adapter (QueryDSL DTO Projection).
 *
 * <p>Entity 직접 조회 금지, DTO Projection 사용 필수.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
@Component
@RequiredArgsConstructor
public class LoadOrderQueryAdapter implements LoadOrderPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<OrderDomain> loadById(String orderId) {
        // ✅ DTO Projection (Entity 직접 조회 금지)
        OrderDto dto = queryFactory
            .select(Projections.constructor(
                OrderDto.class,
                orderJpaEntity.orderId,
                orderJpaEntity.customerId,
                orderJpaEntity.productId,
                orderJpaEntity.quantity,
                orderJpaEntity.status
            ))
            .from(orderJpaEntity)
            .where(orderJpaEntity.orderId.eq(orderId))
            .fetchOne();

        return Optional.ofNullable(dto)
            .map(OrderEntityMapper::toDomain);
    }
}
```

**Step 6: DTO for QueryDSL Projection**
```java
package com.company.template.persistence.dto;

import com.company.template.domain.OrderStatus;

/**
 * Order DTO for QueryDSL Projection.
 *
 * @param orderId 주문 ID
 * @param customerId 고객 ID (Long FK)
 * @param productId 상품 ID (Long FK)
 * @param quantity 수량
 * @param status 주문 상태
 * @author Claude Code
 * @since 2025-01-13
 */
public record OrderDto(
    String orderId,
    Long customerId,
    Long productId,
    Integer quantity,
    OrderStatus status
) {}
```

**Step 7: Entity Mapper**
```java
package com.company.template.persistence.mapper;

import com.company.template.domain.OrderDomain;
import com.company.template.domain.OrderId;
import com.company.template.persistence.dto.OrderDto;
import com.company.template.persistence.entity.OrderJpaEntity;

/**
 * Order Entity Mapper.
 *
 * <p>Domain ↔ Entity, DTO → Domain 변환을 담당합니다.</p>
 *
 * @author Claude Code
 * @since 2025-01-13
 */
public class OrderEntityMapper {

    /**
     * Domain → Entity 변환.
     */
    public static OrderJpaEntity toEntity(OrderDomain domain) {
        return OrderJpaEntity.of(
            domain.getOrderId().getValue(),
            domain.getCustomerId(),
            domain.getProductId(),
            domain.getQuantity(),
            domain.getStatus()
        );
    }

    /**
     * Entity → Domain 변환.
     */
    public static OrderDomain toDomain(OrderJpaEntity entity) {
        return OrderDomain.reconstruct(
            new OrderId(entity.getOrderId()),
            entity.getCustomerId(),
            entity.getProductId(),
            entity.getQuantity(),
            entity.getStatus()
        );
    }

    /**
     * DTO → Domain 변환 (QueryDSL Projection 결과).
     */
    public static OrderDomain toDomain(OrderDto dto) {
        return OrderDomain.reconstruct(
            new OrderId(dto.orderId()),
            dto.customerId(),
            dto.productId(),
            dto.quantity(),
            dto.status()
        );
    }

    private OrderEntityMapper() {
        throw new AssertionError("Mapper 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

## GREEN Phase Workflow

**Step 1: Focus on the Failing Test**
```java
// Test from RED phase
@Test
@DisplayName("주문 저장 - 정상 케이스")
void shouldSaveOrder() {
    // Given
    OrderDomain domain = OrderDomainFixture.create();

    // When
    OrderDomain saved = saveOrderAdapter.save(domain);

    // Then
    assertThat(saved.getOrderId()).isNotNull();
}
```

**Step 2: Write Minimum Code (Adapter + Mapper)**
```java
// SaveOrderAdapter.java
@Component
@RequiredArgsConstructor
public class SaveOrderAdapter implements SaveOrderPort {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public OrderDomain save(OrderDomain domain) {
        OrderJpaEntity entity = OrderEntityMapper.toEntity(domain);
        OrderJpaEntity saved = orderJpaRepository.save(entity);
        return OrderEntityMapper.toDomain(saved);
    }
}
```

**Step 3: Run the Test**
```bash
./gradlew test --tests "*SaveOrderAdapterTest.shouldSaveOrder"
```

**Step 4: Verify GREEN**
```
✅ Test PASSED
```

## Persistence-Specific Implementation Patterns

### 1. Long FK 전략 (관계 어노테이션 금지)
```java
// ✅ CORRECT (Long FK)
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // ✅ Long FK

    @Column(name = "product_id", nullable = false)
    private Long productId;   // ✅ Long FK
}

// ❌ WRONG (JPA 관계 어노테이션)
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)  // ❌ 절대 금지!
    @JoinColumn(name = "customer_id")
    private CustomerJpaEntity customer;
}
```

### 2. Constructor 패턴
```java
// ✅ CORRECT (Protected + Factory Method)
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    // Protected constructor for JPA
    protected OrderJpaEntity() {
    }

    // Private constructor for factory method
    private OrderJpaEntity(String orderId, Long customerId, ...) {
        this.orderId = orderId;
        this.customerId = customerId;
    }

    // Factory method
    public static OrderJpaEntity of(String orderId, Long customerId, ...) {
        return new OrderJpaEntity(orderId, customerId, ...);
    }
}
```

### 3. QueryDSL DTO Projection (Entity 직접 조회 금지)
```java
// ✅ CORRECT (DTO Projection)
@Override
public Optional<OrderDomain> loadById(String orderId) {
    OrderDto dto = queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            orderJpaEntity.orderId,
            orderJpaEntity.customerId,
            orderJpaEntity.productId,
            orderJpaEntity.quantity,
            orderJpaEntity.status
        ))
        .from(orderJpaEntity)
        .where(orderJpaEntity.orderId.eq(orderId))
        .fetchOne();

    return Optional.ofNullable(dto)
        .map(OrderEntityMapper::toDomain);
}

// ❌ WRONG (Entity 직접 조회)
@Override
public Optional<OrderDomain> loadById(String orderId) {
    OrderJpaEntity entity = queryFactory
        .selectFrom(orderJpaEntity)  // ❌ Entity 직접 조회
        .where(orderJpaEntity.orderId.eq(orderId))
        .fetchOne();

    return Optional.ofNullable(entity)
        .map(OrderEntityMapper::toDomain);
}
```

### 4. Audit Entity 상속
```java
// ✅ CORRECT (BaseAuditEntity 상속)
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {
    // createdAt, updatedAt 자동 관리
}
```

### 5. Dynamic Query with QueryDSL
```java
@Override
public List<OrderDomain> findByCustomerId(Long customerId) {
    List<OrderDto> dtos = queryFactory
        .select(Projections.constructor(
            OrderDto.class,
            orderJpaEntity.orderId,
            orderJpaEntity.customerId,
            orderJpaEntity.productId,
            orderJpaEntity.quantity,
            orderJpaEntity.status
        ))
        .from(orderJpaEntity)
        .where(orderJpaEntity.customerId.eq(customerId))
        .fetch();

    return dtos.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}
```

## Common Mistakes to Avoid

### ❌ WRONG: JPA 관계 어노테이션
```java
// ❌ 절대 금지!
@ManyToOne
@JoinColumn(name = "customer_id")
private CustomerJpaEntity customer;
```

### ❌ WRONG: Lombok in Entity
```java
// ❌ Entity에 Lombok 사용 금지
@Entity
@Data  // ❌
@Builder  // ❌
public class OrderJpaEntity {
    // ...
}
```

### ❌ WRONG: Entity 직접 조회
```java
// ❌ QueryDSL에서 Entity 직접 조회 금지
OrderJpaEntity entity = queryFactory
    .selectFrom(orderJpaEntity)  // ❌
    .fetchOne();
```

## Success Criteria

- ✅ Test runs and PASSES
- ✅ Minimum code written (no extra features)
- ✅ Long FK strategy used (no JPA relationship annotations)
- ✅ No Lombok in JPA Entity
- ✅ Constructor pattern followed (Protected + Factory Method)
- ✅ QueryDSL DTO Projection used (no Entity direct query)
- ✅ BaseAuditEntity inherited

## What NOT to Do

- ❌ Don't write more code than needed to pass the test
- ❌ Don't add "nice to have" features
- ❌ Don't refactor yet (that's the next phase!)
- ❌ Don't use JPA relationship annotations
- ❌ Don't use Lombok in Entity
- ❌ Don't query Entity directly

This is Kent Beck's TDD: Write the SIMPLEST code to pass the test, then REFACTOR.
