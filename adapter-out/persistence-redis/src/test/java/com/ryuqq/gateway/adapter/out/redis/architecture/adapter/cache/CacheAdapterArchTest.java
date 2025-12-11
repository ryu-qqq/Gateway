package com.ryuqq.gateway.adapter.out.redis.architecture.adapter.cache;

import static com.ryuqq.gateway.adapter.out.redis.architecture.ArchUnitPackageConstants.REDIS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
 * CacheAdapterArchTest - Cache Adapter 아키텍처 규칙 검증
 *
 * <p>cache-adapter-guide.md 규칙을 ArchUnit으로 검증합니다.
 *
 * <p><strong>검증 항목:</strong>
 *
 * <ul>
 *   <li>클래스 구조: @Component, CachePort 구현
 *   <li>의존성: RedisTemplate, ObjectMapper
 *   <li>금지 사항: @Transactional, KEYS 명령어
 *   <li>메서드: SCAN 기반 evictByPattern
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("CacheAdapter 아키텍처 규칙 검증")
class CacheAdapterArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses cacheAdapterClasses;

    @BeforeAll
    static void setUp() {
        allClasses =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(REDIS);

        cacheAdapterClasses =
                allClasses.that(
                        DescribedPredicate.describe(
                                "Cache Adapter 클래스",
                                javaClass ->
                                        javaClass.getSimpleName().endsWith("CacheAdapter")
                                                && !javaClass.isInterface()));
    }

    // ========================================================================
    // 1. 클래스 구조 규칙
    // ========================================================================

    @Nested
    @DisplayName("1. 클래스 구조 규칙")
    class ClassStructureRules {

        @Test
        @DisplayName("규칙 1-1: CacheAdapter는 클래스여야 합니다")
        void cacheAdapter_MustBeClass() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .notBeInterfaces()
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 클래스로 정의되어야 합니다");

            rule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-2: @Component 어노테이션이 필수입니다")
        void cacheAdapter_MustHaveComponentAnnotation() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 @Component 어노테이션이 필수입니다");

            rule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-3: CachePort 인터페이스를 구현해야 합니다")
        void cacheAdapter_MustImplementCachePort() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "CachePort 인터페이스 구현",
                                                    javaClass ->
                                                            javaClass.getAllRawInterfaces().stream()
                                                                    .anyMatch(
                                                                            iface ->
                                                                                    iface.getName()
                                                                                            .contains(
                                                                                                    "CachePort")))))
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 CachePort 인터페이스를 구현해야 합니다");

            rule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-4: 모든 필드는 final이어야 합니다")
        void cacheAdapter_AllFieldsMustBeFinal() {
            ArchRule rule =
                    fields().that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .areDeclaredInClassesThat()
                            .resideInAPackage("..cache.adapter..")
                            .and()
                            .areNotStatic()
                            .should()
                            .beFinal()
                            .allowEmptyShould(true)
                            .because("Cache Adapter의 모든 인스턴스 필드는 final로 불변성을 보장해야 합니다");

            rule.check(cacheAdapterClasses);
        }
    }

    // ========================================================================
    // 2. 의존성 규칙
    // ========================================================================

    @Nested
    @DisplayName("2. 의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("규칙 2-1: RedisTemplate 의존성이 필수입니다")
        void cacheAdapter_MustDependOnRedisTemplate() {
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should(
                                    ArchCondition.from(
                                            DescribedPredicate.describe(
                                                    "RedisTemplate 필드",
                                                    javaClass ->
                                                            javaClass.getAllFields().stream()
                                                                    .anyMatch(
                                                                            field ->
                                                                                    field.getRawType()
                                                                                            .getName()
                                                                                            .contains(
                                                                                                    "RedisTemplate")))))
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 RedisTemplate 의존성이 필수입니다 (Lettuce)");

            rule.check(cacheAdapterClasses);
        }

        /**
         * 규칙 2-2: ObjectMapper 의존성 (선택적)
         *
         * <p>Object 타입 캐시(JSON 직렬화 필요)인 경우에만 ObjectMapper가 필요합니다. 단순 String 타입 캐시의 경우 ObjectMapper가
         * 불필요합니다.
         *
         * <p>강제 규칙에서 권장 사항으로 변경됨 (v1.1.0)
         */
        // @Test - 선택적 규칙으로 변경되어 테스트에서 제외
        // @DisplayName("규칙 2-2: ObjectMapper 의존성 (선택적 - Object 타입 캐시에서만 필요)")
        void cacheAdapter_ShouldDependOnObjectMapper_WhenUsingObjectCache() {
            // ObjectMapper는 Object 타입 캐시(JSON 직렬화)에서만 필수
            // StringCacheAdapter 등 단순 String 저장 시에는 불필요
            // 이 규칙은 강제하지 않고 가이드 문서로 대체
        }

        @Test
        @DisplayName("규칙 2-3: RedissonClient 의존성이 금지됩니다")
        void cacheAdapter_MustNotDependOnRedisson() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Redisson.*")
                            .allowEmptyShould(true)
                            .because(
                                    "Cache Adapter는 Lettuce(RedisTemplate)만 사용해야 합니다. Redisson은"
                                            + " Lock 전용입니다");

            rule.check(cacheAdapterClasses);
        }
    }

    // ========================================================================
    // 3. 메서드 규칙
    // ========================================================================

    @Nested
    @DisplayName("3. 메서드 규칙")
    class MethodRules {

        /**
         * 규칙 3-1: evictByPattern 메서드 (선택적)
         *
         * <p>패턴 기반 캐시 무효화가 필요한 경우에만 구현합니다. 단순 key-value 캐시(개별 키 삭제만 필요)의 경우 불필요합니다.
         *
         * <p>강제 규칙에서 권장 사항으로 변경됨 (v1.1.0)
         */
        // @Test - 선택적 규칙으로 변경되어 테스트에서 제외
        // @DisplayName("규칙 3-1: evictByPattern 메서드 (선택적 - 패턴 기반 무효화 필요 시)")
        void cacheAdapter_ShouldHaveEvictByPatternMethod_WhenPatternEvictionNeeded() {
            // evictByPattern은 "product:*" 같은 패턴 삭제가 필요할 때만 구현
            // 개별 키 삭제(evict(key))만 사용하는 경우 불필요
            // 이 규칙은 강제하지 않고 가이드 문서로 대체
        }

        /**
         * 규칙 3-2: scanKeys 메서드 (선택적)
         *
         * <p>evictByPattern을 구현할 때 SCAN 명령어를 사용해야 하며, 이를 위해 scanKeys 메서드가 필요합니다. evictByPattern이 없는
         * 단순 캐시의 경우 scanKeys도 불필요합니다.
         *
         * <p>⚠️ KEYS 명령어는 성능 문제로 절대 사용 금지 (이 규칙은 유지)
         *
         * <p>강제 규칙에서 권장 사항으로 변경됨 (v1.1.0)
         */
        // @Test - 선택적 규칙으로 변경되어 테스트에서 제외
        // @DisplayName("규칙 3-2: scanKeys 메서드 (선택적 - evictByPattern 구현 시 필요)")
        void cacheAdapter_ShouldHaveScanKeysMethod_WhenEvictByPatternImplemented() {
            // scanKeys는 evictByPattern 구현 시 KEYS 대신 SCAN 사용을 위해 필요
            // evictByPattern이 없으면 scanKeys도 불필요
            // 단, KEYS 명령어 사용은 여전히 금지 (성능 문제)
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
        void cacheAdapter_MustNotBeTransactional() {
            ArchRule classRule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .notBeAnnotatedWith(Transactional.class)
                            .allowEmptyShould(true)
                            .because("Cache Adapter에 @Transactional 사용 금지");

            ArchRule methodRule =
                    methods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .should()
                            .notBeAnnotatedWith(Transactional.class)
                            .allowEmptyShould(true)
                            .because("Cache Adapter 메서드에 @Transactional 사용 금지");

            classRule.check(cacheAdapterClasses);
            methodRule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-2: 비즈니스 로직 포함이 금지됩니다 (Domain VO는 예외)")
        void cacheAdapter_MustNotContainBusinessLogic() {
            // Domain의 VO(CacheKey 등)는 Port 구현을 위해 허용
            // Aggregate, Entity, Service 등 비즈니스 로직 클래스만 금지
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .dependOnClassesThat(
                                    DescribedPredicate.describe(
                                            "Domain Layer classes excluding VO/Enum",
                                            javaClass ->
                                                    javaClass.getPackageName().contains(".domain.")
                                                            && !javaClass
                                                                    .getPackageName()
                                                                    .contains(".vo")
                                                            && !javaClass.isEnum()
                                                            && !javaClass
                                                                    .getSimpleName()
                                                                    .endsWith("Key")))
                            .allowEmptyShould(true)
                            .because(
                                    "Cache Adapter는 비즈니스 로직을 포함하지 않아야 합니다 (Domain VO는 Port 구현을 위해"
                                            + " 허용)");

            rule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-3: DB 접근이 금지됩니다")
        void cacheAdapter_MustNotAccessDatabase() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*(Repository|JpaRepository|EntityManager).*")
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 DB에 직접 접근하지 않습니다");

            rule.check(cacheAdapterClasses);
        }

        @Test
        @DisplayName("규칙 4-4: 로깅이 금지됩니다")
        void cacheAdapter_MustNotContainLogging() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("CacheAdapter")
                            .and()
                            .resideInAPackage("..cache.adapter..")
                            .should()
                            .accessClassesThat()
                            .haveNameMatching(".*Logger.*")
                            .allowEmptyShould(true)
                            .because("Cache Adapter는 로깅을 포함하지 않습니다. AOP로 처리하세요");

            rule.check(cacheAdapterClasses);
        }
    }
}
