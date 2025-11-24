# Application Response DTO Fixture 생성 커맨드

**목적**: Application Layer Response DTO의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/application/response [ResponseDTOClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 `Response` 또는 `Result` 접미사를 가지는지 확인

### 2. 대상 클래스 찾기
- `application/src/main/java/**/dto/response/` 디렉토리에서 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악

### 3. 타입 검증
**허용**: Response DTO, Result DTO 등
**거부**: Service, UseCase, Manager, Facade, Port

### 4. Fixture 클래스 생성

**생성 위치**:
```
application/src/testFixtures/java/com/ryuqq/fixture/application/response/[ResponseName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.application.response;

import com.ryuqq.application.dto.response.[ResponseName];
// Domain Fixture import (필요 시)
import com.ryuqq.fixture.domain.*;

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
     * Domain 객체로부터 생성 (필요 시)
     */
    public static [ResponseName] fromDomain(...domainObject) {
        // Domain → Response 변환
        return new [ResponseName](...);
    }
}
```

## 출력 메시지
```
✅ Application Response DTO Fixture 생성 완료
   클래스: [ResponseName]
   위치: application/src/testFixtures/java/com/ryuqq/fixture/application/response/[ResponseName]Fixture.java
   메서드: defaultResponse(), customResponse(), fromDomain()
```

## 관련 문서
- [Response DTO Guide](../../../../docs/coding_convention/03-application-layer/dto/response/response-dto-guide.md)
- [Test Fixtures Guide](../../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
