package com.company.connectly.domain.sample.vo;

import com.company.connectly.domain.common.model.Identifier;

/**
 * Sample Order Identifier (예시)
 *
 * <p>이 클래스는 Order Aggregate의 Identifier 예시입니다.</p>
 *
 * <p><strong>TODO: 실제 프로젝트에 맞게 수정</strong></p>
 * <pre>
 * 1. 패키지명 변경: com.ryuqq.domain.sample → com.ryuqq.domain.order
 * 2. 클래스명 변경: SampleOrderId → OrderId
 * </pre>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see Identifier
 */
public record SampleOrderId(Long value) implements Identifier<Long> {

    public SampleOrderId {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("OrderId must be non-negative");
        }
    }

    /**
     * Factory Method - 새 Order ID 생성
     *
     * <p>저장되지 않은 새 Aggregate 생성 시 사용합니다.</p>
     *
     * @return value가 0인 OrderId
     */
    public static SampleOrderId forNew() {
        return new SampleOrderId(0L);
    }

    /**
     * Factory Method - 알려진 값으로 생성
     *
     * @param value ID 값
     * @return OrderId 인스턴스
     */
    public static SampleOrderId of(long value) {
        return new SampleOrderId(value);
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
