package com.ryuqq.gateway.domain.architecture.vo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Value Object ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>⚠️ 템플릿 안내</strong>:
 *
 * <p>이 클래스는 보일러플레이트 템플릿입니다. 프로젝트에 맞게 다음을 수정하세요:
 *
 * <ul>
 *   <li>패키지명: {@code com.ryuqq.domain} → 실제 프로젝트 패키지
 *   <li>테스트 클래스 위치: 프로젝트 구조에 맞게 조정
 * </ul>
 *
 * <p><strong>검증 규칙 (총 9개)</strong>:
 *
 * <ul>
 *   <li>Record 사용 필수 (Enum/Interface/Abstract 제외)
 *   <li>정적 팩토리 메서드 (of) 필수 (Enum/Interface/Abstract 제외)
 *   <li>ID VO는 forNew() 추가 필수
 *   <li>Long ID VO는 isNew() 필수 (UUID ID 제외)
 *   <li>Enum VO는 displayName() 필수
 *   <li>Lombok 금지
 *   <li>JPA 어노테이션 금지
 *   <li>Spring 어노테이션 금지
 *   <li>create*() 메서드 금지
 * </ul>
 *
 * <p><strong>제외 대상</strong>:
 *
 * <ul>
 *   <li>인터페이스 (LockKey, CacheKey, SortKey 등)
 *   <li>추상 클래스
 *   <li>Enum (별도 규칙 적용)
 *   <li>테스트 관련 클래스 (Fixture, Mother, Test)
 * </ul>
 *
 * <p><strong>검증 대상 클래스가 없는 경우</strong>:
 *
 * <p>{@code ..vo..} 패키지에 클래스가 없으면 해당 규칙은 자동으로 통과합니다. 이는 {@code allowEmptyShould(true)} 설정으로 구현되어
 * 있습니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("vo")
@DisplayName("Value Object 아키텍처 검증 테스트")
class VOArchTest {

    private static final String VO_PACKAGE = "..vo..";
    private static final String BASE_PACKAGE = "com.ryuqq.domain"; // ⚠️ 프로젝트에 맞게 수정

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    /** 규칙 1: Value Object는 Record여야 한다 (Enum/Interface/Abstract 제외) */
    @Test
    @DisplayName("[필수] Value Object는 Record로 구현되어야 한다 (Enum/Interface/Abstract 제외)")
    void valueObjectsShouldBeRecords() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .areNotEnums() // Enum VO 제외
                        .and()
                        .areNotInterfaces() // 인터페이스 제외 (LockKey, CacheKey, SortKey 등)
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT) // 추상 클래스 제외
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
                        .because(
                                "Value Object는 Java 21 Record로 구현해야 합니다\n"
                                        + "  - Enum, Interface, Abstract 클래스는 제외됩니다\n"
                                        + "  - 예시: public record Money(BigDecimal amount) {}");

        rule.check(classes);
    }

    /** 규칙 2: Value Object는 of() 메서드를 가져야 한다 (Enum/Interface/Abstract 제외) */
    @Test
    @DisplayName("[필수] Value Object는 of() 정적 팩토리 메서드를 가져야 한다 (Enum/Interface/Abstract 제외)")
    void valueObjectsShouldHaveOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .areNotEnums() // Enum VO 제외
                        .and()
                        .areNotInterfaces() // 인터페이스 제외
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT) // 추상 클래스 제외
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
                        .because(
                                "Value Object는 of() 정적 팩토리 메서드로 생성해야 합니다\n"
                                        + "  - Enum, Interface, Abstract 클래스는 제외됩니다\n"
                                        + "  - 예시: public static Money of(BigDecimal amount) {}");

        rule.check(classes);
    }

    /**
     * 규칙 3: ID VO는 forNew() 메서드를 가져야 한다
     *
     * <p>Long ID: forNew()가 null 반환
     *
     * <p>UUID ID: forNew()가 UUIDv7 문자열 반환
     */
    @Test
    @DisplayName("[필수] ID Value Object는 forNew() 메서드를 가져야 한다")
    void idValueObjectsShouldHaveForNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .haveSimpleNameEndingWith("Id")
                        .and()
                        .areNotInterfaces() // 인터페이스 제외
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT) // 추상 클래스 제외
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
                        .allowEmptyShould(true) // ID VO가 없으면 통과
                        .because(
                                "ID Value Object는 forNew() 메서드를 가져야 합니다\n"
                                    + "  - Long ID: forNew()가 null 반환 (DB가 ID 생성)\n"
                                    + "  - UUID ID: forNew()가 UUIDv7 문자열 반환 (Application이 ID 생성)");

        rule.check(classes);
    }

    /**
     * 규칙 4: Long 타입 ID VO는 isNew() 메서드를 가져야 한다 (UUID ID 제외)
     *
     * <p>Long 타입 ID VO (Auto Increment)만 isNew() 필수. UUID 타입 ID VO는 항상 값이 존재하므로 isNew() 불필요.
     */
    @Test
    @DisplayName("[필수] Long 타입 ID VO는 isNew() 메서드를 가져야 한다 (UUID ID 제외)")
    void longIdValueObjectsShouldHaveIsNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .haveSimpleNameEndingWith("Id")
                        .and()
                        .areNotInterfaces() // 인터페이스 제외
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT) // 추상 클래스 제외
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
                        .should(haveLongFieldAndIsNewMethod())
                        .allowEmptyShould(true) // ID VO가 없으면 통과
                        .because(
                                "Long 타입 ID VO는 isNew() 메서드로 null 여부를 확인해야 합니다\n"
                                        + "  - Long ID (Auto Increment): isNew() 필수\n"
                                        + "  - UUID ID (Application 생성): isNew() 불필요 (항상 값 존재)");

        rule.check(classes);
    }

    /** 규칙 5: Value Object는 Lombok 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Value Object는 Lombok 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseLombok() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
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
                        .resideInAPackage(VO_PACKAGE)
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
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
                        .resideInAPackage(VO_PACKAGE)
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
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
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .areNotInterfaces() // 인터페이스 제외
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT) // 추상 클래스 제외
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
                        .allowEmptyShould(true) // vo 패키지에 클래스 없으면 통과
                        .because("Value Object는 create*() 대신 of(), forNew()를 사용해야 합니다");

        rule.check(classes);
    }

    /** 규칙 9: Enum VO는 displayName() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Enum VO는 displayName() 메서드를 가져야 한다")
    void enumValueObjectsShouldHaveDisplayNameMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(VO_PACKAGE)
                        .and()
                        .areEnums() // Enum만 대상
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(haveMethodWithName("displayName"))
                        .allowEmptyShould(true) // Enum VO가 없으면 통과
                        .because(
                                "Enum VO는 displayName() 메서드로 화면 표시용 이름을 제공해야 합니다\n"
                                    + "  - 예시: public String displayName() { return this.name; }");

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

    /**
     * Long 타입 필드를 가진 ID VO가 isNew() 메서드를 가지는지 검증
     *
     * <p>Long 타입 ID VO (Auto Increment)만 isNew() 필수. String 타입 (UUID) ID VO는 항상 값이 존재하므로 isNew()
     * 불필요.
     *
     * <p><strong>ID VO 유형별 검증</strong>:
     *
     * <ul>
     *   <li>Long ID (Auto Increment): isNew() 필수 (DB가 ID 생성)
     *   <li>UUID ID (Application 생성): isNew() 불필요 (항상 값 존재)
     * </ul>
     */
    private static ArchCondition<JavaClass> haveLongFieldAndIsNewMethod() {
        return new ArchCondition<JavaClass>("have Long field and isNew() method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                // Long 타입 필드가 있는지 확인 (value 또는 id 필드)
                boolean hasLongField =
                        javaClass.getAllFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                                .getName()
                                                                .equals("java.lang.Long")
                                                        || field.getRawType()
                                                                .getName()
                                                                .equals("long"));

                // String 타입 필드만 있으면 (UUID ID) isNew() 불필요
                boolean hasStringFieldOnly =
                        javaClass.getAllFields().stream()
                                        .anyMatch(
                                                field ->
                                                        field.getRawType()
                                                                .getName()
                                                                .equals("java.lang.String"))
                                && !hasLongField;

                // String 타입 (UUID) ID VO는 isNew() 불필요하므로 검증 통과
                if (hasStringFieldOnly) {
                    return;
                }

                // Long 타입 ID VO는 isNew() 메서드 필수
                if (hasLongField) {
                    boolean hasIsNewMethod =
                            javaClass.getAllMethods().stream()
                                    .anyMatch(method -> method.getName().equals("isNew"));

                    if (!hasIsNewMethod) {
                        String message =
                                String.format(
                                        "Long ID VO %s must have isNew() method (UUID ID VOs are"
                                                + " exempt)",
                                        javaClass.getName());
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    }
                }
            }
        };
    }
}
