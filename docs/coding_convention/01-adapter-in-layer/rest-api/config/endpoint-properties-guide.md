# Endpoint Properties Guide — **중앙 집중식 엔드포인트 관리**

> **목적**: REST API 엔드포인트를 Properties로 중앙 관리하여 유지보수성 향상
>
> **철학**: 하드코딩 금지, 버전 관리 용이, Bounded Context별 구조화

---

## 1️⃣ 핵심 원칙 (Core Principles)

### 엔드포인트는 절대 하드코딩하지 않습니다

**모든 엔드포인트는 `ApiEndpointProperties`와 `application.yml`로 중앙 관리합니다.**

### ❌ Bad: 하드코딩

```java
@RestController
@RequestMapping("/api/v1/orders")  // ❌ 하드코딩 금지
public class OrderController {

    @GetMapping("/{id}")  // ❌ 하드코딩 금지
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        // ...
    }

    @PatchMapping("/{id}/cancel")  // ❌ 하드코딩 금지
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        // ...
    }
}
```

**문제점**:
- 엔드포인트 변경 시 모든 Controller 수정 필요
- API 버전 관리 어려움 (v1 → v2 마이그레이션)
- 실수로 잘못된 경로 사용 가능
- 엔드포인트 일관성 보장 어려움

### ✅ Good: Properties 사용

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")  // ✅ Properties 사용
public class OrderController {

    @GetMapping("${api.endpoints.order.by-id}")  // ✅ Properties 사용
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        // ...
    }

    @PatchMapping("${api.endpoints.order.cancel}")  // ✅ Properties 사용
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        // ...
    }
}
```

**장점**:
- ✅ **중앙 관리**: 한 곳에서 모든 엔드포인트 관리
- ✅ **버전 관리**: v1 → v2 마이그레이션 용이
- ✅ **일관성**: Bounded Context별 구조화로 일관성 보장
- ✅ **유지보수**: 변경 시 application.yml만 수정
- ✅ **문서화**: 엔드포인트 구조 한눈에 파악 가능

---

## 2️⃣ ApiEndpointProperties 클래스 구조

### 기본 구조

```java
package com.ryuqq.adapter.in.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API 엔드포인트 경로 설정 Properties
 *
 * <p>REST API 엔드포인트 경로를 application.yml에서 중앙 관리합니다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "api.endpoints")
public class ApiEndpointProperties {

    /**
     * API v1 베이스 경로 (기본값: /api/v1)
     */
    private String baseV1 = "/api/v1";

    /**
     * Order 도메인 엔드포인트 설정
     */
    private OrderEndpoints order = new OrderEndpoints();

    /**
     * Product 도메인 엔드포인트 설정
     */
    private ProductEndpoints product = new ProductEndpoints();

    /**
     * Order 도메인 엔드포인트 경로
     */
    public static class OrderEndpoints {
        /**
         * Order 기본 경로 (기본값: /orders)
         */
        private String base = "/orders";

        /**
         * Order ID 조회 경로 (기본값: /{id})
         */
        private String byId = "/{id}";

        /**
         * Order 취소 경로 (기본값: /{id}/cancel)
         */
        private String cancel = "/{id}/cancel";

        /**
         * Order 확인 경로 (기본값: /{id}/confirm)
         */
        private String confirm = "/{id}/confirm";

        // Getters/Setters (생략)
    }

    /**
     * Product 도메인 엔드포인트 경로
     */
    public static class ProductEndpoints {
        private String base = "/products";
        private String byId = "/{id}";

        // Getters/Setters (생략)
    }

    // Getters/Setters (생략)
}
```

### 핵심 포인트

1. **`@ConfigurationProperties(prefix = "api.endpoints")`**: application.yml의 `api.endpoints` 섹션과 바인딩
2. **`@Component`**: Spring Bean으로 등록 (Controller에서 주입 가능)
3. **Nested Static Class**: Bounded Context별 엔드포인트 그룹화
4. **기본값 제공**: application.yml 없어도 동작 (fallback)
5. **Javadoc 필수**: 각 엔드포인트의 용도 명확히 문서화

---

## 3️⃣ application.yml 구조

### 기본 구조

```yaml
# ===============================================
# API Endpoint Configuration
# ===============================================
api:
  endpoints:
    # API v1 베이스 경로
    base-v1: /api/v1

    # Order 도메인 엔드포인트
    order:
      base: /orders
      by-id: /{id}
      cancel: /{id}/cancel
      confirm: /{id}/confirm

    # Product 도메인 엔드포인트
    product:
      base: /products
      by-id: /{id}
```

### 네이밍 규칙

| 항목 | 규칙 | 예시 |
|------|------|------|
| **Bounded Context** | kebab-case, 단수형 | `order`, `product`, `payment` |
| **엔드포인트** | kebab-case | `by-id`, `admin-search` |
| **PathVariable** | 중괄호 포함 | `/{id}`, `/{orderId}` |
| **복합 경로** | 전체 경로 | `/{id}/cancel`, `/admin/orders/search` |

### 버전 관리

```yaml
api:
  endpoints:
    # v1 베이스 경로
    base-v1: /api/v1

    # v2 베이스 경로 (향후 추가)
    base-v2: /api/v2

    order:
      # v1과 v2에서 동일한 상대 경로 사용
      base: /orders
      by-id: /{id}
```

---

## 4️⃣ Controller에서 사용하기

### Command Controller 예시

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.ryuqq.adapter.in.rest.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Order Command Controller
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")  // ✅ /api/v1/orders
public class OrderCommandController {

    /**
     * 주문 생성
     */
    @PostMapping  // ✅ POST /api/v1/orders
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(...) {
        // ...
    }

    /**
     * 주문 취소
     */
    @PatchMapping("${api.endpoints.order.cancel}")  // ✅ PATCH /api/v1/orders/{id}/cancel
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        // ...
    }

    /**
     * 주문 확인
     */
    @PatchMapping("${api.endpoints.order.confirm}")  // ✅ PATCH /api/v1/orders/{id}/confirm
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable Long id) {
        // ...
    }
}
```

### Query Controller 예시

```java
package com.ryuqq.adapter.in.rest.order.controller;

import com.ryuqq.adapter.in.rest.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Order Query Controller
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")  // ✅ /api/v1/orders
public class OrderQueryController {

    /**
     * 주문 단건 조회
     */
    @GetMapping("${api.endpoints.order.by-id}")  // ✅ GET /api/v1/orders/{id}
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(@PathVariable Long id) {
        // ...
    }

    /**
     * 주문 검색
     */
    @GetMapping  // ✅ GET /api/v1/orders
    public ResponseEntity<ApiResponse<List<OrderSummaryApiResponse>>> searchOrders(...) {
        // ...
    }
}
```

---

## 5️⃣ 새 Bounded Context 추가하기

### Step 1: ApiEndpointProperties에 Nested Class 추가

```java
@Component
@ConfigurationProperties(prefix = "api.endpoints")
public class ApiEndpointProperties {

    private String baseV1 = "/api/v1";

    private OrderEndpoints order = new OrderEndpoints();
    private ProductEndpoints product = new ProductEndpoints();
    private PaymentEndpoints payment = new PaymentEndpoints();  // ✅ 새로운 BC 추가

    /**
     * Payment 도메인 엔드포인트 경로
     */
    public static class PaymentEndpoints {
        private String base = "/payments";
        private String byId = "/{id}";
        private String process = "/{id}/process";
        private String refund = "/{id}/refund";

        // Getters/Setters
    }

    // Getters/Setters
}
```

### Step 2: application.yml에 설정 추가

```yaml
api:
  endpoints:
    base-v1: /api/v1

    order:
      base: /orders
      by-id: /{id}

    product:
      base: /products
      by-id: /{id}

    # ✅ 새로운 BC 추가
    payment:
      base: /payments
      by-id: /{id}
      process: /{id}/process
      refund: /{id}/refund
```

### Step 3: Controller 작성

```java
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.payment.base}")
public class PaymentCommandController {

    @PostMapping("${api.endpoints.payment.process}")
    public ResponseEntity<ApiResponse<PaymentApiResponse>> processPayment(@PathVariable Long id) {
        // ...
    }
}
```

---

## 6️⃣ 관리자 API (Admin API) 패턴

### 일반 API vs 관리자 API

```yaml
api:
  endpoints:
    base-v1: /api/v1

    order:
      # 일반 사용자 API
      base: /orders
      by-id: /{id}

      # 관리자 API (admin 접두사)
      admin-search: /admin/orders/search
      admin-export: /admin/orders/export
```

### Controller 구조

```java
// 일반 사용자 API
@RestController
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")
public class OrderQueryController {
    // GET /api/v1/orders/{id}
}

// 관리자 API
@RestController
@RequestMapping("${api.endpoints.base-v1}")
public class OrderAdminController {

    @GetMapping("${api.endpoints.order.admin-search}")
    public ResponseEntity<ApiResponse<PageApiResponse<OrderAdminApiResponse>>> searchOrders(...) {
        // GET /api/v1/admin/orders/search
    }
}
```

---

## 7️⃣ 환경별 설정 (Profile)

### application.yml (공통)

```yaml
api:
  endpoints:
    base-v1: /api/v1
    order:
      base: /orders
```

### application-dev.yml (개발)

```yaml
api:
  endpoints:
    # 개발 환경에서는 /dev 접두사 추가
    base-v1: /dev/api/v1
```

### application-prod.yml (운영)

```yaml
api:
  endpoints:
    # 운영 환경에서는 기본값 사용
    base-v1: /api/v1
```

---

## 8️⃣ Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. Properties 사용
@RequestMapping("${api.endpoints.base-v1}${api.endpoints.order.base}")

// ✅ 2. 전체 경로를 Properties로 관리
@PatchMapping("${api.endpoints.order.cancel}")  // /{id}/cancel

// ✅ 3. Nested Class로 BC별 그룹화
public static class OrderEndpoints {
    private String base = "/orders";
    private String cancel = "/{id}/cancel";
}

// ✅ 4. 기본값 제공
private String base = "/orders";  // application.yml 없어도 동작

// ✅ 5. Javadoc으로 용도 명확히
/**
 * Order 취소 경로 (기본값: /{id}/cancel)
 */
private String cancel = "/{id}/cancel";
```

### ❌ Anti-Patterns

```java
// ❌ 1. 하드코딩
@RequestMapping("/api/v1/orders")  // ❌ Properties 사용해야 함

// ❌ 2. PathVariable을 Properties에 포함 안 함
@GetMapping("/${api.endpoints.order.by-id}")  // ❌ {id}를 Properties에 포함해야 함

// ❌ 3. 복합 경로를 조합
@GetMapping("${api.endpoints.order.by-id}" + "/cancel")  // ❌ 전체 경로를 Properties로

// ❌ 4. Properties 없이 Controller 작성
@RestController
public class OrderController {  // ❌ @RequestMapping에 Properties 사용해야 함
    // ...
}

// ❌ 5. BC별 그룹화 없이 flat 구조
private String orderBase = "/orders";  // ❌ Nested Class로 그룹화해야 함
private String orderCancel = "/{id}/cancel";
private String productBase = "/products";
```

---

## 9️⃣ 체크리스트

- [ ] `ApiEndpointProperties` 클래스에 `@ConfigurationProperties(prefix = "api.endpoints")` 선언
- [ ] `@Component` 어노테이션으로 Spring Bean 등록
- [ ] Bounded Context별 Nested Static Class 작성
- [ ] 각 필드에 기본값 제공
- [ ] Javadoc으로 각 엔드포인트 용도 명확히 문서화
- [ ] `application.yml`에 `api.endpoints` 섹션 작성
- [ ] Bounded Context별 엔드포인트 계층 구조화
- [ ] kebab-case 네이밍 규칙 준수
- [ ] PathVariable 포함 (`/{id}`, `/{orderId}`)
- [ ] Controller에서 `@RequestMapping("${...}")` 형식으로 Properties 참조
- [ ] 모든 엔드포인트를 Properties로 관리 (하드코딩 금지)
- [ ] 버전 관리 고려 (`base-v1`, `base-v2`)
- [ ] 환경별 설정 분리 (dev, prod)

---

## 🔟 추가 가이드 링크

- **[Controller Guide](../controller/controller-guide.md)** - Controller 작성 가이드
- **[Error Properties Guide](./error-properties-guide.md)** - 에러 응답 Properties 가이드 (TBD)
- **[REST API Guide](../rest-api-guide.md)** - REST API Layer 전체 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
