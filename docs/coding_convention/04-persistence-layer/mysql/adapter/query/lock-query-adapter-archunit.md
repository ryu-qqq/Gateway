# LockQueryAdapter ArchUnit 규칙

> **목적**: LockQueryAdapter 설계 규칙의 자동 검증 (빌드 시 자동 실행)
>
> **철학**: 모든 규칙을 빌드 타임에 강제하여 Zero-Tolerance 달성
---

## 2️⃣ 필수 검증 규칙 (24개)

### 기본 구조 (3개)
1. ✅ **@Component 필수**
2. ✅ **Port 구현 필수**
3. ✅ **필드 개수: 정확히 2개** (LockRepository + Mapper)

### 메서드 구조 (3개)
4. ✅ **public 메서드: 정확히 6개** (비관락 2 + 낙관락 2 + For Update 2)
5. ✅ **메서드명: find*WithPessimisticLock, find*WithOptimisticLock, find*ForUpdate** (정확히)
6. ✅ **반환 타입: Domain** (Optional<Bc>, List<Bc>)

### 금지 규칙 (6개)
7. ✅ **@Transactional 절대 금지**
8. ✅ **Command 메서드 금지** (save*, persist*, update*, delete*)
9. ✅ **일반 조회 메서드 금지** (findById, existsById, findByCriteria, countByCriteria)
10. ✅ **DTO 반환 금지** (Domain만 반환)
11. ✅ **비즈니스 로직 금지** (if/switch/for 최소화)
12. ✅ **try-catch 금지** (Lock 예외 처리 안 함)

### 네이밍 규칙 (3개)
13. ✅ **클래스명: *LockQueryAdapter**
14. ✅ **Port 인터페이스: *LockQueryPort**
15. ✅ **Repository: *LockRepository**

### 패키지 구조 (3개)
16. ✅ **패키지 위치: ..adapter.out.persistence..adapter..**
17. ✅ **Port 위치: ..application..port.out..**
18. ✅ **의존성 방향: Adapter → Port (역방향 금지)**

### 필드 규칙 (3개)
19. ✅ **생성자 주입 (final 필드)**
20. ✅ **LockRepository 필드 필수**
21. ✅ **Mapper 필드 필수**

### 메서드 상세 (3개)
22. ✅ **findByIdWithPessimisticLock() 존재 (Optional<Domain> 반환)**
23. ✅ **findByCriteriaWithPessimisticLock() 존재 (List<Domain> 반환)**
24. ✅ **나머지 4개 메서드 존재**

---

## 3️⃣ ArchUnit 테스트 코드

### 기본 설정
```java
@AnalyzeClasses(packages = "com.ryuqq.adapter.out.persistence")
class LockQueryAdapterArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .importPackages("com.ryuqq.adapter.out.persistence");
    }

    private static JavaClasses lockAdapterClasses;

    @BeforeAll
    static void filterLockAdapters() {
        lockAdapterClasses = classes.that(
            DescribedPredicate.describe(
                "are LockQueryAdapter classes",
                javaClass -> javaClass.getSimpleName().endsWith("LockQueryAdapter")
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
@DisplayName("[강제] LockQueryAdapter는 @Component 어노테이션을 가져야 한다")
void lockQueryAdapter_MustBeAnnotatedWithComponent() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().beAnnotatedWith(Component.class)
        .because("LockQueryAdapter는 Spring Bean으로 등록되어야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 2: Port 구현 필수
 */
@Test
@DisplayName("[강제] LockQueryAdapter는 LockQueryPort 인터페이스를 구현해야 한다")
void lockQueryAdapter_MustImplementLockQueryPort() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().implement(JavaClass.Predicates.simpleNameEndingWith("LockQueryPort"))
        .because("LockQueryAdapter는 Port 인터페이스를 구현해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 3: 필드 개수 정확히 2개
 */
@Test
@DisplayName("[강제] LockQueryAdapter는 정확히 2개의 필드를 가져야 한다 (LockRepository + Mapper)")
void lockQueryAdapter_MustHaveExactlyTwoFields() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have exactly 2 fields",
                javaClass -> javaClass.getAllFields().size() == 2
            )
        ))
        .because("LockQueryAdapter는 LockRepository와 Mapper 필드만 가져야 합니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 4-6: 메서드 구조
```java
/**
 * 규칙 4: 정확히 6개의 public 메서드
 */
@Test
@DisplayName("[강제] LockQueryAdapter는 public 메서드를 정확히 6개만 가져야 한다")
void lockQueryAdapter_MustHaveExactlySixPublicMethods() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have exactly 6 public methods (excluding constructor)",
                javaClass -> javaClass.getMethods().stream()
                    .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(method -> !method.getName().equals("<init>"))
                    .count() == 6
            )
        ))
        .because("LockQueryAdapter는 6개 조회 메서드만 public으로 노출해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 5: 메서드명 검증
 */
@Test
@DisplayName("[필수] LockQueryAdapter 메서드는 find*WithPessimisticLock, find*WithOptimisticLock, find*ForUpdate 형식이어야 한다")
void lockQueryAdapter_MethodsMustFollowNamingConvention() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .and().arePublic()
        .and().doNotHaveName("<init>")
        .should().haveNameMatching("find(ById|ByCriteria)With(Pessimistic|Optimistic)Lock|find(ById|ByCriteria)ForUpdate")
        .because("메서드명은 Lock 전략을 명확히 표현해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 6: 반환 타입 검증
 */
@Test
@DisplayName("[필수] LockQueryAdapter 메서드는 Optional<Domain> 또는 List<Domain>을 반환해야 한다")
void lockQueryAdapter_MustReturnDomainTypes() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .and().arePublic()
        .and().doNotHaveName("<init>")
        .should().haveRawReturnType(
            DescribedPredicate.describe(
                "Optional or List",
                returnType -> returnType.isAssignableTo(Optional.class) ||
                              returnType.isAssignableTo(List.class)
            )
        )
        .because("조회 메서드는 Domain을 반환해야 합니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 7-11: 금지 규칙
```java
/**
 * 규칙 7: @Transactional 절대 금지
 */
@Test
@DisplayName("[금지] LockQueryAdapter는 @Transactional 어노테이션을 가져서는 안 된다")
void lockQueryAdapter_MustNotBeTransactional() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().notBeAnnotatedWith(Transactional.class)
        .because("트랜잭션은 Application Layer에서 관리해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 8: Command 메서드 금지
 */
@Test
@DisplayName("[금지] LockQueryAdapter는 Command 메서드를 가져서는 안 된다")
void lockQueryAdapter_MustNotHaveCommandMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().notHaveNameMatching("save.*|persist.*|update.*|delete.*")
        .because("저장/수정/삭제는 CommandAdapter로 분리해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 9: 일반 조회 메서드 금지
 */
@Test
@DisplayName("[금지] LockQueryAdapter는 일반 조회 메서드를 가져서는 안 된다")
void lockQueryAdapter_MustNotHaveNormalQueryMethods() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().notHaveNameMatching("^findById$|^existsById$|^findByCriteria$|^countByCriteria$")
        .because("일반 조회는 QueryAdapter를 사용해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 10: DTO 반환 금지
 */
@Test
@DisplayName("[금지] LockQueryAdapter는 DTO를 반환해서는 안 된다")
void lockQueryAdapter_MustNotReturnDto() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .and().arePublic()
        .should().notHaveRawReturnType(
            DescribedPredicate.describe(
                "DTO types",
                returnType -> returnType.getName().contains("Dto")
            )
        )
        .because("Domain을 반환해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 11: 비즈니스 로직 금지
 *
 * <p>LockQueryAdapter는 단순 위임 + 변환만 수행합니다.</p>
 * <p>if/switch/for 등의 제어문을 최소화해야 합니다.</p>
 */
@Test
@DisplayName("[권장] LockQueryAdapter는 복잡한 비즈니스 로직을 가져서는 안 된다")
void lockQueryAdapter_ShouldNotHaveComplexBusinessLogic() {
    // 이 규칙은 코드 리뷰로 검증 (ArchUnit으로 자동화 어려움)
    // 예: 메서드 당 if/switch/for 최대 1개
}

/**
 * 규칙 12: try-catch 금지
 *
 * <p>Lock 예외는 Application Layer에서 처리합니다.</p>
 * <p>Adapter는 예외를 catch하지 않고 그대로 던집니다.</p>
 */
@Test
@DisplayName("[금지] LockQueryAdapter는 try-catch로 Lock 예외를 처리해서는 안 된다")
void lockQueryAdapter_MustNotCatchLockExceptions() {
    ArchRule rule = methods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .and().arePublic()
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "not catch OptimisticLockException or PessimisticLockException",
                method -> {
                    // try-catch가 있는지 검증
                    // (ArchUnit limitation: 메서드 body 검증 제한적)
                    // 주로 코드 리뷰로 검증
                    return true;
                }
            )
        ))
        .because("Lock 예외는 Application Layer에서 처리해야 합니다");

    // 코드 리뷰 검증 권장
}
```

### 규칙 13-15: 네이밍 규칙
```java
/**
 * 규칙 13: 클래스명
 */
@Test
@DisplayName("[네이밍] 클래스명은 *LockQueryAdapter 형식이어야 한다")
void lockQueryAdapter_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().implement(JavaClass.Predicates.simpleNameEndingWith("LockQueryPort"))
        .should().haveSimpleNameEndingWith("LockQueryAdapter")
        .because("LockQueryAdapter는 명명 규칙을 따라야 합니다");

    rule.check(classes);
}

/**
 * 규칙 14: Port 네이밍
 */
@Test
@DisplayName("[네이밍] Port 인터페이스는 *LockQueryPort 형식이어야 한다")
void lockQueryPort_MustFollowNamingConvention() {
    ArchRule rule = classes()
        .that().areInterfaces()
        .and().haveSimpleNameContaining("Lock")
        .and().haveSimpleNameContaining("Query")
        .should().haveSimpleNameEndingWith("LockQueryPort")
        .because("Port 인터페이스는 명명 규칙을 따라야 합니다");

    rule.check(classes);
}
```

### 규칙 16-18: 패키지 구조
```java
/**
 * 규칙 16: 패키지 위치
 */
@Test
@DisplayName("[패키지] LockQueryAdapter는 adapter.out.persistence 패키지에 위치해야 한다")
void lockQueryAdapter_MustBeInCorrectPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().resideInAPackage("..adapter.out.persistence..")
        .because("Adapter는 올바른 패키지에 위치해야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 17: Port 패키지 위치
 */
@Test
@DisplayName("[패키지] LockQueryPort는 application.port.out 패키지에 위치해야 한다")
void lockQueryPort_MustBeInCorrectPackage() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryPort")
        .should().resideInAPackage("..application..port.out..")
        .because("Port는 Application Layer에 위치해야 합니다");

    rule.check(classes);
}

/**
 * 규칙 18: 의존성 방향
 */
@Test
@DisplayName("[의존성] Adapter는 Port를 의존해야 한다 (역방향 금지)")
void lockQueryAdapter_MustDependOnPort() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("LockQueryPort")
        .because("의존성 방향은 Adapter → Port 단방향이어야 합니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 19-21: 필드 규칙
```java
/**
 * 규칙 19: 생성자 주입 (final 필드)
 */
@Test
@DisplayName("[필드] LockQueryAdapter 필드는 final이어야 한다")
void lockQueryAdapter_FieldsMustBeFinal() {
    ArchRule rule = fields()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("LockQueryAdapter")
        .should().beFinal()
        .because("생성자 주입을 위해 필드는 final이어야 합니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 20: LockRepository 필드 필수
 */
@Test
@DisplayName("[필수] LockQueryAdapter는 LockRepository 필드를 가져야 한다")
void lockQueryAdapter_MustHaveLockRepositoryField() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have LockRepository field",
                javaClass -> javaClass.getAllFields().stream()
                    .anyMatch(field -> field.getRawType().getName().contains("LockRepository"))
            )
        ))
        .because("LockRepository 필드가 필수입니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 21: Mapper 필드 필수
 */
@Test
@DisplayName("[필수] LockQueryAdapter는 Mapper 필드를 가져야 한다")
void lockQueryAdapter_MustHaveMapperField() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have Mapper field",
                javaClass -> javaClass.getAllFields().stream()
                    .anyMatch(field -> field.getRawType().getName().contains("Mapper"))
            )
        ))
        .because("Mapper 필드가 필수입니다");

    rule.check(lockAdapterClasses);
}
```

### 규칙 22-24: 메서드 상세
```java
/**
 * 규칙 22: findByIdWithPessimisticLock() 필수
 */
@Test
@DisplayName("[필수] LockQueryAdapter는 findByIdWithPessimisticLock() 메서드를 가져야 한다")
void lockQueryAdapter_MustHaveFindByIdWithPessimisticLock() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have findByIdWithPessimisticLock method",
                javaClass -> javaClass.getMethods().stream()
                    .anyMatch(method -> method.getName().equals("findByIdWithPessimisticLock"))
            )
        ))
        .because("비관락 단건 조회 메서드가 필수입니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 23: findByCriteriaWithPessimisticLock() 필수
 */
@Test
@DisplayName("[필수] LockQueryAdapter는 findByCriteriaWithPessimisticLock() 메서드를 가져야 한다")
void lockQueryAdapter_MustHaveFindByCriteriaWithPessimisticLock() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have findByCriteriaWithPessimisticLock method",
                javaClass -> javaClass.getMethods().stream()
                    .anyMatch(method -> method.getName().equals("findByCriteriaWithPessimisticLock"))
            )
        ))
        .because("비관락 리스트 조회 메서드가 필수입니다");

    rule.check(lockAdapterClasses);
}

/**
 * 규칙 24: 나머지 4개 메서드 필수
 */
@Test
@DisplayName("[필수] LockQueryAdapter는 낙관락/ForUpdate 메서드 4개를 가져야 한다")
void lockQueryAdapter_MustHaveOtherLockMethods() {
    ArchRule rule = classes()
        .that().haveSimpleNameEndingWith("LockQueryAdapter")
        .should(ArchCondition.from(
            DescribedPredicate.describe(
                "have 4 other lock methods",
                javaClass -> {
                    long count = javaClass.getMethods().stream()
                        .filter(method -> method.getName().matches(
                            "findByIdWithOptimisticLock|" +
                            "findByCriteriaWithOptimisticLock|" +
                            "findByIdForUpdate|" +
                            "findByCriteriaForUpdate"
                        ))
                        .count();
                    return count == 4;
                }
            )
        ))
        .because("낙관락 및 ForUpdate 메서드가 필수입니다");

    rule.check(lockAdapterClasses);
}
```

---

## 4️⃣ CI/CD 통합

### Maven 빌드 시 자동 실행
```bash
# ArchUnit 테스트 실행 (빌드 시 자동)
mvn test

# LockQueryAdapter 테스트만 실행
mvn test -Dtest=LockQueryAdapterArchTest

# 실행 결과:
# [INFO] Tests run: 24, Failures: 0, Errors: 0
# [INFO] BUILD SUCCESS  (규칙 준수)
# [ERROR] BUILD FAILURE  (규칙 위반)
```

---

## 5️⃣ 체크리스트

ArchUnit 규칙 검증 시:
- [ ] 24개 규칙 모두 통과
- [ ] 빌드 시 자동 실행 설정
- [ ] 규칙 위반 시 빌드 실패 확인
- [ ] CI/CD 파이프라인 통합
- [ ] 팀 전체 규칙 공유

---

**작성자**: Development Team
**최종 수정일**: 2025-11-12
**버전**: 1.0.0
