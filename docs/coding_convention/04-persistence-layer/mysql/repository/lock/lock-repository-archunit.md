# Lock Repository ArchUnit 가이드

> **목적**: Lock Repository 아키텍처 규칙 자동 검증 (6개 규칙)

---

## 1️⃣ 검증 전략

### ArchUnit이 검증하는 것

**Lock Repository 클래스**:
- ✅ 클래스 타입 (class 키워드)
- ✅ @Repository 어노테이션
- ✅ JPAQueryFactory 또는 EntityManager 의존성
- ✅ `*LockRepository` 네이밍 규칙
- ✅ Lock 관련 메서드만 허용 (findByIdForUpdate, findByIdsForUpdate 등)
- ✅ 일반 조회/Command 메서드 금지

### 검증 그룹 (3개)

| 그룹 | 규칙 수 | 내용 |
|-----|--------|------|
| 1. 클래스 구조 규칙 | 3개 | 클래스 타입, @Repository, JPAQueryFactory/EntityManager |
| 2. 메서드 규칙 | 2개 | Lock 메서드만 허용, 일반 메서드 금지 |
| 3. 네이밍 규칙 | 1개 | *LockRepository 접미사 |

---

## 2️⃣ ArchUnit 테스트 코드

### 전체 테스트 구조

```java
package com.company.adapter.out.persistence.architecture.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * LockRepositoryArchTest - Lock Repository 아키텍처 규칙 검증 (6개 규칙)
 *
 * <p>lock-repository-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 그룹:</strong></p>
 * <ul>
 *   <li>클래스 구조 규칙 (3개)</li>
 *   <li>메서드 규칙 (2개)</li>
 *   <li>네이밍 규칙 (1개)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Lock Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class LockRepositoryArchTest {

    private static final String BASE_PACKAGE = "com.company.adapter.out.persistence";

    private static JavaClasses allClasses;
    private static JavaClasses lockRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        // LockRepository 클래스만
        lockRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "Lock Repository 클래스",
                javaClass -> javaClass.getSimpleName().endsWith("LockRepository") &&
                    !javaClass.isInterface()
            )
        );
    }

    // ========================================================================
    // 1. 클래스 구조 규칙 (3개)
    // ========================================================================

    @Nested
    @DisplayName("1. 클래스 구조 규칙")
    class ClassStructureRules {

        @Test
        @DisplayName("규칙 1-1: LockRepository는 클래스여야 합니다")
        void lockRepository_MustBeClass() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("LockRepository")
                .and().resideInAPackage("..repository..")
                .should().notBeInterfaces()
                .allowEmptyShould(true)
                .because("Lock Repository는 클래스로 정의되어야 합니다");

            rule.check(lockRepositoryClasses);
        }

        @Test
        @DisplayName("규칙 1-2: @Repository 어노테이션이 필수입니다")
        void lockRepository_MustHaveRepositoryAnnotation() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("LockRepository")
                .and().resideInAPackage("..repository..")
                .should().beAnnotatedWith(Repository.class)
                .allowEmptyShould(true)
                .because("Lock Repository는 @Repository 어노테이션이 필수입니다");

            rule.check(lockRepositoryClasses);
        }

        @Test
        @DisplayName("규칙 1-3: JPAQueryFactory 또는 EntityManager 의존성이 필수입니다")
        void lockRepository_MustHaveQueryFactoryOrEntityManager() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("LockRepository")
                .and().resideInAPackage("..repository..")
                .should().dependOnClassesThat().areAssignableTo(JPAQueryFactory.class)
                .orShould().dependOnClassesThat().areAssignableTo(EntityManager.class)
                .allowEmptyShould(true)
                .because("Lock Repository는 JPAQueryFactory 또는 EntityManager 의존성이 필수입니다");

            rule.check(lockRepositoryClasses);
        }
    }

    // ========================================================================
    // 2. 메서드 규칙 (2개)
    // ========================================================================

    @Nested
    @DisplayName("2. 메서드 규칙")
    class MethodRules {

        @Test
        @DisplayName("규칙 2-1: Lock 관련 메서드만 허용됩니다")
        void lockRepository_MustHaveOnlyLockMethods() {
            ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockRepository")
                .and().areDeclaredInClassesThat().resideInAPackage("..repository..")
                .and().arePublic()
                .and().areNotStatic()
                .and().doNotHaveName("equals")
                .and().doNotHaveName("hashCode")
                .and().doNotHaveName("toString")
                .should().haveNameMatching(".*ForUpdate.*|.*ForShare.*|.*WithLock.*")
                .allowEmptyShould(true)
                .because("Lock Repository는 Lock 관련 메서드만 허용됩니다 (findByIdForUpdate, findByIdsForUpdate, findByIdForShare 등)");

            rule.check(allClasses);
        }

        @Test
        @DisplayName("규칙 2-2: 일반 조회/Command 메서드가 금지됩니다")
        void lockRepository_MustNotHaveNonLockMethods() {
            ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockRepository")
                .and().haveNameMatching("save|delete|findById$|findAll|existsById$|count")
                .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("LockRepository")
                .allowEmptyShould(true)
                .because("Lock Repository는 일반 조회/Command 메서드가 금지됩니다 (JpaRepository, QueryDslRepository에서 처리)");

            rule.check(allClasses);
        }
    }

    // ========================================================================
    // 3. 네이밍 규칙 (1개)
    // ========================================================================

    @Nested
    @DisplayName("3. 네이밍 규칙")
    class NamingRules {

        @Test
        @DisplayName("규칙 3-1: *LockRepository 네이밍 규칙을 따라야 합니다")
        void lockRepository_MustFollowNamingConvention() {
            ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .and().areAnnotatedWith(Repository.class)
                .and().areNotInterfaces()
                .and().haveSimpleNameContaining("Lock")
                .should().haveSimpleNameEndingWith("LockRepository")
                .allowEmptyShould(true)
                .because("Lock Repository는 *LockRepository 네이밍 규칙을 따라야 합니다");

            rule.check(allClasses);
        }
    }
}
```

---

## 3️⃣ 규칙 상세 설명

### 규칙 1: LockRepository는 클래스

**검증 내용**: Repository 패키지의 `*LockRepository`는 class여야 함

**위반 예시**:
```java
// ❌ 인터페이스로 정의
public interface OrderLockRepository {
}
```

**올바른 예시**:
```java
// ✅ 클래스로 정의
@Repository
public class OrderLockRepository {
}
```

### 규칙 2: @Repository 어노테이션

**검증 내용**: `*LockRepository` 클래스는 `@Repository` 어노테이션 필수

**위반 예시**:
```java
// ❌ @Repository 없음
public class OrderLockRepository {
}
```

**올바른 예시**:
```java
// ✅ @Repository 어노테이션
@Repository
public class OrderLockRepository {
}
```

### 규칙 3: Lock 관련 메서드만 허용

**검증 내용**: `ForUpdate`, `ForShare`, `WithLock` 패턴의 메서드만 허용

**위반 예시**:
```java
// ❌ 일반 조회 메서드
@Repository
public class OrderLockRepository {
    public Optional<OrderJpaEntity> findById(Long id) {  // ❌
        // ...
    }
}
```

**올바른 예시**:
```java
// ✅ Lock 관련 메서드만
@Repository
public class OrderLockRepository {
    public Optional<OrderJpaEntity> findByIdForUpdate(Long id) {  // ✅
        return Optional.ofNullable(
            queryFactory.selectFrom(qOrder)
                .where(qOrder.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne()
        );
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행
./gradlew test

# Lock Repository ArchUnit만 실행
./gradlew test --tests "*LockRepositoryArchTest"
```

### IDE 실행

- IntelliJ IDEA: `LockRepositoryArchTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 5️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] **LockRepository 검증**
  - [ ] 클래스 타입 검증
  - [ ] @Repository 어노테이션 검증
  - [ ] JPAQueryFactory/EntityManager 의존성 검증
- [ ] **메서드 규칙 검증**
  - [ ] Lock 관련 메서드만 허용
  - [ ] 일반 조회/Command 메서드 금지
- [ ] **네이밍 규칙 검증**
  - [ ] *LockRepository 네이밍 규칙

---

## 6️⃣ 참고 문서

- [lock-repository-guide.md](./lock-repository-guide.md) - Lock Repository 컨벤션
- [jpa-repository-archunit.md](./jpa-repository-archunit.md) - JPA Repository ArchUnit
- [querydsl-repository-archunit.md](./querydsl-repository-archunit.md) - QueryDSL Repository ArchUnit

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
