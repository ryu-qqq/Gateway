# LockKey ArchUnit 규칙

> LockKey VO 아키텍처 규칙 및 ArchUnit 테스트 가이드

## 1. 개요

### 1.1 위치

```
domain/src/test/java/
└── com/ryuqq/domain/architecture/vo/
    └── LockKeyArchTest.java    # LockKey VO 아키텍처 검증
```

### 1.2 테스트 태그

```java
@Tag("architecture")
@Tag("domain")
@Tag("vo")
@Tag("lock")
```

### 1.3 실행 방법

```bash
# LockKey ArchUnit 테스트만 실행
./gradlew :domain:test --tests "*LockKeyArchTest" -x jacocoTestCoverageVerification

# 전체 VO ArchUnit 테스트 실행
./gradlew :domain:test --tests "*VOArchTest" --tests "*LockKeyArchTest"

# 태그 기반 실행
./gradlew :domain:test -PincludeTags=lock
```

## 2. ArchUnit 규칙 (10개)

### 2.1 필수 구조 규칙 (3개)

| 규칙 | 설명 | 검증 내용 |
|------|------|----------|
| **규칙 1** | LockKey 인터페이스 구현 | `implements LockKey` 필수 |
| **규칙 2** | Record 타입 필수 | `public record` 사용 |
| **규칙 3** | value() 메서드 필수 | `String value()` 반환 |

### 2.2 네이밍 및 패키지 규칙 (2개)

| 규칙 | 설명 | 검증 내용 |
|------|------|----------|
| **규칙 4** | *LockKey 네이밍 | `OrderLockKey` 형식 |
| **규칙 5** | vo 패키지 위치 | `domain.[bc].vo` 패키지 |

### 2.3 금지 규칙 (4개)

| 규칙 | 설명 | 검증 내용 |
|------|------|----------|
| **규칙 6** | Lombok 금지 | `@Data`, `@Value`, `@Builder` 등 금지 |
| **규칙 7** | JPA 금지 | `@Entity`, `@Embeddable` 등 금지 |
| **규칙 8** | Spring 어노테이션 금지 | `@Component`, `@Service` 등 금지 |
| **규칙 9** | Spring 의존성 금지 | `org.springframework..` 패키지 의존 금지 |

### 2.4 레이어 의존성 규칙 (1개)

| 규칙 | 설명 | 검증 내용 |
|------|------|----------|
| **규칙 10** | 외부 레이어 의존 금지 | `application`, `adapter` 패키지 의존 금지 |

## 3. DomainEvent와의 비교

LockKey는 DomainEvent와 동일한 상속 패턴을 따릅니다:

| 항목 | DomainEvent | LockKey |
|------|-------------|---------|
| 기반 인터페이스 | `interface DomainEvent` | `interface LockKey` |
| 위치 | `domain/common/event/` | `domain/common/vo/` |
| 구현체 위치 | `domain/{bc}/event/` | `domain/{bc}/vo/` |
| 구현체 예시 | `OrderPlacedEvent` | `OrderLockKey` |
| 필수 메서드 | `occurredAt()` | `value()` |
| 타입 | Record | Record |

## 4. 올바른 구현 예시

### 4.1 기본 LockKey

```java
// domain/order/vo/OrderLockKey.java
public record OrderLockKey(Long orderId) implements LockKey {

    private static final String PREFIX = "lock:order:";

    public OrderLockKey {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + orderId;
    }
}
```

### 4.2 복합 키 LockKey

```java
// domain/stock/vo/StockWarehouseLockKey.java
public record StockWarehouseLockKey(
    Long productId,
    Long warehouseId
) implements LockKey {

    public StockWarehouseLockKey {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (warehouseId == null || warehouseId <= 0) {
            throw new IllegalArgumentException("warehouseId must be positive");
        }
    }

    @Override
    public String value() {
        return "lock:stock:" + productId + ":warehouse:" + warehouseId;
    }
}
```

## 5. 위반 사례

### 5.1 Record가 아닌 경우

```java
// ❌ 잘못된 예: class 사용
public class OrderLockKey implements LockKey {
    private final Long orderId;
    // ...
}

// ✅ 올바른 예: record 사용
public record OrderLockKey(Long orderId) implements LockKey {
    // ...
}
```

### 5.2 LockKey 인터페이스 미구현

```java
// ❌ 잘못된 예: LockKey 인터페이스 없음
public record OrderLockKey(Long orderId) {
    public String value() { return "lock:order:" + orderId; }
}

// ✅ 올바른 예: LockKey 구현
public record OrderLockKey(Long orderId) implements LockKey {
    @Override
    public String value() { return "lock:order:" + orderId; }
}
```

### 5.3 잘못된 패키지 위치

```java
// ❌ 잘못된 예: aggregate 패키지
package com.ryuqq.domain.order.aggregate;

// ✅ 올바른 예: vo 패키지
package com.ryuqq.domain.order.vo;
```

### 5.4 Lombok 사용

```java
// ❌ 잘못된 예: Lombok 사용
@Value
public class OrderLockKey implements LockKey {
    Long orderId;
}

// ✅ 올바른 예: Pure Java Record
public record OrderLockKey(Long orderId) implements LockKey {
    // ...
}
```

## 6. 키 형식 규칙

### 6.1 형식

```
lock:{domain}:{id}
lock:{domain}:{entity}:{id}
lock:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
```

### 6.2 예시

| 도메인 | 키 형식 | 예시 |
|--------|---------|------|
| 주문 | `lock:order:{orderId}` | `lock:order:123` |
| 재고 | `lock:stock:item:{productId}` | `lock:stock:item:456` |
| 재고+창고 | `lock:stock:{productId}:warehouse:{warehouseId}` | `lock:stock:456:warehouse:1` |

## 7. 체크리스트

### LockKey 구현 시

- [ ] `domain/{bc}/vo/` 패키지에 위치
- [ ] `record` 키워드 사용
- [ ] `implements LockKey` 선언
- [ ] `value()` 메서드 구현
- [ ] compact constructor에서 유효성 검증
- [ ] 키 형식 규칙 준수 (`lock:{domain}:...`)
- [ ] Lombok, JPA, Spring 어노테이션 없음

### ArchUnit 테스트 실행 시

- [ ] `./gradlew :domain:test --tests "*LockKeyArchTest"`
- [ ] 전체 10개 규칙 통과 확인
