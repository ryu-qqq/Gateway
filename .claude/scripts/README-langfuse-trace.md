# LangFuse Trace/Span 기반 TDD 메트릭 추적

## 🎯 핵심 개념

**문제**: 기존 Event 방식은 Analytics가 자동 계산되지 않음

**해결**: Span 기반으로 각 Phase를 독립적으로 측정

### 작동 원리

```
Red 커밋
    ↓
Trace ID: "Email-VO-검증-테스트"
    ↓
Span: "🔴 Red Phase" (start → end)
    ↓
LangFuse에 duration 기록

Green 커밋 (나중에)
    ↓
Trace ID: "Email-VO-구현-RFC-5322-검증"  ← 같은 Trace ID!
    ↓
Span: "🟢 Green Phase" (start → end)
    ↓
LangFuse에 duration 기록

Structural 커밋
    ↓
Trace ID: "Email-검증-로직-메서드-추출"
    ↓
Span: "♻️ Structural Phase" (start → end)
    ↓
LangFuse에 duration 기록
```

## 🔑 핵심 장점

1. ✅ **세션 관리 불필요**: 각 커밋이 독립적으로 Span 생성
2. ✅ **자동 Trace ID**: 커밋 메시지에서 자동 추출
3. ✅ **Analytics 지원**: LangFuse가 자동으로 p50/p99 계산
4. ✅ **Phase별 측정**: Red/Green/Structural 각각의 duration 측정
5. ✅ **Trace로 묶기**: 같은 기능은 유사한 Trace ID로 필터링 가능

## 📊 Trace ID 생성 규칙

```python
"test: Email VO 검증 테스트"  → "Email-VO-검증-테스트"
"feat: Member 생성 API"      → "Member-생성-API"
"struct: Order 리팩토링"     → "Order-리팩토링"
```

**규칙**:
1. 커밋 prefix 제거 (`test:`, `feat:`, `struct:` 등)
2. 공백을 하이픈(`-`)으로 변경
3. 특수문자 제거 (한글, 영문, 숫자, 하이픈만 유지)
4. 최대 50자로 제한

## 🧪 테스트 예시

### Email VO 기능 (3개 Phase)

```bash
# Red Phase
python3 log-to-langfuse.py --event-type "tdd_commit" --data '{
  "commit_msg": "test: Email VO 검증 테스트 추가",
  "tdd_phase": "red", ...
}'
→ Trace ID: "Email-VO-검증-테스트"
→ Span: "🔴 Red Phase"

# Green Phase
python3 log-to-langfuse.py --event-type "tdd_commit" --data '{
  "commit_msg": "feat: Email VO 구현 (RFC 5322 검증)",
  "tdd_phase": "green", ...
}'
→ Trace ID: "Email-VO-구현-RFC-5322-검증"
→ Span: "🟢 Green Phase"

# Structural Phase
python3 log-to-langfuse.py --event-type "tdd_commit" --data '{
  "commit_msg": "struct: Email 검증 로직 메서드 추출",
  "tdd_phase": "structural", ...
}'
→ Trace ID: "Email-검증-로직-메서드-추출"
→ Span: "♻️ Structural Phase"
```

### Member 생성 기능 (2개 Phase)

```bash
# Red Phase
python3 log-to-langfuse.py --event-type "tdd_commit" --data '{
  "commit_msg": "test: Member 생성 테스트 추가",
  "tdd_phase": "red", ...
}'
→ Trace ID: "Member-생성-테스트"
→ Span: "🔴 Red Phase"

# Green Phase
python3 log-to-langfuse.py --event-type "tdd_commit" --data '{
  "commit_msg": "feat: Member 생성 로직 구현",
  "tdd_phase": "green", ...
}'
→ Trace ID: "Member-생성-로직-구현"
→ Span: "🟢 Green Phase"
```

## 📈 LangFuse 대시보드 사용법

### 1. Trace 필터링

```
Filter: Trace ID contains "Email"
→ Email 관련 모든 Phase 표시 (Red/Green/Structural)

Filter: Trace ID contains "Member"
→ Member 관련 모든 Phase 표시
```

### 2. Phase별 분석

```
Filter: Span name = "🔴 Red Phase"
→ Red Phase만 분석
→ p50, p99, avg duration 확인

Filter: Span name = "🟢 Green Phase"
→ Green Phase만 분석
```

### 3. Analytics 확인

LangFuse Analytics 탭에서:
- **Latency Distribution**: Phase별 duration 분포
- **p50/p95/p99**: 각 Phase의 백분위수 시간
- **Throughput**: 시간당 커밋 수
- **Success Rate**: Phase 완료율

## 🔄 기존 Event 방식과 비교

| 항목 | Event 방식 (기존) | Span 방식 (새로운) |
|------|-------------------|-------------------|
| **Analytics** | ❌ 없음 | ✅ 자동 계산 (p50/p99) |
| **Duration 측정** | ❌ 없음 | ✅ 자동 (start → end) |
| **Trace 묶기** | ⚠️ 수동 필터링 | ✅ Trace ID로 자동 |
| **복잡도** | 🟢 단순 | 🟢 단순 (세션 불필요) |
| **세션 관리** | ✅ 불필요 | ✅ 불필요 |

## 💡 Best Practices

### 1. 일관된 커밋 메시지

```bash
# ✅ 좋은 예
test: Email VO 검증 테스트
feat: Email VO 구현
struct: Email 검증 로직 추출

# ❌ 나쁜 예 (Trace ID가 달라짐)
test: Email 체크
feat: Email 만들기
struct: Email 리팩토링
```

### 2. 커밋 prefix 사용

```bash
# 반드시 prefix 사용
test:, feat:, struct:, fix:, chore:
```

### 3. 한글/영문 혼용 가능

```bash
# 모두 작동함
"test: Email VO 검증"       → "Email-VO-검증"
"test: Email validation"    → "Email-validation"
"test: 이메일 검증 테스트"   → "이메일-검증-테스트"
```

## 🚀 다음 단계

1. **LangFuse 대시보드 확인**:
   ```
   https://us.cloud.langfuse.com
   → Traces 탭에서 확인
   → Analytics 탭에서 메트릭 확인
   ```

2. **로컬 분석 병행**:
   ```bash
   python3 analyze-tdd-metrics.py --detailed
   ```

3. **커스텀 메트릭 추가**:
   - LangFuse: p50/p99, latency distribution
   - 로컬 스크립트: Tidy First 준수율, 커밋 크기 등

## 🎓 FAQ

**Q: Red와 Green이 같은 Trace ID가 아닌데 어떻게 묶나요?**
A: 커밋 메시지가 유사하면 Trace ID도 유사합니다. LangFuse에서 "contains" 필터로 검색하면 관련된 모든 Phase를 볼 수 있습니다.

**Q: 세션 관리가 정말 필요 없나요?**
A: 네! 각 커밋이 독립적으로 Span을 생성하고 즉시 종료합니다. Red와 Green 사이의 연결을 추적할 필요가 없습니다.

**Q: Analytics가 정말 작동하나요?**
A: 네! Span은 start_time과 end_time이 있어서 LangFuse가 자동으로 duration을 계산하고 p50/p99를 표시합니다.

**Q: 로컬 스크립트와 LangFuse 중 뭘 써야 하나요?**
A: 둘 다 사용하세요!
- **LangFuse**: p50/p99, latency distribution (표준 메트릭)
- **로컬 스크립트**: Tidy First 준수율, 커밋 크기 등 (커스텀 메트릭)
