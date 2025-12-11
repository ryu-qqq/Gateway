package com.ryuqq.gateway.application.architecture.port.in;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase (Port-In) ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>Command UseCase와 Query UseCase의 구조 규칙을 통합 검증합니다.
 *
 * <h3>핵심 원칙:</h3>
 *
 * <ul>
 *   <li>UseCase는 Interface여야 함
 *   <li>execute() 메서드 필수
 *   <li>@Transactional 금지 (Service 구현체에만 적용)
 *   <li>Domain 직접 반환 금지
 *   <li>DTO 패키지 분리 (dto/command, dto/query, dto/response)
 * </ul>
 *
 * <h3>Command UseCase:</h3>
 *
 * <ul>
 *   <li>패키지: ..application..port.in.command..
 *   <li>반환: Response DTO 또는 void
 *   <li>네이밍: Create*, Update*, Delete* 등 (권장)
 * </ul>
 *
 * <h3>Query UseCase:</h3>
 *
 * <ul>
 *   <li>패키지: ..application..port.in.query..
 *   <li>반환: Response DTO, List, PageResponse 등
 *   <li>네이밍: Get*, Search*, Find* 등 (권장)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("UseCase ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class UseCaseArchTest {

    private static JavaClasses classes;
    private static boolean hasUseCaseClasses;
    private static boolean hasCommandUseCaseClasses;
    private static boolean hasQueryUseCaseClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        hasUseCaseClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".port.in")
                                                && javaClass.getSimpleName().endsWith("UseCase")
                                                && javaClass.isInterface());

        hasCommandUseCaseClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".port.in.command")
                                                && javaClass.getSimpleName().endsWith("UseCase")
                                                && javaClass.isInterface());

        hasQueryUseCaseClasses =
                classes.stream()
                        .anyMatch(
                                javaClass ->
                                        javaClass.getPackageName().contains(".port.in.query")
                                                && javaClass.getSimpleName().endsWith("UseCase")
                                                && javaClass.isInterface());
    }

    // ========================================
    // 기본 구조 규칙 (공통)
    // ========================================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] UseCase는 '*UseCase' 접미사를 가져야 한다")
        void useCase_MustHaveCorrectSuffix() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..port.in..")
                            .and()
                            .areInterfaces()
                            .should()
                            .haveSimpleNameEndingWith("UseCase")
                            .because("UseCase는 'UseCase' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] UseCase는 Interface여야 한다")
        void useCase_MustBeInterface() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .should()
                            .beInterfaces()
                            .because("UseCase는 Interface로 선언되어야 합니다 (구현체는 Service)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] UseCase는 public이어야 한다")
        void useCase_MustBePublic() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .should()
                            .bePublic()
                            .because("UseCase는 외부에서 접근 가능해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] UseCase는 execute() 메서드를 가져야 한다")
        void useCase_MustHaveExecuteMethod() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            // execute() 메서드 존재 여부 직접 검증
            long violationCount =
                    classes.stream()
                            .filter(javaClass -> javaClass.getPackageName().contains(".port.in"))
                            .filter(javaClass -> javaClass.getSimpleName().endsWith("UseCase"))
                            .filter(JavaClass::isInterface)
                            .filter(
                                    javaClass ->
                                            javaClass.getMethods().stream()
                                                    .noneMatch(
                                                            method ->
                                                                    method.getName()
                                                                            .equals("execute")))
                            .count();

            if (violationCount > 0) {
                fail("UseCase에 execute() 메서드가 없습니다. 위반 수: " + violationCount);
            }
        }
    }

    // ========================================
    // Command UseCase 규칙
    // ========================================

    @Nested
    @DisplayName("Command UseCase 규칙")
    class CommandUseCaseRules {

        @Test
        @DisplayName("[필수] Command UseCase는 ..port.in.command.. 패키지에 위치해야 한다")
        void commandUseCase_MustBeInCorrectPackage() {
            assumeTrue(hasCommandUseCaseClasses, "Command UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in.command..")
                            .should()
                            .resideInAPackage("..application..port.in.command..")
                            .because(
                                    "Command UseCase는 application.*.port.in.command 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[권장] Command UseCase는 상태 변경 동사로 시작하는 것이 좋다")
        void commandUseCase_ShouldStartWithCommandVerb() {
            assumeTrue(hasCommandUseCaseClasses, "Command UseCase 클래스가 없어 테스트를 스킵합니다");

            // 권장 사항 - 경고만 출력, 빌드 실패 아님
            long violationCount =
                    classes.stream()
                            .filter(
                                    javaClass ->
                                            javaClass.getPackageName().contains(".port.in.command"))
                            .filter(javaClass -> javaClass.getSimpleName().endsWith("UseCase"))
                            .filter(JavaClass::isInterface)
                            .filter(
                                    javaClass ->
                                            !javaClass
                                                    .getSimpleName()
                                                    .matches(
                                                            "(Create|Update|Delete|Place|Cancel|Confirm|Register|Remove|Modify|Approve|Reject|Send|Process|Execute).*UseCase"))
                            .count();

            if (violationCount > 0) {
                System.out.println(
                        "[WARNING] " + violationCount + "개의 Command UseCase가 권장 네이밍 패턴을 따르지 않습니다.");
                System.out.println(
                        "권장 prefix: Create, Update, Delete, Place, Cancel, Confirm, Register,"
                                + " Remove, Modify, Approve, Reject, Send, Process, Execute");
            }
            // 테스트는 통과 (권장 사항)
        }
    }

    // ========================================
    // Query UseCase 규칙
    // ========================================

    @Nested
    @DisplayName("Query UseCase 규칙")
    class QueryUseCaseRules {

        @Test
        @DisplayName("[필수] Query UseCase는 ..port.in.query.. 패키지에 위치해야 한다")
        void queryUseCase_MustBeInCorrectPackage() {
            assumeTrue(hasQueryUseCaseClasses, "Query UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in.query..")
                            .should()
                            .resideInAPackage("..application..port.in.query..")
                            .because("Query UseCase는 application.*.port.in.query 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[권장] Query UseCase는 조회 동사로 시작하는 것이 좋다")
        void queryUseCase_ShouldStartWithQueryVerb() {
            assumeTrue(hasQueryUseCaseClasses, "Query UseCase 클래스가 없어 테스트를 스킵합니다");

            // 권장 사항 - 경고만 출력, 빌드 실패 아님
            long violationCount =
                    classes.stream()
                            .filter(
                                    javaClass ->
                                            javaClass.getPackageName().contains(".port.in.query"))
                            .filter(javaClass -> javaClass.getSimpleName().endsWith("UseCase"))
                            .filter(JavaClass::isInterface)
                            .filter(
                                    javaClass ->
                                            !javaClass
                                                    .getSimpleName()
                                                    .matches(
                                                            "(Get|Search|Find|List|Retrieve|Fetch|Query|Count|Check|Exists).*UseCase"))
                            .count();

            if (violationCount > 0) {
                System.out.println(
                        "[WARNING] " + violationCount + "개의 Query UseCase가 권장 네이밍 패턴을 따르지 않습니다.");
                System.out.println(
                        "권장 prefix: Get, Search, Find, List, Retrieve, Fetch, Query, Count, Check,"
                                + " Exists");
            }
            // 테스트는 통과 (권장 사항)
        }
    }

    // ========================================
    // 금지 규칙
    // ========================================

    @Nested
    @DisplayName("금지 규칙")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] UseCase 인터페이스에 @Transactional을 사용하지 않아야 한다")
        void useCase_MustNotHaveTransactionalOnInterface() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .and()
                            .areInterfaces()
                            .should()
                            .beAnnotatedWith(Transactional.class)
                            .because("@Transactional은 Service 구현체에만 적용해야 합니다 (Spring Proxy 제약)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] UseCase는 Domain Entity를 직접 반환하지 않아야 한다")
        void useCase_MustNotReturnDomainEntity() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..port.in..")
                            .and()
                            .arePublic()
                            .should()
                            .haveRawReturnType(resideInAPackage("..domain.."))
                            .because("UseCase는 Response DTO를 반환해야 합니다 (Domain 노출 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] UseCase는 내부 Record를 가지지 않아야 한다")
        void useCase_MustNotHaveInnerRecords() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            // 내부 클래스 중 Command, Query, Response 이름을 가진 것이 있는지 확인
            long violationCount =
                    classes.stream()
                            .filter(javaClass -> javaClass.getPackageName().contains(".port.in"))
                            .filter(javaClass -> javaClass.getSimpleName().endsWith("UseCase"))
                            .filter(JavaClass::isInterface)
                            .flatMap(javaClass -> javaClass.getAllSubclasses().stream())
                            .filter(
                                    subClass ->
                                            subClass.getSimpleName()
                                                    .matches(".*(Command|Query|Response)"))
                            .filter(subClass -> subClass.getEnclosingClass().isPresent())
                            .count();

            // Note: Interface는 보통 내부 클래스를 가지지 않으므로 이 테스트는 대부분 통과함
            if (violationCount > 0) {
                fail(
                        "UseCase 내부에 Command/Query/Response Record가 정의되어 있습니다. "
                                + "별도 dto 패키지에 정의해야 합니다. (위반 수: "
                                + violationCount
                                + ")");
            }
        }

        @Test
        @DisplayName("[금지] UseCase는 JPA Entity를 반환하지 않아야 한다")
        void useCase_MustNotReturnJpaEntity() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..port.in..")
                            .should()
                            .haveRawReturnType(resideInAPackage("..persistence.."))
                            .because("UseCase는 JPA Entity를 반환하지 않아야 합니다");

            rule.check(classes);
        }
    }

    // ========================================
    // 의존성 규칙
    // ========================================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] UseCase는 Domain Layer와 DTO만 의존해야 한다")
        void useCase_MustOnlyDependOnDomainAndDto() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .should()
                            .onlyAccessClassesThat()
                            .resideInAnyPackage(
                                    "com.ryuqq.domain..",
                                    "com.ryuqq.application..dto..",
                                    "com.ryuqq.application..port..",
                                    "java..",
                                    "jakarta..")
                            .because("UseCase는 Domain Layer와 DTO만 의존해야 합니다 (Infrastructure 의존 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] UseCase는 Persistence Layer를 의존하지 않아야 한다")
        void useCase_MustNotDependOnPersistence() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .should()
                            .accessClassesThat()
                            .resideInAPackage("..persistence..")
                            .because("UseCase는 Persistence Layer를 직접 의존하지 않아야 합니다 (Port 사용)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] UseCase는 REST API Layer를 의존하지 않아야 한다")
        void useCase_MustNotDependOnRestApi() {
            assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("UseCase")
                            .and()
                            .resideInAPackage("..port.in..")
                            .should()
                            .accessClassesThat()
                            .resideInAPackage("..rest..")
                            .because("UseCase는 REST API Layer를 의존하지 않아야 합니다 (의존성 역전)");

            rule.check(classes);
        }
    }

    // ========================================
    // DTO 패키지 분리 규칙
    // ========================================

    @Nested
    @DisplayName("DTO 패키지 분리 규칙")
    class DtoPackageRules {

        @Test
        @DisplayName("[필수] Command는 dto.command 패키지에 위치해야 한다")
        void command_MustBeInDtoCommandPackage() {
            boolean hasCommandDtoClasses =
                    classes.stream()
                            .anyMatch(
                                    javaClass ->
                                            javaClass.getSimpleName().endsWith("Command")
                                                    && javaClass.isRecord());

            assumeTrue(hasCommandDtoClasses, "Command 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Command")
                            .and()
                            .areRecords()
                            .and()
                            .resideInAPackage("..application..")
                            .should()
                            .resideInAPackage("..dto.command..")
                            .because("Command DTO는 dto.command 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Query는 dto.query 패키지에 위치해야 한다")
        void query_MustBeInDtoQueryPackage() {
            boolean hasQueryDtoClasses =
                    classes.stream()
                            .anyMatch(
                                    javaClass ->
                                            javaClass.getSimpleName().endsWith("Query")
                                                    && javaClass.isRecord());

            assumeTrue(hasQueryDtoClasses, "Query 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Query")
                            .and()
                            .areRecords()
                            .and()
                            .resideInAPackage("..application..")
                            .should()
                            .resideInAPackage("..dto.query..")
                            .because("Query DTO는 dto.query 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Response는 dto.response 패키지에 위치해야 한다")
        void response_MustBeInDtoResponsePackage() {
            boolean hasResponseDtoClasses =
                    classes.stream()
                            .anyMatch(
                                    javaClass ->
                                            javaClass.getSimpleName().endsWith("Response")
                                                    && javaClass.isRecord()
                                                    && javaClass
                                                            .getPackageName()
                                                            .contains(".application."));

            assumeTrue(hasResponseDtoClasses, "Response 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Response")
                            .and()
                            .areRecords()
                            .and()
                            .resideInAPackage("..application..")
                            .should()
                            .resideInAPackage("..dto.response..")
                            .because("Response DTO는 dto.response 패키지에 위치해야 합니다");

            rule.check(classes);
        }
    }
}
