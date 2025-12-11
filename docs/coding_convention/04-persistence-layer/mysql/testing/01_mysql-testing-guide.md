# Persistence MySQL Layer 테스트 가이드

> **목적**: MySQL Persistence Layer의 테스트 작성 규칙 및 패턴 정의

---

## 1. 개요

### Persistence MySQL Layer 테스트 전략

```
┌─────────────────────────────────────────────────────────────────┐
│  Persistence MySQL Layer 테스트 피라미드                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                      ┌─────────┐                                 │
│                      │ 통합    │  ← Repository 통합 테스트        │
│                      │ Test    │    TestContainers + MySQL       │
│                  ┌───┴─────────┴───┐                             │
│                  │  Slice Test     │  ← @DataJpaTest (선택적)     │
│                  │  (JPA Only)     │    H2/TestContainers        │
│              ┌───┴─────────────────┴───┐                         │
│              │  Unit Test              │  ← Mapper/Entity 단위    │
│              │  (Mapper, Entity)       │    순수 JUnit 5          │
│          ┌───┴─────────────────────────┴───┐                     │
│          │  ArchUnit Tests                 │  ← 아키텍처 검증 (필수)│
│          │  (Architecture Rules)           │    Zero-Tolerance    │
│          └─────────────────────────────────┘                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 테스트 유형별 역할

| 테스트 유형 | 필수 여부 | 목적 | 도구 |
|------------|----------|------|------|
| **통합 테스트** | ✅ 필수 | Repository + DB 실제 동작 검증 | TestContainers + MySQL |
| **Slice 테스트** | 🔶 선택 | JPA 레이어만 빠르게 검증 | @DataJpaTest |
| **단위 테스트** | 🔶 선택 | Mapper, Entity 로직 검증 | JUnit 5 |
| **ArchUnit** | ✅ 필수 | 아키텍처 규칙 강제 | ArchUnit |

### 왜 통합 테스트가 중요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│  MySQL과의 실제 통합 검증이 필요한 이유                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ❌ H2 단독 테스트 문제점:                                       │
│     • MySQL 전용 문법 (JSON 타입, ON DUPLICATE KEY 등) 미지원    │
│     • 트랜잭션 격리 수준 차이                                    │
│     • 인덱스/실행 계획 차이                                      │
│     • 데이터 타입 미묘한 차이 (DATETIME vs TIMESTAMP)            │
│                                                                  │
│  ✅ TestContainers + MySQL 장점:                                 │
│     • 운영 환경과 동일한 DB 버전 사용                            │
│     • 실제 SQL 쿼리 동작 검증                                    │
│     • Flyway 마이그레이션 검증                                   │
│     • 성능 특성 파악 가능                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 유형별 상세 가이드

### 2.1 통합 테스트 (Repository Integration Test)

> **상세 가이드**: [Repository 통합 테스트](./02_repository-integration-test.md)

**적용 대상**:
- JpaRepository CRUD 동작
- QueryDslRepository 복잡 쿼리
- Adapter의 Port 구현 검증

**필수 어노테이션**:
```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
```

### 2.2 Slice 테스트 (@DataJpaTest)

> **상세 가이드**: [Repository Slice 테스트](./03_repository-slice-test.md)

**적용 대상**:
- 빠른 피드백이 필요한 JPA 쿼리 검증
- CI/CD 파이프라인 빠른 테스트

**특징**:
- JPA 관련 Bean만 로드
- 기본 H2 사용 (TestContainers 연동 가능)
- 빠른 실행 속도

### 2.3 단위 테스트 (Mapper/Entity Unit Test)

> **상세 가이드**: [Mapper 단위 테스트](./04_mapper-unit-test.md)

**적용 대상**:
- EntityMapper의 변환 로직
- Entity의 정적 팩토리 메서드

**특징**:
- Spring Context 불필요
- 순수 JUnit 5
- 가장 빠른 실행

---

## 3. Zero-Tolerance 규칙

### 3.1 필수 규칙 ✅

| 규칙 | 설명 | 검증 방법 |
|------|------|----------|
| TestContainers MySQL | 운영 환경 동일 DB | ArchUnit |
| `@Transactional` | 테스트 격리 (롤백) | ArchUnit |
| `@Sql` INSERT만 | DDL 금지 (Flyway 역할) | Code Review |
| Mapper 테스트 필수 | Domain ↔ Entity 변환 검증 | Coverage |

### 3.2 금지 규칙 ❌

| 금지 항목 | 이유 | 대안 |
|----------|------|------|
| H2 단독 통합 테스트 | MySQL 호환성 문제 | TestContainers |
| `@MockBean` Repository | 실제 쿼리 미검증 | 실제 Repository |
| Entity에 `@Transactional` | Persistence Layer 책임 아님 | Application Layer |
| `@Sql`에 DDL | Flyway 역할 침범 | Flyway 마이그레이션 |
| Lombok in Test | Plain Java 원칙 | 수동 생성자 |

### 3.3 H2 vs TestContainers 선택 기준

```
통합 테스트 DB 선택:
    │
    ├─ 간단한 CRUD 검증만? ───────────── H2 (@DataJpaTest)
    │
    ├─ MySQL 전용 기능 사용? ─────────── TestContainers MySQL
    │   • JSON 타입
    │   • ON DUPLICATE KEY UPDATE
    │   • 특정 함수 (DATE_FORMAT 등)
    │
    ├─ QueryDSL 복잡 쿼리? ───────────── TestContainers MySQL
    │
    └─ Flyway 마이그레이션 검증? ──────── TestContainers MySQL
```

---

## 4. 테스트 데이터 관리

### 4.1 @Sql 파일 구조

```
src/test/resources/sql/
├── common/
│   └── cleanup.sql              # 공통 정리 스크립트
├── order/
│   ├── orders-test-data.sql     # 주문 테스트 데이터
│   └── orders-edge-cases.sql    # 엣지 케이스
└── tenant/
    └── tenants-test-data.sql    # 테넌트 테스트 데이터
```

### 4.2 @Sql 작성 규칙

```sql
-- src/test/resources/sql/order/orders-test-data.sql

-- 1. 기존 데이터 정리 (FK 순서: 자식 먼저)
DELETE FROM order_items;
DELETE FROM orders;

-- 2. 테스트 데이터 삽입
INSERT INTO orders (id, customer_id, status, total_amount, created_at)
VALUES
    (100, 1, 'PENDING', 10000, NOW()),
    (101, 1, 'CONFIRMED', 20000, NOW()),
    (102, 2, 'SHIPPED', 30000, NOW());

-- 3. 시퀀스/AUTO_INCREMENT 조정 (필요시)
ALTER TABLE orders AUTO_INCREMENT = 200;
```

### 4.3 TestFixtures 활용

복잡한 테스트 데이터는 TestFixtures 패턴을 사용합니다.

```java
// testFixtures 모듈에 정의
public final class OrderJpaEntityFixture {

    private OrderJpaEntityFixture() {
    }

    public static OrderJpaEntity pending(Long id, Long customerId) {
        return OrderJpaEntity.create(
            id,
            customerId,
            OrderStatus.PENDING,
            Money.of(10000)
        );
    }

    public static OrderJpaEntity confirmed(Long id, Long customerId) {
        return OrderJpaEntity.create(
            id,
            customerId,
            OrderStatus.CONFIRMED,
            Money.of(20000)
        );
    }
}
```

---

## 5. 테스트 디렉토리 구조

```
persistence-mysql/src/test/java/
└── com/ryuqq/adapter/out/persistence/
    ├── architecture/                    # ArchUnit 테스트
    │   ├── PersistenceLayerArchTest.java
    │   ├── entity/
    │   │   └── JpaEntityArchTest.java
    │   ├── repository/
    │   │   ├── JpaRepositoryArchTest.java
    │   │   └── QueryDslRepositoryArchTest.java
    │   ├── mapper/
    │   │   └── MapperArchTest.java
    │   └── adapter/
    │       ├── command/
    │       │   └── CommandAdapterArchTest.java
    │       └── query/
    │           └── QueryAdapterArchTest.java
    │
    ├── common/                          # 공통 테스트 지원
    │   ├── RepositoryTestSupport.java   # Repository 테스트 기반 클래스
    │   ├── JpaSliceTestSupport.java     # @DataJpaTest 기반 클래스
    │   └── MapperTestSupport.java       # Mapper 테스트 기반 클래스
    │
    └── {bc}/                            # Bounded Context별 테스트
        ├── repository/
        │   ├── OrderJpaRepositoryTest.java
        │   └── OrderQueryDslRepositoryTest.java
        ├── mapper/
        │   └── OrderJpaEntityMapperTest.java
        └── adapter/
            ├── OrderCommandAdapterTest.java
            └── OrderQueryAdapterTest.java
```

---

## 6. 체크리스트

### 통합 테스트 작성 전

- [ ] TestContainers MySQL 설정 확인
- [ ] `@SpringBootTest` + `@Testcontainers` 사용
- [ ] `@ActiveProfiles("test")` 설정
- [ ] `@Transactional` 설정 (테스트 격리)
- [ ] application-test.yml DB 설정 확인

### 테스트 메서드 작성

- [ ] `@DisplayName`으로 테스트 의도 명시
- [ ] `@Sql`로 테스트 데이터 준비 (INSERT만)
- [ ] Given-When-Then 구조 준수
- [ ] 쿼리 결과 정확성 검증
- [ ] N+1 문제 검증 (필요시)

### 금지 사항 확인

- [ ] H2 단독 통합 테스트 사용하지 않음
- [ ] `@MockBean` Repository 사용하지 않음
- [ ] Entity에 `@Transactional` 사용하지 않음
- [ ] `@Sql`에 DDL 작성하지 않음

---

## 7. 참고 문서

- [Repository 통합 테스트](./02_repository-integration-test.md)
- [Repository Slice 테스트](./03_repository-slice-test.md)
- [Mapper 단위 테스트](./04_mapper-unit-test.md)
- [Persistence MySQL 전체 가이드](../persistence-mysql-guide.md)
- [Test Fixtures 가이드](../../../05-testing/test-fixtures/01_test-fixtures-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
