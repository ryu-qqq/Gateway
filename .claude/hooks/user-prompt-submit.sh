#!/bin/bash

# =====================================================
# User Prompt Submit Hook (Enhanced)
# Purpose:
#   1. 진행 중인 작업 표시 (세션 시작 시)
#   2. 커맨드 없이 구현 요청 시 가이드 주입
#
# Note: stdout 출력은 Claude에게 system-reminder로 전달됨
# =====================================================

# 프로젝트 루트로 이동 (어느 디렉토리에서 실행해도 동작)
PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
if [ -n "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
fi

USER_PROMPT="$1"

# 프롬프트가 비어있으면 종료
if [ -z "$USER_PROMPT" ]; then
    exit 0
fi

# ==================== 진행 중 작업 표시 ====================

# 세션 첫 메시지인지 확인 (간단한 인사 또는 짧은 메시지)
PROMPT_LENGTH=${#USER_PROMPT}
if [ "$PROMPT_LENGTH" -lt 50 ]; then
    # 진행 중인 작업 확인
    if ls .serena/memories/plan-*.md 1>/dev/null 2>&1; then
        echo ""
        echo "🔄 진행 중인 작업:"

        for f in .serena/memories/plan-*.md; do
            [ -f "$f" ] || continue
            FEATURE=$(basename "$f" | sed 's/plan-//' | sed 's/.md$//')

            # design 존재 여부 확인
            if [ -f ".serena/memories/design-${FEATURE}.md" ]; then
                echo "  ✅ ${FEATURE} (설계 완료, 구현 대기)"
            else
                echo "  📝 ${FEATURE} (분석 완료, 설계 대기)"
            fi
        done

        echo ""
        echo "💡 이어서 작업하려면: \"{feature} 작업 이어서 해줘\""
    fi
fi

# ==================== 커맨드 감지 ====================

# 정식 워크플로우 커맨드 사용 중이면 조용히 통과
if echo "$USER_PROMPT" | grep -qE "^/(plan|impl|design|verify|status|complete|jira-task|jira-register|jira-status|kb-|refactor-plan)"; then
    exit 0
fi

# ==================== 위험 키워드 감지 ====================

# 구현/수정 관련 키워드 (커맨드 없이 직접 요청)
NEEDS_GUIDANCE=false
DETECTED_LAYER=""

# Domain Layer 키워드
if echo "$USER_PROMPT" | grep -qiE "(aggregate|domain|vo|value.?object|entity 만|domain.?event|domain.?exception)"; then
    NEEDS_GUIDANCE=true
    DETECTED_LAYER="domain"
fi

# Application Layer 키워드
if echo "$USER_PROMPT" | grep -qiE "(usecase|use.?case|service 만|facade|manager|assembler|factory)"; then
    NEEDS_GUIDANCE=true
    DETECTED_LAYER="application"
fi

# Persistence Layer 키워드
if echo "$USER_PROMPT" | grep -qiE "(repository|jpa.?entity|querydsl|adapter|mapper 만)"; then
    NEEDS_GUIDANCE=true
    DETECTED_LAYER="persistence"
fi

# REST API Layer 키워드
if echo "$USER_PROMPT" | grep -qiE "(controller|rest.?api|endpoint|request.?dto|response.?dto)"; then
    NEEDS_GUIDANCE=true
    DETECTED_LAYER="rest-api"
fi

# ==================== 가이드 주입 ====================

if [ "$NEEDS_GUIDANCE" = true ]; then
    cat << 'EOF'

⚠️ [WORKFLOW GUIDE]
커맨드 없이 직접 구현을 요청하셨습니다.

권장 워크플로우:
1. /plan "{기능}" - 먼저 계획 수립
2. /impl {layer} {feature} - 문서 기반 구현

또는 기존 코드 수정 시:
- /kb/{layer}/go - TDD 기반 수정

EOF

    # 감지된 레이어별 핵심 컨벤션 주입
    if [ "$DETECTED_LAYER" = "domain" ]; then
        cat << 'EOF'
[Domain Layer 핵심 규칙]
- Lombok 금지 (Pure Java)
- Law of Demeter (Getter 체이닝 금지)
- Tell Don't Ask (상태 묻지 말고 행동 요청)
- VO는 record 사용
- 참조: docs/coding_convention/02-domain-layer/
EOF
    elif [ "$DETECTED_LAYER" = "application" ]; then
        cat << 'EOF'
[Application Layer 핵심 규칙]
- @Transactional 내 외부 API 호출 금지
- CQRS 분리 (Command/Query UseCase)
- DTO는 record 사용
- Assembler로 변환
- 참조: docs/coding_convention/03-application-layer/
EOF
    elif [ "$DETECTED_LAYER" = "persistence" ]; then
        cat << 'EOF'
[Persistence Layer 핵심 규칙]
- Long FK 전략 (JPA 관계 어노테이션 금지)
- QueryDSL DTO Projection 필수
- Lombok 금지
- Mapper 분리
- 참조: docs/coding_convention/04-persistence-layer/
EOF
    elif [ "$DETECTED_LAYER" = "rest-api" ]; then
        cat << 'EOF'
[REST API Layer 핵심 규칙]
- RESTful 설계
- Request/Response DTO 분리
- @Valid 필수
- TestRestTemplate 사용 (MockMvc 금지)
- 참조: docs/coding_convention/01-adapter-in-layer/
EOF
    fi
fi

# 성공 종료 (출력 없이)
exit 0
