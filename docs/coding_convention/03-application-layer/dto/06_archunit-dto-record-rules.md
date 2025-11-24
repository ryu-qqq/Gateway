# DTO Record Type ArchUnit 규칙

> **목적**: Application Layer의 모든 DTO (Command, Query, Response)는 **Record 타입**으로 정의되어야 합니다.

---

## 1️⃣ 핵심 원칙

### DTO는 Record 타입으로 정의

```java
// ✅ Good: Record 타입
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}

// ❌ Bad: Class 타입
package com.ryuqq.application.order.dto.command;

public class CreateOrderCommand {
    private Long customerId;
    private BigDecimal amount;

    // getter/setter...
}
```

### Zero-Tolerance 규칙

- ❌ **Lombok 절대 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter` 등 모두 금지
- ❌ **jakarta.validation 금지**: 검증은 REST API Layer에서 수행
- ❌ **비즈니스 로직 금지**: DTO는 데이터 전달만
- ❌ **Port/Repository 의존성 금지**: 순수 데이터 전달 객체
- ✅ **Record 타입 필수**: 불변성과 간결성 보장

---

## 2️⃣ Anti-Pattern 예시

### Anti-Pattern 1: Class 타입 사용

```java
// ❌ Bad: Class 타입
package com.ryuqq.application.order.dto.command;

public class CreateOrderCommand {
    private Long customerId;
    private BigDecimal amount;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
```

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
// ❌ Bad: jakarta.validation 사용
package com.ryuqq.application.order.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderCommand(
    @NotNull Long customerId,  // ❌ Application Layer에서 검증
    @Positive BigDecimal amount
) {}
```

**해결**:
```java
// ✅ Good: 순수 Java Record (검증은 REST API Layer에서)
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}

// REST API Layer에서 검증
package com.ryuqq.adapter.rest.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
    @NotNull Long customerId,
    @Positive BigDecimal amount
) {}
```

---

### Anti-Pattern 4: 비즈니스 메서드 포함

```java
// ❌ Bad: 비즈니스 로직 포함
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {
    // ❌ 비즈니스 로직은 Domain Layer에!
    public void validate() {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public BigDecimal calculateTax() {
        return amount.multiply(BigDecimal.valueOf(0.1));
    }
}
```

**해결**:
```java
// ✅ Good: 비즈니스 로직 없음 (데이터만)
package com.ryuqq.application.order.dto.command;

public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}

// Domain Layer에서 비즈니스 로직 처리
package com.ryuqq.domain.order;

public class Order {
    public void place() {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Amount must be positive");
        }
    }

    public Money calculateTax() {
        return Money.of(amount.multiply(BigDecimal.valueOf(0.1)));
    }
}
```

---

## 3️⃣ Correct Pattern 예시

### Command DTO (Record)

```java
package com.ryuqq.application.order.dto.command;

/**
 * 주문 생성 커맨드
 *
 * @param customerId 고객 ID
 * @param amount 주문 금액
 *
 * @author development-team
 * @since 1.0.0
 */
public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

### Query DTO (Record)

```java
package com.ryuqq.application.order.dto.query;

import java.time.LocalDate;

/**
 * 주문 조회 쿼리
 *
 * @param customerId 고객 ID (Optional)
 * @param startDate 시작일 (Optional)
 * @param endDate 종료일 (Optional)
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderQuery(
    Long customerId,
    LocalDate startDate,
    LocalDate endDate
) {}
```

### Response DTO (Record)

```java
package com.ryuqq.application.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 응답 DTO
 *
 * @param orderId 주문 ID
 * @param customerId 고객 ID
 * @param amount 주문 금액
 * @param status 주문 상태
 * @param createdAt 생성 시각
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderResponse(
    Long orderId,
    Long customerId,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt
) {}
```

### Nested Record (복잡한 구조)

```java
package com.ryuqq.application.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 응답 DTO
 *
 * @param orderId 주문 ID
 * @param customer 고객 정보
 * @param items 주문 항목 리스트
 * @param totalAmount 총 금액
 * @param createdAt 생성 시각
 *
 * @author development-team
 * @since 1.0.0
 */
public record OrderDetailResponse(
    Long orderId,
    CustomerInfo customer,
    List<OrderItem> items,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {
    /**
     * 고객 정보
     */
    public record CustomerInfo(
        Long customerId,
        String name,
        String email
    ) {}

    /**
     * 주문 항목
     */
    public record OrderItem(
        Long productId,
        String productName,
        int quantity,
        BigDecimal price
    ) {}
}
```

---

## 4️⃣ ArchUnit 테스트 코드

### 위치
```
application/src/test/java/com/ryuqq/application/architecture/dto/DtoRecordArchTest.java
```

### 테스트 코드

```java
package com.ryuqq.application.architecture.dto;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * DTO Record Type ArchUnit 검증 테스트 (Zero-Tolerance)
 *
 * <p>모든 DTO (Command, Query, Response)는 정확히 이 규칙을 따라야 합니다:</p>
 * <ul>
 *   <li>Command: dto/command/ 패키지에 Record 타입으로 정의</li>
 *   <li>Query: dto/query/ 패키지에 Record 타입으로 정의</li>
 *   <li>Response: dto/response/ 패키지에 Record 타입으로 정의</li>
 *   <li>Lombok 절대 금지 (Record 사용)</li>
 *   <li>jakarta.validation 의존성 금지 (순수 Java Record)</li>
 *   <li>비즈니스 로직 금지 (데이터 전달만)</li>
 *   <li>Port/Repository 의존성 금지 (순수 데이터 전달 객체)</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("DTO Record Type ArchUnit Tests (Zero-Tolerance)")
@Tag("architecture")
class DtoRecordArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.application");
    }

    /**
     * 규칙 1: Command는 Record 타입이어야 함
     */
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

    /**
     * 규칙 2: Query는 Record 타입이어야 함
     */
    @Test
    @DisplayName("[필수] Query는 Record 타입이어야 한다")
    void query_MustBeRecord() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().haveSimpleNameEndingWith("Query")
            .should().beRecords()
            .because("Query는 불변 조회 조건 전달을 위해 Record 타입을 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: Response는 Record 타입이어야 함
     */
    @Test
    @DisplayName("[필수] Response는 Record 타입이어야 한다")
    void response_MustBeRecord() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().haveSimpleNameEndingWith("Response")
            .should().beRecords()
            .because("Response는 불변 응답 데이터 전달을 위해 Record 타입을 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: Command 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/command/ 패키지의 클래스는 'Command' 접미사를 가져야 한다")
    void command_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.command..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Command")
            .because("Command DTO는 'Command' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 5: Query 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/query/ 패키지의 클래스는 'Query' 접미사를 가져야 한다")
    void query_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.query..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Query")
            .because("Query DTO는 'Query' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 6: Response 클래스명 규칙
     */
    @Test
    @DisplayName("[필수] dto/response/ 패키지의 클래스는 'Response' 접미사를 가져야 한다")
    void response_MustHaveCorrectSuffix() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto.response..")
            .and().areNotMemberClasses()  // Nested 클래스 제외
            .should().haveSimpleNameEndingWith("Response")
            .because("Response DTO는 'Response' 접미사를 사용해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 7: Lombok 절대 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Lombok 어노테이션을 가지지 않아야 한다")
    void dto_MustNotUseLombok() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Builder")
            .orShould().beAnnotatedWith("lombok.Getter")
            .orShould().beAnnotatedWith("lombok.Setter")
            .orShould().beAnnotatedWith("lombok.AllArgsConstructor")
            .orShould().beAnnotatedWith("lombok.NoArgsConstructor")
            .orShould().beAnnotatedWith("lombok.RequiredArgsConstructor")
            .orShould().beAnnotatedWith("lombok.Value")
            .because("DTO는 Record 타입을 사용해야 합니다 (Lombok 금지)");

        rule.check(classes);
    }

    /**
     * 규칙 8: jakarta.validation 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 jakarta.validation 어노테이션을 가지지 않아야 한다")
    void dto_MustNotUseJakartaValidation() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("DTO는 순수 Java Record를 사용해야 합니다 (jakarta.validation 금지, REST API Layer에서 검증)");

        rule.check(classes);
    }

    /**
     * 규칙 9: 비즈니스 메서드 금지
     */
    @Test
    @DisplayName("[금지] DTO는 비즈니스 메서드를 가지지 않아야 한다")
    void dto_MustNotHaveBusinessMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().arePublic()
            .and().haveNameMatching("validate.*|place.*|confirm.*|cancel.*|approve.*|reject.*|modify.*|change.*|update.*|delete.*|save.*|persist.*")
            .should().beDeclared()
            .because("DTO는 비즈니스 로직을 가질 수 없습니다 (데이터 전달만)");

        rule.check(classes);
    }

    /**
     * 규칙 10: Command 패키지 위치
     */
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

    /**
     * 규칙 11: Query 패키지 위치
     */
    @Test
    @DisplayName("[필수] Query는 ..application..dto.query.. 패키지에 위치해야 한다")
    void query_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Query")
            .and().areRecords()
            .should().resideInAPackage("..application..dto.query..")
            .because("Query는 application.*.dto.query 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 12: Response 패키지 위치
     */
    @Test
    @DisplayName("[필수] Response는 ..application..dto.response.. 패키지에 위치해야 한다")
    void response_MustBeInCorrectPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Response")
            .and().areRecords()
            .should().resideInAPackage("..application..dto.response..")
            .because("Response는 application.*.dto.response 패키지에 위치해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 13: Public 접근 제어
     */
    @Test
    @DisplayName("[필수] DTO는 public 타입이어야 한다")
    void dto_MustBePublic() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto..")
            .and().areRecords()
            .should().bePublic()
            .because("DTO는 계층 간 데이터 전달을 위해 public이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 14: Static 메서드 금지 (생성 메서드 제외)
     */
    @Test
    @DisplayName("[금지] DTO는 비즈니스 로직 static 메서드를 가지지 않아야 한다")
    void dto_MustNotHaveBusinessStaticMethods() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().areStatic()
            .and().arePublic()
            .and().doNotHaveName("of")  // Record 생성 메서드 허용
            .and().doNotHaveName("from")  // Record 생성 메서드 허용
            .and().haveNameMatching("validate.*|process.*|calculate.*")
            .should().beDeclared()
            .because("DTO는 비즈니스 로직을 가질 수 없습니다 (생성 메서드 of/from만 허용)");

        rule.check(classes);
    }

    /**
     * 규칙 15: Domain 객체 반환 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Domain 객체를 반환하는 메서드를 가지지 않아야 한다")
    void dto_MustNotReturnDomainObjects() {
        ArchRule rule = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..dto..")
            .and().arePublic()
            .should().haveRawReturnType("com.ryuqq.domain..")
            .because("DTO에서 Domain 변환은 Assembler에서 처리해야 합니다 (DTO는 데이터만)");

        rule.check(classes);
    }

    /**
     * 규칙 16: Port 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Port 인터페이스를 의존하지 않아야 한다")
    void dto_MustNotDependOnPorts() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().haveNameMatching(".*Port")
            .because("DTO는 Port를 의존할 수 없습니다 (순수 데이터 전달 객체)");

        rule.check(classes);
    }

    /**
     * 규칙 17: Repository 의존성 금지
     */
    @Test
    @DisplayName("[금지] DTO는 Repository를 의존하지 않아야 한다")
    void dto_MustNotDependOnRepositories() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().haveNameMatching(".*Repository")
            .because("DTO는 Repository를 의존할 수 없습니다 (순수 데이터 전달 객체)");

        rule.check(classes);
    }
}
```

---

## 5️⃣ ArchUnit 규칙 위반 예시

### 위반 1: Class 타입 사용

**에러 메시지**:
```
Architecture Violation [Priority: MEDIUM] -
Rule 'classes that reside in a package '..dto.command..'
and have simple name ending with 'Command'
should be records,
because Command는 불변 데이터 전달을 위해 Record 타입을 사용해야 합니다'
was violated (1 times):
Class <com.ryuqq.application.order.dto.command.CreateOrderCommand> is not a record in (CreateOrderCommand.java:0)
```

**수정**:
```java
// ❌ Before: Class 타입
public class CreateOrderCommand {
    private Long customerId;
    private BigDecimal amount;
}

// ✅ After: Record 타입
public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

### 위반 2: Lombok 사용

**에러 메시지**:
```
Architecture Violation [Priority: HIGH] -
Rule 'no classes that reside in a package '..dto..'
should be annotated with @Data
or should be annotated with @Builder
... (중략)
because DTO는 Record 타입을 사용해야 합니다 (Lombok 금지)'
was violated (1 times):
Class <com.ryuqq.application.order.dto.command.CreateOrderCommand> is annotated with @Data in (CreateOrderCommand.java:5)
```

**수정**:
```java
// ❌ Before: Lombok 사용
import lombok.Data;

@Data
public class CreateOrderCommand {
    private Long customerId;
    private BigDecimal amount;
}

// ✅ After: Record 사용
public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

### 위반 3: jakarta.validation 사용

**에러 메시지**:
```
Architecture Violation [Priority: MEDIUM] -
Rule 'no classes that reside in a package '..dto..'
should depend on classes that reside in a package 'jakarta.validation..',
because DTO는 순수 Java Record를 사용해야 합니다 (jakarta.validation 금지, REST API Layer에서 검증)'
was violated (1 times):
Field <com.ryuqq.application.order.dto.command.CreateOrderCommand.customerId> has type <jakarta.validation.constraints.NotNull> in (CreateOrderCommand.java:7)
```

**수정**:
```java
// ❌ Before: jakarta.validation 사용
import jakarta.validation.constraints.NotNull;

public record CreateOrderCommand(
    @NotNull Long customerId,
    BigDecimal amount
) {}

// ✅ After: 순수 Java Record (검증은 REST API Layer에서)
public record CreateOrderCommand(
    Long customerId,
    BigDecimal amount
) {}
```

---

## 6️⃣ 검증 방법

### Gradle로 ArchUnit 테스트 실행

```bash
# DTO Record 규칙만 검증
./gradlew test --tests DtoRecordArchTest

# 전체 ArchUnit 테스트
./gradlew test --tests *ArchTest

# 빌드 시 자동 검증 (CI/CD)
./gradlew build  # ArchUnit 테스트가 자동으로 실행됨
```

### IDE에서 실행
- IntelliJ IDEA: `DtoRecordArchTest` 클래스 우클릭 → Run
- Eclipse: `DtoRecordArchTest` 클래스 우클릭 → Run As → JUnit Test

---

## 7️⃣ 규칙 요약표

| 번호 | 규칙 | 위반 시 빌드 실패 | 심각도 |
|------|------|------------------|--------|
| 1 | Command는 Record 타입 | ✅ | HIGH |
| 2 | Query는 Record 타입 | ✅ | HIGH |
| 3 | Response는 Record 타입 | ✅ | HIGH |
| 4 | Command 접미사 규칙 | ✅ | MEDIUM |
| 5 | Query 접미사 규칙 | ✅ | MEDIUM |
| 6 | Response 접미사 규칙 | ✅ | MEDIUM |
| 7 | Lombok 절대 금지 | ✅ | HIGH |
| 8 | jakarta.validation 금지 | ✅ | MEDIUM |
| 9 | 비즈니스 메서드 금지 | ✅ | HIGH |
| 10 | Command 패키지 위치 | ✅ | MEDIUM |
| 11 | Query 패키지 위치 | ✅ | MEDIUM |
| 12 | Response 패키지 위치 | ✅ | MEDIUM |
| 13 | Public 접근 제어 | ✅ | LOW |
| 14 | 비즈니스 static 메서드 금지 | ✅ | MEDIUM |
| 15 | Domain 객체 반환 금지 | ✅ | MEDIUM |
| 16 | Port 의존성 금지 | ✅ | HIGH |
| 17 | Repository 의존성 금지 | ✅ | HIGH |

**⚠️ Zero-Tolerance**: 모든 규칙 위반 시 빌드가 실패합니다!

---

## 8️⃣ 체크리스트

DTO 작성 시 확인 사항:
- [ ] Command/Query/Response 모두 **Record 타입**으로 정의
- [ ] Lombok 어노테이션 **절대 사용하지 않음**
- [ ] jakarta.validation 어노테이션 **사용하지 않음** (REST API Layer에서 검증)
- [ ] 비즈니스 로직 **포함하지 않음** (데이터 전달만)
- [ ] **public 접근 제어** 사용
- [ ] 올바른 패키지에 위치 (`dto/command/`, `dto/query/`, `dto/response/`)
- [ ] 올바른 접미사 사용 (`*Command`, `*Query`, `*Response`)
- [ ] Port/Repository 의존성 없음
- [ ] Javadoc 포함 (필드 설명)

---

## 9️⃣ 관련 문서

- **[Command Pattern Guide](../dto-patterns/01_command-pattern.md)** - Command DTO 상세 가이드
- **[Query Pattern Guide](../dto-patterns/02_query-pattern.md)** - Query DTO 상세 가이드
- **[Response Pattern Guide](../dto-patterns/03_response-pattern.md)** - Response DTO 상세 가이드
- **[Assembler Guide](../assembler/assembler-guide.md)** - DTO ↔ Domain 변환 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
