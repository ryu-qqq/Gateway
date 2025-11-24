# Redis Cache 설정 가이드

> **목적**: Spring Cache + RedisCacheManager 설정 가이드

---

## 1️⃣ Cache 설정 전략

### Spring Cache Abstraction
**목적**: `@Cacheable`, `@CacheEvict`, `@CachePut` 어노테이션 기반 선언적 캐싱

**장점**:
- ✅ 비즈니스 로직과 캐시 로직 분리
- ✅ 다양한 캐시 구현체 교체 가능 (Redis, Caffeine, Ehcache)
- ✅ AOP 기반 자동 처리

**단점**:
- ❌ 복잡한 캐시 로직은 직접 구현 필요 (CacheAdapter 사용)
- ❌ 메서드 레벨에서만 동작

---

## 2️⃣ 기본 설정

### CacheConfig.java

```java
package com.company.adapter.out.persistence.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache 설정
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>Spring Cache Abstraction 활성화 (@EnableCaching)</li>
 *   <li>RedisCacheManager 설정</li>
 *   <li>Cache별 TTL 전략 설정</li>
 *   <li>Key/Value Serializer 설정</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * RedisCacheManager 생성
     *
     * <p>Cache별 TTL 전략을 다르게 설정할 수 있습니다.</p>
     *
     * @param connectionFactory Redis Connection Factory
     * @return RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))  // 기본 TTL: 30분
            .disableCachingNullValues()        // null 캐싱 금지
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper())
                )
            );

        // Cache별 TTL 전략
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("static-data", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()  // Transaction 연동 (선택)
            .build();
    }

    /**
     * ObjectMapper 커스터마이징
     *
     * <p>Java 8 날짜/시간 타입 지원을 위해 JavaTimeModule 추가</p>
     *
     * @return ObjectMapper
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

---

## 3️⃣ TTL 전략

### Cache별 TTL 권장값

| Cache 이름 | TTL | 용도 | 예시 |
|-----------|-----|------|------|
| `static-data` | 24시간 | 코드 테이블, 설정 | 국가 코드, 카테고리 |
| `products` | 1시간 | Reference Data | 상품 목록, 카테고리 |
| `users` | 10분 | User Data | 프로필, 설정 |
| `orders` | 30분 | Session Data | 주문 정보 |
| `rate-limit` | 1분-1시간 | Rate Limit | API 요청 제한 |
| `otp` | 5분 | Temporary | OTP, 인증 토큰 |

### TTL 설정 예시

```java
// 방법 1: Map으로 관리
Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));

// 방법 2: Enum으로 관리 (권장)
public enum CacheName {
    USERS("users", Duration.ofMinutes(10)),
    PRODUCTS("products", Duration.ofHours(1)),
    ORDERS("orders", Duration.ofMinutes(30));

    private final String name;
    private final Duration ttl;

    CacheName(String name, Duration ttl) {
        this.name = name;
        this.ttl = ttl;
    }
}
```

---

## 4️⃣ 사용 예시

### @Cacheable (조회)

```java
package com.company.application.order.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryService {

    /**
     * Order 조회 (Cache-Aside)
     *
     * @param orderId Order ID
     * @return OrderResponse
     */
    @Cacheable(value = "orders", key = "#orderId")
    public OrderResponse getOrder(Long orderId) {
        // Cache Miss → DB 조회
        return orderQueryPort.findById(orderId)
            .map(orderAssembler::toResponse)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
```

### @CacheEvict (무효화)

```java
@Service
public class OrderCommandService {

    /**
     * Order 수정 후 Cache 무효화
     *
     * @param command UpdateOrderCommand
     */
    @Transactional
    @CacheEvict(value = "orders", key = "#command.orderId")
    public void updateOrder(UpdateOrderCommand command) {
        Order order = orderQueryPort.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        order.update(command);
        orderPersistPort.persist(order);

        // Cache는 @CacheEvict에 의해 자동 무효화
    }
}
```

### @CachePut (갱신)

```java
@Service
public class OrderCommandService {

    /**
     * Order 생성 후 Cache 갱신
     *
     * @param command CreateOrderCommand
     * @return OrderResponse
     */
    @Transactional
    @CachePut(value = "orders", key = "#result.orderId")
    public OrderResponse createOrder(CreateOrderCommand command) {
        Order order = Order.create(command);
        orderPersistPort.persist(order);

        // Cache에 자동 저장 (key: orderId, value: OrderResponse)
        return orderAssembler.toResponse(order);
    }
}
```

---

## 5️⃣ 고급 설정

### 조건부 캐싱

```java
// 조건부 캐싱 (VIP 회원만)
@Cacheable(value = "orders", key = "#orderId", condition = "#isVip == true")
public OrderResponse getOrder(Long orderId, boolean isVip) {
    // ...
}

// 결과 조건부 캐싱 (null이 아닌 경우만)
@Cacheable(value = "orders", key = "#orderId", unless = "#result == null")
public OrderResponse getOrder(Long orderId) {
    // ...
}
```

### 여러 Cache 무효화

```java
// 여러 Cache 동시 무효화
@CacheEvict(value = {"orders", "users"}, key = "#orderId")
public void updateOrder(Long orderId) {
    // ...
}

// 전체 Cache 무효화
@CacheEvict(value = "orders", allEntries = true)
public void clearAllOrders() {
    // ...
}
```

---

## 6️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ null 캐싱 허용
RedisCacheConfiguration.defaultCacheConfig()
    .disableCachingNullValues(false);  // ❌ 메모리 낭비

// ❌ TTL 없이 설정
RedisCacheConfiguration.defaultCacheConfig();  // ❌ 영구 저장

// ❌ Key Prefix 없이 사용
@Cacheable(value = "cache", key = "#id")  // ❌ 충돌 가능
```

### ✅ Good Examples

```java
// ✅ null 캐싱 금지
RedisCacheConfiguration.defaultCacheConfig()
    .disableCachingNullValues();

// ✅ TTL 명시
RedisCacheConfiguration.defaultCacheConfig()
    .entryTtl(Duration.ofMinutes(30));

// ✅ Key Prefix 사용
@Cacheable(value = "orders", key = "'order::' + #orderId")
```

---

## 7️⃣ 체크리스트

Redis Cache 설정 시:
- [ ] `@EnableCaching` 어노테이션 추가
- [ ] RedisCacheManager 빈 등록
- [ ] Cache별 TTL 전략 수립
- [ ] `disableCachingNullValues()` 설정 (필수)
- [ ] Key Serializer: `StringRedisSerializer`
- [ ] Value Serializer: `GenericJackson2JsonRedisSerializer`
- [ ] ObjectMapper 커스터마이징 (JavaTimeModule)
- [ ] Cache 이름 Enum으로 관리 (권장)
- [ ] `@Cacheable`, `@CacheEvict`, `@CachePut` 적절히 사용
- [ ] Transaction 외부에서 Cache 무효화

---

## 📖 관련 문서

- **[Lettuce Configuration](lettuce-configuration.md)** - Redis 연결 설정
- **[CacheAdapter Guide](../adapter/cache-adapter-guide.md)** - 직접 캐시 구현
- **[Spring Cache Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)** - 공식 문서

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
