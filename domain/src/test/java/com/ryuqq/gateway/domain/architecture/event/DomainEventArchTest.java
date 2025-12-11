package com.ryuqq.gateway.domain.architecture.event;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Domain Event ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>DomainEvent 인터페이스 구현 필수
 *   <li>Record 타입 필수 (불변성)
 *   <li>과거형 네이밍 (*edEvent, *dEvent, 불규칙 과거형)
 *   <li>occurredAt (Instant) 필드 필수
 *   <li>domain.[bc].event 패키지에 위치
 *   <li>Lombok, JPA, Spring 금지
 *   <li>불변성 보장 (final fields)
 * </ul>
 *
 * <p><strong>Domain Event 패턴</strong>:
 *
 * <pre>
 * public record OrderPlacedEvent(
 *     OrderId orderId,
 *     MemberId memberId,
 *     Money totalAmount,
 *     Instant occurredAt
 * ) implements DomainEvent {
 *     public static OrderPlacedEvent from(Order order, Instant occurredAt) {
 *         return new OrderPlacedEvent(..., occurredAt);
 *     }
 * }
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("event")
@DisplayName("Domain Event 아키텍처 검증 테스트")
class DomainEventArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.domain");
    }

    // ==================== 필수 구조 규칙 ====================

    @Nested
    @DisplayName("필수 구조 규칙")
    class RequiredStructureTests {

        /** 규칙 1: Domain Event는 DomainEvent 인터페이스를 구현해야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 DomainEvent 인터페이스를 구현해야 한다")
        void domainEvents_ShouldImplementDomainEventInterface() {
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Fixture")
                            .and()
                            .haveSimpleNameNotContaining("Mother")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .should(implementDomainEventInterface())
                            .because("Domain Event는 DomainEvent 인터페이스를 구현해야 합니다");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 2: Domain Event는 Record여야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 Record로 구현되어야 한다")
        void domainEvents_ShouldBeRecords() {
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Fixture")
                            .and()
                            .haveSimpleNameNotContaining("Mother")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .should(beRecords())
                            .because("Domain Event는 Java 21 Record로 구현해야 합니다 (불변성 보장)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 3: Domain Event는 occurredAt 필드를 가져야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 occurredAt (Instant) 필드를 가져야 한다")
        void domainEvents_ShouldHaveOccurredAtField() {
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Fixture")
                            .and()
                            .haveSimpleNameNotContaining("Mother")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .should(haveOccurredAtField())
                            .because("Domain Event는 occurredAt (Instant) 필드를 가져야 합니다 (이벤트 발생 시각)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 4: Domain Event는 from() 정적 팩토리 메서드를 가져야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 from() 정적 팩토리 메서드를 가져야 한다")
        void domainEvents_ShouldHaveFromMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Fixture")
                            .and()
                            .haveSimpleNameNotContaining("Mother")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .should(haveStaticMethodWithName("from"))
                            .because(
                                    "Domain Event는 from() 정적 팩토리 메서드로 생성해야 합니다\n"
                                        + "예시:\n"
                                        + "  public static OrderCreatedEvent from(Order order,"
                                        + " Instant occurredAt) {\n"
                                        + "      return new OrderCreatedEvent(..., occurredAt);\n"
                                        + "  }");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 네이밍 및 패키지 규칙 ====================

    @Nested
    @DisplayName("네이밍 및 패키지 규칙")
    class NamingAndPackageTests {

        /** 규칙 5: Domain Event는 과거형 네이밍을 따라야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 과거형 네이밍을 따라야 한다")
        void domainEvents_ShouldHavePastTenseNaming() {
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Fixture")
                            .and()
                            .haveSimpleNameNotContaining("Mother")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .should(havePastTenseEventName())
                            .because(
                                    "Domain Event는 과거형 네이밍을 사용해야 합니다\n"
                                            + "예시:\n"
                                            + "  - OrderPlacedEvent ✅ (placed는 과거형)\n"
                                            + "  - OrderCreatedEvent ✅ (created는 과거형)\n"
                                            + "  - OrderCancelledEvent ✅ (cancelled는 과거형)\n"
                                            + "  - OrderSentEvent ✅ (sent는 불규칙 과거형)\n"
                                            + "  - OrderPaidEvent ✅ (paid는 불규칙 과거형)\n"
                                            + "  - OrderEvent ❌ (과거형 아님)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 6: Domain Event는 domain.[bc].event 패키지에 위치해야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 domain.[bc].event 패키지에 위치해야 한다")
        void domainEvents_ShouldBeInEventPackage() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Event")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .doNotHaveSimpleName("DomainEvent")
                            .and()
                            .resideInAPackage("..domain..")
                            .should()
                            .resideInAPackage("..domain..event..")
                            .because(
                                    "Domain Event는 domain.[bc].event 패키지에 위치해야 합니다\n"
                                            + "예시:\n"
                                            + "  - domain.order.event.OrderPlacedEvent ✅\n"
                                            + "  - domain.order.aggregate.OrderPlacedEvent ❌ (잘못된"
                                            + " 패키지)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 금지 규칙 (Lombok, JPA, Spring) ====================

    @Nested
    @DisplayName("금지 규칙 (Lombok, JPA, Spring)")
    class ProhibitionTests {

        /** 규칙 7: Domain Event는 Lombok 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] Domain Event는 Lombok 어노테이션을 사용하지 않아야 한다")
        void domainEvents_ShouldNotUseLombok() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .beAnnotatedWith("lombok.Data")
                            .orShould()
                            .beAnnotatedWith("lombok.Value")
                            .orShould()
                            .beAnnotatedWith("lombok.Builder")
                            .orShould()
                            .beAnnotatedWith("lombok.Getter")
                            .orShould()
                            .beAnnotatedWith("lombok.AllArgsConstructor")
                            .orShould()
                            .beAnnotatedWith("lombok.NoArgsConstructor")
                            .because("Domain Event는 Lombok을 사용하지 않고 Pure Java Record로 구현해야 합니다");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 8: Domain Event는 JPA 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] Domain Event는 JPA 어노테이션을 사용하지 않아야 한다")
        void domainEvents_ShouldNotUseJPA() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .beAnnotatedWith("jakarta.persistence.Entity")
                            .orShould()
                            .beAnnotatedWith("jakarta.persistence.Table")
                            .orShould()
                            .beAnnotatedWith("jakarta.persistence.Embeddable")
                            .because(
                                    "Domain Event는 JPA 어노테이션을 사용하지 않아야 합니다 (Domain은 Persistence"
                                            + " 독립적)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 9: Domain Event는 Spring 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] Domain Event는 Spring 어노테이션을 사용하지 않아야 한다")
        void domainEvents_ShouldNotUseSpringAnnotations() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .beAnnotatedWith("org.springframework.stereotype.Component")
                            .orShould()
                            .beAnnotatedWith("org.springframework.context.event.EventListener")
                            .orShould()
                            .beAnnotatedWith("org.springframework.context.ApplicationEvent")
                            .because(
                                    "Domain Event는 Spring 어노테이션을 사용하지 않아야 합니다\n"
                                            + "  - Domain Event는 Pure Java Record\n"
                                            + "  - EventListener는 Application Layer에 위치");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 10: Domain Event는 Spring Framework에 의존하지 않아야 한다 */
        @Test
        @DisplayName("[금지] Domain Event는 Spring Framework에 의존하지 않아야 한다")
        void domainEvents_ShouldNotDependOnSpringFramework() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage("org.springframework..")
                            .because(
                                    "Domain Event는 Spring Framework에 의존하지 않아야 합니다 (Domain Layer"
                                            + " 순수성)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 레이어 의존성 규칙 ====================

    @Nested
    @DisplayName("레이어 의존성 규칙")
    class LayerDependencyTests {

        /** 규칙 11: Domain Event는 Application/Adapter 레이어에 의존하지 않아야 한다 */
        @Test
        @DisplayName("[필수] Domain Event는 Application/Adapter 레이어에 의존하지 않아야 한다")
        void domainEvents_ShouldNotDependOnOuterLayers() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..domain..event..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(
                                    "..application..",
                                    "..adapter..",
                                    "..persistence..",
                                    "..bootstrap..")
                            .because(
                                    "Domain Event는 Application/Adapter 레이어에 의존하지 않아야 합니다 (헥사고날"
                                            + " 아키텍처)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /** DomainEvent 인터페이스를 구현하는지 검증 */
    private static ArchCondition<JavaClass> implementDomainEventInterface() {
        return new ArchCondition<JavaClass>("implement DomainEvent interface") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean implementsDomainEvent =
                        javaClass.getAllRawInterfaces().stream()
                                .anyMatch(iface -> iface.getSimpleName().equals("DomainEvent"));

                if (!implementsDomainEvent) {
                    String message =
                            String.format(
                                    "Class %s does not implement DomainEvent interface",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** Record 타입인지 검증 */
    private static ArchCondition<JavaClass> beRecords() {
        return new ArchCondition<JavaClass>("be records") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                // Java Record는 java.lang.Record를 상속함
                boolean isRecord =
                        javaClass.getAllRawSuperclasses().stream()
                                .anyMatch(
                                        superClass ->
                                                superClass.getName().equals("java.lang.Record"));

                if (!isRecord) {
                    String message =
                            String.format(
                                    "Class %s is not a record. Use 'public record' instead of"
                                            + " 'public class'",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 과거형 이벤트 네이밍인지 검증
     *
     * <p>지원하는 패턴:
     *
     * <ul>
     *   <li>규칙 과거형: *edEvent (Created, Placed, Updated, Deleted, Cancelled)
     *   <li>불규칙 과거형: *SentEvent, *PaidEvent, *SoldEvent, *BoughtEvent, *CaughtEvent, *TaughtEvent,
     *       *BuiltEvent, *MadeEvent, *HeldEvent, *LeftEvent, *LostEvent, *MetEvent, *RunEvent,
     *       *WonEvent, *BegunEvent, *DoneEvent
     * </ul>
     */
    private static ArchCondition<JavaClass> havePastTenseEventName() {
        return new ArchCondition<JavaClass>("have past tense event name") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String simpleName = javaClass.getSimpleName();

                // 규칙 과거형: *edEvent 또는 *dEvent
                boolean hasRegularPastTense = simpleName.matches(".*(?:ed|d)Event");

                // 불규칙 과거형 패턴 (흔히 사용되는 동사들)
                boolean hasIrregularPastTense =
                        simpleName.matches(
                                ".*(?:Sent|Paid|Sold|Bought|Caught|Taught|Built|Made|Held|Left|"
                                    + "Lost|Met|Run|Won|Begun|Done|Gone|Grown|Known|Shown|Thrown|Worn|"
                                    + "Withdrawn|Written|Driven|Given|Taken|Broken|Chosen|Frozen|Spoken|"
                                    + "Stolen|Woken|Hidden|Ridden|Risen|Fallen|Forgotten|Gotten|Eaten|"
                                    + "Beaten|Bitten|Blown|Drawn|Flown|Sworn|Torn)Event");

                if (!hasRegularPastTense && !hasIrregularPastTense) {
                    String message =
                            String.format(
                                    "Event %s should have past tense naming.\n"
                                            + "Examples:\n"
                                            + "  - Regular: OrderCreatedEvent, OrderPlacedEvent,"
                                            + " OrderCancelledEvent\n"
                                            + "  - Irregular: OrderSentEvent, OrderPaidEvent,"
                                            + " OrderSoldEvent",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** occurredAt 필드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveOccurredAtField() {
        return new ArchCondition<JavaClass>("have occurredAt field of type Instant") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasOccurredAt =
                        javaClass.getAllFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getName().equals("occurredAt")
                                                        && field.getRawType()
                                                                .isEquivalentTo(Instant.class));

                if (!hasOccurredAt) {
                    String message =
                            String.format(
                                    "Event %s does not have occurredAt field of type Instant",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have public static method with name " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod =
                        javaClass.getAllMethods().stream()
                                .anyMatch(
                                        method ->
                                                method.getName().equals(methodName)
                                                        && method.getModifiers()
                                                                .contains(JavaModifier.STATIC)
                                                        && method.getModifiers()
                                                                .contains(JavaModifier.PUBLIC));

                if (!hasMethod) {
                    String message =
                            String.format(
                                    "Class %s does not have a public static method named '%s'",
                                    javaClass.getName(), methodName);
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }
}
