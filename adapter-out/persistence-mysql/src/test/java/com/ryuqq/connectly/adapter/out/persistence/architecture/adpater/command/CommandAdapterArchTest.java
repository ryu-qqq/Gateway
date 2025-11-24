package com.ryuqq.connectly.adapter.out.persistence.architecture.adpater.command;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CommandAdapter 아키텍처 규칙 검증 테스트
 *
 * <p>CQRS Command Adapter의 Zero-Tolerance 규칙을 자동으로 검증합니다:
 *
 * <ul>
 *   <li>정확한 필드 개수 (2개): JpaRepository, Mapper
 *   <li>정확한 메서드 개수 (1개): persist()
 *   <li>메서드 네이밍 규칙: persist
 *   <li>반환 타입 규칙: *Id
 *   <li>파라미터 개수 규칙: 정확히 1개
 *   <li>@Component 필수
 *   <li>@Transactional 금지
 *   <li>비즈니스 로직 금지
 *   <li>Query 메서드 금지
 *   <li>Mapper.toEntity() 호출 필수
 *   <li>JpaRepository.save() 호출 필수
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("CommandAdapter 아키텍처 규칙 검증 (Zero-Tolerance)")
class CommandAdapterArchTest {

    private static JavaClasses commandAdapterClasses;

    @BeforeAll
    static void setUp() {
        commandAdapterClasses =
                new ClassFileImporter().importPackages("com.ryuqq.adapter.out.persistence");
    }

    /**
     * 규칙 1: @Component 어노테이션 필수
     *
     * <p>CommandAdapter는 Spring Bean으로 등록되어야 합니다.
     *
     * <ul>
     *   <li>✅ @Component
     *   <li>❌ @Service (Application Layer 전용)
     *   <li>❌ @Repository (JpaRepository 인터페이스 전용)
     * </ul>
     */
    @Test
    @DisplayName("규칙 1: @Component 어노테이션 필수")
    void commandAdapter_MustBeAnnotatedWithComponent() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .beAnnotatedWith(org.springframework.stereotype.Component.class)
                        .because("CommandAdapter는 @Component로 Spring Bean 등록이 필수입니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 2: *PersistencePort 인터페이스 구현 필수
     *
     * <p>CommandAdapter는 Application Layer의 Port 인터페이스를 구현해야 합니다.
     *
     * <ul>
     *   <li>Port 네이밍: *PersistencePort, *CommandPort
     *   <li>Port 위치: application layer의 port.out 패키지
     * </ul>
     */
    @Test
    @DisplayName("규칙 2: *PersistencePort 인터페이스 구현 필수")
    void commandAdapter_MustImplementPersistencePort() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .implement(
                                com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "interface ending with 'PersistencePort' or 'CommandPort'",
                                        javaClass ->
                                                javaClass.getAllRawInterfaces().stream()
                                                        .anyMatch(
                                                                iface ->
                                                                        iface.getSimpleName()
                                                                                        .endsWith(
                                                                                                "PersistencePort")
                                                                                || iface.getSimpleName()
                                                                                        .endsWith(
                                                                                                "CommandPort"))))
                        .because("CommandAdapter는 Application Layer의 Persistence Port를 구현해야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 3: 정확히 2개 필드 (JpaRepository, Mapper)
     *
     * <p>CommandAdapter는 정확히 2개의 필드만 가져야 합니다:
     *
     * <ul>
     *   <li>1. JpaRepository (*JpaRepository)
     *   <li>2. Mapper (*JpaEntityMapper 또는 *EntityMapper)
     * </ul>
     */
    @Test
    @DisplayName("규칙 3: 정확히 2개 필드 (JpaRepository, Mapper)")
    void commandAdapter_MustHaveExactlyTwoFields() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should(
                                com.tngtech.archunit.lang.ArchCondition.from(
                                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                                "have exactly 2 fields",
                                                javaClass -> javaClass.getAllFields().size() == 2)))
                        .because("CommandAdapter는 정확히 2개의 필드(JpaRepository, Mapper)만 가져야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 4: 모든 필드는 final 필수
     *
     * <p>불변성(Immutability) 보장을 위해 모든 필드는 final이어야 합니다.
     */
    @Test
    @DisplayName("규칙 4: 모든 필드는 final 필수")
    void commandAdapter_AllFieldsMustBeFinal() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .beFinal()
                        .because("CommandAdapter의 모든 필드는 final로 불변성을 보장해야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 5: 생성자 주입만 허용 (Field Injection 금지)
     *
     * <p>Spring 권장사항에 따라 생성자 주입만 사용해야 합니다.
     *
     * <ul>
     *   <li>✅ 생성자 주입 (final 필드)
     *   <li>❌ @Autowired 필드 주입
     *   <li>❌ Setter 주입
     * </ul>
     */
    @Test
    @DisplayName("규칙 5: @Autowired 필드 주입 금지")
    void commandAdapter_MustNotUseFieldInjection() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .notBeAnnotatedWith(
                                org.springframework.beans.factory.annotation.Autowired.class)
                        .because("CommandAdapter는 생성자 주입만 허용되며, @Autowired 필드 주입은 금지입니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 6: 정확히 1개의 public 메서드 (persist)
     *
     * <p>CommandAdapter는 persist() 메서드 하나만 public으로 노출해야 합니다.
     *
     * <ul>
     *   <li>✅ persist()
     *   <li>❌ save(), update(), delete() 등 추가 메서드 금지
     * </ul>
     */
    @Test
    @DisplayName("규칙 6: 정확히 1개의 public 메서드")
    void commandAdapter_MustHaveExactlyOnePublicMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should(
                                com.tngtech.archunit.lang.ArchCondition.from(
                                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                                "have exactly 1 public method (excluding"
                                                        + " constructor)",
                                                javaClass ->
                                                        javaClass.getMethods().stream()
                                                                        .filter(
                                                                                method ->
                                                                                        method.getModifiers()
                                                                                                .contains(
                                                                                                        com
                                                                                                                .tngtech
                                                                                                                .archunit
                                                                                                                .core
                                                                                                                .domain
                                                                                                                .JavaModifier
                                                                                                                .PUBLIC))
                                                                        .filter(
                                                                                method ->
                                                                                        !method.getName()
                                                                                                .equals(
                                                                                                        "<init>"))
                                                                        .count()
                                                                == 1)))
                        .because("CommandAdapter는 persist() 메서드 하나만 public으로 노출해야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 7: public 메서드명은 "persist"
     *
     * <p>CQRS 패턴의 일관성을 위해 메서드명은 persist로 통일합니다.
     *
     * <ul>
     *   <li>✅ persist(Aggregate aggregate)
     *   <li>❌ save(), create(), update(), upsert() 등
     * </ul>
     */
    @Test
    @DisplayName("규칙 7: public 메서드명은 'persist'")
    void commandAdapter_PublicMethodNameMustBePersist() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .arePublic()
                        .and()
                        .doNotHaveFullName(".*<init>.*")
                        .should()
                        .haveName("persist")
                        .because("CommandAdapter의 public 메서드명은 반드시 'persist'여야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 8: persist 메서드는 정확히 1개 파라미터
     *
     * <p>persist() 메서드는 Domain Aggregate 하나만 받아야 합니다.
     *
     * <ul>
     *   <li>✅ persist(Order order)
     *   <li>❌ persist(Order order, boolean flag) - 추가 파라미터 금지
     * </ul>
     */
    @Test
    @DisplayName("규칙 8: persist 메서드는 정확히 1개 파라미터")
    void commandAdapter_PersistMethodMustHaveExactlyOneParameter() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .haveName("persist")
                        .should(
                                com.tngtech.archunit.lang.ArchCondition.from(
                                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                                "have exactly 1 parameter",
                                                method ->
                                                        method.getRawParameterTypes().size() == 1)))
                        .because("persist() 메서드는 정확히 1개의 Domain Aggregate 파라미터만 가져야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 9: persist 메서드는 *Id 타입 반환
     *
     * <p>persist() 메서드는 저장된 Entity의 ID를 반환해야 합니다.
     *
     * <ul>
     *   <li>✅ OrderId, ProductId, CustomerId 등
     *   <li>❌ void, boolean, Entity 반환 금지
     * </ul>
     */
    @Test
    @DisplayName("규칙 9: persist 메서드는 *Id 타입 반환")
    void commandAdapter_PersistMethodMustReturnIdType() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .haveName("persist")
                        .should(
                                com.tngtech.archunit.lang.ArchCondition.from(
                                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                                "return type ending with 'Id'",
                                                method ->
                                                        method.getRawReturnType()
                                                                .getSimpleName()
                                                                .endsWith("Id"))))
                        .because("persist() 메서드는 *Id 타입을 반환해야 합니다 (예: OrderId, ProductId)");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 10: 비즈니스 로직 금지 (Domain 호출 금지)
     *
     * <p>CommandAdapter는 단순 저장만 수행하며, Domain 객체의 비즈니스 메서드를 호출하면 안 됩니다.
     *
     * <ul>
     *   <li>❌ order.calculateTotal() - Application Layer에서 호출
     *   <li>❌ order.validate() - Application Layer에서 호출
     *   <li>✅ 단순 변환 → 저장 → ID 반환
     * </ul>
     *
     * <p>주의: if문 감지는 ArchUnit으로 검증 불가 (코드 리뷰로 확인)
     */
    @Test
    @DisplayName("규칙 10: Domain Layer 의존성 금지")
    void commandAdapter_MustNotDependOnDomainLayer() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .accessClassesThat()
                        .resideInAnyPackage("..domain..")
                        .because("CommandAdapter는 Domain Layer에 직접 접근하면 안 됩니다 (Port를 통해서만 접근)");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 11: Query 메서드 금지 (find, load, get 등)
     *
     * <p>CQRS 원칙에 따라 Command Adapter는 조회 메서드를 포함하면 안 됩니다.
     *
     * <ul>
     *   <li>❌ findById(), loadById(), getById() 등
     *   <li>✅ 조회는 QueryAdapter로 분리
     * </ul>
     */
    @Test
    @DisplayName("규칙 11: Query 메서드 금지")
    void commandAdapter_MustNotContainQueryMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .haveNameNotMatching("(find|load|get|query|search|list|count|exists).*")
                        .because("CommandAdapter는 Query 메서드를 포함하면 안 됩니다. QueryAdapter로 분리하세요");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 12: JpaRepository 의존성 필수
     *
     * <p>CommandAdapter는 JpaRepository를 필드로 가져야 합니다.
     *
     * <ul>
     *   <li>✅ private final *JpaRepository repository
     *   <li>❌ EntityManager 직접 사용 금지
     * </ul>
     *
     * <p>주의: save() 호출 검증은 ArchUnit으로 불가 (코드 리뷰로 확인)
     */
    @Test
    @DisplayName("규칙 12: JpaRepository 의존성 필수")
    void commandAdapter_MustDependOnJpaRepository() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleNameEndingWith("JpaRepository")
                        .because("CommandAdapter는 JpaRepository를 의존성으로 가져야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 13: @Transactional 금지
     *
     * <p>Transaction은 Application Layer(UseCase)에서 관리해야 합니다.
     *
     * <ul>
     *   <li>❌ @Transactional - CommandAdapter에 사용 금지
     *   <li>✅ UseCase에서 @Transactional 사용
     * </ul>
     */
    @Test
    @DisplayName("규칙 13: @Transactional 금지")
    void commandAdapter_MustNotBeAnnotatedWithTransactional() {
        ArchRule classRule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .notBeAnnotatedWith(
                                org.springframework.transaction.annotation.Transactional.class)
                        .because("CommandAdapter 클래스에 @Transactional 사용 금지. UseCase에서 관리하세요");

        ArchRule methodRule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .notBeAnnotatedWith(
                                org.springframework.transaction.annotation.Transactional.class)
                        .because("CommandAdapter 메서드에 @Transactional 사용 금지. UseCase에서 관리하세요");

        classRule.check(commandAdapterClasses);
        methodRule.check(commandAdapterClasses);
    }

    /**
     * 규칙 14: Mapper 의존성 필수
     *
     * <p>Domain → Entity 변환은 반드시 Mapper를 통해 수행해야 합니다.
     *
     * <ul>
     *   <li>✅ private final *Mapper mapper
     *   <li>❌ new OrderJpaEntity() - 직접 생성 금지
     * </ul>
     *
     * <p>주의: toEntity() 호출 검증은 ArchUnit으로 불가 (코드 리뷰로 확인)
     */
    @Test
    @DisplayName("규칙 14: Mapper 의존성 필수")
    void commandAdapter_MustDependOnMapper() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .dependOnClassesThat()
                        .haveSimpleNameEndingWith("Mapper")
                        .because("CommandAdapter는 Mapper를 의존성으로 가져야 합니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 15: @Override 어노테이션 필수
     *
     * <p>persist() 메서드는 Port 인터페이스를 구현하므로 @Override가 필수입니다.
     */
    @Test
    @DisplayName("규칙 15: @Override 어노테이션 필수")
    void commandAdapter_PersistMethodMustHaveOverrideAnnotation() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .haveName("persist")
                        .should()
                        .beAnnotatedWith(Override.class)
                        .because("persist() 메서드는 Port 인터페이스 구현이므로 @Override가 필수입니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 16: private helper 메서드 금지
     *
     * <p>단순성 유지를 위해 추가 helper 메서드를 만들지 않습니다.
     *
     * <ul>
     *   <li>❌ private void validate() - 유효성 검사는 Domain에서
     *   <li>❌ private void enrichData() - 데이터 가공은 Application에서
     * </ul>
     */
    @Test
    @DisplayName("규칙 16: private helper 메서드 금지")
    void commandAdapter_MustNotHavePrivateHelperMethods() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .arePrivate()
                        .should()
                        .haveNameMatching("<init>")
                        .because("CommandAdapter는 private helper 메서드를 가질 수 없습니다");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 17: 예외 throw 금지 (Domain에서 처리)
     *
     * <p>비즈니스 예외는 Domain Layer에서 throw되어야 합니다.
     *
     * <ul>
     *   <li>❌ if (!order.isValid()) throw new InvalidOrderException()
     *   <li>✅ order.validate() - Domain 메서드에서 예외 throw
     * </ul>
     */
    @Test
    @DisplayName("규칙 17: 비즈니스 예외 throw 금지")
    void commandAdapter_MustNotThrowBusinessExceptions() {
        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .and()
                        .haveName("persist")
                        .should()
                        .notDeclareThrowableOfType(RuntimeException.class)
                        .because("CommandAdapter는 비즈니스 예외를 throw하지 않습니다. Domain에서 처리하세요");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 18: 로깅 금지
     *
     * <p>단순 저장 작업에 로깅은 불필요합니다. 필요 시 AOP로 처리하세요.
     *
     * <ul>
     *   <li>❌ log.info("Saving order: {}", orderId)
     *   <li>✅ AOP 기반 로깅 또는 Spring Data JPA 이벤트
     * </ul>
     */
    @Test
    @DisplayName("규칙 18: 로깅 금지")
    void commandAdapter_MustNotContainLogging() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .accessClassesThat()
                        .haveNameMatching(".*Logger.*")
                        .because("CommandAdapter는 로깅을 포함하지 않습니다. AOP로 처리하세요");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 19: Validator 의존성 금지 (Domain에서 처리)
     *
     * <p>유효성 검사는 Domain Layer에서 수행되어야 합니다.
     *
     * <ul>
     *   <li>❌ @Autowired Validator validator
     *   <li>✅ Order 생성 시 또는 Domain 메서드에서 검증
     * </ul>
     *
     * <p>주의: validate() 메서드 호출 검증은 ArchUnit으로 불가 (코드 리뷰로 확인)
     */
    @Test
    @DisplayName("규칙 19: Validator 의존성 금지")
    void commandAdapter_MustNotDependOnValidator() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .should()
                        .accessClassesThat()
                        .haveNameMatching(".*Validator.*")
                        .because("CommandAdapter는 Validator를 사용하지 않습니다. Domain에서 처리하세요");

        rule.check(commandAdapterClasses);
    }

    /**
     * 규칙 20: *CommandAdapter 네이밍 규칙
     *
     * <p>CommandAdapter는 반드시 "CommandAdapter"로 끝나야 합니다.
     *
     * <ul>
     *   <li>✅ OrderCommandAdapter, ProductCommandAdapter
     *   <li>❌ OrderAdapter, OrderPersistenceAdapter - 네이밍 불명확
     * </ul>
     */
    @Test
    @DisplayName("규칙 20: *CommandAdapter 네이밍 규칙")
    void commandAdapter_MustFollowNamingConvention() {
        ArchRule rule =
                classes()
                        .that()
                        .implement(
                                com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "interface ending with 'PersistencePort' or 'CommandPort'",
                                        javaClass ->
                                                javaClass.getAllRawInterfaces().stream()
                                                        .anyMatch(
                                                                iface ->
                                                                        iface.getSimpleName()
                                                                                        .endsWith(
                                                                                                "PersistencePort")
                                                                                || iface.getSimpleName()
                                                                                        .endsWith(
                                                                                                "CommandPort"))))
                        .and()
                        .resideInAPackage("..adapter..")
                        .should()
                        .haveSimpleNameEndingWith("CommandAdapter")
                        .because("Command Adapter는 반드시 *CommandAdapter 네이밍 규칙을 따라야 합니다");

        rule.check(commandAdapterClasses);
    }
}
