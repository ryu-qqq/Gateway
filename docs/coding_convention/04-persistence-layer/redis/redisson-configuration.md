# Redisson Configuration Guide

## 1. 개요

Redisson은 분산락, 분산 자료구조를 위한 Redis 클라이언트입니다.

### 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Pub/Sub 기반 Lock** | 스핀락 대신 이벤트 리스너 방식 |
| **Watchdog 지원** | TTL 자동 연장 메커니즘 |
| **Thread-safe** | 모든 연산이 스레드 안전 |

### Lettuce vs Redisson 역할 분리

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis Client 전략                         │
├─────────────────────────────────────────────────────────────┤
│  Lettuce                      │  Redisson                    │
│  ─────────────────────────────│──────────────────────────────│
│  • 캐싱 (@Cacheable)          │  • 분산락 (RLock)            │
│  • 세션 관리                   │  • 분산 자료구조             │
│  • 단순 K-V 저장/조회          │  • 분산 객체                 │
│  • Spring Data Redis 통합     │  • 고급 동기화 기능          │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 설정 구조

### 2.1 YAML 설정

```yaml
# redis.yml - Redisson 설정 섹션

redisson:
  # Thread Pool Settings
  threads: 4                    # 일반 스레드 풀 크기
  netty-threads: 4              # Netty 스레드 풀 크기

  # Single Server Configuration
  single-server-config:
    connection-pool-size: 16    # 커넥션 풀 최대 크기
    connection-minimum-idle-size: 4  # 최소 유휴 커넥션
    idle-connection-timeout: 10000   # 유휴 커넥션 타임아웃 (ms)
    connect-timeout: 3000       # 연결 타임아웃 (ms)
    timeout: 3000               # 명령어 타임아웃 (ms)
    retry-attempts: 3           # 재시도 횟수
    retry-interval: 1500        # 재시도 간격 (ms)
```

### 2.2 환경별 설정 권장값

| 환경 | connection-pool-size | connection-minimum-idle-size | threads |
|------|---------------------|------------------------------|---------|
| Local | 8 | 2 | 2 |
| Dev | 12 | 4 | 4 |
| Prod | 16-32 | 8 | 8 |

---

## 3. Configuration 클래스

### 3.1 RedissonConfig

```java
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${redisson.single-server-config.connection-pool-size:16}")
    private int connectionPoolSize;

    @Value("${redisson.single-server-config.connection-minimum-idle-size:4}")
    private int connectionMinimumIdleSize;

    @Value("${redisson.single-server-config.idle-connection-timeout:10000}")
    private int idleConnectionTimeout;

    @Value("${redisson.single-server-config.connect-timeout:3000}")
    private int connectTimeout;

    @Value("${redisson.single-server-config.timeout:3000}")
    private int timeout;

    @Value("${redisson.single-server-config.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redisson.single-server-config.retry-interval:1500}")
    private int retryInterval;

    @Value("${redisson.threads:4}")
    private int threads;

    @Value("${redisson.netty-threads:4}")
    private int nettyThreads;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = String.format("redis://%s:%d", host, port);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(isPasswordEmpty() ? null : password)
                .setDatabase(database)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval);

        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);

        return Redisson.create(config);
    }

    private boolean isPasswordEmpty() {
        return password == null || password.isEmpty() || "null".equals(password);
    }
}
```

### 3.2 필수 어노테이션 및 규칙

| 항목 | 필수 값 |
|------|--------|
| `@Configuration` | 클래스 레벨 |
| `destroyMethod` | `"shutdown"` |
| Bean 이름 | `redissonClient` |

---

## 4. Watchdog 메커니즘

### 4.1 Watchdog란?

Redisson의 Watchdog은 Lock TTL을 자동으로 연장해주는 메커니즘입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                     Watchdog 동작                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  tryLock(waitTime, -1, unit)  ← leaseTime 미지정            │
│          │                                                   │
│          ▼                                                   │
│  ┌─────────────────┐                                        │
│  │ Watchdog 활성화  │                                        │
│  └────────┬────────┘                                        │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────────────────────────────────┐            │
│  │ 30초(기본) 주기로 TTL 자동 연장              │            │
│  │ → Lock 보유 중인 동안 무한 연장              │            │
│  └─────────────────────────────────────────────┘            │
│           │                                                  │
│           ▼                                                  │
│       unlock()                                               │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐                                        │
│  │ Watchdog 종료    │                                        │
│  └─────────────────┘                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 leaseTime 지정 시

```java
// leaseTime 지정 → Watchdog 비활성화
lockPort.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
// waitTime: 10초 대기
// leaseTime: 30초 후 자동 해제 (Watchdog 없음)
```

### 4.3 권장 사용 패턴

| 작업 유형 | waitTime | leaseTime | Watchdog |
|----------|----------|-----------|----------|
| 빠른 작업 | 5초 | 10초 | OFF |
| 일반 작업 | 10초 | 30초 | OFF |
| 무거운 작업 | 30초 | 60초 | OFF |
| 불확정 작업 | 10초 | -1 | ON |

---

## 5. Pub/Sub vs Spin Lock

### 5.1 왜 Redisson인가?

```
┌─────────────────────────────────────────────────────────────┐
│            Lettuce Spin Lock (비권장)                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   while (!acquired) {                                        │
│       acquired = SETNX(key, value);                         │
│       Thread.sleep(100);  // CPU 낭비, 네트워크 낭비         │
│   }                                                          │
│                                                              │
│   문제점:                                                    │
│   • CPU 리소스 낭비 (polling)                                │
│   • Redis 네트워크 트래픽 증가                               │
│   • Lock 획득 지연 (sleep 간격 의존)                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│            Redisson Pub/Sub Lock (권장)                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   SUBSCRIBE lock_channel                                     │
│   // 이벤트 대기 (블로킹 없음)                               │
│   ON_MESSAGE → Lock 획득 시도                                │
│                                                              │
│   장점:                                                      │
│   • CPU 리소스 효율적 (이벤트 기반)                          │
│   • 네트워크 트래픽 최소화                                   │
│   • 즉시 Lock 획득 가능                                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Cluster Mode 설정

### 6.1 Production Cluster Configuration

```java
// @Bean(destroyMethod = "shutdown")
// @Profile("prod-cluster")
// public RedissonClient redissonClientCluster(
//         @Value("${redisson.cluster-servers-config.node-addresses}") List<String> nodes,
//         @Value("${redisson.cluster-servers-config.password:}") String clusterPassword) {
//
//     Config config = new Config();
//
//     config.useClusterServers()
//         .addNodeAddress(nodes.toArray(new String[0]))
//         .setPassword(clusterPassword.isEmpty() ? null : clusterPassword)
//         .setScanInterval(2000)
//         .setMasterConnectionPoolSize(connectionPoolSize)
//         .setSlaveConnectionPoolSize(connectionPoolSize);
//
//     return Redisson.create(config);
// }
```

### 6.2 Cluster YAML 설정

```yaml
redisson:
  cluster-servers-config:
    node-addresses:
      - redis://node1:6379
      - redis://node2:6379
      - redis://node3:6379
    password: ${REDIS_CLUSTER_PASSWORD}
    scan-interval: 2000
    master-connection-pool-size: 16
    slave-connection-pool-size: 16
```

---

## 7. 금지 사항

### 7.1 Configuration에서 금지

| 금지 항목 | 이유 |
|----------|------|
| `@Transactional` | Redis는 Spring Transaction과 무관 |
| 비즈니스 로직 | 인프라 설정만 포함 |
| Lettuce 의존성 혼합 | Redisson은 독립적으로 사용 |

### 7.2 사용 시 금지

| 금지 항목 | 대안 |
|----------|------|
| `redissonClient.getKeys().delete()` | Lock만 사용 |
| `redissonClient.getMap()` | Lettuce RedisTemplate 사용 |
| Lock 내 DB Transaction | Lock → Transaction 분리 |

---

## 8. 연관 문서

- [Lock Adapter Guide](./lock-adapter-guide.md)
- [Lock Adapter ArchUnit](./lock-adapter-archunit.md)
- [Distributed Lock Guide](./distributed-lock-guide.md)
- [Lettuce Configuration](./lettuce-configuration.md)
