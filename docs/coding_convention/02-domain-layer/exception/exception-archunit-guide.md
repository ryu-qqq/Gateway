# Domain Layer Exception ArchUnit 가이드

## 📋 목차

1. [개요](#개요)
2. [ArchUnit 규칙 카테고리](#archunit-규칙-카테고리)
3. [ErrorCode Enum 규칙](#errorcode-enum-규칙)
4. [Concrete Exception 클래스 규칙](#concrete-exception-클래스-규칙)
5. [DomainException 기본 클래스 규칙](#domainexception-기본-클래스-규칙)
6. [레이어 의존성 규칙](#레이어-의존성-규칙)
7. [네이밍 규칙](#네이밍-규칙)
8. [ArchUnit 테스트 실행](#archunit-테스트-실행)
9. [규칙 위반 시 조치 방법](#규칙-위반-시-조치-방법)
10. [체크리스트](#체크리스트)

---

## 개요

이 문서는 **Domain Layer Exception**의 **ArchUnit 아키텍처 검증 규칙**을 설명합니다.

### 목적

- **자동화된 아키텍처 검증**: 빌드 시 예외 설계 규칙 자동 검증
- **Zero-Tolerance 강제**: Lombok, JPA, Spring 어노테이션 사용 방지
- **일관된 예외 구조**: ErrorCode Enum + Concrete Exception 패턴 강제
- **레이어 의존성 보호**: Domain Layer의 독립성 유지

### 대상

- **ErrorCode Enum**: `domain.[bc].exception.*ErrorCode`
- **Concrete Exception**: `domain.[bc].exception.*Exception`
- **DomainException**: `domain.common.exception.DomainException`

### 핵심 원칙

```
┌─────────────────────────────────────────────────────────────┐
│ ArchUnit이 자동으로 검증하는 예외 설계 규칙                 │
├─────────────────────────────────────────────────────────────┤
│ 1. ErrorCode Enum                                            │
│    - ErrorCode 인터페이스 구현                               │
│    - getCode(), getHttpStatus(), getMessage() 필수           │
│    - Lombok, JPA, Spring 금지                               │
│                                                              │
│ 2. Concrete Exception 클래스                                 │
│    - DomainException 상속                                    │
│    - RuntimeException 계층 (Checked Exception 금지)         │
│    - Lombok, JPA, Spring 금지                               │
│                                                              │
│ 3. 레이어 의존성                                             │
│    - Application/Adapter 레이어 의존 금지                   │
│    - Domain → Domain만 허용                                  │
│                                                              │
│ 4. 네이밍 규칙                                               │
│    - 명확한 의미 전달 (NotFound, Invalid, Cannot 등)        │
└─────────────────────────────────────────────────────────────┘
```

---

## ArchUnit 규칙 카테고리

### 1. ErrorCode Enum 규칙 (8개)

- ✅ ErrorCode 인터페이스 구현
- ✅ domain.[bc].exception 패키지 위치
- ✅ Lombok 금지
- ✅ public 접근 제어자
- ✅ getCode() 메서드 필수
- ✅ getHttpStatus() 메서드 필수
- ✅ getMessage() 메서드 필수
- ✅ HttpStatus 의존성

### 2. Concrete Exception 클래스 규칙 (7개)

- ✅ DomainException 상속
- ✅ domain.[bc].exception 패키지 위치
- ✅ Lombok 금지
- ✅ JPA 어노테이션 금지
- ✅ Spring 어노테이션 금지
- ✅ public 접근 제어자
- ✅ RuntimeException 계층

### 3. DomainException 기본 클래스 규칙 (2개)

- ✅ RuntimeException 상속
- ✅ domain.common.exception 패키지 위치

### 4. 레이어 의존성 규칙 (2개)

- ✅ Application/Adapter 레이어 의존 금지
- ✅ Domain/Adapter만 Exception 접근 허용

### 5. 네이밍 규칙 (1개)

- ✅ 명확한 의미 전달 (NotFound, Invalid, Cannot 등)

---

## ErrorCode Enum 규칙

### 규칙 1: ErrorCode 인터페이스 구현 필수

**검증 내용**:
```java
@Test
void errorCodeEnums_ShouldImplementErrorCodeInterface() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("ErrorCode")
        .and().areEnums()
        .should().implement(ErrorCode.class)
        .because("ErrorCode Enum은 ErrorCode 인터페이스를 구현해야 합니다");

    rule.check(classes);
}
```

**✅ 올바른 예시**:
```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found"),
    INVALID_ORDER_STATUS("ORDER-002", HttpStatus.BAD_REQUEST, "Invalid order status");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getMessage() { return message; }
}
```

**❌ 잘못된 예시**:
```java
// ErrorCode 인터페이스 미구현
public enum OrderErrorCode {
    ORDER_NOT_FOUND("ORDER-001", "Order not found");
    // ArchUnit 검증 실패
}
```

---

### 규칙 2: domain.[bc].exception 패키지 위치

**검증 내용**:
```java
@Test
void errorCodeEnums_ShouldBeInExceptionPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("ErrorCode")
        .and().areEnums()
        .should().resideInAPackage("..domain..exception..")
        .because("ErrorCode Enum은 domain.[bc].exception 패키지에 위치해야 합니다");

    rule.check(classes);
}
```

**✅ 올바른 패키지 구조**:
```
domain/
└── order/
    └── exception/
        ├── OrderErrorCode.java
        ├── OrderNotFoundException.java
        └── InvalidOrderStatusException.java
```

**❌ 잘못된 패키지 구조**:
```
domain/
└── order/
    ├── OrderErrorCode.java  // ❌ exception 패키지 밖
    └── exception/
        └── OrderNotFoundException.java
```

---

### 규칙 3: Lombok 어노테이션 금지

**검증 내용**:
```java
@Test
void errorCodeEnums_ShouldNotUseLombok() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("ErrorCode")
        .and().areEnums()
        .should().beAnnotatedWith("lombok.Getter")
        .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
        .because("ErrorCode Enum은 Pure Java Enum으로 구현해야 합니다");

    rule.check(classes);
}
```

**✅ 올바른 예시 (Pure Java)**:
```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getMessage() { return message; }
}
```

**❌ 잘못된 예시 (Lombok 사용)**:
```java
@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found");
    // ArchUnit 검증 실패
}
```

---

### 규칙 4~7: 필수 메서드 검증

**검증 내용**:
```java
@Test
void errorCodeEnums_ShouldHaveGetCodeMethod() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("ErrorCode")
        .should(haveMethodWithName("getCode"))
        .because("ErrorCode Enum은 getCode() 메서드를 구현해야 합니다");
}

@Test
void errorCodeEnums_ShouldHaveGetHttpStatusMethod() {
    // getHttpStatus() 메서드 필수
}

@Test
void errorCodeEnums_ShouldHaveGetMessageMethod() {
    // getMessage() 메서드 필수
}
```

**✅ 올바른 예시**:
```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found");

    @Override
    public String getCode() { return code; }  // ✅

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }  // ✅

    @Override
    public String getMessage() { return message; }  // ✅
}
```

---

### 규칙 8: HttpStatus 의존성

**검증 내용**:
```java
@Test
void errorCodeEnums_ShouldDependOnHttpStatus() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("ErrorCode")
        .should().dependOnClassesThat().areAssignableTo(HttpStatus.class)
        .because("ErrorCode Enum은 HttpStatus를 사용해야 합니다");
}
```

**✅ 올바른 예시**:
```java
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found"),
    INVALID_ORDER_STATUS("ORDER-002", HttpStatus.BAD_REQUEST, "Invalid order status");

    private final HttpStatus httpStatus;  // ✅ HttpStatus 사용
}
```

---

## Concrete Exception 클래스 규칙

### 규칙 9: DomainException 상속 필수

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldExtendDomainException() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .and().doNotHaveSimpleName("DomainException")
        .should().beAssignableTo(DomainException.class)
        .because("Concrete Exception은 DomainException을 상속해야 합니다");

    rule.check(classes);
}
```

**✅ 올바른 예시**:
```java
public class OrderNotFoundException extends DomainException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
```

**❌ 잘못된 예시**:
```java
// RuntimeException 직접 상속 금지
public class OrderNotFoundException extends RuntimeException {
    // ArchUnit 검증 실패
}

// Exception 직접 상속 금지 (Checked Exception)
public class OrderNotFoundException extends Exception {
    // ArchUnit 검증 실패
}
```

---

### 규칙 10: domain.[bc].exception 패키지 위치

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldBeInExceptionPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Exception")
        .and().resideInAPackage("..domain..")
        .should().resideInAPackage("..domain..exception..")
        .because("Concrete Exception은 domain.[bc].exception에 위치해야 합니다");
}
```

**✅ 올바른 패키지 구조**:
```
domain/
└── order/
    └── exception/
        ├── OrderErrorCode.java
        ├── OrderNotFoundException.java
        └── InvalidOrderStatusException.java
```

---

### 규칙 11~13: Lombok, JPA, Spring 어노테이션 금지

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldNotUseLombok() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .should().beAnnotatedWith("lombok.Data")
        .orShould().beAnnotatedWith("lombok.Builder")
        .because("Concrete Exception은 Pure Java로 구현해야 합니다");
}
```

**✅ 올바른 예시 (Pure Java)**:
```java
public class OrderNotFoundException extends DomainException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
```

**❌ 잘못된 예시 (Lombok 사용)**:
```java
@Getter
@Builder
public class OrderNotFoundException extends DomainException {
    private final Long orderId;
    // ArchUnit 검증 실패
}
```

---

### 규칙 14: public 접근 제어자

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldBePublic() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .should().bePublic()
        .because("Concrete Exception은 public이어야 합니다");
}
```

**✅ 올바른 예시**:
```java
public class OrderNotFoundException extends DomainException {
    // ✅ public 클래스
}
```

**❌ 잘못된 예시**:
```java
class OrderNotFoundException extends DomainException {
    // ❌ package-private (ArchUnit 검증 실패)
}
```

---

### 규칙 15: RuntimeException 계층 (Unchecked Exception)

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldExtendRuntimeException() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .should().beAssignableTo(RuntimeException.class)
        .because("Concrete Exception은 RuntimeException을 상속해야 합니다");
}
```

**✅ 올바른 예시**:
```java
// DomainException이 RuntimeException을 상속하므로
// OrderNotFoundException도 RuntimeException 계층
public class OrderNotFoundException extends DomainException {
    // ✅ Unchecked Exception
}
```

**❌ 잘못된 예시**:
```java
// Checked Exception 금지
public class OrderNotFoundException extends Exception {
    // ❌ ArchUnit 검증 실패
}
```

---

## DomainException 기본 클래스 규칙

### 규칙 16: RuntimeException 상속

**검증 내용**:
```java
@Test
void domainException_ShouldExtendRuntimeException() {
    ArchRule rule = classes()
        .that().haveSimpleName("DomainException")
        .and().resideInAPackage("..domain.common.exception")
        .should().beAssignableTo(RuntimeException.class)
        .because("DomainException은 RuntimeException을 상속해야 합니다");
}
```

**✅ 올바른 예시**:
```java
package com.ryuqq.domain.common.exception;

public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

---

### 규칙 17: domain.common.exception 패키지 위치

**검증 내용**:
```java
@Test
void domainException_ShouldBeInCommonExceptionPackage() {
    ArchRule rule = classes()
        .that().haveSimpleName("DomainException")
        .should().resideInAPackage("..domain.common.exception")
        .because("DomainException은 domain.common.exception에 위치해야 합니다");
}
```

**✅ 올바른 패키지 구조**:
```
domain/
└── common/
    └── exception/
        ├── ErrorCode.java
        └── DomainException.java
```

---

## 레이어 의존성 규칙

### 규칙 18: Application/Adapter 레이어 의존 금지

**검증 내용**:
```java
@Test
void exceptions_ShouldNotDependOnOuterLayers() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..domain..exception..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "..application..",
            "..adapter.."
        )
        .because("Domain Exception은 Application/Adapter에 의존하지 않아야 합니다");
}
```

**✅ 올바른 의존성 방향**:
```
┌─────────────────────────────────────────────────────────────┐
│ Adapter Layer                                                │
│ ├── GlobalExceptionHandler                                   │
│ │   → DomainException을 HTTP 응답으로 변환                   │
├─────────────────────────────────────────────────────────────┤
│ Application Layer                                            │
│ ├── UseCase                                                  │
│ │   → Domain Exception을 그냥 전파 (try-catch 없음)         │
├─────────────────────────────────────────────────────────────┤
│ Domain Layer                                                 │
│ ├── Aggregate/VO                                             │
│ │   → DomainException 직접 throw                             │
│ └── exception/                                               │
│     ├── OrderErrorCode                                       │
│     └── OrderNotFoundException                               │
└─────────────────────────────────────────────────────────────┘
```

**❌ 잘못된 의존성**:
```java
// Domain Exception이 Application Layer 클래스를 의존하면 안 됨
public class OrderNotFoundException extends DomainException {

    private final OrderUseCase useCase;  // ❌ Application Layer 의존
    // ArchUnit 검증 실패
}
```

---

### 규칙 19: Domain/Adapter만 Exception 접근 허용

**검증 내용**:
```java
@Test
void domainExceptions_ShouldBeThrownFromDomainOnly() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .should().onlyBeAccessed().byAnyPackage(
            "..domain..",
            "..adapter.."  // GlobalExceptionHandler
        )
        .because("Domain Exception은 Domain에서 throw, Adapter에서 처리");
}
```

**✅ 올바른 사용 패턴**:
```java
// Domain Layer: throw
public class Order {
    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new OrderCancellationException(id, status);  // ✅
        }
    }
}

// Application Layer: 그냥 전파 (try-catch 없음)
public class CancelOrderUseCase {
    public void execute(Long orderId) {
        Order order = orderRepository.findById(orderId);
        order.cancel();  // ✅ Exception 전파
    }
}

// Adapter Layer: HTTP 응답 변환
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handle(DomainException ex) {
        // ✅ HTTP 응답으로 변환
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(ErrorResponse.from(ex));
    }
}
```

---

## 네이밍 규칙

### 규칙 20: 명확한 의미 전달

**검증 내용**:
```java
@Test
void concreteExceptions_ShouldHaveMeaningfulNames() {
    ArchRule rule = classes()
        .that().resideInAPackage("..domain..exception..")
        .and().haveSimpleNameEndingWith("Exception")
        .should().haveSimpleNameMatching(".*(?:NotFound|Invalid|Already|Cannot|Failed|Exceeded|Unsupported).*Exception")
        .because("Exception 이름은 명확한 의미를 가져야 합니다");
}
```

**✅ 올바른 네이밍 예시**:
```java
// 명확한 의미 전달
OrderNotFoundException          // 404 Not Found
InvalidOrderStatusException     // 400 Bad Request
OrderAlreadyShippedException    // 409 Conflict
CannotCancelOrderException      // 400 Bad Request
OrderProcessingFailedException  // 500 Internal Error
OrderLimitExceededException     // 400 Bad Request
UnsupportedPaymentException     // 400 Bad Request
```

**❌ 잘못된 네이밍 예시**:
```java
OrderException                  // ❌ 너무 일반적 (의미 불명확)
OrderError                      // ❌ Error 접미사 금지 (Exception 사용)
OrderProblem                    // ❌ Problem 접미사 금지
```

---

## ArchUnit 테스트 실행

### Gradle 실행

```bash
# 전체 ArchUnit 테스트 실행
./gradlew :domain:test --tests "*ArchTest"

# Exception ArchUnit만 실행
./gradlew :domain:test --tests "ExceptionArchTest"

# 특정 규칙만 실행
./gradlew :domain:test --tests "ExceptionArchTest.errorCodeEnums_ShouldImplementErrorCodeInterface"
```

### IDE 실행

```java
// IntelliJ IDEA에서
// 1. ExceptionArchTest.java 파일 우클릭
// 2. "Run 'ExceptionArchTest'"

// 특정 테스트만 실행
// 1. 테스트 메서드에 커서 위치
// 2. Ctrl+Shift+F10 (Windows/Linux) 또는 Ctrl+Shift+R (Mac)
```

### 빌드 시 자동 실행

```bash
# 빌드 시 자동으로 ArchUnit 테스트 실행
./gradlew build

# ArchUnit 실패 시 빌드 실패
```

---

## 규칙 위반 시 조치 방법

### 1. ErrorCode Enum 규칙 위반

**위반 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'classes that reside in a package '..domain..exception..'
and have simple name ending with 'ErrorCode' and are enums
should implement com.ryuqq.domain.common.exception.ErrorCode' was violated (1 times):
Class <com.ryuqq.domain.order.exception.OrderErrorCode> does not implement interface com.ryuqq.domain.common.exception.ErrorCode in (OrderErrorCode.java:0)
```

**조치 방법**:
```java
// Before (위반)
public enum OrderErrorCode {
    ORDER_NOT_FOUND("ORDER-001", "Order not found");
}

// After (수정)
public enum OrderErrorCode implements ErrorCode {  // ✅ ErrorCode 인터페이스 구현
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "Order not found");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getMessage() { return message; }
}
```

---

### 2. Concrete Exception 규칙 위반

**위반 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'classes that reside in a package '..domain..exception..'
and have simple name ending with 'Exception' and do not have simple name 'DomainException'
should be assignable to com.ryuqq.domain.common.exception.DomainException' was violated (1 times):
Class <com.ryuqq.domain.order.exception.OrderNotFoundException> is not assignable to com.ryuqq.domain.common.exception.DomainException in (OrderNotFoundException.java:0)
```

**조치 방법**:
```java
// Before (위반)
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found: " + orderId);
    }
}

// After (수정)
public class OrderNotFoundException extends DomainException {  // ✅ DomainException 상속

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
```

---

### 3. Lombok 사용 위반

**위반 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'no classes that reside in a package '..domain..exception..'
should be annotated with @lombok.Getter' was violated (1 times):
Class <com.ryuqq.domain.order.exception.OrderNotFoundException> is annotated with @Getter in (OrderNotFoundException.java:5)
```

**조치 방법**:
```java
// Before (위반)
@Getter
public class OrderNotFoundException extends DomainException {
    private final Long orderId;
}

// After (수정)
public class OrderNotFoundException extends DomainException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND);
        this.orderId = orderId;
    }

    public Long getOrderId() {  // ✅ Pure Java getter
        return orderId;
    }
}
```

---

### 4. 레이어 의존성 위반

**위반 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'no classes that reside in a package '..domain..exception..'
should depend on classes that reside in any package ['..application..', '..adapter..']' was violated (1 times):
Class <com.ryuqq.domain.order.exception.OrderNotFoundException> depends on class <com.ryuqq.application.order.port.in.OrderUseCase> in (OrderNotFoundException.java:10)
```

**조치 방법**:
```java
// Before (위반)
public class OrderNotFoundException extends DomainException {
    private final OrderUseCase useCase;  // ❌ Application Layer 의존
}

// After (수정)
public class OrderNotFoundException extends DomainException {
    private final Long orderId;  // ✅ Domain 내부 타입만 사용

    public OrderNotFoundException(Long orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND);
        this.orderId = orderId;
    }
}
```

---

## 체크리스트

### ErrorCode Enum 체크리스트

```markdown
#### ErrorCode Enum 작성 시

- [ ] ErrorCode 인터페이스를 구현했는가?
- [ ] domain.[bc].exception 패키지에 위치하는가?
- [ ] Lombok 어노테이션을 사용하지 않았는가?
- [ ] public enum으로 선언했는가?
- [ ] getCode() 메서드를 구현했는가?
- [ ] getHttpStatus() 메서드를 구현했는가?
- [ ] getMessage() 메서드를 구현했는가?
- [ ] HttpStatus를 필드로 가지고 있는가?
- [ ] 에러 코드 형식이 {BC}-{3자리 숫자}인가?
```

### Concrete Exception 체크리스트

```markdown
#### Concrete Exception 클래스 작성 시

- [ ] DomainException을 상속했는가?
- [ ] domain.[bc].exception 패키지에 위치하는가?
- [ ] Lombok 어노테이션을 사용하지 않았는가?
- [ ] JPA 어노테이션을 사용하지 않았는가?
- [ ] Spring 어노테이션을 사용하지 않았는가?
- [ ] public class로 선언했는가?
- [ ] RuntimeException 계층인가? (Checked Exception이 아닌가?)
- [ ] Application/Adapter 레이어에 의존하지 않는가?
- [ ] 네이밍이 명확한 의미를 전달하는가? (NotFound, Invalid 등)
- [ ] 생성자에서 ErrorCode를 전달하는가?
```

### DomainException 체크리스트

```markdown
#### DomainException 기본 클래스 작성 시

- [ ] RuntimeException을 상속했는가?
- [ ] domain.common.exception 패키지에 위치하는가?
- [ ] ErrorCode를 필드로 가지고 있는가?
- [ ] protected 생성자로 ErrorCode를 받는가?
- [ ] getErrorCode() 메서드를 제공하는가?
```

### ArchUnit 테스트 실행 체크리스트

```markdown
#### 빌드 전 ArchUnit 테스트 실행

- [ ] `./gradlew :domain:test --tests "*ArchTest"` 실행
- [ ] ExceptionArchTest의 모든 규칙이 통과했는가?
- [ ] 위반 사항이 있다면 수정했는가?
- [ ] 빌드가 성공하는가?
```

---

## 참고 문서

- [Exception Guide](./exception-guide.md) - Domain Layer 예외 설계 가이드
- [Exception Test Guide](./exception-test-guide.md) - Domain Layer 예외 테스트 가이드
- [VO ArchUnit Guide](../vo/vo-archunit-guide.md) - Value Object ArchUnit 가이드
- [Aggregate ArchUnit Guide](../aggregate/aggregate-archunit-guide.md) - Aggregate Root ArchUnit 가이드

---

**✅ 이 가이드를 따르면 ArchUnit이 자동으로 Exception 아키텍처 규칙을 검증합니다!**
