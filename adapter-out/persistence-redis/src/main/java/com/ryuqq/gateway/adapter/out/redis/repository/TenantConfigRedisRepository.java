package com.ryuqq.gateway.adapter.out.redis.repository;

import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Tenant Config Redis Repository
 *
 * <p>Redis에 Tenant Config를 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code gateway:tenant:config:{tenantId}}
 *   <li>TTL: 1시간
 * </ul>
 *
 * <p><strong>기술 스택</strong>:
 *
 * <ul>
 *   <li>ReactiveRedisTemplate (Reactive)
 *   <li>Lettuce (Connection Pool)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class TenantConfigRedisRepository {

    private static final String TENANT_CONFIG_PREFIX = "gateway:tenant:config";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final ReactiveRedisTemplate<String, TenantConfigEntity> reactiveRedisTemplate;

    public TenantConfigRedisRepository(
            @Qualifier("tenantConfigRedisTemplate")
            ReactiveRedisTemplate<String, TenantConfigEntity> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Tenant Config 저장 (TTL 포함)
     *
     * @param tenantId Tenant ID
     * @param tenantConfig TenantConfigEntity
     * @param ttl TTL (Duration)
     * @return Void
     */
    public Mono<Void> save(String tenantId, TenantConfigEntity tenantConfig, Duration ttl) {
        String redisKey = buildRedisKey(tenantId);
        return reactiveRedisTemplate.opsForValue().set(redisKey, tenantConfig, ttl).then();
    }

    /**
     * Tenant Config 저장 (기본 TTL: 1시간)
     *
     * @param tenantId Tenant ID
     * @param tenantConfig TenantConfigEntity
     * @return Void
     */
    public Mono<Void> save(String tenantId, TenantConfigEntity tenantConfig) {
        return save(tenantId, tenantConfig, DEFAULT_TTL);
    }

    /**
     * Tenant Config 조회
     *
     * @param tenantId Tenant ID
     * @return TenantConfigEntity (없으면 Mono.empty())
     */
    public Mono<TenantConfigEntity> findByTenantId(String tenantId) {
        String redisKey = buildRedisKey(tenantId);
        return reactiveRedisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Tenant Config 삭제 (캐시 무효화)
     *
     * @param tenantId Tenant ID
     * @return 삭제 여부 (성공 시 true)
     */
    public Mono<Boolean> deleteByTenantId(String tenantId) {
        String redisKey = buildRedisKey(tenantId);
        return reactiveRedisTemplate.delete(redisKey).map(count -> count > 0);
    }

    /**
     * Tenant Config 존재 여부 확인
     *
     * @param tenantId Tenant ID
     * @return 존재하면 true
     */
    public Mono<Boolean> existsByTenantId(String tenantId) {
        String redisKey = buildRedisKey(tenantId);
        return reactiveRedisTemplate.hasKey(redisKey);
    }

    /**
     * Redis Key 생성
     *
     * @param tenantId Tenant ID
     * @return Redis Key (gateway:tenant:config:{tenantId})
     */
    private String buildRedisKey(String tenantId) {
        return TENANT_CONFIG_PREFIX + ":" + tenantId;
    }
}
