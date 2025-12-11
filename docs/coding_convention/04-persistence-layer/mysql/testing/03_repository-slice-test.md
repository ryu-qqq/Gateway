# Repository Slice 테스트 가이드

> **목적**: @DataJpaTest 기반 빠른 JPA 레이어 테스트 작성 규칙

---

## 1. 개요

### Slice 테스트란?

```
┌─────────────────────────────────────────────────────────────────┐
│  @DataJpaTest Slice 테스트                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  로드되는 Bean:                                                  │
│  ✅ JPA Repository                                               │
│  ✅ EntityManager                                                 │
│  ✅ DataSource                                                    │
│  ✅ QueryDSL JPAQueryFactory (설정 필요)                          │
│  ✅ TestEntityManager                                             │
│                                                                  │
│  로드되지 않는 Bean:                                              │
│  ❌ @Service                                                      │
│  ❌ @Controller                                                   │
│  ❌ @Component (non-JPA)                                          │
│  ❌ Security Configuration                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 언제 사용하는가?

| 상황 | Slice 테스트 | 통합 테스트 |
|------|-------------|------------|
| 간단한 CRUD 검증 | ✅ 적합 | 🔶 과도함 |
| MySQL 전용 기능 | ❌ 부적합 | ✅ 필수 |
| CI/CD 빠른 피드백 | ✅ 적합 | 🔶 느림 |
| Flyway 마이그레이션 검증 | ❌ 불가 | ✅ 필수 |
| QueryDSL 복잡 쿼리 | 🔶 가능 | ✅ 권장 |

---

## 2. 테스트 지원 클래스

### 2.1 JpaSliceTestSupport (기반 클래스)

```java
/**
 * JPA Slice 테스트 지원 추상 클래스
 *
 * <p>@DataJpaTest 기반의 빠른 JPA 테스트를 위한 기반 클래스입니다.
 *
 * <p>제공 기능:
 * <ul>
 *   <li>H2 인메모리 DB 자동 설정</li>
 *   <li>TestEntityManager 자동 주입</li>
 *   <li>트랜잭션 자동 롤백</li>
 *   <li>테스트 유틸리티 메서드</li>
 * </ul>
 *
 * <p><strong>주의:</strong> MySQL 전용 기능 테스트에는 사용하지 마세요.
 * TestContainers 기반 통합 테스트를 사용하세요.
 *
 * @see RepositoryTestSupport MySQL 통합 테스트용
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslTestConfig.class)
public abstract class JpaSliceTestSupport {

    @Autowired
    protected TestEntityManager testEntityManager;

    /**
     * 엔티티 영속화 후 플러시
     *
     * @param entity 영속화할 엔티티
     * @param <T> 엔티티 타입
     * @return 영속화된 엔티티
     */
    protected <T> T persistAndFlush(T entity) {
        return testEntityManager.persistAndFlush(entity);
    }

    /**
     * 영속성 컨텍스트 플러시 및 클리어
     */
    protected void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    /**
     * ID로 엔티티 조회
     *
     * @param entityClass 엔티티 클래스
     * @param id 엔티티 ID
     * @param <T> 엔티티 타입
     * @return 조회된 엔티티 (없으면 null)
     */
    protected <T> T find(Class<T> entityClass, Object id) {
        return testEntityManager.find(entityClass, id);
    }

    /**
     * 여러 엔티티 영속화
     *
     * @param entities 영속화할 엔티티 목록
     */
    protected void persistAll(Object... entities) {
        for (Object entity : entities) {
            testEntityManager.persist(entity);
        }
        testEntityManager.flush();
    }
}
```

### 2.2 QueryDSL 설정

```java
/**
 * QueryDSL 테스트 설정
 *
 * <p>@DataJpaTest에서 QueryDSL을 사용하기 위한 설정입니다.
 */
@TestConfiguration
public class QueryDslTestConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

### 2.3 TestContainers MySQL 연동 (선택)

H2 대신 TestContainers MySQL을 사용하려면:

```java
@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslTestConfig.class)
public abstract class JpaSliceTestSupportWithMySQL {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    protected TestEntityManager testEntityManager;

    // ... 나머지 메서드 동일
}
```

---

## 3. JpaRepository 테스트

### 3.1 기본 CRUD 테스트

```java
@DisplayName("OrderJpaRepository Slice 테스트")
class OrderJpaRepositorySliceTest extends JpaSliceTestSupport {

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("성공 - 주문 저장")
        void success() {
            // Given
            OrderJpaEntity order = OrderJpaEntity.create(
                1L,
                OrderStatus.PENDING,
                Money.of(10000)
            );

            // When
            OrderJpaEntity saved = orderJpaRepository.save(order);
            flushAndClear();

            // Then
            OrderJpaEntity found = find(OrderJpaEntity.class, saved.getId());
            assertThat(found).isNotNull();
            assertThat(found.getCustomerId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("findAllByCustomerId()")
    class FindAllByCustomerId {

        @Test
        @DisplayName("성공 - 고객별 주문 조회")
        void success() {
            // Given
            Long customerId = 1L;
            persistAll(
                OrderJpaEntity.create(customerId, OrderStatus.PENDING, Money.of(10000)),
                OrderJpaEntity.create(customerId, OrderStatus.CONFIRMED, Money.of(20000)),
                OrderJpaEntity.create(2L, OrderStatus.PENDING, Money.of(30000))
            );
            flushAndClear();

            // When
            List<OrderJpaEntity> orders = orderJpaRepository.findAllByCustomerId(customerId);

            // Then
            assertThat(orders).hasSize(2);
            assertThat(orders).allMatch(o -> o.getCustomerId().equals(customerId));
        }
    }

    @Nested
    @DisplayName("existsByCustomerIdAndStatus()")
    class ExistsByCustomerIdAndStatus {

        @Test
        @DisplayName("성공 - 조건에 맞는 주문 존재")
        void exists() {
            // Given
            Long customerId = 1L;
            OrderStatus status = OrderStatus.PENDING;
            persistAndFlush(OrderJpaEntity.create(customerId, status, Money.of(10000)));

            // When
            boolean exists = orderJpaRepository.existsByCustomerIdAndStatus(customerId, status);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("실패 - 조건에 맞는 주문 없음")
        void notExists() {
            // Given
            Long customerId = 999L;
            OrderStatus status = OrderStatus.PENDING;

            // When
            boolean exists = orderJpaRepository.existsByCustomerIdAndStatus(customerId, status);

            // Then
            assertThat(exists).isFalse();
        }
    }
}
```

### 3.2 페이징 테스트

```java
@Nested
@DisplayName("findAllByStatus() - 페이징")
class FindAllByStatusWithPaging {

    @Test
    @DisplayName("성공 - 첫 번째 페이지 조회")
    void firstPage() {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        for (int i = 0; i < 25; i++) {
            testEntityManager.persist(
                OrderJpaEntity.create(1L, status, Money.of(10000 + i))
            );
        }
        flushAndClear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        // When
        Slice<OrderJpaEntity> result = orderJpaRepository.findAllByStatus(status, pageable);

        // Then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("성공 - 마지막 페이지 조회")
    void lastPage() {
        // Given
        OrderStatus status = OrderStatus.PENDING;
        for (int i = 0; i < 25; i++) {
            testEntityManager.persist(
                OrderJpaEntity.create(1L, status, Money.of(10000 + i))
            );
        }
        flushAndClear();

        Pageable pageable = PageRequest.of(2, 10);

        // When
        Slice<OrderJpaEntity> result = orderJpaRepository.findAllByStatus(status, pageable);

        // Then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }
}
```

---

## 4. QueryDslRepository Slice 테스트

### 4.1 동적 쿼리 테스트

```java
@DisplayName("OrderQueryDslRepository Slice 테스트")
class OrderQueryDslRepositorySliceTest extends JpaSliceTestSupport {

    @Autowired
    private OrderQueryDslRepository orderQueryDslRepository;

    @Nested
    @DisplayName("findWithDynamicCondition()")
    class FindWithDynamicCondition {

        @BeforeEach
        void setUp() {
            // 테스트 데이터 준비
            persistAll(
                OrderJpaEntity.create(1L, OrderStatus.PENDING, Money.of(5000)),
                OrderJpaEntity.create(1L, OrderStatus.CONFIRMED, Money.of(15000)),
                OrderJpaEntity.create(2L, OrderStatus.PENDING, Money.of(25000)),
                OrderJpaEntity.create(2L, OrderStatus.SHIPPED, Money.of(35000))
            );
            flushAndClear();
        }

        @Test
        @DisplayName("성공 - 전체 조건 적용")
        void allConditions() {
            // Given
            OrderSearchCondition condition = new OrderSearchCondition(
                1L,                    // customerId
                OrderStatus.PENDING,   // status
                Money.of(1000),       // minAmount
                Money.of(10000)       // maxAmount
            );

            // When
            List<OrderSummaryDto> result = orderQueryDslRepository
                .findWithDynamicCondition(condition);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).customerId()).isEqualTo(1L);
            assertThat(result.get(0).status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("성공 - 일부 조건만 적용")
        void partialConditions() {
            // Given
            OrderSearchCondition condition = new OrderSearchCondition(
                null,                  // customerId 없음
                OrderStatus.PENDING,   // status만
                null,                  // minAmount 없음
                null                   // maxAmount 없음
            );

            // When
            List<OrderSummaryDto> result = orderQueryDslRepository
                .findWithDynamicCondition(condition);

            // Then
            assertThat(result).hasSize(2);  // PENDING 상태인 주문 2개
        }

        @Test
        @DisplayName("성공 - 조건 없이 전체 조회")
        void noConditions() {
            // Given
            OrderSearchCondition condition = OrderSearchCondition.empty();

            // When
            List<OrderSummaryDto> result = orderQueryDslRepository
                .findWithDynamicCondition(condition);

            // Then
            assertThat(result).hasSize(4);  // 전체 4개
        }
    }
}
```

---

## 5. H2 vs MySQL 호환성 주의사항

### 5.1 H2에서 지원되지 않는 MySQL 기능

| MySQL 기능 | H2 대안 | 권장 |
|-----------|--------|------|
| `ON DUPLICATE KEY UPDATE` | MERGE INTO | TestContainers |
| JSON 타입/함수 | 미지원 | TestContainers |
| `REGEXP` 연산자 | 미지원 | TestContainers |
| `DATE_FORMAT()` | `FORMATDATETIME()` | TestContainers |
| 특정 인덱스 힌트 | 무시됨 | 통합 테스트 |

### 5.2 H2 MySQL 모드 설정

```yaml
# application-test.yml (H2 MySQL 호환 모드)
spring:
  datasource:
    url: jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

---

## 6. 체크리스트

### Slice 테스트 작성 전

- [ ] `JpaSliceTestSupport` 상속
- [ ] `@DataJpaTest` 어노테이션 확인
- [ ] QueryDSL 사용 시 `QueryDslTestConfig` Import
- [ ] MySQL 전용 기능 사용 여부 확인 → 사용 시 통합 테스트로

### 테스트 메서드 작성

- [ ] `@DisplayName` 작성
- [ ] TestEntityManager로 데이터 준비
- [ ] Given-When-Then 구조 준수
- [ ] `flushAndClear()` 적절히 사용

### 주의 사항

- [ ] H2 미지원 기능 확인
- [ ] 복잡한 쿼리는 통합 테스트 권장
- [ ] Flyway 마이그레이션 검증은 통합 테스트

---

## 7. 참고 문서

- [MySQL 테스트 가이드](./01_mysql-testing-guide.md)
- [Repository 통합 테스트](./02_repository-integration-test.md)
- [Mapper 단위 테스트](./04_mapper-unit-test.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
