# Domain Exception 테스트 가이드

> **목적**: Domain Layer 예외의 단위 테스트 패턴과 원칙

---

## 1️⃣ 테스트 전략

### 테스트 대상
Domain Exception은 **ErrorCode, 구체적인 예외 클래스, 예외 발생 시나리오**를 테스트합니다:

```
✅ 테스트 항목:
1. ErrorCode Enum (getCode, getHttpStatus, getMessage)
2. 구체적인 예외 클래스 (생성자, 에러 코드 매핑, args)
3. Domain Layer 예외 발생 (IllegalArgumentException, DomainException)
4. 예외 메시지 검증 (비즈니스 용어, 컨텍스트 정보)
```

### 테스트 범위
- ✅ **Pure Java 단위 테스트** (외부 의존성 제로)
- ✅ **@Tag 필수** (@Tag("unit"), @Tag("domain"), @Tag("exception"))
- ✅ **@Nested 클래스로 관심사 묶기** (2-5개 권장)
- ✅ **DisplayName으로 명확한 의도 표현**
- ❌ Mock 불필요 (Exception은 의존성 없음)
- ❌ Spring Context 로딩 금지
- ❌ GlobalExceptionHandler 테스트 제외 (Adapter Layer에서)

---

## 2️⃣ 기본 템플릿

### 2-1) ErrorCode Enum 테스트 템플릿

```java
package com.ryuqq.domain.{bc}.exception;

import com.ryuqq.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

/**
 * {Bc}ErrorCode Enum 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>ErrorCode 인터페이스 구현 검증</li>
 *   <li>에러 코드 형식 검증 ({BC}-{3자리 숫자})</li>
 *   <li>HTTP 상태 코드 매핑 검증</li>
 *   <li>에러 메시지 null 체크</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("{Bc}ErrorCode Enum 단위 테스트")
class {Bc}ErrorCodeTest {

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 검증")
    class ErrorCodeInterfaceTests {

        @ParameterizedTest
        @EnumSource({Bc}ErrorCode.class)
        @DisplayName("모든 ErrorCode는 getCode()를 구현해야 한다")
        void allErrorCodes_ShouldImplementGetCode({Bc}ErrorCode errorCode) {
            // When
            String code = errorCode.getCode();

            // Then
            assertThat(code).isNotNull();
            assertThat(code).isNotBlank();
        }

        @ParameterizedTest
        @EnumSource({Bc}ErrorCode.class)
        @DisplayName("모든 ErrorCode는 getHttpStatus()를 구현해야 한다")
        void allErrorCodes_ShouldImplementGetHttpStatus({Bc}ErrorCode errorCode) {
            // When
            int httpStatus = errorCode.getHttpStatus();

            // Then
            assertThat(httpStatus).isBetween(100, 599);  // HTTP 상태 코드 범위
        }

        @ParameterizedTest
        @EnumSource({Bc}ErrorCode.class)
        @DisplayName("모든 ErrorCode는 getMessage()를 구현해야 한다")
        void allErrorCodes_ShouldImplementGetMessage({Bc}ErrorCode errorCode) {
            // When
            String message = errorCode.getMessage();

            // Then
            assertThat(message).isNotNull();
            assertThat(message).isNotBlank();
        }
    }

    @Nested
    @DisplayName("에러 코드 형식 검증 ({BC}-{3자리 숫자})")
    class CodeFormatTests {

        @ParameterizedTest
        @EnumSource({Bc}ErrorCode.class)
        @DisplayName("에러 코드는 {BC}-{3자리 숫자} 형식이어야 한다")
        void errorCode_ShouldFollowNamingConvention({Bc}ErrorCode errorCode) {
            // When
            String code = errorCode.getCode();

            // Then - 형식: {BC}-{3자리 숫자}
            assertThat(code).matches("^{BC}-\\d{3}$");
        }

        @Test
        @DisplayName("TENANT_NOT_FOUND는 'TENANT-001' 형식이어야 한다")
        void tenantNotFound_ShouldHaveCorrectCodeFormat() {
            // When
            String code = {Bc}ErrorCode.{BC}_NOT_FOUND.getCode();

            // Then
            assertThat(code).isEqualTo("{BC}-001");
        }
    }

    @Nested
    @DisplayName("HTTP 상태 코드 매핑 검증")
    class HttpStatusMappingTests {

        @Test
        @DisplayName("{BC}_NOT_FOUND는 404 NOT FOUND를 반환해야 한다")
        void notFound_ShouldReturn404() {
            // When
            int httpStatus = {Bc}ErrorCode.{BC}_NOT_FOUND.getHttpStatus();

            // Then
            assertThat(httpStatus).isEqualTo(404);
        }

        @Test
        @DisplayName("{BC}_NAME_DUPLICATED는 409 CONFLICT를 반환해야 한다")
        void duplicated_ShouldReturn409() {
            // When
            int httpStatus = {Bc}ErrorCode.{BC}_NAME_DUPLICATED.getHttpStatus();

            // Then
            assertThat(httpStatus).isEqualTo(409);
        }

        @Test
        @DisplayName("INVALID_{BC}_STATUS는 400 BAD REQUEST를 반환해야 한다")
        void invalidStatus_ShouldReturn400() {
            // When
            int httpStatus = {Bc}ErrorCode.INVALID_{BC}_STATUS.getHttpStatus();

            // Then
            assertThat(httpStatus).isEqualTo(400);
        }

        @Test
        @DisplayName("{BC}_CREATION_FAILED는 500 INTERNAL SERVER ERROR를 반환해야 한다")
        void creationFailed_ShouldReturn500() {
            // When
            int httpStatus = {Bc}ErrorCode.{BC}_CREATION_FAILED.getHttpStatus();

            // Then
            assertThat(httpStatus).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("에러 메시지 검증")
    class ErrorMessageTests {

        @ParameterizedTest
        @EnumSource({Bc}ErrorCode.class)
        @DisplayName("에러 메시지는 null이 아니어야 한다")
        void errorMessage_ShouldNotBeNull({Bc}ErrorCode errorCode) {
            // When
            String message = errorCode.getMessage();

            // Then
            assertThat(message).isNotNull();
        }

        @Test
        @DisplayName("{BC}_NOT_FOUND 메시지는 'not found'를 포함해야 한다")
        void notFoundMessage_ShouldContainNotFound() {
            // When
            String message = {Bc}ErrorCode.{BC}_NOT_FOUND.getMessage();

            // Then
            assertThat(message.toLowerCase()).contains("not found");
        }

        @Test
        @DisplayName("{BC}_NAME_DUPLICATED 메시지는 'already exists'를 포함해야 한다")
        void duplicatedMessage_ShouldContainAlreadyExists() {
            // When
            String message = {Bc}ErrorCode.{BC}_NAME_DUPLICATED.getMessage();

            // Then
            assertThat(message.toLowerCase()).contains("already exists");
        }
    }
}
```

---

### 2-2) 구체적인 예외 클래스 테스트 템플릿

```java
package com.ryuqq.domain.{bc}.exception;

import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {Bc}NotFoundException 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>DomainException 상속 검증</li>
 *   <li>생성자 파라미터 테스트</li>
 *   <li>에러 코드 매핑 검증</li>
 *   <li>에러 메시지 컨텍스트 정보 포함 검증</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("{Bc}NotFoundException 단위 테스트")
class {Bc}NotFoundExceptionTest {

    @Nested
    @DisplayName("DomainException 상속 검증")
    class DomainExceptionInheritanceTests {

        @Test
        @DisplayName("{Bc}NotFoundException는 DomainException을 상속해야 한다")
        void shouldExtendDomainException() {
            // Given
            {Bc}NotFoundException exception = new {Bc}NotFoundException(1L);

            // Then
            assertThat(exception).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTests {

        @Test
        @DisplayName("파라미터 생성자 - {bc}Id를 포함한 예외 생성")
        void parameterizedConstructor_ShouldInclude{Bc}Id() {
            // Given
            Long {bc}Id = 123L;

            // When
            {Bc}NotFoundException exception = new {Bc}NotFoundException({bc}Id);

            // Then
            assertThat(exception.getMessage()).contains("123");
            assertThat(exception.code()).isEqualTo({Bc}ErrorCode.{BC}_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("기본 생성자 - ErrorCode 기본 메시지 사용")
        void defaultConstructor_ShouldUseDefaultMessage() {
            // When
            {Bc}NotFoundException exception = new {Bc}NotFoundException();

            // Then
            assertThat(exception.getMessage()).isEqualTo({Bc}ErrorCode.{BC}_NOT_FOUND.getMessage());
            assertThat(exception.code()).isEqualTo({Bc}ErrorCode.{BC}_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("에러 코드 매핑 검증")
    class ErrorCodeMappingTests {

        @Test
        @DisplayName("code()는 '{BC}-001'을 반환해야 한다")
        void code_ShouldReturnCorrectErrorCode() {
            // Given
            {Bc}NotFoundException exception = new {Bc}NotFoundException(1L);

            // When
            String code = exception.code();

            // Then
            assertThat(code).isEqualTo("{BC}-001");
        }
    }

    @Nested
    @DisplayName("에러 메시지 검증")
    class ErrorMessageTests {

        @Test
        @DisplayName("에러 메시지는 {bc}Id를 포함해야 한다")
        void errorMessage_ShouldContain{Bc}Id() {
            // Given
            Long {bc}Id = 456L;

            // When
            {Bc}NotFoundException exception = new {Bc}NotFoundException({bc}Id);

            // Then
            assertThat(exception.getMessage())
                .contains("456")
                .contains("{Bc} not found");
        }

        @Test
        @DisplayName("에러 메시지는 비즈니스 용어를 사용해야 한다")
        void errorMessage_ShouldUseBusinessTerms() {
            // Given
            {Bc}NotFoundException exception = new {Bc}NotFoundException(1L);

            // Then
            assertThat(exception.getMessage())
                .doesNotContain("NullPointerException")
                .doesNotContain("SQL")
                .doesNotContain("Database");
        }
    }
}
```

---

### 2-3) args를 사용하는 복잡한 예외 클래스 테스트 템플릿

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * OrderCancellationException 단위 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>args 매핑 검증 (orderId, currentStatus)</li>
 *   <li>에러 메시지 컨텍스트 정보 포함 검증</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("OrderCancellationException 단위 테스트")
class OrderCancellationExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTests {

        @Test
        @DisplayName("파라미터 생성자 - orderId와 currentStatus를 포함한 예외 생성")
        void parameterizedConstructor_ShouldIncludeOrderIdAndStatus() {
            // Given
            Long orderId = 123L;
            String currentStatus = "SHIPPED";

            // When
            OrderCancellationException exception = new OrderCancellationException(orderId, currentStatus);

            // Then
            assertThat(exception.getMessage())
                .contains("123")
                .contains("SHIPPED")
                .contains("Cannot cancel order");
            assertThat(exception.code()).isEqualTo(OrderErrorCode.CANNOT_CANCEL_ORDER.getCode());
        }
    }

    @Nested
    @DisplayName("args 매핑 검증")
    class ArgsMappingTests {

        @Test
        @DisplayName("args()는 orderId를 포함해야 한다")
        void args_ShouldContainOrderId() {
            // Given
            Long orderId = 456L;
            String currentStatus = "COMPLETED";

            // When
            OrderCancellationException exception = new OrderCancellationException(orderId, currentStatus);
            Map<String, Object> args = exception.args();

            // Then
            assertThat(args).containsKey("orderId");
            assertThat(args.get("orderId")).isEqualTo(456L);
        }

        @Test
        @DisplayName("args()는 currentStatus를 포함해야 한다")
        void args_ShouldContainCurrentStatus() {
            // Given
            Long orderId = 789L;
            String currentStatus = "CANCELLED";

            // When
            OrderCancellationException exception = new OrderCancellationException(orderId, currentStatus);
            Map<String, Object> args = exception.args();

            // Then
            assertThat(args).containsKey("currentStatus");
            assertThat(args.get("currentStatus")).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("args()는 모든 필수 정보를 포함해야 한다")
        void args_ShouldContainAllRequiredInfo() {
            // Given
            Long orderId = 999L;
            String currentStatus = "PENDING";

            // When
            OrderCancellationException exception = new OrderCancellationException(orderId, currentStatus);
            Map<String, Object> args = exception.args();

            // Then
            assertThat(args)
                .hasSize(2)
                .containsKeys("orderId", "currentStatus");
        }
    }

    @Nested
    @DisplayName("에러 메시지 검증")
    class ErrorMessageTests {

        @Test
        @DisplayName("에러 메시지는 orderId와 currentStatus를 포함해야 한다")
        void errorMessage_ShouldContainOrderIdAndStatus() {
            // Given
            Long orderId = 555L;
            String currentStatus = "DELIVERED";

            // When
            OrderCancellationException exception = new OrderCancellationException(orderId, currentStatus);

            // Then
            assertThat(exception.getMessage())
                .contains("555")
                .contains("DELIVERED");
        }
    }
}
```

---

### 2-4) Domain Layer 예외 발생 테스트 (Aggregate/VO)

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.order.exception.OrderCancellationException;
import com.ryuqq.domain.order.exception.InvalidOrderStateException;
import com.ryuqq.domain.order.mother.Orders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Order Aggregate 예외 발생 테스트
 *
 * <p>테스트 전략:</p>
 * <ul>
 *   <li>IllegalArgumentException (생성자 검증)</li>
 *   <li>DomainException 서브클래스 (비즈니스 메서드)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("Order Aggregate 예외 발생 테스트")
class OrderExceptionTest {

    @Nested
    @DisplayName("IllegalArgumentException 발생 테스트 (생성자 검증)")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("of() - ID가 null이면 IllegalArgumentException 발생")
        void of_WithNullId_ShouldThrowIllegalArgumentException() {
            // When & Then
            assertThatThrownBy(() -> Order.of(null, CustomerId.of(1L), OrderStatus.PENDING, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null일 수 없습니다");
        }

        @Test
        @DisplayName("forNew() - CustomerId가 null이면 IllegalArgumentException 발생")
        void forNew_WithNullCustomerId_ShouldThrowIllegalArgumentException() {
            // When & Then
            assertThatThrownBy(() -> Order.forNew(null, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CustomerId must not be null");
        }
    }

    @Nested
    @DisplayName("DomainException 서브클래스 발생 테스트 (비즈니스 메서드)")
    class DomainExceptionTests {

        @Test
        @DisplayName("confirm() - 이미 CONFIRMED 상태면 InvalidOrderStateException 발생")
        void confirm_WhenAlreadyConfirmed_ShouldThrowInvalidOrderStateException() {
            // Given
            Order order = Orders.confirmedOrder();

            // When & Then
            assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Cannot confirm order")
                .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("cancel() - SHIPPED 상태에서 OrderCancellationException 발생")
        void cancel_FromShippedStatus_ShouldThrowOrderCancellationException() {
            // Given
            Order order = Orders.shippedOrder();

            // When & Then
            assertThatThrownBy(order::cancel)
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("Cannot cancel order")
                .hasMessageContaining("SHIPPED");
        }

        @Test
        @DisplayName("ship() - PENDING 상태에서 InvalidOrderStateException 발생")
        void ship_FromPendingStatus_ShouldThrowInvalidOrderStateException() {
            // Given
            Order order = Orders.pendingOrder();

            // When & Then
            assertThatThrownBy(order::ship)
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Cannot ship order")
                .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("예외 메시지 컨텍스트 정보 검증")
    class ExceptionMessageContextTests {

        @Test
        @DisplayName("예외 메시지는 orderId를 포함해야 한다")
        void exceptionMessage_ShouldContainOrderId() {
            // Given
            Order order = Orders.shippedOrder();

            // When & Then
            assertThatThrownBy(order::cancel)
                .isInstanceOf(OrderCancellationException.class)
                .satisfies(exception -> {
                    OrderCancellationException e = (OrderCancellationException) exception;
                    assertThat(e.args()).containsKey("orderId");
                });
        }

        @Test
        @DisplayName("예외 메시지는 currentStatus를 포함해야 한다")
        void exceptionMessage_ShouldContainCurrentStatus() {
            // Given
            Order order = Orders.confirmedOrder();

            // When & Then
            assertThatThrownBy(order::ship)
                .isInstanceOf(InvalidOrderStateException.class)
                .satisfies(exception -> {
                    InvalidOrderStateException e = (InvalidOrderStateException) exception;
                    assertThat(e.args()).containsKey("currentStatus");
                    assertThat(e.args().get("currentStatus")).isEqualTo("CONFIRMED");
                });
        }
    }
}
```

---

## 3️⃣ 테스트 패턴

### 패턴 1: ErrorCode Enum 검증 테스트

**목적**: ErrorCode Enum의 구현과 형식을 검증합니다.

**원칙**:
- ErrorCode 인터페이스 구현 (getCode, getHttpStatus, getMessage)
- 에러 코드 형식 ({BC}-{3자리 숫자})
- HTTP 상태 코드 매핑 (404, 400, 409, 500 등)
- 에러 메시지 null 체크

**테스트 케이스**:
```java
// 1) ErrorCode 인터페이스 구현
@ParameterizedTest
@EnumSource(TenantErrorCode.class)
@DisplayName("모든 ErrorCode는 getCode()를 구현해야 한다")
void allErrorCodes_ShouldImplementGetCode(TenantErrorCode errorCode) {
    // 모든 ErrorCode에 대해 getCode() 검증
}

// 2) 에러 코드 형식 검증
@ParameterizedTest
@EnumSource(TenantErrorCode.class)
@DisplayName("에러 코드는 {BC}-{3자리 숫자} 형식이어야 한다")
void errorCode_ShouldFollowNamingConvention(TenantErrorCode errorCode) {
    // 형식: TENANT-001, ORDER-002 등
}

// 3) HTTP 상태 코드 매핑
@Test
@DisplayName("TENANT_NOT_FOUND는 404 NOT FOUND를 반환해야 한다")
void notFound_ShouldReturn404() {
    // 404 매핑 검증
}

// 4) 에러 메시지 null 체크
@ParameterizedTest
@EnumSource(TenantErrorCode.class)
@DisplayName("에러 메시지는 null이 아니어야 한다")
void errorMessage_ShouldNotBeNull(TenantErrorCode errorCode) {
    // null 체크
}
```

---

### 패턴 2: 구체적인 예외 클래스 검증 테스트

**목적**: DomainException 서브클래스의 생성자와 에러 코드 매핑을 검증합니다.

**원칙**:
- DomainException 상속 검증
- 생성자 파라미터 테스트 (파라미터 생성자, 기본 생성자)
- 에러 코드 매핑 검증
- args 매핑 검증 (복잡한 예외)

**테스트 케이스**:
```java
// 1) DomainException 상속
@Test
@DisplayName("TenantNotFoundException는 DomainException을 상속해야 한다")
void shouldExtendDomainException() {
    TenantNotFoundException exception = new TenantNotFoundException(1L);
    assertThat(exception).isInstanceOf(DomainException.class);
}

// 2) 파라미터 생성자
@Test
@DisplayName("파라미터 생성자 - tenantId를 포함한 예외 생성")
void parameterizedConstructor_ShouldIncludeTenantId() {
    // tenantId 포함 여부 검증
}

// 3) 기본 생성자
@Test
@DisplayName("기본 생성자 - ErrorCode 기본 메시지 사용")
void defaultConstructor_ShouldUseDefaultMessage() {
    // 기본 메시지 사용 검증
}

// 4) 에러 코드 매핑
@Test
@DisplayName("code()는 'TENANT-001'을 반환해야 한다")
void code_ShouldReturnCorrectErrorCode() {
    // 에러 코드 매핑 검증
}

// 5) args 매핑 (복잡한 예외)
@Test
@DisplayName("args()는 orderId를 포함해야 한다")
void args_ShouldContainOrderId() {
    // args 매핑 검증
}
```

---

### 패턴 3: Domain Layer 예외 발생 테스트

**목적**: Aggregate/VO에서 예외가 올바르게 발생하는지 검증합니다.

**원칙**:
- IllegalArgumentException (생성자, Compact Constructor 검증)
- DomainException 서브클래스 (비즈니스 메서드)
- 예외 메시지 컨텍스트 정보 (orderId, currentStatus 등)

**테스트 케이스**:
```java
// 1) IllegalArgumentException (생성자)
@Test
@DisplayName("of() - ID가 null이면 IllegalArgumentException 발생")
void of_WithNullId_ShouldThrowIllegalArgumentException() {
    // null 파라미터 검증
}

// 2) DomainException 서브클래스 (비즈니스 메서드)
@Test
@DisplayName("cancel() - SHIPPED 상태에서 OrderCancellationException 발생")
void cancel_FromShippedStatus_ShouldThrowOrderCancellationException() {
    // 비즈니스 룰 위반 시 예외 발생 검증
}

// 3) 예외 메시지 컨텍스트 정보
@Test
@DisplayName("예외 메시지는 orderId와 currentStatus를 포함해야 한다")
void exceptionMessage_ShouldContainOrderIdAndStatus() {
    // 컨텍스트 정보 포함 검증
}
```

---

## 4️⃣ 테스트 유형별 전략

각 Exception 유형은 위의 3가지 패턴을 조합하여 테스트합니다.

| Exception 유형 | @Nested 클래스 구성 | 특징 |
|----------------|---------------------|------|
| **ErrorCode Enum** | ErrorCodeInterfaceTests, CodeFormatTests, HttpStatusMappingTests, ErrorMessageTests | @ParameterizedTest 활용 |
| **구체적인 예외 클래스** | DomainExceptionInheritanceTests, ConstructorTests, ErrorCodeMappingTests, ErrorMessageTests | args 매핑 추가 (복잡한 예외) |
| **Domain Layer 예외 발생** | IllegalArgumentExceptionTests, DomainExceptionTests, ExceptionMessageContextTests | Object Mother 패턴 활용 |

### ErrorCode Enum
- **패턴 1**: ErrorCode 인터페이스 구현
- **패턴 1**: 에러 코드 형식 검증
- **패턴 1**: HTTP 상태 코드 매핑

### 구체적인 예외 클래스
- **패턴 2**: DomainException 상속 검증
- **패턴 2**: 생성자 파라미터 테스트
- **패턴 2**: 에러 코드 매핑 검증
- **패턴 2**: args 매핑 검증 (복잡한 예외)

### Domain Layer 예외 발생
- **패턴 3**: IllegalArgumentException (생성자)
- **패턴 3**: DomainException 서브클래스 (비즈니스 메서드)
- **패턴 3**: 예외 메시지 컨텍스트 정보

---

## 5️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ @Tag 누락
@DisplayName("TenantErrorCode 테스트")
class TenantErrorCodeTest {  // ❌ @Tag("unit"), @Tag("domain"), @Tag("exception") 없음
}

// ❌ @Nested 없이 평면 구조
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("TenantErrorCode 테스트")
class TenantErrorCodeTest {
    @Test
    void testGetCode() { }  // ❌ 관심사 분리 없음

    @Test
    void testGetHttpStatus() { }

    @Test
    void testGetMessage() { }
}

// ❌ 예외 발생 검증 생략
@Test
void cancel_FromShippedStatus() {
    Order order = Orders.shippedOrder();
    order.cancel();  // ❌ 예외 발생 검증 없음
}

// ❌ 예외 메시지 검증 생략
@Test
void shouldThrowException() {
    assertThatThrownBy(() -> Order.of(null, customerId, status, clock))
        .isInstanceOf(IllegalArgumentException.class);  // ❌ 메시지 검증 없음
}

// ❌ @ParameterizedTest 사용하지 않음 (ErrorCode Enum)
@Test
void testAllErrorCodes() {
    // 각 ErrorCode에 대해 수동으로 테스트 ❌
    assertThat(TenantErrorCode.TENANT_NOT_FOUND.getCode()).isNotNull();
    assertThat(TenantErrorCode.TENANT_NAME_DUPLICATED.getCode()).isNotNull();
    // ...
}
```

### ✅ Good Examples

```java
// ✅ @Tag 필수 3종 세트
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("TenantErrorCode Enum 단위 테스트")
class TenantErrorCodeTest {
    // ...
}

// ✅ @Nested로 관심사 명확히 분리
@Tag("unit")
@Tag("domain")
@Tag("exception")
@DisplayName("TenantErrorCode Enum 단위 테스트")
class TenantErrorCodeTest {

    @Nested
    @DisplayName("ErrorCode 인터페이스 구현 검증")
    class ErrorCodeInterfaceTests {
        // getCode, getHttpStatus, getMessage 테스트
    }

    @Nested
    @DisplayName("에러 코드 형식 검증 ({BC}-{3자리 숫자})")
    class CodeFormatTests {
        // 형식 검증 테스트
    }

    @Nested
    @DisplayName("HTTP 상태 코드 매핑 검증")
    class HttpStatusMappingTests {
        // HTTP 상태 코드 매핑 테스트
    }
}

// ✅ 예외 발생 검증
@Test
@DisplayName("cancel() - SHIPPED 상태에서 OrderCancellationException 발생")
void cancel_FromShippedStatus_ShouldThrowOrderCancellationException() {
    // Given
    Order order = Orders.shippedOrder();

    // When & Then
    assertThatThrownBy(order::cancel)
        .isInstanceOf(OrderCancellationException.class)
        .hasMessageContaining("Cannot cancel order")
        .hasMessageContaining("SHIPPED");
}

// ✅ 예외 메시지 검증
@Test
@DisplayName("of() - ID가 null이면 IllegalArgumentException 발생")
void of_WithNullId_ShouldThrowIllegalArgumentException() {
    // When & Then
    assertThatThrownBy(() -> Order.of(null, customerId, status, clock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null일 수 없습니다");
}

// ✅ @ParameterizedTest 사용 (ErrorCode Enum)
@ParameterizedTest
@EnumSource(TenantErrorCode.class)
@DisplayName("모든 ErrorCode는 getCode()를 구현해야 한다")
void allErrorCodes_ShouldImplementGetCode(TenantErrorCode errorCode) {
    // When
    String code = errorCode.getCode();

    // Then
    assertThat(code).isNotNull().isNotBlank();
}
```

---

## 6️⃣ 체크리스트

Domain Exception 테스트 작성 후 다음을 확인:

### ErrorCode Enum 테스트
- [ ] **@Tag 3종 세트** (@Tag("unit"), @Tag("domain"), @Tag("exception"))
- [ ] **@Nested 클래스로 관심사 분리** (ErrorCodeInterfaceTests, CodeFormatTests, HttpStatusMappingTests, ErrorMessageTests)
- [ ] **@ParameterizedTest 사용** (모든 ErrorCode에 대해)
- [ ] ErrorCode 인터페이스 구현 검증 (getCode, getHttpStatus, getMessage)
- [ ] 에러 코드 형식 검증 ({BC}-{3자리 숫자})
- [ ] HTTP 상태 코드 매핑 검증 (404, 400, 409, 500 등)
- [ ] 에러 메시지 null 체크

### 구체적인 예외 클래스 테스트
- [ ] **@Tag 3종 세트** (@Tag("unit"), @Tag("domain"), @Tag("exception"))
- [ ] **@Nested 클래스로 관심사 분리** (DomainExceptionInheritanceTests, ConstructorTests, ErrorCodeMappingTests, ArgsMappingTests, ErrorMessageTests)
- [ ] DomainException 상속 검증
- [ ] 파라미터 생성자 테스트
- [ ] 기본 생성자 테스트 (있는 경우)
- [ ] 에러 코드 매핑 검증 (code())
- [ ] args 매핑 검증 (복잡한 예외)
- [ ] 에러 메시지 컨텍스트 정보 포함 검증

### Domain Layer 예외 발생 테스트
- [ ] **@Tag 3종 세트** (@Tag("unit"), @Tag("domain"), @Tag("exception"))
- [ ] **@Nested 클래스로 관심사 분리** (IllegalArgumentExceptionTests, DomainExceptionTests, ExceptionMessageContextTests)
- [ ] IllegalArgumentException 발생 테스트 (생성자/Compact Constructor)
- [ ] DomainException 서브클래스 발생 테스트 (비즈니스 메서드)
- [ ] 예외 메시지 컨텍스트 정보 검증 (orderId, currentStatus 등)
- [ ] Object Mother 패턴 활용 (비즈니스 시나리오 명확)

---

## 7️⃣ 참고: GlobalExceptionHandler는 Adapter Layer에서 테스트

**중요**: GlobalExceptionHandler는 **Adapter (REST API) Layer**에서 테스트합니다.

Domain Layer 테스트에서는:
- ✅ ErrorCode Enum 테스트
- ✅ 구체적인 예외 클래스 테스트
- ✅ Domain Layer 예외 발생 테스트

**❌ Domain Layer 테스트에서 제외**:
- GlobalExceptionHandler (REST API Layer)
- HTTP 응답 생성 (ErrorResponse)
- Controller에서 예외 처리

**GlobalExceptionHandler 테스트는 별도로 진행**:
- `adapter-in/rest-api/exception/GlobalExceptionHandlerTest.java`
- `@WebMvcTest` 또는 통합 테스트
- HTTP 응답 검증 (status code, body)

---

**✅ Domain Exception 테스트는 @Tag 필수, @Nested로 관심사 분리, @ParameterizedTest 활용이 핵심입니다!**

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
