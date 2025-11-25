#!/bin/bash

# =====================================================
# Git Post-Commit Hook for LangFuse TDD Tracking
# =====================================================

# LangFuse 환경 변수는 ~/.zshrc에 설정되어 있음
# 설정 방법:
#   echo 'export LANGFUSE_PUBLIC_KEY="pk-lf-..."' >> ~/.zshrc
#   echo 'export LANGFUSE_SECRET_KEY="sk-lf-..."' >> ~/.zshrc
#   echo 'export LANGFUSE_HOST="https://us.cloud.langfuse.com"' >> ~/.zshrc
#   source ~/.zshrc


# 프로젝트 정보
PROJECT_NAME=$(basename "$(pwd)")
TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# 커밋 정보 추출
COMMIT_MSG=$(git log -1 --pretty=%B)
COMMIT_HASH=$(git log -1 --pretty=%h)
FILES_CHANGED=$(git diff --stat HEAD~1 HEAD 2>/dev/null | tail -1 | grep -oE '[0-9]+ files? changed' || echo "0 files changed")
LINES_CHANGED=$(git diff --stat HEAD~1 HEAD 2>/dev/null | tail -1 | grep -oE '[0-9]+ insertions?' || echo "0 insertions")

# TDD Phase 감지 (Kent Beck + Tidy First 커밋 메시지 패턴)
TDD_PHASE="unknown"
if echo "$COMMIT_MSG" | grep -qiE "^struct:"; then
    TDD_PHASE="structural"
elif echo "$COMMIT_MSG" | grep -qiE "^test:.*\(Tidy\)"; then
    TDD_PHASE="tidy"
elif echo "$COMMIT_MSG" | grep -qiE "^test:"; then
    TDD_PHASE="red"
elif echo "$COMMIT_MSG" | grep -qiE "^(impl:|feat:|fix:)"; then
    TDD_PHASE="green"
elif echo "$COMMIT_MSG" | grep -qiE "^refactor:"; then
    TDD_PHASE="refactor"
elif echo "$COMMIT_MSG" | grep -qiE "^(docs:|chore:)"; then
    TDD_PHASE="non-tdd"
fi

# LangFuse 로거 호출
LANGFUSE_LOGGER=".claude/scripts/log-to-langfuse.py"
if [[ -f "$LANGFUSE_LOGGER" ]]; then
    python3 "$LANGFUSE_LOGGER" \
        --event-type "tdd_commit" \
        --data "{
            \"project\": \"$PROJECT_NAME\",
            \"commit_hash\": \"$COMMIT_HASH\",
            \"commit_msg\": \"$COMMIT_MSG\",
            \"tdd_phase\": \"$TDD_PHASE\",
            \"files_changed\": \"$FILES_CHANGED\",
            \"lines_changed\": \"$LINES_CHANGED\",
            \"timestamp\": \"$TIMESTAMP\"
        }" 2>/dev/null
fi
