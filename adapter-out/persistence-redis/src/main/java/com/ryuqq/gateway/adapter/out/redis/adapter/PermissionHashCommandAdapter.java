package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionHashMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionHashRedisRepository;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Hash Command Adapter
 *
 * <p>PermissionHashCommandPort 구현체 (Redis Cache 저장/삭제)
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionHashCommandAdapter implements PermissionHashCommandPort {

    private final PermissionHashRedisRepository permissionHashRedisRepository;
    private final PermissionHashMapper permissionHashMapper;

    public PermissionHashCommandAdapter(
            PermissionHashRedisRepository permissionHashRedisRepository,
            PermissionHashMapper permissionHashMapper) {
        this.permissionHashRedisRepository = permissionHashRedisRepository;
        this.permissionHashMapper = permissionHashMapper;
    }

    /**
     * Permission Hash 저장 (Cache)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param permissionHash Permission Hash
     * @return 완료 Mono
     */
    @Override
    public Mono<Void> save(String tenantId, String userId, PermissionHash permissionHash) {
        return Mono.defer(
                () ->
                        permissionHashRedisRepository.save(
                                tenantId, userId, permissionHashMapper.toEntity(permissionHash)));
    }

    /**
     * Permission Hash 캐시 무효화
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return 완료 Mono
     */
    @Override
    public Mono<Void> invalidate(String tenantId, String userId) {
        return permissionHashRedisRepository.delete(tenantId, userId);
    }
}
