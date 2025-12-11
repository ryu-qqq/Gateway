# Cache Adapter 테스트 가이드

> **목적**: Lettuce 기반 Cache Adapter 테스트 작성 규칙

---

## 1. 개요

### Cache Adapter 테스트 범위

```
┌─────────────────────────────────────────────────────────────────┐
│  Cache Adapter 테스트 범위                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐           ┌──────────────┐                    │
│  │   CachePort  │ ◀──────── │   Redis      │                    │
│  │  (Adapter)   │   Lettuce │ (Container)  │                    │
│  └──────────────┘           └──────────────┘                    │
│                                                                  │
│  검증 대상:                                                      │
│  • set() - 캐시 저장 (TTL 포함)                                  │
│  • get() - 캐시 조회                                             │
│  • evict() - 캐시 무효화                                         │
│  • 직렬화/역직렬화 정확성                                        │
│  • TTL 만료 동작                                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 테스트 지원 클래스

### 2.1 CacheTestSupport (기반 클래스)

```java
/**
 * Cache Adapter 테스트 지원 추상 클래스
 *
 * <p>모든 Cache Adapter 테스트는 이 클래스를 상속받아 작성합니다.
 *
 * <p>제공 기능:
 * <ul>
 *   <li>TestContainers Redis 자동 설정</li>
 *   <li>RedisTemplate 자동 주입</li>
 *   <li>테스트 후 데이터 자동 정리</li>
 *   <li>TTL 검증 유틸리티</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class CacheTestSupport {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 Redis 데이터 정리
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    /**
     * TTL 남은 시간 조회
     *
     * @param key 캐시 키
     * @return TTL (초), 키가 없으면 -2, TTL 없으면 -1
     */
    protected Long getTtl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * TTL이 설정되었는지 검증
     *
     * @param key 캐시 키
     * @param expectedTtlSeconds 예상 TTL (초)
     * @param tolerance 허용 오차 (초)
     */
    protected void assertTtlSet(String key, long expectedTtlSeconds, long tolerance) {
        Long actualTtl = getTtl(key);
        assertThat(actualTtl)
                .as("TTL should be set for key: %s", key)
                .isGreaterThan(0)
                .isLessThanOrEqualTo(expectedTtlSeconds)
                .isGreaterThanOrEqualTo(expectedTtlSeconds - tolerance);
    }

    /**
     * 캐시가 존재하는지 검증
     *
     * @param key 캐시 키
     */
    protected void assertCacheExists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Cache should exist for key: %s", key)
                .isTrue();
    }

    /**
     * 캐시가 존재하지 않는지 검증
     *
     * @param key 캐시 키
     */
    protected void assertCacheNotExists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        assertThat(exists)
                .as("Cache should not exist for key: %s", key)
                .isFalse();
    }
}
```

---

## 3. 통합 테스트

### 3.1 기본 CRUD 테스트

```java
@DisplayName("ObjectCacheAdapter 통합 테스트")
class ObjectCacheAdapterTest extends CacheTestSupport {

    @Autowired
    private ObjectCacheAdapter cacheAdapter;

    @Nested
    @DisplayName("set()")
    class Set {

        @Test
        @DisplayName("성공 - 객체 캐시 저장")
        void success() {
            // Given
            OrderCacheKey key = new OrderCacheKey(100L);
            Order order = createTestOrder();
            Duration ttl = Duration.ofMinutes(10);

            // When
            cacheAdapter.set(key, order, ttl);

            // Then
            assertCacheExists(key.value());
            assertTtlSet(key.value(), 600, 10);  // 10분, 10초 오차 허용
        }

        @Test
        @DisplayName("성공 - 기존 값 덮어쓰기")
        void overwrite() {
            // Given
            OrderCacheKey key = new OrderCacheKey(100L);
            Order original = createTestOrder(OrderStatus.PENDING);
            Order updated = createTestOrder(OrderStatus.CONFIRMED);

            cacheAdapter.set(key, original, Duration.ofMinutes(10));

            // When
            cacheAdapter.set(key, updated, Duration.ofMinutes(10));

            // Then
            Optional<Order> result = cacheAdapter.get(key, Order.class);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("성공 - 캐시 히트")
        void cacheHit() {
            // Given
            OrderCacheKey key = new OrderCacheKey(100L);
            Order order = createTestOrder();
            cacheAdapter.set(key, order, Duration.ofMinutes(10));

            // When
            Optional<Order> result = cacheAdapter.get(key, Order.class);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(order.getId());
            assertThat(result.get().getCustomerId()).isEqualTo(order.getCustomerId());
        }

        @Test
        @DisplayName("성공 - 캐시 미스")
        void cacheMiss() {
            // Given
            OrderCacheKey key = new OrderCacheKey(999L);  // 존재하지 않는 키

            // When
            Optional<Order> result = cacheAdapter.get(key, Order.class);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공 - TTL 만료 후 캐시 미스")
        void cacheExpired() throws InterruptedException {
            // Given
            OrderCacheKey key = new OrderCacheKey(100L);
            Order order = createTestOrder();
            cacheAdapter.set(key, order, Duration.ofSeconds(1));  // 1초 TTL

            // When - 2초 대기
            Thread.sleep(2000);
            Optional<Order> result = cacheAdapter.get(key, Order.class);

            // Then
            assertThat(result).isEmpty();
            assertCacheNotExists(key.value());
        }
    }

    @Nested
    @DisplayName("evict()")
    class Evict {

        @Test
        @DisplayName("성공 - 캐시 삭제")
        void success() {
            // Given
            OrderCacheKey key = new OrderCacheKey(100L);
            Order order = createTestOrder();
            cacheAdapter.set(key, order, Duration.ofMinutes(10));
            assertCacheExists(key.value());

            // When
            cacheAdapter.evict(key);

            // Then
            assertCacheNotExists(key.value());
        }

        @Test
        @DisplayName("성공 - 존재하지 않는 키 삭제 (예외 없음)")
        void evictNonExistent() {
            // Given
            OrderCacheKey key = new OrderCacheKey(999L);

            // When & Then - 예외 없이 성공
            assertThatCode(() -> cacheAdapter.evict(key))
                    .doesNotThrowAnyException();
        }
    }

    private Order createTestOrder() {
        return createTestOrder(OrderStatus.PENDING);
    }

    private Order createTestOrder(OrderStatus status) {
        return Order.reconstitute(
                OrderId.of(100L),
                CustomerId.of(1L),
                status,
                Money.of(50000),
                List.of(),
                LocalDateTime.now(),
                null
        );
    }
}
```

### 3.2 직렬화/역직렬화 테스트

```java
@Nested
@DisplayName("직렬화/역직렬화")
class Serialization {

    @Test
    @DisplayName("성공 - 복잡한 객체 직렬화")
    void complexObject() {
        // Given
        OrderCacheKey key = new OrderCacheKey(100L);
        Order order = Order.reconstitute(
                OrderId.of(100L),
                CustomerId.of(1L),
                OrderStatus.CONFIRMED,
                Money.of(75000),
                List.of(
                        OrderLineItem.of(ProductId.of(10L), 2, Money.of(25000)),
                        OrderLineItem.of(ProductId.of(20L), 1, Money.of(25000))
                ),
                LocalDateTime.of(2024, 1, 15, 10, 30),
                LocalDateTime.of(2024, 1, 15, 11, 0)
        );

        // When
        cacheAdapter.set(key, order, Duration.ofMinutes(10));
        Optional<Order> result = cacheAdapter.get(key, Order.class);

        // Then
        assertThat(result).isPresent();
        Order cached = result.get();

        // 모든 필드 검증
        assertThat(cached.getId()).isEqualTo(order.getId());
        assertThat(cached.getCustomerId()).isEqualTo(order.getCustomerId());
        assertThat(cached.getStatus()).isEqualTo(order.getStatus());
        assertThat(cached.getTotalAmount()).isEqualTo(order.getTotalAmount());
        assertThat(cached.getItems()).hasSize(2);
        assertThat(cached.getCreatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    @DisplayName("성공 - VO 직렬화")
    void valueObject() {
        // Given
        MoneyCacheKey key = new MoneyCacheKey("test-money");
        Money money = Money.of(12345);

        // When
        cacheAdapter.set(key, money, Duration.ofMinutes(10));
        Optional<Money> result = cacheAdapter.get(key, Money.class);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(12345);
    }

    @Test
    @DisplayName("성공 - LocalDateTime 직렬화")
    void localDateTime() {
        // Given
        TimestampCacheKey key = new TimestampCacheKey("test-time");
        LocalDateTime timestamp = LocalDateTime.of(2024, 12, 25, 10, 30, 45);

        // When
        cacheAdapter.set(key, timestamp, Duration.ofMinutes(10));
        Optional<LocalDateTime> result = cacheAdapter.get(key, LocalDateTime.class);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(timestamp);
    }
}
```

### 3.3 CacheKey VO 테스트

```java
@Nested
@DisplayName("CacheKey VO 검증")
class CacheKeyValidation {

    @Test
    @DisplayName("성공 - 유효한 CacheKey 생성")
    void validCacheKey() {
        // When
        OrderCacheKey key = new OrderCacheKey(100L);

        // Then
        assertThat(key.value()).isEqualTo("cache:order:100");
    }

    @Test
    @DisplayName("실패 - null orderId")
    void nullOrderId() {
        // When & Then
        assertThatThrownBy(() -> new OrderCacheKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must be positive");
    }

    @Test
    @DisplayName("실패 - 음수 orderId")
    void negativeOrderId() {
        // When & Then
        assertThatThrownBy(() -> new OrderCacheKey(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must be positive");
    }

    @Test
    @DisplayName("성공 - 동일 ID면 동일 키")
    void sameIdSameKey() {
        // Given
        OrderCacheKey key1 = new OrderCacheKey(100L);
        OrderCacheKey key2 = new OrderCacheKey(100L);

        // Then
        assertThat(key1.value()).isEqualTo(key2.value());
        assertThat(key1).isEqualTo(key2);
    }
}
```

---

## 4. 단위 테스트 (선택)

### 4.1 Mocking 기반 테스트

단위 테스트는 Redis 연결 없이 로직만 검증할 때 사용합니다.

```java
@DisplayName("ObjectCacheAdapter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ObjectCacheAdapterUnitTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ObjectCacheAdapter cacheAdapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheAdapter = new ObjectCacheAdapter(redisTemplate);
    }

    @Test
    @DisplayName("set() - RedisTemplate.opsForValue().set() 호출 검증")
    void set_callsRedisTemplate() {
        // Given
        OrderCacheKey key = new OrderCacheKey(100L);
        Order order = mock(Order.class);
        Duration ttl = Duration.ofMinutes(10);

        // When
        cacheAdapter.set(key, order, ttl);

        // Then
        verify(valueOperations).set(
                eq(key.value()),
                eq(order),
                eq(ttl)
        );
    }

    @Test
    @DisplayName("get() - 캐시 히트 시 Optional 반환")
    void get_cacheHit_returnsOptional() {
        // Given
        OrderCacheKey key = new OrderCacheKey(100L);
        Order order = mock(Order.class);
        when(valueOperations.get(key.value())).thenReturn(order);

        // When
        Optional<Order> result = cacheAdapter.get(key, Order.class);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
    }

    @Test
    @DisplayName("get() - 캐시 미스 시 Optional.empty() 반환")
    void get_cacheMiss_returnsEmpty() {
        // Given
        OrderCacheKey key = new OrderCacheKey(999L);
        when(valueOperations.get(key.value())).thenReturn(null);

        // When
        Optional<Order> result = cacheAdapter.get(key, Order.class);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evict() - RedisTemplate.delete() 호출 검증")
    void evict_callsDelete() {
        // Given
        OrderCacheKey key = new OrderCacheKey(100L);

        // When
        cacheAdapter.evict(key);

        // Then
        verify(redisTemplate).delete(key.value());
    }
}
```

---

## 5. 체크리스트

### 통합 테스트 작성

- [ ] `CacheTestSupport` 상속
- [ ] TestContainers Redis 설정
- [ ] CacheKey VO 사용
- [ ] set/get/evict 기본 동작 검증
- [ ] TTL 설정 및 만료 검증
- [ ] 직렬화/역직렬화 검증
- [ ] 캐시 히트/미스 검증

### CacheKey VO 테스트

- [ ] 유효한 키 생성
- [ ] null/음수 값 검증
- [ ] key prefix 형식 검증
- [ ] 동등성 테스트

### 금지 사항

- [ ] String 키 직접 사용하지 않음
- [ ] Embedded Redis 사용하지 않음
- [ ] TTL 없는 캐시 저장하지 않음

---

## 6. 참고 문서

- [Redis 테스트 가이드](./01_redis-testing-guide.md)
- [Lock Adapter 테스트](./03_lock-adapter-test.md)
- [Cache Adapter 가이드](../adapter/cache-adapter-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 1.0.0
