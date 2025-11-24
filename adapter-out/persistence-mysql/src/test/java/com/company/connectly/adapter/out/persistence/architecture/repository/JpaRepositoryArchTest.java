package com.company.connectly.adapter.out.persistence.architecture.repository;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * JpaRepositoryArchTest - JPA Repository 아키텍처 규칙 검증
 *
 * <p>jpa-repository-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: JpaRepository는 인터페이스여야 함</li>
 *   <li>규칙 2: JpaRepository 상속 필수</li>
 *   <li>규칙 3: QuerydslPredicateExecutor 상속 금지</li>
 *   <li>규칙 4: Query Method 추가 금지</li>
 *   <li>규칙 5: @Query 어노테이션 사용 금지</li>
 *   <li>규칙 6: Custom Repository 구현 금지</li>
 *   <li>규칙 7: 네이밍 규칙 (*Repository)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("JPA Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class JpaRepositoryArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses jpaRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");

        // JpaRepository 인터페이스만 (QueryDsl 제외)
        jpaRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "are JpaRepository interfaces",
                javaClass -> javaClass.getSimpleName().endsWith("Repository") &&
                    !javaClass.getSimpleName().contains("QueryDsl") &&
                    javaClass.isInterface()
            )
        );
    }

    @Test
    @DisplayName("규칙 1: JpaRepository는 인터페이스여야 함")
    void jpaRepository_MustBeInterface() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().resideInAPackage("..repository..")
            .should().beInterfaces()
            .because("JpaRepository는 인터페이스로 정의되어야 합니다");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 2: JpaRepository 상속 필수")
    void jpaRepository_MustExtendJpaRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().areInterfaces()
            .should().beAssignableTo(JpaRepository.class)
            .because("JpaRepository 인터페이스를 상속해야 합니다");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 3: QuerydslPredicateExecutor 상속 금지")
    void jpaRepository_MustNotExtendQuerydslPredicateExecutor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().areInterfaces()
            .should().notBeAssignableTo(QuerydslPredicateExecutor.class)
            .because("JpaRepository는 QuerydslPredicateExecutor 상속이 금지됩니다 (순수 Command 전용)");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 4: Query Method 추가 금지")
    void jpaRepository_MustNotHaveQueryMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
            .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("QueryDsl")
            .and().areDeclaredInClassesThat().areInterfaces()
            .and().arePublic()
            .should().haveNameMatching("find.*|search.*|count.*|exists.*|get.*")
            .because("JpaRepository는 Query Method 추가가 금지됩니다 (QueryDslRepository 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 5: @Query 어노테이션 사용 금지")
    void jpaRepository_MustNotUseQueryAnnotation() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
            .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("QueryDsl")
            .and().areDeclaredInClassesThat().areInterfaces()
            .should().notBeAnnotatedWith(Query.class)
            .because("JpaRepository는 @Query 어노테이션 사용이 금지됩니다 (QueryDSL 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 6: Custom Repository 구현 금지")
    void jpaRepository_MustNotHaveCustomImplementation() {
        ArchRule rule = classes()
            .that(DescribedPredicate.describe("have name matching '.*RepositoryImpl'",
                javaClass -> javaClass.getSimpleName().matches(".*RepositoryImpl")))
            .and().resideInAPackage("..repository..")
            .should().haveSimpleNameNotEndingWith("RepositoryImpl")
            .because("Custom Repository 구현이 금지됩니다 (QueryDslRepository 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 7: JpaRepository 네이밍 규칙 (*Repository)")
    void jpaRepository_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areInterfaces()
            .and().areAssignableTo(JpaRepository.class)
            .and().resideInAPackage("..repository..")
            .should().haveSimpleNameEndingWith("Repository")
            .because("JpaRepository는 *Repository 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }
}
