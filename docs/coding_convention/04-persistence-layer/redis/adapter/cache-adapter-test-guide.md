# CacheAdapter 테스트 가이드

> **목적**: CacheAdapter의 단위 테스트 전략 (Mockito 기반)

---

## 1️⃣ 테스트 전략

### 테스트 대상
CacheAdapter는 **RedisTemplate 호출**만 검증합니다:

```
✅ 테스트 항목:
1. cache() 호출 시 RedisTemplate.opsForValue().set() 검증
2. get() 호출 시 RedisTemplate.opsForValue().get() 검증
3. evict() 호출 시 RedisTemplate.delete() 검증
4. TTL 설정 검증
5. Key 생성 검증
```

### 테스트 범위
- ✅ `@ExtendWith(MockitoExtension.class)` (단위 테스트)
- ✅ Mock을 사용한 의존성 격리
- ✅ 빠른 실행 (밀리초 단위)
- ❌ 실제 Redis 사용 금지 (통합 테스트로 분리)
- ❌ `@DataRedisTest` 사용 금지

---

## 2️⃣ 기본 템플릿

```java
package com.ryuqq.adapter.out.persistence.redis.{bc}.adapter;

import com.ryuqq.domain.{bc}.{Bc};
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {Bc} Cache Adapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("cache")
@Tag("persistence-layer")
@DisplayName("{Bc} Cache Adapter 단위 테스트")
class {Bc}CacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private {Bc}CacheAdapter cacheAdapter;

    @Test
    @DisplayName("cache() 호출 시 RedisTemplate을 올바르게 호출해야 한다")
    void cache_ShouldCallRedisTemplate() {
        // Given
        Long {bc}Id = 1L;
        {Bc} {bc} = mock({Bc}.class);
        String expectedKey = "cache::{bc}s::" + {bc}Id;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.cache({bc}Id, {bc});

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
            eq(expectedKey),
            eq({bc}),
            eq(Duration.ofMinutes(30))
        );
    }

    @Test
    @DisplayName("get() 호출 시 RedisTemplate을 올바르게 호출해야 한다")
    void get_ShouldCallRedisTemplate() {
        // Given
        Long {bc}Id = 1L;
        {Bc} cached{Bc} = mock({Bc}.class);
        String expectedKey = "cache::{bc}s::" + {bc}Id;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(cached{Bc});

        // When
        Optional<{Bc}> result = cacheAdapter.get({bc}Id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cached{Bc});

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("get() 호출 시 Cache Miss면 빈 Optional을 반환해야 한다")
    void get_WhenCacheMiss_ShouldReturnEmptyOptional() {
        // Given
        Long {bc}Id = 999L;
        String expectedKey = "cache::{bc}s::" + {bc}Id;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        Optional<{Bc}> result = cacheAdapter.get({bc}Id);

        // Then
        assertThat(result).isEmpty();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("evict() 호출 시 RedisTemplate.delete()를 올바르게 호출해야 한다")
    void evict_ShouldCallRedisTemplateDelete() {
        // Given
        Long {bc}Id = 1L;
        String expectedKey = "cache::{bc}s::" + {bc}Id;

        // When
        cacheAdapter.evict({bc}Id);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("cache() 호출 시 올바른 순서로 실행되어야 한다")
    void cache_ShouldExecuteInCorrectOrder() {
        // Given
        Long {bc}Id = 1L;
        {Bc} {bc} = mock({Bc}.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.cache({bc}Id, {bc});

        // Then - 실행 순서 검증
        InOrder inOrder = inOrder(redisTemplate, valueOperations);
        inOrder.verify(redisTemplate).opsForValue();
        inOrder.verify(valueOperations).set(
            anyString(),
            eq({bc}),
            any(Duration.class)
        );
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("cache")
@Tag("persistence-layer")
@DisplayName("Order Cache Adapter 단위 테스트")
class OrderCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OrderCacheAdapter cacheAdapter;

    @Test
    @DisplayName("cache() 호출 시 RedisTemplate을 올바르게 호출해야 한다")
    void cache_ShouldCallRedisTemplate() {
        // Given
        Long orderId = 100L;
        Order order = mock(Order.class);
        String expectedKey = "cache::orders::100";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.cache(orderId, order);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
            eq(expectedKey),
            eq(order),
            eq(Duration.ofMinutes(30))
        );
    }

    @Test
    @DisplayName("get() 호출 시 Cache Hit이면 Order를 반환해야 한다")
    void get_WhenCacheHit_ShouldReturnOrder() {
        // Given
        Long orderId = 100L;
        Order cachedOrder = mock(Order.class);
        String expectedKey = "cache::orders::100";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(cachedOrder);

        // When
        Optional<Order> result = cacheAdapter.get(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cachedOrder);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("get() 호출 시 Cache Miss면 빈 Optional을 반환해야 한다")
    void get_WhenCacheMiss_ShouldReturnEmptyOptional() {
        // Given
        Long orderId = 999L;
        String expectedKey = "cache::orders::999";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        Optional<Order> result = cacheAdapter.get(orderId);

        // Then
        assertThat(result).isEmpty();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("evict() 호출 시 RedisTemplate.delete()를 올바르게 호출해야 한다")
    void evict_ShouldCallRedisTemplateDelete() {
        // Given
        Long orderId = 100L;
        String expectedKey = "cache::orders::100";

        // When
        cacheAdapter.evict(orderId);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("cache() 호출 시 올바른 Key를 생성해야 한다")
    void cache_ShouldGenerateCorrectKey() {
        // Given
        Long orderId = 123L;
        Order order = mock(Order.class);
        String expectedKey = "cache::orders::123";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.cache(orderId, order);

        // Then
        verify(valueOperations).set(
            eq(expectedKey),
            any(),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("cache() 호출 시 TTL을 설정해야 한다")
    void cache_ShouldSetTTL() {
        // Given
        Long orderId = 100L;
        Order order = mock(Order.class);
        Duration expectedTTL = Duration.ofMinutes(30);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheAdapter.cache(orderId, order);

        // Then
        verify(valueOperations).set(
            anyString(),
            eq(order),
            eq(expectedTTL)
        );
    }
}
```

---

## 4️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ 실제 Redis 사용
@DataRedisTest
class OrderCacheAdapterTest {
    // 단위 테스트는 Mockito 사용!
}

// ❌ Spring Context 로딩
@SpringBootTest
class OrderCacheAdapterTest {
    // Spring Context 로딩 불필요!
}

// ❌ 비즈니스 로직 테스트
@Test
void cache_ShouldValidateOrder() {
    Order order = Order.create(...);  // 비즈니스 로직은 Domain Test로!
    cacheAdapter.cache(1L, order);
}

// ❌ DB 연동 테스트
@Test
void cache_ShouldSyncWithDatabase() {
    // DB 연동은 통합 테스트로!
}
```

### ✅ Good Examples

```java
// ✅ Mockito 단위 테스트
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("cache")
@Tag("persistence-layer")
class OrderCacheAdapterTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private OrderCacheAdapter adapter;
}

// ✅ Mock 사용
@Test
void cache_ShouldCallRedisTemplate() {
    Order order = mock(Order.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    // ...
}

// ✅ Key 생성 검증
@Test
void cache_ShouldGenerateCorrectKey() {
    String expectedKey = "cache::orders::123";
    verify(valueOperations).set(eq(expectedKey), any(), any());
}

// ✅ TTL 검증
@Test
void cache_ShouldSetTTL() {
    Duration expectedTTL = Duration.ofMinutes(30);
    verify(valueOperations).set(any(), any(), eq(expectedTTL));
}
```

---

## 5️⃣ 체크리스트

CacheAdapter 테스트 작성 시:
- [ ] **테스트 클래스 태그 추가** (필수)
  - [ ] `@Tag("unit")` - 단위 테스트 표시
  - [ ] `@Tag("cache")` - Cache Adapter 테스트 표시
  - [ ] `@Tag("persistence-layer")` - Persistence Layer 표시
- [ ] `@ExtendWith(MockitoExtension.class)` 사용
- [ ] `@Mock` 어노테이션으로 의존성 Mock 생성
- [ ] `@InjectMocks` 어노테이션으로 테스트 대상 주입
- [ ] cache() 호출 검증
- [ ] get() 호출 검증 (Cache Hit/Miss)
- [ ] evict() 호출 검증
- [ ] Key 생성 검증
- [ ] TTL 설정 검증
- [ ] RedisTemplate 호출 검증
- [ ] ValueOperations 호출 검증
- [ ] 실행 순서 검증 (InOrder)
- [ ] Optional 반환 검증
- [ ] 실제 Redis 사용 금지
- [ ] `@DataRedisTest` 사용 금지

---

## 📖 관련 문서

- **[CacheAdapter Guide](cache-adapter-guide.md)** - CacheAdapter 구현 가이드
- **[CacheAdapter ArchUnit](cache-adapter-archunit.md)** - ArchUnit 자동 검증 규칙
- **[Lettuce Configuration](../config/lettuce-configuration.md)** - Redis 설정 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
