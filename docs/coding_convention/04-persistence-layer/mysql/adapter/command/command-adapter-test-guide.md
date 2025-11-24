# CommandAdapter 테스트 가이드

> **목적**: CommandAdapter의 단위 테스트 전략 (Mockito 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
CommandAdapter는 **Mapper와 Repository 호출**만 검증합니다:

```
✅ 테스트 항목:
1. Mapper.toEntity() 호출 검증
2. JpaRepository.save() 호출 검증
3. ID 반환 검증
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
import com.ryuqq.adapter.out.persistence.{bc}.repository.{Bc}JpaRepository;
import com.ryuqq.domain.{bc}.{Bc};
import com.ryuqq.domain.{bc}.{Bc}Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {Bc} Command Adapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("command")
@DisplayName("{Bc} Command Adapter 단위 테스트")
class {Bc}CommandAdapterTest {

    @Mock
    private {Bc}JpaRepository {bc}JpaRepository;

    @Mock
    private {Bc}JpaEntityMapper {bc}JpaEntityMapper;

    @InjectMocks
    private {Bc}CommandAdapter commandAdapter;

    @Test
    @DisplayName("persist() 호출 시 Mapper와 Repository를 올바르게 호출해야 한다")
    void persist_ShouldCallMapperAndRepository() {
        // Given
        {Bc} {bc} = mock({Bc}.class);
        {Bc}JpaEntity entity = mock({Bc}JpaEntity.class);
        {Bc}JpaEntity savedEntity = mock({Bc}JpaEntity.class);

        when({bc}JpaEntityMapper.toEntity({bc})).thenReturn(entity);
        when({bc}JpaRepository.save(entity)).thenReturn(savedEntity);
        when(savedEntity.getId()).thenReturn(1L);

        // When
        {Bc}Id result = commandAdapter.persist({bc});

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo(1L);

        verify({bc}JpaEntityMapper).toEntity({bc});
        verify({bc}JpaRepository).save(entity);
    }

    @Test
    @DisplayName("persist() 호출 시 올바른 순서로 실행되어야 한다")
    void persist_ShouldExecuteInCorrectOrder() {
        // Given
        {Bc} {bc} = mock({Bc}.class);
        {Bc}JpaEntity entity = mock({Bc}JpaEntity.class);
        {Bc}JpaEntity savedEntity = mock({Bc}JpaEntity.class);

        when({bc}JpaEntityMapper.toEntity({bc})).thenReturn(entity);
        when({bc}JpaRepository.save(entity)).thenReturn(savedEntity);
        when(savedEntity.getId()).thenReturn(1L);

        // When
        commandAdapter.persist({bc});

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder({bc}JpaEntityMapper, {bc}JpaRepository);
        inOrder.verify({bc}JpaEntityMapper).toEntity({bc});
        inOrder.verify({bc}JpaRepository).save(entity);
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("command")
@DisplayName("Order Command Adapter 단위 테스트")
class OrderCommandAdapterTest {

    @Mock
    private OrderJpaRepository orderJpaRepository;

    @Mock
    private OrderJpaEntityMapper orderJpaEntityMapper;

    @InjectMocks
    private OrderCommandAdapter commandAdapter;

    @Test
    @DisplayName("persist() 호출 시 Mapper와 Repository를 올바르게 호출해야 한다")
    void persist_ShouldCallMapperAndRepository() {
        // Given
        Order order = mock(Order.class);
        OrderJpaEntity entity = mock(OrderJpaEntity.class);
        OrderJpaEntity savedEntity = mock(OrderJpaEntity.class);

        when(orderJpaEntityMapper.toEntity(order)).thenReturn(entity);
        when(orderJpaRepository.save(entity)).thenReturn(savedEntity);
        when(savedEntity.getId()).thenReturn(100L);

        // When
        OrderId result = commandAdapter.persist(order);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo(100L);

        verify(orderJpaEntityMapper).toEntity(order);
        verify(orderJpaRepository).save(entity);
    }

    @Test
    @DisplayName("persist() 호출 시 올바른 순서로 실행되어야 한다")
    void persist_ShouldExecuteInCorrectOrder() {
        // Given
        Order order = mock(Order.class);
        OrderJpaEntity entity = mock(OrderJpaEntity.class);
        OrderJpaEntity savedEntity = mock(OrderJpaEntity.class);

        when(orderJpaEntityMapper.toEntity(order)).thenReturn(entity);
        when(orderJpaRepository.save(entity)).thenReturn(savedEntity);
        when(savedEntity.getId()).thenReturn(100L);

        // When
        commandAdapter.persist(order);

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder(orderJpaEntityMapper, orderJpaRepository);
        inOrder.verify(orderJpaEntityMapper).toEntity(order);
        inOrder.verify(orderJpaRepository).save(entity);
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ @DataJpaTest 사용 (Repository 테스트용)
@DataJpaTest
class OrderCommandAdapterTest {
    // 단위 테스트는 Mockito 사용!
}

// ❌ 실제 DB 의존성
@SpringBootTest
class OrderCommandAdapterTest {
    // Spring Context 로딩 불필요!
}

// ❌ 실제 객체 사용
@Test
void persist_WithRealObjects() {
    Order order = Order.create(...);  // 실제 객체 생성
    commandAdapter.persist(order);    // Mock 사용해야 함!
}

// ❌ 비즈니스 로직 테스트
@Test
void persist_WithBusinessLogic() {
    order.confirm();  // 비즈니스 로직은 Domain Test로!
}
```

### ✅ Good Examples

```java
// ✅ Mockito 단위 테스트
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("command")
@Tag("persistence-layer")
class OrderCommandAdapterTest {
    @Mock private OrderJpaRepository repository;
    @Mock private OrderJpaEntityMapper mapper;
    @InjectMocks private OrderCommandAdapter adapter;
}

// ✅ Mock 사용
@Test
void persist_ShouldCallMapperAndRepository() {
    Order order = mock(Order.class);
    OrderJpaEntity entity = mock(OrderJpaEntity.class);
    // ...
}

// ✅ 실행 순서 검증
@Test
void persist_ShouldExecuteInCorrectOrder() {
    InOrder inOrder = inOrder(mapper, repository);
    inOrder.verify(mapper).toEntity(order);
    inOrder.verify(repository).save(entity);
}
```

---

## 5️⃣ 체크리스트

CommandAdapter 테스트 작성 시:
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` 어노테이션으로 의존성 Mock 생성
- [ ] `@InjectMocks` 어노테이션으로 테스트 대상 주입
- [ ] `@Tag("unit")`, `@Tag("command")`,`@Tag("persistence-layer")` 필수
- [ ] Mapper.toEntity() 호출 검증
- [ ] JpaRepository.save() 호출 검증
- [ ] ID 반환 검증
- [ ] 실행 순서 검증 (InOrder)
- [ ] 실제 DB 사용 금지
- [ ] `@DataJpaTest` 사용 금지

---

## 📖 관련 문서

- **[CommandAdapter Guide](command-adapter-guide.md)** - CommandAdapter 구현 가이드
- **[CommandAdapter ArchUnit](command-adapter-archunit.md)** - ArchUnit 자동 검증 규칙
- **[Repository Test Guide](../repository/jpa-repository-test-guide.md)** - Repository 테스트 가이드 (@DataJpaTest)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 2.0.0
