package com.ryuqq.gateway.application.tenant.manager;

import com.ryuqq.gateway.application.tenant.port.out.client.AuthClient;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * AuthHub Client Manager (Reactive)
 *
 * <p>AuthHub API를 통해 Tenant Config를 조회하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>AuthHub API 호출하여 Tenant Config 조회
 *   <li>Cache Miss 시 Fallback으로 사용
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>AuthClient - AuthHub API 호출
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthClientManager {

    private final AuthClient authClient;

    public AuthClientManager(AuthClient authClient) {
        this.authClient = authClient;
    }

    /**
     * AuthHub API에서 Tenant Config 조회
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    public Mono<TenantConfig> fetchTenantConfig(String tenantId) {
        return authClient.fetchTenantConfig(tenantId);
    }
}
