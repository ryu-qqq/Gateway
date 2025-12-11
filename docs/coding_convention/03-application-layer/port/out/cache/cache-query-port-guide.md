# CacheQueryPort 가이드

> Application Layer의 도메인 특화 캐시 포트 인터페이스 가이드

## 1. 개요

### 1.1 위치 및 역할

```
application/
└── {bounded-context}/
    └── port/out/
        ├── {Entity}CommandPort.java     # 도메인 쓰기
        ├── {Entity}QueryPort.java       # 도메인 읽기
        ├── {Entity}LockQueryPort.java   # 도메인 DB 락
        └── {Entity}CacheQueryPort.java  # 도메인 캐시 ✨
```

**특징:**
- **도메인 특화**: 각 Bounded Context 내부에 위치
- **Cache-Aside 패턴**: 캐시 조회 → DB 조회 → 캐시 저장
- **도메인 언어 사용**: 키 형식 캡슐화

### 1.2 DistributedLockPort와의 차이

| 구분 | DistributedLockPort | CacheQueryPort |
|------|---------------------|----------------|
| 위치 | `common/port/out/` | `{BC}/port/out/` |
| 범위 | Cross-cutting | 도메인 특화 |
| 용도 | 동시성 제어 | 데이터 캐싱 |
| 키 타입 | `LockKey` VO | 도메인 ID (Long 등) |

## 2. 인터페이스 정의

### 2.1 도메인 특화 CacheQueryPort

```java
// application/product/port/out/ProductCacheQueryPort.java
package com.ryuqq.application.product.port.out;

import com.ryuqq.domain.product.aggregate.Product;
import java.util.List;
import java.util.Optional;

public interface ProductCacheQueryPort {

    /**
     * 상품 캐시 조회
     */
    Optional<Product> findById(Long productId);

    /**
     * 카테고리별 상품 목록 캐시 조회
     */
    List<Product> findByCategory(Long categoryId);

    /**
     * 상품 캐시 저장
     */
    void save(Product product);

    /**
     * 상품 캐시 무효화
     */
    void evict(Long productId);

    /**
     * 카테고리별 캐시 무효화
     */
    void evictByCategory(Long categoryId);
}
```

```java
// application/order/port/out/OrderCacheQueryPort.java
package com.ryuqq.application.order.port.out;

import com.ryuqq.application.order.dto.OrderSummaryDto;
import java.util.Optional;

public interface OrderCacheQueryPort {

    Optional<OrderSummaryDto> findSummaryById(Long orderId);

    void saveSummary(Long orderId, OrderSummaryDto summary);

    void evict(Long orderId);

    void evictByUserId(Long userId);
}
```

## 3. Cache-Aside 패턴 구현

### 3.1 UseCase에서 사용

```java
@Component
public class GetProductUseCase {

    private final ProductCacheQueryPort cacheQueryPort;
    private final ProductQueryPort dbQueryPort;

    public Product execute(Long productId) {
        // 1. Cache 조회
        Optional<Product> cached = cacheQueryPort.findById(productId);
        if (cached.isPresent()) {
            return cached.get();  // Cache Hit
        }

        // 2. Cache Miss → DB 조회
        Product product = dbQueryPort.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        // 3. Cache 저장
        cacheQueryPort.save(product);

        return product;
    }
}
```

### 3.2 캐시 무효화

```java
@Component
public class UpdateProductUseCase {

    private final ProductCommandPort commandPort;
    private final ProductCacheQueryPort cacheQueryPort;

    public void execute(UpdateProductCommand command) {
        // 1. DB 업데이트
        Product product = commandPort.update(command);

        // 2. 캐시 무효화
        cacheQueryPort.evict(product.getId());

        // 3. 연관 캐시 무효화 (카테고리 목록 등)
        cacheQueryPort.evictByCategory(product.getCategoryId());
    }
}
```

## 4. Adapter 구현

### 4.1 도메인 특화 CacheAdapter

```java
// adapter-out/persistence-redis/src/main/java/.../ProductCacheAdapter.java
@Component
public class ProductCacheAdapter implements ProductCacheQueryPort {

    private static final String KEY_PREFIX = "cache::product::";
    private static final String CATEGORY_KEY_PREFIX = "cache::product::category::";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Product> findById(Long productId) {
        String key = KEY_PREFIX + productId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(convertToProduct(value));
    }

    @Override
    public void save(Product product) {
        String key = KEY_PREFIX + product.getId();
        redisTemplate.opsForValue().set(key, product, DEFAULT_TTL);
    }

    @Override
    public void evict(Long productId) {
        String key = KEY_PREFIX + productId;
        redisTemplate.delete(key);
    }

    @Override
    public void evictByCategory(Long categoryId) {
        String pattern = CATEGORY_KEY_PREFIX + categoryId + "::*";
        Set<String> keys = scanKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)
            .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    private Product convertToProduct(Object value) {
        return objectMapper.convertValue(value, Product.class);
    }
}
```

## 5. 키 네이밍 컨벤션

### 5.1 형식

```
cache::{domain}::{id}
cache::{domain}::{entity}::{id}
cache::{domain}::{grouping}::{groupId}::{id}
```

### 5.2 예시

| 도메인 | 키 형식 | 예시 |
|--------|---------|------|
| 상품 | `cache::product::{productId}` | `cache::product::123` |
| 상품 카테고리 | `cache::product::category::{categoryId}` | `cache::product::category::5` |
| 주문 요약 | `cache::order::summary::{orderId}` | `cache::order::summary::789` |
| 사용자 주문 목록 | `cache::order::user::{userId}` | `cache::order::user::456` |

### 5.3 Lock vs Cache 키 비교

| 용도 | 접두어 | 예시 |
|------|--------|------|
| 분산락 | `lock:` | `lock:order:123` |
| 캐시 | `cache::` | `cache::order::123` |

## 6. 금지 사항

### 6.1 Port에서 금지

```java
// ❌ 금지: 범용 CachePort 사용 (도메인 언어 없음)
public interface CachePort<T> {
    void set(String key, T value);  // 도메인 의미 없음
    Optional<T> get(String key);
}

// ✅ 권장: 도메인 특화 CacheQueryPort
public interface ProductCacheQueryPort {
    Optional<Product> findById(Long productId);  // 도메인 언어
    void evict(Long productId);
}
```

### 6.2 Adapter에서 금지

```java
// ❌ 금지: KEYS 명령어 사용
public void evictByPattern(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);  // 블로킹! 금지!
    redisTemplate.delete(keys);
}

// ✅ 권장: SCAN 사용
public void evictByPattern(String pattern) {
    Set<String> keys = scanKeys(pattern);  // 비블로킹
    redisTemplate.delete(keys);
}

// ❌ 금지: @Transactional
@Transactional  // 금지!
public void save(Product product) {
    // ...
}
```

## 7. 범용 CachePort와의 관계

### 7.1 기존 범용 CachePort

```java
// application/common/port/out/CachePort.java (범용, 선택적)
public interface CachePort<T> {
    void set(String key, T value, Duration ttl);
    Optional<T> get(String key, Class<T> clazz);
    void evict(String key);
    void evictByPattern(String pattern);
}
```

### 7.2 관계

| 구분 | 범용 CachePort | 도메인 CacheQueryPort |
|------|---------------|----------------------|
| 위치 | `common/port/out/` | `{BC}/port/out/` |
| 용도 | 인프라 Adapter 내부 | UseCase에서 사용 |
| 키 관리 | 문자열 직접 사용 | 도메인 ID 사용 |
| 권장도 | 내부 구현용 | **권장** |

```
UseCase
   ↓ (도메인 ID)
ProductCacheQueryPort (도메인 특화)
   ↓ (내부적으로 키 생성)
CachePort<Object> (범용, 선택적)
   ↓
RedisTemplate (인프라)
```

## 8. 체크리스트

### Port 정의 시

- [ ] `{BC}/port/out/` 위치
- [ ] 도메인 언어 사용 (findById, evictByCategory 등)
- [ ] 도메인 타입 반환 (Product, OrderSummaryDto)
- [ ] String 키 파라미터 없음 (Long ID 사용)

### Adapter 구현 시

- [ ] SCAN 사용 (KEYS 금지)
- [ ] 키 형식 중앙화 (상수로 관리)
- [ ] TTL 설정
- [ ] `@Transactional` 없음

### UseCase 사용 시

- [ ] Cache-Aside 패턴 준수
- [ ] 캐시 미스 시 DB 조회
- [ ] 업데이트 시 캐시 무효화
- [ ] 연관 캐시 고려
