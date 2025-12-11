# Version Management — **Gradle Version Catalog 기반 의존성 관리**

> **목적**: `gradle/libs.versions.toml` 기반의 중앙 집중식 버전 관리 전략

---

## 1️⃣ 핵심 버전

### Java

```
Java Version: 21 LTS
Release Date: 2023-09-19
Support Until: 2029-09 (LTS)
```

**선택 이유**:
- **LTS (Long Term Support)**: 장기 지원 버전
- **Virtual Threads**: 경량 스레드 지원 (Project Loom)
- **Record Patterns**: Pattern Matching 강화
- **Sequenced Collections**: 순서 보장 컬렉션

### Spring Boot

```
Spring Boot Version: 3.5.x
Spring Framework Version: 6.2.x
Release Date: 2024-11
Support Until: 2027-11 (OSS), 2029-11 (Commercial)
```

**선택 이유**:
- **Java 21 Native Support**: Java 21 완벽 지원
- **Jakarta EE 10**: javax → jakarta 전환 완료
- **AOT (Ahead-of-Time) Compilation**: 네이티브 이미지 지원
- **Observability**: Micrometer 통합 강화

---

## 2️⃣ Version Catalog vs gradle.properties

### 비교

| 항목 | `libs.versions.toml` | `gradle.properties` |
|------|---------------------|---------------------|
| **목적** | 의존성 버전 관리 | Gradle 빌드 설정 |
| **도입 시기** | Gradle 7.0+ (2021년) | 초창기부터 존재 |
| **관리 대상** | 라이브러리, 플러그인 버전 | JVM 옵션, 빌드 플래그 |
| **IDE 지원** | 자동완성, 타입 안전 | 문자열 참조 |
| **멀티모듈** | 자동 공유 | 명시적 참조 필요 |

### 이 프로젝트 선택: Version Catalog (✅ 권장)

```
gradle/
└── libs.versions.toml   ← 의존성 버전 관리 (사용 중)

gradle.properties        ← 빌드 설정용 (JVM 옵션 등)
```

**이유**:
- IDE 자동완성 (`libs.spring.boot.starter.web`)
- 타입 안전한 접근
- 멀티모듈 의존성 일관성 자동 보장
- BOM/Platform 지원
- Gradle 공식 권장 방식 (7.0+)

---

## 3️⃣ libs.versions.toml 구조

### 위치

```
project-root/
└── gradle/
    └── libs.versions.toml   ← 여기
```

### 구조 예시

```toml
# gradle/libs.versions.toml

[versions]
# ========================================
# Spring & Framework
# ========================================
springBoot = "3.5.6"
springDependencyManagement = "1.1.5"

# ========================================
# Database & Persistence
# ========================================
querydsl = "5.1.0"
postgresql = "42.7.3"

# ========================================
# Testing
# ========================================
junit = "5.10.2"
archunit = "1.2.1"
testcontainers = "1.19.7"

[libraries]
# ========================================
# Spring Boot Starters (버전 생략 - BOM 관리)
# ========================================
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }

# ========================================
# 외부 라이브러리 (version.ref 필수)
# ========================================
querydsl-jpa = { module = "com.querydsl:querydsl-jpa", version.ref = "querydsl" }
postgresql = { module = "org.postgresql:postgresql", version.ref = "postgresql" }
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }

[bundles]
# ========================================
# 자주 사용하는 의존성 묶음
# ========================================
testing-basic = ["junit-jupiter", "assertj-core", "mockito-core"]
testcontainers = ["testcontainers-junit", "testcontainers-postgresql"]

[plugins]
# ========================================
# Gradle Plugins
# ========================================
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "springDependencyManagement" }
```

---

## 4️⃣ 버전 관리 규칙 (Zero-Tolerance)

### ⚠️ 필수 규칙

**❌ 금지: [libraries] 섹션에 하드코딩된 버전**
```toml
# ❌ Bad: version = "x.x.x" 직접 사용
my-lib = { module = "com.example:my-lib", version = "1.2.3" }
```

**✅ 필수: [versions] 섹션에 정의 → version.ref 참조**
```toml
# ✅ Good: version.ref 사용
[versions]
myLib = "1.2.3"

[libraries]
my-lib = { module = "com.example:my-lib", version.ref = "myLib" }
```

### 예외: Spring Boot BOM 관리 의존성

Spring Boot가 BOM으로 관리하는 의존성은 버전 생략 가능:

```toml
# ✅ OK: Spring Boot BOM이 버전 관리
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
```

### 자동 검증: Gradle Task

```bash
# Version Catalog 일관성 검증
./gradlew verifyVersionCatalog
```

**빌드 시 자동 실행**되어 하드코딩된 버전이 있으면 빌드 실패:

```
❌ VERSION CATALOG CONSISTENCY VIOLATION

Hardcoded versions found in [libraries] section.
All versions should use 'version.ref' referencing [versions] section.

Violations:
  Line 120: commons-pool2 has hardcoded version "2.12.0"

Fix: Move version to [versions] section and use 'version.ref' in [libraries].
```

---

## 5️⃣ build.gradle에서 사용

### 루트 build.gradle

```gradle
plugins {
    id 'java'
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    apply plugin: 'io.spring.dependency-management'

    dependencyManagement {
        imports {
            mavenBom SpringBootPlugin.BOM_COORDINATES
        }
    }

    dependencies {
        // ✅ Version Catalog 참조
        testImplementation libs.junit.jupiter
        testImplementation libs.archunit.junit5
    }
}
```

### 모듈 build.gradle

```gradle
plugins {
    id 'java-library'
}

dependencies {
    // ✅ Version Catalog 참조 (rootProject 불필요 - 자동 공유)
    api project(':domain')

    // Spring Boot Starters (BOM 관리)
    implementation libs.spring.boot.starter.web
    implementation libs.spring.boot.starter.data.jpa

    // 외부 라이브러리 (version.ref 관리)
    implementation libs.querydsl.jpa
    runtimeOnly libs.postgresql

    // Bundle 사용
    testImplementation libs.bundles.testing.basic
    testImplementation libs.bundles.testcontainers
}
```

---

## 6️⃣ gradle.properties 용도

`gradle.properties`는 의존성 버전이 아닌 **빌드 설정**용으로 사용:

```properties
# gradle.properties

# ========================================
# JVM Settings
# ========================================
org.gradle.jvmargs=-Xmx2g -XX:+UseParallelGC

# ========================================
# Build Optimization
# ========================================
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# ========================================
# Project Metadata
# ========================================
projectVersion=1.0.0-SNAPSHOT
projectGroup=com.ryuqq
```

---

## 7️⃣ 버전 업데이트 프로세스

### Step 1: libs.versions.toml 업데이트

```toml
[versions]
# Before
springBoot = "3.5.5"

# After
springBoot = "3.5.6"
```

### Step 2: 빌드 및 검증

```bash
# Version Catalog 일관성 검증
./gradlew verifyVersionCatalog

# 전체 빌드
./gradlew clean build

# ArchUnit 검증
./gradlew test --tests "*ArchTest"
```

### Step 3: 변경 문서화

```markdown
# CHANGELOG.md

## [1.2.0] - 2025-12-05

### Changed
- Upgrade Spring Boot 3.5.5 → 3.5.6
- Upgrade QueryDSL 5.0.0 → 5.1.0

### Security
- Apply CVE-2025-XXXXX fix
```

---

## 8️⃣ 버전 업데이트 전략

### 정책

1. **Java LTS 버전 사용**: 21 → 다음 LTS는 25 (2025-09)
2. **Spring Boot Minor 업데이트**: 6개월마다 (3.4 → 3.5 → 4.0)
3. **Spring Boot Patch 업데이트**: 보안 패치 즉시 적용
4. **라이브러리 Major 업데이트**: 분기별 검토
5. **라이브러리 Minor/Patch**: 월별 검토

### 업데이트 주기

```
┌─────────────────────────────────────────────────────┐
│ Immediate (즉시 적용)                                │
│ - Critical Security Patches                         │
│ - Spring Boot Patch Releases (3.5.5 → 3.5.6)       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Monthly (월별 검토)                                  │
│ - Library Minor Releases                            │
│ - Dependency Security Updates                       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Quarterly (분기별 검토)                              │
│ - Spring Boot Minor Releases (3.4 → 3.5)           │
│ - Library Major Releases                            │
│ - Jakarta EE Updates                                │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Yearly (연별 검토)                                   │
│ - Java Version Upgrade (21 → 25 LTS)               │
│ - Spring Boot Major Releases (3.x → 4.x)           │
└─────────────────────────────────────────────────────┘
```

---

## 9️⃣ 의존성 보안 관리

### GitHub Dependabot 설정

**위치**: `.github/dependabot.yml`

```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "development-team"
    labels:
      - "dependencies"
      - "security"

    # Major 버전은 수동 검토
    ignore:
      - dependency-name: "*"
        update-types: ["version-update:semver-major"]
```

### OWASP Dependency Check

```bash
# 보안 취약점 스캔
./gradlew dependencyCheckAnalyze
```

---

## 🔟 체크리스트

버전 업데이트 시:
- [ ] `gradle/libs.versions.toml` 업데이트 (build.gradle 하드코딩 금지)
- [ ] `./gradlew verifyVersionCatalog` 통과
- [ ] 전체 빌드 통과 (`./gradlew clean build`)
- [ ] ArchUnit 테스트 통과
- [ ] Integration 테스트 통과
- [ ] 보안 스캔 통과 (`dependencyCheckAnalyze`)
- [ ] CHANGELOG.md 업데이트
- [ ] Breaking Changes 문서화
- [ ] 팀 전체 공유

---

## 📖 관련 문서

- **[Multi-Module Structure](./multi-module-structure.md)** - 멀티모듈 구조
- **[Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html)** - 공식 문서
- **[Spring Boot Release Notes](https://github.com/spring-projects/spring-boot/releases)** - Spring Boot 릴리즈 노트

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 2.0.0
