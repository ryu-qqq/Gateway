# Scheduler ArchUnit Guide — **아키텍처 규칙 자동 검증**

> Scheduler의 아키텍처 규칙을 ArchUnit으로 자동 검증합니다.
>
> **Port 직접 의존 금지**, **UseCase 통한 위임 강제**, **네이밍 규칙** 등을 검증합니다.

---

## 1) 검증 대상 규칙

| 규칙 | 설명 | 심각도 |
|------|------|--------|
| **UseCase 의존 필수** | Scheduler는 UseCase/Service만 의존 | 🔴 Critical |
| **Port 직접 의존 금지** | QueryPort, PersistencePort 직접 의존 금지 | 🔴 Critical |
| **패키지 위치** | `scheduler/` 패키지에 위치 | 🟡 Important |
| **네이밍 규칙** | `*Scheduler` 클래스명 | 🟡 Important |
| **어노테이션 규칙** | `@Component` + `@ConditionalOnProperty` | 🟡 Important |
| **Lombok 금지** | `@Data`, `@Builder` 등 금지 | 🟡 Important |

---

## 2) ArchUnit 테스트

```java
package com.ryuqq.application.architecture.scheduler;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Scheduler ArchUnit Test
 *
 * <p>Scheduler의 아키텍처 규칙을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Scheduler ArchUnit 검증")
class SchedulerArchTest {

    private static JavaClasses applicationClasses;

    @BeforeAll
    static void setUp() {
        applicationClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ryuqq.application");
    }

    // =========================================================================
    // 1. 패키지 및 네이밍 규칙
    // =========================================================================

    @Nested
    @DisplayName("패키지 및 네이밍 규칙")
    class PackageAndNamingRules {

        @Test
        @DisplayName("Scheduler 클래스는 scheduler 패키지에 위치해야 한다")
        void scheduler_should_be_in_scheduler_package() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().resideInAPackage("..scheduler..")
                    .because("Scheduler 클래스는 scheduler 패키지에 위치해야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("scheduler 패키지의 클래스는 Scheduler로 끝나야 한다")
        void classes_in_scheduler_package_should_end_with_Scheduler() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..scheduler..")
                    .should().haveSimpleNameEndingWith("Scheduler")
                    .because("scheduler 패키지의 클래스는 Scheduler로 끝나야 합니다");

            rule.check(applicationClasses);
        }
    }

    // =========================================================================
    // 2. 의존성 규칙 (가장 중요!)
    // =========================================================================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("Scheduler는 QueryPort를 직접 의존하면 안 된다")
        void scheduler_should_not_depend_on_query_port_directly() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("QueryPort")
                    .because("Scheduler는 UseCase/Service를 통해 조회해야 합니다. "
                            + "QueryPort 직접 의존은 CQRS 원칙 위반입니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 PersistencePort를 직접 의존하면 안 된다")
        void scheduler_should_not_depend_on_persistence_port_directly() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("PersistencePort")
                    .because("Scheduler는 UseCase/Service를 통해 저장해야 합니다. "
                            + "PersistencePort 직접 의존은 아키텍처 위반입니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 ReadManager를 직접 의존하면 안 된다")
        void scheduler_should_not_depend_on_read_manager_directly() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("ReadManager")
                    .because("Scheduler는 UseCase/Service를 통해 조회해야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 TransactionManager를 직접 의존하면 안 된다")
        void scheduler_should_not_depend_on_transaction_manager_directly() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("TransactionManager")
                    .because("Scheduler는 UseCase/Service를 통해 저장해야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 UseCase 또는 Service를 의존해야 한다")
        void scheduler_should_depend_on_usecase_or_service() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("UseCase")
                    .orShould().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Service")
                    .because("Scheduler는 UseCase/Service를 통해 비즈니스 로직을 위임해야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 DistributedLockPort를 의존할 수 있다")
        void scheduler_can_depend_on_distributed_lock_port() {
            // 분산 락은 Scheduler에서 직접 사용 가능 (인프라 관심사)
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("DistributedLockPort")
                    .orShould().dependOnClassesThat()
                    .haveSimpleNameEndingWith("LockPort")
                    .allowEmptyShould(true)  // 없어도 OK
                    .because("분산 락은 Scheduler에서 직접 사용 가능합니다");

            rule.check(applicationClasses);
        }
    }

    // =========================================================================
    // 3. 어노테이션 규칙
    // =========================================================================

    @Nested
    @DisplayName("어노테이션 규칙")
    class AnnotationRules {

        @Test
        @DisplayName("Scheduler는 @Component 어노테이션이 있어야 한다")
        void scheduler_should_have_component_annotation() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                    .because("Scheduler는 @Component로 Bean 등록되어야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 @ConditionalOnProperty 어노테이션이 있어야 한다")
        void scheduler_should_have_conditional_on_property_annotation() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(
                            org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class)
                    .because("Scheduler는 @ConditionalOnProperty로 활성화 제어가 가능해야 합니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler에 @Transactional이 있으면 안 된다")
        void scheduler_should_not_have_transactional_annotation() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(
                            org.springframework.transaction.annotation.Transactional.class)
                    .because("Scheduler는 트랜잭션 경계를 가지면 안 됩니다. "
                            + "트랜잭션은 UseCase/Service에서 관리합니다");

            rule.check(applicationClasses);
        }
    }

    // =========================================================================
    // 4. Lombok 금지
    // =========================================================================

    @Nested
    @DisplayName("Lombok 금지 규칙")
    class LombokProhibitionRules {

        @Test
        @DisplayName("Scheduler에서 @Data 사용 금지")
        void scheduler_should_not_use_lombok_data() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(lombok.Data.class)
                    .because("Scheduler에서 Lombok 사용이 금지됩니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler에서 @Builder 사용 금지")
        void scheduler_should_not_use_lombok_builder() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(lombok.Builder.class)
                    .because("Scheduler에서 Lombok 사용이 금지됩니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler에서 @RequiredArgsConstructor 사용 금지")
        void scheduler_should_not_use_lombok_required_args_constructor() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().beAnnotatedWith(lombok.RequiredArgsConstructor.class)
                    .because("Scheduler에서 Lombok 사용이 금지됩니다. "
                            + "생성자를 직접 작성하세요");

            rule.check(applicationClasses);
        }
    }

    // =========================================================================
    // 5. 메서드 규칙
    // =========================================================================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("Scheduler는 @Scheduled 메서드를 가져야 한다")
        void scheduler_should_have_scheduled_method() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().containAnyMethodsThat(method ->
                            method.isAnnotatedWith(org.springframework.scheduling.annotation.Scheduled.class))
                    .because("Scheduler는 @Scheduled 메서드를 가져야 합니다");

            rule.check(applicationClasses);
        }
    }

    // =========================================================================
    // 6. 레이어 분리 규칙
    // =========================================================================

    @Nested
    @DisplayName("레이어 분리 규칙")
    class LayerSeparationRules {

        @Test
        @DisplayName("Scheduler는 adapter 패키지를 의존하면 안 된다")
        void scheduler_should_not_depend_on_adapter_layer() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..")
                    .because("Scheduler는 adapter 레이어를 직접 의존하면 안 됩니다");

            rule.check(applicationClasses);
        }

        @Test
        @DisplayName("Scheduler는 Controller를 의존하면 안 된다")
        void scheduler_should_not_depend_on_controller() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Scheduler")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Controller")
                    .because("Scheduler는 Controller를 의존하면 안 됩니다");

            rule.check(applicationClasses);
        }
    }
}
```

---

## 3) 테스트 실행

```bash
# Scheduler ArchUnit 테스트만 실행
./gradlew :application:test --tests "*SchedulerArchTest*"

# 전체 ArchUnit 테스트 실행
./gradlew :application:test --tests "*ArchTest*"
```

---

## 4) 위반 시 메시지 예시

### Port 직접 의존 위반

```
Architecture Violation:
Rule 'Scheduler는 QueryPort를 직접 의존하면 안 된다' was violated (1 times):

Class <com.ryuqq.application.download.scheduler.ExternalDownloadOutBoxRetryScheduler>
depends on class <com.ryuqq.application.download.port.out.query.ExternalDownloadOutboxQueryPort>

Scheduler는 UseCase/Service를 통해 조회해야 합니다. QueryPort 직접 의존은 CQRS 원칙 위반입니다
```

### @Transactional 사용 위반

```
Architecture Violation:
Rule 'Scheduler에 @Transactional이 있으면 안 된다' was violated (1 times):

Class <com.ryuqq.application.order.scheduler.OrderCleanupScheduler>
is annotated with @Transactional

Scheduler는 트랜잭션 경계를 가지면 안 됩니다. 트랜잭션은 UseCase/Service에서 관리합니다
```

---

## 5) 허용되는 의존성

### Scheduler가 의존 가능한 클래스

| 의존 대상 | 허용 여부 | 이유 |
|-----------|----------|------|
| `*UseCase` | ✅ 허용 | 비즈니스 위임 |
| `*Service` | ✅ 허용 | 비즈니스 위임 |
| `DistributedLockPort` | ✅ 허용 | 분산 락 (인프라) |
| `MeterRegistry` | ✅ 허용 | 메트릭 기록 |
| `Logger` | ✅ 허용 | 로깅 |
| `*QueryPort` | ❌ 금지 | UseCase 통해야 함 |
| `*PersistencePort` | ❌ 금지 | UseCase 통해야 함 |
| `*ReadManager` | ❌ 금지 | UseCase 통해야 함 |
| `*TransactionManager` | ❌ 금지 | UseCase 통해야 함 |
| `*Assembler` | ❌ 금지 | Service에서 사용 |

---

## 6) 체크리스트

- [ ] `SchedulerArchTest.java` 작성
- [ ] 패키지 및 네이밍 규칙 검증
- [ ] **Port 직접 의존 금지** 검증 (가장 중요!)
- [ ] **UseCase 의존 필수** 검증
- [ ] 어노테이션 규칙 검증 (`@Component`, `@ConditionalOnProperty`)
- [ ] `@Transactional` 금지 검증
- [ ] Lombok 금지 검증
- [ ] CI/CD에 ArchUnit 테스트 포함

---

## 📖 관련 문서

- **[Scheduler Guide](./scheduler-guide.md)** - Scheduler 구현 가이드
- **[Scheduler Test Guide](./scheduler-test-guide.md)** - 테스트 가이드
- **[Application Layer Guide](../application-guide.md)** - 전체 아키텍처

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
