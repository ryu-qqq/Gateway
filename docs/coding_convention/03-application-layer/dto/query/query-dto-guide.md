# Query DTO Guide — **조회 조건 데이터**

> Query DTO는 **Read** 작업의 검색 조건을 담는 **순수한 불변 객체**입니다.
>
> **Java Record** 패턴을 사용하며, **데이터 전달만** 담당합니다.

---

## 1) 핵심 역할

* **데이터 전달**: 조회 조건을 계층 간 전달
* **불변성**: Record로 변경 불가능한 데이터
* **프레임워크 독립**: 외부 의존성 없는 순수 Java

---

## 2) 핵심 원칙

### 원칙 1: 순수 Java Record
- ✅ `public record` 키워드로 정의
- ✅ 필드만 존재
- ❌ `jakarta.validation` 의존성 금지
- ❌ Lombok 사용 금지

### 원칙 2: 데이터 전달 전용
- ❌ 비즈니스 로직 금지
- ❌ 비즈니스 검증 금지
- ❌ 기본값 처리 금지 (REST API Layer에서)
- ✅ Optional 필드 null 허용

### 원칙 3: 페이징 지원
- ✅ Offset: `page`, `size`
- ✅ No-Offset (커서): `lastId`, `size`

### 원칙 4: 네이밍 규칙
- ✅ `Get{Bc}Query` - 단건
- ✅ `Search{Bc}Query` - 다건 (Offset)
- ✅ `Search{Bc}CursorQuery` - 다건 (No-Offset)

---

## 3) 패키지 구조

```
application/{bc}/dto/query/
├── Get{Bc}Query.java
├── Search{Bc}Query.java
└── Search{Bc}CursorQuery.java
```

---

## 4) 템플릿 코드

### 단건 조회
```java
package com.ryuqq.application.{bc}.dto.query;

public record Get{Bc}Query(
    Long id
) {}
```

### Offset 페이징
```java
package com.ryuqq.application.{bc}.dto.query;

import java.time.Instant;

public record Search{Bc}Query(
    Long filterId,
    String status,
    Instant startDate,
    Instant endDate,
    String sortBy,        // 정렬 필드 (예: "createdAt", "name")
    String sortDirection, // 정렬 방향 (예: "ASC", "DESC")
    Integer page,
    Integer size
) {}
```

### No-Offset (커서) 페이징
```java
package com.ryuqq.application.{bc}.dto.query;

import java.time.Instant;

public record Search{Bc}CursorQuery(
    Long lastId,
    String status,
    Instant startDate,
    Instant endDate,
    Integer size
) {}
```

---

## 5) 실전 예시

### Offset 페이징
```java
package com.ryuqq.application.order.dto.query;

import java.time.Instant;

public record SearchOrdersQuery(
    Long customerId,
    String status,
    Instant startDate,
    Instant endDate,
    String sortBy,        // 정렬 필드: "orderedAt", "totalAmount" 등
    String sortDirection, // 정렬 방향: "ASC", "DESC"
    Integer page,
    Integer size
) {}
```

### No-Offset (커서) 페이징
```java
package com.ryuqq.application.order.dto.query;

import java.time.Instant;

public record SearchOrdersCursorQuery(
    Long lastOrderId,
    String status,
    Instant startDate,
    Instant endDate,
    Integer size
) {}
```

---

## 6) Do / Don't

### ❌ Bad

```java
// ❌ jakarta.validation
import jakarta.validation.constraints.*;
public record Query(@Min(0) Integer page) {}

// ❌ 기본값 처리
public record Query(Integer page, Integer size) {
    public Query {
        page = (page == null) ? 0 : page;  // REST API 책임!
    }
}

// ❌ 비즈니스 검증
public record Query(Instant startDate, Instant endDate) {
    public Query {
        if (startDate.isAfter(endDate)) {  // 비즈니스 로직!
            throw new IllegalArgumentException();
        }
    }
}
```

### ✅ Good

```java
// ✅ 순수 Record
public record Query(
    Instant startDate,
    Instant endDate,
    Integer page,
    Integer size
) {}
```

---

## 7) Offset vs No-Offset

### Offset
```java
public record Query(Integer page, Integer size) {}
// SQL: OFFSET page * size LIMIT size
// 사용: 관리자 페이지, 페이지 번호 필요
```

### No-Offset (커서)
```java
public record Query(Long lastId, Integer size) {}
// SQL: WHERE id > lastId LIMIT size
// 사용: 무한 스크롤, 대용량 데이터
```

---

## 8) 체크리스트

- [ ] `public record {Get|Search}{Bc}Query`
- [ ] 패키지: `application.{bc}.dto.query`
- [ ] 순수 Java 타입만
- [ ] `jakarta.validation` 사용 금지
- [ ] Optional 필드 null 허용
- [ ] 비즈니스 로직/검증 금지
- [ ] 기본값 처리 금지
- [ ] Lombok 미사용

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 3.0.0 (단순화)
