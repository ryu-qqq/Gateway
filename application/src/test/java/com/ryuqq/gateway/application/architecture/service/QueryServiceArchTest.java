package com.ryuqq.gateway.application.architecture.service;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * QueryService ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: QueryService는 UseCase 구현체로 조율만 수행, 읽기 전용
 *
 * <h3>QueryFactory 의존성 규칙:</h3>
 *
 * <ul>
 *   <li>복잡한 검색 조건을 다루는 QueryService는 QueryFactory를 의존해야 합니다
 *   <li>QueryFactory는 Query DTO → Domain Criteria 변환을 담당합니다
 *   <li>단순 ID 조회는 Domain VO를 직접 사용할 수 있습니다
 * </ul>
 *
 * <h3>호출 흐름 (복잡한 검색):</h3>
 *
 * <pre>
 * Controller → QueryService → QueryFactory → Domain Criteria
 *                          └→ ReadManager → QueryPort
 * </pre>
 *
 * <h3>호출 흐름 (단순 ID 조회):</h3>
 *
 * <pre>
 * Controller → QueryService → Domain VO (ex: OrderId)
 *                          └→ ReadManager → QueryPort
 * </pre>
 */
@DisplayName("QueryService ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("service")
class QueryServiceArchTest {

    private static JavaClasses classes;
    private static boolean hasQueryServiceClasses;
    private static List<JavaClass> queryServiceClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        queryServiceClasses =
                classes.stream()
                        .filter(javaClass -> javaClass.getPackageName().contains("service.query"))
                        .filter(javaClass -> javaClass.getSimpleName().endsWith("Service"))
                        .filter(javaClass -> !javaClass.isInterface())
                        .collect(Collectors.toList());

        hasQueryServiceClasses = !queryServiceClasses.isEmpty();
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] service.query 패키지의 Service는 @Service 어노테이션을 가져야 한다")
        void queryService_MustHaveServiceAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(Service.class)
                .because("QueryService는 @Service 어노테이션을 가져야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] service.query 패키지의 클래스는 'Service' 접미사를 가져야 한다")
        void queryService_MustHaveCorrectSuffix() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().areNotInterfaces()
                .and().areNotAnonymousClasses()
                .should().haveSimpleNameEndingWith("Service")
                .because("Query Service는 'Service' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 final 클래스가 아니어야 한다")
        void queryService_MustNotBeFinal() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().notHaveModifier(FINAL)
                .because("Spring 프록시 생성을 위해 QueryService가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] QueryService는 @Transactional을 가지지 않아야 한다")
        void queryService_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("QueryService는 @Transactional을 가지지 않아야 합니다. " +
                         "읽기 전용이므로 트랜잭션이 필요하지 않습니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 @Component 어노테이션을 가지지 않아야 한다")
        void queryService_MustNotHaveComponentAnnotation() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("org.springframework.stereotype.Component")
                .because("QueryService는 @Component가 아닌 @Service를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Repository를 직접 의존하지 않아야 한다")
        void queryService_MustNotDependOnRepositories() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("QueryService는 Repository를 직접 의존하지 않아야 합니다. " +
                         "ReadManager/QueryFacade를 통해 접근합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Lombok 어노테이션을 가지지 않아야 한다")
        void queryService_MustNotUseLombok() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .because("QueryService는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 Command 관련 클래스를 의존하지 않아야 한다")
        void queryService_MustNotDependOnCommandClasses() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().haveSimpleNameContaining("Command")
                .because("QueryService는 Command 관련 클래스를 의존하지 않아야 합니다. " +
                         "CQRS 분리 원칙을 준수해야 합니다.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] QueryService는 Application Layer와 Domain Layer만 의존해야 한다")
        void queryService_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.ryuqq.application..",
                    "com.ryuqq.domain..",
                    "org.springframework..",
                    "java..",
                    "jakarta.."
                )
                .because("QueryService는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 Adapter Layer를 의존하지 않아야 한다")
        void queryService_MustNotDependOnAdapterLayer() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().resideInAPackage("..application..service.query..")
                .and().haveSimpleNameEndingWith("Service")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .because("QueryService는 Adapter Layer를 의존하지 않아야 합니다");

            rule.check(classes);
        }
    }

    // ==================== Factory 의존성 규칙 ====================

    @Nested
    @DisplayName("Factory 의존성 규칙")
    class FactoryDependencyRules {

        @Test
        @DisplayName("[권장] 복잡한 검색을 다루는 QueryService는 QueryFactory를 의존해야 한다")
        void queryService_ShouldDependOnQueryFactoryForComplexQueries() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            // 복잡한 검색을 다루는 Service (Search, List 등의 메서드를 가진 Service)
            for (JavaClass queryService : queryServiceClasses) {
                boolean hasComplexQueryMethods =
                        queryService.getMethods().stream()
                                .anyMatch(
                                        method ->
                                                method.getName().startsWith("search")
                                                        || method.getName().startsWith("list")
                                                        || method.getName().contains("ByCondition"));

                if (hasComplexQueryMethods) {
                    boolean hasQueryFactory =
                            queryService.getFields().stream()
                                    .anyMatch(
                                            field ->
                                                    field.getRawType()
                                                            .getSimpleName()
                                                            .endsWith("QueryFactory"));

                    if (!hasQueryFactory) {
                        // 권장 사항이므로 경고만 출력 (fail 대신)
                        System.out.println(
                                "[권장] "
                                        + queryService.getSimpleName()
                                        + "는 복잡한 검색 메서드를 가지고 있습니다. "
                                        + "QueryFactory를 의존하여 Query DTO → Domain Criteria 변환을 위임하세요.");
                    }
                }
            }
        }

        @Test
        @DisplayName("[필수] QueryService는 ReadManager를 통해서만 QueryPort를 호출해야 한다")
        void queryService_MustUseReadManagerForQueryPort() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..service.query..")
                            .and()
                            .haveSimpleNameEndingWith("Service")
                            .should()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("QueryPort")
                            .because(
                                    "QueryService는 QueryPort를 직접 의존하지 않아야 합니다. "
                                            + "ReadManager를 통해 접근하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] QueryService는 QueryFactory 또는 ReadManager를 통해서만 Domain을 다뤄야 한다")
        void queryService_MustUseFactoryOrManagerForDomain() {
            assumeTrue(hasQueryServiceClasses, "QueryService 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..application..service.query..")
                            .and()
                            .haveSimpleNameEndingWith("Service")
                            .should()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("QueryFactory")
                            .orShould()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("ReadManager")
                            .orShould()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("Facade")
                            .because(
                                    "QueryService는 QueryFactory, ReadManager 또는 Facade를 통해서만 Domain을 다뤄야 합니다.");

            rule.check(classes);
        }
    }
}
