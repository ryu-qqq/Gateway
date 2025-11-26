package com.ryuqq.gateway.application.authorization.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.dto.response.ValidatePermissionResponse;
import com.ryuqq.gateway.application.authorization.service.query.GetPermissionHashService;
import com.ryuqq.gateway.application.authorization.service.query.GetPermissionSpecService;
import com.ryuqq.gateway.domain.authorization.exception.PermissionDeniedException;
import com.ryuqq.gateway.domain.authorization.exception.PermissionSpecNotFoundException;
import com.ryuqq.gateway.domain.authorization.vo.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePermissionService 단위 테스트")
class ValidatePermissionServiceTest {

    @Mock private GetPermissionSpecService getPermissionSpecService;

    @Mock private GetPermissionHashService getPermissionHashService;

    @InjectMocks private ValidatePermissionService validatePermissionService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PERMISSION_HASH = "hash-abc";

    @Nested
    @DisplayName("Public 엔드포인트 처리")
    class PublicEndpointHandling {

        @Test
        @DisplayName("Public 엔드포인트는 권한 검증 없이 통과")
        void allowPublicEndpointWithoutPermissionCheck() {
            // given
            EndpointPermission publicEndpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/public", HttpMethod.GET);
            PermissionSpec spec = createPermissionSpec(List.of(publicEndpoint));

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/public", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.authorized()).isTrue();
                                assertThat(response.endpointPermission()).isEqualTo(publicEndpoint);
                            })
                    .verifyComplete();

            then(getPermissionHashService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Protected 엔드포인트 권한 검증")
    class ProtectedEndpointValidation {

        @Test
        @DisplayName("필수 권한을 모두 보유한 경우 통과")
        void allowWhenUserHasAllRequiredPermissions() {
            // given
            Permission orderRead = Permission.of("order:read");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders", HttpMethod.GET, Set.of(orderRead), Set.of());
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(
                            PERMISSION_HASH,
                            Set.of(orderRead, Permission.of("order:create")),
                            Set.of(),
                            Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.authorized()).isTrue();
                                assertThat(response.endpointPermission()).isEqualTo(endpoint);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("와일드카드 권한으로 필수 권한 충족")
        void allowWhenUserHasWildcardPermission() {
            // given
            Permission orderRead = Permission.of("order:read");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders", HttpMethod.GET, Set.of(orderRead), Set.of());
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(
                            PERMISSION_HASH,
                            Set.of(Permission.of("order:*")),
                            Set.of(),
                            Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.authorized()).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("필수 권한이 부족한 경우 예외 발생")
        void throwExceptionWhenUserLacksRequiredPermissions() {
            // given
            Permission orderRead = Permission.of("order:read");
            Permission orderCreate = Permission.of("order:create");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders",
                            HttpMethod.POST,
                            Set.of(orderRead, orderCreate),
                            Set.of());
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(
                            PERMISSION_HASH,
                            Set.of(orderRead), // orderCreate 없음
                            Set.of(),
                            Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID,
                            TENANT_ID,
                            PERMISSION_HASH,
                            Set.of(),
                            "/api/v1/orders",
                            "POST");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectError(PermissionDeniedException.class).verify();
        }
    }

    @Nested
    @DisplayName("역할 기반 권한 검증")
    class RoleBasedValidation {

        @Test
        @DisplayName("필수 역할 중 하나를 보유한 경우 통과")
        void allowWhenUserHasAnyRequiredRole() {
            // given
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/admin", HttpMethod.GET, Set.of(), Set.of("ADMIN", "MANAGER"));
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(
                            PERMISSION_HASH, Set.of(), Set.of("MANAGER", "USER"), Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/admin", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.authorized()).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("필수 역할을 하나도 보유하지 않은 경우 예외 발생")
        void throwExceptionWhenUserLacksRequiredRoles() {
            // given
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/admin", HttpMethod.GET, Set.of(), Set.of("ADMIN"));
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(PERMISSION_HASH, Set.of(), Set.of("USER"), Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/admin", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectError(PermissionDeniedException.class).verify();
        }

        @Test
        @DisplayName("권한과 역할을 모두 충족해야 통과")
        void allowWhenUserHasBothPermissionsAndRoles() {
            // given
            Permission orderRead = Permission.of("order:read");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders", HttpMethod.GET, Set.of(orderRead), Set.of("USER"));
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(
                            PERMISSION_HASH, Set.of(orderRead), Set.of("USER"), Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.authorized()).isTrue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("엔드포인트 Spec 미발견 처리")
    class SpecNotFoundHandling {

        @Test
        @DisplayName("매칭되는 엔드포인트가 없으면 예외 발생")
        void throwExceptionWhenEndpointNotFound() {
            // given
            PermissionSpec spec =
                    createPermissionSpec(
                            List.of(
                                    createProtectedEndpoint(
                                            "/api/v1/orders", HttpMethod.GET, Set.of(), Set.of())));

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID,
                            TENANT_ID,
                            PERMISSION_HASH,
                            Set.of(),
                            "/api/v1/products",
                            "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectError(PermissionSpecNotFoundException.class).verify();
        }

        @Test
        @DisplayName("HTTP 메서드가 다르면 예외 발생")
        void throwExceptionWhenHttpMethodDiffers() {
            // given
            PermissionSpec spec =
                    createPermissionSpec(
                            List.of(
                                    createProtectedEndpoint(
                                            "/api/v1/orders", HttpMethod.GET, Set.of(), Set.of())));

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID,
                            TENANT_ID,
                            PERMISSION_HASH,
                            Set.of(),
                            "/api/v1/orders",
                            "POST");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectError(PermissionSpecNotFoundException.class).verify();
        }
    }

    @Nested
    @DisplayName("권한이 필요 없는 엔드포인트")
    class NoAuthorizationRequired {

        @Test
        @DisplayName("권한과 역할이 모두 없는 비공개 엔드포인트는 통과")
        void allowEndpointWithoutPermissionsOrRoles() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "service",
                            "/api/v1/internal",
                            HttpMethod.GET,
                            Set.of(),
                            Set.of(),
                            false);
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID,
                            TENANT_ID,
                            PERMISSION_HASH,
                            Set.of(),
                            "/api/v1/internal",
                            "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.authorized()).isTrue();
                                assertThat(response.endpointPermission()).isEqualTo(endpoint);
                            })
                    .verifyComplete();

            then(getPermissionHashService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Path Variable 매칭")
    class PathVariableMatching {

        @Test
        @DisplayName("Path Variable이 포함된 경로 매칭")
        void matchPathWithPathVariable() {
            // given
            Permission orderRead = Permission.of("order:read");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders/{orderId}",
                            HttpMethod.GET,
                            Set.of(orderRead),
                            Set.of());
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            PermissionHash permissionHash =
                    PermissionHash.of(PERMISSION_HASH, Set.of(orderRead), Set.of(), Instant.now());

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID,
                            TENANT_ID,
                            PERMISSION_HASH,
                            Set.of(),
                            "/api/v1/orders/12345",
                            "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(response -> assertThat(response.authorized()).isTrue())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("PermissionSpec 조회 실패 시 에러 전파")
        void propagateErrorWhenSpecFetchFails() {
            // given
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(getPermissionSpecService.getPermissionSpec())
                    .willReturn(Mono.error(new RuntimeException("Spec fetch failed")));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Spec fetch failed").verify();
        }

        @Test
        @DisplayName("PermissionHash 조회 실패 시 에러 전파")
        void propagateErrorWhenHashFetchFails() {
            // given
            Permission orderRead = Permission.of("order:read");
            EndpointPermission endpoint =
                    createProtectedEndpoint(
                            "/api/v1/orders", HttpMethod.GET, Set.of(orderRead), Set.of());
            PermissionSpec spec = createPermissionSpec(List.of(endpoint));

            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            USER_ID, TENANT_ID, PERMISSION_HASH, Set.of(), "/api/v1/orders", "GET");

            given(getPermissionSpecService.getPermissionSpec()).willReturn(Mono.just(spec));
            given(getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, PERMISSION_HASH))
                    .willReturn(Mono.error(new RuntimeException("Hash fetch failed")));

            // when
            Mono<ValidatePermissionResponse> result = validatePermissionService.execute(command);

            // then
            StepVerifier.create(result).expectErrorMessage("Hash fetch failed").verify();
        }
    }

    // Helper methods
    private PermissionSpec createPermissionSpec(List<EndpointPermission> permissions) {
        return PermissionSpec.of(1L, Instant.now(), permissions);
    }

    private EndpointPermission createProtectedEndpoint(
            String path, HttpMethod method, Set<Permission> permissions, Set<String> roles) {
        return EndpointPermission.of("test-service", path, method, permissions, roles, false);
    }
}
