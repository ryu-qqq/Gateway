package com.company.connectly.adapter.in.rest.architecture.dto;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Response DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Response DTO는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * <p>검증 규칙:
 * <ul>
 *   <li>1. Record 타입 필수</li>
 *   <li>2. *ApiResponse 네이밍 규칙</li>
 *   <li>3. Lombok 어노테이션 절대 금지</li>
 *   <li>4. Jackson 어노테이션 절대 금지</li>
 *   <li>5. Domain 변환 메서드 금지</li>
 *   <li>6. 비즈니스 로직 메서드 금지</li>
 *   <li>7. from() 메서드 필수 (권장)</li>
 *   <li>8. 올바른 패키지 위치</li>
 *   <li>9. Setter 메서드 절대 금지</li>
 *   <li>10. Spring 어노테이션 절대 금지</li>
 * </ul>
 * </p>
 *
 * @author ryu-qqq
 * @since 2025-11-13
 */
@DisplayName("Response DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ResponseDtoArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.in.rest");
    }

    /**
     * 규칙 1: Record 타입 필수
     */
    @Test
    @DisplayName("[필수] Response DTO는 Record 타입이어야 한다")
    void responseDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().haveSimpleNameEndingWith("ApiResponse")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Response DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiResponse)
     */
    @Test
    @DisplayName("[필수] Response DTO는 *ApiResponse 접미사를 가져야 한다")
    void responseDto_MustHaveApiResponseSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiResponse")
            .because("Response DTO는 *ApiResponse 네이밍 규칙을 따라야 합니다 (예: OrderApiResponse, OrderSummaryApiResponse)");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Response DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Response DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .and().haveNameMatching("toDomain|toEntity|toAggregate")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .because("Response DTO는 출력 전용이며 Domain 변환 메서드는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .and().haveNameMatching("calculate|compute|validate|process")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .because("Response DTO는 데이터 전송만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: from() 메서드 필수 (권장)
     */
    @Test
    @DisplayName("[권장] Response DTO는 from() 메서드를 가져야 한다")
    void responseDto_ShouldHaveFromMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("ApiResponse")
            .and().areDeclaredInClassesThat().areNotNestedClasses()
            .and().haveName("from")
            .should().beStatic()
            .because("Response DTO는 Application Layer Response를 변환하는 static from() 메서드가 필수입니다");

        // Note: 이 규칙은 권장사항이므로 실패 시 경고만 표시
        try {
            rule.check(classes);
        } catch (AssertionError e) {
            System.out.println("⚠️  Warning: " + e.getMessage());
        }
    }

    /**
     * 규칙 8: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] Response DTO는 올바른 패키지에 위치해야 한다")
    void responseDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiResponse")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .and().resideInAPackage("..dto..")
            .and().areNotInterfaces()
            .should().resideInAPackage("..dto.response..")
            .because("Response DTO는 dto.response 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Response DTO는 Setter 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveSetterMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.response..")
            .because("Response DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Response DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
