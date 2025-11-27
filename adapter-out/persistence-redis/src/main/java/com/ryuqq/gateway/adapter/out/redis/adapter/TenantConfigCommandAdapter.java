package com.ryuqq.gateway.adapter.out.redis.adapter;

import com.ryuqq.gateway.adapter.out.redis.mapper.TenantConfigMapper;
import com.ryuqq.gateway.adapter.out.redis.repository.TenantConfigRedisRepository;
import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.TenantConfigPersistenceException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant Config Command Adapter
 *
 * <p>TenantConfigCommandPort 구현체 (Redis Cache 저장/삭제)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에 Tenant Config 저장 (TTL: 1시간)
 *   <li>Redis Cache에서 Tenant Config 삭제 (캐시 무효화)
 * </ul>
 *
 * <p><strong>사용 시나리오</strong>:
 *
 * <ul>
 *   <li>save: Cache Miss 시 AuthHub API 호출 후 캐싱
 *   <li>deleteByTenantId: Webhook을 통한 캐시 무효화
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigCommandAdapter implements TenantConfigCommandPort {

    private final TenantConfigRedisRepository tenantConfigRedisRepository;
    private final TenantConfigMapper tenantConfigMapper;

    public TenantConfigCommandAdapter(
            TenantConfigRedisRepository tenantConfigRedisRepository,
            TenantConfigMapper tenantConfigMapper) {
        this.tenantConfigRedisRepository = tenantConfigRedisRepository;
        this.tenantConfigMapper = tenantConfigMapper;
    }

    /**
     * Redis Cache에 Tenant Config 저장
     *
     * @param tenantConfig 저장할 Tenant Config
     * @return Mono&lt;Void&gt;
     */
    @Override
    public Mono<Void> save(TenantConfig tenantConfig) {
        return Mono.fromCallable(() -> tenantConfigMapper.toTenantConfigEntity(tenantConfig))
                .flatMap(
                        entity ->
                                tenantConfigRedisRepository.save(
                                        tenantConfig.getTenantIdValue(), entity))
                .onErrorMap(
                        e ->
                                new TenantConfigPersistenceException(
                                        tenantConfig.getTenantIdValue(), "save", e));
    }

    /**
     * Redis Cache에서 Tenant Config 삭제 (캐시 무효화)
     *
     * @param tenantId 삭제할 테넌트 ID
     * @return Mono&lt;Void&gt;
     */
    @Override
    public Mono<Void> deleteByTenantId(String tenantId) {
        return tenantConfigRedisRepository
                .deleteByTenantId(tenantId)
                .then()
                .onErrorMap(e -> new TenantConfigPersistenceException(tenantId, "delete", e));
    }
}
