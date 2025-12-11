# Gateway Only 인증 아키텍처

> **목적**: Gateway에서 JWT 검증, 서비스는 헤더만 읽는 마이크로서비스 인증 패턴
>
> **적용 대상**: 마이크로서비스 아키텍처, API Gateway 사용 환경

---

## 1️⃣ 아키텍처 개요

### 기존 패턴 vs Gateway Only 패턴

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [Before] 각 서비스가 JWT 직접 검증                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Client → JWT → Service A (JWT 검증 + Secret)                              │
│                → Service B (JWT 검증 + Secret)                              │
│                → Service C (JWT 검증 + Secret)                              │
│                                                                             │
│   ⚠️ 문제점:                                                                │
│   - 모든 서비스에 JWT Secret 배포 필요                                       │
│   - Secret 로테이션 시 전체 서비스 재배포                                    │
│   - 각 서비스마다 JWT 검증 로직 중복                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  [After] Gateway Only - Gateway가 JWT 검증, 서비스는 헤더만 읽음              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Client → JWT → Gateway (JWT 검증) → X-User-Id, X-User-Roles → Service A   │
│                                                             → Service B    │
│                                                             → Service C    │
│                                                                             │
│   ✅ 장점:                                                                  │
│   - JWT Secret은 Gateway만 보유                                             │
│   - Secret 로테이션 시 Gateway만 재배포                                      │
│   - 서비스는 단순히 헤더만 읽으면 됨                                         │
│   - 인증 로직 중앙 집중화                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 전체 흐름

```
┌─────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ Client  │      │ Auth Server │      │ API Gateway │      │  Services   │
└────┬────┘      └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
     │                   │                    │                    │
     │ 1. Login          │                    │                    │
     │──────────────────>│                    │                    │
     │                   │                    │                    │
     │ 2. JWT (RS256)    │                    │                    │
     │<──────────────────│                    │                    │
     │                   │                    │                    │
     │ 3. API Request + JWT                   │                    │
     │───────────────────────────────────────>│                    │
     │                   │                    │                    │
     │                   │  4. JWK Set 요청   │                    │
     │                   │<───────────────────│                    │
     │                   │    (Public Key)    │                    │
     │                   │───────────────────>│                    │
     │                   │                    │                    │
     │                   │                    │ 5. JWT 검증 (Public Key)
     │                   │                    │    + 헤더 추가        │
     │                   │                    │──────────────────────>│
     │                   │                    │  X-User-Id: 019...   │
     │                   │                    │  X-User-Roles: ADMIN │
     │                   │                    │                      │
     │                   │                    │ 6. Response           │
     │<───────────────────────────────────────────────────────────────│
     │                   │                    │                      │
```

---

## 2️⃣ JWT 서명 방식

### 비대칭 키 (RS256) 권장

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Auth Server                                                                │
│  ┌─────────────────┐                                                        │
│  │ Private Key     │ ← JWT 서명 (이 키만 Auth Server 보유)                    │
│  │ (절대 노출 금지) │                                                        │
│  └─────────────────┘                                                        │
│           │                                                                 │
│           ↓                                                                 │
│  ┌─────────────────┐                                                        │
│  │ JWK Set Endpoint│ ← 공개 키 배포 (/.well-known/jwks.json)                 │
│  │ (Public Key)    │                                                        │
│  └─────────────────┘                                                        │
│           │                                                                 │
│           ↓                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│  API Gateway                                                                │
│  ┌─────────────────┐                                                        │
│  │ Public Key Cache│ ← JWK Set에서 공개 키 가져와 캐싱                        │
│  │ (검증용)        │                                                        │
│  └─────────────────┘                                                        │
│           │                                                                 │
│           ↓                                                                 │
│  JWT 서명 검증 → 유효하면 헤더 추가 → 서비스로 전달                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 대칭 키 vs 비대칭 키 비교

| 항목 | 대칭 키 (HS256) | 비대칭 키 (RS256) |
|------|-----------------|-------------------|
| **키 관리** | 모든 서비스에 동일 Secret 배포 | Auth Server만 Private Key 보유 |
| **보안 위험** | Secret 노출 시 전체 시스템 취약 | Public Key 노출해도 안전 |
| **확장성** | 서비스 추가 시 Secret 배포 필요 | JWK Set URL만 알면 됨 |
| **권장 환경** | 단일 서비스, 개발 환경 | 마이크로서비스, 운영 환경 |

---

## 3️⃣ 컴포넌트 구조

### Gateway Only 패키지 구조

```
adapter-in/rest-api/src/main/java/com/company/adapter/in/rest/
├── auth/
│   ├── config/
│   │   ├── SecurityConfig.java           # Spring Security 설정
│   │   └── SecurityProperties.java       # 보안 설정 바인딩
│   ├── paths/
│   │   ├── ApiPaths.java                 # API 경로 상수
│   │   └── SecurityPaths.java            # 보안 정책별 경로 그룹
│   ├── filter/
│   │   ├── GatewayHeaderAuthFilter.java  # Gateway 헤더 인증 필터 ⭐
│   │   └── MdcLoggingFilter.java         # MDC 로깅 필터
│   ├── component/
│   │   ├── GatewayUserResolver.java      # 헤더에서 사용자 정보 추출 ⭐
│   │   ├── SecurityContextAuthenticator.java
│   │   └── MdcContextHolder.java
│   └── handler/
│       └── AuthenticationErrorHandler.java
└── ...
```

### 컴포넌트 역할 (Gateway Only)

| 컴포넌트 | 책임 | 기존 대비 변경 |
|----------|------|----------------|
| **GatewayHeaderAuthFilter** | Gateway 헤더에서 사용자 정보 읽기 | `JwtAuthenticationFilter` 대체 |
| **GatewayUserResolver** | X-User-Id, X-User-Roles 파싱 | 신규 컴포넌트 |
| **SecurityContextAuthenticator** | SecurityContext에 인증 정보 설정 | 유지 |
| **SecurityProperties** | gateway.enabled, 헤더명 설정 바인딩 | gateway 섹션 추가 |

---

## 4️⃣ 구현 가이드

### 4.1 설정 파일 (YAML)

#### 공통 설정 (rest-api.yml)

```yaml
# Gateway 인증 헤더 정의 (공통)
security:
  gateway:
    enabled: true                    # Gateway 인증 사용 여부
    user-id-header: X-User-Id        # 사용자 ID 헤더명
    user-roles-header: X-User-Roles  # 역할 헤더명

  cookie:
    domain: ${COOKIE_DOMAIN:localhost}
    secure: ${COOKIE_SECURE:false}
    same-site: lax
```

#### 로컬 환경 (rest-api-local.yml)

```yaml
security:
  gateway:
    enabled: false   # ⚠️ 로컬에서는 Gateway 우회 허용

  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
```

#### 운영 환경 (rest-api-prod.yml)

```yaml
security:
  gateway:
    enabled: true    # ✅ 운영에서는 Gateway 필수

  cookie:
    domain: ${COOKIE_DOMAIN}
    secure: true
    same-site: strict
```

### 4.2 SecurityProperties

```java
package com.company.adapter.in.rest.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security 설정 바인딩.
 *
 * <p>Gateway Only 아키텍처 설정을 포함합니다.
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
    GatewayProperties gateway,
    CookieProperties cookie
) {

    /**
     * Gateway 인증 설정.
     */
    public record GatewayProperties(
        boolean enabled,
        String userIdHeader,
        String userRolesHeader
    ) {
        public GatewayProperties {
            // 기본값 설정
            if (userIdHeader == null) userIdHeader = "X-User-Id";
            if (userRolesHeader == null) userRolesHeader = "X-User-Roles";
        }
    }

    /**
     * Cookie 설정.
     */
    public record CookieProperties(
        String domain,
        boolean secure,
        String sameSite
    ) {}
}
```

### 4.3 GatewayHeaderAuthFilter

```java
package com.company.adapter.in.rest.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gateway 헤더 인증 필터.
 *
 * <p>Gateway가 전달한 X-User-Id, X-User-Roles 헤더를 읽어
 * SecurityContext에 인증 정보를 설정합니다.
 *
 * <p><strong>Gateway Only 아키텍처</strong>에서 사용됩니다.
 * JWT 검증은 Gateway에서 수행되므로, 이 필터는 헤더만 읽습니다.
 *
 * @see SecurityConfig
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final GatewayUserResolver gatewayUserResolver;
    private final SecurityContextAuthenticator securityContextAuthenticator;

    public GatewayHeaderAuthFilter(
            SecurityProperties securityProperties,
            GatewayUserResolver gatewayUserResolver,
            SecurityContextAuthenticator securityContextAuthenticator) {
        this.securityProperties = securityProperties;
        this.gatewayUserResolver = gatewayUserResolver;
        this.securityContextAuthenticator = securityContextAuthenticator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!securityProperties.gateway().enabled()) {
            // Gateway 비활성화 시 (로컬 개발)
            filterChain.doFilter(request, response);
            return;
        }

        // Gateway 헤더에서 사용자 정보 추출
        GatewayUser gatewayUser = gatewayUserResolver.resolve(request);

        if (gatewayUser != null) {
            // SecurityContext에 인증 정보 설정
            securityContextAuthenticator.authenticate(gatewayUser);
        }

        filterChain.doFilter(request, response);
    }
}
```

### 4.4 GatewayUserResolver

```java
package com.company.adapter.in.rest.auth.component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Gateway 헤더에서 사용자 정보를 추출합니다.
 *
 * <p>X-User-Id, X-User-Roles 헤더를 파싱하여 {@link GatewayUser}를 생성합니다.
 */
@Component
public class GatewayUserResolver {

    private final SecurityProperties securityProperties;

    public GatewayUserResolver(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 요청에서 Gateway 사용자 정보를 추출합니다.
     *
     * @param request HTTP 요청
     * @return GatewayUser 또는 null (헤더 없는 경우)
     */
    public GatewayUser resolve(HttpServletRequest request) {
        String userIdHeader = securityProperties.gateway().userIdHeader();
        String userRolesHeader = securityProperties.gateway().userRolesHeader();

        String userId = request.getHeader(userIdHeader);
        String rolesHeader = request.getHeader(userRolesHeader);

        if (userId == null || userId.isBlank()) {
            return null;
        }

        List<String> roles = parseRoles(rolesHeader);

        UUID userUUID = parseUserId(userId);
        if (userUUID == null) {
            return null;
        }

        return new GatewayUser(userUUID, roles);
    }

    /**
     * 사용자 ID를 UUID로 파싱합니다.
     *
     * @param userIdValue 헤더에서 추출한 사용자 ID 문자열
     * @return UUID 또는 null (파싱 실패 시)
     */
    private UUID parseUserId(String userIdValue) {
        try {
            return UUID.fromString(userIdValue.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Collections.emptyList();
        }
        // "ROLE_ADMIN,ROLE_USER" 형태의 문자열 파싱
        return Arrays.asList(rolesHeader.split(","));
    }
}
```

### 4.5 GatewayUser (VO)

```java
package com.company.adapter.in.rest.auth.component;

import java.util.List;
import java.util.UUID;

/**
 * Gateway에서 전달된 사용자 정보.
 *
 * <p>UUIDv7 형식의 사용자 ID와 역할 목록을 포함합니다.
 *
 * @param userId 사용자 ID (UUIDv7)
 * @param roles 역할 목록
 */
public record GatewayUser(
    UUID userId,
    List<String> roles
) {
    /**
     * 특정 역할을 가지고 있는지 확인합니다.
     *
     * @param role 확인할 역할
     * @return 역할 보유 여부
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * 관리자 역할을 가지고 있는지 확인합니다.
     *
     * @return 관리자 여부
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
}
```

### 4.6 SecurityConfig (Gateway Only)

```java
package com.company.adapter.in.rest.auth.config;

import com.company.adapter.in.rest.auth.filter.GatewayHeaderAuthFilter;
import com.company.adapter.in.rest.auth.handler.AuthenticationErrorHandler;
import com.company.adapter.in.rest.auth.paths.SecurityPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 (Gateway Only 아키텍처).
 *
 * <p>JWT 검증은 Gateway에서 수행되므로, 이 설정에서는
 * Gateway 헤더 인증 필터만 등록합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;
    private final AuthenticationErrorHandler authenticationErrorHandler;

    public SecurityConfig(
            GatewayHeaderAuthFilter gatewayHeaderAuthFilter,
            AuthenticationErrorHandler authenticationErrorHandler) {
        this.gatewayHeaderAuthFilter = gatewayHeaderAuthFilter;
        this.authenticationErrorHandler = authenticationErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Gateway 헤더 인증 필터 등록
            .addFilterBefore(gatewayHeaderAuthFilter,
                UsernamePasswordAuthenticationFilter.class)

            // 인증 에러 핸들러
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationErrorHandler)
                .accessDeniedHandler(authenticationErrorHandler))

            // 엔드포인트 권한 설정
            .authorizeHttpRequests(auth -> {
                // Public 엔드포인트
                SecurityPaths.PUBLIC_ENDPOINTS.forEach(endpoint ->
                    auth.requestMatchers(endpoint.method(), endpoint.pattern()).permitAll()
                );

                // Admin 전용 엔드포인트
                SecurityPaths.ADMIN_ENDPOINTS.forEach(endpoint ->
                    auth.requestMatchers(endpoint.method(), endpoint.pattern())
                        .hasRole("ADMIN")
                );

                // 나머지는 인증 필요
                auth.anyRequest().authenticated();
            })

            .build();
    }
}
```

---

## 5️⃣ 보안 고려사항

### 5.1 네트워크 격리

```
┌─────────────────────────────────────────────────────────────────┐
│  운영 환경 네트워크 구성                                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Internet] ←──── HTTPS ────→ [Gateway]                         │
│                                    │                            │
│                              (내부망 only)                       │
│                                    │                            │
│                                    ↓                            │
│  [Service A] ←── HTTP ──→ [Service B] ←── HTTP ──→ [Service C]  │
│                                                                 │
│  ⚠️ 서비스들은 Gateway 외부에서 직접 접근 불가                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 헤더 스푸핑 방지

```java
// Gateway에서 설정 (예: Kong, Nginx)
// 클라이언트가 보낸 X-User-Id, X-User-Roles 헤더 제거 후 재설정
proxy_set_header X-User-Id "";
proxy_set_header X-User-Roles "";

// JWT 검증 후 헤더 설정
proxy_set_header X-User-Id $jwt_user_id;
proxy_set_header X-User-Roles $jwt_roles;
```

### 5.3 로컬 개발 시 보안

```yaml
# rest-api-local.yml
security:
  gateway:
    enabled: false  # Gateway 우회

# ⚠️ 로컬 개발 시 주의사항:
# - 운영 데이터베이스 직접 연결 금지
# - 운영 API 키 사용 금지
# - 민감 정보 하드코딩 금지
```

---

## 6️⃣ ArchUnit 검증 규칙

### Gateway Only 아키텍처 검증

```java
/**
 * Gateway Only 아키텍처 검증 규칙.
 */
@Nested
@DisplayName("Gateway Only Architecture Rules")
class GatewayOnlyArchitectureRules {

    @Test
    @DisplayName("[필수] Gateway 헤더 인증 필터가 존재해야 한다")
    void gatewayHeaderAuthFilter_MustExist() {
        ArchRule rule = classes()
            .that().haveSimpleNameContaining("GatewayHeaderAuthFilter")
            .should().beAssignableTo(OncePerRequestFilter.class)
            .because("Gateway Only 아키텍처에서는 GatewayHeaderAuthFilter가 필수입니다");

        rule.allowEmptyShould(true).check(classes);
    }

    @Test
    @DisplayName("[필수] GatewayUserResolver가 auth.component 패키지에 존재해야 한다")
    void gatewayUserResolver_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleName("GatewayUserResolver")
            .should().resideInAPackage("..auth.component..")
            .because("GatewayUserResolver는 auth.component 패키지에 위치해야 합니다");

        rule.allowEmptyShould(true).check(classes);
    }

    @Test
    @DisplayName("[금지] 서비스는 JWT Secret을 직접 참조하지 않아야 한다")
    void services_MustNotReferenceJwtSecret() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..adapter.in.rest..")
            .should().accessClassesThat()
            .haveSimpleNameContaining("JwtSecret")
            .because("Gateway Only 아키텍처에서 서비스는 JWT Secret을 직접 사용하면 안 됩니다");

        rule.allowEmptyShould(true).check(classes);
    }
}
```

---

## 7️⃣ 마이그레이션 가이드

### 기존 JWT-per-Service → Gateway Only 전환

| 단계 | 작업 | 확인 사항 |
|------|------|----------|
| 1 | Gateway JWT 검증 설정 | JWK Set URL 연동 확인 |
| 2 | Gateway 헤더 추가 설정 | X-User-Id, X-User-Roles 전달 확인 |
| 3 | `JwtAuthenticationFilter` → `GatewayHeaderAuthFilter` | 기존 필터 제거/교체 |
| 4 | JWT 관련 의존성 제거 | `jjwt`, `nimbus-jose-jwt` 등 |
| 5 | 설정 파일 업데이트 | `security.gateway.enabled: true` |
| 6 | 네트워크 정책 적용 | Gateway 외 직접 접근 차단 |

### 롤백 계획

```yaml
# 긴급 롤백 시
security:
  gateway:
    enabled: false  # Gateway 모드 비활성화
  jwt:
    enabled: true   # 기존 JWT 검증 활성화
```

---

## 8️⃣ 관련 문서

| 문서 | 설명 |
|------|------|
| [security-guide.md](./security-guide.md) | 전체 보안 아키텍처 가이드 |
| [api-paths-guide.md](./api-paths-guide.md) | API 경로 상수 패턴 |
| [security-archunit.md](./security-archunit.md) | ArchUnit 테스트 가이드 |

---

## 변경 이력

| 날짜 | 버전 | 내용 |
|------|------|------|
| 2025-12-08 | 1.1.0 | GatewayUser userId를 Long에서 UUID로 변경 |
| 2025-12-08 | 1.0.0 | Gateway Only 아키텍처 가이드 초안 작성 |
