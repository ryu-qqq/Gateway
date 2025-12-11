# Security ArchUnit — **보안 컴포넌트 아키텍처 검증**

> **목적**: Security 관련 클래스의 아키텍처 규칙을 ArchUnit으로 자동 검증
>
> **Zero-Tolerance**: 보안 아키텍처 위반 시 빌드 실패

---

## 1️⃣ 검증 대상 컴포넌트

| 컴포넌트 | 패키지 위치 | 검증 항목 |
|----------|-------------|-----------|
| **ApiPaths** | `auth/paths/` | final 클래스, private 생성자, static final 필드 |
| **SecurityPaths** | `auth/paths/` | final 클래스, private 생성자, static final 배열 |
| **Filter** | `auth/filter/` | OncePerRequestFilter 상속, 네이밍 |
| **Handler** | `auth/handler/` | Spring Security 인터페이스 구현 |
| **Component** | `auth/component/` | @Component 어노테이션, SRP |
| **Config** | `auth/config/` | @Configuration 어노테이션 |

---

## 2️⃣ ArchUnit 테스트 코드

### SecurityArchTest.java

```java
package com.company.adapter.in.rest.architecture.security;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * Security 컴포넌트 ArchUnit 테스트
 *
 * <p>보안 관련 클래스의 아키텍처 규칙을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("Security ArchUnit 테스트")
class SecurityArchTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.company.adapter.in.rest.auth");
    }

    // ========================================
    // API Paths 규칙
    // ========================================

    @Nested
    @DisplayName("ApiPaths 규칙")
    class ApiPathsRules {

        @Test
        @DisplayName("ApiPaths 클래스는 final이어야 한다")
        void apiPaths_should_be_final() {
            classes()
                .that().haveSimpleName("ApiPaths")
                .should().haveModifier(JavaModifier.FINAL)
                .as("ApiPaths 클래스는 상속을 방지하기 위해 final이어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("ApiPaths 클래스는 private 생성자만 가져야 한다")
        void apiPaths_should_have_private_constructor() {
            classes()
                .that().haveSimpleName("ApiPaths")
                .should(haveOnlyPrivateConstructors())
                .as("ApiPaths 클래스는 인스턴스화를 방지하기 위해 private 생성자만 가져야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("ApiPaths의 모든 필드는 public static final이어야 한다")
        void apiPaths_fields_should_be_public_static_final() {
            fields()
                .that().areDeclaredInClassesThat().haveSimpleName("ApiPaths")
                .and().areNotDeclaredInClassesThat().areInnerClasses()
                .should().bePublic()
                .andShould().beStatic()
                .andShould().beFinal()
                .as("ApiPaths의 모든 필드는 public static final이어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("ApiPaths의 경로 필드는 String 타입이어야 한다")
        void apiPaths_path_fields_should_be_string() {
            fields()
                .that().areDeclaredInClassesThat().haveSimpleName("ApiPaths")
                .and().arePublic()
                .should().haveRawType(String.class)
                .as("ApiPaths의 경로 필드는 String 타입이어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("ApiPaths의 Nested 클래스도 final이어야 한다")
        void apiPaths_nested_classes_should_be_final() {
            classes()
                .that().areInnerClasses()
                .and().resideInAnyPackage("..auth.paths..")
                .should().haveModifier(JavaModifier.FINAL)
                .as("ApiPaths의 Nested 클래스도 final이어야 합니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // SecurityPaths 규칙
    // ========================================

    @Nested
    @DisplayName("SecurityPaths 규칙")
    class SecurityPathsRules {

        @Test
        @DisplayName("SecurityPaths 클래스는 final이어야 한다")
        void securityPaths_should_be_final() {
            classes()
                .that().haveSimpleName("SecurityPaths")
                .should().haveModifier(JavaModifier.FINAL)
                .as("SecurityPaths 클래스는 final이어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("SecurityPaths 클래스는 private 생성자만 가져야 한다")
        void securityPaths_should_have_private_constructor() {
            classes()
                .that().haveSimpleName("SecurityPaths")
                .should(haveOnlyPrivateConstructors())
                .as("SecurityPaths 클래스는 private 생성자만 가져야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("SecurityPaths의 배열 필드는 public static final이어야 한다")
        void securityPaths_arrays_should_be_public_static_final() {
            fields()
                .that().areDeclaredInClassesThat().haveSimpleName("SecurityPaths")
                .should().bePublic()
                .andShould().beStatic()
                .andShould().beFinal()
                .as("SecurityPaths의 배열 필드는 public static final이어야 합니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // Filter 규칙
    // ========================================

    @Nested
    @DisplayName("Filter 규칙")
    class FilterRules {

        @Test
        @DisplayName("Filter 클래스는 OncePerRequestFilter를 상속해야 한다")
        void filters_should_extend_OncePerRequestFilter() {
            classes()
                .that().resideInAPackage("..auth.filter..")
                .and().haveSimpleNameEndingWith("Filter")
                .should().beAssignableTo(OncePerRequestFilter.class)
                .as("Filter 클래스는 OncePerRequestFilter를 상속해야 합니다 (요청당 1회 실행 보장)")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Filter 클래스는 'Filter'로 끝나야 한다")
        void filters_should_have_filter_suffix() {
            classes()
                .that().resideInAPackage("..auth.filter..")
                .and().areAssignableTo(OncePerRequestFilter.class)
                .should().haveSimpleNameEndingWith("Filter")
                .as("Filter 클래스는 'Filter'로 끝나야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("JwtAuthenticationFilter는 doFilterInternal을 오버라이드해야 한다")
        void jwtFilter_should_override_doFilterInternal() {
            classes()
                .that().haveSimpleName("JwtAuthenticationFilter")
                .should(overrideDoFilterInternal())
                .as("JwtAuthenticationFilter는 doFilterInternal을 오버라이드해야 합니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // Handler 규칙
    // ========================================

    @Nested
    @DisplayName("Handler 규칙")
    class HandlerRules {

        @Test
        @DisplayName("AuthenticationErrorHandler는 AuthenticationEntryPoint를 구현해야 한다")
        void authErrorHandler_should_implement_AuthenticationEntryPoint() {
            classes()
                .that().haveSimpleName("AuthenticationErrorHandler")
                .should().implement(AuthenticationEntryPoint.class)
                .as("AuthenticationErrorHandler는 AuthenticationEntryPoint를 구현해야 합니다 (401 처리)")
                .check(importedClasses);
        }

        @Test
        @DisplayName("AuthenticationErrorHandler는 AccessDeniedHandler를 구현해야 한다")
        void authErrorHandler_should_implement_AccessDeniedHandler() {
            classes()
                .that().haveSimpleName("AuthenticationErrorHandler")
                .should().implement(AccessDeniedHandler.class)
                .as("AuthenticationErrorHandler는 AccessDeniedHandler를 구현해야 합니다 (403 처리)")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Handler 클래스는 @Component 어노테이션이 있어야 한다")
        void handlers_should_have_component_annotation() {
            classes()
                .that().resideInAPackage("..auth.handler..")
                .and().haveSimpleNameEndingWith("Handler")
                .should().beAnnotatedWith(Component.class)
                .as("Handler 클래스는 @Component 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // Component 규칙
    // ========================================

    @Nested
    @DisplayName("Component 규칙")
    class ComponentRules {

        @Test
        @DisplayName("auth/component 패키지의 클래스는 @Component 어노테이션이 있어야 한다")
        void components_should_have_component_annotation() {
            classes()
                .that().resideInAPackage("..auth.component..")
                .should().beAnnotatedWith(Component.class)
                .as("auth/component 패키지의 클래스는 @Component 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("TokenCookieWriter는 @Component 어노테이션이 있어야 한다")
        void tokenCookieWriter_should_be_component() {
            classes()
                .that().haveSimpleName("TokenCookieWriter")
                .should().beAnnotatedWith(Component.class)
                .as("TokenCookieWriter는 @Component 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("MdcContextHolder는 @Component 어노테이션이 있어야 한다")
        void mdcContextHolder_should_be_component() {
            classes()
                .that().haveSimpleName("MdcContextHolder")
                .should().beAnnotatedWith(Component.class)
                .as("MdcContextHolder는 @Component 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // Config 규칙
    // ========================================

    @Nested
    @DisplayName("Config 규칙")
    class ConfigRules {

        @Test
        @DisplayName("SecurityConfig는 @Configuration 어노테이션이 있어야 한다")
        void securityConfig_should_have_configuration_annotation() {
            classes()
                .that().haveSimpleName("SecurityConfig")
                .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .as("SecurityConfig는 @Configuration 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("SecurityConfig는 @EnableWebSecurity 어노테이션이 있어야 한다")
        void securityConfig_should_have_enableWebSecurity_annotation() {
            classes()
                .that().haveSimpleName("SecurityConfig")
                .should().beAnnotatedWith(
                    org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class)
                .as("SecurityConfig는 @EnableWebSecurity 어노테이션이 있어야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("SecurityConfig는 @EnableMethodSecurity 어노테이션이 있어야 한다")
        void securityConfig_should_have_enableMethodSecurity_annotation() {
            classes()
                .that().haveSimpleName("SecurityConfig")
                .should().beAnnotatedWith(
                    org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class)
                .as("SecurityConfig는 @EnableMethodSecurity 어노테이션이 있어야 합니다 (Method Security 활성화)")
                .check(importedClasses);
        }
    }

    // ========================================
    // 의존성 규칙
    // ========================================

    @Nested
    @DisplayName("의존성 규칙")
    class DependencyRules {

        @Test
        @DisplayName("auth/paths 패키지는 다른 auth 패키지에 의존하지 않아야 한다")
        void paths_should_not_depend_on_other_auth_packages() {
            classes()
                .that().resideInAPackage("..auth.paths..")
                .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "..auth.paths.."
                    )
                .as("auth/paths 패키지는 순수 상수 클래스여야 합니다")
                .check(importedClasses);
        }

        @Test
        @DisplayName("Filter는 Application Port에 의존할 수 있다")
        void filters_may_depend_on_application_ports() {
            // Filter → Application Port 의존 허용 (TokenProviderPort 등)
            classes()
                .that().resideInAPackage("..auth.filter..")
                .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                        "java..",
                        "jakarta..",
                        "org.springframework..",
                        "org.slf4j..",
                        "..auth..",
                        "..application..port.."
                    )
                .as("Filter는 Application Port에 의존할 수 있습니다")
                .check(importedClasses);
        }
    }

    // ========================================
    // Custom Conditions
    // ========================================

    /**
     * private 생성자만 있는지 검증
     */
    private static ArchCondition<JavaClass> haveOnlyPrivateConstructors() {
        return new ArchCondition<>("have only private constructors") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.getConstructors().forEach(constructor -> {
                    if (!constructor.getModifiers().contains(JavaModifier.PRIVATE)) {
                        events.add(SimpleConditionEvent.violated(
                            constructor,
                            String.format("%s has non-private constructor", javaClass.getName())
                        ));
                    }
                });
            }
        };
    }

    /**
     * doFilterInternal 메서드 오버라이드 검증
     */
    private static ArchCondition<JavaClass> overrideDoFilterInternal() {
        return new ArchCondition<>("override doFilterInternal method") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasMethod = javaClass.getMethods().stream()
                    .anyMatch(method -> method.getName().equals("doFilterInternal"));

                if (!hasMethod) {
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        String.format("%s does not override doFilterInternal", javaClass.getName())
                    ));
                }
            }
        };
    }
}
```

---

## 3️⃣ 검증 규칙 요약

### ApiPaths 규칙 (6개)

| 규칙 | 설명 | 이유 |
|------|------|------|
| final 클래스 | 상속 방지 | 유틸리티 클래스 패턴 |
| private 생성자 | 인스턴스화 방지 | 상수 클래스 |
| public static final 필드 | 상수 정의 | 컴파일 타임 상수 |
| String 타입 필드 | 경로 타입 | 타입 안전성 |
| Nested 클래스 final | BC별 그룹화 | 구조적 일관성 |
| 외부 의존 금지 | 순수 상수 | 의존성 분리 |

### Filter 규칙 (3개)

| 규칙 | 설명 | 이유 |
|------|------|------|
| OncePerRequestFilter 상속 | 요청당 1회 실행 | 중복 실행 방지 |
| Filter 접미사 | 네이밍 규칙 | 가독성 |
| doFilterInternal 오버라이드 | 필터 로직 | 구현 보장 |

### Handler 규칙 (3개)

| 규칙 | 설명 | 이유 |
|------|------|------|
| AuthenticationEntryPoint 구현 | 401 처리 | 인증 실패 |
| AccessDeniedHandler 구현 | 403 처리 | 인가 실패 |
| @Component 어노테이션 | Spring Bean | DI |

### Config 규칙 (3개)

| 규칙 | 설명 | 이유 |
|------|------|------|
| @Configuration | 설정 클래스 | Spring Config |
| @EnableWebSecurity | Security 활성화 | 필수 |
| @EnableMethodSecurity | Method Security | @PreAuthorize |

---

## 4️⃣ 실행 방법

### Gradle 실행

```bash
# Security ArchUnit 테스트만 실행
./gradlew test --tests "*SecurityArchTest*"

# 전체 ArchUnit 테스트 실행
./gradlew test --tests "*ArchTest*"
```

### 빌드 시 자동 실행

```groovy
// build.gradle
test {
    useJUnitPlatform()
    // ArchUnit 테스트 포함
}
```

---

## 5️⃣ 체크리스트

- [ ] `SecurityArchTest.java` 파일 생성
- [ ] ApiPaths 규칙 6개 검증
- [ ] SecurityPaths 규칙 3개 검증
- [ ] Filter 규칙 3개 검증
- [ ] Handler 규칙 3개 검증
- [ ] Component 규칙 3개 검증
- [ ] Config 규칙 3개 검증
- [ ] 의존성 규칙 2개 검증
- [ ] CI/CD에서 자동 실행 설정

---

## 📚 관련 가이드

- **[Security Guide](./security-guide.md)** - 전체 보안 아키텍처
- **[API Paths Guide](./api-paths-guide.md)** - Constants 방식 경로 관리
- **[Security Test Guide](./security-test-guide.md)** - 테스트 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
