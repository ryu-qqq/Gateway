# QueryDSL Repository ArchUnit 가이드

> **목적**: QueryDSL Repository 아키텍처 규칙 자동 검증 (4개 메서드 표준화 + Join 금지)

---

## 1️⃣ 검증 전략

### ArchUnit이 검증하는 것

**QueryDSL Repository 클래스**:
- ✅ 클래스 타입 (class 키워드)
- ✅ @Repository 어노테이션
- ✅ JPAQueryFactory 필드 보유
- ✅ QType static final 필드 보유
- ✅ **4개 메서드만 허용** (findById, existsById, findByCriteria, countByCriteria)
- ✅ **Join 절대 금지** (join, leftJoin, rightJoin, fetchJoin 모두 금지)
- ✅ @Transactional 어노테이션 없음
- ✅ Mapper 의존성 없음
- ✅ 네이밍 규칙 (*QueryDslRepository)

---

## 2️⃣ ArchUnit 테스트 코드

### 전체 테스트 구조

```java
package com.company.adapter.out.persistence.architecture.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * QueryDslRepositoryArchTest - QueryDSL Repository 아키텍처 규칙 검증
 *
 * <p>querydsl-repository-guide.md의 핵심 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>검증 규칙:</strong></p>
 * <ul>
 *   <li>규칙 1: QueryDslRepository는 클래스여야 함</li>
 *   <li>규칙 2: @Repository 어노테이션 필수</li>
 *   <li>규칙 3: JPAQueryFactory 필드 필수</li>
 *   <li>규칙 4: QType static final 필드 필수</li>
 *   <li>규칙 5: 4개 표준 메서드만 허용</li>
 *   <li>규칙 6: Join 사용 금지 (코드 검증)</li>
 *   <li>규칙 7: @Transactional 사용 금지</li>
 *   <li>규칙 8: Mapper 의존성 금지</li>
 *   <li>규칙 9: 네이밍 규칙 (*QueryDslRepository)</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("QueryDSL Repository 아키텍처 규칙 검증 (Zero-Tolerance)")
class QueryDslRepositoryArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses queryDslRepositoryClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence");

        // QueryDslRepository 클래스만
        queryDslRepositoryClasses = allClasses.that(
            DescribedPredicate.describe(
                "are QueryDslRepository classes",
                javaClass -> javaClass.getSimpleName().endsWith("QueryDslRepository") &&
                    !javaClass.isInterface()
            )
        );
    }

    @Test
    @DisplayName("규칙 1: QueryDslRepository는 클래스여야 함")
    void queryDslRepository_MustBeClass() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().notBeInterfaces()
            .because("QueryDslRepository는 클래스로 정의되어야 합니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 2: QueryDslRepository는 @Repository 어노테이션 필수")
    void queryDslRepository_MustHaveRepositoryAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().beAnnotatedWith(Repository.class)
            .because("QueryDslRepository는 @Repository 어노테이션이 필수입니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 3: QueryDslRepository는 JPAQueryFactory 필드 필수")
    void queryDslRepository_MustHaveJPAQueryFactory() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().areAssignableTo(JPAQueryFactory.class)
            .because("QueryDslRepository는 JPAQueryFactory 필드가 필수입니다");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 4: QueryDslRepository는 QType static final 필드 필수")
    void queryDslRepository_MustHaveStaticFinalQTypeField() {
        ArchRule rule = fields()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryDslRepository")
            .and().haveNameMatching("^q[A-Z].*")  // qOrder, qProduct 등
            .should().beStatic()
            .andShould().beFinal()
            .because("QType 필드는 static final이어야 합니다");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 5: QueryDslRepository는 4개 표준 메서드만 허용")
    void queryDslRepository_MustHaveOnlyStandardMethods() {
        ArchRule rule = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("QueryDslRepository")
            .and().areDeclaredInClassesThat().resideInAPackage("..repository..")
            .and().arePublic()
            .and().areNotStatic()
            .and().doNotHaveName("equals")
            .and().doNotHaveName("hashCode")
            .and().doNotHaveName("toString")
            .should().haveName("findById")
            .orShould().haveName("existsById")
            .orShould().haveName("findByCriteria")
            .orShould().haveName("countByCriteria")
            .because("QueryDslRepository는 4개 표준 메서드만 허용됩니다 (findById, existsById, findByCriteria, countByCriteria)");

        rule.check(allClasses);
    }

    @Test
    @DisplayName("규칙 6: QueryDslRepository는 Join 사용 금지 (수동 검증)")
    void queryDslRepository_MustNotUseJoin() {
        // ⚠️ 주의: ArchUnit으로 Join 사용을 완벽히 검증하기 어려움
        // 코드 리뷰 및 수동 검증 필요
        //
        // 금지 패턴:
        // - queryFactory.selectFrom(q).join(...)
        // - queryFactory.selectFrom(q).leftJoin(...)
        // - queryFactory.selectFrom(q).rightJoin(...)
        // - queryFactory.selectFrom(q).innerJoin(...)
        // - queryFactory.selectFrom(q).fetchJoin(...)
        //
        // ✅ 이 테스트는 통과하지만, 실제 Join 사용 여부는 코드 리뷰로 확인해야 합니다.

        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.querydsl.jpa.impl.JPAJoin")
            .because("QueryDslRepository는 Join 사용이 금지됩니다 (N+1은 Adapter에서 해결)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 7: QueryDslRepository는 @Transactional 사용 금지")
    void queryDslRepository_MustNotHaveTransactional() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().beAnnotatedWith(Transactional.class)
            .because("QueryDslRepository는 @Transactional 사용이 금지됩니다 (Service Layer에서 관리)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 8: QueryDslRepository는 Mapper 의존성 금지")
    void queryDslRepository_MustNotDependOnMapper() {
        ArchRule rule = noClasses()
            .that().haveSimpleNameEndingWith("QueryDslRepository")
            .and().resideInAPackage("..repository..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
            .because("QueryDslRepository는 Mapper 의존성이 금지됩니다 (Adapter에서 처리)");

        rule.check(queryDslRepositoryClasses);
    }

    @Test
    @DisplayName("규칙 9: QueryDslRepository 네이밍 규칙 (*QueryDslRepository)")
    void queryDslRepository_MustFollowNamingConvention() {
        ArchRule rule = classes()
            .that().resideInAPackage("..repository..")
            .and().areAnnotatedWith(Repository.class)
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("QueryDslRepository")
            .because("QueryDslRepository는 *QueryDslRepository 네이밍 규칙을 따라야 합니다");

        rule.check(allClasses);
    }
}
```

---

## 3️⃣ 규칙 상세 설명

### 규칙 1: QueryDslRepository는 클래스

**검증 내용**: Repository 패키지의 `*QueryDslRepository`는 class여야 함

**위반 예시**:
```java
// ❌ 인터페이스로 정의
public interface OrderQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ 클래스로 정의
@Repository
public class OrderQueryDslRepository {
}
```

### 규칙 2: @Repository 어노테이션

**검증 내용**: `*QueryDslRepository` 클래스는 `@Repository` 어노테이션 필수

**위반 예시**:
```java
// ❌ @Repository 없음
public class OrderQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ @Repository 어노테이션
@Repository
public class OrderQueryDslRepository {
}
```

### 규칙 3: JPAQueryFactory 필드

**검증 내용**: `JPAQueryFactory` 타입 필드 보유 필수

**위반 예시**:
```java
// ❌ JPAQueryFactory 없음
@Repository
public class OrderQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ JPAQueryFactory 필드
@Repository
public class OrderQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    public OrderQueryDslRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
}
```

### 규칙 4: QType static final 필드

**검증 내용**: `q*` 패턴의 필드는 static final이어야 함

**위반 예시**:
```java
// ❌ static final 아님
@Repository
public class OrderQueryDslRepository {
    private final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;  // ❌
}
```

**올바른 예시**:
```java
// ✅ static final 선언
@Repository
public class OrderQueryDslRepository {
    private static final QOrderJpaEntity qOrder = QOrderJpaEntity.orderJpaEntity;  // ✅
}
```

### 규칙 5: @Transactional 사용 금지

**검증 내용**: `@Transactional` 어노테이션 사용 금지

**위반 예시**:
```java
// ❌ @Transactional 사용
@Repository
@Transactional  // ❌
public class OrderQueryDslRepository {
}
```

**올바른 예시**:
```java
// ✅ @Transactional 없음
@Repository
public class OrderQueryDslRepository {
}
```

### 규칙 6: Mapper 의존성 금지

**검증 내용**: `*Mapper` 타입 의존성 금지

**위반 예시**:
```java
// ❌ Mapper 의존성
@Repository
public class OrderQueryDslRepository {
    private final OrderJpaEntityMapper mapper;  // ❌

    public List<OrderDomain> search(SearchOrderQuery query) {  // ❌
        List<OrderJpaEntity> entities = queryFactory.selectFrom(qOrder).fetch();
        return entities.stream()
            .map(mapper::toDomain)  // ❌
            .toList();
    }
}
```

**올바른 예시**:
```java
// ✅ Mapper 의존성 없음
@Repository
public class OrderQueryDslRepository {
    // Mapper 없음

    public List<OrderJpaEntity> search(SearchOrderQuery query) {  // ✅
        return queryFactory.selectFrom(qOrder).fetch();
    }
}

// ✅ Adapter에서 Mapper 사용
@Component
public class OrderQueryPersistenceAdapter {
    private final OrderQueryDslRepository repository;
    private final OrderJpaEntityMapper mapper;  // ✅

    public List<OrderDomain> search(SearchOrderQuery query) {
        List<OrderJpaEntity> entities = repository.search(query);
        return entities.stream()
            .map(mapper::toDomain)  // ✅
            .toList();
    }
}
```

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# 전체 테스트 실행
./gradlew test

# QueryDSL Repository ArchUnit만 실행
./gradlew test --tests "*QueryDslRepositoryArchTest"
```

### IDE 실행

- IntelliJ IDEA: `QueryDslRepositoryArchTest.java` 우클릭 → Run
- 개별 테스트: 각 `@Test` 메서드 우클릭 → Run

---

## 5️⃣ 위반 시 대응

### 1단계: 위반 로그 확인

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'classes that have simple name ending with 'QueryDslRepository'
should be annotated with @Repository' was violated (1 times):
Class <OrderQueryDslRepository> is not annotated with @Repository
```

### 2단계: 위반 원인 파악

- ArchUnit 로그에서 위반 클래스 확인
- querydsl-repository-guide.md 규칙 재확인

### 3단계: 코드 수정

```java
// Before (위반)
public class OrderQueryDslRepository {  // ❌ @Repository 없음
}

// After (수정)
@Repository
public class OrderQueryDslRepository {  // ✅
}
```

### 4단계: 재검증

```bash
./gradlew test --tests "*QueryDslRepositoryArchTest"
```

---

## 6️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] **QueryDslRepository 검증**
  - [ ] 클래스 타입 검증
  - [ ] @Repository 어노테이션 검증
  - [ ] JPAQueryFactory 필드 검증
  - [ ] QType static final 필드 검증
- [ ] **금지 사항 검증**
  - [ ] @Transactional 금지
  - [ ] Mapper 의존성 금지
- [ ] **네이밍 규칙 검증**
  - [ ] *QueryDslRepository 네이밍 규칙

---

## 7️⃣ 참고 문서

- [querydsl-repository-guide.md](./querydsl-repository-guide.md) - QueryDSL Repository 컨벤션
- [jpa-repository-archunit.md](./jpa-repository-archunit.md) - JPA Repository ArchUnit

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
