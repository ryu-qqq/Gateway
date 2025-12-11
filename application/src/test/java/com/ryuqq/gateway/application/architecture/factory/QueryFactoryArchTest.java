package com.ryuqq.gateway.application.architecture.factory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

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
