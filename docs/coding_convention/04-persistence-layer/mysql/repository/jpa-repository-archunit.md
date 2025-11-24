# JPA Repository ArchUnit 가이드

> **목적**: JPA Repository 아키텍처 규칙 자동 검증

---

## 1️⃣ 검증 전략

### ArchUnit이 검증하는 것

**JPA Repository 인터페이스**:
- ✅ 인터페이스 타입 (interface 키워드)
- ✅ JpaRepository 상속
- ✅ QuerydslPredicateExecutor 상속 금지
- ✅ Query Method 없음
- ✅ @Query 어노테이션 없음
- ✅ Custom Repository 구현 없음
- ✅ 네이밍 규칙 (*Repository)

---

## 2️⃣ ArchUnit 테스트 코드

### 전체 테스트 구조

```java
package com.company.adapter.out.persistence.architecture.repository;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * JpaRepositoryArchTest - JPA Repository 아키텍처 규칙 검증
 *
 * <p>jpa-repository-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: JpaRepository는 인터페이스여야 함</li>
 *   <li>규칙 2: JpaRepository 상속 필수</li>
 *   <li>규칙 3: QuerydslPredicateExecutor 상속 금지</li>
 *   <li>규칙 4: Query Method 추가 금지</li>
 *   <li>규칙 5: @Query 어노테이션 사용 금지</li>
 *   <li>규칙 6: Custom Repository 구현 금지</li>
 *   <li>규칙 7: 네이밍 규칙 (*Repository)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("JPA Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class JpaRepositoryArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses jpaRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence");

        // JpaRepository 인터페이스만 (QueryDsl 제외)
        jpaRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "are JpaRepository interfaces",
                javaClass -> javaClass.getSimpleName().endsWith("Repository") &&
                    !javaClass.getSimpleName().contains("QueryDsl") &&
                    javaClass.isInterface()
            )
        );
    }

    @Test
    @DisplayName("규칙 1: JpaRepository는 인터페이스여야 함")
    void jpaRepository_MustBeInterface() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().resideInAPackage("..repository..")
            .should().beInterfaces()
            .because("JpaRepository는 인터페이스로 정의되어야 합니다");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 2: JpaRepository 상속 필수")
    void jpaRepository_MustExtendJpaRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().areInterfaces()
            .should().beAssignableTo(JpaRepository.class)
            .because("JpaRepository 인터페이스를 상속해야 합니다");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 3: QuerydslPredicateExecutor 상속 금지")
    void jpaRepository_MustNotExtendQuerydslPredicateExecutor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameNotContaining("QueryDsl")
            .and().areInterfaces()
            .should().notBeAssignableTo(QuerydslPredicateExecutor.class)
            .because("JpaRepository는 QuerydslPredicateExecutor 상속이 금지됩니다 (순수 Command 전용)");

        rule.check(jpaRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 4: Query Method 추가 금지")
    void jpaRepository_MustNotHaveQueryMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
            .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("QueryDsl")
            .and().areDeclaredInClassesThat().areInterfaces()
            .and().arePublic()
            .and().haveNameMatching("find.*|search.*|count.*|exists.*|get.*")
            .should().notBeDeclared()
            .because("JpaRepository는 Query Method 추가가 금지됩니다 (QueryDslRepository 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 5: @Query 어노테이션 사용 금지")
    void jpaRepository_MustNotUseQueryAnnotation() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
            .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("QueryDsl")
            .and().areDeclaredInClassesThat().areInterfaces()
            .should().notBeAnnotatedWith(Query.class)
            .because("JpaRepository는 @Query 어노테이션 사용이 금지됩니다 (QueryDSL 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 6: Custom Repository 구현 금지")
    void jpaRepository_MustNotHaveCustomImplementation() {
        ArchRule rule = classes()
            .that().haveSimpleNameMatching(".*RepositoryImpl")
            .and().resideInAPackage("..repository..")
            .should().notExist()
            .because("Custom Repository 구현이 금지됩니다 (QueryDslRepository 사용)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 7: JpaRepository 네이밍 규칙 (*Repository)")
    void jpaRepository_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areInterfaces()
            .and().areAssignableTo(JpaRepository.class)
            .and().resideInAPackage("..repository..")
            .should().haveSimpleNameEndingWith("Repository")
            .because("JpaRepository는 *Repository 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }
}
```

---

## 3️⃣ 규칙 상세 설명

### 규칙 1: JpaRepository는 인터페이스

**검증 내용**: Repository 패키지의 `*Repository` 클래스는 interface여야 함

**위반 예시**:
```java
// ❌ 클래스로 정의
public class OrderRepository extends SimpleJpaRepository<OrderJpaEntity, Long> {
}
```

**올바른 예시**:
```java
// ✅ 인터페이스로 정의
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
```

### 규칙 2: JpaRepository 상속

**검증 내용**: `*Repository` 인터페이스는 `JpaRepository<Entity, ID>` 상속 필수

**위반 예시**:
```java
// ❌ 상속 없음
public interface OrderRepository {
}
```

**올바른 예시**:
```java
// ✅ JpaRepository 상속
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
```

### 규칙 3: QuerydslPredicateExecutor 상속 금지

**검증 내용**: `QuerydslPredicateExecutor` 상속 절대 금지

**위반 예시**:
```java
// ❌ QuerydslPredicateExecutor 상속 금지
public interface OrderRepository extends
    JpaRepository<OrderJpaEntity, Long>,
    QuerydslPredicateExecutor<OrderJpaEntity> {  // ❌
}
```

**올바른 예시**:
```java
// ✅ JpaRepository만 상속
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
```

### 규칙 4: Query Method 추가 금지

**검증 내용**: `find*`, `search*`, `count*`, `exists*`, `get*` 메서드 금지

**위반 예시**:
```java
// ❌ Query Method 추가 금지
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);  // ❌
    List<OrderJpaEntity> findByStatus(OrderStatus status);           // ❌
    long countByStatus(OrderStatus status);                          // ❌
}
```

**올바른 예시**:
```java
// ✅ Query Method 없음
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}

// ✅ QueryDSL Repository에서 처리
@Repository
public class OrderQueryDslRepository {
    public Optional<OrderJpaEntity> findByOrderNumber(String orderNumber) {
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.orderNumber.eq(orderNumber))
                .fetchOne()
        );
    }
}
```

### 규칙 5: @Query 어노테이션 사용 금지

**검증 내용**: `@Query` JPQL 어노테이션 사용 금지

**위반 예시**:
```java
// ❌ @Query 사용 금지
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.status = :status")  // ❌
    List<OrderJpaEntity> findByStatus(@Param("status") OrderStatus status);
}
```

**올바른 예시**:
```java
// ✅ @Query 없음
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}

// ✅ QueryDSL Repository에서 타입 안전 쿼리 사용
@Repository
public class OrderQueryDslRepository {
    public List<OrderJpaEntity> findByStatus(OrderStatus status) {
        return queryFactory.selectFrom(qOrder)
            .where(qOrder.status.eq(status))
            .fetch();
    }
}
```

### 규칙 6: Custom Repository 구현 금지

**검증 내용**: `*RepositoryImpl` 클래스 존재 금지

**위반 예시**:
```java
// ❌ Custom Repository 구현 금지
public interface OrderRepositoryCustom {
    List<OrderJpaEntity> searchOrders(SearchOrderQuery query);
}

public class OrderRepositoryImpl implements OrderRepositoryCustom {  // ❌
    @Override
    public List<OrderJpaEntity> searchOrders(SearchOrderQuery query) {
        // 복잡한 로직
    }
}
```

**올바른 예시**:
```java
// ✅ Custom Repository 없음
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}

// ✅ QueryDSL Repository로 대체
@Repository
public class OrderQueryDslRepository {
    public List<OrderJpaEntity> searchOrders(SearchOrderQuery query) {
        return queryFactory.selectFrom(qOrder)
            .where(buildConditions(query))
            .fetch();
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행
./gradlew test

# JPA Repository ArchUnit만 실행
./gradlew test --tests "*JpaRepositoryArchTest"
```

### IDE 실행

- IntelliJ IDEA: `JpaRepositoryArchTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 5️⃣ 위반 시 대응

### 1단계: 위반 로그 확인

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'classes that have simple name ending with 'Repository'
should not be assignable to QuerydslPredicateExecutor' was violated (1 times):
Interface <OrderRepository> is assignable to QuerydslPredicateExecutor
```

### 2단계: 위반 원인 파악

- ArchUnit 로그에서 위반 클래스 확인
- jpa-repository-guide.md 규칙 재확인

### 3단계: 코드 수정

```java
// Before (위반)
public interface OrderRepository extends
    JpaRepository<OrderJpaEntity, Long>,
    QuerydslPredicateExecutor<OrderJpaEntity> {  // ❌
}

// After (수정)
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
}
```

### 4단계: 재검증

```bash
./gradlew test --tests "*JpaRepositoryArchTest"
```

---

## 6️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] **JpaRepository 검증**
  - [ ] 인터페이스 타입 검증
  - [ ] JpaRepository 상속 검증
  - [ ] QuerydslPredicateExecutor 상속 금지 검증
- [ ] **금지 사항 검증**
  - [ ] Query Method 금지
  - [ ] @Query 어노테이션 금지
  - [ ] Custom Repository 구현 금지
- [ ] **네이밍 규칙 검증**
  - [ ] *Repository 네이밍 규칙

---

## 7️⃣ 참고 문서

- [jpa-repository-guide.md](./jpa-repository-guide.md) - JPA Repository 컨벤션
- [querydsl-repository-archunit.md](./querydsl-repository-archunit.md) - QueryDSL Repository ArchUnit

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
