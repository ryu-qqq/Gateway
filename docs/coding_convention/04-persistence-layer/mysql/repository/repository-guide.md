# Repository Layer 통합 가이드

> **목적**: Persistence Layer Repository 전략 및 패키지 구조 개요

---

## 1️⃣ Repository 전략 개요

### CQRS 기반 Repository 분리

```
┌─────────────────────────────────────────────────────────────────┐
│                      Repository Layer                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  JpaRepository  │  │ QueryDslRepo    │  │ AdminQueryDsl   │ │
│  │   (Command)     │  │   (Query)       │  │   (Admin)       │ │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤ │
│  │ • save()        │  │ • findById()    │  │ • Join 허용     │ │
│  │ • delete()      │  │ • existsById()  │  │ • DTO Projection│ │
│  │ • deleteById()  │  │ • findByCriteria│  │ • 자유 메서드   │ │
│  │                 │  │ • countByCriteria│ │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐                      │
│  │ LockRepository  │  │ JdbcRepository  │                      │
│  │   (Lock)        │  │ (Performance)   │                      │
│  ├─────────────────┤  ├─────────────────┤                      │
│  │ • ForUpdate     │  │ • 직접 UPDATE   │                      │
│  │ • ForShare      │  │ • 배치 처리     │                      │
│  │ • Pessimistic   │  │ • 성능 크리티컬 │                      │
│  └─────────────────┘  └─────────────────┘                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ Repository 유형별 역할

| Repository | 역할 | 네이밍 | 특징 |
|------------|------|--------|------|
| **JpaRepository** | Command (CUD) | `*Repository` | save, delete만 사용, Merge 기반 |
| **QueryDslRepository** | Query (일반 조회) | `*QueryDslRepository` | 4개 메서드, Join 금지 |
| **AdminQueryDslRepository** | Query (관리자) | `*AdminQueryDslRepository` | Join 허용, DTO Projection |
| **LockRepository** | Lock (동시성) | `*LockRepository` | Pessimistic/Optimistic Lock |
| **JdbcRepository** | Performance | `*JdbcRepository` | 직접 SQL, 배치 처리 |

---

## 3️⃣ 핵심 설계 결정

### DDD 순수성을 위한 Merge 전략

```
Domain ↔ Entity 완전 분리
    ↓
JPA save() 호출 시 Merge 동작
    ↓
SELECT + UPDATE 두 쿼리 발생 (트레이드오프)
    ↓
성능 크리티컬 → JdbcRepository로 직접 UPDATE
```

**결정 이유**:
- Domain 객체가 JPA 영속성 컨텍스트에 의존하지 않음
- 레이어 독립성 보장
- 테스트 용이성

### Join 정책

| 유형 | Join | 이유 |
|------|------|------|
| **일반 QueryDsl** | ❌ 금지 | Aggregate 경계 유지, 빠른 개발 |
| **Admin QueryDsl** | ✅ 허용 | 관리자 복잡 조회, 성능 우선 |

### Entity 연관관계

- ❌ `@OneToMany`, `@ManyToOne` 등 **연관관계 어노테이션 금지**
- ✅ `Long userId` 형태의 **Long FK만 사용**
- ✅ QueryDSL에서 **명시적 조인** (Long FK 기반)

### Adapter ↔ Repository 1:1 매핑

```
┌─────────────────────────────────────────────────────────────────┐
│                    1:1 매핑 원칙                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────┐                 │
│  │  QueryAdapter   │  1:1  │ QueryDslRepo    │                 │
│  │  ───────────────│◄─────►│                 │                 │
│  │  • repository   │       │ • findById()    │                 │
│  │  • mapper       │       │ • existsById()  │                 │
│  │  (필드 2개만)   │       │ • findByCriteria│                 │
│  └─────────────────┘       └─────────────────┘                 │
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────┐                 │
│  │ CommandAdapter  │  1:1  │  JpaRepository  │                 │
│  │  ───────────────│◄─────►│                 │                 │
│  │  • repository   │       │ • save()        │                 │
│  │  • mapper       │       │ • delete()      │                 │
│  │  (필드 2개만)   │       │                 │                 │
│  └─────────────────┘       └─────────────────┘                 │
│                                                                 │
│  ┌─────────────────┐       ┌─────────────────┐                 │
│  │ LockQueryAdapter│  1:1  │ LockRepository  │                 │
│  │  ───────────────│◄─────►│                 │                 │
│  │  • repository   │       │ • ForUpdate     │                 │
│  │  • mapper       │       │ • ForShare      │                 │
│  │  (필드 2개만)   │       │ • Optimistic    │                 │
│  └─────────────────┘       └─────────────────┘                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**핵심 원칙**:
- **Adapter = Repository + Mapper** (필드 2개만)
- 각 Adapter는 하나의 Repository에 1:1 매핑
- Adapter는 OutPort 인터페이스의 구현체
- **비즈니스 로직 금지** (단순 위임 + 변환만)

### N+1 해결 전략 (Application Layer)

```
❌ Adapter에서 N+1 해결 (금지)
──────────────────────────────
OrderQueryAdapter
├─ orderQueryDslRepository
├─ customerQueryDslRepository  ← 금지! 1:1 위반
└─ mapper


✅ Application Layer에서 해결 (권장)
──────────────────────────────
OrderQueryUseCase (Application Layer)
├─ orderQueryPort.findByCriteria()     → 주문 목록
├─ customerQueryPort.findByIds()       → 고객 목록 (IN 절)
└─ 조합 및 Response 생성               → Application에서 처리
```

**N+1 해결 패턴**:
```java
// Application Layer (UseCase)
@Component
public class OrderQueryUseCase {
    private final OrderQueryPort orderQueryPort;       // Order Adapter
    private final CustomerQueryPort customerQueryPort; // Customer Adapter

    @Transactional(readOnly = true)
    public List<OrderWithCustomerResponse> findOrdersWithCustomer(
            OrderSearchCriteria criteria) {
        // 1. 주문 조회 (Order Adapter)
        List<Order> orders = orderQueryPort.findByCriteria(criteria);

        // 2. 고객 ID 수집
        Set<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .collect(Collectors.toSet());

        // 3. 고객 일괄 조회 (Customer Adapter) - IN 절로 N+1 해결
        Map<Long, Customer> customerMap = customerQueryPort.findByIds(customerIds)
            .stream()
            .collect(Collectors.toMap(c -> c.getId().getValue(), c -> c));

        // 4. 조합 (Application Layer에서 처리)
        return orders.stream()
            .map(order -> new OrderWithCustomerResponse(
                order,
                customerMap.get(order.getCustomerId())
            ))
            .toList();
    }
}
```

---

## 4️⃣ 디렉토리 구조

```
adapter-out/persistence-mysql/
└─ src/main/java/
   └─ com/company/adapter/out/persistence/
       └─ order/
           ├─ entity/
           │  └─ OrderJpaEntity.java
           │
           ├─ repository/
           │  ├─ OrderRepository.java              (JPA - Command)
           │  ├─ OrderQueryDslRepository.java      (QueryDSL - 일반 Query)
           │  ├─ OrderAdminQueryDslRepository.java (QueryDSL - 관리자)
           │  ├─ OrderLockRepository.java          (Lock - 동시성)
           │  └─ OrderJdbcRepository.java          (JDBC - 성능)
           │
           ├─ mapper/
           │  └─ OrderJpaEntityMapper.java
           │
           └─ adapter/
              ├─ OrderCommandAdapter.java          (JPA + Lock)
              ├─ OrderQueryAdapter.java            (QueryDSL)
              └─ OrderAdminQueryAdapter.java       (Admin QueryDSL)
```

---

## 5️⃣ 문서 구조

```
repository/
├── repository-guide.md          ← 현재 문서 (통합 개요)
│
├── jpa/
│   ├── jpa-repository-guide.md       Command 전용, Merge 전략
│   └── jpa-repository-archunit.md    7개 규칙
│
├── querydsl/
│   ├── querydsl-repository-guide.md       4개 메서드, Join 금지
│   ├── querydsl-repository-archunit.md    9개 규칙
│   └── querydsl-repository-test-guide.md  테스트 가이드
│
├── admin/
│   └── admin-querydsl-repository-archunit.md  6개 규칙, Join 허용
│
└── lock/
    ├── lock-repository-guide.md           Pessimistic/Optimistic Lock
    └── lock-repository-archunit.md        6개 규칙
```

---

## 6️⃣ 사용 기준

### 어떤 Repository를 사용할까?

```
┌─────────────────────────────────────────────────────────────┐
│                    Repository 선택 가이드                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  저장/삭제 필요?                                            │
│  ├─ Yes → JpaRepository.save() / delete()                  │
│  │                                                          │
│  조회 필요?                                                 │
│  ├─ 단순 조회 (ID, 목록) → QueryDslRepository              │
│  ├─ 관리자 복잡 조회 (Join) → AdminQueryDslRepository      │
│  │                                                          │
│  동시성 제어 필요?                                          │
│  ├─ 재고, 포인트, 좌석 예약 → LockRepository               │
│  │                                                          │
│  성능 크리티컬?                                             │
│  └─ 대량 배치, 고빈도 업데이트 → JdbcRepository            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 케이스별 권장 조합

| 케이스 | 조합 |
|--------|------|
| **일반 CRUD** | JpaRepository + QueryDslRepository |
| **관리자 목록** | AdminQueryDslRepository |
| **재고 차감** | LockRepository + JpaRepository |
| **대량 업데이트** | JdbcRepository |
| **복합 트랜잭션** | LockRepository + JpaRepository + QueryDslRepository |

---

## 7️⃣ 체크리스트

Repository 설계 시:
- [ ] **역할 분리**
  - [ ] Command는 JpaRepository
  - [ ] Query는 QueryDslRepository
  - [ ] 관리자 조회는 AdminQueryDslRepository
  - [ ] Lock은 LockRepository
- [ ] **네이밍 규칙**
  - [ ] `*Repository` (JPA)
  - [ ] `*QueryDslRepository` (일반 Query)
  - [ ] `*AdminQueryDslRepository` (관리자)
  - [ ] `*LockRepository` (Lock)
- [ ] **Adapter ↔ Repository 1:1 매핑**
  - [ ] Adapter 필드 2개만 (Repository + Mapper)
  - [ ] Adapter에 다른 Repository 주입 금지
  - [ ] N+1 해결은 Application Layer에서
- [ ] **Join 정책**
  - [ ] 일반 QueryDsl: Join 금지
  - [ ] Admin QueryDsl: Join 허용 (Long FK 기반)
- [ ] **Entity 연관관계**
  - [ ] 연관관계 어노테이션 금지
  - [ ] Long FK만 사용

---

## 8️⃣ 참고 문서

### JPA Repository
- [jpa/jpa-repository-guide.md](./jpa/jpa-repository-guide.md) - Command 전용 가이드
- [jpa/jpa-repository-archunit.md](./jpa/jpa-repository-archunit.md) - ArchUnit 규칙

### QueryDSL Repository
- [querydsl/querydsl-repository-guide.md](./querydsl/querydsl-repository-guide.md) - Query 전용 가이드
- [querydsl/querydsl-repository-archunit.md](./querydsl/querydsl-repository-archunit.md) - ArchUnit 규칙
- [querydsl/querydsl-repository-test-guide.md](./querydsl/querydsl-repository-test-guide.md) - 테스트 가이드

### Admin QueryDSL Repository
- [admin/admin-querydsl-repository-archunit.md](./admin/admin-querydsl-repository-archunit.md) - ArchUnit 규칙

### Lock Repository
- [lock/lock-repository-guide.md](./lock/lock-repository-guide.md) - Lock 전용 가이드
- [lock/lock-repository-archunit.md](./lock/lock-repository-archunit.md) - ArchUnit 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
