package com.ryuqq.gateway.application.architecture.manager;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
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
 * TransactionManager ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>TransactionManager의 구조 규칙을 검증합니다.
 *
 * <h3>핵심 원칙:</h3>
 *
 * <ul>
 *   <li>TransactionManager는 Class여야 함 (구현체)
 *   <li>단일 Out Port만 의존 (PersistencePort, QueryPort, LockQueryPort 중 하나)
 *   <li>persist() 메서드만 허용 (순수 위임)
 *   <li>@Component + @Transactional 필수
 *   <li>Lombok 금지, 생성자 주입만
 * </ul>
 *
 * <h3>책임:</h3>
 *
 * <ul>
 *   <li>트랜잭션 경계 제공
 *   <li>Port 호출 순수 위임
 *   <li>비즈니스 로직 없음
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("TransactionManager ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class TransactionManagerArchTest {

    private static JavaClasses classes;
    private static boolean hasTransactionManagerClasses;
    private static List<JavaClass> transactionManagerClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.fileflow.application");

        transactionManagerClasses =
                classes.stream()
                        .filter(
                                javaClass ->
                                        javaClass.getSimpleName().endsWith("TransactionManager"))
                        .filter(javaClass -> javaClass.getPackageName().contains(".manager"))
                        .filter(javaClass -> !javaClass.isInterface())
                        .collect(Collectors.toList());

        hasTransactionManagerClasses = !transactionManagerClasses.isEmpty();
    }

    // ========================================
    // 기본 구조 규칙
    // ========================================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] TransactionManager는 '*TransactionManager' 접미사를 가져야 한다")
        void transactionManager_MustHaveCorrectSuffix() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .should()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .because("TransactionManager는 'TransactionManager' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] TransactionManager는 ..manager.. 패키지에 위치해야 한다")
        void transactionManager_MustBeInManagerPackage() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .resideInAPackage("..manager..")
                            .because("TransactionManager는 manager 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] TransactionManager는 Class여야 한다 (Interface 아님)")
        void transactionManager_MustBeClass() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            long violationCount =
                    classes.stream()
                            .filter(
                                    javaClass ->
                                            javaClass
                                                    .getSimpleName()
                                                    .endsWith("TransactionManager"))
                            .filter(javaClass -> javaClass.getPackageName().contains(".manager"))
                            .filter(JavaClass::isInterface)
                            .count();

            if (violationCount > 0) {
                fail(
                        "TransactionManager는 Interface가 아닌 Class여야 합니다. (위반 수: "
                                + violationCount
                                + ")");
            }
        }

        @Test
        @DisplayName("[필수] TransactionManager는 public이어야 한다")
        void transactionManager_MustBePublic() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .should()
                            .bePublic()
                            .because("TransactionManager는 외부에서 접근 가능해야 합니다");

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
        @DisplayName("[필수] TransactionManager는 @Component 어노테이션이 필수이다")
        void transactionManager_MustHaveComponentAnnotation() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("TransactionManager는 @Component로 Bean 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] TransactionManager는 @Transactional 어노테이션이 필수이다")
        void transactionManager_MustHaveTransactionalAnnotation() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("TransactionManager")
                            .and()
                            .resideInAPackage("..manager..")
                            .and()
                            .areNotInterfaces()
                            .should()
                            .beAnnotatedWith(Transactional.class)
                            .because("TransactionManager는 @Transactional로 트랜잭션 경계를 정의해야 합니다");

            rule.check(classes);
        }
    }

    // ========================================
    // 의존성 규칙
    // ========================================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] TransactionManager는 Out Port 인터페이스만 필드로 가져야 한다")
        void transactionManager_MustOnlyDependOnOutPort() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                List<JavaField> nonPortFields =
                        txManager.getFields().stream()
                                .filter(
                                        field ->
                                                !field.getName().equals("this$0")) // inner class 제외
                                .filter(
                                        field -> {
                                            String typeName = field.getRawType().getName();
                                            // Out Port 인터페이스인지 확인 (PersistencePort, QueryPort,
                                            // LockQueryPort)
                                            boolean isOutPort =
                                                    typeName.contains("PersistencePort")
                                                            || typeName.contains("QueryPort")
                                                            || typeName.contains("LockQueryPort");
                                            return !isOutPort;
                                        })
                                .collect(Collectors.toList());

                if (!nonPortFields.isEmpty()) {
                    String fieldNames =
                            nonPortFields.stream()
                                    .map(
                                            f ->
                                                    f.getName()
                                                            + " ("
                                                            + f.getRawType().getSimpleName()
                                                            + ")")
                                    .collect(Collectors.joining(", "));
                    fail(
                            txManager.getSimpleName()
                                    + "는 Out Port 인터페이스만 필드로 가져야 합니다. "
                                    + "허용되지 않는 필드: "
                                    + fieldNames);
                }
            }
        }

        @Test
        @DisplayName("[필수] TransactionManager는 단일 Out Port만 가져야 한다")
        void transactionManager_MustHaveSingleOutPort() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                long portFieldCount =
                        txManager.getFields().stream()
                                .filter(field -> !field.getName().equals("this$0"))
                                .filter(
                                        field -> {
                                            String typeName = field.getRawType().getName();
                                            return typeName.contains("PersistencePort")
                                                    || typeName.contains("QueryPort")
                                                    || typeName.contains("LockQueryPort");
                                        })
                                .count();

                if (portFieldCount == 0) {
                    fail(txManager.getSimpleName() + "는 최소 하나의 Out Port를 가져야 합니다.");
                }

                if (portFieldCount > 1) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 단일 Out Port만 가져야 합니다. "
                                    + "(현재 "
                                    + portFieldCount
                                    + "개의 Port를 가지고 있음) "
                                    + "여러 Port 조합은 Facade를 사용하세요.");
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
        @DisplayName("[필수] TransactionManager는 persist* 메서드만 가져야 한다")
        void transactionManager_MustOnlyHavePersistMethod() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                List<JavaMethod> invalidMethods =
                        txManager.getMethods().stream()
                                .filter(method -> !method.getName().startsWith("persist"))
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
                            txManager.getSimpleName()
                                    + "는 persist* 메서드만 가져야 합니다. "
                                    + "허용되지 않는 메서드: "
                                    + methodNames
                                    + ". "
                                    + "비즈니스 로직은 UseCase에서 처리하세요.");
                }
            }
        }

        @Test
        @DisplayName("[필수] TransactionManager는 persist* 메서드를 가져야 한다")
        void transactionManager_MustHavePersistMethod() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean hasPersistMethod =
                        txManager.getMethods().stream()
                                .anyMatch(method -> method.getName().startsWith("persist"));

                if (!hasPersistMethod) {
                    fail(txManager.getSimpleName() + "는 persist* 메서드를 가져야 합니다.");
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
        @DisplayName("[금지] TransactionManager는 Lombok 어노테이션을 사용하지 않아야 한다")
        void transactionManager_MustNotUseLombok() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean usesLombok =
                        txManager.getAnnotations().stream()
                                .anyMatch(
                                        annotation ->
                                                annotation
                                                        .getRawType()
                                                        .getPackageName()
                                                        .startsWith("lombok"));

                if (usesLombok) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 Lombok을 사용하지 않아야 합니다. "
                                    + "Plain Java로 작성하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 필드 주입(@Autowired)을 사용하지 않아야 한다")
        void transactionManager_MustNotUseFieldInjection() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean hasFieldInjection =
                        txManager.getFields().stream()
                                .anyMatch(field -> field.isAnnotatedWith(Autowired.class));

                if (hasFieldInjection) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 필드 주입(@Autowired)을 사용하지 않아야 합니다. "
                                    + "생성자 주입을 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 다른 TransactionManager를 의존하지 않아야 한다")
        void transactionManager_MustNotDependOnOtherTransactionManager() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean dependsOnOtherManager =
                        txManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("TransactionManager"));

                if (dependsOnOtherManager) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 다른 TransactionManager를 의존하지 않아야 합니다. "
                                    + "여러 Manager 조합은 Facade를 사용하세요.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 UseCase를 의존하지 않아야 한다")
        void transactionManager_MustNotDependOnUseCase() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean dependsOnUseCase =
                        txManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("UseCase"));

                if (dependsOnUseCase) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 UseCase를 의존하지 않아야 합니다. "
                                    + "TransactionManager는 Out Port만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 Service를 의존하지 않아야 한다")
        void transactionManager_MustNotDependOnService() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean dependsOnService =
                        txManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Service"));

                if (dependsOnService) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 Service를 의존하지 않아야 합니다. "
                                    + "TransactionManager는 Out Port만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 Facade를 의존하지 않아야 한다")
        void transactionManager_MustNotDependOnFacade() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean dependsOnFacade =
                        txManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Facade"));

                if (dependsOnFacade) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 Facade를 의존하지 않아야 합니다. "
                                    + "TransactionManager는 Out Port만 의존해야 합니다.");
                }
            }
        }

        @Test
        @DisplayName("[금지] TransactionManager는 Factory를 의존하지 않아야 한다")
        void transactionManager_MustNotDependOnFactory() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean dependsOnFactory =
                        txManager.getFields().stream()
                                .anyMatch(
                                        field ->
                                                field.getRawType()
                                                        .getSimpleName()
                                                        .endsWith("Factory"));

                if (dependsOnFactory) {
                    fail(
                            txManager.getSimpleName()
                                    + "는 Factory를 의존하지 않아야 합니다. "
                                    + "Factory는 Service 책임입니다. "
                                    + "TransactionManager는 Out Port만 의존해야 합니다.");
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
        @DisplayName("[필수] TransactionManager는 생성자를 통해 의존성을 주입받아야 한다")
        void transactionManager_MustHaveConstructor() {
            assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");

            for (JavaClass txManager : transactionManagerClasses) {
                boolean hasConstructorWithPort =
                        txManager.getConstructors().stream()
                                .anyMatch(
                                        constructor ->
                                                constructor.getParameters().stream()
                                                        .anyMatch(
                                                                param -> {
                                                                    String typeName =
                                                                            param.getRawType()
                                                                                    .getName();
                                                                    return typeName.contains(
                                                                                    "PersistencePort")
                                                                            || typeName.contains(
                                                                                    "QueryPort")
                                                                            || typeName.contains(
                                                                                    "LockQueryPort");
                                                                }));

                if (!hasConstructorWithPort) {
                    fail(txManager.getSimpleName() + "는 생성자를 통해 Out Port를 주입받아야 합니다.");
                }
            }
        }
    }
}
