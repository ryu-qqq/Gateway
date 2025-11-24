package com.ryuqq.connectly.adapter.out.persistence.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * HikariCPConfigArchTest - HikariCP 설정 규칙 검증
 *
 * <p>HikariCP Connection Pool 설정 관련 아키텍처 규칙을 검증합니다:
 *
 * <p><strong>검증 규칙:</strong>
 *
 * <ul>
 *   <li>규칙 1: DataSourceConfig는 @Configuration 필수
 *   <li>규칙 2: Config 클래스는 config 패키지에 위치
 *   <li>규칙 3: Config 클래스는 public이어야 함
 *   <li>규칙 4: Config 클래스는 final 금지 (프록시 생성)
 *   <li>규칙 5: Config는 Entity/Repository 의존 금지
 *   <li>규칙 6: Config는 Domain/Application Layer 의존 금지
 *   <li>규칙 7: DataSource 설정 클래스는 단일 책임
 * </ul>
 *
 * <p><strong>필수 설정 (application.yml에서 검증):</strong>
 *
 * <ul>
 *   <li>OSIV 비활성화: spring.jpa.open-in-view: false
 *   <li>DDL Auto: spring.jpa.hibernate.ddl-auto: validate
 *   <li>Pool Size: spring.datasource.hikari.maximum-pool-size: 20
 *   <li>Connection Timeout: spring.datasource.hikari.connection-timeout: 30000
 *   <li>Max Lifetime: spring.datasource.hikari.max-lifetime: 1800000
 * </ul>
 *
 * <p><strong>참고:</strong>
 *
 * <ul>
 *   <li>application.yml 설정 검증은 통합 테스트 필요
 *   <li>ArchUnit은 클래스 기반 검증만 가능
 *   <li>Runtime 설정 검증은 별도 테스트 권장
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("HikariCP 설정 규칙 검증 (Zero-Tolerance)")
@Tag("architecture")
class HikariCPConfigArchTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter().importPackages("com.ryuqq.adapter.out.persistence");
    }

    /** 규칙 1: DataSourceConfig는 @Configuration 필수 */
    @Test
    @DisplayName("[필수] DataSourceConfig는 @Configuration 어노테이션을 가져야 한다")
    void dataSourceConfig_MustHaveConfigurationAnnotation() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .and()
                        .resideInAPackage("..config..")
                        .should()
                        .beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                        .because("DataSource/HikariCP 설정 클래스는 @Configuration 어노테이션이 필수입니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 2: Config 클래스는 config 패키지에 위치 */
    @Test
    @DisplayName("[필수] DataSourceConfig는 ..config.. 패키지에 위치해야 한다")
    void dataSourceConfig_MustBeInConfigPackage() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .resideInAPackage("..config..")
                        .because("DataSource/HikariCP 설정 클래스는 config 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 3: Config 클래스는 public이어야 함 */
    @Test
    @DisplayName("[필수] DataSourceConfig는 public이어야 한다")
    void dataSourceConfig_MustBePublic() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .bePublic()
                        .because("Spring Configuration 클래스는 public이어야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 4: Config 클래스는 final 금지 */
    @Test
    @DisplayName("[필수] DataSourceConfig는 final이 아니어야 한다")
    void dataSourceConfig_MustNotBeFinal() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .beAnnotatedWith("final")
                        .because("Spring Configuration 클래스는 프록시 생성을 위해 final이 아니어야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 5: Config는 Entity/Repository 의존 금지 */
    @Test
    @DisplayName("[금지] DataSourceConfig는 Entity/Repository를 의존하지 않아야 한다")
    void dataSourceConfig_MustNotDependOnEntityOrRepository() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..entity..", "..repository..")
                        .because("DataSourceConfig는 순수 설정 클래스로 Entity/Repository를 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 6: Config는 Domain/Application Layer 의존 금지 */
    @Test
    @DisplayName("[금지] DataSourceConfig는 Domain/Application Layer를 의존하지 않아야 한다")
    void dataSourceConfig_MustNotDependOnDomainOrApplication() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..domain..", "..application..")
                        .because(
                                "DataSourceConfig는 Infrastructure Layer 설정으로 Domain/Application을"
                                        + " 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 7: DataSource 설정 클래스는 단일 책임 */
    @Test
    @DisplayName("[권장] DataSourceConfig는 DataSource 설정만 담당해야 한다")
    void dataSourceConfig_ShouldOnlyConfigureDataSource() {
        ArchRule rule =
                classes()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .or()
                        .haveSimpleNameContaining("HikariConfig")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "javax.sql..", "com.zaxxer.hikari..", "org.springframework..")
                        .because(
                                "DataSourceConfig는 DataSource/HikariCP와 Spring Framework 관련 클래스만"
                                        + " 의존해야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 8: Config 클래스는 JPA Config와 분리 */
    @Test
    @DisplayName("[권장] DataSourceConfig는 JPA Config와 분리되어야 한다")
    void dataSourceConfig_ShouldBeSeparateFromJpaConfig() {
        ArchRule rule =
                noClasses()
                        .that()
                        .haveSimpleNameContaining("DataSourceConfig")
                        .should()
                        .haveSimpleNameContaining("Jpa")
                        .because("DataSource 설정과 JPA 설정은 별도 클래스로 분리해야 합니다 (단일 책임)");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 9: Config 패키지는 Adapter/Mapper를 의존하지 않음 */
    @Test
    @DisplayName("[금지] Config 클래스는 Adapter/Mapper를 의존하지 않아야 한다")
    void config_MustNotDependOnAdapterOrMapper() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..config..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("..adapter..", "..mapper..")
                        .because("Config 클래스는 인프라 설정만 담당하며 비즈니스 레이어를 의존하면 안 됩니다");

        rule.allowEmptyShould(true).check(allClasses);
    }

    /** 규칙 10: Config 네이밍 규칙 */
    @Test
    @DisplayName("[권장] Config 클래스는 *Config 네이밍 규칙을 따라야 한다")
    void config_ShouldFollowNamingConvention() {
        ArchRule rule =
                classes()
                        .that()
                        .resideInAPackage("..config..")
                        .and()
                        .areNotInterfaces()
                        .and()
                        .haveSimpleNameNotContaining("Test")
                        .should()
                        .haveSimpleNameEndingWith("Config")
                        .because("Config 클래스는 *Config 네이밍 규칙을 따라야 합니다");

        rule.allowEmptyShould(true).check(allClasses);
    }
}
