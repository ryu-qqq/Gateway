# DTO Record ArchUnit Guide

> Application Layer DTO (Command, Query, Response)의 **ArchUnit 검증 규칙**
>
> 📌 **핵심**: DTO는 Record 타입, 순수 Java, 비즈니스 로직 금지

---

## 1) 핵심 원칙

### DTO = 순수한 불변 데이터 전달 객체

| 규칙 | 설명 |
|-----|------|
| **Record 타입 필수** | Command, Query, Response 모두 `public record` |
| **순수 Java** | Lombok, jakarta.validation 금지 |
| **비즈니스 로직 금지** | 데이터 전달만, 검증/계산 로직 없음 |
| **의존성 제로** | Port, Repository 의존 금지 |

---

## 2) 테스트 위치

```
application/src/test/java/com/ryuqq/application/architecture/dto/
└── DtoRecordArchTest.java  ← 17개 규칙, 통합 테스트
```

---

## 3) 검증 규칙 (17개)

### Command 규칙 (3개)

| 규칙 | 설명 |
|-----|------|
| Record 타입 | `dto.command` 패키지의 `*Command`는 Record |
| 접미사 | `*Command` 접미사 필수 |
| 패키지 위치 | `..application..dto.command..` 패키지 |

### Query 규칙 (3개)

| 규칙 | 설명 |
|-----|------|
| Record 타입 | `dto.query` 패키지의 `*Query`는 Record |
| 접미사 | `*Query` 접미사 필수 |
| 패키지 위치 | `..application..dto.query..` 패키지 |

### Response 규칙 (3개)

| 규칙 | 설명 |
|-----|------|
| Record 타입 | `dto.response` 패키지의 `*Response`는 Record |
| 접미사 | `*Response` 접미사 필수 |
| 패키지 위치 | `..application..dto.response..` 패키지 |

### 금지 규칙 (4개)

| 규칙 | 설명 |
|-----|------|
| Lombok 금지 | `@Data`, `@Builder` 등 Lombok 어노테이션 금지 |
| jakarta.validation 금지 | `@NotNull`, `@Min` 등 검증 어노테이션 금지 |
| @Transactional 금지 | DTO에서 트랜잭션 금지 |
| 비즈니스 메서드 금지 | `validate*`, `calculate*` 등 금지 |

### 의존성 규칙 (3개)

| 규칙 | 설명 |
|-----|------|
| Port 의존 금지 | `*Port` 인터페이스 의존 금지 |
| Repository 의존 금지 | `*Repository` 의존 금지 |
| Domain 반환 금지 | Domain 객체를 반환하는 메서드 금지 |

> ℹ️ **Bundle 예외**: `application..dto.bundle..` 패키지는 Facade ↔ DomainFactory 사이에서 Domain 객체 묶음을 전달해야 하므로 ArchUnit의 "Domain 반환 금지" 검증 대상에서 제외됩니다. (자세한 내용은 Bundle Guide 참고)

### 기본 구조 규칙 (1개)

| 규칙 | 설명 |
|-----|------|
| Public 접근 제어 | DTO는 `public` 타입 필수 |

---

## 4) 클래스 존재 여부 체크

테스트는 해당 DTO 클래스가 존재하지 않으면 **자동 스킵(SKIPPED)**됩니다:

> **`assumeTrue()` vs `return;`**: SKIP 방식을 사용하면 테스트 리포트에서 "스킵됨"으로 명확히 표시되어 개발자가 왜 테스트가 실행되지 않았는지 파악할 수 있습니다.

```java
private static boolean hasCommandClasses;
private static boolean hasQueryClasses;
private static boolean hasResponseClasses;
private static boolean hasDtoClasses;

@BeforeAll
static void setUp() {
    classes = new ClassFileImporter()
        .importPackages("com.ryuqq.application");

    hasCommandClasses = classes.stream()
        .anyMatch(javaClass -> javaClass.getPackageName().contains(".dto.command")
            && javaClass.getSimpleName().endsWith("Command"));

    hasQueryClasses = classes.stream()
        .anyMatch(javaClass -> javaClass.getPackageName().contains(".dto.query")
            && javaClass.getSimpleName().endsWith("Query"));

    hasResponseClasses = classes.stream()
        .anyMatch(javaClass -> javaClass.getPackageName().contains(".dto.response")
            && javaClass.getSimpleName().endsWith("Response"));

    hasDtoClasses = hasCommandClasses || hasQueryClasses || hasResponseClasses;
}

@Test
void command_MustBeRecord() {
    assumeTrue(hasCommandClasses, "Command 클래스가 없어 테스트를 스킵합니다");
    // ... 실제 테스트 로직
}
```

---

## 5) 테스트 실행

```bash
# DTO Record 테스트만
./gradlew test --tests "*DtoRecordArchTest"

# 전체 ArchUnit 테스트
./gradlew test --tests "*ArchTest"

# 특정 테스트
./gradlew test --tests "*DtoRecordArchTest.command_MustBeRecord"
```

---

## 6) 위반 예시 및 해결

### 위반 1: Class 타입 사용

```java
// ❌ Bad
public class CreateOrderCommand {
    private Long customerId;
}

// ✅ Good
public record CreateOrderCommand(
    Long customerId
) {}
```

### 위반 2: Lombok 사용

```java
// ❌ Bad
@Data
public class CreateOrderCommand { ... }

// ✅ Good
public record CreateOrderCommand(...) {}
```

### 위반 3: jakarta.validation 사용

```java
// ❌ Bad (Application Layer DTO)
public record CreateOrderCommand(
    @NotNull Long customerId
) {}

// ✅ Good (순수 Record, 검증은 REST API Layer에서)
public record CreateOrderCommand(
    Long customerId
) {}
```

---

## 7) 관련 문서

- [Command DTO Guide](command/command-dto-guide.md)
- [Query DTO Guide](query/query-dto-guide.md)
- [Response DTO Guide](response/response-dto-guide.md)
- [Assembler ArchUnit Guide](../assembler/assembler-archunit.md)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 2.0.0 (통합 및 간소화)
