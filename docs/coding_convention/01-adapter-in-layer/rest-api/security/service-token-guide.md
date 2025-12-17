# Service Token 인증 가이드

서버 간 내부 호출을 위한 Service Token 인증 메커니즘 구현 가이드입니다.

---

## 1. 개요

### 1.1 목적

마이크로서비스 아키텍처에서 서버 간 내부 통신 시 사용자 인증 없이 시스템 레벨 접근이 필요한 경우가 있습니다.

```
┌─────────────┐      X-Service-Token       ┌─────────────┐
│  Worker     │ ──────────────────────────▶│  Web API    │
│  Service    │    (SSM 공유 시크릿)        │  Service    │
└─────────────┘                            └─────────────┘
```

### 1.2 사용 사례

- **Worker → API 호출**: 파일 처리 완료 후 상태 업데이트
- **Scheduler → API 호출**: 배치 작업 실행
- **서비스 간 동기화**: 내부 데이터 조회/수정

### 1.3 보안 원칙

| 원칙 | 설명 |
|------|------|
| **최소 권한** | 시스템 호출에 필요한 최소 권한만 부여 |
| **비밀 관리** | SSM Parameter Store에서 중앙 관리 |
| **감사 로깅** | 모든 시스템 호출 MDC 로깅 |
| **환경 분리** | 환경별(dev/staging/prod) 별도 토큰 사용 |

---

## 2. 실제 구현 현황

### 2.1 토큰 정보

```bash
# 현재 사용 중인 Service Token (64자 hex)
SECURITY_SERVICE_TOKEN_SECRET=

# 토큰 생성 명령어
openssl rand -hex 32
```

### 2.2 SSM Parameter Store 경로

```bash
# 공유 시크릿 경로 (모든 서버가 동일한 값 참조)
/shared/security/service-token-secret
```

### 2.3 HTTP 헤더

```
X-Service-Token: sample
```

---

## 3. 아키텍처

### 3.1 인증 흐름

```
┌──────────────────────────────────────────────────────────────┐
│                    Service Token 인증 흐름                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. SSM Parameter Store                                      │
│     └─ /shared/security/service-token-secret                 │
│                         │                                    │
│                         ▼                                    │
│  2. Terraform (ECS Task Definition)                          │
│     └─ container_secrets:                                    │
│        SECURITY_SERVICE_TOKEN_SECRET                         │
│        SECURITY_SERVICE_TOKEN_ENABLED=true                   │
│                         │                                    │
│                         ▼                                    │
│  3. Spring Boot Application                                  │
│     └─ @ConfigurationProperties                              │
│        security.service-token.secret=${SECURITY_...}         │
│        security.service-token.enabled=${SECURITY_...}        │
│                         │                                    │
│                         ▼                                    │
│  4. UserContextFilter                                        │
│     └─ X-Service-Token 헤더 검증                             │
│        → 일치: SYSTEM UserContext 생성                       │
│        → 불일치: 다음 인증 방식으로 진행                      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 인증 우선순위

UserContextFilter는 다음 순서로 인증을 처리합니다:

```java
1. X-Service-Token 헤더 확인 (최우선)
   └─ 유효하면 → SYSTEM UserContext 생성 (Gateway 헤더 무시)

2. X-Tenant-Id 헤더 확인 (Gateway 경유)
   └─ 있으면 → Gateway 헤더 기반 UserContext

3. Authorization: Bearer {token} 확인 (JWT 직접)
   └─ 있으면 → JWT 파싱 → UserContext

4. 없으면 → 개발 모드 기본 Admin UserContext (개발 환경만)
```

---

## 4. 수정된 파일 목록

### 4.1 Domain Layer

#### 4.1.1 UserRole.java
**경로**: `domain/src/main/java/com/ryuqq/fileflow/domain/iam/vo/UserRole.java`

```java
public enum UserRole {
    /**
     * 시스템 내부 호출용 역할.
     *
     * <p>서버 간 통신에서 Service Token 인증 시 사용됩니다.
     * 최고 우선순위(ordinal 0)를 가지며, 모든 권한을 부여받습니다.
     */
    SYSTEM(0),

    /** 슈퍼 관리자 - 전체 시스템 관리 권한 */
    SUPER_ADMIN(1),

    /** 관리자 - 테넌트 내 관리 권한 */
    ADMIN(2),

    /** 판매자 - 조직 내 판매 관련 권한 */
    SELLER(3),

    /** 일반 사용자 - 기본 권한 */
    DEFAULT(4);

    // ... 기존 코드 ...
}
```

#### 4.1.2 Organization.java
**경로**: `domain/src/main/java/com/ryuqq/fileflow/domain/iam/vo/Organization.java`

```java
/**
 * 시스템 내부 호출용 Organization을 생성합니다.
 *
 * <p>서버 간 통신에서 Service Token 인증 시 사용됩니다.
 * 시스템 전용 조직으로, 내부 호출임을 명시적으로 표현합니다.
 *
 * @return 시스템 Organization
 */
public static Organization system() {
    return new Organization(null, "SYSTEM", "internal/system");
}

/**
 * 시스템 조직인지 확인합니다.
 *
 * @return 시스템 조직이면 true
 */
public boolean isSystem() {
    return "SYSTEM".equals(name);
}
```

#### 4.1.3 UserContext.java
**경로**: `domain/src/main/java/com/ryuqq/fileflow/domain/iam/vo/UserContext.java`

```java
/**
 * 시스템 내부 호출용 UserContext를 생성합니다.
 *
 * <p>서버 간 통신에서 Service Token 인증 시 사용됩니다.
 * Connectly 테넌트와 SYSTEM 조직으로 설정되며, 전체 권한을 부여받습니다.
 *
 * @return 시스템 UserContext
 */
public static UserContext system() {
    return new UserContext(
            Tenant.connectly(),
            Organization.system(),
            "system@internal",
            null,
            List.of("SYSTEM"),
            Collections.emptyList());
}

/**
 * 시스템 호출인지 확인합니다.
 *
 * @return 시스템 호출이면 true
 */
public boolean isSystem() {
    return organization.isSystem();
}
```

**추가 수정사항**:
- `validateRequiredFields()`: SYSTEM 역할은 email/userId 검증 생략
- `getUserIdentifier()`: SYSTEM일 경우 "SYSTEM" 반환
- `generateS3Key()`: SYSTEM일 경우 internal 경로 사용

---

### 4.2 REST API Layer

#### 4.2.1 ServiceTokenProperties.java (신규 생성)
**경로**: `adapter-in/rest-api/src/main/java/com/ryuqq/fileflow/adapter/in/rest/config/properties/ServiceTokenProperties.java`

```java
package com.ryuqq.fileflow.adapter.in.rest.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Service Token 인증 설정 Properties.
 *
 * <p>서버 간 내부 통신에서 사용되는 Service Token 인증을 위한 설정입니다.
 *
 * <p><strong>설정 예시 (rest-api.yml):</strong>
 *
 * <pre>{@code
 * security:
 *   service-token:
 *     enabled: true
 *     secret: ${SECURITY_SERVICE_TOKEN_SECRET:}
 * }</pre>
 *
 * <p><strong>환경변수 설정:</strong>
 *
 * <ul>
 *   <li>SECURITY_SERVICE_TOKEN_ENABLED: 활성화 여부 (true/false)
 *   <li>SECURITY_SERVICE_TOKEN_SECRET: SSM Parameter Store에서 주입되는 공유 비밀키
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "security.service-token")
public class ServiceTokenProperties {

    private boolean enabled = false;
    private String secret;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Service Token 인증이 사용 가능한 상태인지 확인합니다.
     *
     * @return 활성화되어 있고, 비밀키가 설정되어 있으면 true
     */
    public boolean isAvailable() {
        return enabled && secret != null && !secret.isBlank();
    }

    /**
     * 주어진 토큰이 유효한 Service Token인지 검증합니다.
     *
     * @param token 검증할 토큰
     * @return 유효하면 true
     */
    public boolean isValidToken(String token) {
        return isAvailable() && secret.equals(token);
    }
}
```

#### 4.2.2 SecurityPaths.java
**경로**: `adapter-in/rest-api/src/main/java/com/ryuqq/fileflow/adapter/in/rest/auth/paths/SecurityPaths.java`

```java
public static final class Headers {
    // 기존 헤더들...
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String ORGANIZATION_ID = "X-Organization-Id";
    public static final String PERMISSIONS = "X-Permissions";

    /**
     * Service Token 헤더 - 서버 간 내부 통신 인증용.
     *
     * <p>이 헤더에 유효한 Service Token이 포함되어 있으면
     * SYSTEM UserContext로 인증됩니다.
     */
    public static final String SERVICE_TOKEN = "X-Service-Token";

    private Headers() {}
}
```

#### 4.2.3 UserContextFilter.java
**경로**: `adapter-in/rest-api/src/main/java/com/ryuqq/fileflow/adapter/in/rest/common/filter/UserContextFilter.java`

**추가된 필드 및 생성자**:
```java
private static final String HEADER_SERVICE_TOKEN = SecurityPaths.Headers.SERVICE_TOKEN;

private final ObjectMapper objectMapper;
private final ServiceTokenProperties serviceTokenProperties;

public UserContextFilter(ObjectMapper objectMapper, ServiceTokenProperties serviceTokenProperties) {
    this.objectMapper = objectMapper;
    this.serviceTokenProperties = serviceTokenProperties;
}
```

**수정된 doFilterInternal 메서드**:
```java
@Override
protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    try {
        UserContext userContext;

        // 1. Service Token 확인 (서버 간 내부 통신) - 최우선
        String serviceToken = request.getHeader(HEADER_SERVICE_TOKEN);
        if (serviceTokenProperties.isValidToken(serviceToken)) {
            userContext = createSystemContext();
            log.debug("Service Token 인증 성공 - SYSTEM UserContext 사용");
        }
        // 2. Gateway 헤더 확인
        else {
            String tenantIdHeader = request.getHeader(HEADER_TENANT_ID);

            if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
                userContext = createUserContextFromGatewayHeaders(request);
            }
            // 3. JWT Fallback
            else {
                userContext = createUserContextFromJwt(request, response);
                if (userContext == null) {
                    return; // 에러 응답 이미 전송됨
                }
            }
        }

        // UserContext 설정 및 필터 체인 진행
        UserContextHolder.set(userContext);
        setMdcFromUserContext(userContext);
        synchronizeWithSpringSecurityContext(userContext);

        filterChain.doFilter(request, response);

    } finally {
        UserContextHolder.clear();
        SecurityContextHolder.clearContext();
        clearMdc();
    }
}

/**
 * 시스템 내부 호출용 UserContext를 생성합니다.
 */
private UserContext createSystemContext() {
    return UserContext.system();
}
```

**createOrganization 메서드 수정**:
```java
private Organization createOrganization(UserRole role, OrganizationId orgId, String orgName) {
    return switch (role) {
        case SYSTEM -> Organization.system();
        case SUPER_ADMIN, ADMIN -> Organization.admin();
        case SELLER -> Organization.of(orgId, orgName);
        case DEFAULT -> Organization.none();
    };
}
```

#### 4.2.4 FilterConfig.java
**경로**: `adapter-in/rest-api/src/main/java/com/ryuqq/fileflow/adapter/in/rest/config/FilterConfig.java`

```java
@Configuration
public class FilterConfig {

    private final ObjectMapper objectMapper;
    private final ServiceTokenProperties serviceTokenProperties;

    public FilterConfig(ObjectMapper objectMapper, ServiceTokenProperties serviceTokenProperties) {
        this.objectMapper = objectMapper;
        this.serviceTokenProperties = serviceTokenProperties;
    }

    @Bean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter(objectMapper, serviceTokenProperties);
    }

    // ... 기존 코드 ...
}
```

---

### 4.3 Configuration

#### 4.3.1 rest-api.yml
**경로**: `adapter-in/rest-api/src/main/resources/rest-api.yml`

```yaml
# Gateway Security Settings
security:
  gateway:
    user-id-header: X-User-Id
    user-roles-header: X-User-Roles
    tenant-id-header: X-Tenant-Id
    organization-id-header: X-Organization-Id

  # Service Token 인증 설정 (서버 간 내부 통신용)
  # SSM Parameter Store에서 환경변수로 주입됨
  service-token:
    enabled: ${SECURITY_SERVICE_TOKEN_ENABLED:false}
    secret: ${SECURITY_SERVICE_TOKEN_SECRET:}
```

---

### 4.4 Terraform (Infrastructure)

#### 4.4.1 terraform/ecs-web-api/main.tf

**SSM Parameter 데이터 소스 추가**:
```hcl
# Service Token Secret (for internal service communication)
data "aws_ssm_parameter" "service_token_secret" {
  name = "/shared/security/service-token-secret"
}
```

**ECS 환경변수 추가**:
```hcl
module "ecs_service" {
  # ... 기존 설정 ...

  # Container Environment Variables
  container_environment = [
    { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
    { name = "DB_HOST", value = local.rds_host },
    { name = "DB_PORT", value = local.rds_port },
    { name = "DB_NAME", value = local.rds_dbname },
    { name = "DB_USER", value = local.rds_username },
    { name = "REDIS_HOST", value = local.redis_host },
    { name = "REDIS_PORT", value = tostring(local.redis_port) },
    # Service Token 인증 활성화 (서버 간 내부 통신용)
    { name = "SECURITY_SERVICE_TOKEN_ENABLED", value = "true" }
  ]

  # Container Secrets
  container_secrets = [
    { name = "DB_PASSWORD", valueFrom = "${data.aws_secretsmanager_secret.rds.arn}:password::" },
    # Service Token Secret (서버 간 내부 통신 인증용)
    { name = "SECURITY_SERVICE_TOKEN_SECRET", valueFrom = data.aws_ssm_parameter.service_token_secret.arn }
  ]
}
```

---

### 4.5 테스트

#### 4.5.1 UserContextFilterTest.java
**경로**: `adapter-in/rest-api/src/test/java/com/ryuqq/fileflow/adapter/in/rest/common/filter/UserContextFilterTest.java`

**추가된 테스트 케이스**:
```java
@Nested
@DisplayName("Service Token 인증 (서버 간 내부 통신)")
class ServiceTokenAuthentication {

    private static final String VALID_SERVICE_TOKEN = "test-service-token-secret";

    @Test
    @DisplayName("유효한 Service Token으로 SYSTEM UserContext 생성")
    void shouldCreateSystemContextFromValidServiceToken() throws Exception {
        // given
        when(request.getHeader("X-Service-Token")).thenReturn(VALID_SERVICE_TOKEN);
        when(serviceTokenProperties.isValidToken(VALID_SERVICE_TOKEN)).thenReturn(true);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        // Service Token이 유효하면 Gateway 헤더는 체크하지 않음
        verify(request, never()).getHeader("X-Tenant-Id");
    }

    @Test
    @DisplayName("Service Token 비활성화 시 일반 플로우로 진행")
    void shouldFallbackToNormalFlowWhenServiceTokenDisabled() throws Exception {
        // given
        when(request.getHeader("X-Service-Token")).thenReturn(VALID_SERVICE_TOKEN);
        when(serviceTokenProperties.isValidToken(VALID_SERVICE_TOKEN)).thenReturn(false);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Tenant-Id");
    }

    @Test
    @DisplayName("잘못된 Service Token은 일반 플로우로 진행")
    void shouldFallbackToNormalFlowWhenInvalidServiceToken() throws Exception {
        // given
        String invalidToken = "wrong-token";
        when(request.getHeader("X-Service-Token")).thenReturn(invalidToken);
        when(serviceTokenProperties.isValidToken(invalidToken)).thenReturn(false);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Tenant-Id");
    }

    @Test
    @DisplayName("Service Token 헤더가 없으면 일반 플로우로 진행")
    void shouldFallbackToNormalFlowWhenNoServiceTokenHeader() throws Exception {
        // given
        when(request.getHeader("X-Service-Token")).thenReturn(null);
        when(serviceTokenProperties.isValidToken(null)).thenReturn(false);
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(request).getHeader("X-Tenant-Id");
    }

    @Test
    @DisplayName("Service Token이 Gateway 헤더보다 우선순위가 높음")
    void serviceTokenShouldTakePriorityOverGatewayHeaders() throws Exception {
        // given
        when(request.getHeader("X-Service-Token")).thenReturn(VALID_SERVICE_TOKEN);
        when(serviceTokenProperties.isValidToken(VALID_SERVICE_TOKEN)).thenReturn(true);
        // Gateway 헤더가 설정되어 있어도 Service Token이 우선

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }
}
```

#### 4.5.2 FilterConfigTest.java
**경로**: `adapter-in/rest-api/src/test/java/com/ryuqq/fileflow/adapter/in/rest/config/FilterConfigTest.java`

**수정된 setUp 메서드**:
```java
private FilterConfig filterConfig;
private ObjectMapper objectMapper;
private ServiceTokenProperties serviceTokenProperties;

@BeforeEach
void setUp() {
    objectMapper = new ObjectMapper();
    serviceTokenProperties = mock(ServiceTokenProperties.class);
    filterConfig = new FilterConfig(objectMapper, serviceTokenProperties);
}
```

---

## 5. 사용 방법

### 5.1 클라이언트 측 (호출하는 서버)

```java
// Worker 서비스에서 Web API 호출 예시
@Component
public class FileFlowApiClient {

    private final RestTemplate restTemplate;
    private final String serviceToken;

    public FileFlowApiClient(
            RestTemplate restTemplate,
            @Value("${SECURITY_SERVICE_TOKEN_SECRET}") String serviceToken) {
        this.restTemplate = restTemplate;
        this.serviceToken = serviceToken;
    }

    public void updateFileStatus(String fileId, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Token", serviceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
            Map.of("status", status),
            headers
        );

        restTemplate.exchange(
            "http://web-api.connectly.local:8080/api/v1/files/{fileId}/status",
            HttpMethod.PATCH,
            request,
            Void.class,
            fileId
        );
    }
}
```

### 5.2 curl 테스트

```bash
# Service Token을 사용한 내부 API 호출
curl -X GET \
  http://localhost:8080/api/v1/files \
  -H "X-Service-Token: 8de54dfbc288673edf8ef5e88c5c1cf15afb513f6e41e0b67fa5f82c06112c29" \
  -H "Content-Type: application/json"
```

---

## 6. 운영 가이드

### 6.1 SSM Parameter 생성

```bash
# AWS CLI로 SSM Parameter 생성
aws ssm put-parameter \
  --name "/shared/security/service-token-secret" \
  --type "SecureString" \
  --value "8de54dfbc288673edf8ef5e88c5c1cf15afb513f6e41e0b67fa5f82c06112c29" \
  --description "FileFlow 서버 간 내부 통신용 Service Token"
```

### 6.2 토큰 로테이션

```bash
# 1. 새 토큰 생성
NEW_TOKEN=$(openssl rand -hex 32)
echo "New Token: $NEW_TOKEN"

# 2. SSM Parameter 업데이트
aws ssm put-parameter \
  --name "/shared/security/service-token-secret" \
  --type "SecureString" \
  --value "$NEW_TOKEN" \
  --overwrite

# 3. 모든 ECS 서비스 재배포 (동시에!)
aws ecs update-service \
  --cluster fileflow-cluster-prod \
  --service fileflow-web-api-prod \
  --force-new-deployment

aws ecs update-service \
  --cluster fileflow-cluster-prod \
  --service fileflow-worker-prod \
  --force-new-deployment

# ⚠️ 중요: 모든 관련 서비스를 동시에 재배포해야 합니다!
```

### 6.3 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| 401 Unauthorized | 토큰 불일치 | SSM Parameter 값 확인 |
| 환경변수 없음 | ECS Task Definition 누락 | Terraform 재배포 |
| Service Token 무시됨 | enabled=false | SECURITY_SERVICE_TOKEN_ENABLED=true 확인 |
| Gateway 헤더로 처리됨 | Service Token 검증 실패 | 토큰 값 일치 여부 확인 |

---

## 7. 체크리스트

### 7.1 신규 서비스 적용 시

- [ ] Terraform에서 `/shared/security/service-token-secret` SSM Parameter 참조
- [ ] `container_environment`에 `SECURITY_SERVICE_TOKEN_ENABLED=true` 추가
- [ ] `container_secrets`에 `SECURITY_SERVICE_TOKEN_SECRET` 추가
- [ ] `ServiceTokenProperties` 클래스 추가 (또는 공통 모듈 의존)
- [ ] `rest-api.yml`에 `security.service-token` 설정 추가
- [ ] `UserContextFilter` 수정 (Service Token 검증 로직)
- [ ] `UserRole`에 `SYSTEM` 역할 추가
- [ ] 테스트 작성 및 실행
- [ ] 배포 후 연동 테스트

### 7.2 호출 클라이언트 적용 시

- [ ] Terraform에서 동일 SSM Parameter 참조
- [ ] 환경변수 `SECURITY_SERVICE_TOKEN_SECRET` 주입
- [ ] HTTP 클라이언트에 `X-Service-Token` 헤더 추가
- [ ] 연동 테스트

---

## 8. 참고 자료

- [security-guide.md](./security-guide.md) - Security 아키텍처 가이드
- [api-paths-guide.md](./api-paths-guide.md) - API 경로 Constants 가이드
- [AWS SSM Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html)
- [ECS Task Definition Secrets](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/specifying-sensitive-data.html)
