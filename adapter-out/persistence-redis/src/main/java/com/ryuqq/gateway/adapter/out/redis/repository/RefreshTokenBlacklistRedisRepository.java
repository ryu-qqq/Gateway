package com.ryuqq.gateway.adapter.out.redis.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Refresh Token Blacklist Redis Repository
 *
 * <p>Redis에 Blacklist 등록된 Refresh Token을 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code tenant:{tenantId}:refresh:blacklist:{tokenHash}}
 *   <li>Value: "blacklisted"
 *   <li>TTL: Refresh Token 만료 시간 (일반적으로 7일)
 * </ul>
 *
 * <p><strong>보안</strong>:
 *
 * <ul>
 *   <li>Token 원본 대신 SHA-256 해시 저장
 *   <li>해시 충돌 가능성 무시 (확률적으로 무시 가능)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class RefreshTokenBlacklistRedisRepository {

    private static final String KEY_PREFIX = "tenant";
    private static final String KEY_SUFFIX = "refresh:blacklist";
    private static final String BLACKLISTED_VALUE = "blacklisted";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public RefreshTokenBlacklistRedisRepository(
            @Qualifier("reactiveStringRedisTemplate")
                    ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * Refresh Token을 Blacklist에 등록
     *
     * @param tenantId Tenant 식별자
     * @param tokenValue Refresh Token 원본 값
     * @param ttl TTL
     * @return Mono&lt;Boolean&gt; 등록 성공 여부
     */
    public Mono<Boolean> addToBlacklist(String tenantId, String tokenValue, Duration ttl) {
        String key = buildKey(tenantId, tokenValue);
        return reactiveStringRedisTemplate.opsForValue().set(key, BLACKLISTED_VALUE, ttl);
    }

    /**
     * Refresh Token이 Blacklist에 존재하는지 확인
     *
     * @param tenantId Tenant 식별자
     * @param tokenValue Refresh Token 원본 값
     * @return Mono&lt;Boolean&gt; Blacklist 존재 여부
     */
    public Mono<Boolean> isBlacklisted(String tenantId, String tokenValue) {
        String key = buildKey(tenantId, tokenValue);
        return reactiveStringRedisTemplate.hasKey(key);
    }

    /**
     * Redis Key 생성
     *
     * <p>Format: tenant:{tenantId}:refresh:blacklist:{tokenHash}
     *
     * @param tenantId Tenant 식별자
     * @param tokenValue Refresh Token 원본 값
     * @return Redis Key
     */
    private String buildKey(String tenantId, String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        return KEY_PREFIX + ":" + tenantId + ":" + KEY_SUFFIX + ":" + tokenHash;
    }

    /**
     * Token 해시 생성 (SHA-256)
     *
     * @param tokenValue Token 원본 값
     * @return SHA-256 해시 (Hex)
     */
    private String hashToken(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Byte 배열을 Hex 문자열로 변환
     *
     * @param bytes Byte 배열
     * @return Hex 문자열
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
