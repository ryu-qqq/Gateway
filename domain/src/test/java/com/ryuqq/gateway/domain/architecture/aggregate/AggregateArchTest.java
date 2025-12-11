package com.ryuqq.gateway.domain.architecture.aggregate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Aggregate Root ArchUnit 아키텍처 검증 테스트
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
 * <p><strong>검증 규칙 (총 22개)</strong>:
 *
 * <p><em>금지 규칙 (6개)</em>:
 *
 * <ul>
 *   <li>Lombok 어노테이션 금지
 *   <li>JPA 어노테이션 금지
 *   <li>Spring 어노테이션 금지
 *   <li>Setter 메서드 금지
 *   <li>외래키 원시 타입 금지 (VO 필수)
 *   <li>final 클래스 금지
 * </ul>
 *
 * <p><em>필수 규칙 (12개)</em>:
 *
 * <ul>
 *   <li>생성자 private 필수
 *   <li>forNew() 정적 팩토리 메서드 필수
 *   <li>of() 정적 팩토리 메서드 필수
 *   <li>reconstitute() 정적 팩토리 메서드 필수
 *   <li>ID 필드 final 필수
 *   <li>Clock 필드 의존 필수
 *   <li>패키지 위치 규칙
 *   <li>public 클래스 필수
 *   <li>createdAt 필드 (Instant, final)
 *   <li>updatedAt 필드 (Instant, non-final)
 *   <li>외부 레이어 의존 금지
 *   <li>비즈니스 메서드 명명 규칙
 * </ul>
 *
 * <p><em>TestFixture 규칙 (4개)</em>:
 *
 * <ul>
 *   <li>forNew() 메서드 필수
 *   <li>of() 메서드 필수
 *   <li>reconstitute() 메서드 필수
 *   <li>create*() 메서드 금지
 * </ul>
 *
 * <p><strong>검증 대상 클래스가 없는 경우</strong>:
 *
 * <p>{@code ..aggregate..} 패키지에 클래스가 없으면 해당 규칙은 자동으로 통과합니다. 이는 {@code allowEmptyShould(true)} 설정으로
 * 구현되어 있습니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("aggregate")
@DisplayName("Aggregate Root 아키텍처 검증 테스트")
class AggregateArchTest {

    private static final String AGGREGATE_PACKAGE = "..domain..aggregate..";
    private static final String FIXTURE_PACKAGE = "..fixture..";
    private static final String BASE_PACKAGE = "com.ryuqq.domain"; // ⚠️ 프로젝트에 맞게 수정

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    // ==================== 금지 규칙 (1-4) ====================

    /** 규칙 1: Aggregate Root는 Lombok 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 Lombok 어노테이션을 사용하지 않아야 한다")
    void aggregateRoot_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .beAnnotatedWith("lombok.Data")
                        .orShould()
                        .beAnnotatedWith("lombok.Builder")
                        .orShould()
                        .beAnnotatedWith("lombok.Getter")
                        .orShould()
                        .beAnnotatedWith("lombok.Setter")
                        .orShould()
                        .beAnnotatedWith("lombok.AllArgsConstructor")
                        .orShould()
                        .beAnnotatedWith("lombok.NoArgsConstructor")
                        .orShould()
                        .beAnnotatedWith("lombok.RequiredArgsConstructor")
                        .orShould()
                        .beAnnotatedWith("lombok.Value")
                        .allowEmptyShould(true)
                        .because("Aggregate Root는 Pure Java 원칙에 따라 Lombok 사용 금지");

        rule.check(classes);
    }

    /** 규칙 2: Aggregate Root는 JPA 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 JPA 어노테이션을 사용하지 않아야 한다")
    void aggregateRoot_MustNotUseJPA() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .beAnnotatedWith("jakarta.persistence.Entity")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Table")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Column")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Id")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.ManyToOne")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.OneToMany")
                        .orShould()
                        .beAnnotatedWith("javax.persistence.Entity")
                        .orShould()
                        .beAnnotatedWith("javax.persistence.Table")
                        .allowEmptyShould(true)
                        .because("Domain Layer는 JPA에 독립적이어야 합니다");

        rule.check(classes);
    }

    /** 규칙 3: Aggregate Root는 Spring 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 Spring 어노테이션을 사용하지 않아야 한다")
    void aggregateRoot_MustNotUseSpring() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .beAnnotatedWith("org.springframework.stereotype.Component")
                        .orShould()
                        .beAnnotatedWith("org.springframework.stereotype.Service")
                        .orShould()
                        .beAnnotatedWith("org.springframework.stereotype.Repository")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.annotation.Configuration")
                        .allowEmptyShould(true)
                        .because("Domain Layer는 Spring에 독립적이어야 합니다");

        rule.check(classes);
    }

    /** 규칙 4: Aggregate Root는 Setter 메서드를 가지지 않아야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 Setter 메서드를 가지지 않아야 한다")
    void aggregateRoot_MustNotHaveSetterMethods() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(notHaveSetterMethods())
                        .allowEmptyShould(true)
                        .because("Aggregate는 비즈니스 메서드로 상태 변경 (Setter 금지)");

        rule.check(classes);
    }

    private static ArchCondition<JavaClass> notHaveSetterMethods() {
        return new ArchCondition<JavaClass>("not have setter methods") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(method -> method.getName().matches("set[A-Z].*"))
                        .forEach(
                                method -> {
                                    String message =
                                            String.format(
                                                    "클래스 %s가 setter 메서드 %s()를 가지고 있습니다 (Setter 금지)",
                                                    javaClass.getSimpleName(), method.getName());
                                    events.add(SimpleConditionEvent.violated(javaClass, message));
                                });
            }
        };
    }

    // ==================== 필수 규칙 (5-10) ====================

    /** 규칙 5: Aggregate Root의 생성자는 private이어야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root의 생성자는 private이어야 한다")
    void aggregateRoot_ConstructorMustBePrivate() {
        ArchRule rule =
                constructors()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotAnonymousClasses()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotMemberClasses()
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .bePrivate()
                        .allowEmptyShould(true)
                        .because("정적 팩토리 메서드(forNew, of, reconstitute)로만 생성해야 합니다");

        rule.check(classes);
    }

    /** 규칙 6: Aggregate Root는 forNew() 정적 팩토리 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root는 forNew() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveForNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(haveStaticMethodWithName("forNew"))
                        .allowEmptyShould(true)
                        .because("신규 생성용 팩토리 메서드 forNew() 필수");

        rule.check(classes);
    }

    /** 규칙 7: Aggregate Root는 of() 정적 팩토리 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root는 of() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(haveStaticMethodWithName("of"))
                        .allowEmptyShould(true)
                        .because("ID 기반 생성용 팩토리 메서드 of() 필수");

        rule.check(classes);
    }

    /** 규칙 8: Aggregate Root는 reconstitute() 정적 팩토리 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root는 reconstitute() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveReconstituteMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(haveStaticMethodWithName("reconstitute"))
                        .allowEmptyShould(true)
                        .because("영속성 복원용 팩토리 메서드 reconstitute() 필수");

        rule.check(classes);
    }

    /** 규칙 9: Aggregate Root의 ID 필드는 final이어야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root의 ID 필드는 final이어야 한다")
    void aggregateRoot_IdFieldMustBeFinal() {
        ArchRule rule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveNameMatching("id")
                        .should()
                        .beFinal()
                        .allowEmptyShould(true)
                        .because("Aggregate ID는 불변이어야 합니다");

        rule.check(classes);
    }

    /** 규칙 10: Aggregate Root는 Instant 타입 필드를 가져야 한다 (시간 처리용) */
    @Test
    @DisplayName("[필수] Aggregate Root는 Instant 타입 필드를 가져야 한다")
    void aggregateRoot_MustHaveInstantField() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(haveFieldOfType(Instant.class))
                        .allowEmptyShould(true)
                        .because("시간 처리를 위해 Instant 필드 필수 (Clock은 파라미터로 주입받아 Instant로 변환)");

        rule.check(classes);
    }

    // ==================== 타입 규칙 (11-14) ====================

    /** 규칙 11: 외래키는 VO 타입이어야 한다 (원시 타입 금지) */
    @Test
    @DisplayName("[금지] 외래키는 VO 타입이어야 한다 (Long, String, Integer 금지)")
    void aggregateRoot_ForeignKeyMustBeValueObject() {
        ArchRule rule =
                noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .haveNameMatching(".*[Ii]d")
                        .and()
                        .doNotHaveName("id")
                        .should()
                        .haveRawType(Long.class)
                        .orShould()
                        .haveRawType(String.class)
                        .orShould()
                        .haveRawType(Integer.class)
                        .orShould()
                        .haveRawType(long.class)
                        .orShould()
                        .haveRawType(int.class)
                        .allowEmptyShould(true)
                        .because("외래키는 VO 사용 (Long paymentId ❌ → PaymentId paymentId ✅)");

        rule.check(classes);
    }

    /** 규칙 12: Aggregate Root는 올바른 패키지에 위치해야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root는 domain.[bc].aggregate 패키지에 위치해야 한다")
    void aggregateRoot_MustBeInCorrectPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .resideInAPackage("..domain..aggregate..")
                        .allowEmptyShould(true)
                        .because("Aggregate는 aggregate 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /** 규칙 13: Aggregate Root는 public 클래스여야 한다 */
    @Test
    @DisplayName("[필수] Aggregate Root는 public 클래스여야 한다")
    void aggregateRoot_MustBePublic() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .bePublic()
                        .allowEmptyShould(true)
                        .because("다른 레이어에서 사용하기 위해 public 필수");

        rule.check(classes);
    }

    /** 규칙 14: Aggregate Root는 final 클래스가 아니어야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 final 클래스가 아니어야 한다")
    void aggregateRoot_ShouldNotBeFinal() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areNotInterfaces()
                        .and()
                        .areNotEnums()
                        .and()
                        .areNotAnonymousClasses()
                        .and()
                        .areNotMemberClasses()
                        .and()
                        .haveSimpleNameNotEndingWith("Id")
                        .and()
                        .haveSimpleNameNotEndingWith("Event")
                        .and()
                        .haveSimpleNameNotEndingWith("Exception")
                        .and()
                        .haveSimpleNameNotEndingWith("Status")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should(notBeFinal())
                        .allowEmptyShould(true)
                        .because("확장 가능성을 위해 final 금지");

        rule.check(classes);
    }

    private static ArchCondition<JavaClass> notBeFinal() {
        return new ArchCondition<JavaClass>("not be final") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (javaClass.getModifiers().contains(JavaModifier.FINAL)) {
                    String message =
                            String.format(
                                    "클래스 %s가 final로 선언되어 있습니다 (확장 가능성을 위해 final 금지)",
                                    javaClass.getSimpleName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** 규칙 15: 비즈니스 메서드는 명확한 동사로 시작해야 한다 (권장 - 경고만 출력) */
    @Test
    @DisplayName("[권장] 비즈니스 메서드는 명확한 동사로 시작해야 한다")
    void aggregateRoot_BusinessMethodsShouldHaveExplicitVerbs() {
        // 권장 패턴 - 실패해도 테스트 통과, 경고만 출력
        List<String> violations = new java.util.ArrayList<>();

        JavaClasses targetClasses =
                classes.that(
                        new DescribedPredicate<JavaClass>("aggregate root classes") {
                            @Override
                            public boolean test(JavaClass javaClass) {
                                return javaClass.getPackageName().contains(".aggregate")
                                        && !javaClass.isInterface()
                                        && !javaClass.isEnum()
                                        && !javaClass.isAnonymousClass()
                                        && !javaClass.isLocalClass()
                                        && !javaClass.getSimpleName().endsWith("Id")
                                        && !javaClass.getSimpleName().endsWith("Event")
                                        && !javaClass.getSimpleName().endsWith("Exception")
                                        && !javaClass.getSimpleName().endsWith("Status")
                                        && !javaClass.getSimpleName().contains("Fixture")
                                        && !javaClass.getSimpleName().contains("Mother")
                                        && !javaClass.getSimpleName().contains("Test");
                            }
                        });

        // 확장된 비즈니스 메서드 패턴
        String businessMethodPattern =
                "(add|remove|confirm|cancel|approve|reject|ship|deliver|complete|fail|"
                        + "update|change|place|validate|calculate|transfer|process|register|"
                        + "archive|publish|rename|reorder|revert|deduct|restore|activate|"
                        + "deactivate|suspend|resume|expire|extend|assign|unassign|"
                        + "create|delete|modify|submit|withdraw|accept|decline|"
                        + "close|open|start|stop|pause|reset|increment|decrement|"
                        + "apply|revoke|grant|deny|execute|handle|notify|send|"
                        + "generate|import|export|merge|split|link|unlink|attach|detach).*";

        for (JavaClass javaClass : targetClasses) {
            javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> !method.getModifiers().contains(JavaModifier.STATIC))
                    .filter(method -> !method.getName().matches(".*<init>.*"))
                    // Getter 스타일 (record 스타일) 제외
                    .filter(
                            method ->
                                    !method.getName()
                                            .matches(
                                                    "(id|status|createdAt|updatedAt|pullDomainEvents).*"))
                    // 판단 메서드 제외
                    .filter(method -> !method.getName().matches("(is|has|can|should|get).*"))
                    // 표준 Object 메서드 제외
                    .filter(
                            method ->
                                    !method.getName()
                                            .matches("(equals|hashCode|toString|getClass).*"))
                    .filter(method -> !method.getName().matches(businessMethodPattern))
                    .forEach(
                            method ->
                                    violations.add(
                                            String.format(
                                                    "[경고] %s.%s() - 비즈니스 메서드는 명확한 동사로 시작 권장",
                                                    javaClass.getSimpleName(), method.getName())));
        }

        // 위반 사항 경고 출력 (테스트는 통과)
        if (!violations.isEmpty()) {
            System.out.println("\n=== 비즈니스 메서드 네이밍 권장 위반 ===");
            violations.forEach(System.out::println);
            System.out.println(
                    "=== 권장 패턴: add*, remove*, confirm*, cancel*, approve*, reject*, "
                            + "update*, change*, process*, register*, archive*, publish*, "
                            + "activate*, deactivate*, create*, delete*, submit*, accept* 등 ===\n");
        }
        // 테스트는 항상 통과 (권장 사항이므로)
    }

    /** 규칙 16: Aggregate Root는 Application/Adapter 레이어에 의존하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Aggregate Root는 Application/Adapter 레이어에 의존하지 않아야 한다")
    void aggregateRoot_MustNotDependOnOuterLayers() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..application..", "..adapter..")
                        .allowEmptyShould(true)
                        .because("헥사고날 아키텍처: Domain은 외부 레이어에 의존 금지");

        rule.check(classes);
    }

    // ==================== 시간 필드 규칙 (17-18) ====================

    /** 규칙 17: createdAt 필드는 Instant 타입이고 final이어야 한다 */
    @Test
    @DisplayName("[필수] createdAt 필드는 Instant 타입이고 final이어야 한다")
    void aggregateRoot_CreatedAtMustBeInstantAndFinal() {
        ArchRule typeRule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .haveNameMatching("createdAt")
                        .should()
                        .haveRawType(Instant.class)
                        .allowEmptyShould(true)
                        .because("시간 필드는 Instant 사용 (LocalDateTime 금지)");

        ArchRule finalRule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .haveNameMatching("createdAt")
                        .should()
                        .beFinal()
                        .allowEmptyShould(true)
                        .because("createdAt은 불변이어야 합니다");

        typeRule.check(classes);
        finalRule.check(classes);
    }

    /** 규칙 18: updatedAt 필드는 Instant 타입이고 non-final이어야 한다 */
    @Test
    @DisplayName("[필수] updatedAt 필드는 Instant 타입이고 non-final이어야 한다")
    void aggregateRoot_UpdatedAtMustBeInstantAndNotFinal() {
        ArchRule typeRule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .haveNameMatching("updatedAt")
                        .should()
                        .haveRawType(Instant.class)
                        .allowEmptyShould(true)
                        .because("시간 필드는 Instant 사용 (LocalDateTime 금지)");

        ArchRule notFinalRule =
                fields().that()
                        .areDeclaredInClassesThat()
                        .resideInAPackage(AGGREGATE_PACKAGE)
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotInterfaces()
                        .and()
                        .areDeclaredInClassesThat()
                        .areNotEnums()
                        .and()
                        .haveNameMatching("updatedAt")
                        .should()
                        .notBeFinal()
                        .allowEmptyShould(true)
                        .because("updatedAt은 상태 변경 시 갱신되어야 합니다");

        typeRule.check(classes);
        notFinalRule.check(classes);
    }

    // ==================== TestFixture 규칙 (19-22) ====================

    /** 규칙 19: TestFixture는 forNew() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] TestFixture는 forNew() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveForNewMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Fixture")
                        .and()
                        .resideInAPackage(FIXTURE_PACKAGE)
                        .should(haveStaticMethodWithName("forNew"))
                        .allowEmptyShould(true)
                        .because("Fixture는 Aggregate와 동일한 패턴(forNew, of, reconstitute) 사용");

        rule.check(classes);
    }

    /** 규칙 20: TestFixture는 of() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] TestFixture는 of() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Fixture")
                        .and()
                        .resideInAPackage(FIXTURE_PACKAGE)
                        .should(haveStaticMethodWithName("of"))
                        .allowEmptyShould(true)
                        .because("Fixture는 Aggregate와 동일한 패턴 사용");

        rule.check(classes);
    }

    /** 규칙 21: TestFixture는 reconstitute() 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] TestFixture는 reconstitute() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveReconstituteMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Fixture")
                        .and()
                        .resideInAPackage(FIXTURE_PACKAGE)
                        .should(haveStaticMethodWithName("reconstitute"))
                        .allowEmptyShould(true)
                        .because("Fixture는 Aggregate와 동일한 패턴 사용");

        rule.check(classes);
    }

    /** 규칙 22: TestFixture는 create*() 메서드를 가지지 않아야 한다 */
    @Test
    @DisplayName("[금지] TestFixture는 create*() 메서드를 가지지 않아야 한다")
    void fixtureClassesShouldNotHaveCreateMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Fixture")
                        .and()
                        .resideInAPackage(FIXTURE_PACKAGE)
                        .should(notHaveMethodsWithNameStartingWith("create"))
                        .allowEmptyShould(true)
                        .because("create*() 대신 forNew(), of(), reconstitute() 사용");

        rule.check(classes);
    }

    /** 클래스가 특정 타입의 필드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveFieldOfType(Class<?> fieldType) {
        return new ArchCondition<JavaClass>("have field of type: " + fieldType.getSimpleName()) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasField =
                        javaClass.getAllFields().stream()
                                .anyMatch(field -> field.getRawType().isEquivalentTo(fieldType));

                if (!hasField) {
                    String message =
                            String.format(
                                    "클래스 %s가 %s 타입 필드를 가지고 있지 않습니다",
                                    javaClass.getSimpleName(), fieldType.getSimpleName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /** 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have public static method: " + methodName) {
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
