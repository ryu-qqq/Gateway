package com.company.connectly.adapter.out.persistence.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * PersistenceLayerArchTest - Persistence Layer 전체 아키텍처 규칙 검증
 *
 * <p>Persistence Layer의 핵심 아키텍처 규칙을 검증합니다:</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: Package 구조 검증 (adapter, entity, repository, mapper)</li>
 *   <li>규칙 2: Port 구현 검증 (CommandPort, QueryPort, LockQueryPort)</li>
 *   <li>규칙 3: JPA Entity와 Domain 분리 검증</li>
 *   <li>규칙 4: Layer 의존성 검증 (단방향 의존성)</li>
 *   <li>규칙 5: Application Layer 의존 금지</li>
 *   <li>규칙 6: Domain Layer 의존 금지 (Port를 통해서만)</li>
 *   <li>규칙 7: Adapter 네이밍 규칙 (*CommandAdapter, *QueryAdapter)</li>
 *   <li>규칙 8: Repository 네이밍 규칙 (*Repository, *QueryDslRepository)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Persistence Layer 아키텍처 규칙 검증 (Zero-Tolerance)")
@Tag("architecture")
class PersistenceLayerArchTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");
    }

    /**
     * 규칙 1: Package 구조 검증
     */
    @Test
    @DisplayName("[필수] Adapter는 ..adapter.. 패키지에 위치해야 한다")
    void persistence_AdaptersMustBeInAdapterPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Adapter")
            .should().resideInAPackage("..adapter..")
            .because("Adapter 클래스는 adapter 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] Entity는 ..entity.. 패키지에 위치해야 한다")
    void persistence_EntitiesMustBeInEntityPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("JpaEntity")
            .should().resideInAPackage("..entity..")
            .because("JPA Entity 클래스는 entity 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] Repository는 ..repository.. 패키지에 위치해야 한다")
    void persistence_RepositoriesMustBeInRepositoryPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("Repository")
            .should().resideInAPackage("..repository..")
            .because("Repository 인터페이스/클래스는 repository 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] Mapper는 ..mapper.. 패키지에 위치해야 한다")
    void persistence_MappersMustBeInMapperPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().resideInAPackage("..mapper..")
            .because("Mapper 클래스는 mapper 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 2: Port 구현 검증
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 CommandPort를 구현해야 한다")
    void persistence_CommandAdapterMustImplementCommandPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "CommandPort interface",
                javaClass -> javaClass.getAllRawInterfaces().stream()
                    .anyMatch(i -> i.getSimpleName().endsWith("CommandPort"))
            ))
            .because("CommandAdapter는 CommandPort 인터페이스를 구현해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] QueryAdapter는 QueryPort를 구현해야 한다")
    void persistence_QueryAdapterMustImplementQueryPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .and().haveSimpleNameNotContaining("Lock")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "QueryPort interface",
                javaClass -> javaClass.getAllRawInterfaces().stream()
                    .anyMatch(i -> i.getSimpleName().endsWith("QueryPort") && !i.getSimpleName().contains("Lock"))
            ))
            .because("QueryAdapter는 QueryPort 인터페이스를 구현해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] LockQueryAdapter는 LockQueryPort를 구현해야 한다")
    void persistence_LockQueryAdapterMustImplementLockQueryPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("LockQueryAdapter")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "LockQueryPort interface",
                javaClass -> javaClass.getAllRawInterfaces().stream()
                    .anyMatch(i -> i.getSimpleName().endsWith("LockQueryPort"))
            ))
            .because("LockQueryAdapter는 LockQueryPort 인터페이스를 구현해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 3: JPA Entity와 Domain 분리 검증
     */
    @Test
    @DisplayName("[필수] JPA Entity는 Domain Layer를 의존하지 않아야 한다")
    void persistence_JpaEntityMustNotDependOnDomain() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("JpaEntity")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..")
            .because("JPA Entity는 Domain Layer에 의존하면 안 됩니다 (Infrastructure → Domain 의존 금지)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] JPA Entity는 Application Layer를 의존하지 않아야 한다")
    void persistence_JpaEntityMustNotDependOnApplication() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("JpaEntity")
            .should().dependOnClassesThat().resideInAnyPackage("..application..")
            .because("JPA Entity는 Application Layer에 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    @Test
    @DisplayName("[필수] Domain은 JPA Entity를 의존하지 않아야 한다")
    void persistence_DomainMustNotDependOnJpaEntity() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("JpaEntity")
            .because("Domain은 JPA Entity에 의존하면 안 됩니다 (Clean Architecture 원칙)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 5: Application Layer 의존 금지
     */
    @Test
    @DisplayName("[금지] Persistence Layer는 Application Layer를 직접 의존하지 않아야 한다")
    void persistence_MustNotDependOnApplicationLayer() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..")
            .because("Persistence Layer는 Application Layer를 직접 의존하면 안 됩니다 (Port를 통해서만 접근)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 6: Domain Layer 의존 금지 (Adapter 제외)
     */
    @Test
    @DisplayName("[금지] Repository/Entity/Mapper는 Domain Layer를 직접 의존하지 않아야 한다")
    void persistence_RepositoryEntityMapperMustNotDependOnDomain() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..repository..", "..entity..", "..mapper..")
            .and().resideOutsideOfPackages("..architecture..")  // 테스트 제외
            .should().dependOnClassesThat().resideInAnyPackage("com.ryuqq.domain..")
            .because("Repository/Entity/Mapper는 Domain Layer를 직접 의존하면 안 됩니다 (Adapter만 Domain 접근 가능)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 7: Adapter 네이밍 규칙
     */
    @Test
    @DisplayName("[필수] Adapter는 *CommandAdapter 또는 *QueryAdapter 네이밍 규칙을 따라야 한다")
    void persistence_AdaptersMustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Adapter")
            .and().areNotInterfaces()
            .and().haveSimpleNameNotContaining("Test")
            .should().haveSimpleNameEndingWith("CommandAdapter")
            .orShould().haveSimpleNameEndingWith("QueryAdapter")
            .orShould().haveSimpleNameEndingWith("LockQueryAdapter")
            .because("Adapter는 *CommandAdapter 또는 *QueryAdapter 네이밍 규칙을 따라야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 8: Repository 네이밍 규칙
     */
    @Test
    @DisplayName("[필수] Repository는 *Repository 또는 *QueryDslRepository 네이밍 규칙을 따라야 한다")
    void persistence_RepositoriesMustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().resideInAPackage("..repository..")
            .and().haveSimpleNameNotContaining("Test")
            .should().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("QueryDslRepository")
            .because("Repository는 *Repository 또는 *QueryDslRepository 네이밍 규칙을 따라야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }
}
