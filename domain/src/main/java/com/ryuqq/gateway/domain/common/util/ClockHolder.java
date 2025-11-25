package com.ryuqq.gateway.domain.common.util;

import java.time.Clock;

/**
 * Clock 제공자 인터페이스 (DIP - Dependency Inversion Principle)
 *
 * <p>Domain Layer는 구현체를 모르고, 이 인터페이스에만 의존합니다. 구현체는 Application/Infrastructure Layer에서 제공됩니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>✅ Domain은 ClockHolder 인터페이스에만 의존
 *   <li>✅ Application/Infrastructure에서 구현체 제공 (SystemClockHolder, FixedClockHolder)
 *   <li>✅ Aggregate는 생성자에서 Clock 파라미터로 받음
 *   <li>✅ Assembler/Mapper는 ClockHolder를 주입받아 Clock 제공
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // Assembler (Application)
 * @Component
 * public class OrderAssembler {
 *     private final ClockHolder clockHolder;
 *
 *     public Order toAggregate(PlaceOrderCommand command) {
 *         return Order.forNew(
 *             clockHolder.getClock(),  // Clock 제공
 *             command.amount()
 *         );
 *     }
 * }
 *
 * // Aggregate (Domain)
 * public class Order {
 *     private final Clock clock;
 *     private final LocalDateTime createdAt;
 *
 *     private Order(Clock clock, ...) {
 *         this.clock = clock;
 *         this.createdAt = LocalDateTime.now(clock);
 *     }
 *
 *     public static Order forNew(Clock clock, Money amount) {
 *         return new Order(clock, amount);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>테스트 시:</strong>
 *
 * <pre>{@code
 * // FixedClockHolder (Test)
 * ClockHolder fixedClockHolder = () -> Clock.fixed(
 *     Instant.parse("2025-01-01T00:00:00Z"),
 *     ZoneId.of("UTC")
 * );
 *
 * Order order = Order.forNew(fixedClockHolder.getClock(), Money.of(10000));
 * assertThat(order.createdAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
 * }</pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 */
public interface ClockHolder {

    /**
     * Clock 반환
     *
     * <p>Aggregate 생성 시 시간 정보를 제공하기 위해 사용됩니다.
     *
     * @return Clock 인스턴스 (System Clock 또는 Fixed Clock)
     * @author ryu-qqq
     * @since 2025-11-21
     */
    Clock clock();
}
