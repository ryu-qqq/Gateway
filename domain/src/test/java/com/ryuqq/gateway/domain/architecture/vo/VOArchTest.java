package com.ryuqq.gateway.domain.architecture.vo;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Value Object ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>Record 사용 필수
 *   <li>정적 팩토리 메서드 (of) 필수
 *   <li>ID VO는 forNew() 추가 필수
 *   <li>Lombok 금지
 *   <li>JPA 어노테이션 금지
 *   <li>Spring 어노테이션 금지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("vo")
@DisplayName("Value Object 아키텍처 검증 테스트")
class VOArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.domain");
    }

    /** 규칙 1: Value Object는 Record여야 한다 */
    @Test
    @DisplayName("[필수] Value Object는 Record로 구현되어야 한다")
    void valueObjectsShouldBeRecords() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should(beRecords())
                        .because("Value Object는 Java 21 Record로 구현해야 합니다");

        rule.check(classes);
    }

    /** 규칙 2: Value Object는 of() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Value Object는 of() 정적 팩토리 메서드를 가져야 한다")
    void valueObjectsShouldHaveOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should(haveStaticMethodWithName("of"))
                        .because("Value Object는 of() 정적 팩토리 메서드로 생성해야 합니다");

        rule.check(classes);
    }

    /** 규칙 3: ID VO는 forNew() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] ID Value Object는 forNew() 메서드를 가져야 한다")
    void idValueObjectsShouldHaveForNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .haveSimpleNameEndingWith("Id")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should(haveStaticMethodWithName("forNew"))
                        .because("ID Value Object는 forNew() 메서드로 null 생성을 지원해야 합니다");

        rule.check(classes);
    }

    /** 규칙 4: ID VO는 isNew() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] ID Value Object는 isNew() 메서드를 가져야 한다")
    void idValueObjectsShouldHaveIsNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .haveSimpleNameEndingWith("Id")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should(haveMethodWithName("isNew"))
                        .because("ID Value Object는 isNew() 메서드로 null 여부를 확인해야 합니다");

        rule.check(classes);
    }

    /** 규칙 5: Value Object는 Lombok 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Value Object는 Lombok 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseLombok() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should()
                        .notBeAnnotatedWith("lombok.Data")
                        .andShould()
                        .notBeAnnotatedWith("lombok.Value")
                        .andShould()
                        .notBeAnnotatedWith("lombok.Builder")
                        .andShould()
                        .notBeAnnotatedWith("lombok.Getter")
                        .andShould()
                        .notBeAnnotatedWith("lombok.Setter")
                        .andShould()
                        .notBeAnnotatedWith("lombok.AllArgsConstructor")
                        .andShould()
                        .notBeAnnotatedWith("lombok.NoArgsConstructor")
                        .because("Value Object는 Lombok을 사용하지 않고 Pure Java Record로 구현해야 합니다");

        rule.check(classes);
    }

    /** 규칙 6: Value Object는 JPA 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Value Object는 JPA 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseJpa() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should()
                        .notBeAnnotatedWith("javax.persistence.Entity")
                        .andShould()
                        .notBeAnnotatedWith("javax.persistence.Table")
                        .andShould()
                        .notBeAnnotatedWith("javax.persistence.Embeddable")
                        .andShould()
                        .notBeAnnotatedWith("jakarta.persistence.Entity")
                        .andShould()
                        .notBeAnnotatedWith("jakarta.persistence.Table")
                        .andShould()
                        .notBeAnnotatedWith("jakarta.persistence.Embeddable")
                        .because("Value Object는 JPA 어노테이션을 사용하지 않아야 합니다");

        rule.check(classes);
    }

    /** 규칙 7: Value Object는 Spring 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Value Object는 Spring 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseSpring() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should()
                        .notBeAnnotatedWith("org.springframework.stereotype.Component")
                        .andShould()
                        .notBeAnnotatedWith("org.springframework.stereotype.Service")
                        .andShould()
                        .notBeAnnotatedWith("org.springframework.context.annotation.Configuration")
                        .because("Value Object는 Spring 어노테이션을 사용하지 않아야 합니다");

        rule.check(classes);
    }

    /** 규칙 8: Value Object는 create*() 메서드를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Value Object는 create*() 메서드를 사용하지 않아야 한다")
    void valueObjectsShouldNotHaveCreateMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..vo..")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .should(notHaveMethodsWithNameStartingWith("create"))
                        .because("Value Object는 create*() 대신 of(), forNew()를 사용해야 합니다");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

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

    /** 클래스가 특정 이름의 메서드를 가지고 있는지 검증 (static 아님) */
    private static ArchCondition<JavaClass> haveMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have method with name " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod =
                        javaClass.getAllMethods().stream()
                                .anyMatch(method -> method.getName().equals(methodName));

                if (!hasMethod) {
                    String message =
                            String.format(
                                    "Class %s does not have a method named '%s'",
                                    javaClass.getName(), methodName);
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** 클래스가 특정 접두사로 시작하는 메서드를 가지지 않는지 검증 */
    private static ArchCondition<JavaClass> notHaveMethodsWithNameStartingWith(String prefix) {
        return new ArchCondition<JavaClass>("not have methods with name starting with " + prefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllMethods().stream()
                        .filter(method -> method.getName().startsWith(prefix))
                        .forEach(
                                method -> {
                                    String message =
                                            String.format(
                                                    "Class %s has method %s starting with '%s'"
                                                            + " which is prohibited",
                                                    javaClass.getName(), method.getName(), prefix);
                                    events.add(SimpleConditionEvent.violated(javaClass, message));
                                });
            }
        };
    }
}
