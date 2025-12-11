# UseCase ArchUnit 통합 가이드

> **목적**: Command UseCase와 Query UseCase의 구조 규칙을 ArchUnit으로 자동 검증 (Zero-Tolerance)
>
> 📌 **테스트 위치**: `application/src/test/java/.../architecture/port/in/UseCaseArchTest.java`

---

## 1) 핵심 원칙

### UseCase = Primary Port (Inbound Port)

| 원칙 | 설명 |
|-----|------|
| **Interface 필수** | UseCase는 인터페이스로 선언, 구현체는 Service |
| **execute() 메서드** | 모든 UseCase는 `execute()` 메서드 필수 |
| **@Transactional 금지** | 인터페이스에 금지, Service 구현체에만 적용 |
| **Domain 노출 금지** | Response DTO로 변환하여 반환 |
| **DTO 패키지 분리** | Command, Query, Response는 별도 dto 패키지 |

---

## 2) 테스트 구조

```
UseCaseArchTest.java (18개 테스트, 6개 그룹)
├── @Nested BasicStructureRules      (4개) - 공통 기본 구조
├── @Nested CommandUseCaseRules      (2개) - Command 전용
├── @Nested QueryUseCaseRules        (2개) - Query 전용
├── @Nested ProhibitionRules         (4개) - 금지 규칙
├── @Nested DependencyRules          (3개) - 의존성 규칙
└── @Nested DtoPackageRules          (3개) - DTO 패키지 분리
```

---

## 3) 검증 규칙 (18개)

### 기본 구조 규칙 (4개) - 필수

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| `*UseCase` 접미사 | `port.in` 패키지의 인터페이스는 UseCase 접미사 | 빌드 실패 |
| Interface 필수 | UseCase는 Interface로 선언 | 빌드 실패 |
| Public 필수 | 외부 접근 가능해야 함 | 빌드 실패 |
| execute() 메서드 | 모든 UseCase는 execute() 메서드 필수 | 빌드 실패 |

### Command UseCase 규칙 (2개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| 패키지 위치 | `..port.in.command..` 패키지 | 빌드 실패 |
| 네이밍 패턴 | Create/Update/Delete 등 동사 prefix | ⚠️ **경고만** |

### Query UseCase 규칙 (2개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| 패키지 위치 | `..port.in.query..` 패키지 | 빌드 실패 |
| 네이밍 패턴 | Get/Search/Find 등 동사 prefix | ⚠️ **경고만** |

### 금지 규칙 (4개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| @Transactional 금지 | Interface에 @Transactional 금지 | 빌드 실패 |
| Domain 반환 금지 | Domain Entity 직접 반환 금지 | 빌드 실패 |
| 내부 Record 금지 | UseCase 내부에 Command/Query/Response 정의 금지 | 빌드 실패 |
| JPA Entity 반환 금지 | Persistence Layer 반환 금지 | 빌드 실패 |

### 의존성 규칙 (3개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| Domain/DTO만 의존 | Domain Layer와 DTO만 의존 허용 | 빌드 실패 |
| Persistence 의존 금지 | Persistence Layer 직접 의존 금지 | 빌드 실패 |
| REST API 의존 금지 | REST API Layer 의존 금지 | 빌드 실패 |

### DTO 패키지 분리 규칙 (3개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| Command 위치 | `dto.command` 패키지 | 빌드 실패 |
| Query 위치 | `dto.query` 패키지 | 빌드 실패 |
| Response 위치 | `dto.response` 패키지 | 빌드 실패 |

---

## 4) 클래스 존재 여부 체크

테스트는 해당 클래스가 존재하지 않으면 **자동 스킵(SKIPPED)**됩니다:

> **`assumeTrue()` 패턴**: 클래스가 없으면 테스트 리포트에서 "스킵됨"으로 명확히 표시

```java
@BeforeAll
static void setUp() {
    hasUseCaseClasses = classes.stream()
        .anyMatch(javaClass -> javaClass.getPackageName().contains(".port.in")
            && javaClass.getSimpleName().endsWith("UseCase")
            && javaClass.isInterface());
}

@Test
void useCase_MustBeInterface() {
    assumeTrue(hasUseCaseClasses, "UseCase 클래스가 없어 테스트를 스킵합니다");
    // ... 실제 테스트 로직
}
```

---

## 5) 네이밍 패턴 (권장, 경고만)

### Command UseCase 권장 Prefix

```
Create, Update, Delete, Place, Cancel, Confirm,
Register, Remove, Modify, Approve, Reject, Send, Process, Execute
```

### Query UseCase 권장 Prefix

```
Get, Search, Find, List, Retrieve, Fetch, Query, Count, Check, Exists
```

**중요**: 네이밍 패턴은 **업계 표준 강제 규칙이 아닙니다**. 위반 시 경고만 출력되고 빌드는 성공합니다.

```java
// 권장 사항 - 경고만 출력, 빌드 실패 아님
if (violationCount > 0) {
    System.out.println("[WARNING] " + violationCount + "개의 UseCase가 권장 네이밍 패턴을 따르지 않습니다.");
}
// 테스트는 통과 (권장 사항)
```

---

## 6) 테스트 실행

```bash
# UseCase ArchUnit 테스트 실행
./gradlew :application:test --tests "*UseCaseArchTest"

# 특정 규칙 그룹만 실행
./gradlew :application:test --tests "*UseCaseArchTest\$BasicStructureRules"
./gradlew :application:test --tests "*UseCaseArchTest\$CommandUseCaseRules"
./gradlew :application:test --tests "*UseCaseArchTest\$QueryUseCaseRules"

# 전체 ArchUnit 테스트
./gradlew :application:test --tests "*ArchTest"
```

---

## 7) 위반 예시 및 해결

### 위반 1: Class로 UseCase 정의

```java
// ❌ Bad - Class로 정의
public class PlaceOrderUseCase {
    public OrderResponse execute(PlaceOrderCommand command) { ... }
}

// ✅ Good - Interface로 정의
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}
```

### 위반 2: Interface에 @Transactional

```java
// ❌ Bad - Interface에 @Transactional
@Transactional
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}

// ✅ Good - Service 구현체에만 @Transactional
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}

@Service
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {
    @Override
    public OrderResponse execute(PlaceOrderCommand command) { ... }
}
```

### 위반 3: Domain 직접 반환

```java
// ❌ Bad - Domain Entity 직접 반환
public interface GetOrderUseCase {
    Order execute(GetOrderQuery query);  // Domain 노출!
}

// ✅ Good - Response DTO 반환
public interface GetOrderUseCase {
    OrderDetailResponse execute(GetOrderQuery query);
}
```

### 위반 4: 내부 Record 정의

```java
// ❌ Bad - UseCase 내부에 Record 정의
public interface PlaceOrderUseCase {
    Response execute(Command command);

    record Command(...) {}    // 금지!
    record Response(...) {}   // 금지!
}

// ✅ Good - 별도 DTO 패키지
// dto/command/PlaceOrderCommand.java
public record PlaceOrderCommand(...) {}

// dto/response/OrderResponse.java
public record OrderResponse(...) {}

// port/in/command/PlaceOrderUseCase.java
public interface PlaceOrderUseCase {
    OrderResponse execute(PlaceOrderCommand command);
}
```

---

## 8) 관련 문서

- **[Command UseCase Guide](command/command-usecase-guide.md)** - Command UseCase 구현 상세
- **[Query UseCase Guide](query/query-usecase-guide.md)** - Query UseCase 구현 상세
- **[DTO Record ArchUnit](../dto/dto-record-archunit.md)** - DTO Record 검증 규칙
- **[Assembler ArchUnit](../assembler/assembler-archunit.md)** - Assembler 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 3.0.0 (통합 ArchUnit 가이드)
