package com.ryuqq.gateway.adapter.out.authhub.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.Collections;
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
 *   <li>Permission Spec: {@code GET /api/v1/permissions/spec} (authhub-client.yml 설정)
 *   <li>User Permissions: {@code GET /api/v1/permissions/users/{userId}} (authhub-client.yml 설정)
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

    private static final String X_SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final WebClient webClient;
    private final AuthHubProperties properties;
    private final ObjectMapper objectMapper;

    public AuthHubPermissionAdapter(
            @Qualifier("authHubWebClient") WebClient webClient,
            AuthHubProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Permission Spec 조회 (AuthHub Internal API)
     *
     * <p>X-Service-Token 헤더로 인증합니다.
     *
     * @return Permission Spec
     */
    @Override
    @Retry(name = "authHub", fallbackMethod = "fetchPermissionSpecFallback")
    @CircuitBreaker(name = "authHub", fallbackMethod = "fetchPermissionSpecFallback")
    public Mono<PermissionSpec> fetchPermissionSpec() {
        return webClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(properties.getPermissionSpecEndpoint())
                                        .queryParam("serviceName", properties.getServiceName())
                                        .build())
                .header(X_SERVICE_TOKEN_HEADER, properties.getServiceToken())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parsePermissionSpecResponse)
                .onErrorMap(
                        e -> !(e instanceof AuthHubPermissionException),
                        e -> new AuthHubPermissionException("Failed to fetch Permission Spec", e));
    }

    /**
     * AuthHub API 응답을 PermissionSpec으로 파싱
     *
     * @param responseBody JSON 응답 문자열
     * @return PermissionSpec
     */
    PermissionSpec parsePermissionSpecResponse(String responseBody) {
        try {
            AuthHubApiResponse<PermissionSpecData> response =
                    objectMapper.readValue(
                            responseBody,
                            new TypeReference<AuthHubApiResponse<PermissionSpecData>>() {});

            if (!response.success() || response.data() == null) {
                throw new AuthHubPermissionException("AuthHub returned unsuccessful response");
            }

            PermissionSpecData data = response.data();
            List<EndpointPermission> endpoints =
                    data.endpoints() != null
                            ? data.endpoints().stream().map(this::toEndpointPermission).toList()
                            : Collections.emptyList();

            // version을 타임스탬프로 해시해서 Long으로 변환
            Long versionHash =
                    data.version() != null
                            ? (long) data.version().hashCode()
                            : System.currentTimeMillis();

            return PermissionSpec.of(versionHash, Instant.now(), endpoints);
        } catch (AuthHubPermissionException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthHubPermissionException("Failed to parse Permission Spec response", e);
        }
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
                .uri(properties.getUserPermissionsEndpoint(), userId)
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

    /** AuthHub API 응답 Wrapper DTO */
    record AuthHubApiResponse<T>(boolean success, T data, String timestamp, String requestId) {}

    /** Permission Spec Data DTO (AuthHub 응답의 data 필드) */
    record PermissionSpecData(List<EndpointPermissionResponse> endpoints, String version) {}

    /** Permission Spec Response DTO (기존 호환성 유지) */
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
