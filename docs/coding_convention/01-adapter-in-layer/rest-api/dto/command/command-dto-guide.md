# Command DTO Guide — **상태 변경 요청 DTO**

> Command DTO는 **HTTP 요청을 받아 상태 변경 작업**(POST, PUT, PATCH, DELETE)을 수행하는 DTO입니다.
>
> **API Layer → Application Layer 변환 전용**, Domain 변환 금지.

---

## 1) 핵심 원칙

* **Java 21 Record**: DTO는 `public record` 키워드로 정의
* **Bean Validation**: 모든 필드에 `@NotNull`, `@NotEmpty`, `@Valid` 등 검증 어노테이션
* **Compact Constructor**: 추가 검증 로직은 Compact Constructor에서
* **Immutable**: 불변 객체, Setter 금지
* **API 접미사**: `*ApiRequest` 네이밍 (예: `CreateOrderApiRequest`)
* **액션 명확**: Command 이름에 액션 포함 (Create, Update, Cancel 등)
* **Nested Record**: 복잡한 구조는 Nested Record로 표현
* **Lombok 금지**: `@Data`, `@Builder` 등 모든 Lombok 어노테이션 금지
* **Jackson 어노테이션 금지**: `@JsonFormat`, `@JsonProperty` 금지

### 금지사항

* **Lombok 전면 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter` 등
* **Jackson 어노테이션**: `@JsonFormat`, `@JsonProperty` 등
* **비즈니스 로직**: DTO에 비즈니스 검증 로직 포함 금지 (Domain에서)
* **Domain 직접 변환**: DTO → Domain 변환 금지 (Assembler 책임)

---

## 2) 네이밍 규칙

### 기본 원칙

**접미사**: `*ApiRequest`
**접두사**: **유비쿼터스 언어**로 표현된 **액션 동사** 또는 **비즈니스 행위**

### 네이밍 가이드

| HTTP 메서드 | 액션 동사 예시 | 구체 예시 |
|-------------|---------------|-----------|
| POST | Create, Register, Place, Enroll | `CreateOrderApiRequest`, `PlaceOrderApiRequest` |
| PUT/PATCH | Update, Modify, Change, Adjust | `UpdateOrderStatusApiRequest`, `ChangeShippingAddressApiRequest` |
| POST/PATCH | Cancel, Confirm, Approve, Reject, Complete | `CancelOrderApiRequest`, `ApproveOrderApiRequest` |

**핵심**:
- **유비쿼터스 언어** 사용: 도메인 전문가와 개발자가 공통으로 사용하는 용어
- **액션 명확**: 이름만으로 어떤 작업인지 즉시 이해 가능
- **일관성**: 같은 행위는 항상 같은 동사 사용 (Create vs Register 혼용 금지)

---

## 3) 기본 패턴

### 단순 Command (Flat Structure)

```java
/**
 * Order 상태 변경 요청
 *
 * @param orderId 주문 ID
 * @param status 변경할 상태
 * @author ryu-qqq
 * @since 2025-11-13
 */
public record UpdateOrderStatusApiRequest(
    @NotNull(message = "주문 ID는 필수입니다")
    Long orderId,

    @NotBlank(message = "상태는 필수입니다")
    @Pattern(regexp = "PLACED|CONFIRMED|SHIPPED|DELIVERED|CANCELLED", message = "유효하지 않은 상태입니다")
    String status
) {
    // ✅ Compact Constructor (추가 검증)
    public UpdateOrderStatusApiRequest {
        if (orderId != null && orderId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 주문 ID: " + orderId);
        }
    }
}
```

### 복잡한 Command (Nested Record)

```java
/**
 * Order 생성 요청
 *
 * @param customerId 고객 ID
 * @param items 주문 항목 목록
 * @param shippingAddress 배송 주소
 * @author ryu-qqq
 * @since 2025-11-13
 */
public record CreateOrderApiRequest(
    @NotNull(message = "고객 ID는 필수입니다")
    Long customerId,

    @NotEmpty(message = "주문 항목은 필수입니다")
    @Valid
    List<OrderItemRequest> items,

    @NotNull(message = "배송 주소는 필수입니다")
    @Valid
    AddressRequest shippingAddress
) {
    // ✅ Compact Constructor
    public CreateOrderApiRequest {
        if (customerId != null && customerId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 고객 ID: " + customerId);
        }
        // 불변 리스트로 방어적 복사
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Order 항목 요청
     */
    public record OrderItemRequest(
        @NotNull(message = "상품 ID는 필수입니다")
        Long productId,

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        Integer quantity,

        @NotNull(message = "가격은 필수입니다")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다")
        Long price
    ) {}

    /**
     * 배송 주소 요청
     */
    public record AddressRequest(
        @NotBlank(message = "우편번호는 필수입니다")
        String zipCode,

        @NotBlank(message = "주소는 필수입니다")
        String address,

        String addressDetail
    ) {}
}
```

---

## 4) Bean Validation 규칙

### 필수 검증

```java
public record CreateOrderApiRequest(
    @NotNull(message = "필수입니다")       // null 체크
    Long customerId,

    @NotBlank(message = "필수입니다")      // null, 공백 체크
    String customerName,

    @NotEmpty(message = "필수입니다")      // null, empty 체크
    List<OrderItemRequest> items
) {}
```

### 값 범위 검증

```java
public record CreateOrderApiRequest(
    @Min(value = 1, message = "1 이상")
    @Max(value = 100, message = "100 이하")
    Integer quantity,

    @Positive(message = "양수여야 함")
    Long price,

    @PositiveOrZero(message = "0 이상")
    Long discount
) {}
```

### 패턴 검증

```java
public record CreateOrderApiRequest(
    @Pattern(regexp = "^[A-Z]{2}[0-9]{10}$", message = "유효하지 않은 형식")
    String orderNumber,

    @Email(message = "유효하지 않은 이메일")
    String email,

    @Size(min = 10, max = 11, message = "10-11자리")
    String phoneNumber
) {}
```

### Nested Record 검증

```java
public record CreateOrderApiRequest(
    @NotNull @Valid              // ✅ @Valid 필수
    AddressRequest shippingAddress,

    @NotEmpty @Valid             // ✅ 컬렉션 내부도 검증
    List<OrderItemRequest> items
) {}
```

---

## 5) Compact Constructor 활용

### 추가 검증 로직

```java
public record CreateOrderApiRequest(
    Long customerId,
    List<OrderItemRequest> items
) {
    public CreateOrderApiRequest {
        // ✅ null 방어
        if (customerId != null && customerId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 고객 ID");
        }

        // ✅ 불변 리스트 변환
        items = items == null ? List.of() : List.copyOf(items);

        // ✅ 비즈니스 룰 검증 (간단한 것만)
        if (items.size() > 100) {
            throw new IllegalArgumentException("최대 100개까지 가능");
        }
    }
}
```

### 기본값 설정

```java
public record CreateOrderApiRequest(
    Long customerId,
    String memo
) {
    public CreateOrderApiRequest {
        // ✅ 기본값 설정
        memo = memo == null ? "" : memo.trim();
    }
}
```

---

## 6) Do / Don't

### ✅ Good

```java
// ✅ Good: Record, Bean Validation, Compact Constructor
public record CreateOrderApiRequest(
    @NotNull Long customerId,
    @NotEmpty @Valid List<OrderItemRequest> items
) {
    public CreateOrderApiRequest {
        items = List.copyOf(items);
    }

    public record OrderItemRequest(
        @NotNull Long productId,
        @Min(1) Integer quantity
    ) {}
}
```

### ❌ Bad

```java
// ❌ Bad: Lombok 사용
@Data
@Builder
public class CreateOrderApiRequest {
    private Long customerId;
    private List<OrderItemRequest> items;
}

// ❌ Bad: Domain 변환 메서드 포함
public record CreateOrderApiRequest(...) {
    public Order toDomain() {  // ❌ Mapper/Assembler 책임
        return Order.forNew(...);
    }
}

// ❌ Bad: Jackson 어노테이션
public record CreateOrderApiRequest(
    @JsonProperty("customer_id")  // ❌ 금지
    Long customerId
) {}

// ❌ Bad: 비즈니스 로직
public record CreateOrderApiRequest(...) {
    public boolean isVipOrder() {  // ❌ Domain 책임
        return totalAmount > 100000;
    }
}
```

---

## 7) 변환 흐름

```
[HTTP Request JSON]
    ↓
CreateOrderApiRequest (Command DTO)
    ↓
OrderApiMapper.toCommand()  ← @Component DI
    ↓
CreateOrderCommand (UseCase DTO)
    ↓
OrderAssembler.toDomain()  ← Application Layer
    ↓
Order (Domain Aggregate)
```

**중요**:
- **API DTO → UseCase DTO**: Mapper 책임 (REST API Layer)
- **UseCase DTO → Domain**: Assembler 책임 (Application Layer)
- Command DTO는 Domain 변환 로직 포함 금지

---

## 8) ArchUnit 검증

Command DTO의 아키텍처 규칙 자동 검증 (빌드 시 실행)은 별도 가이드를 참고하세요:

**👉 [Command DTO ArchUnit Guide](./command-dto-archunit.md)**

**주요 검증 규칙** (10개):
- ✅ Record 타입 필수
- ✅ *ApiRequest 네이밍 규칙
- ❌ Lombok 어노테이션 절대 금지
- ❌ Jackson 어노테이션 절대 금지
- ❌ Domain 변환 메서드 금지
- ❌ 비즈니스 로직 메서드 금지
- ✅ Bean Validation 어노테이션 사용 권장
- ✅ 올바른 패키지 위치
- ❌ Setter 메서드 절대 금지
- ❌ Spring 어노테이션 절대 금지

---

## 9) 체크리스트

- [ ] `public record` 키워드 사용
- [ ] `*ApiRequest` 네이밍 규칙 준수
- [ ] 액션 명확 (Create, Update, Cancel 등)
- [ ] Bean Validation 어노테이션 적용
- [ ] Compact Constructor (불변 리스트 변환, 추가 검증)
- [ ] Nested Record (복잡한 구조)
- [ ] Lombok 사용 금지
- [ ] Jackson 어노테이션 금지
- [ ] Domain 변환 메서드 금지
- [ ] Javadoc 작성
- [ ] **테스트 작성**: [Command DTO Test Guide](./command-dto-test-guide.md) 참고

---

**작성자**: Development Team  
**최종 수정일**: 2025-11-13  
**버전**: 1.0.0
