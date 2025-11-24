package com.ryuqq.connectly.adapter.in.rest.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * REST API Layer ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>REST API Layer 전체 아키텍처 및 레이어 분리를 검증합니다.
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: Package 구조 검증 (controller, dto, mapper, error)
 *   <li>규칙 2: Bounded Context별 패키지 구조 검증
 *   <li>규칙 3: Common 패키지 구조 검증
 *   <li>규칙 4: DTO 패키지 분리 (command/query/response)
 *   <li>규칙 5: REST API Layer는 Application Layer Port 의존 필수
 *   <li>규칙 6: REST API Layer는 Domain Layer 직접 의존 금지
 *   <li>규칙 7: ErrorMapper만 Domain Exception 의존 허용
 *   <li>규칙 8: Controller는 Domain 객체 반환 금지
 *   <li>규칙 9: Mapper는 Domain 직접 의존 금지 (ErrorMapper 제외)
 *   <li>규칙 10: REST API Layer는 Persistence Layer 의존 금지
 *   <li>규칙 11: Config 클래스는 config 패키지에 위치
 *   <li>규칙 12: Properties 클래스는 config/properties 패키지에 위치
 * </ul>
 *
 * <p><strong>참고 문서:</strong>
 *
 * <ul>
 *   <li>rest-api-guide.md - REST API Layer 전체 가이드
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("REST API Layer ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class RestApiLayerArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.adapter.in.rest");
    }

    /** 규칙 1: Package 구조 검증 (controller, dto, mapper, error) */
    @Test
    @DisplayName("[필수] REST API Layer는 표준 패키지 구조를 가져야 한다")
    void restApiLayer_MustHaveStandardPackageStructure() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .resideOutsideOfPackages("..architecture..", "..common..")
                        .and()
                        .haveSimpleNameNotEndingWith("Test")
                        .and()
                        .haveSimpleNameNotEndingWith("TestSupport")
                        .should()
                        .resideInAnyPackage(
                                "..controller..",
                                "..dto..",
                                "..mapper..",
                                "..error..",
                                "..config..",
                                "..filter..")
                        .because(
                                "REST API Layer는 controller, dto, mapper, error, config, filter"
                                        + " 패키지로 구성되어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 2: Bounded Context별 패키지 구조 검증 */
    @Test
    @DisplayName("[권장] Bounded Context는 controller/dto/mapper/error 하위 패키지를 가져야 한다")
    void boundedContext_ShouldHaveStandardSubPackages() {
        // Note: 이 규칙은 템플릿 프로젝트에서는 검증하지 않음 (실제 BC 구현 시 적용)
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .resideOutsideOfPackages("..common..", "..config..", "..architecture..")
                        .should()
                        .resideInAnyPackage("..controller..", "..dto..", "..mapper..", "..error..")
                        .because("Bounded Context는 controller, dto, mapper, error 패키지로 구성되어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 3: Common 패키지 구조 검증 */
    @Test
    @DisplayName("[필수] Common 패키지는 dto/controller/error/mapper/filter로 구성되어야 한다")
    void commonPackage_MustHaveStandardStructure() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..adapter.in.rest.common..")
                        .and()
                        .resideOutsideOfPackage("..architecture..")
                        .and()
                        .haveSimpleNameNotEndingWith("Test")
                        .and()
                        .haveSimpleNameNotEndingWith("TestSupport")
                        .should()
                        .resideInAnyPackage(
                                "..common.dto..",
                                "..common.controller..",
                                "..common.error..",
                                "..common.mapper..",
                                "..common.filter..")
                        .because("Common 패키지는 dto, controller, error, mapper, filter로 구성되어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 4: DTO 패키지 분리 (command/query/response) */
    @Test
    @DisplayName("[필수] DTO는 command/query/response 패키지로 분리되어야 한다")
    void dto_MustBeSeparatedIntoCommandQueryResponse() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..dto..")
                        .and()
                        .resideOutsideOfPackages("..common.dto..", "..architecture..")
                        .and()
                        .areNotNestedClasses()
                        .and()
                        .areNotInterfaces()
                        .and()
                        .haveSimpleNameNotEndingWith("Test")
                        .should()
                        .resideInAnyPackage("..dto.command..", "..dto.query..", "..dto.response..")
                        .because("DTO는 command(상태 변경), query(조회), response(응답)로 분리되어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 5: REST API Layer는 Application Layer Port 의존 필수 */
    @Test
    @DisplayName("[필수] Controller는 Application Layer Port를 의존해야 한다")
    void controller_MustDependOnApplicationPorts() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..controller..")
                        .and()
                        .haveSimpleNameEndingWith("Controller")
                        .and()
                        .haveSimpleNameNotContaining("GlobalExceptionHandler")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..application..port..")
                        .because("Controller는 Application Layer의 UseCase Port를 의존해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 6: REST API Layer는 Domain Layer 직접 의존 금지 */
    @Test
    @DisplayName("[금지] REST API Layer는 Domain Layer를 직접 의존하지 않아야 한다")
    void restApiLayer_MustNotDependOnDomain() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .resideOutsideOfPackages("..error..", "..architecture..")
                        .and()
                        .haveSimpleNameNotContaining("GlobalExceptionHandler")
                        .and()
                        .haveSimpleNameNotEndingWith("ErrorMapper")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.ryuqq.domain..")
                        .because(
                                "REST API Layer는 Domain Layer를 직접 의존하면 안 됩니다"
                                        + " (GlobalExceptionHandler, ErrorMapper 제외)");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 7: GlobalExceptionHandler는 Domain Exception 의존 필수 */
    @Test
    @DisplayName("[필수] GlobalExceptionHandler는 Domain Exception을 의존해야 한다")
    void globalExceptionHandler_MustDependOnDomainException() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..controller..")
                        .and()
                        .haveSimpleNameContaining("GlobalExceptionHandler")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("com.ryuqq.domain..exception..")
                        .because("GlobalExceptionHandler는 Domain Exception을 처리하기 위해 의존해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 8: Controller는 Domain 객체 반환 금지 */
    @Test
    @DisplayName("[금지] Controller는 Domain 객체를 반환하지 않아야 한다")
    void controller_MustNotReturnDomainObjects() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..controller..")
                        .and()
                        .arePublic()
                        .should()
                        .haveRawReturnType("com.ryuqq.domain..")
                        .because("Controller는 Domain 객체를 직접 반환하면 안 되며 API DTO로 변환해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 9: Mapper는 Domain 직접 의존 금지 (ErrorMapper 제외) */
    @Test
    @DisplayName("[금지] Mapper는 Domain을 직접 의존하지 않아야 한다 (ErrorMapper 제외)")
    void mapper_MustNotDependOnDomain() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..mapper..")
                        .and()
                        .haveSimpleNameEndingWith("Mapper")
                        .and()
                        .haveSimpleNameNotContaining("Error")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.ryuqq.domain..")
                        .because(
                                "Mapper는 Application DTO만 사용하며 Domain 직접 의존은 금지됩니다 (ErrorMapper"
                                        + " 제외)");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 10: REST API Layer는 Persistence Layer 의존 금지 */
    @Test
    @DisplayName("[금지] REST API Layer는 Persistence Layer를 의존하지 않아야 한다")
    void restApiLayer_MustNotDependOnPersistence() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .resideOutsideOfPackage("..architecture..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..adapter.out.persistence..")
                        .because(
                                "REST API Layer는 Persistence Layer를 직접 의존하면 안 되며 Application Layer를"
                                        + " 통해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 11: Config 클래스는 config 패키지에 위치 */
    @Test
    @DisplayName("[필수] Config 클래스는 config 패키지에 위치해야 한다")
    void config_MustBeInConfigPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Config")
                        .and()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .resideInAPackage("..config..")
                        .because("Config 클래스는 config 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 12: Properties 클래스는 config/properties 패키지에 위치 */
    @Test
    @DisplayName("[필수] Properties 클래스는 config.properties 패키지에 위치해야 한다")
    void properties_MustBeInConfigPropertiesPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Properties")
                        .and()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .resideInAPackage("..config.properties..")
                        .because("Properties 클래스는 config.properties 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 13: REST API Layer는 @Component/@Service/@Repository 계층 분리 */
    @Test
    @DisplayName("[필수] REST API Layer 컴포넌트는 올바른 Stereotype를 사용해야 한다")
    void restApiLayer_MustUseCorrectStereotypes() {
        ArchRule controllerRule =
                classes()
                        .that()
                        .resideInAPackage("..controller..")
                        .and()
                        .haveSimpleNameEndingWith("Controller")
                        .should()
                        .beAnnotatedWith(
                                org.springframework.web.bind.annotation.RestController.class)
                        .orShould()
                        .beAnnotatedWith(
                                org.springframework.web.bind.annotation.RestControllerAdvice.class)
                        .because("Controller는 @RestController 또는 @RestControllerAdvice를 사용해야 합니다");

        ArchRule mapperRule =
                classes()
                        .that()
                        .resideInAPackage("..mapper..")
                        .and()
                        .haveSimpleNameEndingWith("Mapper")
                        .and()
                        .areNotInterfaces()
                        .should()
                        .beAnnotatedWith(org.springframework.stereotype.Component.class)
                        .because("Mapper는 @Component를 사용해야 합니다");

        controllerRule.allowEmptyShould(true).check(classes);
        mapperRule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 14: Lombok 금지 (전체 Layer) */
    @Test
    @DisplayName("[금지] REST API Layer는 Lombok을 사용하지 않아야 한다")
    void restApiLayer_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .resideOutsideOfPackage("..architecture..")
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
                        .because("REST API Layer는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.allowEmptyShould(true).check(classes);
    }
}
