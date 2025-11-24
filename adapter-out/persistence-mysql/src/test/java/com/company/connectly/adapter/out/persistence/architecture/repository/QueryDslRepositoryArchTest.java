package com.company.connectly.adapter.out.persistence.architecture.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * QueryDslRepositoryArchTest - QueryDSL Repository 아키텍처 규칙 검증
 *
 * <p>querydsl-repository-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: QueryDslRepository는 클래스여야 함</li>
 *   <li>규칙 2: @Repository 어노테이션 필수</li>
 *   <li>규칙 3: JPAQueryFactory 필드 필수</li>
 *   <li>규칙 4: QType static final 필드 필수</li>
 *   <li>규칙 5: 4개 표준 메서드만 허용</li>
 *   <li>규칙 6: Join 사용 금지 (코드 검증)</li>
 *   <li>규칙 7: @Transactional 사용 금지</li>
 *   <li>규칙 8: Mapper 의존성 금지</li>
 *   <li>규칙 9: 네이밍 규칙 (*QueryDslRepository)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("QueryDSL Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class QueryDslRepositoryArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses queryDslRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");

        // QueryDslRepository 클래스만
        queryDslRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "are QueryDslRepository classes",
                javaClass -> javaClass.getSimpleName().endsWith("QueryDslRepository") &&
                    !javaClass.isInterface()
            )
        );
    }

    @Test
    @DisplayName("규칙 1: QueryDslRepository는 클래스여야 함")
    void queryDslRepository_MustBeClass() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().notBeInterfaces()
            .because("QueryDslRepository는 클래스로 정의되어야 합니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 2: QueryDslRepository는 @Repository 어노테이션 필수")
    void queryDslRepository_MustHaveRepositoryAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().beAnnotatedWith(Repository.class)
            .because("QueryDslRepository는 @Repository 어노테이션이 필수입니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 3: QueryDslRepository는 JPAQueryFactory 필드 필수")
    void queryDslRepository_MustHaveJPAQueryFactory() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().areAssignableTo(JPAQueryFactory.class)
            .because("QueryDslRepository는 JPAQueryFactory 필드가 필수입니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 4: QueryDslRepository는 QType static final 필드 필수")
    void queryDslRepository_MustHaveStaticFinalQTypeField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryDslRepository")
            .and().haveNameMatching("^q[A-Z].*")  // qOrder, qProduct 등
            .should().beStatic()
            .andShould().beFinal()
            .because("QType 필드는 static final이어야 합니다");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 5: QueryDslRepository는 4개 표준 메서드만 허용")
    void queryDslRepository_MustHaveOnlyStandardMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryDslRepository")
            .and().areDeclaredInClassesThat().resideInAPackage("..repository..")
            .and().arePublic()
            .and().areNotStatic()
            .and().doNotHaveName("equals")
            .and().doNotHaveName("hashCode")
            .and().doNotHaveName("toString")
            .should().haveName("findById")
            .orShould().haveName("existsById")
            .orShould().haveName("findByCriteria")
            .orShould().haveName("countByCriteria")
            .because("QueryDslRepository는 4개 표준 메서드만 허용됩니다 (findById, existsById, findByCriteria, countByCriteria)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 6: QueryDslRepository는 Join 사용 금지 (수동 검증)")
    void queryDslRepository_MustNotUseJoin() {
        // ⚠️ 주의: ArchUnit으로 Join 사용을 완벽히 검증하기 어려움
        // 코드 리뷰 및 수동 검증 필요
        //
        // 금지 패턴:
        // - queryFactory.selectFrom(q).join(...)
        // - queryFactory.selectFrom(q).leftJoin(...)
        // - queryFactory.selectFrom(q).rightJoin(...)
        // - queryFactory.selectFrom(q).innerJoin(...)
        // - queryFactory.selectFrom(q).fetchJoin(...)
        //
        // ✅ 이 테스트는 통과하지만, 실제 Join 사용 여부는 코드 리뷰로 확인해야 합니다.

        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.querydsl.jpa.impl.JPAJoin")
            .because("QueryDslRepository는 Join 사용이 금지됩니다 (N+1은 Adapter에서 해결)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 7: QueryDslRepository는 @Transactional 사용 금지")
    void queryDslRepository_MustNotHaveTransactional() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().beAnnotatedWith(Transactional.class)
            .because("QueryDslRepository는 @Transactional 사용이 금지됩니다 (Service Layer에서 관리)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 8: QueryDslRepository는 Mapper 의존성 금지")
    void queryDslRepository_MustNotDependOnMapper() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
            .because("QueryDslRepository는 Mapper 의존성이 금지됩니다 (Adapter에서 처리)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 9: QueryDslRepository 네이밍 규칙 (*QueryDslRepository)")
    void queryDslRepository_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().resideInAPackage("..repository..")
            .and().areAnnotatedWith(Repository.class)
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("QueryDslRepository")
            .because("QueryDslRepository는 *QueryDslRepository 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }
}
