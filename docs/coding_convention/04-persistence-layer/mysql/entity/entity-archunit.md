# JPA Entity ArchUnit 테스트 가이드

> **목적**: JPA Entity 컨벤션을 ArchUnit으로 자동 검증

---

## 1️⃣ ArchUnit 테스트란?

### 역할
**빌드 시 자동 실행 → 컨벤션 위반 감지 → 빌드 실패**

JPA Entity가 entity-guide.md의 모든 규칙을 준수하는지 자동으로 검증합니다.

### 책임
- ✅ **Zero-Tolerance 검증**: Lombok 금지, JPA 관계 금지, Setter 금지 등
- ✅ **생성자 패턴 검증**: protected 기본 생성자 + private 전체 필드 생성자
- ✅ **Factory Method 검증**: public static of() 메서드 필수
- ✅ **빌드 시 자동 실행**: 위반 시 빌드 실패로 강제 준수

### 핵심 원칙
```
개발자 코드 작성
  └─ Gradle/Maven 빌드
      └─ JpaEntityArchTest 실행 (자동)
            ├─ 17개 규칙 검증
            ├─ 위반 발견 → 빌드 실패
            └─ 모두 통과 → 빌드 성공
```

---

## 2️⃣ 테스트 클래스 구조

### 위치
```
adapter-out/persistence-mysql/
  └─ src/test/java/
      └─ com/ryuqq/adapter/out/persistence/architecture/entity/
          └─ JpaEntityArchTest.java
```

### 기본 구조

```java
@DisplayName("JPA Entity 아키텍처 규칙 검증 (Zero-Tolerance)")
class JpaEntityArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses entityClasses;

    @BeforeAll
    static void setUp() {
        // 1. 전체 패키지 클래스 로딩
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");

        // 2. @Entity 클래스만 필터링
        entityClasses = allClasses.that(
            DescribedPredicate.describe(
                "are JPA Entity classes",
                javaClass -> javaClass.isAnnotatedWith(Entity.class)
            )
        );
    }

    // 17개 테스트 메서드...
}
```

---

## 3️⃣ 17개 검증 규칙

### 규칙 1: @Entity 어노테이션 필수

```java
@Test
@DisplayName("규칙 1: @Entity 어노테이션 필수")
void jpaEntity_MustBeAnnotatedWithEntity() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("JpaEntity")
        .should().beAnnotatedWith(Entity.class)
        .because("JPA Entity 클래스는 @Entity 어노테이션이 필수입니다");

    rule.check(allClasses);
}
```

**검증 내용**:
- ✅ `*JpaEntity` 네이밍 규칙을 따르는 클래스
- ✅ `@Entity` 어노테이션 존재

---

### 규칙 2: Lombok 사용 금지 (6개 테스트)

#### 2-1. @Data 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Data")
void jpaEntity_MustNotUseLombok_Data() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.Data")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

#### 2-2. @Getter 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Getter")
void jpaEntity_MustNotUseLombok_Getter() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.Getter")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

#### 2-3. @Setter 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Setter")
void jpaEntity_MustNotUseLombok_Setter() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.Setter")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

#### 2-4. @Builder 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @Builder")
void jpaEntity_MustNotUseLombok_Builder() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.Builder")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

#### 2-5. @AllArgsConstructor 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @AllArgsConstructor")
void jpaEntity_MustNotUseLombok_AllArgsConstructor() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.AllArgsConstructor")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

#### 2-6. @NoArgsConstructor 금지
```java
@Test
@DisplayName("규칙 2: Lombok 사용 금지 - @NoArgsConstructor")
void jpaEntity_MustNotUseLombok_NoArgsConstructor() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().notBeAnnotatedWith("lombok.NoArgsConstructor")
        .because("JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)");

    rule.check(entityClasses);
}
```

**검증 내용**:
- ❌ `@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` 모두 금지
- ✅ Plain Java getter/setter 직접 작성

---

### 규칙 3: JPA 관계 어노테이션 금지 (4개 테스트)

#### 3-1. @ManyToOne 금지
```java
@Test
@DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @ManyToOne")
void jpaEntity_MustNotUseJpaRelationship_ManyToOne() {
    ArchRule rule = noFields()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .should().beAnnotatedWith(ManyToOne.class)
        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

    rule.check(entityClasses);
}
```

#### 3-2. @OneToMany 금지
```java
@Test
@DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @OneToMany")
void jpaEntity_MustNotUseJpaRelationship_OneToMany() {
    ArchRule rule = noFields()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .should().beAnnotatedWith(OneToMany.class)
        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

    rule.check(entityClasses);
}
```

#### 3-3. @OneToOne 금지
```java
@Test
@DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @OneToOne")
void jpaEntity_MustNotUseJpaRelationship_OneToOne() {
    ArchRule rule = noFields()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .should().beAnnotatedWith(OneToOne.class)
        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

    rule.check(entityClasses);
}
```

#### 3-4. @ManyToMany 금지
```java
@Test
@DisplayName("규칙 3: JPA 관계 어노테이션 금지 - @ManyToMany")
void jpaEntity_MustNotUseJpaRelationship_ManyToMany() {
    ArchRule rule = noFields()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .should().beAnnotatedWith(ManyToMany.class)
        .because("JPA Entity는 관계 어노테이션 사용이 금지됩니다 (Long FK 전략 사용)");

    rule.check(entityClasses);
}
```

**검증 내용**:
- ❌ `@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany` 모두 금지
- ✅ `private Long userId;` (Long FK 전략)

---

### 규칙 4: Setter 메서드 금지

```java
@Test
@DisplayName("규칙 4: Setter 메서드 금지")
void jpaEntity_MustNotHaveSetterMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .and().arePublic()
        .and().haveNameMatching("set[A-Z].*")
        .should(notExist())
        .because("JPA Entity는 Setter 메서드가 금지됩니다 (Getter만 제공)");

    rule.check(entityClasses);
}
```

**검증 내용**:
- ❌ `setXxx()` 형태의 public 메서드 금지
- ✅ `getXxx()` 메서드만 허용

---

### 규칙 5: 비즈니스 로직 금지

```java
@Test
@DisplayName("규칙 5: 비즈니스 로직 금지 (특정 메서드 패턴)")
void jpaEntity_MustNotHaveBusinessLogicMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
        .and().arePublic()
        .and().haveNameMatching("(approve|cancel|complete|activate|deactivate|validate|calculate).*")
        .should(notExist())
        .because("JPA Entity는 비즈니스 로직이 금지됩니다 (Domain Layer에서 처리)");

    rule.check(entityClasses);
}
```

**검증 내용**:
- ❌ `approve()`, `cancel()`, `complete()`, `activate()`, `deactivate()`, `validate()`, `calculate()` 등 비즈니스 메서드 금지
- ✅ 비즈니스 로직은 Domain Layer에서 처리

---

### 규칙 6: protected 기본 생성자 필수

```java
@Test
@DisplayName("규칙 6: protected 기본 생성자 필수")
void jpaEntity_MustHaveProtectedNoArgsConstructor() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should(haveProtectedNoArgsConstructor())
        .because("JPA Entity는 JPA 스펙을 위해 protected 기본 생성자가 필수입니다");

    rule.check(entityClasses);
}
```

**커스텀 ArchCondition**:
```java
private static ArchCondition<JavaClass> haveProtectedNoArgsConstructor() {
    return new ArchCondition<>("have protected no-args constructor") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean hasProtectedNoArgsConstructor = javaClass.getConstructors().stream()
                .anyMatch(constructor ->
                    constructor.getModifiers().contains(JavaModifier.PROTECTED) &&
                    constructor.getParameters().isEmpty()
                );

            if (!hasProtectedNoArgsConstructor) {
                String message = String.format(
                    "Class %s does not have a protected no-args constructor (required by JPA spec)",
                    javaClass.getName()
                );
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    };
}
```

**검증 내용**:
- ✅ `protected ExampleJpaEntity() { }` 필수
- ✅ 파라미터 없는 생성자
- ✅ `protected` 접근 제어자

---

### 규칙 7: private 전체 필드 생성자 필수

```java
@Test
@DisplayName("규칙 7: private 전체 필드 생성자 필수")
void jpaEntity_MustHavePrivateAllArgsConstructor() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should(havePrivateConstructorWithParameters())
        .because("JPA Entity는 무분별한 생성 방지를 위해 private 생성자가 필수입니다");

    rule.check(entityClasses);
}
```

**커스텀 ArchCondition**:
```java
private static ArchCondition<JavaClass> havePrivateConstructorWithParameters() {
    return new ArchCondition<>("have private constructor with parameters") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean hasPrivateConstructor = javaClass.getConstructors().stream()
                .anyMatch(constructor ->
                    constructor.getModifiers().contains(JavaModifier.PRIVATE) &&
                    !constructor.getParameters().isEmpty()
                );

            if (!hasPrivateConstructor) {
                String message = String.format(
                    "Class %s does not have a private constructor with parameters (required to prevent direct instantiation)",
                    javaClass.getName()
                );
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    };
}
```

**검증 내용**:
- ✅ `private ExampleJpaEntity(...) { ... }` 필수
- ✅ 파라미터가 있는 생성자
- ✅ `private` 접근 제어자

---

### 규칙 8: public static of() 메서드 필수

```java
@Test
@DisplayName("규칙 8: public static of() 메서드 필수")
void jpaEntity_MustHavePublicStaticOfMethod() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should(havePublicStaticOfMethod())
        .because("JPA Entity는 Mapper 전용 of() 스태틱 메서드가 필수입니다");

    rule.check(entityClasses);
}
```

**커스텀 ArchCondition**:
```java
private static ArchCondition<JavaClass> havePublicStaticOfMethod() {
    return new ArchCondition<>("have public static of() method") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean hasOfMethod = javaClass.getMethods().stream()
                .anyMatch(method ->
                    method.getName().equals("of") &&
                    method.getModifiers().contains(JavaModifier.PUBLIC) &&
                    method.getModifiers().contains(JavaModifier.STATIC)
                );

            if (!hasOfMethod) {
                String message = String.format(
                    "Class %s does not have a public static of() method (required for Mapper creation pattern)",
                    javaClass.getName()
                );
                events.add(SimpleConditionEvent.violated(javaClass, message));
            }
        }
    };
}
```

**검증 내용**:
- ✅ `public static ExampleJpaEntity of(...) { ... }` 필수
- ✅ 메서드명: `of`
- ✅ `public static` 제어자

---

### 규칙 10: Entity 네이밍 규칙

```java
@Test
@DisplayName("규칙 10: Entity 네이밍 규칙 (*JpaEntity)")
void jpaEntity_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().haveSimpleNameEndingWith("JpaEntity")
        .because("JPA Entity 클래스는 *JpaEntity 네이밍 규칙을 따라야 합니다");

    rule.check(entityClasses);
}
```

**검증 내용**:
- ✅ 클래스명: `*JpaEntity` (예: `OrderJpaEntity`, `ProductJpaEntity`)
- ❌ `Order`, `OrderEntity` 등 다른 네이밍 금지

---

## 4️⃣ 실행 방법

### Gradle

```bash
# 전체 테스트 실행
./gradlew test

# JpaEntityArchTest만 실행
./gradlew test --tests JpaEntityArchTest

# 특정 규칙만 실행
./gradlew test --tests JpaEntityArchTest.jpaEntity_MustNotUseLombok_Data
```

### Maven

```bash
# 전체 테스트 실행
mvn test

# JpaEntityArchTest만 실행
mvn test -Dtest=JpaEntityArchTest

# 특정 규칙만 실행
mvn test -Dtest=JpaEntityArchTest#jpaEntity_MustNotUseLombok_Data
```

### IntelliJ IDEA

1. `JpaEntityArchTest.java` 파일 열기
2. 클래스명 왼쪽 ▶️ 클릭 → "Run 'JpaEntityArchTest'"
3. 특정 테스트만 실행: 메서드명 왼쪽 ▶️ 클릭

---

## 5️⃣ 위반 예시 및 수정

### 예시 1: Lombok 사용 위반

**❌ 위반 코드**:
```java
@Entity
@Table(name = "orders")
@Data  // ❌ Lombok 금지!
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**빌드 에러**:
```
Architecture Violation [Priority: MEDIUM] - Rule 'classes that are annotated with @Entity
should not be annotated with lombok.Data, because JPA Entity는 Lombok 사용이 금지됩니다 (Plain Java 사용)'
was violated (1 times):
Class <com.ryuqq.adapter.out.persistence.order.OrderJpaEntity> is annotated with <lombok.Data>
in (OrderJpaEntity.java:0)
```

**✅ 수정 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected OrderJpaEntity() { }

    private OrderJpaEntity(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.id = id;
    }

    public static OrderJpaEntity of(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new OrderJpaEntity(id, createdAt, updatedAt);
    }

    public Long getId() { return id; }
}
```

---

### 예시 2: JPA 관계 어노테이션 위반

**❌ 위반 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // ❌ JPA 관계 금지!
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;
}
```

**빌드 에러**:
```
Architecture Violation [Priority: MEDIUM] - Rule 'no fields that are declared in classes that
are annotated with @Entity should be annotated with @ManyToOne, because JPA Entity는 관계 어노테이션
사용이 금지됩니다 (Long FK 전략 사용)' was violated (1 times):
Field <com.ryuqq.adapter.out.persistence.order.OrderJpaEntity.user> is annotated with <@ManyToOne>
in (OrderJpaEntity.java:0)
```

**✅ 수정 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // ✅ Long FK 사용
}
```

---

### 예시 3: of() 메서드 누락

**❌ 위반 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected OrderJpaEntity() { }

    // ❌ of() 메서드 없음!

    public Long getId() { return id; }
}
```

**빌드 에러**:
```
Architecture Violation [Priority: MEDIUM] - Rule 'classes that are annotated with @Entity
should have public static of() method, because JPA Entity는 Mapper 전용 of() 스태틱 메서드가 필수입니다'
was violated (1 times):
Class <com.ryuqq.adapter.out.persistence.order.OrderJpaEntity> does not have a public static of() method
(required for Mapper creation pattern) in (OrderJpaEntity.java:0)
```

**✅ 수정 코드**:
```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected OrderJpaEntity() { }

    private OrderJpaEntity(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(createdAt, updatedAt);
        this.id = id;
    }

    public static OrderJpaEntity of(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new OrderJpaEntity(id, createdAt, updatedAt);
    }

    public Long getId() { return id; }
}
```

---

## 6️⃣ CI/CD 통합

### GitHub Actions

```yaml
name: ArchUnit Tests

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  archunit-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run ArchUnit Tests
        run: ./gradlew test --tests JpaEntityArchTest

      - name: Upload Test Report
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: archunit-test-report
          path: build/reports/tests/test/
```

---

## 7️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] `JpaEntityArchTest.java` 파일 생성
- [ ] `@BeforeAll`에서 클래스 로딩 및 필터링
- [ ] 17개 테스트 메서드 작성
  - [ ] 규칙 1: @Entity 어노테이션 필수
  - [ ] 규칙 2: Lombok 금지 (6개)
  - [ ] 규칙 3: JPA 관계 어노테이션 금지 (4개)
  - [ ] 규칙 4: Setter 메서드 금지
  - [ ] 규칙 5: 비즈니스 로직 금지
  - [ ] 규칙 6: protected 기본 생성자 필수
  - [ ] 규칙 7: private 전체 필드 생성자 필수
  - [ ] 규칙 8: public static of() 메서드 필수
  - [ ] 규칙 10: *JpaEntity 네이밍 규칙
- [ ] 3개 커스텀 ArchCondition 구현
- [ ] 빌드 시 자동 실행 확인
- [ ] CI/CD 파이프라인 통합

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
