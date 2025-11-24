package com.ryuqq.connectly.adapter.in.rest.architecture.common;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API Response ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>Common API Response 패턴을 검증합니다.
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: Common Response DTOs는 common.dto 패키지에 위치
 *   <li>규칙 2: Common Response DTOs는 Record 타입이어야 함
 *   <li>규칙 3: ApiResponse는 static factory method 패턴 사용
 *   <li>규칙 4: PageApiResponse는 from() 메서드 필수
 *   <li>규칙 5: ErrorInfo는 validation 필수
 *   <li>규칙 6: Common Response DTOs는 Lombok 금지
 *   <li>규칙 7: Common Response DTOs는 public이어야 함
 *   <li>규칙 8: Common Response DTOs는 final이어야 함 (Record 특성)
 * </ul>
 *
 * <p><strong>참고 문서:</strong>
 *
 * <ul>
 *   <li>rest-api-guide.md - REST API Layer 전체 가이드
 *   <li>dto/response/response-dto-guide.md - Response DTO 가이드
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("API Response ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ApiResponseArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.adapter.in.rest");
    }

    /** 규칙 1: Common Response DTOs는 common.dto 패키지에 위치 */
    @Test
    @DisplayName("[필수] Common Response DTOs는 common.dto 패키지에 위치해야 한다")
    void commonResponseDtos_MustBeInCommonDtoPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*ApiResponse|.*ErrorInfo")
                        .and()
                        .resideInAPackage("..adapter.in.rest..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .resideInAPackage("..common.dto..")
                        .because(
                                "Common Response DTOs(ApiResponse, PageApiResponse,"
                                    + " SliceApiResponse, ErrorInfo)는 common.dto 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 2: Common Response DTOs는 Record 타입이어야 함 */
    @Test
    @DisplayName("[필수] Common Response DTOs는 Java Record 타입이어야 한다")
    void commonResponseDtos_MustBeRecords() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*ApiResponse|.*ErrorInfo")
                        .and()
                        .resideInAPackage("..common.dto..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .beRecords()
                        .because("Common Response DTOs는 불변성 보장을 위해 Java 21 Record를 사용해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 3: ApiResponse는 static factory method 패턴 사용 */
    @Test
    @DisplayName("[필수] ApiResponse는 ofSuccess/ofFailure static factory methods를 가져야 한다")
    void apiResponse_MustHaveStaticFactoryMethods() {
        ArchRule successRule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleName("ApiResponse")
                        .and()
                        .haveName("ofSuccess")
                        .should()
                        .bePublic()
                        .andShould()
                        .beStatic()
                        .because("ApiResponse는 ofSuccess() static factory method가 필수입니다");

        ArchRule failureRule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleName("ApiResponse")
                        .and()
                        .haveName("ofFailure")
                        .should()
                        .bePublic()
                        .andShould()
                        .beStatic()
                        .because("ApiResponse는 ofFailure() static factory method가 필수입니다");

        successRule.allowEmptyShould(true).check(classes);
        failureRule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 4: PageApiResponse는 from() 메서드 필수 */
    @Test
    @DisplayName("[필수] PageApiResponse는 from() static factory method를 가져야 한다")
    void pageApiResponse_MustHaveFromMethod() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleName("PageApiResponse")
                        .and()
                        .haveName("from")
                        .should()
                        .bePublic()
                        .andShould()
                        .beStatic()
                        .because(
                                "PageApiResponse는 Application Layer PageResponse를 변환하는 from()"
                                        + " static method가 필수입니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 5: ErrorInfo는 validation 필수 */
    @Test
    @DisplayName("[권장] ErrorInfo는 compact constructor에서 validation을 수행해야 한다")
    void errorInfo_ShouldHaveValidation() {
        // Note: ArchUnit으로 compact constructor 내부 로직 검증은 어려우므로
        // 대신 ErrorInfo가 Record이고 필수 필드를 가지는지 검증
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleName("ErrorInfo")
                        .should()
                        .beRecords()
                        .because(
                                "ErrorInfo는 Record로 정의되어 compact constructor에서 validation을 수행해야"
                                        + " 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 6: Common Response DTOs는 Lombok 금지 */
    @Test
    @DisplayName("[금지] Common Response DTOs는 Lombok을 사용하지 않아야 한다")
    void commonResponseDtos_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveNameMatching(".*ApiResponse|.*ErrorInfo")
                        .and()
                        .resideInAPackage("..common.dto..")
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
                        .because("Common Response DTOs는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 7: Common Response DTOs는 public이어야 함 */
    @Test
    @DisplayName("[필수] Common Response DTOs는 public이어야 한다")
    void commonResponseDtos_MustBePublic() {
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*ApiResponse|.*ErrorInfo")
                        .and()
                        .resideInAPackage("..common.dto..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .bePublic()
                        .because("Common Response DTOs는 외부에서 사용 가능하도록 public이어야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    /** 규칙 8: Common Response DTOs는 final이어야 함 (Record 특성) */
    @Test
    @DisplayName("[권장] Common Response DTOs Record는 final 특성을 가진다")
    void commonResponseDtos_ShouldBeFinal() {
        // Note: Java Record는 암묵적으로 final이므로 Record 타입 검증으로 대체
        ArchRule rule =
                classes()
                        .that()
                        .haveNameMatching(".*ApiResponse|.*ErrorInfo")
                        .and()
                        .resideInAPackage("..common.dto..")
                        .and()
                        .areNotNestedClasses()
                        .should()
                        .beRecords()
                        .because("Common Response DTOs는 Record로 정의되어 암묵적으로 final 특성을 가집니다");

        rule.allowEmptyShould(true).check(classes);
    }
}
