# /sync-to-jira - Tasks를 Jira Epic + Issues로 동기화

**목적**: 로컬 Task 파일들을 Jira Epic 및 Issues로 생성/업데이트

**사용법**:
```bash
/sync-to-jira <tasks-directory>
/sync-to-jira docs/prd/tasks/
```

---

## 📋 작업 순서

### 1. Task 파일들 읽기

**입력**:
- Tasks 디렉토리 (예: `docs/prd/tasks/`)

**읽을 파일**:
```
docs/prd/tasks/
├── MEMBER-001.md
├── MEMBER-002.md
├── MEMBER-003.md
├── MEMBER-004.md
└── MEMBER-005.md
```

**추출할 정보**:
- Epic 이름 (첫 번째 파일에서)
- Issue Key
- Issue 제목
- Layer
- 요구사항 체크리스트
- 제약사항

### 2. Jira Epic 생성

**Jira API 호출**:
```bash
POST /rest/api/3/issue
Content-Type: application/json
Authorization: Basic {base64(email:api_token)}

{
  "fields": {
    "project": {
      "key": "{JIRA_PROJECT_KEY}"
    },
    "summary": "{Epic 이름}",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            {
              "type": "text",
              "text": "{PRD 요약}"
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "Epic"
    },
    "customfield_10011": "{Epic 이름}"
  }
}
```

**응답**:
```json
{
  "id": "10001",
  "key": "MEMBER",
  "self": "https://your-domain.atlassian.net/rest/api/3/issue/10001"
}
```

### 3. Jira Issues 생성

**각 Task마다 Issue 생성**:

```bash
POST /rest/api/3/issue
Content-Type: application/json

{
  "fields": {
    "project": {
      "key": "{JIRA_PROJECT_KEY}"
    },
    "summary": "{Issue 제목}",
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "heading",
          "attrs": {"level": 2},
          "content": [{"type": "text", "text": "목적"}]
        },
        {
          "type": "paragraph",
          "content": [{"type": "text", "text": "{목적}"}]
        },
        {
          "type": "heading",
          "attrs": {"level": 2},
          "content": [{"type": "text", "text": "요구사항"}]
        },
        {
          "type": "bulletList",
          "content": [
            {
              "type": "listItem",
              "content": [
                {
                  "type": "paragraph",
                  "content": [{"type": "text", "text": "{요구사항 1}"}]
                }
              ]
            }
          ]
        }
      ]
    },
    "issuetype": {
      "name": "Task"
    },
    "customfield_10014": "{Epic Link}",
    "labels": ["{layer}", "tdd", "hexagonal"]
  }
}
```

### 4. 로컬 Task 파일 업데이트

**Jira URL 추가**:

```markdown
# MEMBER-001: Domain Layer 구현

**Epic**: 회원 관리 시스템
**Layer**: Domain
**브랜치**: feature/MEMBER-001-domain
**Jira URL**: https://your-domain.atlassian.net/browse/MEMBER-001  ← 추가

---
```

---

## ⚙️ Jira API 설정

### 환경 변수 설정

**`.claude/settings.local.json`**:
```json
{
  "env": {
    "JIRA_URL": "https://your-company.atlassian.net",
    "JIRA_EMAIL": "your-email@company.com",
    "JIRA_API_TOKEN": "your-jira-api-token",
    "JIRA_PROJECT_KEY": "MEMBER"
  }
}
```

**또는 `.env` 파일**:
```bash
JIRA_URL=https://your-company.atlassian.net
JIRA_EMAIL=your-email@company.com
JIRA_API_TOKEN=your-jira-api-token
JIRA_PROJECT_KEY=MEMBER
```

### Jira API Token 발급

1. Jira 로그인
2. Account Settings → Security
3. API tokens → Create API token
4. 토큰 복사하여 환경 변수에 설정

### Jira Custom Fields 확인

**Epic Link Field ID**:
```bash
curl -u email:token \
  https://your-domain.atlassian.net/rest/api/3/field \
  | jq '.[] | select(.name == "Epic Link")'

# 일반적으로 customfield_10014
```

---

## 🎯 Issue Type별 매핑

### Epic 생성

**조건**:
- 첫 번째 동기화 시 Epic이 없으면 생성
- Epic이 이미 있으면 업데이트

**Fields**:
```json
{
  "project": {"key": "MEMBER"},
  "summary": "회원 관리 시스템",
  "issuetype": {"name": "Epic"},
  "customfield_10011": "회원 관리 시스템",
  "labels": ["hexagonal", "tdd", "spring-boot"],
  "priority": {"name": "High"}
}
```

### Task 생성

**레이어별 Labels**:
- Domain Layer → `["domain", "tdd"]`
- Application Layer → `["application", "tdd"]`
- Persistence Layer → `["persistence", "tdd"]`
- REST API Layer → `["rest-api", "tdd"]`
- Integration Test → `["integration", "e2e"]`

**Fields**:
```json
{
  "project": {"key": "MEMBER"},
  "summary": "MEMBER-001: Domain Layer 구현",
  "issuetype": {"name": "Task"},
  "customfield_10014": "MEMBER",  // Epic Link
  "labels": ["domain", "tdd"],
  "priority": {"name": "Medium"},
  "assignee": {"accountId": "자동 할당 또는 null"}
}
```

---

## 📊 동기화 전략

### 초기 생성 모드

**조건**:
- Task 파일에 Jira URL이 없음

**작업**:
1. Epic 존재 여부 확인 (JQL 검색)
2. Epic 없으면 생성, 있으면 Epic Key 사용
3. 각 Task Issue 생성
4. 로컬 파일에 Jira URL 추가

### 업데이트 모드

**조건**:
- Task 파일에 Jira URL이 이미 있음

**작업**:
1. 기존 Jira Issue 조회 (Issue Key로)
2. 로컬 Task 내용과 비교
3. 변경 사항 있으면 Jira Issue 업데이트
4. 없으면 스킵

### 충돌 해결

**Jira가 최신인 경우**:
- 경고 메시지 출력
- 사용자 선택:
  - `--force-local`: 로컬로 덮어쓰기
  - `--force-jira`: Jira에서 가져오기
  - 기본값: 업데이트 스킵

---

## 🚀 실행 예시

### 기본 사용

**입력**:
```bash
/sync-to-jira docs/prd/tasks/
```

**출력**:
```
🔍 Task 파일 스캔 중...
   ├─ MEMBER-001.md (Domain Layer)
   ├─ MEMBER-002.md (Application Layer)
   ├─ MEMBER-003.md (Persistence Layer)
   ├─ MEMBER-004.md (REST API Layer)
   └─ MEMBER-005.md (Integration Test)

📡 Jira 연결 확인...
   ✅ https://your-company.atlassian.net 연결 성공

🎫 Epic 생성 중...
   ✅ Epic 생성 완료: MEMBER
   └─ https://your-company.atlassian.net/browse/MEMBER

🎫 Issues 생성 중...
   ✅ MEMBER-001 생성 완료
      └─ https://your-company.atlassian.net/browse/MEMBER-001
   ✅ MEMBER-002 생성 완료
      └─ https://your-company.atlassian.net/browse/MEMBER-002
   ✅ MEMBER-003 생성 완료
      └─ https://your-company.atlassian.net/browse/MEMBER-003
   ✅ MEMBER-004 생성 완료
      └─ https://your-company.atlassian.net/browse/MEMBER-004
   ✅ MEMBER-005 생성 완료
      └─ https://your-company.atlassian.net/browse/MEMBER-005

📝 로컬 파일 업데이트 중...
   ✅ 5개 Task 파일에 Jira URL 추가

✅ 동기화 완료!
   - Epic: 1개
   - Issues: 5개
   - 총 소요 시간: 3.2초

🔗 다음 단계:
   /jira-start MEMBER-001  (Issue 시작 + 브랜치 생성)
```

### 강제 업데이트 모드

**입력**:
```bash
/sync-to-jira docs/prd/tasks/ --force-local
```

**동작**:
- 로컬 Task 파일 내용으로 Jira Issue 강제 업데이트

### Dry-Run 모드

**입력**:
```bash
/sync-to-jira docs/prd/tasks/ --dry-run
```

**동작**:
- 실제 Jira 생성/업데이트 없이 미리보기만

---

## 🔄 양방향 동기화

### Jira → Local 동기화

**사용 케이스**:
- PM이 Jira에서 요구사항 수정
- 로컬 Task 파일을 최신으로 업데이트

**커맨드**:
```bash
/sync-from-jira MEMBER-001
```

**동작**:
1. Jira Issue 조회
2. 로컬 Task 파일 업데이트
3. 변경 사항 diff 출력

---

## ⚠️ 에러 처리

### Jira API 에러

**401 Unauthorized**:
```
❌ Jira 인증 실패
   - JIRA_EMAIL 또는 JIRA_API_TOKEN이 올바른지 확인하세요
   - API Token 발급: https://id.atlassian.com/manage/api-tokens
```

**403 Forbidden**:
```
❌ Jira 권한 부족
   - Project MEMBER에 대한 Issue 생성 권한이 필요합니다
   - Jira 관리자에게 권한을 요청하세요
```

**404 Not Found**:
```
❌ Jira Project를 찾을 수 없음
   - JIRA_PROJECT_KEY=MEMBER가 올바른지 확인하세요
```

### 네트워크 에러

**타임아웃**:
```
❌ Jira API 응답 시간 초과 (30초)
   - 네트워크 연결을 확인하세요
   - VPN 연결 여부를 확인하세요
```

---

## 🎯 핵심 원칙

1. **로컬 우선**: 로컬 Task 파일이 Single Source of Truth
2. **Jira는 동기화**: Jira는 팀 협업을 위한 미러
3. **충돌 방지**: 동시 수정 시 경고 및 사용자 선택
4. **추적 가능성**: 모든 동기화 작업 로깅
5. **환경 분리**: Jira 설정은 `.claude/settings.local.json`에만

---

## 📚 Jira API 참고 문서

- [Jira REST API v3 문서](https://developer.atlassian.com/cloud/jira/platform/rest/v3/intro/)
- [Issue 생성](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-post)
- [Epic 관리](https://developer.atlassian.com/cloud/jira/software/rest/api-group-epic/)
- [인증 방법](https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/)
