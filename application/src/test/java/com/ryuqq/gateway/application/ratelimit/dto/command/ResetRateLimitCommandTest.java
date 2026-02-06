package com.ryuqq.gateway.application.ratelimit.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

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
    }
}
