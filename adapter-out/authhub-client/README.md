# AuthHub Client Module

AuthHub 외부 시스템과의 통신을 담당하는 Adapter 모듈입니다.

## 제공 기능

| API | 엔드포인트 | 용도 |
|-----|-----------|------|
| JWKS 조회 | `GET /api/v1/auth/jwks` | JWT 검증용 Public Key 조회 |
| Token Refresh | `POST /api/v1/auth/refresh` | Access Token 갱신 |

## 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Gateway                              │
├─────────────────────────────────────────────────────────┤
│  TokenRefreshFilter                                     │
│    ├── JwtPayloadParser (로컬 JWT 파싱)                 │
│    └── RefreshAccessTokenUseCase                        │
│          └── AuthHubClient (Port)                       │
│                └── AuthHubAdapter (WebClient)           │
│                      └── AuthHub API 호출               │
└─────────────────────────────────────────────────────────┘
```

## 주요 컴포넌트

### AuthHubClient (Port Interface)
- `fetchPublicKeys()`: JWKS 엔드포인트에서 Public Key 목록 조회
- `refreshAccessToken(tenantId, refreshToken)`: 새 Token Pair 발급

### AuthHubAdapter (구현체)
- WebClient + Resilience4j (Retry, Circuit Breaker)
- Connection Pool, Timeout 설정
- TraceId 전파

### JwtPayloadParser (Gateway 모듈)
- 만료된 JWT에서 userId, tenantId 추출 (서명 검증 없음)
- Token Refresh 시 사용자 정보 확인용

## 설정

```yaml
# application.yml
authhub:
  client:
    base-url: http://authhub-web-api-prod.connectly.local:8080
    endpoints:
      jwks: /api/v1/auth/jwks
      refresh: /api/v1/auth/refresh
    webclient:
      connection-timeout: 3000
      response-timeout: 3000
      max-connections: 500
    retry:
      max-attempts: 3
      wait-duration: 100
    circuit-breaker:
      failure-rate-threshold: 50
      wait-duration-in-open-state: 10000
```

## Resilience 전략

| 전략 | 설정 |
|------|------|
| Retry | 최대 3회, Exponential Backoff |
| Circuit Breaker | 50% 실패율 시 Open, 10초 대기 |
| Timeout | Connection 3초, Response 3초 |
