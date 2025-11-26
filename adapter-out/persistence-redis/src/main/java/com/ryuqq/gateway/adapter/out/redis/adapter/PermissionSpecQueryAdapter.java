package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.PermissionSpecMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.PermissionSpecRedisRepository;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionSpecQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Spec Query Adapter
 *
 * <p>PermissionSpecQueryPort 구현체 (Redis Cache 조회만)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Permission Spec 조회
 *   <li>Cache Miss 시 empty Mono 반환
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecQueryAdapter implements PermissionSpecQueryPort {

    private static final Logger log = LoggerFactory.getLogger(PermissionSpecQueryAdapter.class);

    private final PermissionSpecRedisRepository permissionSpecRedisRepository;
    private final PermissionSpecMapper permissionSpecMapper;

    public PermissionSpecQueryAdapter(
            PermissionSpecRedisRepository permissionSpecRedisRepository,
            PermissionSpecMapper permissionSpecMapper) {
        this.permissionSpecRedisRepository = permissionSpecRedisRepository;
        this.permissionSpecMapper = permissionSpecMapper;
    }

    /**
     * Redis Cache에서 Permission Spec 조회
     *
     * @return Mono&lt;PermissionSpec&gt; (Cache Miss 시 empty Mono)
     */
    @Override
    public Mono<PermissionSpec> findPermissionSpec() {
        return permissionSpecRedisRepository
                .find()
                .doOnNext(entity -> log.debug("Permission spec entity found: {}", entity))
                .doOnError(e -> log.error("Error fetching permission spec from Redis", e))
                .map(permissionSpecMapper::toPermissionSpec)
                .doOnNext(
                        spec ->
                                log.debug(
                                        "Permission spec mapped successfully: {} endpoints",
                                        spec.permissions().size()))
                .onErrorMap(
                        e -> {
                            log.error("Failed to get permission spec from Redis", e);
                            return new RuntimeException(
                                    "Failed to get permission spec from Redis", e);
                        });
    }
}
