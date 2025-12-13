package com.ryuqq.gateway.domain.common.vo;

/**
 * 분산락 키 인터페이스
 *
 * <p>Redis 분산락에 사용되는 키의 기반 인터페이스입니다. 각 Bounded Context는 이 인터페이스를 구현하여 도메인 특화 락 키를 정의합니다.
 *
 * <p><strong>구현 가이드:</strong>
 *
 * <ul>
 *   <li>record로 구현 권장 (불변성, equals/hashCode 자동)
 *   <li>compact constructor에서 유효성 검증
 *   <li>키 형식: {@code lock:{domain}:{entity}:{id}}
 * </ul>
 *
 * <p><strong>구현 예시:</strong>
 *
 * <pre>{@code
 * public record OrderLockKey(Long orderId) implements LockKey {
 *
 *     private static final String PREFIX = "lock:order:";
 *
 *     public OrderLockKey {
 *         if (orderId == null || orderId <= 0) {
 *             throw new IllegalArgumentException("orderId must be positive");
 *         }
 *     }
 *
 *     @Override
 *     public String value() {
 *         return PREFIX + orderId;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * OrderLockKey lockKey = new OrderLockKey(orderId);
 * lockPort.tryLock(lockKey, 10, 30, TimeUnit.SECONDS);
 * }</pre>
 *
 * @author Development Team
 * @since 1.0.0
 * @see com.ryuqq.application.common.port.out.DistributedLockPort
 */
public interface LockKey {

    /**
     * Redis Lock Key 값 반환
     *
     * <p><strong>형식 규칙:</strong>
     *
     * <pre>
     * lock:{domain}:{id}
     * lock:{domain}:{entity}:{id}
     * lock:{domain}:{entity}:{id}:{sub-entity}:{sub-id}
     * </pre>
     *
     * <p><strong>예시:</strong>
     *
     * <ul>
     *   <li>{@code lock:order:123}
     *   <li>{@code lock:stock:item:456}
     *   <li>{@code lock:stock:item:456:warehouse:789}
     * </ul>
     *
     * @return Redis에서 사용할 Lock Key 문자열
     */
    String value();
}
