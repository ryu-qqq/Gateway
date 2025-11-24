package com.ryuqq.connectly.domain.common.model;

/**
 * Aggregate Root 마커 인터페이스
 *
 * <p>DDD(Domain-Driven Design)에서 Aggregate의 Root Entity를 나타냅니다. Aggregate Root는 일관성 경계를 정의하고, 외부에서는
 * 반드시 Aggregate Root를 통해서만 Aggregate 내부에 접근할 수 있습니다.
 *
 * <p><strong>Aggregate Root 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 식별자(ID)를 가짐
 *   <li>✅ 생명주기 관리 (생성, 수정, 삭제)
 *   <li>✅ 트랜잭션 일관성 경계
 *   <li>✅ 불변식(Invariant) 보장
 *   <li>✅ Factory Method 제공 (forNew, of, reconstitute)
 *   <li>✅ 도메인 이벤트 발행 (선택적)
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * public class Order implements AggregateRoot {
 *     private final OrderId id;
 *     private final Money totalAmount;
 *     private final LocalDateTime createdAt;
 *
 *     private Order(OrderId id, Money totalAmount, LocalDateTime createdAt) {
 *         this.id = id;
 *         this.totalAmount = totalAmount;
 *         this.createdAt = createdAt;
 *     }
 *
 *     // Factory Method: 새 생성
 *     public static Order forNew(Clock clock, Money amount) {
 *         return new Order(
 *             OrderId.generate(),
 *             amount,
 *             LocalDateTime.now(clock)
 *         );
 *     }
 *
 *     // Factory Method: 재구성 (DB에서 조회)
 *     public static Order reconstitute(OrderId id, Money amount, LocalDateTime createdAt) {
 *         return new Order(id, amount, createdAt);
 *     }
 *
 *     // 비즈니스 로직
 *     public void cancel() {
 *         // 취소 로직
 *     }
 *
 *     // Getter (Tell, Don't Ask 원칙 준수)
 *     public OrderId id() { return id; }
 *     public Money totalAmount() { return totalAmount; }
 * }
 * }</pre>
 *
 * <p><strong>ArchUnit 검증:</strong>
 *
 * <ul>
 *   <li>Aggregate Root는 반드시 이 인터페이스를 구현해야 함
 *   <li>Private 생성자 필수
 *   <li>Factory Method (forNew, of, reconstitute) 필수
 *   <li>Clock 의존성 주입 (생성자 파라미터)
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see Entity
 * @see ValueObject
 */
public interface AggregateRoot {
    // Marker interface - no methods
}
