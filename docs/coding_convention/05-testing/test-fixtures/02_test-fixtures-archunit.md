# Test Fixtures ArchUnit — **의존성 규칙 자동 검증**

> **목적**: Test Fixtures 모듈의 의존성 규칙을 ArchUnit으로 자동 검증

---

## 1️⃣ 검증 규칙 개요

### 핵심 원칙

1. **domain-test-fixtures**: `domain`만 의존
2. **application-test-fixtures**: `application` + `domain-test-fixtures` 의존
3. **adapter-test-fixtures**: 해당 `adapter` + `application-test-fixtures` 의존
4. **역방향 의존 금지**: 하위 레이어가 상위 레이어 Fixture 의존 불가

---

## 2️⃣ ArchUnit 테스트 코드

### 전체 테스트 클래스

**위치**: `application/src/test/java/com/ryuqq/application/architecture/TestFixturesArchTest.java`

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
 * Test Fixtures 의존성 규칙 ArchUnit 검증 (Zero-Tolerance)
 *
 * <p>모든 Test Fixtures 모듈은 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>domain-test-fixtures: domain만 의존</li>
 *   <li>application-test-fixtures: application + domain-test-fixtures 의존</li>
 *   <li>adapter-test-fixtures: 해당 adapter + application-test-fixtures 의존</li>
 *   <li>역방향 의존 금지: 하위 → 상위 레이어 Fixture 의존 불가</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("test-fixtures")
@DisplayName("Test Fixtures Dependency ArchUnit Tests (Zero-Tolerance)")
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
            .resideInAnyPackage(
                "..domain..",
                "java..",
                "org.junit..",
                "org.assertj..",
                "org.mockito.."
            )
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
                "org.assertj..",
                "org.mockito.."
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
            .because("domain-test-fixtures는 application-test-fixtures를 의존할 수 없습니다 (역방향 의존 금지)");

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
            .because("application-test-fixtures는 adapter-test-fixtures를 의존할 수 없습니다 (역방향 의존 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 5: adapter-in-test-fixtures는 adapter-out-test-fixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-in-test-fixtures는 adapter-out-test-fixtures를 의존할 수 없다")
    void adapterInTestFixtures_MustNotDependOnAdapterOutTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter.out..")
            .because("adapter-in-test-fixtures는 adapter-out-test-fixtures를 의존할 수 없습니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: adapter-out-test-fixtures는 adapter-in-test-fixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-out-test-fixtures는 adapter-in-test-fixtures를 의존할 수 없다")
    void adapterOutTestFixtures_MustNotDependOnAdapterInTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.adapter.out..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter.in..")
            .because("adapter-out-test-fixtures는 adapter-in-test-fixtures를 의존할 수 없습니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Fixture 클래스는 public이어야 함
     */
    @Test
    @DisplayName("[필수] Fixture 클래스는 public이어야 한다")
    void fixtureClasses_MustBePublic() {
        ArchRule rule = classes()
            .that().resideInAPackage("..fixture..")
            .and().haveSimpleNameEndingWith("Fixture")
            .should().bePublic()
            .because("Fixture 클래스는 다른 모듈에서 사용하기 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: Fixture 메서드는 static이어야 함
     */
    @Test
    @DisplayName("[필수] Fixture 메서드는 static이어야 한다")
    void fixtureMethods_ShouldBeStatic() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..fixture..")
            .and().arePublic()
            .should().beStatic()
            .because("Fixture 메서드는 인스턴스 생성 없이 사용하기 위해 static이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Fixture 클래스는 상태(필드)를 가지지 않아야 함
     */
    @Test
    @DisplayName("[금지] Fixture 클래스는 인스턴스 필드를 가질 수 없다")
    void fixtureClasses_MustNotHaveInstanceFields() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..fixture..")
            .and().areNotStatic()
            .should().notBeDeclared()
            .because("Fixture 클래스는 상태를 가질 수 없습니다 (Stateless Factory Pattern)");

        rule.check(classes);
    }

    /**
     * 규칙 10: Fixture 클래스는 생성자를 가지지 않아야 함
     */
    @Test
    @DisplayName("[금지] Fixture 클래스는 public 생성자를 가질 수 없다")
    void fixtureClasses_MustNotHavePublicConstructor() {
        ArchRule rule = noConstructors()
            .that().areDeclaredInClassesThat().resideInAPackage("..fixture..")
            .and().arePublic()
            .should().beDeclared()
            .because("Fixture 클래스는 인스턴스 생성이 불필요합니다 (모든 메서드 static)");

        rule.check(classes);
    }

    /**
     * 규칙 11: Fixture 클래스 네이밍 규칙
     */
    @Test
    @DisplayName("[필수] Fixture 클래스는 'Fixture' 접미사를 가져야 한다")
    void fixtureClasses_MustHaveFixtureSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..fixture..")
            .should().haveSimpleNameEndingWith("Fixture")
            .because("Fixture 클래스는 'Fixture' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: Fixture 패키지 위치
     */
    @Test
    @DisplayName("[필수] Fixture 클래스는 fixture 패키지에 위치해야 한다")
    void fixtureClasses_MustResideInFixturePackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .should().resideInAPackage("..fixture..")
            .because("Fixture 클래스는 fixture 패키지에 위치해야 합니다");

        rule.check(classes);
    }
}
```

---

## 3️⃣ 의존성 매트릭스 (ArchUnit 검증)

| From ↓ / To → | domain-test-fixtures | application-test-fixtures | adapter-*-test-fixtures |
|---------------|----------------------|---------------------------|-------------------------|
| **domain-test-fixtures** | - | ❌ (규칙 3) | ❌ (규칙 3) |
| **application-test-fixtures** | ✅ (규칙 2) | - | ❌ (규칙 4) |
| **adapter-in-test-fixtures** | ✅ | ✅ | ❌ (규칙 5) |
| **adapter-out-test-fixtures** | ✅ | ✅ | ❌ (규칙 6) |

---

## 4️⃣ 빌드 시 자동 검증

### build.gradle 설정

```gradle
// application/build.gradle

dependencies {
    // ArchUnit
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.1.0'
}

tasks.named('test') {
    useJUnitPlatform {
        // ⭐ ArchUnit 테스트 포함
        includeTags 'architecture', 'test-fixtures'
    }
}
```

### 빌드 시 검증

```bash
# 전체 빌드 (ArchUnit 자동 실행)
./gradlew clean build

# ArchUnit 테스트만 실행
./gradlew test --tests "*ArchTest"

# Test Fixtures ArchUnit만 실행
./gradlew test --tests "*TestFixturesArchTest"
```

---

## 5️⃣ CI/CD 통합

### GitHub Actions 예시

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew clean build

      - name: Run ArchUnit Tests
        run: ./gradlew test --tests "*ArchTest"

      - name: Run Test Fixtures ArchUnit
        run: ./gradlew test --tests "*TestFixturesArchTest"
```

---

## 6️⃣ 위반 예시 및 해결

### 예시 1: domain-test-fixtures가 application-test-fixtures 의존

**❌ Bad**:
```java
// domain-test-fixtures/OrderFixture.java
package com.ryuqq.fixture.domain;

import com.ryuqq.fixture.application.command.PlaceOrderCommandFixture;  // ❌

public class OrderFixture {
    public static Order fromCommand(PlaceOrderCommand command) {  // ❌
        // ...
    }
}
```

**ArchUnit 실패 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..fixture.domain..' should depend on classes that reside in a package '..fixture.application..'' was violated (1 times):
Class <com.ryuqq.fixture.domain.OrderFixture> depends on class <com.ryuqq.fixture.application.command.PlaceOrderCommandFixture>
```

**✅ Good**:
```java
// domain-test-fixtures/OrderFixture.java
package com.ryuqq.fixture.domain;

public class OrderFixture {
    public static Order defaultOrder() {  // ✅ Domain 객체만 생성
        return Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );
    }
}
```

### 예시 2: Fixture 클래스에 인스턴스 필드 존재

**❌ Bad**:
```java
// domain-test-fixtures/OrderFixture.java
package com.ryuqq.fixture.domain;

public class OrderFixture {
    private static int counter = 0;  // ✅ static은 허용
    private String name;              // ❌ 인스턴스 필드 금지

    public OrderFixture(String name) {  // ❌ public 생성자 금지
        this.name = name;
    }

    public Order create() {  // ❌ non-static 메서드 금지
        return Order.forNew(OrderId.of(counter++), Money.of(BigDecimal.ZERO));
    }
}
```

**ArchUnit 실패 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'fields that are declared in classes that reside in a package '..fixture..' and are not static should not be declared' was violated (1 times):
Field <com.ryuqq.fixture.domain.OrderFixture.name> is declared in <com.ryuqq.fixture.domain.OrderFixture>
```

**✅ Good**:
```java
// domain-test-fixtures/OrderFixture.java
package com.ryuqq.fixture.domain;

public class OrderFixture {
    private static int counter = 0;  // ✅ static 필드 허용

    private OrderFixture() {  // ✅ private 생성자로 인스턴스 생성 방지
        throw new AssertionError("Utility class");
    }

    public static Order defaultOrder() {  // ✅ static 메서드
        return Order.forNew(
            OrderId.of(counter++),
            Money.of(BigDecimal.ZERO)
        );
    }
}
```

---

## 7️⃣ 체크리스트

ArchUnit 검증 구현 시:
- [ ] `TestFixturesArchTest.java` 파일 생성
- [ ] 12개 규칙 모두 구현
- [ ] build.gradle에 ArchUnit 의존성 추가
- [ ] `@Tag("architecture")`, `@Tag("test-fixtures")` 적용
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 위반 시 빌드 실패 확인
- [ ] 팀 전체 규칙 공유

---

## 📖 관련 문서

- **[Test Fixtures Guide](./01_test-fixtures-guide.md)** - 테스트 픽스쳐 전체 가이드
- **[Test Fixtures Migration](./03_test-fixtures-migration.md)** - 마이그레이션 가이드
- **[ArchUnit Rules](../../05-testing/archunit-rules/)** - 전체 ArchUnit 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
