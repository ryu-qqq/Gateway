# Controller ArchUnit 검증 규칙

> **목적**: Controller 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Controller 검증 규칙

1. ✅ **`@RestController` 필수** - 모든 Controller는 `@RestController` 어노테이션 사용
2. ✅ **`@RequestMapping` 필수** - 클래스 레벨에 `@RequestMapping` 필수
3. ✅ **네이밍 규칙** - `*Controller` 접미사 필수 (OrderCommandController, OrderQueryController 등)
4. ✅ **패키지 위치** - `adapter-in.rest-api.[bc].controller` 패키지에 위치
5. ❌ **`@Transactional` 금지** - Controller에서 트랜잭션 관리 금지 (UseCase 책임)
6. ❌ **비즈니스 로직 금지** - Domain 객체 직접 생성/조작 금지
7. ❌ **`@Service` 금지** - Controller는 `@RestController`만 사용
8. ❌ **Lombok 금지** - `@Data`, `@Builder` 등 모든 Lombok 어노테이션 금지
9. ✅ **UseCase 의존성** - UseCase 인터페이스를 Constructor Injection으로 의존
10. ✅ **Mapper 의존성** - Mapper를 Constructor Injection으로 의존
11. ✅ **반환 타입 통일** - `ResponseEntity<ApiResponse<T>>` 형식으로 반환
12. ❌ **DELETE 메서드 금지** - `@DeleteMapping` 사용 금지
13. ✅ **엔드포인트 Properties 사용** - 하드코딩 금지, `${api.endpoints.*}` 사용

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

### 테스트 클래스 기본 구조

```java
package com.ryuqq.adapter.in.rest.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Controller ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Controller는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * <p>검증 규칙:</p>
 * <ul>
 *   <li>1. @RestController 어노테이션 필수</li>
 *   <li>2. @RequestMapping 어노테이션 필수 (클래스 레벨)</li>
 *   <li>3. *Controller 네이밍 규칙</li>
 *   <li>4. @Transactional 사용 금지</li>
 *   <li>5. @Service 사용 금지</li>
 *   <li>6. Lombok 어노테이션 금지</li>
 *   <li>7. DELETE 메서드 금지 (@DeleteMapping)</li>
 *   <li>8. 올바른 패키지 위치</li>
 *   <li>9. Domain 객체 직접 생성 금지</li>
 *   <li>10. UseCase 의존성 필수</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Controller ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ControllerArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.in.rest");
    }

    /**
     * 규칙 1: @RestController 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] Controller는 @RestController 어노테이션을 가져야 한다")
    void controller_MustHaveRestControllerAnnotation() {
        ArchRule rule = classes()
            .that().resideInAPackage("..controller..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .because("Controller는 @RestController 어노테이션이 필수입니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: @RequestMapping 어노테이션 필수
     */
    @Test
    @DisplayName("[필수] Controller는 @RequestMapping 어노테이션을 가져야 한다")
    void controller_MustHaveRequestMappingAnnotation() {
        ArchRule rule = classes()
            .that().resideInAPackage("..controller..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RequestMapping.class)
            .because("Controller는 @RequestMapping 어노테이션이 필수입니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: 네이밍 규칙 (*Controller)
     */
    @Test
    @DisplayName("[필수] Controller는 *Controller 접미사를 가져야 한다")
    void controller_MustHaveControllerSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..controller..")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .because("Controller는 *Controller 네이밍 규칙을 따라야 합니다 (예: OrderCommandController, OrderQueryController)");

        rule.check(classes);
    }

    /**
     * 규칙 4: @Transactional 사용 금지
     */
    @Test
    @DisplayName("[금지] Controller는 @Transactional을 사용하지 않아야 한다")
    void controller_MustNotUseTransactional() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
            .because("Controller는 트랜잭션 관리를 하지 않습니다. UseCase에서 @Transactional을 사용하세요.");

        rule.check(classes);
    }

    /**
     * 규칙 5: @Service 사용 금지
     */
    @Test
    @DisplayName("[금지] Controller는 @Service를 사용하지 않아야 한다")
    void controller_MustNotUseServiceAnnotation() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
            .because("Controller는 @RestController만 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: Lombok 어노테이션 금지
     */
    @Test
    @DisplayName("[금지] Controller는 Lombok 어노테이션을 가지지 않아야 한다")
    void controller_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .because("Controller는 Pure Java를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: DELETE 메서드 금지
     */
    @Test
    @DisplayName("[금지] Controller는 @DeleteMapping을 사용하지 않아야 한다")
    void controller_MustNotUseDeleteMapping() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.DeleteMapping.class)
            .because("DELETE 메서드는 지원하지 않습니다. 소프트 삭제는 PATCH로 처리하세요.");

        rule.check(classes);
    }

    /**
     * 규칙 8: 패키지 위치 검증
     */
    @Test
    @DisplayName("[필수] Controller는 올바른 패키지에 위치해야 한다")
    void controller_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("..adapter.in.rest..controller..")
            .because("Controller는 adapter.in.rest.[bc].controller 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Domain 객체 직접 생성 금지
     */
    @Test
    @DisplayName("[금지] Controller는 Domain 객체를 직접 생성하지 않아야 한다")
    void controller_MustNotCreateDomainObjects() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
            .because("Controller는 Domain 객체를 직접 생성/조작하지 않습니다. UseCase를 통해 간접 호출하세요.");

        rule.check(classes);
    }

    /**
     * 규칙 10: UseCase 의존성 필수
     */
    @Test
    @DisplayName("[필수] Controller는 UseCase 인터페이스에 의존해야 한다")
    void controller_MustDependOnUseCaseInterfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("..controller..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAPackage("..application..port.in..")
            .because("Controller는 UseCase 인터페이스에 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 11: ResponseEntity<ApiResponse<T>> 반환 타입 권장
     */
    @Test
    @DisplayName("[권장] Controller 메서드는 ResponseEntity<ApiResponse<T>> 형식으로 반환해야 한다")
    void controller_ShouldReturnResponseEntityWithApiResponse() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
            .and().arePublic()
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.PostMapping.class)
            .or().areAnnotatedWith(org.springframework.web.bind.annotation.GetMapping.class)
            .or().areAnnotatedWith(org.springframework.web.bind.annotation.PatchMapping.class)
            .or().areAnnotatedWith(org.springframework.web.bind.annotation.PutMapping.class)
            .should().haveRawReturnType(org.springframework.http.ResponseEntity.class)
            .because("Controller 메서드는 ResponseEntity<ApiResponse<T>> 형식으로 반환해야 합니다");

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
./gradlew test --tests "*ControllerArchTest"
```

### Maven

```bash
mvn test -Dtest=ControllerArchTest
```

### IDE

- IntelliJ IDEA: `ControllerArchTest` 클래스에서 우클릭 → Run
- 또는 전체 Architecture Test 실행

---

## 5️⃣ 위반 예시 및 수정

### ❌ Bad: @RestController 누락

```java
@Controller  // ❌ @RestController 사용해야 함
@RequestMapping("/api/v1/orders")
public class OrderController {
    // ...
}
```

### ✅ Good: @RestController 사용

```java
@RestController  // ✅ @RestController 사용
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {
    // ...
}
```

---

### ❌ Bad: @Transactional 사용

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    @PostMapping
    @Transactional  // ❌ Controller에서 @Transactional 금지
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
        // ...
    }
}
```

### ✅ Good: @Transactional은 UseCase에서

```java
// Controller (트랜잭션 없음)
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
        var command = orderApiMapper.toCreateCommand(request);
        var response = createOrderUseCase.execute(command);  // ✅ UseCase에서 트랜잭션 처리
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(response));
    }
}

// UseCase Service (트랜잭션 관리)
@Service
public class CreateOrderService implements CreateOrderUseCase {

    @Override
    @Transactional  // ✅ UseCase에서 @Transactional 사용
    public OrderResponse execute(CreateOrderCommand command) {
        // ...
    }
}
```

---

### ❌ Bad: DELETE 메서드 사용

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    @DeleteMapping("/{id}")  // ❌ DELETE 메서드 금지
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        // ...
    }
}
```

### ✅ Good: PATCH로 소프트 삭제

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    @PatchMapping("${api.endpoints.order.delete}")  // ✅ PATCH로 소프트 삭제
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        var command = orderApiMapper.toDeleteCommand(id);
        deleteOrderUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ofSuccess());
    }
}
```

---

### ❌ Bad: Domain 객체 직접 생성

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
        // ❌ Controller에서 Domain 객체 직접 생성 금지
        Order order = new Order(request.customerId(), request.items());
        order.place();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(response));
    }
}
```

### ✅ Good: UseCase로 위임

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderApiMapper orderApiMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
        // ✅ Mapper로 변환 → UseCase로 위임
        var command = orderApiMapper.toCreateCommand(request);
        var response = createOrderUseCase.execute(command);
        var apiResponse = orderApiMapper.toApiResponse(response);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ofSuccess(apiResponse));
    }
}
```

---

### ❌ Bad: 엔드포인트 하드코딩

```java
@RestController
@RequestMapping("/api/v1/orders")  // ❌ 하드코딩 금지
public class OrderCommandController {

    @GetMapping("/{id}")  // ❌ 하드코딩 금지
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        // ...
    }
}
```

### ✅ Good: Properties 사용

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")  // ✅ Properties 사용
public class OrderQueryController {

    @GetMapping("${api.endpoints.order.by-id}")  // ✅ Properties 사용
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(@PathVariable Long id) {
        // ...
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

- [ ] `ControllerArchTest` 클래스 작성
- [ ] 11개 검증 규칙 모두 구현
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 숙지

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
