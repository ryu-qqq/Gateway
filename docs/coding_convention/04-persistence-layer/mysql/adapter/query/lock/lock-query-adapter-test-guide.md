# LockQueryAdapter 테스트 가이드

> **목적**: LockQueryAdapter Mockito 기반 단위 테스트 가이드

---

## 1️⃣ 테스트 전략

### 테스트 대상
- ✅ LockRepository 호출 검증
- ✅ Mapper.toDomain() 호출 검증
- ✅ Domain 반환 검증
- ✅ Stream map + toList() 변환 검증

### 테스트 범위
- ✅ **단위 테스트**: Mockito 기반, 의존성 Mock
- ❌ Repository 구현 테스트 제외 (Repository 테스트에서 처리)
- ❌ 통합 테스트 제외 (별도 Integration Test)

---

## 2️⃣ 테스트 구조

### 기본 템플릿
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BcLockQueryAdapter 단위 테스트")
class BcLockQueryAdapterTest {

    @Mock
    private BcLockRepository lockRepository;

    @Mock
    private BcJpaEntityMapper bcJpaEntityMapper;

    @InjectMocks
    private BcLockQueryAdapter adapter;

    // 테스트 케이스들...
}
```

---

## 3️⃣ 테스트 케이스 (6개 메서드)

### 3.1 비관락 - 단건 조회

#### 성공 케이스
```java
@Test
@DisplayName("findByIdWithPessimisticLock() 호출 시 Entity를 Domain으로 변환하여 반환해야 한다")
void findByIdWithPessimisticLock_WhenEntityExists_ShouldReturnDomain() {
    // Given
    BcId id = BcId.of(1L);
    BcJpaEntity entity = mock(BcJpaEntity.class);
    Bc domain = mock(Bc.class);

    when(lockRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(entity));
    when(bcJpaEntityMapper.toDomain(entity)).thenReturn(domain);

    // When
    Optional<Bc> result = adapter.findByIdWithPessimisticLock(id);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(domain);

    verify(lockRepository).findByIdWithPessimisticLock(1L);
    verify(bcJpaEntityMapper).toDomain(entity);
}
```

#### 실패 케이스 (Entity 없음)
```java
@Test
@DisplayName("findByIdWithPessimisticLock() 호출 시 Entity가 없으면 Empty를 반환해야 한다")
void findByIdWithPessimisticLock_WhenEntityNotFound_ShouldReturnEmpty() {
    // Given
    BcId id = BcId.of(999L);

    when(lockRepository.findByIdWithPessimisticLock(999L)).thenReturn(Optional.empty());

    // When
    Optional<Bc> result = adapter.findByIdWithPessimisticLock(id);

    // Then
    assertThat(result).isEmpty();

    verify(lockRepository).findByIdWithPessimisticLock(999L);
    verify(bcJpaEntityMapper, never()).toDomain(any());
}
```

### 3.2 비관락 - 리스트 조회

```java
@Test
@DisplayName("findByCriteriaWithPessimisticLock() 호출 시 Entity 목록을 Domain 목록으로 변환하여 반환해야 한다")
void findByCriteriaWithPessimisticLock_WhenEntitiesExist_ShouldReturnDomainList() {
    // Given
    BcSearchCriteria criteria = new BcSearchCriteria("ACTIVE");
    BcJpaEntity entity1 = mock(BcJpaEntity.class);
    BcJpaEntity entity2 = mock(BcJpaEntity.class);
    Bc domain1 = mock(Bc.class);
    Bc domain2 = mock(Bc.class);

    when(lockRepository.findByCriteriaWithPessimisticLock(criteria))
        .thenReturn(List.of(entity1, entity2));
    when(bcJpaEntityMapper.toDomain(entity1)).thenReturn(domain1);
    when(bcJpaEntityMapper.toDomain(entity2)).thenReturn(domain2);

    // When
    List<Bc> result = adapter.findByCriteriaWithPessimisticLock(criteria);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(domain1, domain2);

    verify(lockRepository).findByCriteriaWithPessimisticLock(criteria);
    verify(bcJpaEntityMapper, times(2)).toDomain(any());
}
```

### 3.3 낙관락 - 단건 조회

```java
@Test
@DisplayName("findByIdWithOptimisticLock() 호출 시 Entity를 Domain으로 변환하여 반환해야 한다")
void findByIdWithOptimisticLock_WhenEntityExists_ShouldReturnDomain() {
    // Given
    BcId id = BcId.of(1L);
    BcJpaEntity entity = mock(BcJpaEntity.class);
    Bc domain = mock(Bc.class);

    when(lockRepository.findByIdWithOptimisticLock(1L)).thenReturn(Optional.of(entity));
    when(bcJpaEntityMapper.toDomain(entity)).thenReturn(domain);

    // When
    Optional<Bc> result = adapter.findByIdWithOptimisticLock(id);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(domain);

    verify(lockRepository).findByIdWithOptimisticLock(1L);
    verify(bcJpaEntityMapper).toDomain(entity);
}
```

### 3.4 낙관락 - 리스트 조회

```java
@Test
@DisplayName("findByCriteriaWithOptimisticLock() 호출 시 Entity 목록을 Domain 목록으로 변환하여 반환해야 한다")
void findByCriteriaWithOptimisticLock_WhenEntitiesExist_ShouldReturnDomainList() {
    // Given
    BcSearchCriteria criteria = new BcSearchCriteria("PENDING");
    BcJpaEntity entity1 = mock(BcJpaEntity.class);
    Bc domain1 = mock(Bc.class);

    when(lockRepository.findByCriteriaWithOptimisticLock(criteria))
        .thenReturn(List.of(entity1));
    when(bcJpaEntityMapper.toDomain(entity1)).thenReturn(domain1);

    // When
    List<Bc> result = adapter.findByCriteriaWithOptimisticLock(criteria);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result).containsExactly(domain1);

    verify(lockRepository).findByCriteriaWithOptimisticLock(criteria);
    verify(bcJpaEntityMapper).toDomain(entity1);
}
```

### 3.5 For Update - 단건 조회

```java
@Test
@DisplayName("findByIdForUpdate() 호출 시 Entity를 Domain으로 변환하여 반환해야 한다")
void findByIdForUpdate_WhenEntityExists_ShouldReturnDomain() {
    // Given
    BcId id = BcId.of(1L);
    BcJpaEntity entity = mock(BcJpaEntity.class);
    Bc domain = mock(Bc.class);

    when(lockRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));
    when(bcJpaEntityMapper.toDomain(entity)).thenReturn(domain);

    // When
    Optional<Bc> result = adapter.findByIdForUpdate(id);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(domain);

    verify(lockRepository).findByIdForUpdate(1L);
    verify(bcJpaEntityMapper).toDomain(entity);
}
```

### 3.6 For Update - 리스트 조회

```java
@Test
@DisplayName("findByCriteriaForUpdate() 호출 시 Entity 목록을 Domain 목록으로 변환하여 반환해야 한다")
void findByCriteriaForUpdate_WhenEntitiesExist_ShouldReturnDomainList() {
    // Given
    BcSearchCriteria criteria = new BcSearchCriteria("COMPLETED");
    BcJpaEntity entity1 = mock(BcJpaEntity.class);
    BcJpaEntity entity2 = mock(BcJpaEntity.class);
    Bc domain1 = mock(Bc.class);
    Bc domain2 = mock(Bc.class);

    when(lockRepository.findByCriteriaForUpdate(criteria))
        .thenReturn(List.of(entity1, entity2));
    when(bcJpaEntityMapper.toDomain(entity1)).thenReturn(domain1);
    when(bcJpaEntityMapper.toDomain(entity2)).thenReturn(domain2);

    // When
    List<Bc> result = adapter.findByCriteriaForUpdate(criteria);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactly(domain1, domain2);

    verify(lockRepository).findByCriteriaForUpdate(criteria);
    verify(bcJpaEntityMapper, times(2)).toDomain(any());
}
```

---

## 4️⃣ 검증 항목

### 필수 검증
✅ **테스트 항목**:
1. LockRepository.findByIdWithPessimisticLock() 호출 검증
2. LockRepository.findByCriteriaWithPessimisticLock() 호출 검증
3. LockRepository.findByIdWithOptimisticLock() 호출 검증
4. LockRepository.findByCriteriaWithOptimisticLock() 호출 검증
5. LockRepository.findByIdForUpdate() 호출 검증
6. LockRepository.findByCriteriaForUpdate() 호출 검증
7. Mapper.toDomain() 호출 검증
8. Stream map + toList() 변환 검증
9. Domain 반환 검증
10. Entity 없을 때 Optional.empty() 반환 검증

---

## 5️⃣ 체크리스트

LockQueryAdapter 테스트 작성 시:
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] LockRepository Mock 생성
- [ ] Mapper Mock 생성
- [ ] `@InjectMocks`로 Adapter 생성
- [ ] 6개 메서드별 성공/실패 케이스 작성
- [ ] LockRepository 호출 검증 (`verify()`)
- [ ] Mapper 호출 검증 (`verify()`)
- [ ] Domain 반환 검증 (`assertThat()`)
- [ ] Optional.empty() 케이스 검증
- [ ] List 크기 및 순서 검증

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
