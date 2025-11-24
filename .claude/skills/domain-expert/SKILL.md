---
name: domain-expert
description: DDD Aggregate Root 설계, Law of Demeter 적용, Tell Don't Ask 패턴 강제. /kb-domain 명령 시 자동 활성화.
triggers: [/kb-domain, /go, /red, /green, /refactor, /tidy, aggregate, domain, value object, factory method]
---

# Domain Layer 전문가

## 핵심 원칙 (Zero-Tolerance)

### ✅ Mandatory
1. **Aggregate Root** - 모든 접근은 Root 통해서만
2. **Factory Method** - `Order.create()` 사용
3. **Value Object Immutable** - 불변성 보장
4. **Business Method** - `order.place()`, `order.cancel()`
5. **No Getter 체이닝** - `order.getCustomer().getAddress()` 금지
6. **No Lombok** - Pure Java getter/setter
7. **Domain Event** - 상태 변경 시 발행

### ❌ Prohibited
1. **Getter 체이닝** - Tell, Don't Ask 위반
2. **Public Setter** - Business Method로 상태 변경
3. **JPA 어노테이션** - Domain은 순수 Java
4. **Lombok** - `@Data`, `@Builder` 금지
5. **Service 로직 침범** - UseCase 로직 Domain 진입 금지
6. **DB 의존성** - Infrastructure 독립
7. **Primitive Obsession** - Value Object 사용

## 예시

### ✅ CORRECT: Tell, Don't Ask
```java
order.place();
order.cancel();
order.confirm();
```

### ❌ WRONG: Getter 체이닝
```java
// 금지
String zip = order.getCustomer().getAddress().getZipCode();

// ✅ 올바름
String zip = order.getCustomerZipCode();
```

## 참조

- **전체 가이드**: [Domain Guide](../../../docs/coding_convention/02-domain-layer/domain-guide.md)
- **Aggregate 설계**: [Aggregate Guide](../../../docs/coding_convention/02-domain-layer/aggregate/aggregate-guide.md)
- **상세 규칙 + 템플릿**: [REFERENCE.md](./REFERENCE.md)
- **Law of Demeter**: docs/coding_convention/02-domain-layer/law-of-demeter/

## 자동 활성화

`/kb-domain /go|red|green|refactor|tidy` 실행 시 자동 활성화.
