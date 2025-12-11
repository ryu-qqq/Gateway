# Test Fixtures Guide — **Gradle testFixtures 기반 테스트 픽스쳐 전략**

> **목적**: Gradle `java-test-fixtures` 플러그인을 활용한 테스트 픽스쳐 공유 전략

---

## 1️⃣ 문제 상황

### 기존 방식의 문제점

```
domain/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ Domain 테스트에서만 사용 가능

application/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ 중복! (Domain과 동일)

adapter-in/rest-api/
└── src/test/java/
    └── fixture/
        └── OrderFixture.java   ❌ 중복! (Domain과 동일)
```

**문제**:
- Domain 객체 Fixture가 **여러 레이어에서 중복 작성**
- `src/test/java/`는 다른 모듈에서 접근 불가 (Gradle 모듈 격리)
- Fixture 변경 시 모든 레이어에서 수정 필요
- 유지보수 비용 증가

---

## 2️⃣ 해결 방안: Gradle testFixtures 플러그인

### Gradle `java-test-fixtures` 플러그인

**핵심 원칙**:
1. **Gradle 내장 기능**: 별도 모듈 생성 없이 `src/testFixtures/java/` 디렉토리 사용
2. **자동 의존성 전파**: `testFixtures(project(':domain'))` 문법으로 간편 참조
3. **의존성 흐름 준수**: 헥사고날 아키텍처 의존성 규칙 유지
4. **최소 설정**: `java-test-fixtures` 플러그인만 추가하면 동작

### 디렉터리 구조

```
project/
├── domain/
│   ├── src/main/java/              (Production 코드)
│   ├── src/test/java/              (단위 테스트)
│   └── src/testFixtures/java/      ⭐ Domain Fixture
│       └── com/ryuqq/fixture/domain/
│           ├── OrderFixture.java
│           ├── ProductFixture.java
│           └── CustomerFixture.java
│
├── application/
│   ├── src/main/java/              (Production 코드)
│   ├── src/test/java/              (단위 테스트)
│   └── src/testFixtures/java/      ⭐ Application Fixture
│       └── com/ryuqq/fixture/application/
│           ├── command/
│           │   └── PlaceOrderCommandFixture.java
│           └── response/
│               └── OrderResponseFixture.java
│
├── adapter-in/rest-api/
│   ├── src/main/java/              (Production 코드)
│   ├── src/test/java/              (단위 테스트)
│   └── src/testFixtures/java/      ⭐ REST API Fixture (Optional)
│       └── com/ryuqq/fixture/adapter/rest/
│           └── OrderRequestFixture.java
│
└── adapter-out/persistence-mysql/
    ├── src/main/java/              (Production 코드)
    ├── src/test/java/              (단위 테스트)
    └── src/testFixtures/java/      ⭐ Persistence Fixture (Optional)
        └── com/ryuqq/fixture/adapter/persistence/
            └── OrderEntityFixture.java
```

---

## 3️⃣ 의존성 흐름 (Dependency Flow)

### 허용되는 의존성 (✅)

```
domain testFixtures
    ↓ 의존
  domain (Production)

application testFixtures
    ↓ 의존              ↓ 의존
  application      domain testFixtures

adapter-in testFixtures
    ↓ 의존                    ↓ 의존
  adapter-in         application testFixtures

adapter-out testFixtures
    ↓ 의존              ↓ 의존
  adapter-out       domain testFixtures
```

### 금지된 의존성 (❌)

```
domain testFixtures → application testFixtures   ❌
application testFixtures → adapter-* testFixtures   ❌
adapter-in testFixtures → adapter-out testFixtures   ❌
```

---

## 4️⃣ Gradle 설정

### domain/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'  // ⭐ testFixtures 활성화
}

dependencies {
    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation libs.junit.jupiter
    testImplementation libs.archunit.junit5

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    // Domain TestFixtures는 순수 Java만 사용 (Domain Purity 유지)
    // NO external dependencies
}
```

### application/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'  // ⭐ testFixtures 활성화
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':domain')
    implementation libs.spring.context
    implementation libs.spring.tx

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation libs.spring.boot.starter.test
    testImplementation project(':domain')
    testImplementation testFixtures(project(':domain'))  // ⭐ Domain Fixture 사용

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    // Application TestFixtures는 Domain Fixtures 재사용 가능
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))  // ⭐ Domain Fixture 전파
    testFixturesImplementation libs.spring.context
}
```

### adapter-in/rest-api/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'  // ⭐ testFixtures 활성화
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':application')
    api project(':domain')

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation libs.spring.boot.starter.test
    testImplementation testFixtures(project(':domain'))       // ⭐ Domain Fixture
    testImplementation testFixtures(project(':application'))  // ⭐ Application Fixture

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':application')
    testFixturesApi testFixtures(project(':application'))
}
```

### adapter-out/persistence-mysql/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'  // ⭐ testFixtures 활성화
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':domain')

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation libs.spring.boot.starter.test
    testImplementation testFixtures(project(':domain'))  // ⭐ Domain Fixture

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))
}
```

---

## 5️⃣ 코드 예시

### domain/src/testFixtures/java/.../OrderFixture.java

```java
package com.ryuqq.fixture.domain;

import com.ryuqq.domain.order.Order;
import com.ryuqq.domain.order.OrderId;
import com.ryuqq.domain.order.OrderStatus;
import com.ryuqq.domain.order.Money;

import java.math.BigDecimal;

/**
 * Order Domain 객체 Test Fixture
 *
 * <p>모든 레이어에서 재사용 가능한 Domain 객체 생성 유틸리티</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class OrderFixture {

    private OrderFixture() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * 기본 Order Fixture (신규)
     */
    public static Order defaultNewOrder() {
        return Order.forNew(
            OrderId.forNew(),
            Money.of(BigDecimal.valueOf(50000))
        );
    }

    /**
     * 기존 Order Fixture (저장된 상태)
     */
    public static Order defaultExistingOrder() {
        return Order.forExisting(
            OrderId.of(1L),
            Money.of(BigDecimal.valueOf(50000)),
            OrderStatus.PLACED
        );
    }

    /**
     * 취소된 Order Fixture
     */
    public static Order canceledOrder() {
        Order order = defaultExistingOrder();
        order.cancel();
        return order;
    }

    /**
     * Custom Order Fixture Builder
     */
    public static Order customOrder(Long id, BigDecimal amount, OrderStatus status) {
        return Order.forExisting(
            OrderId.of(id),
            Money.of(amount),
            status
        );
    }
}
```

### application/src/testFixtures/java/.../PlaceOrderCommandFixture.java

```java
package com.ryuqq.fixture.application.command;

import com.ryuqq.application.order.dto.command.PlaceOrderCommand;

import java.math.BigDecimal;

/**
 * PlaceOrderCommand DTO Test Fixture
 *
 * @author development-team
 * @since 1.0.0
 */
public final class PlaceOrderCommandFixture {

    private PlaceOrderCommandFixture() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * 기본 PlaceOrderCommand Fixture
     */
    public static PlaceOrderCommand defaultCommand() {
        return new PlaceOrderCommand(
            BigDecimal.valueOf(50000)
        );
    }

    /**
     * Custom PlaceOrderCommand Fixture
     */
    public static PlaceOrderCommand customCommand(BigDecimal amount) {
        return new PlaceOrderCommand(amount);
    }
}
```

### application/src/test/java/.../PlaceOrderServiceTest.java (사용 예시)

```java
package com.ryuqq.application.order.service;

import com.ryuqq.application.order.port.out.OrderPersistencePort;
import com.ryuqq.domain.order.Order;
import com.ryuqq.fixture.domain.OrderFixture;  // ⭐ Domain Fixture 사용
import com.ryuqq.fixture.application.command.PlaceOrderCommandFixture;  // ⭐ Application Fixture 사용
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderPersistencePort persistencePort;

    @InjectMocks
    private PlaceOrderService service;

    @Test
    void execute_ShouldPlaceOrder() {
        // Given
        var command = PlaceOrderCommandFixture.defaultCommand();  // ⭐ Fixture 사용
        var order = OrderFixture.defaultNewOrder();  // ⭐ Fixture 사용

        given(persistencePort.save(any(Order.class)))
            .willReturn(order);

        // When
        var response = service.execute(command);

        // Then
        assertThat(response).isNotNull();
        then(persistencePort).should(times(1)).save(any(Order.class));
    }
}
```

---

## 6️⃣ 의존성 매트릭스

### 허용/금지 의존성 규칙

| From (테스트 코드) ↓ / To → | domain testFixtures | application testFixtures | adapter-* testFixtures |
|----------------------------|---------------------|--------------------------|------------------------|
| **domain tests** | ✅ | ❌ | ❌ |
| **application tests** | ✅ | ✅ | ❌ |
| **adapter-in tests** | ✅ | ✅ | ✅ (adapter-in만) |
| **adapter-out tests** | ✅ | ❌ | ✅ (adapter-out만) |

---

## 7️⃣ 기존 testFixtures 방식 vs 별도 모듈 방식 비교

| 항목 | Gradle testFixtures (✅ 권장) | 별도 모듈 방식 |
|------|-------------------------------|---------------|
| **설정 복잡도** | 낮음 (`java-test-fixtures` 플러그인만) | 높음 (별도 build.gradle 필요) |
| **의존성 선언** | `testFixtures(project(':domain'))` | `project(':domain-test-fixtures')` |
| **디렉토리** | `src/testFixtures/java/` | `domain-test-fixtures/src/main/java/` |
| **settings.gradle** | 변경 불필요 | 모듈 추가 필요 |
| **IDE 인식** | 자동 | 수동 설정 필요할 수 있음 |
| **Gradle 지원** | 공식 기능 | 커스텀 구조 |

---

## 8️⃣ Fixture 클래스 작성 규칙

### 필수 규칙

```java
// ✅ 1. final 클래스
public final class OrderFixture {

    // ✅ 2. private 생성자 (인스턴스 생성 방지)
    private OrderFixture() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    // ✅ 3. static 메서드만 사용
    public static Order defaultOrder() {
        return Order.forNew(...);
    }

    // ✅ 4. 명확한 메서드 네이밍
    public static Order defaultNewOrder() { ... }     // 신규 객체
    public static Order defaultExistingOrder() { ... } // 기존 객체
    public static Order canceledOrder() { ... }        // 특정 상태
    public static Order customOrder(...) { ... }       // 커스텀 빌더
}
```

### 네이밍 컨벤션

| 패턴 | 용도 | 예시 |
|------|------|------|
| `default*()` | 기본 테스트 객체 | `defaultOrder()`, `defaultNewOrder()` |
| `*WithStatus()` | 특정 상태 객체 | `orderWithPendingStatus()` |
| `custom*()` | 커스텀 빌더 | `customOrder(Long id, BigDecimal amount)` |
| `invalid*()` | 유효하지 않은 객체 | `invalidOrder()` (예외 테스트용) |

---

## 9️⃣ 체크리스트

Test Fixtures 구현 시:
- [ ] `java-test-fixtures` 플러그인 추가 (`build.gradle`)
- [ ] `src/testFixtures/java/` 디렉토리 생성
- [ ] 패키지 구조: `com.ryuqq.fixture.{layer}/`
- [ ] 의존성 설정: `testFixturesApi`, `testFixtures(project(':...'))`
- [ ] Fixture 클래스: `final` + `private 생성자` + `static 메서드`
- [ ] 헥사고날 의존성 규칙 준수
- [ ] ArchUnit 검증 테스트 작성
- [ ] 빌드 및 테스트 통과 확인

---

## 📖 관련 문서

- **[Test Fixtures ArchUnit](./02_test-fixtures-archunit.md)** - ArchUnit 검증 규칙 상세
- **[Integration Testing Overview](../integration-testing/01_integration-testing-overview.md)** - 통합 테스트 가이드
- **[Multi-Module Structure](../../00-project-setup/multi-module-structure.md)** - 멀티모듈 구조 전체 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 2.0.0
