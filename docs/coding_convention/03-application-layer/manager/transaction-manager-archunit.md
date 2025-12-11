# TransactionManager ArchUnit 가이드

> **목적**: TransactionManager의 구조 규칙을 ArchUnit으로 자동 검증 (Zero-Tolerance)
>
> 📌 **테스트 위치**: `application/src/test/java/.../architecture/manager/TransactionManagerArchTest.java`

---

## 1) 핵심 원칙

### TransactionManager = 트랜잭션 경계 제공자

| 원칙 | 설명 |
|-----|------|
| **Class 필수** | TransactionManager는 클래스로 선언 (구현체) |
| **persist() 메서드만** | 순수 위임만, 비즈니스 로직 금지 |
| **@Component 필수** | Spring Bean으로 등록 |
| **@Transactional 필수** | 트랜잭션 경계 정의 |
| **단일 Out Port** | PersistencePort, QueryPort, LockQueryPort 중 하나만 의존 |
| **Lombok 금지** | Plain Java로 작성 |

---

## 2) 테스트 구조

```
TransactionManagerArchTest.java (17개 테스트, 6개 그룹)
├── @Nested BasicStructureRules      (4개) - 기본 구조 규칙
├── @Nested AnnotationRules          (2개) - 어노테이션 규칙
├── @Nested DependencyRules          (2개) - 의존성 규칙
├── @Nested MethodRules              (2개) - 메서드 규칙
├── @Nested ProhibitionRules         (6개) - 금지 규칙
└── @Nested ConstructorRules         (1개) - 생성자 규칙
```

---

## 3) 검증 규칙 (17개)

### 기본 구조 규칙 (4개) - 필수

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| `*TransactionManager` 접미사 | TransactionManager 접미사 필수 | 빌드 실패 |
| `..manager..` 패키지 | manager 패키지에 위치 | 빌드 실패 |
| Class 필수 | Interface가 아닌 Class여야 함 | 빌드 실패 |
| Public 필수 | 외부에서 접근 가능해야 함 | 빌드 실패 |

### 어노테이션 규칙 (2개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| @Component 필수 | Spring Bean 등록 | 빌드 실패 |
| @Transactional 필수 | 트랜잭션 경계 정의 | 빌드 실패 |

### 의존성 규칙 (2개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| Out Port만 의존 | PersistencePort, QueryPort, LockQueryPort만 허용 | 빌드 실패 |
| 단일 Out Port | 하나의 Port만 의존 (여러 개 금지) | 빌드 실패 |

### 메서드 규칙 (2개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| persist() 메서드만 | persist() 외 다른 public 메서드 금지 | 빌드 실패 |
| persist() 필수 | 최소 하나의 persist() 메서드 필수 | 빌드 실패 |

### 금지 규칙 (6개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| Lombok 금지 | Lombok 어노테이션 사용 금지 | 빌드 실패 |
| 필드 주입 금지 | @Autowired 필드 주입 금지 | 빌드 실패 |
| TransactionManager 의존 금지 | 다른 TransactionManager 의존 금지 | 빌드 실패 |
| UseCase 의존 금지 | UseCase 의존 금지 | 빌드 실패 |
| Service 의존 금지 | Service 의존 금지 | 빌드 실패 |
| Facade 의존 금지 | Facade 의존 금지 | 빌드 실패 |

### 생성자 규칙 (1개)

| 규칙 | 설명 | 위반 시 |
|------|-----|--------|
| 생성자 주입 필수 | Out Port를 생성자로 주입 | 빌드 실패 |

---

## 4) 클래스 존재 여부 체크

테스트는 해당 클래스가 존재하지 않으면 **자동 스킵(SKIPPED)**됩니다:

> **`assumeTrue()` 패턴**: 클래스가 없으면 테스트 리포트에서 "스킵됨"으로 명확히 표시

```java
@BeforeAll
static void setUp() {
    transactionManagerClasses = classes.stream()
        .filter(javaClass -> javaClass.getSimpleName().endsWith("TransactionManager"))
        .filter(javaClass -> javaClass.getPackageName().contains(".manager"))
        .filter(javaClass -> !javaClass.isInterface())
        .collect(Collectors.toList());

    hasTransactionManagerClasses = !transactionManagerClasses.isEmpty();
}

@Test
void transactionManager_MustHavePersistMethod() {
    assumeTrue(hasTransactionManagerClasses, "TransactionManager 클래스가 없어 테스트를 스킵합니다");
    // ... 실제 테스트 로직
}
```

---

## 5) persist() 메서드만 허용하는 이유

### 왜 persist()만?

| 관점 | 설명 |
|------|-----|
| **순수 위임** | TransactionManager는 Out Port 호출만 담당 |
| **비즈니스 로직 금지** | 비즈니스 로직은 UseCase에서 처리 |
| **네이밍 일관성** | Out Port의 persist()와 동일한 네이밍 |
| **Soft Delete 지원** | delete 대신 상태 변경 후 persist() |

### 비즈니스 로직은 어디에?

```java
// ❌ Bad - TransactionManager에서 비즈니스 로직
@Component
@Transactional
public class OrderTransactionManager {
    public Order markAsSent(Order order, DeliveryEvent event) {
        order.startDelivery(event);  // 비즈니스 로직 금지!
        return orderPersistencePort.persist(order);
    }
}

// ✅ Good - UseCase에서 비즈니스 로직, TransactionManager는 persist만
@Service
public class SendOrderService implements SendOrderUseCase {
    public OrderResponse execute(SendOrderCommand command) {
        Order order = orderQueryPort.findById(command.orderId());
        DeliveryEvent event = createDeliveryEvent(command);

        order.startDelivery(event);  // 비즈니스 로직은 UseCase에서

        Order savedOrder = orderTransactionManager.persist(order);  // 순수 위임

        externalDeliveryApi.send(event);  // 외부 API (트랜잭션 외부)
        return assembler.toResponse(savedOrder);
    }
}
```

---

## 6) 테스트 실행

```bash
# TransactionManager ArchUnit 테스트 실행
./gradlew :application:test --tests "*TransactionManagerArchTest"

# 특정 규칙 그룹만 실행
./gradlew :application:test --tests "*TransactionManagerArchTest\$BasicStructureRules"
./gradlew :application:test --tests "*TransactionManagerArchTest\$AnnotationRules"
./gradlew :application:test --tests "*TransactionManagerArchTest\$DependencyRules"
./gradlew :application:test --tests "*TransactionManagerArchTest\$MethodRules"
./gradlew :application:test --tests "*TransactionManagerArchTest\$ProhibitionRules"

# 전체 ArchUnit 테스트
./gradlew :application:test --tests "*ArchTest"
```

---

## 7) 위반 예시 및 해결

### 위반 1: persist() 외 다른 메서드

```java
// ❌ Bad - save(), markAsSent() 등 다른 메서드
@Component
@Transactional
public class OrderTransactionManager {
    public Order save(Order order) { ... }           // 금지!
    public Order markAsSent(Order order) { ... }     // 금지!
}

// ✅ Good - persist()만
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPersistencePort;

    public OrderTransactionManager(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public Order persist(Order order) {
        return orderPersistencePort.persist(order);
    }
}
```

### 위반 2: 여러 Out Port 의존

```java
// ❌ Bad - 여러 Port 의존
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPersistencePort;
    private final InventoryPersistencePort inventoryPort;  // 금지!
}

// ✅ Good - 단일 Port, 여러 Port는 Facade로
@Component
public class OrderFacade {
    private final OrderTransactionManager orderTxManager;
    private final InventoryTransactionManager inventoryTxManager;
    // Facade에서 조합
}
```

### 위반 3: @Transactional 누락

```java
// ❌ Bad - @Transactional 누락
@Component
public class OrderTransactionManager {
    public Order persist(Order order) { ... }
}

// ✅ Good - @Transactional 필수
@Component
@Transactional
public class OrderTransactionManager {
    public Order persist(Order order) { ... }
}
```

### 위반 4: Lombok 사용

```java
// ❌ Bad - Lombok 사용
@Component
@Transactional
@RequiredArgsConstructor
public class OrderTransactionManager {
    private final OrderPersistencePort orderPersistencePort;
}

// ✅ Good - Plain Java
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPersistencePort;

    public OrderTransactionManager(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }
}
```

### 위반 5: 다른 컴포넌트 의존

```java
// ❌ Bad - Service, UseCase, Facade 의존
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderService orderService;         // 금지!
    private final PlaceOrderUseCase useCase;        // 금지!
    private final OrderFacade facade;               // 금지!
}

// ✅ Good - Out Port만 의존
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort orderPersistencePort;
}
```

---

## 8) 허용 Out Port 목록

TransactionManager가 의존할 수 있는 Out Port:

| Port 타입 | 설명 | 예시 |
|----------|-----|------|
| **PersistencePort** | CUD 작업 (영속화) | `OrderPersistencePort` |
| **QueryPort** | Read 작업 (조회) | `OrderQueryPort` |
| **LockQueryPort** | 비관적 락 조회 | `OrderLockQueryPort` |

```java
// PersistencePort - 영속화 담당
@Component
@Transactional
public class OrderTransactionManager {
    private final OrderPersistencePort persistencePort;

    public Order persist(Order order) {
        return persistencePort.persist(order);
    }
}

// QueryPort - 조회 담당 (읽기 전용 트랜잭션)
@Component
@Transactional(readOnly = true)
public class OrderQueryTransactionManager {
    private final OrderQueryPort queryPort;

    public Order persist(Long orderId) {
        return queryPort.findById(orderId);
    }
}

// LockQueryPort - 비관적 락 조회
@Component
@Transactional
public class OrderLockTransactionManager {
    private final OrderLockQueryPort lockQueryPort;

    public Order persist(Long orderId) {
        return lockQueryPort.findByIdWithLock(orderId);
    }
}
```

---

## 9) 관련 문서

- **[TransactionManager Guide](transaction-manager-guide.md)** - TransactionManager 구현 상세
- **[TransactionManager Test Guide](transaction-manager-test-guide.md)** - 테스트 작성 가이드
- **[Facade Guide](../facade/facade-guide.md)** - 여러 TransactionManager 조합
- **[UseCase ArchUnit](../port/in/usecase-archunit.md)** - UseCase 검증 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0 (persist() Only 패턴)
