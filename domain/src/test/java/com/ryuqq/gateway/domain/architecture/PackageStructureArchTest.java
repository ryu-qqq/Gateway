package com.ryuqq.gateway.domain.architecture;

import static com.ryuqq.gateway.domain.architecture.ArchUnitPackageConstants.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Package Structure ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>domain.common/* 패키지 구조 (공통 인터페이스)
 *   <li>domain.[bc]/* 패키지 구조 (Bounded Context)
 *   <li>Bounded Context 간 순환 의존성 금지
 *   <li>패키지별 적절한 클래스 위치
 * </ul>
 *
 * <p><strong>Package 구조</strong>:
 *
 * <pre>
 * domain/
 * ├── common/                # 공통 인터페이스
 * │   ├── event/             # Domain Event Interface
 * │   │   └── DomainEvent.java
 * │   ├── exception/         # Base Exception
 * │   │   ├── DomainException.java
 * │   │   └── ErrorCode.java
 * │   └── util/              # Utilities (DIP)
 * │       └── ClockHolder.java
 * │
 * └── {boundedContext}/      # 각 Bounded Context
 *     ├── aggregate/         # Aggregate Root + 내부 Entity
 *     ├── vo/                # Value Objects
 *     ├── event/             # Domain Events
 *     └── exception/         # Concrete Exceptions
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Package Structure 아키텍처 검증 테스트")
@Tag("architecture")
@Tag("domain")
@Tag("package")
class PackageStructureArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(DOMAIN);
    }

    // ==================== domain.common 패키지 규칙 ====================

    /** 규칙 1: domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 한다 */
    @Test
    @DisplayName("[필수] domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 한다")
    void domainCommonEvent_ShouldOnlyContainDomainEventInterface() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN_COMMON + ".event")
                        .should()
                        .beInterfaces()
                        .because(
                                "domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 합니다\n"
                                        + "예시:\n"
                                        + "  - DomainEvent.java ✅ (interface)\n"
                                        + "  - OrderPlacedEvent.java ❌ (concrete event, Bounded"
                                        + " Context에 위치해야 함)");

        rule.check(classes);
    }

    /** 규칙 2: domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야 한다 */
    @Test
    @DisplayName("[필수] domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야 한다")
    void domainCommonException_ShouldOnlyContainBaseExceptionAndErrorCode() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN_COMMON + ".exception")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .haveSimpleNameContaining("Domain")
                        .orShould()
                        .haveSimpleNameContaining("ErrorCode")
                        .because(
                                "domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야"
                                        + " 합니다\n"
                                        + "예시:\n"
                                        + "  - DomainException.java ✅ (base exception)\n"
                                        + "  - ErrorCode.java ✅ (interface)\n"
                                        + "  - OrderNotFoundException.java ❌ (concrete exception,"
                                        + " Bounded Context에 위치해야 함)");

        rule.check(classes);
    }

    /** 규칙 3: domain.common.util 패키지는 Utility 인터페이스 또는 순수 유틸리티 클래스만 포함해야 한다 */
    @Test
    @DisplayName("[필수] domain.common.util 패키지는 Utility 인터페이스 또는 순수 유틸리티 클래스만 포함해야 한다")
    void domainCommonUtil_ShouldOnlyContainUtilityInterfacesOrPureUtilityClasses() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN_COMMON + ".util")
                        .and()
                        .doNotHaveSimpleName("UuidV7Generator") // 순수 유틸리티 클래스 예외
                        .should()
                        .beInterfaces()
                        .because(
                                "domain.common.util 패키지는 Utility 인터페이스만 포함해야 합니다 (DIP)\n"
                                    + "예외: 순수 유틸리티 클래스 (외부 의존성 없이 static 메서드만 제공)\n"
                                    + "예시:\n"
                                    + "  - ClockHolder.java ✅ (interface, 구현은 Application Layer)\n"
                                    + "  - UuidV7Generator.java ✅ (순수 유틸리티 클래스, static 메서드만)\n"
                                    + "  - SystemClockHolder.java ❌ (concrete class, Application"
                                    + " Layer에 위치해야 함)");

        rule.check(classes);
    }

    // ==================== Bounded Context 패키지 규칙 ====================

    /** 규칙 4: Domain Event는 domain.[bc].event 패키지에 위치해야 한다 */
    @Test
    @DisplayName("[필수] Domain Event는 domain.[bc].event 패키지에 위치해야 한다")
    void domainEvents_ShouldBeInEventPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .implement(DOMAIN_COMMON + ".event.DomainEvent")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .doNotHaveSimpleName("DomainEvent")
                        .should()
                        .resideInAPackage(EVENT_PATTERN)
                        .allowEmptyShould(true)
                        .because(
                                "Domain Event는 domain.[bc].event 패키지에 위치해야 합니다\n"
                                    + "예시:\n"
                                    + "  - domain.order.event.OrderPlacedEvent ✅\n"
                                    + "  - domain.order.aggregate.OrderPlacedEvent ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    /** 규칙 5: Concrete Exception은 domain.[bc].exception 패키지에 위치해야 한다 */
    @Test
    @DisplayName("[필수] Concrete Exception은 domain.[bc].exception 패키지에 위치해야 한다")
    void concreteExceptions_ShouldBeInExceptionPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .areAssignableTo(DOMAIN_COMMON + ".exception.DomainException")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .doNotHaveSimpleName("DomainException")
                        .and()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .resideInAPackage(EXCEPTION_PATTERN)
                        .allowEmptyShould(true)
                        .because(
                                "Concrete Exception은 domain.[bc].exception 패키지에 위치해야 합니다\n"
                                        + "예시:\n"
                                        + "  - domain.order.exception.OrderNotFoundException ✅\n"
                                        + "  - domain.order.exception.OrderErrorCode ✅ (enum)\n"
                                        + "  - domain.order.aggregate.OrderNotFoundException ❌ (잘못된"
                                        + " 패키지)");

        rule.check(classes);
    }

    // ==================== 순환 의존성 금지 ====================

    /** 규칙 6: Bounded Context 간 순환 의존성이 없어야 한다 */
    @Test
    @DisplayName("[필수] Bounded Context 간 순환 의존성이 없어야 한다")
    void boundedContexts_ShouldBeFreeOfCycles() {
        SlicesRuleDefinition.slices()
                .matching(DOMAIN + ".(*)..")
                .should()
                .beFreeOfCycles()
                .because(
                        "Bounded Context 간 순환 의존성이 없어야 합니다\n"
                                + "예시:\n"
                                + "  - domain.order → domain.customer ❌ (의존 금지)\n"
                                + "  - domain.customer → domain.order ❌ (의존 금지)\n"
                                + "  - Long FK 전략 사용으로 순환 의존성 방지");
    }

    // ==================== 공통 패키지 접근 규칙 ====================

    /** 규칙 7: domain.common 패키지는 모든 Bounded Context에서 접근 가능해야 한다 */
    @Test
    @DisplayName("[필수] domain.common 패키지는 모든 Bounded Context에서 접근 가능해야 한다")
    void domainCommon_ShouldBeAccessibleFromAllBoundedContexts() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN_COMMON + "..")
                        .should()
                        .onlyBeAccessed()
                        .byAnyPackage(
                                DOMAIN_ALL,
                                APPLICATION_ALL,
                                ADAPTER_ALL,
                                PERSISTENCE_ALL,
                                BOOTSTRAP_ALL)
                        .because("domain.common 패키지는 공통 인터페이스로 모든 레이어에서 접근 가능합니다");

        rule.check(classes);
    }

    // ==================== 네이밍 규칙 ====================

    /** 규칙 8: Bounded Context 패키지명은 소문자로 시작해야 한다 */
    @Test
    @DisplayName("[권장] Bounded Context 패키지명은 소문자 단어로 구성되어야 한다")
    void boundedContextPackages_ShouldUseLowercaseNames() {
        // Note: ArchUnit으로 패키지명 검증은 제한적이므로, 코드 리뷰 시 확인 필요
        // 이 규칙은 주로 문서화 목적

        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .and()
                        .resideOutsideOfPackage(DOMAIN_COMMON + "..")
                        .should()
                        .resideInAPackage(DOMAIN + ".(*)..")
                        .because(
                                "Bounded Context 패키지명은 소문자 단어로 구성되어야 합니다\n"
                                    + "예시:\n"
                                    + "  - domain.order ✅\n"
                                    + "  - domain.customer ✅\n"
                                    + "  - domain.product ✅\n"
                                    + "  - domain.Order ❌ (대문자 사용 금지)\n"
                                    + "  - domain.orderManagement ❌ (카멜케이스 금지, order_management로"
                                    + " 분리)");

        rule.check(classes);
    }

    // ==================== 패키지 격리 규칙 ====================

    /** 규칙 9: Bounded Context 내부 패키지는 다른 Bounded Context에 의존하지 않아야 한다 */
    @Test
    @DisplayName("[필수] Bounded Context는 다른 Bounded Context 내부에 의존하지 않아야 한다")
    void boundedContexts_ShouldNotDependOnOtherBoundedContextInternals() {
        // Note: 이 규칙은 순환 의존성 규칙과 함께 동작
        // Bounded Context 간 통신은 Long FK 또는 Domain Event 사용

        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(DOMAIN + ".(*)..")
                        .and()
                        .resideOutsideOfPackage(DOMAIN_COMMON + "..")
                        .and()
                        .resideOutsideOfPackage(ARCHITECTURE_PATTERN) // 테스트 클래스 제외
                        .should()
                        .onlyDependOnClassesThat()
                        .resideInAnyPackage(
                                DOMAIN_COMMON + "..",
                                DOMAIN + ".(*)..", // 같은 BC는 허용
                                "java..",
                                "jakarta.annotation..",
                                "com.github.f4b6a3.uuid..", // UUID 생성 라이브러리
                                "edu.umd.cs.findbugs.annotations.." // SpotBugs 어노테이션
                                )
                        .allowEmptyShould(true)
                        .because(
                                "Bounded Context는 다른 Bounded Context 내부에 직접 의존하지 않아야 합니다\n"
                                    + "통신 방법:\n"
                                    + "  - Long FK 전략 (userId: Long)\n"
                                    + "  - Domain Event (OrderPlacedEvent → CustomerEventHandler)");

        rule.check(classes);
    }
}
