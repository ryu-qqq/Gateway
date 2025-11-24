package com.ryuqq.connectly.adapter.in.rest.architecture.error;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Error Handling ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>RFC 7807 Problem Details 기반 에러 처리 아키텍처를 검증합니다.
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: ErrorMapper는 @Component 어노테이션 필수
 *   <li>규칙 2: ErrorMapper는 ErrorMapper 인터페이스 구현 필수
 *   <li>규칙 3: ErrorMapper는 supports() 메서드 필수
 *   <li>규칙 4: ErrorMapper는 map() 메서드 필수
 *   <li>규칙 5: ErrorMapper는 비즈니스 로직 메서드 금지
 *   <li>규칙 6: ErrorMapper는 MessageSource 의존 가능
 *   <li>규칙 7: ErrorMapper는 *ErrorMapper 네이밍 규칙
 *   <li>규칙 8: ErrorMapper는 올바른 패키지 위치 (error 패키지)
 *   <li>규칙 9: GlobalExceptionHandler는 @RestControllerAdvice 필수
 *   <li>규칙 10: GlobalExceptionHandler는 ErrorMapperRegistry 의존 필수
 * </ul>
 *
 * <p><strong>참고 문서:</strong>
 *
 * <ul>
 *   <li>error/error-handling-strategy.md - 에러 처리 전략
 *   <li>error/error-mapper-implementation-guide.md - ErrorMapper 구현 가이드
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Error Handling ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ErrorHandlingArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.adapter.in.rest");
    }

    /** 규칙 1: ErrorMapper는 @Component 어노테이션 필수 */
    @Test
    @DisplayName("[필수] ErrorMapper는 @Component 어노테이션을 가져야 한다")
    void errorMapper_MustHaveComponentAnnotation() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areNotInterfaces()
                        .should()
                        .beAnnotatedWith(org.springframework.stereotype.Component.class)
                        .because("ErrorMapper는 @Component로 Bean 등록되어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 2: ErrorMapper는 ErrorMapper 인터페이스 구현 필수 */
    @Test
    @DisplayName("[필수] ErrorMapper 구현체는 ErrorMapper 인터페이스를 구현해야 한다")
    void errorMapper_MustImplementInterface() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areNotInterfaces()
                        .should()
                        .implement("com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper")
                        .because("ErrorMapper 구현체는 ErrorMapper 인터페이스를 구현해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 3: ErrorMapper는 supports() 메서드 필수 */
    @Test
    @DisplayName("[필수] ErrorMapper는 supports() 메서드를 가져야 한다")
    void errorMapper_MustHaveSupportsMethod() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..error..")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .haveName("supports")
                        .should()
                        .bePublic()
                        .andShould()
                        .haveRawReturnType(boolean.class)
                        .because("ErrorMapper는 supports(String code) 메서드가 필수입니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 4: ErrorMapper는 map() 메서드 필수 */
    @Test
    @DisplayName("[필수] ErrorMapper는 map() 메서드를 가져야 한다")
    void errorMapper_MustHaveMapMethod() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..error..")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .haveName("map")
                        .should()
                        .bePublic()
                        .because("ErrorMapper는 map(DomainException, Locale) 메서드가 필수입니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 5: ErrorMapper는 비즈니스 로직 메서드 금지 */
    @Test
    @DisplayName("[금지] ErrorMapper는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void errorMapper_MustNotHaveBusinessLogicMethods() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage("..error..")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .haveNameMatching(
                                "calculate|compute|validate|isValid|check|process|execute")
                        .should()
                        .beDeclaredInClassesThat()
                        .resideInAPackage("..error..")
                        .because("ErrorMapper는 단순 변환만 담당하며 비즈니스 로직은 금지됩니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 6: ErrorMapper는 MessageSource 의존 가능 */
    @Test
    @DisplayName("[권장] ErrorMapper는 MessageSource를 의존할 수 있다")
    void errorMapper_ShouldDependOnMessageSource() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areNotInterfaces()
                        .should()
                        .dependOnClassesThat()
                        .haveNameMatching(".*MessageSource.*")
                        .because("ErrorMapper는 i18n 처리를 위해 MessageSource를 의존할 수 있습니다");

        // Note: 이 규칙은 권장사항이므로 실패 시 경고만 표시
        try {
            rule.allowEmptyShould(true).check(classes);
        } catch (AssertionError e) {
            System.out.println("⚠️  Warning: " + e.getMessage());
        }
    }

    /** 규칙 7: ErrorMapper는 *ErrorMapper 네이밍 규칙 */
    @Test
    @DisplayName("[필수] ErrorMapper는 *ErrorMapper 네이밍 규칙을 따라야 한다")
    void errorMapper_MustFollowNamingConvention() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .implement("com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper")
                        .should()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .because(
                                "ErrorMapper 구현체는 *ErrorMapper 네이밍 규칙을 따라야 합니다 (예:"
                                        + " OrderErrorMapper, ProductErrorMapper)");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 8: ErrorMapper는 올바른 패키지 위치 */
    @Test
    @DisplayName("[필수] ErrorMapper는 올바른 패키지에 위치해야 한다")
    void errorMapper_MustBeInCorrectPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .resideInAPackage("..adapter.in.rest..")
                        .should()
                        .resideInAPackage("..error..")
                        .because(
                                "ErrorMapper는 error 패키지에 위치해야 합니다 (예: adapter.in.rest.order.error,"
                                        + " adapter.in.rest.common.error)");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 9: GlobalExceptionHandler는 @RestControllerAdvice 필수 */
    @Test
    @DisplayName("[필수] GlobalExceptionHandler는 @RestControllerAdvice 어노테이션을 가져야 한다")
    void globalExceptionHandler_MustHaveRestControllerAdvice() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..controller..")
                        .and()
                        .haveSimpleNameContaining("GlobalExceptionHandler")
                        .should()
                        .beAnnotatedWith(
                                org.springframework.web.bind.annotation.RestControllerAdvice.class)
                        .because("GlobalExceptionHandler는 @RestControllerAdvice 어노테이션이 필수입니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 10: GlobalExceptionHandler는 ErrorMapperRegistry 의존 필수 */
    @Test
    @DisplayName("[필수] GlobalExceptionHandler는 ErrorMapperRegistry를 의존해야 한다")
    void globalExceptionHandler_MustDependOnErrorMapperRegistry() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..controller..")
                        .and()
                        .haveSimpleNameContaining("GlobalExceptionHandler")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleName("ErrorMapperRegistry")
                        .because("GlobalExceptionHandler는 ErrorMapperRegistry를 의존해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 11: ErrorMapper는 Lombok 금지 */
    @Test
    @DisplayName("[금지] ErrorMapper는 Lombok 어노테이션을 가지지 않아야 한다")
    void errorMapper_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .haveSimpleNameEndingWith("ErrorMapper")
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
                        .because("ErrorMapper는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 12: ErrorMapper는 @Transactional 금지 */
    @Test
    @DisplayName("[금지] ErrorMapper는 @Transactional을 사용하지 않아야 한다")
    void errorMapper_MustNotUseTransactional() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..error..")
                        .and()
                        .haveSimpleNameEndingWith("ErrorMapper")
                        .should()
                        .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                        .because("ErrorMapper는 변환만 담당하며 Transaction은 필요하지 않습니다");

        rule.allowEmptyShould(true).check(classes);
    }
}
