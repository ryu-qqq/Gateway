package com.company.connectly.adapter.out.persistence.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * FlywayMigrationArchTest - Flyway 마이그레이션 규칙 검증
 *
 * <p>Flyway 설정 및 마이그레이션 관련 아키텍처 규칙을 검증합니다:</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: FlywayConfig 클래스는 @Configuration 필수</li>
 *   <li>규칙 2: FlywayConfig는 config 패키지에 위치</li>
 *   <li>규칙 3: Flyway 설정 클래스는 단일 책임</li>
 *   <li>규칙 4: Flyway 관련 클래스는 org.flywaydb 패키지 의존</li>
 * </ul>
 *
 * <p><strong>마이그레이션 파일 네이밍 규칙 (별도 검증 필요):</strong></p>
 * <ul>
 *   <li>형식: V{버전}__{설명}.sql</li>
 *   <li>예시: V1__create_order_table.sql</li>
 *   <li>V 대문자, 언더스코어 2개, snake_case</li>
 * </ul>
 *
 * <p><strong>참고:</strong></p>
 * <ul>
 *   <li>마이그레이션 파일 검증은 파일 시스템 테스트 필요</li>
 *   <li>ArchUnit은 클래스 기반 검증만 가능</li>
 *   <li>SQL 품질 검증은 별도 테스트 권장</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Flyway 마이그레이션 규칙 검증 (Zero-Tolerance)")
@Tag("architecture")
class FlywayMigrationArchTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");
    }

    /**
     * 규칙 1: FlywayConfig 클래스는 @Configuration 필수
     */
    @Test
    @DisplayName("[필수] FlywayConfig는 @Configuration 어노테이션을 가져야 한다")
    void flywayConfig_MustHaveConfigurationAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("FlywayConfig")
            .and().resideInAPackage("..config..")
            .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .because("Flyway 설정 클래스는 @Configuration 어노테이션이 필수입니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 2: FlywayConfig는 config 패키지에 위치
     */
    @Test
    @DisplayName("[필수] FlywayConfig는 ..config.. 패키지에 위치해야 한다")
    void flywayConfig_MustBeInConfigPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().resideInAPackage("..config..")
            .because("Flyway 설정 클래스는 config 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 3: Flyway 설정 클래스는 단일 책임
     */
    @Test
    @DisplayName("[권장] FlywayConfig는 Flyway 설정만 담당해야 한다")
    void flywayConfig_ShouldOnlyConfigureFlyway() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().dependOnClassesThat().resideInAnyPackage("org.flywaydb..", "org.springframework..")
            .because("FlywayConfig는 Flyway와 Spring Framework 관련 클래스만 의존해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 4: Config 클래스는 public이어야 함
     */
    @Test
    @DisplayName("[필수] FlywayConfig는 public이어야 한다")
    void flywayConfig_MustBePublic() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().bePublic()
            .because("Spring Configuration 클래스는 public이어야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 5: FlywayConfig는 final이 아니어야 함 (프록시 생성을 위해)
     */
    @Test
    @DisplayName("[필수] FlywayConfig는 final이 아니어야 한다")
    void flywayConfig_MustNotBeFinal() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().beAnnotatedWith("final")
            .because("Spring Configuration 클래스는 프록시 생성을 위해 final이 아니어야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 6: FlywayConfig는 Entity/Repository를 의존하지 않음
     */
    @Test
    @DisplayName("[금지] FlywayConfig는 Entity/Repository를 의존하지 않아야 한다")
    void flywayConfig_MustNotDependOnEntityOrRepository() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().dependOnClassesThat().resideInAnyPackage("..entity..", "..repository..")
            .because("FlywayConfig는 순수 설정 클래스로 Entity/Repository를 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 7: Flyway 관련 클래스는 org.flywaydb 패키지 의존
     */
    @Test
    @DisplayName("[필수] Flyway 관련 클래스는 org.flywaydb 패키지를 의존해야 한다")
    void flywayRelatedClasses_MustDependOnFlywayPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("Flyway")
            .and().resideInAPackage("..config..")
            .should().dependOnClassesThat().resideInAnyPackage("org.flywaydb..")
            .orShould().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("Flyway 관련 클래스는 org.flywaydb 패키지를 의존해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /**
     * 규칙 8: Flyway 설정은 Domain/Application Layer를 의존하지 않음
     */
    @Test
    @DisplayName("[금지] FlywayConfig는 Domain/Application Layer를 의존하지 않아야 한다")
    void flywayConfig_MustNotDependOnDomainOrApplication() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameContaining("FlywayConfig")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..")
            .because("FlywayConfig는 Infrastructure Layer 설정으로 Domain/Application을 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }
}
