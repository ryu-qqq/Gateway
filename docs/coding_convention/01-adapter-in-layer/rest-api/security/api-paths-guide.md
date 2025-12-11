# API Paths Guide — **Constants 방식 경로 관리**

> **목적**: REST API 엔드포인트를 Constants 클래스로 중앙 관리하여 컴파일 타임 검증 보장
>
> **철학**: 업계 표준 준수, IDE 지원 극대화, Controller-Security 동기화

---

## 1️⃣ 핵심 원칙 (Core Principles)

### 왜 Constants 방식인가?

**SpEL 방식 (❌ 비권장)**:
```java
// ❌ 런타임에만 오류 발견, IDE 지원 없음
@RequestMapping("${api.endpoints.order.base}")
```

**Constants 방식 (✅ 업계 표준)**:
```java
// ✅ 컴파일 타임 검증, IDE 자동완성/리팩토링 지원
@RequestMapping(ApiPaths.Orders.BASE)
```

### Constants 방식의 장점

| 항목 | SpEL 방식 | Constants 방식 |
|------|-----------|----------------|
| **오타 검증** | 런타임 | 컴파일 타임 |
| **IDE 자동완성** | ❌ 불가 | ✅ 가능 |
| **리팩토링** | ❌ 수동 | ✅ 자동 |
| **Controller-Security 동기화** | ❌ 보장 불가 | ✅ 동일 상수 참조 |
| **Find Usages** | ❌ 불가 | ✅ 가능 |
| **타입 안전성** | ❌ 문자열 | ✅ 상수 |

---

## 2️⃣ ApiPaths 클래스 구조

### 기본 구조

```java
package com.company.adapter.in.rest.auth.paths;

/**
 * API 경로 상수 정의
 *
 * <p>모든 REST API 엔드포인트 경로를 상수로 관리합니다.
 *
 * <p>사용 위치:
 * <ul>
 *   <li>Controller - @RequestMapping, @GetMapping 등</li>
 *   <li>SecurityConfig - 인가 설정</li>
 *   <li>Test - MockMvc 테스트</li>
 * </ul>
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>Bounded Context별 Nested Class로 그룹화</li>
 *   <li>private 생성자로 인스턴스화 방지</li>
 *   <li>final 클래스로 상속 방지</li>
 *   <li>static final 상수로 정의</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class ApiPaths {

    /**
     * API 버전 1 베이스 경로
     */
    public static final String API_V1 = "/api/v1";

    /**
     * API 버전 2 베이스 경로 (향후 확장)
     */
    public static final String API_V2 = "/api/v2";

    /**
     * Health Check 경로
     */
    public static final String HEALTH = API_V1 + "/health";

    // 인스턴스화 방지
    private ApiPaths() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========================================
    // Bounded Context별 경로 정의
    // ========================================

    /**
     * Member 도메인 경로
     */
    public static final class Members {
        public static final String BASE = API_V1 + "/members";
        public static final String REGISTER = BASE;
        public static final String BY_ID = BASE + "/{id}";
        public static final String ME = BASE + "/me";
        public static final String PASSWORD_RESET = BASE + "/password/reset";
        public static final String PASSWORD_CHANGE = BASE + "/password/change";
        public static final String EMAIL_VERIFY = BASE + "/email/verify";

        private Members() {}
    }

    /**
     * Auth 도메인 경로
     */
    public static final class Auth {
        public static final String BASE = API_V1 + "/auth";
        public static final String LOGIN = BASE + "/login";
        public static final String LOGOUT = BASE + "/logout";
        public static final String REFRESH = BASE + "/refresh";

        private Auth() {}
    }

    /**
     * Order 도메인 경로
     */
    public static final class Orders {
        public static final String BASE = API_V1 + "/orders";
        public static final String BY_ID = BASE + "/{id}";
        public static final String SEARCH = BASE + "/search";
        public static final String CANCEL = BASE + "/{id}/cancel";
        public static final String CONFIRM = BASE + "/{id}/confirm";
        public static final String ITEMS = BASE + "/{id}/items";

        private Orders() {}
    }

    /**
     * Product 도메인 경로
     */
    public static final class Products {
        public static final String BASE = API_V1 + "/products";
        public static final String BY_ID = BASE + "/{id}";
        public static final String SEARCH = BASE + "/search";
        public static final String CATEGORIES = BASE + "/categories";
        public static final String BY_CATEGORY = BASE + "/categories/{categoryId}";

        private Products() {}
    }

    /**
     * Admin 도메인 경로
     */
    public static final class Admin {
        public static final String BASE = API_V1 + "/admin";

        // Admin - Members
        public static final String MEMBERS = BASE + "/members";
        public static final String MEMBERS_SEARCH = MEMBERS + "/search";
        public static final String MEMBERS_BY_ID = MEMBERS + "/{id}";

        // Admin - Orders
        public static final String ORDERS = BASE + "/orders";
        public static final String ORDERS_SEARCH = ORDERS + "/search";
        public static final String ORDERS_EXPORT = ORDERS + "/export";
        public static final String ORDERS_BY_ID = ORDERS + "/{id}";

        // Admin - Products
        public static final String PRODUCTS = BASE + "/products";
        public static final String PRODUCTS_BY_ID = PRODUCTS + "/{id}";

        private Admin() {}
    }
}
```

### 핵심 포인트

1. **`final class`**: 상속 방지
2. **`private 생성자`**: 인스턴스화 방지, UnsupportedOperationException throw
3. **`static final` 상수**: 컴파일 타임 상수로 처리
4. **Nested Class**: Bounded Context별 논리적 그룹화
5. **경로 조합**: 베이스 경로를 상속하여 일관성 유지

---

## 3️⃣ SecurityPaths 클래스 구조

### 보안 정책별 그룹화

```java
package com.company.adapter.in.rest.auth.paths;

/**
 * 보안 정책별 경로 그룹화
 *
 * <p>인증/인가 정책에 따라 경로를 그룹화합니다.
 *
 * <p>그룹 유형:
 * <ul>
 *   <li>PUBLIC_ENDPOINTS: 인증 불필요 (정확한 경로)</li>
 *   <li>PUBLIC_PATTERNS: 인증 불필요 (와일드카드 패턴)</li>
 *   <li>ADMIN_ENDPOINTS: 관리자 권한 필요</li>
 *   <li>OWNER_VERIFICATION_REQUIRED: 리소스 소유자 검증 필요</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class SecurityPaths {

    private SecurityPaths() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========================================
    // Public Endpoints (인증 불필요)
    // ========================================

    /**
     * 인증 불필요 엔드포인트 (정확한 경로)
     *
     * <p>로그인, 회원가입 등 인증 없이 접근 가능한 경로
     */
    public static final String[] PUBLIC_ENDPOINTS = {
        ApiPaths.Members.REGISTER,
        ApiPaths.Members.PASSWORD_RESET,
        ApiPaths.Members.EMAIL_VERIFY,
        ApiPaths.Auth.LOGIN,
        ApiPaths.Auth.REFRESH,
        ApiPaths.HEALTH
    };

    /**
     * 인증 불필요 패턴 (와일드카드)
     *
     * <p>Swagger, Actuator, OAuth2 등 공개 패턴
     */
    public static final String[] PUBLIC_PATTERNS = {
        "/oauth2/**",
        "/login/oauth2/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/actuator/**",
        "/error"
    };

    // ========================================
    // Admin Endpoints (관리자 권한 필요)
    // ========================================

    /**
     * 관리자 전용 엔드포인트 (ROLE_ADMIN 필요)
     */
    public static final String[] ADMIN_PATTERNS = {
        ApiPaths.Admin.BASE + "/**"
    };

    // ========================================
    // Owner Verification Required
    // ========================================

    /**
     * 리소스 소유자 검증 필요 엔드포인트
     *
     * <p>Method Security로 추가 검증 필요
     * <ul>
     *   <li>@PreAuthorize로 소유자 검증</li>
     *   <li>SecurityChecker Bean 사용</li>
     * </ul>
     */
    public static final String[] OWNER_VERIFICATION_REQUIRED = {
        ApiPaths.Members.BY_ID,
        ApiPaths.Members.ME,
        ApiPaths.Members.PASSWORD_CHANGE,
        ApiPaths.Orders.BY_ID,
        ApiPaths.Orders.CANCEL
    };

    // ========================================
    // Read-Only Endpoints (GET만 허용)
    // ========================================

    /**
     * 읽기 전용 공개 엔드포인트
     *
     * <p>GET 요청만 허용되는 공개 리소스
     */
    public static final String[] PUBLIC_READ_ONLY = {
        ApiPaths.Products.BASE,
        ApiPaths.Products.BY_ID,
        ApiPaths.Products.SEARCH,
        ApiPaths.Products.CATEGORIES,
        ApiPaths.Products.BY_CATEGORY
    };
}
```

---

## 4️⃣ Controller에서 사용하기

### Command Controller 예시

```java
package com.company.adapter.in.rest.order.controller;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import com.company.adapter.in.rest.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Order Command Controller
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping(ApiPaths.Orders.BASE)  // ✅ Constants 참조
public class OrderCommandController {

    /**
     * 주문 생성
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
            @RequestBody @Valid OrderCreateRequest request) {
        // ...
    }

    /**
     * 주문 취소 - 소유자만 가능
     * PATCH /api/v1/orders/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")  // 상대 경로는 문자열 허용
    @PreAuthorize("@orderSecurityChecker.isOwner(#id, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        // ...
    }

    /**
     * 주문 확인
     * PATCH /api/v1/orders/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("@orderSecurityChecker.isOwner(#id, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable Long id) {
        // ...
    }
}
```

### Query Controller 예시

```java
package com.company.adapter.in.rest.order.controller;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import com.company.adapter.in.rest.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Order Query Controller
 *
 * @author development-team
 * @since 1.0.0
 */
@RestController
@RequestMapping(ApiPaths.Orders.BASE)  // ✅ Constants 참조
public class OrderQueryController {

    /**
     * 주문 단건 조회
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(
            @PathVariable Long id) {
        // ...
    }

    /**
     * 주문 목록 조회 (본인 주문만)
     * GET /api/v1/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryApiResponse>>> getMyOrders(
            @AuthenticationPrincipal MemberPrincipal principal) {
        // ...
    }

    /**
     * 주문 검색
     * GET /api/v1/orders/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageApiResponse<OrderApiResponse>>> searchOrders(
            @ModelAttribute @Valid OrderSearchRequest request) {
        // ...
    }
}
```

---

## 5️⃣ SecurityConfig에서 사용하기

### 기본 설정

```java
package com.company.adapter.in.rest.auth.config;

import com.company.adapter.in.rest.auth.paths.SecurityPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * <p>SecurityPaths의 Constants를 참조하여 인가 규칙 설정
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ✅ Public Endpoints (정확한 경로)
                .requestMatchers(SecurityPaths.PUBLIC_ENDPOINTS).permitAll()

                // ✅ Public Patterns (와일드카드)
                .requestMatchers(SecurityPaths.PUBLIC_PATTERNS).permitAll()

                // ✅ Public Read-Only (GET만)
                .requestMatchers(HttpMethod.GET, SecurityPaths.PUBLIC_READ_ONLY).permitAll()

                // ✅ Admin Patterns (ROLE_ADMIN)
                .requestMatchers(SecurityPaths.ADMIN_PATTERNS).hasRole("ADMIN")

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )

            // ... 나머지 설정
            .build();
    }
}
```

---

## 6️⃣ 테스트에서 사용하기

### MockMvc 테스트

```java
package com.company.adapter.in.rest.order.controller;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderQueryController.class)
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 주문_단건_조회() throws Exception {
        // Given
        Long orderId = 1L;

        // When & Then
        mockMvc.perform(get(ApiPaths.Orders.BY_ID, orderId))  // ✅ Constants 참조
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(orderId));
    }

    @Test
    void 주문_검색() throws Exception {
        mockMvc.perform(get(ApiPaths.Orders.SEARCH)  // ✅ Constants 참조
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk());
    }
}
```

### TestRestTemplate 테스트

```java
package com.company.adapter.in.rest.order.controller;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 주문_생성_성공() {
        // Given
        OrderCreateRequest request = new OrderCreateRequest(/* ... */);

        // When
        ResponseEntity<ApiResponse<OrderApiResponse>> response = restTemplate
            .postForEntity(ApiPaths.Orders.BASE, request, ...);  // ✅ Constants 참조

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

---

## 7️⃣ 새 Bounded Context 추가하기

### Step 1: ApiPaths에 Nested Class 추가

```java
public final class ApiPaths {
    // ... 기존 코드 ...

    /**
     * Payment 도메인 경로 (새로 추가)
     */
    public static final class Payments {
        public static final String BASE = API_V1 + "/payments";
        public static final String BY_ID = BASE + "/{id}";
        public static final String PROCESS = BASE + "/{id}/process";
        public static final String REFUND = BASE + "/{id}/refund";
        public static final String HISTORY = BASE + "/history";

        private Payments() {}
    }
}
```

### Step 2: SecurityPaths에 필요한 그룹 추가

```java
public final class SecurityPaths {
    // ... 기존 코드 ...

    // PUBLIC_ENDPOINTS 배열에 추가 (필요시)
    public static final String[] PUBLIC_ENDPOINTS = {
        // ... 기존 경로 ...
        // ApiPaths.Payments.WEBHOOK,  // 예: 웹훅은 공개
    };

    // OWNER_VERIFICATION_REQUIRED에 추가
    public static final String[] OWNER_VERIFICATION_REQUIRED = {
        // ... 기존 경로 ...
        ApiPaths.Payments.BY_ID,
        ApiPaths.Payments.REFUND,
    };
}
```

### Step 3: Controller 작성

```java
@RestController
@RequestMapping(ApiPaths.Payments.BASE)  // ✅
public class PaymentCommandController {

    @PostMapping("/{id}/process")
    @PreAuthorize("@paymentSecurityChecker.isOwner(#id, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<PaymentApiResponse>> processPayment(
            @PathVariable Long id) {
        // ...
    }
}
```

---

## 8️⃣ 버전 관리 전략

### 다중 API 버전 지원

```java
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";
    public static final String API_V2 = "/api/v2";

    /**
     * Order V1 - 기존 API
     */
    public static final class Orders {
        public static final String BASE = API_V1 + "/orders";
        public static final String BY_ID = BASE + "/{id}";
        // ...
        private Orders() {}
    }

    /**
     * Order V2 - 새 API (Breaking Changes)
     */
    public static final class OrdersV2 {
        public static final String BASE = API_V2 + "/orders";
        public static final String BY_ID = BASE + "/{id}";
        // 새로운 구조...
        private OrdersV2() {}
    }
}
```

### Controller 버전 분리

```java
// V1 Controller
@RestController
@RequestMapping(ApiPaths.Orders.BASE)
public class OrderQueryController { /* ... */ }

// V2 Controller (새 버전)
@RestController
@RequestMapping(ApiPaths.OrdersV2.BASE)
public class OrderQueryControllerV2 { /* ... */ }
```

---

## 9️⃣ Do / Don't

### ✅ Good Patterns

```java
// ✅ 1. Constants 참조
@RequestMapping(ApiPaths.Orders.BASE)

// ✅ 2. SecurityPaths로 그룹화
.requestMatchers(SecurityPaths.PUBLIC_ENDPOINTS).permitAll()

// ✅ 3. 테스트에서 Constants 사용
mockMvc.perform(get(ApiPaths.Orders.BY_ID, orderId))

// ✅ 4. Nested Class로 BC별 그룹화
public static final class Orders {
    public static final String BASE = API_V1 + "/orders";
}

// ✅ 5. private 생성자
private ApiPaths() {
    throw new UnsupportedOperationException("Utility class");
}
```

### ❌ Anti-Patterns

```java
// ❌ 1. 하드코딩
@RequestMapping("/api/v1/orders")

// ❌ 2. SpEL 플레이스홀더 (런타임 바인딩)
@RequestMapping("${api.endpoints.order.base}")

// ❌ 3. 경로 조합 (문자열 연결)
@GetMapping(ApiPaths.Orders.BY_ID + "/cancel")  // ❌ CANCEL 상수 사용해야 함

// ❌ 4. Flat 구조 (그룹화 없음)
public static final String ORDER_BASE = "/api/v1/orders";
public static final String PRODUCT_BASE = "/api/v1/products";

// ❌ 5. public 생성자 허용
public class ApiPaths {  // ❌ final 누락
    // 생성자 없음 - 인스턴스화 가능
}

// ❌ 6. 상대 경로를 Constants로
public static final String BY_ID = "/{id}";  // ❌ 베이스 경로 포함해야 함
```

---

## 🔟 체크리스트

- [ ] `ApiPaths` 클래스가 `final`로 선언됨
- [ ] private 생성자에서 `UnsupportedOperationException` throw
- [ ] Bounded Context별 Nested Class로 그룹화
- [ ] 모든 경로가 `static final` 상수로 정의됨
- [ ] 경로는 베이스 경로를 포함한 전체 경로로 정의
- [ ] `SecurityPaths`에서 보안 정책별 그룹화
- [ ] Controller의 `@RequestMapping`에서 Constants 참조
- [ ] SecurityConfig에서 `SecurityPaths` 참조
- [ ] 테스트에서 Constants 사용
- [ ] 새 BC 추가 시 ApiPaths + SecurityPaths 함께 업데이트

---

## 📚 관련 가이드

- **[Security Guide](./security-guide.md)** - 전체 보안 아키텍처
- **[Security ArchUnit](./security-archunit.md)** - ArchUnit 테스트
- **[Controller Guide](../controller/controller-guide.md)** - Controller 작성 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
