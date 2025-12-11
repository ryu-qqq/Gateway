---
description: Serena MCP Memory 사용 가이드. Plan 저장/로드, 컴팩팅 대응, 작업 재개 방법.
tags: [project]
---

# Memory Guide - Serena MCP 연동

Serena MCP의 memory 기능을 활용하여 **자동 컴팩팅에도 작업 컨텍스트를 유지**합니다.

---

## 핵심 개념

### Memory란?

Serena MCP의 `write_memory` / `read_memory` 기능으로, **세션 간 정보를 영구 저장**합니다.

```
┌─────────────────────────────────────────────────┐
│ 세션 1: /plan "주문 취소"                        │
│   → Plan 생성                                   │
│   → write_memory("plan-order-cancel", plan)     │
│   → 오토컴팩팅 발생 ⚠️                          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│ 세션 2: "아까 하던 작업 계속"                     │
│   → read_memory("plan-order-cancel")            │
│   → 컨텍스트 복구 ✅                            │
│   → /impl domain order-cancel 계속              │
└─────────────────────────────────────────────────┘
```

---

## Memory 파일 규칙

### 네이밍 규칙

| 유형 | 형식 | 예시 |
|------|------|------|
| Plan | `plan-{feature-kebab-case}` | `plan-order-cancel` |
| 진행상태 | `progress-{feature}` | `progress-order-cancel` |
| 아키텍처 결정 | `decision-{topic}` | `decision-auth-strategy` |

### 저장 위치

Serena MCP가 관리하는 `.serena/memories/` 디렉토리에 저장됩니다.

---

## 자동 저장 시점

### `/plan` 실행 시

```python
# Phase 4: Serena memory 저장
mcp__serena__write_memory(
    memory_file_name="plan-order-cancel",
    content="""
# Plan: 주문 취소 기능

## 생성일시
2024-01-15 14:30:00

## 비즈니스 규칙
| ID | 규칙 | 상세 |
|----|------|------|
| BR-001 | 취소 가능 상태 | PLACED, CONFIRMED |
| BR-002 | 취소 범위 | 전체 취소만 |

## 영향도 분석
| 레이어 | 파일 | 상태 | 전략 |
|--------|------|------|------|
| Domain | Order.java | 🔧 수정 | TDD |
| Application | - | 🆕 신규 | Doc |

## 구현 계획
1. [TDD] Domain: Order.cancel()
2. [Doc] Application: CancelOrderUseCase
3. [Doc] Persistence: OrderEntity
4. [Doc] REST API: POST /orders/{id}/cancel

## 진행 상태
- [ ] Domain Layer
- [ ] Application Layer
- [ ] Persistence Layer
- [ ] REST API Layer

## 다음 명령어
/impl domain order-cancel
"""
)
```

### `/impl` 완료 시

```python
# 구현 완료 후 진행상태 업데이트
mcp__serena__edit_memory(
    memory_file_name="plan-order-cancel",
    needle="- [ ] Domain Layer",
    repl="- [x] Domain Layer (completed)",
    mode="literal"
)
```

---

## 작업 재개 방법

### 1. Memory 목록 확인

```bash
# Claude에게 요청
"현재 진행 중인 작업 확인해줘"

# Claude 실행
mcp__serena__list_memories()
```

**출력 예시**:
```
Available memories:
- plan-order-cancel
- plan-member-register
- decision-auth-strategy
```

### 2. 특정 Plan 로드

```bash
# Claude에게 요청
"주문 취소 작업 이어서 해줘"

# Claude 실행
mcp__serena__read_memory(memory_file_name="plan-order-cancel")
```

**Claude가 확인하는 내용**:
- 비즈니스 규칙
- 영향도 분석 결과
- 진행 상태 (체크리스트)
- 다음 실행할 명령어

### 3. 자동 재개

```bash
# Plan 로드 후 자동으로 다음 단계 실행
→ "진행 상태: Domain Layer 완료, Application Layer 대기 중"
→ /impl application order-cancel 자동 제안/실행
```

---

## Memory 조작 명령어

### 읽기

```python
# 특정 Plan 읽기
mcp__serena__read_memory(memory_file_name="plan-order-cancel")
```

### 쓰기

```python
# 새 Plan 저장
mcp__serena__write_memory(
    memory_file_name="plan-{feature}",
    content="..."
)
```

### 수정 (부분)

```python
# 진행상태 업데이트 (literal 모드)
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle="- [ ] Domain Layer",
    repl="- [x] Domain Layer (completed)",
    mode="literal"
)

# 패턴 기반 수정 (regex 모드)
mcp__serena__edit_memory(
    memory_file_name="plan-{feature}",
    needle=r"## 진행 상태.*?(?=##|\Z)",
    repl="## 진행 상태\n- [x] All completed\n\n",
    mode="regex"
)
```

### 삭제

```python
# 완료된 Plan 삭제
mcp__serena__delete_memory(memory_file_name="plan-order-cancel")
```

---

## 컴팩팅 대응 시나리오

### 시나리오 1: 작업 중 컴팩팅 발생

```
[Before Compacting]
사용자: /plan "주문 취소"
Claude: Plan 생성 완료, memory 저장
사용자: /impl domain order-cancel
Claude: Domain Layer 구현 중...
⚠️ 오토 컴팩팅 발생

[After Compacting]
사용자: "아까 작업 이어서 해줘"
Claude:
  1. mcp__serena__list_memories()
  2. mcp__serena__read_memory("plan-order-cancel")
  3. 진행상태 확인: Domain Layer 진행 중
  4. 이어서 구현 계속
```

### 시나리오 2: 다음날 작업 재개

```
사용자: "어제 하던 회원가입 작업 계속해줘"
Claude:
  1. mcp__serena__read_memory("plan-member-register")
  2. 진행상태: Application Layer 완료, Persistence Layer 대기
  3. /impl persistence member-register 제안
```

### 시나리오 3: 여러 작업 전환

```
사용자: "지금 진행 중인 작업들 뭐 있어?"
Claude:
  1. mcp__serena__list_memories()
  2. 각 plan의 진행상태 요약

출력:
## 진행 중인 작업

| Feature | 진행률 | 다음 단계 |
|---------|--------|----------|
| order-cancel | 75% | REST API Layer |
| member-register | 50% | Persistence Layer |
| product-search | 25% | Application Layer |
```

---

## Plan Memory 구조

```markdown
# Plan: {Feature Name}

## 생성일시
{ISO 8601 timestamp}

## 비즈니스 규칙
| ID | 규칙 | 상세 |
|----|------|------|
| BR-001 | ... | ... |

## 영향도 분석
| 레이어 | 파일 | 상태 | 전략 |
|--------|------|------|------|
| ... | ... | 🔧/🆕 | TDD/Doc |

## 구현 계획
1. [전략] 레이어: 작업내용
2. ...

## 진행 상태
- [ ] Domain Layer
- [ ] Application Layer
- [ ] Persistence Layer
- [ ] REST API Layer

## 다음 명령어
/impl {layer} {feature}

## 메모 (선택)
- 특이사항
- 결정사항
```

---

## Best Practices

### 1. Plan은 항상 저장

```
✅ /plan 실행 시 자동 저장
✅ /impl 완료 시 진행상태 업데이트
```

### 2. 의미있는 네이밍

```
✅ plan-order-cancel (명확)
✅ plan-member-register (명확)
❌ plan-1, plan-temp (불명확)
```

### 3. 완료 후 정리

```
# 기능 구현 완료 후
mcp__serena__delete_memory(memory_file_name="plan-{feature}")
```

### 4. 작업 재개 패턴

```bash
# 권장 패턴
"어제 하던 {기능명} 작업 이어서 해줘"
→ Claude가 자동으로 memory 로드 → 상태 확인 → 재개
```

---

## 연관 커맨드

- `/plan {feature}` - Plan 생성 및 memory 저장
- `/impl {layer} {feature}` - 구현 실행 및 memory 업데이트
- `/kb/{layer}/go` - TDD 실행 (기존 코드 수정 시)
