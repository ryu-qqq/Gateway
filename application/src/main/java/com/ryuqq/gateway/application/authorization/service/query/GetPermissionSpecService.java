package com.ryuqq.gateway.application.authorization.service.query;

import com.ryuqq.gateway.application.authorization.internal.PermissionSpecCoordinator;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Permission Spec 조회 Service
 *
 * <p>PermissionSpecCoordinator를 통해 Permission Spec을 조회하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * GetPermissionSpecService (Application Service)
 *   ↓ (calls)
 * PermissionSpecCoordinator (Application Manager - internal)
 *   ↓ (calls)
 * PermissionSpecQueryManager + PermissionClientManager + PermissionSpecCommandManager
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetPermissionSpecService {

    private final PermissionSpecCoordinator permissionSpecCoordinator;

    public GetPermissionSpecService(PermissionSpecCoordinator permissionSpecCoordinator) {
        this.permissionSpecCoordinator = permissionSpecCoordinator;
    }

    /**
     * Permission Spec 조회 (Cache-aside 패턴)
     *
     * @return Permission Spec
     */
    public Mono<PermissionSpec> getPermissionSpec() {
        return permissionSpecCoordinator.findPermissionSpec();
    }
}
