---
name: redis-expert
version: 3.0.0
description: |
  Redis 전문가. Lettuce(캐싱) + Redisson(분산락) 듀얼 전략.
  CacheAdapter(Cache-Aside, TTL 필수, KEYS 금지→SCAN 사용), LockAdapter(Pub/Sub, Watchdog, try-finally).
  Key Naming: {namespace}:{entity}:{id}. @Component 사용. @Transactional 금지.
author: claude-spring-standards
created: 2024-11-01
updated: 2025-12-05
tags: [project, persistence, redis, cache, distributed-lock, lettuce, redisson]
---

# Redis Expert (Redis 전문가)

## 목적 (Purpose)

Persistence Layer에서 **Redis를 활용한 캐싱 및 분산락**을 규칙에 맞게 구현합니다.
Lettuce(캐싱)와 Redisson(분산락) 듀얼 전략으로 각 라이브러리의 강점을 최대한 활용합니다.

## 활성화 조건

- `/impl persistence {feature}` 명령 실행 시 (캐시/락 필요한 경우)
- `/plan` 실행 후 캐싱 또는 분산락 설계 시
- cache, redis, lettuce, redisson, distributed lock, 캐싱 키워드 언급 시

## 산출물 (Output)

| 컴포넌트 | 파일명 패턴 | 위치 |
|----------|-------------|------|
| CacheAdapter | `{Bc}CacheAdapter.java` | `adapter-out/persistence-redis/{bc}/adapter/` |
| LockAdapter | `DistributedLockAdapter.java` | `adapter-out/persistence-redis/common/adapter/` |
| LettuceConfig | `LettuceConfig.java` | `adapter-out/persistence-redis/config/` |
| RedissonConfig | `RedissonConfig.java` | `adapter-out/persistence-redis/config/` |
| CachePort | `{Bc}CachePort.java` | `application/common/port/out/` |
| LockPort | `DistributedLockPort.java` | `application/common/port/out/` |

## 완료 기준 (Acceptance Criteria)

- [ ] Lettuce + Redisson 듀얼 전략 (캐싱 vs 분산락 분리)
- [ ] Cache-Aside 패턴 준수
- [ ] TTL 필수 (모든 캐시 키에 TTL 설정)
- [ ] Key Naming Convention 준수 (`{namespace}:{entity}:{id}`)
- [ ] KEYS 명령어 금지 (SCAN 사용)
- [ ] `@Component` 어노테이션 사용
- [ ] `@Transactional` 금지 (Adapter에서)
- [ ] try-finally 패턴 (분산락)
- [ ] Lombok 금지
- [ ] ArchUnit 테스트 통과

---

## 듀얼 전략 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                   Redis 듀얼 전략                            │
├──────────────────────────┬──────────────────────────────────┤
│       LETTUCE            │          REDISSON                │
│   (Spring Boot 기본)     │       (분산락 전문)               │
├──────────────────────────┼──────────────────────────────────┤
│  • 캐싱 (Caching)        │  • 분산락 (Distributed Lock)     │
│  • 세션 (Session)        │  • Fair Lock (선착순)            │
│  • 단순 K-V 저장         │  • Read/Write Lock              │
│  • 발행/구독 기본        │  • Multi Lock                   │
├──────────────────────────┼──────────────────────────────────┤
│  RedisTemplate 사용      │  RedissonClient 사용            │
│  Cache-Aside 패턴        │  Pub/Sub 기반 (스핀락 아님)      │
│  TTL 필수                │  Watchdog 자동 연장              │
└──────────────────────────┴──────────────────────────────────┘
```

### 라이브러리 선택 기준

| 기능 | 라이브러리 | 이유 |
|------|-----------|------|
| 캐싱 | Lettuce | Spring Boot 기본, 높은 처리량 |
| 세션 | Lettuce | Spring Session 통합 |
| 분산락 | Redisson | Pub/Sub 기반, Watchdog 지원 |
| 분산 자료구조 | Redisson | RMap, RSet, RQueue 등 |

---

## 코드 템플릿

### 1. CachePort 인터페이스 (Application Layer)

```java
package com.ryuqq.application.common.port.out;

import java.util.Optional;

/**
 * 주문 캐시 포트 (출력 포트)
 *
 * <p><strong>메서드명 규칙:</strong></p>
 * <ul>
 *   <li>cache() - 캐시 저장</li>
 *   <li>get() - 캐시 조회</li>
 *   <li>evict() - 단일 캐시 삭제</li>
 *   <li>evictAll() - 패턴 매칭 삭제</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
public interface OrderCachePort {

    /**
     * 주문 캐시 저장
     *
     * @param orderId 주문 ID
     * @param order   주문 데이터
     */
    void cache(Long orderId, OrderCacheData order);

    /**
     * 주문 캐시 조회
     *
     * @param orderId 주문 ID
     * @return 캐시된 주문 데이터 (없으면 empty)
     */
    Optional<OrderCacheData> get(Long orderId);

    /**
     * 단일 주문 캐시 삭제
     *
     * @param orderId 주문 ID
     */
    void evict(Long orderId);

    /**
     * 사용자별 주문 캐시 전체 삭제
     *
     * @param userId 사용자 ID
     */
    void evictAllByUser(Long userId);
}
```

### 2. CacheAdapter 구현 (Cache-Aside 패턴)

```java
package com.ryuqq.adapter.out.persistence.redis.order.adapter;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import com.ryuqq.application.common.port.out.OrderCachePort;
import com.ryuqq.adapter.out.persistence.redis.order.dto.OrderCacheData;

/**
 * 주문 캐시 Adapter
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>Cache-Aside 패턴 구현</li>
 *   <li>TTL 기반 캐시 관리</li>
 *   <li>SCAN 기반 안전한 키 삭제</li>
 * </ul>
 *
 * <p><strong>금지 사항:</strong></p>
 * <ul>
 *   <li>❌ 비즈니스 로직 포함</li>
 *   <li>❌ @Transactional 사용</li>
 *   <li>❌ KEYS 명령어 사용</li>
 *   <li>❌ TTL 없는 캐시 저장</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Component
public class OrderCacheAdapter implements OrderCachePort {

    private static final String KEY_PREFIX = "order:cache:";
    private static final String USER_KEY_PREFIX = "order:user:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cache(Long orderId, OrderCacheData order) {
        String key = generateKey(orderId);
        redisTemplate.opsForValue().set(key, order, DEFAULT_TTL);
    }

    @Override
    public Optional<OrderCacheData> get(Long orderId) {
        String key = generateKey(orderId);
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached instanceof OrderCacheData data) {
            return Optional.of(data);
        }
        return Optional.empty();
    }

    @Override
    public void evict(Long orderId) {
        String key = generateKey(orderId);
        redisTemplate.delete(key);
    }

    /**
     * 사용자별 캐시 삭제 (SCAN 사용 - KEYS 금지)
     *
     * <p>KEYS 명령어는 Redis를 블로킹하므로 SCAN 사용 필수</p>
     */
    @Override
    public void evictAllByUser(Long userId) {
        String pattern = USER_KEY_PREFIX + userId + ":*";

        ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)
            .build();

        Set<String> keysToDelete = redisTemplate.execute((connection) -> {
            Set<String> keys = new java.util.HashSet<>();
            var cursor = connection.scan(options);
            cursor.forEachRemaining(key -> keys.add(new String(key)));
            return keys;
        });

        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }

    private String generateKey(Long orderId) {
        return KEY_PREFIX + orderId;
    }
}
```

### 3. DistributedLockPort 인터페이스 (Application Layer)

```java
package com.ryuqq.application.common.port.out;

import java.util.concurrent.TimeUnit;

/**
 * 분산락 포트 (출력 포트)
 *
 * <p><strong>Redisson 기반 분산락 추상화</strong></p>
 *
 * <p><strong>메서드명 규칙:</strong></p>
 * <ul>
 *   <li>tryLock() - Lock 획득 시도</li>
 *   <li>unlock() - Lock 해제</li>
 *   <li>isHeldByCurrentThread() - 현재 스레드 Lock 보유 확인</li>
 *   <li>isLocked() - Lock 상태 확인</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
public interface DistributedLockPort {

    /**
     * 분산락 획득 시도
     *
     * @param key       Lock 키 (예: "lock:order:123")
     * @param waitTime  최대 대기 시간
     * @param leaseTime Lock 유지 시간
     * @param unit      시간 단위
     * @return Lock 획득 성공 여부
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 분산락 해제
     *
     * <p>현재 스레드가 Lock을 보유한 경우에만 해제</p>
     *
     * @param key Lock 키
     */
    void unlock(String key);

    /**
     * 현재 스레드 Lock 보유 여부
     *
     * @param key Lock 키
     * @return 현재 스레드가 Lock을 보유 중인지 여부
     */
    boolean isHeldByCurrentThread(String key);

    /**
     * Lock 상태 확인
     *
     * @param key Lock 키
     * @return Lock이 걸려있는지 여부
     */
    boolean isLocked(String key);
}
```

### 4. DistributedLockAdapter 구현 (Redisson)

```java
package com.ryuqq.adapter.out.persistence.redis.common.adapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.ryuqq.application.common.port.out.DistributedLockPort;
import com.ryuqq.adapter.out.persistence.redis.common.exception.LockAcquisitionException;

/**
 * Redisson 분산락 Adapter
 *
 * <p><strong>Redisson 장점 (vs Lettuce 스핀락):</strong></p>
 * <ul>
 *   <li>Pub/Sub 기반 이벤트 리스너 (스핀락 X)</li>
 *   <li>Watchdog 자동 TTL 연장</li>
 *   <li>Fair Lock, Read/Write Lock 지원</li>
 * </ul>
 *
 * <p><strong>금지 사항:</strong></p>
 * <ul>
 *   <li>❌ 비즈니스 로직 포함</li>
 *   <li>❌ Lock 내에서 DB 접근</li>
 *   <li>❌ @Transactional 사용</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@Component
public class DistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redissonClient;

    /**
     * Thread-safe Lock 인스턴스 캐시
     */
    private final ConcurrentHashMap<String, RLock> lockCache = new ConcurrentHashMap<>();

    public DistributedLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(key);

        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(
                String.format("Lock 획득 중 인터럽트: key=%s", key), e
            );
        }
    }

    /**
     * Lock 해제 (현재 스레드 보유 확인 필수)
     *
     * <p>무조건 unlock 호출 시 IllegalMonitorStateException 발생 가능</p>
     */
    @Override
    public void unlock(String key) {
        RLock lock = getLock(key);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isHeldByCurrentThread(String key) {
        return getLock(key).isHeldByCurrentThread();
    }

    @Override
    public boolean isLocked(String key) {
        return getLock(key).isLocked();
    }

    private RLock getLock(String key) {
        return lockCache.computeIfAbsent(key, redissonClient::getLock);
    }
}
```

### 5. Lettuce 설정 (LettuceConfig)

```java
package com.ryuqq.adapter.out.persistence.redis.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Lettuce 설정 (캐싱 전용)
 *
 * @author Development Team
 * @since 1.0.0
 */
@Configuration
public class LettuceConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        if (!password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(4);
        poolConfig.setMaxWait(Duration.ofMillis(3000));

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(3000))
            .poolConfig(poolConfig)
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
```

### 6. Redisson 설정 (RedissonConfig)

```java
package com.ryuqq.adapter.out.persistence.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 설정 (분산락 전용)
 *
 * @author Development Team
 * @since 1.0.0
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
            .setAddress(address)
            .setPassword(password.isEmpty() ? null : password)
            .setConnectionPoolSize(16)
            .setConnectionMinimumIdleSize(4)
            .setIdleConnectionTimeout(10000)
            .setConnectTimeout(3000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
```

### 7. YAML 설정 예시

```yaml
# persistence-redis.yml

# ===== Local 환경 =====
spring:
  data:
    redis:
      host: localhost
      port: 16379  # Docker Compose 포트
      password: ""
      lettuce:
        pool:
          max-active: 8
          max-idle: 4
          min-idle: 2
          max-wait: 3000ms
      timeout: 3000ms

---
# ===== Production 환경 =====
spring:
  config:
    activate:
      on-profile: prod
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 3000ms
      timeout: 3000ms
```

### 8. UseCase에서 분산락 사용 예시

```java
package com.ryuqq.application.stock.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ryuqq.application.common.port.out.DistributedLockPort;
import com.ryuqq.application.stock.port.in.DecreaseStockUseCase;

/**
 * 재고 차감 Service (분산락 적용)
 */
@Component
public class DecreaseStockService implements DecreaseStockUseCase {

    private static final String LOCK_KEY_PREFIX = "lock:stock:item:";
    private static final long LOCK_WAIT_TIME = 10;
    private static final long LOCK_LEASE_TIME = 30;

    private final DistributedLockPort lockPort;
    private final StockReadManager stockReadManager;
    private final StockTransactionManager stockTransactionManager;

    public DecreaseStockService(
            DistributedLockPort lockPort,
            StockReadManager stockReadManager,
            StockTransactionManager stockTransactionManager) {
        this.lockPort = lockPort;
        this.stockReadManager = stockReadManager;
        this.stockTransactionManager = stockTransactionManager;
    }

    @Override
    @Transactional
    public void execute(DecreaseStockCommand command) {
        String lockKey = LOCK_KEY_PREFIX + command.itemId();

        boolean acquired = lockPort.tryLock(
            lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS
        );

        if (!acquired) {
            throw new StockLockException("재고 Lock 획득 실패: itemId=" + command.itemId());
        }

        try {
            Stock stock = stockReadManager.getByItemId(command.itemId());
            stock.decrease(command.quantity());
            stockTransactionManager.persist(stock);

        } finally {
            lockPort.unlock(lockKey);  // try-finally 필수!
        }
    }
}
```

---

## TTL 전략 테이블

| 캐시 유형 | TTL | 예시 |
|----------|-----|------|
| **Static Data** | 24시간 | 카테고리, 코드 |
| **Reference Data** | 1시간 | 상품 정보 |
| **User Data** | 10-30분 | 장바구니, 세션 |
| **Real-time Data** | 1-5분 | 재고 수량 |
| **Lock** | 작업시간 + 버퍼 | waitTime=10s, leaseTime=30s |

---

## Key Naming Convention

### 패턴

```
{namespace}:{entity}:{id}
```

### 예시

```java
// 캐시 키
"order:cache:123"           // 주문 캐시
"product:cache:456"         // 상품 캐시
"user:session:789"          // 사용자 세션

// Lock 키
"lock:order:123"            // 주문 Lock
"lock:stock:item:456"       // 재고 Lock
"lock:scheduler:daily"      // 스케줄러 Lock
```

---

## Watchdog 메커니즘 (Redisson)

```
Lock 획득 (leaseTime 미지정 시)
    ↓
Watchdog 활성화 (기본 30초 주기)
    ↓
┌─────────────────────────────────────┐
│ while (Lock 보유 중) {              │
│   10초마다 TTL 갱신 (30초로 연장)   │
│ }                                   │
└─────────────────────────────────────┘
    ↓
unlock() 호출 → Watchdog 종료
```

**장점**: 작업이 예상보다 오래 걸려도 Lock이 만료되지 않음

---

## Lock 타입별 사용 시나리오

| Lock 타입 | 클래스 | 사용 시나리오 |
|----------|--------|--------------|
| **기본 Lock** | `RLock` | 재고 차감, 포인트 사용 |
| **Fair Lock** | `RFairLock` | 좌석 예약, 선착순 이벤트 |
| **Read/Write Lock** | `RReadWriteLock` | 캐시 갱신, 설정 변경 |
| **Multi Lock** | `getMultiLock()` | 계좌 이체, 여러 리소스 동시 수정 |
| **RedLock** | `getRedLock()` | 결제 처리 (고가용성) |

---

## Zero-Tolerance 규칙

### ✅ MANDATORY (필수)

| 규칙 | 설명 |
|------|------|
| `@Component` | Adapter 어노테이션 |
| **TTL 필수** | 모든 캐시에 TTL 설정 |
| **SCAN 사용** | 패턴 삭제 시 SCAN (KEYS 금지) |
| **try-finally** | 분산락 unlock 보장 |
| **isHeldByCurrentThread()** | unlock 전 현재 스레드 확인 |
| **Key Naming** | `{namespace}:{entity}:{id}` 패턴 |
| **생성자 주입** | Lombok 없이 Plain Java |

### ❌ PROHIBITED (금지)

| 항목 | 이유 |
|------|------|
| `KEYS` 명령어 | Redis 블로킹 (O(N)) |
| `@Transactional` in Adapter | 캐시와 DB 트랜잭션 분리 |
| TTL 없는 캐시 | 메모리 누수 위험 |
| 비즈니스 로직 in Adapter | Application Layer 책임 |
| null 캐싱 | Cache Penetration 위험 |
| Lettuce 스핀락 | Redisson Pub/Sub 사용 |
| 무조건 unlock() | isHeldByCurrentThread() 확인 필수 |
| Lombok | Plain Java 사용 |

---

## 패키지 구조

```
adapter-out/persistence-redis/
├── src/main/java/com/ryuqq/adapter/out/persistence/redis/
│   ├── config/
│   │   ├── LettuceConfig.java          # 캐싱용 Lettuce 설정
│   │   └── RedissonConfig.java         # 분산락용 Redisson 설정
│   ├── common/
│   │   ├── adapter/
│   │   │   └── DistributedLockAdapter.java
│   │   └── exception/
│   │       └── LockAcquisitionException.java
│   └── {bc}/                           # Bounded Context별
│       ├── adapter/
│       │   └── {Bc}CacheAdapter.java
│       └── dto/
│           └── {Bc}CacheData.java
└── src/main/resources/
    └── persistence-redis.yml
```

---

## 체크리스트 (Output Checklist)

### CacheAdapter

- [ ] `@Component` 어노테이션
- [ ] Port 인터페이스 구현 (`*CachePort`)
- [ ] `RedisTemplate` 의존성 주입
- [ ] 생성자 주입 (Lombok 없음)
- [ ] **TTL 설정** (모든 캐시 저장 시)
- [ ] Key Naming Convention 준수
- [ ] **SCAN 사용** (패턴 삭제 시, KEYS 금지)
- [ ] `@Transactional` 없음
- [ ] 비즈니스 로직 없음

### LockAdapter

- [ ] `@Component` 어노테이션
- [ ] Port 인터페이스 구현 (`DistributedLockPort`)
- [ ] `RedissonClient` 의존성 주입
- [ ] 생성자 주입 (Lombok 없음)
- [ ] **tryLock() 구현** (waitTime, leaseTime 필수)
- [ ] **unlock() 구현** (isHeldByCurrentThread 확인)
- [ ] **InterruptedException 처리** (Thread.currentThread().interrupt())
- [ ] Lock 인스턴스 캐싱 (ConcurrentHashMap)
- [ ] 커스텀 예외 정의 (LockAcquisitionException)
- [ ] `@Transactional` 없음
- [ ] 비즈니스 로직 없음

### Config

- [ ] LettuceConfig (Connection Pool, Serializer)
- [ ] RedissonConfig (destroyMethod="shutdown")
- [ ] YAML 환경별 분리 (local/prod)
- [ ] Timeout 설정 (2000-3000ms 권장)

### UseCase에서 Lock 사용

- [ ] **try-finally 패턴** (unlock 보장)
- [ ] Lock Key Prefix 상수 정의
- [ ] waitTime, leaseTime 상수 정의
- [ ] Lock 획득 실패 시 예외 던지기
- [ ] Lock 범위 최소화 (필요한 부분만)

---

## 테스트 체크리스트

### CacheAdapter 테스트

- [ ] cache() 후 get() 성공
- [ ] TTL 만료 후 get() empty
- [ ] evict() 후 get() empty
- [ ] evictAllByUser() SCAN 동작 확인
- [ ] 존재하지 않는 키 get() empty

### LockAdapter 테스트

- [ ] tryLock() 성공/실패
- [ ] 동시 Lock 획득 시도 (하나만 성공)
- [ ] unlock() 후 재획득 가능
- [ ] isHeldByCurrentThread() 정확성
- [ ] InterruptedException 처리

---

## 참조 문서

- **Redis 가이드**: `docs/coding_convention/04-persistence-layer/redis/persistence-redis-guide.md`
- **Cache Adapter 가이드**: `docs/coding_convention/04-persistence-layer/redis/adapter/cache-adapter-guide.md`
- **Distributed Lock 가이드**: `docs/coding_convention/04-persistence-layer/redis/lock/distributed-lock-guide.md`
- **Lock Adapter 가이드**: `docs/coding_convention/04-persistence-layer/redis/lock/lock-adapter-guide.md`
- **Lettuce 설정**: `docs/coding_convention/04-persistence-layer/redis/config/lettuce-configuration.md`
