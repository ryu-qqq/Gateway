package com.company.connectly.domain.sample.exception;

import com.company.connectly.domain.common.exception.ErrorCode;

/**
 * Sample Order ErrorCode (예시)
 *
 * <p>Order Bounded Context의 에러 코드 정의입니다.</p>
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong></p>
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: OrderErrorCode (그대로 유지)
 * 3. 실제 비즈니스 에러 코드 추가
 * </pre>
 *
 * <p><strong>ErrorCode 규칙:</strong></p>
 * <ul>
 *   <li>✅ ErrorCode 인터페이스 구현</li>
 *   <li>✅ Enum 타입으로 정의</li>
 *   <li>✅ HTTP 상태 코드는 int 타입 (Spring HttpStatus 의존성 금지)</li>
 *   <li>✅ 명확한 에러 메시지</li>
 *   <li>✅ 일관된 코드 형식 (ORDER-001, ORDER-002 등)</li>
 * </ul>
 *
 * <p><strong>사용 예시:</strong></p>
 * <pre>{@code
 * // 1. Exception에서 사용
 * public class OrderNotFoundException extends DomainException {
 *     public OrderNotFoundException(OrderId orderId) {
 *         super(OrderErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
 *     }
 * }
 *
 * // 2. Adapter Layer에서 매핑
 * @RestControllerAdvice
 * public class GlobalExceptionHandler {
 *     @ExceptionHandler(DomainException.class)
 *     public ResponseEntity<ErrorResponse> handle(DomainException e) {
 *         ErrorCode errorCode = e.getErrorCode();
 *         return ResponseEntity
 *             .status(errorCode.getHttpStatus())
 *             .body(new ErrorResponse(errorCode.getCode(), e.getMessage()));
 *     }
 * }
 * }</pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see ErrorCode
 */
public enum OrderErrorCode implements ErrorCode {

    /**
     * 주문을 찾을 수 없음
     */
    ORDER_NOT_FOUND("ORDER-001", 404, "주문을 찾을 수 없습니다"),

    /**
     * 이미 취소된 주문
     */
    ORDER_ALREADY_CANCELLED("ORDER-002", 400, "이미 취소된 주문입니다"),

    /**
     * 주문 항목이 없음
     */
    ORDER_ITEMS_EMPTY("ORDER-003", 400, "주문 항목이 비어있습니다"),

    /**
     * 잘못된 주문 수량
     */
    INVALID_ORDER_QUANTITY("ORDER-004", 400, "주문 수량이 잘못되었습니다"),

    /**
     * 주문 금액이 최소 금액 미만
     */
    ORDER_AMOUNT_TOO_LOW("ORDER-005", 400, "주문 금액이 최소 금액 미만입니다");

    private final String code;
    private final int httpStatus;
    private final String message;

    /**
     * 생성자
     *
     * @param code 에러 코드 (ORDER-001, ORDER-002 등)
     * @param httpStatus HTTP 상태 코드 (404, 400 등)
     * @param message 에러 메시지
     */
    OrderErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
