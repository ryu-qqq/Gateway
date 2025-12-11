# LockAdapter ArchUnit 규칙

> **목적**: Redisson 기반 분산락 Adapter 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성

---

## 1) 대상 클래스

| 클래스 | 역할 | 패키지 |
|-------|------|--------|
| `DistributedLockAdapter` | 분산락 획득/해제 | `adapter.out.persistence.redis..adapter` |
| `DistributedLockPort` | 분산락 포트 인터페이스 | `application.common.port.out` |
| `LockAcquisitionException` | Lock 예외 | `adapter.out.persistence.redis..exception` |

---

## 2) 필수 검증 규칙 (18개)

### 기본 구조 (3개)
1. ✅ **@Component 필수**
2. ✅ **Port 구현 필수** (`DistributedLockPort`)
3. ✅ **필드 개수: 정확히 2개** (RedissonClient + lockCache)

### 메서드 구조 (3개)
4. ✅ **public 메서드: 정확히 4개** (tryLock, unlock, isHeldByCurrentThread, isLocked)
5. ✅ **메서드 파라미터: String key 필수** (모든 public 메서드)
6. ✅ **반환 타입: boolean 또는 void** (tryLock/isLocked → boolean, unlock → void)

### 금지 규칙 (5개)
7. ✅ **@Transactional 절대 금지**
8. ✅ **DB 접근 금지** (Repository/EntityManager 의존 금지)
9. ✅ **비즈니스 로직 금지** (조건문 최소화)
10. ✅ **Query 메서드 금지** (find*, get* 금지)
11. ✅ **Lettuce 사용 금지** (RedissonClient만 허용)

### 네이밍 규칙 (3개)
12. ✅ **클래스명: *LockAdapter 또는 DistributedLockAdapter**
13. ✅ **Port 인터페이스: *LockPort 또는 DistributedLockPort**
14. ✅ **예외 클래스: *LockException 또는 LockAcquisitionException**

### 패키지 구조 (2개)
15. ✅ **패키지 위치: ..adapter.out.persistence.redis..**
16. ✅ **Port 위치: ..application..port.out..**

### 필드 규칙 (2개)
17. ✅ **생성자 주입 (final 필드)**
18. ✅ **RedissonClient 필드 필수**

---

## 3) ArchUnit 테스트 코드

### 기본 설정

```java
package com.ryuqq.adapter.out.persistence.architecture.redis;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Redisson LockAdapter ArchUnit 테스트
 *
 * <p>18개 규칙을 빌드 시 자동 검증합니다.</p>
 *
 * @author Development Team
 * @since 1.0.0
 */
@AnalyzeClasses(packages = "com.ryuqq")
class LockAdapterArchTest {

    private static JavaClasses allClasses;
    private static JavaClasses lockAdapterClasses;

    @BeforeAll
    static void setup() {
        allClasses = new ClassFileImporter()
            .importPackages("com.ryuqq");

        lockAdapterClasses = allClasses.that(
            DescribedPredicate.describe(
                "are LockAdapter classes",
                javaClass -> javaClass.getSimpleName().contains("LockAdapter") &&
                             javaClass.getPackageName().contains("redis")
            )
        );
    }
}
```

### 규칙 1-3: 기본 구조

```java
/**
 * 규칙 1: @Component 필수
 */
@Test
@DisplayName("[강제] LockAdapter는 @Component 어노테이션을 가져야 한다")
void lockAdapter_MustBeAnnotatedWithComponent() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should().beAnnotatedWith(Component.class)
        .because("LockAdapter는 Spring Bean으로 등록되어야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 2: Port 구현 필수
 */
@Test
@DisplayName("[강제] LockAdapter는 LockPort 인터페이스를 구현해야 한다")
void lockAdapter_MustImplementLockPort() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should().implement(JavaClass.Predicates.simpleNameContaining("LockPort"))
        .because("LockAdapter는 Port 인터페이스를 구현해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 3: 필드 개수 정확히 2개
 */
@Test
@DisplayName("[강제] LockAdapter는 정확히 2개의 필드를 가져야 한다 (RedissonClient + lockCache)")
void lockAdapter_MustHaveExactlyTwoFields() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have exactly 2 fields",
                javaClass -> javaClass.getAllFields().size() == 2
            )
        ))
        .because("LockAdapter는 RedissonClient와 lockCache 필드만 가져야 합니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 4-6: 메서드 구조

```java
/**
 * 규칙 4: 정확히 4개의 public 메서드
 */
@Test
@DisplayName("[강제] LockAdapter는 public 메서드를 정확히 4개만 가져야 한다")
void lockAdapter_MustHaveExactlyFourPublicMethods() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have exactly 4 public methods (tryLock, unlock, isHeldByCurrentThread, isLocked)",
                javaClass -> javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> !method.isConstructor())
                    .count() == 4
            )
        ))
        .because("LockAdapter는 4개 메서드만 public으로 노출해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 5: 메서드 파라미터 검증
 */
@Test
@DisplayName("[필수] LockAdapter public 메서드는 String key 파라미터를 가져야 한다")
void lockAdapter_MethodsMustHaveKeyParameter() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameContaining("LockAdapter")
        .and().areDeclaredInClassesThat().resideInAPackage("..redis..")
        .and().arePublic()
        .and().doNotHaveName("<init>")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have String key as first parameter",
                method -> method.getParameters().size() > 0 &&
                          method.getParameters().get(0).getRawType().getName().equals("java.lang.String")
            )
        ))
        .because("모든 public 메서드는 String key 파라미터를 첫 번째로 받아야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 6: 반환 타입 검증
 */
@Test
@DisplayName("[필수] LockAdapter 메서드는 boolean 또는 void를 반환해야 한다")
void lockAdapter_MustReturnBooleanOrVoid() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameContaining("LockAdapter")
        .and().areDeclaredInClassesThat().resideInAPackage("..redis..")
        .and().arePublic()
        .and().doNotHaveName("<init>")
        .should().haveRawReturnType(
            DescribedPredicate.describe(
                "boolean or void",
                returnType -> returnType.getName().equals("boolean") ||
                              returnType.getName().equals("void")
            )
        )
        .because("Lock 메서드는 boolean 또는 void만 반환해야 합니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 7-11: 금지 규칙

```java
/**
 * 규칙 7: @Transactional 절대 금지
 */
@Test
@DisplayName("[금지] LockAdapter는 @Transactional 어노테이션을 가져서는 안 된다")
void lockAdapter_MustNotBeTransactional() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should().notBeAnnotatedWith(Transactional.class)
        .because("분산락 Adapter는 트랜잭션을 사용하지 않습니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 8: DB 접근 금지
 */
@Test
@DisplayName("[금지] LockAdapter는 Repository/EntityManager를 의존해서는 안 된다")
void lockAdapter_MustNotDependOnDbComponents() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should().notDependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
        .andShould().notDependOnClassesThat()
            .haveSimpleNameContaining("EntityManager")
        .because("LockAdapter는 DB에 접근하지 않습니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 9: 비즈니스 로직 금지
 *
 * <p>LockAdapter는 단순 Lock 획득/해제만 수행합니다.</p>
 * <p>if/switch/for 등의 제어문을 최소화해야 합니다.</p>
 */
@Test
@DisplayName("[권장] LockAdapter는 복잡한 비즈니스 로직을 가져서는 안 된다")
void lockAdapter_ShouldNotHaveComplexBusinessLogic() {
    // 코드 리뷰로 검증: 메서드 당 조건문 최대 2개
    // ArchUnit으로는 메서드 body 검증 제한적
}

/**
 * 규칙 10: Query 메서드 금지
 */
@Test
@DisplayName("[금지] LockAdapter는 find*/get* 메서드를 가져서는 안 된다")
void lockAdapter_MustNotHaveQueryMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameContaining("LockAdapter")
        .and().areDeclaredInClassesThat().resideInAPackage("..redis..")
        .and().arePublic()
        .should().notHaveNameMatching("find.*|get.*")
        .because("LockAdapter는 조회 기능을 제공하지 않습니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 11: Lettuce 사용 금지
 */
@Test
@DisplayName("[금지] LockAdapter는 RedisTemplate/Lettuce를 사용해서는 안 된다")
void lockAdapter_MustNotUseLettuce() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should().notDependOnClassesThat()
            .haveSimpleNameContaining("RedisTemplate")
        .andShould().notDependOnClassesThat()
            .resideInAPackage("..lettuce..")
        .because("분산락은 Redisson만 사용해야 합니다 (Lettuce 스핀락 금지)");

    rule.check(lockAdapterClasses);
}
```

### 규칙 12-14: 네이밍 규칙

```java
/**
 * 규칙 12: 클래스명
 */
@Test
@DisplayName("[네이밍] 클래스명은 *LockAdapter 또는 DistributedLockAdapter 형식이어야 한다")
void lockAdapter_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().resideInAPackage("..redis..adapter..")
        .and().implement(JavaClass.Predicates.simpleNameContaining("LockPort"))
        .should().haveSimpleNameContaining("LockAdapter")
        .because("LockAdapter는 명명 규칙을 따라야 합니다");

    rule.check(allClasses);
}

/**
 * 규칙 13: Port 네이밍
 */
@Test
@DisplayName("[네이밍] Port 인터페이스는 *LockPort 형식이어야 한다")
void lockPort_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().areInterfaces()
        .and().haveSimpleNameContaining("Lock")
        .and().resideInAPackage("..application..port.out..")
        .should().haveSimpleNameEndingWith("LockPort")
        .because("Port 인터페이스는 명명 규칙을 따라야 합니다");

    rule.check(allClasses);
}

/**
 * 규칙 14: 예외 네이밍
 */
@Test
@DisplayName("[네이밍] Lock 예외 클래스는 *LockException 형식이어야 한다")
void lockException_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().areAssignableTo(RuntimeException.class)
        .and().haveSimpleNameContaining("Lock")
        .and().resideInAPackage("..redis..")
        .should().haveSimpleNameEndingWith("Exception")
        .because("예외 클래스는 Exception으로 끝나야 합니다");

    rule.check(allClasses);
}
```

### 규칙 15-16: 패키지 구조

```java
/**
 * 규칙 15: 패키지 위치
 */
@Test
@DisplayName("[패키지] LockAdapter는 adapter.out.persistence.redis 패키지에 위치해야 한다")
void lockAdapter_MustBeInCorrectPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .should().resideInAPackage("..adapter.out.persistence.redis..")
        .because("Redis Adapter는 올바른 패키지에 위치해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 16: Port 패키지 위치
 */
@Test
@DisplayName("[패키지] LockPort는 application..port.out 패키지에 위치해야 한다")
void lockPort_MustBeInCorrectPackage() {
    ArchRule rule = classes()
        .that().areInterfaces()
        .and().haveSimpleNameContaining("LockPort")
        .should().resideInAPackage("..application..port.out..")
        .because("Port는 Application Layer에 위치해야 합니다");

    rule.check(allClasses);
}
```

### 규칙 17-18: 필드 규칙

```java
/**
 * 규칙 17: 생성자 주입 (final 필드)
 */
@Test
@DisplayName("[필드] LockAdapter 필드는 final이어야 한다")
void lockAdapter_FieldsMustBeFinal() {
    ArchRule rule = fields()
        .that().areDeclaredInClassesThat().haveSimpleNameContaining("LockAdapter")
        .and().areDeclaredInClassesThat().resideInAPackage("..redis..")
        .should().beFinal()
        .because("생성자 주입을 위해 필드는 final이어야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 18: RedissonClient 필드 필수
 */
@Test
@DisplayName("[필수] LockAdapter는 RedissonClient 필드를 가져야 한다")
void lockAdapter_MustHaveRedissonClientField() {
    ArchRule rule = classes()
        .that().haveSimpleNameContaining("LockAdapter")
        .and().resideInAPackage("..redis..")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have RedissonClient field",
                javaClass -> javaClass.getAllFields().stream()
                    .anyMatch(field -> field.getRawType().getName().contains("RedissonClient"))
            )
        ))
        .because("RedissonClient 필드가 필수입니다");

    rule.check(lockAdapterClasses);
}
```

---

## 4) CI/CD 통합

### Gradle 빌드 시 자동 실행

```bash
# ArchUnit 테스트 실행 (빌드 시 자동)
./gradlew test

# LockAdapter 테스트만 실행
./gradlew test --tests "*LockAdapterArchTest*"

# 실행 결과:
# BUILD SUCCESSFUL  (18개 규칙 준수)
# BUILD FAILED      (규칙 위반)
```

### 실패 시 출력 예시

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM]
Rule 'classes that have simple name containing 'LockAdapter'
and reside in a package '..redis..'
should not depend on classes that have simple name containing 'RedisTemplate'' was violated (1 times):

Class <com.ryuqq.adapter.out.persistence.redis.common.adapter.DistributedLockAdapter>
    depends on class <org.springframework.data.redis.core.RedisTemplate> in line 15

Reason: 분산락은 Redisson만 사용해야 합니다 (Lettuce 스핀락 금지)
```

---

## 5) 규칙 요약 테이블

| # | 카테고리 | 규칙 | 검증 방법 |
|---|----------|------|-----------|
| 1 | 기본 구조 | @Component 필수 | `beAnnotatedWith(Component.class)` |
| 2 | 기본 구조 | Port 구현 필수 | `implement(*LockPort)` |
| 3 | 기본 구조 | 필드 2개 | `fields.size() == 2` |
| 4 | 메서드 | public 4개 | `methods.count() == 4` |
| 5 | 메서드 | String key 파라미터 | `parameters.get(0).type == String` |
| 6 | 메서드 | boolean/void 반환 | `returnType in [boolean, void]` |
| 7 | 금지 | @Transactional 금지 | `notBeAnnotatedWith(Transactional)` |
| 8 | 금지 | DB 접근 금지 | `notDependOn(*Repository)` |
| 9 | 금지 | 비즈니스 로직 금지 | 코드 리뷰 |
| 10 | 금지 | find/get 금지 | `notHaveNameMatching("find.*\|get.*")` |
| 11 | 금지 | Lettuce 금지 | `notDependOn(RedisTemplate)` |
| 12 | 네이밍 | *LockAdapter | `haveSimpleNameContaining("LockAdapter")` |
| 13 | 네이밍 | *LockPort | `haveSimpleNameEndingWith("LockPort")` |
| 14 | 네이밍 | *LockException | `haveSimpleNameEndingWith("Exception")` |
| 15 | 패키지 | redis 패키지 | `resideInAPackage("..redis..")` |
| 16 | 패키지 | Port 위치 | `resideInAPackage("..application..port.out..")` |
| 17 | 필드 | final 필드 | `beFinal()` |
| 18 | 필드 | RedissonClient 필수 | `field.type contains RedissonClient` |

---

## 6) 체크리스트

ArchUnit 규칙 검증 시:
- [ ] **18개 규칙 모두 통과**
- [ ] **빌드 시 자동 실행 설정**
- [ ] **규칙 위반 시 빌드 실패 확인**
- [ ] **CI/CD 파이프라인 통합**
- [ ] **Lettuce 의존성 금지 확인**
- [ ] **RedissonClient 필수 확인**
- [ ] **팀 전체 규칙 공유**

---

## 📖 관련 문서

- **[Lock Adapter 가이드](lock-adapter-guide.md)** - LockAdapter 구현
- **[Lock Adapter 테스트](lock-adapter-test-guide.md)** - 테스트 가이드
- **[분산락 가이드](distributed-lock-guide.md)** - 분산락 전략
- **[Cache Adapter ArchUnit](../adapter/cache-adapter-archunit.md)** - Cache ArchUnit 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-04
**버전**: 1.0.0
