# All Layers Coding Convention Validation

**목적**: TDD로 작성된 모든 레이어의 코드가 프로젝트 코딩 컨벤션을 준수하는지 통합 검증하고, 통합 리팩토링 PRD를 생성합니다.

---

## 🎯 검증 범위

### 전체 레이어 통합 검증

```
claude-spring-standards/
├── domain/                → /cc/domain/validate
├── application/           → /cc/application/validate
├── persistence-mysql/     → /cc/persistence/validate
├── adapter-in-rest/       → /cc/rest-api/validate
└── 통합 리포트 + PRD 생성
```

---

## 🔍 검증 프로세스

### 1단계: 레이어별 검증 실행

```markdown
**실행 순서**: Domain → Application → Persistence → REST API

이유: 의존성 방향을 따라 검증
- Domain (가장 내부)
- Application (Domain 의존)
- Persistence (Domain 의존)
- REST API (Application 의존)
```

### 검증 워크플로우

```bash
# 1. Domain Layer 검증
/cc/domain/validate
→ Lombok 금지, Law of Demeter, Tell Don't Ask
→ 위반 항목 수집

# 2. Application Layer 검증
/cc/application/validate
→ Transaction 경계, CQRS 분리, Assembler 패턴
→ 위반 항목 수집

# 3. Persistence Layer 검증
/cc/persistence/validate
→ Long FK 전략, QueryDSL DTO Projection, Lombok 금지
→ 위반 항목 수집

# 4. REST API Layer 검증
/cc/rest-api/validate
→ RESTful 설계, MockMvc 금지, Validation 필수
→ 위반 항목 수집

# 5. 통합 리포트 생성
→ 모든 위반 항목 통합
→ 우선순위별 정렬
→ 리팩토링 PRD 생성
```

---

## 📊 통합 검증 결과 리포트

### 리포트 형식

```markdown
# 전체 레이어 코딩 컨벤션 검증 결과

**프로젝트**: claude-spring-standards
**검증 날짜**: {검증 실행 날짜}
**검증 범위**: 전체 레이어 (Domain, Application, Persistence, REST API)

---

## 📈 검증 요약

| 레이어 | 총 위반 건수 | Zero-Tolerance 위반 | 심각도 HIGH | 심각도 MEDIUM | 심각도 LOW |
|--------|-------------|---------------------|-------------|---------------|------------|
| Domain | 23건 | 6건 | 6건 | 12건 | 5건 |
| Application | 26건 | 4건 | 4건 | 15건 | 7건 |
| Persistence | 30건 | 13건 | 13건 | 10건 | 7건 |
| REST API | 41건 | 16건 | 16건 | 18건 | 7건 |
| **총계** | **120건** | **39건** | **39건** | **55건** | **26건** |

---

## 🔴 Zero-Tolerance 위반 항목 (즉시 수정 필수)

### Domain Layer (6건)
1. **Law of Demeter 위반** (3건)
   - `Order.java:45` - Getter 체이닝
   - `Customer.java:23` - Getter 체이닝
   - `Payment.java:67` - Getter 체이닝

2. **불변성 위반** (2건)
   - `Money.java:12` - non-final 필드
   - `Email.java:8` - non-final 필드

3. **Lombok 사용** (1건)
   - `Order.java:8` - @Getter 사용

### Application Layer (4건)
1. **Transaction 경계 위반** (2건)
   - `CreateOrderUseCase.java:12` - UseCase에 @Transactional
   - `UpdateOrderUseCase.java:18` - UseCase에 @Transactional

2. **외부 API Transaction 내부 호출** (1건)
   - `OrderTransactionManager.java:34` - paymentClient 호출

3. **Spring 프록시 제약 위반** (1건)
   - `OrderService.java:56` - private 메서드에 @Transactional

### Persistence Layer (13건)
1. **JPA 관계 어노테이션 사용** (3건)
   - `OrderJpaEntity.java:23` - @ManyToOne
   - `CustomerJpaEntity.java:45` - @OneToMany
   - `PaymentJpaEntity.java:12` - @ManyToOne

2. **Entity 직접 반환** (4건)
   - `OrderQueryDslRepository.java:15` - List<OrderJpaEntity> 반환
   - `CustomerQueryDslRepository.java:28` - List<CustomerJpaEntity> 반환
   - `PaymentQueryDslRepository.java:33` - List<PaymentJpaEntity> 반환
   - `ProductQueryDslRepository.java:19` - List<ProductJpaEntity> 반환

3. **Lombok 사용** (6건)
   - `OrderJpaEntity.java:8` - @Getter
   - `CustomerJpaEntity.java:10` - @NoArgsConstructor
   - `PaymentJpaEntity.java:7` - @Getter
   - `ProductJpaEntity.java:9` - @Getter
   - `OrderPersistenceMapper.java:5` - @RequiredArgsConstructor
   - `CustomerPersistenceMapper.java:6` - @RequiredArgsConstructor

### REST API Layer (16건)
1. **MockMvc 사용** (8건)
   - `OrderRestControllerTest.java:15` - @WebMvcTest
   - `CustomerRestControllerTest.java:18` - mockMvc.perform()
   - `PaymentRestControllerTest.java:20` - @AutoConfigureMockMvc
   - (총 8개 테스트 클래스)

2. **상태 코드 오류** (5건)
   - `OrderRestController.java:23` - POST → 200 (201이어야 함)
   - `CustomerRestController.java:34` - DELETE → 200 (204이어야 함)
   - `PaymentRestController.java:45` - 검증 실패 → 500 (400이어야 함)
   - (총 5개 엔드포인트)

3. **@Valid 누락** (3건)
   - `OrderRestController.java:34` - @RequestBody에 @Valid 없음
   - `CustomerRestController.java:56` - @RequestBody에 @Valid 없음
   - `PaymentRestController.java:23` - @RequestBody에 @Valid 없음

---

## 🟡 권장 수정 항목

### Domain Layer (12건)
- Tell Don't Ask 패턴 적용 (5건)
- 테스트 누락 (2건)
- 생성자 접근 제어자 조정 (5건)

### Application Layer (15건)
- Assembler 패턴 적용 (4건)
- CQRS 분리 강화 (3건)
- Port 네이밍 규칙 준수 (8건)

### Persistence Layer (10건)
- Mapper 패턴 적용 (5건)
- N+1 방지 (Fetch Join) (3건)
- Flyway 마이그레이션 추가 (2건)

### REST API Layer (18건)
- ErrorMapper 패턴 적용 (4건)
- RequestMapper 패턴 적용 (6건)
- REST Docs 추가 (8건)

---

## 📋 통합 리팩토링 우선순위

### Priority 1: CRITICAL (즉시 수정 필수) - 39건

**예상 소요 시간**: 약 16시간 (39건 × 25분)

#### 1.1 Persistence Layer (13건) - 5.5시간
- JPA 관계 어노테이션 제거 + Flyway 마이그레이션 (3건)
- Entity 직접 반환 → DTO Projection (4건)
- Lombok 제거 (6건)

#### 1.2 REST API Layer (16건) - 6시간
- MockMvc → TestRestTemplate 전환 (8건)
- 상태 코드 수정 (5건)
- @Valid 추가 (3건)

#### 1.3 Domain Layer (6건) - 2.5시간
- Law of Demeter 위반 해결 (3건)
- 불변성 보장 (2건)
- Lombok 제거 (1건)

#### 1.4 Application Layer (4건) - 2시간
- Transaction 경계 수정 (2건)
- 외부 API Transaction 외부로 이동 (1건)
- Spring 프록시 제약 준수 (1건)

---

### Priority 2: HIGH (권장 수정) - 55건

**예상 소요 시간**: 약 18시간 (55건 × 20분)

#### 2.1 Application Layer (15건) - 5시간
#### 2.2 REST API Layer (18건) - 6시간
#### 2.3 Domain Layer (12건) - 4시간
#### 2.4 Persistence Layer (10건) - 3시간

---

### Priority 3: MEDIUM (선택 개선) - 26건

**예상 소요 시간**: 약 6.5시간 (26건 × 15분)

---

## 🎯 통합 리팩토링 PRD 생성

### PRD 생성 조건 (충족됨)

```yaml
auto_generate_prd:
  conditions:
    - zero_tolerance_violations: 39건 (> 0) ✅
    - total_violations: 120건 (> 10) ✅
    - severity_critical_count: 39건 (> 3) ✅
    - multiple_layers_affected: 4개 레이어 (> 1) ✅

  result: PRD 생성 필수
  location: "docs/prd/refactoring/REFACTOR-ALL-001-integrated-refactoring.md"
```

---

## 📝 통합 리팩토링 PRD 템플릿

```markdown
# 통합 리팩토링 PRD

**이슈 키**: REFACTOR-ALL-001
**생성 날짜**: {생성 날짜}
**우선순위**: CRITICAL
**예상 소요 시간**: 약 40.5시간

---

## 📋 리팩토링 개요

**목적**: 전체 레이어 코딩 컨벤션 위반 사항 해결
**범위**: Domain, Application, Persistence, REST API
**총 위반 항목**: 120건
**Zero-Tolerance 위반**: 39건

---

## 🎯 리팩토링 전략

### 전략 1: 레이어 순서 (Bottom-Up)

**이유**: 의존성 방향을 따라 안전하게 리팩토링

```
1. Domain Layer (가장 내부)
   → 다른 레이어에 영향 최소화

2. Persistence Layer
   → Domain 의존, Long FK 전략 적용

3. Application Layer
   → Domain, Persistence 의존, Transaction 경계

4. REST API Layer (가장 외부)
   → Application 의존, API 변경 최소화
```

### 전략 2: Zero-Tolerance 우선

**이유**: 심각도 높은 위반부터 해결

```
Priority 1 (CRITICAL): 39건
→ 각 레이어별 Zero-Tolerance 위반 먼저 해결

Priority 2 (HIGH): 55건
→ Zero-Tolerance 완료 후 권장 사항 적용

Priority 3 (MEDIUM): 26건
→ 여유 시간에 선택적 개선
```

### 전략 3: Parallel Tasks (병렬 작업)

**가능한 병렬 작업**:
- Domain Layer Law of Demeter (독립)
- Persistence Layer Long FK 전략 (독립)
- REST API Layer MockMvc 전환 (독립)

**순차 작업**:
- Application Layer Transaction 경계 → Persistence Layer 완료 후
- REST API Layer ErrorMapper → Domain Exception 완료 후

---

## 📝 상세 리팩토링 계획

### Phase 1: Domain Layer (6건) - 2.5시간

**Task 1.1: Law of Demeter 위반 해결 (3건)**

**파일**: `Order.java:45`, `Customer.java:23`, `Payment.java:67`

**TDD 사이클**:
```
1. struct: Getter 체이닝 제거 (Tell Don't Ask 패턴)
2. test: 새 메서드 테스트 추가
3. feat: 도메인 메서드 구현
```

**Task 1.2: 불변성 보장 (2건)**

**파일**: `Money.java:12`, `Email.java:8`

**TDD 사이클**:
```
1. struct: 필드 final 선언 (동작 변경 없음)
```

**Task 1.3: Lombok 제거 (1건)**

**파일**: `Order.java:8`

**TDD 사이클**:
```
1. struct: @Getter 제거, Getter 직접 구현
```

---

### Phase 2: Persistence Layer (13건) - 5.5시간

**Task 2.1: JPA 관계 어노테이션 제거 (3건)**

**파일**: `OrderJpaEntity.java:23`, 기타

**TDD 사이클**:
```
1. struct: Flyway 마이그레이션 작성 (스키마 변경)
2. struct: @ManyToOne 제거, Long FK로 변경
3. test: Long FK 조회 테스트
4. feat: Repository 수정 완료
```

**Task 2.2: Entity 직접 반환 → DTO Projection (4건)**

**파일**: `OrderQueryDslRepository.java:15`, 기타

**TDD 사이클**:
```
1. struct: QueryDto record 생성
2. test: DTO Projection 테스트
3. feat: Projections.constructor() 적용
```

**Task 2.3: Lombok 제거 (6건)**

**파일**: 모든 Entity, Mapper

**TDD 사이클**:
```
1. struct: Lombok 제거, Getter/생성자 직접 구현
```

---

### Phase 3: Application Layer (4건) - 2시간

**Task 3.1: Transaction 경계 수정 (2건)**

**파일**: `CreateOrderUseCase.java:12`, 기타

**TDD 사이클**:
```
1. struct: @Transactional을 TransactionManager로 이동
2. test: TransactionManager 트랜잭션 경계 테스트
3. feat: OrderTransactionManager 구현
```

**Task 3.2: 외부 API Transaction 외부 호출 (1건)**

**파일**: `OrderTransactionManager.java:34`

**TDD 사이클**:
```
1. struct: Transaction 경계 분리 (메서드 추출)
2. test: 외부 API 호출 순서 검증
3. feat: Transaction 외부/내부 로직 분리
```

---

### Phase 4: REST API Layer (16건) - 6시간

**Task 4.1: MockMvc → TestRestTemplate 전환 (8건)**

**파일**: 모든 RestControllerTest

**TDD 사이클**:
```
1. struct: @WebMvcTest → @SpringBootTest 전환
2. test: TestRestTemplate로 E2E 테스트 재작성
3. feat: 모든 테스트 통과 확인
```

**Task 4.2: 상태 코드 수정 (5건)**

**파일**: 각 RestController

**TDD 사이클**:
```
1. struct: ResponseEntity 상태 코드 수정
2. test: 상태 코드 검증 테스트 추가
3. feat: 테스트 통과 확인
```

**Task 4.3: @Valid 추가 (3건)**

**파일**: 각 RestController

**TDD 사이클**:
```
1. struct: @RequestBody에 @Valid 추가
2. test: 검증 실패 시 400 반환 테스트
3. feat: GlobalExceptionHandler 구현
```

---

## ✅ 완료 조건

### Definition of Done

#### 레이어별 완료 조건
- [ ] Domain: 모든 Zero-Tolerance 위반 해결
- [ ] Persistence: Long FK 전략 완료, Lombok 제거
- [ ] Application: Transaction 경계 수정 완료
- [ ] REST API: MockMvc 제거, 상태 코드 정확

#### 전체 완료 조건
- [ ] ArchUnit 테스트 전체 통과
- [ ] 단위 테스트 전체 통과 (95% 이상)
- [ ] 통합 테스트 전체 통과
- [ ] REST Docs 빌드 성공
- [ ] 코딩 컨벤션 재검증 (위반 0건)

### 검증 방법

```bash
# 전체 ArchUnit 실행
./gradlew test --tests "*ArchitectureTest"

# 전체 테스트 실행
./gradlew test

# REST Docs 빌드
./gradlew asciidoctor

# 전체 레이어 재검증
/cc/validate-all
```

---

## 📊 예상 메트릭

### 커밋 수 예상
- **Priority 1**: 39건 × 2.5 = 약 98 커밋 (TDD 사이클 + Tidy First)
- **Priority 2**: 55건 × 2 = 약 110 커밋
- **Priority 3**: 26건 × 1.5 = 약 39 커밋
- **총 예상 커밋**: 약 247 커밋

### 소요 시간 예상
- **Priority 1 (CRITICAL)**: 16시간
- **Priority 2 (HIGH)**: 18시간
- **Priority 3 (MEDIUM)**: 6.5시간
- **총 예상 시간**: 약 40.5시간 (1주일)

### Phase별 분배
- **Week 1 - Day 1-2**: Domain Layer + Persistence Layer (8시간)
- **Week 1 - Day 3-4**: Application Layer + REST API Layer (8시간)
- **Week 1 - Day 5**: Priority 2 시작 (8시간)
- **Week 2 - Day 1-2**: Priority 2 완료 (10시간)
- **Week 2 - Day 3**: Priority 3 (선택) (6.5시간)

---

## 🔄 TDD + Tidy First 워크플로우

### 각 Task마다 반복

```
1️⃣ Structural Changes (필요 시)
   ├─ struct: 리네이밍, 메서드 추출, Flyway 마이그레이션
   ├─ 테스트 통과 확인 (동작 변경 없음)
   └─ struct: 커밋

2️⃣ Red: 테스트 작성
   ├─ test: 실패하는 테스트 작성
   ├─ 컴파일 에러 또는 테스트 실패 확인
   └─ test: 커밋

3️⃣ Green: 최소 구현
   ├─ feat: 테스트 통과할 만큼만 구현
   ├─ 테스트 통과 확인
   └─ feat: 커밋

4️⃣ Refactor: 구조 개선 (필요 시)
   ├─ struct: 중복 제거, 명확성 개선
   ├─ 테스트 통과 확인
   └─ struct: 커밋

5️⃣ LangFuse 메트릭 자동 수집
   └─ post-commit hook → TDD 사이클 추적
```

---

## 📌 참고 문서

- `docs/coding_convention/` (전체 레이어 규칙)
- `.claude/CLAUDE.md` (TDD + Tidy First 철학)
- `.claude/hooks/track-tdd-cycle.sh` (메트릭 수집)
- `.claude/scripts/log-to-langfuse.py` (LangFuse 업로드)
```

---

## 🛠️ 실행 방법

```bash
# 전체 레이어 통합 검증
/cc/validate-all

# 통합 리포트만 생성 (PRD 생성 안 함)
/cc/validate-all --report-only

# 강제 PRD 생성 (위반 건수 무관)
/cc/validate-all --force-prd

# 특정 Priority만 검증
/cc/validate-all --priority critical  # Priority 1만
/cc/validate-all --priority high      # Priority 1-2
/cc/validate-all --priority all       # 전체
```

---

## 🎯 검증 프로세스

1. **레이어별 검증 실행** (순차)
   - Domain Layer 검증
   - Application Layer 검증
   - Persistence Layer 검증
   - REST API Layer 검증

2. **위반 항목 통합**
   - 레이어별 위반 항목 수집
   - 심각도별 분류
   - 우선순위 정렬

3. **통합 리포트 생성**
   - 레이어별 요약 테이블
   - Zero-Tolerance 위반 리스트
   - 우선순위별 리팩토링 계획

4. **리팩토링 PRD 생성**
   - 조건 충족 시 자동 생성
   - Phase별 상세 계획
   - TDD 사이클 가이드

5. **메트릭 계산**
   - 예상 커밋 수
   - 예상 소요 시간
   - Phase별 분배 계획

---

## 📊 성공 지표

### 리팩토링 완료 후 목표

```yaml
zero_tolerance_violations: 0건 (현재: 39건)
total_violations: < 10건 (현재: 120건)
test_coverage: > 95% (Domain, Application)
archunit_pass_rate: 100%
rest_docs_coverage: 100% (모든 API 문서화)

tdd_metrics:
  commit_type_ratio:
    struct: 30-40%
    test: 25-30%
    feat: 25-30%
    fix: < 10%

  tdd_cycle_time:
    average: < 20분
    max: < 45분

  tidy_first_compliance: > 90%
```

---

## 🎓 학습 포인트

### 이 리팩토링을 통해 배우는 것

1. **Kent Beck TDD 사이클**
   - Red → Green → Refactor
   - 작은 커밋의 힘
   - 테스트 주도 개발의 리듬

2. **Tidy First 철학**
   - Structural vs Behavioral 분리
   - 구조 먼저, 기능 나중
   - 안전한 리팩토링

3. **코딩 컨벤션의 중요성**
   - Zero-Tolerance 규칙의 이유
   - 레이어별 책임 분리
   - 헥사고날 아키텍처 실천

4. **메트릭 기반 개선**
   - LangFuse 메트릭 활용
   - 커밋 타입 비율 분석
   - TDD 사이클 시간 최적화

---

## 📌 참고 문서

- `docs/coding_convention/` (전체 레이어 규칙)
- `.claude/CLAUDE.md` (프로젝트 철학)
- `/cc/domain/validate` (Domain Layer 검증)
- `/cc/application/validate` (Application Layer 검증)
- `/cc/persistence/validate` (Persistence Layer 검증)
- `/cc/rest-api/validate` (REST API Layer 검증)
