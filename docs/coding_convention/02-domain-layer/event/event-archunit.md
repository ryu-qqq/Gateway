# Domain Event ArchUnit 검증 가이드

> Domain Event의 **아키텍처 규칙**을 ArchUnit으로 자동 검증
>
> 📌 **Zero-Tolerance**: 모든 규칙 위반 시 빌드 실패

---

## 1) 검증 규칙 요약 (11개)

### 1.1 필수 구조 규칙 (4개)

| 규칙 | 검증 내용 | 설명 |
|-----|----------|------|
| **규칙 1** | DomainEvent 인터페이스 구현 | event 패키지 클래스는 DomainEvent 구현 필수 |
| **규칙 2** | Record 타입 | 불변성 보장을 위해 record 사용 |
| **규칙 3** | occurredAt 필드 | Instant 타입 이벤트 발생 시각 필수 |
| **규칙 4** | from() 팩토리 메서드 | Aggregate로부터 생성하는 정적 메서드 필수 |

### 1.2 네이밍 및 패키지 규칙 (2개)

| 규칙 | 검증 내용 | 설명 |
|-----|----------|------|
| **규칙 5** | 과거형 네이밍 | `*edEvent` 또는 불규칙 과거형 (`*PaidEvent`, `*SentEvent`) |
| **규칙 6** | 패키지 위치 | `domain.[bc].event` 패키지에 위치 |

### 1.3 금지 규칙 (4개)

| 규칙 | 검증 내용 | 설명 |
|-----|----------|------|
| **규칙 7** | Lombok 금지 | `@Data`, `@Value`, `@Builder` 등 사용 금지 |
| **규칙 8** | JPA 금지 | `@Entity`, `@Table`, `@Embeddable` 사용 금지 |
| **규칙 9** | Spring 어노테이션 금지 | `@Component`, `@EventListener` 사용 금지 |
| **규칙 10** | Spring Framework 의존 금지 | `org.springframework..` 패키지 의존 금지 |

### 1.4 레이어 의존성 규칙 (1개)

| 규칙 | 검증 내용 | 설명 |
|-----|----------|------|
| **규칙 11** | 외부 레이어 의존 금지 | `application`, `adapter`, `persistence`, `bootstrap` 패키지 의존 금지 |

---

## 2) ArchUnit 테스트 위치

```
domain/src/test/java/
└── com/ryuqq/domain/architecture/event/
    └── DomainEventArchTest.java    # Domain Event 아키텍처 검증 (11개 규칙)
```

### 테스트 태그

```java
@Tag("architecture")
@Tag("domain")
@Tag("event")
```

### 실행 방법

```bash
# Domain Event ArchUnit 테스트만 실행
./gradlew :domain:test --tests "*DomainEventArchTest" -x jacocoTestCoverageVerification

# 전체 Domain 아키텍처 테스트 실행
./gradlew :domain:test --tests "*ArchTest"

# 태그 기반 실행
./gradlew :domain:test -PincludeTags=event
```

---

## 3) 규칙 상세 설명

### 3.1 필수 구조 규칙

#### 규칙 1: DomainEvent 인터페이스 구현

```java
// ✅ 올바른 예
public record OrderPlacedEvent(...) implements DomainEvent { ... }

// ❌ 잘못된 예
public record OrderPlacedEvent(...) { ... }  // DomainEvent 미구현
```

**이유**:
- 타입 안전한 이벤트 처리
- Application Layer에서 일관된 이벤트 발행/구독

#### 규칙 2: Record 타입

```java
// ✅ 올바른 예
public record OrderPlacedEvent(
    OrderId orderId,
    Instant occurredAt
) implements DomainEvent { ... }

// ❌ 잘못된 예
public class OrderPlacedEvent implements DomainEvent {
    private OrderId orderId;  // Mutable 가능성
    // ...
}
```

**이유**:
- 불변성 자동 보장
- equals/hashCode/toString 자동 생성
- 간결한 코드

#### 규칙 3: occurredAt (Instant) 필드

```java
// ✅ 올바른 예
public record OrderPlacedEvent(
    OrderId orderId,
    Instant occurredAt    // Instant 타입 필수
) implements DomainEvent { ... }

// ❌ 잘못된 예
public record OrderPlacedEvent(
    OrderId orderId
    // occurredAt 누락
) implements DomainEvent { ... }
```

**이유**:
- 이벤트 순서 추적
- 감사(Audit) 로깅
- 이벤트 소싱 지원

#### 규칙 4: from() 팩토리 메서드

```java
// ✅ 올바른 예
public record OrderPlacedEvent(...) implements DomainEvent {
    public static OrderPlacedEvent from(Order order, Instant occurredAt) {
        return new OrderPlacedEvent(
            order.id(),
            order.status(),
            occurredAt
        );
    }
}

// ❌ 잘못된 예
public record OrderPlacedEvent(...) implements DomainEvent {
    // from() 메서드 없음 - 외부에서 직접 생성 유도
}
```

**이유**:
- Aggregate 상태와 Event 일관성 보장
- 생성 로직 캡슐화
- 테스트 용이성 (ClockHolder 주입)

### 3.2 네이밍 및 패키지 규칙

#### 규칙 5: 과거형 네이밍

```java
// ✅ 올바른 예 (규칙 과거형 -ed)
OrderCreatedEvent      // created
OrderPlacedEvent       // placed
OrderCancelledEvent    // cancelled

// ✅ 올바른 예 (불규칙 과거형)
OrderPaidEvent         // pay → paid
OrderSentEvent         // send → sent
OrderSoldEvent         // sell → sold

// ❌ 잘못된 예
OrderEvent             // 과거형 아님
CreateOrderEvent       // 현재형
OrderCreateEvent       // 과거형 아님
```

**이유**:
- Domain Event는 "이미 발생한 사실"을 표현
- 과거형 네이밍으로 완료된 행위 명확화

#### 규칙 6: 패키지 위치

```java
// ✅ 올바른 예
package com.ryuqq.domain.order.event;

// ❌ 잘못된 예
package com.ryuqq.domain.order.aggregate;  // aggregate 패키지에 위치
package com.ryuqq.application.order.event; // Application Layer에 위치
```

**이유**:
- Domain Layer 내 event 패키지에 일관된 위치
- Bounded Context별 이벤트 분리

### 3.3 금지 규칙

#### 규칙 7: Lombok 금지

```java
// ❌ 금지
@Data
@Value
@Builder
@Getter
public class OrderPlacedEvent { ... }

// ✅ 올바른 예: Pure Java Record
public record OrderPlacedEvent(...) implements DomainEvent { ... }
```

#### 규칙 8: JPA 금지

```java
// ❌ 금지
@Entity
@Embeddable
public class OrderPlacedEvent { ... }

// ✅ 올바른 예: Domain Event는 영속성과 무관
public record OrderPlacedEvent(...) implements DomainEvent { ... }
```

#### 규칙 9: Spring 어노테이션 금지

```java
// ❌ 금지
@Component
@EventListener
public class OrderPlacedEvent { ... }

// ✅ 올바른 예: EventListener는 Application Layer에 위치
// domain/order/event/
public record OrderPlacedEvent(...) implements DomainEvent { ... }

// application/order/listener/
@Component
public class OrderEventListener {
    @EventListener
    public void handle(OrderPlacedEvent event) { ... }
}
```

#### 규칙 10: Spring Framework 의존 금지

```java
// ❌ 금지
import org.springframework.context.ApplicationEvent;
public record OrderPlacedEvent(...) extends ApplicationEvent { ... }

// ✅ 올바른 예: Pure Java
import com.ryuqq.domain.common.event.DomainEvent;
public record OrderPlacedEvent(...) implements DomainEvent { ... }
```

### 3.4 레이어 의존성 규칙

#### 규칙 11: 외부 레이어 의존 금지

```java
// ❌ 금지: Application Layer 의존
package com.ryuqq.domain.order.event;
import com.ryuqq.application.order.dto.OrderDto;  // 금지!

// ❌ 금지: Adapter Layer 의존
import com.ryuqq.adapter.out.persistence.OrderEntity;  // 금지!

// ✅ 올바른 예: Domain Layer 내부만 참조
package com.ryuqq.domain.order.event;
import com.ryuqq.domain.order.vo.OrderId;
import com.ryuqq.domain.order.aggregate.Order;
import com.ryuqq.domain.common.event.DomainEvent;
```

**이유**:
- 헥사고날 아키텍처 원칙 준수
- Domain Layer는 가장 안쪽 레이어로 외부에 의존하지 않음
- 의존성 역전 원칙 (DIP) 적용

---

## 4) 위반 사례 및 해결

### 사례 1: DomainEvent 인터페이스 미구현

```
❌ 오류: OrderPlacedEvent does not implement DomainEvent interface

// 수정 전
public record OrderPlacedEvent(OrderId orderId) { }

// 수정 후
public record OrderPlacedEvent(OrderId orderId, Instant occurredAt)
    implements DomainEvent { }
```

### 사례 2: from() 메서드 누락

```
❌ 오류: Class OrderPlacedEvent does not have a public static method named 'from'

// 수정: from() 메서드 추가
public static OrderPlacedEvent from(Order order, Instant occurredAt) {
    return new OrderPlacedEvent(order.id(), occurredAt);
}
```

### 사례 3: 과거형 네이밍 위반

```
❌ 오류: Event OrderCreateEvent should have past tense naming

// 수정 전
public record OrderCreateEvent(...) { }

// 수정 후
public record OrderCreatedEvent(...) { }
```

### 사례 4: 외부 레이어 의존

```
❌ 오류: Domain Event는 Application/Adapter 레이어에 의존하지 않아야 합니다

// 수정 전
import com.ryuqq.application.order.dto.OrderSummary;

// 수정 후: Domain 객체만 사용
import com.ryuqq.domain.order.vo.OrderId;
```

---

## 5) 체크리스트

### Domain Event 생성 시

- [ ] `domain.[bc].event` 패키지에 위치
- [ ] `record` 키워드 사용
- [ ] `implements DomainEvent` 선언
- [ ] `occurredAt` (Instant) 필드 포함
- [ ] `from()` 정적 팩토리 메서드 구현
- [ ] 과거형 네이밍 (`*edEvent` 또는 불규칙 과거형)
- [ ] Lombok, JPA, Spring 어노테이션 없음
- [ ] Application/Adapter 레이어 의존 없음

### ArchUnit 테스트 실행 시

- [ ] `./gradlew :domain:test --tests "*DomainEventArchTest"`
- [ ] 전체 11개 규칙 통과 확인

---

## 6) 관련 문서

- [Event Guide](./event-guide.md) - Domain Event 설계 가이드
- [Aggregate ArchUnit Guide](../aggregate/aggregate-archunit.md) - Aggregate ArchUnit 검증
- [Value Object ArchUnit Guide](../vo/vo-archunit.md) - VO ArchUnit 검증
- [LockKey ArchUnit Guide](../vo/lockkey-archunit.md) - LockKey VO 아키텍처 검증
