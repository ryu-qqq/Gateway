# /jira-start - Jira Issue 시작 + 브랜치 생성 + TDD Plan 준비

**목적**: Jira Issue를 시작하고 TDD 개발 환경 자동 설정

**사용법**:
```bash
/jira-start <issue-key>
/jira-start MEMBER-001
```

---

## 📋 작업 순서

### 1. Jira Issue 조회 및 검증

**Jira API 호출**:
```bash
GET /rest/api/3/issue/{issueKey}
Authorization: Basic {base64(email:api_token)}
```

**검증 항목**:
- ✅ Issue 존재 여부
- ✅ 현재 상태 (`To Do` 또는 `In Progress`)
- ✅ Assignee (미할당이면 자동 할당)
- ✅ Epic Link 존재

**에러 케이스**:
```
❌ Issue를 찾을 수 없음: MEMBER-001
❌ Issue가 이미 완료됨: MEMBER-001 (상태: Done)
❌ Issue가 다른 사람에게 할당됨: MEMBER-001 (Assignee: John Doe)
```

### 2. Layer 감지

**Task 파일에서 Layer 추출**:
```markdown
# MEMBER-001: Domain Layer 구현

**Layer**: Domain  ← 여기서 추출
```

**Layer → 브랜치명 매핑**:
- `Domain` → `feature/{ISSUE-KEY}-domain`
- `Application` → `feature/{ISSUE-KEY}-application`
- `Persistence` → `feature/{ISSUE-KEY}-persistence`
- `REST API` → `feature/{ISSUE-KEY}-rest-api`
- `Integration` → `feature/{ISSUE-KEY}-integration`

### 3. Git 브랜치 생성 및 체크아웃

**Git 작업**:
```bash
# 1. main 브랜치 최신화
git checkout main
git pull origin main

# 2. 브랜치 생성 및 체크아웃
git checkout -b feature/{ISSUE-KEY}-{layer}

# 3. 빈 커밋으로 브랜치 push (추적 설정)
git commit --allow-empty -m "chore: {ISSUE-KEY} 브랜치 시작"
git push -u origin feature/{ISSUE-KEY}-{layer}
```

**에러 처리**:
```bash
# 브랜치가 이미 존재하는 경우
if git show-ref --verify --quiet refs/heads/feature/{ISSUE-KEY}-{layer}; then
    echo "⚠️ 브랜치가 이미 존재합니다: feature/{ISSUE-KEY}-{layer}"
    echo "다음 중 선택하세요:"
    echo "  1. 기존 브랜치로 체크아웃 (git checkout feature/{ISSUE-KEY}-{layer})"
    echo "  2. 브랜치 삭제 후 재생성 (git branch -D feature/{ISSUE-KEY}-{layer})"
    # 사용자 입력 대기
fi
```

### 4. TDD Plan 자동 생성

**Plan 파일 존재 여부 확인**:
```bash
if [ ! -f "docs/prd/plans/{ISSUE-KEY}-{layer}-plan.md" ]; then
    echo "📋 TDD Plan 생성 중..."
    /create-plan {ISSUE-KEY}
else
    echo "✅ TDD Plan이 이미 존재합니다"
fi
```

### 5. Jira 상태 업데이트

**Jira Transition API 호출**:
```bash
POST /rest/api/3/issue/{issueKey}/transitions
Content-Type: application/json

{
  "transition": {
    "id": "21"  // To Do → In Progress (Transition ID)
  },
  "fields": {
    "assignee": {
      "accountId": "{current_user_account_id}"
    }
  }
}
```

**Transition ID 확인**:
```bash
# 사전 조회 (한 번만 실행)
GET /rest/api/3/issue/{issueKey}/transitions

# 응답 예시:
{
  "transitions": [
    {"id": "21", "name": "In Progress", "to": {"id": "3", "name": "In Progress"}},
    {"id": "31", "name": "Done", "to": {"id": "4", "name": "Done"}}
  ]
}
```

### 6. 로컬 Task 파일 업데이트

**Task 파일에 시작 정보 추가**:
```markdown
# MEMBER-001: Domain Layer 구현

**Epic**: 회원 관리 시스템
**Layer**: Domain
**브랜치**: feature/MEMBER-001-domain
**Jira URL**: https://your-domain.atlassian.net/browse/MEMBER-001
**상태**: In Progress  ← 추가
**시작일**: 2025-11-14  ← 추가
**담당자**: John Doe  ← 추가

---
```

---

## 🚀 실행 예시

### 정상 실행

**입력**:
```bash
/jira-start MEMBER-001
```

**출력**:
```
🔍 Jira Issue 조회 중...
   ✅ MEMBER-001: Domain Layer 구현
   └─ 상태: To Do
   └─ Epic: MEMBER (회원 관리 시스템)

📂 Task 파일 분석 중...
   ✅ docs/prd/tasks/MEMBER-001.md
   └─ Layer: Domain

🌿 Git 브랜치 생성 중...
   ✅ main 브랜치 최신화 완료
   ✅ 브랜치 생성: feature/MEMBER-001-domain
   ✅ 브랜치 push 완료

📋 TDD Plan 생성 중...
   ✅ docs/prd/plans/MEMBER-001-domain-plan.md
   └─ 총 5개 사이클 (예상 75분)

📡 Jira 상태 업데이트 중...
   ✅ To Do → In Progress
   ✅ Assignee: John Doe

📝 로컬 파일 업데이트 중...
   ✅ Task 파일에 시작 정보 추가

✅ 시작 준비 완료!

🔗 다음 단계:
   /kb/domain/go  (TDD 사이클 시작)
```

### 브랜치 이미 존재

**입력**:
```bash
/jira-start MEMBER-001
```

**출력**:
```
🔍 Jira Issue 조회 중...
   ✅ MEMBER-001: Domain Layer 구현

⚠️ 브랜치가 이미 존재합니다: feature/MEMBER-001-domain

다음 중 선택하세요:
  1. 기존 브랜치로 체크아웃 (계속 작업)
  2. 브랜치 삭제 후 재생성 (작업 초기화)
  3. 취소

선택 (1-3):
```

### Issue 이미 진행 중

**입력**:
```bash
/jira-start MEMBER-001
```

**출력**:
```
🔍 Jira Issue 조회 중...
   ✅ MEMBER-001: Domain Layer 구현
   └─ 상태: In Progress (이미 진행 중)
   └─ Assignee: John Doe

⚠️ 이 Issue는 이미 진행 중입니다.

브랜치로 체크아웃할까요?
  git checkout feature/MEMBER-001-domain

계속하시겠습니까? (y/N):
```

---

## ⚙️ Jira Transition 설정

### Transition ID 자동 감지

**스크립트 예시**:
```bash
#!/bin/bash
ISSUE_KEY=$1
JIRA_URL=$JIRA_URL
JIRA_AUTH=$(echo -n "$JIRA_EMAIL:$JIRA_API_TOKEN" | base64)

# 현재 가능한 Transition 조회
transitions=$(curl -s -X GET \
  -H "Authorization: Basic $JIRA_AUTH" \
  -H "Content-Type: application/json" \
  "$JIRA_URL/rest/api/3/issue/$ISSUE_KEY/transitions")

# "In Progress" Transition ID 추출
in_progress_id=$(echo $transitions | jq -r '.transitions[] | select(.name == "In Progress") | .id')

echo "In Progress Transition ID: $in_progress_id"
```

### 프로젝트별 Transition 매핑

**`.claude/jira-config.json`**:
```json
{
  "transitions": {
    "to_in_progress": "21",
    "to_in_review": "31",
    "to_done": "41"
  },
  "custom_fields": {
    "epic_link": "customfield_10014",
    "story_points": "customfield_10016"
  }
}
```

---

## 🔄 워크플로우 통합

### /jira-start 이후 흐름

```bash
# 1. Issue 시작
/jira-start MEMBER-001
→ 브랜치 생성: feature/MEMBER-001-domain
→ Plan 생성: docs/prd/plans/MEMBER-001-domain-plan.md
→ Jira 상태: In Progress

# 2. TDD 사이클 수행
/kb/domain/go
→ Plan 파일 읽고 TDD 실행
→ Red → Green → Refactor → Tidy
→ 자동 커밋

# 3. 완료 후 PR 생성
/jira-pr
→ PR 생성
→ Jira 상태: In Review

# 4. PR 머지 후 완료
/jira-done MEMBER-001
→ Jira 상태: Done
```

---

## ⚠️ 에러 처리

### Jira API 에러

**Issue 없음**:
```
❌ Jira Issue를 찾을 수 없습니다: MEMBER-001
   - Issue Key를 확인하세요
   - /sync-to-jira를 먼저 실행했는지 확인하세요
```

**권한 부족**:
```
❌ Issue 할당 권한이 없습니다
   - Jira 관리자에게 권한을 요청하세요
   - 또는 --skip-assign 옵션을 사용하세요
```

### Git 에러

**Uncommitted Changes**:
```
❌ 커밋되지 않은 변경사항이 있습니다
   - git status로 확인하세요
   - 변경사항을 커밋하거나 stash하세요
   - 또는 --force 옵션을 사용하세요
```

**Network 에러**:
```
❌ Remote repository에 연결할 수 없습니다
   - 네트워크 연결을 확인하세요
   - git remote -v로 remote 설정을 확인하세요
```

---

## 🎯 옵션 플래그

### --skip-jira

Jira 업데이트 없이 브랜치만 생성:
```bash
/jira-start MEMBER-001 --skip-jira
```

### --skip-plan

Plan 생성 건너뛰기:
```bash
/jira-start MEMBER-001 --skip-plan
```

### --force

기존 브랜치 강제 삭제 후 재생성:
```bash
/jira-start MEMBER-001 --force
```

---

## 🎯 핵심 원칙

1. **자동화**: 브랜치 생성 + Plan 생성 + Jira 업데이트를 한 번에
2. **안전성**: 기존 브랜치/작업 보호 (경고 + 사용자 선택)
3. **추적성**: Jira와 Git 상태 동기화
4. **편의성**: `/kb/{layer}/go` 바로 실행 가능하도록 환경 구성
5. **복구 가능성**: 모든 작업 커밋으로 추적 (빈 커밋 포함)

---

## 📚 관련 문서

- [Jira REST API - Transitions](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-transitions-post)
- [Git Branch Workflow](https://git-scm.com/book/en/v2/Git-Branching-Branching-Workflows)
- [Kent Beck TDD](http://www.kentbeck.com/tdd-by-example/)
