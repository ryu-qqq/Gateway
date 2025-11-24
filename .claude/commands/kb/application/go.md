# Application Layer TDD Go - Execute Next Test from Plan

You are executing the Kent Beck TDD + Tidy First workflow for **Application Layer**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-application-plan.md`
2. **Find the next unmarked test** in the Application Layer section
3. **Mark the test as in-progress** by adding a checkbox or marker
4. **Execute the TDD Cycle (3 phases)**:
   - **🔴 RED**: Write the simplest failing test first → `test:` 커밋
   - **🟢 GREEN**: Implement minimum code to make the test pass → `feat:` 커밋
   - **♻️ REFACTOR**: Improve structure only after tests pass → `struct:` 커밋
5. **Apply Tidy First** principle:
   - If Structural Changes needed, do them FIRST → `struct:` 커밋
   - Then proceed with Behavioral Changes (Red → Green)
   - Never mix Structural and Behavioral in same commit
6. **Run all tests** (excluding long-running tests)
7. **Verify** all tests pass before proceeding
8. **Mark test complete** in plan file

## Application Layer Specific Rules

### Zero-Tolerance Rules (MUST follow)
- ✅ **Transaction 경계**: `@Transactional` 내 외부 API 호출 절대 금지
- ✅ **Spring Proxy 제약**: Private/Final 메서드에 `@Transactional` 금지
- ✅ **CQRS 분리**: Command UseCase와 Query UseCase 명확히 분리
- ✅ **Assembler 사용**: UseCase 내부 DTO 변환은 Assembler에 위임
- ✅ **Port 명명 규칙**:
  - Command: `Save*Port`, `Delete*Port`
  - Query: `Load*Port`, `Find*Port`

### TestFixture Pattern (MANDATORY)
**Application Layer에서는 TestFixture가 필수입니다**:

```java
// ✅ CORRECT (Use Fixture)
@Test
@DisplayName("주문 생성 - 정상 케이스")
void shouldPlaceOrder() {
    // Given
    PlaceOrderCommand command = PlaceOrderCommandFixture.create();
    OrderDomain order = OrderDomainFixture.create();

    given(loadCustomerPort.loadById(command.customerId()))
        .willReturn(Optional.of(CustomerFixture.create()));
    given(saveOrderPort.save(any(OrderDomain.class)))
        .willReturn(order);

    // When
    OrderResponse response = placeOrderUseCase.execute(command);

    // Then
    assertThat(response.orderId()).isNotNull();
    assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
}

// ❌ WRONG (Inline object creation)
@Test
void shouldPlaceOrder() {
    PlaceOrderCommand command = new PlaceOrderCommand(1L, 100L, 10);
    // ...
}
```

**Fixture 위치**: `application/src/testFixtures/java/{basePackage}/application/fixture/`

### Application Test Focus
- **UseCase 비즈니스 로직 테스트**:
  - Command UseCase: 트랜잭션 내부 로직 검증
  - Query UseCase: 조회 로직 및 DTO 변환 검증
  - Validation: 입력 검증 로직
- **Assembler 테스트**:
  - Command → Domain 변환
  - Domain → Response 변환
  - DTO 불변성 검증
- **Port Mocking**:
  - Outbound Port Mock 사용
  - Interaction 검증 (verify)

## Core Principles (Kent Beck + Tidy First)

- **Tidy First**: Structural Changes BEFORE Behavioral Changes
- **Never mix** Structural and Behavioral in same commit
- **3 commit types**: `test:` (Red) → `feat:` (Green) → `struct:` (Refactor)
- Write ONE test at a time
- Make it run with minimum code
- Improve structure ONLY after green
- Run ALL tests after each change
- Never skip the Red phase
- **ALWAYS use TestFixture** (Application Layer 필수!)

## Success Criteria

- ✅ Plan file updated (test marked as in-progress)
- ✅ Structural Changes (if needed) → `struct:` 커밋
- ✅ Test written and initially failing (RED) → `test:` 커밋
- ✅ Minimum code makes test pass (GREEN) → `feat:` 커밋
- ✅ Code structure improved if needed (REFACTOR) → `struct:` 커밋
- ✅ TestFixture used (NOT inline object creation)
- ✅ All tests passing
- ✅ Zero-Tolerance rules followed (Transaction 경계, Spring Proxy, CQRS)
- ✅ Commit messages follow prefix rules (`test:`/`feat:`/`struct:`)
- ✅ Test marked complete in plan file

## What NOT to Do

- ❌ Don't work on Domain, Persistence, or REST API code
- ❌ Don't create tests without TestFixture
- ❌ Don't call external APIs inside `@Transactional` methods
- ❌ Don't use `@Transactional` on private/final methods
- ❌ Don't mix Command and Query UseCase logic

## Example Workflow

```bash
# 1. User: /kb:application:go
# 2. Claude: Reads docs/prd/plans/PROJ-123-application-plan.md
# 3. Claude: Finds next test: "주문 생성 UseCase - 정상 케이스"
# 4. Claude: Marks test as in-progress

# 5. 🔴 RED Phase
#    - Writes PlaceOrderUseCaseTest.java (failing test)
#    - Uses PlaceOrderCommandFixture for test data
#    - git commit -m "test: 주문 생성 UseCase 테스트 추가"

# 6. 🟢 GREEN Phase
#    - Implements PlaceOrderUseCase.execute() (minimum code)
#    - Follows Zero-Tolerance rules (Transaction 경계, CQRS)
#    - git commit -m "feat: 주문 생성 UseCase 구현 (최소 코드)"

# 7. ♻️ REFACTOR Phase (if needed)
#    - Extracts validation method (NO behavior change)
#    - git commit -m "struct: extract validation logic in PlaceOrderUseCase"

# 8. Claude: Runs all tests (./gradlew test)
# 9. Claude: Marks test as complete
```

## Transaction Boundary Example

```java
// ✅ CORRECT (Transaction 경계 관리)
@UseCase
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderPort {

    private final LoadCustomerPort loadCustomerPort;
    private final SaveOrderPort saveOrderPort;
    private final PaymentClient paymentClient;  // External API

    @Override
    @Transactional
    public OrderResponse execute(PlaceOrderCommand command) {
        // 1. 트랜잭션 내부 로직
        CustomerDomain customer = loadCustomerPort.loadById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        OrderDomain order = OrderDomain.create(
            OrderId.generate(),
            command.customerId(),
            command.productId(),
            command.quantity()
        );

        OrderDomain savedOrder = saveOrderPort.save(order);

        // 2. 트랜잭션 외부로 위임 (외부 API 호출)
        return executeExternalOperations(savedOrder);
    }

    // 트랜잭션 밖에서 외부 API 호출
    private OrderResponse executeExternalOperations(OrderDomain order) {
        // 외부 결제 API 호출
        PaymentResult paymentResult = paymentClient.requestPayment(
            order.getOrderId(),
            order.getTotalPrice()
        );

        if (!paymentResult.isSuccess()) {
            throw new PaymentFailedException(paymentResult.getReason());
        }

        return OrderAssembler.toResponse(order);
    }
}
```

Follow the workflow from CLAUDE.md precisely. Stop and report if any step fails.
