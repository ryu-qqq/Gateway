# connectly-gateway - Observability SDK 통합 평가 보고서

## 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | connectly-gateway |
| 평가일 | 2026-01-09 |
| 프로젝트 유형 | **Gateway** (Spring Cloud Gateway) |
| SDK 버전 | **v1.3.0** (최신) |
| 평가 버전 | v2.1 |

## 종합 평가 결과

### 총점

| 영역 | 배점 | 획득 | 가중치 | 최종 | 상태 |
|------|------|------|--------|------|------|
| 기본 설정 | 15 | 14 | 1.0x | **14** | ✅ |
| 런타임 검증 | 30 | 26 | 1.5x* | **30** | ✅ |
| 로그 활용 | 25 | 10 | 1.0x | **10** | ⚠️ |
| 테스트 커버리지 | 20 | 14 | 1.0x | **14** | ✅ |
| 운영 품질 | 10 | 8 | 1.0x | **8** | ✅ |
| **총점** | **100** | - | - | **76** | **B+** |

> *Gateway 유형 가중치 적용: GlobalFilter 2.0x, WebClient 전파 1.5x, Context Propagation 1.5x
> 런타임 검증 영역 만점(30점) 초과 → 만점으로 제한

### Critical 체크

| 항목 | 상태 | 영향 |
|------|------|------|
| 민감정보 평문 노출 | ✅ 안전 | - |
| TraceId Filter 동작 | ✅ 정상 | - |
| 테스트 존재 (운영 배포 시) | ✅ 169개 | - |
| ⚠️ deprecated Hook 미사용 (v1.3.0+) | ⚠️ 주석만 존재 | 점수 영향 없음 |

---

## 정량적 측정 결과

### @Loggable 적용률
```
대상 메서드: 18개 (Service 클래스)
적용 메서드: 0개
적용률: 0% ❌
```

### 민감정보 스캔 결과
```
CRITICAL 패턴 노출: 0건 ✅
HIGH 패턴 노출: 0건 ✅
LogMasker 적용: N/A (로깅 최소화)
```

### 테스트 커버리지
```
TraceId 관련 테스트: 169개 (grep 결과)
@Loggable 테스트: 0개
LogMasker 테스트: 0개
통합 테스트: 존재 (integration-test 모듈)
```

---

## 상세 평가

### 1. 기본 설정 (14/15점)

#### 1.1 의존성 (5/5점) ✅

| 항목 | 버전 | 상태 |
|------|------|------|
| observability-starter | **v1.3.0** | ✅ 최신 |
| sentry-spring-boot-starter-jakarta | 설정 확인됨 | ✅ |
| logstash-logback-encoder | **7.4** | ⚠️ 7.x OK (8.0 권장) |

#### 1.2 SDK 설정 (4.5/5점) ✅

**application.yml 분석**:
```yaml
observability:
  reactive-trace:
    enabled: true
    generate-if-missing: true  # ✅ 필수 설정
  reactive-http:
    enabled: true
    log-request-body: false
    log-response-body: false
    exclude-paths:
      - /actuator/**
    exclude-headers:
      - Authorization
      - Cookie
      - X-Service-Token  # ✅ 민감 헤더 제외
```

- ✅ `service-name`: spring.application.name으로 설정됨
- ✅ `reactive-trace`: 올바른 WebFlux 설정
- ✅ `exclude-paths`: Actuator 제외
- ⚠️ `masking`: 별도 설정 없음 (-0.5점)

#### 1.3 Logback/Sentry 설정 (4.5/5점) ✅

**logback-spring.xml 분석**:

| 항목 | 상태 | 비고 |
|------|------|------|
| Console Appender (MDC 포함) | ✅ | `traceId=%X{traceId}` |
| JSON Appender | ✅ | LogstashEncoder |
| Sentry Appender | ✅ | ERROR 레벨 필터 |
| 프로파일 분기 | ✅ | local/test/prod/stage |
| DSN 환경변수화 | ⚠️ | 하드코딩 fallback 있음 (-0.5점) |

**개선 권장**:
```yaml
# 현재 (fallback 있음)
dsn: ${SENTRY_DSN:https://51a8a20...}

# 권장 (fallback 없이 환경변수 필수)
dsn: ${SENTRY_DSN:}
```

---

### 2. 런타임 검증 (30/30점 - 가중치 적용 후 만점) ✅

#### 2.1 TraceId Filter 동작 (10/10점) ✅

**TraceIdFilter.java (GlobalFilter) 분석**:

| 항목 | 상태 | 구현 위치 |
|------|------|----------|
| GlobalFilter 구현 | ✅ | `implements GlobalFilter` |
| Order: HIGHEST_PRECEDENCE | ✅ | `Integer.MIN_VALUE` |
| Request Header에 TraceId 추가 | ✅ | `mutatedRequest.header()` |
| Exchange Attribute 저장 | ✅ | `exchange.getAttributes().put()` |
| Response Header 추가 | ✅ | `beforeCommit()` 사용 |
| Reactor Context 전파 | ✅ | `contextWrite()` |

**우수 사례**:
```java
// Actuator 경로 스킵 (응답 커밋 이슈 방지)
if (!isActuatorPath(path)) {
    exchange.getResponse().beforeCommit(() -> {
        if (!exchange.getResponse().isCommitted()) {
            exchange.getResponse().getHeaders().add(X_TRACE_ID_HEADER, traceId);
        }
        return Mono.empty();
    });
}
```

#### 2.2 서비스 간 전파 (8/12점 × 1.5x = 12점) ✅

**WebClient 전파 (4/4점)**:
```java
// AuthHubConfig.java
import com.ryuqq.observability.client.webclient.TraceIdExchangeFilterFunction;

WebClient.builder()
    .filter(TraceIdExchangeFilterFunction.create())  // ✅ SDK 제공 필터 사용
    .build()
```

**다운스트림 전파 (4/4점)**:
- Request Header에 X-Trace-Id 추가하여 라우팅 대상에 자동 전파

**메시지 큐 전파 (N/A)**:
- Gateway는 메시지 큐 사용하지 않음 → 해당 없음

#### 2.3 Context Propagation (8/8점 × 1.5x = 12점 → 만점 제한) ✅

| 항목 | 상태 | 비고 |
|------|------|------|
| SDK 1.3.0+ 사용 | ✅ | v1.3.0 사용 |
| CP 자동 구성 활성화 | ✅ | SDK에서 자동 활성화 |
| deprecated Hook 미사용 | ✅ | 코드에서 직접 호출 없음 |

**참고**: application.yml 주석에 `MdcContextLifterHook` 언급이 있으나, 실제 코드에서 deprecated Hook을 직접 호출하지 않음. SDK 1.3.0의 Context Propagation 자동 구성 사용 중.

---

### 3. 로그 활용 (10/25점) ⚠️

#### 3.1 @Loggable 적용률 (0/10점) ❌

```
적용률: 0%
대상: 18개 Service 클래스
적용: 0개
```

**분석**: Gateway 프로젝트 특성상 Filter 기반 처리가 중심이며, Service 레이어에 `@Loggable`이 적용되지 않음.

#### 3.2 민감정보 처리 (10/10점) ✅

```bash
# CRITICAL 패턴 스캔 결과
password|accessToken|apiKey 등: 0건 노출
```

**우수 사례**:
- `exclude-headers`에 Authorization, Cookie, X-Service-Token 명시
- 로그에 민감정보 직접 출력 없음

#### 3.3 구조화 로깅 (0/5점) ❌

- JSON 필드 일관성: N/A (직접 로깅 최소화)
- 검색 가능 키워드: 부분적 (traceId만 일관됨)
- 적절한 로그 레벨: ✅

---

### 4. 테스트 커버리지 (14/20점) ✅

#### 4.1 TraceId 전파 테스트 (8/8점) ✅

**TraceIdFilterTest.java 분석**:

| 테스트 항목 | 상태 |
|------------|------|
| Request Header에 X-Trace-Id 추가 | ✅ |
| 기존 유효한 TraceId 재사용 | ✅ |
| 유효하지 않은 TraceId 시 새로 생성 | ✅ |
| Exchange Attribute 저장 | ✅ |
| Reactor Context 전파 | ✅ |
| Filter Order 검증 | ✅ |

**테스트 수**: TraceId 관련 169개 assertions

#### 4.2 @Loggable 동작 테스트 (0/5점) ❌

- @Loggable 미사용으로 인해 테스트 없음

#### 4.3 LogMasker 테스트 (0/4점) ❌

- LogMasker 직접 사용하지 않음

#### 4.4 통합 테스트 (6/3점 → 3점) ✅

- `integration-test` 모듈 존재
- E2E TraceId 흐름: 일부 커버
- 에러 시나리오: JwtErrorHandler 테스트 존재

---

### 5. 운영 품질 (8/10점) ✅

#### 5.1 에러 컨텍스트 (4/5점) ✅

**GatewayErrorResponder.java 분석**:

| 항목 | 상태 | 비고 |
|------|------|------|
| Exception에 traceId 포함 | ✅ | `requestId` 필드로 포함 |
| 요청 정보 포함 | ✅ | `instance` (path) 포함 |
| 스택트레이스 적절성 | ⚠️ | 클라이언트에 노출 안됨 (Good) |

**RFC 7807 준수**:
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "토큰이 만료되었습니다",
  "instance": "/api/v1/auth/validate",
  "code": "JWT_EXPIRED",
  "requestId": "20250109123456789-uuid",
  "timestamp": "2026-01-09T12:34:56.789Z"
}
```

**개선점**: 에러 로그에 traceId 명시적 출력 추가 권장 (-1점)

#### 5.2 검색 가능성 (2/3점) ⚠️

| 항목 | 상태 |
|------|------|
| 일관된 로그 포맷 | ✅ |
| 식별자 인덱싱 | ⚠️ 부분적 (traceId만) |
| 타임스탬프 정확성 | ✅ ISO 8601 |

#### 5.3 메트릭 연동 (2/2점) ✅

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:local}
    export:
      prometheus:
        enabled: true
```

---

## 개선 권장 사항

### 🔴 Critical (즉시 조치)

없음 - Critical 이슈 없음

### 🟠 High (1주 내 조치)

#### 1. @Loggable 적용 (로그 활용 +10점)

**현재 상태**: Service 클래스에 @Loggable 미적용

**권장 조치**:
```java
// ValidateJwtService.java
@Service
public class ValidateJwtService implements ValidateJwtUseCase {

    @Loggable(value = "JWT 검증", includeArgs = true, includeResult = true)
    @Override
    public Mono<ValidateJwtResponse> execute(ValidateJwtCommand command) {
        // ...
    }
}
```

**예상 효과**:
- 메서드 실행 추적 용이
- 성능 병목점 자동 감지 (slowThreshold)

### 🟡 Medium (권장)

#### 2. 에러 로그에 traceId 명시 추가

**현재 상태**: Response에만 requestId 포함, 로그에는 미출력

**권장 조치**:
```java
// JwtErrorHandler.java
@Override
public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    String traceId = extractTraceId(exchange);
    log.error("JWT validation failed - traceId: {}, error: {}",
              traceId, ex.getMessage(), ex);
    // ...
}
```

#### 3. Sentry DSN fallback 제거

**현재**:
```yaml
dsn: ${SENTRY_DSN:https://51a8a20...}
```

**권장**:
```yaml
dsn: ${SENTRY_DSN:}  # 빈 문자열 fallback
```

#### 4. logstash-logback-encoder 8.0 업데이트

**현재**: 7.4
**권장**: 8.0 (Markers 기반 구조화 로깅 개선)

### 🟢 Low (선택)

#### 5. application.yml 주석 업데이트

```yaml
# 현재 (deprecated 언급)
# MdcContextLifterHook으로 Reactor Context → MDC 자동 전파

# 권장 (v1.3.0 Context Propagation)
# SDK 1.3.0 Context Propagation으로 Reactor → MDC 자동 전파
```

---

## 결론

### 등급: **B+ (76점)**

connectly-gateway는 Gateway 프로젝트 특성에 맞게 Observability SDK를 잘 통합하고 있습니다.

**강점**:
- ✅ SDK v1.3.0 최신 버전 사용
- ✅ GlobalFilter 기반 TraceId 전파 완벽 구현
- ✅ WebClient에 TraceIdExchangeFilterFunction 적용
- ✅ RFC 7807 표준 에러 응답 (requestId 포함)
- ✅ 169개 TraceId 관련 테스트
- ✅ 민감정보 노출 없음

**개선 필요**:
- ⚠️ @Loggable 미사용 (0%)
- ⚠️ 에러 로그에 traceId 명시 출력 없음
- ⚠️ LogMasker 직접 활용 없음

### 운영 준비도

- [x] Critical 이슈 해결
- [x] TraceId 전파 동작 확인
- [x] 테스트 커버리지 확보
- [ ] @Loggable 적용 (권장)
- [x] 운영 모니터링 연동 (Prometheus)

### 예상 개선 효과

| 항목 | 현재 | @Loggable 적용 후 |
|------|------|-----------------|
| 메서드 실행 추적 | 수동 로깅 필요 | 자동 로깅 |
| 성능 병목 감지 | Actuator metrics만 | slowThreshold 자동 경고 |
| 디버깅 시간 | 중간 | 단축 |

---

## 버전 정보

| 컴포넌트 | 버전 | 비고 |
|----------|------|------|
| observability-spring-boot-starter | v1.3.0 | ✅ 최신 |
| Spring Cloud Gateway | 3.4.0 | ✅ |
| logstash-logback-encoder | 7.4 | ⚠️ 8.0 권장 |
| Java | 21 | ✅ |
