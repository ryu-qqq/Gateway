# Domain Layer 테스트 가이드

> **목적**: Domain Layer 전체 테스트 전략 및 규칙 통합 문서
>
> **핵심 원칙**: Pure Java 단위 테스트, 외부 의존성 제로, 빠른 실행

---

## 1) 테스트 전략 개요

### Domain Layer 테스트 특징

| 특징 | 설명 |
|------|------|
| **테스트 유형** | Pure Java 단위 테스트 |
| **외부 의존성** | 제로 (Spring Context, DB 없음) |
| **실행 속도** | 밀리초 단위 (< 100ms) |
| **Mock 사용** | 최소화 (Clock 등 필수 경우만) |
| **Object Mother** | 권장 (비즈니스 시나리오 표현) |

### 컴포넌트별 테스트 범위

| 컴포넌트 | 테스트 대상 | 상세 가이드 |
|----------|-----------|------------|
| **Aggregate Root** | 팩토리 메서드, 비즈니스 메서드, 상태 전이, Invariant | [aggregate-test-guide.md](../aggregate/aggregate-test-guide.md) |
| **Value Object** | 정적 팩토리, 동등성, 불변성, 유효성 검증 | [vo-test-guide.md](../vo/vo-test-guide.md) |
| **Domain Exception** | 생성자, 메시지 포맷, ErrorCode 매핑 | [exception-test-guide.md](../exception/exception-test-guide.md) |
| **Domain Event** | 불변성, 필드 검증, 직렬화 | [event-guide.md](../event/event-guide.md) |
| **Criteria** | 필터 조건 검증 | 단순 record - 별도 테스트 불필요 |

---

## 2) 테스트 태그 규칙 (Zero-Tolerance)

### 필수 태그 조합

```java
// ✅ 모든 Domain Layer 테스트에 필수
@Tag("unit")      // 단위 테스트 표시
@Tag("domain")    // Domain Layer 표시
```

### 컴포넌트별 추가 태그

| 컴포넌트 | 추가 태그 | 전체 태그 조합 |
|----------|----------|---------------|
| Aggregate | `@Tag("aggregate")` | `@Tag("unit")`, `@Tag("domain")`, `@Tag("aggregate")` |
| VO | `@Tag("vo")` | `@Tag("unit")`, `@Tag("domain")`, `@Tag("vo")` |
| Exception | `@Tag("exception")` | `@Tag("unit")`, `@Tag("domain")`, `@Tag("exception")` |
| Event | `@Tag("event")` | `@Tag("unit")`, `@Tag("domain")`, `@Tag("event")` |
| Policy | `@Tag("policy")` | `@Tag("unit")`, `@Tag("domain")`, `@Tag("policy")` |

### 태그 기반 테스트 실행

```bash
# Domain Layer 전체 테스트
./gradlew :domain:test

# 단위 테스트만 실행
./gradlew :domain:test -PincludeTags=unit

# Domain Aggregate 테스트만 실행
./gradlew :domain:test -PincludeTags=aggregate

# VO 테스트만 실행
./gradlew :domain:test -PincludeTags=vo
```

---

## 3) 테스트 구조 규칙

### 디렉토리 구조

```
domain/
└─ src/
   ├─ main/java/
   │  └─ com/ryuqq/domain/{bc}/
   │      ├─ aggregate/{name}/
   │      │   └─ Order.java
   │      ├─ vo/
   │      │   └─ OrderId.java
   │      ├─ exception/
   │      │   └─ InvalidOrderStateException.java
   │      └─ event/
   │          └─ OrderPlacedEvent.java
   │
   └─ test/java/
      └─ com/ryuqq/domain/{bc}/
          ├─ aggregate/{name}/
          │   └─ OrderTest.java           # ✅ 동일 패키지
          ├─ vo/
          │   └─ OrderIdTest.java         # ✅ 동일 패키지
          ├─ exception/
          │   └─ InvalidOrderStateExceptionTest.java
          ├─ event/
          │   └─ OrderPlacedEventTest.java
          └─ mother/
              └─ Orders.java              # ✅ Object Mother
```

### 테스트 클래스 네이밍

| 패턴 | 용도 | 예시 |
|------|------|------|
| `{ClassName}Test.java` | 단위 테스트 | `OrderTest.java`, `OrderIdTest.java` |
| `{Name}s.java` | Object Mother | `Orders.java`, `OrderIds.java` |

---

## 4) 테스트 템플릿

### Aggregate Root 테스트

```java
package com.ryuqq.domain.order.aggregate.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

/**
 * Order Aggregate Root 단위 테스트
 *
 * @see Order
 */
@Tag("unit")
@Tag("domain")
@Tag("aggregate")
@DisplayName("Order Aggregate Root 단위 테스트")
class OrderTest {

    // ✅ Clock 고정 (테스트 재현성)
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    @Nested
    @DisplayName("정적 팩토리 메서드")
    class FactoryMethodTests {

        @Test
        @DisplayName("forNew() - 신규 생성 시 ID는 null, 상태는 PENDING")
        void forNew_ShouldCreateWithNullIdAndPendingStatus() {
            // Given & When
            Order order = Order.forNew(CustomerId.of(1L), FIXED_CLOCK);

            // Then
            assertThat(order.id()).isNull();
            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("비즈니스 메서드")
    class BusinessMethodTests {

        @Test
        @DisplayName("confirm() - PENDING 상태에서 CONFIRMED로 전이")
        void confirm_ShouldTransitionToConfirmed_WhenPending() {
            // Given
            Order order = Orders.pending();

            // When
            order.confirm(FIXED_CLOCK);

            // Then
            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("confirm() - 이미 CONFIRMED면 예외 발생")
        void confirm_ShouldThrowException_WhenAlreadyConfirmed() {
            // Given
            Order order = Orders.confirmed();

            // When & Then
            assertThatThrownBy(() -> order.confirm(FIXED_CLOCK))
                .isInstanceOf(InvalidOrderStateException.class);
        }
    }
}
```

### Value Object 테스트

```java
package com.ryuqq.domain.order.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderId Value Object 단위 테스트
 *
 * @see OrderId
 */
@Tag("unit")
@Tag("domain")
@Tag("vo")
@DisplayName("OrderId Value Object 단위 테스트")
class OrderIdTest {

    @Nested
    @DisplayName("정적 팩토리 메서드")
    class FactoryMethodTests {

        @Test
        @DisplayName("of() - 유효한 ID로 생성 성공")
        void of_ShouldCreateInstance_WhenValidId() {
            // When
            OrderId orderId = OrderId.of(1L);

            // Then
            assertThat(orderId.value()).isEqualTo(1L);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L})
        @DisplayName("of() - null 또는 0 이하면 예외 발생")
        void of_ShouldThrowException_WhenInvalidId(Long invalidId) {
            // When & Then
            assertThatThrownBy(() -> OrderId.of(invalidId))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("동등성")
    class EqualityTests {

        @Test
        @DisplayName("같은 값이면 equals() true")
        void equals_ShouldReturnTrue_WhenSameValue() {
            // Given
            OrderId id1 = OrderId.of(1L);
            OrderId id2 = OrderId.of(1L);

            // Then
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }
    }
}
```

### Domain Exception 테스트

```java
package com.ryuqq.domain.order.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * InvalidOrderStateException 단위 테스트
 *
 * @see InvalidOrderStateException
 */
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("InvalidOrderStateException 단위 테스트")
class InvalidOrderStateExceptionTest {

    @Nested
    @DisplayName("생성자")
    class ConstructorTests {

        @Test
        @DisplayName("메시지 포맷 검증")
        void constructor_ShouldFormatMessage() {
            // When
            InvalidOrderStateException exception =
                new InvalidOrderStateException(OrderStatus.PENDING, OrderStatus.SHIPPED);

            // Then
            assertThat(exception.getMessage())
                .contains("PENDING")
                .contains("SHIPPED");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑")
    class ErrorCodeTests {

        @Test
        @DisplayName("ErrorCode가 올바르게 매핑되어야 함")
        void errorCode_ShouldBeMapped() {
            // When
            InvalidOrderStateException exception =
                new InvalidOrderStateException(OrderStatus.PENDING, OrderStatus.SHIPPED);

            // Then
            assertThat(exception.getErrorCode())
                .isEqualTo(OrderErrorCode.INVALID_STATE_TRANSITION);
        }
    }
}
```

---

## 5) Object Mother 패턴

### 목적

- 테스트 데이터 중앙 관리
- 비즈니스 시나리오 표현
- 코드 재사용성 향상

### 구현 예시

```java
package com.ryuqq.domain.order.mother;

import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Order Aggregate Object Mother
 *
 * <p>테스트용 Order 인스턴스 생성 팩토리
 */
public final class Orders {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2024-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    private Orders() {
        // Utility class
    }

    // ============ 상태별 팩토리 ============

    /**
     * PENDING 상태의 Order
     */
    public static Order pending() {
        return Order.forNew(CustomerId.of(1L), FIXED_CLOCK);
    }

    /**
     * CONFIRMED 상태의 Order
     */
    public static Order confirmed() {
        Order order = pending();
        order.confirm(FIXED_CLOCK);
        return order;
    }

    /**
     * SHIPPED 상태의 Order
     */
    public static Order shipped() {
        Order order = confirmed();
        order.ship(FIXED_CLOCK);
        return order;
    }

    /**
     * CANCELLED 상태의 Order
     */
    public static Order cancelled() {
        Order order = pending();
        order.cancel("테스트 취소", FIXED_CLOCK);
        return order;
    }

    // ============ 시나리오별 팩토리 ============

    /**
     * 취소 가능한 Order (PENDING)
     */
    public static Order cancellable() {
        return pending();
    }

    /**
     * 취소 불가능한 Order (SHIPPED)
     */
    public static Order notCancellable() {
        return shipped();
    }
}
```

---

## 6) 금지 사항 (Zero-Tolerance)

### 절대 금지

| 항목 | 이유 |
|------|------|
| ❌ `@SpringBootTest` | Domain은 Spring 의존성 없음 |
| ❌ `@DataJpaTest` | DB 의존성 금지 |
| ❌ `@Mock` 과다 사용 | Pure Java 테스트 원칙 위반 |
| ❌ `@Autowired` | Spring Context 로딩 금지 |
| ❌ Testcontainers | Domain은 인프라 의존성 없음 |

### 허용되는 Mock

| 항목 | 허용 이유 |
|------|----------|
| ✅ `Clock` | 시간 고정 필요 |
| ✅ `Random` | 결정적 테스트 필요 시 |

---

## 7) 실행 속도 기준

| 기준 | 임계값 | 조치 |
|------|--------|------|
| 단일 테스트 | < 10ms | 정상 |
| 단일 테스트 | 10-100ms | 검토 필요 |
| 단일 테스트 | > 100ms | 리팩토링 필수 |
| 전체 Domain 테스트 | < 5초 | 정상 |

### 느린 테스트 원인

- ❌ Spring Context 로딩
- ❌ DB 연결
- ❌ 파일 I/O
- ❌ 네트워크 호출

---

## 8) 체크리스트

### 테스트 작성 전

- [ ] 테스트 대상 컴포넌트 확인 (Aggregate, VO, Exception, Event)
- [ ] 해당 컴포넌트 테스트 가이드 참조
- [ ] Object Mother 존재 여부 확인

### 테스트 작성 중

- [ ] `@Tag("unit")`, `@Tag("domain")` 필수 적용
- [ ] 컴포넌트별 추가 태그 적용 (`@Tag("aggregate")` 등)
- [ ] `@DisplayName` 한글로 작성
- [ ] `@Nested`로 관심사 분리
- [ ] AssertJ 사용 (JUnit Assertions 금지)

### 테스트 작성 후

- [ ] 실행 속도 < 100ms 확인
- [ ] Spring 의존성 없음 확인
- [ ] `./gradlew :domain:test` 전체 통과

---

## 참조 문서

- [Aggregate 테스트 가이드](../aggregate/aggregate-test-guide.md)
- [VO 테스트 가이드](../vo/vo-test-guide.md)
- [Exception 테스트 가이드](../exception/exception-test-guide.md)
- [Domain Event 가이드](../event/event-guide.md)
