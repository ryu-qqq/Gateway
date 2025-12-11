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
 * CommandFactory ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: CommandFactory는 Command → Domain 순수 변환만, 비즈니스 로직/조회 금지
 */
@DisplayName("CommandFactory ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("factory")
class CommandFactoryArchTest {

    private static JavaClasses classes;
    private static boolean hasCommandFactoryClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        hasCommandFactoryClasses =
                classes.stream()
                        .anyMatch(
                                javaClass -> javaClass.getSimpleName().endsWith("CommandFactory"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] CommandFactory는 @Component 어노테이션을 가져야 한다")
        void commandFactory_MustHaveComponentAnnotation() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("CommandFactory는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] factory.command 패키지의 Factory 클래스는 'CommandFactory' 접미사를 가져야 한다")
        void factory_MustHaveCorrectSuffix() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..application..factory.command..")
                            .and()
                            .haveSimpleNameContaining("Factory")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .because("Command Factory는 'CommandFactory' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] CommandFactory는 ..application..factory.command.. 패키지에 위치해야 한다")
        void commandFactory_MustBeInCorrectPackage() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .resideInAPackage("..application..factory.command..")
                            .because(
                                    "CommandFactory는 application.{bc}.factory.command 패키지에 위치해야"
                                            + " 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] CommandFactory는 final 클래스가 아니어야 한다")
        void commandFactory_MustNotBeFinal() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .notHaveModifier(FINAL)
                            .because("Spring 프록시 생성을 위해 CommandFactory가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 메서드 규칙 ====================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[권장] CommandFactory의 public 메서드는 create로 시작해야 한다")
        void commandFactory_MethodsShouldStartWithCreate() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    methods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .and()
                            .arePublic()
                            .and()
                            .doNotHaveFullName(".*<init>.*")
                            .should()
                            .haveNameMatching("create.*")
                            .because("CommandFactory 메서드는 create*() 또는 createBundle*() 네이밍을 권장합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] CommandFactory는 @Service 어노테이션을 가지지 않아야 한다")
        void commandFactory_MustNotHaveServiceAnnotation() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .beAnnotatedWith("org.springframework.stereotype.Service")
                            .because("CommandFactory는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] CommandFactory는 @Transactional을 가지지 않아야 한다")
        void commandFactory_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .beAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .because(
                                    "CommandFactory는 @Transactional을 가지지 않아야 합니다. "
                                            + "트랜잭션 경계는 Service 책임입니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] CommandFactory는 Lombok 어노테이션을 가지지 않아야 한다")
        void commandFactory_MustNotUseLombok() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
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
                            .because("CommandFactory는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] CommandFactory는 Port 인터페이스를 의존하지 않아야 한다")
        void commandFactory_MustNotDependOnPorts() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Port")
                            .because(
                                    "CommandFactory는 Port를 주입받지 않아야 합니다. " + "조회 없이 순수 변환만 수행합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] CommandFactory는 Repository를 의존하지 않아야 한다")
        void commandFactory_MustNotDependOnRepositories() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Repository")
                            .because(
                                    "CommandFactory는 Repository를 주입받지 않아야 합니다. "
                                            + "조회 없이 순수 변환만 수행합니다.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] CommandFactory는 Application Layer와 Domain Layer만 의존해야 한다")
        void commandFactory_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasCommandFactoryClasses, "CommandFactory 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CommandFactory")
                            .should()
                            .onlyAccessClassesThat()
                            .resideInAnyPackage(
                                    "com.ryuqq.application..",
                                    "com.ryuqq.domain..",
                                    "org.springframework..",
                                    "java..",
                                    "jakarta..")
                            .because("CommandFactory는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }
    }
}
