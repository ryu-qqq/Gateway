# JPA Entity 가이드

> **목적**: JPA Entity 설계 규칙 및 BaseAuditEntity/SoftDeletableEntity 활용 가이드

---

## 1️⃣ JPA Entity란?

### 역할
**데이터베이스 테이블 ↔ JPA Entity ↔ Mapper ↔ Domain**

Persistence Layer의 데이터 컨테이너로서 데이터베이스 테이블과 1:1 매핑됩니다.

### 책임
- ✅ 데이터베이스 테이블 구조 정의 (@Entity, @Table, @Column)
- ✅ 데이터 저장 및 조회 (데이터 컨테이너)
- ✅ JPA 프록시 생성 지원 (protected 기본 생성자)
- ❌ **비즈니스 로직 금지** (Domain Layer로)
- ❌ **연관 관계 금지** (@ManyToOne, @OneToMany 등)
- ❌ **Lombok 금지** (Plain Java 사용)

### 핵심 원칙
```
데이터베이스 테이블 (MySQL)
  ↓ JPA
JPA Entity (데이터 컨테이너)
  ↓ Mapper
Domain 객체 (비즈니스 로직)
```

---

## 2️⃣ 핵심 원칙

### 원칙 1: Lombok 금지
```java
// ❌ Lombok 사용 금지
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderJpaEntity { }

// ✅ Plain Java 사용
public class OrderJpaEntity {
    protected OrderJpaEntity() { }  // JPA 기본 생성자

    public OrderJpaEntity(Long id, String message, ...) {
        // 명시적 생성자
    }

    public Long getId() { return id; }  // 명시적 getter
}
```

### 원칙 2: 연관 관계 금지 (Long FK 전략)
```java
// ❌ JPA 관계 어노테이션 금지
@ManyToOne
@JoinColumn(name = "user_id")
private UserJpaEntity user;

@OneToMany(mappedBy = "order")
private List<OrderLineItemJpaEntity> items;

// ✅ Long FK 사용
@Column(name = "user_id", nullable = false)
private Long userId;  // 외래키는 Long 타입으로만 관리
```

**이유**:
- N+1 문제 회피
- 지연 로딩 복잡도 제거
- 명시적 쿼리 작성 (QueryDSL)
- 테스트 용이성 향상

### 원칙 3: Setter 금지, Getter만 제공
```java
// ❌ Setter 제공 금지
public void setMessage(String message) {
    this.message = message;
}

// ✅ Getter만 제공
public String getMessage() {
    return message;
}

// ✅ 필요 시 명시적 생성자만 제공
public ExampleJpaEntity(Long id, String message, ExampleStatus status, ...) {
    this.id = id;
    this.message = message;
    this.status = status;
}
```

### 원칙 4: 비즈니스 로직 금지
```java
// ❌ Entity에 비즈니스 로직 금지
public void approve() {
    if (this.status == OrderStatus.PENDING) {
        this.status = OrderStatus.APPROVED;
    }
}

// ✅ Domain Layer에서 처리
// OrderDomain.java (Domain Layer)
public void approve() {
    validateCanApprove();  // 비즈니스 검증
    this.status = OrderStatus.APPROVED;
}
```

### 원칙 5: of() 스태틱 메서드로만 생성 (생성자 private)
```java
// ❌ public 생성자 노출 금지
public ExampleJpaEntity(
    String message,
    ExampleStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    this(null, message, status, createdAt, updatedAt);
}

// ✅ of() 스태틱 메서드만 노출 (Mapper에서 사용)
public static ExampleJpaEntity of(
    Long id,
    String message,
    ExampleStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    return new ExampleJpaEntity(id, message, status, createdAt, updatedAt);
}

// ✅ 전체 필드 생성자는 private (무분별한 생성 방지)
private ExampleJpaEntity(
    Long id,
    String message,
    ExampleStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    super(createdAt, updatedAt);  // 부모 클래스 필드 초기화
    this.id = id;
    this.message = message;
    this.status = status;
}
```

**이유**:
- 생성자를 `private`으로 숨겨 무분별한 생성 방지
- Mapper에서만 `of()` 메서드로 생성하도록 강제
- 생성 로직의 일관성 보장

### 원칙 6: protected 기본 생성자 (JPA 스펙)

**⚠️ 기본 생성자 vs 전체 필드 생성자 구분:**

| 생성자 유형 | 접근 제어자 | super() 호출 | 용도 |
|------------|-----------|-------------|------|
| **기본 생성자** (파라미터 없음) | `protected` | 선택적 | JPA 프록시 생성용 |
| **전체 필드 생성자** (파라미터 있음) | `private` | **필수** | 실제 인스턴스 생성용 |

```java
// ✅ JPA 기본 생성자 (protected, 빈 상태 유지)
protected ExampleJpaEntity() {
    // 비워두거나 super()만 호출 가능
}

// ✅ 상속 시 super() 호출도 허용 (SoftDeletableEntity 패턴)
protected ExampleJpaEntity() {
    super();  // BaseAuditEntity/SoftDeletableEntity 상속 시 허용
}

// ✅ 전체 필드 생성자에서는 super(필드들) 필수
private ExampleJpaEntity(Long id, String message, LocalDateTime createdAt, LocalDateTime updatedAt) {
    super(createdAt, updatedAt);  // 부모 필드 초기화 필수!
    this.id = id;
    this.message = message;
}
```

**핵심 규칙**:
- 기본 생성자: JPA 프록시 생성용, 빈 상태 또는 `super()` 호출
- 전체 필드 생성자: 부모 클래스 상속 시 `super(필드들)` 호출 필수

### 원칙 7: BaseAuditEntity/SoftDeletableEntity 활용

#### 시간 필드 필요 시 → BaseAuditEntity 상속
```java
// ✅ BaseAuditEntity 상속 (createdAt, updatedAt 자동 제공)
@Entity
@Table(name = "example")
public class ExampleJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message")
    private String message;

    // createdAt, updatedAt는 BaseAuditEntity에서 제공
    // 별도 선언 불필요!
}
```

#### 삭제 플래그 필요 시 → SoftDeletableEntity 상속
```java
// ✅ SoftDeletableEntity 상속 (createdAt, updatedAt, deletedAt 자동 제공)
@Entity
@Table(name = "order")
public class OrderJpaEntity extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    // createdAt, updatedAt, deletedAt는 SoftDeletableEntity에서 제공
    // 별도 선언 불필요!

    protected OrderJpaEntity() {
        super();
    }

    public OrderJpaEntity(
        Long id,
        String orderNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        super(createdAt, updatedAt, deletedAt);
        this.id = id;
        this.orderNumber = orderNumber;
    }
}
```

#### BaseAuditEntity vs SoftDeletableEntity 선택 기준

| 상황 | 상속 클래스 | 이유 |
|------|------------|------|
| 시간 정보만 필요 | BaseAuditEntity | createdAt, updatedAt 제공 |
| 소프트 딜리트 필요 | SoftDeletableEntity | createdAt, updatedAt, deletedAt 제공 |
| 시간/삭제 불필요 | 상속 안 함 | 필드 직접 선언 |

---

## 3️⃣ 템플릿 코드

### 템플릿 1: BaseAuditEntity 상속 (시간 정보만)

```java
package com.company.adapter.out.persistence.{module}.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.company.adapter.out.persistence.common.entity.BaseAuditEntity;

import java.time.LocalDateTime;

/**
 * {Domain}JpaEntity - {Domain} JPA Entity
 *
 * <p>Persistence Layer의 JPA Entity로서 데이터베이스 테이블과 매핑됩니다.</p>
 *
 * <p><strong>BaseAuditEntity 상속:</strong></p>
 * <ul>
 *   <li>공통 감사 필드 상속: createdAt, updatedAt</li>
 *   <li>markAsUpdated() 메서드로 수정 일시 자동 갱신</li>
 * </ul>
 *
 * <p><strong>Long FK 전략:</strong></p>
 * <ul>
 *   <li>JPA 관계 어노테이션 사용 금지 (@ManyToOne, @OneToMany 등)</li>
 *   <li>모든 외래키는 Long 타입으로 직접 관리</li>
 * </ul>
 *
 * <p><strong>Lombok 금지:</strong></p>
 * <ul>
 *   <li>Plain Java getter 사용</li>
 *   <li>Setter 제공 금지</li>
 *   <li>명시적 생성자 제공</li>
 * </ul>
 *
 * @author {author}
 * @since 1.0.0
 */
@Entity
@Table(name = "{table_name}")
public class {Domain}JpaEntity extends BaseAuditEntity {

    /**
     * 기본 키 - AUTO_INCREMENT
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * {필드 설명}
     */
    @Column(name = "{column_name}", nullable = false, length = 100)
    private String {fieldName};

    /**
     * 상태 Enum
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private {Domain}Status status;

    /**
     * 외래키 (Long FK 전략)
     *
     * <p>JPA 관계 어노테이션 사용 금지</p>
     * <p>연관 관계는 Application Layer에서 조합</p>
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * JPA 기본 생성자 (protected)
     *
     * <p>JPA 스펙 요구사항으로 반드시 필요합니다.</p>
     */
    protected {Domain}JpaEntity() {
    }

    /**
     * 전체 필드 생성자 (private)
     *
     * <p>직접 호출 금지, of() 스태틱 메서드로만 생성하세요.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     * @param status 상태
     * @param userId 사용자 ID (외래키)
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     */
    private {Domain}JpaEntity(
        Long id,
        String {fieldName},
        {Domain}Status status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        super(createdAt, updatedAt);
        this.id = id;
        this.{fieldName} = {fieldName};
        this.status = status;
        this.userId = userId;
    }

    /**
     * of() 스태틱 팩토리 메서드 (Mapper 전용)
     *
     * <p>Entity 생성은 반드시 이 메서드를 통해서만 가능합니다.</p>
     * <p>Mapper에서 Domain → Entity 변환 시 사용합니다.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     * @param status 상태
     * @param userId 사용자 ID (외래키)
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     * @return {Domain}JpaEntity 인스턴스
     */
    public static {Domain}JpaEntity of(
        Long id,
        String {fieldName},
        {Domain}Status status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return new {Domain}JpaEntity(id, {fieldName}, status, userId, createdAt, updatedAt);
    }

    // ===== Getters (Setter 제공 금지) =====

    public Long getId() {
        return id;
    }

    public String get{FieldName}() {
        return {fieldName};
    }

    public {Domain}Status getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }
}
```

### 템플릿 2: SoftDeletableEntity 상속 (소프트 딜리트)

```java
package com.company.adapter.out.persistence.{module}.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.company.adapter.out.persistence.common.entity.SoftDeletableEntity;

import java.time.LocalDateTime;

/**
 * {Domain}JpaEntity - {Domain} JPA Entity
 *
 * <p>Persistence Layer의 JPA Entity로서 데이터베이스 테이블과 매핑됩니다.</p>
 *
 * <p><strong>SoftDeletableEntity 상속:</strong></p>
 * <ul>
 *   <li>공통 감사 필드 상속: createdAt, updatedAt, deletedAt</li>
 *   <li>소프트 딜리트 지원 (deletedAt != null → 삭제)</li>
 *   <li>isDeleted(), isActive() 메서드 제공</li>
 * </ul>
 *
 * <p><strong>Long FK 전략:</strong></p>
 * <ul>
 *   <li>JPA 관계 어노테이션 사용 금지 (@ManyToOne, @OneToMany 등)</li>
 *   <li>모든 외래키는 Long 타입으로 직접 관리</li>
 * </ul>
 *
 * <p><strong>Lombok 금지:</strong></p>
 * <ul>
 *   <li>Plain Java getter 사용</li>
 *   <li>Setter 제공 금지</li>
 *   <li>명시적 생성자 제공</li>
 * </ul>
 *
 * @author {author}
 * @since 1.0.0
 */
@Entity
@Table(name = "{table_name}")
public class {Domain}JpaEntity extends SoftDeletableEntity {

    /**
     * 기본 키 - AUTO_INCREMENT
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * {필드 설명}
     */
    @Column(name = "{column_name}", nullable = false, length = 100)
    private String {fieldName};

    /**
     * 상태 Enum
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private {Domain}Status status;

    /**
     * 외래키 (Long FK 전략)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * JPA 기본 생성자 (protected)
     *
     * <p>JPA 스펙 요구사항으로 반드시 필요합니다.</p>
     */
    protected {Domain}JpaEntity() {
    }

    /**
     * 전체 필드 생성자 (private, deletedAt 포함)
     *
     * <p>직접 호출 금지, of() 스태틱 메서드로만 생성하세요.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     * @param status 상태
     * @param userId 사용자 ID (외래키)
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     * @param deletedAt 삭제 일시
     */
    private {Domain}JpaEntity(
        Long id,
        String {fieldName},
        {Domain}Status status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        super(createdAt, updatedAt, deletedAt);
        this.id = id;
        this.{fieldName} = {fieldName};
        this.status = status;
        this.userId = userId;
    }

    /**
     * of() 스태틱 팩토리 메서드 (Mapper 전용)
     *
     * <p>Entity 생성은 반드시 이 메서드를 통해서만 가능합니다.</p>
     * <p>Mapper에서 Domain → Entity 변환 시 사용합니다.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     * @param status 상태
     * @param userId 사용자 ID (외래키)
     * @param createdAt 생성 일시
     * @param updatedAt 수정 일시
     * @param deletedAt 삭제 일시
     * @return {Domain}JpaEntity 인스턴스
     */
    public static {Domain}JpaEntity of(
        Long id,
        String {fieldName},
        {Domain}Status status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        return new {Domain}JpaEntity(id, {fieldName}, status, userId, createdAt, updatedAt, deletedAt);
    }

    // ===== Getters (Setter 제공 금지) =====

    public Long getId() {
        return id;
    }

    public String get{FieldName}() {
        return {fieldName};
    }

    public {Domain}Status getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }

    // deletedAt, isDeleted(), isActive()는 SoftDeletableEntity에서 제공
}
```

### 템플릿 3: 상속 없음 (시간/삭제 불필요)

```java
package com.company.adapter.out.persistence.{module}.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {Domain}JpaEntity - {Domain} JPA Entity
 *
 * <p>Persistence Layer의 JPA Entity로서 데이터베이스 테이블과 매핑됩니다.</p>
 *
 * <p><strong>상속 없음:</strong></p>
 * <ul>
 *   <li>시간 정보 불필요 (임시 데이터, 로그성 데이터)</li>
 *   <li>소프트 딜리트 불필요</li>
 * </ul>
 *
 * <p><strong>Long FK 전략:</strong></p>
 * <ul>
 *   <li>JPA 관계 어노테이션 사용 금지</li>
 *   <li>모든 외래키는 Long 타입으로 직접 관리</li>
 * </ul>
 *
 * <p><strong>Lombok 금지:</strong></p>
 * <ul>
 *   <li>Plain Java getter 사용</li>
 *   <li>Setter 제공 금지</li>
 * </ul>
 *
 * @author {author}
 * @since 1.0.0
 */
@Entity
@Table(name = "{table_name}")
public class {Domain}JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "{column_name}", nullable = false)
    private String {fieldName};

    /**
     * JPA 기본 생성자 (protected)
     */
    protected {Domain}JpaEntity() {
    }

    /**
     * 전체 필드 생성자 (private)
     *
     * <p>직접 호출 금지, of() 스태틱 메서드로만 생성하세요.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     */
    private {Domain}JpaEntity(Long id, String {fieldName}) {
        this.id = id;
        this.{fieldName} = {fieldName};
    }

    /**
     * of() 스태틱 팩토리 메서드 (Mapper 전용)
     *
     * <p>Entity 생성은 반드시 이 메서드를 통해서만 가능합니다.</p>
     * <p>Mapper에서 Domain → Entity 변환 시 사용합니다.</p>
     *
     * @param id 기본 키
     * @param {fieldName} {필드 설명}
     * @return {Domain}JpaEntity 인스턴스
     */
    public static {Domain}JpaEntity of(Long id, String {fieldName}) {
        return new {Domain}JpaEntity(id, {fieldName});
    }

    // ===== Getters (Setter 제공 금지) =====

    public Long getId() {
        return id;
    }

    public String get{FieldName}() {
        return {fieldName};
    }
}
```

---

## 4️⃣ BaseAuditEntity / SoftDeletableEntity 활용 전략

### 4.1 BaseAuditEntity 활용 시점

**사용 조건**:
- ✅ 생성 일시 추적 필요
- ✅ 수정 일시 추적 필요
- ❌ 삭제 일시 불필요

**제공 필드**:
- `LocalDateTime createdAt` - 생성 일시 (updatable = false)
- `LocalDateTime updatedAt` - 수정 일시

**제공 메서드**:
- `LocalDateTime getCreatedAt()` - 생성 일시 조회
- `LocalDateTime getUpdatedAt()` - 수정 일시 조회

**사용 예시**:
```java
@Entity
@Table(name = "example")
public class ExampleJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message")
    private String message;

    protected ExampleJpaEntity() {
    }

    private ExampleJpaEntity(
        Long id,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        super(createdAt, updatedAt);
        this.id = id;
        this.message = message;
    }

    public static ExampleJpaEntity of(
        Long id,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return new ExampleJpaEntity(id, message, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public String getMessage() { return message; }
}
```

### 4.2 SoftDeletableEntity 활용 시점

**사용 조건**:
- ✅ 생성 일시 추적 필요
- ✅ 수정 일시 추적 필요
- ✅ 삭제 일시 추적 필요 (소프트 딜리트)

**제공 필드**:
- `LocalDateTime createdAt` - 생성 일시 (BaseAuditEntity 상속)
- `LocalDateTime updatedAt` - 수정 일시 (BaseAuditEntity 상속)
- `LocalDateTime deletedAt` - 삭제 일시 (null이면 활성)

**제공 메서드**:
- `LocalDateTime getDeletedAt()` - 삭제 일시 조회
- `boolean isDeleted()` - 삭제 여부 확인 (deletedAt != null)
- `boolean isActive()` - 활성 여부 확인 (deletedAt == null)

**사용 예시**:
```java
@Entity
@Table(name = "order")
public class OrderJpaEntity extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    protected OrderJpaEntity() {
    }

    private OrderJpaEntity(
        Long id,
        String orderNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        super(createdAt, updatedAt, deletedAt);
        this.id = id;
        this.orderNumber = orderNumber;
    }

    public static OrderJpaEntity of(
        Long id,
        String orderNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        return new OrderJpaEntity(id, orderNumber, createdAt, updatedAt, deletedAt);
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
}
```

### 4.3 상속 없음 (시간/삭제 불필요)

**사용 조건**:
- ❌ 생성 일시 불필요
- ❌ 수정 일시 불필요
- ❌ 삭제 일시 불필요

**적용 케이스**:
- 임시 데이터 (세션, 캐시)
- 로그성 데이터 (이벤트 로그)
- 매핑 테이블 (다대다 관계)

**사용 예시**:
```java
@Entity
@Table(name = "session_token")
public class SessionTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token")
    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected SessionTokenJpaEntity() {
    }

    private SessionTokenJpaEntity(Long id, String token, LocalDateTime expiresAt) {
        this.id = id;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static SessionTokenJpaEntity of(Long id, String token, LocalDateTime expiresAt) {
        return new SessionTokenJpaEntity(id, token, expiresAt);
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
```

---

## 5️⃣ 체크리스트

JPA Entity 작성 시:
- [ ] `@Entity` 어노테이션 추가
- [ ] `@Table(name = "...")` 테이블명 명시
- [ ] Lombok 사용 안 함 (Plain Java)
- [ ] JPA 관계 어노테이션 사용 안 함 (@ManyToOne, @OneToMany 등)
- [ ] 모든 외래키는 Long 타입으로 선언
- [ ] Setter 제공 안 함 (Getter만 제공)
- [ ] 비즈니스 로직 포함 안 함
- [ ] BaseAuditEntity 또는 SoftDeletableEntity 상속 검토
- [ ] **protected 기본 생성자 필수** (JPA 스펙, 빈 상태 유지)
- [ ] **전체 필드 생성자 private으로 숨김** (무분별한 생성 방지)
- [ ] **of() 스태틱 메서드만 public으로 노출** (Mapper 전용)
- [ ] of() 메서드에서 private 생성자 호출
- [ ] Javadoc 작성 (@author, @since 포함)

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.1.0
