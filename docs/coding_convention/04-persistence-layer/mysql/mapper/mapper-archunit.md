# JPA Entity Mapper ArchUnit 테스트 가이드

> **목적**: Mapper 아키텍처 규칙을 ArchUnit으로 자동 검증

---

## 1️⃣ 개요

### ArchUnit이란?

**빌드 시 아키텍처 규칙을 자동 검증하는 라이브러리**

- Java 코드의 아키텍처 규칙을 테스트 코드로 작성
- 빌드 시 자동 실행되어 규칙 위반 즉시 감지
- 코드 리뷰 부담 감소 및 일관성 보장

### Mapper ArchUnit의 역할

**mapper-guide.md의 핵심 규칙을 자동 검증**

- ✅ @Component 필수
- ✅ Lombok 사용 금지
- ✅ Static 메서드 금지
- ✅ 비즈니스 로직 금지
- ✅ 시간 생성 금지 (LocalDateTime.now())
- ✅ toEntity() / toDomain() 메서드 필수
- ✅ Mapper 네이밍 규칙 (*Mapper)

---

## 2️⃣ ArchUnit 의존성

### Gradle 설정

```gradle
dependencies {
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
}
```

### Maven 설정

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## 3️⃣ 12개 검증 규칙

### 규칙 1: @Component 어노테이션 필수

**목적**: Mapper는 Spring Bean으로 등록되어야 함

```java
@Test
@DisplayName("규칙 1: @Component 어노테이션 필수")
void mapper_MustBeAnnotatedWithComponent() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().beAnnotatedWith(Component.class)
        .because("Mapper는 @Component로 Spring Bean 등록이 필수입니다");

    rule.check(mapperClasses);
}
```

**위반 예시**:
```java
// ❌ 위반
public class OrderJpaEntityMapper {
    // @Component 없음!
}

// ✅ 올바른 예시
@Component
public class OrderJpaEntityMapper {
    // ...
}
```

---

### 규칙 2: Lombok 사용 금지 (6개 테스트)

**목적**: Plain Java 사용 강제

#### 2-1. @Data 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Data")
void mapper_MustNotUseLombok_Data() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.Data")
        .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(mapperClasses);
}
```

#### 2-2. @AllArgsConstructor 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @AllArgsConstructor")
void mapper_MustNotUseLombok_AllArgsConstructor() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
        .because("Mapper는 Lombok 사용이 금지됩니다");

    rule.check(mapperClasses);
}
```

#### 2-3. @NoArgsConstructor 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @NoArgsConstructor")
void mapper_MustNotUseLombok_NoArgsConstructor() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
        .because("Mapper는 Lombok 사용이 금지됩니다");

    rule.check(mapperClasses);
}
```

#### 2-4. @RequiredArgsConstructor 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @RequiredArgsConstructor")
void mapper_MustNotUseLombok_RequiredArgsConstructor() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.RequiredArgsConstructor")
        .because("Mapper는 Lombok 사용이 금지됩니다");

    rule.check(mapperClasses);
}
```

#### 2-5. @Builder 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Builder")
void mapper_MustNotUseLombok_Builder() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.Builder")
        .because("Mapper는 Lombok 사용이 금지됩니다");

    rule.check(mapperClasses);
}
```

#### 2-6. @UtilityClass 금지

```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @UtilityClass")
void mapper_MustNotUseLombok_UtilityClass() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should().notBeAnnotatedWith("lombok.experimental.UtilityClass")
        .because("Mapper는 Lombok 사용이 금지됩니다");

    rule.check(mapperClasses);
}
```

**위반 예시**:
```java
// ❌ 위반
@Component
@RequiredArgsConstructor  // ❌ Lombok 금지!
public class OrderJpaEntityMapper {
    // ...
}

// ✅ 올바른 예시
@Component
public class OrderJpaEntityMapper {
    // Plain Java 생성자
    public OrderJpaEntityMapper() {
    }
}
```

---

### 규칙 3: Static 메서드 금지

**목적**: Spring Bean 주입 가능하도록 Instance 메서드 사용

```java
@Test
@DisplayName("규칙 3: Static 메서드 금지")
void mapper_MustNotHaveStaticMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
        .and().arePublic()
        .and().haveNameMatching("(toEntity|toDomain|to[A-Z].*)")
        .should().notBeStatic()
        .because("Mapper는 Static 메서드가 금지됩니다 (Spring Bean 주입 필요)");

    rule.check(mapperClasses);
}
```

**위반 예시**:
```java
// ❌ 위반
public class OrderJpaEntityMapper {
    private OrderJpaEntityMapper() { }

    // ❌ Static 메서드 금지!
    public static OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(...);
    }
}

// ✅ 올바른 예시
@Component
public class OrderJpaEntityMapper {
    // ✅ Instance 메서드
    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(...);
    }
}
```

---

### 규칙 4: 비즈니스 로직 금지

**목적**: Mapper는 단순 변환만 담당

```java
@Test
@DisplayName("규칙 4: 비즈니스 로직 금지")
void mapper_MustNotHaveBusinessLogicMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
        .and().arePublic()
        .and().haveNameMatching("(validate|calculate|approve|cancel|complete|activate|deactivate).*")
        .should(notExist())
        .because("Mapper는 비즈니스 로직이 금지됩니다 (단순 변환만 담당)");

    rule.check(mapperClasses);
}
```

**위반 예시**:
```java
// ❌ 위반
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ❌ 비즈니스 검증 금지!
        if (domain.getTotalAmountValue() < 0) {
            throw new InvalidOrderException("금액은 0보다 커야 합니다");
        }

        return OrderJpaEntity.of(...);
    }
}

// ✅ 올바른 예시
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ✅ 단순 변환만 (검증은 Domain Layer에서)
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getTotalAmountValue(),  // 이미 Domain에서 검증됨
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
```

---

### 규칙 5: toEntity() 메서드 필수

**목적**: Domain → Entity 변환 메서드 제공

```java
@Test
@DisplayName("규칙 5: toEntity() 메서드 필수")
void mapper_MustHaveToEntityMethod() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should(havePublicToEntityMethod())
        .because("Mapper는 toEntity() 메서드가 필수입니다 (Domain → Entity)");

    rule.check(mapperClasses);
}
```

**커스텀 ArchCondition**:
```java
private static ArchCondition<JavaClass> havePublicToEntityMethod() {
    return new ArchCondition<>("have public toEntity() method") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean hasToEntityMethod = javaClass.getMethods().stream()
                .anyMatch(method ->
                    method.getName().equals("toEntity") &&
                    method.getModifiers().contains(JavaModifier.PUBLIC) &&
                    !method.getModifiers().contains(JavaModifier.STATIC)
                );

            if (!hasToEntityMethod) {
                String message = String.format(
                    "Class %s does not have a public toEntity() method (required for Domain → Entity conversion)",
                    javaClass.getName()
                );
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    };
}
```

---

### 규칙 6: toDomain() 메서드 필수

**목적**: Entity → Domain 변환 메서드 제공

```java
@Test
@DisplayName("규칙 6: toDomain() 메서드 필수")
void mapper_MustHaveToDomainMethod() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .should(havePublicToDomainMethod())
        .because("Mapper는 toDomain() 메서드가 필수입니다 (Entity → Domain)");

    rule.check(mapperClasses);
}
```

**커스텀 ArchCondition**:
```java
private static ArchCondition<JavaClass> havePublicToDomainMethod() {
    return new ArchCondition<>("have public toDomain() method") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean hasToDomainMethod = javaClass.getMethods().stream()
                .anyMatch(method ->
                    method.getName().equals("toDomain") &&
                    method.getModifiers().contains(JavaModifier.PUBLIC) &&
                    !method.getModifiers().contains(JavaModifier.STATIC)
                );

            if (!hasToDomainMethod) {
                String message = String.format(
                    "Class %s does not have a public toDomain() method (required for Entity → Domain conversion)",
                    javaClass.getName()
                );
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    };
}
```

---

### 규칙 7: Mapper 네이밍 규칙

**목적**: 일관된 네이밍 규칙 적용

```java
@Test
@DisplayName("규칙 7: Mapper 네이밍 규칙 (*Mapper)")
void mapper_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Component.class)
        .and().resideInAPackage("..mapper..")
        .should().haveSimpleNameEndingWith("Mapper")
        .because("Mapper 클래스는 *Mapper 네이밍 규칙을 따라야 합니다");

    rule.check(allClasses);
}
```

**위반 예시**:
```java
// ❌ 위반
@Component
public class OrderEntityConverter {  // ❌ *Mapper가 아님!
    // ...
}

// ✅ 올바른 예시
@Component
public class OrderJpaEntityMapper {  // ✅ *Mapper
    // ...
}
```

---

## 4️⃣ 전체 테스트 코드

### MapperArchTest.java

```java
package com.company.adapter.out.persistence.architecture.mapper;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * MapperArchTest - Mapper 아키텍처 규칙 검증
 *
 * <p>mapper-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: @Component 어노테이션 필수</li>
 *   <li>규칙 2: Lombok 사용 금지 (6개 어노테이션)</li>
 *   <li>규칙 3: Static 메서드 금지</li>
 *   <li>규칙 4: 비즈니스 로직 금지</li>
 *   <li>규칙 5: toEntity() 메서드 필수</li>
 *   <li>규칙 6: toDomain() 메서드 필수</li>
 *   <li>규칙 7: Mapper 네이밍 규칙 (*Mapper)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Mapper 아키텍처 규칙 검증 (Zero-Tolerance)")
class MapperArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses mapperClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence");

        mapperClasses = allClasses.that(
            DescribedPredicate.describe(
                "are Mapper classes",
                javaClass -> javaClass.getSimpleName().endsWith("Mapper")
            )
        );
    }

    @Test
    @DisplayName("규칙 1: @Component 어노테이션 필수")
    void mapper_MustBeAnnotatedWithComponent() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().beAnnotatedWith(Component.class)
            .because("Mapper는 @Component로 Spring Bean 등록이 필수입니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Data")
    void mapper_MustNotUseLombok_Data() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.Data")
            .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @AllArgsConstructor")
    void mapper_MustNotUseLombok_AllArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @NoArgsConstructor")
    void mapper_MustNotUseLombok_NoArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @RequiredArgsConstructor")
    void mapper_MustNotUseLombok_RequiredArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.RequiredArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Builder")
    void mapper_MustNotUseLombok_Builder() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.Builder")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @UtilityClass")
    void mapper_MustNotUseLombok_UtilityClass() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.experimental.UtilityClass")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 3: Static 메서드 금지")
    void mapper_MustNotHaveStaticMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
            .and().arePublic()
            .and().haveNameMatching("(toEntity|toDomain|to[A-Z].*)")
            .should().notBeStatic()
            .because("Mapper는 Static 메서드가 금지됩니다 (Spring Bean 주입 필요)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 4: 비즈니스 로직 금지")
    void mapper_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
            .and().arePublic()
            .and().haveNameMatching("(validate|calculate|approve|cancel|complete|activate|deactivate).*")
            .should(notExist())
            .because("Mapper는 비즈니스 로직이 금지됩니다 (단순 변환만 담당)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 5: toEntity() 메서드 필수")
    void mapper_MustHaveToEntityMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should(havePublicToEntityMethod())
            .because("Mapper는 toEntity() 메서드가 필수입니다 (Domain → Entity)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 6: toDomain() 메서드 필수")
    void mapper_MustHaveToDomainMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should(havePublicToDomainMethod())
            .because("Mapper는 toDomain() 메서드가 필수입니다 (Entity → Domain)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 7: Mapper 네이밍 규칙 (*Mapper)")
    void mapper_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Component.class)
            .and().resideInAPackage("..mapper..")
            .should().haveSimpleNameEndingWith("Mapper")
            .because("Mapper 클래스는 *Mapper 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }

    // ===== 커스텀 ArchCondition =====

    /**
     * public toEntity() 메서드 존재 검증
     */
    private static ArchCondition<JavaClass> havePublicToEntityMethod() {
        return new ArchCondition<>("have public toEntity() method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasToEntityMethod = javaClass.getMethods().stream()
                    .anyMatch(method ->
                        method.getName().equals("toEntity") &&
                        method.getModifiers().contains(JavaModifier.PUBLIC) &&
                        !method.getModifiers().contains(JavaModifier.STATIC)
                    );

                if (!hasToEntityMethod) {
                    String message = String.format(
                        "Class %s does not have a public toEntity() method (required for Domain → Entity conversion)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * public toDomain() 메서드 존재 검증
     */
    private static ArchCondition<JavaClass> havePublicToDomainMethod() {
        return new ArchCondition<>("have public toDomain() method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasToDomainMethod = javaClass.getMethods().stream()
                    .anyMatch(method ->
                        method.getName().equals("toDomain") &&
                        method.getModifiers().contains(JavaModifier.PUBLIC) &&
                        !method.getModifiers().contains(JavaModifier.STATIC)
                    );

                if (!hasToDomainMethod) {
                    String message = String.format(
                        "Class %s does not have a public toDomain() method (required for Entity → Domain conversion)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 메서드가 존재하지 않아야 함을 검증하는 ArchCondition
     */
    private static ArchCondition<JavaMethod> notExist() {
        return new ArchCondition<>("not exist") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String message = String.format(
                    "Method %s.%s() should not exist",
                    method.getOwner().getSimpleName(),
                    method.getName()
                );
                events.add(SimpleConditionEvent.violated(method, message));
            }
        };
    }
}
```

---

## 5️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행 (ArchUnit 포함)
./gradlew test

# Mapper ArchUnit만 실행
./gradlew test --tests MapperArchTest

# 특정 규칙만 실행
./gradlew test --tests MapperArchTest.mapper_MustBeAnnotatedWithComponent
```

### Maven 실행

```bash
# 전체 테스트 실행
mvn test

# Mapper ArchUnit만 실행
mvn test -Dtest=MapperArchTest
```

### IDE 실행

**IntelliJ IDEA**:
1. `MapperArchTest.java` 파일 우클릭
2. `Run 'MapperArchTest'` 선택

**Eclipse**:
1. `MapperArchTest.java` 파일 우클릭
2. `Run As` → `JUnit Test` 선택

---

## 6️⃣ CI/CD 통합

### GitHub Actions

```yaml
name: Architecture Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  archunit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Mapper ArchUnit Tests
        run: ./gradlew test --tests MapperArchTest
```

### GitLab CI

```yaml
test:archunit:mapper:
  stage: test
  script:
    - ./gradlew test --tests MapperArchTest
  only:
    - merge_requests
    - main
    - develop
```

---

## 7️⃣ 위반 시 처리

### 위반 예시 출력

```
MapperArchTest > 규칙 1: @Component 어노테이션 필수 FAILED
    com.tngtech.archunit.lang.ArchRule$AssertionError:
    Architecture Violation [Priority: MEDIUM] - Rule 'classes that have simple name ending with 'Mapper'
    should be annotated with @Component' was violated (1 times):
    Class <com.company.adapter.out.persistence.order.mapper.OrderJpaEntityMapper>
    is not annotated with @Component in (OrderJpaEntityMapper.java:0)
```

### 수정 방법

```java
// ❌ 위반 코드
public class OrderJpaEntityMapper {
    // @Component 없음!
}

// ✅ 수정 후
@Component
public class OrderJpaEntityMapper {
    // @Component 추가
}
```

### 빌드 실패 방지

**ArchUnit은 위반 시 빌드를 실패시킵니다.**

- ✅ 로컬에서 테스트 실행 후 커밋
- ✅ Pre-commit Hook으로 자동 검증
- ✅ CI/CD에서 조기 실패 (Fail Fast)

---

## 8️⃣ 체크리스트

Mapper ArchUnit 테스트 작성 시:
- [ ] **MapperArchTest.java 파일 작성**
  - [ ] 12개 규칙 모두 구현
  - [ ] 커스텀 ArchCondition 구현
- [ ] **테스트 실행 확인**
  - [ ] 로컬에서 전체 테스트 통과
  - [ ] 위반 사항 수정
- [ ] **CI/CD 통합**
  - [ ] GitHub Actions / GitLab CI 설정
  - [ ] PR 시 자동 실행
- [ ] **문서 작성**
  - [ ] mapper-archunit.md 작성
  - [ ] 팀원 공유

---

## 9️⃣ 참고 문서

- [mapper-guide.md](./mapper-guide.md) - Mapper 컨벤션
- [mapper-test-guide.md](./mapper-test-guide.md) - Mapper 테스트 전략
- [entity-archunit.md](../entity/entity-archunit.md) - Entity ArchUnit 참고

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
