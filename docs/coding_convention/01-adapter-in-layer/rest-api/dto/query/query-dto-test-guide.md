# Query DTO Test Guide — **Query DTO 테스트 가이드**

> Query DTO의 **단위 테스트** 및 **기본값 검증 테스트** 가이드입니다.
>
> **외부 의존성 없이**, **순수 Java 단위 테스트**로 빠르게 검증합니다.

---

## 1) 테스트 원칙

* **순수 Java 단위 테스트**: Spring Context 로딩 불필요
* **빠른 실행**: 밀리초 단위 (외부 의존성 없음)
* **기본값 검증**: Compact Constructor 기본값 테스트
* **날짜 범위 검증**: startDate ≤ endDate 검증
* **페이징 검증**: page, size 기본값 및 범위 테스트
* **Optional 필드 검증**: null 허용 필드 테스트
* **@Tag 필수**: `@Tag("unit")`, `@Tag("adapter-rest")`, `@Tag("dto")` 사용

---

## 2) 단위 테스트 (기본값 검증)

### 기본 생성 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class OrderSearchApiRequestTest {

    @Test
    void givenValidRequest_whenCreate_thenSuccess() {
        // given
        var request = new OrderSearchApiRequest(
            1L,
            "PLACED",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
            0,
            20
        );

        // when & then
        assertThat(request.customerId()).isEqualTo(1L);
        assertThat(request.status()).isEqualTo("PLACED");
        assertThat(request.page()).isEqualTo(0);
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void givenNullPage_whenCreate_thenDefaultValue() {
        // given & when
        var request = new OrderSearchApiRequest(
            1L,
            null,
            null,
            null,
            null,  // ✅ page null
            null   // ✅ size null
        );

        // then
        assertThat(request.page()).isEqualTo(0);   // 기본값
        assertThat(request.size()).isEqualTo(20);  // 기본값
    }

    @Test
    void givenNullOptionalFields_whenCreate_thenNull() {
        // given & when
        var request = new OrderSearchApiRequest(
            null,  // ✅ customerId null 허용
            null,  // ✅ status null 허용
            null,  // ✅ startDate null 허용
            null,  // ✅ endDate null 허용
            0,
            20
        );

        // then
        assertThat(request.customerId()).isNull();
        assertThat(request.status()).isNull();
        assertThat(request.startDate()).isNull();
        assertThat(request.endDate()).isNull();
    }
}
```

### Compact Constructor 검증 테스트

```java
@Test
void givenInvalidDateRange_whenCreate_thenThrowException() {
    // when & then
    assertThatThrownBy(() -> new OrderSearchApiRequest(
        1L,
        null,
        LocalDate.of(2025, 12, 31),  // endDate가 startDate보다 이전
        LocalDate.of(2025, 1, 1),    // startDate
        0,
        20
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("시작 날짜는 종료 날짜보다 이전이어야 합니다");
}

@Test
void givenNegativePage_whenCreate_thenDefaultValue() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        null,
        null,
        -1,  // ✅ 음수는 0으로 변환
        null
    );

    // then
    assertThat(request.page()).isEqualTo(0);  // 기본값으로 변환
}

@Test
void givenTooLargeSize_whenCreate_thenMaxValue() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        null,
        null,
        0,
        200  // ✅ 100 초과 시 100으로 제한
    );

    // then
    assertThat(request.size()).isEqualTo(100);  // 최대값으로 제한
}
```

---

## 3) Bean Validation 테스트

### Validator 설정

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class OrderSearchApiRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void givenInvalidPage_whenValidate_thenViolation() {
        // given
        var request = new OrderSearchApiRequest(
            1L,
            null,
            null,
            null,
            -1,  // ❌ @Min(0) 위반
            20
        );

        // when
        Set<ConstraintViolation<OrderSearchApiRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("페이지 번호는 0 이상이어야 합니다");
    }

    @Test
    void givenInvalidSize_whenValidate_thenViolation() {
        // given
        var request = new OrderSearchApiRequest(
            1L,
            null,
            null,
            null,
            0,
            0  // ❌ @Min(1) 위반
        );

        // when
        Set<ConstraintViolation<OrderSearchApiRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("페이지 크기는 1 이상이어야 합니다");
    }

    @Test
    void givenTooLargeSize_whenValidate_thenViolation() {
        // given
        var request = new OrderSearchApiRequest(
            1L,
            null,
            null,
            null,
            0,
            200  // ❌ @Max(100) 위반
        );

        // when
        Set<ConstraintViolation<OrderSearchApiRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("페이지 크기는 100 이하여야 합니다");
    }
}
```

---

## 4) 패턴별 테스트

### 날짜 범위 검증 테스트

```java
@Test
void givenValidDateRange_whenCreate_thenSuccess() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        LocalDate.of(2025, 1, 1),   // startDate
        LocalDate.of(2025, 12, 31),  // endDate
        0,
        20
    );

    // then
    assertThat(request.startDate()).isBefore(request.endDate());
}

@Test
void givenSameDateRange_whenCreate_thenSuccess() {
    // given
    var sameDate = LocalDate.of(2025, 6, 15);

    // when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        sameDate,  // startDate
        sameDate,  // endDate (같은 날짜 허용)
        0,
        20
    );

    // then
    assertThat(request.startDate()).isEqualTo(request.endDate());
}

@Test
void givenOnlyStartDate_whenCreate_thenEndDateNull() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        LocalDate.of(2025, 1, 1),  // startDate만 있음
        null,                       // endDate null
        0,
        20
    );

    // then
    assertThat(request.startDate()).isNotNull();
    assertThat(request.endDate()).isNull();
}
```

### 페이징 기본값 테스트

```java
@Test
void givenNullPageAndSize_whenCreate_thenDefaultValues() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        null,
        null,
        null,  // page null
        null   // size null
    );

    // then
    assertThat(request.page()).isEqualTo(0);   // 기본값
    assertThat(request.size()).isEqualTo(20);  // 기본값
}

@Test
void givenCustomPageAndSize_whenCreate_thenUseProvidedValues() {
    // given & when
    var request = new OrderSearchApiRequest(
        1L,
        null,
        null,
        null,
        5,   // 사용자 지정 page
        50   // 사용자 지정 size
    );

    // then
    assertThat(request.page()).isEqualTo(5);
    assertThat(request.size()).isEqualTo(50);
}
```

---

## 5) 테스트 조직화

### 테스트 클래스 구조

```
test/
└─ adapter-in/rest-api/
   └─ {bc}/dto/query/
      ├─ OrderSearchApiRequestTest.java           # 기본 생성 테스트
      ├─ OrderSearchApiRequestValidationTest.java # Bean Validation 테스트
      ├─ ProductSearchApiRequestTest.java
      └─ CustomerListApiRequestTest.java
```

### 테스트 네이밍 규칙

| 테스트 유형 | 접미사 | 예시 |
|-------------|--------|------|
| 기본 단위 테스트 | `*Test` | `OrderSearchApiRequestTest` |
| Bean Validation | `*ValidationTest` | `OrderSearchApiRequestValidationTest` |

---

## 6) 테스트 커버리지

### 필수 테스트 항목

- [ ] **생성 성공**: 유효한 데이터로 생성
- [ ] **기본값 설정**: page, size null 시 기본값
- [ ] **Optional 필드**: null 허용 필드 검증
- [ ] **날짜 범위**: startDate ≤ endDate 검증
- [ ] **페이징 범위**: page ≥ 0, 1 ≤ size ≤ 100
- [ ] **@Min/@Max**: 범위 검증
- [ ] **Nested Record**: 중첩 구조 검증 (있을 경우)
- [ ] **Compact Constructor**: 기본값 및 검증 로직

---

## 7) 테스트 예시 (전체)

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class OrderSearchApiRequestTest {

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
            var request = new OrderSearchApiRequest(
                1L,
                "PLACED",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                0,
                20
            );

            // then
            assertThat(request.customerId()).isEqualTo(1L);
            assertThat(request.page()).isEqualTo(0);
            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("null page/size는 기본값으로 변환")
        void givenNullPageSize_whenCreate_thenDefaultValues() {
            var request = new OrderSearchApiRequest(1L, null, null, null, null, null);
            assertThat(request.page()).isEqualTo(0);
            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("Optional 필드는 null 허용")
        void givenNullOptionalFields_whenCreate_thenNull() {
            var request = new OrderSearchApiRequest(null, null, null, null, 0, 20);
            assertThat(request.customerId()).isNull();
            assertThat(request.status()).isNull();
        }
    }

    @Nested
    @DisplayName("날짜 범위 검증")
    class DateRangeTest {

        @Test
        @DisplayName("잘못된 날짜 범위면 예외")
        void givenInvalidDateRange_whenCreate_thenThrow() {
            assertThatThrownBy(() -> new OrderSearchApiRequest(
                1L, null,
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2025, 1, 1),
                0, 20
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("시작 날짜는 종료 날짜보다 이전");
        }

        @Test
        @DisplayName("같은 날짜는 허용")
        void givenSameDate_whenCreate_thenSuccess() {
            var sameDate = LocalDate.of(2025, 6, 15);
            var request = new OrderSearchApiRequest(1L, null, sameDate, sameDate, 0, 20);
            assertThat(request.startDate()).isEqualTo(request.endDate());
        }
    }

    @Nested
    @DisplayName("Bean Validation 테스트")
    class ValidationTest {

        @Test
        @DisplayName("음수 page면 위반")
        void givenNegativePage_whenValidate_thenViolation() {
            var request = new OrderSearchApiRequest(1L, null, null, null, -1, 20);
            Set<ConstraintViolation<OrderSearchApiRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .contains("페이지 번호는 0 이상");
        }

        @Test
        @DisplayName("size가 100 초과면 위반")
        void givenTooLargeSize_whenValidate_thenViolation() {
            var request = new OrderSearchApiRequest(1L, null, null, null, 0, 200);
            Set<ConstraintViolation<OrderSearchApiRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .contains("100 이하");
        }
    }
}
```

---

## 8) 체크리스트

- [ ] 기본 생성 테스트 작성
- [ ] 기본값 설정 테스트 (page, size)
- [ ] Optional 필드 null 허용 테스트
- [ ] 날짜 범위 검증 테스트
- [ ] Bean Validation 테스트
- [ ] 페이징 범위 검증
- [ ] @Nested, @DisplayName 활용
- [ ] Given-When-Then 패턴
- [ ] @Tag 3개 필수 추가

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
