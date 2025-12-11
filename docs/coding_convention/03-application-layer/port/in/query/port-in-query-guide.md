# Query UseCase (Port-In) — **조회 추상화**

> Query UseCase는 조회(Read)를 추상화하는 **Inbound Port**입니다.
>
> `Query`, `Response`는 **별도 DTO 패키지**로 분리하여 관리합니다.

---

## 1) 핵심 역할

* **조회만**: Read 담당, 부작용 없음
* **CQRS Query 담당**: 읽기(R) 전용 Port
* **DTO 패키지 분리**: Query/Response는 dto 패키지에 정의
* **읽기 전용 Transaction**: Service 구현체에서 `@Transactional(readOnly = true)` 적용
* **반환 타입**: 단건 Response, `List<Response>`, 또는 **Pagination Response**

---

## 2) 핵심 원칙

### 원칙 1: DTO 패키지 분리
- ✅ Query는 `dto/query/` 패키지에 정의
- ✅ Response는 `dto/response/` 패키지에 정의
- ❌ UseCase 인터페이스 내부에 Record 정의 금지

### 원칙 2: 단일 조회 책임
- ✅ 하나의 UseCase는 하나의 조회 책임만 수행
- ❌ 여러 조회를 하나의 UseCase에 넣지 않음

### 원칙 3: Assembler 사용
- ✅ Query → Criteria 변환은 Assembler에 위임
- ✅ Domain → Response 변환은 Assembler에 위임
- ❌ Service에서 직접 변환 로직 작성 금지

### 원칙 4: 읽기 전용 Transaction
- ✅ Service 구현체에 `@Transactional(readOnly = true)` 적용
- ❌ UseCase 인터페이스에 `@Transactional` 적용 금지

### 원칙 5: Domain 노출 금지
- ✅ Response Record로 변환하여 반환
- ❌ Domain Entity 직접 반환 금지

---

## 3) 패키지 구조

```
application/order/
├── dto/
│   ├── query/
│   │   ├── GetOrderQuery.java
│   │   └── SearchOrdersQuery.java
│   └── response/
│       ├── OrderDetailResponse.java
│       └── OrderSummaryResponse.java
└── port/
    └── in/
        └── query/
             ├── GetOrderUseCase.java
             └── SearchOrdersUseCase.java
```

---

## 4) 템플릿 코드

### Query DTO

### UseCase Interface
```java
package com.ryuqq.application.{bc}.port.in;

import com.ryuqq.application.{bc}.dto.query.Get{Bc}Query;
import com.ryuqq.application.{bc}.dto.response.{Bc}DetailResponse;

/**
 * Get {Bc} UseCase (Query)
 *
 * <p>조회를 담당하는 Inbound Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface Get{Bc}UseCase {

    /**
     * {Bc} 조회
     *
     * @param query 조회 조건
     * @return 조회 결과
     */
    {Bc}DetailResponse execute(Get{Bc}Query query);
}
```

---

## 5) 실전 예시 (GetOrder)

### UseCase Interface
```java
package com.ryuqq.application.order.port.in;

import com.ryuqq.application.order.dto.query.GetOrderQuery;
import com.ryuqq.application.order.dto.response.OrderDetailResponse;

/**
 * Get Order UseCase (Query)
 *
 * <p>주문 조회를 담당하는 Inbound Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GetOrderUseCase {

    /**
     * 주문 조회
     *
     * @param query 조회 조건
     * @return 주문 상세 정보
     */
    OrderDetailResponse execute(GetOrderQuery query);
}
```
---

### UseCase Interface
```java
package com.ryuqq.application.order.port.in;

import com.ryuqq.application.order.dto.query.SearchOrdersQuery;
import com.ryuqq.application.order.dto.response.OrderSummaryResponse;
import java.util.List;

/**
 * Search Orders UseCase (Query)
 *
 * <p>주문 목록 조회를 담당하는 Inbound Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface SearchOrdersUseCase {

    /**
     * 주문 목록 조회
     *
     * @param query 검색 조건
     * @return 주문 목록
     */
    List<OrderSummaryResponse> execute(SearchOrdersQuery query);
}
```

---

## 6) Pagination 패턴

> **목적**: 대량 데이터 조회 시 효율적인 페이징 처리

### Pagination 유형 선택 가이드

| 패턴 | 사용 시기 | 특징 |
|------|----------|------|
| **PageResponse** | 전체 개수가 필요한 경우 (관리자 페이지) | 총 페이지 수, 총 건수 제공 |
| **SliceResponse** | 무한 스크롤 (다음 페이지 존재 여부만 필요) | COUNT 쿼리 생략, 성능 우수 |
| **CursorResponse** | 실시간 데이터, 대용량 처리 | 일관된 결과, 중복/누락 방지 |

### PageResponse UseCase (Offset 기반)

```java
package com.ryuqq.application.{bc}.port.in.query;

import com.ryuqq.application.{bc}.dto.query.Search{Bc}Query;
import com.ryuqq.application.{bc}.dto.response.{Bc}SummaryResponse;
import com.ryuqq.application.common.dto.PageResponse;

/**
 * Search {Bc} UseCase (Pagination)
 *
 * <p>페이징 조회를 담당하는 Inbound Port</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface Search{Bc}UseCase {

    /**
     * {Bc} 목록 페이징 조회
     *
     * @param query 검색 조건 (page, size 포함)
     * @return 페이징된 결과 (총 개수, 총 페이지 포함)
     */
    PageResponse<{Bc}SummaryResponse> execute(Search{Bc}Query query);
}
```

### SliceResponse UseCase (무한 스크롤)

```java
package com.ryuqq.application.{bc}.port.in.query;

import com.ryuqq.application.{bc}.dto.query.Search{Bc}Query;
import com.ryuqq.application.{bc}.dto.response.{Bc}SummaryResponse;
import com.ryuqq.application.common.dto.SliceResponse;

/**
 * Search {Bc} UseCase (Infinite Scroll)
 *
 * <p>무한 스크롤 조회를 담당하는 Inbound Port</p>
 * <p>COUNT 쿼리를 생략하여 성능 최적화</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface Search{Bc}UseCase {

    /**
     * {Bc} 목록 조회 (무한 스크롤)
     *
     * @param query 검색 조건 (page, size 포함)
     * @return 슬라이스 결과 (hasNext 여부 포함)
     */
    SliceResponse<{Bc}SummaryResponse> execute(Search{Bc}Query query);
}
```

### CursorResponse UseCase (커서 기반)

```java
package com.ryuqq.application.{bc}.port.in.query;

import com.ryuqq.application.{bc}.dto.query.Search{Bc}CursorQuery;
import com.ryuqq.application.{bc}.dto.response.{Bc}SummaryResponse;
import com.ryuqq.application.common.dto.CursorResponse;

/**
 * Search {Bc} UseCase (Cursor-based)
 *
 * <p>커서 기반 조회를 담당하는 Inbound Port</p>
 * <p>실시간 데이터, 대용량 처리에 적합</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface Search{Bc}UseCase {

    /**
     * {Bc} 목록 조회 (커서 기반)
     *
     * @param query 검색 조건 (cursor, size 포함)
     * @return 커서 결과 (nextCursor 포함)
     */
    CursorResponse<{Bc}SummaryResponse> execute(Search{Bc}CursorQuery query);
}
```

### Pagination Response DTO 예시

```java
// application/common/dto/PageResponse.java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
            content, page, size, totalElements, totalPages,
            page < totalPages - 1,
            page > 0
        );
    }
}

// application/common/dto/SliceResponse.java
public record SliceResponse<T>(
    List<T> content,
    int page,
    int size,
    boolean hasNext
) {
    public static <T> SliceResponse<T> of(List<T> content, int page, int size, boolean hasNext) {
        return new SliceResponse<>(content, page, size, hasNext);
    }
}

// application/common/dto/CursorResponse.java
public record CursorResponse<T>(
    List<T> content,
    String nextCursor,
    boolean hasNext,
    int size
) {
    public static <T> CursorResponse<T> of(List<T> content, String nextCursor, int size) {
        return new CursorResponse<>(content, nextCursor, nextCursor != null, size);
    }
}
```

> **Pagination 선택 기준:**
> - 관리자 페이지, 전통적 페이지 네비게이션 → **PageResponse**
> - 모바일 앱, 무한 스크롤 UI → **SliceResponse**
> - 실시간 피드, 대용량 데이터 → **CursorResponse**

---

## 7) Do / Don't

### ❌ Bad Examples

```java
// ❌ UseCase 내부에 Query/Response Record 정의
public interface GetOrderUseCase {
    Response execute(Query query);
    
    record Query(...) {}  // 금지!
    record Response(...) {}  // 금지!
}

// ❌ 여러 조회를 하나의 UseCase에
public interface OrderQueryUseCase {
    OrderDetailResponse getOrder(Long id);  // 금지!
    List<OrderSummaryResponse> searchOrders(SearchOrdersQuery query);  // 금지!
}

// ❌ Domain Entity 직접 반환
public interface GetOrderUseCase {
    Order execute(GetOrderQuery query);  // 금지! Domain 노출
}

// ❌ UseCase 인터페이스에 @Transactional
@Transactional(readOnly = true)  // 금지!
public interface GetOrderUseCase {
    OrderDetailResponse execute(GetOrderQuery query);
}

// ❌ readOnly 없는 Transaction
@Service
@Transactional  // 금지! readOnly = true 필수
public class GetOrderService implements GetOrderUseCase {
    // ...
}
```

### ✅ Good Examples

```java
// ✅ 별도 DTO 패키지
// dto/query/GetOrderQuery.java
public record GetOrderQuery(...) {}

// dto/response/OrderDetailResponse.java
public record OrderDetailResponse(...) {}

// port/in/GetOrderUseCase.java
public interface GetOrderUseCase {
    OrderDetailResponse execute(GetOrderQuery query);
}

// ✅ 단일 조회 책임
public interface GetOrderUseCase {
    OrderDetailResponse execute(GetOrderQuery query);
}

public interface SearchOrdersUseCase {
    List<OrderSummaryResponse> execute(SearchOrdersQuery query);
}

```

---

## 8) 체크리스트

Query UseCase 작성 시:
- [ ] 인터페이스명: `Get{Bc}UseCase` 또는 `Search{Bc}UseCase`
- [ ] 패키지: `application.{bc}.port.in.query`
- [ ] Query DTO: `dto.query.Get{Bc}Query` 또는 `Search{Bc}Query`
- [ ] Response DTO: `dto.response.{Bc}DetailResponse` 또는 `{Bc}SummaryResponse`
- [ ] 단일 메서드: `{Bc}Response execute({Bc}Query query)`
- [ ] Domain 노출 금지: Response로만 반환
- [ ] 부작용 없음: 조회만 수행
- [ ] N+1 문제 방지: Fetch Join 또는 DTO Projection
- [ ] Javadoc 포함: `@author`, `@since`

Pagination 사용 시:
- [ ] 반환 타입 선택: `PageResponse`, `SliceResponse`, 또는 `CursorResponse`
- [ ] Query에 페이징 파라미터 포함: `page`, `size` 또는 `cursor`
- [ ] 관리자 페이지 → `PageResponse` (총 개수 필요)
- [ ] 무한 스크롤 → `SliceResponse` (COUNT 쿼리 생략)
- [ ] 실시간/대용량 → `CursorResponse` (중복/누락 방지)

---

## 📖 관련 문서

- **[Query DTO Guide](../../dto/query/query-dto-guide.md)** - Query DTO 작성 규칙
- **[Response DTO Guide](../../dto/response/response-dto-guide.md)** - Response DTO 작성 규칙
- **[QueryPort Guide](../../out/query/port-out-query-guide.md)** - Query Port 구현
- **[Command UseCase Guide](../command/port-in-command-guide.md)** - Command Port (상태 변경)
- **[Assembler Pattern](../../../assembler/assembler-guide.md)** - DTO ↔ Domain 변환

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.1.0 (Pagination 패턴 추가)
