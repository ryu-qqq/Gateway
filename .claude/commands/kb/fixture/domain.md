# Domain Fixture 생성 커맨드

**목적**: Domain 객체 (Aggregate, VO, Entity, Event)의 Test Fixture를 자동 생성합니다.

## 사용법

```bash
/kb/fixture/domain [ClassName]
```

## 작업 흐름

### 1. 입력 검증
- 클래스명이 제공되었는지 확인
- 클래스명이 비어있으면 에러 메시지 출력

### 2. 대상 클래스 찾기
- `domain/src/main/java/` 디렉토리에서 클래스 검색
- Serena MCP의 `find_symbol` 사용하여 정확한 위치 파악
- 클래스를 찾을 수 없으면 에러 메시지 출력

### 3. 타입 검증 (Zero-Tolerance)
**Fixture 생성 불가능한 타입** (에러 출력):
- `*Service` - "Service는 Fixture 대상이 아닙니다. Mock을 사용하세요."
- `*Repository` - "Repository는 Fixture 대상이 아닙니다. Mock을 사용하세요."
- `*Manager` - "Manager는 Fixture 대상이 아닙니다. Mock을 사용하세요."
- `*Facade` - "Facade는 Fixture 대상이 아닙니다. Mock을 사용하세요."
- `*Adapter` - "Adapter는 Fixture 대상이 아닙니다. Mock을 사용하세요."

**Fixture 생성 가능한 타입** (허용):
- Aggregate Root (Order, Product, Customer)
- Value Object (Email, Money, OrderId)
- Entity (OrderLine, Address)
- Domain Event (OrderPlaced, ProductCreated)

### 4. 클래스 분석
- 필드 목록 추출
- 생성자 분석 (public, package-private)
- Factory 메서드 찾기 (`forNew`, `forExisting`, `of`, `from` 등)
- 의존 객체 파악 (다른 Domain 객체 참조)

### 5. Fixture 클래스 생성

**생성 위치**:
```
domain/src/testFixtures/java/com/ryuqq/fixture/domain/[ClassName]Fixture.java
```

**설명**: Gradle의 표준 `testFixtures` 소스셋을 사용합니다.

**템플릿 구조**:
```java
package com.ryuqq.fixture.domain;

import com.ryuqq.domain.[package].[ClassName];
// 필요한 의존 Fixture import

/**
 * [ClassName] Domain 객체 Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public class [ClassName]Fixture {

    private [ClassName]Fixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * 기본 [ClassName] Fixture
     */
    public static [ClassName] default[ClassName]() {
        // 생성자 또는 factory 메서드 사용
        return [ClassName].forNew(...);
    }

    /**
     * 기존 [ClassName] Fixture (저장된 상태)
     */
    public static [ClassName] defaultExisting[ClassName]() {
        return [ClassName].forExisting(...);
    }

    /**
     * Custom [ClassName] Fixture Builder
     */
    public static [ClassName] custom[ClassName](...parameters) {
        return [ClassName].forExisting(...);
    }
}
```

### 6. ArchUnit 규칙 자동 준수
- ✅ `public class` 선언
- ✅ `private` 생성자 (인스턴스 생성 방지)
- ✅ `static` 메서드만 사용
- ✅ 인스턴스 필드 없음
- ✅ `Fixture` 접미사 사용
- ✅ `..fixture.domain..` 패키지 위치

### 7. 의존 Fixture 처리
- 다른 Domain 객체를 참조하는 경우 해당 Fixture import
- 예: `Order`가 `Money`를 참조하면 `MoneyFixture` import

### 8. 출력 메시지
```
✅ Domain Fixture 생성 완료
   클래스: [ClassName]
   위치: domain/src/testFixtures/java/com/ryuqq/fixture/domain/[ClassName]Fixture.java
   메서드: default[ClassName](), defaultExisting[ClassName](), custom[ClassName]()
```

## 예시

### 성공 케이스
```bash
/kb/fixture/domain Order

# 출력:
# ✅ Domain Fixture 생성 완료
#    클래스: Order
#    위치: domain/src/testFixtures/java/com/ryuqq/fixture/domain/OrderFixture.java
#    메서드: defaultOrder(), defaultExistingOrder(), customOrder()
```

### 실패 케이스 (타입 검증)
```bash
/kb/fixture/domain OrderService

# 출력:
# ❌ OrderService는 Fixture 대상이 아닙니다.
#    Service는 Mock을 사용하세요.
#
#    사용 예시:
#    @Mock
#    private OrderService orderService;
```

## 생성 후 확인사항
- [ ] domain 모듈의 testFixtures 소스셋에 파일 생성 확인
- [ ] `src/testFixtures/java` 위치 확인
- [ ] ArchUnit 규칙 준수 확인
- [ ] 빌드 성공 확인: `./gradlew :domain:build`

## 관련 문서
- [Test Fixtures Guide](../../../docs/coding_convention/05-testing/test-fixtures/01_test-fixtures-guide.md)
- [Test Fixtures ArchUnit](../../../docs/coding_convention/05-testing/test-fixtures/02_test-fixtures-archunit.md)
