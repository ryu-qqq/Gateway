# Query DTO Guide — **조회 조건 요청 DTO**

> Query DTO는 **HTTP GET 요청의 조회 조건**을 담는 DTO입니다.
>
> **검색 필터, 페이징, 정렬 조건** 등을 표현하며, Application Layer의 Query DTO로 변환됩니다.

---

## 1) 핵심 원칙

* **Java 21 Record**: DTO는 `public record` 키워드로 정의
* **Optional 필드**: 조회 조건은 대부분 Optional (null 허용)
* **Bean Validation**: 필수 필드에만 검증 어노테이션
* **Immutable**: 불변 객체, Setter 금지
* **API 접미사**: `*ApiRequest` 네이밍 (예: `OrderSearchApiRequest`)
* **Search/Get 명확**: 조회 의도 명확히 표현
* **Nested Record**: 복잡한 조건은 Nested Record로 표현
* **Lombok 금지**: `@Data`, `@Builder` 등 모든 Lombok 어노테이션 금지
* **Jackson 어노테이션 금지**: `@JsonFormat`, `@JsonProperty` 금지

### 금지사항

* **Lombok 전면 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter` 등
* **Jackson 어노테이션**: `@JsonFormat`, `@JsonProperty` 등
* **비즈니스 로직**: DTO에 비즈니스 검증 로직 포함 금지
* **Domain 직접 변환**: DTO → Domain 변환 금지 (Assembler 책임)

---

## 2) 네이밍 규칙

| 패턴 | 접미사 | 예시 | 용도 |
|------|--------|------|------|
| 검색 | `*SearchApiRequest` | `OrderSearchApiRequest` | 복합 조건 검색 |
| 목록 | `*ListApiRequest` | `OrderListApiRequest` | 목록 조회 (필터) |
| 상세 | `*DetailApiRequest` | `OrderDetailApiRequest` | 단건 조회 (조건 있을 때) |
| 통계 | `*StatsApiRequest` | `OrderStatsApiRequest` | 집계/통계 조회 |

**핵심**: 조회 의도(Search, List, Detail 등)가 명확히 드러나야 함.

---

## 3) 기본 패턴

### 단순 검색 (Flat Structure)

```java
/**
 * Order 검색 요청
 *
 * @param customerId 고객 ID (Optional)
 * @param status 주문 상태 (Optional)
 * @param startDate 시작 날짜 (Optional)
 * @param endDate 종료 날짜 (Optional)
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 * @author ryu-qqq
 * @since 2025-11-13
 */
public record OrderSearchApiRequest(
    // ✅ Optional 필드 (null 허용)
    Long customerId,
    String status,
    LocalDate startDate,
    LocalDate endDate,

    // ✅ 페이징 필수 (기본값 제공)
    @Min(value = 0, message = "페이지 번호는 0 이상")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상")
    @Max(value = 100, message = "페이지 크기는 100 이하")
    Integer size
) {
    // ✅ Compact Constructor (기본값 설정)
    public OrderSearchApiRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;

        // 날짜 범위 검증
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다");
        }
    }
}
```

### 복잡한 검색 (Nested Record)

```java
/**
 * Order 고급 검색 요청
 *
 * @param filter 검색 필터
 * @param sort 정렬 조건
 * @param page 페이징 조건
 * @author ryu-qqq
 * @since 2025-11-13
 */
public record OrderSearchApiRequest(
    FilterRequest filter,
    SortRequest sort,

    @NotNull(message = "페이징 조건은 필수입니다")
    @Valid
    PageRequest page
) {
    public OrderSearchApiRequest {
        filter = filter == null ? new FilterRequest(null, null, null, null) : filter;
        sort = sort == null ? new SortRequest("createdAt", "DESC") : sort;
    }

    /**
     * 검색 필터
     */
    public record FilterRequest(
        Long customerId,
        String status,
        LocalDate startDate,
        LocalDate endDate
    ) {}

    /**
     * 정렬 조건
     */
    public record SortRequest(
        @NotBlank(message = "정렬 필드는 필수입니다")
        String field,

        @Pattern(regexp = "ASC|DESC", message = "정렬 방향은 ASC 또는 DESC")
        String direction
    ) {
        public SortRequest {
            field = field == null ? "createdAt" : field;
            direction = direction == null ? "DESC" : direction.toUpperCase();
        }
    }

    /**
     * 페이징 조건
     */
    public record PageRequest(
        @Min(value = 0, message = "페이지 번호는 0 이상")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상")
        @Max(value = 100, message = "페이지 크기는 100 이하")
        Integer size
    ) {
        public PageRequest {
            page = page == null ? 0 : page;
            size = size == null ? 20 : size;
        }
    }
}
```

---

## 4) 페이징 패턴

### 기본 페이징

```java
public record OrderListApiRequest(
    Long customerId,

    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size
) {
    public OrderListApiRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
```

### Cursor 기반 페이징

```java
public record OrderListApiRequest(
    Long customerId,
    String cursor,  // ✅ 다음 페이지 커서 (Optional)

    @Min(1) @Max(100) Integer size
) {
    public OrderListApiRequest {
        size = size == null ? 20 : size;
    }
}
```

---

## 5) 날짜/시간 조건 패턴

### 기간 조회

```java
public record OrderSearchApiRequest(
    LocalDate startDate,  // ✅ Optional
    LocalDate endDate     // ✅ Optional
) {
    public OrderSearchApiRequest {
        // 날짜 범위 검증
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다");
        }
    }
}
```

### 시간 포함

```java
public record OrderSearchApiRequest(
    LocalDateTime startDateTime,  // ✅ Optional
    LocalDateTime endDateTime     // ✅ Optional
) {
    public OrderSearchApiRequest {
        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다");
        }
    }
}
```

---

## 6) 검색 필터 패턴

### Enum 필터

```java
public record OrderSearchApiRequest(
    @Pattern(regexp = "PLACED|CONFIRMED|SHIPPED|DELIVERED|CANCELLED")
    String status  // ✅ Optional, Enum String
) {}
```

### 범위 필터

```java
public record OrderSearchApiRequest(
    @Min(0) Long minPrice,  // ✅ Optional
    @Min(0) Long maxPrice   // ✅ Optional
) {
    public OrderSearchApiRequest {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 작아야 합니다");
        }
    }
}
```

### 텍스트 검색

```java
public record OrderSearchApiRequest(
    String keyword,  // ✅ Optional, 부분 검색

    @Pattern(regexp = "ORDER_NUMBER|CUSTOMER_NAME|PRODUCT_NAME")
    String searchField  // ✅ 검색 대상 필드
) {
    public OrderSearchApiRequest {
        keyword = keyword == null ? null : keyword.trim();
        searchField = searchField == null ? "ORDER_NUMBER" : searchField;
    }
}
```

---

## 7) Do / Don't

### ✅ Good

```java
// ✅ Good: Record, Optional 필드, 기본값, 페이징
public record OrderSearchApiRequest(
    Long customerId,        // Optional
    String status,          // Optional
    @Min(0) Integer page,   // 필수 (기본값)
    @Min(1) @Max(100) Integer size
) {
    public OrderSearchApiRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
```

### ❌ Bad

```java
// ❌ Bad: Lombok 사용
@Data
@Builder
public class OrderSearchApiRequest {
    private Long customerId;
    private Integer page;
}

// ❌ Bad: 모든 필드 @NotNull (조회 조건은 Optional)
public record OrderSearchApiRequest(
    @NotNull Long customerId,  // ❌ 조회 조건은 Optional
    @NotNull String status
) {}

// ❌ Bad: Domain 변환 메서드
public record OrderSearchApiRequest(...) {
    public OrderSearchCriteria toCriteria() {  // ❌ Mapper 책임
        return new OrderSearchCriteria(...);
    }
}

// ❌ Bad: 페이징 없음
public record OrderSearchApiRequest(
    Long customerId,
    String status
    // ❌ 페이징 필수!
) {}
```

---

## 8) 변환 흐름

```
[HTTP GET Query Params]
    ↓
OrderSearchApiRequest (Query DTO)
    ↓
OrderApiMapper.toQuery()  ← @Component DI
    ↓
OrderSearchQuery (UseCase DTO)
    ↓
OrderQueryAdapter.findByCriteria()  ← Persistence Layer
    ↓
List<Order> (Domain Aggregate)
```

**중요**: 
- **API DTO → UseCase DTO**: Mapper 책임 (REST API Layer)
- Query DTO는 Domain 변환 로직 포함 금지
- 페이징 조건은 항상 포함 (무한 조회 방지)

---

## 9) 테스트

Query DTO의 단위 테스트 가이드는 별도 문서를 참고하세요:

**👉 [Query DTO Test Guide](./query-dto-test-guide.md)**

**주요 테스트 항목**:
- ✅ 기본값 설정 테스트 (page, size null → defaults)
- ✅ Optional 필드 테스트 (null 허용)
- ✅ 날짜 범위 검증 테스트
- ✅ 페이징 범위 검증 (@Min, @Max)
- ✅ Bean Validation 테스트
- ✅ @Tag("unit"), @Tag("adapter-rest"), @Tag("dto") 필수

---

## 10) ArchUnit 검증

Query DTO의 아키텍처 규칙 자동 검증 (빌드 시 실행)은 별도 가이드를 참고하세요:

**👉 [Query DTO ArchUnit Guide](./query-dto-archunit.md)**

**주요 검증 규칙** (10개):
- ✅ Record 타입 필수
- ✅ *ApiRequest 네이밍 규칙
- ❌ Lombok 어노테이션 절대 금지
- ❌ Jackson 어노테이션 절대 금지
- ❌ Domain 변환 메서드 금지 (toCriteria, toFilter 등)
- ❌ 비즈니스 로직 메서드 금지
- ✅ Bean Validation 어노테이션 사용 권장 (페이징 필드)
- ✅ 올바른 패키지 위치
- ❌ Setter 메서드 절대 금지
- ❌ Spring 어노테이션 절대 금지

---

## 11) 체크리스트

- [ ] `public record` 키워드 사용
- [ ] `*ApiRequest` 네이밍 규칙 준수
- [ ] 조회 의도 명확 (Search, List, Detail 등)
- [ ] Optional 필드 (null 허용)
- [ ] 페이징 조건 필수 (page, size)
- [ ] 기본값 설정 (Compact Constructor)
- [ ] 날짜 범위 검증
- [ ] Lombok 사용 금지
- [ ] Jackson 어노테이션 금지
- [ ] Domain 변환 메서드 금지
- [ ] Javadoc 작성

---

**작성자**: Development Team  
**최종 수정일**: 2025-11-13  
**버전**: 1.0.0
