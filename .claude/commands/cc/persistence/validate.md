# Persistence Layer Coding Convention Validation

**목적**: TDD로 작성된 Persistence Layer 코드가 프로젝트 코딩 컨벤션을 준수하는지 검증하고, 위반 사항에 대한 리팩토링 PRD를 생성합니다.

---

## 🎯 검증 범위

### 1. Persistence Layer 코딩 컨벤션 문서

다음 문서들의 규칙을 기준으로 검증합니다:

```
docs/coding_convention/04-persistence-layer/
├── mysql/
│   ├── entity/  (JPA Entity)
│   ├── repository/  (JPA Repository, QueryDSL Repository)
│   ├── adapter/  (Command/Query Adapter)
│   ├── mapper/  (Entity ↔ Domain 변환)
│   └── config/  (Flyway, HikariCP)
├── redis/
│   ├── adapter/
│   └── config/  (Cache Configuration)
└── persistence-guide.md
```

### 2. 핵심 검증 항목

#### Zero-Tolerance 규칙 (절대 위반 금지)
- **Long FK 전략**: JPA 관계 어노테이션 금지 (`@OneToMany`, `@ManyToOne` 등)
- **Long userId 패턴**: Entity는 FK를 `Long userId` 형태로만 보유
- **Lombok 금지**: Entity, Mapper에서 Lombok 사용 금지
- **QueryDSL DTO Projection**: Entity를 직접 반환하지 않고 DTO Projection 사용
- **N+1 방지**: Fetch Join 또는 Batch Size 설정 필수

#### 구조 규칙
- **Entity 네이밍**: `*JpaEntity` 접미사
- **Mapper 네이밍**: `*PersistenceMapper`
- **Adapter 네이밍**: `*PersistenceAdapter` (Command), `*QueryAdapter` (Query)
- **Repository 분리**: JpaRepository + QueryDSL Repository
- **Flyway 마이그레이션**: 모든 스키마 변경은 Flyway로 관리

#### 테스트 규칙
- **@DataJpaTest**: Repository 테스트
- **@Sql**: 테스트 데이터 준비
- **Testcontainers**: 실제 MySQL 사용 (H2 금지)
- **QueryDSL 테스트**: DTO Projection 검증

---

## 🔍 검증 프로세스

### 1단계: Long FK 전략 검증 (Zero-Tolerance)

```markdown
**검증 대상**: `persistence-mysql/src/main/java/**/entity/*JpaEntity.java`

**검증 항목**:
1. JPA 관계 어노테이션 사용 금지
   - ❌ `@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`
   - ✅ `private Long userId;` (FK는 Long 타입만)

2. Lazy Loading 문제 방지
   - ❌ `@ManyToOne(fetch = FetchType.LAZY) private User user;`
   - ✅ `private Long userId;` (연관 엔티티 로딩 없음)

3. Cascade 문제 방지
   - ❌ `@OneToMany(cascade = CascadeType.ALL) private List<OrderItem> items;`
   - ✅ 명시적 저장 (Repository 직접 호출)
```

**Serena MCP 활용**:
```python
# 1. JPA 관계 어노테이션 검색
search_for_pattern(
    substring_pattern="@(OneToMany|ManyToOne|OneToOne|ManyToMany)",
    relative_path="persistence-mysql/src/main/java/**/entity",
    restrict_search_to_code_files=True
)

# 2. Entity 클래스에서 연관 엔티티 필드 검색 (안티패턴)
search_for_pattern(
    substring_pattern="private\\s+(List<|Set<)?[A-Z][a-zA-Z0-9_]*JpaEntity",
    relative_path="persistence-mysql/src/main/java/**/entity"
)

# 3. Long FK 패턴 준수 여부
find_symbol(
    name_path="JpaEntity",
    relative_path="persistence-mysql/src/main/java/**/entity",
    substring_matching=True,
    include_body=True
)
```

### 2단계: Lombok 사용 검증 (Zero-Tolerance)

```markdown
**검증 대상**:
- `persistence-mysql/src/main/java/**/entity/*JpaEntity.java`
- `persistence-mysql/src/main/java/**/mapper/*PersistenceMapper.java`

**검증 항목**:
1. Entity에서 Lombok 금지
   - ❌ `@Getter`, `@Setter`, `@Data`, `@Builder`
   - ✅ Plain Java (수동 구현)

2. Mapper에서 Lombok 금지
   - ❌ `@RequiredArgsConstructor`
   - ✅ 생성자 직접 작성
```

**Serena MCP 활용**:
```python
# Entity에서 Lombok 어노테이션 검색
search_for_pattern(
    substring_pattern="@(Getter|Setter|Data|Builder|AllArgsConstructor|NoArgsConstructor|RequiredArgsConstructor)",
    relative_path="persistence-mysql/src/main/java/**/entity"
)

# Mapper에서 Lombok 어노테이션 검색
search_for_pattern(
    substring_pattern="@(Getter|Setter|Data|RequiredArgsConstructor)",
    relative_path="persistence-mysql/src/main/java/**/mapper"
)
```

### 3단계: QueryDSL DTO Projection 검증 (Zero-Tolerance)

```markdown
**검증 대상**: `persistence-mysql/src/main/java/**/repository/querydsl/`

**검증 항목**:
1. Entity 직접 반환 금지
   - ❌ `List<OrderJpaEntity> findAll()`
   - ✅ `List<OrderQueryDto> findAll()` (DTO Projection)

2. Projections 사용
   - ✅ `Projections.constructor(OrderQueryDto.class, ...)`
   - ✅ `Projections.fields(OrderQueryDto.class, ...)`

3. N+1 방지
   - ✅ Fetch Join 사용
   - ✅ Batch Size 설정
   - ❌ Lazy Loading 문제 방치
```

**Serena MCP 활용**:
```python
# 1. Entity 직접 반환 패턴 검색 (안티패턴)
search_for_pattern(
    substring_pattern="List<.*JpaEntity>\\s+find",
    relative_path="persistence-mysql/src/main/java/**/repository/querydsl"
)

# 2. DTO Projection 사용 여부
search_for_pattern(
    substring_pattern="Projections\\.(constructor|fields|bean)",
    relative_path="persistence-mysql/src/main/java/**/repository/querydsl"
)

# 3. Fetch Join 사용 여부
search_for_pattern(
    substring_pattern="\\.fetchJoin\\(\\)",
    relative_path="persistence-mysql/src/main/java/**/repository/querydsl"
)
```

### 4단계: Adapter 패턴 검증

```markdown
**검증 대상**:
- `persistence-mysql/src/main/java/**/adapter/command/*PersistenceAdapter.java`
- `persistence-mysql/src/main/java/**/adapter/query/*QueryAdapter.java`

**검증 항목**:
1. Adapter 네이밍
   - ✅ `OrderPersistenceAdapter` (Command)
   - ✅ `OrderQueryAdapter` (Query)
   - ❌ `OrderRepositoryImpl` (헷갈림)

2. Port 구현
   - ✅ `implements SaveOrderPort`
   - ✅ `implements LoadOrderPort`

3. Mapper 사용
   - ✅ Entity ↔ Domain 변환은 Mapper 사용
   - ❌ Adapter에서 직접 변환
```

**Serena MCP 활용**:
```python
# 1. Adapter 클래스 탐색
find_symbol(
    name_path="Adapter",
    relative_path="persistence-mysql/src/main/java/**/adapter",
    substring_matching=True,
    include_kinds=[5]  # Class
)

# 2. Port 구현 여부
search_for_pattern(
    substring_pattern="implements\\s+.*Port",
    relative_path="persistence-mysql/src/main/java/**/adapter"
)

# 3. Mapper 사용 여부
search_for_pattern(
    substring_pattern="private\\s+.*PersistenceMapper",
    relative_path="persistence-mysql/src/main/java/**/adapter"
)
```

### 5단계: Mapper 패턴 검증

```markdown
**검증 대상**: `persistence-mysql/src/main/java/**/mapper/*PersistenceMapper.java`

**검증 항목**:
1. Mapper 메서드 네이밍
   - ✅ `toEntity()`: Domain → Entity
   - ✅ `toDomain()`: Entity → Domain
   - ❌ `convert()`, `map()` (모호함)

2. Mapper는 순수 변환 로직만
   - ❌ Repository 호출
   - ❌ 비즈니스 로직

3. Lombok 미사용
   - ✅ 생성자 직접 작성
```

### 6단계: Flyway 마이그레이션 검증

```markdown
**검증 대상**: `persistence-mysql/src/main/resources/db/migration/`

**검증 항목**:
1. 마이그레이션 파일 네이밍
   - ✅ `V1__create_order_table.sql`
   - ❌ `V1.sql`, `create_order.sql`

2. DDL 변경은 Flyway로만
   - ❌ JPA `spring.jpa.hibernate.ddl-auto=update`
   - ✅ Flyway 마이그레이션 파일

3. Rollback 스크립트 존재
   - ✅ `V1__create_order_table.sql` + Rollback 주석
```

### 7단계: 테스트 검증

```markdown
**검증 대상**:
- `persistence-mysql/src/test/java/**/repository/`
- `persistence-mysql/src/test/java/**/adapter/`

**검증 항목**:
1. Repository 테스트
   - @DataJpaTest 사용
   - Testcontainers (실제 MySQL)
   - @Sql 테스트 데이터

2. Adapter 테스트
   - Integration Test
   - Mapper Mock 사용

3. QueryDSL 테스트
   - DTO Projection 검증
   - N+1 문제 검증
```

---

## 📊 검증 결과 리포트

### 리포트 형식

```markdown
# Persistence Layer 코딩 컨벤션 검증 결과

**프로젝트**: claude-spring-standards
**검증 날짜**: {검증 실행 날짜}
**검증 범위**: persistence-mysql/src/main/java, persistence-mysql/src/test/java

---

## ✅ 준수 항목 (통과)

### Long FK 전략
- [✓] JPA 관계 어노테이션 미사용
- [✓] Long userId 패턴 준수

### QueryDSL DTO Projection
- [✓] Entity 직접 반환 금지
- [✓] Projections 사용

### Lombok
- [✓] Entity에서 Lombok 미사용
- [✓] Mapper에서 Lombok 미사용

---

## ❌ 위반 항목 (리팩토링 필요)

### 1. JPA 관계 어노테이션 사용 (Zero-Tolerance)

**파일**: `persistence-mysql/src/main/java/.../entity/OrderJpaEntity.java:23`

```java
// ❌ 위반 (@ManyToOne 사용)
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // ❌ 관계 어노테이션
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;
}

// ✅ 개선 (Long FK 전략)
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // ✅ Long FK
}
```

**심각도**: 🔴 CRITICAL (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 2. Entity 직접 반환 (Zero-Tolerance)

**파일**: `persistence-mysql/src/main/java/.../repository/querydsl/OrderQueryDslRepositoryImpl.java:15`

```java
// ❌ 위반 (Entity 직접 반환)
@Override
public List<OrderJpaEntity> findAllOrders() {
    return queryFactory
        .selectFrom(orderJpaEntity)
        .fetch();
}

// ✅ 개선 (DTO Projection)
@Override
public List<OrderQueryDto> findAllOrders() {
    return queryFactory
        .select(Projections.constructor(
            OrderQueryDto.class,
            orderJpaEntity.id,
            orderJpaEntity.userId,
            orderJpaEntity.totalAmount,
            orderJpaEntity.status
        ))
        .from(orderJpaEntity)
        .fetch();
}
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 3. Lombok 사용 (Zero-Tolerance)

**파일**: `persistence-mysql/src/main/java/.../entity/OrderJpaEntity.java:8`

```java
// ❌ 위반 (Lombok 사용)
@Entity
@Table(name = "orders")
@Getter  // ❌ Lombok
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // ❌ Lombok
public class OrderJpaEntity {
    // ...
}

// ✅ 개선 (Plain Java)
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Getter 직접 구현
    public Long getId() {
        return id;
    }

    // Protected 생성자 직접 구현
    protected OrderJpaEntity() {
    }
}
```

**심각도**: 🔴 HIGH (Zero-Tolerance)
**리팩토링 필요**: 즉시

---

### 4. Mapper에서 직접 변환 (Adapter)

**파일**: `persistence-mysql/src/main/java/.../adapter/OrderPersistenceAdapter.java:25`

```java
// ❌ 위반 (Adapter에서 직접 변환)
@Override
public Order save(Order order) {
    OrderJpaEntity entity = new OrderJpaEntity(
        order.getId(),
        order.getUserId(),
        order.getTotalAmount()
    );

    OrderJpaEntity saved = orderJpaRepository.save(entity);
    return new Order(saved.getId(), saved.getUserId(), saved.getTotalAmount());
}

// ✅ 개선 (Mapper 사용)
@Override
public Order save(Order order) {
    OrderJpaEntity entity = orderPersistenceMapper.toEntity(order);
    OrderJpaEntity saved = orderJpaRepository.save(entity);
    return orderPersistenceMapper.toDomain(saved);
}
```

**심각도**: 🟡 MEDIUM
**리팩토링 필요**: 권장

---

## 📋 리팩토링 우선순위

### Priority 1 (즉시 수정 필요)
1. JPA 관계 어노테이션 제거 (3건)
2. Entity 직접 반환 제거 (4건)
3. Lombok 제거 (6건)

### Priority 2 (권장)
1. Mapper 패턴 적용 (5건)
2. N+1 방지 (Fetch Join) (3건)

### Priority 3 (선택)
1. 테스트 누락 (7건)
2. Flyway 마이그레이션 추가 (2건)

---

## 🎯 리팩토링 PRD 생성 여부

**위반 항목 수**: 30건
**Zero-Tolerance 위반**: 13건

→ **리팩토링 PRD 생성 필수**
```

---

## 🚀 리팩토링 PRD 자동 생성

위반 항목이 발견되면 자동으로 리팩토링 PRD를 생성합니다.

### PRD 생성 조건

```yaml
auto_generate_prd:
  conditions:
    - zero_tolerance_violations > 0  # Zero-Tolerance 위반 1건 이상
    - jpa_relationship_count > 0     # JPA 관계 어노테이션 1건 이상
    - entity_return_count > 0        # Entity 직접 반환 1건 이상

  prd_location: "docs/prd/refactoring/{ISSUE-KEY}-persistence-refactoring.md"
```

### PRD 템플릿

```markdown
# Persistence Layer 리팩토링 PRD

**이슈 키**: REFACTOR-PERSIST-001
**생성 날짜**: {생성 날짜}
**우선순위**: CRITICAL
**예상 소요 시간**: {위반 건수 기반 자동 계산}

---

## 📋 리팩토링 개요

**목적**: Persistence Layer 코딩 컨벤션 위반 사항 해결
**범위**: persistence-mysql/src/main/java, persistence-mysql/src/test/java
**위반 항목 수**: {총 위반 건수}
**Zero-Tolerance 위반**: {심각도 HIGH/CRITICAL 건수}

---

## 🎯 리팩토링 목표

### 필수 목표 (Zero-Tolerance)
- [ ] JPA 관계 어노테이션 제거 (3건)
- [ ] Entity 직접 반환 제거 (4건)
- [ ] Lombok 제거 (6건)

### 권장 목표
- [ ] Mapper 패턴 적용 (5건)
- [ ] N+1 방지 (3건)

---

## 📝 상세 리팩토링 계획

### Task 1: JPA 관계 어노테이션 제거 (Long FK 전략)

**파일**: OrderJpaEntity.java:23

**Before**:
```java
@Entity
public class OrderJpaEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;
}
```

**After**:
```java
@Entity
public class OrderJpaEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
```

**TDD 사이클**:
1. **Struct**: `struct: @ManyToOne 제거, Long userId로 변경` (Flyway 마이그레이션 먼저)
2. **Test**: `test: Long userId로 조회 테스트 추가`
3. **Green**: `feat: Long FK 패턴 적용 완료`

---

### Task 2: Entity 직접 반환 → DTO Projection

**파일**: OrderQueryDslRepositoryImpl.java:15

**Before**:
```java
public List<OrderJpaEntity> findAllOrders() {
    return queryFactory
        .selectFrom(orderJpaEntity)
        .fetch();
}
```

**After**:
```java
public List<OrderQueryDto> findAllOrders() {
    return queryFactory
        .select(Projections.constructor(
            OrderQueryDto.class,
            orderJpaEntity.id,
            orderJpaEntity.userId,
            orderJpaEntity.totalAmount
        ))
        .from(orderJpaEntity)
        .fetch();
}
```

**TDD 사이클**:
1. **Struct**: `struct: OrderQueryDto record 생성`
2. **Test**: `test: DTO Projection 테스트 추가`
3. **Green**: `feat: Projections.constructor() 적용`

---

## ✅ 완료 조건

### Definition of Done
- [ ] 모든 JPA 관계 어노테이션 제거
- [ ] 모든 Entity 직접 반환 제거
- [ ] 모든 Lombok 제거
- [ ] ArchUnit 테스트 통과
- [ ] Repository 테스트 모두 통과

### 검증 방법
```bash
# ArchUnit 실행
./gradlew :persistence-mysql:test --tests "*ArchitectureTest"

# Repository 테스트
./gradlew :persistence-mysql:test

# 코딩 컨벤션 재검증
/cc/persistence/validate
```

---

## 📊 예상 메트릭

**예상 커밋 수**: {위반 건수 * 2.5} (Flyway + TDD)
**예상 소요 시간**: {위반 건수 * 25분}
**우선순위별 분포**:
- Priority 1: 13건 (325분)
- Priority 2: 8건 (200분)
- Priority 3: 9건 (225분)

**총 예상 시간**: 약 12.5시간
```

---

## 🛠️ 실행 방법

```bash
# Persistence Layer 검증 실행
/cc/persistence/validate

# MySQL만 검증
/cc/persistence/validate --target mysql

# Redis만 검증
/cc/persistence/validate --target redis

# 리팩토링 PRD 강제 생성
/cc/persistence/validate --force-prd
```

---

## 🎯 검증 프로세스

1. **Serena MCP**로 Persistence Layer 코드 탐색
2. **Long FK 전략** Zero-Tolerance 규칙 검증
3. **QueryDSL DTO Projection** 검증
4. **Lombok 사용** 검증
5. **Adapter/Mapper 패턴** 검증
6. **Flyway 마이그레이션** 검증
7. **위반 항목 리포트** 생성
8. **리팩토링 PRD** 자동 생성

---

## 📌 참고 문서

- `docs/coding_convention/04-persistence-layer/mysql/persistence-mysql-guide.md`
- `docs/coding_convention/04-persistence-layer/mysql/entity/entity-guide.md`
- `docs/coding_convention/04-persistence-layer/mysql/repository/querydsl-repository-guide.md`
- `.claude/CLAUDE.md` (Long FK 전략)
