# DTO Record Type Rules - ArchUnit으로 Record 패턴 강제

**목적**: ArchUnit을 활용하여 Application Layer의 모든 DTO (Command, Query, Response)가 Record 타입을 사용하도록 강제

**관련 문서**:
- [Command DTO Guide](command/command-dto-guide.md)
- [Query DTO Guide](query/query-dto-guide.md)
- [Response DTO Guide](response/response-dto-guide.md)

**검증 도구**: ArchUnit 1.2.0+

---

## 📌 핵심 원칙

### DTO는 반드시 Record 타입

```
application/{bc}/dto/
├── command/               ← Command (CUD 요청)
│   ├── CreateOrderCommand.java  ← public record
│   ├── UpdateOrderCommand.java  ← public record
│   └── DeleteOrderCommand.java  ← public record
├── query/                 ← Query (Read 요청)
│   ├── GetOrderQuery.java       ← public record
│   └── SearchOrdersQuery.java   ← public record
└── response/              ← Response (응답)
    ├── OrderResponse.java       ← public record
    └── OrderDetailResponse.java ← public record
```

**핵심 규칙**:
1. **Command, Query, Response는 모두 Record 타입**
2. **Lombok 절대 금지** (Record가 자동 생성)
3. **jakarta.validation 금지** (REST API Layer에서 검증)
4. **비즈니스 로직 금지** (데이터 전달만)

---

## ❌ 금지 패턴 (Anti-Patterns)

### Anti-Pattern 1: Class 타입 사용

```java
// ❌ Bad: Command를 Class로 정의
package com.ryuqq.application.order.dto.command;

public class CreateOrderCommand {  // ❌ Record여야 함!
    private Long customerId;
    private BigDecimal amount;

    // getter, setter, constructor...
}
```

**문제점**:
- 불변성 보장 불가
- 보일러플레이트 코드
- Lombok 유혹 증가

**해결**:
```java
// ✅ Good: Record 타입으로 정의
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

### Anti-Pattern 2: Lombok 사용

```java
// ❌ Bad: Lombok 사용
package com.ryuqq.application.order.dto.command;

import lombok.Data;

@Data  // ❌ Record 대신 Lombok
public class CreateOrderCommand {
    private Long customerId;
    private BigDecimal amount;
}
```

**문제점**:
- Lombok 의존성 추가
- Record의 불변성 손실
- 일관성 부족

**해결**:
```java
// ✅ Good: Record 사용
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

### Anti-Pattern 3: jakarta.validation 사용

```java
// ❌ Bad: jakarta.validation 어노테이션
package com.ryuqq.application.order.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record CreateOrderCommand(
    @NotNull Long customerId,  // ❌ jakarta.validation 금지
    @Min(0) BigDecimal amount   // ❌ REST API Layer에서 검증
) {}
```

**문제점**:
- Application Layer가 Jakarta EE 의존
- DTO가 순수 Java 아님
- 검증 책임 혼재

---

### Anti-Pattern 4: 비즈니스 로직 포함

```java
// ❌ Bad: DTO에 비즈니스 로직
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {
    // ❌ 비즈니스 검증
    public CreateOrderCommand {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    // ❌ 비즈니스 메서드
    public BigDecimal calculateTax() {
        return amount.multiply(BigDecimal.valueOf(0.1));
    }
}
```

**문제점**:
- DTO의 책임 과다
- 비즈니스 로직이 여러 곳에 분산
- Domain Layer 역할 침범

---

## ✅ 올바른 패턴 (Best Practices)

### 1. Command Record Pattern

```java
package com.ryuqq.application.order.dto.command;

/**
 * 주문 생성 Command
 *
 * @param customerId 고객 ID
 * @param amount 주문 금액
 * @author development-team
 * @since 1.0.0
 */
public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

### 2. Query Record Pattern

```java
package com.ryuqq.application.order.dto.query;

import java.time.Instant;

/**
 * 주문 검색 Query
 *
 * @param customerId 고객 ID (Optional)
 * @param status 주문 상태 (Optional)
 * @param startDate 시작일 (Optional)
 * @param endDate 종료일 (Optional)
 * @param page 페이지 번호
 * @param size 페이지 크기
 * @author development-team
 * @since 1.0.0
 */
public record SearchOrdersQuery(
    Long customerId,
    String status,
    Instant startDate,
    Instant endDate,
    Integer page,
    Integer size
) {}
```

---

### 3. Response Record Pattern

```java
package com.ryuqq.application.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 주문 응답 Response
 *
 * @param id 주문 ID
 * @param customerId 고객 ID
 * @param amount 주문 금액
 * @param status 주문 상태
 * @param createdAt 생성 시각
 * @author development-team
 * @since 1.0.0
 */
public record OrderResponse(
    Long id,
    Long customerId,
    BigDecimal amount,
    String status,
    Instant createdAt
) {}
```

---

### 4. Nested Record Pattern

```java
package com.ryuqq.application.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 주문 상세 Response (Nested Record)
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderDetailResponse(
    Long id,
    CustomerInfo customer,
    List<LineItem> items,
    BigDecimal totalAmount,
    String status,
    Instant orderedAt
) {
    /**
     * 고객 정보 (Nested Record)
     */
    public record CustomerInfo(
        Long id,
        String name,
        String email
    ) {}

    /**
     * 주문 항목 (Nested Record)
     */
    public record LineItem(
        Long id,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
```

---

## 🧪 ArchUnit 테스트

### 테스트 위치
```
application/src/test/java/com/ryuqq/application/architecture/dto/
└── DtoRecordArchTest.java
```

### 주요 검증 규칙

#### 1. Record 타입 강제
```java
@Test
@DisplayName("[필수] Command는 Record 타입이어야 한다")
void command_MustBeRecord() {
    ArchRule rule = classes()
        .that().resideInAPackage("..dto.command..")
        .and().haveSimpleNameEndingWith("Command")
        .should().beRecords()
        .because("Command는 불변 데이터 전달을 위해 Record 타입을 사용해야 합니다");

    rule.check(classes);
}
```

#### 2. Lombok 금지
```java
@Test
@DisplayName("[금지] DTO는 Lombok 어노테이션을 가지지 않아야 한다")
void dto_MustNotUseLombok() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..dto..")
        .should().beAnnotatedWith("lombok.Data")
        .orShould().beAnnotatedWith("lombok.Builder")
        .because("DTO는 Record 타입을 사용해야 합니다 (Lombok 금지)");

    rule.check(classes);
}
```

#### 3. jakarta.validation 금지
```java
@Test
@DisplayName("[금지] DTO는 jakarta.validation 어노테이션을 가지지 않아야 한다")
void dto_MustNotUseJakartaValidation() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..dto..")
        .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
        .because("DTO는 순수 Java Record를 사용해야 합니다 (jakarta.validation 금지)");

    rule.check(classes);
}
```

#### 4. 비즈니스 메서드 금지
```java
@Test
@DisplayName("[금지] DTO는 비즈니스 메서드를 가지지 않아야 한다")
void dto_MustNotHaveBusinessMethods() {
    ArchRule rule = noMethods()
        .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
        .and().arePublic()
        .and().haveNameMatching("validate.*|calculate.*|process.*")
        .should().beDeclared()
        .because("DTO는 비즈니스 로직을 가질 수 없습니다 (데이터 전달만)");

    rule.check(classes);
}
```

#### 5. 클래스명 규칙
```java
@Test
@DisplayName("[필수] dto/command/ 패키지의 클래스는 'Command' 접미사를 가져야 한다")
void command_MustHaveCorrectSuffix() {
    ArchRule rule = classes()
        .that().resideInAPackage("..dto.command..")
        .and().areNotMemberClasses()
        .should().haveSimpleNameEndingWith("Command")
        .because("Command DTO는 'Command' 접미사를 사용해야 합니다");

    rule.check(classes);
}
```

#### 6. 패키지 위치
```java
@Test
@DisplayName("[필수] Command는 ..application..dto.command.. 패키지에 위치해야 한다")
void command_MustBeInCorrectPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("Command")
        .and().areRecords()
        .should().resideInAPackage("..application..dto.command..")
        .because("Command는 application.*.dto.command 패키지에 위치해야 합니다");

    rule.check(classes);
}
```

---

## 📋 전체 검증 규칙 목록

| 번호 | 규칙 | 설명 | 위반 시 영향 |
|------|------|------|-------------|
| 1 | Command Record 타입 | Command는 Record 타입이어야 함 | 🔴 빌드 실패 |
| 2 | Query Record 타입 | Query는 Record 타입이어야 함 | 🔴 빌드 실패 |
| 3 | Response Record 타입 | Response는 Record 타입이어야 함 | 🔴 빌드 실패 |
| 4 | Command 클래스명 | 'Command' 접미사 필수 | 🔴 빌드 실패 |
| 5 | Query 클래스명 | 'Query' 접미사 필수 | 🔴 빌드 실패 |
| 6 | Response 클래스명 | 'Response' 접미사 필수 | 🔴 빌드 실패 |
| 7 | Lombok 금지 | @Data, @Builder 등 금지 | 🔴 빌드 실패 |
| 8 | jakarta.validation 금지 | @NotNull, @Min 등 금지 | 🔴 빌드 실패 |
| 9 | 비즈니스 메서드 금지 | validate, calculate 등 금지 | 🔴 빌드 실패 |
| 10 | @Transactional 금지 | DTO는 트랜잭션 없음 | 🔴 빌드 실패 |
| 11 | Command 패키지 위치 | dto.command 패키지 필수 | 🔴 빌드 실패 |
| 12 | Query 패키지 위치 | dto.query 패키지 필수 | 🔴 빌드 실패 |
| 13 | Response 패키지 위치 | dto.response 패키지 필수 | 🔴 빌드 실패 |
| 14 | Public 접근 제어 | public 타입이어야 함 | 🔴 빌드 실패 |
| 15 | 비즈니스 Static 메서드 금지 | 생성 메서드 외 금지 | 🔴 빌드 실패 |
| 16 | Domain 객체 반환 금지 | Assembler에서 변환 | 🔴 빌드 실패 |
| 17 | Port 의존성 금지 | DTO는 순수 데이터 | 🔴 빌드 실패 |
| 18 | Repository 의존성 금지 | DTO는 순수 데이터 | 🔴 빌드 실패 |

---

## 🎯 테스트 실행

### Gradle 테스트
```bash
# 전체 ArchUnit 테스트
./gradlew test --tests "*ArchTest"

# DTO Record 테스트만
./gradlew test --tests "*DtoRecordArchTest"

# 특정 테스트 메서드
./gradlew test --tests "*DtoRecordArchTest.command_MustBeRecord"
```

### Maven 테스트
```bash
# 전체 ArchUnit 테스트
mvn test -Dtest="*ArchTest"

# DTO Record 테스트만
mvn test -Dtest="DtoRecordArchTest"
```

---

## 📚 관련 문서

### Application Layer DTO 가이드
- [Command DTO Guide](command/command-dto-guide.md)
- [Query DTO Guide](query/query-dto-guide.md)
- [Response DTO Guide](response/response-dto-guide.md)
- [Assembler Guide](../assembler/assembler-guide.md)

### 다른 ArchUnit 규칙
- [Layer Dependency Rules](../../05-testing/archunit-rules/01_layer-dependency-rules.md)
- [Naming Convention Rules](../../05-testing/archunit-rules/02_naming-convention-rules.md)
- [Annotation Rules](../../05-testing/archunit-rules/03_annotation-rules.md)
- [JPA Entity Rules](../../05-testing/archunit-rules/05_archunit-jpa-entity-rules.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
