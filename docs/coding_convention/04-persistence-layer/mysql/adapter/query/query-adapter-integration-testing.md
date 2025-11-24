# Query Adapter 통합 테스트 가이드

> **목적**: Query Adapter의 N+1 해결 패턴 + Mapper + Domain 변환 전체 흐름 검증

---

## 1️⃣ 테스트 전략

### 왜 통합 테스트가 필요한가?

**Query Adapter의 역할**:
```java
@Component
public class OrderQueryAdapter implements OrderQueryPort {
    private final OrderQueryDslRepository orderRepository;
    private final CustomerQueryDslRepository customerRepository;
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

        // 4. Mapper로 변환
        return orders.stream()
            .map(order -> mapper.toDomain(order, customerMap.get(order.getCustomerId())))
            .toList();
    }
}
```

**검증해야 할 것**:
1. ✅ **N+1 해결 패턴**: Customer 일괄 조회가 실제로 동작하는가?
2. ✅ **Mapper 변환**: Entity → Domain 변환이 올바른가?
3. ✅ **Domain 결합**: Customer 정보가 Order Domain에 올바르게 포함되는가?
4. ✅ **전체 흐름**: Repository → Mapper → Domain 전체가 통합되어 동작하는가?

**단위 테스트와의 차이**:
- 단위 테스트: Repository, Mapper 각각 독립적으로 검증
- 통합 테스트: Repository + Mapper + Adapter 조합 검증

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

import com.company.adapter.out.persistence.entity.CustomerJpaEntity;
import com.company.adapter.out.persistence.entity.OrderJpaEntity;
import com.company.adapter.out.persistence.entity.OrderStatus;
import com.company.adapter.out.persistence.mapper.OrderJpaEntityMapper;
import com.company.adapter.out.persistence.repository.CustomerRepository;
import com.company.adapter.out.persistence.repository.OrderRepository;
import com.company.application.port.in.dto.query.SearchOrderQuery;
import com.company.domain.order.OrderDomain;
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
 * OrderQueryAdapter 통합 테스트
 *
 * <p><strong>검증 내용:</strong></p>
 * <ul>
 *   <li>N+1 해결 패턴: Customer 일괄 조회</li>
 *   <li>Mapper 변환: Entity → Domain 정확도</li>
 *   <li>Domain 결합: Customer 정보 포함 검증</li>
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
    OrderQueryAdapter.class,
    OrderQueryDslRepository.class,
    CustomerQueryDslRepository.class,
    OrderJpaEntityMapper.class
})
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true"
})
@DisplayName("Query Adapter 통합 테스트")
class OrderQueryAdapterIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private OrderQueryAdapter adapter;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ====================================
    // 1. findById 통합 테스트
    // ====================================

    @Test
    @DisplayName("findById: Entity → Domain 변환 정확도")
    void findById_EntityToDomain_CorrectConversion() {
        // Given
        CustomerJpaEntity customer = customerRepository.save(
            createCustomer("Alice", "alice@example.com")
        );
        OrderJpaEntity order = orderRepository.save(
            createOrder(customer.getId(), OrderStatus.PENDING, 10000L, LocalDate.of(2024, 1, 1))
        );

        // When
        Optional<OrderDomain> result = adapter.findById(order.getId());

        // Then
        assertThat(result).isPresent();
        OrderDomain domain = result.get();

        // Entity 필드 검증
        assertThat(domain.getId()).isEqualTo(order.getId());
        assertThat(domain.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(domain.getTotalAmount()).isEqualTo(10000L);
        assertThat(domain.getOrderDate()).isEqualTo(LocalDate.of(2024, 1, 1));

        // Customer 정보 검증 (Mapper가 결합)
        assertThat(domain.getCustomerName()).isEqualTo("Alice");
        assertThat(domain.getCustomerEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("findById: 존재하지 않는 ID는 Empty 반환")
    void findById_NonExistingId_ReturnsEmpty() {
        // When
        Optional<OrderDomain> result = adapter.findById(999L);

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
        CustomerJpaEntity customer = customerRepository.save(createCustomer("Alice", "alice@example.com"));
        OrderJpaEntity order = orderRepository.save(createOrder(customer.getId(), OrderStatus.PENDING, 10000L));

        // When
        boolean exists = adapter.existsById(order.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById: 존재하지 않는 ID는 false 반환")
    void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = adapter.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    // ====================================
    // 3. findByCriteria 통합 테스트 (N+1 해결)
    // ====================================

    @Test
    @DisplayName("findByCriteria: N+1 해결 패턴으로 Customer 정보 포함 조회")
    void findByCriteria_WithCustomerInfo_ResolvedN1Problem() {
        // Given
        CustomerJpaEntity alice = customerRepository.save(createCustomer("Alice", "alice@example.com"));
        CustomerJpaEntity bob = customerRepository.save(createCustomer("Bob", "bob@example.com"));

        orderRepository.save(createOrder(alice.getId(), OrderStatus.PENDING, 10000L));
        orderRepository.save(createOrder(alice.getId(), OrderStatus.CONFIRMED, 20000L));
        orderRepository.save(createOrder(bob.getId(), OrderStatus.PENDING, 30000L));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null, null, null, null, null, null
        );

        // When
        List<OrderDomain> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);

        // Customer 정보 포함 검증
        assertThat(results).allMatch(order -> order.getCustomerName() != null);
        assertThat(results).allMatch(order -> order.getCustomerEmail() != null);

        // Alice 주문 검증
        List<OrderDomain> aliceOrders = results.stream()
            .filter(order -> order.getCustomerName().equals("Alice"))
            .toList();
        assertThat(aliceOrders).hasSize(2);
        assertThat(aliceOrders).allMatch(order -> order.getCustomerEmail().equals("alice@example.com"));

        // Bob 주문 검증
        List<OrderDomain> bobOrders = results.stream()
            .filter(order -> order.getCustomerName().equals("Bob"))
            .toList();
        assertThat(bobOrders).hasSize(1);
        assertThat(bobOrders).allMatch(order -> order.getCustomerEmail().equals("bob@example.com"));
    }

    @Test
    @DisplayName("findByCriteria: 동적 조건 + N+1 해결 + Domain 변환")
    void findByCriteria_ComplexConditions_WithCustomerInfo() {
        // Given
        CustomerJpaEntity alice = customerRepository.save(createCustomer("Alice", "alice@example.com"));
        CustomerJpaEntity bob = customerRepository.save(createCustomer("Bob", "bob@example.com"));

        orderRepository.save(createOrder(alice.getId(), OrderStatus.PENDING, 10000L, LocalDate.of(2024, 1, 1)));
        orderRepository.save(createOrder(alice.getId(), OrderStatus.CONFIRMED, 20000L, LocalDate.of(2024, 1, 15)));
        orderRepository.save(createOrder(bob.getId(), OrderStatus.PENDING, 30000L, LocalDate.of(2024, 1, 20)));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, OrderStatus.PENDING,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
            null, null, null, null, null
        );

        // When
        List<OrderDomain> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(2);  // Alice와 Bob의 PENDING 주문

        // 조건 검증
        assertThat(results).allMatch(order -> order.getStatus() == OrderStatus.PENDING);
        assertThat(results).allMatch(order ->
            !order.getOrderDate().isBefore(LocalDate.of(2024, 1, 1)) &&
            !order.getOrderDate().isAfter(LocalDate.of(2024, 1, 31))
        );

        // Customer 정보 검증
        assertThat(results).allMatch(order -> order.getCustomerName() != null);
        assertThat(results.stream().map(OrderDomain::getCustomerName)).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @DisplayName("findByCriteria: 페이징 + N+1 해결 + Domain 변환")
    void findByCriteria_WithPaging_AndCustomerInfo() {
        // Given
        CustomerJpaEntity alice = customerRepository.save(createCustomer("Alice", "alice@example.com"));

        for (int i = 0; i < 15; i++) {
            orderRepository.save(createOrder(alice.getId(), OrderStatus.PENDING, 10000L));
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            null, null, 0, 10, null  // 첫 페이지, 10개
        );

        // When
        List<OrderDomain> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(10);
        assertThat(results).allMatch(order -> order.getCustomerName().equals("Alice"));
        assertThat(results).allMatch(order -> order.getCustomerEmail().equals("alice@example.com"));
    }

    // ====================================
    // 4. countByCriteria 통합 테스트
    // ====================================

    @Test
    @DisplayName("countByCriteria: 동적 조건으로 카운트")
    void countByCriteria_WithConditions_ReturnsCorrectCount() {
        // Given
        CustomerJpaEntity alice = customerRepository.save(createCustomer("Alice", "alice@example.com"));
        CustomerJpaEntity bob = customerRepository.save(createCustomer("Bob", "bob@example.com"));

        orderRepository.save(createOrder(alice.getId(), OrderStatus.PENDING, 10000L));
        orderRepository.save(createOrder(alice.getId(), OrderStatus.CONFIRMED, 20000L));
        orderRepository.save(createOrder(bob.getId(), OrderStatus.PENDING, 30000L));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, OrderStatus.PENDING, null, null, null, null, null, null, null
        );

        // When
        long count = adapter.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ====================================
    // 5. N+1 문제 검증 (선택적)
    // ====================================

    /**
     * N+1 문제가 실제로 해결되었는지 검증
     *
     * <p>검증 방법:</p>
     * <ul>
     *   <li>Hibernate Statistics 사용</li>
     *   <li>P6Spy 사용</li>
     *   <li>실행된 쿼리 개수 확인</li>
     * </ul>
     *
     * <p>기대 쿼리 수:</p>
     * <ul>
     *   <li>1. Order 조회 쿼리 (1개)</li>
     *   <li>2. Customer 일괄 조회 쿼리 (1개)</li>
     *   <li>총 2개의 쿼리만 실행되어야 함</li>
     * </ul>
     */
    @Test
    @DisplayName("findByCriteria: N+1 문제 해결 검증 (쿼리 개수 확인)")
    void findByCriteria_N1Problem_OnlyTwoQueries() {
        // Given
        CustomerJpaEntity alice = customerRepository.save(createCustomer("Alice", "alice@example.com"));
        CustomerJpaEntity bob = customerRepository.save(createCustomer("Bob", "bob@example.com"));

        for (int i = 0; i < 10; i++) {
            orderRepository.save(createOrder(alice.getId(), OrderStatus.PENDING, 10000L));
        }
        for (int i = 0; i < 10; i++) {
            orderRepository.save(createOrder(bob.getId(), OrderStatus.CONFIRMED, 20000L));
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null, null, null, null, null, null
        );

        // When
        List<OrderDomain> results = adapter.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(20);

        // ⚠️ 실제 쿼리 개수 검증은 다음 방법 중 하나 사용:
        // 1. Hibernate Statistics:
        //    SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        //    Statistics stats = sessionFactory.getStatistics();
        //    assertThat(stats.getQueryExecutionCount()).isEqualTo(2);
        //
        // 2. P6Spy:
        //    P6Spy 로그에서 실행된 쿼리 개수 확인
        //
        // 3. 수동 검증:
        //    spring.jpa.show-sql=true로 콘솔에 출력된 쿼리 개수 확인
    }

    // ====================================
    // Test Fixture 메서드
    // ====================================

    private CustomerJpaEntity createCustomer(String name, String email) {
        return CustomerJpaEntity.builder()
            .name(name)
            .email(email)
            .build();
    }

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status, Long totalAmount) {
        return OrderJpaEntity.builder()
            .customerId(customerId)
            .status(status)
            .totalAmount(totalAmount)
            .orderDate(LocalDate.now())
            .build();
    }

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status, Long totalAmount, LocalDate orderDate) {
        return OrderJpaEntity.builder()
            .customerId(customerId)
            .status(status)
            .totalAmount(totalAmount)
            .orderDate(orderDate)
            .build();
    }
}
```

---

## 3️⃣ N+1 문제 검증 방법 (선택적)

### 방법 1: Hibernate Statistics

```java
@Test
@DisplayName("N+1 문제 해결 검증 - Hibernate Statistics")
void verifyN1Problem_UsingHibernateStatistics() {
    // Given
    EntityManagerFactory emf = entityManager.getEntityManagerFactory();
    SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    // Given: 데이터 준비
    // ... (생략)

    // When
    List<OrderDomain> results = adapter.findByCriteria(criteria);

    // Then
    assertThat(stats.getQueryExecutionCount()).isEqualTo(2);  // Order 1개 + Customer 1개
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

**테스트 실행 후 로그 확인**:
```
# 기대 로그 (2개 쿼리만 실행)
SELECT * FROM orders WHERE ...
SELECT * FROM customers WHERE customer_id IN (1, 2, 3)
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
- Order 조회 쿼리 1개
- Customer 일괄 조회 쿼리 1개
- 총 2개의 쿼리만 실행되어야 함

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

### findById 통합 테스트
- [ ] Entity → Domain 변환 정확도
- [ ] Customer 정보 포함 검증
- [ ] 존재하지 않는 ID는 Empty 반환

### existsById 통합 테스트
- [ ] 존재하는 ID는 true 반환
- [ ] 존재하지 않는 ID는 false 반환

### findByCriteria 통합 테스트
- [ ] N+1 해결 패턴으로 Customer 정보 포함 조회
- [ ] 동적 조건 + N+1 해결 + Domain 변환
- [ ] 페이징 + N+1 해결 + Domain 변환
- [ ] N+1 문제 해결 검증 (쿼리 개수 확인) - 선택적

### countByCriteria 통합 테스트
- [ ] 동적 조건으로 카운트

---

## 6️⃣ 참고 문서

- [query-adapter-guide.md](./query-adapter-guide.md) - Query Adapter 컨벤션
- [querydsl-repository-testing.md](../../repository/querydsl-repository-testing.md) - QueryDSL Repository 단위 테스트
- [flyway-testing-guide.md](../../config/flyway-testing-guide.md) - Flyway 통합 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
