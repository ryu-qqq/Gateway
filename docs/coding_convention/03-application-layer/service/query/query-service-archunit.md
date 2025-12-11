# Query Service ArchUnit — **자동 검증 규칙**

> QueryService의 **아키텍처 규칙을 ArchUnit으로 자동 검증**합니다.
>
> 빌드 시 규칙 위반이 감지되면 **빌드 실패**로 강제합니다.

---

## 1) 검증 규칙 요약

| 카테고리 | 규칙 | 심각도 |
|---------|------|--------|
| **기본 구조** | @Service 어노테이션 필수 | 필수 |
| **기본 구조** | UseCase 인터페이스 구현 필수 | 필수 |
| **기본 구조** | *Service 접미사 필수 | 필수 |
| **기본 구조** | service.query 패키지 위치 | 필수 |
| **기본 구조** | final 클래스 금지 | 필수 |
| **금지** | @Transactional 금지 | 필수 |
| **금지** | @Component 금지 | 필수 |
| **금지** | Port 직접 의존 금지 | 필수 |
| **금지** | Repository 직접 의존 금지 | 필수 |
| **금지** | Lombok 금지 | 필수 |
| **의존성** | Application/Domain Layer만 의존 | 필수 |

---

## 2) ArchUnit 테스트 코드

### 파일 위치

```
application/src/test/java/
└─ com/ryuqq/application/architecture/service/
   └─ QueryServiceArchTest.java
```

### 전체 코드

```java
package com.ryuqq.application.architecture.service;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * QueryService ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: QueryService는 UseCase 구현체로 조율만 수행, 읽기 전용</p>
 */
@DisplayName("QueryService ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("service")
class QueryServiceArchTest {

    private static JavaClasses classes;
    private static boolean hasQueryServiceClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");

        hasQueryServiceClasses = classes.stream()
            .anyMatch(javaClass ->
                javaClass.getPackageName().contains("service.query") &&
                javaClass.getSimpleName().endsWith("Service"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] service.query 패키지의 Service는 @Service 어노테이션을 가져야 한다")
        void queryService_MustHaveServiceAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(Service.class)
                .because("QueryService는 @Service 어노테이션을 가져야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] service.query 패키지의 Service는 UseCase 인터페이스를 구현해야 한다")
        void queryService_MustImplementUseCase() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "UseCase interface",
                    javaClass -> javaClass.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getSimpleName().endsWith("UseCase"))
                ))
                .because("QueryService는 UseCase 인터페이스를 구현해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] service.query 패키지의 클래스는 'Service' 접미사를 가져야 한다")
        void queryService_MustHaveCorrectSuffix() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().areNotInterfaces()
                .and().areNotAnonymousClasses()
                .should().haveSimpleNameEndingWith("Service")
                .because("Query Service는 'Service' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 ..application..service.query.. 패키지에 위치해야 한다")
        void queryService_MustBeInCorrectPackage() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            // service.query 패키지 외부에 있는 Service 클래스 중
            // query 관련 UseCase를 구현한 클래스가 없어야 함
            ArchRule rule = noClasses()
                .that().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "Query UseCase interface",
                    javaClass -> javaClass.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getPackageName().contains("port.in.query"))
                ))
                .should().resideOutsideOfPackage("..application..service.query..")
                .because("Query UseCase 구현체는 service.query 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 final 클래스가 아니어야 한다")
        void queryService_MustNotBeFinal() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().notHaveModifier(FINAL)
                .because("Spring 프록시 생성을 위해 QueryService가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] QueryService는 @Transactional을 가지지 않아야 한다")
        void queryService_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("QueryService는 @Transactional을 가지지 않아야 합니다. " +
                         "읽기 전용이므로 트랜잭션이 필요하지 않습니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 @Component 어노테이션을 가지지 않아야 한다")
        void queryService_MustNotHaveComponentAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("org.springframework.stereotype.Component")
                .because("QueryService는 @Component가 아닌 @Service를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Port 인터페이스를 직접 의존하지 않아야 한다")
        void queryService_MustNotDependOnPorts() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveNameMatching(".*Port")
                .andShould().dependOnClassesThat().resideInAPackage("..port.out..")
                .because("QueryService는 Port를 직접 의존하지 않아야 합니다. " +
                         "ReadManager/QueryFacade를 통해 접근합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Repository를 직접 의존하지 않아야 한다")
        void queryService_MustNotDependOnRepositories() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("QueryService는 Repository를 직접 의존하지 않아야 합니다. " +
                         "ReadManager/QueryFacade를 통해 접근합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Lombok 어노테이션을 가지지 않아야 한다")
        void queryService_MustNotUseLombok() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .because("QueryService는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Command 관련 클래스를 의존하지 않아야 한다")
        void queryService_MustNotDependOnCommandClasses() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveNameMatching(".*Command.*")
                .because("QueryService는 Command 관련 클래스를 의존하지 않아야 합니다. " +
                         "CQRS 분리 원칙을 준수해야 합니다.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] QueryService는 Application Layer와 Domain Layer만 의존해야 한다")
        void queryService_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.ryuqq.application..",
                    "com.ryuqq.domain..",
                    "org.springframework..",
                    "java..",
                    "jakarta.."
                )
                .because("QueryService는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 Adapter Layer를 의존하지 않아야 한다")
        void queryService_MustNotDependOnAdapterLayer() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("QueryService는 Adapter Layer를 의존하지 않아야 합니다");

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
| `@Service` 필수 | Spring Bean 등록 | 빌드 실패 |
| UseCase 구현 | Port-In 인터페이스 | 빌드 실패 |
| `*Service` 접미사 | 일관된 네이밍 | 빌드 실패 |
| `service.query` 패키지 | 올바른 위치 | 빌드 실패 |
| `final` 금지 | 프록시 생성 | 빌드 실패 |

### 금지 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| `@Transactional` 금지 | 읽기 전용, 불필요 | 빌드 실패 |
| `@Component` 금지 | `@Service` 사용 | 빌드 실패 |
| `Port` 직접 의존 금지 | ReadManager 통해 접근 | 빌드 실패 |
| `Repository` 직접 의존 금지 | ReadManager 통해 접근 | 빌드 실패 |
| `Lombok` 금지 | Plain Java | 빌드 실패 |
| `Command` 의존 금지 | CQRS 분리 | 빌드 실패 |

### 의존성 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| Layer 제한 | 아키텍처 준수 | 빌드 실패 |
| Adapter 의존 금지 | 헥사고날 아키텍처 | 빌드 실패 |

---

## 4) CommandService vs QueryService 비교

| 구분 | CommandService | QueryService |
|------|----------------|--------------|
| **역할** | 상태 변경 (CUD) | 조회 (R) |
| **패키지** | `service.command` | `service.query` |
| **UseCase** | Command UseCase | Query UseCase |
| **Factory** | CommandFactory | QueryFactory |
| **Manager** | TransactionManager | ReadManager |
| **Facade** | Facade (persist*) | QueryFacade (fetch*) |
| **트랜잭션** | Manager/Facade에 위임 | 불필요 |
| **CQRS 분리** | Command만 의존 | Query만 의존 |

---

## 5) 관련 문서

- **[QueryService Guide](./query-service-guide.md)** - Service 구현 가이드
- **[QueryService Test Guide](./query-service-test-guide.md)** - 테스트 가이드
- **[CommandService ArchUnit](../command/command-service-archunit.md)** - Command Service 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
