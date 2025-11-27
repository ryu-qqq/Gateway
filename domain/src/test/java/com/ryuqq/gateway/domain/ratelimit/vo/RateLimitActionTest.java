package com.ryuqq.gateway.domain.ratelimit.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitAction Enum 테스트")
class RateLimitActionTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 액션이 정의되어 있음")
        void shouldHaveAllActions() {
            // when
            RateLimitAction[] actions = RateLimitAction.values();

            // then
            assertThat(actions).hasSize(4);
            assertThat(actions)
                    .containsExactly(
                            RateLimitAction.REJECT,
                            RateLimitAction.BLOCK_IP,
                            RateLimitAction.LOCK_ACCOUNT,
                            RateLimitAction.REVOKE_TOKEN);
        }
    }

    @Nested
    @DisplayName("REJECT 액션 테스트")
    class RejectTest {

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (429)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitAction.REJECT.getHttpStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("올바른 설명을 가짐")
        void shouldHaveCorrectDescription() {
            assertThat(RateLimitAction.REJECT.getDescription())
                    .isEqualTo("Too Many Requests - Reject the request");
        }
    }

    @Nested
    @DisplayName("BLOCK_IP 액션 테스트")
    class BlockIpTest {

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (403)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitAction.BLOCK_IP.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 설명을 가짐")
        void shouldHaveCorrectDescription() {
            assertThat(RateLimitAction.BLOCK_IP.getDescription())
                    .isEqualTo("Forbidden - Block IP for 30 minutes");
        }
    }

    @Nested
    @DisplayName("LOCK_ACCOUNT 액션 테스트")
    class LockAccountTest {

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (403)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitAction.LOCK_ACCOUNT.getHttpStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("올바른 설명을 가짐")
        void shouldHaveCorrectDescription() {
            assertThat(RateLimitAction.LOCK_ACCOUNT.getDescription())
                    .isEqualTo("Forbidden - Lock account for 30 minutes");
        }
    }

    @Nested
    @DisplayName("REVOKE_TOKEN 액션 테스트")
    class RevokeTokenTest {

        @Test
        @DisplayName("올바른 HTTP 상태 코드를 가짐 (429)")
        void shouldHaveCorrectHttpStatus() {
            assertThat(RateLimitAction.REVOKE_TOKEN.getHttpStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("올바른 설명을 가짐")
        void shouldHaveCorrectDescription() {
            assertThat(RateLimitAction.REVOKE_TOKEN.getDescription())
                    .isEqualTo("Too Many Requests - Revoke refresh token");
        }
    }

    @Nested
    @DisplayName("Enum 동작 테스트")
    class EnumBehaviorTest {

        @Test
        @DisplayName("valueOf()로 Enum 값 조회")
        void shouldGetEnumValueUsingValueOf() {
            assertThat(RateLimitAction.valueOf("REJECT")).isEqualTo(RateLimitAction.REJECT);
            assertThat(RateLimitAction.valueOf("BLOCK_IP")).isEqualTo(RateLimitAction.BLOCK_IP);
            assertThat(RateLimitAction.valueOf("LOCK_ACCOUNT"))
                    .isEqualTo(RateLimitAction.LOCK_ACCOUNT);
            assertThat(RateLimitAction.valueOf("REVOKE_TOKEN"))
                    .isEqualTo(RateLimitAction.REVOKE_TOKEN);
        }

        @Test
        @DisplayName("ordinal() 값이 올바름")
        void shouldHaveCorrectOrdinalValues() {
            assertThat(RateLimitAction.REJECT.ordinal()).isEqualTo(0);
            assertThat(RateLimitAction.BLOCK_IP.ordinal()).isEqualTo(1);
            assertThat(RateLimitAction.LOCK_ACCOUNT.ordinal()).isEqualTo(2);
            assertThat(RateLimitAction.REVOKE_TOKEN.ordinal()).isEqualTo(3);
        }
    }
}
