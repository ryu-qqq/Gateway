#!/bin/bash

# =====================================================
# Post Tool Use Hook - Auto Format with Spotless
# Purpose: Java 파일 편집 후 자동으로 Spotless 포맷팅 적용
# =====================================================

# 입력: tool_input JSON
TOOL_INPUT="$1"

# ==================== 설정 ====================

# 포맷팅 적용 대상 확장자
TARGET_EXTENSIONS=("java")

# 프로젝트 루트 (gradlew 위치)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ==================== 파일 경로 추출 ====================

FILE_PATH=""

# JSON에서 file_path 추출 시도
if echo "$TOOL_INPUT" | grep -q "file_path"; then
    FILE_PATH=$(echo "$TOOL_INPUT" | grep -oE '"file_path"\s*:\s*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"$/\1/')
fi

# 파일 경로가 없으면 통과
if [ -z "$FILE_PATH" ]; then
    exit 0
fi

# ==================== 확장자 검사 ====================

FILE_EXT="${FILE_PATH##*.}"
IS_TARGET=false

for ext in "${TARGET_EXTENSIONS[@]}"; do
    if [ "$FILE_EXT" == "$ext" ]; then
        IS_TARGET=true
        break
    fi
done

# 대상 파일이 아니면 통과
if [ "$IS_TARGET" = false ]; then
    exit 0
fi

# ==================== Spotless 적용 ====================

cd "$PROJECT_ROOT"

# 파일이 존재하는지 확인
if [ ! -f "$FILE_PATH" ]; then
    exit 0
fi

# Spotless 포맷팅 적용 (quiet 모드, 백그라운드 실행하지 않음)
# --quiet 옵션으로 불필요한 출력 최소화
if ./gradlew spotlessApply --quiet --no-daemon 2>/dev/null; then
    # 성공 시 아무 메시지도 출력하지 않음 (조용히 포맷팅)
    :
else
    # 실패해도 에러로 처리하지 않음 (경고만)
    # 편집 자체는 허용하고 나중에 수동으로 포맷팅 가능
    :
fi

exit 0
