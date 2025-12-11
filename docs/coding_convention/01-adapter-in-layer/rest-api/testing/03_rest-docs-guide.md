# Spring REST Docs 가이드

> **목적**: API 문서 자동화를 위한 Spring REST Docs 적용 규칙

---

## 0. 문서 위치 및 접근 경로

### 디렉토리 구조

```
프로젝트 루트/
├── adapter-in/
│   └── rest-api/
│       ├── src/
│       │   ├── docs/
│       │   │   └── asciidoc/                    ← 📝 AsciiDoc 소스
│       │   │       ├── index.adoc               ← 메인 문서
│       │   │       └── v2/                      ← API 버전별 디렉토리
│       │   │           ├── auth/
│       │   │           │   └── auth.adoc        ← Auth BC 문서
│       │   │           ├── member/
│       │   │           │   └── member.adoc      ← Member BC 문서
│       │   │           └── {bc}/                ← 새 BC 추가 시
│       │   │               └── {bc}.adoc
│       │   └── main/java/.../common/controller/
│       │       └── ApiDocsController.java       ← 🌐 문서 서빙 컨트롤러
│       └── build/
│           ├── generated-snippets/              ← 🔧 테스트 생성 스니펫
│           └── docs/asciidoc/                   ← 📄 빌드된 HTML
│
└── bootstrap/
    └── bootstrap-web-api/
        ├── build.gradle                         ← copyRestDocs 태스크
        └── build/resources/main/static/docs/    ← 📦 JAR 포함 문서
```

### 접근 경로

| 경로 | 설명 |
|------|------|
| `/docs` | API 문서 메인 페이지 |
| `/docs/v2/auth/auth.html` | Auth API 문서 |
| `/docs/v2/member/member.html` | Member API 문서 |
| `/docs/v2/{bc}/{bc}.html` | BC별 API 문서 |

### 빌드 명령어

```bash
# REST Docs 생성 (테스트 실행 + HTML 변환)
./gradlew :adapter-in:rest-api:asciidoctor

# JAR에 문서 포함하여 빌드
./gradlew :bootstrap:bootstrap-web-api:bootJar

# 문서 확인 (로컬)
open adapter-in/rest-api/build/docs/asciidoc/index.html
```

---

## 1. 개요

### Spring REST Docs vs Swagger/OpenAPI

| 항목 | Spring REST Docs | Swagger/OpenAPI |
|------|------------------|-----------------|
| **문서 생성** | 테스트 기반 | 어노테이션 기반 |
| **신뢰성** | ✅ 높음 (테스트 통과 필수) | 🔶 중간 (코드와 불일치 가능) |
| **실행 시점** | 빌드 시 생성 | 런타임 생성 |
| **출력 형식** | AsciiDoc → HTML/PDF | JSON/YAML → HTML |
| **유지보수** | 테스트 = 문서 | 별도 관리 필요 |

### 왜 Spring REST Docs인가?

```
┌─────────────────────────────────────────────────────────────┐
│  Spring REST Docs 선택 이유                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 테스트 강제: 테스트 통과해야 문서 생성                     │
│     → 문서와 실제 API 100% 일치 보장                         │
│                                                             │
│  2. 코드 분리: 프로덕션 코드에 문서 어노테이션 없음             │
│     → 깔끔한 코드 유지                                       │
│                                                             │
│  3. 커스터마이징: AsciiDoc 기반 자유로운 문서 구성             │
│     → 기업 스타일 가이드 적용 가능                            │
│                                                             │
│  ⚠️ 단, OpenAPI 어노테이션도 함께 사용 (Swagger UI용)         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 설정

### 2.1 rest-api 모듈 (adapter-in/rest-api/build.gradle)

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
}

// REST Docs Configuration
ext {
    snippetsDir = file('build/generated-snippets')
}

configurations {
    asciidoctorExt
}

dependencies {
    asciidoctorExt 'org.springframework.restdocs:spring-restdocs-asciidoctor:3.0.1'

    // Spring REST Docs
    testImplementation libs.spring.restdocs.mockmvc
}

// 테스트 실행 시 스니펫 생성
tasks.test {
    outputs.dir snippetsDir
}

// AsciiDoc → HTML 변환
asciidoctor {
    inputs.dir snippetsDir
    configurations 'asciidoctorExt'
    dependsOn test

    baseDirFollowsSourceFile()

    attributes(
        'snippets': snippetsDir,
        'source-highlighter': 'highlightjs',
        'toc': 'left',
        'toclevels': 3,
        'sectlinks': true,
        'sectnums': true
    )
}
```

### 2.2 bootstrap 모듈 (bootstrap/bootstrap-web-api/build.gradle)

```gradle
// REST Docs Configuration
// rest-api 모듈에서 생성된 문서를 static 리소스로 복사
tasks.register('copyRestDocs', Copy) {
    dependsOn ':adapter-in:rest-api:asciidoctor'
    from "${project(':adapter-in:rest-api').buildDir}/docs/asciidoc"
    into "${sourceSets.main.output.resourcesDir}/static/docs"
}

tasks.processResources {
    dependsOn copyRestDocs
}
```

### 2.3 libs.versions.toml

```toml
[versions]
restdocs = "3.0.1"
asciidoctor = "3.3.2"

[libraries]
spring-restdocs-mockmvc = { module = "org.springframework.restdocs:spring-restdocs-mockmvc", version.ref = "restdocs" }
```

---

## 3. 테스트 작성

### 3.1 REST Docs 테스트 기본 구조

> **중요**: REST Docs는 MockMvc 기반이지만, **통합 테스트와 별도로** 문서화 전용 테스트를 작성합니다.

```java
package com.ryuqq.setof.adapter.in.rest.auth.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Auth API 문서화 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(RestDocumentationExtension.class)
@DisplayName("Auth API 문서화")
class AuthControllerDocsTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(documentationConfiguration(restDocumentation)
                .uris()
                    .withScheme("https")
                    .withHost("api.setof.com")
                    .withPort(443))
            .build();
    }

    @Nested
    @DisplayName("로그인 API")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공")
        void loginSuccess() throws Exception {
            String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123!"
                }
                """;

            mockMvc.perform(post("/api/v2/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andDo(document("auth-login",
                    requestFields(
                        fieldWithPath("email").description("이메일"),
                        fieldWithPath("password").description("비밀번호")
                    ),
                    responseFields(
                        fieldWithPath("result").description("결과 상태"),
                        fieldWithPath("data.memberId").description("회원 ID"),
                        fieldWithPath("data.email").description("이메일"),
                        fieldWithPath("message").description("메시지").optional()
                    )
                ));
        }
    }
}
```

### 3.2 테스트 파일 위치

```
adapter-in/rest-api/src/test/java/com/ryuqq/setof/adapter/in/rest/
├── auth/
│   └── controller/
│       └── AuthControllerDocsTest.java      # Auth API 문서화 테스트
├── member/
│   └── controller/
│       └── MemberControllerDocsTest.java    # Member API 문서화 테스트
└── {bc}/
    └── controller/
        └── {Bc}ControllerDocsTest.java      # 새 BC 문서화 테스트
```

### 3.3 테스트 파일 네이밍 규칙

| 패턴 | 용도 |
|------|------|
| `*ControllerDocsTest.java` | REST Docs 문서화 테스트 |
| `*ControllerTest.java` | Controller 단위 테스트 |

---

## 4. 문서화 스니펫

### 4.1 Path Parameters

```java
.andDo(document("order-get",
    pathParameters(
        parameterWithName("orderId").description("주문 ID")
    )
));
```

### 4.2 Query Parameters

```java
.andDo(document("orders-list",
    queryParameters(
        parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
        parameterWithName("size").description("페이지 크기 (기본값: 20)").optional(),
        parameterWithName("status").description("주문 상태 필터").optional()
    )
));
```

### 4.3 Request Body

```java
.andDo(document("order-create",
    requestFields(
        fieldWithPath("customerId").description("고객 ID"),
        fieldWithPath("productId").description("상품 ID"),
        fieldWithPath("quantity").description("주문 수량 (1~1000)")
    )
));
```

### 4.4 Response Body

```java
.andDo(document("order-create",
    responseFields(
        fieldWithPath("result").description("결과 상태"),
        fieldWithPath("data").description("응답 데이터"),
        fieldWithPath("data.orderId").description("생성된 주문 ID"),
        fieldWithPath("data.status").description("주문 상태"),
        fieldWithPath("message").description("메시지").optional()
    )
));
```

### 4.5 Request Headers

```java
.andDo(document("order-create",
    requestHeaders(
        headerWithName("Authorization").description("Bearer JWT 토큰"),
        headerWithName("Content-Type").description("application/json")
    )
));
```

---

## 5. AsciiDoc 문서 구성

### 5.1 디렉토리 구조

```
adapter-in/rest-api/src/docs/asciidoc/
├── index.adoc              # 메인 문서
└── v2/                     # API V2 문서
    ├── auth/
    │   └── auth.adoc       # Auth API
    ├── member/
    │   └── member.adoc     # Member API
    └── {bc}/               # 새 BC 추가 시
        └── {bc}.adoc
```

### 5.2 index.adoc (메인 문서)

```asciidoc
= SetOf Commerce API Documentation
:doctype: book
:icons: font
:source-highlighter: highlightjs
:toc: left
:toclevels: 3
:sectlinks:
:sectnums:

[[overview]]
== Overview

SetOf Commerce REST API 문서입니다.

=== Base URL

[cols="1,3"]
|===
| Environment | URL

| Development
| `http://localhost:8080`

| Production
| `https://api.setof.com`
|===

[[api-v2]]
== API V2

include::v2/auth/auth.adoc[]

include::v2/member/member.adoc[]
```

### 5.3 BC별 문서 템플릿 (v2/{bc}/{bc}.adoc)

```asciidoc
[[{bc}]]
=== {BC 한글명} ({BC})

{BC 설명}

[[{bc}-{action}]]
==== {API 이름}

{API 설명}

===== HTTP Request

include::{snippets}/{bc}-{action}/http-request.adoc[]

===== Request Fields

include::{snippets}/{bc}-{action}/request-fields.adoc[]

===== HTTP Response

include::{snippets}/{bc}-{action}/http-response.adoc[]

===== Response Fields

include::{snippets}/{bc}-{action}/response-fields.adoc[]

===== Example

include::{snippets}/{bc}-{action}/curl-request.adoc[]
```

---

## 6. 새 BC 추가 가이드

### Step 1: DocsTest 작성

```java
// adapter-in/rest-api/src/test/java/.../order/controller/OrderControllerDocsTest.java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(RestDocumentationExtension.class)
@DisplayName("Order API 문서화")
class OrderControllerDocsTest {
    // ... 테스트 작성
}
```

### Step 2: AsciiDoc 파일 생성

```bash
mkdir -p adapter-in/rest-api/src/docs/asciidoc/v2/order
```

```asciidoc
// v2/order/order.adoc
[[order]]
=== 주문 (Order)

주문 관련 API입니다.

[[order-create]]
==== 주문 생성

include::{snippets}/order-create/http-request.adoc[]
// ...
```

### Step 3: index.adoc에 include 추가

```asciidoc
[[api-v2]]
== API V2

include::v2/auth/auth.adoc[]
include::v2/member/member.adoc[]
include::v2/order/order.adoc[]    // ← 추가
```

### Step 4: 빌드 및 확인

```bash
./gradlew :bootstrap:bootstrap-web-api:bootJar
open adapter-in/rest-api/build/docs/asciidoc/index.html
```

---

## 7. 빌드 및 확인

### 7.1 문서 생성

```bash
# 테스트 실행 + 문서 생성
./gradlew :adapter-in:rest-api:asciidoctor

# 결과 확인
open adapter-in/rest-api/build/docs/asciidoc/index.html
```

### 7.2 JAR에 포함하여 빌드

```bash
# bootJar 빌드 (REST Docs 자동 포함)
./gradlew :bootstrap:bootstrap-web-api:bootJar

# JAR 내 문서 확인
jar tf bootstrap/bootstrap-web-api/build/libs/setof-commerce-web-api.jar | grep static/docs
```

### 7.3 CI/CD 통합

```yaml
# GitHub Actions 예시
- name: Build with REST Docs
  run: ./gradlew :bootstrap:bootstrap-web-api:bootJar

- name: Upload API Docs
  uses: actions/upload-artifact@v3
  with:
    name: api-docs
    path: adapter-in/rest-api/build/docs/asciidoc/
```

---

## 8. OpenAPI 어노테이션과 함께 사용

### 8.1 병행 사용 전략

```java
/**
 * 주문 생성 API
 *
 * @apiNote REST Docs로 상세 문서 생성, OpenAPI로 Swagger UI 제공
 */
@Operation(
    summary = "주문 생성",
    description = "새로운 주문을 생성합니다"
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "주문 생성 성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
})
@PostMapping("/orders")
public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
    @Valid @RequestBody PlaceOrderApiRequest request
) {
    // ...
}
```

### 8.2 역할 분담

| 도구 | 용도 | 접근 경로 |
|------|------|----------|
| **REST Docs** | 정적 HTML 문서 (배포용, 인쇄용) | `/docs` |
| **OpenAPI/Swagger** | 대화형 API 탐색기 (개발자용) | `/swagger-ui/index.html` |

---

## 9. 체크리스트

### 새 BC 추가 시

- [ ] `*ControllerDocsTest.java` 작성
- [ ] `src/docs/asciidoc/v2/{bc}/{bc}.adoc` 생성
- [ ] `index.adoc`에 `include::v2/{bc}/{bc}.adoc[]` 추가
- [ ] `./gradlew :adapter-in:rest-api:asciidoctor` 정상 실행
- [ ] 생성된 HTML 확인

### API 추가 시

- [ ] DocsTest에 새 테스트 메서드 추가
- [ ] `document("{bc}-{action}", ...)` 호출
- [ ] BC의 `.adoc` 파일에 include 추가
- [ ] 빌드 후 문서 확인

---

## 10. 참고 문서

- [Spring REST Docs 공식 문서](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
- [REST API 통합 테스트 가이드](./01_rest-api-testing-guide.md)
- [OpenAPI 가이드](../openapi/openapi-guide.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-10
**버전**: 1.1.0
