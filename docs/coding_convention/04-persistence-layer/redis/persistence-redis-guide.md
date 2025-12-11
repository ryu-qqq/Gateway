# Persistence Layer — **Redis (Lettuce + Redisson)**

> 이 문서는 `persistence-layer` 중 **Redis** 파트에 대한 **요약 가이드**
>
> **Lettuce (캐싱)** + **Redisson (분산락)** 이원화 전략을 기반으로 합니다.

---

## 1) 라이브러리 전략

### Lettuce vs Redisson 역할 분리

| 라이브러리 | 용도 | 이유 |
|-----------|------|------|
| **Lettuce** | 캐싱, 세션, 단순 K-V | Spring Boot 기본, 가벼움, 빠름 |
| **Redisson** | 분산락, 분산 자료구조 | Pub/Sub 기반, Watchdog, 검증됨 |

```
┌─────────────────────────────────────────────────────┐
│                    Redis Server                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│   [Lettuce]                    [Redisson]           │
│   Spring Boot 기본              별도 추가            │
│                                                     │
│   • @Cacheable                 • RLock (분산락)     │
│   • RedisTemplate              • RSemaphore         │
│   • Spring Session             • RCountDownLatch    │
│   • 단순 K-V 저장              • RAtomicLong        │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 왜 분산락에 Redisson인가?

**Lettuce 분산락 문제점:**
- ❌ 스핀락 (Polling): Lock 해제까지 계속 Redis 조회 → CPU/네트워크 낭비
- ❌ 알림 없음: Lock 해제 시 대기 스레드에게 알림 불가
- ❌ Lua 스크립트 필요: 원자성 보장 위해 직접 구현
- ❌ Lock 연장 불가: 작업이 길어지면 Lock 만료 위험

**Redisson 분산락 장점:**
- ✅ Pub/Sub 기반: Lock 해제 시 이벤트로 대기 스레드 깨움
- ✅ Watchdog: 작업 중 Lock 자동 연장 (기본 30초마다)
- ✅ 다양한 Lock 타입: Fair, Read/Write, MultiLock
- ✅ 검증된 구현: 수많은 프로덕션 환경에서 사용

---

## 2) 핵심 원칙 (한눈에)

### 캐싱 (Lettuce)
* **어댑터=비즈니스 로직 금지**: Cache/Session 저장/조회만 수행
* **Cache-Aside 패턴 고정**: 조회=Cache → DB → Cache 저장, 수정=DB 저장 → Cache 무효화
* **TTL 전략 필수**: 모든 캐시는 **TTL 설정 필수**. Null 캐싱 금지
* **Key Naming Convention**: `{namespace}:{entity}:{id}` 패턴 고정
* **트랜잭션 없음**: `@Transactional` 내에서 Cache 무효화 금지

### 분산락 (Redisson)
* **Pub/Sub 기반**: 스핀락 금지, 이벤트 리스너 방식 사용
* **Watchdog 활용**: 장시간 작업 시 Lock 자동 연장
* **try-finally 필수**: Lock 획득 시 반드시 finally에서 해제
* **타임아웃 필수**: tryLock()에 waitTime, leaseTime 명시

### 금지사항

| 금지 | 이유 |
|------|------|
| Lombok | Plain Java 원칙 |
| 비즈니스 로직 (Adapter 내) | Application/Domain에서 처리 |
| `@Transactional` 내 Cache 무효화 | Rollback 시 불일치 → `@TransactionalEventListener` 사용 |
| `String key` 파라미터 | 타입 안전성 없음 → `CacheKey`, `LockKey` VO 사용 |
| Null 캐싱 | 메모리 낭비 → Cache Miss 시 Empty 반환 |
| Prod에서 `KEYS` 명령어 | Redis 블로킹 → SCAN 사용 |
| 대용량 데이터 단일 키 저장 (10MB 이상) | 네트워크 병목 |
| Lettuce로 분산락 구현 | 스핀락 문제 → Redisson 사용 |

---

## 3) 패키징 구조

```
persistence-redis/
├─ config/
│  ├─ LettuceConfig.java          # Lettuce 연결 설정
│  ├─ CacheConfig.java            # Spring Cache + RedisCacheManager
│  └─ RedissonConfig.java         # Redisson 연결 설정
│
├─ common/
│  ├─ serializer/
│  │  └─ RedisObjectSerializer.java
│  └─ key/
│     └─ RedisKeyGenerator.java
│
└─ {bc}/                          # ← Bounded Context
   ├─ adapter/
   │  ├─ {Bc}CacheAdapter.java    # ✅ 캐싱 (Lettuce)
   │  └─ {Bc}LockAdapter.java     # ✅ 분산락 (Redisson)
   └─ dto/                        # ← 선택
      └─ {Bc}CacheDto.java
```

---

## 4) 디렉터리별 상세 가이드 링크

### **config/**
* **Lettuce 설정**: [Lettuce Configuration](./config/lettuce-configuration.md) - Connection Pool 설정
* **Redisson 설정**: [Redisson Configuration](./redisson-configuration.md) - 분산락 설정

### **adapter/** (캐싱)
* **Cache Adapter**: [Cache Adapter 가이드](./adapter/cache-adapter-guide.md) | [ArchUnit](./adapter/cache-adapter-archunit.md)

### **lock/** (분산락)
* **Lock Adapter**: [Lock Adapter 가이드](./lock/lock-adapter-guide.md) | [ArchUnit](./lock/lock-adapter-archunit.md)

---

## 5) 주요 패턴

### Cache-Aside (Lettuce + CacheKey VO)
```java
// CacheKey 구현 (Domain Layer)
public record OrderCacheKey(Long orderId) implements CacheKey {
    private static final String PREFIX = "cache:order:";

    public OrderCacheKey {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + orderId;
    }
}

// 조회 (Application Layer)
public Order getOrder(Long orderId) {
    OrderCacheKey cacheKey = new OrderCacheKey(orderId);

    // 1. Cache 조회
    Optional<Order> cached = cachePort.get(cacheKey);
    if (cached.isPresent()) {
        return cached.get();
    }

    // 2. DB 조회
    Order order = queryPort.findById(OrderId.of(orderId));

    // 3. Cache 저장
    cachePort.set(cacheKey, order, Duration.ofMinutes(10));
    return order;
}

// 수정 (Cache 무효화는 @TransactionalEventListener에서 처리)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderUpdated(OrderUpdatedEvent event) {
    OrderCacheKey cacheKey = new OrderCacheKey(event.orderId());
    cachePort.evict(cacheKey);
}
```

### 분산락 (Redisson + LockKey VO)
```java
// LockKey 구현 (Domain Layer)
public record OrderLockKey(Long orderId) implements LockKey {
    private static final String PREFIX = "lock:order:";

    public OrderLockKey {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + orderId;
    }
}

// 사용 (Application Layer)
public void processWithLock(Long orderId) {
    OrderLockKey lockKey = new OrderLockKey(orderId);

    // 최대 10초 대기, 30초 유지
    boolean acquired = lockPort.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);

    if (!acquired) {
        throw new LockAcquisitionException(lockKey.value(), "Lock 획득 실패");
    }

    try {
        // 비즈니스 로직
        processOrder(orderId);
    } finally {
        lockPort.unlock(lockKey);
    }
}
```

### Key Naming (VO에서 관리)
```java
// CacheKey 구현체에서 정의
"cache:order:123"        // OrderCacheKey
"cache:user:profile:456" // UserProfileCacheKey
"cache:product:789"      // ProductCacheKey

// LockKey 구현체에서 정의
"lock:order:123"         // OrderLockKey
"lock:stock:789"         // StockLockKey
```

---

## 6) 설정 체크리스트

### Lettuce (캐싱)
- [ ] Connection Pool 설정 (`max-active: 16`)
- [ ] `timeout: 3000ms` (Command Timeout)
- [ ] `GenericJackson2JsonRedisSerializer` 사용
- [ ] `cache-null-values: false` (Null 캐싱 금지)
- [ ] Key Naming Convention 준수
- [ ] TTL 전략 수립 (용도별 차등)
- [ ] 환경 변수로 비밀번호 관리

### Redisson (분산락)
- [ ] `lockWatchdogTimeout: 30000ms` (Watchdog 주기)
- [ ] `nettyThreads` 설정 (기본: 가용 CPU * 2)
- [ ] Cluster/Sentinel 모드 설정 (Prod)
- [ ] Lock Timeout 전략 수립

### 모니터링
- [ ] Lettuce 메트릭 수집 (Latency, Hit Rate)
- [ ] Redisson Lock 메트릭 (획득 시간, 실패율)
- [ ] Alert 설정 (Latency > 100ms, Lock 실패율 > 1%)

---

## 7) 의존성 설정

### build.gradle
```groovy
dependencies {
    // Lettuce (Spring Boot 기본 포함)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Redisson (분산락용)
    implementation 'org.redisson:redisson-spring-boot-starter:3.27.0'
}
```

### application.yml
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
      timeout: 3000ms

# Redisson은 별도 설정 파일 또는 Java Config 사용
```

---

## 8) 참고 문서

* [Lettuce Documentation](https://lettuce.io/core/release/reference/)
* [Redisson Documentation](https://github.com/redisson/redisson/wiki)
* [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
* [Redis Best Practices](https://redis.io/docs/management/optimization/)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-08
**버전**: 3.0.0

### 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 3.0.0 | 2025-12-08 | CacheKey/LockKey VO 적용, 문서 링크 정리 |
| 2.0.0 | 2025-12-04 | Lettuce + Redisson 이원화 전략 |
| 1.0.0 | 2025-11-01 | 초기 작성 |
