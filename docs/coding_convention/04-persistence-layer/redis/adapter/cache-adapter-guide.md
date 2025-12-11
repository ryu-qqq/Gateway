# CacheAdapter 가이드

> **목적**: Domain Aggregate를 Redis에 캐싱하는 Cache Adapter 구현 가이드

---

## 1️⃣ CacheAdapter란?

### 역할
**Domain Aggregate ↔ Redis Cache**

캐시 저장/조회/무효화만 담당하는 **단순 캐시 저장소** 역할만 수행합니다.

### 책임
- ✅ Domain Model을 Redis에 저장
- ✅ Redis에서 Domain Model 조회
- ✅ Cache 무효화 (Eviction)
- ❌ **비즈니스 로직 금지** (Domain/Application에서 처리)
- ❌ **DB 접근 금지** (QueryAdapter로 분리)

### 핵심 원칙
```
Application Layer (UseCase)
  ├─ 1. Cache 조회 (CachePort)
  │     └─ CacheAdapter.get(key)
  ├─ 2. Cache Miss → DB 조회 (QueryPort)
  │     └─ QueryAdapter.findById(id)
  └─ 3. Cache 저장 (CachePort)
        └─ CacheAdapter.set(key, value, ttl)
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: Cache-Aside 패턴 (권장)
```java
// 조회
public Order getOrder(Long orderId) {
    String key = "cache::orders::" + orderId;

    // 1. Cache 조회
    Order cached = cachePort.get(key, Order.class);
    if (cached != null) return cached;

    // 2. Cache Miss → DB 조회
    Order order = queryPort.findById(OrderId.of(orderId));

    // 3. Cache 저장
    cachePort.set(key, order, Duration.ofMinutes(30));
    return order;
}
```

### 원칙 2: Key Naming Convention
```java
// 패턴: {namespace}:{entity}:{id}
private static final String KEY_PREFIX = "cache::orders::";

private String generateKey(Long id) {
    return KEY_PREFIX + id;
}
```

### 원칙 3: TTL 필수
```java
// ❌ TTL 없이 저장 금지
redisTemplate.opsForValue().set(key, value);

// ✅ 항상 TTL 지정
redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
```

### 원칙 4: Transaction 외부에서 Cache 무효화
```java
// ❌ Transaction 내 무효화 금지
@Transactional
public void updateOrder(Order order) {
    persistPort.persist(order);
    cachePort.evict("cache::orders::" + order.getId());  // ❌ Rollback 시 문제
}

// ✅ Transaction 외부에서 무효화
@Transactional
public void updateOrder(Order order) {
    persistPort.persist(order);
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void evictCache(OrderUpdatedEvent event) {
    cachePort.evict("cache::orders::" + event.getOrderId());  // ✅
}
```

---

## 3️⃣ 템플릿 코드

### 기본 템플릿
```java
package com.ryuqq.adapter.out.persistence.redis.order.adapter;

import com.ryuqq.application.order.port.out.OrderCachePort;
import com.ryuqq.domain.order.Order;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Order Cache Adapter
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>Order Domain 객체를 Redis에 캐싱</li>
 *   <li>Cache-Aside 패턴 지원</li>
 *   <li>TTL 기반 자동 만료</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Component
public class OrderCacheAdapter implements OrderCachePort {

    private static final String KEY_PREFIX = "cache::orders::";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Order 캐시 저장
     *
     * @param orderId Order ID
     * @param order   저장할 Order (Domain)
     */
    @Override
    public void cache(Long orderId, Order order) {
        String key = generateKey(orderId);
        redisTemplate.opsForValue().set(key, order, DEFAULT_TTL);
    }

    /**
     * Order 캐시 조회
     *
     * @param orderId Order ID
     * @return Optional<Order> (Cache Hit 시 Order, Miss 시 Empty)
     */
    @Override
    public Optional<Order> get(Long orderId) {
        String key = generateKey(orderId);
        Order cached = (Order) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(cached);
    }

    /**
     * Order 캐시 무효화
     *
     * @param orderId Order ID
     */
    @Override
    public void evict(Long orderId) {
        String key = generateKey(orderId);
        redisTemplate.delete(key);
    }

    /**
     * Order 전체 캐시 무효화 (패턴 기반)
     *
     * <p>SCAN 명령어를 사용하여 안전하게 키를 삭제합니다.</p>
     * <p>⚠️ KEYS 명령어는 절대 사용 금지 (Redis 블로킹)</p>
     */
    @Override
    public void evictAll() {
        ScanOptions options = ScanOptions.scanOptions()
            .match(KEY_PREFIX + "*")
            .count(100)
            .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
    }

    private String generateKey(Long orderId) {
        return KEY_PREFIX + orderId;
    }
}
```

---

## 4️⃣ TTL 전략

### 용도별 TTL 권장값

| 캐시 타입 | TTL | 예시 |
|-----------|-----|------|
| **Static Data** | 24시간 | 코드 테이블, 설정 |
| **Reference Data** | 1시간 | 카테고리, 상품 목록 |
| **User Data** | 10-30분 | 프로필, 설정 |
| **Session** | 30분 | 로그인 세션 |
| **Rate Limit** | 1분-1시간 | API 요청 제한 |
| **Temporary** | 5분 | OTP, 인증 토큰 |

### TTL 설정 방법
```java
// 방법 1: 상수로 관리
private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(1);

// 방법 2: 메서드 파라미터로 받기
@Override
public void cache(Long id, Order order, Duration ttl) {
    redisTemplate.opsForValue().set(generateKey(id), order, ttl);
}
```

---

## 5️⃣ Do / Don't

### ❌ Bad Examples

```java
// ❌ 비즈니스 로직 포함
@Override
public void cache(Order order) {
    if (order.getStatus() == OrderStatus.PLACED) {  // ❌ 비즈니스 판단
        redisTemplate.opsForValue().set(generateKey(order.getId()), order);
    }
}

// ❌ TTL 없이 저장
@Override
public void cache(Long id, Order order) {
    redisTemplate.opsForValue().set(generateKey(id), order);  // ❌ 영구 저장
}

// ❌ DB 접근
@Override
public Optional<Order> get(Long orderId) {
    Order cached = (Order) redisTemplate.opsForValue().get(generateKey(orderId));
    if (cached == null) {
        return orderRepository.findById(orderId);  // ❌ DB 접근 금지
    }
    return Optional.of(cached);
}

// ❌ @Transactional 사용
@Transactional  // ❌
@Override
public void cache(Long id, Order order) {
    redisTemplate.opsForValue().set(generateKey(id), order, Duration.ofMinutes(30));
}
```

### ✅ Good Examples

```java
// ✅ 단순 저장/조회만
@Override
public void cache(Long id, Order order) {
    redisTemplate.opsForValue().set(generateKey(id), order, Duration.ofMinutes(30));
}

// ✅ Optional 반환
@Override
public Optional<Order> get(Long orderId) {
    Order cached = (Order) redisTemplate.opsForValue().get(generateKey(orderId));
    return Optional.ofNullable(cached);
}

// ✅ Key Naming Convention 준수
private String generateKey(Long orderId) {
    return "cache::orders::" + orderId;
}

// ✅ TTL 명시
private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
```

---

## 6️⃣ Port 인터페이스 예시

```java
package com.ryuqq.application.order.port.out;

import com.ryuqq.domain.order.Order;

import java.time.Duration;
import java.util.Optional;

/**
 * Order Cache Port (출력 포트)
 */
public interface OrderCachePort {

    /**
     * Order 캐시 저장
     */
    void cache(Long orderId, Order order);

    /**
     * Order 캐시 저장 (TTL 커스텀)
     */
    void cache(Long orderId, Order order, Duration ttl);

    /**
     * Order 캐시 조회
     */
    Optional<Order> get(Long orderId);

    /**
     * Order 캐시 무효화
     */
    void evict(Long orderId);

    /**
     * Order 전체 캐시 무효화
     */
    void evictAll();
}
```

---

## 7️⃣ 금지 명령어 (중요!)

### ❌ KEYS 명령어 절대 금지

```java
// ❌ KEYS 사용 금지 (Production 환경에서 Redis 블로킹)
Set<String> keys = redisTemplate.keys("cache::orders::*");  // ❌
keys.forEach(redisTemplate::delete);
```

**문제점**:
- Redis는 Single-Thread: KEYS 실행 중 모든 요청 블로킹
- O(N) 복잡도: 키가 많으면 수십 초 소요
- Production 장애 원인 1위

### ✅ SCAN 사용 (안전)

```java
// ✅ SCAN 사용 (페이지네이션 지원, 블로킹 없음)
@Override
public void evictAll() {
    ScanOptions options = ScanOptions.scanOptions()
        .match(KEY_PREFIX + "*")
        .count(100)  // 한 번에 100개씩
        .build();

    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            redisTemplate.delete(cursor.next());
        }
    }
}
```

**장점**:
- 페이지네이션: 한 번에 일부만 처리
- Non-blocking: 다른 요청 영향 없음
- O(1) 복잡도 (각 iteration)

---

## 8️⃣ 체크리스트

### 필수 규칙 (Zero-Tolerance)
- [ ] `@Component` 어노테이션 추가
- [ ] Port 인터페이스 구현 (CachePort)
- [ ] RedisTemplate 의존성 주입
- [ ] Key Naming Convention 준수 (`{namespace}:{entity}:{id}`)
- [ ] TTL 필수 지정
- [ ] Optional 반환 (get 메서드)
- [ ] 비즈니스 로직 없음 (단순 저장/조회만)
- [ ] DB 접근 없음 (QueryAdapter로 분리)
- [ ] `@Transactional` 절대 금지
- [ ] **KEYS 명령어 절대 금지** (Prod에서 성능 문제)

### 선택적 규칙 (상황에 따라)
- [ ] ObjectMapper 의존성 (Object 타입 캐시, JSON 직렬화 필요 시)
- [ ] evictByPattern 메서드 (패턴 기반 삭제 필요 시)
- [ ] scanKeys 메서드 (evictByPattern 구현 시 SCAN 사용)

> **참고**: 단순 String key-value 캐시의 경우 ObjectMapper, evictByPattern, scanKeys가 불필요합니다.
> Object 타입 캐시(JSON 직렬화)나 패턴 기반 삭제가 필요한 경우에만 구현하세요.

---

## 9️⃣ 참고 문서

- [Lettuce Configuration](../config/lettuce-configuration.md) - Redis 설정
- [Cache Adapter Test Guide](./cache-adapter-test-guide.md) - 테스트 가이드
- [Cache Adapter ArchUnit](./cache-adapter-archunit.md) - ArchUnit 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-09
**버전**: 1.1.0

### 변경 이력
- **v1.1.0** (2025-12-09): ObjectMapper, evictByPattern, scanKeys 규칙을 선택적으로 변경
- **v1.0.0** (2025-11-13): 최초 작성
