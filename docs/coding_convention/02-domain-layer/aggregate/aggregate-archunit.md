# AggregateRoot ArchUnit 검증 규칙

> **목적**: Aggregate Root 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Aggregate Root 검증 규칙
1. ✅ **생성자 private 필수**
2. ✅ **정적 팩토리 메서드 3종** (forNew, of, reconstitute)
3. ✅ **ID 필드 final 필수**
4. ✅ **Setter 메서드 절대 금지**
5. ❌ **Lombok 어노테이션 절대 금지** (@Data, @Builder, @Getter, @Setter, @AllArgsConstructor 등)
6. ❌ **JPA 어노테이션 절대 금지** (@Entity, @Table, @Column 등)
7. ❌ **Spring 어노테이션 절대 금지** (@Component, @Service 등)
8. ✅ **Clock 필드 필수** (테스트 가능성)
9. ✅ **외래키는 VO 타입** (Long, String 같은 원시 타입 금지)
10. ✅ **패키지 위치**: domain.[bc].aggregate.[name]
11. ✅ **updatedAt 갱신 필수** (상태 변경 시 Clock 사용)
12. ✅ **비즈니스 메서드 명명 규칙**

### TestFixture 패턴 검증 규칙 (NEW ⭐)
13. ✅ **TestFixture forNew() 메서드 필수** - Aggregate와 동일 패턴
14. ✅ **TestFixture of() 메서드 필수** - Aggregate와 동일 패턴
15. ✅ **TestFixture reconstitute() 메서드 필수** - Aggregate와 동일 패턴
16. ❌ **TestFixture create*() 메서드 금지** - 일관성 없는 패턴 차단

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

import java.time.Clock;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * AggregateRoot ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Aggregate Root는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AggregateRoot ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class AggregateRootArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.domain");
    }

    /**
     * 규칙 1: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Aggregate Root는 Lombok 어노테이션을 가지지 않아야 한다")
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
            .orShould().beAnnotatedWith("lombok.ToString")
            .orShould().beAnnotatedWith("lombok.EqualsAndHashCode")
            .because("Aggregate Root는 Pure Java로 작성해야 합니다 (Lombok 절대 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 2: JPA 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Aggregate Root는 JPA 어노테이션을 가지지 않아야 한다")
    void aggregateRoot_MustNotUseJPA() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table")
            .orShould().beAnnotatedWith("jakarta.persistence.Column")
            .orShould().beAnnotatedWith("jakarta.persistence.Id")
            .orShould().beAnnotatedWith("jakarta.persistence.GeneratedValue")
            .orShould().beAnnotatedWith("jakarta.persistence.ManyToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToMany")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.ManyToMany")
            .because("Aggregate Root는 JPA에 독립적이어야 합니다 (JPA 어노테이션 절대 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 3: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Aggregate Root는 Spring 어노테이션을 가지지 않아야 한다")
    void aggregateRoot_MustNotUseSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Bean")
            .because("Aggregate Root는 Spring에 독립적이어야 합니다 (Spring 어노테이션 절대 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 4: Setter 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] Aggregate Root는 Setter 메서드를 가지지 않아야 한다")
    void aggregateRoot_MustNotHaveSetterMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().arePublic()
            .and().haveNameMatching("set[A-Z].*")
            .should().beDeclared()
            .because("Aggregate Root는 불변성을 유지하고 비즈니스 메서드로만 상태를 변경해야 합니다 (Setter 절대 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 5: 생성자는 private 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root의 생성자는 private이어야 한다")
    void aggregateRoot_ConstructorMustBePrivate() {
        ArchRule rule = constructors()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().areDeclaredInClassesThat().areNotInterfaces()
            .and().areDeclaredInClassesThat().areNotEnums()
            .should().bePrivate()
            .because("Aggregate Root는 정적 팩토리 메서드(forNew, of, reconstitute)로만 생성해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: forNew() 정적 팩토리 메서드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 forNew() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveForNewMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().areDeclaredInClassesThat().areNotInterfaces()
            .and().areDeclaredInClassesThat().areNotEnums()
            .and().areStatic()
            .and().arePublic()
            .and().haveName("forNew")
            .should().existFor(".*")
            .because("Aggregate Root는 신규 생성을 위한 forNew() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: of() 정적 팩토리 메서드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 of() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveOfMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().areDeclaredInClassesThat().areNotInterfaces()
            .and().areDeclaredInClassesThat().areNotEnums()
            .and().areStatic()
            .and().arePublic()
            .and().haveName("of")
            .should().existFor(".*")
            .because("Aggregate Root는 기존 값으로 생성을 위한 of() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: reconstitute() 정적 팩토리 메서드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 reconstitute() 정적 팩토리 메서드를 가져야 한다")
    void aggregateRoot_MustHaveReconstituteMethod() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().areDeclaredInClassesThat().areNotInterfaces()
            .and().areDeclaredInClassesThat().areNotEnums()
            .and().areStatic()
            .and().arePublic()
            .and().haveName("reconstitute")
            .should().existFor(".*")
            .because("Aggregate Root는 영속성 복원을 위한 reconstitute() 메서드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: ID 필드는 final 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root의 ID 필드는 final이어야 한다")
    void aggregateRoot_IdFieldMustBeFinal() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("id")
            .should().beFinal()
            .because("Aggregate Root의 ID는 불변이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Clock 필드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 Clock 타입 필드를 가져야 한다")
    void aggregateRoot_MustHaveClockField() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().dependOnClassesThat().areAssignableTo(Clock.class)
            .because("Aggregate Root는 테스트 가능성을 위해 Clock을 사용해야 합니다 (LocalDateTime.now(clock))");

        rule.check(classes);
    }

    /**
     * 규칙 11: 외래키는 VO 타입 사용 (원시 타입 금지)
     */
    @Test
    @DisplayName("[금지] Aggregate Root는 외래키로 Long/String 같은 원시 타입을 사용하지 않아야 한다")
    void aggregateRoot_ForeignKeyMustBeValueObject() {
        ArchRule rule = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching(".*[Ii]d")
            .and().doNotHaveName("id")  // 자신의 ID는 제외
            .should().haveRawType(Long.class)
            .orShould().haveRawType(String.class)
            .orShould().haveRawType(Integer.class)
            .because("외래키는 VO를 사용해야 합니다 (Long paymentId ❌, PaymentId paymentId ✅)");

        rule.check(classes);
    }

    /**
     * 규칙 12: 패키지 위치
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 domain.[bc].aggregate.[name] 패키지에 위치해야 한다")
    void aggregateRoot_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .and().haveSimpleNameNotEndingWith("Id")
            .and().haveSimpleNameNotEndingWith("Event")
            .and().haveSimpleNameNotEndingWith("Exception")
            .and().haveSimpleNameNotEndingWith("Status")
            .and().resideInAPackage("..domain..aggregate..")
            .should().resideInAPackage("..domain..aggregate..")
            .because("Aggregate Root는 domain.[bc].aggregate.[name] 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: Public 클래스
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 public 클래스여야 한다")
    void aggregateRoot_MustBePublic() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().bePublic()
            .because("Aggregate Root는 다른 레이어에서 사용되기 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: Final 클래스 금지 (상속 가능성)
     */
    @Test
    @DisplayName("[권장] Aggregate Root는 final 클래스가 아니어야 한다")
    void aggregateRoot_ShouldNotBeFinal() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..aggregate..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().notBeFinal()
            .because("Aggregate Root는 확장 가능성을 위해 final이 아니어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 15: 비즈니스 메서드 명명 규칙 (명확한 동사)
     */
    @Test
    @DisplayName("[권장] Aggregate Root의 비즈니스 메서드는 명확한 동사로 시작해야 한다")
    void aggregateRoot_BusinessMethodsShouldHaveExplicitVerbs() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().arePublic()
            .and().doNotHaveFullName(".*<init>.*")
            .and().doNotHaveName("get.*")
            .and().doNotHaveName("is.*")
            .and().doNotHaveName("has.*")
            .and().areNotStatic()
            .should().haveNameMatching("(add|remove|confirm|cancel|approve|reject|ship|deliver|complete|fail|update|change|place|validate|calculate|transfer|process).*")
            .because("비즈니스 메서드는 명확한 동사로 시작해야 합니다 (confirm, cancel, approve 등)");

        rule.check(classes);
    }

    /**
     * 규칙 16: Domain Layer는 외부 레이어에 의존하지 않음
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 Application/Adapter 레이어에 의존하지 않아야 한다")
    void aggregateRoot_MustNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..aggregate..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application..",
                "..adapter.."
            )
            .because("Domain Layer는 Application/Adapter 레이어에 의존하지 않아야 합니다 (헥사고날 아키텍처)");

        rule.check(classes);
    }

    /**
     * 규칙 17: createdAt 필드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 createdAt 필드를 가져야 한다")
    void aggregateRoot_MustHaveCreatedAtField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("createdAt")
            .should().haveRawType("java.time.LocalDateTime")
            .because("Aggregate Root는 생성 시각 추적을 위해 createdAt 필드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 18: updatedAt 필드 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root는 updatedAt 필드를 가져야 한다")
    void aggregateRoot_MustHaveUpdatedAtField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("updatedAt")
            .should().haveRawType("java.time.LocalDateTime")
            .because("Aggregate Root는 수정 시각 추적을 위해 updatedAt 필드가 필요합니다");

        rule.check(classes);
    }

    /**
     * 규칙 19: createdAt 필드는 final 필수
     */
    @Test
    @DisplayName("[필수] Aggregate Root의 createdAt 필드는 final이어야 한다")
    void aggregateRoot_CreatedAtFieldMustBeFinal() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("createdAt")
            .should().beFinal()
            .because("createdAt은 생성 후 변경되지 않으므로 final이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 20: updatedAt 필드는 final 금지 (변경 가능)
     */
    @Test
    @DisplayName("[필수] Aggregate Root의 updatedAt 필드는 final이 아니어야 한다")
    void aggregateRoot_UpdatedAtFieldMustNotBeFinal() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..aggregate..")
            .and().haveNameMatching("updatedAt")
            .should().notBeFinal()
            .because("updatedAt은 상태 변경 시 갱신되므로 final이 아니어야 합니다");

        rule.check(classes);
    }

    // ==================== TestFixture 패턴 검증 규칙 ====================

    /**
     * 규칙 21: TestFixture는 forNew() 메서드 필수
     */
    @Test
    @DisplayName("[필수] TestFixture는 forNew() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveForNewMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("forNew"))
            .because("Fixture는 Aggregate와 동일한 생성 패턴(forNew, of, reconstitute)을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 22: TestFixture는 of() 메서드 필수
     */
    @Test
    @DisplayName("[필수] TestFixture는 of() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveOfMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("of"))
            .because("Fixture는 Aggregate와 동일한 생성 패턴(forNew, of, reconstitute)을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 23: TestFixture는 reconstitute() 메서드 필수
     */
    @Test
    @DisplayName("[필수] TestFixture는 reconstitute() 메서드를 가져야 한다")
    void fixtureClassesShouldHaveReconstituteMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(haveStaticMethodWithName("reconstitute"))
            .because("Fixture는 Aggregate와 동일한 생성 패턴(forNew, of, reconstitute)을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 24: TestFixture는 create*() 메서드 금지
     */
    @Test
    @DisplayName("[금지] TestFixture는 create*() 메서드를 가지지 않아야 한다")
    void fixtureClassesShouldNotHaveCreateMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Fixture")
            .and().resideInAPackage("..fixture..")
            .should(notHaveMethodsWithNameStartingWith("create"))
            .because("Fixture는 create*() 대신 forNew(), of(), reconstitute()를 사용해야 합니다");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /**
     * 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증
     *
     * @param methodName 검증할 메서드 이름
     * @return ArchCondition
     */
    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have public static method with name " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals(methodName)
                        && method.getModifiers().contains(JavaModifier.STATIC)
                        && method.getModifiers().contains(JavaModifier.PUBLIC));

                if (!hasMethod) {
                    String message = String.format(
                        "Class %s does not have a public static method named '%s'",
                        javaClass.getName(), methodName
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 클래스가 특정 접두사로 시작하는 메서드를 가지지 않는지 검증
     *
     * @param prefix 금지할 메서드 이름 접두사
     * @return ArchCondition
     */
    private static ArchCondition<JavaClass> notHaveMethodsWithNameStartingWith(String prefix) {
        return new ArchCondition<JavaClass>("not have methods with name starting with " + prefix) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getAllMethods().stream()
                    .filter(method -> method.getName().startsWith(prefix))
                    .forEach(method -> {
                        String message = String.format(
                            "Class %s has method %s starting with '%s' which is prohibited",
                            javaClass.getName(), method.getName(), prefix
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    });
            }
        };
    }
}
```

**참고**: 위 코드에 필요한 import 문 추가:
```java
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
```

---

## 4️⃣ 실행 방법

### Maven
```bash
# ArchUnit 테스트만 실행
mvn test -Dtest=AggregateRootArchTest

# 전체 아키텍처 테스트
mvn test -Dtest=*ArchTest
```

### Gradle
```bash
# ArchUnit 테스트만 실행
./gradlew test --tests AggregateRootArchTest

# 전체 아키텍처 테스트
./gradlew test --tests '*ArchTest'
```

---

## 5️⃣ 검증 결과 예시

### ✅ 성공
```
AggregateRootArchTest > aggregateRoot_MustNotUseLombok() PASSED
AggregateRootArchTest > aggregateRoot_MustNotUseJPA() PASSED
AggregateRootArchTest > aggregateRoot_MustNotHaveSetterMethods() PASSED
AggregateRootArchTest > aggregateRoot_ConstructorMustBePrivate() PASSED
AggregateRootArchTest > aggregateRoot_MustHaveForNewMethod() PASSED
...
20 tests passed
```

### ❌ 실패 예시 1: Lombok 사용
```
aggregateRoot_MustNotUseLombok() FAILED
    Rule: no classes should be annotated with @Data
    Violation: Class <Order> is annotated with @Data in (Order.java:10)

➡️ 해결: @Data 제거 → Pure Java getter/setter 작성
```

### ❌ 실패 예시 2: Setter 메서드 존재
```
aggregateRoot_MustNotHaveSetterMethods() FAILED
    Rule: no methods should have name matching 'set[A-Z].*'
    Violation: Method <Order.setStatus(OrderStatus)> in (Order.java:45)

➡️ 해결: setStatus() 제거 → confirm(), cancel() 같은 비즈니스 메서드로 대체
```

### ❌ 실패 예시 3: 생성자 public
```
aggregateRoot_ConstructorMustBePrivate() FAILED
    Rule: constructors should be private
    Violation: Constructor <Order(OrderId, CustomerId, ...)> is public in (Order.java:20)

➡️ 해결: 생성자를 private으로 변경 → 정적 팩토리 메서드 사용
```

### ❌ 실패 예시 4: 외래키 원시 타입 사용
```
aggregateRoot_ForeignKeyMustBeValueObject() FAILED
    Rule: no fields should have raw type Long
    Violation: Field <Order.paymentId> has raw type Long in (Order.java:15)

➡️ 해결: Long paymentId → PaymentId paymentId (VO 사용)
```

### ❌ 실패 예시 5: JPA 어노테이션 사용
```
aggregateRoot_MustNotUseJPA() FAILED
    Rule: no classes should be annotated with @Entity
    Violation: Class <Order> is annotated with @Entity in (Order.java:10)

➡️ 해결: @Entity 제거 → JPA Entity는 Persistence Layer의 별도 클래스
```

### ❌ 실패 예시 6: TestFixture forNew() 메서드 없음 (NEW ⭐)
```
fixtureClassesShouldHaveForNewMethod() FAILED
    Rule: classes should have public static method with name forNew
    Violation: Class <OrderFixture> does not have a public static method named 'forNew' in (OrderFixture.java:10)

➡️ 해결: public static Order forNew() 메서드 추가 → Aggregate와 동일 패턴
```

### ❌ 실패 예시 7: TestFixture create*() 메서드 사용 (NEW ⭐)
```
fixtureClassesShouldNotHaveCreateMethod() FAILED
    Rule: classes should not have methods with name starting with create
    Violation: Class <OrderFixture> has method createWithStatus starting with 'create' which is prohibited in (OrderFixture.java:25)

➡️ 해결: createWithStatus() 제거 → reconstitute(Long id, OrderStatus status) 사용
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
        run: mvn test -Dtest=AggregateRootArchTest

      - name: Fail on Architecture Violation
        if: failure()
        run: |
          echo "❌ Architecture violation detected!"
          echo "Aggregate Root must follow strict rules."
          echo "See test results for details."
          exit 1
```

---

## 7️⃣ 체크리스트 (24개 규칙)

### Aggregate Root 규칙 (20개)
- [ ] ❌ Lombok 어노테이션 절대 금지
- [ ] ❌ JPA 어노테이션 절대 금지
- [ ] ❌ Spring 어노테이션 절대 금지
- [ ] ❌ Setter 메서드 절대 금지
- [ ] ✅ 생성자 private 필수
- [ ] ✅ forNew() 정적 팩토리 메서드
- [ ] ✅ of() 정적 팩토리 메서드
- [ ] ✅ reconstitute() 정적 팩토리 메서드
- [ ] ✅ ID 필드 final 필수
- [ ] ✅ Clock 필드 필수
- [ ] ❌ 외래키 원시 타입 금지 (VO 사용)
- [ ] ✅ 패키지 위치: domain.[bc].aggregate.[name]
- [ ] ✅ public 클래스
- [ ] ✅ final 클래스 금지
- [ ] ✅ 비즈니스 메서드 명명 규칙
- [ ] ✅ Application/Adapter 레이어 의존 금지
- [ ] ✅ createdAt 필드 필수 (final)
- [ ] ✅ updatedAt 필드 필수 (non-final)
- [ ] ✅ createdAt final 필수
- [ ] ✅ updatedAt final 금지

### TestFixture 패턴 규칙 (4개) (NEW ⭐)
- [ ] ✅ TestFixture forNew() 메서드 필수
- [ ] ✅ TestFixture of() 메서드 필수
- [ ] ✅ TestFixture reconstitute() 메서드 필수
- [ ] ❌ TestFixture create*() 메서드 금지

---

## 📖 관련 문서

- **[Aggregate Guide](aggregate-guide.md)** - Aggregate Root 구현 가이드
- **[Domain Guide](../domain-guide.md)** - Domain Layer 전체 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0 (Zero-Tolerance)
