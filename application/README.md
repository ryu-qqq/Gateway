# Application Layer

**Use Case Orchestration - Ports & Adapters Pattern**

Application Layer는 비즈니스 유스케이스를 조율하며, **Domain과 Adapter 사이의 중재자 역할**을 합니다.

---

## 📋 목차

- [핵심 원칙](#핵심-원칙)
- [Zero-Tolerance 규칙](#zero-tolerance-규칙)
- [금지된 의존성](#금지된-의존성)
- [verifyApplicationBoundaries](#verifyapplicationboundaries)
- [디렉토리 구조](#디렉토리-구조)
- [CQRS 패턴](#cqrs-패턴)
- [Transaction 관리](#transaction-관리)
- [테스트 전략](#테스트-전략)
- [ArchUnit 테스트](#archunit-테스트)

---

## 🎯 핵심 원칙

### 1. Use Case Orchestration
- **비즈니스 유스케이스 중심 설계**
- Domain 로직 조율 (직접 구현 금지)
- Port를 통한 외부 의존성 관리
- Assembler를 통한 계층 간 변환

### 2. Ports & Adapters Pattern
- **Port는 Interface만** (구현은 Adapter Layer)
- In Port: 외부에서 Application으로의 진입점
- Out Port: Application에서 외부로의 요청
- Dependency Inversion Principle (DIP) 적용

### 3. CQRS (Command Query Responsibility Segregation)
- **Command**: 상태 변경 (Create, Update, Delete)
- **Query**: 상태 조회 (Read)
- Port, DTO, UseCase 모두 Command/Query 분리
- Transaction 경계가 다름 (Command만 @Transactional)

### 4. Clean Architecture
- **Domain에만 의존** (Adapter 의존 금지)
- Infrastructure 세부사항 모름
- Framework Agnostic한 비즈니스 로직

---

## ⚠️ Zero-Tolerance 규칙

Application Layer에서 다음 규칙을 **절대로** 위반할 수 없습니다:

### 1. ❌ @Transactional 내 외부 API 호출 금지

```java
// ❌ 금지 (Transaction 롱홀딩)
@Transactional
public void placeOrder(PlaceOrderCommand command) {
    Order order = orderAssembler.toAggregate(command);
    OrderId orderId = orderPersistencePort.persist(order);

    // ❌ Transaction 내에서 외부 API 호출!
    paymentApiClient.processPayment(orderId); // SMTP, HTTP 호출 등
}

// ✅ 허용 (Transaction 분리)
public void placeOrder(PlaceOrderCommand command) {
    OrderId orderId = placeOrderTransaction(command);  // TX 1
    notifyOrderPlaced(orderId);  // TX 외부 (별도 실행)
}

@Transactional
private OrderId placeOrderTransaction(PlaceOrderCommand command) {
    Order order = orderAssembler.toAggregate(command);
    return orderPersistencePort.persist(order);
}

private void notifyOrderPlaced(OrderId orderId) {
    paymentApiClient.processPayment(orderId);  // ✅ TX 외부
}
```

**이유**:
- Transaction 롱홀딩 방지 (DB Connection Pool 고갈)
- 외부 API 지연 시 DB Lock 장시간 유지
- 네트워크 장애 시 Rollback 불가능

### 2. ❌ Adapter Layer 직접 의존 금지

```java
// ❌ 금지 (Adapter 직접 의존)
import com.ryuqq.adapter.out.OrderJpaAdapter;

@UseCase
public class PlaceOrderUseCase {
    private final OrderJpaAdapter orderAdapter;  // ❌
}

// ✅ 허용 (Port 의존)
import com.ryuqq.application.port.out.command.OrderPersistencePort;

@UseCase
public class PlaceOrderUseCase {
    private final OrderPersistencePort orderPort;  // ✅ Interface
}
```

**이유**:
- Hexagonal Architecture 준수
- Adapter 교체 가능성 (JPA → MongoDB)
- 테스트 시 Mock 주입 용이

### 3. ❌ Port는 Interface만 허용

```java
// ❌ 금지 (Port가 Class)
public class OrderQueryPort {
    public Optional<Order> findById(OrderId id) { }
}

// ✅ 허용 (Port는 Interface)
public interface OrderQueryPort {
    Optional<Order> findById(OrderId id);
}
```

**이유**:
- DIP (Dependency Inversion Principle)
- 구현은 Adapter Layer 책임
- Interface를 통한 계약 정의

### 4. ❌ DTO는 Record 타입 필수

```java
// ❌ 금지 (Lombok 사용)
@Data
public class PlaceOrderCommand {
    private Money amount;
}

// ❌ 금지 (Plain Class)
public class PlaceOrderCommand {
    private final Money amount;

    public PlaceOrderCommand(Money amount) {
        this.amount = amount;
    }

    public Money getAmount() { return amount; }
}

// ✅ 허용 (Record)
public record PlaceOrderCommand(
    Money amount,
    CustomerId customerId
) {
    // Compact Constructor로 검증 (선택)
    public PlaceOrderCommand {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(customerId);
    }
}
```

**이유**:
- 불변성 보장
- 간결한 코드
- 자동 equals/hashCode/toString

### 5. ❌ Assembler는 변환만 (비즈니스 로직 금지)

```java
// ❌ 금지 (비즈니스 로직 포함)
@Component
public class OrderAssembler {
    public Order toAggregate(PlaceOrderCommand command) {
        // ❌ 비즈니스 검증
        if (command.amount().value() < 1000) {
            throw new MinimumOrderAmountException();
        }

        // ❌ 계산 로직
        Money discountedAmount = command.amount()
            .multiply(0.9);

        return Order.forNew(clock, discountedAmount);
    }
}

// ✅ 허용 (단순 변환만)
@Component
public class OrderAssembler {
    private final ClockHolder clockHolder;

    public Order toAggregate(PlaceOrderCommand command) {
        return Order.forNew(
            clockHolder.getClock(),
            command.amount()  // 그대로 전달
        );
    }
}
```

**이유**:
- Assembler는 계층 간 변환기 역할만
- 비즈니스 로직은 Domain에서 처리
- UseCase에서 조율

---

## 🚫 금지된 의존성

`verifyApplicationBoundaries` Gradle 태스크가 다음 의존성을 **빌드 타임에 차단**합니다:

### Adapter Dependencies
```gradle
'adapter-in'     // REST API, GraphQL 등
'adapter-out'    // JPA, Redis, HTTP Client 등
```
→ Application은 Port만 의존

### Infrastructure Dependencies
```gradle
'org.springframework.data'    // Spring Data JPA
'org.hibernate'              // Hibernate
'com.querydsl'               // QueryDSL
```
→ Adapter Layer에서만 사용

### Lombok (부분 허용)
```java
// ❌ 금지 (UseCase, Port, DTO)
@Data
@Getter
@Setter

// ✅ 허용 (Assembler만 예외)
@Component  // Assembler는 Lombok 사용 가능 (생성자 주입)
@RequiredArgsConstructor
public class OrderAssembler { }
```
→ Assembler 외에는 Plain Java 사용

---

## 🛡️ verifyApplicationBoundaries

### 실행 방법
```bash
# Application Boundaries 검증
./gradlew :application:verifyApplicationBoundaries

# 빌드 시 자동 실행
./gradlew build
# ↑ verifyApplicationBoundaries가 자동으로 실행됨
```

### 동작 원리
1. **빌드 타임에 runtimeClasspath 검사**
2. Adapter 의존성이 발견되면 **빌드 즉시 실패**
3. 실수로 Adapter 직접 의존 시 즉시 차단

### 실패 예시
```bash
❌ APPLICATION BOUNDARY VIOLATION DETECTED

Application layer cannot depend on adapters:
- Dependency: adapter-out-persistence-mysql

Application should only depend on:
- domain module
- Spring Context (DI)

See: application/build.gradle
```

---

## 📁 디렉토리 구조

```
application/
├── src/main/java/com/ryuqq/application/
│   ├── sample/                    # 📚 예시 Bounded Context (참고용)
│   │   ├── README.md              # 사용 가이드
│   │   │
│   │   ├── assembler/             # 🔄 계층 간 변환
│   │   │   ├── SampleOrderAssembler.java
│   │   │   └── SampleOrderResponseAssembler.java
│   │   │
│   │   ├── dto/                   # 📋 Data Transfer Objects
│   │   │   ├── command/           # Command (생성/수정/삭제)
│   │   │   │   └── PlaceSampleOrderCommand.java
│   │   │   ├── query/             # Query (조회 조건)
│   │   │   │   └── SearchSampleOrderQuery.java
│   │   │   └── response/          # Response (응답)
│   │   │       ├── SampleOrderResponse.java
│   │   │       └── SampleOrderDetailResponse.java
│   │   │
│   │   ├── port/                  # 🔌 Ports (Interface)
│   │   │   ├── in/                # In Ports (외부 → Application)
│   │   │   │   ├── command/       # Command UseCase
│   │   │   │   │   └── PlaceSampleOrderUseCase.java
│   │   │   │   └── query/         # Query UseCase
│   │   │   │       └── SearchSampleOrderUseCase.java
│   │   │   │
│   │   │   └── out/               # Out Ports (Application → 외부)
│   │   │       ├── command/       # 상태 변경 (Persistence)
│   │   │       │   └── SampleOrderPersistencePort.java
│   │   │       └── query/         # 상태 조회 (Query)
│   │   │           ├── SampleOrderQueryPort.java
│   │   │           └── SampleOrderLockQueryPort.java
│   │   │
│   │   ├── facade/                # 🎭 Facade (복합 유스케이스)
│   │   │   └── SampleOrderFacade.java
│   │   │
│   │   └── manager/               # 🔧 Transaction Manager
│   │       └── SampleOrderTransactionManager.java
│   │
│   └── common/                    # 공통 유틸리티
│       ├── config/
│       │   └── SystemClockHolder.java  # ClockHolder 구현체
│       └── dto/
│           └── response/
│               ├── PageResponse.java
│               └── SliceResponse.java
│
├── src/test/java/                 # Tests
│   └── com/ryuqq/application/
│       ├── architecture/          # ArchUnit Tests
│       │   ├── assembler/
│       │   │   └── AssemblerArchTest.java
│       │   ├── dto/
│       │   │   └── DtoRecordArchTest.java
│       │   └── port/
│       │       └── out/
│       │           ├── QueryPortArchTest.java
│       │           ├── LockQueryPortArchTest.java
│       │           └── PersistencePortArchTest.java
│       │
│       └── sample/                # UseCase Tests
│           └── PlaceSampleOrderUseCaseTest.java
│
├── src/testFixtures/java/         # Test Fixtures
│   └── com/ryuqq/fixture/application/
│       ├── command/               # Command Fixtures
│       ├── query/                 # Query Fixtures
│       └── response/              # Response Fixtures
│
└── build.gradle                   # Dependencies & Verification
```

### 실제 프로젝트 구조 (Bounded Context 패턴)

```
application/
└── src/main/java/com/ryuqq/application/
    ├── order/          # Order Bounded Context
    │   ├── assembler/
    │   ├── dto/
    │   ├── port/
    │   ├── facade/
    │   └── manager/
    │
    ├── customer/       # Customer Bounded Context
    │   └── ...
    │
    ├── product/        # Product Bounded Context
    │   └── ...
    │
    └── common/         # 공통 (유지)
```

**sample/ 패키지를 참고하여 실제 Bounded Context를 생성하세요!**

---

## 🔄 CQRS 패턴

Command와 Query를 명확히 분리하여 책임을 구분합니다.

### Command Side (상태 변경)

```
플로우: Command DTO → UseCase → Assembler → Domain → PersistencePort

1. Command DTO (불변 입력)
   └─ record PlaceOrderCommand(Money amount, CustomerId customerId)

2. Command UseCase Interface (In Port)
   └─ interface PlaceOrderUseCase { OrderId execute(PlaceOrderCommand); }

3. Assembler (DTO → Domain)
   └─ Order toAggregate(PlaceOrderCommand)

4. Domain Logic
   └─ Order.forNew(clock, amount, customerId)

5. PersistencePort (Out Port)
   └─ interface OrderPersistencePort { OrderId persist(Order); }

6. @Transactional 적용
   └─ UseCase 구현체에만
```

### Query Side (상태 조회)

```
플로우: Query DTO → UseCase → QueryPort → Domain → ResponseAssembler → Response DTO

1. Query DTO (조회 조건)
   └─ record SearchOrderQuery(CustomerId customerId, OrderStatus status)

2. Query UseCase Interface (In Port)
   └─ interface SearchOrderUseCase { PageResponse<OrderResponse> execute(SearchOrderQuery); }

3. QueryPort (Out Port)
   └─ interface OrderQueryPort {
       Optional<Order> findById(OrderId id);
       PageResponse<Order> search(SearchOrderCriteria criteria);
   }

4. Domain 반환
   └─ List<Order>, Optional<Order>

5. Assembler (Domain → Response DTO)
   └─ OrderResponse toResponse(Order order)

6. @Transactional 불필요
   └─ 읽기 전용, readOnly=true 가능
```

### Lock Query (Lock을 사용하는 조회)

```
특수 케이스: 조회지만 Lock을 획득 (Pessimistic Lock)

1. LockQueryPort (별도 Interface)
   └─ interface OrderLockQueryPort {
       Optional<Order> findByIdForUpdate(OrderId id);
       Optional<Order> findByIdForUpdateNowait(OrderId id);
   }

2. @Transactional 필수
   └─ Lock은 Transaction 내에서만 유효

3. 사용 예시
   └─ 재고 차감, 선착순 이벤트, 중복 방지
```

---

## 💼 Transaction 관리

### 원칙

1. **@Transactional은 UseCase 구현체에만**
   - Interface가 아닌 구현체 클래스에 선언
   - Spring AOP 프록시 제약사항 고려

2. **외부 API 호출은 Transaction 외부에서**
   - HTTP, SMTP, Message Queue 등
   - Transaction 롱홀딩 방지

3. **Private 메서드는 @Transactional 불가**
   - Spring AOP 프록시는 public 메서드만
   - Transaction이 필요하면 별도 Bean으로 분리

### Transaction 분리 패턴

```java
@UseCase
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderTransactionManager txManager;
    private final PaymentApiClient paymentClient;

    // ✅ Public 진입점 (Transaction 없음)
    @Override
    public OrderId execute(PlaceOrderCommand command) {
        // 1. Transaction 내부 (DB 작업)
        OrderId orderId = txManager.placeOrder(command);

        // 2. Transaction 외부 (외부 API)
        paymentClient.processPayment(orderId);

        return orderId;
    }
}

@Component
@RequiredArgsConstructor
class OrderTransactionManager {

    private final OrderPersistencePort persistencePort;
    private final OrderAssembler assembler;

    // ✅ Transaction 메서드 (별도 Bean)
    @Transactional
    public OrderId placeOrder(PlaceOrderCommand command) {
        Order order = assembler.toAggregate(command);
        return persistencePort.persist(order);
    }
}
```

### Spring Proxy 제약사항 주의

```java
// ❌ 작동 안 함 (같은 클래스 내부 호출)
@UseCase
public class OrderService {

    public void placeOrder() {
        this.saveOrder();  // ❌ @Transactional 무시됨!
    }

    @Transactional
    private void saveOrder() { }
}

// ✅ 작동 함 (별도 Bean 호출)
@UseCase
public class OrderService {

    private final OrderTxManager txManager;

    public void placeOrder() {
        txManager.saveOrder();  // ✅ Proxy를 통한 호출
    }
}

@Component
class OrderTxManager {

    @Transactional
    public void saveOrder() { }  // ✅ Public 메서드
}
```

---

## 🧪 테스트 전략

### Unit Tests (UseCase)

**순수 비즈니스 로직 테스트**
- Port는 Mock/Stub으로 대체
- Domain 로직 검증
- Transaction 동작 검증 불필요 (Integration Test 영역)

```java
@ExtendWith(MockitoExtension.class)
class PlaceOrderUseCaseTest {

    @Mock
    private OrderPersistencePort persistencePort;

    @Mock
    private OrderAssembler assembler;

    @InjectMocks
    private PlaceOrderService useCase;

    @Test
    void placeOrder_Success() {
        // given
        PlaceOrderCommand command = PlaceOrderCommandFixture.defaultCommand();
        Order order = OrderFixture.forNew();
        OrderId expectedId = OrderIdFixture.random();

        when(assembler.toAggregate(command)).thenReturn(order);
        when(persistencePort.persist(order)).thenReturn(expectedId);

        // when
        OrderId orderId = useCase.execute(command);

        // then
        assertThat(orderId).isEqualTo(expectedId);
        verify(persistencePort).persist(order);
    }
}
```

### Integration Tests

**실제 Port 구현체와 통합 테스트**
- Application → Adapter 전체 플로우 검증
- @SpringBootTest 사용
- TestRestTemplate으로 End-to-End 검증

### Test Fixtures (testFixtures/)

**재사용 가능한 테스트 데이터**
```java
// Command Fixture
public class PlaceOrderCommandFixture {

    public static PlaceOrderCommand defaultCommand() {
        return new PlaceOrderCommand(
            Money.of(10000),
            CustomerIdFixture.random()
        );
    }
}

// Response Fixture
public class OrderResponseFixture {

    public static OrderResponse defaultResponse() {
        return new OrderResponse(
            OrderIdFixture.random(),
            Money.of(10000),
            OrderStatus.PLACED
        );
    }
}
```

---

## 🏛️ ArchUnit 테스트

Application Layer의 아키텍처 규칙을 **자동으로 검증**합니다.

### 1. QueryPortArchTest (17개 규칙)

**목적**: Query Port의 조회 패턴 검증

**주요 규칙**:
- ✅ 필수 메서드: `findById()`, `existsById()`
- ✅ search* 메서드 → PageResponse 반환 (페이징 필수)
- ✅ findBy* 메서드 → Optional/List 반환
- ✅ count* 메서드 → long 반환
- ❌ findAll 금지 (OOM 방지)
- ❌ 저장/수정/삭제 메서드 금지 (CQRS)
- ❌ DTO/Entity 반환 금지 (Domain만)

**왜 필요한가?**
- **페이징 강제**: search* 메서드는 PageResponse 반환 필수 (관리자 화면 등에서 대량 데이터 조회 시 OOM 방지)
- **단순 조회 허용**: findBy* 메서드는 Optional/List 허용 (간단한 조회는 Criteria 불필요)
- **findAll 금지**: 전체 데이터 로드는 메모리 폭발 위험

### 2. LockQueryPortArchTest (12개 규칙)

**목적**: Lock을 사용하는 조회 Port 검증

**주요 규칙**:
- ✅ 인터페이스명: *LockQueryPort
- ✅ 메서드 네이밍 패턴: ForUpdate, ForShare, Nowait, SkipLocked, WithOptimisticLock
- ✅ Optional 반환 (단건만)
- ❌ List/PageResponse 반환 금지 (성능)
- ❌ 저장/수정/삭제 메서드 금지

**왜 필요한가?**
- **Lock 명시화**: 메서드명에 Lock 타입 명시 (ForUpdate = Pessimistic Write Lock)
- **단건 조회 강제**: Lock은 성능상 단건 조회에만 사용 (대량 Lock은 Deadlock 위험)
- **Transaction 인지**: LockQueryPort는 @Transactional 내에서만 사용

**사용 예시**:
```java
// 재고 차감 (동시성 제어)
interface InventoryLockQueryPort {
    Optional<Inventory> findByIdForUpdate(InventoryId id);
}

// 선착순 이벤트 (대기 없이 실패)
interface EventLockQueryPort {
    Optional<Event> findByIdForUpdateNowait(EventId id);
}

// 큐 처리 (Lock 걸린 행 건너뛰기)
interface OrderLockQueryPort {
    Optional<Order> findNextForUpdateSkipLocked();
}
```

### 3. PersistencePortArchTest (11개 규칙)

**목적**: Persistence Port의 저장 패턴 검증

**주요 규칙**:
- ✅ 필수 메서드: `persist(T) → TId`
- ✅ 선택 메서드: `persistAll(List<T>) → List<TId>` (배치)
- ❌ save/update/delete 메서드 금지
- ❌ 조회 메서드 금지 (CQRS)
- ❌ 원시 타입 반환 금지 (Value Object만)

**왜 필요한가?**
- **JPA Merge 활용**: persist() 하나로 insert/update 통합 (PK 있으면 update, 없으면 insert)
- **CQRS 분리**: 저장 Port는 저장만, 조회는 QueryPort
- **타입 안전성**: Long 대신 OrderId 같은 Value Object 반환

**설계 의도**:
```java
// Domain 객체가 비즈니스 로직 판단
Order order = Order.forNew(clock, amount);  // PK 없음
order.cancel();  // PK 있음

// Port는 받은 대로 저장만
OrderId id = persistencePort.persist(order);  // JPA가 알아서 insert/update
```

### 4. AssemblerArchTest (18개 규칙)

**목적**: Assembler의 변환기 역할 검증

**주요 규칙**:
- ✅ @Component 필수 (Bean 등록)
- ✅ 메서드명 패턴: to*/from*/assemble*/map*
- ❌ Lombok 금지 (Plain Java)
- ❌ Static 메서드 금지 (테스트 용이성)
- ❌ Port/Repository 의존성 금지 (단순 변환기)
- ❌ 비즈니스 메서드 금지 (validate*, calculate* 등)
- ❌ @Transactional 금지
- ❌ PageResponse 반환 금지 (UseCase에서 조립)

**왜 필요한가?**
- **단순 변환만**: Assembler는 DTO ↔ Domain 변환만 (비즈니스 로직 금지)
- **의존성 최소화**: Port나 Repository 주입 금지 (데이터만 받아서 변환)
- **테스트 용이성**: Static 메서드 금지, Bean으로 등록하여 Mock 가능

**변환 방향**:
```java
// Command → Domain (In)
Order toAggregate(PlaceOrderCommand command);

// Domain → Response (Out)
OrderResponse toResponse(Order order);

// List 변환 (In/Out)
List<Order> toAggregateList(List<PlaceOrderCommand> commands);
List<OrderResponse> toResponseList(List<Order> orders);
```

### 5. DtoRecordArchTest (18개 규칙)

**목적**: DTO의 Record 타입 강제 및 순수성 검증

**주요 규칙**:
- ✅ Command/Query/Response는 Record 타입 필수
- ✅ 패키지 위치: dto/command, dto/query, dto/response
- ❌ Lombok 금지 (Record 사용)
- ❌ jakarta.validation 금지 (REST API Layer에서 검증)
- ❌ 비즈니스 메서드 금지
- ❌ Domain 객체 반환 금지 (Assembler 책임)
- ❌ Port/Repository 의존성 금지

**왜 필요한가?**
- **불변성 보장**: Record는 final fields, immutable
- **간결성**: equals/hashCode/toString 자동 생성
- **계층 분리**: DTO는 데이터 전달만, 변환은 Assembler, 검증은 REST API Layer

**Record 예시**:
```java
// Command (생성/수정)
public record PlaceOrderCommand(
    Money amount,
    CustomerId customerId
) {
    // Compact Constructor (검증, 선택)
    public PlaceOrderCommand {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(customerId);
    }
}

// Query (조회 조건)
public record SearchOrderQuery(
    CustomerId customerId,
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate
) { }

// Response (응답)
public record OrderResponse(
    OrderId orderId,
    Money amount,
    OrderStatus status,
    LocalDateTime createdAt
) { }
```

### ArchUnit 테스트 실행

```bash
# 모든 ArchUnit 테스트 실행
./gradlew :application:test --tests "*ArchTest"

# 특정 테스트만 실행
./gradlew :application:test --tests "QueryPortArchTest"

# 빌드 시 자동 실행
./gradlew build
```

---

## ✅ 체크리스트

Application Layer 개발 시 다음 사항을 준수하세요:

### Port
- [ ] **Port는 Interface만** (구현은 Adapter)
- [ ] **In Port와 Out Port 분리** (역할 명확화)
- [ ] **Command Port와 Query Port 분리** (CQRS)
- [ ] **Domain 타입 사용** (DTO/Entity 금지)

### DTO
- [ ] **DTO는 Record 타입** (불변성)
- [ ] **패키지 분리** (command, query, response)
- [ ] **jakarta.validation 사용 안 함** (REST API Layer에서 검증)
- [ ] **비즈니스 로직 없음** (데이터 전달만)

### Assembler
- [ ] **@Component로 Bean 등록** (테스트 용이성)
- [ ] **변환 메서드만** (to*, from*, assemble*, map*)
- [ ] **Port/Repository 의존 안 함** (단순 변환기)
- [ ] **비즈니스 로직 없음** (Domain에서 처리)

### UseCase
- [ ] **@Transactional 내 외부 API 호출 금지**
- [ ] **Domain 로직 조율만** (직접 구현 금지)
- [ ] **Assembler로 변환** (DTO ↔ Domain)
- [ ] **Port를 통한 외부 의존성 관리**

### Architecture
- [ ] **Adapter Layer 의존 안 함** (Port만)
- [ ] **verifyApplicationBoundaries 통과** (`./gradlew :application:verifyApplicationBoundaries`)
- [ ] **ArchUnit 테스트 통과** (5개 테스트)

---

## 📚 관련 문서

- [Assembler 가이드](../docs/coding_convention/03-application-layer/assembler/)
- [DTO 가이드](../docs/coding_convention/03-application-layer/dto/)
- [Port 가이드](../docs/coding_convention/03-application-layer/port/)
- [UseCase 가이드](../docs/coding_convention/03-application-layer/facade/) (예정)
- [Transaction 가이드](../docs/coding_convention/03-application-layer/manager/) (예정)

---

**Application Layer는 비즈니스 유스케이스의 조율자입니다. Domain과 Adapter 사이의 깨끗한 경계를 유지하세요.**
