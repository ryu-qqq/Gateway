package com.ryuqq.gateway.adapter.out.authhub.client;

import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * AuthHub Permission Adapter
 *
 * <p>AuthHubPermissionClient 구현체 (WebClient + Resilience4j)
 *
 * <p><strong>통신 대상</strong>:
 *
 * <ul>
 *   <li>AuthHub 외부 시스템
 *   <li>Permission Spec: {@code GET /api/v1/permissions/spec}
 *   <li>User Permissions: {@code GET /api/v1/permissions/users/{userId}}
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 *   <li>Timeout: Connection 3초, Response 3초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubPermissionAdapter implements AuthHubPermissionClient {

    private static final String PERMISSION_SPEC_ENDPOINT = "/api/v1/permissions/spec";
    private static final String USER_PERMISSIONS_ENDPOINT = "/api/v1/permissions/users/{userId}";

    private final WebClient webClient;

    public AuthHubPermissionAdapter(@Qualifier("authHubWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Permission Spec 조회 (AuthHub API)
     *
     * @return Permission Spec
     */
    @Override
    @Retry(name = "authHub", fallbackMethod = "fetchPermissionSpecFallback")
    @CircuitBreaker(name = "authHub", fallbackMethod = "fetchPermissionSpecFallback")
    public Mono<PermissionSpec> fetchPermissionSpec() {
        return webClient
                .get()
                .uri(PERMISSION_SPEC_ENDPOINT)
                .retrieve()
                .bodyToMono(PermissionSpecResponse.class)
                .map(this::toPermissionSpec)
                .onErrorMap(
                        e -> !(e instanceof AuthHubPermissionException),
                        e -> new AuthHubPermissionException("Failed to fetch Permission Spec", e));
    }

    /**
     * 사용자별 Permission Hash 조회 (AuthHub API)
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @return Permission Hash
     */
    @Override
    @Retry(name = "authHub", fallbackMethod = "fetchUserPermissionsFallback")
    @CircuitBreaker(name = "authHub", fallbackMethod = "fetchUserPermissionsFallback")
    public Mono<PermissionHash> fetchUserPermissions(String tenantId, String userId) {
        return webClient
                .get()
                .uri(USER_PERMISSIONS_ENDPOINT, userId)
                .header("X-Tenant-Id", tenantId)
                .retrieve()
                .bodyToMono(PermissionHashResponse.class)
                .map(this::toPermissionHash)
                .onErrorMap(
                        e -> !(e instanceof AuthHubPermissionException),
                        e ->
                                new AuthHubPermissionException(
                                        "Failed to fetch User Permissions: " + userId, e));
    }

    /**
     * Permission Spec 응답 → Domain 변환
     *
     * @param response API 응답
     * @return PermissionSpec
     */
    PermissionSpec toPermissionSpec(PermissionSpecResponse response) {
        if (response == null) {
            throw new AuthHubPermissionException("Empty Permission Spec response");
        }

        List<EndpointPermission> endpoints =
                response.permissions().stream().map(this::toEndpointPermission).toList();

        return PermissionSpec.of(response.version(), response.updatedAt(), endpoints);
    }

    /**
     * Endpoint Permission 응답 → Domain 변환
     *
     * @param ep API 응답
     * @return EndpointPermission
     */
    EndpointPermission toEndpointPermission(EndpointPermissionResponse ep) {
        Set<Permission> permissions =
                ep.requiredPermissions().stream().map(Permission::of).collect(Collectors.toSet());

        HttpMethod method = HttpMethod.valueOf(ep.method().toUpperCase());

        return EndpointPermission.of(
                ep.serviceName(),
                ep.path(),
                method,
                permissions,
                Set.copyOf(ep.requiredRoles()),
                ep.isPublic());
    }

    /**
     * Permission Hash 응답 → Domain 변환
     *
     * @param response API 응답
     * @return PermissionHash
     */
    PermissionHash toPermissionHash(PermissionHashResponse response) {
        if (response == null) {
            throw new AuthHubPermissionException("Empty Permission Hash response");
        }

        return PermissionHash.fromStrings(
                response.hash(),
                Set.copyOf(response.permissions()),
                Set.copyOf(response.roles()),
                response.generatedAt());
    }

    /**
     * Permission Spec Fallback
     *
     * @param throwable 예외
     * @return Mono.error
     */
    @SuppressWarnings("unused")
    Mono<PermissionSpec> fetchPermissionSpecFallback(Throwable throwable) {
        return Mono.error(
                new AuthHubPermissionException("Permission Spec 조회 실패 (Fallback)", throwable));
    }

    /**
     * User Permissions Fallback
     *
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID
     * @param throwable 예외
     * @return Mono.error
     */
    @SuppressWarnings("unused")
    Mono<PermissionHash> fetchUserPermissionsFallback(
            String tenantId, String userId, Throwable throwable) {
        return Mono.error(
                new AuthHubPermissionException(
                        "User Permissions 조회 실패 (Fallback): " + userId, throwable));
    }

    /** Permission Spec Response DTO */
    record PermissionSpecResponse(
            Long version, Instant updatedAt, List<EndpointPermissionResponse> permissions) {}

    /** Endpoint Permission Response DTO */
    record EndpointPermissionResponse(
            String serviceName,
            String path,
            String method,
            List<String> requiredPermissions,
            List<String> requiredRoles,
            boolean isPublic) {}

    /** Permission Hash Response DTO */
    record PermissionHashResponse(
            String hash, List<String> permissions, List<String> roles, Instant generatedAt) {}

    /** AuthHub Permission 예외 */
    public static class AuthHubPermissionException extends RuntimeException {
        public AuthHubPermissionException(String message) {
            super(message);
        }

        public AuthHubPermissionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
