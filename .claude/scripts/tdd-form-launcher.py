#!/usr/bin/env python3
"""
TDD Form Launcher - Playwright를 사용하여 HTML Form을 브라우저에서 오픈
"""

import json
import os
import sys
from pathlib import Path

def generate_html_with_questions(questions, output_path):
    """
    질문 데이터를 주입한 HTML 파일 생성

    Args:
        questions: 질문 배열
        output_path: 출력 HTML 파일 경로
    """
    # 템플릿 HTML 읽기
    template_path = Path(__file__).parent.parent / "tools" / "interactive-form.html"

    with open(template_path, 'r', encoding='utf-8') as f:
        html_content = f.read()

    # 질문 데이터 JSON으로 변환
    questions_json = json.dumps(questions, ensure_ascii=False, indent=2)

    # JavaScript에 질문 데이터 주입
    html_content = html_content.replace(
        'let questions = window.QUESTIONS || [',
        f'let questions = {questions_json} || ['
    )

    # HTML 파일 저장
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(html_content)

    print(f"✅ HTML 파일 생성: {output_path}")


def get_default_questions(question_type="domain"):
    """
    기본 질문 세트 반환

    Args:
        question_type: domain, usecase, persistence, rest-api, full

    Returns:
        질문 배열
    """
    questions = []

    if question_type in ["domain", "full"]:
        questions.extend([
            {
                "id": "domain_name",
                "question": "Domain 이름은 무엇인가요?",
                "help": "예: Order, User, Product",
                "type": "text"
            },
            {
                "id": "aggregate_properties",
                "question": "Aggregate의 주요 속성은?",
                "help": "예: orderId: OrderId, customerId: Long, status: OrderStatus, totalPrice: BigDecimal",
                "type": "textarea"
            },
            {
                "id": "business_rules",
                "question": "핵심 비즈니스 규칙은?",
                "help": "예: 주문은 PLACED 상태에서만 취소 가능",
                "type": "textarea"
            },
            {
                "id": "value_objects",
                "question": "Value Object 목록은?",
                "help": "예: OrderId (UUID), OrderStatus (Enum)",
                "type": "textarea"
            },
            {
                "id": "state_transitions",
                "question": "상태 전환 흐름은?",
                "help": "예: PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED",
                "type": "textarea"
            }
        ])

    if question_type in ["usecase", "full"]:
        questions.extend([
            {
                "id": "usecase_list",
                "question": "필요한 UseCase 목록은?",
                "help": "예: PlaceOrderUseCase, CancelOrderUseCase, GetOrderUseCase",
                "type": "textarea"
            },
            {
                "id": "command_dto",
                "question": "Command DTO 정의는?",
                "help": "예: PlaceOrderCommand(customerId, productId, quantity)",
                "type": "textarea"
            },
            {
                "id": "transaction_boundary",
                "question": "Transaction 경계는 어떻게 설정하나요?",
                "help": "예: 주문 생성 + 재고 차감만 트랜잭션, 결제는 트랜잭션 밖",
                "type": "textarea"
            },
            {
                "id": "external_api",
                "question": "외부 API 호출이 필요한가요?",
                "help": "예: 결제 Gateway (Stripe), 배송 API",
                "type": "textarea"
            }
        ])

    if question_type in ["persistence", "full"]:
        questions.extend([
            {
                "id": "jpa_entity",
                "question": "JPA Entity 목록은?",
                "help": "예: OrderJpaEntity, OrderLineJpaEntity",
                "type": "textarea"
            },
            {
                "id": "querydsl_queries",
                "question": "복잡한 쿼리가 필요한가요?",
                "help": "예: 고객별 주문 조회, 상태별 주문 통계",
                "type": "textarea"
            },
            {
                "id": "index_strategy",
                "question": "인덱스 전략은?",
                "help": "예: idx_customer_id_created_at (customer_id, created_at DESC)",
                "type": "textarea"
            },
            {
                "id": "concurrency_control",
                "question": "동시성 제어 전략은?",
                "help": "예: Optimistic Lock (@Version), Pessimistic Lock",
                "type": "textarea"
            }
        ])

    if question_type in ["rest-api", "full"]:
        questions.extend([
            {
                "id": "api_endpoints",
                "question": "API 엔드포인트 목록은?",
                "help": "예: POST /api/v1/orders, GET /api/v1/orders/{orderId}",
                "type": "textarea"
            },
            {
                "id": "request_dto",
                "question": "Request DTO 정의는?",
                "help": "예: PlaceOrderRequest(customerId, productId, quantity)",
                "type": "textarea"
            },
            {
                "id": "auth_strategy",
                "question": "인증/인가 전략은?",
                "help": "예: JWT (Access Token + Refresh Token), RBAC",
                "type": "textarea"
            },
            {
                "id": "error_handling",
                "question": "Error Handling 전략은?",
                "help": "예: 400 Bad Request (유효성 검증 실패), 409 Conflict (재고 부족)",
                "type": "textarea"
            }
        ])

    return questions


def load_smart_questions():
    """
    Claude가 분석한 대화 기록에서 질문 로드

    Returns:
        질문 배열 또는 None
    """
    analysis_path = Path.home() / ".claude" / "tmp" / "conversation-analysis.json"

    if not analysis_path.exists():
        return None

    with open(analysis_path, 'r', encoding='utf-8') as f:
        analysis = json.load(f)

    # missing 정보를 질문으로 변환
    questions = []
    for item in analysis.get("missing", []):
        questions.append({
            "id": item["id"],
            "question": item["question"],
            "help": item.get("help", ""),
            "type": item.get("type", "textarea")
        })

    return questions


def main():
    """
    메인 실행 함수
    """
    # 인자 파싱
    question_type = sys.argv[1] if len(sys.argv) > 1 else "domain"

    # 질문 데이터 생성
    if question_type == "smart":
        # Smart 모드: 대화 기록 분석 결과 로드
        questions = load_smart_questions()
        if questions is None:
            print("❌ Error: conversation-analysis.json not found", file=sys.stderr)
            print("Claude가 먼저 대화 기록을 분석해야 합니다.", file=sys.stderr)
            sys.exit(1)
        print(f"🧠 Smart 모드: {len(questions)}개 질문 생성됨")
    else:
        # 기본 모드: 고정 질문
        questions = get_default_questions(question_type)
        print(f"📋 기본 모드: {len(questions)}개 질문")

    # HTML 파일 생성
    output_dir = Path.home() / ".claude" / "tmp"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "tdd-form.html"

    generate_html_with_questions(questions, output_path)

    # 출력 경로 반환 (Claude가 읽을 수 있도록)
    print(output_path)


if __name__ == "__main__":
    main()
