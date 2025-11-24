# Response DTO Guide — **클라이언트 응답 데이터**

> Response DTO는 **클라이언트에게 반환하는 데이터**를 담는 **순수한 불변 객체**입니다.
>
> **Java Record** 패턴을 사용하며, **데이터 전달만** 담당합니다.

---

## 1) 핵심 역할

* **데이터 전달**: 응답 데이터를 계층 간 전달
* **불변성**: Record로 변경 불가능한 데이터
* **프레임워크 독립**: 외부 의존성 없는 순수 Java
* **Domain 격리**: Domain Entity 직접 노출 금지

---

## 2) 핵심 원칙

### 원칙 1: 순수 Java Record
- ✅ `public record` 키워드로 정의
- ✅ 필드만 존재
- ❌ Jackson 어노테이션 금지 (`@JsonFormat`, `@JsonProperty`)
- ❌ Lombok 사용 금지

### 원칙 2: 데이터 전달 전용
- ❌ 비즈니스 로직 금지
- ❌ 편의 메서드 금지
- ✅ Nested Record로 구조 표현

### 원칙 3: Domain 노출 금지
- ❌ Domain Entity 직접 반환
- ❌ 민감 정보 노출 (비밀번호, 토큰)
- ✅ Assembler로 변환

### 원칙 4: 네이밍 규칙
- ✅ `{Bc}Response` - 기본
- ✅ `{Bc}DetailResponse` - 상세
- ✅ `{Bc}SummaryResponse` - 요약 (목록용)

---

## 3) 패키지 구조

```
application/{bc}/dto/response/
├── {Bc}Response.java
├── {Bc}DetailResponse.java
└── {Bc}SummaryResponse.java
```

---

## 4) 템플릿 코드

### 기본 패턴
```java
package com.ryuqq.application.{bc}.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * {Bc} Response
 *
 * @author development-team
 * @since 1.0.0
 */
public record {Bc}Response(
    Long id,
    String name,
    String status,
    Instant createdAt,
    List<NestedInfo> nestedInfos
) {
    public record NestedInfo(
        Long id,
        String value
    ) {}
}
```

---

## 5) 실전 예시

### Detail Response
```java
package com.ryuqq.application.order.dto.response;

import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
    Long id,
    CustomerInfo customer,
    List<LineItem> items,
    Long amount,
    String status,
    Instant orderedAt
) {
    public record CustomerInfo(
        Long id,
        String name,
        String email
    ) {}

    public record LineItem(
        Long id,
        String productName,
        Integer quantity,
        Long unitPrice
    ) {}
}
```

### Summary Response
```java
package com.ryuqq.application.order.dto.response;

import java.time.Instant;

public record OrderSummaryResponse(
    Long orderId,
    String customerName,
    Long totalAmount,
    String status,
    Instant orderedAt
) {}
```

---

## 6) 체크리스트

- [ ] `public record {Bc}Response`
- [ ] 패키지: `application.{bc}.dto.response`
- [ ] 순수 Java 타입만
- [ ] Jackson 어노테이션 금지
- [ ] 비즈니스 로직/편의 메서드 금지
- [ ] Lombok 미사용

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0 (단순화)
