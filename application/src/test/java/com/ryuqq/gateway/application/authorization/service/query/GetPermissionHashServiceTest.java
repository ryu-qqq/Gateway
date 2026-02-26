package com.ryuqq.gateway.application.authorization.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.internal.PermissionHashCoordinator;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import java.time.Instant;
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
@DisplayName("GetPermissionHashService 단위 테스트")
class GetPermissionHashServiceTest {

    @Mock private PermissionHashCoordinator permissionHashCoordinator;

    @InjectMocks private GetPermissionHashService getPermissionHashService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String JWT_HASH = "jwt-hash-abc";

    @Nested
    @DisplayName("getPermissionHash() 테스트")
    class GetPermissionHashTest {

        @Test
        @DisplayName("Coordinator를 통해 Permission Hash 조회")
        void shouldGetPermissionHashThroughCoordinator() {
            // given
            PermissionHash permissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashCoordinator.findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash).isEqualTo(permissionHash);
                                assertThat(hash.hash()).isEqualTo(JWT_HASH);
                            })
                    .verifyComplete();

            then(permissionHashCoordinator)
                    .should()
                    .findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH);
        }

        @Test
        @DisplayName("Coordinator에서 빈 Mono 반환 시 빈 Mono 반환")
        void shouldReturnEmptyMonoWhenCoordinatorReturnsEmpty() {
            // given
            given(permissionHashCoordinator.findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).verifyComplete();

            then(permissionHashCoordinator)
                    .should()
                    .findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH);
        }

        @Test
        @DisplayName("Coordinator에서 에러 발생 시 에러 전파")
        void shouldPropagateErrorFromCoordinator() {
            // given
            given(permissionHashCoordinator.findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH))
                    .willReturn(Mono.error(new RuntimeException("Coordinator error")));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectErrorMessage("Coordinator error").verify();

            then(permissionHashCoordinator)
                    .should()
                    .findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH);
        }
    }

    @Nested
    @DisplayName("권한 데이터 검증")
    class PermissionDataValidation {

        @Test
        @DisplayName("조회한 Hash의 권한 목록 검증")
        void validatePermissionsInFetchedHash() {
            // given
            Set<Permission> permissions =
                    Set.of(Permission.of("order:read"), Permission.of("order:create"));
            Set<String> roles = Set.of("ADMIN", "USER");

            PermissionHash permissionHash =
                    PermissionHash.of(JWT_HASH, permissions, roles, Instant.now());

            given(permissionHashCoordinator.findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash.permissions()).hasSize(2);
                                assertThat(hash.roles()).hasSize(2);
                                assertThat(hash.permissions())
                                        .containsExactlyInAnyOrderElementsOf(permissions);
                                assertThat(hash.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("빈 권한 목록도 정상 처리")
        void handleEmptyPermissionsList() {
            // given
            PermissionHash emptyHash =
                    PermissionHash.of(JWT_HASH, Set.of(), Set.of(), Instant.now());

            given(permissionHashCoordinator.findByTenantAndUser(TENANT_ID, USER_ID, JWT_HASH))
                    .willReturn(Mono.just(emptyHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash.permissions()).isEmpty();
                                assertThat(hash.roles()).isEmpty();
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("파라미터 전달 테스트")
    class ParameterPassingTest {

        @Test
        @DisplayName("tenantId, userId, jwtHash가 정확히 Coordinator에 전달되는지 검증")
        void shouldPassParametersCorrectlyToCoordinator() {
            // given
            String customTenantId = "custom-tenant";
            String customUserId = "custom-user";
            String customJwtHash = "custom-jwt-hash";

            PermissionHash permissionHash = createPermissionHash(customJwtHash);

            given(
                            permissionHashCoordinator.findByTenantAndUser(
                                    customTenantId, customUserId, customJwtHash))
                    .willReturn(Mono.just(permissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(
                            customTenantId, customUserId, customJwtHash);

            // then
            StepVerifier.create(result).expectNext(permissionHash).verifyComplete();

            then(permissionHashCoordinator)
                    .should()
                    .findByTenantAndUser(customTenantId, customUserId, customJwtHash);
        }
    }

    // Helper methods
    private PermissionHash createPermissionHash(String hash) {
        return PermissionHash.of(
                hash, Set.of(Permission.of("order:read")), Set.of("USER"), Instant.now());
    }
}
