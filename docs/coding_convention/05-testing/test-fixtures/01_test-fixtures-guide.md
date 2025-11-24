# Test Fixtures Guide — **헥사고날 멀티모듈 테스트 픽스쳐 전략**

> **목적**: 테스트 픽스쳐 중복을 제거하고, 헥사고날 아키텍처 의존성 규칙을 준수하는 테스트 픽스쳐 모듈 구조

---

## 1️⃣ 문제 상황

### 기존 방식의 문제점

```
domain/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ Domain에만 존재

application/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ 중복! (Domain과 동일)

adapter-in-rest/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ 중복! (Domain과 동일)
```

**문제**:
- Domain 객체 Fixture가 **여러 레이어에서 중복 작성**
- 테스트 코드 간 공유 불가능 (각 모듈의 `src/test/java/`는 격리됨)
- Fixture 변경 시 모든 레이어에서 수정 필요
- 유지보수 비용 증가

---

## 2️⃣ 해결 방안: 명시적 Test Fixtures 모듈

### 전략

**핵심 원칙**:
1. **명시적 모듈 이름**: `domain-test-fixtures`, `application-test-fixtures` (testFixtures 대신)
2. **src/main/java 위치**: 다른 모듈에서 import 가능하도록 `main` 소스셋 사용
3. **의존성 흐름 준수**: 헥사고날 아키텍처 의존성 규칙 유지
4. **최소 구성**: `domain-test-fixtures` + `application-test-fixtures`만 필수

### 디렉터리 구조

```
project/
├── domain/                          (Production 코드)
├── domain-test-fixtures/            ⭐ Domain 객체 Fixture
│   └── src/main/java/              (⚠️ main! test 아님)
│       └── com/ryuqq/fixture/domain/
│           ├── OrderFixture.java
│           ├── ProductFixture.java
│           └── CustomerFixture.java
│
├── application/                     (Production 코드)
├── application-test-fixtures/       ⭐ Application DTO Fixture
│   └── src/main/java/
│       └── com/ryuqq/fixture/application/
│           ├── command/
│           │   └── PlaceOrderCommandFixture.java
│           └── response/
│               └── OrderResponseFixture.java
│
├── adapter-in-rest/                 (Production 코드)
└── adapter-in-rest-test-fixtures/   ⭐ Optional (REST Request Fixture)
    └── src/main/java/
        └── com/ryuqq/fixture/adapter/rest/
            └── OrderRequestFixture.java
```

---

## 3️⃣ 의존성 흐름 (Dependency Flow)

### 허용되는 의존성 (✅)

```
domain-test-fixtures
    ↓ api
  domain
```

```
application-test-fixtures
    ↓ api                    ↓ api
  application         domain-test-fixtures
```

```
adapter-in-rest-test-fixtures
    ↓ api                    ↓ api
  adapter-in-rest    application-test-fixtures
```

### 금지된 의존성 (❌)

```
domain-test-fixtures → application-test-fixtures   ❌
application-test-fixtures → adapter-*-test-fixtures   ❌
adapter-in-test-fixtures → adapter-out-test-fixtures   ❌
```

---

## 4️⃣ Gradle 설정

### domain-test-fixtures/build.gradle

```gradle
plugins {
    id 'java-library'
}

dependencies {
    // ✅ Domain 모듈 의존 (api로 전파)
    api project(':domain')

    // ✅ 테스트 라이브러리
    implementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    implementation 'org.assertj:assertj-core:3.24.2'
}

java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```

### application-test-fixtures/build.gradle

```gradle
plugins {
    id 'java-library'
}

dependencies {
    // ✅ Application 모듈 의존
    api project(':application')

    // ✅ Domain Test Fixtures 의존 (Domain 객체 재사용)
    api project(':domain-test-fixtures')

    // ✅ 테스트 라이브러리
    implementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    implementation 'org.assertj:assertj-core:3.24.2'
}

java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```

### adapter-in-rest-test-fixtures/build.gradle (Optional)

```gradle
plugins {
    id 'java-library'
}

dependencies {
    // ✅ Adapter 모듈 의존
    api project(':adapter-in-rest')

    // ✅ Application Test Fixtures 의존
    api project(':application-test-fixtures')

    // ✅ 테스트 라이브러리
    implementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    implementation 'org.assertj:assertj-core:3.24.2'
    implementation 'org.springframework.boot:spring-boot-starter-test'
}

java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}
```

### 실제 테스트 모듈에서 사용 (application/build.gradle)

```gradle
dependencies {
    // Production 의존성
    implementation project(':domain')

    // ✅ Test Fixtures 의존 (testImplementation)
    testImplementation project(':domain-test-fixtures')
    testImplementation project(':application-test-fixtures')

    // 테스트 라이브러리
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-junit-jupiter'
}
```

---

## 5️⃣ 코드 예시

### domain-test-fixtures/OrderFixture.java

```java
package com.ryuqq.fixture.domain;

import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.OrderStatus;
import com.ryuqq.domain.order.Money;

import java.math.BigDecimal;

/**
 * Order Domain 객체 Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class OrderFixture {

    /**
     * 기본 Order Fixture (신규)
     */
    public static Order defaultNewOrder() {
        return Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );
    }

    /**
     * 기존 Order Fixture (저장된 상태)
     */
    public static Order defaultExistingOrder() {
        return Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000)),
            OrderStatus.PLACED
        );
    }

    /**
     * 취소된 Order Fixture
     */
    public static Order canceledOrder() {
        Order order = defaultExistingOrder();
        order.cancel();
        return order;
    }

    /**
     * Custom Order Fixture Builder
     */
    public static Order customOrder(Long id, BigDecimal amount, OrderStatus status) {
        return Order.forExisting(
            OrderId.of(id),
            Money.of(amount),
            status
        );
    }
}
```

### application-test-fixtures/PlaceOrderCommandFixture.java

```java
package com.ryuqq.fixture.application.command;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;

import java.math.BigDecimal;

/**
 * PlaceOrderCommand DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class PlaceOrderCommandFixture {

    /**
     * 기본 PlaceOrderCommand Fixture
     */
    public static PlaceOrderCommand defaultCommand() {
        return new PlaceOrderCommand(
            BigDecimal.valueOf(50000)
        );
    }

    /**
     * Custom PlaceOrderCommand Fixture
     */
    public static PlaceOrderCommand customCommand(BigDecimal amount) {
        return new PlaceOrderCommand(amount);
    }
}
```

### application/src/test/java/.../PlaceOrderServiceTest.java (사용 예시)

```java
package com.ryuqq.application.order.service;

import com.ryuqq.application.order.port.out.OrderPersistencePort;
import com.ryuqq.domain.order.Order;
import com.ryuqq.fixture.domain.OrderFixture;  // ✅ Domain Fixture 사용
import com.ryuqq.fixture.application.command.PlaceOrderCommandFixture;  // ✅ Application Fixture 사용
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderPersistencePort persistencePort;

    @InjectMocks
    private PlaceOrderService service;

    @Test
    void execute_ShouldPlaceOrder() {
        // Given
        var command = PlaceOrderCommandFixture.defaultCommand();  // ✅ Fixture 사용
        var order = OrderFixture.defaultNewOrder();  // ✅ Fixture 사용

        given(persistencePort.save(any(Order.class)))
            .willReturn(order);

        // When
        var response = service.execute(command);

        // Then
        assertThat(response).isNotNull();
        then(persistencePort).should(times(1)).save(any(Order.class));
    }
}
```

---

## 6️⃣ 의존성 매트릭스

### 허용/금지 의존성 규칙

| From ↓ / To → | domain-test-fixtures | application-test-fixtures | adapter-*-test-fixtures |
|---------------|----------------------|---------------------------|-------------------------|
| **domain tests** | ✅ | ❌ | ❌ |
| **application tests** | ✅ | ✅ | ❌ |
| **adapter-in tests** | ✅ | ✅ | ✅ (adapter-in) |
| **adapter-out tests** | ✅ | ❌ | ✅ (adapter-out) |

---

## 7️⃣ 최소 구성 (Recommended)

### 필수 모듈 (2개)

```
project/
├── domain-test-fixtures/        ⭐ 필수 (Domain 객체 Fixture)
└── application-test-fixtures/   ⭐ 필수 (DTO Fixture)
```

**이유**:
- Domain 객체는 **모든 레이어**에서 사용 → Domain Test Fixtures 필수
- Application DTO는 **Adapter에서 변환**에 사용 → Application Test Fixtures 필수
- Adapter 전용 Fixture는 선택적 (필요 시 추가)

### settings.gradle 설정

```gradle
rootProject.name = 'spring-standards'

// Production 모듈
include 'domain'
include 'application'
include 'adapter-in-rest'
include 'adapter-out-persistence'

// ⭐ Test Fixtures 모듈 추가
include 'domain-test-fixtures'
include 'application-test-fixtures'
```

---

## 8️⃣ ArchUnit 검증

### Test Fixtures 의존성 규칙 자동 검증

**위치**: `application/src/test/java/architecture/TestFixturesArchTest.java`

```java
package com.ryuqq.application.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Test Fixtures 의존성 규칙 ArchUnit 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@DisplayName("Test Fixtures Dependency ArchUnit Tests")
class TestFixturesArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq");
    }

    /**
     * 규칙 1: domain-test-fixtures는 domain만 의존
     */
    @Test
    @DisplayName("[필수] domain-test-fixtures는 domain만 의존해야 한다")
    void domainTestFixtures_ShouldOnlyDependOnDomain() {
        ArchRule rule = classes()
            .that().resideInAPackage("..fixture.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "org.junit..", "org.assertj..")
            .because("domain-test-fixtures는 domain만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: application-test-fixtures는 application + domain-test-fixtures 의존
     */
    @Test
    @DisplayName("[필수] application-test-fixtures는 application과 domain-test-fixtures만 의존해야 한다")
    void applicationTestFixtures_ShouldOnlyDependOnApplicationAndDomainFixtures() {
        ArchRule rule = classes()
            .that().resideInAPackage("..fixture.application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..application..",
                "..domain..",
                "..fixture.domain..",
                "java..",
                "org.junit..",
                "org.assertj.."
            )
            .because("application-test-fixtures는 application과 domain-test-fixtures만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: domain-test-fixtures는 application-test-fixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] domain-test-fixtures는 application-test-fixtures를 의존할 수 없다")
    void domainTestFixtures_MustNotDependOnApplicationTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.application..")
            .because("domain-test-fixtures는 application-test-fixtures를 의존할 수 없습니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: application-test-fixtures는 adapter-test-fixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] application-test-fixtures는 adapter-test-fixtures를 의존할 수 없다")
    void applicationTestFixtures_MustNotDependOnAdapterTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter..")
            .because("application-test-fixtures는 adapter-test-fixtures를 의존할 수 없습니다");

        rule.check(classes);
    }
}
```

---

## 9️⃣ 마이그레이션 가이드

### 기존 testFixtures 패키지에서 마이그레이션

**Step 1**: 새 Test Fixtures 모듈 생성

```bash
# 디렉터리 생성
mkdir -p domain-test-fixtures/src/main/java/com/ryuqq/fixture/domain
mkdir -p application-test-fixtures/src/main/java/com/ryuqq/fixture/application
```

**Step 2**: build.gradle 생성

(위 4️⃣ Gradle 설정 참고)

**Step 3**: settings.gradle에 추가

```gradle
include 'domain-test-fixtures'
include 'application-test-fixtures'
```

**Step 4**: 기존 Fixture 코드 이동

```bash
# Domain Fixture 이동
mv domain/src/test/java/.../fixture/OrderFixture.java \
   domain-test-fixtures/src/main/java/com/ryuqq/fixture/domain/

# Application Fixture 이동
mv application/src/test/java/.../fixture/PlaceOrderCommandFixture.java \
   application-test-fixtures/src/main/java/com/ryuqq/fixture/application/command/
```

**Step 5**: 테스트 코드에서 import 변경

```java
// Before
import com.ryuqq.domain.fixture.OrderFixture;

// After
import com.ryuqq.fixture.domain.OrderFixture;
```

**Step 6**: build.gradle에 testImplementation 추가

```gradle
dependencies {
    testImplementation project(':domain-test-fixtures')
    testImplementation project(':application-test-fixtures')
}
```

**Step 7**: 빌드 및 테스트

```bash
./gradlew clean build
./gradlew test
```

---

## 🔟 체크리스트

Test Fixtures 모듈 생성 시:
- [ ] `domain-test-fixtures`, `application-test-fixtures` 모듈 생성
- [ ] `src/main/java` 위치에 Fixture 코드 작성 (test 아님!)
- [ ] build.gradle에 `api` 의존성 설정
- [ ] settings.gradle에 모듈 등록
- [ ] 헥사고날 의존성 규칙 준수
- [ ] ArchUnit 검증 테스트 작성
- [ ] 기존 테스트 코드에서 import 변경
- [ ] 빌드 및 테스트 통과 확인

---

## 📖 관련 문서

- **[Test Fixtures ArchUnit](./02_test-fixtures-archunit.md)** - ArchUnit 검증 규칙 상세
- **[Test Fixtures Migration](./03_test-fixtures-migration.md)** - 기존 코드 마이그레이션 가이드
- **[Multi-Module Structure](../../00-project-setup/multi-module-structure.md)** - 멀티모듈 구조 전체 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
