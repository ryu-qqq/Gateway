# Aggregate Root ArchUnit 검증 규칙

> **목적**: Aggregate Root 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성
>
> **참조**: 상세 설계 원칙은 [Aggregate Guide](aggregate-guide.md) 참조

---

## 1) 검증 항목 요약

### Aggregate Root 규칙 (20개)

| # | 규칙 | 유형 |
|---|------|------|
| 1 | Lombok 어노테이션 금지 | ❌ 금지 |
| 2 | JPA 어노테이션 금지 | ❌ 금지 |
| 3 | Spring 어노테이션 금지 | ❌ 금지 |
| 4 | Setter 메서드 금지 | ❌ 금지 |
| 5 | 생성자 private 필수 | ✅ 필수 |
| 6 | forNew() 메서드 필수 | ✅ 필수 |
| 7 | of() 메서드 필수 | ✅ 필수 |
| 8 | reconstitute() 메서드 필수 | ✅ 필수 |
| 9 | ID 필드 final 필수 | ✅ 필수 |
| 10 | Clock 필드 필수 | ✅ 필수 |
| 11 | 외래키 VO 타입 필수 | ❌ 금지 (원시 타입) |
| 12 | 패키지 위치 규칙 | ✅ 필수 |
| 13 | public 클래스 | ✅ 필수 |
| 14 | final 클래스 금지 | ❌ 금지 |
| 15 | 비즈니스 메서드 명명 규칙 | ✅ 권장 |
| 16 | 외부 레이어 의존 금지 | ❌ 금지 |
| 17 | createdAt 필드 (Instant, final) | ✅ 필수 |
| 18 | updatedAt 필드 (Instant, non-final) | ✅ 필수 |

### TestFixture 패턴 규칙 (4개)

| # | 규칙 | 유형 |
|---|------|------|
| 19 | forNew() 메서드 필수 | ✅ 필수 |
| 20 | of() 메서드 필수 | ✅ 필수 |
| 21 | reconstitute() 메서드 필수 | ✅ 필수 |
| 22 | create*() 메서드 금지 | ❌ 금지 |

---

## 2) 의존성 추가

```groovy
// build.gradle
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```

---

## 3) ArchUnit 테스트 코드

```java
package com.company.architecture;

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

import java.time.Clock;
import java.time.Instant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * AggregateRoot ArchUnit 검증 테스트 (Zero-Tolerance)
 */
@DisplayName("AggregateRoot ArchUnit Tests")
@Tag("architecture")
class AggregateRootArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.domain");
    }

    // ==================== 금지 규칙 (1-4) ====================

    @Test
    @DisplayName("[금지] Lombok 어노테이션 사용 금지")
    void aggregateRoot_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Pure Java 원칙");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] JPA 어노테이션 사용 금지")
    void aggregateRoot_MustNotUseJPA() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table")
            .orShould().beAnnotatedWith("jakarta.persistence.Column")
            .orShould().beAnnotatedWith("jakarta.persistence.Id")
            .orShould().beAnnotatedWith("jakarta.persistence.ManyToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToMany")
            .because("Domain Layer는 JPA에 독립적");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] Spring 어노테이션 사용 금지")
    void aggregateRoot_MustNotUseSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
            .because("Domain Layer는 Spring에 독립적");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] Setter 메서드 금지")
    void aggregateRoot_MustNotHaveSetterMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().arePublic()
            .and().haveNameMatching("set[A-Z].*")
            .should().beDeclared()
            .because("비즈니스 메서드로 상태 변경");

        rule.check(classes);
    }

    // ==================== 필수 규칙 (5-10) ====================

    @Test
    @DisplayName("[필수] 생성자는 private")
    void aggregateRoot_ConstructorMustBePrivate() {
        ArchRule rule = constructors()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().areDeclaredInClassesThat().areNotInterfaces()
            .and().areDeclaredInClassesThat().areNotEnums()
            .should().bePrivate()
            .because("정적 팩토리 메서드(forNew, of, reconstitute)로만 생성");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] forNew() 정적 팩토리 메서드")
    void aggregateRoot_MustHaveForNewMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should(haveStaticMethodWithName("forNew"))
            .because("신규 생성용 팩토리 메서드 필수");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] of() 정적 팩토리 메서드")
    void aggregateRoot_MustHaveOfMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should(haveStaticMethodWithName("of"))
            .because("ID 기반 생성용 팩토리 메서드 필수");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] reconstitute() 정적 팩토리 메서드")
    void aggregateRoot_MustHaveReconstituteMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should(haveStaticMethodWithName("reconstitute"))
            .because("영속성 복원용 팩토리 메서드 필수");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] ID 필드는 final")
    void aggregateRoot_IdFieldMustBeFinal() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("id")
            .should().beFinal()
            .because("ID는 불변");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] Clock 필드 필수")
    void aggregateRoot_MustHaveClockField() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().dependOnClassesThat().areAssignableTo(Clock.class)
            .because("테스트 가능성을 위해 Clock 주입 필수");

        rule.check(classes);
    }

    // ==================== 타입 규칙 (11-14) ====================

    @Test
    @DisplayName("[금지] 외래키는 VO 타입 (원시 타입 금지)")
    void aggregateRoot_ForeignKeyMustBeValueObject() {
        ArchRule rule = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching(".*[Ii]d")
            .and().doNotHaveName("id")
            .should().haveRawType(Long.class)
            .orShould().haveRawType(String.class)
            .orShould().haveRawType(Integer.class)
            .because("외래키는 VO 사용 (Long paymentId ❌ → PaymentId paymentId ✅)");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] 패키지 위치: domain.[bc].aggregate.[name]")
    void aggregateRoot_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .and().haveSimpleNameNotEndingWith("Id")
            .and().haveSimpleNameNotEndingWith("Event")
            .and().haveSimpleNameNotEndingWith("Exception")
            .and().haveSimpleNameNotEndingWith("Status")
            .should().resideInAPackage("..domain..aggregate..")
            .because("Aggregate는 aggregate 패키지에 위치");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] public 클래스")
    void aggregateRoot_MustBePublic() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().bePublic()
            .because("다른 레이어에서 사용하기 위해 public 필수");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] final 클래스 금지")
    void aggregateRoot_ShouldNotBeFinal() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().notBeFinal()
            .because("확장 가능성을 위해 final 금지");

        rule.check(classes);
    }

    // ==================== 비즈니스 규칙 (15-16) ====================

    @Test
    @DisplayName("[권장] 비즈니스 메서드는 명확한 동사")
    void aggregateRoot_BusinessMethodsShouldHaveExplicitVerbs() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().arePublic()
            .and().areNotStatic()
            .and().haveNameNotMatching(".*<init>.*")
            .and().haveNameNotMatching("(id|status|createdAt|updatedAt|pullDomainEvents).*")
            .and().haveNameNotMatching("(is|has|can).*")
            .should().haveNameMatching("(add|remove|confirm|cancel|approve|reject|ship|deliver|complete|fail|update|change|place|validate|calculate|transfer|process|register).*")
            .because("비즈니스 메서드는 명확한 동사로 시작");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] Application/Adapter 레이어 의존 금지")
    void aggregateRoot_MustNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application..",
                "..adapter.."
            )
            .because("헥사고날 아키텍처: Domain은 외부 레이어에 의존 금지");

        rule.check(classes);
    }

    // ==================== 시간 필드 규칙 (17-18) ====================

    @Test
    @DisplayName("[필수] createdAt 필드 (Instant 타입, final)")
    void aggregateRoot_CreatedAtMustBeInstantAndFinal() {
        ArchRule typeRule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("createdAt")
            .should().haveRawType(Instant.class)
            .because("시간 필드는 Instant 사용 (LocalDateTime 금지)");

        ArchRule finalRule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("createdAt")
            .should().beFinal()
            .because("createdAt은 불변");

        typeRule.check(classes);
        finalRule.check(classes);
    }

    @Test
    @DisplayName("[필수] updatedAt 필드 (Instant 타입, non-final)")
    void aggregateRoot_UpdatedAtMustBeInstantAndNotFinal() {
        ArchRule typeRule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("updatedAt")
            .should().haveRawType(Instant.class)
            .because("시간 필드는 Instant 사용 (LocalDateTime 금지)");

        ArchRule notFinalRule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("updatedAt")
            .should().notBeFinal()
            .because("updatedAt은 상태 변경 시 갱신");

        typeRule.check(classes);
        notFinalRule.check(classes);
    }

    // ==================== TestFixture 규칙 (19-22) ====================

    @Test
    @DisplayName("[필수] TestFixture는 forNew() 메서드 필수")
    void fixtureClassesShouldHaveForNewMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("forNew"))
            .because("Fixture는 Aggregate와 동일한 패턴(forNew, of, reconstitute)");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] TestFixture는 of() 메서드 필수")
    void fixtureClassesShouldHaveOfMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("of"))
            .because("Fixture는 Aggregate와 동일한 패턴");

        rule.check(classes);
    }

    @Test
    @DisplayName("[필수] TestFixture는 reconstitute() 메서드 필수")
    void fixtureClassesShouldHaveReconstituteMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("reconstitute"))
            .because("Fixture는 Aggregate와 동일한 패턴");

        rule.check(classes);
    }

    @Test
    @DisplayName("[금지] TestFixture는 create*() 메서드 금지")
    void fixtureClassesShouldNotHaveCreateMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(notHaveMethodsWithNameStartingWith("create"))
            .because("create*() 대신 forNew(), of(), reconstitute() 사용");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition ====================

    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<>("have public static method: " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals(methodName)
                        && method.getModifiers().contains(JavaModifier.STATIC)
                        && method.getModifiers().contains(JavaModifier.PUBLIC));

                if (!hasMethod) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                        String.format("%s에 public static %s() 메서드 없음",
                            javaClass.getName(), methodName)));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> notHaveMethodsWithNameStartingWith(String prefix) {
        return new ArchCondition<>("not have methods starting with: " + prefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllMethods().stream()
                    .filter(method -> method.getName().startsWith(prefix))
                    .forEach(method -> events.add(SimpleConditionEvent.violated(javaClass,
                        String.format("%s에 금지된 메서드 %s() 존재",
                            javaClass.getName(), method.getName()))));
            }
        };
    }
}
```

---

## 4) 실행 방법

```bash
# ArchUnit 테스트 실행
./gradlew test --tests '*ArchTest'

# 특정 테스트만
./gradlew test --tests 'AggregateRootArchTest'
```

---

## 5) 실패 예시

```
❌ aggregateRoot_CreatedAtMustBeInstantAndFinal() FAILED
   Field <Order.createdAt> has raw type LocalDateTime (expected: Instant)
   → 해결: Instant createdAt으로 변경

❌ aggregateRoot_ForeignKeyMustBeValueObject() FAILED
   Field <Order.paymentId> has raw type Long
   → 해결: PaymentId paymentId로 변경
```

---

## 📖 관련 문서

- **[Aggregate Guide](aggregate-guide.md)** - Aggregate Root 설계 원칙
- **[Aggregate Test Guide](aggregate-test-guide.md)** - 테스트 작성 가이드
