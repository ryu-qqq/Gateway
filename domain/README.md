# Domain Layer

**Pure Business Logic - ZERO Framework Dependencies**

Domain Layer는 비즈니스 로직의 핵심으로, **어떠한 프레임워크 의존성도 가져서는 안 됩니다.**

---

## 📋 목차

- [핵심 원칙](#핵심-원칙)
- [Zero-Tolerance 규칙](#zero-tolerance-규칙)
- [금지된 의존성](#금지된-의존성)
- [verifyDomainPurity](#verifydomainpurity)
- [디렉토리 구조](#디렉토리-구조)
- [ClockHolder 패턴](#clockholder-패턴)
- [테스트 전략](#테스트-전략)

---

## 🎯 핵심 원칙

### 1. Pure Java Only
- **Java Standard Library만 사용** (`java.util.*`, `java.time.*` 등)
- 외부 라이브러리 의존성 **절대 금지**
- 프레임워크 어노테이션 **절대 금지**

### 2. Business Logic Focused
- 비즈니스 규칙과 도메인 로직만 포함
- Infrastructure 관심사는 다른 레이어로 위임
- Domain 객체가 자체적으로 불변성과 유효성 보장

### 3. Framework Agnostic
- Spring, JPA, Lombok 등 **모든 프레임워크로부터 독립**
- 테스트 가능한 Plain Old Java Object (POJO)
- 비즈니스 로직 재사용 가능 (다른 프레임워크로 전환 시에도)

---

## ⚠️ Zero-Tolerance 규칙

Domain Layer에서 다음 규칙을 **절대로** 위반할 수 없습니다:

### 1. ❌ Lombok 금지
```java
// ❌ 금지
@Getter @Setter
public class Order { }

// ✅ 허용
public class Order {
    public Money totalAmount() {
        return amount;
    }
}
```

**이유**:
- Lombok은 외부 의존성 (컴파일 타임 코드 생성)
- Getter는 Law of Demeter 위반 가능성
- Domain은 Tell, Don't Ask 원칙 준수

### 2. ❌ JPA 관계 어노테이션 금지
```java
// ❌ 금지
@Entity
public class Order {
    @OneToMany
    private List<OrderItem> items;
}

// ✅ 허용 (Long FK 전략)
public record OrderId(long value) {
    public static OrderId of(long value) {
        return new OrderId(value);
    }
}
```

**이유**:
- JPA는 Persistence Layer 관심사
- Domain은 데이터베이스 모르는 순수 비즈니스 로직

### 3. ❌ Validation API 금지
```java
// ❌ 금지
public record Email(@NotBlank String value) { }

// ✅ 허용 (도메인 로직으로 검증)
public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
```

**이유**:
- Domain 객체가 스스로 유효성 보장
- 어노테이션이 아닌 명시적 검증 로직

### 4. ❌ External Utilities 금지
```java
// ❌ 금지
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.ImmutableList;

// ✅ 허용 (Java Standard Library)
import java.util.List;
import java.util.Objects;
```

**이유**:
- Java Standard Library로 충분
- 외부 유틸리티는 불필요한 의존성

---

## 🚫 금지된 의존성

`verifyDomainPurity` Gradle 태스크가 다음 의존성을 **빌드 타임에 차단**합니다:

### Framework Dependencies
```gradle
'org.springframework'       // Spring Framework
'jakarta.persistence'       // JPA
'org.hibernate'            // Hibernate ORM
'org.projectlombok'        // Lombok
```

### Validation Libraries
```gradle
'jakarta.validation'        // Bean Validation 3.0
'javax.validation'          // Bean Validation 2.0
```
→ Domain이 직접 검증 로직 구현해야 함

### External Utilities
```gradle
'org.apache.commons'        // Commons Lang, Commons Collections 등
'com.google.guava'         // Google Guava
'io.vavr'                  // Vavr (함수형 라이브러리)
```
→ Java Standard Library만 사용

### JSON Libraries
```gradle
'com.fasterxml.jackson'     // Jackson
'com.google.gson'           // Gson
```
→ Domain은 JSON 변환 관심 없음 (Adapter Layer 책임)

### Logging Libraries
```gradle
'org.slf4j'                // SLF4J
'ch.qos.logback'           // Logback
'org.apache.logging.log4j' // Log4j
```
→ Domain은 로깅 관심 없음 (Infrastructure 책임)

---

## 🛡️ verifyDomainPurity

### 실행 방법
```bash
# Domain Purity 검증
./gradlew :domain:verifyDomainPurity

# 빌드 시 자동 실행
./gradlew build
# ↑ verifyDomainPurity가 자동으로 실행됨
```

### 동작 원리
1. **빌드 타임에 runtimeClasspath 검사**
2. 금지된 의존성이 발견되면 **빌드 즉시 실패**
3. 실수로 외부 라이브러리 추가 시 즉시 차단

### 실패 예시
```bash
❌ DOMAIN PURITY VIOLATION DETECTED

Forbidden dependency found in domain module:
- Group: org.apache.commons
- Name: commons-lang3
- Version: 3.14.0

Domain module must remain pure Java.
NO Spring, NO JPA, NO Lombok allowed.

See: domain/build.gradle
```

---

## 📁 디렉토리 구조

```
domain/
├── src/main/java/com/ryuqq/domain/
│   ├── sample/             # 📚 예시 Bounded Context (참고용)
│   │   ├── README.md       # 사용 가이드
│   │   ├── aggregate/
│   │   │   ├── SampleOrder.java
│   │   │   └── SampleOrderItem.java
│   │   ├── vo/
│   │   │   ├── SampleOrderId.java
│   │   │   ├── SampleOrderItemId.java
│   │   │   └── SampleMoney.java
│   │   ├── event/
│   │   │   └── OrderPlacedEvent.java
│   │   └── exception/
│   │       └── OrderNotFoundException.java
│   │
│   └── common/             # 공통 인터페이스
│       ├── event/          # Domain Events
│       │   └── DomainEvent.java
│       ├── exception/      # Domain Exceptions
│       │   ├── DomainException.java
│       │   └── ErrorCode.java
│       ├── model/          # Domain Model Markers
│       │   ├── AggregateRoot.java
│       │   ├── Entity.java
│       │   ├── ValueObject.java
│       │   └── Identifier.java
│       └── util/           # Utilities
│           └── ClockHolder.java  # DIP 인터페이스
│
├── src/test/java/          # Unit Tests
│   └── com/ryuqq/domain/
│       └── architecture/   # ArchUnit Tests
│           ├── aggregate/
│           ├── vo/
│           └── exception/
│
└── build.gradle            # Zero External Dependencies
```

### 실제 프로젝트 구조 (Bounded Context 패턴)

```
domain/
└── src/main/java/com/ryuqq/domain/
    ├── order/          # Order Bounded Context
    │   ├── aggregate/
    │   ├── vo/
    │   ├── event/
    │   └── exception/
    │
    ├── customer/       # Customer Bounded Context
    │   ├── aggregate/
    │   ├── vo/
    │   └── ...
    │
    ├── product/        # Product Bounded Context
    │   └── ...
    │
    └── common/         # 공통 (유지)
```

**sample/ 패키지를 참고하여 실제 Bounded Context를 생성하세요!**

---

## 🕐 ClockHolder 패턴

Domain 객체가 시간 정보를 필요로 할 때 **DIP (Dependency Inversion Principle)**를 적용합니다.

### 구조

```
Domain Layer (Interface)
├── ClockHolder (interface)
     ↑
     │ 의존성 역전
     │
Application Layer (Implementation)
└── SystemClockHolder (class)
     ↑
     │ Bean 등록
     │
Bootstrap Layer (Configuration)
└── ClockConfig (@Configuration)
    └── @Bean ClockHolder
```

### 사용 예시

```java
// Domain: Aggregate Root
public class Order {
    private final LocalDateTime createdAt;

    private Order(Clock clock, Money amount) {
        this.createdAt = LocalDateTime.now(clock);
        this.amount = amount;
    }

    // Factory Method: Clock을 파라미터로 받음
    public static Order forNew(Clock clock, Money amount) {
        return new Order(clock, amount);
    }
}

// Application: Assembler
@Component
public class OrderAssembler {
    private final ClockHolder clockHolder;

    public Order toAggregate(PlaceOrderCommand command) {
        return Order.forNew(
            clockHolder.getClock(),  // Clock 제공
            command.amount()
        );
    }
}

// Test: Fixed Clock 사용
@Test
void orderCreatedAt() {
    Clock fixedClock = Clock.fixed(
        Instant.parse("2025-01-01T00:00:00Z"),
        ZoneId.of("UTC")
    );

    Order order = Order.forNew(fixedClock, Money.of(10000));

    assertThat(order.createdAt())
        .isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
}
```

### 핵심 원칙

1. **Domain은 ClockHolder 인터페이스에만 의존**
   - Domain Layer에 인터페이스 정의
   - 구현체는 모름

2. **Aggregate는 생성자에서 Clock 파라미터로 받음**
   - Factory Method (forNew, of, reconstitute)에서 Clock 주입
   - 내부적으로 LocalDateTime.now(clock) 사용

3. **Assembler/Mapper가 ClockHolder 주입받아 Clock 제공**
   - Application Layer에서 ClockHolder 의존성 주입
   - Aggregate 생성 시 getClock() 호출

4. **테스트 시 Fixed Clock 사용**
   - 시간 고정으로 테스트 안정성 확보
   - LocalDateTime.now() 직접 호출 금지

---

## 🧪 테스트 전략

### Unit Tests
- **순수 Java 객체 테스트**
- 외부 의존성 없이 도메인 로직 검증
- Fixed Clock으로 시간 의존성 제거

```java
@Test
void orderTotalAmount() {
    Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
    Money itemPrice = Money.of(1000);
    int quantity = 5;

    Order order = Order.forNew(clock, itemPrice, quantity);

    assertThat(order.totalAmount())
        .isEqualTo(Money.of(5000));
}
```

### ArchUnit Tests (아키텍처 검증)

Domain Layer의 아키텍처 규칙을 **자동으로 검증**합니다. **총 98개 규칙**이 7개 테스트 스위트로 구성되어 있습니다.

#### 1. AggregateRootArchTest (24개 규칙)

**목적**: Aggregate Root의 DDD 패턴 준수 검증

**주요 규칙**:
- ✅ AggregateRoot 인터페이스 구현 필수
- ✅ Factory Methods 필수: forNew(), of(), reconstitute()
- ✅ 생성자 private 필수 (외부 직접 생성 차단)
- ✅ Clock 필드 필수 (시간 의존성 주입)
- ✅ createdAt, updatedAt (LocalDateTime) 필드 규칙
- ✅ TestFixture 패턴 준수 (fixture() 메서드)
- ❌ Lombok/JPA/Spring 어노테이션 금지
- ❌ Setter 메서드 절대 금지
- ❌ Public 생성자 금지

**왜 필요한가?**
- **Factory Method 강제**: 생성자 private → forNew()/of()/reconstitute()로만 생성 가능 (생성 로직 중앙화)
- **Clock 주입 강제**: LocalDateTime.now() 직접 호출 금지 → 테스트 시 시간 고정 가능
- **Setter 금지**: 불변성 보장 → 상태 변경은 비즈니스 메서드로만 (changeQuantity(), cancel() 등)

**코드 예시**:
```java
// ✅ 올바른 Aggregate Root
public class Order implements AggregateRoot<OrderId> {
    private final OrderId id;
    private final Clock clock;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Private 생성자
    private Order(OrderId id, Clock clock, ...) {
        this.id = id;
        this.clock = clock;
        this.createdAt = LocalDateTime.now(clock);
        this.updatedAt = createdAt;
    }

    // Factory Methods
    public static Order forNew(Clock clock, Money amount) {
        return new Order(OrderId.forNew(), clock, amount);
    }

    public static Order reconstitute(OrderId id, Clock clock, ...) {
        return new Order(id, clock, ...);
    }

    // 비즈니스 메서드로 상태 변경
    public void changeQuantity(int newQuantity) {
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now(clock);
    }

    // TestFixture
    public static Order fixture(Clock clock) {
        return forNew(clock, Money.of(10000));
    }
}
```

#### 2. VOArchTest (8개 규칙)

**목적**: Value Object의 불변성과 패턴 검증

**주요 규칙**:
- ✅ Record 타입 필수 (불변성 보장)
- ✅ of() 정적 팩토리 메서드 필수
- ✅ ID VO는 forNew() 메서드 필수 (신규 생성)
- ✅ ID VO는 isNew() 메서드 필수 (신규 여부 판단)
- ❌ Lombok/JPA/Spring 어노테이션 금지

**왜 필요한가?**
- **Record 강제**: Immutable 보장 + equals/hashCode 자동 구현
- **of() 패턴**: 검증 로직을 Factory Method에 집중 (생성자 대신)
- **ID 생성 구분**: forNew() = 신규 생성 | reconstitute() = 재구성

**코드 예시**:
```java
// ✅ 일반 Value Object
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}

// ✅ ID Value Object
public record OrderId(long value) {
    // 신규 생성 (DB Insert 전)
    public static OrderId forNew() {
        return new OrderId(0L);
    }

    // 재구성 (DB에서 조회한 값)
    public static OrderId of(long value) {
        return new OrderId(value);
    }

    // 신규 여부 판단
    public boolean isNew() {
        return value == 0L;
    }
}
```

#### 3. EntityArchTest (11개 규칙)

**목적**: Entity (not AggregateRoot)의 패턴 검증

**주요 규칙**:
- ✅ Entity 인터페이스 구현 (AggregateRoot 아님)
- ✅ ID 기반 equals/hashCode 구현
- ✅ 생성자 private 필수
- ✅ Factory Methods: forNew(), reconstitute()
- ✅ domain.[bc].aggregate 패키지 위치
- ❌ AggregateRoot 인터페이스 구현 금지 (Aggregate 내부 Entity)

**왜 필요한가?**
- **Aggregate 내부 Entity 구분**: AggregateRoot와 Entity 명확히 분리
- **ID 기반 동등성**: Entity는 ID로 식별 (값이 아닌 식별자 기반)

**코드 예시**:
```java
// ✅ Aggregate 내부 Entity
public class OrderItem implements Entity<OrderItemId> {
    private final OrderItemId id;
    private int quantity;

    private OrderItem(OrderItemId id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public static OrderItem forNew(int quantity) {
        return new OrderItem(OrderItemId.forNew(), quantity);
    }

    public static OrderItem reconstitute(OrderItemId id, int quantity) {
        return new OrderItem(id, quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

#### 4. ExceptionArchTest (20개 규칙)

**목적**: Domain Exception의 일관된 패턴 검증

**주요 규칙**:
- ✅ ErrorCode Enum은 ErrorCode 인터페이스 구현
- ✅ ErrorCode 형식: {BC}-{3자리 숫자} (예: "ORDER-001")
- ✅ Concrete Exception은 DomainException 상속
- ✅ getCode(), getHttpStatus(), getMessage() 메서드 필수
- ✅ domain.[bc].exception 패키지 위치
- ❌ Lombok/JPA/Spring 어노테이션 금지

**왜 필요한가?**
- **에러 코드 표준화**: "ORDER-001", "CUSTOMER-404" → API 응답에서 일관된 에러 코드
- **HTTP Status 매핑**: Domain Exception → HTTP Status 자동 매핑

**코드 예시**:
```java
// ErrorCode Enum
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
    INVALID_QUANTITY("ORDER-002", HttpStatus.BAD_REQUEST, "수량이 유효하지 않습니다");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getMessage() { return message; }
}

// Concrete Exception
public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(OrderId orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND,
              "Order not found: " + orderId.value());
    }
}
```

#### 5. DomainEventArchTest (10개 규칙)

**목적**: Domain Event의 불변성과 네이밍 패턴 검증

**주요 규칙**:
- ✅ DomainEvent 인터페이스 구현
- ✅ Record 타입 필수 (불변성)
- ✅ 과거형 네이밍: *edEvent, *dEvent (예: OrderPlacedEvent, OrderCancelledEvent)
- ✅ occurredAt (LocalDateTime) 필드 필수
- ✅ of() 정적 팩토리 메서드 필수
- ❌ Lombok/JPA/Spring 어노테이션 금지

**왜 필요한가?**
- **과거형 강제**: Event는 이미 발생한 사실 → OrderPlaced (O) / OrderPlace (X)
- **발생 시각 필수**: 이벤트 순서 보장, 이벤트 소싱 시 필수
- **불변성 보장**: Record → Event는 발행 후 변경 불가

**코드 예시**:
```java
// ✅ 올바른 Domain Event
public record OrderPlacedEvent(
    OrderId orderId,
    Money totalAmount,
    LocalDateTime occurredAt
) implements DomainEvent {

    public static OrderPlacedEvent of(OrderId orderId, Money totalAmount) {
        return new OrderPlacedEvent(
            orderId,
            totalAmount,
            LocalDateTime.now()
        );
    }
}

// ❌ 잘못된 네이밍
public record OrderPlaceEvent(...) { } // 과거형 아님
```

#### 6. DomainPurityArchTest (13개 규칙)

**목적**: Domain Layer 전체의 Pure Java 유지 검증

**주요 규칙**:
- ❌ Lombok 어노테이션 금지 (전체 Domain layer)
- ❌ JPA 어노테이션 금지 (@Entity, @Table, @Column 등)
- ❌ Spring 어노테이션 금지 (@Component, @Service 등)
- ❌ Validation API 금지 (@NotNull, @NotBlank 등)
- ❌ External libraries 금지 (commons-lang3, guava, vavr)
- ❌ JSON libraries 금지 (Jackson, Gson)
- ❌ Logger 금지 (SLF4J, Logback)
- ❌ Application/Adapter 레이어 의존 금지

**왜 필요한가?**
- **Framework 독립성**: Domain은 순수 비즈니스 로직 → 어떤 프레임워크로도 전환 가능
- **테스트 용이성**: 외부 의존성 없음 → 단위 테스트 고속 실행
- **재사용성**: Domain Layer를 다른 프로젝트로 복사해도 동작

**검증 예시**:
```java
// ❌ 금지 (Lombok)
@Getter @Setter
public class Order { }

// ❌ 금지 (JPA)
@Entity
public class Order { }

// ❌ 금지 (Spring)
@Component
public class Order { }

// ❌ 금지 (Validation API)
public record Email(@NotBlank String value) { }

// ❌ 금지 (External Library)
import org.apache.commons.lang3.StringUtils;

// ✅ 허용 (Pure Java)
public class Order {
    private final OrderId id;
    // Plain Java만 사용
}
```

#### 7. PackageStructureArchTest (12개 규칙)

**목적**: Domain Layer 패키지 구조 규칙 검증

**주요 규칙**:
- ✅ domain.common/* 패키지 구조 (공통 인터페이스만)
  - domain.common.event (DomainEvent)
  - domain.common.exception (DomainException, ErrorCode)
  - domain.common.model (AggregateRoot, Entity, ValueObject, Identifier)
  - domain.common.util (ClockHolder)
- ✅ domain.[bc]/* 패키지 구조 (Bounded Context)
  - domain.[bc].aggregate (Aggregate Root, Entity)
  - domain.[bc].vo (Value Objects)
  - domain.[bc].event (Domain Events)
  - domain.[bc].exception (Exceptions, ErrorCodes)
- ❌ Bounded Context 간 순환 의존성 금지
- ✅ 패키지별 적절한 클래스 위치

**왜 필요한가?**
- **Bounded Context 분리**: domain.order, domain.customer → 각 도메인 독립성 유지
- **순환 의존성 방지**: Order → Customer 참조 시 CustomerId만 사용 (Customer 객체 직접 참조 금지)
- **패키지 규칙 강제**: Aggregate는 aggregate 패키지, VO는 vo 패키지

**패키지 구조 예시**:
```
domain/
└── src/main/java/com/ryuqq/domain/
    ├── common/
    │   ├── event/DomainEvent.java
    │   ├── exception/DomainException.java
    │   ├── model/AggregateRoot.java
    │   └── util/ClockHolder.java
    │
    ├── order/           # Order Bounded Context
    │   ├── aggregate/
    │   │   ├── Order.java (AggregateRoot)
    │   │   └── OrderItem.java (Entity)
    │   ├── vo/
    │   │   ├── OrderId.java
    │   │   └── Money.java
    │   ├── event/
    │   │   └── OrderPlacedEvent.java
    │   └── exception/
    │       ├── OrderErrorCode.java
    │       └── OrderNotFoundException.java
    │
    └── customer/        # Customer Bounded Context
        ├── aggregate/
        │   └── Customer.java
        └── vo/
            └── CustomerId.java

// ✅ 허용 (ID로만 참조)
public class Order {
    private final CustomerId customerId; // Long FK 전략
}

// ❌ 금지 (다른 BC 객체 직접 참조)
public class Order {
    private final Customer customer; // 순환 의존성 위험
}
```

---

#### 실행 방법

```bash
# ArchUnit 테스트 실행
./gradlew :domain:test --tests "com.ryuqq.domain.architecture.*"

# 전체 테스트 (ArchUnit 포함)
./gradlew :domain:test

# 빌드 시 자동 실행
./gradlew build
```

#### 총 규칙 요약

| 테스트 스위트 | 규칙 수 | 핵심 검증 |
|--------------|---------|----------|
| AggregateRootArchTest | 24 | Factory Methods, Clock 주입, Setter 금지 |
| VOArchTest | 8 | Record 타입, of() 메서드, ID forNew() |
| EntityArchTest | 11 | ID 기반 동등성, Aggregate 내부 Entity |
| ExceptionArchTest | 20 | ErrorCode 형식, HTTP Status 매핑 |
| DomainEventArchTest | 10 | 과거형 네이밍, occurredAt 필수 |
| DomainPurityArchTest | 13 | Lombok/JPA/Spring 금지 |
| PackageStructureArchTest | 12 | Bounded Context 분리, 순환 의존성 방지 |
| **총합** | **98** | **Domain Layer Pure Java 보장** |

---

## ✅ 체크리스트

Domain Layer 개발 시 다음 사항을 준수하세요:

- [ ] **외부 라이브러리 의존성 없음** (Java Standard Library만)
- [ ] **Lombok 사용 안 함** (Plain Java)
- [ ] **JPA 어노테이션 사용 안 함** (Long FK 전략)
- [ ] **Validation 어노테이션 사용 안 함** (도메인 로직으로 검증)
- [ ] **Domain 객체가 자체적으로 유효성 보장** (생성자에서 검증)
- [ ] **Tell, Don't Ask 원칙 준수** (Getter 체이닝 금지)
- [ ] **시간 의존성은 Clock 파라미터로 주입** (LocalDateTime.now() 직접 호출 금지)
- [ ] **verifyDomainPurity 통과** (`./gradlew :domain:verifyDomainPurity`)

---

## 📚 관련 문서

- [Aggregate 가이드](../docs/coding_convention/02-domain-layer/aggregate/) (예정)
- [Value Object 가이드](../docs/coding_convention/02-domain-layer/vo/) (예정)
- [Domain Exception 가이드](../docs/coding_convention/02-domain-layer/exception/) (예정)
- [ArchUnit 테스트 가이드](./src/test/java/com/ryuqq/domain/architecture/) (예정)

---

**Domain Layer는 프로젝트의 핵심입니다. Pure Java를 유지하여 비즈니스 로직의 순수성을 보장하세요.**
