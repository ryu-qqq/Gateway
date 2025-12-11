# JPA Entity ArchUnit 테스트 가이드

> **목적**: JPA Entity 컨벤션을 ArchUnit으로 자동 검증

---

## 1️⃣ ArchUnit 테스트란?

### 역할
**빌드 시 자동 실행 → 컨벤션 위반 감지 → 빌드 실패**

JPA Entity가 entity-guide.md의 모든 규칙을 준수하는지 자동으로 검증합니다.

### 책임
- ✅ **Zero-Tolerance 검증**: Lombok 금지, JPA 관계 금지, Setter 금지 등
- ✅ **생성자 패턴 검증**: protected 기본 생성자 + private 전체 필드 생성자
- ✅ **Factory Method 검증**: public static of() 메서드 필수
- ✅ **빌드 시 자동 실행**: 위반 시 빌드 실패로 강제 준수

### 핵심 원칙
```
개발자 코드 작성
  └─ Gradle/Maven 빌드
      └─ JpaEntityArchTest 실행 (자동)
            ├─ 16개 규칙 검증 (4개 @Nested 그룹)
            ├─ 위반 발견 → 빌드 실패
            └─ 모두 통과 → 빌드 성공
```

---

## 2️⃣ 테스트 클래스 구조

### 위치
```
adapter-out/persistence-mysql/
  └─ src/test/java/
      └─ com/ryuqq/adapter/out/persistence/architecture/entity/
          └─ JpaEntityArchTest.java
```

### 전체 구조 (4개 @Nested 그룹)

```java
package com.ryuqq.adapter.out.persistence.architecture.entity;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * JPA Entity 아키텍처 규칙 검증 (Zero-Tolerance)
 *
 * <p>entity-guide.md에 정의된 모든 규칙을 ArchUnit으로 자동 검증합니다.</p>
 *
 * <p><strong>검증 그룹:</strong></p>
 * <ul>
 *   <li>LombokProhibition: Lombok 어노테이션 금지 (6개 규칙)</li>
 *   <li>JpaRelationshipProhibition: JPA 관계 어노테이션 금지 (4개 규칙)</li>
 *   <li>MethodPatternRules: Setter/비즈니스 로직 금지 (2개 규칙)</li>
 *   <li>ConstructorAndFactoryRules: 생성자/팩토리 패턴 (4개 규칙)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("JPA Entity 아키텍처 규칙 검증 (Zero-Tolerance)")
class JpaEntityArchTest {

    private static final String BASE_PACKAGE = "com.ryuqq.adapter.out.persistence";

    private static JavaClasses allClasses;
    private static JavaClasses entityClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages(BASE_PACKAGE);

        entityClasses = allClasses.that(
            com.tngtech.archunit.base.DescribedPredicate.describe(
                "are JPA Entity classes",
                javaClass -> javaClass.isAnnotatedWith(Entity.class)
            )
        );
    }

    // ========== @Nested 그룹 1: Lombok 금지 ==========

    @Nested
    @DisplayName("Lombok 어노테이션 금지 규칙")
    class LombokProhibition {

        @Test
        @DisplayName("@Data 사용 금지")
        void jpaEntity_MustNotUseLombok_Data() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.Data")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@Getter 사용 금지")
        void jpaEntity_MustNotUseLombok_Getter() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.Getter")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@Setter 사용 금지")
        void jpaEntity_MustNotUseLombok_Setter() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.Setter")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@Builder 사용 금지")
        void jpaEntity_MustNotUseLombok_Builder() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.Builder")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@AllArgsConstructor 사용 금지")
        void jpaEntity_MustNotUseLombok_AllArgsConstructor() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@NoArgsConstructor 사용 금지")
        void jpaEntity_MustNotUseLombok_NoArgsConstructor() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
                .allowEmptyShould(true)
                .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

            rule.check(allClasses);
        }
    }

    // ========== @Nested 그룹 2: JPA 관계 어노테이션 금지 ==========

    @Nested
    @DisplayName("JPA 관계 어노테이션 금지 규칙 (Long FK 전략)")
    class JpaRelationshipProhibition {

        @Test
        @DisplayName("@ManyToOne 사용 금지")
        void jpaEntity_MustNotUseJpaRelationship_ManyToOne() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith(ManyToOne.class)
                .allowEmptyShould(true)
                .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@OneToMany 사용 금지")
        void jpaEntity_MustNotUseJpaRelationship_OneToMany() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith(OneToMany.class)
                .allowEmptyShould(true)
                .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@OneToOne 사용 금지")
        void jpaEntity_MustNotUseJpaRelationship_OneToOne() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith(OneToOne.class)
                .allowEmptyShould(true)
                .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("@ManyToMany 사용 금지")
        void jpaEntity_MustNotUseJpaRelationship_ManyToMany() {
            ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                .should().notBeAnnotatedWith(ManyToMany.class)
                .allowEmptyShould(true)
                .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

            rule.check(allClasses);
        }
    }

    // ========== @Nested 그룹 3: 메서드 패턴 규칙 ==========

    @Nested
    @DisplayName("메서드 패턴 규칙 (Setter/비즈니스 로직 금지)")
    class MethodPatternRules {

        @Test
        @DisplayName("Setter 메서드 금지")
        void jpaEntity_MustNotHaveSetterMethods() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(notHaveSetterMethods())
                .allowEmptyShould(true)
                .because("JPA Entity는 Setter 메서드가 금지됩니다 (Getter만 제공)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("비즈니스 로직 메서드 금지")
        void jpaEntity_MustNotHaveBusinessLogicMethods() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(notHaveBusinessLogicMethods())
                .allowEmptyShould(true)
                .because("JPA Entity는 비즈니스 로직이 금지됩니다 (Domain Layer에서 처리)");

            rule.check(allClasses);
        }
    }

    // ========== @Nested 그룹 4: 생성자 및 팩토리 패턴 ==========

    @Nested
    @DisplayName("생성자 및 팩토리 패턴 규칙")
    class ConstructorAndFactoryRules {

        @Test
        @DisplayName("protected 기본 생성자 필수")
        void jpaEntity_MustHaveProtectedNoArgsConstructor() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(haveProtectedOrPublicNoArgsConstructor())
                .allowEmptyShould(true)
                .because("JPA Entity는 JPA 스펙을 위해 protected/public 기본 생성자가 필수입니다");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("private 전체 필드 생성자 필수")
        void jpaEntity_MustHavePrivateAllArgsConstructor() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(havePrivateConstructorWithParameters())
                .allowEmptyShould(true)
                .because("JPA Entity는 무분별한 생성 방지를 위해 private 생성자가 필수입니다");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("public static of() 메서드 필수")
        void jpaEntity_MustHavePublicStaticOfMethod() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(havePublicStaticOfMethod())
                .allowEmptyShould(true)
                .because("JPA Entity는 Mapper 전용 of() 스태틱 메서드가 필수입니다");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("Entity 네이밍 규칙 (*JpaEntity)")
        void jpaEntity_MustFollowNamingConvention() {
            ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().haveSimpleNameEndingWith("JpaEntity")
                .allowEmptyShould(true)
                .because("JPA Entity 클래스는 *JpaEntity 네이밍 규칙을 따라야 합니다");

            rule.check(allClasses);
        }
    }

    // ========== 커스텀 ArchCondition ==========

    /**
     * Setter 메서드가 없어야 하는 조건
     *
     * <p>public setXxx() 형태의 메서드를 감지합니다.</p>
     */
    private static ArchCondition<JavaClass> notHaveSetterMethods() {
        return new ArchCondition<>("not have setter methods") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> method.getName().matches("set[A-Z].*"))
                    .filter(method -> method.getRawParameterTypes().size() == 1)
                    .forEach(method -> {
                        String message = String.format(
                            "클래스 %s가 setter 메서드 %s()를 가지고 있습니다 (Setter 금지)",
                            javaClass.getSimpleName(), method.getName()
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    });
            }
        };
    }

    /**
     * 비즈니스 로직 메서드가 없어야 하는 조건
     *
     * <p>approve, cancel, complete 등 비즈니스 메서드를 감지합니다.</p>
     */
    private static ArchCondition<JavaClass> notHaveBusinessLogicMethods() {
        return new ArchCondition<>("not have business logic methods") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String businessMethodPattern = "(approve|cancel|complete|activate|deactivate|validate|calculate|process|execute).*";

                javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> method.getName().matches(businessMethodPattern))
                    .forEach(method -> {
                        String message = String.format(
                            "클래스 %s가 비즈니스 로직 메서드 %s()를 가지고 있습니다 (Domain Layer에서 처리)",
                            javaClass.getSimpleName(), method.getName()
                        );
                        events.add(SimpleConditionEvent.violated(javaClass, message));
                    });
            }
        };
    }

    /**
     * protected 또는 public 기본 생성자가 있어야 하는 조건
     *
     * <p>JPA 스펙 요구사항입니다.</p>
     */
    private static ArchCondition<JavaClass> haveProtectedOrPublicNoArgsConstructor() {
        return new ArchCondition<>("have protected or public no-args constructor") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasValidNoArgsConstructor = javaClass.getConstructors().stream()
                    .anyMatch(constructor ->
                        (constructor.getModifiers().contains(JavaModifier.PROTECTED) ||
                         constructor.getModifiers().contains(JavaModifier.PUBLIC)) &&
                        constructor.getRawParameterTypes().isEmpty()
                    );

                if (!hasValidNoArgsConstructor) {
                    String message = String.format(
                        "클래스 %s에 protected/public 기본 생성자가 없습니다 (JPA 스펙 필수)",
                        javaClass.getSimpleName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * private 전체 필드 생성자가 있어야 하는 조건
     *
     * <p>무분별한 Entity 생성을 방지합니다.</p>
     */
    private static ArchCondition<JavaClass> havePrivateConstructorWithParameters() {
        return new ArchCondition<>("have private constructor with parameters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasPrivateConstructor = javaClass.getConstructors().stream()
                    .anyMatch(constructor ->
                        constructor.getModifiers().contains(JavaModifier.PRIVATE) &&
                        !constructor.getRawParameterTypes().isEmpty()
                    );

                if (!hasPrivateConstructor) {
                    String message = String.format(
                        "클래스 %s에 private 전체 필드 생성자가 없습니다 (무분별한 생성 방지)",
                        javaClass.getSimpleName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * public static of() 메서드가 있어야 하는 조건
     *
     * <p>Mapper에서 Entity 생성 시 사용합니다.</p>
     */
    private static ArchCondition<JavaClass> havePublicStaticOfMethod() {
        return new ArchCondition<>("have public static of() method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasOfMethod = javaClass.getMethods().stream()
                    .anyMatch(method ->
                        method.getName().equals("of") &&
                        method.getModifiers().contains(JavaModifier.PUBLIC) &&
                        method.getModifiers().contains(JavaModifier.STATIC)
                    );

                if (!hasOfMethod) {
                    String message = String.format(
                        "클래스 %s에 public static of() 메서드가 없습니다 (Mapper 전용 팩토리 메서드)",
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

## 3️⃣ 16개 검증 규칙 요약

### 그룹별 규칙

| 그룹 | 규칙 수 | 검증 내용 |
|------|--------|----------|
| **LombokProhibition** | 6개 | @Data, @Getter, @Setter, @Builder, @AllArgsConstructor, @NoArgsConstructor 금지 |
| **JpaRelationshipProhibition** | 4개 | @ManyToOne, @OneToMany, @OneToOne, @ManyToMany 금지 |
| **MethodPatternRules** | 2개 | Setter 메서드, 비즈니스 로직 메서드 금지 |
| **ConstructorAndFactoryRules** | 4개 | 생성자 패턴, of() 메서드, 네이밍 규칙 |

### 핵심 수정사항 (기존 문서 대비)

| 항목 | 기존 | 수정 후 |
|------|------|--------|
| JPA 관계 검증 | `noFields()...should().beAnnotatedWith()` | `fields()...should().notBeAnnotatedWith()` |
| Setter 검증 | `should(notExist())` (미존재 메서드) | 커스텀 `notHaveSetterMethods()` |
| 빈 패키지 처리 | 없음 | `allowEmptyShould(true)` 추가 |
| 구조화 | 단일 클래스 | 4개 @Nested 그룹 |
| 기본 생성자 | protected만 | protected 또는 public |

---

## 4️⃣ 실행 방법

### Gradle

```bash
# 전체 테스트 실행
./gradlew test

# JpaEntityArchTest만 실행
./gradlew test --tests JpaEntityArchTest

# 특정 @Nested 그룹만 실행
./gradlew test --tests "JpaEntityArchTest\$LombokProhibition"
./gradlew test --tests "JpaEntityArchTest\$JpaRelationshipProhibition"
```

### IntelliJ IDEA

1. `JpaEntityArchTest.java` 파일 열기
2. 클래스명 왼쪽 ▶️ 클릭 → "Run 'JpaEntityArchTest'"
3. @Nested 그룹별 실행: 그룹명 왼쪽 ▶️ 클릭

---

## 5️⃣ 위반 예시 및 수정

### 예시 1: Lombok 사용 위반

**❌ 위반 코드**:
```java
@Entity
@Table(name = "orders")
@Data  // ❌ Lombok 금지!
public class OrderJpaEntity extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**✅ 수정 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected OrderJpaEntity() { }

    private OrderJpaEntity(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.id = id;
    }

    public static OrderJpaEntity of(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new OrderJpaEntity(id, createdAt, updatedAt);
    }

    public Long getId() { return id; }
}
```

---

### 예시 2: JPA 관계 어노테이션 위반

**❌ 위반 코드**:
```java
@Entity
public class OrderJpaEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)  // ❌ JPA 관계 금지!
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;
}
```

**✅ 수정 코드**:
```java
@Entity
public class OrderJpaEntity extends BaseAuditEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;  // ✅ Long FK 사용
}
```

---

### 예시 3: Setter 메서드 위반

**❌ 위반 코드**:
```java
@Entity
public class OrderJpaEntity extends BaseAuditEntity {

    private String status;

    public void setStatus(String status) {  // ❌ Setter 금지!
        this.status = status;
    }
}
```

**✅ 수정 코드**:
```java
@Entity
public class OrderJpaEntity extends BaseAuditEntity {

    private String status;

    // Setter 제거, of() 메서드로만 상태 설정
    public static OrderJpaEntity of(Long id, String status, ...) {
        return new OrderJpaEntity(id, status, ...);
    }

    public String getStatus() { return status; }  // Getter만 제공
}
```

---

## 6️⃣ CI/CD 통합

### GitHub Actions

```yaml
name: ArchUnit Tests

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  archunit-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run JPA Entity ArchUnit Tests
        run: ./gradlew test --tests JpaEntityArchTest

      - name: Upload Test Report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: archunit-test-report
          path: build/reports/tests/test/
```

---

## 7️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] `JpaEntityArchTest.java` 파일 생성
- [ ] `@BeforeAll`에서 클래스 로딩 및 필터링
- [ ] 4개 @Nested 그룹 구성
  - [ ] LombokProhibition (6개 규칙)
  - [ ] JpaRelationshipProhibition (4개 규칙)
  - [ ] MethodPatternRules (2개 규칙)
  - [ ] ConstructorAndFactoryRules (4개 규칙)
- [ ] 5개 커스텀 ArchCondition 구현
  - [ ] `notHaveSetterMethods()`
  - [ ] `notHaveBusinessLogicMethods()`
  - [ ] `haveProtectedOrPublicNoArgsConstructor()`
  - [ ] `havePrivateConstructorWithParameters()`
  - [ ] `havePublicStaticOfMethod()`
- [ ] 모든 규칙에 `allowEmptyShould(true)` 추가
- [ ] 빌드 시 자동 실행 확인
- [ ] CI/CD 파이프라인 통합

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0
