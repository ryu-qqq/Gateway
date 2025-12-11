# Aggregate 설계 가이드

> **Aggregate Root & Entity 설계 규칙**
>
> DDD의 핵심인 Aggregate 설계 원칙과 구현 패턴을 정의합니다.

---

## 1) 핵심 원칙

| 규칙 | 설명 |
|-----|------|
| **생성자 private** | 생성자는 무조건 `private`. 정적 팩토리 메서드로만 생성 |
| **정적 팩토리 3종** | `forNew()` (신규), `of()` (ID 있음), `reconstitute()` (영속성 복원) |
| **불변 ID** | ID 필드는 `final`. 한 번 생성되면 변경 불가 |
| **비즈니스 메서드** | 상태 변경은 명시적 비즈니스 메서드로만 (`confirm()`, `cancel()` 등) |
| **Getter만 허용** | Setter 절대 금지 |
| **Instant 사용** | 시간 필드는 `Instant` 사용 (LocalDateTime 금지) |
| **Clock 주입** | 시간 생성은 `Clock.instant()` 사용 (테스트 가능) |
| **외부 의존성 제로** | Lombok, JPA, Spring 등 외부 의존성 절대 금지. Pure Java만 사용 |

### 금지사항

| 금지 항목 | 이유 |
|----------|------|
| **Lombok 사용** | Pure Java 원칙, 명시적 코드 작성 |
| **Setter 메서드** | 무분별한 상태 변경 방지, 비즈니스 메서드 사용 |
| **LocalDateTime** | 타임존 문제, 서버 위치에 따라 다른 값 |
| **Instant.now() 직접 호출** | 테스트 불가, Clock 주입 필수 |
| **Long/String FK** | 타입 안전성 부족, VO 사용 필수 (`PaymentId paymentId`) |

---

## 2) 시간 타입 규칙 (Zero-Tolerance)

| 타입 | 의미 | 타임존 | 사용 여부 |
|-----|------|-------|---------|
| **Instant** | 특정 시점 (절대 시간) | UTC 기준 | ✅ 필수 |
| **LocalDateTime** | 날짜+시간 (상대 시간) | 타임존 없음 | ❌ 금지 |

```java
// ❌ LocalDateTime 문제: 서버 위치에 따라 다른 값
// 한국 서버: 2025-12-04T19:30:00
// 미국 서버: 2025-12-04T02:30:00
// → 같은 시점인데 다른 값! 비교 불가

// ✅ Instant: 전 세계 동일한 값
// 한국 서버: 2025-12-04T10:30:00Z
// 미국 서버: 2025-12-04T10:30:00Z
// → 같은 시점, 같은 값! 비교 가능
```

### Clock 빈 위치

```
api/                     ← Runnable (실행 모듈)
├─ config/
│  └─ ClockConfig.java   ✅ 여기에 등록

batch/                   ← Runnable (실행 모듈)
├─ config/
│  └─ ClockConfig.java   ✅ 여기에 등록
```

---

## 3) 생성 메서드 패턴

| 메서드 | ID 전달 | ID null 체크 | Event 등록 | 용도 |
|--------|---------|--------------|-----------|------|
| `forNew()` | ❌ null | - | ✅ | 신규 생성 (Auto Increment) |
| `of()` | ✅ 필수 | ✅ 필수 | - | ID 기반 생성 |
| `reconstitute()` | ✅ 필수 | ✅ 필수 | ❌ | 영속성 복원 (Mapper 전용) |

---

## 4) Domain Event 관리

| 규칙 | 설명 |
|-----|------|
| **Aggregate 내부 생성** | Event는 비즈니스 메서드 실행 시 Aggregate 내부에서 생성 |
| **pullDomainEvents()** | Event 수집 후 반환 |
| **불변 리스트 반환** | `List.copyOf()`로 불변 리스트 반환 |
| **reconstitute()는 Event 없음** | 영속성 복원 시에는 Event 생성하지 않음 |

```java
// Domain Event 저장소
private final List<DomainEvent> domainEvents = new ArrayList<>();

// Event 등록 (Aggregate 내부용)
protected void registerEvent(DomainEvent event) {
    this.domainEvents.add(event);
}

// Event 조회 및 초기화
public List<DomainEvent> pullDomainEvents() {
    List<DomainEvent> events = List.copyOf(this.domainEvents);
    this.domainEvents.clear();
    return events;
}
```

---

## 5) Aggregate Root 템플릿

```java
public class Order {
    // ==================== 필드 ====================

    private final OrderId id;                           // ID (final)
    private final CustomerId customerId;                // 필수 필드
    private OrderStatus status;                         // 상태
    private PaymentId paymentId;                        // 외래키 (VO 필수)
    private final List<OrderLineItem> lineItems;        // 종속 Entity
    private final Instant createdAt;                    // 시간 (Instant)
    private Instant updatedAt;
    private final Clock clock;                          // Clock 주입
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ==================== 생성자 (private) ====================

    private Order(OrderId id, CustomerId customerId, OrderStatus status,
                  List<OrderLineItem> lineItems,
                  Instant createdAt, Instant updatedAt, Clock clock) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.lineItems = lineItems;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.clock = clock;
    }

    // ==================== 정적 팩토리 메서드 ====================

    public static Order forNew(CustomerId customerId, Clock clock) {
        Instant now = clock.instant();
        Order order = new Order(
            null, customerId, OrderStatus.PENDING,
            new ArrayList<>(), now, now, clock
        );
        order.registerEvent(OrderCreatedEvent.from(order));
        return order;
    }

    public static Order of(OrderId id, CustomerId customerId, OrderStatus status,
                           List<OrderLineItem> lineItems,
                           Instant createdAt, Instant updatedAt, Clock clock) {
        if (id == null) {
            throw new IllegalArgumentException("ID는 null일 수 없습니다.");
        }
        return new Order(id, customerId, status, new ArrayList<>(lineItems),
                         createdAt, updatedAt, clock);
    }

    public static Order reconstitute(OrderId id, CustomerId customerId, OrderStatus status,
                                     List<OrderLineItem> lineItems,
                                     Instant createdAt, Instant updatedAt, Clock clock) {
        if (id == null) {
            throw new IllegalArgumentException("ID는 null일 수 없습니다.");
        }
        return new Order(id, customerId, status, new ArrayList<>(lineItems),
                         createdAt, updatedAt, clock);
    }

    // ==================== 비즈니스 메서드 ====================

    public void confirm() {
        if (!canConfirm()) {
            throw new InvalidOrderStateException("주문 확정 불가 상태입니다.");
        }
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = clock.instant();
        registerEvent(OrderConfirmedEvent.from(this, previousStatus));
    }

    public void cancel(String reason) {
        if (!canCancel()) {
            throw new InvalidOrderStateException("주문 취소 불가 상태입니다.");
        }
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = clock.instant();
        registerEvent(OrderCancelledEvent.from(this, previousStatus, reason));
    }

    // ==================== 판단 메서드 (도메인 객체가 스스로 판단) ====================

    private boolean canConfirm() {
        return this.status == OrderStatus.PENDING && !this.lineItems.isEmpty();
    }

    private boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }

    public boolean isShippable() {
        return this.status == OrderStatus.CONFIRMED && !this.lineItems.isEmpty();
    }

    public boolean isCancellable() {
        return canCancel();
    }

    // ==================== Event 관리 ====================

    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // ==================== Getter ====================

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public PaymentId paymentId() { return paymentId; }
    public List<OrderLineItem> lineItems() { return Collections.unmodifiableList(lineItems); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    // Law of Demeter: 필요한 계산은 Aggregate가 직접 제공
    public Money totalAmount() {
        return lineItems.stream()
            .map(OrderLineItem::amount)
            .reduce(Money.ZERO, Money::add);
    }

    public int totalItemCount() {
        return lineItems.stream()
            .mapToInt(item -> item.quantity().value())
            .sum();
    }

    public boolean containsProduct(ProductId productId) {
        return lineItems.stream()
            .anyMatch(item -> item.hasProductId(productId));
    }
}
```

---

## 6) Do / Don't

### ❌ Bad Examples

```java
// ❌ Setter 사용
public void setStatus(OrderStatus status) {
    this.status = status;
}

// ❌ 외부에서 상태 체크 후 변경 (Tell Don't Ask 위반)
if (order.getStatus() == OrderStatus.PENDING) {
    order.setStatus(OrderStatus.CONFIRMED);
}

// ❌ LocalDateTime 사용
private final LocalDateTime createdAt;

// ❌ Long FK 사용
private Long paymentId;  // VO 사용해야 함

// ❌ Instant.now() 직접 호출 (테스트 불가)
this.createdAt = Instant.now();
```

### ✅ Good Examples

```java
// ✅ 비즈니스 메서드로 상태 변경
public void confirm() {
    if (!canConfirm()) {
        throw new InvalidOrderStateException("...");
    }
    this.status = OrderStatus.CONFIRMED;
}

// ✅ 도메인 객체가 스스로 판단 (Tell Don't Ask)
order.confirm();  // Order가 내부에서 canConfirm() 체크

// ✅ Instant 사용
private final Instant createdAt;

// ✅ VO FK 사용
private PaymentId paymentId;

// ✅ Clock 주입으로 시간 생성
this.createdAt = clock.instant();
```

---

## 7) 체크리스트

### 기본 구조
- [ ] 생성자는 `private`
- [ ] 정적 팩토리 메서드 3종 (`forNew`, `of`, `reconstitute`)
- [ ] `forNew()`에 ID는 `null` 전달 (Auto Increment)
- [ ] `of()`, `reconstitute()`에 ID null 체크 있음
- [ ] ID 필드는 `final`

### 비즈니스 로직
- [ ] 비즈니스 메서드로 상태 변경 (`confirm()`, `cancel()` 등)
- [ ] 판단 메서드는 Aggregate 내부에 (`canConfirm()`, `isCancellable()`)
- [ ] Getter만 있고 Setter 없음
- [ ] Law of Demeter: 계산은 Aggregate가 제공 (`totalAmount()` 등)

### 시간 관리
- [ ] 시간 필드는 `Instant` 사용 (LocalDateTime 금지)
- [ ] Clock 의존성 주입
- [ ] 상태 변경 시 `updatedAt` 자동 갱신 (`clock.instant()`)

### Domain Event
- [ ] `List<DomainEvent> domainEvents` 필드
- [ ] `registerEvent(DomainEvent)` 메서드 (protected)
- [ ] `pullDomainEvents()` 메서드 (`List.copyOf()` 사용)
- [ ] `forNew()`와 비즈니스 메서드에서 Event 등록
- [ ] `reconstitute()`에서는 Event 등록 안함

### 의존성 및 타입
- [ ] 외부 의존성 제로 (Lombok, JPA, Spring 등 절대 금지)
- [ ] 외래키는 VO 사용 (`Long paymentId` ❌, `PaymentId paymentId` ✅)

---

## 8) LocalDateTime 사용이 허용되는 경우

**예외적으로** 비즈니스 규칙이 "특정 지역 시간"을 기준으로 할 때만:

```java
// 예: 매장 영업시간 (그 지역 기준)
public class StoreOperatingHours {
    private final LocalTime openTime;   // 오전 9시 (그 지역)
    private final LocalTime closeTime;  // 오후 10시 (그 지역)
    private final ZoneId timezone;      // 타임존 함께 저장
}
```

> ⚠️ **주의**: LocalDateTime 사용 시 반드시 ZoneId도 함께 저장해야 합니다.

---

## 9) 관련 문서

- [Event Guide](../event/event-guide.md) - Domain Event 설계 가이드
- [Exception Guide](../exception/exception-guide.md) - Domain Exception 설계 가이드
- [Value Object Guide](../vo/vo-guide.md) - VO 설계 가이드

---

**✅ Aggregate는 도메인의 핵심입니다. 위 규칙을 엄격히 준수하세요.**
