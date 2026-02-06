package com.ryuqq.gateway.application.authorization.manager;

import com.ryuqq.gateway.application.authorization.port.out.client.PermissionClient;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Client Manager (Reactive)
 *
 * <p>AuthHub API를 통해 Permission 정보를 조회하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>AuthHub API 호출하여 Permission Spec 조회
 *   <li>AuthHub API 호출하여 사용자별 Permission Hash 조회
 *   <li>Cache Miss 시 Fallback으로 사용
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionClient - AuthHub API 호출
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionClientManager {

    private final PermissionClient permissionClient;

    public PermissionClientManager(PermissionClient permissionClient) {
        this.permissionClient = permissionClient;
    }

    /**
     * AuthHub API에서 Permission Spec 조회
     *
     * @return Mono&lt;PermissionSpec&gt;
     */
    public Mono<PermissionSpec> fetchPermissionSpec() {
        return permissionClient.fetchPermissionSpec();
    }

    /**
     * AuthHub API에서 사용자별 Permission Hash 조회
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Mono&lt;PermissionHash&gt;
     */
    public Mono<PermissionHash> fetchUserPermissions(String tenantId, String userId) {
        return permissionClient.fetchUserPermissions(tenantId, userId);
    }
}
