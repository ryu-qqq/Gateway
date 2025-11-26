package com.ryuqq.gateway.domain.authorization.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionHash 단위 테스트")
class PermissionHashTest {

    private static final String VALID_HASH = "abc123def456";
    private static final Instant NOW = Instant.now();

    @Nested
    @DisplayName("생성 및 팩토리 메서드")
    class Creation {

        @Test
        @DisplayName("유효한 값으로 PermissionHash 생성 성공")
        void createPermissionHashWithValidValues() {
            // given
            Set<Permission> permissions =
                    Set.of(Permission.of("order:read"), Permission.of("order:create"));
            Set<String> roles = Set.of("ADMIN", "USER");

            // when
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, permissions, roles, NOW);

            // then
            assertThat(permissionHash.hash()).isEqualTo(VALID_HASH);
            assertThat(permissionHash.permissions())
                    .containsExactlyInAnyOrderElementsOf(permissions);
            assertThat(permissionHash.roles()).containsExactlyInAnyOrderElementsOf(roles);
            assertThat(permissionHash.generatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("문자열 집합으로 PermissionHash 생성 성공")
        void createPermissionHashFromStrings() {
            // given
            Set<String> permissionStrings = Set.of("order:read", "order:create");
            Set<String> roles = Set.of("ADMIN");

            // when
            PermissionHash permissionHash =
                    PermissionHash.fromStrings(VALID_HASH, permissionStrings, roles, NOW);

            // then
            assertThat(permissionHash.hash()).isEqualTo(VALID_HASH);
            assertThat(permissionHash.permissionStrings())
                    .containsExactlyInAnyOrderElementsOf(permissionStrings);
            assertThat(permissionHash.roles()).containsExactlyInAnyOrder("ADMIN");
        }

        @Test
        @DisplayName("null hash로 생성 시 예외 발생")
        void throwExceptionWhenHashIsNull() {
            // when & then
            assertThatThrownBy(() -> PermissionHash.of(null, Set.of(), Set.of(), NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Hash cannot be null");
        }

        @Test
        @DisplayName("빈 hash로 생성 시 예외 발생")
        void throwExceptionWhenHashIsBlank() {
            // when & then
            assertThatThrownBy(() -> PermissionHash.of("", Set.of(), Set.of(), NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hash cannot be blank");

            assertThatThrownBy(() -> PermissionHash.of("   ", Set.of(), Set.of(), NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hash cannot be blank");
        }

        @Test
        @DisplayName("null permissions는 빈 Set으로 초기화")
        void initializeNullPermissionsAsEmptySet() {
            // when
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, null, Set.of(), NOW);

            // then
            assertThat(permissionHash.permissions()).isEmpty();
        }

        @Test
        @DisplayName("null roles는 빈 Set으로 초기화")
        void initializeNullRolesAsEmptySet() {
            // when
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), null, NOW);

            // then
            assertThat(permissionHash.roles()).isEmpty();
        }

        @Test
        @DisplayName("null generatedAt은 현재 시각으로 초기화")
        void initializeNullGeneratedAtAsNow() {
            // given
            Instant before = Instant.now();

            // when
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), null);

            // then
            Instant after = Instant.now();
            assertThat(permissionHash.generatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("불변성 보장 - permissions는 복사본")
        void ensureImmutabilityOfPermissions() {
            // given - mutable Set 사용하여 불변성 검증
            Set<Permission> originalPermissions = new java.util.HashSet<>();
            originalPermissions.add(Permission.of("order:read"));

            // when
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, originalPermissions, Set.of(), NOW);

            // then - 원본 수정해도 내부 상태에 영향 없음
            originalPermissions.add(Permission.of("order:write"));
            assertThat(permissionHash.permissions()).hasSize(1);
            assertThat(permissionHash.permissions()).containsExactly(Permission.of("order:read"));
        }

        @Test
        @DisplayName("불변성 보장 - roles는 복사본")
        void ensureImmutabilityOfRoles() {
            // given - mutable Set 사용하여 불변성 검증
            Set<String> originalRoles = new java.util.HashSet<>();
            originalRoles.add("ADMIN");

            // when
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), originalRoles, NOW);

            // then - 원본 수정해도 내부 상태에 영향 없음
            originalRoles.add("USER");
            assertThat(permissionHash.roles()).hasSize(1);
            assertThat(permissionHash.roles()).containsExactly("ADMIN");
        }
    }

    @Nested
    @DisplayName("해시 매칭")
    class HashMatching {

        @Test
        @DisplayName("동일한 해시는 매칭됨")
        void matchesIdenticalHash() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.matchesHash(VALID_HASH)).isTrue();
        }

        @Test
        @DisplayName("다른 해시는 매칭되지 않음")
        void doesNotMatchDifferentHash() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.matchesHash("different-hash")).isFalse();
        }

        @Test
        @DisplayName("대소문자가 다른 해시는 매칭되지 않음")
        void doesNotMatchCaseDifferentHash() {
            // given
            PermissionHash permissionHash = PermissionHash.of("abc123", Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.matchesHash("ABC123")).isFalse();
        }
    }

    @Nested
    @DisplayName("권한 보유 여부 확인")
    class PermissionCheck {

        @Test
        @DisplayName("정확히 일치하는 권한 보유")
        void hasExactPermission() {
            // given
            Permission orderRead = Permission.of("order:read");
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(orderRead), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasPermission(orderRead)).isTrue();
        }

        @Test
        @DisplayName("와일드카드 권한으로 특정 권한 포함")
        void hasPermissionViaWildcard() {
            // given
            Permission orderWildcard = Permission.of("order:*");
            Permission orderRead = Permission.of("order:read");
            Permission orderCreate = Permission.of("order:create");

            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(orderWildcard), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasPermission(orderRead)).isTrue();
            assertThat(permissionHash.hasPermission(orderCreate)).isTrue();
        }

        @Test
        @DisplayName("보유하지 않은 권한은 false 반환")
        void doesNotHavePermission() {
            // given
            Permission orderRead = Permission.of("order:read");
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(orderRead), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasPermission(Permission.of("order:create"))).isFalse();
            assertThat(permissionHash.hasPermission(Permission.of("product:read"))).isFalse();
        }

        @Test
        @DisplayName("빈 권한 집합은 어떤 권한도 보유하지 않음")
        void emptyPermissionsDoesNotHaveAnyPermission() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasPermission(Permission.of("order:read"))).isFalse();
        }
    }

    @Nested
    @DisplayName("모든 권한 보유 여부 확인")
    class AllPermissionsCheck {

        @Test
        @DisplayName("모든 필수 권한을 보유한 경우 true 반환")
        void hasAllRequiredPermissions() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(
                            VALID_HASH,
                            Set.of(
                                    Permission.of("order:read"),
                                    Permission.of("order:create"),
                                    Permission.of("product:read")),
                            Set.of(),
                            NOW);

            Set<Permission> required =
                    Set.of(Permission.of("order:read"), Permission.of("product:read"));

            // when & then
            assertThat(permissionHash.hasAllPermissions(required)).isTrue();
        }

        @Test
        @DisplayName("와일드카드로 모든 필수 권한 충족")
        void hasAllPermissionsViaWildcard() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(Permission.of("order:*")), Set.of(), NOW);

            Set<Permission> required =
                    Set.of(
                            Permission.of("order:read"),
                            Permission.of("order:create"),
                            Permission.of("order:delete"));

            // when & then
            assertThat(permissionHash.hasAllPermissions(required)).isTrue();
        }

        @Test
        @DisplayName("일부 권한만 보유한 경우 false 반환")
        void doesNotHaveAllPermissions() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(
                            VALID_HASH, Set.of(Permission.of("order:read")), Set.of(), NOW);

            Set<Permission> required =
                    Set.of(Permission.of("order:read"), Permission.of("order:create"));

            // when & then
            assertThat(permissionHash.hasAllPermissions(required)).isFalse();
        }

        @Test
        @DisplayName("빈 필수 권한 집합은 항상 true 반환")
        void emptyRequiredPermissionsAlwaysTrue() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasAllPermissions(Set.of())).isTrue();
        }
    }

    @Nested
    @DisplayName("역할 보유 여부 확인")
    class RoleCheck {

        @Test
        @DisplayName("보유한 역할은 true 반환")
        void hasRole() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("ADMIN", "USER"), NOW);

            // when & then
            assertThat(permissionHash.hasRole("ADMIN")).isTrue();
            assertThat(permissionHash.hasRole("USER")).isTrue();
        }

        @Test
        @DisplayName("보유하지 않은 역할은 false 반환")
        void doesNotHaveRole() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("USER"), NOW);

            // when & then
            assertThat(permissionHash.hasRole("ADMIN")).isFalse();
        }

        @Test
        @DisplayName("빈 역할 집합은 어떤 역할도 보유하지 않음")
        void emptyRolesDoesNotHaveAnyRole() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when & then
            assertThat(permissionHash.hasRole("ADMIN")).isFalse();
        }
    }

    @Nested
    @DisplayName("역할 중 하나라도 보유 여부 확인")
    class AnyRoleCheck {

        @Test
        @DisplayName("필수 역할 중 하나라도 보유한 경우 true 반환")
        void hasAnyRequiredRole() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("USER", "MANAGER"), NOW);

            Set<String> requiredRoles = Set.of("ADMIN", "MANAGER");

            // when & then
            assertThat(permissionHash.hasAnyRole(requiredRoles)).isTrue();
        }

        @Test
        @DisplayName("필수 역할을 하나도 보유하지 않은 경우 false 반환")
        void doesNotHaveAnyRequiredRole() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("USER"), NOW);

            Set<String> requiredRoles = Set.of("ADMIN", "MANAGER");

            // when & then
            assertThat(permissionHash.hasAnyRole(requiredRoles)).isFalse();
        }

        @Test
        @DisplayName("빈 필수 역할 집합은 항상 false 반환")
        void emptyRequiredRolesAlwaysFalse() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("ADMIN"), NOW);

            // when & then
            assertThat(permissionHash.hasAnyRole(Set.of())).isFalse();
        }

        @Test
        @DisplayName("모든 필수 역할을 보유한 경우에도 true 반환")
        void hasAllRequiredRoles() {
            // given
            PermissionHash permissionHash =
                    PermissionHash.of(VALID_HASH, Set.of(), Set.of("ADMIN", "USER"), NOW);

            Set<String> requiredRoles = Set.of("ADMIN", "USER");

            // when & then
            assertThat(permissionHash.hasAnyRole(requiredRoles)).isTrue();
        }
    }

    @Nested
    @DisplayName("권한 문자열 집합 반환")
    class PermissionStrings {

        @Test
        @DisplayName("권한 문자열 집합 반환")
        void returnPermissionStrings() {
            // given
            Set<String> expectedStrings = Set.of("order:read", "order:create", "product:read");
            PermissionHash permissionHash =
                    PermissionHash.fromStrings(VALID_HASH, expectedStrings, Set.of(), NOW);

            // when
            Set<String> actualStrings = permissionHash.permissionStrings();

            // then
            assertThat(actualStrings).containsExactlyInAnyOrderElementsOf(expectedStrings);
        }

        @Test
        @DisplayName("빈 권한 집합은 빈 문자열 집합 반환")
        void returnEmptyStringsForEmptyPermissions() {
            // given
            PermissionHash permissionHash = PermissionHash.of(VALID_HASH, Set.of(), Set.of(), NOW);

            // when
            Set<String> permissionStrings = permissionHash.permissionStrings();

            // then
            assertThat(permissionStrings).isEmpty();
        }
    }
}
