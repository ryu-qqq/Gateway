# QueryAdapter ArchUnit 검증 규칙

> **목적**: QueryAdapter 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### 필수 검증 규칙
1. ✅ **@Component 필수**
2. ✅ **Port 구현 필수**
3. ✅ **필드 개수: 정확히 2개** (QueryDslRepository + Mapper)
4. ✅ **public 메서드: 정확히 4개** (findById, existsById, findByCriteria, countByCriteria)
5. ✅ **메서드명: findById, existsById, findByCriteria, countByCriteria** (정확히)
6. ✅ **반환 타입: Domain** (Optional<Bc>, boolean, List<Bc>, long)
7. ✅ **@Transactional 절대 금지**
8. ✅ **Command 메서드 금지** (save*, persist*, update*, delete*)
9. ✅ **클래스명: *QueryAdapter**
10. ✅ **패키지 위치: ..adapter.out.persistence..adapter..**
11. ✅ **생성자 주입 (final 필드)**
12. ✅ **Mapper 필드 필수**

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
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * QueryAdapter ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 QueryAdapter는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("QueryAdapter ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class QueryAdapterArchTest {

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
    @DisplayName("[필수] QueryAdapter는 @Component 어노테이션을 가져야 한다")
    void queryAdapter_MustHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().beAnnotatedWith(Component.class)
            .because("QueryAdapter는 Spring Bean으로 등록되어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: Port 구현 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 Port 인터페이스를 구현해야 한다")
    void queryAdapter_MustImplementPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().implement("QueryPort")
            .orShould().implement(".*Port")
            .because("QueryAdapter는 Port 인터페이스를 구현해야 합니다 (헥사고날 아키텍처)");

        rule.check(classes);
    }

    /**
     * 규칙 3: 필드 개수 정확히 2개 (QueryDslRepository + Mapper)
     */
    @Test
    @DisplayName("[강제] QueryAdapter는 정확히 2개의 필드를 가져야 한다")
    void queryAdapter_MustHaveExactlyTwoFields() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveOnlyFinalFields()
            .andShould().haveNumberOfFields(2)
            .because("QueryAdapter는 QueryDslRepository와 Mapper 정확히 2개 필드만 가져야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: QueryDslRepository 필드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 QueryDslRepository 타입 필드를 가져야 한다")
    void queryAdapter_MustHaveQueryDslRepositoryField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveRawType(".*QueryDslRepository")
            .because("QueryAdapter는 QueryDslRepository를 통해 조회해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Mapper 필드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 Mapper 타입 필드를 가져야 한다")
    void queryAdapter_MustHaveMapperField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveRawType(".*JpaEntityMapper")
            .orShould().haveRawType(".*Mapper")
            .because("QueryAdapter는 Entity → Domain 변환을 위해 Mapper가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: public 메서드는 정확히 4개 (findById, existsById, findByCriteria, countByCriteria)
     */
    @Test
    @DisplayName("[강제] QueryAdapter는 public 메서드를 정확히 4개만 가져야 한다")
    void queryAdapter_MustHaveExactlyFourPublicMethods() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveOnlyPublicMethods("findById", "existsById", "findByCriteria", "countByCriteria")
            .because("QueryAdapter는 findById, existsById, findByCriteria, countByCriteria 메서드만 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: findById() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 findById() 메서드를 가져야 한다")
    void queryAdapter_MustHaveFindByIdMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().haveName("findById")
            .should().bePublic()
            .andShould().haveRawReturnType("java.util.Optional")
            .because("QueryAdapter는 단건 조회를 위한 findById() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: findByCriteria() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 findByCriteria() 메서드를 가져야 한다")
    void queryAdapter_MustHaveFindByCriteriaMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().haveName("findByCriteria")
            .should().bePublic()
            .andShould().haveRawReturnType("java.util.List")
            .because("QueryAdapter는 목록 조회를 위한 findByCriteria() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: countByCriteria() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 countByCriteria() 메서드를 가져야 한다")
    void queryAdapter_MustHaveCountByCriteriaMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().haveName("countByCriteria")
            .should().bePublic()
            .andShould().haveRawReturnType(long.class)
            .because("QueryAdapter는 개수 조회를 위한 countByCriteria() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: existsById() 메서드 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 existsById() 메서드를 가져야 한다")
    void queryAdapter_MustHaveExistsByIdMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().haveName("existsById")
            .should().bePublic()
            .andShould().haveRawReturnType(boolean.class)
            .because("QueryAdapter는 존재 여부 확인을 위한 existsById() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 11: @Transactional 절대 금지
     */
    @Test
    @DisplayName("[금지] QueryAdapter는 @Transactional을 가지지 않아야 한다")
    void queryAdapter_MustNotHaveTransactionalAnnotation() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .because("@Transactional은 Application Layer(UseCase)에서만 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: Command 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] QueryAdapter는 Command 메서드를 가지지 않아야 한다")
    void queryAdapter_MustNotHaveCommandMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().arePublic()
            .and().haveNameMatching("save.*|persist.*|update.*|delete.*|insert.*|remove.*|create.*")
            .should().beDeclared()
            .because("QueryAdapter는 Command 메서드를 가질 수 없습니다 (CQRS 분리 - CommandAdapter로)");

        rule.check(classes);
    }

    /**
     * 규칙 13: 비즈니스 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] QueryAdapter는 비즈니스 메서드를 가지지 않아야 한다")
    void queryAdapter_MustNotHaveBusinessMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().arePublic()
            .and().haveNameMatching("confirm.*|cancel.*|approve.*|reject.*|modify.*|change.*")
            .should().beDeclared()
            .because("QueryAdapter는 비즈니스 로직을 가질 수 없습니다 (Domain에서 처리)");

        rule.check(classes);
    }

    /**
     * 규칙 13: 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 'QueryAdapter' 접미사를 가져야 한다")
    void queryAdapter_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..adapter.out.persistence..")
            .and().implement(".*QueryPort")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("QueryAdapter")
            .because("Query Adapter는 'QueryAdapter' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: 패키지 위치
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 ..adapter.out.persistence..adapter.. 패키지에 위치해야 한다")
    void queryAdapter_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().resideInAPackage("..adapter.out.persistence..adapter..")
            .because("QueryAdapter는 adapter.out.persistence.*.adapter 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 15: Public 클래스
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 public 클래스여야 한다")
    void queryAdapter_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().bePublic()
            .because("QueryAdapter는 Spring Bean으로 등록되기 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 16: Final 클래스 금지
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 final 클래스가 아니어야 한다")
    void queryAdapter_MustNotBeFinal() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().notBeFinal()
            .because("Spring은 프록시 생성을 위해 QueryAdapter가 final이 아니어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 17: 생성자 주입 (final 필드)
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 생성자 주입을 사용해야 한다 (final 필드)")
    void queryAdapter_MustUseConstructorInjection() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().areNotStatic()
            .should().beFinal()
            .because("QueryAdapter는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

        rule.check(classes);
    }

    /**
     * 규칙 18: Domain Layer 의존성만 허용
     */
    @Test
    @DisplayName("[필수] QueryAdapter는 Domain Layer만 의존해야 한다")
    void queryAdapter_MustOnlyDependOnDomainLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage(
                "com.company.domain..",
                "com.company.adapter.out.persistence..",
                "org.springframework..",
                "java..",
                "jakarta.."
            )
            .because("QueryAdapter는 Domain Layer와 Persistence Layer만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 19: 필드명 규칙 (소문자 시작)
     */
    @Test
    @DisplayName("[권장] QueryAdapter의 필드명은 소문자로 시작해야 한다")
    void queryAdapter_FieldsShouldStartWithLowercase() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().areNotStatic()
            .should().haveNameMatching("[a-z].*")
            .because("필드명은 camelCase 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 20: 생성자는 정확히 1개
     */
    @Test
    @DisplayName("[강제] QueryAdapter는 생성자를 정확히 1개만 가져야 한다")
    void queryAdapter_MustHaveExactlyOneConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().haveOnlyOneConstructor()
            .because("QueryAdapter는 생성자 주입을 위해 생성자 1개만 가져야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 21: 모든 메서드는 @Override 필수
     */
    @Test
    @DisplayName("[필수] QueryAdapter의 모든 public 메서드는 @Override 어노테이션을 가져야 한다")
    void queryAdapterMethods_MustHaveOverrideAnnotation() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryAdapter")
            .and().arePublic()
            .and().doNotHaveFullName(".*<init>.*")  // 생성자 제외
            .should().beAnnotatedWith(Override.class)
            .because("QueryAdapter의 메서드는 Port 인터페이스 구현이므로 @Override가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 22: JPAQueryFactory 직접 사용 금지
     */
    @Test
    @DisplayName("[금지] QueryAdapter는 JPAQueryFactory를 직접 사용하지 않아야 한다")
    void queryAdapter_MustNotUseJPAQueryFactoryDirectly() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryAdapter")
            .should().accessClassesThat().haveNameMatching(".*JPAQueryFactory.*")
            .because("QueryAdapter는 JPAQueryFactory를 직접 사용하지 않고 QueryDslRepository를 통해 조회해야 합니다");

        rule.check(classes);
    }
}
```

---

## 4️⃣ 실행 방법

### Maven
```bash
# ArchUnit 테스트만 실행
mvn test -Dtest=QueryAdapterArchTest

# 전체 아키텍처 테스트
mvn test -Dtest=*ArchTest
```

### Gradle
```bash
# ArchUnit 테스트만 실행
./gradlew test --tests QueryAdapterArchTest

# 전체 아키텍처 테스트
./gradlew test --tests '*ArchTest'
```

---

## 5️⃣ 검증 결과 예시

### ✅ 성공
```
QueryAdapterArchTest > queryAdapter_MustHaveComponentAnnotation() PASSED
QueryAdapterArchTest > queryAdapter_MustHaveExactlyTwoFields() PASSED
QueryAdapterArchTest > queryAdapter_MustHaveFindByIdMethod() PASSED
QueryAdapterArchTest > queryAdapter_MustNotHaveCommandMethods() PASSED
...
22 tests passed
```

### ❌ 실패 예시 1: Command 메서드 포함
```
queryAdapter_MustNotHaveCommandMethods() FAILED
    Rule: no methods that are public and have name matching 'save.*|persist.*'
    Violation: Method <OrderQueryAdapter.saveOrder(Order)> in (OrderQueryAdapter.java:45)

➡️ 해결: saveOrder() 메서드 제거 → CommandAdapter로 이동
```

### ❌ 실패 예시 2: 필드 개수 초과
```
queryAdapter_MustHaveExactlyTwoFields() FAILED
    Rule: should have number of fields 2
    Violation: Class <OrderQueryAdapter> has 3 fields

➡️ 해결: 불필요한 필드 제거 (QueryDslRepository + Mapper만 유지)
```

### ❌ 실패 예시 3: JPAQueryFactory 직접 사용
```
queryAdapter_MustNotUseJPAQueryFactoryDirectly() FAILED
    Rule: no classes should access classes matching '.*JPAQueryFactory.*'
    Violation: Class <OrderQueryAdapter> accesses <JPAQueryFactory>

➡️ 해결: JPAQueryFactory 제거 → QueryDslRepository 사용
```

### ❌ 실패 예시 4: Mapper 누락
```
queryAdapter_MustHaveMapperField() FAILED
    Rule: fields should have raw type '.*JpaEntityMapper' or '.*Mapper'
    Violation: Class <OrderQueryAdapter> has no Mapper field

➡️ 해결: OrderJpaEntityMapper 필드 추가
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
        run: mvn test -Dtest=QueryAdapterArchTest

      - name: Fail on Architecture Violation
        if: failure()
        run: |
          echo "❌ Architecture violation detected!"
          echo "QueryAdapter must follow strict rules."
          echo "See test results for details."
          exit 1
```

---

## 7️⃣ 체크리스트 (23개 규칙)

ArchUnit 테스트 완료 확인:
- [ ] ✅ @Component 필수
- [ ] ✅ Port 구현 필수
- [ ] ✅ 필드 개수: 정확히 2개
- [ ] ✅ QueryDslRepository 필드 필수
- [ ] ✅ Mapper 필드 필수
- [ ] ✅ public 메서드: 정확히 4개
- [ ] ✅ findById() 메서드 필수
- [ ] ✅ findByCriteria() 메서드 필수
- [ ] ✅ countByCriteria() 메서드 필수
- [ ] ✅ existsById() 메서드 필수
- [ ] ❌ @Transactional 절대 금지
- [ ] ❌ Command 메서드 금지
- [ ] ❌ 비즈니스 메서드 금지
- [ ] ✅ 클래스명: *QueryAdapter
- [ ] ✅ 패키지 위치: ..adapter.out.persistence..adapter..
- [ ] ✅ public 클래스
- [ ] ✅ final 클래스 금지
- [ ] ✅ 생성자 주입 (final 필드)
- [ ] ✅ Domain Layer 의존성만
- [ ] ✅ 필드명 소문자 시작
- [ ] ✅ 생성자 정확히 1개
- [ ] ✅ 모든 메서드 @Override 필수
- [ ] ❌ JPAQueryFactory 직접 사용 금지

---

## 📖 관련 문서

- **[QueryAdapter Guide](query-adapter-guide.md)** - QueryAdapter 구현 가이드
- **[QueryAdapter Test Guide](query-adapter-test-guide.md)** - 테스트 전략

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0 (Zero-Tolerance)
