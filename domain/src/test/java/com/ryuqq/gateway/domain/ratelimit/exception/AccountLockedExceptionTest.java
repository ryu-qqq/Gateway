package com.ryuqq.gateway.domain.ratelimit.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AccountLockedException 테스트")
class AccountLockedExceptionTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("기본 생성자로 생성")
        void shouldCreateWithDefaultConstructor() {
            // when
            AccountLockedException exception = new AccountLockedException();

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.code()).isEqualTo("RATE-003");
            assertThat(exception.getMessage())
                    .isEqualTo("Account locked due to too many failures.");
        }

        @Test
        @DisplayName("userId로 생성")
        void shouldCreateWithUserId() {
            // when
            AccountLockedException exception = new AccountLockedException("user-123");

            // then
            assertThat(exception).isNotNull();
            assertThat(exception.code()).isEqualTo("RATE-003");
            assertThat(exception.getUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("userId와 잠금 해제 시간으로 생성")
        void shouldCreateWithUserIdAndUnlockTime() {
            // when
            AccountLockedException exception = new AccountLockedException("user-123", 1800);

            // then
            assertThat(exception.getUserId()).isEqualTo("user-123");
            assertThat(exception.getRetryAfterSeconds()).isEqualTo(1800);
        }

        @Test
        @DisplayName("args에 userId, retryAfterSeconds 포함")
        void shouldIncludeArgsInException() {
            // when
            AccountLockedException exception = new AccountLockedException("user-123", 1800);

            // then
            assertThat(exception.args()).containsEntry("userId", "user-123");
            assertThat(exception.args()).containsEntry("retryAfterSeconds", 1800);
        }
    }

    @Nested
    @DisplayName("DomainException 상속 테스트")
    class InheritanceTest {

        @Test
        @DisplayName("DomainException을 상속함")
        void shouldExtendDomainException() {
            // given
            AccountLockedException exception = new AccountLockedException();

            // when & then
            assertThat(exception).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("RuntimeException을 상속함")
        void shouldExtendRuntimeException() {
            // given
            AccountLockedException exception = new AccountLockedException();

            // when & then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("ErrorCode 연동 테스트")
    class ErrorCodeTest {

        @Test
        @DisplayName("ACCOUNT_LOCKED ErrorCode와 일치")
        void shouldMatchErrorCode() {
            // given
            AccountLockedException exception = new AccountLockedException();

            // when & then
            assertThat(exception.code()).isEqualTo(RateLimitErrorCode.ACCOUNT_LOCKED.getCode());
            assertThat(exception.getMessage())
                    .isEqualTo(RateLimitErrorCode.ACCOUNT_LOCKED.getMessage());
        }
    }
}
