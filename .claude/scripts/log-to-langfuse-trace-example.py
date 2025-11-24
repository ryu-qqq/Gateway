#!/usr/bin/env python3
"""
LangFuse Trace/Span 구조 예시 (Option A)
TDD 사이클을 Trace/Span으로 제대로 저장하는 방법

문제점:
- Red/Green 커밋 사이에 세션이 유지되지 않음 (각각 독립적인 post-commit hook 실행)
- Span은 start_time과 end_time이 모두 필요한데, Red 시점에는 Green 시간을 알 수 없음
- 세션 관리를 위해 파일 기반 상태 저장 필요 (복잡도 증가)

해결 방법:
- Red 커밋 시: Span 시작 정보를 파일에 저장 (~/.claude/logs/tdd-session.json)
- Green 커밋 시: 저장된 Span 정보를 읽어서 종료
"""

import json
import os
from datetime import datetime
from pathlib import Path

try:
    from langfuse import Langfuse
except ImportError:
    print("langfuse SDK가 설치되지 않았습니다.")
    exit(1)

# 세션 상태 파일
SESSION_FILE = Path.home() / ".claude" / "logs" / "tdd-session.json"


def save_session(trace_id: str, span_id: str, red_commit: dict):
    """Red 커밋 시: Span 시작 정보를 파일에 저장"""
    SESSION_FILE.parent.mkdir(parents=True, exist_ok=True)

    session = {
        "trace_id": trace_id,
        "span_id": span_id,
        "red_commit": red_commit,
        "started_at": datetime.utcnow().isoformat()
    }

    with open(SESSION_FILE, "w") as f:
        json.dump(session, f, indent=2)


def load_session():
    """Green 커밋 시: 저장된 Span 정보 읽기"""
    if not SESSION_FILE.exists():
        return None

    with open(SESSION_FILE) as f:
        return json.load(f)


def clear_session():
    """세션 완료 후 정리"""
    if SESSION_FILE.exists():
        SESSION_FILE.unlink()


def log_red_commit(commit_data: dict):
    """
    Red Phase: Trace/Span 시작

    LangFuse에서 p50/p99를 계산하려면:
    - Trace: 전체 TDD 사이클을 나타냄
    - Span: Red → Green 구간 (start_time, end_time)
    """
    langfuse = Langfuse()

    # Trace 시작 (TDD 사이클 전체)
    trace_id = f"tdd-cycle-{commit_data['commit_hash']}"

    # Span 시작 (Red → Green 측정 구간)
    span = langfuse.start_span(
        name="TDD Cycle - Red to Green",
        trace_id=trace_id,
        input={
            "phase": "red",
            "commit_msg": commit_data["commit_msg"],
            "files_changed": commit_data["files_changed"]
        },
        metadata={
            "project": commit_data["project"],
            "commit_hash": commit_data["commit_hash"]
        }
    )

    # Span 정보를 파일에 저장 (Green 커밋 시 사용)
    save_session(
        trace_id=trace_id,
        span_id=span.id,
        red_commit=commit_data
    )

    langfuse.flush()
    print(f"✅ Red 커밋 Span 시작: {span.id}")


def log_green_commit(commit_data: dict):
    """
    Green Phase: Span 종료

    저장된 Span 정보를 읽어서 종료 → LangFuse가 자동으로 duration 계산
    """
    session = load_session()

    if not session:
        print("⚠️ Red 커밋 세션이 없습니다. Span을 종료할 수 없습니다.")
        return

    langfuse = Langfuse()

    # 기존 Span 종료
    span = langfuse.span(
        id=session["span_id"],
        trace_id=session["trace_id"]
    )

    span.update(
        output={
            "phase": "green",
            "commit_msg": commit_data["commit_msg"],
            "files_changed": commit_data["files_changed"]
        }
    )

    span.end()  # end_time 기록 → duration 자동 계산

    langfuse.flush()

    # 세션 정리
    clear_session()

    print(f"✅ Green 커밋 Span 종료: {span.id}")
    print(f"📊 LangFuse에서 TDD 사이클 시간 분석 가능")


def log_structural_commit(commit_data: dict):
    """
    Structural Phase: 독립적인 Event

    Refactor는 시간 측정 대상이 아니므로 단순 Event로 저장
    """
    langfuse = Langfuse()

    trace_id = f"structural-{commit_data['commit_hash']}"

    langfuse.create_event(
        trace_id=trace_id,
        name="Structural Change",
        metadata=commit_data
    )

    langfuse.flush()
    print(f"✅ Structural 커밋 Event 생성")


# 테스트 코드
if __name__ == "__main__":
    # 예시 1: Red 커밋
    red_commit = {
        "project": "claude-spring-standards",
        "commit_hash": "abc123",
        "commit_msg": "test: Email VO 검증 테스트 추가",
        "tdd_phase": "red",
        "files_changed": "2 files changed",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }

    print("\n=== Red 커밋 로깅 ===")
    log_red_commit(red_commit)

    # 예시 2: Green 커밋 (5분 후라고 가정)
    import time
    print("\n(5초 대기... 실제로는 TDD 작업 시간)")
    time.sleep(5)

    green_commit = {
        "project": "claude-spring-standards",
        "commit_hash": "def456",
        "commit_msg": "feat: Email VO 구현 (RFC 5322 검증)",
        "tdd_phase": "green",
        "files_changed": "1 file changed",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }

    print("\n=== Green 커밋 로깅 ===")
    log_green_commit(green_commit)

    # 예시 3: Structural 커밋
    struct_commit = {
        "project": "claude-spring-standards",
        "commit_hash": "ghi789",
        "commit_msg": "struct: Email 검증 로직 메서드 추출",
        "tdd_phase": "structural",
        "files_changed": "1 file changed",
        "timestamp": datetime.utcnow().isoformat() + "Z"
    }

    print("\n=== Structural 커밋 로깅 ===")
    log_structural_commit(struct_commit)

    print("\n✅ 완료! LangFuse 대시보드에서 확인하세요.")
    print("→ Red → Green 구간의 duration이 자동 계산됩니다.")
    print("→ 이제 p50, p99 등의 메트릭이 Analytics에 표시됩니다.")
