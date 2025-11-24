# Application Command DTO Fixture 생성 커맨드

**목적**: Application Layer Command DTO의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/application/command [CommandDTOClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 `Command` 접미사를 가지는지 확인
- 없으면 경고 메시지 출력 (하지만 계속 진행)

### 2. 대상 클래스 찾기
- `application/src/main/java/**/dto/command/` 디렉토리에서 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악
- 클래스를 찾을 수 없으면 에러 메시지 출력

### 3. 타입 검증 (Zero-Tolerance)
**Fixture 생성 불가능한 타입** (에러 출력):
- `*Service` - "Service는 Fixture 대상이 아닙니다."
- `*UseCase` - "UseCase는 Fixture 대상이 아닙니다."
- `*Manager` - "Manager는 Fixture 대상이 아닙니다."
- `*Facade` - "Facade는 Fixture 대상이 아닙니다."
- `*Port` (interface) - "Port는 Fixture 대상이 아닙니다. Mock을 사용하세요."

**Fixture 생성 가능한 타입** (허용):
- Command DTO (PlaceOrderCommand, UpdateProductCommand)
- Record 타입 우선 확인

### 4. 클래스 분석
- Record인지 일반 클래스인지 확인
- 필드 목록 추출
- 생성자 파라미터 분석
- Validation 어노테이션 확인 (`@NotNull`, `@NotBlank` 등)
- 의존 객체 파악 (Domain VO 참조 여부)

### 5. Fixture 클래스 생성

**생성 위치**:
```
application/src/testFixtures/java/com/ryuqq/fixture/application/command/[CommandName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.application.command;

import com.ryuqq.application.dto.command.[CommandName];
// Domain Fixture import (필요 시)
import com.ryuqq.fixture.domain.*;

/**
 * [CommandName] Command DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class [CommandName]Fixture {

    private [CommandName]Fixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * 기본 [CommandName] Fixture
     */
    public static [CommandName] defaultCommand() {
        return new [CommandName](
            // 필드값 기본값
        );
    }

    /**
     * Custom [CommandName] Fixture
     */
    public static [CommandName] customCommand(...parameters) {
        return new [CommandName](...parameters);
    }

    /**
     * Validation 실패 케이스 (필요 시)
     */
    public static [CommandName] invalidCommand() {
        return new [CommandName](
            null  // Validation 실패 값
        );
    }
}
```

### 6. ArchUnit 규칙 자동 준수
- ✅ `public class` 선언
- ✅ `private` 생성자
- ✅ `static` 메서드만 사용
- ✅ 인스턴스 필드 없음
- ✅ `Fixture` 접미사 사용
- ✅ `..fixture.application.command..` 패키지 위치
- ✅ `application` 모듈의 testFixtures 소스셋에서 `domain` 모듈의 testFixtures 의존

### 7. 의존 Fixture 처리
- Domain VO를 참조하는 경우 Domain Fixture import
- 예: `Money`, `Email` 등을 사용하면 `MoneyFixture`, `EmailFixture` import

### 8. 출력 메시지
```
✅ Application Command DTO Fixture 생성 완료
   클래스: [CommandName]
   위치: application/src/testFixtures/java/com/ryuqq/fixture/application/command/[CommandName]Fixture.java
   메서드: defaultCommand(), customCommand(), invalidCommand()
```

## 예시

### 성공 케이스
```bash
/kb/fixture/application/command PlaceOrderCommand

# 출력:
# ✅ Application Command DTO Fixture 생성 완료
#    클래스: PlaceOrderCommand
#    위치: application/src/testFixtures/java/com/ryuqq/fixture/application/command/PlaceOrderCommandFixture.java
#    메서드: defaultCommand(), customCommand(), invalidCommand()
```

### 실패 케이스 (타입 검증)
```bash
/kb/fixture/application/command PlaceOrderService

# 출력:
# ❌ PlaceOrderService는 Fixture 대상이 아닙니다.
#    Service는 Mock을 사용하세요.
```

## 생성 후 확인사항
- [ ] application 모듈의 testFixtures 소스셋에 파일 생성 확인
- [ ] `src/testFixtures/java` 위치 확인
- [ ] ArchUnit 규칙 준수 확인
- [ ] 빌드 성공: `./gradlew :application:build`

## 관련 문서
- [Command DTO Guide](../../../../docs/coding_convention/03-application-layer/dto/command/command-dto-guide.md)
- [Test Fixtures Guide](../../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
