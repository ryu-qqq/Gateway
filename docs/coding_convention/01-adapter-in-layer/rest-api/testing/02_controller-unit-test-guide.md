# REST API Layer 단위 테스트 가이드

> **목적**: Controller, Mapper, DTO의 단위 테스트 작성 규칙 (선택적)

---

## 1. 개요

### 단위 테스트 vs 통합 테스트

| 구분 | 단위 테스트 | 통합 테스트 |
|------|------------|------------|
| **필수 여부** | 🔶 선택적 | ✅ 필수 |
| **범위** | 단일 클래스 | 전체 레이어 |
| **속도** | 빠름 (ms) | 느림 (초) |
| **신뢰도** | 중간 | 높음 |
| **용도** | 복잡한 로직 검증 | 전체 흐름 검증 |

### 언제 단위 테스트를 작성하는가?

```
┌─────────────────────────────────────────────────────────────┐
│  단위 테스트 작성 기준                                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ 작성 권장                                                │
│  ├─ Mapper: 복잡한 변환 로직이 있는 경우                      │
│  ├─ DTO: 커스텀 Validation 로직이 있는 경우                   │
│  └─ Utility: 순수 함수 형태의 유틸리티                        │
│                                                             │
│  ❌ 작성 불필요                                               │
│  ├─ Controller: 통합 테스트로 충분                            │
│  ├─ 단순 Mapper: from() 메서드만 있는 경우                    │
│  └─ 단순 DTO: Record 기본 기능만 사용                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Mapper 단위 테스트

### 2.1 테스트 대상

Mapper는 다음 경우에만 단위 테스트 작성:

| 복잡도 | 예시 | 단위 테스트 |
|--------|------|------------|
| 단순 | `from(dto) → entity` | ❌ 불필요 |
| 중간 | 조건부 변환, null 처리 | 🔶 선택 |
| 복잡 | 여러 DTO 조합, 계산 로직 | ✅ 권장 |

### 2.2 단순 Mapper (테스트 불필요)

```java
// 단순 Mapper - 통합 테스트로 충분
@Component
public class OrderApiMapper {

    public PlaceOrderCommand toCommand(PlaceOrderApiRequest request) {
        return new PlaceOrderCommand(
            request.customerId(),
            request.productId(),
            request.quantity()
        );
    }

    public OrderApiResponse toResponse(OrderResult result) {
        return OrderApiResponse.from(result);
    }
}
```

### 2.3 복잡한 Mapper (테스트 권장)

```java
// 복잡한 Mapper - 단위 테스트 권장
@Component
public class OrderSummaryApiMapper {

    public OrderSummaryApiResponse toSummaryResponse(
            OrderResult order,
            List<OrderItemResult> items,
            CustomerResult customer
    ) {
        long totalAmount = items.stream()
            .mapToLong(item -> item.price() * item.quantity())
            .sum();

        long discountAmount = calculateDiscount(customer.grade(), totalAmount);
        long finalAmount = totalAmount - discountAmount;

        return new OrderSummaryApiResponse(
            order.orderId(),
            customer.name(),
            items.size(),
            totalAmount,
            discountAmount,
            finalAmount,
            formatDeliveryDate(order.orderDate())
        );
    }

    private long calculateDiscount(String grade, long amount) {
        return switch (grade) {
            case "VIP" -> (long) (amount * 0.1);
            case "GOLD" -> (long) (amount * 0.05);
            default -> 0L;
        };
    }

    private String formatDeliveryDate(LocalDate orderDate) {
        return orderDate.plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
```

### 2.4 Mapper 단위 테스트 예시

```java
package com.ryuqq.adapter.in.rest.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OrderSummaryApiMapper 단위 테스트
 *
 * <p><strong>테스트 범위:</strong>
 * <ul>
 *   <li>할인 계산 로직 검증</li>
 *   <li>배송일 포맷팅 검증</li>
 *   <li>총액 계산 검증</li>
 * </ul>
 */
@DisplayName("OrderSummaryApiMapper 단위 테스트")
class OrderSummaryApiMapperTest {

    private OrderSummaryApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderSummaryApiMapper();
    }

    @Nested
    @DisplayName("할인 계산")
    class DiscountCalculation {

        @Test
        @DisplayName("VIP 고객 - 10% 할인")
        void vipCustomer_10PercentDiscount() {
            // Given
            OrderResult order = createOrder(1L, LocalDate.of(2024, 1, 1));
            List<OrderItemResult> items = List.of(
                createItem(10000L, 2),  // 20,000
                createItem(5000L, 4)    // 20,000
            );
            CustomerResult customer = createCustomer("VIP");

            // When
            OrderSummaryApiResponse response = mapper.toSummaryResponse(order, items, customer);

            // Then
            assertThat(response.totalAmount()).isEqualTo(40000L);
            assertThat(response.discountAmount()).isEqualTo(4000L);  // 10%
            assertThat(response.finalAmount()).isEqualTo(36000L);
        }

        @Test
        @DisplayName("GOLD 고객 - 5% 할인")
        void goldCustomer_5PercentDiscount() {
            // Given
            OrderResult order = createOrder(1L, LocalDate.of(2024, 1, 1));
            List<OrderItemResult> items = List.of(createItem(10000L, 1));
            CustomerResult customer = createCustomer("GOLD");

            // When
            OrderSummaryApiResponse response = mapper.toSummaryResponse(order, items, customer);

            // Then
            assertThat(response.discountAmount()).isEqualTo(500L);  // 5%
        }

        @Test
        @DisplayName("일반 고객 - 할인 없음")
        void normalCustomer_NoDiscount() {
            // Given
            OrderResult order = createOrder(1L, LocalDate.of(2024, 1, 1));
            List<OrderItemResult> items = List.of(createItem(10000L, 1));
            CustomerResult customer = createCustomer("NORMAL");

            // When
            OrderSummaryApiResponse response = mapper.toSummaryResponse(order, items, customer);

            // Then
            assertThat(response.discountAmount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("배송일 계산")
    class DeliveryDateCalculation {

        @Test
        @DisplayName("주문일 + 3일 = 배송 예정일")
        void deliveryDate_OrderDatePlus3Days() {
            // Given
            LocalDate orderDate = LocalDate.of(2024, 1, 15);
            OrderResult order = createOrder(1L, orderDate);
            List<OrderItemResult> items = List.of(createItem(10000L, 1));
            CustomerResult customer = createCustomer("NORMAL");

            // When
            OrderSummaryApiResponse response = mapper.toSummaryResponse(order, items, customer);

            // Then
            assertThat(response.deliveryDate()).isEqualTo("2024-01-18");
        }
    }

    // Test Fixtures
    private OrderResult createOrder(Long orderId, LocalDate orderDate) {
        return new OrderResult(orderId, "PENDING", orderDate);
    }

    private OrderItemResult createItem(Long price, int quantity) {
        return new OrderItemResult(1L, price, quantity);
    }

    private CustomerResult createCustomer(String grade) {
        return new CustomerResult(1L, "Test Customer", grade);
    }
}
```

---

## 3. DTO Validation 단위 테스트

### 3.1 Bean Validation 테스트

```java
package com.ryuqq.adapter.in.rest.order.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PlaceOrderApiRequest Validation 테스트
 */
@DisplayName("PlaceOrderApiRequest Validation 테스트")
class PlaceOrderApiRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("customerId 검증")
    class CustomerIdValidation {

        @Test
        @DisplayName("null일 경우 검증 실패")
        void null_ShouldFail() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(null, 1L, 10);

            // When
            Set<ConstraintViolation<PlaceOrderApiRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("고객 ID는 필수입니다");
        }

        @Test
        @DisplayName("양수일 경우 검증 성공")
        void positive_ShouldPass() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 1L, 10);

            // When
            Set<ConstraintViolation<PlaceOrderApiRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("quantity 검증")
    class QuantityValidation {

        @Test
        @DisplayName("0일 경우 검증 실패")
        void zero_ShouldFail() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 1L, 0);

            // When
            Set<ConstraintViolation<PlaceOrderApiRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("수량은 1 이상이어야 합니다");
        }

        @Test
        @DisplayName("음수일 경우 검증 실패")
        void negative_ShouldFail() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 1L, -5);

            // When
            Set<ConstraintViolation<PlaceOrderApiRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("최대 수량 초과 시 검증 실패")
        void overMaxQuantity_ShouldFail() {
            // Given
            PlaceOrderApiRequest request = new PlaceOrderApiRequest(1L, 1L, 1001);

            // When
            Set<ConstraintViolation<PlaceOrderApiRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("수량은 1000개를 초과할 수 없습니다");
        }
    }
}
```

### 3.2 커스텀 Validator 테스트

```java
/**
 * 커스텀 Validator가 있는 경우 단위 테스트 작성
 */
@DisplayName("DateRangeApiRequest Validation 테스트")
class DateRangeApiRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 검증 실패")
    void startDateAfterEndDate_ShouldFail() {
        // Given
        DateRangeApiRequest request = new DateRangeApiRequest(
            LocalDate.of(2024, 12, 31),  // startDate
            LocalDate.of(2024, 1, 1)     // endDate (startDate보다 이전)
        );

        // When
        Set<ConstraintViolation<DateRangeApiRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("시작일은 종료일보다 이전이어야 합니다");
    }

    @Test
    @DisplayName("날짜 범위가 90일을 초과하면 검증 실패")
    void dateRangeOver90Days_ShouldFail() {
        // Given
        DateRangeApiRequest request = new DateRangeApiRequest(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 5, 1)  // 121일
        );

        // When
        Set<ConstraintViolation<DateRangeApiRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("조회 기간은 90일을 초과할 수 없습니다");
    }
}
```

---

## 4. Controller 단위 테스트 (권장하지 않음)

### 4.1 왜 권장하지 않는가?

```
┌─────────────────────────────────────────────────────────────┐
│  Controller 단위 테스트를 권장하지 않는 이유                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. MockMvc 사용 필요 → 실제 HTTP 동작 미검증                 │
│  2. @MockBean 남발 → 실제 통합 동작 미검증                    │
│  3. 통합 테스트로 모든 것 검증 가능                            │
│  4. 테스트 중복 → 유지보수 비용 증가                          │
│                                                             │
│  결론: 통합 테스트만으로 충분, Controller 단위 테스트 불필요    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 예외: Controller 단위 테스트가 필요한 경우

극히 드물지만, 다음 경우에만 고려:

| 상황 | 설명 |
|------|------|
| **외부 API Mock** | 외부 서비스 호출이 필수인 경우 WireMock 활용 |
| **성능 테스트** | 빠른 반복 테스트가 필요한 경우 |

---

## 5. 테스트 클래스 명명 규칙

| 대상 | 패턴 | 예시 |
|------|------|------|
| Mapper | `*MapperTest` | `OrderApiMapperTest` |
| DTO Validation | `*ValidationTest` | `PlaceOrderApiRequestValidationTest` |
| Utility | `*UtilTest` | `DateFormatUtilTest` |

---

## 6. 체크리스트

### Mapper 단위 테스트

- [ ] 복잡한 로직이 있는 Mapper만 테스트
- [ ] 순수 함수처럼 테스트 (의존성 없음)
- [ ] Edge case 검증 (null, 빈 리스트, 경계값)
- [ ] Given-When-Then 구조

### DTO Validation 테스트

- [ ] Jakarta Validation API 사용
- [ ] 모든 제약 조건 검증
- [ ] 에러 메시지 검증
- [ ] 커스텀 Validator가 있으면 반드시 테스트

### 공통

- [ ] `@DisplayName` 필수
- [ ] Nested 클래스로 그룹핑
- [ ] Test Fixtures 활용

---

## 7. 참고 문서

- [REST API 통합 테스트 가이드](./01_rest-api-testing-guide.md)
- [Test Fixtures 가이드](../../../05-testing/test-fixtures/01_test-fixtures-guide.md)
- [Command DTO 가이드](../dto/command-dto-guide.md)
- [Mapper 가이드](../mapper/mapper-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
