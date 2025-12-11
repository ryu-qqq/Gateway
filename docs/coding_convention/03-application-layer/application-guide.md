# Application Layer — **UseCase & Transaction Orchestration**

> 이 문서는 `application-layer`의 **요약 가이드**입니다.
>
> **핵심 원칙**, **컴포넌트 역할**, **CQRS 흐름**, **패키징 구조**, 그리고 각 디렉터리별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **UseCase = 단일 비즈니스 트랜잭션**: 하나의 UseCase는 하나의 유스케이스를 수행
* **Transaction 경계 엄격**: `@Transactional`은 Facade/Manager에서. **외부 API 호출은 트랜잭션 밖**에서
* **CQRS 분리 고정**: Command/Query **절대 섞지 않는다**. 패키지도 분리 (`command/`, `query/`)
* **Domain 중심 설계**: Application은 **Domain을 호출**하는 Orchestrator. 비즈니스 로직은 Domain에 위임
* **Port/Adapter 패턴**: UseCase는 Port Interface만 의존. 구현체는 Adapter에서 주입
* **DTO로 경계 보호**: Controller ↔ UseCase 간 Domain 객체 직접 노출 금지
* **컴포넌트 분리**: 역할별 전용 컴포넌트 사용 (Factory, Assembler, Facade, Manager)
* **Spring Proxy 제약 준수**: Private/Final 메서드, 같은 클래스 내부 호출 시 `@Transactional` 작동 안함

### 금지사항

* Lombok, Domain 직접 노출, UseCase 간 직접 호출
* `@Transactional` 내 외부 API 호출 (RestTemplate, WebClient, FeignClient 등)
* Private/Final 메서드에 `@Transactional` 적용
* Application Layer에 비즈니스 로직 작성
* Service(UseCase 구현체)에서 직접 객체 생성 (Factory 책임)

---

## 2) CQRS 컴포넌트 역할 분리

### Command 측 컴포넌트

| 컴포넌트 | 역할 | 어노테이션 | 의존성 |
|----------|------|-----------|--------|
| **CommandUseCase (Port-In)** | 인터페이스 정의 | Interface | - |
| **CommandService** | Port-In 구현, 흐름 조율 | `@Service` | CommandFactory, Facade/TransactionManager, Assembler |
| **CommandFactory** | Command → Domain, Bundle 생성 | `@Component` | Domain 객체만 |
| **CommandFacade** | 2개 이상 TransactionManager 조합 | `@Component` | TransactionManager 2개 이상 |
| **TransactionManager** | 단일 Port 트랜잭션, persist 담당 | `@Component` | PersistencePort 1개 |

### Query 측 컴포넌트

| 컴포넌트 | 역할 | 어노테이션 | 의존성 |
|----------|------|-----------|--------|
| **QueryUseCase (Port-In)** | 인터페이스 정의 | Interface | - |
| **QueryService** | Port-In 구현, 흐름 조율 | `@Service` | QueryFactory, QueryFacade/ReadManager, Assembler |
| **QueryFactory** | Query → Criteria 변환 | `@Component` | Domain Criteria만 |
| **QueryFacade** | 2개 이상 ReadManager 조합 | `@Component` | ReadManager 2개 이상 |
| **ReadManager** | 단일 Port 조회, 읽기 전용 | `@Component` | QueryPort 1개 |

### 공통 컴포넌트

| 컴포넌트 | 역할 | 어노테이션 | 의존성 |
|----------|------|-----------|--------|
| **Assembler** | Domain/Bundle → Response 변환 | `@Component` | Domain, Response DTO |

---

## 3) 컴포넌트 사용 기준

### Command 흐름

```
CommandService (UseCase 구현체)
├─ 객체 생성 필요 → CommandFactory 사용
├─ 단일 TransactionManager 사용 → TransactionManager 직접 호출
├─ 여러 TransactionManager 사용 → CommandFacade 호출
└─ Response 변환 → Assembler 사용

CommandFactory
├─ Command → Domain 변환
└─ Bundle 생성 (여러 객체 묶음)

CommandFacade
├─ 2개 이상 TransactionManager 조합 필수
├─ @Transactional 메서드 단위
└─ persist*() 메서드 네이밍

TransactionManager
├─ 단일 PersistencePort persist() 호출
├─ @Transactional 메서드 단위
└─ @Component 어노테이션
```

### Query 흐름

```
QueryService (UseCase 구현체)
├─ Criteria 변환 필요 → QueryFactory 사용
├─ 단일 ReadManager 사용 → ReadManager 직접 호출
├─ 여러 ReadManager 사용 → QueryFacade 호출
└─ Response 변환 → Assembler 사용

QueryFactory
└─ Query → Criteria 변환

QueryFacade
├─ 2개 이상 ReadManager 조합 필수
├─ @Transactional 금지 (읽기 전용)
└─ fetch*() 메서드 네이밍

ReadManager
├─ 단일 QueryPort 조회 호출
├─ @Transactional 금지 (읽기 전용)
└─ @Component 어노테이션
```

---

## 4) 전체 흐름

### Command 흐름 (복잡한 경우 - Facade 사용)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Controller                                                                  │
│   PlaceOrderRequest (REST DTO)                                             │
│       ↓                                                                     │
│   RequestMapper.toCommand(request)                                          │
│       ↓                                                                     │
│   PlaceOrderCommand (Application DTO)                                       │
└───────────────────────────────────────────┬─────────────────────────────────┘
                                            ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ CommandService (PlaceOrderService)                                         │
│                                                                             │
│   1. Order order = commandFactory.create(command)                          │
│      └─ CommandFactory: Command → Domain 변환                              │
│                                                                             │
│   2. order.place()                                                          │
│      └─ Domain 메서드: 비즈니스 로직 수행                                   │
│                                                                             │
│   3. OrderPersistBundle bundle = commandFactory.createBundle(order)        │
│      └─ CommandFactory: Bundle 생성 (Order, History, Outbox)               │
│                                                                             │
│   4. Order saved = commandFacade.persistOrderBundle(bundle)                │
│      └─ CommandFacade: 여러 TransactionManager 조합                        │
│                                                                             │
│   5. return assembler.toResponse(saved)                                     │
│      └─ Assembler: Domain → Response 변환                                   │
└───────────────────────────────────────────┬─────────────────────────────────┘
                                            ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ CommandFacade (OrderCommandFacade) — @Transactional                        │
│                                                                             │
│   1. Order saved = orderManager.persist(bundle.order())                     │
│      └─ ID 획득                                                             │
│                                                                             │
│   2. OrderPersistBundle enriched = bundle.enrichWithId(saved.id())          │
│      └─ ID Enrichment                                                       │
│                                                                             │
│   3. historyManager.persist(enriched.history())                             │
│   4. outboxManager.persist(enriched.outboxEvent())                          │
│                                                                             │
│   return saved                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Command 흐름 (단순한 경우 - TransactionManager 직접)

```
CommandService (UseCase 구현체)
├─ commandFactory.create(command)
├─ domain.action()
├─ transactionManager.persist(domain)  // 직접 호출
└─ assembler.toResponse(saved)
```

### Query 흐름 (복잡한 경우 - QueryFacade 사용)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Controller                                                                  │
│   OrderDetailRequest (REST DTO)                                            │
│       ↓                                                                     │
│   RequestMapper.toQuery(request)                                            │
│       ↓                                                                     │
│   OrderDetailQuery (Application DTO)                                        │
└───────────────────────────────────────────┬─────────────────────────────────┘
                                            ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ QueryService (GetOrderDetailService)                                       │
│                                                                             │
│   1. OrderDetailCriteria criteria = queryFactory.createDetailCriteria(query)│
│      └─ QueryFactory: Query → Criteria 변환                                │
│                                                                             │
│   2. OrderDetailQueryBundle bundle = queryFacade.fetchOrderDetail(criteria)│
│      └─ QueryFacade: 여러 ReadManager 조합                                  │
│                                                                             │
│   3. return assembler.toDetailResponse(bundle)                              │
│      └─ Assembler: Bundle → Response 변환                                   │
└───────────────────────────────────────────┬─────────────────────────────────┘
                                            ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ QueryFacade (OrderQueryFacade) — @Transactional 금지                       │
│                                                                             │
│   1. Order order = orderReadManager.findBy(criteria)                        │
│   2. List<Item> items = itemReadManager.findBy(order.id())                  │
│   3. Customer customer = customerReadManager.findBy(order.customerId())     │
│                                                                             │
│   return new OrderDetailQueryBundle(order, items, customer)                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Query 흐름 (단순한 경우 - ReadManager 직접)

```
QueryService (UseCase 구현체)
├─ queryFactory.createCriteria(query)  // 필요 시
├─ readManager.findBy(criteria)        // 직접 호출
└─ assembler.toResponse(domain)
```

---

## 5) 패키징 구조 (CQRS 분리)

```
application/
│
├─ common/
│    ├─ config/
│    │    └─ TransactionEventRegistry.java
│    └─ dto/
│         ├─ PageResponse.java
│         └─ SliceResponse.java
│
└─ order/                              # ← 예시 BC (Bounded Context)
   │
   ├─ assembler/
   │  └─ OrderAssembler.java            # Domain/Bundle → Response
   │
   ├─ dto/
   │  ├─ command/
   │  │   ├─ PlaceOrderCommand.java
   │  │   └─ CancelOrderCommand.java
   │  ├─ query/
   │  │   ├─ OrderDetailQuery.java
   │  │   └─ OrderSearchQuery.java
   │  ├─ response/
   │  │   ├─ OrderResponse.java
   │  │   └─ OrderDetailResponse.java
   │  └─ bundle/
   │      ├─ OrderPersistBundle.java      # Command용 Bundle
   │      └─ OrderDetailQueryBundle.java  # Query용 Bundle
   │
   ├─ facade/
   │  ├─ command/
   │  │   └─ OrderCommandFacade.java      # 2+ TransactionManager 조합
   │  └─ query/
   │      └─ OrderQueryFacade.java        # 2+ ReadManager 조합
   │
   ├─ factory/
   │  ├─ command/
   │  │   └─ OrderCommandFactory.java     # Command → Domain, Bundle 생성
   │  └─ query/
   │      └─ OrderQueryFactory.java       # Query → Criteria 변환
   │
   ├─ manager/
   │  ├─ command/
   │  │   ├─ OrderTransactionManager.java
   │  │   └─ OrderHistoryTransactionManager.java
   │  └─ query/
   │      ├─ OrderReadManager.java
   │      └─ OrderItemReadManager.java
   │
   ├─ port/
   │  ├─ in/
   │  │   ├─ command/
   │  │   │   ├─ PlaceOrderUseCase.java
   │  │   │   └─ CancelOrderUseCase.java
   │  │   └─ query/
   │  │       ├─ GetOrderDetailUseCase.java
   │  │       └─ SearchOrdersUseCase.java
   │  └─ out/
   │      ├─ command/
   │      │    └─ OrderPersistencePort.java
   │      └─ query/
   │           └─ OrderQueryPort.java
   │
   ├─ service/
   │  ├─ command/
   │  │   ├─ PlaceOrderService.java
   │  │   └─ CancelOrderService.java
   │  └─ query/
   │      ├─ GetOrderDetailService.java
   │      └─ SearchOrdersService.java
   │
   ├─ listener/
   │  └─ OrderEventListener.java        # @TransactionalEventListener
   │
   └─ scheduler/
      └─ OrderCleanupScheduler.java
```

---

## 6) 디렉터리별 상세 가이드 링크

### assembler/
* [Assembler Guide](./assembler/assembler-guide.md) - Domain/Bundle → Response 변환
* [Assembler ArchUnit](./assembler/assembler-archunit.md) - ArchUnit 검증 규칙
* [Assembler Test Guide](./assembler/assembler-test-guide.md) - 단위 테스트 가이드

### dto/
* **Command DTO**
  * [Command DTO Guide](./dto/command/command-dto-guide.md) - Command 패턴
* **Query DTO**
  * [Query DTO Guide](./dto/query/query-dto-guide.md) - Query 패턴
* **Response DTO**
  * [Response DTO Guide](./dto/response/response-dto-guide.md) - Response 패턴
* **Bundle DTO**
  * [Bundle Guide](./dto/bundle/bundle-guide.md) - Bundle 패턴 (여러 객체 묶음)
* **ArchUnit**
  * [DTO Record ArchUnit](./dto/dto-record-archunit.md) - Record 타입 강제 규칙

### facade/
* **Command Facade**
  * [CommandFacade Guide](./facade/command/facade-guide.md) - 여러 TransactionManager 조합
  * [CommandFacade ArchUnit](./facade/command/facade-archunit.md) - ArchUnit 검증 규칙
* **Query Facade**
  * [QueryFacade Guide](./facade/query/query-facade-guide.md) - 여러 ReadManager 조합
  * [QueryFacade ArchUnit](./facade/query/query-facade-archunit.md) - ArchUnit 검증 규칙
* [Facade Test Guide](./facade/facade-test-guide.md) - Mock 기반 단위 테스트

### factory/
* **Command Factory**
  * [CommandFactory Guide](./factory/command/command-factory-guide.md) - Command → Domain, Bundle 생성
  * [CommandFactory ArchUnit](./factory/command/command-factory-archunit.md) - ArchUnit 검증 규칙
  * [CommandFactory Test Guide](./factory/command/command-factory-test-guide.md) - 테스트 가이드
* **Query Factory**
  * [QueryFactory Guide](./factory/query/query-factory-guide.md) - Query → Criteria 변환
  * [QueryFactory ArchUnit](./factory/query/query-factory-archunit.md) - ArchUnit 검증 규칙
  * [QueryFactory Test Guide](./factory/query/query-factory-test-guide.md) - 테스트 가이드

### manager/
* **Command Manager (TransactionManager)**
  * [TransactionManager Guide](./manager/command/transaction-manager-guide.md) - 단일 Port 트랜잭션 처리
  * [TransactionManager ArchUnit](./manager/command/transaction-manager-archunit.md) - ArchUnit 검증 규칙
  * [TransactionManager Test Guide](./manager/command/transaction-manager-test-guide.md) - 테스트 가이드
* **Query Manager (ReadManager)**
  * [ReadManager Guide](./manager/query/read-manager-guide.md) - 단일 Port 조회 처리
  * [ReadManager ArchUnit](./manager/query/read-manager-archunit.md) - ArchUnit 검증 규칙
  * [ReadManager Test Guide](./manager/query/read-manager-test-guide.md) - 테스트 가이드

### service/
* **Command Service**
  * [CommandService Guide](./service/command/command-service-guide.md) - UseCase 구현 (Command)
  * [CommandService ArchUnit](./service/command/command-service-archunit.md) - ArchUnit 검증 규칙
  * [CommandService Test Guide](./service/command/command-service-test-guide.md) - 테스트 가이드
* **Query Service**
  * [QueryService Guide](./service/query/query-service-guide.md) - UseCase 구현 (Query)
  * [QueryService ArchUnit](./service/query/query-service-archunit.md) - ArchUnit 검증 규칙
  * [QueryService Test Guide](./service/query/query-service-test-guide.md) - 테스트 가이드
* [Service Guide](./service/service-guide.md) - 전체 Service 가이드

### event/
* [Transaction Event Registry Guide](./event/transaction-event-registry-guide.md) - 커밋 후 Event 발행
* [Event Listener Guide](./listener/event-listener-guide.md) - 동기 처리, 커밋 후 실행

### port/
* **Port In (Driving Port)**
  * **Command**
    * [Port-In Command Guide](./port/in/command/port-in-command-guide.md) - Command UseCase 설계
  * **Query**
    * [Port-In Query Guide](./port/in/query/port-in-query-guide.md) - Query UseCase 설계
  * [UseCase ArchUnit](./port/in/usecase-archunit.md) - ArchUnit 검증

* **Port Out (Driven Port)**
  * **Command**
    * [Port-Out Command Guide](./port/out/command/port-out-command-guide.md) - Persistence Port 설계
  * **Query**
    * [Port-Out Query Guide](./port/out/query/port-out-query-guide.md) - Query Port 설계

### listener/
* [Event Listener Guide](./listener/event-listener-guide.md) - @TransactionalEventListener 사용

### scheduler/
* [Scheduler Guide](./scheduler/scheduler-guide.md) - 배치 작업 오케스트레이션
* [Scheduler ArchUnit](./scheduler/scheduler-archunit.md) - ArchUnit 검증 규칙
* [Scheduler Test Guide](./scheduler/scheduler-test-guide.md) - 테스트 가이드

---

## 7) 컴포넌트 어노테이션 규칙

| 컴포넌트 | 어노테이션 | `@Transactional` | 이유 |
|----------|-----------|-----------------|------|
| CommandService | `@Service` | ❌ 금지 | Facade/Manager 책임 |
| QueryService | `@Service` | ❌ 금지 | 읽기 전용, 불필요 |
| CommandFactory | `@Component` | ❌ 금지 | 변환만, 트랜잭션 불필요 |
| QueryFactory | `@Component` | ❌ 금지 | 변환만, 트랜잭션 불필요 |
| CommandFacade | `@Component` | ✅ 메서드 단위 | 여러 Manager 조합 |
| QueryFacade | `@Component` | ❌ 금지 | 읽기 전용, 불필요 |
| TransactionManager | `@Component` | ✅ 메서드 단위 | persist 담당 |
| ReadManager | `@Component` | ❌ 금지 | 읽기 전용, 불필요 |
| Assembler | `@Component` | ❌ 금지 | 변환만, 트랜잭션 불필요 |

---

## 8) 체크리스트

### CommandService 구현 시
- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/command/` 패키지 위치
- [ ] CommandFactory로 객체 생성 (직접 생성 금지)
- [ ] Domain 메서드 호출 (비즈니스 로직 위임)
- [ ] 단일 Manager → 직접 호출 / 여러 Manager → Facade 호출
- [ ] Assembler로 Response 변환
- [ ] `@Transactional` 직접 사용 금지 (Facade/Manager 책임)

### QueryService 구현 시
- [ ] `@Service` 어노테이션
- [ ] Port-In (UseCase) 인터페이스 구현
- [ ] `service/query/` 패키지 위치
- [ ] QueryFactory로 Criteria 생성 (필요 시)
- [ ] 단일 ReadManager → 직접 호출 / 여러 ReadManager → QueryFacade 호출
- [ ] Assembler로 Response 변환
- [ ] `@Transactional` 금지 (읽기 전용)
- [ ] Command 관련 클래스 의존 금지 (CQRS 분리)

### CommandFacade 구현 시
- [ ] `@Component` 어노테이션
- [ ] 2개 이상 TransactionManager 의존
- [ ] `@Transactional` 메서드 단위 (클래스 레벨 금지)
- [ ] `persist*()` 메서드 네이밍

### QueryFacade 구현 시
- [ ] `@Component` 어노테이션
- [ ] 2개 이상 ReadManager 의존
- [ ] `@Transactional` 금지 (읽기 전용)
- [ ] `fetch*()` 메서드 네이밍

### TransactionManager 구현 시
- [ ] `@Component` 어노테이션
- [ ] 단일 PersistencePort 의존
- [ ] `@Transactional` 메서드 단위
- [ ] `persist()` 메서드만

### ReadManager 구현 시
- [ ] `@Component` 어노테이션
- [ ] 단일 QueryPort 의존
- [ ] `@Transactional` 금지 (읽기 전용)
- [ ] `findBy*()`, `getBy*()` 메서드 네이밍

---

## 9) 관련 문서

- **[Domain Layer Guide](../02-domain-layer/domain-guide.md)** - Aggregate, VO, Event
- **[REST API Layer Guide](../01-adapter-in-layer/rest-api/rest-api-guide.md)** - Controller, Request/Response
- **[Persistence Layer Guide](../04-persistence-layer/mysql/persistence-mysql-guide.md)** - Entity, Repository

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 3.0.0
