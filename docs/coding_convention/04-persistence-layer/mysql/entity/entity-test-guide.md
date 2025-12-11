# JPA Entity 테스트 가이드

> **목적**: JPA Entity 단위 테스트 가이드

---

## 1️⃣ 테스트 전략

### 핵심 원칙

**JPA Entity는 단위 테스트가 거의 불필요합니다!**

**이유**:
- ❌ **비즈니스 로직 없음**: Entity는 Anemic Model (빈약한 도메인 모델)
- ❌ **Setter 없음**: 상태 변경 메서드 없음
- ❌ **복잡한 로직 없음**: Getter와 of() 메서드만 존재

### 테스트 범위

- ✅ **of() 메서드 단위 테스트**: 복잡한 생성 로직이 있을 때만 (선택적)
- ❌ **Getter 테스트 불필요**: 단순 필드 반환
- ❌ **JPA 매핑 테스트 불필요**: Repository 테스트에서 검증

### 다른 레이어 테스트

- **ArchUnit 테스트**: [entity-archunit.md](./entity-archunit.md) 참고
- **JPA 매핑 검증**: Repository 테스트에서 처리
- **통합 테스트**: Adapter 테스트에서 처리

---

## 2️⃣ 단위 테스트 (선택적)

### 언제 작성하는가?

- ✅ of() 메서드에 복잡한 변환 로직이 있을 때
- ✅ BaseAuditEntity/SoftDeletableEntity 상속 로직 검증
- ❌ 단순 필드 할당만 있을 때는 불필요

### 테스트 구조

```java
@DisplayName("OrderJpaEntity Unit Test")
class OrderJpaEntityTest {

    @Test
    @DisplayName("of() 메서드 호출 시 모든 필드가 정확히 설정되어야 한다")
    void of_WhenCalled_ShouldSetAllFieldsCorrectly() {
        // Given
        Long id = 1L;
        String orderNumber = "ORD-2025-001";
        OrderStatus status = OrderStatus.PENDING;
        Long userId = 100L;
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 0, 0);

        // When
        OrderJpaEntity order = OrderJpaEntity.of(
            id, orderNumber, status, userId, createdAt, updatedAt
        );

        // Then
        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getStatus()).isEqualTo(status);
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("of() 메서드에 null ID 전달 시 null로 설정되어야 한다 (신규 생성)")
    void of_WhenIdIsNull_ShouldAllowNullId() {
        // Given
        String orderNumber = "ORD-2025-001";
        Long userId = 100L;
        LocalDateTime now = LocalDateTime.now();

        // When
        OrderJpaEntity order = OrderJpaEntity.of(
            null, orderNumber, OrderStatus.PENDING, userId, now, now
        );

        // Then
        assertThat(order.getId()).isNull();  // 신규 생성 시 ID는 null
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("BaseAuditEntity 상속 시 부모 필드도 정확히 설정되어야 한다")
    void of_WhenBaseAuditEntityInherited_ShouldSetParentFields() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 1, 2, 0, 0);

        // When
        OrderJpaEntity order = OrderJpaEntity.of(
            1L, "ORD-2025-001", OrderStatus.PENDING, 100L, createdAt, updatedAt
        );

        // Then
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
```

---

## 3️⃣ SoftDeletableEntity 테스트

### deletedAt 필드 설정 검증

```java
@DisplayName("ProductJpaEntity Unit Test (SoftDeletableEntity)")
class ProductJpaEntityTest {

    @Test
    @DisplayName("of() 메서드 호출 시 deletedAt이 null이어야 한다 (활성 상태)")
    void of_WhenCalled_ShouldSetDeletedAtToNull() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        ProductJpaEntity product = ProductJpaEntity.of(
            1L,
            "Product Name",
            now,
            now,
            null  // deletedAt
        );

        // Then
        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("deletedAt이 설정되면 삭제 상태로 생성되어야 한다")
    void of_WhenDeletedAtSet_ShouldCreateAsDeleted() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deletedAt = now.plusDays(1);

        // When
        ProductJpaEntity product = ProductJpaEntity.of(
            1L,
            "Product Name",
            now,
            now,
            deletedAt
        );

        // Then
        assertThat(product.getDeletedAt()).isEqualTo(deletedAt);
    }
}
```

---

## 4️⃣ 테스트하지 않는 것

### ❌ Getter 테스트
**이유**: 단순 필드 반환이므로 테스트 불필요

```java
// ❌ 불필요한 테스트
@Test
void getId_ShouldReturnId() {
    OrderJpaEntity order = OrderJpaEntity.of(1L, 100L, now, now);
    assertThat(order.getId()).isEqualTo(1L);  // 의미 없음
}
```

### ❌ Setter 테스트
**이유**: Setter가 없음 (컨벤션 위반)

```java
// ❌ Setter가 없으므로 테스트 불가
@Test
void setId_ShouldSetId() {
    // Setter가 없으므로 이 테스트 자체가 불가능
}
```

### ❌ 비즈니스 로직 테스트
**이유**: Entity에 비즈니스 로직 없음 (컨벤션 위반)

```java
// ❌ 비즈니스 로직이 없으므로 테스트 불가
@Test
void approve_ShouldChangeStatusToApproved() {
    // approve() 메서드가 없음 (컨벤션 위반)
}
```

### ❌ JPA 매핑 테스트
**이유**: Repository 테스트에서 검증

```java
// ❌ Entity 테스트에서 하지 않음
@DataJpaTest
class OrderJpaEntityJpaMappingTest {
    // JPA 매핑 검증은 Repository 테스트에서!
}
```

---

## 5️⃣ 디렉토리 구조

```
adapter-out/persistence-mysql/
├─ src/main/java/
│  └─ com/ryuqq/adapter/out/persistence/
│      ├─ order/
│      │  └─ entity/
│      │      └─ OrderJpaEntity.java
│      └─ common/entity/
│          ├─ BaseAuditEntity.java
│          └─ SoftDeletableEntity.java
│
└─ src/test/java/
   └─ com/ryuqq/adapter/out/persistence/
       ├─ architecture/entity/
       │  └─ JpaEntityArchTest.java  ⭐ ArchUnit (별도 문서)
       │
       └─ order/entity/
           └─ OrderJpaEntityTest.java  ⭐ 단위 테스트 (선택적)
```

---

## 6️⃣ 체크리스트

JPA Entity 테스트 작성 시:
- [ ] **of() 메서드 검증** (복잡한 로직이 있을 때만)
  - [ ] 모든 필드 설정 확인
  - [ ] null 처리 확인
  - [ ] 부모 필드 (BaseAuditEntity) 설정 확인
- [ ] **테스트하지 않는 것 확인**
  - [ ] Getter 테스트 작성 안 함
  - [ ] Setter 테스트 작성 안 함 (없음)
  - [ ] 비즈니스 로직 테스트 작성 안 함 (없음)
  - [ ] JPA 매핑 테스트 작성 안 함 (Repository에서)

---

## 7️⃣ 참고 문서

- [entity-guide.md](./entity-guide.md) - JPA Entity 컨벤션
- [entity-archunit.md](./entity-archunit.md) - ArchUnit 테스트 가이드
- Repository 테스트에서 JPA 매핑 검증
- Adapter 테스트에서 통합 검증

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.1.0
