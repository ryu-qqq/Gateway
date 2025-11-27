package com.ryuqq.gateway.application.ratelimit.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResetRateLimitCommand 단위 테스트")
class ResetRateLimitCommandTest {

    @Nested
    @DisplayName("생성 검증")
    class Creation {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        void shouldCreateWithValidValues() {
            // given
            LimitType limitType = LimitType.IP;
            String identifier = "192.168.1.1";
            String adminId = "admin-001";

            // when
            ResetRateLimitCommand command =
                    new ResetRateLimitCommand(limitType, identifier, adminId);

            // then
            assertThat(command.limitType()).isEqualTo(limitType);
            assertThat(command.identifier()).isEqualTo(identifier);
            assertThat(command.adminId()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("null limitType으로 생성 시 예외 발생")
        void shouldThrowExceptionWhenLimitTypeIsNull() {
            assertThatThrownBy(() -> new ResetRateLimitCommand(null, "identifier", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("LimitType cannot be null");
        }

        @Test
        @DisplayName("null identifier로 생성 시 예외 발생")
        void shouldThrowExceptionWhenIdentifierIsNull() {
            assertThatThrownBy(() -> new ResetRateLimitCommand(LimitType.IP, null, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Identifier cannot be null or blank");
        }

        @Test
        @DisplayName("blank identifier로 생성 시 예외 발생")
        void shouldThrowExceptionWhenIdentifierIsBlank() {
            assertThatThrownBy(() -> new ResetRateLimitCommand(LimitType.IP, "  ", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Identifier cannot be null or blank");
        }

        @Test
        @DisplayName("null adminId로 생성 시 예외 발생")
        void shouldThrowExceptionWhenAdminIdIsNull() {
            assertThatThrownBy(() -> new ResetRateLimitCommand(LimitType.IP, "192.168.1.1", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("AdminId cannot be null or blank");
        }

        @Test
        @DisplayName("blank adminId로 생성 시 예외 발생")
        void shouldThrowExceptionWhenAdminIdIsBlank() {
            assertThatThrownBy(() -> new ResetRateLimitCommand(LimitType.IP, "192.168.1.1", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("AdminId cannot be null or blank");
        }
    }
}
