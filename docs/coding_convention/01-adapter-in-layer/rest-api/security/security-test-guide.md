# Security Test Guide — **보안 컴포넌트 테스트 전략**

> **목적**: Security 관련 컴포넌트의 테스트 작성 가이드
>
> **철학**: TDD 기반, 보안 시나리오 완전 검증, 업계 표준 준수

---

## 1️⃣ 테스트 범위

### 컴포넌트별 테스트 유형

| 컴포넌트 | 단위 테스트 | 통합 테스트 | 테스트 대상 |
|----------|------------|------------|-------------|
| **ApiPaths** | ✅ | - | 경로 상수 값 검증 |
| **SecurityPaths** | ✅ | - | 배열 포함 관계 검증 |
| **JwtAuthenticationFilter** | ✅ | ✅ | 토큰 검증, Silent Refresh |
| **AuthenticationErrorHandler** | ✅ | - | RFC 7807 응답 형식 |
| **TokenCookieWriter** | ✅ | - | 쿠키 속성 검증 |
| **SecurityConfig** | - | ✅ | 인가 규칙 동작 |
| **Method Security** | - | ✅ | @PreAuthorize 동작 |

---

## 2️⃣ ApiPaths 테스트

### 경로 상수 값 검증

```java
package com.company.adapter.in.rest.auth.paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiPaths 단위 테스트
 *
 * <p>경로 상수 값이 올바르게 정의되었는지 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("ApiPaths 테스트")
class ApiPathsTest {

    @Test
    @DisplayName("API V1 베이스 경로는 /api/v1이어야 한다")
    void api_v1_base_path() {
        assertThat(ApiPaths.API_V1).isEqualTo("/api/v1");
    }

    @Nested
    @DisplayName("Members 경로")
    class MembersPathsTest {

        @Test
        @DisplayName("Members BASE 경로는 /api/v1/members이어야 한다")
        void members_base_path() {
            assertThat(ApiPaths.Members.BASE).isEqualTo("/api/v1/members");
        }

        @Test
        @DisplayName("Members BY_ID 경로는 PathVariable을 포함해야 한다")
        void members_by_id_contains_path_variable() {
            assertThat(ApiPaths.Members.BY_ID)
                .startsWith(ApiPaths.Members.BASE)
                .contains("{id}");
        }

        @Test
        @DisplayName("Members 경로들은 BASE 경로로 시작해야 한다")
        void members_paths_start_with_base() {
            assertThat(ApiPaths.Members.REGISTER).startsWith(ApiPaths.Members.BASE);
            assertThat(ApiPaths.Members.BY_ID).startsWith(ApiPaths.Members.BASE);
            assertThat(ApiPaths.Members.PASSWORD_RESET).startsWith(ApiPaths.Members.BASE);
        }
    }

    @Nested
    @DisplayName("Auth 경로")
    class AuthPathsTest {

        @Test
        @DisplayName("Auth BASE 경로는 /api/v1/auth이어야 한다")
        void auth_base_path() {
            assertThat(ApiPaths.Auth.BASE).isEqualTo("/api/v1/auth");
        }

        @Test
        @DisplayName("Auth 경로들은 BASE 경로로 시작해야 한다")
        void auth_paths_start_with_base() {
            assertThat(ApiPaths.Auth.LOGIN).startsWith(ApiPaths.Auth.BASE);
            assertThat(ApiPaths.Auth.LOGOUT).startsWith(ApiPaths.Auth.BASE);
            assertThat(ApiPaths.Auth.REFRESH).startsWith(ApiPaths.Auth.BASE);
        }
    }

    @Nested
    @DisplayName("Orders 경로")
    class OrdersPathsTest {

        @Test
        @DisplayName("Orders BASE 경로는 /api/v1/orders이어야 한다")
        void orders_base_path() {
            assertThat(ApiPaths.Orders.BASE).isEqualTo("/api/v1/orders");
        }

        @Test
        @DisplayName("Orders 액션 경로는 PathVariable과 액션을 포함해야 한다")
        void orders_action_paths() {
            assertThat(ApiPaths.Orders.CANCEL)
                .startsWith(ApiPaths.Orders.BASE)
                .contains("{id}")
                .endsWith("/cancel");

            assertThat(ApiPaths.Orders.CONFIRM)
                .startsWith(ApiPaths.Orders.BASE)
                .contains("{id}")
                .endsWith("/confirm");
        }
    }
}
```

---

## 3️⃣ SecurityPaths 테스트

### 보안 그룹 포함 관계 검증

```java
package com.company.adapter.in.rest.auth.paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityPaths 단위 테스트
 *
 * <p>보안 정책별 경로 그룹이 올바르게 정의되었는지 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("SecurityPaths 테스트")
class SecurityPathsTest {

    @Test
    @DisplayName("PUBLIC_ENDPOINTS에 로그인 경로가 포함되어야 한다")
    void public_endpoints_contains_login() {
        assertThat(SecurityPaths.PUBLIC_ENDPOINTS)
            .contains(ApiPaths.Auth.LOGIN);
    }

    @Test
    @DisplayName("PUBLIC_ENDPOINTS에 회원가입 경로가 포함되어야 한다")
    void public_endpoints_contains_register() {
        assertThat(SecurityPaths.PUBLIC_ENDPOINTS)
            .contains(ApiPaths.Members.REGISTER);
    }

    @Test
    @DisplayName("PUBLIC_ENDPOINTS에 토큰 갱신 경로가 포함되어야 한다")
    void public_endpoints_contains_refresh() {
        assertThat(SecurityPaths.PUBLIC_ENDPOINTS)
            .contains(ApiPaths.Auth.REFRESH);
    }

    @Test
    @DisplayName("PUBLIC_PATTERNS에 Swagger 경로가 포함되어야 한다")
    void public_patterns_contains_swagger() {
        assertThat(SecurityPaths.PUBLIC_PATTERNS)
            .anyMatch(pattern -> pattern.contains("swagger"));
    }

    @Test
    @DisplayName("PUBLIC_PATTERNS에 Actuator 경로가 포함되어야 한다")
    void public_patterns_contains_actuator() {
        assertThat(SecurityPaths.PUBLIC_PATTERNS)
            .anyMatch(pattern -> pattern.contains("actuator"));
    }

    @Test
    @DisplayName("ADMIN_PATTERNS에 /admin 패턴이 포함되어야 한다")
    void admin_patterns_contains_admin() {
        assertThat(SecurityPaths.ADMIN_PATTERNS)
            .anyMatch(pattern -> pattern.contains("/admin"));
    }

    @Test
    @DisplayName("OWNER_VERIFICATION_REQUIRED에 주문 취소 경로가 포함되어야 한다")
    void owner_verification_contains_order_cancel() {
        assertThat(SecurityPaths.OWNER_VERIFICATION_REQUIRED)
            .contains(ApiPaths.Orders.CANCEL);
    }

    @Test
    @DisplayName("PUBLIC_ENDPOINTS의 모든 경로는 /api로 시작해야 한다")
    void public_endpoints_start_with_api() {
        Arrays.stream(SecurityPaths.PUBLIC_ENDPOINTS)
            .forEach(path -> assertThat(path).startsWith("/api"));
    }
}
```

---

## 4️⃣ JwtAuthenticationFilter 테스트

### 단위 테스트 (Mock 기반)

```java
package com.company.adapter.in.rest.auth.filter;

import com.company.adapter.in.rest.auth.component.MdcContextHolder;
import com.company.adapter.in.rest.auth.component.SecurityContextAuthenticator;
import com.company.adapter.in.rest.auth.component.TokenCookieWriter;
import com.company.application.member.dto.response.TokenPairResponse;
import com.company.application.member.port.out.TokenProviderPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.BDDMockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProviderPort tokenProviderPort;

    @Mock
    private TokenCookieWriter tokenCookieWriter;

    @Mock
    private SecurityContextAuthenticator securityContextAuthenticator;

    @Mock
    private MdcContextHolder mdcContextHolder;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("Access Token 유효한 경우")
    class ValidAccessToken {

        @Test
        @DisplayName("인증 성공 후 다음 필터로 진행한다")
        void should_authenticate_and_continue() throws Exception {
            // Given
            String validToken = "valid.access.token";
            String memberId = "123";
            request.setCookies(new Cookie("access_token", validToken));

            given(tokenProviderPort.validateAccessToken(validToken)).willReturn(true);
            given(securityContextAuthenticator.authenticate(request, validToken))
                .willReturn(memberId);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(securityContextAuthenticator).should().authenticate(request, validToken);
            then(mdcContextHolder).should().setMemberId(memberId);
            then(filterChain).should().doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Access Token 만료된 경우")
    class ExpiredAccessToken {

        @Test
        @DisplayName("Silent Refresh 성공 시 새 토큰으로 인증한다")
        void should_silent_refresh_when_access_token_expired() throws Exception {
            // Given
            String expiredAccessToken = "expired.access.token";
            String validRefreshToken = "valid.refresh.token";
            String memberId = "123";
            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";

            request.setCookies(
                new Cookie("access_token", expiredAccessToken),
                new Cookie("refresh_token", validRefreshToken)
            );

            given(tokenProviderPort.validateAccessToken(expiredAccessToken)).willReturn(false);
            given(tokenProviderPort.isAccessTokenExpired(expiredAccessToken)).willReturn(true);
            given(tokenProviderPort.validateRefreshToken(validRefreshToken)).willReturn(true);
            given(tokenProviderPort.extractMemberIdFromRefreshToken(validRefreshToken))
                .willReturn(memberId);
            given(tokenProviderPort.generateTokenPair(memberId))
                .willReturn(new TokenPairResponse(newAccessToken, newRefreshToken, 3600, 604800));
            given(securityContextAuthenticator.authenticate(request, newAccessToken))
                .willReturn(memberId);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(tokenCookieWriter).should().addTokenCookies(
                eq(response), eq(newAccessToken), eq(newRefreshToken), anyLong(), anyLong());
            then(securityContextAuthenticator).should().authenticate(request, newAccessToken);
            then(mdcContextHolder).should().setMemberId(memberId);
            then(filterChain).should().doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 없는 경우")
    class NoToken {

        @Test
        @DisplayName("인증 없이 다음 필터로 진행한다")
        void should_continue_without_authentication() throws Exception {
            // Given
            // 토큰 없음

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(securityContextAuthenticator).shouldHaveNoInteractions();
            then(filterChain).should().doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Authorization 헤더 사용")
    class AuthorizationHeader {

        @Test
        @DisplayName("Bearer 토큰으로 인증 성공한다")
        void should_authenticate_with_bearer_token() throws Exception {
            // Given
            String validToken = "valid.access.token";
            String memberId = "123";
            request.addHeader("Authorization", "Bearer " + validToken);

            given(tokenProviderPort.validateAccessToken(validToken)).willReturn(true);
            given(securityContextAuthenticator.authenticate(request, validToken))
                .willReturn(memberId);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(securityContextAuthenticator).should().authenticate(request, validToken);
            then(filterChain).should().doFilter(request, response);
        }
    }
}
```

---

## 5️⃣ AuthenticationErrorHandler 테스트

### RFC 7807 응답 형식 검증

```java
package com.company.adapter.in.rest.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthenticationErrorHandler 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AuthenticationErrorHandler 테스트")
class AuthenticationErrorHandlerTest {

    private AuthenticationErrorHandler handler;
    private ObjectMapper objectMapper;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new AuthenticationErrorHandler(objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("인증 실패 (401)")
    class AuthenticationFailure {

        @Test
        @DisplayName("401 상태 코드를 반환한다")
        void should_return_401_status() throws Exception {
            // Given
            request.setRequestURI("/api/v1/orders");
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            // When
            handler.commence(request, response, exception);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("RFC 7807 ProblemDetail 형식으로 응답한다")
        void should_return_problem_detail_format() throws Exception {
            // Given
            request.setRequestURI("/api/v1/orders");
            BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

            // When
            handler.commence(request, response, exception);

            // Then
            assertThat(response.getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            ProblemDetail problemDetail = objectMapper.readValue(
                response.getContentAsString(), ProblemDetail.class);

            assertThat(problemDetail.getStatus()).isEqualTo(401);
            assertThat(problemDetail.getTitle()).isEqualTo("Unauthorized");
            assertThat(problemDetail.getDetail()).contains("인증");
            assertThat(problemDetail.getProperties()).containsKey("code");
            assertThat(problemDetail.getProperties()).containsKey("timestamp");
        }

        @Test
        @DisplayName("instance에 요청 경로가 포함된다")
        void should_include_request_uri_in_instance() throws Exception {
            // Given
            request.setRequestURI("/api/v1/orders");
            request.setQueryString("page=1&size=10");
            BadCredentialsException exception = new BadCredentialsException("Invalid");

            // When
            handler.commence(request, response, exception);

            // Then
            ProblemDetail problemDetail = objectMapper.readValue(
                response.getContentAsString(), ProblemDetail.class);

            assertThat(problemDetail.getInstance().toString())
                .contains("/api/v1/orders")
                .contains("page=1");
        }

        @Test
        @DisplayName("MDC의 requestId가 응답에 포함된다")
        void should_include_request_id_from_mdc() throws Exception {
            // Given
            String requestId = "test-request-id";
            MDC.put("requestId", requestId);
            request.setRequestURI("/api/v1/orders");
            BadCredentialsException exception = new BadCredentialsException("Invalid");

            try {
                // When
                handler.commence(request, response, exception);

                // Then
                ProblemDetail problemDetail = objectMapper.readValue(
                    response.getContentAsString(), ProblemDetail.class);

                assertThat(problemDetail.getProperties().get("requestId"))
                    .isEqualTo(requestId);
            } finally {
                MDC.clear();
            }
        }
    }

    @Nested
    @DisplayName("인가 실패 (403)")
    class AccessDenied {

        @Test
        @DisplayName("403 상태 코드를 반환한다")
        void should_return_403_status() throws Exception {
            // Given
            request.setRequestURI("/api/v1/admin/users");
            AccessDeniedException exception = new AccessDeniedException("Access denied");

            // When
            handler.handle(request, response, exception);

            // Then
            assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @DisplayName("RFC 7807 ProblemDetail 형식으로 응답한다")
        void should_return_problem_detail_format() throws Exception {
            // Given
            request.setRequestURI("/api/v1/admin/users");
            AccessDeniedException exception = new AccessDeniedException("Access denied");

            // When
            handler.handle(request, response, exception);

            // Then
            ProblemDetail problemDetail = objectMapper.readValue(
                response.getContentAsString(), ProblemDetail.class);

            assertThat(problemDetail.getStatus()).isEqualTo(403);
            assertThat(problemDetail.getTitle()).isEqualTo("Forbidden");
            assertThat(problemDetail.getDetail()).contains("권한");
            assertThat(problemDetail.getProperties().get("code")).isEqualTo("ACCESS_DENIED");
        }
    }
}
```

---

## 6️⃣ TokenCookieWriter 테스트

### 쿠키 속성 검증

```java
package com.company.adapter.in.rest.auth.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenCookieWriter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("TokenCookieWriter 테스트")
class TokenCookieWriterTest {

    private TokenCookieWriter tokenCookieWriter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // 테스트용 Properties 설정
        SecurityProperties properties = new SecurityProperties();
        properties.getCookie().setSecure(false);
        properties.getCookie().setDomain("localhost");
        properties.getCookie().setSameSite("lax");

        tokenCookieWriter = new TokenCookieWriter(properties);
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("Access Token 쿠키")
    class AccessTokenCookie {

        @Test
        @DisplayName("HttpOnly 속성이 설정된다")
        void should_set_httpOnly_attribute() {
            // When
            tokenCookieWriter.addAccessTokenCookie(response, "token", 3600);

            // Then
            String cookie = response.getHeader("Set-Cookie");
            assertThat(cookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("Max-Age가 올바르게 설정된다")
        void should_set_maxAge_attribute() {
            // When
            tokenCookieWriter.addAccessTokenCookie(response, "token", 3600);

            // Then
            String cookie = response.getHeader("Set-Cookie");
            assertThat(cookie).contains("Max-Age=3600");
        }

        @Test
        @DisplayName("SameSite 속성이 설정된다")
        void should_set_sameSite_attribute() {
            // When
            tokenCookieWriter.addAccessTokenCookie(response, "token", 3600);

            // Then
            String cookie = response.getHeader("Set-Cookie");
            assertThat(cookie).contains("SameSite=Lax");
        }

        @Test
        @DisplayName("쿠키 이름이 access_token이다")
        void should_use_correct_cookie_name() {
            // When
            tokenCookieWriter.addAccessTokenCookie(response, "my-token", 3600);

            // Then
            String cookie = response.getHeader("Set-Cookie");
            assertThat(cookie).startsWith("access_token=my-token");
        }
    }

    @Nested
    @DisplayName("토큰 쿠키 삭제")
    class DeleteCookies {

        @Test
        @DisplayName("Max-Age=0으로 설정하여 쿠키를 삭제한다")
        void should_set_maxAge_zero() {
            // When
            tokenCookieWriter.deleteTokenCookies(response);

            // Then
            String cookies = response.getHeaders("Set-Cookie").toString();
            assertThat(cookies).contains("Max-Age=0");
        }

        @Test
        @DisplayName("access_token과 refresh_token 모두 삭제한다")
        void should_delete_both_tokens() {
            // When
            tokenCookieWriter.deleteTokenCookies(response);

            // Then
            assertThat(response.getHeaders("Set-Cookie"))
                .hasSize(2)
                .anyMatch(cookie -> cookie.contains("access_token="))
                .anyMatch(cookie -> cookie.contains("refresh_token="));
        }
    }

    @Nested
    @DisplayName("Secure 환경")
    class SecureEnvironment {

        @Test
        @DisplayName("Secure=true 설정 시 Secure 속성이 추가된다")
        void should_add_secure_attribute_when_enabled() {
            // Given
            SecurityProperties secureProperties = new SecurityProperties();
            secureProperties.getCookie().setSecure(true);
            secureProperties.getCookie().setSameSite("strict");

            TokenCookieWriter secureWriter = new TokenCookieWriter(secureProperties);
            MockHttpServletResponse secureResponse = new MockHttpServletResponse();

            // When
            secureWriter.addAccessTokenCookie(secureResponse, "token", 3600);

            // Then
            String cookie = secureResponse.getHeader("Set-Cookie");
            assertThat(cookie).contains("Secure");
        }
    }
}
```

---

## 7️⃣ SecurityConfig 통합 테스트

### 인가 규칙 동작 검증

```java
package com.company.adapter.in.rest.auth.config;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig 통합 테스트
 *
 * <p>실제 HTTP 요청으로 인가 규칙을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SecurityConfig 통합 테스트")
class SecurityConfigIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("로그인 엔드포인트는 인증 없이 접근 가능하다")
        void login_endpoint_is_public() {
            // When
            ResponseEntity<String> response = restTemplate.postForEntity(
                ApiPaths.Auth.LOGIN,
                new LoginRequest("test@test.com", "password"),
                String.class
            );

            // Then
            // 401이 아닌 다른 상태 (400, 200 등) → 인증 없이 접근됨
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("회원가입 엔드포인트는 인증 없이 접근 가능하다")
        void register_endpoint_is_public() {
            // When
            ResponseEntity<String> response = restTemplate.postForEntity(
                ApiPaths.Members.REGISTER,
                new RegisterRequest("test@test.com", "password", "testuser"),
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Swagger UI는 인증 없이 접근 가능하다")
        void swagger_is_public() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                "/swagger-ui/index.html",
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Actuator health는 인증 없이 접근 가능하다")
        void actuator_health_is_public() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health",
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Protected Endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("주문 조회는 인증이 필요하다")
        void order_endpoint_requires_auth() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                ApiPaths.Orders.BASE,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("회원 정보 조회는 인증이 필요하다")
        void member_me_endpoint_requires_auth() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                ApiPaths.Members.ME,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Admin Endpoints")
    class AdminEndpoints {

        @Test
        @DisplayName("Admin 엔드포인트는 인증 없이 접근 불가하다")
        void admin_endpoint_requires_auth() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                ApiPaths.Admin.MEMBERS,
                String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // 인증된 사용자 + ROLE_USER → 403 테스트는 별도 설정 필요
    }
}
```

---

## 8️⃣ Method Security 테스트

### @PreAuthorize 동작 검증

```java
package com.company.adapter.in.rest.order.controller;

import com.company.adapter.in.rest.auth.paths.ApiPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Method Security 통합 테스트
 *
 * <p>리소스 소유자 검증이 올바르게 동작하는지 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Method Security 테스트")
class MethodSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("주문 소유자가 아닌 사용자의 주문 취소는 403을 반환한다")
    void non_owner_cancel_returns_403() {
        // Given
        String otherUserToken = getTokenForUser("other-user");
        Long orderId = 1L; // 다른 사용자의 주문

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(otherUserToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            ApiPaths.Orders.BASE + "/" + orderId + "/cancel",
            HttpMethod.PATCH,
            entity,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("주문 소유자의 주문 취소는 성공한다")
    void owner_cancel_succeeds() {
        // Given
        String ownerToken = getTokenForUser("owner");
        Long orderId = 1L; // owner의 주문

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ownerToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            ApiPaths.Orders.BASE + "/" + orderId + "/cancel",
            HttpMethod.PATCH,
            entity,
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
    }

    // 테스트용 토큰 발급 헬퍼 메서드
    private String getTokenForUser(String userId) {
        // 테스트 환경에서 토큰 발급
        return "test-token-for-" + userId;
    }
}
```

---

## 9️⃣ 체크리스트

### 테스트 작성 체크리스트

- [ ] ApiPaths 경로 상수 값 검증 테스트
- [ ] SecurityPaths 그룹 포함 관계 검증 테스트
- [ ] JwtAuthenticationFilter 단위 테스트 (Mock 기반)
  - [ ] 유효한 토큰 인증 성공
  - [ ] 만료된 토큰 Silent Refresh
  - [ ] 토큰 없는 경우 다음 필터 진행
  - [ ] Bearer 헤더 토큰 인증
- [ ] AuthenticationErrorHandler 단위 테스트
  - [ ] 401 응답 형식 (RFC 7807)
  - [ ] 403 응답 형식 (RFC 7807)
  - [ ] MDC 정보 포함
- [ ] TokenCookieWriter 단위 테스트
  - [ ] HttpOnly, SameSite, Secure 속성
  - [ ] 쿠키 삭제
- [ ] SecurityConfig 통합 테스트
  - [ ] Public Endpoints 접근
  - [ ] Protected Endpoints 인증 요구
  - [ ] Admin Endpoints 권한 요구
- [ ] Method Security 통합 테스트
  - [ ] 소유자 검증 성공/실패

---

## 📚 관련 가이드

- **[Security Guide](./security-guide.md)** - 전체 보안 아키텍처
- **[Security ArchUnit](./security-archunit.md)** - ArchUnit 테스트
- **[API Paths Guide](./api-paths-guide.md)** - Constants 방식 경로 관리

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
