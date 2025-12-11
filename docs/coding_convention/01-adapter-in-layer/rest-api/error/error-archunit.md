# Error Handling ArchUnit 검증 규칙

> **목적**: Error Handling 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: RFC 7807 준수, OCP 원칙, 표준화된 에러 처리

---

## 1️⃣ 검증 항목 (완전 강제)

### GlobalExceptionHandler 검증 규칙

1. ✅ **`@RestControllerAdvice` 필수** - GlobalExceptionHandler는 `@RestControllerAdvice` 어노테이션 사용
2. ✅ **네이밍 규칙** - `*ExceptionHandler` 접미사 필수
3. ✅ **패키지 위치** - `adapter-in.rest-api.common.controller` 패키지에 위치
4. ✅ **ErrorMapperRegistry 의존성** - Constructor Injection으로 의존
5. ❌ **`@Transactional` 금지** - ExceptionHandler에서 트랜잭션 관리 금지
6. ❌ **Lombok 금지** - 모든 Lombok 어노테이션 금지

### ErrorMapper 검증 규칙

7. ✅ **`@Component` 필수** - ErrorMapper 구현체는 `@Component` 어노테이션 사용
8. ✅ **인터페이스 구현** - `ErrorMapper` 인터페이스 구현 필수
9. ✅ **네이밍 규칙** - `*ApiErrorMapper` 접미사 권장
10. ✅ **패키지 위치** - `adapter-in.rest-api.[bc].error` 패키지에 위치
11. ❌ **Domain 의존성 제한** - DomainException만 의존 가능, 다른 Domain 객체 금지

### ErrorMapperRegistry 검증 규칙

12. ✅ **`@Component` 필수** - Registry는 `@Component` 어노테이션 사용
13. ✅ **ErrorMapper 목록 의존성** - `List<ErrorMapper>` Constructor Injection

---

## 2️⃣ 의존성 추가

### Gradle

```gradle
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```

### Maven

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

### 테스트 클래스 기본 구조

```java
package com.ryuqq.adapter.in.rest.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Error Handling ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>GlobalExceptionHandler, ErrorMapper, ErrorMapperRegistry 규칙을 검증합니다.</p>
 *
 * <p>검증 규칙:</p>
 * <ul>
 *   <li>1. GlobalExceptionHandler: @RestControllerAdvice 필수</li>
 *   <li>2. ErrorMapper: @Component, 인터페이스 구현</li>
 *   <li>3. ErrorMapperRegistry: List&lt;ErrorMapper&gt; 의존성</li>
 *   <li>4. Lombok 금지</li>
 *   <li>5. 올바른 패키지 위치</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Error Handling ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ErrorHandlingArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.in.rest");
    }

    // 테스트 메서드들...
}
```

---

## 4️⃣ GlobalExceptionHandler 검증 규칙

```java
@Nested
@DisplayName("GlobalExceptionHandler 검증 규칙")
class GlobalExceptionHandlerRules {

    /**
     * 규칙 1: @RestControllerAdvice 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] ExceptionHandler는 @RestControllerAdvice 어노테이션을 가져야 한다")
    void exceptionHandler_MustHaveRestControllerAdviceAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .and().resideInAPackage("..controller..")
            .should().beAnnotatedWith(
                org.springframework.web.bind.annotation.RestControllerAdvice.class)
            .because("GlobalExceptionHandler는 @RestControllerAdvice 어노테이션이 필수입니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ExceptionHandler)
     */
    @Test
    @DisplayName("[필수] ExceptionHandler는 *ExceptionHandler 접미사를 가져야 한다")
    void exceptionHandler_MustHaveExceptionHandlerSuffix() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestControllerAdvice.class)
            .and().resideInAPackage("..common.controller..")
            .should().haveSimpleNameEndingWith("ExceptionHandler")
            .because("GlobalExceptionHandler는 *ExceptionHandler 네이밍 규칙을 따라야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] GlobalExceptionHandler는 common.controller 패키지에 위치해야 한다")
    void exceptionHandler_MustBeInCommonControllerPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .and().areAnnotatedWith(
                org.springframework.web.bind.annotation.RestControllerAdvice.class)
            .should().resideInAPackage("..adapter.in.rest.common.controller..")
            .because("GlobalExceptionHandler는 common.controller 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: @Transactional 사용 금지
     */
    @Test
    @DisplayName("[금지] ExceptionHandler는 @Transactional을 사용하지 않아야 한다")
    void exceptionHandler_MustNotUseTransactional() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .should().beAnnotatedWith(
                org.springframework.transaction.annotation.Transactional.class)
            .because("ExceptionHandler는 트랜잭션 관리를 하지 않습니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Lombok 어노테이션 금지
     */
    @Test
    @DisplayName("[금지] ExceptionHandler는 Lombok 어노테이션을 가지지 않아야 한다")
    void exceptionHandler_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .because("ExceptionHandler는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: ErrorMapperRegistry 의존성 필수
     */
    @Test
    @DisplayName("[필수] GlobalExceptionHandler는 ErrorMapperRegistry에 의존해야 한다")
    void exceptionHandler_MustDependOnErrorMapperRegistry() {
        ArchRule rule = classes()
            .that().haveSimpleName("GlobalExceptionHandler")
            .should().dependOnClassesThat().haveSimpleName("ErrorMapperRegistry")
            .because("GlobalExceptionHandler는 ErrorMapperRegistry를 통해 DomainException을 매핑합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: @ExceptionHandler 메서드 필수
     */
    @Test
    @DisplayName("[필수] ExceptionHandler는 @ExceptionHandler 메서드를 가져야 한다")
    void exceptionHandler_MustHaveExceptionHandlerMethods() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .and().resideInAPackage("..common.controller..")
            .should().containAnyMethodsThat(method ->
                method.isAnnotatedWith(
                    org.springframework.web.bind.annotation.ExceptionHandler.class))
            .because("ExceptionHandler는 @ExceptionHandler 메서드를 가져야 합니다");

        rule.check(classes);
    }
}
```

---

## 5️⃣ ErrorMapper 검증 규칙

```java
@Nested
@DisplayName("ErrorMapper 검증 규칙")
class ErrorMapperRules {

    /**
     * 규칙 8: @Component 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] ErrorMapper 구현체는 @Component 어노테이션을 가져야 한다")
    void errorMapper_MustHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().implement(ErrorMapper.class)
            .and().areNotInterfaces()
            .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
            .because("ErrorMapper 구현체는 @Component로 Bean 등록되어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: 네이밍 규칙 (*ApiErrorMapper)
     */
    @Test
    @DisplayName("[권장] ErrorMapper 구현체는 *ApiErrorMapper 접미사를 가져야 한다")
    void errorMapper_ShouldHaveApiErrorMapperSuffix() {
        ArchRule rule = classes()
            .that().implement(ErrorMapper.class)
            .and().areNotInterfaces()
            .and().resideInAPackage("..error..")
            .should().haveSimpleNameEndingWith("ApiErrorMapper")
            .because("ErrorMapper 구현체는 *ApiErrorMapper 네이밍 규칙을 따르는 것을 권장합니다");

        // 권장 사항이므로 경고만 표시
        try {
            rule.check(classes);
        } catch (AssertionError e) {
            System.out.println("⚠️  Warning: " + e.getMessage());
        }
    }

    /**
     * 규칙 10: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] ErrorMapper 구현체는 [bc].error 패키지에 위치해야 한다")
    void errorMapper_MustBeInBcErrorPackage() {
        ArchRule rule = classes()
            .that().implement(ErrorMapper.class)
            .and().areNotInterfaces()
            .should().resideInAPackage("..adapter.in.rest..error..")
            .because("ErrorMapper 구현체는 adapter.in.rest.[bc].error 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 11: ErrorMapper 인터페이스 위치 검증
     */
    @Test
    @DisplayName("[필수] ErrorMapper 인터페이스는 common.mapper 패키지에 위치해야 한다")
    void errorMapperInterface_MustBeInCommonMapperPackage() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapper")
            .and().areInterfaces()
            .should().resideInAPackage("..adapter.in.rest.common.mapper..")
            .because("ErrorMapper 인터페이스는 common.mapper 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: Lombok 어노테이션 금지
     */
    @Test
    @DisplayName("[금지] ErrorMapper 구현체는 Lombok 어노테이션을 가지지 않아야 한다")
    void errorMapper_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().implement(ErrorMapper.class)
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .because("ErrorMapper는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }
}
```

---

## 6️⃣ ErrorMapperRegistry 검증 규칙

```java
@Nested
@DisplayName("ErrorMapperRegistry 검증 규칙")
class ErrorMapperRegistryRules {

    /**
     * 규칙 13: @Component 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] ErrorMapperRegistry는 @Component 어노테이션을 가져야 한다")
    void errorMapperRegistry_MustHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapperRegistry")
            .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
            .because("ErrorMapperRegistry는 @Component로 Bean 등록되어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] ErrorMapperRegistry는 common.error 패키지에 위치해야 한다")
    void errorMapperRegistry_MustBeInCommonErrorPackage() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapperRegistry")
            .should().resideInAPackage("..adapter.in.rest.common.error..")
            .because("ErrorMapperRegistry는 common.error 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 15: ErrorMapper 목록 의존성
     */
    @Test
    @DisplayName("[필수] ErrorMapperRegistry는 ErrorMapper 목록에 의존해야 한다")
    void errorMapperRegistry_MustDependOnErrorMapperList() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapperRegistry")
            .should().dependOnClassesThat().haveSimpleName("ErrorMapper")
            .because("ErrorMapperRegistry는 List<ErrorMapper>를 Constructor Injection으로 받아야 합니다");

        rule.check(classes);
    }
}
```

---

## 7️⃣ 추가 검증 규칙

### ErrorMapping Record 검증

```java
@Nested
@DisplayName("ErrorMapping 검증 규칙")
class ErrorMappingRules {

    /**
     * 규칙 16: ErrorMapping은 Record여야 한다
     */
    @Test
    @DisplayName("[필수] ErrorMapping은 Record 타입이어야 한다")
    void errorMapping_MustBeRecord() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapping")
            .should().beRecords()
            .because("ErrorMapping은 불변 데이터 구조인 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 17: ErrorMapping 위치 검증
     */
    @Test
    @DisplayName("[필수] ErrorMapping은 common.error 패키지에 위치해야 한다")
    void errorMapping_MustBeInCommonErrorPackage() {
        ArchRule rule = classes()
            .that().haveSimpleName("ErrorMapping")
            .should().resideInAPackage("..adapter.in.rest.common.error..")
            .because("ErrorMapping은 common.error 패키지에 위치해야 합니다");

        rule.check(classes);
    }
}
```

### 의존성 방향 검증

```java
@Nested
@DisplayName("의존성 방향 검증 규칙")
class DependencyRules {

    /**
     * 규칙 18: ErrorMapper는 Domain 예외만 의존 가능
     */
    @Test
    @DisplayName("[제한] ErrorMapper는 DomainException만 의존할 수 있다")
    void errorMapper_OnlyDependOnDomainException() {
        ArchRule rule = classes()
            .that().implement(ErrorMapper.class)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..adapter.in.rest..",
                "..domain.common.exception..",  // DomainException만 허용
                "java..",
                "org.springframework..",
                "jakarta.."
            )
            .because("ErrorMapper는 도메인 예외(DomainException)만 의존할 수 있습니다");

        rule.check(classes);
    }

    /**
     * 규칙 19: error 패키지는 controller 패키지에 의존하지 않음
     */
    @Test
    @DisplayName("[금지] error 패키지는 controller 패키지에 의존하지 않아야 한다")
    void errorPackage_MustNotDependOnController() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..error..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("error 패키지는 controller 패키지에 의존하지 않습니다 (단방향 의존성)");

        rule.check(classes);
    }
}
```

---

## 8️⃣ 실행 방법

### Gradle

```bash
# Error Handling ArchUnit 테스트만 실행
./gradlew test --tests "*ErrorHandlingArchTest"

# 전체 ArchUnit 테스트 실행
./gradlew test --tests "*ArchTest"
```

### Maven

```bash
mvn test -Dtest=ErrorHandlingArchTest
```

### IDE

- IntelliJ IDEA: `ErrorHandlingArchTest` 클래스에서 우클릭 → Run
- 또는 `@Tag("architecture")` 테스트만 실행

---

## 9️⃣ 위반 예시 및 수정

### ❌ Bad: @RestControllerAdvice 누락

```java
@ControllerAdvice  // ❌ @RestControllerAdvice 사용해야 함
public class GlobalExceptionHandler {
    // ...
}
```

### ✅ Good: @RestControllerAdvice 사용

```java
@RestControllerAdvice  // ✅ @RestControllerAdvice 사용
public class GlobalExceptionHandler {
    // ...
}
```

---

### ❌ Bad: ErrorMapper에 @Component 누락

```java
// ❌ @Component 어노테이션 누락
public class OrderApiErrorMapper implements ErrorMapper {
    @Override
    public boolean supports(DomainException ex) {
        return ex.code().startsWith("ORDER_");
    }
    // ...
}
```

### ✅ Good: ErrorMapper에 @Component 사용

```java
@Component  // ✅ @Component로 Bean 등록
public class OrderApiErrorMapper implements ErrorMapper {
    @Override
    public boolean supports(DomainException ex) {
        return ex.code().startsWith("ORDER_");
    }
    // ...
}
```

---

### ❌ Bad: ErrorMapper가 잘못된 패키지에 위치

```java
package com.ryuqq.adapter.in.rest.order.controller;  // ❌ 잘못된 패키지

@Component
public class OrderApiErrorMapper implements ErrorMapper {
    // ...
}
```

### ✅ Good: ErrorMapper가 올바른 패키지에 위치

```java
package com.ryuqq.adapter.in.rest.order.error;  // ✅ [bc].error 패키지

@Component
public class OrderApiErrorMapper implements ErrorMapper {
    // ...
}
```

---

### ❌ Bad: ErrorMapper가 Domain 객체에 의존

```java
@Component
public class OrderApiErrorMapper implements ErrorMapper {

    // ❌ Domain 객체에 직접 의존 (DomainException 외)
    private final OrderRepository orderRepository;

    @Override
    public ErrorMapping map(DomainException ex, Locale locale) {
        // ❌ Repository 호출
        Order order = orderRepository.findById(ex.args().get("orderId"));
        // ...
    }
}
```

### ✅ Good: ErrorMapper는 DomainException만 사용

```java
@Component
public class OrderApiErrorMapper implements ErrorMapper {

    @Override
    public boolean supports(DomainException ex) {
        return ex.code().startsWith("ORDER_");
    }

    @Override
    public ErrorMapping map(DomainException ex, Locale locale) {
        // ✅ DomainException의 정보만 사용
        return switch (ex.code()) {
            case "ORDER_NOT_FOUND" -> ErrorMapping.of(
                HttpStatus.NOT_FOUND,
                "Not Found",
                String.format("주문 ID %s를 찾을 수 없습니다", ex.args().get("orderId"))
            );
            default -> ErrorMapping.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage()
            );
        };
    }
}
```

---

## 🔟 CI/CD 통합

### GitHub Actions

```yaml
- name: Run Error Handling Architecture Tests
  run: ./gradlew test --tests "*ErrorHandlingArchTest"
```

### 빌드 실패 정책

- ArchUnit 테스트 실패 시 빌드 실패
- PR Merge 전 필수 통과
- Zero-Tolerance 정책 적용

---

## 1️⃣1️⃣ 체크리스트

### GlobalExceptionHandler

- [ ] `@RestControllerAdvice` 어노테이션 사용
- [ ] `*ExceptionHandler` 네이밍 규칙 준수
- [ ] `common.controller` 패키지에 위치
- [ ] `ErrorMapperRegistry` 의존성 확인
- [ ] `@ExceptionHandler` 메서드 존재

### ErrorMapper

- [ ] `@Component` 어노테이션 사용
- [ ] `ErrorMapper` 인터페이스 구현
- [ ] `*ApiErrorMapper` 네이밍 규칙 (권장)
- [ ] `[bc].error` 패키지에 위치
- [ ] `DomainException`만 의존

### ErrorMapperRegistry

- [ ] `@Component` 어노테이션 사용
- [ ] `common.error` 패키지에 위치
- [ ] `List<ErrorMapper>` Constructor Injection

### 빌드 통합

- [ ] `ErrorHandlingArchTest` 클래스 작성
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
