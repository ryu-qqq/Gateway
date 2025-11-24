package com.ryuqq.connectly.adapter.out.persistence.architecture.entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JpaEntityArchTest - JPA Entity 아키텍처 규칙 검증
 *
 * <p>entity-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: @Entity 어노테이션 필수
 *   <li>규칙 2: Lombok 사용 금지
 *   <li>규칙 3: JPA 관계 어노테이션 금지 (Long FK 전략)
 *   <li>규칙 4: Setter 메서드 금지
 *   <li>규칙 5: 비즈니스 로직 금지
 *   <li>규칙 6: protected 기본 생성자 필수
 *   <li>규칙 7: private 전체 필드 생성자 필수
 *   <li>규칙 8: public static of() 메서드 필수
 *   <li>규칙 9: Getter만 제공 (public 메서드는 getter 또는 of()만)
 *   <li>규칙 10: Entity 네이밍 규칙 (*JpaEntity)
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("JPA Entity 아키텍처 규칙 검증 (Zero-Tolerance)")
class JpaEntityArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses entityClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter().importPackages("com.ryuqq.adapter.out.persistence");

        entityClasses =
                allClasses.that(
                        DescribedPredicate.describe(
                                "are JPA Entity classes",
                                javaClass -> javaClass.isAnnotatedWith(Entity.class)));
    }

    @Test
    @DisplayName("규칙 1: @Entity 어노테이션 필수")
    void jpaEntity_MustBeAnnotatedWithEntity() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("JpaEntity")
                        .should()
                        .beAnnotatedWith(Entity.class)
                        .because("JPA Entity 클래스는 @Entity 어노테이션이 필수입니다");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Data")
    void jpaEntity_MustNotUseLombok_Data() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.Data")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Getter")
    void jpaEntity_MustNotUseLombok_Getter() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.Getter")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Setter")
    void jpaEntity_MustNotUseLombok_Setter() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.Setter")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Builder")
    void jpaEntity_MustNotUseLombok_Builder() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.Builder")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @AllArgsConstructor")
    void jpaEntity_MustNotUseLombok_AllArgsConstructor() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.AllArgsConstructor")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @NoArgsConstructor")
    void jpaEntity_MustNotUseLombok_NoArgsConstructor() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .notBeAnnotatedWith("lombok.NoArgsConstructor")
                        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @ManyToOne")
    void jpaEntity_MustNotUseJpaRelationship_ManyToOne() {
        ArchRule rule =
                noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .beAnnotatedWith(ManyToOne.class)
                        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @OneToMany")
    void jpaEntity_MustNotUseJpaRelationship_OneToMany() {
        ArchRule rule =
                noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .beAnnotatedWith(OneToMany.class)
                        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @OneToOne")
    void jpaEntity_MustNotUseJpaRelationship_OneToOne() {
        ArchRule rule =
                noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .beAnnotatedWith(OneToOne.class)
                        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @ManyToMany")
    void jpaEntity_MustNotUseJpaRelationship_ManyToMany() {
        ArchRule rule =
                noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .beAnnotatedWith(ManyToMany.class)
                        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 4: Setter 메서드 금지")
    void jpaEntity_MustNotHaveSetterMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .and()
                        .arePublic()
                        .and()
                        .haveNameMatching("set[A-Z].*")
                        .should(notExist())
                        .because("JPA Entity는 Setter 메서드가 금지됩니다 (Getter만 제공)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 5: 비즈니스 로직 금지 (특정 메서드 패턴)")
    void jpaEntity_MustNotHaveBusinessLogicMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .areAnnotatedWith(Entity.class)
                        .and()
                        .arePublic()
                        .and()
                        .haveNameMatching(
                                "(approve|cancel|complete|activate|deactivate|validate|calculate).*")
                        .should(notExist())
                        .because("JPA Entity는 비즈니스 로직이 금지됩니다 (Domain Layer에서 처리)");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 6: protected 기본 생성자 필수")
    void jpaEntity_MustHaveProtectedNoArgsConstructor() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should(haveProtectedNoArgsConstructor())
                        .because("JPA Entity는 JPA 스펙을 위해 protected 기본 생성자가 필수입니다");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 7: private 전체 필드 생성자 필수")
    void jpaEntity_MustHavePrivateAllArgsConstructor() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should(havePrivateConstructorWithParameters())
                        .because("JPA Entity는 무분별한 생성 방지를 위해 private 생성자가 필수입니다");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 8: public static of() 메서드 필수")
    void jpaEntity_MustHavePublicStaticOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should(havePublicStaticOfMethod())
                        .because("JPA Entity는 Mapper 전용 of() 스태틱 메서드가 필수입니다");

        rule.check(entityClasses);
    }

    @Test
    @DisplayName("규칙 10: Entity 네이밍 규칙 (*JpaEntity)")
    void jpaEntity_MustFollowNamingConvention() {
        ArchRule rule =
                classes()
                        .that()
                        .areAnnotatedWith(Entity.class)
                        .should()
                        .haveSimpleNameEndingWith("JpaEntity")
                        .because("JPA Entity 클래스는 *JpaEntity 네이밍 규칙을 따라야 합니다");

        rule.check(entityClasses);
    }

    // ===== 커스텀 ArchCondition =====

    /** protected 기본 생성자 존재 검증 */
    private static ArchCondition<JavaClass> haveProtectedNoArgsConstructor() {
        return new ArchCondition<>("have protected no-args constructor") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasProtectedNoArgsConstructor =
                        javaClass.getConstructors().stream()
                                .anyMatch(
                                        constructor ->
                                                constructor
                                                                .getModifiers()
                                                                .contains(JavaModifier.PROTECTED)
                                                        && constructor.getParameters().isEmpty());

                if (!hasProtectedNoArgsConstructor) {
                    String message =
                            String.format(
                                    "Class %s does not have a protected no-args constructor"
                                            + " (required by JPA spec)",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** private 파라미터 있는 생성자 존재 검증 */
    private static ArchCondition<JavaClass> havePrivateConstructorWithParameters() {
        return new ArchCondition<>("have private constructor with parameters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasPrivateConstructor =
                        javaClass.getConstructors().stream()
                                .anyMatch(
                                        constructor ->
                                                constructor
                                                                .getModifiers()
                                                                .contains(JavaModifier.PRIVATE)
                                                        && !constructor.getParameters().isEmpty());

                if (!hasPrivateConstructor) {
                    String message =
                            String.format(
                                    "Class %s does not have a private constructor with parameters"
                                            + " (required to prevent direct instantiation)",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** public static of() 메서드 존재 검증 */
    private static ArchCondition<JavaClass> havePublicStaticOfMethod() {
        return new ArchCondition<>("have public static of() method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasOfMethod =
                        javaClass.getMethods().stream()
                                .anyMatch(
                                        method ->
                                                method.getName().equals("of")
                                                        && method.getModifiers()
                                                                .contains(JavaModifier.PUBLIC)
                                                        && method.getModifiers()
                                                                .contains(JavaModifier.STATIC));

                if (!hasOfMethod) {
                    String message =
                            String.format(
                                    "Class %s does not have a public static of() method (required"
                                            + " for Mapper creation pattern)",
                                    javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** 메서드가 존재하지 않아야 함을 검증하는 ArchCondition */
    private static ArchCondition<JavaMethod> notExist() {
        return new ArchCondition<>("not exist") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String message =
                        String.format(
                                "Method %s.%s() should not exist",
                                method.getOwner().getSimpleName(), method.getName());
                events.add(SimpleConditionEvent.violated(method, message));
            }
        };
    }
}
