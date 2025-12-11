# CommandAdapter ArchUnit 검증 규칙

> **목적**: CommandAdapter 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### 필수 검증 규칙
1. ✅ **@Component 필수**
2. ✅ **Port 구현 필수**
3. ✅ **필드 개수: 정확히 2개** (JpaRepository + Mapper)
4. ✅ **public 메서드: 정확히 1개** (persist()만)
5. ✅ **메서드명: persist** (정확히)
6. ✅ **반환 타입: *Id** (OrderId, ProductId 등)
7. ✅ **@Transactional 절대 금지**
8. ✅ **Query 메서드 금지** (find*, load*, get*)
9. ✅ **비즈니스 메서드 금지** (confirm*, cancel*, delete*, update*)
10. ✅ **클래스명: *CommandAdapter**
11. ✅ **패키지 위치: ..adapter.out.persistence..adapter..**
12. ✅ **생성자 주입 (final 필드)**

---

## 2️⃣ 의존성 추가

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## 3️⃣ ArchUnit 테스트 (완전 강제 버전)

```java
package com.company.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * CommandAdapter ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 CommandAdapter는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("CommandAdapter ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class CommandAdapterArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence");
    }

    /**
     * 규칙 1: @Component 필수
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 @Component 어노테이션을 가져야 한다")
    void commandAdapter_MustHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().beAnnotatedWith(Component.class)
            .because("CommandAdapter는 Spring Bean으로 등록되어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: Port 구현 필수
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 Port 인터페이스를 구현해야 한다")
    void commandAdapter_MustImplementPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().implement("PersistencePort")
            .orShould().implement(".*Port")
            .because("CommandAdapter는 Port 인터페이스를 구현해야 합니다 (헥사고날 아키텍처)");

        rule.check(classes);
    }

    /**
     * 규칙 3: 필드 개수 정확히 2개 (JpaRepository + Mapper)
     */
    @Test
    @DisplayName("[강제] CommandAdapter는 정확히 2개의 필드를 가져야 한다")
    void commandAdapter_MustHaveExactlyTwoFields() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().haveOnlyFinalFields()
            .andShould().haveNumberOfFields(2)
            .because("CommandAdapter는 JpaRepository와 Mapper 정확히 2개 필드만 가져야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: JpaRepository 필드 필수
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 JpaRepository 타입 필드를 가져야 한다")
    void commandAdapter_MustHaveJpaRepositoryField() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().dependOnClassesThat().areAssignableTo(JpaRepository.class)
            .because("CommandAdapter는 JpaRepository를 통해 저장해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Mapper 필드 필수
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 Mapper 타입 필드를 가져야 한다")
    void commandAdapter_MustHaveMapperField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .should().haveRawType(".*JpaEntityMapper")
            .orShould().haveRawType(".*Mapper")
            .because("CommandAdapter는 Domain ↔ Entity 변환을 위해 Mapper가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: public 메서드는 정확히 1개 (persist만)
     */
    @Test
    @DisplayName("[강제] CommandAdapter는 public 메서드를 정확히 1개만 가져야 한다")
    void commandAdapter_MustHaveExactlyOnePublicMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().haveOnlyPublicMethods("persist")
            .because("CommandAdapter는 persist() 메서드만 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: persist() 메서드명 강제
     */
    @Test
    @DisplayName("[강제] CommandAdapter의 public 메서드명은 정확히 'persist'여야 한다")
    void commandAdapter_PublicMethodNameMustBePersist() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().arePublic()
            .and().doNotHaveFullName(".*<init>.*")  // 생성자 제외
            .should().haveName("persist")
            .because("CommandAdapter는 persist() 메서드만 제공해야 합니다 (update, delete 메서드 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 8: persist() 반환 타입은 *Id
     */
    @Test
    @DisplayName("[강제] persist() 메서드는 *Id 타입을 반환해야 한다")
    void persistMethod_MustReturnIdType() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().haveName("persist")
            .should().haveRawReturnType(".*Id")
            .because("persist() 메서드는 저장된 Entity의 ID를 반환해야 합니다 (OrderId, ProductId 등)");

        rule.check(classes);
    }

    /**
     * 규칙 9: @Transactional 절대 금지
     */
    @Test
    @DisplayName("[금지] CommandAdapter는 @Transactional을 가지지 않아야 한다")
    void commandAdapter_MustNotHaveTransactionalAnnotation() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .because("@Transactional은 Application Layer(UseCase)에서만 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Query 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] CommandAdapter는 Query 메서드를 가지지 않아야 한다")
    void commandAdapter_MustNotHaveQueryMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().arePublic()
            .and().haveNameMatching("find.*|load.*|get.*|search.*|query.*|select.*")
            .should().beDeclared()
            .because("CommandAdapter는 Query 메서드를 가질 수 없습니다 (CQRS 분리 - QueryAdapter로)");

        rule.check(classes);
    }

    /**
     * 규칙 11: 비즈니스 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] CommandAdapter는 비즈니스 메서드를 가지지 않아야 한다")
    void commandAdapter_MustNotHaveBusinessMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().arePublic()
            .and().haveNameMatching("confirm.*|cancel.*|delete.*|update.*|approve.*|reject.*|modify.*|change.*")
            .should().beDeclared()
            .because("CommandAdapter는 비즈니스 로직을 가질 수 없습니다 (Domain에서 처리)");

        rule.check(classes);
    }

    /**
     * 규칙 12: 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 'CommandAdapter' 접미사를 가져야 한다")
    void commandAdapter_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..adapter.out.persistence..")
            .and().implement(".*Port")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("CommandAdapter")
            .because("Command Adapter는 'CommandAdapter' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: 패키지 위치
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 ..adapter.out.persistence..adapter.. 패키지에 위치해야 한다")
    void commandAdapter_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().resideInAPackage("..adapter.out.persistence..adapter..")
            .because("CommandAdapter는 adapter.out.persistence.*.adapter 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: Public 클래스
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 public 클래스여야 한다")
    void commandAdapter_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().bePublic()
            .because("CommandAdapter는 Spring Bean으로 등록되기 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 15: Final 클래스 금지
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 final 클래스가 아니어야 한다")
    void commandAdapter_MustNotBeFinal() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().notBeFinal()
            .because("Spring은 프록시 생성을 위해 CommandAdapter가 final이 아니어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 16: 생성자 주입 (final 필드)
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 생성자 주입을 사용해야 한다 (final 필드)")
    void commandAdapter_MustUseConstructorInjection() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().areNotStatic()
            .should().beFinal()
            .because("CommandAdapter는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

        rule.check(classes);
    }

    /**
     * 규칙 17: Domain Layer 의존성만 허용
     */
    @Test
    @DisplayName("[필수] CommandAdapter는 Domain Layer만 의존해야 한다")
    void commandAdapter_MustOnlyDependOnDomainLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.company.domain..",
                "com.company.adapter.out.persistence..",
                "org.springframework..",
                "java..",
                "jakarta.."
            )
            .because("CommandAdapter는 Domain Layer와 Persistence Layer만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 18: 필드명 규칙 (소문자 시작)
     */
    @Test
    @DisplayName("[권장] CommandAdapter의 필드명은 소문자로 시작해야 한다")
    void commandAdapter_FieldsShouldStartWithLowercase() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().areNotStatic()
            .should().haveNameMatching("[a-z].*")
            .because("필드명은 camelCase 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 19: 생성자는 정확히 1개
     */
    @Test
    @DisplayName("[강제] CommandAdapter는 생성자를 정확히 1개만 가져야 한다")
    void commandAdapter_MustHaveExactlyOneConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CommandAdapter")
            .should().haveOnlyOneConstructor()
            .because("CommandAdapter는 생성자 주입을 위해 생성자 1개만 가져야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 20: persist() 메서드는 Override 필수
     */
    @Test
    @DisplayName("[필수] persist() 메서드는 @Override 어노테이션을 가져야 한다")
    void persistMethod_MustHaveOverrideAnnotation() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CommandAdapter")
            .and().haveName("persist")
            .should().beAnnotatedWith(Override.class)
            .because("persist() 메서드는 Port 인터페이스 구현이므로 @Override가 필요합니다");

        rule.check(classes);
    }
}
```

---

## 4️⃣ 실행 방법

### Maven
```bash
# ArchUnit 테스트만 실행
mvn test -Dtest=CommandAdapterArchTest

# 전체 아키텍처 테스트
mvn test -Dtest=*ArchTest
```

### Gradle
```bash
# ArchUnit 테스트만 실행
./gradlew test --tests CommandAdapterArchTest

# 전체 아키텍처 테스트
./gradlew test --tests '*ArchTest'
```

---

## 5️⃣ 검증 결과 예시

### ✅ 성공
```
CommandAdapterArchTest > commandAdapter_MustHaveComponentAnnotation() PASSED
CommandAdapterArchTest > commandAdapter_MustHaveExactlyTwoFields() PASSED
CommandAdapterArchTest > commandAdapter_PublicMethodNameMustBePersist() PASSED
CommandAdapterArchTest > commandAdapter_MustNotHaveQueryMethods() PASSED
...
20 tests passed
```

### ❌ 실패 예시 1: Query 메서드 포함
```
commandAdapter_MustNotHaveQueryMethods() FAILED
    Rule: no methods that are public and have name matching 'find.*|load.*'
    Violation: Method <OrderCommandAdapter.findById(OrderId)> in (OrderCommandAdapter.java:45)

➡️ 해결: findById() 메서드 제거 → QueryAdapter로 이동
```

### ❌ 실패 예시 2: 필드 개수 초과
```
commandAdapter_MustHaveExactlyTwoFields() FAILED
    Rule: should have number of fields 2
    Violation: Class <OrderCommandAdapter> has 3 fields

➡️ 해결: 불필요한 필드 제거 (JpaRepository + Mapper만 유지)
```

### ❌ 실패 예시 3: 비즈니스 메서드 포함
```
commandAdapter_MustNotHaveBusinessMethods() FAILED
    Rule: no methods that have name matching 'confirm.*|cancel.*'
    Violation: Method <OrderCommandAdapter.confirmOrder(OrderId)> in (OrderCommandAdapter.java:52)

➡️ 해결: confirmOrder() 메서드 제거 → Domain 메서드 활용
```

### ❌ 실패 예시 4: @Transactional 사용
```
commandAdapter_MustNotHaveTransactionalAnnotation() FAILED
    Rule: no classes should be annotated with @Transactional
    Violation: Class <OrderCommandAdapter> is annotated with @Transactional

➡️ 해결: @Transactional 제거 → UseCase에서 관리
```

---

## 6️⃣ CI/CD 통합

### GitHub Actions (빌드 실패 강제)
```yaml
name: Architecture Validation

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ main, develop ]

jobs:
  archunit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run ArchUnit Tests (Zero-Tolerance)
        run: mvn test -Dtest=CommandAdapterArchTest

      - name: Fail on Architecture Violation
        if: failure()
        run: |
          echo "❌ Architecture violation detected!"
          echo "CommandAdapter must follow strict rules."
          echo "See test results for details."
          exit 1
```

---

## 7️⃣ 체크리스트 (20개 규칙)

ArchUnit 테스트 완료 확인:
- [ ] ✅ @Component 필수
- [ ] ✅ Port 구현 필수
- [ ] ✅ 필드 개수: 정확히 2개
- [ ] ✅ JpaRepository 필드 필수
- [ ] ✅ Mapper 필드 필수
- [ ] ✅ public 메서드: 정확히 1개
- [ ] ✅ 메서드명: persist (정확히)
- [ ] ✅ 반환 타입: *Id
- [ ] ❌ @Transactional 절대 금지
- [ ] ❌ Query 메서드 금지
- [ ] ❌ 비즈니스 메서드 금지
- [ ] ✅ 클래스명: *CommandAdapter
- [ ] ✅ 패키지 위치: ..adapter.out.persistence..adapter..
- [ ] ✅ public 클래스
- [ ] ✅ final 클래스 금지
- [ ] ✅ 생성자 주입 (final 필드)
- [ ] ✅ Domain Layer 의존성만
- [ ] ✅ 필드명 소문자 시작
- [ ] ✅ 생성자 정확히 1개
- [ ] ✅ persist() @Override 필수

---

## 📖 관련 문서

- **[CommandAdapter Guide](command-adapter-guide.md)** - CommandAdapter 구현 가이드
- **[CommandAdapter Test Guide](command-adapter-test-guide.md)** - 테스트 전략

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 2.0.0 (Zero-Tolerance)
