package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.TenantConfigMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import com.ryuqq.gateway.application.tenant.port.out.query.TenantConfigQueryPort;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant Config Query Adapter
 *
 * <p>TenantConfigQueryPort 구현체 (Redis Cache 조회만)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Tenant Config 조회
 *   <li>Cache Miss 시 empty Mono 반환
 * </ul>
 *
 * <p><strong>설계 결정</strong>:
 *
 * <ul>
 *   <li>Adapter는 Redis 조회만 담당
 *   <li>Cache Miss Fallback(AuthHub 호출)은 Application Service에서 처리
 *   <li>이렇게 함으로써 Adapter가 Application Port를 호출하지 않음
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigQueryAdapter implements TenantConfigQueryPort {

    private final TenantConfigRedisRepository tenantConfigRedisRepository;
    private final TenantConfigMapper tenantConfigMapper;

    public TenantConfigQueryAdapter(
            TenantConfigRedisRepository tenantConfigRedisRepository,
            TenantConfigMapper tenantConfigMapper) {
        this.tenantConfigRedisRepository = tenantConfigRedisRepository;
        this.tenantConfigMapper = tenantConfigMapper;
    }

    /**
     * Redis Cache에서 Tenant Config 조회
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt; (Cache Miss 시 empty Mono)
     */
    @Override
    public Mono<TenantConfig> findByTenantId(String tenantId) {
        return tenantConfigRedisRepository
                .findByTenantId(tenantId)
                .map(tenantConfigMapper::toTenantConfig)
                .onErrorMap(
                        e -> new RuntimeException(
                                "Failed to get tenant config from Redis: " + tenantId, e));
    }
}
