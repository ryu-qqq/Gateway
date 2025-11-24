# QueryPort ArchUnit 검증 규칙

> **목적**: QueryPort의 구조와 규칙을 ArchUnit으로 자동 검증 (Zero-Tolerance)

---

## 1️⃣ 검증 항목 (완전 강제)

### 필수 검증 규칙

1. ✅ **인터페이스명**: `*QueryPort`
2. ✅ **패키지 위치**: `..application..port.out.query..`
3. ✅ **4개 표준 메서드 필수**:
   - `findById({Bc}Id id)`
   - `existsById({Bc}Id id)`
   - `findByCriteria({Bc}SearchCriteria criteria)`
   - `countByCriteria({Bc}SearchCriteria criteria)`
4. ✅ **Value Object 파라미터**: `{Bc}Id`, `{Bc}SearchCriteria`
5. ✅ **Domain 반환**: DTO/Entity 반환 금지
6. ✅ **Optional 반환**: 단건 조회 시 `Optional<{Bc}>`
7. ❌ **저장/수정/삭제 메서드 금지**: PersistencePort로 분리
8. ❌ **원시 타입 파라미터 금지**: Value Object 사용
9. ✅ **Interface 여야 함**: 구현체는 Adapter에서
10. ✅ **Public Interface**: 외부 접근 가능

---

## 2️⃣ ArchUnit 테스트 템플릿

```java
package com.ryuqq.application.architecture;

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
 *   <li>4개 표준 메서드 필수: findById, existsById, findByCriteria, countByCriteria</li>
 *   <li>Value Object 파라미터: {Bc}Id, {Bc}SearchCriteria</li>
 *   <li>Domain 반환: DTO/Entity 반환 금지</li>
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
            .should().beDeclared()
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
            .should().beDeclared()
            .because("QueryPort는 existsById() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: findByCriteria() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryPort는 findByCriteria() 메서드를 가져야 한다")
    void queryPort_MustHaveFindByCriteriaMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findByCriteria")
            .should().beDeclared()
            .because("QueryPort는 findByCriteria() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: countByCriteria() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryPort는 countByCriteria() 메서드를 가져야 한다")
    void queryPort_MustHaveCountByCriteriaMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("countByCriteria")
            .should().beDeclared()
            .because("QueryPort는 countByCriteria() 메서드를 무조건 제공해야 합니다");

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
            .and().haveNameMatching("save|update|delete|remove|persist")
            .should().beDeclared()
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
     * 규칙 12: findByCriteria는 List 반환
     */
    @Test
    @DisplayName("[필수] findByCriteria()는 List를 반환해야 한다")
    void queryPort_FindByCriteriaMustReturnList() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("findByCriteria")
            .should().haveRawReturnType(List.class)
            .because("findByCriteria()는 List를 반환해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: countByCriteria는 long 반환
     */
    @Test
    @DisplayName("[필수] countByCriteria()는 long을 반환해야 한다")
    void queryPort_CountByCriteriaMustReturnLong() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryPort")
            .and().haveNameMatching("countByCriteria")
            .should().haveRawReturnType(long.class)
            .because("countByCriteria()는 long을 반환해야 합니다");

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
```

---

## 3️⃣ 검증 규칙 요약

| 번호 | 검증 항목 | 규칙 | 위반 시 |
|------|----------|------|---------|
| 1 | 인터페이스명 | `*QueryPort` | 빌드 실패 |
| 2 | 패키지 위치 | `..application..port.out.query..` | 빌드 실패 |
| 3 | Interface | 반드시 Interface | 빌드 실패 |
| 4 | Public | 반드시 Public | 빌드 실패 |
| 5-8 | 4개 표준 메서드 | 필수 | 빌드 실패 |
| 9 | 저장/수정/삭제 | 금지 (PersistencePort로) | 빌드 실패 |
| 10 | findById 반환 | `Optional<{Bc}>` | 빌드 실패 |
| 11 | existsById 반환 | `boolean` | 빌드 실패 |
| 12 | findByCriteria 반환 | `List<{Bc}>` | 빌드 실패 |
| 13 | countByCriteria 반환 | `long` | 빌드 실패 |
| 14 | DTO 반환 | 금지 (Domain 반환) | 빌드 실패 |
| 15 | Entity 반환 | 금지 (Domain 반환) | 빌드 실패 |
| 16 | 원시 타입 파라미터 | 금지 (VO 사용) | 빌드 실패 |
| 17 | 의존성 | Domain Layer만 | 빌드 실패 |

---

## 4️⃣ 실행 방법

```bash
# 전체 ArchUnit 테스트 실행
./gradlew test --tests "*ArchTest"

# QueryPort 테스트만 실행
./gradlew test --tests "QueryPortArchTest"

# 특정 규칙만 실행
./gradlew test --tests "QueryPortArchTest.queryPort_MustHaveFindByIdMethod"
```

---

## 📖 관련 문서

- **[QueryPort Guide](port-out-query-guide.md)** - QueryPort 구현 가이드
- **[QueryAdapter ArchUnit](../../../../04-persistence-layer/mysql/adapter/query/query-adapter-archunit.md)** - Adapter 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
