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
 * Command DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Command DTO는 정확히 이 규칙을 따라야 합니다.</p>
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
@DisplayName("Command DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class CommandDtoArchTest {

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
    @DisplayName("[필수] Command DTO는 Record 타입이어야 한다")
    void commandDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Command DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiRequest)
     */
    @Test
    @DisplayName("[필수] Command DTO는 *ApiRequest 접미사를 가져야 한다")
    void commandDto_MustHaveApiRequestSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiRequest")
            .because("Command DTO는 *ApiRequest 네이밍 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Command DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Command DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .and().haveNameMatching("toDomain|toEntity|toAggregate")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .because("Command DTO → Domain 변환은 Mapper/Assembler의 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .and().haveNameMatching("calculate|compute|validate|isValid|check")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .because("Command DTO는 데이터 전송만 담당하며 비즈니스 로직은 Domain Layer 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Bean Validation 어노테이션 사용 권장
     */
    @Test
    @DisplayName("[권장] Command DTO는 Bean Validation 어노테이션을 사용해야 한다")
    void commandDto_ShouldUseValidationAnnotations() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Command DTO는 Bean Validation을 통해 입력값을 검증해야 합니다");

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
    @DisplayName("[필수] Command DTO는 올바른 패키지에 위치해야 한다")
    void commandDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .should().resideInAPackage("..dto.command..")
            .because("Command DTO는 dto.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Command DTO는 Setter 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveSetterMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("..dto.command..")
            .because("Command DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Command DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
