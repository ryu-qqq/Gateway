package com.ryuqq.gateway.domain.architecture.vo;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LockKey VO ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>LockKey 인터페이스 구현 필수
 *   <li>Record 타입 필수 (불변성)
 *   <li>*LockKey 네이밍 규칙
 *   <li>value() 메서드 필수
 *   <li>domain.[bc].vo 패키지에 위치
 *   <li>Lombok, JPA, Spring 금지
 * </ul>
 *
 * <p><strong>LockKey 패턴</strong>:
 *
 * <pre>
 * public record OrderLockKey(Long orderId) implements LockKey {
 *
 *     private static final String PREFIX = "lock:order:";
 *
 *     public OrderLockKey {
 *         if (orderId == null || orderId <= 0) {
 *             throw new IllegalArgumentException("orderId must be positive");
 *         }
 *     }
 *
 *     @Override
 *     public String value() {
 *         return PREFIX + orderId;
 *     }
 * }
 * </pre>
 *
 * <p><strong>키 형식 규칙</strong>:
 *
 * <pre>
 * lock:{domain}:{id}
 * lock:{domain}:{entity}:{id}
 * lock:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 * @see com.ryuqq.domain.common.vo.LockKey
 */
@Tag("architecture")
@Tag("domain")
@Tag("vo")
@Tag("lock")
@DisplayName("LockKey VO 아키텍처 검증 테스트")
class LockKeyArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.domain");
    }

    // ==================== 필수 구조 규칙 ====================

    @Nested
    @DisplayName("필수 구조 규칙")
    class RequiredStructureTests {

        /** 규칙 1: LockKey 구현체는 LockKey 인터페이스를 구현해야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 LockKey 인터페이스를 구현해야 한다")
        void lockKeyImplementations_ShouldImplementLockKeyInterface() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
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
                            .should(implementLockKeyInterface())
                            .because("LockKey 구현체는 LockKey 인터페이스를 구현해야 합니다");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 2: LockKey 구현체는 Record여야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 Record로 구현되어야 한다")
        void lockKeyImplementations_ShouldBeRecords() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
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
                            .should(beRecords())
                            .because("LockKey 구현체는 Java 21 Record로 구현해야 합니다 (불변성 보장)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 3: LockKey 구현체는 value() 메서드를 가져야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 value() 메서드를 가져야 한다")
        void lockKeyImplementations_ShouldHaveValueMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
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
                            .should(haveValueMethod())
                            .because(
                                    "LockKey 구현체는 value() 메서드로 Lock 키 문자열을 반환해야 합니다\n"
                                            + "예시:\n"
                                            + "  @Override\n"
                                            + "  public String value() {\n"
                                            + "      return \"lock:order:\" + orderId;\n"
                                            + "  }");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 네이밍 및 패키지 규칙 ====================

    @Nested
    @DisplayName("네이밍 및 패키지 규칙")
    class NamingAndPackageTests {

        /** 규칙 4: LockKey 구현체는 *LockKey 네이밍을 따라야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 *LockKey 네이밍을 따라야 한다")
        void lockKeyImplementations_ShouldHaveLockKeySuffix() {
            ArchRule rule =
                    classes()
                            .that()
                            .implement("com.ryuqq.domain.common.vo.LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
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
                            .should()
                            .haveSimpleNameEndingWith("LockKey")
                            .because(
                                    "LockKey 구현체는 *LockKey 네이밍을 따라야 합니다\n"
                                            + "예시:\n"
                                            + "  - OrderLockKey ✅\n"
                                            + "  - StockLockKey ✅\n"
                                            + "  - OrderLock ❌ (LockKey 접미사 누락)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 5: LockKey 구현체는 domain.[bc].vo 패키지에 위치해야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 domain.[bc].vo 패키지에 위치해야 한다")
        void lockKeyImplementations_ShouldBeInVoPackage() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotAnonymousClasses()
                            .and()
                            .areNotMemberClasses()
                            .and()
                            .resideInAPackage("..domain..")
                            .should()
                            .resideInAPackage("..domain..vo..")
                            .because(
                                    "LockKey 구현체는 domain.[bc].vo 패키지에 위치해야 합니다\n"
                                        + "예시:\n"
                                        + "  - domain.order.vo.OrderLockKey ✅\n"
                                        + "  - domain.common.vo.LockKey ✅ (기반 인터페이스)\n"
                                        + "  - domain.order.aggregate.OrderLockKey ❌ (잘못된 패키지)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 금지 규칙 (Lombok, JPA, Spring) ====================

    @Nested
    @DisplayName("금지 규칙 (Lombok, JPA, Spring)")
    class ProhibitionTests {

        /** 규칙 6: LockKey 구현체는 Lombok 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] LockKey 구현체는 Lombok 어노테이션을 사용하지 않아야 한다")
        void lockKeyImplementations_ShouldNotUseLombok() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
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
                            .because("LockKey 구현체는 Lombok을 사용하지 않고 Pure Java Record로 구현해야 합니다");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 7: LockKey 구현체는 JPA 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] LockKey 구현체는 JPA 어노테이션을 사용하지 않아야 한다")
        void lockKeyImplementations_ShouldNotUseJPA() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .beAnnotatedWith("jakarta.persistence.Entity")
                            .orShould()
                            .beAnnotatedWith("jakarta.persistence.Table")
                            .orShould()
                            .beAnnotatedWith("jakarta.persistence.Embeddable")
                            .because(
                                    "LockKey 구현체는 JPA 어노테이션을 사용하지 않아야 합니다 (Domain은 Persistence"
                                            + " 독립적)");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 8: LockKey 구현체는 Spring 어노테이션을 사용하지 않아야 한다 */
        @Test
        @DisplayName("[금지] LockKey 구현체는 Spring 어노테이션을 사용하지 않아야 한다")
        void lockKeyImplementations_ShouldNotUseSpringAnnotations() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .beAnnotatedWith("org.springframework.stereotype.Component")
                            .orShould()
                            .beAnnotatedWith("org.springframework.stereotype.Service")
                            .because(
                                    "LockKey 구현체는 Spring 어노테이션을 사용하지 않아야 합니다\n"
                                            + "  - LockKey는 Pure Java Record\n"
                                            + "  - Spring 의존성 없이 Domain Layer 순수성 유지");

            rule.allowEmptyShould(true).check(classes);
        }

        /** 규칙 9: LockKey 구현체는 Spring Framework에 의존하지 않아야 한다 */
        @Test
        @DisplayName("[금지] LockKey 구현체는 Spring Framework에 의존하지 않아야 한다")
        void lockKeyImplementations_ShouldNotDependOnSpringFramework() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage("org.springframework..")
                            .because(
                                    "LockKey 구현체는 Spring Framework에 의존하지 않아야 합니다 (Domain Layer"
                                            + " 순수성)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 레이어 의존성 규칙 ====================

    @Nested
    @DisplayName("레이어 의존성 규칙")
    class LayerDependencyTests {

        /** 규칙 10: LockKey 구현체는 Application/Adapter 레이어에 의존하지 않아야 한다 */
        @Test
        @DisplayName("[필수] LockKey 구현체는 Application/Adapter 레이어에 의존하지 않아야 한다")
        void lockKeyImplementations_ShouldNotDependOnOuterLayers() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("LockKey")
                            .and()
                            .resideInAPackage("..domain..vo..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(
                                    "..application..",
                                    "..adapter..",
                                    "..persistence..",
                                    "..bootstrap..")
                            .because(
                                    "LockKey 구현체는 Application/Adapter 레이어에 의존하지 않아야 합니다 (헥사고날"
                                            + " 아키텍처)");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /** LockKey 인터페이스를 구현하는지 검증 */
    private static ArchCondition<JavaClass> implementLockKeyInterface() {
        return new ArchCondition<JavaClass>("implement LockKey interface") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean implementsLockKey =
                        javaClass.getAllRawInterfaces().stream()
                                .anyMatch(iface -> iface.getSimpleName().equals("LockKey"));

                if (!implementsLockKey) {
                    String message =
                            String.format(
                                    "Class %s does not implement LockKey interface",
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
                                    "Class %s is not a record. Use 'public record %s(...)"
                                            + " implements LockKey'",
                                    javaClass.getName(), javaClass.getSimpleName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** value() 메서드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveValueMethod() {
        return new ArchCondition<JavaClass>("have value() method returning String") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasValueMethod =
                        javaClass.getAllMethods().stream()
                                .anyMatch(
                                        method ->
                                                method.getName().equals("value")
                                                        && method.getRawReturnType()
                                                                .getName()
                                                                .equals("java.lang.String"));

                if (!hasValueMethod) {
                    String message =
                            String.format(
                                    "Class %s does not have value() method returning String.\n"
                                            + "Expected:\n"
                                            + "  @Override\n"
                                            + "  public String value() {\n"
                                            + "      return \"lock:domain:\" + id;\n"
                                            + "  }",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }
}
