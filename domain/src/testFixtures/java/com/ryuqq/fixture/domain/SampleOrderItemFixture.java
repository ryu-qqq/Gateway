package com.company.connectly.fixture.domain;

import com.company.connectly.domain.sample.aggregate.SampleOrderItem;
import com.company.connectly.domain.sample.vo.SampleMoney;
import com.company.connectly.domain.sample.vo.SampleOrderItemId;

/**
 * SampleOrderItem Entity Test Fixture (domain testFixtures)
 *
 * <p><strong>Fixture 패턴:</strong></p>
 * <ul>
 *   <li>✅ forNew(): 새 Entity 생성 (ID = 0)</li>
 *   <li>✅ of(): 특정 값으로 테스트 데이터 생성</li>
 *   <li>✅ reconstitute(): DB에서 조회한 것처럼 테스트 데이터 생성</li>
 *   <li>❌ create*() 메서드 금지</li>
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public class SampleOrderItemFixture {

    private SampleOrderItemFixture() {
        throw new AssertionError("Utility class");
    }

    /**
     * Factory Method - 새 OrderItem 생성 (기본 값)
     *
     * <p>ID가 0인 새 OrderItem을 생성합니다.</p>
     *
     * @return SampleOrderItem 인스턴스 (새 Entity)
     */
    public static SampleOrderItem forNew() {
        return SampleOrderItem.forNew(
            1001L, // productId
            2,     // quantity
            SampleMoney.of(10000L) // price
        );
    }

    /**
     * Factory Method - 새 OrderItem 생성 (값 지정)
     *
     * @param productId 상품 ID
     * @param quantity 수량
     * @param price 가격
     * @return SampleOrderItem 인스턴스 (새 Entity)
     */
    public static SampleOrderItem forNew(long productId, int quantity, SampleMoney price) {
        return SampleOrderItem.forNew(productId, quantity, price);
    }

    /**
     * Factory Method - 알려진 값으로 OrderItem 생성
     *
     * @param id OrderItem ID
     * @param productId 상품 ID
     * @param quantity 수량
     * @param price 가격
     * @return SampleOrderItem 인스턴스
     */
    public static SampleOrderItem of(
        SampleOrderItemId id,
        long productId,
        int quantity,
        SampleMoney price
    ) {
        return SampleOrderItem.of(id, productId, quantity, price);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션
     *
     * <p>DB에서 조회한 OrderItem을 재구성하는 경우를 시뮬레이션합니다.</p>
     *
     * @param id OrderItem ID
     * @param productId 상품 ID
     * @param quantity 수량
     * @param price 가격
     * @return SampleOrderItem 인스턴스
     */
    public static SampleOrderItem reconstitute(
        SampleOrderItemId id,
        long productId,
        int quantity,
        SampleMoney price
    ) {
        return SampleOrderItem.of(id, productId, quantity, price);
    }

    /**
     * Factory Method - DB 재구성 시뮬레이션 (기본 값)
     *
     * @param id OrderItem ID
     * @return SampleOrderItem 인스턴스
     */
    public static SampleOrderItem reconstitute(SampleOrderItemId id) {
        return SampleOrderItem.of(
            id,
            1001L, // productId
            2,     // quantity
            SampleMoney.of(10000L) // price
        );
    }
}
