# Assembler ArchUnit 검증 규칙

> **목적**: Assembler 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **핵심 철학**: Assembler는 **Domain → Response 변환만** 담당 (toDomain 금지)
>
> **업계 레퍼런스**: [Sairyss/domain-driven-hexagon](https://github.com/Sairyss/domain-driven-hexagon), Martin Fowler PoEAA

---

## 1) 검증 항목 (Zero-Tolerance)

### 필수 검증 규칙 (19개)

| # | 규칙 | 유형 | 설명 |
|---|------|------|------|
| 1 | **@Component 필수** | ✅ 필수 | Spring Bean 등록 필수 |
| 2 | **Lombok 절대 금지** | ❌ 금지 | Plain Java 원칙 |
| 3 | **Static 메서드 금지** | ❌ 금지 | 테스트 용이성 |
| 4 | **Port 의존성 금지** | ❌ 금지 | 단순 변환기 원칙 |
| 5 | **Repository 의존성 금지** | ❌ 금지 | DB 접근 금지 |
| 6 | **Spring Data 의존성 금지** | ❌ 금지 | Page, Slice, Pageable 금지 |
| 7 | **클래스명 규칙** | ✅ 필수 | `*Assembler` 접미사 |
| 8 | **패키지 위치** | ✅ 필수 | `..application..assembler..` |
| 9 | **메서드명 규칙** | ✅ 필수 | `toResponse*`, `toResponseList*`만 허용 |
| 10 | **toDomain 메서드 금지** | ❌ 금지 | **핵심 규칙**: Command → Domain은 Creator에서 |
| 11 | **비즈니스 메서드 금지** | ❌ 금지 | validate*, place* 등 금지 |
| 12 | **@Transactional 금지** | ❌ 금지 | UseCase에서만 사용 |
| 13 | **PageResponse 반환 금지** | ❌ 금지 | UseCase에서 조립 |
| 14 | **public 클래스** | ✅ 필수 | Spring Bean 등록 |
| 15 | **final 클래스 금지** | ✅ 필수 | Spring Proxy |
| 16 | **필드 final** | ✅ 권장 | 생성자 주입 |
| 17 | **Layer 의존성** | ✅ 필수 | Application + Domain만 |
| 18 | **필드명 규칙** | ✅ 권장 | 소문자 시작 |
| 19 | **계산 로직 금지** | ❌ 금지 | Domain에서 처리 |

### 핵심 금지 규칙 강조

```
❌ toDomain() 메서드 절대 금지!

이유:
1. Command → Domain 변환은 DB 조회가 필요할 수 있음
2. 유효성 검증은 Domain 생성 시점에 수행해야 함
3. 복잡한 객체 그래프 구성은 Creator/Factory 책임

→ Creator 패턴 사용: OrderCreator.create(PlaceOrderCommand)
```

---

## 2) 의존성 추가

### Gradle
```groovy
dependencies {
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
}
```

### Maven
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## 3) ArchUnit 테스트 (19개 규칙)

```java
package com.company.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Assembler ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Assembler는 Domain → Response 변환만 담당</p>
 * <p>toDomain() 메서드는 절대 금지 (Creator 패턴 사용)</p>
 *
 * <p>업계 레퍼런스:</p>
 * <ul>
 *   <li>Sairyss/domain-driven-hexagon (12k+ stars)</li>
 *   <li>Martin Fowler - Patterns of Enterprise Application Architecture</li>
 * </ul>
 *
 * @see <a href="https://github.com/Sairyss/domain-driven-hexagon">domain-driven-hexagon</a>
 */
@DisplayName("Assembler ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("assembler")
class AssemblerArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.application");
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        /**
         * 규칙 1: @Component 필수
         */
        @Test
        @DisplayName("[필수] Assembler는 @Component 어노테이션을 가져야 한다")
        void assembler_MustHaveComponentAnnotation() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().beAnnotatedWith(Component.class)
                .because("Assembler는 Spring Bean으로 등록되어야 합니다 (테스트 용이성)");

            rule.check(classes);
        }

        /**
         * 규칙 7: 클래스명 규칙
         */
        @Test
        @DisplayName("[필수] assembler 패키지의 클래스는 'Assembler' 접미사를 가져야 한다")
        void assembler_MustHaveCorrectSuffix() {
            ArchRule rule = classes()
                .that().resideInAPackage("..application..assembler..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .should().haveSimpleNameEndingWith("Assembler")
                .because("Assembler는 'Assembler' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        /**
         * 규칙 8: 패키지 위치
         */
        @Test
        @DisplayName("[필수] Assembler는 ..application..assembler.. 패키지에 위치해야 한다")
        void assembler_MustBeInCorrectPackage() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().resideInAPackage("..application..assembler..")
                .because("Assembler는 application.*.assembler 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        /**
         * 규칙 13: public 클래스
         */
        @Test
        @DisplayName("[필수] Assembler는 public 클래스여야 한다")
        void assembler_MustBePublic() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().bePublic()
                .because("Assembler는 Spring Bean으로 등록되기 위해 public이어야 합니다");

            rule.check(classes);
        }

        /**
         * 규칙 14: Final 클래스 금지
         */
        @Test
        @DisplayName("[필수] Assembler는 final 클래스가 아니어야 한다")
        void assembler_MustNotBeFinal() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().notBeFinal()
                .because("Spring은 프록시 생성을 위해 Assembler가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        /**
         * 규칙 2: Lombok 절대 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 Lombok 어노테이션을 가지지 않아야 한다")
        void assembler_MustNotUseLombok() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .orShould().beAnnotatedWith("lombok.Value")
                .because("Assembler는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        /**
         * 규칙 3: Static 메서드 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 public static 메서드를 가지지 않아야 한다")
        void assembler_MustNotHavePublicStaticMethods() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .and().areStatic()
                .should().beDeclared()
                .because("Assembler는 Bean으로 등록하여 테스트 용이성을 확보해야 합니다 (Static 메서드 금지)");

            rule.check(classes);
        }

        /**
         * 규칙 11: @Transactional 절대 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 @Transactional을 가지지 않아야 한다")
        void assembler_MustNotHaveTransactionalAnnotation() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("@Transactional은 UseCase에서만 사용해야 합니다 (Assembler는 변환만)");

            rule.check(classes);
        }
    }

    // ==================== 핵심 규칙: toDomain 금지 ====================

    @Nested
    @DisplayName("핵심 규칙: toDomain 메서드 금지")
    class ToDomainProhibitionRules {

        /**
         * 규칙 10: toDomain 메서드 금지 (핵심!)
         *
         * <p>업계 표준:</p>
         * <ul>
         *   <li>Sairyss/domain-driven-hexagon: Mapper에 toDomain 없음</li>
         *   <li>Martin Fowler: "Assembler maps Aggregate to DTO"</li>
         * </ul>
         */
        @Test
        @DisplayName("[금지-핵심] Assembler는 toDomain 메서드를 가지지 않아야 한다")
        void assembler_MustNotHaveToDomainMethod() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .and().haveNameMatching("toDomain.*")
                .should().beDeclared()
                .because("Assembler는 Domain → Response 변환만 담당합니다. " +
                         "Command → Domain 변환은 Creator 패턴을 사용하세요. " +
                         "(업계 표준: domain-driven-hexagon, Martin Fowler PoEAA)");

            rule.check(classes);
        }

        /**
         * 규칙 9: 메서드명 규칙 (toResponse* 만 허용)
         */
        @Test
        @DisplayName("[필수] Assembler 메서드명은 toResponse 또는 toResponseList로 시작해야 한다")
        void assembler_MethodsMustStartWithToResponse() {
            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .and().doNotHaveFullName(".*<init>.*")  // 생성자 제외
                .should().haveNameMatching("toResponse.*")
                .because("Assembler는 Domain → Response 변환만 담당합니다. " +
                         "toResponse(), toResponseList() 형태로 작성하세요. " +
                         "toDomain(), toCriteria() 등은 금지됩니다.");

            rule.check(classes);
        }

        /**
         * toCriteria 메서드 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 toCriteria 메서드를 가지지 않아야 한다")
        void assembler_MustNotHaveToCriteriaMethod() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .and().haveNameMatching("toCriteria.*")
                .should().beDeclared()
                .because("Assembler는 Domain → Response 변환만 담당합니다. " +
                         "Query 조건 변환은 UseCase에서 직접 처리하세요.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        /**
         * 규칙 4: Port 의존성 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 Port 인터페이스를 의존하지 않아야 한다")
        void assembler_MustNotDependOnPorts() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().dependOnClassesThat().haveNameMatching(".*Port")
                .because("Assembler는 Port를 주입받지 않아야 합니다. " +
                         "DB 조회가 필요하면 Creator 패턴을 사용하세요.");

            rule.check(classes);
        }

        /**
         * 규칙 5: Repository 의존성 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 Repository를 의존하지 않아야 한다")
        void assembler_MustNotDependOnRepositories() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("Assembler는 Repository를 주입받지 않아야 합니다. " +
                         "DB 조회가 필요하면 Creator 패턴을 사용하세요.");

            rule.check(classes);
        }

        /**
         * 규칙 6: Spring Data 의존성 금지 (Page, Slice)
         */
        @Test
        @DisplayName("[금지] Assembler는 Spring Data Page/Slice를 사용하지 않아야 한다")
        void assembler_MustNotUseSpringDataPageable() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Page")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Slice")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Pageable")
                .because("Assembler는 Spring Data 대신 custom PageResponse를 사용해야 합니다. " +
                         "PageResponse 조립은 UseCase에서 처리하세요.");

            rule.check(classes);
        }

        /**
         * 규칙 16: Application Layer와 Domain Layer만 의존
         */
        @Test
        @DisplayName("[필수] Assembler는 Application Layer와 Domain Layer만 의존해야 한다")
        void assembler_MustOnlyDependOnApplicationAndDomainLayers() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Assembler")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.company.application..",
                    "com.company.domain..",
                    "org.springframework..",
                    "java..",
                    "jakarta.."
                )
                .because("Assembler는 Application Layer와 Domain Layer만 의존해야 합니다. " +
                         "Persistence Layer, API Layer 의존 금지.");

            rule.check(classes);
        }
    }

    // ==================== 비즈니스 로직 금지 규칙 ====================

    @Nested
    @DisplayName("비즈니스 로직 금지 규칙")
    class BusinessLogicProhibitionRules {

        /**
         * 규칙 11: 비즈니스 메서드 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 비즈니스 메서드를 가지지 않아야 한다")
        void assembler_MustNotHaveBusinessMethods() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .and().haveNameMatching("validate.*|place.*|confirm.*|cancel.*|approve.*|" +
                                        "reject.*|modify.*|change.*|update.*|delete.*|" +
                                        "save.*|persist.*|create.*|build.*")
                .should().beDeclared()
                .because("Assembler는 비즈니스 로직을 가질 수 없습니다. " +
                         "비즈니스 로직은 Domain 또는 UseCase에서 처리하세요.");

            rule.check(classes);
        }

        /**
         * 규칙 12: PageResponse/SliceResponse 반환 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 PageResponse/SliceResponse를 반환하지 않아야 한다")
        void assembler_MustNotReturnPageOrSliceResponse() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .should().haveRawReturnType(".*PageResponse")
                .orShould().haveRawReturnType(".*SliceResponse")
                .because("PageResponse/SliceResponse 조립은 UseCase에서 처리해야 합니다. " +
                         "Assembler는 List<XxxResponse> 변환만 담당합니다.");

            rule.check(classes);
        }

        /**
         * 규칙 18: 계산 로직 금지
         */
        @Test
        @DisplayName("[금지] Assembler는 계산 로직 메서드를 가지지 않아야 한다")
        void assembler_MustNotHaveCalculationLogic() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().arePublic()
                .should().haveNameMatching("calculate.*|compute.*|sum.*|multiply.*|" +
                                           "divide.*|add.*|subtract.*")
                .because("Assembler는 계산 로직을 가질 수 없습니다. " +
                         "계산은 Domain에서 처리하세요 (예: order.totalAmount())");

            rule.check(classes);
        }
    }

    // ==================== 필드 규칙 ====================

    @Nested
    @DisplayName("필드 규칙")
    class FieldRules {

        /**
         * 규칙 15: 필드가 있다면 final이어야 함
         */
        @Test
        @DisplayName("[권장] Assembler 필드는 final이어야 한다")
        void assembler_FieldsShouldBeFinal() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().areNotStatic()
                .should().beFinal()
                .because("Assembler는 불변성을 위해 생성자 주입을 사용해야 합니다 (final 필드)");

            rule.check(classes);
        }

        /**
         * 규칙 17: 필드명 규칙
         */
        @Test
        @DisplayName("[권장] Assembler의 필드명은 소문자로 시작해야 한다")
        void assembler_FieldsShouldStartWithLowercase() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Assembler")
                .and().areNotStatic()
                .should().haveNameMatching("[a-z].*")
                .because("필드명은 camelCase 규칙을 따라야 합니다");

            rule.check(classes);
        }
    }
}
```

---

## 4) 실행 방법

### Gradle
```bash
# Assembler ArchUnit 테스트만 실행
./gradlew test --tests AssemblerArchTest

# 전체 아키텍처 테스트
./gradlew test --tests '*ArchTest'

# 특정 규칙만 실행
./gradlew test --tests 'AssemblerArchTest$ToDomainProhibitionRules'
```

### Maven
```bash
# Assembler ArchUnit 테스트만 실행
mvn test -Dtest=AssemblerArchTest

# 전체 아키텍처 테스트
mvn test -Dtest=*ArchTest
```

---

## 5) 검증 결과 예시

### ✅ 성공
```
AssemblerArchTest > BasicStructureRules > assembler_MustHaveComponentAnnotation() PASSED
AssemblerArchTest > ProhibitionRules > assembler_MustNotUseLombok() PASSED
AssemblerArchTest > ToDomainProhibitionRules > assembler_MustNotHaveToDomainMethod() PASSED
AssemblerArchTest > ToDomainProhibitionRules > assembler_MethodsMustStartWithToResponse() PASSED
...
19 tests passed
```

### ❌ 실패 예시 1: toDomain 메서드 사용 (핵심 위반!)

```
assembler_MustNotHaveToDomainMethod() FAILED
    Rule: no methods should have name matching 'toDomain.*'
    Violation: Method <OrderAssembler.toDomain(PlaceOrderCommand)> in (OrderAssembler.java:35)

    Reason: Assembler는 Domain → Response 변환만 담당합니다.
            Command → Domain 변환은 Creator 패턴을 사용하세요.

➡️ 해결방법:
   1. toDomain() 메서드 삭제
   2. OrderCreator 클래스 생성
   3. OrderCreator.create(PlaceOrderCommand) 메서드 구현
```

### ❌ 실패 예시 2: 메서드명 규칙 위반

```
assembler_MethodsMustStartWithToResponse() FAILED
    Rule: methods should have name matching 'toResponse.*'
    Violation: Method <OrderAssembler.convert(Order)> in (OrderAssembler.java:20)

    Reason: Assembler는 Domain → Response 변환만 담당합니다.
            toResponse(), toResponseList() 형태로 작성하세요.

➡️ 해결: convert() → toResponse() 로 이름 변경
```

### ❌ 실패 예시 3: Port 의존성 주입

```
assembler_MustNotDependOnPorts() FAILED
    Rule: should not depend on classes matching '.*Port'
    Violation: Class <OrderAssembler> depends on <LoadOrderPort>

    Reason: Assembler는 Port를 주입받지 않아야 합니다.
            DB 조회가 필요하면 Creator 패턴을 사용하세요.

➡️ 해결:
   1. Assembler에서 Port 의존성 제거
   2. DB 조회가 필요한 변환 로직은 Creator로 이동
   3. Creator는 Port 주입 허용
```

### ❌ 실패 예시 4: PageResponse 반환

```
assembler_MustNotReturnPageOrSliceResponse() FAILED
    Rule: no methods should return type '.*PageResponse'
    Violation: Method <OrderAssembler.toPageResponse(List, long)> returns PageResponse

    Reason: PageResponse/SliceResponse 조립은 UseCase에서 처리해야 합니다.
            Assembler는 List<XxxResponse> 변환만 담당합니다.

➡️ 해결:
   1. toPageResponse() 메서드 삭제
   2. UseCase에서 PageResponse 조립:
      List<OrderResponse> content = assembler.toResponseList(orders);
      return PageResponse.of(content, totalElements, pageRequest);
```

### ❌ 실패 예시 5: Lombok 사용

```
assembler_MustNotUseLombok() FAILED
    Rule: no classes should be annotated with @RequiredArgsConstructor
    Violation: Class <OrderAssembler> is annotated with @RequiredArgsConstructor

    Reason: Assembler는 Plain Java를 사용해야 합니다 (Lombok 금지).

➡️ 해결:
   @RequiredArgsConstructor 삭제 후 직접 생성자 작성:

   public OrderAssembler() {
       // 기본 생성자 또는 의존성 주입 생성자
   }
```

---

## 6) CI/CD 통합

### GitHub Actions

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
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
          restore-keys: ${{ runner.os }}-gradle-

      - name: Run ArchUnit Tests (Zero-Tolerance)
        run: ./gradlew test --tests '*ArchTest' --info

      - name: Fail on Architecture Violation
        if: failure()
        run: |
          echo "❌ Architecture violation detected!"
          echo ""
          echo "Assembler 규칙 위반 가능성:"
          echo "  - toDomain() 메서드 사용 금지"
          echo "  - toResponse*() 메서드만 허용"
          echo "  - Port/Repository 의존성 금지"
          echo ""
          echo "자세한 내용은 테스트 결과를 확인하세요."
          exit 1
```

---

## 7) 체크리스트 (19개 규칙)

ArchUnit 테스트 완료 확인:

### 기본 구조 (5개)
- [ ] ✅ @Component 필수
- [ ] ✅ 클래스명: *Assembler
- [ ] ✅ 패키지 위치: ..application..assembler..
- [ ] ✅ public 클래스
- [ ] ✅ final 클래스 금지

### 금지 규칙 (3개)
- [ ] ❌ Lombok 절대 금지
- [ ] ❌ Static 메서드 금지
- [ ] ❌ @Transactional 금지

### 핵심: toDomain 금지 (3개)
- [ ] ❌ **toDomain 메서드 금지** (핵심!)
- [ ] ✅ 메서드명: toResponse* 만 허용
- [ ] ❌ toCriteria 메서드 금지

### 의존성 규칙 (4개)
- [ ] ❌ Port 의존성 금지
- [ ] ❌ Repository 의존성 금지
- [ ] ❌ Spring Data Page/Slice 금지
- [ ] ✅ Application/Domain Layer만 의존

### 비즈니스 로직 금지 (3개)
- [ ] ❌ 비즈니스 메서드 금지
- [ ] ❌ PageResponse/SliceResponse 반환 금지
- [ ] ❌ 계산 로직 금지

### 필드 규칙 (2개)
- [ ] ✅ 필드는 final (권장)
- [ ] ✅ 필드명 소문자 시작 (권장)

---

## 8) Assembler vs Creator 비교

| 구분 | Assembler | Creator |
|------|-----------|---------|
| **책임** | Domain → Response 변환 | Command → Domain 생성 |
| **메서드** | `toResponse()`, `toResponseList()` | `create()`, `createWithValidation()` |
| **Port 의존** | ❌ 금지 | ✅ 허용 (DB 조회 가능) |
| **DB 접근** | ❌ 금지 | ✅ 허용 (중복 체크 등) |
| **복잡한 로직** | ❌ 금지 (단순 매핑) | ✅ 허용 (유효성 검증) |

### Creator 패턴 예시

```java
@Component
public class OrderCreator {

    private final LoadProductPort loadProductPort;
    private final LoadMemberPort loadMemberPort;

    public OrderCreator(LoadProductPort loadProductPort, LoadMemberPort loadMemberPort) {
        this.loadProductPort = loadProductPort;
        this.loadMemberPort = loadMemberPort;
    }

    /**
     * Command → Domain 변환 (DB 조회 포함)
     */
    public Order create(PlaceOrderCommand command, Clock clock) {
        // 1. DB 조회
        Member member = loadMemberPort.loadById(command.memberId())
            .orElseThrow(() -> new MemberNotFoundException(command.memberId()));

        List<Product> products = loadProductPort.loadByIds(command.productIds());

        // 2. Domain 생성 (정적 팩토리 메서드 사용)
        return Order.forNew(
            member.id(),
            createLineItems(command, products),
            clock
        );
    }

    private List<OrderLineItem> createLineItems(PlaceOrderCommand command, List<Product> products) {
        // 복잡한 변환 로직...
    }
}
```

---

## 9) 관련 문서

- **[Assembler Guide](assembler-guide.md)** - Assembler 구현 가이드
- **[Assembler Test Guide](assembler-test-guide.md)** - Assembler 테스트 가이드
- **[Creator Pattern Guide](../creator/creator-guide.md)** - Creator 패턴 가이드 (작성 예정)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0 (Domain → Response 변환 전용)
