# Sample Bounded Context (예시)

이 패키지는 **Bounded Context 패턴의 예시**입니다.

## 📋 사용 방법

### 1. 이 패키지를 참고하여 실제 도메인 생성

```
domain/
└── com/ryuqq/domain/
    ├── sample/          ← 이 예시를 참고
    ├── order/           ← 실제 주문 도메인
    ├── customer/        ← 실제 고객 도메인
    ├── product/         ← 실제 상품 도메인
    └── common/          ← 공통 (유지)
```

### 2. 패키지 구조 복사

```bash
# 예시: order 도메인 생성
cp -r domain/sample domain/order

# 파일명 변경
# SampleOrder.java → Order.java
# SampleOrderId.java → OrderId.java
```

### 3. 클래스명 및 패키지명 변경

```java
// Before
package com.ryuqq.domain.sample.aggregate;
public class SampleOrder implements AggregateRoot { }

// After
package com.ryuqq.domain.order.aggregate;
public class Order implements AggregateRoot { }
```

### 4. 비즈니스 로직 구현

각 TODO 주석을 참고하여 실제 비즈니스 로직으로 교체하세요.

---

## 🏗️ Bounded Context 구조

### Aggregate
- **SampleOrder**: Aggregate Root (주문)
- **SampleOrderItem**: Entity (주문 항목)

### Value Objects
- **SampleOrderId**: Order 식별자
- **SampleOrderItemId**: OrderItem 식별자
- **SampleMoney**: 금액 (여러 Context에서 공통 사용 가능)

### Domain Events
- **OrderPlacedEvent**: 주문 생성 이벤트

### Domain Exceptions
- **OrderNotFoundException**: 주문 미발견 예외

---

## 🎯 Bounded Context 원칙

### 1. Context 간 참조는 ID로만
```java
// ✅ 올바른 참조
public class SampleOrder {
    private final CustomerId customerId;  // ID만 참조
}

// ❌ 잘못된 참조
public class SampleOrder {
    private final Customer customer;  // 객체 직접 참조 금지
}
```

### 2. Context 내부 응집도 유지
- 같은 Context의 클래스들은 높은 응집도
- 다른 Context와는 느슨한 결합

### 3. ArchUnit으로 경계 검증
```java
@ArchTest
public static final ArchRule contexts_should_not_depend_on_other_aggregates =
    noClasses()
        .that().resideInPackage("..domain.order..")
        .should().dependOnClassesThat()
            .resideInPackage("..domain.customer.aggregate..");
```

---

## 🚀 시작하기

1. **sample 패키지 탐색**: 각 파일의 구조와 패턴 학습
2. **실제 도메인 생성**: 프로젝트 요구사항에 맞게 Bounded Context 정의
3. **sample 패키지 삭제**: 실제 도메인 구현 후 제거

---
