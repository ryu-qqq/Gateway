# Spring Data JPA Repository 가이드

> **목적**: Spring Data JPA Repository 인터페이스 컨벤션 (Command 전용)

---

## 1️⃣ 핵심 원칙

### JPA Repository는 순수하게 JpaRepository만 상속

**규칙**:
- ✅ 인터페이스만 정의 (구현체 불필요)
- ✅ `JpaRepository<Entity, ID>` 상속만
- ❌ `QuerydslPredicateExecutor` 상속 금지
- ❌ Query Method 추가 금지
- ❌ `@Query` JPQL 추가 금지
- ❌ Custom Repository 구현 금지

**이유**:
- JPA Repository는 **Command 작업 (save, delete)만** 담당
- 모든 Query 작업은 **QueryDSL Repository**로 분리
- 관심사 분리 (CQRS 패턴)

---

## 2️⃣ 기본 템플릿

```java
package com.company.adapter.out.persistence.order.repository;

import com.company.adapter.out.persistence.order.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OrderRepository - Order JPA Repository
 *
 * <p>Spring Data JPA Repository로서 Order Entity의 기본 CRUD를 담당합니다.</p>
 *
 * <p><strong>제공 메서드 (Command 전용):</strong></p>
 * <ul>
 *   <li>save(entity): 저장/수정 (INSERT/UPDATE)</li>
 *   <li>delete(entity): 삭제 (DELETE)</li>
 *   <li>deleteById(id): ID로 삭제</li>
 * </ul>
 *
 * <p><strong>Query 작업:</strong></p>
 * <ul>
 *   <li>모든 Query 작업은 OrderQueryDslRepository 사용</li>
 *   <li>findById(), findAll() 등도 QueryDslRepository에서</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    // ❌ Query Method 추가 금지
    // ❌ @Query 추가 금지
    // ❌ QuerydslPredicateExecutor 상속 금지
}
```

---

## 3️⃣ 예시

### ✅ 올바른 예시

```java
// ✅ JpaRepository만 상속
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}

```

### ❌ 위반 예시

```java
// ❌ QuerydslPredicateExecutor 상속 금지
public interface OrderRepository extends
    JpaRepository<OrderJpaEntity, Long>,
    QuerydslPredicateExecutor<OrderJpaEntity> {  // ❌
}

// ❌ Query Method 추가 금지
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);  // ❌
}

// ❌ @Query 추가 금지
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.status = :status")  // ❌
    List<OrderJpaEntity> findByStatus(@Param("status") OrderStatus status);
}

// ❌ Custom Repository 구현 금지
public interface OrderRepositoryCustom {  // ❌
    List<OrderJpaEntity> searchOrders(SearchOrderQuery query);
}
```

---

## 7️⃣ 체크리스트

JPA Repository 작성 시:
- [ ] **인터페이스로 정의**
- [ ] **JpaRepository<Entity, ID>만 상속**
- [ ] **금지 사항**
  - [ ] QuerydslPredicateExecutor 상속 안 함
  - [ ] Query Method 추가 안 함
  - [ ] @Query 추가 안 함
  - [ ] Custom Repository 구현 안 함
- [ ] **네이밍**
  - [ ] `*Repository` 네이밍 규칙

---

## 8️⃣ 참고 문서

- [querydsl-repository-guide.md](./querydsl-repository-guide.md) - QueryDSL Repository 가이드
- [jpa-repository-archunit.md](./jpa-repository-archunit.md) - ArchUnit 규칙
- [repository-test-guide.md](./repository-test-guide.md) - 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
