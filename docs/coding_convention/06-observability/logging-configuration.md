# Logging Configuration Guide — **Logback 상세 설정**

> 이 문서는 **Logback 설정**의 상세 가이드입니다.
>
> Profile별 설정, MDC 활용, JSON 구조화 로깅을 다룹니다.

---

## 1) 파일 구조

```
bootstrap/bootstrap-web-api/src/main/resources/
└── logback-spring.xml
```

---

## 2) Profile별 설정

### 2.1) Development (local, test, default)

**특징**:
- Human-readable 패턴 형식
- 컬러 하이라이트 (터미널)
- DEBUG 레벨 활성화
- Hibernate SQL 로깅

```xml
<springProfile name="local,test,default">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg [traceId=%X{traceId:-N/A}, spanId=%X{spanId:-N/A}, errorCode=%X{errorCode:-}]%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <logger name="com.ryuqq" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.orm.jdbc.bind" level="TRACE"/>
</springProfile>
```

**출력 예시**:
```
2025-12-05 10:30:00.123 ERROR [http-nio-8080-exec-1] c.r.a.i.r.c.GlobalExceptionHandler - DomainException: code=ORDER_NOT_FOUND [traceId=abc123, spanId=xyz789, errorCode=ORDER_NOT_FOUND]
```

### 2.2) Production (prod, staging)

**특징**:
- JSON 구조화 로깅 (LogstashEncoder)
- CloudWatch/ELK 호환
- INFO 레벨 (DEBUG 비활성화)
- 프레임워크 로그 최소화

```xml
<springProfile name="prod,staging">
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- MDC Fields -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>errorCode</includeMdcKeyName>

            <!-- Custom Fields -->
            <customFields>{"service":"${APP_NAME}","environment":"${APP_ENV}"}</customFields>

            <!-- Timestamp (ISO 8601) -->
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</timestampPattern>

            <!-- Field Names -->
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <level>level</level>
                <thread>thread</thread>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>

            <!-- Stack Trace -->
            <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerThrowable>30</maxDepthPerThrowable>
                <maxLength>4096</maxLength>
                <shortenedClassNameLength>20</shortenedClassNameLength>
                <exclude>sun\.reflect\..*</exclude>
                <exclude>java\.lang\.reflect\..*</exclude>
                <exclude>jdk\.internal\..*</exclude>
                <rootCauseFirst>true</rootCauseFirst>
            </throwableConverter>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>

    <logger name="com.ryuqq" level="INFO"/>
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
</springProfile>
```

---

## 3) MDC (Mapped Diagnostic Context)

### 3.1) MDC 필드 정의

| 필드 | 타입 | 설명 | 설정 위치 |
|------|------|------|----------|
| `traceId` | String | 분산 추적 ID | OpenTelemetry/X-Ray 자동 |
| `spanId` | String | 현재 Span ID | OpenTelemetry/X-Ray 자동 |
| `requestId` | String (UUID) | 요청 고유 ID | RequestIdFilter |
| `userId` | String | 인증된 사용자 ID | UserIdMdcFilter |
| `errorCode` | String | 도메인 에러 코드 | GlobalExceptionHandler |

### 3.2) MDC 사용 패턴

```java
// 설정
MDC.put("errorCode", ex.code());

// 사용 (자동으로 로그에 포함)
log.error("DomainException occurred", ex);

// 정리 (필수!)
MDC.remove("errorCode");

// 또는 try-finally 패턴
MDC.put("key", "value");
try {
    // 로직
} finally {
    MDC.remove("key");
}
```

### 3.3) MDC Filter 구현

**RequestIdFilter.java**:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
```

---

## 4) JSON 필드 설명

### 4.1) 표준 필드

| 필드 | 타입 | 설명 | CloudWatch 쿼리 |
|------|------|------|----------------|
| `@timestamp` | String (ISO 8601) | 로그 발생 시간 | `@timestamp` |
| `level` | String | 로그 레벨 | `{ $.level = "ERROR" }` |
| `message` | String | 로그 메시지 | `{ $.message = "*keyword*" }` |
| `logger` | String | 로거 클래스명 | `{ $.logger = "*Handler*" }` |
| `thread` | String | 스레드명 | - |
| `stack_trace` | String | 스택트레이스 | `{ $.stack_trace EXISTS }` |

### 4.2) MDC 필드

| 필드 | 타입 | 설명 | CloudWatch 쿼리 |
|------|------|------|----------------|
| `traceId` | String | 분산 추적 ID | `{ $.traceId = "abc123" }` |
| `spanId` | String | Span ID | - |
| `requestId` | String | 요청 ID | `{ $.requestId = "req-001" }` |
| `userId` | String | 사용자 ID | `{ $.userId = "user-123" }` |
| `errorCode` | String | 에러 코드 | `{ $.errorCode = "ORDER_*" }` |

### 4.3) Custom 필드

| 필드 | 타입 | 설명 | CloudWatch 쿼리 |
|------|------|------|----------------|
| `service` | String | 서비스명 | `{ $.service = "order-api" }` |
| `environment` | String | 환경 | `{ $.environment = "prod" }` |

---

## 5) 로거 레벨 설정

### 5.1) 권장 설정

| 패키지 | Development | Production | 설명 |
|--------|-------------|------------|------|
| `com.ryuqq` | DEBUG | INFO | 애플리케이션 코드 |
| `org.springframework` | INFO | WARN | Spring Framework |
| `org.springframework.web` | INFO | WARN | Spring Web |
| `org.springframework.security` | INFO | WARN | Spring Security |
| `org.hibernate.SQL` | DEBUG | WARN | Hibernate SQL |
| `org.hibernate.orm.jdbc.bind` | TRACE | WARN | Hibernate 바인드 파라미터 |
| `software.amazon.awssdk` | INFO | WARN | AWS SDK |
| `io.netty` | INFO | WARN | Netty |

### 5.2) 동적 레벨 변경 (Actuator)

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: loggers
  endpoint:
    loggers:
      enabled: true
```

```bash
# 런타임 레벨 변경
curl -X POST http://localhost:8080/actuator/loggers/com.ryuqq \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## 6) 스택트레이스 설정

### 6.1) ShortenedThrowableConverter 옵션

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `maxDepthPerThrowable` | 30 | 예외당 최대 스택 깊이 |
| `maxLength` | 4096 | 전체 스택트레이스 최대 길이 |
| `shortenedClassNameLength` | 20 | 클래스명 축약 길이 |
| `rootCauseFirst` | true | Root Cause를 먼저 출력 |
| `exclude` | - | 제외할 패키지 패턴 |

### 6.2) 제외 패턴 권장

```xml
<exclude>sun\.reflect\..*</exclude>
<exclude>java\.lang\.reflect\..*</exclude>
<exclude>jdk\.internal\..*</exclude>
<exclude>org\.springframework\.cglib\..*</exclude>
<exclude>org\.springframework\.aop\..*</exclude>
```

---

## 7) 의존성 설정

### 7.1) Gradle

```groovy
// build.gradle
dependencies {
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
}
```

### 7.2) Maven

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

---

## 8) 트러블슈팅

### 8.1) JSON 로그가 출력되지 않음

**원인**: Profile이 `prod` 또는 `staging`이 아님

**해결**:
```bash
# Profile 확인
java -jar app.jar --spring.profiles.active=prod
```

### 8.2) MDC 필드가 null로 출력

**원인**: MDC 설정/해제 순서 문제

**해결**:
```java
// Filter 순서 확인
@Order(Ordered.HIGHEST_PRECEDENCE)  // RequestIdFilter

@Order(Ordered.HIGHEST_PRECEDENCE + 1)  // UserIdMdcFilter
```

### 8.3) 스택트레이스가 너무 김

**해결**:
```xml
<throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
    <maxDepthPerThrowable>20</maxDepthPerThrowable>
    <maxLength>2048</maxLength>
</throwableConverter>
```

### 8.4) 메모리 누수 (MDC)

**원인**: MDC.remove() 누락

**해결**:
```java
// 항상 try-finally 패턴 사용
MDC.put("key", "value");
try {
    // 로직
} finally {
    MDC.remove("key");  // 필수!
}
```

---

## 9) 체크리스트

### Logback 설정

- [ ] logback-spring.xml 파일 존재
- [ ] Development/Production Profile 분리
- [ ] JSON 구조화 로깅 (LogstashEncoder)
- [ ] MDC 필드 포함 (traceId, spanId, errorCode)
- [ ] ISO 8601 타임스탬프
- [ ] 스택트레이스 설정

### 의존성

- [ ] logstash-logback-encoder 추가

### 테스트

- [ ] local Profile로 패턴 로그 확인
- [ ] prod Profile로 JSON 로그 확인
- [ ] MDC 필드 정상 출력 확인

---

## 10) 관련 문서

| 문서 | 설명 |
|------|------|
| [Observability Guide](./observability-guide.md) | 전체 관측성 가이드 |
| [CloudWatch Integration](./cloudwatch-integration.md) | CloudWatch 연동 |
| [Error Handling Guide](../01-adapter-in-layer/rest-api/error/error-guide.md) | 에러 처리 |

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
