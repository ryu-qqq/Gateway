# Gradle Configuration — **빌드 설정 및 QA 도구 가이드**

> **목적**: 루트 build.gradle 설정, QA 도구 구성, 커스텀 태스크 가이드

---

## 1️⃣ 개요

### 프로젝트 QA 도구 스택

```
┌─────────────────────────────────────────────────────────────┐
│                    Build Quality Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  📐 Code Style        📊 Static Analysis    📈 Coverage     │
│  ┌──────────────┐    ┌──────────────────┐   ┌────────────┐ │
│  │ Checkstyle   │    │ SpotBugs         │   │ JaCoCo     │ │
│  │ Spotless     │    │ PMD              │   │            │ │
│  └──────────────┘    └──────────────────┘   └────────────┘ │
│                                                              │
│  🏗️ Architecture     🔧 Custom Tasks                        │
│  ┌──────────────┐    ┌──────────────────────────────────┐  │
│  │ ArchUnit     │    │ checkNoLombok                    │  │
│  │              │    │ verifyVersionCatalog             │  │
│  │              │    │ detectDeadCode                   │  │
│  └──────────────┘    └──────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 도구별 역할

| 도구 | 역할 | 설정 파일 |
|------|------|----------|
| **Checkstyle** | 코드 스타일 검사 (들여쓰기, 임포트 등) | `config/checkstyle/checkstyle.xml` |
| **SpotBugs** | 버그 패턴 탐지 (NullPointer, 리소스 누수 등) | `config/spotbugs/spotbugs-exclude.xml` |
| **PMD** | 설계 규칙 검사 (Law of Demeter, SRP 등) | `config/pmd/pmd-ruleset.xml` |
| **Spotless** | 코드 포맷팅 (Google Java Format) | build.gradle 내 설정 |
| **JaCoCo** | 테스트 커버리지 측정 | build.gradle 내 설정 |
| **ArchUnit** | 아키텍처 규칙 검증 | 테스트 코드 |

---

## 2️⃣ 루트 build.gradle 구조

### 플러그인 선언

```gradle
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id 'java'
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    id 'checkstyle'
    alias(libs.plugins.spotbugs) apply false
    id 'pmd'
    alias(libs.plugins.spotless) apply false
}
```

**설명**:
- `apply false`: 루트에서는 선언만, 서브프로젝트에서 적용
- `alias(libs.plugins.*)`: Version Catalog 참조
- Checkstyle/PMD는 Gradle 내장 플러그인으로 `id 'checkstyle'` 형태

### 전역 설정 (allprojects)

```gradle
allprojects {
    group = 'com.company.template'
    version = '1.0.0-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}
```

### 서브프로젝트 공통 설정 (subprojects)

```gradle
subprojects {
    apply plugin: 'java'
    apply plugin: 'checkstyle'
    apply plugin: 'com.github.spotbugs'
    apply plugin: 'pmd'
    apply plugin: 'com.diffplug.spotless'

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // ... 세부 설정
}
```

---

## 3️⃣ Checkstyle 설정

### build.gradle 설정

```gradle
checkstyle {
    toolVersion = rootProject.libs.versions.checkstyle.get()
    configFile = rootProject.file('config/checkstyle/checkstyle.xml')
    ignoreFailures = false  // 위반 시 빌드 실패
    maxWarnings = 0         // 경고도 허용 안 함
}
```

### config/checkstyle/checkstyle.xml

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
        "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<!-- Lean Checkstyle tuned for DDD/Hexagonal projects -->
<module name="Checker">
    <property name="severity" value="warning"/>

    <!-- Suppressions 파일 연결 -->
    <module name="SuppressionFilter">
        <property name="file" value="${config_loc}/checkstyle-suppressions.xml"/>
    </module>
    <module name="SuppressWarningsFilter"/>

    <!-- 기본 포맷 규칙 -->
    <module name="FileTabCharacter">
        <property name="eachLine" value="true"/>
    </module>
    <module name="NewlineAtEndOfFile"/>
    <module name="LineLength">
        <property name="max" value="140"/>
        <property name="ignorePattern" value="^package |^import |a href|http://|https://"/>
    </module>

    <module name="TreeWalker">
        <!-- Import 규칙 -->
        <module name="AvoidStarImport"/>
        <module name="UnusedImports"/>
        <module name="RedundantImport"/>

        <!-- 브레이스 규칙 -->
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>

        <!-- 공백 규칙 -->
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>

        <!-- ⚠️ Lombok 금지 (Zero-Tolerance) -->
        <module name="IllegalImport">
            <property name="illegalPkgs" value="lombok"/>
        </module>
    </module>
</module>
```

### 주요 검사 항목

| 규칙 | 설명 | 심각도 |
|------|------|--------|
| `AvoidStarImport` | `import java.util.*` 금지 | Warning |
| `IllegalImport(lombok)` | Lombok 패키지 금지 | Error |
| `LineLength(140)` | 140자 초과 금지 | Warning |
| `NeedBraces` | if/for/while에 중괄호 필수 | Warning |

---

## 4️⃣ SpotBugs 설정

### build.gradle 설정

```gradle
spotbugs {
    toolVersion = rootProject.libs.versions.spotbugs.get()
    effort = 'max'           // 최대 분석 깊이
    reportLevel = 'low'      // 낮은 우선순위 버그도 보고
    excludeFilter = rootProject.file('config/spotbugs/spotbugs-exclude.xml')
}
```

### config/spotbugs/spotbugs-exclude.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- QueryDSL 생성 클래스 제외 -->
    <Match>
        <Class name="~.*\.Q.*"/>
    </Match>

    <!-- generated 패키지 제외 -->
    <Match>
        <Package name="~.*\.generated\..*"/>
    </Match>

    <!-- 테스트 코드 특정 규칙 제외 -->
    <Match>
        <Class name="~.*Test"/>
        <Bug pattern="DMI_HARDCODED_ABSOLUTE_FILENAME"/>
    </Match>

    <!-- Configuration 클래스 특정 규칙 제외 -->
    <Match>
        <Class name="~.*Configuration"/>
        <Bug pattern="UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"/>
    </Match>
</FindBugsFilter>
```

### SpotBugs Annotations 사용

```gradle
// build.gradle (subprojects)
dependencies {
    compileOnly rootProject.libs.spotbugs.annotations
}
```

```java
// 코드에서 사용
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "NP_NULL_ON_SOME_PATH",
    justification = "null 체크가 상위에서 수행됨"
)
public void someMethod() { ... }
```

---

## 5️⃣ PMD 설정 (Law of Demeter 강제)

### build.gradle 설정

```gradle
pmd {
    toolVersion = rootProject.libs.versions.pmd.get()
    consoleOutput = true
    ruleSetFiles = files(rootProject.file('config/pmd/pmd-ruleset.xml'))
    ruleSets = []  // 커스텀 ruleset만 사용
    ignoreFailures = false
}

tasks.withType(Pmd) {
    reports {
        xml.required = true
        html.required = true
    }
}
```

### config/pmd/pmd-ruleset.xml (핵심 규칙)

```xml
<?xml version="1.0"?>
<ruleset name="Hexagonal Architecture - Law of Demeter"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0">

    <description>
        Law of Demeter enforcement for Hexagonal Architecture

        데미터의 법칙 (Law of Demeter):
        - 객체는 자기 자신, 파라미터, 생성한 객체, 인스턴스 변수만 접근
        - Train wreck (a.getB().getC()) 금지
        - Tell, Don't Ask 원칙 준수
    </description>

    <!-- ========================================
         Law of Demeter (최우선 규칙)
         ======================================== -->
    <rule ref="category/java/design.xml/LawOfDemeter">
        <priority>1</priority>
        <properties>
            <property name="trustRadius" value="2"/>
        </properties>
    </rule>

    <!-- ========================================
         Domain Layer 엄격 규칙
         ======================================== -->
    <rule name="DomainLayerDemeterStrict"
          message="Domain layer must strictly follow Law of Demeter"
          language="java"
          class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
        <priority>1</priority>
        <!-- Domain 레이어 체이닝 금지 (Builder/Stream 제외) -->
    </rule>

    <!-- Domain 클래스 메서드 수 제한 (≤ 7) -->
    <rule name="DomainTooManyMethods"
          message="Domain class has too many methods (max 7) - violates SRP">
        <priority>1</priority>
    </rule>

    <!-- UseCase 메서드 수 제한 (≤ 5) -->
    <rule name="UseCaseTooManyMethods"
          message="UseCase has too many methods (max 5)">
        <priority>1</priority>
    </rule>

    <!-- Controller → Repository 직접 의존 금지 -->
    <rule name="ControllerNoRepositoryDependency"
          message="Controller must not depend on Repository">
        <priority>1</priority>
    </rule>

    <!-- ========================================
         설계 품질 규칙
         ======================================== -->
    <rule ref="category/java/design.xml/DataClass">
        <priority>2</priority>
        <!-- DTO, Request, Response, Record는 제외 -->
    </rule>

    <rule ref="category/java/design.xml/ExcessiveImports">
        <priority>3</priority>
        <properties>
            <property name="minimum" value="30"/>
        </properties>
    </rule>

    <rule ref="category/java/design.xml/GodClass">
        <priority>2</priority>
    </rule>

    <rule ref="category/java/design.xml/TooManyFields">
        <priority>2</priority>
        <properties>
            <property name="maxfields" value="7"/>
        </properties>
    </rule>
</ruleset>
```

### PMD 레이어별 클래스 크기 제한

| 레이어 | 최대 라인 수 | 최대 메서드 수 |
|--------|-------------|---------------|
| Domain | 200 | 7 |
| Application | 150 | 5 (UseCase) |
| Adapter | 300 | 10 (Controller) |

---

## 6️⃣ Spotless 설정 (코드 포맷팅)

### build.gradle 설정

```gradle
spotless {
    java {
        // Google Java Format (AOSP style - 4 spaces)
        googleJavaFormat('1.22.0').aosp().reflowLongStrings()

        // 대상 파일
        target 'src/*/java/**/*.java'
        targetExclude '**/generated/**', '**/Q*.java'
    }

    format 'misc', {
        target '*.md', '.gitignore', '.gitattributes', '*.yaml', '*.yml'
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

### 주요 설정

| 설정 | 설명 |
|------|------|
| `googleJavaFormat().aosp()` | Google Java Format (4 spaces 들여쓰기) |
| `reflowLongStrings()` | 긴 문자열 자동 줄바꿈 |
| `targetExclude '**/Q*.java'` | QueryDSL 생성 클래스 제외 |

### 포맷팅 명령

```bash
# 포맷 검사
./gradlew spotlessCheck

# 자동 포맷 적용
./gradlew spotlessApply
```

---

## 7️⃣ JaCoCo 설정 (테스트 커버리지)

### build.gradle 설정

```gradle
apply plugin: 'jacoco'

jacoco {
    toolVersion = rootProject.libs.versions.jacoco.get()
}

tasks.named('jacocoTestReport') {
    dependsOn 'test'

    reports {
        xml.required = true
        html.required = true
    }
}
```

### 모듈별 커버리지 기준

```gradle
tasks.named('jacocoTestCoverageVerification') {
    dependsOn 'jacocoTestReport'

    violationRules {
        // 모듈별 전체 커버리지 기준
        rule {
            enabled = true

            limit {
                minimum = project.name == 'domain' ? 0.90 :
                          project.name == 'application' ? 0.80 :
                          project.name.startsWith('adapter-') ? 0.70 : 0.70
            }
        }

        // 클래스별 라인 커버리지 기준
        rule {
            enabled = true
            element = 'CLASS'

            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.50
            }

            excludes = [
                '*.config.*',
                '*.Application',
                '*.Q*'  // QueryDSL 생성 클래스
            ]
        }
    }
}
```

### 모듈별 커버리지 기준표

| 모듈 | 최소 커버리지 | 근거 |
|------|-------------|------|
| **domain** | 90% | 핵심 비즈니스 로직, 순수 Java |
| **application** | 80% | UseCase 로직 |
| **adapter-*** | 70% | 인프라 통합 코드 |
| **bootstrap-*** | 70% | 설정 및 진입점 |

### 커버리지 리포트 위치

```
build/
└── reports/
    └── jacoco/
        └── test/
            └── html/
                └── index.html  ← 브라우저로 열기
```

---

## 8️⃣ 커스텀 검증 태스크

### checkNoLombok (Lombok 금지 검증)

```gradle
tasks.register('checkNoLombok') {
    doLast {
        def lombokFound = configurations.collect { config ->
            config.dependencies.findAll { dep ->
                dep.group == 'org.projectlombok' && dep.name == 'lombok'
            }
        }.flatten()

        if (!lombokFound.isEmpty()) {
            throw new GradleException("""
❌ LOMBOK DETECTED: Lombok is strictly prohibited in this project.
Found in: ${project.name}

Policy: All modules must use pure Java without Lombok.
""")
        }
    }
}
```

### verifyVersionCatalog (버전 하드코딩 금지)

```gradle
tasks.register('verifyVersionCatalog') {
    group = 'verification'
    description = 'Verify all versions in libs.versions.toml use version.ref'

    doLast {
        def catalogFile = rootProject.file('gradle/libs.versions.toml')
        // [libraries] 섹션에서 version = "x.x.x" 하드코딩 검출
        // 위반 시 빌드 실패
    }
}
```

### detectDeadCode (미사용 코드 탐지)

```gradle
tasks.register('detectDeadCode') {
    group = 'verification'
    description = 'Detect potentially unused code across all modules'

    dependsOn subprojects.collect { it.tasks.named('spotbugsMain') }
    dependsOn subprojects.collect { it.tasks.named('jacocoTestReport') }
}
```

---

## 9️⃣ 빌드 태스크 의존성

### build 태스크 체인

```gradle
tasks.named('build') {
    dependsOn 'spotlessCheck'           // 포맷 검사
    dependsOn 'jacocoTestCoverageVerification'  // 커버리지 검증
    dependsOn 'checkNoLombok'           // Lombok 금지 검증
    dependsOn rootProject.tasks.named('verifyVersionCatalog')  // 버전 관리 검증
}
```

### 실행 순서

```
./gradlew build
    │
    ├── compileJava
    │
    ├── checkstyleMain          ← 코드 스타일
    ├── spotbugsMain            ← 버그 패턴
    ├── pmdMain                 ← 설계 규칙 (Law of Demeter)
    │
    ├── test
    │   └── ArchUnit Tests      ← 아키텍처 규칙
    │
    ├── jacocoTestReport
    ├── jacocoTestCoverageVerification  ← 커버리지 기준
    │
    ├── spotlessCheck           ← 포맷팅
    ├── checkNoLombok           ← Lombok 금지
    └── verifyVersionCatalog    ← 버전 관리
```

---

## 🔟 컴파일러 설정

### JavaCompile 옵션

```gradle
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.compilerArgs.addAll([
        '-Xlint:unchecked',      // 미검사 경고 활성화
        '-Xlint:deprecation',    // Deprecated 경고 활성화
        '-parameters'            // 메서드 파라미터 이름 보존 (Reflection용)
    ])
}
```

### 옵션 설명

| 옵션 | 용도 |
|------|------|
| `-Xlint:unchecked` | 제네릭 타입 미검사 경고 |
| `-Xlint:deprecation` | Deprecated API 사용 경고 |
| `-parameters` | 리플렉션에서 파라미터 이름 사용 가능 (Spring Data, Jackson) |

---

## 1️⃣1️⃣ 명령어 요약

### 검증 명령어

```bash
# 전체 빌드 (모든 검증 포함)
./gradlew clean build

# 코드 스타일 검사
./gradlew checkstyleMain checkstyleTest

# 버그 패턴 검사
./gradlew spotbugsMain

# 설계 규칙 검사 (Law of Demeter)
./gradlew pmdMain

# 포맷팅 검사
./gradlew spotlessCheck

# 포맷팅 자동 적용
./gradlew spotlessApply

# 테스트 + 커버리지
./gradlew test jacocoTestReport

# 커버리지 검증
./gradlew jacocoTestCoverageVerification

# Version Catalog 일관성 검증
./gradlew verifyVersionCatalog

# 미사용 코드 탐지
./gradlew detectDeadCode

# ArchUnit 테스트만 실행
./gradlew test --tests "*ArchTest"
```

### 리포트 위치

| 도구 | 리포트 위치 |
|------|------------|
| Checkstyle | `build/reports/checkstyle/` |
| SpotBugs | `build/reports/spotbugs/` |
| PMD | `build/reports/pmd/` |
| JaCoCo | `build/reports/jacoco/test/html/` |
| Test Results | `build/reports/tests/test/` |

---

## 1️⃣2️⃣ Version Catalog 버전 정보

### libs.versions.toml QA 도구 버전

```toml
[versions]
# Architecture & Quality
archunit = "1.2.1"
checkstyle = "10.14.0"
spotbugs = "4.8.3"
spotbugsPlugin = "6.0.9"
jacoco = "0.8.11"
pmd = "7.0.0"
spotless = "7.0.0.BETA4"

[plugins]
spotbugs = { id = "com.github.spotbugs", version.ref = "spotbugsPlugin" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

---

## 1️⃣3️⃣ 체크리스트

### QA 도구 설정 시

- [ ] `config/checkstyle/checkstyle.xml` 설정
- [ ] `config/checkstyle/checkstyle-suppressions.xml` 설정 (필요 시)
- [ ] `config/spotbugs/spotbugs-exclude.xml` 설정
- [ ] `config/pmd/pmd-ruleset.xml` 설정 (Law of Demeter 포함)
- [ ] `libs.versions.toml`에 도구 버전 정의
- [ ] 루트 `build.gradle`에 플러그인 및 설정 추가
- [ ] 모듈별 JaCoCo 커버리지 기준 확인
- [ ] `./gradlew clean build` 전체 빌드 통과
- [ ] 커버리지 리포트 확인

### 빌드 실패 시 확인 사항

| 오류 | 원인 | 해결 |
|------|------|------|
| Checkstyle 실패 | 코드 스타일 위반 | `./gradlew checkstyleMain` 리포트 확인 |
| SpotBugs 실패 | 버그 패턴 감지 | `build/reports/spotbugs/` 확인 |
| PMD 실패 | Law of Demeter 위반 | Getter 체이닝 제거 |
| JaCoCo 실패 | 커버리지 미달 | 테스트 추가 |
| Lombok 감지 | Lombok 의존성 추가됨 | 의존성 제거 |

---

## 📖 관련 문서

- **[Multi-Module Structure](./multi-module-structure.md)** - 멀티모듈 구조 가이드
- **[Version Management](./version-management.md)** - 버전 관리 전략
- **[Domain Guide](../02-domain-layer/domain-guide.md)** - 도메인 레이어 규칙

---

**작성자**: Development Team
**최종 수정일**: 2025-12-05
**버전**: 1.0.0
