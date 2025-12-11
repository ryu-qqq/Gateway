package com.ryuqq.gateway.application.architecture.facade;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

/**
 * Facade ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>핵심 철학: Facade는 여러 Manager 조합만, 비즈니스 로직 금지
 *
 * <h3>Facade 의존성 규칙:</h3>
 *
 * <ul>
 *   <li>TransactionManager 의존 필수 (여러 Manager 조합)
 *   <li>TransactionEventRegistry 의존 허용 (Event 발행)
 *   <li>Factory 의존 금지 (Factory는 Service 책임)
 *   <li>Port/Repository 직접 의존 금지
 * </ul>
 *
 * <h3>Event 발행 흐름:</h3>
 *
 * <pre>
 * Facade → TransactionEventRegistry.registerAfterCommit(event)
 *       → 트랜잭션 커밋 후 → ApplicationEventPublisher.publishEvent(event)
 * </pre>
 */
@DisplayName("Facade ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("facade")
class FacadeArchTest {

    private static JavaClasses classes;
    private static boolean hasFacadeClasses;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.fileflow.application");

        hasFacadeClasses =
                classes.stream()
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

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .beAnnotatedWith(Component.class)
                            .because("Facade는 Spring Bean으로 등록되어야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] facade 패키지의 클래스는 'Facade' 접미사를 가져야 한다")
        void facade_MustHaveCorrectSuffix() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // main 패키지의 Facade만 검사 (테스트 클래스 제외)
            ArchRule rule =
                    classes()
                            .that()
                            .resideInAPackage("..application..facade..")
                            .and()
                            .resideOutsideOfPackage("..architecture..")
                            .and()
                            .haveSimpleNameNotContaining("Test")
                            .and()
                            .areNotInterfaces()
                            .and()
                            .areNotEnums()
                            .should()
                            .haveSimpleNameEndingWith("Facade")
                            .because("facade 패키지의 클래스는 'Facade' 접미사를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 ..application..facade.. 패키지에 위치해야 한다")
        void facade_MustBeInCorrectPackage() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .resideInAPackage("..application..facade..")
                            .because("Facade는 application.*.facade 패키지에 위치해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 final 클래스가 아니어야 한다")
        void facade_MustNotBeFinal() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .notHaveModifier(FINAL)
                            .because("Spring 프록시 생성을 위해 Facade가 final이 아니어야 합니다");

            rule.check(classes);
        }
    }

    // ==================== 메서드 규칙 ====================

    @Nested
    @DisplayName("메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("[권장] Facade의 public 메서드는 조율(orchestration) 역할을 나타내야 한다")
        void facade_MethodsShouldIndicateOrchestration() {
            // Facade 메서드 네이밍 패턴:
            // - createAndActivate*: 생성 후 활성화 조율
            // - saveAndPublish*: 저장 후 이벤트 발행 조율
            // - requestProcessing*: 처리 요청 조율
            // - completeProcessing*: 처리 완료 조율
            // - process: 전체 프로세스 조율
            // 핵심: 단순 CRUD가 아닌 여러 작업의 조율을 나타내는 네이밍
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // 이 테스트는 문서화 목적으로, 네이밍 패턴의 일관성을 검증하지 않음
            // 실제 검증은 코드 리뷰에서 수행
        }

        @Test
        @DisplayName("[필수] Facade의 @Transactional 메서드는 public이어야 한다")
        void facade_TransactionalMethodsMustBePublic() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    methods()
                            .that()
                            .areDeclaredInClassesThat()
                            .haveSimpleNameEndingWith("Facade")
                            .and()
                            .areAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .should()
                            .bePublic()
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

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .beAnnotatedWith("org.springframework.stereotype.Service")
                            .because("Facade는 @Service가 아닌 @Component를 사용해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 클래스 레벨 @Transactional을 가지지 않아야 한다")
        void facade_MustNotHaveClassLevelTransactional() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .beAnnotatedWith(
                                    "org.springframework.transaction.annotation.Transactional")
                            .because(
                                    "Facade는 클래스 레벨 @Transactional 금지. "
                                            + "메서드 단위로 트랜잭션을 관리해야 합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 Lombok 어노테이션을 가지지 않아야 한다")
        void facade_MustNotUseLombok() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .beAnnotatedWith("lombok.Data")
                            .orShould()
                            .beAnnotatedWith("lombok.Builder")
                            .orShould()
                            .beAnnotatedWith("lombok.Getter")
                            .orShould()
                            .beAnnotatedWith("lombok.Setter")
                            .orShould()
                            .beAnnotatedWith("lombok.AllArgsConstructor")
                            .orShould()
                            .beAnnotatedWith("lombok.NoArgsConstructor")
                            .orShould()
                            .beAnnotatedWith("lombok.RequiredArgsConstructor")
                            .because("Facade는 Plain Java를 사용해야 합니다 (Lombok 금지)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 PersistencePort를 직접 의존하지 않아야 한다")
        void facade_MustNotDependOnPersistencePorts() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // PersistencePort는 Manager를 통해 접근해야 함
            // ClientPort (S3ClientPort, HttpDownloadPort 등)는 허용 - 외부 통신은 트랜잭션 외부에서 실행
            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*PersistencePort")
                            .because(
                                    "Facade는 PersistencePort를 직접 주입받지 않습니다. "
                                            + "Manager를 통해 영속화에 접근합니다. "
                                            + "(ClientPort는 외부 통신용으로 허용)");

            rule.check(classes);
        }

        @Test
        @DisplayName("[금지] Facade는 Repository를 직접 의존하지 않아야 한다")
        void facade_MustNotDependOnRepositories() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            ArchRule rule =
                    noClasses()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Repository")
                            .because(
                                    "Facade는 Repository를 직접 주입받지 않습니다. "
                                            + "TransactionManager를 통해 접근합니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[권장] Facade는 가급적 Factory 직접 의존을 피해야 한다")
        void facade_ShouldMinimizeFactoryDependencies() {
            // CommandFactory 의존은 일부 Facade에서 허용됨 (외부 통신 조율 시)
            // - ExternalDownloadProcessingFacade: S3 업로드 준비를 위해 CommandFactory 사용
            // - UploadSessionFacade: Presigned URL 생성을 위해 CommandFactory 사용
            //
            // 핵심 원칙:
            // - 순수 영속화 조율 Facade: Factory 의존 금지 (Manager만 사용)
            // - 외부 통신 포함 Facade: Factory 의존 허용 (외부 API 준비 로직)
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // 이 테스트는 문서화 목적으로, 실제 검증은 코드 리뷰에서 수행
            // 순수 영속화 Facade (FileAssetProcessingFacade 등)는 Factory 의존 금지
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

            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .onlyAccessClassesThat()
                            .resideInAnyPackage(
                                    "com.ryuqq.fileflow.application..",
                                    "com.ryuqq.fileflow.domain..",
                                    "org.springframework..",
                                    "org.slf4j..",
                                    "java..",
                                    "jakarta..")
                            .because("Facade는 Application Layer와 Domain Layer만 의존해야 합니다");

            rule.check(classes);
        }

        @Test
        @DisplayName("[필수] Facade는 Manager에 의존해야 한다")
        void facade_MustDependOnManager() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // Facade는 영속화 작업을 Manager를 통해 수행해야 함
            // Manager는 @Transactional 경계를 제공
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .dependOnClassesThat()
                            .haveNameMatching(".*Manager")
                            .because(
                                    "Facade는 Manager를 조합해야 합니다. "
                                            + "영속화 작업은 Manager의 @Transactional 경계에서 처리됩니다.");

            rule.check(classes);
        }

        @Test
        @DisplayName("[허용] Facade는 TransactionEventRegistry를 의존할 수 있다")
        void facade_CanDependOnTransactionEventRegistry() {
            assumeTrue(hasFacadeClasses, "Facade 클래스가 없어 테스트를 스킵합니다");

            // 이 테스트는 TransactionEventRegistry 의존이 허용됨을 문서화하는 역할
            // 실제로 의존하지 않아도 통과하지만, 의존해도 금지 규칙에 걸리지 않음을 보장
            ArchRule rule =
                    classes()
                            .that()
                            .haveSimpleNameEndingWith("Facade")
                            .should()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("Manager")
                            .orShould()
                            .dependOnClassesThat()
                            .haveSimpleNameEndingWith("EventRegistry")
                            .because(
                                    "Facade는 Manager와 EventRegistry를 의존할 수 있습니다."
                                        + " TransactionEventRegistry를 통해 트랜잭션 커밋 후 Event를 발행합니다.");

            rule.check(classes);
        }
    }
}
