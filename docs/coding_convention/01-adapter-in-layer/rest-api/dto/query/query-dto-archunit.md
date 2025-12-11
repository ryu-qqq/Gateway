# Query DTO ArchUnit 검증 규칙

> **목적**: Query DTO 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1️⃣ 검증 항목 (완전 강제)

### Query DTO 검증 규칙
1. ✅ **Record 타입 필수** - `public record` 키워드 사용
2. ✅ **네이밍 규칙** - `*ApiRequest` 접미사 필수 (SearchApiRequest, ListApiRequest 등)
3. ❌ **Lombok 어노테이션 절대 금지** (@Data, @Builder, @Getter, @Setter 등)
4. ❌ **Jackson 어노테이션 절대 금지** (@JsonFormat, @JsonProperty, @JsonIgnore 등)
5. ❌ **Domain 변환 메서드 금지** (toCriteria(), toFilter() 등)
6. ❌ **비즈니스 로직 금지** (계산 메서드, 복잡한 검증 등)
7. ✅ **Bean Validation 사용** (@Min, @Max 등 - Command보다 느슨)
8. ✅ **패키지 위치**: adapter-in.rest-api.[bc].dto.query
9. ✅ **Nested Record 허용** - 복잡한 조건은 Nested Record로 표현
10. ✅ **Compact Constructor 허용** - 기본값 설정, 간단한 날짜 범위 검증

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
 * Query DTO ArchUnit 검증 테스트 (완전 강제)
 *
 * <p>모든 Query DTO는 정확히 이 규칙을 따라야 합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Query DTO ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
@Tag("adapter-rest")
class QueryDtoArchTest {

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
    @DisplayName("[필수] Query DTO는 Record 타입이어야 한다")
    void queryDto_MustBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()  // Nested Record는 제외
            .should().beRecords()
            .because("Query DTO는 불변 객체이므로 Record를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: 네이밍 규칙 (*ApiRequest)
     */
    @Test
    @DisplayName("[필수] Query DTO는 *ApiRequest 접미사를 가져야 한다")
    void queryDto_MustHaveApiRequestSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().areNotNestedClasses()
            .should().haveSimpleNameEndingWith("ApiRequest")
            .because("Query DTO는 *ApiRequest 네이밍 규칙을 따라야 합니다 (예: SearchApiRequest, ListApiRequest)");

        rule.check(classes);
    }

    /**
     * 규칙 3: Lombok 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("Query DTO는 Pure Java Record를 사용해야 하며 Lombok은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Jackson 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Jackson 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseJackson() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonProperty")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonFormat")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonIgnore")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.annotation.JsonInclude")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonSerialize")
            .orShould().beAnnotatedWith("com.fasterxml.jackson.databind.annotation.JsonDeserialize")
            .because("Query DTO는 프레임워크 독립적이어야 하며 Jackson 어노테이션은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Domain 변환 메서드 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Domain 변환 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveDomainConversionMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().haveMethodNames("toCriteria", "toFilter", "toSearchCondition", "toQuery")
            .because("Query DTO → Domain 변환은 Mapper의 책임입니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: 비즈니스 로직 메서드 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 비즈니스 로직 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveBusinessLogicMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().haveMethodNames("calculate", "compute", "isValid", "check")
            .because("Query DTO는 검색 조건만 담당하며 비즈니스 로직은 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Bean Validation 어노테이션 사용 권장 (페이징 필드에만)
     */
    @Test
    @DisplayName("[권장] Query DTO는 페이징 필드에 Bean Validation을 사용해야 한다")
    void queryDto_ShouldUseValidationForPaging() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Query DTO는 페이징 필드(page, size)에 Bean Validation을 사용해야 합니다");

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
    @DisplayName("[필수] Query DTO는 올바른 패키지에 위치해야 한다")
    void queryDto_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("ApiRequest")
            .and().areNotNestedClasses()
            .and().resideInAPackage("..adapter.in.rest..")
            .and().resideInAPackage("..dto..")
            .and().areNotInterfaces()
            .should().resideInAPackage("..dto.query..")
            .orShould().resideInAPackage("..dto.command..")  // Command도 *ApiRequest이므로 허용
            .because("Query DTO는 dto.query 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 9: Setter 메서드 절대 금지 (Record이므로 자동 검증)
     */
    @Test
    @DisplayName("[금지] Query DTO는 Setter 메서드를 가지지 않아야 한다")
    void queryDto_MustNotHaveSetterMethods() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().haveMethodsThat().haveNameMatching("set[A-Z].*")
            .because("Query DTO는 불변 객체이므로 Setter는 금지됩니다");

        rule.check(classes);
    }

    /**
     * 규칙 10: Spring 어노테이션 절대 금지
     */
    @Test
    @DisplayName("[금지] Query DTO는 Spring 어노테이션을 가지지 않아야 한다")
    void queryDto_MustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto.query..")
            .should().beAnnotatedWith("org.springframework.stereotype.Component")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.context.annotation.Configuration")
            .because("Query DTO는 순수 데이터 전송 객체이므로 Spring 어노테이션은 금지됩니다");

        rule.check(classes);
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle
```bash
./gradlew test --tests "*QueryDtoArchTest"
```

### Maven
```bash
mvn test -Dtest=QueryDtoArchTest
```

### IDE
- IntelliJ IDEA: `QueryDtoArchTest` 클래스에서 우클릭 → Run
- 또는 전체 Architecture Test 실행

---

## 5️⃣ 위반 예시 및 수정

### ❌ Bad: Lombok 사용
```java
@Data  // ❌ 금지
@Builder  // ❌ 금지
public class OrderSearchApiRequest {
    private Long customerId;
    private Integer page;
    private Integer size;
}
```

### ✅ Good: Record 사용
```java
public record OrderSearchApiRequest(
    Long customerId,  // ✅ Optional (null 허용)
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size
) {
    public OrderSearchApiRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
```

---

### ❌ Bad: Domain 변환 메서드
```java
public record OrderSearchApiRequest(...) {
    public OrderSearchCriteria toCriteria() {  // ❌ 금지
        return new OrderSearchCriteria(...);
    }
}
```

### ✅ Good: Mapper 분리
```java
// Query DTO (변환 로직 없음)
public record OrderSearchApiRequest(...) {}

// Mapper (변환 책임)
@Component
public class OrderApiMapper {
    public OrderSearchQuery toQuery(OrderSearchApiRequest request) {
        return new OrderSearchQuery(...);
    }
}
```

---

### ❌ Bad: 페이징 조건 없음
```java
public record OrderSearchApiRequest(
    Long customerId,
    String status
    // ❌ 페이징 조건 필수!
) {}
```

### ✅ Good: 페이징 조건 포함
```java
public record OrderSearchApiRequest(
    Long customerId,
    String status,
    @Min(0) Integer page,      // ✅ 필수
    @Min(1) @Max(100) Integer size  // ✅ 필수
) {
    public OrderSearchApiRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
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

- [ ] `QueryDtoArchTest` 클래스 작성
- [ ] 10개 검증 규칙 모두 구현
- [ ] 빌드 시 자동 실행 설정
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 숙지

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
