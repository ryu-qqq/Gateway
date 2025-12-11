# 조회용 공통 VO 가이드

> Domain Layer의 조회 조건용 공통 Value Object 설계 가이드

---

## 1. 개요

### 1.1 위치

```
domain/common/vo/
├── DateRange.java         # 날짜 범위
├── SortDirection.java     # 정렬 방향 (ASC/DESC)
├── SortKey.java           # 정렬 키 인터페이스
├── PageRequest.java       # 오프셋 기반 페이징
└── CursorPageRequest.java # 커서 기반 페이징
```

### 1.2 왜 Domain Layer인가?

```
Controller (RequestDto)
    ↓
UseCase (Application Query *Query)
    ↓  비즈니스 로직 적용 (권한별 기간 조정 등)
Domain Criteria ← DateRange, SortKey, PageRequest 사용
    ↓
QueryPort
    ↓
Adapter (QueryDSL)
```

- **Domain Criteria에서 사용**: 조회 조건을 도메인 언어로 표현
- **올바른 의존 방향**: Application → Domain (OK)
- **순수 Java**: 외부 의존성 없음

---

## 2. 조회용 VO 상세

### 2.1 DateRange - 날짜 범위

```java
public record DateRange(
    LocalDate startDate,  // nullable - null이면 제한 없음
    LocalDate endDate     // nullable - null이면 제한 없음
) {
    // Compact Constructor: startDate <= endDate 검증
}
```

**팩토리 메서드:**

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `of(start, end)` | 특정 기간 | `DateRange.of(startDate, endDate)` |
| `lastDays(n)` | 최근 N일 | `DateRange.lastDays(7)` |
| `thisMonth()` | 이번 달 | `DateRange.thisMonth()` |
| `lastMonth()` | 지난 달 | `DateRange.lastMonth()` |
| `from(start)` | 시작일부터 | `DateRange.from(startDate)` |
| `until(end)` | 종료일까지 | `DateRange.until(endDate)` |

**유틸리티 메서드:**

| 메서드 | 반환 타입 | 설명 |
|--------|----------|------|
| `startInstant()` | `Instant` | 시작일 00:00:00 (시스템 ZoneId) |
| `endInstant()` | `Instant` | 종료일 23:59:59.999... (시스템 ZoneId) |
| `isEmpty()` | `boolean` | 시작일, 종료일 모두 null인지 |
| `contains(date)` | `boolean` | 특정 날짜가 범위 내인지 |

**⚠️ 중요**: Domain Layer 규칙에 따라 `LocalDateTime` 대신 `Instant`를 반환합니다.

---

### 2.2 SortDirection - 정렬 방향

```java
public enum SortDirection {
    ASC,   // 오름차순 (오래된순, 낮은순, A→Z)
    DESC;  // 내림차순 (최신순, 높은순, Z→A)
}
```

**메서드:**

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `defaultDirection()` | 기본값 (DESC) | `SortDirection.defaultDirection()` |
| `isAscending()` | ASC인지 | `direction.isAscending()` |
| `isDescending()` | DESC인지 | `direction.isDescending()` |
| `reverse()` | 반대 방향 | `ASC.reverse()` → `DESC` |
| `fromString(value)` | 문자열 파싱 | `fromString("desc")` → `DESC` |

---

### 2.3 SortKey - 정렬 키 인터페이스

각 Bounded Context에서 구현하는 마커 인터페이스입니다.

```java
// domain/common/vo/SortKey.java
public interface SortKey {
    String fieldName();  // 도메인 언어 필드명
}
```

**BC별 구현 예시:**

```java
// domain/order/vo/OrderSortKey.java
public enum OrderSortKey implements SortKey {
    ORDER_DATE("orderDate"),
    TOTAL_AMOUNT("totalAmount"),
    MEMBER_NAME("memberName");

    private final String fieldName;

    OrderSortKey(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }
}
```

**Adapter에서 매핑:**

```java
// adapter-out/persistence-mysql/.../OrderQueryAdapter.java
private OrderSpecifier<?> toOrderSpecifier(OrderSortKey sortKey, SortDirection direction) {
    Order order = direction.isAscending() ? Order.ASC : Order.DESC;
    return switch (sortKey) {
        case ORDER_DATE -> new OrderSpecifier<>(order, orderEntity.orderDate);
        case TOTAL_AMOUNT -> new OrderSpecifier<>(order, orderEntity.totalAmount);
        case MEMBER_NAME -> new OrderSpecifier<>(order, orderEntity.memberName);
    };
}
```

---

### 2.4 PageRequest - 오프셋 기반 페이징

```java
public record PageRequest(
    int page,   // 페이지 번호 (0부터 시작)
    int size    // 페이지 크기 (기본 20, 최대 100)
) {}
```

**팩토리 메서드:**

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `of(page, size)` | 페이지 지정 | `PageRequest.of(0, 20)` |
| `first(size)` | 첫 페이지 | `PageRequest.first(20)` |
| `defaultPage()` | 기본 설정 | `PageRequest.defaultPage()` |

**유틸리티 메서드:**

| 메서드 | 설명 | 용도 |
|--------|------|------|
| `offset()` | OFFSET 계산 | SQL OFFSET 값 |
| `next()` | 다음 페이지 | 페이지 이동 |
| `previous()` | 이전 페이지 | 페이지 이동 |
| `isFirst()` | 첫 페이지인지 | UI 표시 |
| `isLast(total)` | 마지막 페이지인지 | UI 표시 |
| `totalPages(total)` | 전체 페이지 수 | UI 표시 |

---

### 2.5 CursorPageRequest - 커서 기반 페이징

```java
public record CursorPageRequest(
    String cursor,  // 커서 값 (null이면 첫 페이지)
    int size        // 페이지 크기 (기본 20, 최대 100)
) {}
```

**팩토리 메서드:**

| 메서드 | 설명 | 예시 |
|--------|------|------|
| `of(cursor, size)` | 커서 지정 | `CursorPageRequest.of("abc", 20)` |
| `first(size)` | 첫 페이지 | `CursorPageRequest.first(20)` |
| `defaultPage()` | 기본 설정 | `CursorPageRequest.defaultPage()` |
| `afterId(id, size)` | ID 기반 커서 | `CursorPageRequest.afterId(100L, 20)` |

**유틸리티 메서드:**

| 메서드 | 설명 | 용도 |
|--------|------|------|
| `isFirstPage()` | 첫 페이지인지 | cursor == null |
| `hasCursor()` | 커서 있는지 | cursor != null |
| `cursorAsLong()` | Long으로 파싱 | ID 기반 커서 |
| `fetchSize()` | 조회 크기 | size + 1 (hasNext 판단용) |

---

## 3. 페이징 전략 선택

### 3.1 비교표

| 기준 | PageRequest (Offset) | CursorPageRequest (Cursor) |
|------|---------------------|---------------------------|
| **UI** | 페이지 번호 표시 | 무한 스크롤, 더보기 |
| **성능** | 대량 데이터 시 느림 | 대량 데이터에도 일정 |
| **COUNT 쿼리** | 필요 | 불필요 |
| **랜덤 접근** | 가능 (10페이지 점프) | 불가능 (순차만) |
| **실시간 데이터** | 중복/누락 가능 | 안정적 |

### 3.2 선택 가이드

```
Q: 전체 페이지 수를 보여줘야 하나요?
├─ Yes → PageRequest + PageResponse
└─ No
    Q: 대량 데이터(10만건+)인가요?
    ├─ Yes → CursorPageRequest + SliceResponse
    └─ No → 둘 다 가능 (UI 선호도 따라)
```

---

## 4. Domain Criteria에서 사용

### 4.1 Criteria 위치

```
domain/
└── {bounded-context}/
    └── query/
        └── criteria/
            ├── {Entity}SearchCriteria.java   # 검색 조건
            └── {Entity}SortKey.java          # 정렬 키 enum
```

### 4.2 Criteria 예시

```java
// domain/order/query/criteria/OrderSearchCriteria.java
public record OrderSearchCriteria(
    Long memberId,                    // 회원 ID 필터
    OrderStatus status,               // 주문 상태 필터
    DateRange orderDateRange,         // 주문일 범위
    OrderSortKey sortKey,             // 정렬 키
    SortDirection sortDirection,      // 정렬 방향
    PageRequest page                  // 페이징
) {
    /**
     * 기본값 적용 팩토리 메서드
     */
    public static OrderSearchCriteria of(
        Long memberId,
        OrderStatus status,
        DateRange orderDateRange,
        OrderSortKey sortKey,
        SortDirection sortDirection,
        PageRequest page
    ) {
        return new OrderSearchCriteria(
            memberId,
            status,
            orderDateRange,
            sortKey != null ? sortKey : OrderSortKey.ORDER_DATE,
            sortDirection != null ? sortDirection : SortDirection.DESC,
            page != null ? page : PageRequest.defaultPage()
        );
    }
}
```

### 4.3 Application Query → Domain Criteria 변환

```java
// application/order/query/OrderSearchQuery.java
public record OrderSearchQuery(
    Long memberId,
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate,
    String sortKey,
    String sortDirection,
    int page,
    int size
) {
    /**
     * Domain Criteria로 변환
     * 이 과정에서 비즈니스 로직 적용 가능
     */
    public OrderSearchCriteria toCriteria(UserRole userRole) {
        // 비즈니스 로직: 권한별 조회 기간 조정
        DateRange dateRange = adjustDateRangeByRole(
            DateRange.of(startDate, endDate),
            userRole
        );

        return OrderSearchCriteria.of(
            memberId,
            status,
            dateRange,
            parseSortKey(sortKey),
            SortDirection.fromString(sortDirection),
            PageRequest.of(page, size)
        );
    }

    private DateRange adjustDateRangeByRole(DateRange range, UserRole role) {
        // 일반 회원: 최대 1년, 관리자: 제한 없음
        if (role == UserRole.MEMBER && range.startDate() != null) {
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            if (range.startDate().isBefore(oneYearAgo)) {
                return DateRange.of(oneYearAgo, range.endDate());
            }
        }
        return range;
    }
}
```

---

## 5. 체크리스트

### Criteria 설계 시 권장사항

- [ ] 날짜 범위 필터 → `DateRange` 사용
- [ ] 정렬 필요 → `SortKey` + `SortDirection` 사용
- [ ] 페이징 필요 → `PageRequest` 또는 `CursorPageRequest` 사용
- [ ] 기본값 적용하는 팩토리 메서드 제공
- [ ] `domain/{bc}/query/criteria/` 위치

### SortKey 구현 시

- [ ] `domain/{bc}/vo/` 패키지에 위치
- [ ] `enum`으로 구현
- [ ] `implements SortKey` 선언
- [ ] `fieldName()` 메서드 구현
- [ ] 도메인 언어 사용 (DB 컬럼명 X)

### Adapter 구현 시

- [ ] `SortKey` → `OrderSpecifier` 변환 로직
- [ ] `DateRange` → `BooleanExpression` 변환 로직
- [ ] `PageRequest.offset()` 활용
- [ ] `CursorPageRequest.fetchSize()` 활용 (LIMIT+1)

---

## 6. 관련 문서

- [VO Guide](./vo-guide.md) - Value Object 전체 가이드
- [Domain Guide](../domain-guide.md) - Domain Layer 전체 가이드
- [Query Adapter Guide](../../04-persistence-layer/mysql/adapter/query/query-adapter-guide.md) - QueryDSL Adapter
