package com.company.connectly.domain.sample.vo;

import com.company.connectly.domain.common.model.Identifier;

/**
 * Sample OrderItem Identifier (예시)
 *
 * <p>이 클래스는 OrderItem Entity의 Identifier 예시입니다.</p>
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong></p>
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: SampleOrderItemId → OrderItemId
 * </pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see Identifier
 */
public record SampleOrderItemId(Long value) implements Identifier<Long> {

    public SampleOrderItemId {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("OrderItemId must be non-negative");
        }
    }

    /**
     * Factory Method - 새 OrderItem ID 생성
     *
     * <p>저장되지 않은 새 Entity 생성 시 사용합니다.</p>
     *
     * @return value가 0인 OrderItemId
     */
    public static SampleOrderItemId forNew() {
        return new SampleOrderItemId(0L);
    }

    /**
     * Factory Method - 알려진 값으로 생성
     *
     * @param value ID 값
     * @return OrderItemId 인스턴스
     */
    public static SampleOrderItemId of(long value) {
        return new SampleOrderItemId(value);
    }

    /**
     * 새 Entity 여부 확인
     *
     * <p>DB에 저장되지 않은 새 Entity인지 확인합니다.</p>
     *
     * @return value가 0 또는 null이면 true
     */
    public boolean isNew() {
        return value == null || value == 0L;
    }
}
