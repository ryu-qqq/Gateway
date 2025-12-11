# AdminQueryAdapter ArchUnit 가이드

> **목적**: Admin Query Adapter 아키텍처 규칙 자동 검증 (6개 규칙)

---

## 1️⃣ 검증 전략

### ArchUnit이 검증하는 것

**Admin Query Adapter 클래스**:
- ✅ 클래스 타입 (class 키워드)
- ✅ @Component 어노테이션
- ✅ AdminQueryDslRepository 의존성
- ✅ `*AdminQueryAdapter` 네이밍 규칙
- ✅ AdminQueryPort 인터페이스 구현
- ✅ @Transactional 금지 (Service Layer에서 관리)

### 검증 그룹 (3개)

| 그룹 | 규칙 수 | 내용 |
|-----|--------|------|
| 1. 클래스 구조 규칙 | 3개 | 클래스 타입, @Component, Port 구현 |
| 2. 의존성 규칙 | 2개 | AdminQueryDslRepository 필수, 다른 Repository 금지 |
| 3. 금지 사항 규칙 | 1개 | @Transactional 금지 |

---

## 2️⃣ ArchUnit 테스트 코드

### 전체 테스트 구조

```java
package com.company.adapter.out.persistence.architecture.adapter;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * AdminQueryAdapterArchTest - Admin Query Adapter 아키텍처 규칙 검증 (6개 규칙)
 *
 * <p>admin-query-adapter-guide.md 규칙을 ArchUnit으로 검증합니다.</p>
 *
 * <p><strong>일반 QueryAdapter와 차이점:</strong></p>
 * <ul>
 *   <li>✅ AdminQueryDslRepository와 1:1 매핑</li>
 *   <li>✅ DTO Projection 직접 반환 (Domain 아님)</li>
 *   <li>✅ 메서드 제한 없음</li>
 * </ul>
 *
 * @author Development Team
 * @since 1.0.0
 */
@DisplayName("Admin Query Adapter 아키텍처 규칙 검증 (Zero-Tolerance)")
class AdminQueryAdapterArchTest {

    private static final String BASE_PACKAGE = "com.company.adapter.out.persistence";

    private static JavaClasses allClasses;
    private static JavaClasses adminQueryAdapterClasses;

    @BeforeAll
    static void setUp() {
        allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        // AdminQueryAdapter 클래스만
        adminQueryAdapterClasses = allClasses.that(
            DescribedPredicate.describe(
                "Admin Query Adapter 클래스",
                javaClass -> javaClass.getSimpleName().endsWith("AdminQueryAdapter") &&
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
        @DisplayName("규칙 1-1: AdminQueryAdapter는 클래스여야 합니다")
        void adminQueryAdapter_MustBeClass() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().notBeInterfaces()
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 클래스로 정의되어야 합니다");

            rule.check(adminQueryAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-2: @Component 어노테이션이 필수입니다")
        void adminQueryAdapter_MustHaveComponentAnnotation() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().beAnnotatedWith(Component.class)
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 @Component 어노테이션이 필수입니다");

            rule.check(adminQueryAdapterClasses);
        }

        @Test
        @DisplayName("규칙 1-3: AdminQueryPort 인터페이스를 구현해야 합니다")
        void adminQueryAdapter_MustImplementPort() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().implement(
                    DescribedPredicate.describe(
                        "AdminQueryPort 인터페이스",
                        javaClass -> javaClass.getSimpleName().endsWith("AdminQueryPort")
                    )
                )
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 AdminQueryPort 인터페이스를 구현해야 합니다");

            rule.check(adminQueryAdapterClasses);
        }
    }

    // ========================================================================
    // 2. 의존성 규칙 (2개)
    // ========================================================================

    @Nested
    @DisplayName("2. 의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("규칙 2-1: AdminQueryDslRepository 의존성이 필수입니다")
        void adminQueryAdapter_MustDependOnAdminQueryDslRepository() {
            ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("AdminQueryDslRepository")
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 AdminQueryDslRepository 의존성이 필수입니다 (1:1 매핑)");

            rule.check(adminQueryAdapterClasses);
        }

        @Test
        @DisplayName("규칙 2-2: 다른 타입의 Repository 의존성이 금지됩니다")
        void adminQueryAdapter_MustNotDependOnOtherRepositories() {
            // JpaRepository, QueryDslRepository, LockRepository 등 금지
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().dependOnClassesThat(
                    DescribedPredicate.describe(
                        "AdminQueryDslRepository가 아닌 다른 Repository",
                        javaClass -> javaClass.getSimpleName().endsWith("Repository") &&
                            !javaClass.getSimpleName().endsWith("AdminQueryDslRepository")
                    )
                )
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 AdminQueryDslRepository만 의존해야 합니다 (1:1 매핑 원칙)");

            rule.check(adminQueryAdapterClasses);
        }
    }

    // ========================================================================
    // 3. 금지 사항 규칙 (1개)
    // ========================================================================

    @Nested
    @DisplayName("3. 금지 사항 규칙")
    class ProhibitionRules {

        @Test
        @DisplayName("규칙 3-1: @Transactional 사용이 금지됩니다")
        void adminQueryAdapter_MustNotHaveTransactional() {
            ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("AdminQueryAdapter")
                .and().resideInAPackage("..adapter..")
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .because("Admin Query Adapter는 @Transactional 사용이 금지됩니다 (Service Layer에서 관리)");

            rule.check(adminQueryAdapterClasses);
        }
    }

    // ========================================================================
    // 참고: Admin Query Adapter 구현 예시
    // ========================================================================
    //
    // @Component
    // public class OrderAdminQueryAdapter implements OrderAdminQueryPort {
    //
    //     private final OrderAdminQueryDslRepository adminQueryDslRepository;
    //
    //     public OrderAdminQueryAdapter(OrderAdminQueryDslRepository adminQueryDslRepository) {
    //         this.adminQueryDslRepository = adminQueryDslRepository;
    //     }
    //
    //     @Override
    //     public List<AdminOrderListResponse> findList(AdminOrderSearchCriteria criteria) {
    //         return adminQueryDslRepository.findList(criteria);
    //     }
    //
    //     @Override
    //     public Optional<AdminOrderDetailResponse> findDetail(OrderId id) {
    //         return adminQueryDslRepository.findDetail(id.getValue());
    //     }
    // }
    //
}
```

---

## 3️⃣ 규칙 상세 설명

### 규칙 1: AdminQueryAdapter는 클래스

**검증 내용**: Adapter 패키지의 `*AdminQueryAdapter`는 class여야 함

**위반 예시**:
```java
// ❌ 인터페이스로 정의
public interface OrderAdminQueryAdapter {
}
```

**올바른 예시**:
```java
// ✅ 클래스로 정의
@Component
public class OrderAdminQueryAdapter implements OrderAdminQueryPort {
}
```

### 규칙 2: 1:1 매핑 (AdminQueryDslRepository만)

**검증 내용**: AdminQueryDslRepository만 의존, 다른 Repository 금지

**위반 예시**:
```java
// ❌ 다른 Repository 의존
@Component
public class OrderAdminQueryAdapter implements OrderAdminQueryPort {
    private final OrderAdminQueryDslRepository adminRepo;
    private final MemberQueryDslRepository memberRepo;  // ❌ 금지!
}
```

**올바른 예시**:
```java
// ✅ AdminQueryDslRepository만 의존
@Component
public class OrderAdminQueryAdapter implements OrderAdminQueryPort {
    private final OrderAdminQueryDslRepository adminQueryDslRepository;  // ✅ 1:1 매핑
}
```

---

## 4️⃣ 체크리스트

ArchUnit 테스트 작성 시:
- [ ] **클래스 구조 검증**
  - [ ] 클래스 타입 검증
  - [ ] @Component 어노테이션 검증
  - [ ] AdminQueryPort 인터페이스 구현 검증
- [ ] **의존성 검증**
  - [ ] AdminQueryDslRepository 의존성 필수
  - [ ] 다른 Repository 의존성 금지
- [ ] **금지 사항 검증**
  - [ ] @Transactional 금지

---

## 5️⃣ 참고 문서

- [admin-query-adapter-guide.md](./admin-query-adapter-guide.md) - AdminQueryAdapter 컨벤션
- [admin-querydsl-repository-archunit.md](../../../repository/admin/admin-querydsl-repository-archunit.md) - AdminQueryDslRepository ArchUnit

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
