# TDD Form - Interactive Question Answering

TDD 개발 시 긴 질문 리스트를 웹 UI에서 편하게 답변하는 인터랙티브 폼 시스템입니다.

## 사용법

```bash
/tdd-form
```

## 동작 방식

1. **HTML Form 생성**: `.claude/tools/interactive-form.html` 동적 생성
2. **질문 데이터 주입**: Claude가 질문 배열을 JavaScript로 주입
3. **브라우저 자동 오픈**: Playwright로 로컬 HTML 파일 오픈
4. **사용자 답변 입력**: 페이지네이션으로 질문 하나씩 답변
5. **JSON 저장**: 답변 완료 후 `tdd-answers.json` 다운로드
6. **Claude 처리**: JSON 파일 읽고 코드 생성

## 주요 기능

- ✅ **페이지네이션**: 질문 1개씩 보기 (스크롤 지옥 해결)
- ✅ **자동 저장**: LocalStorage로 임시 저장 (새로고침 안전)
- ✅ **진행률 표시**: "3/20 완료" 프로그레스 바
- ✅ **키보드 단축키**:
  - `Ctrl + Enter`: 다음 질문
  - `Shift + Enter`: 이전 질문
- ✅ **반응형 디자인**: 모바일/태블릿/데스크톱 모두 지원

## 질문 타입 지원

### 1. Domain Layer 질문
- Domain 이름
- Aggregate 속성
- 비즈니스 규칙
- Value Object 목록

### 2. Application Layer 질문
- UseCase 목록
- Command/Query DTO
- Transaction 경계
- 외부 API 호출 전략

### 3. Persistence Layer 질문
- JPA Entity 설계
- QueryDSL 쿼리
- 인덱스 전략
- 동시성 제어

### 4. REST API Layer 질문
- API 엔드포인트
- Request/Response DTO
- 인증/인가 전략
- Error Handling

## 예시

```bash
# Domain Layer 질문
/tdd-form domain

# UseCase 생성 질문
/tdd-form usecase

# 전체 Layer 질문 (20개+)
/tdd-form full
```

## 출력 파일

**저장 위치**: `Downloads/tdd-answers.json`

**형식**:
```json
{
  "timestamp": "2025-01-13T12:34:56Z",
  "questions": [
    {
      "id": "domain_name",
      "question": "Domain 이름은 무엇인가요?",
      "help": "예: Order, User, Product",
      "type": "text"
    }
  ],
  "answers": {
    "domain_name": "Order",
    "aggregate_properties": "orderId, customerId, status, totalPrice",
    "business_rules": "주문은 PLACED 상태에서만 취소 가능"
  }
}
```

## 다음 단계

1. **답변 파일 확인**: `Downloads/tdd-answers.json`
2. **Claude 처리**: "tdd-answers.json 읽고 코드 생성해줘"
3. **코드 생성**: Domain/UseCase/Entity/Controller 자동 생성

## 장점

- ✅ **스크롤 지옥 해결**: 질문 1개씩 깔끔하게
- ✅ **답변 실수 방지**: 이전/다음 버튼으로 수정 가능
- ✅ **자동 저장**: 브라우저 꺼져도 답변 유지
- ✅ **시각적 진행률**: 얼마나 남았는지 한눈에
- ✅ **JSON 자동 생성**: 복사/붙여넣기 필요 없음

## 참고

- HTML 파일: `.claude/tools/interactive-form.html`
- 답변 파일: `Downloads/tdd-answers.json`
- 자동 저장: LocalStorage (`tdd_answers` 키)
