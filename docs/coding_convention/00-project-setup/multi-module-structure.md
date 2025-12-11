# Multi-Module Structure — **헥사고날 멀티모듈 구조**

> **목적**: Spring Boot 3.5.x + Java 21 헥사고날 아키텍처 멀티모듈 구조 및 의존성 규칙

---

## 1️⃣ 전체 모듈 구조

### 프로덕션 모듈 (6개)

```
project/
│
├── domain/                              ⭐ 도메인 레이어 (핵심 비즈니스 로직)
│   ├── src/main/java/
│   │   └── com/company/template/domain/
│   │       ├── common/                  # 공통 인터페이스 (DomainEvent, DomainException)
│   │       └── {boundedContext}/        # 예: order, product, customer
│   │           ├── aggregate/           # Aggregate Root + Entity
│   │           ├── vo/                  # Value Object
│   │           ├── event/               # Domain Event
│   │           └── exception/           # BC 전용 예외
│   └── src/testFixtures/java/           # Test Fixtures (Gradle 플러그인)
│
├── application/                         ⭐ 애플리케이션 레이어 (UseCase)
│   ├── src/main/java/
│   │   └── com/company/template/application/
│   │       ├── common/                  # 공통 DTO, Config
│   │       └── {boundedContext}/
│   │           ├── assembler/           # Domain ↔ DTO 변환
│   │           ├── dto/                 # Command, Query, Response, Bundle
│   │           ├── facade/              # 여러 Manager 조합
│   │           ├── factory/             # Command/Query Factory
│   │           ├── manager/             # 단일 Port Transaction
│   │           ├── port/                # Port-In (UseCase), Port-Out (Persistence)
│   │           ├── service/             # UseCase 구현체
│   │           └── listener/            # Event Listener
│   └── src/testFixtures/java/
│
├── adapter-in/                          ⭐ Inbound Adapters (Driving)
│   └── rest-api/                        ⭐ REST API Adapter
│       ├── src/main/java/
│       │   └── com/company/template/adapter/in/rest/
│       │       ├── common/              # 공통 DTO, Error Handler
│       │       └── {boundedContext}/
│       │           ├── controller/      # HTTP Controller (CQRS 분리)
│       │           ├── dto/             # API Request/Response
│       │           ├── mapper/          # API ↔ UseCase DTO 변환
│       │           └── error/           # BC 전용 Error Mapper
│       └── src/testFixtures/java/       # (Optional)
│
├── adapter-out/                         ⭐ Outbound Adapters (Driven)
│   ├── persistence-mysql/               ⭐ MySQL Persistence Adapter
│   │   ├── src/main/java/
│   │   │   └── com/company/template/adapter/out/persistence/
│   │   │       ├── config/              # JPA, Flyway Config
│   │   │       ├── common/              # Base Entity
│   │   │       └── {boundedContext}/
│   │   │           ├── adapter/         # Command/Query Adapter
│   │   │           ├── entity/          # JPA Entity
│   │   │           ├── mapper/          # Entity ↔ Domain 변환
│   │   │           └── repository/      # JPA/QueryDSL Repository
│   │   └── src/testFixtures/java/       # (Optional)
│   │
│   └── persistence-redis/               ⭐ Redis Persistence Adapter
│       ├── src/main/java/
│       │   └── com/company/template/adapter/out/redis/
│       │       ├── config/              # Lettuce, Redisson Config
│       │       └── {boundedContext}/
│       │           └── adapter/         # Cache/Lock Adapter
│       └── src/testFixtures/java/       # (Optional)
│
└── bootstrap/                           ⭐ Bootstrap Modules (Application Entry Points)
    └── bootstrap-web-api/               ⭐ Spring Boot Application
        └── src/main/java/
            └── com/company/template/
                └── BootstrapWebApiApplication.java
```

### 모듈 역할 요약

| 모듈 | 역할 | 핵심 원칙 |
|------|------|----------|
| **domain** | 순수 비즈니스 로직 | Pure Java, Lombok 금지, Law of Demeter |
| **application** | UseCase + Transaction 관리 | CQRS 분리, Port/Adapter 패턴 |
| **adapter-in/rest-api** | HTTP 요청/응답 처리 | Thin Controller, Bean Validation |
| **adapter-out/persistence-mysql** | MySQL 저장/조회 | Long FK 전략, CQRS 분리 |
| **adapter-out/persistence-redis** | 캐싱 + 분산락 | Lettuce (캐싱), Redisson (분산락) |
| **bootstrap/bootstrap-web-api** | Spring Boot 진입점 | 모든 모듈 조립, Config |

---

## 2️⃣ 의존성 흐름 (Dependency Flow)

### 프로덕션 모듈 의존성

```
┌─────────────────────────────────────────────────────────────────────┐
│ bootstrap/bootstrap-web-api (Spring Boot Application)               │
│ ├── adapter-in/rest-api                                             │
│ ├── adapter-out/persistence-mysql                                   │
│ ├── adapter-out/persistence-redis                                   │
│ ├── application                                                     │
│ └── domain                                                          │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ implementation
┌─────────────────────────────────────────────────────────────────────┐
│ adapter-in/rest-api (REST API Adapter)                              │
│ ├── application (Port-In UseCase)                                   │
│ └── domain (DTO 변환용)                                             │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ implementation
┌─────────────────────────────────────────────────────────────────────┐
│ application (UseCase Layer)                                         │
│ └── domain                                                          │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ implementation
┌─────────────────────────────────────────────────────────────────────┐
│ domain (Domain Layer)                                               │
│ └── (No dependencies - Pure Java)                                   │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ implementation
┌─────────────────────────────────────────────────────────────────────┐
│ adapter-out/persistence-mysql (MySQL Adapter)                       │
│ ├── application (Port-Out 구현)                                     │
│ └── domain (Entity ↔ Domain 변환)                                   │
└─────────────────────────────────────────────────────────────────────┘
                              ↑ implementation
┌─────────────────────────────────────────────────────────────────────┐
│ adapter-out/persistence-redis (Redis Adapter)                       │
│ ├── application (Port-Out 구현)                                     │
│ └── domain (Cache DTO 변환)                                         │
└─────────────────────────────────────────────────────────────────────┘
```

### 의존성 규칙 매트릭스

| From ↓ / To → | domain | application | rest-api | persistence-mysql | persistence-redis | bootstrap |
|---------------|--------|-------------|----------|-------------------|-------------------|-----------|
| **domain** | - | ❌ | ❌ | ❌ | ❌ | ❌ |
| **application** | ✅ | - | ❌ | ❌ | ❌ | ❌ |
| **rest-api** | ✅ | ✅ | - | ❌ | ❌ | ❌ |
| **persistence-mysql** | ✅ | ✅ | ❌ | - | ❌ | ❌ |
| **persistence-redis** | ✅ | ✅ | ❌ | ❌ | - | ❌ |
| **bootstrap** | ✅ | ✅ | ✅ | ✅ | ✅ | - |

**핵심 규칙**:
- ✅ domain은 **외부 의존성 없음** (Pure Java)
- ✅ application은 **domain만 의존**
- ✅ adapter는 **application + domain만 의존**
- ❌ adapter-in과 adapter-out은 **서로 의존 금지**

---

## 3️⃣ settings.gradle 설정

```gradle
rootProject.name = 'spring-hexagonal-template'

// ========================================
// Core Modules (Hexagonal Architecture)
// ========================================
include 'domain'
include 'application'

// ========================================
// Adapter Modules (Ports & Adapters)
// ========================================
// Inbound Adapters (Driving)
include 'adapter-in:rest-api'

// Outbound Adapters (Driven)
include 'adapter-out:persistence-mysql'
include 'adapter-out:persistence-redis'

// ========================================
// Bootstrap Modules (Runnable Applications)
// ========================================
include 'bootstrap:bootstrap-web-api'

// ========================================
// Project Structure
// ========================================
project(':domain').projectDir = file('domain')
project(':application').projectDir = file('application')

project(':adapter-in:rest-api').projectDir = file('adapter-in/rest-api')
project(':adapter-out:persistence-mysql').projectDir = file('adapter-out/persistence-mysql')
project(':adapter-out:persistence-redis').projectDir = file('adapter-out/persistence-redis')

project(':bootstrap:bootstrap-web-api').projectDir = file('bootstrap/bootstrap-web-api')
```

---

## 4️⃣ 버전 관리 (Version Catalog)

### ⚠️ 버전 관리 규칙 (Zero-Tolerance)

**❌ 금지**: build.gradle에 직접 버전 하드코딩
**✅ 필수**: `gradle/libs.versions.toml`에 버전 명시 → `libs.xxx` 문법으로 참조

### libs.versions.toml 구조

```toml
# gradle/libs.versions.toml

[versions]
springBoot = "3.5.6"
springDependencyManagement = "1.1.5"
querydsl = "5.1.0"
archunit = "1.2.1"

[libraries]
# Spring Boot Starters (BOM 관리 - 버전 생략)
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }

# 외부 라이브러리 (version.ref 필수)
querydsl-jpa = { module = "com.querydsl:querydsl-jpa", version.ref = "querydsl" }
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "springDependencyManagement" }
```

### build.gradle에서 참조

```gradle
dependencies {
    // ✅ Version Catalog 참조
    implementation libs.spring.boot.starter.web
    implementation libs.querydsl.jpa
    testImplementation libs.archunit.junit5
}
```

> 📖 **상세 문서**: [Version Management](./version-management.md)

---

## 5️⃣ 모듈별 build.gradle

### domain/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'  // ⭐ testFixtures 활성화
}

dependencies {
    // ========================================
    // Domain은 외부 의존성 없음 (Pure Java)
    // ========================================
    // ❌ Lombok 금지 (Zero-Tolerance)
    // ❌ Spring 의존성 금지
    // ❌ JPA 의존성 금지

    // ✅ 허용: 순수 Java 유틸리티만
    implementation rootProject.libs.uuid.creator  // UUIDv7 생성

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.junit.jupiter
    testImplementation rootProject.libs.assertj.core
    testImplementation rootProject.libs.archunit.junit5
}
```

### application/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':domain')

    // Spring (Context, TX only - Starter 제외)
    implementation rootProject.libs.spring.context
    implementation rootProject.libs.spring.tx

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.spring.boot.starter.test
    testImplementation testFixtures(project(':domain'))

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))
}
```

### adapter-in/rest-api/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':application')
    api project(':domain')

    // Spring Boot Web
    implementation rootProject.libs.spring.boot.starter.web
    implementation rootProject.libs.spring.boot.starter.validation

    // OpenAPI/Swagger
    implementation rootProject.libs.springdoc.openapi.starter

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.spring.boot.starter.test
    testImplementation rootProject.libs.spring.restdocs.mockmvc
    testImplementation testFixtures(project(':domain'))
    testImplementation testFixtures(project(':application'))

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':application')
    testFixturesApi testFixtures(project(':application'))
}
```

### adapter-out/persistence-mysql/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':application')
    api project(':domain')

    // Spring Boot Data JPA
    implementation rootProject.libs.spring.boot.starter.data.jpa

    // QueryDSL
    implementation rootProject.libs.querydsl.jpa.jakarta
    annotationProcessor rootProject.libs.querydsl.apt.jakarta
    annotationProcessor rootProject.libs.jakarta.annotation.api
    annotationProcessor rootProject.libs.jakarta.persistence.api

    // Flyway
    implementation rootProject.libs.flyway.core
    implementation rootProject.libs.flyway.mysql

    // Database Drivers
    runtimeOnly rootProject.libs.mysql.connector

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.spring.boot.starter.test
    testImplementation rootProject.libs.testcontainers.mysql
    testImplementation rootProject.libs.testcontainers.junit
    testImplementation testFixtures(project(':domain'))

    testRuntimeOnly rootProject.libs.h2

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))
}
```

### adapter-out/persistence-redis/build.gradle

```gradle
plugins {
    id 'java-library'
    id 'java-test-fixtures'
}

dependencies {
    // ========================================
    // Core Dependencies
    // ========================================
    api project(':application')
    api project(':domain')

    // Lettuce (캐싱 - Spring Boot 기본)
    implementation rootProject.libs.spring.boot.starter.data.redis

    // Redisson (분산락)
    implementation rootProject.libs.redisson.spring.boot.starter

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.spring.boot.starter.test
    testImplementation rootProject.libs.testcontainers.junit
    testImplementation testFixtures(project(':domain'))

    // ========================================
    // Test Fixtures Dependencies
    // ========================================
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))
}
```

### bootstrap/bootstrap-web-api/build.gradle

```gradle
plugins {
    id 'java'
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // ========================================
    // All Modules
    // ========================================
    implementation project(':domain')
    implementation project(':application')
    implementation project(':adapter-in:rest-api')
    implementation project(':adapter-out:persistence-mysql')
    implementation project(':adapter-out:persistence-redis')

    // Spring Boot
    implementation rootProject.libs.spring.boot.starter

    // ========================================
    // Test Dependencies
    // ========================================
    testImplementation rootProject.libs.spring.boot.starter.test
    testImplementation testFixtures(project(':domain'))
    testImplementation testFixtures(project(':application'))
}

bootJar {
    enabled = true
}

jar {
    enabled = false
}
```

---

## 6️⃣ Test Fixtures (Gradle 플러그인)

### 핵심 원칙

Gradle `java-test-fixtures` 플러그인을 사용하여 **별도 모듈 없이** 테스트 픽스쳐를 공유합니다.

### 디렉토리 구조

```
domain/
├── src/main/java/                    (Production 코드)
├── src/test/java/                    (단위 테스트)
└── src/testFixtures/java/            ⭐ Test Fixtures
    └── com/company/template/fixture/domain/
        ├── OrderFixture.java
        ├── ProductFixture.java
        └── MoneyFixture.java
```

### 의존성 전파

```gradle
// application/build.gradle
dependencies {
    // Domain Fixture 사용
    testImplementation testFixtures(project(':domain'))

    // Application Fixture 전파
    testFixturesApi project(':domain')
    testFixturesApi testFixtures(project(':domain'))
}
```

### 의존성 흐름

```
domain testFixtures
    ↓ 의존
  domain (Production)

application testFixtures
    ↓ 의존              ↓ 의존
  application      domain testFixtures

adapter-* testFixtures
    ↓ 의존                    ↓ 의존
  adapter-*         application testFixtures (또는 domain testFixtures)
```

> 📖 **상세 문서**: [Test Fixtures Guide](../05-testing/test-fixtures/01_test-fixtures-guide.md)

---

## 7️⃣ ArchUnit 검증

### 멀티모듈 의존성 검증

**위치**: `bootstrap/bootstrap-web-api/src/test/java/.../ModuleDependencyArchTest.java`

```java
package com.company.template.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
            .importPackages("com.company.template");
    }

    /**
     * 규칙 1: domain은 외부 모듈 의존 금지
     */
    @Test
    @DisplayName("[필수] domain은 application, adapter를 의존할 수 없다")
    void domain_MustNotDependOnOtherModules() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application..",
                "..adapter.."
            )
            .because("domain은 Pure Java로 작성되어야 하며 외부 모듈 의존이 없어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 2: application은 adapter 의존 금지
     */
    @Test
    @DisplayName("[필수] application은 adapter를 의존할 수 없다")
    void application_MustNotDependOnAdapter() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("application은 domain만 의존해야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 3: adapter-in과 adapter-out은 서로 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-in과 adapter-out은 서로 의존할 수 없다")
    void adapterIn_MustNotDependOnAdapterOut() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("adapter-in과 adapter-out은 서로 독립적이어야 합니다");

        rule.check(classes);
    }

    /**
     * 규칙 4: adapter-out은 adapter-in 의존 금지
     */
    @Test
    @DisplayName("[금지] adapter-out은 adapter-in을 의존할 수 없다")
    void adapterOut_MustNotDependOnAdapterIn() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..")
            .because("adapter-out과 adapter-in은 서로 독립적이어야 합니다");

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

# Version Catalog 일관성 검증
./gradlew verifyVersionCatalog

# Lombok 금지 검증
./gradlew checkNoLombok
```

### 모듈별 빌드

```bash
# 특정 모듈만 빌드
./gradlew :domain:build
./gradlew :application:build
./gradlew :adapter-in:rest-api:build
./gradlew :adapter-out:persistence-mysql:build
./gradlew :adapter-out:persistence-redis:build
./gradlew :bootstrap:bootstrap-web-api:build
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# ArchUnit 테스트만
./gradlew test --tests "*ArchTest"

# 모듈별 테스트
./gradlew :domain:test
./gradlew :application:test
```

### 의존성 확인

```bash
# 모듈 의존성 트리 확인
./gradlew :application:dependencies
./gradlew :adapter-in:rest-api:dependencies
```

---

## 9️⃣ 체크리스트

멀티모듈 구조 설정 시:

### 프로젝트 초기화
- [ ] settings.gradle에 모든 모듈 등록
- [ ] `gradle/libs.versions.toml` 생성 (버전 하드코딩 금지)
- [ ] 각 모듈별 build.gradle 작성 (Version Catalog 참조)

### 의존성 규칙
- [ ] domain: 외부 의존성 없음 (Pure Java)
- [ ] application: domain만 의존
- [ ] adapter: application + domain만 의존
- [ ] adapter-in ↔ adapter-out 상호 의존 없음

### Test Fixtures
- [ ] `java-test-fixtures` 플러그인 적용
- [ ] `src/testFixtures/java/` 디렉토리 생성
- [ ] `testFixtures(project(':...'))` 문법으로 의존성 전파

### 검증
- [ ] ArchUnit 의존성 검증 테스트 작성
- [ ] `./gradlew verifyVersionCatalog` 통과
- [ ] `./gradlew checkNoLombok` 통과
- [ ] 전체 빌드 및 테스트 통과

---

## 📖 관련 문서

- **[Version Management](./version-management.md)** - Version Catalog 기반 의존성 관리
- **[Gradle Configuration](./gradle-configuration.md)** - QA 도구 및 플러그인 설정
- **[Test Fixtures Guide](../05-testing/test-fixtures/01_test-fixtures-guide.md)** - 테스트 픽스쳐 전략
- **[Domain Guide](../02-domain-layer/domain-guide.md)** - Domain Layer 가이드
- **[Application Guide](../03-application-layer/application-guide.md)** - Application Layer 가이드
- **[REST API Guide](../01-adapter-in-layer/rest-api/rest-api-guide.md)** - REST API Layer 가이드
- **[MySQL Persistence Guide](../04-persistence-layer/mysql/persistence-mysql-guide.md)** - MySQL Adapter 가이드
- **[Redis Persistence Guide](../04-persistence-layer/redis/persistence-redis-guide.md)** - Redis Adapter 가이드

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 2.0.0
