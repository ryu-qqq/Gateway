# QueryDSL Repository 단위 테스트 가이드

> **목적**: QueryDSL Repository의 4개 표준 메서드 실제 동작 검증

---

## 1️⃣ 테스트 전략

### 왜 테스트가 필요한가?

**JPA Repository vs QueryDSL Repository**:
- ❌ **JPA Repository**: Spring Data가 검증된 구현 제공 → **테스트 불필요**
- ✅ **QueryDSL Repository**: 우리가 작성한 동적 쿼리 로직 → **테스트 필수**

**QueryDSL Repository에서 검증할 것**:
1. **동적 조건 조합** (10+ 케이스)
   - `filterId`, `status`, `startDate`, `endDate` 조합
   - 조건 없음 (전체 조회)
2. **페이징**
   - Offset 페이징 (`page`, `size`)
   - Cursor 페이징 (`lastId`, `size`)
3. **정렬**
   - `sortBy`, `sortDirection` 조합
4. **4개 메서드 각각**
   - `findById(Long id)`
   - `existsById(Long id)`
   - `findByCriteria(Criteria criteria)`
   - `countByCriteria(Criteria criteria)`

---

## ⚠️ 중요: 테스트 케이스는 예시입니다

**이 문서의 테스트 케이스는 패턴 예시입니다**:
- ✅ **목적**: QueryDSL Repository 테스트 작성 방법 제시
- ⚠️ **주의**: 실제 프로젝트의 `SearchQuery` DTO 필드에 따라 테스트 케이스는 달라집니다
- 📝 **적용**: 프로젝트의 동적 조회 조건에 맞게 테스트 케이스를 추가/수정하세요

**예시**:
```java
// 이 문서의 예시 (Order 도메인)
SearchOrderQuery criteria = new SearchOrderQuery(
    filterId, status, startDate, endDate, ...
);

// 실제 프로젝트 (Product 도메인)
SearchProductQuery criteria = new SearchProductQuery(
    categoryId, brandId, minPrice, maxPrice, inStock, ...
);
// → 프로젝트 필드에 맞게 테스트 케이스 작성 필요
```

**테스트 작성 가이드**:
1. **필드 파악**: 프로젝트의 `SearchQuery` DTO 필드 확인
2. **조합 테스트**: 1개, 2개, 3개 이상 필드 조합 케이스 작성
3. **경계 테스트**: null, 빈 값, 범위 초과 등 경계 조건 검증
4. **페이징/정렬**: 프로젝트에서 사용하는 필드로 정렬 테스트

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
package com.company.adapter.out.persistence.repository;

import com.company.adapter.out.persistence.entity.OrderJpaEntity;
import com.company.adapter.out.persistence.entity.OrderStatus;
import com.company.application.port.in.dto.query.SearchOrderQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderQueryDslRepository 단위 테스트
 *
 * <p><strong>검증 내용:</strong></p>
 * <ul>
 *   <li>findById: 단건 조회 성공/실패</li>
 *   <li>existsById: 존재 여부 boolean 반환</li>
 *   <li>findByCriteria: 동적 조건 조합 (10+ 케이스)</li>
 *   <li>countByCriteria: 조건별 카운트 정확도</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true"
})
@DisplayName("QueryDSL Repository 단위 테스트")
class OrderQueryDslRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private OrderQueryDslRepository queryDslRepository;

    @Autowired
    private OrderRepository jpaRepository;  // Fixture 데이터 저장용

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    // ====================================
    // 1. findById 테스트
    // ====================================

    @Test
    @DisplayName("findById: 존재하는 ID로 조회 성공")
    void findById_ExistingId_ReturnsEntity() {
        // Given
        OrderJpaEntity order = jpaRepository.save(createOrder());

        // When
        Optional<OrderJpaEntity> result = queryDslRepository.findById(order.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(order.getId());
    }

    @Test
    @DisplayName("findById: 존재하지 않는 ID로 조회 시 Empty 반환")
    void findById_NonExistingId_ReturnsEmpty() {
        // When
        Optional<OrderJpaEntity> result = queryDslRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    // ====================================
    // 2. existsById 테스트
    // ====================================

    @Test
    @DisplayName("existsById: 존재하는 ID는 true 반환")
    void existsById_ExistingId_ReturnsTrue() {
        // Given
        OrderJpaEntity order = jpaRepository.save(createOrder());

        // When
        boolean exists = queryDslRepository.existsById(order.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsById: 존재하지 않는 ID는 false 반환")
    void existsById_NonExistingId_ReturnsFalse() {
        // When
        boolean exists = queryDslRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    // ====================================
    // 3. findByCriteria 테스트 (동적 조건)
    // ====================================

    @Test
    @DisplayName("findByCriteria: filterId로 조회")
    void findByCriteria_ByFilterId_ReturnsMatchingOrders() {
        // Given
        OrderJpaEntity order1 = jpaRepository.save(createOrder(1L, OrderStatus.PENDING));
        OrderJpaEntity order2 = jpaRepository.save(createOrder(2L, OrderStatus.PENDING));

        SearchOrderQuery criteria = new SearchOrderQuery(
            1L, null, null, null, null, null, null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(order1.getId());
    }

    @Test
    @DisplayName("findByCriteria: status로 조회")
    void findByCriteria_ByStatus_ReturnsMatchingOrders() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING));
        jpaRepository.save(createOrder(2L, OrderStatus.CONFIRMED));
        jpaRepository.save(createOrder(3L, OrderStatus.PENDING));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, OrderStatus.PENDING, null, null, null, null, null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(order -> order.getStatus() == OrderStatus.PENDING);
    }

    @Test
    @DisplayName("findByCriteria: startDate와 endDate 범위 조회")
    void findByCriteria_ByDateRange_ReturnsMatchingOrders() {
        // Given
        jpaRepository.save(createOrder(LocalDate.of(2024, 1, 1)));
        jpaRepository.save(createOrder(LocalDate.of(2024, 1, 15)));
        jpaRepository.save(createOrder(LocalDate.of(2024, 2, 1)));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
            null, null, null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(order ->
            !order.getOrderDate().isBefore(LocalDate.of(2024, 1, 1)) &&
            !order.getOrderDate().isAfter(LocalDate.of(2024, 1, 31))
        );
    }

    @Test
    @DisplayName("findByCriteria: 복합 조건 (filterId + status + dateRange)")
    void findByCriteria_ComplexConditions_ReturnsMatchingOrders() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING, LocalDate.of(2024, 1, 1)));
        jpaRepository.save(createOrder(1L, OrderStatus.CONFIRMED, LocalDate.of(2024, 1, 15)));
        jpaRepository.save(createOrder(2L, OrderStatus.PENDING, LocalDate.of(2024, 1, 20)));

        SearchOrderQuery criteria = new SearchOrderQuery(
            1L, OrderStatus.PENDING,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
            null, null, null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("findByCriteria: 조건 없음 (전체 조회)")
    void findByCriteria_NoConditions_ReturnsAllOrders() {
        // Given
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null, null, null, null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);
    }

    // ====================================
    // 4. findByCriteria 테스트 (페이징)
    // ====================================

    @Test
    @DisplayName("findByCriteria: Offset 페이징 (page=0, size=10)")
    void findByCriteria_OffsetPaging_FirstPage_ReturnsPagedResults() {
        // Given
        for (int i = 0; i < 25; i++) {
            jpaRepository.save(createOrder());
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            null, null, 0, 10, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("findByCriteria: Offset 페이징 (page=2, size=10)")
    void findByCriteria_OffsetPaging_ThirdPage_ReturnsPagedResults() {
        // Given
        for (int i = 0; i < 25; i++) {
            jpaRepository.save(createOrder());
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            null, null, 2, 10, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(5);  // 25개 중 마지막 5개
    }

    @Test
    @DisplayName("findByCriteria: Cursor 페이징 (lastId + size)")
    void findByCriteria_CursorPaging_ReturnsNextPage() {
        // Given
        List<OrderJpaEntity> orders = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            orders.add(jpaRepository.save(createOrder()));
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            null, null, null, 10, orders.get(4).getId()  // lastId = 5번째 주문
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(10);
        assertThat(results.get(0).getId()).isGreaterThan(orders.get(4).getId());
    }

    @Test
    @DisplayName("findByCriteria: Cursor 페이징 (size+1 조회로 다음 페이지 존재 확인)")
    void findByCriteria_CursorPaging_CheckHasNext() {
        // Given
        List<OrderJpaEntity> orders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            orders.add(jpaRepository.save(createOrder()));
        }

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            null, null, null, 10, orders.get(9).getId()  // lastId = 10번째 주문
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(11);  // size+1 조회
        // 실제 반환 시: results.subList(0, 10) 반환, hasNext = results.size() > 10
    }

    // ====================================
    // 5. findByCriteria 테스트 (정렬)
    // ====================================

    @Test
    @DisplayName("findByCriteria: 정렬 ASC (id 오름차순)")
    void findByCriteria_SortByIdAsc_ReturnsSortedResults() {
        // Given
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            "id", "asc", null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).isSortedAccordingTo(Comparator.comparing(OrderJpaEntity::getId));
    }

    @Test
    @DisplayName("findByCriteria: 정렬 DESC (id 내림차순)")
    void findByCriteria_SortByIdDesc_ReturnsSortedResults() {
        // Given
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            "id", "desc", null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).isSortedAccordingTo(Comparator.comparing(OrderJpaEntity::getId).reversed());
    }

    @Test
    @DisplayName("findByCriteria: 정렬 ASC (orderDate 오름차순)")
    void findByCriteria_SortByOrderDateAsc_ReturnsSortedResults() {
        // Given
        jpaRepository.save(createOrder(LocalDate.of(2024, 1, 3)));
        jpaRepository.save(createOrder(LocalDate.of(2024, 1, 1)));
        jpaRepository.save(createOrder(LocalDate.of(2024, 1, 2)));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null,
            "orderDate", "asc", null, null, null
        );

        // When
        List<OrderJpaEntity> results = queryDslRepository.findByCriteria(criteria);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).isSortedAccordingTo(Comparator.comparing(OrderJpaEntity::getOrderDate));
    }

    // ====================================
    // 6. countByCriteria 테스트
    // ====================================

    @Test
    @DisplayName("countByCriteria: filterId로 카운트")
    void countByCriteria_ByFilterId_ReturnsCorrectCount() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING));
        jpaRepository.save(createOrder(1L, OrderStatus.CONFIRMED));
        jpaRepository.save(createOrder(2L, OrderStatus.PENDING));

        SearchOrderQuery criteria = new SearchOrderQuery(
            1L, null, null, null, null, null, null, null, null
        );

        // When
        long count = queryDslRepository.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByCriteria: status로 카운트")
    void countByCriteria_ByStatus_ReturnsCorrectCount() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING));
        jpaRepository.save(createOrder(2L, OrderStatus.PENDING));
        jpaRepository.save(createOrder(3L, OrderStatus.CONFIRMED));

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, OrderStatus.PENDING, null, null, null, null, null, null, null
        );

        // When
        long count = queryDslRepository.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByCriteria: 복합 조건 (filterId + status + dateRange)")
    void countByCriteria_ComplexConditions_ReturnsCorrectCount() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING, LocalDate.of(2024, 1, 1)));
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING, LocalDate.of(2024, 1, 15)));
        jpaRepository.save(createOrder(1L, OrderStatus.CONFIRMED, LocalDate.of(2024, 1, 20)));
        jpaRepository.save(createOrder(2L, OrderStatus.PENDING, LocalDate.of(2024, 1, 25)));

        SearchOrderQuery criteria = new SearchOrderQuery(
            1L, OrderStatus.PENDING,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
            null, null, null, null, null
        );

        // When
        long count = queryDslRepository.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByCriteria: 조건 없음 (전체 카운트)")
    void countByCriteria_NoConditions_ReturnsAllCount() {
        // Given
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());
        jpaRepository.save(createOrder());

        SearchOrderQuery criteria = new SearchOrderQuery(
            null, null, null, null, null, null, null, null, null
        );

        // When
        long count = queryDslRepository.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByCriteria: 매칭되지 않는 조건은 0 반환")
    void countByCriteria_NoMatchingConditions_ReturnsZero() {
        // Given
        jpaRepository.save(createOrder(1L, OrderStatus.PENDING));

        SearchOrderQuery criteria = new SearchOrderQuery(
            999L, null, null, null, null, null, null, null, null
        );

        // When
        long count = queryDslRepository.countByCriteria(criteria);

        // Then
        assertThat(count).isEqualTo(0);
    }

    // ====================================
    // Test Fixture 메서드
    // ====================================

    private OrderJpaEntity createOrder() {
        return OrderJpaEntity.builder()
            .customerId(1L)
            .status(OrderStatus.PENDING)
            .totalAmount(10000L)
            .orderDate(LocalDate.now())
            .build();
    }

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status) {
        return OrderJpaEntity.builder()
            .customerId(customerId)
            .status(status)
            .totalAmount(10000L)
            .orderDate(LocalDate.now())
            .build();
    }

    private OrderJpaEntity createOrder(LocalDate orderDate) {
        return OrderJpaEntity.builder()
            .customerId(1L)
            .status(OrderStatus.PENDING)
            .totalAmount(10000L)
            .orderDate(orderDate)
            .build();
    }

    private OrderJpaEntity createOrder(Long customerId, OrderStatus status, LocalDate orderDate) {
        return OrderJpaEntity.builder()
            .customerId(customerId)
            .status(status)
            .totalAmount(10000L)
            .orderDate(orderDate)
            .build();
    }
}
```

---

## 3️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행
./gradlew test

# QueryDSL Repository 테스트만 실행
./gradlew test --tests "*QueryDslRepositoryTest"

# 특정 테스트 메서드만 실행
./gradlew test --tests "*QueryDslRepositoryTest.findByCriteria_ByFilterId_ReturnsMatchingOrders"
```

### IDE 실행

- IntelliJ IDEA: `OrderQueryDslRepositoryTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 4️⃣ 테스트 작성 체크리스트

QueryDSL Repository 테스트 작성 시:

### findById 테스트
- [ ] 존재하는 ID로 조회 성공
- [ ] 존재하지 않는 ID로 조회 시 Empty 반환

### existsById 테스트
- [ ] 존재하는 ID는 true 반환
- [ ] 존재하지 않는 ID는 false 반환

### findByCriteria 테스트 (동적 조건)
- [ ] `filterId`로 조회
- [ ] `status`로 조회
- [ ] `startDate`와 `endDate` 범위 조회
- [ ] 복합 조건 조합 (3개 이상)
- [ ] 조건 없음 (전체 조회)

### findByCriteria 테스트 (페이징)
- [ ] Offset 페이징 (첫 페이지)
- [ ] Offset 페이징 (중간 페이지)
- [ ] Cursor 페이징 (다음 페이지)
- [ ] Cursor 페이징 (size+1 조회로 hasNext 확인)

### findByCriteria 테스트 (정렬)
- [ ] 정렬 ASC (id)
- [ ] 정렬 DESC (id)
- [ ] 정렬 ASC (기타 필드)

### countByCriteria 테스트
- [ ] `filterId`로 카운트
- [ ] `status`로 카운트
- [ ] 복합 조건 카운트
- [ ] 조건 없음 (전체 카운트)
- [ ] 매칭되지 않는 조건은 0 반환

---

## 5️⃣ 참고 문서

- [querydsl-repository-guide.md](./querydsl-repository-guide.md) - QueryDSL Repository 컨벤션
- [querydsl-repository-archunit.md](./querydsl-repository-archunit.md) - ArchUnit 규칙
- [query-adapter-integration-testing.md](../adapter/query/query-adapter-integration-testing.md) - Query Adapter 통합 테스트

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
