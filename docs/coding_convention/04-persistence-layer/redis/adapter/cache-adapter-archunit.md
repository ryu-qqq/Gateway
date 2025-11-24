# CacheAdapter ArchUnit 검증 가이드

> **목적**: CacheAdapter 컨벤션 자동 검증 (ArchUnit)

---

## 1️⃣ 검증 전략

### ArchUnit 역할
**자동 검증**: 빌드 시 CacheAdapter 컨벤션 자동 검증

**검증 항목**:
- ✅ 네이밍 규칙: `*CacheAdapter`
- ✅ 어노테이션: `@Component` 필수
- ✅ 의존성: `RedisTemplate<String, Object>` 사용
- ✅ 금지 사항: `@Transactional`, 비즈니스 로직

---

## 2️⃣ 기본 템플릿

```java
package com.company.adapter.out.persistence.architecture.cache;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * CacheAdapter ArchUnit 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("archunit")
@Tag("cache")
@Tag("persistence-layer")
@DisplayName("CacheAdapter ArchUnit 검증")
class CacheAdapterArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence.redis");
    }

    @Test
    @DisplayName("CacheAdapter는 *CacheAdapter 네이밍 규칙을 따라야 한다")
    void cacheAdapterShouldFollowNamingConvention() {
        ArchRule rule = classes()
            .that().resideInAPackage("..adapter..")
            .and().haveSimpleNameEndingWith("CacheAdapter")
            .should().haveSimpleNameEndingWith("CacheAdapter");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 @Component 어노테이션을 가져야 한다")
    void cacheAdapterShouldHaveComponentAnnotation() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().beAnnotatedWith(Component.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 Port 인터페이스를 구현해야 한다")
    void cacheAdapterShouldImplementPort() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().implement(
                com.tngtech.archunit.base.DescribedPredicate.describe(
                    "Port interface",
                    cls -> cls.getInterfaces().stream()
                        .anyMatch(iface -> iface.getSimpleName().endsWith("CachePort"))
                )
            );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 RedisTemplate 필드를 가져야 한다")
    void cacheAdapterShouldHaveRedisTemplateField() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().haveOnlyFinalFields()
            .andShould().dependOnClassesThat().areAssignableTo(RedisTemplate.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 @Transactional을 사용하면 안 된다")
    void cacheAdapterShouldNotUseTransactional() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().notBeAnnotatedWith(Transactional.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter 메서드는 @Transactional을 사용하면 안 된다")
    void cacheAdapterMethodsShouldNotUseTransactional() {
        ArchRule rule = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("CacheAdapter")
            .should().beAnnotatedWith(Transactional.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 JpaRepository를 의존하면 안 된다")
    void cacheAdapterShouldNotDependOnJpaRepository() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().notDependOnClassesThat().haveNameMatching(".*Repository");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 다른 Adapter를 의존하면 안 된다")
    void cacheAdapterShouldNotDependOnOtherAdapters() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().notDependOnClassesThat().haveSimpleNameEndingWith("Adapter")
            .because("Cache Adapter should not depend on other Adapters");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 순환 의존성이 없어야 한다")
    void cacheAdapterShouldHaveNoCyclicDependencies() {
        ArchRule rule = slices()
            .matching("..adapter.out.persistence.redis.(*)..")
            .should().beFreeOfCycles();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter는 public 생성자를 가져야 한다 (Spring 빈 주입)")
    void cacheAdapterShouldHavePublicConstructor() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().haveOnlyPublicConstructors();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("CacheAdapter 패키지는 adapter 하위에 있어야 한다")
    void cacheAdapterShouldResideInAdapterPackage() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("CacheAdapter")
            .should().resideInAPackage("..adapter..");

        rule.check(importedClasses);
    }
}
```

---

## 3️⃣ 실전 예시 (Order)

```java
package com.company.adapter.out.persistence.architecture.cache;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Tag("archunit")
@Tag("cache")
@Tag("persistence-layer")
@DisplayName("Order CacheAdapter ArchUnit 검증")
class OrderCacheAdapterArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
            .importPackages("com.company.adapter.out.persistence.redis.order");
    }

    @Test
    @DisplayName("OrderCacheAdapter는 OrderCachePort를 구현해야 한다")
    void orderCacheAdapterShouldImplementOrderCachePort() {
        ArchRule rule = classes()
            .that().haveSimpleName("OrderCacheAdapter")
            .should().implement(
                com.company.application.order.port.out.OrderCachePort.class
            );

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("OrderCacheAdapter는 RedisTemplate을 의존해야 한다")
    void orderCacheAdapterShouldDependOnRedisTemplate() {
        ArchRule rule = classes()
            .that().haveSimpleName("OrderCacheAdapter")
            .should().dependOnClassesThat().haveNameMatching(".*RedisTemplate");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("OrderCacheAdapter는 @Component 어노테이션을 가져야 한다")
    void orderCacheAdapterShouldBeComponent() {
        ArchRule rule = classes()
            .that().haveSimpleName("OrderCacheAdapter")
            .should().beAnnotatedWith(org.springframework.stereotype.Component.class);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("OrderCacheAdapter는 @Transactional을 사용하면 안 된다")
    void orderCacheAdapterShouldNotUseTransactional() {
        ArchRule rule = classes()
            .that().haveSimpleName("OrderCacheAdapter")
            .should().notBeAnnotatedWith(
                org.springframework.transaction.annotation.Transactional.class
            );

        rule.check(importedClasses);
    }
}
```

---

## 4️⃣ Key 네이밍 규칙 검증 (심화)

```java
@Test
@DisplayName("CacheAdapter는 KEY_PREFIX 상수를 가져야 한다")
void cacheAdapterShouldHaveKeyPrefixConstant() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("CacheAdapter")
        .should().haveOnlyFinalFields()
        .andShould().containFieldsMatching("KEY_PREFIX.*");

    rule.check(importedClasses);
}

@Test
@DisplayName("CacheAdapter는 DEFAULT_TTL 상수를 가져야 한다")
void cacheAdapterShouldHaveDefaultTtlConstant() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("CacheAdapter")
        .should().haveOnlyFinalFields()
        .andShould().containFieldsMatching("DEFAULT_TTL.*");

    rule.check(importedClasses);
}
```

---

## 5️⃣ 디렉토리 구조

```
adapter-out/persistence-redis/
└─ src/test/java/
   └─ com/company/adapter/out/persistence/
       └─ architecture/cache/
           ├─ CacheAdapterArchUnitTest.java     ⭐ 전체 규칙
           └─ OrderCacheAdapterArchUnitTest.java  (선택)
```

---

## 6️⃣ 체크리스트

CacheAdapter ArchUnit 테스트 작성 시:
- [ ] **테스트 클래스 태그 추가** (필수)
  - [ ] `@Tag("archunit")` - ArchUnit 테스트 표시
  - [ ] `@Tag("cache")` - Cache Adapter 테스트 표시
  - [ ] `@Tag("persistence-layer")` - Persistence Layer 표시
- [ ] **네이밍 규칙 검증** (`*CacheAdapter`)
- [ ] **어노테이션 검증** (`@Component` 필수)
- [ ] **Port 구현 검증** (`*CachePort` 구현)
- [ ] **RedisTemplate 의존성 검증**
- [ ] **@Transactional 금지 검증**
- [ ] **JpaRepository 의존 금지 검증**
- [ ] **순환 의존성 금지 검증**
- [ ] **public 생성자 검증**
- [ ] **패키지 위치 검증** (`..adapter..`)
- [ ] **KEY_PREFIX 상수 검증** (선택)
- [ ] **DEFAULT_TTL 상수 검증** (선택)

---

## 📖 관련 문서

- **[CacheAdapter Guide](cache-adapter-guide.md)** - CacheAdapter 구현 가이드
- **[CacheAdapter Test Guide](cache-adapter-test-guide.md)** - 단위 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
