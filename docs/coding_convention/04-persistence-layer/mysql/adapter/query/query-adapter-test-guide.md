# QueryAdapter 테스트 가이드

> **목적**: QueryAdapter의 단위 테스트 전략 (Mockito 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
QueryAdapter는 **QueryDslRepository 호출 + Mapper 변환**만 검증합니다:

```
✅ 테스트 항목:
1. QueryDslRepository.findById() 호출 검증
2. Mapper.toDomain() 호출 검증
3. existsById() 호출 검증 (findById 재사용)
4. QueryDslRepository.findByCriteria() 호출 검증
5. Stream map + toList() 변환 검증
6. QueryDslRepository.countByCriteria() 호출 검증
7. Domain 반환 검증
```

### 테스트 범위
- ✅ `@ExtendWith(MockitoExtension.class)` (단위 테스트)
- ✅ Mock을 사용한 의존성 격리
- ✅ 빠른 실행 (밀리초 단위)
- ❌ 실제 DB 사용 금지 (Repository 테스트로 분리)
- ❌ `@DataJpaTest` 사용 금지

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.adapter.out.persistence.{bc}.adapter;

import com.ryuqq.adapter.out.persistence.{bc}.entity.{Bc}JpaEntity;
import com.ryuqq.adapter.out.persistence.{bc}.mapper.{Bc}JpaEntityMapper;
import com.ryuqq.adapter.out.persistence.{bc}.repository.{Bc}QueryDslRepository;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import com.ryuqq.domain.{bc}.{Bc}SearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {Bc} Query Adapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("query")
@Tag("persistence-layer")
@DisplayName("{Bc} Query Adapter 단위 테스트")
class {Bc}QueryAdapterTest {

    @Mock
    private {Bc}QueryDslRepository queryDslRepository;

    @Mock
    private {Bc}JpaEntityMapper {bc}JpaEntityMapper;

    @InjectMocks
    private {Bc}QueryAdapter queryAdapter;

    @Test
    @DisplayName("findById() 호출 시 Repository와 Mapper를 올바르게 호출해야 한다")
    void findById_ShouldCallRepositoryAndMapper() {
        // Given
        {Bc}Id id = {Bc}Id.of(1L);
        {Bc}JpaEntity entity = mock({Bc}JpaEntity.class);
        {Bc} domain = mock({Bc}.class);

        when(queryDslRepository.findById(1L)).thenReturn(Optional.of(entity));
        when({bc}JpaEntityMapper.toDomain(entity)).thenReturn(domain);

        // When
        Optional<{Bc}> result = queryAdapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(domain);

        verify(queryDslRepository).findById(1L);
        verify({bc}JpaEntityMapper).toDomain(entity);
    }

    @Test
    @DisplayName("findById() 호출 시 Entity가 없으면 빈 Optional을 반환해야 한다")
    void findById_WhenEntityNotFound_ShouldReturnEmptyOptional() {
        // Given
        {Bc}Id id = {Bc}Id.of(999L);

        when(queryDslRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<{Bc}> result = queryAdapter.findById(id);

        // Then
        assertThat(result).isEmpty();

        verify(queryDslRepository).findById(999L);
        verify({bc}JpaEntityMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("existsById() 호출 시 Entity가 존재하면 true를 반환해야 한다")
    void existsById_WhenEntityExists_ShouldReturnTrue() {
        // Given
        {Bc}Id id = {Bc}Id.of(1L);
        {Bc}JpaEntity entity = mock({Bc}JpaEntity.class);
        {Bc} domain = mock({Bc}.class);

        when(queryDslRepository.findById(1L)).thenReturn(Optional.of(entity));
        when({bc}JpaEntityMapper.toDomain(entity)).thenReturn(domain);

        // When
        boolean result = queryAdapter.existsById(id);

        // Then
        assertThat(result).isTrue();

        verify(queryDslRepository).findById(1L);
    }

    @Test
    @DisplayName("existsById() 호출 시 Entity가 없으면 false를 반환해야 한다")
    void existsById_WhenEntityNotFound_ShouldReturnFalse() {
        // Given
        {Bc}Id id = {Bc}Id.of(999L);

        when(queryDslRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = queryAdapter.existsById(id);

        // Then
        assertThat(result).isFalse();

        verify(queryDslRepository).findById(999L);
        verify({bc}JpaEntityMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("findByCriteria() 호출 시 Repository와 Mapper를 올바르게 호출해야 한다")
    void findByCriteria_ShouldCallRepositoryAndMapper() {
        // Given
        {Bc}SearchCriteria criteria = mock({Bc}SearchCriteria.class);
        {Bc}JpaEntity entity1 = mock({Bc}JpaEntity.class);
        {Bc}JpaEntity entity2 = mock({Bc}JpaEntity.class);
        {Bc} domain1 = mock({Bc}.class);
        {Bc} domain2 = mock({Bc}.class);

        when(queryDslRepository.findByCriteria(criteria)).thenReturn(List.of(entity1, entity2));
        when({bc}JpaEntityMapper.toDomain(entity1)).thenReturn(domain1);
        when({bc}JpaEntityMapper.toDomain(entity2)).thenReturn(domain2);

        // When
        List<{Bc}> result = queryAdapter.findByCriteria(criteria);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(domain1, domain2);

        verify(queryDslRepository).findByCriteria(criteria);
        verify({bc}JpaEntityMapper).toDomain(entity1);
        verify({bc}JpaEntityMapper).toDomain(entity2);
    }

    @Test
    @DisplayName("findByCriteria() 호출 시 빈 리스트를 반환해야 한다")
    void findByCriteria_WhenNoResults_ShouldReturnEmptyList() {
        // Given
        {Bc}SearchCriteria criteria = mock({Bc}SearchCriteria.class);

        when(queryDslRepository.findByCriteria(criteria)).thenReturn(List.of());

        // When
        List<{Bc}> result = queryAdapter.findByCriteria(criteria);

        // Then
        assertThat(result).isEmpty();

        verify(queryDslRepository).findByCriteria(criteria);
        verify({bc}JpaEntityMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("countByCriteria() 호출 시 Repository를 올바르게 호출해야 한다")
    void countByCriteria_ShouldCallRepository() {
        // Given
        {Bc}SearchCriteria criteria = mock({Bc}SearchCriteria.class);

        when(queryDslRepository.countByCriteria(criteria)).thenReturn(100L);

        // When
        long result = queryAdapter.countByCriteria(criteria);

        // Then
        assertThat(result).isEqualTo(100L);

        verify(queryDslRepository).countByCriteria(criteria);
    }

    @Test
    @DisplayName("findById() 호출 시 올바른 순서로 실행되어야 한다")
    void findById_ShouldExecuteInCorrectOrder() {
        // Given
        {Bc}Id id = {Bc}Id.of(1L);
        {Bc}JpaEntity entity = mock({Bc}JpaEntity.class);
        {Bc} domain = mock({Bc}.class);

        when(queryDslRepository.findById(1L)).thenReturn(Optional.of(entity));
        when({bc}JpaEntityMapper.toDomain(entity)).thenReturn(domain);

        // When
        queryAdapter.findById(id);

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder(queryDslRepository, {bc}JpaEntityMapper);
        inOrder.verify(queryDslRepository).findById(1L);
        inOrder.verify({bc}JpaEntityMapper).toDomain(entity);
    }

    @Test
    @DisplayName("findByCriteria() 호출 시 올바른 순서로 실행되어야 한다")
    void findByCriteria_ShouldExecuteInCorrectOrder() {
        // Given
        {Bc}SearchCriteria criteria = mock({Bc}SearchCriteria.class);
        {Bc}JpaEntity entity1 = mock({Bc}JpaEntity.class);
        {Bc} domain1 = mock({Bc}.class);

        when(queryDslRepository.findByCriteria(criteria)).thenReturn(List.of(entity1));
        when({bc}JpaEntityMapper.toDomain(entity1)).thenReturn(domain1);

        // When
        queryAdapter.findByCriteria(criteria);

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder(queryDslRepository, {bc}JpaEntityMapper);
        inOrder.verify(queryDslRepository).findByCriteria(criteria);
        inOrder.verify({bc}JpaEntityMapper).toDomain(entity1);
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("query")
@Tag("persistence-layer")
@DisplayName("Order Query Adapter 단위 테스트")
class OrderQueryAdapterTest {

    @Mock
    private OrderQueryDslRepository queryDslRepository;

    @Mock
    private OrderJpaEntityMapper orderJpaEntityMapper;

    @InjectMocks
    private OrderQueryAdapter queryAdapter;

    @Test
    @DisplayName("findById() 호출 시 Repository와 Mapper를 올바르게 호출해야 한다")
    void findById_ShouldCallRepositoryAndMapper() {
        // Given
        OrderId orderId = OrderId.of(100L);
        OrderJpaEntity entity = mock(OrderJpaEntity.class);
        Order domain = mock(Order.class);

        when(queryDslRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(orderJpaEntityMapper.toDomain(entity)).thenReturn(domain);

        // When
        Optional<Order> result = queryAdapter.findById(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(domain);

        verify(queryDslRepository).findById(100L);
        verify(orderJpaEntityMapper).toDomain(entity);
    }

    @Test
    @DisplayName("findById() 호출 시 Entity가 없으면 빈 Optional을 반환해야 한다")
    void findById_WhenEntityNotFound_ShouldReturnEmptyOptional() {
        // Given
        OrderId orderId = OrderId.of(999L);

        when(queryDslRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Order> result = queryAdapter.findById(orderId);

        // Then
        assertThat(result).isEmpty();

        verify(queryDslRepository).findById(999L);
        verify(orderJpaEntityMapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("findByCriteria() 호출 시 Repository와 Mapper를 올바르게 호출해야 한다")
    void findByCriteria_ShouldCallRepositoryAndMapper() {
        // Given
        OrderSearchCriteria criteria = mock(OrderSearchCriteria.class);
        OrderJpaEntity entity1 = mock(OrderJpaEntity.class);
        OrderJpaEntity entity2 = mock(OrderJpaEntity.class);
        Order domain1 = mock(Order.class);
        Order domain2 = mock(Order.class);

        when(queryDslRepository.findByCriteria(criteria)).thenReturn(List.of(entity1, entity2));
        when(orderJpaEntityMapper.toDomain(entity1)).thenReturn(domain1);
        when(orderJpaEntityMapper.toDomain(entity2)).thenReturn(domain2);

        // When
        List<Order> result = queryAdapter.findByCriteria(criteria);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(domain1, domain2);

        verify(queryDslRepository).findByCriteria(criteria);
        verify(orderJpaEntityMapper).toDomain(entity1);
        verify(orderJpaEntityMapper).toDomain(entity2);
    }

    @Test
    @DisplayName("countByCriteria() 호출 시 Repository를 올바르게 호출해야 한다")
    void countByCriteria_ShouldCallRepository() {
        // Given
        OrderSearchCriteria criteria = mock(OrderSearchCriteria.class);

        when(queryDslRepository.countByCriteria(criteria)).thenReturn(50L);

        // When
        long result = queryAdapter.countByCriteria(criteria);

        // Then
        assertThat(result).isEqualTo(50L);

        verify(queryDslRepository).countByCriteria(criteria);
    }

    @Test
    @DisplayName("findById() 호출 시 올바른 순서로 실행되어야 한다")
    void findById_ShouldExecuteInCorrectOrder() {
        // Given
        OrderId orderId = OrderId.of(100L);
        OrderJpaEntity entity = mock(OrderJpaEntity.class);
        Order domain = mock(Order.class);

        when(queryDslRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(orderJpaEntityMapper.toDomain(entity)).thenReturn(domain);

        // When
        queryAdapter.findById(orderId);

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder(queryDslRepository, orderJpaEntityMapper);
        inOrder.verify(queryDslRepository).findById(100L);
        inOrder.verify(orderJpaEntityMapper).toDomain(entity);
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ @DataJpaTest 사용 (Repository 테스트용)
@DataJpaTest
class OrderQueryAdapterTest {
    // 단위 테스트는 Mockito 사용!
}

// ❌ 실제 DB 의존성
@SpringBootTest
class OrderQueryAdapterTest {
    // Spring Context 로딩 불필요!
}

// ❌ 실제 객체 사용
@Test
void findById_WithRealObjects() {
    Order order = Order.create(...);  // 실제 객체 생성
    queryAdapter.findById(orderId);    // Mock 사용해야 함!
}

// ❌ 비즈니스 로직 테스트
@Test
void findById_WithBusinessLogic() {
    order.confirm();  // 비즈니스 로직은 Domain Test로!
}

// ❌ DTO 반환 검증
@Test
void findById_ShouldReturnDto() {
    OrderDto dto = queryAdapter.findById(orderId);  // Domain 반환해야 함!
}
```

### ✅ Good Examples

```java
// ✅ Mockito 단위 테스트
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("query")
@Tag("persistence-layer")
class OrderQueryAdapterTest {
    @Mock private OrderQueryDslRepository repository;
    @Mock private OrderJpaEntityMapper mapper;
    @InjectMocks private OrderQueryAdapter adapter;
}

// ✅ Mock 사용
@Test
void findById_ShouldCallRepositoryAndMapper() {
    OrderId id = OrderId.of(100L);
    OrderJpaEntity entity = mock(OrderJpaEntity.class);
    Order domain = mock(Order.class);
    // ...
}

// ✅ 실행 순서 검증
@Test
void findById_ShouldExecuteInCorrectOrder() {
    InOrder inOrder = inOrder(repository, mapper);
    inOrder.verify(repository).findById(100L);
    inOrder.verify(mapper).toDomain(entity);
}

// ✅ Domain 반환 검증
@Test
void findById_ShouldReturnDomain() {
    Optional<Order> result = queryAdapter.findById(orderId);
    assertThat(result.get()).isInstanceOf(Order.class);
}
```

---

## 5️⃣ 체크리스트

QueryAdapter 테스트 작성 시:
- [ ] **테스트 클래스 태그 추가** (필수)
  - [ ] `@Tag("unit")` - 단위 테스트 표시
  - [ ] `@Tag("query")` - Query Adapter 테스트 표시
  - [ ] `@Tag("persistence-layer")` - Persistence Layer 표시
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` 어노테이션으로 의존성 Mock 생성
- [ ] `@InjectMocks` 어노테이션으로 테스트 대상 주입
- [ ] findById() 호출 검증 (존재/비존재)
- [ ] existsById() 호출 검증 (true/false)
- [ ] findByCriteria() 호출 검증
- [ ] countByCriteria() 호출 검증
- [ ] QueryDslRepository 호출 검증
- [ ] Mapper.toDomain() 호출 검증
- [ ] Domain 반환 검증
- [ ] 실행 순서 검증 (InOrder)
- [ ] 빈 결과 케이스 테스트 (Optional.empty(), List.of())
- [ ] 실제 DB 사용 금지
- [ ] `@DataJpaTest` 사용 금지

---

## 📖 관련 문서

- **[QueryAdapter Guide](query-adapter-guide.md)** - QueryAdapter 구현 가이드
- **[QueryAdapter ArchUnit](query-adapter-archunit.md)** - ArchUnit 자동 검증 규칙
- **[Repository Test Guide](../repository/querydsl-repository-test-guide.md)** - Repository 테스트 가이드 (@DataJpaTest)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
