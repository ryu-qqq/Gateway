package com.ryuqq.gateway.application.authorization.service.query;

import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionHashQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * GetPermissionHashService - Permission Hash 조회 Service
 *
 * <p>2-Tier 캐시 전략: JWT Payload → Redis → AuthHub
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPermissionHashService {

    private static final Logger log = LoggerFactory.getLogger(GetPermissionHashService.class);

    private final PermissionHashQueryPort permissionHashQueryPort;
    private final PermissionHashCommandPort permissionHashCommandPort;
    private final AuthHubPermissionClient authHubPermissionClient;

    public GetPermissionHashService(
            PermissionHashQueryPort permissionHashQueryPort,
            PermissionHashCommandPort permissionHashCommandPort,
            AuthHubPermissionClient authHubPermissionClient) {
        this.permissionHashQueryPort = permissionHashQueryPort;
        this.permissionHashCommandPort = permissionHashCommandPort;
        this.authHubPermissionClient = authHubPermissionClient;
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
     * @return Permission Hash
     */
    public Mono<PermissionHash> getPermissionHash(
            String tenantId, String userId, String jwtPermissionHash) {

        return permissionHashQueryPort
                .findByTenantAndUser(tenantId, userId)
                .flatMap(cached -> validateAndReturn(cached, jwtPermissionHash, tenantId, userId))
                .switchIfEmpty(Mono.defer(() -> fetchAndCachePermissionHash(tenantId, userId)));
    }

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
        return fetchAndCachePermissionHash(tenantId, userId);
    }

    private Mono<PermissionHash> fetchAndCachePermissionHash(String tenantId, String userId) {
        log.info(
                "Permission hash not found in cache, fetching from AuthHub: tenantId={}, userId={}",
                tenantId,
                userId);

        return authHubPermissionClient
                .fetchUserPermissions(tenantId, userId)
                .flatMap(
                        hash ->
                                permissionHashCommandPort
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
