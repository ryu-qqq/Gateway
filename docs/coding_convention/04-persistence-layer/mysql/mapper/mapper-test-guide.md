# JPA Entity Mapper 테스트 가이드

> **목적**: Mapper 단위 테스트 가이드

---

## 1️⃣ 테스트 전략

### 핵심 원칙

**Mapper는 단위 테스트 필수!**

**이유**:
- ✅ **변환 로직 검증**: Domain ↔ Entity 정확성 확인
- ✅ **빠른 피드백**: 의존성 없이 독립적 테스트
- ✅ **회귀 방지**: 변환 로직 변경 시 즉시 감지

### 테스트 범위

- ✅ **toEntity() 단위 테스트**: Domain → Entity 변환 정확성 (필수)
- ✅ **toDomain() 단위 테스트**: Entity → Domain 변환 정확성 (필수)
- ✅ **왕복 변환 테스트**: 데이터 손실 없음 검증
- ❌ **통합 테스트 불필요**: Adapter 테스트에서 검증

### 다른 레이어 테스트

- **ArchUnit 테스트**: [mapper-archunit.md](./mapper-archunit.md) 참고
- **통합 테스트**: Adapter 테스트에서 처리

---

## 2️⃣ 단위 테스트 (필수)

### 테스트 구조

```java
import org.junit.jupiter.api.Tag;

@Tag("unit")
@Tag("mapper")
@Tag("persistence-layer")
@DisplayName("OrderJpaEntityMapper Unit Test")
class OrderJpaEntityMapperTest {

    private OrderJpaEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderJpaEntityMapper();
    }

    @Test
    @DisplayName("toEntity() 호출 시 모든 필드가 정확히 매핑되어야 한다")
    void toEntity_WhenCalled_ShouldMapAllFieldsCorrectly() {
        // Given
        Order domain = Order.of(
            1L,
            "ORD-001",
            100L,
            BigDecimal.valueOf(50000),
            OrderStatus.PLACED,
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 2, 0, 0)
        );

        // When
        OrderJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(entity.getUserId()).isEqualTo(100L);
        assertThat(entity.getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 2, 0, 0));
    }

    @Test
    @DisplayName("toEntity() 호출 시 ID가 null이면 null로 매핑되어야 한다 (신규 생성)")
    void toEntity_WhenIdIsNull_ShouldMapAsNull() {
        // Given
        Order domain = Order.of(
            null,  // 신규 생성
            "ORD-002",
            100L,
            BigDecimal.valueOf(50000),
            OrderStatus.PLACED,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // When
        OrderJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getId()).isNull();
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("toDomain() 호출 시 모든 필드가 정확히 매핑되어야 한다")
    void toDomain_WhenCalled_ShouldMapAllFieldsCorrectly() {
        // Given
        OrderJpaEntity entity = OrderJpaEntity.of(
            1L,
            "ORD-001",
            100L,
            BigDecimal.valueOf(50000),
            OrderStatus.PLACED,
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 2, 0, 0)
        );

        // When
        Order domain = mapper.toDomain(entity);

        // Then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(domain.getUserId()).isEqualTo(100L);
        assertThat(domain.getTotalAmountValue()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(domain.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(domain.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        assertThat(domain.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 1, 2, 0, 0));
    }

    @Test
    @DisplayName("toEntity() → toDomain() 왕복 변환 시 데이터 손실이 없어야 한다")
    void roundTrip_WhenCalled_ShouldPreserveData() {
        // Given
        Order originalDomain = Order.of(
            1L,
            "ORD-001",
            100L,
            BigDecimal.valueOf(50000),
            OrderStatus.PLACED,
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 2, 0, 0)
        );

        // When
        OrderJpaEntity entity = mapper.toEntity(originalDomain);
        Order resultDomain = mapper.toDomain(entity);

        // Then
        assertThat(resultDomain.getId()).isEqualTo(originalDomain.getId());
        assertThat(resultDomain.getOrderNumber()).isEqualTo(originalDomain.getOrderNumber());
        assertThat(resultDomain.getUserId()).isEqualTo(originalDomain.getUserId());
        assertThat(resultDomain.getTotalAmountValue()).isEqualTo(originalDomain.getTotalAmountValue());
    }
}
```

---

## 3️⃣ SoftDeletableEntity 테스트

### deletedAt 필드 변환 검증

```java
import org.junit.jupiter.api.Tag;

@Tag("unit")
@Tag("mapper")
@Tag("persistence-layer")
@DisplayName("ProductJpaEntityMapper Unit Test (SoftDeletableEntity)")
class ProductJpaEntityMapperTest {

    private ProductJpaEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductJpaEntityMapper();
    }

    @Test
    @DisplayName("toEntity() 호출 시 deletedAt이 null이면 활성 상태로 매핑되어야 한다")
    void toEntity_WhenDeletedAtIsNull_ShouldMapAsActive() {
        // Given
        Product domain = Product.of(
            1L,
            "상품명",
            BigDecimal.valueOf(10000),
            100,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null  // 활성 상태
        );

        // When
        ProductJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity() 호출 시 deletedAt이 있으면 삭제 상태로 매핑되어야 한다")
    void toEntity_WhenDeletedAtExists_ShouldMapAsDeleted() {
        // Given
        LocalDateTime deletedAt = LocalDateTime.of(2025, 1, 10, 0, 0);
        Product domain = Product.of(
            1L,
            "상품명",
            BigDecimal.valueOf(10000),
            100,
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 2, 0, 0),
            deletedAt  // 삭제 상태
        );

        // When
        ProductJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("toDomain() 호출 시 deletedAt이 null이면 활성 상태 Domain을 반환해야 한다")
    void toDomain_WhenDeletedAtIsNull_ShouldReturnActiveDomain() {
        // Given
        ProductJpaEntity entity = ProductJpaEntity.of(
            1L,
            "상품명",
            BigDecimal.valueOf(10000),
            100,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null  // 활성 상태
        );

        // When
        Product domain = mapper.toDomain(entity);

        // Then
        assertThat(domain.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("toDomain() 호출 시 deletedAt이 있으면 삭제 상태 Domain을 반환해야 한다")
    void toDomain_WhenDeletedAtExists_ShouldReturnDeletedDomain() {
        // Given
        LocalDateTime deletedAt = LocalDateTime.of(2025, 1, 10, 0, 0);
        ProductJpaEntity entity = ProductJpaEntity.of(
            1L,
            "상품명",
            BigDecimal.valueOf(10000),
            100,
            LocalDateTime.of(2025, 1, 1, 0, 0),
            LocalDateTime.of(2025, 1, 2, 0, 0),
            deletedAt  // 삭제 상태
        );

        // When
        Product domain = mapper.toDomain(entity);

        // Then
        assertThat(domain.getDeletedAt()).isEqualTo(deletedAt);
    }
}
```

---

## 4️⃣ 복잡한 Value Object 변환 테스트

### 중첩 Value Object 변환 검증

```java
import org.junit.jupiter.api.Tag;

@Tag("unit")
@Tag("mapper")
@Tag("persistence-layer")
@DisplayName("OrderJpaEntityMapper Unit Test (Complex Value Object)")
class OrderJpaEntityMapperComplexTest {

    private OrderJpaEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderJpaEntityMapper();
    }

    @Test
    @DisplayName("toEntity() 호출 시 Value Object가 Primitive로 정확히 변환되어야 한다")
    void toEntity_WhenComplexValueObject_ShouldMapToPrimitive() {
        // Given
        Order domain = Order.builder()
            .id(1L)
            .orderNumber(OrderNumber.of("ORD-001"))        // Value Object → String
            .customer(Customer.of(CustomerId.of(100L)))   // Value Object → Long
            .totalAmount(Money.of(BigDecimal.valueOf(50000)))  // Value Object → BigDecimal
            .build();

        // When
        OrderJpaEntity entity = mapper.toEntity(domain);

        // Then
        assertThat(entity.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(entity.getUserId()).isEqualTo(100L);
        assertThat(entity.getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("toDomain() 호출 시 Primitive가 Value Object로 정확히 변환되어야 한다")
    void toDomain_WhenPrimitive_ShouldMapToValueObject() {
        // Given
        OrderJpaEntity entity = OrderJpaEntity.of(
            1L,
            "ORD-001",
            100L,
            BigDecimal.valueOf(50000),
            OrderStatus.PLACED,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // When
        Order domain = mapper.toDomain(entity);

        // Then
        assertThat(domain.getOrderNumber()).isInstanceOf(OrderNumber.class);
        assertThat(domain.getOrderNumber().getValue()).isEqualTo("ORD-001");
        assertThat(domain.getCustomer().getId()).isInstanceOf(CustomerId.class);
        assertThat(domain.getCustomer().getIdValue()).isEqualTo(100L);
        assertThat(domain.getTotalAmount()).isInstanceOf(Money.class);
        assertThat(domain.getTotalAmountValue()).isEqualTo(BigDecimal.valueOf(50000));
    }
}
```

---

## 5️⃣ 테스트하지 않는 것

### ❌ 컨벤션 검증
**이유**: ArchUnit이 자동으로 검증

```java
// ❌ 불필요한 테스트
@Test
void mapper_ShouldHaveComponentAnnotation() {
    // ArchUnit이 이미 검증함
}

@Test
void mapper_ShouldNotUseLombok() {
    // ArchUnit이 이미 검증함
}
```

### ❌ 통합 테스트
**이유**: Adapter 테스트에서 Mapper + Repository 통합 검증

```java
// ❌ Mapper 테스트에서 하지 않음
@DataJpaTest
class OrderJpaEntityMapperIntegrationTest {
    // 통합 검증은 Adapter 테스트에서!
}
```

---

## 6️⃣ 디렉토리 구조

```
adapter-out/persistence-mysql/
├─ src/main/java/
│  └─ com/company/adapter/out/persistence/
│      └─ order/
│          ├─ entity/
│          │  └─ OrderJpaEntity.java
│          └─ mapper/
│              └─ OrderJpaEntityMapper.java
│
└─ src/test/java/
   └─ com/company/adapter/out/persistence/
       ├─ architecture/mapper/
       │  └─ MapperArchTest.java  ⭐ ArchUnit (별도 문서)
       │
       └─ order/mapper/
           └─ OrderJpaEntityMapperTest.java  ⭐ 단위 테스트 (필수)
```

---

## 7️⃣ 체크리스트

Mapper 테스트 작성 시:
- [ ] **테스트 클래스 태그 추가** (필수)
  - [ ] `@Tag("unit")` - 단위 테스트 표시
  - [ ] `@Tag("mapper")` - Mapper 테스트 표시
  - [ ] `@Tag("persistence-layer")` - Persistence Layer 표시
- [ ] **toEntity() 변환 정확성 검증** (필수)
  - [ ] 모든 필드 매핑 확인
  - [ ] null 처리 확인
  - [ ] Value Object 변환 확인
- [ ] **toDomain() 변환 정확성 검증** (필수)
  - [ ] 모든 필드 매핑 확인
  - [ ] Value Object 재구성 확인
- [ ] **왕복 변환 (round-trip) 검증** (권장)
  - [ ] 데이터 손실 없음 확인
- [ ] **테스트하지 않는 것 확인**
  - [ ] 컨벤션 검증 (ArchUnit이 담당)
  - [ ] 통합 테스트 (Adapter에서)

---

## 8️⃣ 참고 문서

- [mapper-guide.md](./mapper-guide.md) - Mapper 컨벤션
- [mapper-archunit.md](./mapper-archunit.md) - ArchUnit 테스트 가이드
- Adapter 테스트에서 통합 검증

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
