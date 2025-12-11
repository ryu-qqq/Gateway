# Lettuce Configuration 가이드

> **목적**: Spring Boot + Lettuce 기반 Redis 연결 설정 가이드

---

## 1) Lettuce란?

### 개요
**Lettuce**는 Spring Boot 2.0+에서 **기본 Redis 클라이언트**입니다.

| 특징 | 설명 |
|------|------|
| **비동기/논블로킹** | Netty 기반, Reactive 스트림 지원 |
| **Thread-safe** | 단일 연결로 여러 스레드 공유 가능 |
| **Connection Pool** | 선택적 (기본: 단일 연결 공유) |
| **가벼움** | 캐싱, 세션 등 단순 작업에 최적화 |

### 언제 Lettuce를 사용하나?
- ✅ 캐싱 (`@Cacheable`, `@CacheEvict`)
- ✅ 세션 관리 (Spring Session)
- ✅ 단순 K-V 저장/조회
- ✅ Pub/Sub (단순 메시지)
- ❌ 분산락 → **Redisson 사용**
- ❌ 분산 자료구조 → **Redisson 사용**

---

## 2) YAML 기반 설정 (권장)

### 환경별 설정 파일

#### redis-local.yml
```yaml
# ============================================================
# Redis Local 환경 설정
# ============================================================
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: (로컬은 비밀번호 없음)

      # Connection Pool 설정
      lettuce:
        pool:
          max-active: 8        # 최대 활성 연결 수
          max-idle: 8          # 최대 유휴 연결 수
          min-idle: 2          # 최소 유휴 연결 수
          max-wait: 1000ms     # 연결 대기 최대 시간
        shutdown-timeout: 100ms

      # Command Timeout
      timeout: 2000ms

      # Client Name (모니터링용)
      client-name: app-local
```

#### redis-prod.yml
```yaml
# ============================================================
# Redis Production 환경 설정
# ============================================================
# ⚠️ 모든 민감 정보는 환경 변수로 관리
# ============================================================
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}

      # Connection Pool 설정 (Production)
      lettuce:
        pool:
          max-active: 16       # CPU cores * 2 권장
          max-idle: 8
          min-idle: 4
          max-wait: 2000ms
          time-between-eviction-runs: 30000ms  # 유휴 연결 검사 주기
        shutdown-timeout: 200ms

      # Command Timeout
      timeout: 3000ms

      # SSL 설정 (AWS ElastiCache 등)
      ssl:
        enabled: true

      # Client Name (모니터링용)
      client-name: ${APP_NAME:app}-${HOSTNAME:unknown}

      # Cluster 모드 (선택)
      # cluster:
      #   nodes:
      #     - ${REDIS_NODE_1}:6379
      #     - ${REDIS_NODE_2}:6379
      #     - ${REDIS_NODE_3}:6379
      #   max-redirects: 3

      # Sentinel 모드 (선택)
      # sentinel:
      #   master: mymaster
      #   nodes:
      #     - ${SENTINEL_1}:26379
      #     - ${SENTINEL_2}:26379
      #     - ${SENTINEL_3}:26379
```

---

## 3) Connection Pool 설정 가이드

### Pool Size 권장값

| 환경 | max-active | max-idle | min-idle | 근거 |
|------|-----------|----------|----------|------|
| **Local** | 8 | 8 | 2 | 개발 환경, 리소스 절약 |
| **Dev/QA** | 12 | 8 | 4 | 테스트 부하 고려 |
| **Prod** | 16-32 | 8-16 | 4-8 | CPU cores * 2 ~ 4 |

### Pool Size 계산 공식
```
max-active = (CPU cores) * 2 + (예상 동시 요청 수 / 10)
```

예시: 4 Core CPU, 동시 요청 100개
```
max-active = 4 * 2 + 100/10 = 18 → 반올림 → 16 또는 20
```

### Timeout 설정

| 설정 | 권장값 | 설명 |
|------|--------|------|
| **timeout** | 2000-3000ms | Redis 명령어 실행 타임아웃 |
| **max-wait** | 1000-2000ms | Pool에서 연결 획득 대기 시간 |
| **shutdown-timeout** | 100-200ms | 애플리케이션 종료 시 정리 시간 |

---

## 4) Java Config (선택)

YAML로 부족한 고급 설정이 필요할 때만 사용합니다.

### LettuceConfig.java

```java
package com.ryuqq.adapter.out.persistence.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Lettuce Redis 연결 설정
 *
 * <p><strong>책임:</strong></p>
 * <ul>
 *   <li>Redis 연결 설정</li>
 *   <li>Connection Pool 설정</li>
 *   <li>RedisTemplate Serializer 설정</li>
 * </ul>
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

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    /**
     * ClientResources 설정 (Netty 이벤트 루프 공유)
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build();
    }

    /**
     * Connection Pool 설정
     */
    @Bean
    public GenericObjectPoolConfig<Object> poolConfig() {
        GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setMinIdle(4);
        config.setMaxWait(Duration.ofMillis(2000));
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        return config;
    }

    /**
     * Lettuce ClientOptions 설정
     */
    private ClientOptions clientOptions() {
        return ClientOptions.builder()
            .socketOptions(SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(3000))
                .keepAlive(true)
                .build())
            .timeoutOptions(TimeoutOptions.enabled(timeout))
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build();
    }

    /**
     * RedisConnectionFactory 생성
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            ClientResources clientResources,
            GenericObjectPoolConfig<Object> poolConfig) {

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        if (!password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .clientResources(clientResources)
            .clientOptions(clientOptions())
            .poolConfig(poolConfig)
            .commandTimeout(timeout)
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * RedisTemplate 설정
     *
     * <p>Key: String, Value: JSON (GenericJackson2Json)</p>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Serializer (JSON)
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 5) Cluster/Sentinel 설정

### Redis Cluster (Prod 권장)

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-node-1:6379
          - redis-node-2:6379
          - redis-node-3:6379
        max-redirects: 3
      lettuce:
        cluster:
          refresh:
            adaptive: true
            period: 30000ms
```

### Redis Sentinel (HA)

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - sentinel-1:26379
          - sentinel-2:26379
          - sentinel-3:26379
      password: ${REDIS_PASSWORD}
```

---

## 6) 모니터링 설정

### Micrometer 메트릭 활성화

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus

# Lettuce 메트릭 활성화
spring:
  data:
    redis:
      lettuce:
        pool:
          # Pool 메트릭 수집 활성화
          enabled: true
```

### 주요 메트릭

| 메트릭 | 설명 | 알림 기준 |
|--------|------|----------|
| `lettuce.command.completion` | 명령어 실행 시간 | p99 > 100ms |
| `lettuce.command.firstresponse` | 첫 응답 시간 | p99 > 50ms |
| `lettuce.pool.active` | 활성 연결 수 | > max-active * 0.8 |
| `lettuce.pool.idle` | 유휴 연결 수 | < min-idle |

---

## 7) Do / Don't

### ❌ Bad Examples

```yaml
# ❌ Pool 없이 사용 (고부하 시 문제)
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # lettuce.pool 설정 없음

# ❌ 타임아웃 없음 (무한 대기)
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # timeout 설정 없음

# ❌ Prod에서 하드코딩된 비밀번호
spring:
  data:
    redis:
      password: mypassword123  # ❌
```

### ✅ Good Examples

```yaml
# ✅ Pool 설정 포함
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 2000ms

# ✅ Cluster 모드 (Prod)
spring:
  data:
    redis:
      cluster:
        nodes: ${REDIS_CLUSTER_NODES}
        max-redirects: 3
```

---

## 8) 체크리스트

Lettuce 설정 시:
- [ ] **Connection Pool 설정** (`max-active`, `max-idle`, `min-idle`)
- [ ] **Timeout 설정** (`timeout`, `max-wait`)
- [ ] **환경 변수 사용** (host, port, password)
- [ ] **Serializer 설정** (StringRedisSerializer, GenericJackson2JsonRedisSerializer)
- [ ] **Cluster/Sentinel 설정** (Prod 환경)
- [ ] **SSL 활성화** (AWS ElastiCache 등)
- [ ] **메트릭 수집 활성화** (Micrometer)
- [ ] **Client Name 설정** (모니터링용)

---

## 📖 관련 문서

- **[Cache Configuration](cache-configuration.md)** - Spring Cache 설정
- **[Cache Adapter Guide](../adapter/cache-adapter-guide.md)** - CacheAdapter 구현
- **[Lettuce Documentation](https://lettuce.io/core/release/reference/)** - 공식 문서

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
