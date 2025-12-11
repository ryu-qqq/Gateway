package com.ryuqq.gateway.application.tenant.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SyncTenantConfigCommand 테스트")
class SyncTenantConfigCommandTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreationTest {

        @Test
        @DisplayName("유효한 tenantId로 생성 성공")
        void shouldCreateWithValidTenantId() {
            // given
            String tenantId = "tenant-1";

            // when
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
        }

        @ParameterizedTest
        @ValueSource(strings = {"tenant-123", "tenant-1", "test-tenant-id"})
        @DisplayName("다양한 유효한 tenantId로 생성 성공")
        void shouldCreateWithVariousValidTenantIds(String tenantId) {
            // when
            SyncTenantConfigCommand command = new SyncTenantConfigCommand(tenantId);

            // then
            assertThat(command.tenantId()).isEqualTo(tenantId);
        }
    }

    @Nested
    @DisplayName("검증 실패 테스트")
    class ValidationFailureTest {

        @Test
        @DisplayName("null tenantId로 생성 시 IllegalArgumentException 발생")
        void shouldThrowExceptionWhenTenantIdIsNull() {
            // when & then
            assertThatThrownBy(() -> new SyncTenantConfigCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("TenantId cannot be null or blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("null 또는 blank tenantId로 생성 시 IllegalArgumentException 발생")
        void shouldThrowExceptionWhenTenantIdIsNullOrBlank(String tenantId) {
            // when & then
            assertThatThrownBy(() -> new SyncTenantConfigCommand(tenantId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("TenantId cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Record 동등성 테스트")
    class EqualityTest {

        @Test
        @DisplayName("같은 tenantId를 가진 Command는 동등하다")
        void shouldBeEqualWhenSameTenantId() {
            // given
            String tenantId = "tenant-1";

            // when
            SyncTenantConfigCommand command1 = new SyncTenantConfigCommand(tenantId);
            SyncTenantConfigCommand command2 = new SyncTenantConfigCommand(tenantId);

            // then
            assertThat(command1).isEqualTo(command2);
            assertThat(command1.hashCode()).isEqualTo(command2.hashCode());
        }

        @Test
        @DisplayName("다른 tenantId를 가진 Command는 동등하지 않다")
        void shouldNotBeEqualWhenDifferentTenantId() {
            // given
            SyncTenantConfigCommand command1 = new SyncTenantConfigCommand("tenant-1");
            SyncTenantConfigCommand command2 = new SyncTenantConfigCommand("tenant-2");

            // then
            assertThat(command1).isNotEqualTo(command2);
        }
    }
}
