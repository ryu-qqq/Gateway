# Integration Testing 종합 가이드

> **목적**: E2E 통합 테스트의 개념, Flyway vs @Sql 구분, 전체 실행 흐름 이해

---

## 1️⃣ Integration 테스트란?

### 정의

**Integration 테스트 = E2E (End-to-End) 테스트**

전체 레이어가 통합되어 실제 환경과 유사하게 동작하는지 검증하는 테스트입니다.

```
HTTP Request (TestRestTemplate)
    ↓
Controller (REST API Layer)
    ↓
UseCase (Application Layer)
    ↓
Repository (Persistence Layer)
    ↓
Real Database (MySQL via TestContainers)
    ↓
HTTP Response
```

### 단위 테스트 vs 통합 테스트

| 항목 | 단위 테스트 | 통합 테스트 |
|------|-------------|-------------|
| **범위** | 하나의 클래스/메서드 | 전체 레이어 통합 |
| **의존성** | Mock/Stub | 실제 Bean + 실제 DB |
| **속도** | 빠름 (ms) | 느림 (초) |
| **목적** | 로직 정확도 | 전체 흐름 검증 |
| **테스트** | `@DataJpaTest`, `@WebMvcTest` | `@SpringBootTest` |
| **HTTP** | MockMvc (가짜) | TestRestTemplate (실제) |
| **DB** | H2 (인메모리) | MySQL (실제) |
| **신뢰도** | 중간 | 높음 |

**예시**:

```java
// ✅ 단위 테스트 (Repository Layer)
@DataJpaTest
class OrderRepositoryTest {
    @Autowired
    private OrderRepository repository;

    @Test
    void saveOrder() {
        OrderEntity order = new OrderEntity(...);
        OrderEntity saved = repository.save(order);
        assertThat(saved.getId()).isNotNull();
    }
}

// ✅ 통합 테스트 (전체 레이어)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Sql("/sql/test-data.sql")
@Transactional
class OrderIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndGetOrder() {
        // When - POST /api/orders (실제 HTTP)
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/orders",
            new PlaceOrderRequest(...),
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String orderId = response.getBody().orderId();

        // When - GET /api/orders/{orderId} (실제 HTTP)
        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId,
            OrderResponse.class
        );

        // Then
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 2️⃣ Flyway vs @Sql 명확한 구분

이 부분이 가장 혼동되는 부분입니다. **Flyway와 @Sql은 역할이 완전히 다릅니다.**

### 역할 비교

| 항목 | Flyway | @Sql |
|------|--------|------|
| **목적** | DB 스키마 버전 관리 | 테스트 데이터 삽입 |
| **역할** | `CREATE TABLE`, `ALTER TABLE` (DDL) | `INSERT`, `UPDATE`, `DELETE` (DML) |
| **실행 시점** | 애플리케이션 시작 시 (1번) | 각 테스트 메서드 실행 전 |
| **파일 위치** | `src/main/resources/db/migration/` | `src/test/resources/sql/` |
| **파일 명** | `V1__create_order_table.sql` | `test-data.sql` |
| **운영 사용** | ✅ 사용 (스키마 관리) | ❌ 사용 안 함 (테스트 전용) |
| **롤백** | ❌ 불가 (한번 실행하면 영구적) | ✅ 가능 (`@Transactional` 롤백) |

### 구체적 예시

#### Flyway: 스키마 생성 (DDL)

**파일**: `src/main/resources/db/migration/V1__create_order_table.sql`

```sql
-- Flyway가 실행: 테이블 생성
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount BIGINT NOT NULL,
    order_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_customer_id (customer_id),
    INDEX idx_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### @Sql: 테스트 데이터 삽입 (DML)

**파일**: `src/test/resources/sql/orders-test-data.sql`

```sql
-- @Sql이 실행: 테스트 데이터 삽입
DELETE FROM orders;
DELETE FROM customers;

INSERT INTO customers (customer_id, name, email)
VALUES (1, 'Alice', 'alice@example.com');

INSERT INTO customers (customer_id, name, email)
VALUES (2, 'Bob', 'bob@example.com');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (100, 1, 'PENDING', 10000, '2024-01-01');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (101, 1, 'CONFIRMED', 20000, '2024-01-02');

INSERT INTO orders (order_id, customer_id, status, total_amount, order_date)
VALUES (102, 2, 'PENDING', 30000, '2024-01-03');
```

### ❌ 잘못된 사용 패턴

```java
// ❌ WRONG: @Sql에 DDL 작성
@Sql("/sql/create-tables.sql")  // CREATE TABLE ... (금지!)
@Test
void test() { }

// ❌ WRONG: EntityManager로 데이터 삽입
@BeforeEach
void setUp() {
    OrderEntity order = new OrderEntity(...);
    entityManager.persist(order);  // 권장하지 않음
}
```

### ✅ 올바른 사용 패턴

```java
// ✅ CORRECT: Flyway + @Sql 조합
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Sql("/sql/orders-test-data.sql")  // INSERT만 포함
@Transactional
class OrderIntegrationTest {
    // Flyway: 테이블 이미 생성됨 (V1, V2, V3...)
    // @Sql: 테스트 데이터 삽입
    // @Transactional: 테스트 후 데이터 자동 롤백
}
```

---

## 3️⃣ 전체 실행 흐름

Integration 테스트의 전체 실행 흐름을 이해하면 복잡성이 많이 줄어듭니다.

### 실행 순서

```
1. TestContainers 시작
   └─ Docker로 MySQL 8.0 컨테이너 시작
   └─ 임시 데이터베이스 생성

2. Spring Boot 애플리케이션 시작
   └─ @SpringBootTest로 전체 Bean 로딩

3. Flyway 자동 실행 (spring.flyway.enabled=true)
   └─ V1__create_order_table.sql 실행 (orders 테이블 생성)
   └─ V2__create_customer_table.sql 실행 (customers 테이블 생성)
   └─ V3__add_order_status_column.sql 실행
   └─ ... (순서대로 마이그레이션)

4. @Sql 실행 (각 테스트 메서드 실행 전)
   └─ orders-test-data.sql 실행
   └─ INSERT 데이터 삽입

5. 테스트 메서드 실행
   └─ TestRestTemplate으로 실제 HTTP 요청
   └─ Controller → UseCase → Repository → DB
   └─ 전체 흐름 검증

6. @Transactional 롤백
   └─ @Sql로 삽입한 데이터 자동 삭제
   └─ 다음 테스트를 위한 클린 상태 유지

7. 다음 테스트 메서드 실행 (4~6 반복)

8. 모든 테스트 완료 후
   └─ TestContainers 종료
   └─ MySQL 컨테이너 자동 삭제
```

### 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│ 1. TestContainers Start (MySQL 8.0)                     │
│    Docker → MySQL Container → 임시 DB 생성                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 2. Spring Boot Start (@SpringBootTest)                  │
│    전체 Bean 로딩 (Controller, UseCase, Repository...)   │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 3. Flyway Auto Migration (spring.flyway.enabled=true)  │
│    V1 → V2 → V3 → ... (순서대로 테이블 생성)              │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 4. @Sql Execution (테스트 메서드 실행 전)                 │
│    INSERT 데이터 삽입 (orders, customers)                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 5. Test Method Execution                                │
│    TestRestTemplate → Controller → UseCase → Repository │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 6. @Transactional Rollback                             │
│    @Sql 데이터 자동 삭제 (클린 상태 유지)                  │
└─────────────────────────────────────────────────────────┘
                         ↓
        (다음 테스트 메서드 4~6 반복)
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 7. TestContainers Stop                                  │
│    MySQL Container 자동 삭제                             │
└─────────────────────────────────────────────────────────┘
```

---

## 4️⃣ 기본 설정

### Gradle 의존성

```gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // Flyway
    implementation 'org.flywaydb:flyway-core:9.22.0'
    implementation 'org.flywaydb:flyway-mysql:9.22.0'

    // MySQL
    runtimeOnly 'com.mysql:mysql-connector-j:8.0.33'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers:1.19.0'
    testImplementation 'org.testcontainers:mysql:1.19.0'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
}
```

### application-test.yml

```yaml
spring:
  datasource:
    # TestContainers가 자동으로 설정하므로 url, username, password 불필요

  flyway:
    enabled: true                           # Flyway 활성화 (중요!)
    locations: classpath:db/migration       # 마이그레이션 파일 위치
    baseline-on-migrate: true
    clean-disabled: false                   # 테스트 시 DB 초기화 허용

  jpa:
    hibernate:
      ddl-auto: validate                    # Flyway가 스키마 관리, Hibernate는 검증만
    show-sql: true                          # SQL 로그 출력
    properties:
      hibernate:
        format_sql: true
```

### 테스트 클래스 기본 템플릿

```java
package com.company.adapter.in.restapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order 통합 테스트
 *
 * <p><strong>테스트 범위:</strong></p>
 * <ul>
 *   <li>HTTP Request → Controller → UseCase → Repository → DB</li>
 *   <li>실제 HTTP 요청/응답 검증</li>
 *   <li>전체 레이어 통합 동작 검증</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Order 통합 테스트")
class OrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Sql("/sql/orders-test-data.sql")  // 테스트 데이터 삽입
    @DisplayName("E2E - 주문 생성 및 조회")
    void shouldCreateAndGetOrder() {
        // Given
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 100L, 10);

        // When - 주문 생성 (실제 HTTP POST)
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then - 생성 검증
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        String orderId = createResponse.getBody().orderId();

        // When - 주문 조회 (실제 HTTP GET)
        ResponseEntity<OrderResponse> getResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId,
            OrderResponse.class
        );

        // Then - 조회 검증
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().orderId()).isEqualTo(orderId);
        assertThat(getResponse.getBody().customerId()).isEqualTo(1L);
    }
}
```

---

## 5️⃣ Zero-Tolerance 규칙

Integration 테스트에서 반드시 지켜야 할 규칙:

### 필수 규칙 ✅

```yaml
1. @SpringBootTest(webEnvironment = RANDOM_PORT) 필수
   - 전체 Spring 컨텍스트 로딩
   - 실제 HTTP 서버 시작

2. TestRestTemplate 사용 (MockMvc 금지)
   - 실제 HTTP 요청/응답 검증
   - 직렬화/역직렬화 검증

3. @Transactional + @Rollback(true) 필수
   - 테스트 격리 (각 테스트는 독립적)
   - 데이터 자동 롤백

4. Flyway로 스키마 생성 (spring.flyway.enabled=true)
   - 운영 환경과 동일한 스키마
   - 마이그레이션 파일 재사용

5. @Sql로 테스트 데이터 삽입 (INSERT만)
   - DDL 작성 금지 (CREATE TABLE 금지)
   - DML만 포함 (INSERT, UPDATE, DELETE)

6. @ActiveProfiles("test") 필수
   - 테스트 전용 설정 사용
   - application-test.yml 로드

7. @Testcontainers 필수
   - 실제 DB 사용 (H2 금지)
   - TestContainers로 MySQL 시작
```

### 금지 규칙 ❌

```yaml
1. @Sql에 DDL 작성 금지
   ❌ @Sql("/sql/create-tables.sql")  # CREATE TABLE 금지
   ✅ @Sql("/sql/test-data.sql")      # INSERT만 허용

2. Flyway 마이그레이션 파일 수정 금지
   - 운영 환경 스키마 파일이므로 절대 수정 금지
   - 새로운 파일 추가만 가능

3. @WebMvcTest 사용 금지
   - Integration 테스트는 전체 컨텍스트 필요
   - @SpringBootTest 사용

4. MockMvc 사용 금지
   - 실제 HTTP 필요
   - TestRestTemplate 사용

5. @MockBean 남발 금지
   - 실제 Bean 사용 (통합 테스트 목적)
   - 외부 API만 WireMock으로 모킹

6. EntityManager.persist() 직접 호출 금지
   - @Sql 사용
   - 테스트 데이터는 SQL 파일로 관리

7. @DirtiesContext 남발 금지
   - 느림 (전체 컨텍스트 재로딩)
   - @Transactional 롤백으로 충분
```

---

## 6️⃣ 체크리스트

Integration 테스트 작성 시 확인:

### 프로젝트 구조
- [ ] `src/main/resources/db/migration/` Flyway 마이그레이션 파일 존재
- [ ] `src/test/resources/sql/` @Sql 테스트 데이터 파일 존재
- [ ] `src/test/resources/application-test.yml` 테스트 설정 존재

### Gradle 의존성
- [ ] `spring-boot-starter-web` 존재
- [ ] `spring-boot-starter-data-jpa` 존재
- [ ] `flyway-core`, `flyway-mysql` 존재
- [ ] `testcontainers`, `testcontainers:mysql` 존재

### 설정 파일
- [ ] `spring.flyway.enabled=true` (application-test.yml)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (Flyway가 스키마 관리)
- [ ] `spring.jpa.show-sql=true` (SQL 로그 출력)

### 테스트 클래스
- [ ] `@SpringBootTest(webEnvironment = RANDOM_PORT)` 존재
- [ ] `@ActiveProfiles("test")` 존재
- [ ] `@Testcontainers` 존재
- [ ] `@Transactional` 존재
- [ ] `@Container static MySQLContainer` 존재
- [ ] `TestRestTemplate` 주입 존재

### 테스트 메서드
- [ ] `@Sql("/sql/test-data.sql")` 존재
- [ ] TestRestTemplate으로 실제 HTTP 요청
- [ ] ResponseEntity로 HTTP 상태 코드 검증
- [ ] 전체 흐름 검증 (Create → Get, Create → Update → Get 등)

### @Sql 파일
- [ ] DDL 없음 (CREATE TABLE, ALTER TABLE 금지)
- [ ] DML만 포함 (INSERT, UPDATE, DELETE)
- [ ] DELETE 먼저, INSERT 나중 (데이터 클린업)
- [ ] FK 순서 고려 (부모 먼저, 자식 나중)

---

## 7️⃣ 참고 문서

- [flyway-testing-guide.md](../../04-persistence-layer/mysql/config/flyway-testing-guide.md) - Flyway 상세 가이드
- [query-adapter-integration-testing.md](../../04-persistence-layer/mysql/adapter/query/query-adapter-integration-testing.md) - Query Adapter 통합 테스트
- [02_e2e-test-pattern.md](./02_e2e-test-pattern.md) - E2E 테스트 패턴 (작성 예정)
- [03_test-data-strategy.md](./03_test-data-strategy.md) - @Sql 데이터 전략 (작성 예정)

---

## 8️⃣ 요약

### 핵심 개념

1. **Integration 테스트 = E2E 테스트**
   - 전체 레이어 통합 검증
   - 실제 HTTP + 실제 DB

2. **Flyway vs @Sql**
   - Flyway: 스키마 생성 (DDL) - 운영 환경과 동일
   - @Sql: 테스트 데이터 삽입 (DML) - 테스트 전용

3. **실행 흐름**
   - TestContainers → Flyway → @Sql → Test → Rollback

4. **Zero-Tolerance**
   - @SpringBootTest + TestRestTemplate 필수
   - Flyway 스키마 + @Sql 데이터
   - @Transactional 롤백

### 다음 단계

- E2E 테스트 패턴 학습 (02_e2e-test-pattern.md)
- @Sql 데이터 전략 학습 (03_test-data-strategy.md)
- WireMock 외부 API 모킹 (04_wiremock-integration.md)
- TestContainers 고급 설정 (05_testcontainers-setup.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
