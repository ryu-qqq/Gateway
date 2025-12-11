# Apply Standards Command

기존 프로젝트에 Claude Spring Standards를 적용하는 커맨드입니다.

---

## Claude 실행 지침

이 커맨드가 실행되면 Claude는 다음 단계를 **순차적으로** 수행합니다:

### Step 1: 파라미터 파싱
```
$ARGUMENTS에서 추출:
- 첫 번째 인자: project-path (대상 프로젝트 경로)
- 두 번째 인자: package-name (새 패키지명)

예: "/apply-standards ~/projects/fileflow com.ryuqq.fileflow"
→ project-path: ~/projects/fileflow
→ package-name: com.ryuqq.fileflow
```

### Step 2: 스크립트 실행
```bash
# 현재 프로젝트(claude-spring-standards) 위치 확인
SOURCE_PROJECT=$(pwd)

# apply-standards.sh 스크립트 실행
bash scripts/apply-standards.sh {project-path} {package-name}
```

### Step 3: Serena MCP 분석
스크립트 완료 후, Serena MCP를 사용하여 대상 프로젝트 분석:
```
1. mcp__serena__activate_project → 대상 프로젝트 활성화
2. mcp__serena__search_for_pattern → Zero-Tolerance 위반 탐지
3. mcp__serena__write_memory → 분석 결과 저장
```

### Step 4: 결과 보고 및 다음 단계 제안
분석 결과를 사용자에게 보고하고 `/refactor-plan` 연계를 제안합니다.

---

## 명령어

```
/apply-standards {project-path} {package-name}
```

**파라미터:**
- `project-path`: 대상 프로젝트 경로 (절대 경로 또는 상대 경로)
- `package-name`: 새 패키지명 (예: `com.company.projectname`)

**예시:**
```bash
/apply-standards ~/projects/fileflow com.ryuqq.fileflow
/apply-standards ../my-project com.mycompany.myapp
```

---

## 실행 프로세스

```
┌─────────────────────────────────────────────────────────────┐
│              Apply Standards Process                         │
├─────────────────────────────────────────────────────────────┤
│  1️⃣ 프로젝트 검증 (Project Validation)                      │
│     └─ 대상 프로젝트 존재 확인 + Gradle 프로젝트 확인         │
│                                                              │
│  2️⃣ 기존 설정 백업 (Backup Existing)                        │
│     └─ .claude/, .serena/, docs/ 백업                       │
│                                                              │
│  3️⃣ 표준 적용 (Apply Standards)                             │
│     └─ 모든 설정 파일 복사 + 패키지명 치환                   │
│                                                              │
│  4️⃣ 현황 분석 (Current State Analysis)                      │
│     └─ Serena MCP로 위반 사항 탐지                          │
│                                                              │
│  5️⃣ 리팩토링 계획 제안 (Suggest Refactoring)                │
│     └─ /refactor-plan 연계                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 1️⃣ 프로젝트 검증

### 체크 항목

```markdown
## 프로젝트 검증
- [ ] 경로 존재 확인
- [ ] build.gradle 또는 build.gradle.kts 존재
- [ ] src/main/java 디렉토리 존재
- [ ] 기존 패키지 구조 확인
```

### 검증 실패 시

```
❌ 프로젝트 검증 실패

문제: {검증 실패 사유}
- Gradle 프로젝트가 아닙니다
- src/main/java 디렉토리가 없습니다

해결 방법:
1. 올바른 프로젝트 경로를 지정하세요
2. Gradle 프로젝트인지 확인하세요
```

---

## 2️⃣ 기존 설정 백업

### 백업 대상

```markdown
## 백업 항목
- .claude/ → .claude.backup.{timestamp}/
- .serena/ → .serena.backup.{timestamp}/
- docs/coding_convention/ → docs/coding_convention.backup.{timestamp}/
```

### 백업 알림

```
📦 기존 설정 백업 완료

백업 위치:
- .claude.backup.20241205_143022/
- .serena.backup.20241205_143022/
- docs/coding_convention.backup.20241205_143022/

복원 방법:
mv .claude.backup.20241205_143022 .claude
```

---

## 3️⃣ 표준 적용

### 복사 항목

```markdown
## 적용 항목

### Claude 설정 (15 Skills, 13 Commands)
- [ ] .claude/CLAUDE.md
- [ ] .claude/commands/*.md
- [ ] .claude/skills/**/SKILL.md

### Serena MCP 설정 (5 Memories)
- [ ] .serena/project.yml
- [ ] .serena/memories/*.md

### 코딩 컨벤션 문서 (146개)
- [ ] docs/coding_convention/**/*.md
- [ ] docs/index.md

### ArchUnit 테스트 (49개)
- [ ] */src/test/java/**/architecture/**/*ArchTest.java

### Gradle 설정
- [ ] gradle/libs.versions.toml
- [ ] config/checkstyle/ (선택)
```

### 패키지명 치환

```
치환 대상: com.ryuqq → {새 패키지명}

적용 파일:
- ArchUnit 테스트 내 패키지 참조
- Serena Memory 내 예시 코드
- 설정 파일 내 패키지 참조
```

---

## 4️⃣ 현황 분석 (Serena MCP)

### 자동 분석 항목

표준 적용 후 Serena MCP를 사용하여 현재 코드베이스를 분석합니다:

```markdown
## Zero-Tolerance 위반 탐지

### 🔴 Critical
- Lombok 사용: `import lombok.` 검색
- Law of Demeter: `.get*().get*()` 패턴 검색
- JPA 관계 어노테이션: `@ManyToOne`, `@OneToMany` 검색
- Transaction 경계 위반: `@Transactional` 내 외부 호출

### 🟡 Important
- CQRS 미분리: Command/Query 혼재
- DTO 미분리: Entity 직접 반환
- MockMvc 사용: TestRestTemplate 권장
```

### 분석 결과 형식

```markdown
## 📊 현황 분석 결과

### 프로젝트 통계
- 총 파일 수: {count}개
- Java 파일: {count}개
- 테스트 파일: {count}개

### Zero-Tolerance 위반 현황
| 위반 유형 | 파일 수 | 심각도 |
|-----------|---------|--------|
| Lombok 사용 | 45개 | 🔴 Critical |
| JPA 관계 어노테이션 | 12개 | 🔴 Critical |
| Law of Demeter | 8개 | 🔴 Critical |
| CQRS 미분리 | 15개 | 🟡 Important |
| MockMvc 사용 | 23개 | 🟡 Important |

### 권장 조치
1. `/refactor-plan` 실행하여 상세 계획 수립
2. Phase 1부터 순차적으로 리팩토링 진행
```

---

## 5️⃣ 리팩토링 계획 연계

### 자동 연계

```
표준 적용 완료 후:

📋 다음 단계 제안:

1. ArchUnit 테스트 실행
   ./gradlew test --tests "*ArchTest*"

2. 위반 사항 상세 분석
   /refactor-plan

3. Phase별 리팩토링 진행
   - Phase 1: Lombok 제거, Long FK 전환
   - Phase 2: CQRS 분리, Port 인터페이스
   - Phase 3: DTO Record 전환, Assembler 추가
   - Phase 4: MockMvc → TestRestTemplate

리팩토링 계획을 수립할까요? [Y/N]
```

---

## 실행 예시

### 신규 프로젝트 적용

```bash
User: /apply-standards ~/projects/new-service com.mycompany.newservice

Claude: 🚀 Claude Spring Standards 적용을 시작합니다.

📍 대상: ~/projects/new-service
📦 패키지: com.mycompany.newservice

## 1️⃣ 프로젝트 검증
✅ Gradle 프로젝트 확인
✅ src/main/java 디렉토리 존재
✅ 기존 .claude 설정 없음

## 2️⃣ 표준 적용 중...
✅ Claude 설정 복사 완료 (15 Skills, 13 Commands)
✅ Serena MCP 설정 복사 완료 (5 Memories)
✅ 코딩 컨벤션 문서 복사 완료 (146개)
✅ ArchUnit 테스트 복사 완료 (49개)
✅ 패키지명 치환 완료 (com.ryuqq → com.mycompany.newservice)

## 3️⃣ 현황 분석 중...
[Serena MCP 분석 진행]

## 📊 분석 결과
- Lombok 사용: 32개 파일
- JPA 관계 어노테이션: 8개 파일
- CQRS 미분리: 12개 파일

## 📋 다음 단계
리팩토링 계획 수립을 권장합니다.
`/refactor-plan` 실행하시겠습니까?
```

### 기존 설정 업데이트

```bash
User: /apply-standards ~/projects/legacy-service com.company.legacy

Claude: 🚀 Claude Spring Standards 적용을 시작합니다.

⚠️ 기존 설정 발견

기존 설정:
- .claude/ (v2.0.0)
- .serena/ (5 memories)
- docs/coding_convention/ (120개 문서)

백업 후 최신 버전으로 업데이트할까요? [Y/N]

User: Y

Claude: 📦 기존 설정 백업 중...
✅ .claude.backup.20241205_143022/ 생성
✅ .serena.backup.20241205_143022/ 생성

## 업데이트 적용 중...
✅ Claude 설정 업데이트 (v2.0.0 → v3.0.0)
✅ Serena Memory 업데이트
✅ 코딩 컨벤션 문서 추가 (120 → 146개, +26)
✅ 신규 ArchUnit 테스트 추가 (+8개)

## 변경 사항 요약
- 신규 Skills: 3개 추가
- 신규 Commands: 2개 추가
- 문서 업데이트: 26개 추가
- ArchUnit 테스트: 8개 추가
```

---

## 적용 완료 후 체크리스트

```markdown
## ✅ 적용 완료 체크리스트

### 필수 검증
- [ ] `./gradlew build` 성공
- [ ] `./gradlew test --tests "*ArchTest*"` 실행 (위반 사항 확인)
- [ ] IDE에서 프로젝트 리로드

### 권장 작업
- [ ] `/refactor-plan` 실행하여 리팩토링 계획 수립
- [ ] README.md 업데이트 (컨벤션 적용 명시)
- [ ] 팀원 공유 및 가이드라인 전파

### 선택 작업
- [ ] CI/CD 파이프라인에 ArchUnit 테스트 추가
- [ ] pre-commit hook 설정
- [ ] Checkstyle 설정 적용
```

---

## 참조

- **표준 소스**: `claude-spring-standards` 프로젝트
- **적용 스크립트**: `scripts/apply-standards.sh`
- **리팩토링 분석**: `/refactor-plan` 커맨드
- **Skill 참조**: `.claude/skills/refactoring-analyst/SKILL.md`
