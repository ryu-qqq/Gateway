package com.ryuqq.connectly.domain.sample.event;

import com.ryuqq.connectly.domain.common.event.DomainEvent;
import com.ryuqq.connectly.domain.sample.vo.SampleMoney;
import com.ryuqq.connectly.domain.sample.vo.SampleOrderId;
import java.time.LocalDateTime;

/**
 * Sample OrderPlaced Domain Event (예시)
 *
 * <p>주문 생성 시 발생하는 Domain Event 예시입니다.
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong>
 *
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: OrderPlacedEvent (그대로 유지)
 * 3. 필요한 데이터 추가
 * </pre>
 *
 * <p><strong>Domain Event 규칙:</strong>
 *
 * <ul>
 *   <li>✅ DomainEvent 인터페이스 구현
 *   <li>✅ Record 타입 (불변)
 *   <li>✅ 과거형 네이밍 (OrderPlaced, OrderCancelled 등)
 *   <li>✅ Aggregate ID 포함
 *   <li>✅ 이벤트 발생 시각 포함
 *   <li>✅ 이벤트 처리에 필요한 최소 데이터만 포함
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 1. Event 발행 (Aggregate)
 * public class Order {
 *     public static Order forNew(...) {
 *         Order order = new Order(...);
 *         // Event 발행 (Spring ApplicationEventPublisher 사용)
 *         publishEvent(new OrderPlacedEvent(...));
 *         return order;
 *     }
 * }
 *
 * // 2. Event 처리 (Application Layer)
 * @Component
 * public class OrderEventHandler {
 *     @EventListener
 *     public void handle(OrderPlacedEvent event) {
 *         // 재고 차감
 *         // 알림 전송
 *         // 통계 업데이트
 *     }
 * }
 * }</pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see DomainEvent
 */
public record OrderPlacedEvent(
        SampleOrderId orderId,
        long customerId,
        SampleMoney totalAmount,
        int itemCount,
        LocalDateTime occurredAt)
        implements DomainEvent {

    /**
     * Factory Method - 이벤트 생성
     *
     * @param orderId 주문 ID
     * @param customerId 고객 ID
     * @param totalAmount 총 금액
     * @param itemCount 항목 수
     * @return OrderPlacedEvent 인스턴스
     */
    public static OrderPlacedEvent of(
            SampleOrderId orderId, long customerId, SampleMoney totalAmount, int itemCount) {
        return new OrderPlacedEvent(
                orderId, customerId, totalAmount, itemCount, LocalDateTime.now());
    }

    // TODO: 추가 비즈니스 로직
    // 예: 고액 주문 여부, VIP 고객 여부 등
}
