# Query Factory Test Guide — **단위 테스트**

> QueryFactory는 **Query DTO → Domain Criteria 변환**을 담당합니다.
>
> **순수 변환 로직**이므로 **단위 테스트만** 작성합니다.

---

## 1) 테스트 전략

| 테스트 유형 | 목적 | 범위 |
|------------|------|------|
| **단위 테스트** | 변환 로직 검증 | QueryFactory만 |

### 테스트 포인트

| 항목 | 검증 내용 |
|------|----------|
| **Query → Criteria 변환** | 모든 필드 올바르게 매핑 |
| **VO 변환** | Query 값 → Domain VO |
| **null 처리** | 선택 필드 null 안전 |
| **기본값 처리** | 페이징 등 기본값 적용 |
| **Enum 변환** | String → Domain Enum |

---

## 2) 테스트 구조

```
application/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/application/{bc}/factory/query/
   │      └─ {Bc}QueryFactory.java
   └─ test/java/
      └─ com/ryuqq/application/{bc}/factory/query/
          └─ {Bc}QueryFactoryTest.java
```

---

## 3) 단위 테스트 예시

### 기본 테스트

```java
package com.ryuqq.application.order.factory.query;

import com.ryuqq.application.order.dto.query.OrderDetailQuery;
import com.ryuqq.application.order.dto.query.OrderSearchQuery;
import com.ryuqq.domain.order.criteria.OrderDetailCriteria;
import com.ryuqq.domain.order.criteria.OrderSearchCriteria;
import com.ryuqq.domain.order.vo.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderQueryFactory 단위 테스트")
class OrderQueryFactoryTest {

    private OrderQueryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OrderQueryFactory();
    }

    @Nested
    @DisplayName("createSearchCriteria 테스트")
    class CreateSearchCriteriaTest {

        @Test
        @DisplayName("OrderSearchQuery를 OrderSearchCriteria로 변환한다")
        void shouldConvertQueryToCriteria() {
            // given
            OrderSearchQuery query = new OrderSearchQuery(
                100L,           // customerId
                "CONFIRMED",    // status
                LocalDate.of(2025, 1, 1),   // fromDate
                LocalDate.of(2025, 12, 31), // toDate
                0,              // page
                20              // size
            );

            // when
            OrderSearchCriteria criteria = factory.createSearchCriteria(query);

            // then
            assertThat(criteria).isNotNull();
            assertThat(criteria.customerId().value()).isEqualTo(100L);
            assertThat(criteria.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(criteria.fromDate()).isEqualTo(LocalDate.of(2025, 1, 1));
            assertThat(criteria.toDate()).isEqualTo(LocalDate.of(2025, 12, 31));
            assertThat(criteria.page()).isZero();
            assertThat(criteria.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("customerId가 null이면 Criteria의 customerId도 null이다")
        void shouldHandleNullCustomerId() {
            // given
            OrderSearchQuery query = new OrderSearchQuery(
                null,           // customerId null
                "CONFIRMED",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                0,
                20
            );

            // when
            OrderSearchCriteria criteria = factory.createSearchCriteria(query);

            // then
            assertThat(criteria.customerId()).isNull();
        }

        @Test
        @DisplayName("status가 null이면 Criteria의 status도 null이다")
        void shouldHandleNullStatus() {
            // given
            OrderSearchQuery query = new OrderSearchQuery(
                100L,
                null,           // status null
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                0,
                20
            );

            // when
            OrderSearchCriteria criteria = factory.createSearchCriteria(query);

            // then
            assertThat(criteria.status()).isNull();
        }

        @Test
        @DisplayName("날짜 범위가 null이면 Criteria의 날짜도 null이다")
        void shouldHandleNullDateRange() {
            // given
            OrderSearchQuery query = new OrderSearchQuery(
                100L,
                "CONFIRMED",
                null,   // fromDate null
                null,   // toDate null
                0,
                20
            );

            // when
            OrderSearchCriteria criteria = factory.createSearchCriteria(query);

            // then
            assertThat(criteria.fromDate()).isNull();
            assertThat(criteria.toDate()).isNull();
        }
    }

    @Nested
    @DisplayName("createDetailCriteria 테스트")
    class CreateDetailCriteriaTest {

        @Test
        @DisplayName("OrderDetailQuery를 OrderDetailCriteria로 변환한다")
        void shouldConvertDetailQueryToCriteria() {
            // given
            OrderDetailQuery query = new OrderDetailQuery(
                999L,   // orderId
                true,   // includeItems
                true,   // includeShipping
                false   // includePayment
            );

            // when
            OrderDetailCriteria criteria = factory.createDetailCriteria(query);

            // then
            assertThat(criteria).isNotNull();
            assertThat(criteria.orderId().value()).isEqualTo(999L);
            assertThat(criteria.includeItems()).isTrue();
            assertThat(criteria.includeShipping()).isTrue();
            assertThat(criteria.includePayment()).isFalse();
        }

        @Test
        @DisplayName("ID만으로 기본 Criteria를 생성한다")
        void shouldCreateCriteriaByIdOnly() {
            // given
            Long orderId = 999L;

            // when
            OrderDetailCriteria criteria = factory.createByIdCriteria(orderId);

            // then
            assertThat(criteria.orderId().value()).isEqualTo(999L);
            assertThat(criteria.includeItems()).isTrue();
            assertThat(criteria.includeShipping()).isTrue();
            assertThat(criteria.includePayment()).isTrue();
        }
    }

    @Nested
    @DisplayName("복잡한 변환 테스트")
    class ComplexConversionTest {

        @Test
        @DisplayName("여러 상태 목록을 변환한다")
        void shouldConvertMultipleStatuses() {
            // given
            OrderAnalyticsQuery query = new OrderAnalyticsQuery(
                100L,
                List.of("CONFIRMED", "SHIPPED", "DELIVERED"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "MONTHLY"
            );

            // when
            OrderAnalyticsCriteria criteria = factory.createAnalyticsCriteria(query);

            // then
            assertThat(criteria.statuses()).hasSize(3);
            assertThat(criteria.statuses()).containsExactly(
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED
            );
        }

        @Test
        @DisplayName("정렬 조건을 변환한다")
        void shouldConvertSortConditions() {
            // given
            OrderListQuery query = new OrderListQuery(
                100L,
                "CONFIRMED",
                "createdAt",
                "desc",
                0,
                10
            );

            // when
            OrderListCriteria criteria = factory.createListCriteria(query);

            // then
            assertThat(criteria.sortBy()).isEqualTo(SortField.CREATED_AT);
            assertThat(criteria.sortDirection()).isEqualTo(SortDirection.DESC);
        }

        @Test
        @DisplayName("기본 정렬 방향은 ASC이다")
        void shouldDefaultToAscendingSort() {
            // given
            OrderListQuery query = new OrderListQuery(
                100L,
                "CONFIRMED",
                "createdAt",
                null,       // sortDirection null
                0,
                10
            );

            // when
            OrderListCriteria criteria = factory.createListCriteria(query);

            // then
            assertThat(criteria.sortDirection()).isEqualTo(SortDirection.ASC);
        }
    }
}
```

---

## 4) 테스트 체크리스트

### Query → Criteria 변환
- [ ] 모든 필드가 올바르게 매핑됨
- [ ] VO 생성 정확성 (CustomerId, OrderId 등)
- [ ] Enum 변환 정확성 (String → Domain Enum)
- [ ] 페이징 정보 변환

### null 처리
- [ ] 각 선택 필드 null 시 Criteria도 null
- [ ] null 안전 처리 (NPE 방지)

### 기본값 처리
- [ ] 페이징 기본값 적용
- [ ] 정렬 기본값 적용
- [ ] 날짜 범위 기본값 (필요 시)

### 복잡한 변환
- [ ] 컬렉션(List) 변환
- [ ] 중첩 객체 변환
- [ ] 조건부 변환

---

## 5) Do / Don't

### ✅ Good

```java
// ✅ Good: 순수 단위 테스트 (Mock 불필요)
@BeforeEach
void setUp() {
    factory = new OrderQueryFactory();  // 직접 생성
}

// ✅ Good: VO 값 검증
assertThat(criteria.customerId().value()).isEqualTo(100L);
assertThat(criteria.orderId().value()).isEqualTo(999L);

// ✅ Good: Enum 변환 검증
assertThat(criteria.status()).isEqualTo(OrderStatus.CONFIRMED);

// ✅ Good: null 안전 처리 검증
assertThat(criteria.customerId()).isNull();

// ✅ Good: 기본값 검증
assertThat(criteria.sortDirection()).isEqualTo(SortDirection.ASC);
```

### ❌ Bad

```java
// ❌ Bad: Mock 사용 (Factory는 순수 변환)
@Mock
private SomePort somePort;  // ❌ Factory는 Port 의존 안 함

// ❌ Bad: 비즈니스 로직 테스트 (Criteria 검증이 아님)
assertThat(criteria.isValidDateRange()).isTrue();  // ❌ Domain 책임

// ❌ Bad: 트랜잭션 테스트 (Factory는 트랜잭션 없음)
@Transactional
void testCreateCriteria() { ... }  // ❌

// ❌ Bad: 데이터베이스 접근 테스트
@SpringBootTest
class OrderQueryFactoryTest { ... }  // ❌ 단위 테스트로 충분

// ❌ Bad: 실제 조회 테스트 (Service 책임)
assertThat(repository.findBy(criteria)).hasSize(5);  // ❌
```

---

## 6) Fixture 활용

### TestFixtures 사용

```java
import com.ryuqq.fixture.application.OrderSearchQueryFixture;

@DisplayName("OrderQueryFactory 단위 테스트")
class OrderQueryFactoryTest {

    private OrderQueryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OrderQueryFactory();
    }

    @Test
    @DisplayName("기본 Query를 Criteria로 변환한다")
    void shouldConvertDefaultQuery() {
        // given
        OrderSearchQuery query = OrderSearchQueryFixture.create();

        // when
        OrderSearchCriteria criteria = factory.createSearchCriteria(query);

        // then
        assertThat(criteria).isNotNull();
    }

    @Test
    @DisplayName("커스텀 Query를 Criteria로 변환한다")
    void shouldConvertCustomQuery() {
        // given
        OrderSearchQuery query = OrderSearchQueryFixture.builder()
            .customerId(999L)
            .status("SHIPPED")
            .build();

        // when
        OrderSearchCriteria criteria = factory.createSearchCriteria(query);

        // then
        assertThat(criteria.customerId().value()).isEqualTo(999L);
        assertThat(criteria.status()).isEqualTo(OrderStatus.SHIPPED);
    }
}
```

### Fixture 정의

```java
// application/src/testFixtures/java/com/ryuqq/fixture/application/OrderSearchQueryFixture.java
public final class OrderSearchQueryFixture {

    private OrderSearchQueryFixture() {}

    public static OrderSearchQuery create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long customerId = 100L;
        private String status = "CONFIRMED";
        private LocalDate fromDate = LocalDate.now().minusMonths(1);
        private LocalDate toDate = LocalDate.now();
        private int page = 0;
        private int size = 20;

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public OrderSearchQuery build() {
            return new OrderSearchQuery(
                customerId, status, fromDate, toDate, page, size
            );
        }
    }
}
```

---

## 7) 관련 문서

- **[Query Factory Guide](./query-factory-guide.md)** - Factory 구현 가이드
- **[Query Factory ArchUnit](./query-factory-archunit.md)** - 아키텍처 규칙
- **[Command Factory Test Guide](../command/command-factory-test-guide.md)** - Command Factory 테스트
- **[Assembler Test Guide](../../assembler/assembler-test-guide.md)** - 비교: Domain → Response

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
