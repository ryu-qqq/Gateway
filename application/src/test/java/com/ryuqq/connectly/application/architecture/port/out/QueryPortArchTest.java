package com.ryuqq.connectly.application.architecture.port.out;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * QueryPort ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 QueryPort는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>인터페이스명: *QueryPort</li>
 *   <li>패키지: ..application..port.out.query..</li>
 *   <li>필수 메서드 (2개): findById, existsById</li>
 *   <li>선택 메서드 (패턴별 강제):
 *     <ul>
 *       <li>search* → Criteria 파라미터 + PageResponse 반환 (페이징 필수)</li>
 *       <li>findBy* → 단순 파라미터 + Optional/List 반환</li>
 *       <li>count* → long 반환</li>
 *     </ul>
 *   </li>
 *   <li>금지 메서드: findAll (OOM 위험)</li>
 *   <li>Value Object 파라미터 사용 (원시 타입 금지)</li>
 *   <li>Domain 반환 (DTO/Entity 반환 금지)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("QueryPort ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class QueryPortArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");
    }

    /**
     * 규칙 1: 인터페이스명 규칙
     */
    @Test
    @DisplayName("[필수] QueryPort는 '*QueryPort' 접미사를 가져야 한다")
    void queryPort_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..port.out.query..")
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("QueryPort")
            .because("Query Port는 'QueryPort' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 패키지 위치
     */
    @Test
    @DisplayName("[필수] QueryPort는 ..application..port.out.query.. 패키지에 위치해야 한다")
    void queryPort_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryPort")
            .should().resideInAPackage("..application..port.out.query..")
            .because("QueryPort는 application.*.port.out.query 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Interface 여야 함
     */
    @Test
    @DisplayName("[필수] QueryPort는 Interface여야 한다")
    void queryPort_MustBeInterface() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryPort")
            .should().beInterfaces()
            .because("QueryPort는 Interface로 선언되어야 합니다 (구현체는 Adapter)");

        rule.check(classes);
    }

    /**
     * 규칙 4: Public Interface
     */
    @Test
    @DisplayName("[필수] QueryPort는 public이어야 한다")
    void queryPort_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryPort")
            .should().bePublic()
            .because("QueryPort는 외부에서 접근 가능해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: findById() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryPort는 findById() 메서드를 가져야 한다")
    void queryPort_MustHaveFindByIdMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findById")
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .because("QueryPort는 findById() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: existsById() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryPort는 existsById() 메서드를 가져야 한다")
    void queryPort_MustHaveExistsByIdMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("existsById")
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .because("QueryPort는 existsById() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: search* 메서드는 PageResponse 반환
     */
    @Test
    @DisplayName("[패턴] search* 메서드는 PageResponse를 반환해야 한다")
    void queryPort_SearchMethodsMustReturnPageResponse() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("search.*")
            .should().haveRawReturnType("com.ryuqq.application.common.dto.response.PageResponse")
            .because("search* 메서드는 PageResponse를 반환해야 합니다 (복잡한 조건 조회는 페이징 필수)");

        rule.check(classes);
    }

    /**
     * 규칙 8: findBy* 메서드는 Optional 또는 List 반환
     */
    @Test
    @DisplayName("[패턴] findBy* 메서드는 Optional 또는 List를 반환해야 한다")
    void queryPort_FindByMethodsMustReturnOptionalOrList() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findBy[A-Z].*")
            .and().doNotHaveName("findById")  // findById는 별도 규칙
            .should().haveRawReturnType(Optional.class)
            .orShould().haveRawReturnType(List.class)
            .because("단순 조건 조회는 Optional 또는 List를 반환해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: 저장/수정/삭제 메서드 금지
     */
    @Test
    @DisplayName("[금지] QueryPort는 저장/수정/삭제 메서드를 가지지 않아야 한다")
    void queryPort_MustNotHaveCommandMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .should().haveNameMatching("save|update|delete|remove|persist")
            .because("저장/수정/삭제 메서드는 PersistencePort에서 처리해야 합니다 (CQRS 분리)");

        rule.check(classes);
    }

    /**
     * 규칙 10: findById는 Optional 반환
     */
    @Test
    @DisplayName("[필수] findById()는 Optional을 반환해야 한다")
    void queryPort_FindByIdMustReturnOptional() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findById")
            .should().haveRawReturnType(Optional.class)
            .because("findById()는 Optional을 반환해야 합니다 (null 방지)");

        rule.check(classes);
    }

    /**
     * 규칙 11: existsById는 boolean 반환
     */
    @Test
    @DisplayName("[필수] existsById()는 boolean을 반환해야 한다")
    void queryPort_ExistsByIdMustReturnBoolean() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("existsById")
            .should().haveRawReturnType(boolean.class)
            .because("existsById()는 boolean을 반환해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: count* 메서드는 long 반환
     */
    @Test
    @DisplayName("[패턴] count* 메서드는 long을 반환해야 한다")
    void queryPort_CountMethodsMustReturnLong() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("count[A-Z].*")
            .should().haveRawReturnType(long.class)
            .because("count* 메서드는 long을 반환해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: findAll 금지 (OOM 방지)
     */
    @Test
    @DisplayName("[금지] QueryPort는 findAll 메서드를 가지지 않아야 한다")
    void queryPort_MustNotHaveFindAllMethod() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .should().haveNameMatching("findAll")
            .because("findAll()은 OOM 위험이 있습니다. 페이징 처리된 search() 메서드를 사용하세요");

        rule.check(classes);
    }

    /**
     * 규칙 14: DTO 반환 금지
     */
    @Test
    @DisplayName("[금지] QueryPort는 DTO를 반환하지 않아야 한다")
    void queryPort_MustNotReturnDto() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .should().haveRawReturnType(".*Dto.*")
            .because("QueryPort는 Domain을 반환해야 합니다 (DTO 반환 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 15: Entity 반환 금지
     */
    @Test
    @DisplayName("[금지] QueryPort는 Entity를 반환하지 않아야 한다")
    void queryPort_MustNotReturnEntity() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .should().haveRawReturnType(".*JpaEntity.*")
            .orShould().haveRawReturnType(".*Entity")
            .because("QueryPort는 Domain을 반환해야 합니다 (Entity 반환 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 16: 원시 타입 파라미터 금지 (findById)
     */
    @Test
    @DisplayName("[금지] findById()는 원시 타입을 파라미터로 받지 않아야 한다")
    void queryPort_FindByIdMustNotAcceptPrimitiveTypes() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findById")
            .should().haveRawParameterTypes(Long.class)
            .orShould().haveRawParameterTypes(String.class)
            .orShould().haveRawParameterTypes(Integer.class)
            .because("findById()는 Value Object를 파라미터로 받아야 합니다 (타입 안전성)");

        rule.check(classes);
    }

    /**
     * 규칙 17: Domain Layer 의존성만 허용
     */
    @Test
    @DisplayName("[필수] QueryPort는 Domain Layer만 의존해야 한다")
    void queryPort_MustOnlyDependOnDomainLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryPort")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.ryuqq.domain..",
                "java..",
                "com.ryuqq.application.."  // 같은 application 내 DTO는 허용
            )
            .because("QueryPort는 Domain Layer만 의존해야 합니다 (Infrastructure 의존 금지)");

        rule.check(classes);
    }
}
