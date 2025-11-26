package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionHashMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionHashRedisRepository;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionHashQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Hash Query Adapter
 *
 * <p>PermissionHashQueryPort 구현체 (Redis Cache 조회만)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Permission Hash 조회
 *   <li>Cache Miss 시 empty Mono 반환
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashQueryAdapter implements PermissionHashQueryPort {

    private final PermissionHashRedisRepository permissionHashRedisRepository;
    private final PermissionHashMapper permissionHashMapper;

    public PermissionHashQueryAdapter(
            PermissionHashRedisRepository permissionHashRedisRepository,
            PermissionHashMapper permissionHashMapper) {
        this.permissionHashRedisRepository = permissionHashRedisRepository;
        this.permissionHashMapper = permissionHashMapper;
    }

    /**
     * Redis Cache에서 Permission Hash 조회
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;PermissionHash&gt; (Cache Miss 시 empty Mono)
     */
    @Override
    public Mono<PermissionHash> findByTenantAndUser(String tenantId, String userId) {
        return permissionHashRedisRepository
                .findByTenantAndUser(tenantId, userId)
                .map(permissionHashMapper::toPermissionHash)
                .onErrorMap(
                        e ->
                                new RuntimeException(
                                        "Failed to get permission hash from Redis: tenantId="
                                                + tenantId
                                                + ", userId="
                                                + userId,
                                        e));
    }
}
