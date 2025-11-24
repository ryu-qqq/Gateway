# Command UseCase (Port-In) — **상태 변경 추상화**

> Command UseCase는 상태 변경(CUD)을 추상화하는 **Inbound Port**입니다.
>
> `Command`, `Response`는 **별도 DTO 패키지**로 분리하여 관리합니다.

---

## 1) 핵심 역할

* **상태 변경**: Create, Update, Delete 담당
* **CQRS Command 담당**: 쓰기(CUD) 전용 Port
* **DTO 패키지 분리**: Command/Response는 dto 패키지에 정의
* **Transaction 경계**: Service 구현체에서 `@Transactional` 적용

---

## 2) 핵심 원칙

### 원칙 1: DTO 패키지 분리
- ✅ Command는 `dto/command/` 패키지에 정의
- ✅ Response는 `dto/response/` 패키지에 정의
- ❌ UseCase 인터페이스 내부에 Record 정의 금지

### 원칙 2: 단일 메서드
- ✅ 하나의 UseCase는 하나의 비즈니스 액션만 수행
- ❌ 여러 액션을 하나의 UseCase에 넣지 않음

### 원칙 3: Assembler 사용
- ✅ Command → Domain, Domain → Response 변환은 Assembler에 위임
- ❌ Service에서 직접 변환 로직 작성 금지

### 원칙 4: Transaction 경계
- ✅ Service 구현체에 `@Transactional` 적용
- ❌ UseCase 인터페이스에 `@Transactional` 적용 금지

---

## 3) 패키지 구조

```
application/order/
├── dto/
│   ├── command/
│   │   ├── PlaceOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   └── UpdateOrderCommand.java
│   └── response/
│       ├── OrderResponse.java
│       └── OrderSummaryResponse.java
└── port/
    └── in/
        └── command/
             ├── PlaceOrderUseCase.java
             ├── CancelOrderUseCase.java
             └── UpdateOrderUseCase.java
```

---

## 4) 템플릿 코드

### UseCase Interface
```java
package com.ryuqq.application.{bc}.port.in;

import com.ryuqq.application.{bc}.dto.command.{Action}{Bc}Command;
import com.ryuqq.application.{bc}.dto.response.{Bc}Response;

/**
 * {Action} {Bc} UseCase (Command)
 *
 * <p>상태 변경을 담당하는 Inbound Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface {Action}{Bc}UseCase {

    /**
     * {Action} {Bc}
     *
     * @param command {Action} 명령
     * @return {Action} 결과
     */
    {Bc}Response execute({Action}{Bc}Command command);
}
```

---

## 6) Do / Don't

### ❌ Bad Examples

```java
// ❌ UseCase 내부에 Command/Response Record 정의
public interface PlaceOrderUseCase {
    Response execute(Command command);
    
    record Command(...) {}  // 금지!
    record Response(...) {}  // 금지!
}

// ❌ 여러 액션을 하나의 UseCase에
public interface OrderUseCase {
    OrderResponse placeOrder(PlaceOrderCommand command);  // 금지!
    OrderResponse cancelOrder(CancelOrderCommand command);  // 금지!
}

// ❌ UseCase 인터페이스에 @Transactional
@Transactional  // 금지!
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}

// ❌ Domain Entity 직접 반환
public interface PlaceOrderUseCase {
    Order execute(PlaceOrderCommand command);  // 금지! Domain 노출
}
```

### ✅ Good Examples

```java
// ✅ 별도 DTO 패키지
// dto/command/PlaceOrderCommand.java
public record PlaceOrderCommand(...) {}

// dto/response/OrderResponse.java
public record OrderResponse(...) {}

// port/in/PlaceOrderUseCase.java
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}

// ✅ 단일 액션
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}

public interface CancelOrderUseCase {
    OrderResponse execute(CancelOrderCommand command);
}
```

---

## 7) 체크리스트

Command UseCase 작성 시:
- [ ] 인터페이스명: `{Action}{Bc}UseCase`
- [ ] 패키지: `application.{bc}.port.in`
- [ ] Command DTO: `dto.command.{Action}{Bc}Command`
- [ ] Response DTO: `dto.response.{Bc}Response`
- [ ] 단일 메서드: `{Bc}Response execute({Action}{Bc}Command command)`
- [ ] Command 검증: Compact Constructor 사용
- [ ] 불변성: `List.copyOf()` 사용
- [ ] Domain 노출 금지: Response로만 반환
- [ ] Javadoc 포함: `@author`, `@since`

---

## 📖 관련 문서

- **[Command DTO Guide](../../dto/command/command-dto-guide.md)** - Command DTO 작성 규칙
- **[Response DTO Guide](../../dto/response/response-dto-guide.md)** - Response DTO 작성 규칙
- **[PersistencePort Guide](../../out/command/port-out-command-guide.md)** - Command Port 구현
- **[Query UseCase Guide](../query/port-in-query-guide.md)** - Query Port (읽기 전용)
- **[Assembler Pattern](../../../assembler/assembler-guide.md)** - DTO ↔ Domain 변환

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 2.0.0 (DTO 패키지 분리)
