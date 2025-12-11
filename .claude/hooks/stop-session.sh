#!/bin/bash

# =====================================================
# Stop Session Hook
# Purpose: 세션 종료 시 미완료 작업 알림 + 아카이브 제안
# =====================================================

# 프로젝트 루트로 이동 (어느 디렉토리에서 실행해도 동작)
PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
if [ -n "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
fi

# 진행 중인 plan 메모리 확인
ACTIVE_PLANS=$(ls .serena/memories/plan-*.md 2>/dev/null)
ACTIVE_DESIGNS=$(ls .serena/memories/design-*.md 2>/dev/null)

PLAN_COUNT=$(echo "$ACTIVE_PLANS" | grep -c "plan-" 2>/dev/null || echo "0")
DESIGN_COUNT=$(echo "$ACTIVE_DESIGNS" | grep -c "design-" 2>/dev/null || echo "0")

if [ "$PLAN_COUNT" -gt 0 ] || [ "$DESIGN_COUNT" -gt 0 ]; then
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📋 진행 중인 작업이 있습니다:"
    echo ""

    # plan 목록 표시
    for f in .serena/memories/plan-*.md; do
        [ -f "$f" ] || continue
        FEATURE=$(basename "$f" | sed 's/plan-//' | sed 's/.md$//')

        # design 존재 여부 확인
        if [ -f ".serena/memories/design-${FEATURE}.md" ]; then
            echo "  ✅ ${FEATURE} - 설계 완료, 구현 대기"
        else
            echo "  📝 ${FEATURE} - 분석 완료, 설계 대기"
        fi
    done

    echo ""
    echo "💡 작업 완료 시: /complete {feature}"
    echo "💡 작업 재개 시: \"{feature} 작업 이어서 해줘\""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
fi

# 오래된 메모리 정리 알림 (30일 이상)
OLD_MEMORIES=$(find .serena/memories -name "plan-*.md" -mtime +30 2>/dev/null | wc -l)
if [ "$OLD_MEMORIES" -gt 0 ]; then
    echo ""
    echo "🧹 30일 이상 된 메모리가 ${OLD_MEMORIES}개 있습니다."
    echo "   정리하려면: /cleanup"
    echo ""
fi

exit 0
