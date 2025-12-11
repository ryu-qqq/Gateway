# MySQL HikariCP 설정 가이드

> **목적**: Spring Boot 3.5.x + MySQL 8.0+ HikariCP Connection Pool 표준 설정

---

## 1️⃣ 핵심 원칙

### HikariCP를 사용하는 이유

**Spring Boot 기본 커넥션 풀**:
- ✅ 별도 의존성 불필요 (Spring Boot 기본 포함)
- ✅ 세계에서 가장 빠른 커넥션 풀 (Zero-Overhead)
- ✅ Dead Connection 자동 감지 및 제거
- ✅ 경량 (130KB JAR)

**성능 비교**:
- HikariCP: 1,000,000 ops/sec
- Tomcat JDBC: 700,000 ops/sec
- C3P0: 300,000 ops/sec

---

## 2️⃣ 필수 설정 5가지

### 1. Pool Size

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 최대 커넥션 수
      minimum-idle: 10       # 최소 유휴 커넥션
```

**HikariCP 공식 권장**:
```
connections = ((core_count * 2) + effective_spindle_count)
```

**환경별 권장 범위**:
| 환경 | Pool Size | 이유 |
|------|-----------|------|
| **Local** | 5-10 | 단일 개발자 |
| **Dev** | 10-20 | 팀 개발 |
| **Prod** | 20-50 | CPU 기반 조정 |

**주의사항**:
- ❌ `maximum-pool-size: 100+` → DB 부하, Context Switching 증가
- ✅ DB `max_connections` > (App 인스턴스 수 * pool-size)

```sql
-- MySQL max_connections 확인
SHOW VARIABLES LIKE 'max_connections';
-- 권장: 3개 인스턴스 * 20 = 60 < 151 (MySQL 기본값)
```

---

### 2. Connection Timeout

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000  # 30초 (밀리초)
```

**의미**: 커넥션 풀에서 커넥션을 얻기 위한 최대 대기 시간

**권장값**: 30초 (30000ms)

**기준**:
- 너무 짧으면 (10초 이하): 순간 부하에 TimeoutException
- 너무 길면 (60초 이상): 장애 시 응답 지연

---

### 3. Max Lifetime

```yaml
spring:
  datasource:
    hikari:
      max-lifetime: 1800000  # 30분 (밀리초)
```

**의미**: 커넥션이 풀에서 유지되는 최대 시간

**권장값**: DB `wait_timeout`의 70-80%

**계산 방법**:
```sql
-- MySQL wait_timeout 확인
SHOW VARIABLES LIKE 'wait_timeout';
-- 기본값: 28800초 (8시간)

-- HikariCP max-lifetime 설정
-- 8시간 * 0.7 = 5.6시간 = 20160초 = 20160000ms
```

**이유**: DB가 커넥션을 닫기 전에 먼저 종료 (Dead Connection 방지)

---

### 4. Leak Detection

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 60000  # Prod: 60초, Local: 0
```

**의미**: 커넥션 누수 감지 시간 (반환하지 않은 경우)

**환경별 설정**:
- **Prod**: 60000 (60초) - 누수 감지 활성화
- **Local/Dev**: 0 - 비활성화 (개발 편의)

**누수 예방**:
```java
// ❌ 나쁜 예 - 커넥션 반환 안 함
Connection conn = dataSource.getConnection();
// conn.close() 호출 안 함!

// ✅ 좋은 예 - try-with-resources
try (Connection conn = dataSource.getConnection()) {
    // 작업 수행
}  // 자동 close()
```

---

### 5. OSIV 비활성화

```yaml
spring:
  jpa:
    open-in-view: false  # ❌ 필수!
```

**OSIV의 문제점**:
- Transaction 밖에서 Lazy Loading → N+1 문제
- HTTP 요청 전체 기간 커넥션 점유 → 커넥션 부족
- 성능 저하의 주범

**대안 (Transaction 내 Fetch Join)**:
```java
@Service
@Transactional(readOnly = true)
public class GetOrderWithUserService {
    @Override
    public OrderResponse execute(GetOrderQuery query) {
        // ✅ Transaction 내에서 Fetch Join
        Order order = loadOrderPort.loadWithUser(query.orderId());
        return OrderResponse.of(order);
    }
}
```

---

## 3️⃣ 환경별 설정 템플릿

### Local 환경 (application-local.yml)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/spring_standards?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: root
    password: ${DB_PASSWORD}

    hikari:
      # Pool Size
      maximum-pool-size: 10
      minimum-idle: 5

      # Timeout
      connection-timeout: 20000  # 20초
      idle-timeout: 300000  # 5분
      max-lifetime: 600000  # 10분

      # Leak Detection (비활성화)
      leak-detection-threshold: 0

      # Pool Name
      pool-name: HikariPool-Local

      # MySQL 최적화
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        rewriteBatchedStatements: true

  jpa:
    hibernate:
      ddl-auto: validate  # ✅ Flyway 사용 시 validate
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
    show-sql: false  # ❌ Logback 사용

  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    com.ryuqq: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

---

### Prod 환경 (application-prod.yml)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}?useSSL=true&requireSSL=true&serverTimezone=Asia/Seoul
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

    hikari:
      # Pool Size (CPU 기반 조정)
      maximum-pool-size: 20
      minimum-idle: 10

      # Timeout
      connection-timeout: 30000  # 30초
      idle-timeout: 600000  # 10분
      max-lifetime: 1800000  # 30분

      # Leak Detection (활성화)
      leak-detection-threshold: 60000  # 60초

      # Pool Name
      pool-name: HikariPool-Prod

      # Connection Init SQL
      connection-init-sql: SELECT 1

      # MySQL 최적화
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        rewriteBatchedStatements: true
        cacheResultSetMetadata: true
        cacheServerConfiguration: true
        elideSetAutoCommits: true
        maintainTimeStats: false

  jpa:
    hibernate:
      ddl-auto: validate  # ✅ Flyway 사용
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
        jdbc:
          batch_size: 50
          fetch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
        query:
          plan_cache_max_size: 2048
          in_clause_parameter_padding: true
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

logging:
  level:
    com.ryuqq: INFO
    org.hibernate.SQL: WARN
```

---

## 4️⃣ JPA/Hibernate 최적화

### 1. DDL Auto 전략

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ Flyway 사용 시 validate 필수
```

**옵션 설명**:
| 옵션 | 동작 | 권장 환경 |
|------|------|-----------|
| `validate` | 엔티티-테이블 검증만 | ✅ **Flyway 사용 시 (권장)** |
| `update` | 스키마 자동 ALTER | ❌ 위험 (데이터 손실) |
| `create` | 시작 시 DROP + CREATE | ❌ Prod 절대 금지 |
| `create-drop` | 종료 시 DROP | ❌ 테스트 전용 |

---

### 2. Batch Processing

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          fetch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
```

**효과**:
```java
// 50개 INSERT를 1번의 네트워크 라운드트립으로 처리
for (int i = 0; i < 1000; i++) {
    Order order = Order.create(customerId, orderItems, clock);
    orderRepository.save(order);
}
// Without Batch: 1000번 네트워크 왕복
// With Batch: 20번 네트워크 왕복 (50개씩)
```

---

### 3. Query Plan Cache

```yaml
spring:
  jpa:
    properties:
      hibernate:
        query:
          plan_cache_max_size: 2048
          in_clause_parameter_padding: true
```

**Query Plan Cache**:
- JPQL → SQL 변환 결과 캐시
- 동일 쿼리 재사용 시 변환 생략

**IN Clause Parameter Padding**:
```sql
-- Without Padding
WHERE id IN (?, ?, ?)  -- 3개 (새 Plan)
WHERE id IN (?, ?, ?, ?, ?)  -- 5개 (새 Plan)

-- With Padding (2의 제곱수)
WHERE id IN (?, ?, ?, ?)  -- 4개 (2^2)
WHERE id IN (?, ?, ?, ?, ?, ?, ?, ?)  -- 8개 (2^3)
```

---

## 5️⃣ 모니터링 (Prod)

### Actuator + Prometheus

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**주요 메트릭**:
- `hikaricp.connections.active`: 활성 커넥션 수
- `hikaricp.connections.idle`: 유휴 커넥션 수
- `hikaricp.connections.pending`: 대기 중 스레드 수
- `hikaricp.connections.timeout`: 타임아웃 발생 횟수

**Alert 기준**:
- Pool 사용률 80% 초과: 경고
- Timeout 발생 (5분간 10회 이상): 긴급

---

## 6️⃣ 보안 설정

### 환경 변수 사용

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**환경 변수 설정 (Docker Compose)**:
```yaml
services:
  app:
    environment:
      DB_HOST: mysql-server
      DB_PORT: 3306
      DB_NAME: spring_standards
      DB_USERNAME: app_user
      DB_PASSWORD: ${DB_PASSWORD}  # .env 파일에서 로드
```

**주의**:
- ❌ 하드코딩 금지
- ✅ 환경 변수 또는 Secrets Manager 사용

---

## 7️⃣ 체크리스트

### 필수 설정
- [ ] `maximum-pool-size` 설정 (Local: 10, Prod: 20-50)
- [ ] `connection-timeout` 30초
- [ ] `max-lifetime` DB `wait_timeout`의 70-80%
- [ ] `leak-detection-threshold` Prod 활성화 (60초)
- [ ] `open-in-view: false` 설정
- [ ] `ddl-auto: validate` 설정 (Flyway 사용 시)
- [ ] 환경 변수로 민감 정보 관리

### 최적화 설정
- [ ] Batch Processing 활성화 (`batch_size: 50`)
- [ ] Query Plan Cache 설정 (`plan_cache_max_size: 2048`)
- [ ] MySQL 최적화 속성 (`cachePrepStmts`, `rewriteBatchedStatements`)

### 모니터링 (Prod)
- [ ] Actuator health, metrics 엔드포인트 활성화
- [ ] Prometheus 메트릭 노출
- [ ] HikariCP 메트릭 수집
- [ ] Alert 설정 (Pool 사용률 80% 초과)

---

## 8️⃣ 절대 금지 사항

- ❌ `open-in-view: true` (성능 저하)
- ❌ `ddl-auto: create` 또는 `update` (Prod)
- ❌ `maximum-pool-size: 100+` (과다 설정)
- ❌ DB 자격증명 하드코딩
- ❌ `connection-test-query` 설정 (HikariCP는 JDBC4 `isValid()` 사용)

---

## 9️⃣ 참고 문서

### HikariCP
- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)

### MySQL
- [MySQL Connector/J Configuration](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-configuration-properties.html)

### Spring Boot
- [Spring Boot Data Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.data)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/)

### 관련 문서
- [flyway-guide.md](./flyway-guide.md) - Flyway 통합 가이드
- [persistence-mysql-guide.md](../persistence-mysql-guide.md) - MySQL Persistence Layer 전체 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
