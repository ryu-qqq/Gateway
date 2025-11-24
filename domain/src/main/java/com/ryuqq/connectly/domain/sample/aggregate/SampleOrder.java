package com.ryuqq.connectly.domain.sample.aggregate;

import com.ryuqq.connectly.domain.common.model.AggregateRoot;
import com.ryuqq.connectly.domain.sample.vo.SampleMoney;
import com.ryuqq.connectly.domain.sample.vo.SampleOrderId;
import com.ryuqq.connectly.domain.sample.vo.SampleOrderItemId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sample Order Aggregate Root (예시)
 *
 * <p>이 클래스는 Order Bounded Context의 Aggregate Root 예시입니다.
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong>
 *
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: SampleOrder → Order
 * 3. 비즈니스 로직 추가 (결제, 배송 등)
 * 4. Domain Event 발행 추가
 * </pre>
 *
 * <p><strong>Aggregate Root 규칙:</strong>
 *
 * <ul>
 *   <li>✅ AggregateRoot 인터페이스 구현
 *   <li>✅ Private 생성자
 *   <li>✅ Factory Method (forNew, of, reconstitute)
 *   <li>✅ Clock 의존성 (생성자 파라미터)
 *   <li>✅ 불변식(Invariant) 보장
 *   <li>✅ Aggregate 내부 Entity 관리 (OrderItem)
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see AggregateRoot
 */
public class SampleOrder implements AggregateRoot {

    private final SampleOrderId id;
    private final long customerId; // Long FK (Customer Aggregate 참조)
    private final List<SampleOrderItem> items;
    private final Clock clock;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Private 생성자
     *
     * <p>Factory Method를 통해서만 생성 가능합니다.
     */
    private SampleOrder(
            SampleOrderId id,
            long customerId,
            List<SampleOrderItem> items,
            Clock clock,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.clock = clock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        validateInvariants();
    }

    /**
     * Factory Method - 새 주문 생성
     *
     * <p>주문 생성 시 사용합니다.
     *
     * @param clock Clock (ClockHolder에서 제공)
     * @param customerId 고객 ID (FK)
     * @param items 주문 항목 리스트
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder forNew(Clock clock, long customerId, List<SampleOrderItem> items) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new SampleOrder(
                SampleOrderId.of(0L), // 생성 시 0, 저장 시 할당
                customerId,
                items,
                clock,
                now,
                now);
    }

    /**
     * Factory Method - 알려진 값으로 생성
     *
     * <p>테스트나 Migration 시 사용합니다.
     *
     * @param clock Clock (ClockHolder에서 제공)
     * @param id 주문 ID
     * @param customerId 고객 ID (FK)
     * @param items 주문 항목 리스트
     * @return SampleOrder 인스턴스
     */
    public static SampleOrder of(
            Clock clock, SampleOrderId id, long customerId, List<SampleOrderItem> items) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new SampleOrder(id, customerId, items, clock, now, now);
    }

    /**
     * Factory Method - DB에서 재구성
     *
     * <p>Repository에서 조회 시 사용합니다.
     */
    public static SampleOrder reconstitute(
            Clock clock,
            SampleOrderId id,
            long customerId,
            List<SampleOrderItem> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new SampleOrder(id, customerId, items, clock, createdAt, updatedAt);
    }

    // ========================================
    // 비즈니스 로직
    // ========================================

    /**
     * 주문 항목 추가
     *
     * @param productId 상품 ID
     * @param quantity 수량
     * @param price 가격
     */
    public void addItem(long productId, int quantity, SampleMoney price) {
        SampleOrderItem item = SampleOrderItem.forNew(productId, quantity, price);
        items.add(item);
        updateTimestamp();
        validateInvariants();
    }

    /**
     * 주문 항목 제거
     *
     * @param itemId 제거할 항목 ID
     */
    public void removeItem(SampleOrderItemId itemId) {
        items.removeIf(item -> item.id().equals(itemId));
        updateTimestamp();
        validateInvariants();
    }

    /**
     * 주문 항목 수량 변경
     *
     * @param itemId 항목 ID
     * @param newQuantity 새 수량
     */
    public void changeItemQuantity(SampleOrderItemId itemId, int newQuantity) {
        SampleOrderItem item = findItem(itemId);
        item.changeQuantity(newQuantity);
        updateTimestamp();
    }

    /**
     * 타임스탬프 업데이트
     *
     * <p>Aggregate 상태 변경 시 호출됩니다.
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    /**
     * 주문 총 금액 계산
     *
     * @return 모든 항목의 합계 금액
     */
    public SampleMoney totalAmount() {
        return items.stream()
                .map(SampleOrderItem::totalAmount)
                .reduce(SampleMoney.zero(), SampleMoney::add);
    }

    /**
     * 불변식 검증
     *
     * <p>Aggregate의 일관성을 보장합니다.
     *
     * @throws IllegalStateException 불변식 위반 시
     */
    private void validateInvariants() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Order must have at least one item");
        }
        // TODO: 추가 불변식 검증
        // 예: 최소/최대 주문 금액, 재고 확인 등
    }

    private SampleOrderItem findItem(SampleOrderItemId itemId) {
        return items.stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
    }

    // ========================================
    // Getter (Tell, Don't Ask 원칙 준수)
    // ========================================

    public SampleOrderId id() {
        return id;
    }

    public long customerId() {
        return customerId;
    }

    /**
     * 주문 항목 조회
     *
     * <p>방어적 복사로 불변성 보장
     */
    public List<SampleOrderItem> items() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    // ========================================
    // equals/hashCode (ID 기반)
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SampleOrder that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // TODO: Domain Event 발행
    // 예: OrderPlacedEvent, OrderCancelledEvent 등
}
