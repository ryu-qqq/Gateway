# Value Object 설계 가이드

> **Value Object (VO) 설계 규칙**
>
> Java 21 Record를 활용한 불변 Value Object 구현 패턴을 정의합니다.

---

## 1) 핵심 원칙

* **Record 사용 필수**: Java 21 Record로 구현. 불변성과 equals/hashCode 자동 보장.
* **정적 팩토리 메서드**: `of()` 메서드로 생성 (ID VO는 `forNew()` 추가).
* **Compact Constructor**: Self-Validation 로직은 Compact Constructor에 구현.
* **값 기반 동등성**: Record가 자동으로 equals/hashCode 생성.
* **외부 의존성 제로**: Lombok, JPA, Spring 등 외부 의존성 절대 금지. Pure Java만 사용.

---

## 2) 생성 메서드 패턴

### ID VO - Long 타입 (Auto Increment)

| 메서드 | 반환값 | null 허용 | 용도 |
|--------|--------|-----------|------|
| `forNew()` | null | ✅ 허용 | 신규 생성 (DB가 ID 할당) |
| `of(Long value)` | 값 | ❌ 금지 | 기존 ID 참조 |
| `isNew()` | boolean | - | null 여부 확인 |

### ID VO - UUID 타입 (UUIDv7)

| 메서드 | 반환값 | null 허용 | 용도 |
|--------|--------|-----------|------|
| `forNew()` | UUIDv7 | ❌ 금지 | 신규 생성 (Application이 ID 생성) |
| `of(String value)` | 값 | ❌ 금지 | 기존 UUID 파싱 |

### 일반 VO (Money, Email 등)

| 메서드 | 값 전달 | null 체크 | 용도 |
|--------|---------|-----------|------|
| `of(...)` | ✅ 필수 | ✅ 필수 | 값 기반 생성 |

---

## 3) VO 유형별 템플릿

### 3-1) ID VO - Long 타입 (Auto Increment)

**특징**:
- Record로 구현
- **Long 타입 래핑 (DB AUTO_INCREMENT 사용)**
- `forNew()` 제공 (**null 허용** - DB가 ID 할당 예정)
- `of(Long value)` 제공 (null 체크 필수)
- `isNew()` 제공 (null 여부 확인)
- Compact Constructor에서 검증

**사용 시점**: 내부 PK로 사용, 외부 노출 비권장 (추측 가능)

```java
/**
 * Order ID Value Object (Auto Increment)
 *
 * <p><strong>DB 전략</strong>: MySQL AUTO_INCREMENT - DB가 ID 할당</p>
 *
 * <p><strong>생성 패턴</strong>:</p>
 * <ul>
 *   <li>{@code forNew()} - 신규 엔티티 생성 시 (ID = null, DB가 할당 예정)</li>
 *   <li>{@code of(Long value)} - 기존 엔티티 조회/참조 시 (ID 필수)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderId(Long value) {

    /**
     * Compact Constructor (검증 로직)
     *
     * <p>주의: forNew()로 생성 시 null 허용 (DB AUTO_INCREMENT 대비)</p>
     */
    public OrderId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("OrderId는 양수여야 합니다: " + value);
        }
    }

    /**
     * 신규 생성 - DB AUTO_INCREMENT가 ID 할당 예정
     *
     * @return OrderId (value = null)
     */
    public static OrderId forNew() {
        return new OrderId(null);
    }

    /**
     * 기존 ID 참조 - null 금지
     *
     * @param value ID 값 (null 불가)
     * @return OrderId
     * @throws IllegalArgumentException value가 null이거나 음수인 경우
     */
    public static OrderId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("기존 OrderId는 null일 수 없습니다");
        }
        return new OrderId(value);
    }

    /**
     * 신규 엔티티 여부 확인
     *
     * @return ID가 null이면 true (아직 DB에 저장되지 않음)
     */
    public boolean isNew() {
        return value == null;
    }
}
```

---

### 3-2) ID VO - UUID 타입 (UUIDv7, Application 생성)

**특징**:
- Record로 구현
- **String 타입 래핑 (UUIDv7 형식)**
- `forNew()` 제공 (**UUIDv7 자동 생성** - null 불가)
- `of(String value)` 제공 (기존 UUID 파싱)
- `isNew()` 없음 (항상 값 존재)
- Compact Constructor에서 UUIDv7 형식 검증

**사용 시점**: 외부 노출 ID (보안), 분산 환경, 추측 불가능한 ID 필요 시

**의존성**: `com.github.f4b6a3:uuid-creator:6.0.0` (domain-guide.md 허용 예외 참조)

```java
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.regex.Pattern;

/**
 * User ID Value Object (UUIDv7 - Application Generated)
 *
 * <p><strong>특징</strong>:</p>
 * <ul>
 *   <li>UUIDv7: 시간 기반 정렬 가능 (첫 48bit = Unix timestamp ms)</li>
 *   <li>Application에서 생성 (DB 의존 없음)</li>
 *   <li>외부 노출 안전 (Long보다 추측 불가)</li>
 * </ul>
 *
 * <p><strong>MySQL 저장</strong>: BINARY(16) 권장 (36바이트 → 16바이트 절약)</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public record UserId(String value) {

    // UUIDv7 형식: 버전 7 (7xxx), 변형 8/9/a/b
    private static final Pattern UUIDV7_PATTERN =
        Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    /**
     * Compact Constructor (검증 로직)
     *
     * <p>UUIDv7 형식 검증 - null 절대 금지</p>
     */
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId는 null이거나 빈 문자열일 수 없습니다");
        }
        value = value.toLowerCase().trim();
        if (!UUIDV7_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 UUIDv7 형식입니다: " + value);
        }
    }

    /**
     * 신규 생성 - UUIDv7 자동 생성 (null 불가)
     *
     * @return UserId (UUIDv7 값)
     */
    public static UserId forNew() {
        return new UserId(UuidCreator.getTimeOrderedEpoch().toString());
    }

    /**
     * 기존 UUID 파싱
     *
     * @param value UUID 문자열
     * @return UserId
     * @throws IllegalArgumentException UUIDv7 형식이 아닌 경우
     */
    public static UserId of(String value) {
        return new UserId(value);
    }
}
```

---

### ID VO 비교 요약

| 항목 | Long ID (Auto Increment) | UUID ID (UUIDv7) |
|------|--------------------------|------------------|
| **타입** | `Long` | `String` (UUIDv7) |
| **생성 주체** | DB (AUTO_INCREMENT) | Application (uuid-creator) |
| **`forNew()`** | `null` 반환 | UUIDv7 생성 반환 |
| **`isNew()`** | ✅ 있음 | ❌ 없음 (항상 값 존재) |
| **null 허용** | ✅ 허용 (DB 할당 전) | ❌ 금지 |
| **MySQL 저장** | `BIGINT` (8바이트) | `BINARY(16)` (16바이트) |
| **외부 노출** | ❌ 비권장 (추측 가능) | ✅ 안전 (추측 불가) |
| **정렬** | 자연 정렬 | 시간순 정렬 (UUIDv7) |

### MySQL 인덱스 전략

```sql
-- Long ID (Auto Increment) - 기본 PK
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);

-- UUID ID (UUIDv7) - BINARY(16) 저장
CREATE TABLE users (
    id BINARY(16) PRIMARY KEY,  -- UUIDv7: 시간순 정렬 보장
    ...
);

-- Entity에서 변환 예시 (Persistence Layer)
-- UUID String → BINARY(16): UUID_TO_BIN(?, 1)
-- BINARY(16) → UUID String: BIN_TO_UUID(id, 1)
```

---

### 3-3) Simple VO - 단일 필드 (Money, Quantity 등)

**특징**:
- Record로 구현
- **단일 원시 타입 래핑**
- Compact Constructor에서 도메인 규칙 검증
- 연산 메서드 제공 (add, multiply 등)

```java
/**
 * Money Value Object
 *
 * <p><strong>도메인 규칙</strong>: 금액은 0 이상이어야 한다.</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public record Money(Long amount) {

    public static final Money ZERO = Money.of(0L);

    /**
     * Compact Constructor (검증 로직)
     */
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
    }

    /**
     * 값 기반 생성
     *
     * @param amount 금액 (null 불가, 0 이상)
     * @return Money
     * @throws IllegalArgumentException amount가 null이거나 음수인 경우
     */
    public static Money of(Long amount) {
        return new Money(amount);
    }

    /**
     * 금액 더하기
     *
     * @param other 더할 금액
     * @return 합계
     */
    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    /**
     * 금액 빼기
     *
     * @param other 뺄 금액
     * @return 차액
     * @throws IllegalArgumentException 결과가 음수인 경우
     */
    public Money subtract(Money other) {
        return new Money(this.amount - other.amount);
    }

    /**
     * 금액 곱하기
     *
     * @param multiplier 배수
     * @return 곱셈 결과
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount * multiplier);
    }

    /**
     * 금액 비교 (큰지)
     *
     * @param other 비교 대상
     * @return this가 크면 true
     */
    public boolean isGreaterThan(Money other) {
        return this.amount > other.amount;
    }

    /**
     * 금액 비교 (작은지)
     *
     * @param other 비교 대상
     * @return this가 작으면 true
     */
    public boolean isLessThan(Money other) {
        return this.amount < other.amount;
    }
}
```

---

### 3-4) Simple VO - 단일 필드 (Email, PhoneNumber 등)

**특징**:
- Record로 구현
- **단일 String 타입 래핑**
- Compact Constructor에서 복잡한 검증 로직 구현
- 정규식, 포맷 체크 등

```java
/**
 * Email Value Object
 *
 * <p><strong>도메인 규칙</strong>:</p>
 * <ul>
 *   <li>이메일 포맷: xxx@yyy.zzz</li>
 *   <li>최대 길이: 255자</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record Email(String value) {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final int MAX_LENGTH = 255;

    /**
     * Compact Constructor (검증 로직)
     */
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 null이거나 빈 문자열일 수 없습니다.");
        }

        value = value.trim();

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("이메일은 " + MAX_LENGTH + "자를 초과할 수 없습니다: " + value.length());
        }

        if (!value.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + value);
        }
    }

    /**
     * 값 기반 생성
     *
     * @param value 이메일 주소
     * @return Email
     * @throws IllegalArgumentException 이메일 포맷이 잘못된 경우
     */
    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * 도메인 추출
     *
     * @return 도메인 부분 (예: "gmail.com")
     */
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * 로컬 부분 추출
     *
     * @return 로컬 부분 (예: "user")
     */
    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }
}
```

---

### 3-5) Multi-field VO - 여러 원시 타입 필드 (Address 등)

**특징**:
- Record로 구현
- **여러 원시 타입(String, Long 등) 필드 조합**
- Compact Constructor에서 각 필드 검증
- VO 안에 VO는 없음 (모두 원시 타입)

```java
/**
 * Address Value Object
 *
 * <p><strong>도메인 규칙</strong>:</p>
 * <ul>
 *   <li>우편번호: 5자리 숫자</li>
 *   <li>주소: 100자 이내</li>
 *   <li>상세주소: 200자 이내</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record Address(String zipCode, String street, String detail) {

    private static final String ZIPCODE_PATTERN = "^\\d{5}$";
    private static final int MAX_STREET_LENGTH = 100;
    private static final int MAX_DETAIL_LENGTH = 200;

    /**
     * Compact Constructor (검증 로직)
     */
    public Address {
        if (zipCode == null || !zipCode.matches(ZIPCODE_PATTERN)) {
            throw new IllegalArgumentException("우편번호는 5자리 숫자여야 합니다: " + zipCode);
        }

        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("주소는 null이거나 빈 문자열일 수 없습니다.");
        }

        street = street.trim();
        if (street.length() > MAX_STREET_LENGTH) {
            throw new IllegalArgumentException("주소는 " + MAX_STREET_LENGTH + "자를 초과할 수 없습니다: " + street.length());
        }

        detail = detail != null ? detail.trim() : "";
        if (detail.length() > MAX_DETAIL_LENGTH) {
            throw new IllegalArgumentException("상세주소는 " + MAX_DETAIL_LENGTH + "자를 초과할 수 없습니다: " + detail.length());
        }
    }

    /**
     * 값 기반 생성
     *
     * @param zipCode 우편번호 (5자리 숫자)
     * @param street 주소 (100자 이내)
     * @param detail 상세주소 (200자 이내, null 가능)
     * @return Address
     * @throws IllegalArgumentException 검증 실패 시
     */
    public static Address of(String zipCode, String street, String detail) {
        return new Address(zipCode, street, detail);
    }

    /**
     * 전체 주소 문자열 반환
     *
     * @return "[우편번호] 주소 상세주소"
     */
    public String getFullAddress() {
        return String.format("[%s] %s %s", zipCode, street, detail).trim();
    }
}
```

---

### 3-6) Composite VO - VO 안에 VO (FullAddress 등)

**특징**:
- Record로 구현
- **VO 안에 다른 VO들을 포함**
- 각 VO는 독립적으로 검증됨
- 진정한 복합 필드 (Composite)

```java
/**
 * ZipCode Value Object
 *
 * <p><strong>도메인 규칙</strong>: 5자리 숫자</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public record ZipCode(String value) {

    private static final String ZIPCODE_PATTERN = "^\\d{5}$";

    public ZipCode {
        if (value == null || !value.matches(ZIPCODE_PATTERN)) {
            throw new IllegalArgumentException("우편번호는 5자리 숫자여야 합니다: " + value);
        }
    }

    public static ZipCode of(String value) {
        return new ZipCode(value);
    }
}
```

```java
/**
 * Street Value Object
 *
 * <p><strong>도메인 규칙</strong>: 100자 이내</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public record Street(String value) {

    private static final int MAX_LENGTH = 100;

    public Street {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("주소는 null이거나 빈 문자열일 수 없습니다.");
        }

        value = value.trim();

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("주소는 " + MAX_LENGTH + "자를 초과할 수 없습니다: " + value.length());
        }
    }

    public static Street of(String value) {
        return new Street(value);
    }
}
```

```java
/**
 * City Value Object
 *
 * <p><strong>도메인 규칙</strong>: 50자 이내</p>
 *
 * @author development-team
 * @since 1.0.0
 */
public record City(String name) {

    private static final int MAX_LENGTH = 50;

    public City {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("도시명은 null이거나 빈 문자열일 수 없습니다.");
        }

        name = name.trim();

        if (name.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("도시명은 " + MAX_LENGTH + "자를 초과할 수 없습니다: " + name.length());
        }
    }

    public static City of(String name) {
        return new City(name);
    }
}
```

```java
/**
 * FullAddress Value Object (Composite VO)
 *
 * <p><strong>복합 필드</strong>: VO 안에 다른 VO들을 포함</p>
 * <ul>
 *   <li>{@link ZipCode} - 우편번호 VO</li>
 *   <li>{@link Street} - 주소 VO</li>
 *   <li>{@link City} - 도시 VO</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record FullAddress(
    ZipCode zipCode,  // ← VO
    Street street,    // ← VO
    City city        // ← VO
) {

    /**
     * Compact Constructor (null 체크만)
     */
    public FullAddress {
        if (zipCode == null) {
            throw new IllegalArgumentException("우편번호는 null일 수 없습니다.");
        }
        if (street == null) {
            throw new IllegalArgumentException("주소는 null일 수 없습니다.");
        }
        if (city == null) {
            throw new IllegalArgumentException("도시는 null일 수 없습니다.");
        }
        // 각 VO는 이미 자체 검증을 거쳤으므로 null 체크만 필요
    }

    /**
     * 값 기반 생성
     *
     * @param zipCode 우편번호 VO
     * @param street 주소 VO
     * @param city 도시 VO
     * @return FullAddress
     * @throws IllegalArgumentException null인 VO가 있을 경우
     */
    public static FullAddress of(ZipCode zipCode, Street street, City city) {
        return new FullAddress(zipCode, street, city);
    }

    /**
     * 전체 주소 문자열 반환
     *
     * @return "[우편번호] 도시 주소"
     */
    public String getFullAddress() {
        return String.format("[%s] %s %s", zipCode.value(), city.name(), street.value());
    }
}
```

**Composite VO 사용 예시**:
```java
// 각 VO 개별 생성 (각각 검증됨)
ZipCode zipCode = ZipCode.of("12345");
Street street = Street.of("123 Main St");
City city = City.of("Seoul");

// Composite VO 생성
FullAddress address = FullAddress.of(zipCode, street, city);

// 전체 주소 출력
System.out.println(address.getFullAddress());  // [12345] Seoul 123 Main St
```

---

## 4) Record의 자동 생성 기능

### 4-1) equals/hashCode 자동 생성

Record는 모든 필드를 기반으로 equals/hashCode를 자동 생성합니다.

```java
// Record 선언만으로 자동 생성됨
public record Money(Long amount) {
    // equals/hashCode 자동 생성 ✅
}

// 사용 예시
Money money1 = Money.of(1000L);
Money money2 = Money.of(1000L);
money1.equals(money2);  // true (값 기반 동등성)
```

### 4-2) toString 자동 생성

Record는 모든 필드를 포함한 toString을 자동 생성합니다.

```java
Money money = Money.of(1000L);
System.out.println(money);  // Money[amount=1000]
```

### 4-3) Getter 자동 생성

Record는 필드명과 동일한 Getter를 자동 생성합니다 (get 접두사 없음).

```java
public record OrderId(Long value) {
    // value() 메서드 자동 생성 ✅
}

OrderId id = OrderId.of(100L);
Long value = id.value();  // ✅ value() 사용 (getValue() 아님!)
```

---

## 5) VO 유형 정리

| VO 유형 | 특징 | 예시 |
|---------|------|------|
| **ID VO (Long)** | DB Auto Increment, forNew()=null, isNew() 있음 | OrderId, ProductId |
| **ID VO (UUID)** | UUIDv7 Application 생성, forNew()=UUID, null 금지 | UserId, TraceId |
| **Simple VO (단일 필드)** | 원시 타입 1개 래핑 | Money, Email, PhoneNumber |
| **Multi-field VO** | 여러 원시 타입 조합 | Address (zipCode, street, detail) |
| **Composite VO** | VO 안에 다른 VO들 포함 | FullAddress (ZipCode, Street, City) |

---

## 6) Do / Don't

### ❌ Bad Examples

```java
// ❌ Record 대신 일반 클래스 사용
public class Money {
    private final Long amount;
    // Record 사용해야 함!
}

// ❌ Lombok 사용 (외부 의존성 금지)
@Value  // ❌
public record Money(Long amount) {
}

// ❌ Compact Constructor 없이 검증 생략
public record Money(Long amount) {
    public static Money of(Long amount) {
        return new Money(amount);  // ❌ 검증 없음
    }
}

// ❌ ID VO에 forNew() 없음
public record OrderId(Long value) {
    public static OrderId of(Long value) {
        // null 체크 필수 → forNew() 없으면 신규 생성 불가
        if (value == null) {
            throw new IllegalArgumentException("null 불가");
        }
        return new OrderId(value);
    }
}

// ❌ getValue() 사용 (Record는 value() 사용)
OrderId id = OrderId.of(100L);
Long value = id.getValue();  // ❌ 컴파일 오류!

// ❌ 잘못된 Composite VO (원시 타입을 Composite라고 착각)
public record Address(String zipCode, String street, String detail) {
    // 이건 Multi-field VO (Composite 아님)
}
```

### ✅ Good Examples

```java
// ✅ Record + Compact Constructor + 정적 팩토리
public record Money(Long amount) {

    public Money {  // ✅ Compact Constructor
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
    }

    public static Money of(Long amount) {  // ✅ 정적 팩토리
        return new Money(amount);
    }
}

// ✅ ID VO는 forNew() + of() 모두 제공
public record OrderId(Long value) {

    public OrderId {  // ✅ Compact Constructor
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("OrderId 값은 양수여야 합니다: " + value);
        }
    }

    public static OrderId forNew() {  // ✅ null 허용
        return new OrderId(null);
    }

    public static OrderId of(Long value) {  // ✅ 검증
        if (value == null) {
            throw new IllegalArgumentException("OrderId는 null일 수 없습니다.");
        }
        return new OrderId(value);
    }

    public boolean isNew() {  // ✅ null 체크 헬퍼
        return value == null;
    }
}

// ✅ value() 사용 (Record의 자동 생성 메서드)
OrderId id = OrderId.of(100L);
Long value = id.value();  // ✅ value() 사용

// ✅ 진짜 Composite VO (VO 안에 VO)
public record FullAddress(ZipCode zipCode, Street street, City city) {
    // ✅ 각 필드가 VO 타입
}
```

---

## 7) 체크리스트

Value Object 작성 후 다음을 확인:

### ID VO - Long 타입 (OrderId, ProductId 등)
- [ ] `record` 키워드 사용
- [ ] Compact Constructor에 검증 로직 (양수 체크, null 허용)
- [ ] `forNew()` 메서드 있음 (**null 반환** - DB Auto Increment)
- [ ] `of(Long value)` 메서드 있음 (null 체크 필수)
- [ ] `isNew()` 메서드 있음 (null 여부 확인)
- [ ] 외부 의존성 제로 (Lombok, JPA, Spring 등 절대 금지)

### ID VO - UUID 타입 (UserId, TraceId 등)
- [ ] `record` 키워드 사용
- [ ] Compact Constructor에 UUIDv7 형식 검증 (**null 금지**)
- [ ] `forNew()` 메서드 있음 (**UUIDv7 자동 생성**)
- [ ] `of(String value)` 메서드 있음 (기존 UUID 파싱)
- [ ] `isNew()` 없음 (항상 값 존재)
- [ ] 의존성: `uuid-creator` 라이브러리만 허용

### Simple VO (Money, Email 등)
- [ ] `record` 키워드 사용
- [ ] **단일 필드** (원시 타입 1개)
- [ ] Compact Constructor에 검증 로직 (null 체크, 도메인 규칙)
- [ ] `of(...)` 정적 팩토리 메서드 있음
- [ ] 외부 의존성 제로 (Lombok, JPA, Spring 등 절대 금지)
- [ ] 필요한 경우 연산 메서드 제공 (add, multiply 등)

### Multi-field VO (Address 등)
- [ ] `record` 키워드 사용
- [ ] **여러 원시 타입 필드** (String, Long 등)
- [ ] Compact Constructor에 각 필드 검증
- [ ] `of(...)` 정적 팩토리 메서드 있음
- [ ] 외부 의존성 제로

### Composite VO (FullAddress 등)
- [ ] `record` 키워드 사용
- [ ] **VO 안에 다른 VO들 포함**
- [ ] Compact Constructor에 null 체크 (각 VO는 이미 검증됨)
- [ ] `of(...)` 정적 팩토리 메서드 있음
- [ ] 외부 의존성 제로

---

## 8) Record vs 일반 클래스 비교

| 항목 | Record | 일반 클래스 |
|------|--------|-------------|
| 불변성 | 자동 보장 (final) | 수동 구현 필요 |
| equals/hashCode | 자동 생성 | 수동 구현 필요 |
| toString | 자동 생성 | 수동 구현 필요 |
| Getter | 자동 생성 (value()) | 수동 구현 (getValue()) |
| 생성자 | Compact Constructor | Private 생성자 |
| 검증 | Compact Constructor | 생성자 본문 |
| 코드량 | 짧음 | 길음 |

**✅ Record를 사용하면 100줄 이상의 Boilerplate 코드를 10줄로 줄일 수 있습니다!**

---

## 9) 조회용 공통 VO

Domain Layer의 `common/vo/` 패키지에는 **조회 조건용 공통 VO**들도 포함됩니다.

### 위치

```
domain/common/vo/
├── LockKey.java           # 분산 락 키 인터페이스
├── CacheKey.java          # 캐시 키 인터페이스
├── DateRange.java         # 날짜 범위
├── SortDirection.java     # 정렬 방향 (ASC/DESC)
├── SortKey.java           # 정렬 키 인터페이스
├── PageRequest.java       # 오프셋 기반 페이징
└── CursorPageRequest.java # 커서 기반 페이징
```

### 조회용 VO 요약

| VO | 설명 | 주요 메서드 |
|----|------|-------------|
| `DateRange` | 시작일~종료일 범위 | `of()`, `lastDays()`, `thisMonth()`, `startInstant()`, `endInstant()` |
| `SortDirection` | ASC/DESC 정렬 방향 | `isAscending()`, `reverse()`, `fromString()` |
| `SortKey` | BC별 정렬 키 인터페이스 | `fieldName()` - BC별 enum으로 구현 |
| `PageRequest` | 오프셋 기반 페이징 | `of()`, `offset()`, `totalPages()` |
| `CursorPageRequest` | 커서 기반 페이징 | `of()`, `afterId()`, `fetchSize()` |

**⚠️ 중요**: `DateRange`는 내부적으로 `LocalDate`를 사용하지만, Domain Layer 규칙에 따라 시간 변환 메서드(`startInstant()`, `endInstant()`)는 **`Instant`를 반환**합니다. `LocalDateTime` 사용은 금지됩니다.

### LockKey 구현 (분산 락)

분산 락에 사용되는 키를 정의하는 인터페이스입니다.

```java
// domain/common/vo/LockKey.java (인터페이스)
public interface LockKey {
    String value();
}

// domain/order/vo/OrderLockKey.java (구현체)
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

**키 형식 규칙**:
```
lock:{domain}:{id}
lock:{domain}:{entity}:{id}
lock:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
```

**예시**: `lock:order:123`, `lock:stock:item:456`

### CacheKey 구현 (캐싱)

Redis 캐시에 사용되는 키를 정의하는 인터페이스입니다.

```java
// domain/common/vo/CacheKey.java (인터페이스)
public interface CacheKey {
    String value();
}

// domain/product/vo/ProductCacheKey.java (구현체)
public record ProductCacheKey(Long productId) implements CacheKey {

    private static final String PREFIX = "cache:product:";

    public ProductCacheKey {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
    }

    @Override
    public String value() {
        return PREFIX + productId;
    }
}
```

**키 형식 규칙**:
```
cache:{domain}:{id}
cache:{domain}:{entity}:{id}
cache:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
```

**예시**: `cache:product:123`, `cache:user:profile:456`

### LockKey vs CacheKey 비교

| 구분 | LockKey | CacheKey |
|------|---------|----------|
| 목적 | 분산 락 동시성 제어 | 데이터 캐싱 |
| 키 접두사 | `lock:` | `cache:` |
| TTL 특성 | 락 획득 시간 (짧음) | 캐시 만료 시간 (길음) |
| 사용 Port | `DistributedLockPort` | `CachePort` |

### SortKey 구현 예시

```java
// domain/order/vo/OrderSortKey.java
public enum OrderSortKey implements SortKey {
    ORDER_DATE("orderDate"),
    TOTAL_AMOUNT("totalAmount");

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

### Domain Criteria에서 사용

```java
// domain/order/query/criteria/OrderSearchCriteria.java
public record OrderSearchCriteria(
    Long memberId,
    DateRange orderDateRange,
    OrderSortKey sortKey,
    SortDirection sortDirection,
    PageRequest page
) {}
```

**자세한 내용**: 각 VO 파일의 JavaDoc 참조

---

**✅ Value Object는 도메인의 불변 값입니다. Record를 활용하여 간결하게 구현하세요.**
