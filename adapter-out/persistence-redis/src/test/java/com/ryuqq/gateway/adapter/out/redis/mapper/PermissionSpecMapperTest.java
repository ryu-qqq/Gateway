package com.ryuqq.gateway.adapter.out.redis.mapper;

import static org.assertj.core.api.Assertions.*;

import com.ryuqq.gateway.adapter.out.redis.entity.EndpointPermissionEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.PermissionSpecEntity;
import com.ryuqq.gateway.domain.authorization.vo.EndpointPermission;
import com.ryuqq.gateway.domain.authorization.vo.HttpMethod;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionSpec;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionSpecMapper 단위 테스트")
class PermissionSpecMapperTest {

    private PermissionSpecMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PermissionSpecMapper();
    }

    @Nested
    @DisplayName("Entity → Domain 변환")
    class EntityToDomain {

        @Test
        @DisplayName("정상적인 Entity를 Domain으로 변환")
        void convertEntityToDomain() {
            // given
            Long version = 1L;
            Instant updatedAt = Instant.now();

            EndpointPermissionEntity endpointEntity1 =
                    new EndpointPermissionEntity(
                            "order-service",
                            "/api/v1/orders",
                            "GET",
                            Set.of("order:read"),
                            Set.of("USER"),
                            false);

            EndpointPermissionEntity endpointEntity2 =
                    new EndpointPermissionEntity(
                            "product-service",
                            "/api/v1/products",
                            "POST",
                            Set.of("product:create"),
                            Set.of(),
                            false);

            PermissionSpecEntity entity =
                    new PermissionSpecEntity(
                            version, updatedAt, List.of(endpointEntity1, endpointEntity2));

            // when
            PermissionSpec domain = mapper.toPermissionSpec(entity);

            // then
            assertThat(domain.version()).isEqualTo(version);
            assertThat(domain.updatedAt()).isEqualTo(updatedAt);
            assertThat(domain.permissions()).hasSize(2);

            EndpointPermission firstPermission = domain.permissions().get(0);
            assertThat(firstPermission.serviceName()).isEqualTo("order-service");
            assertThat(firstPermission.path()).isEqualTo("/api/v1/orders");
            assertThat(firstPermission.method()).isEqualTo(HttpMethod.GET);
            assertThat(firstPermission.requiredPermissions())
                    .containsExactly(Permission.of("order:read"));
            assertThat(firstPermission.requiredRoles()).containsExactly("USER");
            assertThat(firstPermission.isPublic()).isFalse();
        }

        @Test
        @DisplayName("Public 엔드포인트 Entity를 Domain으로 변환")
        void convertPublicEndpointEntityToDomain() {
            // given
            EndpointPermissionEntity publicEndpointEntity =
                    new EndpointPermissionEntity(
                            "auth-service", "/api/v1/public", "GET", Set.of(), Set.of(), true);

            PermissionSpecEntity entity =
                    new PermissionSpecEntity(1L, Instant.now(), List.of(publicEndpointEntity));

            // when
            PermissionSpec domain = mapper.toPermissionSpec(entity);

            // then
            assertThat(domain.permissions()).hasSize(1);
            EndpointPermission permission = domain.permissions().get(0);
            assertThat(permission.isPublic()).isTrue();
            assertThat(permission.requiredPermissions()).isEmpty();
            assertThat(permission.requiredRoles()).isEmpty();
        }

        @Test
        @DisplayName("빈 permissions 리스트를 가진 Entity 변환")
        void convertEntityWithEmptyPermissions() {
            // given
            PermissionSpecEntity entity = new PermissionSpecEntity(1L, Instant.now(), List.of());

            // when
            PermissionSpec domain = mapper.toPermissionSpec(entity);

            // then
            assertThat(domain.permissions()).isEmpty();
        }

        @Test
        @DisplayName("여러 HTTP 메서드 변환")
        void convertVariousHttpMethods() {
            // given
            List<EndpointPermissionEntity> endpoints =
                    List.of(
                            createEndpointEntity("/api/v1/orders", "GET"),
                            createEndpointEntity("/api/v1/orders", "POST"),
                            createEndpointEntity("/api/v1/orders/{id}", "PUT"),
                            createEndpointEntity("/api/v1/orders/{id}", "PATCH"),
                            createEndpointEntity("/api/v1/orders/{id}", "DELETE"));

            PermissionSpecEntity entity = new PermissionSpecEntity(1L, Instant.now(), endpoints);

            // when
            PermissionSpec domain = mapper.toPermissionSpec(entity);

            // then
            assertThat(domain.permissions()).hasSize(5);
            assertThat(domain.permissions().get(0).method()).isEqualTo(HttpMethod.GET);
            assertThat(domain.permissions().get(1).method()).isEqualTo(HttpMethod.POST);
            assertThat(domain.permissions().get(2).method()).isEqualTo(HttpMethod.PUT);
            assertThat(domain.permissions().get(3).method()).isEqualTo(HttpMethod.PATCH);
            assertThat(domain.permissions().get(4).method()).isEqualTo(HttpMethod.DELETE);
        }

        @Test
        @DisplayName("여러 권한을 가진 엔드포인트 변환")
        void convertEndpointWithMultiplePermissions() {
            // given
            EndpointPermissionEntity endpointEntity =
                    new EndpointPermissionEntity(
                            "order-service",
                            "/api/v1/orders",
                            "POST",
                            Set.of("order:read", "order:create", "order:update"),
                            Set.of("ADMIN", "MANAGER"),
                            false);

            PermissionSpecEntity entity =
                    new PermissionSpecEntity(1L, Instant.now(), List.of(endpointEntity));

            // when
            PermissionSpec domain = mapper.toPermissionSpec(entity);

            // then
            EndpointPermission permission = domain.permissions().get(0);
            assertThat(permission.requiredPermissions()).hasSize(3);
            assertThat(permission.requiredPermissions())
                    .containsExactlyInAnyOrder(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("order:update"));
            assertThat(permission.requiredRoles()).containsExactlyInAnyOrder("ADMIN", "MANAGER");
        }
    }

    @Nested
    @DisplayName("Domain → Entity 변환")
    class DomainToEntity {

        @Test
        @DisplayName("정상적인 Domain을 Entity로 변환")
        void convertDomainToEntity() {
            // given
            Long version = 2L;
            Instant updatedAt = Instant.now();

            EndpointPermission endpoint1 =
                    EndpointPermission.of(
                            "order-service",
                            "/api/v1/orders",
                            HttpMethod.GET,
                            Set.of(Permission.of("order:read")),
                            Set.of("USER"),
                            false);

            EndpointPermission endpoint2 =
                    EndpointPermission.of(
                            "product-service",
                            "/api/v1/products",
                            HttpMethod.POST,
                            Set.of(Permission.of("product:create")),
                            Set.of(),
                            false);

            PermissionSpec domain =
                    PermissionSpec.of(version, updatedAt, List.of(endpoint1, endpoint2));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getVersion()).isEqualTo(version);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(entity.getPermissions()).hasSize(2);

            EndpointPermissionEntity firstEntity = entity.getPermissions().get(0);
            assertThat(firstEntity.getServiceName()).isEqualTo("order-service");
            assertThat(firstEntity.getPath()).isEqualTo("/api/v1/orders");
            assertThat(firstEntity.getMethod()).isEqualTo("GET");
            assertThat(firstEntity.getRequiredPermissions()).containsExactly("order:read");
            assertThat(firstEntity.getRequiredRoles()).containsExactly("USER");
            assertThat(firstEntity.isPublic()).isFalse();
        }

        @Test
        @DisplayName("Public 엔드포인트 Domain을 Entity로 변환")
        void convertPublicEndpointDomainToEntity() {
            // given
            EndpointPermission publicEndpoint =
                    EndpointPermission.publicEndpoint(
                            "auth-service", "/api/v1/public", HttpMethod.GET);

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of(publicEndpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).hasSize(1);
            EndpointPermissionEntity endpointEntity = entity.getPermissions().get(0);
            assertThat(endpointEntity.isPublic()).isTrue();
            assertThat(endpointEntity.getRequiredPermissions()).isEmpty();
            assertThat(endpointEntity.getRequiredRoles()).isEmpty();
        }

        @Test
        @DisplayName("빈 permissions 리스트를 가진 Domain 변환")
        void convertDomainWithEmptyPermissions() {
            // given
            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of());

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("여러 HTTP 메서드를 문자열로 변환")
        void convertHttpMethodsToStrings() {
            // given
            List<EndpointPermission> endpoints =
                    List.of(
                            createEndpointPermission("/api/v1/orders", HttpMethod.GET),
                            createEndpointPermission("/api/v1/orders", HttpMethod.POST),
                            createEndpointPermission("/api/v1/orders/{id}", HttpMethod.PUT),
                            createEndpointPermission("/api/v1/orders/{id}", HttpMethod.PATCH),
                            createEndpointPermission("/api/v1/orders/{id}", HttpMethod.DELETE));

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), endpoints);

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).hasSize(5);
            assertThat(entity.getPermissions().get(0).getMethod()).isEqualTo("GET");
            assertThat(entity.getPermissions().get(1).getMethod()).isEqualTo("POST");
            assertThat(entity.getPermissions().get(2).getMethod()).isEqualTo("PUT");
            assertThat(entity.getPermissions().get(3).getMethod()).isEqualTo("PATCH");
            assertThat(entity.getPermissions().get(4).getMethod()).isEqualTo("DELETE");
        }

        @Test
        @DisplayName("Permission 객체를 문자열로 변환")
        void convertPermissionObjectsToStrings() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "order-service",
                            "/api/v1/orders",
                            HttpMethod.POST,
                            Set.of(
                                    Permission.of("order:read"),
                                    Permission.of("order:create"),
                                    Permission.of("order:update")),
                            Set.of("ADMIN", "MANAGER"),
                            false);

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of(endpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);

            // then
            EndpointPermissionEntity endpointEntity = entity.getPermissions().get(0);
            assertThat(endpointEntity.getRequiredPermissions()).hasSize(3);
            assertThat(endpointEntity.getRequiredPermissions())
                    .containsExactlyInAnyOrder("order:read", "order:create", "order:update");
            assertThat(endpointEntity.getRequiredRoles())
                    .containsExactlyInAnyOrder("ADMIN", "MANAGER");
        }
    }

    @Nested
    @DisplayName("양방향 변환 일관성")
    class BidirectionalConsistency {

        @Test
        @DisplayName("Domain → Entity → Domain 변환 일관성")
        void domainToEntityToDomainConsistency() {
            // given
            Long version = 3L;
            Instant updatedAt = Instant.now();

            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "order-service",
                            "/api/v1/orders/{orderId}",
                            HttpMethod.GET,
                            Set.of(Permission.of("order:read")),
                            Set.of("USER", "ADMIN"),
                            false);

            PermissionSpec originalDomain =
                    PermissionSpec.of(version, updatedAt, List.of(endpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(originalDomain);
            PermissionSpec convertedDomain = mapper.toPermissionSpec(entity);

            // then
            assertThat(convertedDomain.version()).isEqualTo(originalDomain.version());
            assertThat(convertedDomain.updatedAt()).isEqualTo(originalDomain.updatedAt());
            assertThat(convertedDomain.permissions()).hasSize(originalDomain.permissions().size());

            EndpointPermission originalEndpoint = originalDomain.permissions().get(0);
            EndpointPermission convertedEndpoint = convertedDomain.permissions().get(0);

            assertThat(convertedEndpoint.serviceName()).isEqualTo(originalEndpoint.serviceName());
            assertThat(convertedEndpoint.path()).isEqualTo(originalEndpoint.path());
            assertThat(convertedEndpoint.method()).isEqualTo(originalEndpoint.method());
            assertThat(convertedEndpoint.requiredPermissions())
                    .isEqualTo(originalEndpoint.requiredPermissions());
            assertThat(convertedEndpoint.requiredRoles())
                    .isEqualTo(originalEndpoint.requiredRoles());
            assertThat(convertedEndpoint.isPublic()).isEqualTo(originalEndpoint.isPublic());
        }

        @Test
        @DisplayName("Entity → Domain → Entity 변환 일관성")
        void entityToDomainToEntityConsistency() {
            // given
            Long version = 4L;
            Instant updatedAt = Instant.now();

            EndpointPermissionEntity endpointEntity =
                    new EndpointPermissionEntity(
                            "product-service",
                            "/api/v1/products/{productId}",
                            "PUT",
                            Set.of("product:update"),
                            Set.of("ADMIN"),
                            false);

            PermissionSpecEntity originalEntity =
                    new PermissionSpecEntity(version, updatedAt, List.of(endpointEntity));

            // when
            PermissionSpec domain = mapper.toPermissionSpec(originalEntity);
            PermissionSpecEntity convertedEntity = mapper.toEntity(domain);

            // then
            assertThat(convertedEntity.getVersion()).isEqualTo(originalEntity.getVersion());
            assertThat(convertedEntity.getUpdatedAt()).isEqualTo(originalEntity.getUpdatedAt());
            assertThat(convertedEntity.getPermissions())
                    .hasSize(originalEntity.getPermissions().size());

            EndpointPermissionEntity originalEndpoint = originalEntity.getPermissions().get(0);
            EndpointPermissionEntity convertedEndpoint = convertedEntity.getPermissions().get(0);

            assertThat(convertedEndpoint.getServiceName())
                    .isEqualTo(originalEndpoint.getServiceName());
            assertThat(convertedEndpoint.getPath()).isEqualTo(originalEndpoint.getPath());
            assertThat(convertedEndpoint.getMethod()).isEqualTo(originalEndpoint.getMethod());
            assertThat(convertedEndpoint.getRequiredPermissions())
                    .isEqualTo(originalEndpoint.getRequiredPermissions());
            assertThat(convertedEndpoint.getRequiredRoles())
                    .isEqualTo(originalEndpoint.getRequiredRoles());
            assertThat(convertedEndpoint.isPublic()).isEqualTo(originalEndpoint.isPublic());
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("Path Variable이 포함된 경로 변환")
        void convertPathWithPathVariables() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "order-service",
                            "/api/v1/users/{userId}/orders/{orderId}",
                            HttpMethod.GET,
                            Set.of(Permission.of("order:read")),
                            Set.of(),
                            false);

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of(endpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);
            PermissionSpec convertedDomain = mapper.toPermissionSpec(entity);

            // then
            assertThat(convertedDomain.permissions().get(0).path())
                    .isEqualTo("/api/v1/users/{userId}/orders/{orderId}");
        }

        @Test
        @DisplayName("와일드카드 권한 변환")
        void convertWildcardPermissions() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "admin-service",
                            "/api/v1/admin",
                            HttpMethod.GET,
                            Set.of(Permission.of("admin:*")),
                            Set.of(),
                            false);

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of(endpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);
            PermissionSpec convertedDomain = mapper.toPermissionSpec(entity);

            // then
            assertThat(convertedDomain.permissions().get(0).requiredPermissions())
                    .containsExactly(Permission.of("admin:*"));
        }

        @Test
        @DisplayName("특수 문자가 포함된 서비스명 변환")
        void convertServiceNameWithSpecialCharacters() {
            // given
            EndpointPermission endpoint =
                    EndpointPermission.of(
                            "order-management-service",
                            "/api/v1/orders",
                            HttpMethod.GET,
                            Set.of(Permission.of("order:read")),
                            Set.of(),
                            false);

            PermissionSpec domain = PermissionSpec.of(1L, Instant.now(), List.of(endpoint));

            // when
            PermissionSpecEntity entity = mapper.toEntity(domain);
            PermissionSpec convertedDomain = mapper.toPermissionSpec(entity);

            // then
            assertThat(convertedDomain.permissions().get(0).serviceName())
                    .isEqualTo("order-management-service");
        }
    }

    // Helper methods
    private EndpointPermissionEntity createEndpointEntity(String path, String method) {
        return new EndpointPermissionEntity(
                "test-service", path, method, Set.of("test:read"), Set.of(), false);
    }

    private EndpointPermission createEndpointPermission(String path, HttpMethod method) {
        return EndpointPermission.of(
                "test-service", path, method, Set.of(Permission.of("test:read")), Set.of(), false);
    }
}
