package com.ryuqq.gateway.domain.common.vo;

/**
 * 캐시 키 인터페이스
 *
 * <p>Redis 캐시에 사용되는 키의 기반 인터페이스입니다. 각 Bounded Context는 이 인터페이스를 구현하여 도메인 특화 캐시 키를 정의합니다.
 *
 * <p><strong>구현 가이드:</strong>
 *
 * <ul>
 *   <li>record로 구현 권장 (불변성, equals/hashCode 자동)
 *   <li>compact constructor에서 유효성 검증
 *   <li>키 형식: {@code cache:{domain}:{entity}:{id}}
 * </ul>
 *
 * <p><strong>구현 예시:</strong>
 *
 * <pre>{@code
 * public record RefreshTokenCacheKey(String token) implements CacheKey {
 *
 *     private static final String PREFIX = "cache:refresh-token:";
 *
 *     public RefreshTokenCacheKey {
 *         if (token == null || token.isBlank()) {
 *             throw new IllegalArgumentException("token must not be blank");
 *         }
 *     }
 *
 *     @Override
 *     public String value() {
 *         return PREFIX + token;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * RefreshTokenCacheKey cacheKey = new RefreshTokenCacheKey(token);
 * cachePort.set(cacheKey, memberId, Duration.ofDays(7));
 * }</pre>
 *
 * @author Development Team
 * @since 1.0.0
 * @see LockKey
 */
public interface CacheKey {

    /**
     * Redis Cache Key 값 반환
     *
     * <p><strong>형식 규칙:</strong>
     *
     * <pre>
     * cache:{domain}:{id}
     * cache:{domain}:{entity}:{id}
     * </pre>
     *
     * <p><strong>예시:</strong>
     *
     * <ul>
     *   <li>{@code cache:refresh-token:abc123}
     *   <li>{@code cache:member:session:uuid-456}
     * </ul>
     *
     * @return Redis에서 사용할 Cache Key 문자열
     */
    String value();
}
