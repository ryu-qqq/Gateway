# QueryPort (Query Port) — **CQRS Query 추상화**

> QueryPort는 Domain Aggregate를 조회하는 **읽기 전용** Port입니다.
>
> `findById()`, `existsById()`, `findByCriteria()`, `countByCriteria()` 메서드를 제공합니다.

---

## 1) 핵심 역할

* **Domain Aggregate 조회**: 단건, 목록, 개수 조회
* **CQRS Query 담당**: 읽기(R) 전용 Port
* **Value Object 파라미터**: 타입 안전성 확보
* **Domain 반환**: DTO가 아닌 Domain 객체 반환

---

## 2) 핵심 원칙

### 원칙 1: 조회 메서드만 제공
- ✅ `findById()`, `existsById()`, `findByCriteria()`, `countByCriteria()`
- ❌ 저장/수정/삭제 메서드 금지 (PersistencePort로 분리)

### 원칙 2: Value Object 파라미터
- ✅ Value Object 사용 (`{Bc}Id`, `{Bc}SearchCriteria`)
- ❌ 원시 타입 파라미터 금지 (`Long id`, `String name` 등)

### 원칙 3: Domain 반환
- ✅ Domain Aggregate 반환
- ❌ DTO, Entity 반환 금지

### 원칙 4: Optional 반환 (단건 조회)
- ✅ 단건 조회 시 `Optional<{Bc}>` 반환
- ❌ `null` 반환 금지

---

## 3) 템플릿 코드

```java
package com.ryuqq.application.{bc}.port.out.query;

import com.ryuqq.application.{bc}.dto.query.{Bc}SearchCriteria;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;

import java.util.List;
import java.util.Optional;

/**
 * {Bc} Query Port (Query)
 *
 * <p>Domain Aggregate를 조회하는 읽기 전용 Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface {Bc}QueryPort {

    /**
     * ID로 {Bc} 단건 조회
     *
     * @param id {Bc} ID (Value Object)
     * @return {Bc} Domain (Optional)
     */
    Optional<{Bc}> findById({Bc}Id id);

    /**
     * ID로 {Bc} 존재 여부 확인
     *
     * @param id {Bc} ID (Value Object)
     * @return 존재 여부
     */
    boolean existsById({Bc}Id id);

    /**
     * 검색 조건으로 {Bc} 목록 조회
     *
     * @param criteria 검색 조건 (DTO)
     * @return {Bc} Domain 목록
     */
    List<{Bc}> findByCriteria({Bc}SearchCriteria criteria);

    /**
     * 검색 조건으로 {Bc} 개수 조회
     *
     * @param criteria 검색 조건 (DTO)
     * @return {Bc} 개수
     */
    long countByCriteria({Bc}SearchCriteria criteria);
}
```

---

## 4) 실전 예시 (Order)

```java
package com.ryuqq.application.order.port.out.query;

import com.ryuqq.application.order.dto.query.OrderSearchCriteria;
import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Order Query Port (Query)
 *
 * <p>Order Aggregate를 조회하는 읽기 전용 Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface OrderQueryPort {

    /**
     * ID로 Order 단건 조회
     *
     * @param id Order ID (Value Object)
     * @return Order Domain (Optional)
     */
    Optional<Order> findById(OrderId id);

    /**
     * ID로 Order 존재 여부 확인
     *
     * @param id Order ID (Value Object)
     * @return 존재 여부
     */
    boolean existsById(OrderId id);

    /**
     * 검색 조건으로 Order 목록 조회
     *
     * @param criteria 검색 조건 (DTO)
     * @return Order Domain 목록
     */
    List<Order> findByCriteria(OrderSearchCriteria criteria);

    /**
     * 검색 조건으로 Order 개수 조회
     *
     * @param criteria 검색 조건 (DTO)
     * @return Order 개수
     */
    long countByCriteria(OrderSearchCriteria criteria);
}
```

---

## 5) Do / Don't

### ❌ Bad Examples

```java
// ❌ 저장 메서드 포함
public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);
    void save(Order order);  // 금지! PersistencePort로 분리
}

// ❌ 원시 타입 파라미터
public interface OrderQueryPort {
    Optional<Order> findById(Long id);  // 금지! OrderId 사용
}

// ❌ DTO 반환
public interface OrderQueryPort {
    Optional<OrderDto> findById(OrderId id);  // 금지! Domain 반환
}

// ❌ Entity 반환
public interface OrderQueryPort {
    Optional<OrderJpaEntity> findById(OrderId id);  // 금지! Domain 반환
}

// ❌ null 반환
public interface OrderQueryPort {
    Order findById(OrderId id);  // 금지! Optional 사용
}
```

### ✅ Good Examples

```java
// ✅ 조회 메서드만
public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);
    boolean existsById(OrderId id);
    List<Order> findByCriteria(OrderSearchCriteria criteria);
    long countByCriteria(OrderSearchCriteria criteria);
}

// ✅ Value Object 파라미터, Domain 반환
public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);  // OrderId (VO), Order (Domain)
}

// ✅ Optional 반환 (단건 조회)
public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);  // Optional 사용
}

// ✅ 패키지 위치
// application/{bc}/port/out/query/{Bc}QueryPort.java
package com.ryuqq.application.order.port.out.query;

public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);
    boolean existsById(OrderId id);
    List<Order> findByCriteria(OrderSearchCriteria criteria);
    long countByCriteria(OrderSearchCriteria criteria);
}
```

---

## 6) 체크리스트

QueryPort 작성 시:
- [ ] 인터페이스명: `{Bc}QueryPort`
- [ ] 패키지: `application.{bc}.port.out.query`
- [ ] `findById({Bc}Id id)` 메서드 제공
- [ ] `existsById({Bc}Id id)` 메서드 제공
- [ ] `findByCriteria({Bc}SearchCriteria criteria)` 메서드 제공
- [ ] `countByCriteria({Bc}SearchCriteria criteria)` 메서드 제공
- [ ] Value Object 파라미터 사용 (`{Bc}Id`, `{Bc}SearchCriteria`)
- [ ] Domain 반환 (DTO, Entity 반환 금지)
- [ ] Optional 반환 (단건 조회 시)
- [ ] 저장/수정/삭제 메서드 없음 (PersistencePort로 분리)
- [ ] Javadoc 포함 (`@author`, `@since`)

---

## 📖 관련 문서

- **[QueryAdapter Guide](../../../../04-persistence-layer/mysql/adapter/query/query-adapter-guide.md)** - QueryPort 구현체
- **[PersistencePort Guide](../command/port-out-command-guide.md)** - Command Port (쓰기 전용)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
