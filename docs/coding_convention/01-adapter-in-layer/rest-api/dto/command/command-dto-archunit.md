# Command DTO ArchUnit 검증 규칙

> **목적**: Command DTO 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Command DTO 검증 규칙
1. ✅ **Record 타입 필수** - `public record` 키워드 사용
2. ✅ **네이밍 규칙** - `*ApiRequest` 접미사 필수
3. ❌ **Lombok 어노테이션 절대 금지** (@Data, @Builder, @Getter, @Setter 등)
4. ❌ **Jackson 어노테이션 절대 금지** (@JsonFormat, @JsonProperty, @JsonIgnore 등)
5. ❌ **Domain 변환 메서드 금지** (toDomain(), toEntity() 등)
6. ❌ **비즈니스 로직 금지** (계산 메서드, 검증 메서드 등)
7. ✅ **Bean Validation 사용** (@NotNull, @NotEmpty, @Valid 등)
8. ✅ **패키지 위치**: adapter-in.rest-api.[bc].dto.command
9. ✅ **Nested Record 허용** - 복잡한 구조는 Nested Record로 표현
10. ✅ **Compact Constructor 허용** - 방어적 복사, 간단한 검증만

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
 * Command DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Command DTO는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Command DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class CommandDtoArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.company.adapter.in.rest");
    }

    /**
     * 규칙 1: Record 타입 필수
     */
    @Test
    @DisplayName("[필수] Command DTO는 Record 타입이어야 한다")
    void commandDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Command DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiRequest)
     */
    @Test
    @DisplayName("[필수] Command DTO는 *ApiRequest 접미사를 가져야 한다")
    void commandDto_MustHaveApiRequestSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiRequest")
            .because("Command DTO는 *ApiRequest 네이밍 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Command DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Command DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().haveMethodNames("toDomain", "toEntity", "toAggregate")
            .because("Command DTO → Domain 변환은 Mapper/Assembler의 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().haveMethodNames("calculate", "compute", "validate", "isValid", "check")
            .because("Command DTO는 데이터 전송만 담당하며 비즈니스 로직은 Domain Layer 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Bean Validation 어노테이션 사용
     */
    @Test
    @DisplayName("[권장] Command DTO는 Bean Validation 어노테이션을 사용해야 한다")
    void commandDto_ShouldUseValidationAnnotations() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Command DTO는 Bean Validation을 통해 입력값을 검증해야 합니다");

        // Note: 이 규칙은 권장사항이므로 실패 시 경고만 표시
        try {
            rule.check(classes);
        } catch (AssertionError e) {
            System.out.println("⚠️  Warning: " + e.getMessage());
        }
    }

    /**
     * 규칙 8: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] Command DTO는 올바른 패키지에 위치해야 한다")
    void commandDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .should().resideInAPackage("..dto.command..")
            .because("Command DTO는 dto.command 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Command DTO는 Setter 메서드를 가지지 않아야 한다")
    void commandDto_MustNotHaveSetterMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().haveMethodsThat().haveNameMatching("set[A-Z].*")
            .because("Command DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Command DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void commandDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.command..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Command DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle
```bash
./gradlew test --tests "*CommandDtoArchTest"
```

### Maven
```bash
mvn test -Dtest=CommandDtoArchTest
```

### IDE
- IntelliJ IDEA: `CommandDtoArchTest` 클래스에서 우클릭 → Run
- 또는 전체 Architecture Test 실행

---

## 5️⃣ 위반 예시 및 수정

### ❌ Bad: Lombok 사용
```java
@Data  // ❌ 금지
@Builder  // ❌ 금지
public class CreateOrderApiRequest {
    private Long customerId;
    private List<OrderItemRequest> items;
}
```

### ✅ Good: Record 사용
```java
public record CreateOrderApiRequest(
    @NotNull Long customerId,
    @NotEmpty @Valid List<OrderItemRequest> items
) {
    public CreateOrderApiRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
```

---

### ❌ Bad: Jackson 어노테이션
```java
public record CreateOrderApiRequest(
    @JsonProperty("customer_id")  // ❌ 금지
    Long customerId
) {}
```

### ✅ Good: Pure Record
```java
public record CreateOrderApiRequest(
    Long customerId  // ✅ JSON 자동 매핑
) {}
```

---

### ❌ Bad: Domain 변환 메서드
```java
public record CreateOrderApiRequest(...) {
    public Order toDomain() {  // ❌ 금지
        return Order.forNew(...);
    }
}
```

### ✅ Good: Mapper 분리
```java
// Command DTO (변환 로직 없음)
public record CreateOrderApiRequest(...) {}

// Mapper (변환 책임)
@Component
public class OrderApiMapper {
    public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
        return new CreateOrderCommand(...);
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

- [ ] `CommandDtoArchTest` 클래스 작성
- [ ] 10개 검증 규칙 모두 구현
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 숙지

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
