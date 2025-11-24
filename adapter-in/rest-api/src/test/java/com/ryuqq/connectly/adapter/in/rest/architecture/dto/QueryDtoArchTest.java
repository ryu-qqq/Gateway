package com.ryuqq.connectly.adapter.in.rest.architecture.dto;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Query DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Query DTO는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * <p>검증 규칙:
 * <ul>
 *   <li>1. Record 타입 필수</li>
 *   <li>2. *ApiRequest 네이밍 규칙</li>
 *   <li>3. Lombok 어노테이션 절대 금지</li>
 *   <li>4. Jackson 어노테이션 절대 금지</li>
 *   <li>5. Domain 변환 메서드 금지</li>
 *   <li>6. 비즈니스 로직 메서드 금지</li>
 *   <li>7. Bean Validation 어노테이션 사용 권장</li>
 *   <li>8. 올바른 패키지 위치</li>
 *   <li>9. Setter 메서드 절대 금지</li>
 *   <li>10. Spring 어노테이션 절대 금지</li>
 * </ul>
 * </p>
 *
 * @author ryu-qqq
 * @since 2025-11-13
 */
@DisplayName("Query DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class QueryDtoArchTest {

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
    @DisplayName("[필수] Query DTO는 Record 타입이어야 한다")
    void queryDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Query DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiRequest)
     */
    @Test
    @DisplayName("[필수] Query DTO는 *ApiRequest 접미사를 가져야 한다")
    void queryDto_MustHaveApiRequestSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiRequest")
            .because("Query DTO는 *ApiRequest 네이밍 규칙을 따라야 합니다 (예: SearchApiRequest, ListApiRequest)");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Query DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Query DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .and().haveNameMatching("toCriteria|toFilter|toSearchCondition|toQuery")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .because("Query DTO → Domain 변환은 Mapper의 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .and().haveNameMatching("calculate|compute|isValid|check")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .because("Query DTO는 검색 조건만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Bean Validation 어노테이션 사용 권장 (페이징 필드에만)
     */
    @Test
    @DisplayName("[권장] Query DTO는 페이징 필드에 Bean Validation을 사용해야 한다")
    void queryDto_ShouldUseValidationForPaging() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Query DTO는 페이징 필드(page, size)에 Bean Validation을 사용해야 합니다");

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
    @DisplayName("[필수] Query DTO는 올바른 패키지에 위치해야 한다")
    void queryDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .and().resideInAPackage("..dto..")
            .and().areNotInterfaces()
            .should().resideInAPackage("..dto.query..")
            .orShould().resideInAPackage("..dto.command..")  // Command도 *ApiRequest이므로 허용
            .because("Query DTO는 dto.query 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Query DTO는 Setter 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveSetterMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.query..")
            .because("Query DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Query DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
