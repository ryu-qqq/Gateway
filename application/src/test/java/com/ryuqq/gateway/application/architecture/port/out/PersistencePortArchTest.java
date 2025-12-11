package com.ryuqq.gateway.application.architecture.port.out;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PersistencePort ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 PersistencePort는 정확히 이 규칙을 따라야 합니다:
 *
 * <ul>
 *   <li>인터페이스명: *PersistencePort
 *   <li>패키지: ..application..port.out.command..
 *   <li>허용 메서드:
 *       <ul>
 *         <li>persist(T) → TId (단건 저장, 필수)
 *         <li>persistAll(List&lt;T&gt;) → List&lt;TId&gt; (다건 저장, 선택)
 *       </ul>
 *   <li>금지 메서드: save, update, delete, remove
 *   <li>반환 타입: {Bc}Id 또는 List&lt;{Bc}Id&gt; (Value Object)
 *   <li>파라미터: {Bc} 또는 List&lt;{Bc}&gt; (Domain Aggregate)
 *   <li>의도: JPA merge 활용 (PK 있으면 update, 없으면 insert)
 *   <li>삭제: Soft Delete(상태 변경) 권장, Hard Delete 필요 시 별도 예외 처리
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PersistencePort ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class PersistencePortArchTest {

    private static JavaClasses classes;
    private static boolean hasPersistencePortClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.application");

        hasPersistencePortClasses =
                classes.stream()
                        .anyMatch(
                                javaClass -> javaClass.getSimpleName().endsWith("PersistencePort"));
    }

    /** 규칙 1: 인터페이스명 규칙 */
    @Test
    @DisplayName("[필수] PersistencePort는 '*PersistencePort' 접미사를 가져야 한다")
    void persistencePort_MustHaveCorrectSuffix() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..port.out.command..")
                        .and()
                        .areInterfaces()
                        .should()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .because("Command Port는 'PersistencePort' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /** 규칙 2: 패키지 위치 */
    @Test
    @DisplayName("[필수] PersistencePort는 ..application..port.out.command.. 패키지에 위치해야 한다")
    void persistencePort_MustBeInCorrectPackage() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .resideInAPackage("..application..port.out.command..")
                        .because("PersistencePort는 application.*.port.out.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /** 규칙 3: Interface 여야 함 */
    @Test
    @DisplayName("[필수] PersistencePort는 Interface여야 한다")
    void persistencePort_MustBeInterface() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .beInterfaces()
                        .because("PersistencePort는 Interface로 선언되어야 합니다 (구현체는 Adapter)");

        rule.check(classes);
    }

    /** 규칙 4: Public Interface */
    @Test
    @DisplayName("[필수] PersistencePort는 public이어야 한다")
    void persistencePort_MustBePublic() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .bePublic()
                        .because("PersistencePort는 외부에서 접근 가능해야 합니다");

        rule.check(classes);
    }

    /** 규칙 5: persist() 메서드 존재 (필수) */
    @Test
    @DisplayName("[필수] PersistencePort는 persist() 메서드를 가져야 한다")
    void persistencePort_MustHavePersistMethod() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .and()
                        .haveNameMatching("persist")
                        .should()
                        .beDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .because("PersistencePort는 persist() 메서드를 무조건 제공해야 합니다 (단건 저장)");

        rule.check(classes);
    }

    /** 규칙 6: persistAll() 메서드 허용 (선택) */
    @Test
    @DisplayName("[허용] PersistencePort는 persistAll() 메서드를 가질 수 있다")
    void persistencePort_CanHavePersistAllMethod() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                methods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .and()
                        .haveNameMatching("persistAll")
                        .should()
                        .haveRawParameterTypes(List.class)
                        .andShould()
                        .haveRawReturnType(List.class)
                        .because("PersistencePort는 배치 저장을 위해 persistAll()을 제공할 수 있습니다");

        rule.check(classes);
    }

    /** 규칙 7: save/update/delete/remove 메서드 금지 */
    @Test
    @DisplayName("[금지] PersistencePort는 save/update/delete/remove 메서드를 가지지 않아야 한다")
    void persistencePort_MustNotHaveSaveUpdateDeleteMethods() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .haveNameMatching("save|update|delete|remove")
                        .because(
                                "PersistencePort는 persist()/persistAll()만 허용됩니다. "
                                        + "삭제가 필요한 경우 Soft Delete(상태 변경)를 사용하세요.");

        rule.check(classes);
    }

    /** 규칙 8: 조회 메서드 금지 */
    @Test
    @DisplayName("[금지] PersistencePort는 조회 메서드를 가지지 않아야 한다")
    void persistencePort_MustNotHaveFindMethods() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .haveNameMatching("find.*|get.*|load.*|exists.*|count.*")
                        .because("조회 메서드는 QueryPort에서 처리해야 합니다 (CQRS 분리)");

        rule.check(classes);
    }

    /** 규칙 9: Domain Layer 의존성만 허용 */
    @Test
    @DisplayName("[필수] PersistencePort는 Domain Layer만 의존해야 한다")
    void persistencePort_MustOnlyDependOnDomainLayer() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .onlyAccessClassesThat()
                        .resideInAnyPackage(
                                "com.ryuqq.domain..",
                                "java..",
                                "com.ryuqq.application.." // 같은 application 내 DTO는 허용
                                )
                        .because("PersistencePort는 Domain Layer만 의존해야 합니다 (Infrastructure 의존 금지)");

        rule.check(classes);
    }

    /** 규칙 10: 원시 타입 반환 금지 */
    @Test
    @DisplayName("[금지] PersistencePort는 원시 타입을 반환하지 않아야 한다")
    void persistencePort_MustNotReturnPrimitiveTypes() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .and()
                        .haveNameMatching("persist.*") // persist, persistAll 모두 포함
                        .should()
                        .haveRawReturnType(Long.class)
                        .orShould()
                        .haveRawReturnType(String.class)
                        .orShould()
                        .haveRawReturnType(Integer.class)
                        .because(
                                "PersistencePort는 Value Object 또는 List<Value Object>를 반환해야 합니다 (타입"
                                        + " 안전성)");

        rule.check(classes);
    }

    /** 규칙 11: DTO/Entity 파라미터 금지 */
    @Test
    @DisplayName("[금지] PersistencePort는 DTO/Entity를 파라미터로 받지 않아야 한다")
    void persistencePort_MustNotAcceptDtoOrEntity() {
        assumeTrue(hasPersistencePortClasses, "PersistencePort 클래스가 없어 테스트를 스킵합니다");

        ArchRule rule =
                noMethods()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveSimpleNameEndingWith("PersistencePort")
                        .should()
                        .haveRawParameterTypes(".*Dto.*")
                        .orShould()
                        .haveRawParameterTypes(".*JpaEntity.*")
                        .orShould()
                        .haveRawParameterTypes(".*Entity")
                        .because(
                                "PersistencePort는 Domain Aggregate 또는 List<Domain Aggregate>를 파라미터로"
                                        + " 받아야 합니다");

        rule.check(classes);
    }
}
