package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.authhub.sdk.api.InternalApi;
import com.ryuqq.authhub.sdk.client.GatewayClient;
import com.ryuqq.authhub.sdk.exception.AuthHubServerException;
import com.ryuqq.authhub.sdk.model.common.ApiResponse;
import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpec;
import com.ryuqq.authhub.sdk.model.internal.EndpointPermissionSpecList;
import com.ryuqq.authhub.sdk.model.internal.UserPermissions;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.PermissionException;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubPermissionMapper;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/**
 * AuthHubPermissionAdapter 단위 테스트
 *
 * <p>Permission Spec: SDK (GatewayClient), User Permissions: SDK (GatewayClient)
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHubPermissionAdapter 단위 테스트")
class AuthHubPermissionAdapterTest {

    @Mock private GatewayClient gatewayClient;
    @Mock private InternalApi internalApi;
    @Mock private AuthHubPermissionMapper permissionMapper;

    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private AuthHubPermissionAdapter adapter;

    @BeforeEach
    void setUp() {
        RetryConfig retryConfig =
                RetryConfig.custom().maxAttempts(1).waitDuration(Duration.ofMillis(10)).build();
        retryRegistry = RetryRegistry.of(retryConfig);

        CircuitBreakerConfig cbConfig =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .permittedNumberOfCallsInHalfOpenState(1)
                        .slidingWindowSize(2)
                        .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        adapter =
                new AuthHubPermissionAdapter(
                        gatewayClient, permissionMapper, retryRegistry, circuitBreakerRegistry);
    }

    @Nested
    @DisplayName("fetchPermissionSpec 테스트")
    class FetchPermissionSpecTest {

        @Test
        @DisplayName("Permission Spec 조회 성공 (SDK)")
        void fetchPermissionSpec_success() {
            // given
            EndpointPermissionSpec sdkSpec =
                    new EndpointPermissionSpec(
                            "gateway",
                            "/api/v1/users",
                            "GET",
                            List.of("user:read"),
                            List.of("USER"),
                            false,
                            "User list endpoint");
            EndpointPermissionSpecList specList =
                    new EndpointPermissionSpecList("v1", Instant.now(), List.of(sdkSpec));

            ApiResponse<EndpointPermissionSpecList> apiResponse =
                    new ApiResponse<>(true, specList, LocalDateTime.now(), "req-123");

            EndpointPermission domainPermission =
                    EndpointPermission.of(
                            "gateway",
                            "/api/v1/users",
                            HttpMethod.GET,
                            Set.of(Permission.of("user:read")),
                            Set.of("USER"),
                            false);
            PermissionSpec expectedSpec =
                    PermissionSpec.of(1L, Instant.now(), List.of(domainPermission));

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getPermissionSpec()).willReturn(apiResponse);
            given(permissionMapper.toPermissionSpec(specList)).willReturn(expectedSpec);

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .assertNext(spec -> assertThat(spec.permissions()).hasSize(1))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Permission Spec 조회 실패 - SDK 예외")
        void fetchPermissionSpec_sdkException() {
            // given
            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getPermissionSpec())
                    .willThrow(
                            new AuthHubServerException(
                                    500, "Internal server error", "SERVER_ERROR"));

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("Permission Spec 조회 실패 - 빈 응답")
        void fetchPermissionSpec_emptyResponse() {
            // given
            ApiResponse<EndpointPermissionSpecList> emptyResponse =
                    new ApiResponse<>(true, null, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getPermissionSpec()).willReturn(emptyResponse);

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("Permission Spec 조회 실패 - success=false")
        void fetchPermissionSpec_failureResponse() {
            // given
            ApiResponse<EndpointPermissionSpecList> failureResponse =
                    new ApiResponse<>(false, null, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getPermissionSpec()).willReturn(failureResponse);

            // when & then
            StepVerifier.create(adapter.fetchPermissionSpec())
                    .expectError(PermissionException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("fetchUserPermissions 테스트")
    class FetchUserPermissionsTest {

        @Test
        @DisplayName("사용자 권한 조회 성공 (SDK)")
        void fetchUserPermissions_success() {
            // given
            String tenantId = "tenant-1";
            String userId = "user-123";

            Instant generatedAt = Instant.now();
            UserPermissions sdkResponse =
                    new UserPermissions(
                            "hash-abc", Set.of("user:read"), Set.of("USER"), userId, generatedAt);

            PermissionHash expectedHash =
                    PermissionHash.fromStrings(
                            "hash-abc", Set.of("user:read"), Set.of("USER"), generatedAt);

            ApiResponse<UserPermissions> apiResponse =
                    new ApiResponse<>(true, sdkResponse, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId)).willReturn(apiResponse);
            given(permissionMapper.toPermissionHash(sdkResponse)).willReturn(expectedHash);

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .assertNext(hash -> assertThat(hash.hash()).isEqualTo("hash-abc"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("사용자 권한 조회 실패 - SDK 예외 (404)")
        void fetchUserPermissions_notFound() {
            // given
            String tenantId = "tenant-1";
            String userId = "non-existent-user";

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId))
                    .willThrow(new AuthHubServerException(404, "User not found", "NOT_FOUND"));

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("사용자 권한 조회 실패 - SDK 예외 (500)")
        void fetchUserPermissions_serverError() {
            // given
            String tenantId = "tenant-1";
            String userId = "user-123";

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId))
                    .willThrow(
                            new AuthHubServerException(
                                    500, "Internal Server Error", "SERVER_ERROR"));

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("사용자 권한 조회 실패 - 빈 응답")
        void fetchUserPermissions_emptyResponse() {
            // given
            String tenantId = "tenant-1";
            String userId = "user-123";

            ApiResponse<UserPermissions> emptyResponse =
                    new ApiResponse<>(true, null, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId)).willReturn(emptyResponse);

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("사용자 권한 조회 실패 - success=false")
        void fetchUserPermissions_failureResponse() {
            // given
            String tenantId = "tenant-1";
            String userId = "user-123";

            ApiResponse<UserPermissions> failureResponse =
                    new ApiResponse<>(false, null, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId)).willReturn(failureResponse);

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .expectError(PermissionException.class)
                    .verify();
        }

        @Test
        @DisplayName("사용자 권한 조회 실패 - Mapper 예외 (null response)")
        void fetchUserPermissions_mapperException() {
            // given
            String tenantId = "tenant-1";
            String userId = "user-123";

            UserPermissions nullFieldsResponse = new UserPermissions(null, null, null, null, null);
            ApiResponse<UserPermissions> apiResponse =
                    new ApiResponse<>(true, nullFieldsResponse, LocalDateTime.now(), "req-123");

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getUserPermissions(userId)).willReturn(apiResponse);
            given(permissionMapper.toPermissionHash(any(UserPermissions.class)))
                    .willThrow(new PermissionException("Empty UserPermissions response"));

            // when & then
            StepVerifier.create(adapter.fetchUserPermissions(tenantId, userId))
                    .expectError(PermissionException.class)
                    .verify();
        }
    }
}
