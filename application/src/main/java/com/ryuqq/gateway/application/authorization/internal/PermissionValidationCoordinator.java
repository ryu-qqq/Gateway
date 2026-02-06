package com.ryuqq.gateway.application.authorization.internal;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.domain.authorization.exception.PermissionDeniedException;
import com.ryuqq.gateway.domain.authorization.exception.PermissionSpecNotFoundException;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Permission Validation Coordinator (Reactive)
 *
 * <p>Permission Spec + Permission Hash 기반 권한 검증을 조율하는 Coordinator
 *
 * <p><strong>검증 흐름</strong>:
 *
 * <ol>
 *   <li>PermissionSpecCoordinator로 Permission Spec 조회
 *   <li>요청 경로/메서드에 해당하는 EndpointPermission 찾기
 *   <li>Public 엔드포인트면 바로 승인
 *   <li>권한 필요시 PermissionHashCoordinator로 사용자 권한 조회
 *   <li>권한/역할 검증 후 승인 또는 거부
 * </ol>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>PermissionSpecCoordinator - Permission Spec 조회
 *   <li>PermissionHashCoordinator - 사용자별 Permission Hash 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionValidationCoordinator {

    private static final Logger log =
            LoggerFactory.getLogger(PermissionValidationCoordinator.class);

    private final PermissionSpecCoordinator permissionSpecCoordinator;
    private final PermissionHashCoordinator permissionHashCoordinator;

    public PermissionValidationCoordinator(
            PermissionSpecCoordinator permissionSpecCoordinator,
            PermissionHashCoordinator permissionHashCoordinator) {
        this.permissionSpecCoordinator = permissionSpecCoordinator;
        this.permissionHashCoordinator = permissionHashCoordinator;
    }

    /**
     * 권한 검증 실행
     *
     * @param command 검증 요청 (경로, 메서드, 테넌트, 사용자, permissionHash)
     * @return 검증 결과
     */
    public Mono<ValidatePermissionResponse> validate(ValidatePermissionCommand command) {
        HttpMethod method = HttpMethod.from(command.requestMethod());

        return permissionSpecCoordinator
                .findPermissionSpec()
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

    /** Permission Spec 기반 검증 */
    private Mono<ValidatePermissionResponse> validateWithSpec(
            ValidatePermissionCommand command, PermissionSpec spec, HttpMethod method) {

        return Mono.justOrEmpty(spec.findPermission(command.requestPath(), method))
                .switchIfEmpty(
                        Mono.error(
                                new PermissionSpecNotFoundException(
                                        command.requestPath(), command.requestMethod())))
                .flatMap(endpoint -> validateEndpoint(command, endpoint));
    }

    /** 엔드포인트 권한 검증 */
    private Mono<ValidatePermissionResponse> validateEndpoint(
            ValidatePermissionCommand command, EndpointPermission endpoint) {

        if (endpoint.isPublic()) {
            return Mono.just(ValidatePermissionResponse.publicEndpoint(endpoint));
        }

        if (!endpoint.requiresAuthorization()) {
            return Mono.just(ValidatePermissionResponse.authorized(endpoint));
        }

        return permissionHashCoordinator
                .findByTenantAndUser(command.tenantId(), command.userId(), command.permissionHash())
                .flatMap(permissionHash -> validatePermissions(endpoint, permissionHash));
    }

    /** 권한/역할 검증 */
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
                                .collect(Collectors.toSet()),
                        permissionHash.permissionStrings()));
    }
}
