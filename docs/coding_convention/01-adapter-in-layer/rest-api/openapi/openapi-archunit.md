# OpenAPI ArchUnit Guide — **OpenAPI 어노테이션 강제 규칙**

> 이 문서는 `adapter-in/rest-api` 레이어의 **OpenAPI ArchUnit 규칙**입니다.
>
> Controller, DTO의 OpenAPI 어노테이션 적용을 자동으로 검증합니다.

---

## 1) 테스트 클래스 구조

```java
@AnalyzeClasses(
    packages = "com.ryuqq.adapter.in.rest",
    importOptions = ImportOption.DoNotIncludeTests.class
)
@DisplayName("OpenAPI ArchUnit 규칙")
class OpenApiArchTest {

    // ======= 상수 정의 =======
    private static final String CONTROLLER_PACKAGE = "..controller..";
    private static final String DTO_COMMAND_PACKAGE = "..dto.command..";
    private static final String DTO_QUERY_PACKAGE = "..dto.query..";
    private static final String DTO_RESPONSE_PACKAGE = "..dto.response..";

    // ... 규칙들
}
```

---

## 2) Controller 규칙 (6개)

### 2.1) Controller에 @Tag 필수

```java
@ArchTest
@DisplayName("Controller 클래스는 @Tag 어노테이션이 있어야 한다")
static final ArchRule controller_should_have_tag_annotation =
    classes()
        .that().resideInAPackage(CONTROLLER_PACKAGE)
        .and().haveSimpleNameEndingWith("Controller")
        .and().areAnnotatedWith(RestController.class)
        .should().beAnnotatedWith(Tag.class)
        .because("Controller는 @Tag로 API 그룹을 정의해야 합니다");
```

### 2.2) Controller 메서드에 @Operation 필수

```java
@ArchTest
@DisplayName("Controller public 메서드는 @Operation 어노테이션이 있어야 한다")
static final ArchRule controller_methods_should_have_operation_annotation =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
        .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
        .and().arePublic()
        .and().areNotConstructors()
        .and(areHttpMappingMethods())
        .should().beAnnotatedWith(Operation.class)
        .because("모든 API 메서드는 @Operation으로 설명이 필요합니다");

private static DescribedPredicate<JavaMethod> areHttpMappingMethods() {
    return new DescribedPredicate<>("HTTP 매핑 메서드") {
        @Override
        public boolean test(JavaMethod method) {
            return method.isAnnotatedWith(GetMapping.class)
                || method.isAnnotatedWith(PostMapping.class)
                || method.isAnnotatedWith(PutMapping.class)
                || method.isAnnotatedWith(PatchMapping.class)
                || method.isAnnotatedWith(DeleteMapping.class)
                || method.isAnnotatedWith(RequestMapping.class);
        }
    };
}
```

### 2.3) Controller 메서드에 @ApiResponses 필수

```java
@ArchTest
@DisplayName("Controller public 메서드는 @ApiResponses 어노테이션이 있어야 한다")
static final ArchRule controller_methods_should_have_api_responses =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
        .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
        .and().arePublic()
        .and(areHttpMappingMethods())
        .should().beAnnotatedWith(ApiResponses.class)
        .because("모든 API 메서드는 @ApiResponses로 응답 코드를 정의해야 합니다");
```

### 2.4) PathVariable에 @Parameter 필수

```java
@ArchTest
@DisplayName("@PathVariable 파라미터는 @Parameter 어노테이션이 있어야 한다")
static final ArchRule path_variable_should_have_parameter_annotation =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
        .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
        .should(haveParameterAnnotationOnPathVariable())
        .because("PathVariable은 @Parameter로 설명이 필요합니다");

private static ArchCondition<JavaMethod> haveParameterAnnotationOnPathVariable() {
    return new ArchCondition<>("PathVariable에 @Parameter가 있어야 함") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            for (JavaParameter param : method.getParameters()) {
                boolean hasPathVariable = param.isAnnotatedWith(PathVariable.class);
                boolean hasParameter = param.isAnnotatedWith(Parameter.class);

                if (hasPathVariable && !hasParameter) {
                    events.add(SimpleConditionEvent.violated(
                        method,
                        String.format("%s의 @PathVariable 파라미터에 @Parameter가 없습니다",
                            method.getFullName())
                    ));
                }
            }
        }
    };
}
```

### 2.5) RequestParam에 @Parameter 필수

```java
@ArchTest
@DisplayName("@RequestParam 파라미터는 @Parameter 어노테이션이 있어야 한다")
static final ArchRule request_param_should_have_parameter_annotation =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
        .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
        .should(haveParameterAnnotationOnRequestParam())
        .because("RequestParam은 @Parameter로 설명이 필요합니다");

private static ArchCondition<JavaMethod> haveParameterAnnotationOnRequestParam() {
    return new ArchCondition<>("RequestParam에 @Parameter가 있어야 함") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            for (JavaParameter param : method.getParameters()) {
                boolean hasRequestParam = param.isAnnotatedWith(RequestParam.class);
                boolean hasParameter = param.isAnnotatedWith(Parameter.class);

                if (hasRequestParam && !hasParameter) {
                    events.add(SimpleConditionEvent.violated(
                        method,
                        String.format("%s의 @RequestParam 파라미터에 @Parameter가 없습니다",
                            method.getFullName())
                    ));
                }
            }
        }
    };
}
```

### 2.6) @Operation에 summary 필수

```java
@ArchTest
@DisplayName("@Operation 어노테이션에 summary가 비어있으면 안 된다")
static final ArchRule operation_should_have_summary =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
        .and().areAnnotatedWith(Operation.class)
        .should(haveNonEmptyOperationSummary())
        .because("@Operation의 summary는 필수입니다");

private static ArchCondition<JavaMethod> haveNonEmptyOperationSummary() {
    return new ArchCondition<>("Operation.summary가 비어있지 않아야 함") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            Operation operation = method.getAnnotationOfType(Operation.class);
            if (operation != null && operation.summary().isBlank()) {
                events.add(SimpleConditionEvent.violated(
                    method,
                    String.format("%s의 @Operation.summary가 비어있습니다",
                        method.getFullName())
                ));
            }
        }
    };
}
```

---

## 3) DTO 규칙 (6개)

### 3.1) Request DTO 클래스에 @Schema 필수

```java
@ArchTest
@DisplayName("Command Request DTO는 @Schema 어노테이션이 있어야 한다")
static final ArchRule command_request_dto_should_have_schema =
    classes()
        .that().resideInAPackage(DTO_COMMAND_PACKAGE)
        .and().haveSimpleNameEndingWith("ApiRequest")
        .and().areRecords()
        .should().beAnnotatedWith(Schema.class)
        .because("Request DTO는 @Schema로 문서화해야 합니다");
```

### 3.2) Query Request DTO 클래스에 @Schema 필수

```java
@ArchTest
@DisplayName("Query Request DTO는 @Schema 어노테이션이 있어야 한다")
static final ArchRule query_request_dto_should_have_schema =
    classes()
        .that().resideInAPackage(DTO_QUERY_PACKAGE)
        .and().haveSimpleNameEndingWith("ApiRequest")
        .and().areRecords()
        .should().beAnnotatedWith(Schema.class)
        .because("Request DTO는 @Schema로 문서화해야 합니다");
```

### 3.3) Response DTO 클래스에 @Schema 필수

```java
@ArchTest
@DisplayName("Response DTO는 @Schema 어노테이션이 있어야 한다")
static final ArchRule response_dto_should_have_schema =
    classes()
        .that().resideInAPackage(DTO_RESPONSE_PACKAGE)
        .and().haveSimpleNameEndingWith("ApiResponse")
        .and().areRecords()
        .should().beAnnotatedWith(Schema.class)
        .because("Response DTO는 @Schema로 문서화해야 합니다");
```

### 3.4) DTO 필드에 @Schema 필수 (Record Components)

```java
@ArchTest
@DisplayName("Request/Response DTO의 모든 필드는 @Schema가 있어야 한다")
static final ArchRule dto_fields_should_have_schema =
    classes()
        .that().resideInAnyPackage(DTO_COMMAND_PACKAGE, DTO_QUERY_PACKAGE, DTO_RESPONSE_PACKAGE)
        .and().areRecords()
        .and().haveSimpleNameMatching(".*Api(Request|Response)")
        .should(haveSchemaOnAllRecordComponents())
        .because("DTO의 모든 필드는 @Schema로 문서화해야 합니다");

private static ArchCondition<JavaClass> haveSchemaOnAllRecordComponents() {
    return new ArchCondition<>("모든 Record 컴포넌트에 @Schema가 있어야 함") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            for (JavaField field : javaClass.getFields()) {
                // synthetic 필드 제외
                if (field.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                    continue;
                }

                boolean hasSchema = field.isAnnotatedWith(Schema.class);
                if (!hasSchema) {
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        String.format("%s.%s 필드에 @Schema가 없습니다",
                            javaClass.getSimpleName(), field.getName())
                    ));
                }
            }
        }
    };
}
```

### 3.5) @Schema에 description 필수

```java
@ArchTest
@DisplayName("@Schema 어노테이션에 description이 비어있으면 안 된다")
static final ArchRule schema_should_have_description =
    classes()
        .that().resideInAnyPackage(DTO_COMMAND_PACKAGE, DTO_QUERY_PACKAGE, DTO_RESPONSE_PACKAGE)
        .and().areRecords()
        .and().areAnnotatedWith(Schema.class)
        .should(haveNonEmptySchemaDescription())
        .because("@Schema의 description은 필수입니다");

private static ArchCondition<JavaClass> haveNonEmptySchemaDescription() {
    return new ArchCondition<>("Schema.description이 비어있지 않아야 함") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            Schema schema = javaClass.getAnnotationOfType(Schema.class);
            if (schema != null && schema.description().isBlank()) {
                events.add(SimpleConditionEvent.violated(
                    javaClass,
                    String.format("%s의 @Schema.description이 비어있습니다",
                        javaClass.getSimpleName())
                ));
            }
        }
    };
}
```

### 3.6) Enum에 @Schema(enumAsRef) 필수

```java
@ArchTest
@DisplayName("DTO에서 사용하는 Enum은 @Schema(enumAsRef = true)가 있어야 한다")
static final ArchRule enum_should_have_schema_enum_as_ref =
    classes()
        .that().resideInAPackage("..dto..")
        .and().areEnums()
        .should().beAnnotatedWith(Schema.class)
        .because("Enum은 @Schema로 문서화해야 합니다");
```

---

## 4) 전체 테스트 클래스

```java
package com.ryuqq.adapter.in.rest.architecture.openapi;

import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.bind.annotation.*;

@AnalyzeClasses(
    packages = "com.ryuqq.adapter.in.rest",
    importOptions = ImportOption.DoNotIncludeTests.class
)
@DisplayName("OpenAPI ArchUnit 규칙")
class OpenApiArchTest {

    // ======= 패키지 상수 =======
    private static final String CONTROLLER_PACKAGE = "..controller..";
    private static final String DTO_COMMAND_PACKAGE = "..dto.command..";
    private static final String DTO_QUERY_PACKAGE = "..dto.query..";
    private static final String DTO_RESPONSE_PACKAGE = "..dto.response..";

    // ======= Controller 규칙 =======

    @ArchTest
    @DisplayName("Controller 클래스는 @Tag 어노테이션이 있어야 한다")
    static final ArchRule controller_should_have_tag_annotation =
        classes()
            .that().resideInAPackage(CONTROLLER_PACKAGE)
            .and().haveSimpleNameEndingWith("Controller")
            .and().areAnnotatedWith(RestController.class)
            .should().beAnnotatedWith(Tag.class)
            .because("Controller는 @Tag로 API 그룹을 정의해야 합니다");

    @ArchTest
    @DisplayName("Controller public 메서드는 @Operation 어노테이션이 있어야 한다")
    static final ArchRule controller_methods_should_have_operation_annotation =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
            .and().arePublic()
            .and(areHttpMappingMethods())
            .should().beAnnotatedWith(Operation.class)
            .because("모든 API 메서드는 @Operation으로 설명이 필요합니다");

    @ArchTest
    @DisplayName("Controller public 메서드는 @ApiResponses 어노테이션이 있어야 한다")
    static final ArchRule controller_methods_should_have_api_responses =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage(CONTROLLER_PACKAGE)
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
            .and().arePublic()
            .and(areHttpMappingMethods())
            .should().beAnnotatedWith(ApiResponses.class)
            .because("모든 API 메서드는 @ApiResponses로 응답 코드를 정의해야 합니다");

    // ======= DTO 규칙 =======

    @ArchTest
    @DisplayName("Command Request DTO는 @Schema 어노테이션이 있어야 한다")
    static final ArchRule command_request_dto_should_have_schema =
        classes()
            .that().resideInAPackage(DTO_COMMAND_PACKAGE)
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areRecords()
            .should().beAnnotatedWith(Schema.class)
            .because("Request DTO는 @Schema로 문서화해야 합니다");

    @ArchTest
    @DisplayName("Query Request DTO는 @Schema 어노테이션이 있어야 한다")
    static final ArchRule query_request_dto_should_have_schema =
        classes()
            .that().resideInAPackage(DTO_QUERY_PACKAGE)
            .and().haveSimpleNameEndingWith("ApiRequest")
            .and().areRecords()
            .should().beAnnotatedWith(Schema.class)
            .because("Request DTO는 @Schema로 문서화해야 합니다");

    @ArchTest
    @DisplayName("Response DTO는 @Schema 어노테이션이 있어야 한다")
    static final ArchRule response_dto_should_have_schema =
        classes()
            .that().resideInAPackage(DTO_RESPONSE_PACKAGE)
            .and().haveSimpleNameEndingWith("ApiResponse")
            .and().areRecords()
            .should().beAnnotatedWith(Schema.class)
            .because("Response DTO는 @Schema로 문서화해야 합니다");

    // ======= Helper Methods =======

    private static DescribedPredicate<JavaMethod> areHttpMappingMethods() {
        return new DescribedPredicate<>("HTTP 매핑 메서드") {
            @Override
            public boolean test(JavaMethod method) {
                return method.isAnnotatedWith(GetMapping.class)
                    || method.isAnnotatedWith(PostMapping.class)
                    || method.isAnnotatedWith(PutMapping.class)
                    || method.isAnnotatedWith(PatchMapping.class)
                    || method.isAnnotatedWith(DeleteMapping.class)
                    || method.isAnnotatedWith(RequestMapping.class);
            }
        };
    }
}
```

---

## 5) 규칙 요약 (12개)

| 카테고리 | 규칙 | 검증 내용 |
|---------|------|----------|
| **Controller** | @Tag 필수 | Controller 클래스에 @Tag |
| **Controller** | @Operation 필수 | HTTP 메서드에 @Operation |
| **Controller** | @ApiResponses 필수 | HTTP 메서드에 @ApiResponses |
| **Controller** | @Parameter 필수 | @PathVariable에 @Parameter |
| **Controller** | @Parameter 필수 | @RequestParam에 @Parameter |
| **Controller** | summary 필수 | @Operation.summary 비어있지 않음 |
| **DTO** | Command @Schema 필수 | Command DTO 클래스에 @Schema |
| **DTO** | Query @Schema 필수 | Query DTO 클래스에 @Schema |
| **DTO** | Response @Schema 필수 | Response DTO 클래스에 @Schema |
| **DTO** | 필드 @Schema 필수 | 모든 필드에 @Schema |
| **DTO** | description 필수 | @Schema.description 비어있지 않음 |
| **Enum** | @Schema 필수 | Enum에 @Schema |

---

## 6) 관련 문서

| 문서 | 설명 |
|------|------|
| [OpenAPI Guide](./openapi-guide.md) | OpenAPI 어노테이션 가이드 |
| [Controller ArchUnit](../controller/controller-archunit.md) | Controller 아키텍처 규칙 |
| [DTO ArchUnit](../dto/command/command-dto-archunit.md) | DTO 아키텍처 규칙 |

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
