#!/bin/bash

# =====================================================
# Pre Tool Use Hook - Edit/Write 보호
# Purpose: ArchUnit 및 핵심 아키텍처 파일 수정 방지
# =====================================================

# 프로젝트 루트로 이동 (어느 디렉토리에서 실행해도 동작)
PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"
if [ -n "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
fi

# 입력: tool_input JSON
TOOL_INPUT="$1"

# ==================== 보호 대상 패턴 ====================

PROTECTED_PATTERNS=(
    # ArchUnit 테스트 파일
    "**/architecture/*ArchTest.java"
    "**/architecture/*ArchitectureTest.java"

    # Test Fixtures ArchUnit
    "**/test-fixtures/*ArchTest.java"

    # Claude Hook 설정 (자기 자신 제외는 불가, 주의 필요)
    # ".claude/hooks/**"  # 이건 관리자만 수정하도록 별도 관리
)

# ==================== 파일 경로 추출 ====================

# Edit, Write, MultiEdit 도구에서 file_path 추출
FILE_PATH=""

# JSON에서 file_path 추출 시도 (jq가 없을 수 있으므로 grep 사용)
if echo "$TOOL_INPUT" | grep -q "file_path"; then
    FILE_PATH=$(echo "$TOOL_INPUT" | grep -oE '"file_path"\s*:\s*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
fi

# 파일 경로가 없으면 통과
if [ -z "$FILE_PATH" ]; then
    exit 0
fi

# ==================== 보호 검사 ====================

for pattern in "${PROTECTED_PATTERNS[@]}"; do
    # 패턴 매칭 (fnmatch 스타일)
    if [[ "$FILE_PATH" == $pattern ]] || [[ "$FILE_PATH" == *"$pattern"* ]]; then
        # ArchTest 패턴 확인
        if [[ "$FILE_PATH" == *"ArchTest.java" ]] || [[ "$FILE_PATH" == *"ArchitectureTest.java" ]]; then
            cat << 'EOF'

🚨 ArchUnit 테스트 파일 수정이 차단되었습니다!

이 파일은 프로젝트 아키텍처를 강제하는 핵심 파일입니다.
무단 수정 시 아키텍처 무결성이 훼손될 수 있습니다.

수정이 필요한 경우:
1. 팀 리드 또는 아키텍처 팀에 문의하세요
2. 아키텍처 결정 회의를 통해 승인을 받으세요
3. 승인 후 아키텍처 팀이 직접 수정합니다

참고 문서:
- docs/coding_convention/01-adapter-in-layer/rest-api/testing/04_rest-api-archunit-guide.md
- docs/coding_convention/05-testing/test-fixtures/02_test-fixtures-archunit.md

차단된 파일: $FILE_PATH

EOF
            exit 2  # 차단 (exit code 2 = block)
        fi
    fi
done

# 통과
exit 0
