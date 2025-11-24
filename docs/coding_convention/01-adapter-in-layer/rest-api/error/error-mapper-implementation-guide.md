# ErrorMapper Implementation Guide — **ErrorMapper 구현 가이드**

> **목적**: Bounded Context별 ErrorMapper 구체 구현 가이드
>
> **철학**: OCP 준수, MessageSource I18N, RFC 7807 표준

---

## 1️⃣ ErrorMapper 인터페이스 (Interface Contract)

```java
package com.ryuqq.adapter.in.rest.common.mapper;

import com.ryuqq.domain.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Locale;

/**
 * Domain Exception → HTTP Problem Details 변환 인터페이스
 *
 * <p>각 Bounded Context는 이 인터페이스를 구현하여
 * Domain Exception을 RFC 7807 형식으로 변환합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface ErrorMapper {

    /**
     * 이 Mapper가 처리 가능한 에러 코드인지 판단
     *
     * @param code Domain Exception의 에러 코드
     * @return 처리 가능하면 true
     */
    boolean supports(String code);

    /**
     * Domain Exception → HTTP Problem Details 변환
     *
     * @param ex Domain Exception
     * @param locale 클라이언트 언어 (I18N)
     * @return HTTP 상태, 제목, 상세, Type URI
     */
    MappedError map(DomainException ex, Locale locale);

    /**
     * HTTP Problem Details 매핑 결과
     *
     * @param status HTTP 상태 코드
     * @param title  에러 제목 (I18N)
     * @param detail 에러 상세 (I18N)
     * @param type   RFC 7807 Type URI
     */
    record MappedError(
        HttpStatus status,
        String title,
        String detail,
        URI type
    ) {}
}
```

---

## 2️⃣ ErrorMapper 구현 패턴 (Implementation Pattern)

### 기본 템플릿 (Basic Template)

```java
package com.ryuqq.adapter.in.rest.order.error;

import com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper;
import com.ryuqq.domain.common.exception.DomainException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * Order Bounded Context ErrorMapper
 *
 * <p>ORDER_ 접두사를 가진 Domain Exception을 HTTP Problem Details로 변환합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class OrderErrorMapper implements ErrorMapper {

    // ✅ 1. PREFIX 정의 (Bounded Context 식별자)
    private static final String PREFIX = "ORDER_";

    // ✅ 2. RFC 7807 Type URI Base
    private static final String TYPE_BASE = "https://api.example.com/problems/order/";

    // ✅ 3. MessageSource DI (I18N 지원)
    private final MessageSource messageSource;

    public OrderErrorMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean supports(String code) {
        // ✅ PREFIX 기반 지원 여부 판단
        return code != null && code.startsWith(PREFIX);
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        String code = ex.code();

        // ✅ 4. HttpStatus 매핑 (Switch Expression)
        HttpStatus status = mapHttpStatus(code);

        // ✅ 5. I18N 메시지 조회 (MessageSource)
        String title = resolveTitle(code, status, locale);
        String detail = resolveDetail(code, ex, locale);

        // ✅ 6. RFC 7807 Type URI 생성
        URI type = createTypeUri(code);

        return new MappedError(status, title, detail, type);
    }

    /**
     * 에러 코드 → HTTP 상태 코드 매핑
     */
    private HttpStatus mapHttpStatus(String code) {
        return switch (code) {
            // 404: 리소스 없음
            case "ORDER_NOT_FOUND" -> HttpStatus.NOT_FOUND;

            // 409: 충돌 (중복, 상태 오류)
            case "ORDER_DUPLICATE_KEY" -> HttpStatus.CONFLICT;
            case "ORDER_INVALID_STATE" -> HttpStatus.CONFLICT;
            case "ORDER_ALREADY_CANCELLED" -> HttpStatus.CONFLICT;

            // 400: 잘못된 요청 (기본값)
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * I18N 제목 조회 (MessageSource)
     */
    private String resolveTitle(String code, HttpStatus status, Locale locale) {
        String key = "problem.title." + code.toLowerCase();
        return messageSource.getMessage(
            key,
            new Object[0],
            status.getReasonPhrase(),  // ✅ Fallback: HTTP 상태 문구
            locale
        );
    }

    /**
     * I18N 상세 메시지 조회 (MessageSource)
     */
    private String resolveDetail(String code, DomainException ex, Locale locale) {
        String key = "problem.detail." + code.toLowerCase();
        return messageSource.getMessage(
            key,
            ex.args().toArray(),  // ✅ 파라미터 바인딩 (args)
            ex.getMessage(),      // ✅ Fallback: Exception 메시지
            locale
        );
    }

    /**
     * RFC 7807 Type URI 생성
     */
    private URI createTypeUri(String code) {
        // ✅ 소문자, 하이픈 구분 (order-not-found)
        String path = code.toLowerCase().replace('_', '-');
        return URI.create(TYPE_BASE + path);
    }
}
```

---

## 3️⃣ supports() 메서드 패턴 (Prefix-Based Selection)

### PREFIX 기반 선택 전략

```java
// ✅ Good: PREFIX 상수로 관리
private static final String PREFIX = "ORDER_";

@Override
public boolean supports(String code) {
    return code != null && code.startsWith(PREFIX);
}
```

### 복수 PREFIX 지원 (Multi-Prefix)

```java
// ✅ 여러 PREFIX 지원 (드물지만 가능)
private static final Set<String> PREFIXES = Set.of("ORDER_", "INVOICE_");

@Override
public boolean supports(String code) {
    if (code == null) return false;
    return PREFIXES.stream().anyMatch(code::startsWith);
}
```

### ❌ Bad: 하드코딩된 코드 매칭

```java
// ❌ Bad: 확장성 없음 (새 코드 추가마다 수정 필요)
@Override
public boolean supports(String code) {
    return "ORDER_NOT_FOUND".equals(code)
        || "ORDER_INVALID_STATE".equals(code)
        || "ORDER_DUPLICATE_KEY".equals(code);
}
```

---

## 4️⃣ HttpStatus 매핑 전략 (Status Mapping Strategy)

### Switch Expression 패턴 (권장)

```java
private HttpStatus mapHttpStatus(String code) {
    return switch (code) {
        // 404 - 리소스 없음
        case "ORDER_NOT_FOUND"    -> HttpStatus.NOT_FOUND;
        case "CUSTOMER_NOT_FOUND" -> HttpStatus.NOT_FOUND;

        // 409 - 충돌 (중복, 상태 오류)
        case "ORDER_DUPLICATE_KEY"      -> HttpStatus.CONFLICT;
        case "ORDER_INVALID_STATE"      -> HttpStatus.CONFLICT;
        case "ORDER_ALREADY_CANCELLED"  -> HttpStatus.CONFLICT;

        // 403 - 권한 없음
        case "ORDER_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;

        // 422 - 처리 불가 (비즈니스 규칙 위반)
        case "ORDER_AMOUNT_TOO_LOW"  -> HttpStatus.UNPROCESSABLE_ENTITY;
        case "ORDER_QUANTITY_EXCEED" -> HttpStatus.UNPROCESSABLE_ENTITY;

        // 400 - 잘못된 요청 (기본값)
        default -> HttpStatus.BAD_REQUEST;
    };
}
```

### 카테고리별 매핑 (Category-Based Mapping)

```java
private HttpStatus mapHttpStatus(String code) {
    // ✅ 접미사 기반 카테고리 매핑
    if (code.endsWith("_NOT_FOUND")) {
        return HttpStatus.NOT_FOUND;
    }
    if (code.endsWith("_DUPLICATE") || code.endsWith("_DUPLICATE_KEY")) {
        return HttpStatus.CONFLICT;
    }
    if (code.endsWith("_ACCESS_DENIED")) {
        return HttpStatus.FORBIDDEN;
    }

    // ✅ 개별 코드 매핑
    return switch (code) {
        case "ORDER_INVALID_STATE" -> HttpStatus.CONFLICT;
        case "ORDER_AMOUNT_TOO_LOW" -> HttpStatus.UNPROCESSABLE_ENTITY;
        default -> HttpStatus.BAD_REQUEST;
    };
}
```

### 주요 HTTP 상태 코드 가이드

| HTTP Status | 용도 | 예시 |
|-------------|------|------|
| **400 BAD_REQUEST** | 잘못된 요청 (기본값) | 유효하지 않은 파라미터 |
| **403 FORBIDDEN** | 권한 없음 | 접근 권한 없는 리소스 |
| **404 NOT_FOUND** | 리소스 없음 | 존재하지 않는 주문 |
| **409 CONFLICT** | 충돌 (중복, 상태 오류) | 중복 키, 잘못된 상태 전이 |
| **422 UNPROCESSABLE_ENTITY** | 처리 불가 (비즈니스 규칙) | 최소 주문 금액 미달 |
| **500 INTERNAL_SERVER_ERROR** | 서버 오류 (사용 금지!) | ❌ ErrorMapper에서 매핑 금지 |

**⚠️ 중요**: `500 INTERNAL_SERVER_ERROR`는 ErrorMapper에서 매핑하지 **않습니다**. 시스템 예외는 GlobalExceptionHandler의 별도 핸들러에서 처리합니다.

---

## 5️⃣ MessageSource 통합 (I18N Integration)

### messages.properties 구조

#### messages_ko.properties (한국어)

```properties
# Order ErrorMapper - Title
problem.title.order_not_found=주문을 찾을 수 없음
problem.title.order_invalid_state=잘못된 주문 상태
problem.title.order_duplicate_key=중복된 주문

# Order ErrorMapper - Detail
problem.detail.order_not_found=주문 ID {0}을(를) 찾을 수 없습니다.
problem.detail.order_invalid_state=주문 상태가 {0}에서 {1}(으)로 변경할 수 없습니다.
problem.detail.order_duplicate_key=주문 번호 {0}이(가) 이미 존재합니다.
```

#### messages_en.properties (영어)

```properties
# Order ErrorMapper - Title
problem.title.order_not_found=Order Not Found
problem.title.order_invalid_state=Invalid Order State
problem.title.order_duplicate_key=Duplicate Order

# Order ErrorMapper - Detail
problem.detail.order_not_found=Order ID {0} not found.
problem.detail.order_invalid_state=Cannot change order state from {0} to {1}.
problem.detail.order_duplicate_key=Order number {0} already exists.
```

### 메시지 조회 패턴

```java
/**
 * I18N 제목 조회
 */
private String resolveTitle(String code, HttpStatus status, Locale locale) {
    String key = "problem.title." + code.toLowerCase();  // ✅ 소문자 변환
    return messageSource.getMessage(
        key,
        new Object[0],                // ✅ 제목은 파라미터 없음
        status.getReasonPhrase(),     // ✅ Fallback: HTTP 상태 문구
        locale
    );
}

/**
 * I18N 상세 메시지 조회
 */
private String resolveDetail(String code, DomainException ex, Locale locale) {
    String key = "problem.detail." + code.toLowerCase();  // ✅ 소문자 변환
    return messageSource.getMessage(
        key,
        ex.args().toArray(),          // ✅ 파라미터 바인딩 (args)
        ex.getMessage(),              // ✅ Fallback: Exception 메시지
        locale
    );
}
```

### 파라미터 바인딩 예시

```java
// Domain Layer: Exception 생성
throw new DomainException(
    "ORDER_INVALID_STATE",
    "Invalid state transition",
    List.of("PLACED", "CANCELLED")  // ✅ args: [PLACED, CANCELLED]
);

// messages_ko.properties
problem.detail.order_invalid_state=주문 상태가 {0}에서 {1}(으)로 변경할 수 없습니다.

// 결과 (한국어)
"주문 상태가 PLACED에서 CANCELLED(으)로 변경할 수 없습니다."

// 결과 (영어)
"Cannot change order state from PLACED to CANCELLED."
```

---

## 6️⃣ RFC 7807 Type URI 설계 (Type URI Design)

### Type URI 생성 패턴

```java
// ✅ 1. BASE URI 정의 (Bounded Context별)
private static final String TYPE_BASE = "https://api.example.com/problems/order/";

// ✅ 2. Type URI 생성 메서드
private URI createTypeUri(String code) {
    // 소문자, 하이픈 구분 (order-not-found)
    String path = code.toLowerCase().replace('_', '-');
    return URI.create(TYPE_BASE + path);
}

// 예시:
// "ORDER_NOT_FOUND" → "https://api.example.com/problems/order/order-not-found"
// "ORDER_INVALID_STATE" → "https://api.example.com/problems/order/order-invalid-state"
```

### Type URI 네이밍 규칙

| 에러 코드 (Domain) | Type URI (RFC 7807) |
|-------------------|---------------------|
| `ORDER_NOT_FOUND` | `https://api.example.com/problems/order/order-not-found` |
| `ORDER_INVALID_STATE` | `https://api.example.com/problems/order/order-invalid-state` |
| `PRODUCT_OUT_OF_STOCK` | `https://api.example.com/problems/product/product-out-of-stock` |

**규칙**:
- ✅ 소문자 사용 (`order-not-found`)
- ✅ 하이픈(`-`) 구분자 사용
- ✅ Bounded Context별 경로 분리 (`/order/`, `/product/`)
- ❌ 언더스코어(`_`) 사용 금지

### about:blank (기본 URI)

```java
// ✅ ErrorMapperRegistry에서 기본 매핑 시 사용
public ErrorMapper.MappedError defaultMapping(DomainException ex) {
    return new ErrorMapper.MappedError(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        ex.getMessage(),
        URI.create("about:blank")  // ✅ RFC 7807 표준 기본 URI
    );
}
```

---

## 7️⃣ 완전한 구현 예시 (Complete Example)

### Product Bounded Context ErrorMapper

```java
package com.ryuqq.adapter.in.rest.product.error;

import com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper;
import com.ryuqq.domain.common.exception.DomainException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * Product Bounded Context ErrorMapper
 *
 * <p>PRODUCT_ 접두사를 가진 Domain Exception을 HTTP Problem Details로 변환합니다.</p>
 *
 * <p>예시:</p>
 * <ul>
 *   <li>PRODUCT_NOT_FOUND → 404 Not Found</li>
 *   <li>PRODUCT_OUT_OF_STOCK → 409 Conflict</li>
 *   <li>PRODUCT_INVALID_PRICE → 422 Unprocessable Entity</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class ProductErrorMapper implements ErrorMapper {

    private static final String PREFIX = "PRODUCT_";
    private static final String TYPE_BASE = "https://api.example.com/problems/product/";

    private final MessageSource messageSource;

    public ProductErrorMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean supports(String code) {
        return code != null && code.startsWith(PREFIX);
    }

    @Override
    public MappedError map(DomainException ex, Locale locale) {
        String code = ex.code();

        HttpStatus status = mapHttpStatus(code);
        String title = resolveTitle(code, status, locale);
        String detail = resolveDetail(code, ex, locale);
        URI type = createTypeUri(code);

        return new MappedError(status, title, detail, type);
    }

    /**
     * 에러 코드 → HTTP 상태 코드 매핑
     */
    private HttpStatus mapHttpStatus(String code) {
        return switch (code) {
            case "PRODUCT_NOT_FOUND" -> HttpStatus.NOT_FOUND;

            case "PRODUCT_OUT_OF_STOCK" -> HttpStatus.CONFLICT;
            case "PRODUCT_DUPLICATE_KEY" -> HttpStatus.CONFLICT;

            case "PRODUCT_INVALID_PRICE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "PRODUCT_INVALID_CATEGORY" -> HttpStatus.UNPROCESSABLE_ENTITY;

            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String resolveTitle(String code, HttpStatus status, Locale locale) {
        String key = "problem.title." + code.toLowerCase();
        return messageSource.getMessage(key, new Object[0], status.getReasonPhrase(), locale);
    }

    private String resolveDetail(String code, DomainException ex, Locale locale) {
        String key = "problem.detail." + code.toLowerCase();
        return messageSource.getMessage(key, ex.args().toArray(), ex.getMessage(), locale);
    }

    private URI createTypeUri(String code) {
        String path = code.toLowerCase().replace('_', '-');
        return URI.create(TYPE_BASE + path);
    }
}
```

---

## 8️⃣ 테스트 전략 (Testing Strategy)

### Unit Test 예시 (JUnit 5)

```java
package com.ryuqq.adapter.in.rest.product.error;

import com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper;
import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ProductErrorMapper 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("ProductErrorMapper Unit Tests")
class ProductErrorMapperTest {

    private MessageSource messageSource;
    private ProductErrorMapper errorMapper;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        errorMapper = new ProductErrorMapper(messageSource);
    }

    /**
     * supports() 메서드 테스트
     */
    @Test
    @DisplayName("PRODUCT_ 접두사를 가진 코드는 지원한다")
    void supports_ProductPrefix_ReturnsTrue() {
        // when & then
        assertThat(errorMapper.supports("PRODUCT_NOT_FOUND")).isTrue();
        assertThat(errorMapper.supports("PRODUCT_OUT_OF_STOCK")).isTrue();
    }

    @Test
    @DisplayName("PRODUCT_ 접두사가 없는 코드는 지원하지 않는다")
    void supports_NonProductPrefix_ReturnsFalse() {
        // when & then
        assertThat(errorMapper.supports("ORDER_NOT_FOUND")).isFalse();
        assertThat(errorMapper.supports("CUSTOMER_NOT_FOUND")).isFalse();
    }

    @Test
    @DisplayName("null 코드는 지원하지 않는다")
    void supports_NullCode_ReturnsFalse() {
        // when & then
        assertThat(errorMapper.supports(null)).isFalse();
    }

    /**
     * HttpStatus 매핑 테스트
     */
    @Test
    @DisplayName("PRODUCT_NOT_FOUND는 404 Not Found로 매핑된다")
    void map_ProductNotFound_Returns404() {
        // given
        DomainException ex = new DomainException("PRODUCT_NOT_FOUND", "Not found", List.of());
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn("Product Not Found");

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.ENGLISH);

        // then
        assertThat(mapped.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PRODUCT_OUT_OF_STOCK은 409 Conflict로 매핑된다")
    void map_ProductOutOfStock_Returns409() {
        // given
        DomainException ex = new DomainException("PRODUCT_OUT_OF_STOCK", "Out of stock", List.of());
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn("Out of Stock");

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.ENGLISH);

        // then
        assertThat(mapped.status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PRODUCT_INVALID_PRICE는 422 Unprocessable Entity로 매핑된다")
    void map_ProductInvalidPrice_Returns422() {
        // given
        DomainException ex = new DomainException("PRODUCT_INVALID_PRICE", "Invalid price", List.of());
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn("Invalid Price");

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.ENGLISH);

        // then
        assertThat(mapped.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Type URI 생성 테스트
     */
    @Test
    @DisplayName("Type URI는 소문자, 하이픈 구분으로 생성된다")
    void map_ProductNotFound_GeneratesCorrectTypeUri() {
        // given
        DomainException ex = new DomainException("PRODUCT_NOT_FOUND", "Not found", List.of());
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
            .thenReturn("Product Not Found");

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.ENGLISH);

        // then
        URI expected = URI.create("https://api.example.com/problems/product/product-not-found");
        assertThat(mapped.type()).isEqualTo(expected);
    }

    /**
     * MessageSource 통합 테스트
     */
    @Test
    @DisplayName("MessageSource에서 Title과 Detail을 조회한다")
    void map_CallsMessageSourceForTitleAndDetail() {
        // given
        DomainException ex = new DomainException(
            "PRODUCT_OUT_OF_STOCK",
            "Out of stock",
            List.of("P001", "5")  // productId, quantity
        );

        when(messageSource.getMessage(
            eq("problem.title.product_out_of_stock"),
            any(),
            anyString(),
            eq(Locale.KOREAN)
        )).thenReturn("재고 부족");

        when(messageSource.getMessage(
            eq("problem.detail.product_out_of_stock"),
            eq(new Object[]{"P001", "5"}),
            anyString(),
            eq(Locale.KOREAN)
        )).thenReturn("상품 P001의 재고가 5개 부족합니다.");

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.KOREAN);

        // then
        assertThat(mapped.title()).isEqualTo("재고 부족");
        assertThat(mapped.detail()).isEqualTo("상품 P001의 재고가 5개 부족합니다.");
    }
}
```

### Integration Test (Spring Context 로딩)

```java
package com.ryuqq.adapter.in.rest.product.error;

import com.ryuqq.adapter.in.rest.common.mapper.ErrorMapper;
import com.ryuqq.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductErrorMapper 통합 테스트 (실제 MessageSource 사용)
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("ProductErrorMapper Integration Tests")
class ProductErrorMapperIntegrationTest {

    @Autowired
    private ProductErrorMapper errorMapper;

    @Test
    @DisplayName("한국어 메시지가 올바르게 조회된다")
    void map_KoreanLocale_ReturnsKoreanMessages() {
        // given
        DomainException ex = new DomainException(
            "PRODUCT_NOT_FOUND",
            "Not found",
            List.of("P001")
        );

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.KOREAN);

        // then
        assertThat(mapped.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mapped.title()).contains("상품");  // 한국어 확인
        assertThat(mapped.detail()).contains("P001");
    }

    @Test
    @DisplayName("영어 메시지가 올바르게 조회된다")
    void map_EnglishLocale_ReturnsEnglishMessages() {
        // given
        DomainException ex = new DomainException(
            "PRODUCT_NOT_FOUND",
            "Not found",
            List.of("P001")
        );

        // when
        ErrorMapper.MappedError mapped = errorMapper.map(ex, Locale.ENGLISH);

        // then
        assertThat(mapped.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mapped.title()).contains("Product");  // 영어 확인
        assertThat(mapped.detail()).contains("P001");
    }
}
```

---

## 9️⃣ Common Patterns & Anti-Patterns

### ✅ Good Patterns

```java
// ✅ 1. PREFIX 상수로 관리
private static final String PREFIX = "ORDER_";

// ✅ 2. Type URI Base 상수로 관리
private static final String TYPE_BASE = "https://api.example.com/problems/order/";

// ✅ 3. MessageSource DI (Constructor Injection)
private final MessageSource messageSource;

public OrderErrorMapper(MessageSource messageSource) {
    this.messageSource = messageSource;
}

// ✅ 4. Switch Expression 사용 (Java 21)
private HttpStatus mapHttpStatus(String code) {
    return switch (code) {
        case "ORDER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
        default -> HttpStatus.BAD_REQUEST;
    };
}

// ✅ 5. Fallback 메시지 제공
String title = messageSource.getMessage(
    key,
    new Object[0],
    status.getReasonPhrase(),  // ✅ Fallback
    locale
);

// ✅ 6. 파라미터 바인딩 (args)
String detail = messageSource.getMessage(
    key,
    ex.args().toArray(),  // ✅ 파라미터 전달
    ex.getMessage(),
    locale
);
```

### ❌ Anti-Patterns

```java
// ❌ 1. 하드코딩된 메시지 (I18N 불가)
private String resolveTitle(String code, HttpStatus status, Locale locale) {
    return "Order Not Found";  // ❌ I18N 지원 불가
}

// ❌ 2. 500 에러 매핑 (시스템 예외와 혼동)
private HttpStatus mapHttpStatus(String code) {
    return switch (code) {
        case "ORDER_SYSTEM_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;  // ❌ 금지
        default -> HttpStatus.BAD_REQUEST;
    };
}

// ❌ 3. MessageSource 예외 무시
private String resolveTitle(String code, HttpStatus status, Locale locale) {
    try {
        return messageSource.getMessage(key, new Object[0], locale);
    } catch (Exception e) {
        return "";  // ❌ 빈 문자열 반환 금지
    }
}

// ❌ 4. Type URI 하드코딩
private URI createTypeUri(String code) {
    return URI.create("https://api.example.com/problems/order-not-found");  // ❌ 동적 생성 필요
}

// ❌ 5. 비즈니스 로직 포함
@Override
public MappedError map(DomainException ex, Locale locale) {
    // ❌ ErrorMapper는 변환만 담당, 비즈니스 로직 금지
    if (shouldRetry(ex)) {
        // ...
    }
}
```

---

## 🔟 체크리스트

- [ ] `@Component` 어노테이션 선언
- [ ] `ErrorMapper` 인터페이스 구현
- [ ] `PREFIX` 상수 정의 (Bounded Context 식별자)
- [ ] `TYPE_BASE` 상수 정의 (RFC 7807 Type URI Base)
- [ ] `MessageSource` DI (Constructor Injection)
- [ ] `supports()` 메서드 구현 (PREFIX 기반)
- [ ] `map()` 메서드 구현 (HttpStatus, Title, Detail, Type URI)
- [ ] Switch Expression 사용 (HttpStatus 매핑)
- [ ] MessageSource 통합 (I18N 지원)
- [ ] Type URI 동적 생성 (소문자, 하이픈 구분)
- [ ] Fallback 메시지 제공 (MessageSource 조회 실패 시)
- [ ] `500` 에러 매핑 금지 (시스템 예외와 분리)
- [ ] Unit Test 작성 (supports, map, HttpStatus, Type URI)
- [ ] Integration Test 작성 (실제 MessageSource 사용)
- [ ] messages.properties 파일 작성 (한국어, 영어)
- [ ] Javadoc 작성 (`@author`, `@since`)

---

## 1️⃣1️⃣ 추가 가이드 링크

- **[Error Handling Strategy Guide](./error-handling-strategy.md)** - 전체 에러 처리 전략 (이미 읽음)
- **[GlobalExceptionHandler Guide](./global-exception-handler-guide.md)** - GlobalExceptionHandler 구현 가이드 (다음)
- **[RFC 7807 Response Format Guide](./rfc-7807-response-format-guide.md)** - RFC 7807 응답 형식 상세
- **[Error Package Structure Guide](./error-package-structure-guide.md)** - 패키지 구조 및 파일 배치

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
