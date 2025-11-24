# Version Management — **Java, Spring Boot, 라이브러리 버전 관리**

> **목적**: 프로젝트 전체의 Java, Spring Boot, 주요 라이브러리 버전 정보 및 업데이트 가이드

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
Spring Boot Version: 3.5.0
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

## 2️⃣ 주요 라이브러리 버전

### Persistence

```yaml
# JPA & Hibernate
spring-boot-starter-data-jpa: 3.5.0 (Spring Boot 관리)
hibernate-core: 6.6.x (Spring Boot BOM 관리)

# QueryDSL
querydsl-jpa: 5.0.0:jakarta
querydsl-apt: 5.0.0:jakarta

# Database Drivers
mysql-connector-j: 8.4.0 (Spring Boot 관리)
h2: 2.2.x (Spring Boot 관리)
```

### Testing

```yaml
# JUnit
junit-jupiter: 5.10.0 (Spring Boot 관리)

# Mockito
mockito-junit-jupiter: 5.5.0 (Spring Boot 관리)

# AssertJ
assertj-core: 3.24.2 (Spring Boot 관리)

# ArchUnit
archunit-junit5: 1.1.0

# Testcontainers
testcontainers: 1.19.x (Spring Boot 관리)
testcontainers-mysql: 1.19.x (Spring Boot 관리)
```

### Validation

```yaml
# Bean Validation
jakarta.validation-api: 3.0.2 (Spring Boot 관리)
hibernate-validator: 8.0.x (Spring Boot 관리)
```

### Utilities

```yaml
# Apache Commons
commons-lang3: 3.14.0 (Spring Boot 관리)
commons-collections4: 4.4

# Guava
guava: 32.1.3-jre
```

---

## 3️⃣ Gradle 버전 관리 (gradle.properties 필수)

### ⚠️ 버전 관리 규칙 (Zero-Tolerance)

**❌ 금지**: build.gradle에 직접 버전 하드코딩
```gradle
// ❌ Bad: 버전 하드코딩
id 'org.springframework.boot' version '3.5.0'
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
```

**✅ 필수**: gradle.properties에 버전 명시 → build.gradle에서 참조
```gradle
// ✅ Good: gradle.properties 참조
id 'org.springframework.boot' version "$springBootVersion"
implementation "com.querydsl:querydsl-jpa:${querydslVersion}:jakarta"
```

### gradle.properties (루트 필수)

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
testcontainersVersion=1.19.3
```

### build.gradle 사용 예시

```gradle
plugins {
    id 'java-library'
    // ✅ gradle.properties 버전 참조
    id 'org.springframework.boot' version "$springBootVersion" apply false
    id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
}

dependencies {
    // ✅ gradle.properties 버전 참조
    implementation "com.querydsl:querydsl-jpa:${querydslVersion}:jakarta"
    testImplementation "com.tngtech.archunit:archunit-junit5:${archunitVersion}"
    testImplementation "org.testcontainers:testcontainers:${testcontainersVersion}"
}

java {
    // ✅ gradle.properties 버전 참조
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}
```

### gradle/libs.versions.toml (대안, 선택적)

```toml
[versions]
java = "21"
springBoot = "3.5.0"
springDependencyManagement = "1.1.4"
querydsl = "5.0.0"
archunit = "1.1.0"
commonsCollections4 = "4.4"
guava = "32.1.3-jre"

[libraries]
# QueryDSL
querydsl-jpa = { module = "com.querydsl:querydsl-jpa", version.ref = "querydsl" }
querydsl-apt = { module = "com.querydsl:querydsl-apt", version.ref = "querydsl" }

# ArchUnit
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }

# Apache Commons
commons-collections4 = { module = "org.apache.commons:commons-collections4", version.ref = "commonsCollections4" }

# Guava
guava = { module = "com.google.guava:guava", version.ref = "guava" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "springBoot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "springDependencyManagement" }
```

### 루트 build.gradle

```gradle
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType(JavaCompile) {
        options.encoding = 'UTF-8'
    }
}
```

---

## 4️⃣ 버전 업데이트 전략

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
│ - Spring Boot Patch Releases (3.5.0 → 3.5.1)       │
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

## 5️⃣ 버전 업데이트 프로세스

### Step 1: 버전 확인

```bash
# Gradle 의존성 확인
./gradlew dependencies

# 업데이트 가능한 버전 확인 (Gradle Plugin 필요)
./gradlew dependencyUpdates
```

### Step 2: gradle.properties 업데이트

```properties
# Before
springBootVersion=3.4.0

# After
springBootVersion=3.5.0
```

### Step 3: 빌드 및 테스트

```bash
# 전체 빌드
./gradlew clean build

# ArchUnit 검증
./gradlew test --tests "*ArchTest"

# Integration 테스트
./gradlew integrationTest
```

### Step 4: 버전 변경 문서화

```markdown
# CHANGELOG.md

## [1.2.0] - 2024-11-13

### Changed
- Upgrade Spring Boot 3.4.0 → 3.5.0
- Upgrade QueryDSL 5.0.0 → 5.1.0

### Security
- Apply CVE-2024-XXXXX fix (dependency-X)

### Breaking Changes
- None
```

---

## 6️⃣ 의존성 보안 관리

### GitHub Dependabot 설정

**위치**: `.github/dependabot.yml`

```yaml
version: 2
updates:
  # Gradle 의존성 자동 업데이트
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

    # 보안 패치는 즉시 머지
    commit-message:
      prefix: "chore"
      include: "scope"

    # Major 버전은 수동 검토
    ignore:
      - dependency-name: "*"
        update-types: ["version-update:semver-major"]
```

### OWASP Dependency Check

```gradle
// build.gradle
plugins {
    id 'org.owasp.dependencycheck' version '8.4.0'
}

dependencyCheck {
    failBuildOnCVSS = 7
    suppressionFile = 'config/owasp-suppressions.xml'
}
```

```bash
# 보안 취약점 스캔
./gradlew dependencyCheckAnalyze
```

---

## 7️⃣ 버전 호환성 매트릭스

### Spring Boot 3.5.x 호환성

| Component | Minimum Version | Recommended | Notes |
|-----------|----------------|-------------|-------|
| **Java** | 17 | 21 LTS | Java 21 권장 |
| **Jakarta EE** | 10 | 10 | javax → jakarta |
| **Hibernate** | 6.2 | 6.6.x | JPA 3.1 지원 |
| **QueryDSL** | 5.0.0 | 5.0.0 | Jakarta 전환 필수 |
| **JUnit** | 5.9 | 5.10.x | Jupiter API |
| **Mockito** | 5.0 | 5.5.x | JUnit 5 통합 |

### Jakarta EE 10 마이그레이션

```java
// ❌ Before (javax)
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

// ✅ After (jakarta)
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
```

---

## 8️⃣ 버전 충돌 해결

### Gradle 의존성 해결 전략

```gradle
// build.gradle
configurations.all {
    resolutionStrategy {
        // 특정 버전 강제 사용
        force 'com.google.guava:guava:32.1.3-jre'

        // 버전 충돌 시 최신 버전 선택
        preferProjectModules()

        // Snapshot 버전 캐시 시간 설정
        cacheChangingModulesFor 0, 'seconds'
    }
}
```

### 의존성 충돌 확인

```bash
# 의존성 트리 출력
./gradlew :application:dependencies --configuration runtimeClasspath

# 특정 라이브러리 버전 확인
./gradlew :application:dependencyInsight --dependency guava
```

---

## 9️⃣ 체크리스트

버전 업데이트 시:
- [ ] **gradle.properties 업데이트** (build.gradle 하드코딩 금지)
- [ ] 전체 빌드 통과 (`./gradlew clean build`)
- [ ] ArchUnit 테스트 통과
- [ ] Integration 테스트 통과
- [ ] 보안 스캔 통과 (`dependencyCheckAnalyze`)
- [ ] CHANGELOG.md 업데이트
- [ ] Breaking Changes 문서화
- [ ] 팀 전체 공유

---

## 📖 관련 문서

- **[Gradle Configuration](./gradle-configuration.md)** - Gradle 설정 상세
- **[Multi-Module Structure](./multi-module-structure.md)** - 멀티모듈 구조
- **[Spring Boot Release Notes](https://github.com/spring-projects/spring-boot/releases)** - Spring Boot 릴리즈 노트

---

**작성자**: Development Team
**최종 수정일**: 2025-11-13
**버전**: 1.0.0
