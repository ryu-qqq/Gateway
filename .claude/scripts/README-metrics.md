# TDD Metrics 분석 가이드

## 🎯 목적

Kent Beck TDD + Tidy First 워크플로우의 메트릭을 **로컬에서** 분석합니다.

LangFuse 대시보드는 이벤트 저장용이고, **실제 분석은 이 스크립트**로 하는 것이 더 편리합니다.

## 📊 사용법

### 기본 사용 (최근 7일)

```bash
python3 .claude/scripts/analyze-tdd-metrics.py
```

### 최근 30일 분석

```bash
python3 .claude/scripts/analyze-tdd-metrics.py --days 30
```

### 상세 분석 (TDD 사이클별 시간 등)

```bash
python3 .claude/scripts/analyze-tdd-metrics.py --detailed
```

## 📈 메트릭 설명

### 1. Phase별 커밋 분포

```
🔴 Red (test:)       - 실패하는 테스트 작성
🟢 Green (feat:)     - 최소 구현으로 테스트 통과
♻️  Refactor (struct:) - 구조 개선 (동작 변경 없음)
```

**목표**: Balanced (Red ≈ Green ≈ Refactor)

### 2. TDD 사이클 시간 (Red → Green)

Red 커밋부터 Green 커밋까지 걸린 시간.

**목표**: < 15분 (Kent Beck의 권장사항)

### 3. 커밋 크기

변경된 파일 수.

**목표**: ≤ 3 files/commit (작은 커밋)

### 4. Tidy First 준수율

전체 커밋 중 Structural 커밋 비율.

**목표**: ≥ 30% (구조 개선을 자주 하는 습관)

## 🔍 실전 활용

### 주간 리뷰

```bash
# 지난 7일 분석
python3 .claude/scripts/analyze-tdd-metrics.py --detailed

# 결과 해석:
# - TDD 사이클 시간 > 15분? → 테스트를 더 작게 쪼개기
# - Refactor 비율 낮음? → 구조 개선 습관 들이기
# - 커밋 크기 큼? → 더 작은 단위로 커밋
```

### 월간 추세 분석

```bash
# 이번 달 전체
python3 .claude/scripts/analyze-tdd-metrics.py --days 30 --detailed
```

### 특정 커밋만 필터링

```bash
# JSONL에서 직접 검색
grep "Email" ~/.claude/logs/tdd-cycle.jsonl | \
  jq -r '"\(.data.tdd_phase) | \(.data.commit_msg)"'
```

## 📁 데이터 위치

- **로그 파일**: `~/.claude/logs/tdd-cycle.jsonl`
- **LangFuse 대시보드**: https://us.cloud.langfuse.com (이벤트 저장용)

## 💡 Tip

LangFuse 대시보드에서 Analytics가 안 보이는 건 정상입니다!

우리는 **Event** 타입으로 저장하고 있어서 메트릭이 자동 계산되지 않습니다.
대신 이 **로컬 스크립트**가 훨씬 더 강력하고 편리합니다.

## 🎓 다음 단계

1. 매주 금요일 리뷰:
   ```bash
   python3 .claude/scripts/analyze-tdd-metrics.py --detailed
   ```

2. 목표 설정:
   - TDD 사이클 시간 < 15분
   - Tidy First 준수율 > 30%
   - 커밋 크기 ≤ 3 files

3. 개선 추적:
   - 월별로 비교하여 개선 확인
