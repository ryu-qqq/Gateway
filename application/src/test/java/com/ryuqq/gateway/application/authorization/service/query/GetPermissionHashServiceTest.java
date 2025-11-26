package com.ryuqq.gateway.application.authorization.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authorization.port.out.client.AuthHubPermissionClient;
import com.ryuqq.gateway.application.authorization.port.out.command.PermissionHashCommandPort;
import com.ryuqq.gateway.application.authorization.port.out.query.PermissionHashQueryPort;
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

    @Mock private PermissionHashQueryPort permissionHashQueryPort;

    @Mock private PermissionHashCommandPort permissionHashCommandPort;

    @Mock private AuthHubPermissionClient authHubPermissionClient;

    @InjectMocks private GetPermissionHashService getPermissionHashService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String JWT_HASH = "jwt-hash-abc";
    private static final String CACHED_HASH = "cached-hash-xyz";

    @Nested
    @DisplayName("Cache Hit - Hash 일치")
    class CacheHitWithMatchingHash {

        @Test
        @DisplayName("캐시에 Hash가 존재하고 JWT Hash와 일치하면 캐시에서 반환")
        void returnCachedHashWhenMatches() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash).isEqualTo(cachedPermissionHash);
                                assertThat(hash.hash()).isEqualTo(JWT_HASH);
                            })
                    .verifyComplete();

            then(authHubPermissionClient).shouldHaveNoInteractions();
            then(permissionHashCommandPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Hash 일치 시 AuthHub 호출하지 않음")
        void notCallAuthHubWhenHashMatches() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectNextCount(1).verifyComplete();

            then(authHubPermissionClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Cache Hit - Hash 불일치")
    class CacheHitWithMismatchedHash {

        @Test
        @DisplayName("캐시에 Hash가 존재하지만 JWT Hash와 불일치하면 AuthHub에서 재조회")
        void refetchFromAuthHubWhenHashMismatches() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash(CACHED_HASH);
            PermissionHash freshPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(freshPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, freshPermissionHash))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash).isEqualTo(freshPermissionHash);
                                assertThat(hash.hash()).isEqualTo(JWT_HASH);
                            })
                    .verifyComplete();

            then(authHubPermissionClient).should().fetchUserPermissions(TENANT_ID, USER_ID);
            then(permissionHashCommandPort).should().save(TENANT_ID, USER_ID, freshPermissionHash);
        }

        @Test
        @DisplayName("Hash 불일치 시 새로운 Hash를 캐시에 저장")
        void saveNewHashToCacheWhenMismatches() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash(CACHED_HASH);
            PermissionHash freshPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(freshPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, freshPermissionHash))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectNext(freshPermissionHash).verifyComplete();

            then(permissionHashCommandPort).should().save(TENANT_ID, USER_ID, freshPermissionHash);
        }
    }

    @Nested
    @DisplayName("Cache Miss")
    class CacheMissScenario {

        @Test
        @DisplayName("캐시에 Hash가 없으면 AuthHub에서 조회 후 캐시")
        void fetchFromAuthHubAndCacheWhenCacheMiss() {
            // given
            PermissionHash fetchedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(fetchedPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, fetchedPermissionHash))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            hash -> {
                                assertThat(hash).isEqualTo(fetchedPermissionHash);
                                assertThat(hash.hash()).isEqualTo(JWT_HASH);
                            })
                    .verifyComplete();

            then(authHubPermissionClient).should().fetchUserPermissions(TENANT_ID, USER_ID);
            then(permissionHashCommandPort)
                    .should()
                    .save(TENANT_ID, USER_ID, fetchedPermissionHash);
        }

        @Test
        @DisplayName("캐시 미스 시 AuthHub에서 조회한 Hash를 캐시에 저장")
        void saveHashToCacheAfterFetchingFromAuthHub() {
            // given
            PermissionHash fetchedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(fetchedPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, fetchedPermissionHash))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectNext(fetchedPermissionHash).verifyComplete();

            then(permissionHashCommandPort)
                    .should()
                    .save(TENANT_ID, USER_ID, fetchedPermissionHash);
        }
    }

    @Nested
    @DisplayName("2-Tier 캐시 전략 검증")
    class TwoTierCacheStrategy {

        @Test
        @DisplayName("JWT Hash → Redis → AuthHub 순서로 조회")
        void followTwoTierCacheStrategy() {
            // given
            PermissionHash fetchedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(fetchedPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, fetchedPermissionHash))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectNext(fetchedPermissionHash).verifyComplete();

            then(permissionHashQueryPort).should().findByTenantAndUser(TENANT_ID, USER_ID);
            then(authHubPermissionClient).should().fetchUserPermissions(TENANT_ID, USER_ID);
            then(permissionHashCommandPort)
                    .should()
                    .save(TENANT_ID, USER_ID, fetchedPermissionHash);
        }

        @Test
        @DisplayName("캐시에서 찾으면 AuthHub 호출 생략")
        void skipAuthHubWhenFoundInCache() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectNext(cachedPermissionHash).verifyComplete();

            then(authHubPermissionClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("오류 처리")
    class ErrorHandling {

        @Test
        @DisplayName("캐시 조회 실패 시 AuthHub에서 조회")
        void fetchFromAuthHubWhenCacheQueryFails() {
            // given
            PermissionHash fetchedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.error(new RuntimeException("Cache query failed")));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectErrorMessage("Cache query failed").verify();

            then(authHubPermissionClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("AuthHub 조회 실패 시 에러 전파")
        void propagateErrorWhenAuthHubFetchFails() {
            // given
            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.error(new RuntimeException("AuthHub fetch failed")));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectErrorMessage("AuthHub fetch failed").verify();
        }

        @Test
        @DisplayName("캐시 저장 실패해도 조회한 Hash 반환하지 않고 에러 전파")
        void propagateErrorWhenCacheSaveFails() {
            // given
            PermissionHash fetchedPermissionHash = createPermissionHash(JWT_HASH);

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(fetchedPermissionHash));
            given(permissionHashCommandPort.save(TENANT_ID, USER_ID, fetchedPermissionHash))
                    .willReturn(Mono.error(new RuntimeException("Cache save failed")));

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).expectErrorMessage("Cache save failed").verify();
        }

        @Test
        @DisplayName("AuthHub가 빈 응답 반환 시 빈 Mono 반환")
        void returnEmptyMonoWhenAuthHubReturnsEmpty() {
            // given
            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, JWT_HASH);

            // then
            StepVerifier.create(result).verifyComplete();

            then(permissionHashCommandPort).shouldHaveNoInteractions();
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

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
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

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
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
    @DisplayName("Hash 매칭 로직")
    class HashMatchingLogic {

        @Test
        @DisplayName("대소문자 구분하여 Hash 비교")
        void comparHashCaseSensitively() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash("ABC123");

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(createPermissionHash("abc123")));
            given(permissionHashCommandPort.save(any(), any(), any())).willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, "abc123");

            // then
            StepVerifier.create(result).expectNextCount(1).verifyComplete();

            then(authHubPermissionClient).should().fetchUserPermissions(TENANT_ID, USER_ID);
        }

        @Test
        @DisplayName("정확히 일치하는 Hash만 캐시 히트로 판단")
        void requireExactHashMatch() {
            // given
            PermissionHash cachedPermissionHash = createPermissionHash("hash-123");

            given(permissionHashQueryPort.findByTenantAndUser(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(cachedPermissionHash));
            given(authHubPermissionClient.fetchUserPermissions(TENANT_ID, USER_ID))
                    .willReturn(Mono.just(createPermissionHash("hash-456")));
            given(permissionHashCommandPort.save(any(), any(), any())).willReturn(Mono.empty());

            // when
            Mono<PermissionHash> result =
                    getPermissionHashService.getPermissionHash(TENANT_ID, USER_ID, "hash-456");

            // then
            StepVerifier.create(result).expectNextCount(1).verifyComplete();

            then(authHubPermissionClient).should().fetchUserPermissions(TENANT_ID, USER_ID);
        }
    }

    // Helper methods
    private PermissionHash createPermissionHash(String hash) {
        return PermissionHash.of(
                hash, Set.of(Permission.of("order:read")), Set.of("USER"), Instant.now());
    }
}
