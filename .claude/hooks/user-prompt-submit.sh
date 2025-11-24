#!/bin/bash

# =====================================================
# User Prompt Submit Hook + LangFuse Integration
# Trigger: 사용자 프롬프트 제출 시
# Purpose: TDD 워크플로우 추적 및 메트릭 수집
# =====================================================

USER_PROMPT="$1"

# LangFuse 로거 경로
LANGFUSE_LOGGER=".claude/scripts/log-to-langfuse.py"

# 프로젝트 정보
PROJECT_NAME=$(basename "$(pwd)")
TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# LangFuse 로깅 함수
log_to_langfuse() {
    local event_type="$1"
    local data="$2"

    if [[ -f "$LANGFUSE_LOGGER" ]]; then
        python3 "$LANGFUSE_LOGGER" \
            --event-type "$event_type" \
            --data "$data" 2>/dev/null
    fi
}

# ==================== TDD 키워드 감지 ====================

# TDD Phase 감지
TDD_PHASE="none"
TDD_LAYER="none"

# Red Phase (테스트 작성)
if echo "$USER_PROMPT" | grep -qiE "(write.*test|create.*test|add.*test|red phase|failing test|/kb.*red)"; then
    TDD_PHASE="red"
fi

# Green Phase (구현)
if echo "$USER_PROMPT" | grep -qiE "(implement|구현|green phase|pass.*test|make.*pass|/kb.*green)"; then
    TDD_PHASE="green"
fi

# Refactor Phase (리팩토링)
if echo "$USER_PROMPT" | grep -qiE "(refactor|리팩토링|clean.*up|improve|optimize|/kb.*refactor)"; then
    TDD_PHASE="refactor"
fi

# Tidy Phase (정리)
if echo "$USER_PROMPT" | grep -qiE "(tidy|정리|cleanup|fixture|/kb.*tidy)"; then
    TDD_PHASE="tidy"
fi

# Layer 감지
if echo "$USER_PROMPT" | grep -qiE "(domain|aggregate|entity|vo|value object)"; then
    TDD_LAYER="domain"
elif echo "$USER_PROMPT" | grep -qiE "(application|usecase|use case|service|facade)"; then
    TDD_LAYER="application"
elif echo "$USER_PROMPT" | grep -qiE "(persistence|repository|adapter|jpa|querydsl)"; then
    TDD_LAYER="persistence"
elif echo "$USER_PROMPT" | grep -qiE "(rest.*api|controller|endpoint|dto)"; then
    TDD_LAYER="rest-api"
elif echo "$USER_PROMPT" | grep -qiE "(integration|e2e|end.*to.*end)"; then
    TDD_LAYER="integration"
fi

# KB 명령어 감지
KB_COMMAND="none"
if echo "$USER_PROMPT" | grep -qE "/kb"; then
    if echo "$USER_PROMPT" | grep -qE "/kb/domain"; then
        KB_COMMAND="kb-domain"
        TDD_LAYER="domain"
    elif echo "$USER_PROMPT" | grep -qE "/kb/application"; then
        KB_COMMAND="kb-application"
        TDD_LAYER="application"
    elif echo "$USER_PROMPT" | grep -qE "/kb/persistence"; then
        KB_COMMAND="kb-persistence"
        TDD_LAYER="persistence"
    elif echo "$USER_PROMPT" | grep -qE "/kb/rest-api"; then
        KB_COMMAND="kb-rest-api"
        TDD_LAYER="rest-api"
    elif echo "$USER_PROMPT" | grep -qE "/kb/integration"; then
        KB_COMMAND="kb-integration"
        TDD_LAYER="integration"
    fi
fi

# ==================== LangFuse 로깅 ====================

# TDD 관련 프롬프트일 경우에만 로깅
if [[ "$TDD_PHASE" != "none" ]] || [[ "$TDD_LAYER" != "none" ]] || [[ "$KB_COMMAND" != "none" ]]; then
    # 프롬프트 요약 (처음 100자만)
    PROMPT_SUMMARY=$(echo "$USER_PROMPT" | head -c 100)

    log_to_langfuse "tdd_prompt" "{
        \"project\": \"$PROJECT_NAME\",
        \"tdd_phase\": \"$TDD_PHASE\",
        \"tdd_layer\": \"$TDD_LAYER\",
        \"kb_command\": \"$KB_COMMAND\",
        \"prompt_summary\": \"$PROMPT_SUMMARY\",
        \"timestamp\": \"$TIMESTAMP\"
    }"
fi

# ==================== Git Commit 체크 (자동 감지) ====================

# 최근 커밋 정보 (프롬프트 제출 시점 기준)
RECENT_COMMIT=$(git log -1 --pretty=%h 2>/dev/null || echo "")
if [[ -n "$RECENT_COMMIT" ]]; then
    COMMIT_MSG=$(git log -1 --pretty=%B 2>/dev/null || echo "")

    # 커밋 메시지에서 TDD Phase 재감지 (Kent Beck + Tidy First)
    COMMIT_PHASE="unknown"
    if echo "$COMMIT_MSG" | grep -qiE "^struct:"; then
        COMMIT_PHASE="structural"
    elif echo "$COMMIT_MSG" | grep -qiE "^test:|(test|red|failing)"; then
        COMMIT_PHASE="red"
    elif echo "$COMMIT_MSG" | grep -qiE "^feat:|^impl:|(implement|green|pass)"; then
        COMMIT_PHASE="green"
    elif echo "$COMMIT_MSG" | grep -qiE "^refactor:|(refactor|clean|improve)"; then
        COMMIT_PHASE="refactor"
    fi

    # 최근 5분 이내 커밋이면 로깅
    COMMIT_TIME=$(git log -1 --pretty=%ct 2>/dev/null || echo "0")
    CURRENT_TIME=$(date +%s)
    TIME_DIFF=$((CURRENT_TIME - COMMIT_TIME))

    if [[ $TIME_DIFF -lt 300 ]]; then  # 5분 = 300초
        FILES_CHANGED=$(git diff --stat HEAD~1 HEAD 2>/dev/null | tail -1 | grep -oE '[0-9]+ files? changed' || echo "0 files changed")
        LINES_CHANGED=$(git diff --stat HEAD~1 HEAD 2>/dev/null | tail -1 | grep -oE '[0-9]+ insertions?' || echo "0 insertions")

        log_to_langfuse "tdd_commit" "{
            \"project\": \"$PROJECT_NAME\",
            \"commit_hash\": \"$RECENT_COMMIT\",
            \"commit_msg\": \"$COMMIT_MSG\",
            \"tdd_phase\": \"$COMMIT_PHASE\",
            \"files_changed\": \"$FILES_CHANGED\",
            \"lines_changed\": \"$LINES_CHANGED\",
            \"timestamp\": \"$TIMESTAMP\"
        }"
    fi
fi

# 원본 USER_PROMPT 그대로 출력 (파이프라인 유지)
echo "$USER_PROMPT"
