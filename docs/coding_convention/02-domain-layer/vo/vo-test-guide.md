# Value Object 테스트 가이드

> **목적**: Record 기반 Value Object의 단위 테스트 패턴과 원칙

---

## 1️⃣ 테스트 전략

### 테스트 대상
Record 기반 VO는 **Compact Constructor 검증**과 **비즈니스 메서드**를 테스트합니다:

```
✅ 테스트 항목:
1. Compact Constructor 검증 로직 (null 체크, 도메인 규칙)
2. 정적 팩토리 메서드 (of, forNew)
3. 비즈니스 메서드 (연산, 비교, 상태 변화, 검증)
4. equals/hashCode (Record 자동 생성이지만 확인)
```

### 테스트 범위
- ✅ **Pure Java 단위 테스트** (외부 의존성 제로)
- ✅ **@Tag 필수** (@Tag("unit"), @Tag("domain"), @Tag("vo"))
- ✅ **@Nested 클래스로 관심사 묶기** (2-5개 권장)
- ✅ **DisplayName으로 명확한 의도 표현**
- ❌ Mock 불필요 (VO는 의존성 없음)
- ❌ Spring Context 로딩 금지

---

## 2️⃣ 기본 템플릿

### 2-1) Simple VO 템플릿 (Money, Email, Price 등)

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {VoName} Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>Compact Constructor 검증 (도메인 규칙)</li>
 *   <li>정적 팩토리 메서드 (of)</li>
 *   <li>비즈니스 메서드 (연산, 비교)</li>
 *   <li>equals/hashCode (Record 자동 생성)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("{VoName} VO 단위 테스트")
class {VoName}Test {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("of() - 올바른 값으로 VO가 생성되어야 한다")
        void of_WithValidValue_ShouldCreateVO() {
            // Given
            Long amount = 1000L;

            // When
            Money money = Money.of(amount);

            // Then
            assertThat(money).isNotNull();
            assertThat(money.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("of() - null 값이면 예외가 발생해야 한다")
        void of_WithNullValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트")
    class CompactConstructorTests {

        @Test
        @DisplayName("음수 값이면 예외가 발생해야 한다")
        void of_WithNegativeValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Money.of(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상이어야 합니다");
        }

        @Test
        @DisplayName("0은 허용되어야 한다")
        void of_WithZeroValue_ShouldSucceed() {
            // When
            Money money = Money.of(0L);

            // Then
            assertThat(money.amount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트")
    class BusinessMethodTests {

        @Test
        @DisplayName("add() - 두 금액의 합이 반환되어야 한다")
        void add_ShouldReturnSumOfTwoAmounts() {
            // Given
            Money money1 = Money.of(1000L);
            Money money2 = Money.of(500L);

            // When
            Money result = money1.add(money2);

            // Then
            assertThat(result.amount()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("subtract() - 두 금액의 차이가 반환되어야 한다")
        void subtract_ShouldReturnDifferenceOfTwoAmounts() {
            // Given
            Money money1 = Money.of(1000L);
            Money money2 = Money.of(500L);

            // When
            Money result = money1.subtract(money2);

            // Then
            assertThat(result.amount()).isEqualTo(500L);
        }

        @Test
        @DisplayName("isGreaterThan() - 큰 금액이면 true를 반환해야 한다")
        void isGreaterThan_WithGreaterAmount_ShouldReturnTrue() {
            // Given
            Money money1 = Money.of(1000L);
            Money money2 = Money.of(500L);

            // When & Then
            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isGreaterThan(money1)).isFalse();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 값을 가진 VO는 equals()로 동등해야 한다")
        void equals_WithSameValue_ShouldBeEqual() {
            // Given
            Money money1 = Money.of(1000L);
            Money money2 = Money.of(1000L);

            // When & Then
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 VO는 equals()로 다르다")
        void equals_WithDifferentValue_ShouldNotBeEqual() {
            // Given
            Money money1 = Money.of(1000L);
            Money money2 = Money.of(2000L);

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
        }
    }

    @Nested
    @DisplayName("상수 테스트")
    class ConstantTests {

        @Test
        @DisplayName("ZERO 상수는 금액이 0인 Money여야 한다")
        void zero_ShouldBeMoneyWithZeroAmount() {
            // When & Then
            assertThat(Money.ZERO.amount()).isEqualTo(0L);
        }
    }
}
```

---

### 2-2) Long ID VO 템플릿 (Auto Increment - OrderId 등)

> **특징**: DB가 ID 생성 (Auto Increment)
> **forNew()**: null 반환
> **isNew()**: 필수 (null 여부 확인)

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {IdName} Long ID Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>forNew() null 생성 (Auto Increment, DB가 ID 생성)</li>
 *   <li>isNew() 체크</li>
 *   <li>양수 검증</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("{IdName} Long ID VO 단위 테스트")
class {IdName}Test {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("forNew() - null 값을 가진 ID VO가 생성되어야 한다")
        void forNew_ShouldCreateVOWithNullValue() {
            // When
            OrderId orderId = OrderId.forNew();

            // Then
            assertThat(orderId).isNotNull();
            assertThat(orderId.value()).isNull();
            assertThat(orderId.isNew()).isTrue();
        }

        @Test
        @DisplayName("of() - 올바른 값을 가진 ID VO가 생성되어야 한다")
        void of_WithValidValue_ShouldCreateVO() {
            // Given
            Long value = 100L;

            // When
            OrderId orderId = OrderId.of(value);

            // Then
            assertThat(orderId).isNotNull();
            assertThat(orderId.value()).isEqualTo(value);
            assertThat(orderId.isNew()).isFalse();
        }

        @Test
        @DisplayName("of() - null 값이면 예외가 발생해야 한다")
        void of_WithNullValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> OrderId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트")
    class CompactConstructorTests {

        @Test
        @DisplayName("0 이하 값이면 예외가 발생해야 한다")
        void of_WithNonPositiveValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> OrderId.of(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수여야 합니다");

            assertThatThrownBy(() -> OrderId.of(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("양수여야 합니다");
        }
    }

    @Nested
    @DisplayName("isNew() 메서드 테스트")
    class IsNewMethodTests {

        @Test
        @DisplayName("forNew()로 생성한 ID는 isNew()가 true")
        void isNew_WithNullValue_ShouldReturnTrue() {
            // Given
            OrderId orderId = OrderId.forNew();

            // When & Then
            assertThat(orderId.isNew()).isTrue();
        }

        @Test
        @DisplayName("of()로 생성한 ID는 isNew()가 false")
        void isNew_WithValue_ShouldReturnFalse() {
            // Given
            OrderId orderId = OrderId.of(100L);

            // When & Then
            assertThat(orderId.isNew()).isFalse();
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 값을 가진 ID VO는 equals()로 동등해야 한다")
        void equals_WithSameValue_ShouldBeEqual() {
            // Given
            OrderId orderId1 = OrderId.of(100L);
            OrderId orderId2 = OrderId.of(100L);

            // When & Then
            assertThat(orderId1).isEqualTo(orderId2);
            assertThat(orderId1.hashCode()).isEqualTo(orderId2.hashCode());
        }

        @Test
        @DisplayName("forNew()로 생성한 ID는 null 기준으로 동등해야 한다")
        void equals_WithBothNull_ShouldBeEqual() {
            // Given
            OrderId orderId1 = OrderId.forNew();
            OrderId orderId2 = OrderId.forNew();

            // When & Then
            assertThat(orderId1).isEqualTo(orderId2);
        }
    }
}
```

---

### 2-3) UUID ID VO 템플릿 (Application 생성 - UserId 등)

> **특징**: Application이 ID 생성 (UUIDv7)
> **forNew()**: UUID 반환 (항상 값 존재)
> **isNew()**: 불필요 (항상 값 존재)

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {IdName} UUID ID Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>forNew() UUIDv7 생성 (Application이 ID 생성)</li>
 *   <li>UUID 형식 검증</li>
 *   <li>null 금지 (항상 값 존재)</li>
 * </ul>
 *
 * <p>Note: UUID ID는 isNew() 메서드가 없습니다 (항상 값 존재)</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("{IdName} UUID ID VO 단위 테스트")
class {IdName}Test {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("forNew() - UUIDv7 값을 가진 ID VO가 생성되어야 한다")
        void forNew_ShouldCreateVOWithUUIDValue() {
            // When
            UserId userId = UserId.forNew();

            // Then
            assertThat(userId).isNotNull();
            assertThat(userId.value()).isNotNull();
            assertThat(userId.value()).isNotBlank();
            // UUID 형식 검증 (36자: 8-4-4-4-12)
            assertThat(userId.value()).matches(
                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
            );
        }

        @Test
        @DisplayName("forNew() - 매 호출마다 다른 UUID가 생성되어야 한다")
        void forNew_ShouldCreateDifferentUUIDsEachTime() {
            // When
            UserId userId1 = UserId.forNew();
            UserId userId2 = UserId.forNew();

            // Then
            assertThat(userId1.value()).isNotEqualTo(userId2.value());
        }

        @Test
        @DisplayName("of() - 올바른 UUID 값을 가진 ID VO가 생성되어야 한다")
        void of_WithValidValue_ShouldCreateVO() {
            // Given
            String value = "01234567-89ab-cdef-0123-456789abcdef";

            // When
            UserId userId = UserId.of(value);

            // Then
            assertThat(userId).isNotNull();
            assertThat(userId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("of() - null 값이면 예외가 발생해야 한다")
        void of_WithNullValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
        }

        @Test
        @DisplayName("of() - 빈 문자열이면 예외가 발생해야 한다")
        void of_WithBlankValue_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> UserId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("빈 문자열일 수 없습니다");

            assertThatThrownBy(() -> UserId.of("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("빈 문자열일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트")
    class CompactConstructorTests {

        @Test
        @DisplayName("UUID 형식이 아니면 예외가 발생해야 한다")
        void of_WithInvalidUUIDFormat_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> UserId.of("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID 형식이어야 합니다");

            assertThatThrownBy(() -> UserId.of("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID 형식이어야 합니다");
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 값을 가진 UUID ID VO는 equals()로 동등해야 한다")
        void equals_WithSameValue_ShouldBeEqual() {
            // Given
            String uuidValue = "01234567-89ab-cdef-0123-456789abcdef";
            UserId userId1 = UserId.of(uuidValue);
            UserId userId2 = UserId.of(uuidValue);

            // When & Then
            assertThat(userId1).isEqualTo(userId2);
            assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
        }

        @Test
        @DisplayName("forNew()로 생성한 UUID ID는 서로 다르다")
        void equals_WithDifferentForNew_ShouldNotBeEqual() {
            // Given
            UserId userId1 = UserId.forNew();
            UserId userId2 = UserId.forNew();

            // When & Then
            assertThat(userId1).isNotEqualTo(userId2);
        }
    }
}
```

**Long ID vs UUID ID 비교**:
| 항목 | Long ID (Auto Increment) | UUID ID (UUIDv7) |
|------|--------------------------|------------------|
| 필드 타입 | Long | String |
| forNew() 반환 | null | UUID 문자열 |
| isNew() 메서드 | 필수 (null 여부 확인) | 없음 (항상 값 존재) |
| ID 생성 주체 | DB (Auto Increment) | Application (UUIDv7) |
| 외부 노출 | 순차 증가 (보안 취약) | 랜덤 (보안 강함) |
| MySQL 저장 | BIGINT | BINARY(16) |

---

### 2-4) Enum VO 템플릿 (OrderStatus 등)

> **특징**: Enum 기반 상태/타입 표현
> **displayName()**: 필수 (화면 표시용)
> **Record가 아님**: of() 메서드 불필요

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderStatus Enum Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>displayName() 화면 표시명 검증</li>
 *   <li>상태 체크 메서드 검증 (isActive, isFinal 등)</li>
 *   <li>모든 Enum 값 검증 (누락 방지)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("OrderStatus Enum VO 단위 테스트")
class OrderStatusTest {

    @Nested
    @DisplayName("displayName() 테스트")
    class DisplayNameTests {

        @Test
        @DisplayName("PENDING은 '주문 대기'를 반환해야 한다")
        void pending_ShouldReturnCorrectDisplayName() {
            assertThat(OrderStatus.PENDING.displayName()).isEqualTo("주문 대기");
        }

        @Test
        @DisplayName("CONFIRMED는 '주문 확정'을 반환해야 한다")
        void confirmed_ShouldReturnCorrectDisplayName() {
            assertThat(OrderStatus.CONFIRMED.displayName()).isEqualTo("주문 확정");
        }

        @Test
        @DisplayName("SHIPPED는 '배송 중'을 반환해야 한다")
        void shipped_ShouldReturnCorrectDisplayName() {
            assertThat(OrderStatus.SHIPPED.displayName()).isEqualTo("배송 중");
        }

        @Test
        @DisplayName("DELIVERED는 '배송 완료'를 반환해야 한다")
        void delivered_ShouldReturnCorrectDisplayName() {
            assertThat(OrderStatus.DELIVERED.displayName()).isEqualTo("배송 완료");
        }

        @Test
        @DisplayName("CANCELLED는 '주문 취소'를 반환해야 한다")
        void cancelled_ShouldReturnCorrectDisplayName() {
            assertThat(OrderStatus.CANCELLED.displayName()).isEqualTo("주문 취소");
        }

        @Test
        @DisplayName("모든 상태는 displayName()이 null이 아니어야 한다")
        void allStatuses_ShouldHaveNonNullDisplayName() {
            for (OrderStatus status : OrderStatus.values()) {
                assertThat(status.displayName())
                    .as("Status %s should have non-null displayName", status)
                    .isNotNull()
                    .isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("상태 체크 메서드 테스트")
    class StatusCheckTests {

        @Test
        @DisplayName("isActive() - 활성 상태인 경우 true를 반환해야 한다")
        void isActive_WithActiveStatus_ShouldReturnTrue() {
            assertThat(OrderStatus.PENDING.isActive()).isTrue();
            assertThat(OrderStatus.CONFIRMED.isActive()).isTrue();
            assertThat(OrderStatus.SHIPPED.isActive()).isTrue();
        }

        @Test
        @DisplayName("isActive() - 종료 상태인 경우 false를 반환해야 한다")
        void isActive_WithFinalStatus_ShouldReturnFalse() {
            assertThat(OrderStatus.DELIVERED.isActive()).isFalse();
            assertThat(OrderStatus.CANCELLED.isActive()).isFalse();
        }

        @Test
        @DisplayName("isFinal() - 종료 상태인 경우 true를 반환해야 한다")
        void isFinal_WithFinalStatus_ShouldReturnTrue() {
            assertThat(OrderStatus.DELIVERED.isFinal()).isTrue();
            assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
        }

        @Test
        @DisplayName("isFinal() - 진행 중 상태인 경우 false를 반환해야 한다")
        void isFinal_WithActiveStatus_ShouldReturnFalse() {
            assertThat(OrderStatus.PENDING.isFinal()).isFalse();
            assertThat(OrderStatus.CONFIRMED.isFinal()).isFalse();
            assertThat(OrderStatus.SHIPPED.isFinal()).isFalse();
        }

        @Test
        @DisplayName("isCancellable() - 취소 가능 상태인 경우 true를 반환해야 한다")
        void isCancellable_WithCancellableStatus_ShouldReturnTrue() {
            assertThat(OrderStatus.PENDING.isCancellable()).isTrue();
            assertThat(OrderStatus.CONFIRMED.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("isCancellable() - 취소 불가 상태인 경우 false를 반환해야 한다")
        void isCancellable_WithNonCancellableStatus_ShouldReturnFalse() {
            assertThat(OrderStatus.SHIPPED.isCancellable()).isFalse();
            assertThat(OrderStatus.DELIVERED.isCancellable()).isFalse();
            assertThat(OrderStatus.CANCELLED.isCancellable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Enum 값 검증 테스트")
    class EnumValuesTests {

        @Test
        @DisplayName("OrderStatus는 5개의 상태를 가져야 한다")
        void orderStatus_ShouldHaveFiveValues() {
            assertThat(OrderStatus.values()).hasSize(5);
        }

        @Test
        @DisplayName("모든 상태 값이 존재해야 한다")
        void allStatuses_ShouldExist() {
            assertThat(OrderStatus.valueOf("PENDING")).isNotNull();
            assertThat(OrderStatus.valueOf("CONFIRMED")).isNotNull();
            assertThat(OrderStatus.valueOf("SHIPPED")).isNotNull();
            assertThat(OrderStatus.valueOf("DELIVERED")).isNotNull();
            assertThat(OrderStatus.valueOf("CANCELLED")).isNotNull();
        }
    }
}
```

**Enum VO vs Record VO 차이점**:
| 항목 | Enum VO | Record VO |
|------|---------|-----------|
| 기반 | enum | record |
| of() 메서드 | 불필요 (Enum.valueOf 사용) | 필수 |
| displayName() | 필수 | 선택적 |
| 인스턴스 수 | 고정 (열거 값 개수) | 무제한 |
| equals/hashCode | 자동 (싱글톤) | 자동 (값 기반) |

---

### 2-5) Multi-field VO 템플릿 (Address 등)

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Address Multi-field Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>각 필드별 도메인 규칙 검증</li>
 *   <li>조합 메서드 (getFullAddress)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("Address Multi-field VO 단위 테스트")
class AddressTest {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("of() - 올바른 주소가 생성되어야 한다")
        void of_WithValidAddress_ShouldCreateAddress() {
            // Given
            String zipCode = "12345";
            String street = "123 Main St";
            String detail = "Apt 101";

            // When
            Address address = Address.of(zipCode, street, detail);

            // Then
            assertThat(address).isNotNull();
            assertThat(address.zipCode()).isEqualTo(zipCode);
            assertThat(address.street()).isEqualTo(street);
            assertThat(address.detail()).isEqualTo(detail);
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트 - 우편번호")
    class ZipCodeValidationTests {

        @Test
        @DisplayName("우편번호가 5자리가 아니면 예외가 발생해야 한다")
        void of_WithInvalidZipCode_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Address.of("1234", "123 Main St", "Apt 101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5자리 숫자여야 합니다");

            assertThatThrownBy(() -> Address.of("123456", "123 Main St", "Apt 101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5자리 숫자여야 합니다");
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트 - 주소")
    class StreetValidationTests {

        @Test
        @DisplayName("주소가 null이거나 빈 문자열이면 예외가 발생해야 한다")
        void of_WithNullOrBlankStreet_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Address.of("12345", null, "Apt 101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null이거나 빈 문자열일 수 없습니다");

            assertThatThrownBy(() -> Address.of("12345", "", "Apt 101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null이거나 빈 문자열일 수 없습니다");
        }

        @Test
        @DisplayName("주소가 100자를 초과하면 예외가 발생해야 한다")
        void of_WithTooLongStreet_ShouldThrowException() {
            // Given
            String longStreet = "a".repeat(101);

            // When & Then
            assertThatThrownBy(() -> Address.of("12345", longStreet, "Apt 101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100자를 초과할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트")
    class BusinessMethodTests {

        @Test
        @DisplayName("getFullAddress() - 전체 주소 문자열이 반환되어야 한다")
        void getFullAddress_ShouldReturnFormattedAddress() {
            // Given
            Address address = Address.of("12345", "123 Main St", "Apt 101");

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("[12345] 123 Main St Apt 101");
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 주소를 가진 Address는 equals()로 동등해야 한다")
        void equals_WithSameAddress_ShouldBeEqual() {
            // Given
            Address address1 = Address.of("12345", "123 Main St", "Apt 101");
            Address address2 = Address.of("12345", "123 Main St", "Apt 101");

            // When & Then
            assertThat(address1).isEqualTo(address2);
            assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
        }
    }
}
```

---

### 2-6) Composite VO 템플릿 (FullAddress 등)

```java
package com.ryuqq.domain.{bc}.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * FullAddress Composite Value Object 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>각 VO 개별 테스트 먼저 작성 (ZipCode, Street, City)</li>
 *   <li>Composite는 null 체크만 (각 VO가 이미 검증)</li>
 *   <li>조합 로직 테스트</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("FullAddress Composite VO 단위 테스트")
class FullAddressTest {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {

        @Test
        @DisplayName("of() - 올바른 VO들로 FullAddress가 생성되어야 한다")
        void of_WithValidVOs_ShouldCreateFullAddress() {
            // Given
            ZipCode zipCode = ZipCode.of("12345");
            Street street = Street.of("123 Main St");
            City city = City.of("Seoul");

            // When
            FullAddress address = FullAddress.of(zipCode, street, city);

            // Then
            assertThat(address).isNotNull();
            assertThat(address.zipCode()).isEqualTo(zipCode);
            assertThat(address.street()).isEqualTo(street);
            assertThat(address.city()).isEqualTo(city);
        }
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트 - null 체크만")
    class CompactConstructorTests {

        @Test
        @DisplayName("ZipCode가 null이면 예외가 발생해야 한다")
        void of_WithNullZipCode_ShouldThrowException() {
            // Given
            Street street = Street.of("123 Main St");
            City city = City.of("Seoul");

            // When & Then
            assertThatThrownBy(() -> FullAddress.of(null, street, city))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("우편번호는 null일 수 없습니다");
        }

        @Test
        @DisplayName("각 VO의 검증은 이미 완료되므로 Composite는 null 체크만 수행한다")
        void compositeVO_OnlyChecksNullNotValidation() {
            // Given: 각 VO는 자체 검증을 거친다
            assertThatThrownBy(() -> ZipCode.of("1234"))  // 5자리 아님
                .isInstanceOf(IllegalArgumentException.class);

            // When: 올바른 VO들로 Composite 생성 시
            ZipCode zipCode = ZipCode.of("12345");
            Street street = Street.of("123 Main St");
            City city = City.of("Seoul");

            // Then: 추가 검증 없이 생성 성공
            FullAddress address = FullAddress.of(zipCode, street, city);
            assertThat(address).isNotNull();
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트")
    class BusinessMethodTests {

        @Test
        @DisplayName("getFullAddress() - 모든 VO를 조합한 주소가 반환되어야 한다")
        void getFullAddress_ShouldReturnCombinedAddress() {
            // Given
            ZipCode zipCode = ZipCode.of("12345");
            Street street = Street.of("123 Main St");
            City city = City.of("Seoul");
            FullAddress address = FullAddress.of(zipCode, street, city);

            // When
            String fullAddress = address.getFullAddress();

            // Then
            assertThat(fullAddress).isEqualTo("[12345] Seoul 123 Main St");
        }
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 VO들을 가진 FullAddress는 equals()로 동등해야 한다")
        void equals_WithSameVOs_ShouldBeEqual() {
            // Given
            FullAddress address1 = FullAddress.of(
                ZipCode.of("12345"),
                Street.of("123 Main St"),
                City.of("Seoul")
            );

            FullAddress address2 = FullAddress.of(
                ZipCode.of("12345"),
                Street.of("123 Main St"),
                City.of("Seoul")
            );

            // When & Then
            assertThat(address1).isEqualTo(address2);
            assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
        }
    }
}
```

---

## 3️⃣ 테스트 패턴

### 패턴 1: Compact Constructor 검증 테스트

**목적**: Record의 Compact Constructor에서 도메인 규칙을 검증합니다.

**원칙**:
- null 체크 (필수 필드)
- 도메인 규칙 검증 (범위, 포맷, 길이 등)
- 불변성 보장 (trim, 정규화)

**테스트 케이스**:
```java
// 1) null 체크
@Test
@DisplayName("필수 필드가 null이면 예외가 발생해야 한다")
void of_WithNullValue_ShouldThrowException() {
    // 모든 필수 필드에 대해 null 체크
}

// 2) 도메인 규칙 검증
@Test
@DisplayName("도메인 규칙을 위반하면 예외가 발생해야 한다")
void of_WithInvalidValue_ShouldThrowException() {
    // 범위: 음수, 0 이하, 최대값 초과
    // 포맷: 정규식 불일치, 잘못된 형식
    // 길이: 최소/최대 길이 위반
    // 상태: 만료일 과거, 유효하지 않은 상태
}

// 3) 정규화 처리
@Test
@DisplayName("입력값이 정규화되어야 한다")
void of_WithUnnormalizedValue_ShouldNormalize() {
    // trim 처리
    // 대소문자 변환
    // null → 기본값 변환
}
```

**다양한 타입 예시**:
```java
// BigDecimal (Price)
Price.of(new BigDecimal("-10.00"))  // ❌ 음수
Price.of(new BigDecimal("0"))       // ✅ 0 허용 (할인가)
Price.of(new BigDecimal("1000000")) // ✅ 정상

// LocalDate (ExpiryDate)
ExpiryDate.of(LocalDate.now().minusDays(1))  // ❌ 과거 날짜
ExpiryDate.of(LocalDate.now())               // ✅ 오늘
ExpiryDate.of(LocalDate.now().plusYears(10)) // ✅ 미래

// Enum (OrderStatus)
OrderStatus.of(null)    // ❌ null
OrderStatus.of("DRAFT") // ✅ 유효한 상태
OrderStatus.of("INVALID") // ❌ 유효하지 않은 상태

// Long (ID)
OrderId.of(null)  // ❌ null (forNew() 사용해야 함)
OrderId.of(0L)    // ❌ 0 이하
OrderId.of(-1L)   // ❌ 음수
OrderId.of(100L)  // ✅ 양수

// String (Email)
Email.of(null)               // ❌ null
Email.of("")                 // ❌ 빈 문자열
Email.of("invalid-email")    // ❌ 포맷 위반
Email.of("a".repeat(256))    // ❌ 길이 초과
Email.of("user@example.com") // ✅ 정상
```

---

### 패턴 2: 정적 팩토리 메서드 테스트

**목적**: of(), forNew() 같은 정적 팩토리 메서드의 생성 로직을 검증합니다.

**원칙**:
- of(): 유효한 값으로 VO 생성
- forNew(): ID VO에서 null 생성 (Auto Increment 지원)
- 팩토리 메서드는 Compact Constructor를 호출하므로 검증 로직 재사용

**테스트 케이스**:
```java
// 1) of() 정상 생성
@Test
@DisplayName("of() 호출 시 올바른 값으로 VO가 생성되어야 한다")
void of_WithValidValue_ShouldCreateVO() {
    // Given: 유효한 값
    // When: of() 호출
    // Then: VO 생성 확인, 필드값 검증
}

// 2) forNew() null 생성 (ID VO)
@Test
@DisplayName("forNew() 호출 시 null 값을 가진 ID VO가 생성되어야 한다")
void forNew_ShouldCreateVOWithNullValue() {
    // When: forNew() 호출
    // Then: value() == null, isNew() == true
}

// 3) isNew() 체크 (ID VO)
@Test
@DisplayName("isNew() 호출 시 null 여부를 반환해야 한다")
void isNew_ShouldReturnTrueForNullValue() {
    // Given: forNew()로 생성
    // When: isNew() 호출
    // Then: true 반환
}
```

**다양한 타입 예시**:
```java
// ID VO (Long)
OrderId orderId = OrderId.forNew();         // null
assertThat(orderId.isNew()).isTrue();

OrderId orderId = OrderId.of(100L);         // 100
assertThat(orderId.isNew()).isFalse();

// Simple VO (BigDecimal)
Price price = Price.of(new BigDecimal("1000.00"));
assertThat(price.value()).isEqualByComparingTo("1000.00");

// Simple VO (LocalDate)
ExpiryDate expiryDate = ExpiryDate.of(LocalDate.now().plusDays(30));
assertThat(expiryDate.value()).isAfter(LocalDate.now());

// Multi-field VO (여러 원시 타입)
Address address = Address.of("12345", "123 Main St", "Apt 101");
assertThat(address.zipCode()).isEqualTo("12345");
assertThat(address.street()).isEqualTo("123 Main St");

// Composite VO (VO 안에 VO)
FullAddress fullAddress = FullAddress.of(
    ZipCode.of("12345"),
    Street.of("123 Main St"),
    City.of("Seoul")
);
assertThat(fullAddress.zipCode()).isEqualTo(ZipCode.of("12345"));
```

---

### 패턴 3: 비즈니스 메서드 테스트

**목적**: VO의 도메인 로직(연산, 비교, 변환 등)을 검증합니다.

**원칙**:
- 연산 메서드: add, subtract, multiply 등
- 비교 메서드: isGreaterThan, isLessThan, isBetween 등
- 검증 메서드: isValid, canTransition, hasPermission 등
- 변환 메서드: toDisplayString, toCurrency 등

**테스트 케이스**:
```java
// 1) 연산 메서드
@Test
@DisplayName("연산 메서드는 올바른 결과를 반환해야 한다")
void operation_ShouldReturnCorrectResult() {
    // Given: 두 VO
    // When: add, subtract, multiply 등
    // Then: 결과값 검증
}

// 2) 비교 메서드
@Test
@DisplayName("비교 메서드는 올바른 boolean을 반환해야 한다")
void comparison_ShouldReturnCorrectBoolean() {
    // Given: 두 VO
    // When: isGreaterThan, isLessThan 등
    // Then: true/false 검증
}

// 3) 검증 메서드
@Test
@DisplayName("검증 메서드는 도메인 규칙을 확인해야 한다")
void validation_ShouldCheckDomainRules() {
    // Given: VO
    // When: isValid, canTransition 등
    // Then: true/false 검증
}

// 4) 변환 메서드
@Test
@DisplayName("변환 메서드는 올바른 형식으로 변환해야 한다")
void conversion_ShouldReturnFormattedValue() {
    // Given: VO
    // When: toDisplayString, toCurrency 등
    // Then: 변환된 값 검증
}
```

**다양한 타입 예시**:
```java
// BigDecimal (Price) - 연산
Price price1 = Price.of(new BigDecimal("1000.00"));
Price price2 = Price.of(new BigDecimal("500.00"));
Price result = price1.add(price2);
assertThat(result.value()).isEqualByComparingTo("1500.00");

// BigDecimal (Price) - 비교
assertThat(price1.isGreaterThan(price2)).isTrue();
assertThat(price1.isBetween(Price.of("400"), Price.of("1500"))).isTrue();

// LocalDate (ExpiryDate) - 검증
ExpiryDate expiryDate = ExpiryDate.of(LocalDate.now().plusDays(30));
assertThat(expiryDate.isExpired()).isFalse();
assertThat(expiryDate.isExpiringSoon(7)).isFalse(); // 7일 이내 만료?
assertThat(expiryDate.daysUntilExpiry()).isEqualTo(30);

// Enum (OrderStatus) - 상태 전이
OrderStatus status = OrderStatus.PENDING;
assertThat(status.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
assertThat(status.canTransitionTo(OrderStatus.SHIPPED)).isFalse();

// Multi-field VO (Address) - 변환
Address address = Address.of("12345", "123 Main St", "Apt 101");
assertThat(address.getFullAddress()).isEqualTo("[12345] 123 Main St Apt 101");

// Composite VO (FullAddress) - 조합
FullAddress fullAddress = FullAddress.of(...);
assertThat(fullAddress.getFullAddress()).contains("Seoul");
```

---

### 패턴 4: 상태 변화 및 검증 로직 테스트

**목적**: VO의 상태 의존적 로직과 복잡한 검증 규칙을 테스트합니다.

**원칙**:
- 상태 전이 검증 (Enum VO)
- 만료 검증 (날짜 VO)
- 권한 검증 (역할 VO)
- 복합 조건 검증 (Multi-field VO)

**테스트 케이스**:
```java
// 1) 상태 전이 검증 (Enum VO)
@Test
@DisplayName("상태 전이가 도메인 규칙을 따라야 한다")
void stateTransition_ShouldFollowDomainRules() {
    // Given: 현재 상태
    // When: 다음 상태로 전이 가능 여부 확인
    // Then: canTransitionTo() 검증
}

// 2) 만료 검증 (날짜 VO)
@Test
@DisplayName("만료 여부를 올바르게 판단해야 한다")
void expiry_ShouldBeCheckedCorrectly() {
    // Given: 만료일이 과거/현재/미래인 VO
    // When: isExpired() 호출
    // Then: true/false 검증
}

// 3) 권한 검증 (역할 VO)
@Test
@DisplayName("권한 여부를 올바르게 판단해야 한다")
void permission_ShouldBeCheckedCorrectly() {
    // Given: 역할 VO
    // When: hasPermission(Action) 호출
    // Then: true/false 검증
}

// 4) 복합 조건 검증
@Test
@DisplayName("복합 조건을 만족하는지 검증해야 한다")
void complexValidation_ShouldCheckMultipleConditions() {
    // Given: Multi-field VO
    // When: isValid() 호출 (여러 필드 조합 검증)
    // Then: true/false 검증
}
```

**다양한 타입 예시**:
```java
// Enum (OrderStatus) - 상태 전이
OrderStatus pending = OrderStatus.PENDING;
assertThat(pending.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
assertThat(pending.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
assertThat(pending.canTransitionTo(OrderStatus.CANCELLED)).isTrue();

OrderStatus shipped = OrderStatus.SHIPPED;
assertThat(shipped.canTransitionTo(OrderStatus.CANCELLED)).isFalse(); // 배송 후 취소 불가

// LocalDate (ExpiryDate) - 만료 검증
ExpiryDate expired = ExpiryDate.of(LocalDate.now().minusDays(1));
assertThat(expired.isExpired()).isTrue();

ExpiryDate expiringSoon = ExpiryDate.of(LocalDate.now().plusDays(3));
assertThat(expiringSoon.isExpiringSoon(7)).isTrue();  // 7일 이내
assertThat(expiringSoon.daysUntilExpiry()).isEqualTo(3);

// Enum (UserRole) - 권한 검증
UserRole admin = UserRole.ADMIN;
assertThat(admin.hasPermission(Action.DELETE_USER)).isTrue();
assertThat(admin.hasPermission(Action.VIEW_ONLY)).isTrue();

UserRole viewer = UserRole.VIEWER;
assertThat(viewer.hasPermission(Action.DELETE_USER)).isFalse();
assertThat(viewer.hasPermission(Action.VIEW_ONLY)).isTrue();

// Multi-field VO (PhoneNumber) - 복합 조건
PhoneNumber phone = PhoneNumber.of("010", "1234", "5678");
assertThat(phone.isValid()).isTrue();  // 형식 + 길이 + 번호 유효성
assertThat(phone.isMobile()).isTrue(); // 010으로 시작

PhoneNumber landline = PhoneNumber.of("02", "1234", "5678");
assertThat(landline.isMobile()).isFalse();
assertThat(landline.isLandline()).isTrue();
```

---

### 패턴 5: equals/hashCode 테스트

**목적**: Record의 자동 생성된 equals/hashCode가 올바르게 동작하는지 확인합니다.

**원칙**:
- 같은 값 → equals() true, hashCode() 동일
- 다른 값 → equals() false, hashCode() 다를 수 있음
- Record는 자동 생성이지만 확인 필수

**테스트 케이스**:
```java
// 1) 같은 값 equals
@Test
@DisplayName("같은 값을 가진 VO는 equals()로 동등해야 한다")
void equals_WithSameValue_ShouldBeEqual() {
    // Given: 같은 값으로 생성한 두 VO
    // When: equals() 호출
    // Then: true, hashCode() 동일
}

// 2) 다른 값 equals
@Test
@DisplayName("다른 값을 가진 VO는 equals()로 다르다")
void equals_WithDifferentValue_ShouldNotBeEqual() {
    // Given: 다른 값으로 생성한 두 VO
    // When: equals() 호출
    // Then: false
}

// 3) null equals (ID VO)
@Test
@DisplayName("forNew()로 생성한 ID VO는 null 기준으로 동등해야 한다")
void equals_WithBothNull_ShouldBeEqual() {
    // Given: forNew()로 생성한 두 ID VO
    // When: equals() 호출
    // Then: true (둘 다 null)
}
```

**다양한 타입 예시**:
```java
// BigDecimal (Price)
Price price1 = Price.of(new BigDecimal("1000.00"));
Price price2 = Price.of(new BigDecimal("1000.00"));
assertThat(price1).isEqualTo(price2);
assertThat(price1.hashCode()).isEqualTo(price2.hashCode());

// LocalDate (ExpiryDate)
LocalDate date = LocalDate.of(2025, 12, 31);
ExpiryDate expiry1 = ExpiryDate.of(date);
ExpiryDate expiry2 = ExpiryDate.of(date);
assertThat(expiry1).isEqualTo(expiry2);

// Enum (OrderStatus)
OrderStatus status1 = OrderStatus.PENDING;
OrderStatus status2 = OrderStatus.PENDING;
assertThat(status1).isEqualTo(status2); // Enum은 싱글톤

// Multi-field VO (Address)
Address addr1 = Address.of("12345", "123 Main St", "Apt 101");
Address addr2 = Address.of("12345", "123 Main St", "Apt 101");
assertThat(addr1).isEqualTo(addr2);
assertThat(addr1.hashCode()).isEqualTo(addr2.hashCode());

// Composite VO (FullAddress)
FullAddress full1 = FullAddress.of(
    ZipCode.of("12345"),
    Street.of("123 Main St"),
    City.of("Seoul")
);
FullAddress full2 = FullAddress.of(
    ZipCode.of("12345"),
    Street.of("123 Main St"),
    City.of("Seoul")
);
assertThat(full1).isEqualTo(full2);
```

---

## 4️⃣ VO 유형별 테스트 전략

각 VO 유형은 위의 5가지 패턴을 조합하여 테스트합니다. 강조할 패턴만 표시합니다.

| VO 유형 | @Nested 클래스 구성 | 특징 |
|---------|---------------------|------|
| **Long ID VO** | FactoryMethodTests, CompactConstructorTests, IsNewMethodTests, EqualsAndHashCodeTests | forNew()→null, isNew() 필수 |
| **UUID ID VO** | FactoryMethodTests, CompactConstructorTests, EqualsAndHashCodeTests | forNew()→UUID, isNew() 없음 |
| **Enum VO** | DisplayNameTests, StatusCheckTests, EnumValuesTests | displayName() 필수, of() 없음 |
| **Simple VO** | FactoryMethodTests, CompactConstructorTests, BusinessMethodTests, EqualsAndHashCodeTests, ConstantTests | 연산/비교 메서드 중심 |
| **Multi-field VO** | FactoryMethodTests, (필드별) ValidationTests, BusinessMethodTests, EqualsAndHashCodeTests | 각 필드별 @Nested 분리 |
| **Composite VO** | FactoryMethodTests, CompactConstructorTests, BusinessMethodTests, EqualsAndHashCodeTests | null 체크만, 조합 로직 |

### Long ID VO (OrderId - Auto Increment)
- **패턴 1**: 양수 검증, null 거부 (of에서)
- **패턴 2**: forNew() null 생성, isNew() 체크
- **패턴 5**: null 기준 equals

### UUID ID VO (UserId - UUIDv7)
- **패턴 1**: UUID 형식 검증, null/빈문자열 거부
- **패턴 2**: forNew() UUID 생성, 매번 다른 값
- **패턴 5**: 값 기준 equals (forNew()는 항상 다름)

### Enum VO (OrderStatus)
- **패턴 3**: 상태 체크 메서드 (isActive, isFinal)
- **패턴 4**: 상태 검증 (displayName, 모든 값 검증)
- **패턴 5**: 싱글톤 equals (Enum 기본 동작)

### Simple VO (Money, Email, Price, ExpiryDate)
- **패턴 1**: 도메인 규칙 검증 (범위, 포맷, 길이)
- **패턴 3**: 비즈니스 메서드 (연산, 비교, 검증)
- **패턴 4**: 상태 검증 (만료, 유효성)

### Multi-field VO (Address, PhoneNumber)
- **패턴 1**: 각 필드별 검증 (@Nested로 분리)
- **패턴 3**: 조합 메서드 (getFullAddress)
- **패턴 4**: 복합 조건 검증 (isValid)

### Composite VO (FullAddress)
- **패턴 2**: 각 VO 개별 테스트 먼저 작성 ✅
- **패턴 1**: Composite는 null 체크만 (각 VO가 이미 검증)
- **패턴 3**: 조합 로직 (getFullAddress)

---

## 5️⃣ TestFixture 패턴 (선택적)

Record는 생성이 간단하므로 Fixture가 필수는 아니지만, **일관성**을 위해 사용 가능합니다.

```java
/**
 * OrderId TestFixture
 */
public class OrderIdFixture {
    public static OrderId forNew() {
        return OrderId.forNew();
    }

    public static OrderId defaultId() {
        return OrderId.of(100L);
    }

    public static OrderId of(Long value) {
        return OrderId.of(value);
    }

    private OrderIdFixture() {
        throw new AssertionError("Fixture 클래스는 인스턴스화할 수 없습니다.");
    }
}
```

---

## 6️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ @Tag 누락
@DisplayName("Money VO 테스트")
class MoneyTest {  // ❌ @Tag("unit"), @Tag("domain"), @Tag("vo") 없음
}

// ❌ @Nested 없이 평면 구조
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("Money VO 테스트")
class MoneyTest {
    @Test
    void testOf() { }  // ❌ 관심사 분리 없음

    @Test
    void testAdd() { }

    @Test
    void testEquals() { }
}

// ❌ Record 검증 로직을 우회
@Test
void badTest() {
    Money money = new Money(-100L);  // ❌ 직접 생성 (검증 우회)
}

// ❌ Composite VO의 각 VO를 검증하지 않음
@Test
void badTest() {
    // ZipCode, Street, City 테스트 없음 ❌
    FullAddress address = FullAddress.of(...);
}
```

### ✅ Good Examples

```java
// ✅ @Tag 필수 3종 세트
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("Money VO 단위 테스트")
class MoneyTest {
    // ...
}

// ✅ @Nested로 관심사 명확히 분리 (2-5개 권장)
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("Money VO 단위 테스트")
class MoneyTest {

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class FactoryMethodTests {
        // of(), forNew() 테스트
    }

    @Nested
    @DisplayName("Compact Constructor 검증 테스트")
    class CompactConstructorTests {
        // 도메인 규칙 검증 테스트
    }

    @Nested
    @DisplayName("비즈니스 메서드 테스트")
    class BusinessMethodTests {
        // add, subtract 등
    }

    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsAndHashCodeTests {
        // equals/hashCode
    }
}

// ✅ 정적 팩토리 메서드 사용
@Test
void goodTest() {
    Money money = Money.of(1000L);  // ✅ of() 사용
}

// ✅ Composite VO는 각 VO를 먼저 테스트
@Test
void goodTest() {
    // 1. 각 VO 개별 테스트 ✅
    ZipCode zipCode = ZipCode.of("12345");
    Street street = Street.of("123 Main St");
    City city = City.of("Seoul");

    // 2. Composite VO 테스트 ✅
    FullAddress address = FullAddress.of(zipCode, street, city);
}
```

---

## 7️⃣ 체크리스트

Value Object 테스트 작성 후 다음을 확인:

### 기본 테스트 (모든 VO)
- [ ] **@Tag 3종 세트** (@Tag("unit"), @Tag("domain"), @Tag("vo"))
- [ ] **@Nested 클래스로 관심사 분리** (2-5개 권장)
- [ ] 정적 팩토리 메서드 (of, forNew) 테스트
- [ ] Compact Constructor 검증 로직 테스트
- [ ] null 체크 테스트
- [ ] 도메인 규칙 위반 테스트
- [ ] equals/hashCode 테스트

### Long ID VO 추가 체크 (Auto Increment)
- [ ] forNew() null 생성 테스트
- [ ] isNew() 메서드 테스트
- [ ] 양수 검증 테스트
- [ ] **@Nested IsNewMethodTests** 존재

### UUID ID VO 추가 체크 (UUIDv7)
- [ ] forNew() UUID 생성 테스트
- [ ] 매 호출마다 다른 UUID 생성 테스트
- [ ] UUID 형식 검증 테스트
- [ ] null/빈 문자열 금지 테스트
- [ ] **isNew() 메서드 없음** (항상 값 존재)

### Enum VO 추가 체크
- [ ] displayName() 메서드 테스트
- [ ] 모든 Enum 값의 displayName() null 아님 테스트
- [ ] 상태 체크 메서드 테스트 (isActive, isFinal 등)
- [ ] Enum 값 개수 검증 테스트
- [ ] **@Nested DisplayNameTests** 존재
- [ ] **@Nested StatusCheckTests** 존재 (상태 체크 메서드 있는 경우)

### Simple VO 추가 체크
- [ ] 비즈니스 메서드 테스트 (연산, 비교, 검증)
- [ ] 상태 변화 테스트 (만료, 유효성)
- [ ] 상수 (ZERO 등) 테스트
- [ ] **@Nested BusinessMethodTests** 존재
- [ ] **@Nested ConstantTests** 존재 (상수 있는 경우)

### Multi-field VO 추가 체크
- [ ] 각 필드별 검증 테스트
- [ ] 조합 메서드 테스트 (getFullAddress 등)
- [ ] 복합 조건 검증 테스트
- [ ] **@Nested (필드명)ValidationTests** 각 필드별 분리

### Composite VO 추가 체크
- [ ] **각 VO 개별 테스트 먼저 작성** ✅
- [ ] Composite VO의 null 체크 테스트
- [ ] 조합 로직 테스트
- [ ] **@Nested CompactConstructorTests** (null 체크만)

---

**✅ Value Object 테스트는 @Tag 필수, @Nested로 관심사 분리, 패턴 중심 접근이 핵심입니다!**
