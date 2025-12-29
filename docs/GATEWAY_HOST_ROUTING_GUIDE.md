# Gateway Host-Based Routing 구현 가이드

## 개요

Cross-Origin 쿠키 문제 해결을 위해 Gateway에서 Host-based routing을 구현합니다.
프론트엔드와 API가 같은 도메인(Same-Origin)을 사용하여 쿠키 저장/전송 문제를 해결합니다.

---

## 아키텍처

### 기존 구조 (Cross-Origin - 문제 발생)
```
프론트엔드: stage.set-of.com
API 호출:   commerce.set-of.com/api/v1/...
→ 다른 도메인이라 쿠키 저장 안 됨 (Third-party cookie 차단)
```

### 새로운 구조 (Same-Origin - 문제 해결)
```
프론트엔드: stage.set-of.com
API 호출:   stage.set-of.com/api/v1/...
→ 같은 도메인이라 쿠키 정상 작동

┌─────────────────────────────────────────────────────────────┐
│                        Route53                               │
├─────────────────────────────────────────────────────────────┤
│  stage.set-of.com       ─┐                                  │
│  set-of.com             ─┼──→ Gateway ALB (gateway-alb-prod)│
│  admin.set-of.com       ─┤         │                        │
│  server.set-of.net      ─┤         ▼                        │
│  admin-server.set-of.net ┘    ┌─────────┐                   │
│                               │ Gateway │                   │
│                               └────┬────┘                   │
│                                    │ (Host-based routing)   │
│                     ┌──────────────┴──────────────┐         │
│                     ▼                             ▼         │
│            Legacy Web API              Legacy Admin API     │
│   (setof-commerce-legacy-api-prod) (setof-commerce-legacy-admin-prod)
└─────────────────────────────────────────────────────────────┘
```

---

## 라우팅 테이블

| Host | Target Service | Cloud Map DNS |
|------|----------------|---------------|
| `stage.set-of.com` | Legacy Web API | `setof-commerce-legacy-api-prod.connectly.local:8080` |
| `set-of.com` | Legacy Web API | `setof-commerce-legacy-api-prod.connectly.local:8080` |
| `server.set-of.net` | Legacy Web API | `setof-commerce-legacy-api-prod.connectly.local:8080` |
| `admin.set-of.com` | Legacy Admin API | `setof-commerce-legacy-admin-prod.connectly.local:8080` |
| `admin-server.set-of.net` | Legacy Admin API | `setof-commerce-legacy-admin-prod.connectly.local:8080` |

---

## 코드 변경 사항

### 1. GatewayRoutingConfig.java

`ServiceRoute` 클래스에 `hosts` 필드 추가:

```java
/** Host patterns for host-based routing */
private List<String> hosts = List.of();

public List<String> getHosts() {
    return Collections.unmodifiableList(hosts);
}

public void setHosts(List<String> hosts) {
    this.hosts = hosts == null ? List.of() : new ArrayList<>(hosts);
}
```

Route 빌딩 로직에 host predicate 추가:

```java
for (String path : service.getPaths()) {
    String routeId = serviceId + "-" + (path.hashCode() & Integer.MAX_VALUE);

    routes = routes.route(
        routeId,
        r -> {
            var predicateSpec = r.path(path);

            // Host predicate 추가
            if (!hosts.isEmpty()) {
                predicateSpec = predicateSpec.and()
                    .host(hosts.toArray(new String[0]));
            }

            return predicateSpec
                .filters(f -> {
                    if (service.isStripPrefix()) {
                        return f.stripPrefix(service.getStripPrefixParts());
                    }
                    return f;
                })
                .uri(uri);
        });
}
```

### 2. application.yml (prod 프로필)

```yaml
gateway:
  routing:
    services:
      # Legacy Web API Service
      - id: legacy-web
        uri: ${LEGACY_WEB_URI:http://setof-commerce-legacy-api-prod.connectly.local:8080}
        paths:
          - /**
        hosts:
          - stage.set-of.com
          - set-of.com
          - server.set-of.net
        public-paths:
          - /**

      # Legacy Admin API Service
      - id: legacy-admin
        uri: ${LEGACY_ADMIN_URI:http://setof-commerce-legacy-admin-prod.connectly.local:8080}
        paths:
          - /**
        hosts:
          - admin.set-of.com
          - admin-server.set-of.net
        public-paths:
          - /**
```

---

## 테스트 코드 작성 가이드

### 1. Host-based Routing 단위 테스트

```java
@SpringBootTest
@AutoConfigureWebTestClient
class GatewayHostRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteLegacyWebByHost_stageSetOfCom() {
        webTestClient.get()
            .uri("/api/v1/products")
            .header("Host", "stage.set-of.com")
            .exchange()
            .expectStatus().isOk(); // 또는 적절한 상태 코드
    }

    @Test
    void shouldRouteLegacyWebByHost_setOfCom() {
        webTestClient.get()
            .uri("/api/v1/products")
            .header("Host", "set-of.com")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldRouteLegacyAdminByHost_adminSetOfCom() {
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header("Host", "admin.set-of.com")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldRouteLegacyAdminByHost_adminServerSetOfNet() {
        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header("Host", "admin-server.set-of.net")
            .exchange()
            .expectStatus().isOk();
    }
}
```

### 2. Configuration 로딩 테스트

```java
@SpringBootTest
class GatewayRoutingPropertiesTest {

    @Autowired
    private GatewayRoutingConfig.GatewayRoutingProperties properties;

    @Test
    void shouldLoadLegacyWebHosts() {
        var legacyWeb = properties.getServices().stream()
            .filter(s -> "legacy-web".equals(s.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(legacyWeb.getHosts())
            .containsExactlyInAnyOrder(
                "stage.set-of.com",
                "set-of.com",
                "server.set-of.net"
            );
    }

    @Test
    void shouldLoadLegacyAdminHosts() {
        var legacyAdmin = properties.getServices().stream()
            .filter(s -> "legacy-admin".equals(s.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(legacyAdmin.getHosts())
            .containsExactlyInAnyOrder(
                "admin.set-of.com",
                "admin-server.set-of.net"
            );
    }
}
```

---

## 인프라 변경 사항

### Route53 변경

다음 도메인들의 A 레코드를 Gateway ALB (`gateway-alb-prod`)로 변경:

| 도메인 | 현재 | 변경 후 |
|--------|------|---------|
| `stage.set-of.com` | CloudFront/기존 ALB | Gateway ALB |
| `set-of.com` | CloudFront/기존 ALB | Gateway ALB |
| `server.set-of.net` | 기존 Legacy ALB | Gateway ALB |
| `admin.set-of.com` | 기존 Admin ALB | Gateway ALB |
| `admin-server.set-of.net` | 기존 Legacy Admin ALB | Gateway ALB |

### Gateway ALB 정보

- **ALB 이름**: `gateway-alb-prod`
- **Target Group**: `gateway-tg-prod`

### ACM 인증서

Gateway ALB에 다음 도메인들을 포함하는 인증서 필요:
- `*.set-of.com`
- `*.set-of.net` (또는 `server.set-of.net`, `admin-server.set-of.net` 개별)

---

## 배포 순서

1. **Gateway 코드 변경** (완료)
   - `GatewayRoutingConfig.java` - hosts 필드 추가
   - `application.yml` - Legacy 서비스 설정 추가

2. **테스트 코드 작성 & 검증**

3. **Gateway 빌드 & ECR Push & ECS 배포**

4. **Route53 변경**
   - 도메인별로 순차적으로 변경 권장
   - `stage.set-of.com` 먼저 테스트 후 나머지 적용

5. **기존 Legacy ALB 제거** (모든 트래픽 전환 확인 후)

---

## 프론트엔드 변경 사항

API 호출 시 Same-Origin으로 변경:

```javascript
// 기존 (Cross-Origin)
fetch('https://commerce.set-of.com/api/v1/auth/login', {
  credentials: 'include'
})

// 변경 (Same-Origin - 상대 경로 권장)
fetch('/api/v1/auth/login')
// credentials 설정 불필요, 쿠키 자동 전송/저장
```

---

## 롤백 계획

문제 발생 시:
1. Route53에서 해당 도메인을 기존 ALB로 복구
2. DNS TTL에 따라 전파 시간 소요 (기본 300초)

---

## 담당자

- Gateway 코드: [담당자]
- 인프라 (Route53, ALB): [담당자]
- 프론트엔드: [담당자]
