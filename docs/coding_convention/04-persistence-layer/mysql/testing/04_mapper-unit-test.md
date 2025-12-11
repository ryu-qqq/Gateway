# Mapper 단위 테스트 가이드

> **목적**: JpaEntityMapper의 Domain ↔ Entity 변환 로직 검증

---

## 1. 개요

### Mapper 테스트 범위

```
┌─────────────────────────────────────────────────────────────────┐
│  Mapper 단위 테스트 범위                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐           ┌──────────────┐                    │
│  │   Domain     │ ◀──────▶  │   Entity     │                    │
│  │  (Order)     │   Mapper  │ (OrderJpa)   │                    │
│  └──────────────┘           └──────────────┘                    │
│                                                                  │
│  검증 대상:                                                      │
│  • toDomain() - Entity → Domain 변환                            │
│  • toEntity() - Domain → Entity 변환                            │
│  • 필드 매핑 정확성                                              │
│  • null 처리                                                     │
│  • VO 변환 (Money, OrderId 등)                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 왜 Mapper 테스트가 중요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│  Mapper 버그 영향도                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ❌ 버그 발생 시:                                                │
│     • 데이터 불일치 (DB ↔ 애플리케이션)                          │
│     • 잘못된 비즈니스 로직 실행                                  │
│     • 디버깅 어려움 (레이어 경계에서 발생)                        │
│                                                                  │
│  ✅ 테스트 효과:                                                 │
│     • Spring Context 불필요 → 빠른 실행                         │
│     • 변환 로직 명시적 검증                                      │
│     • 리팩토링 안전망                                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 지원 클래스

### 2.1 MapperTestSupport (선택적)

Mapper 테스트는 Spring Context 없이 순수 JUnit으로 작성하므로,
기반 클래스가 필수는 아닙니다. 필요시 공통 유틸리티만 제공합니다.

```java
/**
 * Mapper 단위 테스트 지원 클래스
 *
 * <p>Mapper 테스트를 위한 공통 유틸리티를 제공합니다.
 * Spring Context 없이 순수 JUnit 5로 테스트합니다.
 *
 * <p><strong>특징:</strong>
 * <ul>
 *   <li>Spring Context 불필요</li>
 *   <li>가장 빠른 실행 속도</li>
 *   <li>변환 로직만 집중 검증</li>
 * </ul>
 */
public abstract class MapperTestSupport {

    /**
     * 두 객체의 필드 값이 동일한지 비교
     *
     * @param expected 기대값 객체
     * @param actual 실제값 객체
     * @param fieldNames 비교할 필드 이름들
     */
    protected void assertFieldsMatch(Object expected, Object actual, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object expectedValue = getFieldValue(expected, fieldName);
            Object actualValue = getFieldValue(actual, fieldName);
            assertThat(actualValue)
                .as("Field '%s' should match", fieldName)
                .isEqualTo(expectedValue);
        }
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field: " + fieldName, e);
        }
    }
}
```

---

## 3. toDomain() 테스트

### 3.1 기본 변환 테스트

```java
@DisplayName("OrderJpaEntityMapper 단위 테스트")
class OrderJpaEntityMapperTest {

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("성공 - Entity를 Domain으로 변환")
        void success() {
            // Given
            OrderJpaEntity entity = createTestEntity();

            // When
            Order domain = OrderJpaEntityMapper.toDomain(entity);

            // Then
            assertThat(domain.getId().value()).isEqualTo(entity.getId());
            assertThat(domain.getCustomerId().value()).isEqualTo(entity.getCustomerId());
            assertThat(domain.getStatus()).isEqualTo(entity.getStatus());
            assertThat(domain.getTotalAmount()).isEqualTo(entity.getTotalAmount());
            assertThat(domain.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        }

        @Test
        @DisplayName("성공 - null Entity는 null 반환")
        void nullEntity() {
            // Given
            OrderJpaEntity entity = null;

            // When
            Order domain = OrderJpaEntityMapper.toDomain(entity);

            // Then
            assertThat(domain).isNull();
        }

        @Test
        @DisplayName("성공 - 주문 아이템 목록 변환")
        void withOrderItems() {
            // Given
            OrderJpaEntity entity = createTestEntityWithItems();

            // When
            Order domain = OrderJpaEntityMapper.toDomain(entity);

            // Then
            assertThat(domain.getItems()).hasSize(2);
            assertThat(domain.getItems().get(0).getProductId().value())
                .isEqualTo(entity.getItems().get(0).getProductId());
            assertThat(domain.getItems().get(0).getQuantity())
                .isEqualTo(entity.getItems().get(0).getQuantity());
        }

        private OrderJpaEntity createTestEntity() {
            return new OrderJpaEntity(
                100L,                          // id
                1L,                            // customerId
                OrderStatus.PENDING,           // status
                Money.of(50000),              // totalAmount
                LocalDateTime.of(2024, 1, 15, 10, 0),  // createdAt
                null                           // updatedAt
            );
        }

        private OrderJpaEntity createTestEntityWithItems() {
            OrderJpaEntity entity = createTestEntity();
            entity.addItem(new OrderItemJpaEntity(
                1L, entity, 100L, 2, Money.of(10000)
            ));
            entity.addItem(new OrderItemJpaEntity(
                2L, entity, 200L, 3, Money.of(15000)
            ));
            return entity;
        }
    }
}
```

### 3.2 VO 변환 테스트

```java
@Nested
@DisplayName("VO 변환 검증")
class VoConversion {

    @Test
    @DisplayName("성공 - Money VO 변환")
    void moneyConversion() {
        // Given
        OrderJpaEntity entity = new OrderJpaEntity(
            100L, 1L, OrderStatus.PENDING,
            Money.of(12345),  // 원시값 저장
            LocalDateTime.now(), null
        );

        // When
        Order domain = OrderJpaEntityMapper.toDomain(entity);

        // Then
        assertThat(domain.getTotalAmount()).isEqualTo(Money.of(12345));
        assertThat(domain.getTotalAmount().getValue()).isEqualTo(12345);
    }

    @Test
    @DisplayName("성공 - OrderId VO 변환")
    void orderIdConversion() {
        // Given
        Long rawId = 999L;
        OrderJpaEntity entity = new OrderJpaEntity(
            rawId, 1L, OrderStatus.PENDING,
            Money.of(10000), LocalDateTime.now(), null
        );

        // When
        Order domain = OrderJpaEntityMapper.toDomain(entity);

        // Then
        assertThat(domain.getId()).isEqualTo(OrderId.of(rawId));
        assertThat(domain.getId().value()).isEqualTo(rawId);
    }

    @Test
    @DisplayName("성공 - CustomerId VO 변환")
    void customerIdConversion() {
        // Given
        Long rawCustomerId = 123L;
        OrderJpaEntity entity = new OrderJpaEntity(
            100L, rawCustomerId, OrderStatus.PENDING,
            Money.of(10000), LocalDateTime.now(), null
        );

        // When
        Order domain = OrderJpaEntityMapper.toDomain(entity);

        // Then
        assertThat(domain.getCustomerId()).isEqualTo(CustomerId.of(rawCustomerId));
    }
}
```

---

## 4. toEntity() 테스트

### 4.1 신규 생성 테스트

```java
@Nested
@DisplayName("toEntity()")
class ToEntity {

    @Test
    @DisplayName("성공 - 신규 Domain을 Entity로 변환")
    void newDomain() {
        // Given
        Order domain = Order.create(
            CustomerId.of(1L),
            List.of(
                OrderLineItem.of(ProductId.of(100L), 2, Money.of(10000))
            )
        );

        // When
        OrderJpaEntity entity = OrderJpaEntityMapper.toEntity(domain);

        // Then
        assertThat(entity.getId()).isNull();  // 신규이므로 ID 없음
        assertThat(entity.getCustomerId()).isEqualTo(1L);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(entity.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("성공 - 기존 Domain을 Entity로 변환 (ID 유지)")
    void existingDomain() {
        // Given
        Order domain = Order.reconstitute(
            OrderId.of(100L),
            CustomerId.of(1L),
            OrderStatus.CONFIRMED,
            Money.of(50000),
            List.of(),
            LocalDateTime.of(2024, 1, 15, 10, 0),
            LocalDateTime.of(2024, 1, 16, 14, 30)
        );

        // When
        OrderJpaEntity entity = OrderJpaEntityMapper.toEntity(domain);

        // Then
        assertThat(entity.getId()).isEqualTo(100L);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("성공 - null Domain은 null 반환")
    void nullDomain() {
        // Given
        Order domain = null;

        // When
        OrderJpaEntity entity = OrderJpaEntityMapper.toEntity(domain);

        // Then
        assertThat(entity).isNull();
    }
}
```

### 4.2 양방향 변환 테스트

```java
@Nested
@DisplayName("양방향 변환 정합성")
class RoundTripConversion {

    @Test
    @DisplayName("성공 - Domain → Entity → Domain 변환 후 동일성 유지")
    void domainToEntityToDomain() {
        // Given
        Order original = Order.reconstitute(
            OrderId.of(100L),
            CustomerId.of(1L),
            OrderStatus.PENDING,
            Money.of(50000),
            List.of(
                OrderLineItem.of(ProductId.of(100L), 2, Money.of(25000))
            ),
            LocalDateTime.of(2024, 1, 15, 10, 0),
            null
        );

        // When
        OrderJpaEntity entity = OrderJpaEntityMapper.toEntity(original);
        Order converted = OrderJpaEntityMapper.toDomain(entity);

        // Then
        assertThat(converted.getId()).isEqualTo(original.getId());
        assertThat(converted.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(converted.getStatus()).isEqualTo(original.getStatus());
        assertThat(converted.getTotalAmount()).isEqualTo(original.getTotalAmount());
        assertThat(converted.getItems()).hasSize(original.getItems().size());
    }

    @Test
    @DisplayName("성공 - Entity → Domain → Entity 변환 후 동일성 유지")
    void entityToDomainToEntity() {
        // Given
        OrderJpaEntity original = new OrderJpaEntity(
            100L, 1L, OrderStatus.CONFIRMED,
            Money.of(75000),
            LocalDateTime.of(2024, 1, 15, 10, 0),
            LocalDateTime.of(2024, 1, 16, 14, 30)
        );

        // When
        Order domain = OrderJpaEntityMapper.toDomain(original);
        OrderJpaEntity converted = OrderJpaEntityMapper.toEntity(domain);

        // Then
        assertThat(converted.getId()).isEqualTo(original.getId());
        assertThat(converted.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(converted.getStatus()).isEqualTo(original.getStatus());
        assertThat(converted.getTotalAmount()).isEqualTo(original.getTotalAmount());
    }
}
```

---

## 5. 엣지 케이스 테스트

### 5.1 빈 컬렉션 처리

```java
@Nested
@DisplayName("엣지 케이스")
class EdgeCases {

    @Test
    @DisplayName("성공 - 빈 아이템 목록 처리")
    void emptyItemsList() {
        // Given
        Order domain = Order.reconstitute(
            OrderId.of(100L),
            CustomerId.of(1L),
            OrderStatus.PENDING,
            Money.of(0),
            List.of(),  // 빈 목록
            LocalDateTime.now(),
            null
        );

        // When
        OrderJpaEntity entity = OrderJpaEntityMapper.toEntity(domain);

        // Then
        assertThat(entity.getItems()).isEmpty();
    }

    @Test
    @DisplayName("성공 - null 아이템 목록은 빈 목록으로 처리")
    void nullItemsListTreatedAsEmpty() {
        // Given
        OrderJpaEntity entity = new OrderJpaEntity(
            100L, 1L, OrderStatus.PENDING,
            Money.of(10000), LocalDateTime.now(), null
        );
        // items가 null인 상태

        // When
        Order domain = OrderJpaEntityMapper.toDomain(entity);

        // Then
        assertThat(domain.getItems()).isNotNull();
        assertThat(domain.getItems()).isEmpty();
    }
}
```

### 5.2 특수 값 처리

```java
@Test
@DisplayName("성공 - 0원 금액 처리")
void zeroAmount() {
    // Given
    OrderJpaEntity entity = new OrderJpaEntity(
        100L, 1L, OrderStatus.PENDING,
        Money.of(0),  // 0원
        LocalDateTime.now(), null
    );

    // When
    Order domain = OrderJpaEntityMapper.toDomain(entity);

    // Then
    assertThat(domain.getTotalAmount()).isEqualTo(Money.ZERO);
}

@Test
@DisplayName("성공 - 최대값 금액 처리")
void maxAmount() {
    // Given
    long maxAmount = Long.MAX_VALUE;
    OrderJpaEntity entity = new OrderJpaEntity(
        100L, 1L, OrderStatus.PENDING,
        Money.of(maxAmount),
        LocalDateTime.now(), null
    );

    // When
    Order domain = OrderJpaEntityMapper.toDomain(entity);

    // Then
    assertThat(domain.getTotalAmount().getValue()).isEqualTo(maxAmount);
}
```

---

## 6. 테스트 클래스 템플릿

```java
package com.ryuqq.adapter.out.persistence.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ryuqq.adapter.out.persistence.order.entity.OrderJpaEntity;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.OrderStatus;
import com.ryuqq.domain.common.Money;

/**
 * OrderJpaEntityMapper 단위 테스트
 *
 * <p><strong>테스트 범위:</strong>
 * <ul>
 *   <li>toDomain() - Entity → Domain 변환</li>
 *   <li>toEntity() - Domain → Entity 변환</li>
 *   <li>양방향 변환 정합성</li>
 *   <li>엣지 케이스 처리</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("OrderJpaEntityMapper 단위 테스트")
class OrderJpaEntityMapperTest {

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {
        // toDomain 테스트 메서드들
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {
        // toEntity 테스트 메서드들
    }

    @Nested
    @DisplayName("양방향 변환")
    class RoundTrip {
        // 양방향 변환 테스트
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {
        // 엣지 케이스 테스트
    }
}
```

---

## 7. 체크리스트

### 테스트 작성 전

- [ ] Spring Context 불필요 확인
- [ ] Mapper 클래스의 모든 public 메서드 식별
- [ ] VO 변환 로직 파악

### 테스트 메서드 작성

- [ ] `@DisplayName` 작성
- [ ] Given-When-Then 구조 준수
- [ ] 모든 필드 매핑 검증
- [ ] null 처리 테스트
- [ ] 양방향 변환 정합성 테스트

### 엣지 케이스

- [ ] null 입력 처리
- [ ] 빈 컬렉션 처리
- [ ] 경계값 (0, MAX) 처리

---

## 8. 참고 문서

- [MySQL 테스트 가이드](./01_mysql-testing-guide.md)
- [Repository 통합 테스트](./02_repository-integration-test.md)
- [Mapper 가이드](../mapper/mapper-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
