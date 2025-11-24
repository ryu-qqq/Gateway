#!/usr/bin/env python3
"""
대화 기록 분석 - 이미 언급된 정보와 부족한 정보 파악

Claude가 이 스크립트를 직접 호출하지 않고,
대화 기록을 분석한 후 결과를 이 형식으로 저장함
"""

import json
from pathlib import Path
from typing import Dict, List

# PRD 필수 항목 체크리스트
PRD_CHECKLIST = {
    "domain": {
        "domain_name": {
            "question": "Domain 이름은 무엇인가요?",
            "help": "예: Order, User, Product",
            "keywords": ["domain", "aggregate", "entity", "도메인", "엔티티"]
        },
        "aggregate_properties": {
            "question": "Aggregate의 주요 속성은 무엇인가요?",
            "help": "예: orderId: OrderId, customerId: Long, status: OrderStatus, totalPrice: BigDecimal",
            "keywords": ["속성", "property", "field", "필드", "변수"]
        },
        "business_rules": {
            "question": "핵심 비즈니스 규칙은 무엇인가요?",
            "help": "예: 주문은 PLACED 상태에서만 취소 가능, 재고 부족 시 주문 거절",
            "keywords": ["규칙", "rule", "제약", "constraint", "조건"]
        },
        "value_objects": {
            "question": "Value Object는 어떤 것들이 필요한가요?",
            "help": "예: OrderId (UUID), OrderStatus (Enum), Money",
            "keywords": ["value object", "vo", "enum", "불변 객체"]
        },
        "state_transitions": {
            "question": "상태 전환 흐름은 어떻게 되나요?",
            "help": "예: PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED",
            "keywords": ["상태", "state", "status", "전환", "transition", "흐름"]
        }
    },
    "application": {
        "usecase_list": {
            "question": "필요한 UseCase는 무엇인가요?",
            "help": "예: PlaceOrderUseCase, CancelOrderUseCase, GetOrderUseCase",
            "keywords": ["usecase", "기능", "작업", "command", "query"]
        },
        "command_dto": {
            "question": "Command DTO는 어떻게 정의하나요?",
            "help": "예: PlaceOrderCommand(customerId, productId, quantity, deliveryAddress)",
            "keywords": ["command", "dto", "input", "request", "입력"]
        },
        "transaction_boundary": {
            "question": "Transaction 경계는 어떻게 설정하나요?",
            "help": "예: 주문 생성 + 재고 차감만 트랜잭션, 결제는 트랜잭션 밖",
            "keywords": ["transaction", "트랜잭션", "경계", "boundary", "commit", "rollback"]
        },
        "external_api": {
            "question": "외부 API 호출이 필요한가요?",
            "help": "예: 결제 Gateway (Stripe, Toss), 배송 API, 알림 서비스",
            "keywords": ["외부 api", "external", "gateway", "결제", "배송", "알림"]
        }
    },
    "persistence": {
        "jpa_entity": {
            "question": "JPA Entity는 어떻게 설계하나요?",
            "help": "예: OrderJpaEntity, OrderLineJpaEntity (테이블 구조)",
            "keywords": ["jpa", "entity", "table", "테이블", "엔티티"]
        },
        "querydsl_queries": {
            "question": "복잡한 조회 쿼리가 필요한가요?",
            "help": "예: 고객별 주문 조회, 상태별 주문 통계, 기간별 매출 집계",
            "keywords": ["query", "조회", "querydsl", "검색", "통계"]
        },
        "index_strategy": {
            "question": "인덱스 전략은 어떻게 설정하나요?",
            "help": "예: idx_customer_id_created_at (customer_id, created_at DESC)",
            "keywords": ["index", "인덱스", "성능", "optimization", "최적화"]
        },
        "concurrency_control": {
            "question": "동시성 제어는 어떻게 하나요?",
            "help": "예: Optimistic Lock (@Version), Pessimistic Lock (SELECT FOR UPDATE)",
            "keywords": ["lock", "동시성", "concurrency", "race condition", "충돌"]
        }
    },
    "rest_api": {
        "api_endpoints": {
            "question": "API 엔드포인트는 어떻게 정의하나요?",
            "help": "예: POST /api/v1/orders, GET /api/v1/orders/{orderId}, DELETE /api/v1/orders/{orderId}",
            "keywords": ["api", "endpoint", "rest", "url", "path", "엔드포인트"]
        },
        "request_dto": {
            "question": "Request DTO는 어떻게 정의하나요?",
            "help": "예: PlaceOrderRequest(customerId, productId, quantity, deliveryAddress)",
            "keywords": ["request", "dto", "입력", "validation", "검증"]
        },
        "auth_strategy": {
            "question": "인증/인가 전략은 무엇인가요?",
            "help": "예: JWT (Access Token + Refresh Token), RBAC, OAuth2",
            "keywords": ["인증", "auth", "jwt", "token", "인가", "권한"]
        },
        "error_handling": {
            "question": "Error Handling은 어떻게 하나요?",
            "help": "예: 400 Bad Request, 404 Not Found, 409 Conflict (재고 부족)",
            "keywords": ["error", "exception", "에러", "예외", "처리"]
        }
    }
}


def save_analysis_result(mentioned_info: Dict, missing_info: List[Dict], output_path: str):
    """
    분석 결과를 JSON 파일로 저장

    Args:
        mentioned_info: 이미 언급된 정보
        missing_info: 부족한 정보 (질문 목록)
        output_path: 출력 파일 경로
    """
    result = {
        "mentioned": mentioned_info,
        "missing": missing_info,
        "total_questions": len(missing_info)
    }

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"✅ 분석 결과 저장: {output_path}")
    print(f"📊 이미 언급됨: {len(mentioned_info)} 항목")
    print(f"❓ 질문 필요: {len(missing_info)} 개")


def generate_questions_from_missing(missing_info: List[Dict]) -> List[Dict]:
    """
    부족한 정보를 바탕으로 질문 생성

    Args:
        missing_info: 부족한 정보 리스트

    Returns:
        질문 배열 (HTML Form 형식)
    """
    questions = []

    for idx, item in enumerate(missing_info, 1):
        questions.append({
            "id": item["id"],
            "question": item["question"],
            "help": item["help"],
            "type": "textarea" if "detail" in item["id"] or "rules" in item["id"] else "text"
        })

    return questions


if __name__ == "__main__":
    # 예시: Claude가 분석한 결과를 여기 저장
    mentioned = {
        "domain_name": "Order",
        "usecase_list": "생성, 취소, 수정",
        "external_api": "Stripe (결제)",
        "business_rules": "재고 즉시 확인, 부족 시 거절"
    }

    missing = [
        {
            "id": "aggregate_properties",
            "question": "Aggregate의 주요 속성은 무엇인가요?",
            "help": "예: orderId: OrderId, customerId: Long, status: OrderStatus, totalPrice: BigDecimal"
        },
        {
            "id": "value_objects",
            "question": "Value Object는 어떤 것들이 필요한가요?",
            "help": "예: OrderId (UUID), OrderStatus (Enum), Money"
        },
        {
            "id": "state_transitions",
            "question": "상태 전환 흐름은 어떻게 되나요?",
            "help": "예: PENDING → PLACED → CONFIRMED → SHIPPED → DELIVERED"
        },
        {
            "id": "transaction_boundary",
            "question": "Transaction 경계는 어떻게 설정하나요?",
            "help": "예: 주문 생성 + 재고 차감만 트랜잭션, 결제는 트랜잭션 밖"
        }
    ]

    output_dir = Path.home() / ".claude" / "tmp"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / "conversation-analysis.json"

    save_analysis_result(mentioned, missing, str(output_path))

    # 질문 생성
    questions = generate_questions_from_missing(missing)
    print(f"\n📝 생성된 질문: {len(questions)}개")
    for q in questions:
        print(f"  - {q['question']}")
