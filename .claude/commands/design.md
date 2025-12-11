---
description: 컴포넌트 설계 및 체크리스트 생성. 레이어별 필요 컴포넌트 도출 + 규칙 기반 체크리스트 JSON 생성. Cursor/다른 AI에서 사용 가능.
tags: [project]
---

# /design - Component Design & Checklist Generator

기능에 필요한 컴포넌트를 설계하고 체크리스트 JSON을 생성합니다.

## 사용 형식

```bash
# 전체 레이어 설계
/design "주문 취소 기능"

# 특정 레이어만 설계
/design application "주문 취소"
/design domain "주문 취소"
/design persistence "주문 취소"
/design rest-api "주문 취소"
```

---

## 실행 프로세스

```
/design "주문 취소 기능"
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 1️⃣ 사용 이력 검색                                           │
│    - read_memory("component-usage-history")                 │
│    - 키워드 매칭으로 유사 패턴 찾기                          │
│    - 추천 패턴 제시                                          │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2️⃣ 기본 골격 생성                                           │
│    - read_memory("component-dependency-graph")              │
│    - Command/Query 흐름 결정                                │
│    - 필수 컴포넌트 목록 생성                                 │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3️⃣ 옵션 질문 (필요시)                                       │
│    - read_memory("component-options")                       │
│    - 트랜잭션 복잡도, 이벤트, 동시성, 외부 연동 등          │
│    - 애매한 경우 사용자에게 질문                             │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4️⃣ 컴포넌트 목록 확정                                       │
│    - 레이어별 컴포넌트 정리                                  │
│    - 패키지, 네이밍, 의존성 명시                             │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5️⃣ 체크리스트 생성                                          │
│    - 각 컴포넌트별 규칙 메모리에서 체크리스트 추출           │
│    - JSON 형태로 구조화                                      │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│ 6️⃣ 저장 및 이력 업데이트                                    │
│    - write_memory("design-{feature}", 체크리스트JSON)       │
│    - edit_memory("component-usage-history", 사용 이력 추가) │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: 사용 이력 검색

### 프로세스

```python
# 1. 사용 이력 읽기
mcp__serena__read_memory(memory_file_name="component-usage-history")

# 2. 키워드 추출 및 매칭
keywords = extract_keywords("{feature_name}")  # "주문", "취소" 등
matched_patterns = match_triggers(keywords)

# 3. 추천 패턴 정렬 (사용 빈도 + 최근 사용)
recommendations = sort_by_relevance(matched_patterns)
```

### 출력 형식

```markdown
## 💡 추천 패턴 발견!

### [1] complex_with_event (5회 사용, 최근 2024-12-07)
- **설명**: 복합 트랜잭션 + 이벤트
- **예시**: `PlaceOrderService`, `CancelOrderService`
- **매칭 이유**: "주문", "취소" 키워드 일치

### [2] simple_crud (12회 사용, 최근 2024-12-08)
- **설명**: 단순 CRUD
- **예시**: `UpdateCategoryService`

어떤 패턴으로 시작할까요? [1/2/직접 선택]
```

---

## Phase 2: 기본 골격 생성

### 프로세스

```python
# 1. 의존성 그래프 읽기
mcp__serena__read_memory(memory_file_name="component-dependency-graph")

# 2. 흐름 타입 결정
flow_type = "command" if is_state_change else "query"

# 3. 필수 컴포넌트 목록 생성
components = get_required_components(flow_type, selected_pattern)
```

---

## Phase 3: 옵션 질문

### 프로세스

```python
# 1. 옵션 메모리 읽기
mcp__serena__read_memory(memory_file_name="component-options")

# 2. 자동 추론 시도
auto_inferred = auto_inference(keywords)

# 3. 애매한 경우 질문
if not confident:
    ask_user(question, choices)
```

### 질문 예시

```markdown
## 🤔 몇 가지 확인이 필요합니다

### Q1. 트랜잭션 복잡도
몇 개의 Aggregate를 변경하나요?

- [ ] 1개 (Order만) → TransactionManager 사용
- [x] 2개+ (Order + Payment) → Facade + Manager 사용

### Q2. 이벤트 발행
"취소" 키워드가 감지되었습니다. 보통 후속 작업이 필요한데요.

- [ ] 이벤트 없음
- [x] 트랜잭션 커밋 후 발행 (추천)
```

---

## Phase 4: 컴포넌트 목록 확정

### 출력 형식

```markdown
## 📦 컴포넌트 설계 완료

### Domain Layer
| 컴포넌트 | 패키지 | 네이밍 |
|----------|--------|--------|
| Aggregate | `domain/order/aggregate` | `Order.java` |
| VO (상태) | `domain/order/vo` | `OrderStatus.java` |
| Event | `domain/order/event` | `OrderCancelledEvent.java` |
| Exception | `domain/order/exception` | `OrderNotCancellableException.java` |

### Application Layer
| 컴포넌트 | 패키지 | 네이밍 |
|----------|--------|--------|
| UseCase | `application/order/port/in/command` | `CancelOrderUseCase.java` |
| Service | `application/order/service/command` | `CancelOrderService.java` |
| Command | `application/order/dto/command` | `CancelOrderCommand.java` |
| Response | `application/order/dto/response` | `OrderResponse.java` |
| Factory | `application/order/factory/command` | `OrderCommandFactory.java` |
| Facade | `application/order/facade/command` | `CancelOrderFacade.java` |
| Manager | `application/order/manager/command` | `OrderTransactionManager.java` |
| Manager | `application/payment/manager/command` | `PaymentTransactionManager.java` |
| Assembler | `application/order/assembler` | `OrderAssembler.java` |
| EventRegistry | `application/common` | `TransactionEventRegistry.java` |

### Persistence Layer
| 컴포넌트 | 패키지 | 네이밍 |
|----------|--------|--------|
| Entity | `adapter-out/persistence/order/entity` | `OrderJpaEntity.java` |
| Repository | `adapter-out/persistence/order/repository` | `OrderJpaRepository.java` |
| Mapper | `adapter-out/persistence/order/mapper` | `OrderEntityMapper.java` |
| Adapter | `adapter-out/persistence/order/adapter/command` | `OrderPersistenceAdapter.java` |

### REST API Layer
| 컴포넌트 | 패키지 | 네이밍 |
|----------|--------|--------|
| Controller | `adapter-in/rest-api/order/controller` | `OrderCommandController.java` |
| Request | `adapter-in/rest-api/order/dto/request` | `CancelOrderRequest.java` |
| Mapper | `adapter-in/rest-api/order/mapper` | `OrderRequestMapper.java` |
```

---

## Phase 4-1: 레이어 간 계약 명시 (병렬 작업용)

> **중요**: 워크트리로 병렬 작업 시 레이어 간 인터페이스 불일치를 방지하기 위해
> 각 레이어의 연결 지점을 명확히 정의합니다.

### Domain ↔ Application 계약

| 항목 | 스펙 | 설명 |
|------|------|------|
| **Aggregate 메서드** | `Order.cancel(Instant now): void` | 상태 변경 메서드 시그니처 |
| **반환 VO** | `OrderId` | ID Value Object 타입 |
| **발행 Event** | `OrderCancelledEvent(OrderId orderId, Instant cancelledAt)` | 도메인 이벤트 필드 |
| **예외** | `OrderNotCancellableException extends DomainException` | 비즈니스 예외 |
| **상태 VO** | `OrderStatus.CANCELLED` | 상태 enum 값 |

### Application ↔ Persistence 계약

| Port | 메서드 시그니처 | 반환 |
|------|----------------|------|
| `OrderPersistencePort` | `persist(Order order)` | `OrderId` |
| `OrderQueryPort` | `findById(OrderId id)` | `Optional<Order>` |
| `OrderLockQueryPort` | `findByIdForUpdate(OrderId id)` | `Optional<Order>` |

### Application ↔ REST API 계약

| 구분 | 타입 | 필드 |
|------|------|------|
| **Request** | `CancelOrderRequest` | `reason: String` (optional) |
| **Response** | `OrderResponse` | `id: Long`, `status: String`, `cancelledAt: Instant` |
| **Command** | `CancelOrderCommand` | `orderId: OrderId`, `reason: String` |

### 계약 출력 형식

```markdown
## 📜 레이어 간 계약 (Contract)

### Domain Layer 제공
```java
// Aggregate
Order.cancel(Instant now): void
Order.id(): OrderId

// Event
OrderCancelledEvent(OrderId orderId, Instant cancelledAt)

// Exception
OrderNotCancellableException(OrderId orderId, OrderStatus currentStatus)
```

### Application Layer 제공
```java
// Port-In (Controller가 호출)
CancelOrderUseCase.execute(CancelOrderCommand): OrderResponse

// Port-Out (Adapter가 구현)
OrderPersistencePort.persist(Order): OrderId
OrderQueryPort.findById(OrderId): Optional<Order>
```

### REST API Layer 제공
```java
// Endpoint
POST /api/v1/orders/{orderId}/cancel

// Request Body
CancelOrderRequest { reason: String }

// Response Body
OrderResponse { id, status, cancelledAt }
```
```

---

## Phase 5: 체크리스트 JSON 생성

### JSON 구조

```json
{
  "feature": "cancel-order",
  "created_at": "2024-12-08T10:30:00Z",
  "pattern": "complex_with_event",
  "bc": "order",

  "layers": {
    "domain": {
      "components": [
        {
          "type": "Aggregate",
          "name": "Order",
          "package": "com.ryuqq.domain.order.aggregate",
          "file": "Order.java",
          "action": "modify",
          "checklist": [
            "[ ] cancel() 메서드 추가",
            "[ ] 상태 검증 (PLACED, CONFIRMED만 취소 가능)",
            "[ ] OrderCancelledEvent 발행",
            "[ ] Lombok 금지",
            "[ ] Tell, Don't Ask 원칙"
          ],
          "rules_reference": "domain-rules-01-aggregate"
        },
        {
          "type": "Event",
          "name": "OrderCancelledEvent",
          "package": "com.ryuqq.domain.order.event",
          "file": "OrderCancelledEvent.java",
          "action": "create",
          "checklist": [
            "[ ] DomainEvent 상속",
            "[ ] final 필드 (불변)",
            "[ ] occurredAt 포함",
            "[ ] orderId 포함"
          ],
          "rules_reference": "domain-rules-04-event"
        }
      ]
    },

    "application": {
      "components": [
        {
          "type": "UseCase",
          "name": "CancelOrderUseCase",
          "package": "com.ryuqq.application.order.port.in.command",
          "file": "CancelOrderUseCase.java",
          "action": "create",
          "checklist": [
            "[ ] interface 타입",
            "[ ] execute(CancelOrderCommand) 메서드",
            "[ ] OrderResponse 반환"
          ],
          "rules_reference": "app-rules-03-port"
        },
        {
          "type": "Service",
          "name": "CancelOrderService",
          "package": "com.ryuqq.application.order.service.command",
          "file": "CancelOrderService.java",
          "action": "create",
          "dependencies": [
            "OrderCommandFactory",
            "CancelOrderFacade",
            "TransactionEventRegistry",
            "OrderAssembler"
          ],
          "checklist": [
            "[ ] @Service 어노테이션",
            "[ ] CancelOrderUseCase 구현",
            "[ ] @Transactional 금지 (Facade 책임)",
            "[ ] Port 직접 호출 금지",
            "[ ] Lombok 금지",
            "[ ] 생성자 주입"
          ],
          "rules_reference": "app-rules-01-service"
        },
        {
          "type": "Facade",
          "name": "CancelOrderFacade",
          "package": "com.ryuqq.application.order.facade.command",
          "file": "CancelOrderFacade.java",
          "action": "create",
          "dependencies": [
            "OrderTransactionManager",
            "PaymentTransactionManager"
          ],
          "checklist": [
            "[ ] @Component 어노테이션 (@Service X)",
            "[ ] @Transactional 메서드 레벨",
            "[ ] 2+ Manager 의존",
            "[ ] persist*() 메서드 네이밍",
            "[ ] Lombok 금지"
          ],
          "rules_reference": "app-rules-04-manager-facade"
        }
      ]
    },

    "persistence": {
      "components": [
        {
          "type": "Entity",
          "name": "OrderJpaEntity",
          "package": "com.ryuqq.adapter.out.persistence.order.entity",
          "file": "OrderJpaEntity.java",
          "action": "modify",
          "checklist": [
            "[ ] status 필드 업데이트 로직",
            "[ ] cancelledAt 필드 추가 (필요시)",
            "[ ] Long FK 유지",
            "[ ] Lombok 금지"
          ],
          "rules_reference": "persistence-rules-01-entity"
        }
      ]
    },

    "rest_api": {
      "components": [
        {
          "type": "Controller",
          "name": "OrderCommandController",
          "package": "com.ryuqq.adapter.in.rest.order.controller",
          "file": "OrderCommandController.java",
          "action": "modify",
          "checklist": [
            "[ ] POST /api/v1/orders/{id}/cancel 엔드포인트",
            "[ ] @Valid 사용",
            "[ ] UseCase 위임만",
            "[ ] 비즈니스 로직 금지"
          ],
          "rules_reference": "rest-api-rules-01-controller"
        }
      ]
    }
  },

  "options_selected": {
    "transaction_complexity": "multiple_same_bc",
    "event_publishing": "async_event",
    "concurrency_control": "no_lock",
    "external_integration": "no_external"
  },

  "execution_order": [
    "domain/Order.java (modify)",
    "domain/OrderCancelledEvent.java (create)",
    "application/CancelOrderUseCase.java (create)",
    "application/CancelOrderCommand.java (create)",
    "application/CancelOrderFacade.java (create)",
    "application/CancelOrderService.java (create)",
    "persistence/OrderJpaEntity.java (modify)",
    "rest-api/OrderCommandController.java (modify)"
  ]
}
```

---

## Phase 6: 저장 및 이력 업데이트

### 저장

```python
# 1. 설계 결과 저장
mcp__serena__write_memory(
    memory_file_name="design-cancel-order",
    content=checklist_json
)

# 2. 사용 이력 업데이트
mcp__serena__edit_memory(
    memory_file_name="component-usage-history",
    needle="usage_count: (\\d+)",
    repl="usage_count: {incremented}",
    mode="regex"
)
```

---

## 출력 형식 (최종)

```markdown
## ✅ 설계 완료: cancel-order

### 📊 요약
- **패턴**: complex_with_event
- **레이어**: 4개 (Domain, Application, Persistence, REST API)
- **컴포넌트**: 12개 (신규 8개, 수정 4개)

### 💾 저장됨
- `design-cancel-order` 메모리에 체크리스트 저장
- 사용 이력 업데이트 완료

### 🚀 다음 단계

**옵션 1: Cursor에서 구현**
```
design-cancel-order 메모리 내용을 Cursor에 전달하여
체크리스트 기반 빠른 구현
```

**옵션 2: Claude Code에서 구현**
```bash
/impl domain cancel-order
/impl application cancel-order
/impl persistence cancel-order
/impl rest-api cancel-order
```

**옵션 3: 검증만**
```bash
/verify cancel-order
```

어떻게 진행할까요?
```

---

## /plan과의 연동

```
/plan "주문 취소"
        ↓
    요구사항 분석
    영향도 분석
        ↓
    [신규 생성 필요]
        ↓
/design "주문 취소"  ← 자동 연결 가능
        ↓
    컴포넌트 설계
    체크리스트 생성
        ↓
/impl or Cursor 구현
        ↓
/verify 검증
```

---

## 관련 메모리

| 메모리 | 용도 |
|--------|------|
| `component-dependency-graph` | 전체 레이어 컴포넌트 의존성 |
| `component-options` | 선택적 컴포넌트 + 질문 목록 |
| `component-usage-history` | 패턴 사용 이력 + 추천 |
| `design-{feature}` | 생성된 설계 체크리스트 |
