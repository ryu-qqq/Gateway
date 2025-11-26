package com.ryuqq.gateway.application.authorization.service.command;

import com.ryuqq.gateway.application.authorization.dto.command.InvalidateUserPermissionCommand;
import com.ryuqq.gateway.application.authorization.port.in.command.InvalidateUserPermissionUseCase;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * InvalidateUserPermissionService - 사용자 권한 캐시 무효화 Service
 *
 * <p>AuthHub Webhook을 받아 사용자별 Permission Hash 캐시를 무효화합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class InvalidateUserPermissionService implements InvalidateUserPermissionUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(InvalidateUserPermissionService.class);

    private final PermissionHashCommandPort permissionHashCommandPort;

    public InvalidateUserPermissionService(PermissionHashCommandPort permissionHashCommandPort) {
        this.permissionHashCommandPort = permissionHashCommandPort;
    }

    @Override
    public Mono<Void> execute(InvalidateUserPermissionCommand command) {
        log.info(
                "Invalidating user permission cache: tenantId={}, userId={}",
                command.tenantId(),
                command.userId());

        return permissionHashCommandPort
                .invalidate(command.tenantId(), command.userId())
                .doOnSuccess(
                        v ->
                                log.info(
                                        "User permission cache invalidated: tenantId={}, userId={}",
                                        command.tenantId(),
                                        command.userId()))
                .doOnError(
                        e ->
                                log.error(
                                        "Failed to invalidate user permission cache: tenantId={},"
                                                + " userId={}, error={}",
                                        command.tenantId(),
                                        command.userId(),
                                        e.getMessage()));
    }
}
