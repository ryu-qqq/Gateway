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
│ 1. 간단한 검증 실패                                          │
│    → IllegalArgumentException (JDK 기본)                    │
│                                                              │
│ 2. 비즈니스 룰 위반                                          │
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

### 언제 어떤 예외를 사용하는가?

| 상황 | 예외 종류 | 용도 | HTTP 매핑 |
|------|-----------|------|-----------|
| **간단한 파라미터 검증** | IllegalArgumentException | null 체크, 범위 검증 | 500 (내부 검증) |
| **비즈니스 룰 위반** | DomainException 상속 | 상태 전환 불가, 중복 등 | 400, 409 |
| **엔티티 못 찾음** | XxxNotFoundException | Repository 조회 실패 | 404 |
| **상태 전환 불가** | XxxInvalidStateException | 비즈니스 메서드 조건 미충족 | 400 |

---

## 2) Domain Layer 예외 처리

### 2-1) IllegalArgumentException 사용 시점

**용도**: 생성자, 정적 팩토리 메서드, Compact Constructor에서 **간단한 파라미터 검증**

**원칙**:
- ✅ **null 체크**: `if (value == null) throw new IllegalArgumentException("must not be null")`
- ✅ **범위 검증**: `if (amount < 0) throw new IllegalArgumentException("must be positive")`
- ✅ **형식 검증**: `if (!email.contains("@")) throw new IllegalArgumentException("invalid format")`
- ✅ **HTTP 매핑 불필요**: GlobalExceptionHandler에서 500으로 처리

**템플릿: VO Compact Constructor**

```java
package com.ryuqq.domain.order.vo;

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
     * <p>음수 금액은 허용되지 않습니다.</p>
     *
     * @throws IllegalArgumentException 금액이 음수인 경우
     * @author development-team
     * @since 1.0.0
     */
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException(
                "Money amount must not be negative: " + amount
            );
        }
    }

    /**
     * 정적 팩토리 메서드 - Money 생성
     *
     * @param amount 금액 (0 이상)
     * @return Money 인스턴스
     * @throws IllegalArgumentException 금액이 음수인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static Money of(long amount) {
        return new Money(amount);
    }

    /**
     * 두 금액의 합을 반환
     *
     * @param other 더할 금액
     * @return 합계 금액
     * @throws IllegalArgumentException other가 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public Money add(Money other) {
        if (other == null) {
            throw new IllegalArgumentException("Other money must not be null");
        }
        return new Money(this.amount + other.amount);
    }

    public static final Money ZERO = new Money(0);
}
```

**템플릿: Aggregate 생성자**

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.order.vo.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Aggregate Root
 *
 * <p>주문의 생명주기와 상태를 관리하는 집합 루트입니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLineItem> lineItems;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Clock clock;

    /**
     * Private 생성자 - 직접 생성 방지
     *
     * @param id 주문 ID (null 가능 - forNew()용)
     * @param customerId 고객 ID (null 불가)
     * @param status 주문 상태 (null 불가)
     * @param lineItems 주문 항목 리스트 (null 불가)
     * @param createdAt 생성 시각 (null 불가)
     * @param updatedAt 수정 시각 (null 불가)
     * @param clock 시간 제어 (null 불가)
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    private Order(OrderId id, CustomerId customerId, OrderStatus status,
                  List<OrderLineItem> lineItems,
                  LocalDateTime createdAt, LocalDateTime updatedAt, Clock clock) {
        // null 체크 (forNew()는 id가 null이므로 예외)
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("OrderStatus must not be null");
        }
        if (lineItems == null) {
            throw new IllegalArgumentException("LineItems must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("UpdatedAt must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null");
        }

        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.lineItems = new ArrayList<>(lineItems); // 방어적 복사
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.clock = clock;
    }

    /**
     * 신규 주문 생성 (Auto Increment ID)
     *
     * @param customerId 고객 ID
     * @param clock 시간 제어
     * @return 신규 Order
     * @throws IllegalArgumentException customerId 또는 clock이 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static Order forNew(CustomerId customerId, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new Order(
            null,  // Auto Increment: ID null
            customerId,
            OrderStatus.PENDING,
            new ArrayList<>(),
            now,
            now,
            clock
        );
    }

    /**
     * ID 기반 주문 생성
     *
     * @param id 주문 ID (null 불가)
     * @param customerId 고객 ID
     * @param status 주문 상태
     * @param lineItems 주문 항목
     * @param createdAt 생성 시각
     * @param updatedAt 수정 시각
     * @param clock 시간 제어
     * @return Order
     * @throws IllegalArgumentException id가 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static Order of(OrderId id, CustomerId customerId, OrderStatus status,
                           List<OrderLineItem> lineItems,
                           LocalDateTime createdAt, LocalDateTime updatedAt, Clock clock) {
        if (id == null) {
            throw new IllegalArgumentException("OrderId must not be null for of() method");
        }
        return new Order(id, customerId, status, lineItems, createdAt, updatedAt, clock);
    }

    // Getters...
    public OrderId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
}
```

### 2-2) DomainException 상속 클래스 사용 시점

**용도**: 비즈니스 메서드에서 **비즈니스 룰 위반** (HTTP 매핑 필요)

**원칙**:
- ✅ **상태 전환 불가**: 구체적인 예외 클래스 던지기 (`OrderInvalidStateException`)
- ✅ **조건 미충족**: 구체적인 예외 클래스 던지기 (`OrderCancellationException`)
- ✅ **HTTP 매핑**: 400 Bad Request, 404 Not Found 등
- ✅ **GlobalExceptionHandler 연동**: ErrorCode에서 자동으로 HTTP 상태 코드 추출

**템플릿: Aggregate 비즈니스 메서드 (DomainException 직접 던지기)**

```java
package com.ryuqq.domain.order.aggregate.order;

import com.ryuqq.domain.order.vo.*;
import com.ryuqq.domain.order.exception.OrderInvalidStateException;
import com.ryuqq.domain.order.exception.OrderCancellationException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Aggregate Root - 비즈니스 메서드에서 DomainException 직접 던지기
 *
 * @author development-team
 * @since 1.0.0
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLineItem> lineItems;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Clock clock;

    // 생성자 생략...

    /**
     * 주문 확정
     *
     * <p>PENDING 상태에서만 확정 가능하며, 주문 항목이 1개 이상 있어야 합니다.</p>
     *
     * @throws OrderInvalidStateException 확정 불가능한 상태인 경우
     * @author development-team
     * @since 1.0.0
     */
    public void confirm() {
        if (!canConfirm()) {
            // ✅ Domain에서 직접 DomainException 던지기
            throw new OrderInvalidStateException(
                this.id.value(),
                this.status.name(),
                "confirm",
                "Order must be in PENDING status and have at least one item"
            );
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 주문 취소
     *
     * <p>PENDING 또는 CONFIRMED 상태에서만 취소 가능합니다.</p>
     *
     * @throws OrderCancellationException 취소 불가능한 상태인 경우
     * @author development-team
     * @since 1.0.0
     */
    public void cancel() {
        if (!canCancel()) {
            // ✅ Domain에서 직접 DomainException 던지기
            throw new OrderCancellationException(
                this.id.value(),
                this.status.name()
            );
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 배송 시작
     *
     * <p>CONFIRMED 상태에서만 배송 시작 가능합니다.</p>
     *
     * @throws OrderInvalidStateException 배송 시작 불가능한 상태인 경우
     * @author development-team
     * @since 1.0.0
     */
    public void ship() {
        if (!canShip()) {
            // ✅ Domain에서 직접 DomainException 던지기
            throw new OrderInvalidStateException(
                this.id.value(),
                this.status.name(),
                "ship",
                "Order must be in CONFIRMED status to start shipping"
            );
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 주문 확정 가능 여부 확인 (private)
     */
    private boolean canConfirm() {
        return this.status == OrderStatus.PENDING && !this.lineItems.isEmpty();
    }

    /**
     * 주문 취소 가능 여부 확인 (private)
     */
    private boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }

    /**
     * 배송 시작 가능 여부 확인 (private)
     */
    private boolean canShip() {
        return this.status == OrderStatus.CONFIRMED;
    }
}
```

**예외 클래스 정의 예시**:

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * OrderInvalidStateException - 주문 상태 전환 불가 시 발생
 *
 * <p>비즈니스 메서드 실행 조건이 맞지 않을 때 발생합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderInvalidStateException extends DomainException {

    public OrderInvalidStateException(Long orderId, String currentStatus,
                                       String action, String reason) {
        super(
            OrderErrorCode.INVALID_ORDER_STATE.getCode(),
            String.format("Cannot %s order %d. Current status: %s. Reason: %s",
                action, orderId, currentStatus, reason),
            Map.of(
                "orderId", orderId,
                "currentStatus", currentStatus,
                "action", action
            )
        );
    }
}

/**
 * OrderCancellationException - 주문 취소 불가 시 발생
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderCancellationException extends DomainException {

    public OrderCancellationException(Long orderId, String currentStatus) {
        super(
            OrderErrorCode.CANNOT_CANCEL_ORDER.getCode(),
            String.format("Cannot cancel order %d. Current status: %s", orderId, currentStatus),
            Map.of("orderId", orderId, "currentStatus", currentStatus)
        );
    }
}
```

---

## 3) ErrorCode + 구체적인 예외 클래스

### 3-1) ErrorCode Enum 작성법

**용도**: Bounded Context별 에러 코드 정의 (HTTP 상태 코드 매핑)

**위치**: `domain/{boundedContext}/exception/{BoundedContext}ErrorCode.java`

**원칙**:
- ✅ **ErrorCode 인터페이스 구현**: `implements ErrorCode`
- ✅ **형식**: `{BC}-{3자리 숫자}` (예: TENANT-001, ORDER-001)
- ✅ **HTTP 상태 코드 매핑**: 404, 409, 400, 500 등
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
 *   <li>✅ 형식: TENANT-{3자리 숫자}</li>
 *   <li>✅ HTTP 상태 코드 매핑</li>
 *   <li>✅ 명확한 에러 메시지</li>
 * </ul>
 *
 * <p><strong>사용 예시:</strong></p>
 * <pre>{@code
 * throw new TenantNotFoundException("tenant-123");
 * // → ErrorCode: TENANT-001, HTTP Status: 404
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum TenantErrorCode implements ErrorCode {

    /**
     * Tenant를 찾을 수 없음
     */
    TENANT_NOT_FOUND("TENANT-001", 404, "Tenant not found"),

    /**
     * Tenant 이름 중복
     */
    TENANT_NAME_DUPLICATED("TENANT-002", 409, "Tenant name already exists"),

    /**
     * Tenant 생성 실패
     */
    TENANT_CREATION_FAILED("TENANT-003", 500, "Failed to create tenant"),

    /**
     * Tenant 업데이트 실패
     */
    TENANT_UPDATE_FAILED("TENANT-004", 500, "Failed to update tenant"),

    /**
     * Tenant 삭제 실패
     */
    TENANT_DELETION_FAILED("TENANT-005", 500, "Failed to delete tenant"),

    /**
     * 유효하지 않은 Tenant 상태
     */
    INVALID_TENANT_STATUS("TENANT-006", 400, "Invalid tenant status");

    private final String code;
    private final int httpStatus;
    private final String message;

    /**
     * Constructor - ErrorCode 생성
     *
     * @param code 에러 코드 (TENANT-XXX)
     * @param httpStatus HTTP 상태 코드
     * @param message 에러 메시지
     * @author development-team
     * @since 1.0.0
     */
    TenantErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 문자열 (예: TENANT-001)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * HTTP 상태 코드 반환
     *
     * @return HTTP 상태 코드 (예: 404, 409, 500)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * 에러 메시지 반환
     *
     * @return 에러 메시지 문자열
     * @author development-team
     * @since 1.0.0
     */
    @Override
    public String getMessage() {
        return message;
    }
}
```

### 3-2) 구체적인 예외 클래스 작성법

**용도**: ErrorCode를 사용하는 구체적인 비즈니스 예외

**위치**: `domain/{boundedContext}/exception/{Specific}Exception.java`

**원칙**:
- ✅ **DomainException 상속**: `extends DomainException`
- ✅ **명확한 클래스명**: `TenantNotFoundException`, `OrderCancellationException`
- ✅ **2개 생성자**: 파라미터 있는 생성자 + 기본 생성자
- ✅ **컨텍스트 정보**: 에러 메시지에 ID, 상태 등 포함

**템플릿: 구체적인 예외 클래스**

```java
package com.ryuqq.domain.tenant.exception;

import com.ryuqq.domain.common.exception.DomainException;

/**
 * TenantNotFoundException - Tenant를 찾을 수 없을 때 발생하는 예외
 *
 * <p>Tenant 조회 시 해당 ID의 Tenant가 존재하지 않을 때 발생합니다.</p>
 *
 * <p><strong>HTTP 응답:</strong></p>
 * <ul>
 *   <li>Status Code: 404 NOT FOUND</li>
 *   <li>Error Code: TENANT-001</li>
 *   <li>Message: "Tenant not found: {tenantId}"</li>
 * </ul>
 *
 * <p><strong>사용 예시:</strong></p>
 * <pre>{@code
 * // UseCase에서 사용
 * Tenant tenant = tenantRepository.findById(tenantId)
 *     .orElseThrow(() -> new TenantNotFoundException(tenantId.value()));
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public class TenantNotFoundException extends DomainException {

    /**
     * Constructor - Tenant ID를 포함한 예외 생성
     *
     * <p>에러 메시지에 찾지 못한 Tenant ID를 포함시킵니다.</p>
     *
     * @param tenantId 찾지 못한 Tenant ID (Long)
     * @author development-team
     * @since 1.0.0
     */
    public TenantNotFoundException(Long tenantId) {
        super(
            TenantErrorCode.TENANT_NOT_FOUND.getCode(),
            "Tenant not found: " + tenantId
        );
    }

    /**
     * Constructor - 기본 에러 메시지 사용
     *
     * <p>TenantErrorCode의 기본 메시지를 사용합니다.</p>
     *
     * @author development-team
     * @since 1.0.0
     */
    public TenantNotFoundException() {
        super(
            TenantErrorCode.TENANT_NOT_FOUND.getCode(),
            TenantErrorCode.TENANT_NOT_FOUND.getMessage()
        );
    }
}
```

**템플릿: 복잡한 예외 (args 사용)**

```java
package com.ryuqq.domain.order.exception;

import com.ryuqq.domain.common.exception.DomainException;
import java.util.Map;

/**
 * OrderCancellationException - 주문 취소 불가 상태에서 취소 시도 시 발생
 *
 * <p>PENDING 또는 CONFIRMED 상태가 아닌 주문은 취소할 수 없습니다.</p>
 *
 * <p><strong>HTTP 응답:</strong></p>
 * <ul>
 *   <li>Status Code: 400 BAD REQUEST</li>
 *   <li>Error Code: ORDER-002</li>
 *   <li>Message: "Cannot cancel order. Current status: {status}"</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderCancellationException extends DomainException {

    /**
     * Constructor - 현재 주문 상태를 포함한 예외 생성
     *
     * @param orderId 주문 ID
     * @param currentStatus 현재 주문 상태
     * @author development-team
     * @since 1.0.0
     */
    public OrderCancellationException(Long orderId, String currentStatus) {
        super(
            OrderErrorCode.CANNOT_CANCEL_ORDER.getCode(),
            "Cannot cancel order " + orderId + ". Current status: " + currentStatus,
            Map.of(
                "orderId", orderId,
                "currentStatus", currentStatus
            )
        );
    }
}
```

### 3-3) 언제 만들어야 하는가?

**✅ 구체적인 예외 클래스를 만들어야 하는 경우**:

1. **여러 UseCase에서 재사용**
   - ✅ `TenantNotFoundException`: 3개 이상의 UseCase에서 사용
   - ❌ 한 번만 사용: DomainException 직접 throw

2. **코드만 봐도 예외 원인 이해 가능**
   - ✅ `OrderCancellationException`: "아, 취소 불가 상태구나"
   - ❌ `DomainException("ORDER-002", ...)`: 코드 찾아봐야 함

3. **복잡한 컨텍스트 정보 필요**
   - ✅ `PaymentFailedException(orderId, paymentId, reason)`
   - ❌ 단순 ID만: DomainException 직접 사용

**❌ 불필요한 예외 클래스 (Over-Engineering)**:

```java
// ❌ Bad: 한 번만 사용하는 예외
public class TenantIdNullException extends DomainException { }

// ✅ Good: DomainException 직접 사용
throw new DomainException(
    TenantErrorCode.INVALID_TENANT_ID.getCode(),
    "Tenant ID must not be null"
);
```

---

## 4) UseCase에서 예외 처리 (그냥 전파)

### 핵심 원칙: try-catch 불필요

**전략**: Domain에서 이미 구체적인 DomainException을 던지므로, UseCase는 그냥 전파만 하면 됩니다.

**이유**:
- ✅ **Domain이 책임**: 비즈니스 룰 위반 시 Domain에서 직접 구체적인 예외 던짐
- ✅ **UseCase는 단순**: Domain 메서드 호출만, 예외는 자동 전파
- ✅ **GlobalExceptionHandler가 처리**: ErrorCode에서 HTTP 상태 코드 자동 추출
- ✅ **보일러플레이트 제거**: 모든 UseCase에 try-catch 쓸 필요 없음

### 템플릿 1: 주문 생성 UseCase (예외 그냥 전파)

```java
package com.ryuqq.application.order.usecase;

import com.ryuqq.application.order.port.in.CreateOrderCommand;
import com.ryuqq.application.order.port.in.CreateOrderUseCase;
import com.ryuqq.application.order.port.out.LoadCustomerPort;
import com.ryuqq.application.order.port.out.SaveOrderPort;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.CustomerId;
import com.ryuqq.domain.customer.exception.CustomerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

/**
 * CreateOrderService - 주문 생성 UseCase
 *
 * <p>Domain에서 던진 예외를 그대로 전파합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final LoadCustomerPort loadCustomerPort;
    private final SaveOrderPort saveOrderPort;
    private final Clock clock;

    public CreateOrderService(LoadCustomerPort loadCustomerPort,
                               SaveOrderPort saveOrderPort,
                               Clock clock) {
        this.loadCustomerPort = loadCustomerPort;
        this.saveOrderPort = saveOrderPort;
        this.clock = clock;
    }

    /**
     * 주문 생성
     *
     * <p>Domain에서 발생한 예외는 자동으로 전파됩니다.</p>
     *
     * @param command 주문 생성 명령
     * @return 생성된 주문 ID
     * @throws CustomerNotFoundException 고객을 찾을 수 없는 경우 (404)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    @Transactional
    public Long execute(CreateOrderCommand command) {
        // ✅ Repository 조회 실패 시 예외 던지기
        CustomerId customerId = CustomerId.of(command.customerId());
        loadCustomerPort.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        // ✅ Domain 객체 생성 (IllegalArgumentException 발생 가능 → 자동 전파)
        Order order = Order.forNew(customerId, clock);

        // ✅ 저장
        return saveOrderPort.save(order).value();
    }
}
```

### 템플릿 2: 주문 취소 UseCase (비즈니스 예외 자동 전파)

```java
package com.ryuqq.application.order.usecase;

import com.ryuqq.application.order.port.in.CancelOrderCommand;
import com.ryuqq.application.order.port.in.CancelOrderUseCase;
import com.ryuqq.application.order.port.out.LoadOrderPort;
import com.ryuqq.application.order.port.out.SaveOrderPort;
import com.ryuqq.domain.order.aggregate.order.Order;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.exception.OrderNotFoundException;
import com.ryuqq.domain.order.exception.OrderCancellationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CancelOrderService - 주문 취소 UseCase
 *
 * <p>Domain에서 던진 OrderCancellationException이 자동 전파됩니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;

    public CancelOrderService(LoadOrderPort loadOrderPort,
                               SaveOrderPort saveOrderPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
    }

    /**
     * 주문 취소
     *
     * <p>try-catch 없이 Domain 예외를 자동 전파합니다.</p>
     *
     * @param command 주문 취소 명령
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우 (404)
     * @throws OrderCancellationException 취소 불가능한 상태인 경우 (400)
     * @author development-team
     * @since 1.0.0
     */
    @Override
    @Transactional
    public void execute(CancelOrderCommand command) {
        // ✅ Repository 조회
        OrderId orderId = OrderId.of(command.orderId());
        Order order = loadOrderPort.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // ✅ Domain 비즈니스 메서드 호출
        // OrderCancellationException 발생 가능 → 자동 전파 (try-catch 불필요!)
        order.cancel();

        // ✅ 저장
        saveOrderPort.save(order);
    }
}
```

### GlobalExceptionHandler 자동 처리

**원리**: DomainException의 ErrorCode에서 HTTP 상태 코드 자동 추출

```java
package com.ryuqq.adapter.in.rest.common;

import com.ryuqq.domain.common.exception.DomainException;
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
     * DomainException 처리 - ErrorCode에서 HTTP 상태 코드 자동 추출
     *
     * <p>OrderCancellationException, TenantNotFoundException 등 모든 DomainException 처리</p>
     *
     * @param e DomainException
     * @return ErrorResponse (HTTP 상태 코드 자동 매핑)
     * @author development-team
     * @since 1.0.0
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        // ✅ ErrorCode에서 HTTP 상태 코드 자동 추출
        ErrorCode errorCode = findErrorCodeByCode(e.code());

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(new ErrorResponse(
                e.code(),
                e.getMessage(),
                e.args()
            ));
    }

    /**
     * IllegalArgumentException 처리 - 500 Internal Server Error
     *
     * <p>Domain 내부 검증 실패 (개발자 오류)</p>
     *
     * @param e IllegalArgumentException
     * @return ErrorResponse (500)
     * @author development-team
     * @since 1.0.0
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
            .status(500)
            .body(new ErrorResponse(
                "INTERNAL-ERROR",
                "Internal validation error: " + e.getMessage(),
                Map.of()
            ));
    }
}
```

### 정리: 예외 처리 흐름

```
1. Domain Layer
   ├─ IllegalArgumentException (간단한 검증)
   │  → GlobalExceptionHandler → 500 Internal Server Error
   │
   └─ DomainException 상속 (비즈니스 룰 위반)
      ├─ OrderCancellationException
      │  → GlobalExceptionHandler → 400 Bad Request
      │
      ├─ TenantNotFoundException
      │  → GlobalExceptionHandler → 404 Not Found
      │
      └─ OrderInvalidStateException
         → GlobalExceptionHandler → 400 Bad Request

2. Application Layer
   ├─ UseCase는 Domain 메서드 호출만
   └─ 예외 발생 시 자동 전파 (try-catch 불필요)

3. Adapter Layer
   └─ GlobalExceptionHandler가 자동으로 HTTP 응답 생성
```

---

## 5) Do/Don't

### ❌ Bad: 간단한 검증에 DomainException 사용

```java
// ❌ Bad: VO 파라미터 검증에 DomainException (과도한 예외 클래스)
public record Money(long amount) {
    public Money {
        if (amount < 0) {
            throw new MoneyInvalidAmountException(amount);  // ❌ 과도한 예외 클래스
        }
    }
}
```

### ✅ Good: 간단한 검증은 IllegalArgumentException

```java
// ✅ Good: 간단한 파라미터 검증은 IllegalArgumentException
public record Money(long amount) {
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount must not be negative: " + amount);
        }
    }
}
```

### ❌ Bad: 비즈니스 룰 위반에 IllegalStateException 사용

```java
// ❌ Bad: 상태 전환 불가 시 IllegalStateException (HTTP 매핑 불가)
public void cancel() {
    if (!canCancel()) {
        throw new IllegalStateException("Cannot cancel order");  // ❌ 500 에러로 처리됨
    }
    this.status = OrderStatus.CANCELLED;
}
```

### ✅ Good: 비즈니스 룰 위반은 DomainException 상속

```java
// ✅ Good: 상태 전환 불가 시 구체적인 DomainException (HTTP 400)
public void cancel() {
    if (!canCancel()) {
        throw new OrderCancellationException(this.id.value(), this.status.name());  // ✅ 400 에러로 처리
    }
    this.status = OrderStatus.CANCELLED;
}
```

### ❌ Bad: 과도한 예외 클래스 생성

```java
// ❌ Bad: 한 번만 사용하는 예외
public class TenantIdNullException extends DomainException { }
public class TenantNameNullException extends DomainException { }
public class TenantStatusNullException extends DomainException { }
```

### ✅ Good: 재사용 가능한 예외만 생성

```java
// ✅ Good: 여러 UseCase에서 재사용
public class TenantNotFoundException extends DomainException {
    // 3개 이상의 UseCase에서 사용
}

// ✅ Good: 한 번만 사용 → DomainException 직접 사용
throw new DomainException(
    TenantErrorCode.INVALID_TENANT_ID.getCode(),
    "Tenant ID must not be null"
);
```

### ❌ Bad: ErrorCode 형식 불일치

```java
// ❌ Bad: 형식 불일치
TENANT_NOT_FOUND("TENANT_NOT_FOUND", 404, "..."),  // ❌ 언더스코어
ORDER_ERROR("ORDER-1", 400, "..."),                 // ❌ 1자리 숫자
INVALID("ERR-001", 500, "..."),                     // ❌ BC 이름 없음
```

### ✅ Good: ErrorCode 형식 일관성

```java
// ✅ Good: {BC}-{3자리 숫자}
TENANT_NOT_FOUND("TENANT-001", 404, "Tenant not found"),
ORDER_CANCELLATION_FAILED("ORDER-002", 400, "Cannot cancel order"),
PAYMENT_DECLINED("PAYMENT-003", 400, "Payment declined"),
```

### ❌ Bad: 메시지에 기술 용어 노출

```java
// ❌ Bad: 기술 용어, Stack Trace 노출
throw new TenantNotFoundException("NullPointerException in line 42");
throw new OrderException("SQL Error: duplicate key violation");
```

### ✅ Good: 사용자 친화적 메시지

```java
// ✅ Good: 비즈니스 용어 사용
throw new TenantNotFoundException(tenantId);  // "Tenant not found: 123"
throw new OrderCancellationException(orderId, status);  // "Cannot cancel order 456. Current status: SHIPPED"
```

### ❌ Bad: UseCase에서 불필요한 try-catch (보일러플레이트)

```java
// ❌ Bad: Domain에서 이미 구체적인 예외를 던지는데 UseCase에서 또 try-catch
@Override
@Transactional
public void execute(CancelOrderCommand command) {
    try {
        Order order = loadOrderPort.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel();  // OrderCancellationException 발생 가능
        saveOrderPort.save(order);
    } catch (OrderCancellationException e) {
        // ❌ 불필요한 catch: 그냥 전파하면 됨
        throw e;
    }
}
```

### ✅ Good: UseCase에서 예외 그냥 전파 (try-catch 불필요)

```java
// ✅ Good: Domain 예외를 그냥 전파 (GlobalExceptionHandler가 처리)
@Override
@Transactional
public void execute(CancelOrderCommand command) {
    Order order = loadOrderPort.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    order.cancel();  // OrderCancellationException 발생 시 자동 전파
    saveOrderPort.save(order);
}
```

---

## 6) 체크리스트

### ErrorCode Enum 작성 체크리스트

- [ ] **ErrorCode 인터페이스 구현** (`implements ErrorCode`)
- [ ] **형식 준수** (`{BC}-{3자리 숫자}`)
- [ ] **HTTP 상태 코드 매핑** (404, 400, 409, 500 등)
- [ ] **명확한 에러 메시지** (사용자가 이해 가능)
- [ ] **Javadoc 작성** (에러 코드별 설명)
- [ ] **위치** (`domain/{bc}/exception/{BC}ErrorCode.java`)

### 구체적인 예외 클래스 작성 체크리스트

- [ ] **DomainException 상속** (`extends DomainException`)
- [ ] **명확한 클래스명** (`{Specific}Exception`)
- [ ] **2개 생성자** (파라미터 있는 생성자 + 기본 생성자)
- [ ] **ErrorCode 사용** (super 호출 시 ErrorCode.getCode())
- [ ] **컨텍스트 정보** (에러 메시지에 ID, 상태 등 포함)
- [ ] **Javadoc 작성** (HTTP 응답, 사용 예시 포함)
- [ ] **위치** (`domain/{bc}/exception/{Specific}Exception.java`)
- [ ] **재사용성 확인** (3개 이상 UseCase에서 사용 → 클래스 생성 / 1-2개 → DomainException 직접 사용)

### Domain Layer 예외 사용 체크리스트

- [ ] **간단한 검증**: IllegalArgumentException 사용 (생성자/팩토리 메서드 파라미터 검증)
- [ ] **비즈니스 룰 위반**: DomainException 상속 클래스 사용 (상태 전환 불가, 조건 미충족)
- [ ] **명확한 에러 메시지** (어떤 검증/비즈니스 룰이 실패했는지 명시)
- [ ] **@throws Javadoc** (어떤 상황에 어떤 예외 발생하는지 명시)
- [ ] **구체적인 예외 클래스** (OrderCancellationException, TenantNotFoundException 등)
- [ ] **ErrorCode 사용** (super 호출 시 ErrorCode.getCode(), HTTP 매핑)

### UseCase 예외 처리 체크리스트

- [ ] **try-catch 불필요** (Domain에서 던진 예외를 그냥 전파)
- [ ] **Repository 조회 실패**: orElseThrow()로 XxxNotFoundException 던지기
- [ ] **메서드 Javadoc @throws** (어떤 예외 던지는지 명시, HTTP 상태 코드 포함)
- [ ] **GlobalExceptionHandler 연동** (ErrorCode에서 HTTP 상태 코드 자동 추출)

---

**✅ 이 가이드는 Domain에서 직접 구체적인 예외를 던져 UseCase의 보일러플레이트를 제거하는 실용적인 접근을 따릅니다.**
