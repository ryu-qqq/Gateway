---
description: Application Layer Doc-Driven 구현. UseCase, Service, Command/Query DTO, Assembler 생성. 구현 + 테스트 동시 작성.
tags: [project]
---

# /impl application - Application Layer Implementation

**Doc-Driven Development**로 Application Layer 신규 코드를 생성합니다.

## 사용법

```bash
/impl application {feature-name}
/impl application order-cancel
/impl application member-register
```

## 실행 프로세스

```
/impl application cancel-order
        ↓
┌─────────────────────────────────────────────────┐
│ 1️⃣ Plan 로드 (Serena memory)                    │
│    - read_memory("plan-{feature}")              │
│    - 비즈니스 규칙 확인                           │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 2️⃣ Application Skill 활성화                     │
│    - usecase-expert, transaction-expert         │
│    - Zero-Tolerance 검증                        │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 3️⃣ 구현 + 테스트 동시 작성                       │
│    - UseCase Interface (Port In)               │
│    - Service 구현체                             │
│    - Command/Query DTO                         │
│    - Unit 테스트 (Mock Port Out)               │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 4️⃣ 검증 및 커밋                                 │
│    - ./gradlew test                            │
│    - feat: 커밋 (테스트 포함)                    │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 5️⃣ Memory 업데이트                              │
│    - Plan 진행상태 업데이트                       │
└─────────────────────────────────────────────────┘
```

---

## Zero-Tolerance 규칙 (필수)

### ✅ MUST
- **CQRS 분리**: Command/Query UseCase 분리
- **Transaction 경계**: `@Transactional` 내 외부 API 호출 금지
- **DTO는 Record**: Command, Query, Response 모두 Record
- **Assembler 사용**: Domain ↔ DTO 변환

### ❌ NEVER
```java
// ❌ Transaction 내 외부 API 호출
@Transactional
public OrderResponse cancelOrder(CancelOrderCommand cmd) {
    order.cancel();
    paymentGateway.refund();  // 🚨 절대 금지!
    return response;
}

// ❌ DTO에 class 사용
public class CancelOrderCommand {  // Record 사용해야 함
    private Long orderId;
}
```

---

## 생성 대상

### 1. Port In (UseCase Interface)

```java
// application/src/main/java/{basePackage}/application/{feature}/port/in/
public interface CancelOrderUseCase {

    /**
     * 주문을 취소합니다.
     *
     * @param command 취소 요청 정보
     * @return 취소된 주문 정보
     * @throws OrderNotFoundException 주문이 존재하지 않는 경우
     * @throws OrderCannotBeCancelledException 취소 불가능한 상태인 경우
     */
    OrderResponse cancel(CancelOrderCommand command);
}
```

### 2. Command DTO (Record)

```java
// application/src/main/java/{basePackage}/application/{feature}/dto/
public record CancelOrderCommand(
    Long orderId,
    String reason
) {
    public CancelOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason cannot be blank");
        }
    }

    public static CancelOrderCommand of(Long orderId, String reason) {
        return new CancelOrderCommand(orderId, reason);
    }
}
```

### 3. Service 구현체

```java
// application/src/main/java/{basePackage}/application/{feature}/service/
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderPersistencePort orderPersistencePort;
    private final OrderAssembler orderAssembler;

    public CancelOrderService(
            OrderQueryPort orderQueryPort,
            OrderPersistencePort orderPersistencePort,
            OrderAssembler orderAssembler) {
        this.orderQueryPort = orderQueryPort;
        this.orderPersistencePort = orderPersistencePort;
        this.orderAssembler = orderAssembler;
    }

    @Override
    @Transactional
    public OrderResponse cancel(CancelOrderCommand command) {
        // 1. 조회
        Order order = orderQueryPort.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2. 도메인 로직 실행
        CancelReason reason = CancelReason.of(command.reason());
        order.cancel(reason);

        // 3. 저장
        Order savedOrder = orderPersistencePort.save(order);

        // 4. 응답 변환
        return orderAssembler.toResponse(savedOrder);
    }
}
```

### 4. Port Out (Query/Persistence)

```java
// application/src/main/java/{basePackage}/application/{feature}/port/out/
public interface OrderQueryPort {
    Optional<Order> findById(Long orderId);
    List<Order> findByCustomerId(Long customerId);
}

public interface OrderPersistencePort {
    Order save(Order order);
    void delete(Order order);
}
```

### 5. Assembler

```java
// application/src/main/java/{basePackage}/application/{feature}/assembler/
@Component
public class OrderAssembler {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getId().value(),
            order.getStatus().name(),
            order.getTotalPrice()
        );
    }

    public Order toDomain(CancelOrderCommand command, Order existingOrder) {
        // 기존 Order에 변경 적용 후 반환
        return existingOrder;
    }
}
```

### 6. Response DTO

```java
// application/src/main/java/{basePackage}/application/{feature}/dto/
public record OrderResponse(
    String orderId,
    String status,
    BigDecimal totalPrice
) {
    public static OrderResponse of(String orderId, String status, BigDecimal totalPrice) {
        return new OrderResponse(orderId, status, totalPrice);
    }
}
```

---

## 외부 API 호출 패턴

### ⚠️ Transaction 밖에서 호출 (Orchestration Pattern)

```java
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderQueryPort orderQueryPort;
    private final OrderPersistencePort orderPersistencePort;
    private final RefundPort refundPort;  // 외부 API
    private final OrderAssembler orderAssembler;
    private final TransactionTemplate transactionTemplate;

    @Override
    public OrderResponse cancel(CancelOrderCommand command) {
        // 1. Transaction 내: 주문 취소 + 재고 복구
        Order savedOrder = transactionTemplate.execute(status -> {
            Order order = orderQueryPort.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

            order.cancel(CancelReason.of(command.reason()));
            return orderPersistencePort.save(order);
        });

        // 2. Transaction 밖: 환불 API 호출
        refundPort.requestRefund(savedOrder.getId());

        return orderAssembler.toResponse(savedOrder);
    }
}
```

---

## 테스트 작성

### Unit Test (Mock Port Out)

```java
// application/src/test/java/{basePackage}/application/{feature}/
class CancelOrderServiceTest {

    private CancelOrderService sut;
    private OrderQueryPort orderQueryPort;
    private OrderPersistencePort orderPersistencePort;
    private OrderAssembler orderAssembler;

    @BeforeEach
    void setUp() {
        orderQueryPort = mock(OrderQueryPort.class);
        orderPersistencePort = mock(OrderPersistencePort.class);
        orderAssembler = new OrderAssembler();

        sut = new CancelOrderService(
            orderQueryPort,
            orderPersistencePort,
            orderAssembler
        );
    }

    @Test
    @DisplayName("주문 취소 - 성공")
    void shouldCancelOrder() {
        // Given
        Long orderId = 1L;
        Order order = OrderFixture.createPlaced();
        CancelOrderCommand command = CancelOrderCommand.of(orderId, "고객 요청");

        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));
        when(orderPersistencePort.save(any())).thenReturn(order);

        // When
        OrderResponse response = sut.cancel(command);

        // Then
        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(orderPersistencePort).save(any());
    }

    @Test
    @DisplayName("주문 취소 - 존재하지 않는 주문")
    void shouldThrowWhenOrderNotFound() {
        // Given
        CancelOrderCommand command = CancelOrderCommand.of(999L, "고객 요청");
        when(orderQueryPort.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sut.cancel(command))
            .isInstanceOf(OrderNotFoundException.class);
    }
}
```

---

## 커밋 규칙

```bash
# 구현 + 테스트 함께 커밋
git commit -m "feat: 주문 취소 UseCase 구현 (Application Layer)

- CancelOrderUseCase 인터페이스 정의
- CancelOrderService 구현
- CancelOrderCommand DTO 추가
- OrderAssembler 추가
- Unit 테스트 추가"
```

---

## Memory 업데이트

구현 완료 후 Plan 상태 업데이트:

```python
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle="- [ ] Application Layer",
    repl="- [x] Application Layer (completed)",
    mode="literal"
)
```

---

## 다음 단계

Application Layer 완료 후:

```bash
/impl persistence {feature}  # Persistence Layer 구현
```
