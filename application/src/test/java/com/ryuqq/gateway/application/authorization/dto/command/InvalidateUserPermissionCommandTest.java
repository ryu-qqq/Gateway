package com.ryuqq.gateway.application.authorization.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidateUserPermissionCommand 테스트")
class InvalidateUserPermissionCommandTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("정상적인 InvalidateUserPermissionCommand 생성")
        void shouldCreateInvalidateUserPermissionCommand() {
            // given
            String tenantId = "tenant123";
            String userId = "user456";

            // when
            InvalidateUserPermissionCommand command =
                    new InvalidateUserPermissionCommand(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("null tenantId로 생성 시 예외 발생")
        void shouldThrowExceptionForNullTenantId() {
            assertThatThrownBy(() -> new InvalidateUserPermissionCommand(null, "user456"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("tenantId cannot be null");
        }

        @Test
        @DisplayName("null userId로 생성 시 예외 발생")
        void shouldThrowExceptionForNullUserId() {
            assertThatThrownBy(() -> new InvalidateUserPermissionCommand("tenant123", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("userId cannot be null");
        }

        @Test
        @DisplayName("둘 다 null로 생성 시 tenantId 예외가 먼저 발생")
        void shouldThrowTenantIdExceptionFirst() {
            assertThatThrownBy(() -> new InvalidateUserPermissionCommand(null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("tenantId cannot be null");
        }

        @Test
        @DisplayName("빈 문자열로 생성")
        void shouldCreateWithEmptyStrings() {
            // given
            String tenantId = "";
            String userId = "";

            // when
            InvalidateUserPermissionCommand command =
                    new InvalidateUserPermissionCommand(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEmpty();
            assertThat(command.userId()).isEmpty();
        }

        @Test
        @DisplayName("공백 문자열로 생성")
        void shouldCreateWithBlankStrings() {
            // given
            String tenantId = "   ";
            String userId = "   ";

            // when
            InvalidateUserPermissionCommand command =
                    new InvalidateUserPermissionCommand(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo("   ");
            assertThat(command.userId()).isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("of() 정적 팩토리 메서드 테스트")
    class OfMethodTest {

        @Test
        @DisplayName("정적 팩토리 메서드로 생성")
        void shouldCreateUsingStaticFactoryMethod() {
            // given
            String tenantId = "tenant789";
            String userId = "user999";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("정적 팩토리 메서드도 null 검증 수행")
        void shouldValidateNullsInStaticFactoryMethod() {
            assertThatThrownBy(() -> InvalidateUserPermissionCommand.of(null, "user456"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("tenantId cannot be null");

            assertThatThrownBy(() -> InvalidateUserPermissionCommand.of("tenant123", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("userId cannot be null");
        }
    }

    @Nested
    @DisplayName("Record 동작 테스트")
    class RecordBehaviorTest {

        @Test
        @DisplayName("equals()가 올바르게 동작")
        void shouldHaveCorrectEquals() {
            // given
            InvalidateUserPermissionCommand command1 =
                    InvalidateUserPermissionCommand.of("tenant123", "user456");
            InvalidateUserPermissionCommand command2 =
                    InvalidateUserPermissionCommand.of("tenant123", "user456");
            InvalidateUserPermissionCommand command3 =
                    InvalidateUserPermissionCommand.of("tenant999", "user456");

            // when & then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1).isNotEqualTo(command3);
        }

        @Test
        @DisplayName("hashCode()가 올바르게 동작")
        void shouldHaveCorrectHashCode() {
            // given
            InvalidateUserPermissionCommand command1 =
                    InvalidateUserPermissionCommand.of("tenant123", "user456");
            InvalidateUserPermissionCommand command2 =
                    InvalidateUserPermissionCommand.of("tenant123", "user456");

            // when & then
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("toString()이 모든 필드를 포함")
        void shouldIncludeAllFieldsInToString() {
            // given
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of("tenant123", "user456");

            // when
            String toString = command.toString();

            // then
            assertThat(toString).contains("tenant123");
            assertThat(toString).contains("user456");
        }
    }

    @Nested
    @DisplayName("다양한 입력값 테스트")
    class VariousInputTest {

        @Test
        @DisplayName("특수 문자가 포함된 ID로 생성")
        void shouldCreateWithSpecialCharactersInIds() {
            // given
            String tenantId = "tenant-with-dash_123";
            String userId = "user@domain.com";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("긴 문자열로 생성")
        void shouldCreateWithLongStrings() {
            // given
            String tenantId = "very-long-tenant-id-with-many-characters-1234567890";
            String userId = "very-long-user-id-with-many-characters-abcdefghijklmnopqrstuvwxyz";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("숫자로만 구성된 ID로 생성")
        void shouldCreateWithNumericIds() {
            // given
            String tenantId = "123456789";
            String userId = "987654321";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("UUID 형식의 ID로 생성")
        void shouldCreateWithUuidFormatIds() {
            // given
            String tenantId = "550e8400-e29b-41d4-a716-446655440000";
            String userId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("이메일 형식의 userId로 생성")
        void shouldCreateWithEmailFormatUserId() {
            // given
            String tenantId = "company-tenant";
            String userId = "john.doe@example.com";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("한글이 포함된 ID로 생성")
        void shouldCreateWithKoreanCharacters() {
            // given
            String tenantId = "테넌트123";
            String userId = "사용자456";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("URL 인코딩된 문자가 포함된 ID로 생성")
        void shouldCreateWithUrlEncodedCharacters() {
            // given
            String tenantId = "tenant%20with%20spaces";
            String userId = "user%40domain.com";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("비즈니스 시나리오 테스트")
    class BusinessScenarioTest {

        @Test
        @DisplayName("단일 사용자 권한 무효화 시나리오")
        void shouldCreateForSingleUserInvalidationScenario() {
            // given
            String tenantId = "enterprise-tenant";
            String userId = "employee-001";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("관리자 권한 변경 시나리오")
        void shouldCreateForAdminPermissionChangeScenario() {
            // given
            String tenantId = "admin-tenant";
            String userId = "super-admin";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("멀티 테넌트 환경에서의 사용자 권한 무효화")
        void shouldCreateForMultiTenantScenario() {
            // given
            String tenantId = "tenant-a";
            String userId = "shared-user-123";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("임시 사용자 권한 무효화 시나리오")
        void shouldCreateForTemporaryUserScenario() {
            // given
            String tenantId = "temp-tenant";
            String userId = "guest-user-temp-001";

            // when
            InvalidateUserPermissionCommand command =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
            assertThat(command.userId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("동일성 검증 테스트")
    class IdentityTest {

        @Test
        @DisplayName("동일한 tenantId와 userId를 가진 명령은 동일함")
        void shouldBeEqualWithSameIds() {
            // given
            String tenantId = "same-tenant";
            String userId = "same-user";

            // when
            InvalidateUserPermissionCommand command1 =
                    InvalidateUserPermissionCommand.of(tenantId, userId);
            InvalidateUserPermissionCommand command2 =
                    InvalidateUserPermissionCommand.of(tenantId, userId);

            // then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("다른 tenantId를 가진 명령은 다름")
        void shouldNotBeEqualWithDifferentTenantId() {
            // given
            String userId = "same-user";

            // when
            InvalidateUserPermissionCommand command1 =
                    InvalidateUserPermissionCommand.of("tenant-1", userId);
            InvalidateUserPermissionCommand command2 =
                    InvalidateUserPermissionCommand.of("tenant-2", userId);

            // then
            assertThat(command1).isNotEqualTo(command2);
        }

        @Test
        @DisplayName("다른 userId를 가진 명령은 다름")
        void shouldNotBeEqualWithDifferentUserId() {
            // given
            String tenantId = "same-tenant";

            // when
            InvalidateUserPermissionCommand command1 =
                    InvalidateUserPermissionCommand.of(tenantId, "user-1");
            InvalidateUserPermissionCommand command2 =
                    InvalidateUserPermissionCommand.of(tenantId, "user-2");

            // then
            assertThat(command1).isNotEqualTo(command2);
        }
    }
}
