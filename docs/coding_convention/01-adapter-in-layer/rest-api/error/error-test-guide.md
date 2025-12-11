# Error Handling Test Guide — **@WebMvcTest로 예외 처리 검증**

> **목적**: GlobalExceptionHandler와 ErrorMapper의 RFC 7807 준수 여부 검증
>
> **철학**: 표준 에러 응답, Content-Type 검증, 에러 코드 일관성

---

## 1️⃣ 핵심 원칙 (Core Principles)

### 에러 처리 테스트는 RFC 7807 준수를 검증합니다

**GlobalExceptionHandler는 모든 예외를 표준 형식으로 변환합니다.**

### 검증 범위

| 검증 항목 | 설명 | 방법 |
|----------|------|------|
| **Content-Type** | `application/problem+json` | `.andExpect(content().contentType())` |
| **x-error-code 헤더** | 에러 코드 헤더 포함 | `.andExpect(header())` |
| **ProblemDetail 필드** | type, title, status, detail, instance | JsonPath 검증 |
| **확장 필드** | code, timestamp, traceId | JsonPath 검증 |
| **HTTP Status** | 올바른 상태 코드 반환 | `.andExpect(status())` |
| **에러 코드 일관성** | 표준 에러 코드 사용 | 상수 검증 |

### 검증하지 않는 것

- ❌ UseCase 비즈니스 로직 → UseCase Test에서
- ❌ Domain 예외 발생 조건 → Domain Test에서
- ❌ Database 트랜잭션 롤백 → Integration Test에서

---

## 2️⃣ 테스트 구조 (Test Structure)

### GlobalExceptionHandler 테스트 클래스

```java
package com.ryuqq.adapter.in.rest.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.adapter.in.rest.common.error.ErrorMapperRegistry;
import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 단위 테스트
 *
 * <p>검증 범위:</p>
 * <ul>
 *   <li>RFC 7807 ProblemDetail 형식</li>
 *   <li>Content-Type: application/problem+json</li>
 *   <li>x-error-code 응답 헤더</li>
 *   <li>표준 에러 코드 일관성</li>
 *   <li>로깅 레벨 전략 (5xx, 404, 4xx)</li>
 * </ul>
 */
@WebMvcTest(controllers = TestController.class)  // 테스트용 Controller
@DisplayName("GlobalExceptionHandler 단위 테스트")
@Tag("unit")
@Tag("adapter-rest")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ErrorMapperRegistry errorMapperRegistry;

    // 테스트 메서드들...
}
```

### 테스트용 Controller (예외 발생용)

```java
package com.ryuqq.adapter.in.rest.common.controller;

import com.ryuqq.domain.common.exception.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * GlobalExceptionHandler 테스트를 위한 테스트 전용 Controller
 */
@RestController
@RequestMapping("/test")
@Validated
class TestController {

    @PostMapping("/validation")
    public void testValidation(@Valid @RequestBody TestRequest request) {
        // Validation 테스트용
    }

    @GetMapping("/illegal-argument")
    public void testIllegalArgument() {
        throw new IllegalArgumentException("잘못된 인자입니다");
    }

    @GetMapping("/illegal-state")
    public void testIllegalState() {
        throw new IllegalStateException("상태 충돌입니다");
    }

    @GetMapping("/domain-exception")
    public void testDomainException() {
        throw new TestDomainException("TEST_ERROR", "테스트 에러입니다");
    }

    @GetMapping("/runtime-exception")
    public void testRuntimeException() {
        throw new RuntimeException("예상치 못한 에러");
    }

    record TestRequest(
        @NotBlank(message = "이름은 필수입니다")
        String name
    ) {}

    static class TestDomainException extends DomainException {
        TestDomainException(String code, String message) {
            super(code, message);
        }
    }
}
```

---

## 3️⃣ Validation 에러 테스트 (400)

### MethodArgumentNotValidException 테스트

```java
@Nested
@DisplayName("400 - Validation 에러")
class ValidationExceptionTest {

    @Test
    @DisplayName("MethodArgumentNotValidException - RFC 7807 형식 검증")
    void handleValidationException_ReturnsRfc7807Format() throws Exception {
        // Given
        String invalidRequest = """
            {
                "name": ""
            }
            """;

        // When & Then
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            // HTTP Status
            .andExpect(status().isBadRequest())
            // Content-Type: application/problem+json
            .andExpect(content().contentType("application/problem+json"))
            // x-error-code 헤더
            .andExpect(header().string("x-error-code", "VALIDATION_FAILED"))
            // RFC 7807 필수 필드
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Validation failed for request"))
            .andExpect(jsonPath("$.instance").exists())
            // 확장 필드
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.timestamp").exists())
            // Validation 에러 상세
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.errors.name").value("이름은 필수입니다"));
    }

    @Test
    @DisplayName("다중 필드 Validation 에러 - errors 맵 검증")
    void handleValidationException_MultipleFieldErrors() throws Exception {
        // Given
        String invalidRequest = """
            {
                "name": "",
                "email": "invalid-email"
            }
            """;

        // When & Then
        mockMvc.perform(post("/test/validation-multiple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.email").exists());
    }
}
```

### ConstraintViolationException 테스트

```java
@Test
@DisplayName("ConstraintViolationException - 파라미터 Validation")
void handleConstraintViolation_ReturnsRfc7807Format() throws Exception {
    // When & Then
    mockMvc.perform(get("/test/constraint-violation")
            .param("id", "-1"))  // @Min(1) 위반
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(header().string("x-error-code", "CONSTRAINT_VIOLATION"))
        .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"))
        .andExpect(jsonPath("$.errors").exists());
}
```

---

## 4️⃣ 클라이언트 에러 테스트 (4xx)

### IllegalArgumentException 테스트

```java
@Nested
@DisplayName("400 - 클라이언트 에러")
class ClientErrorTest {

    @Test
    @DisplayName("IllegalArgumentException - INVALID_ARGUMENT 코드")
    void handleIllegalArgument_ReturnsInvalidArgumentCode() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/illegal-argument"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.detail").value("잘못된 인자입니다"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException - JSON 파싱 실패")
    void handleHttpMessageNotReadable_ReturnsInvalidFormatCode() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "INVALID_FORMAT"))
            .andExpect(jsonPath("$.code").value("INVALID_FORMAT"))
            // 보안: 상세 파서 에러 노출하지 않음
            .andExpect(jsonPath("$.detail").value("잘못된 요청 형식입니다. JSON 형식을 확인해주세요."));
    }

    @Test
    @DisplayName("TypeMismatchException - 타입 변환 실패")
    void handleTypeMismatch_ReturnsTypeMismatchCode() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/orders/{id}", "not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "TYPE_MISMATCH"))
            .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("MissingServletRequestParameterException - 필수 파라미터 누락")
    void handleMissingParam_ReturnsMissingParameterCode() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/search"))  // 필수 파라미터 없이 호출
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "MISSING_PARAMETER"))
            .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }
}
```

### IllegalStateException 테스트 (409 Conflict)

```java
@Test
@DisplayName("IllegalStateException - STATE_CONFLICT 코드 (409)")
void handleIllegalState_ReturnsConflictStatus() throws Exception {
    // When & Then
    mockMvc.perform(get("/test/illegal-state"))
        .andExpect(status().isConflict())  // 409
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(header().string("x-error-code", "STATE_CONFLICT"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
}
```

---

## 5️⃣ 리소스 에러 테스트 (404, 405)

### NoResourceFoundException 테스트

```java
@Nested
@DisplayName("404/405 - 리소스 에러")
class ResourceErrorTest {

    @Test
    @DisplayName("NoResourceFoundException - RESOURCE_NOT_FOUND 코드")
    void handleNoResource_ReturnsNotFoundStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/non-existent-path"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("MethodNotAllowedException - Allow 헤더 포함")
    void handleMethodNotAllowed_ReturnsAllowHeader() throws Exception {
        // When & Then
        mockMvc.perform(delete("/test/validation"))  // DELETE 지원하지 않음
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "METHOD_NOT_ALLOWED"))
            .andExpect(header().exists("Allow"))  // 지원 메서드 헤더
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
```

---

## 6️⃣ Domain Exception 테스트

### DomainException + ErrorMapper 테스트

```java
@Nested
@DisplayName("Domain Exception - ErrorMapper 통합")
class DomainExceptionTest {

    @Test
    @DisplayName("DomainException - ErrorMapper로 매핑")
    void handleDomainException_UsesErrorMapper() throws Exception {
        // Given
        ErrorMapping mapping = ErrorMapping.of(
            HttpStatus.NOT_FOUND,
            "Not Found",
            "주문을 찾을 수 없습니다"
        );
        given(errorMapperRegistry.map(any(DomainException.class), any(Locale.class)))
            .willReturn(Optional.of(mapping));

        // When & Then
        mockMvc.perform(get("/test/domain-exception"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "TEST_ERROR"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("TEST_ERROR"))
            .andExpect(jsonPath("$.detail").value("주문을 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("DomainException - ErrorMapper 없으면 기본 매핑")
    void handleDomainException_FallbackToDefaultMapping() throws Exception {
        // Given
        given(errorMapperRegistry.map(any(DomainException.class), any(Locale.class)))
            .willReturn(Optional.empty());
        given(errorMapperRegistry.defaultMapping(any(DomainException.class)))
            .willReturn(ErrorMapping.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "테스트 에러입니다"
            ));

        // When & Then
        mockMvc.perform(get("/test/domain-exception"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "TEST_ERROR"));
    }

    @Test
    @DisplayName("DomainException - args 확장 필드 포함")
    void handleDomainException_IncludesArgsProperty() throws Exception {
        // Given - args가 있는 DomainException
        // ...

        // When & Then
        mockMvc.perform(get("/test/domain-exception-with-args"))
            .andExpect(jsonPath("$.args").exists())
            .andExpect(jsonPath("$.args.orderId").value(12345));
    }
}
```

---

## 7️⃣ 서버 에러 테스트 (500)

### 예상치 못한 예외 테스트

```java
@Nested
@DisplayName("500 - 서버 에러")
class ServerErrorTest {

    @Test
    @DisplayName("RuntimeException - INTERNAL_ERROR 코드")
    void handleRuntimeException_ReturnsInternalErrorCode() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/runtime-exception"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(header().string("x-error-code", "INTERNAL_ERROR"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            // 보안: 상세 에러 메시지 노출하지 않음
            .andExpect(jsonPath("$.detail").value("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    @DisplayName("500 에러 - 스택트레이스 노출하지 않음")
    void handleServerError_DoesNotExposeStackTrace() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/runtime-exception"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
```

---

## 8️⃣ ErrorMapper 단위 테스트

### ErrorMapper 구현체 테스트

```java
package com.ryuqq.adapter.in.rest.order.error;

import com.ryuqq.domain.order.exception.OrderNotFoundException;
import com.ryuqq.domain.order.exception.InvalidOrderStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderApiErrorMapper 단위 테스트
 */
@DisplayName("OrderApiErrorMapper 단위 테스트")
class OrderApiErrorMapperTest {

    private final OrderApiErrorMapper mapper = new OrderApiErrorMapper();

    @Test
    @DisplayName("ORDER_ prefix 예외 지원")
    void supports_OrderPrefixException_ReturnsTrue() {
        // Given
        var exception = new OrderNotFoundException(1L);

        // When & Then
        assertThat(mapper.supports(exception)).isTrue();
    }

    @Test
    @DisplayName("ORDER_ prefix 아닌 예외 미지원")
    void supports_NonOrderException_ReturnsFalse() {
        // Given
        var exception = new UserNotFoundException(1L);

        // When & Then
        assertThat(mapper.supports(exception)).isFalse();
    }

    @Test
    @DisplayName("ORDER_NOT_FOUND - 404 매핑")
    void map_OrderNotFound_Returns404() {
        // Given
        var exception = new OrderNotFoundException(1L);

        // When
        var result = mapper.map(exception, Locale.KOREA);

        // Then
        assertThat(result.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.title()).isEqualTo("Not Found");
        assertThat(result.detail()).contains("주문");
    }

    @Test
    @DisplayName("ORDER_INVALID_STATUS - 409 매핑")
    void map_InvalidOrderStatus_Returns409() {
        // Given
        var exception = new InvalidOrderStatusException("DELIVERED", "PLACED");

        // When
        var result = mapper.map(exception, Locale.KOREA);

        // Then
        assertThat(result.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.title()).isEqualTo("Conflict");
    }
}
```

### ErrorMapperRegistry 테스트

```java
package com.ryuqq.adapter.in.rest.common.error;

import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ErrorMapperRegistry 단위 테스트
 */
@DisplayName("ErrorMapperRegistry 단위 테스트")
class ErrorMapperRegistryTest {

    @Test
    @DisplayName("지원하는 ErrorMapper 찾아서 매핑")
    void map_FindsSupportingMapper_ReturnsMappedResult() {
        // Given
        ErrorMapper orderMapper = mock(ErrorMapper.class);
        ErrorMapper userMapper = mock(ErrorMapper.class);

        var exception = new TestDomainException("ORDER_NOT_FOUND", "주문 없음");
        var expectedMapping = ErrorMapping.of(HttpStatus.NOT_FOUND, "Not Found", "주문 없음");

        given(orderMapper.supports(exception)).willReturn(true);
        given(orderMapper.map(exception, Locale.KOREA)).willReturn(expectedMapping);

        var registry = new ErrorMapperRegistry(List.of(orderMapper, userMapper));

        // When
        var result = registry.map(exception, Locale.KOREA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("지원하는 ErrorMapper 없으면 Optional.empty()")
    void map_NoSupportingMapper_ReturnsEmpty() {
        // Given
        ErrorMapper orderMapper = mock(ErrorMapper.class);
        var exception = new TestDomainException("USER_NOT_FOUND", "사용자 없음");

        given(orderMapper.supports(exception)).willReturn(false);

        var registry = new ErrorMapperRegistry(List.of(orderMapper));

        // When
        var result = registry.map(exception, Locale.KOREA);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("기본 매핑 - 500 반환")
    void defaultMapping_ReturnsInternalServerError() {
        // Given
        var exception = new TestDomainException("UNKNOWN_ERROR", "알 수 없는 에러");
        var registry = new ErrorMapperRegistry(List.of());

        // When
        var result = registry.defaultMapping(exception);

        // Then
        assertThat(result.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static class TestDomainException extends DomainException {
        TestDomainException(String code, String message) {
            super(code, message);
        }
    }
}
```

---

## 9️⃣ RFC 7807 필드 검증 테스트

### 모든 필수/확장 필드 검증

```java
@Nested
@DisplayName("RFC 7807 필드 검증")
class Rfc7807FieldsTest {

    @Test
    @DisplayName("RFC 7807 필수 필드 - type, title, status, detail, instance")
    void verifyRequiredFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/illegal-argument"))
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").exists())
            .andExpect(jsonPath("$.status").isNumber())
            .andExpect(jsonPath("$.detail").exists())
            .andExpect(jsonPath("$.instance").exists());
    }

    @Test
    @DisplayName("확장 필드 - code, timestamp, traceId")
    void verifyExtensionFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/illegal-argument"))
            .andExpect(jsonPath("$.code").exists())
            .andExpect(jsonPath("$.timestamp").exists());
        // traceId는 MDC 설정 시에만 포함
    }

    @Test
    @DisplayName("instance 필드 - 요청 URI 포함")
    void verifyInstanceField_ContainsRequestUri() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/illegal-argument")
                .param("foo", "bar"))
            .andExpect(jsonPath("$.instance").value("/test/illegal-argument?foo=bar"));
    }
}
```

---

## 🔟 Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. Content-Type 검증
.andExpect(content().contentType("application/problem+json"))

// ✅ 2. x-error-code 헤더 검증
.andExpect(header().string("x-error-code", "VALIDATION_FAILED"))

// ✅ 3. RFC 7807 필드 검증
.andExpect(jsonPath("$.type").exists())
.andExpect(jsonPath("$.title").value("Bad Request"))
.andExpect(jsonPath("$.status").value(400))
.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))

// ✅ 4. 테스트용 Controller 분리
@WebMvcTest(controllers = TestController.class)

// ✅ 5. Nested 클래스로 에러 유형별 그룹화
@Nested
@DisplayName("400 - Validation 에러")
class ValidationExceptionTest { }

// ✅ 6. 보안 검증 (스택트레이스 미노출)
.andExpect(jsonPath("$.stackTrace").doesNotExist())
```

### ❌ Anti-Patterns

```java
// ❌ 1. Content-Type 검증 누락
mockMvc.perform(...).andExpect(status().isBadRequest());
// → application/problem+json 검증 필요

// ❌ 2. 일반 JSON Content-Type 기대
.andExpect(content().contentType(MediaType.APPLICATION_JSON))
// → application/problem+json이어야 함

// ❌ 3. 에러 코드 검증 누락
.andExpect(jsonPath("$.error").exists())
// → $.code 또는 x-error-code 헤더 검증 필요

// ❌ 4. 실제 Controller로 테스트
@WebMvcTest(OrderCommandController.class)
// → 테스트용 Controller 분리 권장

// ❌ 5. 에러 메시지 하드코딩 검증
.andExpect(jsonPath("$.detail").value("정확한 에러 메시지"))
// → 메시지 변경에 취약
```

---

## 1️⃣1️⃣ 체크리스트

### GlobalExceptionHandler 테스트

- [ ] `application/problem+json` Content-Type 검증
- [ ] `x-error-code` 응답 헤더 검증
- [ ] RFC 7807 필수 필드 검증 (type, title, status, detail, instance)
- [ ] 확장 필드 검증 (code, timestamp)
- [ ] Validation 에러 `errors` 맵 검증
- [ ] 보안: 스택트레이스 미노출 검증
- [ ] 보안: 상세 파서 에러 미노출 검증

### ErrorMapper 테스트

- [ ] `supports()` 메서드 테스트 (prefix 매칭)
- [ ] `map()` 메서드 테스트 (HTTP 상태, 메시지)
- [ ] ErrorMapperRegistry 통합 테스트

### HTTP 상태별 테스트

- [ ] 400 Bad Request (Validation, IllegalArgument, TypeMismatch)
- [ ] 404 Not Found (NoResourceFound, DomainException)
- [ ] 405 Method Not Allowed (Allow 헤더 포함)
- [ ] 409 Conflict (IllegalState)
- [ ] 500 Internal Server Error (RuntimeException)

---

## 1️⃣2️⃣ 테스트 프로젝트 구조

```
adapter-in/rest-api/
└── src/
    └── test/java/
        └── com/ryuqq/adapter/in/rest/
            ├── common/
            │   ├── controller/
            │   │   ├── GlobalExceptionHandlerTest.java  # ← 전역 예외 테스트
            │   │   └── TestController.java              # ← 테스트용 Controller
            │   └── error/
            │       └── ErrorMapperRegistryTest.java     # ← Registry 테스트
            │
            └── order/
                └── error/
                    └── OrderApiErrorMapperTest.java     # ← BC별 ErrorMapper 테스트
```

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
