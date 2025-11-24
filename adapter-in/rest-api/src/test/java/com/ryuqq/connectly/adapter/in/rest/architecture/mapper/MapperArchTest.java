package com.ryuqq.connectly.adapter.in.rest.architecture.mapper;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Mapper ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 Mapper는 정확히 이 규칙을 따라야 합니다.
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>✅ @Component 어노테이션 필수
 *   <li>✅ *ApiMapper 네이밍 규칙
 *   <li>❌ Lombok 어노테이션 절대 금지
 *   <li>❌ Static 메서드 절대 금지
 *   <li>❌ Domain 객체 직접 사용 금지
 *   <li>❌ 비즈니스 로직 메서드 금지
 *   <li>❌ Port 의존성 주입 금지
 *   <li>✅ 올바른 패키지 위치
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Mapper ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class MapperArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.adapter.in.rest");
    }

    /** 규칙 1: @Component 어노테이션 필수 */
    @Test
    @DisplayName("[필수] Mapper는 @Component 어노테이션을 가져야 한다")
    void mapper_MustHaveComponentAnnotation() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..mapper..")
                        .and()
                        .haveSimpleNameEndingWith("ApiMapper")
                        .should()
                        .beAnnotatedWith("org.springframework.stereotype.Component")
                        .because("Mapper는 @Component로 Bean 등록되어야 하며 Static 메서드는 금지됩니다");

        rule.check(classes);
    }

    /** 규칙 2: 네이밍 규칙 (*ApiMapper) */
    @Test
    @DisplayName("[필수] Mapper는 *ApiMapper 접미사를 가져야 한다")
    void mapper_MustHaveApiMapperSuffix() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..mapper..")
                        .and()
                        .areAnnotatedWith("org.springframework.stereotype.Component")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .haveSimpleNameEndingWith("ApiMapper")
                        .because("Mapper는 *ApiMapper 네이밍 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /** 규칙 3: Lombok 어노테이션 절대 금지 */
    @Test
    @DisplayName("[금지] Mapper는 Lombok 어노테이션을 가지지 않아야 한다")
    void mapper_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
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
                        .because("Mapper는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /** 규칙 4: Static 메서드 절대 금지 */
    @Test
    @DisplayName("[금지] Mapper는 Public Static 메서드를 가지지 않아야 한다")
    void mapper_MustNotHavePublicStaticMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..mapper..")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("ApiMapper")
                        .and()
                        .arePublic()
                        .should()
                        .notBeStatic()
                        .because("Mapper는 @Component Bean이므로 Public Static 메서드는 금지됩니다");

        rule.check(classes);
    }

    /** 규칙 5: Domain 객체 직접 사용 금지 (ErrorMapper 제외) */
    @Test
    @DisplayName("[금지] Mapper는 Domain 객체를 직접 사용하지 않아야 한다 (ErrorMapper 제외)")
    void mapper_MustNotUseDomainObjects() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
                        .and()
                        .areNotNestedClasses()
                        .and()
                        .haveSimpleNameNotContaining("Error")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..domain..")
                        .because(
                                "Mapper는 Application DTO만 사용하며 Domain 직접 의존은 금지됩니다 (ErrorMapper는"
                                        + " 예외)");

        rule.check(classes);
    }

    /** 규칙 6: 비즈니스 로직 메서드 금지 */
    @Test
    @DisplayName("[금지] Mapper는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void mapper_MustNotHaveBusinessLogicMethods() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..mapper..")
                        .and()
                        .haveNameMatching(
                                "calculate|compute|validate|isValid|check|process|execute")
                        .should()
                        .beDeclaredInClassesThat()
                        .resideInAPackage("..mapper..")
                        .allowEmptyShould(true)
                        .because("Mapper는 필드 매핑만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /** 규칙 7: Port 의존성 주입 금지 */
    @Test
    @DisplayName("[금지] Mapper는 Port 의존성을 주입받지 않아야 한다")
    void mapper_MustNotDependOnPorts() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..port.in..")
                        .orShould()
                        .dependOnClassesThat()
                        .resideInAPackage("..port.out..")
                        .because("Mapper는 UseCase/Repository를 주입받지 않으며 Controller가 주입합니다");

        rule.check(classes);
    }

    /** 규칙 8: 패키지 위치 검증 */
    @Test
    @DisplayName("[필수] Mapper는 올바른 패키지에 위치해야 한다")
    void mapper_MustBeInCorrectPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("ApiMapper")
                        .and()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .resideInAPackage("..mapper..")
                        .because("Mapper는 mapper 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /** 규칙 9: @Service, @Repository 어노테이션 금지 */
    @Test
    @DisplayName("[금지] Mapper는 @Service/@Repository 어노테이션을 가지지 않아야 한다")
    void mapper_MustNotUseServiceOrRepository() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
                        .should()
                        .beAnnotatedWith("org.springframework.stereotype.Service")
                        .orShould()
                        .beAnnotatedWith("org.springframework.stereotype.Repository")
                        .because("Mapper는 @Component 어노테이션만 사용해야 합니다");

        rule.check(classes);
    }

    /** 규칙 10: @Transactional 어노테이션 금지 */
    @Test
    @DisplayName("[금지] Mapper는 @Transactional 어노테이션을 가지지 않아야 한다")
    void mapper_MustNotUseTransactional() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
                        .should()
                        .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                        .because("Mapper는 변환만 담당하며 Transaction은 UseCase 책임입니다");

        rule.check(classes);
    }
}
