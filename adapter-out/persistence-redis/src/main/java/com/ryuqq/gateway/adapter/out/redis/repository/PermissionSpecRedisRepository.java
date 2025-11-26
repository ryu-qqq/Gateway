package com.ryuqq.gateway.adapter.out.redis.repository;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Permission Spec Redis Repository
 *
 * <p>Redis에 Permission Spec을 저장/조회하는 Repository
 *
 * <p><strong>Redis Key 규칙</strong>:
 *
 * <ul>
 *   <li>Key: {@code authhub:permission:spec}
 *   <li>TTL: 30초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Repository
public class PermissionSpecRedisRepository {

    private static final String PERMISSION_SPEC_KEY = "authhub:permission:spec";
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final ReactiveRedisTemplate<String, PermissionSpecEntity> reactiveRedisTemplate;

    public PermissionSpecRedisRepository(
            @Qualifier("permissionSpecRedisTemplate")
                    ReactiveRedisTemplate<String, PermissionSpecEntity> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Permission Spec 저장 (TTL 포함)
     *
     * @param permissionSpec PermissionSpecEntity
     * @param ttl TTL (Duration)
     * @return Void
     */
    public Mono<Void> save(PermissionSpecEntity permissionSpec, Duration ttl) {
        return reactiveRedisTemplate
                .opsForValue()
                .set(PERMISSION_SPEC_KEY, permissionSpec, ttl)
                .then();
    }

    /**
     * Permission Spec 저장 (기본 TTL: 30초)
     *
     * @param permissionSpec PermissionSpecEntity
     * @return Void
     */
    public Mono<Void> save(PermissionSpecEntity permissionSpec) {
        return save(permissionSpec, DEFAULT_TTL);
    }

    /**
     * Permission Spec 조회
     *
     * @return PermissionSpecEntity (없으면 Mono.empty())
     */
    public Mono<PermissionSpecEntity> find() {
        return reactiveRedisTemplate.opsForValue().get(PERMISSION_SPEC_KEY);
    }

    /**
     * Permission Spec 삭제
     *
     * @return Void
     */
    public Mono<Void> delete() {
        return reactiveRedisTemplate.delete(PERMISSION_SPEC_KEY).then();
    }
}
