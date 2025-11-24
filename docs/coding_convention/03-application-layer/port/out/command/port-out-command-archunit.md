# PersistencePort ArchUnit 검증 규칙

> **목적**: PersistencePort의 구조와 규칙을 ArchUnit으로 자동 검증 (Zero-Tolerance)

---

## 1️⃣ 검증 항목 (완전 강제)

### 필수 검증 규칙

1. ✅ **인터페이스명**: `*PersistencePort`
2. ✅ **패키지 위치**: `..application..port.out.command..`
3. ✅ **메서드 개수**: `persist()` 메서드 **하나**만 제공
4. ✅ **메서드 시그니처**: `{Bc}Id persist({Bc} {bc})`
5. ✅ **반환 타입**: Value Object (`{Bc}Id`)
6. ✅ **파라미터 타입**: Domain Aggregate (`{Bc}`)
7. ❌ **조회 메서드 금지**: `findById`, `existsById` 등
8. ❌ **save/update/delete 금지**: `persist()` 하나로 통합
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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * PersistencePort ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 PersistencePort는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>인터페이스명: *PersistencePort</li>
 *   <li>패키지: ..application..port.out.command..</li>
 *   <li>메서드: persist() 하나만</li>
 *   <li>반환 타입: {Bc}Id (Value Object)</li>
 *   <li>파라미터: {Bc} (Domain Aggregate)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("PersistencePort ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class PersistencePortArchTest {

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
    @DisplayName("[필수] PersistencePort는 '*PersistencePort' 접미사를 가져야 한다")
    void persistencePort_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..port.out.command..")
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("PersistencePort")
            .because("Command Port는 'PersistencePort' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 패키지 위치
     */
    @Test
    @DisplayName("[필수] PersistencePort는 ..application..port.out.command.. 패키지에 위치해야 한다")
    void persistencePort_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("PersistencePort")
            .should().resideInAPackage("..application..port.out.command..")
            .because("PersistencePort는 application.*.port.out.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Interface 여야 함
     */
    @Test
    @DisplayName("[필수] PersistencePort는 Interface여야 한다")
    void persistencePort_MustBeInterface() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("PersistencePort")
            .should().beInterfaces()
            .because("PersistencePort는 Interface로 선언되어야 합니다 (구현체는 Adapter)");

        rule.check(classes);
    }

    /**
     * 규칙 4: Public Interface
     */
    @Test
    @DisplayName("[필수] PersistencePort는 public이어야 한다")
    void persistencePort_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("PersistencePort")
            .should().bePublic()
            .because("PersistencePort는 외부에서 접근 가능해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: persist() 메서드 존재
     */
    @Test
    @DisplayName("[필수] PersistencePort는 persist() 메서드를 가져야 한다")
    void persistencePort_MustHavePersistMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistencePort")
            .and().haveNameMatching("persist")
            .should().beDeclared()
            .because("PersistencePort는 persist() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: save/update/delete 메서드 금지
     */
    @Test
    @DisplayName("[금지] PersistencePort는 save/update/delete 메서드를 가지지 않아야 한다")
    void persistencePort_MustNotHaveSaveUpdateDeleteMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistencePort")
            .and().haveNameMatching("save|update|delete|remove")
            .should().beDeclared()
            .because("PersistencePort는 persist() 하나로 신규/수정을 통합 처리해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: 조회 메서드 금지
     */
    @Test
    @DisplayName("[금지] PersistencePort는 조회 메서드를 가지지 않아야 한다")
    void persistencePort_MustNotHaveFindMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistencePort")
            .and().haveNameMatching("find.*|get.*|load.*|exists.*|count.*")
            .should().beDeclared()
            .because("조회 메서드는 QueryPort에서 처리해야 합니다 (CQRS 분리)");

        rule.check(classes);
    }

    /**
     * 규칙 8: Domain Layer 의존성만 허용
     */
    @Test
    @DisplayName("[필수] PersistencePort는 Domain Layer만 의존해야 한다")
    void persistencePort_MustOnlyDependOnDomainLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("PersistencePort")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.ryuqq.domain..",
                "java..",
                "com.ryuqq.application.."  // 같은 application 내 DTO는 허용
            )
            .because("PersistencePort는 Domain Layer만 의존해야 합니다 (Infrastructure 의존 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 9: 원시 타입 반환 금지
     */
    @Test
    @DisplayName("[금지] PersistencePort는 원시 타입을 반환하지 않아야 한다")
    void persistencePort_MustNotReturnPrimitiveTypes() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistencePort")
            .and().haveNameMatching("persist")
            .should().haveRawReturnType(Long.class)
            .orShould().haveRawReturnType(String.class)
            .orShould().haveRawReturnType(Integer.class)
            .because("PersistencePort는 Value Object를 반환해야 합니다 (타입 안전성)");

        rule.check(classes);
    }

    /**
     * 규칙 10: DTO/Entity 파라미터 금지
     */
    @Test
    @DisplayName("[금지] PersistencePort는 DTO/Entity를 파라미터로 받지 않아야 한다")
    void persistencePort_MustNotAcceptDtoOrEntity() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistencePort")
            .should().haveRawParameterTypes(".*Dto.*")
            .orShould().haveRawParameterTypes(".*JpaEntity.*")
            .orShould().haveRawParameterTypes(".*Entity")
            .because("PersistencePort는 Domain Aggregate를 파라미터로 받아야 합니다");

        rule.check(classes);
    }
}
```

---

## 3️⃣ 검증 규칙 요약

| 번호 | 검증 항목 | 규칙 | 위반 시 |
|------|----------|------|---------|
| 1 | 인터페이스명 | `*PersistencePort` | 빌드 실패 |
| 2 | 패키지 위치 | `..application..port.out.command..` | 빌드 실패 |
| 3 | Interface | 반드시 Interface | 빌드 실패 |
| 4 | Public | 반드시 Public | 빌드 실패 |
| 5 | persist() 메서드 | 반드시 존재 | 빌드 실패 |
| 6 | save/update/delete | 금지 | 빌드 실패 |
| 7 | 조회 메서드 | 금지 (QueryPort로) | 빌드 실패 |
| 8 | 의존성 | Domain Layer만 | 빌드 실패 |
| 9 | 원시 타입 반환 | 금지 (VO 반환) | 빌드 실패 |
| 10 | DTO/Entity 파라미터 | 금지 (Domain만) | 빌드 실패 |

---

## 4️⃣ 실행 방법

```bash
# 전체 ArchUnit 테스트 실행
./gradlew test --tests "*ArchTest"

# PersistencePort 테스트만 실행
./gradlew test --tests "PersistencePortArchTest"

# 특정 규칙만 실행
./gradlew test --tests "PersistencePortArchTest.persistencePort_MustHavePersistMethod"
```

---

## 📖 관련 문서

- **[PersistencePort Guide](port-out-command-guide.md)** - PersistencePort 구현 가이드
- **[CommandAdapter ArchUnit](../../../../04-persistence-layer/mysql/adapter/command/command-adapter-archunit.md)** - Adapter 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
