# Assembler 테스트 가이드

> **목적**: Assembler의 단위 테스트 전략 (순수 Java 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
Assembler는 **DTO ↔ Domain 변환**만 검증합니다:

```
✅ 테스트 항목:
1. Command/Query DTO → Domain 변환 검증
2. Domain → Response DTO 변환 검증
3. List 변환 검증
4. Law of Demeter 준수 검증 (Getter 체이닝 없음)
5. 필드 매핑 정확성 검증
```

### 테스트 범위
- ✅ 순수 Java 단위 테스트 (외부 의존성 없음)
- ✅ 실제 Domain 객체 사용 (Mock 불필요)
- ✅ 빠른 실행 (밀리초 단위)
- ❌ Spring Context 로딩 금지
- ❌ Mock 사용 불필요 (의존성 없는 단순 변환기)
- ❌ 비즈니스 로직 테스트 금지 (Domain Test로 분리)

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.application.{bc}.assembler;

import com.ryuqq.application.{bc}.dto.command.{Action}{Bc}Command;
import com.ryuqq.application.{bc}.dto.query.{Bc}SearchQuery;
import com.ryuqq.application.{bc}.dto.response.{Bc}Response;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * {Bc} Assembler 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
@DisplayName("{Bc} Assembler 단위 테스트")
class {Bc}AssemblerTest {

    private {Bc}Assembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new {Bc}Assembler();
    }

    @Test
    @DisplayName("Command → Domain 변환이 올바르게 동작해야 한다")
    void toDomain_ShouldConvertCommandToDomain() {
        // Given
        {Action}{Bc}Command command = new {Action}{Bc}Command(
            /* command fields */
        );

        // When
        {Bc} result = assembler.toDomain(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isNotNull();
        // 필드 매핑 검증
    }

    @Test
    @DisplayName("Domain → Response 변환이 올바르게 동작해야 한다")
    void toResponse_ShouldConvertDomainToResponse() {
        // Given
        {Bc} {bc} = {Bc}.forNew(/* domain fields */);

        // When
        {Bc}Response result = assembler.toResponse({bc});

        // Then
        assertThat(result).isNotNull();
        // 필드 매핑 검증
    }

    @Test
    @DisplayName("List 변환이 올바르게 동작해야 한다")
    void toResponseList_ShouldConvertListCorrectly() {
        // Given
        List<{Bc}> {bc}List = List.of(
            {Bc}.forNew(/* domain fields 1 */),
            {Bc}.forNew(/* domain fields 2 */)
        );

        // When
        List<{Bc}Response> result = assembler.toResponseList({bc}List);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(1)).isNotNull();
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
package com.ryuqq.application.order.assembler;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;
import com.ryuqq.application.order.dto.response.OrderResponse;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Order Assembler 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
@DisplayName("Order Assembler 단위 테스트")
class OrderAssemblerTest {

    private OrderAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new OrderAssembler();
    }

    @Test
    @DisplayName("Command → Domain 변환이 올바르게 동작해야 한다")
    void toDomain_ShouldConvertCommandToDomain() {
        // Given
        PlaceOrderCommand command = new PlaceOrderCommand(
            BigDecimal.valueOf(50000)
        );

        // When
        Order result = assembler.toDomain(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIdValue()).isNotNull();
        assertThat(result.getAmountValue()).isEqualTo(BigDecimal.valueOf(50000));
        // ✅ OrderStatus는 forNew() 내부에서 PLACED로 설정됨
    }

    @Test
    @DisplayName("Domain → Response 변환이 올바르게 동작해야 한다")
    void toResponse_ShouldConvertDomainToResponse() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        // When
        OrderResponse result = assembler.toResponse(order);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(order.getIdValue());
        assertThat(result.amount()).isEqualTo(order.getAmountValue());
        assertThat(result.status()).isEqualTo(order.getStatusName());
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("List 변환이 올바르게 동작해야 한다")
    void toResponseList_ShouldConvertListCorrectly() {
        // Given
        List<Order> orders = List.of(
            Order.forNew(OrderId.forNew(), Money.of(BigDecimal.valueOf(10000))),
            Order.forNew(OrderId.forNew(), Money.of(BigDecimal.valueOf(20000))),
            Order.forNew(OrderId.forNew(), Money.of(BigDecimal.valueOf(30000)))
        );

        // When
        List<OrderResponse> result = assembler.toResponseList(orders);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).amount()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(result.get(1).amount()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(result.get(2).amount()).isEqualTo(BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("빈 리스트 변환이 올바르게 동작해야 한다")
    void toResponseList_ShouldHandleEmptyList() {
        // Given
        List<Order> emptyList = List.of();

        // When
        List<OrderResponse> result = assembler.toResponseList(emptyList);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Law of Demeter 준수: Getter 체이닝 없이 값을 가져와야 한다")
    void toResponse_ShouldFollowLawOfDemeter() {
        // Given
        Order order = Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );

        // When
        OrderResponse result = assembler.toResponse(order);

        // Then
        // ✅ order.getIdValue() 사용 (체이닝 없음)
        assertThat(result.orderId()).isEqualTo(order.getIdValue());

        // ❌ order.getId().value() 체이닝 금지
        // assertThat(result.orderId()).isEqualTo(order.getId().value());
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ Spring Context 로딩
@SpringBootTest
class OrderAssemblerTest {
    // Spring Context 로딩 불필요!
}

// ❌ @ExtendWith(MockitoExtension.class) 사용
@ExtendWith(MockitoExtension.class)
class OrderAssemblerTest {
    @Mock private Order order;  // Assembler는 의존성이 없어서 Mock 불필요!
}

// ❌ 비즈니스 로직 테스트
@Test
void toDomain_WithBusinessLogic() {
    Order order = assembler.toDomain(command);
    order.confirm();  // 비즈니스 로직은 Domain Test로!
}

// ❌ Getter 체이닝 사용
@Test
void toResponse_WithGetterChaining() {
    OrderResponse response = assembler.toResponse(order);

    // ❌ Law of Demeter 위반!
    assertThat(response.orderId()).isEqualTo(order.getId().value());
}

// ❌ PageResponse/SliceResponse 변환 테스트
@Test
void toPageResponse_ShouldConvert() {
    // ❌ PageResponse 조립은 UseCase 책임!
}
```

### ✅ Good Examples

```java
// ✅ 순수 Java 단위 테스트
@Tag("unit")
@Tag("assembler")
@Tag("application-layer")
class OrderAssemblerTest {
    private OrderAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new OrderAssembler();
    }
}

// ✅ 실제 Domain 객체 사용
@Test
void toDomain_ShouldConvert() {
    PlaceOrderCommand command = new PlaceOrderCommand(
        BigDecimal.valueOf(50000)
    );

    Order order = assembler.toDomain(command);

    assertThat(order).isNotNull();
}

// ✅ Law of Demeter 준수 검증
@Test
void toResponse_ShouldFollowLawOfDemeter() {
    Order order = Order.forNew(...);

    OrderResponse response = assembler.toResponse(order);

    // ✅ 체이닝 없이 직접 값 반환
    assertThat(response.orderId()).isEqualTo(order.getIdValue());
}

// ✅ List 변환 검증
@Test
void toResponseList_ShouldConvertList() {
    List<Order> orders = List.of(
        Order.forNew(...),
        Order.forNew(...)
    );

    List<OrderResponse> responses = assembler.toResponseList(orders);

    assertThat(responses).hasSize(2);
}
```

---

## 5️⃣ 테스트 시나리오

### Command → Domain 변환
```java
@Test
@DisplayName("Command 필드가 Domain 객체에 올바르게 매핑되어야 한다")
void toDomain_ShouldMapAllFields() {
    // Given
    PlaceOrderCommand command = new PlaceOrderCommand(
        BigDecimal.valueOf(50000)
    );

    // When
    Order order = assembler.toDomain(command);

    // Then
    assertThat(order.getAmountValue()).isEqualTo(command.amount());
    // ✅ Status는 Domain 내부에서 설정
}
```

### Domain → Response 변환
```java
@Test
@DisplayName("Domain 필드가 Response에 올바르게 매핑되어야 한다")
void toResponse_ShouldMapAllFields() {
    // Given
    Order order = Order.forNew(
        OrderId.forNew(),
        Money.of(BigDecimal.valueOf(50000))
    );

    // When
    OrderResponse response = assembler.toResponse(order);

    // Then
    assertThat(response.orderId()).isEqualTo(order.getIdValue());
    assertThat(response.amount()).isEqualTo(order.getAmountValue());
    assertThat(response.status()).isEqualTo(order.getStatusName());
    assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
}
```

### Null 처리
```java
@Test
@DisplayName("null List를 전달하면 빈 List를 반환해야 한다")
void toResponseList_ShouldHandleNull() {
    // When
    List<OrderResponse> result = assembler.toResponseList(null);

    // Then
    assertThat(result).isEmpty();
}
```

### 빈 컬렉션 처리
```java
@Test
@DisplayName("빈 List를 전달하면 빈 List를 반환해야 한다")
void toResponseList_ShouldHandleEmptyList() {
    // Given
    List<Order> emptyList = List.of();

    // When
    List<OrderResponse> result = assembler.toResponseList(emptyList);

    // Then
    assertThat(result).isEmpty();
}
```

---

## 6️⃣ 체크리스트

Assembler 테스트 작성 시:
- [ ] `@Tag("unit")`, `@Tag("assembler")`, `@Tag("application-layer")` 필수
- [ ] `@BeforeEach`에서 Assembler 인스턴스 생성
- [ ] Command → Domain 변환 검증
- [ ] Domain → Response 변환 검증
- [ ] List 변환 검증
- [ ] 빈 List 처리 검증
- [ ] Null 처리 검증 (필요 시)
- [ ] Law of Demeter 준수 검증 (Getter 체이닝 없음)
- [ ] 모든 필드 매핑 검증
- [ ] Spring Context 로딩 금지
- [ ] Mock 사용 금지 (의존성 없음)
- [ ] 비즈니스 로직 테스트 금지
- [ ] PageResponse/SliceResponse 변환 테스트 금지

---

## 7️⃣ 성능 고려사항

### 빠른 실행
```java
@Test
@DisplayName("Assembler 테스트는 밀리초 단위로 실행되어야 한다")
void assembler_ShouldExecuteQuickly() {
    // Given
    long startTime = System.currentTimeMillis();

    PlaceOrderCommand command = new PlaceOrderCommand(
        BigDecimal.valueOf(50000)
    );

    // When
    Order order = assembler.toDomain(command);
    OrderResponse response = assembler.toResponse(order);

    // Then
    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(10);  // 10ms 이하
}
```

---

## 📖 관련 문서

- **[Assembler Guide](assembler-guide.md)** - Assembler 구현 가이드
- **[Assembler ArchUnit](assembler-archunit.md)** - ArchUnit 자동 검증 규칙
- **[UseCase Test Guide](../testing/01_usecase-unit-test.md)** - UseCase 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
