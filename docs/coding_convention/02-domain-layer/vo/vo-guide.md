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

### ID VO (OrderId, CustomerId 등)

| 메서드 | 값 전달 | null 체크 | 용도 |
|--------|---------|-----------|------|
| `forNew()` | ❌ null | - | 신규 생성 (Auto Increment 대비) |
| `of(Long value)` | ✅ 필수 | ✅ 필수 | 값 기반 생성 |

### 일반 VO (Money, Email 등)

| 메서드 | 값 전달 | null 체크 | 용도 |
|--------|---------|-----------|------|
| `of(...)` | ✅ 필수 | ✅ 필수 | 값 기반 생성 |

---

## 3) VO 유형별 템플릿

### 3-1) ID VO (OrderId, CustomerId 등)

**특징**:
- Record로 구현
- Long 타입 래핑
- `forNew()` 제공 (null 허용, Auto Increment 대비)
- `of(Long value)` 제공 (null 체크 필수)
- Compact Constructor에서 검증

```java
/**
 * Order ID Value Object
 *
 * <p><strong>생성 패턴</strong>:</p>
 * <ul>
 *   <li>{@code forNew()} - 신규 생성 (ID = null, Auto Increment 대비)</li>
 *   <li>{@code of(Long value)} - 값 기반 생성 (null 체크 필수)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderId(Long value) {

    /**
     * Compact Constructor (검증 로직)
     *
     * <p>주의: forNew()로 생성 시 null 허용</p>
     */
    public OrderId {
        // of()로 생성 시에만 검증 (forNew()는 null 허용)
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("OrderId 값은 양수여야 합니다: " + value);
        }
    }

    /**
     * 신규 생성 (Auto Increment 대비)
     *
     * <p>ID가 null인 상태로 생성. DB에서 Auto Increment로 할당될 예정.</p>
     *
     * @return OrderId (value = null)
     */
    public static OrderId forNew() {
        return new OrderId(null);
    }

    /**
     * 값 기반 생성
     *
     * @param value ID 값 (null 불가)
     * @return OrderId
     * @throws IllegalArgumentException value가 null이거나 음수인 경우
     */
    public static OrderId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderId 값은 null일 수 없습니다.");
        }
        return new OrderId(value);
    }

    /**
     * null 여부 확인
     *
     * @return ID가 null이면 true
     */
    public boolean isNew() {
        return value == null;
    }
}
```

---

### 3-2) Simple VO - 단일 필드 (Money, Quantity 등)

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

### 3-3) Simple VO - 단일 필드 (Email, PhoneNumber 등)

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

### 3-4) Multi-field VO - 여러 원시 타입 필드 (Address 등)

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

### 3-5) Composite VO - VO 안에 VO (FullAddress 등)

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
| **ID VO** | Long 래핑, forNew() + of() + isNew() | OrderId, CustomerId |
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

### ID VO (OrderId, CustomerId 등)
- [ ] `record` 키워드 사용
- [ ] Compact Constructor에 검증 로직 (양수 체크, null 허용)
- [ ] `forNew()` 메서드 있음 (null 허용)
- [ ] `of(Long value)` 메서드 있음 (null 체크 필수)
- [ ] `isNew()` 메서드 있음 (null 여부 확인)
- [ ] 외부 의존성 제로 (Lombok, JPA, Spring 등 절대 금지)

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

**✅ Value Object는 도메인의 불변 값입니다. Record를 활용하여 간결하게 구현하세요.**
