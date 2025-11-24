package com.ryuqq.connectly.application.architecture.assembler;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

/**
 * Assembler ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Assembler는 정확히 이 규칙을 따라야 합니다:
 *
 * <ul>
 *   <li>@Component 필수 (Bean 등록)
 *   <li>Lombok 절대 금지 (Plain Java)
 *   <li>Static 메서드 금지 (테스트 용이성)
 *   <li>Port 의존성 금지 (단순 변환기)
 *   <li>Repository 의존성 금지
 *   <li>Spring Data Page/Slice 금지
 *   <li>비즈니스 메서드 금지
 *   <li>@Transactional 절대 금지
 *   <li>PageResponse/SliceResponse 반환 금지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Assembler ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class AssemblerArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");
    }

    /** 규칙 1: @Component 필수 */
    @Test
    @DisplayName("[필수] Assembler는 @Component 어노테이션을 가져야 한다")
    void assembler_MustHaveComponentAnnotation() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .beAnnotatedWith(Component.class)
                        .because("Assembler는 Spring Bean으로 등록되어야 합니다 (테스트 용이성)");

        rule.check(classes);
    }

    /** 규칙 2: Lombok 절대 금지 */
    @Test
    @DisplayName("[금지] Assembler는 Lombok 어노테이션을 가지지 않아야 한다")
    void assembler_MustNotUseLombok() {
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

    /** 규칙 3: Static 메서드 금지 */
    @Test
    @DisplayName("[금지] Assembler는 public static 메서드를 가지지 않아야 한다")
    void assembler_MustNotHavePublicStaticMethods() {
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

    /** 규칙 4: Port 의존성 금지 */
    @Test
    @DisplayName("[금지] Assembler는 Port 인터페이스를 의존하지 않아야 한다")
    void assembler_MustNotDependOnPorts() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .dependOnClassesThat()
                        .haveNameMatching(".*Port")
                        .because("Assembler는 Port를 주입받지 않아야 합니다 (단순 변환기)");

        rule.check(classes);
    }

    /** 규칙 5: Repository 의존성 금지 */
    @Test
    @DisplayName("[금지] Assembler는 Repository를 의존하지 않아야 한다")
    void assembler_MustNotDependOnRepositories() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .dependOnClassesThat()
                        .haveNameMatching(".*Repository")
                        .because("Assembler는 Repository를 주입받지 않아야 합니다 (UseCase에서 처리)");

        rule.check(classes);
    }

    /** 규칙 6: Spring Data 의존성 금지 (Page, Slice) */
    @Test
    @DisplayName("[금지] Assembler는 Spring Data Page/Slice를 사용하지 않아야 한다")
    void assembler_MustNotUseSpringDataPageable() {
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
                                "Assembler는 Spring Data 대신 custom PageResponse/SliceResponse를 사용해야"
                                        + " 합니다");

        rule.check(classes);
    }

    /** 규칙 7: 클래스명 규칙 */
    @Test
    @DisplayName("[필수] Assembler는 'Assembler' 접미사를 가져야 한다")
    void assembler_MustHaveCorrectSuffix() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..application..assembler..")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .should()
                        .haveSimpleNameEndingWith("Assembler")
                        .because("Assembler는 'Assembler' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /** 규칙 8: 패키지 위치 */
    @Test
    @DisplayName("[필수] Assembler는 ..application..assembler.. 패키지에 위치해야 한다")
    void assembler_MustBeInCorrectPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .resideInAPackage("..application..assembler..")
                        .because("Assembler는 application.*.assembler 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /** 규칙 9: 메서드명 규칙 (변환 메서드만 허용) */
    @Test
    @DisplayName("[권장] Assembler 메서드명은 변환 패턴을 따라야 한다")
    void assembler_MethodsShouldFollowConversionPattern() {
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
                        .haveNameMatching("(to|from|assemble|map)[A-Z].*")
                        .because("Assembler 메서드는 변환 메서드이므로 to*/from*/assemble*/map* 패턴을 따라야 합니다");

        rule.check(classes);
    }

    /** 규칙 10: 비즈니스 메서드 금지 */
    @Test
    @DisplayName("[금지] Assembler는 비즈니스 메서드를 가지지 않아야 한다")
    void assembler_MustNotHaveBusinessMethods() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .bePublic()
                        .andShould()
                        .haveNameMatching(
                                "validate.*|place.*|confirm.*|cancel.*|approve.*|reject.*|modify.*|change.*|update.*|delete.*|save.*|persist.*")
                        .because("Assembler는 비즈니스 로직을 가질 수 없습니다 (Domain에서 처리)");

        rule.check(classes);
    }

    /** 규칙 11: @Transactional 절대 금지 */
    @Test
    @DisplayName("[금지] Assembler는 @Transactional을 가지지 않아야 한다")
    void assembler_MustNotHaveTransactionalAnnotation() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                        .because("@Transactional은 UseCase에서만 사용해야 합니다 (Assembler는 변환만)");

        rule.check(classes);
    }

    /** 규칙 12: PageResponse/SliceResponse 반환 금지 */
    @Test
    @DisplayName("[금지] Assembler는 PageResponse/SliceResponse를 반환하지 않아야 한다")
    void assembler_MustNotReturnPageOrSliceResponse() {
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
                                "PageResponse/SliceResponse 조립은 UseCase에서 처리해야 합니다 (Assembler는 List"
                                        + " 변환만)");

        rule.check(classes);
    }

    /** 규칙 13: Public 클래스 */
    @Test
    @DisplayName("[필수] Assembler는 public 클래스여야 한다")
    void assembler_MustBePublic() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .bePublic()
                        .because("Assembler는 Spring Bean으로 등록되기 위해 public이어야 합니다");

        rule.check(classes);
    }

    /** 규칙 14: Final 클래스 금지 */
    @Test
    @DisplayName("[필수] Assembler는 final 클래스가 아니어야 한다")
    void assembler_MustNotBeFinal() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .notHaveModifier(FINAL)
                        .because("Spring은 프록시 생성을 위해 Assembler가 final이 아니어야 합니다");

        rule.check(classes);
    }

    /** 규칙 15: 필드가 있다면 final이어야 함 (생성자 주입) */
    @Test
    @DisplayName("[권장] Assembler 필드는 final이어야 한다")
    void assembler_FieldsShouldBeFinal() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("Assembler")
                        .and()
                        .areNotStatic()
                        .should()
                        .beFinal()
                        .because("Assembler는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

        rule.check(classes);
    }

    /** 규칙 16: Application Layer만 의존 */
    @Test
    @DisplayName("[필수] Assembler는 Application Layer와 Domain Layer만 의존해야 한다")
    void assembler_MustOnlyDependOnApplicationAndDomainLayers() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Assembler")
                        .should()
                        .onlyAccessClassesThat()
                        .resideInAnyPackage(
                                "com.ryuqq.application..",
                                "com.ryuqq.domain..",
                                "org.springframework..",
                                "java..",
                                "jakarta..")
                        .because(
                                "Assembler는 Application Layer와 Domain Layer만 의존해야 합니다 (Port,"
                                        + " Repository 금지)");

        rule.check(classes);
    }

    /** 규칙 17: 필드명 규칙 (소문자 시작) */
    @Test
    @DisplayName("[권장] Assembler의 필드명은 소문자로 시작해야 한다")
    void assembler_FieldsShouldStartWithLowercase() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("Assembler")
                        .and()
                        .areNotStatic()
                        .should()
                        .haveNameMatching("[a-z].*")
                        .because("필드명은 camelCase 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /** 규칙 18: 계산 로직 금지 (BigDecimal 연산) */
    @Test
    @DisplayName("[금지] Assembler는 계산 로직 메서드를 가지지 않아야 한다")
    void assembler_MustNotHaveCalculationLogic() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("Assembler")
                        .and()
                        .arePublic()
                        .should()
                        .haveNameMatching(
                                "calculate.*|compute.*|sum.*|multiply.*|divide.*|add.*|subtract.*")
                        .because("Assembler는 계산 로직을 가질 수 없습니다 (Domain에서 처리)");

        rule.check(classes);
    }
}
