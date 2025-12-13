package com.ryuqq.gateway.domain.common.vo;

/**
 * SortKey - 정렬 키 마커 인터페이스
 *
 * <p>각 Bounded Context에서 정렬 가능한 필드를 enum으로 정의할 때 구현합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>각 BC는 자신만의 SortKey enum을 정의
 *   <li>정렬 가능한 필드만 enum 값으로 노출
 *   <li>DB 컬럼명은 Adapter에서 매핑 (도메인 언어 유지)
 * </ul>
 *
 * <p><strong>구현 예시:</strong>
 *
 * <pre>{@code
 * // domain/order/vo/OrderSortKey.java
 * public enum OrderSortKey implements SortKey {
 *     ORDER_DATE("orderDate"),
 *     TOTAL_AMOUNT("totalAmount"),
 *     MEMBER_NAME("memberName");
 *
 *     private final String fieldName;
 *
 *     OrderSortKey(String fieldName) {
 *         this.fieldName = fieldName;
 *     }
 *
 *     @Override
 *     public String fieldName() {
 *         return fieldName;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Adapter에서 매핑:</strong>
 *
 * <pre>{@code
 * // adapter-out/persistence-mysql/.../OrderQueryAdapter.java
 * private OrderSpecifier<?> toOrderSpecifier(OrderSortKey sortKey, SortDirection direction) {
 *     Order order = direction.isAscending() ? Order.ASC : Order.DESC;
 *     return switch (sortKey) {
 *         case ORDER_DATE -> new OrderSpecifier<>(order, orderEntity.orderDate);
 *         case TOTAL_AMOUNT -> new OrderSpecifier<>(order, orderEntity.totalAmount);
 *         case MEMBER_NAME -> new OrderSpecifier<>(order, orderEntity.memberName);
 *     };
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface SortKey {

    /**
     * 정렬 필드명 반환
     *
     * <p>도메인 언어로 표현된 필드명입니다. 실제 DB 컬럼명으로의 매핑은 Adapter 레이어에서 수행합니다.
     *
     * @return 필드명 (예: "orderDate", "totalAmount")
     */
    String fieldName();

    /**
     * enum 이름 반환 (기본 구현)
     *
     * @return enum 상수 이름
     */
    default String name() {
        return this.toString();
    }
}
