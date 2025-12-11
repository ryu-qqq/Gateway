package com.ryuqq.gateway.adapter.out.redis.architecture.adapter.lock;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DistributedLockAdapterArchTest - 분산락 Adapter 아키텍처 규칙 검증
 *
 * <p>lock-adapter-guide.md 규칙을 ArchUnit으로 검증합니다.
 *
 * <p><strong>검증 항목:</strong>
 *
 * <ul>
 *   <li>클래스 구조: @Component, DistributedLockPort 구현
 *   <li>의존성: RedissonClient (Pub/Sub 기반 Lock)
 *   <li>금지 사항: @Transactional, DB 접근, 비즈니스 로직
 *   <li>메서드: tryLock, unlock, isHeldByCurrentThread, isLocked
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("DistributedLockAdapter 아키텍처 규칙 검증")
class DistributedLockAdapterArchTest {

    private static final String BASE_PACKAGE = "com.ryuqq.adapter.out.persistence.redis";

    private static JavaClasses allClasses;
    private static JavaClasses lockAdapterClasses;

    @BeforeAll
    static void setUp() {
        allClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(BASE_PACKAGE);

        lockAdapterClasses =
                allClasses.that(
                        DescribedPredicate.describe(
                                "Distributed Lock Adapter 클래스",
                                javaClass ->
                                        javaClass.getSimpleName().contains("LockAdapter")
                                                && !javaClass.isInterface()));
    }

    // ========================================================================
    // 1. 클래스 구조 규칙
    // ========================================================================

    @Nested
    @DisplayName("1. 클래스 구조 규칙")
    class ClassStructureRules {

        @Test
        @DisplayName("규칙 1-1: LockAdapter는 클래스여야 합니다")
        void lockAdapter_MustBeClass() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .notBeInterfaces()
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 클래스로 정의되어야 합니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-2: @Component 어노테이션이 필수입니다")
        void lockAdapter_MustHaveComponentAnnotation() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 @Component 어노테이션이 필수입니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-3: DistributedLockPort 인터페이스를 구현해야 합니다")
        void lockAdapter_MustImplementDistributedLockPort() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "DistributedLockPort 인터페이스 구현",
                                                    javaClass ->
                                                            javaClass.getAllRawInterfaces().stream()
                                                                    .anyMatch(
                                                                            iface ->
                                                                                    iface.getName()
                                                                                            .contains(
                                                                                                    "LockPort")))))
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 DistributedLockPort 인터페이스를 구현해야 합니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-4: 모든 필드는 final이어야 합니다")
        void lockAdapter_AllFieldsMustBeFinal() {
            ArchRule rule =
                    fields().that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..lock.adapter..")
                            .and()
                            .areNotStatic()
                            .should()
                            .beFinal()
                            .allowEmptyShould(true)
                            .because("Lock Adapter의 모든 인스턴스 필드는 final로 불변성을 보장해야 합니다");

            rule.check(lockAdapterClasses);
        }
    }

    // ========================================================================
    // 2. 의존성 규칙
    // ========================================================================

    @Nested
    @DisplayName("2. 의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("규칙 2-1: RedissonClient 의존성이 필수입니다")
        void lockAdapter_MustDependOnRedissonClient() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "RedissonClient 필드",
                                                    javaClass ->
                                                            javaClass.getAllFields().stream()
                                                                    .anyMatch(
                                                                            field ->
                                                                                    field.getRawType()
                                                                                            .getName()
                                                                                            .contains(
                                                                                                    "RedissonClient")))))
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 RedissonClient 의존성이 필수입니다 (Pub/Sub 기반 Lock)");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 2-2: RedisTemplate 의존성이 금지됩니다")
        void lockAdapter_MustNotDependOnRedisTemplate() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*RedisTemplate.*")
                            .allowEmptyShould(true)
                            .because(
                                    "Lock Adapter는 Redisson만 사용해야 합니다. RedisTemplate은 Cache 전용입니다");

            rule.check(lockAdapterClasses);
        }
    }

    // ========================================================================
    // 3. 메서드 규칙
    // ========================================================================

    @Nested
    @DisplayName("3. 메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("규칙 3-1: tryLock 메서드가 필수입니다")
        void lockAdapter_MustHaveTryLockMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "tryLock 메서드",
                                                    javaClass ->
                                                            javaClass.getMethods().stream()
                                                                    .anyMatch(
                                                                            method ->
                                                                                    method.getName()
                                                                                            .equals(
                                                                                                    "tryLock")))))
                            .allowEmptyShould(true)
                            .because("분산락 획득 메서드(tryLock)가 필수입니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 3-2: unlock 메서드가 필수입니다")
        void lockAdapter_MustHaveUnlockMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "unlock 메서드",
                                                    javaClass ->
                                                            javaClass.getMethods().stream()
                                                                    .anyMatch(
                                                                            method ->
                                                                                    method.getName()
                                                                                            .equals(
                                                                                                    "unlock")))))
                            .allowEmptyShould(true)
                            .because("분산락 해제 메서드(unlock)가 필수입니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 3-3: isHeldByCurrentThread 메서드가 필수입니다")
        void lockAdapter_MustHaveIsHeldByCurrentThreadMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "isHeldByCurrentThread 메서드",
                                                    javaClass ->
                                                            javaClass.getMethods().stream()
                                                                    .anyMatch(
                                                                            method ->
                                                                                    method.getName()
                                                                                            .equals(
                                                                                                    "isHeldByCurrentThread")))))
                            .allowEmptyShould(true)
                            .because("현재 스레드 Lock 보유 확인 메서드가 필수입니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 3-4: isLocked 메서드가 필수입니다")
        void lockAdapter_MustHaveIsLockedMethod() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "isLocked 메서드",
                                                    javaClass ->
                                                            javaClass.getMethods().stream()
                                                                    .anyMatch(
                                                                            method ->
                                                                                    method.getName()
                                                                                            .equals(
                                                                                                    "isLocked")))))
                            .allowEmptyShould(true)
                            .because("Lock 상태 확인 메서드가 필수입니다");

            rule.check(lockAdapterClasses);
        }
    }

    // ========================================================================
    // 4. 금지 사항 규칙
    // ========================================================================

    @Nested
    @DisplayName("4. 금지 사항 규칙")
    class ProhibitionRules {

        @Test
        @DisplayName("규칙 4-1: @Transactional 사용이 금지됩니다")
        void lockAdapter_MustNotBeTransactional() {
            ArchRule classRule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .notBeAnnotatedWith(Transactional.class)
                            .allowEmptyShould(true)
                            .because("Lock Adapter에 @Transactional 사용 금지");

            ArchRule methodRule =
                    methods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameContaining("LockAdapter")
                            .should()
                            .notBeAnnotatedWith(Transactional.class)
                            .allowEmptyShould(true)
                            .because("Lock Adapter 메서드에 @Transactional 사용 금지");

            classRule.check(lockAdapterClasses);
            methodRule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-2: 비즈니스 로직 포함이 금지됩니다")
        void lockAdapter_MustNotContainBusinessLogic() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage("..domain..")
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 비즈니스 로직을 포함하지 않아야 합니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-3: DB 접근이 금지됩니다")
        void lockAdapter_MustNotAccessDatabase() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*(Repository|JpaRepository|EntityManager).*")
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 DB에 직접 접근하지 않습니다");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-4: 로깅이 금지됩니다")
        void lockAdapter_MustNotContainLogging() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should()
                            .accessClassesThat()
                            .haveNameMatching(".*Logger.*")
                            .allowEmptyShould(true)
                            .because("Lock Adapter는 로깅을 포함하지 않습니다. AOP로 처리하세요");

            rule.check(lockAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-5: 스핀락 패턴이 금지됩니다")
        void lockAdapter_MustNotUseSpinlock() {
            ArchRule rule =
                    noMethods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameContaining("LockAdapter")
                            .should()
                            .haveNameMatching(".*(sleep|wait|Thread\\.sleep).*")
                            .allowEmptyShould(true)
                            .because("Redisson Pub/Sub 기반 Lock을 사용하세요. 스핀락 금지입니다");

            rule.check(lockAdapterClasses);
        }
    }

    // ========================================================================
    // 5. 안전성 규칙
    // ========================================================================

    @Nested
    @DisplayName("5. 안전성 규칙")
    class SafetyRules {

        @Test
        @DisplayName("규칙 5-1: Lock 캐시를 사용해야 합니다 (Thread-safe)")
        void lockAdapter_MustUseLockCache() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameContaining("LockAdapter")
                            .and()
                            .resideInAPackage("..lock.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "ConcurrentHashMap Lock 캐시",
                                                    javaClass ->
                                                            javaClass.getAllFields().stream()
                                                                    .anyMatch(
                                                                            field ->
                                                                                    field.getRawType()
                                                                                                    .getName()
                                                                                                    .contains(
                                                                                                            "ConcurrentHashMap")
                                                                                            || field.getName()
                                                                                                    .contains(
                                                                                                            "lockCache")
                                                                                            || field.getName()
                                                                                                    .contains(
                                                                                                            "locks")))))
                            .allowEmptyShould(true)
                            .because("같은 키에 대해 동일한 Lock 인스턴스를 반환하기 위해 Thread-safe 캐시가 필수입니다");

            rule.check(lockAdapterClasses);
        }
    }
}
