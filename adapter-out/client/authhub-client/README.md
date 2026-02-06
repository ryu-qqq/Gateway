# AuthHub SDK

AuthHub REST API와 통합하기 위한 공식 Java SDK입니다. 멀티 테넌트 IAM(Identity and Access Management) 시스템과의 쉬운 연동을 제공합니다.

## 목차

- [개요](#개요)
- [요구사항](#요구사항)
- [설치](#설치)
- [빠른 시작](#빠른-시작)
- [설정](#설정)
- [API 레퍼런스](#api-레퍼런스)
- [인증](#인증)
- [에러 처리](#에러-처리)
- [고급 사용법](#고급-사용법)
- [예제](#예제)
- [멀티모듈 프로젝트 통합](#멀티모듈-프로젝트-통합)
- [권한 체크 (Access Control)](#권한-체크-access-control)
- [서비스 토큰 인증](#서비스-토큰-인증)
- [엔드포인트 자동 동기화](#엔드포인트-자동-동기화)
- [GatewayClient (Internal API)](#gatewayclient-internal-api)
- [변경 이력](#변경-이력)

---

## 개요

AuthHub SDK는 두 개의 모듈로 구성됩니다:

| 모듈 | 설명 | 용도 |
|------|------|------|
| `authhub-sdk-core` | 순수 Java SDK 코어 | 모든 Java 프로젝트 |
| `authhub-sdk-spring-boot-starter` | Spring Boot 자동 설정 | Spring Boot 프로젝트 |

### 클라이언트 유형

| 클라이언트 | 설명 | 인증 방식 |
|------------|------|-----------|
| `AuthHubClient` | 일반 API 클라이언트 | 서비스 토큰 / 사용자 토큰 |
| `GatewayClient` | Gateway 전용 클라이언트 (v2.0.1+) | 서비스 토큰 |

### 주요 기능

- **6개 도메인 API 지원**: Tenant, Organization, User, Role, Permission, Onboarding
- **Gateway Internal API 지원** (v2.0.1): Permission Spec 조회 API
- **멀티 테넌트 구조**: 테넌트 > 조직 > 사용자 계층 관리
- **RBAC 지원**: 역할 기반 접근 제어 (Role-Based Access Control)
- **타입 안전성**: Java Record 기반의 강타입 DTO
- **Spring Boot 통합**: 자동 설정, 토큰 컨텍스트 필터 자동 등록
- **유연한 인증**: 서비스 토큰 / 사용자 토큰 자동 전파
- **권한 체크**: `@RequirePermission` 어노테이션 및 `BaseAccessChecker` 지원
- **서비스 토큰 인증**: 내부 서비스 간 통신을 위한 `ServiceTokenAuthenticationFilter`
- **엔드포인트 자동 동기화**: 애플리케이션 시작 시 권한 자동 등록

---

## 요구사항

| 요구사항 | 버전 |
|----------|------|
| Java | 21+ |
| Spring Boot (선택) | 3.x |

---

## 설치

### Gradle (JitPack)

```groovy
// settings.gradle 또는 build.gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

#### Spring Boot 프로젝트 (권장)

```groovy
dependencies {
    implementation 'com.github.ryu-qqq.AuthHub:authhub-sdk-spring-boot-starter:v2.0.2'
}
```

#### 순수 Java 프로젝트

```groovy
dependencies {
    implementation 'com.github.ryu-qqq.AuthHub:authhub-sdk-core:v2.0.2'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ryu-qqq.AuthHub</groupId>
    <artifactId>authhub-sdk-spring-boot-starter</artifactId>
    <version>v2.0.2</version>
</dependency>
```

---

## 빠른 시작

### Spring Boot

```yaml
# application.yml
authhub:
  base-url: https://auth.example.com
  service-token: ${AUTHHUB_SERVICE_TOKEN}  # 환경변수 권장
```

```java
@Service
public class UserService {

    private final AuthHubClient authHub;

    public UserService(AuthHubClient authHub) {
        this.authHub = authHub;
    }

    public void createUser(String tenantId, String orgId, String email) {
        CreateUserRequest request = new CreateUserRequest(
            tenantId,
            orgId,
            email,
            "010-1234-5678",
            "initial-password"
        );

        ApiResponse<CreateUserResponse> response = authHub.users().create(request);
        System.out.println("Created user: " + response.data().userId());
    }
}
```

### 순수 Java

```java
AuthHubClient client = AuthHubClient.builder()
    .baseUrl("https://auth.example.com")
    .serviceToken("your-service-token")
    .build();

// Tenant 생성
CreateTenantRequest request = new CreateTenantRequest("My Company");
ApiResponse<CreateTenantResponse> response = client.tenants().create(request);

System.out.println("Tenant ID: " + response.data().tenantId());
```

---

## 설정

### Spring Boot 설정 옵션

```yaml
authhub:
  # [필수] AuthHub 서버 URL
  base-url: https://auth.example.com

  # [선택] 서비스 토큰 (M2M 통신용)
  # ThreadLocal 토큰이 없을 경우 fallback으로 사용
  service-token: ${AUTHHUB_SERVICE_TOKEN}

  # [선택] 타임아웃 설정
  timeout:
    connect: 5s      # 연결 타임아웃 (기본: 5초)
    read: 30s        # 읽기 타임아웃 (기본: 30초)

  # [선택] 재시도 설정
  retry:
    enabled: true    # 재시도 활성화 (기본: true)
    max-attempts: 3  # 최대 재시도 횟수 (기본: 3)
    delay: 1s        # 재시도 대기 시간 (기본: 1초)
```

### 프로그래매틱 설정

```java
AuthHubClient client = AuthHubClient.builder()
    .baseUrl("https://auth.example.com")
    .serviceToken("your-service-token")
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(60))
    .build();
```

---

## API 레퍼런스

### TenantApi - 테넌트 관리

테넌트는 AuthHub의 최상위 격리 단위입니다.

```java
TenantApi tenants = client.tenants();

// 테넌트 생성
ApiResponse<CreateTenantResponse> created = tenants.create(
    new CreateTenantRequest("My Company")
);

// 테넌트 조회
ApiResponse<TenantResponse> tenant = tenants.getById("tenant-uuid");

// 테넌트 검색
Map<String, Object> params = Map.of(
    "name", "Company",
    "status", "ACTIVE",
    "page", 0,
    "size", 20
);
ApiResponse<PageResponse<TenantResponse>> list = tenants.search(params);

// 테넌트 이름 수정
tenants.updateName("tenant-uuid", new UpdateTenantNameRequest("New Name"));

// 테넌트 상태 변경
tenants.updateStatus("tenant-uuid", new UpdateTenantStatusRequest("INACTIVE"));

// 테넌트 삭제 (비활성화)
tenants.delete("tenant-uuid");
```

### OrganizationApi - 조직 관리

조직은 테넌트 하위의 논리적 그룹입니다.

```java
OrganizationApi organizations = client.organizations();

// 조직 생성
ApiResponse<CreateOrganizationResponse> created = organizations.create(
    new CreateOrganizationRequest("tenant-uuid", "Engineering Team")
);

// 조직 조회
ApiResponse<OrganizationResponse> org = organizations.getById(1L);

// 조직 검색
Map<String, Object> params = Map.of(
    "tenantId", "tenant-uuid",
    "name", "Engineering",
    "status", "ACTIVE",
    "page", 0,
    "size", 20,
    "sortKey", "CREATED_DATE",
    "sortDirection", "DESC"
);
ApiResponse<PageResponse<OrganizationResponse>> list = organizations.search(params);

// 조직 정보 수정
organizations.update(1L, new UpdateOrganizationRequest("DevOps Team"));

// 조직 상태 변경
organizations.updateStatus(1L, new UpdateOrganizationStatusRequest("INACTIVE"));

// 조직 삭제
organizations.delete(1L);
```

### UserApi - 사용자 관리

```java
UserApi users = client.users();

// 사용자 생성
ApiResponse<CreateUserResponse> created = users.create(
    new CreateUserRequest(
        "tenant-uuid",      // 테넌트 ID
        "org-uuid",         // 조직 ID
        "user@example.com", // 식별자 (이메일)
        "010-1234-5678",    // 전화번호
        "password123"       // 비밀번호
    )
);

// 사용자 조회
ApiResponse<UserResponse> user = users.getById(1L);

// 사용자 검색
Map<String, Object> params = Map.of(
    "tenantId", "tenant-uuid",
    "organizationId", "org-uuid",
    "identifier", "user@",
    "status", "ACTIVE",
    "page", 0,
    "size", 20
);
ApiResponse<PageResponse<UserResponse>> list = users.search(params);

// 사용자 정보 수정 (identifier 변경)
users.update(1L, new UpdateUserRequest("new-identifier"));

// 사용자 상태 변경
users.updateStatus(1L, new UpdateUserStatusRequest("SUSPENDED"));

// 비밀번호 변경 (현재 비밀번호, 새 비밀번호)
users.updatePassword(1L, new UpdateUserPasswordRequest("currentPassword123", "newPassword123"));

// 역할 할당
users.assignRole(1L, new AssignUserRoleRequest(10L));

// 역할 해제
users.unassignRole(1L, 10L);

// 사용자 삭제
users.delete(1L);
```

### RoleApi - 역할 관리

```java
RoleApi roles = client.roles();

// 역할 생성
ApiResponse<CreateRoleResponse> created = roles.create(
    new CreateRoleRequest("org-uuid", "Admin", "관리자 역할")
);

// 역할 조회
ApiResponse<RoleResponse> role = roles.getById(1L);

// 역할 검색
Map<String, Object> params = Map.of(
    "tenantId", "tenant-uuid",
    "name", "Admin",
    "page", 0,
    "size", 20,
    "sortKey", "NAME",
    "sortDirection", "ASC"
);
ApiResponse<PageResponse<RoleResponse>> list = roles.search(params);

// 역할 정보 수정
roles.update(1L, new UpdateRoleRequest("Super Admin", "최고 관리자"));

// 권한 부여
roles.grantPermissions(1L, new GrantRolePermissionRequest(List.of(1L, 2L, 3L)));

// 권한 회수
roles.revokePermissions(1L, new GrantRolePermissionRequest(List.of(3L)));

// 역할 삭제
roles.delete(1L);
```

### PermissionApi - 권한 관리

```java
PermissionApi permissions = client.permissions();

// 권한 생성
ApiResponse<CreatePermissionResponse> created = permissions.create(
    new CreatePermissionRequest(
        "USER",           // 리소스
        "READ",           // 액션
        "사용자 조회 권한", // 설명
        false             // 시스템 권한 여부
    )
);

// 권한 조회
ApiResponse<PermissionResponse> permission = permissions.getById(1L);

// 권한 검색
Map<String, Object> params = Map.of(
    "tenantId", "tenant-uuid",
    "name", "USER",
    "page", 0,
    "size", 20
);
ApiResponse<PageResponse<PermissionResponse>> list = permissions.search(params);

// 권한 정보 수정
permissions.update(1L, new UpdatePermissionRequest("사용자 전체 조회 권한"));

// 권한 삭제
permissions.delete(1L);
```

### OnboardingApi - 테넌트 온보딩

테넌트 + 조직 + 기본 역할 + 관리자를 한 번에 생성합니다.

```java
OnboardingApi onboarding = client.onboarding();

// 테넌트 온보딩
ApiResponse<TenantOnboardingResponse> result = onboarding.onboard(
    new TenantOnboardingRequest(
        "New Company",           // 테넌트 이름
        "Main Organization",     // 조직 이름
        "admin@company.com",     // 관리자 이메일
        "010-0000-0000"          // 관리자 전화번호
    )
);

// 결과
String tenantId = result.data().tenantId();
String organizationId = result.data().organizationId();
String userId = result.data().userId();
String temporaryPassword = result.data().temporaryPassword();
```

---

## 인증

### 인증 방식

SDK는 두 가지 인증 방식을 지원합니다:

| 방식 | 용도 | 설정 |
|------|------|------|
| 서비스 토큰 | 백엔드 서비스 간 통신 (M2M) | `authhub.service-token` |
| 사용자 토큰 | 사용자 요청 전파 | 자동 (ThreadLocal) |

### 토큰 우선순위

```
1. ThreadLocal 토큰 (사용자 요청에서 자동 추출)
2. 서비스 토큰 (설정된 경우)
3. 인증 실패 예외
```

### Spring Boot에서의 토큰 전파

`AuthHubTokenContextFilter`가 자동으로 등록되어, 들어오는 요청의 `Authorization: Bearer {token}` 헤더를 자동으로 추출하여 AuthHub API 호출 시 전파합니다.

```
[Client] --Bearer token--> [Your API] --same token--> [AuthHub API]
```

---

## 에러 처리

### 예외 계층

```
AuthHubException (base)
├── AuthHubBadRequestException     (400)
├── AuthHubUnauthorizedException   (401)
├── AuthHubForbiddenException      (403)
├── AuthHubNotFoundException       (404)
└── AuthHubServerException         (5xx)
```

### 예외 처리 예시

```java
try {
    ApiResponse<UserResponse> response = client.users().getById(userId);
    // 성공 처리
} catch (AuthHubNotFoundException e) {
    // 사용자 없음
    log.warn("User not found: {}", e.getMessage());
} catch (AuthHubUnauthorizedException e) {
    // 인증 실패
    log.error("Authentication failed: {}", e.getMessage());
} catch (AuthHubForbiddenException e) {
    // 권한 부족
    log.error("Access denied: {}", e.getMessage());
} catch (AuthHubBadRequestException e) {
    // 잘못된 요청
    log.error("Bad request: {} - {}", e.getErrorCode(), e.getMessage());
} catch (AuthHubServerException e) {
    // 서버 에러
    log.error("Server error: {}", e.getMessage());
}
```

### 예외 정보

```java
catch (AuthHubException e) {
    String errorCode = e.getErrorCode();  // "USER_NOT_FOUND"
    String message = e.getMessage();       // "User not found with ID: 123"
    int statusCode = e.getStatusCode();    // 404
}
```

---

## 고급 사용법

### 페이징 처리

```java
int page = 0;
int pageSize = 20;
List<UserResponse> allUsers = new ArrayList<>();

while (true) {
    Map<String, Object> params = Map.of(
        "tenantId", tenantId,
        "page", page,
        "size", pageSize
    );

    ApiResponse<PageResponse<UserResponse>> response = client.users().search(params);
    PageResponse<UserResponse> pageData = response.data();

    allUsers.addAll(pageData.content());

    if (!pageData.hasNext()) {
        break;
    }
    page++;
}
```

### Admin API 사용

Admin API는 추가 집계 정보가 포함된 Summary 응답을 반환합니다:

```java
// Admin용 조직 검색 (요약 정보 포함)
ApiResponse<PageResponse<OrganizationSummaryResponse>> adminList =
    client.organizations().searchAdmin(params);

for (OrganizationSummaryResponse summary : adminList.data().content()) {
    System.out.println("Organization: " + summary.name());
    System.out.println("User count: " + summary.userCount());
    System.out.println("Role count: " + summary.roleCount());
}
```

### 커스텀 TokenResolver

Spring Boot 이외의 환경에서 커스텀 토큰 처리:

```java
TokenResolver customResolver = () -> {
    // 커스텀 토큰 획득 로직
    String token = SecurityContextHolder.getContext()
        .getAuthentication()
        .getCredentials()
        .toString();
    return Optional.ofNullable(token);
};

AuthHubClient client = AuthHubClient.builder()
    .baseUrl("https://auth.example.com")
    .tokenResolver(customResolver)
    .build();
```

---

## 예제

### 완전한 테넌트 온보딩 플로우

```java
@Service
@RequiredArgsConstructor
public class TenantSetupService {

    private final AuthHubClient authHub;

    public TenantSetupResult setupNewTenant(TenantSetupCommand command) {
        // 1. 테넌트 온보딩 (테넌트 + 조직 + 관리자 일괄 생성)
        TenantOnboardingResponse onboarding = authHub.onboarding()
            .onboard(new TenantOnboardingRequest(
                command.companyName(),
                command.organizationName(),
                command.adminEmail(),
                command.adminPhone()
            ))
            .data();

        // 2. 추가 권한 생성
        CreatePermissionResponse readPermission = authHub.permissions()
            .create(new CreatePermissionRequest("ORDER", "READ", "주문 조회", false))
            .data();

        CreatePermissionResponse writePermission = authHub.permissions()
            .create(new CreatePermissionRequest("ORDER", "WRITE", "주문 수정", false))
            .data();

        // 3. 역할 생성 및 권한 부여
        CreateRoleResponse role = authHub.roles()
            .create(new CreateRoleRequest(
                onboarding.organizationId(),
                "OrderManager",
                "주문 관리자"
            ))
            .data();

        authHub.roles().grantPermissions(
            role.roleId(),
            new GrantRolePermissionRequest(List.of(
                readPermission.permissionId(),
                writePermission.permissionId()
            ))
        );

        return new TenantSetupResult(
            onboarding.tenantId(),
            onboarding.organizationId(),
            onboarding.userId(),
            onboarding.temporaryPassword()
        );
    }
}
```

### 사용자 초대 플로우

```java
@Service
@RequiredArgsConstructor
public class UserInvitationService {

    private final AuthHubClient authHub;
    private final EmailService emailService;

    public void inviteUser(String tenantId, String orgId, String email, Long roleId) {
        // 1. 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 2. 사용자 생성
        CreateUserResponse user = authHub.users()
            .create(new CreateUserRequest(tenantId, orgId, email, null, tempPassword))
            .data();

        // 3. 역할 할당
        authHub.users().assignRole(user.userId(), new AssignUserRoleRequest(roleId));

        // 4. 초대 이메일 발송
        emailService.sendInvitation(email, tempPassword);
    }
}
```

---

## 모듈 구조

```
sdk/
├── authhub-sdk-core/                    # 코어 SDK
│   └── src/main/java/
│       └── com/ryuqq/authhub/sdk/
│           ├── api/                      # API 인터페이스
│           │   ├── AuthApi.java
│           │   ├── OnboardingApi.java
│           │   └── InternalApi.java      # (v2.0.1) Internal API
│           ├── client/                   # 클라이언트 구현
│           │   ├── AuthHubClient.java
│           │   ├── AuthHubClientBuilder.java
│           │   ├── GatewayClient.java        # (v2.0.1) Gateway 전용 클라이언트
│           │   ├── GatewayClientBuilder.java # (v2.0.1)
│           │   └── internal/
│           │       ├── DefaultGatewayClient.java
│           │       ├── DefaultInternalApi.java
│           │       └── ServiceTokenHttpClientSupport.java
│           ├── config/                   # 설정
│           │   └── GatewayClientConfig.java  # (v2.0.1)
│           ├── exception/                # 예외
│           ├── model/                    # DTO 모델
│           │   ├── common/               # 공통 응답
│           │   ├── auth/                 # 인증 관련
│           │   ├── user/                 # 사용자 관련
│           │   ├── onboarding/           # 온보딩 관련
│           │   └── internal/             # (v2.0.1) Internal API 모델
│           │       ├── EndpointPermissionSpec.java
│           │       └── EndpointPermissionSpecList.java
│           └── auth/                     # 토큰 리졸버
│               ├── TokenResolver.java
│               ├── ChainTokenResolver.java
│               ├── StaticTokenResolver.java
│               └── ThreadLocalTokenResolver.java
│
└── authhub-sdk-spring-boot-starter/     # Spring Boot 통합
    └── src/main/java/
        └── com/ryuqq/authhub/sdk/
            ├── autoconfigure/            # 자동 설정
            │   ├── AuthHubAutoConfiguration.java
            │   ├── AuthHubProperties.java
            │   └── AuthHubTokenContextFilter.java
            ├── access/                   # 권한 체크
            │   ├── AccessChecker.java
            │   └── BaseAccessChecker.java
            ├── annotation/               # 어노테이션
            │   └── RequirePermission.java
            ├── context/                  # 사용자 컨텍스트
            │   ├── SecurityContext.java
            │   ├── UserContext.java
            │   └── UserContextHolder.java
            ├── filter/                   # 필터
            │   ├── GatewayAuthenticationFilter.java
            │   └── ServiceTokenAuthenticationFilter.java
            ├── header/                   # 헤더 처리
            │   ├── GatewayHeaderParser.java
            │   └── SecurityHeaders.java
            ├── sync/                     # 엔드포인트 동기화
            │   ├── EndpointInfo.java
            │   ├── EndpointScanner.java
            │   ├── EndpointSyncClient.java
            │   ├── EndpointSyncRequest.java
            │   └── EndpointSyncRunner.java
            ├── constant/                 # 상수
            │   ├── Permissions.java
            │   ├── Roles.java
            │   └── Scopes.java
            └── util/                     # 유틸리티
                ├── PermissionMatcher.java
                └── ScopeValidator.java
```

---

## 멀티모듈 프로젝트 통합

헥사고날 아키텍처 멀티모듈 프로젝트에서 SDK를 사용하는 방법입니다.

### 의존성 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                        YOUR PROJECT                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐                                                    │
│  │   DOMAIN    │  ← SDK 사용 ❌ (순수 Java 유지)                     │
│  └──────┬──────┘                                                    │
│         │                                                            │
│  ┌──────▼──────┐                                                    │
│  │ APPLICATION │  ← SDK 사용 ❌ (도메인 로직만)                      │
│  └──────┬──────┘                                                    │
│         │                                                            │
│  ┌──────▼──────────────────────────────────────────────────────┐    │
│  │                    ADAPTER LAYER                              │    │
│  │  ┌────────────────┐           ┌─────────────────────────┐   │    │
│  │  │  adapter-in    │           │     adapter-out         │   │    │
│  │  │  (REST API)    │           │  ┌─────────────────┐    │   │    │
│  │  │                │           │  │  client/authhub │    │   │    │
│  │  │ ✅ SDK 사용    │           │  │  ✅ SDK 사용    │    │   │    │
│  │  │ - @RequirePerm │           │  │ - AuthHubClient │    │   │    │
│  │  │ - AccessChecker│           │  │ - AuthApi       │    │   │    │
│  │  │ - UserContext  │           │  └─────────────────┘    │   │    │
│  │  └────────────────┘           └─────────────────────────┘   │    │
│  └──────────────────────────────────────────────────────────────┘    │
│         │                                                            │
│  ┌──────▼──────┐                                                    │
│  │  BOOTSTRAP  │  ← SDK AutoConfiguration 활성화                     │
│  │             │  ← 필터 등록 (ServiceToken, Gateway)                │
│  └─────────────┘                                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 모듈별 의존성 설정

#### adapter-in/rest-api/build.gradle

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    api project(':application')
    api project(':domain')

    // ✅ AuthHub SDK - api로 선언하여 bootstrap에 전이
    api 'com.github.ryu-qqq.AuthHub:authhub-sdk-spring-boot-starter:v2.0.1'

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
}
```

> **Note**: `api`로 선언하면 bootstrap 모듈에서 별도 의존성 추가가 불필요합니다.

#### bootstrap/web-api/build.gradle

```groovy
dependencies {
    implementation project(':domain')
    implementation project(':application')
    implementation project(':adapter-in:rest-api')  // SDK가 전이적으로 포함됨

    // SDK 별도 추가 불필요!
}
```

### 모듈별 SDK 사용 원칙

| 레이어 | SDK 사용 | 이유 |
|--------|----------|------|
| **Domain** | ❌ 금지 | 순수 Java 유지, 외부 의존성 없음 |
| **Application** | ❌ 금지 | 도메인 로직만, 인프라 관심사 분리 |
| **Adapter-In** | ✅ 허용 | HTTP 요청 처리, 권한 체크는 인프라 관심사 |
| **Adapter-Out** | ✅ 허용 | 외부 시스템(AuthHub) 연동 |
| **Bootstrap** | ✅ 허용 | 필터 등록, AutoConfiguration |

---

## 권한 체크 (Access Control)

SDK는 선언적 권한 체크와 프로그래매틱 권한 체크를 모두 지원합니다.

### @RequirePermission 어노테이션

엔드포인트에 필요한 권한을 선언적으로 명시합니다.

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping("/{id}")
    @RequirePermission("product:read")
    public ProductResponse getProduct(@PathVariable Long id) {
        // ...
    }

    @PostMapping
    @RequirePermission(value = "product:create", description = "상품 생성")
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        // ...
    }

    @DeleteMapping("/{id}")
    @RequirePermission("product:delete")
    public void deleteProduct(@PathVariable Long id) {
        // ...
    }
}
```

> **Note**: `@RequirePermission`은 문서화 및 엔드포인트 자동 동기화 목적이며, 실제 권한 체크는 Gateway 또는 AccessChecker에서 수행됩니다.

### BaseAccessChecker 확장

프로그래매틱 권한 체크를 위해 `BaseAccessChecker`를 확장합니다.

```java
@Component("access")  // SpEL에서 사용할 이름
public class ProductAccessChecker extends BaseAccessChecker {

    // 도메인별 권한 체크 메서드
    public boolean canReadProduct() {
        return hasPermission("product:read");
    }

    public boolean canWriteProduct() {
        return hasPermission("product:write");
    }

    public boolean canDeleteProduct() {
        return hasPermission("product:delete");
    }

    // 복합 권한 체크
    public boolean canManageProduct() {
        return hasAllPermissions("product:read", "product:write", "product:delete");
    }

    // 리소스 격리 + 권한 체크
    public boolean canAccessProduct(String productTenantId) {
        return sameTenant(productTenantId) && hasPermission("product:read");
    }
}
```

### Controller에서 권한 체크

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductAccessChecker access;
    private final GetProductUseCase getProductUseCase;

    public ProductController(ProductAccessChecker access, GetProductUseCase getProductUseCase) {
        this.access = access;
        this.getProductUseCase = getProductUseCase;
    }

    @GetMapping("/{id}")
    @RequirePermission("product:read")
    public ProductResponse getProduct(@PathVariable Long id) {
        // 권한 체크
        if (!access.canReadProduct()) {
            throw new AccessDeniedException("상품 조회 권한이 없습니다");
        }

        // 현재 사용자 컨텍스트 조회
        UserContext context = UserContextHolder.getContext();
        String tenantId = context.getTenantId();

        return getProductUseCase.execute(id, tenantId);
    }
}
```

### BaseAccessChecker 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `authenticated()` | 인증된 사용자인지 확인 |
| `superAdmin()` | SUPER_ADMIN 역할인지 확인 |
| `admin()` | 관리자 역할(SUPER_ADMIN, TENANT_ADMIN, ORG_ADMIN) 중 하나인지 확인 |
| `hasRole(role)` | 특정 역할 보유 여부 |
| `hasAnyRole(roles...)` | 역할 중 하나라도 보유 여부 |
| `hasPermission(perm)` | 특정 권한 보유 여부 |
| `hasAnyPermission(perms...)` | 권한 중 하나라도 보유 여부 |
| `hasAllPermissions(perms...)` | 모든 권한 보유 여부 |
| `sameTenant(tenantId)` | 같은 테넌트인지 확인 |
| `sameOrganization(orgId)` | 같은 조직인지 확인 |
| `myself(userId)` | 본인인지 확인 |
| `myselfOr(userId, perm)` | 본인이거나 특정 권한 보유 여부 |
| `serviceAccount()` | 서비스 계정인지 확인 |

### UserContext 사용

현재 요청의 사용자 정보를 조회합니다.

```java
// 현재 사용자 컨텍스트 조회
UserContext context = UserContextHolder.getContext();

// 사용자 정보
String userId = context.getUserId();
String tenantId = context.getTenantId();
String organizationId = context.getOrganizationId();
String email = context.getEmail();

// 역할/권한 확인
Set<String> roles = context.getRoles();
Set<String> permissions = context.getPermissions();
boolean hasRole = context.hasRole("ADMIN");
boolean hasPerm = context.hasPermission("product:read");

// 서비스 계정 여부
boolean isServiceAccount = context.isServiceAccount();

// 요청 추적
String correlationId = context.getCorrelationId();
String requestSource = context.getRequestSource();
```

---

## 서비스 토큰 인증

내부 서비스 간 통신을 위한 서비스 토큰 인증을 지원합니다.

### ServiceTokenAuthenticationFilter 설정

```java
@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<ServiceTokenAuthenticationFilter> serviceTokenFilter() {
        ServiceTokenAuthenticationFilter filter = new ServiceTokenAuthenticationFilter(
            (serviceName, token) -> validateServiceToken(serviceName, token)
        );

        FilterRegistrationBean<ServiceTokenAuthenticationFilter> registration =
            new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/v1/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    private boolean validateServiceToken(String serviceName, String token) {
        // 서비스별 토큰 검증
        Map<String, String> serviceTokens = Map.of(
            "order-service", System.getenv("ORDER_SERVICE_TOKEN"),
            "inventory-service", System.getenv("INVENTORY_SERVICE_TOKEN")
        );
        String expectedToken = serviceTokens.get(serviceName);
        return expectedToken != null && expectedToken.equals(token);
    }
}
```

### 다른 서비스에서 호출 시

```java
// HTTP 헤더 설정
HttpHeaders headers = new HttpHeaders();
headers.set("X-Service-Name", "order-service");
headers.set("X-Service-Token", "${ORDER_SERVICE_TOKEN}");

// 원본 사용자 정보 전달 (선택)
headers.set("X-Original-User-Id", "user-123");
headers.set("X-Original-Tenant-Id", "tenant-456");
headers.set("X-Correlation-Id", UUID.randomUUID().toString());
```

### 보안 헤더 상수

```java
// SecurityHeaders 클래스에서 제공
public static final String SERVICE_NAME = "X-Service-Name";
public static final String SERVICE_TOKEN = "X-Service-Token";
public static final String ORIGINAL_USER_ID = "X-Original-User-Id";
public static final String ORIGINAL_TENANT_ID = "X-Original-Tenant-Id";
public static final String ORIGINAL_ORGANIZATION_ID = "X-Original-Organization-Id";
public static final String CORRELATION_ID = "X-Correlation-Id";
```

---

## 엔드포인트 자동 동기화

애플리케이션 시작 시 `@RequirePermission` 어노테이션이 붙은 엔드포인트를 AuthHub에 자동 동기화합니다.

### 동작 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  1. 애플리케이션 시작                                            │
│         ↓                                                       │
│  2. EndpointSyncRunner 실행 (ApplicationRunner)                 │
│         ↓                                                       │
│  3. EndpointScanner가 @RequirePermission 어노테이션 스캔         │
│         ↓                                                       │
│  4. EndpointSyncClient를 통해 AuthHub에 동기화 요청              │
│     POST /api/v1/internal/endpoints/sync                        │
│         ↓                                                       │
│  5. AuthHub가 권한 자동 등록                                     │
└─────────────────────────────────────────────────────────────────┘
```

### 설정 방법

#### 1. EndpointSyncClient 구현

```java
@Component
public class HttpEndpointSyncClient implements EndpointSyncClient {

    private final RestTemplate restTemplate;
    private final String authHubUrl;
    private final String serviceToken;

    public HttpEndpointSyncClient(
            RestTemplate restTemplate,
            @Value("${authhub.base-url}") String authHubUrl,
            @Value("${authhub.service-token}") String serviceToken) {
        this.restTemplate = restTemplate;
        this.authHubUrl = authHubUrl;
        this.serviceToken = serviceToken;
    }

    @Override
    public void sync(EndpointSyncRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Name", request.serviceName());
        headers.set("X-Service-Token", serviceToken);

        HttpEntity<EndpointSyncRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(
            authHubUrl + "/api/v1/internal/endpoints/sync",
            entity,
            Void.class
        );
    }
}
```

#### 2. EndpointSyncRunner Bean 등록

```java
@Configuration
@ConditionalOnProperty(name = "authhub.endpoint-sync.enabled", havingValue = "true")
public class EndpointSyncConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public EndpointSyncRunner endpointSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            EndpointSyncClient syncClient) {
        return new EndpointSyncRunner(handlerMapping, syncClient, serviceName);
    }
}
```

#### 3. application.yml 설정

```yaml
spring:
  application:
    name: product-service

authhub:
  base-url: https://auth.example.com
  service-token: ${AUTHHUB_SERVICE_TOKEN}
  endpoint-sync:
    enabled: true  # 엔드포인트 동기화 활성화
```

### 동기화 요청 형식

AuthHub에 전송되는 요청 형식입니다:

```json
{
  "serviceName": "product-service",
  "endpoints": [
    {
      "httpMethod": "GET",
      "pathPattern": "/api/v1/products/{id}",
      "permissionKey": "product:read",
      "description": "상품 조회"
    },
    {
      "httpMethod": "POST",
      "pathPattern": "/api/v1/products",
      "permissionKey": "product:create",
      "description": "상품 생성"
    },
    {
      "httpMethod": "DELETE",
      "pathPattern": "/api/v1/products/{id}",
      "permissionKey": "product:delete",
      "description": ""
    }
  ]
}
```

### 동기화 비활성화

개발 환경이나 테스트에서 동기화를 비활성화하려면:

```yaml
authhub:
  endpoint-sync:
    enabled: false
```

또는 EndpointSyncRunner 생성 시:

```java
new EndpointSyncRunner(handlerMapping, syncClient, serviceName, false);  // enabled = false
```

### 주의사항

- 동기화 실패 시에도 애플리케이션 시작은 계속 진행됩니다 (fail-safe)
- 동기화는 애플리케이션 시작 시 한 번만 실행됩니다
- AuthHub 서버가 내부 네트워크에서만 접근 가능해야 보안이 유지됩니다
- 서비스 토큰은 환경 변수로 관리하세요

---

## GatewayClient (Internal API)

> **v2.0.1에서 추가됨**

Gateway에서 AuthHub의 Permission Spec을 조회하기 위한 전용 클라이언트입니다.

### GatewayClient vs AuthHubClient

| 구분 | AuthHubClient | GatewayClient |
|------|---------------|---------------|
| **용도** | 일반 API 호출 | Gateway 전용 Internal API |
| **인증** | 서비스 토큰 / 사용자 토큰 | 서비스 토큰 전용 |
| **API** | Tenant, User, Role, Permission 등 | Permission Spec |
| **헤더** | Authorization | X-Service-Name, X-Service-Token |

### 설치

```groovy
dependencies {
    implementation 'com.github.ryu-qqq.AuthHub:authhub-sdk-core:v2.0.2'
}
```

### 기본 사용법

```java
import com.ryuqq.authhub.sdk.client.GatewayClient;
import com.ryuqq.authhub.sdk.model.common.ApiResponse;
import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpecList;

// 클라이언트 생성
GatewayClient client = GatewayClient.builder()
    .baseUrl("https://authhub.example.com")
    .serviceName("gateway")
    .serviceToken("your-service-token")
    .build();

// Permission Spec 조회
ApiResponse<EndpointPermissionSpecList> response = client.internal().getPermissionSpec();

if (response.success()) {
    EndpointPermissionSpecList specList = response.data();

    // 버전 정보 (캐시 키로 사용)
    String version = specList.version();
    Instant updatedAt = specList.updatedAt();

    // 엔드포인트별 권한 정보
    for (EndpointPermissionSpec spec : specList.endpoints()) {
        System.out.println("Service: " + spec.serviceName());
        System.out.println("Path: " + spec.pathPattern());
        System.out.println("Method: " + spec.httpMethod());
        System.out.println("Permissions: " + spec.requiredPermissions());
        System.out.println("Public: " + spec.isPublic());
    }
}
```

### JWKS 조회 (JWT 서명 검증용)

> **v2.0.1에서 추가됨**

Gateway에서 JWT 토큰 서명 검증을 위한 공개키를 조회합니다.

```java
import com.ryuqq.authhub.sdk.model.internal.PublicKeys;
import com.ryuqq.authhub.sdk.model.internal.PublicKey;

// JWKS 조회
PublicKeys publicKeys = client.internal().getJwks();

// 공개키 목록 순회
for (PublicKey key : publicKeys.keys()) {
    System.out.println("Key ID: " + key.kid());
    System.out.println("Algorithm: " + key.alg());
    System.out.println("Key Type: " + key.kty());
    System.out.println("Public Key (n): " + key.n());
    System.out.println("Exponent (e): " + key.e());
}
```

### 테넌트 설정 조회

> **v2.0.1에서 추가됨**

Gateway에서 요청 처리 시 테넌트 유효성을 검증하기 위한 설정을 조회합니다.

```java
import com.ryuqq.authhub.sdk.model.internal.TenantConfig;

// 테넌트 설정 조회
String tenantId = "tenant-123";
ApiResponse<TenantConfig> response = client.internal().getTenantConfig(tenantId);

if (response.success()) {
    TenantConfig config = response.data();
    System.out.println("Tenant ID: " + config.tenantId());
    System.out.println("Name: " + config.name());
    System.out.println("Status: " + config.status());
    System.out.println("Active: " + config.active());

    // 비활성 테넌트 요청 차단
    if (!config.active()) {
        throw new TenantInactiveException("Tenant is not active: " + tenantId);
    }
}
```

### 사용자 권한 조회 (인가 검증용)

> **v2.0.1에서 추가됨**

Gateway에서 사용자 인가 검증을 위해 역할/권한 정보를 조회합니다.

```java
import com.ryuqq.authhub.sdk.model.internal.UserPermissions;

// 사용자 권한 조회
String userId = "user-456";
ApiResponse<UserPermissions> response = client.internal().getUserPermissions(userId);

if (response.success()) {
    UserPermissions permissions = response.data();
    System.out.println("User ID: " + permissions.userId());
    System.out.println("Roles: " + permissions.roles());
    System.out.println("Permissions: " + permissions.permissions());

    // 권한 검증 예시
    if (permissions.permissions().contains("product:write")) {
        // 쓰기 권한 있음
    }

    if (permissions.roles().contains("ADMIN")) {
        // 관리자 역할 있음
    }
}
```

### 타임아웃 설정

```java
import java.time.Duration;

GatewayClient client = GatewayClient.builder()
    .baseUrl("https://authhub.example.com")
    .serviceName("gateway")
    .serviceToken("your-service-token")
    .connectTimeout(Duration.ofSeconds(5))   // 기본: 5초
    .readTimeout(Duration.ofSeconds(30))     // 기본: 30초
    .build();
```

### Spring Boot 통합

```java
@Configuration
public class AuthHubClientConfig {

    @Value("${authhub.base-url}")
    private String baseUrl;

    @Value("${authhub.service-name}")
    private String serviceName;

    @Value("${authhub.service-token}")
    private String serviceToken;

    @Bean
    public GatewayClient gatewayClient() {
        return GatewayClient.builder()
            .baseUrl(baseUrl)
            .serviceName(serviceName)
            .serviceToken(serviceToken)
            .build();
    }
}
```

```yaml
# application.yml
authhub:
  base-url: ${AUTHHUB_BASE_URL:http://localhost:8080}
  service-name: gateway
  service-token: ${AUTHHUB_SERVICE_TOKEN}
```

### EndpointPermissionSpec 모델

| 필드 | 타입 | 설명 |
|------|------|------|
| `serviceName` | `String` | 서비스 이름 (예: `product-service`) |
| `pathPattern` | `String` | URL 패턴 (예: `/api/v1/users/{id}`) |
| `httpMethod` | `String` | HTTP 메서드 (GET, POST, PUT, DELETE 등) |
| `requiredPermissions` | `List<String>` | 필요 권한 목록 |
| `requiredRoles` | `List<String>` | 필요 역할 목록 |
| `isPublic` | `boolean` | 공개 엔드포인트 여부 |
| `description` | `String` | 엔드포인트 설명 |

### PublicKey 모델 (JWKS)

> **v2.0.1에서 추가됨**

| 필드 | 타입 | 설명 |
|------|------|------|
| `kid` | `String` | Key ID (키 식별자) |
| `kty` | `String` | Key Type (예: `RSA`) |
| `alg` | `String` | Algorithm (예: `RS256`) |
| `use` | `String` | 용도 (예: `sig` - 서명용) |
| `n` | `String` | RSA 공개키 모듈러스 (Base64URL 인코딩) |
| `e` | `String` | RSA 공개키 지수 (Base64URL 인코딩) |

### TenantConfig 모델

> **v2.0.1에서 추가됨**

| 필드 | 타입 | 설명 |
|------|------|------|
| `tenantId` | `String` | 테넌트 ID |
| `name` | `String` | 테넌트 이름 |
| `status` | `String` | 테넌트 상태 (`ACTIVE`, `INACTIVE`) |
| `active` | `boolean` | 활성 여부 (빠른 검증용) |

### UserPermissions 모델

> **v2.0.1에서 추가됨**

| 필드 | 타입 | 설명 |
|------|------|------|
| `userId` | `String` | 사용자 ID |
| `roles` | `Set<String>` | 역할 이름 Set (예: `ADMIN`, `USER`) |
| `permissions` | `Set<String>` | 권한 키 Set (예: `product:read`, `order:write`) |

### 캐싱 권장 사항

Permission Spec은 자주 변경되지 않으므로 캐싱을 권장합니다:

```java
@Service
public class PermissionSpecCacheService {

    private final GatewayClient gatewayClient;
    private final RedisTemplate<String, EndpointPermissionSpecList> redisTemplate;

    private static final String CACHE_KEY = "permission:spec";
    private static final Duration TTL = Duration.ofHours(1);

    public EndpointPermissionSpecList getPermissionSpec() {
        // 1. Redis 캐시 확인
        EndpointPermissionSpecList cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        // 2. AuthHub API 호출
        ApiResponse<EndpointPermissionSpecList> response =
            gatewayClient.internal().getPermissionSpec();

        if (response.success()) {
            EndpointPermissionSpecList spec = response.data();

            // 3. Redis 캐시 저장 (TTL: 1시간)
            redisTemplate.opsForValue().set(CACHE_KEY, spec, TTL);
            return spec;
        }

        throw new RuntimeException("Permission Spec 조회 실패");
    }

    public void invalidateCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
```

### 예외 처리

```java
try {
    ApiResponse<EndpointPermissionSpecList> response =
        gatewayClient.internal().getPermissionSpec();
} catch (AuthHubUnauthorizedException e) {
    // 401: 서비스 토큰 인증 실패
    log.error("인증 실패: {}", e.getMessage());
} catch (AuthHubForbiddenException e) {
    // 403: 권한 없음
    log.error("권한 없음: {}", e.getMessage());
} catch (AuthHubServerException e) {
    // 5xx: 서버 오류
    log.error("서버 오류: {}", e.getMessage());
}
```

### 상세 가이드

자세한 내용은 [GATEWAY_CLIENT_GUIDE.md](./docs/sdk/GATEWAY_CLIENT_GUIDE.md)를 참조하세요.

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| **v2.0.2** | 2026-02-03 | JWKS/Tenant Config/User Permissions API 추가, SDK 테스트 개선, 문서화 보강 |
| v2.0.1 | 2026-02-03 | GatewayClient 추가 (Internal API 지원), Permission Spec API, 성능 최적화 |
| v2.0.0 | 2026-01-20 | Spring Boot Starter 추가, 권한 체크 기능, 엔드포인트 자동 동기화 |
| v1.0.0 | 2025-01-15 | 최초 릴리즈 (AuthHubClient) |

---

## 라이선스

MIT License - 자유롭게 사용, 수정, 배포할 수 있습니다.

---

## 문의

- **GitHub Issues**: [https://github.com/ryu-qqq/AuthHub/issues](https://github.com/ryu-qqq/AuthHub/issues)
- **이메일**: support@authhub.example.com
