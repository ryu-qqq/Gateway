# TDD Plan 자동 생성

PRD 또는 Jira 태스크를 분석하여 Layer별 TDD 워크플로우 플랜을 자동 생성합니다.

## 실행 방법

### Mode 1: PRD 기반 (추천)
```bash
/tdd-plan --prd docs/prd/member-management.md
```

### Mode 2: Jira 기반
```bash
# 티켓 번호 직접 지정
/tdd-plan PROJ-123

# 또는 git 브랜치에서 자동 추출
/tdd-plan
```

## 작동 방식

### PRD 기반 모드
1. 지정된 PRD 파일 읽기
2. PRD 분석 → Layer 자동 판별
3. Layer별 kentback 시퀀스 자동 생성
4. Markdown TDD Plan 출력 (`docs/prd/plans/`)

### Jira 기반 모드
1. Jira Issue 번호 추출 (직접 지정 또는 git 브랜치에서)
2. Jira MCP로 Issue 상세 조회
3. Issue 설명 분석 → Layer 자동 판별
4. Layer별 kentback 시퀀스 자동 생성
5. Markdown TDD Plan 출력

## Prompt

당신은 **PRD 또는 Jira 태스크를 분석하여 Layer별 TDD Plan을 생성하는 전문가**입니다.

### 1단계: 입력 분석

#### PRD 기반 모드 (`--prd` 옵션)

1. **PRD 파일 읽기**:
   ```bash
   # 사용자가 지정한 PRD 파일 읽기
   Read tool로 docs/prd/member-management.md 읽기
   ```

2. **PRD 분석**:
   - 요구사항 추출
   - 구현 범위 파악
   - 테스트 대상 식별
   - Layer별 작업 분류

3. **Plan 파일명 생성**:
   - PRD 파일명에서 추출: `member-management.md` → `MEMBER-001`
   - 출력 위치: `docs/prd/plans/MEMBER-001-{layer}-plan.md`

#### Jira 기반 모드 (Jira 티켓 번호 또는 git 브랜치)

1. **Issue 번호 추출**:
   ```bash
   # 방법 1: 직접 지정 (사용자가 /tdd-plan PROJ-123)
   # 방법 2: git 브랜치에서 추출
   git branch --show-current
   # 예: feature/PROJ-123-order-aggregate → PROJ-123
   ```

2. **Jira MCP로 Issue 조회**:
   ```bash
   # Jira Issue 상세 정보 가져오기
   - Issue 제목
   - Issue 설명 (Description)
   - Issue Type (Story/Task/Bug)
   - Priority
   - Acceptance Criteria
   ```

3. **Issue 분석**:
   - 요구사항 추출
   - 구현 범위 파악
   - 테스트 대상 식별

### 2단계: Layer 자동 판별

Issue 설명에서 다음 키워드를 감지하여 필요한 Layer를 판별합니다:

#### Domain Layer 키워드
- `Aggregate`, `Entity`, `Value Object`, `Domain Event`
- `비즈니스 로직`, `도메인 모델`, `Factory Method`
- `DDD`, `Aggregate Root`

#### Application Layer 키워드
- `UseCase`, `Service`, `Command`, `Query`, `CQRS`
- `Transaction`, `Orchestration`, `Port Interface`
- `@Transactional`, `비즈니스 흐름`

#### Persistence Layer 키워드
- `Repository`, `JPA Entity`, `QueryDSL`
- `Database`, `Query Optimization`, `Long FK`
- `저장`, `조회`, `데이터베이스`

#### REST API Layer 키워드
- `Controller`, `API`, `REST`, `Endpoint`
- `Request`, `Response`, `DTO`, `Mapper`
- `HTTP`, `API 엔드포인트`

#### Integration Layer 키워드
- `E2E`, `Integration Test`, `TestRestTemplate`
- `Testcontainers`, `Full Spring Context`
- `통합 테스트`, `End-to-End`

**판별 규칙**:
- 모든 Layer 감지 → Full Stack TDD Plan 생성
- 일부만 감지 → 해당 Layer만 Plan 생성
- 감지 안됨 → 사용자에게 Layer 선택 요청

### 3단계: Layer별 TDD Plan 생성

감지된 Layer별로 다음 템플릿을 사용하여 TDD Plan을 생성합니다.

#### Domain Layer TDD Plan

```markdown
## 1️⃣ Domain Layer TDD

### Phase 1: /kb-domain /go (설계)
**목표**: Domain 모델 설계

**체크리스트**:
- [ ] Aggregate Root 설계 (예: `Order`, `Customer`)
- [ ] Value Objects 식별 (예: `OrderId`, `Money`)
- [ ] Domain Events 정의 (예: `OrderPlaced`, `OrderCancelled`)
- [ ] Business Methods 시그니처 정의
- [ ] Law of Demeter 준수 설계

**참고 문서**: [Domain Layer Guide](../../docs/coding_convention/02-domain-layer/domain-guide.md)

### Phase 2: /kb-domain /red (실패 테스트 작성)
**목표**: 테스트 먼저 작성 (실패 확인)

**체크리스트**:
- [ ] Aggregate 생성 테스트 작성 (Factory Method) - **실패 확인**
- [ ] Business Method 테스트 작성 - **실패 확인**
- [ ] Domain Event 발행 테스트 작성 - **실패 확인**
- [ ] Validation 테스트 작성 - **실패 확인**

**Domain Expert 스킬 활성화**: 자동

### Phase 3: /kb-domain /green (최소 구현)
**목표**: 테스트 통과하는 최소 구현

**체크리스트**:
- [ ] Aggregate Root 구현 (Factory Method 패턴)
- [ ] Value Objects 구현 (Java 21 Record)
- [ ] Business Logic 구현 (Tell, Don't Ask)
- [ ] Domain Event 발행
- [ ] 모든 테스트 통과 확인 ✅

**Zero-Tolerance 규칙**:
- ✅ No Lombok (Pure Java getter/setter)
- ✅ No Getter 체이닝
- ✅ Factory Method 필수
- ✅ Encapsulation 엄격

### Phase 4: /kb-domain /refactor (리팩토링)
**목표**: 코드 품질 개선 (테스트 유지)

**체크리스트**:
- [ ] Law of Demeter 검증 (no `obj.getX().getY()`)
- [ ] Tell Don't Ask 적용
- [ ] Encapsulation 강화 (Business Method로 상태 변경)
- [ ] Domain Event 발행 위치 최적화
- [ ] 테스트 여전히 통과 확인 ✅

### Phase 5: /kb-domain /tidy (최종 검증)
**목표**: Zero-Tolerance 규칙 최종 검증

**체크리스트**:
- [ ] validation-helper.py 자동 검증 통과
- [ ] No Lombok 확인
- [ ] No JPA 어노테이션 확인
- [ ] Javadoc 작성 (`@author`, `@since`)
- [ ] Domain Layer ArchUnit 테스트 통과
```

#### Application Layer TDD Plan

```markdown
## 2️⃣ Application Layer TDD

### Phase 1: /kb-application /go (설계)
**목표**: UseCase 인터페이스 설계

**체크리스트**:
- [ ] UseCase 인터페이스 정의 (Port In)
- [ ] Command/Query 분리 (CQRS)
- [ ] Inner Record DTO 설계 (Command, Response)
- [ ] Port Out 인터페이스 정의
- [ ] Transaction 경계 계획

**참고 문서**: [Application Layer Guide](../../docs/coding_convention/03-application-layer/application-guide.md)

### Phase 2: /kb-application /red (실패 테스트 작성)
**목표**: UseCase 테스트 작성 (실패 확인)

**체크리스트**:
- [ ] UseCase 실행 테스트 작성 - **실패 확인**
- [ ] Command Validation 테스트 작성
- [ ] Transaction Rollback 테스트 작성
- [ ] Port Out Mock 설정

**Application Expert 스킬 활성화**: 자동

### Phase 3: /kb-application /green (최소 구현)
**목표**: UseCase 구현

**체크리스트**:
- [ ] UseCase 구현체 작성 (`@Component`)
- [ ] `@Transactional` 적용 (Command UseCase)
- [ ] Port Out 의존성 주입 (Constructor Injection)
- [ ] Domain 로직 위임
- [ ] 모든 테스트 통과 확인 ✅

**Zero-Tolerance 규칙**:
- ✅ @Transactional 필수 (Command)
- ✅ Port 인터페이스 의존
- ✅ Command/Query 분리
- ✅ Inner Record DTO

### Phase 4: /kb-application /refactor (리팩토링)
**목표**: Transaction 경계 최적화

**체크리스트**:
- [ ] Transaction 경계 재검증 (외부 API 호출 분리)
- [ ] Domain 로직 위임 확인
- [ ] Port In/Out 분리 검증
- [ ] 테스트 여전히 통과 확인 ✅

### Phase 5: /kb-application /tidy (최종 검증)
**목표**: Transaction 경계 최종 검증

**체크리스트**:
- [ ] Git pre-commit hook 통과 (Transaction 경계)
- [ ] @Transactional 내 외부 API 호출 없음
- [ ] Application Layer ArchUnit 테스트 통과
- [ ] Javadoc 작성
```

#### Persistence Layer TDD Plan

```markdown
## 3️⃣ Persistence Layer TDD

### Phase 1: /kb-persistence /go (설계)
**목표**: Command/Query Adapter 설계

**체크리스트**:
- [ ] Command Adapter 설계 (SavePort)
- [ ] Query Adapter 설계 (LoadPort)
- [ ] JPA Entity 설계 (Long FK 전략)
- [ ] QueryDSL DTO Projection 설계

**참고 문서**: [Persistence Layer Guide](../../docs/coding_convention/04-persistence-layer/persistence-guide.md)

### Phase 2: /kb-persistence /red (실패 테스트 작성)
**목표**: Persistence 테스트 작성

**체크리스트**:
- [ ] Command Adapter 저장 테스트 - **실패 확인**
- [ ] Query Adapter 조회 테스트 - **실패 확인**
- [ ] QueryDSL DTO Projection 테스트 - **실패 확인**

**Persistence Expert 스킬 활성화**: 자동

### Phase 3: /kb-persistence /green (최소 구현)
**목표**: Adapter 및 Entity 구현

**체크리스트**:
- [ ] JpaEntity 구현 (Long FK, BaseAuditEntity 상속)
- [ ] Command Adapter 구현
- [ ] Query Adapter 구현
- [ ] QueryDSL Repository 구현
- [ ] 모든 테스트 통과 확인 ✅

**Zero-Tolerance 규칙**:
- ✅ Long FK 전략 (no 관계 어노테이션)
- ✅ Command/Query Adapter 분리
- ✅ QueryDSL DTO Projection
- ✅ No N+1 (fetch join)

### Phase 4: /kb-persistence /refactor (리팩토링)
**목표**: 쿼리 최적화

**체크리스트**:
- [ ] N+1 쿼리 제거 (fetch join)
- [ ] DTO Projection 최적화
- [ ] Index 전략 검토
- [ ] 테스트 여전히 통과 확인 ✅

### Phase 5: /kb-persistence /tidy (최종 검증)
**목표**: Long FK 전략 최종 검증

**체크리스트**:
- [ ] No JPA 관계 어노테이션 확인
- [ ] BaseAuditEntity 상속 확인
- [ ] Constructor Pattern 확인
- [ ] Persistence Layer ArchUnit 테스트 통과
- [ ] Flyway Migration 작성
```

#### REST API Layer TDD Plan

```markdown
## 4️⃣ REST API Layer TDD

### Phase 1: /kb-rest-api /go (설계)
**목표**: API 엔드포인트 설계

**체크리스트**:
- [ ] API 엔드포인트 설계 (RESTful)
- [ ] Request DTO 설계 (Command)
- [ ] Response DTO 설계
- [ ] ApiMapper 설계

**참고 문서**: [REST API Guide](../../docs/coding_convention/01-adapter-in-layer/rest-api/rest-api-guide.md)

### Phase 2: /kb-rest-api /red (실패 테스트 작성)
**목표**: Controller 테스트 작성

**체크리스트**:
- [ ] Controller 테스트 작성 (TestRestTemplate) - **실패 확인**
- [ ] Request Validation 테스트 작성
- [ ] GlobalExceptionHandler 테스트 작성

**REST API Expert 스킬 활성화**: 자동

### Phase 3: /kb-rest-api /green (최소 구현)
**목표**: Controller 구현

**체크리스트**:
- [ ] Controller 구현 (`@RestController`)
- [ ] Request DTO 구현 (`@Valid`)
- [ ] Response DTO 구현
- [ ] ApiMapper 구현
- [ ] 모든 테스트 통과 확인 ✅

**Zero-Tolerance 규칙**:
- ✅ TestRestTemplate 필수 (MockMvc 금지)
- ✅ Request/Response DTO 분리
- ✅ @Valid 필수
- ✅ ResponseEntity 반환

### Phase 4: /kb-rest-api /refactor (리팩토링)
**목표**: API 설계 개선

**체크리스트**:
- [ ] GlobalExceptionHandler 적용
- [ ] ResponseEntity 반환 검증
- [ ] Mapper 분리 확인
- [ ] 테스트 여전히 통과 확인 ✅

### Phase 5: /kb-rest-api /tidy (최종 검증)
**목표**: REST API 규칙 최종 검증

**체크리스트**:
- [ ] No MockMvc 확인
- [ ] No Domain Entity 직접 반환 확인
- [ ] No Business 로직 확인
- [ ] REST API Layer ArchUnit 테스트 통과
- [ ] Swagger/OpenAPI 문서 생성
```

#### Integration Layer TDD Plan

```markdown
## 5️⃣ Integration TDD

### Phase 1: /kb-integration /go (설계)
**목표**: E2E 시나리오 설계

**체크리스트**:
- [ ] E2E Test 시나리오 작성
- [ ] IntegrationTestFixture 설계
- [ ] Flyway Migration 준비 (DDL)
- [ ] @Sql Test Data 준비 (DML)

**참고 문서**: [Integration Testing Guide](../../docs/coding_convention/05-testing/integration-testing/01_integration-testing-overview.md)

### Phase 2: /kb-integration /red (실패 테스트 작성)
**목표**: E2E 테스트 작성

**체크리스트**:
- [ ] E2E 테스트 작성 (TestRestTemplate) - **실패 확인**
- [ ] IntegrationTestSupport 상속
- [ ] @Sql Test Data 로드

**Integration Test Expert 스킬 활성화**: 자동

### Phase 3: /kb-integration /green (최소 구현)
**목표**: E2E 테스트 통과

**체크리스트**:
- [ ] 전체 Layer 통합 확인
- [ ] TestRestTemplate 실제 HTTP 요청
- [ ] Testcontainers 설정 (Real Database)
- [ ] 모든 E2E 테스트 통과 확인 ✅

**Zero-Tolerance 규칙**:
- ✅ @SpringBootTest(RANDOM_PORT)
- ✅ TestRestTemplate 필수
- ✅ Flyway (DDL), @Sql (DML) 분리
- ✅ @Testcontainers (Real DB)

### Phase 4: /kb-integration /refactor (리팩토링)
**목표**: 테스트 격리 검증

**체크리스트**:
- [ ] @Transactional + @Rollback 적용
- [ ] 테스트 간 데이터 격리 확인
- [ ] 테스트 여전히 통과 확인 ✅

### Phase 5: /kb-integration /tidy (최종 검증)
**목표**: Integration 규칙 최종 검증

**체크리스트**:
- [ ] No DDL in @Sql 확인
- [ ] No MockMvc 확인
- [ ] Full Spring Context 확인
- [ ] Integration Layer ArchUnit 테스트 통과
```

### 4단계: TDD Plan 출력

다음 형식으로 Markdown TDD Plan을 생성합니다:

```markdown
# 🎯 TDD Plan: [JIRA-123] {Issue 제목}

## 📋 Issue 요약
- **Jira Key**: PROJ-123
- **Type**: Story/Task/Bug
- **Priority**: High/Medium/Low
- **감지된 Layer**: Domain, Application, Persistence, REST API, Integration

## 🎯 주요 요구사항
- [요구사항 1]
- [요구사항 2]
- [요구사항 3]

## 🏗️ Layer별 TDD 계획

[위에서 정의한 Layer별 템플릿 삽입]

## ✅ 최종 검증 체크리스트

### 자동 검증 (3-Tier)
- [ ] **Tier 1**: validation-helper.py 실시간 검증 통과
- [ ] **Tier 2**: Git pre-commit hooks 통과
- [ ] **Tier 3**: ArchUnit 테스트 통과 (빌드 시)

### 수동 검증
- [ ] /validate-architecture 실행
- [ ] Zero-Tolerance 규칙 준수 확인
- [ ] Javadoc 작성 확인

### PR 생성 준비
- [ ] gh pr create 실행
- [ ] PR Description 작성
- [ ] AI Review 요청 (/ai-review)

---

**💡 Tip**: 각 Layer별로 `/kb-<layer> /go|red|green|refactor|tidy` 순서로 진행하세요!
```

## 예시 출력

```markdown
# 🎯 TDD Plan: [PROJ-123] Order Aggregate 개발

## 📋 Issue 요약
- **Jira Key**: PROJ-123
- **Type**: Story
- **Priority**: High
- **감지된 Layer**: Domain, Application, Persistence, REST API, Integration

## 🎯 주요 요구사항
- Order Aggregate 생성 (주문 생성, 취소, 상태 변경)
- PlaceOrderUseCase 구현
- OrderCommandAdapter 구현
- OrderCommandController 구현
- E2E 테스트 작성

## 🏗️ Layer별 TDD 계획

### 1️⃣ Domain Layer TDD
[Domain Layer 5단계 체크리스트]

### 2️⃣ Application Layer TDD
[Application Layer 5단계 체크리스트]

### 3️⃣ Persistence Layer TDD
[Persistence Layer 5단계 체크리스트]

### 4️⃣ REST API Layer TDD
[REST API Layer 5단계 체크리스트]

### 5️⃣ Integration TDD
[Integration Layer 5단계 체크리스트]

## ✅ 최종 검증 체크리스트
[검증 체크리스트]

---

**💡 Tip**: 각 Layer별로 `/kb-<layer> /go|red|green|refactor|tidy` 순서로 진행하세요!
```

## 실행

### PRD 기반 실행

```bash
/tdd-plan --prd docs/prd/member-management.md
```

1. PRD 파일 읽기
2. PRD 분석 → Layer 자동 판별
3. 위 템플릿 기반 TDD Plan 자동 생성
4. `docs/prd/plans/MEMBER-001-{layer}-plan.md` 형식으로 출력

### Jira 기반 실행

```bash
# 방법 1: 티켓 번호 직접 지정
/tdd-plan PROJ-123

# 방법 2: git 브랜치에서 자동 추출
/tdd-plan
```

1. Jira Issue 번호 추출 (직접 또는 브랜치에서)
2. Jira MCP로 Issue 상세 조회
3. Issue 설명 분석 → Layer 자동 판별
4. 위 템플릿 기반 TDD Plan 자동 생성
5. `docs/prd/plans/{ISSUE-KEY}-{layer}-plan.md` 형식으로 출력

**시작하세요!** 🚀
