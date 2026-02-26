package com.ryuqq.gateway.application.authorization.internal;

import com.ryuqq.gateway.application.authorization.manager.PermissionClientManager;
import com.ryuqq.gateway.application.authorization.manager.PermissionHashCommandManager;
import com.ryuqq.gateway.application.authorization.manager.PermissionHashQueryManager;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Hash Coordinator (Reactive)
 *
 * <p>2-Tier 캐시 전략을 조율하는 Coordinator: JWT Payload → Redis → AuthHub
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>PermissionHashQueryManager로 Redis Cache 조회
 *   <li>Cache Hit 시 JWT permissionHash와 비교하여 유효성 검증
 *   <li>Cache Miss 또는 Hash 불일치 시 PermissionClientManager로 AuthHub API 호출
 *   <li>조회된 Permission Hash를 PermissionHashCommandManager로 Redis에 저장
 * </ol>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionHashQueryManager - Redis Cache 조회
 *   <li>PermissionClientManager - AuthHub API 호출 (Cache Miss Fallback)
 *   <li>PermissionHashCommandManager - Redis Cache 저장
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashCoordinator {

    private static final Logger log = LoggerFactory.getLogger(PermissionHashCoordinator.class);

    private final PermissionHashQueryManager permissionHashQueryManager;
    private final PermissionClientManager permissionClientManager;
    private final PermissionHashCommandManager permissionHashCommandManager;

    public PermissionHashCoordinator(
            PermissionHashQueryManager permissionHashQueryManager,
            PermissionClientManager permissionClientManager,
            PermissionHashCommandManager permissionHashCommandManager) {
        this.permissionHashQueryManager = permissionHashQueryManager;
        this.permissionClientManager = permissionClientManager;
        this.permissionHashCommandManager = permissionHashCommandManager;
    }

    /**
     * Permission Hash 조회 (2-Tier Cache 전략)
     *
     * <p>1. JWT Payload의 permissionHash로 Redis 캐시 검증
     *
     * <p>2. Redis 캐시 없으면 AuthHub에서 조회 후 캐시
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param jwtPermissionHash JWT에 포함된 Permission Hash
     * @return Mono&lt;PermissionHash&gt;
     */
    public Mono<PermissionHash> findByTenantAndUser(
            String tenantId, String userId, String jwtPermissionHash) {

        return permissionHashQueryManager
                .findByTenantAndUser(tenantId, userId)
                .flatMap(cached -> validateAndReturn(cached, jwtPermissionHash, tenantId, userId))
                .switchIfEmpty(Mono.defer(() -> fetchFromAuthHubAndCache(tenantId, userId)));
    }

    /**
     * 캐시된 Permission Hash 검증 후 반환
     *
     * @param cached 캐시된 Permission Hash
     * @param jwtPermissionHash JWT Permission Hash
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;PermissionHash&gt;
     */
    private Mono<PermissionHash> validateAndReturn(
            PermissionHash cached, String jwtPermissionHash, String tenantId, String userId) {

        if (cached.matchesHash(jwtPermissionHash)) {
            log.debug(
                    "Permission hash validated from cache: tenantId={}, userId={}",
                    tenantId,
                    userId);
            return Mono.just(cached);
        }

        log.info("Permission hash mismatch, refetching: tenantId={}, userId={}", tenantId, userId);
        return fetchFromAuthHubAndCache(tenantId, userId);
    }

    /**
     * AuthHub에서 Permission Hash 조회 후 Redis에 캐싱
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;PermissionHash&gt;
     */
    private Mono<PermissionHash> fetchFromAuthHubAndCache(String tenantId, String userId) {
        log.info(
                "Permission hash not found in cache, fetching from AuthHub: tenantId={}, userId={}",
                tenantId,
                userId);

        return permissionClientManager
                .fetchUserPermissions(tenantId, userId)
                .flatMap(
                        hash ->
                                permissionHashCommandManager
                                        .save(tenantId, userId, hash)
                                        .thenReturn(hash)
                                        .doOnSuccess(
                                                h ->
                                                        log.info(
                                                                "Permission hash cached:"
                                                                        + " tenantId={}, userId={},"
                                                                        + " permissions={}",
                                                                tenantId,
                                                                userId,
                                                                h.permissions().size())))
                .doOnError(
                        e ->
                                log.error(
                                        "Failed to fetch permission hash from AuthHub: tenantId={},"
                                                + " userId={}, error={}",
                                        tenantId,
                                        userId,
                                        e.getMessage()));
    }
}
