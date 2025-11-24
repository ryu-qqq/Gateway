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
- [ ] 패키지: `application.{bc}.port.in`
- [ ] Query DTO: `dto.query.Get{Bc}Query` 또는 `Search{Bc}Query`
- [ ] Response DTO: `dto.response.{Bc}DetailResponse` 또는 `{Bc}SummaryResponse`
- [ ] 단일 메서드: `{Bc}Response execute({Bc}Query query)`
- [ ] Domain 노출 금지: Response로만 반환
- [ ] 부작용 없음: 조회만 수행
- [ ] N+1 문제 방지: Fetch Join 또는 DTO Projection
- [ ] Javadoc 포함: `@author`, `@since`

---

## 📖 관련 문서

- **[Query DTO Guide](../../dto/query/query-dto-guide.md)** - Query DTO 작성 규칙
- **[Response DTO Guide](../../dto/response/response-dto-guide.md)** - Response DTO 작성 규칙
- **[QueryPort Guide](../../out/query/port-out-query-guide.md)** - Query Port 구현
- **[Command UseCase Guide](../command/port-in-command-guide.md)** - Command Port (상태 변경)
- **[Assembler Pattern](../../../assembler/assembler-guide.md)** - DTO ↔ Domain 변환

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 2.0.0 (DTO 패키지 분리)
