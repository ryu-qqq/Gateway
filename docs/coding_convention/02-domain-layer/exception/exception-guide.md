# Domain Exception 설계 가이드

> **2-Tier Exception 시스템**
>
> Domain 순수성을 유지하면서 비즈니스 에러를 명확히 표현하는 예외 처리 전략을 정의합니다.

---

## 1) 핵심 원칙: Domain에서 직접 던지기

### 전략 개요

```
┌─────────────────────────────────────────────────────────────┐
│ Domain Layer: 예외를 직접 던진다                            │
├─────────────────────────────────────────────────────────────┤
│ 1. 내부 불변조건 위반 (개발자 오류)                         │
│    → IllegalArgumentException (JDK 기본)                    │
│    → HTTP 500 (서버 내부 오류)                              │
│                                                              │
│ 2. 클라이언트 입력 검증 실패                                │
│    → DomainException 상속 클래스                            │
│    → HTTP 400 (Bad Request)                                 │
│                                                              │
│ 3. 비즈니스 룰 위반                                         │
│    → DomainException 상속 클래스                            │
│    → HTTP 매핑 필요 (404, 400, 409 등)                      │
│                                                              │
│ ✅ 도메인에서 직접 던지므로 UseCase는 그냥 전파             │
│ ✅ try-catch 보일러플레이트 제거                            │
│ ✅ GlobalExceptionHandler가 자동으로 HTTP 매핑              │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ Application Layer: 그냥 전파 (try-catch 불필요)             │
├─────────────────────────────────────────────────────────────┤
│ - UseCase는 Domain 메서드 호출만                            │
│ - 예외 발생 시 자동 전파                                     │
│ - GlobalExceptionHandler가 HTTP 응답 생성                   │
└─────────────────────────────────────────────────────────────┘
```

### 예외 분류 체계

| 상황 | 예외 종류 | 용도 | HTTP 매핑 |
|------|-----------|------|-----------|
| **내부 불변조건 위반** | IllegalArgumentException | 개발자 오류, 절대 발생하면 안 됨 | 500 |
| **클라이언트 입력 검증** | XxxValidationException | 잘못된 입력값 | 400 |
| **비즈니스 룰 위반** | DomainException 상속 | 상태 전환 불가, 조건 미충족 | 400, 409 |
| **엔티티 못 찾음** | XxxNotFoundException | Repository 조회 실패 | 404 |
| **상태 전환 불가** | XxxInvalidStateException | 비즈니스 메서드 조건 미충족 | 400 |

### IllegalArgumentException vs DomainException 구분 기준

```
┌─────────────────────────────────────────────────────────────┐
│ 질문: 이 예외가 발생하면 누구의 잘못인가?                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ 개발자의 잘못 (버그)                                        │
│ ├─ 프로덕션에서 절대 발생하면 안 됨                        │
│ ├─ 테스트에서 잡아야 함                                     │
│ ├─ 예: null이 절대 들어오면 안 되는 곳에 null               │
│ └─ → IllegalArgumentException (500)                         │
│                                                              │
│ 클라이언트의 잘못 (잘못된 요청)                             │
│ ├─ 프로덕션에서 발생 가능                                   │
│ ├─ 클라이언트에게 명확한 에러 메시지 필요                  │
│ ├─ 예: 이메일 형식 틀림, 금액 음수 등                       │
│ └─ → DomainException 상속 (400)                             │
│                                                              │
│ 비즈니스 규칙 위반                                          │
│ ├─ 프로덕션에서 발생 가능                                   │
│ ├─ 현재 상태에서 수행 불가능한 작업                        │
│ ├─ 예: 이미 배송된 주문 취소, 잔액 부족 등                 │
│ └─ → DomainException 상속 (400, 404, 409)                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2) Domain Layer 예외 처리

### 2-1) IllegalArgumentException 사용 시점

**용도**: **절대 발생하면 안 되는 내부 불변조건 위반** (개발자 오류)

**원칙**:
- ✅ **null이 절대 들어오면 안 되는 내부 메서드**: `if (value == null) throw new IllegalArgumentException(...)`
- ✅ **외부에서 호출 불가능한 private 메서드의 검증**
- ✅ **테스트로 커버되어야 하는 버그**
- ❌ **클라이언트 입력 검증에는 사용 금지** → DomainException 사용

**예시: 내부 불변조건 위반 (개발자 오류)**

```java
package com.ryuqq.domain.order.aggregate.order;

/**
 * Order Aggregate Root
 *
 * @author development-team
 * @since 1.0.0
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final Clock clock;

    /**
     * Private 생성자 - 내부 불변조건 검증
     *
     * <p>이 검증은 개발자 오류를 잡기 위한 것입니다.
     * 정상적인 코드 흐름에서는 절대 발생하면 안 됩니다.</p>
     *
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우 (개발자 오류)
     */
    private Order(OrderId id, CustomerId customerId, OrderStatus status, Clock clock) {
        // ✅ 내부 불변조건: 개발자 오류 → IllegalArgumentException
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId must not be null - this is a bug");
        }
        if (status == null) {
            throw new IllegalArgumentException("OrderStatus must not be null - this is a bug");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null - this is a bug");
        }

        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.clock = clock;
    }

    /**
     * 신규 주문 생성
     */
    public static Order forNew(CustomerId customerId, Clock clock) {
        return new Order(null, customerId, OrderStatus.PENDING, clock);
    }
}
```

### 2-2) DomainException 사용 시점

**용도**: **클라이언트 입력 검증 실패** 또는 **비즈니스 룰 위반** (HTTP 매핑 필요)

**원칙**:
- ✅ **클라이언트 입력 검증**: 이메일 형식, 금액 범위 등 → 400 Bad Request
- ✅ **비즈니스 룰 위반**: 상태 전환 불가, 조건 미충족 → 400, 409
- ✅ **엔티티 조회 실패**: Repository에서 못 찾음 → 404 Not Found
- ✅ **명확한 에러 코드와 메시지**: 클라이언트가 이해 가능

**예시: 클라이언트 입력 검증 (VO)**

```java
package com.ryuqq.domain.order.vo;

import com.ryuqq.domain.order.exception.MoneyValidationException;

/**
 * Money - 금액을 표현하는 Value Object
 *
 * <p>음수 금액을 허용하지 않으며, 불변 객체로 설계되었습니다.</p>
 *
 * @param amount 금액 (0 이상)
 * @author development-team
 * @since 1.0.0
 */
public record Money(long amount) {

    /**
     * Compact Constructor - 금액 검증
     *
     * <p>클라이언트가 음수 금액을 입력할 수 있으므로 DomainException으로 처리합니다.</p>
     *
     * @throws MoneyValidationException 금액이 음수인 경우 (클라이언트 입력 오류, 400)
     */
    public Money {
        // ✅ 클라이언트 입력 검증 → DomainException (400)
        if (amount < 0) {
            throw new MoneyValidationException(amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public static final Money ZERO = new Money(0);
}
```

**예시: 비즈니스 룰 위반 (Aggregate)**

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.order.exception.OrderInvalidStateException;
import com.ryuqq.domain.order.exception.OrderCancellationException;

/**
 * Order Aggregate Root - 비즈니스 메서드
 *
 * @author development-team
 * @since 1.0.0
 */
public class Order {

    // 필드 생략...

    /**
     * 주문 확정
     *
     * @throws OrderInvalidStateException 확정 불가능한 상태인 경우 (400)
     */
    public void confirm() {
        if (!canConfirm()) {
            // ✅ 비즈니스 룰 위반 → DomainException (400)
            throw OrderInvalidStateException.cannotConfirm(
                this.id.value(),
                this.status.name()
            );
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = clock.instant();
    }

    /**
     * 주문 취소
     *
     * @throws OrderCancellationException 취소 불가능한 상태인 경우 (400)
     */
    public void cancel() {
        if (!canCancel()) {
            // ✅ 비즈니스 룰 위반 → DomainException (400)
            throw new OrderCancellationException(
                this.id.value(),
                this.status.name()
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = clock.instant();
    }

    private boolean canConfirm() {
        return this.status == OrderStatus.PENDING && !this.lineItems.isEmpty();
    }

    private boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }
}
```

---

## 3) DomainException 기본 클래스

### 3-1) DomainException 설계 (Domain Layer 순수성 유지)

**핵심 원칙**:
- ✅ **Spring 의존성 금지**: `HttpStatus` 대신 `int httpStatus` 사용
- ✅ **ErrorCode 객체 기반**: 에러 코드, HTTP 상태, 메시지를 캡슐화
- ✅ **RuntimeException 상속**: Unchecked Exception으로 try-catch 강제 없음
- ✅ **args Map 지원**: 디버깅을 위한 컨텍스트 정보

**위치**: `domain/common/exception/`

```java
package com.ryuqq.domain.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * DomainException - Domain Layer 예외의 최상위 클래스
 *
 * <p>모든 비즈니스 예외는 이 클래스를 상속해야 합니다.</p>
 *
 * <p><strong>설계 원칙:</strong></p>
 * <ul>
 *   <li>Spring 의존성 금지 (HttpStatus 대신 int 사용)</li>
 *   <li>ErrorCode 객체 기반 (에러 코드, HTTP 상태, 메시지 캡슐화)</li>
 *   <li>RuntimeException 상속 (Unchecked Exception)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> args;

    /**
     * Constructor - ErrorCode 기반 예외 생성
     *
     * @param errorCode 에러 코드 (필수)
     */
    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = Collections.emptyMap();
    }

    /**
     * Constructor - ErrorCode + 커스텀 메시지
     *
     * @param errorCode 에러 코드 (필수)
     * @param message 커스텀 에러 메시지
     */
    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = Collections.emptyMap();
    }

    /**
     * Constructor - ErrorCode + 커스텀 메시지 + 컨텍스트 정보
     *
     * @param errorCode 에러 코드 (필수)
     * @param message 커스텀 에러 메시지
     * @param args 디버깅용 컨텍스트 정보
     */
    protected DomainException(ErrorCode errorCode, String message, Map<String, Object> args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args != null ? Map.copyOf(args) : Collections.emptyMap();
    }

    /**
     * 에러 코드 반환
     *
     * @return ErrorCode 객체
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 문자열 반환 (편의 메서드)
     *
     * @return 에러 코드 문자열 (예: "ORDER-001")
     */
    public String code() {
        return errorCode.getCode();
    }

    /**
     * HTTP 상태 코드 반환 (편의 메서드)
     *
     * @return HTTP 상태 코드 (예: 404, 400, 409)
     */
    public int httpStatus() {
        return errorCode.getHttpStatus();
    }

    /**
     * 컨텍스트 정보 반환
     *
     * @return 디버깅용 컨텍스트 정보 (불변 Map)
     */
    public Map<String, Object> args() {
        return args;
    }
}
```

### 3-2) ErrorCode 인터페이스

**위치**: `domain/common/exception/`

```java
package com.ryuqq.domain.common.exception;

/**
 * ErrorCode - 에러 코드 인터페이스
 *
 * <p>모든 ErrorCode Enum은 이 인터페이스를 구현해야 합니다.</p>
 *
 * <p><strong>설계 원칙:</strong></p>
 * <ul>
 *   <li>Spring 의존성 금지: HttpStatus 대신 int 사용</li>
 *   <li>에러 코드 형식: {BC}-{3자리 숫자} (예: ORDER-001)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface ErrorCode {

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 문자열 (예: "ORDER-001", "TENANT-002")
     */
    String getCode();

    /**
     * HTTP 상태 코드 반환
     *
     * <p>Spring HttpStatus 대신 int를 사용하여 Domain Layer 순수성을 유지합니다.</p>
     *
     * @return HTTP 상태 코드 (예: 404, 400, 409, 500)
     */
    int getHttpStatus();

    /**
     * 기본 에러 메시지 반환
     *
     * @return 에러 메시지 문자열
     */
    String getMessage();
}
```

---

## 4) ErrorCode Enum 작성법

### 4-1) Bounded Context별 ErrorCode Enum

**용도**: Bounded Context별 에러 코드 정의 (HTTP 상태 코드 매핑)

**위치**: `domain/{boundedContext}/exception/{BoundedContext}ErrorCode.java`

**원칙**:
- ✅ **ErrorCode 인터페이스 구현**: `implements ErrorCode`
- ✅ **int httpStatus 사용**: Spring `HttpStatus` 금지 (Domain 순수성)
- ✅ **형식**: `{BC}-{3자리 숫자}` (예: TENANT-001, ORDER-001)
- ✅ **명확한 메시지**: 사용자가 이해 가능한 에러 메시지

**템플릿: ErrorCode Enum**

```java
package com.ryuqq.domain.tenant.exception;

import com.ryuqq.domain.common.exception.ErrorCode;

/**
 * TenantErrorCode - Tenant Bounded Context 에러 코드
 *
 * <p>Tenant 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.</p>
 *
 * <p><strong>에러 코드 규칙:</strong></p>
 * <ul>
 *   <li>형식: TENANT-{3자리 숫자}</li>
 *   <li>HTTP 상태 코드는 int 사용 (Spring HttpStatus 금지)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum TenantErrorCode implements ErrorCode {

    // === 404 Not Found ===
    TENANT_NOT_FOUND("TENANT-001", 404, "Tenant not found"),

    // === 400 Bad Request (Validation) ===
    INVALID_TENANT_NAME("TENANT-010", 400, "Invalid tenant name"),
    INVALID_TENANT_STATUS("TENANT-011", 400, "Invalid tenant status"),

    // === 409 Conflict ===
    TENANT_NAME_DUPLICATED("TENANT-020", 409, "Tenant name already exists"),

    // === 500 Internal Server Error ===
    TENANT_CREATION_FAILED("TENANT-050", 500, "Failed to create tenant");

    private final String code;
    private final int httpStatus;  // ✅ int 사용 (Spring HttpStatus 금지)
    private final String message;

    TenantErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

### 4-2) ErrorCode 번호 체계 권장 사항

```
{BC}-0XX: 404 Not Found (리소스 못 찾음)
{BC}-01X: 400 Bad Request (입력 검증 실패)
{BC}-02X: 409 Conflict (중복, 충돌)
{BC}-03X: 400 Bad Request (비즈니스 룰 위반)
{BC}-05X: 500 Internal Server Error (내부 오류)
```

---

## 5) 구체적인 예외 클래스 작성법

### 5-1) 언제 만들어야 하는가?

**✅ 구체적인 예외 클래스를 만들어야 하는 경우**:

1. **명확한 비즈니스 시나리오**
   - ✅ `OrderNotFoundException`: "주문을 찾을 수 없음"이 명확한 시나리오
   - ✅ `OrderCancellationException`: "주문 취소 불가"가 명확한 시나리오

2. **고유한 컨텍스트 정보 필요**
   - ✅ `PaymentDeclinedException(orderId, paymentId, reason)`: 3개 파라미터 필요
   - ❌ 단순 ID만: DomainException 직접 사용 고려

3. **타입 기반 예외 처리 필요**
   - ✅ GlobalExceptionHandler에서 특정 예외 타입으로 분기 필요
   - ✅ 테스트에서 특정 예외 타입 검증 필요

**❌ 불필요한 예외 클래스 (Over-Engineering)**:

```java
// ❌ Bad: 너무 세분화된 예외
public class TenantIdNullException extends DomainException { }
public class TenantNameNullException extends DomainException { }
public class TenantStatusNullException extends DomainException { }

// ✅ Good: 하나의 Validation 예외로 통합
public class TenantValidationException extends DomainException {
    public TenantValidationException(String field, String reason) {
        super(TenantErrorCode.INVALID_TENANT_FIELD,
              String.format("Invalid %s: %s", field, reason));
    }
}
```

### 5-2) 구체적인 예외 클래스 템플릿

**Not Found 예외 템플릿**:

```java
package com.ryuqq.domain.tenant.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * TenantNotFoundException - Tenant를 찾을 수 없을 때 발생
 *
 * <p>HTTP 응답: 404 NOT FOUND</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class TenantNotFoundException extends DomainException {

    public TenantNotFoundException(Long tenantId) {
        super(
            TenantErrorCode.TENANT_NOT_FOUND,
            String.format("Tenant not found: %d", tenantId),
            Map.of("tenantId", tenantId)
        );
    }

    public TenantNotFoundException(String tenantName) {
        super(
            TenantErrorCode.TENANT_NOT_FOUND,
            String.format("Tenant not found with name: %s", tenantName),
            Map.of("tenantName", tenantName)
        );
    }
}
```

**Validation 예외 템플릿**:

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * MoneyValidationException - 금액 검증 실패 시 발생
 *
 * <p>HTTP 응답: 400 BAD REQUEST</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class MoneyValidationException extends DomainException {

    public MoneyValidationException(long invalidAmount) {
        super(
            OrderErrorCode.INVALID_MONEY_AMOUNT,
            String.format("Money amount must not be negative: %d", invalidAmount),
            Map.of("invalidAmount", invalidAmount)
        );
    }
}
```

**상태 전환 예외 템플릿**:

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * OrderInvalidStateException - 주문 상태 전환 불가 시 발생
 *
 * <p>HTTP 응답: 400 BAD REQUEST</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderInvalidStateException extends DomainException {

    private OrderInvalidStateException(String message, Map<String, Object> args) {
        super(OrderErrorCode.INVALID_ORDER_STATE, message, args);
    }

    public static OrderInvalidStateException cannotConfirm(Long orderId, String currentStatus) {
        return new OrderInvalidStateException(
            String.format("Cannot confirm order %d. Current status: %s", orderId, currentStatus),
            Map.of("orderId", orderId, "currentStatus", currentStatus, "action", "confirm")
        );
    }

    public static OrderInvalidStateException cannotShip(Long orderId, String currentStatus) {
        return new OrderInvalidStateException(
            String.format("Cannot ship order %d. Current status: %s", orderId, currentStatus),
            Map.of("orderId", orderId, "currentStatus", currentStatus, "action", "ship")
        );
    }
}
```

**취소 예외 템플릿**:

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * OrderCancellationException - 주문 취소 불가 시 발생
 *
 * <p>HTTP 응답: 400 BAD REQUEST</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderCancellationException extends DomainException {

    public OrderCancellationException(Long orderId, String currentStatus) {
        super(
            OrderErrorCode.CANNOT_CANCEL_ORDER,
            String.format("Cannot cancel order %d. Current status: %s", orderId, currentStatus),
            Map.of("orderId", orderId, "currentStatus", currentStatus)
        );
    }
}
```

---

## 6) UseCase에서 예외 처리 (그냥 전파)

### 핵심 원칙: try-catch 불필요

**전략**: Domain에서 이미 구체적인 DomainException을 던지므로, UseCase는 그냥 전파만 합니다.

**이유**:
- ✅ **Domain이 책임**: 비즈니스 룰 위반 시 Domain에서 직접 구체적인 예외 던짐
- ✅ **UseCase는 단순**: Domain 메서드 호출만, 예외는 자동 전파
- ✅ **GlobalExceptionHandler가 처리**: ErrorCode에서 HTTP 상태 코드 자동 추출
- ✅ **보일러플레이트 제거**: 모든 UseCase에 try-catch 쓸 필요 없음

**템플릿: UseCase (예외 그냥 전파)**

```java
package com.ryuqq.application.order.usecase;

import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.exception.OrderNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CancelOrderService - 주문 취소 UseCase
 *
 * <p>Domain에서 던진 예외를 그대로 전파합니다 (try-catch 불필요).</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;

    public CancelOrderService(LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
    }

    /**
     * 주문 취소
     *
     * @param command 주문 취소 명령
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우 (404)
     * @throws OrderCancellationException 취소 불가능한 상태인 경우 (400)
     */
    @Override
    @Transactional
    public void execute(CancelOrderCommand command) {
        // ✅ Repository 조회 - 못 찾으면 OrderNotFoundException (404)
        Order order = loadOrderPort.findById(OrderId.of(command.orderId()))
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // ✅ Domain 비즈니스 메서드 - 취소 불가 시 OrderCancellationException (400)
        // try-catch 없이 자동 전파!
        order.cancel();

        saveOrderPort.save(order);
    }
}
```

---

## 7) GlobalExceptionHandler (Adapter Layer)

### 7-1) DomainException 처리

**핵심**: `DomainException.httpStatus()`로 HTTP 상태 코드 직접 획득

```java
package com.ryuqq.adapter.in.rest.common;

import com.ryuqq.domain.common.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler - 모든 예외를 HTTP 응답으로 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DomainException 처리
     *
     * <p>ErrorCode에서 HTTP 상태 코드를 직접 추출합니다.</p>
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        // ✅ DomainException에서 직접 HTTP 상태 코드 획득
        int httpStatus = e.httpStatus();

        return ResponseEntity
            .status(httpStatus)
            .body(new ErrorResponse(
                e.code(),
                e.getMessage(),
                e.args()
            ));
    }

    /**
     * IllegalArgumentException 처리 - 500 Internal Server Error
     *
     * <p>개발자 오류 (절대 발생하면 안 됨). 로그에 상세 정보 기록.</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        // ⚠️ 개발자 오류 - 로그 기록 필수
        log.error("Internal validation error (bug): {}", e.getMessage(), e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL-001",
                "Internal server error",  // 클라이언트에게는 상세 정보 숨김
                Map.of()
            ));
    }
}
```

### 7-2) ErrorResponse DTO

```java
package com.ryuqq.adapter.in.rest.common;

import java.util.Map;

/**
 * ErrorResponse - API 에러 응답 DTO
 */
public record ErrorResponse(
    String code,
    String message,
    Map<String, Object> details
) {
    public ErrorResponse(String code, String message) {
        this(code, message, Map.of());
    }
}
```

---

## 8) 예외 처리 흐름 요약

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Domain Layer                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ IllegalArgumentException (내부 불변조건 위반)               │
│ ├─ 용도: 개발자 오류, 절대 발생하면 안 됨                  │
│ ├─ 예: private 메서드의 null 체크                          │
│ └─ → GlobalExceptionHandler → 500 (로그 기록)              │
│                                                              │
│ DomainException 상속 (클라이언트 입력/비즈니스 룰)          │
│ ├─ MoneyValidationException (입력 검증)                    │
│ │  └─ → GlobalExceptionHandler → 400 Bad Request           │
│ │                                                           │
│ ├─ OrderNotFoundException                                   │
│ │  └─ → GlobalExceptionHandler → 404 Not Found             │
│ │                                                           │
│ ├─ OrderCancellationException                               │
│ │  └─ → GlobalExceptionHandler → 400 Bad Request           │
│ │                                                           │
│ └─ TenantNameDuplicatedException                            │
│    └─ → GlobalExceptionHandler → 409 Conflict              │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ 2. Application Layer                                         │
├─────────────────────────────────────────────────────────────┤
│ UseCase는 Domain 메서드 호출만                              │
│ → 예외 발생 시 자동 전파 (try-catch 불필요)                │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ 3. Adapter Layer                                             │
├─────────────────────────────────────────────────────────────┤
│ GlobalExceptionHandler가 자동으로 HTTP 응답 생성            │
│ → DomainException.httpStatus()로 상태 코드 획득            │
└─────────────────────────────────────────────────────────────┘
```

---

## 9) Do/Don't

### ❌ Bad: 클라이언트 입력 검증에 IllegalArgumentException 사용

```java
// ❌ Bad: 클라이언트가 음수 금액을 입력할 수 있음 → DomainException 사용해야 함
public record Money(long amount) {
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative");  // ❌ 500 에러
        }
    }
}
```

### ✅ Good: 클라이언트 입력 검증은 DomainException

```java
// ✅ Good: 클라이언트 입력 오류 → DomainException (400)
public record Money(long amount) {
    public Money {
        if (amount < 0) {
            throw new MoneyValidationException(amount);  // ✅ 400 에러
        }
    }
}
```

### ❌ Bad: Domain에서 Spring HttpStatus 사용

```java
// ❌ Bad: Domain Layer에서 Spring 의존성
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "...");  // ❌ Spring 의존
}
```

### ✅ Good: Domain에서는 int 사용

```java
// ✅ Good: Domain Layer 순수성 유지
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", 404, "Order not found");  // ✅ int 사용
}
```

### ❌ Bad: UseCase에서 불필요한 try-catch

```java
// ❌ Bad: Domain에서 이미 예외를 던지는데 UseCase에서 또 try-catch
@Override
@Transactional
public void execute(CancelOrderCommand command) {
    try {
        Order order = loadOrderPort.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel();
        saveOrderPort.save(order);
    } catch (OrderCancellationException e) {
        throw e;  // ❌ 의미 없는 catch
    }
}
```

### ✅ Good: UseCase에서 예외 그냥 전파

```java
// ✅ Good: try-catch 없이 자동 전파
@Override
@Transactional
public void execute(CancelOrderCommand command) {
    Order order = loadOrderPort.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    order.cancel();  // 예외 발생 시 자동 전파
    saveOrderPort.save(order);
}
```

### ❌ Bad: 너무 세분화된 예외 클래스

```java
// ❌ Bad: Over-Engineering
public class OrderIdNullException extends DomainException { }
public class OrderStatusNullException extends DomainException { }
public class OrderLineItemsEmptyException extends DomainException { }
```

### ✅ Good: 적절한 수준의 예외 클래스

```java
// ✅ Good: 명확한 비즈니스 시나리오별 예외
public class OrderNotFoundException extends DomainException { }        // 조회 실패
public class OrderCancellationException extends DomainException { }    // 취소 불가
public class OrderInvalidStateException extends DomainException { }    // 상태 전환 불가
```

---

## 10) 체크리스트

### ErrorCode Enum 작성 체크리스트

- [ ] **ErrorCode 인터페이스 구현** (`implements ErrorCode`)
- [ ] **int httpStatus 사용** (Spring HttpStatus 금지)
- [ ] **형식 준수** (`{BC}-{3자리 숫자}`)
- [ ] **명확한 에러 메시지** (사용자가 이해 가능)
- [ ] **번호 체계 준수** (0XX: 404, 01X: 400 검증, 02X: 409, 03X: 400 비즈니스, 05X: 500)
- [ ] **Javadoc 작성**
- [ ] **위치** (`domain/{bc}/exception/{BC}ErrorCode.java`)

### 구체적인 예외 클래스 작성 체크리스트

- [ ] **DomainException 상속** (`extends DomainException`)
- [ ] **명확한 클래스명** (`{Specific}Exception`)
- [ ] **ErrorCode 기반 생성자** (super(ErrorCode, message, args))
- [ ] **컨텍스트 정보** (Map<String, Object> args에 ID, 상태 등 포함)
- [ ] **Javadoc 작성** (HTTP 응답, 사용 예시 포함)
- [ ] **위치** (`domain/{bc}/exception/{Specific}Exception.java`)
- [ ] **필요성 검토** (명확한 비즈니스 시나리오인가?)

### IllegalArgumentException vs DomainException 선택 체크리스트

- [ ] **누구의 잘못인가?**
  - 개발자 오류 (버그) → IllegalArgumentException
  - 클라이언트 잘못 → DomainException
- [ ] **프로덕션에서 발생 가능한가?**
  - 발생하면 안 됨 → IllegalArgumentException
  - 발생 가능 → DomainException
- [ ] **클라이언트에게 명확한 메시지가 필요한가?**
  - 필요 없음 (버그) → IllegalArgumentException
  - 필요함 → DomainException

### UseCase 예외 처리 체크리스트

- [ ] **try-catch 불필요** (Domain에서 던진 예외를 그냥 전파)
- [ ] **Repository 조회 실패**: orElseThrow()로 XxxNotFoundException 던지기
- [ ] **메서드 Javadoc @throws** (어떤 예외 던지는지 명시)

---

**✅ 이 가이드는 Domain Layer 순수성을 유지하면서 명확한 예외 처리 전략을 제시합니다.**
