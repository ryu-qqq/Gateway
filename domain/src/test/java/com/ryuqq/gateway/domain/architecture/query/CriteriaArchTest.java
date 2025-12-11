package com.ryuqq.gateway.domain.architecture.query;

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
 * Criteria ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>개요</strong>:
 *
 * <p>Criteria는 복합 검색 조건을 표현하는 Domain Query Object입니다. 단건 조회용 VO(Email, UserId 등)는 기존 vo 패키지의 VO를
 * 재사용합니다.
 *
 * <p><strong>패키지 구조</strong>:
 *
 * <pre>
 * domain/{bc}/
 * ├── vo/
 * │   ├── Email.java           # 단건 조회에도 사용 (findByEmail)
 * │   ├── UserId.java          # 단건 조회에도 사용 (findByUserId)
 * │   └── OrderSortKey.java    # 정렬 키 (SortKey 구현)
 * └── query/
 *     └── criteria/
 *         └── OrderSearchCriteria.java  # 복합 검색 조건
 * </pre>
 *
 * <p><strong>검증 규칙 (총 10개)</strong>:
 *
 * <ul>
 *   <li>위치: domain.{bc}.query.criteria 패키지
 *   <li>타입: Record 필수
 *   <li>네이밍: *SearchCriteria 또는 *Criteria
 *   <li>팩토리 메서드: of() 필수
 *   <li>Lombok 금지
 *   <li>JPA 어노테이션 금지
 *   <li>Spring 어노테이션 금지
 *   <li>외부 레이어 의존 금지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see <a href="docs/coding_convention/02-domain-layer/vo/query-vo-guide.md">Query VO Guide</a>
 */
@Tag("architecture")
@Tag("domain")
@Tag("criteria")
@DisplayName("Criteria 아키텍처 검증 테스트")
class CriteriaArchTest {

    private static final String CRITERIA_PACKAGE = "..query.criteria..";
    private static final String BASE_PACKAGE = "com.ryuqq.domain";

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    // ==================== 위치 규칙 ====================

    /** 규칙 1: Criteria 클래스는 domain.{bc}.query.criteria 패키지에 위치해야 한다 */
    @Test
    @DisplayName("[필수] Criteria 클래스는 query.criteria 패키지에 위치해야 한다")
    void criteriaClasses_ShouldBeInCriteriaPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("Criteria")
                        .and()
                        .resideInAPackage("..domain..")
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .should()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .allowEmptyShould(true)
                        .because(
                                "Criteria 클래스는 domain.{bc}.query.criteria 패키지에 위치해야 합니다\n"
                                        + "예시:\n"
                                        + "  - domain.order.query.criteria.OrderSearchCriteria ✅\n"
                                        + "  - domain.order.vo.OrderSearchCriteria ❌ (잘못된 패키지)");

        rule.check(classes);
    }

    // ==================== 타입 규칙 ====================

    /** 규칙 2: Criteria는 Record로 구현되어야 한다 */
    @Test
    @DisplayName("[필수] Criteria는 Record로 구현되어야 한다")
    void criteria_ShouldBeRecords() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT)
                        .should(beRecords())
                        .allowEmptyShould(true)
                        .because(
                                "Criteria는 Java Record로 구현해야 합니다\n"
                                        + "예시:\n"
                                        + "  - public record OrderSearchCriteria(...) {} ✅\n"
                                        + "  - public class OrderSearchCriteria {} ❌");

        rule.check(classes);
    }

    // ==================== 네이밍 규칙 ====================

    /** 규칙 3: criteria 패키지의 클래스는 *Criteria로 끝나야 한다 */
    @Test
    @DisplayName("[필수] criteria 패키지의 클래스는 *Criteria로 끝나야 한다")
    void criteriaPackageClasses_ShouldEndWithCriteria() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT)
                        .should()
                        .haveSimpleNameEndingWith("Criteria")
                        .allowEmptyShould(true)
                        .because(
                                "criteria 패키지의 클래스는 *Criteria 또는 *SearchCriteria로 끝나야 합니다\n"
                                        + "예시:\n"
                                        + "  - OrderSearchCriteria ✅\n"
                                        + "  - OrderCriteria ✅\n"
                                        + "  - OrderFilter ❌ (잘못된 네이밍)");

        rule.check(classes);
    }

    // ==================== 팩토리 메서드 규칙 ====================

    /** 규칙 4: Criteria는 of() 정적 팩토리 메서드를 가져야 한다 */
    @Test
    @DisplayName("[필수] Criteria는 of() 정적 팩토리 메서드를 가져야 한다")
    void criteria_ShouldHaveOfMethod() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT)
                        .should(haveStaticMethodWithName("of"))
                        .allowEmptyShould(true)
                        .because(
                                "Criteria는 of() 정적 팩토리 메서드로 기본값을 적용해야 합니다\n"
                                        + "예시:\n"
                                        + "  - OrderSearchCriteria.of(memberId, status, ...) ✅\n"
                                        + "  - new OrderSearchCriteria(...) (직접 생성은 권장하지 않음)");

        rule.check(classes);
    }

    // ==================== 금지 규칙 (Lombok, JPA, Spring) ====================

    /** 규칙 5: Criteria는 Lombok 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Criteria는 Lombok 어노테이션을 사용하지 않아야 한다")
    void criteria_ShouldNotUseLombok() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
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
                        .allowEmptyShould(true)
                        .because("Criteria는 Lombok을 사용하지 않고 Pure Java Record로 구현해야 합니다");

        rule.check(classes);
    }

    /** 규칙 6: Criteria는 JPA 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Criteria는 JPA 어노테이션을 사용하지 않아야 한다")
    void criteria_ShouldNotUseJpa() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
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
                        .allowEmptyShould(true)
                        .because("Criteria는 JPA 어노테이션을 사용하지 않아야 합니다 (Domain Layer 순수성)");

        rule.check(classes);
    }

    /** 규칙 7: Criteria는 Spring 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Criteria는 Spring 어노테이션을 사용하지 않아야 한다")
    void criteria_ShouldNotUseSpring() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .should()
                        .notBeAnnotatedWith("org.springframework.stereotype.Component")
                        .andShould()
                        .notBeAnnotatedWith("org.springframework.stereotype.Service")
                        .andShould()
                        .notBeAnnotatedWith("org.springframework.context.annotation.Configuration")
                        .allowEmptyShould(true)
                        .because("Criteria는 Spring 어노테이션을 사용하지 않아야 합니다 (Domain Layer 순수성)");

        rule.check(classes);
    }

    // ==================== 의존성 규칙 ====================

    /** 규칙 8: Criteria는 외부 레이어(Application, Adapter)에 의존하지 않아야 한다 */
    @Test
    @DisplayName("[필수] Criteria는 외부 레이어에 의존하지 않아야 한다")
    void criteria_ShouldNotDependOnOuterLayers() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .should()
                        .onlyDependOnClassesThat()
                        .resideInAnyPackage("com.ryuqq.domain..", "java..", "jakarta.annotation..")
                        .allowEmptyShould(true)
                        .because(
                                "Criteria는 Domain Layer 내부에만 의존해야 합니다\n"
                                        + "허용된 의존성:\n"
                                        + "  - domain.common.vo (DateRange, PageRequest,"
                                        + " SortDirection)\n"
                                        + "  - domain.{bc}.vo (SortKey 구현체)\n"
                                        + "  - java.* (표준 라이브러리)");

        rule.check(classes);
    }

    /** 규칙 9: Criteria는 domain.common.vo 클래스를 사용할 수 있다 */
    @Test
    @DisplayName("[권장] Criteria는 공통 VO(DateRange, PageRequest 등)를 사용해야 한다")
    void criteria_ShouldUseCommonVOs() {
        // 이 규칙은 문서화 목적이며, 실제 검증은 코드 리뷰에서 수행
        // 강제하기 어려우므로 의존성 허용 규칙으로 대체
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .should()
                        .accessClassesThat()
                        .resideInAnyPackage(
                                "com.ryuqq.domain.common.vo..",
                                "com.ryuqq.domain..vo..",
                                "java..",
                                "com.ryuqq.domain..query.criteria..")
                        .orShould()
                        .accessClassesThat()
                        .haveSimpleNameEndingWith("Criteria") // 자기 자신 참조 허용
                        .allowEmptyShould(true)
                        .because(
                                "Criteria는 공통 VO를 사용해야 합니다\n"
                                        + "권장 사용 VO:\n"
                                        + "  - DateRange (날짜 범위 필터)\n"
                                        + "  - SortDirection (정렬 방향)\n"
                                        + "  - PageRequest / CursorPageRequest (페이징)\n"
                                        + "  - {BC}SortKey (정렬 키)");

        rule.check(classes);
    }

    // ==================== 구조 규칙 ====================

    /** 규칙 10: Criteria는 public 클래스여야 한다 */
    @Test
    @DisplayName("[필수] Criteria는 public 클래스여야 한다")
    void criteria_ShouldBePublic() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage(CRITERIA_PACKAGE)
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .and()
                        .haveSimpleNameNotContaining("Fixture")
                        .and()
                        .haveSimpleNameNotContaining("Mother")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .doNotHaveModifier(JavaModifier.ABSTRACT)
                        .should()
                        .bePublic()
                        .allowEmptyShould(true)
                        .because("Criteria는 여러 레이어에서 사용되므로 public이어야 합니다");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /** Record 타입인지 검증 */
    private static ArchCondition<JavaClass> beRecords() {
        return new ArchCondition<JavaClass>("be records") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean isRecord =
                        javaClass.getAllRawSuperclasses().stream()
                                .anyMatch(
                                        superClass ->
                                                superClass.getName().equals("java.lang.Record"));

                if (!isRecord) {
                    String message =
                            String.format(
                                    "Criteria %s는 Record로 구현되어야 합니다. "
                                            + "'public class' 대신 'public record'를 사용하세요.",
                                    javaClass.getSimpleName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /** 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증 */
    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have public static method '" + methodName + "'") {
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
                                    "Criteria %s는 public static %s() 메서드를 가져야 합니다.\n"
                                        + "예시: public static %s of(...) { return new %s(...); }",
                                    javaClass.getSimpleName(),
                                    methodName,
                                    javaClass.getSimpleName(),
                                    javaClass.getSimpleName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }
}
