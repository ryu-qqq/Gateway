package com.ryuqq.gateway.application.authorization.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ValidatePermissionCommand 테스트")
class ValidatePermissionCommandTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 ValidatePermissionCommand 생성")
        void shouldCreateValidatePermissionCommand() {
            // given
            String userId = "user123";
            String tenantId = "tenant456";
            String permissionHash = "hash789";
            Set<String> roles = Set.of("USER", "ADMIN");
            String requestPath = "/api/v1/users";
            String requestMethod = "GET";

            // when
            ValidatePermissionCommand command =
                    new ValidatePermissionCommand(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.userId()).isEqualTo(userId);
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.permissionHash()).isEqualTo(permissionHash);
            assertThat(command.roles()).isEqualTo(roles);
            assertThat(command.requestPath()).isEqualTo(requestPath);
            assertThat(command.requestMethod()).isEqualTo(requestMethod);
        }

        @Test
        @DisplayName("null 역할 Set은 빈 Set으로 변환됨")
        void shouldConvertNullRolesToEmptySet() {
            // given
            String userId = "user123";
            String tenantId = "tenant456";
            String permissionHash = "hash789";
            Set<String> roles = null;
            String requestPath = "/api/v1/users";
            String requestMethod = "GET";

            // when
            ValidatePermissionCommand command =
                    new ValidatePermissionCommand(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.roles()).isEmpty();
        }

        @Test
        @DisplayName("빈 역할 Set으로 생성")
        void shouldCreateWithEmptyRoles() {
            // given
            String userId = "user123";
            String tenantId = "tenant456";
            String permissionHash = "hash789";
            Set<String> roles = Set.of();
            String requestPath = "/api/v1/users";
            String requestMethod = "GET";

            // when
            ValidatePermissionCommand command =
                    new ValidatePermissionCommand(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.roles()).isEmpty();
        }

        @Test
        @DisplayName("null permissionHash로 생성")
        void shouldCreateWithNullPermissionHash() {
            // given
            String userId = "user123";
            String tenantId = "tenant456";
            String permissionHash = null;
            Set<String> roles = Set.of("USER");
            String requestPath = "/api/v1/users";
            String requestMethod = "GET";

            // when
            ValidatePermissionCommand command =
                    new ValidatePermissionCommand(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.permissionHash()).isNull();
        }
    }

    @Nested
    @DisplayName("of() 정적 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("정적 팩토리 메서드로 생성")
        void shouldCreateUsingStaticFactoryMethod() {
            // given
            String userId = "user999";
            String tenantId = "tenant888";
            String permissionHash = "hash777";
            Set<String> roles = Set.of("MANAGER");
            String requestPath = "/api/v1/orders";
            String requestMethod = "POST";

            // when
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.userId()).isEqualTo(userId);
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.permissionHash()).isEqualTo(permissionHash);
            assertThat(command.roles()).isEqualTo(roles);
            assertThat(command.requestPath()).isEqualTo(requestPath);
            assertThat(command.requestMethod()).isEqualTo(requestMethod);
        }

        @Test
        @DisplayName("정적 팩토리 메서드도 null 역할을 빈 Set으로 변환")
        void shouldConvertNullRolesInStaticFactoryMethod() {
            // when
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123", "tenant456", "hash789", null, "/api/v1/users", "GET");

            // then
            assertThat(command.roles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("roles Set이 불변임")
        void shouldHaveImmutableRoles() {
            // given
            Set<String> originalRoles = Set.of("USER", "ADMIN");
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            originalRoles,
                            "/api/v1/users",
                            "GET");

            // when
            Set<String> returnedRoles = command.roles();

            // then
            assertThat(returnedRoles).isEqualTo(originalRoles);
            assertThatThrownBy(() -> returnedRoles.add("NEW_ROLE"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("원본 roles Set 변경이 command에 영향 없음")
        void shouldNotBeAffectedByOriginalRolesModification() {
            // given
            Set<String> mutableRoles = Set.of("USER");
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            mutableRoles,
                            "/api/v1/users",
                            "GET");

            // when & then
            assertThat(command.roles()).containsExactly("USER");
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals()가 올바르게 동작")
        void shouldHaveCorrectEquals() {
            // given
            ValidatePermissionCommand command1 =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");
            ValidatePermissionCommand command2 =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");
            ValidatePermissionCommand command3 =
                    ValidatePermissionCommand.of(
                            "user999",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");

            // when & then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1).isNotEqualTo(command3);
        }

        @Test
        @DisplayName("hashCode()가 올바르게 동작")
        void shouldHaveCorrectHashCode() {
            // given
            ValidatePermissionCommand command1 =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");
            ValidatePermissionCommand command2 =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");

            // when & then
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("toString()이 모든 필드를 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123",
                            "tenant456",
                            "hash789",
                            Set.of("USER"),
                            "/api/v1/users",
                            "GET");

            // when
            String toString = command.toString();

            // then
            assertThat(toString).contains("user123");
            assertThat(toString).contains("tenant456");
            assertThat(toString).contains("hash789");
            assertThat(toString).contains("USER");
            assertThat(toString).contains("/api/v1/users");
            assertThat(toString).contains("GET");
        }
    }

    @Nested
    @DisplayName("다양한 입력값 테스트")
    class VariousInputTest {

        @Test
        @DisplayName("특수 문자가 포함된 ID로 생성")
        void shouldCreateWithSpecialCharactersInIds() {
            // given
            String userId = "user@domain.com";
            String tenantId = "tenant-with-dash_123";
            String permissionHash = "hash-with-special_chars";
            Set<String> roles = Set.of("ROLE_WITH_UNDERSCORE");
            String requestPath = "/api/v1/users/{userId}/orders";
            String requestMethod = "PATCH";

            // when
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            userId, tenantId, permissionHash, roles, requestPath, requestMethod);

            // then
            assertThat(command.userId()).isEqualTo(userId);
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.permissionHash()).isEqualTo(permissionHash);
            assertThat(command.roles()).isEqualTo(roles);
            assertThat(command.requestPath()).isEqualTo(requestPath);
            assertThat(command.requestMethod()).isEqualTo(requestMethod);
        }

        @Test
        @DisplayName("복수의 역할로 생성")
        void shouldCreateWithMultipleRoles() {
            // given
            Set<String> roles = Set.of("USER", "ADMIN", "MANAGER", "VIEWER", "EDITOR");

            // when
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123", "tenant456", "hash789", roles, "/api/v1/users", "GET");

            // then
            assertThat(command.roles()).hasSize(5);
            assertThat(command.roles())
                    .containsExactlyInAnyOrder("USER", "ADMIN", "MANAGER", "VIEWER", "EDITOR");
        }

        @Test
        @DisplayName("단일 역할로 생성")
        void shouldCreateWithSingleRole() {
            // given
            Set<String> roles = Set.of("SINGLE_ROLE");

            // when
            ValidatePermissionCommand command =
                    ValidatePermissionCommand.of(
                            "user123", "tenant456", "hash789", roles, "/api/v1/users", "GET");

            // then
            assertThat(command.roles()).hasSize(1);
            assertThat(command.roles()).containsExactly("SINGLE_ROLE");
        }
    }
}
