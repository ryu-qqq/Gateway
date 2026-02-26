package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import com.ryuqq.authhub.sdk.client.GatewayClient;
import com.ryuqq.authhub.sdk.exception.AuthHubException;
import com.ryuqq.authhub.sdk.model.common.ApiResponse;
import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpecList;
import com.ryuqq.authhub.sdk.model.internal.UserPermissions;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.PermissionException;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubPermissionMapper;
import com.ryuqq.gateway.application.authorization.port.out.client.PermissionClient;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * AuthHub Permission Adapter
 *
 * <p>Permission 관련 AuthHub API 호출 Adapter
 *
 * <p><strong>구현 방식</strong>:
 *
 * <ul>
 *   <li>Permission Spec: SDK (GatewayClient.internal().getPermissionSpec())
 *   <li>User Permissions: SDK (GatewayClient.internal().getUserPermissions())
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubPermissionAdapter implements PermissionClient {

    private static final Logger log = LoggerFactory.getLogger(AuthHubPermissionAdapter.class);
    private static final String CIRCUIT_BREAKER_NAME = "authHubPermission";

    private final GatewayClient gatewayClient;
    private final AuthHubPermissionMapper permissionMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public AuthHubPermissionAdapter(
            GatewayClient gatewayClient,
            AuthHubPermissionMapper permissionMapper,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.gatewayClient = gatewayClient;
        this.permissionMapper = permissionMapper;
        this.retry = retryRegistry.retry(CIRCUIT_BREAKER_NAME);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    /**
     * Permission Spec 조회 (SDK)
     *
     * <p>GatewayClient SDK를 사용하여 Permission Spec을 조회합니다.
     *
     * @return Permission Spec
     */
    @Override
    public Mono<PermissionSpec> fetchPermissionSpec() {
        log.debug("Fetching permission spec via SDK");

        return executeWithResilience(
                () -> callPermissionSpecApi(),
                spec ->
                        log.debug(
                                "Fetched permission spec: {} permissions",
                                spec.permissions().size()),
                e -> log.error("Failed to fetch permission spec", e));
    }

    /**
     * 사용자별 Permission Hash 조회 (SDK)
     *
     * <p>GatewayClient SDK를 사용하여 사용자 권한 해시를 조회합니다.
     *
     * <p>Note: tenantId는 SDK 내부에서 ServiceToken의 tenantId를 사용합니다.
     *
     * @param tenantId 테넌트 ID (현재 미사용, SDK에서 ServiceToken 기반 처리)
     * @param userId 사용자 ID
     * @return Permission Hash
     */
    @Override
    public Mono<PermissionHash> fetchUserPermissions(String tenantId, String userId) {
        log.debug("Fetching user permissions via SDK: tenantId={}, userId={}", tenantId, userId);

        return executeWithResilience(
                () -> callUserPermissionsApi(userId),
                hash ->
                        log.debug(
                                "Fetched user permissions: userId={}, hash={}",
                                userId,
                                hash.hash()),
                e -> log.error("Failed to fetch user permissions: userId={}", userId, e));
    }

    /**
     * Resilience 패턴 적용 공통 메서드
     *
     * <p>Retry + Circuit Breaker 패턴을 적용하여 API 호출을 수행합니다.
     *
     * @param apiCall API 호출 Callable
     * @param onSuccess 성공 시 콜백
     * @param onError 에러 시 콜백
     * @param <T> 반환 타입
     * @return Mono 결과
     */
    private <T> Mono<T> executeWithResilience(
            Callable<T> apiCall, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        return Mono.fromCallable(apiCall)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(onSuccess)
                .doOnError(onError);
    }

    /**
     * Permission Spec API 호출
     *
     * @return PermissionSpec
     * @throws PermissionException API 호출 실패 시
     */
    private PermissionSpec callPermissionSpecApi() {
        try {
            ApiResponse<EndpointPermissionSpecList> response =
                    gatewayClient.internal().getPermissionSpec();

            validateResponse(response, "permission spec");
            return permissionMapper.toPermissionSpec(response.data());
        } catch (AuthHubException e) {
            throw new PermissionException("Failed to fetch permission spec: " + e.getMessage(), e);
        }
    }

    /**
     * User Permissions API 호출
     *
     * @param userId 사용자 ID
     * @return PermissionHash
     * @throws PermissionException API 호출 실패 시
     */
    private PermissionHash callUserPermissionsApi(String userId) {
        try {
            ApiResponse<UserPermissions> response =
                    gatewayClient.internal().getUserPermissions(userId);

            validateResponse(response, "user permissions");
            return permissionMapper.toPermissionHash(response.data());
        } catch (AuthHubException e) {
            throw new PermissionException("Failed to fetch user permissions: " + e.getMessage(), e);
        }
    }

    /**
     * API 응답 유효성 검증
     *
     * @param response API 응답
     * @param operation 작업명 (로깅용)
     * @throws PermissionException 응답이 유효하지 않을 때
     */
    private <T> void validateResponse(ApiResponse<T> response, String operation) {
        if (!response.success() || response.data() == null) {
            throw new PermissionException(
                    "Failed to fetch " + operation + ": empty response from AuthHub");
        }
    }
}
