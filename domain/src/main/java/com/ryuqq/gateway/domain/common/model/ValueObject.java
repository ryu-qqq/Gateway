package com.ryuqq.gateway.domain.common.model;

/**
 * Value Object 마커 인터페이스
 *
 * <p>DDD(Domain-Driven Design)에서 Value Object를 나타냅니다. Value Object는 식별자 없이 속성 값으로만 구분되며, 완전히 불변입니다.
 *
 * <p><strong>Value Object 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 완전 불변 (Immutable)
 *   <li>✅ 식별자 없음 (값으로만 동일성 판단)
 *   <li>✅ equals/hashCode는 모든 속성 기반
 *   <li>✅ Java Record 사용 권장
 *   <li>✅ of() Factory Method 제공
 *   <li>✅ 생성자에서 유효성 검증
 * </ul>
 *
 * <p><strong>Value Object 특징:</strong>
 *
 * <table border="1">
 *   <tr>
 *     <th>특징</th>
 *     <th>설명</th>
 *     <th>예시</th>
 *   </tr>
 *   <tr>
 *     <td>불변성</td>
 *     <td>생성 후 변경 불가</td>
 *     <td>Money(1000) → 새 객체 생성</td>
 *   </tr>
 *   <tr>
 *     <td>교체 가능</td>
 *     <td>같은 값이면 교체 가능</td>
 *     <td>Money(1000) == Money(1000)</td>
 *   </tr>
 *   <tr>
 *     <td>자가 검증</td>
 *     <td>생성자에서 유효성 보장</td>
 *     <td>Email("invalid") → 예외</td>
 *   </tr>
 * </table>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * // 1. 단순 VO (Record 사용)
 * public record Money(BigDecimal amount) implements ValueObject {
 *     public Money {
 *         if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
 *             throw new IllegalArgumentException("Amount must be positive");
 *         }
 *     }
 *
 *     public static Money of(long amount) {
 *         return new Money(BigDecimal.valueOf(amount));
 *     }
 *
 *     // 비즈니스 로직
 *     public Money add(Money other) {
 *         return new Money(this.amount.add(other.amount));
 *     }
 * }
 *
 * // 2. 복잡한 VO
 * public record Email(String value) implements ValueObject {
 *     private static final Pattern EMAIL_PATTERN =
 *         Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
 *
 *     public Email {
 *         if (value == null || value.isBlank()) {
 *             throw new IllegalArgumentException("Email cannot be blank");
 *         }
 *         if (!EMAIL_PATTERN.matcher(value).matches()) {
 *             throw new IllegalArgumentException("Invalid email format");
 *         }
 *     }
 *
 *     public static Email of(String value) {
 *         return new Email(value);
 *     }
 *
 *     public String domain() {
 *         return value.substring(value.indexOf('@') + 1);
 *     }
 * }
 *
 * // 3. Identifier VO
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
 * }</pre>
 *
 * <p><strong>Record 사용 시 자동 제공:</strong>
 *
 * <ul>
 *   <li>불변 필드 (final)
 *   <li>생성자 (Compact Constructor 사용 가능)
 *   <li>equals/hashCode (모든 필드 기반)
 *   <li>toString
 *   <li>Getter (필드명과 동일)
 * </ul>
 *
 * <p><strong>ArchUnit 검증:</strong>
 *
 * <ul>
 *   <li>VO는 반드시 Record 타입
 *   <li>of() Factory Method 필수
 *   <li>생성자에서 유효성 검증 필수
 *   <li>Lombok 어노테이션 금지
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 * @see Entity
 * @see Identifier
 */
public interface ValueObject {
    // Marker interface - no methods
}
