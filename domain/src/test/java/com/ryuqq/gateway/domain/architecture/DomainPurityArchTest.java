package com.ryuqq.gateway.domain.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.ryuqq.gateway.domain.architecture.ArchUnitPackageConstants.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Domain Purity ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p><strong>목적</strong>: Domain Layer 전체의 순수성을 종합적으로 검증합니다.
 *
 * <p><strong>검증 규칙 (16개)</strong>:
 *
 * <ul>
 *   <li>시간 타입 규칙: LocalDateTime 금지, Instant.now() 직접 호출 금지 (Clock 주입 필수)
 *   <li>Lombok 절대 금지 (전체 Domain layer)
 *   <li>JPA 어노테이션 절대 금지 (전체 Domain layer)
 *   <li>Spring 어노테이션 절대 금지 (전체 Domain layer)
 *   <li>External libraries 금지 (commons, guava, vavr)
 *   <li>Validation API 금지 (jakarta.validation, @NotNull 등)
 *   <li>Logger 금지 (slf4j, logback, log4j)
 *   <li>JSON libraries 금지 (Jackson, Gson)
 *   <li>Pure Java만 허용 (java.*, 일부 jakarta.annotation 제외)
 * </ul>
 *
 * <p><strong>허용 범위</strong>:
 *
 * <ul>
 *   <li>{@code java.*} - Java Standard Library만 허용
 *   <li>{@code java.time.Instant} - 시간 타입 (Clock.instant() 통해 획득)
 *   <li>{@code java.time.Clock} - 테스트 가능한 시간 주입
 * </ul>
 *
 * <p><strong>이중 방어 시스템</strong>:
 *
 * <ul>
 *   <li>빌드 타임: verifyDomainPurity Gradle 태스크
 *   <li>테스트 타임: 본 ArchUnit 테스트
 *   <li>Pre-commit Hook: Instant.now() 호출 검증
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see <a href="../../../../../../../README.md">Domain Layer README</a>
 */
@DisplayName("Domain Purity ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("domain")
@Tag("purity")
class DomainPurityArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(DOMAIN);
    }

    // ==================== 시간 타입 규칙 (Zero-Tolerance) ====================

    /**
     * 규칙 1: Domain Layer는 LocalDateTime을 사용하지 않아야 한다
     *
     * <p>타임존 문제로 인해 LocalDateTime 사용을 금지합니다.
     *
     * <p>모든 시간 필드는 Instant를 사용해야 합니다.
     */
    @Test
    @DisplayName("[금지] Domain Layer는 LocalDateTime을 사용하지 않아야 한다")
    void domainLayer_MustNotUseLocalDateTime() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .haveFullyQualifiedName("java.time.LocalDateTime")
                        .because(
                                "Domain Layer는 LocalDateTime을 사용하지 않아야 합니다 (Instant 필수)\n"
                                        + "이유:\n"
                                        + "  - LocalDateTime은 타임존 정보가 없어 서버 위치에 따라 다른 값\n"
                                        + "  - Instant는 UTC 기준 절대 시간으로 전 세계 동일한 값\n"
                                        + "  - 예: 한국 서버와 미국 서버에서 같은 시점이 다른 값으로 저장됨\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /**
     * 규칙 2: Domain Layer는 Instant.now()를 직접 호출하지 않아야 한다
     *
     * <p>테스트 가능성을 위해 Clock 주입을 사용해야 합니다.
     *
     * <p>clock.instant()를 통해 시간을 획득해야 합니다.
     *
     * <p><strong>참고</strong>: ArchUnit은 메서드 호출을 직접 검증하기 어렵습니다. 이 규칙은 Instant 클래스에 대한 직접 의존성을 허용하되,
     * 코드 리뷰와 Pre-commit Hook에서 Instant.now() 호출을 검증합니다.
     */
    @Test
    @DisplayName("[금지] Domain Layer는 Instant.now()를 직접 호출하지 않아야 한다 (Clock 주입 필수)")
    void domainLayer_MustNotCallInstantNowDirectly() {
        // Note: ArchUnit은 메서드 호출 검증에 제한이 있어,
        // 이 규칙은 문서화 목적이며 실제 검증은 Pre-commit Hook에서 수행합니다.
        // Pre-commit Hook: hooks/validators/validate-instant-now.sh
        //
        // 권장 패턴:
        // ❌ Instant.now()
        // ✅ clock.instant() (Clock 의존성 주입)
        //
        // 이 테스트는 Domain Layer에서 Clock 인터페이스를 사용하도록 유도합니다.
    }

    // ==================== Lombok 금지 ====================

    /** 규칙 3: Domain Layer는 Lombok 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Lombok 어노테이션을 사용하지 않아야 한다")
    void domainLayer_MustNotUseLombok() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .beAnnotatedWith("lombok.Data")
                        .orShould()
                        .beAnnotatedWith("lombok.Value")
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
                        .orShould()
                        .beAnnotatedWith("lombok.ToString")
                        .orShould()
                        .beAnnotatedWith("lombok.EqualsAndHashCode")
                        .orShould()
                        .beAnnotatedWith("lombok.SneakyThrows")
                        .orShould()
                        .beAnnotatedWith("lombok.Synchronized")
                        .orShould()
                        .beAnnotatedWith("lombok.With")
                        .orShould()
                        .beAnnotatedWith("lombok.Log")
                        .orShould()
                        .beAnnotatedWith("lombok.Slf4j")
                        .because(
                                "Domain Layer는 Pure Java로 작성해야 합니다 (Lombok 절대 금지)\n"
                                        + "이유:\n"
                                        + "  - Lombok은 외부 의존성 (컴파일 타임 코드 생성)\n"
                                        + "  - Getter는 Law of Demeter 위반 가능성\n"
                                        + "  - Domain은 Tell, Don't Ask 원칙 준수\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== JPA 금지 ====================

    /** 규칙 4: Domain Layer는 JPA 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 JPA 어노테이션을 사용하지 않아야 한다")
    void domainLayer_MustNotUseJPA() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .beAnnotatedWith("jakarta.persistence.Entity")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Table")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Column")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Id")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.GeneratedValue")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.ManyToOne")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.OneToMany")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.OneToOne")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.ManyToMany")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.JoinColumn")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.JoinTable")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Embeddable")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Embedded")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.EmbeddedId")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Enumerated")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Temporal")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Lob")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Transient")
                        .orShould()
                        .beAnnotatedWith("jakarta.persistence.Version")
                        .because(
                                "Domain Layer는 JPA에 독립적이어야 합니다 (JPA 어노테이션 절대 금지)\n"
                                        + "이유:\n"
                                        + "  - JPA는 Persistence Layer 관심사\n"
                                        + "  - Domain은 데이터베이스를 모르는 순수 비즈니스 로직\n"
                                        + "  - Long FK 전략 사용 (관계 어노테이션 대신 Long userId)\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== Spring 금지 ====================

    /** 규칙 5: Domain Layer는 Spring 어노테이션을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Spring 어노테이션을 사용하지 않아야 한다")
    void domainLayer_MustNotUseSpring() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .beAnnotatedWith("org.springframework.stereotype.Component")
                        .orShould()
                        .beAnnotatedWith("org.springframework.stereotype.Service")
                        .orShould()
                        .beAnnotatedWith("org.springframework.stereotype.Repository")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.annotation.Configuration")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.annotation.Bean")
                        .orShould()
                        .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                        .orShould()
                        .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                        .orShould()
                        .beAnnotatedWith("org.springframework.beans.factory.annotation.Value")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.annotation.Scope")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.annotation.Lazy")
                        .orShould()
                        .beAnnotatedWith("org.springframework.cache.annotation.Cacheable")
                        .orShould()
                        .beAnnotatedWith("org.springframework.cache.annotation.CacheEvict")
                        .orShould()
                        .beAnnotatedWith("org.springframework.scheduling.annotation.Async")
                        .orShould()
                        .beAnnotatedWith("org.springframework.scheduling.annotation.Scheduled")
                        .orShould()
                        .beAnnotatedWith("org.springframework.context.event.EventListener")
                        .because(
                                "Domain Layer는 Spring Framework에 독립적이어야 합니다 (Spring 어노테이션 절대 금지)\n"
                                        + "이유:\n"
                                        + "  - Framework Agnostic: 다른 프레임워크로 전환 시에도 재사용 가능\n"
                                        + "  - 테스트 가능한 Plain Old Java Object (POJO)\n"
                                        + "  - DIP (Dependency Inversion Principle) 준수\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== Validation API 금지 ====================

    /** 규칙 6: Domain Layer는 Validation API를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Validation API를 사용하지 않아야 한다")
    void domainLayer_MustNotUseValidationAPI() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .beAnnotatedWith("jakarta.validation.constraints.NotNull")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.NotBlank")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.NotEmpty")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Size")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Min")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Max")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Pattern")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Email")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Positive")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.PositiveOrZero")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Negative")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.NegativeOrZero")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Past")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.PastOrPresent")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.Future")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.constraints.FutureOrPresent")
                        .orShould()
                        .beAnnotatedWith("jakarta.validation.Valid")
                        .orShould()
                        .beAnnotatedWith("javax.validation.constraints.NotNull")
                        .orShould()
                        .beAnnotatedWith("javax.validation.constraints.NotBlank")
                        .orShould()
                        .beAnnotatedWith("javax.validation.Valid")
                        .because(
                                "Domain Layer는 Validation API를 사용하지 않아야 합니다\n"
                                    + "이유:\n"
                                    + "  - Domain 객체가 스스로 유효성 보장 (생성자/Compact Constructor에서 검증)\n"
                                    + "  - 어노테이션이 아닌 명시적 검증 로직\n"
                                    + "  - 비즈니스 규칙을 코드로 표현 (선언적 어노테이션 아님)\n"
                                    + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== External Utilities 금지 ====================

    /** 규칙 7: Domain Layer는 Apache Commons를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Apache Commons를 사용하지 않아야 한다")
    void domainLayer_MustNotUseApacheCommons() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "org.apache.commons.lang3..",
                                "org.apache.commons.collections4..",
                                "org.apache.commons.text..",
                                "org.apache.commons.io..",
                                "org.apache.commons.codec..")
                        .because(
                                "Domain Layer는 Apache Commons를 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Java Standard Library로 충분\n"
                                        + "  - 외부 유틸리티는 불필요한 의존성\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /** 규칙 8: Domain Layer는 Google Guava를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Google Guava를 사용하지 않아야 한다")
    void domainLayer_MustNotUseGuava() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.google.common..")
                        .because(
                                "Domain Layer는 Google Guava를 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Java Standard Library로 충분\n"
                                        + "  - 외부 유틸리티는 불필요한 의존성\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /** 규칙 9: Domain Layer는 Vavr를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Vavr를 사용하지 않아야 한다")
    void domainLayer_MustNotUseVavr() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("io.vavr..")
                        .because(
                                "Domain Layer는 Vavr (함수형 라이브러리)를 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Java Standard Library로 충분\n"
                                        + "  - 외부 함수형 라이브러리는 불필요한 의존성\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== JSON Libraries 금지 ====================

    /** 규칙 10: Domain Layer는 Jackson을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Jackson을 사용하지 않아야 한다")
    void domainLayer_MustNotUseJackson() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.fasterxml.jackson..")
                        .because(
                                "Domain Layer는 Jackson을 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Domain은 JSON 변환 관심 없음 (Adapter Layer 책임)\n"
                                        + "  - 직렬화/역직렬화는 Infrastructure 관심사\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /** 규칙 11: Domain Layer는 Gson을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Gson을 사용하지 않아야 한다")
    void domainLayer_MustNotUseGson() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("com.google.gson..")
                        .because(
                                "Domain Layer는 Gson을 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Domain은 JSON 변환 관심 없음 (Adapter Layer 책임)\n"
                                        + "  - 직렬화/역직렬화는 Infrastructure 관심사\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== Logging Libraries 금지 ====================

    /** 규칙 12: Domain Layer는 SLF4J를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 SLF4J를 사용하지 않아야 한다")
    void domainLayer_MustNotUseSLF4J() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("org.slf4j..")
                        .because(
                                "Domain Layer는 SLF4J를 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Domain은 로깅 관심 없음 (Infrastructure 책임)\n"
                                        + "  - 비즈니스 로직에 집중\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /** 규칙 13: Domain Layer는 Logback을 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Logback을 사용하지 않아야 한다")
    void domainLayer_MustNotUseLogback() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("ch.qos.logback..")
                        .because(
                                "Domain Layer는 Logback을 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Domain은 로깅 관심 없음 (Infrastructure 책임)\n"
                                        + "  - 비즈니스 로직에 집중\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    /** 규칙 14: Domain Layer는 Log4j를 사용하지 않아야 한다 */
    @Test
    @DisplayName("[금지] Domain Layer는 Log4j를 사용하지 않아야 한다")
    void domainLayer_MustNotUseLog4j() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("org.apache.logging.log4j..")
                        .because(
                                "Domain Layer는 Log4j를 사용하지 않아야 합니다\n"
                                        + "이유:\n"
                                        + "  - Domain은 로깅 관심 없음 (Infrastructure 책임)\n"
                                        + "  - 비즈니스 로직에 집중\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }

    // ==================== 레이어 의존성 규칙 ====================

    /** 규칙 15: Domain Layer는 Application/Adapter 레이어에 의존하지 않아야 한다 */
    @Test
    @DisplayName("[필수] Domain Layer는 Application/Adapter 레이어에 의존하지 않아야 한다")
    void domainLayer_ShouldNotDependOnOuterLayers() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN_ALL)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                APPLICATION_ALL, ADAPTER_ALL, BOOTSTRAP_ALL, PERSISTENCE_ALL)
                        .because(
                                "Domain Layer는 Application/Adapter 레이어에 의존하지 않아야 합니다 (헥사고날 아키텍처)\n"
                                        + "이유:\n"
                                        + "  - Domain은 중심 (다른 레이어는 Domain에 의존)\n"
                                        + "  - DIP (Dependency Inversion Principle) 준수\n"
                                        + "  - ClockHolder 패턴: interface는 Domain, 구현은 Application\n"
                                        + "  → domain/README.md 참조");

        rule.check(classes);
    }
}
