package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionSpecMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionSpecRedisRepository;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionSpecCommandPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Spec Command Adapter
 *
 * <p>PermissionSpecCommandPort 구현체 (Redis Cache 저장/삭제)
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecCommandAdapter implements PermissionSpecCommandPort {

    private final PermissionSpecRedisRepository permissionSpecRedisRepository;
    private final PermissionSpecMapper permissionSpecMapper;

    public PermissionSpecCommandAdapter(
            PermissionSpecRedisRepository permissionSpecRedisRepository,
            PermissionSpecMapper permissionSpecMapper) {
        this.permissionSpecRedisRepository = permissionSpecRedisRepository;
        this.permissionSpecMapper = permissionSpecMapper;
    }

    /**
     * Permission Spec 저장 (Cache)
     *
     * @param permissionSpec Permission Spec
     * @return 완료 Mono
     */
    @Override
    public Mono<Void> save(PermissionSpec permissionSpec) {
        return Mono.defer(
                () ->
                        permissionSpecRedisRepository.save(
                                permissionSpecMapper.toEntity(permissionSpec)));
    }

    /**
     * Permission Spec 캐시 무효화
     *
     * @return 완료 Mono
     */
    @Override
    public Mono<Void> invalidate() {
        return permissionSpecRedisRepository.delete();
    }
}
