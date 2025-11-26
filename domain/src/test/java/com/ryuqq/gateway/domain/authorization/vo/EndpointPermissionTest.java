package com.ryuqq.gateway.domain.authorization.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EndpointPermission 테스트")
class EndpointPermissionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 EndpointPermission 생성")
        void shouldCreateEndpointPermission() {
            // given
            String serviceName = "user-service";
            String path = "/api/v1/users";
            HttpMethod method = HttpMethod.GET;
            Set<Permission> permissions = Set.of(Permission.of("user:read"));
            Set<String> roles = Set.of("USER", "ADMIN");

            // when
            EndpointPermission endpoint =
                    new EndpointPermission(serviceName, path, method, permissions, roles, false);

            // then
            assertThat(endpoint.serviceName()).isEqualTo(serviceName);
            assertThat(endpoint.path()).isEqualTo(path);
            assertThat(endpoint.method()).isEqualTo(method);
            assertThat(endpoint.requiredPermissions()).isEqualTo(permissions);
            assertThat(endpoint.requiredRoles()).isEqualTo(roles);
            assertThat(endpoint.isPublic()).isFalse();
        }

        @Test
        @DisplayName("null 권한과 역할은 빈 Set으로 초기화")
        void shouldInitializeNullPermissionsAndRolesToEmptySet() {
            // when
            EndpointPermission endpoint =
                    new EndpointPermission("service", "/path", HttpMethod.GET, null, null, true);

            // then
            assertThat(endpoint.requiredPermissions()).isEmpty();
            assertThat(endpoint.requiredRoles()).isEmpty();
        }

        @Test
        @DisplayName("serviceName이 null이면 예외 발생")
        void shouldThrowExceptionWhenServiceNameIsNull() {
            assertThatThrownBy(
                            () ->
                                    new EndpointPermission(
                                            null,
                                            "/path",
                                            HttpMethod.GET,
                                            Set.of(),
                                            Set.of(),
                                            false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Service name cannot be null");
        }

        @Test
        @DisplayName("path가 null이면 예외 발생")
        void shouldThrowExceptionWhenPathIsNull() {
            assertThatThrownBy(
                            () ->
                                    new EndpointPermission(
                                            "service",
                                            null,
                                            HttpMethod.GET,
                                            Set.of(),
                                            Set.of(),
                                            false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Path cannot be null");
        }

        @Test
        @DisplayName("method가 null이면 예외 발생")
        void shouldThrowExceptionWhenMethodIsNull() {
            assertThatThrownBy(
                            () ->
                                    new EndpointPermission(
                                            "service", "/path", null, Set.of(), Set.of(), false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Method cannot be null");
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드 테스트")
    class StaticFactoryMethodTest {

        @Test
        @DisplayName("of() 메서드로 EndpointPermission 생성")
        void shouldCreateEndpointPermissionUsingOf() {
            // given
            String serviceName = "order-service";
            String path = "/api/v1/orders";
            HttpMethod method = HttpMethod.POST;
            Set<Permission> permissions = Set.of(Permission.of("order:create"));
            Set<String> roles = Set.of("ADMIN");

            // when
            EndpointPermission endpoint =
                    EndpointPermission.of(serviceName, path, method, permissions, roles, false);

            // then
            assertThat(endpoint.serviceName()).isEqualTo(serviceName);
            assertThat(endpoint.path()).isEqualTo(path);
            assertThat(endpoint.method()).isEqualTo(method);
            assertThat(endpoint.requiredPermissions()).isEqualTo(permissions);
            assertThat(endpoint.requiredRoles()).isEqualTo(roles);
            assertThat(endpoint.isPublic()).isFalse();
        }

        @Test
        @DisplayName("publicEndpoint() 메서드로 공개 엔드포인트 생성")
        void shouldCreatePublicEndpoint() {
            // when
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint(
                            "auth-service", "/api/v1/login", HttpMethod.POST);

            // then
            assertThat(endpoint.serviceName()).isEqualTo("auth-service");
            assertThat(endpoint.path()).isEqualTo("/api/v1/login");
            assertThat(endpoint.method()).isEqualTo(HttpMethod.POST);
            assertThat(endpoint.requiredPermissions()).isEmpty();
            assertThat(endpoint.requiredRoles()).isEmpty();
            assertThat(endpoint.isPublic()).isTrue();
        }
    }

    @Nested
    @DisplayName("requiresAuthorization() 테스트")
    class RequiresAuthorizationTest {

        @Test
        @DisplayName("공개 엔드포인트는 인가가 필요하지 않음")
        void shouldNotRequireAuthorizationForPublicEndpoint() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/public", HttpMethod.GET);

            // when & then
            assertThat(endpoint.requiresAuthorization()).isFalse();
        }

        @Test
        @DisplayName("권한이 필요한 엔드포인트는 인가가 필요함")
        void shouldRequireAuthorizationWhenPermissionsRequired() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "service",
                            "/protected",
                            HttpMethod.GET,
                            Set.of(Permission.of("user:read")),
                            Set.of(),
                            false);

            // when & then
            assertThat(endpoint.requiresAuthorization()).isTrue();
        }

        @Test
        @DisplayName("역할이 필요한 엔드포인트는 인가가 필요함")
        void shouldRequireAuthorizationWhenRolesRequired() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "service", "/admin", HttpMethod.GET, Set.of(), Set.of("ADMIN"), false);

            // when & then
            assertThat(endpoint.requiresAuthorization()).isTrue();
        }

        @Test
        @DisplayName("권한과 역할이 모두 없는 비공개 엔드포인트는 인가가 필요하지 않음")
        void shouldNotRequireAuthorizationWhenNoPermissionsOrRoles() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "service", "/internal", HttpMethod.GET, Set.of(), Set.of(), false);

            // when & then
            assertThat(endpoint.requiresAuthorization()).isFalse();
        }
    }

    @Nested
    @DisplayName("matchesPath() 테스트")
    class MatchesPathTest {

        @Test
        @DisplayName("정확한 경로 매칭")
        void shouldMatchExactPath() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/users", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/users")).isTrue();
        }

        @Test
        @DisplayName("다른 경로는 매칭되지 않음")
        void shouldNotMatchDifferentPath() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/users", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/orders")).isFalse();
        }

        @Test
        @DisplayName("Path Variable 패턴 매칭")
        void shouldMatchPathVariablePattern() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint(
                            "service", "/api/v1/users/{userId}", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/users/123")).isTrue();
            assertThat(endpoint.matchesPath("/api/v1/users/abc")).isTrue();
            assertThat(endpoint.matchesPath("/api/v1/users/user-123")).isTrue();
        }

        @Test
        @DisplayName("복수 Path Variable 패턴 매칭")
        void shouldMatchMultiplePathVariables() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint(
                            "service", "/api/v1/users/{userId}/orders/{orderId}", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/users/123/orders/456")).isTrue();
            assertThat(endpoint.matchesPath("/api/v1/users/abc/orders/def")).isTrue();
        }

        @Test
        @DisplayName("Path Variable 패턴이 부분적으로만 매칭되면 false")
        void shouldNotMatchPartialPathVariable() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint(
                            "service", "/api/v1/users/{userId}/orders", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/users")).isFalse();
            assertThat(endpoint.matchesPath("/api/v1/users/123")).isFalse();
            assertThat(endpoint.matchesPath("/api/v1/users/123/orders/456")).isFalse();
        }

        @Test
        @DisplayName("null 경로는 매칭되지 않음")
        void shouldNotMatchNullPath() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/users", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath(null)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 경로 처리")
        void shouldHandleEmptyPath() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint("service", "/api/v1/users", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("")).isFalse();
        }

        @Test
        @DisplayName("슬래시가 포함된 Path Variable 패턴은 매칭되지 않음")
        void shouldNotMatchPathVariableWithSlash() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.publicEndpoint(
                            "service", "/api/v1/users/{userId}", HttpMethod.GET);

            // when & then
            assertThat(endpoint.matchesPath("/api/v1/users/123/extra")).isFalse();
        }
    }
}
