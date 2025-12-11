package com.ryuqq.gateway.application.architecture.dto;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * DTO Record ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: DTO는 순수한 불변 데이터 전달 객체
 *
 * <p>Command, Query, Response 모두 Record 타입으로 정의되어야 합니다.
 *
 * <p>클래스 존재 여부 체크:
 *
 * <ul>
 *   <li>해당 DTO 클래스가 없으면 테스트 성공 처리 (빈 프로젝트 지원)
 *   <li>클래스가 존재할 때만 규칙 검증 수행
 * </ul>
 *
 * <p>업계 레퍼런스:
 *
 * <ul>
 *   <li>Sairyss/domain-driven-hexagon (12k+ stars)
 *   <li>CQRS Pattern - Microsoft Azure Architecture
 * </ul>
 *
 * @see <a href="https://github.com/Sairyss/domain-driven-hexagon">domain-driven-hexagon</a>
 */
@DisplayName("DTO Record ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("dto")
class DtoRecordArchTest {

    private static final String DTO_BUNDLE_PACKAGE = "..dto.bundle..";

    private static JavaClasses classes;
    private static boolean hasCommandClasses;
    private static boolean hasQueryClasses;
    private static boolean hasResponseClasses;
    private static boolean hasDtoClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        hasCommandClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".dto.command")
                                                && javaClass.getSimpleName().endsWith("Command"));

        hasQueryClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".dto.query")
                                                && javaClass.getSimpleName().endsWith("Query"));

        hasResponseClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".dto.response")
                                                && javaClass.getSimpleName().endsWith("Response"));

        hasDtoClasses = hasCommandClasses || hasQueryClasses || hasResponseClasses;
    }

    // ==================== Command 규칙 ====================

    @Nested
    @DisplayName("Command DTO 규칙")
    class CommandRules {

        @Test
        @DisplayName("[필수] Command는 Record 타입이어야 한다")
        void command_MustBeRecord() {
            assumeTrue(hasCommandClasses, "Command 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.command..")
                            .and()
                            .haveSimpleNameEndingWith("Command")
                            .should()
                            .beRecords()
                            .because("Command는 불변 데이터 전달을 위해 Record 타입을 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] dto/command/ 패키지의 클래스는 'Command' 접미사를 가져야 한다")
        void command_MustHaveCorrectSuffix() {
            assumeTrue(hasCommandClasses, "Command 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.command..")
                            .and()
                            .areNotMemberClasses()
                            .should()
                            .haveSimpleNameEndingWith("Command")
                            .because("Command DTO는 'Command' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Command는 ..application..dto.command.. 패키지에 위치해야 한다")
        void command_MustBeInCorrectPackage() {
            assumeTrue(hasCommandClasses, "Command 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Command")
                            .and()
                            .areRecords()
                            .should()
                            .resideInAPackage("..application..dto.command..")
                            .because("Command는 application.*.dto.command 패키지에 위치해야 합니다");

            rule.check(classes);
        }
    }

    // ==================== Query 규칙 ====================

    @Nested
    @DisplayName("Query DTO 규칙")
    class QueryRules {

        @Test
        @DisplayName("[필수] Query는 Record 타입이어야 한다")
        void query_MustBeRecord() {
            assumeTrue(hasQueryClasses, "Query 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.query..")
                            .and()
                            .haveSimpleNameEndingWith("Query")
                            .should()
                            .beRecords()
                            .because("Query는 불변 조회 조건 전달을 위해 Record 타입을 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] dto/query/ 패키지의 클래스는 'Query' 접미사를 가져야 한다")
        void query_MustHaveCorrectSuffix() {
            assumeTrue(hasQueryClasses, "Query 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.query..")
                            .and()
                            .areNotMemberClasses()
                            .should()
                            .haveSimpleNameEndingWith("Query")
                            .because("Query DTO는 'Query' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Query는 ..application..dto.query.. 패키지에 위치해야 한다")
        void query_MustBeInCorrectPackage() {
            assumeTrue(hasQueryClasses, "Query 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Query")
                            .and()
                            .areRecords()
                            .should()
                            .resideInAPackage("..application..dto.query..")
                            .because("Query는 application.*.dto.query 패키지에 위치해야 합니다");

            rule.check(classes);
        }
    }

    // ==================== Response 규칙 ====================

    @Nested
    @DisplayName("Response DTO 규칙")
    class ResponseRules {

        @Test
        @DisplayName("[필수] Response는 Record 타입이어야 한다")
        void response_MustBeRecord() {
            assumeTrue(hasResponseClasses, "Response 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.response..")
                            .and()
                            .haveSimpleNameEndingWith("Response")
                            .should()
                            .beRecords()
                            .because("Response는 불변 응답 데이터 전달을 위해 Record 타입을 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] dto/response/ 패키지의 클래스는 'Response' 접미사를 가져야 한다")
        void response_MustHaveCorrectSuffix() {
            assumeTrue(hasResponseClasses, "Response 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto.response..")
                            .and()
                            .areNotMemberClasses()
                            .should()
                            .haveSimpleNameEndingWith("Response")
                            .because("Response DTO는 'Response' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Response는 ..application..dto.response.. 패키지에 위치해야 한다")
        void response_MustBeInCorrectPackage() {
            assumeTrue(hasResponseClasses, "Response 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Response")
                            .and()
                            .areRecords()
                            .should()
                            .resideInAPackage("..application..dto.response..")
                            .because("Response는 application.*.dto.response 패키지에 위치해야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] DTO는 Lombok 어노테이션을 가지지 않아야 한다")
        void dto_MustNotUseLombok() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..dto..")
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
                            .because("DTO는 Record 타입을 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] DTO는 jakarta.validation 어노테이션을 가지지 않아야 한다")
        void dto_MustNotUseJakartaValidation() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..dto..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage("jakarta.validation..")
                            .because(
                                    "DTO는 순수 Java Record를 사용해야 합니다 "
                                            + "(jakarta.validation 금지, REST API Layer에서 검증)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] DTO는 @Transactional을 가지지 않아야 한다")
        void dto_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..dto..")
                            .should()
                            .beAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .because("@Transactional은 UseCase에서만 사용해야 합니다 (DTO는 데이터 전달만)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] DTO는 비즈니스 메서드를 가지지 않아야 한다")
        void dto_MustNotHaveBusinessMethods() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..dto..")
                            .and()
                            .arePublic()
                            .should()
                            .haveNameMatching(
                                    "validate.*|place.*|confirm.*|cancel.*|"
                                        + "approve.*|reject.*|modify.*|change.*|update.*|delete.*|save.*|persist.*")
                            .because("DTO는 비즈니스 로직을 가질 수 없습니다 (데이터 전달만)");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[금지] DTO는 Port 인터페이스를 의존하지 않아야 한다")
        void dto_MustNotDependOnPorts() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..dto..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Port")
                            .because("DTO는 Port를 의존할 수 없습니다 (순수 데이터 전달 객체)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] DTO는 Repository를 의존하지 않아야 한다")
        void dto_MustNotDependOnRepositories() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..dto..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Repository")
                            .because("DTO는 Repository를 의존할 수 없습니다 (순수 데이터 전달 객체)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] DTO는 Domain 객체를 반환하는 메서드를 가지지 않아야 한다")
        void dto_MustNotReturnDomainObjects() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..dto..")
                            .and()
                            .areDeclaredInClassesThat()
                            .resideOutsideOfPackage(DTO_BUNDLE_PACKAGE)
                            .and()
                            .arePublic()
                            .should()
                            .haveRawReturnType("com.ryuqq.domain..")
                            .because("DTO에서 Domain 변환은 Assembler에서 처리해야 합니다 (DTO는 데이터만)");

            rule.check(classes);
        }
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] DTO는 public 타입이어야 한다")
        void dto_MustBePublic() {
            assumeTrue(hasDtoClasses, "DTO 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..dto..")
                            .and()
                            .areRecords()
                            .should()
                            .bePublic()
                            .because("DTO는 계층 간 데이터 전달을 위해 public이어야 합니다");

            rule.check(classes);
        }
    }
}
