# Repository 통합 테스트 가이드

> **목적**: TestContainers + MySQL 기반 Repository 통합 테스트 작성 규칙

---

## 1. 개요

### 통합 테스트 범위

```
┌─────────────────────────────────────────────────────────────────┐
│  Repository 통합 테스트 범위                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   Adapter    │───▶│  Repository  │───▶│    MySQL     │       │
│  │  (테스트)     │    │   (테스트)    │    │ (Container)  │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                                                                  │
│  검증 대상:                                                      │
│  • JpaRepository CRUD 동작                                       │
│  • QueryDslRepository 복잡 쿼리                                  │
│  • Adapter → Repository → DB 전체 플로우                         │
│  • Flyway 마이그레이션 적용                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 지원 클래스

### 2.1 RepositoryTestSupport (기반 클래스)

모든 Repository 통합 테스트는 이 클래스를 상속받습니다.

```java
/**
 * Repository 통합 테스트 지원 추상 클래스
 *
 * <p>모든 Repository 통합 테스트는 이 클래스를 상속받아 작성합니다.
 *
 * <p>제공 기능:
 * <ul>
 *   <li>TestContainers MySQL 자동 설정</li>
 *   <li>EntityManager 자동 주입</li>
 *   <li>트랜잭션 자동 롤백</li>
 *   <li>테스트 유틸리티 메서드</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class RepositoryTestSupport {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @Autowired
    protected EntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    /**
     * 영속성 컨텍스트 플러시 및 클리어
     *
     * <p>INSERT 후 조회 테스트 시 사용합니다.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 엔티티 영속화 후 플러시
     *
     * @param entity 영속화할 엔티티
     * @param <T> 엔티티 타입
     * @return 영속화된 엔티티
     */
    protected <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    /**
     * 여러 엔티티 영속화
     *
     * @param entities 영속화할 엔티티 목록
     */
    protected void persistAll(Object... entities) {
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }
}
```

### 2.2 application-test.yml 설정

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    # TestContainers가 동적으로 설정
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway가 스키마 관리
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## 3. JpaRepository 테스트

### 3.1 기본 CRUD 테스트

```java
@DisplayName("OrderJpaRepository 통합 테스트")
class OrderJpaRepositoryTest extends RepositoryTestSupport {

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
                1L,                    // customerId
                OrderStatus.PENDING,
                Money.of(10000)
            );

            // When
            OrderJpaEntity saved = orderJpaRepository.save(order);
            flushAndClear();

            // Then
            OrderJpaEntity found = orderJpaRepository.findById(saved.getId())
                .orElseThrow();

            assertThat(found.getCustomerId()).isEqualTo(1L);
            assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(found.getTotalAmount()).isEqualTo(Money.of(10000));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @Sql("/sql/order/orders-test-data.sql")
        @DisplayName("성공 - 주문 조회")
        void success() {
            // Given
            Long orderId = 100L;

            // When
            Optional<OrderJpaEntity> result = orderJpaRepository.findById(orderId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void notFound() {
            // Given
            Long nonExistentId = 999999L;

            // When
            Optional<OrderJpaEntity> result = orderJpaRepository.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        @Sql("/sql/order/orders-test-data.sql")
        @DisplayName("성공 - 주문 삭제")
        void success() {
            // Given
            Long orderId = 100L;
            assertThat(orderJpaRepository.existsById(orderId)).isTrue();

            // When
            orderJpaRepository.deleteById(orderId);
            flushAndClear();

            // Then
            assertThat(orderJpaRepository.existsById(orderId)).isFalse();
        }
    }
}
```

### 3.2 커스텀 쿼리 메서드 테스트

```java
@Nested
@DisplayName("findByCustomerIdAndStatus()")
class FindByCustomerIdAndStatus {

    @Test
    @Sql("/sql/order/orders-test-data.sql")
    @DisplayName("성공 - 고객 ID와 상태로 주문 조회")
    void success() {
        // Given
        Long customerId = 1L;
        OrderStatus status = OrderStatus.PENDING;

        // When
        List<OrderJpaEntity> orders = orderJpaRepository
            .findByCustomerIdAndStatus(customerId, status);

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders).allMatch(o -> o.getCustomerId().equals(customerId));
        assertThat(orders).allMatch(o -> o.getStatus() == status);
    }
}
```

---

## 4. QueryDslRepository 테스트

### 4.1 복잡 쿼리 테스트

```java
@DisplayName("OrderQueryDslRepository 통합 테스트")
class OrderQueryDslRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private OrderQueryDslRepository orderQueryDslRepository;

    @Nested
    @DisplayName("findOrdersWithCondition()")
    class FindOrdersWithCondition {

        @Test
        @Sql("/sql/order/orders-bulk-data.sql")
        @DisplayName("성공 - 다중 조건 검색")
        void success() {
            // Given
            OrderSearchCondition condition = new OrderSearchCondition(
                1L,                           // customerId
                OrderStatus.PENDING,          // status
                LocalDate.of(2024, 1, 1),    // startDate
                LocalDate.of(2024, 12, 31),  // endDate
                Money.of(5000),              // minAmount
                Money.of(50000)              // maxAmount
            );
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

            // When
            Slice<OrderSummaryDto> result = orderQueryDslRepository
                .findOrdersWithCondition(condition, pageable);

            // Then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).allMatch(dto ->
                dto.customerId().equals(1L) &&
                dto.status() == OrderStatus.PENDING
            );
        }

        @Test
        @Sql("/sql/order/orders-bulk-data.sql")
        @DisplayName("성공 - 빈 조건으로 전체 조회")
        void emptyCondition() {
            // Given
            OrderSearchCondition condition = OrderSearchCondition.empty();
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Slice<OrderSummaryDto> result = orderQueryDslRepository
                .findOrdersWithCondition(condition, pageable);

            // Then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("DTO Projection 검증")
    class DtoProjection {

        @Test
        @Sql("/sql/order/orders-with-items.sql")
        @DisplayName("성공 - 주문 상세 DTO 조회")
        void orderDetailDto() {
            // Given
            Long orderId = 100L;

            // When
            Optional<OrderDetailDto> result = orderQueryDslRepository
                .findOrderDetailById(orderId);

            // Then
            assertThat(result).isPresent();
            OrderDetailDto dto = result.get();
            assertThat(dto.orderId()).isEqualTo(orderId);
            assertThat(dto.items()).isNotEmpty();
            assertThat(dto.totalItemCount()).isGreaterThan(0);
        }
    }
}
```

### 4.2 N+1 문제 검증

```java
@Nested
@DisplayName("N+1 문제 검증")
class NPlusOneCheck {

    @Test
    @Sql("/sql/order/orders-with-items.sql")
    @DisplayName("성공 - N+1 없이 주문과 아이템 조회")
    void noNPlusOne() {
        // Given
        Long customerId = 1L;

        // When - 쿼리 수 측정
        long queryCountBefore = getQueryCount();

        List<OrderWithItemsDto> orders = orderQueryDslRepository
            .findOrdersWithItemsByCustomerId(customerId);

        long queryCountAfter = getQueryCount();

        // Then - 단일 쿼리로 조회 확인
        assertThat(queryCountAfter - queryCountBefore).isEqualTo(1);
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).items()).isNotEmpty();
    }

    private long getQueryCount() {
        // Hibernate 통계 또는 p6spy 활용
        return entityManager.unwrap(Session.class)
            .getSessionFactory()
            .getStatistics()
            .getQueryExecutionCount();
    }
}
```

---

## 5. Adapter 테스트

### 5.1 CommandAdapter 테스트

```java
@DisplayName("OrderCommandAdapter 통합 테스트")
class OrderCommandAdapterTest extends RepositoryTestSupport {

    @Autowired
    private OrderCommandAdapter orderCommandAdapter;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Nested
    @DisplayName("persist()")
    class Persist {

        @Test
        @DisplayName("성공 - 신규 주문 저장")
        void createNew() {
            // Given
            Order order = Order.create(
                CustomerId.of(1L),
                List.of(OrderLineItem.of(ProductId.of(100L), 2, Money.of(5000)))
            );

            // When
            orderCommandAdapter.persist(order);
            flushAndClear();

            // Then
            List<OrderJpaEntity> saved = orderJpaRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getCustomerId()).isEqualTo(1L);
        }

        @Test
        @Sql("/sql/order/orders-test-data.sql")
        @DisplayName("성공 - 기존 주문 수정")
        void updateExisting() {
            // Given
            OrderJpaEntity existing = orderJpaRepository.findById(100L).orElseThrow();
            Order order = OrderJpaEntityMapper.toDomain(existing);
            order.confirm();  // 상태 변경

            // When
            orderCommandAdapter.persist(order);
            flushAndClear();

            // Then
            OrderJpaEntity updated = orderJpaRepository.findById(100L).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }
}
```

### 5.2 QueryAdapter 테스트

```java
@DisplayName("OrderQueryAdapter 통합 테스트")
class OrderQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private OrderQueryAdapter orderQueryAdapter;

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @Sql("/sql/order/orders-test-data.sql")
        @DisplayName("성공 - 주문 도메인 조회")
        void success() {
            // Given
            OrderId orderId = OrderId.of(100L);

            // When
            Optional<Order> result = orderQueryAdapter.findById(orderId);

            // Then
            assertThat(result).isPresent();
            Order order = result.get();
            assertThat(order.getId()).isEqualTo(orderId);
        }
    }

    @Nested
    @DisplayName("findSummaries()")
    class FindSummaries {

        @Test
        @Sql("/sql/order/orders-bulk-data.sql")
        @DisplayName("성공 - 주문 요약 목록 조회")
        void success() {
            // Given
            OrderSearchCriteria criteria = OrderSearchCriteria.builder()
                .customerId(CustomerId.of(1L))
                .status(OrderStatus.PENDING)
                .build();
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Slice<OrderSummary> result = orderQueryAdapter.findSummaries(criteria, pageable);

            // Then
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).allMatch(s ->
                s.customerId().value().equals(1L)
            );
        }
    }
}
```

---

## 6. 테스트 클래스 템플릿

### 6.1 Repository 통합 테스트 템플릿

```java
package com.ryuqq.adapter.out.persistence.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.ryuqq.adapter.out.persistence.common.RepositoryTestSupport;
import com.ryuqq.adapter.out.persistence.order.entity.OrderJpaEntity;

/**
 * OrderJpaRepository 통합 테스트
 *
 * <p><strong>테스트 범위:</strong>
 * <ul>
 *   <li>기본 CRUD 동작 검증</li>
 *   <li>커스텀 쿼리 메서드 검증</li>
 *   <li>데이터 정합성 검증</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("OrderJpaRepository 통합 테스트")
class OrderJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("성공 - 엔티티 저장")
        void success() {
            // Given

            // When

            // Then
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @Sql("/sql/order/orders-test-data.sql")
        @DisplayName("성공 - ID로 조회")
        void success() {
            // Given

            // When

            // Then
        }
    }
}
```

---

## 7. 체크리스트

### 테스트 작성 전

- [ ] `RepositoryTestSupport` 상속
- [ ] TestContainers MySQL 컨테이너 확인
- [ ] `application-test.yml` 설정 확인
- [ ] Flyway 마이그레이션 파일 준비

### 테스트 메서드 작성

- [ ] `@DisplayName` 작성
- [ ] `@Sql` 또는 TestFixtures로 데이터 준비
- [ ] Given-When-Then 구조 준수
- [ ] `flushAndClear()` 적절히 사용
- [ ] 쿼리 결과 정확성 검증

### QueryDSL 테스트

- [ ] DTO Projection 필드 매핑 검증
- [ ] 동적 쿼리 조건 테스트
- [ ] 페이징/정렬 동작 검증
- [ ] N+1 문제 없음 확인

---

## 8. 참고 문서

- [MySQL 테스트 가이드](./01_mysql-testing-guide.md)
- [Repository Slice 테스트](./03_repository-slice-test.md)
- [Mapper 단위 테스트](./04_mapper-unit-test.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
