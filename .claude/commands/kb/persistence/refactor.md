# Persistence Layer TDD Refactor - Improve Structure

You are in the ♻️ REFACTOR phase of Kent Beck's TDD cycle for **Persistence Layer**.

## Tidy First Principle

**CRITICAL**: Refactoring = **Structural Changes ONLY** (동작 변경 없음!)

- ✅ **DO**: Extract QueryDSL projection, extract Mapper methods, improve query readability
- ❌ **DON'T**: Add new queries, change FK strategy, modify Entity schema
- 🧪 **VERIFY**: Tests pass before AND after (same results)

**Kent Beck's Rule**: "Separate Structural Changes from Behavioral Changes!"

## Instructions

1. **Test is already PASSING** (GREEN phase complete)
2. **Improve code structure** without changing behavior (Structural Changes only!)
3. **Apply design patterns** and best practices
4. **Ensure Zero-Tolerance compliance** (Long FK, Lombok 금지, QueryDSL DTO Projection)
5. **Run all tests** after each refactoring step
6. **Commit with struct: prefix**:
   ```bash
   git add .
   git commit -m "struct: extract OrderEntityMapper projection logic"
   ```
7. **IMPORTANT**: Never mix Structural and Behavioral changes!

## Refactoring Goals

- **Clarity**: Make code easier to understand
- **Maintainability**: Make code easier to change
- **Query Optimization**: Improve QueryDSL performance
- **Long FK Integrity**: Maintain FK strategy consistency
- **Mapper Efficiency**: Streamline Entity ↔ Domain conversion

## Persistence Layer Refactoring Patterns

### 1. Extract QueryDSL Projection Template

**Before**:
```java
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

**After**:
```java
// Extract projection template
private static final QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

private static final Expression<OrderDto> ORDER_DTO_PROJECTION =
    Projections.constructor(
        OrderDto.class,
        order.orderId,
        order.customerId,
        order.productId,
        order.quantity,
        order.status
    );

@Override
public Optional<OrderDomain> loadById(String orderId) {
    OrderDto dto = queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(order.orderId.eq(orderId))
        .fetchOne();

    return Optional.ofNullable(dto)
        .map(OrderEntityMapper::toDomain);
}

@Override
public List<OrderDomain> findByCustomerId(Long customerId) {
    List<OrderDto> dtos = queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(order.customerId.eq(customerId))
        .fetch();

    return dtos.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}
```

### 2. Extract Dynamic Query Builder

**Before**:
```java
@Override
public List<OrderDomain> findOrders(OrderSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    if (condition.getCustomerId() != null) {
        builder.and(order.customerId.eq(condition.getCustomerId()));
    }
    if (condition.getStatus() != null) {
        builder.and(order.status.eq(condition.getStatus()));
    }
    if (condition.getStartDate() != null) {
        builder.and(order.createdAt.goe(condition.getStartDate()));
    }
    if (condition.getEndDate() != null) {
        builder.and(order.createdAt.loe(condition.getEndDate()));
    }

    List<OrderDto> dtos = queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(builder)
        .fetch();

    return dtos.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}
```

**After**:
```java
// Extract query builder
@Override
public List<OrderDomain> findOrders(OrderSearchCondition condition) {
    List<OrderDto> dtos = queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(buildSearchConditions(condition))
        .fetch();

    return dtos.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}

private BooleanBuilder buildSearchConditions(OrderSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    Optional.ofNullable(condition.getCustomerId())
        .ifPresent(id -> builder.and(order.customerId.eq(id)));

    Optional.ofNullable(condition.getStatus())
        .ifPresent(status -> builder.and(order.status.eq(status)));

    Optional.ofNullable(condition.getStartDate())
        .ifPresent(date -> builder.and(order.createdAt.goe(date)));

    Optional.ofNullable(condition.getEndDate())
        .ifPresent(date -> builder.and(order.createdAt.loe(date)));

    return builder;
}
```

### 3. Optimize Batch Insert

**Before**:
```java
@Override
public List<OrderDomain> saveAll(List<OrderDomain> domains) {
    List<OrderJpaEntity> entities = domains.stream()
        .map(OrderEntityMapper::toEntity)
        .toList();

    List<OrderJpaEntity> saved = orderJpaRepository.saveAll(entities);

    return saved.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}
```

**After**:
```java
// Enable batch insert in application.yml
// spring.jpa.properties.hibernate.jdbc.batch_size: 100

@Override
public List<OrderDomain> saveAll(List<OrderDomain> domains) {
    List<OrderJpaEntity> entities = domains.stream()
        .map(OrderEntityMapper::toEntity)
        .toList();

    // JPA batch insert (configured in application.yml)
    List<OrderJpaEntity> saved = orderJpaRepository.saveAll(entities);

    return saved.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();
}
```

### 4. Add Index Hints for Performance

**Before**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "status", nullable = false)
    private OrderStatus status;
}
```

**After**:
```java
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_order_id", columnList = "order_id", unique = true)
    }
)
public class OrderJpaEntity extends BaseAuditEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;
}
```

### 5. Extract Paging/Sorting Logic

**Before**:
```java
@Override
public Page<OrderDomain> findOrdersWithPaging(OrderSearchCondition condition, Pageable pageable) {
    List<OrderDto> content = queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(buildSearchConditions(condition))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(order.createdAt.desc())
        .fetch();

    long total = queryFactory
        .select(order.count())
        .from(order)
        .where(buildSearchConditions(condition))
        .fetchOne();

    List<OrderDomain> domains = content.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();

    return new PageImpl<>(domains, pageable, total);
}
```

**After**:
```java
// Extract paging logic
@Override
public Page<OrderDomain> findOrdersWithPaging(OrderSearchCondition condition, Pageable pageable) {
    JPAQuery<OrderDto> query = createBaseQuery(condition);

    List<OrderDto> content = query
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(getOrderSpecifiers(pageable.getSort()))
        .fetch();

    long total = createCountQuery(condition);

    List<OrderDomain> domains = content.stream()
        .map(OrderEntityMapper::toDomain)
        .toList();

    return new PageImpl<>(domains, pageable, total);
}

private JPAQuery<OrderDto> createBaseQuery(OrderSearchCondition condition) {
    return queryFactory
        .select(ORDER_DTO_PROJECTION)
        .from(order)
        .where(buildSearchConditions(condition));
}

private long createCountQuery(OrderSearchCondition condition) {
    Long count = queryFactory
        .select(order.count())
        .from(order)
        .where(buildSearchConditions(condition))
        .fetchOne();

    return count != null ? count : 0L;
}

private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
    return sort.stream()
        .map(this::toOrderSpecifier)
        .toArray(OrderSpecifier[]::new);
}

private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
    Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

    return switch (sortOrder.getProperty()) {
        case "createdAt" -> new OrderSpecifier<>(direction, order.createdAt);
        case "customerId" -> new OrderSpecifier<>(direction, order.customerId);
        case "status" -> new OrderSpecifier<>(direction, order.status);
        default -> new OrderSpecifier<>(Order.DESC, order.createdAt);
    };
}
```

### 6. Strengthen Mapper with Validation

**Before**:
```java
public static OrderJpaEntity toEntity(OrderDomain domain) {
    return OrderJpaEntity.of(
        domain.getOrderId().getValue(),
        domain.getCustomerId(),
        domain.getProductId(),
        domain.getQuantity(),
        domain.getStatus()
    );
}
```

**After**:
```java
public static OrderJpaEntity toEntity(OrderDomain domain) {
    if (domain == null) {
        throw new IllegalArgumentException("Domain cannot be null");
    }

    return OrderJpaEntity.of(
        domain.getOrderId().getValue(),
        domain.getCustomerId(),
        domain.getProductId(),
        domain.getQuantity(),
        domain.getStatus()
    );
}

public static OrderDomain toDomain(OrderJpaEntity entity) {
    if (entity == null) {
        throw new IllegalArgumentException("Entity cannot be null");
    }

    return OrderDomain.reconstruct(
        new OrderId(entity.getOrderId()),
        entity.getCustomerId(),
        entity.getProductId(),
        entity.getQuantity(),
        entity.getStatus()
    );
}
```

## Refactoring Workflow

### Step 1: Identify Code Smells
- Duplicated QueryDSL projections
- Complex dynamic query conditions
- Missing indexes on frequently queried columns
- Inefficient batch operations
- Missing validation in Mappers

### Step 2: Apply Refactoring
```bash
# 1. Run tests to ensure GREEN
./gradlew test

# 2. Apply ONE refactoring
# (e.g., Extract QueryDSL projection template)

# 3. Run tests again
./gradlew test

# 4. If GREEN, commit
git add .
git commit -m "refactor: extract QueryDSL projection template in LoadOrderQueryAdapter"

# 5. Repeat for next refactoring
```

### Step 3: Verify Zero-Tolerance Compliance

**Check 1: Long FK Strategy**
```bash
# Find JPA relationship annotations
grep -r "@ManyToOne\|@OneToMany\|@OneToOne\|@ManyToMany" persistence/src/main/java/
# Should return empty
```

**Check 2: Lombok in Entity**
```bash
# Find Lombok annotations in Entity classes
grep -r "@Data\|@Builder\|@Getter\|@Setter" persistence/src/main/java/.../entity/
# Should return empty
```

**Check 3: Entity Direct Query**
```bash
# Find selectFrom(entity) patterns
grep -r "selectFrom(" persistence/src/main/java/.../adapter/
# Should use DTO Projection instead
```

## Common Persistence Refactorings

### 1. Extract Repository Custom Interface
```java
// Before: Complex query in Adapter
@Component
@RequiredArgsConstructor
public class LoadOrderQueryAdapter {
    // Complex QueryDSL logic
}

// After: Extract to Custom Repository
public interface OrderRepositoryCustom {
    List<OrderDto> findOrdersWithCondition(OrderSearchCondition condition);
}

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<OrderDto> findOrdersWithCondition(OrderSearchCondition condition) {
        // QueryDSL logic
    }
}
```

### 2. Add Soft Delete Support
```java
// Add to BaseAuditEntity
@MappedSuperclass
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

### 3. Extract Query Result Transformer
```java
// Before: Repeated stream mapping
List<OrderDomain> domains = dtos.stream()
    .map(OrderEntityMapper::toDomain)
    .toList();

// After: Extract transformer
public class QueryResultTransformer {

    public static <T, R> List<R> transform(List<T> source, Function<T, R> mapper) {
        return source.stream()
            .map(mapper)
            .toList();
    }

    public static List<OrderDomain> toOrderDomains(List<OrderDto> dtos) {
        return transform(dtos, OrderEntityMapper::toDomain);
    }
}

// Usage
List<OrderDomain> domains = QueryResultTransformer.toOrderDomains(dtos);
```

## Success Criteria

- ✅ All tests still PASS after refactoring
- ✅ Code is more readable and maintainable
- ✅ QueryDSL projections extracted and reused
- ✅ Dynamic queries organized with builders
- ✅ Indexes added for performance
- ✅ Long FK strategy maintained
- ✅ No Lombok in Entity classes
- ✅ DTO Projection used consistently

## What NOT to Do

- ❌ Don't change behavior (tests should still pass)
- ❌ Don't refactor without tests passing first
- ❌ Don't add JPA relationship annotations
- ❌ Don't use Lombok in Entity
- ❌ Don't query Entity directly
- ❌ Don't over-engineer (YAGNI)

This is Kent Beck's TDD: After tests pass, REFACTOR to improve structure while keeping tests GREEN.
