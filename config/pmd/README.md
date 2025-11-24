# PMD 규칙셋: Hexagonal Architecture - Law of Demeter

이 디렉터리는 프로젝트 전반의 설계 일관성과 헥사고날 아키텍처 원칙 준수를 위해 작성된 PMD 규칙셋(`pmd-ruleset.xml`)을 보관합니다. 특히 데미터의 법칙(LoD), 단일 책임 원칙(SRP), 레이어 경계, 과도한 결합/복잡도 방지에 초점을 둡니다.

- 규칙셋 파일: `config/pmd/pmd-ruleset.xml`
- 이름: "Hexagonal Architecture - Law of Demeter"
- 작성자: Sangwon Ryu (ryu@company.com)
- 도입일: 2025-01-10

## 적용 범위 및 네이밍 기준
- Java 코드 전반. 레이어 식별은 패키지명으로 판별합니다.
  - 도메인 레이어: 패키지에 `.domain.` 포함
  - 애플리케이션 레이어: 패키지에 `.application.` 포함
  - 컨트롤러(웹) 어댑터: 패키지에 `adapter.in.` 포함

## 우선순위(Severity)
- PMD Priority 1이 가장 높음(가장 엄격/중요). 숫자가 클수록 우선순위 낮음.

## 규칙 요약

### 1) 데미터의 법칙 (Law of Demeter)
- LawOfDemeter (`category/java/design.xml/LawOfDemeter`)
  - priority: 1
  - properties
    - `violationSuppressRegex`: `""` (비활성 예외 없음)
    - `trustRadius`: `2` (Builder/Stream 등 Fluent API를 제한적으로 허용)

- DomainLayerDemeterStrict (사용자 정의 XPath 규칙)
  - priority: 1
  - 목적: 도메인 레이어에서 체이닝 메서드 호출(2단계 이상) 금지
  - 예외 허용
    - 빌더 체인: `builder`, `toBuilder`
    - Stream 파이프라인: `stream`, `map`, `flatMap`, `filter`, `distinct`, `sorted`, `collect`, `reduce`, `peek`, `limit`, `skip`
    - Optional 체인: `orElse`, `orElseGet`, `orElseThrow`, `ifPresent`, `ifPresentOrElse`, `map`, `flatMap`
    - BigDecimal 연산 체인
  - 비고: PMD 6.x/7.x 호환을 위해 `PrimaryExpression | ASTPrimaryExpression`를 함께 사용

### 2) 설계 일반 규칙
- DataClass (`category/java/design.xml/DataClass`)
  - priority: 2
  - DTO/Request/Response/Record/Interface는 위반에서 제외 (`violationSuppressXPath`)

- ExcessiveImports (`category/java/design.xml/ExcessiveImports`)
  - priority: 3
  - `minimum`: `30` (임포트 30개 초과 시 경고)

- AvoidDeeplyNestedIfStmts (`category/java/design.xml/AvoidDeeplyNestedIfStmts`)
  - priority: 2
  - `problemDepth`: `3` (3단계 초과 중첩 if 금지)

- GodClass (`category/java/design.xml/GodClass`)
  - priority: 2
  - `topscore`: `70` (LCOM > 0.7 수준으로 간주 시 경고)

### 3) 단일 책임 원칙(SRP) 관련
- DomainTooManyMethods (사용자 정의 XPath)
  - priority: 1
  - 대상: `.domain.` 패키지의 클래스(예외 클래스 제외)
  - 제한: public 메서드 개수 > 7 금지

- UseCaseTooManyMethods (사용자 정의 XPath)
  - priority: 1
  - 대상: `.application.` 패키지 내 `*UseCase`, `*Service` 클래스
  - 제한: public 메서드 개수 > 5 금지

- TooManyMethods (`category/java/design.xml/TooManyMethods`)
  - priority: 2
  - 컨트롤러(웹) 전용 적용: `violationSuppressXPath`로 `adapter.in.` 외 영역은 무시
  - 제한: `maxmethods = 10`

- TooManyFields (`category/java/design.xml/TooManyFields`)
  - priority: 2
  - 제한: `maxfields = 7`

### 4) 클래스 길이 제한
- DomainExcessiveClassLength (사용자 정의 XPath)
  - priority: 2
  - 대상: `.domain.` 패키지 클래스
  - 제한: 200라인 초과 금지

- ApplicationExcessiveClassLength (사용자 정의 XPath)
  - priority: 2
  - 대상: `.application.` 패키지 클래스
  - 제한: 150라인 초과 금지

- ExcessiveClassLength (`category/java/design.xml/ExcessiveClassLength`)
  - priority: 3
  - 전역 완충 룰: `minimum = 300`

### 5) 레이어 경계 규칙
- ControllerNoRepositoryDependency (사용자 정의 XPath)
  - priority: 1
  - 대상: `adapter.in.` 패키지의 컨트롤러
  - 제한: 클래스 필드에 `*Repository` 타입 직접 의존 금지 (UseCase에만 의존)

### 6) 허용(화이트리스트) 패턴 (False Positive 방지 가이드)
- AllowBuilderPattern (정보성)
  - priority: 5, 메시지: "Builder pattern is allowed"
  - 빌더 체인은 정책적으로 허용됨을 알리기 위한 안내성 규칙

- AllowStreamAPI (정보성)
  - priority: 5, 메시지: "Stream API is allowed"
  - Stream 파이프라인은 허용됨을 알리기 위한 안내성 규칙

> 참고: 위 두 규칙은 정책 의도를 문서화하기 위한 정보성 수준(우선순위 5)으로 설정되어 있습니다.

---

## 사용 방법

### Maven (pmd-maven-plugin)
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-pmd-plugin</artifactId>
  <version>3.24.0</version>
  <configuration>
    <rulesets>
      <ruleset>config/pmd/pmd-ruleset.xml</ruleset>
    </rulesets>
    <printFailingErrors>true</printFailingErrors>
    <failOnViolation>true</failOnViolation>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### Gradle (pmd 플러그인)
```groovy
plugins {
  id 'pmd'
}

pmd {
  toolVersion = '6.55.0' // 또는 프로젝트 호환 버전
  ruleSets = []           // 기본 룰셋 비활성화
  ruleSetFiles = files('config/pmd/pmd-ruleset.xml')
}

tasks.withType(Pmd) {
  consoleOutput = true
  ignoreFailures = false
}
```

### CLI (단독 실행)
```bash
pmd check \
  -d src/main/java \
  -R config/pmd/pmd-ruleset.xml \
  -f text
```

## 예외(무시) 처리 방법
필요 시 코드 단위에서 특정 룰을 예외 처리할 수 있습니다. 반드시 팀 합의와 코드리뷰 근거를 남기세요.

- 어노테이션 사용
  ```java
  @SuppressWarnings("PMD.LawOfDemeter")
  void method() { ... }
  ```

- 라인 단위 무시
  ```java
  doSomething(); // NOPMD - 합당한 예외 사유를 여기에 적으세요
  ```

- 대표 룰 이름
  - 표준: `LawOfDemeter`, `DataClass`, `ExcessiveImports`, `AvoidDeeplyNestedIfStmts`, `GodClass`, `TooManyMethods`, `TooManyFields`, `ExcessiveClassLength`
  - 사용자 정의: `DomainTooManyMethods`, `UseCaseTooManyMethods`, `DomainExcessiveClassLength`, `ApplicationExcessiveClassLength`, `DomainLayerDemeterStrict`, `ControllerNoRepositoryDependency`, `AllowBuilderPattern`, `AllowStreamAPI`

## 개선 가이드(위반 시 리팩터링 힌트)
- 데미터의 법칙: 체이닝 제거. 질의보다 명령(`Tell, Don't Ask`) 우선. 협력자에 일을 위임.
- 과도한 메서드/필드: 클래스를 책임 단위로 분리. 내부 응집도 재점검.
- 과도한 클래스 길이/중첩: 메서드 추출, 조기 반환(early return), 전략/상태 등의 패턴 도입 검토.
- 레이어 경계: 컨트롤러는 오직 UseCase에만 의존. Repository는 아웃고잉 어댑터에서 다룸.

## 버전 호환성
- `DomainLayerDemeterStrict`는 PMD 6.x/7.x 모두에서 동작하도록 노드 타입을 병행 사용합니다.

---
본 규칙셋은 헥사고날 아키텍처의 **도메인 순수성**, **응집도**, **레이어 분리**를 강제합니다. 위반이 보고될 경우, 규칙의 의도와 팀 표준에 맞도록 리팩터링을 우선 검토하세요.
