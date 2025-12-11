package com.ryuqq.gateway.application.architecture.event;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * TransactionEventRegistry ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Registry는 커밋 후 Event 발행을 보장, ThreadLocal 금지
 */
@DisplayName("TransactionEventRegistry ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("event")
class TransactionEventRegistryArchTest {

    private static JavaClasses classes;
    private static boolean hasEventRegistryClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        hasEventRegistryClasses =
                classes.stream()
                        .anyMatch(javaClass -> javaClass.getSimpleName().endsWith("EventRegistry"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] EventRegistry는 @Component 어노테이션을 가져야 한다")
        void eventRegistry_MustHaveComponentAnnotation() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("EventRegistry는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] config 패키지의 Registry 클래스는 'EventRegistry' 접미사를 가져야 한다")
        void config_MustHaveCorrectSuffix() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..application..config..")
                            .and()
                            .haveSimpleNameContaining("Event")
                            .and()
                            .haveSimpleNameContaining("Registry")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .because("Registry는 'EventRegistry' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventRegistry는 ..application..config.. 패키지에 위치해야 한다")
        void eventRegistry_MustBeInCorrectPackage() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .resideInAPackage("..application..config..")
                            .because("EventRegistry는 application.common.config 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventRegistry는 final 클래스가 아니어야 한다")
        void eventRegistry_MustNotBeFinal() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .notHaveModifier(FINAL)
                            .because("Spring 프록시 생성을 위해 EventRegistry가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] EventRegistry는 ApplicationEventPublisher를 의존해야 한다")
        void eventRegistry_MustDependOnApplicationEventPublisher() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .dependOnClassesThat()
                            .areAssignableTo(ApplicationEventPublisher.class)
                            .because("EventRegistry는 ApplicationEventPublisher로 Event를 발행해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventRegistry는 Application/Domain Layer만 의존해야 한다")
        void eventRegistry_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .onlyAccessClassesThat()
                            .resideInAnyPackage(
                                    "com.ryuqq.application..",
                                    "com.ryuqq.domain..",
                                    "org.springframework..",
                                    "java..",
                                    "jakarta..")
                            .because("EventRegistry는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] EventRegistry는 ThreadLocal을 사용하지 않아야 한다")
        void eventRegistry_MustNotUseThreadLocal() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .dependOnClassesThat()
                            .areAssignableTo(ThreadLocal.class)
                            .because(
                                    "ThreadLocal은 Virtual Thread에서 문제가 발생합니다. "
                                            + "TransactionSynchronizationManager를 사용하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventRegistry는 @Transactional을 가지지 않아야 한다")
        void eventRegistry_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .beAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .because(
                                    "EventRegistry는 트랜잭션을 열지 않습니다. " + "호출자(Facade)가 트랜잭션을 관리합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventRegistry는 Lombok 어노테이션을 가지지 않아야 한다")
        void eventRegistry_MustNotUseLombok() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .beAnnotatedWith("lombok.Data")
                            .orShould()
                            .beAnnotatedWith("lombok.Builder")
                            .orShould()
                            .beAnnotatedWith("lombok.Getter")
                            .orShould()
                            .beAnnotatedWith("lombok.Setter")
                            .orShould()
                            .beAnnotatedWith("lombok.AllArgsConstructor")
                            .orShould()
                            .beAnnotatedWith("lombok.NoArgsConstructor")
                            .orShould()
                            .beAnnotatedWith("lombok.RequiredArgsConstructor")
                            .because("EventRegistry는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventRegistry는 @Service 어노테이션을 가지지 않아야 한다")
        void eventRegistry_MustNotHaveServiceAnnotation() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .should()
                            .beAnnotatedWith("org.springframework.stereotype.Service")
                            .because("EventRegistry는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 필드 규칙 ====================

    @Nested
    @DisplayName("필드 규칙")
    class FieldRules {

        @Test
        @DisplayName("[권장] EventRegistry 필드는 final이어야 한다")
        void eventRegistry_FieldsShouldBeFinal() {
            assumeTrue(hasEventRegistryClasses, "EventRegistry 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    fields().that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .and()
                            .areNotStatic()
                            .should()
                            .beFinal()
                            .because("EventRegistry는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

            rule.check(classes);
        }
    }
}
