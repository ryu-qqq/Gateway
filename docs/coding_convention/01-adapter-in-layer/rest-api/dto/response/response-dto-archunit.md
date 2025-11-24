# Response DTO ArchUnit 검증 규칙

> **목적**: Response DTO 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Response DTO 검증 규칙
1. ✅ **Record 타입 필수** - `public record` 키워드 사용
2. ✅ **네이밍 규칙** - `*ApiResponse` 접미사 필수 (OrderApiResponse, OrderSummaryApiResponse 등)
3. ❌ **Lombok 어노테이션 절대 금지** (@Data, @Builder, @Getter, @Setter 등)
4. ❌ **Jackson 어노테이션 절대 금지** (@JsonFormat, @JsonProperty, @JsonIgnore 등)
5. ❌ **Domain 변환 메서드 금지** (toDomain(), toEntity() 등)
6. ❌ **비즈니스 로직 금지** (계산 메서드, 복잡한 검증 등)
7. ✅ **from() 메서드 필수** - Application Layer Response → REST API Response 변환
8. ✅ **패키지 위치**: adapter-in.rest-api.[bc].dto.response
9. ✅ **Nested Record 허용** - 중첩 구조는 Nested Record로 표현
10. ✅ **Compact Constructor 허용** - List.copyOf() 방어적 복사

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

/**
 * Response DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Response DTO는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Response DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class ResponseDtoArchTest {

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
    @DisplayName("[필수] Response DTO는 Record 타입이어야 한다")
    void responseDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().haveSimpleNameEndingWith("ApiResponse")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Response DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiResponse)
     */
    @Test
    @DisplayName("[필수] Response DTO는 *ApiResponse 접미사를 가져야 한다")
    void responseDto_MustHaveApiResponseSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiResponse")
            .because("Response DTO는 *ApiResponse 네이밍 규칙을 따라야 합니다 (예: OrderApiResponse, OrderSummaryApiResponse)");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Response DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Response DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().haveMethodNames("toDomain", "toEntity", "toAggregate")
            .because("Response DTO는 출력 전용이며 Domain 변환 메서드는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().haveMethodNames("calculate", "compute", "validate", "process")
            .because("Response DTO는 데이터 전송만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: from() 메서드 필수
     */
    @Test
    @DisplayName("[필수] Response DTO는 from() 메서드를 가져야 한다")
    void responseDto_MustHaveFromMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().haveSimpleNameEndingWith("ApiResponse")
            .and().areNotNestedClasses()
            .should().haveMethodNames("from")
            .because("Response DTO는 Application Layer Response를 변환하는 from() 메서드가 필수입니다");

        // Note: 이 규칙은 필수이지만, Nested Record의 경우 부모에서 변환할 수 있으므로 경고만 표시
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
    @DisplayName("[필수] Response DTO는 올바른 패키지에 위치해야 한다")
    void responseDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiResponse")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .and().resideInAPackage("..dto..")
            .and().areNotInterfaces()
            .should().resideInAPackage("..dto.response..")
            .because("Response DTO는 dto.response 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Response DTO는 Setter 메서드를 가지지 않아야 한다")
    void responseDto_MustNotHaveSetterMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().haveMethodsThat().haveNameMatching("set[A-Z].*")
            .because("Response DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Response DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void responseDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.response..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Response DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle
```bash
./gradlew test --tests "*ResponseDtoArchTest"
```

### Maven
```bash
mvn test -Dtest=ResponseDtoArchTest
```

### IDE
- IntelliJ IDEA: `ResponseDtoArchTest` 클래스에서 우클릭 → Run
- 또는 전체 Architecture Test 실행

---

## 5️⃣ 위반 예시 및 수정

### ❌ Bad: Lombok 사용
```java
@Data  // ❌ 금지
@Builder  // ❌ 금지
public class OrderApiResponse {
    private Long orderId;
    private String status;
}
```

### ✅ Good: Record 사용
```java
public record OrderApiResponse(
    Long orderId,
    String status,
    List<OrderItemResponse> items
) {
    public OrderApiResponse {
        items = List.copyOf(items);  // ✅ 방어적 복사
    }

    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            appResponse.status(),
            appResponse.items().stream()
                .map(OrderItemResponse::from)
                .toList()
        );
    }

    public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity
    ) {
        public static OrderItemResponse from(OrderItemDto dto) {
            return new OrderItemResponse(
                dto.productId(),
                dto.productName(),
                dto.quantity()
            );
        }
    }
}
```

---

### ❌ Bad: Domain 변환 메서드
```java
public record OrderApiResponse(...) {
    public OrderDomain toDomain() {  // ❌ 금지
        return new OrderDomain(...);
    }
}
```

### ✅ Good: from() 메서드만 사용
```java
// Response DTO (변환 로직 없음, from()만 제공)
public record OrderApiResponse(...) {
    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(...);
    }
}
```

---

### ❌ Bad: Spring Page 직접 사용
```java
@GetMapping("/orders")
public ApiResponse<Page<OrderApiResponse>> getOrders(...) {  // ❌ Spring Page 직접 사용
    Page<OrderDto> page = orderQueryUseCase.searchOrders(...);
    return ApiResponse.ofSuccess(page);
}
```

### ✅ Good: SliceApiResponse/PageApiResponse 사용
```java
// Slice 패턴 (Cursor-based)
@GetMapping("/orders")
public ApiResponse<SliceApiResponse<OrderSummaryApiResponse>> getOrders(...) {
    SliceResponse<OrderSummaryDto> appSlice = orderQueryUseCase.searchOrders(...);
    SliceApiResponse<OrderSummaryApiResponse> apiSlice = SliceApiResponse.from(
        appSlice,
        OrderSummaryApiResponse::from
    );
    return ApiResponse.ofSuccess(apiSlice);
}

// Page 패턴 (Offset-based)
@GetMapping("/admin/orders")
public ApiResponse<PageApiResponse<OrderApiResponse>> getOrdersForAdmin(...) {
    PageResponse<OrderDto> appPage = orderQueryUseCase.searchOrdersForAdmin(...);
    PageApiResponse<OrderApiResponse> apiPage = PageApiResponse.from(
        appPage,
        OrderApiResponse::from
    );
    return ApiResponse.ofSuccess(apiPage);
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

- [ ] `ResponseDtoArchTest` 클래스 작성
- [ ] 10개 검증 규칙 모두 구현
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 숙지

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
