# JPA Entity Mapper ArchUnit 테스트 가이드

> **목적**: Mapper 아키텍처 규칙을 ArchUnit으로 자동 검증
>
> 📌 **Zero-Tolerance**: 15개 규칙 자동 검증 (Lombok 금지 9개 포함)

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
- ✅ Lombok 사용 금지 (9개 어노테이션)
- ✅ Static 메서드 금지
- ✅ 비즈니스 로직 금지
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

---

## 3️⃣ 15개 검증 규칙

### 규칙 구성

| 그룹 | 규칙 수 | 검증 내용 |
|-----|--------|----------|
| **@Component 규칙** | 1개 | Spring Bean 등록 필수 |
| **Lombok 금지** | 9개 | 모든 Lombok 어노테이션 금지 |
| **메서드 규칙** | 4개 | Static 금지, 비즈니스 로직 금지, toEntity/toDomain 필수 |
| **네이밍 규칙** | 1개 | *Mapper 접미사 필수 |
| **총계** | **15개** | |

---

## 4️⃣ 전체 테스트 코드

### MapperArchTest.java

```java
package com.ryuqq.adapter.out.persistence.architecture.mapper;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * MapperArchTest - Mapper 아키텍처 규칙 검증 (15개 규칙)
 *
 * <p>mapper-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 그룹:</strong></p>
 * <ul>
 *   <li>@Component 규칙 (1개)</li>
 *   <li>Lombok 금지 규칙 (9개)</li>
 *   <li>메서드 규칙 (4개)</li>
 *   <li>네이밍 규칙 (1개)</li>
 * </ul>
 *
 * @author Development Team
 * @since 2.0.0
 */
@DisplayName("Mapper 아키텍처 규칙 검증 (Zero-Tolerance)")
class MapperArchTest {

    private static final String BASE_PACKAGE = "com.ryuqq.adapter.out.persistence";

    private static JavaClasses allClasses;
    private static JavaClasses mapperClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        mapperClasses = allClasses.that(
            DescribedPredicate.describe(
                "Mapper 클래스",
                javaClass -> javaClass.getSimpleName().endsWith("Mapper")
            )
        );
    }

    // ========================================================================
    // 1. @Component 규칙 (1개)
    // ========================================================================

    @Nested
    @DisplayName("1. @Component 규칙")
    class ComponentRules {

        @Test
        @DisplayName("규칙 1-1: Mapper는 @Component 어노테이션이 필수입니다")
        void mapper_MustBeAnnotatedWithComponent() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().beAnnotatedWith(Component.class)
                .allowEmptyShould(true)
                .because("Mapper는 @Component로 Spring Bean 등록이 필수입니다");

            rule.check(mapperClasses);
        }
    }

    // ========================================================================
    // 2. Lombok 금지 규칙 (9개)
    // ========================================================================

    @Nested
    @DisplayName("2. Lombok 금지 규칙")
    class LombokProhibitionRules {

        @Test
        @DisplayName("규칙 2-1: @Data 금지")
        void mapper_MustNotUseLombok_Data() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.Data")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-2: @Getter 금지")
        void mapper_MustNotUseLombok_Getter() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.Getter")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-3: @Setter 금지")
        void mapper_MustNotUseLombok_Setter() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.Setter")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-4: @Value 금지")
        void mapper_MustNotUseLombok_Value() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.Value")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-5: @Builder 금지")
        void mapper_MustNotUseLombok_Builder() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.Builder")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-6: @AllArgsConstructor 금지")
        void mapper_MustNotUseLombok_AllArgsConstructor() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-7: @NoArgsConstructor 금지")
        void mapper_MustNotUseLombok_NoArgsConstructor() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-8: @RequiredArgsConstructor 금지")
        void mapper_MustNotUseLombok_RequiredArgsConstructor() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.RequiredArgsConstructor")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 2-9: @UtilityClass 금지")
        void mapper_MustNotUseLombok_UtilityClass() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().notBeAnnotatedWith("lombok.experimental.UtilityClass")
                .allowEmptyShould(true)
                .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(mapperClasses);
        }
    }

    // ========================================================================
    // 3. 메서드 규칙 (4개)
    // ========================================================================

    @Nested
    @DisplayName("3. 메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("규칙 3-1: Static 변환 메서드 금지")
        void mapper_MustNotHaveStaticConversionMethods() {
            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
                .and().arePublic()
                .and().haveNameMatching("(toEntity|toDomain|to[A-Z].*)")
                .should().notBeStatic()
                .allowEmptyShould(true)
                .because("Mapper는 Static 메서드가 금지됩니다 (Spring Bean 주입 필요)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 3-2: 비즈니스 로직 메서드 금지")
        void mapper_MustNotHaveBusinessLogicMethods() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should(notHaveBusinessLogicMethods())
                .allowEmptyShould(true)
                .because("Mapper는 비즈니스 로직이 금지됩니다 (단순 변환만 담당)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 3-3: toEntity() 메서드 필수")
        void mapper_MustHaveToEntityMethod() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should(havePublicToEntityMethod())
                .allowEmptyShould(true)
                .because("Mapper는 toEntity() 메서드가 필수입니다 (Domain → Entity)");

            rule.check(mapperClasses);
        }

        @Test
        @DisplayName("규칙 3-4: toDomain() 메서드 필수")
        void mapper_MustHaveToDomainMethod() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should(havePublicToDomainMethod())
                .allowEmptyShould(true)
                .because("Mapper는 toDomain() 메서드가 필수입니다 (Entity → Domain)");

            rule.check(mapperClasses);
        }
    }

    // ========================================================================
    // 4. 네이밍 규칙 (1개)
    // ========================================================================

    @Nested
    @DisplayName("4. 네이밍 규칙")
    class NamingRules {

        @Test
        @DisplayName("규칙 4-1: mapper 패키지의 @Component 클래스는 *Mapper 접미사 필수")
        void mapper_MustFollowNamingConvention() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Component.class)
                .and().resideInAPackage("..mapper..")
                .should().haveSimpleNameEndingWith("Mapper")
                .allowEmptyShould(true)
                .because("Mapper 클래스는 *Mapper 네이밍 규칙을 따라야 합니다");

            rule.check(allClasses);
        }
    }

    // ========================================================================
    // 커스텀 ArchCondition
    // ========================================================================

    /**
     * 비즈니스 로직 메서드 존재 여부 검증
     *
     * <p>검증 패턴: validate*, calculate*, approve*, cancel*, complete*, activate*, deactivate*</p>
     */
    private static ArchCondition<JavaClass> notHaveBusinessLogicMethods() {
        return new ArchCondition<>("비즈니스 로직 메서드가 없어야 합니다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> method.getName().matches(
                        "(validate|calculate|approve|cancel|complete|activate|deactivate).*"
                    ))
                    .forEach(method -> {
                        String message = String.format(
                            "클래스 %s가 비즈니스 로직 메서드 %s()를 가지고 있습니다 (Mapper는 단순 변환만 담당)",
                            javaClass.getSimpleName(), method.getName()
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    });
            }
        };
    }

    /**
     * public toEntity() 메서드 존재 검증
     */
    private static ArchCondition<JavaClass> havePublicToEntityMethod() {
        return new ArchCondition<>("public toEntity() 메서드가 있어야 합니다") {
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
                        "클래스 %s가 public toEntity() 메서드를 가지고 있지 않습니다 (Domain → Entity 변환 필수)",
                        javaClass.getSimpleName()
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
        return new ArchCondition<>("public toDomain() 메서드가 있어야 합니다") {
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
                        "클래스 %s가 public toDomain() 메서드를 가지고 있지 않습니다 (Entity → Domain 변환 필수)",
                        javaClass.getSimpleName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
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
./gradlew test --tests "MapperArchTest\$LombokProhibitionRules"
```

---

## 6️⃣ 위반 시 처리

### 위반 예시 출력

```
MapperArchTest$ComponentRules > 규칙 1-1: Mapper는 @Component 어노테이션이 필수입니다 FAILED
    com.tngtech.archunit.lang.ArchRule$AssertionError:
    Architecture Violation [Priority: MEDIUM] - Rule 'classes that have simple name ending with 'Mapper'
    should be annotated with @Component' was violated (1 times):
    Class <com.ryuqq.adapter.out.persistence.order.mapper.OrderJpaEntityMapper>
    is not annotated with @Component
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

---

## 7️⃣ 체크리스트

Mapper ArchUnit 테스트 작성 시:
- [ ] **MapperArchTest.java 파일 작성**
  - [ ] 15개 규칙 모두 구현
  - [ ] 커스텀 ArchCondition 구현
  - [ ] `allowEmptyShould(true)` 모든 규칙에 추가
  - [ ] `@Nested` 구조로 그룹화
- [ ] **테스트 실행 확인**
  - [ ] 로컬에서 전체 테스트 통과
  - [ ] 위반 사항 수정
- [ ] **CI/CD 통합**
  - [ ] GitHub Actions / GitLab CI 설정
  - [ ] PR 시 자동 실행

---

## 8️⃣ 참고 문서

- [mapper-guide.md](./mapper-guide.md) - Mapper 컨벤션
- [mapper-test-guide.md](./mapper-test-guide.md) - Mapper 테스트 전략
- [entity-archunit.md](../entity/entity-archunit.md) - Entity ArchUnit 참고

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0
