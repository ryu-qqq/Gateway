---
description: 대화형 PRD (Product Requirements Document) 생성 with 비즈니스 질문 가이드
tags: [project]
---

# Create PRD - Interactive PRD Generation with Business Question Guide

당신은 대화형 방식으로 PRD (Product Requirements Document)를 생성하는 작업을 수행합니다.

**핵심 개선**: 추상적인 요구사항("Order 생성")을 **구체적인 비즈니스 규칙**으로 변환하기 위한 **비즈니스 질문 가이드**를 제공합니다.

---

## 목적

사용자와 Socratic 대화를 통해 **구체적인 비즈니스 요구사항**을 수집하고, Layer별로 구조화된 PRD 문서를 생성합니다.

## 입력 형식

사용자는 다음과 같이 기능명을 제공합니다:
```bash
/create-prd "Order Management"
/create-prd "User Authentication"
/create-prd "Product Catalog"
```

---

## 실행 단계

### 1. 프로젝트 개요 수집

**질문 템플릿**:
```markdown
## 📋 프로젝트 개요

이 기능의 핵심 목적은 무엇인가요?
- 어떤 비즈니스 문제를 해결하나요?
- 주요 사용자는 누구인가요?
- 성공 기준은 무엇인가요?
```

**수집 항목**:
- 기능명 (Feature Name)
- 비즈니스 목적 (Business Purpose)
- 주요 사용자 (Target Users)
- 성공 기준 (Success Criteria)

---

### 2. Layer별 요구사항 수집 (비즈니스 질문 가이드 포함)

#### 2.1 Domain Layer 요구사항 (⭐ 비즈니스 질문 강화)

**질문 템플릿** (기본):
```markdown
## 🏗️ Domain Layer

핵심 비즈니스 개념(Aggregate)은 무엇인가요?
- 어떤 엔티티가 필요한가요? (예: Order, Customer, Product)
- 각 엔티티의 핵심 속성은?
- 엔티티 간 관계는? (단, Long FK 전략 사용 - JPA 관계 어노테이션 금지)
- 핵심 비즈니스 규칙은?
```

**⭐ 비즈니스 질문 가이드** (Order 도메인 예시):

```markdown
## 🔍 비즈니스 규칙 구체화 질문

사용자가 "주문 생성, 취소, 수정"이라고만 답하면, 다음 질문들로 **구체화**하세요:

### 📦 주문 생성 (Order Creation)

**재고 관리**:
- ❓ 재고 확인은 즉시? 나중에?
- ❓ 재고 차감 시점은? (주문 생성 시? 결제 완료 시?)
- ❓ 재고 부족 시 처리는? (주문 거절? 대기? 부분 배송?)
- ❓ 재고 예약 기능 필요? (결제 전 30분 예약 등)

**결제 처리**:
- ❓ 결제는 동기? 비동기?
- ❓ 결제 실패 시 주문 상태는? (PENDING? FAILED? 자동 취소?)
- ❓ PG사는? (Stripe, Toss Payments, PG사 등)
- ❓ 결제 재시도 가능?

**주문 제약사항**:
- ❓ 주문 수량 제한은? (최소 1개, 최대 100개 등)
- ❓ 동일 상품 중복 주문 가능?
- ❓ 배송지 여러 개 가능? (선물 배송, 부분 배송)
- ❓ 쿠폰/할인 적용 시점은? (주문 생성 시? 결제 시?)

**주문 번호 생성**:
- ❓ 주문 번호 형식은? (UUID? Sequence? 날짜 기반?)
- ❓ 주문 번호 중복 방지 전략은?

**주문 금액 계산**:
- ❓ 금액 계산은 Application? Domain?
- ❓ 부가세 포함? (세율은?)
- ❓ 배송비는? (무료 배송 조건은?)

---

### 🚫 주문 취소 (Order Cancellation)

**취소 가능 조건**:
- ❓ 어느 상태까지 취소 가능? (PLACED? CONFIRMED? SHIPPED?)
- ❓ 부분 취소 가능? (일부 상품만 취소)
- ❓ 취소 사유 필수?

**환불 처리**:
- ❓ 취소 시 환불은 즉시? 승인 필요?
- ❓ 취소 수수료는? (상태별 차등 적용?)
- ❓ 환불 금액 계산은? (배송비 포함? 쿠폰 복구?)

**재고 복구**:
- ❓ 취소 시 재고는 즉시 복구?
- ❓ 취소 후 재주문 가능?

---

### ✏️ 주문 수정 (Order Modification)

**수정 가능 항목**:
- ❓ 어떤 항목 수정 가능? (수량? 배송지? 상품 추가/삭제?)
- ❓ 어느 상태까지 수정 가능?
- ❓ 수정 이력 관리 필요? (Audit Log)

**금액 재계산**:
- ❓ 수정 시 금액 재계산은 자동?
- ❓ 이미 결제한 경우 추가 결제? 환불?

---

### 🔄 주문 상태 변경 (Order Status Transition)

**상태 흐름**:
- ❓ 주문 상태 흐름은? (예: PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED)
- ❓ 각 상태 전환 조건은?
- ❓ 상태 롤백 가능? (예: CONFIRMED → PLACED)

**상태별 제약**:
- ❓ PLACED 상태에서 가능한 작업은? (취소? 수정?)
- ❓ SHIPPED 상태에서 취소 가능? (배송 중 취소)

---

### 🔢 동시성 제어

**Race Condition 방지**:
- ❓ 동일 주문 동시 취소 시도 시 처리는?
- ❓ 재고 동시 차감 시 처리는?
- ❓ Optimistic Lock? Pessimistic Lock?

```

**수집 항목**:
- Aggregate 목록 (구체적 비즈니스 규칙 포함)
- Entity 속성 (필드, 타입, 제약사항)
- 비즈니스 규칙 (Invariant)
- Value Object 목록
- 상태 전환 다이어그램

**Zero-Tolerance 규칙 자동 체크**:
- ✅ Law of Demeter 준수 (Getter 체이닝 금지)
- ✅ Lombok 금지 (Pure Java 또는 Record 사용)
- ✅ Long FK 전략 (JPA 관계 어노테이션 금지)

---

#### 2.2 Application Layer 요구사항 (⭐ Transaction 경계 강화)

**질문 템플릿** (기본):
```markdown
## ⚙️ Application Layer

어떤 UseCase가 필요한가요?
- 사용자가 수행하는 주요 작업은? (예: 주문 생성, 주문 취소)
- 각 UseCase의 입력/출력은?
- Transaction 경계는 어떻게 설정하나요?
- 외부 API 호출이 필요한가요? (트랜잭션 밖에서 호출 필수!)
```

**⭐ Transaction 경계 질문 가이드**:

```markdown
## 🔍 Transaction 경계 구체화 질문

### 🔐 Transaction Scope

**PlaceOrderUseCase 예시**:
- ❓ 주문 생성 + 재고 차감 + 결제 요청을 **하나의 트랜잭션**으로?
  - ⚠️ **위험**: 결제 API 호출 실패 시 전체 롤백 (재고 복구 누락)
  - ✅ **권장**: 주문 생성 + 재고 차감만 트랜잭션, 결제는 트랜잭션 밖

**CancelOrderUseCase 예시**:
- ❓ 주문 취소 + 재고 복구 + 환불 요청을 **하나의 트랜잭션**으로?
  - ⚠️ **위험**: 환불 API 호출 실패 시 전체 롤백
  - ✅ **권장**: 주문 취소 + 재고 복구만 트랜잭션, 환불은 Orchestration Pattern

---

### 🌐 외부 API 호출 전략

**결제 Gateway**:
- ❓ 결제 API는 트랜잭션 밖에서 호출? (필수!)
- ❓ 결제 실패 시 보상 트랜잭션 필요?
- ❓ 결제 성공 후 주문 상태 업데이트는 별도 트랜잭션?

**배송 API**:
- ❓ 배송 요청은 비동기? (Orchestration Pattern 사용 권장)
- ❓ 배송 실패 시 재시도 전략은? (WAL + Reaper Pattern)

**알림 서비스 (SMS, Email)**:
- ❓ 알림 전송은 트랜잭션 밖? (필수!)
- ❓ 알림 실패 시 재시도 필요?

---

### ⏱️ Timeout & Retry

**외부 API Timeout**:
- ❓ 결제 API Timeout은? (3초? 5초? 10초?)
- ❓ Timeout 발생 시 주문 상태는?

**Retry 전략**:
- ❓ 결제 API 실패 시 재시도 횟수는? (3회? 5회?)
- ❓ Exponential Backoff 사용?

```

**수집 항목**:
- UseCase 목록 (Command/Query 분리)
- Command DTO 정의
- Query DTO 정의
- **Transaction 경계 설계** (외부 API 호출 시점 명확화)
- 외부 API 호출 전략 (동기/비동기, Timeout, Retry)

**Zero-Tolerance 규칙 자동 체크**:
- ✅ Command/Query 분리 (CQRS)
- ✅ **Transaction 경계 엄격 관리** (`@Transactional` 내 외부 API 호출 금지)

---

#### 2.3 Persistence Layer 요구사항 (⭐ 데이터 무결성 강화)

**질문 템플릿** (기본):
```markdown
## 💾 Persistence Layer

어떤 데이터 저장소가 필요한가요?
- JPA Entity 목록은? (Domain과 1:1 매핑 권장)
- 복잡한 쿼리가 필요한가요? (QueryDSL 사용)
- 인덱스 전략은?
```

**⭐ 데이터 무결성 질문 가이드**:

```markdown
## 🔍 데이터 무결성 구체화 질문

### 🔢 주문 금액 계산

**계산 주체**:
- ❓ 주문 금액 계산은 **Application**? **DB Trigger**?
  - ✅ **권장**: Application에서 계산 (Domain 비즈니스 로직)
  - ❌ **비권장**: DB Trigger (비즈니스 로직 분산)

**금액 검증**:
- ❓ DB에 Constraint 필요? (CHECK 제약, 금액 > 0)
- ❓ 금액 불일치 시 예외 발생?

---

### 🔑 주문 번호 생성

**생성 전략**:
- ❓ UUID? Sequence? Custom 생성 로직?
- ❓ 주문 번호 Unique 제약 필요? (DB Constraint)
- ❓ 주문 번호 형식은? (ORD-20250113-0001 등)

---

### 🔒 동시성 제어

**Lock 전략**:
- ❓ Optimistic Lock? Pessimistic Lock?
  - **Optimistic Lock**: `@Version` 사용, 충돌 시 재시도
  - **Pessimistic Lock**: `SELECT FOR UPDATE`, 성능 저하 주의

**재고 차감**:
- ❓ 재고 차감 시 Lock 필요?
- ❓ 동시 주문 시 재고 부족 처리는?

---

### 📊 성능 최적화

**페이징**:
- ❓ 주문 조회 페이징은? (Offset? Cursor-based?)
- ❓ 대량 주문 조회 최적화 전략은?

**인덱스**:
- ❓ 주문 상태별 인덱스 필요? (status, created_at)
- ❓ 고객별 주문 조회 인덱스는? (customer_id, created_at)

**주문 이력**:
- ❓ 주문 이력 별도 테이블? (OrderHistory)
- ❓ 주문 수정 이력 관리 필요? (Audit Log)

```

**수집 항목**:
- JPA Entity 목록
- Repository 인터페이스 정의
- QueryDSL 쿼리 목록
- 인덱스 전략
- **동시성 제어 전략** (Optimistic/Pessimistic Lock)

**Zero-Tolerance 규칙 자동 체크**:
- ✅ Long FK 전략 (JPA 관계 어노테이션 금지)
- ✅ QueryDSL 최적화 (N+1 방지)

---

#### 2.4 REST API Layer 요구사항 (⭐ API 설계 구체화)

**질문 템플릿** (기본):
```markdown
## 🌐 REST API Layer

어떤 API 엔드포인트가 필요한가요?
- HTTP Method는? (POST, GET, PUT, DELETE)
- Request/Response DTO는?
- 인증/인가 전략은?
- 에러 처리 전략은?
```

**⭐ API 설계 질문 가이드**:

```markdown
## 🔍 API 설계 구체화 질문

### 🌐 RESTful 설계

**주문 생성 API**:
- ❓ `POST /api/orders` vs `POST /api/v1/orders`? (버전 관리 전략)
- ❓ Request Body 형식은? (JSON? XML?)
- ❓ Response Status Code는? (201 Created? 200 OK?)

**주문 조회 API**:
- ❓ `GET /api/orders/{orderId}` vs `GET /api/users/{userId}/orders/{orderId}`?
- ❓ 조회 권한 체크 필요? (본인 주문만 조회)

**주문 목록 조회 API**:
- ❓ `GET /api/orders?page=1&size=10` (페이징)
- ❓ `GET /api/orders?status=PLACED` (필터링)
- ❓ `GET /api/orders?sort=createdAt,desc` (정렬)

---

### 🔐 인증/인가

**인증 전략**:
- ❓ JWT? Session?
- ❓ Access Token + Refresh Token?
- ❓ Token 만료 시간은?

**인가 전략**:
- ❓ 주문 생성은 로그인 필수?
- ❓ 관리자 API 별도 필요? (`/api/admin/orders`)
- ❓ RBAC (Role-Based Access Control) 필요?

---

### ⚠️ Error Handling

**Error Response 형식**:
```json
{
  "errorCode": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다.",
  "timestamp": "2025-01-13T12:34:56Z",
  "path": "/api/orders/123"
}
```

**HTTP Status Code 전략**:
- ❓ 400 Bad Request: 유효성 검증 실패
- ❓ 401 Unauthorized: 인증 실패
- ❓ 403 Forbidden: 권한 없음
- ❓ 404 Not Found: 리소스 없음
- ❓ 409 Conflict: 비즈니스 규칙 위반 (재고 부족 등)
- ❓ 500 Internal Server Error: 서버 오류

---

### 📏 Validation

**Request Validation**:
- ❓ Bean Validation (`@NotNull`, `@Min`, `@Max`) 사용?
- ❓ Custom Validator 필요?
- ❓ Validation 실패 시 Error Response 형식은?

**예시**:
```java
public record PlaceOrderRequest(
    @NotNull Long customerId,
    @NotEmpty List<OrderLineRequest> orderLines,
    @NotBlank String deliveryAddress
) {}
```

```

**수집 항목**:
- API 엔드포인트 목록 (HTTP Method, Path)
- Request DTO 정의 (Validation 포함)
- Response DTO 정의
- HTTP 상태 코드 전략
- Exception Handling 전략
- **인증/인가 전략** (JWT, RBAC 등)

**Zero-Tolerance 규칙 자동 체크**:
- ✅ RESTful 설계 원칙
- ✅ 일관된 Error Response 형식

---

### 3. 제약사항 및 테스트 전략

**질문 템플릿**:
```markdown
## ⚠️ 제약사항

비기능 요구사항은?
- 성능 목표는? (응답 시간, TPS)
- 보안 요구사항은?
- 확장성 요구사항은?

## 🧪 테스트 전략

테스트 범위는?
- Unit Test 범위는?
- Integration Test 범위는?
- E2E Test가 필요한가요?
```

**수집 항목**:
- 성능 요구사항 (응답 시간, TPS)
- 보안 요구사항 (인증/인가, 데이터 암호화)
- 확장성 요구사항 (동시 사용자, 데이터 크기)
- Unit Test 전략
- Integration Test 전략

---

### 4. PRD 문서 생성

수집한 정보를 바탕으로 구조화된 PRD 문서를 생성합니다.

**문서 경로**: `docs/prd/{feature-name-kebab-case}.md`

**문서 구조**:
```markdown
# PRD: {Feature Name}

**작성일**: {YYYY-MM-DD}
**작성자**: {사용자명}
**상태**: Draft

---

## 📋 프로젝트 개요

### 비즈니스 목적
{수집된 비즈니스 목적}

### 주요 사용자
{수집된 사용자 정보}

### 성공 기준
{수집된 성공 기준}

---

## 🏗️ Layer별 요구사항

### 1. Domain Layer

#### Aggregate: Order

**속성**:
- orderId: OrderId (Value Object)
- customerId: Long (FK)
- productId: Long (FK)
- status: OrderStatus (Enum: PENDING, PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- totalPrice: BigDecimal

**비즈니스 규칙** (⭐ 구체화):
1. **주문 생성**:
   - 재고 확인: 주문 생성 시 즉시 확인 (재고 부족 시 예외 발생)
   - 결제 처리: 비동기 (Orchestration Pattern)
   - 주문 수량 제한: 최소 1개, 최대 100개
   - 주문 번호: UUID v4 사용

2. **주문 취소**:
   - 취소 가능 상태: PLACED, CONFIRMED만 가능 (SHIPPED는 불가)
   - 환불 처리: 비동기 (Orchestration Pattern)
   - 취소 수수료: PLACED (0원), CONFIRMED (1,000원)
   - 재고 복구: 취소 시 즉시 복구

3. **주문 수정**:
   - 수정 가능 항목: 수량, 배송지만 가능 (상품 추가/삭제 불가)
   - 수정 가능 상태: PLACED만 가능
   - 수정 이력: OrderHistory 테이블에 기록

4. **상태 전환**:
   ```
   PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED
                ↓
           CANCELLED (PLACED/CONFIRMED에서만 가능)
   ```

**Value Objects**:
- **OrderId**: UUID 기반 주문 번호
- **OrderStatus**: Enum (PENDING, PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)

**Zero-Tolerance 규칙 준수**:
- ✅ Law of Demeter (Getter 체이닝 금지)
  - `order.getCustomerAddress()` (O)
  - `order.getCustomer().getAddress()` (X)
- ✅ Lombok 금지 (Pure Java/Record 사용)
- ✅ Long FK 전략 (JPA 관계 어노테이션 금지)

---

### 2. Application Layer

#### Command UseCase

**PlaceOrderUseCase**:
- **Input**: `PlaceOrderCommand(customerId, productId, quantity, deliveryAddress)`
- **Output**: `OrderResponse(orderId, status, totalPrice)`
- **Transaction**: Yes (주문 생성 + 재고 차감만)
  - ⚠️ **결제 API 호출은 트랜잭션 밖** (Orchestration Pattern 사용)
- **비즈니스 로직**:
  1. 재고 확인 (재고 부족 시 예외)
  2. 주문 생성 (Domain Aggregate)
  3. 재고 차감
  4. **트랜잭션 커밋**
  5. 결제 요청 (Orchestration Pattern - 비동기)

**CancelOrderUseCase**:
- **Input**: `CancelOrderCommand(orderId, reason)`
- **Output**: `OrderResponse(orderId, status)`
- **Transaction**: Yes (주문 취소 + 재고 복구만)
  - ⚠️ **환불 API 호출은 트랜잭션 밖** (Orchestration Pattern 사용)
- **비즈니스 로직**:
  1. 주문 조회 (존재하지 않으면 예외)
  2. 취소 가능 여부 확인 (상태 체크)
  3. 주문 취소 (Domain 메서드)
  4. 재고 복구
  5. **트랜잭션 커밋**
  6. 환불 요청 (Orchestration Pattern - 비동기)

#### Query UseCase

**GetOrderUseCase**:
- **Input**: `GetOrderQuery(orderId)`
- **Output**: `OrderDetailResponse(orderId, customerId, status, totalPrice, orderLines, ...)`
- **Transaction**: ReadOnly
- **권한 체크**: 본인 주문만 조회 가능

**ListOrdersUseCase**:
- **Input**: `ListOrdersQuery(customerId, status, page, size)`
- **Output**: `PageResponse<OrderSummaryResponse>`
- **Transaction**: ReadOnly
- **페이징**: Cursor-based Pagination (성능 최적화)

#### Zero-Tolerance 규칙 준수
- ✅ Command/Query 분리 (CQRS)
- ✅ **Transaction 경계 엄격 관리** (외부 API 호출은 트랜잭션 밖)

---

### 3. Persistence Layer

#### JPA Entity

**OrderJpaEntity**:
- **테이블**: `orders`
- **필드**:
  - `id`: Long (PK, Auto Increment)
  - `order_id`: String (UUID, Unique, Not Null)
  - `customer_id`: Long (FK, Not Null, Index)
  - `product_id`: Long (FK, Not Null)
  - `status`: String (Not Null, Index)
  - `total_price`: BigDecimal (Not Null, CHECK > 0)
  - `created_at`: LocalDateTime (Not Null, Index)
  - `updated_at`: LocalDateTime (Not Null)
- **인덱스**:
  - `idx_customer_id_created_at` (customer_id, created_at DESC) - 고객별 주문 조회
  - `idx_status_created_at` (status, created_at DESC) - 상태별 주문 조회
- **Unique Constraint**:
  - `order_id` (UUID 중복 방지)

**OrderHistoryJpaEntity**:
- **테이블**: `order_history`
- **필드**:
  - `id`: Long (PK, Auto Increment)
  - `order_id`: String (FK, Not Null)
  - `action`: String (CREATED, CANCELLED, MODIFIED, Not Null)
  - `reason`: String (Nullable)
  - `created_at`: LocalDateTime (Not Null)

#### Repository

**OrderJpaRepository**:
```java
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderId(String orderId);
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
```

**OrderQueryDslRepository**:
- **메서드**: `findByCustomerIdAndStatus(customerId, status, Pageable)`
- **최적화**: DTO Projection (N+1 방지)

#### Zero-Tolerance 규칙 준수
- ✅ Long FK 전략 (관계 어노테이션 금지)
  - `private Long customerId;` (O)
  - `@ManyToOne private Customer customer;` (X)
- ✅ QueryDSL 최적화 (N+1 방지)

---

### 4. REST API Layer

#### API 엔드포인트

| Method | Path | Description | Request DTO | Response DTO | Status Code |
|--------|------|-------------|-------------|--------------|-------------|
| POST | /api/v1/orders | 주문 생성 | PlaceOrderRequest | OrderResponse | 201 Created |
| GET | /api/v1/orders/{orderId} | 주문 조회 | - | OrderDetailResponse | 200 OK |
| GET | /api/v1/orders | 주문 목록 조회 | ListOrdersRequest | PageResponse<OrderSummaryResponse> | 200 OK |
| POST | /api/v1/orders/{orderId}/cancel | 주문 취소 | CancelOrderRequest | OrderResponse | 200 OK |
| PATCH | /api/v1/orders/{orderId} | 주문 수정 | UpdateOrderRequest | OrderResponse | 200 OK |

#### Request/Response DTO

**PlaceOrderRequest**:
```java
public record PlaceOrderRequest(
    @NotNull Long customerId,
    @NotNull Long productId,
    @Min(1) @Max(100) Integer quantity,
    @NotBlank String deliveryAddress
) {}
```

**OrderResponse**:
```java
public record OrderResponse(
    String orderId,
    OrderStatus status,
    BigDecimal totalPrice,
    LocalDateTime createdAt
) {}
```

**Error Response**:
```json
{
  "errorCode": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다.",
  "timestamp": "2025-01-13T12:34:56Z",
  "path": "/api/v1/orders/invalid-order-id"
}
```

#### 인증/인가

- **인증**: JWT (Access Token + Refresh Token)
- **Access Token 만료**: 1시간
- **Refresh Token 만료**: 7일
- **권한**: 로그인 필수 (주문 생성, 조회, 취소 모두)
- **본인 확인**: 주문 조회 시 본인 주문만 조회 가능

#### Zero-Tolerance 규칙 준수
- ✅ RESTful 설계 원칙
- ✅ 일관된 Error Response 형식

---

## ⚠️ 제약사항

### 비기능 요구사항

**성능**:
- 주문 생성 응답 시간: < 500ms (P95)
- 주문 조회 응답 시간: < 100ms (P95)
- TPS: 1,000 requests/sec (Peak Time)

**보안**:
- JWT 인증 필수
- HTTPS 통신 (TLS 1.2+)
- 주문 금액 암호화 (DB 저장 시)

**확장성**:
- 동시 사용자: 10,000명
- 주문 데이터 크기: 1,000만 건 (1년)

---

## 🧪 테스트 전략

### Unit Test

**Domain**:
- Order Aggregate 비즈니스 로직 (placeOrder, cancelOrder 등)
- OrderStatus Enum 상태 전환 로직
- Value Object (OrderId) 생성 및 검증

**Application**:
- PlaceOrderUseCase (Mock PersistencePort)
- CancelOrderUseCase (Mock PersistencePort)

### Integration Test

**Persistence**:
- OrderJpaRepository CRUD 테스트 (TestContainers MySQL)
- OrderQueryDslRepository 복잡한 쿼리 테스트

**REST API**:
- OrderApiController (MockMvc)
- Validation 테스트 (400 Bad Request)
- 인증/인가 테스트 (401 Unauthorized, 403 Forbidden)

### E2E Test

- 주문 생성 → 조회 → 취소 플로우
- 동시 주문 생성 (동시성 테스트)

---

## 🚀 개발 계획

### Phase 1: Domain Layer (예상: 3일)
- [ ] Order Aggregate 구현
- [ ] OrderId, OrderStatus Value Object 구현
- [ ] Domain Unit Test (TestFixture 패턴)

### Phase 2: Application Layer (예상: 4일)
- [ ] PlaceOrderUseCase 구현 (Mock PersistencePort)
- [ ] CancelOrderUseCase 구현
- [ ] Command/Query DTO 구현
- [ ] Application Unit Test

### Phase 3: Persistence Layer (예상: 3일)
- [ ] OrderJpaEntity 구현
- [ ] OrderJpaRepository 구현
- [ ] QueryDSL 쿼리 구현
- [ ] Integration Test (TestContainers)

### Phase 4: REST API Layer (예상: 3일)
- [ ] OrderApiController 구현
- [ ] Request/Response DTO 구현
- [ ] Exception Handling 구현
- [ ] REST API Integration Test (MockMvc)

### Phase 5: Integration Test (예상: 1일)
- [ ] End-to-End Test 작성
- [ ] 동시성 테스트

---

## 📚 참고 문서

- [Domain Layer 규칙](../../docs/coding_convention/02-domain-layer/)
- [Application Layer 규칙](../../docs/coding_convention/03-application-layer/)
- [Persistence Layer 규칙](../../docs/coding_convention/04-persistence-layer/)
- [REST API Layer 규칙](../../docs/coding_convention/01-adapter-rest-api-layer/)

---

**다음 단계**: `/jira-from-prd docs/prd/{feature-name-kebab-case}.md`
```

---

### 5. kentback TDD 계획 제안 (선택사항)

PRD 생성 후, kentback TDD 계획을 제안합니다:

```markdown
## 🎯 kentback TDD 계획 제안

이 PRD를 기반으로 kentback TDD 사이클을 시작할 수 있습니다:

**Domain Layer TDD**:
1. **RED**: Law of Demeter 테스트 작성
2. **GREEN**: Domain Aggregate 구현
3. **REFACTOR**: Java 21 Record 패턴 적용

**Application Layer TDD**:
1. **RED**: Transaction 경계 테스트 작성
2. **GREEN**: UseCase 구현
3. **REFACTOR**: Command/Query 분리 최적화

커맨드: `/kentback-plan docs/prd/{feature-name-kebab-case}.md`
```

---

## 대화 흐름 예시

```
사용자: /create-prd "Order Management"

Claude:
📋 Order Management 기능 PRD를 생성하겠습니다!

## 1단계: 프로젝트 개요

이 기능의 핵심 목적은 무엇인가요?
- 어떤 비즈니스 문제를 해결하나요?
- 주요 사용자는 누구인가요?
- 성공 기준은 무엇인가요?

사용자:
- 비즈니스 문제: 주문 관리 프로세스 자동화
- 사용자: 고객, 관리자
- 성공 기준: 주문 생성 응답 시간 < 500ms, 주문 취소율 < 5%

Claude:
✅ 프로젝트 개요 수집 완료

## 2단계: Domain Layer

핵심 비즈니스 개념(Aggregate)은 무엇인가요?
- 어떤 엔티티가 필요한가요?

사용자:
Order, Customer, Product

Claude:
좋습니다! 이제 **주문 생성**에 대해 구체적으로 질문할게요:

### 📦 주문 생성 (Order Creation)

**재고 관리**:
- ❓ 재고 확인은 즉시? 나중에?
- ❓ 재고 차감 시점은? (주문 생성 시? 결제 완료 시?)
- ❓ 재고 부족 시 처리는? (주문 거절? 대기? 부분 배송?)

**결제 처리**:
- ❓ 결제는 동기? 비동기?
- ❓ 결제 실패 시 주문 상태는?

**주문 제약사항**:
- ❓ 주문 수량 제한은?
- ❓ 동일 상품 중복 주문 가능?

사용자:
- 재고 확인: 즉시 (재고 부족 시 주문 거절)
- 재고 차감: 주문 생성 시
- 결제: 비동기 (Orchestration Pattern)
- 결제 실패: 주문 상태 PENDING 유지
- 주문 수량 제한: 최소 1개, 최대 100개
- 중복 주문: 가능

Claude:
✅ 주문 생성 비즈니스 규칙 수집 완료!

이제 **주문 취소**에 대해 질문할게요:

### 🚫 주문 취소 (Order Cancellation)

**취소 가능 조건**:
- ❓ 어느 상태까지 취소 가능?
- ❓ 부분 취소 가능?

**환불 처리**:
- ❓ 취소 시 환불은 즉시? 승인 필요?
- ❓ 취소 수수료는?

...

Claude:
✅ PRD 문서 생성 완료!

파일: docs/prd/order-management.md

**구체화된 비즈니스 규칙**:
- 주문 생성: 재고 즉시 확인, 비동기 결제, 수량 제한 1-100
- 주문 취소: PLACED/CONFIRMED만 가능, 취소 수수료 상태별 차등
- Transaction 경계: 외부 API 호출은 트랜잭션 밖

다음 단계:
1. PRD 검토 및 수정
2. `/jira-from-prd docs/prd/order-management.md` - Jira 티켓 생성
3. kentback TDD 사이클 시작
```

---

## 에러 처리

- **docs/prd/ 디렉토리 없음**: 자동 생성
- **중복 PRD 파일**: 사용자에게 덮어쓰기 확인
- **불완전한 답변**: 추가 질문으로 보완

---

## Write 도구 사용

PRD 문서 생성 시 Write 도구를 사용합니다:
```
Write 도구:
- file_path: docs/prd/{feature-name-kebab-case}.md
- content: {위 PRD 구조 기반 생성}
```

---

## 추가 기능

- PRD 템플릿 커스터마이징 지원
- Layer별 우선순위 설정 (선택적 질문)
- 기존 PRD 확장 (v2, v3 등)
- PRD 요약 (1-page Executive Summary)

---

## 사용 예시

```bash
/create-prd "Order Management"
/create-prd "User Authentication"
/create-prd "Product Catalog"
```

---

## 출력 형식

```markdown
✅ PRD 생성 완료!

**파일**: docs/prd/{feature-name-kebab-case}.md
**Layer**: Domain, Application, Persistence, REST API
**비즈니스 규칙**: 구체화 완료 (재고, 결제, Transaction 경계 등)
**Zero-Tolerance 규칙**: 자동 체크 완료

**다음 단계**:
1. PRD 검토 및 수정
2. `/jira-from-prd docs/prd/{feature-name-kebab-case}.md`
3. `/kentback-plan docs/prd/{feature-name-kebab-case}.md` (선택)
```
