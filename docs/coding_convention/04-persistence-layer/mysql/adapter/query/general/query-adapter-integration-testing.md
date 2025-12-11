# Query Adapter 통합 테스트 가이드

> **목적**: Query Adapter의 Repository 위임 + Mapper 변환 전체 흐름 검증

---

## 1️⃣ 테스트 전략

### 왜 통합 테스트가 필요한가?

**Query Adapter의 역할 (1:1 매핑)**:
```java
@Component
public class OrderQueryAdapter implements OrderQueryPort {

    private final OrderQueryDslRepository queryDslRepository;  // 1:1 매핑
    private final OrderJpaEntityMapper mapper;                  // Mapper

    public OrderQueryAdapter(
        OrderQueryDslRepository queryDslRepository,
        OrderJpaEntityMapper mapper
    ) {
        this.queryDslRepository = queryDslRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return queryDslRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(OrderId id) {
        return queryDslRepository.existsById(id.getValue());
    }

    @Override
    public List<Order> findByCriteria(OrderSearchCriteria criteria) {
        return queryDslRepository.findByCriteria(criteria).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public long countByCriteria(OrderSearchCriteria criteria) {
        return queryDslRepository.countByCriteria(criteria);
    }
}
```

**핵심 원칙**:
- ✅ **1:1 매핑**: QueryAdapter는 정확히 1개의 QueryDslRepository와 1:1 매핑
- ✅ **필드 2개만**: QueryDslRepository + Mapper (정확히 2개)
- ✅ **단순 위임**: Repository 호출 → Mapper 변환 → Domain 반환
- ❌ **N+1 해결은 Application Layer에서**: 여러 Adapter 조합은 UseCase에서 처리

**검증해야 할 것**:
1. ✅ **Repository 위임**: QueryDslRepository 호출이 올바른가?
2. ✅ **Mapper 변환**: Entity → Domain 변환이 올바른가?
3. ✅ **전체 흐름**: Repository → Mapper → Domain 전체가 통합되어 동작하는가?

**단위 테스트와의 차이**:
- 단위 테스트: Repository, Mapper 각각 독립적으로 검증
- 통합 테스트: Repository + Mapper + Adapter 조합 검증

### N+1 해결은 Application Layer에서

```java
// ✅ Application Layer(UseCase)에서 여러 Adapter 조합으로 N+1 해결
@Component
public class OrderQueryUseCase {

    private final OrderQueryPort orderQueryPort;       // Order Adapter
    private final CustomerQueryPort customerQueryPort; // Customer Adapter

    @Transactional(readOnly = true)
    public List<OrderWithCustomerResponse> findOrdersWithCustomer(OrderSearchCriteria criteria) {
        // 1. Order 조회 (OrderQueryAdapter)
        List<Order> orders = orderQueryPort.findByCriteria(criteria);

        // 2. Customer ID 추출
        Set<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .collect(Collectors.toSet());

        // 3. Customer 일괄 조회 (CustomerQueryAdapter) - N+1 해결
        Map<Long, Customer> customerMap = customerQueryPort
            .findByIds(customerIds).stream()
            .collect(Collectors.toMap(Customer::getId, Function.identity()));

        // 4. Response 조합
        return orders.stream()
            .map(order -> new OrderWithCustomerResponse(
                order,
                customerMap.get(order.getCustomerId())
            ))
            .toList();
    }
}
```

**N+1 해결 책임 분리**:
- ❌ **Adapter**: 여러 Repository 조합 금지 (1:1 매핑 위반)
- ✅ **Application Layer**: 여러 Adapter 조합으로 N+1 해결

---

## 2️⃣ 테스트 환경 설정

### Gradle 의존성

```gradle
dependencies {
    // 기존 의존성
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'

    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers:1.19.0'
    testImplementation 'org.testcontainers:mysql:1.19.0'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
}
```

### 테스트 클래스 구조

```java
package com.company.adapter.out.persistence.adapter;

import com.company.adapter.out.persistence.entity.OrderJpaEntity;
import com.company.adapter.out.persistence.entity.OrderStatus;
import com.company.adapter.out.persistence.mapper.OrderJpaEntityMapper;
import com.company.adapter.out.persistence.repository.OrderRepository;
import com.company.adapter.out.persistence.repository.OrderQueryDslRepository;
import com.company.application.port.in.dto.query.OrderSearchCriteria;
import com.company.domain.order.Order;
import com.company.domain.order.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderQueryAdapter 통합 테스트 (1:1 매핑)
 *
 * <p><strong>핵심 원칙:</strong></p>
 * <ul>
 *   <li>1:1 매핑: OrderQueryAdapter ↔ OrderQueryDslRepository</li>
 *   <li>필드 2개: QueryDslRepository + Mapper</li>
 *   <li>단순 위임: Repository → Mapper → Domain</li>
 * </ul>
 *
 * <p><strong>검증 내용:</strong></p>
 * <ul>
 *   <li>Repository 위임: QueryDslRepository 호출</li>
 *   <li>Mapper 변환: Entity → Domain 정확도</li>
 *   <li>전체 흐름: Repository → Mapper → Domain</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({
    OrderQueryAdapter.class,        // 테스트 대상 Adapter
    OrderQueryDslRepository.class,  // 1:1 매핑된 Repository
    OrderJpaEntityMapper.class      // Mapper
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true"
})
@DisplayName("Query Adapter 통합 테스트 (1:1 매핑)")
class OrderQueryAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private OrderQueryAdapter adapter;

    @Autowired
    private OrderRepository orderRepository;  // 테스트 데이터 준비용 (JpaRepository)

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // ====================================
    // 1. findById 통합 테스트
    // ====================================

    @Test
    @DisplayName("findById: Entity → Domain 변환 정확도")
    void findById_EntityToDomain_CorrectConversion() {
        // Given
        OrderJpaEntity order = orderRepository.save(
            createOrder(1L, OrderStatus.PENDING, 10000L, LocalDate.of(2024, 1, 1))
        );

        // When
        Optional<Order> result = adapter.findById(new OrderId(order.getId()));

        // Then
        assertThat(result).isPresent();
        Order domain = result.get();

        // Entity 필드 검증
        assertThat(domain.getId().getValue()).isEqualTo(order.getId());
        assertThat(domain.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(domain.getTotalAmount()).isEqualTo(10000L);
        assertThat(domain.getOrderDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(domain.getCustomerId()).isEqualTo(1L);  // Long FK 검증
    }

    @Test
    @DisplayName("findById: 존재하지 않는 ID는 Empty 반환")
    void findById_NonExistingId_ReturnsEmpty() {
        // When
        Optional<Order> result = adapter.findById(new OrderId(999L));

        // Then
        assertThat(result).isEmpty();
    }

    // ====================================
    // 2. existsById 통합 테스트
    // ====================================

    @Test
    @DisplayName("existsById: 존재하는 ID는 true 반환")
    void existsById_ExistingId_ReturnsTrue() {
        // Given
        OrderJpaEntity order = orderRepository.save(
            createOrder(1L, OrderStatus.PENDING, 10000L)
        );

        // When
        boolean exists = adapter.existsById(new OrderId(order.getId()));

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById: 존재하지 않는 ID는 false 반환")
    void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = adapter.existsById(new OrderId(999L));

        // Then
        assertThat(exists).isFalse();
    }

    // ====================================
    // 3. findByCriteria 통합 테스트
    // ====================================

    @Test
    @DisplayName("findByCriteria: 목록 조회 + Domain 변환")
    void findByCriteria_ReturnsOrderDomainList() {
        // Given
        orderRepository.save(createOrder(1L, OrderStatus.PENDING, 10000L));
        orderRepository.save(createOrder(1L, OrderStatus.CONFIRMED, 20000L));
        orderRepository.save(createOrder(2L, OrderStatus.PENDING, 30000L));

        OrderSearchCriteria criteria = OrderSearchCriteria.builder().build();

        // When
        List<Order> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(order -> order.getId() != null);
        assertThat(results).allMatch(order -> order.getCustomerId() != null);  // Long FK 포함
    }

    @Test
    @DisplayName("findByCriteria: 동적 조건 + Domain 변환")
    void findByCriteria_WithDynamicConditions() {
        // Given
        orderRepository.save(createOrder(1L, OrderStatus.PENDING, 10000L, LocalDate.of(2024, 1, 1)));
        orderRepository.save(createOrder(1L, OrderStatus.CONFIRMED, 20000L, LocalDate.of(2024, 1, 15)));
        orderRepository.save(createOrder(2L, OrderStatus.PENDING, 30000L, LocalDate.of(2024, 1, 20)));

        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
            .status(OrderStatus.PENDING)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 1, 31))
            .build();

        // When
        List<Order> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(2);  // PENDING 주문만
        assertThat(results).allMatch(order -> order.getStatus() == OrderStatus.PENDING);
        assertThat(results).allMatch(order ->
            !order.getOrderDate().isBefore(LocalDate.of(2024, 1, 1)) &&
            !order.getOrderDate().isAfter(LocalDate.of(2024, 1, 31))
        );
    }

    @Test
    @DisplayName("findByCriteria: 페이징 + Domain 변환")
    void findByCriteria_WithPaging() {
        // Given
        for (int i = 0; i < 15; i++) {
            orderRepository.save(createOrder(1L, OrderStatus.PENDING, 10000L));
        }

        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
            .page(0)
            .size(10)
            .build();

        // When
        List<Order> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(10);
    }

    // ====================================
    // 4. countByCriteria 통합 테스트
    // ====================================

    @Test
    @DisplayName("countByCriteria: 동적 조건으로 카운트")
    void countByCriteria_WithConditions_ReturnsCorrectCount() {
        // Given
        orderRepository.save(createOrder(1L, OrderStatus.PENDING, 10000L));
        orderRepository.save(createOrder(1L, OrderStatus.CONFIRMED, 20000L));
        orderRepository.save(createOrder(2L, OrderStatus.PENDING, 30000L));

        OrderSearchCriteria criteria = OrderSearchCriteria.builder()
            .status(OrderStatus.PENDING)
            .build();

        // When
        long count = adapter.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ====================================
    // Test Fixture 메서드
    // ====================================

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status, Long totalAmount) {
        return OrderJpaEntity.builder()
            .customerId(customerId)  // Long FK
            .status(status)
            .totalAmount(totalAmount)
            .orderDate(LocalDate.now())
            .build();
    }

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status, Long totalAmount, LocalDate orderDate) {
        return OrderJpaEntity.builder()
            .customerId(customerId)  // Long FK
            .status(status)
            .totalAmount(totalAmount)
            .orderDate(orderDate)
            .build();
    }
}
```

---

## 3️⃣ 쿼리 검증 방법 (선택적)

### 방법 1: Hibernate Statistics

```java
@Test
@DisplayName("쿼리 개수 검증 - Hibernate Statistics")
void verifyQueryCount_UsingHibernateStatistics() {
    // Given
    EntityManagerFactory emf = entityManager.getEntityManagerFactory();
    SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    OrderSearchCriteria criteria = OrderSearchCriteria.builder().build();

    // When
    List<Order> results = adapter.findByCriteria(criteria);

    // Then
    assertThat(stats.getQueryExecutionCount()).isEqualTo(1);  // Order 조회 1개
}
```

### 방법 2: P6Spy

**build.gradle**:
```gradle
testImplementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0'
```

**application-test.yml**:
```yaml
decorator:
  datasource:
    p6spy:
      enable-logging: true
```

### 방법 3: 수동 검증

**application-test.yml**:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**테스트 실행 후 콘솔 출력 확인**:
- Order 조회 쿼리만 실행되는지 확인
- 1:1 매핑 원칙에 따라 단일 Repository 쿼리만 실행

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 통합 테스트 실행
./gradlew test

# Query Adapter 통합 테스트만 실행
./gradlew test --tests "*QueryAdapterIntegrationTest"

# 특정 테스트 메서드만 실행
./gradlew test --tests "*QueryAdapterIntegrationTest.findByCriteria_WithCustomerInfo_ResolvedN1Problem"
```

### IDE 실행

- IntelliJ IDEA: `OrderQueryAdapterIntegrationTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 5️⃣ 테스트 작성 체크리스트

Query Adapter 통합 테스트 작성 시:

### 핵심 원칙 검증
- [ ] **1:1 매핑 준수**: Adapter ↔ Repository 1:1 매핑
- [ ] **필드 2개만**: QueryDslRepository + Mapper
- [ ] **단순 위임**: Repository 호출 → Mapper 변환 → Domain 반환

### findById 통합 테스트
- [ ] Entity → Domain 변환 정확도
- [ ] Long FK 필드 포함 검증
- [ ] 존재하지 않는 ID는 Empty 반환

### existsById 통합 테스트
- [ ] 존재하는 ID는 true 반환
- [ ] 존재하지 않는 ID는 false 반환

### findByCriteria 통합 테스트
- [ ] 목록 조회 + Domain 변환
- [ ] 동적 조건 + Domain 변환
- [ ] 페이징 + Domain 변환

### countByCriteria 통합 테스트
- [ ] 동적 조건으로 카운트

### N+1 해결 (Application Layer 책임)
- [ ] ❌ Adapter에서 여러 Repository 조합 금지
- [ ] ✅ Application Layer에서 여러 Adapter 조합으로 해결

---

## 6️⃣ 참고 문서

- [query-adapter-guide.md](./query-adapter-guide.md) - Query Adapter 컨벤션
- [querydsl-repository-test-guide.md](../../repository/querydsl/querydsl-repository-test-guide.md) - QueryDSL Repository 단위 테스트
- [flyway-guide.md](../../config/flyway-guide.md) - Flyway 통합 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
