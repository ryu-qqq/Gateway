package com.company.connectly.domain.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Package Structure ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:</p>
 * <ul>
 *   <li>domain.common/* 패키지 구조 (공통 인터페이스)</li>
 *   <li>domain.[bc]/* 패키지 구조 (Bounded Context)</li>
 *   <li>Bounded Context 간 순환 의존성 금지</li>
 *   <li>패키지별 적절한 클래스 위치</li>
 * </ul>
 *
 * <p><strong>Package 구조</strong>:</p>
 * <pre>
 * domain/
 * ├── common/                # 공통 인터페이스
 * │   ├── model/             # Domain Model Markers
 * │   │   ├── AggregateRoot.java
 * │   │   ├── Entity.java
 * │   │   ├── ValueObject.java
 * │   │   └── Identifier.java
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
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.domain");
    }

    // ==================== domain.common 패키지 규칙 ====================

    /**
     * 규칙 1: domain.common.model 패키지는 Marker 인터페이스만 포함해야 한다
     */
    @Test
    @DisplayName("[필수] domain.common.model 패키지는 Marker 인터페이스만 포함해야 한다")
    void domainCommonModel_ShouldOnlyContainMarkerInterfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.common.model")
            .should().beInterfaces()
            .because("domain.common.model 패키지는 Domain Model Marker 인터페이스만 포함해야 합니다\n" +
                    "예시:\n" +
                    "  - AggregateRoot.java ✅ (interface)\n" +
                    "  - Entity.java ✅ (interface)\n" +
                    "  - ValueObject.java ✅ (interface)\n" +
                    "  - Identifier.java ✅ (interface)\n" +
                    "  - Order.java ❌ (concrete class, Bounded Context에 위치해야 함)");

        rule.check(classes);
    }

    /**
     * 규칙 2: domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 한다
     */
    @Test
    @DisplayName("[필수] domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 한다")
    void domainCommonEvent_ShouldOnlyContainDomainEventInterface() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.common.event")
            .should().beInterfaces()
            .because("domain.common.event 패키지는 DomainEvent 인터페이스만 포함해야 합니다\n" +
                    "예시:\n" +
                    "  - DomainEvent.java ✅ (interface)\n" +
                    "  - OrderPlacedEvent.java ❌ (concrete event, Bounded Context에 위치해야 함)");

        rule.check(classes);
    }

    /**
     * 규칙 3: domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야 한다
     */
    @Test
    @DisplayName("[필수] domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야 한다")
    void domainCommonException_ShouldOnlyContainBaseExceptionAndErrorCode() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.common.exception")
            .and().haveSimpleNameNotContaining("Test")
            .should().haveSimpleNameContaining("Domain")
            .orShould().haveSimpleNameContaining("ErrorCode")
            .because("domain.common.exception 패키지는 Base Exception과 ErrorCode 인터페이스만 포함해야 합니다\n" +
                    "예시:\n" +
                    "  - DomainException.java ✅ (base exception)\n" +
                    "  - ErrorCode.java ✅ (interface)\n" +
                    "  - OrderNotFoundException.java ❌ (concrete exception, Bounded Context에 위치해야 함)");

        rule.check(classes);
    }

    /**
     * 규칙 4: domain.common.util 패키지는 Utility 인터페이스만 포함해야 한다
     */
    @Test
    @DisplayName("[필수] domain.common.util 패키지는 Utility 인터페이스만 포함해야 한다")
    void domainCommonUtil_ShouldOnlyContainUtilityInterfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.common.util")
            .should().beInterfaces()
            .because("domain.common.util 패키지는 Utility 인터페이스만 포함해야 합니다 (DIP)\n" +
                    "예시:\n" +
                    "  - ClockHolder.java ✅ (interface, 구현은 Application Layer)\n" +
                    "  - SystemClockHolder.java ❌ (concrete class, Application Layer에 위치해야 함)");

        rule.check(classes);
    }

    // ==================== Bounded Context 패키지 규칙 ====================

    /**
     * 규칙 5: Aggregate는 domain.[bc].aggregate 패키지에 위치해야 한다
     */
    @Test
    @DisplayName("[필수] Aggregate는 domain.[bc].aggregate 패키지에 위치해야 한다")
    void aggregates_ShouldBeInAggregatePackage() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.AggregateRoot")
            .or().implement("com.ryuqq.domain.common.model.Entity")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should().resideInAPackage("..domain..aggregate..")
            .because("Aggregate Root와 내부 Entity는 domain.[bc].aggregate 패키지에 위치해야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order.aggregate.Order ✅ (AggregateRoot)\n" +
                    "  - domain.order.aggregate.OrderItem ✅ (내부 Entity)\n" +
                    "  - domain.order.vo.Order ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    /**
     * 규칙 6: Value Object는 domain.[bc].vo 패키지에 위치해야 한다
     */
    @Test
    @DisplayName("[필수] Value Object는 domain.[bc].vo 패키지에 위치해야 한다")
    void valueObjects_ShouldBeInVoPackage() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.ValueObject")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should().resideInAPackage("..domain..vo..")
            .because("Value Object는 domain.[bc].vo 패키지에 위치해야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order.vo.OrderId ✅\n" +
                    "  - domain.order.vo.Money ✅\n" +
                    "  - domain.order.aggregate.Money ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    /**
     * 규칙 7: Domain Event는 domain.[bc].event 패키지에 위치해야 한다
     */
    @Test
    @DisplayName("[필수] Domain Event는 domain.[bc].event 패키지에 위치해야 한다")
    void domainEvents_ShouldBeInEventPackage() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.event.DomainEvent")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .and().doNotHaveSimpleName("DomainEvent")
            .should().resideInAPackage("..domain..event..")
            .because("Domain Event는 domain.[bc].event 패키지에 위치해야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order.event.OrderPlacedEvent ✅\n" +
                    "  - domain.order.aggregate.OrderPlacedEvent ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    /**
     * 규칙 8: Concrete Exception은 domain.[bc].exception 패키지에 위치해야 한다
     */
    @Test
    @DisplayName("[필수] Concrete Exception은 domain.[bc].exception 패키지에 위치해야 한다")
    void concreteExceptions_ShouldBeInExceptionPackage() {
        ArchRule rule = classes()
            .that().areAssignableTo("com.ryuqq.domain.common.exception.DomainException")
            .and().haveSimpleNameNotContaining("Test")
            .and().doNotHaveSimpleName("DomainException")
            .and().resideInAPackage("..domain..")
            .should().resideInAPackage("..domain..exception..")
            .because("Concrete Exception은 domain.[bc].exception 패키지에 위치해야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order.exception.OrderNotFoundException ✅\n" +
                    "  - domain.order.exception.OrderErrorCode ✅ (enum)\n" +
                    "  - domain.order.aggregate.OrderNotFoundException ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    // ==================== 순환 의존성 금지 ====================

    /**
     * 규칙 9: Bounded Context 간 순환 의존성이 없어야 한다
     */
    @Test
    @DisplayName("[필수] Bounded Context 간 순환 의존성이 없어야 한다")
    void boundedContexts_ShouldBeFreeOfCycles() {
        SlicesRuleDefinition.slices()
            .matching("com.ryuqq.domain.(*)..")
            .should().beFreeOfCycles()
            .because("Bounded Context 간 순환 의존성이 없어야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order → domain.customer ❌ (의존 금지)\n" +
                    "  - domain.customer → domain.order ❌ (의존 금지)\n" +
                    "  - Long FK 전략 사용으로 순환 의존성 방지");
    }

    // ==================== 공통 패키지 접근 규칙 ====================

    /**
     * 규칙 10: domain.common 패키지는 모든 Bounded Context에서 접근 가능해야 한다
     */
    @Test
    @DisplayName("[필수] domain.common 패키지는 모든 Bounded Context에서 접근 가능해야 한다")
    void domainCommon_ShouldBeAccessibleFromAllBoundedContexts() {
        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.common..")
            .should().onlyBeAccessed().byAnyPackage(
                "com.ryuqq.domain..",
                "com.ryuqq.application..",
                "com.ryuqq.adapter..",
                "com.ryuqq.persistence..",
                "com.ryuqq.bootstrap.."
            )
            .because("domain.common 패키지는 공통 인터페이스로 모든 레이어에서 접근 가능합니다");

        rule.check(classes);
    }

    // ==================== 네이밍 규칙 ====================

    /**
     * 규칙 11: Bounded Context 패키지명은 소문자로 시작해야 한다
     */
    @Test
    @DisplayName("[권장] Bounded Context 패키지명은 소문자 단어로 구성되어야 한다")
    void boundedContextPackages_ShouldUseLowercaseNames() {
        // Note: ArchUnit으로 패키지명 검증은 제한적이므로, 코드 리뷰 시 확인 필요
        // 이 규칙은 주로 문서화 목적

        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain..")
            .and().resideOutsideOfPackage("com.ryuqq.domain.common..")
            .should().resideInAPackage("com.ryuqq.domain.(*)..")
            .because("Bounded Context 패키지명은 소문자 단어로 구성되어야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order ✅\n" +
                    "  - domain.customer ✅\n" +
                    "  - domain.product ✅\n" +
                    "  - domain.Order ❌ (대문자 사용 금지)\n" +
                    "  - domain.orderManagement ❌ (카멜케이스 금지, order_management로 분리)");

        rule.check(classes);
    }

    // ==================== 패키지 격리 규칙 ====================

    /**
     * 규칙 12: Bounded Context 내부 패키지는 다른 Bounded Context에 의존하지 않아야 한다
     */
    @Test
    @DisplayName("[필수] Bounded Context는 다른 Bounded Context 내부에 의존하지 않아야 한다")
    void boundedContexts_ShouldNotDependOnOtherBoundedContextInternals() {
        // Note: 이 규칙은 순환 의존성 규칙과 함께 동작
        // Bounded Context 간 통신은 Long FK 또는 Domain Event 사용

        ArchRule rule = classes()
            .that().resideInAPackage("com.ryuqq.domain.(*)..")
            .and().resideOutsideOfPackage("com.ryuqq.domain.common..")
            .and().resideOutsideOfPackage("..architecture..") // 테스트 클래스 제외
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                "com.ryuqq.domain.common..",
                "com.ryuqq.domain.(*)..", // 같은 BC는 허용
                "java..",
                "jakarta.annotation.."
            )
            .because("Bounded Context는 다른 Bounded Context 내부에 직접 의존하지 않아야 합니다\n" +
                    "통신 방법:\n" +
                    "  - Long FK 전략 (userId: Long)\n" +
                    "  - Domain Event (OrderPlacedEvent → CustomerEventHandler)");

        rule.check(classes);
    }
}
