# Aggregate 설계 가이드

> **Aggregate Root & Entity 설계 규칙**
>
> DDD의 핵심인 Aggregate 설계 원칙과 구현 패턴을 정의합니다.

---

## 1) 핵심 원칙

* **생성자 private 강제**: 생성자는 무조건 `private`. 정적 팩토리 메서드로만 생성.
* **정적 팩토리 3종**: `forNew()` (신규), `of()` (ID 있음), `reconstitute()` (영속성 변환).
* **불변 ID**: ID 필드는 `final`. 한 번 생성되면 변경 불가.
* **비즈니스 메서드**: 상태 변경은 명시적 비즈니스 메서드로만 (`confirm()`, `cancel()` 등).
* **Getter만 허용**: Setter 절대 금지.
* **Clock 의존성**: `createdAt`, `updatedAt`은 `Clock` 주입.
* **외부 의존성 제로**: Lombok, JPA, Spring 등 외부 의존성 절대 금지. Pure Java만 사용.

---

## 2) 생성 메서드 패턴

| 메서드 | ID 전달 | ID null 체크 | 용도 |
|--------|---------|--------------|------|
| `forNew()` | ❌ null | - | 신규 생성 (Auto Increment) |
| `of()` | ✅ 필수 | ✅ 필수 | ID 기반 생성 |
| `reconstitute()` | ✅ 필수 | ✅ 필수 | 영속성 복원 (Mapper 전용) |

---

## 3) Aggregate Root 템플릿

```java
public class Order {
    // ==================== 필드 ====================

    // ID (final, Auto Increment)
    private final OrderId id;

    // 필수 필드
    private final CustomerId customerId;
    private OrderStatus status;

    // 외래키 (VO 사용 필수, Long/String 같은 원시 타입 금지)
    private PaymentId paymentId;  // ✅ 올바른 방식
    // private Long paymentId;    // ❌ 잘못된 방식

    // 종속 Entity (방어적 복사)
    private final List<OrderLineItem> lineItems;
    
    // 시간 (Clock 사용)
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Clock clock;

    // ==================== 생성 메서드 (3종) ====================

    /**
     * 신규 생성 (Auto Increment ID)
     * @param customerId 고객 ID
     * @param clock 시간 제어
     * @return 신규 Order
     */
    public static Order forNew(CustomerId customerId, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new Order(
            null,  // Auto Increment: ID null
            customerId,
            OrderStatus.PENDING,
            new ArrayList<>(),
            now,
            now,
            clock
        );
    }

    /**
     * ID 기반 생성 (비즈니스 로직용)
     * @param id 주문 ID (null 불가)
     * @param customerId 고객 ID
     * @param status 주문 상태
     * @param lineItems 주문 항목
     * @param createdAt 생성 시각
     * @param updatedAt 수정 시각
     * @param clock 시간 제어
     * @return Order
     */
    public static Order of(OrderId id, CustomerId customerId, OrderStatus status,
                           List<OrderLineItem> lineItems,
                           LocalDateTime createdAt, LocalDateTime updatedAt, Clock clock) {
        if (id == null) {
            throw new IllegalArgumentException("ID는 null일 수 없습니다.");
        }
        return new Order(id, customerId, status, new ArrayList<>(lineItems),
                         createdAt, updatedAt, clock);
    }

    /**
     * 영속성 복원 (Mapper 전용)
     * @param id 주문 ID (null 불가)
     * @param customerId 고객 ID
     * @param status 주문 상태
     * @param lineItems 주문 항목
     * @param createdAt 생성 시각
     * @param updatedAt 수정 시각
     * @param clock 시간 제어
     * @return Order
     */
    public static Order reconstitute(OrderId id, CustomerId customerId, OrderStatus status,
                                     List<OrderLineItem> lineItems,
                                     LocalDateTime createdAt, LocalDateTime updatedAt, Clock clock) {
        if (id == null) {
            throw new IllegalArgumentException("ID는 null일 수 없습니다.");
        }
        return new Order(id, customerId, status, new ArrayList<>(lineItems),
                         createdAt, updatedAt, clock);
    }

    /**
     * 생성자 (private)
     */
    private Order(OrderId id, CustomerId customerId, OrderStatus status,
                  List<OrderLineItem> lineItems,
                  LocalDateTime createdAt, LocalDateTime updatedAt, Clock clock) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.lineItems = lineItems;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.clock = clock;
    }

    // ==================== 비즈니스 메서드 ====================

    /**
     * 주문 항목 추가
     */
    public void addLineItem(ProductId productId, Quantity quantity, Money price) {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("PENDING 상태에서만 상품 추가 가능합니다.");
        }
        OrderLineItem item = OrderLineItem.forNew(productId, quantity, price);
        this.lineItems.add(item);
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 주문 확정
     */
    public void confirm() {
        if (!canConfirm()) {
            throw new InvalidOrderStateException("주문 확정 불가 상태입니다.");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (!canCancel()) {
            throw new InvalidOrderStateException("주문 취소 불가 상태입니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 배송 시작
     */
    public void ship() {
        if (!canShip()) {
            throw new InvalidOrderStateException("배송 시작 불가 상태입니다.");
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    // ==================== private 검증 메서드 ====================

    private boolean canConfirm() {
        return this.status == OrderStatus.PENDING && !this.lineItems.isEmpty();
    }

    private boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }

    private boolean canShip() {
        return this.status == OrderStatus.CONFIRMED;
    }

    // ==================== Getter ====================

    public OrderId getId() {
        return id;
    }

    /**
     * Law of Demeter: 원시 타입이 필요한 경우 별도 메서드 제공
     */
    public Long getIdValue() {
        return id != null ? id.getValue() : null;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    /**
     * Law of Demeter: Customer ID의 원시값이 필요한 경우
     */
    public Long getCustomerIdValue() {
        return customerId.getValue();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    /**
     * Law of Demeter: Payment ID의 원시값이 필요한 경우
     */
    public Long getPaymentIdValue() {
        return paymentId != null ? paymentId.getValue() : null;
    }

    public List<OrderLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Law of Demeter: 필요한 계산은 Root가 제공
     */
    public Money getTotalAmount() {
        return lineItems.stream()
            .map(OrderLineItem::getAmount)
            .reduce(Money.ZERO, Money::add);
    }

    /**
     * Law of Demeter: 상품 개수 계산
     */
    public int getTotalItemCount() {
        return lineItems.stream()
            .mapToInt(item -> item.getQuantity().getValue())
            .sum();
    }

    /**
     * Law of Demeter: 특정 상품 포함 여부 확인
     */
    public boolean containsProduct(ProductId productId) {
        return lineItems.stream()
            .anyMatch(item -> item.hasProductId(productId));
    }

    /**
     * Law of Demeter: 배송 가능 여부 (상태 + 상품 존재 확인)
     */
    public boolean isShippable() {
        return this.status == OrderStatus.CONFIRMED && !this.lineItems.isEmpty();
    }

    /**
     * Law of Demeter: 취소 가능 여부
     */
    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.CONFIRMED;
    }
}
```

---

## 4) 체크리스트

Aggregate 작성 후 다음을 확인:

- [ ] 생성자는 `private`
- [ ] 정적 팩토리 메서드 3종 (`forNew`, `of`, `reconstitute`) 존재
- [ ] `forNew()`에 ID는 `null` 전달 (Auto Increment)
- [ ] `of()`, `reconstitute()`에 ID null 체크 있음
- [ ] ID 필드는 `final`
- [ ] 비즈니스 메서드는 명시적 이름 (`confirm()`, `cancel()` 등)
- [ ] 상태 변경 시 `updatedAt` 자동 갱신 (`LocalDateTime.now(clock)`)
- [ ] Getter만 있고 Setter 없음
- [ ] Clock 의존성 주입
- [ ] 외부 의존성 제로 (Lombok, JPA, Spring 등 절대 금지)
- [ ] 외래키는 VO 사용 (Long paymentId ❌, PaymentId paymentId ✅)
- [ ] Law of Demeter: 필요 시 getIdValue() 같은 별도 메서드 제공

---

**✅ Aggregate는 도메인의 핵심입니다. 위 규칙을 엄격히 준수하세요.**
