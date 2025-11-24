# Mapper ArchUnit 검증 규칙

> **목적**: Mapper 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Mapper 검증 규칙
1. ✅ **@Component 어노테이션 필수** - Spring Bean 등록
2. ✅ **네이밍 규칙** - `*ApiMapper` 접미사 필수
3. ❌ **Lombok 어노테이션 절대 금지** (@Data, @Builder, @Getter, @Setter 등)
4. ❌ **Static 메서드 절대 금지** - 인스턴스 메서드만 허용
5. ❌ **Domain 객체 직접 사용 금지** - Application DTO만 사용
6. ❌ **비즈니스 로직 금지** - 필드 매핑만 수행
7. ❌ **Port 의존성 주입 금지** - Repository, UseCase 주입 금지
8. ✅ **패키지 위치**: adapter-in.rest-api.[bc].mapper
9. ✅ **의존성 주입 허용** - MessageSource, ObjectMapper 등 유틸리티만
10. ❌ **Domain Entity 의존 금지** - Domain Layer 직접 의존 금지

---

## 2️⃣ 의존성 추가

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## 3️⃣ ArchUnit 테스트 (완전 강제 버전)

```java
package com.company.adapter.in.rest.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;

/**
 * Mapper ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Mapper는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Mapper ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class MapperArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.adapter.in.rest");
    }

    /**
     * 규칙 1: @Component 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] Mapper는 @Component 어노테이션을 가져야 한다")
    void mapper_MustHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().resideInAPackage("..mapper..")
            .and().haveSimpleNameEndingWith("ApiMapper")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .because("Mapper는 @Component로 Bean 등록되어야 하며 Static 메서드는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiMapper)
     */
    @Test
    @DisplayName("[필수] Mapper는 *ApiMapper 접미사를 가져야 한다")
    void mapper_MustHaveApiMapperSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..mapper..")
            .and().areAnnotatedWith("org.springframework.stereotype.Component")
            .should().haveSimpleNameEndingWith("ApiMapper")
            .because("Mapper는 *ApiMapper 네이밍 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 Lombok 어노테이션을 가지지 않아야 한다")
    void mapper_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Mapper는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Static 메서드 절대 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 Static 메서드를 가지지 않아야 한다")
    void mapper_MustNotHaveStaticMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .and().haveSimpleNameEndingWith("ApiMapper")
            .should().haveMethodsThat().areStatic()
            .andShould().haveMethodsThat().arePublic()
            .because("Mapper는 @Component Bean이므로 Static 메서드는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 객체 직접 사용 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 Domain 객체를 직접 사용하지 않아야 한다")
    void mapper_MustNotUseDomainObjects() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
            .because("Mapper는 Application DTO만 사용하며 Domain 직접 의존은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void mapper_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().haveMethodNames("calculate", "compute", "validate", "isValid", "check", "process")
            .because("Mapper는 필드 매핑만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Port 의존성 주입 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 Port 의존성을 주입받지 않아야 한다")
    void mapper_MustNotDependOnPorts() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().dependOnClassesThat().resideInAPackage("..port.in..")
            .orShould().dependOnClassesThat().resideInAPackage("..port.out..")
            .because("Mapper는 UseCase/Repository를 주입받지 않으며 Controller가 주입합니다");

        rule.check(classes);
    }

    /**
     * 규칙 8: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] Mapper는 올바른 패키지에 위치해야 한다")
    void mapper_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiMapper")
            .and().resideInAPackage("..adapter.in.rest..")
            .should().resideInAPackage("..mapper..")
            .because("Mapper는 mapper 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: @Service, @Repository 어노테이션 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 @Service/@Repository 어노테이션을 가지지 않아야 한다")
    void mapper_MustNotUseServiceOrRepository() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
            .because("Mapper는 @Component 어노테이션만 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Domain Entity 의존 금지
     */
    @Test
    @DisplayName("[금지] Mapper는 Domain Entity를 의존하지 않아야 한다")
    void mapper_MustNotDependOnDomainEntities() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().accessClassesThat().resideInAPackage("..domain..")
            .because("Mapper는 Application Layer DTO만 사용하며 Domain Entity는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 11: 메서드 네이밍 규칙 (권장)
     */
    @Test
    @DisplayName("[권장] Mapper 메서드는 to* 접두사를 가져야 한다")
    void mapper_ShouldHaveToMethodPrefix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..mapper..")
            .and().haveSimpleNameEndingWith("ApiMapper")
            .should().haveMethodsThat().haveNameMatching("to[A-Z].*")
            .because("Mapper 변환 메서드는 to* 접두사를 사용하는 것이 좋습니다 (예: toCommand, toApiResponse)");

        // Note: 이 규칙은 권장사항이므로 실패 시 경고만 표시
        try {
            rule.check(classes);
        } catch (AssertionError e) {
            System.out.println("⚠️  Warning: " + e.getMessage());
        }
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle
```bash
./gradlew test --tests "*MapperArchTest"
```

### Maven
```bash
mvn test -Dtest=MapperArchTest
```

### IDE
- IntelliJ IDEA: `MapperArchTest` 클래스에서 우클릭 → Run
- 또는 전체 Architecture Test 실행

---

## 5️⃣ 위반 예시 및 수정

### ❌ Bad: Static 메서드 사용

```java
// ❌ Static 메서드 금지
public class OrderApiMapper {

    public static CreateOrderCommand toCommand(CreateOrderApiRequest request) {
        return CreateOrderCommand.of(request.customerId());
    }
}
```

### ✅ Good: @Component Bean 등록

```java
// ✅ @Component + 인스턴스 메서드
@Component
public class OrderApiMapper {

    public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
        return CreateOrderCommand.of(request.customerId());
    }
}
```

---

### ❌ Bad: Domain 객체 직접 사용

```java
// ❌ Domain Entity 직접 사용 금지
@Component
public class OrderApiMapper {

    public Order toDomain(CreateOrderApiRequest request) {  // ← Domain Entity
        return Order.forNew(...);
    }
}
```

### ✅ Good: Application DTO만 사용

```java
// ✅ Application Command/Query만 사용
@Component
public class OrderApiMapper {

    public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
        return CreateOrderCommand.of(request.customerId());
    }

    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        return OrderApiResponse.of(appResponse.id());
    }
}
```

---

### ❌ Bad: Port 의존성 주입

```java
// ❌ UseCase/Repository 주입 금지
@Component
public class OrderApiMapper {

    private final GetOrderUseCase getOrderUseCase;  // ← 금지

    public OrderApiMapper(GetOrderUseCase getOrderUseCase) {
        this.getOrderUseCase = getOrderUseCase;
    }
}
```

### ✅ Good: 유틸리티 의존성만 주입

```java
// ✅ MessageSource, ObjectMapper 등 유틸리티만 주입 가능
@Component
public class OrderApiMapper {

    private final MessageSource messageSource;

    public OrderApiMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        String statusLabel = messageSource.getMessage(
            "order.status." + appResponse.status(),
            null,
            LocaleContextHolder.getLocale()
        );
        return OrderApiResponse.of(appResponse.id(), statusLabel);
    }
}
```

---

### ❌ Bad: 비즈니스 로직 포함

```java
// ❌ 계산 로직 금지
@Component
public class OrderApiMapper {

    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        BigDecimal tax = appResponse.totalAmount().multiply(0.1);  // ← 금지
        return OrderApiResponse.of(
            appResponse.id(),
            appResponse.totalAmount().add(tax)
        );
    }
}
```

### ✅ Good: 필드 매핑만

```java
// ✅ 필드 매핑만 수행
@Component
public class OrderApiMapper {

    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        return OrderApiResponse.of(
            appResponse.id(),
            appResponse.totalAmount()  // 그대로 전달
        );
    }
}
```

---

## 6️⃣ CI/CD 통합

### GitHub Actions
```yaml
- name: Run Architecture Tests
  run: ./gradlew test --tests "*ArchTest"
```

### 빌드 실패 정책
- ArchUnit 테스트 실패 시 빌드 실패
- PR Merge 전 필수 통과
- Zero-Tolerance 정책 적용

---

## 7️⃣ 체크리스트

- [ ] `MapperArchTest` 클래스 작성
- [ ] 11개 검증 규칙 모두 구현
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 숙지

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
