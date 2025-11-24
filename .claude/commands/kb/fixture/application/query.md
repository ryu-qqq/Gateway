# Application Query DTO Fixture 생성 커맨드

**목적**: Application Layer Query DTO의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/application/query [QueryDTOClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 `Query` 또는 `SearchCondition` 접미사를 가지는지 확인

### 2. 대상 클래스 찾기
- `application/src/main/java/**/dto/query/` 디렉토리에서 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악

### 3. 타입 검증
**허용**: Query DTO, SearchCondition, Criteria 등
**거부**: Service, UseCase, Manager, Facade, Port

### 4. Fixture 클래스 생성

**생성 위치**:
```
application/src/testFixtures/java/com/ryuqq/fixture/application/query/[QueryName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.application.query;

import com.ryuqq.application.dto.query.[QueryName];

/**
 * [QueryName] Query DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class [QueryName]Fixture {

    private [QueryName]Fixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * 기본 [QueryName] Fixture
     */
    public static [QueryName] defaultQuery() {
        return new [QueryName](
            // 기본 검색 조건
        );
    }

    /**
     * 빈 검색 조건 Fixture
     */
    public static [QueryName] emptyQuery() {
        return new [QueryName](
            null, null, null  // 모든 조건 null
        );
    }

    /**
     * Custom [QueryName] Fixture
     */
    public static [QueryName] customQuery(...parameters) {
        return new [QueryName](...parameters);
    }
}
```

## 출력 메시지
```
✅ Application Query DTO Fixture 생성 완료
   클래스: [QueryName]
   위치: application/src/testFixtures/java/com/ryuqq/fixture/application/query/[QueryName]Fixture.java
   메서드: defaultQuery(), emptyQuery(), customQuery()
```

## 관련 문서
- [Query DTO Guide](../../../../docs/coding_convention/03-application-layer/dto/query/query-dto-guide.md)
- [Test Fixtures Guide](../../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
