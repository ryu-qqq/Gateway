# Persistence Layer — **Redis (Lettuce/Cache)**

> 이 문서는 `persistence-layer` 중 **Redis(Lettuce/Cache)** 파트에 대한 **요약 가이드**
>
> **핵심 원칙**, **패키징 구조**, 그리고 각 디렉터리별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **어댑터=비즈니스 로직 금지**: Cache/Session 저장/조회만 수행. 가공·검증·정책 판단은 애플리케이션/도메인에서.
* **Cache-Aside 패턴 고정**: **조회=Cache → DB → Cache 저장**, **수정=DB 저장 → Cache 무효화**. Write-Through는 특수 상황에만.
* **TTL 전략 필수**: 모든 캐시는 **TTL 설정 필수**. Null 캐싱 금지 (`cache-null-values: false`).
* **Key Naming Convention**: `{namespace}:{entity}:{id}` 패턴 고정 (예: `cache::users::123`).
* **Serializer 통일**: `GenericJackson2JsonRedisSerializer` 사용 (타입 정보 보존).
* **Lettuce 동기 API 사용**: `RedisTemplate<String, Object>` 사용. Reactive는 WebFlux 전환 시에만.
* **트랜잭션 없음**: Redis는 트랜잭션 외부에서 사용. `@Transactional` 내에서 Cache 무효화 금지 (순서: DB 저장 → Commit → Cache 무효화).


### 금지사항

* Lombok, 비즈니스 로직, `@Transactional` 내 Cache 무효화
* Null 캐싱 (`cache-null-values: true`)
* Prod에서 `KEYS` 명령어 (SCAN 사용)
* 대용량 데이터 단일 키 저장 (10MB 이상)

---

## 2) 패키징 구조 (BC 예시)

```
persistence-redis/
├─ config/
│  ├─ RedisConfig.java
│  └─ CacheConfig.java
│
├─ common/
│  ├─ serializer/
│  │  └─ RedisObjectSerializer.java
│  └─ key/
│     └─ RedisKeyGenerator.java
│
└─ order/                         # ← 예시 BC
   ├─ adapter/
   │  └─ OrderCacheAdapter.java   # ✅ 필수 (RedisTemplate 사용)
   ├─ dto/                        # ← DTO 패키지 (선택)
   │  └─ OrderCacheDto.java       # Serialization 최적화 필요 시
   └─ repository/                 # ← Repository 패키지 (선택)
      └─ OrderRedisRepository.java  # Spring Data Redis 사용 시
```

### 선택 가이드: DTO vs Domain 직접 캐싱

| 방식 | 장점 | 단점 | 추천 시나리오 |
|------|------|------|--------------|
| **Domain 직접 캐싱** | 코드 단순, DTO 오버헤드 없음 | Domain 변경 시 캐시 호환성 문제 | 단순 Domain, 자주 변경 안 됨 |
| **Cache DTO 사용** | 직렬화 최적화, Domain 변경 격리 | DTO 변환 오버헤드, 코드 복잡 | 복잡한 Domain, 버전 관리 필요 |

### 선택 가이드: RedisTemplate vs Repository

| 방식 | 장점 | 단점 | 추천 시나리오 |
|------|------|------|--------------|
| **RedisTemplate** | 단순, TTL/Key 제어 용이 | 반복 코드 | 대부분의 경우 (권장) |
| **Spring Data Redis** | Spring Data 일관성 | 추상화 오버헤드 | JPA Repository와 통일된 API 필요 시 |

---

## 3) 디렉터리별 상세 가이드 링크

### **config/**
* **Lettuce 설정**: [Lettuce Configuration](./config/lettuce-configuration.md) - HikariCP급 설정 가이드
* **Cache 설정**: [Cache Configuration](./config/cache-configuration.md) - Spring Cache + RedisCacheManager

### **adapter/**
* **Cache Adapter**: [Cache Adapter 가이드](./adapter/cache-adapter-guide.md) | [테스트](./adapter/cache-adapter-test-guide.md) | [ArchUnit](./adapter/cache-adapter-archunit.md)

### **dto/** (선택)
* **Cache DTO**: [Cache DTO 가이드](./dto/cache-dto-guide.md) - Serialization 최적화

### **repository/** (선택)
* **Redis Repository**: [Redis Repository 가이드](./repository/redis-repository-guide.md) - Spring Data Redis 사용 시

---

## 4) 주요 패턴

### Cache-Aside (권장)
```java
// 조회
public Order getOrder(Long orderId) {
    // 1. Cache 조회
    Order cached = cachePort.get("order:" + orderId);
    if (cached != null) return cached;

    // 2. DB 조회
    Order order = queryPort.findById(OrderId.of(orderId));

    // 3. Cache 저장
    cachePort.set("order:" + orderId, order, Duration.ofMinutes(30));
    return order;
}

// 수정
@Transactional
public void updateOrder(Order order) {
    // 1. DB 저장
    persistPort.persist(order);

    // 2. Commit 후 Cache 무효화 (@TransactionalEventListener 사용)
}
```

### Key Naming
```java
// 패턴: {namespace}:{entity}:{id}
"cache::users::123"
"session::user:456"
"ratelimit::api:789::2024-01-01"
```

---

## 5) 설정 체크리스트

### 필수
- [ ] `max-active: 16` (Connection Pool)
- [ ] `timeout: 3000ms` (Command Timeout)
- [ ] `GenericJackson2JsonRedisSerializer` 사용
- [ ] `cache-null-values: false` (Null 캐싱 금지)
- [ ] Key Naming Convention 준수
- [ ] TTL 전략 수립 (용도별 차등)
- [ ] 환경 변수로 비밀번호 관리

### 최적화
- [ ] ObjectMapper 커스터마이징 (JavaTimeModule)
- [ ] 캐시별 TTL 커스터마이징
- [ ] Key Prefix 설정

### 모니터링
- [ ] Lettuce 메트릭 수집 (Latency, Hit Rate)
- [ ] Alert 설정 (Latency > 100ms)

---

## 6) 참고 문서

* [Lettuce Documentation](https://lettuce.io/core/release/reference/)
* [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
* [Redis Best Practices](https://redis.io/docs/management/optimization/)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
