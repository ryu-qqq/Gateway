package com.ryuqq.gateway.application.authorization.manager;

import com.ryuqq.gateway.application.authorization.port.out.command.PermissionSpecCommandPort;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Spec Command Manager (Reactive)
 *
 * <p>Redis Cache에 Permission Spec을 저장하거나 삭제하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에 Permission Spec 저장
 *   <li>Redis Cache에서 Permission Spec 삭제 (캐시 무효화)
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionSpecCommandPort - Redis Cache 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionSpecCommandManager {

    private final PermissionSpecCommandPort permissionSpecCommandPort;

    public PermissionSpecCommandManager(PermissionSpecCommandPort permissionSpecCommandPort) {
        this.permissionSpecCommandPort = permissionSpecCommandPort;
    }

    /**
     * Permission Spec 저장 (Redis Cache)
     *
     * @param permissionSpec 저장할 Permission Spec
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> save(PermissionSpec permissionSpec) {
        return permissionSpecCommandPort.save(permissionSpec);
    }

    /**
     * Permission Spec 캐시 무효화
     *
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> invalidate() {
        return permissionSpecCommandPort.invalidate();
    }
}
