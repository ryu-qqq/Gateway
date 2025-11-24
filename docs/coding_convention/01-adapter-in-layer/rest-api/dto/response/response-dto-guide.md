# Response DTO Guide — **HTTP 응답 DTO**

> Response DTO는 **HTTP 응답 Body**를 구성하는 DTO입니다.
>
> **UseCase 실행 결과**를 HTTP 클라이언트에게 반환하는 전용 DTO입니다.

---

## 1) 핵심 원칙

* **Java 21 Record**: DTO는 `public record` 키워드로 정의
* **Immutable**: 불변 객체, Setter 금지
* **API 접미사**: `*ApiResponse` 네이밍 (예: `OrderApiResponse`)
* **from() 메서드**: Application Layer Response → REST API Response 변환
* **Nested Record**: 복잡한 구조는 Nested Record로 표현
* **Lombok 금지**: `@Data`, `@Builder` 등 모든 Lombok 어노테이션 금지
* **Jackson 어노테이션 금지**: `@JsonFormat`, `@JsonProperty` 금지
* **페이징 전용 DTO**: `SliceApiResponse` (Cursor), `PageApiResponse` (Offset)
* **표준 래퍼**: `ApiResponse<T>` 사용 (success, data, error, timestamp)

### 금지사항

* **Lombok 전면 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter` 등
* **Jackson 어노테이션**: `@JsonFormat`, `@JsonProperty` 등
* **Spring Page 직접 사용**: `Page<T>` 반환 금지 → `PageApiResponse<T>` 사용
* **비즈니스 로직**: DTO에 계산 로직, 검증 로직 포함 금지
* **Domain 직접 노출**: Domain Entity를 Response DTO로 직접 반환 금지

---

## 2) 네이밍 규칙

### 기본 원칙

**접미사**: `*ApiResponse`

### 네이밍 가이드

| 응답 유형 | 네이밍 예시 | 용도 |
|-----------|-------------|------|
| 단건 조회 | `OrderApiResponse` | Order 단건 응답 |
| 목록 조회 (항목) | `OrderSummaryApiResponse` | Order 목록 항목 (요약 정보) |
| 생성 결과 | `OrderCreatedApiResponse` | Order 생성 후 결과 |
| 통계/집계 | `OrderStatsApiResponse` | Order 통계 정보 |

**핵심**:
- 응답의 목적이 명확히 드러나야 함
- 도메인 엔티티와 1:1 매핑 불필요 (필요한 정보만 선택적으로 포함)

---

## 3) 기본 패턴

### 단건 응답

```java
/**
 * Order 응답
 *
 * @param orderId 주문 ID
 * @param customerId 고객 ID
 * @param status 주문 상태
 * @param totalAmount 총 금액
 * @param items 주문 항목 목록
 * @param createdAt 생성 일시
 * @author ryu-qqq
 * @since 2025-11-13
 */
public record OrderApiResponse(
    Long orderId,
    Long customerId,
    String status,
    Long totalAmount,
    List<OrderItemResponse> items,
    LocalDateTime createdAt
) {
    // ✅ Compact Constructor (불변 컬렉션)
    public OrderApiResponse {
        items = List.copyOf(items);
    }

    /**
     * Application Layer Response → REST API Response
     */
    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            appResponse.customerId(),
            appResponse.status(),
            appResponse.totalAmount(),
            appResponse.items().stream()
                .map(OrderItemResponse::from)
                .toList(),
            appResponse.createdAt()
        );
    }

    /**
     * 주문 항목 응답
     */
    public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        Long price
    ) {
        public static OrderItemResponse from(OrderItemDto dto) {
            return new OrderItemResponse(
                dto.productId(),
                dto.productName(),
                dto.quantity(),
                dto.price()
            );
        }
    }
}
```

### 목록 응답 (Slice - Cursor 기반)

```java
/**
 * Controller
 */
@GetMapping("/orders")
public ApiResponse<SliceApiResponse<OrderSummaryApiResponse>> getOrders(
    @Valid OrderSearchApiRequest request
) {
    // 1. UseCase 호출
    SliceResponse<OrderSummaryDto> appSlice = orderQueryUseCase.searchOrders(
        orderApiMapper.toQuery(request)
    );

    // 2. Application Response → REST API Response 변환
    SliceApiResponse<OrderSummaryApiResponse> apiSlice = SliceApiResponse.from(
        appSlice,
        OrderSummaryApiResponse::from  // ← 매퍼 함수
    );

    // 3. ApiResponse 래핑
    return ApiResponse.ofSuccess(apiSlice);
}

/**
 * Order 요약 응답 (목록용)
 */
public record OrderSummaryApiResponse(
    Long orderId,
    Long customerId,
    String status,
    Long totalAmount,
    LocalDateTime createdAt
) {
    public static OrderSummaryApiResponse from(OrderSummaryDto dto) {
        return new OrderSummaryApiResponse(
            dto.orderId(),
            dto.customerId(),
            dto.status(),
            dto.totalAmount(),
            dto.createdAt()
        );
    }
}
```

**SliceApiResponse 응답 형식**:
```json
{
  "success": true,
  "data": {
    "content": [
      { "orderId": 1, "customerId": 100, ... },
      { "orderId": 2, "customerId": 101, ... }
    ],
    "size": 20,
    "hasNext": true,
    "nextCursor": "xyz123"
  },
  "error": null,
  "timestamp": "2025-11-13T10:30:00"
}
```

### 목록 응답 (Page - Offset 기반)

```java
/**
 * Controller (관리자용)
 */
@GetMapping("/admin/orders")
public ApiResponse<PageApiResponse<OrderApiResponse>> getOrdersForAdmin(
    @Valid OrderSearchApiRequest request
) {
    // 1. UseCase 호출
    PageResponse<OrderDto> appPage = orderQueryUseCase.searchOrdersForAdmin(
        orderApiMapper.toQuery(request)
    );

    // 2. Application Response → REST API Response 변환
    PageApiResponse<OrderApiResponse> apiPage = PageApiResponse.from(
        appPage,
        OrderApiResponse::from  // ← 매퍼 함수
    );

    // 3. ApiResponse 래핑
    return ApiResponse.ofSuccess(apiPage);
}
```

**PageApiResponse 응답 형식**:
```json
{
  "success": true,
  "data": {
    "content": [
      { "orderId": 1, "customerId": 100, ... },
      { "orderId": 2, "customerId": 101, ... }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "error": null,
  "timestamp": "2025-11-13T10:30:00"
}
```

---

## 4) 페이징 패턴 비교

### Slice vs Page

| 구분 | SliceApiResponse (Cursor) | PageApiResponse (Offset) |
|------|---------------------------|--------------------------|
| **사용 사례** | 무한 스크롤 (일반 사용자) | 페이지 번호 (관리자) |
| **성능** | 빠름 (COUNT 불필요) | 느림 (COUNT 필수) |
| **제공 정보** | hasNext, nextCursor | totalElements, totalPages |
| **적합한 UI** | 모바일, SNS 피드 | 관리자 테이블 |

### 선택 가이드

```java
// ✅ Slice 사용 (일반 사용자)
@GetMapping("/products")
public ApiResponse<SliceApiResponse<ProductApiResponse>> getProducts() {
    // 무한 스크롤, 고성능
}

// ✅ Page 사용 (관리자)
@GetMapping("/admin/products")
public ApiResponse<PageApiResponse<ProductApiResponse>> getProductsForAdmin() {
    // 페이지 번호, 전체 개수 필요
}
```

---

## 5) Nested Record 패턴

### 복잡한 응답 구조

```java
/**
 * Order 상세 응답 (복잡한 구조)
 */
public record OrderDetailApiResponse(
    Long orderId,
    CustomerInfo customer,
    List<OrderItemInfo> items,
    ShippingInfo shipping,
    PaymentInfo payment,
    LocalDateTime createdAt
) {
    public OrderDetailApiResponse {
        items = List.copyOf(items);
    }

    /**
     * 고객 정보
     */
    public record CustomerInfo(
        Long customerId,
        String name,
        String email
    ) {}

    /**
     * 주문 항목 정보
     */
    public record OrderItemInfo(
        Long productId,
        String productName,
        Integer quantity,
        Long price
    ) {}

    /**
     * 배송 정보
     */
    public record ShippingInfo(
        String address,
        String zipCode,
        String trackingNumber
    ) {}

    /**
     * 결제 정보
     */
    public record PaymentInfo(
        String method,
        Long amount,
        String status
    ) {}

    public static OrderDetailApiResponse from(OrderDetailDto dto) {
        return new OrderDetailApiResponse(
            dto.orderId(),
            new CustomerInfo(
                dto.customer().customerId(),
                dto.customer().name(),
                dto.customer().email()
            ),
            dto.items().stream()
                .map(item -> new OrderItemInfo(
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    item.price()
                ))
                .toList(),
            new ShippingInfo(
                dto.shipping().address(),
                dto.shipping().zipCode(),
                dto.shipping().trackingNumber()
            ),
            new PaymentInfo(
                dto.payment().method(),
                dto.payment().amount(),
                dto.payment().status()
            ),
            dto.createdAt()
        );
    }
}
```

---

## 6) from() 메서드 패턴

### 단순 변환

```java
public record OrderApiResponse(...) {
    /**
     * Application Layer Response → REST API Response
     */
    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            appResponse.customerId(),
            // ... 필드 매핑
        );
    }
}
```

### Nested Record 변환

```java
public record OrderApiResponse(...) {
    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            // ✅ Nested Record 변환
            appResponse.items().stream()
                .map(OrderItemResponse::from)
                .toList(),
            appResponse.createdAt()
        );
    }
}
```

### 조건부 변환

```java
public record OrderApiResponse(
    Long orderId,
    String status,
    String cancelReason  // ✅ CANCELLED 상태일 때만 값 있음
) {
    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            appResponse.status(),
            // ✅ 조건부 매핑
            "CANCELLED".equals(appResponse.status())
                ? appResponse.cancelReason()
                : null
        );
    }
}
```

---

## 7) ApiResponse 래퍼 사용

### 성공 응답

```java
// 단건
@GetMapping("/orders/{id}")
public ApiResponse<OrderApiResponse> getOrder(@PathVariable Long id) {
    OrderResponse appResponse = orderQueryUseCase.getOrder(new OrderId(id));
    OrderApiResponse apiResponse = OrderApiResponse.from(appResponse);
    return ApiResponse.ofSuccess(apiResponse);
}

// 목록 (Slice)
@GetMapping("/orders")
public ApiResponse<SliceApiResponse<OrderSummaryApiResponse>> getOrders(...) {
    SliceResponse<OrderSummaryDto> appSlice = orderQueryUseCase.searchOrders(...);
    SliceApiResponse<OrderSummaryApiResponse> apiSlice = SliceApiResponse.from(
        appSlice,
        OrderSummaryApiResponse::from
    );
    return ApiResponse.ofSuccess(apiSlice);
}

// 목록 (Page)
@GetMapping("/admin/orders")
public ApiResponse<PageApiResponse<OrderApiResponse>> getOrdersForAdmin(...) {
    PageResponse<OrderDto> appPage = orderQueryUseCase.searchOrdersForAdmin(...);
    PageApiResponse<OrderApiResponse> apiPage = PageApiResponse.from(
        appPage,
        OrderApiResponse::from
    );
    return ApiResponse.ofSuccess(apiPage);
}

// 생성 (데이터 없음)
@PostMapping("/orders")
public ResponseEntity<ApiResponse<Void>> createOrder(...) {
    orderCommandUseCase.createOrder(...);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.ofSuccess());
}
```

### 에러 응답

```java
@ExceptionHandler(OrderNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
    ErrorInfo error = new ErrorInfo("ORDER_NOT_FOUND", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.ofFailure(error));
}
```

---

## 8) Do / Don't

### ✅ Good

```java
// ✅ Good: Record, from() 메서드, Nested Record
public record OrderApiResponse(
    Long orderId,
    Long customerId,
    List<OrderItemResponse> items
) {
    public OrderApiResponse {
        items = List.copyOf(items);
    }

    public static OrderApiResponse from(OrderResponse appResponse) {
        return new OrderApiResponse(
            appResponse.orderId(),
            appResponse.customerId(),
            appResponse.items().stream()
                .map(OrderItemResponse::from)
                .toList()
        );
    }

    public record OrderItemResponse(
        Long productId,
        Integer quantity
    ) {
        public static OrderItemResponse from(OrderItemDto dto) {
            return new OrderItemResponse(dto.productId(), dto.quantity());
        }
    }
}

// ✅ Good: Slice 사용 (일반 사용자)
@GetMapping("/orders")
public ApiResponse<SliceApiResponse<OrderSummaryApiResponse>> getOrders(...) {
    SliceResponse<OrderSummaryDto> appSlice = orderQueryUseCase.searchOrders(...);
    return ApiResponse.ofSuccess(
        SliceApiResponse.from(appSlice, OrderSummaryApiResponse::from)
    );
}

// ✅ Good: Page 사용 (관리자)
@GetMapping("/admin/orders")
public ApiResponse<PageApiResponse<OrderApiResponse>> getOrdersForAdmin(...) {
    PageResponse<OrderDto> appPage = orderQueryUseCase.searchOrdersForAdmin(...);
    return ApiResponse.ofSuccess(
        PageApiResponse.from(appPage, OrderApiResponse::from)
    );
}
```

### ❌ Bad

```java
// ❌ Bad: Lombok 사용
@Data
@Builder
public class OrderApiResponse {
    private Long orderId;
    private Long customerId;
}

// ❌ Bad: Spring Page 직접 사용
@GetMapping("/orders")
public Page<OrderApiResponse> getOrders(...) {  // ❌ 금지
    return orderQueryUseCase.searchOrders(...);
}

// ❌ Bad: Domain Entity 직접 노출
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {  // ❌ 금지
    return orderQueryUseCase.getOrder(new OrderId(id));
}

// ❌ Bad: Jackson 어노테이션
public record OrderApiResponse(
    @JsonProperty("order_id")  // ❌ 금지
    Long orderId,

    @JsonFormat(pattern = "yyyy-MM-dd")  // ❌ 금지
    LocalDate createdAt
) {}

// ❌ Bad: 비즈니스 로직
public record OrderApiResponse(...) {
    public boolean isExpensive() {  // ❌ Controller/UseCase 책임
        return totalAmount > 100000;
    }
}
```

---

## 9) 변환 흐름

```
[UseCase 실행 결과]
    ↓
OrderResponse (Application Layer DTO)
    ↓
OrderApiResponse.from()  ← Response DTO 변환
    ↓
OrderApiResponse (REST API Layer DTO)
    ↓
ApiResponse.ofSuccess()  ← 표준 래퍼
    ↓
ApiResponse<OrderApiResponse> (HTTP Response Body)
```

**중요**:
- **Application Layer → REST API Layer**: from() 정적 팩토리 메서드
- Response DTO는 Application Layer DTO와 독립적
- 필요한 필드만 선택적으로 포함 (정보 은닉)

---

## 10) 테스트

Response DTO의 단위 테스트 가이드는 별도 문서를 참고하세요:

**👉 [Response DTO Test Guide](./response-dto-test-guide.md)**

**주요 테스트 항목**:
- ✅ from() 메서드 변환 테스트
- ✅ Nested Record 변환 테스트
- ✅ 불변 컬렉션 테스트
- ✅ null 필드 처리 테스트
- ✅ @Tag("unit"), @Tag("adapter-rest"), @Tag("dto") 필수

---

## 11) ArchUnit 검증

Response DTO의 아키텍처 규칙 자동 검증 (빌드 시 실행)은 별도 가이드를 참고하세요:

**👉 [Response DTO ArchUnit Guide](./response-dto-archunit.md)**

**주요 검증 규칙** (10개):
- ✅ Record 타입 필수
- ✅ *ApiResponse 네이밍 규칙
- ❌ Lombok 어노테이션 절대 금지
- ❌ Jackson 어노테이션 절대 금지
- ❌ 비즈니스 로직 메서드 금지
- ✅ from() 정적 팩토리 메서드 권장
- ✅ 올바른 패키지 위치
- ❌ Setter 메서드 절대 금지
- ❌ Spring 어노테이션 절대 금지
- ❌ Spring Page 직접 사용 금지

---

## 12) 체크리스트

- [ ] `public record` 키워드 사용
- [ ] `*ApiResponse` 네이밍 규칙 준수
- [ ] from() 정적 팩토리 메서드 구현
- [ ] 불변 컬렉션 (List.copyOf())
- [ ] Nested Record (복잡한 구조)
- [ ] Lombok 사용 금지
- [ ] Jackson 어노테이션 금지
- [ ] Spring Page 직접 사용 금지
- [ ] SliceApiResponse / PageApiResponse 사용
- [ ] ApiResponse 래퍼 사용
- [ ] Javadoc 작성
- [ ] **테스트 작성**: [Response DTO Test Guide](./response-dto-test-guide.md) 참고

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
