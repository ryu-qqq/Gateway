# Adapter Response DTO Fixture 생성 커맨드

**목적**: Adapter Layer Response DTO의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/adapter/response [ResponseDTOClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 `Response` 또는 `ApiResponse` 접미사를 가지는지 확인

### 2. 대상 클래스 찾기
- `adapter-in-rest/src/main/java/**/dto/` 디렉토리에서 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악

### 3. 타입 검증
**허용**: Response DTO, Api Response 등
**거부**: Controller, Service, Adapter, Mapper

### 4. Fixture 클래스 생성

**생성 위치**:
```
adapter-in-rest/src/testFixtures/java/com/ryuqq/fixture/adapter/rest/[ResponseName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.adapter.rest;

import com.ryuqq.adapter.in.rest.dto.[ResponseName];

/**
 * [ResponseName] Response DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class [ResponseName]Fixture {

    private [ResponseName]Fixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * 기본 [ResponseName] Fixture
     */
    public static [ResponseName] defaultResponse() {
        return new [ResponseName](
            // 기본 응답 데이터
        );
    }

    /**
     * Custom [ResponseName] Fixture
     */
    public static [ResponseName] customResponse(...parameters) {
        return new [ResponseName](...parameters);
    }

    /**
     * 성공 응답 Fixture
     */
    public static [ResponseName] successResponse() {
        return new [ResponseName](
            // 성공 케이스 데이터
        );
    }

    /**
     * 에러 응답 Fixture (필요 시)
     */
    public static [ResponseName] errorResponse() {
        return new [ResponseName](
            // 에러 케이스 데이터
        );
    }
}
```

## 출력 메시지
```
✅ Adapter Response DTO Fixture 생성 완료
   클래스: [ResponseName]
   위치: adapter-in-rest/src/testFixtures/java/com/ryuqq/fixture/adapter/rest/[ResponseName]Fixture.java
   메서드: defaultResponse(), customResponse(), successResponse(), errorResponse()
```

## 관련 문서
- [REST API DTO Guide](../../../../docs/coding_convention/01-adapter-in-layer/rest-api/dto/)
- [Test Fixtures Guide](../../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
