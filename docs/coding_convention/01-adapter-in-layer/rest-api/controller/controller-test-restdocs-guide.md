# Controller REST Docs Guide — **테스트 기반 API 문서 자동 생성**

> **목적**: Controller 테스트로 API 문서 자동 생성 및 동기화 유지
>
> **철학**: 코드가 문서, 테스트가 검증, 항상 최신 상태 보장

---

## 1️⃣ 핵심 원칙 (Core Principles)

### REST Docs는 테스트 기반 API 문서 생성 도구입니다

**코드와 문서가 항상 동기화됩니다.**

### 왜 REST Docs를 사용하는가?

| 비교 항목 | Swagger/OpenAPI | Spring REST Docs |
|----------|-----------------|------------------|
| **문서 생성** | 어노테이션 기반 | 테스트 기반 |
| **코드 침투성** | 높음 (`@ApiOperation` 등) | 없음 (순수 테스트) |
| **정확성 보장** | ❌ 수동 유지보수 | ✅ 테스트 실패 시 빌드 실패 |
| **최신 상태** | ❌ 코드와 불일치 가능 | ✅ 항상 동기화 |
| **학습 곡선** | 낮음 | 중간 |
| **추천 용도** | 빠른 프로토타입 | 프로덕션 API 문서 |

### REST Docs의 장점

- ✅ **테스트 실패 = 문서 생성 실패** → 항상 정확한 문서
- ✅ **코드 침투성 없음** → 순수한 비즈니스 로직 유지
- ✅ **AsciiDoc 기반** → 유연한 문서 구조
- ✅ **버전 관리** → 문서도 Git으로 관리
- ✅ **OpenAPI 변환 가능** → Swagger UI 통합 가능

---

## 2️⃣ Gradle 설정 (Build Configuration)

### 멀티모듈 아키텍처

본 프로젝트는 **멀티모듈 구조**로 REST Docs를 설정합니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                         빌드 시점                                │
├─────────────────────────────────────────────────────────────────┤
│  adapter-in/rest-api                                            │
│  └── src/test/java/                                            │
│      └── **/*DocsTest.java  →  ./gradlew test                   │
│                              ↓                                  │
│  build/generated-snippets/   (adoc 스니펫 생성)                  │
│                              ↓                                  │
│  bootstrap/bootstrap-web-api                                    │
│  └── ./gradlew asciidoctor                                      │
│                              ↓                                  │
│  build/docs/asciidoc/index.html  (HTML 문서 생성)                │
│                              ↓                                  │
│  src/main/resources/static/docs/  (복사)                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         런타임 시점                              │
├─────────────────────────────────────────────────────────────────┤
│  Spring Boot 앱                                                 │
│  └── GET /docs/index.html  →  static 리소스로 서빙               │
└─────────────────────────────────────────────────────────────────┘
```

| 모듈 | 역할 |
|------|------|
| `adapter-in/rest-api` | 테스트 실행 → Snippets 생성 |
| `bootstrap/bootstrap-web-api` | Asciidoctor → HTML 생성 → JAR 패키징 |

### Version Catalog 설정 (gradle/libs.versions.toml)

```toml
[versions]
restdocs = "3.0.1"
asciidoctor = "3.3.2"

[libraries]
spring-restdocs-mockmvc = { module = "org.springframework.restdocs:spring-restdocs-mockmvc", version.ref = "restdocs" }
spring-restdocs-asciidoctor = { module = "org.springframework.restdocs:spring-restdocs-asciidoctor", version.ref = "restdocs" }

[plugins]
asciidoctor = { id = "org.asciidoctor.jvm.convert", version.ref = "asciidoctor" }
```

### adapter-in/rest-api/build.gradle

```gradle
plugins {
    id 'java-library'
}

// ========================================
// REST Docs Snippets Output Directory
// ========================================
ext {
    snippetsDir = file('build/generated-snippets')
}

dependencies {
    // REST Docs (테스트 의존성)
    testImplementation libs.spring.restdocs.mockmvc
}

tasks.test {
    outputs.dir snippetsDir  // ✅ 테스트 실행 시 Snippet 생성
}
```

### bootstrap/bootstrap-web-api/build.gradle

```gradle
plugins {
    id 'java'
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.asciidoctor)  // ✅ Asciidoctor 플러그인
}

// ========================================
// REST Docs Configuration
// ========================================
ext {
    // rest-api 모듈의 snippets 디렉토리 참조
    snippetsDir = file("${project(':adapter-in:rest-api').buildDir}/generated-snippets")
}

configurations {
    asciidoctorExt  // ✅ REST Docs 확장
}

dependencies {
    // Adapters
    implementation project(':adapter-in:rest-api')

    // REST Docs (Asciidoctor Extension)
    asciidoctorExt libs.spring.restdocs.asciidoctor
}

// ========================================
// Asciidoctor Task
// ========================================
asciidoctor {
    dependsOn ':adapter-in:rest-api:test'  // ✅ rest-api 테스트 먼저 실행

    inputs.dir snippetsDir
    configurations 'asciidoctorExt'

    sources {
        include '**/index.adoc'  // ✅ 메인 문서만 처리
    }

    baseDirFollowsSourceFile()  // ✅ include 상대 경로 지원
}

// ========================================
// Copy Docs to Static Resources
// ========================================
tasks.register('copyDocs', Copy) {
    dependsOn asciidoctor
    from "${asciidoctor.outputDir}"
    into "${sourceSets.main.output.resourcesDir}/static/docs"
}

bootJar {
    dependsOn copyDocs  // ✅ JAR 패키징 시 문서 포함
}
```

### 디렉토리 구조

```
bootstrap/bootstrap-web-api/
├── src/
│   ├── docs/
│   │   └── asciidoc/
│   │       └── index.adoc          # 메인 문서 (스니펫 include)
│   └── main/
│       └── resources/
│           └── static/
│               └── docs/           # 빌드된 HTML 위치
│                   └── .gitkeep
└── build/
    └── docs/
        └── asciidoc/
            └── index.html          # 생성된 HTML
```

### 빌드 플로우 (멀티모듈)

```
./gradlew :bootstrap:bootstrap-web-api:bootJar
    ↓
1. :adapter-in:rest-api:test 실행
    → adapter-in/rest-api/build/generated-snippets/ 생성
    ↓
2. :bootstrap:bootstrap-web-api:asciidoctor 실행
    → src/docs/asciidoc/index.adoc 읽기
    → Snippet include (rest-api 모듈 참조)
    → build/docs/asciidoc/index.html 생성
    ↓
3. :bootstrap:bootstrap-web-api:copyDocs 실행
    → HTML을 static/docs/로 복사
    ↓
4. :bootstrap:bootstrap-web-api:bootJar 실행
    → JAR에 문서 포함
    → /BOOT-INF/classes/static/docs/index.html
```

### 빌드 명령어

```bash
# 문서만 빌드
./gradlew :bootstrap:bootstrap-web-api:asciidoctor

# JAR 빌드 (문서 포함)
./gradlew :bootstrap:bootstrap-web-api:bootJar

# 생성된 HTML 확인
open bootstrap/bootstrap-web-api/build/docs/asciidoc/index.html
```

### Docker 배포

**마운트 불필요** - JAR 내부에 문서가 포함됩니다.

```dockerfile
# Dockerfile (변경 불필요)
COPY bootstrap/bootstrap-web-api/build/libs/*.jar app.jar

# static 리소스는 JAR 내부에 포함됨
# /BOOT-INF/classes/static/docs/index.html
```

---

## 3️⃣ 테스트 설정 (Test Configuration)

### REST Docs 테스트 Base 클래스

**위치**: `adapter-in/rest-api/src/test/java/com/ryuqq/adapter/in/rest/common/RestDocsTestSupport.java`

> ✅ **프로젝트에 이미 생성되어 있습니다!**
>
> 모든 REST Docs 테스트는 이 클래스를 상속받아 작성하면 됩니다.

**주요 기능**:
- `@ExtendWith(RestDocumentationExtension.class)` 자동 적용
- `MockMvc` 자동 설정 (Pretty Print)
- `ObjectMapper` 자동 주입

**사용 예시**:

```java
@WebMvcTest(OrderCommandController.class)
@DisplayName("OrderCommandController REST Docs")
class OrderCommandControllerDocsTest extends RestDocsTestSupport {  // ✅ 상속

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 API 문서")
    void createOrder() throws Exception {
        // mockMvc, objectMapper는 상속받아 사용 가능
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andDo(document("order-create", /* ... */));
    }
}
```

**전체 코드 확인**: `com.ryuqq.adapter.in.rest.common.RestDocsTestSupport`

### Controller 테스트에 REST Docs 적용

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.ryuqq.adapter.in.rest.common.RestDocsTestSupport;
import com.ryuqq.adapter.in.rest.order.dto.command.CreateOrderApiRequest;
import com.ryuqq.adapter.in.rest.order.mapper.OrderApiMapper;
import com.ryuqq.application.order.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderCommandController REST Docs 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@WebMvcTest(OrderCommandController.class)
@DisplayName("OrderCommandController REST Docs")
@Tag("restdocs")        // ✅ REST Docs 테스트
@Tag("adapter-rest")    // ✅ REST API Layer
class OrderCommandControllerDocsTest extends RestDocsTestSupport {

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private OrderApiMapper orderApiMapper;

    @Test
    @DisplayName("POST /api/v1/orders - 주문 생성 API 문서")
    void createOrder() throws Exception {
        // Given
        CreateOrderApiRequest request = new CreateOrderApiRequest(
            1L,
            List.of(
                new CreateOrderApiRequest.OrderItem(101L, 2),
                new CreateOrderApiRequest.OrderItem(102L, 1)
            )
        );

        given(orderApiMapper.toCommand(any())).willReturn(/* ... */);
        given(createOrderUseCase.execute(any())).willReturn(/* ... */);
        given(orderApiMapper.toApiResponse(any())).willReturn(/* ... */);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andDo(document("order-create",  // ✅ 문서 식별자
                requestFields(  // ✅ 요청 필드 문서화
                    fieldWithPath("customerId")
                        .description("고객 ID"),
                    fieldWithPath("items")
                        .description("주문 상품 목록"),
                    fieldWithPath("items[].productId")
                        .description("상품 ID"),
                    fieldWithPath("items[].quantity")
                        .description("주문 수량")
                ),
                responseFields(  // ✅ 응답 필드 문서화
                    fieldWithPath("success")
                        .description("성공 여부"),
                    fieldWithPath("data")
                        .description("응답 데이터"),
                    fieldWithPath("data.orderId")
                        .description("생성된 주문 ID"),
                    fieldWithPath("data.status")
                        .description("주문 상태"),
                    fieldWithPath("data.createdAt")
                        .description("주문 생성 시각"),
                    fieldWithPath("error")
                        .optional()
                        .description("에러 정보 (성공 시 null)")
                )
            ));
    }
}
```

---

## 4️⃣ Snippet 생성 패턴

### Request/Response Fields 문서화

```java
// Request Fields
requestFields(
    fieldWithPath("customerId").description("고객 ID"),
    fieldWithPath("items").description("주문 상품 목록"),
    fieldWithPath("items[].productId").description("상품 ID"),
    fieldWithPath("items[].quantity").description("주문 수량")
        .attributes(key("constraints").value("1 이상"))  // ✅ 제약사항 추가
)

// Response Fields
responseFields(
    fieldWithPath("success").description("성공 여부"),
    fieldWithPath("data").description("응답 데이터"),
    fieldWithPath("data.orderId").description("주문 ID"),
    fieldWithPath("error").optional().description("에러 정보")  // ✅ 선택 필드
)
```

### Path Parameters 문서화

```java
mockMvc.perform(get("/api/v1/orders/{id}", 1001L))
    .andDo(document("order-get",
        pathParameters(  // ✅ Path Variable 문서화
            parameterWithName("id").description("주문 ID")
        ),
        responseFields(/* ... */)
    ));
```

### Query Parameters 문서화

```java
mockMvc.perform(get("/api/v1/orders")
        .param("status", "PLACED")
        .param("page", "0")
        .param("size", "20"))
    .andDo(document("order-search",
        queryParameters(  // ✅ Query Parameter 문서화
            parameterWithName("status")
                .description("주문 상태")
                .optional(),
            parameterWithName("page")
                .description("페이지 번호 (0부터 시작)"),
            parameterWithName("size")
                .description("페이지 크기")
        ),
        responseFields(/* ... */)
    ));
```

### Request/Response Headers 문서화

```java
mockMvc.perform(post("/api/v1/orders")
        .header("X-API-Key", "test-api-key")
        .contentType(MediaType.APPLICATION_JSON)
        .content(/* ... */))
    .andDo(document("order-create",
        requestHeaders(  // ✅ 요청 헤더 문서화
            headerWithName("X-API-Key").description("API 키"),
            headerWithName("Content-Type").description("요청 Content Type")
        ),
        responseHeaders(  // ✅ 응답 헤더 문서화
            headerWithName("Location").description("생성된 리소스 URI")
        ),
        requestFields(/* ... */),
        responseFields(/* ... */)
    ));
```

---

## 5️⃣ AsciiDoc 문서 작성

### 디렉토리 구조

```
src/
└── docs/
    └── asciidoc/
        ├── index.adoc              # 메인 문서
        ├── order/
        │   ├── order-api.adoc      # Order API 전체
        │   ├── create-order.adoc   # 주문 생성
        │   └── get-order.adoc      # 주문 조회
        └── common/
            └── error-response.adoc # 공통 에러 응답
```

### index.adoc (메인 문서)

```asciidoc
= REST API 문서
개발팀;
:doctype: book
:icons: font
:source-highlighter: highlightjs
:toc: left
:toclevels: 3
:sectlinks:

[[overview]]
= 개요

본 문서는 REST API의 사용 방법을 설명합니다.

[[overview-http-verbs]]
== HTTP 메서드

본 API는 다음과 같은 HTTP 메서드를 사용합니다:

|===
| 메서드 | 용도

| `GET`
| 리소스 조회

| `POST`
| 리소스 생성

| `PATCH`
| 리소스 부분 수정

| `PUT`
| 리소스 전체 수정
|===

[[overview-http-status-codes]]
== HTTP 상태 코드

|===
| 상태 코드 | 의미

| `200 OK`
| 요청 성공

| `201 Created`
| 리소스 생성 성공

| `400 Bad Request`
| 잘못된 요청

| `404 Not Found`
| 리소스 없음

| `500 Internal Server Error`
| 서버 에러
|===

[[order-api]]
= Order API

include::order/order-api.adoc[]

[[common]]
= 공통

include::common/error-response.adoc[]
```

### order-api.adoc (Order API)

```asciidoc
[[order-create]]
== 주문 생성

`POST /api/v1/orders`

=== 요청

include::{snippets}/order-create/http-request.adoc[]

==== 요청 필드

include::{snippets}/order-create/request-fields.adoc[]

=== 응답

include::{snippets}/order-create/http-response.adoc[]

==== 응답 필드

include::{snippets}/order-create/response-fields.adoc[]

=== 예시

.curl 요청
include::{snippets}/order-create/curl-request.adoc[]

.HTTP 요청
include::{snippets}/order-create/http-request.adoc[]

.HTTP 응답
include::{snippets}/order-create/http-response.adoc[]

[[order-get]]
== 주문 조회

`GET /api/v1/orders/{id}`

=== 요청

include::{snippets}/order-get/http-request.adoc[]

==== Path Parameters

include::{snippets}/order-get/path-parameters.adoc[]

=== 응답

include::{snippets}/order-get/http-response.adoc[]

==== 응답 필드

include::{snippets}/order-get/response-fields.adoc[]
```

### error-response.adoc (공통 에러 응답)

```asciidoc
[[error-response]]
== 에러 응답

모든 에러 응답은 RFC 7807 Problem Details 형식을 따릅니다.

=== 구조

[source,json]
----
{
  "success": false,
  "data": null,
  "error": {
    "type": "https://api.example.com/errors/validation-failed",
    "title": "Validation Failed",
    "status": 400,
    "detail": "요청 데이터 검증에 실패했습니다.",
    "instance": "/api/v1/orders",
    "timestamp": "2025-11-13T12:00:00Z",
    "invalidParams": [
      {
        "field": "customerId",
        "message": "must not be null"
      }
    ]
  }
}
----

=== 필드 설명

|===
| 필드 | 타입 | 설명

| `success`
| Boolean
| 성공 여부 (에러 시 항상 false)

| `error.type`
| String
| 에러 타입 URI

| `error.title`
| String
| 에러 제목

| `error.status`
| Integer
| HTTP 상태 코드

| `error.detail`
| String
| 에러 상세 설명

| `error.invalidParams`
| Array
| Validation 실패 필드 목록 (선택)
|===
```

---

## 6️⃣ 생성된 Snippet 종류

### 자동 생성되는 Snippet

테스트 실행 후 `build/generated-snippets/{문서식별자}/` 디렉토리에 생성:

```
build/generated-snippets/order-create/
├── curl-request.adoc           # curl 명령어
├── http-request.adoc           # HTTP 요청
├── http-response.adoc          # HTTP 응답
├── httpie-request.adoc         # HTTPie 명령어
├── request-body.adoc           # 요청 Body
├── request-fields.adoc         # 요청 필드 테이블
├── response-body.adoc          # 응답 Body
└── response-fields.adoc        # 응답 필드 테이블
```

### Snippet 커스터마이징

```java
// Custom Snippet 템플릿 위치
src/test/resources/org/springframework/restdocs/templates/asciidoctor/

// 예: request-fields.snippet (커스텀 템플릿)
|===
|Path|Type|Description|Constraints

{{#fields}}
|{{path}}
|{{type}}
|{{description}}
|{{constraints}}

{{/fields}}
|===
```

---

## 7️⃣ HTML 문서 생성 및 서빙

### 문서 생성 명령어 (멀티모듈)

```bash
# 1. 테스트 실행 + Snippet 생성 (rest-api 모듈)
./gradlew :adapter-in:rest-api:test

# 2. AsciiDoc → HTML 변환 (bootstrap 모듈)
./gradlew :bootstrap:bootstrap-web-api:asciidoctor

# 3. 생성된 HTML 확인
open bootstrap/bootstrap-web-api/build/docs/asciidoc/index.html

# 4. JAR에 포함하여 패키징 (문서 자동 포함)
./gradlew :bootstrap:bootstrap-web-api:bootJar

# 5. 전체 빌드 (테스트 → 문서 → JAR)
./gradlew clean build
```

### 애플리케이션에서 서빙

```java
package com.ryuqq.adapter.in.rest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static Resource 설정
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // REST Docs HTML 서빙
        registry
            .addResourceHandler("/docs/**")  // ✅ /docs/** 경로로 접근
            .addResourceLocations("classpath:/static/docs/");  // ✅ JAR 내부 경로
    }
}
```

### 접근 URL

```
# 로컬 개발
http://localhost:8080/docs/index.html

# 프로덕션
https://api.example.com/docs/index.html
```

---

## 8️⃣ OpenAPI/Swagger 통합 (선택)

### restdocs-api-spec 플러그인 사용

```gradle
plugins {
    id 'com.epages.restdocs-api-spec' version '0.19.2'
}

dependencies {
    testImplementation 'com.epages.restdocs-api-spec:restdocs-api-spec-mockmvc:0.19.2'
}

openapi3 {
    server = 'https://api.example.com'
    title = 'API Documentation'
    description = 'REST API Documentation'
    version = '1.0.0'
    format = 'yaml'  // 또는 'json'
}
```

### 테스트에서 OpenAPI Snippet 생성

```java
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.*;

mockMvc.perform(post("/api/v1/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content(/* ... */))
    .andDo(document("order-create",
        resource(  // ✅ OpenAPI 리소스 정의
            ResourceSnippetParameters.builder()
                .tag("Order")
                .summary("주문 생성")
                .description("새로운 주문을 생성합니다")
                .requestFields(/* ... */)
                .responseFields(/* ... */)
                .build()
        )
    ));
```

### OpenAPI 문서 생성

```bash
# OpenAPI YAML 생성
./gradlew openapi3

# 생성 위치
build/api-spec/openapi3.yaml

# Swagger UI에서 사용
curl http://localhost:8080/swagger-ui.html
```

---

## 9️⃣ Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. 문서 식별자는 kebab-case
.andDo(document("order-create"))

// ✅ 2. 필드 설명은 명확하고 간결하게
fieldWithPath("customerId").description("고객 ID")

// ✅ 3. 선택 필드는 optional() 명시
fieldWithPath("error").optional().description("에러 정보")

// ✅ 4. 제약사항은 attributes로 추가
fieldWithPath("quantity").description("주문 수량")
    .attributes(key("constraints").value("1 이상"))

// ✅ 5. 모든 필드 문서화 (누락 시 테스트 실패)
requestFields(
    fieldWithPath("customerId").description("고객 ID"),
    fieldWithPath("items").description("상품 목록"),
    fieldWithPath("items[].productId").description("상품 ID"),
    fieldWithPath("items[].quantity").description("수량")
)

// ✅ 6. Pretty Print 적용
.apply(documentationConfiguration(restDocumentation)
    .operationPreprocessors()
    .withRequestDefaults(prettyPrint())
    .withResponseDefaults(prettyPrint()))

// ✅ 7. Base 클래스 상속으로 중복 제거
class OrderControllerDocsTest extends RestDocsTestSupport { }
```

### ❌ Anti-Patterns

```java
// ❌ 1. 문서 식별자가 불명확
.andDo(document("test1"))  // ❌ 의미 없는 이름

// ❌ 2. 필드 설명 부실
fieldWithPath("customerId").description("ID")  // ❌ 무슨 ID?

// ❌ 3. 필드 누락 (테스트 실패)
requestFields(
    fieldWithPath("customerId").description("고객 ID")
    // ❌ items 필드 누락
)

// ❌ 4. 하드코딩된 문서 (테스트 없이 작성)
// ❌ 테스트 없이 .adoc만 작성하면 동기화 불가

// ❌ 5. Swagger 어노테이션 혼용
@ApiOperation("주문 생성")  // ❌ REST Docs와 혼용 금지
public ResponseEntity<?> createOrder() { }

// ❌ 6. 문서 식별자 중복
.andDo(document("order"))  // ❌ 다른 테스트와 충돌
.andDo(document("order"))  // ❌ 덮어쓰기 발생

// ❌ 7. Pretty Print 누락 (가독성 저하)
// ❌ JSON이 한 줄로 출력되어 읽기 어려움
```

---

## 🔟 체크리스트

### 초기 설정 체크리스트 (멀티모듈)

- [ ] `gradle/libs.versions.toml`에 REST Docs 버전 및 플러그인 추가
- [ ] `adapter-in/rest-api/build.gradle`에 snippetsDir 및 테스트 의존성 설정
- [ ] `bootstrap/bootstrap-web-api/build.gradle`에 asciidoctor 플러그인 및 태스크 설정
- [ ] `RestDocsTestSupport` Base 클래스 작성 (rest-api 모듈)
- [ ] `bootstrap/bootstrap-web-api/src/docs/asciidoc/` 디렉토리 생성
- [ ] `index.adoc` 메인 문서 작성
- [ ] `static/docs/.gitkeep` 생성 (빌드된 HTML 위치)

### 테스트 작성 체크리스트

- [ ] `RestDocsTestSupport` 상속
- [ ] `@DisplayName`으로 명확한 의도 표현
- [ ] `.andDo(document("문서식별자"))` 추가
- [ ] `requestFields()` / `responseFields()` 모든 필드 문서화
- [ ] `pathParameters()` / `queryParameters()` 문서화 (필요 시)
- [ ] 선택 필드는 `.optional()` 명시
- [ ] 제약사항은 `.attributes()` 추가

### AsciiDoc 작성 체크리스트

- [ ] `index.adoc`에 API 섹션 추가
- [ ] `include::{snippets}/` 경로로 Snippet 포함
- [ ] HTTP 메서드, 상태 코드 설명
- [ ] 예시 (curl, HTTP 요청/응답) 포함
- [ ] 공통 에러 응답 문서화

### 빌드 및 배포 체크리스트 (멀티모듈)

- [ ] `./gradlew :adapter-in:rest-api:test` 실행 (Snippet 생성)
- [ ] `./gradlew :bootstrap:bootstrap-web-api:asciidoctor` 실행 (HTML 생성)
- [ ] `bootstrap/bootstrap-web-api/build/docs/asciidoc/index.html` 확인
- [ ] `./gradlew :bootstrap:bootstrap-web-api:bootJar` 실행 (JAR에 포함)
- [ ] Docker 마운트 불필요 확인 (JAR 내부에 문서 포함)
- [ ] `/docs/index.html` 접근 확인

---

## 1️⃣1️⃣ 추가 가이드 링크

- **[Controller Unit Test Guide](../controller/controller-unit-test-guide.md)** - @WebMvcTest 단위 테스트 가이드
- **[Controller Guide](controller-guide.md)** - Controller 작성 가이드
- **[Controller ArchUnit Guide](controller-archunit.md)** - 아키텍처 검증 가이드
- **[E2E Test Guide](../../../05-testing/e2e-testing/e2e-test-guide.md)** - 전체 시스템 E2E 테스트 가이드 (TBD)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.1.0
