package com.ryuqq.gateway.application.architecture.assembler;

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
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Assembler ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Assembler는 Domain → Response 변환만 담당
 *
 * <p>toResponse() 메서드만 허용 (toDomain, toCriteria 등 자동 금지)
 *
 * <p>업계 레퍼런스:
 *
 * <ul>
 *   <li>Sairyss/domain-driven-hexagon (12k+ stars)
 *   <li>Martin Fowler - Patterns of Enterprise Application Architecture
 * </ul>
 *
 * @see <a href="https://github.com/Sairyss/domain-driven-hexagon">domain-driven-hexagon</a>
 */
@DisplayName("Assembler ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("assembler")
class AssemblerArchTest {

    private static JavaClasses classes;
    private static boolean hasAssemblerClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.fileflow.application");

        hasAssemblerClasses =
                classes.stream()
                        .anyMatch(javaClass -> javaClass.getSimpleName().endsWith("Assembler"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] Assembler는 @Component 어노테이션을 가져야 한다")
        void assembler_MustHaveComponentAnnotation() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("Assembler는 Spring Bean으로 등록되어야 합니다 (테스트 용이성)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] assembler 패키지의 클래스는 'Assembler' 접미사를 가져야 한다")
        void assembler_MustHaveCorrectSuffix() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..application..service.assembler..")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotEnums()
                            .should()
                            .haveSimpleNameEndingWith("Assembler")
                            .because("Assembler는 'Assembler' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Assembler는 ..application..service.assembler.. 패키지에 위치해야 한다")
        void assembler_MustBeInCorrectPackage() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .resideInAPackage("..application..service.assembler..")
                            .because(
                                    "Assembler는 application.{bc}.service.assembler 패키지에 위치해야 합니다"
                                            + " (APP-AS-007)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Assembler는 public 클래스여야 한다")
        void assembler_MustBePublic() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .bePublic()
                            .because("Assembler는 Spring Bean으로 등록되기 위해 public이어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Assembler는 final 클래스가 아니어야 한다")
        void assembler_MustNotBeFinal() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .notHaveModifier(FINAL)
                            .because("Spring은 프록시 생성을 위해 Assembler가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] Assembler는 Lombok 어노테이션을 가지지 않아야 한다")
        void assembler_MustNotUseLombok() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
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
                            .orShould()
                            .beAnnotatedWith("lombok.Value")
                            .because("Assembler는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Assembler는 public static 메서드를 가지지 않아야 한다")
        void assembler_MustNotHavePublicStaticMethods() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .bePublic()
                            .andShould()
                            .beStatic()
                            .because("Assembler는 Bean으로 등록하여 테스트 용이성을 확보해야 합니다 (Static 메서드 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Assembler는 @Transactional을 가지지 않아야 한다")
        void assembler_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .beAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .because("@Transactional은 UseCase에서만 사용해야 합니다 (Assembler는 변환만)");

            rule.check(classes);
        }
    }

    // ==================== 핵심 규칙: toResponse만 허용 ====================

    @Nested
    @DisplayName("핵심 규칙: toResponse 메서드만 허용")
    class MethodNamingRules {

        /**
         * 핵심 규칙: toResponse* 만 허용
         *
         * <p>이 규칙 하나로 toDomain, toCriteria, from* 등 모든 금지 규칙이 자동 적용됩니다.
         *
         * <p>업계 표준:
         *
         * <ul>
         *   <li>Sairyss/domain-driven-hexagon: Mapper에 toResponse만 존재
         *   <li>Martin Fowler: "Assembler maps Aggregate to DTO"
         * </ul>
         */
        @Test
        @DisplayName("[필수-핵심] Assembler 메서드명은 toResponse로 시작해야 한다")
        void assembler_MethodsMustStartWithToResponse() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    methods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Assembler")
                            .and()
                            .arePublic()
                            .and()
                            .doNotHaveFullName(".*<init>.*") // 생성자 제외
                            .should()
                            .haveNameMatching("toResponse.*")
                            .because(
                                    "Assembler는 Domain → Response 변환만 담당합니다. "
                                            + "toResponse(), toResponseList() 형태만 허용됩니다. "
                                            + "(toDomain, toCriteria, from* 등은 이 규칙으로 자동 금지)");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[금지] Assembler는 Port 인터페이스를 의존하지 않아야 한다")
        void assembler_MustNotDependOnPorts() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Port")
                            .because(
                                    "Assembler는 Port를 주입받지 않아야 합니다. "
                                            + "DB 조회가 필요하면 Creator 패턴을 사용하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Assembler는 Repository를 의존하지 않아야 한다")
        void assembler_MustNotDependOnRepositories() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Repository")
                            .because(
                                    "Assembler는 Repository를 주입받지 않아야 합니다. "
                                            + "DB 조회가 필요하면 Creator 패턴을 사용하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Assembler는 Spring Data Page/Slice를 사용하지 않아야 한다")
        void assembler_MustNotUseSpringDataPageable() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName("org.springframework.data.domain.Page")
                            .orShould()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName("org.springframework.data.domain.Slice")
                            .orShould()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName("org.springframework.data.domain.Pageable")
                            .because(
                                    "Assembler는 Spring Data 대신 custom PageResponse를 사용해야 합니다. "
                                            + "PageResponse 조립은 UseCase에서 처리하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Assembler는 Application Layer와 Domain Layer만 의존해야 한다")
        void assembler_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .onlyAccessClassesThat()
                            .resideInAnyPackage(
                                    "com.ryuqq.fileflow.application..",
                                    "com.ryuqq.fileflow.domain..",
                                    "org.springframework..",
                                    "java..",
                                    "jakarta..")
                            .because(
                                    "Assembler는 Application Layer와 Domain Layer만 의존해야 합니다. "
                                            + "Persistence Layer, API Layer 의존 금지.");

            rule.check(classes);
        }
    }

    // ==================== 비즈니스 로직 금지 규칙 ====================

    @Nested
    @DisplayName("비즈니스 로직 금지 규칙")
    class BusinessLogicProhibitionRules {

        @Test
        @DisplayName("[금지] Assembler는 PageResponse/SliceResponse를 반환하지 않아야 한다")
        void assembler_MustNotReturnPageOrSliceResponse() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Assembler")
                            .and()
                            .arePublic()
                            .should()
                            .haveRawReturnType(".*PageResponse")
                            .orShould()
                            .haveRawReturnType(".*SliceResponse")
                            .because(
                                    "PageResponse/SliceResponse 조립은 UseCase에서 처리해야 합니다. "
                                            + "Assembler는 List<XxxResponse> 변환만 담당합니다.");

            rule.check(classes);
        }
    }

    // ==================== 필드 규칙 ====================

    @Nested
    @DisplayName("필드 규칙")
    class FieldRules {

        @Test
        @DisplayName("[권장] Assembler 필드는 final이어야 한다")
        void assembler_FieldsShouldBeFinal() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    fields().that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Assembler")
                            .and()
                            .areNotStatic()
                            .should()
                            .beFinal()
                            .allowEmptyShould(true)
                            .because("Assembler는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[권장] Assembler의 필드명은 소문자로 시작해야 한다")
        void assembler_FieldsShouldStartWithLowercase() {
            assumeTrue(hasAssemblerClasses, "Assembler 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    fields().that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Assembler")
                            .and()
                            .areNotStatic()
                            .should()
                            .haveNameMatching("[a-z].*")
                            .allowEmptyShould(true)
                            .because("필드명은 camelCase 규칙을 따라야 합니다");

            rule.check(classes);
        }
    }
}
