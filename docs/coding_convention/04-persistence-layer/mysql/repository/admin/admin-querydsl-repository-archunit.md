# Admin QueryDSL Repository ArchUnit 가이드

> **목적**: Admin QueryDSL Repository 아키텍처 규칙 자동 검증 (6개 규칙)

---

## 1️⃣ 검증 전략

### ArchUnit이 검증하는 것

**Admin QueryDSL Repository 클래스**:
- ✅ 클래스 타입 (class 키워드)
- ✅ @Repository 어노테이션
- ✅ JPAQueryFactory 의존성
- ✅ `*AdminQueryDslRepository` 네이밍 규칙
- ✅ DTO Projection 반환 권장
- ✅ @Transactional 금지 (Service Layer에서 관리)

### 검증 그룹 (3개)

| 그룹 | 규칙 수 | 내용 |
|-----|--------|------|
| 1. 클래스 구조 규칙 | 3개 | 클래스 타입, @Repository, JPAQueryFactory |
| 2. 금지 사항 규칙 | 2개 | @Transactional, Mapper 의존성 금지 |
| 3. 네이밍 규칙 | 1개 | *AdminQueryDslRepository 접미사 |

### 일반 QueryDslRepository와 차이점

| 항목 | QueryDslRepository | AdminQueryDslRepository |
|------|-------------------|------------------------|
| **메서드 제한** | 4개 고정 | ✅ 자유 |
| **Join** | ❌ 금지 | ✅ 허용 |
| **반환 타입** | Entity | DTO Projection 권장 |

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * AdminQueryDslRepositoryArchTest - Admin QueryDSL Repository 아키텍처 규칙 검증 (6개 규칙)
 *
 * <p>querydsl-repository-guide.md의 Admin 섹션 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>일반 QueryDslRepository와 차이점:</strong></p>
 * <ul>
 *   <li>✅ Join 허용 (Long FK 기반)</li>
 *   <li>✅ 메서드 제한 없음</li>
 *   <li>✅ DTO Projection 권장</li>
 * </ul>
 *
 * <p><strong>검증 그룹:</strong></p>
 * <ul>
 *   <li>클래스 구조 규칙 (3개)</li>
 *   <li>금지 사항 규칙 (2개)</li>
 *   <li>네이밍 규칙 (1개)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Admin QueryDSL Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class AdminQueryDslRepositoryArchTest {

    private static final String BASE_PACKAGE = "com.company.adapter.out.persistence";

    private static JavaClasses allClasses;
    private static JavaClasses adminQueryDslRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        // AdminQueryDslRepository 클래스만
        adminQueryDslRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "Admin QueryDSL Repository 클래스",
                javaClass -> javaClass.getSimpleName().endsWith("AdminQueryDslRepository") &&
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
        @DisplayName("규칙 1-1: AdminQueryDslRepository는 클래스여야 합니다")
        void adminQueryDslRepository_MustBeClass() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .and().resideInAPackage("..repository..")
                .should().notBeInterfaces()
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 클래스로 정의되어야 합니다");

            rule.check(adminQueryDslRepositoryClasses);
        }

        @Test
        @DisplayName("규칙 1-2: @Repository 어노테이션이 필수입니다")
        void adminQueryDslRepository_MustHaveRepositoryAnnotation() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .and().resideInAPackage("..repository..")
                .should().beAnnotatedWith(Repository.class)
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 @Repository 어노테이션이 필수입니다");

            rule.check(adminQueryDslRepositoryClasses);
        }

        @Test
        @DisplayName("규칙 1-3: JPAQueryFactory 의존성이 필수입니다")
        void adminQueryDslRepository_MustHaveJPAQueryFactory() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .and().resideInAPackage("..repository..")
                .should().dependOnClassesThat().areAssignableTo(JPAQueryFactory.class)
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 JPAQueryFactory 의존성이 필수입니다");

            rule.check(adminQueryDslRepositoryClasses);
        }
    }

    // ========================================================================
    // 2. 금지 사항 규칙 (2개)
    // ========================================================================

    @Nested
    @DisplayName("2. 금지 사항 규칙")
    class ProhibitionRules {

        @Test
        @DisplayName("규칙 2-1: @Transactional 사용이 금지됩니다")
        void adminQueryDslRepository_MustNotHaveTransactional() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .and().resideInAPackage("..repository..")
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 @Transactional 사용이 금지됩니다 (Service Layer에서 관리)");

            rule.check(adminQueryDslRepositoryClasses);
        }

        @Test
        @DisplayName("규칙 2-2: Mapper 의존성이 금지됩니다")
        void adminQueryDslRepository_MustNotDependOnMapper() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .and().resideInAPackage("..repository..")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 Mapper 의존성이 금지됩니다 (DTO Projection 직접 사용 또는 Adapter에서 처리)");

            rule.check(adminQueryDslRepositoryClasses);
        }
    }

    // ========================================================================
    // 3. 네이밍 규칙 (1개)
    // ========================================================================

    @Nested
    @DisplayName("3. 네이밍 규칙")
    class NamingRules {

        @Test
        @DisplayName("규칙 3-1: *AdminQueryDslRepository 네이밍 규칙을 따라야 합니다")
        void adminQueryDslRepository_MustFollowNamingConvention() {
            ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .and().areAnnotatedWith(Repository.class)
                .and().areNotInterfaces()
                .and().haveSimpleNameContaining("Admin")
                .and().haveSimpleNameContaining("QueryDsl")
                .should().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .allowEmptyShould(true)
                .because("Admin QueryDSL Repository는 *AdminQueryDslRepository 네이밍 규칙을 따라야 합니다");

            rule.check(allClasses);
        }
    }
}
```

---

## 3️⃣ 규칙 상세 설명

### 규칙 1: AdminQueryDslRepository는 클래스

**검증 내용**: Repository 패키지의 `*AdminQueryDslRepository`는 class여야 함

**위반 예시**:
```java
// ❌ 인터페이스로 정의
public interface OrderAdminQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ 클래스로 정의
@Repository
public class OrderAdminQueryDslRepository {
}
```

### 규칙 2: @Repository 어노테이션

**검증 내용**: `*AdminQueryDslRepository` 클래스는 `@Repository` 어노테이션 필수

**위반 예시**:
```java
// ❌ @Repository 없음
public class OrderAdminQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ @Repository 어노테이션
@Repository
public class OrderAdminQueryDslRepository {
}
```

### 규칙 3: Join 허용 (일반 QueryDslRepository와 차이)

**Admin은 Join 허용**:
```java
// ✅ Admin에서 Join 허용
@Repository
public class OrderAdminQueryDslRepository {

    public List<AdminOrderResponse> findOrderListWithMember(AdminOrderListQuery criteria) {
        return queryFactory
            .select(Projections.constructor(
                AdminOrderResponse.class,
                qOrder.id,
                qOrder.orderNumber,
                qMember.name,
                qMember.email
            ))
            .from(qOrder)
            .leftJoin(qMember).on(qOrder.memberId.eq(qMember.id))  // ✅ Join 허용
            .where(buildConditions(criteria))
            .fetch();
    }
}
```

### 규칙 4: @Transactional 금지

**검증 내용**: `@Transactional` 어노테이션 사용 금지 (Service Layer에서 관리)

**위반 예시**:
```java
// ❌ @Transactional 사용
@Repository
@Transactional  // ❌
public class OrderAdminQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ @Transactional 없음
@Repository
public class OrderAdminQueryDslRepository {
}
```

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행
./gradlew test

# Admin QueryDSL Repository ArchUnit만 실행
./gradlew test --tests "*AdminQueryDslRepositoryArchTest"
```

### IDE 실행

- IntelliJ IDEA: `AdminQueryDslRepositoryArchTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 5️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] **AdminQueryDslRepository 검증**
  - [ ] 클래스 타입 검증
  - [ ] @Repository 어노테이션 검증
  - [ ] JPAQueryFactory 의존성 검증
- [ ] **금지 사항 검증**
  - [ ] @Transactional 금지
  - [ ] Mapper 의존성 금지
- [ ] **네이밍 규칙 검증**
  - [ ] *AdminQueryDslRepository 네이밍 규칙

---

## 6️⃣ 참고 문서

- [querydsl-repository-guide.md](./querydsl-repository-guide.md) - QueryDSL Repository 컨벤션 (Admin 섹션 포함)
- [querydsl-repository-archunit.md](./querydsl-repository-archunit.md) - 일반 QueryDSL Repository ArchUnit
- [jpa-repository-archunit.md](./jpa-repository-archunit.md) - JPA Repository ArchUnit

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
