# Mapper Test Guide — **Mapper 테스트 가이드**

> Mapper의 **단위 테스트** 가이드입니다.
>
> **외부 의존성 없이**, **순수 Java 단위 테스트**로 빠르게 검증합니다.

---

## 1) 테스트 원칙

* **순수 Java 단위 테스트**: Spring Context 로딩 불필요
* **빠른 실행**: 밀리초 단위 (외부 의존성 없음)
* **변환 검증**: API Request → Command/Query 변환 정확성
* **역변환 검증**: Application Response → API Response 변환 정확성
* **Nested 변환 검증**: 복잡한 구조 변환 테스트
* **@Tag 필수**: `@Tag("unit")`, `@Tag("adapter-rest")`, `@Tag("mapper")` 사용

---

## 2) 단위 테스트 (변환 검증)

### API Request → Command 변환 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("mapper")
class OrderApiMapperTest {

    private OrderApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderApiMapper();
    }

    @Test
    @DisplayName("CreateOrderApiRequest → CreateOrderCommand 변환 성공")
    void givenApiRequest_whenToCommand_thenSuccess() {
        // given
        var items = List.of(
            new CreateOrderApiRequest.OrderItem(1L, 2, 10000L),
            new CreateOrderApiRequest.OrderItem(2L, 1, 20000L)
        );
        var apiRequest = new CreateOrderApiRequest(
            100L,
            items,
            new CreateOrderApiRequest.Address("12345", "Seoul", null)
        );

        // when
        CreateOrderCommand command = mapper.toCommand(apiRequest);

        // then
        assertThat(command.customerId()).isEqualTo(100L);
        assertThat(command.items()).hasSize(2);
        assertThat(command.items().get(0).productId()).isEqualTo(1L);
        assertThat(command.items().get(0).quantity()).isEqualTo(2);
        assertThat(command.deliveryAddress().zipCode()).isEqualTo("12345");
    }
}
```

### API Request → Query 변환 테스트

```java
@Test
@DisplayName("OrderSearchApiRequest → SearchOrdersQuery 변환 성공 (Offset)")
void givenOffsetRequest_whenToQuery_thenOffsetQuery() {
    // given
    var apiRequest = new OrderSearchApiRequest(
        100L,
        "PLACED",
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31),
        null,  // cursor = null → Offset 페이징
        0,     // page
        20,    // size
        "createdAt",
        "DESC"
    );

    // when
    SearchOrdersQuery query = mapper.toQuery(apiRequest);

    // then
    assertThat(query.paginationType()).isEqualTo(PaginationType.OFFSET);
    assertThat(query.customerId()).isEqualTo(100L);
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
}

@Test
@DisplayName("OrderSearchApiRequest → SearchOrdersQuery 변환 성공 (Cursor)")
void givenCursorRequest_whenToQuery_thenCursorQuery() {
    // given
    var apiRequest = new OrderSearchApiRequest(
        100L,
        "PLACED",
        null,
        null,
        "cursor123",  // cursor 있음 → Cursor 페이징
        null,
        20,
        "createdAt",
        "DESC"
    );

    // when
    SearchOrdersQuery query = mapper.toQuery(apiRequest);

    // then
    assertThat(query.paginationType()).isEqualTo(PaginationType.CURSOR);
    assertThat(query.cursor()).isEqualTo("cursor123");
    assertThat(query.size()).isEqualTo(20);
}
```

### ID → Query 변환 테스트

```java
@Test
@DisplayName("ID → GetOrderQuery 변환 성공")
void givenId_whenToQuery_thenSuccess() {
    // given
    Long orderId = 123L;

    // when
    GetOrderQuery query = mapper.toQuery(orderId);

    // then
    assertThat(query.id()).isEqualTo(123L);
}
```

---

## 3) Application Response → API Response 변환 테스트

### 기본 Response 변환 테스트

```java
@Test
@DisplayName("OrderResponse → OrderApiResponse 변환 성공")
void givenAppResponse_whenToApiResponse_thenSuccess() {
    // given
    var appResponse = new OrderResponse(
        1L,
        100L,
        BigDecimal.valueOf(30000),
        "PLACED",
        LocalDateTime.of(2025, 1, 1, 10, 0, 0)
    );

    // when
    OrderApiResponse apiResponse = mapper.toApiResponse(appResponse);

    // then
    assertThat(apiResponse.id()).isEqualTo(1L);
    assertThat(apiResponse.customerId()).isEqualTo(100L);
    assertThat(apiResponse.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    assertThat(apiResponse.status()).isEqualTo("PLACED");
}
```

### Detail Response 변환 테스트

```java
@Test
@DisplayName("OrderDetailResponse → OrderDetailApiResponse 변환 성공")
void givenDetailAppResponse_whenToDetailApiResponse_thenSuccess() {
    // given
    var customer = new OrderDetailResponse.CustomerInfo(100L, "홍길동", "hong@example.com");
    var items = List.of(
        new OrderDetailResponse.LineItem(1L, "상품A", 2, BigDecimal.valueOf(10000)),
        new OrderDetailResponse.LineItem(2L, "상품B", 1, BigDecimal.valueOf(20000))
    );
    var appDetailResponse = new OrderDetailResponse(
        1L,
        customer,
        items,
        BigDecimal.valueOf(40000),
        "PLACED",
        LocalDateTime.of(2025, 1, 1, 10, 0, 0)
    );

    // when
    OrderDetailApiResponse apiResponse = mapper.toDetailApiResponse(appDetailResponse);

    // then
    assertThat(apiResponse.id()).isEqualTo(1L);
    assertThat(apiResponse.customer().name()).isEqualTo("홍길동");
    assertThat(apiResponse.lineItems()).hasSize(2);
    assertThat(apiResponse.lineItems().get(0).productName()).isEqualTo("상품A");
}
```

### Page/Slice Response 변환 테스트

```java
@Test
@DisplayName("PageResponse → OrderPageApiResponse 변환 성공")
void givenPageResponse_whenToPageApiResponse_thenSuccess() {
    // given
    var content = List.of(
        new OrderDetailResponse(1L, null, List.of(), BigDecimal.ZERO, "PLACED", null),
        new OrderDetailResponse(2L, null, List.of(), BigDecimal.ZERO, "PLACED", null)
    );
    var appPageResponse = new PageResponse<>(content, 0, 20, 100L);

    // when
    OrderPageApiResponse apiResponse = mapper.toPageApiResponse(appPageResponse);

    // then
    assertThat(apiResponse.content()).hasSize(2);
    assertThat(apiResponse.page()).isEqualTo(0);
    assertThat(apiResponse.size()).isEqualTo(20);
    assertThat(apiResponse.totalElements()).isEqualTo(100L);
}

@Test
@DisplayName("SliceResponse → OrderSliceApiResponse 변환 성공")
void givenSliceResponse_whenToSliceApiResponse_thenSuccess() {
    // given
    var content = List.of(
        new OrderDetailResponse(1L, null, List.of(), BigDecimal.ZERO, "PLACED", null),
        new OrderDetailResponse(2L, null, List.of(), BigDecimal.ZERO, "PLACED", null)
    );
    var appSliceResponse = new SliceResponse<>(content, "cursor123", true);

    // when
    OrderSliceApiResponse apiResponse = mapper.toSliceApiResponse(appSliceResponse);

    // then
    assertThat(apiResponse.content()).hasSize(2);
    assertThat(apiResponse.nextCursor()).isEqualTo("cursor123");
    assertThat(apiResponse.hasNext()).isTrue();
}
```

---

## 4) 의존성 주입 테스트 (MessageSource 등)

### MessageSource 주입 테스트

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("mapper")
class OrderApiMapperWithDependencyTest {

    private OrderApiMapper mapper;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        mapper = new OrderApiMapper(messageSource);
    }

    @Test
    @DisplayName("MessageSource를 사용한 상태 변환 테스트")
    void givenAppResponse_whenToApiResponseWithI18n_thenSuccess() {
        // given
        var appResponse = new OrderResponse(1L, 100L, BigDecimal.ZERO, "PLACED", null);

        when(messageSource.getMessage(
            eq("order.status.PLACED"),
            any(),
            any(Locale.class)
        )).thenReturn("주문완료");

        // when
        OrderApiResponse apiResponse = mapper.toApiResponse(appResponse);

        // then
        assertThat(apiResponse.statusLabel()).isEqualTo("주문완료");
        verify(messageSource).getMessage(eq("order.status.PLACED"), any(), any(Locale.class));
    }
}
```

---

## 5) Edge Case 테스트

### Null 처리 테스트

```java
@Test
@DisplayName("Null 필드가 있는 경우 처리")
void givenNullFields_whenToCommand_thenHandleGracefully() {
    // given
    var apiRequest = new CreateOrderApiRequest(
        100L,
        null,  // items = null
        null   // address = null
    );

    // when
    CreateOrderCommand command = mapper.toCommand(apiRequest);

    // then
    assertThat(command.customerId()).isEqualTo(100L);
    assertThat(command.items()).isEmpty();  // null → 빈 리스트
    assertThat(command.deliveryAddress()).isNull();
}
```

### 빈 컬렉션 처리 테스트

```java
@Test
@DisplayName("빈 컬렉션 변환")
void givenEmptyCollection_whenToCommand_thenEmptyList() {
    // given
    var apiRequest = new CreateOrderApiRequest(
        100L,
        List.of(),  // 빈 리스트
        null
    );

    // when
    CreateOrderCommand command = mapper.toCommand(apiRequest);

    // then
    assertThat(command.items()).isEmpty();
}
```

---

## 6) 테스트 조직화

### 테스트 클래스 구조

```
test/
└─ adapter-in/rest-api/
   └─ {bc}/mapper/
      ├─ OrderApiMapperTest.java                    # 기본 변환 테스트
      ├─ OrderApiMapperWithDependencyTest.java      # 의존성 주입 테스트
      └─ OrderApiMapperEdgeCaseTest.java            # Edge Case 테스트
```

### @Nested 활용 (권장)

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("mapper")
class OrderApiMapperTest {

    private OrderApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderApiMapper();
    }

    @Nested
    @DisplayName("API Request → Command 변환")
    class ToCommandTest {

        @Test
        @DisplayName("CreateOrderApiRequest → CreateOrderCommand 변환")
        void givenApiRequest_whenToCommand_thenSuccess() {
            // ...
        }

        @Test
        @DisplayName("Null items는 빈 리스트로 변환")
        void givenNullItems_whenToCommand_thenEmptyList() {
            // ...
        }
    }

    @Nested
    @DisplayName("API Request → Query 변환")
    class ToQueryTest {

        @Test
        @DisplayName("Offset 페이징 Query 변환")
        void givenOffsetRequest_whenToQuery_thenOffsetQuery() {
            // ...
        }

        @Test
        @DisplayName("Cursor 페이징 Query 변환")
        void givenCursorRequest_whenToQuery_thenCursorQuery() {
            // ...
        }
    }

    @Nested
    @DisplayName("Application Response → API Response 변환")
    class ToApiResponseTest {

        @Test
        @DisplayName("OrderResponse → OrderApiResponse 변환")
        void givenAppResponse_whenToApiResponse_thenSuccess() {
            // ...
        }

        @Test
        @DisplayName("OrderDetailResponse → OrderDetailApiResponse 변환")
        void givenDetailResponse_whenToDetailApiResponse_thenSuccess() {
            // ...
        }
    }
}
```

---

## 7) 테스트 커버리지

### 필수 테스트 항목

- [ ] **API Request → Command**: 모든 필드 정확히 매핑
- [ ] **API Request → Query**: Offset/Cursor 페이징 타입 자동 판단
- [ ] **ID → Query**: PathVariable 변환
- [ ] **Application Response → API Response**: 모든 필드 정확히 매핑
- [ ] **Nested 변환**: 복잡한 구조 변환
- [ ] **Page/Slice 변환**: 페이징 메타데이터 정확성
- [ ] **Null 처리**: null → 기본값 (빈 리스트 등)
- [ ] **의존성 주입**: MessageSource, ObjectMapper 등
- [ ] **Edge Case**: 빈 컬렉션, 극단적 값

---

## 8) 테스트 예시 (전체)

```java
@Tag("unit")
@Tag("adapter-rest")
@Tag("mapper")
class OrderApiMapperTest {

    private OrderApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderApiMapper();
    }

    @Nested
    @DisplayName("API Request → Command 변환")
    class ToCommandTest {

        @Test
        @DisplayName("CreateOrderApiRequest → CreateOrderCommand 변환 성공")
        void givenValidRequest_whenToCommand_thenSuccess() {
            // given
            var items = List.of(
                new CreateOrderApiRequest.OrderItem(1L, 2, 10000L)
            );
            var apiRequest = new CreateOrderApiRequest(
                100L,
                items,
                new CreateOrderApiRequest.Address("12345", "Seoul", null)
            );

            // when
            CreateOrderCommand command = mapper.toCommand(apiRequest);

            // then
            assertThat(command.customerId()).isEqualTo(100L);
            assertThat(command.items()).hasSize(1);
            assertThat(command.items().get(0).productId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Null items는 빈 리스트로 변환")
        void givenNullItems_whenToCommand_thenEmptyList() {
            // given
            var apiRequest = new CreateOrderApiRequest(100L, null, null);

            // when
            CreateOrderCommand command = mapper.toCommand(apiRequest);

            // then
            assertThat(command.items()).isEmpty();
        }
    }

    @Nested
    @DisplayName("API Request → Query 변환")
    class ToQueryTest {

        @Test
        @DisplayName("Offset 페이징 Query 변환")
        void givenOffsetRequest_whenToQuery_thenOffsetQuery() {
            // given
            var apiRequest = new OrderSearchApiRequest(
                100L, "PLACED", null, null,
                null,  // cursor = null
                0, 20, "createdAt", "DESC"
            );

            // when
            SearchOrdersQuery query = mapper.toQuery(apiRequest);

            // then
            assertThat(query.paginationType()).isEqualTo(PaginationType.OFFSET);
            assertThat(query.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("Cursor 페이징 Query 변환")
        void givenCursorRequest_whenToQuery_thenCursorQuery() {
            // given
            var apiRequest = new OrderSearchApiRequest(
                100L, "PLACED", null, null,
                "cursor123",  // cursor 있음
                null, 20, "createdAt", "DESC"
            );

            // when
            SearchOrdersQuery query = mapper.toQuery(apiRequest);

            // then
            assertThat(query.paginationType()).isEqualTo(PaginationType.CURSOR);
            assertThat(query.cursor()).isEqualTo("cursor123");
        }

        @Test
        @DisplayName("ID → GetOrderQuery 변환")
        void givenId_whenToQuery_thenSuccess() {
            // when
            GetOrderQuery query = mapper.toQuery(123L);

            // then
            assertThat(query.id()).isEqualTo(123L);
        }
    }

    @Nested
    @DisplayName("Application Response → API Response 변환")
    class ToApiResponseTest {

        @Test
        @DisplayName("OrderResponse → OrderApiResponse 변환")
        void givenAppResponse_whenToApiResponse_thenSuccess() {
            // given
            var appResponse = new OrderResponse(
                1L, 100L, BigDecimal.valueOf(30000), "PLACED",
                LocalDateTime.of(2025, 1, 1, 10, 0, 0)
            );

            // when
            OrderApiResponse apiResponse = mapper.toApiResponse(appResponse);

            // then
            assertThat(apiResponse.id()).isEqualTo(1L);
            assertThat(apiResponse.totalAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(30000));
        }

        @Test
        @DisplayName("PageResponse → OrderPageApiResponse 변환")
        void givenPageResponse_whenToPageApiResponse_thenSuccess() {
            // given
            var content = List.of(
                new OrderDetailResponse(1L, null, List.of(), BigDecimal.ZERO, "PLACED", null)
            );
            var appPageResponse = new PageResponse<>(content, 0, 20, 100L);

            // when
            OrderPageApiResponse apiResponse = mapper.toPageApiResponse(appPageResponse);

            // then
            assertThat(apiResponse.content()).hasSize(1);
            assertThat(apiResponse.totalElements()).isEqualTo(100L);
        }
    }
}
```

---

## 9) 체크리스트

- [ ] 모든 변환 메서드 테스트 작성
- [ ] Nested 변환 테스트
- [ ] Null/Empty 처리 테스트
- [ ] 의존성 주입 테스트 (필요 시)
- [ ] @Nested, @DisplayName 활용
- [ ] Given-When-Then 패턴
- [ ] @Tag 어노테이션 적용

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
