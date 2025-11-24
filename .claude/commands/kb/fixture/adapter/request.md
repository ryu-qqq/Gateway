# Adapter Request DTO Fixture 생성 커맨드

**목적**: Adapter Layer Request DTO의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/adapter/request [RequestDTOClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 `Request` 접미사를 가지는지 확인

### 2. 대상 클래스 찾기
- `adapter-in-rest/src/main/java/**/dto/` 디렉토리에서 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악

### 3. 타입 검증
**허용**: Request DTO, Api Request 등
**거부**: Controller, Service, Adapter, Mapper

### 4. Fixture 클래스 생성

**생성 위치**:
```
adapter-in-rest/src/testFixtures/java/com/ryuqq/fixture/adapter/rest/[RequestName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.adapter.rest;

import com.ryuqq.adapter.in.rest.dto.[RequestName];

/**
 * [RequestName] Request DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class [RequestName]Fixture {

    private [RequestName]Fixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * 기본 [RequestName] Fixture
     */
    public static [RequestName] defaultRequest() {
        return new [RequestName](
            // 기본 요청 데이터
        );
    }

    /**
     * Custom [RequestName] Fixture
     */
    public static [RequestName] customRequest(...parameters) {
        return new [RequestName](...parameters);
    }

    /**
     * Validation 실패 케이스
     */
    public static [RequestName] invalidRequest() {
        return new [RequestName](
            null  // Validation 실패 값
        );
    }
}
```

## 출력 메시지
```
✅ Adapter Request DTO Fixture 생성 완료
   클래스: [RequestName]
   위치: adapter-in-rest/src/testFixtures/java/com/ryuqq/fixture/adapter/rest/[RequestName]Fixture.java
   메서드: defaultRequest(), customRequest(), invalidRequest()
```

## 관련 문서
- [REST API DTO Guide](../../../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/)
- [Test Fixtures Guide](../../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
