# Test Fixtures ArchUnit — **의존성 규칙 자동 검증**

> **목적**: Gradle testFixtures의 의존성 규칙을 ArchUnit으로 자동 검증

---

## 1️⃣ 검증 규칙 개요

### 핵심 원칙

1. **domain testFixtures**: `domain`만 의존
2. **application testFixtures**: `application` + `domain testFixtures` 의존
3. **adapter testFixtures**: 해당 `adapter` + 상위 레이어 testFixtures 의존
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
 * <p>모든 Test Fixtures는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>domain testFixtures: domain만 의존</li>
 *   <li>application testFixtures: application + domain testFixtures 의존</li>
 *   <li>adapter testFixtures: 해당 adapter + 상위 레이어 testFixtures 의존</li>
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
     * 규칙 1: domain testFixtures는 domain만 의존
     */
    @Test
    @DisplayName("[필수] domain testFixtures는 domain만 의존해야 한다")
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
            .because("domain testFixtures는 domain만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: application testFixtures는 application + domain testFixtures 의존
     */
    @Test
    @DisplayName("[필수] application testFixtures는 application과 domain testFixtures만 의존해야 한다")
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
            .because("application testFixtures는 application과 domain testFixtures만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: domain testFixtures는 application testFixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] domain testFixtures는 application testFixtures를 의존할 수 없다")
    void domainTestFixtures_MustNotDependOnApplicationTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.application..")
            .because("domain testFixtures는 application testFixtures를 의존할 수 없습니다 (역방향 의존 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 4: application testFixtures는 adapter testFixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] application testFixtures는 adapter testFixtures를 의존할 수 없다")
    void applicationTestFixtures_MustNotDependOnAdapterTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter..")
            .because("application testFixtures는 adapter testFixtures를 의존할 수 없습니다 (역방향 의존 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 5: adapter-in testFixtures는 adapter-out testFixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-in testFixtures는 adapter-out testFixtures를 의존할 수 없다")
    void adapterInTestFixtures_MustNotDependOnAdapterOutTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter.out..")
            .because("adapter-in testFixtures는 adapter-out testFixtures를 의존할 수 없습니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: adapter-out testFixtures는 adapter-in testFixtures 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-out testFixtures는 adapter-in testFixtures를 의존할 수 없다")
    void adapterOutTestFixtures_MustNotDependOnAdapterInTestFixtures() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..fixture.adapter.out..")
            .should().dependOnClassesThat()
            .resideInAPackage("..fixture.adapter.in..")
            .because("adapter-out testFixtures는 adapter-in testFixtures를 의존할 수 없습니다");

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
     * 규칙 8: Fixture 클래스 네이밍 규칙
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
     * 규칙 9: Fixture 패키지 위치
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

| From ↓ / To → | domain testFixtures | application testFixtures | adapter-* testFixtures |
|---------------|---------------------|--------------------------|------------------------|
| **domain testFixtures** | - | ❌ (규칙 3) | ❌ (규칙 3) |
| **application testFixtures** | ✅ (규칙 2) | - | ❌ (규칙 4) |
| **adapter-in testFixtures** | ✅ | ✅ | ❌ (규칙 5) |
| **adapter-out testFixtures** | ✅ | ❌ | ❌ (규칙 6) |

---

## 4️⃣ 빌드 시 자동 검증

### build.gradle 설정

```gradle
// application/build.gradle

dependencies {
    // ArchUnit
    testImplementation libs.archunit.junit5
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
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
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

### 예시 1: domain testFixtures가 application testFixtures 의존

**❌ Bad**:
```java
// domain/src/testFixtures/java/.../OrderFixture.java
package com.ryuqq.fixture.domain;

import com.ryuqq.fixture.application.command.PlaceOrderCommandFixture;  // ❌

public final class OrderFixture {
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
// domain/src/testFixtures/java/.../OrderFixture.java
package com.ryuqq.fixture.domain;

public final class OrderFixture {

    private OrderFixture() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    public static Order defaultOrder() {  // ✅ Domain 객체만 생성
        return Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );
    }
}
```

### 예시 2: Fixture 클래스가 'Fixture' 접미사 누락

**❌ Bad**:
```java
// domain/src/testFixtures/java/.../OrderFactory.java
package com.ryuqq.fixture.domain;

public final class OrderFactory {  // ❌ 'Fixture' 접미사 누락
    public static Order create() { ... }
}
```

**ArchUnit 실패 메시지**:
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in a package '..fixture..' should have simple name ending with 'Fixture'' was violated (1 times):
Class <com.ryuqq.fixture.domain.OrderFactory> does not have simple name ending with 'Fixture'
```

**✅ Good**:
```java
// domain/src/testFixtures/java/.../OrderFixture.java
package com.ryuqq.fixture.domain;

public final class OrderFixture {  // ✅ 'Fixture' 접미사 사용
    public static Order create() { ... }
}
```

---

## 7️⃣ Fixture 클래스 구조 권장 사항

### 권장 패턴

```java
package com.ryuqq.fixture.domain;

/**
 * Order Domain 객체 Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public final class OrderFixture {  // ✅ final 클래스

    // ✅ private 생성자 (인스턴스 생성 방지)
    private OrderFixture() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    // ✅ static 메서드만 사용
    public static Order defaultNewOrder() {
        return Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );
    }

    public static Order defaultExistingOrder() {
        return Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000)),
            OrderStatus.PLACED
        );
    }

    // ✅ 커스텀 빌더 메서드
    public static Order customOrder(Long id, BigDecimal amount, OrderStatus status) {
        return Order.forExisting(
            OrderId.of(id),
            Money.of(amount),
            status
        );
    }
}
```

---

## 8️⃣ 체크리스트

ArchUnit 검증 구현 시:
- [ ] `TestFixturesArchTest.java` 파일 생성
- [ ] 9개 규칙 모두 구현
- [ ] build.gradle에 ArchUnit 의존성 추가 (`libs.archunit.junit5`)
- [ ] `@Tag("architecture")`, `@Tag("test-fixtures")` 적용
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 위반 시 빌드 실패 확인
- [ ] 팀 전체 규칙 공유

---

## 📖 관련 문서

- **[Test Fixtures Guide](./01_test-fixtures-guide.md)** - Gradle testFixtures 전체 가이드
- **[Integration Testing Overview](../integration-testing/01_integration-testing-overview.md)** - 통합 테스트 가이드
- **[Domain Layer ArchUnit](../../02-domain-layer/aggregate/aggregate-archunit.md)** - Domain 아키텍처 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 2.0.0
