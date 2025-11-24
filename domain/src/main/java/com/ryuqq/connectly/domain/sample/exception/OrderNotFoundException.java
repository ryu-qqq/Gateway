package com.ryuqq.connectly.domain.sample.exception;

import com.ryuqq.connectly.domain.common.exception.DomainException;
import com.ryuqq.connectly.domain.sample.vo.SampleOrderId;

/**
 * Sample OrderNotFound Exception (예시)
 *
 * <p>주문을 찾을 수 없을 때 발생하는 Domain Exception 예시입니다.
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong>
 *
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: OrderNotFoundException (그대로 유지)
 * </pre>
 *
 * <p><strong>Domain Exception 규칙:</strong>
 *
 * <ul>
 *   <li>✅ DomainException 상속
 *   <li>✅ ErrorCode enum 사용
 *   <li>✅ 명확한 에러 메시지
 *   <li>✅ 컨텍스트 정보 포함 (OrderId 등)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 1. 조회 실패 시
 * public Order findById(OrderId orderId) {
 *     return orderRepository.findById(orderId)
 *         .orElseThrow(() -> new OrderNotFoundException(orderId));
 * }
 *
 * // 2. Exception 처리 (Adapter Layer)
 * @RestControllerAdvice
 * public class GlobalExceptionHandler {
 *     @ExceptionHandler(OrderNotFoundException.class)
 *     public ResponseEntity<ErrorResponse> handle(OrderNotFoundException e) {
 *         return ResponseEntity
 *             .status(HttpStatus.NOT_FOUND)
 *             .body(new ErrorResponse(e.code(), e.getMessage()));
 *     }
 * }
 * }</pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see DomainException
 * @see OrderErrorCode
 */
public class OrderNotFoundException extends DomainException {

    private static final String MESSAGE_FORMAT = "Order not found: %s";

    /**
     * 생성자
     *
     * @param orderId 찾을 수 없는 주문 ID
     */
    public OrderNotFoundException(SampleOrderId orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND.getCode(), MESSAGE_FORMAT.formatted(orderId.value()));
    }
}
