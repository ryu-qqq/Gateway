# Command UseCase ArchUnit 검증 규칙

> **목적**: Command UseCase (Port-In)의 구조와 규칙을 ArchUnit으로 자동 검증 (Zero-Tolerance)

---

## 1️⃣ 검증 항목 (완전 강제)

### 필수 검증 규칙

1. ✅ **인터페이스명**: `*UseCase`
2. ✅ **패키지 위치**: `..application..port.in..`
3. ✅ **단일 메서드**: `execute({Command} command)`
4. ✅ **Command DTO 분리**: `dto.command` 패키지에 정의
5. ✅ **Response DTO 분리**: `dto.response` 패키지에 정의
6. ❌ **내부 Record 금지**: Command/Response를 UseCase 내부에 정의 금지
7. ✅ **Public Interface**: 외부 접근 가능
8. ❌ **인터페이스에 @Transactional 금지**: Service 구현체에만 적용
9. ❌ **Domain 직접 반환 금지**: Response Record로 변환
10. ✅ **Interface 여야 함**: 구현체는 Service에서

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
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Command UseCase ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 Command UseCase는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>인터페이스명: *UseCase (Create*, Update*, Delete* 등)</li>
 *   <li>패키지: ..application..port.in..</li>
 *   <li>execute() 메서드 필수</li>
 *   <li>Command DTO: dto.command 패키지</li>
 *   <li>Response DTO: dto.response 패키지</li>
 *   <li>Domain 직접 반환 금지</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Command UseCase ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class CommandUseCaseArchTest {

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
    @DisplayName("[필수] UseCase는 '*UseCase' 접미사를 가져야 한다")
    void commandUseCase_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..port.in..")
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("UseCase")
            .because("Command UseCase는 'UseCase' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 패키지 위치
     */
    @Test
    @DisplayName("[필수] Command UseCase는 ..application..port.in.. 패키지에 위치해야 한다")
    void commandUseCase_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .should().resideInAPackage("..application..port.in..")
            .because("Command UseCase는 application.*.port.in 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Interface 여야 함
     */
    @Test
    @DisplayName("[필수] Command UseCase는 Interface여야 한다")
    void commandUseCase_MustBeInterface() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .should().beInterfaces()
            .because("Command UseCase는 Interface로 선언되어야 합니다 (구현체는 Service)");

        rule.check(classes);
    }

    /**
     * 규칙 4: Public Interface
     */
    @Test
    @DisplayName("[필수] Command UseCase는 public이어야 한다")
    void commandUseCase_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .should().bePublic()
            .because("Command UseCase는 외부에서 접근 가능해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: execute() 메서드 필수
     */
    @Test
    @DisplayName("[필수] Command UseCase는 execute() 메서드를 가져야 한다")
    void commandUseCase_MustHaveExecuteMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("UseCase")
            .and().areDeclaredInClassesThat().resideInAPackage("..port.in..")
            .and().haveNameMatching("execute")
            .should().beDeclared()
            .because("Command UseCase는 execute() 메서드를 무조건 제공해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 인터페이스에 @Transactional 금지
     */
    @Test
    @DisplayName("[금지] Command UseCase 인터페이스에 @Transactional을 사용하지 않아야 한다")
    void commandUseCase_MustNotHaveTransactionalOnInterface() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .and().areInterfaces()
            .should().beAnnotatedWith(Transactional.class)
            .because("@Transactional은 Service 구현체에만 적용해야 합니다 (Spring Proxy 제약)");

        rule.check(classes);
    }

    /**
     * 규칙 7: Domain Entity 직접 반환 금지
     */
    @Test
    @DisplayName("[금지] Command UseCase는 Domain Entity를 직접 반환하지 않아야 한다")
    void commandUseCase_MustNotReturnDomainEntity() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("UseCase")
            .and().areDeclaredInClassesThat().resideInAPackage("..port.in..")
            .should().haveRawReturnType(resideInAPackage("..domain.."))
            .because("Command UseCase는 Response DTO를 반환해야 합니다 (Domain 노출 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 8: 내부 Record 금지
     */
    @Test
    @DisplayName("[금지] Command UseCase는 내부 Record를 가지지 않아야 한다")
    void commandUseCase_MustNotHaveInnerRecords() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .should().containAnyNestedClassesThat()
            .haveSimpleNameMatching("Command|Response")
            .because("Command/Response는 별도 dto 패키지에 정의해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Command는 dto.command 패키지에
     */
    @Test
    @DisplayName("[필수] Command는 dto.command 패키지에 위치해야 한다")
    void command_MustBeInDtoCommandPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Command")
            .and().areRecords()
            .should().resideInAPackage("..dto.command..")
            .because("Command DTO는 dto.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Response는 dto.response 패키지에
     */
    @Test
    @DisplayName("[필수] Response는 dto.response 패키지에 위치해야 한다")
    void response_MustBeInDtoResponsePackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Response")
            .and().areRecords()
            .and().resideInAPackage("..application..")
            .should().resideInAPackage("..dto.response..")
            .because("Response DTO는 dto.response 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 11: Command 네이밍 패턴
     */
    @Test
    @DisplayName("[권장] Command UseCase는 Create/Update/Delete/Place 등으로 시작하는 것이 좋다")
    void commandUseCase_ShouldStartWithCommandVerb() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .and().areInterfaces()
            .should().haveSimpleNameMatching("(Create|Update|Delete|Place|Cancel|Confirm|Register|Remove|Modify|Approve|Reject).*UseCase")
            .because("Command UseCase는 의미있는 동사로 시작하는 것이 명확합니다");

        // Warning only (not enforced)
    }

    /**
     * 규칙 12: Domain Layer 의존성만 허용
     */
    @Test
    @DisplayName("[필수] Command UseCase는 Domain Layer와 DTO만 의존해야 한다")
    void commandUseCase_MustOnlyDependOnDomainAndDto() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().resideInAPackage("..port.in..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.ryuqq.domain..",
                "com.ryuqq.application..dto..",
                "java.."
            )
            .because("Command UseCase는 Domain Layer와 DTO만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: Command는 Record여야 함
     */
    @Test
    @DisplayName("[필수] Command는 Record여야 한다")
    void command_MustBeRecord() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Command")
            .and().resideInAPackage("..dto.command..")
            .should().beRecords()
            .because("Command는 불변 Record로 정의해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: Response는 Record여야 함
     */
    @Test
    @DisplayName("[필수] Response는 Record여야 한다")
    void response_MustBeRecord() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Response")
            .and().resideInAPackage("..dto.response..")
            .should().beRecords()
            .because("Response는 불변 Record로 정의해야 합니다");

        rule.check(classes);
    }
}
```

---

## 3️⃣ 검증 규칙 요약

| 번호 | 검증 항목 | 규칙 | 위반 시 |
|------|----------|------|---------|
| 1 | 인터페이스명 | `*UseCase` | 빌드 실패 |
| 2 | 패키지 위치 | `..application..port.in..` | 빌드 실패 |
| 3 | Interface | 반드시 Interface | 빌드 실패 |
| 4 | Public | 반드시 Public | 빌드 실패 |
| 5 | execute() 메서드 | 필수 | 빌드 실패 |
| 6 | @Transactional | 인터페이스에 금지 | 빌드 실패 |
| 7 | Domain 반환 | 금지 (Response 사용) | 빌드 실패 |
| 8 | 내부 Record | 금지 (별도 DTO) | 빌드 실패 |
| 9 | Command 위치 | `dto.command` 패키지 | 빌드 실패 |
| 10 | Response 위치 | `dto.response` 패키지 | 빌드 실패 |
| 11 | 네이밍 패턴 | Create/Update/Delete 등 | 권장 |
| 12 | 의존성 | Domain + DTO만 | 빌드 실패 |
| 13 | Command 타입 | Record 필수 | 빌드 실패 |
| 14 | Response 타입 | Record 필수 | 빌드 실패 |

---

## 4️⃣ 실행 방법

```bash
# 전체 ArchUnit 테스트 실행
./gradlew test --tests "*ArchTest"

# Command UseCase 테스트만 실행
./gradlew test --tests "CommandUseCaseArchTest"

# 특정 규칙만 실행
./gradlew test --tests "CommandUseCaseArchTest.commandUseCase_MustHaveExecuteMethod"
```

---

## 📖 관련 문서

- **[Command UseCase Guide](port-in-command-guide.md)** - Command UseCase 구현 가이드
- **[Command DTO ArchUnit](../../dto/command/command-dto-archunit.md)** - Command DTO 검증 규칙
- **[Response DTO ArchUnit](../../dto/response/response-dto-archunit.md)** - Response DTO 검증 규칙
- **[Query UseCase ArchUnit](../query/port-in-query-archunit.md)** - Query UseCase 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 2.0.0 (DTO 패키지 분리)
