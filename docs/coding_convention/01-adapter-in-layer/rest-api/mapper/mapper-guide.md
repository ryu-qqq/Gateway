# ApiMapper — **API DTO ↔ Application DTO 변환 전용**

> ApiMapper는 **API DTO와 Application DTO 간 변환만** 담당하는 단순 변환기입니다.
>
> 비즈니스 로직 없이 **필드 매핑만** 수행합니다.

---

## 1) 핵심 역할

* **API Request → Command/Query**: HTTP 요청 DTO를 Application Layer Command/Query로 변환
* **Application Response → API Response**: Application Response를 HTTP 응답 DTO로 변환
* **단순 변환만**: 비즈니스 로직 포함 금지, 필드 매핑만
* **Bean 등록**: `@Component`로 등록 (Static 메서드 ❌)
* **계층 분리**: REST API Layer ↔ Application Layer 간 의존성 격리

---

## 2) 패키지 구조

```
adapter-in/rest-api/{bc}/mapper/
└── {Bc}ApiMapper.java
```

**예시**:
```
adapter-in/rest-api/order/mapper/
└── OrderApiMapper.java
```

---

## 3) 기본 구조

```java
package com.ryuqq.adapter.in.rest.{bc}.mapper;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * {Bc}ApiMapper - {Bc} REST API ↔ Application Layer 변환
 *
 * <p>REST API Layer와 Application Layer 간의 DTO 변환을 담당합니다.</p>
 *
 * <p><strong>변환 방향:</strong></p>
 * <ul>
 *   <li>API Request → Command/Query (Controller → Application)</li>
 *   <li>Application Response → API Response (Application → Controller)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class {Bc}ApiMapper {

    /**
     * API Request → Command
     */
    public {Action}{Bc}Command toCommand({Action}{Bc}ApiRequest request) {
        // 필드 매핑만
    }

    /**
     * API Request → Query
     */
    public {Action}{Bc}Query toQuery({Action}{Bc}ApiRequest request) {
        // 필드 매핑만
    }

    /**
     * Application Response → API Response
     */
    public {Bc}ApiResponse toApiResponse({Bc}Response appResponse) {
        // 필드 매핑만
    }

    /**
     * List 변환
     */
    public List<{Bc}ApiResponse> toApiResponseList(List<{Bc}Response> appResponses) {
        return appResponses.stream()
            .map(this::toApiResponse)
            .toList();
    }
}
```

---

## 4) API Request → Command 변환

### 기본 패턴

```java
/**
 * CreateOrderApiRequest → CreateOrderCommand 변환
 */
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    return CreateOrderCommand.of(
        request.customerId(),
        request.amount(),
        request.deliveryAddress()
    );
}
```

### Nested Record 변환

```java
/**
 * CreateOrderApiRequest → CreateOrderCommand 변환 (Nested)
 */
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    List<CreateOrderCommand.OrderItem> items = request.items().stream()
        .map(item -> CreateOrderCommand.OrderItem.of(
            item.productId(),
            item.quantity(),
            item.unitPrice()
        ))
        .toList();

    return CreateOrderCommand.of(
        request.customerId(),
        items,
        request.deliveryAddress()
    );
}
```

### Do / Don't

```java
// ✅ Good: 필드 매핑만
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    return CreateOrderCommand.of(
        request.customerId(),
        request.amount()
    );
}

// ❌ Bad: 기본값 설정 금지 (Controller 책임)
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    return CreateOrderCommand.of(
        request.customerId() != null ? request.customerId() : 0L,  // ← Controller에서!
        request.amount()
    );
}

// ❌ Bad: 검증 로직 금지 (Bean Validation 사용)
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    if (request.amount() < 0) {  // ← @Positive 사용!
        throw new IllegalArgumentException();
    }
    return CreateOrderCommand.of(...);
}

// ❌ Bad: 비즈니스 로직 금지 (UseCase 책임)
public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
    BigDecimal tax = request.amount().multiply(0.1);  // ← UseCase에서!
    return CreateOrderCommand.of(
        request.customerId(),
        request.amount().add(tax)
    );
}
```

---

## 5) API Request → Query 변환

### 기본 패턴

```java
/**
 * OrderSearchApiRequest → SearchOrdersQuery 변환
 */
public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
    return SearchOrdersQuery.of(
        request.customerId(),
        request.status(),
        request.startDate(),
        request.endDate()
    );
}
```

### ID 파라미터 변환

```java
/**
 * ID → GetOrderQuery 변환
 *
 * <p>PathVariable 또는 RequestParam의 ID를 Query로 변환합니다.</p>
 */
public GetOrderQuery toQuery(Long id) {
    return GetOrderQuery.of(id);
}
```

### 페이징 조건 변환 (Cursor vs Offset 자동 판단)

```java
/**
 * SearchApiRequest → SearchQuery 변환 (Cursor/Offset 자동 판단)
 *
 * <p>Cursor 유무에 따라 CURSOR 또는 OFFSET 페이징 타입을 자동 설정합니다.</p>
 */
public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
    // Cursor가 있으면 CURSOR 타입, 없으면 OFFSET 타입
    boolean isCursor = request.cursor() != null && !request.cursor().isBlank();

    if (isCursor) {
        return SearchOrdersQuery.ofCursor(
            request.customerId(),
            request.status(),
            request.cursor(),
            request.size(),
            request.sortBy(),
            request.sortDirection()
        );
    } else {
        return SearchOrdersQuery.ofOffset(
            request.customerId(),
            request.status(),
            request.page(),
            request.size(),
            request.sortBy(),
            request.sortDirection()
        );
    }
}
```

### Do / Don't

```java
// ✅ Good: 필드 매핑만
public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
    return SearchOrdersQuery.of(
        request.customerId(),
        request.status()
    );
}

// ❌ Bad: 정렬 기본값 설정 금지 (Controller 책임)
public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
    return SearchOrdersQuery.of(
        request.customerId(),
        request.status(),
        request.sortBy() != null ? request.sortBy() : "createdAt"  // ← Controller에서!
    );
}

// ❌ Bad: 날짜 범위 검증 금지 (UseCase 책임)
public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
    if (request.startDate().isAfter(request.endDate())) {  // ← UseCase에서!
        throw new IllegalArgumentException();
    }
    return SearchOrdersQuery.of(...);
}
```

---

## 6) Application Response → API Response 변환

### 기본 패턴

```java
/**
 * OrderResponse → OrderApiResponse 변환
 */
public OrderApiResponse toApiResponse(OrderResponse appResponse) {
    return OrderApiResponse.of(
        appResponse.id(),
        appResponse.customerId(),
        appResponse.totalAmount(),
        appResponse.status(),
        appResponse.orderedAt()
    );
}
```

### Nested Response 변환

```java
/**
 * OrderDetailResponse → OrderDetailApiResponse 변환
 */
public OrderDetailApiResponse toDetailApiResponse(OrderDetailResponse appResponse) {
    return OrderDetailApiResponse.of(
        appResponse.id(),
        toCustomerInfo(appResponse.customer()),
        toLineItems(appResponse.lineItems()),
        appResponse.totalAmount(),
        appResponse.status(),
        appResponse.orderedAt()
    );
}

private OrderDetailApiResponse.CustomerInfo toCustomerInfo(
    OrderDetailResponse.CustomerInfo appCustomer
) {
    return new OrderDetailApiResponse.CustomerInfo(
        appCustomer.id(),
        appCustomer.name(),
        appCustomer.email()
    );
}

private List<OrderDetailApiResponse.LineItem> toLineItems(
    List<OrderDetailResponse.LineItem> appItems
) {
    return appItems.stream()
        .map(item -> new OrderDetailApiResponse.LineItem(
            item.id(),
            item.productName(),
            item.quantity(),
            item.unitPrice()
        ))
        .toList();
}
```

### 페이징 Response 변환

```java
/**
 * PageResponse → PageApiResponse 변환
 *
 * <p>Application Layer의 페이지 응답을 REST API Layer의 페이지 응답으로 변환합니다.</p>
 */
public OrderPageApiResponse toPageApiResponse(PageResponse<OrderDetailResponse> appPageResponse) {
    return OrderPageApiResponse.from(appPageResponse);
}

/**
 * SliceResponse → SliceApiResponse 변환
 *
 * <p>Application Layer의 슬라이스 응답을 REST API Layer의 슬라이스 응답으로 변환합니다.</p>
 */
public OrderSliceApiResponse toSliceApiResponse(SliceResponse<OrderDetailResponse> appSliceResponse) {
    return OrderSliceApiResponse.from(appSliceResponse);
}
```

### Do / Don't

```java
// ✅ Good: 필드 매핑만
public OrderApiResponse toApiResponse(OrderResponse appResponse) {
    return OrderApiResponse.of(
        appResponse.id(),
        appResponse.totalAmount()
    );
}

// ❌ Bad: 상태 변환 로직 금지 (Application Layer 책임)
public OrderApiResponse toApiResponse(OrderResponse appResponse) {
    String statusLabel = switch (appResponse.status()) {  // ← Application에서!
        case "PLACED" -> "주문완료";
        case "CANCELLED" -> "취소됨";
        default -> "알 수 없음";
    };
    return OrderApiResponse.of(appResponse.id(), statusLabel);
}

// ❌ Bad: 계산 로직 금지 (Domain/UseCase 책임)
public OrderApiResponse toApiResponse(OrderResponse appResponse) {
    BigDecimal tax = appResponse.totalAmount().multiply(0.1);  // ← Domain에서!
    return OrderApiResponse.of(
        appResponse.id(),
        appResponse.totalAmount().add(tax)
    );
}

// ❌ Bad: 조건부 필드 설정 금지 (Application Layer 책임)
public OrderApiResponse toApiResponse(OrderResponse appResponse) {
    return OrderApiResponse.of(
        appResponse.id(),
        appResponse.status().equals("COMPLETED") ? appResponse.completedAt() : null  // ← Application에서!
    );
}
```

---

## 7) 실전 예시

### OrderApiMapper

```java
package com.ryuqq.adapter.in.rest.order.mapper;

import com.ryuqq.adapter.in.rest.order.dto.request.CreateOrderApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.request.OrderSearchApiRequest;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderDetailApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderPageApiResponse;
import com.ryuqq.adapter.in.rest.order.dto.response.OrderSliceApiResponse;
import com.ryuqq.application.common.dto.response.PageResponse;
import com.ryuqq.application.common.dto.response.SliceResponse;
import com.ryuqq.application.order.dto.command.CreateOrderCommand;
import com.ryuqq.application.order.dto.query.GetOrderQuery;
import com.ryuqq.application.order.dto.query.SearchOrdersQuery;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;
import com.ryuqq.application.order.dto.response.OrderResponse;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * OrderApiMapper - Order REST API ↔ Application Layer 변환
 *
 * <p>REST API Layer와 Application Layer 간의 DTO 변환을 담당합니다.</p>
 *
 * <p><strong>변환 방향:</strong></p>
 * <ul>
 *   <li>API Request → Command/Query (Controller → Application)</li>
 *   <li>Application Response → API Response (Application → Controller)</li>
 * </ul>
 *
 * <p><strong>CQRS 패턴 적용:</strong></p>
 * <ul>
 *   <li>Command: Create/Update/Delete 요청 변환</li>
 *   <li>Query: Get/Search 요청 변환</li>
 * </ul>
 *
 * <p><strong>의존성 역전 원칙 준수:</strong></p>
 * <ul>
 *   <li>REST API Layer → Application Layer 의존 (OK)</li>
 *   <li>Application Layer → REST API Layer 의존 (NG)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class OrderApiMapper {

    /**
     * CreateOrderApiRequest → CreateOrderCommand 변환
     *
     * @param request REST API 주문 생성 요청
     * @return Application Layer 주문 생성 명령
     */
    public CreateOrderCommand toCommand(CreateOrderApiRequest request) {
        List<CreateOrderCommand.OrderItem> items = request.items().stream()
            .map(item -> CreateOrderCommand.OrderItem.of(
                item.productId(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();

        return CreateOrderCommand.of(
            request.customerId(),
            items,
            request.deliveryAddress()
        );
    }

    /**
     * ID → GetOrderQuery 변환
     *
     * @param id 주문 ID
     * @return Application Layer 주문 조회 쿼리
     */
    public GetOrderQuery toQuery(Long id) {
        return GetOrderQuery.of(id);
    }

    /**
     * OrderSearchApiRequest → SearchOrdersQuery 변환
     *
     * <p>Cursor 유무에 따라 CURSOR 또는 OFFSET 페이징 타입을 자동 설정합니다.</p>
     *
     * @param request REST API 주문 검색 요청
     * @return Application Layer 주문 검색 쿼리
     */
    public SearchOrdersQuery toQuery(OrderSearchApiRequest request) {
        // Cursor가 있으면 CURSOR 타입, 없으면 OFFSET 타입
        boolean isCursor = request.cursor() != null && !request.cursor().isBlank();

        if (isCursor) {
            return SearchOrdersQuery.ofCursor(
                request.customerId(),
                request.status(),
                request.startDate(),
                request.endDate(),
                request.cursor(),
                request.size(),
                request.sortBy(),
                request.sortDirection()
            );
        } else {
            return SearchOrdersQuery.ofOffset(
                request.customerId(),
                request.status(),
                request.startDate(),
                request.endDate(),
                request.page(),
                request.size(),
                request.sortBy(),
                request.sortDirection()
            );
        }
    }

    /**
     * OrderResponse → OrderApiResponse 변환
     *
     * @param appResponse Application Layer 주문 응답
     * @return REST API 주문 응답
     */
    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        return OrderApiResponse.of(
            appResponse.id(),
            appResponse.customerId(),
            appResponse.totalAmount(),
            appResponse.status(),
            appResponse.orderedAt()
        );
    }

    /**
     * OrderDetailResponse → OrderDetailApiResponse 변환
     *
     * @param appResponse Application Layer 주문 상세 응답
     * @return REST API 주문 상세 응답
     */
    public OrderDetailApiResponse toDetailApiResponse(OrderDetailResponse appResponse) {
        return OrderDetailApiResponse.of(
            appResponse.id(),
            toCustomerInfo(appResponse.customer()),
            toLineItems(appResponse.lineItems()),
            appResponse.totalAmount(),
            appResponse.status(),
            appResponse.orderedAt()
        );
    }

    /**
     * PageResponse → OrderPageApiResponse 변환
     *
     * @param appPageResponse Application Layer 페이지 응답
     * @return REST API 페이지 응답
     */
    public OrderPageApiResponse toPageApiResponse(PageResponse<OrderDetailResponse> appPageResponse) {
        return OrderPageApiResponse.from(appPageResponse);
    }

    /**
     * SliceResponse → OrderSliceApiResponse 변환
     *
     * @param appSliceResponse Application Layer 슬라이스 응답
     * @return REST API 슬라이스 응답
     */
    public OrderSliceApiResponse toSliceApiResponse(SliceResponse<OrderDetailResponse> appSliceResponse) {
        return OrderSliceApiResponse.from(appSliceResponse);
    }

    // ========== Private Helper 메서드 ==========

    private OrderDetailApiResponse.CustomerInfo toCustomerInfo(
        OrderDetailResponse.CustomerInfo appCustomer
    ) {
        return new OrderDetailApiResponse.CustomerInfo(
            appCustomer.id(),
            appCustomer.name(),
            appCustomer.email()
        );
    }

    private List<OrderDetailApiResponse.LineItem> toLineItems(
        List<OrderDetailResponse.LineItem> appItems
    ) {
        return appItems.stream()
            .map(item -> new OrderDetailApiResponse.LineItem(
                item.id(),
                item.productName(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();
    }
}
```

---

## 8) Controller에서 사용 예시

### Command 처리

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderApiMapper mapper;  // ← @Component DI

    public OrderCommandController(
        CreateOrderUseCase createOrderUseCase,
        OrderApiMapper mapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderApiResponse>> createOrder(
        @Valid @RequestBody CreateOrderApiRequest request
    ) {
        // 1. API Request → Command 변환
        CreateOrderCommand command = mapper.toCommand(request);

        // 2. UseCase 실행 (Application Layer 응답)
        OrderResponse appResponse = createOrderUseCase.execute(command);

        // 3. Application Response → API Response 변환
        OrderApiResponse apiResponse = mapper.toApiResponse(appResponse);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(apiResponse));
    }
}
```

### Query 처리

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderQueryController {

    private final GetOrderUseCase getOrderUseCase;
    private final SearchOrdersUseCase searchOrdersUseCase;
    private final OrderApiMapper mapper;  // ← @Component DI

    public OrderQueryController(
        GetOrderUseCase getOrderUseCase,
        SearchOrdersUseCase searchOrdersUseCase,
        OrderApiMapper mapper
    ) {
        this.getOrderUseCase = getOrderUseCase;
        this.searchOrdersUseCase = searchOrdersUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailApiResponse>> getOrder(
        @PathVariable Long id
    ) {
        // 1. ID → Query 변환
        GetOrderQuery query = mapper.toQuery(id);

        // 2. UseCase 실행
        OrderDetailResponse appResponse = getOrderUseCase.execute(query);

        // 3. Application Response → API Response 변환
        OrderDetailApiResponse apiResponse = mapper.toDetailApiResponse(appResponse);

        return ResponseEntity.ok(ApiResponse.success(apiResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderSliceApiResponse>> searchOrders(
        @ModelAttribute @Valid OrderSearchApiRequest request
    ) {
        // 1. API Request → Query 변환
        SearchOrdersQuery query = mapper.toQuery(request);

        // 2. UseCase 실행
        SliceResponse<OrderDetailResponse> appSliceResponse = searchOrdersUseCase.execute(query);

        // 3. Application Response → API Response 변환
        OrderSliceApiResponse apiResponse = mapper.toSliceApiResponse(appSliceResponse);

        return ResponseEntity.ok(ApiResponse.success(apiResponse));
    }
}
```

---

## 9) 의존성 주입 패턴

### ✅ Good: @Component Bean 등록

```java
@Component
public class OrderApiMapper {

    // 필요 시 의존성 주입 가능 (예: MessageSource, ObjectMapper 등)
    private final MessageSource messageSource;

    public OrderApiMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public OrderApiResponse toApiResponse(OrderResponse appResponse) {
        // MessageSource 사용 가능
        String statusLabel = messageSource.getMessage(
            "order.status." + appResponse.status(),
            null,
            LocaleContextHolder.getLocale()
        );

        return OrderApiResponse.of(
            appResponse.id(),
            statusLabel
        );
    }
}
```

### ❌ Bad: Static 메서드

```java
// ❌ Static 메서드는 의존성 주입 불가능
public class OrderApiMapper {

    public static OrderApiResponse toApiResponse(OrderResponse appResponse) {
        // MessageSource 주입 불가능
        return OrderApiResponse.of(appResponse.id(), appResponse.status());
    }
}
```

---

## 11) 금지사항

* **비즈니스 로직**: 검증, 계산, 상태 설정 금지
* **Static 메서드**: 반드시 `@Component` Bean 등록
* **Domain 객체 의존**: Domain Entity/Aggregate 직접 사용 금지 (Application DTO만 사용)
* **기본값 설정**: Controller 책임
* **검증 로직**: Bean Validation 사용
* **Lombok**: Pure Java 사용
* **Port 의존성**: Repository/UseCase 주입 금지 (UseCase는 Controller가 주입)

---

## 12) 체크리스트

- [ ] `@Component` 어노테이션 적용
- [ ] 패키지: `adapter-in.rest-api.{bc}.mapper`
- [ ] 클래스명: `{Bc}ApiMapper` 접미사
- [ ] API Request → Command 변환 메서드
- [ ] API Request → Query 변환 메서드
- [ ] Application Response → API Response 변환 메서드
- [ ] 비즈니스 로직 포함하지 않음
- [ ] Static 메서드 사용하지 않음
- [ ] Lombok 사용하지 않음
- [ ] Domain 객체 직접 사용하지 않음
- [ ] 필요 시 의존성 주입 가능 (MessageSource 등)

---

---

## 📚 관련 문서

* [Mapper ArchUnit Guide](./mapper-archunit.md) - ArchUnit 자동 검증 가이드
* [Mapper Test Guide](./mapper-test-guide.md) - 단위 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
