# Multi-Module Structure — **헥사고날 멀티모듈 구조**

> **목적**: Spring Boot 3.5.x + Java 21 헥사고날 아키텍처 멀티모듈 구조 및 의존성 규칙

---

## 1️⃣ 전체 모듈 구조

### 프로덕션 모듈

```
project/
├── domain/                         ⭐ 도메인 레이어 (핵심 비즈니스 로직)
│   └── src/main/java/
│       └── com/{owner}/domain/
│           ├── order/
│           ├── product/
│           └── customer/
│
├── application/                    ⭐ 애플리케이션 레이어 (UseCase)
│   └── src/main/java/
│       └── com/{owner}/application/
│           ├── order/
│           ├── product/
│           └── customer/
│
├── adapter-in/                     ⭐ Inbound Adapters
│   └── rest-api/                   ⭐ REST API Adapter
│       └── src/main/java/
│           └── com/{owner}/adapter/in/rest/
│               ├── order/
│               ├── product/
│               └── customer/
│
├── adapter-out/                    ⭐ Outbound Adapters
│   └── persistence-mysql/          ⭐ MySQL Persistence Adapter
│       └── src/main/java/
│           └── com/{owner}/adapter/out/persistence/
│               ├── order/
│               ├── product/
│               └── customer/
│
└── bootstrap/                      ⭐ Spring Boot Application (진입점)
    └── src/main/java/
        └── com/{owner}/BootstrapApplication.java
```

### 테스트 Fixtures 모듈

```
project/
├── domain-test-fixtures/                       ⭐ Domain 객체 Fixture
│   └── src/main/java/
│       └── com/{owner}/fixture/domain/
│
├── application-test-fixtures/                  ⭐ Application DTO Fixture
│   └── src/main/java/
│       └── com/{owner}/fixture/application/
│
├── adapter-in/
│   └── rest-api-test-fixtures/                 ⭐ Optional (REST Request Fixture)
│       └── src/main/java/
│           └── com/{owner}/fixture/adapter/rest/
│
└── adapter-out/
    └── persistence-mysql-test-fixtures/        ⭐ Optional (Entity Fixture)
        └── src/main/java/
            └── com/{owner}/fixture/adapter/persistence/
```

---

## 2️⃣ 의존성 흐름 (Dependency Flow)

### 프로덕션 모듈 의존성

```
┌─────────────────────────────────────────┐
│ bootstrap (Spring Boot Application)    │
│ - adapter-in/rest-api                   │
│ - adapter-out/persistence-mysql         │
│ - application                           │
│ - domain                                │
└─────────────────────────────────────────┘
             ↓ implementation
┌─────────────────────────────────────────┐
│ adapter-in/rest-api (REST API)          │
│ - application (Port-In)                 │
│ - domain (DTO 변환)                     │
└─────────────────────────────────────────┘
             ↓ implementation
┌─────────────────────────────────────────┐
│ application (UseCase)                   │
│ - domain                                │
└─────────────────────────────────────────┘
             ↓ implementation
┌─────────────────────────────────────────┐
│ domain (Domain Objects)                 │
│ - (No dependencies)                     │
└─────────────────────────────────────────┘
             ↑ implementation
┌─────────────────────────────────────────┐
│ adapter-out/persistence-mysql (MySQL)   │
│ - application (Port-Out)                │
│ - domain (Entity → Domain 변환)        │
└─────────────────────────────────────────┘
```

### Test Fixtures 의존성

```
domain-test-fixtures
    ↓ api
  domain
```

```
application-test-fixtures
    ↓ api                    ↓ api
  application         domain-test-fixtures
```

```
adapter-*-test-fixtures
    ↓ api                    ↓ api
  adapter-*          application-test-fixtures
```

---

## 3️⃣ settings.gradle 설정

```gradle
rootProject.name = 'spring-standards'

// ============================================================
// Production Modules
// ============================================================
include 'domain'
include 'application'

// Adapter-In Modules
include 'adapter-in:rest-api'
project(':adapter-in:rest-api').name = 'rest-api'

// Adapter-Out Modules
include 'adapter-out:persistence-mysql'
project(':adapter-out:persistence-mysql').name = 'persistence-mysql'

// Bootstrap
include 'bootstrap'

// ============================================================
// Test Fixtures Modules
// ============================================================
include 'domain-test-fixtures'
include 'application-test-fixtures'

// Adapter-In Test Fixtures
include 'adapter-in:rest-api-test-fixtures'
project(':adapter-in:rest-api-test-fixtures').name = 'rest-api-test-fixtures'

// Adapter-Out Test Fixtures
include 'adapter-out:persistence-mysql-test-fixtures'
project(':adapter-out:persistence-mysql-test-fixtures').name = 'persistence-mysql-test-fixtures'
```

---

## 4️⃣ 버전 관리 (gradle.properties 필수)

### ⚠️ 버전 관리 규칙 (Zero-Tolerance)

**❌ 금지**: build.gradle에 직접 버전 하드코딩
**✅ 필수**: gradle.properties에 버전 명시 → build.gradle에서 참조

### gradle.properties (루트)

```properties
# ============================================================
# Java Version
# ============================================================
javaVersion=21

# ============================================================
# Plugin Versions
# ============================================================
springBootVersion=3.5.0
springDependencyManagementVersion=1.1.4

# ============================================================
# Library Versions
# ============================================================
querydslVersion=5.0.0
archunitVersion=1.1.0
commonsCollections4Version=4.4
guavaVersion=32.1.3-jre
```

---

## 5️⃣ 모듈별 build.gradle

### domain/build.gradle

```gradle
plugins {
    id 'java-library'
}

dependencies {
    // ✅ Domain은 외부 의존성 없음 (Pure Java)
    // Lombok 금지 (Zero-Tolerance)
}

java {
    // ✅ gradle.properties에서 버전 참조 (하드코딩 금지)
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

### application/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version "$springBootVersion" apply false
    id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
}

dependencies {
    // ✅ Domain 의존
    implementation project(':domain')

    // Spring Boot (Starter 제외, @Transactional 등 어노테이션만)
    implementation 'org.springframework:spring-context'
    implementation 'org.springframework:spring-tx'

    // Test Fixtures
    testImplementation project(':domain-test-fixtures')
    testImplementation project(':application-test-fixtures')

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mockito:mockito-junit-jupiter'
}

dependencyManagement {
    imports {
        mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    }
}

java {
    // ✅ gradle.properties에서 버전 참조
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

### adapter-in/rest-api/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version "$springBootVersion" apply false
    id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
}

dependencies {
    // ✅ Application, Domain 의존
    implementation project(':application')
    implementation project(':domain')

    // Spring Boot Web
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Test Fixtures
    testImplementation project(':domain-test-fixtures')
    testImplementation project(':application-test-fixtures')
    testImplementation project(':adapter-in:rest-api-test-fixtures')

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
}

dependencyManagement {
    imports {
        mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    }
}

java {
    // ✅ gradle.properties에서 버전 참조
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

### adapter-out/persistence-mysql/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version "$springBootVersion" apply false
    id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
}

dependencies {
    // ✅ Application, Domain 의존
    implementation project(':application')
    implementation project(':domain')

    // Spring Boot Data JPA
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // QueryDSL (✅ gradle.properties 버전 참조)
    implementation "com.querydsl:querydsl-jpa:${querydslVersion}:jakarta"
    annotationProcessor "com.querydsl:querydsl-apt:${querydslVersion}:jakarta"
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'
    runtimeOnly 'com.h2database:h2'

    // Test Fixtures
    testImplementation project(':domain-test-fixtures')
    testImplementation project(':application-test-fixtures')
    testImplementation project(':adapter-out:persistence-mysql-test-fixtures')

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:testcontainers'
    testImplementation 'org.testcontainers:mysql'
}

dependencyManagement {
    imports {
        mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    }
}

java {
    // ✅ gradle.properties에서 버전 참조
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

### bootstrap/build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version "$springBootVersion"
    id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
}

dependencies {
    // ✅ 모든 모듈 의존
    implementation project(':domain')
    implementation project(':application')
    implementation project(':adapter-in:rest-api')
    implementation project(':adapter-out:persistence-mysql')

    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter'

    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

java {
    // ✅ gradle.properties에서 버전 참조
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

---

## 6️⃣ 의존성 규칙 매트릭스

### 프로덕션 모듈 의존성

| From ↓ / To → | domain | application | adapter-in/rest-api | adapter-out/persistence-mysql | bootstrap |
|---------------|--------|-------------|---------------------|-------------------------------|-----------|
| **domain** | - | ❌ | ❌ | ❌ | ❌ |
| **application** | ✅ | - | ❌ | ❌ | ❌ |
| **adapter-in/rest-api** | ✅ | ✅ | - | ❌ | ❌ |
| **adapter-out/persistence-mysql** | ✅ | ✅ | ❌ | - | ❌ |
| **bootstrap** | ✅ | ✅ | ✅ | ✅ | - |

### Test Fixtures 의존성

| From ↓ / To → | domain-tf | application-tf | adapter-*-tf |
|---------------|-----------|----------------|--------------|
| **domain-tf** | - | ❌ | ❌ |
| **application-tf** | ✅ | - | ❌ |
| **adapter-*-tf** | ✅ | ✅ | - |

---

## 7️⃣ ArchUnit 검증

### 멀티모듈 의존성 검증

**위치**: `bootstrap/src/test/java/architecture/ModuleDependencyArchTest.java`

```java
package com.{owner}.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.*;

/**
 * Multi-Module 의존성 규칙 ArchUnit 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@Tag("architecture")
@DisplayName("Multi-Module Dependency ArchUnit Tests")
class ModuleDependencyArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .importPackages("com.{owner}");
    }

    /**
     * 규칙 1: domain은 외부 의존성 없음
     */
    @Test
    @DisplayName("[필수] domain은 외부 모듈을 의존할 수 없다")
    void domain_MustNotDependOnAnyOtherModule() {
        ArchRule rule = slices()
            .matching("com.{owner}.domain.(*)..")
            .should().notDependOnEachOther()
            .because("domain은 Pure Java로 작성되어야 하며 외부 의존성이 없어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: application은 domain만 의존
     */
    @Test
    @DisplayName("[필수] application은 domain만 의존해야 한다")
    void application_ShouldOnlyDependOnDomain() {
        ArchRule rule = slices()
            .matching("com.{owner}.application.(*)..")
            .should().onlyDependOn("com.{owner}.domain..", "java..", "org.springframework..")
            .because("application은 domain만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: adapter는 application + domain 의존
     */
    @Test
    @DisplayName("[필수] adapter는 application과 domain만 의존해야 한다")
    void adapter_ShouldOnlyDependOnApplicationAndDomain() {
        ArchRule rule = slices()
            .matching("com.{owner}.adapter.(*)..")
            .should().onlyDependOn(
                "com.{owner}.application..",
                "com.{owner}.domain..",
                "java..",
                "org.springframework..",
                "jakarta.."
            )
            .because("adapter는 application과 domain만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: adapter-in과 adapter-out은 서로 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-in과 adapter-out은 서로 의존할 수 없다")
    void adapterIn_MustNotDependOnAdapterOut() {
        ArchRule rule = slices()
            .matching("com.{owner}.adapter.in.(*)..")
            .should().notDependOn("com.{owner}.adapter.out..")
            .because("adapter-in과 adapter-out은 서로 의존할 수 없습니다");

        rule.check(classes);
    }
}
```

---

## 8️⃣ 빌드 및 테스트

### 전체 빌드

```bash
# 루트에서 전체 빌드
./gradlew clean build

# 특정 모듈만 빌드
./gradlew :domain:build
./gradlew :application:build
./gradlew :adapter-in:rest-api:build
./gradlew :adapter-out:persistence-mysql:build
```

### 모듈 의존성 확인

```bash
# 모듈 의존성 트리 확인
./gradlew :application:dependencies
./gradlew :adapter-in:rest-api:dependencies

# ArchUnit 검증
./gradlew test --tests "*ArchTest"
```

---

## 9️⃣ 체크리스트

멀티모듈 구조 설정 시:
- [ ] **gradle.properties에 버전 명시** (하드코딩 금지)
- [ ] settings.gradle에 모든 모듈 등록
- [ ] 각 모듈별 build.gradle 작성 (버전은 gradle.properties 참조)
- [ ] 의존성 흐름 준수 (domain → application → adapter)
- [ ] Test Fixtures 모듈 추가
- [ ] ArchUnit 검증 테스트 작성
- [ ] 빌드 및 테스트 통과 확인
- [ ] CI/CD 파이프라인 통합

---

## 📖 관련 문서

- **[Gradle Configuration](./gradle-configuration.md)** - Gradle 설정 상세
- **[Version Management](./version-management.md)** - Java, Spring Boot, 라이브러리 버전
- **[Test Fixtures Guide](../05-testing/test-fixtures/01_test-fixtures-guide.md)** - 테스트 픽스쳐 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
