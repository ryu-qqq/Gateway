# LockQueryAdapter 가이드

> **목적**: Lock을 걸고 조회하는 CQRS Query Adapter 구현 가이드 (1:1 매핑)

---

## 1️⃣ LockQueryAdapter란?

### 역할
**Lock Query 요청 → LockRepository → Entity → Mapper → Domain 반환**

Lock을 걸고 조회하는 Query(읽기) 담당으로, LockRepository에 1:1 매핑되어 위임하고 Mapper를 통해 Domain으로 변환하여 반환합니다.

### 책임
- ✅ Lock 조회만 담당 (ForUpdate, ForShare, Optimistic)
- ✅ LockRepository 1:1 매핑
- ✅ Mapper를 통한 Entity → Domain 변환
- ❌ **비즈니스 로직 금지**
- ❌ **저장/수정/삭제 금지** (CommandAdapter로)
- ❌ **일반 조회 금지** (QueryAdapter로)

### 핵심 원칙
```
Application Layer (UseCase)
  └─ Lock Query Port 호출
      └─ LockQueryAdapter (1:1 매핑)
            └─ LockRepository
                  └─ DB Lock 조회
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: 1:1 매핑
- **LockQueryAdapter ↔ LockRepository**: 정확히 1:1 매핑
- **필드 2개만**: LockRepository + Mapper

### 원칙 2: Lock 조회만 담당
- **Lock Query**: ForUpdate, ForShare, Optimistic 조회만
- **Command**: 저장/수정/삭제는 CommandAdapter로
- **일반 Query**: Lock 없는 조회는 QueryAdapter로

### 원칙 3: LockRepository 위임 + Mapper 변환
```java
// ✅ LockRepository 호출 → Mapper 변환 → Domain 반환
public Optional<Bc> findByIdForUpdate(BcId id) {
    return lockRepository.findByIdForUpdate(id.getValue())
        .map(bcJpaEntityMapper::toDomain);
}

// ❌ 비즈니스 로직 금지
public Optional<Bc> findByIdForUpdate(BcId id) {
    Optional<Bc> bc = lockRepository.findByIdForUpdate(id.getValue())
        .map(bcJpaEntityMapper::toDomain);

    if (bc.isPresent() && bc.get().isExpired()) {  // 금지!
        return Optional.empty();
    }
    return bc;
}
```

### 원칙 4: Type-Safe Value Objects
```java
// ✅ Value Object 사용 (타입 안전성)
public Optional<Bc> findByIdForUpdate(BcId id) {
    return lockRepository.findByIdForUpdate(id.getValue())
        .map(bcJpaEntityMapper::toDomain);
}

// ❌ 원시 타입 사용 금지
public Optional<Bc> findByIdForUpdate(Long id) {  // 금지!
}
```

### 원칙 5: 6개 메서드 고정 (통일된 네이밍)

| Lock 유형 | 단건 조회 | 목록 조회 | SQL |
|-----------|----------|----------|-----|
| **Pessimistic Write** | `findByIdForUpdate` | `findByCriteriaForUpdate` | SELECT ... FOR UPDATE |
| **Pessimistic Read** | `findByIdForShare` | `findByCriteriaForShare` | SELECT ... FOR SHARE |
| **Optimistic** | `findByIdWithOptimisticLock` | `findByCriteriaWithOptimisticLock` | @Version 사용 |

### 원칙 6: Lock 예외 명시 (JavaDoc)
```java
// ✅ JavaDoc에 @throws 명시 (처리는 안 함)
/**
 * ID로 Bc 단건 조회 (FOR UPDATE)
 *
 * @param id Bc ID
 * @return Bc Domain (Optional)
 * @throws PessimisticLockException Lock 획득 실패 시
 * @throws LockTimeoutException Lock 타임아웃 시
 */
public Optional<Bc> findByIdForUpdate(BcId id) {
    // 예외를 catch하지 않고 그대로 던짐
    return lockRepository.findByIdForUpdate(id.getValue())
        .map(bcJpaEntityMapper::toDomain);
}

// ❌ try-catch로 예외 처리 금지 (Application Layer에서 처리)
public Optional<Bc> findByIdForUpdate(BcId id) {
    try {
        return lockRepository.findByIdForUpdate(id.getValue())
            .map(bcJpaEntityMapper::toDomain);
    } catch (PessimisticLockException e) {  // 금지!
        throw new CustomLockException(e);
    }
}
```

**예외 처리 책임**:
- ✅ **Adapter**: JavaDoc에 `@throws` 명시만 (catch 안 함)
- ✅ **Application Layer**: try-catch로 예외 처리 (재시도, 실패 등)

---

## 3️⃣ 템플릿 코드

### Port 인터페이스
```java
public interface {Bc}LockQueryPort {

    // Pessimistic Write Lock (FOR UPDATE)
    Optional<Bc> findByIdForUpdate({Bc}Id id);
    List<Bc> findByCriteriaForUpdate({Bc}SearchCriteria criteria);

    // Pessimistic Read Lock (FOR SHARE)
    Optional<Bc> findByIdForShare({Bc}Id id);
    List<Bc> findByCriteriaForShare({Bc}SearchCriteria criteria);

    // Optimistic Lock (@Version)
    Optional<Bc> findByIdWithOptimisticLock({Bc}Id id);
    List<Bc> findByCriteriaWithOptimisticLock({Bc}SearchCriteria criteria);
}
```

### Adapter 구현
```java
@Component
public class {Bc}LockQueryAdapter implements {Bc}LockQueryPort {

    private final {Bc}LockRepository lockRepository;
    private final {Bc}JpaEntityMapper {bc}JpaEntityMapper;

    public {Bc}LockQueryAdapter(
        {Bc}LockRepository lockRepository,
        {Bc}JpaEntityMapper {bc}JpaEntityMapper
    ) {
        this.lockRepository = lockRepository;
        this.{bc}JpaEntityMapper = {bc}JpaEntityMapper;
    }

    // ========================================================================
    // Pessimistic Write Lock (FOR UPDATE)
    // ========================================================================

    /**
     * ID로 Bc 단건 조회 (FOR UPDATE)
     *
     * @param id Bc ID
     * @return Bc Domain (Optional)
     * @throws PessimisticLockException Lock 획득 실패 시
     * @throws LockTimeoutException Lock 타임아웃 시
     */
    @Override
    public Optional<Bc> findByIdForUpdate({Bc}Id id) {
        return lockRepository.findByIdForUpdate(id.getValue())
            .map({bc}JpaEntityMapper::toDomain);
    }

    /**
     * Criteria로 Bc 목록 조회 (FOR UPDATE)
     *
     * @param criteria 검색 조건
     * @return Bc Domain 목록
     * @throws PessimisticLockException Lock 획득 실패 시
     * @throws LockTimeoutException Lock 타임아웃 시
     */
    @Override
    public List<Bc> findByCriteriaForUpdate({Bc}SearchCriteria criteria) {
        List<{Bc}JpaEntity> entities = lockRepository.findByCriteriaForUpdate(criteria);
        return entities.stream()
            .map({bc}JpaEntityMapper::toDomain)
            .toList();
    }

    // ========================================================================
    // Pessimistic Read Lock (FOR SHARE)
    // ========================================================================

    /**
     * ID로 Bc 단건 조회 (FOR SHARE)
     *
     * @param id Bc ID
     * @return Bc Domain (Optional)
     * @throws PessimisticLockException Lock 획득 실패 시
     * @throws LockTimeoutException Lock 타임아웃 시
     */
    @Override
    public Optional<Bc> findByIdForShare({Bc}Id id) {
        return lockRepository.findByIdForShare(id.getValue())
            .map({bc}JpaEntityMapper::toDomain);
    }

    /**
     * Criteria로 Bc 목록 조회 (FOR SHARE)
     *
     * @param criteria 검색 조건
     * @return Bc Domain 목록
     * @throws PessimisticLockException Lock 획득 실패 시
     * @throws LockTimeoutException Lock 타임아웃 시
     */
    @Override
    public List<Bc> findByCriteriaForShare({Bc}SearchCriteria criteria) {
        List<{Bc}JpaEntity> entities = lockRepository.findByCriteriaForShare(criteria);
        return entities.stream()
            .map({bc}JpaEntityMapper::toDomain)
            .toList();
    }

    // ========================================================================
    // Optimistic Lock (@Version)
    // ========================================================================

    /**
     * ID로 Bc 단건 조회 (Optimistic Lock)
     *
     * <p>조회 시 Lock을 걸지 않으며, 업데이트 시 OptimisticLockException 발생 가능</p>
     *
     * @param id Bc ID
     * @return Bc Domain (Optional)
     */
    @Override
    public Optional<Bc> findByIdWithOptimisticLock({Bc}Id id) {
        return lockRepository.findByIdWithOptimisticLock(id.getValue())
            .map({bc}JpaEntityMapper::toDomain);
    }

    /**
     * Criteria로 Bc 목록 조회 (Optimistic Lock)
     *
     * <p>조회 시 Lock을 걸지 않으며, 업데이트 시 OptimisticLockException 발생 가능</p>
     *
     * @param criteria 검색 조건
     * @return Bc Domain 목록
     */
    @Override
    public List<Bc> findByCriteriaWithOptimisticLock({Bc}SearchCriteria criteria) {
        List<{Bc}JpaEntity> entities = lockRepository.findByCriteriaWithOptimisticLock(criteria);
        return entities.stream()
            .map({bc}JpaEntityMapper::toDomain)
            .toList();
    }
}
```

---

## 4️⃣ 체크리스트

LockQueryAdapter 구현 시:

### 구조
- [ ] `@Component` 어노테이션 추가
- [ ] LockQueryPort 인터페이스 구현
- [ ] LockRepository 의존성 주입 (1:1 매핑)
- [ ] Mapper 의존성 주입 (BcJpaEntityMapper)
- [ ] **필드 2개만** (LockRepository + Mapper)

### 6개 메서드 (통일된 네이밍)
- [ ] `findByIdForUpdate` - Pessimistic Write Lock (FOR UPDATE)
- [ ] `findByCriteriaForUpdate` - Pessimistic Write Lock (FOR UPDATE)
- [ ] `findByIdForShare` - Pessimistic Read Lock (FOR SHARE)
- [ ] `findByCriteriaForShare` - Pessimistic Read Lock (FOR SHARE)
- [ ] `findByIdWithOptimisticLock` - Optimistic Lock (@Version)
- [ ] `findByCriteriaWithOptimisticLock` - Optimistic Lock (@Version)

### 예외 처리
- [ ] JavaDoc에 `@throws` 명시 (PessimisticLockException, LockTimeoutException)
- [ ] try-catch로 예외 처리 안 함 (Application Layer에서 처리)

### 금지 사항
- [ ] Mapper를 통한 Entity → Domain 변환
- [ ] Domain 반환 (DTO 반환 금지)
- [ ] 비즈니스 로직 없음 (단순 위임 + 변환만)
- [ ] Command 메서드 없음 (저장/수정/삭제 금지)
- [ ] 일반 조회 메서드 없음 (QueryAdapter 사용)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
