package com.company.connectly.application.architecture.dto;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * DTO Record Type ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 DTO (Command, Query, Response)는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>Command: dto/command/ 패키지에 Record 타입으로 정의</li>
 *   <li>Query: dto/query/ 패키지에 Record 타입으로 정의</li>
 *   <li>Response: dto/response/ 패키지에 Record 타입으로 정의</li>
 *   <li>Lombok 절대 금지 (Record 사용)</li>
 *   <li>jakarta.validation 의존성 금지 (순수 Java Record)</li>
 *   <li>비즈니스 로직 금지 (데이터 전달만)</li>
 *   <li>@Transactional 절대 금지</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("DTO Record Type ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class DtoRecordArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");
    }

    /**
     * 규칙 1: Command는 Record 타입이어야 함
     */
    @Test
    @DisplayName("[필수] Command는 Record 타입이어야 한다")
    void command_MustBeRecord() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().haveSimpleNameEndingWith("Command")
            .should().beRecords()
            .because("Command는 불변 데이터 전달을 위해 Record 타입을 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: Query는 Record 타입이어야 함
     */
    @Test
    @DisplayName("[필수] Query는 Record 타입이어야 한다")
    void query_MustBeRecord() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("Query")
            .should().beRecords()
            .because("Query는 불변 조회 조건 전달을 위해 Record 타입을 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Response는 Record 타입이어야 함
     */
    @Test
    @DisplayName("[필수] Response는 Record 타입이어야 한다")
    void response_MustBeRecord() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().haveSimpleNameEndingWith("Response")
            .should().beRecords()
            .because("Response는 불변 응답 데이터 전달을 위해 Record 타입을 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Command 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/command/ 패키지의 클래스는 'Command' 접미사를 가져야 한다")
    void command_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Command")
            .because("Command DTO는 'Command' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Query 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/query/ 패키지의 클래스는 'Query' 접미사를 가져야 한다")
    void query_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Query")
            .because("Query DTO는 'Query' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: Response 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/response/ 패키지의 클래스는 'Response' 접미사를 가져야 한다")
    void response_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Response")
            .because("Response DTO는 'Response' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Lombok 절대 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void dto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("DTO는 Record 타입을 사용해야 합니다 (Lombok 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 8: jakarta.validation 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 jakarta.validation 어노테이션을 가지지 않아야 한다")
    void dto_MustNotUseJakartaValidation() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("DTO는 순수 Java Record를 사용해야 합니다 (jakarta.validation 금지, REST API Layer에서 검증)");

        rule.check(classes);
    }

    /**
     * 규칙 9: 비즈니스 메서드 금지
     */
    @Test
    @DisplayName("[금지] DTO는 비즈니스 메서드를 가지지 않아야 한다")
    void dto_MustNotHaveBusinessMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().arePublic()
            .should().haveNameMatching("validate.*|place.*|confirm.*|cancel.*|approve.*|reject.*|modify.*|change.*|update.*|delete.*|save.*|persist.*")
            .because("DTO는 비즈니스 로직을 가질 수 없습니다 (데이터 전달만)");

        rule.check(classes);
    }

    /**
     * 규칙 10: @Transactional 절대 금지
     */
    @Test
    @DisplayName("[금지] DTO는 @Transactional을 가지지 않아야 한다")
    void dto_MustNotHaveTransactionalAnnotation() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .because("@Transactional은 UseCase에서만 사용해야 합니다 (DTO는 데이터 전달만)");

        rule.check(classes);
    }

    /**
     * 규칙 11: Command 패키지 위치
     */
    @Test
    @DisplayName("[필수] Command는 ..application..dto.command.. 패키지에 위치해야 한다")
    void command_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Command")
            .and().areRecords()
            .should().resideInAPackage("..application..dto.command..")
            .because("Command는 application.*.dto.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: Query 패키지 위치
     */
    @Test
    @DisplayName("[필수] Query는 ..application..dto.query.. 패키지에 위치해야 한다")
    void query_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Query")
            .and().areRecords()
            .should().resideInAPackage("..application..dto.query..")
            .because("Query는 application.*.dto.query 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: Response 패키지 위치
     */
    @Test
    @DisplayName("[필수] Response는 ..application..dto.response.. 패키지에 위치해야 한다")
    void response_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Response")
            .and().areRecords()
            .should().resideInAPackage("..application..dto.response..")
            .because("Response는 application.*.dto.response 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: Public 접근 제어
     */
    @Test
    @DisplayName("[필수] DTO는 public 타입이어야 한다")
    void dto_MustBePublic() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto..")
            .and().areRecords()
            .should().bePublic()
            .because("DTO는 계층 간 데이터 전달을 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 15: Static 메서드 금지 (생성 메서드 제외)
     */
    @Test
    @DisplayName("[금지] DTO는 비즈니스 로직 static 메서드를 가지지 않아야 한다")
    void dto_MustNotHaveBusinessStaticMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().areStatic()
            .and().arePublic()
            .and().doNotHaveName("of")  // Record 생성 메서드 허용
            .and().doNotHaveName("from")  // Record 생성 메서드 허용
            .should().haveNameMatching("validate.*|process.*|calculate.*")
            .because("DTO는 비즈니스 로직을 가질 수 없습니다 (생성 메서드 of/from만 허용)");

        rule.check(classes);
    }

    /**
     * 규칙 16: Domain 객체 반환 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Domain 객체를 반환하는 메서드를 가지지 않아야 한다")
    void dto_MustNotReturnDomainObjects() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().arePublic()
            .should().haveRawReturnType("com.ryuqq.domain..")
            .because("DTO에서 Domain 변환은 Assembler에서 처리해야 합니다 (DTO는 데이터만)");

        rule.check(classes);
    }

    /**
     * 규칙 17: Port 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Port 인터페이스를 의존하지 않아야 한다")
    void dto_MustNotDependOnPorts() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().haveNameMatching(".*Port")
            .because("DTO는 Port를 의존할 수 없습니다 (순수 데이터 전달 객체)");

        rule.check(classes);
    }

    /**
     * 규칙 18: Repository 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Repository를 의존하지 않아야 한다")
    void dto_MustNotDependOnRepositories() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().haveNameMatching(".*Repository")
            .because("DTO는 Repository를 의존할 수 없습니다 (순수 데이터 전달 객체)");

        rule.check(classes);
    }
}
