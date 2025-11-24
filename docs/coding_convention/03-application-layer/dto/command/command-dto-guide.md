# Command DTO Guide — **상태 변경 요청 데이터**

> Command DTO는 **CUD(Create, Update, Delete)** 작업의 입력 데이터를 담는 **순수한 불변 객체**입니다.
>
> **Java Record** 패턴을 사용하며, **데이터 전달만** 담당합니다.

---

## 1) 핵심 역할

* **데이터 전달**: CUD 요청 데이터를 계층 간 전달
* **불변성**: Record로 변경 불가능한 데이터
* **프레임워크 독립**: 외부 의존성 없는 순수 Java

---

## 2) 핵심 원칙

### 원칙 1: 순수 Java Record
- ✅ `public record` 키워드로 정의
- ✅ 필드만 존재
- ❌ `jakarta.validation` 의존성 금지
- ❌ Lombok 사용 금지

### 원칙 2: 데이터 전달 전용
- ❌ 비즈니스 로직 금지
- ❌ 비즈니스 검증 금지
- ✅ 불변 컬렉션 변환만 허용 (`List.copyOf()`)

### 원칙 3: 네이밍 규칙
- ✅ `{Action}{Bc}Command`
- 예: `PlaceOrderCommand`, `CancelOrderCommand`

---

## 3) 패키지 구조

```
application/{bc}/dto/command/
├── Create{Bc}Command.java
├── Update{Bc}Command.java
└── {Action}{Bc}Command.java
```

---

## 4) 템플릿 코드

### 기본 패턴
```java
package com.ryuqq.application.{bc}.dto.command;

import java.util.List;

/**
 * {Action} {Bc} Command
 *
 * @author development-team
 * @since 1.0.0
 */
public record {Action}{Bc}Command(
    Long field1,
    String field2,
    List<NestedItem> items
) {
    /**
     * Compact Constructor: 불변화만
     */
    public {Action}{Bc}Command {
        items = (items != null) ? List.copyOf(items) : List.of();
    }

    public record NestedItem(
        Long id,
        String value
    ) {}
}
```

---

## 5) 실전 예시

### Command DTO
```java
package com.ryuqq.application.order.dto.command;

import java.util.List;

/**
 * Place Order Command
 *
 * @author development-team
 * @since 1.0.0
 */
public record PlaceOrderCommand(
    Long customerId,
    List<OrderItem> items,
    String deliveryAddress
) {
    public PlaceOrderCommand {
        items = (items != null) ? List.copyOf(items) : List.of();
    }

    public record OrderItem(
        Long productId,
        Integer quantity,
        Long unitPrice
    ) {}
}
```
---

## 6) Do / Don't

### ❌ Bad

```java
// ❌ jakarta.validation
import jakarta.validation.constraints.*;
public record Command(@NotNull Long id) {}

// ❌ 비즈니스 검증
public record Command(List<Item> items) {
    public Command {
        if (items.size() > 50) {  // 비즈니스 로직!
            throw new IllegalArgumentException();
        }
    }
}

// ❌ Lombok
@Data @Builder
public class Command {}
```

### ✅ Good

```java
// ✅ 순수 Record + 불변화만
public record Command(
    Long id,
    List<Item> items
) {
    public Command {
        items = (items != null) ? List.copyOf(items) : List.of();
    }
}
```

---

## 7) 체크리스트

- [ ] `public record {Action}{Bc}Command`
- [ ] 패키지: `application.{bc}.dto.command`
- [ ] 순수 Java 타입만
- [ ] `jakarta.validation` 사용 금지
- [ ] Compact Constructor: 불변화만
- [ ] 비즈니스 로직/검증 금지
- [ ] Lombok 미사용

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 3.0.0 (단순화)
