# QueryAdapter 가이드

> **목적**: QueryDSL 기반 조회를 위한 CQRS Query Adapter 구현 가이드

---

## 1️⃣ QueryAdapter란?

### 역할
**Query 요청 → QueryDslRepository → Entity → Mapper → Domain 반환**

CQRS의 Query(읽기) 담당으로, 조회 요청을 QueryDslRepository에 위임하고 Mapper를 통해 Domain으로 변환하여 반환합니다.

### 책임
- ✅ 단건 조회 (findById)
- ✅ 존재 여부 확인 (existsById)
- ✅ 목록 조회 (findByCriteria)
- ✅ 카운트 조회 (countByCriteria)
- ✅ QueryDslRepository 호출
- ✅ Mapper를 통한 Entity → Domain 변환
- ❌ **비즈니스 로직 금지**
- ❌ **저장/수정/삭제 금지** (CommandAdapter로 분리)
- ❌ **JPAQueryFactory 직접 사용 금지** (QueryDslRepository에서 처리)
- ❌ **특수 조회 금지** (Lock, Bulk 등은 별도 Adapter로)

### 핵심 원칙
```
Application Layer (UseCase)
  └─ Query Port 호출
      └─ QueryAdapter
            └─ QueryDslRepository
                  └─ JPAQueryFactory
                        └─ DB 조회
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: 조회만 담당
- **Query**: 조회만 (findById, existsById, findByCriteria, countByCriteria)
- **Command**: 저장/수정/삭제는 CommandAdapter로
- **특수 조회**: Lock, Bulk 등은 별도 Adapter로 (OrderLockQueryAdapter 등)

### 원칙 2: QueryDslRepository 위임 + Mapper 변환
```java
// ✅ QueryDslRepository 호출 → Mapper 변환 → Domain 반환
public List<Bc> findByCriteria(BcSearchCriteria criteria) {
    List<BcJpaEntity> entities = queryDslRepository.findByCriteria(criteria);
    return entities.stream()
        .map(bcJpaEntityMapper::toDomain)
        .toList();
}

// ❌ JPAQueryFactory 직접 사용 금지
public List<Bc> findByCriteria(BcSearchCriteria criteria) {
    return queryFactory.selectFrom(qBc)...  // 금지!
}

// ❌ DTO 반환 금지 (Domain 반환)
public List<BcDto> findByCriteria(BcSearchCriteria criteria) {  // 금지!
}
```

### 원칙 3: Type-Safe Value Objects
```java
// ✅ Value Object 사용 (타입 안전성)
public Optional<Bc> findById(BcId id) {
    return queryDslRepository.findById(id.getValue())
        .map(bcJpaEntityMapper::toDomain);
}

// ❌ 원시 타입 사용 금지
public Optional<Bc> findById(Long id) {  // 금지!
}
```

### 원칙 4: Criteria 패턴
- **단건 조회**: Value Object (BcId)
- **목록 조회**: Criteria DTO (BcSearchCriteria)

---

## 3️⃣ 템플릿 코드

### 기본 템플릿
```java
@Component
public class {Bc}QueryAdapter implements {Bc}QueryPort {

    private final {Bc}QueryDslRepository queryDslRepository;
    private final {Bc}JpaEntityMapper {bc}JpaEntityMapper;

    public {Bc}QueryAdapter(
        {Bc}QueryDslRepository queryDslRepository,
        {Bc}JpaEntityMapper {bc}JpaEntityMapper
    ) {
        this.queryDslRepository = queryDslRepository;
        this.{bc}JpaEntityMapper = {bc}JpaEntityMapper;
    }

    /**
     * ID로 Bc 단건 조회
     *
     * @param id Bc ID
     * @return Bc Domain (Optional)
     */
    @Override
    public Optional<Bc> findById({Bc}Id id) {
        return queryDslRepository.findById(id.getValue())
            .map({bc}JpaEntityMapper::toDomain);
    }

    /**
     * ID로 Bc 존재 여부 확인
     *
     * @param id Bc ID
     * @return 존재 여부
     */
    @Override
    public boolean existsById({Bc}Id id) {
        return findById(id).isPresent();
    }

    /**
     * 검색 조건으로 Bc 목록 조회
     *
     * @param criteria 검색 조건
     * @return Bc Domain 목록
     */
    @Override
    public List<Bc> findByCriteria({Bc}SearchCriteria criteria) {
        List<{Bc}JpaEntity> entities = queryDslRepository.findByCriteria(criteria);
        return entities.stream()
            .map({bc}JpaEntityMapper::toDomain)
            .toList();
    }

    /**
     * 검색 조건으로 Bc 개수 조회
     *
     * @param criteria 검색 조건
     * @return Bc 개수
     */
    @Override
    public long countByCriteria({Bc}SearchCriteria criteria) {
        return queryDslRepository.countByCriteria(criteria);
    }
}
```
---

## 4️⃣ 체크리스트

QueryAdapter 구현 시:
- [ ] `@Component` 어노테이션 추가
- [ ] Port 인터페이스 구현 (BcQueryPort)
- [ ] QueryDslRepository 의존성 주입
- [ ] Mapper 의존성 주입 (BcJpaEntityMapper)
- [ ] findById() 메서드 구현 (Value Object 파라미터, Domain 반환)
- [ ] existsById() 메서드 구현 (findById 재사용)
- [ ] findByCriteria() 메서드 구현 (Criteria DTO 파라미터, Domain List 반환)
- [ ] countByCriteria() 메서드 구현 (Criteria DTO 파라미터)
- [ ] Mapper를 통한 Entity → Domain 변환
- [ ] JPAQueryFactory 직접 사용 금지 (QueryDslRepository 위임)
- [ ] 비즈니스 로직 없음 (단순 위임 + 변환만)
- [ ] Command 메서드 없음 (저장/수정/삭제는 CommandAdapter로)
- [ ] Domain 반환 (DTO 반환 금지)
- [ ] 특수 조회 (Lock, Bulk 등)는 별도 Adapter로 분리

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
