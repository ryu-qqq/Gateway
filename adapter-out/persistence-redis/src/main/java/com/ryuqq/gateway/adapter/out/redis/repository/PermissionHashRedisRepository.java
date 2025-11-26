package com.ryuqq.gateway.adapter.out.redis.repository;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Permission Hash Redis Repository
 *
 * <p>Redis에 사용자별 Permission Hash를 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code authhub:permission:hash:{tenantId}:{userId}}
 *   <li>TTL: 30초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class PermissionHashRedisRepository {

    private static final String PERMISSION_HASH_PREFIX = "authhub:permission:hash";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final ReactiveRedisTemplate<String, PermissionHashEntity> reactiveRedisTemplate;

    public PermissionHashRedisRepository(
            @Qualifier("permissionHashRedisTemplate")
                    ReactiveRedisTemplate<String, PermissionHashEntity> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Permission Hash 저장 (TTL 포함)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param permissionHash PermissionHashEntity
     * @param ttl TTL (Duration)
     * @return Void
     */
    public Mono<Void> save(
            String tenantId, String userId, PermissionHashEntity permissionHash, Duration ttl) {
        String redisKey = buildRedisKey(tenantId, userId);
        return reactiveRedisTemplate.opsForValue().set(redisKey, permissionHash, ttl).then();
    }

    /**
     * Permission Hash 저장 (기본 TTL: 30초)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param permissionHash PermissionHashEntity
     * @return Void
     */
    public Mono<Void> save(String tenantId, String userId, PermissionHashEntity permissionHash) {
        return save(tenantId, userId, permissionHash, DEFAULT_TTL);
    }

    /**
     * Permission Hash 조회
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return PermissionHashEntity (없으면 Mono.empty())
     */
    public Mono<PermissionHashEntity> findByTenantAndUser(String tenantId, String userId) {
        String redisKey = buildRedisKey(tenantId, userId);
        return reactiveRedisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Permission Hash 삭제
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Void
     */
    public Mono<Void> delete(String tenantId, String userId) {
        String redisKey = buildRedisKey(tenantId, userId);
        return reactiveRedisTemplate.delete(redisKey).then();
    }

    /**
     * 테넌트별 모든 Permission Hash 삭제
     *
     * <p>KEYS 대신 SCAN을 사용하여 프로덕션 환경에서 Redis 블로킹을 방지합니다.
     *
     * @param tenantId 테넌트 ID
     * @return Void
     */
    public Mono<Void> deleteByTenant(String tenantId) {
        String pattern = PERMISSION_HASH_PREFIX + ":" + tenantId + ":*";
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
        return reactiveRedisTemplate
                .scan(scanOptions)
                .flatMap(reactiveRedisTemplate::delete)
                .then();
    }

    /**
     * Redis Key 생성
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Redis Key (authhub:permission:hash:{tenantId}:{userId})
     */
    private String buildRedisKey(String tenantId, String userId) {
        return PERMISSION_HASH_PREFIX + ":" + tenantId + ":" + userId;
    }
}
