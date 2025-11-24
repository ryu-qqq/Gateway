# PersistencePort (Command Port) — **CQRS Command 추상화**

> PersistencePort는 Domain Aggregate를 영속화하는 **쓰기 전용** Port입니다.
>
> `persist()` 메서드 **하나**로 INSERT/UPDATE를 통합 처리합니다.

---

## 1) 핵심 역할

* **Domain Aggregate 영속화**: 신규 생성 또는 수정을 하나의 메서드로 처리
* **CQRS Command 담당**: 쓰기(CUD) 전용 Port
* **JPA 더티체킹 활용**: ID 없음 → INSERT, ID 있음 → UPDATE
* **Value Object 반환**: 저장된 Entity의 ID를 Value Object로 반환

---

## 2) 핵심 원칙

### 원칙 1: persist() 메서드 하나만
- ✅ `persist({Bc} {bc})` 메서드 하나로 신규/수정 통합
- ❌ `save()`, `update()`, `delete()` 메서드 금지

### 원칙 2: Domain Aggregate 파라미터
- ✅ Domain Aggregate 객체를 파라미터로 받음
- ❌ DTO, Entity 파라미터 금지

### 원칙 3: Value Object 반환
- ✅ 저장된 ID를 Value Object로 반환 (`{Bc}Id`)
- ❌ 원시 타입 반환 금지 (`Long`, `String` 등)

### 원칙 4: 조회 메서드 금지
- ✅ 쓰기 전용 Port (Command)
- ❌ 조회 메서드는 QueryPort로 분리

---

## 3) 템플릿 코드

```java
package com.ryuqq.application.{bc}.port.out.command;

import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;

/**
 * {Bc} Persistence Port (Command)
 *
 * <p>Domain Aggregate를 영속화하는 쓰기 전용 Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface {Bc}PersistencePort {

    /**
     * {Bc} 저장 (신규 생성 또는 수정)
     *
     * <p>신규 생성 (ID 없음) → INSERT</p>
     * <p>기존 수정 (ID 있음) → UPDATE (JPA 더티체킹)</p>
     *
     * @param {bc} 저장할 {Bc} (Domain Aggregate)
     * @return 저장된 {Bc}의 ID (Value Object)
     */
    {Bc}Id persist({Bc} {bc});
}
```

---

## 4) 실전 예시 (Order)

```java
package com.ryuqq.application.order.port.out.command;

import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;

/**
 * Order Persistence Port (Command)
 *
 * <p>Order Aggregate를 영속화하는 쓰기 전용 Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface OrderPersistencePort {

    /**
     * Order 저장 (신규 생성 또는 수정)
     *
     * <p>신규 생성 (ID 없음) → INSERT</p>
     * <p>기존 수정 (ID 있음) → UPDATE (JPA 더티체킹)</p>
     *
     * @param order 저장할 Order (Domain Aggregate)
     * @return 저장된 Order의 ID
     */
    OrderId persist(Order order);
}
```

---

## 5) Do / Don't

### ❌ Bad Examples

```java
// ❌ save(), update(), delete() 메서드
public interface OrderPersistencePort {
    void save(Order order);    // 금지!
    void update(Order order);  // 금지!
    void delete(OrderId id);   // 금지!
}

// ❌ 조회 메서드 포함
public interface OrderPersistencePort {
    OrderId persist(Order order);
    Optional<Order> findById(OrderId id);  // 금지! QueryPort로 분리
}

// ❌ 원시 타입 반환
public interface OrderPersistencePort {
    Long persist(Order order);  // 금지! OrderId 반환
}

// ❌ DTO 파라미터
public interface OrderPersistencePort {
    OrderId persist(OrderDto dto);  // 금지! Domain Aggregate 사용
}

// ❌ Entity 파라미터
public interface OrderPersistencePort {
    OrderId persist(OrderJpaEntity entity);  // 금지! Domain Aggregate 사용
}
```

### ✅ Good Examples

```java
// ✅ persist() 메서드 하나만
public interface OrderPersistencePort {
    OrderId persist(Order order);
}

// ✅ Domain Aggregate 파라미터, Value Object 반환
public interface OrderPersistencePort {
    OrderId persist(Order order);  // Order (Domain), OrderId (VO)
}

// ✅ 패키지 위치
// application/{bc}/port/out/command/{Bc}PersistencePort.java
package com.ryuqq.application.order.port.out.command;

public interface OrderPersistencePort {
    OrderId persist(Order order);
}
```

---

## 6) 체크리스트

PersistencePort 작성 시:
- [ ] 인터페이스명: `{Bc}PersistencePort`
- [ ] 패키지: `application.{bc}.port.out.command`
- [ ] `persist({Bc} {bc})` 메서드 하나만 제공
- [ ] 반환 타입: `{Bc}Id` (Value Object)
- [ ] 파라미터: `{Bc}` (Domain Aggregate)
- [ ] 조회 메서드 없음 (QueryPort로 분리)
- [ ] `save()`, `update()`, `delete()` 메서드 금지
- [ ] Javadoc 포함 (`@author`, `@since`)
- [ ] 메서드 설명에 INSERT/UPDATE 로직 명시

---

## 📖 관련 문서

- **[CommandAdapter Guide](../../../../04-persistence-layer/mysql/adapter/command/command-adapter-guide.md)** - PersistencePort 구현체
- **[QueryPort Guide](../query/port-out-query-guide.md)** - Query Port (읽기 전용)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
