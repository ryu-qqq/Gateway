# Flyway 가이드

> **목적**: Flyway를 활용한 DB 스키마 버전 관리 (운영 + 테스트 환경)

---

## 1️⃣ Flyway란?

### 개념

**Flyway**는 데이터베이스 스키마 버전 관리 도구입니다.

**주요 기능**:
- 📝 **버전 관리**: SQL 마이그레이션 파일로 스키마 버전 추적
- 🔄 **자동 마이그레이션**: 애플리케이션 시작 시 자동으로 스키마 업데이트
- 🔒 **일관성 보장**: 모든 환경(개발/테스트/운영)에서 동일한 스키마 사용

### 왜 테스트에서 Flyway를 사용하나?

**옵션 1: @Sql로 테스트용 DDL 직접 관리**
```java
@Sql("/test-schema.sql")
class MyTest {
    // 테스트용 DDL 파일 별도 관리
}
```
- ❌ **문제점**: 운영 스키마와 테스트 스키마가 불일치할 수 있음
- ❌ **유지보수**: DDL 변경 시 테스트 파일도 수동으로 동기화 필요

**옵션 2: Flyway 마이그레이션 파일 재사용** ✅
```java
@DataJpaTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class MyTest {
    // Flyway가 자동으로 스키마 생성
}
```
- ✅ **일관성**: 운영 환경과 동일한 스키마 사용
- ✅ **자동화**: 마이그레이션 파일 변경 시 테스트 스키마도 자동 업데이트
- ✅ **신뢰성**: 실제 운영 스키마로 테스트 실행

---

## 2️⃣ Flyway 프로젝트 구조

### 디렉토리 구조

```
adapter-out/persistence-mysql/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.company.adapter.out.persistence/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/              # Flyway 마이그레이션 파일
│   │               ├── V1__create_order_table.sql
│   │               ├── V2__create_customer_table.sql
│   │               ├── V3__add_order_status_column.sql
│   │               └── V4__add_customer_email_column.sql
│   └── test/
│       ├── java/
│       │   └── com.company.adapter.out.persistence/
│       └── resources/
│           └── application-test.yml        # 테스트용 Flyway 설정
```

### 마이그레이션 파일 네이밍 규칙

**형식**: `V{버전}__{설명}.sql`

**예시**:
- `V1__create_order_table.sql` - 초기 테이블 생성
- `V2__add_status_column.sql` - 컬럼 추가
- `V3__create_index_on_order_date.sql` - 인덱스 생성

**규칙**:
- ✅ `V` 대문자로 시작
- ✅ 버전 번호 (`1`, `2`, `3`, ...)
- ✅ 언더스코어 2개 (`__`)
- ✅ 설명은 snake_case
- ❌ 공백 불가
- ❌ 특수문자 불가 (언더스코어 제외)

---

## 📂 마이그레이션 파일 구조 전략

### 권장: 단일 패키지 순차 관리 ✅

**구조**:
```
db/migration/
├── V1__create_order_table.sql
├── V2__create_customer_table.sql
├── V3__create_product_table.sql
├── V4__create_payment_table.sql
└── V5__add_order_status_column.sql
```

**장점**:
- ✅ **버전 관리 단순**: 전체 프로젝트의 스키마 버전을 하나의 타임라인으로 관리
- ✅ **충돌 방지**: BC(Bounded Context)별로 버전 번호가 겹치지 않음
- ✅ **Flyway 표준**: Flyway 권장 방식
- ✅ **마이그레이션 순서 명확**: V1 → V2 → V3 순서대로 실행

**단점**:
- ⚠️ **파일 증가**: BC가 많을수록 한 디렉토리에 파일이 많아짐

---

### 대안: BC별 패키지 분리 (비권장)

**구조**:
```
db/migration/
├── order/
│   ├── V1__create_order_table.sql
│   └── V2__add_order_status_column.sql
├── customer/
│   ├── V1__create_customer_table.sql
│   └── V2__add_customer_email_column.sql
└── product/
    └── V1__create_product_table.sql
```

**문제점**:
- ❌ **버전 충돌**: 각 BC에서 V1, V2가 중복됨
- ❌ **실행 순서 불명확**: `order/V1` vs `customer/V1` 중 어느 것이 먼저?
- ❌ **Flyway 비표준**: Flyway는 단일 디렉토리 순차 실행 가상
- ❌ **복잡성 증가**: 여러 디렉토리 관리 필요

**해결 방법** (BC별 분리가 필요하다면):
```
db/migration/
├── order/
│   ├── V1__create_order_table.sql       # V1 (전체 타임라인 기준)
│   └── V5__add_order_status_column.sql  # V5 (전체 타임라인 기준)
├── customer/
│   ├── V2__create_customer_table.sql    # V2 (전체 타임라인 기준)
│   └── V6__add_customer_email_column.sql # V6 (전체 타임라인 기준)
└── product/
    └── V3__create_product_table.sql     # V3 (전체 타임라인 기준)
```
- ✅ **전체 타임라인 유지**: BC별로 디렉토리는 나누되, 버전 번호는 전체 프로젝트 기준으로 순차 부여
- ✅ **충돌 방지**: 버전 번호가 겹치지 않음

---

### 권장 사항 요약

| 항목 | 단일 패키지 | BC별 패키지 |
|------|-------------|-------------|
| **버전 관리** | ✅ 단순 | ❌ 복잡 |
| **충돌 위험** | ✅ 없음 | ⚠️ 있음 (해결 가능) |
| **Flyway 표준** | ✅ 권장 | ❌ 비권장 |
| **파일 개수** | ⚠️ 많을 수 있음 | ✅ 분산 |
| **실행 순서** | ✅ 명확 | ⚠️ 설정 필요 |

**✅ 권장**: 단일 패키지 순차 관리 (`db/migration/V1__`, `V2__`, ...)
- 대부분의 프로젝트에 적합
- Flyway 표준 방식
- 버전 관리 단순

**⚠️ 예외**: BC가 10개 이상이고 각 BC의 마이그레이션 파일이 수십 개라면 BC별 패키지 분리 고려
- 단, 버전 번호는 전체 프로젝트 타임라인 기준으로 부여

---

## 3️⃣ 운영 환경 Flyway 설정

### application.yml (운영)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  flyway:
    enabled: true                           # Flyway 활성화
    locations: classpath:db/migration       # 마이그레이션 파일 위치
    baseline-on-migrate: true               # 기존 DB에 Flyway 적용 시 필요
    validate-on-migrate: true               # 마이그레이션 검증
    out-of-order: false                     # 순서대로 마이그레이션 실행

  jpa:
    hibernate:
      ddl-auto: validate                    # Hibernate는 검증만, Flyway가 스키마 관리
    show-sql: false
```

**설정 설명**:
- `enabled: true`: Flyway 활성화 (애플리케이션 시작 시 자동 마이그레이션)
- `locations`: 마이그레이션 파일 위치 (`src/main/resources/db/migration/`)
- `baseline-on-migrate: true`: 기존 DB에 Flyway를 처음 적용할 때 필요
- `validate-on-migrate: true`: 마이그레이션 실행 전 검증
- `ddl-auto: validate`: Hibernate는 스키마 생성하지 않고 검증만 수행

---

## 4️⃣ 테스트 환경 Flyway 설정

### application-test.yml (테스트)

```yaml
spring:
  datasource:
    # TestContainers가 자동으로 설정하므로 url, username, password 불필요

  flyway:
    enabled: true                           # 테스트에서도 Flyway 활성화
    locations: classpath:db/migration       # 운영과 동일한 마이그레이션 파일 사용
    baseline-on-migrate: true
    clean-disabled: false                   # 테스트 시 DB 초기화 허용

  jpa:
    hibernate:
      ddl-auto: validate                    # Flyway가 스키마 관리, Hibernate는 검증만
    show-sql: true                          # 테스트 시 SQL 로그 출력
    properties:
      hibernate:
        format_sql: true
```

**설정 설명**:
- `enabled: true`: 테스트에서도 Flyway 자동 실행
- `locations`: 운영 환경과 동일한 마이그레이션 파일 사용 (중요!)
- `clean-disabled: false`: 테스트 시 DB 초기화 허용 (운영에서는 true)
- `show-sql: true`: 테스트 시 실행되는 SQL 확인

### 테스트 클래스 설정

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",           // Flyway 활성화
    "spring.jpa.hibernate.ddl-auto=validate" // Hibernate 검증 모드
})
class MyRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    // 테스트 코드
}
```

**설정 설명**:
- `@AutoConfigureTestDatabase(replace = NONE)`: H2 대신 실제 MySQL 사용
- `@Testcontainers`: TestContainers 활성화
- `spring.flyway.enabled=true`: Flyway 자동 실행
- `ddl-auto=validate`: Hibernate는 스키마 생성하지 않음

---

## 5️⃣ 마이그레이션 파일 작성 예시

### V1__create_order_table.sql

```sql
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount BIGINT NOT NULL,
    order_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_customer_id (customer_id),
    INDEX idx_order_date (order_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### V2__create_customer_table.sql

```sql
CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_email (email),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### V3__add_order_status_column.sql

```sql
-- 기존 status 컬럼 타입 변경
ALTER TABLE orders
    MODIFY COLUMN status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL;
```

### V4__add_customer_email_column.sql

```sql
-- 이미 V2에서 email 컬럼을 추가했으므로 이 파일은 예시용
-- 실제로는 중복되지 않도록 관리 필요
```

---

## 6️⃣ Gradle 의존성

```gradle
dependencies {
    // Flyway
    implementation 'org.flywaydb:flyway-core:9.22.0'
    implementation 'org.flywaydb:flyway-mysql:9.22.0'

    // MySQL
    runtimeOnly 'com.mysql:mysql-connector-j:8.0.33'

    // TestContainers
    testImplementation 'org.testcontainers:testcontainers:1.19.0'
    testImplementation 'org.testcontainers:mysql:1.19.0'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
}
```

---

## 7️⃣ 테스트 실행 흐름

### 1. TestContainers 시작

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
```
- Docker로 MySQL 8.0 컨테이너 시작
- 임시 데이터베이스 생성

### 2. Flyway 자동 실행

```
spring.flyway.enabled=true
```
- 애플리케이션 시작 시 Flyway 자동 실행
- `db/migration/` 폴더의 마이그레이션 파일 순서대로 실행
- V1 → V2 → V3 → V4 순서로 스키마 생성

### 3. 테스트 실행

```java
@Test
void myTest() {
    // Flyway가 생성한 스키마에서 테스트 실행
}
```

### 4. TestContainers 종료

- 테스트 완료 후 MySQL 컨테이너 자동 삭제
- 다음 테스트 실행 시 새로운 컨테이너 생성

---

## 8️⃣ Flyway 명령어

### Gradle Task

```bash
# 마이그레이션 정보 조회
./gradlew flywayInfo

# 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 검증
./gradlew flywayValidate

# DB 초기화 (운영 환경 절대 금지!)
./gradlew flywayClean
```

### 주의사항

⚠️ **운영 환경에서 절대 사용 금지**:
- `flywayClean`: 모든 테이블 삭제
- `clean-disabled: true`: 운영 환경에서 반드시 설정

---

## 9️⃣ 실전 예시

### 전체 테스트 클래스

```java
package com.company.adapter.out.persistence.repository;

import com.company.adapter.out.persistence.entity.OrderJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway 통합 테스트 예시
 *
 * <p>Flyway가 자동으로 스키마를 생성하고 테스트가 실행됩니다.</p>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true"
})
@DisplayName("Flyway 통합 테스트")
class FlywayIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Flyway로 생성된 스키마에서 Order 저장 및 조회")
    void saveAndFind_WithFlywaySchema() {
        // Given
        OrderJpaEntity order = OrderJpaEntity.create(
            1L,                      // customerId
            OrderStatus.PENDING,     // status
            10000L,                  // totalAmount
            LocalDate.now()          // orderDate
        );

        // When
        OrderJpaEntity saved = orderRepository.save(order);
        OrderJpaEntity found = orderRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getCustomerId()).isEqualTo(1L);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

---

## 🔟 문제 해결

### 문제 1: Flyway 마이그레이션 실패

**증상**:
```
FlywayException: Validate failed: Migration checksum mismatch
```

**원인**: 마이그레이션 파일이 수정됨

**해결**:
1. **테스트 환경**: DB 초기화 후 재실행
   ```bash
   ./gradlew flywayClean flywayMigrate
   ```
2. **운영 환경**: 새로운 마이그레이션 파일 추가
   ```sql
   -- V5__fix_previous_migration.sql
   ```

### 문제 2: Hibernate와 Flyway 충돌

**증상**:
```
Table 'orders' already exists
```

**원인**: Hibernate `ddl-auto=create`와 Flyway가 동시에 실행

**해결**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # create 대신 validate 사용
```

### 문제 3: TestContainers 시작 실패

**증상**:
```
Could not start container
```

**원인**: Docker가 실행되지 않음

**해결**:
```bash
# Docker Desktop 실행 확인
docker ps
```

---

## 1️⃣1️⃣ 체크리스트

Flyway 테스트 통합 시:

### 프로젝트 구조
- [ ] `src/main/resources/db/migration/` 디렉토리 생성
- [ ] 마이그레이션 파일 네이밍 규칙 준수 (`V1__description.sql`)

### 운영 환경 설정
- [ ] `application.yml`에 Flyway 설정 추가
- [ ] `spring.flyway.enabled=true`
- [ ] `spring.jpa.hibernate.ddl-auto=validate`

### 테스트 환경 설정
- [ ] `application-test.yml`에 Flyway 설정 추가
- [ ] TestContainers 의존성 추가
- [ ] `@DataJpaTest` + `@Testcontainers` 설정

### 마이그레이션 파일
- [ ] 초기 테이블 생성 (V1)
- [ ] 컬럼 추가/수정 (V2, V3, ...)
- [ ] 인덱스 생성 (필요 시)

### 테스트 실행
- [ ] Flyway 자동 실행 확인
- [ ] 테스트 통과 확인
- [ ] SQL 로그 확인 (`show-sql=true`)

---

## 1️⃣2️⃣ 참고 문서

- [Flyway 공식 문서](https://flywaydb.org/documentation/)
- [TestContainers 공식 문서](https://www.testcontainers.org/)
- [hikaricp-configuration.md](./hikaricp-configuration.md) - HikariCP 설정 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
