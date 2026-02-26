package com.ryuqq.gateway.application.authorization.manager;

import com.ryuqq.gateway.application.authorization.port.out.query.PermissionSpecQueryPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Spec 조회 Manager (Reactive)
 *
 * <p>Redis Cache에서 Permission Spec을 조회하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Permission Spec 조회
 *   <li>Cache Hit 시 PermissionSpec 반환
 *   <li>Cache Miss 시 빈 Mono 반환
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionSpecQueryPort - Redis Cache 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecQueryManager {

    private final PermissionSpecQueryPort permissionSpecQueryPort;

    public PermissionSpecQueryManager(PermissionSpecQueryPort permissionSpecQueryPort) {
        this.permissionSpecQueryPort = permissionSpecQueryPort;
    }

    /**
     * Permission Spec 조회 (Redis Cache)
     *
     * @return Mono&lt;PermissionSpec&gt; (Cache Miss 시 빈 Mono)
     */
    public Mono<PermissionSpec> findPermissionSpec() {
        return permissionSpecQueryPort.findPermissionSpec();
    }
}
