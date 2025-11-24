# Response DTO Test Guide — **Response DTO 테스트 가이드**

> Response DTO의 **단위 테스트** 및 **from() 메서드 변환 테스트** 가이드입니다.
>
> **외부 의존성 없이**, **순수 Java 단위 테스트**로 빠르게 검증합니다.

---

## 1) 테스트 원칙

* **순수 Java 단위 테스트**: Spring Context 로딩 불필요
* **빠른 실행**: 밀리초 단위 (외부 의존성 없음)
* **from() 메서드 검증**: Application Layer → REST API Layer 변환 테스트
* **불변성 검증**: 컬렉션 방어적 복사 테스트 (List.copyOf())
* **Nested Record 검증**: 중첩 구조 변환 테스트
* **Slice/Page 생성 검증**: SliceApiResponse/PageApiResponse from() 메서드 테스트
* **@Tag 필수**: `@Tag("unit")`, `@Tag("adapter-rest")`, `@Tag("dto")` 사용

---

## 2) 단위 테스트 (from() 메서드 변환)

### 기본 from() 메서드 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class OrderApiResponseTest {

    @Test
    void givenApplicationResponse_whenFrom_thenConvertSuccess() {
        // given
        var appResponse = new OrderResponse(
            1L,
            100L,
            "PLACED",
            50000L,
            List.of(new OrderItemDto(1L, "Product A", 2, 25000L)),
            LocalDateTime.of(2025, 1, 1, 10, 0)
        );

        // when
        var apiResponse = OrderApiResponse.from(appResponse);

        // then
        assertThat(apiResponse.orderId()).isEqualTo(1L);
        assertThat(apiResponse.customerId()).isEqualTo(100L);
        assertThat(apiResponse.status()).isEqualTo("PLACED");
        assertThat(apiResponse.totalAmount()).isEqualTo(50000L);
        assertThat(apiResponse.items()).hasSize(1);
        assertThat(apiResponse.createdAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
    }

    @Test
    void givenApplicationResponseWithNullFields_whenFrom_thenHandleNull() {
        // given
        var appResponse = new OrderResponse(
            1L,
            100L,
            "PLACED",
            50000L,
            null,  // ✅ null items
            LocalDateTime.of(2025, 1, 1, 10, 0)
        );

        // when
        var apiResponse = OrderApiResponse.from(appResponse);

        // then
        assertThat(apiResponse.items()).isEmpty();  // null → 빈 리스트
    }
}
```

### Nested Record 변환 테스트

```java
@Test
void givenApplicationResponseWithNestedItems_whenFrom_thenConvertNestedRecords() {
    // given
    var appItems = List.of(
        new OrderItemDto(1L, "Product A", 2, 25000L),
        new OrderItemDto(2L, "Product B", 1, 30000L)
    );
    var appResponse = new OrderResponse(
        1L,
        100L,
        "PLACED",
        55000L,
        appItems,
        LocalDateTime.of(2025, 1, 1, 10, 0)
    );

    // when
    var apiResponse = OrderApiResponse.from(appResponse);

    // then
    assertThat(apiResponse.items()).hasSize(2);
    assertThat(apiResponse.items().get(0).productId()).isEqualTo(1L);
    assertThat(apiResponse.items().get(0).productName()).isEqualTo("Product A");
    assertThat(apiResponse.items().get(1).productId()).isEqualTo(2L);
    assertThat(apiResponse.items().get(1).productName()).isEqualTo("Product B");
}
```

---

## 3) 불변성 테스트 (Immutable Collections)

### 컬렉션 수정 불가 테스트

```java
@Test
void givenApiResponse_whenModifyList_thenThrowException() {
    // given
    var appResponse = new OrderResponse(
        1L,
        100L,
        "PLACED",
        50000L,
        List.of(new OrderItemDto(1L, "Product A", 2, 25000L)),
        LocalDateTime.of(2025, 1, 1, 10, 0)
    );
    var apiResponse = OrderApiResponse.from(appResponse);

    // when & then
    assertThatThrownBy(() -> apiResponse.items().add(
        new OrderApiResponse.OrderItemResponse(2L, "Product B", 1, 30000L)
    )).isInstanceOf(UnsupportedOperationException.class);  // ✅ 불변
}

@Test
void givenMutableList_whenCreate_thenImmutableList() {
    // given
    List<OrderApiResponse.OrderItemResponse> mutableList = new ArrayList<>();
    mutableList.add(new OrderApiResponse.OrderItemResponse(1L, "Product A", 2, 25000L));

    // when
    var apiResponse = new OrderApiResponse(
        1L,
        100L,
        "PLACED",
        50000L,
        mutableList,
        LocalDateTime.now()
    );

    // then
    assertThat(apiResponse.items()).isUnmodifiable();
    assertThatThrownBy(() -> apiResponse.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
}
```

### 방어적 복사 검증 테스트

```java
@Test
void givenMutableList_whenCreate_thenDefensiveCopy() {
    // given
    List<OrderApiResponse.OrderItemResponse> mutableList = new ArrayList<>();
    mutableList.add(new OrderApiResponse.OrderItemResponse(1L, "Product A", 2, 25000L));

    // when
    var apiResponse = new OrderApiResponse(
        1L,
        100L,
        "PLACED",
        50000L,
        mutableList,
        LocalDateTime.now()
    );

    // 원본 리스트 수정
    mutableList.add(new OrderApiResponse.OrderItemResponse(2L, "Product B", 1, 30000L));

    // then
    assertThat(apiResponse.items()).hasSize(1);  // ✅ 방어적 복사로 원본 변경 영향 없음
}
```

---

## 4) Nested Record 변환 테스트

### Nested Record from() 메서드 테스트

```java
@Test
void givenOrderItemDto_whenFrom_thenConvertToNestedRecord() {
    // given
    var itemDto = new OrderItemDto(1L, "Product A", 2, 25000L);

    // when
    var itemResponse = OrderApiResponse.OrderItemResponse.from(itemDto);

    // then
    assertThat(itemResponse.productId()).isEqualTo(1L);
    assertThat(itemResponse.productName()).isEqualTo("Product A");
    assertThat(itemResponse.quantity()).isEqualTo(2);
    assertThat(itemResponse.price()).isEqualTo(25000L);
}

@Test
void givenMultipleOrderItems_whenFrom_thenConvertAll() {
    // given
    var itemDtos = List.of(
        new OrderItemDto(1L, "Product A", 2, 25000L),
        new OrderItemDto(2L, "Product B", 1, 30000L),
        new OrderItemDto(3L, "Product C", 3, 15000L)
    );

    // when
    var itemResponses = itemDtos.stream()
        .map(OrderApiResponse.OrderItemResponse::from)
        .toList();

    // then
    assertThat(itemResponses).hasSize(3);
    assertThat(itemResponses.get(0).productId()).isEqualTo(1L);
    assertThat(itemResponses.get(1).productId()).isEqualTo(2L);
    assertThat(itemResponses.get(2).productId()).isEqualTo(3L);
}
```

---

## 5) SliceApiResponse/PageApiResponse 생성 테스트

### SliceApiResponse from() 메서드 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class SliceApiResponseTest {

    @Test
    void givenSliceResponse_whenFrom_thenConvertSuccess() {
        // given
        var appSlice = new SliceResponse<>(
            List.of(
                new OrderSummaryDto(1L, "PLACED", 50000L),
                new OrderSummaryDto(2L, "CONFIRMED", 75000L)
            ),
            20,
            true,
            "eyJpZCI6Mn0="
        );

        // when
        var apiSlice = SliceApiResponse.from(appSlice);

        // then
        assertThat(apiSlice.content()).hasSize(2);
        assertThat(apiSlice.size()).isEqualTo(20);
        assertThat(apiSlice.hasNext()).isTrue();
        assertThat(apiSlice.nextCursor()).isEqualTo("eyJpZCI6Mn0=");
    }

    @Test
    void givenSliceResponseWithMapper_whenFrom_thenConvertWithMapping() {
        // given
        var appSlice = new SliceResponse<>(
            List.of(
                new OrderSummaryDto(1L, "PLACED", 50000L),
                new OrderSummaryDto(2L, "CONFIRMED", 75000L)
            ),
            20,
            true,
            "eyJpZCI6Mn0="
        );

        // when
        var apiSlice = SliceApiResponse.from(
            appSlice,
            OrderSummaryApiResponse::from
        );

        // then
        assertThat(apiSlice.content()).hasSize(2);
        assertThat(apiSlice.content().get(0).orderId()).isEqualTo(1L);
        assertThat(apiSlice.content().get(1).orderId()).isEqualTo(2L);
        assertThat(apiSlice.hasNext()).isTrue();
    }

    @Test
    void givenSliceResponseNoNext_whenFrom_thenHasNextFalse() {
        // given
        var appSlice = new SliceResponse<>(
            List.of(new OrderSummaryDto(1L, "PLACED", 50000L)),
            20,
            false,  // ✅ 다음 페이지 없음
            null
        );

        // when
        var apiSlice = SliceApiResponse.from(appSlice);

        // then
        assertThat(apiSlice.hasNext()).isFalse();
        assertThat(apiSlice.nextCursor()).isNull();
    }
}
```

### PageApiResponse from() 메서드 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class PageApiResponseTest {

    @Test
    void givenPageResponse_whenFrom_thenConvertSuccess() {
        // given
        var appPage = new PageResponse<>(
            List.of(
                new OrderDto(1L, "PLACED", 50000L),
                new OrderDto(2L, "CONFIRMED", 75000L)
            ),
            0,
            20,
            100L,
            5,
            true,
            false
        );

        // when
        var apiPage = PageApiResponse.from(appPage);

        // then
        assertThat(apiPage.content()).hasSize(2);
        assertThat(apiPage.page()).isEqualTo(0);
        assertThat(apiPage.size()).isEqualTo(20);
        assertThat(apiPage.totalElements()).isEqualTo(100L);
        assertThat(apiPage.totalPages()).isEqualTo(5);
        assertThat(apiPage.first()).isTrue();
        assertThat(apiPage.last()).isFalse();
    }

    @Test
    void givenPageResponseWithMapper_whenFrom_thenConvertWithMapping() {
        // given
        var appPage = new PageResponse<>(
            List.of(
                new OrderDto(1L, "PLACED", 50000L),
                new OrderDto(2L, "CONFIRMED", 75000L)
            ),
            0,
            20,
            100L,
            5,
            true,
            false
        );

        // when
        var apiPage = PageApiResponse.from(
            appPage,
            OrderApiResponse::from
        );

        // then
        assertThat(apiPage.content()).hasSize(2);
        assertThat(apiPage.content().get(0).orderId()).isEqualTo(1L);
        assertThat(apiPage.content().get(1).orderId()).isEqualTo(2L);
        assertThat(apiPage.totalElements()).isEqualTo(100L);
    }

    @Test
    void givenLastPage_whenFrom_thenLastTrue() {
        // given
        var appPage = new PageResponse<>(
            List.of(new OrderDto(1L, "PLACED", 50000L)),
            4,
            20,
            100L,
            5,
            false,
            true  // ✅ 마지막 페이지
        );

        // when
        var apiPage = PageApiResponse.from(appPage);

        // then
        assertThat(apiPage.last()).isTrue();
        assertThat(apiPage.first()).isFalse();
    }
}
```

---

## 6) 테스트 조직화

### 테스트 클래스 구조

```
test/
└─ adapter-in/rest-api/
   ├─ common/dto/
   │  ├─ SliceApiResponseTest.java           # Slice 테스트
   │  └─ PageApiResponseTest.java            # Page 테스트
   │
   └─ {bc}/dto/response/
      ├─ OrderApiResponseTest.java           # 단건 응답 테스트
      ├─ OrderSummaryApiResponseTest.java    # 목록 응답 테스트
      └─ OrderDetailApiResponseTest.java     # 상세 응답 테스트
```

### 테스트 네이밍 규칙

| 테스트 유형 | 접미사 | 예시 |
|-------------|--------|------|
| 기본 단위 테스트 | `*Test` | `OrderApiResponseTest` |
| from() 메서드 | `*Test` | `OrderApiResponseTest` |
| Slice/Page 생성 | `*Test` | `SliceApiResponseTest` |

---

## 7) 테스트 커버리지

### 필수 테스트 항목

- [ ] **from() 메서드**: Application Response → REST API Response 변환
- [ ] **Nested Record 변환**: 중첩 구조 from() 메서드
- [ ] **불변성 검증**: List.copyOf() 방어적 복사
- [ ] **컬렉션 수정 불가**: UnsupportedOperationException
- [ ] **null 필드 처리**: Optional 필드 null 허용
- [ ] **SliceApiResponse 생성**: from() 메서드 (단순 + Mapper)
- [ ] **PageApiResponse 생성**: from() 메서드 (단순 + Mapper)
- [ ] **hasNext 검증**: Slice 다음 페이지 여부
- [ ] **first/last 검증**: Page 첫/마지막 페이지 여부

---

## 8) 테스트 예시 (전체)

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class OrderApiResponseTest {

    @Nested
    @DisplayName("from() 메서드 변환 테스트")
    class FromMethodTest {

        @Test
        @DisplayName("Application Response를 REST API Response로 변환 성공")
        void givenApplicationResponse_whenFrom_thenConvertSuccess() {
            // given
            var appResponse = new OrderResponse(
                1L,
                100L,
                "PLACED",
                50000L,
                List.of(new OrderItemDto(1L, "Product A", 2, 25000L)),
                LocalDateTime.of(2025, 1, 1, 10, 0)
            );

            // when
            var apiResponse = OrderApiResponse.from(appResponse);

            // then
            assertThat(apiResponse.orderId()).isEqualTo(1L);
            assertThat(apiResponse.customerId()).isEqualTo(100L);
            assertThat(apiResponse.status()).isEqualTo("PLACED");
            assertThat(apiResponse.totalAmount()).isEqualTo(50000L);
            assertThat(apiResponse.items()).hasSize(1);
            assertThat(apiResponse.createdAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
        }

        @Test
        @DisplayName("null items는 빈 리스트로 변환")
        void givenNullItems_whenFrom_thenEmptyList() {
            var appResponse = new OrderResponse(1L, 100L, "PLACED", 50000L, null, LocalDateTime.now());
            var apiResponse = OrderApiResponse.from(appResponse);

            assertThat(apiResponse.items()).isEmpty();
        }

        @Test
        @DisplayName("Nested Record 변환 성공")
        void givenNestedItems_whenFrom_thenConvertNested() {
            var appResponse = new OrderResponse(
                1L,
                100L,
                "PLACED",
                50000L,
                List.of(
                    new OrderItemDto(1L, "Product A", 2, 25000L),
                    new OrderItemDto(2L, "Product B", 1, 30000L)
                ),
                LocalDateTime.now()
            );

            var apiResponse = OrderApiResponse.from(appResponse);

            assertThat(apiResponse.items()).hasSize(2);
            assertThat(apiResponse.items().get(0).productId()).isEqualTo(1L);
            assertThat(apiResponse.items().get(1).productId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("리스트 수정 시도 시 예외 발생")
        void givenApiResponse_whenModifyList_thenThrowException() {
            var appResponse = new OrderResponse(
                1L,
                100L,
                "PLACED",
                50000L,
                List.of(new OrderItemDto(1L, "Product A", 2, 25000L)),
                LocalDateTime.now()
            );
            var apiResponse = OrderApiResponse.from(appResponse);

            assertThatThrownBy(() -> apiResponse.items().add(
                new OrderApiResponse.OrderItemResponse(2L, "Product B", 1, 30000L)
            )).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("방어적 복사로 원본 리스트 변경 영향 없음")
        void givenMutableList_whenCreate_thenDefensiveCopy() {
            List<OrderApiResponse.OrderItemResponse> mutableList = new ArrayList<>();
            mutableList.add(new OrderApiResponse.OrderItemResponse(1L, "Product A", 2, 25000L));

            var apiResponse = new OrderApiResponse(
                1L,
                100L,
                "PLACED",
                50000L,
                mutableList,
                LocalDateTime.now()
            );

            mutableList.add(new OrderApiResponse.OrderItemResponse(2L, "Product B", 1, 30000L));

            assertThat(apiResponse.items()).hasSize(1);
        }
    }
}
```

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("dto")
class SliceApiResponseTest {

    @Nested
    @DisplayName("Slice 생성 테스트")
    class SliceCreationTest {

        @Test
        @DisplayName("SliceResponse를 SliceApiResponse로 변환 성공")
        void givenSliceResponse_whenFrom_thenConvertSuccess() {
            var appSlice = new SliceResponse<>(
                List.of(
                    new OrderSummaryDto(1L, "PLACED", 50000L),
                    new OrderSummaryDto(2L, "CONFIRMED", 75000L)
                ),
                20,
                true,
                "eyJpZCI6Mn0="
            );

            var apiSlice = SliceApiResponse.from(appSlice);

            assertThat(apiSlice.content()).hasSize(2);
            assertThat(apiSlice.size()).isEqualTo(20);
            assertThat(apiSlice.hasNext()).isTrue();
            assertThat(apiSlice.nextCursor()).isEqualTo("eyJpZCI6Mn0=");
        }

        @Test
        @DisplayName("Mapper 함수로 변환 성공")
        void givenSliceResponseWithMapper_whenFrom_thenConvertWithMapping() {
            var appSlice = new SliceResponse<>(
                List.of(new OrderSummaryDto(1L, "PLACED", 50000L)),
                20,
                false,
                null
            );

            var apiSlice = SliceApiResponse.from(
                appSlice,
                OrderSummaryApiResponse::from
            );

            assertThat(apiSlice.content()).hasSize(1);
            assertThat(apiSlice.content().get(0).orderId()).isEqualTo(1L);
            assertThat(apiSlice.hasNext()).isFalse();
        }
    }
}
```

---

## 9) 체크리스트

- [ ] from() 메서드 변환 테스트 작성
- [ ] Nested Record from() 메서드 테스트
- [ ] 불변성 검증 테스트 (List.copyOf())
- [ ] 컬렉션 수정 불가 테스트
- [ ] null 필드 처리 테스트
- [ ] SliceApiResponse 생성 테스트
- [ ] PageApiResponse 생성 테스트
- [ ] @Nested, @DisplayName 활용
- [ ] Given-When-Then 패턴
- [ ] @Tag 3개 필수 추가

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
