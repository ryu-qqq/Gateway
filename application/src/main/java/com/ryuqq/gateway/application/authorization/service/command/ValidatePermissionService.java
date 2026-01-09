package com.ryuqq.gateway.application.authorization.service.command;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.application.authorization.port.in.command.ValidatePermissionUseCase;
import com.ryuqq.gateway.application.authorization.service.query.GetPermissionHashService;
import com.ryuqq.gateway.application.authorization.service.query.GetPermissionSpecService;
import com.ryuqq.gateway.domain.authorization.exception.PermissionDeniedException;
import com.ryuqq.gateway.domain.authorization.exception.PermissionSpecNotFoundException;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import com.ryuqq.observability.logging.annotation.Loggable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * ValidatePermissionService - 권한 검증 Service
 *
 * <p>Permission Spec + Permission Hash 기반 권한 검증을 수행합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class ValidatePermissionService implements ValidatePermissionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValidatePermissionService.class);

    private final GetPermissionSpecService getPermissionSpecService;
    private final GetPermissionHashService getPermissionHashService;

    public ValidatePermissionService(
            GetPermissionSpecService getPermissionSpecService,
            GetPermissionHashService getPermissionHashService) {
        this.getPermissionSpecService = getPermissionSpecService;
        this.getPermissionHashService = getPermissionHashService;
    }

    @Loggable(value = "권한 검증", includeArgs = false, slowThreshold = 300)
    @Override
    public Mono<ValidatePermissionResponse> execute(ValidatePermissionCommand command) {
        HttpMethod method = HttpMethod.from(command.requestMethod());

        return getPermissionSpecService
                .getPermissionSpec()
                .flatMap(spec -> validateWithSpec(command, spec, method))
                .doOnSuccess(
                        response ->
                                log.debug(
                                        "Permission validation result: authorized={}, path={},"
                                                + " method={}",
                                        response.authorized(),
                                        command.requestPath(),
                                        command.requestMethod()))
                .doOnError(
                        e ->
                                log.warn(
                                        "Permission validation failed: path={}, method={},"
                                                + " error={}",
                                        command.requestPath(),
                                        command.requestMethod(),
                                        e.getMessage()));
    }

    private Mono<ValidatePermissionResponse> validateWithSpec(
            ValidatePermissionCommand command, PermissionSpec spec, HttpMethod method) {

        return Mono.justOrEmpty(spec.findPermission(command.requestPath(), method))
                .switchIfEmpty(
                        Mono.error(
                                new PermissionSpecNotFoundException(
                                        command.requestPath(), command.requestMethod())))
                .flatMap(endpoint -> validateEndpoint(command, endpoint));
    }

    private Mono<ValidatePermissionResponse> validateEndpoint(
            ValidatePermissionCommand command, EndpointPermission endpoint) {

        if (endpoint.isPublic()) {
            return Mono.just(ValidatePermissionResponse.publicEndpoint(endpoint));
        }

        if (!endpoint.requiresAuthorization()) {
            return Mono.just(ValidatePermissionResponse.authorized(endpoint));
        }

        return getPermissionHashService
                .getPermissionHash(command.tenantId(), command.userId(), command.permissionHash())
                .flatMap(permissionHash -> validatePermissions(endpoint, permissionHash));
    }

    private Mono<ValidatePermissionResponse> validatePermissions(
            EndpointPermission endpoint, PermissionHash permissionHash) {

        boolean hasPermissions = permissionHash.hasAllPermissions(endpoint.requiredPermissions());
        boolean hasRoles =
                endpoint.requiredRoles().isEmpty()
                        || permissionHash.hasAnyRole(endpoint.requiredRoles());

        if (hasPermissions && hasRoles) {
            return Mono.just(ValidatePermissionResponse.authorized(endpoint));
        }

        return Mono.error(
                new PermissionDeniedException(
                        endpoint.requiredPermissions().stream()
                                .map(p -> p.value())
                                .collect(java.util.stream.Collectors.toSet()),
                        permissionHash.permissionStrings()));
    }
}
