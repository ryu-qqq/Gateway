package com.company.connectly.domain.sample.aggregate;

import com.company.connectly.domain.common.model.Entity;
import com.company.connectly.domain.sample.vo.SampleMoney;
import com.company.connectly.domain.sample.vo.SampleOrderItemId;

import java.util.Objects;

/**
 * Sample OrderItem Entity (예시)
 *
 * <p>Aggregate 내부 Entity 예시입니다. (AggregateRoot 아님)</p>
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong></p>
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: SampleOrderItem → OrderItem
 * 3. 비즈니스 로직 추가
 * </pre>
 *
 * <p><strong>Aggregate 내부 Entity 규칙:</strong></p>
 * <ul>
 *   <li>✅ Entity 인터페이스 구현 (AggregateRoot 아님!)</li>
 *   <li>✅ Private 생성자 (Factory Method 사용)</li>
 *   <li>✅ equals/hashCode는 ID 기반</li>
 *   <li>✅ 외부 접근 불가 (package-private 또는 public이지만 Aggregate Root를 통해서만)</li>
 *   <li>✅ Repository 없음</li>
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see Entity
 */
public class SampleOrderItem implements Entity {

    private final SampleOrderItemId id;
    private final long productId;  // Long FK (Product Aggregate 참조)
    private int quantity;
    private final SampleMoney price;

    /**
     * Private 생성자
     *
     * <p>Factory Method를 통해서만 생성 가능합니다.</p>
     */
    private SampleOrderItem(
        SampleOrderItemId id,
        long productId,
        int quantity,
        SampleMoney price
    ) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Factory Method - 새 OrderItem 생성
     *
     * <p>Order가 생성될 때 사용합니다.</p>
     *
     * @param productId 상품 ID (FK)
     * @param quantity 수량
     * @param price 가격
     * @return SampleOrderItem 인스턴스
     */
    public static SampleOrderItem forNew(long productId, int quantity, SampleMoney price) {
        validateQuantity(quantity);
        return new SampleOrderItem(
            SampleOrderItemId.of(0L),  // 생성 시 0, 저장 시 할당
            productId,
            quantity,
            price
        );
    }

    /**
     * Factory Method - 알려진 값으로 생성
     *
     * <p>테스트나 알려진 ID로 OrderItem을 생성할 때 사용합니다.</p>
     */
    public static SampleOrderItem of(
        SampleOrderItemId id,
        long productId,
        int quantity,
        SampleMoney price
    ) {
        validateQuantity(quantity);
        return new SampleOrderItem(id, productId, quantity, price);
    }

    /**
     * Factory Method - DB에서 재구성
     *
     * <p>Repository에서 조회 시 사용합니다.</p>
     */
    public static SampleOrderItem reconstitute(
        SampleOrderItemId id,
        long productId,
        int quantity,
        SampleMoney price
    ) {
        return new SampleOrderItem(id, productId, quantity, price);
    }

    // ========================================
    // 비즈니스 로직
    // ========================================

    /**
     * 수량 변경
     *
     * <p>Order를 통해서만 호출되어야 합니다.</p>
     *
     * @param newQuantity 새 수량
     */
    public void changeQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    /**
     * 항목 총 금액 계산
     *
     * @return 수량 * 가격
     */
    public SampleMoney totalAmount() {
        return price.multiply(quantity);
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    // ========================================
    // Getter (Tell, Don't Ask 원칙 준수)
    // ========================================

    public SampleOrderItemId id() {
        return id;
    }

    public long productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    public SampleMoney price() {
        return price;
    }

    // ========================================
    // equals/hashCode (ID 기반)
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SampleOrderItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // TODO: 추가 비즈니스 로직
    // 예: 할인 적용, 재고 확인 등
}
