#!/bin/bash

# =====================================================
# Pre Tool Use Hook: /impl 실행 전 검증
# Purpose: plan/design 메모리 존재 확인
# =====================================================

# 프로젝트 루트로 이동 (어느 디렉토리에서 실행해도 동작)
PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
if [ -n "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
fi

TOOL_INPUT="$1"

# SlashCommand인지 확인
if ! echo "$TOOL_INPUT" | grep -q "impl"; then
    exit 0
fi

# feature 이름 추출 (예: /impl:domain order-cancel → order-cancel)
FEATURE=$(echo "$TOOL_INPUT" | sed -E 's/.*impl[^ ]* +([^ ]+).*/\1/' | tr -d '"')

if [ -z "$FEATURE" ]; then
    exit 0
fi

# plan 메모리 확인
PLAN_FILE=".serena/memories/plan-${FEATURE}.md"
if [ ! -f "$PLAN_FILE" ]; then
    cat << EOF

⚠️ [PRE-IMPL CHECK FAILED]
plan-${FEATURE} 메모리가 없습니다.

먼저 실행하세요:
  /plan "${FEATURE}"

EOF
    exit 1
fi

# design 메모리 확인
DESIGN_FILE=".serena/memories/design-${FEATURE}.md"
if [ ! -f "$DESIGN_FILE" ]; then
    cat << EOF

⚠️ [PRE-IMPL CHECK FAILED]
design-${FEATURE} 메모리가 없습니다.

먼저 실행하세요:
  /design "${FEATURE}"

EOF
    exit 1
fi

# 모든 검증 통과
echo "✅ plan/design 메모리 확인 완료: ${FEATURE}"
exit 0
