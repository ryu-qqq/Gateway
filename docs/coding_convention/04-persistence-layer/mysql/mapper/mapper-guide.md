# JPA Entity Mapper 가이드

> **목적**: JPA Entity와 Domain 간 변환 Mapper 패턴 및 규칙

---

## 1️⃣ Mapper란?

### 정의
**Domain과 Mapper와 JPA Entity**

Persistence Layer와 Domain Layer 사이의 변환을 담당하는 컴포넌트입니다.

### 책임
- ✅ Domain → Entity 변환 (저장 시 사용)
- ✅ Entity → Domain 변환 (조회 시 사용)
- ✅ Value Object 추출 및 재구성
- ❌ **비즈니스 로직 포함 금지**
- ❌ **검증 로직 포함 금지** (Domain Layer에서)

### 헥사고날 아키텍처
```
Application Layer (Domain 사용)
  ↓
Mapper (변환 담당)
  ↓
Persistence Layer (Entity 사용)
  ↓
Database (MySQL)
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: 단순 변환만 담당

```java
// ✅ 단순 변환 (필드 매핑)
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    return ExampleJpaEntity.of(
        domain.getId(),
        domain.getMessage(),
        domain.status(),
        domain.getCreatedAt(),
        domain.getUpdatedAt()
    );
}

// ❌ 비즈니스 로직 포함 금지
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    if (domain.isExpired()) {  // ❌ 비즈니스 검증 금지!
        throw new BusinessException("만료된 도메인");
    }
    return ExampleJpaEntity.of(...);
}

// ❌ 검증 로직 포함 금지
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    ExampleJpaEntity entity = ExampleJpaEntity.of(...);
    entity.markAsUpdated();  // ❌ 상태 변경 금지! (메서드도 없어야 함)
    return entity;
}
```

### 원칙 2: Entity.of() 메서드 사용

```java
// ✅ Entity.of() 스태틱 메서드 사용
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    return ExampleJpaEntity.of(
        domain.getId(),
        domain.getMessage(),
        domain.status(),
        domain.getCreatedAt(),
        domain.getUpdatedAt()
    );
}

// ❌ new 생성자 직접 호출 금지
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    return new ExampleJpaEntity(  // ❌ private 생성자라서 불가능!
        domain.getId(),
        domain.getMessage(),
        domain.status(),
        domain.getCreatedAt(),
        domain.getUpdatedAt()
    );
}
```

**핵심 이유**:
- Entity의 생성자는 `private`으로 숨김
- `of()` 메서드만 public으로 노출
- 생성 방식 변경 시에도 of() 메서드만 수정

### 원칙 3: Domain.of() 또는 reconstitute() 사용

```java
// ✅ Domain.of() 또는 reconstitute() 사용
public ExampleDomain toDomain(ExampleJpaEntity entity) {
    return ExampleDomain.of(
        entity.getId(),
        entity.getMessage(),
        entity.getStatus().asString(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
}

// 또는 reconstitute() (DB에서 재구성)
public ExampleDomain toDomain(ExampleJpaEntity entity) {
    return ExampleDomain.reconstitute(
        ExampleId.of(entity.getId()),
        ExampleContent.of(entity.getMessage()),
        entity.getStatus(),
        ExampleAudit.of(entity.getCreatedAt(), entity.getUpdatedAt())
    );
}
```

### 원칙 4: 시간 필드 직접 전달

```java
// ✅ createdAt, updatedAt 직접 전달
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    return ExampleJpaEntity.of(
        domain.getId(),
        domain.getMessage(),
        domain.status(),
        domain.getCreatedAt(),      // ✅ Domain → Entity
        domain.getUpdatedAt()       // ✅ Domain → Entity
    );
}

// ❌ LocalDateTime.now() 사용 금지
public ExampleJpaEntity toEntity(ExampleDomain domain) {
    return ExampleJpaEntity.of(
        domain.getId(),
        domain.getMessage(),
        domain.status(),
        LocalDateTime.now(),  // ❌ Mapper에서 시간 생성 금지!
        LocalDateTime.now()   // ❌ Domain에서 이미 관리함
    );
}
```

**핵심 이유**:
- 시간 생성은 Domain Layer의 책임
- Mapper는 단순 변환만
- 일관성 보장 (동일 시간 보장)

### 원칙 5: @Component로 Spring Bean 등록

```java
// ✅ @Component 사용
@Component
public class ExampleEntityMapper {
    public ExampleJpaEntity toEntity(ExampleDomain domain) { ... }
    public ExampleDomain toDomain(ExampleJpaEntity entity) { ... }
}

// ❌ Utility 클래스 금지
public class ExampleEntityMapper {
    private ExampleEntityMapper() { }  // ❌ static 메서드 금지

    public static ExampleJpaEntity toEntity(ExampleDomain domain) { ... }
    public static ExampleDomain toDomain(ExampleJpaEntity entity) { ... }
}
```

**핵심 이유**:
- Spring Bean으로 주입 가능
- 의존성 관리 용이
- 테스트 작성 용이 (Mock 가능)

---

## 3️⃣ 템플릿 패턴

### 템플릿 1: BaseAuditEntity 상속 경우

```java
package com.company.adapter.out.persistence.{module}.mapper;

import com.company.adapter.out.persistence.{module}.entity.{Domain}JpaEntity;
import com.company.domain.{module}.{Domain};

import org.springframework.stereotype.Component;

/**
 * {Domain}JpaEntityMapper - Entity ↔ Domain 변환 Mapper
 *
 * <p>Persistence Layer의 JPA Entity와 Domain Layer의 Domain 객체 간 변환을 담당합니다.</p>
 *
 * <p><strong>변환 책임:</strong></p>
 * <ul>
 *   <li>{Domain} → {Domain}JpaEntity (저장용)</li>
 *   <li>{Domain}JpaEntity → {Domain} (조회용)</li>
 *   <li>Value Object 추출 및 재구성</li>
 * </ul>
 *
 * <p><strong>Hexagonal Architecture 관점:</strong></p>
 * <ul>
 *   <li>Adapter Layer의 책임</li>
 *   <li>Domain과 Infrastructure 기술 분리</li>
 *   <li>Domain은 JPA 의존성 없음</li>
 * </ul>
 *
 * @author {author}
 * @since 1.0.0
 */
@Component
public class {Domain}JpaEntityMapper {

    /**
     * Domain → Entity 변환
     *
     * <p><strong>사용 시나리오:</strong></p>
     * <ul>
     *   <li>신규 {Domain} 저장 (ID가 null)</li>
     *   <li>기존 {Domain} 수정 (ID가 있음)</li>
     * </ul>
     *
     * <p><strong>변환 규칙:</strong></p>
     * <ul>
     *   <li>ID: Domain.getId() → Entity.id</li>
     *   <li>{Field}: Domain.get{Field}() → Entity.{field}</li>
     *   <li>CreatedAt: Domain.getCreatedAt() → Entity.createdAt</li>
     *   <li>UpdatedAt: Domain.getUpdatedAt() → Entity.updatedAt</li>
     * </ul>
     *
     * @param domain {Domain} 도메인
     * @return {Domain}JpaEntity
     */
    public {Domain}JpaEntity toEntity({Domain} domain) {
        return {Domain}JpaEntity.of(
            domain.getId(),
            domain.get{Field}(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }

    /**
     * Entity → Domain 변환
     *
     * <p><strong>사용 시나리오:</strong></p>
     * <ul>
     *   <li>데이터베이스에서 조회한 Entity를 Domain으로 변환</li>
     *   <li>Application Layer로 전달</li>
     * </ul>
     *
     * <p><strong>변환 규칙:</strong></p>
     * <ul>
     *   <li>ID: Entity.id → Domain.{Domain}Id</li>
     *   <li>{Field}: Entity.{field} → Domain.{Field}</li>
     *   <li>CreatedAt/UpdatedAt: Entity → Domain.Audit</li>
     * </ul>
     *
     * @param entity {Domain}JpaEntity
     * @return {Domain} 도메인
     */
    public {Domain} toDomain({Domain}JpaEntity entity) {
        return {Domain}.reconstitute(
            {Domain}Id.of(entity.getId()),
            {Domain}{Field}.of(entity.get{Field}()),
            {Domain}Audit.of(entity.getCreatedAt(), entity.getUpdatedAt())
        );
    }
}
```

### 템플릿 2: SoftDeletableEntity 상속 경우

```java
package com.company.adapter.out.persistence.{module}.mapper;

import com.company.adapter.out.persistence.{module}.entity.{Domain}JpaEntity;
import com.company.domain.{module}.{Domain};

import org.springframework.stereotype.Component;

/**
 * {Domain}JpaEntityMapper - Entity ↔ Domain 변환 Mapper (Soft Delete 지원)
 *
 * <p>SoftDeletableEntity 상속 시 deletedAt 필드를 함께 변환합니다.</p>
 *
 * @author {author}
 * @since 1.0.0
 */
@Component
public class {Domain}JpaEntityMapper {

    /**
     * Domain → Entity 변환 (Soft Delete 지원)
     *
     * <p><strong>deletedAt 처리:</strong></p>
     * <ul>
     *   <li>Domain의 isDeleted() 확인</li>
     *   <li>삭제되었다면 deletedAt 전달</li>
     *   <li>아니면 null 전달</li>
     * </ul>
     *
     * @param domain {Domain} 도메인
     * @return {Domain}JpaEntity
     */
    public {Domain}JpaEntity toEntity({Domain} domain) {
        return {Domain}JpaEntity.of(
            domain.getId(),
            domain.get{Field}(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()  // ✅ deletedAt 전달
        );
    }

    /**
     * Entity → Domain 변환 (Soft Delete 지원)
     *
     * <p><strong>deletedAt 처리:</strong></p>
     * <ul>
     *   <li>Entity.deletedAt → Domain.deletedAt</li>
     *   <li>Domain의 isDeleted() 메서드가 이를 확인</li>
     * </ul>
     *
     * @param entity {Domain}JpaEntity
     * @return {Domain} 도메인
     */
    public {Domain} toDomain({Domain}JpaEntity entity) {
        return {Domain}.reconstitute(
            {Domain}Id.of(entity.getId()),
            {Domain}{Field}.of(entity.get{Field}()),
            {Domain}Audit.of(
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()  // ✅ deletedAt 전달
            )
        );
    }
}
```

### 템플릿 3: 상속 없음 (생성/수정 시간 미관리)

```java
package com.company.adapter.out.persistence.{module}.mapper;

import com.company.adapter.out.persistence.{module}.entity.{Domain}JpaEntity;
import com.company.domain.{module}.{Domain};

import org.springframework.stereotype.Component;

/**
 * {Domain}JpaEntityMapper - Entity ↔ Domain 변환 Mapper (감사 정보 없음)
 *
 * <p>감사 정보 필드가 없는 간단한 엔티티입니다.</p>
 *
 * @author {author}
 * @since 1.0.0
 */
@Component
public class {Domain}JpaEntityMapper {

    /**
     * Domain → Entity 변환 (감사 정보 없음)
     *
     * @param domain {Domain} 도메인
     * @return {Domain}JpaEntity
     */
    public {Domain}JpaEntity toEntity({Domain} domain) {
        return {Domain}JpaEntity.of(
            domain.getId(),
            domain.get{Field}()
        );
    }

    /**
     * Entity → Domain 변환 (감사 정보 없음)
     *
     * @param entity {Domain}JpaEntity
     * @return {Domain} 도메인
     */
    public {Domain} toDomain({Domain}JpaEntity entity) {
        return {Domain}.reconstitute(
            {Domain}Id.of(entity.getId()),
            {Domain}{Field}.of(entity.get{Field}())
        );
    }
}
```

---

## 4️⃣ 실전 예시

### 예시 1: Order Mapper (BaseAuditEntity)

```java
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getOrderNumber(),
            domain.getUserId(),
            domain.getTotalAmount(),
            domain.getStatus(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }

    public Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(
            OrderId.of(entity.getId()),
            OrderNumber.of(entity.getOrderNumber()),
            UserId.of(entity.getUserId()),
            Money.of(entity.getTotalAmount()),
            entity.getStatus(),
            OrderAudit.of(entity.getCreatedAt(), entity.getUpdatedAt())
        );
    }
}
```

### 예시 2: Product Mapper (SoftDeletableEntity)

```java
@Component
public class ProductJpaEntityMapper {

    public ProductJpaEntity toEntity(Product domain) {
        return ProductJpaEntity.of(
            domain.getId(),
            domain.getName(),
            domain.getPrice(),
            domain.getStock(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getDeletedAt()  // ✅ Soft Delete
        );
    }

    public Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
            ProductId.of(entity.getId()),
            ProductName.of(entity.getName()),
            Money.of(entity.getPrice()),
            Stock.of(entity.getStock()),
            ProductAudit.of(
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
            )
        );
    }
}
```

### 예시 3: 복잡한 Value Object 변환

```java
@Component
public class OrderJpaEntityMapper {

    /**
     * Domain → Entity 변환 (복잡한 Value Object)
     */
    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getOrderNumberValue(),           // OrderNumber → String
            domain.getCustomer().getIdValue(),      // Customer → Long userId
            domain.getShippingAddressValue(), // Address → String
            domain.getTotalAmountValue(),           // Money → BigDecimal
            domain.getStatus(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }

    /**
     * Entity → Domain 변환 (복잡한 Value Object 재구성)
     */
    public Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(
            OrderId.of(entity.getId()),
            OrderNumber.of(entity.getOrderNumber()),
            // 주의: Customer는 별도 조회 필요 (userId만 저장됨)
            CustomerId.of(entity.getUserId()),
            // 주의: ShippingAddress는 별도 Entity에 저장 가능
            Address.parse(entity.getShippingAddress()),
            Money.of(entity.getTotalAmount()),
            entity.getStatus(),
            OrderAudit.of(entity.getCreatedAt(), entity.getUpdatedAt())
        );
    }
}
```

---

## 5️⃣ 안티패턴

### 안티패턴 1: Mapper에 비즈니스 로직 포함

```java
// ❌ 안티패턴
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ❌ 비즈니스 검증 금지!
        if (domain.getTotalAmount().isNegative()) {
            throw new InvalidOrderException("금액은 0보다 커야 합니다");
        }

        // ❌ 비즈니스 로직 금지!
        if (domain.getStatus() == OrderStatus.CANCELLED) {
            // 취소 관련 로직...
        }

        return OrderJpaEntity.of(...);
    }
}

// ✅ 올바른 방법: Domain Layer에서 검증
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ✅ 단순 변환만
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getTotalAmountValue(),  // 이미 Domain에서 검증됨
            domain.getStatus(),                   // 이미 Domain에서 검증됨
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
```

### 안티패턴 2: Mapper에서 시간 생성

```java
// ❌ 안티패턴
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getOrderNumber(),
            LocalDateTime.now(),  // ❌ Mapper에서 시간 생성 금지!
            LocalDateTime.now()
        );
    }
}

// ✅ 올바른 방법: Domain의 시간 전달
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getOrderNumber(),
            domain.getCreatedAt(),  // ✅ Domain의 시간 전달
            domain.getUpdatedAt()   // ✅ Domain의 시간 전달
        );
    }
}
```

### 안티패턴 3: new 생성자 직접 호출

```java
// ❌ 안티패턴
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ❌ private 생성자라서 불가능!
        return new OrderJpaEntity(
            domain.getId(),
            domain.getOrderNumber(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}

// ✅ 올바른 방법: of() 메서드 사용
@Component
public class OrderJpaEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        // ✅ of() 스태틱 메서드 사용
        return OrderJpaEntity.of(
            domain.getId(),
            domain.getOrderNumber(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
```

### 안티패턴 4: Static 메서드 사용

```java
// ❌ 안티패턴
public class OrderJpaEntityMapper {

    private OrderJpaEntityMapper() { }

    // ❌ Static 메서드 금지!
    public static OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(...);
    }

    public static Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(...);
    }
}

// ✅ 올바른 방법: @Component로 Spring Bean 등록
@Component
public class OrderJpaEntityMapper {

    // ✅ Instance 메서드
    public OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.of(...);
    }

    public Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(...);
    }
}
```

---

## 6️⃣ 사용 예시 (Adapter에서)

### CommandAdapter에서 사용

```java
@Component
public class OrderCommandAdapter implements SaveOrderPort {

    private final OrderRepository orderRepository;
    private final OrderJpaEntityMapper orderJpaEntityMapper;

    public OrderCommandAdapter(
        OrderRepository orderRepository,
        OrderJpaEntityMapper orderJpaEntityMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderJpaEntityMapper = orderJpaEntityMapper;
    }

    @Override
    public Order save(Order order) {
        // 1. Domain → Entity 변환
        OrderJpaEntity entity = orderJpaEntityMapper.toEntity(order);

        // 2. 저장
        OrderJpaEntity savedEntity = orderRepository.save(entity);

        // 3. Entity → Domain 변환
        return orderJpaEntityMapper.toDomain(savedEntity);
    }
}
```

### QueryAdapter에서 사용

```java
@Component
public class OrderQueryAdapter implements LoadOrderPort {

    private final OrderRepository orderRepository;
    private final OrderJpaEntityMapper orderJpaEntityMapper;

    public OrderQueryAdapter(
        OrderRepository orderRepository,
        OrderJpaEntityMapper orderJpaEntityMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderJpaEntityMapper = orderJpaEntityMapper;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return orderRepository.findById(id.getValue())
            .map(orderJpaEntityMapper::toDomain);  // ✅ Entity → Domain
    }

    @Override
    public List<Order> findByCriteria(OrderSearchCriteria criteria) {
        List<OrderJpaEntity> entities = orderRepository.findByCriteria(criteria);

        return entities.stream()
            .map(orderJpaEntityMapper::toDomain)  // ✅ Entity → Domain
            .toList();
    }
}
```

---

## 7️⃣ 체크리스트

Mapper 작성 시:
- [ ] `@Component` 어노테이션 추가 확인
- [ ] `toEntity()` 메서드 존재
  - [ ] Domain → Entity 변환
  - [ ] `Entity.of()` 메서드 사용
  - [ ] 시간 필드 직접 전달 (LocalDateTime.now() 사용 금지)
- [ ] `toDomain()` 메서드 존재
  - [ ] Entity → Domain 변환
  - [ ] `Domain.reconstitute()` 또는 `Domain.of()` 사용
  - [ ] Value Object 재구성
- [ ] **비즈니스 로직 포함 없음** (단순 변환만)
- [ ] **검증 로직 없음** (Domain Layer에서 검증)
- [ ] BaseAuditEntity 상속 경우: createdAt, updatedAt 전달
- [ ] SoftDeletableEntity 상속 경우: deletedAt 전달
- [ ] Javadoc 작성 (변환 규칙 설명)

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
