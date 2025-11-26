package com.ryuqq.gateway.domain.authorization.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionSpec 단위 테스트")
class PermissionSpecTest {

    private static final Long VERSION = 1L;
    private static final Instant NOW = Instant.now();

    @Nested
    @DisplayName("생성 및 팩토리 메서드")
    class Creation {

        @Test
        @DisplayName("유효한 값으로 PermissionSpec 생성 성공")
        void createPermissionSpecWithValidValues() {
            // given
            List<EndpointPermission> permissions =
                    List.of(
                            createEndpointPermission("/api/v1/orders", HttpMethod.GET),
                            createEndpointPermission("/api/v1/products", HttpMethod.POST));

            // when
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, permissions);

            // then
            assertThat(spec.version()).isEqualTo(VERSION);
            assertThat(spec.updatedAt()).isEqualTo(NOW);
            assertThat(spec.permissions()).hasSize(2);
        }

        @Test
        @DisplayName("null version으로 생성 시 예외 발생")
        void throwExceptionWhenVersionIsNull() {
            // when & then
            assertThatThrownBy(() -> PermissionSpec.of(null, NOW, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Version cannot be null");
        }

        @Test
        @DisplayName("null updatedAt은 현재 시각으로 초기화")
        void initializeNullUpdatedAtAsNow() {
            // given
            Instant before = Instant.now();

            // when
            PermissionSpec spec = PermissionSpec.of(VERSION, null, List.of());

            // then
            Instant after = Instant.now();
            assertThat(spec.updatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("null permissions는 빈 List로 초기화")
        void initializeNullPermissionsAsEmptyList() {
            // when
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, null);

            // then
            assertThat(spec.permissions()).isEmpty();
        }

        @Test
        @DisplayName("불변성 보장 - permissions는 복사본")
        void ensureImmutabilityOfPermissions() {
            // given - mutable List 사용하여 불변성 검증
            java.util.List<EndpointPermission> originalPermissions = new java.util.ArrayList<>();
            originalPermissions.add(createEndpointPermission("/api/v1/orders", HttpMethod.GET));

            // when
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, originalPermissions);

            // then - 원본 수정해도 내부 상태에 영향 없음
            originalPermissions.add(createEndpointPermission("/api/v1/users", HttpMethod.POST));
            assertThat(spec.permissions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("엔드포인트 권한 찾기")
    class FindPermission {

        @Test
        @DisplayName("정확히 일치하는 경로와 메서드로 권한 찾기")
        void findPermissionWithExactMatch() {
            // given
            EndpointPermission expected =
                    createEndpointPermission("/api/v1/orders", HttpMethod.GET);
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(
                                    expected,
                                    createEndpointPermission("/api/v1/products", HttpMethod.POST)));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/orders", HttpMethod.GET);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }

        @Test
        @DisplayName("Path Variable 패턴 매칭으로 권한 찾기")
        void findPermissionWithPathVariableMatching() {
            // given
            EndpointPermission expected =
                    createEndpointPermission("/api/v1/orders/{orderId}", HttpMethod.GET);
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, List.of(expected));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/orders/123", HttpMethod.GET);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }

        @Test
        @DisplayName("여러 Path Variable 패턴 매칭")
        void findPermissionWithMultiplePathVariables() {
            // given
            EndpointPermission expected =
                    createEndpointPermission(
                            "/api/v1/users/{userId}/orders/{orderId}", HttpMethod.GET);
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, List.of(expected));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/users/456/orders/789", HttpMethod.GET);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }

        @Test
        @DisplayName("메서드가 다르면 권한을 찾지 못함")
        void notFindPermissionWhenMethodDiffers() {
            // given
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(createEndpointPermission("/api/v1/orders", HttpMethod.GET)));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/orders", HttpMethod.POST);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("경로가 다르면 권한을 찾지 못함")
        void notFindPermissionWhenPathDiffers() {
            // given
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(createEndpointPermission("/api/v1/orders", HttpMethod.GET)));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/products", HttpMethod.GET);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 permissions에서는 권한을 찾지 못함")
        void notFindPermissionInEmptyPermissions() {
            // given
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, List.of());

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/orders", HttpMethod.GET);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("첫 번째 매칭되는 권한 반환")
        void returnFirstMatchingPermission() {
            // given
            EndpointPermission first =
                    createEndpointPermission("/api/v1/orders/{orderId}", HttpMethod.GET);
            EndpointPermission second =
                    createEndpointPermission("/api/v1/orders/{id}", HttpMethod.GET);
            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, List.of(first, second));

            // when
            Optional<EndpointPermission> result =
                    spec.findPermission("/api/v1/orders/123", HttpMethod.GET);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("서비스별 엔드포인트 찾기")
    class FindByServiceName {

        @Test
        @DisplayName("특정 서비스의 엔드포인트 목록 반환")
        void findEndpointsByServiceName() {
            // given
            EndpointPermission orderEndpoint1 =
                    createEndpointPermission("order-service", "/api/v1/orders", HttpMethod.GET);
            EndpointPermission orderEndpoint2 =
                    createEndpointPermission("order-service", "/api/v1/orders", HttpMethod.POST);
            EndpointPermission productEndpoint =
                    createEndpointPermission("product-service", "/api/v1/products", HttpMethod.GET);

            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION, NOW, List.of(orderEndpoint1, orderEndpoint2, productEndpoint));

            // when
            List<EndpointPermission> result = spec.findByServiceName("order-service");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(orderEndpoint1, orderEndpoint2);
        }

        @Test
        @DisplayName("해당 서비스가 없으면 빈 목록 반환")
        void returnEmptyListWhenServiceNotFound() {
            // given
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(
                                    createEndpointPermission(
                                            "order-service", "/api/v1/orders", HttpMethod.GET)));

            // when
            List<EndpointPermission> result = spec.findByServiceName("product-service");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("버전 비교")
    class VersionComparison {

        @Test
        @DisplayName("현재 버전이 더 최신이면 true 반환")
        void returnTrueWhenCurrentVersionIsNewer() {
            // given
            PermissionSpec spec = PermissionSpec.of(10L, NOW, List.of());

            // when & then
            assertThat(spec.isNewerThan(5L)).isTrue();
            assertThat(spec.isNewerThan(9L)).isTrue();
        }

        @Test
        @DisplayName("현재 버전이 더 오래되었으면 false 반환")
        void returnFalseWhenCurrentVersionIsOlder() {
            // given
            PermissionSpec spec = PermissionSpec.of(5L, NOW, List.of());

            // when & then
            assertThat(spec.isNewerThan(10L)).isFalse();
        }

        @Test
        @DisplayName("버전이 같으면 false 반환")
        void returnFalseWhenVersionsAreEqual() {
            // given
            PermissionSpec spec = PermissionSpec.of(5L, NOW, List.of());

            // when & then
            assertThat(spec.isNewerThan(5L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Public 엔드포인트 필터링")
    class PublicEndpoints {

        @Test
        @DisplayName("Public 엔드포인트만 반환")
        void returnOnlyPublicEndpoints() {
            // given
            EndpointPermission publicEndpoint1 =
                    EndpointPermission.publicEndpoint("service", "/api/v1/public", HttpMethod.GET);
            EndpointPermission publicEndpoint2 =
                    EndpointPermission.publicEndpoint("service", "/api/v1/health", HttpMethod.GET);
            EndpointPermission protectedEndpoint =
                    createProtectedEndpoint("/api/v1/orders", HttpMethod.GET);

            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(publicEndpoint1, protectedEndpoint, publicEndpoint2));

            // when
            List<EndpointPermission> result = spec.publicEndpoints();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(publicEndpoint1, publicEndpoint2);
        }

        @Test
        @DisplayName("Public 엔드포인트가 없으면 빈 목록 반환")
        void returnEmptyListWhenNoPublicEndpoints() {
            // given
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(createProtectedEndpoint("/api/v1/orders", HttpMethod.GET)));

            // when
            List<EndpointPermission> result = spec.publicEndpoints();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Protected 엔드포인트 필터링")
    class ProtectedEndpoints {

        @Test
        @DisplayName("권한이 필요한 엔드포인트만 반환")
        void returnOnlyProtectedEndpoints() {
            // given
            EndpointPermission protectedEndpoint1 =
                    createProtectedEndpoint("/api/v1/orders", HttpMethod.GET);
            EndpointPermission protectedEndpoint2 =
                    createProtectedEndpoint("/api/v1/products", HttpMethod.POST);
            EndpointPermission publicEndpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/public", HttpMethod.GET);

            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(protectedEndpoint1, publicEndpoint, protectedEndpoint2));

            // when
            List<EndpointPermission> result = spec.protectedEndpoints();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(protectedEndpoint1, protectedEndpoint2);
        }

        @Test
        @DisplayName("Protected 엔드포인트가 없으면 빈 목록 반환")
        void returnEmptyListWhenNoProtectedEndpoints() {
            // given
            PermissionSpec spec =
                    PermissionSpec.of(
                            VERSION,
                            NOW,
                            List.of(
                                    EndpointPermission.publicEndpoint(
                                            "service", "/api/v1/public", HttpMethod.GET)));

            // when
            List<EndpointPermission> result = spec.protectedEndpoints();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("권한과 역할이 모두 없는 비공개 엔드포인트는 제외")
        void excludeNonPublicEndpointsWithoutPermissionsOrRoles() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "service",
                            "/api/v1/internal",
                            HttpMethod.GET,
                            Set.of(),
                            Set.of(),
                            false);

            PermissionSpec spec = PermissionSpec.of(VERSION, NOW, List.of(endpoint));

            // when
            List<EndpointPermission> result = spec.protectedEndpoints();

            // then
            assertThat(result).isEmpty();
        }
    }

    // Helper methods
    private EndpointPermission createEndpointPermission(String path, HttpMethod method) {
        return EndpointPermission.of(
                "test-service", path, method, Set.of(Permission.of("test:read")), Set.of(), false);
    }

    private EndpointPermission createEndpointPermission(
            String serviceName, String path, HttpMethod method) {
        return EndpointPermission.of(
                serviceName, path, method, Set.of(Permission.of("test:read")), Set.of(), false);
    }

    private EndpointPermission createProtectedEndpoint(String path, HttpMethod method) {
        return EndpointPermission.of(
                "test-service",
                path,
                method,
                Set.of(Permission.of("order:read")),
                Set.of("USER"),
                false);
    }
}
