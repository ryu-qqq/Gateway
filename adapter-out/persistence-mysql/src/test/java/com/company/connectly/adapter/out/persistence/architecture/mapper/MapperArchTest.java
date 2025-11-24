package com.company.connectly.adapter.out.persistence.architecture.mapper;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * MapperArchTest - Mapper 아키텍처 규칙 검증
 *
 * <p>mapper-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: @Component 어노테이션 필수</li>
 *   <li>규칙 2: Lombok 사용 금지 (6개 어노테이션)</li>
 *   <li>규칙 3: Static 메서드 금지</li>
 *   <li>규칙 4: 비즈니스 로직 금지</li>
 *   <li>규칙 5: toEntity() 메서드 필수</li>
 *   <li>규칙 6: toDomain() 메서드 필수</li>
 *   <li>규칙 7: Mapper 네이밍 규칙 (*Mapper)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Mapper 아키텍처 규칙 검증 (Zero-Tolerance)")
class MapperArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses mapperClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");

        mapperClasses = allClasses.that(
            DescribedPredicate.describe(
                "are Mapper classes",
                javaClass -> javaClass.getSimpleName().endsWith("Mapper")
            )
        );
    }

    @Test
    @DisplayName("규칙 1: @Component 어노테이션 필수")
    void mapper_MustBeAnnotatedWithComponent() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().beAnnotatedWith(Component.class)
            .because("Mapper는 @Component로 Spring Bean 등록이 필수입니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Data")
    void mapper_MustNotUseLombok_Data() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.Data")
            .because("Mapper는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @AllArgsConstructor")
    void mapper_MustNotUseLombok_AllArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @NoArgsConstructor")
    void mapper_MustNotUseLombok_NoArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @RequiredArgsConstructor")
    void mapper_MustNotUseLombok_RequiredArgsConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.RequiredArgsConstructor")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @Builder")
    void mapper_MustNotUseLombok_Builder() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.Builder")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 2: Lombok 사용 금지 - @UtilityClass")
    void mapper_MustNotUseLombok_UtilityClass() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().notBeAnnotatedWith("lombok.experimental.UtilityClass")
            .because("Mapper는 Lombok 사용이 금지됩니다");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 3: Static 메서드 금지")
    void mapper_MustNotHaveStaticMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
            .and().arePublic()
            .and().haveNameMatching("(toEntity|toDomain|to[A-Z].*)")
            .should().notBeStatic()
            .because("Mapper는 Static 메서드가 금지됩니다 (Spring Bean 주입 필요)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 4: 비즈니스 로직 금지")
    void mapper_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Mapper")
            .and().arePublic()
            .and().haveNameMatching("(validate|calculate|approve|cancel|complete|activate|deactivate).*")
            .should(notExist())
            .because("Mapper는 비즈니스 로직이 금지됩니다 (단순 변환만 담당)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 5: toEntity() 메서드 필수")
    void mapper_MustHaveToEntityMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should(havePublicToEntityMethod())
            .because("Mapper는 toEntity() 메서드가 필수입니다 (Domain → Entity)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 6: toDomain() 메서드 필수")
    void mapper_MustHaveToDomainMethod() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should(havePublicToDomainMethod())
            .because("Mapper는 toDomain() 메서드가 필수입니다 (Entity → Domain)");

        rule.check(mapperClasses);
    }

    @Test
    @DisplayName("규칙 7: Mapper 네이밍 규칙 (*Mapper)")
    void mapper_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Component.class)
            .and().resideInAPackage("..mapper..")
            .should().haveSimpleNameEndingWith("Mapper")
            .because("Mapper 클래스는 *Mapper 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }

    // ===== 커스텀 ArchCondition =====

    /**
     * public toEntity() 메서드 존재 검증
     */
    private static ArchCondition<JavaClass> havePublicToEntityMethod() {
        return new ArchCondition<>("have public toEntity() method") {
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
                        "Class %s does not have a public toEntity() method (required for Domain → Entity conversion)",
                        javaClass.getName()
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
        return new ArchCondition<>("have public toDomain() method") {
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
                        "Class %s does not have a public toDomain() method (required for Entity → Domain conversion)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 메서드가 존재하지 않아야 함을 검증하는 ArchCondition
     */
    private static ArchCondition<JavaMethod> notExist() {
        return new ArchCondition<>("not exist") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String message = String.format(
                    "Method %s.%s() should not exist",
                    method.getOwner().getSimpleName(),
                    method.getName()
                );
                events.add(SimpleConditionEvent.violated(method, message));
            }
        };
    }
}
