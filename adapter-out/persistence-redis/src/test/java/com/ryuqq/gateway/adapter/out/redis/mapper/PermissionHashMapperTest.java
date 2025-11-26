package com.ryuqq.gateway.adapter.out.redis.mapper;

import static org.assertj.core.api.Assertions.*;

import com.ryuqq.gateway.adapter.out.redis.entity.PermissionHashEntity;
import com.ryuqq.gateway.domain.authorization.vo.Permission;
import com.ryuqq.gateway.domain.authorization.vo.PermissionHash;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionHashMapper 단위 테스트")
class PermissionHashMapperTest {

    private PermissionHashMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PermissionHashMapper();
    }

    @Nested
    @DisplayName("Entity → Domain 변환")
    class EntityToDomain {

        @Test
        @DisplayName("정상적인 Entity를 Domain으로 변환")
        void convertEntityToDomain() {
            // given
            String hash = "hash-abc123";
            Set<String> permissions = Set.of("order:read", "order:create", "product:read");
            Set<String> roles = Set.of("ADMIN", "USER");
            Instant generatedAt = Instant.now();

            PermissionHashEntity entity =
                    new PermissionHashEntity(hash, permissions, roles, generatedAt);

            // when
            PermissionHash domain = mapper.toPermissionHash(entity);

            // then
            assertThat(domain.hash()).isEqualTo(hash);
            assertThat(domain.permissions()).hasSize(3);
            assertThat(domain.permissions())
                    .containsExactlyInAnyOrder(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("product:read"));
            assertThat(domain.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
            assertThat(domain.generatedAt()).isEqualTo(generatedAt);
        }

        @Test
        @DisplayName("빈 permissions와 roles를 가진 Entity 변환")
        void convertEntityWithEmptyPermissionsAndRoles() {
            // given
            PermissionHashEntity entity =
                    new PermissionHashEntity("hash-empty", Set.of(), Set.of(), Instant.now());

            // when
            PermissionHash domain = mapper.toPermissionHash(entity);

            // then
            assertThat(domain.permissions()).isEmpty();
            assertThat(domain.roles()).isEmpty();
        }

        @Test
        @DisplayName("와일드카드 권한을 포함한 Entity 변환")
        void convertEntityWithWildcardPermissions() {
            // given
            Set<String> permissions = Set.of("order:*", "product:read");
            PermissionHashEntity entity =
                    new PermissionHashEntity("hash-wildcard", permissions, Set.of(), Instant.now());

            // when
            PermissionHash domain = mapper.toPermissionHash(entity);

            // then
            assertThat(domain.permissions()).hasSize(2);
            assertThat(domain.permissions())
                    .containsExactlyInAnyOrder(
                            Permission.of("order:*"), Permission.of("product:read"));
        }

        @Test
        @DisplayName("단일 권한과 역할을 가진 Entity 변환")
        void convertEntityWithSinglePermissionAndRole() {
            // given
            PermissionHashEntity entity =
                    new PermissionHashEntity(
                            "hash-single", Set.of("order:read"), Set.of("USER"), Instant.now());

            // when
            PermissionHash domain = mapper.toPermissionHash(entity);

            // then
            assertThat(domain.permissions()).hasSize(1);
            assertThat(domain.permissions()).containsExactly(Permission.of("order:read"));
            assertThat(domain.roles()).hasSize(1);
            assertThat(domain.roles()).containsExactly("USER");
        }

        @Test
        @DisplayName("여러 역할을 가진 Entity 변환")
        void convertEntityWithMultipleRoles() {
            // given
            Set<String> roles = Set.of("ADMIN", "MANAGER", "USER", "GUEST");
            PermissionHashEntity entity =
                    new PermissionHashEntity(
                            "hash-roles", Set.of("order:read"), roles, Instant.now());

            // when
            PermissionHash domain = mapper.toPermissionHash(entity);

            // then
            assertThat(domain.roles()).hasSize(4);
            assertThat(domain.roles()).containsExactlyInAnyOrderElementsOf(roles);
        }
    }

    @Nested
    @DisplayName("Domain → Entity 변환")
    class DomainToEntity {

        @Test
        @DisplayName("정상적인 Domain을 Entity로 변환")
        void convertDomainToEntity() {
            // given
            String hash = "hash-xyz789";
            Set<Permission> permissions =
                    Set.of(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("product:read"));
            Set<String> roles = Set.of("ADMIN", "USER");
            Instant generatedAt = Instant.now();

            PermissionHash domain = PermissionHash.of(hash, permissions, roles, generatedAt);

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getHash()).isEqualTo(hash);
            assertThat(entity.getPermissions()).hasSize(3);
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrder("order:read", "order:create", "product:read");
            assertThat(entity.getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
            assertThat(entity.getGeneratedAt()).isEqualTo(generatedAt);
        }

        @Test
        @DisplayName("빈 permissions와 roles를 가진 Domain 변환")
        void convertDomainWithEmptyPermissionsAndRoles() {
            // given
            PermissionHash domain =
                    PermissionHash.of("hash-empty", Set.of(), Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).isEmpty();
            assertThat(entity.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("와일드카드 권한을 포함한 Domain 변환")
        void convertDomainWithWildcardPermissions() {
            // given
            Set<Permission> permissions =
                    Set.of(Permission.of("order:*"), Permission.of("product:read"));
            PermissionHash domain =
                    PermissionHash.of("hash-wildcard", permissions, Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).hasSize(2);
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrder("order:*", "product:read");
        }

        @Test
        @DisplayName("Permission 객체를 문자열로 변환")
        void convertPermissionObjectsToStrings() {
            // given
            Set<Permission> permissions =
                    Set.of(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("order:update"),
                            Permission.of("order:delete"));
            PermissionHash domain =
                    PermissionHash.of("hash-permissions", permissions, Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions()).hasSize(4);
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrder(
                            "order:read", "order:create", "order:update", "order:delete");
        }

        @Test
        @DisplayName("fromStrings 팩토리 메서드로 생성한 Domain 변환")
        void convertDomainCreatedFromStrings() {
            // given
            Set<String> permissionStrings = Set.of("order:read", "product:create");
            Set<String> roles = Set.of("USER");
            Instant generatedAt = Instant.now();

            PermissionHash domain =
                    PermissionHash.fromStrings(
                            "hash-from-strings", permissionStrings, roles, generatedAt);

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);

            // then
            assertThat(entity.getPermissions())
                    .containsExactlyInAnyOrderElementsOf(permissionStrings);
            assertThat(entity.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
        }
    }

    @Nested
    @DisplayName("양방향 변환 일관성")
    class BidirectionalConsistency {

        @Test
        @DisplayName("Domain → Entity → Domain 변환 일관성")
        void domainToEntityToDomainConsistency() {
            // given
            String hash = "hash-consistency-1";
            Set<Permission> permissions =
                    Set.of(Permission.of("order:read"), Permission.of("product:create"));
            Set<String> roles = Set.of("ADMIN", "USER");
            Instant generatedAt = Instant.now();

            PermissionHash originalDomain =
                    PermissionHash.of(hash, permissions, roles, generatedAt);

            // when
            PermissionHashEntity entity = mapper.toEntity(originalDomain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.hash()).isEqualTo(originalDomain.hash());
            assertThat(convertedDomain.permissions()).isEqualTo(originalDomain.permissions());
            assertThat(convertedDomain.roles()).isEqualTo(originalDomain.roles());
            assertThat(convertedDomain.generatedAt()).isEqualTo(originalDomain.generatedAt());
        }

        @Test
        @DisplayName("Entity → Domain → Entity 변환 일관성")
        void entityToDomainToEntityConsistency() {
            // given
            String hash = "hash-consistency-2";
            Set<String> permissions = Set.of("order:read", "product:create");
            Set<String> roles = Set.of("ADMIN", "USER");
            Instant generatedAt = Instant.now();

            PermissionHashEntity originalEntity =
                    new PermissionHashEntity(hash, permissions, roles, generatedAt);

            // when
            PermissionHash domain = mapper.toPermissionHash(originalEntity);
            PermissionHashEntity convertedEntity = mapper.toEntity(domain);

            // then
            assertThat(convertedEntity.getHash()).isEqualTo(originalEntity.getHash());
            assertThat(convertedEntity.getPermissions()).isEqualTo(originalEntity.getPermissions());
            assertThat(convertedEntity.getRoles()).isEqualTo(originalEntity.getRoles());
            assertThat(convertedEntity.getGeneratedAt()).isEqualTo(originalEntity.getGeneratedAt());
        }

        @Test
        @DisplayName("빈 데이터에 대한 양방향 변환 일관성")
        void emptyDataBidirectionalConsistency() {
            // given
            PermissionHash originalDomain =
                    PermissionHash.of("hash-empty", Set.of(), Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(originalDomain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.hash()).isEqualTo(originalDomain.hash());
            assertThat(convertedDomain.permissions()).isEmpty();
            assertThat(convertedDomain.roles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("매우 긴 해시 값 변환")
        void convertVeryLongHash() {
            // given
            String longHash = "a".repeat(256);
            PermissionHash domain =
                    PermissionHash.of(
                            longHash, Set.of(Permission.of("order:read")), Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.hash()).isEqualTo(longHash);
        }

        @Test
        @DisplayName("많은 권한을 가진 Domain 변환")
        void convertDomainWithManyPermissions() {
            // given
            Set<Permission> manyPermissions =
                    Set.of(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("order:update"),
                            Permission.of("order:delete"),
                            Permission.of("product:read"),
                            Permission.of("product:create"),
                            Permission.of("product:update"),
                            Permission.of("product:delete"),
                            Permission.of("user:read"),
                            Permission.of("user:create"));

            PermissionHash domain =
                    PermissionHash.of("hash-many", manyPermissions, Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.permissions()).hasSize(10);
            assertThat(convertedDomain.permissions()).isEqualTo(manyPermissions);
        }

        @Test
        @DisplayName("하이픈이 포함된 권한 변환")
        void convertPermissionsWithHyphens() {
            // given
            Set<Permission> permissions =
                    Set.of(
                            Permission.of("order-management:read"),
                            Permission.of("product-catalog:create"));

            PermissionHash domain =
                    PermissionHash.of("hash-hyphen", permissions, Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.permissions())
                    .containsExactlyInAnyOrder(
                            Permission.of("order-management:read"),
                            Permission.of("product-catalog:create"));
        }

        @Test
        @DisplayName("특수 문자가 포함된 역할명 변환")
        void convertRolesWithSpecialCharacters() {
            // given
            Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER");
            PermissionHash domain =
                    PermissionHash.of(
                            "hash-special",
                            Set.of(Permission.of("order:read")),
                            roles,
                            Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.roles()).containsExactlyInAnyOrderElementsOf(roles);
        }

        @Test
        @DisplayName("권한만 있고 역할이 없는 경우")
        void convertWithPermissionsOnly() {
            // given
            Set<Permission> permissions =
                    Set.of(Permission.of("order:read"), Permission.of("product:read"));
            PermissionHash domain =
                    PermissionHash.of(
                            "hash-permissions-only", permissions, Set.of(), Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.permissions()).hasSize(2);
            assertThat(convertedDomain.roles()).isEmpty();
        }

        @Test
        @DisplayName("역할만 있고 권한이 없는 경우")
        void convertWithRolesOnly() {
            // given
            Set<String> roles = Set.of("ADMIN", "USER");
            PermissionHash domain =
                    PermissionHash.of("hash-roles-only", Set.of(), roles, Instant.now());

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.permissions()).isEmpty();
            assertThat(convertedDomain.roles()).hasSize(2);
        }

        @Test
        @DisplayName("과거 시간의 generatedAt 변환")
        void convertWithPastGeneratedAt() {
            // given
            Instant pastTime = Instant.parse("2024-01-01T00:00:00Z");
            PermissionHash domain =
                    PermissionHash.of(
                            "hash-past", Set.of(Permission.of("order:read")), Set.of(), pastTime);

            // when
            PermissionHashEntity entity = mapper.toEntity(domain);
            PermissionHash convertedDomain = mapper.toPermissionHash(entity);

            // then
            assertThat(convertedDomain.generatedAt()).isEqualTo(pastTime);
        }
    }
}
