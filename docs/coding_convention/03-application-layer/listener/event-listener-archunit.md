# Event Listener ArchUnit — **자동 검증 규칙**

> Event Listener의 **아키텍처 규칙을 ArchUnit으로 자동 검증**합니다.
>
> 빌드 시 규칙 위반이 감지되면 **빌드 실패**로 강제합니다.

---

## 1) 검증 규칙 요약

| 카테고리 | 규칙 | 심각도 |
|---------|------|--------|
| **기본 구조** | @Component 어노테이션 필수 | 필수 |
| **기본 구조** | *EventListener 접미사 필수 | 필수 |
| **기본 구조** | listener 패키지 위치 | 필수 |
| **기본 구조** | final 클래스 금지 | 필수 |
| **메서드** | public 메서드에 @EventListener 필수 | 필수 |
| **메서드** | handle* 네이밍 | 권장 |
| **금지** | @Transactional 금지 | 필수 |
| **금지** | Port/Repository 의존 금지 | 필수 |
| **금지** | Lombok 금지 | 필수 |
| **금지** | @Service 금지 | 필수 |
| **금지** | @Async 금지 | 필수 |
| **의존성** | Application/Domain Layer만 의존 | 필수 |

---

## 2) ArchUnit 테스트 코드

### 파일 위치

```
application/src/test/java/
└─ com/ryuqq/application/architecture/listener/
   └─ EventListenerArchTest.java
```

### 전체 코드

```java
package com.ryuqq.application.architecture.listener;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Event Listener ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Listener는 부가 작업만 처리, 예외 전파 금지</p>
 */
@DisplayName("EventListener ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("listener")
class EventListenerArchTest {

    private static JavaClasses classes;
    private static boolean hasListenerClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");

        hasListenerClasses = classes.stream()
            .anyMatch(javaClass -> javaClass.getSimpleName().endsWith("EventListener"));
    }

    // ==================== 기본 구조 규칙 ====================

    @Nested
    @DisplayName("기본 구조 규칙")
    class BasicStructureRules {

        @Test
        @DisplayName("[필수] EventListener는 @Component 어노테이션을 가져야 한다")
        void eventListener_MustHaveComponentAnnotation() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().beAnnotatedWith(Component.class)
                .because("EventListener는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] listener 패키지의 클래스는 'EventListener' 접미사를 가져야 한다")
        void listener_MustHaveCorrectSuffix() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().resideInAPackage("..application..listener..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .should().haveSimpleNameEndingWith("EventListener")
                .because("Listener는 'EventListener' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventListener는 ..application..listener.. 패키지에 위치해야 한다")
        void eventListener_MustBeInCorrectPackage() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().resideInAPackage("..application..listener..")
                .because("EventListener는 application.*.listener 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventListener는 final 클래스가 아니어야 한다")
        void eventListener_MustNotBeFinal() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().notHaveModifier(FINAL)
                .because("Spring 프록시 생성을 위해 EventListener가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 메서드 규칙 ====================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[권장] EventListener의 @EventListener 메서드는 handle로 시작해야 한다")
        void eventListener_MethodsShouldStartWithHandle() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("EventListener")
                .and().areAnnotatedWith(EventListener.class)
                .should().haveNameMatching("handle.*")
                .because("EventListener 메서드는 handle*() 네이밍을 권장합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventListener의 public 메서드는 @EventListener를 가져야 한다")
        void eventListener_PublicMethodsMustHaveEventListenerAnnotation() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("EventListener")
                .and().arePublic()
                .and().doNotHaveFullName(".*<init>.*")
                .should().beAnnotatedWith(EventListener.class)
                .because("EventListener의 public 메서드는 @EventListener 어노테이션을 가져야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 금지 규칙 (Zero-Tolerance) ====================

    @Nested
    @DisplayName("금지 규칙 (Zero-Tolerance)")
    class ProhibitionRules {

        @Test
        @DisplayName("[금지] EventListener는 @Service 어노테이션을 가지지 않아야 한다")
        void eventListener_MustNotHaveServiceAnnotation() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .because("EventListener는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventListener는 @Transactional을 가지지 않아야 한다")
        void eventListener_MustNotHaveTransactionalAnnotation() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .because("EventListener는 @Transactional을 가지지 않아야 합니다. " +
                         "트랜잭션이 필요하면 새 트랜잭션을 시작하세요 (REQUIRES_NEW)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventListener는 Lombok 어노테이션을 가지지 않아야 한다")
        void eventListener_MustNotUseLombok() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.Builder")
                .orShould().beAnnotatedWith("lombok.Getter")
                .orShould().beAnnotatedWith("lombok.Setter")
                .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
                .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
                .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
                .because("EventListener는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventListener는 @Async를 가지지 않아야 한다")
        void eventListener_MustNotHaveAsyncAnnotation() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().beAnnotatedWith("org.springframework.scheduling.annotation.Async")
                .because("EventListener는 동기 처리를 기본으로 합니다. " +
                         "비동기가 필요하면 Outbox 패턴을 사용하세요.");

            rule.check(classes);
        }
    }

    // ==================== 의존성 규칙 ====================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("[금지] EventListener는 Port 인터페이스를 의존하지 않아야 한다")
        void eventListener_MustNotDependOnPorts() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().dependOnClassesThat().haveNameMatching(".*Port")
                .because("EventListener는 Port를 주입받지 않아야 합니다. " +
                         "DB 작업이 필요하면 별도 서비스를 호출하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] EventListener는 Repository를 의존하지 않아야 한다")
        void eventListener_MustNotDependOnRepositories() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .because("EventListener는 Repository를 주입받지 않아야 합니다. " +
                         "DB 작업이 필요하면 별도 서비스를 호출하세요.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] EventListener는 Application Layer와 Domain Layer만 의존해야 한다")
        void eventListener_MustOnlyDependOnApplicationAndDomainLayers() {
            assumeTrue(hasListenerClasses, "EventListener 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("EventListener")
                .should().onlyAccessClassesThat()
                .resideInAnyPackage(
                    "com.ryuqq.application..",
                    "com.ryuqq.domain..",
                    "org.springframework..",
                    "org.slf4j..",
                    "java..",
                    "jakarta.."
                )
                .because("EventListener는 Application Layer와 Domain Layer만 의존해야 합니다");

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
| `*EventListener` 접미사 | 일관된 네이밍 | 빌드 실패 |
| `listener` 패키지 | 올바른 위치 | 빌드 실패 |
| `final` 금지 | 프록시 생성 | 빌드 실패 |

### 메서드 규칙

| 규칙 | 설명 | 위반 시 |
|------|------|--------|
| `@EventListener` 존재 | 이벤트 수신 | 빌드 실패 |
| `handle*` 네이밍 | 일관된 네이밍 | 경고 |

### 금지 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| `@Service` 금지 | `@Component` 사용 | 빌드 실패 |
| `@Transactional` 금지 | 커밋 후 실행 | 빌드 실패 |
| `Lombok` 금지 | Plain Java | 빌드 실패 |
| `@Async` 금지 | 동기 처리 기본 | 빌드 실패 |

### 의존성 규칙

| 규칙 | 이유 | 위반 시 |
|------|------|--------|
| `Port` 의존 금지 | 부가 작업만 | 빌드 실패 |
| `Repository` 의존 금지 | 부가 작업만 | 빌드 실패 |
| Layer 제한 | 아키텍처 준수 | 빌드 실패 |

---

## 4) 관련 문서

- **[Event Listener Guide](./event-listener-guide.md)** - Listener 구현 가이드
- **[Event Listener Test Guide](./event-listener-test-guide.md)** - 테스트 가이드
- **[Assembler ArchUnit](../assembler/assembler-archunit.md)** - 참고 패턴

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
