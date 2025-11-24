# Domain Layer — **DDD Aggregate & Pure Java**

> 이 문서는 `domain-layer`에 대한 **요약 가이드**
>
> **핵심 원칙**, **패키징 구조**, 그리고 각 영역별 **상세 가이드 링크**를 제공합니다.

---

## 1) 핵심 원칙 (한눈에)

* **Pure Java 원칙**: Lombok, MapStruct 등 코드 생성 라이브러리 **절대 금지**. 모든 코드는 명시적으로 작성.
* **Law of Demeter 엄수**: Getter 체이닝 금지 (`order.getCustomer().getAddress()` ❌). **Tell, Don't Ask** 패턴 적용.
* **Aggregate 중심 설계**: 트랜잭션 일관성 경계 = Aggregate. Root를 통한 접근만 허용.
* **불변성 우선**: 상태 변경은 **명시적 비즈니스 메서드**로만. Setter 금지.
* **캡슐화 철저**: 내부 상태 보호. 외부에서 판단·계산 금지, 도메인이 스스로 결정.
* **기술 독립성**: JPA, Spring 등 기술적 의존성 **절대 금지**. 순수 비즈니스 로직만.
* **도메인 이벤트 (옵션)**: 필요 시 상태 변경 시 이벤트 발행으로 타 Aggregate와 느슨한 결합.
* **생성 메서드 패턴**: 정적 팩토리 메서드 3종 - `forNew()` (새 생성), `of()` (기존 값), `reconstitute()` (재구성).


### 금지사항

* **Lombok 전면 금지**: `@Data`, `@Builder`, `@Getter`, `@Setter`, `@AllArgsConstructor` 등 모든 어노테이션
* **Getter 체이닝**: `order.getCustomer().getAddress().getZipCode()` 같은 연쇄 호출
* **Setter 메서드**: 상태 변경은 `changeXxx()`, `updateXxx()` 같은 비즈니스 메서드로만
* **빈혈 모델**: 비즈니스 로직 없는 단순 데이터 홀더
* **기술 의존성**: JPA 어노테이션(`@Entity`, `@Table`), Spring 어노테이션(`@Component` 등)
* **외부 판단 로직**: `if (order.getStatus() == PAID)` 같이 외부에서 상태 판단

---

## 2) 패키징 구조 (Bounded Context 예)

```
domain/
├─ common/
│  ├─ DomainEvent.java           # 도메인 이벤트 인터페이스
│  ├─ ErrorCode.java              # 공통 에러 코드 enum
│  └─ DomainException.java        # 기본 도메인 예외
│
└─ [boundedContext]/              # 예: order
   ├─ aggregate/
   │  └─ [aggregateName]/         # 예: order (Aggregate Root 이름 소문자)
   │     ├─ Order.java            # ← Aggregate Root (forNew, of, reconstitute 포함)
   │     └─ OrderLineItem.java    # ← Entity (Root에 종속)
   │
   ├─ vo/                          # Value Object
   │  ├─ OrderId.java
   │  ├─ Money.java
   │  └─ OrderStatus.java
   │
   ├─ event/                       # Domain Event (옵션)
   │  └─ OrderCreatedEvent.java
   │
   └─ exception/                   # BC 전용 예외
      ├─ OrderNotFoundException.java
      └─ InvalidOrderStateException.java
```

**패키지 네이밍 규칙**:
* Bounded Context: 소문자, 단수형 (`order`, `product`)
* Aggregate 폴더명: 소문자, 단수형 (`order`, `cart`)
* 클래스명: PascalCase, 명확한 역할 표현 (`Order`, `OrderLineItem`, `OrderId`)

**패키지 배치 원칙**:
* `aggregate/`: **Aggregate Root + 종속 Entity만**. 트랜잭션 일관성 경계.
* `vo/`: 불변 값 객체. ID, Money, Status 등.
* `event/`: 도메인 이벤트 (필요 시).
* `exception/`: BC 전용 예외.

---

## 3) 영역별 상세 가이드 링크

### 📦 **Aggregate 설계**
* [Aggregate Guide](./aggregate/aggregate-guide.md) - Aggregate Root 설계 가이드
* [Aggregate Test Guide](./aggregate/aggregate-test-guide.md) - Aggregate Root 테스트 가이드
* [Aggregate ArchUnit Guide](./aggregate/aggregate-archunit.md) - Aggregate Root ArchUnit 검증 가이드

### 🎁 **Value Object 설계**
* [Value Object Guide](./vo/vo-guide.md) - Value Object 설계 가이드
* [Value Object Test Guide](./vo/vo-test-guide.md) - Value Object 테스트 가이드
* [Value Object ArchUnit Guide](./vo/vo-archunit.md) - Value Object ArchUnit 검증 가이드

### 🚨 **Exception 설계**
* [Exception Guide](./exception/exception-guide.md) - Domain Layer 예외 설계 가이드
* [Exception Test Guide](./exception/exception-test-guide.md) - Domain Layer 예외 테스트 가이드
* [Exception ArchUnit Guide](./exception/exception-archunit-guide.md) - Domain Layer 예외 ArchUnit 검증 가이드

### 📢 **도메인 이벤트 (옵션)**
* TBD (작업 예정)

---
