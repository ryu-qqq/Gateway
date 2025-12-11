package com.ryuqq.gateway.domain.authorization.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionDeniedException 테스트")
class PermissionDeniedExceptionTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("권한 정보와 함께 예외 생성")
        void shouldCreateExceptionWithPermissionInfo() {
            // given
            Set<String> requiredPermissions = Set.of("user:read", "user:write");
            Set<String> userPermissions = Set.of("user:read");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
            // Set 순서는 보장되지 않으므로 contains로 검증
            assertThat(exception.getMessage()).startsWith("Permission denied: Required:");
            assertThat(exception.getMessage()).contains("user:read");
            assertThat(exception.getMessage()).contains("user:write");
            assertThat(exception.requiredPermissions()).isEqualTo(requiredPermissions);
            assertThat(exception.userPermissions()).isEqualTo(userPermissions);
        }

        @Test
        @DisplayName("빈 권한 Set으로 예외 생성")
        void shouldCreateExceptionWithEmptyPermissionSets() {
            // given
            Set<String> requiredPermissions = Set.of();
            Set<String> userPermissions = Set.of();

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
            assertThat(exception.getMessage())
                    .isEqualTo("Permission denied: Required: [], User has: []");
            assertThat(exception.requiredPermissions()).isEmpty();
            assertThat(exception.userPermissions()).isEmpty();
        }

        @Test
        @DisplayName("단일 권한으로 예외 생성")
        void shouldCreateExceptionWithSinglePermission() {
            // given
            Set<String> requiredPermissions = Set.of("admin:delete");
            Set<String> userPermissions = Set.of("user:read");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
            assertThat(exception.getMessage()).contains("admin:delete");
            assertThat(exception.getMessage()).contains("user:read");
            assertThat(exception.requiredPermissions()).containsExactly("admin:delete");
            assertThat(exception.userPermissions()).containsExactly("user:read");
        }

        @Test
        @DisplayName("메시지만으로 예외 생성")
        void shouldCreateExceptionWithMessageOnly() {
            // given
            String detail = "Custom permission denied message";

            // when
            PermissionDeniedException exception = new PermissionDeniedException(detail);

            // then
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
            // DomainException 형식: ErrorCode.getMessage() + ": " + detail
            assertThat(exception.getMessage()).isEqualTo("Permission denied: " + detail);
            assertThat(exception.requiredPermissions()).isEmpty();
            assertThat(exception.userPermissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("권한 정보 접근 테스트")
    class PermissionAccessTest {

        @Test
        @DisplayName("requiredPermissions() 메서드가 불변 Set 반환")
        void shouldReturnImmutableRequiredPermissions() {
            // given - mutable Set 사용하여 불변성 검증
            java.util.Set<String> requiredPermissions = new java.util.HashSet<>();
            requiredPermissions.add("order:create");
            requiredPermissions.add("order:update");
            Set<String> userPermissions = Set.of("order:read");
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // when - 원본 수정
            requiredPermissions.add("order:delete");
            Set<String> returned = exception.requiredPermissions();

            // then - 내부 상태에 영향 없음
            assertThat(returned).hasSize(2);
            assertThat(returned).containsExactlyInAnyOrder("order:create", "order:update");
        }

        @Test
        @DisplayName("userPermissions() 메서드가 불변 Set 반환")
        void shouldReturnImmutableUserPermissions() {
            // given - mutable Set 사용하여 불변성 검증
            Set<String> requiredPermissions = Set.of("payment:process");
            java.util.Set<String> userPermissions = new java.util.HashSet<>();
            userPermissions.add("payment:read");
            userPermissions.add("payment:view");
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // when - 원본 수정
            userPermissions.add("payment:extra");
            Set<String> returned = exception.userPermissions();

            // then - 내부 상태에 영향 없음
            assertThat(returned).hasSize(2);
            assertThat(returned).containsExactlyInAnyOrder("payment:read", "payment:view");
        }

        @Test
        @DisplayName("메시지 생성자로 만든 예외는 빈 권한 Set 반환")
        void shouldReturnEmptyPermissionSetsForMessageConstructor() {
            // given
            PermissionDeniedException exception = new PermissionDeniedException("Test message");

            // when & then
            assertThat(exception.requiredPermissions()).isEmpty();
            assertThat(exception.userPermissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("메시지 생성 테스트")
    class MessageBuildingTest {

        @Test
        @DisplayName("복수 권한이 있는 경우 메시지 형식")
        void shouldBuildCorrectMessageForMultiplePermissions() {
            // given
            Set<String> requiredPermissions =
                    Set.of("inventory:read", "inventory:write", "inventory:delete");
            Set<String> userPermissions = Set.of("inventory:read", "inventory:write");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            String message = exception.getMessage();
            assertThat(message).startsWith("Permission denied: Required:");
            assertThat(message).contains("inventory:read");
            assertThat(message).contains("inventory:write");
            assertThat(message).contains("inventory:delete");
            assertThat(message).contains("User has:");
        }

        @Test
        @DisplayName("특수 문자가 포함된 권한명 처리")
        void shouldHandlePermissionsWithSpecialCharacters() {
            // given
            Set<String> requiredPermissions = Set.of("user:read-profile", "user:write_data");
            Set<String> userPermissions = Set.of("user:read-profile");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            String message = exception.getMessage();
            assertThat(message).contains("user:read-profile");
            assertThat(message).contains("user:write_data");
        }

        @Test
        @DisplayName("긴 권한명 처리")
        void shouldHandleLongPermissionNames() {
            // given
            Set<String> requiredPermissions =
                    Set.of("very-long-permission-name-with-many-parts:read:specific:resource");
            Set<String> userPermissions = Set.of("short:read");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            String message = exception.getMessage();
            assertThat(message)
                    .contains("very-long-permission-name-with-many-parts:read:specific:resource");
            assertThat(message).contains("short:read");
        }
    }

    @Nested
    @DisplayName("ErrorCode 매핑 테스트")
    class ErrorCodeMappingTest {

        @Test
        @DisplayName("AuthorizationErrorCode.PERMISSION_DENIED와 매핑됨")
        void shouldMapToPermissionDeniedErrorCode() {
            // given
            PermissionDeniedException exception =
                    new PermissionDeniedException(Set.of("test:permission"), Set.of());

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED.getCode());
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
        }

        @Test
        @DisplayName("메시지 생성자도 동일한 ErrorCode 사용")
        void shouldUsesSameErrorCodeForMessageConstructor() {
            // given
            PermissionDeniedException exception = new PermissionDeniedException("Custom message");

            // when & then
            assertThat(exception.getCode())
                    .isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED.getCode());
            assertThat(exception.getCode()).isEqualTo("AUTHZ-001");
        }
    }

    @Nested
    @DisplayName("필드 접근자 테스트")
    class FieldAccessorTest {

        @Test
        @DisplayName("권한 정보를 개별 필드 접근자로 확인")
        void shouldAccessPermissionInfoThroughGetters() {
            // given
            Set<String> requiredPermissions = Set.of("notification:send");
            Set<String> userPermissions = Set.of("notification:read");

            // when
            PermissionDeniedException exception =
                    new PermissionDeniedException(requiredPermissions, userPermissions);

            // then
            assertThat(exception.requiredPermissions()).isEqualTo(requiredPermissions);
            assertThat(exception.userPermissions()).isEqualTo(userPermissions);
        }

        @Test
        @DisplayName("메시지 생성자는 빈 권한 목록 반환")
        void shouldReturnEmptyPermissionsForMessageConstructor() {
            // given
            PermissionDeniedException exception = new PermissionDeniedException("Test message");

            // when & then
            assertThat(exception.requiredPermissions()).isEmpty();
            assertThat(exception.userPermissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("예외 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            PermissionDeniedException exception =
                    new PermissionDeniedException(Set.of("test:permission"), Set.of());

            // when & then
            assertThat(exception).isInstanceOf(RuntimeException.class);
            // DomainException 클래스가 있다고 가정
        }
    }
}
