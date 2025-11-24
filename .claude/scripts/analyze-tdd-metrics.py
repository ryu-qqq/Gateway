#!/usr/bin/env python3
"""
TDD Metrics Analyzer
Kent Beck TDD + Tidy First 워크플로우 메트릭 분석

Usage: python3 analyze-tdd-metrics.py [--days 7] [--detailed]
"""

import argparse
import json
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Tuple


JSONL_LOG = Path.home() / ".claude" / "logs" / "tdd-cycle.jsonl"


def load_commits(days: int = 7) -> List[Dict]:
    """최근 N일간의 커밋 로드"""
    if not JSONL_LOG.exists():
        return []

    from datetime import timezone
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)
    commits = []

    with open(JSONL_LOG) as f:
        for line in f:
            entry = json.loads(line)
            if entry.get("event_type") != "tdd_commit":
                continue

            timestamp_str = entry["timestamp"].replace("Z", "+00:00")
            timestamp = datetime.fromisoformat(timestamp_str)
            if timestamp >= cutoff:
                commits.append(entry["data"])

    return commits


def analyze_phase_distribution(commits: List[Dict]) -> Dict[str, int]:
    """Phase별 커밋 분포"""
    distribution = defaultdict(int)
    for commit in commits:
        phase = commit.get("tdd_phase", "unknown")
        distribution[phase] += 1
    return dict(distribution)


def analyze_commit_size(commits: List[Dict]) -> Tuple[float, List[int]]:
    """커밋 크기 분석"""
    sizes = []
    for commit in commits:
        files_str = commit.get("files_changed", "0 files changed")
        # "2 files changed" → 2
        num = int(files_str.split()[0]) if files_str.split()[0].isdigit() else 0
        sizes.append(num)

    avg = sum(sizes) / len(sizes) if sizes else 0
    return avg, sizes


def analyze_tdd_cycle_time(commits: List[Dict]) -> List[Dict]:
    """TDD 사이클 시간 분석 (Red → Green)"""
    cycles = []
    red_commit = None

    # 시간순 정렬
    sorted_commits = sorted(commits, key=lambda c: c.get("timestamp", ""))

    for commit in sorted_commits:
        phase = commit.get("tdd_phase")

        if phase == "red":
            red_commit = commit
        elif phase == "green" and red_commit:
            # Red → Green 사이클 시간 계산
            red_time = datetime.fromisoformat(red_commit["timestamp"].replace("Z", "+00:00"))
            green_time = datetime.fromisoformat(commit["timestamp"].replace("Z", "+00:00"))
            duration = (green_time - red_time).total_seconds() / 60  # 분 단위

            cycles.append({
                "red_commit": red_commit["commit_hash"],
                "green_commit": commit["commit_hash"],
                "duration_minutes": round(duration, 1),
                "red_msg": red_commit["commit_msg"],
                "green_msg": commit["commit_msg"]
            })
            red_commit = None

    return cycles


def calculate_tidy_first_score(commits: List[Dict]) -> float:
    """Tidy First 준수율 계산"""
    total = len(commits)
    if total == 0:
        return 0.0

    structural_count = sum(1 for c in commits if c.get("tdd_phase") == "structural")
    return (structural_count / total) * 100


def print_summary(commits: List[Dict], cycles: List[Dict], detailed: bool = False):
    """메트릭 요약 출력"""
    print("\n" + "="*70)
    print("📊 TDD Metrics Summary (Kent Beck + Tidy First)")
    print("="*70)

    # 1. 전체 커밋 수
    print(f"\n📝 총 커밋 수: {len(commits)}")

    # 2. Phase별 분포
    distribution = analyze_phase_distribution(commits)
    total = len(commits)

    print("\n🎯 Phase별 커밋 분포:")
    print("─" * 70)

    phases_display = {
        "red": ("🔴 Red (test:)", "실패하는 테스트 작성"),
        "green": ("🟢 Green (feat:)", "최소 구현으로 테스트 통과"),
        "structural": ("♻️  Refactor (struct:)", "구조 개선 (동작 변경 없음)"),
        "unknown": ("❓ Unknown", "분류 안 된 커밋")
    }

    for phase, count in sorted(distribution.items(), key=lambda x: x[1], reverse=True):
        display, desc = phases_display.get(phase, (phase, ""))
        percentage = (count / total * 100) if total > 0 else 0
        bar_length = int(percentage / 2)  # 50% = 25 chars
        bar = "█" * bar_length + "░" * (50 - bar_length)
        print(f"{display:25} {count:3} ({percentage:5.1f}%) {bar}")
        if detailed:
            print(f"{'':25} → {desc}")

    # 3. TDD 사이클 시간
    if cycles:
        print("\n⏱️  TDD 사이클 시간 (Red → Green):")
        print("─" * 70)

        durations = [c["duration_minutes"] for c in cycles]
        avg_duration = sum(durations) / len(durations)
        max_duration = max(durations)
        min_duration = min(durations)

        print(f"평균: {avg_duration:.1f}분 {'✅' if avg_duration < 15 else '⚠️'} (목표: < 15분)")
        print(f"최소: {min_duration:.1f}분")
        print(f"최대: {max_duration:.1f}분")

        if detailed:
            print("\n최근 5개 사이클:")
            for cycle in cycles[-5:]:
                status = "✅" if cycle["duration_minutes"] < 15 else "⚠️"
                print(f"  {status} {cycle['duration_minutes']:5.1f}분 | {cycle['red_commit'][:7]} → {cycle['green_commit'][:7]}")
                print(f"     Red:   {cycle['red_msg'][:60]}")
                print(f"     Green: {cycle['green_msg'][:60]}")

    # 4. 커밋 크기
    avg_size, sizes = analyze_commit_size(commits)
    print(f"\n📏 평균 커밋 크기: {avg_size:.1f} files/commit {'✅' if avg_size <= 3 else '⚠️'} (목표: ≤ 3)")

    if detailed and sizes:
        size_dist = defaultdict(int)
        for size in sizes:
            if size == 1:
                size_dist["1 file"] += 1
            elif size <= 3:
                size_dist["2-3 files"] += 1
            elif size <= 5:
                size_dist["4-5 files"] += 1
            else:
                size_dist["6+ files"] += 1

        print("\n커밋 크기 분포:")
        for category, count in sorted(size_dist.items()):
            percentage = (count / len(sizes) * 100)
            print(f"  {category:12}: {count:3} ({percentage:5.1f}%)")

    # 5. Tidy First 준수율
    tidy_score = calculate_tidy_first_score(commits)
    print(f"\n🧹 Tidy First 준수율: {tidy_score:.1f}% {'✅' if tidy_score >= 30 else '⚠️'} (목표: ≥ 30%)")
    print("   → Structural 커밋 비율 (구조 개선 먼저 하는 습관)")

    # 6. 최근 커밋들
    print("\n📋 최근 5개 커밋:")
    print("─" * 70)

    recent = sorted(commits, key=lambda c: c.get("timestamp", ""), reverse=True)[:5]
    for commit in recent:
        phase = commit.get("tdd_phase", "unknown")
        phase_emoji = {
            "red": "🔴",
            "green": "🟢",
            "structural": "♻️",
            "unknown": "❓"
        }.get(phase, "❓")

        timestamp = commit.get("timestamp", "")[:19].replace("T", " ")
        hash_short = commit.get("commit_hash", "")[:7]
        msg = commit.get("commit_msg", "")[:50]

        print(f"{phase_emoji} {timestamp} | {hash_short} | {msg}")

    print("\n" + "="*70)
    print("💡 Tip: --detailed 옵션으로 더 자세한 분석 확인")
    print("="*70 + "\n")


def main():
    parser = argparse.ArgumentParser(description="Analyze TDD metrics from commit logs")
    parser.add_argument("--days", type=int, default=7, help="Number of days to analyze (default: 7)")
    parser.add_argument("--detailed", action="store_true", help="Show detailed analysis")

    args = parser.parse_args()

    # 데이터 로드
    commits = load_commits(days=args.days)

    if not commits:
        print(f"\n⚠️  최근 {args.days}일간 TDD 커밋이 없습니다.")
        print(f"로그 파일: {JSONL_LOG}\n")
        return

    # TDD 사이클 분석
    cycles = analyze_tdd_cycle_time(commits)

    # 결과 출력
    print_summary(commits, cycles, detailed=args.detailed)


if __name__ == "__main__":
    main()
