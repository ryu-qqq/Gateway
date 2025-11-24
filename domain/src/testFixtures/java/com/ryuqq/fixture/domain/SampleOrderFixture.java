package com.ryuqq.connectly.fixture.domain;

import com.ryuqq.connectly.domain.sample.aggregate.SampleOrder;
import com.ryuqq.connectly.domain.sample.aggregate.SampleOrderItem;
import com.ryuqq.connectly.domain.sample.vo.SampleMoney;
import com.ryuqq.connectly.domain.sample.vo.SampleOrderId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SampleOrder Aggregate Test Fixture (domain testFixtures)
 *
 * <p><strong>Fixture 패턴:</strong>
 *
 * <ul>
 *   <li>✅ forNew(): 새 Entity 생성 (ID = 0)
 *   <li>✅ of(): 특정 값으로 테스트 데이터 생성
 *   <li>✅ reconstitute(): DB에서 조회한 것처럼 테스트 데이터 생성
 *   <li>❌ create*() 메서드 금지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class SampleOrderFixture {

    private SampleOrderFixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * Factory Method - 새 Order 생성 (기본 값)
     *
     * <p>ID가 0인 새 Order를 생성합니다.
     *
     * @param clock Clock 인스턴스
     * @return SampleOrder 인스턴스 (새 Entity)
     */
    public static SampleOrder forNew(Clock clock) {
        return SampleOrder.forNew(
                clock,
                1000L, // customerId
                defaultItems());
    }

    /**
     * Factory Method - 새 Order 생성 (customerId 지정)
     *
     * @param clock Clock 인스턴스
     * @param customerId 고객 ID
     * @return SampleOrder 인스턴스 (새 Entity)
     */
    public static SampleOrder forNew(Clock clock, long customerId) {
        return SampleOrder.forNew(clock, customerId, defaultItems());
    }

    /**
     * Factory Method - 새 Order 생성 (items 지정)
     *
     * @param clock Clock 인스턴스
     * @param customerId 고객 ID
     * @param items Order Items
     * @return SampleOrder 인스턴스 (새 Entity)
     */
    public static SampleOrder forNew(Clock clock, long customerId, List<SampleOrderItem> items) {
        return SampleOrder.forNew(clock, customerId, items);
    }

    /**
     * Factory Method - 알려진 값으로 Order 생성
     *
     * @param clock Clock 인스턴스
     * @param orderId Order ID
     * @param customerId 고객 ID
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder of(Clock clock, SampleOrderId orderId, long customerId) {
        return SampleOrder.of(clock, orderId, customerId, defaultItems());
    }

    /**
     * Factory Method - 알려진 값으로 Order 생성 (items 지정)
     *
     * @param clock Clock 인스턴스
     * @param orderId Order ID
     * @param customerId 고객 ID
     * @param items Order Items
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder of(
            Clock clock, SampleOrderId orderId, long customerId, List<SampleOrderItem> items) {
        return SampleOrder.of(clock, orderId, customerId, items);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션
     *
     * <p>DB에서 조회한 Order를 재구성하는 경우를 시뮬레이션합니다.
     *
     * @param clock Clock 인스턴스
     * @param orderId Order ID
     * @param customerId 고객 ID
     * @param items Order Items
     * @param createdAt 생성 시각
     * @param updatedAt 수정 시각
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder reconstitute(
            Clock clock,
            SampleOrderId orderId,
            long customerId,
            List<SampleOrderItem> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return SampleOrder.reconstitute(clock, orderId, customerId, items, createdAt, updatedAt);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션 (기본 timestamps)
     *
     * @param clock Clock 인스턴스
     * @param orderId Order ID
     * @param customerId 고객 ID
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder reconstitute(Clock clock, SampleOrderId orderId, long customerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return SampleOrder.reconstitute(clock, orderId, customerId, defaultItems(), now, now);
    }

    // ==================== Helper Methods ====================

    /** 기본 Order Items 생성 */
    private static List<SampleOrderItem> defaultItems() {
        List<SampleOrderItem> items = new ArrayList<>();
        items.add(
                SampleOrderItem.forNew(
                        1001L, // productId
                        2, // quantity
                        SampleMoney.of(10000L) // price
                        ));
        return items;
    }
}
