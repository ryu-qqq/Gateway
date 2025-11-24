package com.company.connectly.adapter.out.persistence.architecture.adpater.query;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * QueryAdapter 아키텍처 규칙 검증 테스트
 *
 * <p>CQRS Query Adapter의 Zero-Tolerance 규칙을 자동으로 검증합니다:</p>
 * <ul>
 *   <li>정확한 필드 개수 (2개): QueryDslRepository, Mapper</li>
 *   <li>정확한 메서드 개수 (3개): findById(), findByCriteria(), countByCriteria()</li>
 *   <li>메서드 네이밍 규칙: findById, findByCriteria, countByCriteria</li>
 *   <li>반환 타입 규칙: Optional&lt;Domain&gt;, List&lt;Domain&gt;, long</li>
 *   <li>@Component 필수</li>
 *   <li>@Transactional 금지</li>
 *   <li>Command 메서드 금지 (save, persist, update, delete)</li>
 *   <li>비즈니스 로직 금지</li>
 *   <li>JPAQueryFactory 직접 사용 금지</li>
 *   <li>Mapper.toDomain() 호출 필수</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("QueryAdapter 아키텍처 규칙 검증 (Zero-Tolerance)")
class QueryAdapterArchTest {

    private static JavaClasses queryAdapterClasses;

    @BeforeAll
    static void setUp() {
        queryAdapterClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");
    }

    /**
     * 규칙 1: @Component 어노테이션 필수
     *
     * <p>QueryAdapter는 Spring Bean으로 등록되어야 합니다.</p>
     * <ul>
     *   <li>✅ @Component</li>
     *   <li>❌ @Service (Application Layer 전용)</li>
     *   <li>❌ @Repository (JpaRepository 인터페이스 전용)</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 1: @Component 어노테이션 필수")
    void queryAdapter_MustBeAnnotatedWithComponent() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
            .because("QueryAdapter는 @Component로 Spring Bean 등록이 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 2: *QueryPort 인터페이스 구현 필수
     *
     * <p>QueryAdapter는 Application Layer의 Port 인터페이스를 구현해야 합니다.</p>
     * <ul>
     *   <li>Port 네이밍: *QueryPort, *LoadPort</li>
     *   <li>Port 위치: application layer의 port.out 패키지</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 2: *QueryPort 인터페이스 구현 필수")
    void queryAdapter_MustImplementQueryPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "interface ending with 'QueryPort' or 'LoadPort'",
                javaClass -> javaClass.getAllRawInterfaces().stream()
                    .anyMatch(iface ->
                        iface.getSimpleName().endsWith("QueryPort") ||
                        iface.getSimpleName().endsWith("LoadPort")
                    )
            ))
            .because("QueryAdapter는 Application Layer의 Query Port를 구현해야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 3: 정확히 2개 필드 (QueryDslRepository, Mapper)
     *
     * <p>QueryAdapter는 정확히 2개의 필드만 가져야 합니다:</p>
     * <ul>
     *   <li>1. QueryDslRepository (*QueryDslRepository)</li>
     *   <li>2. Mapper (*JpaEntityMapper 또는 *EntityMapper)</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 3: 정확히 2개 필드 (QueryDslRepository, Mapper)")
    void queryAdapter_MustHaveExactlyTwoFields() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have exactly 2 fields",
                    javaClass -> javaClass.getAllFields().size() == 2
                )
            ))
            .because("QueryAdapter는 정확히 2개의 필드(QueryDslRepository, Mapper)만 가져야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 4: 모든 필드는 final 필수
     *
     * <p>불변성(Immutability) 보장을 위해 모든 필드는 final이어야 합니다.</p>
     */
    @Test
    @DisplayName("규칙 4: 모든 필드는 final 필수")
    void queryAdapter_AllFieldsMustBeFinal() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().beFinal()
            .because("QueryAdapter의 모든 필드는 final로 불변성을 보장해야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 5: 생성자 주입만 허용 (Field Injection 금지)
     *
     * <p>Spring 권장사항에 따라 생성자 주입만 사용해야 합니다.</p>
     * <ul>
     *   <li>✅ 생성자 주입 (final 필드)</li>
     *   <li>❌ @Autowired 필드 주입</li>
     *   <li>❌ Setter 주입</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 5: @Autowired 필드 주입 금지")
    void queryAdapter_MustNotUseFieldInjection() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .because("QueryAdapter는 생성자 주입만 허용되며, @Autowired 필드 주입은 금지입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 6: 정확히 4개의 public 메서드 (findById, existsById, findByCriteria, countByCriteria)
     *
     * <p>QueryAdapter는 4개의 조회 메서드만 public으로 노출해야 합니다.</p>
     * <ul>
     *   <li>✅ findById()</li>
     *   <li>✅ existsById()</li>
     *   <li>✅ findByCriteria()</li>
     *   <li>✅ countByCriteria()</li>
     *   <li>❌ 추가 메서드 금지</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 6: 정확히 4개의 public 메서드")
    void queryAdapter_MustHaveExactlyFourPublicMethods() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have exactly 4 public methods (excluding constructor)",
                    javaClass -> javaClass.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                        .filter(method -> !method.getName().equals("<init>"))
                        .count() == 4
                )
            ))
            .because("QueryAdapter는 findById, existsById, findByCriteria, countByCriteria 메서드만 public으로 노출해야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 7: findById() 메서드 필수
     *
     * <p>단건 조회를 위한 findById() 메서드가 필수입니다.</p>
     * <ul>
     *   <li>✅ Optional&lt;Domain&gt; findById(DomainId id)</li>
     *   <li>❌ Domain findById(Long id) - Optional 필수</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 7: findById() 메서드 필수")
    void queryAdapter_MustHaveFindByIdMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have public method 'findById'",
                    javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.getName().equals("findById") &&
                            method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                )
            ))
            .because("QueryAdapter는 단건 조회를 위한 findById() 메서드가 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 8: findByCriteria() 메서드 필수
     *
     * <p>목록 조회를 위한 findByCriteria() 메서드가 필수입니다.</p>
     * <ul>
     *   <li>✅ List&lt;Domain&gt; findByCriteria(SearchCriteria criteria)</li>
     *   <li>❌ List&lt;DTO&gt; findByCriteria() - Domain 반환 필수</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 8: findByCriteria() 메서드 필수")
    void queryAdapter_MustHaveFindByCriteriaMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have public method 'findByCriteria'",
                    javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.getName().equals("findByCriteria") &&
                            method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                )
            ))
            .because("QueryAdapter는 목록 조회를 위한 findByCriteria() 메서드가 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 9: countByCriteria() 메서드 필수
     *
     * <p>개수 조회를 위한 countByCriteria() 메서드가 필수입니다.</p>
     * <ul>
     *   <li>✅ long countByCriteria(SearchCriteria criteria)</li>
     *   <li>❌ int countByCriteria() - long 반환 필수</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 9: countByCriteria() 메서드 필수")
    void queryAdapter_MustHaveCountByCriteriaMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have public method 'countByCriteria'",
                    javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.getName().equals("countByCriteria") &&
                            method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                )
            ))
            .because("QueryAdapter는 개수 조회를 위한 countByCriteria() 메서드가 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 10: existsById() 메서드 필수
     *
     * <p>존재 여부 확인을 위한 existsById() 메서드가 필수입니다.</p>
     * <ul>
     *   <li>✅ boolean existsById(DomainId id)</li>
     *   <li>✅ 내부적으로 findById().isPresent() 재사용</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 10: existsById() 메서드 필수")
    void queryAdapter_MustHaveExistsByIdMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should(com.tngtech.archunit.lang.ArchCondition.from(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "have public method 'existsById'",
                    javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.getName().equals("existsById") &&
                            method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                )
            ))
            .because("QueryAdapter는 존재 여부 확인을 위한 existsById() 메서드가 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 11: @Transactional 금지
     *
     * <p>Query는 읽기 전용이므로 Transaction이 불필요합니다.</p>
     * <ul>
     *   <li>❌ @Transactional - QueryAdapter에 사용 금지</li>
     *   <li>✅ 읽기 전용 작업은 Transaction 불필요</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 10: @Transactional 금지")
    void queryAdapter_MustNotBeAnnotatedWithTransactional() {
        ArchRule classRule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().notBeAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
            .because("QueryAdapter 클래스에 @Transactional 사용 금지. 읽기 전용 작업입니다");

        ArchRule methodRule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().notBeAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
            .because("QueryAdapter 메서드에 @Transactional 사용 금지. 읽기 전용 작업입니다");

        classRule.check(queryAdapterClasses);
        methodRule.check(queryAdapterClasses);
    }

    /**
     * 규칙 11: Command 메서드 금지 (save, persist, update, delete 등)
     *
     * <p>CQRS 원칙에 따라 Query Adapter는 쓰기 메서드를 포함하면 안 됩니다.</p>
     * <ul>
     *   <li>❌ save(), persist(), update(), delete() 등</li>
     *   <li>✅ 쓰기는 CommandAdapter로 분리</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 11: Command 메서드 금지")
    void queryAdapter_MustNotContainCommandMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveNameNotMatching("(save|persist|update|delete|insert|remove|create).*")
            .because("QueryAdapter는 Command 메서드를 포함하면 안 됩니다. CommandAdapter로 분리하세요");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 12: 비즈니스 메서드 금지
     *
     * <p>QueryAdapter는 단순 조회만 수행하며, 비즈니스 로직을 포함하면 안 됩니다.</p>
     * <ul>
     *   <li>❌ confirm(), cancel(), approve() 등</li>
     *   <li>✅ 비즈니스 로직은 Domain에서</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 12: 비즈니스 메서드 금지")
    void queryAdapter_MustNotContainBusinessMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveNameNotMatching("(confirm|cancel|approve|reject|modify|change|validate|calculate).*")
            .because("QueryAdapter는 비즈니스 메서드를 포함하면 안 됩니다. Domain에서 처리하세요");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 13: QueryDslRepository 의존성 필수
     *
     * <p>QueryAdapter는 QueryDslRepository를 필드로 가져야 합니다.</p>
     * <ul>
     *   <li>✅ private final *QueryDslRepository repository</li>
     *   <li>❌ JPAQueryFactory 직접 사용 금지</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 13: QueryDslRepository 의존성 필수")
    void queryAdapter_MustDependOnQueryDslRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("QueryDslRepository")
            .because("QueryAdapter는 QueryDslRepository를 의존성으로 가져야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 14: Mapper 의존성 필수
     *
     * <p>Entity → Domain 변환은 반드시 Mapper를 통해 수행해야 합니다.</p>
     * <ul>
     *   <li>✅ private final *Mapper mapper</li>
     *   <li>❌ Entity를 그대로 반환 금지</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 14: Mapper 의존성 필수")
    void queryAdapter_MustDependOnMapper() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
            .because("QueryAdapter는 Mapper를 의존성으로 가져야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 15: @Override 어노테이션 필수
     *
     * <p>모든 public 메서드는 Port 인터페이스를 구현하므로 @Override가 필수입니다.</p>
     */
    @Test
    @DisplayName("규칙 15: @Override 어노테이션 필수")
    void queryAdapter_AllPublicMethodsMustHaveOverrideAnnotation() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().arePublic()
            .and().doNotHaveFullName(".*<init>.*")
            .should().beAnnotatedWith(Override.class)
            .because("QueryAdapter의 모든 public 메서드는 Port 인터페이스 구현이므로 @Override가 필수입니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 16: JPAQueryFactory 직접 사용 금지
     *
     * <p>QueryAdapter는 QueryDslRepository를 통해서만 조회해야 합니다.</p>
     * <ul>
     *   <li>❌ private final JPAQueryFactory queryFactory - 직접 사용 금지</li>
     *   <li>✅ private final *QueryDslRepository repository - Repository 사용</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 16: JPAQueryFactory 직접 사용 금지")
    void queryAdapter_MustNotUseJPAQueryFactoryDirectly() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().accessClassesThat().haveNameMatching(".*JPAQueryFactory.*")
            .because("QueryAdapter는 JPAQueryFactory를 직접 사용하지 않고 QueryDslRepository를 통해 조회해야 합니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 17: private helper 메서드 금지
     *
     * <p>단순성 유지를 위해 추가 helper 메서드를 만들지 않습니다.</p>
     * <ul>
     *   <li>❌ private List&lt;Domain&gt; convertToDomain() - Mapper 사용</li>
     *   <li>❌ private void enrichData() - 데이터 가공은 Application에서</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 17: private helper 메서드 금지")
    void queryAdapter_MustNotHavePrivateHelperMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().arePrivate()
            .should().haveNameMatching("<init>")
            .because("QueryAdapter는 private helper 메서드를 가질 수 없습니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 18: 로깅 금지
     *
     * <p>단순 조회 작업에 로깅은 불필요합니다. 필요 시 AOP로 처리하세요.</p>
     * <ul>
     *   <li>❌ log.info("Querying order: {}", orderId)</li>
     *   <li>✅ AOP 기반 로깅</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 18: 로깅 금지")
    void queryAdapter_MustNotContainLogging() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().accessClassesThat().haveNameMatching(".*Logger.*")
            .because("QueryAdapter는 로깅을 포함하지 않습니다. AOP로 처리하세요");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 19: Validator 의존성 금지
     *
     * <p>조회 시 유효성 검사는 불필요합니다.</p>
     * <ul>
     *   <li>❌ @Autowired Validator validator</li>
     *   <li>✅ 조회는 검증 불필요</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 19: Validator 의존성 금지")
    void queryAdapter_MustNotDependOnValidator() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().accessClassesThat().haveNameMatching(".*Validator.*")
            .because("QueryAdapter는 Validator를 사용하지 않습니다");

        rule.check(queryAdapterClasses);
    }

    /**
     * 규칙 20: *QueryAdapter 네이밍 규칙
     *
     * <p>QueryAdapter는 반드시 "QueryAdapter"로 끝나야 합니다.</p>
     * <ul>
     *   <li>✅ OrderQueryAdapter, ProductQueryAdapter</li>
     *   <li>❌ OrderAdapter, OrderLoadAdapter - 네이밍 불명확</li>
     * </ul>
     */
    @Test
    @DisplayName("규칙 20: *QueryAdapter 네이밍 규칙")
    void queryAdapter_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "interface ending with 'QueryPort' or 'LoadPort'",
                javaClass -> javaClass.getAllRawInterfaces().stream()
                    .anyMatch(iface ->
                        iface.getSimpleName().endsWith("QueryPort") ||
                        iface.getSimpleName().endsWith("LoadPort")
                    )
            ))
            .and().resideInAPackage("..adapter..")
            .should().haveSimpleNameEndingWith("QueryAdapter")
            .because("Query Adapter는 반드시 *QueryAdapter 네이밍 규칙을 따라야 합니다");

        rule.check(queryAdapterClasses);
    }
}
