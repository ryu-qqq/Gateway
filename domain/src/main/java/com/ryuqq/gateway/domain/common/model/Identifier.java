package com.ryuqq.gateway.domain.common.model;

/**
 * Identifier Value Object 인터페이스
 *
 * <p>Entity의 식별자를 나타내는 특수한 Value Object입니다. 타입 안전한 ID를 제공하여 Long, String 등 원시 타입 사용을 방지합니다.
 *
 * <p><strong>Identifier 규칙:</strong>
 *
 * <ul>
 *   <li>✅ ValueObject 인터페이스 확장
 *   <li>✅ 단일 value 필드 (Long, String, UUID 등)
 *   <li>✅ 불변 (Immutable)
 *   <li>✅ Java Record 사용 권장
 *   <li>✅ of() Factory Method 필수
 *   <li>✅ 타입별로 별도 클래스 (OrderId, CustomerId 등)
 * </ul>
 *
 * <p><strong>Identifier의 장점:</strong>
 *
 * <table border="1">
 *   <tr>
 *     <th>문제</th>
 *     <th>원시 타입 (Long)</th>
 *     <th>Identifier (OrderId)</th>
 *   </tr>
 *   <tr>
 *     <td>타입 안전성</td>
 *     <td>❌ findById(customerId) // 컴파일 성공</td>
 *     <td>✅ findById(customerId) // 컴파일 에러</td>
 *   </tr>
 *   <tr>
 *     <td>의미 명확성</td>
 *     <td>❌ Long id (무엇의 ID?)</td>
 *     <td>✅ OrderId (주문 ID 명확)</td>
 *   </tr>
 *   <tr>
 *     <td>유효성 검증</td>
 *     <td>❌ 검증 로직 분산</td>
 *     <td>✅ 생성자에서 중앙 검증</td>
 *   </tr>
 * </table>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 1. Long 기반 Identifier
 * public record OrderId(long value) implements Identifier<Long> {
 *     public OrderId {
 *         if (value <= 0) {
 *             throw new IllegalArgumentException("OrderId must be positive");
 *         }
 *     }
 *
 *     public static OrderId of(long value) {
 *         return new OrderId(value);
 *     }
 * }
 *
 * // 2. String 기반 Identifier
 * public record ProductCode(String value) implements Identifier<String> {
 *     private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{3}-\\d{6}$");
 *
 *     public ProductCode {
 *         if (value == null || !CODE_PATTERN.matcher(value).matches()) {
 *             throw new IllegalArgumentException("Invalid product code format");
 *         }
 *     }
 *
 *     public static ProductCode of(String value) {
 *         return new ProductCode(value);
 *     }
 * }
 *
 * // 3. UUID 기반 Identifier
 * public record TransactionId(UUID value) implements Identifier<UUID> {
 *     public TransactionId {
 *         if (value == null) {
 *             throw new IllegalArgumentException("TransactionId cannot be null");
 *         }
 *     }
 *
 *     public static TransactionId of(UUID value) {
 *         return new TransactionId(value);
 *     }
 *
 *     public static TransactionId generate() {
 *         return new TransactionId(UUID.randomUUID());
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Entity에서 사용:</strong>
 *
 * <pre>{@code
 * public class Order implements AggregateRoot {
 *     private final OrderId id;  // ← Identifier VO
 *     private final CustomerId customerId;  // ← FK도 Identifier VO
 *     private final Money totalAmount;
 *
 *     private Order(OrderId id, CustomerId customerId, Money totalAmount) {
 *         this.id = id;
 *         this.customerId = customerId;
 *         this.totalAmount = totalAmount;
 *     }
 *
 *     // Factory Method
 *     public static Order forNew(Clock clock, CustomerId customerId, Money amount) {
 *         return new Order(
 *             OrderId.of(0L),  // 생성 시 0, 저장 시 할당
 *             customerId,
 *             amount
 *         );
 *     }
 *
 *     // Getter
 *     public OrderId id() { return id; }
 *     public CustomerId customerId() { return customerId; }
 * }
 * }</pre>
 *
 * <p><strong>Long FK 전략과 조합:</strong>
 *
 * <pre>{@code
 * // ❌ JPA 관계 어노테이션 금지
 * @ManyToOne
 * private Customer customer;
 *
 * // ✅ Long FK with Identifier
 * private final CustomerId customerId;  // Long을 감싼 타입 안전 ID
 * }</pre>
 *
 * <p><strong>Persistence Layer 변환:</strong>
 *
 * <pre>{@code
 * // Domain
 * public record OrderId(long value) implements Identifier<Long> { }
 *
 * // JPA Entity
 * @Entity
 * public class OrderEntity {
 *     @Id
 *     private Long id;  // ← OrderId.value() 저장
 * }
 *
 * // Mapper
 * public class OrderMapper {
 *     public Order toDomain(OrderEntity entity) {
 *         return Order.reconstitute(
 *             OrderId.of(entity.getId()),  // Long → OrderId
 *             // ...
 *         );
 *     }
 *
 *     public OrderEntity toEntity(Order domain) {
 *         return new OrderEntity(
 *             domain.id().value(),  // OrderId → Long
 *             // ...
 *         );
 *     }
 * }
 * }</pre>
 *
 * @param <T> Identifier의 실제 타입 (Long, String, UUID 등)
 * @author ryu-qqq
 * @since 2025-11-21
 * @see ValueObject
 * @see Entity
 */
public interface Identifier<T> extends ValueObject {

    /**
     * Identifier의 실제 값 반환
     *
     * <p>Persistence Layer로의 변환 또는 외부 시스템 연동 시 사용됩니다.
     *
     * @return Identifier의 실제 값 (Long, String, UUID 등)
     */
    T value();
}
