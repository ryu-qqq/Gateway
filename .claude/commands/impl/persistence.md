---
description: Persistence Layer Doc-Driven 구현. Entity, Repository, Mapper, Adapter 생성. 구현 + 테스트 동시 작성.
tags: [project]
---

# /impl persistence - Persistence Layer Implementation

**Doc-Driven Development**로 Persistence Layer 신규 코드를 생성합니다.

## 사용법

```bash
/impl persistence {feature-name}
/impl persistence order-cancel
/impl persistence member-register
```

## 실행 프로세스

```
/impl persistence cancel-order
        ↓
┌─────────────────────────────────────────────────┐
│ 1️⃣ Plan 로드 (Serena memory)                    │
│    - read_memory("plan-{feature}")              │
│    - 엔티티 설계 확인                             │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 2️⃣ Persistence Skill 활성화                     │
│    - entity-mapper-expert, repository-expert    │
│    - Zero-Tolerance 검증                        │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 3️⃣ 구현 + 테스트 동시 작성                       │
│    - JPA Entity                                │
│    - JPA Repository                            │
│    - QueryDSL Repository                       │
│    - Mapper (Entity ↔ Domain)                  │
│    - Adapter (Port Out 구현)                   │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 4️⃣ 검증 및 커밋                                 │
│    - ./gradlew test                            │
│    - feat: 커밋 (테스트 포함)                    │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 5️⃣ Memory 업데이트                              │
│    - Plan 진행상태 업데이트                       │
└─────────────────────────────────────────────────┘
```

---

## Zero-Tolerance 규칙 (필수)

### ✅ MUST
- **Long FK 전략**: JPA 관계 어노테이션 금지
- **Lombok 금지**: Entity도 Pure Java
- **QueryDSL DTO Projection**: N+1 방지
- **Mapper 분리**: Entity ↔ Domain 변환 로직 분리

### ❌ NEVER
```java
// ❌ JPA 관계 어노테이션
@ManyToOne
private CustomerEntity customer;

// ❌ Lombok
@Entity
@Data
public class OrderEntity { }

// ❌ Lazy Loading (N+1 원인)
@OneToMany(fetch = FetchType.LAZY)
private List<OrderItemEntity> items;
```

---

## 생성 대상

### 1. JPA Entity

```java
// adapter-out/persistence-mysql/src/main/java/{basePackage}/adapter/out/persistence/{feature}/entity/
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // Long FK (관계 어노테이션 금지!)

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기본 생성자 (JPA 용)
    protected OrderEntity() {}

    // 정적 팩토리 메서드
    public static OrderEntity of(
            String orderId,
            Long customerId,
            OrderStatus status,
            BigDecimal totalPrice) {
        OrderEntity entity = new OrderEntity();
        entity.orderId = orderId;
        entity.customerId = customerId;
        entity.status = status;
        entity.totalPrice = totalPrice;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    // 비즈니스 메서드
    public void updateStatus(OrderStatus status, String reason) {
        this.status = status;
        this.cancelReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Getter (필요한 것만)
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

### 2. JPA Repository

```java
// adapter-out/persistence-mysql/src/main/java/{basePackage}/adapter/out/persistence/{feature}/repository/
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<OrderEntity> findByStatus(OrderStatus status);
}
```

### 3. QueryDSL Repository

```java
// adapter-out/persistence-mysql/src/main/java/{basePackage}/adapter/out/persistence/{feature}/repository/
@Repository
public class OrderQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public OrderQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * 고객별 주문 목록 조회 (DTO Projection)
     */
    public List<OrderSummaryDto> findOrderSummaryByCustomerId(Long customerId) {
        QOrderEntity order = QOrderEntity.orderEntity;

        return queryFactory
            .select(Projections.constructor(OrderSummaryDto.class,
                order.orderId,
                order.status,
                order.totalPrice,
                order.createdAt
            ))
            .from(order)
            .where(order.customerId.eq(customerId))
            .orderBy(order.createdAt.desc())
            .fetch();
    }

    /**
     * 상태별 주문 페이징 조회
     */
    public Page<OrderEntity> findByStatusWithPaging(
            OrderStatus status,
            Pageable pageable) {
        QOrderEntity order = QOrderEntity.orderEntity;

        List<OrderEntity> content = queryFactory
            .selectFrom(order)
            .where(order.status.eq(status))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(order.createdAt.desc())
            .fetch();

        Long total = queryFactory
            .select(order.count())
            .from(order)
            .where(order.status.eq(status))
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
```

### 4. Mapper

```java
// adapter-out/persistence-mysql/src/main/java/{basePackage}/adapter/out/persistence/{feature}/mapper/
@Component
public class OrderPersistenceMapper {

    /**
     * Entity → Domain
     */
    public Order toDomain(OrderEntity entity) {
        return Order.reconstitute(
            OrderId.of(entity.getOrderId()),
            entity.getCustomerId(),
            entity.getStatus(),
            entity.getTotalPrice(),
            entity.getCancelReason() != null
                ? CancelReason.of(entity.getCancelReason())
                : null
        );
    }

    /**
     * Domain → Entity (신규 생성)
     */
    public OrderEntity toEntity(Order domain) {
        return OrderEntity.of(
            domain.getId().value(),
            domain.getCustomerId(),
            domain.getStatus(),
            domain.getTotalPrice()
        );
    }

    /**
     * Domain → Entity (업데이트)
     */
    public void updateEntity(OrderEntity entity, Order domain) {
        entity.updateStatus(
            domain.getStatus(),
            domain.getCancelReason() != null
                ? domain.getCancelReason().value()
                : null
        );
    }
}
```

### 5. Adapter (Port Out 구현)

```java
// adapter-out/persistence-mysql/src/main/java/{basePackage}/adapter/out/persistence/{feature}/adapter/
@Repository
public class OrderPersistenceAdapter implements OrderQueryPort, OrderPersistencePort {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderQueryDslRepository orderQueryDslRepository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(
            OrderJpaRepository orderJpaRepository,
            OrderQueryDslRepository orderQueryDslRepository,
            OrderPersistenceMapper mapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderQueryDslRepository = orderQueryDslRepository;
        this.mapper = mapper;
    }

    // === Query Port ===

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findById(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        return orderJpaRepository.findByOrderId(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        return orderJpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    // === Persistence Port ===

    @Override
    public Order save(Order order) {
        OrderEntity entity = orderJpaRepository.findByOrderId(order.getId().value())
            .map(existing -> {
                mapper.updateEntity(existing, order);
                return existing;
            })
            .orElseGet(() -> mapper.toEntity(order));

        OrderEntity saved = orderJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(Order order) {
        orderJpaRepository.findByOrderId(order.getId().value())
            .ifPresent(orderJpaRepository::delete);
    }
}
```

---

## 테스트 작성

### Repository Integration Test

```java
// adapter-out/persistence-mysql/src/test/java/{basePackage}/adapter/out/persistence/{feature}/
@DataJpaTest
@Import({OrderQueryDslRepository.class, TestJpaConfig.class})
class OrderQueryDslRepositoryTest {

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderQueryDslRepository sut;

    @Test
    @DisplayName("고객별 주문 요약 조회")
    void shouldFindOrderSummaryByCustomerId() {
        // Given
        Long customerId = 1L;
        orderJpaRepository.save(OrderEntityFixture.create(customerId));
        orderJpaRepository.save(OrderEntityFixture.create(customerId));

        // When
        List<OrderSummaryDto> result = sut.findOrderSummaryByCustomerId(customerId);

        // Then
        assertThat(result).hasSize(2);
    }
}
```

### Adapter Integration Test

```java
// adapter-out/persistence-mysql/src/test/java/{basePackage}/adapter/out/persistence/{feature}/
@SpringBootTest
@Transactional
class OrderPersistenceAdapterTest {

    @Autowired
    private OrderPersistenceAdapter sut;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Test
    @DisplayName("주문 저장 및 조회")
    void shouldSaveAndFindOrder() {
        // Given
        Order order = OrderFixture.create();

        // When
        Order saved = sut.save(order);

        // Then
        Optional<Order> found = sut.findByOrderId(saved.getId().value());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }
}
```

---

## 커밋 규칙

```bash
# 구현 + 테스트 함께 커밋
git commit -m "feat: 주문 취소 Persistence 구현

- OrderEntity 수정 (cancelReason 필드)
- OrderPersistenceAdapter 구현
- OrderPersistenceMapper 추가
- Integration 테스트 추가"
```

---

## Memory 업데이트

구현 완료 후 Plan 상태 업데이트:

```python
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle="- [ ] Persistence Layer",
    repl="- [x] Persistence Layer (completed)",
    mode="literal"
)
```

---

## 다음 단계

Persistence Layer 완료 후:

```bash
/impl rest-api {feature}  # REST API Layer 구현
```
