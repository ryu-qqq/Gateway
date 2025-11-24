# Domain Layer TDD Go - Execute Next Test from Plan

You are executing the **Kent Beck TDD + Tidy First** workflow for **Domain Layer**.

## Instructions

1. **Read plan file** from `docs/prd/plans/{ISSUE-KEY}-domain-plan.md`
2. **Find the next unmarked test** in the Domain Layer section
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

## Domain Layer Specific Rules

### Zero-Tolerance Rules (MUST follow)
- ✅ **Lombok 금지**: Pure Java 또는 Record 패턴 사용
- ✅ **Law of Demeter**: Getter 체이닝 금지 (`order.getCustomer().getAddress()` ❌)
- ✅ **Tell, Don't Ask**: 비즈니스 로직은 Domain 내부에 캡슐화
- ✅ **Long FK 전략**: JPA 관계 어노테이션 금지 (`private Long customerId;` 사용)

### TestFixture Pattern (MANDATORY)
**Domain Layer에서는 TestFixture가 필수입니다**:

```java
// ✅ CORRECT (Use Fixture)
@Test
@DisplayName("주문 취소 - PLACED 상태만 가능")
void shouldCancelOrderWhenPlaced() {
    // Given
    OrderDomain order = OrderDomainFixture.create();

    // When
    order.cancel(CancelReason.CUSTOMER_REQUEST);

    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}

// ❌ WRONG (Inline object creation)
@Test
void shouldCancelOrder() {
    OrderDomain order = OrderDomain.create(1L, 2L, 3L, 10, OrderStatus.PLACED, ...);
    // ...
}
```

**Fixture 위치**: `domain/src/testFixtures/java/{basePackage}/domain/fixture/`

### Domain Test Focus
- **Aggregate 비즈니스 로직 테스트**:
  - State transition (상태 전환)
  - Invariant validation (불변식 검증)
  - Business rule enforcement (비즈니스 규칙 강제)
- **Value Object 테스트**:
  - Immutability (불변성)
  - Validation (검증 로직)
  - Equality (동등성)

## Core Principles (Kent Beck + Tidy First)

- Write ONE test at a time
- Make it run with minimum code
- Improve structure ONLY after green
- Run ALL tests after each change
- Never skip the Red phase
- **Tidy First**: Structural Changes BEFORE Behavioral Changes
- **Never mix** Structural and Behavioral in same commit
- **3 commit types**: `test:` (Red) → `feat:` (Green) → `struct:` (Refactor)
- **ALWAYS use TestFixture** (Domain Layer 필수!)

## Success Criteria

- ✅ Plan file updated (test marked as in-progress)
- ✅ Structural Changes (if needed) → `struct:` 커밋
- ✅ Test written and initially failing (RED) → `test:` 커밋
- ✅ Minimum code makes test pass (GREEN) → `feat:` 커밋
- ✅ Code structure improved if needed (REFACTOR) → `struct:` 커밋
- ✅ TestFixture used (NOT inline object creation)
- ✅ All tests passing
- ✅ Commit messages follow prefix rules (`test:`/`feat:`/`struct:`)
- ✅ Zero-Tolerance rules followed (Lombok 금지, Law of Demeter, Long FK 전략)
- ✅ Test marked complete in plan file

## What NOT to Do

- ❌ Don't work on Application, Persistence, or REST API code
- ❌ Don't create tests without TestFixture
- ❌ Don't use Lombok (`@Data`, `@Getter`, `@Setter`, etc.)
- ❌ Don't use Getter chaining (`order.getCustomer().getAddress()`)
- ❌ Don't use JPA relationship annotations (`@ManyToOne`, `@OneToMany`, etc.)

## Example Workflow

```bash
# 1. User: /kb/domain/go
# 2. Claude: Reads docs/prd/plans/PROJ-123-domain-plan.md
# 3. Claude: Finds next test: "Email VO 검증"
# 4. Claude: Marks test as in-progress

# 5. 🔴 RED Phase
#    - Writes EmailTest.java (failing test)
#    - git commit -m "test: Email VO 검증 테스트 추가"

# 6. 🟢 GREEN Phase
#    - Implements Email.java (minimum code)
#    - git commit -m "feat: Email VO 구현 (RFC 5322 검증)"

# 7. ♻️ REFACTOR Phase (if needed)
#    - Extracts validation method (NO behavior change)
#    - git commit -m "struct: Email 검증 로직 메서드 추출"

# 8. Runs all tests (./gradlew test)
# 9. Marks test as complete
```

**Tidy First Example**:
```bash
# If variable rename needed BEFORE adding new feature:
# 1. struct: Rename variable → commit
# 2. test: Add test → commit
# 3. feat: Implement → commit
# NEVER: feat: Implement + rename variable (mixed!)
```

Follow the workflow from CLAUDE.md precisely. Stop and report if any step fails.
