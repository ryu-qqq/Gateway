# 프로젝트 커스터마이징 가이드

이 문서는 Spring Standards 템플릿 프로젝트를 새로운 프로젝트에 적용할 때 필요한 설정 변경 사항을 설명합니다.

---

## 목차

1. [개요](#개요)
2. [필수 변경 사항](#필수-변경-사항)
3. [패키지 경로 변경](#패키지-경로-변경)
4. [ArchUnit 테스트 설정](#archunit-테스트-설정)
5. [Gradle 설정](#gradle-설정)
6. [체크리스트](#체크리스트)

---

## 개요

템플릿 프로젝트의 기본 패키지는 `com.ryuqq`입니다. 새로운 프로젝트에 적용할 때는 이 패키지 경로를 프로젝트에 맞게 변경해야 합니다.

### 변경 예시

| 항목 | 템플릿 기본값 | 변경 예시 |
|------|--------------|----------|
| 기본 패키지 | `com.ryuqq` | `com.mycompany.ecommerce` |
| REST API Adapter | `com.ryuqq.adapter.in.rest` | `com.mycompany.ecommerce.adapter.in.rest` |
| Domain Layer | `com.ryuqq.domain` | `com.mycompany.ecommerce.domain` |
| Application Layer | `com.ryuqq.application` | `com.mycompany.ecommerce.application` |

---

## 필수 변경 사항

### 1. ArchUnit 패키지 상수 변경 (가장 중요!)

모든 ArchUnit 테스트는 중앙 집중식 패키지 상수를 사용합니다. **단 한 곳만 변경**하면 모든 테스트가 자동으로 적용됩니다.

#### 변경 파일

```
adapter-in/rest-api/src/test/java/com/ryuqq/adapter/in/rest/architecture/ArchUnitPackageConstants.java
```

#### 변경 내용

```java
public final class ArchUnitPackageConstants {

    // ========================================================================
    // 🔧 프로젝트 적용 시 이 값만 변경하세요
    // ========================================================================

    /**
     * 프로젝트 기본 패키지 (루트 패키지)
     *
     * 다른 프로젝트에 적용할 때 이 값을 해당 프로젝트의 기본 패키지로 변경합니다.
     *
     * 예시:
     * - 기본값: "com.ryuqq"
     * - E커머스: "com.acme.ecommerce"
     * - 결제 시스템: "com.acme.payment"
     */
    public static final String BASE_PACKAGE = "com.ryuqq";  // ← 이 값만 변경!

    // 아래 상수들은 자동으로 파생되므로 수정 불필요
    public static final String ADAPTER_IN_REST = BASE_PACKAGE + ".adapter.in.rest";
    public static final String DOMAIN = BASE_PACKAGE + ".domain";
    public static final String APPLICATION = BASE_PACKAGE + ".application";
    // ...
}
```

#### 변경 예시

```java
// 기본값
public static final String BASE_PACKAGE = "com.ryuqq";

// 변경 후 (예: E커머스 프로젝트)
public static final String BASE_PACKAGE = "com.acme.ecommerce";
```

---

## 패키지 경로 변경

### 1. 소스 코드 디렉토리 구조 변경

```bash
# 템플릿 구조
adapter-in/rest-api/src/main/java/com/ryuqq/adapter/in/rest/
domain/src/main/java/com/ryuqq/domain/
application/src/main/java/com/ryuqq/application/

# 변경 후 (예: com.acme.ecommerce)
adapter-in/rest-api/src/main/java/com/acme/ecommerce/adapter/in/rest/
domain/src/main/java/com/acme/ecommerce/domain/
application/src/main/java/com/acme/ecommerce/application/
```

### 2. 패키지 선언 변경

각 Java 파일의 package 선언을 변경합니다.

```java
// 변경 전
package com.ryuqq.adapter.in.rest.common.controller;

// 변경 후
package com.acme.ecommerce.adapter.in.rest.common.controller;
```

### 3. IDE 리팩토링 활용

IntelliJ IDEA에서 패키지 리팩토링을 사용하면 자동으로 변경됩니다:
1. `com.ryuqq` 패키지 우클릭
2. `Refactor` → `Rename` 선택
3. 새 패키지명 입력 (예: `com.acme.ecommerce`)

---

## ArchUnit 테스트 설정

### 상수 파일 위치

각 모듈별로 ArchUnit 상수 클래스가 있습니다:

| 모듈 | 상수 클래스 위치 |
|------|-----------------|
| REST API Adapter | `adapter-in/rest-api/.../architecture/ArchUnitPackageConstants.java` |
| Domain | `domain/.../architecture/DomainArchUnitPackageConstants.java` |
| Application | `application/.../architecture/ApplicationArchUnitPackageConstants.java` |
| Persistence | `adapter-out/persistence/.../architecture/PersistenceArchUnitPackageConstants.java` |

### 적용 규칙

1. 각 모듈의 `BASE_PACKAGE` 상수만 변경
2. 나머지 상수는 자동 파생됨
3. 테스트 실행으로 검증

---

## Gradle 설정

### settings.gradle 변경

```groovy
// 변경 전
rootProject.name = 'claude-spring-standards'

// 변경 후
rootProject.name = 'your-project-name'
```

### build.gradle 변경

```groovy
// 변경 전
group = 'com.ryuqq'

// 변경 후
group = 'com.acme.ecommerce'
```

---

## 체크리스트

프로젝트 적용 시 아래 항목을 순서대로 확인하세요:

### 필수 변경

- [ ] `ArchUnitPackageConstants.java`의 `BASE_PACKAGE` 변경
- [ ] `settings.gradle`의 `rootProject.name` 변경
- [ ] `build.gradle`의 `group` 변경
- [ ] 소스 코드 패키지 경로 변경 (IDE 리팩토링 사용)
- [ ] 테스트 코드 패키지 경로 변경

### 검증

- [ ] `./gradlew compileJava` 성공
- [ ] `./gradlew compileTestJava` 성공
- [ ] `./gradlew test --tests "*ArchTest*"` 성공
- [ ] `./gradlew test` 전체 테스트 성공

### 선택 변경

- [ ] README.md 프로젝트 설명 업데이트
- [ ] CLAUDE.md 프로젝트 정보 업데이트
- [ ] CI/CD 설정 업데이트 (GitHub Actions, etc.)
- [ ] 문서 내 예시 코드 패키지 경로 변경

---

## 참고 문서

- [멀티 모듈 구조](multi-module-structure.md)
- [Gradle 설정 가이드](gradle-configuration.md)
- [버전 관리](version-management.md)
