# Facade ArchUnit — **자동 검증 규칙**

> Facade의 **아키텍처 규칙을 ArchUnit으로 자동 검증**합니다.
>
> 빌드 시 규칙 위반이 감지되면 **빌드 실패**로 강제합니다.

---

## 1) 검증 규칙 요약

| 카테고리 | 규칙 | 심각도 |
|---------|------|--------|
| **기본 구조** | @Component 어노테이션 필수 | 필수 |
| **기본 구조** | *Facade 접미사 필수 | 필수 |
| **기본 구조** | facade 패키지 위치 | 필수 |
| **기본 구조** | final 클래스 금지 | 필수 |
| **메서드** | persist* 메서드 네이밍 | 권장 |
| **금지** | @Transactional 클래스 레벨 금지 | 필수 |
| **금지** | @Service 금지 | 필수 |
| **금지** | Lombok 금지 | 필수 |
| **금지** | Port 의존 금지 | 필수 |
| **의존성** | Application/Domain Layer만 의존 | 필수 |

---

## 2) ArchUnit 테스트 코드

### 파일 위치

```
application/src/test/java/
└─ com/ryuqq/application/architecture/facade/
   └─ FacadeArchTest.java
```

### 전체 코드

```java
package com.ryuqq.application.architecture.facade;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Facade ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Facade는 여러 Manager 조합만, 비즈니스 로직 금지</p>
 */
@DisplayName("Facade ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("facade")
class FacadeArchTest {

    private static JavaClasses classes;
    private static boolean hasFacadeClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");

        hasFacadeClasses = classes.stream()
            .anyMatch(javaClass -> javaClass.getSimpleName().endsWith("Facade"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] Facade는 @Component 어노테이션을 가져야 한다")
        void facade_MustHaveComponentAnnotation() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().beAnnotatedWith(Component.class)
                .because("Facade는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] facade 패키지의 클래스는 'Facade' 접미사를 가져야 한다")
        void facade_MustHaveCorrectSuffix() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..facade..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .should().haveSimpleNameEndingWith("Facade")
                .because("facade 패키지의 클래스는 'Facade' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 ..application..facade.. 패키지에 위치해야 한다")
        void facade_MustBeInCorrectPackage() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().resideInAPackage("..application..facade..")
                .because("Facade는 application.*.facade 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 final 클래스가 아니어야 한다")
        void facade_MustNotBeFinal() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().notHaveModifier(FINAL)
                .because("Spring 프록시 생성을 위해 Facade가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 메서드 규칙 ====================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[권장] Facade의 public 메서드는 persist로 시작해야 한다")
        void facade_MethodsShouldStartWithPersist() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Facade")
                .and().arePublic()
                .and().doNotHaveFullName(".*<init>.*")
                .should().haveNameMatching("persist.*")
                .because("Facade 메서드는 persist*() 네이밍을 권장합니다 (save, create 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade의 @Transactional 메서드는 메서드 레벨에서만 허용")
        void facade_TransactionalOnMethodLevelOnly() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // 메서드 레벨 @Transactional 존재 확인 (권장)
            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Facade")
                .and().areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .should().bePublic()
                .because("Facade의 @Transactional은 public 메서드에서만 유효합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] Facade는 @Service 어노테이션을 가지지 않아야 한다")
        void facade_MustNotHaveServiceAnnotation() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("Facade는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 클래스 레벨 @Transactional을 가지지 않아야 한다")
        void facade_MustNotHaveClassLevelTransactional() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("Facade는 클래스 레벨 @Transactional 금지. " +
                         "메서드 단위로 트랜잭션을 관리해야 합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 Lombok 어노테이션을 가지지 않아야 한다")
        void facade_MustNotUseLombok() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .because("Facade는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 Port 인터페이스를 직접 의존하지 않아야 한다")
        void facade_MustNotDependOnPorts() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .should().dependOnClassesThat().haveNameMatching(".*Port")
                .because("Facade는 Port를 직접 주입받지 않습니다. " +
                         "TransactionManager를 통해 Port에 접근합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 Repository를 직접 의존하지 않아야 한다")
        void facade_MustNotDependOnRepositories() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("Facade는 Repository를 직접 주입받지 않습니다. " +
                         "TransactionManager를 통해 접근합니다.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[필수] Facade는 Application Layer와 Domain Layer만 의존해야 한다")
        void facade_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.ryuqq.application..",
                    "com.ryuqq.domain..",
                    "org.springframework..",
                    "java..",
                    "jakarta.."
                )
                .because("Facade는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 TransactionManager에 의존해야 한다")
        void facade_MustDependOnTransactionManager() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Facade")
                .should().dependOnClassesThat().haveNameMatching(".*TransactionManager")
                .because("Facade는 TransactionManager를 조합해야 합니다");

            rule.check(classes);
        }
    }
}
```

---

## 3) 규칙 상세 설명

### 기본 구조 규칙

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `@Component` 필수 | Spring Bean 등록 | 빌드 실패 |
| `*Facade` 접미사 | 일관된 네이밍 | 빌드 실패 |
| `facade` 패키지 | 올바른 위치 | 빌드 실패 |
| `final` 금지 | 프록시 생성 | 빌드 실패 |

### 메서드 규칙

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `persist*` 네이밍 | save, create 금지 | 경고 |
| `@Transactional` 메서드 레벨 | 클래스 레벨 금지 | 빌드 실패 |

### 금지 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| `@Service` 금지 | `@Component` 사용 | 빌드 실패 |
| 클래스 레벨 `@Transactional` 금지 | 메서드 단위 관리 | 빌드 실패 |
| `Lombok` 금지 | Plain Java | 빌드 실패 |
| `Port` 직접 의존 금지 | Manager 통해 접근 | 빌드 실패 |
| `Repository` 직접 의존 금지 | Manager 통해 접근 | 빌드 실패 |

### 의존성 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| Layer 제한 | 아키텍처 준수 | 빌드 실패 |
| Manager 의존 필수 | Facade 핵심 역할 | 빌드 실패 |

---

## 4) Facade vs TransactionManager 비교

| 구분 | TransactionManager | Facade |
|------|---------------------|--------|
| **역할** | 단일 Port 트랜잭션 | 여러 Manager 조합 |
| **의존성** | Persistence Port 1개 | Manager 2개 이상 |
| **어노테이션** | `@Component` | `@Component` |
| **트랜잭션** | 메서드 단위 `@Transactional` | 메서드 단위 `@Transactional` |
| **ArchUnit** | Port 의존 허용 | Port 의존 금지 |

---

## 5) 관련 문서

- **[Facade Guide](./facade-guide.md)** - Facade 구현 가이드
- **[Facade Test Guide](./facade-test-guide.md)** - 테스트 가이드
- **[Transaction Manager ArchUnit](../manager/transaction-manager-archunit.md)** - Manager 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
