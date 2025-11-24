# Value Object ArchUnit 검증 가이드

> **목적**: Value Object 아키텍처 규칙 자동 검증
>
> ArchUnit을 활용하여 VO 설계 규칙을 빌드 시 자동으로 검증합니다.

---

## 1️⃣ 검증 규칙 개요

### Value Object 검증 규칙
1. ✅ **Record 사용 필수**
2. ✅ **정적 팩토리 메서드 (of) 필수**
3. ✅ **ID VO는 forNew() 필수**
4. ✅ **ID VO는 isNew() 필수**
5. ❌ **Lombok 어노테이션 절대 금지**
6. ❌ **JPA 어노테이션 절대 금지**
7. ❌ **Spring 어노테이션 절대 금지**
8. ❌ **create*() 메서드 절대 금지**

**총 8개 규칙** (필수 4개, 금지 4개)

---

## 2️⃣ 테스트 파일 위치

```
domain/
└── src/test/java/com/ryuqq/domain/architecture/
    └── VOArchTest.java
```

---

## 3️⃣ 전체 ArchUnit 테스트 코드

```java
package com.ryuqq.domain.architecture;

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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Value Object ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:</p>
 * <ul>
 *   <li>Record 사용 필수</li>
 *   <li>정적 팩토리 메서드 (of) 필수</li>
 *   <li>ID VO는 forNew() 추가 필수</li>
 *   <li>Lombok 금지</li>
 *   <li>JPA 어노테이션 금지</li>
 *   <li>Spring 어노테이션 금지</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("vo")
@DisplayName("Value Object 아키텍처 검증 테스트")
class VOArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.domain");
    }

    /**
     * 규칙 1: Value Object는 Record여야 한다
     */
    @Test
    @DisplayName("[필수] Value Object는 Record로 구현되어야 한다")
    void valueObjectsShouldBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(beRecords())
            .because("Value Object는 Java 21 Record로 구현해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: Value Object는 of() 메서드를 가져야 한다
     */
    @Test
    @DisplayName("[필수] Value Object는 of() 정적 팩토리 메서드를 가져야 한다")
    void valueObjectsShouldHaveOfMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveStaticMethodWithName("of"))
            .because("Value Object는 of() 정적 팩토리 메서드로 생성해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: ID VO는 forNew() 메서드를 가져야 한다
     */
    @Test
    @DisplayName("[필수] ID Value Object는 forNew() 메서드를 가져야 한다")
    void idValueObjectsShouldHaveForNewMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .and().haveSimpleNameEndingWith("Id")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveStaticMethodWithName("forNew"))
            .because("ID Value Object는 forNew() 메서드로 null 생성을 지원해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: ID VO는 isNew() 메서드를 가져야 한다
     */
    @Test
    @DisplayName("[필수] ID Value Object는 isNew() 메서드를 가져야 한다")
    void idValueObjectsShouldHaveIsNewMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .and().haveSimpleNameEndingWith("Id")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveMethodWithName("isNew"))
            .because("ID Value Object는 isNew() 메서드로 null 여부를 확인해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Value Object는 Lombok 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Value Object는 Lombok 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseLombok() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .should().notBeAnnotatedWith("lombok.Data")
            .andShould().notBeAnnotatedWith("lombok.Value")
            .andShould().notBeAnnotatedWith("lombok.Builder")
            .andShould().notBeAnnotatedWith("lombok.Getter")
            .andShould().notBeAnnotatedWith("lombok.Setter")
            .andShould().notBeAnnotatedWith("lombok.AllArgsConstructor")
            .andShould().notBeAnnotatedWith("lombok.NoArgsConstructor")
            .because("Value Object는 Lombok을 사용하지 않고 Pure Java Record로 구현해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: Value Object는 JPA 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Value Object는 JPA 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseJpa() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .should().notBeAnnotatedWith("javax.persistence.Entity")
            .andShould().notBeAnnotatedWith("javax.persistence.Table")
            .andShould().notBeAnnotatedWith("javax.persistence.Embeddable")
            .andShould().notBeAnnotatedWith("jakarta.persistence.Entity")
            .andShould().notBeAnnotatedWith("jakarta.persistence.Table")
            .andShould().notBeAnnotatedWith("jakarta.persistence.Embeddable")
            .because("Value Object는 JPA 어노테이션을 사용하지 않아야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Value Object는 Spring 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Value Object는 Spring 어노테이션을 사용하지 않아야 한다")
    void valueObjectsShouldNotUseSpring() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .should().notBeAnnotatedWith("org.springframework.stereotype.Component")
            .andShould().notBeAnnotatedWith("org.springframework.stereotype.Service")
            .andShould().notBeAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Value Object는 Spring 어노테이션을 사용하지 않아야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: Value Object는 create*() 메서드를 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Value Object는 create*() 메서드를 사용하지 않아야 한다")
    void valueObjectsShouldNotHaveCreateMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..vo..")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(notHaveMethodsWithNameStartingWith("create"))
            .because("Value Object는 create*() 대신 of(), forNew()를 사용해야 합니다");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /**
     * Record 타입인지 검증
     */
    private static ArchCondition<JavaClass> beRecords() {
        return new ArchCondition<JavaClass>("be records") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean isRecord = javaClass.getModifiers().contains(JavaModifier.FINAL)
                    && javaClass.getAllMethods().stream()
                        .anyMatch(method -> method.getName().equals("toString")
                            && method.getModifiers().contains(JavaModifier.PUBLIC)
                            && method.getModifiers().contains(JavaModifier.FINAL));

                if (!isRecord) {
                    String message = String.format(
                        "Class %s is not a record. Use 'public record' instead of 'public class'",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증
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
     * 클래스가 특정 이름의 메서드를 가지고 있는지 검증 (static 아님)
     */
    private static ArchCondition<JavaClass> haveMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have method with name " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals(methodName));

                if (!hasMethod) {
                    String message = String.format(
                        "Class %s does not have a method named '%s'",
                        javaClass.getName(), methodName
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 클래스가 특정 접두사로 시작하는 메서드를 가지지 않는지 검증
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

**주의**: 커스텀 ArchCondition 사용을 위해 다음 import 필요:
```java
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
```

---

## 4️⃣ 규칙 상세 설명

### 규칙 1: Record 사용 필수

```java
@Test
void valueObjectsShouldBeRecords() {
    ArchRule rule = classes()
        .that().resideInAPackage("..vo..")
        .and().haveSimpleNameNotContaining("Fixture")
        .and().haveSimpleNameNotContaining("Mother")
        .and().haveSimpleNameNotContaining("Test")
        .should(beRecords())
        .because("Value Object는 Java 21 Record로 구현해야 합니다");

    rule.check(classes);
}
```

**검증 내용**:
- `..vo..` 패키지의 모든 클래스
- Fixture, Mother, Test 제외
- Record 타입인지 확인

**위반 시**:
```
Class com.ryuqq.domain.order.vo.Money is not a record. 
Use 'public record' instead of 'public class'
```

---

### 규칙 2: of() 메서드 필수

```java
@Test
void valueObjectsShouldHaveOfMethod() {
    ArchRule rule = classes()
        .that().resideInAPackage("..vo..")
        .and().haveSimpleNameNotContaining("Fixture")
        .and().haveSimpleNameNotContaining("Mother")
        .and().haveSimpleNameNotContaining("Test")
        .should(haveStaticMethodWithName("of"))
        .because("Value Object는 of() 정적 팩토리 메서드로 생성해야 합니다");

    rule.check(classes);
}
```

**검증 내용**:
- public static 메서드
- 메서드 이름이 "of"

**위반 시**:
```
Class com.ryuqq.domain.order.vo.Money does not have a public static method named 'of'
```

---

### 규칙 3: ID VO는 forNew() 필수

```java
@Test
void idValueObjectsShouldHaveForNewMethod() {
    ArchRule rule = classes()
        .that().resideInAPackage("..vo..")
        .and().haveSimpleNameEndingWith("Id")
        .and().haveSimpleNameNotContaining("Fixture")
        .and().haveSimpleNameNotContaining("Mother")
        .and().haveSimpleNameNotContaining("Test")
        .should(haveStaticMethodWithName("forNew"))
        .because("ID Value Object는 forNew() 메서드로 null 생성을 지원해야 합니다");

    rule.check(classes);
}
```

**검증 내용**:
- 클래스 이름이 "Id"로 끝남
- public static forNew() 메서드 존재

**위반 시**:
```
Class com.ryuqq.domain.order.vo.OrderId does not have a public static method named 'forNew'
```

---

### 규칙 4: ID VO는 isNew() 필수

```java
@Test
void idValueObjectsShouldHaveIsNewMethod() {
    ArchRule rule = classes()
        .that().resideInAPackage("..vo..")
        .and().haveSimpleNameEndingWith("Id")
        .and().haveSimpleNameNotContaining("Fixture")
        .and().haveSimpleNameNotContaining("Mother")
        .and().haveSimpleNameNotContaining("Test")
        .should(haveMethodWithName("isNew"))
        .because("ID Value Object는 isNew() 메서드로 null 여부를 확인해야 합니다");

    rule.check(classes);
}
```

**검증 내용**:
- 클래스 이름이 "Id"로 끝남
- isNew() 메서드 존재

**위반 시**:
```
Class com.ryuqq.domain.order.vo.OrderId does not have a method named 'isNew'
```

---

### 규칙 5-7: 외부 의존성 금지

Lombok, JPA, Spring 어노테이션 사용 금지.

**위반 시**:
```
Class com.ryuqq.domain.order.vo.Money should not be annotated with @lombok.Value
```

---

### 규칙 8: create*() 메서드 금지

```java
@Test
void valueObjectsShouldNotHaveCreateMethod() {
    ArchRule rule = classes()
        .that().resideInAPackage("..vo..")
        .and().haveSimpleNameNotContaining("Fixture")
        .and().haveSimpleNameNotContaining("Mother")
        .and().haveSimpleNameNotContaining("Test")
        .should(notHaveMethodsWithNameStartingWith("create"))
        .because("Value Object는 create*() 대신 of(), forNew()를 사용해야 합니다");

    rule.check(classes);
}
```

**위반 시**:
```
Class com.ryuqq.domain.order.vo.Money has method createMoney starting with 'create' which is prohibited
```

---

## 5️⃣ 실패 예시

### ❌ 실패 예시 1: Record 대신 class 사용

```
valueObjectsShouldBeRecords() FAILED
    Rule: classes should be records
    Violation: Class <Money> is not a record. Use 'public record' instead of 'public class'

➡️ 해결: public class Money → public record Money
```

---

### ❌ 실패 예시 2: of() 메서드 없음

```
valueObjectsShouldHaveOfMethod() FAILED
    Rule: classes should have public static method with name of
    Violation: Class <Money> does not have a public static method named 'of'

➡️ 해결: public static Money of(Long amount) 추가
```

---

### ❌ 실패 예시 3: ID VO에 forNew() 없음

```
idValueObjectsShouldHaveForNewMethod() FAILED
    Rule: classes should have public static method with name forNew
    Violation: Class <OrderId> does not have a public static method named 'forNew'

➡️ 해결: public static OrderId forNew() 추가
```

---

### ❌ 실패 예시 4: Lombok 사용

```
valueObjectsShouldNotUseLombok() FAILED
    Rule: classes should not be annotated with @lombok.Value
    Violation: Class <Money> is annotated with @lombok.Value

➡️ 해결: @lombok.Value 제거 → pure Record 사용
```

---

### ❌ 실패 예시 5: create*() 메서드 사용

```
valueObjectsShouldNotHaveCreateMethod() FAILED
    Rule: classes should not have methods with name starting with create
    Violation: Class <Money> has method createMoney starting with 'create'

➡️ 해결: createMoney() 제거 → of() 사용
```

---

## 6️⃣ 빌드 통합

### Gradle 설정

```gradle
dependencies {
    testImplementation 'com.tngtech.archunit:archunit:1.2.1'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.1'
}

test {
    useJUnitPlatform()
}
```

### 빌드 시 자동 실행

```bash
./gradlew test
```

ArchUnit 테스트가 실패하면 **빌드가 실패**하여 규칙 위반을 방지합니다.

---

## 7️⃣ 체크리스트

ArchUnit 테스트 작성 후 다음을 확인:

### Value Object 규칙 (8개)
- [ ] Record 사용 필수
- [ ] of() 메서드 필수
- [ ] ID VO는 forNew() 필수
- [ ] ID VO는 isNew() 필수
- [ ] Lombok 금지
- [ ] JPA 어노테이션 금지
- [ ] Spring 어노테이션 금지
- [ ] create*() 메서드 금지

### 통합 확인
- [ ] `./gradlew test` 실행 성공
- [ ] ArchUnit 테스트 모두 통과
- [ ] CI/CD 파이프라인에서 자동 실행

---

**✅ ArchUnit은 아키텍처 규칙을 자동으로 검증하여 Zero-Tolerance를 달성합니다.**
