package com.ryuqq.connectly.adapter.out.persistence.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * DataAccessPatternArchTest - Data Access 패턴 일관성 검증
 *
 * <p>Persistence Layer의 데이터 접근 패턴 일관성을 검증합니다:
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: QueryDslRepository는 JPAQueryFactory 필드 필수
 *   <li>규칙 2: QueryDslRepository는 QType static final 필드 필수
 *   <li>규칙 3: QueryAdapter는 QueryDslRepository 의존 필수
 *   <li>규칙 4: CommandAdapter는 JpaRepository 의존 필수
 *   <li>규칙 5: QueryDslRepository는 DTO Projection 사용 (Entity 반환 금지)
 *   <li>규칙 6: Repository는 Domain 반환 금지 (DTO만 반환)
 *   <li>규칙 7: Test Fixtures는 fixture() 메서드 패턴 사용
 *   <li>규칙 8: Test Fixtures는 Builder 패턴 금지
 * </ul>
 *
 * <p><strong>참고:</strong>
 *
 * <ul>
 *   <li>N+1 문제 예방: Join 사용은 수동 코드 리뷰로 검증 필요
 *   <li>DTO Projection: Projections.constructor() 사용 권장
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Data Access 패턴 일관성 검증 (Zero-Tolerance)")
@Tag("architecture")
class DataAccessPatternArchTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter().importPackages("com.ryuqq.adapter.out.persistence");
    }

    /** 규칙 1: QueryDslRepository는 JPAQueryFactory 필드 필수 */
    @Test
    @DisplayName("[필수] QueryDslRepository는 JPAQueryFactory 필드를 가져야 한다")
    void queryDslRepository_MustHaveJPAQueryFactoryField() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("QueryDslRepository")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleName("JPAQueryFactory")
                        .because("QueryDslRepository는 JPAQueryFactory 필드가 필수입니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 2: QueryDslRepository는 QType static final 필드 필수 */
    @Test
    @DisplayName("[필수] QueryDslRepository는 QType static final 필드를 가져야 한다")
    void queryDslRepository_MustHaveQTypeStaticField() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("QueryDslRepository")
                        .and()
                        .areStatic()
                        .and()
                        .areFinal()
                        .should()
                        .haveRawType(
                                com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "Q-type class",
                                        javaClass -> javaClass.getSimpleName().startsWith("Q")))
                        .because("QueryDslRepository는 QType static final 필드가 필수입니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 3: QueryAdapter는 QueryDslRepository 의존 필수 */
    @Test
    @DisplayName("[필수] QueryAdapter는 QueryDslRepository를 의존해야 한다")
    void queryAdapter_MustDependOnQueryDslRepository() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("QueryAdapter")
                        .and()
                        .haveSimpleNameNotContaining("Lock")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleNameEndingWith("QueryDslRepository")
                        .because("QueryAdapter는 QueryDslRepository를 의존해야 합니다 (CQRS Query 패턴)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 4: CommandAdapter는 JpaRepository 의존 필수 */
    @Test
    @DisplayName("[필수] CommandAdapter는 JpaRepository를 의존해야 한다")
    void commandAdapter_MustDependOnJpaRepository() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .dependOnClassesThat()
                        .areAssignableTo(
                                org.springframework.data.jpa.repository.JpaRepository.class)
                        .because("CommandAdapter는 JpaRepository를 의존해야 합니다 (CQRS Command 패턴)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 5: QueryDslRepository는 DTO Projection 사용 (Entity 반환 금지) */
    @Test
    @DisplayName("[권장] QueryDslRepository는 JpaEntity를 반환하지 않아야 한다")
    void queryDslRepository_ShouldNotReturnJpaEntity() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("QueryDslRepository")
                        .and()
                        .arePublic()
                        .should()
                        .haveRawReturnType(
                                com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "JPA Entity class",
                                        javaClass ->
                                                javaClass.getSimpleName().endsWith("JpaEntity")))
                        .because(
                                "QueryDslRepository는 JpaEntity 대신 DTO Projection을 사용해야 합니다 (N+1"
                                        + " 예방)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 6: Repository는 Domain 반환 금지 (DTO만 반환) */
    @Test
    @DisplayName("[금지] Repository는 Domain을 직접 반환하지 않아야 한다")
    void repository_MustNotReturnDomain() {
        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameContaining("Repository")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .arePublic()
                        .should()
                        .haveRawReturnType(
                                com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "Domain class from ..domain.. package",
                                        javaClass ->
                                                javaClass.getPackageName().contains(".domain.")
                                                        && !javaClass
                                                                .getSimpleName()
                                                                .endsWith("Dto")
                                                        && !javaClass
                                                                .getSimpleName()
                                                                .endsWith("JpaEntity")))
                        .because("Repository는 Domain을 직접 반환하면 안 됩니다 (DTO 변환은 Mapper가 담당)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 7: Test Fixtures는 fixture() 메서드 패턴 사용 */
    @Test
    @DisplayName("[권장] Test Fixtures는 fixture() 메서드를 제공해야 한다")
    void testFixtures_ShouldProvideFixtureMethod() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("Fixture")
                        .and()
                        .arePublic()
                        .and()
                        .areStatic()
                        .should()
                        .haveNameMatching(".*fixture.*")
                        .because("Test Fixtures는 fixture() 메서드 패턴을 사용해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 8: Test Fixtures는 Builder 패턴 금지 */
    @Test
    @DisplayName("[금지] Test Fixtures는 Builder 패턴을 사용하지 않아야 한다")
    void testFixtures_MustNotUseBuilderPattern() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("Fixture")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleNameContaining("Builder")
                        .because("Test Fixtures는 Builder 패턴 대신 fixture() 메서드를 사용해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 9: Adapter는 Mapper를 통해 변환해야 함 */
    @Test
    @DisplayName("[필수] Adapter는 Mapper를 의존해야 한다")
    void adapter_MustDependOnMapper() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Adapter")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleNameEndingWith("Mapper")
                        .because("Adapter는 Entity ↔ Domain 변환을 위해 Mapper를 의존해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 10: QueryDslRepository는 정확히 4개 표준 메서드만 허용 */
    @Test
    @DisplayName("[필수] QueryDslRepository는 표준 메서드만 허용한다")
    void queryDslRepository_MustOnlyHaveStandardMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("QueryDslRepository")
                        .and()
                        .arePublic()
                        .and()
                        .areNotStatic()
                        .should()
                        .haveName("findById")
                        .orShould()
                        .haveName("existsById")
                        .orShould()
                        .haveName("findByCriteria")
                        .orShould()
                        .haveName("countByCriteria")
                        .because(
                                "QueryDslRepository는 4개 표준 메서드만 허용합니다 (findById, existsById,"
                                        + " findByCriteria, countByCriteria)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 11: Adapter는 JPAQueryFactory를 직접 사용 금지 */
    @Test
    @DisplayName("[금지] Adapter는 JPAQueryFactory를 직접 사용하지 않아야 한다")
    void adapter_MustNotUseJPAQueryFactoryDirectly() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("Adapter")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleName("JPAQueryFactory")
                        .because(
                                "Adapter는 JPAQueryFactory를 직접 사용하면 안 됩니다 (QueryDslRepository를 통해서만"
                                        + " 접근)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 12: Config 클래스는 @Configuration 필수 */
    @Test
    @DisplayName("[필수] Config 클래스는 @Configuration 어노테이션을 가져야 한다")
    void config_MustHaveConfigurationAnnotation() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Config")
                        .and()
                        .resideInAPackage("..config..")
                        .should()
                        .beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                        .because("Config 클래스는 @Configuration 어노테이션이 필수입니다");

        rule.allowEmptyShould(true).check(allClasses);
    }
}
