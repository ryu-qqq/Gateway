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

## 2️⃣ Command 처리 전략 (Merge 기반)

### DDD 순수성을 위한 설계 결정

**아키텍처 흐름**:
```
QueryDSL 조회 → Entity → Mapper → Domain (Application Layer에서 조립/로직 실행)
    → Domain 값 변경 → Mapper → Entity → JPA save() (Merge)
```

**핵심 원칙**:
- ✅ **Domain ↔ Entity 완전 분리**: 매퍼를 통한 변환으로 레이어 독립성 보장
- ✅ **DDD 순수성 유지**: Domain 객체는 영속성 레이어와 무관하게 동작
- ⚠️ **Merge 동작 인지**: `save()` 호출 시 SELECT + UPDATE 두 쿼리 발생

### Merge vs Dirty Checking

```java
// ❌ Dirty Checking (영속 엔티티 직접 수정 - 우리 방식 아님)
@Transactional
void update(Long id) {
    Entity entity = entityManager.find(Entity.class, id); // 영속
    entity.setName("changed"); // 직접 수정
    // 트랜잭션 커밋 → UPDATE 1회
}

// ✅ Merge (우리 방식 - DDD 순수성 유지)
@Transactional
void update(Domain domain) {
    Entity entity = mapper.toEntity(domain); // 새 Entity 객체 (비영속)
    jpaRepository.save(entity); // Merge 동작
    // ID 존재 → SELECT 1회 + UPDATE 1회
}
```

**Merge를 선택한 이유**:
- Domain과 Entity의 완전한 분리 (레이어 독립성)
- Domain 객체가 JPA 영속성 컨텍스트에 의존하지 않음
- 테스트 용이성 (Domain 단위 테스트 시 JPA 불필요)

**트레이드오프**:
- 쿼리 1회 추가 발생 (SELECT)
- 대부분의 경우 PK 인덱스 조회로 성능 영향 미미

---

## 3️⃣ 성능 최적화 전략

### 성능 크리티컬 케이스: JdbcRepository 사용

Merge의 추가 SELECT가 **성능 병목**이 되는 경우:
- 대량 배치 업데이트
- 초당 수천 건 이상의 고빈도 업데이트
- 레이턴시 민감한 API

**해결책**: JdbcRepository로 직접 UPDATE 쿼리

```java
// JdbcRepository (성능 크리티컬 케이스)
@Repository
@RequiredArgsConstructor
public class OrderJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 성능 크리티컬: 직접 UPDATE (SELECT 없이 1회 쿼리)
     */
    public void updateStatus(Long orderId, String status) {
        jdbcTemplate.update(
            "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?",
            status, orderId
        );
    }

    /**
     * 대량 배치 업데이트
     */
    public void batchUpdateStatus(List<Long> orderIds, String status) {
        jdbcTemplate.batchUpdate(
            "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, status);
                    ps.setLong(2, orderIds.get(i));
                }
                @Override
                public int getBatchSize() {
                    return orderIds.size();
                }
            }
        );
    }
}
```

### 사용 기준

| 케이스 | 권장 방식 | 이유 |
|--------|----------|------|
| 일반 CRUD | JPA Repository (Merge) | DDD 순수성, 코드 간결성 |
| 대량 배치 | JdbcRepository | 성능 (N개 → 1개 쿼리) |
| 고빈도 업데이트 | JdbcRepository | 레이턴시 최소화 |
| 단순 상태 변경 | JdbcRepository 고려 | SELECT 생략 가능 |

**원칙**:
> 기본은 JPA Merge, 성능 문제 **측정 후** JdbcRepository 도입

---

## 4️⃣ 기본 템플릿

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

## 5️⃣ 예시

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

## 6️⃣ 테스트 전략

### JPA Repository는 단위 테스트 불필요

**이유**:
- ✅ **Spring Data JPA 검증 완료**: save(), delete(), deleteById() 등 검증된 구현 제공
- ✅ **ArchUnit으로 규칙 검증**: 구조적 규칙은 ArchUnit이 자동 검증
- ✅ **통합 테스트에서 검증**: Adapter 테스트에서 실제 동작 검증

**결론**: JPA Repository는 테스트 파일을 작성하지 않습니다.

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
- [lock-repository-guide.md](./lock-repository-guide.md) - Lock Repository 가이드
- [jpa-repository-archunit.md](./jpa-repository-archunit.md) - ArchUnit 규칙
- [querydsl-repository-test-guide.md](./querydsl-repository-test-guide.md) - QueryDSL Repository 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.1.0
