---
description: Plan 결과를 Jira Sub-task로 등록 + 브랜치 생성
tags: [project]
---

# /jira-register - Plan → Jira 등록 + 브랜치 생성

**목적**: `/plan` 결과의 구현 항목들을 Jira Sub-task로 등록하고, 작업 브랜치 생성

## 사용법

```bash
/jira-register {PARENT-ISSUE-KEY}
/jira-register MEMBER-123
```

---

## 실행 프로세스

```
/jira-register MEMBER-123
        ↓
┌─────────────────────────────────────────────────┐
│ 1️⃣ Plan 확인                                    │
│    - Serena Memory에서 최근 plan 로드            │
│    - 구현 항목 목록 추출                         │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 2️⃣ Jira Sub-task 생성                           │
│    - 각 Layer별 작업을 Sub-task로 등록           │
│    - Parent Issue에 연결                         │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 3️⃣ 브랜치 생성                                  │
│    - feature/{ISSUE-KEY}-{description}          │
│    - main에서 분기                               │
└─────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────┐
│ 4️⃣ Parent Issue 상태 변경                       │
│    - To Do → In Progress                        │
└─────────────────────────────────────────────────┘
```

---

## Plan → Sub-task 변환

### Plan 구현 항목 예시

```markdown
## 구현 계획
1. [TDD] Domain: Member.withdraw() 추가
2. [Doc] Application: WithdrawMemberUseCase 생성
3. [TDD] Persistence: MemberEntity soft delete
4. [Doc] REST API: DELETE /members/{id}
```

### 생성되는 Sub-task

| Sub-task | Summary | Labels |
|----------|---------|--------|
| MEMBER-124 | [Domain] Member.withdraw() 추가 | `domain`, `tdd` |
| MEMBER-125 | [Application] WithdrawMemberUseCase 생성 | `application`, `doc-driven` |
| MEMBER-126 | [Persistence] MemberEntity soft delete | `persistence`, `tdd` |
| MEMBER-127 | [REST API] DELETE /members/{id} | `rest-api`, `doc-driven` |

---

## Jira API 호출

### Sub-task 생성

```bash
POST /rest/api/3/issue
Authorization: Basic {base64(email:api_token)}
Content-Type: application/json

{
  "fields": {
    "project": { "key": "MEMBER" },
    "parent": { "key": "MEMBER-123" },
    "summary": "[Domain] Member.withdraw() 추가",
    "issuetype": { "name": "Sub-task" },
    "labels": ["domain", "tdd"],
    "description": {
      "type": "doc",
      "version": 1,
      "content": [
        {
          "type": "paragraph",
          "content": [
            { "type": "text", "text": "구현 전략: TDD" }
          ]
        }
      ]
    }
  }
}
```

### Parent Issue 상태 변경

```bash
POST /rest/api/3/issue/{issueKey}/transitions
Authorization: Basic {base64(email:api_token)}
Content-Type: application/json

{
  "transition": { "id": "21" }  // In Progress
}
```

---

## 브랜치 명명 규칙

```
feature/{PARENT-ISSUE-KEY}-{kebab-case-description}
```

### 예시

| Parent Issue | 기능 | 브랜치명 |
|--------------|------|---------|
| MEMBER-123 | 회원 탈퇴 | `feature/MEMBER-123-member-withdraw` |
| ORDER-456 | 주문 취소 | `feature/ORDER-456-order-cancel` |

---

## 출력 예시

```
📋 Plan 확인 중...
   ✅ plan-member-withdraw 로드 완료
   └─ 구현 항목: 4개

📝 Jira Sub-task 생성 중...
   ✅ MEMBER-124: [Domain] Member.withdraw() 추가
   ✅ MEMBER-125: [Application] WithdrawMemberUseCase 생성
   ✅ MEMBER-126: [Persistence] MemberEntity soft delete
   ✅ MEMBER-127: [REST API] DELETE /members/{id}

🌿 브랜치 생성 중...
   ✅ feature/MEMBER-123-member-withdraw 생성
   ✅ 원격 브랜치 푸시 완료

📊 Parent Issue 상태 변경...
   ✅ MEMBER-123: To Do → In Progress

─────────────────────────────────────

## ✅ 등록 완료

| 항목 | 값 |
|------|-----|
| Parent Issue | MEMBER-123 |
| Sub-tasks | 4개 생성 |
| 브랜치 | feature/MEMBER-123-member-withdraw |
| 상태 | In Progress |

### 생성된 Sub-task
| Key | Summary | 전략 |
|-----|---------|------|
| MEMBER-124 | [Domain] Member.withdraw() | TDD |
| MEMBER-125 | [Application] WithdrawMemberUseCase | Doc |
| MEMBER-126 | [Persistence] MemberEntity soft delete | TDD |
| MEMBER-127 | [REST API] DELETE /members/{id} | Doc |

🚀 다음 단계:
   1. /impl domain member-withdraw (또는 /kb/domain/go)
   2. Sub-task 완료 시 /jira-status MEMBER-124 done
```

---

## 워크플로우 전체 흐름

```
/jira-task MEMBER-123
        ↓
Jira에서 요구사항 추출 → /plan 자동 실행
        ↓
/jira-register MEMBER-123
        ↓
Plan → Sub-task 등록 → 브랜치 생성 → In Progress
        ↓
/impl domain member-withdraw
        ↓
/jira-status MEMBER-124 done  (Sub-task 완료)
        ↓
... 반복 ...
        ↓
모든 Sub-task 완료
        ↓
/jira-status MEMBER-123 done  (Parent Issue 완료)
```

---

## 에러 처리

### Plan 없음
```
❌ Plan을 찾을 수 없습니다
   - /plan "{기능}" 먼저 실행하세요
   - 또는 /jira-task {ISSUE-KEY}로 시작하세요
```

### Parent Issue 없음
```
❌ Jira Issue를 찾을 수 없습니다: MEMBER-999
   - Issue Key를 확인하세요
```

### Sub-task 생성 실패
```
❌ Sub-task 생성 실패
   - Jira 프로젝트에서 Sub-task 활성화 여부 확인
   - 권한 확인
```

---

## 연관 커맨드

- `/jira-task {KEY}` - Jira Issue → Plan 실행
- `/jira-status {KEY} {상태}` - Jira 상태 관리
- `/plan "{기능}"` - 직접 Plan 실행 (Jira 없이)
