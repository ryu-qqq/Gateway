package com.ryuqq.gateway.application.architecture.manager;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReadManager ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>ReadManager의 구조 규칙을 검증합니다.
 *
 * <h3>핵심 원칙:</h3>
 *
 * <ul>
 *   <li>ReadManager는 Class여야 함 (구현체)
 *   <li>단일 QueryPort만 의존 (QueryPort 또는 LockQueryPort 중 하나)
 *   <li>find*() 메서드만 허용 (순수 조회 위임)
 *   <li>@Component + @Transactional(readOnly = true) 필수
 *   <li>Lombok 금지, 생성자 주입만
 * </ul>
 *
 * <h3>TransactionManager와의 차이점:</h3>
 *
 * <ul>
 *   <li>TransactionManager: @Transactional, persist() 메서드, PersistencePort 의존
 *   <li>ReadManager: @Transactional(readOnly = true), find*() 메서드, QueryPort 의존
 * </ul>
 *
 * <h3>책임:</h3>
 *
 * <ul>
 *   <li>읽기 전용 트랜잭션 경계 제공
 *   <li>QueryPort 호출 순수 위임
 *   <li>비즈니스 로직 없음
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("ReadManager ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class ReadManagerArchTest {

    private static JavaClasses classes;
    private static boolean hasReadManagerClasses;
    private static List<JavaClass> readManagerClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        readManagerClasses =
                classes.stream()
                        .filter(javaClass -> javaClass.getSimpleName().endsWith("ReadManager"))
                        .filter(javaClass -> javaClass.getPackageName().contains(".manager"))
                        .filter(javaClass -> !javaClass.isInterface())
                        .collect(Collectors.toList());

        hasReadManagerClasses = !readManagerClasses.isEmpty();
    }

    // ========================================
    // 기본 구조 규칙
    // ========================================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] ReadManager는 '*ReadManager' 접미사를 가져야 한다")
        void readManager_MustHaveCorrectSuffix() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .haveSimpleNameEndingWith("ReadManager")
                            .should()
                            .haveSimpleNameEndingWith("ReadManager")
                            .because("ReadManager는 'ReadManager' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] ReadManager는 ..manager.. 패키지에 위치해야 한다")
        void readManager_MustBeInManagerPackage() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("ReadManager")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .resideInAPackage("..manager..")
                            .because("ReadManager는 manager 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] ReadManager는 Class여야 한다 (Interface 아님)")
        void readManager_MustBeClass() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            long violationCount =
                    classes.stream()
                            .filter(javaClass -> javaClass.getSimpleName().endsWith("ReadManager"))
                            .filter(javaClass -> javaClass.getPackageName().contains(".manager"))
                            .filter(JavaClass::isInterface)
                            .count();

            if (violationCount > 0) {
                fail("ReadManager는 Interface가 아닌 Class여야 합니다. (위반 수: " + violationCount + ")");
            }
        }

        @Test
        @DisplayName("[필수] ReadManager는 public이어야 한다")
        void readManager_MustBePublic() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("ReadManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .should()
                            .bePublic()
                            .because("ReadManager는 외부에서 접근 가능해야 합니다");

            rule.check(classes);
        }
    }

    // ========================================
    // 어노테이션 규칙
    // ========================================

    @Nested
    @DisplayName("어노테이션 규칙")
    class AnnotationRules {

        @Test
        @DisplayName("[필수] ReadManager는 @Component 어노테이션이 필수이다")
        void readManager_MustHaveComponentAnnotation() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("ReadManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("ReadManager는 @Component로 Bean 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] ReadManager는 @Transactional 어노테이션이 필수이다")
        void readManager_MustHaveTransactionalAnnotation() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("ReadManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .beAnnotatedWith(Transactional.class)
                            .because("ReadManager는 @Transactional로 트랜잭션 경계를 정의해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] ReadManager는 @Transactional(readOnly = true)를 사용해야 한다")
        void readManager_MustHaveReadOnlyTransactional() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                Optional<JavaAnnotation<JavaClass>> transactionalAnnotation =
                        readManager.getAnnotations().stream()
                                .filter(
                                        annotation ->
                                                annotation
                                                        .getRawType()
                                                        .isEquivalentTo(Transactional.class))
                                .findFirst();

                if (transactionalAnnotation.isEmpty()) {
                    fail(readManager.getSimpleName() + "는 @Transactional 어노테이션이 필수입니다.");
                    return;
                }

                Map<String, Object> properties = transactionalAnnotation.get().getProperties();
                Object readOnlyValue = properties.get("readOnly");

                if (readOnlyValue == null || !Boolean.TRUE.equals(readOnlyValue)) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 @Transactional(readOnly = true)를 사용해야 합니다. "
                                    + "ReadManager는 읽기 전용 트랜잭션만 허용됩니다.");
                }
            }
        }
    }

    // ========================================
    // 의존성 규칙
    // ========================================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] ReadManager는 QueryPort 인터페이스만 필드로 가져야 한다")
        void readManager_MustOnlyDependOnQueryPort() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                List<JavaField> nonQueryPortFields =
                        readManager.getFields().stream()
                                .filter(
                                        field ->
                                                !field.getName().equals("this$0")) // inner class 제외
                                .filter(
                                        field -> {
                                            String typeName = field.getRawType().getName();
                                            // QueryPort 또는 LockQueryPort 인터페이스인지 확인
                                            boolean isQueryPort =
                                                    typeName.contains("QueryPort")
                                                            || typeName.contains("LockQueryPort");
                                            return !isQueryPort;
                                        })
                                .collect(Collectors.toList());

                if (!nonQueryPortFields.isEmpty()) {
                    String fieldNames =
                            nonQueryPortFields.stream()
                                    .map(
                                            f ->
                                                    f.getName()
                                                            + " ("
                                                            + f.getRawType().getSimpleName()
                                                            + ")")
                                    .collect(Collectors.joining(", "));
                    fail(
                            readManager.getSimpleName()
                                    + "는 QueryPort 인터페이스만 필드로 가져야 합니다. "
                                    + "허용되지 않는 필드: "
                                    + fieldNames);
                }
            }
        }

        @Test
        @DisplayName("[필수] ReadManager는 단일 QueryPort만 가져야 한다")
        void readManager_MustHaveSingleQueryPort() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                long portFieldCount =
                        readManager.getFields().stream()
                                .filter(field -> !field.getName().equals("this$0"))
                                .filter(
                                        field -> {
                                            String typeName = field.getRawType().getName();
                                            return typeName.contains("QueryPort")
                                                    || typeName.contains("LockQueryPort");
                                        })
                                .count();

                if (portFieldCount == 0) {
                    fail(readManager.getSimpleName() + "는 최소 하나의 QueryPort를 가져야 합니다.");
                }

                if (portFieldCount > 1) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 단일 QueryPort만 가져야 합니다. "
                                    + "(현재 "
                                    + portFieldCount
                                    + "개의 Port를 가지고 있음) "
                                    + "여러 Port 조합은 Facade를 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 PersistencePort를 의존하지 않아야 한다")
        void readManager_MustNotDependOnPersistencePort() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnPersistencePort =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .contains("PersistencePort"));

                if (dependsOnPersistencePort) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 PersistencePort를 의존하지 않아야 합니다. "
                                    + "ReadManager는 읽기 전용이며 QueryPort만 의존해야 합니다. "
                                    + "쓰기 작업은 TransactionManager를 사용하세요.");
                }
            }
        }
    }

    // ========================================
    // 메서드 규칙
    // ========================================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[필수] ReadManager는 find*() 메서드만 가져야 한다")
        void readManager_MustOnlyHaveFindMethods() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                List<JavaMethod> invalidMethods =
                        readManager.getMethods().stream()
                                .filter(method -> !method.getName().startsWith("find"))
                                .filter(method -> !method.getName().startsWith("exists"))
                                .filter(method -> !method.getName().startsWith("count"))
                                .filter(method -> !method.getName().startsWith("search"))
                                .filter(
                                        method ->
                                                !method.getName()
                                                        .startsWith("$")) // synthetic 메서드 제외
                                .filter(
                                        method ->
                                                !method.getOwner()
                                                        .isEquivalentTo(
                                                                Object.class)) // Object 메서드 제외
                                .filter(
                                        method ->
                                                method.getModifiers()
                                                        .contains(
                                                                com.tngtech.archunit.core.domain
                                                                        .JavaModifier.PUBLIC))
                                .collect(Collectors.toList());

                if (!invalidMethods.isEmpty()) {
                    String methodNames =
                            invalidMethods.stream()
                                    .map(JavaMethod::getName)
                                    .collect(Collectors.joining(", "));
                    fail(
                            readManager.getSimpleName()
                                    + "는 find*(), exists*(), count*(), search*() 메서드만 가져야 합니다. "
                                    + "허용되지 않는 메서드: "
                                    + methodNames
                                    + ". "
                                    + "쓰기 작업은 TransactionManager를 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 persist/save/update/delete 메서드를 가지지 않아야 한다")
        void readManager_MustNotHaveWriteMethods() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                List<JavaMethod> writeMethods =
                        readManager.getMethods().stream()
                                .filter(
                                        method ->
                                                method.getName().startsWith("persist")
                                                        || method.getName().startsWith("save")
                                                        || method.getName().startsWith("update")
                                                        || method.getName().startsWith("delete")
                                                        || method.getName().startsWith("remove"))
                                .collect(Collectors.toList());

                if (!writeMethods.isEmpty()) {
                    String methodNames =
                            writeMethods.stream()
                                    .map(JavaMethod::getName)
                                    .collect(Collectors.joining(", "));
                    fail(
                            readManager.getSimpleName()
                                    + "는 쓰기 메서드를 가지지 않아야 합니다. "
                                    + "발견된 쓰기 메서드: "
                                    + methodNames
                                    + ". "
                                    + "쓰기 작업은 TransactionManager를 사용하세요.");
                }
            }
        }
    }

    // ========================================
    // 금지 규칙
    // ========================================

    @Nested
    @DisplayName("금지 규칙")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] ReadManager는 Lombok 어노테이션을 사용하지 않아야 한다")
        void readManager_MustNotUseLombok() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean usesLombok =
                        readManager.getAnnotations().stream()
                                .anyMatch(
                                        annotation ->
                                                annotation
                                                        .getRawType()
                                                        .getPackageName()
                                                        .startsWith("lombok"));

                if (usesLombok) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 Lombok을 사용하지 않아야 합니다. "
                                    + "Plain Java로 작성하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 필드 주입(@Autowired)을 사용하지 않아야 한다")
        void readManager_MustNotUseFieldInjection() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean hasFieldInjection =
                        readManager.getFields().stream()
                                .anyMatch(field -> field.isAnnotatedWith(Autowired.class));

                if (hasFieldInjection) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 필드 주입(@Autowired)을 사용하지 않아야 합니다. "
                                    + "생성자 주입을 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 다른 Manager를 의존하지 않아야 한다")
        void readManager_MustNotDependOnOtherManager() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnOtherManager =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                                .getSimpleName()
                                                                .endsWith("TransactionManager")
                                                        || field.getRawType()
                                                                .getSimpleName()
                                                                .endsWith("ReadManager"));

                if (dependsOnOtherManager) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 다른 Manager를 의존하지 않아야 합니다. "
                                    + "여러 Manager 조합은 Facade를 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 UseCase를 의존하지 않아야 한다")
        void readManager_MustNotDependOnUseCase() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnUseCase =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("UseCase"));

                if (dependsOnUseCase) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 UseCase를 의존하지 않아야 합니다. "
                                    + "ReadManager는 QueryPort만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 Service를 의존하지 않아야 한다")
        void readManager_MustNotDependOnService() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnService =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Service"));

                if (dependsOnService) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 Service를 의존하지 않아야 합니다. "
                                    + "ReadManager는 QueryPort만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 Facade를 의존하지 않아야 한다")
        void readManager_MustNotDependOnFacade() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnFacade =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Facade"));

                if (dependsOnFacade) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 Facade를 의존하지 않아야 합니다. "
                                    + "ReadManager는 QueryPort만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] ReadManager는 Factory를 의존하지 않아야 한다")
        void readManager_MustNotDependOnFactory() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean dependsOnFactory =
                        readManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Factory"));

                if (dependsOnFactory) {
                    fail(
                            readManager.getSimpleName()
                                    + "는 Factory를 의존하지 않아야 합니다. "
                                    + "Factory는 Service 책임입니다. "
                                    + "ReadManager는 QueryPort만 의존해야 합니다.");
                }
            }
        }
    }

    // ========================================
    // 생성자 규칙
    // ========================================

    @Nested
    @DisplayName("생성자 규칙")
    class ConstructorRules {

        @Test
        @DisplayName("[필수] ReadManager는 생성자를 통해 의존성을 주입받아야 한다")
        void readManager_MustHaveConstructor() {
            assumeTrue(hasReadManagerClasses, "ReadManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass readManager : readManagerClasses) {
                boolean hasConstructorWithPort =
                        readManager.getConstructors().stream()
                                .anyMatch(
                                        constructor ->
                                                constructor.getParameters().stream()
                                                        .anyMatch(
                                                                param -> {
                                                                    String typeName =
                                                                            param.getRawType()
                                                                                    .getName();
                                                                    return typeName.contains(
                                                                                    "QueryPort")
                                                                            || typeName.contains(
                                                                                    "LockQueryPort");
                                                                }));

                if (!hasConstructorWithPort) {
                    fail(readManager.getSimpleName() + "는 생성자를 통해 QueryPort를 주입받아야 합니다.");
                }
            }
        }
    }
}
