# Persistence Layer (MySQL) - Hexagonal Architecture

> **목적**: MySQL 기반 영속성 계층 구현 (Ports & Adapters Infrastructure Layer)

---

## 📋 목차

1. [개요](#-개요)
2. [아키텍처 원칙](#-아키텍처-원칙)
3. [디렉토리 구조](#-디렉토리-구조)
4. [핵심 패턴](#-핵심-패턴)
5. [레이어별 가이드](#-레이어별-가이드)
6. [ArchUnit 검증](#-archunit-검증)
7. [테스트 전략](#-테스트-전략)
8. [설정 가이드](#-설정-가이드)

---

## 🎯 개요

### Persistence Layer의 역할

**헥사고날 아키텍처의 Infrastructure Layer (Adapter Out)**:
- **Domain → Infrastructure 의존성 역전**: Domain이 Persistence를 의존하지 않음
- **Port 구현**: Application Layer의 Port 인터페이스 구현
- **데이터 영속화**: Domain 객체를 MySQL에 저장/조회
- **기술 세부사항 캡슐화**: JPA/QueryDSL/Flyway 등 기술 스택 숨김

### 핵심 원칙

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│  ┌──────────────────────────────────────────────────┐   │
│  │     Port Interface (CommandPort, QueryPort)      │   │
│  └────────────────────┬─────────────────────────────┘   │
└───────────────────────┼─────────────────────────────────┘
                        │ 의존성 역전 (Interface)
┌───────────────────────▼─────────────────────────────────┐
│              Persistence Layer (MySQL)                   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Adapter (CommandAdapter, QueryAdapter)          │   │
│  │    ↓                         ↓                   │   │
│  │  Mapper                  Repository               │   │
│  │    ↓                         ↓                   │   │
│  │  Entity ←──────────────→ QueryDSL                │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                        ↓
                ┌───────────────┐
                │  MySQL 8.0+   │
                └───────────────┘
```

**Zero-Tolerance 원칙**:
- ❌ **Lombok 금지**: Plain Java 사용 (특히 Entity)
- ❌ **JPA 관계 어노테이션 금지**: Long FK 전략 사용
- ❌ **Entity 반환 금지**: DTO Projection 필수
- ❌ **Domain 의존 금지**: Repository/Entity/Mapper는 Domain 직접 의존 불가

---

## 🏗️ 아키텍처 원칙

### 1. CQRS 패턴 (Command Query Responsibility Segregation)

**Command (쓰기)**:
```java
@Component
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public OrderId persist(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return new OrderId(saved.getOrderId());
    }
}
```

**Query (읽기)**:
```java
@Component
public class OrderQueryAdapter implements OrderQueryPort {

    private final OrderQueryDslRepository queryDslRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> loadById(OrderId id) {
        return queryDslRepository.findById(id.value())
            .map(mapper::toDomain);
    }
}
```

**Lock Query (비관적 락 읽기)**:
```java
@Component
public class OrderLockQueryAdapter implements OrderLockQueryPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findByIdForUpdate(OrderId id) {
        return jpaRepository.findByIdWithPessimisticWriteLock(id.value())
            .map(mapper::toDomain);
    }
}
```

### 2. Long FK 전략

**❌ 잘못된 방법 (JPA 관계 어노테이션)**:
```java
@Entity
public class OrderJpaEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerJpaEntity customer;  // ❌ 금지!
}
```

**✅ 올바른 방법 (Long FK)**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // ✅ Long FK
}
```

**이유**:
- ✅ **N+1 문제 원천 차단**: 의도하지 않은 지연 로딩 방지
- ✅ **명시적 Join**: QueryDSL에서 필요한 경우만 명시적 Join
- ✅ **단순성**: Entity 간 복잡한 관계 제거

### 3. DTO Projection (Entity 반환 금지)

**❌ 잘못된 방법 (Entity 반환)**:
```java
public class OrderQueryDslRepository {
    public Optional<OrderJpaEntity> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.orderId.eq(id))
                .fetchOne()  // ❌ Entity 반환
        );
    }
}
```

**✅ 올바른 방법 (DTO Projection)**:
```java
public class OrderQueryDslRepository {
    public Optional<OrderDto> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.select(Projections.constructor(
                OrderDto.class,
                qOrder.orderId,
                qOrder.customerId,
                qOrder.status,
                qOrder.totalAmount
            ))
            .from(qOrder)
            .where(qOrder.orderId.eq(id))
            .fetchOne()  // ✅ DTO 반환
        );
    }
}
```

**이유**:
- ✅ **N+1 문제 방지**: 필요한 컬럼만 SELECT
- ✅ **성능 최적화**: 불필요한 데이터 로딩 방지
- ✅ **명시성**: 어떤 데이터를 조회하는지 명확

### 4. Mapper 패턴 (Entity ↔ Domain 변환)

**Mapper 책임**:
```java
@Component
public class OrderMapper {

    // Domain → Entity (저장용)
    public OrderJpaEntity toEntity(Order domain) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setOrderId(domain.getId().value());
        entity.setCustomerId(domain.getCustomerId().value());
        entity.setStatus(domain.getStatus().name());
        entity.setTotalAmount(domain.getTotalAmount().value());
        return entity;
    }

    // DTO → Domain (조회용)
    public Order toDomain(OrderDto dto) {
        return Order.builder()
            .id(new OrderId(dto.orderId()))
            .customerId(new CustomerId(dto.customerId()))
            .status(OrderStatus.valueOf(dto.status()))
            .totalAmount(new Money(dto.totalAmount()))
            .build();
    }
}
```

**규칙**:
- ✅ `@Component` 어노테이션 필수
- ✅ `toEntity(Domain)` 메서드 필수 (Command용)
- ✅ `toDomain(Dto)` 메서드 필수 (Query용)
- ❌ Static 메서드 금지
- ❌ 비즈니스 로직 금지

---

## 📁 디렉토리 구조

```
adapter-out/persistence-mysql/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.ryuqq.adapter.out.persistence/
│   │   │       ├── adapter/
│   │   │       │   ├── command/              # Command Adapter
│   │   │       │   │   └── OrderCommandAdapter.java
│   │   │       │   └── query/                # Query Adapter
│   │   │       │       ├── OrderQueryAdapter.java
│   │   │       │       └── OrderLockQueryAdapter.java
│   │   │       ├── entity/                   # JPA Entity
│   │   │       │   ├── OrderJpaEntity.java
│   │   │       │   └── common/
│   │   │       │       └── BaseAuditEntity.java
│   │   │       ├── repository/               # Repository
│   │   │       │   ├── OrderJpaRepository.java
│   │   │       │   └── OrderQueryDslRepository.java
│   │   │       ├── mapper/                   # Mapper
│   │   │       │   └── OrderMapper.java
│   │   │       ├── dto/                      # DTO (QueryDSL Projection용)
│   │   │       │   └── OrderDto.java
│   │   │       └── config/                   # Configuration
│   │   │           ├── FlywayConfig.java
│   │   │           └── JpaConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/                # Flyway 마이그레이션
│   │               ├── V1__create_order_table.sql
│   │               └── V2__add_order_index.sql
│   └── test/
│       ├── java/
│       │   └── com.ryuqq.adapter.out.persistence/
│       │       ├── adapter/
│       │       │   └── command/              # Adapter 통합 테스트
│       │       │       └── OrderCommandAdapterTest.java
│       │       ├── repository/               # Repository 단위 테스트
│       │       │   └── OrderQueryDslRepositoryTest.java
│       │       └── architecture/             # ArchUnit 테스트
│       │           ├── PersistenceLayerArchTest.java
│       │           ├── DataAccessPatternArchTest.java
│       │           ├── FlywayMigrationArchTest.java
│       │           └── HikariCPConfigArchTest.java
│       └── resources/
│           └── application-test.yml
```

---

## 🔧 핵심 패턴

### 1. Command Adapter 패턴

**책임**: Domain 객체를 MySQL에 저장/수정/삭제

**구조**:
```java
@Component
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    public OrderCommandAdapter(
        OrderJpaRepository jpaRepository,
        OrderMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OrderId persist(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return new OrderId(saved.getOrderId());
    }
}
```

**규칙**:
- ✅ `*CommandAdapter` 네이밍
- ✅ `*CommandPort` 구현
- ✅ `JpaRepository + Mapper` 의존
- ✅ `persist()` 메서드 (1개 파라미터, *Id 반환)
- ❌ `@Transactional` 금지 (Service Layer에서 관리)
- ❌ Query 메서드 금지
- ❌ Domain 직접 의존 금지

### 2. Query Adapter 패턴

**책임**: MySQL에서 데이터 조회 → Domain 변환

**구조**:
```java
@Component
public class OrderQueryAdapter implements OrderQueryPort {

    private final OrderQueryDslRepository queryDslRepository;
    private final OrderMapper mapper;

    public OrderQueryAdapter(
        OrderQueryDslRepository queryDslRepository,
        OrderMapper mapper
    ) {
        this.queryDslRepository = queryDslRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> loadById(OrderId id) {
        return queryDslRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public List<Order> loadByCriteria(OrderSearchCriteria criteria) {
        return queryDslRepository.findByCriteria(criteria).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
```

**규칙**:
- ✅ `*QueryAdapter` 네이밍
- ✅ `*QueryPort` 구현
- ✅ `QueryDslRepository + Mapper` 의존
- ✅ `load*()` 메서드 네이밍
- ❌ `@Transactional` 금지
- ❌ 저장/수정/삭제 금지
- ❌ Domain 직접 의존 금지

### 3. Lock Query Adapter 패턴

**책임**: 비관적 락을 사용한 조회

**구조**:
```java
@Component
public class OrderLockQueryAdapter implements OrderLockQueryPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    public OrderLockQueryAdapter(
        OrderJpaRepository jpaRepository,
        OrderMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findByIdForUpdate(OrderId id) {
        return jpaRepository.findByIdWithPessimisticWriteLock(id.value())
            .map(mapper::toDomain);
    }
}
```

**규칙**:
- ✅ `*LockQueryAdapter` 네이밍
- ✅ `*LockQueryPort` 구현
- ✅ `JpaRepository + Mapper` 의존
- ✅ `ForUpdate`, `ForShare`, `WithLock` 네이밍
- ✅ `Optional<Domain>` 반환 (단건만)
- ❌ `List` 반환 금지 (성능 이슈)
- ❌ `@Transactional` 사용 금지 (Service Layer에서 관리)

### 4. QueryDSL Repository 패턴

**책임**: Type-safe 쿼리 빌딩 + DTO Projection

**구조**:
```java
@Repository
public class OrderQueryDslRepository {

    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;

    private final JPAQueryFactory queryFactory;

    public OrderQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    // 표준 메서드 1: 단건 조회
    public Optional<OrderDto> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.select(orderProjection())
                .from(qOrder)
                .where(qOrder.orderId.eq(id))
                .fetchOne()
        );
    }

    // 표준 메서드 2: 존재 여부
    public boolean existsById(Long id) {
        return queryFactory.selectOne()
            .from(qOrder)
            .where(qOrder.orderId.eq(id))
            .fetchFirst() != null;
    }

    // 표준 메서드 3: 조건 조회
    public List<OrderDto> findByCriteria(OrderSearchCriteria criteria) {
        return queryFactory.select(orderProjection())
            .from(qOrder)
            .where(buildPredicates(criteria))
            .fetch();
    }

    // 표준 메서드 4: 조건 개수
    public long countByCriteria(OrderSearchCriteria criteria) {
        return queryFactory.select(qOrder.count())
            .from(qOrder)
            .where(buildPredicates(criteria))
            .fetchOne();
    }

    private ConstructorExpression<OrderDto> orderProjection() {
        return Projections.constructor(
            OrderDto.class,
            qOrder.orderId,
            qOrder.customerId,
            qOrder.status,
            qOrder.totalAmount
        );
    }
}
```

**규칙**:
- ✅ `*QueryDslRepository` 네이밍
- ✅ `@Repository` 어노테이션
- ✅ `JPAQueryFactory` 필드 (private final)
- ✅ `QType` static final 필드
- ✅ **정확히 4개 표준 메서드**: `findById`, `existsById`, `findByCriteria`, `countByCriteria`
- ✅ DTO Projection 사용 (`Projections.constructor()`)
- ❌ Entity 반환 금지
- ❌ `@Transactional` 금지
- ❌ Mapper 의존 금지

### 5. JPA Repository 패턴

**책임**: 단순 CRUD (Command Adapter 전용)

**구조**:
```java
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    // 비관적 락 조회 (LockQueryAdapter 전용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.orderId = :id")
    Optional<OrderJpaEntity> findByIdWithPessimisticWriteLock(@Param("id") Long id);
}
```

**규칙**:
- ✅ `*Repository` 네이밍 (JpaRepository 상속)
- ✅ Interface 선언
- ✅ `JpaRepository<Entity, Long>` 상속
- ❌ Query Method 추가 금지
- ❌ `@Query` 사용 금지 (Lock 제외)
- ❌ Custom Repository 구현 금지
- ❌ `QuerydslPredicateExecutor` 상속 금지

---

## 📚 레이어별 가이드

### 1. Adapter Layer

**Command Adapter**:
- [Command Adapter 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/command/command-adapter-guide.md)
- [Command Adapter 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/command/command-adapter-test-guide.md)
- [Command Adapter ArchUnit](docs/coding_convention/04-persistence-layer/mysql/adapter/command/command-adapter-archunit.md)

**Query Adapter**:
- [Query Adapter 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/query/query-adapter-guide.md)
- [Query Adapter 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/query/query-adapter-test-guide.md)
- [Query Adapter 통합 테스트](docs/coding_convention/04-persistence-layer/mysql/adapter/query/query-adapter-integration-testing.md)
- [Query Adapter ArchUnit](docs/coding_convention/04-persistence-layer/mysql/adapter/query/query-adapter-archunit.md)

**Lock Query Adapter**:
- [Lock Query Adapter 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/query/lock-query-adapter-guide.md)
- [Lock Query Adapter 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/adapter/query/lock-query-adapter-test-guide.md)
- [Lock Query Adapter ArchUnit](docs/coding_convention/04-persistence-layer/mysql/adapter/query/lock-query-adapter-archunit.md)

### 2. Entity Layer

**JPA Entity**:
- [Entity 가이드](docs/coding_convention/04-persistence-layer/mysql/entity/entity-guide.md)
- [Entity 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/entity/entity-test-guide.md)
- [Entity ArchUnit](docs/coding_convention/04-persistence-layer/mysql/entity/entity-archunit.md)

**핵심 규칙**:
- ✅ `*JpaEntity` 네이밍
- ✅ `@Entity`, `@Table` 어노테이션
- ✅ Plain Java (Lombok 금지)
- ✅ Long FK 전략
- ❌ JPA 관계 어노테이션 금지 (`@OneToMany`, `@ManyToOne`, `@OneToOne`)
- ❌ Domain 의존 금지

### 3. Repository Layer

**JPA Repository**:
- [JPA Repository 가이드](docs/coding_convention/04-persistence-layer/mysql/repository/jpa-repository-guide.md)
- [JPA Repository ArchUnit](docs/coding_convention/04-persistence-layer/mysql/repository/jpa-repository-archunit.md)

**QueryDSL Repository**:
- [QueryDSL Repository 가이드](docs/coding_convention/04-persistence-layer/mysql/repository/querydsl-repository-guide.md)
- [QueryDSL Repository 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/repository/querydsl-repository-test-guide.md)
- [QueryDSL Repository ArchUnit](docs/coding_convention/04-persistence-layer/mysql/repository/querydsl-repository-archunit.md)

### 4. Mapper Layer

**Mapper**:
- [Mapper 가이드](docs/coding_convention/04-persistence-layer/mysql/mapper/mapper-guide.md)
- [Mapper 테스트 가이드](docs/coding_convention/04-persistence-layer/mysql/mapper/mapper-test-guide.md)
- [Mapper ArchUnit](docs/coding_convention/04-persistence-layer/mysql/mapper/mapper-archunit.md)

**핵심 규칙**:
- ✅ `@Component` 어노테이션
- ✅ `toEntity(Domain)` 메서드
- ✅ `toDomain(Dto)` 메서드
- ❌ Static 메서드 금지
- ❌ 비즈니스 로직 금지

### 5. Config Layer

**Flyway**:
- [Flyway 테스팅 가이드](docs/coding_convention/04-persistence-layer/mysql/config/flyway-testing-guide.md)

**HikariCP**:
- [HikariCP 설정 가이드](docs/coding_convention/04-persistence-layer/mysql/config/hikaricp-configuration.md)

---

## ✅ ArchUnit 검증

### 1. PersistenceLayerArchTest (14개 규칙)

**검증 항목**:
- Package 구조 (adapter, entity, repository, mapper)
- Port 구현 (CommandPort, QueryPort, LockQueryPort)
- JPA Entity-Domain 분리
- Layer 의존성 (단방향)
- Application Layer 의존 금지
- Domain Layer 의존 금지 (Port 통해서만)
- Adapter 네이밍 규칙
- Repository 네이밍 규칙

**실행**:
```bash
./gradlew :adapter-out:persistence-mysql:test --tests "PersistenceLayerArchTest"
```

### 2. DataAccessPatternArchTest (12개 규칙)

**검증 항목**:
- QueryDslRepository JPAQueryFactory 필드
- QueryDslRepository QType static final 필드
- QueryAdapter QueryDslRepository 의존 (CQRS)
- CommandAdapter JpaRepository 의존 (CQRS)
- QueryDslRepository DTO Projection
- Repository Domain 반환 금지
- Test Fixtures 패턴
- Adapter Mapper 의존
- QueryDslRepository 표준 메서드
- Adapter JPAQueryFactory 직접 사용 금지

**실행**:
```bash
./gradlew :adapter-out:persistence-mysql:test --tests "DataAccessPatternArchTest"
```

### 3. FlywayMigrationArchTest (8개 규칙)

**검증 항목**:
- FlywayConfig @Configuration
- Config 패키지 위치
- Public/Final 검증
- Entity/Repository 의존 금지
- Domain/Application Layer 의존 금지

**실행**:
```bash
./gradlew :adapter-out:persistence-mysql:test --tests "FlywayMigrationArchTest"
```

### 4. HikariCPConfigArchTest (10개 규칙)

**검증 항목**:
- DataSourceConfig @Configuration
- Config 패키지 위치
- Public/Final 검증
- Entity/Repository 의존 금지
- Domain/Application Layer 의존 금지
- JPA Config 분리
- Adapter/Mapper 의존 금지
- 네이밍 규칙

**실행**:
```bash
./gradlew :adapter-out:persistence-mysql:test --tests "HikariCPConfigArchTest"
```

### 전체 ArchUnit 실행

```bash
# 추가된 4개 ArchUnit 테스트 실행 (44개 규칙)
./gradlew :adapter-out:persistence-mysql:test \
  --tests "PersistenceLayerArchTest" \
  --tests "DataAccessPatternArchTest" \
  --tests "FlywayMigrationArchTest" \
  --tests "HikariCPConfigArchTest"
```

---

## 🧪 테스트 전략

### 1. 단위 테스트 (Mapper, QueryDslRepository)

**Mapper 테스트**:
```java
@ExtendWith(MockitoExtension.class)
class OrderMapperTest {

    private OrderMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
    }

    @Test
    @DisplayName("Domain → Entity 변환 성공")
    void toEntity_Success() {
        // Given
        Order domain = OrderFixture.default();

        // When
        OrderJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getOrderId()).isEqualTo(domain.getId().value());
        assertThat(entity.getCustomerId()).isEqualTo(domain.getCustomerId().value());
    }
}
```

**QueryDslRepository 테스트**:
```java
@DataJpaTest
@Import({QueryDslConfig.class, OrderQueryDslRepository.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderQueryDslRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private OrderQueryDslRepository repository;

    @Test
    @DisplayName("ID로 주문 조회 성공")
    void findById_Success() {
        // Given
        Long orderId = 1L;

        // When
        Optional<OrderDto> result = repository.findById(orderId);

        // Then
        assertThat(result).isPresent();
    }
}
```

### 2. 통합 테스트 (Adapter)

**Command Adapter 통합 테스트**:
```java
@DataJpaTest
@Import({OrderCommandAdapter.class, OrderMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderCommandAdapterTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private OrderCommandAdapter adapter;

    @Test
    @DisplayName("Order 저장 성공")
    void persist_Success() {
        // Given
        Order order = OrderFixture.default();

        // When
        OrderId savedId = adapter.persist(order);

        // Then
        assertThat(savedId).isNotNull();
    }
}
```

**Query Adapter 통합 테스트**:
```java
@DataJpaTest
@Import({
    QueryDslConfig.class,
    OrderQueryAdapter.class,
    OrderQueryDslRepository.class,
    OrderMapper.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderQueryAdapterTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private OrderQueryAdapter adapter;

    @Test
    @DisplayName("ID로 Order 조회 성공")
    void loadById_Success() {
        // Given
        OrderId orderId = new OrderId(1L);

        // When
        Optional<Order> result = adapter.loadById(orderId);

        // Then
        assertThat(result).isPresent();
    }
}
```

### 3. ArchUnit 테스트

**자동 실행**:
```bash
# Gradle 빌드 시 자동 실행
./gradlew :adapter-out:persistence-mysql:test

# 특정 ArchUnit 테스트만 실행
./gradlew :adapter-out:persistence-mysql:test --tests "*ArchTest"
```

---

## ⚙️ 설정 가이드

### 1. Flyway 마이그레이션

**디렉토리 구조**:
```
src/main/resources/db/migration/
├── V1__create_order_table.sql
├── V2__create_customer_table.sql
└── V3__add_order_index.sql
```

**네이밍 규칙**:
- `V{버전}__{설명}.sql`
- V 대문자, 언더스코어 2개
- 설명은 snake_case

**예시 (V1__create_order_table.sql)**:
```sql
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. HikariCP Connection Pool

**application-prod.yml**:
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: HikariPool-Prod

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway 사용 시 validate
    open-in-view: false   # OSIV 비활성화 (필수!)

  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 3. QueryDSL 설정

**build.gradle**:
```gradle
dependencies {
    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
}
```

**QueryDslConfig.java**:
```java
@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
```

### 4. TestContainers 설정

**application-test.yml**:
```yaml
spring:
  datasource:
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  flyway:
    enabled: true
    clean-disabled: false

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

**테스트 클래스**:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MyRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    // 테스트 코드
}
```

---

## 📋 체크리스트

### Adapter 구현 시

- [ ] `*CommandAdapter` 또는 `*QueryAdapter` 네이밍
- [ ] `*CommandPort` 또는 `*QueryPort` 구현
- [ ] `@Component` 어노테이션
- [ ] 생성자 주입 (JpaRepository/QueryDslRepository + Mapper)
- [ ] 모든 필드 `private final`
- [ ] `@Transactional` 금지
- [ ] Domain 직접 의존 금지

### QueryDslRepository 구현 시

- [ ] `*QueryDslRepository` 네이밍
- [ ] `@Repository` 어노테이션
- [ ] `JPAQueryFactory` 필드 (private final)
- [ ] `QType` static final 필드
- [ ] 4개 표준 메서드만 사용
- [ ] DTO Projection 사용
- [ ] Entity 반환 금지
- [ ] `@Transactional` 금지

### JPA Entity 구현 시

- [ ] `*JpaEntity` 네이밍
- [ ] `@Entity`, `@Table` 어노테이션
- [ ] Plain Java (Lombok 금지)
- [ ] Long FK 전략
- [ ] JPA 관계 어노테이션 금지
- [ ] Domain 의존 금지

### Mapper 구현 시

- [ ] `*Mapper` 네이밍
- [ ] `@Component` 어노테이션
- [ ] `toEntity(Domain)` 메서드
- [ ] `toDomain(Dto)` 메서드
- [ ] Static 메서드 금지
- [ ] 비즈니스 로직 금지

### Flyway 마이그레이션 파일 작성 시

- [ ] `V{버전}__{설명}.sql` 네이밍
- [ ] V 대문자, 언더스코어 2개
- [ ] snake_case 설명
- [ ] `CREATE TABLE IF NOT EXISTS`
- [ ] ENGINE, CHARSET, COLLATE 지정
- [ ] 인덱스 네이밍 (idx_, uk_)

---

## 📚 참고 문서

### 내부 문서
- [Persistence Layer 가이드](docs/coding_convention/04-persistence-layer/mysql/persistence-mysql-guide.md)
- [Domain Layer README](../domain/README.md)
- [Application Layer README](../application/README.md)

### 외부 문서
- [Spring Data JPA 공식 문서](https://docs.spring.io/spring-data/jpa/reference/)
- [QueryDSL 공식 문서](http://querydsl.com/)
- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [Flyway 공식 문서](https://flywaydb.org/documentation/)
- [TestContainers 공식 문서](https://www.testcontainers.org/)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-23
**버전**: 1.0.0
