package com.ryuqq.gateway.application.authorization.service.command;

import com.ryuqq.gateway.application.authorization.dto.command.SyncPermissionSpecCommand;
import com.ryuqq.gateway.application.authorization.manager.PermissionSpecCommandManager;
import com.ryuqq.gateway.application.authorization.port.in.command.SyncPermissionSpecUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Permission Spec 동기화 Service
 *
 * <p>AuthHub Webhook을 받아 Permission Spec 캐시를 무효화합니다.
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * SyncPermissionSpecService (Application Service)
 *   ↓ (calls)
 * PermissionSpecCommandManager (Application Manager)
 *   ↓ (calls)
 * PermissionSpecCommandPort (Port)
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class SyncPermissionSpecService implements SyncPermissionSpecUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncPermissionSpecService.class);

    private final PermissionSpecCommandManager permissionSpecCommandManager;

    public SyncPermissionSpecService(PermissionSpecCommandManager permissionSpecCommandManager) {
        this.permissionSpecCommandManager = permissionSpecCommandManager;
    }

    @Override
    public Mono<Void> execute(SyncPermissionSpecCommand command) {
        log.info(
                "Syncing permission spec: version={}, changedServices={}",
                command.version(),
                command.changedServices());

        return permissionSpecCommandManager
                .invalidate()
                .doOnSuccess(
                        v ->
                                log.info(
                                        "Permission spec cache invalidated: version={}",
                                        command.version()))
                .doOnError(
                        e ->
                                log.error(
                                        "Failed to invalidate permission spec cache: version={},"
                                                + " error={}",
                                        command.version(),
                                        e.getMessage()));
    }
}
