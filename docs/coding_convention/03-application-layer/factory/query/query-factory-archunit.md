# Query Factory ArchUnit — **자동 검증 규칙**

> QueryFactory의 **아키텍처 규칙을 ArchUnit으로 자동 검증**합니다.
>
> 빌드 시 규칙 위반이 감지되면 **빌드 실패**로 강제합니다.

---

## 1) 검증 규칙 요약

| 카테고리 | 규칙 | 심각도 |
|---------|------|--------|
| **기본 구조** | @Component 어노테이션 필수 | 필수 |
| **기본 구조** | *QueryFactory 접미사 필수 | 필수 |
| **기본 구조** | factory.query 패키지 위치 | 필수 |
| **기본 구조** | final 클래스 금지 | 필수 |
| **메서드** | createCriteria* 메서드 네이밍 | 권장 |
| **금지** | @Transactional 금지 | 필수 |
| **금지** | Port 의존 금지 | 필수 |
| **금지** | Lombok 금지 | 필수 |
| **금지** | @Service 금지 | 필수 |
| **의존성** | Application/Domain Layer만 의존 | 필수 |

---

## 2) ArchUnit 테스트 코드

### 파일 위치

```
application/src/test/java/
└─ com/ryuqq/application/architecture/factory/
   └─ QueryFactoryArchTest.java
```

### 전체 코드

```java
package com.ryuqq.application.architecture.factory;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * QueryFactory ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: QueryFactory는 Query → Criteria 순수 변환만, 비즈니스 로직/조회 금지</p>
 */
@DisplayName("QueryFactory ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("factory")
class QueryFactoryArchTest {

    private static JavaClasses classes;
    private static boolean hasQueryFactoryClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");

        hasQueryFactoryClasses = classes.stream()
            .anyMatch(javaClass -> javaClass.getSimpleName().endsWith("QueryFactory"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] QueryFactory는 @Component 어노테이션을 가져야 한다")
        void queryFactory_MustHaveComponentAnnotation() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().beAnnotatedWith(Component.class)
                .because("QueryFactory는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] factory.query 패키지의 Factory 클래스는 'QueryFactory' 접미사를 가져야 한다")
        void factory_MustHaveCorrectSuffix() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..factory.query..")
                .and().haveSimpleNameContaining("Factory")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("QueryFactory")
                .because("Query Factory는 'QueryFactory' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryFactory는 ..application..factory.query.. 패키지에 위치해야 한다")
        void queryFactory_MustBeInCorrectPackage() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().resideInAPackage("..application..factory.query..")
                .because("QueryFactory는 application.{bc}.factory.query 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryFactory는 final 클래스가 아니어야 한다")
        void queryFactory_MustNotBeFinal() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().notHaveModifier(FINAL)
                .because("Spring 프록시 생성을 위해 QueryFactory가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 메서드 규칙 ====================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[권장] QueryFactory의 public 메서드는 createCriteria로 시작해야 한다")
        void queryFactory_MethodsShouldStartWithCreateCriteria() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryFactory")
                .and().arePublic()
                .and().doNotHaveFullName(".*<init>.*")
                .should().haveNameMatching("createCriteria.*|create.*")
                .because("QueryFactory 메서드는 createCriteria*() 네이밍을 권장합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] QueryFactory는 @Service 어노테이션을 가지지 않아야 한다")
        void queryFactory_MustNotHaveServiceAnnotation() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("QueryFactory는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryFactory는 @Transactional을 가지지 않아야 한다")
        void queryFactory_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("QueryFactory는 @Transactional을 가지지 않아야 합니다. " +
                         "트랜잭션 경계는 Service 책임입니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryFactory는 Lombok 어노테이션을 가지지 않아야 한다")
        void queryFactory_MustNotUseLombok() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .because("QueryFactory는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryFactory는 Port 인터페이스를 의존하지 않아야 한다")
        void queryFactory_MustNotDependOnPorts() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().dependOnClassesThat().haveNameMatching(".*Port")
                .because("QueryFactory는 Port를 주입받지 않아야 합니다. " +
                         "조회 없이 순수 변환만 수행합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryFactory는 Repository를 의존하지 않아야 한다")
        void queryFactory_MustNotDependOnRepositories() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("QueryFactory는 Repository를 주입받지 않아야 합니다. " +
                         "조회 없이 순수 변환만 수행합니다.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] QueryFactory는 Application Layer와 Domain Layer만 의존해야 한다")
        void queryFactory_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasQueryFactoryClasses, "QueryFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("QueryFactory")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.ryuqq.application..",
                    "com.ryuqq.domain..",
                    "org.springframework..",
                    "java..",
                    "jakarta.."
                )
                .because("QueryFactory는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }
    }
}
```

---

## 3) 규칙 상세 설명

### 기본 구조 규칙

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `@Component` 필수 | Spring Bean 등록 | 빌드 실패 |
| `*QueryFactory` 접미사 | 일관된 네이밍 | 빌드 실패 |
| `factory.query` 패키지 | 올바른 위치 | 빌드 실패 |
| `final` 금지 | 프록시 생성 | 빌드 실패 |

### 메서드 규칙

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `createCriteria*` 네이밍 | 일관된 메서드명 | 경고 |

### 금지 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| `@Service` 금지 | `@Component` 사용 | 빌드 실패 |
| `@Transactional` 금지 | Service 책임 | 빌드 실패 |
| `Lombok` 금지 | Plain Java | 빌드 실패 |
| `Port` 의존 금지 | 순수 변환 | 빌드 실패 |
| `Repository` 의존 금지 | 순수 변환 | 빌드 실패 |

### 의존성 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| Layer 제한 | 아키텍처 준수 | 빌드 실패 |

---

## 4) CommandFactory vs QueryFactory 비교

| 구분 | CommandFactory | QueryFactory |
|------|----------------|--------------|
| **역할** | Command → Domain | Query → Criteria |
| **접미사** | `*CommandFactory` | `*QueryFactory` |
| **패키지** | `factory.command` | `factory.query` |
| **메서드** | `create*()`, `createBundle*()` | `createCriteria*()` |
| **출력** | Domain, PersistBundle | Criteria |

---

## 5) 관련 문서

- **[Query Factory Guide](./query-factory-guide.md)** - Factory 구현 가이드
- **[Query Factory Test Guide](./query-factory-test-guide.md)** - 테스트 가이드
- **[Command Factory ArchUnit](../command/command-factory-archunit.md)** - Command Factory 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
