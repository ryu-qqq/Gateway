# Command DTO Test Guide — **Command DTO 테스트 가이드**

> Command DTO의 **단위 테스트** 및 **Bean Validation 테스트** 가이드입니다.
>
> **외부 의존성 없이**, **순수 Java 단위 테스트**로 빠르게 검증합니다.

---

## 1) 테스트 원칙

* **순수 Java 단위 테스트**: Spring Context 로딩 불필요
* **빠른 실행**: 밀리초 단위 (외부 의존성 없음)
* **Validation 검증**: Bean Validation 어노테이션 테스트
* **Compact Constructor 검증**: 추가 검증 로직 테스트
* **불변성 검증**: 컬렉션 방어적 복사 테스트
* **@Tag 필수**: `@Tag("unit")`, `@Tag("adapter-rest")`, `@Tag("dto")` 사용

---

## 2) 단위 테스트 (Validation)

### 기본 생성 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class CreateOrderApiRequestTest {

    @Test
    void givenValidRequest_whenCreate_thenSuccess() {
        // given
        var request = new CreateOrderApiRequest(
            1L,
            List.of(new OrderItemRequest(1L, 2, 10000L)),
            new AddressRequest("12345", "Seoul", null)
        );

        // when & then
        assertThat(request.customerId()).isEqualTo(1L);
        assertThat(request.items()).hasSize(1);
        assertThat(request.items()).isUnmodifiable();  // ✅ 불변 리스트
    }
}
```

### Compact Constructor 검증 테스트

```java
@Test
void givenInvalidCustomerId_whenCreate_thenThrowException() {
    // when & then
    assertThatThrownBy(() -> new CreateOrderApiRequest(
        -1L,  // ❌ 잘못된 ID (Compact Constructor에서 검증)
        List.of(),
        null
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("유효하지 않은 고객 ID");
}

@Test
void givenNullItems_whenCreate_thenEmptyList() {
    // given & when
    var request = new CreateOrderApiRequest(
        1L,
        null,  // ✅ null → 빈 리스트로 변환
        null
    );

    // then
    assertThat(request.items()).isEmpty();
    assertThat(request.items()).isUnmodifiable();
}
```

---

## 3) Bean Validation 테스트

### Validator 설정

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class CreateOrderApiRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void givenNullCustomerId_whenValidate_thenViolation() {
        // given
        var request = new CreateOrderApiRequest(
            null,  // ❌ @NotNull 위반
            List.of(),
            null
        );

        // when
        Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("고객 ID는 필수입니다");
    }

    @Test
    void givenEmptyItems_whenValidate_thenViolation() {
        // given
        var request = new CreateOrderApiRequest(
            1L,
            List.of(),  // ❌ @NotEmpty 위반
            null
        );

        // when
        Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("주문 항목은 필수입니다");
    }
}
```

### Nested Record Validation 테스트

```java
@Test
void givenInvalidNestedItem_whenValidate_thenViolation() {
    // given
    var invalidItem = new OrderItemRequest(
        null,  // ❌ productId null
        0,     // ❌ quantity < 1
        -1L    // ❌ price < 0
    );
    var request = new CreateOrderApiRequest(
        1L,
        List.of(invalidItem),
        null
    );

    // when
    Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(3);  // productId, quantity, price
}
```

---

## 4) 패턴별 테스트

### 날짜 범위 검증 테스트

```java
@Test
void givenInvalidDateRange_whenCreate_thenThrowException() {
    // when & then
    assertThatThrownBy(() -> new OrderSearchApiRequest(
        LocalDate.of(2025, 12, 31),  // endDate
        LocalDate.of(2025, 1, 1)     // startDate (endDate보다 나중)
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("시작 날짜는 종료 날짜보다 이전이어야 합니다");
}
```

### 컬렉션 불변성 테스트

```java
@Test
void givenMutableList_whenCreate_thenImmutableList() {
    // given
    List<OrderItemRequest> mutableList = new ArrayList<>();
    mutableList.add(new OrderItemRequest(1L, 1, 10000L));

    // when
    var request = new CreateOrderApiRequest(1L, mutableList, null);

    // then
    assertThatThrownBy(() -> request.items().add(new OrderItemRequest(2L, 1, 20000L)))
        .isInstanceOf(UnsupportedOperationException.class);  // ✅ 불변
}
```

---

## 5) 테스트 조직화

### 테스트 클래스 구조

```
test/
└─ adapter-in/rest-api/
   └─ {bc}/dto/command/
      ├─ CreateOrderApiRequestTest.java           # 기본 생성 테스트
      ├─ CreateOrderApiRequestValidationTest.java # Bean Validation 테스트
      ├─ UpdateOrderStatusApiRequestTest.java
      └─ CancelOrderApiRequestTest.java
```

### 테스트 네이밍 규칙

| 테스트 유형 | 접미사 | 예시 |
|-------------|--------|------|
| 기본 단위 테스트 | `*Test` | `CreateOrderApiRequestTest` |
| Bean Validation | `*ValidationTest` | `CreateOrderApiRequestValidationTest` |

---

## 6) 테스트 커버리지

### 필수 테스트 항목

- [ ] **생성 성공**: 유효한 데이터로 생성
- [ ] **Compact Constructor**: 추가 검증 로직
- [ ] **@NotNull**: null 필드 검증
- [ ] **@NotEmpty**: 빈 컬렉션 검증
- [ ] **@Min/@Max**: 범위 검증
- [ ] **@Pattern**: 패턴 검증
- [ ] **Nested Record**: 중첩 구조 검증
- [ ] **불변성**: 컬렉션 수정 불가
- [ ] **날짜 범위**: startDate ≤ endDate

---

## 7) 테스트 예시 (전체)

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class CreateOrderApiRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 데이터로 생성 성공")
        void givenValidData_whenCreate_thenSuccess() {
            // given
            var items = List.of(new OrderItemRequest(1L, 2, 10000L));
            var address = new AddressRequest("12345", "Seoul", null);

            // when
            var request = new CreateOrderApiRequest(1L, items, address);

            // then
            assertThat(request.customerId()).isEqualTo(1L);
            assertThat(request.items()).hasSize(1);
            assertThat(request.items()).isUnmodifiable();
        }

        @Test
        @DisplayName("잘못된 customerId면 예외")
        void givenInvalidCustomerId_whenCreate_thenThrow() {
            assertThatThrownBy(() -> new CreateOrderApiRequest(-1L, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 고객 ID");
        }

        @Test
        @DisplayName("null items는 빈 리스트로 변환")
        void givenNullItems_whenCreate_thenEmptyList() {
            var request = new CreateOrderApiRequest(1L, null, null);
            assertThat(request.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bean Validation 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null customerId면 위반")
        void givenNullCustomerId_whenValidate_thenViolation() {
            var request = new CreateOrderApiRequest(null, List.of(), null);
            Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("고객 ID는 필수입니다");
        }

        @Test
        @DisplayName("빈 items면 위반")
        void givenEmptyItems_whenValidate_thenViolation() {
            var request = new CreateOrderApiRequest(1L, List.of(), null);
            Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("주문 항목은 필수입니다");
        }

        @Test
        @DisplayName("잘못된 Nested Record면 위반")
        void givenInvalidNestedItem_whenValidate_thenViolation() {
            var invalidItem = new OrderItemRequest(null, 0, -1L);
            var request = new CreateOrderApiRequest(1L, List.of(invalidItem), null);

            Set<ConstraintViolation<CreateOrderApiRequest>> violations = validator.validate(request);
            assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
        }
    }
}
```

---

## 8) 체크리스트

- [ ] 기본 생성 테스트 작성
- [ ] Compact Constructor 검증 테스트
- [ ] Bean Validation 테스트
- [ ] Nested Record Validation 테스트
- [ ] 불변성 검증 (컬렉션)
- [ ] 날짜 범위 검증
- [ ] @Nested, @DisplayName 활용
- [ ] Given-When-Then 패턴

---

**작성자**: Development Team  
**최종 수정일**: 2025-11-13  
**버전**: 1.0.0
