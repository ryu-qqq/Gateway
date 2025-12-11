# 🐛 SpotBugs 설정 가이드

## 개요

SpotBugs는 Java 바이트코드를 분석하여 잠재적인 버그, 성능 문제, 보안 취약점을 탐지하는 정적 분석 도구입니다.
FindBugs의 후속 프로젝트로, 400개 이상의 버그 패턴을 검출합니다.

## 설정 파일

- **위치**: `config/spotbugs/spotbugs-exclude.xml`
- **적용 범위**: 모든 서브프로젝트의 컴파일된 클래스 파일
- **실행 시점**: 빌드 시 자동 실행 (`./gradlew build`)

## 설정 상세

### Effort Level: MAX
```kotlin
effort.set(com.github.spotbugs.snom.Effort.MAX)
```

가장 철저한 분석을 수행합니다. 빌드 시간이 약간 증가하지만 더 많은 버그를 탐지합니다.

### Report Level: LOW
```kotlin
reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
```

낮은 확신도의 버그도 모두 리포트합니다. False positive가 증가할 수 있지만 잠재적 문제를 놓치지 않습니다.

## 제외 규칙

### 1. 생성된 코드 제외

#### QueryDSL Q클래스
```xml
<Match>
    <Class name="~.*\.Q.*"/>
</Match>
```

**이유**: QueryDSL이 자동 생성한 Q클래스는 분석 대상에서 제외합니다.

#### Generated 패키지
```xml
<Match>
    <Package name="~.*\.generated\..*"/>
</Match>
```

**이유**: 코드 생성 도구(MapStruct, Lombok 등)가 생성한 코드는 제어 불가능합니다.

### 2. 테스트 코드 완화

```xml
<Match>
    <Class name="~.*Test"/>
    <Bug pattern="DMI_HARDCODED_ABSOLUTE_FILENAME"/>
</Match>
```

**완화된 규칙**:
- `DMI_HARDCODED_ABSOLUTE_FILENAME`: 테스트에서 하드코딩된 파일 경로 허용

**이유**: 테스트 코드는 재현 가능한 환경에서 실행되므로 일부 규칙을 완화합니다.

### 3. Configuration 클래스 완화

```xml
<Match>
    <Class name="~.*Configuration"/>
    <Bug pattern="UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"/>
</Match>
```

**완화된 규칙**:
- `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR`: 생성자에서 초기화되지 않은 필드 허용

**이유**: Spring Configuration 클래스는 필드 주입을 사용할 수 있습니다.

## 주요 검출 버그 패턴

### 1. Correctness (정확성)

| 패턴 | 설명 | 예제 |
|------|------|------|
| `NP_NULL_ON_SOME_PATH` | null 참조 가능성 | `obj.method()` where obj could be null |
| `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` | 불필요한 null 체크 | `if (nonNullValue != null)` |
| `EC_NULL_ARG` | null 인자 전달 | `method(null)` where parameter is @NonNull |

### 2. Bad Practice (나쁜 관행)

| 패턴 | 설명 | 예제 |
|------|------|------|
| `SE_BAD_FIELD` | Serializable에 non-serializable 필드 | `class A implements Serializable { B field; }` |
| `EQ_COMPARETO_USE_OBJECT_EQUALS` | compareTo와 equals 불일치 | `compareTo` implemented but not `equals` |
| `DMI_EMPTY_DB_PASSWORD` | 빈 데이터베이스 비밀번호 | `conn.setPassword("")` |

### 3. Performance (성능)

| 패턴 | 설명 | 예제 |
|------|------|------|
| `SIC_INNER_SHOULD_BE_STATIC` | static으로 선언 가능한 inner class | `class Outer { class Inner {} }` |
| `DM_STRING_CTOR` | 불필요한 String 생성자 | `new String("text")` |
| `SBSC_USE_STRINGBUFFER_CONCATENATION` | StringBuilder 미사용 | `str = str + "a"` in loop |

### 4. Security (보안)

| 패턴 | 설명 | 예제 |
|------|------|------|
| `SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING` | SQL Injection 취약점 | `"SELECT * FROM users WHERE id=" + id` |
| `HARD_CODE_PASSWORD` | 하드코딩된 비밀번호 | `password = "admin123"` |
| `PREDICTABLE_RANDOM` | 예측 가능한 난수 | `new Random()` for security |

## 실행 방법

### 전체 검사
```bash
./gradlew spotbugsMain
```

### 특정 모듈만 검사
```bash
./gradlew :domain:spotbugsMain
./gradlew :application:spotbugsMain
```

### 빌드 시 자동 실행
```bash
./gradlew build
# SpotBugs가 자동으로 실행되며, 버그 발견 시 빌드 실패
```

## 리포트 확인

### HTML 리포트
```bash
# 리포트 생성
./gradlew spotbugsMain

# 리포트 위치
open build/reports/spotbugs/main.html
```

### XML 리포트 (CI/CD 통합용)
```
build/reports/spotbugs/main.xml
```

## 설정 커스터마이징

### SpotBugs 버전 변경
`gradle/libs.versions.toml`:
```toml
[versions]
spotbugs = "4.8.3"  # 원하는 버전으로 변경
```

### Effort/Report Level 조정
`build.gradle.kts`:
```kotlin
spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)  // MAX → DEFAULT
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)  // LOW → MEDIUM
}
```

### 추가 제외 규칙
`config/spotbugs/spotbugs-exclude.xml`:
```xml
<Match>
    <Class name="com.company.template.LegacyCode"/>
    <Bug pattern="NP_NULL_ON_SOME_PATH"/>
</Match>
```

### 특정 버그 패턴만 검출
`config/spotbugs/spotbugs-include.xml` 생성:
```xml
<FindBugsFilter>
    <Match>
        <Bug pattern="SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"/>
    </Match>
    <Match>
        <Bug pattern="HARD_CODE_PASSWORD"/>
    </Match>
</FindBugsFilter>
```

## 문제 해결

### 자주 발생하는 패턴

#### 1. Null Pointer 경고
```bash
Bug: NP_NULL_ON_SOME_PATH
```

**해결**:
```java
// ❌ Before
public String getName(User user) {
    return user.getName();  // user가 null일 수 있음
}

// ✅ After
public String getName(User user) {
    Objects.requireNonNull(user, "User cannot be null");
    return user.getName();
}
```

#### 2. equals/hashCode 불일치
```bash
Bug: HE_EQUALS_USE_HASHCODE
```

**해결**:
```java
// ❌ Before
@Override
public boolean equals(Object o) {
    // equals만 구현
}

// ✅ After
@Override
public boolean equals(Object o) {
    // equals 구현
}

@Override
public int hashCode() {
    return Objects.hash(id, name);  // hashCode도 구현
}
```

#### 3. 리소스 누수
```bash
Bug: OBL_UNSATISFIED_OBLIGATION
```

**해결**:
```java
// ❌ Before
InputStream is = new FileInputStream(file);
is.read();

// ✅ After
try (InputStream is = new FileInputStream(file)) {
    is.read();
}  // 자동으로 close()
```

## 통합 도구

### IntelliJ IDEA
1. Plugins → SpotBugs 설치
2. Analyze → Run Inspection by Name → SpotBugs
3. 설정 파일: `config/spotbugs/spotbugs-exclude.xml`

### SonarQube 통합
```kotlin
plugins {
    id("org.sonarqube") version "4.0.0"
}

sonarqube {
    properties {
        property("sonar.java.spotbugs.reportPaths", "build/reports/spotbugs/main.xml")
    }
}
```

## False Positive 처리

### @SuppressFBWarnings 사용
```java
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "NP_NULL_ON_SOME_PATH",
    justification = "Validated by @NotNull annotation"
)
public String getName(User user) {
    return user.getName();
}
```

**주의**: 남용하지 말고 정당한 이유가 있을 때만 사용하세요.

## 참고 자료

- [SpotBugs 공식 문서](https://spotbugs.github.io/)
- [버그 패턴 전체 목록](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
- [프로젝트 코딩 표준](../../docs/CODING_STANDARDS.md)
