# Application Layer — **UseCase & Transaction Orchestration**

> 이 문서는 `application-layer`의 **요약 가이드**입니다.
>
> **핵심 원칙**, **패키징 구조**, 그리고 각 디렉터리별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **UseCase = 단일 비즈니스 트랜잭션**: 하나의 UseCase는 하나의 유스케이스를 수행. 여러 UseCase 조합은 Facade에서.
* **Transaction 경계 엄격**: `@Transactional`은 UseCase에만. **외부 API 호출은 트랜잭션 밖**에서.
* **CQRS 분리 고정**: Command/Query **절대 섞지 않는다**. 패키지도 `service/command/`, `service/query/`로 구분.
* **Domain 중심 설계**: Application은 **Domain을 호출**하는 Orchestrator. 비즈니스 로직은 Domain에 위임.
* **Port/Adapter 패턴**: UseCase는 Port Interface만 의존. 구현체는 Adapter에서 주입.
* **DTO로 경계 보호**: Controller ↔ UseCase 간 Domain 객체 직접 노출 금지.
* **Assembler로 변환 집중**: DTO ↔ Domain 변환은 Assembler에 집중.
* **Spring Proxy 제약 준수**: Private/Final 메서드, 같은 클래스 내부 호출 시 `@Transactional` 작동 안함.

### 금지사항

* Lombok, Domain 직접 노출, UseCase 간 직접 호출
* `@Transactional` 내 외부 API 호출 (RestTemplate, WebClient, FeignClient 등)
* Private/Final 메서드에 `@Transactional` 적용
* 스프링 AOP 한계 이해하기
* Application Layer에 비즈니스 로직 작성

---

## 2) 패키징 구조 (CQRS 분리, BC 예)

```
application/
│
├─ common/
│    └─ dto/
│        ├─ PageResponse.java
│        └─ SliceResponse.java
└─ order/                         # ← 예시 BC
   ├─ assembler/
   │  └─ OrderAssembler.java
   ├─ component/
   │  └─ OrderNotifier.java
   ├─ manager/
   │  └─ OrderTransactionManager.java
   ├─ dto/
   │  ├─ command/
   │  │   ├─ PlaceOrderCommand.java
   │  │   └─ CancelOrderCommand.java
   │  ├─ query/
   │  │   └─ OrderSearchQuery.java
   │  └─ response/
   │     └─ OrderResponse.java
   ├─ facade/
   │  └─ OrderFacade.java
   ├─ port/
   │  ├─ in/
   │  │   ├─ command/
   │  │   │   └─ PlaceOrderUseCase.java
   │  │   └─ query/
   │  │       └─ GetOrderUseCase.java
   │  └─ out/
   │      ├─ command/
   │      │    └─ OrderPersistencePort.java
   │      └─ query/
   │           └─ OrderQueryPort.java
   ├─ service/
   │  ├─ command/
   │  │   └─ PlaceOrderService.java
   │  └─ query/
   │     └─ GetOrderService.java
   ├─ listener/
   │  └─ OrderEventListener.java
   └─ scheduler/
      └─ OrderCleanupScheduler.java
```

---

## 3) 디렉터리별 상세 가이드 링크

### assembler/
* [Assembler Guide](./assembler/assembler-guide.md) - Assembler 역할, 변환 패턴
* [Assembler ArchUnit](./assembler/assembler-archunit.md) - ArchUnit 검증 규칙
* [Assembler Test Guide](./assembler/assembler-test-guide.md) - 단위 테스트 가이드

### dto/
* **Command DTO**
  * [Command DTO Guide](./dto/command/command-dto-guide.md) - Command 패턴
* **Query DTO**
  * [Query DTO Guide](./dto/query/query-dto-guide.md) - Query 패턴
* **Response DTO**
  * [Response DTO Guide](./dto/response/response-dto-guide.md) - Response 패턴
* **ArchUnit**
  * [DTO Record ArchUnit](./dto/06_archunit-dto-record-rules.md) - Record 타입 강제 규칙 (17개)

### facade/
* [Facade Guide](./facade/facade-guide.md) - 여러 Transaction Manager 조합
* [Facade Test Guide](./facade/facade-test-guide.md) - Mock 기반 단위 테스트

### manager/
* [Transaction Manager Guide](./manager/transaction-manager-guide.md) - 단일 Port 트랜잭션 처리
* [Transaction Manager Test Guide](./manager/transaction-manager-test-guide.md) - Mock 기반 단위 테스트

### port/
* **Port In (Driving Port)**
  * **Command**
    * [Port-In Command Guide](./port/in/command/port-in-command-guide.md) - Command UseCase 설계
    * [Port-In Command ArchUnit](./port/in/command/port-in-command-archunit.md) - ArchUnit 검증
  * **Query**
    * [Port-In Query Guide](./port/in/query/port-in-query-guide.md) - Query UseCase 설계
    * [Port-In Query ArchUnit](./port/in/query/port-in-query-archunit.md) - ArchUnit 검증

* **Port Out (Driven Port)**
  * **Command**
    * [Port-Out Command Guide](./port/out/command/port-out-command-guide.md) - Persistence Port 설계
    * [Port-Out Command ArchUnit](./port/out/command/port-out-command-archunit.md) - ArchUnit 검증
  * **Query**
    * [Port-Out Query Guide](./port/out/query/port-out-query-guide.md) - Query Port 설계
    * [Port-Out Query ArchUnit](./port/out/query/port-out-query-archunit.md) - ArchUnit 검증

### service/
* TBD (작업 예정) - UseCase 구현 Service 가이드

### listener/
* TBD (작업 예정) - Domain Event Listener 가이드

### scheduler/
* TBD (작업 예정) - Scheduler 패턴 가이드

---
