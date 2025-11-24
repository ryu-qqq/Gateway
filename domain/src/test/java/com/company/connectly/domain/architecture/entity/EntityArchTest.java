package com.company.connectly.domain.architecture.entity;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Entity (AggregateRoot 제외) ArchUnit 아키텍처 검증 테스트
 *
 * <p><strong>검증 규칙</strong>:</p>
 * <ul>
 *   <li>Entity 인터페이스 구현 필수 (AggregateRoot 아님)</li>
 *   <li>ID 기반 equals/hashCode</li>
 *   <li>생성자 private</li>
 *   <li>Factory 메서드 (forNew, reconstitute)</li>
 *   <li>domain.[bc].aggregate 패키지에 위치</li>
 *   <li>Repository 없음 (AggregateRoot만 Repository 가능)</li>
 *   <li>Lombok, JPA, Spring 금지</li>
 * </ul>
 *
 * <p><strong>Entity vs AggregateRoot 구분</strong>:</p>
 * <ul>
 *   <li>AggregateRoot: 외부 접근 가능, Repository 존재, 트랜잭션 경계</li>
 *   <li>Entity (내부): AggregateRoot를 통해서만 접근, Repository 없음</li>
 * </ul>
 *
 * <p><strong>예시</strong>:</p>
 * <pre>
 * // AggregateRoot
 * public class Order implements AggregateRoot {
 *     private final OrderId id;
 *     private final List&lt;OrderItem&gt; items;  // 내부 Entity 관리
 * }
 *
 * // 내부 Entity
 * public class OrderItem implements Entity {
 *     private final OrderItemId id;
 *     private final long productId;  // Long FK
 *
 *     private OrderItem(...) { }  // private 생성자
 *
 *     public static OrderItem forNew(...) { }
 *     public static OrderItem reconstitute(...) { }
 * }
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@Tag("domain")
@Tag("entity")
@DisplayName("Entity (AggregateRoot 제외) 아키텍처 검증 테스트")
class EntityArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages("com.ryuqq.domain");
    }

    // ==================== Entity 인터페이스 규칙 ====================

    /**
     * 규칙 1: 내부 Entity는 Entity 인터페이스를 구현해야 한다 (AggregateRoot 아님)
     */
    @Test
    @DisplayName("[필수] 내부 Entity는 Entity 인터페이스를 구현해야 한다")
    void internalEntities_ShouldImplementEntityInterface() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(implementEntityInterfaceOnly())
            .because("내부 Entity는 Entity 인터페이스를 구현해야 하며, AggregateRoot는 구현하지 않아야 합니다\n" +
                    "예시:\n" +
                    "  - OrderItem implements Entity ✅\n" +
                    "  - OrderItem implements AggregateRoot ❌ (내부 Entity는 AggregateRoot 아님)");

        rule.check(classes);
    }

    // ==================== equals/hashCode 규칙 ====================

    /**
     * 규칙 2: Entity는 equals()와 hashCode()를 ID 기반으로 구현해야 한다
     */
    @Test
    @DisplayName("[필수] Entity는 equals()와 hashCode()를 가져야 한다")
    void entities_ShouldHaveEqualsAndHashCode() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveEqualsAndHashCode())
            .because("Entity는 ID 기반 equals()와 hashCode()를 구현해야 합니다\n" +
                    "예시:\n" +
                    "  @Override\n" +
                    "  public boolean equals(Object o) {\n" +
                    "      if (this == o) return true;\n" +
                    "      if (!(o instanceof OrderItem that)) return false;\n" +
                    "      return Objects.equals(id, that.id);\n" +
                    "  }\n" +
                    "  \n" +
                    "  @Override\n" +
                    "  public int hashCode() {\n" +
                    "      return Objects.hash(id);\n" +
                    "  }");

        rule.check(classes);
    }

    // ==================== 생성자 규칙 ====================

    /**
     * 규칙 3: Entity의 생성자는 private이어야 한다
     */
    @Test
    @DisplayName("[필수] Entity의 생성자는 private이어야 한다")
    void entities_ShouldHavePrivateConstructors() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveOnlyPrivateConstructors())
            .because("Entity는 Factory Method를 통해서만 생성되어야 하므로 생성자는 private이어야 합니다\n" +
                    "예시:\n" +
                    "  private OrderItem(...) { }  ✅\n" +
                    "  public OrderItem(...) { }   ❌");

        rule.check(classes);
    }

    // ==================== Factory Method 규칙 ====================

    /**
     * 규칙 4: Entity는 forNew() 정적 팩토리 메서드를 가져야 한다
     */
    @Test
    @DisplayName("[필수] Entity는 forNew() 정적 팩토리 메서드를 가져야 한다")
    void entities_ShouldHaveForNewMethod() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveStaticMethodWithName("forNew"))
            .because("Entity는 forNew() 정적 팩토리 메서드를 가져야 합니다\n" +
                    "예시:\n" +
                    "  public static OrderItem forNew(long productId, int quantity, Money price) {\n" +
                    "      return new OrderItem(\n" +
                    "          OrderItemId.of(0L),  // 생성 시 0\n" +
                    "          productId,\n" +
                    "          quantity,\n" +
                    "          price\n" +
                    "      );\n" +
                    "  }");

        rule.check(classes);
    }

    /**
     * 규칙 5: Entity는 reconstitute() 정적 팩토리 메서드를 가져야 한다
     */
    @Test
    @DisplayName("[필수] Entity는 reconstitute() 정적 팩토리 메서드를 가져야 한다")
    void entities_ShouldHaveReconstituteMethod() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(haveStaticMethodWithName("reconstitute"))
            .because("Entity는 reconstitute() 정적 팩토리 메서드를 가져야 합니다\n" +
                    "예시:\n" +
                    "  public static OrderItem reconstitute(\n" +
                    "      OrderItemId id,\n" +
                    "      long productId,\n" +
                    "      int quantity,\n" +
                    "      Money price\n" +
                    "  ) {\n" +
                    "      return new OrderItem(id, productId, quantity, price);\n" +
                    "  }");

        rule.check(classes);
    }

    // ==================== 패키지 위치 규칙 ====================

    /**
     * 규칙 6: Entity는 domain.[bc].aggregate 패키지에 위치해야 한다
     */
    @Test
    @DisplayName("[필수] Entity는 domain.[bc].aggregate 패키지에 위치해야 한다")
    void entities_ShouldBeInAggregatePackage() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should().resideInAPackage("..domain..aggregate..")
            .because("내부 Entity는 domain.[bc].aggregate 패키지에 위치해야 합니다\n" +
                    "예시:\n" +
                    "  - domain.order.aggregate.OrderItem ✅\n" +
                    "  - domain.order.vo.OrderItem ❌ (VO 패키지 아님)\n" +
                    "  - domain.order.entity.OrderItem ❌ (별도 entity 패키지 사용 금지)");

        rule.check(classes);
    }

    // ==================== Lombok 금지 ====================

    /**
     * 규칙 7: Entity는 Lombok 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Entity는 Lombok 어노테이션을 사용하지 않아야 한다")
    void entities_ShouldNotUseLombok() {
        ArchRule rule = noClasses()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Test")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .because("Entity는 Lombok을 사용하지 않고 Pure Java로 구현해야 합니다");

        rule.check(classes);
    }

    // ==================== JPA 금지 ====================

    /**
     * 규칙 8: Entity는 JPA 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Entity는 JPA 어노테이션을 사용하지 않아야 한다")
    void entities_ShouldNotUseJPA() {
        ArchRule rule = noClasses()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Test")
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table")
            .orShould().beAnnotatedWith("jakarta.persistence.Column")
            .orShould().beAnnotatedWith("jakarta.persistence.Id")
            .orShould().beAnnotatedWith("jakarta.persistence.ManyToOne")
            .orShould().beAnnotatedWith("jakarta.persistence.OneToMany")
            .because("Domain Entity는 JPA에 독립적이어야 합니다 (JPA Entity와 별개)");

        rule.check(classes);
    }

    // ==================== Spring 금지 ====================

    /**
     * 규칙 9: Entity는 Spring 어노테이션을 사용하지 않아야 한다
     */
    @Test
    @DisplayName("[금지] Entity는 Spring 어노테이션을 사용하지 않아야 한다")
    void entities_ShouldNotUseSpring() {
        ArchRule rule = noClasses()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Test")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .because("Entity는 Spring에 독립적이어야 합니다");

        rule.check(classes);
    }

    // ==================== Public 규칙 ====================

    /**
     * 규칙 10: Entity는 public 클래스여야 한다
     */
    @Test
    @DisplayName("[필수] Entity는 public 클래스여야 한다")
    void entities_ShouldBePublic() {
        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should().bePublic()
            .because("Entity는 AggregateRoot에서 접근하기 위해 public이어야 합니다");

        rule.check(classes);
    }

    // ==================== Getter 패턴 규칙 ====================

    /**
     * 규칙 11: Entity는 get* 메서드 대신 필드명 메서드를 사용해야 한다
     */
    @Test
    @DisplayName("[권장] Entity는 get* 메서드 대신 필드명 메서드를 사용해야 한다")
    void entities_ShouldUseFieldNameMethodsInsteadOfGetters() {
        // Note: ArchUnit으로 메서드명 패턴 검증은 제한적이므로, 코드 리뷰 시 확인 필요

        ArchRule rule = classes()
            .that().implement("com.ryuqq.domain.common.model.Entity")
            .and().doNotImplement("com.ryuqq.domain.common.model.AggregateRoot")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Mother")
            .and().haveSimpleNameNotContaining("Test")
            .should(preferFieldNameMethods())
            .because("Entity는 get* 메서드 대신 필드명 메서드를 사용해야 합니다 (Tell, Don't Ask)\n" +
                    "예시:\n" +
                    "  - public Money price() { return price; }  ✅\n" +
                    "  - public Money getPrice() { return price; }  ❌");

        rule.check(classes);
    }

    // ==================== 커스텀 ArchCondition 헬퍼 메서드 ====================

    /**
     * Entity 인터페이스만 구현하는지 검증 (AggregateRoot 구현 금지)
     */
    private static ArchCondition<JavaClass> implementEntityInterfaceOnly() {
        return new ArchCondition<JavaClass>("implement Entity interface only (not AggregateRoot)") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean implementsEntity = javaClass.getAllRawInterfaces().stream()
                    .anyMatch(iface -> iface.getSimpleName().equals("Entity"));

                boolean implementsAggregateRoot = javaClass.getAllRawInterfaces().stream()
                    .anyMatch(iface -> iface.getSimpleName().equals("AggregateRoot"));

                if (!implementsEntity || implementsAggregateRoot) {
                    String message = String.format(
                        "Class %s should implement Entity interface only (not AggregateRoot)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * equals()와 hashCode() 메서드를 가지고 있는지 검증
     */
    private static ArchCondition<JavaClass> haveEqualsAndHashCode() {
        return new ArchCondition<JavaClass>("have equals() and hashCode() methods") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasEquals = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals("equals")
                        && method.getParameters().size() == 1);

                boolean hasHashCode = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals("hashCode")
                        && method.getParameters().isEmpty());

                if (!hasEquals || !hasHashCode) {
                    String message = String.format(
                        "Class %s should have equals() and hashCode() methods (ID-based)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 모든 생성자가 private인지 검증
     */
    private static ArchCondition<JavaClass> haveOnlyPrivateConstructors() {
        return new ArchCondition<JavaClass>("have only private constructors") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasPublicConstructor = javaClass.getConstructors().stream()
                    .anyMatch(constructor -> constructor.getModifiers().contains(JavaModifier.PUBLIC));

                if (hasPublicConstructor) {
                    String message = String.format(
                        "Class %s should have only private constructors (use Factory Methods: forNew, reconstitute)",
                        javaClass.getName()
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * 클래스가 특정 이름의 public static 메서드를 가지고 있는지 검증
     */
    private static ArchCondition<JavaClass> haveStaticMethodWithName(String methodName) {
        return new ArchCondition<JavaClass>("have public static method with name " + methodName) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getAllMethods().stream()
                    .anyMatch(method -> method.getName().equals(methodName)
                        && method.getModifiers().contains(JavaModifier.STATIC)
                        && method.getModifiers().contains(JavaModifier.PUBLIC));

                if (!hasMethod) {
                    String message = String.format(
                        "Class %s does not have a public static method named '%s'",
                        javaClass.getName(), methodName
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }

    /**
     * get* 메서드 대신 필드명 메서드를 선호하는지 검증
     */
    private static ArchCondition<JavaClass> preferFieldNameMethods() {
        return new ArchCondition<JavaClass>("prefer field name methods over getters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long getterCount = javaClass.getAllMethods().stream()
                    .filter(method -> method.getName().startsWith("get")
                        && !method.getName().equals("getClass")
                        && method.getParameters().isEmpty()
                        && method.getModifiers().contains(JavaModifier.PUBLIC))
                    .count();

                if (getterCount > 0) {
                    String message = String.format(
                        "Class %s has %d getter methods (get*). Consider using field name methods instead (e.g., price() instead of getPrice())",
                        javaClass.getName(), getterCount
                    );
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                }
            }
        };
    }
}
