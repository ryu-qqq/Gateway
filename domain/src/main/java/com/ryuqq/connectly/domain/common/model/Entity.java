package com.ryuqq.connectly.domain.common.model;

/**
 * Domain Entity 마커 인터페이스
 *
 * <p>DDD(Domain-Driven Design)에서 Entity를 나타냅니다. Entity는 식별자(Identity)로 구분되며, 생명주기 동안 속성이 변경될 수 있지만
 * 식별자는 불변입니다.
 *
 * <p><strong>⚠️ 중요: JPA @Entity와 다름!</strong>
 *
 * <ul>
 *   <li>Domain Entity: 비즈니스 로직 담당 (이 인터페이스)
 *   <li>JPA Entity: 데이터베이스 테이블 매핑 ({@code @Entity} 어노테이션)
 *   <li>Persistence Layer의 JPA Entity는 이 인터페이스를 구현하지 않음
 * </ul>
 *
 * <p><strong>Entity 특징:</strong>
 *
 * <ul>
 *   <li>✅ 식별자(ID)로 동일성 판단 (equals/hashCode는 ID 기반)
 *   <li>✅ 생명주기 존재 (생성 → 변경 → 삭제)
 *   <li>✅ 속성은 변경 가능하지만 ID는 불변
 *   <li>✅ 비즈니스 로직 포함 가능
 * </ul>
 *
 * <p><strong>Entity vs Value Object:</strong>
 *
 * <table border="1">
 *   <tr>
 *     <th>구분</th>
 *     <th>Entity</th>
 *     <th>Value Object</th>
 *   </tr>
 *   <tr>
 *     <td>식별자</td>
 *     <td>있음 (ID로 구분)</td>
 *     <td>없음 (값으로 구분)</td>
 *   </tr>
 *   <tr>
 *     <td>불변성</td>
 *     <td>속성 변경 가능</td>
 *     <td>완전 불변</td>
 *   </tr>
 *   <tr>
 *     <td>equals</td>
 *     <td>ID 기반</td>
 *     <td>모든 속성 기반</td>
 *   </tr>
 * </table>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // Aggregate Root는 자동으로 Entity
 * public class Order implements AggregateRoot {
 *     private final OrderId id;  // 식별자 (불변)
 *     private Money totalAmount; // 속성 (변경 가능)
 *
 *     // equals/hashCode는 ID 기반
 *     @Override
 *     public boolean equals(Object o) {
 *         if (this == o) return true;
 *         if (!(o instanceof Order)) return false;
 *         Order order = (Order) o;
 *         return Objects.equals(id, order.id);
 *     }
 *
 *     @Override
 *     public int hashCode() {
 *         return Objects.hash(id);
 *     }
 * }
 *
 * // Aggregate 내부의 Entity (Aggregate Root가 아님)
 * public class OrderItem implements Entity {
 *     private final OrderItemId id;
 *     private int quantity;  // 변경 가능
 *
 *     public void changeQuantity(int newQuantity) {
 *         this.quantity = newQuantity;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Aggregate 내부 Entity 주의사항:</strong>
 *
 * <ul>
 *   <li>Aggregate Root를 통해서만 접근 가능
 *   <li>외부에서 직접 참조 금지
 *   <li>Repository는 Aggregate Root에만 존재
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see AggregateRoot
 * @see ValueObject
 */
public interface Entity {
    // Marker interface - no methods
}
