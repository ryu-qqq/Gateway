# Lock Repository 가이드

> **목적**: 비관적/낙관적 락을 위한 전용 Repository 컨벤션

---

## 1️⃣ 핵심 원칙

### Lock Repository는 별도 클래스로 분리

**규칙**:
- ✅ `*LockRepository` 네이밍
- ✅ `@Repository` 클래스로 구현
- ✅ `JPAQueryFactory` 또는 `EntityManager` 사용
- ✅ Lock 관련 메서드만 제공
- ❌ 일반 조회 메서드 금지 (QueryDslRepository에서)
- ❌ Command 메서드 금지 (JpaRepository에서)

**분리 이유**:
- **명시적**: `LockRepository` 주입 = "Lock 사용" 즉시 인지
- **코드 리뷰**: Lock 사용처가 눈에 띄어 리뷰 시 주의 가능
- **관심사 분리**: Lock 전략 변경 시 한 곳에서 관리
- **테스트**: Lock 관련 테스트 독립적으로 가능

---

## 2️⃣ Lock 전략

### Pessimistic Lock (비관적 락)

**사용 케이스**:
- 재고 차감 (동시 주문)
- 포인트 차감/적립
- 좌석 예약
- 선착순 이벤트

```java
// SELECT ... FOR UPDATE
@Repository
public class OrderLockRepository {

    private final JPAQueryFactory queryFactory;
    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;

    public OrderLockRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Pessimistic Lock으로 Order 조회
     * SELECT * FROM orders WHERE id = ? FOR UPDATE
     */
    public Optional<OrderJpaEntity> findByIdForUpdate(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne()
        );
    }
}
```

### Optimistic Lock (낙관적 락)

**사용 케이스**:
- 충돌 빈도가 낮은 경우
- 긴 트랜잭션 방지
- 사용자 편집 동시성 (문서, 게시글)

```java
// Entity에 @Version 필드 필수
@Entity
public class OrderJpaEntity {
    @Id
    private Long id;

    @Version  // 낙관적 락 버전 관리
    private Long version;

    // ...
}

// Repository에서 버전 체크
@Repository
public class OrderLockRepository {

    private final EntityManager entityManager;

    /**
     * Optimistic Lock으로 Order 조회
     * 업데이트 시 version 불일치하면 OptimisticLockException 발생
     */
    public Optional<OrderJpaEntity> findByIdWithVersion(Long id) {
        return Optional.ofNullable(
            entityManager.find(OrderJpaEntity.class, id, LockModeType.OPTIMISTIC)
        );
    }
}
```

---

## 3️⃣ 기본 템플릿

```java
package com.company.adapter.out.persistence.order.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.company.adapter.out.persistence.order.entity.OrderJpaEntity;
import com.company.adapter.out.persistence.order.entity.QOrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OrderLockRepository - Order Lock 전용 Repository
 *
 * <p>비관적/낙관적 락이 필요한 조회를 담당합니다.</p>
 *
 * <p><strong>사용 케이스:</strong></p>
 * <ul>
 *   <li>재고 차감 (동시성 제어)</li>
 *   <li>포인트 처리 (원자적 연산)</li>
 *   <li>상태 변경 (경쟁 조건 방지)</li>
 * </ul>
 *
 * <p><strong>주의사항:</strong></p>
 * <ul>
 *   <li>Lock은 트랜잭션 내에서만 유효</li>
 *   <li>데드락 주의 (Lock 순서 일관성 유지)</li>
 *   <li>Lock 범위 최소화 (성능)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Repository
public class OrderLockRepository {

    private final JPAQueryFactory queryFactory;
    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;

    public OrderLockRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Pessimistic Write Lock으로 Order 조회
     *
     * <p>SELECT * FROM orders WHERE id = ? FOR UPDATE</p>
     *
     * @param id Order ID
     * @return OrderJpaEntity (Lock 획득됨)
     */
    public Optional<OrderJpaEntity> findByIdForUpdate(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne()
        );
    }

    /**
     * Pessimistic Write Lock으로 여러 Order 조회
     *
     * <p>데드락 방지를 위해 ID 오름차순 정렬</p>
     *
     * @param ids Order ID 목록
     * @return OrderJpaEntity 목록 (Lock 획득됨)
     */
    public List<OrderJpaEntity> findByIdsForUpdate(List<Long> ids) {
        return queryFactory.selectFrom(qOrder)
            .where(qOrder.id.in(ids))
            .orderBy(qOrder.id.asc())  // 데드락 방지: 항상 같은 순서로 Lock
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetch();
    }

    /**
     * Pessimistic Read Lock으로 Order 조회
     *
     * <p>SELECT * FROM orders WHERE id = ? FOR SHARE (MySQL)</p>
     * <p>다른 트랜잭션의 읽기 허용, 쓰기 차단</p>
     *
     * @param id Order ID
     * @return OrderJpaEntity (Read Lock 획득됨)
     */
    public Optional<OrderJpaEntity> findByIdForShare(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .fetchOne()
        );
    }
}
```

---

## 4️⃣ Adapter에서 사용

```java
@Component
public class OrderCommandAdapter implements OrderCommandPort {

    private final OrderRepository jpaRepository;           // Command (save, delete)
    private final OrderLockRepository lockRepository;      // Lock 조회
    private final OrderJpaEntityMapper mapper;

    public OrderCommandAdapter(
        OrderRepository jpaRepository,
        OrderLockRepository lockRepository,
        OrderJpaEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.lockRepository = lockRepository;
        this.mapper = mapper;
    }

    /**
     * Lock을 사용한 재고 차감
     */
    @Override
    @Transactional
    public void decreaseStock(Long orderId, int quantity) {
        // 1. Lock으로 조회 (FOR UPDATE)
        OrderJpaEntity entity = lockRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Domain 변환 및 로직 실행
        Order order = mapper.toDomain(entity);
        order.decreaseStock(quantity);

        // 3. 저장 (Merge)
        OrderJpaEntity updatedEntity = mapper.toEntity(order);
        jpaRepository.save(updatedEntity);
    }
}
```

---

## 5️⃣ Lock 사용 시 주의사항

### 데드락 방지

```java
// ❌ 데드락 위험: 순서 불일치
// Transaction A: Lock order 1 → Lock order 2
// Transaction B: Lock order 2 → Lock order 1

// ✅ 데드락 방지: 항상 ID 오름차순으로 Lock
public List<OrderJpaEntity> findByIdsForUpdate(List<Long> ids) {
    List<Long> sortedIds = ids.stream().sorted().toList();  // 정렬
    return queryFactory.selectFrom(qOrder)
        .where(qOrder.id.in(sortedIds))
        .orderBy(qOrder.id.asc())  // 정렬된 순서로 Lock 획득
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .fetch();
}
```

### Lock 범위 최소화

```java
// ❌ Lock 범위가 넓음
@Transactional
public void processOrder(Long orderId) {
    OrderJpaEntity entity = lockRepository.findByIdForUpdate(orderId);  // Lock 시작

    // 외부 API 호출 (Lock 유지 중 - 위험!)
    externalService.notify(entity);

    // 복잡한 비즈니스 로직
    // ...
}  // Lock 해제 (트랜잭션 종료)

// ✅ Lock 범위 최소화
@Transactional
public void processOrder(Long orderId) {
    // 1. Lock 없이 먼저 조회
    OrderJpaEntity entity = queryDslRepository.findById(orderId);

    // 2. 외부 작업 수행 (Lock 없음)
    externalService.notify(entity);

    // 3. Lock이 필요한 부분만 별도 처리
    decreaseStockWithLock(orderId, quantity);
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void decreaseStockWithLock(Long orderId, int quantity) {
    OrderJpaEntity entity = lockRepository.findByIdForUpdate(orderId);
    // 최소한의 작업만 수행
    entity.decreaseStock(quantity);
}
```

### Lock Timeout 설정

```java
// application.yml
spring:
  jpa:
    properties:
      jakarta.persistence.lock.timeout: 3000  # 3초 타임아웃

// 또는 쿼리 레벨에서 설정
public Optional<OrderJpaEntity> findByIdForUpdate(Long id) {
    return Optional.ofNullable(
        queryFactory.selectFrom(qOrder)
            .where(qOrder.id.eq(id))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", 3000)
            .fetchOne()
    );
}
```

---

## 6️⃣ 디렉토리 구조

```
adapter-out/persistence-mysql/
└─ src/main/java/
   └─ com/company/adapter/out/persistence/
       └─ order/
           ├─ entity/
           │  └─ OrderJpaEntity.java
           ├─ repository/
           │  ├─ OrderRepository.java           (JPA - Command)
           │  ├─ OrderQueryDslRepository.java   (QueryDSL - Query)
           │  └─ OrderLockRepository.java       ⭐ (Lock 전용)
           └─ adapter/
              ├─ OrderCommandAdapter.java       (JPA + Lock 사용)
              └─ OrderQueryAdapter.java         (QueryDSL 사용)
```

---

## 7️⃣ 체크리스트

Lock Repository 작성 시:
- [ ] **클래스 구조**
  - [ ] `*LockRepository` 네이밍
  - [ ] `@Repository` 어노테이션
  - [ ] `JPAQueryFactory` 생성자 주입
- [ ] **Lock 메서드**
  - [ ] `findByIdForUpdate()` - Pessimistic Write
  - [ ] `findByIdsForUpdate()` - 복수 Lock (정렬 필수)
  - [ ] `findByIdForShare()` - Pessimistic Read (필요 시)
- [ ] **안전성**
  - [ ] 데드락 방지 (ID 정렬)
  - [ ] Lock 범위 최소화
  - [ ] Timeout 설정
- [ ] **금지 사항**
  - [ ] 일반 조회 메서드 없음 (QueryDslRepository에서)
  - [ ] Command 메서드 없음 (JpaRepository에서)

---

## 8️⃣ 참고 문서

- [jpa-repository-guide.md](./jpa-repository-guide.md) - JPA Repository 가이드
- [querydsl-repository-guide.md](./querydsl-repository-guide.md) - QueryDSL Repository 가이드
- [command-adapter-guide.md](../adapter/command/command-adapter-guide.md) - Command Adapter 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
