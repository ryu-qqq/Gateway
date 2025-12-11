package com.ryuqq.gateway.application.architecture.factory;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.ryuqq.gateway.application.architecture.ArchUnitPackageConstants;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * ClockHolder 의존성 제어 ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: ClockHolder는 CommandFactory에서만 직접 사용, 다른 컴포넌트는 Factory를 통해 접근
 *
 * <h3>ClockHolder 의존성 규칙:</h3>
 *
 * <ul>
 *   <li>CommandFactory: ClockHolder 직접 의존 허용 (유일한 진입점)
 *   <li>Service, Assembler, Facade, Manager, Listener, Strategy: ClockHolder 직접 의존 금지
 *   <li>Clock이 필요한 경우 CommandFactory.getClock() 또는 Factory 메서드를 통해 접근
 * </ul>
 *
 * <h3>호출 흐름:</h3>
 *
 * <pre>
 * Service/Facade/Manager → CommandFactory → ClockHolder → Clock
 *                                        └→ Domain Aggregate (상태 변경)
 * </pre>
 *
 * <h3>이점:</h3>
 *
 * <ul>
 *   <li>시간 관련 로직의 단일 진입점으로 테스트 용이성 향상
 *   <li>일관된 시간 처리 보장
 *   <li>향후 시간 정책 변경 시 Factory만 수정
 * </ul>
 */
@DisplayName("ClockHolder Dependency ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("factory")
@Tag("clockholder")
class ClockHolderDependencyArchTest {

    private static JavaClasses classes;

    private static final String CLOCK_HOLDER_CLASS =
            "com.ryuqq.gateway.domain.common.util.ClockHolder";

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(ArchUnitPackageConstants.APPLICATION);
    }

    // ==================== Service ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Service ClockHolder 의존성 금지")
    class ServiceClockHolderRules {

        @Test
        @DisplayName("[금지] CommandService는 ClockHolder를 직접 의존하지 않아야 한다")
        void commandService_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..service.command..")
                            .and()
                            .haveSimpleNameEndingWith("Service")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "CommandService는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }

        @Test
        @DisplayName("[금지] QueryService는 ClockHolder를 직접 의존하지 않아야 한다")
        void queryService_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..service.query..")
                            .and()
                            .haveSimpleNameEndingWith("Service")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "QueryService는 ClockHolder를 직접 의존하지 않아야 합니다. "
                                            + "QueryFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Assembler ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Assembler ClockHolder 의존성 금지")
    class AssemblerClockHolderRules {

        @Test
        @DisplayName("[금지] Assembler는 ClockHolder를 직접 의존하지 않아야 한다")
        void assembler_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..assembler..")
                            .and()
                            .haveSimpleNameEndingWith("Assembler")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "Assembler는 ClockHolder를 직접 의존하지 않아야 합니다. "
                                            + "CommandFactory를 통해 접근하세요. "
                                            + "Assembler는 Domain → Response 변환만 담당합니다.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Facade ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Facade ClockHolder 의존성 금지")
    class FacadeClockHolderRules {

        @Test
        @DisplayName("[금지] Facade는 ClockHolder를 직접 의존하지 않아야 한다")
        void facade_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..facade..")
                            .and()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "Facade는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Manager ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Manager ClockHolder 의존성 금지")
    class ManagerClockHolderRules {

        @Test
        @DisplayName("[금지] Manager는 ClockHolder를 직접 의존하지 않아야 한다")
        void manager_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..manager..")
                            .and()
                            .haveSimpleNameEndingWith("Manager")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "Manager는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Listener ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Listener ClockHolder 의존성 금지")
    class ListenerClockHolderRules {

        @Test
        @DisplayName("[금지] EventListener는 ClockHolder를 직접 의존하지 않아야 한다")
        void listener_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..listener..")
                            .and()
                            .haveSimpleNameEndingWith("Listener")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "EventListener는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Strategy ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Strategy ClockHolder 의존성 금지")
    class StrategyClockHolderRules {

        @Test
        @DisplayName("[금지] Strategy는 ClockHolder를 직접 의존하지 않아야 한다")
        void strategy_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..strategy..")
                            .and()
                            .haveSimpleNameEndingWith("Strategy")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "Strategy는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }

    // ==================== Scheduler ClockHolder 금지 규칙 ====================

    @Nested
    @DisplayName("Scheduler ClockHolder 의존성 금지")
    class SchedulerClockHolderRules {

        @Test
        @DisplayName("[금지] Scheduler는 ClockHolder를 직접 의존하지 않아야 한다")
        void scheduler_MustNotDependOnClockHolder() {
            ArchRule rule =
                    noClasses()
                            .that()
                            .resideInAPackage("..application..scheduler..")
                            .and()
                            .haveSimpleNameEndingWith("Scheduler")
                            .should()
                            .dependOnClassesThat()
                            .haveFullyQualifiedName(CLOCK_HOLDER_CLASS)
                            .because(
                                    "Scheduler는 ClockHolder를 직접 의존하지 않아야 합니다."
                                        + " CommandFactory.getClock() 또는 Factory 메서드를 통해 접근하세요.");

            rule.allowEmptyShould(true).check(classes);
        }
    }
}
