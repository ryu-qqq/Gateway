# CommandAdapter 가이드

> **목적**: Domain Aggregate를 JPA Entity로 변환하여 저장하는 CQRS Command Adapter 구현 가이드

---

## 1️⃣ CommandAdapter란?

### 역할
**Domain Aggregate → JPA Entity → DB 저장**

CQRS의 Command(쓰기) 담당으로, Domain Model을 영속화하는 **단순 저장소** 역할만 수행합니다.

### 책임
- ✅ Domain Model을 Entity로 변환
- ✅ JpaRepository.save() 호출
- ✅ ID 반환
- ❌ **비즈니스 로직 금지** (Domain 메서드에서 처리)
- ❌ **조회 로직 금지** (QueryAdapter로 분리)

### 핵심 원칙
```
Application Layer (UseCase)
  ├─ 1. Domain 조회 (Load Port)
  ├─ 2. 비즈니스 로직 실행 (Domain 메서드 호출)
  │     └─ order.confirm()
  │     └─ order.softDelete()
  │     └─ order.updateStatus()
  └─ 3. 변경 반영 (Persist Port)
        └─ CommandAdapter.persist(order)  ← 단순 저장만!
              └─ save() → JPA 더티체킹 자동 UPDATE
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: 비즈니스 로직은 Domain 에서

### 원칙 2: 더티체킹 활용 (신규/수정 통합)
```java
// ✅ persist() 하나로 신규/수정 모두 처리
public OrderId persist(Order order) {
    OrderJpaEntity entity = mapper.toEntity(order);
    OrderJpaEntity saved = repository.save(entity);
    return OrderId.of(saved.getId());
}

// JPA가 알아서 판단:
// - ID 없음 → INSERT
// - ID 있음 → 더티체킹으로 UPDATE
```

### 원칙 3: CQRS 엄격 분리
- **Command**: 저장/수정/삭제만 (JpaRepository)
- **Query**: 조회는 QueryAdapter로 (QueryDSL)

### 원칙 3: Transaction 어노테이션 절대 금지
- **Transaction** 은 Application 레이어에서 담당

---

## 3️⃣  템플릿 코드

### 기본 템플릿
```java
@Component
public class {Bc}CommandAdapter implements {Bc}PersistencePort {

    private final {Bc}JpaRepository {bc}JpaRepository;
    private final {Bc}JpaEntityMapper {bc}JpaEntityMapper;

    public {Bc}CommandAdapter(
        {Bc}JpaRepository {bc}JpaRepository,
        {Bc}JpaEntityMapper {bc}JpaEntityMapper
    ) {
        this.{bc}JpaRepository = {bc}JpaRepository;
        this.{bc}JpaEntityMapper = {bc}JpaEntityMapper;
    }

    /**
     * Bc 저장 (신규 생성 또는 수정)
     *
     * <p>신규 생성 (ID 없음) → JPA가 ID 자동 할당 (INSERT)</p>
     * <p>기존 수정 (ID 있음) → 더티체킹으로 자동 UPDATE</p>
     *
     * @param bc 저장할 Bc (Domain)
     * @return 저장된 Bc의 ID
     */
    @Override
    public {Bc}Id persist(Bc bc) {
        // 1. Domain → Entity 변환
        {Bc}JpaEntity entity = {bc}JpaEntityMapper.toEntity(bc);

        // 2. JPA 저장 (신규/수정 JPA가 자동 판단)
        {Bc}JpaEntity savedEntity = {bc}JpaRepository.save(entity);

        // 3. ID 반환
        return {Bc}Id.of(savedEntity.getId());
    }
}
```

---

## 8️⃣ 체크리스트

CommandAdapter 구현 시:
- [ ] `@Component` 어노테이션 추가
- [ ] Port 인터페이스 구현
- [ ] `persist()` 메서드만 제공 (update, delete 메서드 없음)
- [ ] JpaRepository 의존성 주입
- [ ] Mapper 의존성 주입
- [ ] 비즈니스 로직 없음 (단순 변환/저장만)
- [ ] Query 메서드 없음 (QueryAdapter로 분리)
- [ ] Domain → Entity 변환 사용
- [ ] save() 호출로 더티체킹 활용
- [ ] ID 반환
- [ ] @Transaction 절대 금지
---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
