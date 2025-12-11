# Adapter Layer 통합 가이드

> **목적**: Persistence Layer Adapter 전략 및 패키지 구조 개요

---

## 1️⃣ Adapter 전략 개요

### CQRS 기반 Adapter 분리

```
┌─────────────────────────────────────────────────────────────────┐
│                       Adapter Layer                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                    Command Adapter                         │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │  CommandAdapter                                           │ │
│  │  └─ JpaRepository (1:1) + Mapper                         │ │
│  │  └─ save(), delete(), deleteById()                       │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                     Query Adapters                         │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐ │ │
│  │  │  QueryAdapter   │ │AdminQueryAdapter│ │LockQueryAdp │ │ │
│  │  │   (일반 조회)   │ │   (관리자)      │ │   (Lock)    │ │ │
│  │  ├─────────────────┤ ├─────────────────┤ ├─────────────┤ │ │
│  │  │• QueryDslRepo   │ │• AdminQueryDsl  │ │• LockRepo   │ │ │
│  │  │• 4개 메서드     │ │• Join 허용      │ │• 6개 메서드 │ │ │
│  │  │• Domain 반환    │ │• DTO Projection │ │• Domain 반환│ │ │
│  │  └─────────────────┘ └─────────────────┘ └─────────────┘ │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ 핵심 원칙: 1:1 매핑

### Adapter = Repository + Mapper (필드 2개만)

```
┌─────────────────────────────────────────────────────────────────┐
│                    1:1 매핑 원칙                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Adapter                           Repository                   │
│  ─────────                         ──────────                   │
│                                                                 │
│  ┌─────────────────┐      1:1      ┌─────────────────┐         │
│  │ CommandAdapter  │◄────────────►│  JpaRepository  │         │
│  │ • repository    │               │ • save()        │         │
│  │ • mapper        │               │ • delete()      │         │
│  └─────────────────┘               └─────────────────┘         │
│                                                                 │
│  ┌─────────────────┐      1:1      ┌─────────────────┐         │
│  │  QueryAdapter   │◄────────────►│ QueryDslRepo    │         │
│  │ • repository    │               │ • findById()    │         │
│  │ • mapper        │               │ • findByCriteria│         │
│  └─────────────────┘               └─────────────────┘         │
│                                                                 │
│  ┌─────────────────┐      1:1      ┌─────────────────┐         │
│  │AdminQueryAdapter│◄────────────►│AdminQueryDslRepo│         │
│  │ • repository    │               │ • Join 허용     │         │
│  │ (mapper 선택적) │               │ • DTO Projection│         │
│  └─────────────────┘               └─────────────────┘         │
│                                                                 │
│  ┌─────────────────┐      1:1      ┌─────────────────┐         │
│  │LockQueryAdapter │◄────────────►│  LockRepository │         │
│  │ • repository    │               │ • ForUpdate     │         │
│  │ • mapper        │               │ • ForShare      │         │
│  └─────────────────┘               └─────────────────┘         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**핵심 규칙**:
- ✅ 각 Adapter는 **하나의 Repository에만** 의존
- ✅ 필드 **2개만** 허용 (Repository + Mapper)
- ✅ **비즈니스 로직 금지** (단순 위임 + 변환만)
- ❌ 다른 Repository 주입 금지

---

## 3️⃣ Adapter 유형별 역할

| Adapter | 역할 | Repository | 반환 타입 |
|---------|------|------------|----------|
| **CommandAdapter** | Command (CUD) | JpaRepository | Domain |
| **QueryAdapter** | Query (일반 조회) | QueryDslRepository | Domain |
| **AdminQueryAdapter** | Query (관리자) | AdminQueryDslRepository | DTO |
| **LockQueryAdapter** | Lock (동시성) | LockRepository | Domain |

### 상세 비교

| 항목 | CommandAdapter | QueryAdapter | AdminQueryAdapter | LockQueryAdapter |
|------|---------------|--------------|-------------------|------------------|
| **메서드 수** | 3개 | 4개 | 자유 | 6개 |
| **Mapper** | 필수 | 필수 | 선택적 | 필수 |
| **Join** | N/A | ❌ 금지 | ✅ 허용 | N/A |
| **반환** | Domain | Domain | DTO | Domain |

---

## 4️⃣ N+1 해결 전략

### Application Layer에서 해결

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
    private final OrderQueryPort orderQueryPort;
    private final CustomerQueryPort customerQueryPort;

    @Transactional(readOnly = true)
    public List<OrderWithCustomerResponse> findOrdersWithCustomer(
            OrderSearchCriteria criteria) {
        // 1. 주문 조회
        List<Order> orders = orderQueryPort.findByCriteria(criteria);

        // 2. 고객 ID 수집
        Set<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .collect(Collectors.toSet());

        // 3. 고객 일괄 조회 (IN 절로 N+1 해결)
        Map<Long, Customer> customerMap = customerQueryPort.findByIds(customerIds)
            .stream()
            .collect(Collectors.toMap(c -> c.getId().getValue(), c -> c));

        // 4. 조합
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

## 5️⃣ 디렉토리 구조

### 소스 코드 구조
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
           │  ├─ OrderQueryDslRepository.java      (QueryDSL - 일반)
           │  ├─ OrderAdminQueryDslRepository.java (QueryDSL - 관리자)
           │  └─ OrderLockRepository.java          (Lock)
           │
           ├─ mapper/
           │  └─ OrderJpaEntityMapper.java
           │
           └─ adapter/
              ├─ OrderCommandAdapter.java          (JpaRepository 1:1)
              ├─ OrderQueryAdapter.java            (QueryDslRepository 1:1)
              ├─ OrderAdminQueryAdapter.java       (AdminQueryDslRepository 1:1)
              └─ OrderLockQueryAdapter.java        (LockRepository 1:1)
```

### 문서 구조
```
adapter/
├── adapter-guide.md              ← 현재 문서 (통합 개요)
│
├── command/
│   ├── command-adapter-guide.md
│   ├── command-adapter-test-guide.md
│   └── command-adapter-archunit.md
│
└── query/
    ├── general/                  ← 일반 QueryAdapter
    │   ├── query-adapter-guide.md
    │   ├── query-adapter-test-guide.md
    │   ├── query-adapter-integration-testing.md
    │   └── query-adapter-archunit.md
    │
    ├── admin/                    ← 관리자 AdminQueryAdapter
    │   ├── admin-query-adapter-guide.md
    │   └── admin-query-adapter-archunit.md
    │
    └── lock/                     ← LockQueryAdapter
        ├── lock-query-adapter-guide.md
        ├── lock-query-adapter-test-guide.md
        └── lock-query-adapter-archunit.md
```

---

## 6️⃣ 사용 기준

### 어떤 Adapter를 사용할까?

```
┌─────────────────────────────────────────────────────────────┐
│                    Adapter 선택 가이드                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  저장/삭제 필요?                                            │
│  ├─ Yes → CommandAdapter                                   │
│  │                                                          │
│  조회 필요?                                                 │
│  ├─ 단순 조회 (ID, 목록) → QueryAdapter                    │
│  ├─ 관리자 복잡 조회 (Join) → AdminQueryAdapter            │
│  │                                                          │
│  동시성 제어 필요?                                          │
│  └─ 재고, 포인트, 좌석 예약 → LockQueryAdapter             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 케이스별 권장 조합

| 케이스 | Adapter 조합 |
|--------|-------------|
| **일반 CRUD** | CommandAdapter + QueryAdapter |
| **관리자 목록** | AdminQueryAdapter |
| **재고 차감** | LockQueryAdapter + CommandAdapter |
| **복합 조회** | Application Layer에서 여러 QueryPort 조합 |

---

## 7️⃣ 체크리스트

Adapter 설계 시:

### 1:1 매핑 원칙
- [ ] 각 Adapter는 하나의 Repository만 의존
- [ ] 필드 2개만 (Repository + Mapper)
- [ ] 다른 Repository 주입 금지
- [ ] N+1 해결은 Application Layer에서

### 역할 분리
- [ ] Command는 CommandAdapter
- [ ] 일반 Query는 QueryAdapter
- [ ] 관리자 조회는 AdminQueryAdapter
- [ ] Lock은 LockQueryAdapter

### 네이밍 규칙
- [ ] `*CommandAdapter` (Command)
- [ ] `*QueryAdapter` (일반 Query)
- [ ] `*AdminQueryAdapter` (관리자)
- [ ] `*LockQueryAdapter` (Lock)

### 금지 사항
- [ ] 비즈니스 로직 없음
- [ ] @Transactional 금지
- [ ] 다른 Repository 주입 금지

---

## 8️⃣ 참고 문서

### Command Adapter
- [command/command-adapter-guide.md](./command/command-adapter-guide.md) - Command Adapter 가이드
- [command/command-adapter-archunit.md](./command/command-adapter-archunit.md) - ArchUnit 규칙

### Query Adapter (일반)
- [query/general/query-adapter-guide.md](./query/general/query-adapter-guide.md) - Query Adapter 가이드
- [query/general/query-adapter-archunit.md](./query/general/query-adapter-archunit.md) - ArchUnit 규칙
- [query/general/query-adapter-integration-testing.md](./query/general/query-adapter-integration-testing.md) - 통합 테스트 가이드

### Admin Query Adapter
- [query/admin/admin-query-adapter-guide.md](./query/admin/admin-query-adapter-guide.md) - Admin Query Adapter 가이드
- [query/admin/admin-query-adapter-archunit.md](./query/admin/admin-query-adapter-archunit.md) - ArchUnit 규칙

### Lock Query Adapter
- [query/lock/lock-query-adapter-guide.md](./query/lock/lock-query-adapter-guide.md) - Lock Query Adapter 가이드
- [query/lock/lock-query-adapter-archunit.md](./query/lock/lock-query-adapter-archunit.md) - ArchUnit 규칙

### Repository Layer
- [../repository/repository-guide.md](../repository/repository-guide.md) - Repository Layer 통합 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
