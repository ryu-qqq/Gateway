# PRD: AccessGateway (Zero-Trust API Gateway)

**작성일**: 2025-01-24
**작성자**: AuthHub Team
**상태**: Draft
**관련 PRD**: [IAM Platform (AuthHub)](./iam-platform.md)

---

## 📋 프로젝트 개요

### 비즈니스 목적

AccessGateway는 **AuthHub와 연동하여 모든 마이크로서비스 요청에 대해 일관된 인증·인가 정책을 적용하는 Zero-Trust API Gateway**입니다.

**핵심 가치**:
- **Stateless 인증**: JWT 기반 인증으로 Gateway는 상태를 유지하지 않음 (Scalability 확보)
- **Permission 기반 인가**: Fine-grained permission 체크로 세밀한 접근 제어
- **자동 토큰 재발급**: Access Token 만료 시 Refresh Token으로 자동 재발급 (UX 개선)
- **멀티테넌트 격리**: Tenant 간 데이터 격리 보장
- **Zero-Trust 보안**: 모든 요청을 검증하고 최소 권한 원칙 적용

---

### 주요 사용자

| 사용자 | 역할 | 요구사항 |
|--------|------|----------|
| **Backend Services** | API 제공자 | 인증/인가 로직을 Gateway에 위임하고 비즈니스 로직에 집중 |
| **Frontend Clients** | API 소비자 | 투명한 토큰 재발급 경험 (Access Token 만료 시 자동 갱신) |
| **Platform Admin** | 운영자 | Metrics, Audit Log 기반 보안 모니터링 |
| **Security Team** | 보안 담당자 | Rate Limiting, Abuse Protection 정책 관리 |

---

### 성공 기준

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| **인증 성능** | JWT 검증 < 10ms (P95) | Prometheus Histogram |
| **가용성** | 99.9% Uptime | Prometheus Counter |
| **보안 사고** | 무단 접근 0건 | Audit Log 분석 |
| **토큰 재발급 성공률** | > 95% | Prometheus Counter |
| **Rate Limit 정확도** | False Positive < 1% | Audit Log 분석 |

---

## 🏗️ 시스템 아키텍처

### 전체 구성도

```
┌─────────────┐
│   Client    │
│ (Frontend)  │
└──────┬──────┘
       │ 1. HTTP Request (Authorization: Bearer {accessToken})
       │    Cookie: refresh_token={refreshToken}
       ▼
┌──────────────────────────────────────────────────────┐
│            AccessGateway (Spring Cloud Gateway)       │
├──────────────────────────────────────────────────────┤
│  Filter Chain:                                       │
│  1️⃣ TraceIdFilter         → X-Trace-Id 생성         │
│  2️⃣ RateLimitFilter       → Rate Limit 체크         │
│  3️⃣ JwtAuthenticationFilter → JWT 검증 (AuthHub PK)│
│  4️⃣ PermissionFilter       → Permission 인가        │
│  5️⃣ TenantContextFilter    → X-Tenant-Id 전달      │
│  6️⃣ TokenRefreshFilter     → Access Token 재발급    │
└──────────────────────────────────────────────────────┘
       │ 2. Forward Request (Authenticated)
       │    Headers: X-User-Id, X-Tenant-Id, X-Trace-Id, X-Permissions
       ▼
┌─────────────────────────────────────────────────────┐
│         Backend Services (Order, Product, etc.)     │
│  - 인증/인가 로직 없음                              │
│  - X-User-Id, X-Tenant-Id 헤더 신뢰                 │
│  - 비즈니스 로직에만 집중                           │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                     AuthHub (IAM)                   │
│  - Public Key 제공 (JWT 검증용)                     │
│  - Refresh Token 재발급 API                        │
│  - Audit Log 수집                                  │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                   Redis Cluster                     │
│  - Rate Limit 카운터                               │
│  - Permission Policy 캐시                          │
│  - Blacklist Token 저장                            │
└─────────────────────────────────────────────────────┘
```

---

## 🔑 핵심 기능 상세

### 1. Stateless JWT 인증

#### 1.1 JWT 검증 프로세스

**검증 단계**:
1. **Header 추출**: `Authorization: Bearer {accessToken}` 파싱
2. **Signature 검증**: AuthHub Public Key로 RS256 검증
3. **Expiration 검증**: `exp` claim 체크 (만료 시 401 Unauthorized)
4. **Issuer 검증**: `iss` claim이 AuthHub인지 확인
5. **Audience 검증**: `aud` claim이 Gateway인지 확인
6. **Claim 추출**: `userId`, `tenantId`, `permissions`, `roles` 추출

**Java 구현 예시**:
```java
@Component
public class JwtValidator {
    private final RSAPublicKey publicKey;

    public JwtClaims validate(String accessToken) {
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("AuthHub")
                .withAudience("AccessGateway")
                .build();

            DecodedJWT jwt = verifier.verify(accessToken);

            // Expiration 체크
            if (jwt.getExpiresAt().before(new Date())) {
                throw new JwtExpiredException("Access Token expired");
            }

            // Claims 추출
            return JwtClaims.builder()
                .userId(jwt.getClaim("userId").asLong())
                .tenantId(jwt.getClaim("tenantId").asString())
                .permissions(jwt.getClaim("permissions").asList(String.class))
                .roles(jwt.getClaim("roles").asList(String.class))
                .build();
        } catch (JWTVerificationException e) {
            throw new InvalidJwtException("Invalid JWT", e);
        }
    }
}
```

#### 1.2 Public Key 관리 및 Key Rotation ⭐

##### 1.2.1 Public Key Rotation TTL 정책

**벤치마크 비교 (타 서비스 TTL 정책)**:

| 서비스 | TTL 정책 | Key Rotation 주기 | 특징 |
|--------|---------|------------------|------|
| **Keycloak** | 5분 | 매일 자동 | 짧은 TTL, 빠른 보안 대응 |
| **AWS Cognito** | 24시간 | 30일마다 | 긴 TTL, 성능 최적화 |
| **AuthHub (Our System)** | **1시간** | **1주일마다 자동** | 보안과 성능 균형 |

---

**AuthHub Public Key Rotation 정책**:

1. **TTL: 1시간**
   - Gateway는 1시간마다 `/api/v1/auth/jwks` 엔드포인트에서 Public Key 갱신
   - 캐시 만료 시 자동 갱신 (Scheduled Task)
   - 긴급 갱신 필요 시 수동 `/actuator/refresh` 호출 가능

2. **Key Rotation 주기: 1주일**
   - AuthHub는 매주 일요일 00:00에 새로운 Private/Public Key 생성
   - 이전 Key는 7일간 병렬 유지 (Graceful Rotation)
   - `kid` (Key ID) claim으로 어떤 Key로 서명했는지 명시

3. **Multi-Key Support (JWKS Endpoint)**
   - JWT Header에 `kid` (Key ID) 포함
     ```json
     {
       "alg": "RS256",
       "typ": "JWT",
       "kid": "key-2025-01-01"  ← Key ID
     }
     ```
   - Gateway는 `/api/v1/auth/jwks` 엔드포인트에서 여러 Public Key 동시 로드
   - JWKS Response 예시:
     ```json
     {
       "keys": [
         {
           "kid": "key-2025-01-01",
           "kty": "RSA",
           "use": "sig",
           "n": "...",  // Public Key Modulus
           "e": "AQAB"
         },
         {
           "kid": "key-2024-12-25",  ← 이전 Key (7일간 유효)
           "kty": "RSA",
           "use": "sig",
           "n": "...",
           "e": "AQAB"
         }
       ]
     }
     ```

4. **Graceful Rotation (무중단 전환)**
   - **Day 0 (일요일)**: 새 Key 생성 (`key-2025-01-01`)
   - **Day 0 ~ Day 7**: 이전 Key (`key-2024-12-25`)와 신규 Key 동시 유효
   - **Day 7**: 이전 Key 만료, 신규 Key만 사용
   - **장점**: Access Token 만료(15분) 전에 Key Rotation 완료 → 서비스 중단 없음

---

##### 1.2.2 Public Key 로드 및 갱신 전략

**로드 전략**:
- **초기화 시점**: Gateway 시작 시 AuthHub의 `/api/v1/auth/jwks` 엔드포인트에서 로드
- **캐싱**: 메모리에 캐싱 (TTL: 1시간)
- **갱신 전략**:
  - ✅ **주기적 갱신**: 1시간마다 Public Key 갱신 (Scheduled Task)
  - ✅ **실패 시 재시도**: Exponential Backoff (1초, 2초, 4초, 8초, 최대 5회)
  - ✅ **Multi-Key 지원**: JWT `kid` (Key ID) claim으로 여러 Public Key 관리
  - ⚠️ **Fallback**: AuthHub 장애 시 기존 캐시 유지 (최대 24시간)

---

**Java 구현 예시 (JWKS 기반 Multi-Key 로드)**:
```java
@Component
public class JwksPublicKeyLoader {
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, RSAPublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private volatile Instant lastRefresh = Instant.now();

    @PostConstruct
    public void initialize() {
        refreshPublicKeys(); // Gateway 시작 시 Public Key 로드
    }

    @Scheduled(fixedDelay = 3600000) // 1시간마다
    public void refreshPublicKeys() {
        try {
            // 1. JWKS 엔드포인트에서 Public Key 목록 로드
            JwksResponse jwksResponse = restTemplate.getForObject(
                "http://authhub/api/v1/auth/jwks",
                JwksResponse.class
            );

            if (jwksResponse == null || jwksResponse.keys().isEmpty()) {
                log.warn("JWKS response is empty, keeping existing cache");
                return;
            }

            // 2. 각 Key를 Cache에 저장
            Map<String, RSAPublicKey> newCache = new HashMap<>();
            for (JwkKey jwkKey : jwksResponse.keys()) {
                RSAPublicKey publicKey = parseJwkToPublicKey(jwkKey);
                newCache.put(jwkKey.kid(), publicKey);
            }

            // 3. Cache 전체 교체 (Atomic Operation)
            publicKeyCache.clear();
            publicKeyCache.putAll(newCache);
            lastRefresh = Instant.now();

            log.info("Public Keys refreshed successfully: {} keys loaded", newCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh Public Keys", e);
            // 기존 캐시 유지 (Fallback)
            // 24시간 경과 시에만 경고
            if (Duration.between(lastRefresh, Instant.now()).toHours() > 24) {
                log.error("Public Key cache is stale (>24h), service may be at risk");
            }
        }
    }

    public RSAPublicKey getPublicKey(String kid) {
        RSAPublicKey publicKey = publicKeyCache.get(kid);
        if (publicKey == null) {
            // kid에 해당하는 Key가 없으면 즉시 갱신 시도
            refreshPublicKeys();
            publicKey = publicKeyCache.get(kid);

            if (publicKey == null) {
                throw new PublicKeyNotFoundException("No public key found for kid: " + kid);
            }
        }
        return publicKey;
    }

    private RSAPublicKey parseJwkToPublicKey(JwkKey jwkKey) throws Exception {
        // JWK → RSA Public Key 변환
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwkKey.n()));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwkKey.e()));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}

record JwksResponse(List<JwkKey> keys) {}
record JwkKey(String kid, String kty, String use, String n, String e) {}
```

---

**JWT 검증 시 kid 기반 Public Key 조회**:
```java
@Component
public class JwtValidator {
    private final JwksPublicKeyLoader publicKeyLoader;

    public void validate(String token) throws JWTVerificationException {
        // 1. JWT Header에서 kid 추출
        DecodedJWT jwt = JWT.decode(token);
        String kid = jwt.getKeyId(); // Header의 "kid" claim

        if (kid == null) {
            throw new InvalidJwtException("Missing 'kid' in JWT header");
        }

        // 2. kid로 Public Key 조회
        RSAPublicKey publicKey = publicKeyLoader.getPublicKey(kid);

        // 3. Public Key로 JWT 검증
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
            .withIssuer("authhub")
            .build();

        try {
            verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new InvalidJwtException("Invalid JWT signature", e);
        }
    }
}
```

---

**긴급 Public Key 갱신 (Manual Trigger)**:
```java
@RestController
@RequestMapping("/actuator")
public class PublicKeyRefreshController {
    private final JwksPublicKeyLoader publicKeyLoader;

    @PostMapping("/refresh-public-keys")
    public ResponseEntity<String> refreshPublicKeys() {
        publicKeyLoader.refreshPublicKeys();
        return ResponseEntity.ok("Public keys refreshed successfully");
    }
}
```

---

**Key Rotation 시나리오 예시**:

```
Day 0 (2025-01-01 00:00):
  ├─ AuthHub: 새 Key 생성 (key-2025-01-01)
  ├─ JWKS 엔드포인트: ["key-2025-01-01", "key-2024-12-25"]
  └─ Gateway: 1시간 후 자동 갱신 → 2개 Key 모두 캐시

Day 1~6:
  ├─ 새 JWT: kid="key-2025-01-01"로 서명
  ├─ 기존 JWT (15분 이내): kid="key-2024-12-25"로 검증 가능
  └─ Gateway: 두 Key 모두 유효

Day 7 (2025-01-08 00:00):
  ├─ AuthHub: 이전 Key 만료 (key-2024-12-25 삭제)
  ├─ JWKS 엔드포인트: ["key-2025-01-01"]
  └─ Gateway: 1시간 후 자동 갱신 → 신규 Key만 캐시

→ 무중단 Key Rotation 완료 ✅
```

---

### 2. Permission 기반 인가

#### 2.1 권한 캐싱 전략 (2-Tier Cache Architecture) ⭐

**문제 인식**: Gateway가 매 요청마다 DB/API로 Role/Permission을 조회하면 성능이 급격히 저하됨

**해결 전략**: 2단계 캐싱으로 성능과 실시간성을 동시에 확보

---

##### Tier 1: JWT Payload 기반 빠른 체크 (Primary)

**목적**: 가장 빠른 인증/인가 (추가 I/O 없음)

**동작 방식**:
1. Access Token의 `permissions` claim에 사용자 권한 목록 포함
   ```json
   {
     "userId": 123,
     "tenantId": "tenant-1",
     "permissions": ["order:read", "order:create", "product:read"],
     "roles": ["USER"],
     "exp": 1706097296
   }
   ```

2. PermissionFilter가 JWT Payload만으로 1차 검증
3. **장점**: 0ms 추가 Latency (메모리 연산만)
4. **단점**: 권한 변경 시 Access Token 만료까지 반영 안 됨 (최대 15분)

---

##### Tier 2: AuthHub Permission Hash Cache (Secondary)

**목적**: 권한 변경 즉시 반영 (TTL 기반 캐싱)

**동작 방식**:

**Step 1: Permission Hash 조회**
```java
@Component
public class PermissionCacheService {
    private final RedisTemplate<String, PermissionHash> redisTemplate;
    private final RestTemplate restTemplate;

    public PermissionHash getPermissionHash(Long userId, String tenantId) {
        String cacheKey = "permission_hash:" + tenantId + ":" + userId;

        // 1. Redis 캐시 조회
        PermissionHash cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached; // Cache Hit (5~30초 TTL)
        }

        // 2. Cache Miss → AuthHub API 호출
        PermissionHashResponse response = restTemplate.getForObject(
            "http://authhub/api/v1/permissions/hash?userId={userId}&tenantId={tenantId}",
            PermissionHashResponse.class,
            userId, tenantId
        );

        PermissionHash permissionHash = new PermissionHash(
            response.permissions(),
            response.roles(),
            response.hash() // SHA-256 hash of permissions
        );

        // 3. Redis에 캐시 (TTL: 30초)
        redisTemplate.opsForValue().set(cacheKey, permissionHash, Duration.ofSeconds(30));

        return permissionHash;
    }
}
```

**Step 2: JWT vs Cache 비교**
```java
@Component
public class PermissionValidator {
    private final PermissionCacheService cacheService;

    public void validatePermission(JwtClaims jwtClaims, String requiredPermission) {
        // 1차: JWT Payload 체크 (빠른 경로)
        if (jwtClaims.getPermissions().contains(requiredPermission)) {
            // Permission Hash 검증 (2차)
            PermissionHash cached = cacheService.getPermissionHash(
                jwtClaims.getUserId(),
                jwtClaims.getTenantId()
            );

            // Hash 비교 (권한 변경 감지)
            if (jwtClaims.getPermissionHash().equals(cached.getHash())) {
                return; // ✅ 권한 확인 (JWT == Cache)
            } else {
                // ⚠️ 권한 변경 감지 → Cache 우선
                if (cached.getPermissions().contains(requiredPermission)) {
                    return; // ✅ 새 권한으로 허용
                } else {
                    throw new PermissionDeniedException("Permission revoked");
                }
            }
        }

        // 2차: Cache 체크 (JWT에 없지만 Cache에 있을 수 있음)
        PermissionHash cached = cacheService.getPermissionHash(
            jwtClaims.getUserId(),
            jwtClaims.getTenantId()
        );

        if (!cached.getPermissions().contains(requiredPermission)) {
            throw new PermissionDeniedException("Missing permission: " + requiredPermission);
        }
    }
}
```

---

##### TTL 정책

| Cache Type | TTL | 갱신 전략 | 이유 |
|-----------|-----|----------|------|
| **Permission Hash (Redis)** | 30초 | Pull (Lazy Load) | 권한 변경 최대 30초 지연 허용 |
| **Access Token** | 15분 | N/A (만료 후 재발급) | UX 고려 (너무 짧으면 재발급 빈번) |
| **Refresh Token** | 7일 | Rotation | 보안 고려 (Reuse 감지) |

---

##### 권한 변경 시 즉시 반영 (Webhook 기반 Cache Invalidation)

**시나리오**: Admin이 사용자 권한 변경 (ADMIN Role 부여)

**프로세스**:
```
1. Admin → AuthHub: POST /api/v1/users/123/roles
                    Body: { "roleId": 2 (ADMIN) }

2. AuthHub:
   - User 123의 Role 변경 (DB UPDATE)
   - Webhook 전송 (Gateway에게 알림)
     POST http://gateway/webhook/permission-changed
     Body: {
       "userId": 123,
       "tenantId": "tenant-1",
       "reason": "ROLE_ASSIGNED"
     }

3. Gateway (WebhookController):
   - Redis 캐시 삭제
     DEL permission_hash:tenant-1:123

   - 다음 요청 시 AuthHub에서 새 Permission Hash pull
```

**Java 구현 예시 (Gateway Webhook Endpoint)**:
```java
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private final RedisTemplate<String, PermissionHash> redisTemplate;

    @PostMapping("/permission-changed")
    public ResponseEntity<Void> onPermissionChanged(@RequestBody PermissionChangedEvent event) {
        String cacheKey = "permission_hash:" + event.tenantId() + ":" + event.userId();

        // Redis 캐시 무효화
        redisTemplate.delete(cacheKey);

        log.info("Permission cache invalidated: userId={}, tenantId={}, reason={}",
            event.userId(), event.tenantId(), event.reason());

        return ResponseEntity.ok().build();
    }
}
```

---

##### 성능 비교

| 방식 | Latency (P95) | 권한 반영 속도 | 장점 | 단점 |
|------|---------------|----------------|------|------|
| **JWT Only** | < 1ms | 최대 15분 (Access Token 만료) | 가장 빠름 | 권한 변경 지연 |
| **DB 매번 조회** | 50~100ms | 즉시 | 실시간 반영 | 성능 저하 심각 |
| **2-Tier Cache (권장)** | 1~5ms (Cache Hit), 10~20ms (Cache Miss) | 최대 30초 | 성능+실시간 균형 | 구현 복잡도 높음 |

---

#### 2.2 엔드포인트-권한 매핑 자동 동기화 (AuthHub 연동) ⭐

**문제 인식**: 정적 파일(`api-permissions.yml`)로 관리하면 서비스 변경 시 Gateway가 인지하지 못함 (Drift 발생)

**해결 전략**: AuthHub가 중앙에서 권한 매핑을 관리하고, Gateway는 AuthHub로부터 실시간 동기화

---

##### Step 1: Backend Service가 Permission 노출 (/actuator/permissions)

**각 Backend Service (Order, Product 등)**가 자신이 필요한 Permission을 `/actuator/permissions` 엔드포인트로 노출:

**예시: Order Service**
```java
@RestController
@RequestMapping("/actuator/permissions")
public class PermissionDiscoveryController {

    @GetMapping
    public PermissionDiscoveryResponse getPermissions() {
        return PermissionDiscoveryResponse.builder()
            .serviceName("order-service")
            .version("1.0.0")
            .endpoints(List.of(
                EndpointPermission.builder()
                    .path("/api/v1/orders")
                    .method("POST")
                    .requiredPermissions(List.of("order:create"))
                    .requiredRoles(List.of())
                    .build(),
                EndpointPermission.builder()
                    .path("/api/v1/orders/{orderId}")
                    .method("GET")
                    .requiredPermissions(List.of("order:read"))
                    .requiredRoles(List.of())
                    .build(),
                EndpointPermission.builder()
                    .path("/api/v1/orders")
                    .method("GET")
                    .requiredPermissions(List.of("order:list"))
                    .requiredRoles(List.of("ADMIN"))
                    .build()
            ))
            .build();
    }
}
```

**Response 예시**:
```json
{
  "serviceName": "order-service",
  "version": "1.0.0",
  "endpoints": [
    {
      "path": "/api/v1/orders",
      "method": "POST",
      "requiredPermissions": ["order:create"],
      "requiredRoles": []
    },
    {
      "path": "/api/v1/orders/{orderId}",
      "method": "GET",
      "requiredPermissions": ["order:read"],
      "requiredRoles": []
    },
    {
      "path": "/api/v1/orders",
      "method": "GET",
      "requiredPermissions": ["order:list"],
      "requiredRoles": ["ADMIN"]
    }
  ]
}
```

---

##### Step 2: AuthHub가 주기적으로 Permission 스캔 (Drift 방지)

**AuthHub의 PermissionSyncScheduler**가 모든 Backend Service의 `/actuator/permissions`를 주기적으로 스캔:

**프로세스**:
```
1. AuthHub (Scheduled Task - 1시간마다):
   - 등록된 모든 서비스 목록 조회 (Service Registry)
   - 각 서비스의 /actuator/permissions 호출

2. Permission Drift 감지:
   - 서비스가 신규 엔드포인트 추가 → AuthHub DB에 자동 등록
   - 서비스가 엔드포인트 삭제 → AuthHub DB에서 Deprecated 표시
   - 필요 권한 변경 → AuthHub DB 업데이트

3. Gateway 알림:
   - Webhook 전송 (Gateway에 Permission Policy 변경 알림)
     POST http://gateway/webhook/permission-policy-changed
   - Gateway는 Redis 캐시 무효화 후 재로드
```

**Java 구현 예시 (AuthHub)**:
```java
@Component
public class PermissionSyncScheduler {
    private final ServiceDiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final PermissionPolicyRepository policyRepository;
    private final WebhookService webhookService;

    @Scheduled(fixedDelay = 3600000) // 1시간마다
    public void syncPermissions() {
        List<String> services = discoveryClient.getServices();

        for (String serviceName : services) {
            try {
                // 1. /actuator/permissions 호출
                PermissionDiscoveryResponse response = restTemplate.getForObject(
                    "http://" + serviceName + "/actuator/permissions",
                    PermissionDiscoveryResponse.class
                );

                // 2. DB와 비교하여 Drift 감지
                List<PermissionPolicy> existingPolicies = policyRepository.findByServiceName(serviceName);
                PermissionDrift drift = detectDrift(existingPolicies, response.getEndpoints());

                // 3. Drift 발견 시 DB 업데이트
                if (drift.hasChanges()) {
                    updatePermissionPolicies(drift);

                    // 4. Gateway에 Webhook 전송
                    webhookService.sendToGateway(new PermissionPolicyChangedEvent(
                        serviceName,
                        drift.getAddedEndpoints(),
                        drift.getRemovedEndpoints(),
                        drift.getUpdatedEndpoints()
                    ));

                    log.info("Permission drift detected and synced: service={}, added={}, removed={}, updated={}",
                        serviceName, drift.getAddedEndpoints().size(),
                        drift.getRemovedEndpoints().size(), drift.getUpdatedEndpoints().size());
                }
            } catch (Exception e) {
                log.error("Failed to sync permissions for service: {}", serviceName, e);
            }
        }
    }
}
```

---

##### Step 3: Gateway Permission Spec Cache (Version 기반 Push + Pull) ⭐

**목적**: Gateway가 **단일 Permission Spec 객체**를 version 기반으로 관리하여 Drift 방지 및 실시간 동기화

---

###### 3.1 Permission Spec 구조

**Gateway는 전체 Permission Spec을 하나의 캐시 객체로 관리**:

```java
public record PermissionSpec(
    Long version,  // Spec 버전 (AuthHub에서 관리)
    Instant updatedAt,  // 마지막 업데이트 시각
    List<EndpointPermission> permissions  // 전체 엔드포인트 권한 목록
) {}

public record EndpointPermission(
    String serviceName,
    String path,
    String method,
    List<String> requiredPermissions,
    List<String> requiredRoles
) {}
```

**Redis Cache 키**:
- **Key**: `permission:spec`
- **Value**: `{ "version": 42, "updatedAt": "...", "permissions": [...] }`
- **TTL**: 30초 (+ Webhook 기반 즉시 갱신)

---

###### 3.2 AuthHub → Gateway Push (Webhook)

**AuthHub가 Permission Spec 변경 시 Gateway에 알림 전송**:

**Webhook Endpoint (Gateway)**:
```
POST /internal/gateway/permissions/refresh
```

**Payload**:
```json
{
  "version": 42,
  "changedServices": ["order-service", "product-service"]
}
```

**Gateway Webhook Handler**:
```java
@RestController
@RequestMapping("/internal/gateway")
public class PermissionSpecWebhookController {
    private final PermissionSpecService permissionSpecService;

    @PostMapping("/permissions/refresh")
    public ResponseEntity<Void> refreshPermissionSpec(@RequestBody PermissionSpecRefreshEvent event) {
        log.info("Received permission spec refresh webhook: version={}, changedServices={}",
            event.version(), event.changedServices());

        // 현재 캐시된 version 확인
        PermissionSpec currentSpec = permissionSpecService.getCurrentSpec();

        if (currentSpec == null || currentSpec.version() < event.version()) {
            // version이 더 최신이면 AuthHub에서 Pull
            permissionSpecService.pullLatestSpec(event.version());
        } else {
            log.debug("Current spec version is up-to-date: currentVersion={}, newVersion={}",
                currentSpec.version(), event.version());
        }

        return ResponseEntity.ok().build();
    }
}
```

---

###### 3.3 Gateway → AuthHub Pull (API 조회)

**Gateway가 AuthHub로부터 최신 Permission Spec 조회**:

**AuthHub API Endpoint**:
```
GET /internal/authhub/permissions/spec?version=42
```

**Response**:
```json
{
  "version": 42,
  "updatedAt": "2025-01-24T12:34:56Z",
  "permissions": [
    {
      "serviceName": "order-service",
      "path": "/api/v1/orders",
      "method": "POST",
      "requiredPermissions": ["order:create"],
      "requiredRoles": []
    },
    {
      "serviceName": "product-service",
      "path": "/api/v1/products",
      "method": "GET",
      "requiredPermissions": ["product:read"],
      "requiredRoles": []
    }
  ]
}
```

**Gateway Permission Spec Service**:
```java
@Service
public class PermissionSpecService {
    private final RedisTemplate<String, PermissionSpec> redisTemplate;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, PermissionSpec> localCache = new ConcurrentHashMap<>();
    private static final String CACHE_KEY = "permission:spec";

    @PostConstruct
    public void loadSpecOnStartup() {
        pullLatestSpec(null); // Gateway 시작 시 최신 Spec 로드
    }

    @Scheduled(fixedDelay = 30000) // 30초마다 주기적 갱신 (Fallback)
    public void refreshSpecPeriodically() {
        pullLatestSpec(null);
    }

    public void pullLatestSpec(Long expectedVersion) {
        try {
            // 1. AuthHub에서 최신 Permission Spec 조회
            String url = expectedVersion != null
                ? "http://authhub/internal/authhub/permissions/spec?version=" + expectedVersion
                : "http://authhub/internal/authhub/permissions/spec";

            PermissionSpec latestSpec = restTemplate.getForObject(url, PermissionSpec.class);

            if (latestSpec == null) {
                log.warn("No permission spec returned from AuthHub");
                return;
            }

            // 2. Version 검증 (Race Condition 방지)
            PermissionSpec currentSpec = getCurrentSpec();
            if (currentSpec != null && currentSpec.version() >= latestSpec.version()) {
                log.debug("Current spec is already up-to-date: currentVersion={}, latestVersion={}",
                    currentSpec.version(), latestSpec.version());
                return;
            }

            // 3. Redis에 캐싱 (TTL: 30초)
            redisTemplate.opsForValue().set(CACHE_KEY, latestSpec, Duration.ofSeconds(30));

            // 4. Local Memory Cache 동기화 (빠른 조회)
            localCache.put(CACHE_KEY, latestSpec);

            log.info("Permission spec updated: version={}, totalPermissions={}",
                latestSpec.version(), latestSpec.permissions().size());
        } catch (Exception e) {
            log.error("Failed to pull latest permission spec from AuthHub", e);
            // 기존 캐시 유지 (Fallback)
        }
    }

    public PermissionSpec getCurrentSpec() {
        // 1. Local Memory Cache 조회 (0ms latency)
        PermissionSpec spec = localCache.get(CACHE_KEY);
        if (spec != null) {
            return spec;
        }

        // 2. Redis 조회 (Fallback)
        spec = redisTemplate.opsForValue().get(CACHE_KEY);
        if (spec != null) {
            localCache.put(CACHE_KEY, spec); // Local Cache 동기화
            return spec;
        }

        // 3. Cache Miss → AuthHub에서 즉시 Pull
        pullLatestSpec(null);
        return localCache.get(CACHE_KEY);
    }

    public EndpointPermission findPermission(String path, String method) {
        PermissionSpec spec = getCurrentSpec();
        if (spec == null) {
            log.warn("No permission spec available, denying request by default");
            return null;
        }

        // Path 매칭 (정확 매칭 + PathVariable 지원)
        return spec.permissions().stream()
            .filter(p -> matchesPath(p.path(), path) && p.method().equalsIgnoreCase(method))
            .findFirst()
            .orElse(null);
    }

    private boolean matchesPath(String pattern, String actualPath) {
        // /api/v1/orders/{orderId} → /api/v1/orders/123 매칭
        String regex = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return actualPath.matches(regex);
    }
}
```

---

###### 3.4 동기화 흐름 (Push + Pull 하이브리드)

**시나리오 1: Permission 변경 발생**
```
1️⃣ Order Service: 새 엔드포인트 추가 (/api/v1/orders/cancel)
   ↓
2️⃣ AuthHub: PermissionSyncScheduler가 /actuator/permissions 스캔
   → Drift 감지 (신규 엔드포인트)
   → DB 업데이트 (version 41 → 42)
   ↓
3️⃣ AuthHub → Gateway: Webhook 전송
   POST /internal/gateway/permissions/refresh
   Payload: { "version": 42 }
   ↓
4️⃣ Gateway: Webhook 수신
   → 현재 version(41) < 새 version(42) 확인
   → AuthHub API 호출 (Pull)
   ↓
5️⃣ Gateway → AuthHub: API 조회
   GET /internal/authhub/permissions/spec?version=42
   ↓
6️⃣ Gateway: 최신 Spec 수신 (version 42)
   → Redis 캐시 갱신 (TTL: 30초)
   → Local Memory Cache 동기화
   ↓
✅ Gateway: 신규 엔드포인트 즉시 인지 (Drift 0)
```

**시나리오 2: Webhook 실패 (Fallback)**
```
1️⃣ AuthHub → Gateway: Webhook 전송 실패 (네트워크 장애)
   ↓
2️⃣ Gateway: 30초 TTL 만료 (주기적 갱신 Scheduled Task)
   → pullLatestSpec() 자동 실행
   ↓
3️⃣ Gateway → AuthHub: API 조회
   GET /internal/authhub/permissions/spec
   ↓
4️⃣ Gateway: 최신 Spec 수신
   → Redis 캐시 갱신
   ↓
✅ 최대 30초 지연으로 동기화 완료
```

---

###### 3.5 Version 기반 Race Condition 방지

**문제**: 여러 Gateway Pod가 동시에 Spec을 Pull할 때 version 충돌 가능

**해결**: Version 기반 Optimistic Lock

```java
public void pullLatestSpec(Long expectedVersion) {
    // ... AuthHub에서 Spec 조회 ...

    // Version 검증 (현재 버전 >= 새 버전이면 무시)
    PermissionSpec currentSpec = getCurrentSpec();
    if (currentSpec != null && currentSpec.version() >= latestSpec.version()) {
        log.debug("Skip update: currentVersion={} >= latestVersion={}",
            currentSpec.version(), latestSpec.version());
        return;
    }

    // Version이 더 최신일 때만 업데이트
    redisTemplate.opsForValue().set(CACHE_KEY, latestSpec, Duration.ofSeconds(30));
    localCache.put(CACHE_KEY, latestSpec);
}
```

---

##### ⚠️ 정적 파일(`api-permissions.yml`) 절대 금지 이유

| 방식 | Drift 위험 | 동기화 속도 | 관리 복잡도 | 결론 |
|------|-----------|-------------|-------------|------|
| **정적 YAML 파일** | ❌ 매우 높음 (서비스 변경 시 수동 업데이트) | ❌ 느림 (수동 배포) | ❌ 높음 (서비스마다 별도 파일) | 절대 사용 금지 |
| **AuthHub 중앙 관리 (권장)** | ✅ 낮음 (자동 스캔 + Drift 감지) | ✅ 빠름 (Webhook 기반 즉시 반영) | ✅ 낮음 (AuthHub가 단일 진실 공급원) | 필수 채택 |

---

##### 동기화 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                 Backend Service (Order, Product)                │
│  - /actuator/permissions 엔드포인트 노출                       │
│  - 필요 권한 정의 (코드 기반)                                  │
└────────────────────┬────────────────────────────────────────────┘
                     │ 1️⃣ GET /actuator/permissions (1시간마다)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AuthHub (IAM)                            │
│  - PermissionSyncScheduler가 주기적 스캔                       │
│  - Drift 감지 (신규/삭제/변경 엔드포인트)                      │
│  - DB 업데이트 (permission_policies 테이블)                    │
└────────────────────┬────────────────────────────────────────────┘
                     │ 2️⃣ POST /webhook/permission-policy-changed
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AccessGateway                              │
│  - Webhook 수신                                                │
│  - Redis 캐시 무효화                                           │
│  - AuthHub에서 최신 Policy pull                                │
│  - Redis 재캐싱 (TTL: 1시간)                                   │
└─────────────────────────────────────────────────────────────────┘
                     │ 3️⃣ 요청 처리 시 Permission 체크
                     ▼
                  Client Request
```

---

#### 2.3 Permission Policy 데이터 구조 (AuthHub DB)

**테이블: `permission_policies`**

```sql
CREATE TABLE permission_policies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_name VARCHAR(255) NOT NULL,
  path VARCHAR(500) NOT NULL,
  method VARCHAR(10) NOT NULL,
  required_permissions JSON, -- ["order:create", "order:read"]
  required_roles JSON,       -- ["ADMIN"]
  is_public BOOLEAN DEFAULT FALSE,
  is_deprecated BOOLEAN DEFAULT FALSE,
  version VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_path_method (service_name, path, method)
);
```

**예시 데이터**:
```json
{
  "serviceName": "order-service",
  "path": "/api/v1/orders",
  "method": "POST",
  "requiredPermissions": ["order:create"],
  "requiredRoles": [],
  "isPublic": false,
  "isDeprecated": false,
  "version": "1.0.0"
}
```

---

#### 2.3 PermissionFilter의 인가(Authorization) 흐름 ⭐

**목적**: Permission Spec Cache를 기반으로 실시간 권한 검증

**핵심 개념**:
- Gateway로 들어오는 **모든 요청**에 대해 Permission Spec 기반 권한 체크
- JWT Payload의 `permissions` claim과 **Required Permissions** 비교
- 매칭 성공 시 통과, 실패 시 **403 Forbidden**

---

##### 3.1 PermissionFilter 구현

**Filter Order**: `Ordered.HIGHEST_PRECEDENCE + 6` (JwtAuthenticationFilter 다음)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
public class PermissionFilter implements GlobalFilter {
    private final PermissionSpecService permissionSpecService;
    private final JwtParser jwtParser;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethodValue();

        // 1️⃣ PermissionSpecCache에서 해당 엔드포인트의 required_permission 조회
        EndpointPermission endpointPermission = permissionSpecService.findPermission(path, method);

        if (endpointPermission == null) {
            // Permission Spec에 정의되지 않은 엔드포인트 → Default Deny
            log.warn("No permission spec found for endpoint: {} {}", method, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Public 엔드포인트 체크
        if (endpointPermission.isPublic()) {
            log.debug("Public endpoint accessed: {} {}", method, path);
            return chain.filter(exchange); // 인증 없이 통과
        }

        // 2️⃣ JWT Payload에서 user.permissions 확인
        JwtClaims jwtClaims = exchange.getAttribute("jwtClaims");
        if (jwtClaims == null) {
            // JWT 없음 (JwtAuthenticationFilter에서 401 처리되어야 하지만 방어 코드)
            log.error("JWT claims not found in exchange attributes");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        List<String> userPermissions = jwtClaims.getPermissions();
        List<String> requiredPermissions = endpointPermission.requiredPermissions();

        // 3️⃣ 권한 매칭 (Required Permissions ALL 보유 여부)
        boolean hasAllPermissions = requiredPermissions.stream()
            .allMatch(userPermissions::contains);

        if (!hasAllPermissions) {
            // 4️⃣ 권한 부족 → 403 Forbidden
            log.warn("Permission denied: userId={}, tenantId={}, required={}, actual={}",
                jwtClaims.getUserId(), jwtClaims.getTenantId(),
                requiredPermissions, userPermissions);

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 5️⃣ 권한 확인 완료 → 통과
        log.debug("Permission granted: userId={}, endpoint={} {}, permissions={}",
            jwtClaims.getUserId(), method, path, requiredPermissions);

        return chain.filter(exchange);
    }
}
```

---

##### 3.2 인가 흐름 예시

**시나리오**: 사용자가 주문 생성 요청

```
1️⃣ 요청 수신
   POST /api/v1/orders
   Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

2️⃣ JwtAuthenticationFilter (Order 5)
   → JWT 검증 통과
   → JwtClaims 추출:
     {
       "userId": 123,
       "tenantId": "tenant-1",
       "permissions": ["order:read", "order:create", "product:read"],
       "roles": ["USER"]
     }
   → exchange.setAttribute("jwtClaims", claims)

3️⃣ PermissionFilter (Order 6)
   → permissionSpecService.findPermission("/api/v1/orders", "POST")
   → EndpointPermission 조회:
     {
       "path": "/api/v1/orders",
       "method": "POST",
       "requiredPermissions": ["order:create"],
       "requiredRoles": []
     }

4️⃣ 권한 매칭
   Required: ["order:create"]
   User has: ["order:read", "order:create", "product:read"]
   → "order:create" ∈ User Permissions ✅

5️⃣ 결과: 200 OK (통과)
```

**실패 시나리오**: 권한 부족

```
1️⃣ 요청 수신
   DELETE /api/v1/orders/123
   Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

2️⃣ JWT Claims:
   {
     "userId": 456,
     "permissions": ["order:read"],  ❌ order:delete 없음
     "roles": ["USER"]
   }

3️⃣ Permission Spec 조회:
   {
     "path": "/api/v1/orders/{orderId}",
     "method": "DELETE",
     "requiredPermissions": ["order:delete"]
   }

4️⃣ 권한 매칭 실패
   Required: ["order:delete"]
   User has: ["order:read"]
   → "order:delete" ∉ User Permissions ❌

5️⃣ 결과: 403 Forbidden
   {
     "error": "PERMISSION_DENIED",
     "message": "Required permission 'order:delete' not found",
     "requiredPermissions": ["order:delete"],
     "userPermissions": ["order:read"]
   }
```

---

##### 3.3 Path Matching 로직

**문제**: PathVariable이 있는 경로 매칭 (`/api/v1/orders/{orderId}`)

**해결**: Regex 기반 동적 매칭

```java
public class PermissionSpecService {
    public EndpointPermission findPermission(String actualPath, String method) {
        PermissionSpec spec = getCurrentSpec();
        if (spec == null) {
            return null;
        }

        // Permission Spec의 모든 엔드포인트 순회
        return spec.permissions().stream()
            .filter(p -> matchesPath(p.path(), actualPath))
            .filter(p -> p.method().equalsIgnoreCase(method))
            .findFirst()
            .orElse(null);
    }

    private boolean matchesPath(String pattern, String actualPath) {
        // PathVariable 패턴을 Regex로 변환
        // 예: /api/v1/orders/{orderId} → /api/v1/orders/[^/]+
        String regex = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return actualPath.matches(regex);
    }
}
```

**매칭 예시**:

| Pattern | Actual Path | Matches? |
|---------|-------------|----------|
| `/api/v1/orders` | `/api/v1/orders` | ✅ (정확 매칭) |
| `/api/v1/orders/{orderId}` | `/api/v1/orders/123` | ✅ (PathVariable 매칭) |
| `/api/v1/orders/{orderId}` | `/api/v1/orders/123/items` | ❌ (하위 경로 불일치) |
| `/api/v1/products/{productId}/reviews/{reviewId}` | `/api/v1/products/456/reviews/789` | ✅ (다중 PathVariable) |

---

##### 3.4 성능 최적화

**Local Memory Cache 활용** (PermissionSpecService):

```java
@Service
public class PermissionSpecService {
    // Redis + Local Memory 2-Tier Cache
    private final ConcurrentHashMap<String, PermissionSpec> localCache = new ConcurrentHashMap<>();
    private final RedisTemplate<String, PermissionSpec> redisTemplate;

    public PermissionSpec getCurrentSpec() {
        // 1️⃣ Local Memory Cache 조회 (0ms latency)
        PermissionSpec spec = localCache.get("permission:spec");
        if (spec != null) {
            return spec;
        }

        // 2️⃣ Redis 조회 (1-3ms latency)
        spec = redisTemplate.opsForValue().get("permission:spec");
        if (spec != null) {
            localCache.put("permission:spec", spec); // Local Cache 동기화
            return spec;
        }

        // 3️⃣ Cache Miss → AuthHub에서 즉시 Pull
        pullLatestSpec(null);
        return localCache.get("permission:spec");
    }
}
```

**성능 비교**:

| 조회 방법 | Latency | 용도 |
|----------|---------|------|
| **Local Memory** | 0ms | 99% 요청 처리 (Hot Path) |
| **Redis** | 1-3ms | Cache Eviction 후 복구 |
| **AuthHub API** | 5-15ms | Cache Miss 시 Fallback |

---

##### 3.5 에러 처리 및 Fallback

**Permission Spec이 없는 경우**:

```java
EndpointPermission endpointPermission = permissionSpecService.findPermission(path, method);

if (endpointPermission == null) {
    // Default Deny (Fail-Safe)
    log.warn("No permission spec found for endpoint: {} {}", method, path);
    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
    return exchange.getResponse().setComplete();
}
```

**JWT Claims가 없는 경우** (방어 코드):

```java
JwtClaims jwtClaims = exchange.getAttribute("jwtClaims");
if (jwtClaims == null) {
    // JwtAuthenticationFilter에서 401 처리되어야 하지만 방어
    log.error("JWT claims not found in exchange attributes");
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
}
```

**AuthHub 장애 시**:

```java
@Scheduled(fixedDelay = 30000)
public void refreshSpecPeriodically() {
    try {
        pullLatestSpec(null);
    } catch (Exception e) {
        log.error("Failed to refresh permission spec, keeping stale cache", e);
        // 기존 캐시 유지 (Graceful Degradation)
    }
}
```

**Graceful Degradation 전략**:
- AuthHub 장애 시 **기존 캐시 유지** (30초 TTL이지만 Stale 캐시 허용)
- Cache Miss + AuthHub 장애 → **403 Forbidden** (Fail-Safe)

---

#### 2.4 Permission Policy 정의 (Legacy - 참고용)

⚠️ **주의**: 아래는 정적 YAML 파일 예시로, **실제 운영에서는 사용하지 않습니다**. AuthHub 중앙 관리 방식을 사용하세요.

**api-permissions.yml** (참고용):
```yaml
# ⚠️ 이 파일은 사용하지 않음 - AuthHub에서 관리됨
# 서비스별 Permission Policy
services:
  - name: order-service
    base_path: /api/v1/orders
    endpoints:
      - path: /api/v1/orders
        method: POST
        required_permissions:
          - order:create
        required_roles: []  # 권한만 체크 (Role은 선택)

      - path: /api/v1/orders/{orderId}
        method: GET
        required_permissions:
          - order:read

      - path: /api/v1/orders/{orderId}
        method: PATCH
        required_permissions:
          - order:update

      - path: /api/v1/orders/{orderId}/cancel
        method: POST
        required_permissions:
          - order:cancel

      - path: /api/v1/orders
        method: GET
        required_permissions:
          - order:list
        required_roles:
          - ADMIN  # 관리자만 전체 주문 목록 조회 가능

  - name: product-service
    base_path: /api/v1/products
    endpoints:
      - path: /api/v1/products
        method: POST
        required_permissions:
          - product:create
        required_roles:
          - ADMIN

      - path: /api/v1/products/{productId}
        method: GET
        required_permissions: []  # Public API (인증 필요 없음)

      - path: /api/v1/products/{productId}
        method: PATCH
        required_permissions:
          - product:update
        required_roles:
          - ADMIN
```

#### 2.2 Permission 인가 로직

**검증 단계**:
1. **Endpoint 매칭**: 요청 Path + Method로 api-permissions.yml에서 Policy 조회
2. **Permission 체크**: JWT의 `permissions` claim과 `required_permissions` 비교
3. **Role 체크** (선택적): JWT의 `roles` claim과 `required_roles` 비교
4. **권한 없음**: 403 Forbidden 반환

**Java 구현 예시**:
```java
@Component
public class PermissionChecker {
    private final PermissionPolicyLoader policyLoader;

    public void checkPermission(ServerHttpRequest request, JwtClaims claims) {
        String path = request.getURI().getPath();
        String method = request.getMethodValue();

        // 1. Policy 조회
        PermissionPolicy policy = policyLoader.findPolicy(path, method);

        if (policy == null) {
            // Policy 없으면 기본적으로 거부 (Deny by Default)
            throw new PermissionDeniedException("No permission policy found");
        }

        // 2. Required Permissions 체크
        if (!policy.getRequiredPermissions().isEmpty()) {
            boolean hasPermission = claims.getPermissions().containsAll(
                policy.getRequiredPermissions()
            );
            if (!hasPermission) {
                throw new PermissionDeniedException(
                    "Missing required permissions: " + policy.getRequiredPermissions()
                );
            }
        }

        // 3. Required Roles 체크 (선택)
        if (!policy.getRequiredRoles().isEmpty()) {
            boolean hasRole = claims.getRoles().stream()
                .anyMatch(policy.getRequiredRoles()::contains);
            if (!hasRole) {
                throw new PermissionDeniedException(
                    "Missing required roles: " + policy.getRequiredRoles()
                );
            }
        }
    }
}
```

#### 2.3 Permission Policy 캐싱

**캐싱 전략**:
- **캐시 저장소**: Redis (분산 캐시)
- **캐시 키**: `permission_policy:{service}:{path}:{method}`
- **TTL**: 1시간
- **갱신 전략**:
  - ✅ **초기 로드**: Gateway 시작 시 api-permissions.yml 파싱 후 Redis 저장
  - ✅ **Hot Reload**: api-permissions.yml 변경 시 `/admin/reload-permissions` 엔드포인트로 갱신
  - ✅ **Cache Miss**: Redis에 없으면 api-permissions.yml에서 로드 후 캐싱

---

### 3. 자동 토큰 재발급

#### 3.1 토큰 재발급 시나리오

**시나리오 1: Access Token 만료 감지**
```
Client → Gateway: GET /api/v1/orders
                  Authorization: Bearer {expiredAccessToken}
                  Cookie: refresh_token={refreshToken}

Gateway (JwtAuthenticationFilter):
  1. JWT 검증 실패 (JwtExpiredException)
  2. Refresh Token 추출 (Cookie)

Gateway (TokenRefreshFilter):
  3. AuthHub에 재발급 요청
     POST /api/v1/auth/refresh
     Body: { "refreshToken": "..." }
  4. 새 Access Token 수령
  5. Response Header에 추가
     X-New-Access-Token: {newAccessToken}
  6. 새 Refresh Token을 Cookie에 추가 (Rotation)
     Set-Cookie: refresh_token={newRefreshToken}; HttpOnly; Secure; SameSite=Strict
  7. 원래 요청 재시도 (새 Access Token 사용)

Backend Service ← Gateway: GET /api/v1/orders
                           Authorization: Bearer {newAccessToken}
                           X-User-Id: 123
                           X-Tenant-Id: tenant-1

Client ← Gateway: 200 OK
                  X-New-Access-Token: {newAccessToken}
                  Set-Cookie: refresh_token={newRefreshToken}
```

**시나리오 2: Refresh Token도 만료**
```
Client → Gateway: GET /api/v1/orders
                  Authorization: Bearer {expiredAccessToken}
                  Cookie: refresh_token={expiredRefreshToken}

Gateway (TokenRefreshFilter):
  1. AuthHub에 재발급 요청
     POST /api/v1/auth/refresh
     Body: { "refreshToken": "..." }
  2. 401 Unauthorized 수령 (Refresh Token 만료)

Client ← Gateway: 401 Unauthorized
                  WWW-Authenticate: Bearer error="token_expired"

Client → Login Page (자동 리다이렉트)
```

#### 3.2 Race Condition 방지

**문제 상황**: 동일 사용자가 여러 탭에서 동시에 API 요청 시 Refresh Token 중복 사용

**해결 전략**: AuthHub의 Refresh Token Rotation과 Gateway의 Redis Lock 조합

**Java 구현 예시**:
```java
@Component
public class TokenRefreshService {
    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate;

    public TokenPair refreshAccessToken(String refreshToken) {
        String lockKey = "token:refresh:" + refreshToken;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3초 대기, 5초 동안 Lock 유지
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ConcurrentRefreshException("Token refresh in progress");
            }

            // AuthHub에 재발급 요청
            RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
            RefreshTokenResponse response = restTemplate.postForObject(
                "http://authhub/api/v1/auth/refresh",
                request,
                RefreshTokenResponse.class
            );

            return new TokenPair(response.accessToken(), response.refreshToken());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TokenRefreshException("Token refresh interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

### 4. Trace-ID 생성 및 전달

#### 4.1 Trace-ID 생성 규칙

**형식**: `{timestamp}-{randomUUID}`
- 예: `20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789`

**생성 시점**: Gateway 진입 시 (TraceIdFilter, 첫 번째 Filter)

**전달 방식**:
- **Downstream (Backend Services)**: `X-Trace-Id` 헤더
- **Response (Client)**: `X-Trace-Id` 헤더 (동일한 Trace-ID 반환)
- **Audit Log**: 모든 로그에 Trace-ID 포함

**Java 구현 예시**:
```java
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = generateTraceId();

        // 1. Request에 Trace-ID 추가 (Downstream으로 전달)
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-Trace-Id", traceId)
            .build();

        // 2. Response에 Trace-ID 추가 (Client로 반환)
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);

        // 3. MDC에 Trace-ID 추가 (로깅용)
        MDC.put("traceId", traceId);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private String generateTraceId() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
        );
        String uuid = UUID.randomUUID().toString();
        return timestamp + "-" + uuid;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 첫 번째 Filter
    }
}
```

---

### 5. 멀티테넌트 격리 및 라우팅 ⭐

#### 5.1 Tenant Config Cache (테넌트별 동작 제어)

**문제 인식**: 단순 도메인/Path 기반 라우팅만으로는 테넌트별 정책 차이를 반영하지 못함

**해결 전략**: Tenant Config를 Redis에 캐싱하여 테넌트별로 다른 보안 정책 적용

---

##### Tenant Config 데이터 구조

**Redis 저장소**: `tenant_config:{tenantId}`

**Config 항목**:
```json
{
  "tenantId": "tenant-1",
  "mfaRequired": true,
  "allowedSocialLogins": ["kakao", "naver"],
  "roleHierarchy": {
    "ADMIN": ["order:*", "product:*", "user:*"],
    "USER": ["order:read", "order:create", "product:read"]
  },
  "sessionConfig": {
    "maxActiveSessions": 3,
    "accessTokenTTL": 900,
    "refreshTokenTTL": 604800
  },
  "rateLimitConfig": {
    "loginAttemptsPerHour": 10,
    "otpRequestsPerHour": 3
  }
}
```

---

##### Tenant Config 로드 및 캐싱

**프로세스**:
```
1. Gateway 시작 시:
   - AuthHub의 GET /api/v1/tenants/configs 호출
   - 모든 Tenant Config 조회
   - Redis에 캐싱 (TTL: 1시간)

2. Tenant 생성/수정 시:
   - AuthHub가 Webhook 전송
     POST http://gateway/webhook/tenant-config-changed
   - Gateway는 Redis 캐시 무효화 후 재로드

3. 요청 처리 시:
   - JWT에서 tenantId 추출
   - Redis에서 Tenant Config 조회
   - Config 기반으로 Filter 동작 변경
```

**Java 구현 예시 (Gateway)**:
```java
@Component
public class TenantConfigLoader {
    private final RedisTemplate<String, TenantConfig> redisTemplate;
    private final RestTemplate restTemplate;

    @PostConstruct
    public void loadTenantConfigsOnStartup() {
        loadConfigsFromAuthHub();
    }

    public void loadConfigsFromAuthHub() {
        try {
            TenantConfigResponse response = restTemplate.getForObject(
                "http://authhub/api/v1/tenants/configs",
                TenantConfigResponse.class
            );

            for (TenantConfig config : response.getConfigs()) {
                String cacheKey = "tenant_config:" + config.getTenantId();
                redisTemplate.opsForValue().set(cacheKey, config, Duration.ofHours(1));
            }

            log.info("Tenant configs loaded: {} tenants", response.getConfigs().size());
        } catch (Exception e) {
            log.error("Failed to load tenant configs", e);
            throw new TenantConfigLoadException("Cannot start without tenant configs", e);
        }
    }

    public TenantConfig getTenantConfig(String tenantId) {
        String cacheKey = "tenant_config:" + tenantId;
        TenantConfig config = redisTemplate.opsForValue().get(cacheKey);

        if (config == null) {
            // Cache Miss → AuthHub API 호출
            config = restTemplate.getForObject(
                "http://authhub/api/v1/tenants/{tenantId}/config",
                TenantConfig.class,
                tenantId
            );

            if (config != null) {
                redisTemplate.opsForValue().set(cacheKey, config, Duration.ofHours(1));
            }
        }

        return config;
    }
}
```

---

##### Tenant Config 기반 동적 정책 적용

###### 1️⃣ MFA 필수 여부 체크

**시나리오**: Tenant A는 MFA 필수, Tenant B는 선택

**적용 로직**:
```java
@Component
public class MfaRequiredFilter implements GlobalFilter {
    private final TenantConfigLoader configLoader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        JwtClaims claims = exchange.getAttribute("jwtClaims");
        if (claims == null) {
            return chain.filter(exchange);
        }

        // Tenant Config 조회
        TenantConfig config = configLoader.getTenantConfig(claims.getTenantId());

        if (config.isMfaRequired() && !claims.isMfaVerified()) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String errorBody = """
                {
                  "errorCode": "MFA_REQUIRED",
                  "message": "This tenant requires MFA verification"
                }
                """;

            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
            );
        }

        return chain.filter(exchange);
    }
}
```

---

###### 2️⃣ 소셜 로그인 허용 여부 체크

**시나리오**: Tenant A는 Kakao만 허용, Tenant B는 Kakao + Naver 허용

**적용 로직**:
```java
@Component
public class SocialLoginFilter implements GlobalFilter {
    private final TenantConfigLoader configLoader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isSocialLoginRequest(exchange)) {
            return chain.filter(exchange);
        }

        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        String provider = extractSocialProvider(exchange); // "kakao", "naver" 등

        TenantConfig config = configLoader.getTenantConfig(tenantId);

        if (!config.getAllowedSocialLogins().contains(provider)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String errorBody = String.format("""
                {
                  "errorCode": "SOCIAL_LOGIN_NOT_ALLOWED",
                  "message": "Provider '%s' is not allowed for this tenant"
                }
                """, provider);

            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorBody.getBytes()))
            );
        }

        return chain.filter(exchange);
    }
}
```

---

###### 3️⃣ Role Hierarchy 기반 Permission 확장

**시나리오**: Tenant A의 ADMIN은 `order:*` 와일드카드 권한 보유

**적용 로직**:
```java
@Component
public class PermissionValidator {
    private final TenantConfigLoader configLoader;

    public void validatePermission(JwtClaims claims, String requiredPermission) {
        // 1. Tenant Config 조회
        TenantConfig config = configLoader.getTenantConfig(claims.getTenantId());

        // 2. Role Hierarchy에서 Permission 확장
        Set<String> expandedPermissions = expandPermissions(
            claims.getRoles(),
            config.getRoleHierarchy()
        );

        // 3. 확장된 Permission으로 체크
        if (!hasPermission(expandedPermissions, requiredPermission)) {
            throw new PermissionDeniedException("Missing permission: " + requiredPermission);
        }
    }

    private Set<String> expandPermissions(List<String> roles, Map<String, List<String>> roleHierarchy) {
        Set<String> permissions = new HashSet<>();

        for (String role : roles) {
            List<String> rolePermissions = roleHierarchy.get(role);
            if (rolePermissions != null) {
                permissions.addAll(rolePermissions);
            }
        }

        return permissions;
    }

    private boolean hasPermission(Set<String> permissions, String requiredPermission) {
        // 와일드카드 체크 (order:* → order:read, order:create 등)
        for (String permission : permissions) {
            if (permission.endsWith(":*")) {
                String prefix = permission.substring(0, permission.length() - 1);
                if (requiredPermission.startsWith(prefix)) {
                    return true;
                }
            } else if (permission.equals(requiredPermission)) {
                return true;
            }
        }

        return false;
    }
}
```

---

###### 4️⃣ 테넌트별 Rate Limit 차등 적용

**시나리오**: Enterprise Tenant는 높은 Rate Limit, Free Tenant는 낮은 Rate Limit

**적용 로직**:
```java
@Component
public class RateLimitFilter implements GlobalFilter {
    private final TenantConfigLoader configLoader;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        JwtClaims claims = exchange.getAttribute("jwtClaims");
        if (claims == null) {
            return chain.filter(exchange);
        }

        // Tenant Config 조회
        TenantConfig config = configLoader.getTenantConfig(claims.getTenantId());

        // 테넌트별 Rate Limit 적용
        String key = "rate_limit:tenant:" + claims.getTenantId() + ":user:" + claims.getUserId();
        int maxRequests = config.getRateLimitConfig().getLoginAttemptsPerHour();

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }

        if (count > maxRequests) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

##### Tenant Config 변경 시 즉시 반영 (Webhook)

**프로세스**:
```
1. Admin → AuthHub: PATCH /api/v1/tenants/{tenantId}/config
                     Body: { "mfaRequired": true }

2. AuthHub:
   - Tenant Config 업데이트 (DB UPDATE)
   - Webhook 전송 (Gateway에게 알림)
     POST http://gateway/webhook/tenant-config-changed
     Body: { "tenantId": "tenant-1" }

3. Gateway (WebhookController):
   - Redis 캐시 삭제
     DEL tenant_config:tenant-1
   - 다음 요청 시 AuthHub에서 새 Config pull
```

**Java 구현 예시 (Gateway Webhook)**:
```java
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private final TenantConfigLoader configLoader;
    private final RedisTemplate<String, TenantConfig> redisTemplate;

    @PostMapping("/tenant-config-changed")
    public ResponseEntity<Void> onTenantConfigChanged(@RequestBody TenantConfigChangedEvent event) {
        String cacheKey = "tenant_config:" + event.tenantId();

        // Redis 캐시 무효화
        redisTemplate.delete(cacheKey);

        log.info("Tenant config cache invalidated: tenantId={}", event.tenantId());

        return ResponseEntity.ok().build();
    }
}
```

---

#### 5.2 Tenant Context 전달 (기본 기능)

**Tenant 식별**:
- **Source**: JWT의 `tenantId` claim
- **Destination**: `X-Tenant-Id` 헤더 (Backend Services로 전달)

**Backend Service 처리**:
- **X-Tenant-Id 헤더 신뢰**: Gateway에서 검증된 Tenant ID 사용
- **Query 자동 필터링**: JPA Query에 자동으로 `WHERE tenant_id = ?` 추가 (Hibernate Filter)

**Java 구현 예시 (Gateway)**:
```java
@Component
public class TenantContextFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // JwtAuthenticationFilter에서 추출한 JWT Claims
        JwtClaims claims = exchange.getAttribute("jwtClaims");

        // X-Tenant-Id 헤더 추가
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("X-Tenant-Id", claims.getTenantId())
            .header("X-User-Id", String.valueOf(claims.getUserId()))
            .header("X-Permissions", String.join(",", claims.getPermissions()))
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
}
```

**Java 구현 예시 (Backend Service - Hibernate Filter)**:
```java
@Entity
@Table(name = "orders")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // ... other fields
}

@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Autowired
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null) {
            throw new TenantNotFoundException("X-Tenant-Id header missing");
        }

        // Hibernate Filter 활성화
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);

        return true;
    }
}
```

---

### 6. Rate Limiting & Abuse Protection

#### 6.1 Rate Limit 정책 확장 ⭐

**문제 인식**: 기본 Rate Limit만으로는 Brute Force, Credential Stuffing, DDoS 공격을 막기 부족

**해결 전략**: 보안 취약점별로 세밀한 Rate Limit 정책 적용

---

##### Rate Limit 규칙 (확장)

| Limit Type | Key Pattern | Max Requests | Window | 초과 시 조치 | HTTP Status | Audit Log |
|------------|-------------|--------------|--------|-------------|-------------|-----------|
| **Endpoint별** | `rate_limit:endpoint:{path}:{method}` | 1,000 req/min | 1분 | 429 Too Many Requests | 429 | ❌ |
| **User별** | `rate_limit:user:{userId}` | 100 req/min | 1분 | 429 Too Many Requests | 429 | ❌ |
| **IP별** | `rate_limit:ip:{ipAddress}` | 50 req/min | 1분 | 429, 5분 후 차단 해제 | 429 | ✅ (초과 시) |
| **🔐 OTP 요청** | `rate_limit:otp:{phoneNumber}` | **3 req/hour** | **1시간** | 1시간 대기, Audit Log 기록 | 429 | ✅ (필수) |
| **🔐 Login API (IP)** | `rate_limit:login:{ipAddress}` | **5 req/5min** | **5분** | 5분 대기, Brute Force 감지 | 429 | ✅ (필수) |
| **🔐 Token Refresh** | `rate_limit:token_refresh:{userId}` | **3 req/min** | **1분** | 재발급 차단, Suspicious Activity | 429 | ✅ (필수) |
| **🔐 잘못된 JWT 제출** | `rate_limit:invalid_jwt:{ipAddress}` | **10 req/5min** | **5분** | **IP 차단 30분**, Audit Log | 403 | ✅ (필수) |
| **🔐 Password 로그인 실패** | `rate_limit:password_fail:{email}` | **5 req/10min** | **10분** | **계정 잠금 30분** (ACCOUNT_LOCKED) | 403 | ✅ (필수) |

---

##### 보안 공격 방어 메커니즘

###### 1️⃣ OTP 요청 남용 방지

**공격 시나리오**: 악의적 사용자가 타인의 전화번호로 무작위 OTP 요청 (SMS 폭탄)

**방어 전략**:
- **Rate Limit**: 동일 전화번호로 1시간 3회 제한
- **초과 시 조치**:
  1. 429 Too Many Requests 반환
  2. Audit Log 기록 (`OTP_ABUSE_DETECTED`)
  3. 1시간 후 자동 해제
- **예외**: Admin이 수동으로 해제 가능 (`DELETE /admin/rate-limit/otp/{phoneNumber}`)

**Java 구현 예시**:
```java
@Component
public class OtpRateLimitFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isOtpRequest(exchange)) {
            return chain.filter(exchange);
        }

        String phoneNumber = extractPhoneNumber(exchange);
        String key = "rate_limit:otp:" + phoneNumber;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }

        if (count > 3) {
            // Audit Log 기록
            auditLogService.log(AuditEventType.OTP_ABUSE_DETECTED, Map.of(
                "phoneNumber", phoneNumber,
                "attemptCount", count
            ));

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

###### 2️⃣ Brute Force 공격 방지 (Login API)

**공격 시나리오**: 동일 IP에서 다양한 사용자 계정으로 무작위 로그인 시도

**방어 전략**:
- **Rate Limit**: 동일 IP로 5분 5회 제한
- **초과 시 조치**:
  1. 429 Too Many Requests 반환
  2. Audit Log 기록 (`BRUTE_FORCE_DETECTED`)
  3. 5분 후 자동 해제
  4. **추가**: 10회 초과 시 IP 차단 30분

**Java 구현 예시**:
```java
@Component
public class LoginRateLimitFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isLoginRequest(exchange)) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange);
        String key = "rate_limit:login:" + clientIp;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(5));
        }

        if (count > 5) {
            // Audit Log 기록
            auditLogService.log(AuditEventType.BRUTE_FORCE_DETECTED, Map.of(
                "ipAddress", clientIp,
                "attemptCount", count
            ));

            // 10회 초과 시 IP 차단 30분
            if (count > 10) {
                String blockKey = "ip_block:" + clientIp;
                redisTemplate.opsForValue().set(blockKey, "BLOCKED", Duration.ofMinutes(30));

                log.warn("IP blocked due to brute force attack: {}", clientIp);
            }

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

###### 3️⃣ Refresh Token 남용 방지

**공격 시나리오**: 악의적 사용자가 탈취한 Refresh Token으로 연속 재발급 시도

**방어 전략**:
- **Rate Limit**: 동일 사용자로 1분 3회 제한
- **초과 시 조치**:
  1. 429 Too Many Requests 반환
  2. Audit Log 기록 (`TOKEN_REFRESH_ABUSE_DETECTED`)
  3. **즉시 Refresh Token 무효화** (보안 강화)
  4. 사용자는 재로그인 필요

**Java 구현 예시**:
```java
@Component
public class TokenRefreshRateLimitFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isTokenRefreshRequest(exchange)) {
            return chain.filter(exchange);
        }

        JwtClaims claims = exchange.getAttribute("jwtClaims");
        if (claims == null) {
            return chain.filter(exchange);
        }

        Long userId = claims.getUserId();
        String key = "rate_limit:token_refresh:" + userId;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count > 3) {
            // Audit Log 기록
            auditLogService.log(AuditEventType.TOKEN_REFRESH_ABUSE_DETECTED, Map.of(
                "userId", userId,
                "attemptCount", count
            ));

            // Refresh Token 무효화 (AuthHub API 호출)
            restTemplate.postForObject(
                "http://authhub/api/v1/auth/revoke-refresh-token?userId=" + userId,
                null,
                Void.class
            );

            log.warn("Refresh Token revoked due to abuse: userId={}", userId);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

###### 4️⃣ 잘못된 JWT 반복 제출 방지

**공격 시나리오**: 악의적 사용자가 무작위 JWT를 생성하여 반복 제출 (Token 탈취 시도)

**방어 전략**:
- **Rate Limit**: 동일 IP로 5분 10회 제한
- **초과 시 조치**:
  1. 403 Forbidden 반환
  2. Audit Log 기록 (`INVALID_JWT_ABUSE_DETECTED`)
  3. **IP 차단 30분** (보안 강화)

**Java 구현 예시**:
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter {
    private final JwtValidator jwtValidator;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditLogService auditLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accessToken = extractAccessToken(exchange);
        if (accessToken == null) {
            return chain.filter(exchange);
        }

        try {
            JwtClaims claims = jwtValidator.validate(accessToken);
            exchange.getAttributes().put("jwtClaims", claims);
            return chain.filter(exchange);
        } catch (InvalidJwtException e) {
            // 잘못된 JWT 카운터 증가
            String clientIp = getClientIp(exchange);
            String key = "rate_limit:invalid_jwt:" + clientIp;

            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(5));
            }

            if (count > 10) {
                // Audit Log 기록
                auditLogService.log(AuditEventType.INVALID_JWT_ABUSE_DETECTED, Map.of(
                    "ipAddress", clientIp,
                    "attemptCount", count
                ));

                // IP 차단 30분
                String blockKey = "ip_block:" + clientIp;
                redisTemplate.opsForValue().set(blockKey, "BLOCKED", Duration.ofMinutes(30));

                log.warn("IP blocked due to invalid JWT abuse: {}", clientIp);

                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
```

---

###### 5️⃣ Password 로그인 실패 → 계정 잠금

**공격 시나리오**: 특정 계정을 타겟으로 무작위 비밀번호 시도

**방어 전략**:
- **Rate Limit**: 동일 이메일로 10분 5회 제한
- **초과 시 조치**:
  1. 403 Forbidden 반환
  2. Audit Log 기록 (`PASSWORD_LOGIN_FAILED`)
  3. **계정 잠금 30분** (`user_status = ACCOUNT_LOCKED`)
  4. 사용자는 이메일로 잠금 해제 링크 수신

**Java 구현 예시 (AuthHub - Login UseCase)**:
```java
@Transactional
public AuthTokenResponse loginWithPassword(LoginWithPasswordCommand command) {
    String email = command.email();
    String key = "rate_limit:password_fail:" + email;

    // 1. 비밀번호 검증
    User user = loadUserPort.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    if (!passwordEncoder.matches(command.password(), user.getPassword())) {
        // 2. 로그인 실패 카운터 증가
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(10));
        }

        // 3. 5회 초과 시 계정 잠금
        if (count > 5) {
            user.lock("BRUTE_FORCE_DETECTED");
            saveUserPort.save(user);

            // Audit Log 기록
            auditLogService.log(AuditEventType.ACCOUNT_LOCKED, Map.of(
                "userId", user.getId(),
                "email", email,
                "reason", "Too many password failures"
            ));

            throw new AccountLockedException("Account locked due to too many failures");
        }

        throw new InvalidPasswordException("Invalid password");
    }

    // 4. 로그인 성공 시 카운터 리셋
    redisTemplate.delete(key);

    return issueTokens(user);
}
```

---

##### Rate Limit 정책 비교표

| 공격 유형 | Rate Limit 전 | Rate Limit 후 | 효과 |
|----------|---------------|---------------|------|
| **SMS 폭탄** | 무제한 OTP 요청 가능 | 1시간 3회 제한 | ✅ SMS 비용 절감 + 사용자 보호 |
| **Brute Force** | 무제한 로그인 시도 가능 | 5분 5회 제한 + IP 차단 | ✅ 계정 탈취 방지 |
| **Token Refresh 남용** | 무제한 재발급 가능 | 1분 3회 제한 + Token 무효화 | ✅ Token 탈취 피해 최소화 |
| **JWT 무작위 제출** | 무제한 검증 시도 가능 | 5분 10회 제한 + IP 차단 | ✅ Token 탈취 시도 차단 |
| **Credential Stuffing** | 무제한 비밀번호 시도 | 10분 5회 제한 + 계정 잠금 | ✅ 계정 탈취 방지 |

**Java 구현 예시**:
```java
@Component
public class RateLimitFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitPolicyLoader policyLoader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethodValue();

        // 1. Policy 조회
        RateLimitPolicy policy = policyLoader.findPolicy(path, method);
        if (policy == null) {
            return chain.filter(exchange); // Policy 없으면 통과
        }

        // 2. Rate Limit Key 생성
        String key = buildRateLimitKey(exchange, policy);

        // 3. Redis Increment
        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == 1) {
            // 첫 요청이면 TTL 설정
            redisTemplate.expire(key, policy.getWindow());
        }

        // 4. Limit 초과 체크
        if (currentCount > policy.getMaxRequests()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                String.valueOf(policy.getMaxRequests()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");

            // Audit Log 기록
            auditLogService.log(AuditEventType.RATE_LIMIT_EXCEEDED, exchange);

            return exchange.getResponse().setComplete();
        }

        // 5. Response Header에 Rate Limit 정보 추가
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
            String.valueOf(policy.getMaxRequests()));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
            String.valueOf(policy.getMaxRequests() - currentCount));

        return chain.filter(exchange);
    }

    private String buildRateLimitKey(ServerWebExchange exchange, RateLimitPolicy policy) {
        return switch (policy.getLimitType()) {
            case ENDPOINT -> "rate_limit:endpoint:" + exchange.getRequest().getURI().getPath()
                + ":" + exchange.getRequest().getMethodValue();
            case USER -> {
                JwtClaims claims = exchange.getAttribute("jwtClaims");
                yield "rate_limit:user:" + claims.getUserId();
            }
            case IP -> "rate_limit:ip:" + getClientIp(exchange);
        };
    }
}
```

#### 6.2 Abuse Protection (DDoS 방어)

**IP 차단 전략**:
- **자동 차단**: 1분에 50회 초과 시 5분 차단
- **수동 차단**: Admin API로 특정 IP 차단 (영구 또는 기간 지정)
- **Whitelist**: 신뢰할 수 있는 IP (사내망, 파트너사 등)

**Java 구현 예시**:
```java
@Component
public class IpBlockingFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange);

        // 1. Blacklist 체크
        String blockKey = "ip_block:" + clientIp;
        if (redisTemplate.hasKey(blockKey)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

### 7. Metrics & Audit Logging

#### 7.1 Prometheus Metrics

**수집 Metrics**:

| Metric Name | Type | Labels | Description |
|-------------|------|--------|-------------|
| `gateway_request_total` | Counter | `path`, `method`, `status` | 총 요청 수 |
| `gateway_request_duration_seconds` | Histogram | `path`, `method` | 요청 처리 시간 (P50, P95, P99) |
| `gateway_jwt_validation_total` | Counter | `result` (success/failure) | JWT 검증 결과 |
| `gateway_permission_check_total` | Counter | `result` (granted/denied), `path` | Permission 체크 결과 |
| `gateway_token_refresh_total` | Counter | `result` (success/failure) | 토큰 재발급 결과 |
| `gateway_rate_limit_exceeded_total` | Counter | `limit_type`, `path` | Rate Limit 초과 횟수 |

**Java 구현 예시**:
```java
@Component
public class MetricsFilter implements GlobalFilter {
    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;

            // Request Duration
            meterRegistry.timer("gateway_request_duration_seconds",
                "path", exchange.getRequest().getURI().getPath(),
                "method", exchange.getRequest().getMethodValue()
            ).record(duration, TimeUnit.MILLISECONDS);

            // Request Total
            meterRegistry.counter("gateway_request_total",
                "path", exchange.getRequest().getURI().getPath(),
                "method", exchange.getRequest().getMethodValue(),
                "status", String.valueOf(exchange.getResponse().getStatusCode().value())
            ).increment();
        });
    }
}
```

#### 7.2 Audit Log 정책

**기록 대상 이벤트**:

| Event Type | 기록 시점 | 보관 기간 | 포함 정보 |
|------------|----------|----------|----------|
| `JWT_VALIDATION_FAILED` | JWT 검증 실패 시 | 90일 | IP, Path, Reason (expired/invalid signature) |
| `PERMISSION_DENIED` | Permission 체크 실패 시 | 90일 | User ID, Tenant ID, Path, Required Permissions |
| `RATE_LIMIT_EXCEEDED` | Rate Limit 초과 시 | 30일 | IP, User ID, Path, Limit Type |
| `TOKEN_REFRESHED` | 토큰 재발급 성공 시 | 90일 | User ID, Tenant ID, IP |
| `SUSPICIOUS_ACTIVITY` | 비정상 패턴 감지 시 | 1년 | IP, User ID, Pattern Type (예: 짧은 시간 다중 로그인) |

**Audit Log 전송 전략**:
- **비동기 전송**: Kafka Topic (`audit-log-events`)로 전송
- **실패 시 재시도**: 최대 3회 재시도 (Exponential Backoff)
- **로컬 Fallback**: Kafka 장애 시 로컬 파일 로그 저장

**Java 구현 예시**:
```java
@Component
public class AuditLogService {
    private final KafkaTemplate<String, AuditLogEvent> kafkaTemplate;

    public void log(AuditEventType eventType, ServerWebExchange exchange) {
        AuditLogEvent event = AuditLogEvent.builder()
            .eventType(eventType)
            .timestamp(Instant.now())
            .traceId(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
            .userId(extractUserId(exchange))
            .tenantId(extractTenantId(exchange))
            .ipAddress(getClientIp(exchange))
            .path(exchange.getRequest().getURI().getPath())
            .method(exchange.getRequest().getMethodValue())
            .build();

        // Kafka로 비동기 전송
        kafkaTemplate.send("audit-log-events", event)
            .addCallback(
                success -> log.debug("Audit log sent: {}", event),
                failure -> log.error("Failed to send audit log", failure)
            );
    }
}
```

---

#### 7.3 로그 포맷 표준 ⭐

**목적**: 운영 입장에서 필수적인 로그 표준화 (ELK Stack, Grafana Loki 등 로그 분석 플랫폼 연동)

---

##### 표준 로그 필드

**모든 로그에 필수로 포함되어야 하는 필드**:

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| **timestamp** | ISO 8601 | 로그 발생 시각 (UTC) | `2025-01-24T12:34:56.789Z` |
| **traceId** | String (UUID) | 요청 추적 ID (전체 플로우 추적) | `20250124123456-a1b2c3d4` |
| **userId** | Long | 사용자 ID (인증된 경우) | `123` |
| **tenantId** | String | 테넌트 ID (멀티테넌트 환경) | `tenant-1` |
| **orgId** | Long | 조직 ID (테넌트 내 조직) | `456` |
| **roles** | Array | 사용자 역할 목록 | `["USER", "ADMIN"]` |
| **permissions** | Array | 사용자 권한 목록 (주요 권한만) | `["order:read", "product:*"]` |
| **path** | String | 요청 경로 | `/api/v1/orders` |
| **method** | String | HTTP 메서드 | `POST` |
| **statusCode** | Integer | HTTP 응답 코드 | `200` |
| **elapsedTime** | Long (ms) | 요청 처리 시간 | `45` |
| **errorCode** | String | 에러 코드 (실패 시) | `PERMISSION_DENIED` |
| **errorMessage** | String | 에러 메시지 (실패 시) | `Missing permission: order:create` |
| **clientIp** | String | 클라이언트 IP 주소 | `192.168.1.100` |
| **userAgent** | String | 클라이언트 User-Agent | `Mozilla/5.0 ...` |

---

##### 로그 레벨별 출력 형식

**1️⃣ INFO 레벨 (정상 요청)**

```json
{
  "timestamp": "2025-01-24T12:34:56.789Z",
  "level": "INFO",
  "traceId": "20250124123456-a1b2c3d4",
  "userId": 123,
  "tenantId": "tenant-1",
  "orgId": 456,
  "roles": ["USER"],
  "permissions": ["order:read", "order:create"],
  "path": "/api/v1/orders",
  "method": "POST",
  "statusCode": 201,
  "elapsedTime": 45,
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
  "message": "Request processed successfully"
}
```

**2️⃣ WARN 레벨 (Rate Limit 초과)**

```json
{
  "timestamp": "2025-01-24T12:35:12.456Z",
  "level": "WARN",
  "traceId": "20250124123512-e5f6g7h8",
  "userId": 123,
  "tenantId": "tenant-1",
  "orgId": 456,
  "roles": ["USER"],
  "path": "/api/v1/orders",
  "method": "POST",
  "statusCode": 429,
  "elapsedTime": 5,
  "clientIp": "192.168.1.100",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "errorMessage": "Too many requests: 10 requests in 1 minute (limit: 5)",
  "rateLimitType": "endpoint",
  "rateLimitKey": "rate_limit:endpoint:/api/v1/orders:123",
  "message": "Rate limit exceeded"
}
```

**3️⃣ ERROR 레벨 (Permission 거부)**

```json
{
  "timestamp": "2025-01-24T12:36:23.123Z",
  "level": "ERROR",
  "traceId": "20250124123623-i9j0k1l2",
  "userId": 123,
  "tenantId": "tenant-1",
  "orgId": 456,
  "roles": ["USER"],
  "permissions": ["order:read"],
  "path": "/api/v1/products",
  "method": "POST",
  "statusCode": 403,
  "elapsedTime": 8,
  "clientIp": "192.168.1.100",
  "errorCode": "PERMISSION_DENIED",
  "errorMessage": "Missing permission: product:create",
  "requiredPermissions": ["product:create"],
  "userPermissions": ["order:read"],
  "message": "Permission denied"
}
```

**4️⃣ ERROR 레벨 (JWT 검증 실패)**

```json
{
  "timestamp": "2025-01-24T12:37:45.678Z",
  "level": "ERROR",
  "traceId": "20250124123745-m3n4o5p6",
  "path": "/api/v1/orders",
  "method": "GET",
  "statusCode": 401,
  "elapsedTime": 3,
  "clientIp": "192.168.1.100",
  "errorCode": "JWT_EXPIRED",
  "errorMessage": "JWT token expired at 2025-01-24T12:00:00Z",
  "jwtIssuer": "authhub",
  "jwtExpiredAt": "2025-01-24T12:00:00Z",
  "message": "JWT validation failed"
}
```

**5️⃣ ERROR 레벨 (Circuit Breaker OPEN)**

```json
{
  "timestamp": "2025-01-24T12:38:56.234Z",
  "level": "ERROR",
  "traceId": "20250124123856-q7r8s9t0",
  "userId": 123,
  "tenantId": "tenant-1",
  "path": "/api/v1/orders",
  "method": "POST",
  "statusCode": 503,
  "elapsedTime": 2,
  "clientIp": "192.168.1.100",
  "errorCode": "SERVICE_UNAVAILABLE",
  "errorMessage": "AuthHub is unavailable, using stale cache",
  "circuitBreakerState": "OPEN",
  "circuitBreakerName": "authhub",
  "fallbackUsed": "stale_cache",
  "message": "Circuit breaker triggered"
}
```

---

##### Java 구현 예시 (Structured Logging)

**LoggingFilter (모든 요청에 대한 표준 로그 출력)**:
```java
@Component
public class StructuredLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingFilter.class);
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            // 표준 로그 포맷 생성
            StructuredLog logEntry = buildStructuredLog(exchange, elapsedTime);

            // JSON 직렬화
            try {
                String logJson = objectMapper.writeValueAsString(logEntry);

                // 로그 레벨 결정
                HttpStatus statusCode = exchange.getResponse().getStatusCode();
                if (statusCode == null || statusCode.is2xxSuccessful()) {
                    log.info(logJson);
                } else if (statusCode.is4xxClientError()) {
                    if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                        log.warn(logJson);
                    } else {
                        log.error(logJson);
                    }
                } else if (statusCode.is5xxServerError()) {
                    log.error(logJson);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize structured log", e);
            }
        });
    }

    private StructuredLog buildStructuredLog(ServerWebExchange exchange, long elapsedTime) {
        JwtClaims claims = exchange.getAttribute("jwtClaims");

        return StructuredLog.builder()
            .timestamp(Instant.now())
            .level(determineLogLevel(exchange))
            .traceId(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
            .userId(claims != null ? claims.getUserId() : null)
            .tenantId(claims != null ? claims.getTenantId() : null)
            .orgId(claims != null ? claims.getOrgId() : null)
            .roles(claims != null ? claims.getRoles() : List.of())
            .permissions(claims != null ? claims.getPermissions() : List.of())
            .path(exchange.getRequest().getURI().getPath())
            .method(exchange.getRequest().getMethodValue())
            .statusCode(exchange.getResponse().getStatusCode().value())
            .elapsedTime(elapsedTime)
            .clientIp(getClientIp(exchange))
            .userAgent(exchange.getRequest().getHeaders().getFirst("User-Agent"))
            .errorCode(exchange.getAttribute("errorCode"))
            .errorMessage(exchange.getAttribute("errorMessage"))
            .message(determineMessage(exchange))
            .build();
    }

    private String determineLogLevel(ServerWebExchange exchange) {
        HttpStatus status = exchange.getResponse().getStatusCode();
        if (status == null || status.is2xxSuccessful()) {
            return "INFO";
        } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return "WARN";
        } else {
            return "ERROR";
        }
    }

    private String determineMessage(ServerWebExchange exchange) {
        HttpStatus status = exchange.getResponse().getStatusCode();
        if (status == null || status.is2xxSuccessful()) {
            return "Request processed successfully";
        } else if (status == HttpStatus.UNAUTHORIZED) {
            return "JWT validation failed";
        } else if (status == HttpStatus.FORBIDDEN) {
            return "Permission denied";
        } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return "Rate limit exceeded";
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Circuit breaker triggered";
        } else {
            return "Request failed";
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // 마지막에 실행
    }
}
```

**StructuredLog Record**:
```java
@Builder
public record StructuredLog(
    Instant timestamp,
    String level,
    String traceId,
    Long userId,
    String tenantId,
    Long orgId,
    List<String> roles,
    List<String> permissions,
    String path,
    String method,
    Integer statusCode,
    Long elapsedTime,
    String clientIp,
    String userAgent,
    String errorCode,
    String errorMessage,
    String message
) {}
```

---

##### Logback 설정 (JSON 출력)

**logback-spring.xml**:
```xml
<configuration>
    <!-- Console Appender (JSON 출력) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/> <!-- MDC 필드 포함 -->
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <!-- File Appender (로그 파일 저장) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/gateway.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/gateway-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <message/>
                <mdc/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

##### ELK Stack 연동 (Elasticsearch + Logstash + Kibana)

**Filebeat 설정 (로그 파일 → Logstash 전송)**:
```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /app/logs/gateway*.log
    json.keys_under_root: true  # JSON 필드를 최상위로
    json.add_error_key: true

output.logstash:
  hosts: ["logstash:5044"]
  compression_level: 3
```

**Logstash Pipeline (로그 파싱 및 Elasticsearch 인덱싱)**:
```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # traceId로 인덱싱
  if [traceId] {
    mutate {
      add_field => { "[@metadata][index_suffix]" => "%{traceId}" }
    }
  }

  # 타임스탬프 파싱
  date {
    match => ["timestamp", "ISO8601"]
    target => "@timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "gateway-logs-%{+YYYY.MM.dd}"
  }
}
```

**Kibana 대시보드 예시 쿼리**:
```json
// 특정 사용자의 모든 요청
{
  "query": {
    "term": { "userId": 123 }
  }
}

// Permission 거부된 요청
{
  "query": {
    "term": { "errorCode": "PERMISSION_DENIED" }
  }
}

// traceId로 전체 플로우 추적
{
  "query": {
    "term": { "traceId": "20250124123456-a1b2c3d4" }
  }
}

// 특정 테넌트의 Rate Limit 초과
{
  "query": {
    "bool": {
      "must": [
        { "term": { "tenantId": "tenant-1" } },
        { "term": { "errorCode": "RATE_LIMIT_EXCEEDED" } }
      ]
    }
  }
}
```

---

**핵심 원칙**:
- ✅ **표준화**: 모든 로그가 동일한 필드 구조 (ELK Stack 연동 용이)
- 🔍 **추적 가능**: traceId로 전체 요청 플로우 추적
- 🎯 **운영 필수**: userId, tenantId, orgId, roles, permissions, elapsedTime 필수 포함
- 📊 **분석 용이**: JSON 구조화로 Elasticsearch 인덱싱 및 Kibana 대시보드 생성 용이

---

## 🔧 Layer별 설계

### 1. Application Layer (Gateway Core)

#### 1.1 Filter Chain 설계

**Filter 실행 순서** (Spring Cloud Gateway):
```
1️⃣ TraceIdFilter (Ordered.HIGHEST_PRECEDENCE)
   → X-Trace-Id 생성 및 전달

2️⃣ MetricsFilter (Ordered.HIGHEST_PRECEDENCE + 1)
   → Request Duration 측정 시작

3️⃣ IpBlockingFilter (Ordered.HIGHEST_PRECEDENCE + 2)
   → Blacklist IP 차단

4️⃣ RateLimitFilter (Ordered.HIGHEST_PRECEDENCE + 3)
   → Rate Limit 체크

5️⃣ JwtAuthenticationFilter (Ordered.HIGHEST_PRECEDENCE + 4)
   → JWT 검증 및 Claims 추출
   → 실패 시: TokenRefreshFilter로 위임

6️⃣ TokenRefreshFilter (Ordered.HIGHEST_PRECEDENCE + 5)
   → Access Token 만료 시 재발급
   → 성공 시: JwtAuthenticationFilter 재실행

7️⃣ PermissionFilter (Ordered.HIGHEST_PRECEDENCE + 6)
   → Permission 기반 인가

8️⃣ TenantContextFilter (Ordered.HIGHEST_PRECEDENCE + 7)
   → X-Tenant-Id, X-User-Id 헤더 추가

9️⃣ RouteFilter (Ordered.LOWEST_PRECEDENCE)
   → Backend Service로 요청 전달
```

#### 1.2 Exception Handling

**Exception 처리 전략**:
- **JwtExpiredException**: TokenRefreshFilter로 위임
- **InvalidJwtException**: 401 Unauthorized
- **PermissionDeniedException**: 403 Forbidden
- **RateLimitExceededException**: 429 Too Many Requests
- **ServiceUnavailableException**: 503 Service Unavailable (Circuit Breaker)

**Error Response 형식**:
```json
{
  "errorCode": "PERMISSION_DENIED",
  "message": "Missing required permissions: [order:create]",
  "timestamp": "2025-01-24T12:34:56Z",
  "path": "/api/v1/orders",
  "traceId": "20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789"
}
```

---

### 2. Configuration Layer

#### 2.1 application.yml

```yaml
spring:
  application:
    name: access-gateway
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - StripPrefix=0

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/v1/products/**
          filters:
            - StripPrefix=0

      default-filters:
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}

  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

gateway:
  authhub:
    base-url: ${AUTHHUB_URL:http://authhub}
    public-key-endpoint: /api/v1/auth/public-key
    refresh-token-endpoint: /api/v1/auth/refresh

  permission-policy:
    config-path: classpath:api-permissions.yml
    cache-ttl: 3600 # 1시간

  rate-limit:
    enabled: true
    default-limit: 100
    default-window: 60 # 1분

  metrics:
    enabled: true
    export:
      prometheus:
        enabled: true

  audit-log:
    enabled: true
    kafka-topic: audit-log-events

resilience4j:
  circuitbreaker:
    instances:
      defaultCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
```

---

## ⚠️ 제약사항

### 비기능 요구사항

**성능**:
- JWT 검증: < 10ms (P95)
- Permission 체크: < 5ms (P95)
- 전체 Gateway Latency: < 20ms (P95, 캐시 히트 시)
- TPS: 10,000 requests/sec (Peak Time)

**보안**:
- TLS 1.3 통신 (Backend Services 간 mTLS)
- JWT RS256 (2048-bit RSA)
- Refresh Token Rotation (Reuse 감지)
- IP Blocking (DDoS 방어)

**확장성 및 장애 복원력**:
- Stateless 설계 (Redis 공유 캐시)
- Horizontal Scaling (최소 3 Pods, Auto-Scaling)
- Circuit Breaker + Heartbeat Check (상세 내용은 아래 섹션 참조)

**가용성**:
- 99.9% Uptime (연간 8.76시간 다운타임)
- Multi-AZ 배포 (AWS 3개 가용 영역)
- Health Check (Readiness, Liveness Probe)

---

### Circuit Breaker 및 Heartbeat 전략 ⭐

#### 문제 인식

**AuthHub 장애 시나리오**:
- AuthHub가 죽으면 Gateway는 어떻게 대응할지?
- Public Key 갱신 실패 → JWT 검증 불가 → 전체 서비스 중단?
- Permission 캐시 만료 → AuthHub API 호출 실패 → 모든 요청 차단?

**해결 전략**: Circuit Breaker + Heartbeat + Graceful Degradation (점진적 성능 저하)

---

#### 1️⃣ Heartbeat Check (AuthHub 상태 모니터링)

**목적**: AuthHub 가용성을 주기적으로 확인하여 Circuit Breaker 상태 결정

**동작 방식**:
1. **5초마다** Gateway가 AuthHub의 `/actuator/health` 엔드포인트 호출
2. **200 OK** 응답 → AuthHub 정상 (Circuit Breaker CLOSED)
3. **5xx 에러 또는 Timeout** → AuthHub 장애 감지 (Circuit Breaker OPEN)
4. **연속 3회 실패** → Circuit Breaker OPEN (모든 AuthHub API 호출 차단)

**Java 구현 예시**:
```java
@Component
public class AuthHubHealthChecker {
    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private volatile boolean authHubHealthy = true; // Volatile for thread-safety

    @Scheduled(fixedDelay = 5000) // 5초마다
    public void checkAuthHubHealth() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://authhub/actuator/health",
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // AuthHub 정상
                authHubHealthy = true;
                transitionToHealthy();
                log.debug("AuthHub health check: OK");
            } else {
                // AuthHub 비정상
                authHubHealthy = false;
                log.warn("AuthHub health check: Failed (Status: {})", response.getStatusCode());
            }
        } catch (Exception e) {
            // AuthHub 연결 실패
            authHubHealthy = false;
            log.error("AuthHub health check: Connection failed", e);
        }
    }

    private void transitionToHealthy() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("authhub");
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            circuitBreaker.transitionToHalfOpen(); // HALF_OPEN으로 전환 시도
        }
    }

    public boolean isAuthHubHealthy() {
        return authHubHealthy;
    }
}
```

---

#### 2️⃣ Circuit Breaker (Resilience4j)

**목적**: AuthHub 장애 시 Gateway가 반복적으로 실패 요청을 보내지 않도록 차단

**Circuit Breaker 3가지 상태**:

```
┌─────────────┐
│   CLOSED    │  ← 정상 상태: 모든 요청 허용
│ (정상 상태)  │
└──────┬──────┘
       │ 실패율 > 50% (10초 내 연속 3회 실패)
       ▼
┌─────────────┐
│    OPEN     │  ← 장애 상태: 모든 요청 즉시 차단 (Fallback 실행)
│ (장애 감지)  │
└──────┬──────┘
       │ 30초 경과
       ▼
┌─────────────┐
│  HALF_OPEN  │  ← 복구 시도: 일부 요청만 허용 (테스트)
│ (복구 시도)  │
└──────┬──────┘
       │ 성공 시 → CLOSED
       │ 실패 시 → OPEN
```

**Resilience4j 설정**:
```yaml
resilience4j.circuitbreaker:
  configs:
    default:
      registerHealthIndicator: true
      slidingWindowSize: 10  # 최근 10개 요청 기준
      minimumNumberOfCalls: 3  # 최소 3개 요청 후 판단
      failureRateThreshold: 50  # 50% 실패 시 OPEN
      waitDurationInOpenState: 30s  # OPEN 상태 30초 유지
      permittedNumberOfCallsInHalfOpenState: 5  # HALF_OPEN에서 5개 요청 허용
      automaticTransitionFromOpenToHalfOpenEnabled: true
      slowCallDurationThreshold: 5s  # 5초 이상 → Slow Call로 간주
      slowCallRateThreshold: 50  # Slow Call 50% 이상 → OPEN

  instances:
    authhub:
      baseConfig: default
```

**Java 구현 예시**:
```java
@Component
public class AuthHubApiClient {
    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final PermissionCacheService permissionCacheService;

    @CircuitBreaker(name = "authhub", fallbackMethod = "getPermissionHashFallback")
    public PermissionHash getPermissionHash(Long userId, String tenantId) {
        // AuthHub API 호출
        String url = "http://authhub/api/v1/permissions/hash?userId={userId}&tenantId={tenantId}";
        PermissionHashResponse response = restTemplate.getForObject(
            url,
            PermissionHashResponse.class,
            userId, tenantId
        );

        return new PermissionHash(
            response.permissions(),
            response.roles(),
            response.hash()
        );
    }

    // Fallback Method (Circuit Breaker OPEN 시 실행)
    private PermissionHash getPermissionHashFallback(Long userId, String tenantId, Throwable t) {
        log.warn("AuthHub API failed, using cached permission (Circuit Breaker OPEN)", t);

        // 1. Redis 캐시에서 조회 (TTL 만료되어도 강제 조회)
        String cacheKey = "permission_hash:" + tenantId + ":" + userId;
        PermissionHash cached = permissionCacheService.getCachedPermission(cacheKey);

        if (cached != null) {
            log.info("Using stale cache for userId={}, tenantId={}", userId, tenantId);
            return cached;
        }

        // 2. 캐시도 없으면 빈 권한 반환 (Deny by Default)
        log.error("No cached permission found, denying access for userId={}", userId);
        return PermissionHash.empty(); // 빈 권한 → 모든 요청 거부
    }
}
```

---

#### 3️⃣ Graceful Degradation (점진적 성능 저하)

**목적**: AuthHub 장애 시에도 Gateway가 최대한 서비스 제공

**Fallback 전략 우선순위**:

| 시나리오 | Fallback 전략 | 사용자 영향 |
|---------|-------------|-----------|
| **Public Key 갱신 실패** | 기존 캐시 유지 (최대 24시간) | ✅ 영향 없음 (기존 JWT 계속 검증 가능) |
| **Permission Cache 만료** | Stale Cache 사용 (만료되어도 강제 조회) | ⚠️ 권한 변경 반영 지연 (최대 1시간) |
| **Stale Cache도 없음** | **빈 권한 반환 (Deny by Default)** | 🚫 해당 사용자 요청 거부 |
| **AuthHub 완전 장애** | **Read-Only Mode** (기존 캐시만 사용) | ⚠️ 신규 로그인 불가, 기존 사용자는 정상 |

**Fallback 예시 (Permission 체크)**:
```java
@Component
public class PermissionCacheService {
    private final RedisTemplate<String, PermissionHash> redisTemplate;

    // TTL 만료되어도 강제 조회 (Stale Cache 사용)
    public PermissionHash getCachedPermission(String cacheKey) {
        // 1. Redis TTL 무시하고 Raw 데이터 조회
        PermissionHash cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.warn("Using stale cache: {}", cacheKey);
            return cached;
        }

        // 2. 캐시도 없으면 null 반환
        return null;
    }

    // 빈 권한 (모든 요청 거부)
    public static PermissionHash empty() {
        return new PermissionHash(
            List.of(), // 빈 권한
            List.of(), // 빈 역할
            "empty"    // 빈 해시
        );
    }
}
```

---

#### 4️⃣ 복구 절차 (Recovery Process)

**Circuit Breaker가 OPEN → HALF_OPEN → CLOSED로 전환되는 과정**:

```
1️⃣ OPEN 상태 (장애 감지):
   ├─ AuthHub API 호출 즉시 차단
   ├─ Fallback Method 실행 (Stale Cache 사용)
   └─ 30초 대기

2️⃣ HALF_OPEN 전환 (복구 시도):
   ├─ Heartbeat Check가 200 OK 응답 감지
   ├─ 일부 요청만 AuthHub로 전달 (5개)
   └─ 5개 중 3개 이상 성공 → CLOSED 전환

3️⃣ CLOSED 상태 (정상 복구):
   ├─ 모든 요청 AuthHub로 전달
   ├─ 캐시 갱신 재개
   └─ 서비스 정상화 ✅
```

**복구 시나리오 예시**:
```
12:00:00 - AuthHub 장애 발생
12:00:05 - Heartbeat Check 실패 (1/3)
12:00:10 - Heartbeat Check 실패 (2/3)
12:00:15 - Heartbeat Check 실패 (3/3) → Circuit Breaker OPEN
           → 모든 AuthHub API 호출 차단, Stale Cache 사용

12:05:00 - AuthHub 복구 완료
12:05:05 - Heartbeat Check 성공 → Circuit Breaker HALF_OPEN
           → 일부 요청 AuthHub로 전달 (5개 테스트)
12:05:10 - 5개 중 5개 성공 → Circuit Breaker CLOSED
           → 서비스 정상화 ✅
```

---

#### 5️⃣ 모니터링 및 알림

**Circuit Breaker 이벤트 로깅**:
```java
@Component
public class CircuitBreakerEventListener {
    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition transition = event.getStateTransition();

        log.error(
            "Circuit Breaker State Transition: {} → {} (CircuitBreaker: {})",
            transition.getFromState(),
            transition.getToState(),
            event.getCircuitBreakerName()
        );

        // Slack/PagerDuty 알림
        if (transition.getToState() == CircuitBreaker.State.OPEN) {
            alertService.sendAlert(
                "🚨 AuthHub Circuit Breaker OPEN - Service Degraded",
                "AuthHub API calls are failing, using stale cache"
            );
        } else if (transition.getToState() == CircuitBreaker.State.CLOSED) {
            alertService.sendAlert(
                "✅ AuthHub Circuit Breaker CLOSED - Service Recovered",
                "AuthHub API calls are now succeeding"
            );
        }
    }
}
```

**Prometheus 메트릭**:
```yaml
# Circuit Breaker 상태
resilience4j_circuitbreaker_state{name="authhub"} 0  # CLOSED
resilience4j_circuitbreaker_state{name="authhub"} 1  # OPEN
resilience4j_circuitbreaker_state{name="authhub"} 2  # HALF_OPEN

# 실패율
resilience4j_circuitbreaker_failure_rate{name="authhub"} 0.52  # 52%

# Slow Call 비율
resilience4j_circuitbreaker_slow_call_rate{name="authhub"} 0.31  # 31%
```

---

#### 6️⃣ 장애 시뮬레이션 테스트

**시나리오 1: AuthHub 완전 장애 (30초)**
```
Given: AuthHub가 30초간 응답 불가
When: 사용자가 /api/v1/orders 요청
Then:
  1. Circuit Breaker OPEN 전환 (15초 후)
  2. Stale Cache로 Permission 체크 → 통과
  3. Order Service로 요청 전달 → 성공 ✅
  4. AuthHub 복구 (30초 후) → Circuit Breaker CLOSED
```

**시나리오 2: AuthHub 장애 + Stale Cache 없음**
```
Given: AuthHub가 장애 + Redis에 Permission Cache 없음
When: 신규 사용자가 /api/v1/orders 요청
Then:
  1. Circuit Breaker OPEN → Fallback 실행
  2. Stale Cache 조회 → 없음
  3. 빈 권한 반환 (PermissionHash.empty())
  4. Permission 체크 실패 → 403 Forbidden ⚠️
```

**시나리오 3: Public Key 갱신 실패 (AuthHub 장애)**
```
Given: AuthHub 장애로 Public Key 갱신 실패
When: 사용자가 JWT로 요청
Then:
  1. 기존 캐시된 Public Key로 JWT 검증 → 성공 ✅
  2. 24시간 이내 AuthHub 복구 → Public Key 갱신 재개
  3. 24시간 초과 시 → 경고 로그 ("Public Key cache is stale")
```

---

**핵심 원칙**:
- ✅ **Zero Downtime**: AuthHub 장애에도 기존 사용자는 서비스 이용 가능 (Stale Cache 사용)
- ⚠️ **Graceful Degradation**: 신규 로그인/권한 변경은 지연되지만, 읽기 작업은 정상
- 🚫 **Deny by Default**: 캐시도 없으면 요청 거부 (보안 우선)
- ♻️ **Auto Recovery**: Circuit Breaker가 자동으로 복구 시도 (30초 후)

---

## 🧪 테스트 전략

### Unit Test

**JwtValidator**:
- ✅ 유효한 JWT 검증 성공
- ✅ 만료된 JWT 검증 실패 (JwtExpiredException)
- ✅ 잘못된 Signature 검증 실패 (InvalidJwtException)
- ✅ Issuer 불일치 검증 실패
- ✅ Audience 불일치 검증 실패

**PermissionChecker**:
- ✅ Required Permissions 충족 시 통과
- ✅ Required Permissions 미충족 시 예외 (PermissionDeniedException)
- ✅ Required Roles 충족 시 통과
- ✅ Required Roles 미충족 시 예외

**RateLimiter**:
- ✅ Limit 미만 요청 시 통과
- ✅ Limit 초과 요청 시 429 반환
- ✅ Window 경과 후 카운터 리셋

### Integration Test

**Filter Chain**:
- ✅ TraceIdFilter → JWT → Permission → TenantContext 순서 검증
- ✅ JWT 만료 시 TokenRefreshFilter 실행 확인
- ✅ Permission 없으면 403 반환 확인

**Token Refresh Flow**:
- ✅ Access Token 만료 + Refresh Token 유효 → 재발급 성공
- ✅ Access Token 만료 + Refresh Token 만료 → 401 반환
- ✅ Refresh Token Reuse 감지 → 모든 토큰 무효화

**Rate Limit**:
- ✅ Endpoint별 Rate Limit 정확도 (Redis 카운터)
- ✅ User별 Rate Limit 정확도
- ✅ IP별 Rate Limit 정확도

### E2E Test

**시나리오 1: 정상 요청 플로우**
```
Client → Gateway: POST /api/v1/orders
                  Authorization: Bearer {validAccessToken}

Gateway → Order Service: POST /api/v1/orders
                         X-User-Id: 123
                         X-Tenant-Id: tenant-1
                         X-Trace-Id: 20250124...

Client ← Gateway: 201 Created
                  X-Trace-Id: 20250124...
```

**시나리오 2: Access Token 만료 + 자동 재발급**
```
Client → Gateway: GET /api/v1/orders
                  Authorization: Bearer {expiredAccessToken}
                  Cookie: refresh_token={validRefreshToken}

Gateway → AuthHub: POST /api/v1/auth/refresh
                   Body: { "refreshToken": "..." }

Gateway ← AuthHub: 200 OK
                   Body: { "accessToken": "...", "refreshToken": "..." }

Gateway → Order Service: GET /api/v1/orders
                         Authorization: Bearer {newAccessToken}

Client ← Gateway: 200 OK
                  X-New-Access-Token: {newAccessToken}
                  Set-Cookie: refresh_token={newRefreshToken}
```

**시나리오 3: Permission 없음**
```
Client → Gateway: POST /api/v1/products
                  Authorization: Bearer {validAccessToken (permissions: [order:read])}

Gateway → Permission Check: Required [product:create] vs User [order:read]
                            → DENIED

Client ← Gateway: 403 Forbidden
                  Body: {
                    "errorCode": "PERMISSION_DENIED",
                    "message": "Missing required permissions: [product:create]"
                  }
```

---

## 🚀 배포 전략

### 1. Docker Image

**Dockerfile**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/access-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

### 2. Kubernetes Deployment

**deployment.yml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: access-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: access-gateway
  template:
    metadata:
      labels:
        app: access-gateway
    spec:
      containers:
      - name: access-gateway
        image: authhub/access-gateway:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: redis-service
        - name: AUTHHUB_URL
          value: http://authhub-service
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: access-gateway-service
spec:
  type: LoadBalancer
  selector:
    app: access-gateway
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

### 3. HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: access-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: access-gateway
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 📊 모니터링 & 알림

### Prometheus Queries

**Gateway Latency (P95)**:
```promql
histogram_quantile(0.95,
  sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le, path)
)
```

**JWT 검증 실패율**:
```promql
sum(rate(gateway_jwt_validation_total{result="failure"}[5m]))
/
sum(rate(gateway_jwt_validation_total[5m]))
```

**Permission Denied 비율**:
```promql
sum(rate(gateway_permission_check_total{result="denied"}[5m])) by (path)
```

**Rate Limit 초과 Top 10 Users**:
```promql
topk(10,
  sum(rate(gateway_rate_limit_exceeded_total[5m])) by (user_id)
)
```

### Grafana Dashboard

**패널 구성**:
- **RPS (Requests Per Second)**: 전체 요청 처리량
- **Latency (P50, P95, P99)**: Gateway 응답 시간
- **Error Rate**: 4xx, 5xx 에러 비율
- **JWT Validation**: 성공/실패 비율
- **Permission Denied**: 경로별 거부 비율
- **Rate Limit Exceeded**: 시간대별 초과 횟수
- **Token Refresh**: 성공/실패 비율

### Alert Rules

```yaml
groups:
  - name: gateway_alerts
    rules:
      - alert: HighErrorRate
        expr: sum(rate(gateway_request_total{status=~"5.."}[5m])) > 100
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Gateway 5xx error rate is high"

      - alert: HighLatency
        expr: histogram_quantile(0.95, sum(rate(gateway_request_duration_seconds_bucket[5m])) by (le)) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Gateway P95 latency > 500ms"

      - alert: JwtValidationFailureSpike
        expr: sum(rate(gateway_jwt_validation_total{result="failure"}[5m])) > 50
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "JWT validation failures spiking"
```

---

## 📚 API 문서

### Admin API (내부 관리용)

#### 1. Reload Permission Policy

**Endpoint**: `POST /admin/reload-permissions`

**Description**: api-permissions.yml 변경 시 Redis 캐시 갱신

**Request**:
```bash
curl -X POST http://localhost:8080/admin/reload-permissions \
  -H "Authorization: Bearer {adminToken}"
```

**Response**:
```json
{
  "status": "success",
  "reloadedPolicies": 42,
  "timestamp": "2025-01-24T12:34:56Z"
}
```

#### 2. Block IP

**Endpoint**: `POST /admin/block-ip`

**Description**: 특정 IP 차단 (DDoS 방어)

**Request**:
```json
{
  "ipAddress": "192.168.1.100",
  "reason": "DDoS attack",
  "expiresAt": "2025-01-25T00:00:00Z"
}
```

**Response**:
```json
{
  "status": "success",
  "blockedIp": "192.168.1.100",
  "expiresAt": "2025-01-25T00:00:00Z"
}
```

#### 3. Unblock IP

**Endpoint**: `DELETE /admin/block-ip/{ipAddress}`

**Description**: IP 차단 해제

**Response**:
```json
{
  "status": "success",
  "unblockedIp": "192.168.1.100"
}
```

---

## 🔐 보안 고려사항

### 1. JWT 보안

- **RS256 사용**: 비대칭 암호화 (Public/Private Key)
- **Short-lived Access Token**: 15분 만료 (탈취 위험 최소화)
- **Refresh Token Rotation**: Reuse 감지로 탈취 차단

### 2. Transport 보안

- **TLS 1.3**: Client ↔ Gateway
- **mTLS**: Gateway ↔ Backend Services (상호 인증)
- **HSTS**: Strict-Transport-Security 헤더

### 3. Rate Limiting

- **DDoS 방어**: IP별 Rate Limit
- **Brute Force 방어**: Login API Rate Limit
- **Credential Stuffing 방어**: Token Refresh Rate Limit

### 4. Audit Logging

- **Security Event 우선 기록**: JWT 검증 실패, Permission Denied
- **장기 보관**: 보안 이벤트는 1년 보관
- **실시간 모니터링**: Kafka + ELK Stack

---

## 🛠️ 개발 계획

### Phase 1: Core Filters (예상: 5일)
- [ ] TraceIdFilter 구현
- [ ] JwtAuthenticationFilter 구현 (Public Key 로드, JWT 검증)
- [ ] PermissionFilter 구현 (api-permissions.yml 파싱, Redis 캐싱)
- [ ] TenantContextFilter 구현
- [ ] Unit Test (JwtValidator, PermissionChecker)

### Phase 2: Token Refresh (예상: 3일)
- [ ] TokenRefreshFilter 구현 (AuthHub 연동)
- [ ] Race Condition 방지 (Redisson Lock)
- [ ] Integration Test (Token Refresh Flow)

### Phase 3: Rate Limiting (예상: 3일)
- [ ] RateLimitFilter 구현 (Redis 카운터)
- [ ] IpBlockingFilter 구현
- [ ] Admin API (Reload Permissions, Block IP)
- [ ] Unit Test (RateLimiter)

### Phase 4: Observability (예상: 2일)
- [ ] MetricsFilter 구현 (Prometheus)
- [ ] AuditLogService 구현 (Kafka)
- [ ] Grafana Dashboard 구성
- [ ] Alert Rules 설정

### Phase 5: E2E Test & 배포 (예상: 2일)
- [ ] E2E Test (정상 플로우, Token Refresh, Permission Denied)
- [ ] Kubernetes Deployment 작성
- [ ] HPA 설정
- [ ] Production 배포

---

## 📚 참고 문서

- [IAM Platform (AuthHub) PRD](./iam-platform.md)
- [Spring Cloud Gateway 공식 문서](https://spring.io/projects/spring-cloud-gateway)
- [JWT Best Practices (RFC 8725)](https://datatracker.ietf.org/doc/html/rfc8725)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)

---

**다음 단계**:
1. `/jira-from-prd docs/prd/access-gateway.md` - Jira 티켓 생성
2. `/kentback-plan docs/prd/access-gateway.md` - TDD 계획 수립 (선택)
